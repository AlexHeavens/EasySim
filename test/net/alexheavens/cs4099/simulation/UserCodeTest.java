package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import net.alexheavens.cs4099.concurrent.LeaderBarrier;
import net.alexheavens.cs4099.network.MockSimObserver;
import net.alexheavens.cs4099.network.MockUserNode;

public class UserCodeTest {

	private static final int N_NODES = 100;
	
	private LeaderBarrier leaderBarrier;
	private SimulationProfiler simProfiler;
	private MockUserNode testNode;

	@Before
	public void setup() {
		leaderBarrier = new LeaderBarrier(Thread.currentThread(), N_NODES);
		simProfiler = new SimulationProfiler(N_NODES);
	}

	@Test(timeout = 500)
	/**
	 * Tests that should the execution of a user node throw an unchecked
	 * exception, the simulation will catch it, proclaim to observers that the
	 * node has failed and mark the node as past simulation.
	 */
	public void testExecuteRuntimeException() {
		testNode = new MockUserNode(32432) {
			public void execute() {
				throw new RuntimeException();
			}
		};

		MockSimObserver observer = new MockSimObserver();
		testNode.addObserver(observer);

		try {
			testNode.simulate(leaderBarrier, simProfiler);
		} catch (RuntimeException e) {
			fail("Did not catch user runtime exception.");
		}

		while (testNode.getSimulationState() == SimulationState.PRE_SIMULATION
				|| testNode.getSimulationState() == SimulationState.SIMULATING)
			;

		assertEquals(SimulationState.NODE_ERROR,
				testNode.getSimulationState());
		assertEquals(1, observer.updateCount());
		assertTrue(observer.getUpdate(0) instanceof NodeFailureEvent);
		NodeFailureEvent failureEvent = (NodeFailureEvent) observer
				.getUpdate(0);
		assertEquals(testNode, failureEvent.node());
	}

	@Test(timeout = 100)
	/**
	 * Test that a user Node that throws a runtime exception in user code
	 * correctly catches the exception and issues a NodeFailureEvent to any
	 * observer.
	 */
	public void testSetupRuntimeException() {
		testNode = new MockUserNode(32432) {
			public void setup() {
				throw new RuntimeException();
			}
		};

		MockSimObserver observer = new MockSimObserver();
		testNode.addObserver(observer);

		try {
			testNode.simulate(leaderBarrier, simProfiler);
		} catch (RuntimeException e) {
			fail("Did not catch user runtime exception.");
		}

		while (testNode.getSimulationState() != SimulationState.NODE_ERROR) {
		}

		assertEquals(1, observer.updateCount());
		assertTrue(observer.getUpdate(0) instanceof NodeFailureEvent);
		NodeFailureEvent failureEvent = (NodeFailureEvent) observer
				.getUpdate(0);
		assertEquals(testNode, failureEvent.node());
	}

}
