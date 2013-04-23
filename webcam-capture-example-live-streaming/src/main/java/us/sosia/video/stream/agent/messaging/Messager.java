package us.sosia.video.stream.agent.messaging;

import java.io.IOException;
import java.net.UnknownHostException;

import us.sosia.video.stream.common.Player;

import com.ericsson.otp.erlang.OtpAuthException;
import com.ericsson.otp.erlang.OtpConnection;
import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangExit;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpPeer;
import com.ericsson.otp.erlang.OtpSelf;

public class Messager {
	private OtpSelf			client;
	private OtpPeer			sender_server;
	private OtpConnection	connection;
	private String			myIp;

	/** interfaces */
	public OtpSelf getClient() {
		return client;
	}

	public OtpPeer getServer() {
		return sender_server;
	}

	public String getMyIp() {
		return myIp;
	}

	/**
	 * 
	 * @param selfName
	 *            - eg.dmei, david
	 * @param senderServerName
	 *            - eg. sender_server0@128.237.231.0
	 * @param cookie
	 *            - for example, test
	 */
	public Messager(String selfName, String senderServerName, String cookie) {
		IPAddress ip = new IPAddress();
		this.myIp = ip.getIPaddress();// auto get my IP

		/* construct nodes */
		try {
			client = new OtpSelf(selfName + "@".concat(myIp), cookie);
			/*
			 * System.out.println("create client " + selfNode); server = new OtpPeer(local_server); // must create a server instance on each node to handle sending/receiving
			 * connection = client.connect(server); System.out.println("connected to server");
			 */
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// setup local sender server
		initServers(selfName, myIp, senderServerName.substring(0, senderServerName.indexOf("@"))); // start up the servers
	}

	private void initServers(String selfNode, String myIP, String serverName) {
		/*
		 * Sets up local server services on each node. The sender_server takes Java to Erlang sendRPC calls and integrates with the Erlang lower layer to perform message passing.
		 * The receiver_server receives all messages from other nodes and allows for Java-level processing.
		 */
		// SenderServer mySender = new SenderServer(serverName);
		// Thread threadSendServer = new Thread(mySender);
		// threadSendServer.start();
		//
		// try {
		// Thread.sleep(2000);
		// System.out.println("started sender server "+serverName);
		// } catch (InterruptedException e1) {
		// e1.printStackTrace();
		// }
		// String receiver_server = "david@"; //"receiver_server@";
		// OtpNode node = null;
		// NameIpPort[] ipArray = null;
		// ArrayList<Message> receivedMsgs = null;

		// Listener myListener = new Listener(selfNode, myIP, receivedMsgs, ipArray); // start the Java server for receiving messages
		// Thread threadServer = new Thread(myListener);
		// threadServer.start(); // may have a race condition between this and the sendMsg function

		sender_server = new OtpPeer(serverName.concat("@" + myIP)); // must create a sender_server instance on each node to handle sending
		System.out.println(sender_server);
		while (true) {// poll for sender_server until it's up...this is slightly more elegant than s leeping for some arbitrary time period
			try {
				connection = client.connect(sender_server);
				System.out.println("Messager>>>client connect to sender_server " + sender_server);
				break;
			} catch (UnknownHostException e) {
				System.out.println("server not found\n");
			} catch (OtpAuthException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}
	}

	public void unicastMsg(OtpErlangAtom type, String dstOtp, String dstIP, String content) {
		if (content == null)
			content = "";
		// public void sendMsg(OtpSelf self, OtpErlangAtom type, OtpPeer dst, String myIP, String yourIP) {
		// Unicast Stuff
		OtpErlangObject[] payload = new OtpErlangObject[4]; // contains src, dst, type, seq #
		OtpErlangObject[] msg = new OtpErlangObject[3];
		OtpPeer dst = new OtpPeer(dstOtp);
		msg[0] = new OtpErlangAtom(dst.alive());//the one before @
		msg[1] = new OtpErlangAtom(dst.toString());
		// A more advanced payload
		// TODO the "self" param can be used to identify sender?? consider remove it ...
		payload[0] = new OtpErlangAtom(client.alive()); // we only want the username
		payload[1] = new OtpErlangAtom(dst.alive()); // this will later most likely be passed in as an argument of type OtpPeer
		payload[2] = type;
		payload[3] = new OtpErlangAtom(content); // this will need to be defined like in our labs
		msg[2] = new OtpErlangTuple(payload);

		OtpErlangTuple tuple = new OtpErlangTuple(msg);
		OtpErlangObject response = null;

		try {
			// System.out.println("Tuple as string before: "+withArgs(tuple).toString()+"\n");
			connection.sendRPC("message_passing", "unicastSend", formatArgs(tuple));
			// System.out.println("Testing multicast...\n");
			// connection.sendRPC("message_passing", "multicastSend", formatArgs(mcTuple));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			// This only captures the success of sending the RPC, not of the response /
			response = connection.receiveRPC(); // connection.receiveMsg().getMsg();
		} catch (OtpErlangExit e) {
			e.printStackTrace();
		} catch (OtpAuthException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// can do some error checking here to make sure it sent successfully
		System.out.println("Status of sendRPC call: " + response.toString() + "\n");
	}

	public void multicastMsg(OtpErlangAtom type, User[] dsts, String content) {
		if (content == null)
			content = "";
//	public void multicastMsg(OtpErlangAtom type, String[] dstOtp, String[] dstIP, String content) {
		OtpErlangObject[] payload = new OtpErlangObject[4]; // contains src, dst, type, seq #
		payload[0] = new OtpErlangAtom(client.alive()); // we only want the username
//		payload[1] = new OtpErlangAtom(dst.alive()); // this will later most likely be passed in as an argument of type OtpPeer
		payload[1] = new OtpErlangAtom(new Integer(dsts.length).toString());
		payload[2] = type;
		payload[3] = new OtpErlangAtom(content); // this will need to be defined like in our labs
		System.out.println("prepare multicast to "+payload[1] + " " + payload[2] + " " + payload[3]);
		
//		String[][] userList = new String[3][2]; // contains the raw list of users (get this from node logic later)
		OtpErlangObject[] user = new OtpErlangObject[2]; // contains the pieces of a user (username, IP)
		OtpErlangObject[] user_list = new OtpErlangTuple[3]; // user list as an Erlang object
		OtpErlangTuple temp = null;
		// TODO the hard-coded chunk below should be replaced by node logic to get userList
//		userList[0][0] = "david";
//		userList[0][1] = myIp;// "192.168.1.48";
//		userList[1][0] = "shifa";
//		userList[1][1] = dstIP;// "192.168.1.48";
//		userList[2][0] = "dan";
//		userList[2][1] = myIp;// "192.168.1.48";
		// Erlang's list of tuples looks like this: [{david, '192.168.1.44'}, {joe, '192.168.1.44'}, {local_server, '192.168.1.44'}]

		for (int i = 0; i < dsts.length; i++) {
//			user[0] = new OtpErlangAtom(userList[i][0]);
//			user[1] = new OtpErlangAtom(userList[i][1]);
			user[0] = new OtpErlangAtom(dsts[i].hostname);
			user[1] = new OtpErlangAtom(dsts[i].ip);
			temp = new OtpErlangTuple(user);
			// System.out.println("Temp is "+temp.toString()+"\n");
			user_list[i] = temp;
		}

		OtpErlangObject[] mcMsg = new OtpErlangObject[4];
		// TODO in reality the first two fields must be some node in the user list (instead of hard-coded in)
		OtpPeer dst = new OtpPeer(dsts[0].otpString);
		mcMsg[0] = new OtpErlangAtom(dst.alive());
		mcMsg[1] = new OtpErlangAtom(dst.toString()); 
		mcMsg[2] = new OtpErlangTuple(payload);
		mcMsg[3] = new OtpErlangList(user_list);
		System.out.println(mcMsg);
		OtpErlangTuple mcTuple = new OtpErlangTuple(mcMsg);
		OtpErlangObject response = null;
		
		try {
			// System.out.println("Tuple as string before: "+withArgs(tuple).toString()+"\n");
//			connection.sendRPC("message_passing", "unicastSend", formatArgs(tuple));
			// System.out.println("Testing multicast...\n");
			 connection.sendRPC("message_passing", "multicastSend", formatArgs(mcTuple));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			// This only captures the success of sending the RPC, not of the response /
			response = connection.receiveRPC(); // connection.receiveMsg().getMsg();
		} catch (OtpErlangExit e) {
			e.printStackTrace();
		} catch (OtpAuthException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// can do some error checking here to make sure it sent successfully
		System.out.println("Status of sendRPC call: " + response.toString() + "\n");
	}

	// Starting integration between David's and Han's implementations of sendMsg()
	public void sendMsg(String payload, String name, String ip) {// TODO other paras instead of hardcode
		System.out.println("Han's send...........");
		/* Unicast Stuff */
		OtpErlangObject[] msg = new OtpErlangObject[3];
		msg[0] = new OtpErlangAtom(name);
		msg[1] = new OtpErlangAtom(name + "@" + ip);
		msg[2] = new OtpErlangAtom(payload);
		OtpErlangTuple tuple = new OtpErlangTuple(msg);
		OtpErlangObject response = null;
		// Multicast Stuff
		// String[][] userList = new String[3][2]; //contains the raw list of users (get this from node logic later)
		// OtpErlangObject[] user = new OtpErlangObject[2]; //contains the pieces of a user (username, IP)
		// OtpErlangObject[] user_list = new OtpErlangTuple[3]; //user list as an Erlang object
		// OtpErlangTuple temp = null;
		// //the hard-coded chunk below should be replaced by node logic to get userList
		// userList[0][0] = "david";
		// userList[0][1] = myIP;//"192.168.1.48";
		// userList[1][0] = "shifa";
		// userList[1][1] = yourIP;//"192.168.1.48";
		// userList[2][0] = "dan";
		// userList[2][1] = myIP;//"192.168.1.48";
		// //Erlang's list of tuples looks like this: [{david, '192.168.1.44'}, {joe, '192.168.1.44'}, {local_server, '192.168.1.44'}]
		//
		// for(int i=0; i<userList.length; i++)
		// {
		// user[0] = new OtpErlangAtom(userList[i][0]);
		// user[1] = new OtpErlangAtom(userList[i][1]);
		// temp = new OtpErlangTuple(user);
		// //System.out.println("Temp is "+temp.toString()+"\n");
		// user_list[i] = temp;
		// }
		//
		// OtpErlangObject[] mcMsg = new OtpErlangObject[4];
		// mcMsg[0] = new OtpErlangAtom(dst.alive());
		// mcMsg[1] = new OtpErlangAtom(dst.toString()); //in reality the first two fields must be some node in the user list (instead of hard-coded in)
		// //mcMsg[2] = new OtpErlangAtom("testingMC"); //specify the payload here
		// mcMsg[2] = new OtpErlangTuple(payload);
		// mcMsg[3] = new OtpErlangList(user_list);
		// OtpErlangTuple mcTuple = new OtpErlangTuple(mcMsg);

		try {
			System.out.println("Tuple as string before: " + formatArgs(tuple).toString() + "\n");
			connection.sendRPC("message_passing", "unicastSend", formatArgs(tuple)); // unicast send
			// connection.sendRPC("message_passing", "multicastSend", withArgs(mcTuple)); //multicast send
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			response = connection.receiveRPC(); // connection.receiveMsg().getMsg();
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
		System.out.println("Status of RPC: " + response.toString() + "\n");
	}

	private OtpErlangObject[] formatArgs(OtpErlangTuple tup) {
		System.out.println("Tuple as string: " + tup + "\n");
		return new OtpErlangObject[] { tup };
	}

	// TODO don't use this...This does not belong at this level. sendMsg should be called with an array of users, and the multicastSend RPC should be used. It is simple.
	public void multicast(int clientNo, String content, User[] ipArray) {
		for (int i = 0; i < ipArray.length; i++) {
			System.out.println("sending to " + i + ipArray[i].hostname);
			if (i != clientNo) {
				sendMsg(content, ipArray[i].hostname, ipArray[i].ip);
			}
		}
	}

	@Override
	public String toString() {
		return client + "<->" + sender_server;
	}
}