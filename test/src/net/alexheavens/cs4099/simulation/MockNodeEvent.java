package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.INodeImpl;

public class MockNodeEvent extends NodeEvent {

	private static final int PRIORITY = IEventController.EVENT_NOTIFY_THRESHOLD;

	private int simProcessedCount;
	private int eventProcessedCount;

	public MockNodeEvent(long eventTime, INodeImpl node) {
		super(eventTime, node);
		simProcessedCount = 0;
		eventProcessedCount = 0;
	}

	public void process(IEventController controller) {
		eventProcessedCount++;
	}

	public void process(SimulationRunner runner) {
		simProcessedCount++;
	}

	public int simProcessedCount() {
		return simProcessedCount;
	}

	public int eventProcessedCount() {
		return eventProcessedCount;
	}

	public int priority() {
		return PRIORITY;
	}

	public SimEventType getEventType() {
		return null;
	}

}
