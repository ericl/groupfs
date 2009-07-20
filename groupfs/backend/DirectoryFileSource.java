package groupfs.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fuse.FilesystemConstants;
import fuse.FuseException;

import groupfs.QueryGroup.Type;

import groupfs.QueryGroup;

import groupfs.backend.FileHandler;
import groupfs.backend.FileSource;

import static groupfs.Util.*;

public class DirectoryFileSource implements FileSource {

	private final Set<FileHandler> raw_files, files;
	private File root;

	public DirectoryFileSource(File originDir) {
		this.root = originDir;
		files = Collections.unmodifiableSet(raw_files = new HashSet<FileHandler>());
		scan(root);
	}

	private void scan(File dir) {
		for (File child : dir.listFiles()) {
			if (!child.canRead())
				continue;
			if (child.isDirectory()) {
				if (!child.getName().startsWith("."))
					scan(child);
			} else {
				Set<QueryGroup> groups = groupsOf(child);
				raw_files.add(new DirectoryFileHandler(root, child, groups));
			}
		}
	}

	private Set<QueryGroup> groupsOf(File file) {
		String a = file.getParentFile().getAbsolutePath();
		String b = root.getAbsolutePath();
		Set<QueryGroup> groups = new HashSet<QueryGroup>();
		if (a.equals(b)) {
			groups.add(QueryGroup.GROUP_NO_GROUP);
			return groups; // else things are broken by substring()
		}
		String path = a.substring(b.length() + 1);
		Set<String> tags = new HashSet<String>(
			Arrays.asList(path.split("/"))
		);
		tags.remove("");
		for (String tag : tags)
			groups.add(QueryGroup.create(tag, Type.TAG));
		groups.add(QueryGroup.create(extensionOf(file.getName()), Type.MIME));
		assert maxOneMimeGroup(groups);
		return groups;
	}

	public Set<FileHandler> getAll() {
		return files;
	}

	public FileHandler create(String name, Set<QueryGroup> groups) throws FuseException {
		File file = null;
		try {
			file = getDestination(newPath(root, groups), name);
			file.createNewFile();
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
		FileHandler fh = new DirectoryFileHandler(root, file, groups);
		raw_files.add(fh);
		return fh;
	}

	public long getFreeSpace() {
		return root.getFreeSpace();
	}

	public long getUsableSpace() {
		return root.getUsableSpace();
	}

	public long getTotalSpace() {
		return root.getTotalSpace();
	}
}

class DirectoryFileHandler implements FileHandler {
	private FileChannel channel;
	private int channelFlags = FilesystemConstants.O_RDONLY;
	private final Set<QueryGroup> groups, raw_groups;
	private File file, root;
	private String name;
	
	public DirectoryFileHandler(File root, File file, Set<QueryGroup> groups) {
		this.root = root;
		this.file = file;
		this.name = file.getName();
		this.groups = Collections.unmodifiableSet(raw_groups = new HashSet<QueryGroup>(groups));
	}

	public Set<QueryGroup> getAllGroups() {
		return groups;
	}

	public String getName() {
		return name;
	}

	public void setTagGroups(Set<QueryGroup> groups) throws FuseException {
		assert maxOneMimeGroup(groups);
		raw_groups.clear();
		raw_groups.addAll(groups);
		File loc = null;
		try {
			Set<QueryGroup> consideredGroups = new HashSet<QueryGroup>(groups);
			consideredGroups.remove(QueryGroup.GROUP_NO_GROUP);
			loc = getDestination(newPath(root, consideredGroups), name);
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
		file.renameTo(loc);
		rmdirs(file.getParentFile());
		file = loc;
		close();
	}

	public void setName(String name) throws FuseException {
		if (this.name.equals(name))
			return;
		this.name = name;
		File dest = null;
		try {
			dest = getDestination(file.getParent(), name);
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
		file.renameTo(dest);
		file = dest;
		close();
	}

	public void delete() {
		file.delete();
	}

	public long lastModified() {
		return file.lastModified();
	}

	public long length() {
		return file.length();
	}

	public void setLastModified(long mtime) {
		file.setLastModified(mtime);
	}

	public void close() throws FuseException {
		if (channel == null)
			return;
		try {
			channel.close();
		} catch (IOException eio) {
			throw new FuseException("IO Exception", eio).initErrno(FuseException.EIO);
		}
		channel = null;
		channelFlags = FilesystemConstants.O_RDONLY;
	}

	public int read(ByteBuffer buf, long offset) throws FuseException {
		open(FilesystemConstants.O_RDONLY);
		try {
			channel.read(buf, offset);
		} catch (IOException e) {
			throw new FuseException("IO Exception", e).initErrno(FuseException.EIO);
		}
		return 0;
	}

	public int write(ByteBuffer buf, long offset) throws FuseException {
		open(FilesystemConstants.O_RDWR);
		try {
			channel.position(offset);
			channel.write(buf);
		} catch (IOException e) {
			throw new FuseException("IO Exception", e).initErrno(FuseException.EIO);
		}
		return 0;
	}

	public int truncate(long size) throws FuseException {
		open(FilesystemConstants.O_RDWR);
		try {
			channel.truncate(size);
		} catch (IOException e) {
			throw new FuseException("IO Exception", e).initErrno(FuseException.EIO);
		}
		return 0;
	}

	private void open(int flags) throws FuseException {
		if (!file.exists())
			throw new FuseException("No Such Entry").initErrno(FuseException.ENOENT);
		if (channel == null || channelFlags < flags) {
			try {
				String mode = "r";
				if (flags > FilesystemConstants.O_RDONLY)
					mode = "rw";
				channel = new RandomAccessFile(file, mode).getChannel();
			} catch (FileNotFoundException e) {
				throw new FuseException("No Such Entry", e).initErrno(FuseException.ENOENT);
			}
			channelFlags = flags;
		}
	}
}
