package net.alexheavens.cs4099.child;

import java.awt.Color;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.javaflow.api.continuable;

import net.alexheavens.cs4099.child.Frame.Message;
import net.alexheavens.cs4099.child.Frame.Payload;
import net.alexheavens.cs4099.network.ILink;
import net.alexheavens.cs4099.network.IMessage;
import net.alexheavens.cs4099.network.INode;
import net.alexheavens.cs4099.network.MessageImpl;
import net.alexheavens.cs4099.network.NumericMessage;
import net.alexheavens.cs4099.network.StringMessage;
import net.alexheavens.cs4099.scheduler.ContinuationScheduler;

/**
 * The node API a student script sees inside the child JVM. Each call becomes a request frame, and the node parks on
 * the scheduler until the simulator's reply arrives (ADR 0002). A neighbour is only an index, so
 * {@link #getNeighbour(int)} returns a handle that other calls accept but that does nothing itself.
 */
final class RemoteNode implements INode {

	private final int id;
	private final FrameSink sink;
	private final AtomicLong requestIds;
	private final ConcurrentHashMap<Long, Pending> pending = new ConcurrentHashMap<>();
	private final boolean initiator;
	private final int machineId;
	private final int neighbourIndex;
	private volatile ContinuationScheduler.Task task;
	private volatile int[] neighbours;

	/** Where request frames go. */
	interface FrameSink {
		void write(Frame frame);
	}

	private static final class Pending {
		volatile Frame reply;
	}

	RemoteNode(int id, boolean initiator, int machineId, FrameSink sink, AtomicLong requestIds) {
		this.id = id;
		this.initiator = initiator;
		this.machineId = machineId;
		this.sink = sink;
		this.requestIds = requestIds;
		this.neighbourIndex = -1;
	}

	private RemoteNode(int neighbourIndex) {
		this.id = -1;
		this.initiator = false;
		this.machineId = -1;
		this.sink = null;
		this.requestIds = null;
		this.neighbourIndex = neighbourIndex;
	}

	void bind(ContinuationScheduler.Task task) {
		this.task = task;
	}

	/** Called by the host's reader when a reply for this node arrives. */
	void reply(long request, Frame reply) {
		Pending waiting = pending.get(request);
		if (waiting == null)
			return;
		waiting.reply = reply;
		task.unpark();
	}

	@continuable
	private Frame call(java.util.function.LongFunction<Frame> request) {
		long requestId = requestIds.incrementAndGet();
		Pending waiting = new Pending();
		pending.put(requestId, waiting);
		sink.write(request.apply(requestId));
		while (waiting.reply == null)
			ContinuationScheduler.park();
		pending.remove(requestId);
		return waiting.reply;
	}

	@Override
	@continuable
	public void pause(long pauseSteps) {
		call(r -> new Frame.Pause(id, r, pauseSteps));
	}

	@Override
	@continuable
	public void send(INode recipient, IMessage<?> message) {
		int index = ((RemoteNode) recipient).neighbourIndex;
		Message wire = toWire(message);
		call(r -> new Frame.Send(id, r, index, wire));
	}

	@Override
	@continuable
	public void sendAll(IMessage<?> message) {
		Message wire = toWire(message);
		call(r -> new Frame.SendAll(id, r, wire));
	}

	@Override
	@continuable
	@SuppressWarnings("unchecked")
	public <MsgType extends IMessage<?>> MsgType receive() {
		return (MsgType) fromWire((Frame.Received) call(r -> new Frame.Receive(id, r, Frame.Receive.ANY_NEIGHBOUR)));
	}

	@Override
	@continuable
	@SuppressWarnings("unchecked")
	public <MsgType extends IMessage<?>> MsgType receive(INode neighbour) {
		int index = ((RemoteNode) neighbour).neighbourIndex;
		return (MsgType) fromWire((Frame.Received) call(r -> new Frame.Receive(id, r, index)));
	}

	@Override
	@continuable
	public int neighbourCount() {
		if (neighbours == null)
			neighbours = ((Frame.NeighbourList) call(r -> new Frame.Neighbours(id, r))).neighbours();
		return neighbours.length;
	}

	@Override
	@continuable
	public void setColour(Color colour) {
		call(r -> new Frame.SetColour(id, r, colour.getRGB()));
	}

	@Override
	public INode getNeighbour(int index) {
		return new RemoteNode(index);
	}

	@Override
	public int getIndex(INode sourceNode) {
		return ((RemoteNode) sourceNode).neighbourIndex;
	}

	@Override
	public int getMachineId() {
		return machineId;
	}

	@Override
	public boolean isInitiator() {
		return initiator;
	}

	private static Message toWire(IMessage<?> message) {
		Object data = message.getData();
		Payload payload;
		if (data instanceof String text)
			payload = new Payload.Text(text);
		else if (data instanceof Long || data instanceof Integer || data instanceof Short || data instanceof Byte)
			payload = new Payload.Whole(((Number) data).longValue());
		else if (data instanceof Double || data instanceof Float)
			payload = new Payload.Fractional(((Number) data).doubleValue());
		else
			throw new IllegalArgumentException("Messages may carry only a string or a number, not "
					+ (data == null ? "null" : data.getClass().getName()));
		return new Message(message.getTag(), payload);
	}

	private static IMessage<?> fromWire(Frame.Received received) {
		Message wire = received.message();
		MessageImpl<?> message = switch (wire.data()) {
		case Payload.Text p -> new StringMessage(wire.tag(), p.value());
		case Payload.Whole p -> new NumericMessage(wire.tag(), p.value());
		case Payload.Fractional p -> new NumericMessage(wire.tag(), p.value());
		case Payload.Constant p -> throw new UnsupportedOperationException("Enum messages are not supported yet");
		};
		message.setSourceIndex(received.neighbour());
		return message;
	}

	// The rest of the API has no meaning inside the child.
	@Override
	public void setup() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void execute() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<INode> neighbours() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ILink neighbourLink(INode n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isNeighbour(INode n) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getSimulationId() {
		throw new UnsupportedOperationException();
	}
}
