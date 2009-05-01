package queryfs.tests;

import fuse.FuseException;

import queryfs.backend.*;

import queryfs.*;

// test a hack to allow new files to survive disappearing directories
public class HoldNewFilesOpen extends Test {
	public void run() {
		QueryBackend backend = getNewBackend();
		Filesystem fs = new Filesystem(backend);
		expect(fs,
			new String[] {
			},
			new String[] {
				".",
			}
		);
		try {
			fs.mkdir("/foo", 0);
			fs.mkdir("/foo/bar", 0);
			fs.mknod("/foo/bar/FileWithStuffHere.java", 0, 0);
			int ret = fs.open("/foo/bar/FileWithStuffHere.java", 0, null);
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
				"./foo/FileWithStuffHere.java",
				"./bar/FileWithStuffHere.java",
				"./.java/FileWithStuffHere.java",
			},
			new String[] {
				".",
				"./foo",
				"./bar",
				"./.java",
			}
		);
	}
}
