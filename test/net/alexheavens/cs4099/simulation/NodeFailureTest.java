package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.assertEquals;
import net.alexheavens.cs4099.network.MockUserNode;

import org.junit.Test;

public class NodeFailureTest {

	@Test
	public void testCreationValid() {
		MockUserNode testNode = new MockUserNode(32);
		Exception ex = new Exception("BLAH");
		NodeFailureEvent failureEvent = new NodeFailureEvent(424, testNode, ex);
		assertEquals(NodeFailureEvent.PRIORITY, failureEvent.priority());
		assertEquals(SimEventType.NODE_FAILURE, failureEvent.getEventType());
		assertEquals(ex, failureEvent.getFailure());
	}

}
