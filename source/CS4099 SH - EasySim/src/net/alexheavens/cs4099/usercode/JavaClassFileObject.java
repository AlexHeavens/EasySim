package net.alexheavens.cs4099.usercode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public class JavaClassFileObject extends SimpleJavaFileObject {


	private ByteArrayOutputStream outputStream;
	
	protected JavaClassFileObject(String name, Kind kind) {
		super(URI.create("string:///" + name.replace('.', '/')
	            + kind.extension), kind);
		outputStream = new ByteArrayOutputStream();
	}
	
	public byte[] getBytes() {
        return outputStream.toByteArray();
    }

    public OutputStream openOutputStream() throws IOException {
        return outputStream;
    }

}
