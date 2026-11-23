package net.alexheavens.cs4099.child;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentHashMap;

import net.alexheavens.cs4099.scheduler.ContinuationScheduler;
import net.alexheavens.cs4099.usercode.DynamicClassLoader;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * Entry point of the child JVM (ADR 0002). {@link ChildJvm} starts it in a container with the Tascalate agent. The
 * protocol runs over standard input and output, because the container has no network. {@code System.out} is pointed at
 * standard error first, so that student output cannot corrupt the frames. The child then runs {@link NodeHost} until
 * the simulator closes its input.
 * <p>
 * Arguments: worker count, time slice in safe-point ticks, directory holding the student sources ({@code -} for
 * none).
 */
public final class ChildMain {

	/** Passed as the source directory when scripts are classes on the classpath. */
	static final String NO_SCRIPT_DIR = "-";

	private static final ConcurrentHashMap<String, Class<?>> CLASSES = new ConcurrentHashMap<>();

	private ChildMain() {
	}

	public static void main(String[] args) throws Exception {
		int workers = Integer.parseInt(args[0]);
		int sliceTicks = Integer.parseInt(args[1]);
		File scriptDir = args[2].equals(NO_SCRIPT_DIR) ? null : new File(args[2]);
		// The host writes a frame as several small writes with no flush, so the stream is not buffered.
		DataOutputStream out = new DataOutputStream(new FileOutputStream(FileDescriptor.out));
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(FileDescriptor.in)));
		System.setOut(System.err);
		try (ContinuationScheduler scheduler = new ContinuationScheduler(workers, sliceTicks)) {
			out.writeInt(ChildJvm.READY);
			out.flush();
			new NodeHost(in, out, name -> script(scriptDir, name), scheduler).run();
		}
	}

	/** Compiles {@code name}.java from the source directory if it is there, and otherwise loads the class from the classpath. */
	private static NodeScript script(File dir, String name) {
		try {
			// Every node runs the same class, so it is compiled once, as when the nodes run in the simulator's JVM.
			Class<?> script = CLASSES.computeIfAbsent(name, n -> load(dir, n));
			return (NodeScript) script.getDeclaredConstructor().newInstance();
		} catch (IllegalStateException e) {
			throw e;
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
			// The failure frame carries only this exception's text, so the cause's message goes in it.
			throw new IllegalStateException("Cannot load student script " + name + ": " + e.getMessage(), e);
		}
	}
}
