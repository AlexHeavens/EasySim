package net.alexheavens.cs4099.child;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The simulator's side of the child JVM that runs the student nodes (ADR 0002). It starts the process, owns the
 * socket, and routes frames: requests and the end of a node go to that node's inbox, and replies and commands go
 * out through {@link #send(Frame)}.
 * <p>
 * Kill ladder: {@link #kill(int)} sends {@link Frame.Kill}. If {@link Frame.Ended} does not arrive within the grace
 * period, the child process is killed, the run is failed, and the failure names the node.
 * <p>
 * Inboxes run on the reader thread, so they must not block.
 */
public final class ChildJvm implements AutoCloseable {

	/** Node number in a {@link Failure} that no single node caused, such as the child JVM running out of memory. */
	public static final int NO_NODE = -1;

	/**
	 * How to start the child.
	 *
	 * @param agent       the Tascalate agent jar
	 * @param scriptDir   the directory holding the student sources, or null if scripts are classes on the classpath
	 * @param maxHeap     the child's heap cap, as the {@code -Xmx} value, for example {@code 512m}
	 * @param workers     scheduler worker threads
	 * @param sliceTicks  safe points a node runs before it yields its worker
	 * @param killGrace   how long a killed node may take to end before the process is killed
	 * @param startupWait how long the child may take to connect
	 */
	public record Config(Path agent, Path scriptDir, String maxHeap, int workers, int sliceTicks, Duration killGrace,
			Duration startupWait) {
	}

	/** Why the run failed. {@code node} is {@link #NO_NODE} when the child ended without a node to blame. */
	public record Failure(int node, String reason) {
	}

	private final Config config;
	private final Consumer<Failure> onFailure;
	private final Process process;
	private final Socket socket;
	private final DataOutputStream out;
	private final ConcurrentHashMap<Integer, Node> nodes = new ConcurrentHashMap<>();
	private final ScheduledExecutorService timers = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread timer = new Thread(r, "child-jvm-kill-timer");
		timer.setDaemon(true);
		return timer;
	});
	private volatile Failure failure;
	private volatile boolean closing;

	private static final class Node {
		final Consumer<Frame> inbox;
		volatile boolean ended;
		volatile boolean killRequested;

		Node(Consumer<Frame> inbox) {
			this.inbox = inbox;
		}
	}

	/** Starts the child and waits for it to connect. {@code onFailure} is called once, if the run fails. */
	public ChildJvm(Config config, Consumer<Failure> onFailure) throws IOException {
		this.config = config;
		this.onFailure = onFailure;
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			process = new ProcessBuilder(command(server.getLocalPort())).inheritIO().start();
			try {
				server.setSoTimeout((int) config.startupWait().toMillis());
				socket = server.accept();
			} catch (IOException e) {
				process.destroyForcibly();
				throw e;
			}
		}
		socket.setTcpNoDelay(true);
		out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		Thread reader = new Thread(() -> read(in), "child-jvm-reader");
		reader.setDaemon(true);
		reader.start();
		process.onExit().thenAccept(p -> {
			if (!closing)
				fail(new Failure(NO_NODE, "The child JVM exited with code " + p.exitValue() + "."));
		});
	}

	private List<String> command(int port) {
		List<String> command = new ArrayList<>();
		command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
		command.add("-javaagent:" + config.agent());
		command.add("-Xmx" + config.maxHeap());
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.add(ChildMain.class.getName());
		command.add(Integer.toString(port));
		command.add(Integer.toString(config.workers()));
		command.add(Integer.toString(config.sliceTicks()));
		command.add(config.scriptDir() == null ? ChildMain.NO_SCRIPT_DIR : config.scriptDir().toString());
		return command;
	}

	/** Starts a node in the child. Its requests and its end arrive in {@code inbox}. */
	public void start(int node, String scriptClass, boolean initiator, int machineId, Consumer<Frame> inbox) {
		nodes.put(node, new Node(inbox));
		send(new Frame.Start(node, scriptClass, initiator, machineId));
	}

	/** Sends a reply or command to the child. Does nothing once the run has failed. */
	public void send(Frame frame) {
		if (failure != null || closing)
			return;
		try {
			synchronized (out) {
				FrameCodec.write(out, frame);
				out.flush();
			}
		} catch (IOException e) {
			if (failure == null && !closing)
				throw new UncheckedIOException(e);
		}
	}

	/** Asks the node to stop. If it has not ended within the grace period, kills the child and fails the run. */
	public void kill(int node) {
		Node target = nodes.get(node);
		if (target == null || target.ended)
			return;
		target.killRequested = true;
		send(new Frame.Kill(node));
		timers.schedule(() -> {
			if (!target.ended)
				fail(new Failure(node, "Node " + node + " did not stop within " + config.killGrace().toMillis()
						+ " ms of being killed. The child JVM was killed."));
		}, config.killGrace().toMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Waits until every node that was asked to stop has ended, or the run has failed. The grace period bounds the
	 * wait, so after this a node that could not be stopped is in {@link #failure()}.
	 */
	public void awaitKills() throws InterruptedException {
		while (failure == null && nodes.values().stream().anyMatch(n -> n.killRequested && !n.ended))
			Thread.sleep(10);
	}

	/** The failure that ended the run, or null while it has not failed. */
	public Failure failure() {
		return failure;
	}

	public boolean isAlive() {
		return process.isAlive();
	}

	private void read(DataInputStream in) {
		try {
			while (true) {
				Frame frame = FrameCodec.read(in);
				Node node = nodes.get(frame.node());
				if (node == null)
					throw new IOException("Frame for unknown node: " + frame);
				if (frame instanceof Frame.Ended)
					node.ended = true;
				node.inbox.accept(frame);
			}
		} catch (EOFException e) {
			// The child closed the connection; the process exit decides whether that is a failure.
		} catch (IOException | RuntimeException e) {
			if (!closing)
				fail(new Failure(NO_NODE, "The child JVM connection broke: " + e));
		}
	}

	/** Records the first failure and kills the child. */
	private synchronized void fail(Failure reason) {
		if (failure != null || closing)
			return;
		failure = reason;
		process.destroyForcibly();
		onFailure.accept(reason);
	}

	/** Closes the connection, which ends the child, and kills the child if it does not exit. */
	@Override
	public void close() {
		closing = true;
		timers.shutdownNow();
		try {
			socket.close();
		} catch (IOException e) {
			// Closing anyway.
		}
		try {
			if (!process.waitFor(5, TimeUnit.SECONDS))
				process.destroyForcibly();
		} catch (InterruptedException e) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
		}
	}
}
