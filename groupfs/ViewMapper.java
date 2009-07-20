package groupfs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fuse.FuseDirFiller;

import groupfs.QueryGroup.Type;

import groupfs.backend.Node;

public class ViewMapper {
	private final Directory root;
	private Path latestPath;
	private Node latestNode;
	private Map<Path,FloatingDirectory> floats = new HashMap<Path,FloatingDirectory>();
	private Map<Path,List<FloatingDirectory>> parents = new HashMap<Path,List<FloatingDirectory>>();

	ViewMapper(Directory root) {
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

