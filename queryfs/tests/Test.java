package queryfs.tests;

import java.util.HashSet;
import java.util.Set;

import fuse.FuseException;
import fuse.FuseGetattr;
import fuse.FuseGetattrSetter;

import queryfs.*;

import queryfs.QueryGroup.Type;

import queryfs.backend.*;

import static queryfs.Util.*;

public abstract class Test {
	protected boolean error = false;
	protected String log = "";

	public abstract void run();

	public String toString() {
		String cl = this.getClass().getName();
		return cl.substring(cl.lastIndexOf(".") + 1);
	}

	protected void syn(QueryBackend backend, String name, String ... tags) {
		Set<QueryGroup> groups = new HashSet<QueryGroup>();
		for (String tag : tags)
			groups.add(QueryGroup.create(tag, Type.TAG));
		groups.add(QueryGroup.create(extensionOf(name), Type.MIME));
		if (groups.size() == 1)
			groups.add(QueryGroup.GROUP_NO_GROUP);
		try {
			backend.create(groups, name);
		} catch (FuseException e) {
			throw new AssertionError(e);
		}
	}

	protected void error(String msg) {
		appendTrace(msg);
		error = true;
	}

	protected void appendTrace(String entry) {
		log += entry + "\n";
	}

	protected void assertExists(Filesystem fs, String ... paths) {
		for (String path : paths) {
			appendTrace("expect " + path);
			FuseGetattrSetter setter = new FuseGetattr();
			try {
				if (fs.getattr(path, setter) != 0)
					error("E: " + path + " does not exist");
			} catch (FuseException e) {
				error(e.toString());
			}
		}
	}

	protected void assertMissing(Filesystem fs, String ... paths) {
		for (String path : paths) {
			appendTrace("noexpect " + path);
			FuseGetattrSetter setter = new FuseGetattr();
			try {
				if (fs.getattr(path, setter) != fuse.Errno.ENOENT)
					error(path + " exists");
			} catch (FuseException e) {
				if (e.getErrno() != fuse.Errno.ENOENT)
					error(e.toString());
			}
		}
	}
}
