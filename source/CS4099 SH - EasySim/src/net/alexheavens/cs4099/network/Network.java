package net.alexheavens.cs4099.network;

import java.util.ArrayList;
import java.util.Random;

import net.alexheavens.cs4099.network.configuration.ILinkConfig;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.simulation.ImportedNode;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * The representation of a simulated network.
 * 
 * Provides access to all links and nodes within the network.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class Network implements INetwork {

	private ArrayList<ILinkImpl> links;
	private ArrayList<Node> nodes;
	private final ImportedNode userNodeClass;

	/**
	 * Creates an empty network.
	 * 
	 * @param nodeClass
	 *            the class of script that the nodes within the network will
	 *            execute.
	 */
	public Network(Class<? extends NodeScript> nodeClass) {
		userNodeClass = ImportedNode.createType(nodeClass);
		links = new ArrayList<ILinkImpl>();
		nodes = new ArrayList<Node>();
	}

	/**
	 * Creates a network for a particular configuration.
	 * 
	 * @param config
	 *            the configuration of the network.
	 * @param nodeClass
	 *            the class of script that the nodes within the network will
	 *            execute.
	 * @throws InstantiationException
	 *             if the node class cannot be instantiated.
	 * @throws IllegalAccessException
	 *             if a field cannot be accessed in the script class.
	 */
	public Network(INetworkConfig config, Class<? extends Node> nodeClass)
			throws InstantiationException, IllegalAccessException {
		userNodeClass = ImportedNode.createType(nodeClass);
		links = new ArrayList<ILinkImpl>();
		nodes = new ArrayList<Node>();
		for (int i = 0; i < config.nodeCount(); i++) {
			addNode((Node) userNodeClass.create(), 0);
		}

		addConfig(config);
	}

	/**
	 * Creates a network for a particular configuration.
	 * 
	 * @param config
	 *            the configuration of the network.
	 * @param nodeClass
	 *            the class of script that the nodes within the network will
	 *            execute.
	 * @param scrambleIds
	 *            if the machine IDs of the nodes are to be scambled.
	 * @throws InstantiationException
	 *             if the node class cannot be instantiated.
	 * @throws IllegalAccessException
	 *             if a field cannot be accessed in the script class.
	 */
	public Network(Class<? extends NodeScript> nodeClass,
			INetworkConfig config, boolean scrambleIds)
			throws InstantiationException, IllegalAccessException {
		this(nodeClass);

		// Create a single, random initiator node.
		final Random randomGenerator = new Random();
		final int initiator = randomGenerator.nextInt(config.nodeCount());
		final int scrambleCode = (scrambleIds) ? randomGenerator.nextInt(config
				.nodeCount()) : 0;

		for (int i = 0; i < config.nodeCount(); i++) {
			Node newNode = new Node((NodeScript) userNodeClass.create(),
					i == initiator);
			addNode(newNode, scrambleCode);
		}

		addConfig(config);
	}

	private void addConfig(INetworkConfig config)
			throws InstantiationException, IllegalAccessException {
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
