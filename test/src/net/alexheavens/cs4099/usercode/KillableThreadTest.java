package net.alexheavens.cs4099.usercode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * The watchdog behind {@link KillableThread#kill}: it keeps interrupting, and reports a thread that will not stop.
 * The threads here run plain code, with no safe points, as a node stuck in a long library call would.
 */
public class KillableThreadTest {

	private static final Duration GRACE = Duration.ofMillis(500);
	private static final long WAIT_MS = 5000;

	@Test
	public void aKilledThreadThatEndsIsNotReportedAsAbandoned() throws Exception {
		CountDownLatch running = new CountDownLatch(1);
		KillableThread thread = new KillableThread(() -> {
			running.countDown();
			try {
				Thread.sleep(60_000);
			} catch (InterruptedException e) {
				// ends on the interrupt
			}
		});
		thread.start();
		assertTrue(running.await(WAIT_MS, TimeUnit.MILLISECONDS));
		AtomicInteger abandoned = new AtomicInteger();

		thread.kill(GRACE, abandoned::incrementAndGet);
		thread.join(WAIT_MS);
		Thread.sleep(GRACE.toMillis() + 200);

		assertFalse(thread.isAlive());
		assertEquals(0, abandoned.get());
	}

	@Test
	public void aThreadThatSwallowsTheFirstInterruptIsInterruptedAgain() throws Exception {
		CountDownLatch running = new CountDownLatch(1);
		AtomicInteger interrupts = new AtomicInteger();
		KillableThread thread = new KillableThread(() -> {
			running.countDown();
			while (interrupts.get() < 3) {
				try {
					Thread.sleep(60_000);
				} catch (InterruptedException e) {
					interrupts.incrementAndGet();
				}
			}
		});
		thread.start();
		assertTrue(running.await(WAIT_MS, TimeUnit.MILLISECONDS));

		thread.kill(Duration.ofSeconds(4), () -> {
		});
		thread.join(WAIT_MS);

		assertFalse("repeated interrupts should end a thread that swallows the first", thread.isAlive());
	}

	@Test
	public void aThreadThatIgnoresTheKillIsReportedAsAbandoned() throws Exception {
		CountDownLatch running = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		KillableThread thread = new KillableThread(() -> {
			running.countDown();
			while (true) {
				try {
					release.await();
					return;
				} catch (InterruptedException e) {
					// ignores every interrupt, like code stuck where no safe point runs
				}
			}
		});
		thread.start();
		assertTrue(running.await(WAIT_MS, TimeUnit.MILLISECONDS));
		CountDownLatch abandoned = new CountDownLatch(1);

		thread.kill(GRACE, abandoned::countDown);

		try {
			assertTrue("the thread should be reported once the grace period ends",
					abandoned.await(WAIT_MS, TimeUnit.MILLISECONDS));
			assertTrue("the abandoned thread cannot be forced to stop", thread.isAlive());
		} finally {
			release.countDown();
			thread.join(WAIT_MS);
		}
	}
}
