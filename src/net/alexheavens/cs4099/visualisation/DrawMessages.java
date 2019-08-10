package net.alexheavens.cs4099.visualisation;

import java.util.HashMap;
import java.util.HashSet;

import net.sf.json.JSONObject;

import prefuse.action.Action;

public class DrawMessages extends Action implements ClockListener {

	private long timestep;
	private int tick;
	private int ticksPerTimestep;
	private HashMap<Long, HashSet<JSONObject>> messages;

	public DrawMessages(HashMap<Long, HashSet<JSONObject>> messages) {
		this.messages = messages;
		timestep = 0;
		tick = 0;
		ticksPerTimestep = -1;
	}

	public void run(double frac) {
		HashSet<JSONObject> messagesInTransit = messages.get(timestep);
		if (messagesInTransit != null)
			for (JSONObject message : messagesInTransit) {
				System.out.println("messageRatio " + getMessageRatio(message));
			}
	}

	public void onTimestepEvent(ClockTimestepEvent event) {
		this.timestep = event.getTimestep();
	}

	public void onStateChange(ClockStateEvent event) {
	}

	public void onTickEvent(ClockTickEvent tickEvent) {
		this.tick = tickEvent.getTick();
	}

	private float getMessageRatio(JSONObject message) {
		long messageStart = message.getLong("sentAt");
		long messageEnd = message.getLong("arrivedAt");
		long messageDuration = messageEnd - messageStart;
		System.out.println("s " + messageStart + " e " + messageEnd + " t " + ticksPerTimestep);
		return ((float) (((timestep - messageStart) * ticksPerTimestep) + tick))
				/ ((float) (messageDuration * ticksPerTimestep));

	}

	public void onSpeedChange(ClockSpeedEvent event) {
		ticksPerTimestep = event.getTicksPerTimestep();
	}

}
