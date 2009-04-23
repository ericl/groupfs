package queryfs.tests;

import queryfs.backend.*;

import fuse.FuseException;

import queryfs.*;

// directory rename
// directory move in
public class MoveDirectory extends Test {
	public void run() {
		QueryBackend backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.rename("/Manual", "/Perl");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Perl/perl-in-perl.pl",
				"./.pl/perl-in-perl.pl",
			},
			new String[] {
				".",
				"./.pl",
				"./Perl",
			}
		);
		backend = getNewBackend();
		syn(backend, "perl-in-perl.pl", "Manual", "Perl");
		syn(backend, "bash-in-bash.sh", "Bash");
		fs = new Filesystem(backend);
		try {
			fs.rename("/Bash", "/Manual/Bash");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./.pl/perl-in-perl.pl",
				"./.sh/bash-in-bash.sh",
				"./Bash/bash-in-bash.sh",
				"./Manual/.pl/perl-in-perl.pl",
				"./Manual/.sh/bash-in-bash.sh",
				"./Manual/Bash/bash-in-bash.sh",
				"./Manual/Perl/perl-in-perl.pl",
				"./Manual/bash-in-bash.sh",
				"./Manual/perl-in-perl.pl",
				"./Perl/perl-in-perl.pl",
			},
			new String[] {
				".",
				"./.pl",
				"./.sh",
				"./Bash",
				"./Perl",
				"./Manual",
				"./Manual/.pl",
				"./Manual/.sh",
				"./Manual/Bash",
				"./Manual/Perl",
			}
		);
	}
}
