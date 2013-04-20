package messaging;

public class MessageFactory {
	private static Messager messager = null;
//	private static HashMap<String, String> config;

	private MessageFactory(String selfNode, String sender_server, String cookie) {
		messager = new Messager(selfNode, sender_server, cookie);
	}

//	public static Messager getMessager(HashMap<String, String> config) {
//		if (messager == null)
//			messager = new Messager(config.get("selfNode"), config.get("peerNode"), config.get("cookie"));
//		return messager;
//	}

	/**
	 * 
	 * @param selfNode - eg.client
	 * @param sender_server - eg. sender_server0@128.237.231.0
	 * @param cookie - for example, test
	 */
	//TODO perhaps pass in playerId
	public static Messager getMessager(String selfNode, String sender_server, String cookie) {
		if (messager == null)
			messager = new Messager(selfNode, sender_server, cookie);
		return messager;
	}
}