package net.alexheavens.cs4099.network;

/**
 * Defines the non-user methods that act upon a message.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @param <DataType>
 *            The type of the data that is contained within the message. For
 *            example, a String or Integer.
 */
public interface IMessageImpl<DataType> extends IMessage<DataType>,
		Comparable<DataType> {

	/**
	 * The value that the time-step of a message will be initialised as,
	 * indicating it has not been sent.
	 */
	public static final int TIMESTEP_NOT_SENT = -1;

	/**
	 * Adds a source address and target address to the message. This can only be
	 * done before the message has been sent.
	 * 
	 * @param source
	 *            the node from which the message is addressed.
	 * @param target
	 *            the node to which the message is addressed.
	 */
	public void attachSendData(INodeImpl source, INodeImpl target);

	/**
	 * @return If in transit, the time-step which transit began, else -1.
	 */
	public long getSentAt();

	/**
	 * @return The node from which the message is addressed.
	 */
	public INode source();

	/**
	 * @return The node to which the message is addressed.
	 */
	public INode target();

	/**
	 * @return a clean copy of the message.
	 */
	public IMessageImpl<?> clone();

	/**
	 * @return the link the message is travelling down.
	 */
	public ILinkImpl link();

	/**
	 * @return the ID of the target Node.
	 */
	public int getTargetId();

	/**
	 * @return the ID of the source Node.
	 */
	public int getSourceId();

	/**
	 * Marks the message as in transit.
	 * 
	 * This method is not for use by a user.
	 * 
	 * @param currentTimestep
	 *            the timestep at which the message began transit.
	 */
	public void markAsSent(long currentTimestep);

	/**
	 * @return the time that message arrived at.
	 */
	public long getArrivedAt();

	/**
	 * Marks the message as arrived.
	 * 
	 * This method is not for use by a user.
	 * 
	 * @param currentTimestep
	 *            the timestep at which the message arrived.
	 */
	public void markAsArrived(long currentTimestep);
}
