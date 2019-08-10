package net.alexheavens.cs4099.visualisation;

import net.alexheavens.cs4099.network.MessageImpl;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import prefuse.data.Graph;
import prefuse.data.Node;

public class VisualisationMessage {

	private final String tag, data;
	private final Node source, target;
	private final long sentAt, arrivedAt;
	
	public VisualisationMessage(JSONObject jsonMessage, Graph graph) throws JSONException{
		tag = jsonMessage.getString(MessageImpl.TAG_TAG);
		data = jsonMessage.get(MessageImpl.DATA_TAG).toString();
		sentAt = jsonMessage.getLong(MessageImpl.SENT_AT_TAG);
		arrivedAt = jsonMessage.getLong(MessageImpl.ARRIVED_AT_TAG);
		int sourceId = jsonMessage.getInt(MessageImpl.SOURCE_ID_TAG);
		int targetId = jsonMessage.getInt(MessageImpl.TARGET_ID_TAG);
		source = graph.getNode(sourceId);
		target = graph.getNode(targetId);
	}

	public String getTag() {
		return tag;
	}

	public String getData() {
		return data;
	}

	public Node getSource() {
		return source;
	}

	public Node getTarget() {
		return target;
	}

	public long getSentAt() {
		return sentAt;
	}

	public long getArrivedAt() {
		return arrivedAt;
	}
	
}
