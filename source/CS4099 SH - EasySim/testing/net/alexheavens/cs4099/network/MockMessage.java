package net.alexheavens.cs4099.network;

public class MockMessage extends MessageImpl<Object> {

	public MockMessage(INodeImpl source, INodeImpl target, long sentAt) {
		super(new Object());
		sourceNode = source;
		targetNode = target;
		link = (ILinkImpl)sourceNode.neighbourLink(target);
		marked = true;
		timestepSent = sentAt;
	}

}
