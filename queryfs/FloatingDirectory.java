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
	protected static Permissions floating_perms = new Permissions(
		true, true, true, true, true, true, true
	);

	public FloatingDirectory(ViewMapper mapper, String parent, String id, QueryGroup group) {
		this.mapper = mapper;
		this.group = group;
		this.parent = parent;
		this.id = id;
		groups = new HashSet<QueryGroup>();
		groups.addAll(getParent().getQueryGroups());
		groups.add(group);
	}

	public Permissions getPerms() {
		return floating_perms;
	}

	public String getHost() {
		return parent;
	}

	public int getFType() {
		return FuseFtype.TYPE_DIR;
	}

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

	public int setModified(long mtime) throws FuseException {
		time = mtime;
		return 0;
	}

	public int rename(String from, String to, View v, Set<QueryGroup> hintRemove, Set<QueryGroup> hintAdd) {
		if (v != null)
			return fuse.Errno.EPERM;
		mapper.remap(from, to);
		return 0;
	}


	public int delete() throws FuseException {
		mapper.delete(id, true);
		return 0;
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
