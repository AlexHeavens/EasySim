package net.alexheavens.cs4099.child;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import net.alexheavens.cs4099.scheduler.ContinuationScheduler;
import net.alexheavens.cs4099.usercode.DynamicClassLoader;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * Entry point of the child JVM (ADR 0002). {@link ChildJvm} starts it with the Tascalate agent. It connects to the
 * simulator on the loopback port, then runs {@link NodeHost} until the simulator closes the connection.
 * <p>
 * Arguments: port, worker count, time slice in safe-point ticks, directory holding the student sources ({@code -} for
 * none).
 */
public final class ChildMain {

	/** Passed as the source directory when scripts are classes on the classpath. */
	static final String NO_SCRIPT_DIR = "-";

	private static final ConcurrentHashMap<String, Class<?>> CLASSES = new ConcurrentHashMap<>();

	private ChildMain() {
	}

	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		int workers = Integer.parseInt(args[1]);
		int sliceTicks = Integer.parseInt(args[2]);
		File scriptDir = args[3].equals(NO_SCRIPT_DIR) ? null : new File(args[3]);
		try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
				ContinuationScheduler scheduler = new ContinuationScheduler(workers, sliceTicks)) {
			// The host writes a frame as several small writes with no flush, so the stream is not buffered.
			socket.setTcpNoDelay(true);
			new NodeHost(new DataInputStream(new BufferedInputStream(socket.getInputStream())),
					new DataOutputStream(socket.getOutputStream()), name -> script(scriptDir, name), scheduler).run();
		}
	}

	/** Compiles {@code name}.java from the source directory if it is there, and otherwise loads the class from the classpath. */
	private static NodeScript script(File dir, String name) {
		try {
			// Every node runs the same class, so it is compiled once, as when the nodes run in the simulator's JVM.
			Class<?> script = CLASSES.computeIfAbsent(name, n -> load(dir, n));
			return (NodeScript) script.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load student script " + name, e);
		}
	}

	private static Class<?> load(File dir, String name) {
		try {
			File source = dir == null ? null : new File(dir, name + ".java");
			return source != null && source.exists() ? new DynamicClassLoader().loadClass(source)
					: new ClasspathScriptLoader(name).loadClass(name);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load student script " + name, e);
		}
	}
}
