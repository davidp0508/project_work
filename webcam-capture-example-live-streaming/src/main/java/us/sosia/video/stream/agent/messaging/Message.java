package us.sosia.video.stream.agent.messaging;

import java.io.Serializable;

import us.sosia.video.stream.common.CharadesConfig;

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

	private int					srcId;//the playerId
	private String				srcName;//TODO this needs a map (from GHS)..
	private String				srcIp;
	private MSGTYPE				type;
	private String				payload;

	public Message(String srcNode, MSGTYPE type, String payload) {
		int idx = srcNode.indexOf("@");
		this.srcId = Integer.parseInt(srcNode.substring(srcNode.indexOf(CharadesConfig.SENDER_PREFIX) + 1, idx));
//		this.srcName = srcNode.substring(0, idx);
		this.srcIp = srcNode.substring(idx + 1);
		this.type = type;
		this.payload = payload;
	}

	public int getSrcId() {
		return srcId;
	}

	public void setSrcId(int srcId) {
		this.srcId = srcId;
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
		return srcId + " " + type + " " + payload;
	}
}