package queryfs.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import java.util.HashSet;
import java.util.Set;

import fuse.FilesystemConstants;
import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import queryfs.QueryGroup.Type;

import queryfs.QueryGroup;

import static queryfs.Util.*;

public class TestingBackend extends CachingQueryBackend {
	protected final static String root = "/dev/shm/tmp-1000/queryfs-data-dump";
	static {
		File f = new File(root);
		f.mkdirs();
		f.deleteOnExit();
	}

	public void create(Set<QueryGroup> groups, String name) throws FuseException {
		assert maxOneMimeGroup(groups);
		flagged.addAll(groups);
		flush();
		nodes.add(new VirtualNode(this, groups, name));
		checkRootAdd(groups);
	}

	public long getFreeSpace() {
		return Integer.MAX_VALUE;
	}

	public long getTotalSpace() {
		return Integer.MAX_VALUE;
	}

	public long getUsableSpace() {
		return Integer.MAX_VALUE;
	}
}

class VirtualNode extends Node {
	private static int count;
	private long mtime = System.currentTimeMillis();
	private TestingBackend backend;
	private File file;
	private FileChannel channel;
	private int channelFlags = FilesystemConstants.O_RDONLY;

	public VirtualNode(TestingBackend backend, Set<QueryGroup> groups, String name) {
		super(groups);
		this.backend = backend;
		file = new File(TestingBackend.root + "/data." + count++);
		try {
			file.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		file.deleteOnExit();
		this.name = name;
	}

	public int stat(FuseGetattrSetter setter) {
		int time = (int)(mtime / 1000L);
		long length = file.length();
		setter.set(
			0l, // inode
			FuseFtype.TYPE_FILE | 0644,
			1, // nlink
			UID,
			GID,
			0, // rdev
			length,
			(int)((length + 511L) / 512L),
			time, time, time // atime, mtime, ctime
		);
		return 0;
	}

	public int setModified(long mtime) throws FuseException {
		this.mtime = mtime;
		return 0;
	}

	public void changeQueryGroups(Set<QueryGroup> add, Set<QueryGroup> remove) throws FuseException {
		changeQueryGroups(add, remove, false);
	}

	public void setName(String name) throws FuseException {
		this.name = name;
	}

	public int unlink() throws FuseException {
		changeQueryGroups(null, new HashSet<QueryGroup>(groups), true);
		return 0;
	}

	public int deleteFromBackingMedia() throws FuseException {
		backend.unref(this);
		for (QueryGroup q : groups)
			backend.flag(q);
		groups.clear();
		backend.flush();
		file.delete();
		return 0;
	}

	public void close() throws FuseException {}

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

	protected void changeQueryGroups(Set<QueryGroup> add, Set<QueryGroup> remove, boolean allowMimetypeChange) throws FuseException {
		if (remove != null)
			for (QueryGroup r : remove) {
				if (allowMimetypeChange || r.getType() != Type.MIME) {
					groups.remove(r);
					backend.flag(r);
				}
			}
		if (add != null)
			for (QueryGroup a : add) {
				if (allowMimetypeChange || a.getType() != Type.MIME)
					groups.add(a);
			}
		if (hasCategory(groups)) {
			groups.remove(QueryGroup.GROUP_NO_GROUP);
			backend.flag(QueryGroup.GROUP_NO_GROUP);
		} else {
			for (QueryGroup group : groups)
				backend.flag(group);
			groups.clear();
			groups.add(QueryGroup.GROUP_NO_GROUP);
		}
		for (QueryGroup g : groups)
			backend.flag(g);
		backend.flush();
		if (remove != null)
			backend.checkRoot(remove);
		if (add != null)
			backend.checkRootAdd(add);
	}

	private void open(int flags) throws FuseException {
		synchronized (this) {
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
}
