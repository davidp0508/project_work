package us.sosia.video.stream.common;

public class CharadesConfig {
	/** application high-level and UI paras */
	public static final String	APP_NAME		= "Act Something ";
	// number of players decide the number of video window
	public final static int		NUM_PLAYERS		= 2;
	// seconds for a round of game.
	public final static int		MAXSEC			= 120;
	// TODO any other states? if yes, use enum
	public final static int				ACTING			= 1;
	public final static int				GUESSING		= 2;
	//TODO game hints, messages, etc
	// TODO UI size, etc

	/** Messaging stuff */
	public static final String	COOKIE			= "test";
	public static final String	SENDER_PREFIX	= "sender_server";

}
