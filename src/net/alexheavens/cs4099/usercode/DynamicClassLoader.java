package net.alexheavens.cs4099.usercode;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * The <code>DynamicClassLoader</code> allows user defined Classes for use in
 * simulation to be loaded and compiled from file at runtime.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @version 1.0
 */
public class DynamicClassLoader {

	/**
	 * Loads the Class type of the Node specified in <code>file</code>
	 * 
	 * @param file
	 *            the file containing the Node definition.
	 * @return The Class of the User Node.
	 * @throws IOException
	 *             If the source file cannot be found.
	 * @throws ClassNotFoundException
	 *             If the user's class cannot be found.
	 * @throws ClassLoaderException
	 *             if the user's code cannot compile.
	 */
	public Class<?> loadClass(File file) throws IOException,
			ClassNotFoundException, ClassLoaderException {
		String classString = file.getName();
		classString = classString.substring(0, classString.lastIndexOf('.'));

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		if (compiler == null)
			throw new IllegalStateException(
					"No default system compiler provided.");

		JavaFileManager fileManager = new ClassFileManager(
				compiler.getStandardFileManager(null, null, null));

		List<JavaFileObject> jfiles = new LinkedList<JavaFileObject>();
		jfiles.add(new JavaSourceFileObject(file.toURI()));

		compiler.getTask(null, fileManager, null, null, null, jfiles).call();
		final Class<?> newClass = fileManager.getClassLoader(null).loadClass(
				classString);
		if (newClass == null)
			throw new ClassLoaderException(
					"Unable to load class from user file.  May be corrupted.");
		return newClass;
	}
}
