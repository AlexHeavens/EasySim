package net.alexheavens.cs4099.network;

public class MockPauseNode extends MockUserNode {

	public static final long DEFAULT_PAUSE_TIME = 20;
	public static final int DEFAULT_PAUSE_COUNT = 1000;

	private long waitTime;
	private int waitPeriods;
	private int pauseCount;

	public MockPauseNode() {
		this(Node.INIT_MACHINE_ID, DEFAULT_PAUSE_TIME, DEFAULT_PAUSE_COUNT);
	}
	
	public MockPauseNode(int i){
		this(i, DEFAULT_PAUSE_TIME, DEFAULT_PAUSE_COUNT);
	}

	public MockPauseNode(int i, long w, int p) {
		machineId = i;
		waitTime = w;
		waitPeriods = p;
		pauseCount = 0;
	}

	public void execute() {
		for (int i = 0; i < waitPeriods; i++) {
			pauseCount++;
			pause(waitTime);
		}

		executedCount++;
	}

	public int pauseCount() {
		return pauseCount;
	}
}
