package groupfs.backend;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.JournalingDirectory;
import groupfs.Link;
import groupfs.QueryGroup;
import groupfs.SubclassingDirectory;

import static groupfs.QueryGroup.Type.*;

import static groupfs.Util.*;

public class DataProvider {
	public final Journal journal = new Journal();
	private final Set<Node> nodes = new HashSet<Node>();
	private final FileSource source;
	private final Set<Node> nodes_ro = Collections.unmodifiableSet(nodes);
	protected Map<Set<QueryGroup>,SubclassingDirectory> cache = new HashMap<Set<QueryGroup>,SubclassingDirectory>();

	public DataProvider(FileSource source) {
		this.source = source;
		for (FileHandler fh : source.getAll())
			nodes.add(makeNode(fh));
	}

	public SubclassingDirectory get(JournalingDirectory parent, QueryGroup group) {
		Set<QueryGroup> key = new HashSet<QueryGroup>(parent.getQueryGroups());
		key.add(group);
		SubclassingDirectory link = cache.get(key);
		if (link == null) {
			link = new SubclassingDirectory(this, parent, group);
			cache.put(key, link);
			return link;
		} else {
			return new Link(this, parent, group, link);
		}
	}

	public void drop(Set<QueryGroup> groups) {
		cache.remove(groups);
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
		journal.log(node, groups);
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
	private DataProvider backend;

	protected JournalingNode(DataProvider backend, FileHandler fh) {
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
		try {
			fh.setName(name);
			this.name = name;
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
		if (!hadMime || !extI.equals(extF)) {
			Set<QueryGroup> add = new HashSet<QueryGroup>();
			Set<QueryGroup> remove = new HashSet<QueryGroup>();
			remove.add(QueryGroup.create(extI, MIME));
			add.add(QueryGroup.create(extF, MIME));
			changeQueryGroups(add, remove, true);
		}
		assert maxOneMimeGroup(groups);
	}

	public int unlink() throws FuseException {
		changeQueryGroups(null, new HashSet<QueryGroup>(groups), true);
		return 0;
	}

	public int deleteFromBackingMedia() {
		backend.unref(this);
		backend.journal.log(this, groups);
		raw_groups.clear();
		fh.delete();
		return 0;
	}

	public void close() throws FuseException {
		try {
			fh.close();
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
	}

	public int read(ByteBuffer buf, long offset) throws FuseException {
		try {
			return fh.read(buf, offset);
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
	}

	public int write(ByteBuffer buf, long offset) throws FuseException {
		try {
			return fh.write(buf, offset);
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
	}

	public int truncate(long size) throws FuseException {
		try {
			return fh.truncate(size);
		} catch (IOException e) {
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
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
		try {
			fh.setTagGroups(groups);
		} catch (IOException e) {
			raw_groups.clear();
			raw_groups.addAll(original);
			throw new FuseException(e.getMessage()).initErrno(FuseException.EIO);
		}
		logDifference(original, groups);
		assert maxOneMimeGroup(groups);
	}

	protected void logDifference(Set<QueryGroup> original, Set<QueryGroup> current) {
		Set<QueryGroup> all = new HashSet<QueryGroup>(original);
		all.addAll(current);
		backend.journal.log(this, all);
	}
}
