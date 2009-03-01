package queryfs;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import fuse.FuseException;

import queryfs.QueryGroup.Type;

import static queryfs.Util.*;

class DirectoryQueryBackend implements QueryBackend {
	public final File root;
	private Collection<Node> nodes = new ArrayList<Node>();
	private QueryGroupManager manager = new QueryGroupManager();

	public DirectoryQueryBackend(File origin) {
		assert origin.isDirectory();
		root = origin;
		scan(root);
	}

	private void scan(File dir) {
		for (File child : dir.listFiles()) {
			if (!child.canRead())
				continue;
			if (child.isDirectory())
				scan(child);
			else
				nodes.add(fileToNode(child));
		}
	}

	private Node fileToNode(File file) {
		assert !file.isDirectory();
		String path = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
		Set<String> tags = new HashSet<String>(
			Arrays.asList(path.split("/"))
		);
		tags.remove(file.getName());
		tags.remove("");
		Set<QueryGroup> groups = new HashSet<QueryGroup>();
		for (String tag : tags)
			groups.add(manager.create(tag, Type.TAG));
		String ext = extensionOf(file);
		if (ext != null)
			groups.add(manager.create(ext, Type.MIME));
		return new Node(root, file, groups);
	}

	public Node create(Set<QueryGroup> groups, String name) throws FuseException {
		File file = null;
		try {
			file = getDestination(newPath(root, groups), name);
			file.createNewFile();
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
		return new Node(root, file, groups);
	}

	// TODO implement the vfs-like structure for these:
	public Set<Node> query(Set<QueryGroup> groups) {
	}
	public Set<QueryGroup> subclass(Set<QueryGroup> groups) {
	}
}
