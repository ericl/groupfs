package groupfs.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtype;

import groupfs.*;

import groupfs.QueryGroup.Type;

import groupfs.backend.*;

import static groupfs.Util.*;

public abstract class Test {
	protected boolean error = false;
	protected String log = "";
	protected FileSource source;

	public abstract void run();

	protected QueryBackend getNewBackend() {
		return new FlexibleBackend(source = new VirtualFileSource());
	}

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

	protected class TestDirFiller extends ArrayList<Node> implements FuseDirFiller {
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
			backend.raw_create(groups, name);
		} catch (FuseException e) {
			throw new AssertionError(e);
		}
	}

	protected void expect(Filesystem fs, String[] files, String[] dirs) {
		expect_nocopy(fs, files, dirs);
		if (error) {
			System.out.println("first run failed, not rebuilding filesystem");
			return;
		}
		expect_nocopy(new Filesystem(new FlexibleBackend(source)), files, dirs);
	}

	protected void expect_nocopy(Filesystem fs, String[] files, String[] dirs) {
		SortedSet<String> fe = new TreeSet<String>(Arrays.asList(files));
		SortedSet<String> de = new TreeSet<String>(Arrays.asList(dirs));
		SortedSet<String> f = new TreeSet<String>();
		SortedSet<String> d = new TreeSet<String>();
		buildFileSet(fs, f, d, ".");
		if (!fe.equals(f)) {
			log += "expected: " + fe + "\n";
			log += "found:    " + f + "\n";
			error = true;
		}
		if (!de.equals(d)) {
			log += "expected: " + de + "\n";
			log += "found:    " + d + "\n";
			error = true;
		}
	}

	protected void buildFileSet(Filesystem fs, Set<String> f, Set<String> d, String path) {
		TestDirFiller filler = new TestDirFiller();
		d.add(path);
		try {
			fs.getdir(path, filler);
			for (Node n : filler) {
				String realpath = path + "/" + n.name;
				if (n.dir) {
					buildFileSet(fs, f, d, realpath);
				} else {
					f.add(realpath);
				}
			}
		} catch (FuseException e) {
			throw new RuntimeException(e);
		}
	}
}
