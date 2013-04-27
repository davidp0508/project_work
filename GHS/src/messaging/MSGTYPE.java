package messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public enum MSGTYPE {
	
	SHOW_ROOMS("SHOW_ROOMS"), CREATE_NEWROOM("CREATE_NEWROOM"), JOIN_ROOM("JOIN_ROOM"),  
	GET_CARD("GET_CARD"),LEAVE("LEAVE"),
	IS_PRIMARY("IS_PRIMARY"), NODE_DOWN("NODE_DOWN"), ACK("ACK")
	;
//	 TODO more types

	private final String	name;
	private static final Map<String, MSGTYPE> map = new HashMap<String, MSGTYPE>();
	static {
		for (MSGTYPE t : MSGTYPE.values())
			map.put(t.name, t);
	}

	private MSGTYPE(String name) {
		this.name = name;
	}

	public static MSGTYPE fromString(String name) {
		if (map.containsKey(name))
			return map.get(name);
		throw new NoSuchElementException(name+" not found.");
	}
//	public boolean equalsType(String otherType) {
//		return (otherType == null) ? false : type.equals(otherType);
//	}

	@Override
	public String toString() {
		return name;
	}
}
