package queryfs.backend;

import java.util.Set;

import queryfs.QueryGroup;

public interface FileSource {
	public Set<FileHandler> getAll();
	public FileHandler create(String name, Set<QueryGroup> groups);
	public long getFreeSpace();
	public long getUsableSpace();
	public long getTotalSpace();
}
