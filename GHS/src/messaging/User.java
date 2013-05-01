package messaging;

public class User {
	public String hostname;
	public String ip;
	public int port;
	public String otpString;

	public User(String hostname, String ip, int port){
		this.hostname = hostname;
		this.ip = ip;
		this.port = port;
		this.otpString = hostname + "@" + ip;
	}

	public User(String hostname, String ip) {
		this.hostname = hostname;
		this.ip = ip;
		this.otpString = hostname + "@" + ip;
	}

	
}
