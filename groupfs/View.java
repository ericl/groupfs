package groupfs;

import fuse.FuseException;
import fuse.FuseGetattrSetter;

public interface View {
	public int stat(FuseGetattrSetter setter);
	public int getFType();
	public int rename(Path from, Path to, View target, Directory orig, Directory dest) throws FuseException;
	public int setModified(long time) throws FuseException;
}
