package net.alexheavens.cs4099.visualisation;

import prefuse.data.Node;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class VisualisationNodeEvent extends VisualisationEvent {

	private final Node node;

	public Node getNode() {
		return node;
	}

	public VisualisationNodeEvent(JSONObject json, Node node)
			throws JSONException {
		super(json);
		this.node = node;
	}

}
