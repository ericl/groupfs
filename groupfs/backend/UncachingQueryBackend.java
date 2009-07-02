package groupfs.backend;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import groupfs.QueryGroup.Type;

import groupfs.QueryGroup;

import static groupfs.Util.*;

public abstract class UncachingQueryBackend implements QueryBackendWithCache {
	protected Set<QueryGroup> flagged = new HashSet<QueryGroup>();
	protected Set<Node> nodes = new HashSet<Node>();
	protected boolean rebuild_root;

	public Set<Node> query(Set<QueryGroup> groups) {
		if (groups.isEmpty())
			return nodes;
		assert maxOneMimeGroup(groups);
		Set<Node> output = new HashSet<Node>();
		// looking for a cached query 1 level up
		// (optimized for ever narrowing queries)
		Set<QueryGroup> selection = null;
		for (Set<QueryGroup> sel : broader(groups))
			selection = sel;
		Set<Node> pool = query(selection);
		for (Node node : pool)
			if (node.getQueryGroups().containsAll(groups))
				output.add(node);
		return Collections.unmodifiableSet(output);
	}

	public Set<QueryGroup> subclass(Set<QueryGroup> groups) {
		assert maxOneMimeGroup(groups);
		for (QueryGroup q : groups)
			if (q.getType() == Type.MIME)
				return QueryGroup.SET_EMPTY_SET;
		Set<Node> pool = query(groups);
		Map<QueryGroup,Integer> gcount = new HashMap<QueryGroup,Integer>();
		for (Node node : pool) {
			for (QueryGroup group : node.getQueryGroups()) {
				Integer n = gcount.get(group);
				gcount.put(group, n == null ? 1 : n + 1);
			}
		}
		Set<QueryGroup> output = new HashSet<QueryGroup>();
		for (QueryGroup g : gcount.keySet())
			if (!groups.contains(g) && (groups.isEmpty() || gcount.get(g) < pool.size()))
				output.add(g);
		return Collections.unmodifiableSet(output);
	}

	protected Set<Set<QueryGroup>> broader(Set<QueryGroup> input) {
		Set<Set<QueryGroup>> output = new HashSet<Set<QueryGroup>>();
		for (QueryGroup q : input) {
			Set<QueryGroup> t = new HashSet<QueryGroup>(input);
			t.remove(q);
			output.add(t);
		}
		return output;
	}

	public void flag(QueryGroup updated) {
		flagged.add(updated);
	}

	public void flush() {
		for (QueryGroup q : flagged)
			q.touch();
		flagged.clear();
	}

	public void unref(Node n) {
		nodes.remove(n);
	}

	public void checkRootRm(Set<QueryGroup> removed) {
		Set<QueryGroup> p = new HashSet<QueryGroup>();
		for (QueryGroup q : removed) {
			p.clear();
			p.add(q);
			if (query(p).isEmpty()) {
				rebuild_root = true;
				QueryGroup.touchRoot();
				flush();
				return;
			}
		}
	}

	public void checkRootAdd(Set<QueryGroup> added) {
		Set<QueryGroup> p = new HashSet<QueryGroup>();
		for (QueryGroup q : added) {
			p.clear();
			p.add(q);
			if (query(p).size() == 1) {
				rebuild_root = true;
				QueryGroup.touchRoot();
				flush();
				return;
			}
		}
	}
}
