package groupfs;

/**
 * What filesystem operations are permissible for a directory.
 */
public class Permissions {
	private boolean cd, cmn, cr, cmi, cmo, cmd, cdn;

	/**
	 * @param cd Can it be deleted?
	 * @param cmn Can new files be created in it?
	 * @param cr Can it be renamed?
	 * @param cmi Can files be moved into it?
	 * @param cmo Can files be moved out of it?
	 * @param cmd Can subdirectores be created in it?
	 * @param cdn Can files be deleted in it?
	 */
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
