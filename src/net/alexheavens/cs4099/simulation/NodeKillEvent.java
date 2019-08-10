package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.Node;

/**
 * Marks the halting of a node's execution. This event is provided to allow the
 * death of a node to be scheduled before simulation. To aid this, the node is
 * specified by its ID, rather than directly.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class NodeKillEvent extends SimulationEvent implements PrescribedEvent {

	private final int nodeId;
	private Node node;

	public NodeKillEvent(long timestep, int nodeId) {
		super(timestep);
		this.nodeId = nodeId;
	}

	@Override
	public void process(IEventController controller) throws DeadNodeException {
		node.halt();
	}

	@Override
	public SimEventType getEventType() {
		return SimEventType.NODE_REMOTE_KILL;
	}

	/**
	 * @return The ID of the node that is to be halted.
	 */
	public int getNodeId() {
		return nodeId;
	}

	@Override
	public void prepare(SimulationRunner runner) {
		node = runner.network().nodes().get(nodeId);
	}

}
