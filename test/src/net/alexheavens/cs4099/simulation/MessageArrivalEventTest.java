package net.alexheavens.cs4099.simulation;

import net.alexheavens.cs4099.network.StringMessage;

import org.junit.Test;

public class MessageArrivalEventTest {

	@Test(expected = IllegalArgumentException.class)
	public void testUnsentMessage(){
		StringMessage message = new StringMessage("BLAH");
		new MessageArrivalEvent(message, 1);
	}
	
}
