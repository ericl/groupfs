package groupfs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.QueryGroup.Type;

import groupfs.backend.Entry;
import groupfs.backend.JournalingBackend;
import groupfs.backend.Node;
import groupfs.backend.Update;

import static groupfs.Util.*;

public class JournalingDirectory implements Directory {
	protected Entry head;
	protected JournalingBackend backend;
	protected NameMapper mapper = new NameMapper();
	protected long time = System.currentTimeMillis();
	protected boolean populated;
	protected final Set<QueryGroup> groups, raw_groups;
	protected static Permissions root_perms = new Permissions(
		false, false, false, true, true, true, false
	);

	public JournalingDirectory(JournalingBackend backend) {
		this.backend = backend;
		head = backend.journal.head();
		groups = Collections.unmodifiableSet(raw_groups = new HashSet<QueryGroup>());
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

	public int rename(String from, String to, View v, Set<QueryGroup> hintRemove, Set<QueryGroup> hintAdd) throws FuseException {
		return fuse.Errno.EPERM;
	}

	public int delete() throws FuseException {
		return fuse.Errno.EPERM;
	}

	protected Set<Node> getPool() {
		return getPoolDirect();
	}

	// same as getpool but without pool updated first
	protected Set<Node> getPoolDirect() {
		return backend.getAll();
	}

	protected void populateSelf() {
		for (QueryGroup group : backend.findAllGroups())
			mapper.map(new SubclassingDirectory(backend, this, group));
	}

	public View get(String name) {
		update();
		return mapper.viewMap().get(name);
	}

	public Directory getDir(String name) {
		View v = get(name);
		if (v != null && v instanceof Directory)
			return (Directory)v;
		return null;
	}

	public int stat(FuseGetattrSetter setter) {
		update();
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

	public QueryGroup getGroup() {
		return null;
	}

	public Set<QueryGroup> getQueryGroups() {
		return groups;
	}

	public Directory getParent() {
		return null;
	}

	public Map<String,View> list() {
		update();
		return mapper.viewMap();
	}

	protected void update() {
		if (!populated) {
			populateSelf();
			populated = true;
			time = System.currentTimeMillis();
		}
		if (head != backend.journal.head()) {
			replayJournal();
			time = System.currentTimeMillis();
		}
	}

	protected void replayJournal() {
		while (head != backend.journal.head()) {
			Entry next = head.getNext();
			if (next != null) {
				head = next;
				if (isPertinent(head)) {
//					System.out.println("process by " + getQueryGroups() + " " + head);
					process(head);
				}
			}
		}
	}

	protected boolean isPertinent(Entry e) {
		return e != null && e.getNode() != null && e.getGroups().containsAll(getQueryGroups());
	}

	protected void process(Entry e) {
		if (getGroup() == null) {
			process_dirs(e);
		} else if (getGroup().getType() == Type.MIME) {
			process_file(e);
		} else {
			process_file(e);
			process_dirs(e);
		}
	}

	// always call after process_file since getPoolDirect() is called
	// getPoolDirect() will return correct value then
	protected void process_dirs(Entry e) {
		int current = getPoolDirect().size();
		for (Update u : e.getUpdates()) {
			if (!getQueryGroups().contains(u.group)) {
				int next = 0;
				for (Node n : getPoolDirect())
					if (n.getQueryGroups().contains(u.group))
						next++;
				if (next > 0 && (next < current || getQueryGroups().isEmpty())) {
//					System.out.println("create " + u.group);
					if (!mapper.contains(u.group))
						mapper.map(new SubclassingDirectory(backend, this, u.group));
				} else {
//					System.out.println("delete " + u.group);
					if (mapper.contains(u.group))
						mapper.unmap(u.group);
				}
			}
		}
		Set<QueryGroup> others = new HashSet<QueryGroup>();
		for (Node node : getPoolDirect())
			others.addAll(node.getQueryGroups());
		others.removeAll(e.getGroups());
//		System.out.println("OTHERS: " + others);
		for (QueryGroup g : others) {
			if (mapper.count(g) == current) {
				if (mapper.contains(g))
					mapper.unmap(g);
			} else {
				if (!mapper.contains(g))
					mapper.map(new SubclassingDirectory(backend, this, g));
			}
		}
	}

	protected void process_file(Entry e) {
		Node node = e.getNode();
		if (node.getQueryGroups().containsAll(groups)) {
			if (mapper.contains(node))
				mapper.unmap(node);
			mapper.map(node);
		} else {
			if (mapper.contains(node))
				mapper.unmap(node);
		}
	}
}
