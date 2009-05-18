package groupfs.backend;

import java.util.Set;

import fuse.FuseException;

import groupfs.QueryGroup;

public interface QueryBackend {
	public Set<Node> query(Set<QueryGroup> groups);
	public Set<QueryGroup> subclass(Set<QueryGroup> groups);
	public Node create(Set<QueryGroup> groups, String name) throws FuseException;
	public long getFreeSpace();
	public long getTotalSpace();
	public long getUsableSpace();
}
