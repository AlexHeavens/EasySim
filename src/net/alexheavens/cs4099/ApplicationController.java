package net.alexheavens.cs4099;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.alexheavens.cs4099.network.Network;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfigException;
import net.alexheavens.cs4099.simulation.SimulationResults;
import net.alexheavens.cs4099.simulation.SimulationRunner;
import net.alexheavens.cs4099.usercode.ClassLimitation;
import net.alexheavens.cs4099.usercode.ClassLoaderException;
import net.alexheavens.cs4099.usercode.CodeValidator;
import net.alexheavens.cs4099.usercode.DynamicClassLoader;
import net.alexheavens.cs4099.usercode.LimitationFailureException;
import net.alexheavens.cs4099.usercode.NodeScript;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * The ApplicationController processes requests from the client to modify client
 * data and run simulations.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class ApplicationController {

	protected ApplicationModel model;
	private DynamicClassLoader dynamicClassLoader;

	/**
	 * Creates an <code>ApplicationController</code>, ensuring that a
	 * corresponding <code>ApplicationModel</code> is created.
	 * 
	 * @param model
	 */
	public ApplicationController(ApplicationModel model) {
		this.model = model;
		dynamicClassLoader = new DynamicClassLoader();
	}

	/**
	 * Creates a new ApplicationController with a default model.
	 */
	public ApplicationController() {
		this(new ApplicationModel());
	}

	/**
	 * Simulates user code under specified network conditions for a set number
	 * of timesteps.
	 * 
	 * The given user code is first validated to ensure that it does not include
	 * code that could be used to bypass the message passing system used in
	 * simulation.
	 * 
	 * The results of this simulation will be stored in the ApplicationState
	 * list of recent simulation results as a JSONObject. This JSONObject
	 * contains a NetworkConfiguration, and EventLog, under the tags 'network',
	 * 'events' and 'messages'. Should the list of recent simulation results be
	 * at capacity, the least recent simulation result will be discarded.
	 * 
	 * The simulation results will also be written to the file
	 * <code>output</code> in the same JSON format.
	 * 
	 * As a result of the simulation, the <code>net</code> will also be stored
	 * in the <code>ApplicationModel</code> of the controller as recently used.
	 * 
	 * @param net
	 *            the network configuration under which to simulate.
	 * @param length
	 *            the number of timesteps that the simulation will execute for.
	 * @param userCode
	 *            the user-defined Node that is replicated and simulated across
	 *            the network.
	 * @param output
	 *            the destination file that a copy of the simulation results
	 *            will be written to.
	 * @param limitation
	 *            the static limitations placed on the user code.
	 * @param timeout
	 *            the time a node can execute for in one timestep before being
	 *            timed out.
	 * @param scrambleIds
	 *            if the machine IDs seen by the user code are to be scrambled.
	 */
	public void simulate(INetworkConfig net, long length,
			Class<? extends NodeScript> userCode, ClassLimitation limitation,
			File output, long timeout, boolean scrambleIds) {

		boolean completed = false;
		boolean isNewFile = false;
		try {

			// First validate the user code.
			CodeValidator validator = new CodeValidator(limitation);
			validator.validate(userCode);

			isNewFile = output.createNewFile();

			Network simNetwork = new Network(userCode, net, scrambleIds);

			SimulationRunner simRunner = new SimulationRunner(simNetwork,
					length, net.generateDeathEvents(), timeout);
			final SimulationResults results = simRunner.simulate();
			JSONObject resultsJson = results.getEvents().toJSONObject();
			resultsJson.element("network", net.toJSONObject());
			resultsJson.element("length", length);
			resultsJson.element("nodeSimTimes", results.getJSONTimeMap());

			BufferedWriter writer = new BufferedWriter(new FileWriter(output));
			resultsJson.write(writer);
			writer.flush();
			writer.close();
			completed = true;

			model.resetException();

		} catch (IOException e) {
			model.setSimulationException(e);
		} catch (InstantiationException e) {
			model.setSimulationException(e);
		} catch (IllegalAccessException e) {
			model.setSimulationException(e);
		} catch (LimitationFailureException e) {
			model.setSimulationException(e);
		} finally {
			if (!completed && isNewFile)
				if (!output.delete())
					throw new IllegalStateException(
							"Attempted to delete file incorrectly identified as created.");
		}
	}

	/**
	 * Simulates user code under specified network conditions for a set number
	 * of timesteps.
	 * 
	 * The given user code is first validated to ensure that it does not include
	 * code that could be used to bypass the message passing system used in
	 * simulation.
	 * 
	 * The results of this simulation will be stored in the ApplicationState
	 * list of recent simulation results as a JSONObject. This JSONObject
	 * contains a NetworkConfiguration, and EventLog, under the tags 'network',
	 * 'events' and 'messages'. Should the list of recent simulation results be
	 * at capacity, the least recent simulation result will be discarded.
	 * 
	 * The simulation results will also be written to the file
	 * <code>output</code> in the same JSON format.
	 * 
	 * As a result of the simulation, the <code>net</code> will also be stored
	 * in the <code>ApplicationModel</code> of the controller as recently used.
	 * 
	 * @param netFile
	 *            the network configuration file from which to take the network,
	 * @param length
	 *            the number of timesteps that the simulation will execute for.
	 * @param userCode
	 *            the user-defined Node that is replicated and simulated across
	 *            the network.
	 * @param output
	 *            the destination file that a copy of the simulation results
	 *            will be written to.
	 * @param limitation
	 *            the static limitations placed on the user code.
	 * @param timeout
	 *            the time a node can execute for in one timestep before being
	 *            timed out.
	 * @param scrambleIds
	 *            if the machine IDs seen by the user code are to be scrambled.
	 */
	@SuppressWarnings("unchecked")
	public void simulate(File netFile, long length, File userCode,
			ClassLimitation limitation, File output, long timeout,
			boolean scrambleIds) {
		try {

			// Construct the user class.
			Class<?> userClass = dynamicClassLoader.loadClass(userCode);
			Class<? extends NodeScript> userNodeClass = (Class<? extends NodeScript>) userClass;

			// Construct the network configuration.
			NetworkConfig net = new NetworkConfig(netFile);

			simulate(net, length, userNodeClass, limitation, output, timeout,
					scrambleIds);
		} catch (IOException e) {
			model.setSimulationException(e);
		} catch (ClassNotFoundException e) {
			model.setSimulationException(e);
		} catch (NetworkConfigException e) {
			model.setSimulationException(e);
		} catch (ClassLoaderException e) {
			model.setSimulationException(e);
		} catch (JSONException e) {
			model.setSimulationException(e);
		}

	}

	/**
	 * Marks the occurrence of an error in the Model.
	 * 
	 * @param e1
	 *            the error that has occurred.
	 */
	public void noteError(Exception e1) {
		model.setSimulationException(e1);
	}
}
