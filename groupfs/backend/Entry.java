package groupfs.backend;

import groupfs.QueryGroup;

import java.util.*;

public class Entry {
	private final Node node;
	private final Set<QueryGroup> updates;
	private long UUID = System.nanoTime() % 10000;
	private Entry next;

	public Entry(Node node, Set<QueryGroup> updates) {
		this.node = node;
		this.updates = Collections.unmodifiableSet(new HashSet<QueryGroup>(updates));
	}

	public Set<QueryGroup> getGroups() {
		return updates;
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
