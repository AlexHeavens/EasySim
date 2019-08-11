package net.alexheavens.cs4099.examples;

import net.alexheavens.cs4099.network.IMessage;
import net.alexheavens.cs4099.network.NumericMessage;
import net.alexheavens.cs4099.usercode.NodeScript;

import java.awt.*;
import java.util.HashMap;

public class TreeLeaderNode extends NodeScript {

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

		NumericMessage candidateMessage = new NumericMessage("CANDIDATE",
				leader);
		send(parent, candidateMessage);
		
		setColour(Color.GREEN);

		NumericMessage leaderMessage = receive(parent);
		if (leaderMessage.getTag().equals("CANDIDATE")) {
			if (leaderMessage.getData().intValue() > leader)
				leader = leaderMessage.getData().intValue();
			leaderMessage = new NumericMessage("LEADER", leader);
		}

		leader = leaderMessage.getData().intValue();
		sendAllExcept(parent, leaderMessage);
		
		if(leader == machineId()) setColour(Color.RED);
		else setColour(Color.GRAY);
	}

	private void sendAllExcept(int exception, IMessage<?> message) {
		for (int i = 0; i < neighbourCount(); i++) {
			if (i != exception)
				send(i, message);
		}
	}
}
