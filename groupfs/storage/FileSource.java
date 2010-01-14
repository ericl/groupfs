package groupfs.storage;

import java.util.Set;

import fuse.FuseException;

import groupfs.Group;

/**
 * Interface for handling data storage.
 */
public interface FileSource {
	public Set<FileHandler> getAll();
	public FileHandler create(String name, Set<Group> groups) throws FuseException;
	public StorageInfo getInfo();
}
