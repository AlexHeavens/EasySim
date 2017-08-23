package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.INodeImpl;

public class NodeReceiveBlockEvent extends NodeEvent {

	public NodeReceiveBlockEvent(long eventTime, INodeImpl node) {
		super(eventTime, node);
	}

	public SimEventType getEventType() {
		return SimEventType.NODE_RECEIVE_BLOCK;
	}

}
