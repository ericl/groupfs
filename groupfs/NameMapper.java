package groupfs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import groupfs.QueryGroup.Type;

import groupfs.backend.DataProvider;
import groupfs.backend.Node;

/**
 * Maps "filenames" to their backing data objects in each directory.
 * The following operations are guaranteed fast:
 *   Mapping a file or directory.
 *   Unmapping a file or group.
 *   Looking up a name.
 *   Looking up all groups of child directories.
 *   Looking up all groups of file nodes.
 */
public class NameMapper {
	protected DataProvider backend;
	private Map<String,View> views = new HashMap<String,View>();
	private Map<QueryGroup,String> dirs = new HashMap<QueryGroup,String>();
	private Map<Node,String> files = new HashMap<Node,String>();

	private Map<String,View> views_ro = Collections.unmodifiableMap(views);
	private Set<Node> files_keyset_ro = Collections.unmodifiableSet(files.keySet());
	private Set<QueryGroup> dirs_keyset_ro = Collections.unmodifiableSet(dirs.keySet());

	public NameMapper(DataProvider backend) {
		this.backend = backend;
	}

	/**
	 * @return	Map of filenames to data views.
	 */
	public Map<String,View> viewMap() {
		return views_ro;
	}

	/**
	 * @return	All groups represented by child directories.
	 */
	public Set<QueryGroup> getGroups() {
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
	public void map(JournalingDirectory dir) {
		QueryGroup group = dir.getGroup();
		String key = group.getValue();
		assert !getGroups().contains(group);
		if (group.getType() == Type.MIME)
			key = "." + key;
		assert !views.containsKey(key);
		views.put(key, dir);
		dirs.put(group, key);
	}

	/**
	 * Maps file, automatically resolving name collisions.
	 * @param	file	Not null.
	 */
	public void map(Node file) {
		String key = file.getName();
		if (views.containsKey(key)) {
			int num = 0;
			while (views.containsKey(key + "." + num))
				num++;
			key += "." + num;
		}
		views.put(key, file);
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
	public boolean contains(QueryGroup dir) {
		return dirs.containsKey(dir);
	}

	/**
	 * @param	file	Any value.
	 */
	public void unmap(Node file) {
		String key = files.get(file);
		files.remove(file);
		views.remove(key);
	}

	/**
	 * @param	group	Any value.
	 */
	public void unmap(QueryGroup group) {
		String key = dirs.get(group);
		dirs.remove(group);
		View dir = views.get(key);
		// directory caches are dropped here
		if (dir != null && dir instanceof JournalingDirectory)
			backend.drop(((JournalingDirectory)dir).getQueryGroups());
		views.remove(key);
	}

	/**
	 * @return	Unsynchronized size of file pool of any child group,
	 *			or -1 if no such group exists.
	 * @param	group	Any value.
	 */
	public int count(QueryGroup group) {
		String key = dirs.get(group);
		if (key == null)
			return -1;
		JournalingDirectory dir = (JournalingDirectory)views.get(key);
		return dir.getPoolDirect().size();
	}
}
