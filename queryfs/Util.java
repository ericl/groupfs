package queryfs;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import queryfs.QueryGroup.Type;

public class Util {
	public static String extensionOf(File file) {
		return extensionOf(file.getName());
	}

	public static Set<String> tagsOf(String parent) {
		Set<String> tags = new HashSet<String>(
			Arrays.asList(parent.split("/"))
		);
		tags.remove("");
		return tags;
	}

	public static String unNumbered(String name) {
		if (name.matches(".*\\.[0-9]+"))
			name = name.substring(0, name.lastIndexOf("."));
		return name;
	}

	public static String extensionOf(String name) {
		if (name.matches(".*\\.[0-9]+"))
			name = name.substring(0, name.lastIndexOf("."));
		if (!name.contains(".") || name.endsWith("."))
			return "undefined";
		String[] parts = name.split("\\.");
		return parts[parts.length-1].toLowerCase();
	}

	public static String newPath(File root, Set<QueryGroup> groups) {
		String path = root.getAbsolutePath();
		for (QueryGroup group : groups)
			if (group.getType() == Type.TAG)
				path += "/" + group.getValue();
		return path;
	}

	public static File getDestination(String path, String name) throws IOException {
		File dir = new File(path);
		if (dir.exists() && !dir.isDirectory())
			throw new IOException("Destination path exists as a file.");
		else if (!dir.exists())
			if (!dir.mkdirs())
				throw new IOException("Could not create destination path.");
		File dest = new File(path + "/" + name);
		if (dest.exists()) {
			int counter = 0;
			while (dest.exists()) {
				dest = new File(path + "/" + name + "." + counter);
				counter++;
			}
		}
		return dest;
	}
}
