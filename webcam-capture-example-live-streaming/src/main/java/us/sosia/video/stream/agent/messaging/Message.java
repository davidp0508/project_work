package us.sosia.video.stream.agent.messaging;

import java.io.Serializable;

/**
 * Wrap message passed by Erlang layer and deliver to client (GameWindow)
 * 
 * @author dmei
 * 
 */
public class Message implements Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	private String				srcName;
	private String				srcIp;
	private MSGTYPE				type;
	private String				payload;

	public Message(String srcNode, MSGTYPE type, String payload) {
		int idx = srcNode.indexOf("@");
		this.srcName = srcNode.substring(0, idx);
		this.srcIp = srcNode.substring(idx + 1);
		this.type = type;
		this.payload = payload;
	}

	public String getSrcName() {
		return srcName;
	}

	public void setSrcName(String srcName) {
		this.srcName = srcName;
	}

	public String getSrcIp() {
		return srcIp;
	}

	public void setSrcIp(String srcIp) {
		this.srcIp = srcIp;
	}

	public MSGTYPE getType() {
		return type;
	}

	public void setType(MSGTYPE type) {
		this.type = type;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		return srcName + " " + type + " " + payload;
	}
}