package queryfs;

import java.io.File;
import java.io.IOException;

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
import fuse.FuseStatfsSetter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import queryfs.QueryGroup.Type;

import static queryfs.Util.*;

public class Filesystem implements Filesystem3 {
	public static final Log log = LogFactory.getLog(Filesystem.class);
	private QueryBackend backend;
	private ViewMapper mapper;

	protected class ViewMapper {
		private final RootDirectory root;
		private Map<String,FloatingDirectory> floats = new HashMap<String,FloatingDirectory>();
		private Map<String,List<FloatingDirectory>> parents = new HashMap<String,List<FloatingDirectory>>();

		public ViewMapper(RootDirectory root) {
			this.root = root;
		}

		public void createFloat(String path) {
			String parent = new File(path).getParent();
			String name = new File(path).getName();
			log.info("Creating floating directory " + path);
			FloatingDirectory f = new FloatingDirectory(this, parent, path, backend.getManager().create(name, Type.TAG));
			floats.put(path, f);
			List<FloatingDirectory> s = parents.get(parent);
			if (s == null) {
				s = new LinkedList<FloatingDirectory>();
				parents.put(parent, s);
			}
			s.add(f);
		}

		public void delete(String path, boolean recursive) {
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

		public void finish(String parent, Set<String> taken, FuseDirFiller filler) {
			List<FloatingDirectory> s = parents.get(parent);
			if (s != null)
				for (FloatingDirectory f : s) {
					String name = f.getGroup().getValue();
					if (!taken.contains(name))
						filler.add(name, 0, f.getFType());
				}
		}

		public View get(String path) {
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
			if (output == null)
				output = floats.get(path);
			else
				delete(path, false); // garbage collect those floatingdirs
			return output;
		}

		public Directory getDir(String path) {
			View dir = get(path);
			if (dir instanceof Directory)
				return (Directory)dir;
			else
				return null;
		}

		public Node getNode(String path) {
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
		backend = new QueryBackend(originDir);
		mapper = new ViewMapper(new RootDirectory(backend));
	}

	public int getattr(String path, FuseGetattrSetter getattrSetter) throws FuseException {
		View o = mapper.get(path);
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
		log.info("MKNOD " + path);
		String name = new File(path).getName();
		Directory d = mapper.getDir(new File(path).getParent());
		if (d == null)
			return fuse.Errno.ENOENT;
		Set<QueryGroup> groups = d.getQueryGroups();
		if (groups.isEmpty())
			return fuse.Errno.EPERM;
		String ext = extensionOf(new File(path));
		if (ext != null)
			groups.add(backend.getManager().create(ext, Type.MIME));
		backend.create(groups, name);
		return 0;
	}

	public int mkdir(String path, int mode) throws FuseException {
		String name = new File(path).getName();
		if (name.startsWith("."))
			return fuse.Errno.EACCES;
		View v = mapper.get(path);
		if (v != null && v instanceof Node)
			return fuse.Errno.EACCES;
		Directory parent = mapper.getDir(new File(path).getParent());
		if (parent == null)
			return fuse.Errno.ENOENT;
		mapper.createFloat(path);
		return 0;
	}

	public int unlink(String path) throws FuseException {
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		else
			n.unlink();
		return 0;
	}

	public int rmdir(String path) throws FuseException {
		Directory d = mapper.getDir(path);
		if (d == null)
			return fuse.Errno.ENOENT;
		else
			d.delete();
		log.info(d);
		return 0;
	}

	public int symlink(String from, String to) throws FuseException {
		return fuse.Errno.ENOTSUPP;
	}

	public int rename(String from, String to) throws FuseException {
		if (new File(to).getName().startsWith("."))
			return fuse.Errno.EPERM;
		View o = mapper.get(from);
		if (o == null)
			return fuse.Errno.ENOENT;
		return o.rename(from, to, mapper.get(to));
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
		n.truncate(size);
		return 0;
	}

	public int utime(String path, int atime, int mtime) throws FuseException {
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		n.setModified(mtime);
		return 0;
	}

	public int statfs(FuseStatfsSetter statfsSetter) throws FuseException {
		statfsSetter.set(
			1024, // blockSize
			Integer.MAX_VALUE, // blocks
			Integer.MAX_VALUE, // blocksFree
			Integer.MAX_VALUE, // blocksAvail
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
		n.open(flags);
		return 0;
	}

	public int read(String path, Object fh, ByteBuffer buf, long offset) throws FuseException {
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		n.read(buf, offset);
		return 0;
	}

	public int write(String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException {
		Node n = mapper.getNode(path);
		if (n == null)
			return fuse.Errno.ENOENT;
		n.write(buf, offset);
		return 0;
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
}
