package net.alexheavens.cs4099.child;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.alexheavens.cs4099.ApplicationController;
import net.alexheavens.cs4099.examples.TreeLeaderNode;
import net.alexheavens.cs4099.network.Network;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfigFactory;
import net.alexheavens.cs4099.simulation.SimulationResults;
import net.alexheavens.cs4099.simulation.SimulationRunner;
import net.alexheavens.cs4099.usercode.ClassLimitation;
import net.alexheavens.cs4099.usercode.DynamicClassLoader;
import net.alexheavens.cs4099.usercode.NodeScript;

/** Runs the same simulation with the student nodes in this JVM and in a child JVM, and compares the two. */
public class ChildModeSimulationTest {

	private static final long LENGTH = 200;
	private static final long TIMEOUT_NANOS = Duration.ofSeconds(1).toNanos();
	private static final Path TREE_LEADER = Path
			.of("test/node_scripts/net/alexheavens/cs4099/examples/TreeLeaderNode.java");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/** The example script without its package line, because the dynamic loader names a class after its file. */
	private File treeLeaderSource() throws Exception {
		String source = Files.readString(TREE_LEADER).replaceFirst("(?m)^package .*;$", "");
		File file = folder.newFile("TreeLeaderNode.java");
		Files.writeString(file.toPath(), source);
		return file;
	}

	private ChildNodes startChild() throws Exception {
		return new ChildNodes(new ChildJvm.Config(Path.of(System.getProperty("javaflow.agent")),
				folder.getRoot().toPath(), "256m", 2, 1000, Duration.ofSeconds(1), Duration.ofSeconds(60)));
	}

	private void source(String name, String body) throws Exception {
		Files.writeString(new File(folder.getRoot(), name + ".java").toPath(), """
				public class %s extends net.alexheavens.cs4099.usercode.NodeScript {
					public void execute() {
						%s
					}
				}
				""".formatted(name, body));
	}

	private SimulationRunner runnerOf(ChildNodes children, String script) {
		INetworkConfig config = new NetworkConfigFactory().createTreeNetwork(2, 2);
		return new SimulationRunner(new Network(children, script, config, false), LENGTH, null, TIMEOUT_NANOS);
	}

	@SuppressWarnings("unchecked")
	private SimulationResults inThisJvm(File source, INetworkConfig config) throws Exception {
		Class<? extends NodeScript> script = (Class<? extends NodeScript>) new DynamicClassLoader().loadClass(source);
		return new SimulationRunner(new Network(script, config, false), LENGTH).simulate();
	}

	private SimulationResults inChildJvm(INetworkConfig config) throws Exception {
		try (ChildNodes children = startChild()) {
			return new SimulationRunner(new Network(children, "TreeLeaderNode", config, false), LENGTH).simulate();
		}
	}

	/** The number of events of each type, which does not depend on the order of events within a timestep. */
	private Map<String, Long> eventCounts(SimulationResults results) {
		JSONArray events = results.getEvents().toJSONObject().getJSONArray("events");
		Map<String, Long> counts = new TreeMap<>();
		for (int i = 0; i < events.size(); i++)
			counts.merge(events.getJSONObject(i).getString("eventType"), 1L, Long::sum);
		return counts;
	}

	/** The data of every message with the tag, sorted. */
	private List<Object> messageData(SimulationResults results, String tag) {
		JSONArray messages = results.getEvents().toJSONObject().getJSONArray("messages");
		List<Object> data = new ArrayList<>();
		for (int i = 0; i < messages.size(); i++)
			if (tag.equals(messages.getJSONObject(i).getString("tag")))
				data.add(messages.getJSONObject(i).get("data"));
		return data;
	}

	/**
	 * Compares what the algorithm decides, not the exact event log: the tree's root treats whichever child's message
	 * it reads last as its parent, and which that is depends on the order in which messages that arrive in the same
	 * timestep are processed. That order varies between runs in either mode.
	 */
	@Test(timeout = 180000)
	public void testTreeLeaderDecidesTheSameInBothModes() throws Exception {
		INetworkConfig config = new NetworkConfigFactory().createTreeNetwork(6, 3);
		File source = treeLeaderSource();

		SimulationResults thread = inThisJvm(source, config);
		SimulationResults child = inChildJvm(config);

		Map<String, Long> threadCounts = eventCounts(thread);
		assertFalse(threadCounts.isEmpty());
		assertEquals(threadCounts, eventCounts(child));
		List<Object> leaders = messageData(child, "LEADER");
		assertFalse(leaders.isEmpty());
		assertEquals(messageData(thread, "LEADER"), leaders);
		// The leader is the highest machine ID, and every node is told so.
		assertEquals(Set.of(363), new HashSet<>(leaders));
	}

	@Test(timeout = 180000)
	public void testSpinningNodeTimesOutAndTheRunFinishes() throws Exception {
		source("Spinner", "setColour(java.awt.Color.RED); while (true) { }");
		try (ChildNodes children = startChild()) {
			SimulationResults results = runnerOf(children, "Spinner").simulate();
			assertNull(children.failure());
			assertTrue(results.getEvents().toJSONObject().toString().contains("Node timeout"));
		}
	}

	@Test(timeout = 180000)
	public void testNodeThatCannotBeKilledFailsTheRunNamingTheNode() throws Exception {
		source("Stuck", """
				setColour(java.awt.Color.RED);
				try { new java.util.concurrent.CountDownLatch(1).await(); } catch (InterruptedException e) { }
				""");
		try (ChildNodes children = startChild()) {
			try {
				runnerOf(children, "Stuck").simulate();
				fail("The run should have failed");
			} catch (IllegalStateException e) {
				assertTrue(e.getMessage(), e.getMessage().matches("(?s).*Node \\d+ did not stop.*"));
			}
			assertTrue(children.failure().node() >= 0);
		}
	}

	@Test(timeout = 180000)
	public void testControllerRunsNodesInChildJvmWhenFlagged() throws Exception {
		File output = folder.newFile("results.json");
		System.setProperty(ChildNodes.ENABLED_PROPERTY, "true");
		try {
			new ApplicationController().simulate(new NetworkConfigFactory().createTreeNetwork(4, 3), LENGTH,
					TreeLeaderNode.class, new ClassLimitation(true, true), output, 0, false);
		} finally {
			System.clearProperty(ChildNodes.ENABLED_PROPERTY);
		}
		JSONObject results = JSONObject.fromObject(Files.readString(output.toPath()));
		assertTrue(results.getJSONArray("events").size() > 0);
		// 1 + 3 + 9 + 27 nodes, so the highest machine ID is 39.
		JSONArray messages = results.getJSONArray("messages");
		Set<Object> leaders = new HashSet<>();
		for (int i = 0; i < messages.size(); i++)
			if ("LEADER".equals(messages.getJSONObject(i).getString("tag")))
				leaders.add(messages.getJSONObject(i).get("data"));
		assertEquals(Set.of(39), leaders);
	}
}
