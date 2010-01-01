package groupfs;

import fuse.FuseException;
import fuse.FuseGetattrSetter;

/**
 * All files and directories must implement this.
 */
public interface View {
	/**
	 * @return	Type of the view - file or directory.
	 */
	public int getFType();

	/**
	 * Logically implements rename operation requested by user.
	 * @param	from	The old path of the view.
	 * @param	to		The path the view should end up with.
	 * @param	target	The view that would be overwritten, or null.
	 * @param	orig	The directory the view is currently in.
	 * @param	dest	The directory the view should end up in.
	 */
	public int rename(Path from, Path to, View target, Directory orig, Directory dest) throws FuseException;

	public int stat(FuseGetattrSetter setter);
	public int setModified(long time) throws FuseException;
}
