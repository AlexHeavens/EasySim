package net.alexheavens.cs4099.visualisation;

import java.util.EventObject;

public class ClockEvent extends EventObject{

	private static final long serialVersionUID = 1L;

	public ClockEvent(VisualisationClock source) {
		super(source);
	}

}
