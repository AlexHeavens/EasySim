package net.alexheavens.cs4099.simulation;

/**
 * Marks the occurrence, or expected occurrence, of an event within a simulated
 * network. Note that this is reserved to simulated events (the sending or
 * receiving of a message, for example). Simulation "meta-events" (such as the
 * tick of the simulation clock).
 * 
 * @author Alexander Heavens
 * @version 1.0
 */
public interface ISimulationEvent{

	public final static String NEGATIVE_TIMESTEP = "occurence timestep less that zero";

	/**
	 * If the timestep of the event has this value, it should be marked with the
	 * current timestep as soon as it is available.
	 */
	public final static long CURRENT_TIMESTEP = -1;

	/**
	 * @return The timestep that the event will, or has, occur at.
	 */
	public long getTimestep();

	/**
	 * Marks a node with a timestep. Note that this can only be called once on
	 * events that are marked as CURRENT_TIMESTEP.
	 * 
	 * @throws IllegalStateException
	 *             if called on an event that has already been marked.
	 */
	public void markWithTimestep(long timestep);

	/**
	 * This method enacts the event's behaviour upon the system. The
	 * <code>EventController</code> is passed to allow new events to be
	 * scheduled as a result of one occurring.
	 * 
	 * @param controller
	 *            the event controller that schedules events across the current
	 *            simulation.
	 */
	public void process(IEventController controller) throws DeadNodeException;

	/**
	 * Defines the actions that should occur should an event be processed by a
	 * simulation runner.
	 * 
	 * @param runner
	 *            the runner that will process the event.
	 */
	public void process(SimulationRunner runner);

	/**
	 * @return The priority of the event as a positive integer, 0 being the most
	 *         important.
	 */
	public int priority();
	
	/**
	 * For JSON conversion.
	 * 
	 * @return Event type as enum.
	 */
	public SimEventType getEventType();

}
