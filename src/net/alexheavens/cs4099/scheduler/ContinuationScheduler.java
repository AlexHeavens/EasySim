package net.alexheavens.cs4099.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.api.continuable;

import net.alexheavens.cs4099.usercode.NodeKilledError;

/**
 * Runs many nodes as continuations on a few worker threads, so thousands of nodes cost kilobytes each instead of a
 * thread each (ADR 0001). A node body runs until it parks or reaches a safe point ({@link #tick()}) after its time
 * slice, then yields its worker to the next node in a FIFO queue. A node that never blocks therefore cannot starve
 * the others, which the JDK's own virtual-thread scheduler does not guarantee.
 * <p>
 * Every method on the path from a node body down to {@link #park()} or {@link #tick()} must be
 * <code>@continuable</code>, and the JVM must run with the Tascalate JavaFlow agent. A blocking call that does not
 * go through {@link #park()} holds a worker thread.
 */
public final class ContinuationScheduler implements AutoCloseable {

	private static final ThreadLocal<Task> CURRENT = new ThreadLocal<>();

	private final BlockingQueue<Task> runQueue = new LinkedBlockingQueue<>();
	private final Thread[] workers;
	private final int sliceTicks;

	public ContinuationScheduler(int workerCount, int sliceTicks) {
		this.sliceTicks = sliceTicks;
		workers = IntStream.range(0, workerCount).mapToObj(i -> {
			Thread worker = new Thread(this::work, "node-scheduler-" + i);
			worker.setDaemon(true);
			worker.start();
			return worker;
		}).toArray(Thread[]::new);
	}

	/** Starts a node. The body's run method, and everything it calls that parks or ticks, must be @continuable. */
	public Task start(Runnable body) {
		Task task = new Task(body);
		runQueue.add(task);
		return task;
	}

	@Override
	public void close() {
		for (Thread worker : workers)
			worker.interrupt();
	}

	/** Safe point: ends the node if it was killed, and yields the worker once the time slice is used up. */
	@continuable
	public static void tick() {
		Task task = CURRENT.get();
		task.throwIfKilled();
		if (++task.ticks >= task.scheduler.sliceTicks) {
			task.ticks = 0;
			Continuation.suspend();
			task.throwIfKilled();
		}
	}

	/** Suspends the calling node until {@link Task#unpark()}; returns at once if that already happened. */
	@continuable
	public static void park() {
		Task task = CURRENT.get();
		while (!task.state.compareAndSet(Task.PERMIT, Task.RUNNING)) {
			if (task.state.compareAndSet(Task.RUNNING, Task.PARKING)) {
				Continuation.suspend();
				break;
			}
		}
		task.throwIfKilled();
	}

	private void work() {
		try {
			while (true)
				run(runQueue.take());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void run(Task task) {
		CURRENT.set(task);
		try {
			task.continuation = task.continuation == null ? Continuation.startWith(task.body)
					: task.continuation.resume();
		} catch (NodeKilledError e) {
			task.continuation = null;
		} catch (Throwable e) {
			task.continuation = null;
			task.completion.completeExceptionally(e);
		} finally {
			CURRENT.remove();
		}
		if (task.continuation == null) {
			task.completion.complete(null);
		} else if (!task.state.compareAndSet(Task.PARKING, Task.PARKED)) {
			// Either a slice ended, or an unpark raced with the park: in both cases the node is runnable again.
			task.state.compareAndSet(Task.PERMIT, Task.RUNNING);
			runQueue.add(task);
		}
	}

	/** A node's handle: wake it, kill it, or wait for it to finish. */
	public final class Task {
		private static final int RUNNING = 0, PERMIT = 1, PARKING = 2, PARKED = 3;

		private final Runnable body;
		private final AtomicInteger state = new AtomicInteger(RUNNING);
		private final CompletableFuture<Void> completion = new CompletableFuture<>();
		private final ContinuationScheduler scheduler = ContinuationScheduler.this;
		private volatile boolean killed;
		private Continuation continuation;
		private int ticks;

		private Task(Runnable body) {
			this.body = body;
		}

		/** Wakes the node if parked; otherwise makes its next {@link ContinuationScheduler#park()} return at once. */
		public void unpark() {
			while (true) {
				int current = state.get();
				if (current == PERMIT)
					return;
				if (current == PARKED && state.compareAndSet(PARKED, RUNNING)) {
					runQueue.add(this);
					return;
				}
				if ((current == RUNNING || current == PARKING) && state.compareAndSet(current, PERMIT))
					return;
			}
		}

		/** Ends the node at its next safe point or park. Completes {@link #completion()} normally. */
		public void kill() {
			killed = true;
			unpark();
		}

		/** Completes when the body returns or is killed, and fails with whatever the body threw. */
		public CompletableFuture<Void> completion() {
			return completion;
		}

		private void throwIfKilled() {
			if (killed)
				throw new NodeKilledError();
		}
	}
}
