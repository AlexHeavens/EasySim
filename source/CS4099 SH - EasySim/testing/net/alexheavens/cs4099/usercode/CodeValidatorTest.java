package net.alexheavens.cs4099.usercode;

import org.junit.Before;
import org.junit.Test;

public class CodeValidatorTest {

	private ClassLimitation limitation;
	private CodeValidator validator;

	@Before
	public void setup() {
		limitation = new ClassLimitation(false, true);
		validator = new CodeValidator(limitation);
	}

	private class InheritedStatic extends StaticFieldNodeScript {
	}

	private class ConstantStatic extends MockNodeScript {
		@SuppressWarnings("unused")
		private final static int CONSTANT = 100;
	}

	@Test
	/**
	 * Test that a valid class passes the validator.
	 */
	public void testValid() throws LimitationFailureException {
		validator.<NodeScript> validate(MockNodeScript.class);
	}

	@Test(expected = LimitationFailureException.class)
	/**
	 * Test that a class with a static field fails validation.
	 */
	public void testInvalidStatic() throws LimitationFailureException {
		validator.<NodeScript> validate(StaticFieldNodeScript.class);
	}

	@Test(expected = LimitationFailureException.class)
	/**
	 * Test that a class that inherits a static field still fails validation.
	 */
	public void testInheritedInvalidStatic() throws LimitationFailureException {
		validator.<NodeScript> validate(InheritedStatic.class);
	}

	@Test
	/**
	 * Test that constant fields are allowed.
	 */
	public void testConstantValidation() throws LimitationFailureException {
		validator.<NodeScript> validate(ConstantStatic.class);
	}
}
