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

	protected Set<Node> getPool() {
		return getPoolDirect();
	}

	// same as getpool but without pool updated first
	protected Set<Node> getPoolDirect() {
		return backend.getAll();
	}

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

	public Inode get(String name) {
		update();
		return mapper.inodeMap().get(name);
	}

	public Directory getDir(String name) {
		Inode v = get(name);
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

	public Group getGroup() {
		return null;
	}

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

	public Directory getParent() {
		return null;
	}

	public Map<String,Inode> list() {
		update();
		return mapper.inodeMap();
	}

	public void mkdir(Group g) {
		mapper.map(new SubDirectory(backend, this, g));
	}

	protected NameMapper getMapper() {
		return mapper;
	}

	protected void update() {
		if (!populateSelf() && head != backend.journal.head()) {
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
		return e != null && e.getNode() != null && e.getGroups().containsAll(getGroups());
	}

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
