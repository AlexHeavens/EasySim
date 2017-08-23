package net.alexheavens.cs4099.simulation;

public class SimulationRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7071783245486451496L;
	private final RuntimeException exception;
	
	public SimulationRuntimeException(RuntimeException e) {
		exception = e;
	}

	public RuntimeException getException(){
		return exception;
	}

}
