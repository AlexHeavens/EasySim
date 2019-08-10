package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.IMessageImpl;

/**
 * An event based upon an IMessage.
 * 
 * @author Alexander Heavens
 * @version 1.0
 */
public abstract class MessageEvent extends SimulationEvent {

	public static final String MESSAGE_ID_TAG = "messageId";

	protected final IMessageImpl<?> eventMessage;

	/**
	 * Creates a MessageEvent, marked to occur at timestep
	 * <code>eventTime</code>, that provides the IMessage <code>message</code>
	 * for use in event processing.
	 * 
	 * @param message
	 *            the event that a MessageEvent encapsulates.
	 * @param eventTime
	 *            the timestep at which this event is marked to occur.
	 */
	public MessageEvent(IMessageImpl<?> message, long eventTime) {
		super(eventTime);
		eventMessage = message;
	}

	/**
	 * @return The message encapsulated by this event.
	 */
	public IMessageImpl<?> message() {
		return eventMessage;
	}

}
