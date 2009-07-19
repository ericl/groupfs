package groupfs.tests;

import groupfs.backend.*;

import fuse.FuseException;

import groupfs.*;

/* 
 * root mkdir
 * mknod in mkdir
 * mknod in nested mkdir -> residual dir
 * root mknod -> fail
 * mime mknod -> fail
 * autodeletion of floating dirs
 * mv to dotfile
 */
public class StrangeMovements extends Test {
	public void run() {
		JournalingBackend backend = getNewBackend();
		Filesystem fs = new Filesystem(backend);
		try {
			fs.mkdir("/Perl", 0);
			fs.mknod("/Perl/foo", 0, 0);
			fs.mkdir("/Foo", 0);
			fs.mkdir("/Foo/Bar", 0);
			fs.mknod("/Foo/Bar/node", 0, 0);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		try {
			fs.mknod("/rootnode", 0, 0);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Perl/foo",
				"./Foo/node",
				"./Bar/node",
				"./.undefined/node",
				"./.undefined/foo",
			},
			new String[] {
				".",
				"./Perl",
				"./Foo",
				"./Bar",
				"./.undefined",
			}
		);
		try {
			fs.unlink("/Foo/node");
			fs.unlink("/Perl/foo");
			fs.mkdir("/Foo", 0);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_nocopy(fs,
			new String[] {
				"./.Trash/foo",
				"./.Trash/node",
			},
			new String[] {
				".",
				"./.Trash",
				"./Foo",
			}
		);
		backend = getNewBackend();
		fs = new Filesystem(backend);
		try {
			fs.mkdir("/Perl", 0);
			fs.mknod("/Perl/foo", 0, 0);
			fs.mkdir("/Foo", 0);
			fs.mkdir("/Foo/Bar", 0);
			fs.mkdir("/Foo/Bar/Baz", 0);
			fs.mknod("/Foo/node", 0, 0);
			fs.rename("/Foo/node", "/Foo/Bar/node");
			fs.rename("/Foo/node", "/Foo/.node");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Perl/foo",
				"./Foo/node",
				"./Bar/node",
				"./.undefined/node",
				"./.undefined/foo",
			},
			new String[] {
				".",
				"./Perl",
				"./Foo", // no ./Foo/Bar
				"./Bar",
				"./.undefined",
			}
		);
		try {
			fs.mkdir("/Foo/Bar", 0);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_nocopy(fs,
			new String[] {
				"./Perl/foo",
				"./Foo/node",
				"./Bar/node",
				"./.undefined/node",
				"./.undefined/foo",
			},
			new String[] {
				".",
				"./Perl",
				"./Foo",
				"./Foo/Bar", // no ./Foo/Bar/Baz
				"./Bar",
				"./.undefined",
			}
		);
		backend = getNewBackend();
		fs = new Filesystem(backend);
		try {
			fs.mkdir("/Perl", 0);
			fs.mkdir("/Perl/one", 0);
			fs.mkdir("/Perl/two", 0);
			fs.mkdir("/Perl/three", 0);
			fs.mknod("/Perl/one/x", 0, 0);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_nocopy(fs,
			new String[] {
				"./Perl/x",
				"./one/x",
				"./.undefined/x",
			},
			new String[] {
				".",
				"./Perl",
				"./one",
				"./Perl/two",
				"./Perl/three",
				"./.undefined",
			}
		);
		backend = getNewBackend();
		fs = new Filesystem(backend);
		try {
			fs.mkdir("/Perl", 0);
			fs.mkdir("/Perl/one", 0);
			fs.mkdir("/Perl/one/I_SHOULD_BE_DELETED", 0);
			fs.mkdir("/Perl/two", 0);
			fs.mkdir("/Perl/three", 0);
			fs.mknod("/Perl/x", 0, 0);
			fs.rename("/Perl/x", "/Perl/one/x");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_nocopy(fs,
			new String[] {
				"./Perl/x",
				"./one/x",
				"./.undefined/x",
			},
			new String[] {
				".",
				"./Perl",
				"./one",
				"./Perl/two",
				"./Perl/three",
				"./.undefined",
			}
		);
	}
}
