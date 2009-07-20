package groupfs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import groupfs.backend.Node;
import groupfs.QueryGroup.Type;

public class NameMapper {
	private Map<String,View> views = new HashMap<String,View>();
	private Map<QueryGroup,String> dirs = new HashMap<QueryGroup,String>();
	private Map<Node,String> files = new HashMap<Node,String>();

	private Map<String,View> views_ro = Collections.unmodifiableMap(views);
	private Set<Node> files_keyset_ro = Collections.unmodifiableSet(files.keySet());
	private Set<QueryGroup> dirs_keyset_ro = Collections.unmodifiableSet(dirs.keySet());

	public void clear() {
		views.clear();
		dirs.clear();
		files.clear();
	}

	public Map<String,View> viewMap() {
		return views_ro;
	}

	public Set<Node> getPool() {
		return files_keyset_ro;
	}

	public Set<QueryGroup> getMappedGroups() {
		return dirs_keyset_ro;
	}

	public void map(JournalingDirectory dir) {
		QueryGroup group = dir.getGroup();
		String key = group.getValue();
		if (group.getType() == Type.MIME)
			key = "." + key;
		if (views.containsKey(key)) {
			int num = 0;
			while (views.containsKey(key + "." + num))
				num++;
			key += "." + num;
		}
		views.put(key, dir);
		assert !dirs.containsKey(group);
		dirs.put(group, key);
	}

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

	public boolean contains(Node file) {
		return files.containsKey(file);
	}

	public boolean contains(QueryGroup dir) {
		return dirs.containsKey(dir);
	}

	public void unmap(Node file) {
		String key = files.get(file);
		files.remove(file);
		views.remove(key);
	}

	public void unmap(QueryGroup group) {
		String key = dirs.get(group);
		dirs.remove(group);
		views.remove(key);
	}

	public int count(QueryGroup group) {
		String key = dirs.get(group);
		if (key == null)
			return -1;
		JournalingDirectory dir = (JournalingDirectory)views.get(key);
		return dir.getPoolDirect().size();
	}
}
