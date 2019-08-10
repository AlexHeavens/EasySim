package net.alexheavens.cs4099.visualisation;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class VisualisationMessageEvent extends VisualisationEvent {

	private final VisualisationMessage message;

	public VisualisationMessageEvent(JSONObject json,
			VisualisationMessage message) throws JSONException {
		super(json);
		this.message = message;
	}

	public VisualisationMessage getMessage() {
		return message;
	}

}
