package net.alexheavens.cs4099.network;

/**
 * A messages that can hold a number as the data.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class NumericMessage extends MessageImpl<Number> {

	/**
	 * Creates a new message without a tag.
	 * 
	 * @param data the data stored.
	 */
	public NumericMessage(Number data) {
		this(null, data);
	}

	/**
	 * Creates a new message.
	 * 
	 * @param tag the tag stored.
	 * @param data the data stored.
	 */
	public NumericMessage(String tag, Number data) {
		super(tag, data);
	}

	public final NumericMessage clone() {
		return (NumericMessage) super.clone();
	}
}
