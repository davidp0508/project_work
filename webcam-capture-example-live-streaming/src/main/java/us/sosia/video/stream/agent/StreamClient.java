package us.sosia.video.stream.agent;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.sosia.video.stream.agent.messaging.Listener;
import us.sosia.video.stream.agent.messaging.Message;
import us.sosia.video.stream.agent.messaging.MessageFactory;
import us.sosia.video.stream.agent.messaging.Messager;
import us.sosia.video.stream.agent.messaging.NameIpPort;
import us.sosia.video.stream.agent.ui.GameWindow;
import us.sosia.video.stream.agent.ui.VideoPanel;
import us.sosia.video.stream.common.CharadesConfig;
import us.sosia.video.stream.handler.StreamFrameListener;

public class StreamClient {

	// currently I tested all the client in my own machine
	private final static String				testip			= "128.237.118.211";
	private final static String myName = "dmei";
	//game constants
	private final static int num_players = CharadesConfig.NUM_PLAYERS;
	private final static int round_time = CharadesConfig.MAXSEC;
	// contains all the IP, Name and port number of all players
	private static NameIpPort[]				ipArray			= new NameIpPort[num_players];

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

	private static Messager					messager;
	private static int						state;

	// client ID: each client is assigned a client No to identify.
	private static int						playerId;
	private static int						actorId			= 0;

	public static void main(String[] args) {
		int winner;

		// build a messager to send msgs to other
		messager = MessageFactory.getMessager("client" + args[0], CharadesConfig.SENDER_PREFIX + args[0] + "@" + testip, "test");

		playerId = Integer.parseInt(args[0]);

		// three test case
		ipArray[0] = new NameIpPort(myName, testip, 20000);
		ipArray[1] = new NameIpPort("hanyang", testip, 20001);
		// ipArray[2] = new NameIpPort("hyang", testip, 20002);

		try {
			displayWindow = new GameWindow(dimension, testip, playerId, ipArray);
		} catch (IOException e) {
			e.printStackTrace();
		}

		scores[0] = 0;
		scores[1] = 0;
		// scores[2] = 0;

		// setup the videoWindow
		displayWindow.ipArray = ipArray;
		displayWindow.myHostId = playerId;
		displayWindow.setVisible(true);
		displayWindow.scorecard.setText(getScoreStr());

		// setup the connection
		logger.info("setup dimension :{}", dimension);

		// make every video window connected
		for (int i = 0; i < ipArray.length; i++) {
			StreamClientAgent clientAgent = new StreamClientAgent(new StreamFrameListenerIMPL(displayWindow.videoPanelArray[i]), dimension);
			clientAgent.connect(new InetSocketAddress(ipArray[i].ip, 20000));
		}

		// timer start
		Timer timer = new Timer(CharadesConfig.MAXSEC);
		Thread t = new Thread(timer);
		t.start();

		// player with clientNo 0 will be the first actor
		state = CharadesConfig.GUESSING;
		displayWindow.submitbutton.setEnabled(true);
		if (playerId == 0) {
			//TODO move such messages into config
			int option = JOptionPane.showConfirmDialog(null, ipArray[playerId].hostname + " Do you want to be the actor?", "Confirm",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (option == 0) {
				change2ACTING();
			} else {
				change2GUESSING();
			}
		}

		// listener thread start
		logger.info("start listener of " + ipArray[playerId].hostname);
		Listener myServer = new Listener(ipArray[playerId].hostname, testip, receivedMsgs, ipArray);
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
						messager.multicast(playerId, "SCOREUPDATE " + compressScores(), ipArray);

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
				int option = JOptionPane.showConfirmDialog(null, ipArray[playerId].hostname + " Do you want to be the actor?", "Confirm",
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
		messager.multicast(playerId, "BEGIN ", ipArray);// TODO change to real multicast
		stop = false;
	}

	private static void change2GUESSING() {
		state = CharadesConfig.GUESSING;
		displayWindow.submitbutton.setEnabled(true);
		messager.multicast(playerId, "ROUND_FINISHED " + " ", ipArray);
		displayWindow.changeActor((++actorId) % 3, dimension.height);
		messager.sendMsg("TOKEN " + " ", ipArray[(playerId + 1) % num_players].hostname, ipArray[(playerId + 1) % num_players].ip);
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
		for (int i = 0; i < ipArray.length; i++)
			if (ipArray[i].hostname.equals(name))
				return i;
		return -1;
	}

	private static String getScoreStr() {
		String str_scores = "";
		for (int i = 0; i < scores.length; i++) {
			str_scores += ipArray[i].hostname + " " + scores[i].toString() + "\n";
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