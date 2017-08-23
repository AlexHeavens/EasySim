package net.alexheavens.cs4099.visualisation;

import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class NodeSimulationProfile {

	private final SortedMap<Long, Long> execTimes;
	private final SortedMap<Long, Integer> messagesSent, messagesReceived,
			messagesRead;
	private long totalSent, totalRecieved, totalRead = 0;

	public NodeSimulationProfile() {
		execTimes = new TreeMap<Long, Long>();
		messagesSent = new TreeMap<Long, Integer>();
		messagesReceived = new TreeMap<Long, Integer>();
		messagesRead = new TreeMap<Long, Integer>();
	}

	public void setExecTime(long timestep, long time) {
		execTimes.put(timestep, time);
	}

	public void incrementMessagesSent(long timestep) {
		if (!messagesSent.containsKey(timestep))
			messagesSent.put(timestep, 1);
		else
			messagesSent.put(timestep, messagesSent.get(timestep) + 1);
	}

	public void incrementMessagesReceived(long timestep) {
		if (!messagesReceived.containsKey(timestep))
			messagesReceived.put(timestep, 1);
		else
			messagesReceived.put(timestep, messagesReceived.get(timestep) + 1);
		totalRecieved++;
	}

	public void incrementMessagesRead(long timestep) {
		if (!messagesRead.containsKey(timestep))
			messagesRead.put(timestep, 1);
		else
			messagesRead.put(timestep, messagesRead.get(timestep) + 1);
		totalRead++;
	}

	public Set<Entry<Long, Long>> getExecutionTimes() {
		return execTimes.entrySet();
	}

	public Set<Entry<Long, Integer>> getMessagesSent() {
		return messagesSent.entrySet();
	}

	public Set<Entry<Long, Integer>> getMessagesReceived() {
		return messagesReceived.entrySet();
	}

	public Set<Entry<Long, Integer>> getMessagesRead() {
		return messagesRead.entrySet();
	}

	public long getTotalMessagesSent() {
		return totalSent;
	}

	public long getTotalMessagesReceived() {
		return totalRecieved;
	}

	public long getTotalMessagesRead() {
		return totalRead;
	}
}
