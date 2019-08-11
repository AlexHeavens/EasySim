package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import net.alexheavens.cs4099.network.MockMessage;
import net.alexheavens.cs4099.network.MockUserNode;
import net.alexheavens.cs4099.network.Node;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class EventJSONTest {

	private Node source, target;
	private MockMessage testMessage;
	private static final long EVENT_TIMESTEP = 435435;

	@Before
	public void setup() {
		source = new MockUserNode(34);
		target = new MockUserNode(9696);
		source.addNeighbour(target, 1);
		testMessage = new MockMessage(source, target, EVENT_TIMESTEP);
	}

	@Test
	public void testPauseEvent() {
		final long pauseDuration = 43243;
		NodePauseEvent pauseEvent = new NodePauseEvent(pauseDuration, source);

		JSONObject pauseObj = (JSONObject) JSONSerializer.toJSON(pauseEvent);
		assertEquals(4, pauseObj.size());
		assertEquals(SimEventType.NODE_PAUSE.name(), pauseObj.get("eventType"));
		assertEquals(pauseEvent.getPauseTime(), pauseObj.getLong("pauseTime"));
	}

	@Test
	public void testUnpauseEvent() {
		NodeUnpauseEvent pauseEvent = new NodeUnpauseEvent(EVENT_TIMESTEP,
				source);

		JSONObject pauseObj = (JSONObject) JSONSerializer.toJSON(pauseEvent);
		assertEquals(3, pauseObj.size());
		assertEquals(SimEventType.NODE_UNPAUSE.name(),
				pauseObj.get("eventType"));
	}

	@Test
	public void testMessageEvent() {

		MessageEvent messageEvent = new MessageEvent(testMessage,
				EVENT_TIMESTEP) {
			public void process(IEventController controller) {
			}

			public SimEventType getEventType() {
				return null;
			}

		};

		JSONObject messageObj = (JSONObject) JSONSerializer
				.toJSON(messageEvent);
		assertEquals(2, messageObj.size());
	}

	@Test
	public void testMessageSentEvent() {

		MessageSentEvent messageEvent = new MessageSentEvent(testMessage,
				EVENT_TIMESTEP);

		JSONObject messageObj = (JSONObject) JSONSerializer
				.toJSON(messageEvent);
		assertEquals(2, messageObj.size());
		assertEquals(SimEventType.MESSAGE_SENT.name(),
				messageObj.get("eventType"));
	}

	@Test
	public void testMessageArrivedEvent() {

		MessageArrivalEvent messageEvent = new MessageArrivalEvent(testMessage,
				EVENT_TIMESTEP);

		JSONObject messageObj = (JSONObject) JSONSerializer
				.toJSON(messageEvent);
		assertEquals(2, messageObj.size());
		assertEquals(SimEventType.MESSAGE_ARRIVAL.name(),
				messageObj.get("eventType"));
	}

	@Test
	public void testMessageReadEvent() {
		MessageReadEvent messageEvent = new MessageReadEvent(testMessage,
				EVENT_TIMESTEP);

		JSONObject messageObj = (JSONObject) JSONSerializer
				.toJSON(messageEvent);
		assertEquals(2, messageObj.size());
		assertEquals(SimEventType.MESSAGE_READ.name(),
				messageObj.get("eventType"));
	}

}
