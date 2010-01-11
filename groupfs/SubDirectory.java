package groupfs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.Map.Entry;
import java.util.Set;

import fuse.FuseException;

import groupfs.Group.Type;

import groupfs.backend.DataProvider;
import groupfs.backend.Node;

import static groupfs.Util.*;

public class SubDirectory extends BaseDirectory {
	protected BaseDirectory parent;
	protected Group group;

	protected static Permissions class_tag_perms = new Permissions(
		true, true, true, true, true, true, true
	);
	protected static Permissions class_mime_perms = new Permissions(
		false, false, false, false, true, false, true
	);

	public SubDirectory(DataProvider backend, BaseDirectory parent, Group group) {
		super(backend);
		this.parent = parent;
		this.group = group;
	}

	public SubDirectory(DataProvider backend, BaseDirectory parent, Group group, NameMapper mapper) {
		super(backend);
		this.parent = parent;
		this.group = group;
		this.mapper = mapper;
	}

	public Permissions getPerms() {
		if (group.type == Type.MIME)
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
		Set<Group> del = new HashSet<Group>();
		del.add(group);
		if (getPool().isEmpty()) {
			parent.mapper.unmap(group);
		} else for (Inode v : mapper.inodeMap().values()) {
			if (v instanceof Node) {
				Node n = (Node)v;
				n.changeGroups(null, del);
			}
		}
		return 0;
	}

	private Set<Node> filter(Group x, Set<Node> all) {
		Set<Node> pool = new HashSet<Node>();
		for (Node n : all)
			if (n.getGroups().contains(x))
				pool.add(n);
		return pool;
	}

	protected boolean populateSelf() {
		if (!populated) {
			Set<Node> pool = filter(group, parent.getPool());
			Set<Group> output = new HashSet<Group>();
			Set<Group> groups = getGroups();
			if (group.type != Type.MIME) {
				Map<Group,Integer> gcount = new HashMap<Group,Integer>();
				for (Node node : pool) {
					for (Group group : node.getGroups()) {
						Integer n = gcount.get(group);
						gcount.put(group, n == null ? 1 : n + 1);
					}
				}
				for (Entry<Group,Integer> entry : gcount.entrySet()) {
					Group g = entry.getKey();
					if (!groups.contains(g) && (groups.isEmpty() || entry.getValue() < pool.size()))
						output.add(g);
				}
			}
			for (Group group : output)
				mapper.map(backend.get(this, group));
			for (Node node : pool)
				mapper.map(node);
			populated = true;
			time = System.currentTimeMillis();
			head = backend.journal.head(); // we're up to date
			return true;
		} else {
			return false;
		}
	}

	private boolean fromEqualsThis(String from) {
		Set<Group> fromGroups = new HashSet<Group>();
		for (String tag : tagsOf(from))
			fromGroups.add(Group.create(tag, Type.TAG));
		return fromGroups.equals(getGroups());
	}

	public int rename(Path from, Path to, Inode target, Directory orig, Directory dest) throws FuseException {
		assert fromEqualsThis(from.value);
		if (target != null)
			return fuse.Errno.EPERM;
		if (group.type == Type.MIME)
			return fuse.Errno.EPERM;
		if (getPool().isEmpty()) {
			this.parent.mapper.unmap(group);
			// reassigning these directly is ok
			// because empty dirs still around are
			// not created cached
			this.group = Group.create(to.name(), Type.TAG);
			this.parent = (BaseDirectory)dest;
			this.parent.mapper.map(this);
			return 0;
		}
		Set<Group> add = new HashSet<Group>();
		add.addAll(dest.getGroups());
		add.add(Group.create(to.name(), Type.TAG));
		Set<Group> groups = getGroups();
		for (Node n : getPool())
			n.changeGroups(add, groups);
		return 0;
	}

	public Group getGroup() {
		return group;
	}

	public Directory getParent() {
		return parent;
	}
}
