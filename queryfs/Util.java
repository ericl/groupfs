package queryfs;

import java.io.File;
import java.io.IOException;

import java.util.Set;

import queryfs.QueryGroup.Type;

public class Util {
	public static String extensionOf(File file) {
		return extensionOf(file.getName());
	}

	public static String extensionOf(String name) {
		if (!name.contains(".") || name.endsWith("."))
			return null;
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
		File dir = new File(path + "/" + name);
		if (!dir.isDirectory())
			throw new IOException("Destination path exists as a file.");
		else if (!dir.exists())
			if (!dir.mkdirs())
				throw new IOException("Could not create destination path.");
		File dest = new File(path + "/" + name);
		if (dest.exists()) {
			String ext = "";
			String base = name;
			String[] parts = name.split("\\.");
			if (parts.length > 1) {
				ext = "." + parts[parts.length-1];
				base = name.substring(0, name.length() - ext.length());
			}
			int counter = 0;
			while (dest.exists()) {
				dest = new File(path + "/" + base + "." + counter + ext);
				counter++;
			}
		}
		return dest;
	}
}
