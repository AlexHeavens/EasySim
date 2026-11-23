package net.alexheavens.cs4099.child;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.alexheavens.cs4099.network.Node;

/**
 * The child JVM for one simulation, and the simulator's nodes that stand in for the student nodes inside it
 * (ADR 0002). Close it when the simulation is over.
 */
public final class ChildNodes implements AutoCloseable {

	/** Set this system property to {@code true} to run student nodes in a child JVM. */
	public static final String ENABLED_PROPERTY = "easysim.child";
	/** The system property that holds the path of the Tascalate agent jar. */
	public static final String AGENT_PROPERTY = "javaflow.agent";
	/** The system property that sets the child's heap cap, as an {@code -Xmx} value. */
	public static final String HEAP_PROPERTY = "easysim.child.heap";

	private static final String DEFAULT_HEAP = "512m";
	private static final int SLICE_TICKS = 1000;
	private static final Duration KILL_GRACE = Duration.ofSeconds(5);
	private static final Duration STARTUP_WAIT = Duration.ofSeconds(60);

	private final ChildJvm child;
	private final List<ChildScript> scripts = new CopyOnWriteArrayList<>();

	public ChildNodes(ChildJvm.Config config) throws IOException {
		child = new ChildJvm(config, this::fail);
	}

	/**
	 * Starts a child JVM if the system property {@value #ENABLED_PROPERTY} is {@code true}, and returns null if not.
	 *
	 * @param scriptDir the folder of the student sources, or null if the script is a class on the classpath
	 */
	public static ChildNodes ifEnabled(Path scriptDir) {
		if (!Boolean.getBoolean(ENABLED_PROPERTY))
			return null;
		String agent = System.getProperty(AGENT_PROPERTY);
		if (agent == null)
			throw new IllegalStateException("Set the system property " + AGENT_PROPERTY
					+ " to the path of the Tascalate agent jar to run nodes in a child JVM.");
		try {
			return new ChildNodes(new ChildJvm.Config(Path.of(agent), scriptDir,
					System.getProperty(HEAP_PROPERTY, DEFAULT_HEAP), Runtime.getRuntime().availableProcessors(),
					SLICE_TICKS, KILL_GRACE, STARTUP_WAIT));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** Creates a node that runs the student script {@code scriptClass} in the child JVM. */
	public Node node(String scriptClass, boolean initiator) {
		ChildScript script = new ChildScript(child, scriptClass);
		Node node = new Node(script, initiator);
		script.bind(node);
		scripts.add(script);
		return node;
	}

	/**
	 * Waits for every node that was told to stop to end, once the simulation has halted its nodes. A node that cannot
	 * be stopped fails the run, and {@link #failure()} then names it.
	 */
	public void settle() {
		try {
			for (ChildScript script : scripts)
				script.awaitExit();
			child.awaitKills();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** The failure that ended the run, or null while it has not failed. */
	public ChildJvm.Failure failure() {
		return child.failure();
	}

	private void fail(ChildJvm.Failure failure) {
		for (ChildScript script : scripts)
			script.fail(failure);
	}

	@Override
	public void close() {
		child.close();
	}
}
