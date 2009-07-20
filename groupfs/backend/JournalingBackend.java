package groupfs.backend;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.QueryGroup;

import static groupfs.QueryGroup.Type.*;

import static groupfs.Util.*;

public class JournalingBackend {
	public final Journal journal = new Journal();
	private final Set<Node> nodes = new HashSet<Node>();
	private final FileSource source;
	private final Set<Node> nodes_ro = Collections.unmodifiableSet(nodes);

	public JournalingBackend(FileSource source) {
		this.source = source;
		for (FileHandler fh : source.getAll())
			nodes.add(makeNode(fh));
	}

	private JournalingNode makeNode(FileHandler fh) {
		Set<QueryGroup> groups = fh.getAllGroups();
		assert maxOneMimeGroup(groups);
		return new JournalingNode(this, fh);
	}

	public Set<Node> getAll() {
		return nodes_ro;
	}

	public Set<QueryGroup> findAllGroups() {
		Set<QueryGroup> output = new HashSet<QueryGroup>();
		for (Node node : getAll())
			output.addAll(node.getQueryGroups());
		return output;
	}

	public Node create(Set<QueryGroup> groups, String name) throws FuseException {
		assert maxOneMimeGroup(groups);
		FileHandler fh = source.create(name, groups);
		Node node = new JournalingNode(this, fh);
		nodes.add(node);
		List<Update> updates = new ArrayList<Update>();
		for (QueryGroup group : groups)
			updates.add(new Update(group, true));
		journal.log(node, updates);
		return node;
	}

	public void unref(Node n) {
		nodes.remove(n);
	}

	public long getFreeSpace() {
		return source.getFreeSpace();
	}

	public long getTotalSpace() {
		return source.getTotalSpace();
	}

	public long getUsableSpace() {
		return source.getUsableSpace();
	}
}

class JournalingNode extends Node {
	private FileHandler fh;
	private JournalingBackend backend;

	protected JournalingNode(JournalingBackend backend, FileHandler fh) {
		super(fh.getAllGroups());
		this.backend = backend;
		this.fh = fh;
		this.name = unNumbered(fh.getName());
	}

	public String toString() {
		return name;
	}

	public int stat(FuseGetattrSetter setter) {
		int time = (int)(fh.lastModified() / 1000L);
		long size = fh.length();
		setter.set(
			0, // inode
			FuseFtype.TYPE_FILE | 0644,
			1, // nlink
			UID,
			GID,
			0, // rdev
			size,
			(int)((size + 511L) / 512L),
			time, time, time // atime, mtime, ctime
		);
		return 0;
	}

	public int setModified(long mtime) throws FuseException {
		fh.setLastModified(mtime);
		return 0;
	}

	public void changeQueryGroups(Set<QueryGroup> add, Set<QueryGroup> remove) throws FuseException {
		changeQueryGroups(add, remove, false);
	}

	protected void setName(String name, boolean hadMime) throws FuseException {
		String extI = extensionOf(this.name);
		String extF = extensionOf(name);
		assert maxOneMimeGroup(groups);
		if (!hadMime || !extI.equals(extF)) {
			Set<QueryGroup> add = new HashSet<QueryGroup>();
			Set<QueryGroup> remove = new HashSet<QueryGroup>();
			remove.add(QueryGroup.create(extI, MIME));
			add.add(QueryGroup.create(extF, MIME));
			changeQueryGroups(add, remove, true);
		}
		assert maxOneMimeGroup(groups);
		this.name = name;
		fh.setName(name);
	}

	public int unlink() throws FuseException {
		changeQueryGroups(null, new HashSet<QueryGroup>(groups), true);
		return 0;
	}

	public int deleteFromBackingMedia() {
		backend.unref(this);
		List<Update> updates = new ArrayList<Update>();
		for (QueryGroup g : groups)
			updates.add(new Update(g, true));
		backend.journal.log(this, updates);
		raw_groups.clear();
		fh.delete();
		return 0;
	}

	public void close() throws FuseException {
		fh.close();
	}

	public int read(ByteBuffer buf, long offset) throws FuseException {
		return fh.read(buf, offset);
	}

	public int write(ByteBuffer buf, long offset) throws FuseException {
		return fh.write(buf, offset);
	}

	public int truncate(long size) throws FuseException {
		return fh.truncate(size);
	}

	protected void changeQueryGroups(Set<QueryGroup> add, Set<QueryGroup> remove, boolean allowMimetypeChange) throws FuseException {
		Set<QueryGroup> original = new HashSet<QueryGroup>(groups);
		if (remove != null)
			for (QueryGroup r : remove) {
				if (allowMimetypeChange || r.getType() != MIME)
					raw_groups.remove(r);
			}
		if (add != null)
			for (QueryGroup a : add) {
				if (allowMimetypeChange || a.getType() != MIME)
					raw_groups.add(a);
			}
		if (hasCategory(groups)) {
			raw_groups.remove(QueryGroup.GROUP_NO_GROUP);
		} else {
			raw_groups.clear();
			if (!groups.contains(QueryGroup.GROUP_NO_GROUP))
				raw_groups.add(QueryGroup.GROUP_NO_GROUP);
		}
		fh.setTagGroups(groups);
		logDifference(original, groups);
		assert maxOneMimeGroup(groups);
	}

	protected void logDifference(Set<QueryGroup> original, Set<QueryGroup> current) {
		List<Update> updates = new ArrayList<Update>();

		Set<QueryGroup> neutral = new HashSet<QueryGroup>(original);
		neutral.retainAll(current);
		for (QueryGroup g : neutral)
			updates.add(new Update(g, false));

		Set<QueryGroup> changed = new HashSet<QueryGroup>(original);
		changed.addAll(current);
		changed.removeAll(neutral);
		for (QueryGroup g : changed)
			updates.add(new Update(g, true));

		backend.journal.log(this, updates);
	}
}
