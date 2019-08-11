package net.alexheavens.cs4099.network;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.alexheavens.cs4099.network.configuration.ILinkConfig;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.LinkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfigException;
import net.alexheavens.cs4099.network.configuration.NetworkConfigFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.junit.Before;
import org.junit.Test;

public class NetworkConfigTest {

	private static final String TEMP_TEST_FILEPATH = "test/testconfigfiles/TestOutput.json";
	private static final int N_NODES = 100;
	private INetworkConfig netcon;
	private INetworkConfig fullCon;

	@Before
	public void setup() {
		netcon = new NetworkConfig();
		fullCon = new NetworkConfig(N_NODES);
	}

	@Test
	public void testNewNetworkConfig() {
		assertEquals(0, netcon.nodeCount());
		Collection<ILinkConfig> links = netcon.links();
		assertEquals(0, links.size());
	}

	@Test
	public void testCreateWithNodes() {
		assertEquals(N_NODES, fullCon.nodeCount());
		assertEquals(0, fullCon.links().size());
	}

	@Test
	public void testCreateInvalidNodes() {
		final int N_NODES = -1;
		try {
			new NetworkConfig(N_NODES);
			fail("Allowed to create a network config with an invalid number of nodes.");
		} catch (IllegalArgumentException e) {
			assertEquals(INetworkConfig.INVALID_NODES_MSG, e.getMessage());
		}
	}

	@Test
	public void testAddLink() {
		final int source = 5;
		final int target = 20;
		ILinkConfig linkComp = new LinkConfig(source, target, 1);
		ILinkConfig link = new LinkConfig(target, source, 1);
		assertFalse(fullCon.links().contains(linkComp));
		fullCon.addLink(link);
		assertTrue(fullCon.links().contains(linkComp));
	}

	@Test
	public void testAddLinkUnknownSource() {
		final int source = N_NODES + 1;
		final int target = 20;
		ILinkConfig link = new LinkConfig(source, target, 1);
		try {
			fullCon.addLink(link);
			fail("Able to add link with unknown source to config.");
		} catch (IllegalArgumentException e) {
			assertEquals(INetworkConfig.UNKNOWN_LINK_SOURCE_MSG, e.getMessage());
		}
	}

	@Test
	public void testAddLinkUnknownTarget() {
		final int target = N_NODES + 1;
		final int source = 20;
		ILinkConfig link = new LinkConfig(source, target, 1);
		try {
			fullCon.addLink(link);
			fail("Able to add link with unknown target to config.");
		} catch (IllegalArgumentException e) {
			assertEquals(INetworkConfig.UNKNOWN_LINK_TARGET_MSG, e.getMessage());
		}
	}

	@Test
	public void testAddLinkTwice() {
		final int target = 5;
		final int source = 20;
		ILinkConfig link = new LinkConfig(source, target, 1);
		fullCon.addLink(link);
		try {
			fullCon.addLink(link);
			fail("Able to add link twice.");
		} catch (IllegalArgumentException e) {
			assertEquals(INetworkConfig.DUP_LINK_MSG, e.getMessage());
		}
	}

	@Test
	public void testReadSimpleNetwork() throws JSONException, NetworkConfigException, IOException {

		File file = new File("test/testconfigfiles/SimpleNet.json");
		netcon.appendFromFile(file);

		Collection<ILinkConfig> links = netcon.links();

		assertEquals(6, netcon.nodeCount());
		assertEquals(6, links.size());

		assertTrue(links.contains(new LinkConfig(0, 1, 10)));
		assertTrue(links.contains(new LinkConfig(1, 2, 5)));
		assertTrue(links.contains(new LinkConfig(0, 3, 8)));
		assertTrue(links.contains(new LinkConfig(3, 4, 1)));
		assertTrue(links.contains(new LinkConfig(4, 5, 20)));
		assertTrue(links.contains(new LinkConfig(3, 1, 16)));
	}

	@Test
	public void testReadRubbishFile() {

		File file = new File("test/testconfigfiles/rubbish.txt");
		try {
			netcon.appendFromFile(file);
			fail("Did not throw JSONException.");
		} catch (JSONException e) {
		} catch (Exception e) {
			fail("Threw unexpected Exception: " + e.getLocalizedMessage());
		}

	}

	@Test
	public void testReadBadJSON() {

		File file = new File("test/testconfigfiles/bad.json");
		try {
			netcon.appendFromFile(file);
			fail("Did not throw JSONException.");
		} catch (JSONException e) {
		} catch (Exception e) {
			fail("Threw unexpected Exception: " + e.getLocalizedMessage());
		}
	}

	@Test(expected = NetworkConfigException.class)
	public void testReadRepLinkConfig() throws JSONException,
			NetworkConfigException, IOException {
		File file = new File("test/testconfigfiles/RepLinkNet.json");
		netcon.appendFromFile(file);

	}

	@Test(expected = NetworkConfigException.class)
	public void testReadRepRevLinkConfig() throws JSONException,
			NetworkConfigException, IOException {
		File file = new File("test/testconfigfiles/RepRevLinkNet.json");
		netcon.appendFromFile(file);
	}

	@Test
	public void testWriteDuplicateNetwork() {

		File from = new File("test/testconfigfiles/SimpleNet.json");
		File to = new File(TEMP_TEST_FILEPATH);
		try {
			netcon.appendFromFile(from);
		} catch (Exception e) {
			fail("Exception occured while reading in test network: "
					+ e.getLocalizedMessage());
		}

		try {
			netcon.writeToFile(to);
		} catch (Exception e) {
			fail("Unable to write network to file: " + e.getLocalizedMessage());
		}

		INetworkConfig newNet = new NetworkConfig();
		try {
			newNet.appendFromFile(to);
		} catch (Exception e) {
			fail("Unable to read from created network file: "
					+ e.getLocalizedMessage());
		}

		assertTrue(netcon.links().equals(newNet.links()));
		assertEquals(netcon.nodeCount(), newNet.nodeCount());
	}

	@Test
	public void testWriteBadFile() {

		File badFile = new File("/usr/badfilelocation.json");
		try {
			netcon.writeToFile(badFile);
			fail("Able to write to bad file location.");
		} catch (IOException e) {
		}
	}

	@Test
	/**
	 * Tests that writing the <code>NetworkConfig</code> to a JSON version
	 * retains the right information.
	 */
	public void testWriteToJSON() {
		final int NODES = 10;
		NetworkConfigFactory netFact = new NetworkConfigFactory();
		INetworkConfig net = netFact.createCompleteGraph(NODES);

		JSONObject netJSON = net.toJSONObject();

		assertNotNull(netJSON.getInt("nodes"));
		assertEquals(NODES, netJSON.getInt("nodes"));
		JSONArray edgeList = netJSON.getJSONArray("edges");

		// Check that we have the expected number of edges.
		assertNotNull(edgeList);
		final int expectedEdgeCount = NODES * (NODES - 1) / 2;
		assertEquals(expectedEdgeCount, edgeList.size());

		for (int i = 0; i < NODES; i++) {
			for (int j = 0; j < NODES; j++) {

				// Check that all distinct nodes have an edge between them.
				if (i != j) {
					JSONArray edge1 = new JSONArray();
					edge1.add(0, i);
					edge1.add(1, j);
					edge1.add(2, 1);
					JSONArray edge2 = new JSONArray();
					edge2.add(0, j);
					edge2.add(1, i);
					edge2.add(2, 1);
					assertTrue(edgeList.contains(edge1)
							|| edgeList.contains(edge2));
					assertFalse(edgeList.contains(edge1)
							&& edgeList.contains(edge2));
				}
			}
		}
	}

	@Test
	/**
	 * Test that the addition of valid node death events is reflected in the
	 * file.
	 */
	public void testSetNodeDeathEventsValid() {

		// Generate a list of node death mappings.
		final Map<Integer, Long> deathMap = new HashMap<Integer, Long>(N_NODES);
		for (int nodeId = 0; nodeId < N_NODES; nodeId++) {
			deathMap.put(nodeId, (long) nodeId);
		}

		netcon = new NetworkConfig(N_NODES);
		netcon.setNodeDeathEvents(deathMap);
		final JSONObject netJson = netcon.toJSONObject();

		// Check that the map was correctly stored.
		assertTrue(netJson.containsKey(INetworkConfig.DEATH_EVENTS_TAG));
		final JSONObject jsonMap = netJson
				.getJSONObject(INetworkConfig.DEATH_EVENTS_TAG);
		final Map<Integer, Long> storedDeathMap = new HashMap<Integer, Long>(
				N_NODES);
		@SuppressWarnings("unchecked")
		final Iterator<String> keyIt = jsonMap.keys();
		while (keyIt.hasNext()) {
			String nextKey = keyIt.next();
			storedDeathMap.put(new Integer(nextKey), jsonMap.getLong(nextKey));
		}
		assertEquals(deathMap, storedDeathMap);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that node values below zero are not accepted as death events.
	 */
	public void testSetNodeDeathEventsInvalidId() {

		final Map<Integer, Long> deathMap = new HashMap<Integer, Long>(N_NODES);
		deathMap.put(-1, 435l);

		netcon = new NetworkConfig(N_NODES);
		netcon.setNodeDeathEvents(deathMap);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot pass the ID of node we have not added to the config.
	 */
	public void testSetNodeDeathEventsUnknownNodeId() {
		final Map<Integer, Long> deathMap = new HashMap<Integer, Long>(N_NODES);
		deathMap.put(N_NODES, 435l);

		netcon = new NetworkConfig(N_NODES);
		netcon.setNodeDeathEvents(deathMap);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot pass a negative timestep as the timestep of death for
	 * a node.
	 */
	public void testSetDeathEventsInvalidTimestep() {
		final Map<Integer, Long> deathMap = new HashMap<Integer, Long>(N_NODES);
		deathMap.put(N_NODES - 1, -1l);

		netcon = new NetworkConfig(N_NODES);
		netcon.setNodeDeathEvents(deathMap);
	}

	@Test
	/**
	 * Test that the writing and reading of the network config file correctly
	 * include the node death events.
	 */
	public void testWriteReadEventListToFile() throws JSONException,
			NetworkConfigException, IOException, InterruptedException {
		// Generate a list of node death mappings.
		final Map<Integer, Long> deathMap = new HashMap<Integer, Long>(N_NODES);
		for (int nodeId = 0; nodeId < N_NODES; nodeId++) {
			deathMap.put(nodeId, (long) nodeId);
		}

		final File testFile = new File(
				"test/testconfigfiles/eventtestnet.json");
		netcon = new NetworkConfig(N_NODES);
		netcon.setNodeDeathEvents(deathMap);
		testFile.delete();
		netcon.writeToFile(testFile);

		final NetworkConfig readConfig = new NetworkConfig(testFile);
		assertEquals(deathMap, readConfig.getNodeDeathEvents());
		//testFile.delete();
	}
}
