package groupfs.tests;

import groupfs.storage.*;
import groupfs.state.Manager;

import fuse.FuseException;

import groupfs.*;

// directory rename
// directory move in
public class MoveDirectory extends Test {
	public void run() {
		Manager backend = getNewBackend();
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
				"./Perl/perl-in-perl.pl #Perl",
				"./.pl/perl-in-perl.pl #Perl",
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
				"./.pl/perl-in-perl.pl #Manual #Perl",
				"./.sh/bash-in-bash.sh #Bash #Manual",
				"./Bash/bash-in-bash.sh #Bash #Manual",
				"./Manual/.pl/perl-in-perl.pl #Manual #Perl",
				"./Manual/.sh/bash-in-bash.sh #Bash #Manual",
				"./Manual/Bash/bash-in-bash.sh #Bash #Manual",
				"./Manual/Perl/perl-in-perl.pl #Manual #Perl",
				"./Manual/bash-in-bash.sh #Bash #Manual",
				"./Manual/perl-in-perl.pl #Manual #Perl",
				"./Perl/perl-in-perl.pl #Manual #Perl",
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
