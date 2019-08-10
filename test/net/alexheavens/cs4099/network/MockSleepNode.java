package net.alexheavens.cs4099.network;

import net.alexheavens.cs4099.usercode.NodeScript;

public class MockSleepNode extends Node {

	private final static long SLEEP_TIME = 1000;

	public MockSleepNode() {
		super(new NodeScript() {

			@Override
			public void execute() {
			}
		});
	}

	public void setup() {
	}

	public void execute() {
		try {
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
