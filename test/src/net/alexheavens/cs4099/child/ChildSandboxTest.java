package net.alexheavens.cs4099.child;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

/** Runs hostile student scripts in a real child container and checks what the container stops (ADR 0003). */
public class ChildSandboxTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final AtomicReference<ChildJvm.Failure> failed = new AtomicReference<>();
	private ChildJvm child;

	@Before
	public void setUp() throws Exception {
		ChildJvm.Config config = new ChildJvm.Config(Path.of(System.getProperty("javaflow.agent")),
				folder.getRoot().toPath(), 256, 2, 1000, Duration.ofSeconds(2), Duration.ofSeconds(120));
		child = new ChildJvm(config, failed::set);
	}

	@After
	public void tearDown() {
		child.close();
	}

	private BlockingQueue<Frame> start(String name, String body) throws Exception {
		Files.writeString(new File(folder.getRoot(), name + ".java").toPath(), """
				import net.alexheavens.cs4099.network.*;
				public class %s extends net.alexheavens.cs4099.usercode.NodeScript {
					public void execute() {
						%s
					}
				}
				""".formatted(name, body));
		BlockingQueue<Frame> inbox = new LinkedBlockingQueue<>();
		child.start(1, name, false, 1, inbox::add);
		return inbox;
	}

	private Frame next(BlockingQueue<Frame> inbox) throws InterruptedException {
		Frame frame = inbox.poll(60, TimeUnit.SECONDS);
		assertNotNull("No frame within 60 s; run failure: " + failed.get(), frame);
		return frame;
	}

	/** The text the script sends with sendAll. */
	private String report(String name, String body) throws Exception {
		BlockingQueue<Frame> inbox = start(name, body);
		Frame.SendAll sent = (Frame.SendAll) next(inbox);
		child.send(new Frame.Accepted(1, sent.request()));
		return ((Frame.Payload.Text) sent.message().data()).value();
	}

	@Test(timeout = 180000)
	public void testScriptCannotReadFilesOutsideItsMounts() throws Exception {
		// A private file of the simulator's user, outside every folder that the container mounts.
		Path secret = Files.createTempFile("easysim-secret", ".txt");
		try {
			Files.writeString(secret, "hunter2");
			String body = """
					String r;
					try { r = java.nio.file.Files.readString(java.nio.file.Path.of("%s")); }
					catch (Throwable t) { r = "DENIED"; }
					sendAll(new StringMessage("R", r));
					""".formatted(secret);
			assertEquals("DENIED", report("ReadsFiles", body));
		} finally {
			Files.delete(secret);
		}
	}

	@Test(timeout = 180000)
	public void testScriptCannotWriteToTheFileSystemOrItsOwnFolder() throws Exception {
		String body = """
				String r = "";
				for (String path : new String[] { "/tmp/pwned", "/easysim/scripts/pwned", "/easysim/agent.jar" }) {
					try { java.nio.file.Files.writeString(java.nio.file.Path.of(path), "x"); r += "WROTE "; }
					catch (Throwable t) { r += "DENIED "; }
				}
				// The scratch folder is the one place that may be written.
				try { java.nio.file.Files.writeString(java.nio.file.Path.of("/sandbox-tmp/scratch"), "x"); r += "SCRATCH"; }
				catch (Throwable t) { r += "NO-SCRATCH"; }
				sendAll(new StringMessage("R", r));
				""";
		assertEquals("DENIED DENIED DENIED SCRATCH", report("WritesFiles", body));
		assertTrue(!new File(folder.getRoot(), "pwned").exists());
	}

	@Test(timeout = 180000)
	public void testScriptHasNoNetwork() throws Exception {
		String body = """
				String r;
				try (java.net.Socket socket = new java.net.Socket()) {
					socket.connect(new java.net.InetSocketAddress("1.1.1.1", 53), 3000);
					r = "CONNECTED";
				} catch (Throwable t) { r = "DENIED"; }
				try (java.net.Socket socket = new java.net.Socket()) {
					socket.connect(new java.net.InetSocketAddress("127.0.0.1", 22), 3000);
					r += " CONNECTED";
				} catch (java.net.ConnectException t) { r += " DENIED"; }
				catch (Throwable t) { r += " DENIED"; }
				sendAll(new StringMessage("R", r));
				""";
		assertEquals("DENIED DENIED", report("UsesNetwork", body));
	}

	@Test(timeout = 180000)
	public void testScriptCannotSeeTheHostEnvironment() throws Exception {
		String body = """
				sendAll(new StringMessage("R", System.getenv("HOME") + "|" + System.getenv("USER") + "|" + System.getenv("SSH_AUTH_SOCK")));
				""";
		String seen = report("ReadsEnvironment", body);
		assertTrue(seen, !seen.contains(System.getProperty("user.name")) && !seen.contains(System.getProperty("user.home")));
	}

	@Test(timeout = 180000)
	public void testScriptThatAllocatesWithoutLimitFailsItsNodeNotTheSimulator() throws Exception {
		BlockingQueue<Frame> inbox = start("Hog", """
				java.util.List<long[]> hoard = new java.util.ArrayList<>();
				while (true) hoard.add(new long[1_000_000]);
				""");
		Frame frame = next(inbox);
		// Either the node ends with the out-of-memory error, or the container is killed and the run fails.
		if (frame instanceof Frame.Ended ended)
			assertNotNull(ended.failure());
		else
			assertNotNull(failed.get());
	}

	@Test(timeout = 180000)
	public void testScriptThatForksWithoutLimitIsStopped() throws Exception {
		BlockingQueue<Frame> inbox = start("Forker", """
				int started = 0;
				try {
					while (started < 5000) { new ProcessBuilder("sleep", "60").start(); started++; }
				} catch (Throwable t) { }
				sendAll(new StringMessage("R", started < 5000 ? "STOPPED" : "UNBOUNDED"));
				""");
		Frame frame = next(inbox);
		if (frame instanceof Frame.SendAll sent) {
			child.send(new Frame.Accepted(1, sent.request()));
			assertEquals("STOPPED", ((Frame.Payload.Text) sent.message().data()).value());
		}
	}
}
