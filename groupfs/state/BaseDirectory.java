package groupfs.state;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.Directory;
import groupfs.Permissions;
import groupfs.Group;
import groupfs.Path;
import groupfs.Inode;
import groupfs.Group.Type;

import groupfs.state.Entry;
import groupfs.state.Manager;
import groupfs.state.Node;

import static groupfs.Util.*;

/**
 * The root directory of the filesystem. It is a special case in that
 * no file nodes are permitted to exist in it, only directories.
 * Each subdirectory and itself is lazily updated to guarantee
 * logical consistency from an outside viewpoint.
 * See README for filesystem axioms.
 */
public class BaseDirectory implements Directory {
	protected Entry head;
	protected Manager backend;
	protected GroupCounter counter = new GroupCounter();
	protected NameMapper mapper;
	protected long time = System.currentTimeMillis();
	protected boolean populated;
	protected static Permissions root_perms = new Permissions(
		false, false, false, true, true, true, false
	);

	public BaseDirectory(Manager backend) {
		this.backend = backend;
		mapper = new NameMapper(backend);
	}

	public Permissions getPerms() {
		return root_perms;
	}

	public int getFType() {
		return FuseFtype.TYPE_DIR;
	}

	public int setModified(long mtime) throws FuseException {
		time = mtime;
		return 0;
	}

	public int rename(Path from, Path to, Inode target, Directory orig, Directory dest) throws FuseException {
		return fuse.Errno.EPERM;
	}

	public int delete() throws FuseException {
		return fuse.Errno.EPERM;
	}

	/**
	 * @return All nodes for which node.getGroups() contains this.getGroups().
	 * The set is guaranteed to be correct set in light of
	 * recent filesystem operations.
	 */
	protected Set<Node> getPool() {
		return getPoolDirect();
	}

	/**
	 * @return All nodes for which node.getGroups() contains this.getGroups().
	 * The set is *NOT* guaranteed to be correct in light of
	 * recent filesystem opreations.
	 */
	protected Set<Node> getPoolDirect() {
		return backend.getAll();
	}

	/**
	 * Maps all nodes that belong to this directory.
	 * @return True if the nodes were not previously mapped.
	 */
	protected boolean populateSelf() {
		if (!populated) {
			long x = System.currentTimeMillis();
			for (Node n : getPoolDirect())
				counter.consider(n);
			for (Group g : counter.positiveGroups())
				mapper.map(backend.directoryInstance(this, g));
			populated = true;
			time = System.currentTimeMillis();
			head = backend.journal.head(); // we're up to date
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return Inode mapped to name if any, consistent with recent fs ops.
	 * @param name Any value.
	 */
	public Inode get(String name) {
		update();
		return mapper.inodeMap().get(name);
	}

	/**
	 * @return Directory mapped to name if any, consistent with recent fs ops.
	 * @param name Any value.
	 */
	public Directory getDir(String name) {
		Inode v = get(name);
		if (v != null && v instanceof Directory)
			return (Directory)v;
		return null;
	}

	/**
	 * Provides information similar to the stat() system call.
	 */
	public int stat(FuseGetattrSetter setter) {
		int mtime = (int)(time / 1000L);
		setter.set(
			0, // inode
			FuseFtype.TYPE_DIR | 0755,
			1, // nlink
			UID,
			GID,
			0, // rdev
			0,
			0,
			mtime, mtime, mtime // atime, mtime, ctime
		);
		return 0;
	}

	/**
	 * @return Group represented by this directory - always null for root dir.
	 * Information guaranteed consistent with fs ops.
	 */
	public Group getGroup() {
		return null;
	}

	/**
	 * @return Group of this directory and all parents up to the root dir.
	 * Information guaranteed consistent with fs ops.
	 */
	public Set<Group> getGroups() {
		Set<Group> x = new HashSet<Group>();
		Directory p = this;
		Group g = getGroup();
		while (p != null && g != null) {
			x.add(g);
			p = p.getParent();
			g = p.getGroup();
		}
		return x;
	}

	/**
	 * @return Parent directory, always null in case of root directory.
	 */
	public Directory getParent() {
		return null;
	}

	/**
	 * It is forbidden to modify the map returned by this method.
	 * @return Underlying map of names to inodes.
	 */
	public Map<String,Inode> list() {
		update();
		return mapper.inodeMap();
	}

	/**
	 * Maps new directory that represents the group.
	 */
	public void mkdir(Group g) {
		mapper.map(new SubDirectory(backend, this, g));
	}

	/**
	 * Guarantees that the directory contents are consistent in light of
	 * recent filesystem operations.
	 */
	public void update() {
		if (!populateSelf() && head != backend.journal.head()) {
			replayJournal();
			time = System.currentTimeMillis();
		}
	}

	private boolean analyze_was_here(Entry e) {
		return e.prev.containsAll(getGroups());
	}

	private boolean analyze_now_here(Entry e) {
		return e.current.containsAll(getGroups());
	}

	private Set<Group> analyze_new_groups(Entry e) {
		Set<Group> p = new HashSet<Group>(e.current);
		p.removeAll(e.prev);
		return p;
	}

	private Set<Group> analyze_removed_groups(Entry e) {
		Set<Group> p = new HashSet<Group>(e.prev);
		p.removeAll(e.current);
		return p;
	}

	/**
	 * Reads log of recent filesystem operations and modifies
	 * directory contents to maintain logical consistency.
	 * No-op if up-to-date.
	 */
	protected void replayJournal() {
		assert head != null;
		boolean counter_changed = false, root = getGroup() == null;
		while (head != backend.journal.head()) {
			Entry next = head.getNext();
			head = next;
			if (head != null && head.node != null) {
				boolean node_was_here = analyze_was_here(head);
				boolean node_now_here = analyze_now_here(head);
				Set<Group> new_groups = analyze_new_groups(head);
				Set<Group> removed_groups = analyze_removed_groups(head);
				if (node_was_here) {
					if (node_now_here) {
						if (!root) {
							mapper.unmap(head.node);
							mapper.map(head.node);
						}
						counter.consider(new_groups, removed_groups, head.node, 0);
						counter_changed = true;
					} else {
						if (!root)
							mapper.unmap(head.node);
						counter.consider(new_groups, removed_groups, head.node, -1);
						counter_changed = true;
					}
				} else if (node_now_here) {
					if (!root)
						mapper.map(head.node);
					counter.consider(head.node.getGroups(), Group.SET_EMPTY_SET, head.node, +1);
					counter_changed = true;
				}
			}
		}
		if (counter_changed && (root || getGroup().type != Type.MIME)) {
			Set<Group> allowed = root ? counter.positiveGroups() : counter.visibleGroups();
			for (Group g : new HashSet<Group>(mapper.getGroups()))
				if (!allowed.contains(g))
					mapper.unmap(g);
			for (Group g : allowed)
				if (!mapper.contains(g))
					mapper.map(backend.directoryInstance(this, g));
		}
	}
}
