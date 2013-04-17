package us.sosia.video.stream.agent.messaging;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.android.dx.util.ByteArray.MyInputStream;
import com.ericsson.otp.erlang.OtpAuthException;
import com.ericsson.otp.erlang.OtpConnection;
import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangExit;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpNode;
import com.ericsson.otp.erlang.OtpPeer;
import com.ericsson.otp.erlang.OtpSelf;

public class Messager {
	private OtpSelf			client;
	private OtpPeer			server;
	private OtpConnection	connection;
	private String myIp;
//	private String					selfNode;
//	private String					sender_server;
//	private String					cookie;

	/** interfaces */
	public OtpSelf getClient() {
		return client;
	}

	public OtpPeer getServer() {
		return server;
	}

	public String getMyIp() {
		return myIp;
	}

	/**
	 * 
	 * @param selfNode
	 *            - eg.client
	 * @param sender_server
	 *            - eg. local_server@128.237.231.0
	 * @param cookie
	 *            - for example, test
	 */
	public Messager(String selfNode, String sender_server, String cookie) {
//		this.selfNode = selfNode;
//		this.sender_server = sender_server;
//		this.cookie = cookie;
		IPAddress ip = new IPAddress();
		this.myIp = ip.getIPaddress();// auto get my IP
		// String yourIP = "192.168.1.59";//myIP; //should be whatever the other user's IP is, ultimately
		// String me = "david"; //will come from the user starting up the application, ultimately
		// String sender_server = "sender_server@";
		// //String tmp_dst = "shifa@"; //should come from node logic that knows other players
		// String[] tmp_dst = new String[2]; //just temporary for testing
		// tmp_dst[0] = "shifa@";
		// tmp_dst[1] = "joe@";

		/* construct nodes */
		try {
			client = new OtpSelf(selfNode, cookie);
			/*
			 * System.out.println("create client " + selfNode); server = new OtpPeer(local_server); // must create a server instance on each node to handle sending/receiving
			 * connection = client.connect(server); System.out.println("connected to server");
			 */
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//setup local sender server 
		initServers(selfNode, myIp, sender_server.substring(0, sender_server.indexOf("@") + 1)); // start up the servers
	}

	private void initServers(String selfNode, String myIP, String sender_server) {
		/*
		 * Sets up local server services on each node. The sender_server takes Java to Erlang sendRPC calls and integrates with the Erlang lower layer to perform message passing.
		 * The receiver_server receives all messages from other nodes and allows for Java-level processing.
		 */
		// String receiver_server = "david@"; //"receiver_server@";
		OtpNode node = null;
		NameIpPort[] ipArray = null;
		ArrayList<Message> receivedMsgs = null;

		Listener myListener = new Listener(selfNode, myIP, receivedMsgs, ipArray); // start the Java server for receiving messages
		Thread threadServer = new Thread(myListener);
		threadServer.start(); // may have a race condition between this and the sendMsg function

		/*
		 * The code commented out below is an attempt at trying to get the sender_server to run from Java. It currently doesn't work because of some issues with putting the command
		 * line together. I will try to figure it out at some point, but right now there are more pressing issues.
		 */
		// String erlName = "erl"; //may be platform independent?
		// String options = " +K true +P 500000 -name '"; //must be there
		// String server = sender_server.concat(myIP);
		// String cookiePrefix = "' -setcookie ";
		// String cookie = "test"; //may change based on what we plan to do (needs to be changed elsewhere in this Project if so)
		// String module = "message_passing"; //name of Erlang module
		// String function = ":start("; //name of function to call
		// String closing = ").";
		//
		// final long timeout = 10000000;
		// final ProcessExecutorHandler procHdlr = null;
		// ProcessExecutor proc = null;
		// final CommandLine cmd = new CommandLine(erlName);
		// cmd.addArgument(options);
		// cmd.addArgument(server);
		// cmd.addArgument(cookiePrefix);
		// cmd.addArgument(cookie);
		// try {
		// proc.runProcess(cmd, procHdlr, timeout);
		// } catch (IOException e2) {
		// // TODO Auto-generated catch block
		// e2.printStackTrace();
		// }

		server = new OtpPeer(sender_server.concat(myIP)); // must create a sender_server instance on each node to handle sending
		try {
			connection = client.connect(server);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OtpAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* David's implementation of sendMsg() */
	public void sendMsg(OtpSelf self, OtpErlangAtom type, OtpPeer dst, String myIP, String yourIP) {
		System.out.println("David's send...........");
		System.out.println(client + " "+dst+" "+ yourIP);
		// Unicast Stuff
		OtpErlangObject[] payload = new OtpErlangObject[4]; // contains src, dst, type, seq #
		OtpErlangObject[] msg = new OtpErlangObject[3];
		msg[0] = new OtpErlangAtom(dst.alive());
		msg[1] = new OtpErlangAtom(dst.toString());
		// msg[2] = new OtpErlangAtom("hello_world"); //a basic payload

		// A more advanced payload
		payload[0] = new OtpErlangAtom(client.alive()); // we only want the username
		payload[1] = new OtpErlangAtom(dst.alive()); // this will later most likely be passed in as an argument of type OtpPeer
		payload[2] = type;
		payload[3] = new OtpErlangAtom("1"); // this will need to be defined like in our labs
		msg[2] = new OtpErlangTuple(payload);

		OtpErlangTuple tuple = new OtpErlangTuple(msg);
		OtpErlangObject response = null;

		// Multicast Stuff
		String[][] userList = new String[3][2]; // contains the raw list of users (get this from node logic later)
		OtpErlangObject[] user = new OtpErlangObject[2]; // contains the pieces of a user (username, IP)
		OtpErlangObject[] user_list = new OtpErlangTuple[3]; // user list as an Erlang object
		OtpErlangTuple temp = null;
		// the hard-coded chunk below should be replaced by node logic to get userList
		userList[0][0] = "david";
		userList[0][1] = myIP;// "192.168.1.48";
		userList[1][0] = "shifa";
		userList[1][1] = yourIP;// "192.168.1.48";
		userList[2][0] = "dan";
		userList[2][1] = myIP;// "192.168.1.48";
		// Erlang's list of tuples looks like this: [{david, '192.168.1.44'}, {joe, '192.168.1.44'}, {local_server, '192.168.1.44'}]

		for (int i = 0; i < userList.length; i++) {
			user[0] = new OtpErlangAtom(userList[i][0]);
			user[1] = new OtpErlangAtom(userList[i][1]);
			temp = new OtpErlangTuple(user);
			// System.out.println("Temp is "+temp.toString()+"\n");
			user_list[i] = temp;
		}

		OtpErlangObject[] mcMsg = new OtpErlangObject[4];
		mcMsg[0] = new OtpErlangAtom(dst.alive());
		mcMsg[1] = new OtpErlangAtom(dst.toString()); // in reality the first two fields must be some node in the user list (instead of hard-coded in)
		// mcMsg[2] = new OtpErlangAtom("testingMC"); //specify the payload here
		mcMsg[2] = new OtpErlangTuple(payload);
		mcMsg[3] = new OtpErlangList(user_list);
		OtpErlangTuple mcTuple = new OtpErlangTuple(mcMsg);

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OtpAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
			System.out.println(connection);
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
		System.out.println("Tuple as string: " + tup.toString() + "\n");
		return new OtpErlangObject[] { tup };
	}

	// TODO don't use this...This does not belong at this level. sendMsg should be called with an array of users, and the multicastSend RPC should be used. It is simple.
	public void multicast(int clientNo, String content, NameIpPort[] ipArray) {
		for (int i = 0; i < ipArray.length; i++) {
			System.out.println("sending to " + i + ipArray[i].hostname);
			if (i != clientNo) {
				sendMsg(content, ipArray[i].hostname, ipArray[i].ip);
			}
		}
	}

	@Override
	public String toString() {
		return client + "<->" + server;
	}

}