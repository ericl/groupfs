package groupfs.tests;

import fuse.FuseException;

import groupfs.storage.*;
import groupfs.state.*;

import groupfs.*;

// two existing nodes of same name
// node moved to have different name
// node moved to have same name
public class DuplicateHandling extends Test {
	public void run() {
		Util.setHashTagsEnabled(false);
		Manager backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual", "Random");
		syn(backend, "perl-in-perl.pl", "Random");
		Filesystem fs = new Filesystem(backend);
		expect(fs,
			new String[] {
				"./Manual/perl-in-perl.pl",
				"./Random/perl-in-perl.pl",
				"./Random/perl-in-perl.pl.0",
				"./Random/Manual/perl-in-perl.pl",
				"./.pl/perl-in-perl.pl",
				"./.pl/perl-in-perl.pl.0",
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
			fs.rename("/Manual/perl-in-perl.pl", "/Manual/perl.pl");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect_alternatives(fs,
			new String[] {
				".",
				"./.pl",
				"./Manual",
				"./Random",
				"./Random/Manual",
			},
			new String[] {
				"./Manual/perl.pl",
				"./Random/perl-in-perl.pl",
				"./Random/perl.pl",
				"./Random/Manual/perl.pl",
				"./.pl/perl-in-perl.pl",
				"./.pl/perl.pl",
			},
			new String[] {
				"./Manual/perl.pl",
				"./Random/perl-in-perl.pl.0",
				"./Random/perl.pl",
				"./Random/Manual/perl.pl",
				"./.pl/perl-in-perl.pl.0",
				"./.pl/perl.pl",
			}
		);
		try {
			fs.rename("/Manual/perl.pl", "/Manual/perl-in-perl.pl");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Manual/perl-in-perl.pl",
				"./Random/perl-in-perl.pl",
				"./Random/perl-in-perl.pl.0",
				"./Random/Manual/perl-in-perl.pl",
				"./.pl/perl-in-perl.pl",
				"./.pl/perl-in-perl.pl.0",
			},
			new String[] {
				".",
				"./.pl",
				"./Manual",
				"./Random",
				"./Random/Manual",
			}
		);
		backend = getNewBackend();
		syn(backend, "foo.txt", "Manual", "Random");
		syn(backend, "bar.txt", "Random");
		fs = new Filesystem(backend);
		expect(fs,
			new String[] {
				"./Manual/foo.txt",
				"./Random/foo.txt",
				"./Random/bar.txt",
				"./Random/Manual/foo.txt",
				"./.txt/foo.txt",
				"./.txt/bar.txt",
			},
			new String[] {
				".",
				"./.txt",
				"./Manual",
				"./Random",
				"./Random/Manual",
			}
		);
		try {
			fs.rename("/Random/Manual/foo.txt", "/Random/Manual/bar.txt");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Manual/bar.txt",
				"./Random/bar.txt",
				"./Random/bar.txt.0",
				"./Random/Manual/bar.txt",
				"./.txt/bar.txt.0",
				"./.txt/bar.txt",
			},
			new String[] {
				".",
				"./.txt",
				"./Manual",
				"./Random",
				"./Random/Manual",
			}
		);
	}
}
