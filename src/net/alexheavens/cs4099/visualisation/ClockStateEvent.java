package net.alexheavens.cs4099.visualisation;

public class ClockStateEvent extends ClockEvent{

	private static final long serialVersionUID = 1L;
	private final VisualisationClock.State state;
	
	public ClockStateEvent(VisualisationClock source, VisualisationClock.State state) {
		super(source);
		this.state = state;
	}
	
	public VisualisationClock.State getState(){
		return state;
	}

}
