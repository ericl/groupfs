package groupfs.backend;

import java.util.Set;

import fuse.FuseException;

import groupfs.QueryGroup;

public interface QueryBackendWithCache extends QueryBackend {
	public void flag(QueryGroup group);
	public void flush();
	public void checkRootRm(Set<QueryGroup> groups);
	public void checkRootAdd(Set<QueryGroup> groups);
}
