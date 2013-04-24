package us.sosia.video.stream.common;

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
	
	@Override
	public String toString() {
		return otpString;
	}
}