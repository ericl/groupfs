package groupfs.backend;

import groupfs.*;

public class Update {
	public final boolean altered;
	public final QueryGroup group;

	public Update(QueryGroup group, boolean altered) {
		this.group = group;
		this.altered = altered;
	}

	public String toString() {
		return group + "-" + altered;
	}
}
