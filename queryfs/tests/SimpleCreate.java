package queryfs.tests;

import queryfs.*;

import queryfs.backend.*;

public class SimpleCreate extends Test {
	public void run() {
		QueryBackend backend = new TestingBackend();
		syn(backend, "sed.txt", "Manual", "Readable");
		syn(backend, "Random Book.txt", "Book", "Readable");
		syn(backend, "trashed.txt");
		Filesystem fs = new Filesystem(backend);

		assertExists(fs,
			"/Manual/sed.txt",
			"/Book/Random Book.txt",
			"/Readable/sed.txt",
			"/Readable/Random Book.txt",
			"/.txt/sed.txt",
			"/.txt/Random Book.txt",
			"/.txt/trashed.txt",
			"/.Trash/trashed.txt"
		);

		assertMissing(fs,
			"/Manual/Random Book.txt",
			"/.Trash/Random Book.txt",
			"/Book/sed.txt",
			"/Book/Book",
			"/Book/Readable"
		);
	}
}
