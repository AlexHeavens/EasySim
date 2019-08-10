import java.awt.Color;

import net.alexheavens.cs4099.network.NumericMessage;
import net.alexheavens.cs4099.usercode.NodeScript;

public class RingLeader extends NodeScript {

	private int successor = -1;
	private int predecessor = -1;
	private int leader = -1;

	public void setup() {
		leader = machineId();
	}

	public void execute() {

		// One node begins the chain to decide successors.
		if (isInitiator()) {
			successor = 0;
			
			// We have forgotten to set the predecessor!
			
			send(successor, new NumericMessage("PREDECESSOR", machineId()));
			receive();
			send(successor, new NumericMessage("CANDIDATE", machineId()));
			setColour(Color.GREEN);
		} else {

			NumericMessage predMessage = receive();
			predecessor = predMessage.sourceIndex();
			successor = (predecessor == 0) ? 1 : 0;
			send(successor, new NumericMessage("PREDECESSOR", machineId()));

			// Yellow indicates that node knows its successor.
			setColour(Color.YELLOW);
		}

		while (true) {
			
			// The initiator will try to receive from an invalid index!
			NumericMessage message = receive(predecessor);
			
			if (message.getTag().equals("CANDIDATE")) {
				int candidate = message.getData().intValue();
				if (candidate == leader) {
					send(successor, new NumericMessage("LEADER", leader));
					setColour(Color.BLUE);

				} else {
					leader = (candidate > leader) ? candidate : leader;
					send(successor, new NumericMessage("CANDIDATE", leader));

					// Green indicates that a node has reached the candidate
					// stage.
					setColour(Color.GREEN);
					break;
				}
			} else if (message.getTag().equals("LEAD")) {
				leader = message.getData().intValue();
				send(successor, new NumericMessage("LEADER", leader));

				// Blue indicates that a node has received the leader.
				setColour(Color.BLUE);
				break;
			}
		}
	}
}
