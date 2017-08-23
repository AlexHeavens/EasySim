package net.alexheavens.cs4099;

import java.util.Observable;

/**
 * ApplicationModel acts as a container for the state produced by simulation and
 * network topology creation.
 * 
 * As the size of the data that can be contained in the ApplicationModel can be
 * sizable, capacities of NetworkConfigs and SimulationResults are enforced to
 * ensure memory efficiency.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class ApplicationModel extends Observable {

	private Exception simException = null;

	/**
	 * Remove the log exception that has occurred.
	 */
	public void resetException() {
		simException = null;
	}

	/**
	 * Set the simulation exception that has occurred.
	 * 
	 * @param e
	 *            an exception that has occurred as a result of the user's
	 *            actions.
	 */
	public synchronized void setSimulationException(Exception e) {
		simException = e;
		setChanged();
		notifyObservers();
		clearChanged();
	}

	/**
	 * @return the latest simulation exception that has occurred, or null if it
	 *         has been cleared.
	 */
	public synchronized Exception getSimulationException() {
		return simException;
	}

}
