package groupfs;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fuse.FuseException;

import groupfs.QueryGroup.Type;
import groupfs.backend.QueryBackend;
import groupfs.backend.Node;

import static groupfs.Util.*;

public class SubclassingDirectory extends RootDirectory {
	private final Directory parent;
	private final QueryGroup group;
	protected static Permissions class_tag_perms = new Permissions(
		true, true, true, true, true, true, true
	);
	protected static Permissions class_mime_perms = new Permissions(
		false, false, false, false, true, false, true
	);

	public SubclassingDirectory(QueryBackend backend, Directory parent, QueryGroup group) {
		super(backend);
		this.parent = parent;
		this.group = group;
		raw_groups.addAll(parent.getQueryGroups());
		raw_groups.add(group);
	}

	public Permissions getPerms() {
		if (group.getType() == Type.MIME)
			return class_mime_perms;
		else
			return class_tag_perms;
	}

	public int delete() throws FuseException {
		Set<QueryGroup> del = new HashSet<QueryGroup>();
		del.add(group);
		for (String ref : views.keySet()) {
			View v = views.get(ref);
			if (v instanceof Node) {
				Node n = (Node)v;
				n.changeQueryGroups(null, del);
			}
		}
		return 0;
	}

	protected void populateSelf() {
		super.populateSelf();
		for (Node node : backend.query(groups))
			register(node.getName(), node);
	}

	private boolean fromEqualsThis(String from) {
		Set<QueryGroup> remove = new HashSet<QueryGroup>();
		for (String tag : tagsOf(from))
			remove.add(QueryGroup.create(tag, Type.TAG));
		return remove.equals(groups);
	}

	public int rename(String from, String to, View target, Set<QueryGroup> hintRemove, Set<QueryGroup> hintAdd) throws FuseException {
		assert fromEqualsThis(from);
		if (target != null)
			return fuse.Errno.EPERM;
		if (group.getType() == Type.MIME)
			return fuse.Errno.EPERM;
		Set<QueryGroup> add = new HashSet<QueryGroup>();
		for (String tag : tagsOf(to))
			add.add(QueryGroup.create(tag, Type.TAG));
		for (Node n : backend.query(groups))
			n.changeQueryGroups(add, groups);
		return 0;
	}

	public QueryGroup getGroup() {
		return group;
	}

	public Directory getParent() {
		return parent;
	}

	protected synchronized void update() {
		if (!group.stampValid(stamp))
			populated = false;
		super.update();
	}

	public Map<String,View> list() {
		update();
		return views;
	}
}
