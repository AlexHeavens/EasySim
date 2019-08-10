
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;

import net.alexheavens.cs4099.usercode.ClassLoaderException;
import net.alexheavens.cs4099.usercode.DynamicClassLoader;

import org.junit.Before;
import org.junit.Test;

public class DynamicClassLoaderTest {

	private DynamicClassLoader testLoader;

	@Before
	public void setup() {
		testLoader = new DynamicClassLoader();
	}

	@Test
	/**
	 * Tests that loading a valid Node is as expected.
	 */
	public void testLoadValid() throws IOException, ClassNotFoundException, ClassLoaderException {
		File mockScriptFile = new File("test/NoPackageTreeLeaderNode.java");
		Class<?> someClass = testLoader.loadClass(mockScriptFile);
		assertEquals(NoPackageTreeLeaderNode.class, someClass);
	}

	@Test(expected = ClassLoaderException.class)
	/**
	 * Test that loading an invalid .java file is not possible.
	 */
	public void testLoadInvalid() throws IOException, ClassNotFoundException, ClassLoaderException{
		File badScript = new File("test/BadClass.java");
		testLoader.loadClass(badScript);
	}
	
}
