package net.alexheavens.cs4099.visualisation;

import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;

/**
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class MessageMap {

	private final HashMap<Long, HashSet<VisualisationMessage>> map;

	public MessageMap(VisualisationMessage[] messageJSon) {
		map = createMessageMap(messageJSon);
	}

	public Collection<VisualisationMessage> getMessages(long timestep) {
		while (map.get(timestep) == null && timestep >= 0)
			timestep--;
		if (timestep < 0)
			return null;
		return map.get(timestep);
	}

	private HashMap<Long, HashSet<VisualisationMessage>> createMessageMap(
			VisualisationMessage[] messages) {
		HashMap<Long, HashSet<VisualisationMessage>> map = new HashMap<Long, HashSet<VisualisationMessage>>();

		// Add each message to the HashSet for each timestep entry in the map
		// that it is in transit at
		for (VisualisationMessage message : messages) {
			final long sendTime = message.getSentAt();
			final long arriveTime = message.getArrivedAt();
			for (long timestep = sendTime; timestep < arriveTime; timestep++) {
				if (!map.containsKey(timestep))
					map.put(timestep, new HashSet<VisualisationMessage>());
				map.get(timestep).add(message);
			}
			if (!map.containsKey(arriveTime))
				map.put(arriveTime, new HashSet<VisualisationMessage>());
		}
		return map;
	}

}
