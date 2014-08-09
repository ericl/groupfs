package groupfs.tests;

import groupfs.storage.*;
import groupfs.state.Manager;

import fuse.FuseException;

import groupfs.*;

// mkdir
// nested mkdir
// moving new dir
// deletion of nested dirs
// moving of nested empty directories
public class MovingEmptyDirs extends Test {
	public void run() {
		Manager backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.mkdir("/Bar", 0);
			fs.mkdir("/Manual/untitled folder", 0);
			fs.mkdir("/Manual/untitled folder/tree", 0);
			fs.rename("/Manual/untitled folder", "/Manual/Foo");
			fs.mkdir("/Bar/1", 0);
			fs.mkdir("/Bar/1/2", 0);
			fs.rmdir("/Bar/1");
			fs.mkdir("/Bar/1", 0);
			fs.mkdir("/Bar/3", 0);
			fs.mkdir("/Bar/3/4", 0);
			fs.rename("/Bar/3", "/Bar/1/3");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_nocopy(fs,
			new String[] {
				"./Manual/perl-in-perl.pl #Manual",
				"./.pl/perl-in-perl.pl #Manual",
			},
			new String[] {
				".",
				"./.pl",
				"./Manual",
				"./Manual/Foo",
				"./Manual/Foo/tree",
				"./Bar",
				"./Bar/1",
				"./Bar/1/3",
				"./Bar/1/3/4",
			}
		);
	}
}
