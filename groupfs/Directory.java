package groupfs;

import java.util.Map;
import java.util.Set;

import fuse.FuseException;

/**
 * All directories must implement this.
 */
public interface Directory extends Inode {
	public int delete() throws FuseException;
	public Inode get(String name);
	public Directory getDir(String name);
	public Group getGroup();
	public Set<Group> getGroups();
	public Directory getParent();
	public Map<String,Inode> list();
	public Permissions getPerms();
	public void mkdir(Group group);
}
