package groupfs;

import java.util.Collections;
import java.util.HashMap;
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
	protected long time = System.currentTimeMillis();
	protected boolean populated;
	protected final Set<QueryGroup> groups, raw_groups;
	protected Map<String,View> views = new HashMap<String,View>();
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

	protected Set<Node> getPool() {
		return backend.getAll();
	}

	protected void populateSelf() {
		for (QueryGroup group : backend.findAllGroups()) {
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

	public Map<String,View> list() {
		update();
		return views;
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
		// for now just destroy everything and restart
		views.clear();
		populateSelf();
		// TODO fine grained journal replay
		head = backend.journal.head();
	}
}
