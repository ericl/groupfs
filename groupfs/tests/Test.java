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

import groupfs.Group.Type;

import groupfs.state.*;
import groupfs.storage.*;

import static groupfs.Util.*;

public abstract class Test {
	protected boolean error = false;
	protected String log = "";
	protected FileSource source;

	public abstract void run();

	protected Manager getNewBackend() {
		return new Manager(source = new VirtualFileSource());
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

	protected void syn(Manager backend, String name, String ... tags) {
		Set<Group> groups = new HashSet<Group>();
		for (String tag : tags)
			groups.add(Group.create(tag, Type.TAG));
		groups.add(Group.create(extensionOf(name), Type.MIME));
		if (!hasCategory(groups)) {
			groups.clear();
			groups.add(Group.GROUP_NO_GROUP);
		}
		try {
			backend.create(groups, name);
		} catch (FuseException e) {
			throw new AssertionError(e);
		}
	}

	protected void expect_alternatives_ignoreHashTags(Filesystem fs, String[] dirs, String[] ... files) {
		expect_alternatives(fs, dirs, true, files);
	}

	protected void expect_alternatives(Filesystem fs, String[] dirs, String[] ... files) {
		expect_alternatives(fs, dirs, false, files);
	}

	private void expect_alternatives(Filesystem fs, String[] dirs, boolean stripTags, String[] ... files) {
		SortedSet<String> de = new TreeSet<String>(Arrays.asList(dirs));
		SortedSet<String> f = new TreeSet<String>();
		SortedSet<String> d = new TreeSet<String>();
		buildFileSet(fs, f, d, ".", stripTags);
		boolean ok = false;
		for (int i=0; i < files.length; i++) {
			SortedSet<String> fe = new TreeSet<String>(Arrays.asList(files[i]));
			if (fe.equals(f))
				ok = true;
			else {
				log += "expected: " + fe + "\n";
				log += "found:    " + f + "\n";
				error = true;
			}
		}
		if (ok) {
			error = false;
			log += "please ignore some of the above expect statements\n";
		}
		if (!de.equals(d)) {
			log += "expected: " + de + "\n";
			log += "found:    " + d + "\n";
			error = true;
		}
	}

	protected void expect_ignoreHashTags(Filesystem fs, String[] files, String[] dirs) {
		expect(fs, files, dirs, true);
	}

	protected void expect(Filesystem fs, String[] files, String[] dirs) {
		expect(fs, files, dirs, false);
	}

	private void expect(Filesystem fs, String[] files, String[] dirs, boolean stripTags) {
		expect_nocopy(fs, files, dirs, stripTags);
		if (error) {
			if (FilesystemTests.SHOWERR)
				System.out.println("first run failed, not rebuilding filesystem");
			return;
		}
		expect_nocopy(new Filesystem(new Manager(source)), files, dirs, stripTags);
	}

	protected void expect_nocopy(Filesystem fs, String[] files, String[] dirs) {
		expect_nocopy(fs, files, dirs, false);
	}

	protected void expect_nocopy_ignoreHashTags(Filesystem fs, String[] files, String[] dirs) {
		expect_nocopy(fs, files, dirs, true);
	}

	private void expect_nocopy(Filesystem fs, String[] files, String[] dirs, boolean stripTags) {
		SortedSet<String> fe = new TreeSet<String>(Arrays.asList(files));
		SortedSet<String> de = new TreeSet<String>(Arrays.asList(dirs));
		SortedSet<String> f = new TreeSet<String>();
		SortedSet<String> d = new TreeSet<String>();
		buildFileSet(fs, f, d, ".", stripTags);
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
		buildFileSet(fs, f, d, path, false);
	}

	protected void buildFileSet(Filesystem fs, Set<String> f, Set<String> d, String path, boolean stripTags) {
		TestDirFiller filler = new TestDirFiller();
		d.add(path);
		try {
			fs.getdir(path, filler);
			for (Node n : filler) {
				String realpath = path + "/" + n.name;
				if (n.dir) {
					buildFileSet(fs, f, d, realpath, stripTags);
				} else {
					f.add(stripTags ? stripHashTags(realpath) : realpath);
				}
			}
		} catch (FuseException e) {
			throw new RuntimeException(e);
		}
	}
}
