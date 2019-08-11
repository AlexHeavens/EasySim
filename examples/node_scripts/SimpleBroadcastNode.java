import net.alexheavens.cs4099.network.StringMessage;
import net.alexheavens.cs4099.usercode.NodeScript;

public class SimpleBroadcastNode extends NodeScript {

	public boolean visited = false;
	
	public void setup() {

	}

	public void execute() {

		if (machineId() == 0) {
			StringMessage message = new StringMessage("HELLO");
			sendAll(message);
			visited = true;
		}

		while (!visited) {
			StringMessage message = receive();
			sendAll(message);
			visited = true;
		}

	}
}