package net.alexheavens.cs4099.visualisation;

import java.util.TimerTask;

public class ClockTickTask extends TimerTask {

	private final VisualisationClock source;
	private final int changeCheck;

	public ClockTickTask(VisualisationClock source) {
		super();
		synchronized (source) {
			this.source = source;
			changeCheck = source.changeCheck;
		}
	}

	public void run() {
		synchronized (source) {
			if (changeCheck == source.changeCheck)
				source.nextTick();
		}
	}
}
