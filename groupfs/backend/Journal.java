package groupfs.backend;

import java.util.*;

import groupfs.QueryGroup;

public class Journal {
	private Entry head;

	public Journal() {
		head = new Entry(null, new HashSet<QueryGroup>());
	}

	public Entry head() {
		return head;
	}

	public void log(Node node, Set<QueryGroup> updates) {
		Entry next = new Entry(node, updates);
		if (head != null)
			head.setNext(next);
		head = next;
	}
}
