package groupfs.state;

import java.util.*;

import groupfs.Group;

public class Journal {
	private Entry head = new Entry(null, new HashSet<Group>(), new HashSet<Group>());

	public Entry head() {
		return head;
	}

	public void log(Node node, Set<Group> prev, Set<Group> current) {
		Entry next = new Entry(node, prev, current);
		head.setNext(next);
		head = next;
	}
}
