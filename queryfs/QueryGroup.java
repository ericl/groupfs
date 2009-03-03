package queryfs;

public class QueryGroup {
	public static long alltime = System.currentTimeMillis();
	public enum Type { TAG, MIME }
	private final String value;
	private long time = System.currentTimeMillis();
	private final Type type;

	public QueryGroup(String value, Type type) {
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
		time = System.currentTimeMillis();
	}

	public static void touchAll() {
		alltime = System.currentTimeMillis();
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
