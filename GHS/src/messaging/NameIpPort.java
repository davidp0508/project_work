package messaging;

public class NameIpPort {
	public String hostname;
	public String ip;
	public int port;
	public String otpString;

	public NameIpPort(String hostname, String ip, int port){
		this.hostname = hostname;
		this.ip = ip;
		this.port = port;
		this.otpString = hostname + "@" + ip;
	}
}