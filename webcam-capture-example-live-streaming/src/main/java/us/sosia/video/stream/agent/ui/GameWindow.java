package us.sosia.video.stream.agent.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpPeer;
import com.ericsson.otp.erlang.OtpSelf;

import us.sosia.video.stream.agent.messaging.MSGTYPE;
import us.sosia.video.stream.agent.messaging.MessageFactory;
import us.sosia.video.stream.agent.messaging.Messager;
import us.sosia.video.stream.agent.messaging.NameIpPort;


public class GameWindow {
	/** UI stuff */
	protected final JFrame window;
	public final int MAXVNUM = 2; //TODO make sure configured same as NUM_PLAYERS

	private JLabel timerLabel = new JLabel("Time Remaining");
	public JTextField timerfield = new JTextField();

	private JLabel scoreLabel = new JLabel("Scorecard:");
	public JTextArea scorecard = new JTextArea(1, 100);

	private JLabel answersLabel = new JLabel("Guessed Answers:");
	public JTextArea wanswers = new JTextArea(1, 200);

	private JLabel answerLabel = new JLabel("Answer");
	public JTextField answerfield = new JTextField();

	public JLabel actorLabel = new JLabel("Actor: ");

	public JButton submitbutton = new JButton("Submit");	
	
	public VideoPanel[] videoPanelArray;
	
	/** messaging stuff */
	public NameIpPort[] ipArray;
	public Integer myHostId;
	private static final String	COOKIE	= "test";
	private Messager messager;
	//TODO
	private final OtpSelf selfNode;
	
	public GameWindow(Dimension dimension, String testip, int hostId, final NameIpPort[] ipArr) throws IOException {
		super();
		/** messaging */
		selfNode = new OtpSelf("dmei", COOKIE);
		this.myHostId = hostId;
		ipArray = ipArr;
		messager = MessageFactory.getMessager("client", "sender_server" + this.myHostId.toString() + "@" + testip, COOKIE);
	
		/** UI */
		videoPanelArray = new VideoPanel[MAXVNUM];
		JLabel[] labelArray = new JLabel[MAXVNUM];
		
		this.window = new JFrame("Act Something " + ipArray[myHostId].hostname);
		this.window.setSize(dimension.width*4, dimension.height*3 + 60);	

		JPanel pane = new JPanel();
		pane.setLayout(null);
		Font font = new Font("Courier New" ,0, 11);

		timerLabel.setFont(font);
		timerLabel.setBounds(600, 0, 100, 10);
		pane.add(timerLabel);

		timerfield.setEditable(false);
		timerfield.setFont(font);
		timerfield.setBounds(600, 30, 100, 20);
		pane.add(timerfield);		

		scoreLabel.setFont(font);
		scoreLabel.setBounds(320, 100, 100, 10);
		pane.add(scoreLabel);

		scorecard.setFont(font);
		scorecard.setBounds(320, 115, 200, 200);
		pane.add(scorecard);

		answersLabel.setFont(font);
		answersLabel.setBounds(760, 100, 100, 10);
		pane.add(answersLabel);

		wanswers.setFont(font);
		wanswers.setBounds(760, 115, 200, 200);
		pane.add(wanswers);

		answerLabel.setFont(font);
		answerLabel.setBounds(600, 300, 100, 10);
		pane.add(answerLabel);

		answerfield.setFont(font);
		answerfield.setBounds(600, 320, 100, 20);
		pane.add(answerfield);

		submitbutton.setBounds(600, 350, 100, 20);
		pane.add(submitbutton);

		submitbutton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(answerfield.getText().isEmpty())
					return;
				//TODO multicast
				for(int i=0;i<ipArray.length;i++){
					if(i!=myHostId) {
						messager.sendMsg(selfNode, new OtpErlangAtom(MSGTYPE.ANSWER), new OtpPeer(ipArray[myHostId].hostname), messager.getMyIp(), ipArray[i].ip);
//					    messager.sendMsg("ANSWER " + ipArray[myHostId].hostname  + " " + answerfield.getText(), ipArray[i].hostname, ipArray[i].ip);
					}
				}
				wanswers.setText(wanswers.getText() + ipArray[myHostId].hostname + " " + answerfield.getText() + "\n");
		   }
        });

		/* video layout */
		for (int i=0;i<MAXVNUM;i++){
			videoPanelArray[i] = new VideoPanel();
			videoPanelArray[i].setPreferredSize(dimension);
			labelArray[i] = new JLabel(ipArray[i].hostname);
		}
		labelArray[0].setBounds(120, 0 , 100, 20);
		videoPanelArray[0].setBounds(0, 20, dimension.width, dimension.height);
		labelArray[1].setBounds(120, 20 + dimension.height , 100, 20);
		videoPanelArray[1].setBounds(0, 40 + dimension.height, dimension.width, dimension.height);
		//TODO auto layout according to num_players
//		labelArray[2].setBounds(120, 40 + 2*dimension.height , 100, 20);
//		videoPanelArray[2].setBounds(0, 60 + 2*dimension.height, dimension.width, dimension.height);

		actorLabel.setBounds(10, 0 , 100, 20);
		pane.add(actorLabel);

//		videoPanelArray[4].setBounds(3*dimension.width, 0, dimension.width, dimension.height);
//		videoPanelArray[5].setBounds(3*dimension.width, dimension.height, dimension.width, dimension.height);
//		videoPanelArray[6].setBounds(3*dimension.width, 2*dimension.height, dimension.width, dimension.height);
//
//		videoPanelArray[0].setBounds((int)(1.5*dimension.width), 2*dimension.height, dimension.width, dimension.height);

		for (int i=0;i<MAXVNUM;i++){
			pane.add(videoPanelArray[i]);
			pane.add(labelArray[i]);
		}

		this.window.setContentPane(pane);
		this.window.setVisible(true);
		this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void setVisible(boolean visible) {
		this.window.setVisible(visible);
	}

	public void changeActor(int i, int height){
		switch(i){
		case 0:
			actorLabel.setBounds(10, 0 , 100, 20);
			break;
		case 1:
			actorLabel.setBounds(10, 20 + height , 100, 20);
			break;
		case 2:
			actorLabel.setBounds(10, 40 + 2*height , 100, 20);
			break;
		}
	}

	public void close(){
		window.dispose();
		for(int i=0;i<MAXVNUM;i++){
			videoPanelArray[i].close();			
		}
	}
}