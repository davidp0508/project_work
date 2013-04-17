package us.sosia.video.stream.agent.messaging;

import java.util.HashMap;

public class MessageFactory {
	private static Messager messager = null;
//	private static HashMap<String, String> config;

	private MessageFactory(String selfNode, String peerNode, String cookie) {
		messager = new Messager(selfNode, peerNode, cookie);
	}

//	public static Messager getMessager(HashMap<String, String> config) {
//		if (messager == null)
//			messager = new Messager(config.get("selfNode"), config.get("peerNode"), config.get("cookie"));
//		return messager;
//	}

	/**
	 * 
	 * @param selfNode - eg.client
	 * @param peerNode - eg. local_server@128.237.231.0
	 * @param cookie - for example, test
	 */
	public static Messager getMessager(String selfNode, String peerNode, String cookie) {
		if (messager == null)
			messager = new Messager(selfNode, peerNode, cookie);
		return messager;
	}
}