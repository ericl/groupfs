package queryfs.backend;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fuse.FuseException;

import queryfs.QueryGroup.Type;

import queryfs.QueryGroup;

import static queryfs.Util.*;

public abstract class CachingQueryBackend implements QueryBackend {
	protected QueryCache cache = new QueryCache();
	protected Set<QueryGroup> flagged = new HashSet<QueryGroup>();
	protected Set<Node> nodes = new HashSet<Node>();
	protected boolean rebuild_root;

	private class QueryCache {
		// keyindex is shared by nodecache and groupcache
		Map<QueryGroup,Set<Set<QueryGroup>>> keyIndex = new HashMap<QueryGroup,Set<Set<QueryGroup>>>();
		Map<Set<QueryGroup>,Set<Node>> nodeCache = new HashMap<Set<QueryGroup>,Set<Node>>();
		Map<Set<QueryGroup>,Set<QueryGroup>> groupCache = new HashMap<Set<QueryGroup>,Set<QueryGroup>>();

		Set<Node> getNodes(Set<QueryGroup> groups) {
			if (groups.isEmpty())
				return nodes;
			return nodeCache.get(groups);
		}

		Set<QueryGroup> getGroups(Set<QueryGroup> groups) {
			if (groups.isEmpty() && rebuild_root) {
				groupCache.remove(groups);
				rebuild_root = false;
			}
			return groupCache.get(groups);
		}

		void putNodes(Set<QueryGroup> key, Set<Node> value) {
			if (key.isEmpty()) // handled specially
				return;
			assert maxOneMimeGroup(key);
			nodeCache.put(key, value);
			for (QueryGroup group : key) {
				Set<Set<QueryGroup>> related = keyIndex.get(group);
				if (related == null) {
					related = new HashSet<Set<QueryGroup>>();
					keyIndex.put(group, related);
				}
				related.add(key);
			}
		}

		void putGroups(Set<QueryGroup> key, Set<QueryGroup> value) {
			assert maxOneMimeGroup(key);
			groupCache.put(key, value);
			for (QueryGroup group : key) {
				Set<Set<QueryGroup>> related = keyIndex.get(group);
				if (related == null) {
					related = new HashSet<Set<QueryGroup>>();
					keyIndex.put(group, related);
				}
				related.add(key);
			}
		}

		void drop(Set<QueryGroup> groups) {
			for (QueryGroup group : groups) {
				Set<Set<QueryGroup>> related = keyIndex.get(group);
				if (related != null) {
					for (Set<QueryGroup> key : related) {
						nodeCache.remove(key);
						groupCache.remove(key);
					}
				}
				keyIndex.remove(group);
			}
		}
	}

	public Set<Node> query(Set<QueryGroup> groups) {
		if (groups.isEmpty())
			return nodes;
		assert maxOneMimeGroup(groups);
		Set<Node> output = cache.getNodes(groups);
		if (output == null) {
			output = new HashSet<Node>();
			// looking for a cached query 1 level up
			// (optimized for ever narrowing queries)
			Set<QueryGroup> selection = null;
			for (Set<QueryGroup> sel : broader(groups)) {
				selection = sel;
				if (cache.getNodes(sel) != null)
					break; // yes!
			}
			Set<Node> pool = query(selection);
			for (Node node : pool)
				if (node.getQueryGroups().containsAll(groups))
					output.add(node);
			cache.putNodes(groups, output);
		}
		return Collections.unmodifiableSet(output);
	}

	public Set<QueryGroup> subclass(Set<QueryGroup> groups) {
		assert maxOneMimeGroup(groups);
		for (QueryGroup q : groups)
			if (q.getType() == Type.MIME)
				return new HashSet<QueryGroup>();
		Set<QueryGroup> output = cache.getGroups(groups);
		if (output == null) {
			Set<Node> pool = query(groups);
			Map<QueryGroup,Integer> gcount = new HashMap<QueryGroup,Integer>();
			for (Node node : pool) {
				for (QueryGroup group : node.getQueryGroups()) {
					Integer n = gcount.get(group);
					gcount.put(group, n == null ? 1 : n + 1);
				}
			}
			output = new HashSet<QueryGroup>();
			for (QueryGroup g : gcount.keySet())
				if (!groups.contains(g) && (groups.isEmpty() || gcount.get(g) < pool.size()))
					output.add(g);
			cache.putGroups(groups, output);
		}
		return Collections.unmodifiableSet(output);
	}

	public abstract void create(Set<QueryGroup> groups, String name) throws FuseException;

	protected Set<Set<QueryGroup>> broader(Set<QueryGroup> input) {
		Set<Set<QueryGroup>> output = new HashSet<Set<QueryGroup>>();
		for (QueryGroup q : input) {
			Set<QueryGroup> t = new HashSet<QueryGroup>(input);
			t.remove(q);
			output.add(t);
		}
		return output;
	}

	protected void flag(QueryGroup updated) {
		flagged.add(updated);
	}

	protected void flush() {
		cache.drop(flagged);
		for (QueryGroup q : flagged)
			q.touch();
		flagged.clear();
	}

	protected void checkRoot(Set<QueryGroup> removed) {
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

	protected void unref(Node n) {
		nodes.remove(n);
	}

	protected void checkRootAdd(Set<QueryGroup> added) {
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
