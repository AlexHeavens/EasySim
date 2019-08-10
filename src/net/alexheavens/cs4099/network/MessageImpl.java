package net.alexheavens.cs4099.network;

/**
 * 
 * Note: this class has a natural ordering that is inconsistent with equals.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @param <DataType>
 */
public abstract class MessageImpl<DataType> implements IMessageImpl<DataType>,
		Cloneable {

	public static final int DEFAULT_LOG_INDEX = -1;
	public static final String SOURCE_ID_TAG = "sourceId";
	public static final String TARGET_ID_TAG = "targetId";
	public static final String SENT_AT_TAG = "sentAt";
	public static final String TAG_TAG = "tag";
	public static final String DATA_TAG = "data";
	public static final String ARRIVED_AT_TAG = "arrivedAt";

	private DataType messageData = null;
	private String messageTag = null;
	protected INode sourceNode = null;
	protected INode targetNode = null;
	protected ILinkImpl link = null;
	protected long timestepSent = IMessageImpl.TIMESTEP_NOT_SENT;
	protected long timestepArrived = IMessageImpl.TIMESTEP_NOT_SENT;
	protected boolean marked = false;

	private int sourceId = -1;
	private int targetId = -1;

	/**
	 * Creates an empty data packet without a source and no tag.
	 * 
	 * @param data
	 *            the data stored.
	 */
	public MessageImpl(DataType data) {
		if (data == null)
			throw new IllegalArgumentException(
					"Unable to create a message with a null data field.");
		messageData = data;
	}

	/**
	 * Creates an empty data packet without a source.
	 * 
	 * @param tag
	 *            the tag stored.
	 * @param data
	 *            the data stored.
	 */
	public MessageImpl(String tag, DataType data) {
		this(data);
		if (tag != null && tag.length() > IMessage.MAX_TAG_LENGTH)
			throw new IllegalArgumentException("Message tag lenth too long");
		messageTag = tag;
	}

	/**
	 * Attaches the source and target data to a message.
	 * 
	 * @param source
	 *            the source node.
	 * @param target
	 *            the target node.
	 */
	public void attachSendData(INodeImpl source, INodeImpl target) {
		if (marked)
			throw new IllegalStateException(
					"Marked message with send data twice.");
		if (source == null)
			throw new IllegalArgumentException(
					"Marked message with null source.");
		if (target == null)
			throw new IllegalArgumentException(
					"Marked message with null target.");
		if (source == target)
			throw new IllegalArgumentException(
					"Marked message with the same source and target node.");
		if (!source.isNeighbour(target))
			throw new IllegalArgumentException(
					"Marked message with source and target of non-neighbours.");
		sourceNode = source;
		targetNode = target;
		sourceId = source.getSimulationId();
		targetId = target.getSimulationId();
		link = (ILinkImpl) source.neighbourLink(target);
		marked = true;
	}

	public DataType getData() {
		return messageData;
	}

	public String getTag() {
		return messageTag;
	}

	public INode source() {
		return sourceNode;
	}

	public int getSourceId() {
		return sourceId;
	}

	public INode target() {
		return targetNode;
	}

	public int getTargetId() {
		return targetId;
	}

	public long getSentAt() {
		return timestepSent;
	}

	public MessageImpl<DataType> clone() {
		try {
			@SuppressWarnings("unchecked")
			MessageImpl<DataType> message = (MessageImpl<DataType>) super
					.clone();
			message.timestepArrived = TIMESTEP_NOT_SENT;
			message.timestepSent = TIMESTEP_NOT_SENT;
			message.marked = false;
			return message;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public ILinkImpl link() {
		return link;
	}

	public void markAsSent(long timestep) {
		if (timestepSent != TIMESTEP_NOT_SENT)
			throw new IllegalStateException(
					"Attempted to mark message as sent twice.");

		if (timestep < 0)
			throw new IllegalArgumentException(
					"Attempted to mark message as sent at invalid timestep "
							+ timestep);
		timestepSent = timestep;
	}

	public final int compareTo(Object obj) {
		IMessageImpl<?> objMessage = (IMessageImpl<?>) obj;
		long objTimestep = objMessage.getSentAt();
		if (objTimestep > timestepSent)
			return -1;
		else if (objTimestep < timestepSent)
			return 1;
		else
			return 0;
	}

	public int sourceIndex() {
		return targetNode.getIndex(sourceNode);
	}

	public long getArrivedAt() {
		return timestepArrived;
	}

	public void markAsArrived(long timestep) {
		if (timestepArrived != TIMESTEP_NOT_SENT)
			throw new IllegalStateException(
					"Attempted to mark message as arrived twice.");

		if (timestep < 0)
			throw new IllegalArgumentException(
					"Attempted to mark message as arrived at invalid timestep "
							+ timestep);

		if (timestepSent == TIMESTEP_NOT_SENT)
			throw new IllegalStateException(
					"Attempted to mark a message as arrived before it was sent.");

		if (timestep <= timestepSent)
			throw new IllegalArgumentException(
					"Attempted to mark message as arrived before or at timestep of send.");

		timestepArrived = timestep;
	}
}
