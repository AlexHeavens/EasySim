package net.alexheavens.cs4099.network;

import net.alexheavens.cs4099.concurrent.WaitRegistrar;
import net.alexheavens.cs4099.network.Node;
import net.alexheavens.cs4099.simulation.SimulationProfiler;
import net.alexheavens.cs4099.simulation.SimulationState;
import net.alexheavens.cs4099.usercode.NodeScript;

public class MockUserNode extends Node {

	private static final int N_NODES = 100;

	protected int setupCount = 0;
	protected int executedCount = 0;

	public MockUserNode() {
		super(new NodeScript() {
			
			@Override
			public void execute() {
			}
		});
		profiler = new SimulationProfiler(N_NODES);
		nodeThread = new Thread();
	}

	public MockUserNode(int id) {
		super(new NodeScript() {
			
			@Override
			public void execute() {
			}
		});
		machineId = id;
		profiler = new SimulationProfiler(N_NODES);
		nodeThread = new Thread();
	}

	public void setup() {
		setupCount++;
	}

	public void execute() {
		executedCount++;
	}

	public int executedCount() {
		return executedCount;
	}

	public int setupCount() {
		return setupCount;
	}

	public void triggerEvent(Object event) {
		setChanged();
		notifyObservers(event);
	}

	public void setSimulationState(SimulationState simulating) {
		super.setSimulationState(simulating);
	}

	public Thread getThread() {
		return nodeThread;
	}

	public void setWaitRegistrar(WaitRegistrar registrar) {
		this.waitRegistrar = registrar;
	}
}
