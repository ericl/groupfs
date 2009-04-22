package queryfs.tests;

public class FilesystemTests {
	protected boolean error;

	public static void main(String[] args) {
		FilesystemTests tests = new FilesystemTests();
		tests.runTests(
			new SimpleCreate()
		);
		if (tests.error)
			System.err.println("One or more tests FAILED");
		else
			System.out.println("All tests succeeded.");

	}

	public void runTests(Test ... tests) {
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
