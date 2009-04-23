package queryfs.backend;

import java.util.Set;

import fuse.FuseException;

import queryfs.QueryGroup;

public interface FileSource {
	public Set<FileHandler> getAll();
	public FileHandler create(String name, Set<QueryGroup> groups) throws FuseException;
	public long getFreeSpace();
	public long getUsableSpace();
	public long getTotalSpace();
}
