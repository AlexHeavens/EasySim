package net.alexheavens.cs4099.child;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.alexheavens.cs4099.child.Frame.Message;
import net.alexheavens.cs4099.child.Frame.Payload;
import net.alexheavens.cs4099.scheduler.ContinuationScheduler;
import net.alexheavens.cs4099.usercode.DynamicClassLoader;
import net.alexheavens.cs4099.usercode.NodeScript;

/** Drives a NodeHost over pipes, playing the simulator's part, with student scripts compiled at run time. */
public class NodeHostTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private ContinuationScheduler scheduler;
	private DataOutputStream toHost;
	private DataInputStream fromHost;
	private Thread hostThread;

	@Before
	public void setUp() throws Exception {
		scheduler = new ContinuationScheduler(2, 1000);
		PipedOutputStream simulatorOut = new PipedOutputStream();
		PipedInputStream hostIn = new PipedInputStream(simulatorOut, 1 << 16);
		PipedOutputStream hostOut = new PipedOutputStream();
		PipedInputStream simulatorIn = new PipedInputStream(hostOut, 1 << 16);
		toHost = new DataOutputStream(simulatorOut);
		fromHost = new DataInputStream(simulatorIn);
		NodeHost host = new NodeHost(new DataInputStream(hostIn), new DataOutputStream(hostOut), this::script,
				scheduler);
		hostThread = new Thread(() -> {
			try {
				host.run();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		hostThread.setDaemon(true);
		hostThread.start();
	}

	@After
	public void tearDown() {
		scheduler.close();
	}

	private NodeScript script(String name) {
		try {
			return (NodeScript) new DynamicClassLoader().loadClass(new File(folder.getRoot(), name + ".java"))
					.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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

	private final BlockingQueue<Frame> seen = new LinkedBlockingQueue<>();

	/** Answers requests the way the simulator would, until the node ends; returns every frame the node sent. */
	private List<Frame> serve(Frame.Start start) throws Exception {
		FrameCodec.write(toHost, start);
		List<Frame> frames = new ArrayList<>();
		while (true) {
			Frame frame = FrameCodec.read(fromHost);
			frames.add(frame);
			switch (frame) {
			case Frame.SendAll f -> FrameCodec.write(toHost, new Frame.Accepted(f.node(), f.request()));
			case Frame.Send f -> FrameCodec.write(toHost, new Frame.Accepted(f.node(), f.request()));
			case Frame.SetColour f -> FrameCodec.write(toHost, new Frame.Accepted(f.node(), f.request()));
			case Frame.Pause f -> FrameCodec.write(toHost, new Frame.Resumed(f.node(), f.request()));
			case Frame.Neighbours f -> FrameCodec.write(toHost,
					new Frame.NeighbourList(f.node(), f.request(), new int[] { 0, 1 }));
			case Frame.Receive f -> FrameCodec.write(toHost, new Frame.Received(f.node(), f.request(), 1,
					new Message("N", new Payload.Whole(40))));
			case Frame.Ended f -> {
				return frames;
			}
			default -> throw new AssertionError(frame);
			}
		}
	}

	@Test(timeout = 30000)
	public void testStudentNodeRunsThroughTheProtocol() throws Exception {
		source("Chatty", """
				sendAll(new StringMessage("HI", "hello"));
				NumericMessage m = receive();
				setColour(java.awt.Color.RED);
				pause(5);
				send(m.sourceIndex(), new NumericMessage("SUM", m.getData().longValue() + neighbourCount()));
				""");
		List<Frame> frames = serve(new Frame.Start(3, "Chatty", true, 77));
		assertEquals(7, frames.size());
		assertTrue(frames.get(0) instanceof Frame.SendAll);
		assertEquals(new Message("HI", new Payload.Text("hello")), ((Frame.SendAll) frames.get(0)).message());
		assertTrue(frames.get(1) instanceof Frame.Receive);
		assertEquals(0xffff0000, ((Frame.SetColour) frames.get(2)).rgb());
		assertEquals(5, ((Frame.Pause) frames.get(3)).steps());
		assertTrue(frames.get(4) instanceof Frame.Neighbours);
		Frame.Send last = (Frame.Send) frames.get(5);
		assertEquals(1, last.neighbour());
		assertEquals(new Message("SUM", new Payload.Whole(42)), last.message());
		assertNull(((Frame.Ended) frames.get(6)).failure());
	}

	@Test(timeout = 30000)
	public void testNodeThatThrowsEndsWithFailure() throws Exception {
		source("Buggy", "throw new IllegalStateException(\"student bug\");");
		List<Frame> frames = serve(new Frame.Start(4, "Buggy", false, 1));
		assertEquals(1, frames.size());
		assertEquals("java.lang.IllegalStateException: student bug", ((Frame.Ended) frames.get(0)).failure());
	}

	@Test(timeout = 30000)
	public void testKillEndsNodeWaitingForAMessage() throws Exception {
		source("Waiter", "receive();");
		FrameCodec.write(toHost, new Frame.Start(5, "Waiter", false, 1));
		assertTrue(FrameCodec.read(fromHost) instanceof Frame.Receive);
		FrameCodec.write(toHost, new Frame.Kill(5));
		Frame ended = FrameCodec.read(fromHost);
		assertEquals(new Frame.Ended(5, null), ended);
	}

	@Test(timeout = 30000)
	public void testUnsupportedMessageDataFailsTheNode() throws Exception {
		source("BadData", "sendAll(new NumericMessage(\"T\", new java.math.BigDecimal(1)));");
		List<Frame> frames = serve(new Frame.Start(6, "BadData", false, 1));
		assertTrue(((Frame.Ended) frames.get(0)).failure().startsWith("java.lang.IllegalArgumentException"));
	}

	@Test(timeout = 30000)
	public void testKillEndsSpinningStudentNodeWithoutBlockingOthers() throws Exception {
		source("Spinner", "while (true) { }");
		source("Quick", "setColour(java.awt.Color.BLUE);");
		// More spinners than workers: the quick node still runs because spinners yield at their safe points.
		for (int i = 0; i < 4; i++)
			FrameCodec.write(toHost, new Frame.Start(10 + i, "Spinner", false, 1));
		FrameCodec.write(toHost, new Frame.Start(20, "Quick", false, 1));
		Frame.SetColour colour = (Frame.SetColour) FrameCodec.read(fromHost);
		assertEquals(20, colour.node());
		FrameCodec.write(toHost, new Frame.Accepted(20, colour.request()));
		assertEquals(new Frame.Ended(20, null), FrameCodec.read(fromHost));
		for (int i = 0; i < 4; i++)
			FrameCodec.write(toHost, new Frame.Kill(10 + i));
		List<Frame> ended = new ArrayList<>();
		for (int i = 0; i < 4; i++)
			ended.add(FrameCodec.read(fromHost));
		for (Frame frame : ended)
			assertNull(((Frame.Ended) frame).failure());
		assertEquals(4, ended.size());
	}
}
