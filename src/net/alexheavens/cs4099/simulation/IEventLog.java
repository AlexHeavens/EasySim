package net.alexheavens.cs4099.simulation;

import java.util.Set;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * The event log of a simulation maintains the events and state manipulated to
 * produce an accurate review of the simulation for use in visualisation.
 * 
 * @author Alexander Heavens
 * @version 1.0
 */
public interface IEventLog {

	/**
	 * Stores a copy of the information concerning <code>event</code> in the
	 * log. Common information (Node Ids, messages) will not be duplicated.
	 * 
	 * @param event
	 *            an event that has occurred during simulation.
	 */
	public void addEvent(ISimulationEvent event);

	/**
	 * @throws JSONException
	 *             on error converting to JSON.
	 * @return A JSONObject representation of the event and message logs.
	 */
	public JSONObject toJSONObject() throws JSONException;

	/**
	 * @parameter a timestep in simulation.
	 * @return The set of events that occured in the specific timestep.
	 */
	public Set<ISimulationEvent> getEventsInTimestep(long timestep);
}
