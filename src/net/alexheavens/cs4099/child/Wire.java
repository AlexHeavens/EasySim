package net.alexheavens.cs4099.child;

import net.alexheavens.cs4099.child.Frame.Message;
import net.alexheavens.cs4099.child.Frame.Payload;
import net.alexheavens.cs4099.network.IMessage;
import net.alexheavens.cs4099.network.MessageImpl;
import net.alexheavens.cs4099.network.NumericMessage;
import net.alexheavens.cs4099.network.StringMessage;

/** Converts between the messages a script sees and the messages on the wire (ADR 0002). Both sides use it. */
final class Wire {

	private Wire() {
	}

	static Message toWire(IMessage<?> message) {
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

	/** Rebuilds a message the script sends. It has no source index; the link sets the sender when it arrives. */
	static MessageImpl<?> fromWire(Message wire) {
		return switch (wire.data()) {
		case Payload.Text p -> new StringMessage(wire.tag(), p.value());
		case Payload.Whole p -> new NumericMessage(wire.tag(), p.value());
		case Payload.Fractional p -> new NumericMessage(wire.tag(), p.value());
		case Payload.Constant p -> throw new UnsupportedOperationException("Enum messages are not supported yet");
		};
	}

	/** Rebuilds a message that arrived from the neighbour with the given index. */
	static MessageImpl<?> fromWire(Message wire, int neighbour) {
		MessageImpl<?> message = fromWire(wire);
		message.setSourceIndex(neighbour);
		return message;
	}
}
