package ghs;

import messaging.*;

import java.util.ArrayList;
import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpPeer;

import dao.MyDAOException;
import databean.Card;
import databean.GameRoom;
import databean.MsgObj;
import databean.Player;

public class GameHallInterface {

	private final static String				myIp			= "128.237.229.87";
	private final static ArrayList<Message>	receivedMsgs	= new ArrayList<Message>();
	private static Messager					sender_client;

	private static GameHallServer gh = new GameHallServer();
	private static WordLibrary wl = new WordLibrary();

	public static void main(String[] args) throws MyDAOException {

		// Setup db connections
		gh.initGhs();
		wl.initWordLib();

		// build a messager to send msgs to other
		sender_client = MessageFactory.getMessager("client", "sender_server" + args[0] + "@" + myIp, "test");
		System.out.println("StreamClient>>>"+sender_client);

		// listener thread start
		Listener myServer = new Listener("game_hall", myIp, receivedMsgs);
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

			System.out.println(rec.type + "#");

			if(rec.type.equals(MSGTYPE.SHOW_ROOMS)){

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
					responseContent = "No_Rooms";
				}
				sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.SHOW_ROOMS), 
						new OtpPeer(rec.name+"@"+rec.ip), rec.ip, responseContent);
			}


			if(rec.type.equals(MSGTYPE.CREATE_NEWROOM)){

				// Parse content field of message to retrieve (gameRoomName, playerName, ip, port)
				String[] incomingArgs = rec.type.split(" ");

				String gameRoomName = incomingArgs[0];
				String playerName = incomingArgs[1];
				String ip = incomingArgs[2];
				String port = incomingArgs[3];

				// database transaction - create player and room
				MsgObj newObj = gh.createNewRoom(gameRoomName, playerName, ip, port);

				// return player Id, client No and Room Id
				String responseContent = new String(newObj.getPlayerId()+" "+newObj.getClientNo()+
						" "+newObj.getRoomId());

				sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.CREATE_NEWROOM), 
						new OtpPeer(rec.name+"@"+rec.ip), rec.ip, responseContent);

			}

			if(rec.type.equals(MSGTYPE.JOIN_ROOM)){

				// Parse content field of message to retrieve (roomId, playerName, ip, port)
				String[] incomingArgs = rec.type.split(" ");

				String roomId = incomingArgs[0];
				String playerName = incomingArgs[1];
				String ip = incomingArgs[2];
				String port = incomingArgs[3];

				//database transaction
				MsgObj newObj = gh.joinRoom(Integer.parseInt(roomId), playerName, ip, port);

				//return player Id and info of all players in room
				String responseContent = new String(newObj.getPlayerId()+" "+newObj.getClientNo()+" ");

				for(Player p: newObj.getAllPlayers()){
					responseContent += p.getPlayerName()+","+p.getIp()+","+p.getPort()+","+p.getClientNo()+" ";
				}
				sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.JOIN_ROOM), 
						new OtpPeer(rec.name+"@"+rec.ip), rec.ip, responseContent);

			}

			if(rec.type.equals(MSGTYPE.GET_CARD)){

				// Parse content field of message to retrieve (playerId, roomId, genre)
				String[] incomingArgs = rec.type.split(" ");
				int playerId = Integer.parseInt(incomingArgs[0]);
				int roomId = Integer.parseInt(incomingArgs[1]);
				String genre = incomingArgs[2];

				//db transaction
				Card card = wl.getCard(playerId, roomId, genre);

				//return words belonging to the 3 difficulty levels
				String responseContent = new String(card.getEasy()+" "+card.getMedium()+" "+card.getHard());

				sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.GET_CARD), 
						new OtpPeer(rec.name+"@"+rec.ip), rec.ip, responseContent);			
			}

			if(rec.type.equals(MSGTYPE.LEAVE)){
				/*
				 * To do: see TestApp
				 * 		 also, handle sudden abort
				 */
				String[] incomingArgs = rec.type.split(" ");
				int playerId = Integer.parseInt(incomingArgs[0]);
				int  clientNo = Integer.parseInt(incomingArgs[1]);
				int roomId = Integer.parseInt(incomingArgs[2]);
				
				boolean res = gh.leaveRoom(playerId, roomId, clientNo);
				
				String responseContent = "";
				if(res){
					responseContent = new String("Success");
				}else{
					responseContent = new String("Leave failed");
				}
				sender_client.unicastMsg(new OtpErlangAtom(MSGTYPE.LEAVE), 
						new OtpPeer(rec.name+"@"+rec.ip), rec.ip, responseContent);	
			}


		}
	}

}
