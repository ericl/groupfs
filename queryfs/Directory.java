package queryfs;

import java.util.Map;
import java.util.Set;

import com.sun.security.auth.module.UnixSystem;

import fuse.FuseException;

public interface Directory extends View {
	public final static int UID = (int)new UnixSystem().getUid();
	public final static int GID = (int)new UnixSystem().getGid();

	public Set<Node> getNodes();

	public void delete() throws FuseException;

	public View get(String name);

	public Directory getDir(String name);

	public QueryGroup getGroup();

	public Set<QueryGroup> getQueryGroups();

	public Directory getParent();

	public Map<String,View> list();
}
