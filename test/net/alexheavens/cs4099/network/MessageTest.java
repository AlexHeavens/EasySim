package net.alexheavens.cs4099.network;

import static org.junit.Assert.*;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.junit.Before;
import org.junit.Test;

public class MessageTest {

	private IMessageImpl<String> stringMessage;
	private String testTag = "TAG";
	private String testString = "TEST";
	private IMessageImpl<Number> numberMessage;
	private Number testNumber = 123456.789;
	private MockUserNode testSource;
	private MockUserNode testTarget;

	@Before
	public void setup() {
		stringMessage = new StringMessage(testTag, testString);
		numberMessage = new NumericMessage(testTag, testNumber);
		testSource = new MockUserNode(0);
		testTarget = new MockUserNode(1);
		testSource.addNeighbour(testTarget, 1);
	}

	@Test
	public void testMessageCreationCorrect() {
		assertEquals(IMessageImpl.TIMESTEP_NOT_SENT, stringMessage.getSentAt());
		assertEquals(IMessageImpl.TIMESTEP_NOT_SENT, numberMessage.getSentAt());
		assertEquals(IMessageImpl.TIMESTEP_NOT_SENT,
				stringMessage.getArrivedAt());
		assertEquals(IMessageImpl.TIMESTEP_NOT_SENT,
				numberMessage.getArrivedAt());
		assertNull(stringMessage.link());
		assertNull(numberMessage.link());
	}

	@Test
	public void testMessageLargeValidTag() {

		// Construct a tag of length threshold.
		final int TAG_SIZE = IMessage.MAX_TAG_LENGTH;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TAG_SIZE; i++)
			sb.append('a');

		StringMessage testMessage = new StringMessage(sb.toString(), testString);
		assertEquals(TAG_SIZE, testMessage.getTag().length());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMessageLargeTagInvalid() {

		// Construct a tag of length threshold + 1.
		final int TAG_SIZE = IMessage.MAX_TAG_LENGTH + 1;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TAG_SIZE; i++)
			sb.append('a');

		new StringMessage(sb.toString(), testString);
	}

	@Test
	public void testMessageLargeValidData() {

		// Construct a data of length threshold.
		final int DATA_SIZE = StringMessage.MAX_DATA_LENGTH;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < DATA_SIZE; i++)
			sb.append('a');

		StringMessage testMessage = new StringMessage(testTag, sb.toString());
		assertEquals(DATA_SIZE, testMessage.getData().length());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMessageLargeDataInvalid() {

		// Construct data of length threshold + 1.
		final int DATA_SIZE = StringMessage.MAX_DATA_LENGTH + 1;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < DATA_SIZE; i++)
			sb.append('a');

		new StringMessage(testTag, sb.toString());
	}

	@Test
	public void testMessageNullTag() {

		// Null tagged messages should be considered tagless.
		StringMessage stringMessage = new StringMessage(null, testString);
		assertNull(stringMessage.getTag());
		NumericMessage numericMessage = new NumericMessage(null, testNumber);
		assertNull(numericMessage.getTag());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMessageNullData() {
		new StringMessage(null);
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Test that a message cannot be marked with source and target data twice.
	 */
	public void testAttachSendDataTwice() {
		stringMessage.attachSendData(testSource, testTarget);
		stringMessage.attachSendData(testSource, testTarget);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAttachNullSender() {
		stringMessage.attachSendData(null, testTarget);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAttachNullTarget() {
		stringMessage.attachSendData(testSource, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAttachSameSenderTarget() {
		stringMessage.attachSendData(testSource, testSource);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAttachNonNeighbourSendData() {
		MockUserNode someNode = new MockUserNode(2);
		stringMessage.attachSendData(testSource, someNode);
	}

	@Test
	/**
	 * Tests that messages compare on timesteps appropriately.
	 * 
	 * This is important so that they can be ordered in priority queues
	 * according to the timestep they arrived.
	 */
	public void testCompare() {

		// Create some successive messages.
		final int secondMessageTime = 50;
		MockMessage firstMessage = new MockMessage(testSource, testTarget,
				secondMessageTime + 1);
		MockMessage secondMessage = new MockMessage(testSource, testTarget,
				secondMessageTime);
		MockMessage thirdMessage = new MockMessage(testSource, testTarget,
				secondMessageTime);
		MockMessage lastMessage = new MockMessage(testSource, testTarget,
				secondMessageTime - 1);

		// Check that the compare method correctly orders them by timestep.
		assertEquals(-1, secondMessage.compareTo(firstMessage));
		assertEquals(0, secondMessage.compareTo(thirdMessage));
		assertEquals(1, secondMessage.compareTo(lastMessage));
		assertEquals(0, secondMessage.compareTo(secondMessage));
	}

	@Test
	public void testToJSONStringMessage() {

		stringMessage.attachSendData(testSource, testTarget);

		JSONObject stringObj = (JSONObject) JSONSerializer
				.toJSON(stringMessage);
		assertEquals(6, stringObj.size());
		assertEquals(stringMessage.getTag(), stringObj.get("tag"));
		assertEquals(stringMessage.getData(), stringObj.get("data"));
		assertEquals(stringMessage.getSentAt(), stringObj.getLong("sentAt"));
		assertEquals(stringMessage.getArrivedAt(),
				stringObj.getLong("arrivedAt"));
		assertEquals(((INodeImpl) stringMessage.source()).getSimulationId(),
				stringObj.get("sourceId"));
		assertEquals(((INodeImpl) stringMessage.target()).getSimulationId(),
				stringObj.get("targetId"));
	}

	@Test
	public void testToJSONNumberMessage() {

		numberMessage.attachSendData(testSource, testTarget);

		JSONObject numberObj = (JSONObject) JSONSerializer
				.toJSON(numberMessage);
		assertEquals(6, numberObj.size());
		assertEquals(numberMessage.getTag(), numberObj.get("tag"));
		assertEquals(numberMessage.getData().doubleValue(),
				numberObj.get("data"));
		assertEquals(numberMessage.getSentAt(), numberObj.getLong("sentAt"));
		assertEquals(numberMessage.getArrivedAt(),
				numberObj.getLong("arrivedAt"));
		assertEquals(((INodeImpl) numberMessage.source()).getSimulationId(),
				numberObj.get("sourceId"));
		assertEquals(((INodeImpl) numberMessage.target()).getSimulationId(),
				numberObj.get("targetId"));
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Test that we cannot mark a message as sent more than once.
	 */
	public void testMarkAsSentTwice() {
		numberMessage.markAsSent(0);
		numberMessage.markAsSent(324);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot mark a message as sent at an invalid timestep.
	 */
	public void testMarkAsSentInvalid() {
		numberMessage.markAsSent(-1);
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Test that we cannot mark a message as arrived before it ahs been sent.
	 */
	public void testMarkAsArrivedBeforeSent() {
		numberMessage.markAsArrived(234);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot arrive a message before or at the time it was sent
	 * (all links have a minimum latency of 1).
	 */
	public void testMarkAsArrivedTimestepBeforeSend() {
		numberMessage.markAsSent(50);
		numberMessage.markAsArrived(50);
	}

	@Test(expected = IllegalStateException.class)
	/**
	 * Test that we cannot mark a message as arrived more than once.
	 */
	public void testMarkAsArrivedTwice() {
		numberMessage.markAsSent(0);
		numberMessage.markAsArrived(1);
		numberMessage.markAsArrived(324);
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot mark a message as arrived at an invalid timestep.
	 */
	public void testMarkAsArrivedInvalid() {
		numberMessage.markAsSent(0);
		numberMessage.markAsArrived(-1);
	}
}
