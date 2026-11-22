package net.alexheavens.cs4099.usercode;

/**
 * The thread a node runs on. Killing it marks it, interrupts any blocking call, and the next safe point that
 * {@link NodeInstrumenter} added to the user's code throws {@link NodeKilledError}, ending the thread even in a
 * loop that never blocks. This replaces <code>Thread.stop()</code>, which JDK 20 removed.
 */
public class KillableThread extends Thread {

	private volatile boolean killed;

	public KillableThread(Runnable target) {
		super(target);
		setDaemon(true);
	}

	public void kill() {
		killed = true;
		interrupt();
	}

	/** Called from the safe points in user code. */
	public static void check() {
		if (Thread.currentThread() instanceof KillableThread t && t.killed)
			throw new NodeKilledError();
	}
}
