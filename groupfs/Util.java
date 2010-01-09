package groupfs;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.sun.security.auth.module.UnixSystem;

import groupfs.Group.Type;

public final class Util {
	public final static int UID = (int)new UnixSystem().getUid();
	public final static int GID = (int)new UnixSystem().getGid();

	private Util() {}

	public static void validate(File origin, File mount) throws IOException {
		if (!origin.exists() || !origin.isDirectory())
			throw new IllegalArgumentException("Origin is invalid.");
		if (!mount.exists() || !mount.isDirectory())
			throw new IllegalArgumentException("Mount point is invalid.");
		String op = origin.getCanonicalPath();
		String mp = mount.getCanonicalPath();
		if (!op.endsWith("/"))
			op += "/";
		if (!mp.endsWith("/"))
			mp += "/";
		if (mp.startsWith(op) || op.startsWith(mp))
			throw new IllegalArgumentException("Mount point overlaps origin.");
	}

	public static boolean allUnique(Path path) {
		List<String> tags = Arrays.asList(path.value.split("/"));
		Set<String> tmp = new HashSet<String>();
		for (String tag : tags) {
			if (tmp.contains(tag))
				return false;
			tmp.add(tag);
		}
		return true;
	}

	public static void rmdirs(File f) {
		if (f == null || !f.isDirectory() || f.list().length != 0)
			return;
		f.delete();
		rmdirs(f.getParentFile());
	}

	public static Set<String> tagsOf(String parent) {
		Set<String> tags = new HashSet<String>(
			Arrays.asList(parent.split("/"))
		);
		tags.remove("");
		return tags;
	}

	public static Set<Group> groupsOf(Path path) {
		Set<Group> groups = new HashSet<Group>();
		for (String tag : tagsOf(path.parent().value))
			groups.add(Group.create(tag, Type.TAG));
		String ext = extensionOf(path.name());
		if (ext != null)
			groups.add(Group.create(ext, Type.MIME));
		return groups;
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

	public static String newPath(File root, Set<Group> groups) {
		String path = root.getAbsolutePath();
		for (Group group : groups)
			if (group.type == Type.TAG)
				path += "/" + group.value;
		return path;
	}

	public static boolean hasCategory(Set<Group> groups) {
		if (groups != null) {
			for (Group group : groups)
				if (group.type != Type.MIME)
					return true;
		}
		return false;
	}

	public static boolean hasMime(Set<Group> groups) {
		if (groups != null) {
			for (Group group : groups)
				if (group.type == Type.MIME && group != Group.GROUP_NO_GROUP)
					return true;
		}
		return false;
	}

	public static boolean maxOneMimeGroup(Set<Group> groups) {
		int count = 0;
		for (Group q : groups)
			if (q.type == Type.MIME)
			// GROUP_NO_GROUP counts too - trashed files must not show up elsewhere
				count++;
		return count <= 1;
	}

	public static File getDestination(String path, String name) throws IOException {
		File dir = new File(path);
		if (dir.exists() && !dir.isDirectory())
			throw new IOException("Destination path exists as a file.");
		else if (!dir.exists())
			if (!dir.mkdirs())
				throw new IOException("Could not create destination path " + dir.getAbsolutePath());
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
