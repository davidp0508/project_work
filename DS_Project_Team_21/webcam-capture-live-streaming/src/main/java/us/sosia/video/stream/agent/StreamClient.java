package us.sosia.video.stream.agent;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.sosia.video.stream.agent.ui.GameWindow;
import us.sosia.video.stream.agent.ui.VideoPanel;
import us.sosia.video.stream.handler.StreamFrameListener;

public class StreamClient {

	/** Swing control */
	private final static Dimension	dimension		= new Dimension(320, 240);
	private final static GameWindow	displayWindow	= new GameWindow("Stream example", dimension);
	/** logger */
	protected final static Logger	logger			= LoggerFactory.getLogger(StreamClient.class);
	/** messaging */
	
	
	public static void main(String[] args) {
		// setup the videoWindow
		displayWindow.setVisible(true);

		// setup the connection
		logger.info("setup dimension :{}", dimension);

		for (int i = 0; i < GameWindow.MAXVNUM; i++) {
			StreamClientAgent clientAgent = new StreamClientAgent(new StreamFrameListenerIMPL(displayWindow.videoPanelArray[i]), dimension);
			clientAgent.connect(new InetSocketAddress("128.237.118.168", 20000));
		}
	}

	protected static class StreamFrameListenerIMPL implements StreamFrameListener {
		private volatile long		count	= 0;
		private volatile VideoPanel	v;

		StreamFrameListenerIMPL(VideoPanel video) {
			v = video;
		}

		public void onFrameReceived(BufferedImage image) {
//			logger.info("frame received :{}", count++);
			v.updateImage(image);
		}
	}
}
