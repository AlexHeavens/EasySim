package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.ILink;
import net.alexheavens.cs4099.network.ILinkImpl;
import net.alexheavens.cs4099.network.IMessageImpl;
import net.alexheavens.cs4099.network.MockMessage;
import net.alexheavens.cs4099.network.MockSimObserver;
import net.alexheavens.cs4099.network.MockUserNode;
import net.alexheavens.cs4099.network.Network;
import net.alexheavens.cs4099.network.StringMessage;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.INetworkConfigFactory;
import net.alexheavens.cs4099.network.configuration.NetworkConfigFactory;
import net.alexheavens.cs4099.usercode.MockNodeScript;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

public class EventControllerTest {

	private static final long SIM_LENGTH = 1000;

	private EventController testController;
	private MockSimulationRunner mockRunner;
	private Network testNet;
	private MockUserNode mockNode;

	@Before
	public void setup() {
		INetworkConfigFactory configFactory = new NetworkConfigFactory();
		INetworkConfig testConfig = configFactory.createTreeNetwork(3, 5);
		try {
			testNet = new Network(MockNodeScript.class, testConfig, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mockRunner = new MockSimulationRunner(testNet, SIM_LENGTH);
		testController = new EventController(SIM_LENGTH);

		// Have the controller ready to perceive updates from a test Node.
		mockNode = new MockUserNode(0);
		mockNode.addObserver(testController);
	}

	@Test
	public void testControllerCreationValid() {
		assertEquals(IEventController.NO_EVENTS_TIMESTEP,
				testController.nextEventTimestep());
	}

	@Test
	/**
	 * Test that events that are scheduled with the current timestep are
	 * processed immediately.
	 * 
	 * This is important as certain events must be processed immediately to
	 * allow the timestep to advance.
	 */
	public void testScheduleEventStepCurrent() {

		// Schedule an event to occur at timestep 0, the current timestep.
		final long eventTimestep = 0;
		MockSimEvent mockEvent = new MockSimEvent(eventTimestep);
		testController.scheduleEvent(mockEvent);

		// Check that the event was processed immediately.
		assertTrue(mockEvent.eventProcessed());
	}

	@Test
	public void testScheduleEventStepFifty() {

		// Schedule an event to occur at time-step 50.
		final long eventTimestep = 50;
		MockSimEvent mockEvent = new MockSimEvent(eventTimestep);
		testController.scheduleEvent(mockEvent);

		// Check that we are expecting the event.
		assertEquals(eventTimestep, testController.nextEventTimestep());
		assertFalse(mockEvent.eventProcessed());
	}

	@Test
	public void testProcessEventStepFifty() {

		// Schedule an event to occur at time-step 50.
		final long eventTimestep = 50;
		MockSimEvent mockEvent = new MockSimEvent(eventTimestep);
		testController.scheduleEvent(mockEvent);

		mockRunner.setTimestep(eventTimestep);
		testController.processEvent();

		// Check that the event has been processed and no other event is
		// expected.
		assertTrue(mockEvent.eventProcessed());
		assertEquals(IEventController.NO_EVENTS_TIMESTEP,
				testController.nextEventTimestep());
	}

	@Test
	public void testStressScheduleEvents() {

		// Create a lot of events, all within the simulation length.
		final int nTestEvents = 20000;
		MockSimEvent[] testEvents = new MockSimEvent[nTestEvents];
		for (int i = 0; i < nTestEvents; i++) {
			testEvents[i] = new MockSimEvent(i % SIM_LENGTH);
		}

		// Schedule the first 100 events in reverse order to test the time-step
		// best.
		for (int i = (int) (SIM_LENGTH - 1); i > 0; i--) {
			testController.scheduleEvent(testEvents[i]);
			assertEquals(i, testController.nextEventTimestep());
		}

		// 0 is a special case, as it is automatically processed.
		testController.scheduleEvent(testEvents[0]);
		assertTrue(testEvents[0].eventProcessed());

		// Schedule the rest, checking that the next time-step remains
		// unchanged.
		for (int i = (int) SIM_LENGTH; i < nTestEvents; i++) {
			testController.scheduleEvent(testEvents[i]);
			if (testEvents[i].getTimestep() == 0)
				assertTrue(testEvents[i].eventProcessed());
			assertEquals(1, testController.nextEventTimestep());
		}
	}

	@Test
	public void testProcessDistantTimestep() {

		// Schedule an event for time-step 50.
		final long eventTimestep = 50;
		MockSimEvent event = new MockSimEvent(eventTimestep);
		testController.scheduleEvent(event);

		// Attempt to process that event whilst the simulation time-step is 0.
		testController.processEvent();

		// Check that the event has been processed and no other event is
		// expected.
		assertTrue(event.eventProcessed());
		assertEquals(IEventController.NO_EVENTS_TIMESTEP,
				testController.nextEventTimestep());
	}

	@Test
	public void testStressProcessEvents() {

		// Create a lot of events, all within the simulation length.
		final int nTestEvents = 500;
		MockSimEvent[] testEvents = new MockSimEvent[nTestEvents];
		for (int i = 0; i < nTestEvents; i++) {
			long eventTime = (long) ((float) i / (float) nTestEvents)
					* ((SIM_LENGTH - 1));
			testEvents[i] = new MockSimEvent(eventTime);
		}

		// Schedule the events.
		for (int i = 0; i < nTestEvents; i++) {
			testController.scheduleEvent(testEvents[i]);
		}

		// Process these events, assuming that all 0 timestep events have
		// already been processed.
		for (int i = 0; i < nTestEvents; i++) {
			if (testEvents[i].getTimestep() == 0) {
				assertTrue(testEvents[i].eventProcessed());
			} else {
				testController.processEvent();

				// Check that the timestep of the next event is as expected.
				final long expectedTimestep = (long) ((float) i / (float) nTestEvents)
						* ((SIM_LENGTH - 1));

				if (i != nTestEvents - 1)
					assertEquals(expectedTimestep,
							testController.nextEventTimestep());
				else
					assertEquals(IEventController.NO_EVENTS_TIMESTEP,
							testController.nextEventTimestep());

				// Check that the correct number of events have been processed.
				int eventsProcessed = 0;
				for (int j = 0; j < nTestEvents; j++) {
					final long eventTime = testEvents[j].getTimestep();
					if (eventTime > expectedTimestep) {
						assertFalse(testEvents[j].eventProcessed());
					} else if (eventTime < expectedTimestep) {
						assertTrue(testEvents[i].eventProcessed());
						eventsProcessed++;
					} else if (testEvents[j].eventProcessed()) {
						eventsProcessed++;
					}
				}
				assertEquals(i + 1, eventsProcessed);
			}
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testProcessNoEvent() {
		testController.processEvent();
	}

	@Test
	public void testScheduleEventInPast() {

		// Bring our simulation up to a point.
		final long timestep = 50;
		MockSimEvent eventA = new MockSimEvent(timestep);
		testController.scheduleEvent(eventA);
		testController.processEvent();

		// Attempt to schedule past event.
		MockSimEvent eventB = new MockSimEvent(timestep - 1);

		try {
			testController.scheduleEvent(eventB);
			fail("Able to schedule event in the past.");
		} catch (IllegalStateException e) {
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testScheduleNullEvent() {
		testController.scheduleEvent(null);
	}

	@Test(expected = IllegalStateException.class)
	public void testProcessLimit() {
		MockSimEvent event = new MockSimEvent(SIM_LENGTH);
		testController.scheduleEvent(event);
		testController.processEvent();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidProcessLimit() {
		new EventController(0);
	}

	@Test
	/**
	 * Test that an event marked as "CURRENT_TIMESTEP" will be remarked to the
	 * current timestep by the event controller. This is expected for all nodes
	 * and links during simulation as these do not have the means to keep track
	 * of the current timestep.
	 */
	public void testScheduleEventTimestepMarking() {

		// Prepare the controller and event.
		final long timestep = 50;
		MockSimEvent testEvent = new MockSimEvent(timestep);

		testController.scheduleEvent(testEvent);

		// Check that the event has been marked with the appropriate timestep.
		assertEquals(timestep, testEvent.getTimestep());
		assertEquals(timestep, testController.nextEventTimestep());
	}

	@Test
	/**
	 * Tests if events whose <code>priority</code> meet the
	 * <code>EVENT_NOTIFY_THRESHOLD</code> are indeed passed onto observers of
	 * the EventController once processed.
	 */
	public void testPriorityEventsPassedToObservers() {

		// Prepare our event and observer.
		MockSimEvent event = new MockSimEvent(40) {
			public int priority() {
				return IEventController.EVENT_NOTIFY_THRESHOLD;
			}
		};
		MockSimObserver observer = new MockSimObserver();
		testController.addObserver(observer);
		testController.scheduleEvent(event);

		// Check that no update has occurred yet.
		assertEquals(0, observer.updateCount());
		assertNull(observer.lastObservable());
		assertNull(observer.lastUpdate());

		testController.processEvent();

		// Check that the appropriate event was observed by the observer.
		assertEquals(1, observer.updateCount());
		assertEquals(testController, observer.lastObservable());
		assertEquals(event, observer.lastUpdate());
	}

	@Test
	/**
	 * Tests whether Objects observed by the EventController (e.g. Nodes) can
	 * schedule events by notifying them as an Observable.
	 */
	public void testScheduleEventViaObserverNotify() {

		final long timestep = 50;
		MockSimEvent event = new MockSimEvent(timestep);

		// Check that no event has been noticed yet by the controller.
		assertEquals(IEventController.NO_EVENTS_TIMESTEP,
				testController.nextEventTimestep());

		mockNode.triggerEvent(event);

		// Check that the event was scheduled accordingly.
		assertFalse(event.eventProcessed());
		testController.processEvent();
		assertTrue(event.eventProcessed());
	}

	@Test
	/**
	 * Tests if an EventController correctly marks an event with the current
	 * timestep if it is scheduled with CURRENT_TIMESTEP. This is important to
	 * allow Nodes and other objects that send ISimulationEvents to the
	 * controller to send these events without knowledge of the current
	 * timestep.
	 */
	public void testScheduleMarkCurrentEvents() {

		// Update our controller to some timestep.
		final long timestep = 50;
		MockSimEvent prevEvent = new MockSimEvent(timestep);
		MockSimEvent futureEvent = new MockSimEvent(SIM_LENGTH - 1);
		testController.scheduleEvent(prevEvent);
		testController.scheduleEvent(futureEvent);
		testController.processEvent();

		// Have the controller ready to perceive updates from a test Node.
		MockSimEvent event = new MockSimEvent(ISimulationEvent.CURRENT_TIMESTEP);

		// Check that the event timestep has not changed.
		assertFalse(event.eventProcessed());
		assertEquals(ISimulationEvent.CURRENT_TIMESTEP, event.getTimestep());

		mockNode.triggerEvent(event);

		// Check that the event has the current timestep.
		assertTrue(event.eventProcessed());
		assertEquals(timestep, event.getTimestep());
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Tests if the passing of something other than an ISimulationEvent throws
	 * an IllegalArgumentException.
	 * 
	 * This is important as the observer notify channel is reserved for
	 * ISimulationEvents only.
	 */
	public void testNotifyInvalidObject() {
		mockNode.triggerEvent(new Object());
	}

	@Test
	/**
	 * Tests to ensure that none priority messages (those which do not meet the
	 * priorty threshold) are not passed on to EventController observers.
	 */
	public void testNotifyNonPriorityMessage() {

		// Prepare our event and observer.
		MockSimEvent event = new MockSimEvent(50);
		event.setPriority(IEventController.EVENT_NOTIFY_THRESHOLD + 1);
		MockSimObserver observer = new MockSimObserver();
		testController.addObserver(observer);
		testController.scheduleEvent(event);

		// Check that no update has occurred yet.
		assertFalse(event.eventProcessed());
		assertFalse(event.simProcessed());

		testController.processEvent();

		// Check that the event was not passed to the observer.
		assertTrue(event.eventProcessed());
		assertFalse(event.simProcessed());
	}

	@Test
	public void testProcessMessageSentEvent() {

		// Prepare to send a message from one node to another.
		MockUserNode neighbour = new MockUserNode(100);
		mockNode.addNeighbour(neighbour, 1);
		final String messageTag = "Grab my wizard's staff!";
		final String messageData = "Ouch! Not that hard.";
		StringMessage testMessage = new StringMessage(messageTag, messageData);

		mockNode.setSimulationState(SimulationState.SIMULATING);
		mockNode.send(neighbour, testMessage);

		// Check that an appropriate message event was created.
		assertEquals(1, testController.eventQueue.size());
		ISimulationEvent event = testController.eventQueue.peek();
		assertTrue(event instanceof MessageArrivalEvent);
		MessageArrivalEvent arrivalEvent = (MessageArrivalEvent) event;
		IMessageImpl<?> message = arrivalEvent.message();
		assertEquals(mockNode, message.source());
		assertEquals(neighbour, message.target());
		assertEquals(messageTag, message.getTag());
		assertEquals(messageData, message.getData());
		assertEquals(0, message.getSentAt());

		// Check that the timing of the message is as expected.
		assertEquals(0, arrivalEvent.message().getSentAt());
		ILink link = mockNode.neighbourLink(neighbour);
		assertEquals(link.latency(), arrivalEvent.getTimestep());
	}

	@Test
	public void testProcessMessageArrivalEvent() {
		MockUserNode neighbour = new MockUserNode(100);
		mockNode.addNeighbour(neighbour, 1);
		MockMessage testMessage = new MockMessage(neighbour, mockNode, 0);
		MessageArrivalEvent arrivalEvent = new MessageArrivalEvent(testMessage,
				1);

		testController.scheduleEvent(arrivalEvent);
		testController.processEvent();

		ILinkImpl link = (ILinkImpl) mockNode.neighbourLink(neighbour);
		assertEquals(testMessage, link.popMessage(mockNode));
		assertEquals(1, testMessage.getArrivedAt());
	}

	@Test
	public void testLogEventsOnProcess() {
		MockUserNode target = new MockUserNode(43);
		mockNode.addNeighbour(target, 1);
		final long EVENT_TIME = 435;
		MockMessage testMessage = new MockMessage(mockNode, target,
				IMessageImpl.TIMESTEP_NOT_SENT);
		MessageSentEvent event = new MessageSentEvent(testMessage, EVENT_TIME);
		testController.scheduleEvent(event);
		testController.processEvent();

		JSONObject log = testController.getEventLog().toJSONObject();
		JSONObject expEventJSON = (JSONObject) JSONSerializer.toJSON(event);
		expEventJSON.element("messageId", 0);
		JSONObject expMsgJSON = (JSONObject) JSONSerializer.toJSON(testMessage);

		assertEquals(1, log.getJSONArray("events").size());
		assertEquals(1, log.getJSONArray("messages").size());
		assertEquals(expEventJSON, log.getJSONArray("events").getJSONObject(0));
		assertEquals(expMsgJSON, log.getJSONArray("messages").getJSONObject(0));
	}

	@Test
	/**
	 * Tests that node events are not processed if their node has failed.
	 */
	public void testIgnoreDeadNodeEvents() {
		mockNode.setSimulationState(SimulationState.POST_SIMULATION);
		NodeUnpauseEvent unpauseEvent = new NodeUnpauseEvent(50, mockNode);
		testController.scheduleEvent(unpauseEvent);
		testController.processEvent();
	}
}
