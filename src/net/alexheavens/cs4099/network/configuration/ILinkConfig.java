package net.alexheavens.cs4099.network.configuration;

/**
 * Defines a link in the network topology provided by a
 * <code>INetworkConfig</code>.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @see INetworkConfig
 */
public interface ILinkConfig {

	/**
	 * @return The integer identifier of the source node of this link.
	 */
	public int source();

	/**
	 * @return The integer identifier of the target node of this link.
	 */
	public int target();

	/**
	 * @return The delay, in simulation timesteps, that messages will take to
	 *         transmit across this link.
	 */
	public long latency();

}
