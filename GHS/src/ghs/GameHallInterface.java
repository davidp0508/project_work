package ghs;

import messaging.*;

import java.util.ArrayList;
import java.util.Calendar;

import com.ericsson.otp.erlang.OtpErlangAtom;
import dao.MyDAOException;
import databean.Card;
import databean.GameRoom;
import databean.MsgObj;
import databean.Player;

public class GameHallInterface {

	static IPAddress ip = new IPAddress();
	private final static int				MAX_RETRIES 	= 3;
	private final static ArrayList<Integer> seqBuff			= new ArrayList<Integer>(); //keeps track of seen seqNos on Backup and
	private static long 					seqCtr			= 0; //counter for seqNo generation on Primary
	private final static String				myIp 			= ip.getIPaddress();
	private final static String				myName			= "game_hall"; // "backup" for bkup
	private final static ArrayList<Message>	receivedMsgs	= new ArrayList<Message>();
	private final static ArrayList<Message>	receivedAcks	= new ArrayList<Message>();
	private static Messager					sender_client;

	private final static String				replicaIp 		= "128.237.245.215";
	private final static String				replicaName		= "backup"; // "game_hall" for bkup
	private static boolean 					isPrimary 		= true; // 'false' for bkup

	private static GameHallServer gh = new GameHallServer();
	private static WordLibrary wl = new WordLibrary();

	public static void main(String[] args) throws MyDAOException {

		// Setup db connections
		gh.initGhs();
		wl.initWordLib();

		// build a messager to send msgs to other
		sender_client = MessageFactory.getMessager("client", "sender_server"+"GHS"+"@" + myIp, "test");
		System.out.println("StreamClient>>>"+sender_client);

		// listener thread start
		Listener myServer = new Listener(myName, myIp, receivedMsgs, receivedAcks);
		Thread threadServer = new Thread(myServer);
		threadServer.start();

		// loop to receive
		while(true){

			// check if new msgs have reached
			synchronized (receivedMsgs) {
				if (receivedMsgs.size() == 0)
					continue;
			}

			// get the new msg out
			Message rec = receivedMsgs.remove(0);
			seqCtr++;

			if(rec.getType() ==MSGTYPE.SHOW_ROOMS){

				String[] incomingArgs = rec.getContent().split(" ");
				String playerName = incomingArgs[0];

				String responseContent = new String();
				//db transaction
				ArrayList<GameRoom> availableRooms = gh.showRooms();

				if(!(availableRooms.isEmpty())){
					for(GameRoom g: availableRooms){
						/*
						 *  build response string containing info of all rooms, 
						 *  call send client
						 */
						responseContent += g.getRoomId()+","+g.getRoomName()+","+
								g.getCreatorName()+","+g.getNoPlayers()+" ";
					}
				}else{
					responseContent = "No_Room";
				}

				System.out.println(responseContent);
				sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.SHOW_ROOMS.toString()), 
						playerName+"@"+rec.getSrcIp(), rec.getSrcIp(), responseContent);
			}


			if(rec.getType() == MSGTYPE.CREATE_NEWROOM){

				// Parse content field of message to retrieve (gameRoomName, playerName, ip, port)
				String[] incomingArgs = rec.getContent().split(" ");

				String gameRoomName = incomingArgs[0];
				String playerName = incomingArgs[1];
				String ip = incomingArgs[2];
				String port = incomingArgs[3];
				String seqno = null;
				if(incomingArgs.length > 4)
					seqno = incomingArgs[4];
				
				if(!isPrimary){
					if(!seqBuff.contains(seqno)) //doesn't contain
					{
						seqBuff.add(Integer.parseInt(seqno));
						// database transaction - create player and room
						MsgObj newObj = gh.createNewRoom(gameRoomName, playerName, ip, port);
					}
					sendACKtoPrim(seqno);
				}
				else //if(isPrimary)
				{
					// database transaction - create player and room
					MsgObj newObj = gh.createNewRoom(gameRoomName, playerName, ip, port);

					propagate_state(rec.getType().toString(), rec.getContent(), seqCtr);

					/*respond to client
					 return player Id, client No and Room Id */

					String responseContent = new String(newObj.getPlayerId()+" "+newObj.getClientNo()+
							" "+newObj.getRoomId());

					sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.CREATE_NEWROOM.toString()), 
							playerName+"@"+rec.getSrcIp(), rec.getSrcIp(), responseContent);
				}
			}

			if(rec.getType() == MSGTYPE.JOIN_ROOM){

				// Parse content field of message to retrieve (roomId, playerName, ip, port)
				String[] incomingArgs = rec.getContent().split(" ");

				String roomId = incomingArgs[0];
				String playerName = incomingArgs[1];
				String ip = incomingArgs[2];
				String port = incomingArgs[3];
				String seqno = null;
				
				if(incomingArgs.length > 4)
					seqno = incomingArgs[4];

				if(!isPrimary){
					if(!seqBuff.contains(seqno)) //doesn't contain
					{
						seqBuff.add(Integer.parseInt(seqno));
						//database transaction				
						MsgObj newObj = gh.joinRoom(Integer.parseInt(roomId), playerName, ip, port);
					}
					sendACKtoPrim(seqno);
					
					
				}
				else //if(isPrimary){
				{
					//database transaction				
					MsgObj newObj = gh.joinRoom(Integer.parseInt(roomId), playerName, ip, port);
					
					propagate_state(rec.getType().toString(), rec.getContent(), seqCtr);

					/*respond to client */
					if(newObj != null){

						//return player Id and info of all players in room
						String responseContent = new String(newObj.getPlayerId()+" "+newObj.getClientNo()+" ");

						for(Player p: newObj.getAllPlayers()){
							responseContent += p.getPlayerName()+","+p.getIp()+","+p.getPort()+","+p.getClientNo()+" ";
						}
						sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.JOIN_ROOM.toString()), 
								playerName+"@"+rec.getSrcIp(), rec.getSrcIp(), responseContent);
					}else{
						String responseContent = new String("No_Room");
						sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.JOIN_ROOM.toString()), 
								playerName+"@"+rec.getSrcIp(), rec.getSrcIp(), responseContent);
					}
				}

			}

			if(rec.getType() == MSGTYPE.GET_CARD){
				
				if(isPrimary){
					
					// Parse content field of message to retrieve (playerId, roomId, genre)
					String[] incomingArgs = rec.getContent().split(" ");
					int playerId = Integer.parseInt(incomingArgs[0]);
					int roomId = Integer.parseInt(incomingArgs[1]);
					String genre = incomingArgs[2];
					String playerName = incomingArgs[3];
					
					//db transaction
					Card card = wl.getCard(playerId, roomId, genre);

					//return words belonging to the 3 difficulty levels
					String responseContent = new String(card.getEasy()+" "+card.getMedium()+" "+card.getHard());

					sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.GET_CARD.toString()), 
							playerName+"@"+rec.getSrcIp(), rec.getSrcIp(), responseContent);
					
					/* TO DO
					 * Propagate state of givenCards hashlist to prim backup
					 * 		send playerId, roomId, card ID
					 */
					propagate_state(rec.getType().toString(), playerId+" "+roomId+" "+card.getCardId(), seqCtr);

				}else{
					
					// if prim backup 
					String[] incomingArgs = rec.getContent().split(" ");
					int playerId = Integer.parseInt(incomingArgs[0]);
					int roomId = Integer.parseInt(incomingArgs[1]);
					int cardId = Integer.parseInt(incomingArgs[2]);
					String seqno = null;
					
					if(incomingArgs.length > 3)
						seqno = incomingArgs[3];
					if(!seqBuff.contains(seqno)) //doesn't contain
					{
						seqBuff.add(Integer.parseInt(seqno));
						wl.updateHashList(playerId, roomId, cardId);
					}
					sendACKtoPrim(seqno);
				}			
			}

			if(rec.getType() == MSGTYPE.LEAVE ){
			
				String[] incomingArgs = rec.getContent().split(" ");
				int playerId = Integer.parseInt(incomingArgs[0]);
				int  clientNo = Integer.parseInt(incomingArgs[1]);
				int roomId = Integer.parseInt(incomingArgs[2]);
				//String playerName = incomingArgs[3];
				String seqno = null;

				if(incomingArgs.length > 3)
					seqno = incomingArgs[3];

				if(!isPrimary){
					if(!seqBuff.contains(seqno)) //doesn't contain
					{
						seqBuff.add(Integer.parseInt(seqno));
						//database transaction				
						gh.leaveRoom(playerId, roomId, clientNo);
					}
					sendACKtoPrim(seqno);		
				}else{
					gh.leaveRoom(playerId, roomId, clientNo);
					propagate_state(rec.getType().toString(), rec.getContent(), seqCtr);;
				}
				
				

//				String responseContent = "";
//				if(res){
//					responseContent = new String("Success");
//				}else{
//					responseContent = new String("Leave failed");
//				}
//				sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.LEAVE.toString()), 
//						playerName+"@"+rec.getSrcIp(), rec.getSrcIp(), responseContent);	
			}


			if(rec.getType() == MSGTYPE.IS_PRIMARY){

				// Parse content field of message to retrieve (playerName)
				String[] incomingArgs = rec.getContent().split(" ");
				String playerName = incomingArgs[0];

				String responseContent = Boolean.toString(isPrimary); 
				sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.IS_PRIMARY.toString()), 
						playerName+"@"+rec.getSrcIp(), rec.getSrcIp(), responseContent);	
			}

			if(rec.getType() == MSGTYPE.NODE_DOWN){
				if(!isPrimary)//the backup
					isPrimary = true;
			}
		}
	}

	private static boolean propagate_state(String type, String content, long seqno){

		
		// TO DO		
		sender_client.unicastMsg(new OtpErlangAtom(type), 
				replicaName+"@"+replicaIp, replicaIp, content+" "+seqno); //forward to backup
		
		int retries = 0;
		long timeout = 1000;
		Calendar time = Calendar.getInstance(); 
		long startTime = time.getTimeInMillis();
		//wait for the corresponding ACK
		while(true){
			long curTime = time.getTimeInMillis();
			if(retries < MAX_RETRIES)
			{
				if(startTime+timeout <= curTime)
				{	
					//we timed out :(
					sender_client.unicastMsg(new OtpErlangAtom(type), 
							replicaName+"@"+replicaIp, replicaIp, content+" "+seqno); //forward to backup
					retries++;
					startTime = time.getTimeInMillis(); //reset timeout
				}
			}
			else //no retries left
			{
				if(startTime+timeout <= curTime) //and timed out
				{
					System.out.println("XXXXXXX  TIMEOUT __ RETRIES OVER __ BACK UP FAILURE XXX");
					//complete failure...if this happens: 
					//if backup fails for real and not just the ack getting back to the primary, then the backup
					//will need to query for missed messages (via querying the primary) upon reboot. This will
					//take care of this state inconsistency issue.
					//IF the ack just fails to be returned...there are three retries...if they all fail, then
					//the system is in a bad state in general. But, at this point there should be information
					//coming from your Erlang level telling you that there is a nodedown issue.
					return false; //but right now we don't check for this
				}
				//else we go into the code below
			}
			
			
			synchronized(receivedAcks) {
				if(receivedAcks.size() ==0)
					continue;
			}
			for(Message rec : receivedAcks)
			{
				String newSeqno = rec.getContent();
				if(Long.parseLong(newSeqno.trim()) == seqno)
				{
					receivedAcks.remove(rec); //this may fail due to iterator issues
					return true;
				}
			}
			return false;
		}
		
		// else resend & timeout 

	}

	private static void sendACKtoPrim(String seqno){
		// respond to primary with ACK
		String responseContent = seqno;
		sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.ACK.toString()), 
				replicaName+"@"+replicaIp, replicaIp, responseContent);
	}

}
