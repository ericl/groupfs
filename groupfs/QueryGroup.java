package groupfs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class QueryGroup {
	public enum Type { TAG, MIME }
	private final String value;
	private long time = System.nanoTime();
	private final Type type;
	private static long alltime = System.nanoTime();
	private static Map<String,QueryGroup> mimetypes = new HashMap<String,QueryGroup>();
	private static Map<String,QueryGroup> tags = new HashMap<String,QueryGroup>();
	public final static QueryGroup GROUP_NO_GROUP = QueryGroup.create("Trash", Type.MIME);
	public final static Set<QueryGroup> SET_NO_GROUP;
	static {
		Set<QueryGroup> tmp = new HashSet<QueryGroup>();
		tmp.add(GROUP_NO_GROUP);
		SET_NO_GROUP = Collections.unmodifiableSet(tmp);
	}

	public static QueryGroup create(String value, Type type) {
		QueryGroup q = null;
		switch (type) {
			case MIME:
				q = mimetypes.get(value);
				if (q == null) {
					mimetypes.put(value, q = new QueryGroup(value, Type.MIME));
					QueryGroup.touchRoot();
				}
				break;
			case TAG:
				q = tags.get(value);
				if (q == null) {
					tags.put(value, q = new QueryGroup(value, Type.TAG));
					QueryGroup.touchRoot();
				}
				break;
		}
		assert q != null;
		return q;
	}

	private QueryGroup(String value, Type type) {
		this.type = type;
		this.value = value;
	}

	public static boolean allValid(long timestamp) {
		return timestamp >= alltime;
	}

	public boolean stampValid(long timestamp) {
		return timestamp >= time;
	}

	public void touch() {
		time = System.nanoTime();
	}

	public static void touchRoot() {
		alltime = System.nanoTime();
	}

	public Type getType() {
		return this.type;
	}

	public String getValue() {
		return this.value;
	}

	public boolean equals(Object other) {
		if (other == this)
			return true;
		QueryGroup o = (QueryGroup)other;
		return o.getType() == getType() && o.getValue().equals(getValue());
	}

	public int hashCode() {
		return type.hashCode() + value.hashCode();
	}

	public String toString() {
		return "<" + getType() + " " + getValue() + ">";
	}
}
