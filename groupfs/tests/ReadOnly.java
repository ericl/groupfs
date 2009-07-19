package groupfs.tests;

import java.util.*;

import groupfs.Filesystem;

import groupfs.backend.*;

public class ReadOnly extends Test {
	public void run() {
		JournalingBackend backend = getNewBackend();
		long time = System.currentTimeMillis();
		for (int i=0; i < 1000; i++)
			syn(backend, "generic file." + rstr(), rstr(), rstr(), rstr(), rstr());
		System.err.println("  synth " + (System.currentTimeMillis() - time) + " ms");
		time = System.currentTimeMillis();
		Filesystem fs = new Filesystem(backend);
		System.err.println("  mount " + (System.currentTimeMillis() - time) + " ms");
		for (int i=0; i < 4; i++) {
			time = System.currentTimeMillis();
			SortedSet<String> f = new TreeSet<String>();
			SortedSet<String> d = new TreeSet<String>();
			buildFileSet(fs, f, d, ".");
			System.err.println(i + " crawl " + (System.currentTimeMillis() - time) + " ms");
			System.gc();
		}
	}

	private String rstr() {
		switch ((int)(13*Math.random())) {
			case 0: return "a";
			case 1: return "b";
			case 2: return "c";
			case 3: return "d";
			case 4: return "e";
			case 5: return "f";
			case 6: return "g";
			case 7: return "h";
			case 8: return "i";
			case 9: return "j";
			case 10: return "k";
			case 11: return "l";
			case 12: return "m";
			default:
				assert false;
				return null;
		}
	}
}
