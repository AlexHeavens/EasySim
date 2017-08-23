package net.alexheavens.cs4099.network;

/**
 * A messages that can hold a String as the data.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class StringMessage extends MessageImpl<String> {

	/**
	 * The maximum length of the data part of the string message.
	 */
	public static final int MAX_DATA_LENGTH = 32;

	/**
	 * Creates a new message without a tag.
	 * 
	 * @param data
	 *            the data stored.
	 */
	public StringMessage(String data) {
		this(null, data);
	}

	/**
	 * Creates a new message.
	 * 
	 * @param tag
	 *            the tag stored.
	 * @param data
	 *            the data stored.
	 */
	public StringMessage(String tag, String data) {
		super(tag, data);
		if (data.length() > MAX_DATA_LENGTH)
			throw new IllegalArgumentException(
					"Message data length exceeds maximum.");
	}

	public final StringMessage clone() {
		return (StringMessage) super.clone();
	}

}
