package net.alexheavens.cs4099.simulation;

import java.util.Set;

import net.alexheavens.cs4099.network.Network;

public class MockSimulationRunner extends SimulationRunner {

	public MockSimulationRunner(Network net, long simLength) {
		super(net, simLength);
	}

	public MockSimulationRunner(Network net, long simLength,
			Set<PrescribedEvent> events) {
		super(net, simLength, events, 0);
	}

	public void setSimState(SimulationState s) {
		simState = s;
	}

	public void setTimestep(long eventTimestep) {
		timestep = eventTimestep;
	}

	public IEventController eventController() {
		return eventController;
	}

}
