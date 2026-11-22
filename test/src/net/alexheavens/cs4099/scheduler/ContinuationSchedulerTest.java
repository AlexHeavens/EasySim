package net.alexheavens.cs4099.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.javaflow.api.continuable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContinuationSchedulerTest {

	private static final int WORKERS = 2;
	private static final int SLICE_TICKS = 1000;
	private static final long TIMEOUT_SECONDS = 20;

	private ContinuationScheduler scheduler;

	@Before
	public void setUp() {
		scheduler = new ContinuationScheduler(WORKERS, SLICE_TICKS);
	}

	@After
	public void tearDown() {
		scheduler.close();
	}

	private static class Spinner implements Runnable {
		final AtomicBoolean stop = new AtomicBoolean();

		@Override
		@continuable
		public void run() {
			while (!stop.get())
				ContinuationScheduler.tick();
		}
	}

	private static class Replier implements Runnable {
		final AtomicInteger replies;
		ContinuationScheduler.Task self;

		Replier(AtomicInteger replies) {
			this.replies = replies;
		}

		@Override
		@continuable
		public void run() {
			ContinuationScheduler.park();
			replies.incrementAndGet();
		}
	}

	@Test
	public void testSpinnersDoNotStarveParkedNodes() throws Exception {
		AtomicInteger replies = new AtomicInteger();
		List<Spinner> spinners = new ArrayList<>();
		for (int i = 0; i < 4 * WORKERS; i++) {
			Spinner spinner = new Spinner();
			spinners.add(spinner);
			scheduler.start(spinner);
		}
		List<ContinuationScheduler.Task> repliers = new ArrayList<>();
		for (int i = 0; i < 5000; i++)
			repliers.add(scheduler.start(new Replier(replies)));
		for (ContinuationScheduler.Task replier : repliers)
			replier.unpark();
		for (ContinuationScheduler.Task replier : repliers)
			replier.completion().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		assertEquals(repliers.size(), replies.get());
		spinners.forEach(s -> s.stop.set(true));
	}

	@Test
	public void testUnparkBeforeParkIsNotLost() throws Exception {
		AtomicInteger replies = new AtomicInteger();
		for (int i = 0; i < 500; i++) {
			ContinuationScheduler.Task task = scheduler.start(new Replier(replies));
			task.unpark();
			task.completion().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		assertEquals(500, replies.get());
	}

	@Test
	public void testKillEndsSpinnerWithoutFailure() throws Exception {
		ContinuationScheduler.Task task = scheduler.start(new Spinner());
		task.kill();
		task.completion().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		assertFalse(task.completion().isCompletedExceptionally());
	}

	@Test
	public void testKillEndsParkedNode() throws Exception {
		AtomicInteger replies = new AtomicInteger();
		ContinuationScheduler.Task task = scheduler.start(new Replier(replies));
		task.kill();
		task.completion().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		assertEquals(0, replies.get());
	}

	@Test
	public void testBodyFailureIsReported() throws Exception {
		IllegalStateException failure = new IllegalStateException("student bug");
		ContinuationScheduler.Task task = scheduler.start(new Runnable() {
			@Override
			@continuable
			public void run() {
				throw failure;
			}
		});
		CompletableFuture<Void> completion = task.completion();
		try {
			completion.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			throw new AssertionError("expected failure");
		} catch (ExecutionException e) {
			assertSame(failure, e.getCause());
		}
		assertTrue(completion.isCompletedExceptionally());
	}
}
