package messaging;

import java.io.IOException;
import java.util.ArrayList;

import com.ericsson.otp.erlang.*;

public class Listener implements Runnable {	

	private String selfName;
	private String ip;
	private ArrayList<Message> receivedMsgs;

	public Listener(String selfName, String ip, ArrayList<Message> receivedMsgs){
		this.selfName = selfName;
		this.ip = ip;
		this.receivedMsgs = receivedMsgs;
	}

	@Override
	public void run() {
//		IPAddress ip = new IPAddress();
//		String myIP = ip.getIPaddress(); 
		OtpNode node = null;

		try {
			node = new OtpNode(selfName + "@" + ip, "test");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        OtpMbox mbox = node.createMbox(selfName); //just to make it easier to follow the same format
        OtpErlangAtom SHUTDOWN = new OtpErlangAtom("shutdown");
        System.out.println("Listener ("+node.toString()+") started\n");
        
        while (true) 
        {
            OtpErlangObject message = null;
			try {
//				System.out.println("Waiting to receive...\n");
				message = mbox.receive();
				//System.out.println("Got message "+message+"\n");
			} catch (OtpErlangExit e) {
				e.printStackTrace();
			} catch (OtpErlangDecodeException e) {
				e.printStackTrace();
			}
			System.out.format("Listener>>received %s%n", message);
			
            OtpErlangTuple messageTuple = (OtpErlangTuple) message;
            
            /* *****************New - Payload parsing */
            OtpErlangObject payload = (OtpErlangObject) messageTuple.elementAt(0);
            OtpErlangTuple payloadTuple = (OtpErlangTuple) payload;
            
            /* * modified */
            String content = payloadTuple.elementAt(3).toString();
            content = content.substring(1, content.length()-1);
            
            String type = payloadTuple.elementAt(2).toString();
            type =	type.substring(1, type.length()-1);
            /* * end modify */
            
            String srcAddr = messageTuple.elementAt(1).toString();
            String srcIP = srcAddr.substring(srcAddr.indexOf("@")+1, srcAddr.length()-1); 
            String srcName = srcAddr.substring(1, srcAddr.indexOf("@")) ;
            
            //TODO
            //String type = content.substring(0, content.indexOf(' '));
            //content = content.substring(content.indexOf(' ')+1);
            
            Message received = Message.getInstance(srcName, srcIP, type, content);
            //System.out.println(received.name + received.ip + received.type + received.content);
            synchronized(receivedMsgs){
            	receivedMsgs.add(received);
            	System.out.println("Listener>>"+receivedMsgs.size() + receivedMsgs.get(0).ip);           	
            }


        }	
	}
}