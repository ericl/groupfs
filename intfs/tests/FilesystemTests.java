package intfs.tests;

public class FilesystemTests {
	protected boolean error;

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
		});
		if (tests.error)
			System.err.println("One or more tests FAILED.");

	}

	public void runTests(Test[] tests) {
		for (Test test : tests) {
			System.out.println("Running test: " + test);
			test.run();
			if (test.error) {
				System.out.println(test.log);
				error = true;
			}
		}
	}
}
