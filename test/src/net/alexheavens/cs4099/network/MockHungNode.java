package net.alexheavens.cs4099.network;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * A node whose user code never returns and ignores every interrupt, as student code stuck in a lock or a long
 * library call does. It runs no instrumented code, so no safe point can end it.
 */
public class MockHungNode extends MockUserNode {

	private final CountDownLatch release = new CountDownLatch(1);

	public MockHungNode(int id) {
		super(id);
	}

	@Override
	public void execute() {
		while (true) {
			try {
				release.await();
				return;
			} catch (InterruptedException e) {
				// ignored, so only release() ends the node
			}
		}
	}

	@Override
	protected Duration killGrace() {
		return Duration.ofMillis(200);
	}

	/** Lets the stuck thread end, so a test does not leave it running. */
	public void release() {
		release.countDown();
	}
}
