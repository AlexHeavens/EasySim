import net.alexheavens.cs4099.usercode.NodeScript;

public class FailureNode extends NodeScript {

	int[] neighbourCounts;

	public void setup() {

	}

	public void execute() {

		// Error here! Will only count to 100 timesteps.
		neighbourCounts = new int[100];
		int time = 0;

		while (true) {
			pause(1);
			neighbourCounts[time++] = this.neighbourCount();
		}

	}

}
