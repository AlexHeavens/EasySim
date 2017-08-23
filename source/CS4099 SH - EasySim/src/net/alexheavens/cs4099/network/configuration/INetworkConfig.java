package net.alexheavens.cs4099.network.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import prefuse.data.Graph;
import net.alexheavens.cs4099.simulation.NodeKillEvent;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * Stores the configuration of a network in preparation for the actual
 * <code>Network</code> generation. Configurations can be loaded from, and
 * stored in, JSON formatted text files.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @see ILinkConfig
 */
public interface INetworkConfig {

	/**
	 * The JSON tag of a network configuration file, specifying the number of
	 * nodes are within a network.
	 */
	public static final String NODE_COUNT_JSON = "nodes";

	/**
	 * The JSON tag of a network configuration file, containing the array of
	 * links.
	 */
	public static final String LINKS_JSON = "edges";

	/**
	 * The exception message given if an <code>INetworkConfig</code> is
	 * initialised with an invalid number of nodes.
	 */
	public static final String INVALID_NODES_MSG = "An invalid number of nodes was specified for this configuration.";

	/**
	 * The exception message given if an <code>ILinkConfig</code> is added with
	 * an invalid source identifier.
	 */
	public static final String UNKNOWN_LINK_SOURCE_MSG = "Link source is not within the network.";

	/**
	 * The exception message given if an <code>ILinkConfig</code> is added with
	 * an invalid target identifier.
	 */
	public static final String UNKNOWN_LINK_TARGET_MSG = "Link target is not within the network.";

	/**
	 * The exception message given if an <code>ILinkConfig</code> is added more
	 * than once.
	 */
	public static final String DUP_LINK_MSG = "Link has been added previously.";

	/**
	 * A tag specifying the mapping of death events for particular nodes at
	 * particular times in simulation. This is used for storing the config in
	 * JSON format.
	 */
	public static final String DEATH_EVENTS_TAG = "deathEvents";

	/**
	 * @return The number of nodes contained within the configuration.
	 */
	public int nodeCount();

	/**
	 * @return The number of links between nodes in the configuration.
	 */
	public int linkCount();

	/**
	 * Define a link between two nodes in the configuration.
	 * 
	 * @param link
	 *            the configuration of the new link.
	 */
	public void addLink(ILinkConfig link);

	/**
	 * @return The links within the configuration.
	 */
	public Collection<ILinkConfig> links();

	/**
	 * Adds to a <code>INetworkConfig</code> the configuration specified by the
	 * JSON file, given by the file-path <code>file</code>. The added nodes will
	 * be given greater machine identifiers to ensure no confusion between a
	 * previously added node.
	 * 
	 * @param file
	 *            the file-path of the JSON-based network configuration.
	 * @throws NetworkConfigException
	 *             in the event of a duplicate link.
	 * @throws JSONException
	 *             in the event of an error in JSON conversion.
	 * @throws IOException
	 *             should an error occur in reading from the configuration file.
	 */
	public void appendFromFile(File file) throws NetworkConfigException,
			JSONException, IOException;

	/**
	 * Writes the current network configuration to a file, specified by the path
	 * <code>file</code>. If a file by the given name does not exist, it will be
	 * created.
	 * 
	 * @param file
	 *            the path of the created JSON file.
	 * @throws IOException
	 *             should an error occur in the creation or writing of the
	 *             configuration.
	 */
	public void writeToFile(File file) throws IOException;

	/**
	 * Writes the current network configuration to a JSONObject detailing the
	 * number of nodes and an edge list with latencies.
	 * 
	 * @return A JSON representation of the network configuration.
	 */
	public JSONObject toJSONObject();

	/**
	 * @return A Prefuse compatible graph for use in visualisation.
	 */
	public Graph toPrefuseGraph();

	/**
	 * Specifies a list of death events that will occur for nodes within any
	 * simulation that uses this configuration.
	 * 
	 * @param eventlist
	 *            a mapping of the ID of nodes to die to the timestep that they
	 *            die.
	 */
	public void setNodeDeathEvents(Map<Integer, Long> eventlist);

	/**
	 * @return A mapping of Node IDs that are designated to be terminated at
	 *         specific timesteps.
	 */
	public Map<Integer, Long> getNodeDeathEvents();

	/**
	 * @return A set of events corresponding to the deaths of all nodes.
	 */
	public Set<NodeKillEvent> generateDeathEvents();

}
