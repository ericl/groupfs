package queryfs.backend;

import java.nio.ByteBuffer;

import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import queryfs.QueryGroup;
import queryfs.View;

public abstract class Node implements View {
	protected Set<QueryGroup> groups;
	protected String name = "__undefined__";

	public Node(Set<QueryGroup> groups) {
		this.groups = groups;
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

	public boolean permanent() {
		return false;
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
