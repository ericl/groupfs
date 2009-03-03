package queryfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import java.util.Set;

import com.sun.security.auth.module.UnixSystem;

import fuse.FilesystemConstants;
import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import queryfs.QueryGroup.Type;

import static queryfs.Util.*;

public class Node implements View {
	private QueryBackend backend;
	private final static int UID = (int)new UnixSystem().getUid();
	private final static int GID = (int)new UnixSystem().getGid();
	private Set<QueryGroup> groups;
	private FileChannel channel;
	private int channelFlags = FilesystemConstants.O_RDONLY;
	private File file;
	private String name;

	public Node(QueryBackend backend, File file, Set<QueryGroup> groups) {
		assert file.exists() && !file.isDirectory();
		assert backend.root.isDirectory();
		this.backend = backend;
		this.file = file;
		this.groups = groups;
		this.name = file.getName();
	}

	public int getFType() {
		return FuseFtype.TYPE_FILE;
	}

	public Set<QueryGroup> getQueryGroups() {
		return groups;
	}

	public String getName() {
		return name;
	}

	public void stat(FuseGetattrSetter setter) {
		int time = (int)(file.lastModified() / 1000L);
		long size = file.length();
		setter.set(
			0, // inode
			FuseFtype.TYPE_FILE | 0644,
			1, // nlink
			UID,
			GID,
			0, // rdev
			size,
			(int)((size + 511L) / 512L),
			time, time, time // atime, mtime, ctime
		);
	}

	public void setModified(long mtime) throws FuseException {
		file.setLastModified(mtime);
	}

	public void changeQueryGroups(Set<QueryGroup> add, Set<QueryGroup> remove) throws FuseException {
		if (add != null)
			for (QueryGroup a : add) {
				groups.add(a);
				assert a.getType() != Type.MIME;
			}
		if (remove != null)
			for (QueryGroup r : remove) {
				groups.remove(r);
				backend.flag(r);
			}
		File loc = null;
		try {
			loc = getDestination(newPath(backend.root, groups), name);
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
		file.renameTo(loc);
		file = loc;
		for (QueryGroup g : groups)
			backend.flag(g);
		backend.flush();
		if (groups.isEmpty())
			backend.unref(this);
	}

	public void setName(String name) throws FuseException {
		this.name = name;
		synchronized (this) {
			String current = file.getName();
			if (current.equals(name))
				return;
			File dest = null;
			try {
				dest = getDestination(file.getParent(), name);
			} catch (IOException e) {
				throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
			}
			file.renameTo(dest);
			file = dest;
		}
	}

	public void unlink() throws FuseException {
		this.file = null;
		changeQueryGroups(null, groups);
	}

	public void open(int flags) throws FuseException {
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

	public void close() throws FuseException {
		synchronized (this) {
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
	}

	public void read(ByteBuffer buf, long offset) throws FuseException {
		open(FilesystemConstants.O_RDONLY);
		try {
			channel.read(buf, offset);
		} catch (IOException e) {
			throw new FuseException("IO Exception", e).initErrno(FuseException.EIO);
		}
	}

	public void write(ByteBuffer buf, long offset) throws FuseException {
		open(FilesystemConstants.O_RDWR);
		try {
			channel.position(offset);
			channel.write(buf);
		} catch (IOException e) {
			throw new FuseException("IO Exception", e).initErrno(FuseException.EIO);
		}
	}

	public void truncate(long size) throws FuseException {
		open(FilesystemConstants.O_RDWR);
		try {
			channel.truncate(size);
		} catch (IOException e) {
			throw new FuseException("IO Exception", e).initErrno(FuseException.EIO);
		}
	}
}
