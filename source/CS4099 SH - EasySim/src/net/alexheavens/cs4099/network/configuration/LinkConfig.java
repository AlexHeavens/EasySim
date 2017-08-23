package net.alexheavens.cs4099.network.configuration;

/**
 * Defines the configuration of a link in a network config.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class LinkConfig implements ILinkConfig {

	private final int sourceNode;
	private final int targetNode;
	private final long lat;

	/**
	 * Creates a new link given the IDs of two source nodes.
	 * 
	 * @param source
	 *            the source ID.
	 * @param target
	 *            the target ID.
	 * @param latency
	 *            the latency of the link.
	 */
	public LinkConfig(int source, int target, long latency) {
		if (source < 0)
			throw new IllegalArgumentException(
					"Negative link source identifier.");
		if (target < 0)
			throw new IllegalArgumentException(
					"Negative link target identifier.");
		if (latency < 1)
			throw new IllegalArgumentException("Latency less than 1 timestep.");
		if (source == target)
			throw new IllegalArgumentException("Identical source and target.");

		sourceNode = source;
		targetNode = target;
		lat = latency;
	}

	public long latency() {
		return lat;
	}

	public int source() {
		return sourceNode;
	}

	public int target() {
		return targetNode;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof ILinkConfig))
			return false;
		ILinkConfig b = (ILinkConfig) obj;
		return ((sourceNode == b.source() && targetNode == b.target()) || (sourceNode == b
				.target() && targetNode == b.source()));
	}

	public int hashCode() {
		int sourceCode = sourceNode & 0x0000FFFF;
		int targetCode = targetNode & 0x0000FFFF;
		return (sourceNode > targetNode) ? sourceCode
				| targetCode << (Integer.SIZE / 2) : targetCode
				| sourceNode << (Integer.SIZE / 2);
	}

}
