package messagePassing;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
//import java.util.concurrent.locks.Lock;
import  com.ericsson.otp.erlang.*;
import org.apache.commons.exec.*;

public class Messaging {

	private static OtpSelf client;
	private static OtpPeer mySendserver;
	private static OtpConnection connection;


	public static void sendMsg(OtpSelf self, OtpErlangAtom type, OtpPeer dst, String myIP, String yourIP) 
	{    
		/* Unicast Stuff */
		OtpErlangObject[] payload = new OtpErlangObject[4]; //contains src, dst, type, seq #
		OtpErlangObject[] msg = new OtpErlangObject[3];
		msg[0] = new OtpErlangAtom(dst.alive());
		msg[1] = new OtpErlangAtom(dst.toString());
		//msg[2] = new OtpErlangAtom("hello_world"); //a basic payload

		/* A more advanced payload */
		payload[0] = new OtpErlangAtom(self.alive()); //we only want the username
		payload[1] = new OtpErlangAtom(dst.alive()); //this will later most likely be passed in as an argument of type OtpPeer
		payload[2] = type;
		payload[3] = new OtpErlangAtom("1"); //this will need to be defined like in our labs
		msg[2] = new OtpErlangTuple(payload);

		OtpErlangTuple tuple = new OtpErlangTuple(msg);
		OtpErlangObject response = null;

		/* Multicast Stuff */
		String[][] userList = new String[3][2]; //contains the raw list of users (get this from node logic later)
		OtpErlangObject[] user = new OtpErlangObject[2]; //contains the pieces of a user (username, IP)
		OtpErlangObject[] user_list = new OtpErlangTuple[3]; //user list as an Erlang object
		OtpErlangTuple temp = null;
		/*the hard-coded chunk below should be replaced by node logic to get userList*/
		userList[0][0] = "david";
		userList[0][1] = myIP;
		userList[1][0] = "shifa";
		userList[1][1] = yourIP;
		userList[2][0] = "joe";
		userList[2][1] = myIP;
		//Erlang's list of tuples looks like this: [{david, '192.168.1.44'}, {joe, '192.168.1.44'}, {local_server, '192.168.1.44'}]

		for(int i=0; i<userList.length; i++)
		{
			user[0] = new OtpErlangAtom(userList[i][0]);
			user[1] = new OtpErlangAtom(userList[i][1]);
			temp = new OtpErlangTuple(user);
			//System.out.println("Temp is "+temp.toString()+"\n");
			user_list[i] = temp;
		}

		OtpErlangObject[] mcMsg = new OtpErlangObject[4];
		mcMsg[0] = new OtpErlangAtom(dst.alive());
		mcMsg[1] = new OtpErlangAtom(dst.toString()); //in reality the first two fields must be some node in the user list (instead of hard-coded in)
		//mcMsg[2] = new OtpErlangAtom("testingMC"); //specify the payload here
		mcMsg[2] = new OtpErlangTuple(payload);
		mcMsg[3] = new OtpErlangList(user_list);
		OtpErlangTuple mcTuple = new OtpErlangTuple(mcMsg);

		try {
			//System.out.println("Tuple as string before: "+withArgs(tuple).toString()+"\n");
			connection.sendRPC("message_passing", "unicastSend", formatArgs(tuple));
			//System.out.println("Testing multicast...\n");
			//connection.sendRPC("message_passing", "multicastSend", formatArgs(mcTuple));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			/* This only captures the success of sending the RPC, not of the response */
			response = connection.receiveRPC(); //connection.receiveMsg().getMsg();
		} catch (OtpErlangExit e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OtpAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//can do some error checking here to make sure it sent successfully
		System.out.println("Status of sendRPC call: "+response.toString()+"\n");
	}


	private static OtpErlangObject[] formatArgs(OtpErlangTuple tup) {
		System.out.println("Tuple to send: "+tup.toString()+"\n");
		return new OtpErlangObject[] { 
				tup 
		};
	}


	public static void initServers(String myIP, String sender_server)
	{
		/* Sets up local server services on each node.
		 * The sender_server takes Java to Erlang sendRPC calls
		 * and integrates with the Erlang lower layer to perform
		 * message passing. The receiver_server receives all messages
		 * from other nodes and allows for Java-level processing.
		 */

		SenderServer mySender = new SenderServer(); //start the Java sender server for sending messages
		Thread threadSendServer = new Thread(mySender);
		threadSendServer.start(); //may have a race condition between this and the following line

		ReceiverServer myReceiver = new ReceiverServer(); //start the Java echo server for receiving messages
		Thread threadServer = new Thread(myReceiver);
		threadServer.start(); //may have a race condition between this and the sendMsg function

		//		try { //sleep for an arbitrary amount of time to "ensure" the sender_server is up...see while loop below
		//			Thread.sleep(5000);
		//		} catch (InterruptedException e1) {
		//			// TODO Auto-generated catch block
		//			e1.printStackTrace();
		//		}

		mySendserver = new OtpPeer(sender_server+"@"+myIP); //must create a sender_server instance on each node to handle sending 
		while(true) //poll for sender_server until it's up...this is slightly more elegant than sleeping for some arbitrary time period
		{
			try {
				connection = client.connect(mySendserver);
				try {
					Thread.sleep(1000); //if there's no sleep, the first RPC call almost always fails. checking connection.isAlive() or .isConnected() does not help
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				System.out.println("server not found\n"); //e.printStackTrace();
			} catch (OtpAuthException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//System.out.println("server not found\n"); //e.printStackTrace();
			}
		}
	}


	public static void main(String[] args)
	{
		IPAddress ip = new IPAddress();
		String myIP = ip.getIPaddress();
		String yourIP = myIP; //should be whatever the other user's IP is, ultimately
		String me = "david"; //will come from the user starting up the application, ultimately
		String sender_server = "sender_server";
		//String tmp_dst = "shifa@"; //should come from node logic that knows other players
		String[] tmp_dst = new String[2]; //just temporary for testing
		tmp_dst[0] = "shifa@";
		tmp_dst[1] = "joe@";

		/* Build a node for the local player */
		try {
			client = new OtpSelf(me, "test"); //this should be the username of the local player
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		initServers(myIP, sender_server);

		//OtpPeer dst = new OtpPeer(tmp_dst.concat(yourIP)); //commented out for testing purposes ONLY
		OtpPeer dst = null; 
		OtpErlangAtom type = new OtpErlangAtom("test"); //just for proof of concept; will need to be defined based on which logic is being used
		for(int i=0; i<2; i++) //loop is for testing purposes only
		{
			dst = new OtpPeer(tmp_dst[i].concat(yourIP));
			sendMsg(client, type, dst, myIP, yourIP); //send a message
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//if(i == 1)
			//i=-1; //reset it to go back and replay the list
		}
	}
}