package groupfs.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.Map.Entry;
import java.util.Set;

import fuse.FuseException;

import groupfs.Permissions;
import groupfs.Directory;
import groupfs.Group.Type;
import groupfs.Group;
import groupfs.Path;
import groupfs.Inode;

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

	public SubDirectory(Manager backend, BaseDirectory parent, Group group) {
		super(backend);
		this.parent = parent;
		this.group = group;
	}

	public SubDirectory(Manager backend, BaseDirectory parent, Group group, NameMapper mapper) {
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
			for (Node n : pool)
				counter.consider(n);
			if (group.type != Type.MIME)
				for (Group g : counter.visibleGroups())
					mapper.map(backend.directoryInstance(this, g));
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
