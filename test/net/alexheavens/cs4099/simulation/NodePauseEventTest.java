package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.alexheavens.cs4099.concurrent.LeaderBarrier;
import net.alexheavens.cs4099.network.MockPauseNode;
import net.alexheavens.cs4099.network.MockSimObserver;
import net.alexheavens.cs4099.network.MockUserNode;

public class NodePauseEventTest {

	private final static int N_NODES = 100;
	
	private LeaderBarrier leaderBarrier;
	private SimulationProfiler simProfiler;

	@Before
	public void setup() {
		leaderBarrier = new LeaderBarrier(Thread.currentThread(), N_NODES);
		simProfiler = new SimulationProfiler(N_NODES);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidPauseLength() {
		new NodePauseEvent(0, new MockUserNode());
	}

	@Test(timeout = 500)
	/**
	 * Test that a pause event is correctly processed by an event controller.
	 */
	public void testPauseNodeProcessing() {

		// Create a controller that can process the pause event.
		final long SIM_LENGTH = 100;
		EventController testController = new EventController(SIM_LENGTH);

		// Create an observer to the controller to make sure that node executed
		// event occurs.
		MockSimObserver mockRunner = new MockSimObserver();
		testController.addObserver(mockRunner);

		// Create a test node that we can pause.
		final long pauseLength = 30;
		final int pauses = 1;
		MockPauseNode testNode = new MockPauseNode(0, pauseLength, pauses);
		testNode.addObserver(testController);

		testNode.simulate(leaderBarrier, simProfiler);

		// Wait for the pause.
		while (!leaderBarrier.containsThread(testNode.getThread())) {
		}

		// Test that the pause event has queued an unpause at the appropriate
		// timestep.
		assertEquals(1, testController.eventQueue.size());
		ISimulationEvent event = testController.eventQueue.peek();
		assertTrue(event instanceof NodeUnpauseEvent);
		NodeUnpauseEvent unpauseEvent = (NodeUnpauseEvent) event;
		assertEquals(testNode, unpauseEvent.node());
		assertEquals(pauseLength, unpauseEvent.getTimestep());
	}

}
