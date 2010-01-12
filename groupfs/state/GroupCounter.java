package groupfs.state;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import groupfs.Group;

public class GroupCounter {
	Map<Group,Integer> map = new HashMap<Group,Integer>();
	int total;

	public Set<Group> positiveGroups() {
		return map.keySet();
	}

	public String toString() {
		return "total: " + total + ", " + map.entrySet();
	}

	public Set<Group> visibleGroups() {
		Set<Group> q = new HashSet<Group>();
		for (Map.Entry<Group,Integer> entry : map.entrySet())
			if (entry.getValue() < total)
				q.add(entry.getKey());
		q.remove(Group.GROUP_NO_GROUP);
		return q;
	}

	public void consider(Node n) {
		for (Group g : n.getGroups())
			incr(g);
		total++;
	}

	public void consider(Set<Group> plus, Set<Group> minus, Node n, int tmod) {
		for (Group g : plus)
			incr(g);
		for (Group g : minus)
			decr(g);
		total += tmod;
	}

	private void incr(Group g) {
		if (map.containsKey(g)) {
			int i = map.get(g) + 1;
			map.put(g, map.get(g) + 1);
		} else
			map.put(g, 1);
	}

	private void decr(Group g) {
		if (map.containsKey(g)) {
			int i = map.get(g) - 1;
			if (i < 1)
				map.remove(g);
			else
				map.put(g, i);
		} else {
			assert false;
		}
	}
}
