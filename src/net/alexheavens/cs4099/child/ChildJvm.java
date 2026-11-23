package net.alexheavens.cs4099.child;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The simulator's side of the child JVM that runs the student nodes (ADR 0002). The child runs in a Docker container
 * that has no network, a read-only file system, no capabilities and capped memory, CPU and processes (ADR 0003). This
 * class starts the container and routes frames over its standard input and output: requests and the end of a node go
 * to that node's inbox, and replies and commands go out through {@link #send(Frame)}.
 * <p>
 * Kill ladder: {@link #kill(int)} sends {@link Frame.Kill}. If {@link Frame.Ended} does not arrive within the grace
 * period, the container is removed, the run is failed, and the failure names the node.
 * <p>
 * Inboxes run on the reader thread, so they must not block.
 */
public final class ChildJvm implements AutoCloseable {

	/** The first four bytes that the child writes, once it can take frames. */
	static final int READY = 0x45535931;

	/** Node number in a {@link Failure} that no single node caused, such as the child JVM running out of memory. */
	public static final int NO_NODE = -1;

	/**
	 * How to start the child.
	 *
	 * @param agent       the Tascalate agent jar
	 * @param scriptDir   the directory holding the student sources, or null if scripts are classes on the classpath
	 * @param heapMegabytes the child's heap cap in megabytes. The container's memory limit adds room for the JVM itself.
	 * @param workers     scheduler worker threads
	 * @param sliceTicks  safe points a node runs before it yields its worker
	 * @param killGrace   how long a killed node may take to end before the process is killed
	 * @param startupWait how long the child may take to say it is ready, which includes pulling the image
	 */
	public record Config(Path agent, Path scriptDir, int heapMegabytes, int workers, int sliceTicks,
			Duration killGrace, Duration startupWait) {
	}

	/** The resource that the build writes from {@code pins/sandbox/Dockerfile}: the image the child runs in. */
	private static final String IMAGE_RESOURCE = "sandbox-image.txt";

	private static final int NON_HEAP_MEGABYTES = 256;
	private static final int MAX_PROCESSES = 512;
	private static final int SCRATCH_MEGABYTES = 64;
	private static final String SANDBOX_DIR = "/easysim";

	/** Why the run failed. {@code node} is {@link #NO_NODE} when the child ended without a node to blame. */
	public record Failure(int node, String reason) {
	}

	private final Config config;
	private final Consumer<Failure> onFailure;
	private final String container = "easysim-" + UUID.randomUUID();
	private final Process process;
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

	/**
	 * Starts the container and waits for the child to say it is ready. {@code onFailure} is called once, if the run
	 * fails.
	 */
	public ChildJvm(Config config, Consumer<Failure> onFailure) throws IOException {
		this.config = config;
		this.onFailure = onFailure;
		process = new ProcessBuilder(command()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
		out = new DataOutputStream(new BufferedOutputStream(process.getOutputStream()));
		DataInputStream in = new DataInputStream(new BufferedInputStream(process.getInputStream()));
		ScheduledFuture<?> startupLimit = timers.schedule(this::terminate, config.startupWait().toMillis(),
				TimeUnit.MILLISECONDS);
		try {
			if (in.readInt() != READY)
				throw new IOException("The child JVM did not start as expected.");
		} catch (IOException e) {
			closing = true;
			terminate();
			timers.shutdownNow();
			throw new IOException("The child container did not start. Is Docker running? " + e, e);
		} finally {
			startupLimit.cancel(false);
		}
		Thread reader = new Thread(() -> read(in), "child-jvm-reader");
		reader.setDaemon(true);
		reader.start();
		process.onExit().thenAccept(p -> {
			if (!closing)
				fail(new Failure(NO_NODE, "The child JVM exited with code " + p.exitValue() + "."));
		});
	}

	/**
	 * Runs the child as the simulator's own user, not as root. Without capabilities, root cannot read the files of a
	 * user whose directories are private. A system without Unix user ids gets the image's default user.
	 */
	private static List<String> user() {
		try {
			Path home = Path.of(System.getProperty("user.home"));
			return List.of("--user", Files.getAttribute(home, "unix:uid") + ":" + Files.getAttribute(home, "unix:gid"));
		} catch (IOException | UnsupportedOperationException | IllegalArgumentException e) {
			return List.of();
		}
	}

	private static String image() {
		try (InputStream resource = ChildJvm.class.getResourceAsStream(IMAGE_RESOURCE)) {
			if (resource == null)
				throw new IllegalStateException("The sandbox image name is not bundled. Build with Gradle.");
			return new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private List<String> command() {
		List<String> command = new ArrayList<>(List.of("docker", "run", "--rm", "--interactive", "--name", container,
				"--network", "none", "--read-only", "--cap-drop", "ALL", "--security-opt", "no-new-privileges",
				"--pids-limit", Integer.toString(MAX_PROCESSES),
				"--memory", (config.heapMegabytes() + NON_HEAP_MEGABYTES) + "m",
				"--memory-swap", (config.heapMegabytes() + NON_HEAP_MEGABYTES) + "m",
				"--cpus", Integer.toString(config.workers()),
				"--tmpfs", "/sandbox-tmp:rw,noexec,nosuid,size=" + SCRATCH_MEGABYTES + "m",
				"--volume", config.agent().toAbsolutePath() + ":" + SANDBOX_DIR + "/agent.jar:ro"));
		command.addAll(user());
		if (config.scriptDir() != null)
			command.addAll(List.of("--volume", config.scriptDir().toAbsolutePath() + ":" + SANDBOX_DIR + "/scripts:ro"));
		// The classpath entries are mounted read-only at their own paths, so the child sees the classpath as it is here.
		Set<String> classpath = new LinkedHashSet<>();
		for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
			Path path = Path.of(entry).toAbsolutePath();
			if (Files.exists(path) && classpath.add(path.toString()))
				command.addAll(List.of("--volume", path + ":" + path + ":ro"));
		}
		command.addAll(List.of(image(), "java", "-javaagent:" + SANDBOX_DIR + "/agent.jar", "-Xmx" + config.heapMegabytes() + "m",
				"-Djava.io.tmpdir=/sandbox-tmp", "-XX:-UsePerfData", "-cp", String.join(File.pathSeparator, classpath),
				ChildMain.class.getName(), Integer.toString(config.workers()), Integer.toString(config.sliceTicks()),
				config.scriptDir() == null ? ChildMain.NO_SCRIPT_DIR : SANDBOX_DIR + "/scripts"));
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
		terminate();
		onFailure.accept(reason);
	}

	/**
	 * Removes the container, and then the client process. Killing only the client would leave the container running,
	 * because the Docker daemon owns it.
	 */
	private void terminate() {
		try {
			Process remove = new ProcessBuilder("docker", "rm", "--force", container).redirectErrorStream(true)
					.redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
			if (!remove.waitFor(10, TimeUnit.SECONDS))
				remove.destroyForcibly();
		} catch (IOException e) {
			// The client kill below still ends the run; the container then ends when its input closes.
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		process.destroyForcibly();
	}

	/** Closes the child's input, which ends it, and removes the container if it does not exit. */
	@Override
	public void close() {
		closing = true;
		timers.shutdownNow();
		try {
			out.close();
		} catch (IOException e) {
			// Closing anyway.
		}
		try {
			if (!process.waitFor(5, TimeUnit.SECONDS))
				terminate();
		} catch (InterruptedException e) {
			terminate();
			Thread.currentThread().interrupt();
		}
	}
}
