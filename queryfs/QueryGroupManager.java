package queryfs;

import java.util.HashMap;
import java.util.Map;

import queryfs.QueryGroup.Type;

public class QueryGroupManager {
	private Map<String,QueryGroup> mimetypes = new HashMap<String,QueryGroup>();
	private Map<String,QueryGroup> tags = new HashMap<String,QueryGroup>();

	public QueryGroup create(String value, Type type) {
		QueryGroup q = null;
		switch (type) {
			case MIME:
				q = mimetypes.get(value);
				if (q == null) {
					mimetypes.put(value, q = new QueryGroup(value, Type.MIME));
					QueryGroup.touchAll();
				}
				break;
			case TAG:
				q = tags.get(value);
				if (q == null) {
					tags.put(value, q = new QueryGroup(value, Type.TAG));
					QueryGroup.touchAll();
				}
				break;
		}
		assert q != null;
		return q;
	}
}
