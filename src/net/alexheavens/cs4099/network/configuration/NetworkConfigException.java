package net.alexheavens.cs4099.network.configuration;

/**
 * An exception occurring on the incorrect configuration of a network.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class NetworkConfigException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception with a given String message.
	 * 
	 * @param message
	 *            the message attached to this exception.
	 */
	public NetworkConfigException(String message) {
		super(message);
	}
}
