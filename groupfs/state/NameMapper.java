package groupfs.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import groupfs.Group.Type;
import groupfs.Group;
import groupfs.Inode;
import groupfs.Directory;

import static groupfs.Util.*;

/**
 * Maps "filenames" to their backing inodes in each directory.
 * The following operations are guaranteed fast:
 *   Mapping a file or directory.
 *   Unmapping a file or group.
 *   Looking up a name.
 *   Looking up all groups of child directories.
 *   Looking up all groups of file nodes.
 */
public class NameMapper {
	protected Manager backend;
	private Map<String,Inode> names = new HashMap<String,Inode>();
	private Map<Group,String> dirs = new HashMap<Group,String>();
	private Map<Node,String> files = new HashMap<Node,String>();

	private Map<String,Inode> names_ro = Collections.unmodifiableMap(names);
	private Set<Node> files_keyset_ro = Collections.unmodifiableSet(files.keySet());
	private Set<Group> dirs_keyset_ro = Collections.unmodifiableSet(dirs.keySet());

	public NameMapper(Manager backend) {
		this.backend = backend;
	}

	/**
	 * @return	Map of filenames to inodes.
	 */
	public Map<String,Inode> inodeMap() {
		return names_ro;
	}

	/**
	 * @return	All groups represented by child directories.
	 */
	public Set<Group> getGroups() {
		return dirs_keyset_ro;
	}

	/**
	 * @return All children file nodes.
	 */
	public Set<Node> getPool() {
		return files_keyset_ro;
	}

	/**
	 * Maps directory as child.
	 * No two directories with the same group can be mapped.
	 * @param	dir	Not null.
	 */
	public void map(BaseDirectory dir) {
		Group group = dir.getGroup();
		String key = group.value;
		assert !getGroups().contains(group);
		if (group.type == Type.MIME)
			key = "." + key;
		assert !names.containsKey(key);
		names.put(key, dir);
		dirs.put(group, key);
	}

	/**
	 * Maps file, automatically resolving name collisions.
	 * @param	file	Not null.
	 */
	public void map(Node file) {
		String key = file.getName();
		if (names.containsKey(key)) {
			int num = 0;
			while (names.containsKey(key + "." + num))
				num++;
			key += "." + num;
		}
		key = recomputeHashTags(key, file.getGroups());
		names.put(key, file);
		assert !files.containsKey(file);
		files.put(file, key);
	}

	/**
	 * @return	True if the file is mapped.
	 * @param	file	Any value.
	 */
	public boolean contains(Node file) {
		return files.containsKey(file);
	}

	/**
	 * @return	True if the group is represented by any mapped directory.
	 * @param	dir	Any value.
	 */
	public boolean contains(Group dir) {
		return dirs.containsKey(dir);
	}

	/**
	 * @param	file	Any value.
	 */
	public void unmap(Node file) {
		String key = files.get(file);
		files.remove(file);
		names.remove(key);
	}

	/**
	 * @param	group	Any value.
	 */
	public void unmap(Group group) {
		String key = dirs.get(group);
		dirs.remove(group);
		Inode dir = names.get(key);
		// directory caches are dropped here
		if (dir != null && dir instanceof BaseDirectory)
			backend.drop(((BaseDirectory)dir).getGroups());
		names.remove(key);
	}

	/**
	 * @return	Unsynchronized size of file pool of any child group,
	 *			or -1 if no such group exists.
	 * @param	group	Any value.
	 */
	public int count(Group group) {
		String key = dirs.get(group);
		if (key == null)
			return -1;
		BaseDirectory dir = (BaseDirectory)names.get(key);
		return dir.getPoolDirect().size();
	}
}
