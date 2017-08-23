package net.alexheavens.cs4099.visualisation;

import static org.junit.Assert.*;
import net.alexheavens.cs4099.visualisation.VisualisationClock.State;

import org.junit.Before;
import org.junit.Test;

public class VisualisationClockTest {

	private static final long TEST_LENGTH = 5l;
	private static final int MILLIS_PER_TIMESTEP = 100;
	private VisualisationClock testClock;
	private MockClockListener clockListener;

	@Before
	public void setup() {
		testClock = new VisualisationClock(TEST_LENGTH);
		clockListener = new MockClockListener();
		testClock.addListener(clockListener);
		testClock.setMillisPerTimestep(MILLIS_PER_TIMESTEP);
	}

	@Test
	/**
	 * Tests that a clock is created with the expected default values.
	 */
	public void testValidCreation() {
		assertEquals(TEST_LENGTH, testClock.getLength());
		assertEquals(0, testClock.getTimestep());
		assertEquals(0, testClock.getTick());
		assertEquals(MILLIS_PER_TIMESTEP,
				testClock.getMillisPerTimestep());
		assertEquals(VisualisationClock.State.STOPPED, testClock.getState());
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that the clock cannot be created with a length less than 1.
	 */
	public void testInvalidCreation() {
		new VisualisationClock(0l);
	}

	@Test(timeout = 600)
	/**
	 * Test that playing through a clock in its entirety produces the correct
	 * timestep updates.
	 */
	public void testPlayDefault() {
		testClock.play();

		while (testClock.getState() == State.STOPPED
				|| testClock.getState() == State.PLAYING) {
		}

		assertEquals(VisualisationClock.State.ENDED, testClock.getState());

		// Check that the play event went as expected.
		assertTrue(clockListener.getEvent(0) instanceof ClockStateEvent);
		ClockStateEvent playEvent = (ClockStateEvent) clockListener.getEvent(0);
		assertEquals(VisualisationClock.State.PLAYING, playEvent.getState());

		// Check that the timestep events were as expected.
		final int ticksPerTimestep = testClock.ticksPerTimestep;
		int ticks = -1;
		int timestep = 0;
		int i = -1;
		for (i = 1; i <= TEST_LENGTH * ticksPerTimestep + TEST_LENGTH + 2; i++) {
			if (++ticks == ticksPerTimestep + 1) {
				ticks = 0;
				timestep++;
			}

			if (ticks == 0) {
				assertTrue(clockListener.getEvent(i) instanceof ClockTimestepEvent);
				ClockTimestepEvent event = (ClockTimestepEvent) clockListener
						.getEvent(i);
				assertEquals(timestep, event.getTimestep());
			} else {
				assertTrue(clockListener.getEvent(i) instanceof ClockTickEvent);
				ClockTickEvent tickEvent = (ClockTickEvent) clockListener
						.getEvent(i);
				assertEquals(ticks - 1, tickEvent.getTick());
			}
		}

		// Check the end event.
		final int endEventIndex = i;
		assertTrue(clockListener.getEvent(endEventIndex - 2) instanceof ClockTimestepEvent);
		assertTrue(clockListener.getEvent(endEventIndex - 1) instanceof ClockTickEvent);
		assertEquals(endEventIndex + 1, clockListener.getEventCount());
		assertTrue(clockListener.getEvent(endEventIndex) instanceof ClockStateEvent);
		ClockStateEvent endEvent = (ClockStateEvent) clockListener
				.getEvent(endEventIndex);
		assertEquals(VisualisationClock.State.ENDED, endEvent.getState());
	}

	@Test(timeout = 250)
	/**
	 * Test that pausing the clock correctly stops it.
	 */
	public void testPause() {

		final long expectedTimestep = testClock.getLength() / 2;

		testClock.play();
		while (testClock.getTimestep() < expectedTimestep) {
		}
		assertEquals(expectedTimestep, testClock.pause());
		assertEquals(State.PAUSED, testClock.getState());
	}

	@Test(timeout = 100)
	/**
	 * Test that the rate at which the clock ticks can be decreased.
	 */
	public void testIncreaseClockSpeed() {

		testClock.setMillisPerTimestep(10);
		testClock.play();

		while (testClock.getState() != State.ENDED) {
		}
	}

	@Test(timeout = 500)
	/**
	 * Tests that pausing and rewinding the clock is correct.
	 */
	public void testRewindClock() {
		testClock.play();
		while (testClock.getTimestep() < testClock.getLength() / 2) {
		}

		testClock.pause();
		testClock.setMillisPerTimestep(-100);
		testClock.play();

		while (testClock.getState() != State.STOPPED) {
		}
		assertEquals(0, testClock.getTick());
		assertEquals(0, testClock.getTimestep());
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that you cannot set the speed of the clock to 0.
	 */
	public void testSetClockSpeedZero() {
		testClock.setMillisPerTimestep(0);
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Test that we cannot call play whilst the clocprivatek is already playing.
	 */
	public void testPlayWhilePlay() {
		testClock.play();
		testClock.play();
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * The clock should not be able to pause execution whilst it is not in a
	 * state of playing.
	 */
	public void testPauseWhileNotPlaying() {
		testClock.pause();
	}

	@Test
	/**
	 * The clock must allow the timestep to jump at the user's command to any
	 * valid timestep in simulation. Doing so must reset the tick of the clock
	 * to zero.
	 */
	public void testJumpToTimestep() {
		final long jumpStep = testClock.getLength() / 2;
		testClock.play();
		while (testClock.getTimestep() < jumpStep) {
		}
		testClock.pause();
		testClock.jumpToTimestep(jumpStep);
		assertEquals(jumpStep, testClock.getTimestep());
		assertEquals(0, testClock.getTick());
		assertEquals(State.PAUSED, testClock.getState());
	}

	@Test
	/**
	 * Jumping to timestep whilst the clock is playing should continue the
	 * execution of the clock.
	 */
	public void testJumpToTimestepPlay() {
		final long jumpStep = testClock.getLength() / 2;
		testClock.play();
		while (testClock.getTimestep() < jumpStep - 1) {
		}
		testClock.jumpToTimestep(jumpStep);
		assertEquals(jumpStep, testClock.getTimestep());
		assertEquals(State.PLAYING, testClock.getState());
	}

	@Test(timeout = 100)
	/**
	 * Attempting to rewind the clock whilst at the start should have no effect.
	 */
	public void testRewindAtStart() throws InterruptedException {

		// Attempt to rewind at the start.
		testClock.setMillisPerTimestep(-100);
		testClock.play();

		// Wait for the clock to start playing (if erroneous).
		synchronized (testClock) {
			testClock.wait(50);
		}

		// Check that no change has occurred.
		assertEquals(0, testClock.getTimestep());
		assertEquals(0, testClock.getTick());
		assertEquals(State.STOPPED, testClock.getState());
		assertEquals(0, clockListener.getEventCount());
	}

	@Test
	/**
	 * Calling stop() should reset the clock to 0 timestep.
	 */
	public void testStop() {
		final long stopTimestep = testClock.getLength() / 2;
		testClock.play();
		while (testClock.getTimestep() < stopTimestep) {
		}
		testClock.stop();
		assertEquals(0, testClock.getTick());
		assertEquals(0, testClock.getTimestep());
		assertEquals(State.STOPPED, testClock.getState());
	}

	@Test(timeout = 1100)
	/**
	 * Calling play while in an end state should reset the clock and continue
	 * playing.
	 */
	public void testPlayAtEnd() {
		testClock.play();
		while (testClock.getState() != State.ENDED) {
		}
		testClock.play();
		assertEquals(0, testClock.getTimestep());
		assertEquals(State.PLAYING, testClock.getState());
		while (testClock.getState() != State.ENDED) {
		}
	}

	@Test
	/**
	 * Setting the speed of the clock should reset the tick of the clock to
	 * avoid the tick becoming inconsistent.
	 */
	public void testSetSpeed() {
		final int newSpeed = 100;
		testClock.tick = 15;
		testClock.setMillisPerTimestep(newSpeed);
		assertEquals(0, testClock.tick);
		assertEquals(newSpeed, testClock.getMillisPerTimestep());
	}

}
