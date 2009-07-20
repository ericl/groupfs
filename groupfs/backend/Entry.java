package groupfs.backend;

import groupfs.QueryGroup;

import java.util.*;

public class Entry {
	private final Node node;
	private final List<Update> updates;
	private final Set<QueryGroup> groups = new HashSet<QueryGroup>();
	private long UUID = System.nanoTime() % 10000;
	private Entry next;

	public Entry(Node node, List<Update> updates) {
		this.node = node;
		this.updates = Collections.unmodifiableList(new ArrayList<Update>(updates));
		for (Update u : updates)
			groups.add(u.group);
	}

	public List<Update> getUpdates() {
		return updates;
	}

	public Set<QueryGroup> getGroups() {
		return groups;
	}

	public Node getNode() {
		return node;
	}

	public void setNext(Entry next) {
		this.next = next;
	}

	public Entry getNext() {
		return next;
	}

	public String toString() {
		return "{\n uuid: " + UUID + ",\n node: " + node.getName() + ",\n groups: " + updates + "\n}";
	}
}
