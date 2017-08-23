package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.IMessageImpl;
import net.alexheavens.cs4099.network.INodeImpl;

public class MessageArrivalEvent extends MessageEvent {

	public MessageArrivalEvent(IMessageImpl<?> message, long eventTime) {
		super(message, eventTime);
		if (message.getSentAt() == IMessageImpl.TIMESTEP_NOT_SENT)
			throw new IllegalArgumentException(
					"Message arrival event created where message had not bee sent.");
	}

	public void process(IEventController controller) {
		INodeImpl target = (INodeImpl) eventMessage.target();
		eventMessage.markAsArrived(controller.currentTimestep());
		target.queueMessage(eventMessage);
	}

	public SimEventType getEventType() {
		return SimEventType.MESSAGE_ARRIVAL;
	}
}
