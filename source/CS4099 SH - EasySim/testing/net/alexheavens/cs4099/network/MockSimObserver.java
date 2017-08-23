package net.alexheavens.cs4099.network;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class MockSimObserver implements Observer {

	protected ArrayList<Object> updates;
	private Object lastObservable;
	private Object lastUpdate;
	private int updateCount;

	public MockSimObserver() {
		lastObservable = null;
		lastUpdate = null;
		updateCount = 0;
		updates = new ArrayList<Object>();
	}

	public void update(Observable observable, Object update) {
		lastObservable = observable;
		lastUpdate = update;
		updates.add(update);
		updateCount++;
	}

	public Object lastObservable() {
		return lastObservable;
	}

	public Object lastUpdate() {
		return lastUpdate;
	}

	public int updateCount() {
		return updateCount;
	}

	public Object getUpdate(int i) {
		return updates.get(i);
	}

}
