package groupfs.state;

import java.nio.ByteBuffer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.Directory;
import groupfs.Path;
import groupfs.Group;
import groupfs.Inode;

import static groupfs.Util.*;

public abstract class Node implements Inode {
	protected final Set<Group> groups, raw_groups;
	protected String name;

	public Node(Set<Group> groups) {
		this.groups = Collections.unmodifiableSet(this.raw_groups = new HashSet<Group>(groups));
	}

	public int getFType() {
		return FuseFtype.TYPE_FILE;
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public String getName() {
		return name;
	}

	protected abstract void logDifference(Set<Group> original, Set<Group> current);

	public int rename(Path from, Path to, Inode target, Directory orig, Directory dest) throws FuseException {
		Set<Group> original = new HashSet<Group>(groups);
		boolean hadMime = hasMime(groups);
		if (target != null && target != this) {
			// the common rename-swap-file-to-write behavior
			if (target instanceof Node) {
				Node node = (Node)target;
				changeGroups(node.getGroups(), null);
				node.deleteFromBackingMedia();
			}
			// if not a node then probably root dir
		}
		if (!dest.getGroups().equals(orig.getGroups()))
			changeGroups(dest.getGroups(), orig.getGroups());
		else
			logDifference(original, groups);
		setName(to.name(), hadMime);
		return 0;
	}

	public abstract int stat(FuseGetattrSetter setter);

	public abstract int setModified(long mtime) throws FuseException;

	public abstract void changeGroups(Set<Group> add, Set<Group> remove) throws FuseException;

	protected abstract void setName(String name, boolean hadMime) throws FuseException;

	public abstract int unlink() throws FuseException;

	public abstract int deleteFromBackingMedia() throws FuseException;

	public abstract void close() throws FuseException;

	public abstract int read(ByteBuffer buf, long offset) throws FuseException;

	public abstract int write(ByteBuffer buf, long offset) throws FuseException;

	public abstract int truncate(long size) throws FuseException;
}
