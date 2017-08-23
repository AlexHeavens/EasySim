package net.alexheavens.cs4099.network.configuration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import prefuse.data.Graph;

import net.alexheavens.cs4099.simulation.NodeKillEvent;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Used to store the topology and preseribed events of a network.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class NetworkConfig implements INetworkConfig {

	private static int READ_WRITE_BUFFER_SIZE = 1024;
	private int nodeCount;

	private HashSet<ILinkConfig> links;
	private Map<Integer, Long> nodeDeathTimesteps;

	/**
	 * Creates an empty network.
	 */
	public NetworkConfig() {
		this(0);
		nodeCount = 0;
	}

	/**
	 * Creates a network with some nodes.
	 * 
	 * @param nNodes
	 *            the number of nodes in the network.
	 */
	public NetworkConfig(int nNodes) {
		if (nNodes < 0)
			throw new IllegalArgumentException(INVALID_NODES_MSG);
		nodeCount = nNodes;
		links = new HashSet<ILinkConfig>();
	}

	/**
	 * Creates a network configuration from a JSON formatted file.
	 * 
	 * @param file
	 *            the file tor read from.
	 * @throws JSONException
	 *             if the JSON is malformed
	 * @throws NetworkConfigException
	 *             if the network is malformed.
	 * @throws IOException
	 *             if the file cannot be read.
	 */
	public NetworkConfig(File file) throws JSONException,
			NetworkConfigException, IOException {
		this();
		appendFromFile(file);
	}

	/**
	 * Creates a network configuration from a JSON formatted String.
	 * 
	 * @param networkJson
	 *            the String from which the configuraition is read.
	 * @throws JSONException
	 *             if the JSON is malformed.
	 * @throws NetworkConfigException
	 *             if the network is malformed.
	 */
	public NetworkConfig(JSONObject networkJson) throws JSONException,
			NetworkConfigException {
		this();
		appendFromJSON(networkJson);
	}

	@Override
	public synchronized void addLink(ILinkConfig link) {
		if (link.source() >= nodeCount)
			throw new IllegalArgumentException(UNKNOWN_LINK_SOURCE_MSG);
		if (link.target() >= nodeCount)
			throw new IllegalArgumentException(UNKNOWN_LINK_TARGET_MSG);
		if (links.contains(link))
			throw new IllegalArgumentException(DUP_LINK_MSG);
		links.add(link);
	}

	@Override
	public synchronized Collection<ILinkConfig> links() {
		return links;
	}

	@Override
	public synchronized int nodeCount() {
		return nodeCount;
	}

	@Override
	public synchronized int linkCount() {
		return links.size();
	}

	@Override
	public synchronized void appendFromFile(File file)
			throws NetworkConfigException, JSONException, IOException {

		// Prepare to read from file.
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		StringBuilder jsonString = new StringBuilder();

		// Read from file.
		char[] readBuffer = new char[READ_WRITE_BUFFER_SIZE];
		int nRead;
		while ((nRead = bufferedReader.read(readBuffer)) > 0) {
			for (int i = 0; i < nRead; i++)
				jsonString.append(readBuffer[i]);
		}

		// Convert the String to a JSONObject.
		JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(jsonString
				.toString());

		appendFromJSON(jsonObject);

		// Close the file.
		bufferedReader.close();
		fileReader.close();
	}

	/**
	 * Adds the configuration from a JSONObject to the config.
	 * 
	 * @param jsonObject
	 *            the object from which the config is appended.
	 * @throws NetworkConfigException
	 *             if the network is malformed.
	 */
	public synchronized void appendFromJSON(JSONObject jsonObject)
			throws NetworkConfigException {
		// Calculate the Node count.
		nodeCount += jsonObject.getInt(NODE_COUNT_JSON);
		JSONArray jsonLinks = jsonObject.getJSONArray(LINKS_JSON);

		// Create a link for each specified in the JSONObject.
		for (Iterator<?> it = jsonLinks.iterator(); it.hasNext();) {
			JSONArray jLink = (JSONArray) it.next();
			ILinkConfig link = new LinkConfig(jLink.getInt(0), jLink.getInt(1),
					jLink.getInt(2));
			if (links.contains(link))
				throw new NetworkConfigException("More than one edge between <"
						+ link.source() + "> and <" + link.target()
						+ "> is declared.");
			links.add(link);
		}

		// Create the mapping of node IDs to death times if they exist.
		if (jsonObject.containsKey(INetworkConfig.DEATH_EVENTS_TAG)) {
			final JSONObject deathJson = jsonObject
					.getJSONObject(INetworkConfig.DEATH_EVENTS_TAG);
			nodeDeathTimesteps = new HashMap<Integer, Long>(deathJson.size());
			for (Object nodeId : deathJson.keySet()) {
				nodeDeathTimesteps.put(new Integer((String) nodeId),
						deathJson.getLong((String) nodeId));
			}
		}
	}

	@Override
	public synchronized void writeToFile(File file) throws IOException {

		// Open the file.
		FileWriter fileWriter = new FileWriter(file);
		BufferedWriter writer = new BufferedWriter(fileWriter);

		// Write the node count.
		writer.write("{\n");
		writer.write("\t\'nodes\': " + nodeCount + ",\n");

		// Write the edges.
		writer.write("\t\'edges\': [\n");
		for (Iterator<ILinkConfig> linkIt = links.iterator(); linkIt.hasNext();) {
			ILinkConfig link = linkIt.next();
			String commaString = (linkIt.hasNext()) ? "," : "";
			writer.write("\t\t[" + link.source() + ", " + link.target() + ", "
					+ link.latency() + "]" + commaString + "\n");
		}
		writer.write("\t]");

		// Write the node death events
		if (nodeDeathTimesteps != null) {
			writer.write(",\n\t\'" + INetworkConfig.DEATH_EVENTS_TAG
					+ "\': {\n");
			for (Iterator<Integer> nodeIdIt = nodeDeathTimesteps.keySet()
					.iterator(); nodeIdIt.hasNext();) {
				final int nodeId = nodeIdIt.next();
				final String commaString = (nodeIdIt.hasNext()) ? "," : "";
				writer.write("\t\t\"" + nodeId + "\": "
						+ nodeDeathTimesteps.get(nodeId) + commaString + "\n");
			}
			writer.write("\t}\n");
		} else {
			writer.write("\n");
		}

		// Close the file.
		writer.write("}\n");
		writer.flush();
		writer.close();
		fileWriter.close();
	}

	@Override
	public synchronized JSONObject toJSONObject() {
		JSONObject netJson = new JSONObject();
		netJson.element("nodes", nodeCount);
		JSONArray edgeList = new JSONArray();
		for (ILinkConfig link : links) {
			JSONArray edge = new JSONArray();
			edge.add(0, link.source());
			edge.add(1, link.target());
			edge.add(2, link.latency());
			edgeList.add(edge);
		}

		netJson.element("edges", edgeList);
		if (nodeDeathTimesteps != null) {
			final JSONObject deathJson = new JSONObject();
			for (int nodeId : nodeDeathTimesteps.keySet()) {
				deathJson.element(nodeId + "", nodeDeathTimesteps.get(nodeId));
			}
			netJson.element(INetworkConfig.DEATH_EVENTS_TAG, deathJson);
		}
		return netJson;
	}

	@Override
	public synchronized Graph toPrefuseGraph() {

		final Graph graph = new Graph();
		graph.addColumn("label", String.class);
		for (int i = 0; i < nodeCount; i++) {
			prefuse.data.Node node = graph.addNode();
			node.setString("label", i + "");
		}

		graph.addColumn("latency", long.class);
		for (ILinkConfig link : links) {
			int i = graph.addEdge(link.source(), link.target());
			graph.getEdge(i).setLong("latency", link.latency());
		}

		return graph;
	}

	@Override
	public synchronized void setNodeDeathEvents(Map<Integer, Long> eventlist) {
		// Check that validity of the events passed.
		for (int nodeId : eventlist.keySet()) {
			if (nodeId < 0 || nodeId >= nodeCount)
				throw new IllegalArgumentException(
						"Node ID of Node death event is invalid: " + nodeId);
			if (eventlist.get(nodeId) < 0)
				throw new IllegalArgumentException(
						"Timestep of death for Node ID " + nodeId
								+ " is invalid: " + eventlist.get(nodeId));
		}
		nodeDeathTimesteps = eventlist;
	}

	@Override
	public synchronized Map<Integer, Long> getNodeDeathEvents() {
		return nodeDeathTimesteps;
	}

	@Override
	public Set<NodeKillEvent> generateDeathEvents() {
		if (nodeDeathTimesteps == null)
			return null;
		final Set<NodeKillEvent> killEvents = new HashSet<NodeKillEvent>(
				nodeDeathTimesteps.size());
		for (int nodeId : nodeDeathTimesteps.keySet()) {
			killEvents.add(new NodeKillEvent(nodeDeathTimesteps.get(nodeId),
					nodeId));
		}
		return killEvents;
	}

}
