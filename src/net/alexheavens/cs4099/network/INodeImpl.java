package net.alexheavens.cs4099.network;

import net.alexheavens.cs4099.concurrent.WaitRegistrar;
import net.alexheavens.cs4099.simulation.ISimulationEvent;
import net.alexheavens.cs4099.simulation.ProfiledProcess;
import net.alexheavens.cs4099.simulation.SimulationProfiler;
import net.alexheavens.cs4099.simulation.SimulationState;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * INodeImpl defines the functionality contained within a Node that should not
 * be used by the user.
 * 
 * @author Alexander Heavens
 * @version 1.0
 */
public interface INodeImpl extends INode, ProfiledProcess {

	/**
	 * The default machineId for an uninitialised node.
	 */
	public final static int INIT_MACHINE_ID = -1;

	/**
	 * The exception message given should a node be linked with a null node.
	 */
	public static final String NULL_NEIGHBOUR_MSG = "Attempted to add a NULL neighbour.";

	/**
	 * The exception message given should a node be linked with another twice.
	 */
	public static final String DUP_NEIGHBOUR_MSG = "Attempted to add a Node as a neighbour twice.";

	/**
	 * The exception message given should a node be linked with an uninitialised
	 * neighbour.
	 */
	public static final String UNINIT_NEIGHBOUR_MSG = "Attempted to add an unitialised Node as a neighbour.";

	/**
	 * The exception message given should a node be given a negative machine
	 * identifier.
	 */
	public static final String INVALID_ID_MSG = "Attempted to set machine Id to below 0.";

	/**
	 * The exception message given should a node be given any machine identifier
	 * more than once.
	 */
	public static final String ID_SET_TWICE_MSG = "Attempted to set machine Id twice.";

	/**
	 * Adds a node to this node's list of neighbours, simultaneously it's own
	 * list and creating the appropriate Link.
	 * 
	 * @param n
	 *            the node to add.
	 * @param latency
	 *            the delay a message will have when sent along the link.
	 * @return The link created as a result of adding <code>n</code> as a
	 *         neighbour.
	 */
	public ILinkImpl addNeighbour(INodeImpl n, long latency);

	/**
	 * @param id
	 *            the integer identifier attached to this node.
	 * @param scrambleCode
	 *            an offset attached to the simulation ID to give the machine
	 *            ID.
	 */
	public void setSimulationId(int id, int scrambleCode);

	/**
	 * Begins simulation of the node. The setup method will first be called,
	 * followed by execute.
	 * 
	 * @param registrar
	 *            the location that the simulation threads will wait when in a
	 *            blocking state.
	 * @param profiler
	 *            the profiler used to monitor node execution.
	 */
	public void simulate(WaitRegistrar registrar, SimulationProfiler profiler);

	/**
	 * @return The current state of the node's simulation.
	 */
	public SimulationState getSimulationState();

	/**
	 * Causes an event to be passed to any observers.
	 * 
	 * @param event
	 *            an event that has occurred within the node.
	 */
	public void raiseEvent(ISimulationEvent event);

	/**
	 * Resumes execution of the node from a pause.
	 */
	public void unpause();

	/**
	 * Queues a message for this node.
	 * 
	 * This adds the message to the appropriate link queue.
	 * 
	 * @param message
	 *            the message we wish to queue.
	 */
	public void queueMessage(IMessageImpl<?> message);

	/**
	 * Causes the execution of the node to halt on the next blocking operation.
	 */
	public void halt();

	/**
	 * @return the thread executing the user script.
	 */
	public Thread getThread();

	/**
	 * @return the user being executed by this node.
	 */
	public NodeScript getScript();

}
