package queryfs;

public class QueryGroup {

	public enum Type { TAG, MIME }
	private final String value;
	private final Type type;

	public QueryGroup(String value, Type type) {
		this.type = type;
		this.value = value;
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
