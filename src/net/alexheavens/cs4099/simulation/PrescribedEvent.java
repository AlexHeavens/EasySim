package net.alexheavens.cs4099.simulation;

/**
 * Prescribed events can be passed to simulation before it has begun so that
 * they are scheduled to occur.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public interface PrescribedEvent extends ISimulationEvent {

	/**
	 * As PrescribedEvents are created without knowledge of the simulation
	 * network or setting, it may be necessary to imbue with such knowledge.
	 * 
	 * This method is guaranteed to be called prior to simulation for any
	 * Prescribed event passed to the simulator.
	 * 
	 * @param runner
	 *            the runner executing the simulation.
	 */
	public void prepare(SimulationRunner runner);
}
