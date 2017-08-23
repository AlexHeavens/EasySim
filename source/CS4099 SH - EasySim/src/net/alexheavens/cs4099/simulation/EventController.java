package net.alexheavens.cs4099.simulation;

import java.util.Observable;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Controls the processing of events during simulation.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class EventController extends Observable implements IEventController {

	protected PriorityBlockingQueue<ISimulationEvent> eventQueue;
	private IEventLog eventLog;
	private long currentTimestep;
	private long processLimitTimestep;

	/**
	 * Creates an EventController with an empty event queue. A limit is
	 * specified as to when the controller should expect to stop processing
	 * events with <code>processLimit</code>. However, events can still be
	 * scheduled for after this point, although they will never occur within the
	 * simulation.
	 * 
	 * @param processLimit
	 *            the timestep at which no further events can occur. Must be
	 *            greater than 0.
	 */
	public EventController(long processLimit) {
		if (processLimit < 1)
			throw new IllegalArgumentException("Invalid process limit: "
					+ processLimit);
		eventQueue = new PriorityBlockingQueue<ISimulationEvent>();
		currentTimestep = 0;
		processLimitTimestep = processLimit;
		eventLog = new EventLog();
	}

	public void scheduleEvent(ISimulationEvent event) {
		scheduleEvent(event, true);
	}

	@Override
	public void scheduleEvent(ISimulationEvent event, boolean immediateProcess) {

		if (event == null)
			throw new IllegalArgumentException(
					"Attempted to schedule null event.");

		if (event.getTimestep() == ISimulationEvent.CURRENT_TIMESTEP)
			event.markWithTimestep(currentTimestep);

		if (event.getTimestep() < currentTimestep)
			throw new IllegalStateException(
					"Attempted to schedule event before the current timestep.");
		if (event.getTimestep() == currentTimestep && immediateProcess) {
			processEvent(event);
		} else {
			eventQueue.add(event);
		}
	}

	public void processEvent() {
		if (eventQueue.size() == 0)
			throw new IllegalStateException(
					"Attempted to process event where none was waiting.");
		ISimulationEvent event = eventQueue.poll();
		processEvent(event);
	}

	private synchronized void processEvent(ISimulationEvent event) {

		currentTimestep = event.getTimestep();
		if (currentTimestep >= processLimitTimestep)
			throw new IllegalStateException(
					"Attempted to process event past process limit.");

		try {
			event.process(this);
			eventLog.addEvent(event);

			// If the event is important, pass it on to any observers.
			if (event.priority() <= EVENT_NOTIFY_THRESHOLD) {
				setChanged();
				notifyObservers(event);
			}
		} catch (DeadNodeException e) {
		}
	}

	public long nextEventTimestep() {
		return (eventQueue.size() > 0) ? eventQueue.peek().getTimestep()
				: NO_EVENTS_TIMESTEP;
	}

	public long currentTimestep() {
		return currentTimestep;
	}

	public void update(Observable sender, Object eventUpdate) {
		if (!(eventUpdate instanceof ISimulationEvent))
			throw new IllegalArgumentException(
					"Non ISimulationEvent passed as update to EventController.");
		ISimulationEvent event = (ISimulationEvent) eventUpdate;
		if (event.getTimestep() == ISimulationEvent.CURRENT_TIMESTEP)
			event.markWithTimestep(currentTimestep);
		scheduleEvent(event);
	}

	/**
	 * @return the log of events that controller keeps track of.
	 */
	public IEventLog getEventLog() {
		return eventLog;
	}
}
