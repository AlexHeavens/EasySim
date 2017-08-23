package net.alexheavens.cs4099.network;

/**
 * Defines the non-user methods that manipulate a link.
 * 
 * @author Alexander Heavens
 * @version 1.0
 */
public interface ILinkImpl extends ILink {

	/**
	 * Places a message onto the appropriate queue in the link.
	 * 
	 * @param message
	 *            the message to be placed.
	 */
	public void queueMessage(IMessageImpl<?> message);

	/**
	 * Remove the foremost message from the queue for a particular target node.
	 * 
	 * @param target
	 *            the target node whose queue we want.
	 * @return the foremost message for the target node.
	 */
	public IMessageImpl<?> popMessage(INodeImpl target);

	/**
	 * Removes a given message from its queue.
	 * 
	 * @param returnMessage
	 *            the message we wish to queue.
	 */
	public void removeMessage(IMessageImpl<?> returnMessage);

}
