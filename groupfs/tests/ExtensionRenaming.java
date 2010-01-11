package groupfs.tests;

import groupfs.storage.*;
import groupfs.state.Manager;

import fuse.FuseException;

import groupfs.*;

// moving into mime dir
// moving out of mime dir
// delete from mime dir
// rename within mime dir
// rmdir mime dir -> fail
public class ExtensionRenaming extends Test {
	public void run() {
		Manager backend = getNewBackend();
		syn(backend, "perl.pl", "Manual");
		syn(backend, "doc.txt", "Manual");
		syn(backend, "doc2.txt", "Manual");
		Filesystem fs = new Filesystem(backend);
		try {
			fs.mkdir("/Perl", 0);
			int ret = fs.rename("/.pl/perl.pl", "/Perl/perl.pl");
			if (ret != 0)
				throw new FuseException(
					"\nexpected: " + 0 +
					"\nreturned: " + ret
				);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		try {
			int ret = fs.rename("/Manual/doc.txt","/Manual/.pl/doc.txt");
			if (ret != fuse.Errno.EPERM)
				throw new FuseException(
					"\nexpected: " + fuse.Errno.EPERM +
					"\nreturned: " + ret
				);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		try {
			fs.unlink("/.txt/doc2.txt");
			int ret = fs.rmdir("/.txt");
			if (ret != fuse.Errno.EPERM)
				throw new FuseException(
					"\nexpected: " + fuse.Errno.EPERM +
					"\nreturned: " + ret
				);
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Manual/perl.pl",
				"./Manual/doc.txt",
				"./Manual/Perl/perl.pl",
				"./Manual/.txt/doc.txt",
				"./Manual/.pl/perl.pl",
				"./Perl/perl.pl",
				"./.pl/perl.pl",
				"./.txt/doc.txt",
				"./.Trash/doc2.txt",
			},
			new String[] {
				".",
				"./.pl",
				"./.txt",
				"./.Trash",
				"./Manual",
				"./Manual/Perl",
				"./Perl",
				"./Manual/.txt",
				"./Manual/.pl",
			}
		);
		try {
			fs.rename("/.txt/doc.txt","/.txt/Official Document.pdf");
		} catch (FuseException e) {
			log += e;
			error = true;
			return;
		}
		expect(fs,
			new String[] {
				"./Manual/perl.pl",
				"./Manual/Official Document.pdf",
				"./Manual/Perl/perl.pl",
				"./Manual/.pdf/Official Document.pdf",
				"./Manual/.pl/perl.pl",
				"./Perl/perl.pl",
				"./.pl/perl.pl",
				"./.pdf/Official Document.pdf",
				"./.Trash/doc2.txt",
			},
			new String[] {
				".",
				"./.pl",
				"./.pdf",
				"./.Trash",
				"./Manual",
				"./Manual/Perl",
				"./Perl",
				"./Manual/.pdf",
				"./Manual/.pl",
			}
		);
	}
}
