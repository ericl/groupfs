package queryfs.tests;

import queryfs.backend.*;

import fuse.FuseException;

import queryfs.*;

// mkdir
// file gaining TAG
// file losing TAG
public class FileRenaming extends Test {
	public void run() {
		QueryBackend backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.rename("/Manual/perl-in-perl.pl", "/Manual/perl-in-english.man");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Manual/perl-in-english.man",
				"./.man/perl-in-english.man",
			},
			new String[] {
				".",
				"./.man",
				"./Manual",
			}
		);
	}
}
