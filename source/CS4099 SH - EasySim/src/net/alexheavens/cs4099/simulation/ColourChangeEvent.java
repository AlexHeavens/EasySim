package net.alexheavens.cs4099.simulation;

import java.awt.Color;

import net.alexheavens.cs4099.network.INodeImpl;

/**
 * Denotes the change of colour in a node.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class ColourChangeEvent extends NodeEvent {

	private final int colour;

	/**
	 * @param colour
	 *            the colour changed to.
	 * @param node
	 *            the node that is changing colour.
	 * @param eventTime
	 *            the timestep at which it occurs.
	 */
	public ColourChangeEvent(Color colour, INodeImpl node, long eventTime) {
		super(eventTime, node);
		this.colour = colour.getRGB();
	}

	@Override
	public SimEventType getEventType() {
		return SimEventType.COLOUR_CHANGE_EVENT;
	}

	/**
	 * @return the colour the node changed to.
	 */
	public int getColour() {
		return colour;
	}

}
