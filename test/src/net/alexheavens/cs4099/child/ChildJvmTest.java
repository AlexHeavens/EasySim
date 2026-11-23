package net.alexheavens.cs4099.child;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.alexheavens.cs4099.child.Frame.Message;
import net.alexheavens.cs4099.child.Frame.Payload;

/** Starts a real child JVM and plays the simulator's part, with student scripts compiled in the child. */
public class ChildJvmTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final AtomicReference<ChildJvm.Failure> failed = new AtomicReference<>();
	private ChildJvm child;

	@Before
	public void setUp() throws Exception {
		ChildJvm.Config config = new ChildJvm.Config(Path.of(System.getProperty("javaflow.agent")),
				folder.getRoot().toPath(), 256, 2, 1000, Duration.ofSeconds(2), Duration.ofSeconds(300));
		child = new ChildJvm(config, failed::set);
	}

	@After
	public void tearDown() {
		child.close();
	}

	private void source(String name, String body) throws Exception {
		Files.writeString(new File(folder.getRoot(), name + ".java").toPath(), """
				import net.alexheavens.cs4099.network.*;
				public class %s extends net.alexheavens.cs4099.usercode.NodeScript {
					public void execute() {
						%s
					}
				}
				""".formatted(name, body));
	}

	private BlockingQueue<Frame> start(int node, String script) {
		BlockingQueue<Frame> inbox = new LinkedBlockingQueue<>();
		child.start(node, script, false, 1, inbox::add);
		return inbox;
	}

	private Frame next(BlockingQueue<Frame> inbox) throws InterruptedException {
		Frame frame = inbox.poll(30, TimeUnit.SECONDS);
		assertNotNull("No frame within 30 s; run failure: " + failed.get(), frame);
		return frame;
	}

	@Test(timeout = 120000)
	public void testNormalExchangeBetweenSimulatorAndNode() throws Exception {
		source("Chatty", """
				sendAll(new StringMessage("HI", "hello"));
				NumericMessage m = receive();
				send(m.sourceIndex(), new NumericMessage("SUM", m.getData().longValue() + 2));
				""");
		BlockingQueue<Frame> inbox = start(3, "Chatty");

		Frame.SendAll hello = (Frame.SendAll) next(inbox);
		assertEquals(new Message("HI", new Payload.Text("hello")), hello.message());
		child.send(new Frame.Accepted(3, hello.request()));

		Frame.Receive receive = (Frame.Receive) next(inbox);
		child.send(new Frame.Received(3, receive.request(), 1, new Message("N", new Payload.Whole(40))));

		Frame.Send reply = (Frame.Send) next(inbox);
		assertEquals(new Message("SUM", new Payload.Whole(42)), reply.message());
		child.send(new Frame.Accepted(3, reply.request()));

		assertEquals(new Frame.Ended(3, null), next(inbox));
		assertNull(failed.get());
		assertTrue(child.isAlive());
	}

	@Test(timeout = 120000)
	public void testKillEndsSpinningNodeInProcess() throws Exception {
		source("Spinner", """
				setColour(java.awt.Color.RED);
				while (true) { }
				""");
		BlockingQueue<Frame> inbox = start(7, "Spinner");
		// The child compiles the script before it reads the kill, so wait until the node is running.
		Frame.SetColour colour = (Frame.SetColour) next(inbox);
		child.send(new Frame.Accepted(7, colour.request()));
		child.kill(7);
		assertEquals(new Frame.Ended(7, null), next(inbox));
		// Wait past the grace period: a late process kill would show here.
		Thread.sleep(3000);
		assertNull(failed.get());
		assertTrue(child.isAlive());
	}

	@Test(timeout = 120000)
	public void testNodeThatCannotBeKilledInProcessEndsRunByProcessKill() throws Exception {
		// A latch await has no safe point and ignores the kill flag, so only killing the process stops it.
		source("Stuck", """
				setColour(java.awt.Color.RED);
				try { new java.util.concurrent.CountDownLatch(1).await(); } catch (InterruptedException e) { }
				""");
		BlockingQueue<Frame> inbox = start(9, "Stuck");
		// The colour request shows the node is running, so the kill finds it blocked.
		Frame.SetColour colour = (Frame.SetColour) next(inbox);
		child.send(new Frame.Accepted(9, colour.request()));
		Thread.sleep(500);
		child.kill(9);

		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (failed.get() == null && System.nanoTime() < deadline)
			Thread.sleep(50);

		ChildJvm.Failure failure = failed.get();
		assertNotNull("The run did not fail", failure);
		assertEquals(9, failure.node());
		assertTrue(failure.reason(), failure.reason().contains("Node 9"));
		assertEquals(failure, child.failure());
		// destroyForcibly returns before the process has gone.
		long gone = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (child.isAlive() && System.nanoTime() < gone)
			Thread.sleep(50);
		assertFalse(child.isAlive());
		assertTrue(inbox.isEmpty());
	}
}
