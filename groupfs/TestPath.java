package groupfs;

public class TestPath {
	static volatile Path p;
	public static void main(String[] args) {
		String simple = "/usr/share/icons/foo.png";
		String complex = "//usr/share////icons/../themes/Dust Sand/index.theme";
		/* JIT warmup */
		for (int i=0; i < 1e5; i++) {
			p = Path.get(simple);
		}

		long start = System.nanoTime();
		for (int i=0; i < 1e5; i++) {
			p = Path.get(simple);
		}
		long end = System.nanoTime();
		System.out.println("microseconds per parse: " + (end-start)/1e3/1e5);
	}
}
