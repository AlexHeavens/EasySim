package net.alexheavens.cs4099.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import net.alexheavens.cs4099.network.configuration.ILinkConfig;
import net.alexheavens.cs4099.network.configuration.LinkConfig;

import org.junit.Test;

public class LinkConfigTest {

	@Test
	public void testNewLinkConfig() {
		ILinkConfig link = new LinkConfig(1, 2, 100);
		assertEquals(1, link.source());
		assertEquals(2, link.target());
		assertEquals(100, link.latency());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidSource() {
		new LinkConfig(-1, 0, 100);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidTarget() {
		new LinkConfig(0, -1, 100);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidLatency() {
		new LinkConfig(0, 1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSameSourceTarget() {
		new LinkConfig(1, 1, 10);

	}

	@Test
	public void testLinkEquals() {
		ILinkConfig a = new LinkConfig(0, 1, 100);
		ILinkConfig b = new LinkConfig(0, 1, 100);
		ILinkConfig c = new LinkConfig(1, 0, 100);
		ILinkConfig d = new LinkConfig(5, 6, 50);

		assertTrue(a.equals(b));
		assertTrue(a.equals(c));
		assertFalse(a.equals(d));
		assertFalse(a.equals("Blah"));
	}

	@Test
	public void testSingleHashCode() {
		int expHash = 328680;
		ILinkConfig link = new LinkConfig(5, 1000, 500);
		assertEquals(expHash, link.hashCode());
	}

	@Test
	public void testLinkHashCodeEquals() {
		for (int a = 0; a < 1000; a++) {
			for (int b = 0; b < 1000; b++) {
				if (a == b)
					continue;
				ILinkConfig linkA = new LinkConfig(a, b, 100);
				ILinkConfig linkB = new LinkConfig(b, a, 100);
				assertEquals(linkB.hashCode(), linkA.hashCode());
			}
		}
	}

	@Test
	public void testLinkHashCodeNotEquals() {
		HashSet<Integer> hashes = new HashSet<Integer>();
		for (int a = 0; a < 1000; a++) {
			for (int b = 0; b < 1000 / 2; b++) {
				if (a == b)
					continue;

				ILinkConfig link = new LinkConfig(a, b, 100);
				boolean contained = hashes.add(link.hashCode());
				assertTrue(!contained || link.equals(new LinkConfig(b, a, 100)));
			}
		}
	}

}
