package net.alexheavens.cs4099.simulation;

import java.util.HashSet;
import java.util.Set;

import net.alexheavens.cs4099.network.INodeImpl;
import net.alexheavens.cs4099.network.MockPauseNode;
import net.alexheavens.cs4099.network.MockUserNode;
import net.alexheavens.cs4099.network.Network;
import net.alexheavens.cs4099.network.Node;
import net.alexheavens.cs4099.network.configuration.INetworkConfigFactory;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfigFactory;
import net.alexheavens.cs4099.testframework.MockTestSimulator;
import net.alexheavens.cs4099.usercode.MockReceiveScript;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class SimulationRunnerTest {

	private static final long TEST_SIM_LENGTH = 100;
	private Network treeNet = null;
	private MockSimulationRunner testSim = null;
	private NetworkConfig treeConfig = null;

	@Before
	public void setup() throws InstantiationException, IllegalAccessException {
		INetworkConfigFactory netFact = new NetworkConfigFactory();
		treeConfig = netFact.createTreeNetwork(3, 5);
		treeNet = new Network(treeConfig, MockUserNode.class);
		testSim = new MockSimulationRunner(treeNet, TEST_SIM_LENGTH);
	}

	@Test
	public void testCreationValid() {

		// Pre-flight checks.
		assertEquals(SimulationRunner.TIMESTEP_NOT_START,
				testSim.getVisibleTimestep());
		assertEquals(SimulationState.PRE_SIMULATION, testSim.simulationState());
		assertEquals(TEST_SIM_LENGTH, testSim.simulationLength());

		// Test that the runner was added as an observer to the EventController.
		EventController testController = testSim.eventController;
		assertEquals(1, testController.countObservers());
		testController.deleteObserver(testSim);
		assertEquals(0, testController.countObservers());

		// Check that the runner was added as an observer to all nodes.
		for (INodeImpl node : testSim.network().nodes()) {
			MockUserNode simpleNode = (MockUserNode) node;
			assertEquals(0, simpleNode.setupCount());
			assertEquals(0, simpleNode.executedCount());
			assertEquals(1, simpleNode.countObservers());
			simpleNode.deleteObserver(testSim.eventController);
			assertEquals(0, simpleNode.countObservers());
		}
	}

	@Test
	public void testCreationNullNetwork() {
		try {
			new SimulationRunner(null, TEST_SIM_LENGTH);
			fail("Able to create simulation runner with a null network.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testCreationInvalidLength() {
		try {
			new SimulationRunner(treeNet, 0);
			fail("Able to create simulation runner with an invalid length.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Tests if the appropriate IllegalStateException is thrown is
	 * advanceTimestep is called outside before simulation.
	 */
	public void testAdvanceTimestepBeforeSimulation() {
		testSim.setSimState(SimulationState.PRE_SIMULATION);
		testSim.advanceTimestepTo(1);
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Tests if the appropriate IllegalStateException is thrown is
	 * advanceTimestep is called outside after simulation.
	 */
	public void testAdvanceTimestepAfterSimulation() {
		testSim.setSimState(SimulationState.POST_SIMULATION);
		testSim.advanceTimestepTo(1);
	}

	@Test
	public void testAdvanceTimestepInvalidTimestep() {
		testSim.setSimState(SimulationState.SIMULATING);
		testSim.setTimestep(-1);

		try {
			testSim.advanceTimestepTo(1);
			fail("Able to advance timestep on invalid existing timestep.");
		} catch (IllegalStateException e) {
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAdvanceToInvalidTimestep() {
		testSim.setSimState(SimulationState.SIMULATING);
		testSim.advanceTimestepTo(SimulationRunner.TIMESTEP_END - 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAdvancePastSimulationLength() {
		final long timestep = 100;
		testSim.setSimState(SimulationState.SIMULATING);
		testSim.setTimestep(timestep);
		testSim.advanceTimestepTo(TEST_SIM_LENGTH + 1);
	}

	@Test(timeout = 5000)
	public void testSimulate() {

		testSim.simulate();

		assertEquals(SimulationRunner.TIMESTEP_END,
				testSim.getVisibleTimestep());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAdvanceToPreviousTimestep() {
		final long timestep = 50;
		testSim.setSimState(SimulationState.SIMULATING);
		testSim.setTimestep(timestep);
		testSim.advanceTimestepTo(timestep - 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAdvanceToCurrentTimestep() {
		final long timestep = 50;
		testSim.setSimState(SimulationState.SIMULATING);
		testSim.setTimestep(timestep);
		testSim.advanceTimestepTo(timestep);
	}

	@Test
	public void testAdvanceTimestepValidIncrement() {

		testSim.setSimState(SimulationState.SIMULATING);
		testSim.setTimestep(0);

		for (int i = 0; i < testSim.simulationLength(); i++) {
			testSim.advanceTimestepTo(testSim.getVisibleTimestep() + 1);

			assertEquals(i + 1, testSim.getVisibleTimestep());
			assertEquals(SimulationState.SIMULATING, testSim.simulationState());
		}
	}

	@Test
	public void testAdvanceTimestepValidJumps() {

		testSim.setSimState(SimulationState.SIMULATING);
		testSim.setTimestep(0);

		for (int i = 10; i <= testSim.simulationLength(); i += 10) {
			testSim.advanceTimestepTo(i);

			assertEquals(i, testSim.getVisibleTimestep());
			assertEquals(SimulationState.SIMULATING, testSim.simulationState());
		}
	}

	@Test(timeout = 1000)
	public void testSimulateSimpleNodes() {

		testSim.simulate();

		while (testSim.simulationState() != SimulationState.POST_SIMULATION) {
		}

		for (INodeImpl node : testSim.network().nodes()) {
			MockUserNode simpleNode = (MockUserNode) node;
			assertEquals(1, simpleNode.setupCount());
			assertEquals(1, simpleNode.executedCount());
		}
	}

	@Test
	/**
	 * Test that a node passed by a notify update is correctly processed.
	 */
	public void testProcessEventValid() {

		testSim.setTimestep(0);

		// Prepare the event.
		INodeImpl node = testSim.network().nodes().get(0);

		MockNodeEvent event = new MockNodeEvent(
				ISimulationEvent.CURRENT_TIMESTEP, node);

		// Check that the event has not been processed at all.
		assertEquals(0, event.eventProcessedCount());
		assertEquals(0, event.simProcessedCount());

		node.raiseEvent(event);

		// Check that the event was processed correctly.
		assertEquals(1, event.eventProcessedCount());
		assertEquals(1, event.simProcessedCount());
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Tests that processing a low priority event will cause an
	 * IllegalStateException for an unmodified SimulationEvent.
	 * 
	 * This is important to ensure the easy detection of low priority events
	 * being erroneously passed to a SimulationRunner.
	 */
	public void testProcessNonPriorityEvent() {

		// Create an observable that can pass our event to the runner.
		MockUserNode observable = new MockUserNode();
		observable.addObserver(testSim);

		// Create an event just below the priority threshold.
		MockBlankEvent event = new MockBlankEvent(0);

		observable.triggerEvent(event);
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Tests that a SimulationRunner will not accept an out of date event.
	 */
	public void testPastTimestepEventNotify() {

		// Prepare our runner.
		final long simTime = 50;
		testSim.setTimestep(simTime);

		// Create the out of date event.
		final long eventTime = 0;
		MockSimEvent event = new MockSimEvent(eventTime);
		event.setPriority(0);

		testSim.eventController.scheduleEvent(event);
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Tests that a SimulationRunner will not accept a future event.
	 */
	public void testFutureTimestepEventNotify() {

		// Prepare our runner.
		final long simTime = 50;
		testSim.setTimestep(simTime);

		// Create the out of date event.
		final long eventTime = 100;
		MockSimEvent event = new MockSimEvent(eventTime);
		event.setPriority(0);

		testSim.eventController.scheduleEvent(event);
		testSim.eventController.processEvent();
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that non-events are not accepted by the SimulationRunner.
	 */
	public void testNonEventNotify() {
		MockUserNode observable = new MockUserNode();
		observable.addObserver(testSim);
		observable.triggerEvent(new Object());

	}

	@Test(timeout = 1000)
	public void testSimulateRepeatedPauses() throws InstantiationException,
			IllegalAccessException {

		// Create our simulation with some repeatedly pausing nodes.
		treeNet = new Network(treeConfig, MockPauseNode.class);
		testSim = new MockSimulationRunner(treeNet, TEST_SIM_LENGTH);

		testSim.simulate();

		// Wait for simulation to complete.
		while (testSim.simulationState() != SimulationState.POST_SIMULATION) {
		}

		// Check that the nodes paused the expected number of times.
		final int expectedPauseCount = (int) (TEST_SIM_LENGTH / MockPauseNode.DEFAULT_PAUSE_TIME);
		for (Node node : testSim.network().nodes()) {
			MockPauseNode pauseNode = (MockPauseNode) node;
			assertEquals(expectedPauseCount, pauseNode.pauseCount());
		}
	}

	@Test(timeout = 1000)
	public void testReturnLog() {
		IEventLog log = testSim.simulate().getEvents();
		assertEquals(testSim.eventController.getEventLog(), log);
	}

	@Test(timeout = 1000)
	public void testStopNodes() {
		try {
			treeNet = new Network(treeConfig, MockPauseNode.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		testSim = new MockSimulationRunner(treeNet, TEST_SIM_LENGTH);

		testSim.simulate();

		while (testSim.simState != SimulationState.POST_SIMULATION) {
		}

		for (Node node : testSim.network().nodes()) {
			assertEquals(SimulationState.POST_SIMULATION,
					node.getSimulationState());
		}
	}

	@Test
	/**
	 * Tests that a set of events can be prescribed to occur during simulation.
	 */
	public void testSimulateWithEvents() throws InstantiationException,
			IllegalAccessException {

		// Create some events killing a node each timestep.
		Set<PrescribedEvent> events = new HashSet<PrescribedEvent>();
		final int N_EVENTS = treeConfig.nodeCount() / 2;
		for (int i = 0; i < N_EVENTS; i++) {
			events.add(new NodeKillEvent(i, i));
		}

		MockTestSimulator sim = new MockTestSimulator(treeConfig,
				MockReceiveScript.class, events);

		Network net = sim.getRunner().network();

		// Simulate for enough timesteps for all prescribed nodes to die.
		for (long timestep = 0; timestep < N_EVENTS; timestep++) {
			sim.simulateTo(timestep);
			for (Node node : net.nodes()) {
				assertEquals(
						node.getSimulationId() < timestep,
						node.getSimulationState() == SimulationState.POST_SIMULATION);
			}
		}

		// For the remainder of simulation, ensure only these nodes are dead.
		for (long timestep = N_EVENTS; timestep < TEST_SIM_LENGTH; timestep++) {
			sim.simulateTo(timestep);
			for (Node node : net.nodes()) {
				assertEquals(
						node.getSimulationId() < N_EVENTS,
						node.getSimulationState() == SimulationState.POST_SIMULATION);
			}
		}
	}
}
