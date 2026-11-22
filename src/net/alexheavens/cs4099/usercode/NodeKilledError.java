package net.alexheavens.cs4099.usercode;

/**
 * Thrown inside a node's thread, at the safe points added to user code, once the node has been killed. It is an
 * {@link Error} so that user code catching {@link Exception} does not stop it, and the check throws it again at
 * the next safe point if user code catches it anyway. It plays the part of the old <code>ThreadDeath</code>.
 */
public class NodeKilledError extends Error {

	private static final long serialVersionUID = 1L;

	NodeKilledError() {
		super("Node killed.", null, false, false);
	}
}
