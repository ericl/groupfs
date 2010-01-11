package groupfs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.Group.Type;

import groupfs.backend.Entry;
import groupfs.backend.DataProvider;
import groupfs.backend.Node;

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
	protected DataProvider backend;
	protected Set<Group> queued = new HashSet<Group>();
	protected NameMapper mapper;
	protected long time = System.currentTimeMillis();
	protected boolean populated;
	protected static Permissions root_perms = new Permissions(
		false, false, false, true, true, true, false
	);

	public BaseDirectory(DataProvider backend) {
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
			for (Group group : backend.findAllGroups())
				mapper.map(backend.get(this, group));
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
	protected void update() {
		if (!populateSelf() && head != backend.journal.head()) {
			replayJournal();
			time = System.currentTimeMillis();
		}
	}

	/**
	 * Reads log of recent filesystem operations and modifies
	 * directory contents to maintain logical consistency.
	 * No-op if up-to-date.
	 */
	protected void replayJournal() {
		assert head != null;
		while (head != backend.journal.head()) {
			Entry next = head.getNext();
			head = next;
			if (isPertinent(head))
				process(head);
		}
		process_queued();
	}

	/**
	 * @return True if a filesystem operations possibly affect directory contents.
	 * @param e Any value.
	 */
	protected boolean isPertinent(Entry e) {
		return e != null && e.getNode() != null && e.getGroups().containsAll(getGroups());
	}

	/**
	 * Updates contents to be logically consistent following operation e.
	 * Directory changes will be queued; file changes applied immediately.
	 * @param e Not null.
	 */
	protected void process(Entry e) {
		if (getGroup() == null) {
			queue_dirs(e);
		} else if (getGroup().type == Type.MIME) {
			process_file(e);
		} else {
			process_file(e);
			queue_dirs(e);
		}
	}

	/**
	 * Apply all queued directory flags.
	 */
	protected void process_queued() {
		if (!queued.isEmpty())
			process_dirs(queued);
		queued.clear();
	}

	/**
	 * Flag directories that may need to be created/deleted.
	 * @param e Filesystem operation to consider, not null.
	 */
	protected void queue_dirs(Entry e) {
		queued.addAll(e.getGroups());
	}

	/**
	 * Considers creation/deletion of subdirectories as flagged
	 * by queue_dirs to ensure logical consistency of directory contents
	 * with respect to recent filesystem operations.
	 * Will only produce correct results after all file changes
	 * have been applied, since getPoolDirect() is used.
	 */
	protected void process_dirs(Set<Group> e) {
		int current = getPoolDirect().size();
		Set<Group> groups = getGroups();
		for (Group u : e) {
			if (!groups.contains(u)) {
				int next = 0;
				for (Node n : getPoolDirect())
					if (n.getGroups().contains(u))
						next++;
				if (next > 0 && (next < current || getGroups().isEmpty())) {
					if (!mapper.contains(u))
						mapper.map(backend.get(this, u));
				} else {
					if (mapper.contains(u))
						mapper.unmap(u);
				}
			}
		}
		// hide/unhide dirs that do not narrow the query
		Set<Group> others = new HashSet<Group>();
		for (Node node : getPoolDirect())
			others.addAll(node.getGroups());
		others.removeAll(e);
		for (Group g : others) {
			if (mapper.count(g) == current) {
				mapper.unmap(g);
			} else {
				if (!mapper.contains(g))
					mapper.map(backend.get(this, g));
			}
		}
	}

	/**
	 * Updates file in contents to have new name.
	 * @param e Filesystem operation describing change, not null.
	 */
	protected void process_file(Entry e) {
		Node node = e.getNode();
		if (node.getGroups().containsAll(getGroups())) {
			if (mapper.contains(node))
				mapper.unmap(node);
			mapper.map(node);
		} else {
			if (mapper.contains(node))
				mapper.unmap(node);
		}
	}
}
