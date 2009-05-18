package groupfs.backend;

import java.nio.ByteBuffer;

import java.util.HashSet;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.QueryGroup.Type;

import groupfs.QueryGroup;

import static groupfs.Util.*;

public class FlexibleBackend extends CachingQueryBackend {
	private final FileSource source;

	public FlexibleBackend(FileSource source) {
		this.source = source;
		for (FileHandler fh : source.getAll())
			nodes.add(makeNode(fh));
	}

	private Node makeNode(FileHandler fh) {
		Set<QueryGroup> groups = fh.getAllGroups();
		assert maxOneMimeGroup(groups);
		return new FlexibleNode(this, fh);
	}

	public Node create(Set<QueryGroup> groups, String name) throws FuseException {
		assert maxOneMimeGroup(groups);
		FileHandler fh = source.create(name, groups);
		Node ret = new FlexibleNode(this, fh);
		nodes.add(ret);
		flagged.addAll(groups);
		flush();
		checkRootAdd(groups);
		return ret;
	}

	public long getFreeSpace() {
		return source.getFreeSpace();
	}

	public long getUsableSpace() {
		return source.getUsableSpace();
	}

	public long getTotalSpace() {
		return source.getTotalSpace();
	}
}

class FlexibleNode extends Node {
	private FileHandler fh;
	private CachingQueryBackend backend;

	protected FlexibleNode(CachingQueryBackend backend, FileHandler fh) {
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

	public void setName(String name) throws FuseException {
		String extI = extensionOf(this.name);
		String extF = extensionOf(name);
		assert maxOneMimeGroup(groups);
		if (!extI.equals(extF)) {
			Set<QueryGroup> add = new HashSet<QueryGroup>();
			Set<QueryGroup> remove = new HashSet<QueryGroup>();
			remove.add(QueryGroup.create(extI, Type.MIME));
			add.add(QueryGroup.create(extF, Type.MIME));
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
		for (QueryGroup q : groups)
			backend.flag(q);
		Set<QueryGroup> removed = new HashSet<QueryGroup>(groups);
		raw_groups.clear();
		backend.flush();
		backend.checkRootRm(removed);
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

	protected void update(Set<QueryGroup> all, Set<QueryGroup> add, Set<QueryGroup> remove) {
		for (QueryGroup g : groups)
			backend.flag(g);
		backend.flush();
		if (remove != null)
			backend.checkRootRm(remove);
		if (add != null)
			backend.checkRootAdd(add);
	}

	protected void changeQueryGroups(Set<QueryGroup> add, Set<QueryGroup> remove, boolean allowMimetypeChange) throws FuseException {
		if (remove != null)
			for (QueryGroup r : remove) {
				if (allowMimetypeChange || r.getType() != Type.MIME) {
					raw_groups.remove(r);
					backend.flag(r);
				}
			}
		if (add != null)
			for (QueryGroup a : add) {
				if (allowMimetypeChange || a.getType() != Type.MIME)
					raw_groups.add(a);
			}
		if (hasCategory(groups)) {
			assert !groups.contains(QueryGroup.GROUP_NO_GROUP);
		} else {
			for (QueryGroup group : groups)
				backend.flag(group);
			raw_groups.clear();
			if (!groups.contains(QueryGroup.GROUP_NO_GROUP)) {
				raw_groups.add(QueryGroup.GROUP_NO_GROUP);
				backend.checkRootAdd(QueryGroup.SET_NO_GROUP);
			}
		}
		fh.setTagGroups(groups);
		update(groups, add, remove);
		assert maxOneMimeGroup(groups);
	}
}
