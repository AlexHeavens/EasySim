package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.INodeImpl;

public class NodeFailureEvent extends NodeEvent {

	public static final int PRIORITY = 2;
	private final Exception cause;

	public NodeFailureEvent(long eventTime, INodeImpl node, Exception ex) {
		super(eventTime, node);
		cause = ex;
	}

	public void process(IEventController controller) {

		System.err.println("Node ID " + getNodeId()
				+ " has failed at timestep " + getTimestep()
				+ " as a result of the following exception: "
				+ getFailure().toString() + "");
		final StackTraceElement[] trace = cause.getStackTrace();
		for (int i = 0; i < trace.length; i++) {
			System.err.println(trace[i]);
		}
		System.err.println();
	}

	public void process(SimulationRunner runner) {
	}

	public SimEventType getEventType() {
		return SimEventType.NODE_FAILURE;
	}

	public int priority() {
		return PRIORITY;
	}

	public Exception getFailure() {
		return cause;
	}

}
