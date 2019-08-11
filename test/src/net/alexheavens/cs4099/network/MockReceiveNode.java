package net.alexheavens.cs4099.network;

public class MockReceiveNode extends MockUserNode {

	protected IMessageImpl<?> recMessage = null;
	protected int nMessagesRead = 1;
	protected INode recNode = null;

	public MockReceiveNode(int id) {
		super(id);
	}

	public void setRecNode(INode node) {
		recNode = node;
	}

	public void setNumMsgsRead(int num) {
		nMessagesRead = num;
	}

	public void execute() {
		for (int i = 0; i < nMessagesRead; i++) {
			if (recNode != null)
				recMessage = receive(recNode);
			else
				recMessage = receive();
		}
	}

	public IMessageImpl<?> recMessage() {
		return recMessage;
	}

}
