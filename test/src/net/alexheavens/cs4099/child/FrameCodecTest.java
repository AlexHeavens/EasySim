package net.alexheavens.cs4099.child;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import net.alexheavens.cs4099.child.Frame.Message;
import net.alexheavens.cs4099.child.Frame.Payload;

public class FrameCodecTest {

	private static byte[] encode(Frame frame) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		FrameCodec.write(new DataOutputStream(bytes), frame);
		return bytes.toByteArray();
	}

	private static Frame decode(byte[] bytes) throws IOException {
		return FrameCodec.read(new DataInputStream(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testEveryFrameRoundTrips() throws IOException {
		List<Frame> frames = List.of(
				new Frame.Send(1, 2, 3, new Message("tag", new Payload.Text("hello"))),
				new Frame.SendAll(1, 2, new Message(null, new Payload.Whole(-7))),
				new Frame.Receive(1, 2, Frame.Receive.ANY_NEIGHBOUR),
				new Frame.Pause(1, 2, 30),
				new Frame.SetColour(1, 2, 0xff8800),
				new Frame.Neighbours(1, 2),
				new Frame.Accepted(1, 2),
				new Frame.Received(1, 2, 4, new Message("t", new Payload.Fractional(2.5))),
				new Frame.Resumed(1, 2),
				new Frame.Start(1, "TreeLeaderNode", true, 99),
				new Frame.Kill(1),
				new Frame.Ended(1, null),
				new Frame.Ended(1, "student bug"),
				new Frame.Send(1, 2, 3, new Message("t", new Payload.Constant("Colour", "RED"))));
		for (Frame frame : frames)
			assertEquals(frame, decode(encode(frame)));
	}

	@Test
	public void testNeighbourListRoundTrips() throws IOException {
		Frame.NeighbourList decoded = (Frame.NeighbourList) decode(
				encode(new Frame.NeighbourList(1, 2, new int[] { 5, 6, 7 })));
		assertArrayEquals(new int[] { 5, 6, 7 }, decoded.neighbours());
	}

	@Test
	public void testStringAtLimitIsAccepted() throws IOException {
		Frame frame = new Frame.Send(1, 2, 3,
				new Message(null, new Payload.Text("x".repeat(FrameCodec.MAX_STRING_LENGTH))));
		assertEquals(frame, decode(encode(frame)));
	}

	@Test
	public void testOverlongStringIsRejectedOnWrite() {
		Frame frame = new Frame.Send(1, 2, 3,
				new Message(null, new Payload.Text("x".repeat(FrameCodec.MAX_STRING_LENGTH + 1))));
		assertThrows(IOException.class, () -> encode(frame));
	}

	@Test
	public void testOverlongStringIsRejectedOnReadBeforeAllocating() {
		// A hostile frame: Ended, node 1, failure present, length 65535 with no chars following.
		byte[] hostile = { 13, 0, 0, 0, 1, 1, (byte) 0xff, (byte) 0xff };
		assertThrows(IOException.class, () -> decode(hostile));
	}

	@Test
	public void testUnknownTypesAreRejected() {
		assertThrows(IOException.class, () -> decode(new byte[] { 99 }));
	}

	@Test
	public void testNegativeNeighbourCountIsRejected() {
		byte[] hostile = { 10, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };
		assertThrows(IOException.class, () -> decode(hostile));
	}
}
