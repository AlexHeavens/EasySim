import java.util.Random;

import net.alexheavens.cs4099.usercode.NodeScript;

public class LoopScript extends NodeScript {

	private Random randomGenerator = new Random();

	@Override
	public void execute() {

		// On average, a tenth of all nodes will begin looping indefinitely each
		// timestep.
		while (true) {
			if (randomGenerator.nextInt() % 10 == 0) {
				while (true) {
				}
			} else {
				pause(1);
			}
		}
	}

}
