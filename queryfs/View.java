package queryfs;

import fuse.FuseException;
import fuse.FuseGetattrSetter;

public interface View {
	public void stat(FuseGetattrSetter setter);
	public int getFType();
	public void setModified(long time) throws FuseException;
}
