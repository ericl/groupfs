package queryfs;

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

import queryfs.QueryGroup.Type;

import queryfs.backend.DirectoryFileSource;
import queryfs.backend.FileSource;
import queryfs.backend.FlexibleBackend;
import queryfs.backend.Node;
import queryfs.backend.QueryBackend;

import static queryfs.Util.*;

public class Filesystem implements Filesystem3, XattrSupport {
	public static final Log log = LogFactory.getLog(Filesystem.class);
	public static final int blksize = 1024;
	private QueryBackend backend;
	private ViewMapper mapper;

	// TODO make this method all-encompassing and robust
	// TODO find all places where necessary in viewmapper
	static String standardize(String path) {
		if (path.startsWith(".")) {
			path = path.substring(1);
			if (path.isEmpty())
				path = "/";
		}
		return path;
	}

	protected class ViewMapper {
		private final RootDirectory root;
		private String latestPath;
		private Node latestNode;
		private Map<String,FloatingDirectory> floats = new HashMap<String,FloatingDirectory>();
		private Map<String,List<FloatingDirectory>> parents = new HashMap<String,List<FloatingDirectory>>();

		ViewMapper(RootDirectory root) {
			this.root = root;
		}

		void createFloat(String path) {
			String parent = new File(path).getParent();
			String name = new File(path).getName();
			FloatingDirectory f = new FloatingDirectory(this, parent, path, QueryGroup.create(name, Type.TAG));
			floats.put(path, f);
			List<FloatingDirectory> s = parents.get(parent);
			if (s == null) {
				s = new LinkedList<FloatingDirectory>();
				parents.put(parent, s);
			}
			s.add(f);
		}

		void delete(String path, boolean recursive) {
			FloatingDirectory target = floats.remove(path);
			if (target == null)
				return;
			List<FloatingDirectory> l = parents.get(target.getHost());
			l.remove(target);
			if (l.isEmpty())
				parents.remove(target.getHost());
			if (recursive)
				for (String test : new HashSet<String>(floats.keySet()))
					if (test.startsWith(path))
						delete(test, false);
		}

		void remap(String from, String to) {
			delete(from, false);
			createFloat(to);
			for (String test : new HashSet<String>(floats.keySet()))
				if (test.startsWith(from)) {
					delete(test, false);
					createFloat(test.replaceFirst(from, to));
				}
		}

		void finish(String parent, Set<String> taken, FuseDirFiller filler) {
			parent = standardize(parent);
			List<FloatingDirectory> s = parents.get(parent);
			if (s != null)
				for (FloatingDirectory f : s) {
					String name = f.getGroup().getValue();
					if (!taken.contains(name))
						filler.add(name, 0, f.getFType());
				}
		}

		void notifyLatest(String path, Node node) {
			latestPath = path;
			latestNode = node;
		}

		View get(String path) {
			if (path.equals(latestPath))
				return latestNode;
			String[] parts = path.split("/");
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
			if (output == null) {
				path = standardize(path);
				output = floats.get(path);
			}
			else
				delete(path, false); // garbage collect those floatingdirs
			return output;
		}

		Directory getDir(String path) {
			View dir = get(path);
			if (dir instanceof Directory)
				return (Directory)dir;
			else
				return null;
		}

		Node getNode(String path) {
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
		View o = mapper.get(path);
		if (o == null)
			return fuse.Errno.ENOENT;
		else
			return o.stat(getattrSetter);
	}

	public int readlink(String path, CharBuffer link) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int getdir(String path, FuseDirFiller dirFiller) throws FuseException {
		Directory d = mapper.getDir(path);
		if (d == null)
			return fuse.Errno.ENOENT;
		Map<String,View> list = d.list();
		for (String ref : list.keySet())
			dirFiller.add(ref, 0, list.get(ref).getFType());
		mapper.finish(path, list.keySet(), dirFiller);
		return 0;
	}

	public int mknod(String path, int mode, int rdev) throws FuseException {
		String name = new File(path).getName();
		Directory d = mapper.getDir(new File(path).getParent());
		if (d == null)
			return fuse.Errno.ENOENT;
		else if (!d.getPerms().canMknod() || name.startsWith("."))
			return fuse.Errno.EPERM;
		mapper.delete(new File(path).getParent(), false);
		Set<QueryGroup> groups = new HashSet<QueryGroup>(d.getQueryGroups());
		String ext = extensionOf(new File(path));
		if (ext != null)
			groups.add(QueryGroup.create(ext, Type.MIME));
		mapper.notifyLatest(path, backend.create(groups, name));
		return 0;
	}

	public int mkdir(String path, int mode) throws FuseException {
		String name = new File(path).getName();
		if (name.startsWith("."))
			return fuse.Errno.EPERM;
		Directory parent = mapper.getDir(new File(path).getParent());
		if (parent == null)
			return fuse.Errno.ENOENT;
		else if (!parent.getPerms().canMkdir())
			return fuse.Errno.EPERM;
		mapper.createFloat(path);
		return 0;
	}

	public int unlink(String path) throws FuseException {
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		Directory d = mapper.getDir(new File(path).getParent());
		if (d == null)
			return fuse.Errno.ENOENT;
		else if (d.getGroup() == QueryGroup.GROUP_NO_GROUP) {
			return n.deleteFromBackingMedia();
		} else if (!d.getPerms().canDeleteNode())
			return fuse.Errno.EPERM;
		return n.unlink();
	}

	public int rmdir(String path) throws FuseException {
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

	public int rename(String from, String to) throws FuseException {
		View o = mapper.get(from);
		Directory d = mapper.getDir(new File(from).getParent());
		Directory dd = mapper.getDir(new File(to).getParent());
		if (o == null || d == null || dd == null)
			return fuse.Errno.ENOENT;
		else if (d != dd && (!d.getPerms().canMoveOut() || !dd.getPerms().canMoveIn()))
			return fuse.Errno.EPERM;
		else if (new File(to).getName().startsWith("."))
			return fuse.Errno.EPERM;
		if (o instanceof Node)
			mapper.delete(new File(to).getParent(), true);
		return o.rename(from, to, mapper.get(to), d.getQueryGroups(), dd.getQueryGroups());
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
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		return n.truncate(size);
	}

	public int utime(String path, int atime, int mtime) throws FuseException {
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

	public int open(String path, int flags, FuseOpenSetter openSetter) throws FuseException {
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		// not really opening file; flags aren't supported by backend anyways
		return 0;
	}

	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		return n.read(buf, offset);
	}

	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		return n.write(buf, offset);
	}

	public int flush(String path, Object fh) throws FuseException {
		return 0;
	}

	public int release(String path, Object fh, int flags) throws FuseException {
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
