package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.junit.Before;
import org.junit.Test;

public class SimulationEventTest {

	private SimulationEvent event;
	private final static long EVENT_TIME = 100l;

	@Before
	public void setup() {
		event = new MockSimEvent(EVENT_TIME);
	}

	@Test
	public void testValidCreation() {
		assertEquals(EVENT_TIME, event.getTimestep());
	}

	@Test
	public void testCreationInvalidTime() {
		try {
			new MockSimEvent(-2l);
			fail("Able to create simulation event with negative timestep.");
		} catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), ISimulationEvent.NEGATIVE_TIMESTEP);
		}
	}

	@Test
	public void testCompareToEventNormal() {
		final long eventBTime = 55l;
		final long eventCTime = EVENT_TIME;
		final long eventDTime = 125l;
		SimulationEvent eventB = new MockSimEvent(eventBTime);
		SimulationEvent eventC = new MockSimEvent(eventCTime);
		SimulationEvent eventD = new MockSimEvent(eventDTime);
		assertEquals(1, event.compareTo(eventB));
		assertEquals(0, event.compareTo(eventC));
		assertEquals(-1, event.compareTo(eventD));
		assertEquals(0, event.compareTo(event));
	}

	@Test
	public void testCompareToEventClose() {
		final long eventBTime = EVENT_TIME - 1;
		final long eventCTime = EVENT_TIME + 1;
		SimulationEvent eventB = new MockSimEvent(eventBTime);
		SimulationEvent eventC = new MockSimEvent(eventCTime);
		assertEquals(1, event.compareTo(eventB));
		assertEquals(-1, event.compareTo(eventC));
	}

	@Test
	public void testCompareToEventFar() {
		final long eventBTime = Long.MAX_VALUE;
		final long eventCTime = 0l;
		SimulationEvent eventB = new MockSimEvent(eventBTime);
		SimulationEvent eventC = new MockSimEvent(eventCTime);
		assertEquals(-1, event.compareTo(eventB));
		assertEquals(1, event.compareTo(eventC));
		assertEquals(1, eventB.compareTo(eventC));
		assertEquals(-1, eventC.compareTo(eventB));
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Tests if setting an event not marked with CURRENT_TIMESTEP throws
	 * IllegalStateException as expected. This is necessary to ensure that the
	 * timestep of an event is not set to an actual timestep more than once.
	 */
	public void testSetTimestepNonCurrent() {
		MockSimEvent event = new MockSimEvent(ISimulationEvent.CURRENT_TIMESTEP);
		event.markWithTimestep(5);
		event.markWithTimestep(5);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test if marking the timestep of an event to an invalid number does throw
	 * an IllegalArgumentException.
	 */
	public void testSetTimestepInvalid() {
		MockSimEvent event = new MockSimEvent(ISimulationEvent.CURRENT_TIMESTEP);
		event.markWithTimestep(-1);
	}

	@Test
	public void testJSONSerialise() {
		SimulationEvent someEvent = new SimulationEvent(50) {

			public void process(IEventController controller) {
			}

			public SimEventType getEventType() {
				return null;
			}

		};
		JSONObject object = (JSONObject) JSONSerializer.toJSON(someEvent);
		assertEquals(2, object.size());
		assertEquals(someEvent.timestep, object.getLong("timestep"));
	}

}
