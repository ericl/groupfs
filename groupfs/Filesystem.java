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

import groupfs.QueryGroup.Type;

import groupfs.backend.DirectoryFileSource;
import groupfs.backend.JournalingBackend;
import groupfs.backend.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static groupfs.Util.*;

public class Filesystem implements Filesystem3, XattrSupport {
	public static final Log log = LogFactory.getLog(Filesystem.class);
	public static final int blksize = 1024;
	private JournalingBackend backend;
	private ViewMapper mapper;

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
			FuseMount.mount(fuseArgs, new Filesystem(new JournalingBackend(new DirectoryFileSource(originDir))), log);
		} catch (Exception e) {
			log.error(e);
		}
	}

	public Filesystem(JournalingBackend backend) {
		this.backend = backend;
		mapper = new ViewMapper(new JournalingDirectory(backend));
	}

	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
		View o = mapper.get(Path.get(path));
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
		for (Entry<String,View> entry : d.list().entrySet())
			dirFiller.add(entry.getKey(), 0, entry.getValue().getFType());
		return 0;
	}

	public int mknod(String input, int mode, int rdev) throws FuseException {
		Path path = Path.get(input);
		String name = path.name();
		if (!canMknod(path.parent()) || name.startsWith("."))
			return fuse.Errno.EPERM;
		Directory d = mapper.getDir(path.parent());
		Set<QueryGroup> groups = null;
		if (d == null) {
			groups = new HashSet<QueryGroup>();
			for (String tag : tagsOf(path.parent().value))
				groups.add(QueryGroup.create(tag, Type.TAG));
		} else
			groups = new HashSet<QueryGroup>(d.getQueryGroups());
		String ext = extensionOf(path.name());
		if (ext != null)
			groups.add(QueryGroup.create(ext, Type.MIME));
		Node node = backend.create(groups, name);
		mapper.notifyLatest(path, node);
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
		QueryGroup group = QueryGroup.create(name, Type.TAG);
		if (parent.getQueryGroups().contains(group))
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
		else if (d.getGroup() == QueryGroup.GROUP_NO_GROUP) {
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
		View view = mapper.get(from);
		Directory orig = mapper.getDir(from.parent());
		Directory dest = mapper.getDir(to.parent());

		if (view == null || orig == null || dest == null)
			return fuse.Errno.ENOENT;
		// allow renaming within mime dir
		else if (orig != dest && (!orig.getPerms().canMoveOut() || !dest.getPerms().canMoveIn()))
			return fuse.Errno.EPERM;
		// forbid creation of mime-type dirs or nodes
		else if (to.name().startsWith("."))
			return fuse.Errno.EPERM;

		return view.rename(from, to, mapper.get(to), orig, dest);
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
		statfsSetter.set(
			blksize, // blockSize
			(int)(backend.getTotalSpace() / blksize), // blocks
			(int)(backend.getFreeSpace() / blksize), // blocksFree
			(int)(backend.getUsableSpace() / blksize), // blocksAvail
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
