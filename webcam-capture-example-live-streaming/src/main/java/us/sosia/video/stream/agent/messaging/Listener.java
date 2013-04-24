package us.sosia.video.stream.agent.messaging;

import java.io.IOException;
import java.util.ArrayList;

import us.sosia.video.stream.common.CharadesConfig;
import us.sosia.video.stream.common.User;

import com.ericsson.otp.erlang.*;

public class Listener implements Runnable {

	private String				selfName;
	private String				ip;
	private User[]				ipArray;
	private ArrayList<Message>	receivedMsgs;

	public Listener(String selfName, String ip, ArrayList<Message> receivedMsgs, User[] ipArray) {
		this.selfName = selfName;
		this.ip = ip;
		this.ipArray = ipArray;
		this.receivedMsgs = receivedMsgs;
	}

	@Override
	public void run() {
		OtpNode node = null;

		try {
			node = new OtpNode(selfName + "@" + ip, CharadesConfig.COOKIE);
		} catch (IOException e) {
			e.printStackTrace();
		}

		OtpMbox mbox = node.createMbox(selfName); // just to make it easier to follow the same format
		// OtpErlangAtom SHUTDOWN = new OtpErlangAtom("shutdown");
		System.out.println("Listener (" + node.toString() + ") started\n");

		while (true) {
			OtpErlangObject message = null;
			try {
				// System.out.println("Waiting to receive...\n");
				message = mbox.receive();
				// System.out.println("Got message "+message+"\n");
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
		}
	}
}