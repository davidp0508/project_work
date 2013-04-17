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
import us.sosia.video.stream.handler.StreamFrameListener;

public class StreamClient {

	// currently I tested all the client in my own machine
	private final static String				testip			= "128.237.118.211";
	// number of players decide the number of video window
	private final static int				NUM_PLAYERS		= 2;
	// seconds for a round of game.
	private final static int				MAXSEC			= 120;
	// contains all the IP, Name and port number of all players
	private static NameIpPort[]				ipArray			= new NameIpPort[NUM_PLAYERS];

	// video window size
	private final static Dimension			dimension		= new Dimension(320, 240);
	// main application window
	private static GameWindow				displayWindow;
	protected final static Logger			logger			= LoggerFactory.getLogger(StreamClient.class);

	// Just like what we did in lab. Listener thread receive msgs and deliver them to the arraylist
	// another thread loop and read these msgs
	private final static ArrayList<Message>	receivedMsgs	= new ArrayList<Message>();
	private final static String				correct_answer	= "initial_demo";

	// TBD: score algorithm here
	private static Integer[]				scores			= new Integer[NUM_PLAYERS];

	// variables for timer
	private static Integer					timeout			= 0;
	private static int						currentsec;
	private static boolean					stop;

	private static Messager					messager;
	private static int						state;
	// TODO any other states? if yes, use enum
	private final static int				ACTING			= 1;
	private final static int				GUESSING		= 2;

	// client ID: each client is assigned a client No to identify.
	private static int						clientId;
	private static int						actorId			= 0;

	public static void main(String[] args) {
		int winner;

		// build a messager to send msgs to other
		messager = MessageFactory.getMessager("client" + args[0], "sender_server" + args[0] + "@" + testip, "test");

		clientId = Integer.parseInt(args[0]);

		// three test case
		ipArray[0] = new NameIpPort("dmei", testip, 20000);
		ipArray[1] = new NameIpPort("hanyang", testip, 20001);
		// ipArray[2] = new NameIpPort("hyang", testip, 20002);

		try {
			displayWindow = new GameWindow(dimension, testip, clientId, ipArray);
		} catch (IOException e) {
			e.printStackTrace();
		}

		scores[0] = 0;
		scores[1] = 0;
		// scores[2] = 0;

		// setup the videoWindow
		displayWindow.ipArray = ipArray;
		displayWindow.myHostId = clientId;
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
		Timer timer = new Timer(MAXSEC);
		Thread t = new Thread(timer);
		t.start();

		// player with clientNo 0 will be the first actor
		state = GUESSING;
		displayWindow.submitbutton.setEnabled(true);
		if (clientId == 0) {
			int option = JOptionPane.showConfirmDialog(null, ipArray[clientId].hostname + " Do you want to be the actor?", "Confirm",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (option == 0) {
				change2ACTING();
			} else {
				change2GUESSING();
			}
		}

		// listener thread start
		Listener myServer = new Listener(ipArray[clientId].hostname, testip, receivedMsgs, ipArray);
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
					System.out.println("really timeout!!!!!");
					if (state == ACTING) {
						change2GUESSING();
						t.start();
					}
				}
			}

			// begin -> start timer
			if (rec.type.equals("BEGIN")) {
				stop = false;
			}

			// round finished -> reset timer
			if (rec.type.equals("ROUND_FINISHED")) {
				currentsec = MAXSEC;
				stop = true;
			}

			if (rec.type.equals("ANSWER")) {
				// if I am a actor I need to update the score card
				if (state == ACTING) {
					String reved_answer = rec.content.substring(rec.content.indexOf(' ') + 1);
					System.out.println("received " + reved_answer);
					// if the answer is correct, this round of game ends
					if (reved_answer.equals(correct_answer)) {
						String str_winner = rec.name;
						winner = getIndexFromName(str_winner);
						scores[clientId]++;
						scores[winner]++;
						String str_scores = getScoreStr();
						displayWindow.scorecard.setText(str_scores);
						messager.multicast(clientId, "SCOREUPDATE " + compressScores(), ipArray);

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
				for (int i = 0; i < NUM_PLAYERS; i++) {
					scores[i] = Integer.parseInt(rec_scores[i]);
				}
				displayWindow.scorecard.setText(getScoreStr());
				continue;
			}

			if (rec.type.equals("ROUND_FINISHED")) {
				// clear all textfield in swing window
				displayWindow.wanswers.setText("");
				displayWindow.changeActor((++actorId) % 3, dimension.height);
				currentsec = MAXSEC;
				stop = true;
				continue;
			}

			// pass the token to next player
			if (rec.type.equals("TOKEN")) {
				// user can choose whether they want to be an actor
				int option = JOptionPane.showConfirmDialog(null, ipArray[clientId].hostname + " Do you want to be the actor?", "Confirm",
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
		state = ACTING;
		displayWindow.submitbutton.setEnabled(false);
		messager.multicast(clientId, "BEGIN ", ipArray);// TODO change to real multicast
		stop = false;
	}

	private static void change2GUESSING() {
		state = GUESSING;
		displayWindow.submitbutton.setEnabled(true);
		messager.multicast(clientId, "ROUND_FINISHED " + " ", ipArray);
		displayWindow.changeActor((++actorId) % 3, dimension.height);
		messager.sendMsg("TOKEN " + " ", ipArray[(clientId + 1) % NUM_PLAYERS].hostname, ipArray[(clientId + 1) % NUM_PLAYERS].ip);
		currentsec = MAXSEC;
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