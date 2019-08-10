package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.INodeImpl;

public class NodePauseEvent extends NodeEvent {

	private final long pauseTime;

	public NodePauseEvent(long pauseDuration, INodeImpl node) {
		super(ISimulationEvent.CURRENT_TIMESTEP, node);
		if (pauseDuration < 1)
			throw new IllegalArgumentException(
					"Created NodePauseEvent with a pause duration of less than 1 timestep.");
		pauseTime = pauseDuration;
	}

	public void process(IEventController controller) {
		NodeUnpauseEvent unpauseEvent = new NodeUnpauseEvent(
				controller.currentTimestep() + pauseTime, eventNode);
		controller.scheduleEvent(unpauseEvent);
	}

	public long getPauseTime() {
		return pauseTime;
	}

	public SimEventType getEventType() {
		return SimEventType.NODE_PAUSE;
	}

}
