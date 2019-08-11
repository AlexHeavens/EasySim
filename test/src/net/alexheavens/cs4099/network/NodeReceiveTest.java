package net.alexheavens.cs4099.network;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.alexheavens.cs4099.concurrent.LeaderBarrier;
import net.alexheavens.cs4099.simulation.ISimulationEvent;
import net.alexheavens.cs4099.simulation.InvalidCallException;
import net.alexheavens.cs4099.simulation.MessageReadEvent;
import net.alexheavens.cs4099.simulation.NodeReceiveBlockEvent;
import net.alexheavens.cs4099.simulation.NodeReceiveBlockResumeEvent;
import net.alexheavens.cs4099.simulation.SimulationProfiler;
import net.alexheavens.cs4099.simulation.SimulationState;

import org.junit.Before;
import org.junit.Test;

public class NodeReceiveTest {

	private static final int N_NEIGHBOURS = 20;

	private MockReceiveNode testNode;
	private MockUserNode testNeighbour;
	private ILinkImpl testNeighbourLink;
	private MockUserNode[] testNeighbours;
	private MockSimObserver testObserver;
	private LeaderBarrier leaderBarrier;
	private SimulationProfiler simProfiler;

	@Before
	/**
	 * Before all tests, create a node with some neighbours and an observer.
	 */
	public void setup() {

		// Create some Nodes to test with.
		int nextId = 0;
		testNode = new MockReceiveNode(nextId++);
		testNeighbour = new MockUserNode(nextId++);
		testNode.addNeighbour(testNeighbour, 1);
		testNeighbourLink = (ILinkImpl) testNode.neighbourLink(testNeighbour);
		testNeighbours = new MockUserNode[N_NEIGHBOURS];
		for (int i = 0; i < testNeighbours.length; i++) {
			testNeighbours[i] = new MockUserNode(nextId++);
			testNode.addNeighbour(testNeighbours[i], 1);
		}

		// Create an Observer to test for event raising.
		testObserver = new MockSimObserver();
		testNode.addObserver(testObserver);

		// Create a leader barrier to pause nodes at.
		leaderBarrier = new LeaderBarrier(Thread.currentThread(), nextId);
		simProfiler = new SimulationProfiler(nextId);
		testNode.setWaitRegistrar(leaderBarrier);
	}

	@Test(timeout = 500)
	/**
	 * Tests that a user node making a call to the receive method will block if
	 * no message is available.
	 */
	public void testReceiveMessageBlock() {

		testNode.setRecNode(testNeighbour);
		testNode.simulate(leaderBarrier, simProfiler);

		// Wait for the node Thread to wait.
		while (!leaderBarrier.containsThread(testNode.getThread())) {
		}

		// Check that the Node is in a blocking state, waiting for a particular
		// sender.
		assertEquals(SimulationState.RECEIVE_BLOCK,
				testNode.getSimulationState());
		assertEquals(testNeighbour, testNode.expectedSender);

	}

	@Test(timeout = 500)
	/**
	 * Tests that the generic receive method also blocks if no message has
	 * arrived at a node.
	 */
	public void testReceiveGenericMessageBlock() {
		testNode.simulate(leaderBarrier, simProfiler);

		// Wait for the node Thread to wait.
		while (!leaderBarrier.containsThread(testNode.getThread())) {
		}

		// Check that the Node is in a blocking state.
		assertEquals(SimulationState.RECEIVE_BLOCK,
				testNode.getSimulationState());
		assertNull(testNode.expectedSender);
	}

	@Test(timeout = 100)
	/**
	 * Tests that the receive method of Node will return a Message from a
	 * specific link, if one is available.
	 * 
	 * This will timeout if the Node (incorrectly) blocks for the Message.
	 */
	public void testReceiveMessageNonBlock() {

		// Send a bunch of messages from one to the other.
		final int nMessages = 100;
		MockMessage[] messages = new MockMessage[nMessages];
		for (int i = 0; i < messages.length; i++) {
			messages[i] = new MockMessage(testNeighbour, testNode, i);
			testNode.queueMessage(messages[i]);
		}

		testNode.setRecNode(testNeighbour);
		testNode.nodeThread = new Thread(testNode);
		testNode.setSimulationState(SimulationState.SIMULATING);
		testNode.execute();

		// Check that the received message matches the first sent.
		assertEquals(messages[0], testNode.recMessage());
		assertEquals(nMessages - 1, messages[0].link.messageCount(testNode));
		assertEquals(0, messages[0].link.messageCount(testNeighbour));

		assertNull(testNode.expectedSender);

		// Check that a read event is raised.
		assertEquals(1, testObserver.updateCount());
		assertEquals(testNode, testObserver.lastObservable());
		assertTrue(testObserver.lastUpdate() instanceof MessageReadEvent);
		MessageReadEvent readEvent = (MessageReadEvent) testObserver
				.lastUpdate();
		assertEquals(messages[0], readEvent.message());
		assertEquals(ISimulationEvent.CURRENT_TIMESTEP, readEvent.getTimestep());
	}

	@Test(timeout = 100)
	/**
	 * Tests that on blocking for a receive, a Node will unblock and return a
	 * Message that has just become available.
	 * 
	 * This will timeout if the Node never unblocks for the Message.
	 */
	public void testBlockThenReceive() {

		testNode.setRecNode(testNeighbour);
		testNode.simulate(leaderBarrier, simProfiler);

		// Wait for the Node to be waiting for the Message.
		while (testNode.getSimulationState() != SimulationState.RECEIVE_BLOCK) {
		}

		// Check that a block event was raised.
		assertEquals(1, testObserver.updateCount());
		assertTrue(testObserver.getUpdate(0) instanceof NodeReceiveBlockEvent);
		NodeReceiveBlockEvent blockEvent = (NodeReceiveBlockEvent) testObserver
				.getUpdate(0);
		assertEquals(testNode, blockEvent.node());
		assertEquals(ISimulationEvent.CURRENT_TIMESTEP,
				blockEvent.getTimestep());

		assertEquals(testNeighbour, testNode.expectedSender);

		// Send a bunch of messages from one to the other.
		final int nMessages = 100;
		MockMessage[] messages = new MockMessage[nMessages];
		for (int i = 0; i < nMessages; i++) {
			messages[i] = new MockMessage(testNeighbour, testNode, 0);
			testNode.queueMessage(messages[i]);
		}

		// Wait for the Node to complete simulation.
		while (testNode.getSimulationState() != SimulationState.COMPLETED) {
		}

		// Check that the messages caused arrival events and that the node was
		// resumed.

		assertEquals(3, testObserver.updateCount());

		boolean resumeEventOccurred = false;
		IMessageImpl<?> readMessage = null;
		for (int i = 1; i < 3; i++) {
			if (testObserver.getUpdate(i) instanceof NodeReceiveBlockResumeEvent) {
				NodeReceiveBlockResumeEvent unblockEvent = (NodeReceiveBlockResumeEvent) testObserver
						.getUpdate(i);
				if (resumeEventOccurred)
					fail("More than one receive event.");
				else
					resumeEventOccurred = true;
				assertEquals(testNode, unblockEvent.node());
				assertEquals(ISimulationEvent.CURRENT_TIMESTEP,
						unblockEvent.getTimestep());
			} else if (testObserver.getUpdate(i) instanceof MessageReadEvent) {
				MessageReadEvent readEvent = (MessageReadEvent) testObserver
						.getUpdate(i);
				if (readMessage != null)
					fail("More than one message read event.");
				readMessage = readEvent.message();
			} else {
				fail("Unexpected event.");
			}
		}

		assertTrue(resumeEventOccurred);
		assertNotNull(readMessage);
	}

	@Test
	/**
	 * Tests that a call to the parameterless receive methods will take a
	 * message from any Message queue that is non-empty, when it is available.
	 * 
	 * This test will timeout if the method never unblocks waiting for a
	 * Message.
	 */
	public void testReceiveAnyOnce() {

		testNode.simulate(leaderBarrier, simProfiler);

		// Wait for the execution of the Node to complete.
		while (testNode.getSimulationState() != SimulationState.RECEIVE_BLOCK) {
		}

		// Queue a message from an arbitrary neighbour.
		final int messageTime = 15;
		MockMessage message = new MockMessage(testNeighbour, testNode,
				messageTime);
		testNode.queueMessage(message);

		// Wait for the execution of the Node to complete.
		while (testNode.getSimulationState() != SimulationState.COMPLETED) {
		}

		// Check that the message was returned.
		assertEquals(message, testNode.recMessage());
		assertEquals(messageTime, testNode.recMessage().getSentAt());

		// We expect a receive block event, receive
		// unblock and message read event, in that order.
		assertEquals(3, testObserver.updateCount());
		assertTrue(testObserver.getUpdate(0) instanceof NodeReceiveBlockEvent);

		// ReceiveBlockEvent.
		NodeReceiveBlockEvent blockEvent = (NodeReceiveBlockEvent) testObserver
				.getUpdate(0);
		assertEquals(testNode, blockEvent.node());
		assertEquals(ISimulationEvent.CURRENT_TIMESTEP,
				blockEvent.getTimestep());

		// Unblock event.
		assertTrue(testObserver.getUpdate(1) instanceof NodeReceiveBlockResumeEvent);
		NodeReceiveBlockResumeEvent unblockEvent = (NodeReceiveBlockResumeEvent) testObserver
				.getUpdate(1);
		assertEquals(testNode, unblockEvent.node());
		assertEquals(ISimulationEvent.CURRENT_TIMESTEP,
				blockEvent.getTimestep());

		// MessageRead event.
		assertTrue(testObserver.getUpdate(2) instanceof MessageReadEvent);
		MessageReadEvent readEvent = (MessageReadEvent) testObserver
				.getUpdate(2);
		assertEquals(message, readEvent.message());
		assertEquals(ISimulationEvent.CURRENT_TIMESTEP, readEvent.getTimestep());
	}

	@Test(timeout = 500)
	/**
	 * Tests that a generic receive call takes messages in order of arrival at
	 * the node and will not strangle any link across timesteps.
	 */
	public void testReceiveAcrossTimesteps() {

		final int nMessages0PerNode = 10;
		final int nMessagesIncPerNode = 20;
		final int nMessagesStep0 = nMessages0PerNode * testNeighbours.length;
		final int nIncMessages = nMessagesIncPerNode * testNeighbours.length;
		final int nMessagesRead = nMessagesStep0 + nIncMessages / 2;

		// Create some messages from a range of neighbours.
		MockMessage[] messagesStep0 = new MockMessage[nMessagesStep0];
		for (int i = 0; i < messagesStep0.length; i++) {
			INodeImpl source = testNeighbours[i % testNeighbours.length];
			messagesStep0[i] = new MockMessage(source, testNode, 0);
			testNode.queueMessage(messagesStep0[i]);
		}

		MockMessage[] messagesIncStep = new MockMessage[nIncMessages];
		for (int i = 0; i < messagesIncStep.length; i++) {
			INodeImpl source = testNeighbours[i % testNeighbours.length];
			messagesIncStep[i] = new MockMessage(source, testNode, 1 + i);
			testNode.queueMessage(messagesIncStep[i]);
		}

		testNode.setNumMsgsRead(nMessagesRead);
		testNode.simulate(leaderBarrier, simProfiler);

		while (testNode.getSimulationState() != SimulationState.COMPLETED) {
		}

		assertNotNull(testNode.recMessage());
		for (ILinkImpl link : testNode.links.values()) {
			if (link == testNeighbourLink)
				assertEquals(0, link.messageCount(testNode));
			else
				assertEquals(nMessagesIncPerNode / 2,
						link.messageCount(testNode));
		}

	}

	@Test(timeout = 500)
	/**
	 * Tests that calling the parameterless receive method pops messages off in
	 * chronological order/
	 */
	public void testReceiveOrderedByTimestep() {

		// Create and queue messages from neighbours with incrementing
		// timesteps.
		final int nMessagesIncPerNode = 20;
		final int nIncMessages = nMessagesIncPerNode * testNeighbours.length;
		final int nMessagesRead = nIncMessages / 2;

		MockMessage[] messagesIncStep = new MockMessage[nIncMessages];
		for (int i = nIncMessages - 1; i >= 0; i--) {
			INodeImpl source = testNeighbours[i % testNeighbours.length];
			messagesIncStep[i] = new MockMessage(source, testNode, i);
			testNode.queueMessage(messagesIncStep[i]);
		}

		testNode.setNumMsgsRead(nMessagesRead);
		testNode.simulate(leaderBarrier, simProfiler);

		// Wait for the simulation to complete.
		while (testNode.getSimulationState() != SimulationState.COMPLETED) {
		}

		// Check that the remaining messages are in the order we would expect.
		for (int i = 0; i < nIncMessages / 2; i++) {
			final long expectedTimestep = i + nIncMessages / 2;
			assertEquals(expectedTimestep, testNode.messages.poll().getSentAt());
		}
	}

	@Test(timeout = 100)
	/**
	 * Test that a call to receive where a node has no neighbours immediately
	 * return null.
	 */
	public void testReceiveNoNeighbours() {
		testNode = new MockReceiveNode(0);
		testNode.nodeThread = new Thread();
		testNode.setSimulationState(SimulationState.SIMULATING);
		testNode.receive();
	}

	@Test(expected = InvalidCallException.class, timeout = 100)
	/**
	 * Test that a call to the general receive method cannot be made in setup.
	 */
	public void testGeneralReceiveInSetup() {
		testNode.setSimulationState(SimulationState.SETUP);
		testNode.nodeThread = new Thread();
		testNode.receive();
	}

	@Test(expected = InvalidCallException.class, timeout = 100)
	/**
	 * Test that a call to the specific receive method cannot be made in setup.
	 */
	public void testSpecificReceiveInSetup() {
		testNode.setSimulationState(SimulationState.SETUP);
		testNode.nodeThread = new Thread();
		testNode.receive(null); // Should take priority over
								// NullPointerException.
	}

	@Test
	/**
	 * Test that when expected a message from one nodes, messages from others do
	 * not unblock the recipient.
	 */
	public void testReceiveDifferentNodes() {
		testNode.setRecNode(testNeighbour);
		testNode.simulate(leaderBarrier, simProfiler);

		// Wait for the node Thread to wait.
		while (!leaderBarrier.containsThread(testNode.getThread())) {
		}

		testNode.queueMessage(new MockMessage(testNeighbours[0], testNode, 0));
		assertEquals(SimulationState.RECEIVE_BLOCK,
				testNode.getSimulationState());

	}
}
