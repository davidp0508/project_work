package messaging;

public class Message {
	private static Message instance;
	public String name;
	public String ip;
	public String type;
	public String content;
	
	Message(String name, String ip, String type, String content){
		this.name = name;
		this.ip = ip;
		this.type = type;
		this.content = content;
	}

	public static synchronized Message getInstance(String name, String ip, String type, String content){
		if(instance == null){
			instance = new Message(name, ip, type, content);
		} else {
			instance.name = name;
			instance.ip = ip;
			instance.type = type;
			instance.content = content;
		}
		return instance;
	}
}