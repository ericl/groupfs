package groupfs.tests;

import groupfs.backend.*;

import fuse.FuseException;

import groupfs.*;

// overwriting file with another
public class OverwriteRenaming extends Test {
	public void run() {
		JournalingBackend backend = getNewBackend();
		syn(backend, "Random Book.txt", "Book", "Readable");
		Filesystem fs = new Filesystem(backend);
		try {
			int ret = fs.mknod("/Book/4091.swp", 0, 0);
			if (ret != 0)
				throw new FuseException(
					"\nexpected: " + 0 +
					"\nreturned: " + ret
				);
			ret = fs.rename("/Book/4091.swp", "/Book/Random Book.txt");
			if (ret != 0)
				throw new FuseException(
					"\nexpected: " + 0 +
					"\nreturned: " + ret
				);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Book/Random Book.txt",
				"./.txt/Random Book.txt",
				"./Readable/Random Book.txt",
			},
			new String[] {
				".",
				"./Book",
				"./.txt",
				"./Readable",
			}
		);
	}
}
