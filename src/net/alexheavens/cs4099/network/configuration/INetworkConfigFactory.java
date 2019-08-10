package net.alexheavens.cs4099.network.configuration;

/**
 * Used to create complex network configurations.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @see INetworkConfig
 */
public interface INetworkConfigFactory {

	/**
	 * Generates a network configuration with a complete graph topology.
	 * 
	 * @param nNodes
	 *            the number of nodes within the network.
	 * @return The constructed network.
	 */
	public NetworkConfig createCompleteGraph(int nNodes);

	/**
	 * Generates a network configuration with a tree graph topology in which
	 * each parent has a fixed number of children.
	 * 
	 * @param generations
	 *            the number of rings of child nodes from the centre of the tree
	 *            (i.e. half the network's diameter).
	 * @param childrenPerParent
	 *            the number of children nodes that will branch from parents
	 *            (excluding leaf nodes).
	 * @return An INetworkConfig corresponding to the specified tree structure.
	 */
	public NetworkConfig createTreeNetwork(int generations,
			int childrenPerParent);

}
