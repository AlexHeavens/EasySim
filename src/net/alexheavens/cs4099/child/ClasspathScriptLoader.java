package net.alexheavens.cs4099.child;

import java.io.IOException;
import java.io.InputStream;

import net.alexheavens.cs4099.usercode.NodeInstrumenter;

/**
 * Loads a script that is already compiled and on the classpath, and instruments it as the compiler path does, so the
 * node can suspend inside it (ADR 0001). The script's top-level class and its nested classes are instrumented. Any
 * other class a script uses is loaded as usual and is not instrumented.
 */
final class ClasspathScriptLoader extends ClassLoader {

	private final String topLevel;

	ClasspathScriptLoader(String scriptClass) {
		super(ClasspathScriptLoader.class.getClassLoader());
		int nested = scriptClass.indexOf('$');
		this.topLevel = nested < 0 ? scriptClass : scriptClass.substring(0, nested);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (!name.equals(topLevel) && !name.startsWith(topLevel + "$"))
			return super.loadClass(name, resolve);
		synchronized (getClassLoadingLock(name)) {
			Class<?> loaded = findLoadedClass(name);
			if (loaded == null)
				loaded = define(name);
			return loaded;
		}
	}

	private Class<?> define(String name) throws ClassNotFoundException {
		try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
			if (in == null)
				throw new ClassNotFoundException(name);
			byte[] bytes = NodeInstrumenter.instrument(in.readAllBytes());
			return defineClass(name, bytes, 0, bytes.length);
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		}
	}
}
