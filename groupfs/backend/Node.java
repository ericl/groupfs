package groupfs.backend;

import java.io.File;

import java.nio.ByteBuffer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.QueryGroup;
import groupfs.View;

public abstract class Node implements View {
	protected final Set<QueryGroup> groups, raw_groups;
	protected String name;

	public Node(Set<QueryGroup> groups) {
		this.groups = Collections.unmodifiableSet(this.raw_groups = new HashSet<QueryGroup>(groups));
	}

	public int getFType() {
		return FuseFtype.TYPE_FILE;
	}

	public Set<QueryGroup> getQueryGroups() {
		return groups;
	}

	public String getName() {
		return name;
	}

	protected abstract void update(Set<QueryGroup> all, Set<QueryGroup> add, Set<QueryGroup> remove);

	public int rename(String from, String to, View target, Set<QueryGroup> hintRemove, Set<QueryGroup> hintAdd) throws FuseException {
		if (target != null && target != this) {
			// the common rename-swap-file-to-write behavior
			if (target instanceof Node) {
				Node node = (Node)target;
				changeQueryGroups(node.getQueryGroups(), null);
				node.deleteFromBackingMedia();
			}
			// if not a node then probably root dir !?
		}
		setName(new File(to).getName());
		if (!hintRemove.equals(hintAdd))
			changeQueryGroups(hintAdd, hintRemove);
		else
			update(groups, hintAdd, hintRemove);
		return 0;
	}

	public abstract int stat(FuseGetattrSetter setter);

	public abstract int setModified(long mtime) throws FuseException;

	public abstract void changeQueryGroups(Set<QueryGroup> add, Set<QueryGroup> remove) throws FuseException;

	public abstract void setName(String name) throws FuseException;

	public abstract int unlink() throws FuseException;

	public abstract int deleteFromBackingMedia() throws FuseException;

	public abstract void close() throws FuseException;

	public abstract int read(ByteBuffer buf, long offset) throws FuseException;

	public abstract int write(ByteBuffer buf, long offset) throws FuseException;

	public abstract int truncate(long size) throws FuseException;
}
