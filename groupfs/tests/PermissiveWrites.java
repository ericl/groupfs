package groupfs.tests;

import groupfs.backend.*;

import fuse.FuseException;

import groupfs.*;

// deep mkdir -> fail
// mknod 2x in nested new dirs
public class PermissiveWrites extends Test {
	public void run() {
		QueryBackend backend = getNewBackend();
		Filesystem fs = new Filesystem(backend);
		try {
			int ret = fs.mkdir("/foo/bar", 0);
			if (ret == 0)
				throw new FuseException(
					"\nexpected: >0" +
					"\nreturned: " + ret
				);
			fs.mkdir("/foo", 0);
			fs.mkdir("/foo/bar", 0);
			fs.mknod("/foo/bar/new file", 0, 0);
			fs.mknod("/foo/bar/another file", 0, 0);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./foo/new file",
				"./foo/another file",
				"./bar/new file",
				"./bar/another file",
				"./.undefined/new file",
				"./.undefined/another file",
			},
			new String[] {
				".",
				"./foo",
				"./bar",
				"./.undefined",
			}
		);
	}
}
