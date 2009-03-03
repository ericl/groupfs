package queryfs;

import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import fuse.Filesystem3;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseGetattrSetter;
import fuse.FuseMount;
import fuse.FuseOpenSetter;
import fuse.FuseStatfsSetter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import queryfs.QueryGroup.Type;
import static queryfs.Util.*;

public class Filesystem implements Filesystem3 {
	public static final Log log = LogFactory.getLog(Filesystem.class);
	private QueryBackend backend;
	private Directory root;

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
			FuseMount.mount(fuseArgs, new Filesystem(originDir, mountPoint), log);
		} catch (Exception e) {
			log.error(e);
		}
	}

	public static void validate(File origin, File mount) throws IOException {
		if (!origin.exists() || !origin.isDirectory())
			throw new IllegalArgumentException("Origin is invalid.");
		if (!mount.exists() || !mount.isDirectory())
			throw new IllegalArgumentException("Mount point is invalid.");
		String op = origin.getCanonicalPath();
		String mp = mount.getCanonicalPath();
		if (!op.endsWith("/"))
			op += "/";
		if (!mp.endsWith("/"))
			mp += "/";
		if (mp.startsWith(op) || op.startsWith(mp))
			throw new IllegalArgumentException("Mount point overlaps origin.");
	}


	public Filesystem(File originDir, File mountPoint) {
		root = new Directory(backend = new QueryBackend(originDir));
	}

	private View get(String path) {
		String[] parts = path.split("/");
		if (parts.length < 2)
			return root;
		Directory directory = root;
		for (int i=0; i+1 < parts.length; i++) {
			String part = parts[i];
			if (part.equals("") || part.equals(".")) {
				// the same node
			} else if (part.equals("..")) {
				directory = directory.getParent();
			} else {
				directory = directory.getDir(part);
				if (directory == null)
					return null;
			}
		}
		return directory.get(parts[parts.length-1]);
	}

	private Directory getDir(String path) {
		View dir = get(path);
		if (dir instanceof Directory)
			return (Directory)dir;
		else
			return null;
	}

	private Node getNode(String path) {
		View dir = get(path);
		if (dir instanceof Node)
			return (Node)dir;
		else
			return null;
	}

	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
		View o = get(path);
		if (o == null)
			return fuse.Errno.ENOENT;
		else
			o.stat(getattrSetter);
		return 0;
	}

	public int readlink(String path, CharBuffer link) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int getdir(String path, FuseDirFiller dirFiller) throws FuseException {
		getDir(path).fill(dirFiller);
		return 0;
	}

	public int mknod(String path, int mode, int rdev) throws FuseException {
		String name = new File(path).getName();
		Set<QueryGroup> groups = new HashSet<QueryGroup>();
		for (String tag : tagsOf(new File(path).getParent()))
			groups.add(backend.getManager().create(tag, Type.TAG));
		String ext = extensionOf(new File(path));
		if (ext != null)
			groups.add(backend.getManager().create(ext, Type.MIME));
		backend.create(groups, name);
		return 0;
	}

	public int mkdir(String path, int mode) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int unlink(String path) throws FuseException {
		getNode(path).unlink();
		return 0;
	}

	public int rmdir(String path) throws FuseException {
		getDir(path).delete();
		return 0;
	}

	public int symlink(String from, String to) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	private Set<String> tagsOf(String parent) {
		Set<String> tags = new HashSet<String>(
			Arrays.asList(parent.split("/"))
		);
		tags.remove("");
		return tags;
	}

	public int rename(String from, String to) throws FuseException {
		View o = get(from);
		if (o instanceof Directory) {
			Directory d = (Directory)o;
			if (d.getGroup().getType() == Type.MIME)
				return fuse.Errno.EPERM;
			Set<QueryGroup> add = new HashSet<QueryGroup>();
			Set<QueryGroup> remove = new HashSet<QueryGroup>();
			for (String tag : tagsOf(to))
				add.add(backend.getManager().create(tag, Type.TAG));
			for (String tag : tagsOf(from))
				remove.add(backend.getManager().create(tag, Type.TAG));
			for (Node n : d.getNodes())
				n.changeQueryGroups(add, remove);
		} else {
			Node n = getNode(to);
			if (n != null) {
				n.setName(new File(to).getName());
				Set<QueryGroup> add = new HashSet<QueryGroup>();
				for (String tag : tagsOf(new File(to).getParent()))
					add.add(backend.getManager().create(tag, Type.TAG));
				n.changeQueryGroups(add, n.getQueryGroups());
			}
		}
		return 0;
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

	public int truncate(String path, long size) throws FuseException {
		getNode(path).truncate(size);
		return 0;
	}

	public int utime(String path, int atime, int mtime) throws FuseException {
		get(path).setModified(mtime);
		return 0;
	}

	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		statfsSetter.set(
			1024, // blockSize
			Integer.MAX_VALUE, // blocks
			Integer.MAX_VALUE, // blocksFree
			0, // blocksAvail
			0, // files
			0, // filesFree
			0 // namelen
		);
		return 0;
	}

	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		getNode(path).open(flags);
		return 0;
	}

	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		getNode(path).read(buf, offset);
		return 0;
	}

	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
		getNode(path).write(buf, offset);
		return 0;
	}

	public int flush(String path, Object fh) throws FuseException {
		return 0;
	}

	public int release(String path, Object fh, int flags) throws FuseException {
		getNode(path).close();
		return 0;
	}

	public int fsync(String path, Object fh, boolean isDatasync) throws FuseException {
		return 0;
	}
}
