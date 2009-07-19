package groupfs.backend;

import java.util.*;

public class Entry {
	private final Node node;
	private final List<Update> updates;
	private Entry next;

	public Entry(Node node, List<Update> updates) {
		this.node = node;
		this.updates = Collections.unmodifiableList(new ArrayList<Update>(updates));
	}

	public List<Update> getUpdates() {
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
}

