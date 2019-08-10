package net.alexheavens.cs4099.network;

/**
 * Defines the message interface perceivable to to a user node.
 * 
 * @author Alexander Heavens
 * @version 1.0
 * @param <DataType>
 *            The type of the data that is contained within the message. For
 *            example, a String or Integer.
 */
public interface IMessage<DataType> {

	/**
	 * The maximum length that the String identifier of the message can have.
	 */
	public static final int MAX_TAG_LENGTH = 32;

	/**
	 * @return The data stored within the message.
	 */
	public DataType getData();

	/**
	 * @return The tag attached to the data, or null if none.
	 */
	public String getTag();

	/**
	 * Finds the index of the source as seen by the target.
	 * 
	 * @return an index between 0 and <code>getNeighbourCount()</code> of the
	 *         target.
	 */
	public int sourceIndex();

}
