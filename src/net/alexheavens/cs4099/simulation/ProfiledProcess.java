package net.alexheavens.cs4099.simulation;

public interface ProfiledProcess {

	public void kill();
	
	public Thread getThread();
	
	public int getSimulationId();
}
