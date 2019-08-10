package net.alexheavens.cs4099.usercode;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureClassLoader;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

public class ClassFileManager extends
		ForwardingJavaFileManager<JavaFileManager> {

	private JavaClassFileObject classObject;

	protected ClassFileManager(JavaFileManager fileManager) {
		super(fileManager);
	}

	public ClassLoader getClassLoader(Location location){

		// Create a new anonymous class from the bytestream of our class object.
		return AccessController
				.doPrivileged(new PrivilegedAction<SecureClassLoader>() {
					public SecureClassLoader run() {
						return new SecureClassLoader() {
							protected Class<?> findClass(String name)
									throws ClassNotFoundException {
								if (classObject == null)
									return null;
								final byte[] classBytes = classObject
										.getBytes();
								return super.defineClass(name,
										classObject.getBytes(), 0,
										classBytes.length);
							}
						};
					}
				});

	}

	public JavaFileObject getJavaFileForOutput(Location location,
			String className, Kind kind, FileObject sibling) throws IOException {
		classObject = new JavaClassFileObject(className, kind);
		return classObject;
	}
}
