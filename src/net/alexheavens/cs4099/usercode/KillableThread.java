package net.alexheavens.cs4099.usercode;

import java.time.Duration;

import org.apache.commons.javaflow.api.continuable;

import net.alexheavens.cs4099.scheduler.ContinuationScheduler;

/**
 * The thread a node runs on. Killing it marks it, interrupts any blocking call, and the next safe point that
 * {@link NodeInstrumenter} added to the user's code throws {@link NodeKilledError}, ending the thread even in a
 * loop that never blocks. This replaces <code>Thread.stop()</code>, which JDK 20 removed.
 * <p>
 * A thread stuck where no safe point runs (a long library or native call) cannot be forced to stop. A watchdog
 * keeps interrupting it, so a call that swallowed the first interrupt is woken again, and if it is still alive
 * after a grace period reports it as abandoned.
 */
public class KillableThread extends Thread {

	private static final long INTERRUPT_INTERVAL_MILLIS = 100;

	private volatile boolean killed;

	public KillableThread(Runnable target) {
		super(target);
		setDaemon(true);
		// Like ThreadDeath, a kill ends the thread quietly; any other uncaught throwable is reported as usual.
		setUncaughtExceptionHandler((thread, error) -> {
			if (!(error instanceof NodeKilledError))
				thread.getThreadGroup().uncaughtException(thread, error);
		});
	}

	public void kill(Duration grace, Runnable onAbandoned) {
		killed = true;
		interrupt();
		Thread watchdog = new Thread(() -> watch(grace, onAbandoned), "kill-watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}

	private void watch(Duration grace, Runnable onAbandoned) {
		long deadline = System.nanoTime() + grace.toNanos();
		try {
			while (System.nanoTime() < deadline) {
				if (!isAlive())
					return;
				join(INTERRUPT_INTERVAL_MILLIS);
				interrupt();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		if (isAlive())
			onAbandoned.run();
	}

	/**
	 * Called from the safe points in user code. On a node thread it ends a killed node; on a scheduler worker it
	 * ends a killed node and yields the worker after the time slice.
	 */
	@continuable
	public static void check() {
		if (Thread.currentThread() instanceof KillableThread t) {
			if (t.killed)
				throw new NodeKilledError();
		} else {
			ContinuationScheduler.tick();
		}
	}
}
