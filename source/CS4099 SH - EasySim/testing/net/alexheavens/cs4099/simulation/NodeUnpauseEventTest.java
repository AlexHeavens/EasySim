package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.assertEquals;
import net.alexheavens.cs4099.concurrent.LeaderBarrier;
import net.alexheavens.cs4099.network.MockPauseNode;

import org.junit.Before;
import org.junit.Test;

public class NodeUnpauseEventTest {

	private final static int N_NODES = 100;

	private LeaderBarrier leaderBarrier;
	private SimulationProfiler simProfiler;

	@Before
	public void setup() {
		leaderBarrier = new LeaderBarrier(Thread.currentThread(), N_NODES);
		simProfiler = new SimulationProfiler(N_NODES);
	}

	@Test
	/**
	 * Test that a Node is correctly unpaused on the processing of a
	 * NodeUnpauseEvent by an EventController.
	 */
	public void testProcessing() {

		// Create a controller that can process the pause event.
		final long SIM_LENGTH = 100;
		EventController testController = new EventController(SIM_LENGTH);

		// Create a test node that we can pause.
		final long pauseLength = 30;
		final int pauses = 1;
		MockPauseNode testNode = new MockPauseNode(0, pauseLength, pauses);
		testNode.addObserver(testController);

		testNode.simulate(leaderBarrier, simProfiler);

		// Wait for the pause.
		while (testController.eventQueue.size() < 1) {
		}

		assertEquals(SimulationState.PAUSED, testNode.getSimulationState());

		testController.processEvent();

		while (testNode.getSimulationState() == SimulationState.SIMULATING) {
		}

		// Test that the unpause event has correctly unpaused the node's
		// execution.
		assertEquals(SimulationState.COMPLETED, testNode.getSimulationState());
	}

}
