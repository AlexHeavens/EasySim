package net.alexheavens.cs4099.child;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.apache.commons.javaflow.api.continuable;

import net.alexheavens.cs4099.scheduler.ContinuationScheduler;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * Runs the student nodes inside the child JVM (ADR 0002). It reads commands from the simulator, runs each node as a
 * continuation on one {@link ContinuationScheduler}, and writes each node's requests and its end back.
 */
public final class NodeHost {

	private final DataInput in;
	private final DataOutput out;
	private final Function<String, NodeScript> scripts;
	private final ContinuationScheduler scheduler;
	private final ConcurrentHashMap<Integer, RemoteNode> nodes = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, ContinuationScheduler.Task> tasks = new ConcurrentHashMap<>();
	private final AtomicLong requestIds = new AtomicLong();

	/** {@code scripts} creates a script from the class name in a {@link Frame.Start}. */
	public NodeHost(DataInput in, DataOutput out, Function<String, NodeScript> scripts,
			ContinuationScheduler scheduler) {
		this.in = in;
		this.out = out;
		this.scripts = scripts;
		this.scheduler = scheduler;
	}

	/** Handles frames until the simulator closes the connection. */
	public void run() throws IOException {
		try {
			while (true)
				handle(FrameCodec.read(in));
		} catch (EOFException e) {
			// The simulator closed the connection.
		}
	}

	private void handle(Frame frame) {
		switch (frame) {
		case Frame.Start start -> start(start);
		case Frame.Kill kill -> {
			ContinuationScheduler.Task task = tasks.get(kill.node());
			if (task != null)
				task.kill();
		}
		case Frame.Accepted f -> reply(f.node(), f.request(), f);
		case Frame.Received f -> reply(f.node(), f.request(), f);
		case Frame.Resumed f -> reply(f.node(), f.request(), f);
		case Frame.NeighbourList f -> reply(f.node(), f.request(), f);
		default -> throw new IllegalStateException("Frame not meant for the child: " + frame);
		}
	}

	private void reply(int node, long request, Frame reply) {
		RemoteNode remote = nodes.get(node);
		if (remote != null)
			remote.reply(request, reply);
	}

	private void start(Frame.Start start) {
		RemoteNode remote = new RemoteNode(start.node(), start.initiator(), start.machineId(), this::write,
				requestIds);
		nodes.put(start.node(), remote);
		// The script is loaded inside the node's own task, so compiling it never delays reading a Kill.
		ContinuationScheduler.Task task = scheduler.start(new NodeBody(scripts, start.scriptClass(), remote));
		remote.bind(task);
		tasks.put(start.node(), task);
		task.completion().whenComplete((ignored, failure) -> {
			write(new Frame.Ended(start.node(), failure == null ? null : describe(failure)));
			nodes.remove(start.node());
			tasks.remove(start.node());
		});
	}

	private static String describe(Throwable failure) {
		String text = failure.toString();
		return text.length() <= FrameCodec.MAX_STRING_LENGTH ? text : text.substring(0, FrameCodec.MAX_STRING_LENGTH);
	}

	private void write(Frame frame) {
		try {
			synchronized (out) {
				FrameCodec.write(out, frame);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private record NodeBody(Function<String, NodeScript> scripts, String scriptClass, RemoteNode remote)
			implements Runnable {
		@Override
		@continuable
		public void run() {
			NodeScript script = scripts.apply(scriptClass);
			script.setNode(remote);
			script.setup();
			remote.endSetup();
			script.execute();
		}
	}
}
