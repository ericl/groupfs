package queryfs;

import fuse.FuseException;
import fuse.FuseGetattrSetter;

public interface View {
	public int stat(FuseGetattrSetter setter);
	public int getFType();
	public int rename(String from, String to, View target) throws FuseException;
	public int setModified(long time) throws FuseException;
}
