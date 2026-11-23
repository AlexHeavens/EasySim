package net.alexheavens.cs4099.network;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;

import net.alexheavens.cs4099.child.ChildNodes;
import net.alexheavens.cs4099.network.configuration.ILinkConfig;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;

/**
 * The representation of a simulated network.
 * 
 * Provides access to all links and nodes within the network.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class Network implements INetwork {

	private final ArrayList<ILinkImpl> links = new ArrayList<ILinkImpl>();
	private final ArrayList<Node> nodes = new ArrayList<Node>();
	private final ChildNodes childNodes;

	/** Creates an empty network. */
	public Network() {
		childNodes = null;
	}

	/**
	 * Creates a network for a particular configuration, with one randomly chosen initiator node.
	 * 
	 * @param config
	 *            the configuration of the network.
	 * @param scrambleIds
	 *            if the machine IDs of the nodes are to be scrambled.
	 * @param nodeFactory
	 *            creates each node, given whether it is the initiator.
	 */
	public Network(INetworkConfig config, boolean scrambleIds,
			Function<Boolean, Node> nodeFactory) {
		this(null, config, scrambleIds, nodeFactory);
	}

	/**
	 * Creates a network whose nodes run the student script in a child JVM.
	 * 
	 * @param childNodes
	 *            the child JVM that runs the scripts. The caller closes it after the simulation.
	 * @param scriptClass
	 *            the name of the script class, which the child compiles from source or loads from the classpath.
	 * @param config
	 *            the configuration of the network.
	 * @param scrambleIds
	 *            if the machine IDs of the nodes are to be scrambled.
	 */
	public Network(ChildNodes childNodes, String scriptClass,
			INetworkConfig config, boolean scrambleIds) {
		this(childNodes, config, scrambleIds,
				initiator -> childNodes.node(scriptClass, initiator));
	}

	private Network(ChildNodes childNodes, INetworkConfig config,
			boolean scrambleIds, Function<Boolean, Node> nodeFactory) {
		this.childNodes = childNodes;

		final Random randomGenerator = new Random();
		final int count = config.nodeCount();
		final int initiator = (count == 0) ? -1 : randomGenerator.nextInt(count);
		final int scrambleCode = (scrambleIds) ? randomGenerator.nextInt(count) : 0;

		for (int i = 0; i < count; i++)
			addNode(nodeFactory.apply(i == initiator), scrambleCode);

		addConfig(config);
	}

	/** @return The child JVM that runs the nodes' scripts, or null if they run in this JVM. */
	public ChildNodes childNodes() {
		return childNodes;
	}

	private void addConfig(INetworkConfig config) {
		for (ILinkConfig link : config.links()) {
			INodeImpl source = nodes.get(link.source());
			INodeImpl target = nodes.get(link.target());
			createLink(source, target, link.latency());
		}
	}

	public void addNode(Node userNode, int scrambleCode) {
		if (userNode.getSimulationId() >= 0
				&& userNode.getSimulationId() < nodes.size())
			throw new IllegalArgumentException(DUP_NODE_MSG);
		if (userNode.getSimulationId() != INodeImpl.INIT_MACHINE_ID)
			throw new IllegalArgumentException(INVALID_NODE_MSG);
		userNode.setSimulationId(nodes.size(), scrambleCode);
		nodes.add(userNode);
	}

	public ArrayList<ILinkImpl> links() {
		return links;
	}

	public ArrayList<Node> nodes() {
		return nodes;
	}

	public int nodeCount() {
		return nodes.size();
	}

	public int linkCount() {
		return links.size();
	}

	public void createLink(INodeImpl source, INodeImpl target, long latency) {
		if (!nodes.contains(source))
			throw new IllegalArgumentException(
					"attempted to add a link from a non-network node");
		if (!nodes.contains(target))
			throw new IllegalArgumentException(
					"attempted to add a link to a non-network node");
		if (source == target)
			throw new IllegalArgumentException(
					"cannot create link with the same source and target");
		ILinkImpl link = source.addNeighbour(target, latency);
		links.add(link);
	}

}
