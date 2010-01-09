package groupfs.tests;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.HashSet;
import java.util.Set;

import fuse.FuseException;

import groupfs.Group;

import groupfs.backend.FileHandler;
import groupfs.backend.FileSource;

public class VirtualFileSource implements FileSource {
	private Set<FileHandler> files = new HashSet<FileHandler>();

	public Set<FileHandler> getAll() {
		return files;
	}

	public FileHandler create(String name, Set<Group> groups) {
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
	private Set<Group> groups;
	private String name;
	
	public VirtualFileHandler(String name, Set<Group> groups) {
		this.groups = groups;
		this.name = name;
	}

	public Set<Group> getAllGroups() {
		return groups;
	}

	public String getName() {
		return name;
	}

	public void setTagGroups(Set<Group> groups) {
		this.groups = new HashSet<Group>(groups);
	}

	public void setName(String name) {
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

	public int read(ByteBuffer buf, long offset) throws IOException {
		return fuse.Errno.ENOTSUPP;
	}

	public int write(ByteBuffer buf, long offset) throws IOException {
		return fuse.Errno.ENOTSUPP;
	}

	public int truncate(long size) throws IOException {
		return fuse.Errno.ENOTSUPP;
	}
}
