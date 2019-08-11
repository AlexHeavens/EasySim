package net.alexheavens.cs4099.simulation;

import static org.junit.Assert.*;
import net.alexheavens.cs4099.network.INodeImpl;
import net.alexheavens.cs4099.network.MockUserNode;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.junit.Before;
import org.junit.Test;

public class NodeEventTest {

	private MockNodeEvent testEvent;
	private INodeImpl testNode;
	private static final long EVENT_TIMESTEP = 50;

	@Before
	public void setup() {
		testNode = new MockUserNode(234234);
		testEvent = new MockNodeEvent(EVENT_TIMESTEP, testNode);
	}

	@Test
	public void testValidCreation() {
		assertEquals(testNode, testEvent.node());
		assertEquals(0, testEvent.eventProcessedCount());
		assertEquals(0, testEvent.simProcessedCount());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullNodeCreation() {
		new MockNodeEvent(EVENT_TIMESTEP, null);
	}

	@Test
	public void testToJSON() {
		JSONObject eventObj = (JSONObject) JSONSerializer.toJSON(testEvent);
		assertEquals(3, eventObj.size());
		assertEquals(testEvent.eventNode.getSimulationId(), eventObj.get("nodeId"));
	}
}
