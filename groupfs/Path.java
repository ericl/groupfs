package groupfs;

import java.util.Stack;

public final class Path {
	public final String value;

	private Path(String path, boolean parseNeeded) {
		if (parseNeeded) {
			this.value = parse(path).intern();
			assert value.equals(reference_parse(path))
				: "\"" + value + "\" vs \"" + reference_parse(path) + "\"";
		} else {
			this.value = path;
		}
	}

	public Path parent() {
		return new Path(parent(value), false);
	}

	public static Path get(String path) {
		return new Path(path, true);
	}

	public String name() {
		return name(value);
	}

	public boolean equals(Object other) {
		return other instanceof Path && value.equals(((Path)other).value);
	}

	public int hashCode() {
		return value.hashCode();
	}

	private static String parse(String path) {
		final int len = path.length();
		final char[] output = new char[len+2];
		output[0] = '/';
		int index = 1, last = 1;
		for (int i=0; i < len + 1; i++) {
			if (i == len || path.charAt(i) == '/') {
				int diff = index - last;
				if (diff == 0) {
					continue;
				} else if (diff == 1 && output[last] == '.') {
					index = last - 1;
					output[index++] = '/';
				} else if (diff == 2 && output[last] == '.' && output[last+1] == '.') {
					index = last - 1;
					while (index > 0) {
						if (output[--index] == '/')
							break;
					}
					output[index++] = '/';
				} else {
					output[index++] = '/';
					last = index;
				}
			} else {
				output[index++] = path.charAt(i);
			}
		}
		while (index > 1 && output[index-1] == '/')
			index--;
		return new String(output, 0, index);
	}

	private static String reference_parse(String path) {
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

	private static String parent(String path) {
		int index = path.lastIndexOf('/');
		if (index < 1)
			return "/";
		else
			return path.substring(0, index);
	}

	private static String name(String path) {
		int index = path.lastIndexOf('/');
		if (index < 1)
			return path.substring(1);
		else
			return path.substring(index + 1);
	}
}
