package net.alexheavens.cs4099.usercode;

/**
 * Defines a series of limitations that a class should pass to be valid.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class ClassLimitation {

	private final boolean staticAllowed;
	private final boolean constantsAllowed;
	
	/**
	 * Creates a limitation on the use of purely static fields. Note that this
	 * does allow the combination of final and static.
	 * 
	 * @param allowStatic
	 *            whether static fields are allowed.
	 */
	public ClassLimitation(boolean allowStatic, boolean allowConstants) {
		staticAllowed = allowStatic;
		constantsAllowed = allowConstants;
	}
	
	/**
	 * @return If statics are limited.
	 */
	public boolean staticAllowed(){
		return staticAllowed;
	}
	
	/**
	 * @return If static/finals are limited.
	 */
	public boolean constantsAllowed(){
		return constantsAllowed;
	}

}
