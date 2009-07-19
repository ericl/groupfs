package groupfs.tests;

public class Benchmark {
	protected boolean error;

	public static void main(String[] args) {
		Benchmark tests = new Benchmark();
		tests.runTests(new Test[] {
			new ReadOnly(),
		});
		if (tests.error) {
			System.err.println("One or more benchmarks FAILED.");
			System.exit(1);
		}
	}

	public void runTests(Test[] tests) {
		for (Test test : tests) {
			System.out.println("Running benchmark: " + test);
			test.run();
			if (test.error) {
				System.out.println(test.log);
				error = true;
			}
		}
	}
}
