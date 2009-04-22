package queryfs.backend;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import fuse.FuseException;

import queryfs.QueryGroup.Type;

import queryfs.QueryGroup;

import static queryfs.Util.*;

public class DirectoryQueryBackend extends CachingQueryBackend {
	private final File root;

	public DirectoryQueryBackend(File origin) {
		assert origin.isDirectory();
		root = origin;
		scan(root);
	}

	public void create(Set<QueryGroup> groups, String name) throws FuseException {
		File file = null;
		try {
			file = getDestination(newPath(root, groups), name);
			file.createNewFile();
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
		assert maxOneMimeGroup(groups);
		flagged.addAll(groups);
		flush();
		nodes.add(new DirectoryBackedNode(this, file, groups));
		checkRootAdd(groups);
	}

	public long getFreeSpace() {
		return root.getFreeSpace();
	}

	public long getUsableSpace() {
		return root.getUsableSpace();
	}

	public long getTotalSpace() {
		return root.getTotalSpace();
	}

	protected File getRoot() {
		return root;
	}

	private void scan(File dir) {
		for (File child : dir.listFiles()) {
			if (!child.canRead())
				continue;
			if (child.isDirectory()) {
				if (!child.getName().startsWith("."))
					scan(child);
			} else {
				nodes.add(fileToNode(child));
			}
		}
	}

	private Node fileToNode(File file) {
		assert !file.isDirectory();
		String a = file.getParentFile().getAbsolutePath();
		String b = root.getAbsolutePath();
		Set<QueryGroup> groups = new HashSet<QueryGroup>();
		if (a.equals(b)) {
			groups.add(QueryGroup.GROUP_NO_GROUP);
			return new DirectoryBackedNode(this, file, groups);
		}
		String path = a.substring(b.length() + 1);
		Set<String> tags = new HashSet<String>(
			Arrays.asList(path.split("/"))
		);
		tags.remove("");
		for (String tag : tags)
			groups.add(QueryGroup.create(tag, Type.TAG));
		groups.add(QueryGroup.create(extensionOf(file), Type.MIME));
		assert maxOneMimeGroup(groups);
		return new DirectoryBackedNode(this, file, groups);
	}
}
