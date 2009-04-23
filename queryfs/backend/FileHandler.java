package queryfs.backend;

import java.nio.ByteBuffer;

import java.util.Set;

import fuse.FuseException;

import queryfs.QueryGroup;

public interface FileHandler {
	public Set<QueryGroup> getAllGroups();
	public String getName();
	public void delete();
	public long lastModified();
	public long length();
	public void setLastModified(long mtime);
	public void close() throws FuseException;
	public void setTagGroups(Set<QueryGroup> groups) throws FuseException;
	public void renameTo(String name) throws FuseException;
	public int read(ByteBuffer buf, long offset) throws FuseException;
	public int write(ByteBuffer buf, long offset) throws FuseException;
	public int truncate(long size) throws FuseException;
}
