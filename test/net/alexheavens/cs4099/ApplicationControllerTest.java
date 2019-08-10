package net.alexheavens.cs4099;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import net.alexheavens.cs4099.examples.TreeLeaderNode;
import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfigFactory;
import net.alexheavens.cs4099.usercode.ClassLimitation;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ApplicationControllerTest {

	private static final int SIM_LENGTH = 15;
	private INetworkConfig testNet;
	private ApplicationController testController;
	private File testOutputFile;
	private ClassLimitation testLimit;

	@Before
	public void setup() {
		testController = new ApplicationController();
		NetworkConfigFactory netFact = new NetworkConfigFactory();
		testNet = netFact.createTreeNetwork(6, 3);
		testOutputFile = new File("test/testoutputfiles/testsimresults.json");
		testOutputFile.delete();
		testLimit = new ClassLimitation(true, true);
	}

	@After
	public void breakdown() {
		//testOutputFile.delete();
	}

	@Test
	/**
	 * Tests that simulation meets the criteria specified in the JavaDoc of
	 * <code>ApplicationController</code>
	 */
	public void testSimulate() throws IOException {

		testController.simulate(testNet, SIM_LENGTH, TreeLeaderNode.class,
				testLimit, testOutputFile, 0, false);

		JSONObject expectedNetJson = testNet.toJSONObject();
		
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new FileReader(testOutputFile));
		String line;
		while((line = br.readLine()) != null){
			sb.append(line + "\n");
		}
		
		JSONObject simResults = (JSONObject) JSONSerializer.toJSON(sb.toString());		
		JSONObject storedNet = simResults.getJSONObject("network");
		assertEquals(expectedNetJson, storedNet);
		assertEquals(SIM_LENGTH, simResults.getLong("length"));
	}

	@Test
	/**
	 * Tests that a null <code>NetworkConfig</code> is not a valid parameter,
	 * and that no side effects can occur in this case.
	 */
	public void testSimulateNullNet() {
		try {
			testController.simulate(null, SIM_LENGTH, TreeLeaderNode.class,
					testLimit, testOutputFile, 0, false);
			fail("Able to simulate.");
		} catch (NullPointerException e) {
		} catch (Exception e) {
			fail("Exception: " + e.getLocalizedMessage());
		}

		testNoChangeInModel();
	}

	private void testNoChangeInModel() {
		assertFalse(testOutputFile.exists());
	}

}
