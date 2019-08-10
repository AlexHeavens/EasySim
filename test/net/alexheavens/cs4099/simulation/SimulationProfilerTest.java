package net.alexheavens.cs4099.simulation;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import net.alexheavens.cs4099.concurrent.LeaderBarrier;
import net.alexheavens.cs4099.network.MockPauseNode;
import net.alexheavens.cs4099.network.MockUserNode;
import net.alexheavens.cs4099.network.Node;
import net.alexheavens.cs4099.usercode.NodeScript;

public class SimulationProfilerTest {

	private static final int N_NODES = 100;
	private static final long NODE_TIMEOUT = 500000000;
	private static final long EXP_COMP_TIME = 700;
	private SimulationProfiler testProfiler;
	private LeaderBarrier testBarrier;
	private Node loopNode;

	@Before
	public void setup() {
		testProfiler = new SimulationProfiler(N_NODES);
		testBarrier = new LeaderBarrier(Thread.currentThread(), N_NODES);
		loopNode = new MockPauseNode(){
			public int getSimulationId(){
				return 0;
			}
		};
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot create a profiler with a negative timeout.
	 */
	public void testInvalidCreation() {
		new SimulationProfiler(-1);
	}

	@Test(timeout = EXP_COMP_TIME)
	/**
	 * Test that adding a node that never blocks will kill it of after the
	 * threshold.
	 */
	public void testTrackLoopingNode() {
		testProfiler = new SimulationProfiler(N_NODES, NODE_TIMEOUT);
		loopNode = new MockUserNode(0){

			@Override
			public void execute() {
				while (true) {
				}
			}
		};
		loopNode.simulate(testBarrier, testProfiler);
		while (loopNode.getSimulationState() != SimulationState.POST_SIMULATION) {
		}
	}

	@Test
	/**
	 * Test that a zero timeout means that node will not be killed off.
	 */
	public void testNoTimeout() throws InterruptedException {
		loopNode.simulate(testBarrier, testProfiler);
		Thread.sleep(EXP_COMP_TIME);
		assertNotSame(SimulationState.POST_SIMULATION,
				loopNode.getSimulationState());
	}

	@Test
	/**
	 * Test that tracking the execution of a node for a single timestep is
	 * possible.
	 */
	public void testSingleTimestepTrack() throws InterruptedException {
		final long pauseNanos = NODE_TIMEOUT / 2;
		loopNode = new MockUserNode(0){

			@Override
			public void execute() {
				while (testProfiler.threadInterface.getThreadCpuTime(loopNode
						.getThread().getId()) < pauseNanos) {
				}
			}
		};
		testProfiler.incrementTimestep(0);
		loopNode.simulate(testBarrier, testProfiler);
		Thread.sleep(EXP_COMP_TIME);
		final Map<Long, Long> testMap = testProfiler
				.getTimeStepProcessIdMap().get(loopNode.getSimulationId());
		final Long time = testMap.get(0l);
		assertTrue(time >= pauseNanos);
		assertTrue(time < pauseNanos + 100000000);
	}

	@Test
	/**
	 * Test that node that resumes simulation in the same timestep correctly
	 * calculates the combined simulation time.
	 */
	public void testIntermitentSimulationTrack() throws InterruptedException {
		final long pauseNanos = NODE_TIMEOUT / 2;
		loopNode = new MockUserNode(0) {

			@Override
			public void execute() {
				while (testProfiler.threadInterface.getThreadCpuTime(loopNode
						.getThread().getId()) < pauseNanos) {
				}
				loopNode.setSimulationState(SimulationState.RECEIVE_BLOCK);
				loopNode.setSimulationState(SimulationState.SIMULATING);
				while (testProfiler.threadInterface.getThreadCpuTime(loopNode
						.getThread().getId()) < pauseNanos * 2) {
				}
			}
		};
		testProfiler.incrementTimestep(0);
		loopNode.simulate(testBarrier, testProfiler);
		Thread.sleep(pauseNanos * 2 / 1000000 + 300);
		final Map<Long, Long> testMap = testProfiler
				.getTimeStepProcessIdMap().get(loopNode.getSimulationId());
		final Long time = testMap.get(0l);
		assertTrue(time >= pauseNanos * 2);
		assertTrue(time < pauseNanos * 2 + 100000000);
	}

	@Test
	/**
	 * Test that the profiler keeps track of different times in different
	 * timesteps.
	 */
	public void testSimulationTrackMultipleSteps() {
		final long pauseNanos = 10000000;
		loopNode = new MockUserNode(0){

			@Override
			public void execute() {
				long loop = 1;
				while (true) {
					while (testProfiler.threadInterface
							.getThreadCpuTime(loopNode.getThread().getId()) < pauseNanos
							* 2 * loop - pauseNanos) {
					}
					loopNode.setSimulationState(SimulationState.RECEIVE_BLOCK);
					loopNode.setSimulationState(SimulationState.SIMULATING);
					while (testProfiler.threadInterface
							.getThreadCpuTime(loopNode.getThread().getId()) < pauseNanos
							* 2 * loop) {
					}
					loop++;
					loopNode.pause(1);
				}
			}
		};

		// Faux simulate the node for a million timesteps.
		testProfiler.incrementTimestep(0);
		loopNode.simulate(testBarrier, testProfiler);

		final long N_TIMESTEPS = 50;
		for (long i = 1; i < N_TIMESTEPS; i++) {
			while (loopNode.getSimulationState() != SimulationState.PAUSED) {
			}
			testProfiler.incrementTimestep(i);
			loopNode.unpause();
		}
		while (loopNode.getSimulationState() != SimulationState.PAUSED) {
		}

		// Check that for each timestep, the node has the correct time.
		for (long i = 0; i < N_TIMESTEPS; i++) {
			final Map<Long, Long> testMap = testProfiler
					.getTimeStepProcessIdMap().get(loopNode.getSimulationId());
			final Long time = testMap.get(0l);
			assertTrue(time >= pauseNanos * 2);
			assertTrue(time < pauseNanos * 2 + 200000000);
		}
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Test that we cannot track a process that is already tracked.
	 */
	public void testTrackWhileAlreadyTracking() throws InterruptedException {
		loopNode = new Node(new NodeScript() {

			@Override
			public void execute() {
				while (true) {
				}
			}
		});
		loopNode.simulate(testBarrier, testProfiler);
		Thread.sleep(300);
		testProfiler.trackProcessSimulation(loopNode);
		testProfiler.trackProcessSimulation(loopNode);
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Test that we cannot untrack a process that is not already tracked.
	 */
	public void testUntrackUnknownProcess() throws InterruptedException {
		loopNode.simulate(testBarrier, testProfiler);
		Thread.sleep(300);
		testProfiler.untrackNodeSimulation(loopNode);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot increment the timestep of a profiler to a negative
	 * value.
	 */
	public void testIncrementTimeStepInvalid() {
		testProfiler.incrementTimestep(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot increment the timestep of a profiler to a past step.
	 */
	public void testIncrementTimestepPast() {
		testProfiler.incrementTimestep(0);
		testProfiler.incrementTimestep(0);
	}
	
	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that null is not a valid argument.
	 */
	public void testTrackNullProcess(){
		testProfiler.trackProcessSimulation(null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that null is not a valid argument.
	 */
	public void testUnrackNullProcess(){
		testProfiler.untrackNodeSimulation(null);
	}
}
