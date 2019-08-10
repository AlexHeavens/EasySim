package net.alexheavens.cs4099.testframework;

import java.util.HashSet;
import java.util.Set;

import org.junit.runners.JUnit4;

import net.alexheavens.cs4099.network.Network;
import net.alexheavens.cs4099.network.Node;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;
import net.alexheavens.cs4099.simulation.PrescribedEvent;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * A portable simulator for use in unit testing. This class provides an
 * accessible means for users to simulate code intermittently.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 * 
 * @see JUnit4
 */
public class TestSimulator {

	private final NetworkConfig network;
	private final Class<? extends NodeScript> scriptClass;
	private final Set<PrescribedEvent> events;
	private Set<NodeScript> nodes;

	protected PausableSimulationRunner simRunner;

	/**
	 * Creates a simulator, defining the network and node script from a config.
	 * 
	 * @param network
	 *            the config of the network.
	 * @param scriptClass
	 *            the class run on all nodes.
	 * @throws IllegalAccessException
	 *             if a field cannot be accessed in the class.
	 * @throws InstantiationException
	 *             if the script class cannot be instantiated.
	 */
	public TestSimulator(NetworkConfig network,
			Class<? extends NodeScript> scriptClass)
			throws InstantiationException, IllegalAccessException {
		this(network, scriptClass, null);
	}

	/**
	 * Creates a simulator, defining the network and node script from a config.
	 * Allows the passing of prescribed events.
	 * 
	 * @param network
	 *            the file containing the config of the network.
	 * @param scriptClass
	 *            the class run on all nodes.
	 * @param events
	 *            the events that are preset to happen.
	 * @throws IllegalAccessException
	 *             if a field cannot be accessed in the class.
	 * @throws InstantiationException
	 *             if the script class cannot be instantiated.
	 */
	public TestSimulator(NetworkConfig network,
			Class<? extends NodeScript> scriptClass, Set<PrescribedEvent> events)
			throws InstantiationException, IllegalAccessException {
		this.events = events;
		this.network = network;
		this.scriptClass = scriptClass;
		resetInner();
	}

	/**
	 * @return The node script objects for all nodes within the network.
	 */
	public synchronized Set<NodeScript> getNodes() {
		return nodes;
	}

	/**
	 * Simulates the model for a given number of timesteps, continuing from the
	 * last call.
	 * 
	 * @param timesteps
	 *            the number of timesteps to simulate for.
	 */
	public synchronized void simulateFor(long timesteps) {
		final long visibleStep = simRunner.getVisibleTimestep();
		final long startStep = (visibleStep < 0) ? 0 : visibleStep;
		simRunner.simulateUntil(startStep + timesteps);
	}

	/**
	 * Simulates the model until (but not including) the given timestep. Each
	 * subsequent call that is made between <code>reset()</code> calls must be
	 * made with increasing timesteps.
	 * 
	 * @param timestep
	 *            the timestep at which the simulation will continue until.
	 */
	public synchronized void simulateTo(long timestep) {
		simRunner.simulateUntil(timestep);
	}

	/**
	 * Returns the simulation to timestep 0.
	 */
	public synchronized void reset() {
		try {
			resetInner();
		} catch (InstantiationException e) {
			throw new IllegalStateException(
					"Could not re-instantiate already simulated class.");
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(
					"Could not access field in already simulated class.");
		}
	}

	private synchronized void resetInner() throws InstantiationException,
			IllegalAccessException {
		this.simRunner = new PausableSimulationRunner(new Network(scriptClass,
				network, false), Long.MAX_VALUE, events);

		// Create a collection of the nodescript objects from the nodes in sim.
		nodes = new HashSet<NodeScript>(network.nodeCount());
		for (Node node : simRunner.network().nodes()) {
			nodes.add(node.getScript());
		}
	}

	/**
	 * @return The current timestep of the simulation.
	 */
	public synchronized long getTimestep() {
		return simRunner.getVisibleTimestep();
	}
}
