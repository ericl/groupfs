package groupfs.storage;

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

import groupfs.Group.Type;

import groupfs.Group;

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
				Set<Group> groups = groupsOf(child);
				raw_files.add(new DirectoryFileHandler(root, child, groups));
			}
		}
	}

	private Set<Group> groupsOf(File file) {
		String a = file.getParentFile().getAbsolutePath();
		String b = root.getAbsolutePath();
		Set<Group> groups = new HashSet<Group>();
		if (a.equals(b)) {
			groups.add(Group.GROUP_NO_GROUP);
			return groups; // else things are broken by substring()
		}
		String path = a.substring(b.length() + 1);
		Set<String> tags = new HashSet<String>(
			Arrays.asList(path.split("/"))
		);
		tags.remove("");
		for (String tag : tags)
			groups.add(Group.create(tag, Type.TAG));
		groups.add(Group.create(extensionOf(file.getName()), Type.MIME));
		assert maxOneMimeGroup(groups);
		return groups;
	}

	public Set<FileHandler> getAll() {
		return files;
	}

	public FileHandler create(String name, Set<Group> groups) throws FuseException {
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
	private final Set<Group> groups, raw_groups;
	private File file, root;
	private String name;
	
	public DirectoryFileHandler(File root, File file, Set<Group> groups) {
		this.root = root;
		this.file = file;
		this.name = file.getName();
		this.groups = Collections.unmodifiableSet(raw_groups = new HashSet<Group>(groups));
	}

	public Set<Group> getAllGroups() {
		return groups;
	}

	public String getName() {
		return name;
	}

	public void setTagGroups(Set<Group> groups) throws IOException {
		assert maxOneMimeGroup(groups);
		File loc = null;
		Set<Group> consideredGroups = new HashSet<Group>(groups);
		consideredGroups.remove(Group.GROUP_NO_GROUP);
		loc = getDestination(newPath(root, consideredGroups), name);
		raw_groups.clear();
		raw_groups.addAll(groups);
		file.renameTo(loc);
		rmdirs(file.getParentFile());
		file = loc;
		close();
	}

	public void setName(String name) throws IOException {
		if (this.name.equals(name))
			return;
		this.name = name;
		File dest = null;
		dest = getDestination(file.getParent(), name);
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

	public void close() throws IOException {
		if (channel == null)
			return;
		channel.close();
		channel = null;
		channelFlags = FilesystemConstants.O_RDONLY;
	}

	public int read(ByteBuffer buf, long offset) throws IOException {
		open(FilesystemConstants.O_RDONLY);
		channel.read(buf, offset);
		return 0;
	}

	public int write(ByteBuffer buf, long offset) throws IOException {
		open(FilesystemConstants.O_RDWR);
		channel.position(offset);
		channel.write(buf);
		return 0;
	}

	public int truncate(long size) throws IOException {
		open(FilesystemConstants.O_RDWR);
		channel.truncate(size);
		return 0;
	}

	private void open(int flags) throws IOException {
		if (!file.exists())
			throw new IOException("No such entry");
		if (channel == null || channelFlags < flags) {
			try {
				String mode = "r";
				if (flags > FilesystemConstants.O_RDONLY)
					mode = "rw";
				channel = new RandomAccessFile(file, mode).getChannel();
			} catch (FileNotFoundException e) {
				throw new IOException("No such entry");
			}
			channelFlags = flags;
		}
	}
}
