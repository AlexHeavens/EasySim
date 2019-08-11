package net.alexheavens.cs4099.testframework;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import net.alexheavens.cs4099.examples.TreeLeaderNode;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfigException;
import net.alexheavens.cs4099.simulation.SimulationRunner;
import net.alexheavens.cs4099.usercode.NodeScript;
import net.sf.json.JSONException;

import org.junit.Before;
import org.junit.Test;

public class TestSimulatorTest {

	private NetworkConfig testNet;
	private TestSimulator testSim;

	@Before
	public void setup() throws JSONException, NetworkConfigException,
			IOException, InstantiationException, IllegalAccessException {
		testNet = new NetworkConfig(new File(
				"test/networks/simple.network.json"));
		testSim = new TestSimulator(testNet, TreeLeaderNode.class);
	}

	@Test
	/**
	 * Test that the creation of the simulator is valid.
	 */
	public void testCreationValid() {
		Collection<NodeScript> nodes = testSim.getNodes();
		assertEquals(testNet.nodeCount(), nodes.size());
		assertEquals(SimulationRunner.TIMESTEP_NOT_START, testSim.getTimestep());
	}

	@Test(timeout = 1000)
	/**
	 * Test that a single bout of simulation is possible.
	 */
	public void testSimulateTo() {
		final long timestep = 10000;
		testSim.simulateTo(timestep);
		assertEquals(timestep, testSim.getTimestep());
	}

	@Test
	/**
	 * Test that simulation can be resumed repeatedly after an initial spurt.
	 */
	public void testSimulateToRepeated() throws InstantiationException,
			IllegalAccessException {
		final long endTimestep = 10000;
		long currentTimestep = 0;
		while (testSim.getTimestep() < endTimestep) {
			testSim.simulateTo(currentTimestep);
			assertEquals(currentTimestep, testSim.getTimestep());
			currentTimestep++;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that the simulator refuses a negative timestep.
	 */
	public void testSimulateToInvalidTimestep() {
		testSim.simulateTo(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that the simulator refuses to simulate a timestep already simulated.
	 */
	public void testSimulateToPreviousTimestep() {
		final long timestep = 50;
		testSim.simulateTo(timestep);
		testSim.simulateTo(timestep);
	}

	@Test
	/**
	 * Test that resetting the simulation is possible after some initial
	 * simulation.
	 */
	public void testReset() {

		// Simulate for a bit.
		final long stopTimeStep = 50;
		long currentTimestep = 0;
		while (testSim.getTimestep() < stopTimeStep)
			testSim.simulateTo(currentTimestep++);

		// Reset the simulation.
		testSim.reset();
		assertEquals(SimulationRunner.TIMESTEP_NOT_START, testSim.getTimestep());

		// Repeat simulation, checking that the timestep is as expected.
		currentTimestep = 0;
		while (testSim.getTimestep() < stopTimeStep) {
			testSim.simulateTo(currentTimestep);
			assertEquals(currentTimestep, testSim.getTimestep());
			currentTimestep++;
		}
	}

	@Test
	/**
	 * Test that the simulateFor() call can incrementally move forward the
	 * simulation.
	 */
	public void testSimulateFor() {
		// Simulate for a bit.
		final long jumpSize = 1;
		final int jumpIts = 50;
		for (int i = 0; i < jumpIts; i++) {
			testSim.simulateFor(jumpSize);
			assertEquals((i + 1) * jumpSize, testSim.getTimestep());
		}

		// Reset the simulation.
		testSim.reset();
		assertEquals(SimulationRunner.TIMESTEP_NOT_START, testSim.getTimestep());

		// Repeat simulation, checking that the timestep is as expected.
		for (int i = 0; i < jumpIts; i++) {
			testSim.simulateFor(jumpSize);
			assertEquals((i + 1) * jumpSize, testSim.getTimestep());
		}
	}
}
