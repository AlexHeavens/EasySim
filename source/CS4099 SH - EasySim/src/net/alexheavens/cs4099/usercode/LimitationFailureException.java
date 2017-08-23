package net.alexheavens.cs4099.usercode;

/**
 * Marks the failure of a class to meet the ClassLimitation it was validated
 * against.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class LimitationFailureException extends Exception {

	private static final long serialVersionUID = 1L;
	private final Class<?> failureClass;
	private final ClassLimitation limitation;

	/**
	 * Creates a LimitationFailureException marking the class c as failing the
	 * limitation l.
	 * 
	 * @param c
	 *            the class that has failed.
	 * @param l
	 *            the limitation that c failed.
	 */
	public LimitationFailureException(Class<?> c, ClassLimitation l) {
		if (c == null)
			throw new IllegalArgumentException("Null class.");
		if (l == null)
			throw new IllegalArgumentException("Null limitation.");
		failureClass = c;
		limitation = l;
	}

	/**
	 * @return the Class that has failed the limitation.
	 */
	public Class<?> getExceptionClass() {
		return failureClass;
	}

	/**
	 * @return the ClassLimitation that the class has failed to pass.
	 */
	public ClassLimitation getLimitation() {
		return limitation;
	}

}
