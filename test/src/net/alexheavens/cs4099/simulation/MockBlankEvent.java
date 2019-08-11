package net.alexheavens.cs4099.simulation;

public class MockBlankEvent extends SimulationEvent {
	
	public MockBlankEvent(long eventTime) {
		super(eventTime);
	}

	public void process(IEventController controller) {

	}

	public SimEventType getEventType() {
		return null;
	}

}
