package net.alexheavens.cs4099.network;

import static org.junit.Assert.*;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

public class NetworkTest {

	private INetwork testNet;
	private Node[] testNodes;
	private static final int N_TESTNODES = 128;

	@Before
	public void setup() throws InstantiationException, IllegalAccessException {
		testNet = new Network(new NetworkConfig(), MockUserNode.class);
		testNodes = new Node[N_TESTNODES];

		for (int i = 0; i < testNodes.length; i++) {
			testNodes[i] = new MockUserNode();
			testNet.addNode(testNodes[i], 0);
		}
	}

	@Test
	public void testNetworkCreationValid() {

		// Test that our nodes are there.
		assertEquals(N_TESTNODES, testNet.nodeCount());
		for (int i = 0; i < testNodes.length; i++) {
			assertTrue(testNet.nodes().contains(testNodes[i]));
			assertEquals(i, testNodes[i].getSimulationId());
		}

		// Test that link insertion works.
		final int N_LINKS = N_TESTNODES * (N_TESTNODES - 1) / 2;
		for (INodeImpl node : testNodes) {

			for (INodeImpl neighbour : testNodes) {

				if (node != neighbour && !node.isNeighbour(neighbour)) {
					testNet.createLink(node, neighbour, 1);
				}
			}
		}

		assertEquals(N_LINKS, testNet.linkCount());

		for (INodeImpl node : testNodes) {

			for (INodeImpl neighbour : testNodes) {

				if (node != neighbour) {
					assertTrue(node.isNeighbour(neighbour));
					assertTrue(neighbour.isNeighbour(node));
				}
			}
		}
	}

	@Test
	public void testAddNodeTwice() {
		try {
			testNet.addNode(testNodes[0], 0);
		} catch (IllegalArgumentException e) {
			assertEquals(INetwork.DUP_NODE_MSG, e.getMessage());
		}
	}

	@Test
	public void testAddInitialisedNode() {
		try {
			MockUserNode testNode = new MockUserNode();
			testNode.setSimulationId(N_TESTNODES, 0);
			testNet.addNode(testNode, 0);
			fail("Able to add initialised Node to Network.");
		} catch (IllegalArgumentException e) {
			assertEquals(INetwork.INVALID_NODE_MSG, e.getMessage());
		}
	}

	@Test
	public void testLinkUnknownTarget() {
		try {
			testNet.createLink(testNodes[0], new MockUserNode(), 1);
			fail("Able to create link to non-graph target.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testLinkUnknownSource() {
		try {
			testNet.createLink(new MockUserNode(), testNodes[0], 1);
			fail("Able to create link from non-graph source.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testLinkUnknownSourceAndTarget() {
		try {
			testNet.createLink(new MockUserNode(), new MockUserNode(), 1);
			fail("Able to create link from non-graph source to non-graph target.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testLinkNullTarget() {
		try {
			testNet.createLink(testNodes[0], null, 1);
			fail("Able to create link to null target.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testLinkNullSource() {
		try {
			testNet.createLink(null, testNodes[0], 1);
			fail("Able to create link from null source.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testLinkNullSourceAndTarget() {
		try {
			testNet.createLink(null, null, 1);
			fail("Able to create link from null source to null target.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testLinkAgain() {
		testNet.createLink(testNodes[0], testNodes[1], 1);
		try {
			testNet.createLink(testNodes[0], testNodes[1], 1);
			fail("Able to re-create a link.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testNetworkCreationFromConfig() {
		INetworkConfig netCon = new NetworkConfig();
		File conFile = new File("test/testconfigfiles/SimpleNet.json");
		try {
			netCon.appendFromFile(conFile);
		} catch (Exception e) {
			fail("Exception reading config from file: "
					+ e.getLocalizedMessage());
		}

		try {
			testNet = new Network(netCon, MockUserNode.class);
		} catch (Exception e) {
			fail("Exception in Network creation: " + e.getLocalizedMessage());
		}
		assertEquals(6, testNet.nodeCount());
		assertEquals(6, testNet.linkCount());

		HashSet<ILink> seenLinks = new HashSet<ILink>();

		for (ILink link : testNet.links()) {
			assertFalse(seenLinks.contains(link));
			assertEquals(link, link.getTarget().neighbourLink(link.getSource()));
			assertEquals(link, link.getSource().neighbourLink(link.getTarget()));
			assertTrue(link.getTarget().isNeighbour(link.getSource()));
			assertTrue(link.getSource().isNeighbour(link.getTarget()));
			seenLinks.add(link);
		}

		for (INode node : testNet.nodes()) {
			switch (node.getSimulationId()) {
			case 0:
				for (Iterator<INode> it = node.neighbours(); it.hasNext();) {
					INode neighbour = it.next();
					assertTrue(neighbour.getSimulationId() == 1
							|| neighbour.getSimulationId() == 3);
				}
				break;
			case 1:
				for (Iterator<INode> it = node.neighbours(); it.hasNext();) {
					INode neighbour = it.next();
					assertTrue(neighbour.getSimulationId() == 0
							|| neighbour.getSimulationId() == 2
							|| neighbour.getSimulationId() == 3);
				}
				break;
			case 2:
				for (Iterator<INode> it = node.neighbours(); it.hasNext();) {
					INode neighbour = it.next();
					assertTrue(neighbour.getSimulationId() == 1);
				}
				break;
			case 3:
				for (Iterator<INode> it = node.neighbours(); it.hasNext();) {
					INode neighbour = it.next();
					assertTrue(neighbour.getSimulationId() == 0
							|| neighbour.getSimulationId() == 1
							|| neighbour.getSimulationId() == 4);
				}
				break;
			case 4:
				for (Iterator<INode> it = node.neighbours(); it.hasNext();) {
					INode neighbour = it.next();
					assertTrue(neighbour.getSimulationId() == 5
							|| neighbour.getSimulationId() == 3);
				}
				break;
			case 5:
				for (Iterator<INode> it = node.neighbours(); it.hasNext();) {
					INode neighbour = it.next();
					assertTrue(neighbour.getSimulationId() == 4);
				}
				break;
			default:
				fail("Unknown node with machine id " + node.getSimulationId()
						+ ".");
			}
		}

	}
}
