package net.alexheavens.cs4099.usercode;

import static org.junit.Assert.*;

import org.junit.Test;

public class LimitationFailureExceptionTest {

	@Test
	/**
	 * Test that a valid LimitationFailureException reflects the class and
	 * limitation it has failed.
	 */
	public void testValidCreation() {
		ClassLimitation limitation = new ClassLimitation(false, true);
		LimitationFailureException failure = new LimitationFailureException(
				Object.class, limitation);
		assertEquals(Object.class, failure.getExceptionClass());
		assertEquals(limitation, failure.getLimitation());
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot create an exception with a null class.
	 */
	public void testNullCreation() {
		
		new LimitationFailureException(null, new ClassLimitation(false, true));
	}

	@Test(expected = IllegalArgumentException.class)
	/**
	 * Test that we cannot create an exception with a null limitation.
	 */
	public void testNullLimitation() {
		new LimitationFailureException(Object.class, null);
	}
}
