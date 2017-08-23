package net.alexheavens.cs4099.visualisation;

import java.util.Timer;
import javax.swing.event.EventListenerList;

/**
 * This class provides a convenient means to tracking the timestep and
 * sub-timestep ("tick") of a simulation visualisation.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class VisualisationClock {

	public enum State {
		PLAYING, PAUSED, STOPPED, ENDED
	};

	public static final int DEFAULT_MILLIS_PER_TIMESTEP = 1000;
	public static final int MAX_MILLIS_PER_TIMESTEP = 5000;
	public static final int TICKS_PER_SECOND = 60;

	private long length;
	private volatile int millisPerTimestep;
	protected volatile int ticksPerTimestep;
	protected volatile long timestep;
	protected volatile int tick;
	protected volatile State state;
	private final EventListenerList listeners;
	private Timer timestepTimer;
	private ClockTickTask incTickTask;
	protected volatile int changeCheck;

	/**
	 * @param length
	 *            length of the simulation in timesteps.
	 */
	public VisualisationClock(long length) {
		if (length < 1)
			throw new IllegalArgumentException(
					"Created VisualisationClock with invalid length.");
		this.length = length;
		listeners = new EventListenerList();
		timestep = 0;
		tick = 0;
		state = State.STOPPED;
		changeCheck = 0;
		setMillisPerTimestep(DEFAULT_MILLIS_PER_TIMESTEP);
	}

	/**
	 * Sets the rate at which the clock changes. The number of ticks per
	 * timestep adapts to ensure smooth transition of messages in visualisation.
	 * 
	 * @param millis
	 *            the number of milliseconds taken for the visualisation to
	 *            advance one timestep of simulation. If negative, the clock
	 *            will travel backwards through time.
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>millis</code> is less than 1.
	 */
	public void setMillisPerTimestep(int millis) {
		synchronized (this) {
			if (millis == 0)
				throw new IllegalArgumentException(
						"Attempted to set clock to advance at 0 milliseconds per timestep.");
			changeCheck++;
			final boolean playing = state == State.PLAYING;
			if (playing) {
				pause();
			}
			millisPerTimestep = millis;
			ticksPerTimestep = TICKS_PER_SECOND * Math.abs(millis) / 1000;
			if (ticksPerTimestep == 0)
				ticksPerTimestep++;
			tick = (millis < 0 && timestep > 0) ? ticksPerTimestep - 1 : 0;
			if (playing)
				play();
		}
	}

	/**
	 * @return The time taken for the clock to advance one timestep whilst
	 *         playing.
	 */
	public int getMillisPerTimestep() {
		return millisPerTimestep;
	}

	/**
	 * Resumes the advancement of the clock at the rate returned by
	 * <code>getMillisPerTimestep()</code>.
	 */
	public void play() {
		synchronized (this) {
			if (state == State.PLAYING)
				throw new IllegalStateException(
						"Called play whilst already playing.");
			final boolean atStart = millisPerTimestep < 0 && timestep == 0
					&& tick == 0;
			final boolean atEnd = millisPerTimestep > 0
					&& ticksPerTimestep == length;
			if (atStart || atEnd)
				return;

			if (state == State.ENDED && millisPerTimestep > 0) {
				stop();
			}

			ClockSpeedEvent event = new ClockSpeedEvent(this, ticksPerTimestep);
			for (ClockListener listener : listeners
					.getListeners(ClockListener.class)) {
				listener.onSpeedChange(event);
			}

			setState(State.PLAYING);
			final int timeBetweenTicks = 1000 / TICKS_PER_SECOND;
			timestepTimer = new Timer();
			incTickTask = new ClockTickTask(this);
			if (timestep == 0 && tick == 0) {
				setTimestep(0);
				broadcastTick();
			} else if (timestep == length) {
				setTimestep(length - 1);
				broadcastTick();
			}
			timestepTimer.scheduleAtFixedRate(incTickTask, 0, timeBetweenTicks);
		}
	}

	/**
	 * Pauses the advancement of the clock.
	 * 
	 * @return the timestep that the clock has paused at.
	 */
	public long pause() {
		synchronized (this) {
			if (state != State.PLAYING)
				throw new IllegalStateException("Paused clock outside of play.");
			timestepTimer.cancel();
			setState(State.PAUSED);
			return timestep;
		}
	}

	/**
	 * @return The timestep of the visualisation,
	 */
	public long getTimestep() {
		return timestep;
	}

	/**
	 * @return The tick of the visualisation.
	 */
	public int getTick() {
		return tick;
	}

	private void setTimestep(long timestep) {
		synchronized (this) {
			this.timestep = timestep;
			ClockTimestepEvent event = new ClockTimestepEvent(this, timestep);
			for (ClockListener listener : listeners
					.getListeners(ClockListener.class)) {
				listener.onTimestepEvent(event);
			}
		}
	}

	/**
	 * @return The timestep that the clock will run to.
	 */
	public long getLength() {
		return length;
	}

	public State getState() {
		return state;
	}

	public void addListener(ClockListener listener) {
		listeners.add(ClockListener.class, listener);
	}

	private void setState(State state) {
		synchronized (this) {
			this.state = state;
			ClockStateEvent stateEvent = new ClockStateEvent(this, state);
			for (ClockListener listener : listeners
					.getListeners(ClockListener.class)) {
				listener.onStateChange(stateEvent);
			}
		}
	}

	protected void nextTick() {

		synchronized (this) {
			final boolean forwards = millisPerTimestep > 0;

			if (timestep == length)
				return;

			if (forwards) {
				if (++tick % ticksPerTimestep == 0) {
					tick = 0;
					setTimestep(timestep + 1);
				}
			} else {
				if (--tick < 0) {
					tick = ticksPerTimestep - 1;
					setTimestep(timestep - 1);
				}
			}

			broadcastTick();

			if (timestep == length) {
				timestepTimer.cancel();
				setState(State.ENDED);
			} else if (timestep == 0 && tick == 0 && !forwards) {
				timestepTimer.cancel();
				setState(State.STOPPED);
			}
		}
	}

	/**
	 * Allows the visualisation clock to jump the timestep it is at to a
	 * specific step. This resets the tick of the clock to zero.
	 * 
	 * @param jumpStep
	 *            the step to jump to.
	 */
	public synchronized void jumpToTimestep(long jumpStep) {
		synchronized (this) {
			changeCheck++;
			final boolean playing = state == State.PLAYING;
			if (playing) {
				pause();
			}
			setTimestep(jumpStep);
			tick = (millisPerTimestep < 0 && state != State.STOPPED) ? ticksPerTimestep - 1
					: 0;
			broadcastTick();
			if (playing && state != State.STOPPED)
				play();
		}
	}

	private void broadcastTick() {
		ClockTickEvent tickEvent = new ClockTickEvent(this, tick);
		for (ClockListener listener : listeners
				.getListeners(ClockListener.class)) {
			listener.onTickEvent(tickEvent);
		}
	}

	/**
	 * Stops execution of the clock and resets it to the start of the
	 * simulation.
	 */
	public void stop() {
		synchronized (this) {
			setState(State.STOPPED);
			jumpToTimestep(0);
		}
	}

	public int getTicksPerTimestep() {
		synchronized (this) {
			return ticksPerTimestep;
		}
	}

	public synchronized void setLength(long visLength) {
		length = visLength;
	}
}
