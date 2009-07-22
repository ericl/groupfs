package groupfs.tests;

import groupfs.backend.*;

import fuse.FuseException;

import groupfs.*;

// automkdir
// file gaining TAG
// file losing TAG
public class FileRenaming extends Test {
	public void run() {
		DataProvider backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual", "Random");
		syn(backend, "x.pl", "Random");
		Filesystem fs = new Filesystem(backend);
		expect(fs,
			new String[] {
				"./Manual/perl-in-perl.pl",
				"./Random/perl-in-perl.pl",
				"./Random/x.pl",
				"./Random/Manual/perl-in-perl.pl",
				"./.pl/perl-in-perl.pl",
				"./.pl/x.pl",
			},
			new String[] {
				".",
				"./.pl",
				"./Manual",
				"./Random",
				"./Random/Manual",
			}
		);
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
				"./Random/perl-in-english.man",
				"./Random/x.pl",
				"./Random/Manual/perl-in-english.man",
				"./Random/.man/perl-in-english.man",
				"./Random/.pl/x.pl",
				"./.man/perl-in-english.man",
				"./.pl/x.pl",
			},
			new String[] {
				".",
				"./.man",
				"./.pl",
				"./Manual",
				"./Random",
				"./Random/Manual",
				"./Random/.pl",
				"./Random/.man",
			}
		);
	}
}
