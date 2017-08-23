package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.INodeImpl;

public class NodeUnpauseEvent extends NodeEvent {

	public NodeUnpauseEvent(long eventTime, INodeImpl node) {
		super(eventTime, node);
	}

	public void process(IEventController controller) throws DeadNodeException {
		super.process(controller);
		node().unpause();
	}
	
	public SimEventType getEventType(){
		return SimEventType.NODE_UNPAUSE;
	}

}
