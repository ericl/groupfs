package groupfs.state;

import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.Group;

import groupfs.state.Manager;

/**
 * Hard link for directories. The target directory provides the name mapper
 * and associated data structures, while the link has its own update time,
 * parent, and group.
 *
 * Used transparently by the filesystem implementation to eliminate
 * duplicate computation for the contents of, say, /A/B vs /B/A.
 */
public class Link extends SubDirectory {
	private final SubDirectory link;

	public Link(Manager backend, BaseDirectory parent, Group group, SubDirectory link) {
		super(backend, parent, group);
		this.link = link;
		mapper = link.mapper;
		queued = null; // not used
		head = null; // not used
	}

	/**
	 * Delegates the update() operation to the link target.
	 */
	protected void update() {
		link.update();
	}
}
