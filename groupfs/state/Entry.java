package groupfs.state;

import groupfs.Group;

import java.util.*;

public class Entry {
	private final Node node;
	private final Set<Group> updates;
	private long UUID = System.nanoTime() % 10000;
	private Entry next;

	public Entry(Node node, Set<Group> updates) {
		this.node = node;
		this.updates = Collections.unmodifiableSet(new HashSet<Group>(updates));
	}

	public Set<Group> getGroups() {
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
