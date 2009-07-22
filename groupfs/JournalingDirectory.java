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

import static groupfs.Util.*;

public class JournalingDirectory implements Directory {
	protected Entry head;
	protected JournalingBackend backend;
	protected Set<QueryGroup> queued = new HashSet<QueryGroup>();
	protected NameMapper mapper;
	protected long time = System.currentTimeMillis();
	protected boolean populated;
	protected Set<QueryGroup> groups, raw_groups;
	protected static Permissions root_perms = new Permissions(
		false, false, false, true, true, true, false
	);

	public JournalingDirectory(JournalingBackend backend) {
		this.backend = backend;
		mapper = new NameMapper(backend);
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

	public int rename(Path from, Path to, View target, Directory orig, Directory dest) throws FuseException {
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
			mapper.map(backend.get(this, group));
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

	public void mkdir(QueryGroup g) {
		mapper.map(new SubclassingDirectory(backend, this, g));
	}

	protected NameMapper getMapper() {
		return mapper;
	}

	protected void update() {
		if (!populated) {
			populateSelf();
			populated = true;
			time = System.currentTimeMillis();
			head = backend.journal.head(); // we're up to date
		} else if (head != backend.journal.head()) {
			replayJournal();
			time = System.currentTimeMillis();
		}
	}

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

	protected boolean isPertinent(Entry e) {
		return e != null && e.getNode() != null && e.getGroups().containsAll(getQueryGroups());
	}

	protected void process(Entry e) {
		if (getGroup() == null) {
			queue_dirs(e);
		} else if (getGroup().getType() == Type.MIME) {
			process_file(e);
		} else {
			process_file(e);
			queue_dirs(e);
		}
	}

	protected void process_queued() {
		if (!queued.isEmpty())
			process_dirs(queued);
		queued.clear();
	}

	protected void queue_dirs(Entry e) {
		queued.addAll(e.getGroups());
	}

	// always call after process_file since getPoolDirect() is called
	// getPoolDirect() will return correct value then
	protected void process_dirs(Set<QueryGroup> e) {
		int current = getPoolDirect().size();
		for (QueryGroup u : e) {
			if (!getQueryGroups().contains(u)) {
				int next = 0;
				for (Node n : getPoolDirect())
					if (n.getQueryGroups().contains(u))
						next++;
				if (next > 0 && (next < current || getQueryGroups().isEmpty())) {
					if (!mapper.contains(u))
						mapper.map(backend.get(this, u));
				} else {
					if (mapper.contains(u))
						mapper.unmap(u);
				}
			}
		}
		Set<QueryGroup> others = new HashSet<QueryGroup>();
		for (Node node : getPoolDirect())
			others.addAll(node.getQueryGroups());
		others.removeAll(e);
		for (QueryGroup g : others) {
			if (mapper.count(g) == current) {
				if (mapper.contains(g))
					mapper.unmap(g);
			} else {
				if (!mapper.contains(g))
					mapper.map(backend.get(this, g));
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
