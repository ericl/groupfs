package groupfs.backend;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.Set;

import fuse.FuseException;

import groupfs.QueryGroup;

public interface FileHandler {
	public Set<QueryGroup> getAllGroups();
	public String getName();
	public void delete();
	public long lastModified();
	public long length();
	public void setLastModified(long mtime);
	public void close() throws IOException;
	public void setTagGroups(Set<QueryGroup> groups) throws IOException;
	public void setName(String name) throws IOException;
	public int read(ByteBuffer buf, long offset) throws IOException;
	public int write(ByteBuffer buf, long offset) throws IOException;
	public int truncate(long size) throws IOException;
}
