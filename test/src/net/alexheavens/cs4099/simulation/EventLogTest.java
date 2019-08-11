package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.alexheavens.cs4099.network.MockMessage;
import net.alexheavens.cs4099.network.MockUserNode;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.junit.Before;
import org.junit.Test;

public class EventLogTest {

	private EventLog testLog;
	private MockUserNode testSource;
	private MockUserNode[] testNeighbours;
	private MockMessage[] testMessages;
	private MessageSentEvent[] testEvents;

	@Before
	public void setup() {
		testLog = new EventLog();

		// Create some nodes to source our messages.
		final int nNeighbours = 100;
		int nextId = 0;
		testSource = new MockUserNode(nextId++);
		testNeighbours = new MockUserNode[nNeighbours];
		for (int i = 0; i < testNeighbours.length; i++) {
			testNeighbours[i] = new MockUserNode(nextId++);
			testSource.addNeighbour(testNeighbours[i], 1);
		}

		// Create some events from an arbitrary timestep.
		final long startTimestep = 50;
		testMessages = new MockMessage[testNeighbours.length];
		for (int i = 0; i < testMessages.length; i++) {
			testMessages[i] = new MockMessage(testSource, testNeighbours[i], i
					+ startTimestep);
		}

		testEvents = new MessageSentEvent[testMessages.length];
		for (int i = 0; i < testEvents.length; i++) {
			testEvents[i] = new MessageSentEvent(testMessages[i], startTimestep
					+ i);
		}

	}

	@Test
	/**
	 * Tests that the log is created successfully.
	 */
	public void testCreation() {

		assertEquals(0, testLog.events.size());
		assertEquals(0, testLog.messageKeyMap.size());
	}

	@Test
	/**
	 * Tests that adding several valid events is correctly reflected in the
	 * event log."
	 */
	public void testAddEventsValid() {

		for (int i = testEvents.length - 1; i >= 0; i--) {
			testLog.addEvent(testEvents[i]);
			assertEquals(testEvents.length - i, testLog.events.size());
		}

		for (int i = 0; i < testEvents.length; i++) {
			assertEquals(testEvents[i], testLog.events.poll());
		}

	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test to make sure null is not a valid event to log.
	 */
	public void testAddNullEvent() {
		testLog.addEvent(null);
	}

	@Test
	/**
	 * Test that on an event being stored, new messages are also stored.
	 */
	public void testStoreMessages() {
		for (int i = 0; i < testEvents.length; i++) {
			testLog.addEvent(testEvents[i]);
		}

		assertEquals(testMessages.length, testLog.messageKeyMap.size());
		for (int i = 0; i < testMessages.length; i++) {
			assertTrue(testLog.messageKeyMap.keySet().contains(testMessages[i]));
		}
	}

	@Test
	/**
	 * Tests that storing the same message twice causes no error but doesn't
	 * affect the message list.
	 */
	public void testStoreMessageTwice() {
		MessageSentEvent dupEvent = new MessageSentEvent(testMessages[0], 0);
		testLog.addEvent(testEvents[0]);
		testLog.addEvent(dupEvent);
		assertEquals(1, testLog.messageKeyMap.size());
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Tests that it is not possible to store an event twice, which is a sign of
	 * a coding error.
	 */
	public void testStoreEventTwice() {
		testLog.addEvent(testEvents[0]);
		testLog.addEvent(testEvents[0]);
	}

	@Test
	public void testToJSONValid() {
		for (int i = 0; i < testEvents.length; i++) {
			testLog.addEvent(testEvents[i]);
		}

		// Check that the events and messages lists are there.
		JSONObject logJSON = testLog.toJSONObject();

		assertTrue(logJSON.containsKey("events"));
		assertTrue(logJSON.containsKey("messages"));
		assertTrue(logJSON.get("events") instanceof JSONArray);
		assertTrue(logJSON.get("messages") instanceof JSONArray);
		JSONArray events = logJSON.getJSONArray("events");
		JSONArray messages = logJSON.getJSONArray("messages");
		assertEquals(testEvents.length, events.size());
		assertEquals(testMessages.length, messages.size());

		// For each event.
		for (int i = 0; i < events.size(); i++) {

			// Check that the message id is attached.
			JSONObject eventObj = events.getJSONObject(i);
			JSONObject storedEvent = (JSONObject) JSONSerializer
					.toJSON(testEvents[i]);
			storedEvent.element("messageId", eventObj.getInt("messageId"));
			assertEquals(storedEvent, eventObj);
			assertTrue(eventObj.containsKey("messageId"));
			assertTrue(eventObj.getInt("messageId") < messages.size());

			// Check that the message matches.
			JSONObject messageObj = (JSONObject) JSONSerializer
					.toJSON(testMessages[i]);
			JSONObject storedMessage = messages.getJSONObject(events
					.getJSONObject(i).getInt("messageId"));
			assertEquals(messageObj, storedMessage);
		}
	}

	@Test
	/**
	 * Test that the observation of the log does not tamper with the data in the
	 * log.
	 */
	public void testLogEventsNonDestructive() {
		for (int i = 0; i < testEvents.length; i++) {
			testLog.addEvent(testEvents[i]);
		}

		// Test by simply comparing two calls to the log.
		JSONObject logA = testLog.toJSONObject();
		JSONObject logB = testLog.toJSONObject();
		assertEquals(logA, logB);
	}

	@Test
	/**
	 * Test that we can retrieve the events that occurred in a particular
	 * timestep only.
	 */
	public void testGetEventsInTimestepValid() {

		// Add a smattering of events, varying in timestep.
		final long focusTimestep = 100;
		final Set<ISimulationEvent> timestepEvents = new HashSet<ISimulationEvent>();
		for (int i = 0; i < testEvents.length; i++) {
			testLog.addEvent(testEvents[i]);
			if (testEvents[i].getTimestep() == focusTimestep)
				timestepEvents.add(testEvents[i]);
		}

		// Add a focus of events on one timestep.
		final int nExtraTimestepEvents = 100;
		for (int i = 0; i < nExtraTimestepEvents; i++) {
			ISimulationEvent timestepEvent = new NodeKillEvent(focusTimestep, i);
			timestepEvents.add(timestepEvent);
			testLog.addEvent(timestepEvent);
		}

		// Check that the events retrieved for that step match up.
		final Set<ISimulationEvent> events = testLog
				.getEventsInTimestep(focusTimestep);
		assertEquals(timestepEvents.size(), events.size());
		for (ISimulationEvent focusEvent : timestepEvents) {
			assertTrue(events.contains(focusEvent));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test to check parameter timestep of call to getEventsInTimestep(). We
	 * should only ever expected to call this with a positive timestep.
	 */
	public void testGetInTimestepInvalid() {
		testLog.getEventsInTimestep(-1);
	}

}
