package us.sosia.video.stream.agent.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import us.sosia.video.stream.agent.messaging.MessageFactory;
import us.sosia.video.stream.agent.messaging.Messager;


public class GameWindow {
	protected final JFrame window;
	public static final int MAXVNUM = 4;
	
	private static JLabel timerLabel = new JLabel("Time Remaining");
	private static JTextField timerfield = new JTextField();
	
	private static JLabel scoreLabel = new JLabel("Scorecard:");
	private static JTextArea scorecard = new JTextArea(1, 100);
	
	private static JLabel answersLabel = new JLabel("Guessed Answers:");
	private static JTextArea wanswers = new JTextArea(1, 100);
	
	private static JLabel answerLabel = new JLabel("Answer");
	private static JTextField answerfield = new JTextField();

	public static VideoPanel[] videoPanelArray;
	
	private Messager messager = MessageFactory.getMessager("client", "local_server@128.237.231.0", "test");
	
	public GameWindow(String name,Dimension dimension) {
		super();
		videoPanelArray = new VideoPanel[MAXVNUM];

		this.window = new JFrame(name);
		this.window.setSize(dimension.width*4, dimension.height*3);	
		
		JPanel pane = new JPanel();
		pane.setLayout(null);
		Font font = new Font("Courier New" ,0, 11);
		 
		timerLabel.setFont(font);
		timerLabel.setBounds(600, 0, 100, 10);
		pane.add(timerLabel);
		
		JButton submit = new JButton("Submit");
		
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
		
		JButton submitbutton = new JButton("Submit");
		submitbutton.setBounds(600, 350, 100, 20);
		pane.add(submitbutton);

		submitbutton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				System.out.println("click button");
				messager.sendMsg("hello...click submit");
		   }
        });        

		/* video layout */
		for (int i=0;i<MAXVNUM;i++){
			videoPanelArray[i] = new VideoPanel();
			videoPanelArray[i].setPreferredSize(dimension);
		}
		videoPanelArray[1].setBounds(0, 0, dimension.width, dimension.height);
		videoPanelArray[2].setBounds(0, dimension.height, dimension.width, dimension.height);
		videoPanelArray[3].setBounds(0, 2*dimension.height, dimension.width, dimension.height);
		
//		videoPanelArray[4].setBounds(3*dimension.width, 0, dimension.width, dimension.height);
//		videoPanelArray[5].setBounds(3*dimension.width, dimension.height, dimension.width, dimension.height);
//		videoPanelArray[6].setBounds(3*dimension.width, 2*dimension.height, dimension.width, dimension.height);
//
//		videoPanelArray[0].setBounds((int)(1.5*dimension.width), 2*dimension.height, dimension.width, dimension.height);
		
		for (int i=0;i<MAXVNUM;i++){
			pane.add(videoPanelArray[i]);
		}
		
		this.window.setContentPane(pane);
		this.window.setVisible(true);
		this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void setVisible(boolean visible) {
		this.window.setVisible(visible);
	}
	
	public void close(){
		window.dispose();
		for(int i=0;i<MAXVNUM;i++){
			videoPanelArray[i].close();			
		}
	}
}
