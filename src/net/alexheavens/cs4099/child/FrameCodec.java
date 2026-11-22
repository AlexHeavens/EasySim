package net.alexheavens.cs4099.child;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.alexheavens.cs4099.child.Frame.Payload;

/**
 * Reads and writes {@link Frame}s. A frame is one type byte followed by its fields. Strings are length-prefixed and
 * the length is checked before anything is allocated, so a buggy node cannot make the simulator allocate without
 * bound.
 */
public final class FrameCodec {

	/** Longest string a message, tag, class name or failure text may carry, in UTF-16 chars. */
	public static final int MAX_STRING_LENGTH = 1024;

	private static final int MAX_NEIGHBOURS = 1_000_000;

	private static final byte SEND = 1, SEND_ALL = 2, RECEIVE = 3, PAUSE = 4, SET_COLOUR = 5, NEIGHBOURS = 6,
			ACCEPTED = 7, RECEIVED = 8, RESUMED = 9, NEIGHBOUR_LIST = 10, START = 11, KILL = 12, ENDED = 13;
	private static final byte TEXT = 1, WHOLE = 2, FRACTIONAL = 3, CONSTANT = 4;

	private FrameCodec() {
	}

	public static void write(DataOutput out, Frame frame) throws IOException {
		switch (frame) {
		case Frame.Send f -> {
			out.writeByte(SEND);
			head(out, f.node(), f.request());
			out.writeInt(f.neighbour());
			message(out, f.message());
		}
		case Frame.SendAll f -> {
			out.writeByte(SEND_ALL);
			head(out, f.node(), f.request());
			message(out, f.message());
		}
		case Frame.Receive f -> {
			out.writeByte(RECEIVE);
			head(out, f.node(), f.request());
			out.writeInt(f.neighbour());
		}
		case Frame.Pause f -> {
			out.writeByte(PAUSE);
			head(out, f.node(), f.request());
			out.writeLong(f.steps());
		}
		case Frame.SetColour f -> {
			out.writeByte(SET_COLOUR);
			head(out, f.node(), f.request());
			out.writeInt(f.rgb());
		}
		case Frame.Neighbours f -> {
			out.writeByte(NEIGHBOURS);
			head(out, f.node(), f.request());
		}
		case Frame.Accepted f -> {
			out.writeByte(ACCEPTED);
			head(out, f.node(), f.request());
		}
		case Frame.Received f -> {
			out.writeByte(RECEIVED);
			head(out, f.node(), f.request());
			out.writeInt(f.neighbour());
			message(out, f.message());
		}
		case Frame.Resumed f -> {
			out.writeByte(RESUMED);
			head(out, f.node(), f.request());
		}
		case Frame.NeighbourList f -> {
			out.writeByte(NEIGHBOUR_LIST);
			head(out, f.node(), f.request());
			out.writeInt(f.neighbours().length);
			for (int neighbour : f.neighbours())
				out.writeInt(neighbour);
		}
		case Frame.Start f -> {
			out.writeByte(START);
			out.writeInt(f.node());
			string(out, f.scriptClass());
			out.writeBoolean(f.initiator());
			out.writeInt(f.machineId());
		}
		case Frame.Kill f -> {
			out.writeByte(KILL);
			out.writeInt(f.node());
		}
		case Frame.Ended f -> {
			out.writeByte(ENDED);
			out.writeInt(f.node());
			nullableString(out, f.failure());
		}
		}
	}

	public static Frame read(DataInput in) throws IOException {
		byte type = in.readByte();
		return switch (type) {
		case SEND -> new Frame.Send(in.readInt(), in.readLong(), in.readInt(), readMessage(in));
		case SEND_ALL -> new Frame.SendAll(in.readInt(), in.readLong(), readMessage(in));
		case RECEIVE -> new Frame.Receive(in.readInt(), in.readLong(), in.readInt());
		case PAUSE -> new Frame.Pause(in.readInt(), in.readLong(), in.readLong());
		case SET_COLOUR -> new Frame.SetColour(in.readInt(), in.readLong(), in.readInt());
		case NEIGHBOURS -> new Frame.Neighbours(in.readInt(), in.readLong());
		case ACCEPTED -> new Frame.Accepted(in.readInt(), in.readLong());
		case RECEIVED -> new Frame.Received(in.readInt(), in.readLong(), in.readInt(), readMessage(in));
		case RESUMED -> new Frame.Resumed(in.readInt(), in.readLong());
		case NEIGHBOUR_LIST -> {
			int node = in.readInt();
			long request = in.readLong();
			int count = in.readInt();
			if (count < 0 || count > MAX_NEIGHBOURS)
				throw new IOException("Neighbour count out of range: " + count);
			int[] neighbours = new int[count];
			for (int i = 0; i < count; i++)
				neighbours[i] = in.readInt();
			yield new Frame.NeighbourList(node, request, neighbours);
		}
		case START -> new Frame.Start(in.readInt(), readString(in), in.readBoolean(), in.readInt());
		case KILL -> new Frame.Kill(in.readInt());
		case ENDED -> new Frame.Ended(in.readInt(), readNullableString(in));
		default -> throw new IOException("Unknown frame type: " + type);
		};
	}

	private static void head(DataOutput out, int node, long request) throws IOException {
		out.writeInt(node);
		out.writeLong(request);
	}

	private static void message(DataOutput out, Frame.Message message) throws IOException {
		nullableString(out, message.tag());
		switch (message.data()) {
		case Payload.Text p -> {
			out.writeByte(TEXT);
			string(out, p.value());
		}
		case Payload.Whole p -> {
			out.writeByte(WHOLE);
			out.writeLong(p.value());
		}
		case Payload.Fractional p -> {
			out.writeByte(FRACTIONAL);
			out.writeDouble(p.value());
		}
		case Payload.Constant p -> {
			out.writeByte(CONSTANT);
			string(out, p.type());
			string(out, p.name());
		}
		}
	}

	private static Frame.Message readMessage(DataInput in) throws IOException {
		String tag = readNullableString(in);
		byte type = in.readByte();
		Payload data = switch (type) {
		case TEXT -> new Payload.Text(readString(in));
		case WHOLE -> new Payload.Whole(in.readLong());
		case FRACTIONAL -> new Payload.Fractional(in.readDouble());
		case CONSTANT -> new Payload.Constant(readString(in), readString(in));
		default -> throw new IOException("Unknown payload type: " + type);
		};
		return new Frame.Message(tag, data);
	}

	private static void string(DataOutput out, String value) throws IOException {
		if (value.length() > MAX_STRING_LENGTH)
			throw new IOException("String of " + value.length() + " chars exceeds " + MAX_STRING_LENGTH);
		out.writeShort(value.length());
		out.writeChars(value);
	}

	private static String readString(DataInput in) throws IOException {
		int length = in.readUnsignedShort();
		if (length > MAX_STRING_LENGTH)
			throw new IOException("String of " + length + " chars exceeds " + MAX_STRING_LENGTH);
		char[] chars = new char[length];
		for (int i = 0; i < length; i++)
			chars[i] = in.readChar();
		return new String(chars);
	}

	private static void nullableString(DataOutput out, String value) throws IOException {
		out.writeBoolean(value != null);
		if (value != null)
			string(out, value);
	}

	private static String readNullableString(DataInput in) throws IOException {
		return in.readBoolean() ? readString(in) : null;
	}
}
