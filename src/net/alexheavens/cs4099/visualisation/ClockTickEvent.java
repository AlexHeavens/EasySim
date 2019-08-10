package net.alexheavens.cs4099.visualisation;

public class ClockTickEvent extends ClockEvent {

	private static final long serialVersionUID = 1L;
	private final int tick;
	
	public ClockTickEvent(VisualisationClock source, int tick) {
		super(source);
		this.tick = tick;
	}
	
	public int getTick(){
		return tick;
	}

}
