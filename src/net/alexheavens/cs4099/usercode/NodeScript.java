package net.alexheavens.cs4099.usercode;

import java.awt.Color;

import net.alexheavens.cs4099.network.IMessage;
import net.alexheavens.cs4099.network.INode;
import net.alexheavens.cs4099.simulation.InvalidCallException;

/**
 * A NodeScript is a script of execution used in simulation of a network
 * algorithm.
 * 
 * Each Node within a simulated network must be given direction from the user.
 * By overriding the NodeScript class, this behaviour can be defined in a manner
 * similar to any Thread.
 * 
 * A NodeScript can specify neighbouring Nodes by identifying them with their
 * index. This is a value between 0 (inclusive) and neighbourCount()
 * (exclusive). All indexes to the neighbours of a given Npde are unique, but no
 * ordering is given to the user.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 * 
 * @see Thread
 */
public abstract class NodeScript {

	private INode node;

	/**
	 * The setup() method is called for all Nodes within a Network prior to the
	 * start of simulation.
	 * 
	 * When called, the setup() method is guaranteed to have access to the
	 * machine ID of the Node. As simulation has yet to begin, no calls to
	 * pause(), send(), setColour() or receive() can be made, which will result
	 * in an {@link InvalidCallException}.
	 */
	public void setup() {

	}

	/**
	 * Sets the underlying Node acting on behalf of the script.
	 * 
	 * This method is called automatically by the SimulationRunner after a
	 * NodeScript's instantiation. It is not intended for user user and its use
	 * within user code will result in failure of the Node within simulation.
	 * 
	 * @param node
	 *            the Node that interacts with the simulation on behalf of the
	 *            NodeScript.
	 */
	public final void setNode(INode node) {
		if (this.node != null)
			throw new IllegalStateException("Attempted to set node twice");
		this.node = node;
	}

	/**
	 * Marks a change in the colouring of a Node at the timestep in which this
	 * method is called.
	 * 
	 * This call will affect the visualisation of this Node accordingly during
	 * visualisation.
	 * 
	 * @param colour
	 *            the colour that the Node will now appear as.
	 */
	public void setColour(Color colour) {
		node.setColour(colour);
	}

	/**
	 * Defines the execution behaviour of the Node within simulation.
	 * 
	 * This method must be overridden by a user code definition. It is
	 * guaranteed to be called at timestep 0 for all Nodes that do not fail in
	 * the setup phase of simulation.
	 */
	public abstract void execute();

	/**
	 * Pauses the execution of a Node for a given number of simulation
	 * timesteps.
	 * 
	 * During a pause, the Node will remain in a waiting state until woken by
	 * the simulation with a corresponding unpause event.
	 * 
	 * @param timesteps
	 *            the number of timesteps the Node will pause for.
	 */
	public void pause(long timesteps) {
		node.pause(timesteps);
	}

	/**
	 * Sends a Message to a neighbouring Node, arriving after the given link
	 * latency to that Node.
	 * 
	 * @param recipient
	 *            the index of the recipient neighbour.
	 * @param message
	 *            either a Numeric or String message.
	 */
	public void send(int recipient, IMessage<?> message) {
		INode recipientNode = node.getNeighbour(recipient);
		node.send(recipientNode, message);
	}

	/**
	 * Sends a Message to all neighbouring Nodes, arriving after their
	 * respective link latencies to that Node.
	 * 
	 * @param message
	 *            either a Numeric or String message.
	 */
	public void sendAll(IMessage<?> message) {
		node.sendAll(message);
	}

	/**
	 * Waits for a message from any neighbour to arrive.
	 * 
	 * If no message is in a message queue for this Node, this method will block
	 * until one is available.
	 * 
	 * @return the most recent message within any message queue for the Node, or
	 *         null if no neighbours are attached to this Node.
	 */
	public <MsgType extends IMessage<?>> MsgType receive() {
		return node.receive();
	}

	/**
	 * Waits for a message from a particular neighbour to arrive.
	 * 
	 * If no message is in the nieghbour's message queue for this Node, this
	 * method will block until one is available.
	 * 
	 * @param neighbour
	 *            the index of the neighbour from which a message is required.
	 * @return the most recent message within the message queue corresponding to
	 *         the specified neighbour for this Node, or null if no neighbours
	 *         are attached to this Node.
	 */
	public <MsgType extends IMessage<?>> MsgType receive(int neighbour) {
		INode senderNode = node.getNeighbour(neighbour);
		return node.receive(senderNode);
	}

	/**
	 * @return the number of neighbouring Nodes connected to this.
	 */
	public int neighbourCount() {
		return node.neighbourCount();
	}

	/**
	 * @return the unique identifier for this Node within simulation.
	 */
	public int machineId() {
		return node.getMachineId();
	}

	/**
	 * @return if the Node is the single initiator within the Network.
	 */
	public boolean isInitiator() {
		return node.isInitiator();
	}

}
