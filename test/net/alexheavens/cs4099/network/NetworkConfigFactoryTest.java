package net.alexheavens.cs4099.network;

import static org.junit.Assert.*;

import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.INetworkConfigFactory;
import net.alexheavens.cs4099.network.configuration.NetworkConfigFactory;

import org.junit.Test;
import org.junit.Before;

import java.util.Iterator;

public class NetworkConfigFactoryTest {

	private INetworkConfigFactory netFact;

	@Before
	public void setup() {
		netFact = new NetworkConfigFactory();
	}

	@Test
	public void testCreateCompleteGraph() {

		// Define input.
		final int N_NODES = 128;

		// Define expected output.
		final int expectedNodeCount = N_NODES;
		final int expectedLinkCount = N_NODES * (N_NODES - 1) / 2;

		// Run test code.
		INetworkConfig testNet = null;
		testNet = netFact.createCompleteGraph(N_NODES);

		// Run tests.
		assertEquals(expectedNodeCount, testNet.nodeCount());
		assertEquals(expectedLinkCount, testNet.linkCount());

		// Assume network has not repeat copied nodes or links.
	}

	@Test
	public void testCreateCompleteGraphInvalid() {

		// Define input.
		final int N_NODES = 0;

		try {
			netFact.createCompleteGraph(N_NODES);
			fail("Able to create complete graph with " + N_NODES + " nodes.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testCreateRegularTreeNetwork() {

		final int N_GENERATIONS = 4;
		final int N_CHILDPERPAR = 5;

		INetworkConfig treeCon = null;
		treeCon = netFact.createTreeNetwork(N_GENERATIONS, N_CHILDPERPAR);

		final int expNodes = 156; // f(g,c) = f(g - 1, c) + (g - 1)^c
		final int expLinks = expNodes - 1;

		assertEquals(expNodes, treeCon.nodeCount());
		assertEquals(expLinks, treeCon.linkCount());

		// We can address links more easily as a Network.
		Network tree = null;
		try {
			tree = new Network(treeCon, MockUserNode.class);
		} catch (Exception e) {
			fail("Exception drawing network from config: "
					+ e.getLocalizedMessage());
		}

		// Check that the number of neighbours matches up, also finding the
		// centre of the tree.
		INodeImpl centreNode = null;
		for (INodeImpl node : tree.nodes()) {
			switch (node.neighbourCount()) {
			case N_CHILDPERPAR: // Centre Node
				if (centreNode != null)
					fail("Expected only one node (the centre node) to have "
							+ N_CHILDPERPAR
							+ " neighbours, but more than one does.");
				else
					centreNode = node;
				break;
			case N_CHILDPERPAR + 1: // Other parents.
				break;
			case 1: // Leaf nodes.
				break;
			default:
				fail("Node has an unexpected number of neighbours: "
						+ node.neighbourCount());
			}
		}

		// Check that the centre of the tree is the right length from all
		// leaves.
		for (Iterator<INode> it = centreNode.neighbours(); it.hasNext();) {
			INode node = it.next();
			testLeafDistance(N_GENERATIONS - 1, node, centreNode);
		}
	}

	private void testLeafDistance(int remLinks, INode node, INode from) {
		if (remLinks == 0 && node.neighbourCount() != 1) {
			fail("Distance from the centre of the tree is not uniform.");
		} else {
			for (Iterator<INode> it = node.neighbours(); it.hasNext();) {
				INode next = it.next();
				if (next != from)
					testLeafDistance(remLinks - 1, next, node);
			}
		}
	}

	@Test
	public void testTreeNetworkInvalidGen() {

		try {
			netFact.createTreeNetwork(0, 5);
			fail("Able to create tree network with less than one generation.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testTreeNetworkInvalidChildren() {

		try {
			netFact.createTreeNetwork(5, 0);
			fail("Able to create tree network with less than one child per parent.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testTreeNetworkSingleGen() {

		INetworkConfig net = null;
		net = netFact.createTreeNetwork(1, 0);

		assertEquals(1, net.nodeCount());
		assertEquals(0, net.linkCount());
	}

	@Test
	public void testTreeNetworkSingleGenInvalidChildren() {

		try {
			netFact.createTreeNetwork(1, 50);
			fail("Able to create single node tree network when children isn't zero.");
		} catch (IllegalArgumentException e) {
		}
	}
}
