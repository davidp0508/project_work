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

		//create new player
		Player newPlayer = createNewPlayer(gameRoomId, playerName, ip, port);

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
		
		return returnObj;
	}

	/*
	 * Create New Player
	 */
	public Player createNewPlayer(int gameRoomId, String playerName, String ip,
			String port) throws MyDAOException{
		
		Player newPlayer = new Player();
		newPlayer.setGameRoomId(gameRoomId);
		newPlayer.setPlayerName(playerName);
		newPlayer.setIp(ip);
		newPlayer.setPort(port);

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
		
		// create new room
		int newRoomId = roomDao.createRoom(newRoom);
		
		//create player in new Room
		Player newPlayer = createNewPlayer(newRoomId, playerName, ip, port);
		
		returnObj.setPlayerId(newPlayer.getPlayerId());
		returnObj.setRoomId(newRoomId);
		
		return returnObj;
	}
	
	/*
	 * Leave Room
	 */
	public boolean leaveRoom(int playerId, int roomId) throws MyDAOException{
		
		int delPlayer = 0;
		int currentPlayerCount = 0;
		int delRoom = 0;
		
		delPlayer = playerDao.deletePlayer(playerId);
		if(delPlayer == 1){
			currentPlayerCount = roomDao.getNoPlayers(roomId);
			currentPlayerCount--;
			delRoom = roomDao.updateNoPlayers(currentPlayerCount, roomId);
		}
	
		if( delPlayer==1 && delRoom ==1)
			return true;
		
		return false;
	}
}
