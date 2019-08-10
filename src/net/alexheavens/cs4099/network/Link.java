package net.alexheavens.cs4099.network;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * A <code>Link</code> marks a two-way communication band between two
 * <code>INodes</code>. This includes queues of arrived <code>IMessages</code>
 * in each direction.
 * 
 * @author Alexander Heavens
 * @version 1.0
 */
public class Link implements ILinkImpl {

	private final INode source;
	private final INode target;
	private final long latency;
	private final PriorityBlockingQueue<IMessageImpl<?>> sourceMessages;
	private final PriorityBlockingQueue<IMessageImpl<?>> targetMessages;

	/**
	 * Creates a <code>Link</code> between the nodes <code>s</code> and
	 * <code>t</code>.
	 * 
	 * @param s
	 *            the <code>INode</code> from which the <code>Link</code>.
	 * @param t
	 *            the destination <code>INode</code> of the <code>Link</code>.
	 * @param l
	 *            the latency of the link.
	 */
	public Link(INode s, INode t, long l) {
		if (s == null)
			throw new IllegalArgumentException("Null link source.");
		if (t == null)
			throw new IllegalArgumentException("Null link target.");
		if (l < MIN_LATENCY)
			throw new IllegalArgumentException(
					"Link latency less than minimum.");

		source = s;
		target = t;
		latency = l;
		sourceMessages = new PriorityBlockingQueue<IMessageImpl<?>>();
		targetMessages = new PriorityBlockingQueue<IMessageImpl<?>>();
	}

	/**
	 * Creates a unit latency link between two nodes.
	 * 
	 * @param s
	 *            the source node.
	 * @param t
	 *            the target node.
	 */
	public Link(INode s, INode t) {
		this(s, t, MIN_LATENCY);
	}

	public INode getSource() {
		return source;
	}

	public INode getTarget() {
		return target;
	}

	public INode opposite(INode n) {
		if (n == source) {
			return target;
		} else if (n == target) {
			return source;
		} else {
			throw new IllegalArgumentException(
					"Passed node not source or target.");
		}
	}

	public long latency() {
		return latency;
	}

	public void queueMessage(IMessageImpl<?> message) {
		INodeImpl messageTarget = (INodeImpl) message.target();
		INode messageSource = message.source();
		if (messageSource != target && messageSource != source)
			throw new IllegalArgumentException(
					"Attempted to queue message from non-link node.");

		if (messageTarget == source)
			sourceMessages.add(message);
		else if (messageTarget == target)
			targetMessages.add(message);
		else
			throw new IllegalArgumentException(
					"Attempted to queue message to non-link node.");
	}

	public IMessageImpl<?> popMessage(INodeImpl messageTarget) {
		if (messageTarget == source)
			return sourceMessages.poll();
		else if (messageTarget == target)
			return targetMessages.poll();
		else
			throw new IllegalArgumentException(
					"Attempted to pop message for non-link node.");
	}

	public int messageCount(INode targetNode) {
		if (targetNode != source && targetNode != target)
			throw new IllegalArgumentException(
					"Message count for non-link node.");
		if (targetNode == source)
			return sourceMessages.size();
		else if (targetNode == target)
			return targetMessages.size();
		else
			return -1;
	}

	public void removeMessage(IMessageImpl<?> returnMessage) {
		final INode recipient = returnMessage.target();
		if (recipient == source) {
			sourceMessages.remove(returnMessage);
		} else if (recipient == target) {
			targetMessages.remove(returnMessage);
		}

	}

}
