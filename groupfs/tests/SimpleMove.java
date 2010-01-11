package groupfs.tests;

import groupfs.storage.*;

import fuse.FuseException;

import groupfs.*;
import groupfs.state.Manager;

// mkdir
// file gaining TAG
// file losing TAG
// file losing all tags
public class SimpleMove extends Test {
	public void run() {
		Manager backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.mkdir("/Manual/Readable", 0);
			fs.rename("/Manual/perl-in-perl.pl", "/Manual/Readable/perl-in-perl.pl");
			fs.rename("/Manual/perl-in-perl.pl", "/Readable/perl-in-perl.pl");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Readable/perl-in-perl.pl",
				"./.pl/perl-in-perl.pl",
			},
			new String[] {
				".",
				"./.pl",
				"./Readable",
			}
		);
		backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual");
		fs = new Filesystem(backend);
		try {
			fs.rename("/Manual/perl-in-perl.pl", "/perl-in-perl.pl");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./.Trash/perl-in-perl.pl",
			},
			new String[] {
				".",
				"./.Trash",
			}
		);
	}
}
