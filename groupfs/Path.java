package groupfs;

import java.io.File;

import java.util.Stack;

final class Path {
	public final String value;
	private String name;
	private Path parent;

	private static String parse(String path) {
		String[] parts = path.split("/");
		Stack<String> stack = new Stack<String>();
		for (int i=0; i < parts.length; i++) {
			String part = parts[i];
			if (part.equals("") || part.equals(".")) {
				// the same node
			} else if (part.equals("..")) {
				if (!stack.isEmpty())
					stack.pop();
			} else {
				stack.push(part);
			}
		}
		String ret = "";
		if (stack.isEmpty())
			ret = "/";
		else
			for (String part : stack)
				ret += "/" + part;
		return ret;
	}

	private Path(String path) {
		this.value = parse(path).intern();
	}

	public static Path get(String path) {
		return new Path(path);
	}

	public Path parent() {
		if (parent != null)
			return parent;
		return parent = Path.get(new File(value).getParent());
	}

	public String name() {
		if (name != null)
			return name;
		return name = new File(value).getName();
	}

	public boolean equals(Object other) {
		return other instanceof Path && value.equals(((Path)other).value);
	}

	public int hashCode() {
		return value.hashCode();
	}
}

