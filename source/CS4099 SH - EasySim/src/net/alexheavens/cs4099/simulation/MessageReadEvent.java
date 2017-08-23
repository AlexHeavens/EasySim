package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.IMessageImpl;

public class MessageReadEvent extends MessageEvent {

	public MessageReadEvent(IMessageImpl<?> message, long eventTime) {
		super(message, eventTime);
	}

	public void process(IEventController controller) {
	}

	public SimEventType getEventType() {
		return SimEventType.MESSAGE_READ;
	}

}
