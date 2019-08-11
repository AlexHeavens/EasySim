package net.alexheavens.cs4099.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import net.alexheavens.cs4099.simulation.ISimulationEvent;
import net.alexheavens.cs4099.simulation.InvalidCallException;
import net.alexheavens.cs4099.simulation.MessageEvent;
import net.alexheavens.cs4099.simulation.MessageSentEvent;
import net.alexheavens.cs4099.simulation.SimulationState;

import org.junit.Before;
import org.junit.Test;

public class NodeSendTest {

	private MockUserNode testNode, testNeighbour;
	private MockSimObserver testObserver;

	@Before
	public void setup() {
		testNode = new MockUserNode(0);
		testNeighbour = new MockUserNode(1);
		testNode.addNeighbour(testNeighbour, 1);
		testObserver = new MockSimObserver();
		testNode.addObserver(testObserver);
	}

	@Test
	/**
	 * Tests that sending a message results in the correct event being raised.
	 */
	public void testSendMessageValid() {

		// Define the data we are sending and to whom we are sending it.
		final String testTag = "TEST_TAG";
		final String testData = "TEST_DATA";
		StringMessage testMessage = new StringMessage(testTag, testData);

		testNode.simState = SimulationState.SIMULATING;
		testNode.send(testNeighbour, testMessage);

		// Check that the event was raised correctly.
		assertEquals(1, testObserver.updateCount());
		assertEquals(testNode, testObserver.lastObservable());
		assertTrue(testObserver.lastUpdate() instanceof MessageSentEvent);
		MessageSentEvent event = (MessageSentEvent) testObserver.lastUpdate();
		assertEquals(ISimulationEvent.CURRENT_TIMESTEP, event.getTimestep());
		assertFalse(testMessage == event.message());
		StringMessage message = (StringMessage) event.message();
		assertEquals(testNode, message.source());
		assertEquals(testNeighbour, message.target());
		assertEquals(testNode.neighbourLink(testNeighbour), message.link());
	}

	@Test(expected = NullPointerException.class)
	public void testSendNullMessage() {
		testNode.setSimulationState(SimulationState.SIMULATING);
		testNode.send(testNeighbour, null);
	}

	@Test(expected = NullPointerException.class)
	/**
	 * Test that a user cannot attempt to send to a messsage to a null
	 * recipient.
	 */
	public void testSendNullRecipient() {
		testNode.setSimulationState(SimulationState.SIMULATING);
		testNode.send(null, new StringMessage("Yer a Wizard Harry!"));
	}

	@Test
	public void testSendAll() {

		testNode = new MockUserNode(0);
		testObserver = new MockSimObserver();
		testNode.addObserver(testObserver);

		final int nNodes = 100;
		MockUserNode[] neighbours = new MockUserNode[nNodes];
		for (int i = 0; i < neighbours.length; i++) {
			neighbours[i] = new MockUserNode(i + 1);
			testNode.addNeighbour(neighbours[i], 1);
		}

		final String testTag = "TEST_TAG";
		final String testData = "TEST_DATA";
		StringMessage testMessage = new StringMessage(testTag, testData);

		testNode.simState = SimulationState.SIMULATING;
		testNode.sendAll(testMessage);

		assertEquals(testNode.neighbourCount(), testObserver.updateCount());
		assertEquals(testNode, testObserver.lastObservable());

		HashSet<IMessage<?>> received = new HashSet<IMessage<?>>(
				testNode.neighbourCount());

		for (int i = 0; i < testObserver.updateCount(); i++) {
			Object event = testObserver.getUpdate(i);
			assertTrue(event instanceof MessageSentEvent);
			MessageSentEvent sentEvent = (MessageSentEvent) event;
			assertEquals(ISimulationEvent.CURRENT_TIMESTEP,
					sentEvent.getTimestep());
			assertFalse(received.contains(sentEvent.message()));

			received.add(sentEvent.message());
		}

		assertEquals(testNode.neighbourCount(), received.size());
	}

	@Test
	/**
	 * Tests that resending a message appropriately copies the message.
	 */
	public void testResendMessage() {
		final String testTag = "TEST_TAG";
		final String testData = "TEST_DATA";
		StringMessage testMessage = new StringMessage(testTag, testData);

		testNeighbour.addObserver(testObserver);

		testNode.simState = SimulationState.SIMULATING;
		testNode.send(testNeighbour, testMessage);

		StringMessage receivedMessage = (StringMessage) ((MessageEvent) testObserver
				.lastUpdate()).message();
		testNeighbour.simState = SimulationState.SIMULATING;
		testNeighbour.send(testNode, receivedMessage);

		StringMessage returnMessage = (StringMessage) ((MessageEvent) testObserver
				.lastUpdate()).message();
		assertEquals(testMessage.getData(), returnMessage.getData());
		assertEquals(testMessage.getTag(), returnMessage.getTag());
	}

	@Test(expected = InvalidCallException.class)
	public void testSendFromSetup() {
		testNode = new MockUserNode(0) {
			public void setup() {
				MockMessage message = new MockMessage(testNode, testNeighbour,
						0);
				send(testNeighbour, message);
			}
		};

		testNode.setSimulationState(SimulationState.SETUP);
		testNode.setup();
	}

}
