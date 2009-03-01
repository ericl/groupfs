package queryfs;

import java.util.Set;

import fuse.FuseException;

public interface QueryBackend {
	public Node create(Set<QueryGroup> groups, String name) throws FuseException;

	/**
	 * @return All nodes under the given groups.
	 */
	public Set<Node> query(Set<QueryGroup> groups);

	/**
	 * @return All groups under the given groups.
	 */
	public Set<QueryGroup> subclass(Set<QueryGroup> groups);
}
