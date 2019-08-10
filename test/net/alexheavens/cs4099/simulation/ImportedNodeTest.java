package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.*;
import net.alexheavens.cs4099.usercode.MockNodeScript;
import net.alexheavens.cs4099.usercode.NodeScript;

import org.junit.Test;

public class ImportedNodeTest {

	@Test
	public void testCreationValid() {

		ImportedNode testNodeClass = new ImportedNode(MockNodeScript.class);
		try {
			MockNodeScript node = (MockNodeScript) testNodeClass.create();
			assertNotNull(node);
		} catch (InstantiationException e) {
			fail("Threw InstantiationException on node create.");
		} catch (IllegalAccessException e) {
			fail("Threw IllegalAccessException on node create.");
		}
	}

	public abstract class AbstractNode extends NodeScript {
	}

	@Test(expected = InstantiationException.class)
	public void testCreationAbstractNode() throws InstantiationException, IllegalAccessException {

		ImportedNode testNodeClass = new ImportedNode(AbstractNode.class);
		testNodeClass.create();
	}

	@Test
	public void testNullClass() {

		try {
			new ImportedNode(null);
			fail("Able to create null node type.");
		} catch (IllegalArgumentException e) {

		}
	}

}
