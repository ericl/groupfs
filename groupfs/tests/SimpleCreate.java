package groupfs.tests;

import groupfs.storage.*;

import fuse.FuseException;

import groupfs.*;
import groupfs.state.Manager;

// MIME subclassing
// TAG subclassing
// multiple subclassing
// Trash subclassing
// mknod of dotfile
public class SimpleCreate extends Test {
	public void run() {
		Manager backend = getNewBackend();
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
				"./Book/Random Book.txt #Book #Readable",
				"./.txt/sed.txt #Manual #Readable",
				"./.txt/Random Book.txt #Book #Readable",
				"./.Trash/trashed.txt",
				"./Manual/sed.txt #Manual #Readable",
				"./Readable/Book/Random Book.txt #Book #Readable",
				"./Readable/sed.txt #Manual #Readable",
				"./Readable/Manual/sed.txt #Manual #Readable",
				"./Readable/Random Book.txt #Book #Readable",
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
