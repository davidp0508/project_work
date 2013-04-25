package messaging;

import java.io.IOException;
import java.net.UnknownHostException;


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
	 *            - eg.client..
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
		// TODO command line not work
		SenderServer mySender = new SenderServer(serverName);
		Thread threadSendServer = new Thread(mySender);
		threadSendServer.start();

//		try {
//			Thread.sleep(2000);
			System.out.println("started sender server " + serverName);
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}

		sender_server = new OtpPeer(serverName.concat("@" + myIP)); // must create a sender_server instance on each node to handle sending
		System.out.println(sender_server);
		while (true) {// poll for sender_server until it's up...this is slightly more elegant than s leeping for some arbitrary time period
			try {
				Thread.sleep(500);
				connection = client.connect(sender_server);
				System.out.println("Messager>>>client connect to sender_server " + sender_server);
				break;
			} catch (UnknownHostException e) {
				System.out.println("server not found\n");
			} catch (OtpAuthException e) {
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} catch (InterruptedException e) {
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
		msg[0] = new OtpErlangAtom(dst.alive());// the one before @
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
			connection.sendRPC("message_passing", "unicastSend", formatArgs(tuple));
		} catch (IOException e1) {
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

	
	private OtpErlangObject[] formatArgs(OtpErlangTuple tup) {
		System.out.println("Tuple as string: " + tup + "\n");
		return new OtpErlangObject[] { tup };
	}


	@Override
	public String toString() {
		return client + "<->" + sender_server;
	}
}