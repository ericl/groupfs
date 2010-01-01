package groupfs;

import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.backend.DataProvider;

/**
 * Hard link for directories. The target directory provides the name mapper
 * and associated data structures, while the link has its own update time,
 * parent, and group.
 *
 * Used transparently by the filesystem implementation to eliminate
 * duplicate computation for the contents of, say, /A/B vs /B/A.
 */
public class Link extends SubclassingDirectory {
	private final SubclassingDirectory link;

	public Link(DataProvider backend, JournalingDirectory parent, QueryGroup group, SubclassingDirectory link) {
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
