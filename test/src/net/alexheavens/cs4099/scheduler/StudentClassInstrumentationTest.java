package net.alexheavens.cs4099.scheduler;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.alexheavens.cs4099.usercode.DynamicClassLoader;

/** Proves the agent instruments student code compiled at run time, which carries no @continuable of its own. */
public class StudentClassInstrumentationTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	public static volatile int counter;

	@Test
	public void testStudentClassParksAndResumes() throws Exception {
		File source = folder.newFile("StudentParker.java");
		Files.writeString(source.toPath(), """
				public class StudentParker implements Runnable {
					public void run() {
						helper();
						net.alexheavens.cs4099.scheduler.StudentClassInstrumentationTest.counter++;
					}
					private void helper() {
						net.alexheavens.cs4099.scheduler.ContinuationScheduler.park();
					}
				}
				""");
		Class<?> student = new DynamicClassLoader().loadClass(source);
		counter = 0;
		try (ContinuationScheduler scheduler = new ContinuationScheduler(1, 1000)) {
			ContinuationScheduler.Task task = scheduler.start((Runnable) student.getDeclaredConstructor().newInstance());
			Thread.sleep(300);
			assertEquals("body must be parked, not finished", 0, counter);
			task.unpark();
			task.completion().get(10, TimeUnit.SECONDS);
		}
		assertEquals(1, counter);
	}
}
