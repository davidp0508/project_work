package messaging;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
	private OtpPeer			sender_server;
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
		return sender_server;
	}

	public String getMyIp() {
		return myIp;
	}

	/**
	 * 
	 * @param selfName
	 *            - eg.dmei, david
	 * @param sender_server
	 *            - eg. sender_server0@128.237.231.0
	 * @param cookie
	 *            - for example, test
	 */
	public Messager(String selfName, String sender_server, String cookie) {
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
			client = new OtpSelf(selfName + "@".concat(myIp), cookie);
			System.out.println("construct self node: " + client);
			/*
			 * System.out.println("create client " + selfNode); server = new OtpPeer(local_server); // must create a server instance on each node to handle sending/receiving
			 * connection = client.connect(server); System.out.println("connected to server");
			 */
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//setup local sender server 
		initServers(selfName, myIp, sender_server.substring(0, sender_server.indexOf("@") + 1)); // start up the servers
	}

	private void initServers(String selfNode, String myIP, String serverName) {
		/*
		 * Sets up local server services on each node. The sender_server takes Java to Erlang sendRPC calls and integrates with the Erlang lower layer to perform message passing.
		 * The receiver_server receives all messages from other nodes and allows for Java-level processing.
		 */
		// String receiver_server = "david@"; //"receiver_server@";
		OtpNode node = null;
		NameIpPort[] ipArray = null;
		ArrayList<Message> receivedMsgs = null;

//		Listener myListener = new Listener(selfNode, myIP, receivedMsgs, ipArray); // start the Java server for receiving messages
//		Thread threadServer = new Thread(myListener);
//		threadServer.start(); // may have a race condition between this and the sendMsg function

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

		sender_server = new OtpPeer(serverName.concat(myIP)); // must create a sender_server instance on each node to handle sending
		try {
			connection = client.connect(sender_server);
			System.out.println("Messager>>>client connect to sender_server "+sender_server);
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
	public void unicastMsg(OtpErlangAtom type, OtpPeer dst, String dstIP, String content) {
//	public void sendMsg(OtpSelf self, OtpErlangAtom type, OtpPeer dst, String myIP, String yourIP) {
		// Unicast Stuff
		OtpErlangObject[] payload = new OtpErlangObject[4]; // contains src, dst, type, seq #
		OtpErlangObject[] msg = new OtpErlangObject[3];
		msg[0] = new OtpErlangAtom(dst.alive());
		msg[1] = new OtpErlangAtom(dst.toString());
		// msg[2] = new OtpErlangAtom("hello_world"); //a basic payload

		// A more advanced payload
		//TODO the "self" param can be used to identify sender?? consider remove it ...
		payload[0] = new OtpErlangAtom(client.alive()); // we only want the username
		payload[1] = new OtpErlangAtom(dst.alive()); // this will later most likely be passed in as an argument of type OtpPeer
		payload[2] = type;
		//TODO remove hardcode
		payload[3] = new OtpErlangAtom(content); // this will need to be defined like in our labs
		msg[2] = new OtpErlangTuple(payload);

		OtpErlangTuple tuple = new OtpErlangTuple(msg);
		OtpErlangObject response = null;

		try {
			// System.out.println("Tuple as string before: "+withArgs(tuple).toString()+"\n");
			connection.sendRPC("message_passing", "unicastSend", formatArgs(tuple));
		
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


	private OtpErlangObject[] formatArgs(OtpErlangTuple tup) {
		System.out.println("Tuple as string: " + tup.toString() + "\n");
		return new OtpErlangObject[] { tup };
	}


	@Override
	public String toString() {
		return client + "<->" + sender_server;
	}

}