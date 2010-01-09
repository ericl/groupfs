package groupfs;

import fuse.FuseException;
import fuse.FuseGetattrSetter;

/**
 * A filesystem object.
 * All files and directories must implement this.
 */
public interface Inode {
	/**
	 * @return	Type of the inode - file or directory.
	 */
	public int getFType();

	/**
	 * Logically implements rename operation requested by user.
	 * @param	from	The old path of the inode.
	 * @param	to		The path the inode should end up with.
	 * @param	target	The inode that would be overwritten, or null.
	 * @param	orig	The directory the inode is currently in.
	 * @param	dest	The directory the inode should end up in.
	 */
	public int rename(Path from, Path to, Inode target, Directory orig, Directory dest) throws FuseException;

	public int stat(FuseGetattrSetter setter);
	public int setModified(long time) throws FuseException;
}
