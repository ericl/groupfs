package groupfs;

import java.util.Map;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseGetattrSetter;

import groupfs.backend.DataProvider;
import groupfs.backend.Node;

public class Link extends SubclassingDirectory {
	private final SubclassingDirectory link;

	public Link(DataProvider backend, JournalingDirectory parent, QueryGroup group, SubclassingDirectory link) {
		super(backend, parent, group);
		this.link = link;
		// guard against mistaken access
		groups = null;
		raw_groups = null;
		mapper = null;
	}

	public Permissions getPerms() {
		return link.getPerms();
	}

	public int getFType() {
		return FuseFtype.TYPE_DIR;
	}

	protected NameMapper getMapper() {
		return link.getMapper();
	}

	public int setModified(long mtime) throws FuseException {
		return link.setModified(mtime);
	}

	public int rename(Path from, Path to, View target, Directory orig, Directory dest) throws FuseException {
		return link.rename(from, to, target, orig, dest);
	}

	public int delete() throws FuseException {
		return fuse.Errno.EPERM;
	}

	protected Set<Node> getPool() {
		return link.getPool();
	}

	protected Set<Node> getPoolDirect() {
		return link.getPoolDirect();
	}

	public View get(String name) {
		assert groups == null && raw_groups == null && mapper == null;
		return link.get(name);
	}

	public Directory getDir(String name) {
		assert groups == null && raw_groups == null && mapper == null;
		return link.getDir(name);
	}

	public int stat(FuseGetattrSetter setter) {
		return link.stat(setter);
	}

	public QueryGroup getGroup() {
		return group;
	}

	public Set<QueryGroup> getQueryGroups() {
		assert groups == null && raw_groups == null && mapper == null;
		return link.getQueryGroups();
	}

	public Directory getParent() {
		return parent;
	}

	public Map<String,View> list() {
		assert groups == null && raw_groups == null && mapper == null;
		return link.list();
	}

	public void mkdir(QueryGroup g) {
		link.mkdir(g);
	}
}
