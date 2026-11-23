package net.alexheavens.cs4099.usercode;

/** Student code uses a construct that cannot run as a node, with a message that tells the student what to change. */
public class UnsupportedNodeCodeException extends RuntimeException {

	public UnsupportedNodeCodeException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;
}
