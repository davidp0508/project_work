package databean;

import java.util.ArrayList;

public class MsgObj {

	int playerId;
	
	int roomId;
	
	ArrayList<Player> allPlayers;

	public int getPlayerId() {
		return playerId;
	}

	public void setPlayerId(int playerId) {
		this.playerId = playerId;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public ArrayList<Player> getAllPlayers() {
		return allPlayers;
	}

	public void setAllPlayers(ArrayList<Player> allPlayers) {
		this.allPlayers = allPlayers;
	}
	
	
}
