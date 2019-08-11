package net.alexheavens.cs4099.concurrent;

public class MockLeaderBarrierThread extends Thread {

	protected LeaderBarrier barrier;
	protected boolean passedBarrier;

	public MockLeaderBarrierThread() {
		passedBarrier = false;
	}

	public void setBarrier(LeaderBarrier barrier) {
		this.barrier = barrier;
	}

	public void run() {
		try {
			barrier.waitThread();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		passedBarrier = true;
	}

	public boolean hasPassedBarrier() {
		return passedBarrier;
	}

}
