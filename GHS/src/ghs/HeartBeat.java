package ghs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import messaging.MSGTYPE;
import messaging.User;

import com.ericsson.otp.erlang.OtpErlangAtom;

public class HeartBeat {

	//heartbeat handling stuff
	private static int						curSec; //incoming seconds
	private static String					curPlayer;
	private static Thread					heartbeatThreadIn;
	private static int						HEARTBEAT_INTERVAL = 10;
	private static int 						MAX_TIMEOUT = 21;
	private static int						curSecOut; //outgoing seconds
	private static Thread					heartbeatThreadOut;
	static Map<String, Integer>	 			receivedBeats = new HashMap<String, Integer>();
	static volatile User[] 					fullIpArray	= null;	
	public static volatile ArrayList<User> 	fullIpList 	= new ArrayList<User>();

	public static void nodeDown(ArrayList<String> failedNodes)
	{
		int i;
		String fail_node_name = null;
		for(i=0; i<failedNodes.size(); i++)
		{
			fail_node_name = failedNodes.get(i);

			// You are backup and received node down from prim
			if(!GameHallInterface.isPrimary){ //the backup
				if(fail_node_name.equals(GameHallInterface.replicaName+"@"+GameHallInterface.replicaIp)){

					GameHallInterface.isPrimary = true;
					System.out.println("XX Detected Primary Failure. Now I am primary B-) ")		;
				}
			}else{
				System.out.println("XX Detected Back Up Failure")		;
			}
		}
	}

	private static class HeartBeatTimerIncoming implements Runnable {

		public HeartBeatTimerIncoming(int sec) {
			curSec = sec;
		}

		public void run() {

			ArrayList<String> failedNodes = new ArrayList<String>();

			while (curSec < MAX_TIMEOUT) 
			{
				curSec++;
				try {
					Thread.sleep(1000);
				} 
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			for (Map.Entry<String, Integer> entry : receivedBeats.entrySet()) {
				String key = entry.getKey();
				Integer value = entry.getValue();

				if(value != 1) //no heartbeat response
				{
					System.out.println("Timed out for "+key);
					failedNodes.add(key); //add node to list of failed nodes
					nodeDown(failedNodes); //call the nodeDown case stuff
				}
			}
			resetTimerIncoming();
		}

		public void resetTimerIncoming()
		{
		
			for(String key : receivedBeats.keySet()){
				receivedBeats.put(key, 0);
			}
			startHeartBeatTimerIncoming();
		}

	}

	public static void startHeartBeatTimerIncoming() {
		//int i;
		/*TODO WE NEED TO have the GHS send heartbeats to the current 
		 * actor (and then forget them in next round) for each room.
		 * This will allow us to determine when to send the IS_PRIMARY
		 * message. */
		HeartBeatTimerIncoming hbtimer = new HeartBeatTimerIncoming(0);
		heartbeatThreadIn = new Thread(hbtimer);
		heartbeatThreadIn.start();

	}

	public static void sendOutgoingHeartbeat() {
		//Send heartbeat messages every 10 seconds via multicast
		//REQUIRES correct name in CharadesConfig.MY_NAME slot!

		fullIpArray = convertListToArray();
		GameHallInterface.sender_client.multicastMsg(new OtpErlangAtom(MSGTYPE.HEARTBEAT.toString()),fullIpArray, GameHallInterface.myName+"@"+GameHallInterface.myIp);
		//		OutgoingHeartBeatTimer hbtimerOut = new OutgoingHeartBeatTimer(curSecOut);
		//		heartbeatThreadOut = new Thread(hbtimerOut);
		//		heartbeatThreadOut.start();
	}

	private static User[] convertListToArray() {
		// TODO Auto-generated method stub
		User[] tempArray = new User[fullIpList.size()];
		int index = 0;
		for(User temp : fullIpList){
			tempArray[index] = temp;
			index++;
		}
		return tempArray;
	}



	private static class OutgoingHeartBeatTimer implements Runnable {

		public OutgoingHeartBeatTimer(int sec) {
			curSecOut = sec;
		}

		public void run() {
			while (curSecOut < HEARTBEAT_INTERVAL) 
			{
				curSecOut++;
				try {
					Thread.sleep(1000);
				} 
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sendOutgoingHeartbeat();
			System.out.println(HEARTBEAT_INTERVAL+" seconds elapsed since last sent heartbeat; sent again ");
			resetTimer();//need to reset the seconds timer
		}

		public void resetTimer()
		{
			startHeartBeatTimerOutgoing();
		}
	}

	public static void startHeartBeatTimerOutgoing() {
		OutgoingHeartBeatTimer hbtimerOut = new OutgoingHeartBeatTimer(0);
		heartbeatThreadOut = new Thread(hbtimerOut);
		heartbeatThreadOut.start();
	}

}
