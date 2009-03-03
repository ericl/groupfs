package queryfs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.security.auth.module.UnixSystem;

import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import queryfs.QueryGroup.Type;

public class Directory implements View {
	private static final Log log =
		LogFactory.getLog(Directory.class);

	private final static int UID = (int)new UnixSystem().getUid();
	private final static int GID = (int)new UnixSystem().getGid();
	private final Directory parent;
	private final QueryGroup group;
	private final QueryBackend backend;
	private final Set<QueryGroup> groups;
	private final Map<String,View> views = new HashMap<String,View>();
	private long time = System.currentTimeMillis();
	private boolean populated;

	public Directory(QueryBackend backend) {
		this.backend = backend;
		parent = null;
		group = null;
		groups = new HashSet<QueryGroup>();
		if (parent != null)
			groups.addAll(parent.getQueryGroups());
	}

	public Set<Node> getNodes() {
		if (group == null)
			return null;
		else
			return backend.query(groups);
	}

	public Directory(QueryBackend backend, Directory parent, QueryGroup group) {
		this.backend = backend;
		this.parent = parent;
		this.group = group;
		groups = new HashSet<QueryGroup>();
		groups.addAll(parent.getQueryGroups());
		groups.add(group);
	}

	public int getFType() {
		return FuseFtype.TYPE_DIR;
	}

	public void setModified(long mtime) throws FuseException {
		time = mtime;
	}

	public void delete() throws FuseException {
		Set<QueryGroup> del = new HashSet<QueryGroup>();
		del.add(group);
		for (String ref : views.keySet()) {
			View v = views.get(ref);
			if (v instanceof Node) {
				Node n = (Node)v;
				n.changeQueryGroups(null, del);
			}
		}
		return;
	}

	private void resolveView(String key, View value) {
		if (views.containsKey(key)) {
			int num = 0;
			while (views.containsKey(key + "." + num))
				num++;
			key += "." + num;
		}
		views.put(key, value);
	}

	private void populateSelf() {
		for (QueryGroup group : backend.subclass(groups)) {
			if (group.getType() == Type.MIME)
				resolveView("." + group.getValue(), new Directory(backend, this, group));
			else
				resolveView(group.getValue(), new Directory(backend, this, group));
		}
		if (group != null)
			for (Node node : backend.query(groups))
				resolveView(node.getName(), node);
	}

	public View get(String name) {
		return views.get(name);
	}

	public Directory getDir(String name) {
		View v = get(name);
		if (v != null && v instanceof Directory)
			return (Directory)v;
		return null;
	}

	public void stat(FuseGetattrSetter setter) {
		log.debug("test");
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

	public QueryGroup getGroup() {
		return group;
	}

	public Set<QueryGroup> getQueryGroups() {
		return groups;
	}

	public Directory getParent() {
		return parent;
	}

	public synchronized void fill(FuseDirFiller filler) {
		if (!populated)
			populateSelf();
		populated = true;
		for (String ref : views.keySet())
			filler.add(ref, 0, views.get(ref).getFType());
	}
}
