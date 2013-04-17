package us.sosia.video.stream.agent.messaging;

import java.io.IOException;
import java.util.ArrayList;

import com.ericsson.otp.erlang.*;

public class Listener implements Runnable {	

	private String name;
	private String ip;
	private NameIpPort[] ipArray;
	private ArrayList<Message> receivedMsgs;
	public Listener(String name, String ip, ArrayList<Message> receivedMsgs, NameIpPort[] ipArray){
		this.name = name;
		this.ip = ip;
		this.ipArray = ipArray;
		this.receivedMsgs = receivedMsgs;
	}
	@Override
	public void run() {
//		IPAddress ip = new IPAddress();
//		String myIP = ip.getIPaddress(); 
		OtpNode node = null;

		try {
			node = new OtpNode(name + "@" + ip, "test");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
        OtpMbox mbox = node.createMbox(name); //just to make it easier to follow the same format
        OtpErlangAtom SHUTDOWN = new OtpErlangAtom("shutdown");
        System.out.println("Listener ("+node.toString()+") started\n");
        
        while (true) 
        {
            OtpErlangObject message = null;
			try {
				//System.out.println("Waiting to receive...\n");
				message = mbox.receive();
				//System.out.println("Got message "+message+"\n");
			} catch (OtpErlangExit e) {
				e.printStackTrace();
			} catch (OtpErlangDecodeException e) {
				e.printStackTrace();
			}
			System.out.format("Listener>>received %s%n", message);
            OtpErlangTuple messageTuple = (OtpErlangTuple) message;
            
            String content = messageTuple.elementAt(0).toString();
            content = content.substring(1, content.length()-1);
            
            String srcIP = messageTuple.elementAt(1).toString();
            srcIP = srcIP.substring(srcIP.indexOf("@")+1, srcIP.length()-1);
            
            String srcName = "";
            
            String type = content.substring(0, content.indexOf(' '));
            content = content.substring(content.indexOf(' ')+1);
            for (int i=0;i<ipArray.length;i++)
            	if(ipArray[i].ip.equals(srcIP))
            		srcName = ipArray[i].hostname;
            Message received = Message.getInstance(srcName, srcIP, type, content);
            //System.out.println(received.name + received.ip + received.type + received.content);
            synchronized(receivedMsgs){
            	receivedMsgs.add(received);
            	System.out.println("Listener>>"+receivedMsgs.size() + receivedMsgs.get(0).ip);           	
            }
			//System.out.println(messageTuple.elementAt(0));
			//System.out.println(messageTuple.elementAt(1));

        	//OtpErlangTuple payload = (OtpErlangTuple) messageTuple.elementAt(0);
        	//System.out.println("Received a message of type "+payload.elementAt(2)+" from "+payload.elementAt(0)+"\n");


//            if (SHUTDOWN.equals(message)) 
//            {
//                System.out.format("%s shutting down...%n", mbox.self());
//                break;
//            } 
//            else if (message instanceof OtpErlangTuple) 
//            {
//                messageTuple = (OtpErlangTuple) message;
//                
//                //System.out.println("message arity is "+messageTuple.arity()+"\n");
//                //System.out.println("first element is "+messageTuple.elementAt(0)+"\n");
//                //if (messageTuple.arity() == 2 && messageTuple.elementAt(0) instanceof OtpErlangPid)
//                if (messageTuple.arity() == 2 && messageTuple.elementAt(0) instanceof OtpErlangTuple) 
//                {
//                	payload = (OtpErlangTuple) messageTuple.elementAt(0);
//                	System.out.println("Received a message of type "+payload.elementAt(2)+" from "+payload.elementAt(0)+"\n");
//                    /*OtpErlangPid sender = (OtpErlangPid) messageTuple.elementAt(0);
//                    OtpErlangObject sendersMessage = messageTuple.elementAt(1);
//                    mbox.send(sender, sendersMessage);*/
//                }
//            }
        }	
	}
}