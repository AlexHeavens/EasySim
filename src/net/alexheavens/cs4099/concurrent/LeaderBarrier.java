package net.alexheavens.cs4099.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The LeaderBarrier provides a Threadsafe means to block a Thread (the
 * "leader") until enough "follower" Threads are waiting. Once this threshold
 * hold has been met, the Leader is released.
 * 
 * The follower Threads can be prompted back into execution once waiting at the
 * barrier, decrementing the count of follower Threads.
 * 
 * On arriving at the barrier, follower Threads will wait, while a leader Thread
 * will only wait if the follower threshold is not met.
 * 
 * @author Alexander Heavens
 * @version 1.0
 */
public class LeaderBarrier implements WaitRegistrar {

	private volatile boolean leaderBlocking;
	private final ReentrantLock lock;
	private final Condition followerThresholdMet;
	protected final ConcurrentHashMap<Thread, Condition> followers;
	private final Thread leader;
	private final int followerThreshold;

	/**
	 * Creates a new LeaderBarrier blocking for Thread leader until
	 * followerThreshold number of Threads are also waiting. If
	 * 
	 * @param leader
	 *            the blocking leader Thread.
	 * @param followerThreshold
	 *            the required number of followers waiting before the leader can
	 *            continue.
	 * 
	 * @throws NullPointerException
	 *             If the leader is null.
	 * @throws IllegalArgumentException
	 *             If the followerThreshold is less than 1.
	 */
	public LeaderBarrier(Thread leader, int followerThreshold) {
		if (leader == null)
			throw new NullPointerException(
					"Cannot create a LeaderBarrier with a null leader Thread.");
		if (followerThreshold < 1)
			throw new IllegalArgumentException(
					"Cannot create a LeaderBarrier with a follower threshold with less than 1 Thread.");
		this.leader = leader;
		this.followerThreshold = followerThreshold;
		lock = new ReentrantLock();
		followerThresholdMet = lock.newCondition();
		followers = new ConcurrentHashMap<Thread, Condition>(followerThreshold);
		leaderBlocking = false;
	}

	/**
	 * @return The number of follower Threads waiting at the barrier required to
	 *         allow the leader to pass through it.
	 */
	public int getFollowerThreshold() {
		return followerThreshold;
	}

	/**
	 * @return The leader Thread that will block at the barrier without enough
	 *         followers.
	 */
	public Thread getLeader() {
		return leader;
	}

	/**
	 * @return The current number of Threads waiting at the barrier other than
	 *         the leader Thread.
	 */
	public int getNumberOfFollowers() {
		return followers.size();
	}

	/**
	 * Blocks a Thread at the barrier, unless that Thread is the leader Thread
	 * and the follower threshold is met.
	 * 
	 * @throws InterruptedException
	 *             If a waiting Thread is interrupted whilst within the barrier.
	 */
	public void waitThread() throws InterruptedException {
		waitThread(false);
	}

	/**
	 * Resumes the execution of a follower Thread, removing it as a follower and
	 * decrementing the follower count. If the follower count drops below the
	 * follower count, the leader Thread will block if it reaches the barrier.
	 * 
	 * @note The Thread that makes this call will block until the follower
	 *       Thread is in a waiting state. If the follower is not expected to
	 *       enter the barrier, this will block indefinitely.
	 * 
	 * @param follower
	 *            the Thread to release from the barrier.
	 */
	public void notifyThread(Thread follower) {

		if (follower == leader)
			throw new IllegalArgumentException(
					"Cannot resume leader node from follower resume method.");

		if (follower == Thread.currentThread())
			throw new IllegalArgumentException(
					"Cannot call resumeFollower with current Thread.");

		// Spin lock until the follower is ready to resume.
		while (!followers.containsKey(follower)) {
		}

		lock.lock();
		try {
			Condition followerResumed = followers.remove(follower);
			followerResumed.signal();
		} finally {
			lock.unlock();
		}

	}

	/**
	 * @param member
	 *            any Thread.
	 * @return Whether the Thread <code>member</code>, whether leader, follower
	 *         or neither, is currently waiting at the barrier.
	 */
	public boolean containsThread(Thread member) {
		lock.lock();
		try {
			boolean isLeader = leaderBlocking && member == leader;
			return isLeader || followers.containsKey(member);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Resume all follower threads locked in the barrier.
	 */
	public void resumeAll() {
		lock.lock();
		try {
			for (Thread follower : followers.keySet()) {
				Condition followerResumed = followers.remove(follower);
				followerResumed.signal();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Blocks a Thread at the barrier, unless that Thread is the leader Thread
	 * and the follower threshold is met.
	 * 
	 * If the current thread's interrupted status is set when it enters this
	 * method, or it is interrupted while waiting, it will continue to wait
	 * until signaled. When it finally returns from this method its interrupted
	 * status will still be set.
	 */
	public void waitThreadUnInterruptedly() {
		try {
			waitThread(true);
		} catch (InterruptedException e) {
			throw new IllegalArgumentException();
		}
	}

	private void waitThread(boolean uninterruptible)
			throws InterruptedException {
		final Thread currentThread = Thread.currentThread();
		lock.lock();
		try {
			if (currentThread == leader) {
				leaderBlocking = true;
				while (followers.size() < followerThreshold)
					followerThresholdMet.await();
				leaderBlocking = false;
			} else {
				Condition followerResumed = lock.newCondition();
				followers.put(currentThread, followerResumed);
				if (followers.size() >= followerThreshold)
					followerThresholdMet.signal();
				while (followers.containsKey(currentThread)) {
					if (uninterruptible)
						followerResumed.awaitUninterruptibly();
					else
						followerResumed.await();
				}

				// TODO remove Thread from barrier on interrupt.
			}
		} finally {
			lock.unlock();
		}
	}
}
