package messaging;

public final class MSGTYPE {
	public static final String TOKEN = "TOKEN";
	public static final String BEGIN = "BEGIN";
	public static final String ROUNDFINISHED = "ROUNDFINISHED";
	public static final String ANSWER = "ANSWER";
	public static final String SCOREUPDATE = "SCOREUPDATE";
	//TODO more types
	
	// Game Hall Server
	public static final String  SHOW_ROOMS = "SHOW_ROOMS";
	public static final String  CREATE_NEWROOM = "CREATE_NEWROOM";
	public static final String  JOIN_ROOM = "JOIN_ROOM";
	public static final String  GET_CARD = "GET_CARD";
	public static final String  LEAVE = "LEAVE";
	
	private MSGTYPE() {
	
	}
}
