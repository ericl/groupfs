package groupfs;

import java.io.File;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.Map.Entry;
import java.util.Set;

import fuse.FuseException;

import groupfs.QueryGroup.Type;

import groupfs.backend.JournalingBackend;
import groupfs.backend.Node;

import static groupfs.Util.*;

public class SubclassingDirectory extends JournalingDirectory {
	protected JournalingDirectory parent;
	protected QueryGroup group;

	protected static Permissions class_tag_perms = new Permissions(
		true, true, true, true, true, true, true
	);
	protected static Permissions class_mime_perms = new Permissions(
		false, false, false, false, true, false, true
	);

	public SubclassingDirectory(JournalingBackend backend, JournalingDirectory parent, QueryGroup group) {
		super(backend);
		this.parent = parent;
		this.group = group;
		raw_groups.addAll(parent.getQueryGroups());
		raw_groups.add(group);
	}

	public SubclassingDirectory(JournalingBackend backend, JournalingDirectory parent, QueryGroup group, NameMapper mapper) {
		super(backend);
		this.parent = parent;
		this.group = group;
		this.mapper = mapper;
		raw_groups.addAll(parent.getQueryGroups());
		raw_groups.add(group);
	}

	public Permissions getPerms() {
		if (group.getType() == Type.MIME)
			return class_mime_perms;
		else
			return class_tag_perms;
	}

	protected Set<Node> getPool() {
		update();
		return getPoolDirect();
	}

	protected Set<Node> getPoolDirect() {
		return mapper.getPool();
	}

	public int delete() throws FuseException {
		Set<QueryGroup> del = new HashSet<QueryGroup>();
		del.add(group);
		if (getPool().isEmpty()) {
			parent.mapper.unmap(group);
		} else for (View v : mapper.viewMap().values()) {
			if (v instanceof Node) {
				Node n = (Node)v;
				n.changeQueryGroups(null, del);
			}
		}
		return 0;
	}

	private Set<Node> filter(QueryGroup x, Set<Node> all) {
		Set<Node> pool = new HashSet<Node>();
		for (Node n : all)
			if (n.getQueryGroups().contains(x))
				pool.add(n);
		return pool;
	}

	protected void populateSelf() {
		Set<Node> pool = filter(group, parent.getPool());
		Set<QueryGroup> output = new HashSet<QueryGroup>();
		if (group.getType() != Type.MIME) {
			Map<QueryGroup,Integer> gcount = new HashMap<QueryGroup,Integer>();
			for (Node node : pool) {
				for (QueryGroup group : node.getQueryGroups()) {
					Integer n = gcount.get(group);
					gcount.put(group, n == null ? 1 : n + 1);
				}
			}
			for (Entry<QueryGroup,Integer> entry : gcount.entrySet()) {
				QueryGroup g = entry.getKey();
				if (!groups.contains(g) && (groups.isEmpty() || entry.getValue() < pool.size()))
					output.add(g);
			}
		}
		for (QueryGroup group : output)
			mapper.map(backend.get(this, group));
		for (Node node : pool)
			mapper.map(node);
	}

	private boolean fromEqualsThis(String from) {
		Set<QueryGroup> fromGroups = new HashSet<QueryGroup>();
		for (String tag : tagsOf(from))
			fromGroups.add(QueryGroup.create(tag, Type.TAG));
		return fromGroups.equals(groups);
	}

	public int rename(String from, String to, View target, Set<QueryGroup> hintRemove, Set<QueryGroup> hintAdd, Directory parent) throws FuseException {
		assert fromEqualsThis(from);
		if (target != null)
			return fuse.Errno.EPERM;
		if (group.getType() == Type.MIME)
			return fuse.Errno.EPERM;
		QueryGroup myGroup = QueryGroup.create(new File(to).getName(), Type.TAG);
		if (hintRemove.contains(myGroup))
			return fuse.Errno.EPERM;
		if (getPool().isEmpty()) {
			this.parent.getMapper().unmap(group);
			// reassigning these directly is ok
			// because empty dirs still around are
			// not created cached
			this.group = QueryGroup.create(Path.get(to).name(), Type.TAG);
			this.raw_groups.clear();
			this.raw_groups.add(this.group);
			this.raw_groups.addAll(parent.getQueryGroups());
			this.parent = (JournalingDirectory)parent;
			this.parent.getMapper().map(this);
			return 0;
		}
		Set<QueryGroup> add = new HashSet<QueryGroup>();
		for (String tag : tagsOf(to))
			add.add(QueryGroup.create(tag, Type.TAG));
		for (Node n : getPool())
			n.changeQueryGroups(add, groups);
		return 0;
	}

	public QueryGroup getGroup() {
		return group;
	}

	public Directory getParent() {
		return parent;
	}
}
