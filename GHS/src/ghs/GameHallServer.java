package ghs;

import java.util.ArrayList;

import dao.CardDAO;
import dao.GameRoomDAO;
import dao.GenreDAO;
import dao.MyDAOException;
import dao.PlayerDAO;
import databean.GameRoom;
import databean.MsgObj;
import databean.Player;

public class GameHallServer {

	static PlayerDAO playerDao;
	static GameRoomDAO roomDao;

	static String jdbcDriverName;
	static String jdbcURL;

	/*
	 * Set up mysql connection
	 */
	public void initGhs(){

		String jdbcDriverName = "com.mysql.jdbc.Driver";
		String jdbcURL        =  "jdbc:mysql:///ghs";

		try {
			playerDao = new PlayerDAO( jdbcDriverName, jdbcURL, "players");
			roomDao = new GameRoomDAO(jdbcDriverName, jdbcURL, "gameRooms");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 *  Show all existing rooms with vacant slots
	 */
	public ArrayList<GameRoom> showRooms() throws MyDAOException{

		ArrayList<GameRoom> allRooms = roomDao.showAllRooms();
		ArrayList<GameRoom> availableRooms = new ArrayList<GameRoom>();

		if(allRooms != null){
			for(GameRoom gr : allRooms){
				if( gr.getNoPlayers() < 6){
					availableRooms.add(gr);
				}
			}
		}	
		return availableRooms;
	}

	/*
	 * Join existing room
	 *  Arguments : Id of gameRoom chosen by player,
	 *  		    player Name, Ip and Port
	 */
	public MsgObj joinRoom( int gameRoomId, String playerName, String ip, String port)
			throws MyDAOException{


		int clientNo = retrieveClientNo(gameRoomId);

		if(clientNo != -1){

			//create new player
			Player newPlayer = createNewPlayer(gameRoomId, playerName, ip, port, clientNo);

			// update no. of players in room
			int currentPlayers = roomDao.getNoPlayers(gameRoomId);
			currentPlayers++;
			roomDao.updateNoPlayers(currentPlayers, gameRoomId);	

			// return list of players
			ArrayList<Player> playersInRoom = new ArrayList<Player>();
			playersInRoom = playerDao.showPlayers(gameRoomId);


			MsgObj returnObj = new MsgObj();
			returnObj.setAllPlayers(playersInRoom);
			returnObj.setPlayerId(newPlayer.getPlayerId());
			returnObj.setClientNo(newPlayer.getClientNo());

			return returnObj;
		}
		return null;
	}

	/*
	 * Create New Player
	 */
	public Player createNewPlayer(int gameRoomId, String playerName, String ip,
			String port, int clientNo) throws MyDAOException{

		Player newPlayer = new Player();
		newPlayer.setGameRoomId(gameRoomId);
		newPlayer.setPlayerName(playerName);
		newPlayer.setIp(ip);
		newPlayer.setPort(port);
		newPlayer.setClientNo(clientNo);

		int myPlayerId = playerDao.createPlayer(newPlayer);
		newPlayer.setPlayerId(myPlayerId);
		return newPlayer;
	}

	/* 
	 * Create New Room
	 */
	public MsgObj createNewRoom ( String roomName, String playerName, String ip, String port) 
			throws MyDAOException{

		MsgObj returnObj = new MsgObj();
		GameRoom newRoom = new GameRoom();
		newRoom.setRoomName(roomName);
		newRoom.setCreatorName(playerName);
		newRoom.setNoPlayers(1);	
		/*
		 *  hard code available slots & client no,
		 *   since this player is first client in room
		 */
		newRoom.setAvailableSlots("100000");
		int clientNo = 1;
		// create new room
		int newRoomId = roomDao.createRoom(newRoom);

		//create player in new Room
		Player newPlayer = createNewPlayer(newRoomId, playerName, ip, port, clientNo);

		returnObj.setPlayerId(newPlayer.getPlayerId());
		returnObj.setClientNo(newPlayer.getClientNo());
		returnObj.setRoomId(newRoomId);

		return returnObj;
	}

	/*
	 * Get clientNo from next availableSlot
	 */
	private int retrieveClientNo(int gameRoomId) throws MyDAOException{

		String availableSlots = roomDao.getSlots(gameRoomId);
		int newSlot = availableSlots.indexOf("0");
		if(newSlot != -1){
			StringBuilder newSlots = new StringBuilder(availableSlots);
			newSlots.setCharAt(newSlot, '1');
			availableSlots = newSlots.toString();
			roomDao.updateSlots(availableSlots, gameRoomId);
		}
		return newSlot+1;
	}


	/*
	 * Leave Room
	 */
	public boolean leaveRoom(int playerId, int roomId, int clientNo) throws MyDAOException{

		int delPlayer = 0;
		int currentPlayerCount = 0;
		int delRoom = 0;
		int updateRoom = 0;
		String availableSlots = "";

		delPlayer = playerDao.deletePlayer(playerId);
		if(delPlayer == 1){
			currentPlayerCount = roomDao.getNoPlayers(roomId);
			currentPlayerCount--;
			delRoom = roomDao.updateNoPlayers(currentPlayerCount, roomId);
			availableSlots = roomDao.getSlots(roomId);
			StringBuilder newSlots = new StringBuilder(availableSlots);
			newSlots.setCharAt(clientNo-1, '0');
			availableSlots = newSlots.toString();
			updateRoom = roomDao.updateSlots(availableSlots, roomId);
		}

		if( delPlayer==1 && delRoom ==1 && updateRoom ==1)
			return true;

		return false;
	}
}
