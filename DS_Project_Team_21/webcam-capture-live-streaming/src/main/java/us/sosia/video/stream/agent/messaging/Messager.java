package us.sosia.video.stream.agent.messaging;

import java.io.IOException;
import java.net.UnknownHostException;

import com.ericsson.otp.erlang.OtpAuthException;
import com.ericsson.otp.erlang.OtpConnection;
import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangExit;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpPeer;
import com.ericsson.otp.erlang.OtpSelf;

public class Messager {
	private OtpSelf			client;
	private OtpPeer			server;
	private OtpConnection	connection;

	private String			selfNode;
	private String			local_server;
	private String			cookie;

	/**
	 * 
	 * @param selfNode - eg.client
	 * @param local_server - eg. local_server@128.237.231.0
	 * @param cookie - for example, test
	 */
	public Messager(String selfNode, String local_server, String cookie) {
		this.selfNode = selfNode;
		this.local_server = local_server;
		this.cookie = cookie;
		/* construct nodes */
		try {
			client = new OtpSelf(selfNode, cookie);
			System.out.println("create client " + selfNode);
			server = new OtpPeer(local_server); // must create a server instance on each node to handle sending/receiving
			connection = client.connect(server);
			System.out.println("connected to server");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OtpAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// @Test
	public void sendMsg(String payload) {//TODO other paras instead of hardcode
		/* Unicast Stuff */
		OtpErlangObject[] msg = new OtpErlangObject[3];
		msg[0] = new OtpErlangAtom("hanyang");//peers
		msg[1] = new OtpErlangAtom("hanyang@128.237.118.168");
		msg[2] = new OtpErlangAtom(payload);
		OtpErlangTuple tuple = new OtpErlangTuple(msg);
		OtpErlangObject response = null;
		/* Multicast Stuff */
		// String[][] userList = new String[3][2]; //contains the raw list of users (get this from node logic later)
		// OtpErlangObject[] user = new OtpErlangObject[2]; //contains the pieces of a user (username, IP)
		// OtpErlangObject[] user_list = new OtpErlangTuple[3]; //user list as an Erlang object
		// OtpErlangTuple temp = null;
		// /*the hard-coded chunk below should be replaced by node logic to get userList*/
		// userList[0][0] = "david";
		// userList[0][1] = "192.168.1.44";
		// userList[1][0] = "joe";
		// userList[1][1] = "192.168.1.44";
		// userList[2][0] = "local_server";
		// userList[2][1] = "192.168.1.44";
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
		// mcMsg[0] = new OtpErlangAtom("joe");
		// mcMsg[1] = new OtpErlangAtom("joe@192.168.1.44"); //in reality the first two fields must be some node in the user list (instead of hard-coded in)
		// mcMsg[2] = new OtpErlangAtom("testingMC"); //specify the payload here
		// mcMsg[3] = new OtpErlangList(user_list);
		// OtpErlangTuple mcTuple = new OtpErlangTuple(mcMsg);
		try {
			System.out.println("Tuple as string before: " + withArgs(tuple).toString() + "\n");
			connection.sendRPC("message_passing", "unicastSend", withArgs(tuple));
			// System.out.println("Testing multicast...\n");
			// connection.sendRPC("message_passing", "multicastSend", withArgs(mcTuple));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// while(true) -- ADD THIS and make it a thread!
		// {
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
		System.out.println("Response received: " + response.toString() + "\n");
		// }
	}

	private OtpErlangObject[] withArgs(OtpErlangTuple tup) {
		System.out.println("Tuple as string: " + tup.toString() + "\n");
		return new OtpErlangObject[] { tup };
	}
	
	@Override
	public String toString() {
		return client + "<->" + server;
	}

}
