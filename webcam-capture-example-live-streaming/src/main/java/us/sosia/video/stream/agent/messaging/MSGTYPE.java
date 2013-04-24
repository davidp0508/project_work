package us.sosia.video.stream.agent.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public enum MSGTYPE {
	TOKEN("TOKEN"), BEGIN("BEGIN"), ROUNDFINISHED("ROUNDFINISHED"), ANSWER("ANSWER"), SCOREUPDATE("SCOREUPDATE"), FAILED("FAILED");
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
