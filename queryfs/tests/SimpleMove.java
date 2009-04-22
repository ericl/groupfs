package queryfs.tests;

import java.util.Arrays;

import fuse.FuseException;

import queryfs.*;

import queryfs.backend.*;

public class SimpleMove extends Test {
	public void run() {
		QueryBackend backend = new TestingBackend();
		syn(backend, "sed.txt", "Manual", "Readable");
		syn(backend, "awk.txt", "Manual", "Readable");
		syn(backend, "perl-in-perl.pl", "Manual");
		Filesystem fs = new Filesystem(backend);
		expect(fs,
			Arrays.asList(new String[] {
				"./.txt/sed.txt",
				"./.txt/awk.txt",
				"./Manual/perl-in-perl.pl",
				"./Manual/sed.txt",
				"./Manual/.txt/sed.txt",
				"./Manual/.txt/awk.txt",
				"./Manual/awk.txt",
				"./Manual/.pl/perl-in-perl.pl",
				"./Manual/Readable/sed.txt",
				"./Manual/Readable/awk.txt",
				"./.pl/perl-in-perl.pl",
				"./Readable/sed.txt",
				"./Readable/awk.txt",
			}),
			Arrays.asList(new String[] {
				".",
				"./.txt",
				"./Manual",
				"./Manual/.txt",
				"./Manual/.pl",
				"./Manual/Readable",
				"./.pl",
				"./Readable",
			})
		);
		try {
			// I declare perl readable
			fs.rename("/Manual/perl-in-perl.pl", "/Manual/Readable/perl-in-perl.pl");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			Arrays.asList(new String[] {
				"./.txt/sed.txt",
				"./.txt/awk.txt",
				"./Manual/perl-in-perl.pl",
				"./Manual/sed.txt",
				"./Manual/.txt/sed.txt",
				"./Manual/.txt/awk.txt",
				"./Manual/awk.txt",
				"./Manual/.pl/perl-in-perl.pl",
				"./.pl/perl-in-perl.pl",
				"./Readable/perl-in-perl.pl",
				"./Readable/sed.txt",
				"./Readable/.txt/sed.txt",
				"./Readable/.txt/awk.txt",
				"./Readable/awk.txt",
				"./Readable/.pl/perl-in-perl.pl",
			}),
			Arrays.asList(new String[] {
				".",
				"./.txt",
				"./Manual",
				"./Manual/.txt",
				"./Manual/.pl",
				"./.pl",
				"./Readable",
				"./Readable/.txt",
				"./Readable/.pl",
			})
		);
	}
}
