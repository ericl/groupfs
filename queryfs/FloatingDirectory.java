package queryfs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import queryfs.Filesystem.ViewMapper;

public class FloatingDirectory implements Directory {
	private final ViewMapper mapper;
	private final QueryGroup group;
	private final String id;
	private final String parent;
	private long time = System.currentTimeMillis();
	private Set<QueryGroup> groups;

	public FloatingDirectory(ViewMapper mapper, String parent, String id, QueryGroup group) {
		this.mapper = mapper;
		this.group = group;
		this.parent = parent;
		this.id = id;
		groups = new HashSet<QueryGroup>();
		groups.addAll(getParent().getQueryGroups());
		groups.add(group);
	}

	public String getPath() {
		return id;
	}

	public String getHost() {
		return parent;
	}

	public int getFType() {
		return FuseFtype.TYPE_DIR;
	}

	public void stat(FuseGetattrSetter setter) {
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
	}

	public void setModified(long mtime) throws FuseException {
		time = mtime;
	}

	public Set<Node> getNodes() {
		return new HashSet<Node>();
	}

	public int rename(String from, String to, View v) {
		if (v != null)
			return fuse.Errno.EPERM;
		// TODO mapper.remap() to handle those subdirs that otherwise disappear
		mapper.delete(from, true);
		mapper.createFloat(to);
		return 0;
	}


	public void delete() throws FuseException {
		mapper.delete(id, true);
	}
	
	public View get(String name) {
		return null;
	}

	public Directory getDir(String name) {
		return null;
	}

	public QueryGroup getGroup() {
		return group;
	}

	public Set<QueryGroup> getQueryGroups() {
		return groups;
	}

	public Directory getParent() {
		return mapper.getDir(parent);
	}

	public Map<String,View> list() {
		return new HashMap<String,View>();
	}
}
