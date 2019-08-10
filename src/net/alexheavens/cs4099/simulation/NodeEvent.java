package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.INodeImpl;

public abstract class NodeEvent extends SimulationEvent {

	public final static String NODE_ID_TAG = "nodeId";
	
	protected final INodeImpl eventNode;
	private final int nodeId;

	public NodeEvent(long eventTime, INodeImpl node) {
		super(eventTime);
		if (node == null)
			throw new IllegalArgumentException(
					"Attempted to create NodeEvent with null node.");
		eventNode = node;
		nodeId = node.getSimulationId();
	}

	public void process(IEventController controller) throws DeadNodeException {
		if (eventNode.getSimulationState() == SimulationState.POST_SIMULATION)
			throw new DeadNodeException();
	}

	public INodeImpl node() {
		return eventNode;
	}

	public int getNodeId() {
		return nodeId;
	}

}
