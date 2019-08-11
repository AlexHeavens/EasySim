package net.alexheavens.cs4099.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class LeaderBarrierTest {

	private MockLeaderBarrierThread[] otherThreads;
	private MockLeaderBarrierThread leader;
	private LeaderBarrier barrier;
	private static final int FOLLOWER_THRESHOLD = 200;
	private static final int TOTAL_THREADS = FOLLOWER_THRESHOLD + 100;

	@Before
	public void setup() {
		leader = new MockLeaderBarrierThread();
		barrier = new LeaderBarrier(leader, FOLLOWER_THRESHOLD);
		leader.setBarrier(barrier);
		otherThreads = new MockLeaderBarrierThread[TOTAL_THREADS];
		for (int i = 0; i < otherThreads.length; i++) {
			otherThreads[i] = new MockLeaderBarrierThread();
			otherThreads[i].setBarrier(barrier);
		}
	}

	@Test
	/**
	 * Test that the constructor of the barrier correctly initialises its state.
	 */
	public void testCreationValid() {
		assertEquals(leader, barrier.getLeader());
		assertEquals(FOLLOWER_THRESHOLD, barrier.getFollowerThreshold());
		assertEquals(0, barrier.getNumberOfFollowers());
	}

	@Test(expected = NullPointerException.class)
	/**
	 * Tests that we cannot create a LeaderBarrier with a null leader.
	 */
	public void testNullLeader() {
		new LeaderBarrier(null, FOLLOWER_THRESHOLD);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Tests that we cannot create a LeaderBarrier with a follower threshold
	 * less than one.
	 */
	public void testInvalidFollowerThreshold() {
		new LeaderBarrier(leader, 0);
	}

	@Test(timeout = 3000)
	/**
	 * Tests that a follower correctly blocks at the barrier.
	 */
	public void testFollowerAwait() {

		for (int i = 0; i < otherThreads.length; i++) {
			otherThreads[i].start();
		}

		while (barrier.getNumberOfFollowers() < otherThreads.length) {
		}

		for (int i = 0; i < otherThreads.length; i++) {
			assertTrue(barrier.followers.containsKey(otherThreads[i]));
		}
	}

	@Test(timeout = 5000)
	/**
	 * Tests that a follower can be resumed from a waiting state inside the
	 * barrier, removing it as a follower.
	 */
	public void testFollowerResume() {
		for (int i = 0; i < otherThreads.length; i++) {
			otherThreads[i].start();
		}

		while (barrier.getNumberOfFollowers() < otherThreads.length) {
		}

		for (int i = 0; i < otherThreads.length; i++) {
			barrier.notifyThread(otherThreads[i]);
			while (otherThreads.length - i - 1 != barrier
					.getNumberOfFollowers()) {
			}
		}
	}

	@Test(timeout = 1000, expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot attempt to resume the leader node through the
	 * resumeFollower method.
	 */
	public void testFollowerLeaderResume() {
		barrier.notifyThread(leader);
	}

	@Test(timeout = 500, expected = IllegalArgumentException.class)
	/**
	 * Test that we a thread cannot attempt to resume itself.
	 */
	public void testFollowerResumeSelf() {
		barrier.notifyThread(Thread.currentThread());
	}

	@Test(timeout = 5000)
	/**
	 * Tests that the leader of the node will await block when the number of
	 * followers is less than the follower threshold.
	 */
	public void testLeaderBlockAwait() {

		leader.start();
		while (!barrier.containsThread(leader)) {
		}

		for (int i = 0; i < FOLLOWER_THRESHOLD - 1; i++) {
			otherThreads[i].start();
		}

		assertFalse(leader.hasPassedBarrier());
	}

	@Test(timeout = 10000)
	/**
	 * Tests that a leader correctly resumes only once the follower threshole is
	 * met.
	 */
	public void testLeaderResume() {

		for (int i = 0; i < FOLLOWER_THRESHOLD / 2; i++) {
			otherThreads[i].start();
		}

		while (barrier.getNumberOfFollowers() < FOLLOWER_THRESHOLD / 2) {
		}

		leader.start();
		while (!barrier.containsThread(leader)) {
		}

		for (int i = FOLLOWER_THRESHOLD / 2; i < FOLLOWER_THRESHOLD - 1; i++) {
			otherThreads[i].start();
		}

		while (barrier.getNumberOfFollowers() < FOLLOWER_THRESHOLD - 1) {
		}

		assertTrue(barrier.containsThread(leader));

		otherThreads[FOLLOWER_THRESHOLD - 1].start();

		while (barrier.containsThread(leader)) {
		}

	}

	@Test(timeout = 500)
	/**
	 * Tests that a follower Thread that makes a call to
	 * waitThreadUninterruptedly, cannot be interrupted.
	 */
	public void testFollowerWaitUninterruptedly() {

		MockLeaderBarrierThread follower = new MockLeaderBarrierThread() {
			public void run() {
				barrier.waitThreadUnInterruptedly();
				passedBarrier = true;
			}
		};
		follower.setBarrier(barrier);

		follower.start();

		while (!barrier.containsThread(follower)) {
		}

		follower.interrupt();
		
		assertTrue(barrier.containsThread(follower));
	}
}
