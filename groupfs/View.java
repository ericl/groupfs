package groupfs;

import java.util.Set;

import fuse.FuseException;
import fuse.FuseGetattrSetter;

public interface View {
	public int stat(FuseGetattrSetter setter);
	public int getFType();
	public int rename(String from, String to, View target, Set<QueryGroup> hintRemove, Set<QueryGroup> hintAdd) throws FuseException;
	public int setModified(long time) throws FuseException;
}
