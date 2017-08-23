import java.awt.Color;
import java.util.HashMap;
import net.alexheavens.cs4099.network.IMessage;
import net.alexheavens.cs4099.network.NumericMessage;
import net.alexheavens.cs4099.usercode.NodeScript;

public class NoPackageTreeLeaderNode extends NodeScript {

	public void setup() {
	}

	public void execute() {
		HashMap<Integer, Integer> neighboursIds = new HashMap<Integer, Integer>(
				neighbourCount());

		int neighboursReplied = 0;
		while (neighboursReplied < neighbourCount() - 1) {
			NumericMessage candidateMessage = receive();
			neighboursIds.put(candidateMessage.sourceIndex(), candidateMessage
					.getData().intValue());
			neighboursReplied++;
		}

		setColour(Color.red);
		pause(10);

		int leader = machineId();
		int parent = -1;
		for (int i = 0; i < neighbourCount(); i++) {
			if (neighboursIds.containsKey(i)) {
				int neighbourLeader = neighboursIds.get(i);
				if (machineId() == 0)
					System.out.println("0 possible leader " + neighbourLeader);
				if (neighbourLeader > leader) {
					leader = neighbourLeader;
				}
			} else {
				parent = i;
				if (machineId() == 0)
					System.out.println("0 parent " + parent);
			}
		}
		if (machineId() == 0)
			System.out.println("0 leader " + leader);

		NumericMessage candidateMessage = new NumericMessage("CANDIDATE",
				leader);

		setColour(Color.blue);
		send(parent, candidateMessage);

		NumericMessage leaderMessage = receive(parent);
		if (leaderMessage.getTag().equals("CANDIDATE")) {
			if (leaderMessage.getData().intValue() > leader)
				leader = leaderMessage.getData().intValue();
			leaderMessage = new NumericMessage("LEADER", leader);
		}

		setColour(Color.green);
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
