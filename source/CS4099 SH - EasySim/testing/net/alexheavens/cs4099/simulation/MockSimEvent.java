package net.alexheavens.cs4099.simulation;

public class MockSimEvent extends SimulationEvent {

	private int priority;

	private boolean eventProcessed;
	private boolean simProcessed;

	public MockSimEvent(long eventTime) {
		super(eventTime);
		eventProcessed = false;
		simProcessed = false;
		priority = 0;
	}

	public boolean eventProcessed() {
		return eventProcessed;
	}

	public boolean simProcessed() {
		return simProcessed;
	}

	public void process(IEventController controller) {
		eventProcessed = true;
	}

	public void process(SimulationRunner runner) {
		simProcessed = true;
	}

	public int priority() {
		return priority;
	}

	public void setPriority(int p) {
		priority = p;
	}

	public SimEventType getEventType() {
		return null;
	}

}
