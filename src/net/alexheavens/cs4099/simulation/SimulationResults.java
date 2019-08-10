package net.alexheavens.cs4099.simulation;

import java.util.HashMap;
import java.util.Map;

public class SimulationResults {

	private final IEventLog events;
	private final Map<Integer, Map<Long, Long>> timeMap;

	public SimulationResults(IEventLog iEventLog,
			Map<Integer, Map<Long, Long>> nodeTimes) {
		this.events = iEventLog;
		this.timeMap = nodeTimes;
	}

	public IEventLog getEvents() {
		return events;
	}

	public Map<String, Map<String, Long>> getJSONTimeMap() {
		final Map<String, Map<String, Long>> stringTimeMap = new HashMap<String, Map<String,Long>>(timeMap.size());
		for(Integer id : timeMap.keySet()){
			final Map<Long, Long> processMap = timeMap.get(id);
			final Map<String, Long> stringProcessMap = new HashMap<String, Long>(processMap.size());
			stringTimeMap.put(id.toString(), stringProcessMap);
			for(Long timestep : processMap.keySet()){
				stringProcessMap.put(timestep.toString(), processMap.get(timestep));
			}
		}
		return stringTimeMap;
	}

}
