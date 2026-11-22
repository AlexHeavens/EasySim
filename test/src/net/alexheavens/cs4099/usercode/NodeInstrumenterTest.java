package net.alexheavens.cs4099.usercode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.Test;

/**
 * A node that never blocks has to be killable, because user code cannot be trusted to stop. Each case compiles real
 * source through {@link DynamicClassLoader}, as a student's file is, then kills the thread running it.
 */
public class NodeInstrumenterTest {

	private static final long KILL_WAIT_MS = 5000;

	private Thread start(String className, String body) throws Exception {
		Path dir = Files.createTempDirectory("instrumenter");
		File source = dir.resolve(className + ".java").toFile();
		Files.writeString(source.toPath(), "public class " + className + " implements Runnable {\n"
				+ "	public void run() {\n" + body + "\n	}\n}\n");
		Class<?> loaded = new DynamicClassLoader().loadClass(source);
		Runnable node = (Runnable) loaded.getDeclaredConstructor().newInstance();
		KillableThread thread = new KillableThread(node);
		thread.start();
		return thread;
	}

	private void assertKilled(Thread thread) throws InterruptedException {
		assertTrue("the node should be running before the kill", thread.isAlive());
		((KillableThread) thread).kill(Duration.ofSeconds(1), () -> {});
		thread.join(KILL_WAIT_MS);
		assertFalse("a killed node must end", thread.isAlive());
	}

	@Test
	public void killsAnEmptyLoop() throws Exception {
		assertKilled(start("EmptyLoopNode", "while (true) { }"));
	}

	@Test
	public void killsALoopThatCatchesEverything() throws Exception {
		assertKilled(start("CatchAllNode", "while (true) { try { Math.sqrt(2); } catch (Throwable t) { } }"));
	}

	@Test
	public void killsALoopInsideACatchAllHandler() throws Exception {
		assertKilled(start("HandlerLoopNode",
				"try { throw new RuntimeException(); } catch (Throwable t) { while (true) { try { Math.sqrt(2); } catch (Throwable u) { } } }"));
	}

	@Test
	public void killsARecursionThatNeverReturns() throws Exception {
		Thread thread = start("RecursiveNode",
				"try { spin(0); } catch (StackOverflowError e) { run(); }\n	}\n	private void spin(int depth) { spin(depth + 1);");
		assertKilled(thread);
	}

	@Test
	public void leavesAnUnkilledNodeRunning() throws Exception {
		Thread thread = start("QuietNode", "long end = System.nanoTime() + 300_000_000L; while (System.nanoTime() < end) { }");
		thread.join(KILL_WAIT_MS);
		assertFalse("an unkilled node must finish by itself", thread.isAlive());
	}
}
