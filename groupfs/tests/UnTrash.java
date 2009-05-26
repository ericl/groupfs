package groupfs.tests;

import groupfs.backend.*;

import fuse.FuseException;

import groupfs.*;

/* 
 * mv from trash -> has tags
 */
public class UnTrash extends Test {
	public void run() {
		QueryBackend backend = getNewBackend();
		syn(backend, "foo.png");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.mkdir("/A", 0);
			fs.rename("/.Trash/foo.png", "/A/foo.png");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./A/foo.png",
				"./.png/foo.png",
			},
			new String[] {
				".",
				"./A",
				"./.png",
			}
		);
	}
}
