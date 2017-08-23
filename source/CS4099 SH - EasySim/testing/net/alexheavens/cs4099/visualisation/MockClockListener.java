package net.alexheavens.cs4099.visualisation;

import java.util.ArrayList;

public class MockClockListener implements ClockListener {

	private final ArrayList<ClockEvent> clockEvents;

	public MockClockListener() {
		clockEvents = new ArrayList<ClockEvent>();
	}

	public void onTimestepEvent(ClockTimestepEvent event) {
		clockEvents.add(event);
	}

	public void onStateChange(ClockStateEvent event) {
		clockEvents.add(event);
	}

	public ClockEvent getEvent(int index) {
		return clockEvents.get(index);
	}

	public int getEventCount() {
		return clockEvents.size();
	}

	public void onTickEvent(ClockTickEvent tickEvent) {
		clockEvents.add(tickEvent);
	}

	public void onSpeedChange(ClockSpeedEvent event) {
	}

}
