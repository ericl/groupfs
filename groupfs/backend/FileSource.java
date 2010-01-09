package groupfs.backend;

import java.util.Set;

import fuse.FuseException;

import groupfs.Group;

public interface FileSource {
	public Set<FileHandler> getAll();
	public FileHandler create(String name, Set<Group> groups) throws FuseException;
	public long getFreeSpace();
	public long getUsableSpace();
	public long getTotalSpace();
}
