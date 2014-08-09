package groupfs.tests;

import groupfs.storage.*;
import groupfs.state.Manager;

import fuse.FuseException;

import groupfs.*;

// automkdir
// file gaining TAG
// file losing TAG
public class FileRenaming extends Test {
	public void run() {
		Manager backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual", "Random");
		syn(backend, "x.pl", "Random");
		Filesystem fs = new Filesystem(backend);
		expect(fs,
			new String[] {
				"./Manual/perl-in-perl.pl #Manual #Random",
				"./Random/perl-in-perl.pl #Manual #Random",
				"./Random/x.pl #Random",
				"./Random/Manual/perl-in-perl.pl #Manual #Random",
				"./.pl/perl-in-perl.pl #Manual #Random",
				"./.pl/x.pl #Random",
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
			fs.rename(
				"/Manual/perl-in-perl.pl #Manual #Random",
				"/Manual/perl-in-english.man");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Manual/perl-in-english.man #Manual #Random",
				"./Random/perl-in-english.man #Manual #Random",
				"./Random/x.pl #Random",
				"./Random/Manual/perl-in-english.man #Manual #Random",
				"./Random/.man/perl-in-english.man #Manual #Random",
				"./Random/.pl/x.pl #Random",
				"./.man/perl-in-english.man #Manual #Random",
				"./.pl/x.pl #Random",
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
