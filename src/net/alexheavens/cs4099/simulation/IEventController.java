package net.alexheavens.cs4099.simulation;

import java.util.Observer;

/**
 * The IEventController interface specifies the required functionality to
 * maintaining an event queue. Classes that implement this are advised to use a
 * priority queue to maintain of SimulationEvents, to maintain quick scheduling
 * and processing of events. As many Node threads may be running at once, any
 * data structure used to maintain these events must allow concurrent access.
 * 
 * The controller is
 * <code>Observable<code> and will notify <code>Observers</code> of any
 * <code>ISimulationEvent</code> that has at least a priority of
 * <code>EVENT_NOTIFY_THRESHOLD</code> on the processing of that event.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @see java.util.concurrent.PriorityBlockingQueue
 */
public interface IEventController extends Observer {

	/**
	 * The value returned for the next time-step if there are no existing events
	 * within the event queue.
	 */
	public static final long NO_EVENTS_TIMESTEP = -1;

	/**
	 * The minimum level of priority required for the controller to notify any
	 * observers of an event on the processing of the event.
	 */
	public static final int EVENT_NOTIFY_THRESHOLD = 3;

	/**
	 * Schedules an event to be processed in a future simulation timestep.
	 * 
	 * @param event
	 *            the event to be scheduled.
	 */
	public void scheduleEvent(ISimulationEvent event);

	/**
	 * Removes the earliest event from the event queue and processes it.
	 * 
	 * @throws IllegalStateException
	 *             if no event is waiting.
	 */
	public void processEvent();

	/**
	 * @return The time-step of occurrence for the earliest event in the event
	 *         queue if their is a remaining event, otherwise
	 *         <code>NO_EVENTS_TIMESTEP</code> (-1).
	 */
	public long nextEventTimestep();

	/**
	 * @return The timestep that the event controller is currently processing
	 *         events for.
	 */
	public long currentTimestep();

	/**
	 * Schedules an event to be processed in a future simulation timestep,
	 * marking whether the events should be processed immediately if its
	 * timestep matches the controller's.
	 * 
	 * @param event
	 *            the event to be scheduled.
	 * @param immediateProcess
	 *            if the event should be processed immediately when possible.
	 */
	public void scheduleEvent(ISimulationEvent event, boolean immediateProcess);

}
