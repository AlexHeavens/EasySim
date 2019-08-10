package net.alexheavens.cs4099.testframework;

import java.util.Set;

import net.alexheavens.cs4099.network.configuration.NetworkConfig;
import net.alexheavens.cs4099.simulation.PrescribedEvent;
import net.alexheavens.cs4099.usercode.NodeScript;

public class MockTestSimulator extends TestSimulator {

	public MockTestSimulator(NetworkConfig network,
			Class<? extends NodeScript> scriptClass, Set<PrescribedEvent> events)
			throws InstantiationException, IllegalAccessException {
		super(network, scriptClass, events);
	}

	public PausableSimulationRunner getRunner(){
		return simRunner;
	}
	
}
