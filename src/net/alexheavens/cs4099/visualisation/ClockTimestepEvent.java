package net.alexheavens.cs4099.visualisation;

public class ClockTimestepEvent extends ClockEvent {

	private static final long serialVersionUID = 1L;
	private final long timestep;
	
	public ClockTimestepEvent(VisualisationClock source, long timestep) {
		super(source);
		this.timestep = timestep;
	}
	
	public long getTimestep(){
		return timestep;
	}

}
