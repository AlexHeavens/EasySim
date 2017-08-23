import java.awt.Color;
import java.util.HashMap;
import net.alexheavens.cs4099.network.IMessage;
import net.alexheavens.cs4099.network.NumericMessage;
import net.alexheavens.cs4099.usercode.NodeScript;

public class TreeLeaderScript extends NodeScript {

	public void execute() {
		HashMap<Integer, Integer> neighboursIds = new HashMap<Integer, Integer>(
				neighbourCount());
       
        // Collect the largest candidate from all neighbours.
		while (neighboursIds.size() < neighbourCount() - 1) {
			NumericMessage candidateMessage = receive();
			neighboursIds.put(candidateMessage.sourceIndex(), candidateMessage
					.getData().intValue());
		}

        // Figure out the largest candidate and the parent neighbour.
		int leader = machineId();
		int parent = -1;
		for (int i = 0; i < neighbourCount(); i++) {
			if (neighboursIds.containsKey(i)) {
				int neighbourLeader = neighboursIds.get(i);
				if (neighbourLeader > leader) {
					leader = neighbourLeader;
				}
			} else {
				parent = i;
            }
		}

        // Send the parent the best candidate.
		NumericMessage candidateMessage = new NumericMessage("CANDIDATE",
				leader);
		send(parent, candidateMessage);
		
        setColour(Color.CYAN);
        pause(1);

        // Wait for the leader message.
		NumericMessage leaderMessage = receive(parent);
		if (leaderMessage.getTag().equals("CANDIDATE")) {
			if (leaderMessage.getData().intValue() > leader)
				leader = leaderMessage.getData().intValue();
			leaderMessage = new NumericMessage("LEADER", leader);
		}

		setColour(Color.green);

        // Send the leader to all neighbours except the parent.
		leader = leaderMessage.getData().intValue();
		sendAllExcept(parent, leaderMessage);
	}

	private void sendAllExcept(int exception, IMessage<?> message) {
		for (int i = 0; i < neighbourCount(); i++) {
			if (i != exception)
				send(i, message);
		}
	}
}
