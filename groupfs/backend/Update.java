package groupfs.backend;

import groupfs.*;

public class Update {
	public enum Disposition {POSITIVE, NEGATIVE, NEUTRAL};
	public final Disposition disp;
	public final QueryGroup group;

	public Update(QueryGroup group, Disposition disp) {
		this.group = group;
		this.disp = disp;
	}
}
