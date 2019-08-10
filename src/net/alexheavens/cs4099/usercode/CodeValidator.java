package net.alexheavens.cs4099.usercode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Provides a means of validating user code.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class CodeValidator {

	private ClassLimitation limit;

	/**
	 * Creates a validator that only passes code that meets the given
	 * limitation.
	 * 
	 * @param limit
	 *            a limitation that validated code must pass.
	 */
	public CodeValidator(ClassLimitation limit) {
		this.limit = limit;
	}

	/**
	 * If passed, a class is valid to the specification.
	 * 
	 * @param c
	 *            a class which we are testing.
	 * @param C
	 *            a restriction on the class type.
	 * @throws LimitationFailureException
	 *             if c fails to meet the validators limitation.
	 */
	public <C> void validate(Class<? extends C> c)
			throws LimitationFailureException {
		Collection<Field> classFields = new ArrayList<Field>();
		appendFields(c, classFields);
		for (Field field : classFields) {
			int modifiers = field.getModifiers();
			boolean constantFail = !limit.constantsAllowed()
					&& Modifier.isFinal(modifiers)
					&& Modifier.isStatic(modifiers);
			boolean staticFail = !limit.staticAllowed()
					&& Modifier.isStatic(modifiers)
					&& !Modifier.isFinal(modifiers);
			if (constantFail || staticFail)
				throw new LimitationFailureException(c, limit);
		}
	}

	private static void appendFields(Class<?> c, Collection<Field> fields) {
		for (Field field : c.getDeclaredFields()) {
			fields.add(field);
		}
		Class<?> superclass = c.getSuperclass();
		if (superclass != null)
			appendFields(superclass, fields);
	}
}
