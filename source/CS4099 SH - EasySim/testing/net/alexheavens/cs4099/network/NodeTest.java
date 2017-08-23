package net.alexheavens.cs4099.network;

import static org.junit.Assert.*;
import net.alexheavens.cs4099.concurrent.LeaderBarrier;
import net.alexheavens.cs4099.simulation.ColourChangeEvent;
import net.alexheavens.cs4099.simulation.ISimulationEvent;
import net.alexheavens.cs4099.simulation.MockSimEvent;
import net.alexheavens.cs4099.simulation.NodePauseEvent;
import net.alexheavens.cs4099.simulation.SimulationProfiler;
import net.alexheavens.cs4099.simulation.SimulationRuntimeException;
import net.alexheavens.cs4099.simulation.SimulationState;

import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NodeTest {

	private static final int N_NEIGHBOURS = 20;
	private MockUserNode testNode;
	private MockUserNode[] otherNodes;
	private LeaderBarrier leaderBarrier;
	private MockSimObserver observer;
	private SimulationProfiler simProfiler;

	@Before
	public void setup() {

		// Create a test node and some test neighbours.
		int nextId = 0;
		testNode = new MockUserNode(nextId++);
		otherNodes = new MockUserNode[N_NEIGHBOURS];
		for (int i = 0; i < N_NEIGHBOURS; i++) {
			otherNodes[i] = new MockUserNode();
			otherNodes[i].setSimulationId(nextId++, 0);
		}

		observer = new MockSimObserver();
		testNode.addObserver(observer);
		leaderBarrier = new LeaderBarrier(Thread.currentThread(), nextId);
		simProfiler = new SimulationProfiler(nextId);
	}

	@Test
	public void testCreateNode() {

		INodeImpl testLocalNode = new MockUserNode();

		// Test that the node is empty.
		assertEquals(INodeImpl.INIT_MACHINE_ID, testLocalNode.getSimulationId());
		assertEquals(0, testLocalNode.neighbourCount());
		Iterator<INode> it = testLocalNode.neighbours();
		assertFalse(it.hasNext());
	}

	@Test
	public void testAddNeighbour() {

		// Add neighbours to the test node, testing how many are added.
		for (int i = 0; i < otherNodes.length; i++) {
			testNode.addNeighbour(otherNodes[i], 1);
			assertEquals(i + 1, testNode.neighbourCount());
		}

		// Test that all the neighbours are contained.
		Iterator<INode> it = testNode.neighbours();
		Set<INode> addedNodes = new HashSet<INode>();
		for (INode n : otherNodes)
			addedNodes.add(n);

		while (it.hasNext()) {
			INode n = it.next();
			assertTrue(addedNodes.contains(n));
		}

		// Test that the structure of the nodes and edges is complete.
		for (INodeImpl neighbour : otherNodes) {
			ILink createdLink = testNode.neighbourLink(neighbour);
			assertEquals(createdLink, testNode.neighbourLink(neighbour));
			assertEquals(testNode, createdLink.getSource());
			assertEquals(neighbour, createdLink.getTarget());
			assertEquals(1, neighbour.neighbourCount());
		}
	}

	@Test
	public void testAddNeighbourNull() {
		try {
			testNode.addNeighbour(null, 1);
			fail("Added null neighbour.");
		} catch (IllegalArgumentException e) {
			assertEquals(INodeImpl.NULL_NEIGHBOUR_MSG, e.getMessage());
		}
	}

	@Test
	public void testAddNeighbourExisting() {
		INodeImpl neighbour = new MockUserNode();
		neighbour.setSimulationId(N_NEIGHBOURS, 0);
		testNode.addNeighbour(neighbour, 1);
		try {
			testNode.addNeighbour(neighbour, 1);
			fail("Added existing neighbour.");
		} catch (IllegalArgumentException e) {
			assertEquals(INodeImpl.DUP_NEIGHBOUR_MSG, e.getMessage());
		}
	}

	@Test
	public void testAddUnitialisedNeighbour() {

		try {
			testNode.addNeighbour(new MockUserNode(), 1);
			fail("Added uninitialised neighbour.");
		} catch (IllegalArgumentException e) {
			assertEquals(INodeImpl.UNINIT_NEIGHBOUR_MSG, e.getMessage());
		}
	}

	@Test
	public void testSetInvalidId() {
		try {
			MockUserNode testLocalNode = new MockUserNode();
			testLocalNode.setSimulationId(INodeImpl.INIT_MACHINE_ID - 1, 0);
			fail("Allowed invalid ID.");
		} catch (IllegalArgumentException e) {
			assertEquals(INodeImpl.INVALID_ID_MSG, e.getMessage());
		}
	}

	@Test
	public void testSetIdTwice() {

		try {
			MockUserNode testLocalNode = new MockUserNode();
			testLocalNode.setSimulationId(0, 0);
			testLocalNode.setSimulationId(1, 0);
			fail("Allowed machine ID to be set twice.");
		} catch (IllegalStateException e) {
			assertEquals(INodeImpl.ID_SET_TWICE_MSG, e.getMessage());
		}
	}

	@Test(timeout = 500)
	public void testSimpleSimulate() {

		assertEquals(0, testNode.executedCount());
		assertEquals(0, testNode.setupCount());
		assertEquals(SimulationState.PRE_SIMULATION,
				testNode.getSimulationState());

		testNode.simulate(leaderBarrier, simProfiler);

		while (testNode.getSimulationState() != SimulationState.COMPLETED) {
		}

		assertEquals(1, testNode.setupCount());
		assertEquals(1, testNode.executedCount());
	}

	@Test
	/**
	 * Tests that an event caused within a <code>Node</code> is correctly passed
	 * to an observer.
	 * 
	 * This is needed to ensure easy scheduling of
	 * <code>ISimulationEvents</code> with an <code>IEventController</code>.
	 */
	public void testRaiseEvent() {

		// Create an observer and prepare to update it with an event.
		MockSimEvent event = new MockSimEvent(ISimulationEvent.CURRENT_TIMESTEP);
		MockSimObserver observer = new MockSimObserver();
		testNode.addObserver(observer);

		// Check that no event has been observed yet.
		assertEquals(0, observer.updateCount());
		assertNull(observer.lastObservable());
		assertNull(observer.lastUpdate());

		testNode.raiseEvent(event);

		// Check that the event was observed.
		assertEquals(1, observer.updateCount());
		assertEquals(testNode, observer.lastObservable());
		assertEquals(event, observer.lastUpdate());
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Tests if raising a null event correctly causes an
	 * IllegalArgumentException.
	 * 
	 * Nodes are not exptected to raise null events, which cannot be handled by
	 * EventControllers.
	 */
	public void testRaiseNullEvent() {

		// Create an observer and prepare to update it with an event.
		MockSimObserver observer = new MockSimObserver();
		testNode.addObserver(observer);

		testNode.raiseEvent(null);

	}

	@Test(timeout = 500)
	/**
	 * Test that a pause in user code does cause a pause of the user thread and
	 * the generation of an appropriate pause event.
	 */
	public void testNodePause() {

		// Create a node that pauses just once.
		final long pauseTime = 20;
		MockPauseNode pauseNode = new MockPauseNode(0, pauseTime, 1);

		// Create an observer to test that the right event is raised.
		MockSimObserver observer = new MockSimObserver();
		pauseNode.addObserver(observer);

		pauseNode.simulate(leaderBarrier, simProfiler);

		// Wait for simulation to stop.
		while (!leaderBarrier.containsThread(pauseNode.getThread())) {
		}

		// Check that the observer saw the pause.
		assertEquals(1, observer.updateCount());
		assertEquals(pauseNode, observer.lastObservable());
		assertTrue(observer.lastUpdate() instanceof NodePauseEvent);
		NodePauseEvent waitEvent = (NodePauseEvent) observer.lastUpdate();
		assertEquals(pauseNode, waitEvent.node());
		assertEquals(pauseTime, waitEvent.getPauseTime());

		// Check that the node is actually paused.
		assertEquals(SimulationState.PAUSED, pauseNode.getSimulationState());
		assertTrue(pauseNode.nodeThread.isAlive());
	}

	@Test(expected = SimulationRuntimeException.class)
	/**
	 * Test that a node cannot be paused outside of simulation.
	 */
	public void testNodePauseInvalidSimState() {
		testNode.pause(1);
	}

	@Test(timeout = 500)
	/**
	 * Test the the call to unpause correctly unpauses the node thread's
	 * execution.
	 */
	public void testNodeUnpause() {

		// Create a node that pauses just once.
		final long pauseTime = 20;
		MockPauseNode pauseNode = new MockPauseNode(0, pauseTime, 1);

		pauseNode.simulate(leaderBarrier, simProfiler);

		// Wait for simulation to pause.
		while (!leaderBarrier.containsThread(pauseNode.nodeThread)) {
		}

		pauseNode.unpause();

		// Check that the node has unpaused.
		assertFalse(SimulationState.PAUSED == pauseNode.getSimulationState());

		while (!leaderBarrier.containsThread(pauseNode.nodeThread)) {
		}

		assertEquals(SimulationState.COMPLETED, pauseNode.getSimulationState());
	}

	@Test(expected = SimulationRuntimeException.class)
	/**
	 * Tests that a node cannot be unpaused if it is not already in the paused
	 * state.
	 */
	public void testNodeUnpauseInvalidSimState() {
		testNode.simState = SimulationState.SIMULATING;
		testNode.unpause();
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we can't queue a message that is marked as unsent at a Node.
	 */
	public void testQueueUnsentMessage() {
		StringMessage message = new StringMessage("BLAH");
		testNode.queueMessage(message);
	}

	@Test
	/**
	 * Tests that queuing a message at a Node stores the message correctly.
	 */
	public void testQueueMessageValid() {

		Node source = otherNodes[0];
		testNode.addNeighbour(source, 1);
		ILinkImpl link = testNode.links.get(source);

		MockMessage testMessage = new MockMessage(source, testNode, 34);
		testNode.queueMessage(testMessage);

		assertEquals(1, testNode.messages.size());
		assertEquals(testMessage, testNode.messages.peek());
		assertEquals(1, link.messageCount(testNode));
		assertEquals(testMessage, link.popMessage(testNode));
	}

	@Test(expected = IllegalStateException.class)
	public void testHaltInvalid() {
		testNode.simState = SimulationState.PRE_SIMULATION;
		testNode.halt();
	}

	@Test(timeout = 500)
	/**
	 * Test that halting a paused node correctly interrupts the node.
	 */
	public void testHaltValidPaused() {
		MockPauseNode pauseNode = new MockPauseNode(0);

		pauseNode.simulate(leaderBarrier, simProfiler);

		while (!leaderBarrier.containsThread(pauseNode.getThread())) {
		}

		pauseNode.halt();

		while (pauseNode.getSimulationState() != SimulationState.POST_SIMULATION) {
		}
	}

	@Test
	/**
	 * Tests that <code>getNeighbour(int)</code> correctly returns a unique
	 * neighbour for all indexes from 0 to <code>neighbourCount()</code>.
	 */
	public void testGetNeighbourValid() {

		// Add some neighbours.
		for (int i = 0; i < otherNodes.length; i++) {
			testNode.addNeighbour(otherNodes[i], 1);
		}

		// Get all the INodes that getNeighbour returns.
		HashSet<INode> returnedNeighbours = new HashSet<INode>(
				otherNodes.length);
		for (int i = 0; i < testNode.neighbourCount(); i++) {
			returnedNeighbours.add(testNode.getNeighbour(i));
		}

		// Check that the expected INodes were returned and that there were no
		// duplicates.
		assertEquals(testNode.neighbourCount(), returnedNeighbours.size());
		for (INode node : returnedNeighbours)
			assertTrue(testNode.isNeighbour(node));
	}

	@Test(expected = IndexOutOfBoundsException.class)
	/**
	 * Tests that a call to <code>getNeighbour(int)</code> cannot be made with
	 * an index greater than <code>neighbourCount()</code> - 1.
	 */
	public void testGetNeighbourIndexGreaterOutOfBounds() {
		testNode.getNeighbour(testNode.neighbourCount());
	}

	@Test(expected = IndexOutOfBoundsException.class)
	/**
	 * Test that a call to <code>getNeighbour(int)</code> cannot be made with
	 * an index less than 0.
	 */
	public void testGetNeihgbourIndexLessOutOfBounds() {
		testNode.getNeighbour(-1);
	}

	@Test
	/**
	 * Test that getting the index of a valid neighbour is possible.
	 */
	public void testNeighbourIndex() {
		for (int i = 0; i < otherNodes.length; i++) {
			testNode.addNeighbour(otherNodes[i], 1);
		}

		for (int i = 0; i < otherNodes.length; i++) {
			assertEquals(i, testNode.getIndex(otherNodes[i]));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that getting an index for a non neighbour is not possible.
	 */
	public void testNonNeighbourIndex() {
		testNode.getIndex(new MockUserNode());
	}

	@Test
	/**
	 * Test that setting the color of a node to a valid colour is possible.
	 */
	public void testSetColourValid() {
		final Color testColour = new Color(5, 5, 5);
		testNode.setColour(testColour);
		assertEquals(1, observer.updateCount());
		assertTrue(observer.getUpdate(0) instanceof ColourChangeEvent);
		ColourChangeEvent event = (ColourChangeEvent) observer.getUpdate(0);
		assertEquals(testColour, new Color(event.getColour()));
		assertEquals(testNode, event.node());
		assertEquals(ISimulationEvent.CURRENT_TIMESTEP, event.getTimestep());
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that a node cannot set a node colour to null;
	 */
	public void testSetColourNull() {
		testNode.setColour(null);
	}
}
