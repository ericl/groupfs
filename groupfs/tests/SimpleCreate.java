package groupfs.tests;

import groupfs.backend.*;

import fuse.FuseException;

import groupfs.*;

// MIME subclassing
// TAG subclassing
// multiple subclassing
// Trash subclassing
// mknod of dotfile
public class SimpleCreate extends Test {
	public void run() {
		QueryBackend backend = getNewBackend();
		syn(backend, "sed.txt", "Manual", "Readable");
		syn(backend, "Random Book.txt", "Book", "Readable");
		syn(backend, "trashed.txt");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.mknod("/Book/.DNE.txt", 0, 0);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Book/Random Book.txt",
				"./.txt/sed.txt",
				"./.txt/Random Book.txt",
				"./.Trash/trashed.txt",
				"./Manual/sed.txt",
				"./Readable/Book/Random Book.txt",
				"./Readable/sed.txt",
				"./Readable/Manual/sed.txt",
				"./Readable/Random Book.txt",
			},
			new String[] {
				".",
				"./Book",
				"./.txt",
				"./.Trash",
				"./Manual",
				"./Readable",
				"./Readable/Book",
				"./Readable/Manual",
			}
		);
	}
}
