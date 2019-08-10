package net.alexheavens.cs4099.network;

import java.awt.Color;
import java.util.Iterator;

/**
 * The INode interface provides the user of the network simulator an API to
 * program their network algorithm against. The following methods are those
 * intended for use by the user.
 * 
 * @author Alexander Heavens
 * @version 1.0
 **/
public interface INode {

	/**
	 * Called on the creation of the Node.
	 */
	public abstract void setup();

	/**
	 * Called on simulation of the node.
	 */
	public abstract void execute();

	/**
	 * Pauses the execution of the nodes for <code>sleepSteps</code> timesteps.
	 * During this period, the node will be unresponsive to any incoming
	 * messages.
	 * 
	 * @param pauseSteps
	 *            the number of steps to pause the node for.
	 */
	public void pause(long pauseSteps);

	/**
	 * Sends a message to a particular neighbour from a <code>Node</code>. This
	 * will add the message to the recipient's message queue on arrival.
	 * 
	 * @param recipient
	 *            the node that will received the message, must be a neighbour.
	 * @param message
	 *            the message that is sent to the neighbour.
	 */
	public void send(INode recipient, IMessage<?> message);

	/**
	 * Send a message to all neighbouring nodes.
	 * 
	 * @param message
	 *            the message to be sent.
	 */
	public void sendAll(IMessage<?> message);

	/**
	 * Wait for a message to arrive from any neighbour before removing it from
	 * the incoming queue. A round-robin selection method is used to prevent
	 * strangulation of any incoming queue.
	 * 
	 * @note This method will block until a message arrives.
	 * @return The incoming message.
	 */
	public <MsgType extends IMessage<?>> MsgType receive();

	/**
	 * Waits for a message to arrive from a particular neighbour before removing
	 * it from the incoming queue.
	 * 
	 * @note This method will block until a message arrives from the specified
	 *       neighbour.
	 * @param neighbour
	 *            The neighbouring node from which a message is expected.
	 * @return A message from the specified neighbour.
	 */
	public <MsgType extends IMessage<?>> MsgType receive(INode neighbour);

	/**
	 * @return A list of adjacent neighbours.
	 */
	public Iterator<INode> neighbours();

	/**
	 * @return The number of neighbours connected to an <code>INode</code>.
	 */
	public int neighbourCount();

	/**
	 * @return The integer identifier attached to an <code>INode</code>.
	 */
	public int getMachineId();

	/**
	 * @param n
	 *            the node to which we want the link.
	 * @return The link joining this node and n, or null if none exists.
	 */
	public ILink neighbourLink(INode n);

	/**
	 * @param n
	 *            a node other than this.
	 * @return Whether n is a neighbour.
	 */
	public boolean isNeighbour(INode n);

	/**
	 * All neighbours to an INode must be addressable given a unique, but
	 * arbitrary, index from 0 to <code>neigbourcount()</code> - 1. This index
	 * is permanent once a neighbour is added.
	 * 
	 * @param index
	 *            the index of a neighbour, between 0 and
	 *            <code>neighbourCount()</code> - 1.
	 * @return The INode neighbour with the index.
	 */
	public INode getNeighbour(int index);

	/**
	 * @param sourceNode
	 *            A neighbouring node.
	 * @return The index addressing that node.
	 */
	public int getIndex(INode sourceNode);

	/**
	 * Change the colour of the node at this point in simulation.
	 * 
	 * @param colour
	 *            the new colour of the node.
	 */
	public void setColour(Color colour);

	/**
	 * @return If the node is the initiator of the sim.
	 */
	public boolean isInitiator();

	/**
	 * @return The unique ID given to the machine.
	 */
	public int getSimulationId();

}
