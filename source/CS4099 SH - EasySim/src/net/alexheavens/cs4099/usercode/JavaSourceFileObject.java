package net.alexheavens.cs4099.usercode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class JavaSourceFileObject extends SimpleJavaFileObject {

	private File file;

	protected JavaSourceFileObject(URI path) {
		super(path, Kind.SOURCE);
		file = new File(uri);
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors)
			throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();
		String lineStr = null;
		while ((lineStr = reader.readLine()) != null) {
			sb.append(lineStr);
			sb.append("\n");
		}
		reader.close();
		return sb;
	}
}
