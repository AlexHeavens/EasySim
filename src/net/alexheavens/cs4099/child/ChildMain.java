package net.alexheavens.cs4099.child;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.Socket;

import net.alexheavens.cs4099.scheduler.ContinuationScheduler;
import net.alexheavens.cs4099.usercode.DynamicClassLoader;
import net.alexheavens.cs4099.usercode.NodeScript;

/**
 * Entry point of the child JVM (ADR 0002). {@link ChildJvm} starts it with the Tascalate agent. It connects to the
 * simulator on the loopback port, then runs {@link NodeHost} until the simulator closes the connection.
 * <p>
 * Arguments: port, worker count, time slice in safe-point ticks, directory holding the student sources.
 */
public final class ChildMain {

	private ChildMain() {
	}

	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		int workers = Integer.parseInt(args[1]);
		int sliceTicks = Integer.parseInt(args[2]);
		File scriptDir = new File(args[3]);
		try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
				ContinuationScheduler scheduler = new ContinuationScheduler(workers, sliceTicks)) {
			// The host writes a frame as several small writes with no flush, so the stream is not buffered.
			socket.setTcpNoDelay(true);
			new NodeHost(new DataInputStream(new BufferedInputStream(socket.getInputStream())),
					new DataOutputStream(socket.getOutputStream()), name -> script(scriptDir, name), scheduler).run();
		}
	}

	private static NodeScript script(File dir, String name) {
		try {
			return (NodeScript) new DynamicClassLoader().loadClass(new File(dir, name + ".java"))
					.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot load student script " + name, e);
		}
	}
}
