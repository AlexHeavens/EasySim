package net.alexheavens.cs4099.network;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class LinkTest {

	private ILinkImpl testLink;
	private Node testTarget;
	private Node testSource;

	@Before
	public void setup() {
		testSource = new MockUserNode(0);
		testTarget = new MockUserNode(1);
		testLink = new Link(testSource, testTarget);
	}

	@Test
	public void testCreateLinkCorrect() {
		assertEquals(testSource, testLink.getSource());
		assertEquals(testTarget, testLink.getTarget());
		assertEquals(0, testLink.messageCount(testSource));
		assertEquals(0, testLink.messageCount(testTarget));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateNullSource() {
		new Link(null, new MockUserNode());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateNullTarget() {
		new Link(new MockUserNode(), null);
	}

	@Test
	public void testOppositeCorrect() {
		assertEquals(testTarget, testLink.opposite(testSource));
		assertEquals(testSource, testLink.opposite(testTarget));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOppositeUnknownNode() {
		testLink.opposite(new MockUserNode());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOppositeNullNode() {
		testLink.opposite(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidLatency() {
		new Link(testSource, testTarget, ILink.MIN_LATENCY - 1);
	}

	@Test
	public void testQueueValidMessage() {
		MockMessage testMessage = new MockMessage(testSource, testTarget, 50);
		testLink.queueMessage(testMessage);

		assertEquals(1, testLink.messageCount(testTarget));
		assertEquals(0, testLink.messageCount(testSource));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonLinkNodeMessageCount() {
		testLink.messageCount(new MockUserNode(2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonLinkTargetQueueMessage() {
		MockMessage testMessage = new MockMessage(testSource,
				new MockUserNode(), 0);
		testLink.queueMessage(testMessage);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonLinkSourceTargetQueueMessage() {
		MockMessage testMessage = new MockMessage(new MockUserNode(),
				testSource, 0);
		testLink.queueMessage(testMessage);
	}

	@Test
	public void testPopMessageValid() {
		MockMessage testMessage = new MockMessage(testSource, testTarget, 50);
		testLink.queueMessage(testMessage);
		assertEquals(testMessage, testLink.popMessage(testTarget));
		assertEquals(0, testLink.messageCount(testTarget));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonLinkNodePopNode() {
		testLink.popMessage(new MockUserNode());
	}

	@Test
	/**
	 * Tests that popped messages are ordered by timestep.
	 */
	public void testPopMessagesAreOrdered() {

		// Create some messages with different timesteps.
		final int nMessages = 1000;
		MockMessage[] messages = new MockMessage[nMessages];
		for (int i = messages.length - 1; i >= 0; i--) {
			messages[i] = new MockMessage(testSource, testTarget, i);
			testLink.queueMessage(messages[i]);
		}

		// Test that the messages pop off in the correct order.
		for (int i = 0; i < messages.length; i++) {
			assertEquals(messages[i], testLink.popMessage(testTarget));
		}
	}
}
