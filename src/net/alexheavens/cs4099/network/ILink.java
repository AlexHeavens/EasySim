package net.alexheavens.cs4099.network;

/**
 * The ILink interface defines a bond between two <code>INodes</code> within an
 * <code>INetwork</code>.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @see INode
 * @see INetwork
 */
public interface ILink {

	/**
	 * The minimum latency a link can have.
	 */
	public static final long MIN_LATENCY = 1;

	/**
	 * @return The Node from which this Link emanates.
	 */
	public INode getSource();

	/**
	 * @return The Node to which this Link goes.
	 */
	public INode getTarget();

	/**
	 * @param node
	 *            the node of which the opposite is needed.
	 * @return The node other than the specified, either source or target.
	 */
	public INode opposite(INode node);

	/**
	 * @return The number of timesteps taken for a message to traverse the link.
	 */
	public long latency();

	/**
	 * @param node
	 *            the node from which we want the number of messages.
	 * @return The number of message queued for node.
	 */
	public int messageCount(INode node);

}
