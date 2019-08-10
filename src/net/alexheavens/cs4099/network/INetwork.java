package net.alexheavens.cs4099.network;

import java.util.ArrayList;

/**
 * Encompasses a network of User defined nodes and <code>ILink</code> between
 * these nodes.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @see INode
 * @see ILink
 */
public interface INetwork {

	/**
	 * The exception message given should an uninitialised node be added to the
	 * network.
	 */
	public static final String INVALID_NODE_MSG = "Attempted to add an initialised Node";

	/**
	 * The exception message given should a node be added more than once to a
	 * network.
	 */
	public static final String DUP_NODE_MSG = "Attempted to add a Node twice to a Network.";

	/**
	 * @return The number of nodes currently contained within the network.
	 */
	public int nodeCount();

	/**
	 * @return The number of links currently contained within the network.
	 */
	public int linkCount();

	/**
	 * @return The nodes within the network.
	 */
	public ArrayList<Node> nodes();

	/**
	 * @return The links within the network.
	 */
	public ArrayList<ILinkImpl> links();

	/**
	 * Adds a node to the network. The node must be uninitialised.
	 * 
	 * @param node
	 *            a new <code>Node</code> to add.
	 * @param an
	 *            offset added to the node ID.
	 * @param scrambleCode
	 *            the offset to all node ID that the scramble will put.
	 */
	public void addNode(Node node, int scrambleCode);

	/**
	 * Creates a link between two existing Nodes within a Network. Both nodes
	 * must already have been added to the network. This will result in a new
	 * <code>Link</code> being added to the list of links within the network.
	 * 
	 * @param source
	 *            source <code>Node</code>.
	 * @param target
	 *            target <code>Node</code>.
	 * @param latency
	 *            the time take for messages to transmit across the link.
	 */
	public void createLink(INodeImpl source, INodeImpl target, long latency);

}
