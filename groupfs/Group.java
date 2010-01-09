package groupfs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Either a tag or a mime type.
 * Groups are associated with file nodes, and represented by directories.
 */
public final class Group {
	public enum Type { TAG, MIME }
	public final String value;
	public final Type type;
	private static Map<String,Group> mimetypes = new HashMap<String,Group>();
	private static Map<String,Group> tags = new HashMap<String,Group>();

	/**
	 * The group that must be assigned to nodes with no other groups.
	 * (no mimetype either - the node is considered "in trash")
	 */
	public final static Group GROUP_NO_GROUP = Group.create("Trash", Type.MIME);

	/**
	 * The set that has only GROUP_NO_GROUP.
	 */
	public final static Set<Group> SET_NO_GROUP;

	/**
	 * The empty set.
	 */
	public final static Set<Group> SET_EMPTY_SET;

	static {
		Set<Group> tmp = new HashSet<Group>();
		tmp.add(GROUP_NO_GROUP);
		SET_NO_GROUP = Collections.unmodifiableSet(tmp);
		tmp = new HashSet<Group>();
		SET_EMPTY_SET = Collections.unmodifiableSet(tmp);
	}

	/**
	 * @return Instance of group or new group.
	 * Groups of same value and type are guaranteed to have the
	 * same memory address.
	 */
	public static Group create(String value, Type type) {
		Group q = null;
		switch (type) {
			case MIME:
				q = mimetypes.get(value);
				if (q == null)
					mimetypes.put(value, q = new Group(value, Type.MIME));
				break;
			case TAG:
				q = tags.get(value);
				if (q == null)
					tags.put(value, q = new Group(value, Type.TAG));
				break;
		}
		assert q != null;
		return q;
	}

	/**
	 * @param	value	Not null.
	 * @param	type	Not null.
	 */
	private Group(String value, Type type) {
		this.type = type;
		this.value = value;
	}

	public String toString() {
		return "<" + type + " " + value + ">";
	}
}
