package groupfs.tests;

public class FilesystemTests {
	protected boolean error;
	public final static boolean SHOWERR = true;

	public static void main(String[] args) {
		FilesystemTests tests = new FilesystemTests();
		tests.runTests(new Test[] {
			new SimpleCreate(),
			new SimpleMove(),
			new SimpleUnlink(),
			new MovingEmptyDirs(),
			new FileRenaming(),
			new ExtensionRenaming(),
			new MoveDirectory(),
			new StrangeMovements(),
			new DuplicateHandling(),
			new LargeScaleValidation(),
			new HoldNewFilesOpen(),
			new LargeScale2(),
			new PermissiveWrites(),
			new UnTrash(),
			new MkdirRenamed(),
		});
		if (tests.error) {
			System.err.println("One or more tests FAILED.");
			System.exit(1);
		}
	}

	public void runTests(Test[] tests) {
		for (Test test : tests) {
			if (SHOWERR)
				System.out.println("Running test: " + test);
			test.run();
			if (test.error) {
				if (SHOWERR)
					System.out.println(test.log);
				else
					System.out.println("FAIL " + test);
				error = true;
			} else if (!SHOWERR) {
				System.out.println("OK   " + test);
			}
		}
	}
}
