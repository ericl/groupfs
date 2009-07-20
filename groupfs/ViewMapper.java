package groupfs;

import groupfs.backend.Node;

public class ViewMapper {
	private final Directory root;
	private Path latestPath;
	private Node latestNode;

	ViewMapper(Directory root) {
		this.root = root;
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

