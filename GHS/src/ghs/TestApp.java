package ghs;

import java.util.ArrayList;
import java.util.Scanner;

import dao.GameRoomDAO;
import dao.MyDAOException;
import dao.PlayerDAO;
import databean.Card;
import databean.GameRoom;
import databean.MsgObj;
import databean.Player;

public class TestApp {

	static GameHallServer gh = new GameHallServer();
	static WordLibrary wl = new WordLibrary();
	
	public static void main(String[] args) throws MyDAOException {

		gh.initGhs();
		wl.initWordLib();
		
		while(true){
			int ret = -1;
			try {
				ret = test1();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(ret == 0){
				System.out.println("BYE!");
				break;
			}
		}
	}

	public static int test1() throws MyDAOException, InterruptedException{

		System.out.println("Game Hall");
		System.out.println("Enter :  1. Show existing rooms, 2. Create New Room 3. Join Existing 4. Exit");
		Scanner in = new Scanner(System.in);

		//gh.leaveRoom(3, 1, 3);
		
		int choice = in.nextInt();
		switch(choice){

		case 1:
			ArrayList<GameRoom> availableRooms = gh.showRooms();
			if(!(availableRooms.isEmpty())){
				for(GameRoom g: availableRooms){
					System.out.println("Room ID:"+g.getRoomId()+"  Room Name:"+g.getRoomName()+
							"  Creator Name: "+g.getCreatorName()+"  No. of Players: "+g.getNoPlayers()
							+"  Available Slots: "+g.getAvailableSlots());
				}
			}else{
				System.out.println("No Rooms");
			}
			System.out.println("____________________________________");
			break;

		case 2: 
			System.out.println("Enter Game room name:");
			String gameRoomName = in.next();
			System.out.println("Enter Player name:");
			String playerName = in.next();
			System.out.println("Enter IP:");
			String ip = in.next();
			System.out.println("Enter Port:");
			String port = in.next();
			MsgObj newObj = gh.createNewRoom(gameRoomName, playerName, ip, port);
			System.out.println("Your player ID is: "+newObj.getPlayerId());
			System.out.println("Your Game Room ID is: "+newObj.getRoomId());
			System.out.println("Your ClientNo is: "+newObj.getClientNo());
			System.out.println("____________________________________");

			playGame(newObj.getPlayerId(),newObj.getRoomId(),newObj.getClientNo());
			break;

		case 3: 
			System.out.println("Enter Game room ID:");
			int roomId = in.nextInt();
			System.out.println("Enter Player name:");
			playerName = in.next();
			System.out.println("Enter IP:");
			ip = in.next();
			System.out.println("Enter Port:");
			port = in.next();
			newObj = gh.joinRoom(roomId, playerName, ip, port);
			System.out.println("Your player ID is: "+newObj.getPlayerId());
			System.out.println("Your Client No is: "+newObj.getClientNo());
			System.out.println("\nOther Player details :");

			for(Player p: newObj.getAllPlayers()){
				System.out.println(p.getPlayerName()+" "+p.getIp()+" "+p.getPort()+" "+p.getClientNo());
			}
			System.out.println("____________________________________");

			//while(true){
				playGame(newObj.getPlayerId(),roomId,newObj.getClientNo());
			//}		
			//break;

		case 4: return 0;
				
		default: test1();
		}
		return 1;
	}

	public static void playGame(int playerId, int roomId, int clientNo) throws MyDAOException{
		
		System.out.println("Enter :  1. Start Game, 2. Leave GameRoom ");
		Scanner in = new Scanner(System.in);
		
		int choice = in.nextInt();
		
		switch(choice){
			
		case 1: chooseCard(playerId, roomId);
			break;
		case 2:
			gh.leaveRoom(playerId, roomId, clientNo);
			break;
		default : return;
		}
	}

	private static void chooseCard(int playerId, int roomId) throws MyDAOException {

		System.out.println("Choose genre: 1 -> Movies, 2 -> Words");
		Scanner in = new Scanner(System.in);
		//int choice = in.nextInt();
		String choice = in.next();
		Card card = wl.getCard(playerId, roomId, choice);
		
		System.out.println("1 Level:Easy  - "+card.getEasy());
		System.out.println("2 Level:Medium  - "+card.getMedium());
		System.out.println("3 Level:Hard  - "+card.getHard());
		
	}
}
