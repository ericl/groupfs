package groupfs.tests;

import groupfs.storage.*;

import fuse.FuseException;

import groupfs.*;
import groupfs.state.Manager;

// unlink from TAG
// unlink from MIME
// unlink from Trash
public class SimpleUnlink extends Test {
	public void run() {
		Manager backend = getNewBackend();
		syn(backend, "sed.txt", "Manual", "Doc");
		syn(backend, "sed2.txt", "Manual", "Doc");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.unlink("/Manual/sed.txt #Doc #Manual");
			fs.unlink("/.txt/sed2.txt #Doc #Manual");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./.Trash/sed.txt",
				"./.Trash/sed2.txt",
			},
			new String[] {
				".",
				"./.Trash",
			}
		);
		try {
			fs.unlink("/.Trash/sed.txt");
			fs.unlink("/.Trash/sed2.txt");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
			},
			new String[] {
				".",
			}
		);
	}
}
