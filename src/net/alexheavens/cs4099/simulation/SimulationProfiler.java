package net.alexheavens.cs4099.simulation;

import java.lang.management.ManagementFactory;

import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import net.alexheavens.cs4099.network.Node;

/**
 * Tracks that time of simulation processes.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class SimulationProfiler {

	// The number of times the sweep task tests for timed out node per threshold
	// time.
	private final static int TIME_CHECKS_PER_TIMEOUT = 5;

	private final long timeout;
	private final TimerTask sweepProcessesTask;
	private final Map<ProfiledProcess, Long> startTimes = new ConcurrentHashMap<ProfiledProcess, Long>();
	protected final Map<Integer, Map<Long, Long>> processSimTimes;
	protected final ThreadMXBean threadInterface = ManagementFactory
			.getThreadMXBean();

	private long timestep = -1;

	public SimulationProfiler(final int nProcesses) {
		this(nProcesses, 0);
	}

	public SimulationProfiler(final int nProcess, final long nodeTimeout) {

		if (nodeTimeout < 0)
			throw new IllegalArgumentException(
					"Attempted to create profiler with negative process kill timeout.");

		timeout = nodeTimeout;
		processSimTimes = new HashMap<Integer, Map<Long, Long>>(nProcess);
		for(int i = 0; i < nProcess; i++){
			processSimTimes.put(i, new HashMap<Long, Long>());
		}

		// Only create a sweep task if a timeout is specified.
		if (timeout > 0) {
			sweepProcessesTask = new TimerTask() {

				@Override
				public void run() {
					killUnresponsiveNodes();
				}
			};

			final long killDelay = ((nodeTimeout / 1000000) / TIME_CHECKS_PER_TIMEOUT) + 1;
			final Timer killTimer = new Timer();
			killTimer.scheduleAtFixedRate(sweepProcessesTask, killDelay,
					killDelay);
		} else {
			sweepProcessesTask = null;
		}
	}

	/**
	 * Increments the timestep for which processes are tracked. This must be
	 * called while no processes are currently being tracked. The passed
	 * timestep must always be increasing.
	 * 
	 * @param timestep
	 *            the timestep in which the tracked times of nodes are
	 * @throws IllegalArgumentException
	 *             If timestep is less or equal to a previous call, or less than
	 *             0.
	 */
	public synchronized void incrementTimestep(long timestep) {
		if (timestep < 0)
			throw new IllegalArgumentException(
					"Negative timestep passed to profiler.");

		if (timestep <= this.timestep)
			throw new IllegalArgumentException(
					"Attempted to profile a past timestep.");

		this.timestep = timestep;
	}

	/**
	 * Resumes tracking the time that a node is executing in this timestep.
	 * 
	 * If a time limit of execution per timestep is given to the profiler, the
	 * count of execution time this method maintains is used to determine if a
	 * node has exceeded this limit.
	 * 
	 * @param process
	 *            the process that has resumed simulation.
	 * @throws IllegalArgumentException
	 *             if process is null.
	 * @throws IllegalStateException
	 *             if process is currently tracked.
	 * @see SimulationProfiler#untrackNodeSimulation(Node node)
	 */
	public synchronized void trackProcessSimulation(ProfiledProcess process) {

		if (process == null)
			throw new IllegalArgumentException(
					"Attempted to track null process.");

		if (startTimes.containsKey(process))
			throw new IllegalStateException(
					"Attempted to track an already tracked process.");
		startTimes.put(process,
				threadInterface.getThreadCpuTime(process.getThread().getId()));
	}

	/**
	 * Stop timing the execution of a process that has paused simulation.
	 * 
	 * This must be called following a call to
	 * {@link #trackProcessSimulation(ProfiledProcess)}.
	 * 
	 * @param process
	 *            the process that has paused simulation.
	 * @throws IllegalStateException
	 *             if called on an untracked process.
	 * @see SimulationProfiler#trackProcessSimulation(ProfiledProcess)
	 */
	public synchronized void untrackNodeSimulation(ProfiledProcess process) {

		if (process == null)
			throw new IllegalArgumentException(
					"Attempted to track null process.");

		if (!startTimes.containsKey(process))
			throw new IllegalStateException(
					"Attempted to untrack process without first tracking it.");

		final long currentTime = threadInterface.getThreadCpuTime(process
				.getThread().getId());
		final long simTime = currentTime - startTimes.get(process);

		final int processId = process.getSimulationId();
		final Map<Long, Long> timestepMap = processSimTimes.get(processId);
		
		final long previousTime = (timestepMap.containsKey(timestep)) ? timestepMap
				.get(timestep) : 0l;
		timestepMap.put(timestep, previousTime + simTime);
		startTimes.remove(process);
	}

	/**
	 * @return A mapping of each timestep to the time taken by each process ID
	 *         in that step.
	 */
	public synchronized Map<Integer, Map<Long, Long>> getTimeStepProcessIdMap() {
		return processSimTimes;
	}

	private synchronized void killUnresponsiveNodes() {
		final Iterator<ProfiledProcess> nodes = startTimes.keySet().iterator();
		while (nodes.hasNext()) {
			final ProfiledProcess node = nodes.next();
			long currentTime = threadInterface.getThreadCpuTime(node
					.getThread().getId());

			if (currentTime - startTimes.get(node) > timeout) {
				node.kill();
				nodes.remove();
			}
		}
	}
}
