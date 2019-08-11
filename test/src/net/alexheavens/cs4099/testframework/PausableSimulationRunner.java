package net.alexheavens.cs4099.testframework;

import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.alexheavens.cs4099.concurrent.LeaderBarrier;
import net.alexheavens.cs4099.network.Network;
import net.alexheavens.cs4099.simulation.PrescribedEvent;
import net.alexheavens.cs4099.simulation.SimulationRunner;
import net.alexheavens.cs4099.simulation.SimulationState;

public class PausableSimulationRunner extends SimulationRunner {

	private final LoopThread simLoop = new LoopThread();
	private final Lock syncLock = new ReentrantLock();
	private final Condition loopBegun, loopComplete, loopEntered;

	private volatile boolean begun = false;
	private long visibleTimestep = SimulationRunner.TIMESTEP_NOT_START;

	public PausableSimulationRunner(Network net, long simLength) {
		this(net, simLength, null);
	}

	public PausableSimulationRunner(Network net, long simLength,
			Set<PrescribedEvent> events) {
		super(net, simLength, events, 0);
		loopComplete = syncLock.newCondition();
		loopBegun = syncLock.newCondition();
		loopEntered = syncLock.newCondition();
		pauseBarrier = new LeaderBarrier(simLoop, net.nodeCount());
	}

	public void simulateUntil(long toTimestep) {
		syncLock.lock();
		try {

			// Validate timestep.
			if (toTimestep < 0)
				throw new IllegalArgumentException(
						"Attempted to simulate to negative timestep.");
			if (toTimestep <= visibleTimestep)
				throw new IllegalArgumentException(
						"Attempted to simulate to a timestep already visited.");

			if (!begun) {
				simLoop.start();
				try {
					while (!begun)
						loopEntered.await();
				} catch (InterruptedException e) {
					throw new IllegalStateException();
				}
			}

			visibleTimestep = toTimestep;
			if (visibleTimestep > timestep) {
				if (simState == SimulationState.PAUSED)
					simState = SimulationState.SIMULATING;
				simLoop.loop();
				try {
					loopComplete.await();
				} catch (InterruptedException e) {
					throw new IllegalStateException();
				}
			}
		} finally {
			syncLock.unlock();
		}
	}

	/**
	 * @return The timestep that the simulation appears to have. This differs
	 *         from its actual timestep, which can only increment to the
	 *         timestep of event occurences.
	 */
	public long getVisibleTimestep() {
		return visibleTimestep;
	}

	/**
	 * Provides a means to call the <code>simulateUntil()</code> repeatedly
	 * within its own thread.
	 * 
	 * @author Alexander Heavens <alexander.heavens@gmail.com>
	 * @version 1.0
	 */
	private class LoopThread extends Thread {

		public void loop() {
			syncLock.lock();
			try {
				loopBegun.signal();
			} finally {
				syncLock.unlock();
			}
		}

		public void run() {

			syncLock.lock();
			try {
				while (true) {
					begun = true;
					loopEntered.signal();
					try {
						loopBegun.await();
					} catch (InterruptedException e) {
						throw new IllegalStateException();
					}
					PausableSimulationRunner.super
							.simulateUntil(visibleTimestep);

					simState = SimulationState.PAUSED;
					loopComplete.signal();
				}
			} finally {
				syncLock.unlock();
			}
		}
	}

}
