package net.alexheavens.cs4099.simulation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import net.alexheavens.cs4099.network.IMessageImpl;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class EventLog implements IEventLog {

	protected PriorityBlockingQueue<ISimulationEvent> events;
	protected ConcurrentHashMap<IMessageImpl<?>, Integer> messageKeyMap;
	private int nextMessageId = 0;

	public EventLog() {
		events = new PriorityBlockingQueue<ISimulationEvent>();
		messageKeyMap = new ConcurrentHashMap<IMessageImpl<?>, Integer>();
	}

	public synchronized void addEvent(ISimulationEvent event) {
		if (event == null)
			throw new IllegalArgumentException("Attempted to log null event.");
		if (events.contains(event))
			throw new IllegalArgumentException(
					"Attempted to add an event to a log twice.");

		events.put(event);
		if (event instanceof MessageEvent) {
			MessageEvent messageEvent = (MessageEvent) event;
			if (!messageKeyMap.containsKey(messageEvent.message())) {
				messageKeyMap.put(messageEvent.message(), nextMessageId++);
			}
		}
	}

	public synchronized JSONObject toJSONObject() throws JSONException {

		PriorityBlockingQueue<ISimulationEvent> processedEvents = new PriorityBlockingQueue<ISimulationEvent>();
		JSONArray eventJSONArray = new JSONArray();

		final int nEvents = events.size();
		for (int i = 0; i < nEvents; i++) {
			ISimulationEvent event = events.poll();
			JSONObject eventObj = (JSONObject) JSONSerializer.toJSON(event);
			if (event instanceof MessageEvent) {
				MessageEvent messageEvent = (MessageEvent) event;
				eventObj.element("messageId",
						messageKeyMap.get(messageEvent.message()).intValue());
			}
			eventJSONArray.add(i, eventObj);
			processedEvents.add(event);
		}

		JSONArray messagesJSONArray = new JSONArray();
		for (int i = 0; i < messageKeyMap.size(); i++)
			messagesJSONArray.add(null);

		for (Entry<IMessageImpl<?>, Integer> keyMap : messageKeyMap.entrySet()) {
			JSONObject messageJSONObject = (JSONObject) JSONSerializer
					.toJSON(keyMap.getKey());
			messagesJSONArray.set(keyMap.getValue().intValue(),
					messageJSONObject);
		}

		JSONObject bean = new JSONObject();
		bean.element("events", eventJSONArray);
		bean.element("messages", messagesJSONArray);
		events = processedEvents;
		return bean;
	}
	
	@Override
	public Set<ISimulationEvent> getEventsInTimestep(long timestep) {

		if (timestep < 0)
			throw new IllegalArgumentException(
					"Attempted to get events in a negative timestep: "
							+ timestep);

		Iterator<ISimulationEvent> eventsIt = events.iterator();
		Set<ISimulationEvent> timeStepEvents = new HashSet<ISimulationEvent>();
		while (eventsIt.hasNext()) {
			ISimulationEvent nextEvent = eventsIt.next();
			if (nextEvent.getTimestep() == timestep)
				timeStepEvents.add(nextEvent);
		}
		return timeStepEvents;
	}

}
