package net.alexheavens.cs4099.visualisation;

import prefuse.data.Graph;
import net.alexheavens.cs4099.simulation.MessageEvent;
import net.alexheavens.cs4099.simulation.NodeEvent;
import net.alexheavens.cs4099.simulation.SimEventType;
import net.alexheavens.cs4099.simulation.SimulationEvent;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class VisualisationEvent {

	private final SimEventType type;
	private final long timestep;

	public VisualisationEvent(JSONObject json) throws JSONException {
		
		type = SimEventType.valueOf(json.getString(SimulationEvent.TYPE_TAG));
		timestep = json.getLong(SimulationEvent.TIME_TAG);
	}

	public SimEventType getType() {
		return type;
	}

	public long getTimestep() {
		return timestep;
	}
	
	public static VisualisationEvent generateFromJSON(JSONObject json, VisualisationMessage[] messages, Graph network){
		if(json.containsKey(MessageEvent.MESSAGE_ID_TAG)){
			int messageId = json.getInt(MessageEvent.MESSAGE_ID_TAG);
			return new VisualisationMessageEvent(json, messages[messageId]);
		}else if(json.containsKey(NodeEvent.NODE_ID_TAG)){
			return new VisualisationNodeEvent(json, network.getNode(json.getInt(NodeEvent.NODE_ID_TAG)));
		}
		return new VisualisationEvent(json);
	}
	
}
