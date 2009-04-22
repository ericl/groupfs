package queryfs.tests;

import java.util.Arrays;

import queryfs.*;

import queryfs.backend.*;

public class SimpleCreate extends Test {
	public void run() {
		QueryBackend backend = new TestingBackend();
		syn(backend, "sed.txt", "Manual", "Readable");
		syn(backend, "Random Book.txt", "Book", "Readable");
		syn(backend, "trashed.txt");
		Filesystem fs = new Filesystem(backend);
		expect(fs,
			Arrays.asList(new String[] {
				"./Book/Random Book.txt",
				"./.txt/sed.txt",
				"./.txt/Random Book.txt",
				"./.Trash/trashed.txt",
				"./Manual/sed.txt",
				"./Readable/Book/Random Book.txt",
				"./Readable/sed.txt",
				"./Readable/Manual/sed.txt",
				"./Readable/Random Book.txt",
			}),
			Arrays.asList(new String[] {
				".",
				"./Book",
				"./.txt",
				"./.Trash",
				"./Manual",
				"./Readable",
				"./Readable/Book",
				"./Readable/Manual",
			})
		);
	}
}
