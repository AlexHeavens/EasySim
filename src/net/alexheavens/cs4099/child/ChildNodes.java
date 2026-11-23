package net.alexheavens.cs4099.child;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.alexheavens.cs4099.network.Node;

/**
 * The child JVM for one simulation, and the simulator's nodes that stand in for the student nodes inside it
 * (ADR 0002). Close it when the simulation is over.
 */
public final class ChildNodes implements AutoCloseable {

	/** Optional system property holding the path of a Tascalate agent jar to use instead of the bundled one. */
	public static final String AGENT_PROPERTY = "javaflow.agent";
	/** The system property that sets the child's heap cap, in megabytes. */
	public static final String HEAP_PROPERTY = "easysim.child.heap";

	/** The agent jar that the build bundles next to this class. */
	private static final String BUNDLED_AGENT = "javaflow-agent.jar";
	private static final int DEFAULT_HEAP_MEGABYTES = 512;
	private static final int SLICE_TICKS = 1000;
	private static final Duration KILL_GRACE = Duration.ofSeconds(5);
	private static final Duration STARTUP_WAIT = Duration.ofSeconds(300);

	private final ChildJvm child;
	private final List<ChildScript> scripts = new CopyOnWriteArrayList<>();

	public ChildNodes(ChildJvm.Config config) throws IOException {
		child = new ChildJvm(config, this::fail);
	}

	/**
	 * Starts a child JVM for one simulation.
	 *
	 * @param scriptDir the folder of the student sources, or null if the script is a class on the classpath
	 */
	public static ChildNodes start(Path scriptDir) {
		try {
			return new ChildNodes(new ChildJvm.Config(agent(), scriptDir,
					Integer.getInteger(HEAP_PROPERTY, DEFAULT_HEAP_MEGABYTES), Runtime.getRuntime().availableProcessors(),
					SLICE_TICKS, KILL_GRACE, STARTUP_WAIT));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The agent jar named by {@value #AGENT_PROPERTY}, or else the bundled one, copied to a temporary file. */
	private static Path agent() throws IOException {
		String override = System.getProperty(AGENT_PROPERTY);
		if (override != null)
			return Path.of(override);
		try (InputStream bundled = ChildNodes.class.getResourceAsStream(BUNDLED_AGENT)) {
			if (bundled == null)
				throw new IllegalStateException("The Tascalate agent jar is not bundled. Set the system property "
						+ AGENT_PROPERTY + " to its path.");
			Path agent = Files.createTempFile("easysim-agent", ".jar");
			agent.toFile().deleteOnExit();
			Files.copy(bundled, agent, StandardCopyOption.REPLACE_EXISTING);
			return agent;
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
