package net.alexheavens.cs4099.visualisation;

public class ClockSpeedEvent extends ClockEvent {

	private static final long serialVersionUID = 1L;
	private final int ticksPerTimestep;

	public ClockSpeedEvent(VisualisationClock source, int ticksPerTimestep) {
		super(source);
		this.ticksPerTimestep = ticksPerTimestep;
	}

	public int getTicksPerTimestep() {
		return ticksPerTimestep;
	}

}
