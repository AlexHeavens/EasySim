package net.alexheavens.cs4099.simulation;

import java.util.Observable;

import java.util.Observer;
import java.util.Set;

import net.alexheavens.cs4099.concurrent.LeaderBarrier;
import net.alexheavens.cs4099.network.Network;
import net.alexheavens.cs4099.network.Node;

public class SimulationRunner implements Observer {

	/**
	 * The value of the time step before the simulation has run.
	 */
	public static final int TIMESTEP_NOT_START = -2;
	public static final int TIMESTEP_END = -1;

	protected LeaderBarrier pauseBarrier;
	protected long timestep;
	protected volatile SimulationState simState;

	protected final EventController eventController;
	private final long length;
	private final Network network;
	private final Thread simThread;
	private final SimulationProfiler profiler;

	public SimulationRunner(Network net, long simLength) {
		this(net, simLength, null, 0);
	}

	public SimulationRunner(Network net, long simLength,
			Set<? extends PrescribedEvent> events, long nodeTimeout) {
		if (net == null)
			throw new IllegalArgumentException(
					"Cannot create a simulation runner with a null network.");
		if (simLength < 1)
			throw new IllegalArgumentException(
					"Cannot create a simulation runner that is less than 1 timestep in length.");

		profiler = new SimulationProfiler(net.nodeCount(), nodeTimeout);

		timestep = TIMESTEP_NOT_START;
		length = simLength;
		network = net;
		simThread = new Thread() {
			public void run() {
				simulateUntil(length);
			}
		};
		simState = SimulationState.PRE_SIMULATION;
		pauseBarrier = new LeaderBarrier(simThread, network.nodeCount());

		// Have the EventController observe events from Nodes and observe
		// priority events from it.
		eventController = new EventController(simLength);
		eventController.addObserver(this);
		for (Node node : network.nodes()) {
			node.addObserver(eventController);
		}

		// Prepare all prescribed events and add to the event controller.
		if (events != null) {
			for (PrescribedEvent event : events) {
				event.prepare(this);
				eventController.scheduleEvent(event, false);
			}
		}
	}

	/**
	 * @return The timestep that the simulation is currently at, 0 if simulation
	 *         has not started, -1 if simulation has ended.
	 */
	public long getVisibleTimestep() {
		return timestep;
	}

	/**
	 * @return The state of simulation.
	 */
	public SimulationState simulationState() {
		return simState;
	}

	protected void advanceTimestepTo(long newTimestep) {

		if (newTimestep == IEventController.NO_EVENTS_TIMESTEP)
			newTimestep = length;

		// State checks.
		if (simState != SimulationState.SIMULATING)
			throw new IllegalStateException(
					"Unable to advance timestep when out of simulation.");
		if (newTimestep < 0)
			throw new IllegalArgumentException(
					"Attempted to move simulation to invalid timestep: "
							+ newTimestep);
		if (newTimestep > length)
			newTimestep = length;
		if (newTimestep <= timestep)
			throw new IllegalArgumentException(
					"Attempted to advance to a past timestep.");
		if ((timestep < 0 || timestep > length)
				&& timestep != TIMESTEP_NOT_START)
			throw new IllegalStateException("Invalid timestep: " + timestep);

		timestep = newTimestep;
	}

	/**
	 * Simulates the constructed network.
	 * 
	 * @return A log of the events that occurred during simulation and timing
	 *         information of each node (id) to the time taken per timestep.
	 */
	public SimulationResults simulate() {

		simThread.start();
		try {
			simThread.join();
		} catch (InterruptedException e) {
			throw new IllegalStateException();
		}

		for (Node node : network.nodes()) {
			node.halt();
		}

		pauseBarrier.resumeAll();
		
		timestep = TIMESTEP_END;
		simState = SimulationState.POST_SIMULATION;

		return new SimulationResults(eventController.getEventLog(),
				profiler.getTimeStepProcessIdMap());
	}

	private void setupNodes() {
		for (Node node : network.nodes()) {
			node.simulate(pauseBarrier, profiler);
		}
	}

	protected synchronized void simulateUntil(long stopStep) {

		if (simState == SimulationState.PRE_SIMULATION) {
			simState = SimulationState.SETUP;
			profiler.incrementTimestep(0);
			setupNodes();
			simState = SimulationState.SIMULATING;
			pauseBarrier.waitThreadUnInterruptedly();
			advanceTimestepTo(eventController.nextEventTimestep());
		}

		long nextTimestep = eventController.nextEventTimestep();
		while (timestep < stopStep) {
			do {
				eventController.processEvent();
				pauseBarrier.waitThreadUnInterruptedly();
				nextTimestep = eventController.nextEventTimestep();
			} while (nextTimestep == timestep);

			advanceTimestepTo(nextTimestep);
			if (nextTimestep != TIMESTEP_END)
				profiler.incrementTimestep(nextTimestep);
		}

		pauseBarrier.waitThreadUnInterruptedly();

	}

	public long simulationLength() {
		return length;
	}

	public Network network() {
		return network;
	}

	public void update(Observable trigger, Object eventObject) {
		if (!(eventObject instanceof ISimulationEvent))
			throw new IllegalArgumentException(
					"Passed a non event to SimulationRunner in update.");
		ISimulationEvent event = (ISimulationEvent) eventObject;
		if (event.getTimestep() != timestep && timestep != TIMESTEP_NOT_START)
			throw new IllegalStateException(
					"ISimulationEvent with a timestep other than the current passed to SimulationRunner.");
		event.process(this);
	}

}
