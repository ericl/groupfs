package groupfs;

import java.io.File;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import java.util.HashSet;

import java.util.Map.Entry;

import java.util.Set;

import fuse.Filesystem3;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseGetattrSetter;
import fuse.FuseMount;
import fuse.FuseOpenSetter;
import fuse.FuseSizeSetter;
import fuse.FuseStatfsSetter;
import fuse.XattrLister;
import fuse.XattrSupport;

import groupfs.Group.Type;

import groupfs.state.BaseDirectory;
import groupfs.state.Manager;
import groupfs.state.Node;
import groupfs.storage.DirectoryFileSource;
import groupfs.storage.StorageInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static groupfs.Util.*;

public class Filesystem implements Filesystem3, XattrSupport {
	public static final Log log = LogFactory.getLog(Filesystem.class);
	private Manager backend;
	private PathMapper mapper;

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Must specify origin directory and mountpoint.");
			System.exit(-1);
		}
		String fuseArgs[] = new String[args.length - 1];
		System.arraycopy(args, 0, fuseArgs, 0, fuseArgs.length);
		File originDir = new File(args[args.length - 1]);
		File mountPoint = new File(args[args.length - 2]);
		try {
			validate(originDir, mountPoint);
			FuseMount.mount(fuseArgs, new Filesystem(
				new Manager(new DirectoryFileSource(originDir))
			), log);
		} catch (Exception e) {
			log.error(e);
		}
	}

	public Filesystem(Manager backend) {
		this.backend = backend;
		mapper = new PathMapper(new BaseDirectory(backend));
	}

	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
		Inode o = mapper.get(Path.get(path));
		if (o == null)
			return fuse.Errno.ENOENT;
		else
			return o.stat(getattrSetter);
	}

	public int readlink(String path, CharBuffer link) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int getdir(String input, FuseDirFiller dirFiller) throws FuseException {
		Path path = Path.get(input);
		Directory d = mapper.getDir(path);
		if (d == null)
			return fuse.Errno.ENOENT;
		for (Entry<String,Inode> entry : d.list().entrySet())
			dirFiller.add(entry.getKey(), 0, entry.getValue().getFType());
		return 0;
	}

	public int mknod(String input, int mode, int rdev) throws FuseException {
		Path path = Path.get(input);
		if (!canMknod(path.parent()) || path.name().startsWith("."))
			return fuse.Errno.EPERM;
		mapper.notifyLatest(path, backend.create(groupsOf(path), path.name()));
		return 0;
	}

	private boolean _canMknod(Path path, boolean rootok) {
		if (path.value.length() <= 1)
			return rootok;
		Directory parent = mapper.getDir(path);
		if (parent != null && !parent.getPerms().canMknod())
			return false;
		return _canMknod(path.parent(), true);
	}

	private boolean canMknod(Path path) {
		return _canMknod(path, false);
	}

	public int mkdir(String input, int mode) throws FuseException {
		Path path = Path.get(input);
		String name = path.name();
		if (name.startsWith("."))
			return fuse.Errno.EPERM;
		Directory parent = mapper.getDir(path.parent());
		if (parent == null)
			return fuse.Errno.ENOENT;
		else if (!parent.getPerms().canMkdir())
			return fuse.Errno.EPERM;
		parent.update();
		Group group = Group.create(name, Type.TAG);
		if (parent.getGroups().contains(group))
			return fuse.Errno.EPERM;
		parent.mkdir(group);
		return 0;
	}

	public int unlink(String input) throws FuseException {
		Path path = Path.get(input);
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		Directory d = mapper.getDir(path.parent());
		if (d == null)
			return fuse.Errno.ENOENT;
		else if (d.getGroup() == Group.GROUP_NO_GROUP) {
			return n.deleteFromBackingMedia();
		} else if (!d.getPerms().canDeleteNode())
			return fuse.Errno.EPERM;
		return n.unlink();
	}

	public int rmdir(String input) throws FuseException {
		Path path = Path.get(input);
		Directory d = mapper.getDir(path);
		if (d == null)
			return fuse.Errno.ENOENT;
		else if (!d.getPerms().canDelete())
			return fuse.Errno.EPERM;
		return d.delete();
	}

	public int symlink(String from, String to) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int rename(String fromPath, String toPath) throws FuseException {
		Path from = Path.get(fromPath);
		Path to = Path.get(toPath);
		Inode inode = mapper.get(from);
		Directory orig = mapper.getDir(from.parent());
		Directory dest = mapper.getDir(to.parent());

		if (inode == null || orig == null || dest == null)
			return fuse.Errno.ENOENT;
		// allow renaming within mime dir
		else if (orig != dest && (!orig.getPerms().canMoveOut() || !dest.getPerms().canMoveIn()))
			return fuse.Errno.EPERM;
		// forbid creation of mime-type dirs or nodes
		else if (to.name().startsWith("."))
			return fuse.Errno.EPERM;
		if (!allUnique(to))
			return fuse.Errno.EPERM;
		return inode.rename(from, to, mapper.get(to), orig, dest);
	}

	public int link(String from, String to) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int chmod(String path, int mode) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int chown(String path, int uid, int gid) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int truncate(String input, long size) throws FuseException {
		Path path = Path.get(input);
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		return n.truncate(size);
	}

	public int utime(String input, int atime, int mtime) throws FuseException {
		Path path = Path.get(input);
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		return n.setModified(mtime);
	}

	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		StorageInfo info = backend.getInfo();
		statfsSetter.set(
			info.getBlockSize(), // blockSize
			info.getBlocks(),
			info.getBlocksFree(),
			info.getBlocksAvail(),
			0, // files
			0, // filesFree
			0 // namelen
		);
		return 0;
	}

	public int open(String input, int flags, FuseOpenSetter openSetter) throws FuseException {
		Path path = Path.get(input);
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		// not really opening file; flags aren't supported by backend anyways
		return 0;
	}

	public int read(String input, Object fh, ByteBuffer buf, long offset) throws FuseException {
		Path path = Path.get(input);
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		return n.read(buf, offset);
	}

	public int write(String input, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
		Path path = Path.get(input);
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		return n.write(buf, offset);
	}

	public int flush(String path, Object fh) throws FuseException {
		return 0;
	}

	public int release(String input, Object fh, int flags) throws FuseException {
		Path path = Path.get(input);
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		n.close();
		return 0;
	}

	public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
		return 0;
	}

	public int getxattrsize(String path, String name, FuseSizeSetter sizeSetter) throws FuseException {
		return 0;
	}

	public int getxattr(String path, String name, ByteBuffer dst) throws FuseException, BufferOverflowException {
		return 0;
	}

	public int listxattr(String path, XattrLister lister) throws FuseException {
		return 0;
	}

	public int setxattr(String path, String name, ByteBuffer value, int flags) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int removexattr(String path, String name) throws FuseException {
		return fuse.Errno.ENOENT;
	}
}
