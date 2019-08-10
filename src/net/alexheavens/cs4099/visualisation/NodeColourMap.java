package net.alexheavens.cs4099.visualisation;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import prefuse.data.Node;

public class NodeColourMap {

	private Map<Node, SortedMap<Long, Integer>> colourMap = new HashMap<Node, SortedMap<Long, Integer>>();

	public void addNodeColour(Node node, long timestep, int colour) {
		if (!colourMap.containsKey(node))
			colourMap.put(node, new TreeMap<Long, Integer>());
		colourMap.get(node).put(timestep, colour);
	}

	public Integer getNodeColour(Node node, long timestep) {
		if (!colourMap.containsKey(node))
			return null;
		final SortedMap<Long, Integer> nodeColours = colourMap.get(node);
		Integer lastColour = null;
		for (Long nextTime : colourMap.get(node).keySet()) {
			if (nextTime > timestep)
				return lastColour;
			else
				lastColour = nodeColours.get(nextTime);
		}
		return lastColour;
	}
}
