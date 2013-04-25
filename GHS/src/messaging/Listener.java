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

				message = mbox.receive();

			} catch (OtpErlangExit e) {
				e.printStackTrace();
			} catch (OtpErlangDecodeException e) {
				e.printStackTrace();
			}
			System.out.format("Listener>>received %s%n", message);
			
			if (message instanceof OtpErlangTuple) {
				// eg: received {{client,dmei,'ANSWER','wtf...!!'},'sender_server1@192.168.0.114'}
				OtpErlangTuple messageTuple = (OtpErlangTuple) message;
				System.out.println(messageTuple.arity());
				String srcNode = null, type = null, payload = null;
				srcNode = messageTuple.elementAt(1).toString().replaceAll("'", "");
				if (messageTuple.elementAt(0) instanceof OtpErlangTuple) {
					
					OtpErlangTuple tmp = (OtpErlangTuple) messageTuple.elementAt(0);
					type = tmp.elementAt(2).toString().replaceAll("'", "");
					payload = tmp.elementAt(tmp.arity() - 1).toString().replaceAll("'", "");
					System.out.println(srcNode + ">>> type: " + type + ", payload: " + payload);
				}

				Message received = new Message(srcNode, MSGTYPE.fromString(type), payload);
				synchronized (receivedMsgs) {
					receivedMsgs.add(received);
					System.out.println("Listener>>" + receivedMsgs.size() + receivedMsgs.get(0));
				}
			} else {
				// TODO message might be corrupted. request resend or whatever
			}
			
//            OtpErlangTuple messageTuple = (OtpErlangTuple) message;
//            
//            /* *****************New - Payload parsing */
//            OtpErlangObject payload = (OtpErlangObject) messageTuple.elementAt(0);
//            OtpErlangTuple payloadTuple = (OtpErlangTuple) payload;
//            
//            String content = payloadTuple.elementAt(3).toString();
//            if(content.startsWith("'")){
//            	content = content.substring(1, content.length()-1);
//            }
//              
//            String type = payloadTuple.elementAt(2).toString();
//            type =	type.substring(1, type.length()-1);
//
//            
//            String srcAddr = messageTuple.elementAt(1).toString();
//            String srcIP = srcAddr.substring(srcAddr.indexOf("@")+1, srcAddr.length()-1); 
//            String srcName = srcAddr.substring(1, srcAddr.indexOf("@")) ;
//            
//          
//            Message received = Message.getInstance(srcName, srcIP, type, content);
//            //System.out.println(received.name + received.ip + received.type + received.content);
//            synchronized(receivedMsgs){
//            	receivedMsgs.add(received);
//            	System.out.println("Listener>>"+receivedMsgs.size() + receivedMsgs.get(0).ip);           	
//            }


        }	
	}
}