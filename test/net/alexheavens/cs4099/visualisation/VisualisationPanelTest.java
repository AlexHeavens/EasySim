package net.alexheavens.cs4099.visualisation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;

import net.alexheavens.cs4099.network.configuration.NetworkConfigException;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class VisualisationPanelTest extends JFrame {

	private static final long serialVersionUID = 1L;

	public VisualisationPanelTest() throws IOException, JSONException,
			NetworkConfigException, VisualisationException {
		super();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		File resultsFile = new File(
				"test/testoutputfiles/treeleaderexample.json");

		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new FileReader(resultsFile));

		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();

		JSONObject results = JSONObject.fromObject(sb.toString());
		VisualisationPanel visPanel;
		visPanel = new VisualisationPanel(results);
		add(visPanel);
		pack();
	}

	public static void main(String[] args) throws JSONException, IOException,
			NetworkConfigException, VisualisationException {
		VisualisationPanelTest appFrame = new VisualisationPanelTest();
		appFrame.setVisible(true);
	}

}
