package groupfs;

import groupfs.backend.Node;

/**
 * Maps full filesystem paths to inodes.
 */
public class PathMapper {
	private final Directory root;

	PathMapper(Directory root) {
		this.root = root;
	}

	/**
	 * XXX cache an inode to support operations like
	 *	mknod() and then accessing the node
	 *	while the underlying directory structure has shifted
	 * This is impossible otherwise since there is no way
	 * to know if it is ok to let the structures shift
	 * in the middle of a sequence of operations.
	 */
	private Path latestPath;
	private Node latestNode;

	/**
	 * XXX support operations on new nodes
	 * @param	path	Path of the new node.
	 * @param	node	Node of the new node.
	 */
	void notifyLatest(Path path, Node node) {
		latestPath = path;
		latestNode = node;
	}

	/**
	 * @return Inode of path or null.
	 */
	Inode get(Path path) {
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
		Inode output = null;
		if (parent_ok)
			output = directory.get(parts[parts.length-1]);
		return output;
	}

	/**
	 * @return Directory at path iff inode is a directory.
	 */
	Directory getDir(Path path) {
		Inode dir = get(path);
		if (dir instanceof Directory)
			return (Directory)dir;
		else
			return null;
	}

	/**
	 * @return File node at path iff inode is a file node.
	 */
	Node getNode(Path path) {
		Inode dir = get(path);
		if (dir instanceof Node)
			return (Node)dir;
		else
			return null;
	}
}

