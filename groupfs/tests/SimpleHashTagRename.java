package groupfs.tests;

import groupfs.storage.*;

import fuse.FuseException;

import groupfs.*;
import groupfs.state.Manager;

// mkdir
// file gaining TAG
// file losing TAG
// tag alphabetization and spaces in tags
public class SimpleHashTagRename extends Test {
	public void run() {
		Manager backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.mkdir("/Manual/Readable", 0);
			fs.rename("/Manual/perl-in-perl.pl #Manual", "/Manual/perl-in-perl.pl #Manual #Readable");
			fs.rename("/Manual/perl-in-perl.pl #Manual #Readable", "/Manual/perl-in-perl.pl #Readable");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Readable/perl-in-perl.pl #Readable",
				"./.pl/perl-in-perl.pl #Readable",
			},
			new String[] {
				".",
				"./.pl",
				"./Readable",
			}
		);
		backend = getNewBackend();
		syn(backend, "perl-in-perl", "ab c", "z");
		fs = new Filesystem(backend);
		try {
			fs.rename("/ab c/perl-in-perl #ab c #z", "/ab c/perl-in-perl #z #abc");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./abc/perl-in-perl #abc #z",
				"./z/perl-in-perl #abc #z",
				"./.undefined/perl-in-perl #abc #z",
			},
			new String[] {
				".",
				"./.undefined",
				"./abc",
				"./z",
			}
		);
	}
}
