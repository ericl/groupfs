package groupfs;

import java.util.Map;
import java.util.Set;

import fuse.FuseException;

/**
 * All directories must implement this.
 */
public interface Directory extends View {
	public int delete() throws FuseException;
	public View get(String name);
	public Directory getDir(String name);
	public QueryGroup getGroup();
	public Set<QueryGroup> getQueryGroups();
	public Directory getParent();
	public Map<String,View> list();
	public Permissions getPerms();
	public void mkdir(QueryGroup group);
}

class Permissions {
	private boolean cd, cmn, cr, cmi, cmo, cmd, cdn;

	public Permissions(boolean cd, boolean cmn, boolean cr, boolean cmi, boolean cmo, boolean cmd, boolean cdn) {
		this.cd = cd;
		this.cmn = cmn;
		this.cr = cr;
		this.cmi = cmi;
		this.cmo = cmo;
		this.cmd = cmd;
		this.cdn = cdn;
	}

	public boolean canDelete() {
		return cd;
	}

	public boolean canMknod() {
		return cmn;
	}

	public boolean canRename() {
		return cr;
	}

	public boolean canMoveIn() {
		return cmi;
	}

	public boolean canMoveOut() {
		return cmo;
	}

	public boolean canMkdir() {
		return cmd;
	}

	public boolean canDeleteNode() {
		return cdn;
	}
}
