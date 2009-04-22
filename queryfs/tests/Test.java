package queryfs.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtype;

import queryfs.*;

import queryfs.QueryGroup.Type;

import queryfs.backend.*;

import static queryfs.Util.*;

public abstract class Test {
	protected boolean error = false;
	protected String log = "";

	public abstract void run();

	class Node {
		String name;
		boolean dir;

		public Node(String name, boolean dir) {
			this.name = name;
			this.dir = dir;
		}

		public String toString() {
			return name + (dir ? "/" : "");
		}
	}

	class TestDirFiller extends ArrayList<Node> implements FuseDirFiller {
		public static final long serialVersionUID = 193413423873L;
		public void add(String name, long inode, int mode) {
			super.add(new Node(name, (mode & FuseFtype.TYPE_DIR) > 0));
		}
	}

	public String toString() {
		String cl = this.getClass().getName();
		return cl.substring(cl.lastIndexOf(".") + 1);
	}

	protected void syn(QueryBackend backend, String name, String ... tags) {
		Set<QueryGroup> groups = new HashSet<QueryGroup>();
		for (String tag : tags)
			groups.add(QueryGroup.create(tag, Type.TAG));
		groups.add(QueryGroup.create(extensionOf(name), Type.MIME));
		if (!hasCategory(groups)) {
			groups.clear();
			groups.add(QueryGroup.GROUP_NO_GROUP);
		}
		try {
			backend.create(groups, name);
		} catch (FuseException e) {
			throw new AssertionError(e);
		}
	}

	protected void expect(Filesystem fs, List<String> files, List<String> dirs) {
		List<String> f = new ArrayList<String>();
		List<String> d = new ArrayList<String>();
		buildFileList(fs, f, d, ".");
		if (!files.equals(f)) {
			log += "expected: " + files + "\n";
			log += "found:    " + f + "\n";
			error = true;
		}
		if (!dirs.equals(d)) {
			log += "expected: " + dirs + "\n";
			log += "found:    " + d + "\n";
			error = true;
		}
	}

	private void buildFileList(Filesystem fs, List<String> f, List<String> d, String path) {
		TestDirFiller filler = new TestDirFiller();
		d.add(path);
		try {
			fs.getdir(path, filler);
			for (Node n : filler) {
				String realpath = path + "/" + n.name;
				if (n.dir) {
					buildFileList(fs, f, d, realpath);
				} else {
					f.add(realpath);
				}
			}
		} catch (FuseException e) {
			throw new RuntimeException(e);
		}
	}
}
