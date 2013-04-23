package us.sosia.video.stream.agent;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.otp.erlang.OtpErlangAtom;

import us.sosia.video.stream.agent.messaging.Listener;
import us.sosia.video.stream.agent.messaging.MSGTYPE;
import us.sosia.video.stream.agent.messaging.Message;
import us.sosia.video.stream.agent.messaging.MessageFactory;
import us.sosia.video.stream.agent.messaging.Messager;
import us.sosia.video.stream.agent.messaging.User;
import us.sosia.video.stream.agent.ui.GameWindow;
import us.sosia.video.stream.agent.ui.VideoPanel;
import us.sosia.video.stream.common.CharadesConfig;
import us.sosia.video.stream.handler.StreamFrameListener;

public class StreamClient {

	// currently I tested all the client in my own machine
	private final static String				testip			= CharadesConfig.TEST_IP;
	//game constants
	private final static int num_players = CharadesConfig.NUM_PLAYERS;
	private final static int round_time = CharadesConfig.MAXSEC;
	// TODO contains all the IP, Name and port number of all players
	private static User[]				userList = new User[num_players];

	// video window size
	private final static Dimension			dimension		= new Dimension(320, 240);
	// main application window
	private static GameWindow				displayWindow;
	protected final static Logger			logger			= LoggerFactory.getLogger(StreamClient.class);

	// Just like what we did in lab. Listener thread receive msgs and deliver them to the arraylist
	// another thread loop and read these msgs
	private final static ArrayList<Message>	receivedMsgs	= new ArrayList<Message>();
	private final static String				correct_answer	= "initial_demo";//TODO fetch from GHS?

	// TBD: score algorithm here
	private static Integer[]				scores			= new Integer[num_players];

	// variables for timer
	private static Integer					timeout			= 0;
	private static int						currentsec;
	private static boolean					stop;

	private static Messager					sender_client;
	private static int						state;

	// client ID: each client is assigned a client No to identify.
	private static int						playerId;
	private static int						actorId			= 0;

	public static void main(String[] args) {
		int winner;

		// build a messager to send msgs to other
		sender_client = MessageFactory.getMessager("client", CharadesConfig.SENDER_PREFIX + args[0] + "@" + testip, CharadesConfig.COOKIE);
		System.out.println("StreamClient>>>"+sender_client);

		playerId = Integer.parseInt(args[0]);

		// three test case TODO get from GHS
		userList[0] = new User(CharadesConfig.myName, testip, 20000);
		userList[1] = new User(CharadesConfig.yourName, testip, 20001);//TODO get from GHS
//		userList[2] = new User("hyang", testip, 20002);

		//user list...
		
		try {
			displayWindow = new GameWindow(dimension, testip, playerId, userList);
		} catch (IOException e) {
			e.printStackTrace();
		}

		scores[0] = 0;
		scores[1] = 0;
		// scores[2] = 0;

		// setup the videoWindow
		displayWindow.ipArray = userList;
		displayWindow.myHostId = playerId;
		displayWindow.setVisible(true);
		displayWindow.scorecard.setText(getScoreStr());

		// setup the connection
//		logger.info("setup dimension :{}", dimension);

		// make every video window connected
		for (int i = 0; i < userList.length; i++) {
			StreamClientAgent clientAgent = new StreamClientAgent(new StreamFrameListenerIMPL(displayWindow.videoPanelArray[i]), dimension);
			logger.info(userList[i].hostname + " connecting video server");
			clientAgent.connect(new InetSocketAddress(userList[i].ip, CharadesConfig.VIDEO_PORT));
		}

		// timer start
		Timer timer = new Timer(round_time);
		Thread t = new Thread(timer);
		t.start();

		// player with clientNo 0 will be the first actor
		state = CharadesConfig.GUESSING;
		displayWindow.submitbutton.setEnabled(true);
		if (playerId == 0) {
			//TODO move such messages into config
			int option = JOptionPane.showConfirmDialog(null, userList[playerId].hostname + " Do you want to be the actor?", "Confirm",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (option == 0) {
				change2ACTING();
			} else {
				change2GUESSING();
			}
		}

		// listener thread start
		logger.info("start listener of " + userList[playerId].hostname);
		Listener myServer = new Listener(userList[playerId].hostname, testip, receivedMsgs, userList);
		Thread threadServer = new Thread(myServer);
		threadServer.start();

		// loop and check
		while (true) {
			// check if new msgs have reached
			synchronized (receivedMsgs) {
				if (receivedMsgs.size() == 0)
					continue;
			}

			// get the new msg out
			Message rec = receivedMsgs.remove(0);
			System.out.println(rec.type + "#");

			// if time out happens
			synchronized (timeout) {
				if (timeout == 1) {
					System.out.println("really timeout!!!!!");//TODO move to config
					if (state == CharadesConfig.ACTING) {
						change2GUESSING();
						t.start();
					}
				}
			}
			
			//TODO swich case
			// begin -> start timer
			if (rec.type.equals("BEGIN")) {
				stop = false;
			}

			// round finished -> reset timer
			if (rec.type.equals("ROUND_FINISHED")) {
				currentsec = round_time;
				stop = true;
			}

			if (rec.type.equals("ANSWER")) {
				// if I am a actor I need to update the score card
				if (state == CharadesConfig.ACTING) {
					String reved_answer = rec.content.substring(rec.content.indexOf(' ') + 1);
					System.out.println("received " + reved_answer);
					// if the answer is correct, this round of game ends
					if (reved_answer.equals(correct_answer)) {
						String str_winner = rec.name;
						winner = getIndexFromName(str_winner);
						scores[playerId]++;
						scores[winner]++;
						String str_scores = getScoreStr();
						displayWindow.scorecard.setText(str_scores);
						sender_client.multicast(playerId, "SCOREUPDATE " + compressScores(), userList);

						change2GUESSING();
						continue;
					}
				}
				String text = displayWindow.wanswers.getText() + rec.content + "\n";
				displayWindow.wanswers.setText(text);
			}

			// received new scores
			if (rec.type.equals("SCOREUPDATE")) {
				String[] rec_scores = rec.content.split(" ");
				for (int i = 0; i < num_players; i++) {
					scores[i] = Integer.parseInt(rec_scores[i]);
				}
				displayWindow.scorecard.setText(getScoreStr());
				continue;
			}

			if (rec.type.equals("ROUND_FINISHED")) {
				// clear all textfield in swing window
				displayWindow.wanswers.setText("");
				displayWindow.changeActor((++actorId) % 3, dimension.height);
				currentsec = round_time;
				stop = true;
				continue;
			}

			// pass the token to next player
			if (rec.type.equals("TOKEN")) {
				// user can choose whether they want to be an actor
				int option = JOptionPane.showConfirmDialog(null, userList[playerId].hostname + " Do you want to be the actor?", "Confirm",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (option == 0) {
					change2ACTING();
				} else {
					change2GUESSING();
				}
			}
		}
	}

	private static void change2ACTING() {
		state = CharadesConfig.ACTING;
		displayWindow.submitbutton.setEnabled(false);
		sender_client.multicastMsg(new OtpErlangAtom(MSGTYPE.BEGIN), userList, null);
//		sender_client.multicast(playerId, "BEGIN ", userList);
		stop = false;
	}

	private static void change2GUESSING() {
		state = CharadesConfig.GUESSING;
		displayWindow.submitbutton.setEnabled(true);
		sender_client.multicast(playerId, "ROUND_FINISHED " + " ", userList);//TODO
		displayWindow.changeActor((++actorId) % 3, dimension.height);
		User next = userList[(playerId + 1) % num_players];
		sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.TOKEN), next.otpString, next.ip, null);
//		sender_client.sendMsg("TOKEN " + " ", userList[(playerId + 1) % num_players].hostname, userList[(playerId + 1) % num_players].ip);
		currentsec = round_time;
		stop = true;

	}

	private static class Timer implements Runnable {
		public Timer(int sec) {
			currentsec = sec;
			stop = true;
		}

		public void run() {
			while (currentsec >= 0) {
				int min = currentsec / 60;
				Integer sec = currentsec % 60;
				String strsec = sec.toString();
				if (sec < 10) {
					strsec = "0" + sec.toString();
				}

				displayWindow.timerfield.setText("        " + min + ":" + strsec);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (stop == true)
					continue;
				currentsec--;
			}
			synchronized (timeout) {
				timeout = 1;
				System.out.println("timeout !!!!!!!!");
			}
		}
	}

	protected static class StreamFrameListenerIMPL implements StreamFrameListener {
		private volatile long		count	= 0;
		private volatile VideoPanel	v;

		StreamFrameListenerIMPL(VideoPanel video) {
			v = video;
		}

		public void onFrameReceived(BufferedImage image) {
			// logger.info("frame received :{}",count++);
			v.updateImage(image);
		}
	}

	private static int getIndexFromName(String name) {
		for (int i = 0; i < userList.length; i++)
			if (userList[i].hostname.equals(name))
				return i;
		return -1;
	}

	private static String getScoreStr() {
		String str_scores = "";
		for (int i = 0; i < scores.length; i++) {
			str_scores += userList[i].hostname + " " + scores[i].toString() + "\n";
		}
		return str_scores;
	}

	private static String compressScores() {
		String str_scores = "";
		for (int i = 0; i < scores.length; i++) {
			str_scores += scores[i].toString() + " ";
		}
		return str_scores;
	}
}