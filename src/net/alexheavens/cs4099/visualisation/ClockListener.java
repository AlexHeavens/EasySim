package net.alexheavens.cs4099.visualisation;

import java.util.EventListener;

public interface ClockListener extends EventListener {

	public void onTimestepEvent(ClockTimestepEvent event);
	
	public void onStateChange(ClockStateEvent event);

	public void onTickEvent(ClockTickEvent event);
	
	public void onSpeedChange(ClockSpeedEvent event);
	
}
