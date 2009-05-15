package intfs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import intfs.QueryGroup.Type;

import intfs.backend.QueryBackend;

import static intfs.Util.*;

public class RootDirectory implements Directory {
	protected QueryBackend backend;
	protected long time = System.currentTimeMillis();
	protected long stamp = System.nanoTime();
	protected boolean populated;
	protected final Set<QueryGroup> groups, raw_groups;
	protected Map<String,View> views = new HashMap<String,View>();
	protected static Permissions root_perms = new Permissions(
		false, false, false, true, true, true, false
	);

	public RootDirectory(QueryBackend backend) {
		this.backend = backend;
		groups = Collections.unmodifiableSet(raw_groups = new HashSet<QueryGroup>());
	}

	public Permissions getPerms() {
		return root_perms;
	}

	protected void register(String key, View value) {
		if (views.containsKey(key)) {
			int num = 0;
			while (views.containsKey(key + "." + num))
				num++;
			key += "." + num;
		}
		views.put(key, value);
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

	protected void populateSelf() {
		views.clear();
		for (QueryGroup group : backend.subclass(groups)) {
			if (group.getType() == Type.MIME)
				register("." + group.getValue(), new SubclassingDirectory(backend, this, group));
			else {
				String value = group.getValue();
				register(value, new SubclassingDirectory(backend, this, group));
			}
		}
	}

	public View get(String name) {
		update();
		return views.get(name);
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

	protected synchronized void update() {
		if (!QueryGroup.allValid(stamp))
			populated = false;
		if (!populated) {
			populateSelf();
			populated = true;
			time = System.currentTimeMillis();
			stamp = System.nanoTime();
		}
	}

	public Map<String,View> list() {
		update();
		return views;
	}
}
