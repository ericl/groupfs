package groupfs.tests;

import java.util.*;

import fuse.FuseException;

import groupfs.Filesystem;

import groupfs.backend.*;

public class ReadWrite extends Test {
	public void run() {
		JournalingBackend backend = getNewBackend();
		Random r = new Random(123);
		for (int i=0; i < 1000; i++) {
			String a = rstr(r);
			String b = rstr(r);
			String c = rstr(r);
			String d = rstr(r);
			String e = rstr(r);
			syn(backend, "generic file", a, b, c, d, e);
		}
		Filesystem fs = new Filesystem(backend);
		System.out.print("base build ");
		for (int i=0; i < 8; i++) {
			long time = System.currentTimeMillis();
			SortedSet<String> f = new TreeSet<String>();
			SortedSet<String> d = new TreeSet<String>();
			buildFileSet(fs, f, d, ".");
			System.err.println((System.currentTimeMillis() - time) + " ms");
			System.gc();
			try {
				switch (i) {
					case 0:
						System.out.print("base crawl ");
						break;
					case 1:
						fs.rename("/a/generic file", "/a/b/c/d/foo.str");
						fs.rename("/b/generic file", "/b/c/lo.txt");
						fs.rename("/c/generic file", "/c/b/bar.str");
						fs.rename("/d/generic file", "/d/foo.ls");
						System.out.print("rename 4x  ");
						break;
					case 2:
						System.out.print("ren crawl  ");
						break;
					case 3:
						System.out.print("ren crawl2 ");
						break;
					case 4:
						fs.unlink("/e/generic file");
						fs.unlink("/f/generic file");
						fs.unlink("/g/generic file");
						fs.unlink("/h/generic file");
						fs.unlink("/i/generic file");
						fs.unlink("/j/generic file");
						System.out.print("delete 4x  ");
						break;
					case 5:
						System.out.print("del crawl  ");
						break;
					case 6:
						System.out.print("del crawl2 ");
						break;
				}
			} catch (FuseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private String rstr(Random rand) {
		switch (rand.nextInt(13)) {
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
