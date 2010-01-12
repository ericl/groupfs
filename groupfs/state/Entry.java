package groupfs.state;

import groupfs.Group;

import java.util.*;

public class Entry {
	private Entry next;
	public final Node node;
	public final Set<Group> prev, current;

	public Entry(Node node, Set<Group> prev, Set<Group> current) {
		this.node = node;
		this.prev = Collections.unmodifiableSet(new HashSet<Group>(prev));
		this.current = Collections.unmodifiableSet(new HashSet<Group>(current));
	}

	public void setNext(Entry next) {
		this.next = next;
	}

	public Entry getNext() {
		return next;
	}

	public String toString() {
		return node + " " + prev + " " + current;
	}
}
