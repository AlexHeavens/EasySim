package net.alexheavens.cs4099.child;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import net.alexheavens.cs4099.network.INode;
import net.alexheavens.cs4099.network.MessageImpl;
import net.alexheavens.cs4099.network.Node;
import net.alexheavens.cs4099.simulation.NodeHaltedException;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * Stands in for a student script in the simulator (ADR 0002). The student's code runs in the child JVM. This script
 * runs on the {@link Node}'s own thread and carries out each request the child sends on the node, so the barrier,
 * the events and the kill paths of {@link Node} work as they do for a script that runs in this JVM.
 */
final class ChildScript extends NodeScript {

	private final ChildJvm child;
	private final String scriptClass;
	private final BlockingQueue<Object> inbox = new LinkedBlockingQueue<>();
	private final CountDownLatch exited = new CountDownLatch(1);
	private volatile boolean started;
	private Object first;
	private volatile Node node;

	ChildScript(ChildJvm child, String scriptClass) {
		this.child = child;
		this.scriptClass = scriptClass;
	}

	/** Gives the script the node it acts on. Called once, before the simulation starts. */
	void bind(Node node) {
		this.node = node;
	}

	/** Ends the node's bridge after the run has failed. */
	void fail(ChildJvm.Failure failure) {
		inbox.add(failure);
	}

	/** Waits until the node's bridge has stopped, if it ever started, so that any kill request has been sent. */
	void awaitExit() throws InterruptedException {
		if (started)
			exited.await();
	}

	/**
	 * Starts the student's node and waits for its first request, so the child's start-up, the compile of the script
	 * and the student's setup do not count against the time the node may take in a timestep.
	 */
	@Override
	public void setup() {
		started = true;
		int id = node.getSimulationId();
		child.start(id, scriptClass, node.isInitiator(), node.getMachineId(), inbox::add);
		try {
			first = inbox.take();
		} catch (InterruptedException e) {
			stop(id);
		}
	}

	@Override
	public void execute() {
		try {
			bridge();
		} finally {
			exited.countDown();
		}
	}

	private void bridge() {
		int id = node.getSimulationId();
		try {
			Object item = first;
			boolean ended = false;
			while (!ended) {
				if (item instanceof ChildJvm.Failure failure)
					throw new IllegalStateException(failure.reason());
				ended = handle(id, (Frame) item);
				if (!ended)
					item = inbox.take();
			}
		} catch (InterruptedException e) {
			stop(id);
		} catch (RuntimeException e) {
			child.kill(id);
			throw e;
		}
	}

	/** The node was halted or timed out. The child stops its node, or fails the run if it cannot. */
	private void stop(int id) {
		child.kill(id);
		exited.countDown();
		throw new NodeHaltedException();
	}

	/** Carries out one frame on the node. Returns true once the node's body has ended. */
	private boolean handle(int id, Frame frame) {
		switch (frame) {
		case Frame.Send f -> {
			node.send(node.getNeighbour(f.neighbour()), Wire.fromWire(f.message()));
			child.send(new Frame.Accepted(id, f.request()));
		}
		case Frame.SendAll f -> {
			node.sendAll(Wire.fromWire(f.message()));
			child.send(new Frame.Accepted(id, f.request()));
		}
		case Frame.Receive f -> {
			INode from = f.neighbour() == Frame.Receive.ANY_NEIGHBOUR ? null : node.getNeighbour(f.neighbour());
			MessageImpl<?> message = from == null ? node.receive() : node.receive(from);
			if (message == null)
				throw new IllegalStateException("Called receive on a node with no neighbours.");
			child.send(new Frame.Received(id, f.request(), message.sourceIndex(), Wire.toWire(message)));
		}
		case Frame.Pause f -> {
			node.pause(f.steps());
			child.send(new Frame.Resumed(id, f.request()));
		}
		case Frame.SetColour f -> {
			node.setColour(new Color(f.rgb(), true));
			child.send(new Frame.Accepted(id, f.request()));
		}
		case Frame.Neighbours f -> {
			int[] indexes = new int[node.neighbourCount()];
			for (int i = 0; i < indexes.length; i++)
				indexes[i] = i;
			child.send(new Frame.NeighbourList(id, f.request(), indexes));
		}
		case Frame.Ended f -> {
			if (f.failure() != null)
				throw new IllegalStateException(f.failure());
			return true;
		}
		default -> throw new IllegalStateException("Frame not meant for the simulator: " + frame);
		}
		return false;
	}
}
