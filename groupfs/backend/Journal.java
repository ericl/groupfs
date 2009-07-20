package groupfs.backend;

import java.util.*;

public class Journal {
	private Entry head;

	public Journal() {
		head = new Entry(null, new ArrayList<Update>());
	}

	public Entry head() {
		return head;
	}

	public void log(Node node, List<Update> updates) {
		Entry next = new Entry(node, updates);
		if (head != null)
			head.setNext(next);
		head = next;
	}
}
