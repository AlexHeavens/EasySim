package net.alexheavens.cs4099.network;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import net.alexheavens.cs4099.concurrent.WaitRegistrar;
import net.alexheavens.cs4099.simulation.ColourChangeEvent;
import net.alexheavens.cs4099.simulation.ISimulationEvent;
import net.alexheavens.cs4099.simulation.InvalidCallException;
import net.alexheavens.cs4099.simulation.MessageReadEvent;
import net.alexheavens.cs4099.simulation.MessageSentEvent;
import net.alexheavens.cs4099.simulation.NodeFailureEvent;
import net.alexheavens.cs4099.simulation.NodeHaltedException;
import net.alexheavens.cs4099.simulation.NodePauseEvent;
import net.alexheavens.cs4099.simulation.NodeReceiveBlockEvent;
import net.alexheavens.cs4099.simulation.NodeReceiveBlockResumeEvent;
import net.alexheavens.cs4099.simulation.SimulationProfiler;
import net.alexheavens.cs4099.simulation.SimulationRuntimeException;
import net.alexheavens.cs4099.simulation.SimulationState;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * The underlying Node structure that acts on behalf of a user script.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class Node extends Observable implements INodeImpl, Runnable {

	private ReentrantLock lock;
	private Condition blocked, completed;
	protected HashMap<INode, ILinkImpl> links;
	private ArrayList<INode> neighbours;
	private HashMap<INode, Integer> neighbourIds;
	protected PriorityBlockingQueue<IMessageImpl<?>> messages;
	protected INode expectedSender;
	protected int machineId = INIT_MACHINE_ID;
	protected volatile SimulationState simState;
	protected Thread nodeThread;
	protected WaitRegistrar waitRegistrar;
	private final NodeScript script;
	protected SimulationProfiler profiler;
	private final boolean isInitiator;
	private int scrambleCode;

	/**
	 * Creates a new node for a given script.
	 * 
	 * @param script
	 *            the script executed by the node.
	 */
	public Node(NodeScript script) {
		this(script, false);
	}

	/**
	 * Creates a new node for a given script that may be an initiator.
	 * 
	 * @param script
	 *            the script executed by the node.
	 * @param isInitiator
	 *            whether the node is an initiator.
	 */
	public Node(NodeScript script, boolean isInitiator) {
		super();
		this.script = script;
		this.isInitiator = isInitiator;
		script.setNode(this);
		simState = SimulationState.PRE_SIMULATION;
		neighbours = new ArrayList<INode>();
		neighbourIds = new HashMap<INode, Integer>();
		links = new HashMap<INode, ILinkImpl>();
		messages = new PriorityBlockingQueue<IMessageImpl<?>>();
		nodeThread = null;
		lock = new ReentrantLock();
		blocked = lock.newCondition();
		completed = lock.newCondition();
	}

	@Override
	public ILinkImpl addNeighbour(INodeImpl n, long latency) {
		if (n == null)
			throw new IllegalArgumentException(NULL_NEIGHBOUR_MSG);
		if (n.getSimulationId() == INIT_MACHINE_ID)
			throw new IllegalArgumentException(UNINIT_NEIGHBOUR_MSG);
		if (isNeighbour(n))
			throw new IllegalArgumentException(DUP_NEIGHBOUR_MSG);

		if (n.isNeighbour(this)) {
			links.put(n, (ILinkImpl) n.neighbourLink(this));
			neighbourIds.put(n, neighbours.size());
			neighbours.add(n);
		} else {
			ILinkImpl link = new Link(this, n, latency);
			links.put(n, link);
			neighbourIds.put(n, neighbours.size());
			neighbours.add(n);
			n.addNeighbour(this, latency);
		}

		return links.get(n);
	}

	@Override
	public Iterator<INode> neighbours() {
		return links.keySet().iterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <MsgType extends IMessage<?>> MsgType receive() {

		if (getSimulationState() == SimulationState.SETUP)
			throw new InvalidCallException("Receive call in setup method.");

		if (neighbourCount() == 0)
			return null;

		// Receive block if no message.
		while (messages.size() == 0) {
			lock.lock();
			try {
				// In the odd case that a message arrives between the message
				// check and locking.
				if (messages.size() > 0)
					break;

				setSimulationState(SimulationState.RECEIVE_BLOCK);
				NodeReceiveBlockEvent blockEvent = new NodeReceiveBlockEvent(
						ISimulationEvent.CURRENT_TIMESTEP, this);
				raiseEvent(blockEvent);
			} finally {
				lock.unlock();
			}
			try {
				waitRegistrar.waitThread();
			} catch (InterruptedException e) {
				throw new NodeHaltedException();
			}

			NodeReceiveBlockResumeEvent unblockEvent = new NodeReceiveBlockResumeEvent(
					ISimulationEvent.CURRENT_TIMESTEP, this);
			raiseEvent(unblockEvent);
		}

		// Remove the message from the top of the general message pile.
		lock.lock();
		try {
			IMessageImpl<?> returnMessage = messages.poll();
			returnMessage.link().removeMessage(returnMessage);
			MessageReadEvent readEvent = new MessageReadEvent(returnMessage,
					ISimulationEvent.CURRENT_TIMESTEP);
			raiseEvent(readEvent);
			return (MsgType) returnMessage;
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public <MsgType extends IMessage<?>> MsgType receive(INode neighbour) {

		if (getSimulationState() == SimulationState.SETUP)
			throw new InvalidCallException("Receive call in setup method.");

		// Receive block if no message.
		while (messages.size() == 0) {
			lock.lock();

			try {
				// In the odd case that a message arrives between the message
				// check and locking.
				if (messages.size() > 0)
					break;

				NodeReceiveBlockEvent blockEvent = new NodeReceiveBlockEvent(
						ISimulationEvent.CURRENT_TIMESTEP, this);
				expectedSender = neighbour;
				raiseEvent(blockEvent);
				setSimulationState(SimulationState.RECEIVE_BLOCK);
			} finally {
				lock.unlock();
			}
			try {
				waitRegistrar.waitThread();
			} catch (InterruptedException e) {
				throw new NodeHaltedException();
			}

			NodeReceiveBlockResumeEvent unblockEvent = new NodeReceiveBlockResumeEvent(
					ISimulationEvent.CURRENT_TIMESTEP, this);
			raiseEvent(unblockEvent);
		}

		// Remove the message from the top of the general message pile.
		lock.lock();
		try {
			IMessageImpl<?> returnMessage = links.get(neighbour).popMessage(
					this);
			messages.remove(returnMessage);
			MessageReadEvent readEvent = new MessageReadEvent(returnMessage,
					ISimulationEvent.CURRENT_TIMESTEP);
			raiseEvent(readEvent);
			return (MsgType) returnMessage;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void send(INode recipient, IMessage<?> message) {

		if (getSimulationState() == SimulationState.SETUP)
			throw new InvalidCallException("Called send from setup.");

		if (recipient == null)
			throw new NullPointerException(
					"Attempted to send to null recipient");

		if (message == null)
			throw new NullPointerException("Attempted to send null message.");

		try {

			lock.lock();
			try {
				IMessageImpl<?> newMessage = ((IMessageImpl<?>) message)
						.clone();
				newMessage.attachSendData(this, (INodeImpl) recipient);
				MessageSentEvent sentEvent = new MessageSentEvent(newMessage,
						ISimulationEvent.CURRENT_TIMESTEP);
				raiseEvent(sentEvent);
			} finally {
				lock.unlock();
			}

		} catch (RuntimeException e) {
			throw new SimulationRuntimeException(e);
		}
	}

	@Override
	public void sendAll(IMessage<?> message) {
		for (ILinkImpl link : links.values()) {
			send(link.opposite(this), message);
		}
	}

	@Override
	public int neighbourCount() {
		return links.size();
	}

	@Override
	public ILink neighbourLink(INode n) {
		return links.get(n);
	}

	@Override
	public boolean isNeighbour(INode n) {
		return links.containsKey(n);
	}

	@Override
	public void setSimulationId(int id, int scrambleCode) {
		if (machineId != INIT_MACHINE_ID)
			throw new IllegalStateException(ID_SET_TWICE_MSG);
		if (id < 0)
			throw new IllegalArgumentException(INVALID_ID_MSG);

		this.machineId = id;
		this.scrambleCode = scrambleCode;
	}

	@Override
	public int getSimulationId() {
		return machineId;
	}

	@Override
	public int getMachineId() {
		return machineId + scrambleCode;
	}

	@Override
	public void simulate(WaitRegistrar registrar,
			SimulationProfiler testProfiler) {
		lock.lock();
		try {
			this.waitRegistrar = registrar;
			this.profiler = testProfiler;
			nodeThread = new Thread(this);
			nodeThread.start();
		} finally {
			lock.unlock();
		}
		while (getSimulationState() == SimulationState.PRE_SIMULATION) {
		}
	}

	@Override
	public void run() {

		try {
			setSimulationState(SimulationState.SETUP);
			setup();
			setSimulationState(SimulationState.SIMULATING);

			execute();
			setSimulationState(SimulationState.COMPLETED);
			try {
				waitRegistrar.waitThread();
			} catch (InterruptedException e) {
				throw new NodeHaltedException();
			}
		} catch (SimulationRuntimeException e) {

			// Unpack non-user code RuntimeExceptions, rethrow.
			throw e.getException();
		} catch (NodeHaltedException e) {

			// Allow halted nodes to end simulation.

		} catch (RuntimeException e) {

			// User created RuntimeExceptions cause a failure event.
			NodeFailureEvent failureEvent = new NodeFailureEvent(
					ISimulationEvent.CURRENT_TIMESTEP, this, e);

			lock.lock();
			try {
				raiseEvent(failureEvent);
				setSimulationState(SimulationState.NODE_ERROR);
			} finally {
				lock.unlock();
			}

			try {
				waitRegistrar.waitThread();
			} catch (InterruptedException e1) {
			}
		} finally {
			setSimulationState(SimulationState.POST_SIMULATION);
		}

	}

	@Override
	public void halt() {

		lock.lock();
		try {
			final SimulationState state = getSimulationState();
			if (state == SimulationState.PRE_SIMULATION)
				throw new IllegalStateException(
						"Attempted to halt non-executing node.");

			if (state == SimulationState.POST_SIMULATION)
				return;

			setSimulationState(SimulationState.HALTED);
			nodeThread.interrupt();
			while (simState != SimulationState.POST_SIMULATION)
				completed.await();

		} catch (InterruptedException e) {
			throw new IllegalStateException();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public SimulationState getSimulationState() {
		lock.lock();
		try {
			return simState;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void raiseEvent(ISimulationEvent event) {
		if (event == null)
			throw new IllegalArgumentException(
					"Raised null ISimulationEvent from Node.");
		setChanged();
		notifyObservers(event);
	}

	@Override
	public void pause(long pauseSteps) {

		if (getSimulationState() == SimulationState.SETUP)
			throw new InvalidCallException("Called pause from setup.");

		try {
			NodePauseEvent pauseEvent = new NodePauseEvent(pauseSteps, this);
			lock.lock();
			try {

				if (getSimulationState() != SimulationState.SIMULATING)
					throw new IllegalStateException(
							"Paused node outside of simulation, in state "
									+ simState + ".");

				raiseEvent(pauseEvent);
				setSimulationState(SimulationState.PAUSED);
			} finally {
				lock.unlock();
			}
			try {
				waitRegistrar.waitThread();
			} catch (InterruptedException e) {
				throw new NodeHaltedException();
			}
		} catch (NodeHaltedException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new SimulationRuntimeException(e);
		}
	}

	@Override
	public void unpause() {
		try {
			lock.lock();
			try {

				if (getSimulationState() != SimulationState.PAUSED)
					throw new IllegalStateException("Unpaused non-paused Node.");

				setSimulationState(SimulationState.SIMULATING);
				waitRegistrar.notifyThread(nodeThread);
			} finally {
				lock.unlock();
			}
		} catch (RuntimeException e) {
			throw new SimulationRuntimeException(e);
		}
	}

	@Override
	public void queueMessage(IMessageImpl<?> message) {
		if (message.getSentAt() == IMessageImpl.TIMESTEP_NOT_SENT)
			throw new IllegalArgumentException("Queued unsent message at Node.");

		try {
			lock.lock();
			try {
				messages.add(message);
				links.get(message.source()).queueMessage(message);
				final boolean isExpected = expectedSender == null
						|| message.source() == expectedSender;
				if (getSimulationState() == SimulationState.RECEIVE_BLOCK
						&& isExpected) {
					expectedSender = null;
					setSimulationState(SimulationState.SIMULATING);
					waitRegistrar.notifyThread(nodeThread);
				}
			} finally {
				lock.unlock();
			}
		} catch (RuntimeException e) {
			throw new SimulationRuntimeException(e);
		}
	}

	@Override
	public Thread getThread() {
		return nodeThread;
	}

	/**
	 * @param state
	 *            the new simulation state of the node.
	 */
	public void setSimulationState(SimulationState state) {
		lock.lock();
		try {
			simState = state;
			switch (state) {
			case PAUSED:
			case RECEIVE_BLOCK:
			case COMPLETED:
				blocked.signalAll();
				break;
			case POST_SIMULATION:
				completed.signalAll();
				break;
			default:
				break;
			}

			if (state == SimulationState.SIMULATING)
				profiler.trackProcessSimulation(this);
			else if (state != SimulationState.SETUP
					&& state != SimulationState.POST_SIMULATION
					&& state != SimulationState.HALTED
					&& state != SimulationState.NODE_ERROR)
				profiler.untrackNodeSimulation(this);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public INode getNeighbour(int index) {
		if (index < 0 || index >= neighbourCount())
			throw new IndexOutOfBoundsException();

		return neighbours.get(index);
	}

	@Override
	public void setup() {
		script.setup();
	}

	@Override
	public void execute() {
		script.execute();
	}

	public NodeScript getScript() {
		return script;
	}

	@Override
	public int getIndex(INode neighbour) {
		if (!isNeighbour(neighbour))
			throw new IllegalArgumentException("Non neighbour index request.");
		return neighbourIds.get(neighbour);
	}

	@Override
	public void setColour(Color colour) {
		if (getSimulationState() == SimulationState.SETUP)
			throw new InvalidCallException("Called setColour from setup.");

		if (colour == null)
			throw new IllegalArgumentException(
					"Cannot set Node colour to null.");
		ColourChangeEvent event = new ColourChangeEvent(colour, this,
				ISimulationEvent.CURRENT_TIMESTEP);

		try {
			lock.lock();
			try {
				raiseEvent(event);
			} finally {
				lock.unlock();
			}
		} catch (RuntimeException e) {
			throw new SimulationRuntimeException(e);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void kill() {

		NodeFailureEvent failureEvent = new NodeFailureEvent(
				ISimulationEvent.CURRENT_TIMESTEP, this,
				new IllegalStateException("Node timeout."));
		lock.lock();
		try {
			raiseEvent(failureEvent);
			setSimulationState(SimulationState.TIMEOUT);
			new Thread() {
				public void run() {
					try {
						waitRegistrar.waitThread();
					} catch (InterruptedException e) {
					}
				}
			}.start();
			synchronized (nodeThread) {
				nodeThread.stop();
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isInitiator() {
		return isInitiator;
	}

}
