package us.sosia.video.stream.agent.messaging;

public class NameIpPort {
	public String hostname;
	public String ip;
	public int port;
	public NameIpPort(String hostname, String ip, int port){
		this.hostname = hostname;
		this.ip = ip;
		this.port = port;
	}
}