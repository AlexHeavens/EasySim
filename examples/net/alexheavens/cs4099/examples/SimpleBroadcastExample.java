package net.alexheavens.cs4099.examples;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.alexheavens.cs4099.network.Network;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.INetworkConfigFactory;
import net.alexheavens.cs4099.network.configuration.NetworkConfigFactory;
import net.alexheavens.cs4099.simulation.SimulationRunner;
import net.sf.json.JSONObject;

public class SimpleBroadcastExample {

	public static void main(String[] arStrings) {
		INetworkConfigFactory configFactory = new NetworkConfigFactory();
		INetworkConfig netConfig = configFactory.createTreeNetwork(5, 5);
		Network network = null;
		try {
			network = new Network(SimpleBroadcastNode.class, netConfig, false);
			SimulationRunner sim = new SimulationRunner(network, 1000);
			JSONObject obj = sim.simulate().getEvents().toJSONObject();
			FileWriter fw = new FileWriter(new File(
					"examples/simplebroadcast.json"));
			BufferedWriter writer = new BufferedWriter(fw);
			obj.write(writer);
			writer.close();
		} catch (InstantiationException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
