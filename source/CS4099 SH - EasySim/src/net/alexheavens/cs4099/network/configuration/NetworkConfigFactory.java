package net.alexheavens.cs4099.network.configuration;

/**
 * Used to generate complex network topologies.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class NetworkConfigFactory implements INetworkConfigFactory {

	public NetworkConfig createCompleteGraph(int nNodes) {

		if (nNodes < 1)
			throw new IllegalArgumentException(
					"Attempted to create a complete graph with fewer than 1 node.");

		NetworkConfig newNet = new NetworkConfig(nNodes);

		// For each node.
		for (int node = 0; node < nNodes; node++) {

			// And each other node.
			for (int neighbour = 0; neighbour < nNodes; neighbour++) {

				// Create a link between them, if none already exists.
				if (node != neighbour) {
					ILinkConfig link = new LinkConfig(node, neighbour, 1);
					if (!newNet.links().contains(link))
						newNet.addLink(link);
				}
			}
		}

		return newNet;
	}

	public NetworkConfig createTreeNetwork(int generations,
			int childrenPerParent) {

		// Parameter checking.
		if (generations < 1) {
			throw new IllegalArgumentException(
					"Cannot create a tree network with fewer than 1 generation.");
		} else if (generations == 1) {
			if (childrenPerParent > 0)
				throw new IllegalArgumentException(
						"Cannot create a single generation tree network with children");
			if (childrenPerParent < 0)
				throw new IllegalArgumentException(
						"Cannot create a single generation tree network with negative children");
		} else if (childrenPerParent < 1) {
			throw new IllegalArgumentException(
					"Cannot create a multi-generation tree network with fewer than 1 child per parent.");
		}

		// Create a link-less network with enough nodes.
		final int nNodes = calcTreeNodeCount(generations, childrenPerParent);
		NetworkConfig newNet = new NetworkConfig(nNodes);

		int genStart = 0;
		int genSize = 0;

		// For each generation.
		for (int gen = 1; gen < generations; gen++) {
			genStart += genSize;
			genSize = (int) Math.pow(childrenPerParent, gen - 1);
			int genEnd = genStart + genSize;

			// Create enough links.
			for (int genPos = 0; genPos < genSize; genPos++) {
				int source = genStart + genPos;
				for (int child = 0; child < childrenPerParent; child++) {
					int target = genEnd + genPos * childrenPerParent + child;
					newNet.addLink(new LinkConfig(source, target, 1)); // TODO
																		// Add
																		// latency.
				}
			}
		}

		return newNet;
	}

	private int calcTreeNodeCount(int gens, final int children) {
		int nodes = 0;
		int gen = 0;
		while (gen < gens) {
			nodes += (int) Math.pow(children, gen);
			gen++;
		}
		return nodes;
	}

}
