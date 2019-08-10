package net.alexheavens.cs4099.concurrent;

/**
 * The WaitRegistrar specifies an interface for Threads to wait and resume,
 * allowing custom operations on the part of the WaitRegistrar to be performed
 * on either.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public interface WaitRegistrar {

	/**
	 * Performs operations before waiting the Thread that made the call to
	 * await. Any implementation must wait the current Thread during this call.
	 * 
	 * @throws InterruptedException
	 *             On interruption of the waiting Thread.
	 */
	public void waitThread() throws InterruptedException;

	/**
	 * Notifies a specific thread waiting at the registrar. Any implementation
	 * must notify the target Thread in this call.
	 * 
	 * @param target
	 *            the thread to be resumed.
	 */
	public void notifyThread(Thread target);

	/**
	 * @param member
	 *            any Thread.
	 * @return Whether member is waiting in the registrar.
	 */
	public boolean containsThread(Thread member);
}
