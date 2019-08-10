package net.alexheavens.cs4099.simulation;

/**
 * 
 * @author Alexander Heavens
 * 
 *         Note: this class has a natural ordering that is inconsistent with
 *         equals.
 */
public abstract class SimulationEvent implements ISimulationEvent,
		Comparable<SimulationEvent> {

	public final static String TYPE_TAG = "eventType";
	public final static String TIME_TAG = "timestep";
	
	protected long timestep;
	private static final int DEFAULT_PRIORITY = 5;

	public SimulationEvent(long eventTime) {
		if (eventTime != CURRENT_TIMESTEP && eventTime < 0)
			throw new IllegalArgumentException(NEGATIVE_TIMESTEP);
		timestep = eventTime;
	}

	public long getTimestep() {
		return timestep;
	}

	public void markWithTimestep(long eventTimestep) {
		if (timestep != CURRENT_TIMESTEP)
			throw new IllegalStateException(
					"Attempted to mark an event with a timestep when it already has one.");
		if (eventTimestep < 0)
			throw new IllegalArgumentException(
					"Attempted to mark timestep of event with invalid number: "
							+ eventTimestep);
		timestep = eventTimestep;
	}

	public int compareTo(SimulationEvent event) {
		long diff = (timestep - event.timestep);
		if (diff > 0l)
			return 1;
		else if (diff < 0l)
			return -1;
		else
			return 0;
	}

	public void process(SimulationRunner runner) {
		throw new IllegalStateException(
				"Default low prioirty event was processed by a SimulationRunner.");
	}

	public int priority() {
		return DEFAULT_PRIORITY;
	}

}
