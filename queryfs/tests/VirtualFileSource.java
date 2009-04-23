package queryfs.tests;

import java.nio.ByteBuffer;

import java.util.HashSet;
import java.util.Set;

import fuse.FuseException;

import queryfs.QueryGroup;

import queryfs.backend.FileHandler;
import queryfs.backend.FileSource;

public class VirtualFileSource implements FileSource {
	private Set<FileHandler> files = new HashSet<FileHandler>();

	public Set<FileHandler> getAll() {
		return files;
	}

	public FileHandler create(String name, Set<QueryGroup> groups) {
		FileHandler fh = new VirtualFileHandler(name, groups);
		files.add(fh);
		return fh;
	}

	public long getFreeSpace() {
		return Integer.MAX_VALUE;
	}

	public long getUsableSpace() {
		return Integer.MAX_VALUE;
	}

	public long getTotalSpace() {
		return Integer.MAX_VALUE;
	}
}

class VirtualFileHandler implements FileHandler {
	private long time = System.currentTimeMillis();
	private Set<QueryGroup> groups;
	private String name;
	
	public VirtualFileHandler(String name, Set<QueryGroup> groups) {
		this.groups = groups;
		this.name = name;
	}

	public Set<QueryGroup> getAllGroups() {
		return groups;
	}

	public String getName() {
		return name;
	}

	public void setTagGroups(Set<QueryGroup> groups) {
		this.groups = groups;
	}

	public void renameTo(String name) {
		this.name = name;
	}

	public void delete() {
		groups.clear();
	}

	public long lastModified() {
		return time;
	}

	public long length() {
		return 0;
	}

	public void setLastModified(long mtime) {
		this.time = mtime;
	}

	public void close() {}

	public int read(ByteBuffer buf, long offset) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int write(ByteBuffer buf, long offset) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int truncate(long size) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}
}
