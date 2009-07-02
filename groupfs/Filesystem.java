package groupfs;

import java.io.File;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import groupfs.QueryGroup.Type;

import groupfs.backend.DirectoryFileSource;
import groupfs.backend.FlexibleBackend;
import groupfs.backend.Node;
import groupfs.backend.QueryBackend;

import static groupfs.Util.*;

public class Filesystem implements Filesystem3, XattrSupport {
	public static final Log log = LogFactory.getLog(Filesystem.class);
	public static final int blksize = 1024;
	private QueryBackend backend;
	private ViewMapper mapper;

	protected class ViewMapper {
		private final RootDirectory root;
		private Path latestPath;
		private Node latestNode;
		private Map<Path,FloatingDirectory> floats = new HashMap<Path,FloatingDirectory>();
		private Map<Path,List<FloatingDirectory>> parents = new HashMap<Path,List<FloatingDirectory>>();

		ViewMapper(RootDirectory root) {
			this.root = root;
		}

		void createFloat(Path path) {
			Path parent = path.parent();
			String name = path.name();
			FloatingDirectory f = new FloatingDirectory(this, parent, path, QueryGroup.create(name, Type.TAG));
			floats.put(path, f);
			List<FloatingDirectory> s = parents.get(parent);
			if (s == null) {
				s = new LinkedList<FloatingDirectory>();
				parents.put(parent, s);
			}
			s.add(f);
		}

		void delete(Path path, boolean recursive) {
			FloatingDirectory target = floats.remove(path);
			if (target == null)
				return;
			List<FloatingDirectory> l = parents.get(target.getHost());
			l.remove(target);
			if (l.isEmpty())
				parents.remove(target.getHost());
			if (recursive)
				for (Path test : new HashSet<Path>(floats.keySet()))
					if (test.value.startsWith(path.value))
						delete(test, false);
		}

		void remap(Path from, Path to) {
			delete(from, false);
			createFloat(to);
			for (Path test : new HashSet<Path>(floats.keySet()))
				if (test.value.startsWith(from.value)) {
					delete(test, false);
					createFloat(Path.get(test.value.replaceFirst(from.value, to.value)));
				}
		}

		void finish(Path parent, Set<String> taken, FuseDirFiller filler) {
			List<FloatingDirectory> s = parents.get(parent);
			if (s != null)
				for (FloatingDirectory f : s) {
					String name = f.getGroup().getValue();
					if (!taken.contains(name))
						filler.add(name, 0, f.getFType());
				}
		}

		void notifyLatest(Path path, Node node) {
			latestPath = path;
			latestNode = node;
		}

		View get(Path path) {
			if (latestPath != null && latestPath.equals(path))
				return latestNode;
			String[] parts = path.value.split("/");
			if (parts.length < 2)
				return root;
			Directory directory = root;
			boolean parent_ok = true;
			for (int i=0; i+1 < parts.length; i++) {
				String part = parts[i];
				if (part.equals("") || part.equals(".")) {
					// the same node
				} else if (part.equals("..")) {
					directory = directory.getParent();
				} else {
					directory = directory.getDir(part);
					if (directory == null) {
						parent_ok = false;
						break;
					}
				}
			}
			View output = null;
			if (parent_ok)
				output = directory.get(parts[parts.length-1]);
			if (output == null)
				output = floats.get(path);
			else
				delete(path, false); // garbage collect those floatingdirs
			return output;
		}

		Directory getDir(Path path) {
			View dir = get(path);
			if (dir instanceof Directory)
				return (Directory)dir;
			else
				return null;
		}

		Node getNode(Path path) {
			View dir = get(path);
			if (dir instanceof Node)
				return (Node)dir;
			else
				return null;
		}
	}

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
			FuseMount.mount(fuseArgs, new Filesystem(new FlexibleBackend(new DirectoryFileSource(originDir))), log);
		} catch (Exception e) {
			log.error(e);
		}
	}

	public Filesystem(QueryBackend backend) {
		this.backend = backend;
		mapper = new ViewMapper(new RootDirectory(backend));
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

	static long ai, bi, ci, di, ei;
	static long a, b, c, d, e;
	static void ai() {
		ai = System.nanoTime();
	}
	static void a() {
		a += System.nanoTime() - ai;
	}
	static void bi() {
		bi = System.nanoTime();
	}
	static void b() {
		b += System.nanoTime() - bi;
	}
	static void ci() {
		ci = System.nanoTime();
	}
	static void c() {
		c += System.nanoTime() - ci;
	}
	static void di() {
		di = System.nanoTime();
	}
	static void d() {
		d += System.nanoTime() - di;
	}
	static void ei() {
		ei = System.nanoTime();
	}
	static void e() {
		e += System.nanoTime() - ei;
	}
	public static void stats() {
		System.out.println("a: " + a / 1e6);
		System.out.println("b: " + b / 1e6);
		System.out.println("c: " + c / 1e6);
		System.out.println("d: " + d / 1e6);
		System.out.println("e: " + e / 1e6);
		System.out.println();
		a = b = c = d = e = 0;
	}
	public int getdir(String input, FuseDirFiller dirFiller) throws FuseException {
		ai();
		Path path = Path.get(input);
		a(); bi();
		Directory d = mapper.getDir(path);
		b();
		if (d == null)
			return fuse.Errno.ENOENT;
		ci();
		Map<String,View> list = d.list();
		c(); di();
		for (String ref : list.keySet())
			dirFiller.add(ref, 0, list.get(ref).getFType());
		d(); ei();
		mapper.finish(path, list.keySet(), dirFiller);
		e();
		return 0;
	}

	public int mknod(String input, int mode, int rdev) throws FuseException {
		Path path = Path.get(input);
		String name = path.name();
		if (!canMknod(path.parent()) || name.startsWith("."))
			return fuse.Errno.EPERM;
		Directory d = mapper.getDir(path.parent());
		mapper.delete(path.parent(), false);
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
		mapper.notifyLatest(path, backend.create(groups, name));
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
		mapper.createFloat(path);
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

	public int rename(String fi, String ft) throws FuseException {
		Path from = Path.get(fi);
		Path to = Path.get(ft);
		View o = mapper.get(from);
		Directory d = mapper.getDir(from.parent());
		Directory dd = mapper.getDir(to.parent());
		if (o == null || d == null || dd == null)
			return fuse.Errno.ENOENT;
		else if (d != dd && (!d.getPerms().canMoveOut() || !dd.getPerms().canMoveIn()))
			return fuse.Errno.EPERM;
		else if (new File(to.value).getName().startsWith("."))
			return fuse.Errno.EPERM;
		if (o instanceof Node)
			mapper.delete(to.parent(), true);
		return o.rename(from.value, to.value, mapper.get(to), d.getQueryGroups(), dd.getQueryGroups());
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

// there is no point in optimizing/cache this - it takes c0.01 ms anyways
final class Path {
	public final String value;
	private static final Map<String,Path> cache = new HashMap<String,Path>();
	private String name;
	private Path parent;

	private static String parse(String path) {
		String[] parts = path.split("/");
		Stack<String> stack = new Stack<String>();
		for (int i=0; i < parts.length; i++) {
			String part = parts[i];
			if (part.equals("") || part.equals(".")) {
				// the same node
			} else if (part.equals("..")) {
				if (!stack.isEmpty())
					stack.pop();
			} else {
				stack.push(part);
			}
		}
		String ret = "";
		if (stack.isEmpty())
			ret = "/";
		else
			for (String part : stack)
				ret += "/" + part;
		return ret;
	}

	private Path(String path) {
		this.value = parse(path).intern();
	}

	public static Path get(String path) {
		Path p = cache.get(path);
		if (p != null)
			return p;
		p = new Path(path);
		cache.put(path, p);
		return p;
	}

	public Path parent() {
		if (parent != null)
			return parent;
		return parent = Path.get(new File(value).getParent());
	}

	public String name() {
		if (name != null)
			return name;
		return name = new File(value).getName();
	}

	public boolean equals(Object other) {
		return other instanceof Path && value.equals(((Path)other).value);
	}

	public int hashCode() {
		return value.hashCode();
	}
}

