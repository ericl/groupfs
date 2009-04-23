package queryfs.backend;

import java.nio.ByteBuffer;

import java.util.Set;

import fuse.FuseException;

import queryfs.QueryGroup;

public interface FileHandler {
	public Set<QueryGroup> getAllGroups();
	public String getName();
	public void setTagGroups(Set<QueryGroup> groups);
	public void renameTo(String name);
	public void delete();
	public long lastModified();
	public long length();
	public void setLastModified(long mtime);
	public void close();
	public int read(ByteBuffer buf, long offset) throws FuseException;
	public int write(ByteBuffer buf, long offset) throws FuseException;
	public int truncate(long size) throws FuseException;
}
