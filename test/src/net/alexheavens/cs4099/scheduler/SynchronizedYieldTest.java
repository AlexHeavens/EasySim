package net.alexheavens.cs4099.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.javaflow.api.continuable;
import org.junit.After;
import org.junit.Test;

/**
 * ADR 0002's open question: what a node does when it parks or ticks while holding a monitor. It neither deadlocks
 * nor fails, but the continuation does not keep the monitor, so a second node enters the same lock. That is why
 * {@link net.alexheavens.cs4099.usercode.NodeInstrumenter} rejects <code>synchronized</code>. These tests pin the
 * scheduler's behaviour, so a Tascalate upgrade that changes it shows up here.
 */
public class SynchronizedYieldTest {

	private static final long TIMEOUT_SECONDS = 10;

	private ContinuationScheduler scheduler;

	@After
	public void tearDown() {
		scheduler.close();
	}

	private static final class Holder implements Runnable {
		final Object lock;
		final boolean parks;
		static volatile boolean release;
		static final AtomicInteger INSIDE = new AtomicInteger();
		static final AtomicInteger MAX_INSIDE = new AtomicInteger();

		Holder(Object lock, boolean parks) {
			this.lock = lock;
			this.parks = parks;
		}

		@Override
		@continuable
		public void run() {
			synchronized (lock) {
				MAX_INSIDE.accumulateAndGet(INSIDE.incrementAndGet(), Math::max);
				if (parks)
					ContinuationScheduler.park();
				else
					while (!release)
						ContinuationScheduler.tick();
				INSIDE.decrementAndGet();
			}
		}
	}

	private static void pause() {
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private String outcome(ContinuationScheduler.Task task) {
		try {
			task.completion().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			return "completed";
		} catch (TimeoutException e) {
			return "TIMEOUT";
		} catch (Exception e) {
			return "failed: " + e.getCause();
		}
	}

	private void probe(int workers, boolean parks) {
		scheduler = new ContinuationScheduler(workers, 2);
		Object lock = new Object();
		Holder a = new Holder(lock, parks);
		Holder b = new Holder(lock, parks);
		ContinuationScheduler.Task taskA = scheduler.start(a);
		Holder.release = false;
		pause(); // let A park or tick inside the lock before B starts
		ContinuationScheduler.Task taskB = scheduler.start(b);
		pause();
		Holder.release = true;
		if (parks) {
			taskA.unpark();
			taskB.unpark();
		}
		assertEquals("completed", outcome(taskA));
		assertEquals("completed", outcome(taskB));
		assertEquals("the monitor is not kept across a suspend", 2, Holder.MAX_INSIDE.getAndSet(0));
	}

	@Test
	public void parkInsideSynchronizedLosesTheMonitor() {
		probe(1, true);
		scheduler.close();
		probe(2, true);
	}

	@Test
	public void tickInsideSynchronizedLosesTheMonitor() {
		probe(1, false);
		scheduler.close();
		probe(2, false);
	}
}
