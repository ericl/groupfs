package groupfs.state;

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

import groupfs.Group;
import groupfs.storage.FileSource;
import groupfs.storage.FileHandler;
import groupfs.storage.StorageInfo;

import static groupfs.Group.Type.*;

import static groupfs.Util.*;

/**
 * Go-between filesystem and data storage.
 * Abstracts away directory and data allocation, while
 * tracking filesystem changes and ensuring the
 * logical consistency of the filesystem.
 */
public class Manager {
	public final Journal journal = new Journal();
	private final Set<Node> nodes = new HashSet<Node>();
	private final FileSource source;
	private final Set<Node> nodes_ro = Collections.unmodifiableSet(nodes);
	protected Map<Set<Group>,SubDirectory> cache = new HashMap<Set<Group>,SubDirectory>();

	public Manager(FileSource source) {
		this.source = source;
		for (FileHandler fh : source.getAll())
			nodes.add(makeNode(fh));
	}

	public SubDirectory directoryInstance(BaseDirectory parent, Group group) {
		Set<Group> key = new HashSet<Group>(parent.getGroups());
		key.add(group);
		SubDirectory link = cache.get(key);
		if (link == null) {
			link = new SubDirectory(this, parent, group);
			cache.put(key, link);
			return link;
		} else {
			return new Link(this, parent, group, link);
		}
	}

	public void drop(Set<Group> groups) {
		cache.remove(groups);
	}

	private JournalingNode makeNode(FileHandler fh) {
		Set<Group> groups = fh.getAllGroups();
		assert maxOneMimeGroup(groups);
		return new JournalingNode(this, fh);
	}

	public Set<Node> getAll() {
		return nodes_ro;
	}

	public Node create(Set<Group> groups, String name) throws FuseException {
		assert maxOneMimeGroup(groups);
		FileHandler fh = source.create(name, groups);
		Node node = new JournalingNode(this, fh);
		nodes.add(node);
		journal.log(node, Group.SET_EMPTY_SET, groups);
		return node;
	}

	public void unref(Node n) {
		nodes.remove(n);
	}

	public StorageInfo getInfo() {
		return source.getInfo();
	}
}

class JournalingNode extends Node {
	private FileHandler fh;
	private Manager backend;

	protected JournalingNode(Manager backend, FileHandler fh) {
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

	public void changeGroups(Set<Group> add, Set<Group> remove) throws FuseException {
		changeGroups(add, remove, false);
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
			Set<Group> add = new HashSet<Group>();
			Set<Group> remove = new HashSet<Group>();
			remove.add(Group.create(extI, MIME));
			add.add(Group.create(extF, MIME));
			changeGroups(add, remove, true);
		}
		assert maxOneMimeGroup(groups);
	}

	public int unlink() throws FuseException {
		changeGroups(null, new HashSet<Group>(groups), true);
		return 0;
	}

	public int deleteFromBackingMedia() {
		backend.unref(this);
		backend.journal.log(this, groups, Group.SET_EMPTY_SET);
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

	protected void changeGroups(Set<Group> add, Set<Group> remove, boolean allowMimetypeChange) throws FuseException {
		Set<Group> original = new HashSet<Group>(groups);
		if (remove != null)
			for (Group r : remove) {
				if (allowMimetypeChange || r.type != MIME)
					raw_groups.remove(r);
			}
		if (add != null)
			for (Group a : add) {
				if (allowMimetypeChange || a.type != MIME)
					raw_groups.add(a);
			}
		if (hasCategory(groups)) {
			raw_groups.remove(Group.GROUP_NO_GROUP);
		} else {
			raw_groups.clear();
			if (!groups.contains(Group.GROUP_NO_GROUP))
				raw_groups.add(Group.GROUP_NO_GROUP);
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

	protected void logDifference(Set<Group> original, Set<Group> next) {
		backend.journal.log(this, original, next);
	}
}
