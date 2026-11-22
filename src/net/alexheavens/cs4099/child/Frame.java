package net.alexheavens.cs4099.child;

/**
 * A message between the simulator and the child JVM that runs the student nodes (ADR 0002). Requests come from a
 * node and carry a request id; the reply with the same id unparks it.
 */
public sealed interface Frame {

	/** The node the frame is about. */
	int node();

	/** A message's content: a length-limited string, a number, or an enum constant (ADR 0002). */
	sealed interface Payload {
		record Text(String value) implements Payload {
		}

		record Whole(long value) implements Payload {
		}

		record Fractional(double value) implements Payload {
		}

		/** An enum constant of a type the simulator ships, named rather than serialised. */
		record Constant(String type, String name) implements Payload {
		}
	}

	record Message(String tag, Payload data) {
	}

	// Requests, child to simulator.
	record Send(int node, long request, int neighbour, Message message) implements Frame {
	}

	record SendAll(int node, long request, Message message) implements Frame {
	}

	/** Waits for a message; {@link #ANY_NEIGHBOUR} accepts one from any neighbour. */
	record Receive(int node, long request, int neighbour) implements Frame {
		public static final int ANY_NEIGHBOUR = -1;
	}

	record Pause(int node, long request, long steps) implements Frame {
	}

	record SetColour(int node, long request, int rgb) implements Frame {
	}

	record Neighbours(int node, long request) implements Frame {
	}

	// Replies, simulator to child.
	record Accepted(int node, long request) implements Frame {
	}

	record Received(int node, long request, int neighbour, Message message) implements Frame {
	}

	record Resumed(int node, long request) implements Frame {
	}

	record NeighbourList(int node, long request, int[] neighbours) implements Frame {
	}

	// Commands, simulator to child.
	record Start(int node, String scriptClass, boolean initiator, int machineId) implements Frame {
	}

	record Kill(int node) implements Frame {
	}

	// Events, child to simulator.
	/** The node's body returned, was killed ({@code failure} is null), or threw. */
	record Ended(int node, String failure) implements Frame {
	}
}
