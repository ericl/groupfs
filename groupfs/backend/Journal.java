package groupfs.backend;

import java.util.*;

import groupfs.Group;

public class Journal {
	private Entry head = new Entry(null, new HashSet<Group>());

	public Entry head() {
		return head;
	}

	public void log(Node node, Set<Group> updates) {
		Entry next = new Entry(node, updates);
		head.setNext(next);
		head = next;
	}
}
