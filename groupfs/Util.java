package groupfs;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Set;

import com.sun.security.auth.module.UnixSystem;

import groupfs.Group.Type;

public final class Util {
	public final static int UID = (int)new UnixSystem().getUid();
	public final static int GID = (int)new UnixSystem().getGid();

	private Util() {}

	/**
	 * Checks that the mountpoint and origin directories exist and
	 * do not overlap. This would be very bad if the DirectoryFileSource
	 * was being used.
	 */
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

	public static boolean isReservedName(String name) {
		return name.startsWith(".");
	}

	/**
	 * @return True if all groups represented by a path are unique.
	 */
	public static boolean allUnique(Path path) {
		List<String> list = Arrays.asList(path.value.split("/"));
		Set<String> set = new HashSet<String>(list);
		return set.size() == list.size();
	}

	/**
	 * Recursively removes directories upwards until a non-empty one is found.
	 * @param f Any value.
	 */
	public static void rmdirs(File f) {
		if (f == null || !f.isDirectory() || f.list().length != 0)
			return;
		f.delete();
		rmdirs(f.getParentFile());
	}

	/**
	 * @return Set of all tags represented by a path.
	 */
	public static Set<String> tagsOf(String parent) {
		Set<String> tags = new HashSet<String>(
			Arrays.asList(parent.split("/"))
		);
		tags.remove("");
		return tags;
	}

	/**
	 * @return All groups of a path.
	 */
	public static Set<Group> groupsOf(Path path) {
		Set<Group> groups = new HashSet<Group>();
		for (String tag : tagsOf(path.parent().value))
			groups.add(Group.create(tag, Type.TAG));
		String ext = extensionOf(path.name());
		if (ext != null)
			groups.add(Group.create(ext, Type.MIME));
		return groups;
	}

	/**
	 * @param name String of any form.
	 * @return "{name}" if name matches "{name}.[0-9]+"
	 */
	public static String unNumbered(String name) {
		if (name.matches(".*\\.[0-9]+"))
			name = name.substring(0, name.lastIndexOf("."));
		return name;
	}

	/**
	 * @param name Non-null string of any form.
	 * @return "{ext}" if unNumbered(name) matches "name.{ext}" else "undefined"
	 */
	public static String extensionOf(String name) {
		name = unNumbered(name);
		if (!name.contains(".") || name.endsWith("."))
			return "undefined";
        if (name.startsWith("."))
            return "dotfiles";
		String[] parts = name.split("\\.");
		return parts[parts.length-1].toLowerCase();
	}

	/**
	 * @param root The root directory from which to build a path.
	 * @param groups Groups to be represented in a new path.
	 * @return Path of directory that represents groups.
	 */
	public static String newPath(File root, Set<Group> groups) {
		String path = root.getAbsolutePath();
		for (Group group : groups)
			if (group.type == Type.TAG)
				path += "/" + group.value;
		return path;
	}

	/**
	 * @return True if groups has a group of type "tag".
	 * @param groups Any value.
	 * A return value of false means that groups is inconsistent.
	 */
	public static boolean hasCategory(Set<Group> groups) {
		if (groups != null) {
			for (Group group : groups)
				if (group.type != Type.MIME)
					return true;
		}
		return false;
	}

	/**
	 * @return True if groups has a group of type "mime".
	 * @param groups Any value.
	 * A return value of false means that groups is inconsistent.
	 */
	public static boolean hasMime(Set<Group> groups) {
		if (groups != null) {
			for (Group group : groups)
				if (group.type == Type.MIME && group != Group.GROUP_NO_GROUP)
					return true;
		}
		return false;
	}

	/**
	 * @return True if groups has has at max one group of type "mime".
	 * A return value of false means that groups is inconsistent.
	 */
	public static boolean maxOneMimeGroup(Set<Group> groups) {
		int count = 0;
		for (Group q : groups)
			if (q.type == Type.MIME)
			// GROUP_NO_GROUP counts too - trashed files must not show up elsewhere
				count++;
		return count <= 1;
	}

	/**
	 * @return An unoccupied file path such that name == unNumbered(path.getName())
	 * @param name Name of file such that name == unNumbered(name)
	 * @param path Directory name the new file path should be in.
	 */
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

	/**
	 * @return Filename modified to have no hash tags.
	 * @param name Name of file.
	 */
	public static String stripHashTags(String name) {
		return name.split(" #")[0];
	}

	/**
	 * @return Filename modified to have hash tags as specified by groups.
	 * @param name Name of file.
	 */
	public static String recomputeHashTags(String name, Set<Group> groups) {
		String base = name.split(" #")[0];
		for (Group g : new TreeSet<Group>(groups)) {
			if (g.type == Type.TAG) {
				base += " #" + g.value;
			}
		}
		return base;
	}

	/**
	 * @return The set of groups represented by the specified filename.
	 * @param name Name of file.
	 */
	public static Set<Group> groupsFromHashTags(String name) {
		String[] tags = name.split(" #", 2);
		Set<Group> groupsFound = new HashSet<Group>();
		if (tags.length > 1) {
			assert tags.length == 2;
			for (String tag : tags[1].split(" #")) {
				if (!tag.isEmpty()) {
					groupsFound.add(Group.create(tag, Type.TAG));
				}
			}
		}
		String ext = extensionOf(name);
		if (ext != null) {
			groupsFound.add(Group.create(ext, Type.MIME));
		}
		return groupsFound;
	}
}
