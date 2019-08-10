package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.ILink;
import net.alexheavens.cs4099.network.IMessageImpl;

/**
 * Marks the occurrence of a message being sent from one node in a network to
 * another.
 * 
 * @author Alexander Heavens
 * @version 1.0
 */
public class MessageSentEvent extends MessageEvent {

	/**
	 * Constructs an event that marks the sending of <code>message</code> at
	 * timestep <code>eventTime</code>
	 * 
	 * @param message
	 *            the message that has been sent.
	 * @param eventTime
	 *            the time the message was sent.
	 */
	public MessageSentEvent(IMessageImpl<?> message, long eventTime) {
		super(message, eventTime);
	}

	public void process(IEventController controller) {
		ILink link = message().link();
		message().markAsSent(controller.currentTimestep());
		long arrivalTime = controller.currentTimestep() + link.latency();
		MessageArrivalEvent arrivalEvent = new MessageArrivalEvent(message(),
				arrivalTime);
		controller.scheduleEvent(arrivalEvent);
	}

	public SimEventType getEventType() {
		return SimEventType.MESSAGE_SENT;
	}
}
