package net.alexheavens.cs4099.simulation;

public class ImportedNode implements
		IImportedNode {

	private final Class<?> createType;

	public static ImportedNode createType(Class<?> clazz) {
        return new ImportedNode(clazz);
	}
	
	protected ImportedNode(Class<?> nodeClass) {
		if (nodeClass == null)
			throw new IllegalArgumentException(
					"Attempted to create null class type from user code.");
		createType = nodeClass;
	}

	public Object create() throws InstantiationException,
			IllegalAccessException {
		return createType.newInstance();
	}

}
