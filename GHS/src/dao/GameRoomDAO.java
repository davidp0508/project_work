package dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import databean.GameRoom;

public class GameRoomDAO {

	private List<Connection> connectionPool = new ArrayList<Connection>();

	private String jdbcDriver;
	private String jdbcURL;
	private String tableName;

	public GameRoomDAO(String jdbcDriver,
			String jdbcURL, String tableName) throws MyDAOException{
		this.jdbcDriver = jdbcDriver;
		this.jdbcURL = jdbcURL;
		this.tableName = tableName;

		if (!tableExists()) createTable();
	}

	private synchronized Connection getConnection() throws MyDAOException {
		if (connectionPool.size() > 0) {
			return connectionPool.remove(connectionPool.size()-1);
		}

		try {
			Class.forName(jdbcDriver);
		} catch (ClassNotFoundException e) {
			throw new MyDAOException(e);
		}

		try {
			return DriverManager.getConnection(jdbcURL);
		} catch (SQLException e) {
			throw new MyDAOException(e);
		}
	}

	private synchronized void releaseConnection(Connection con) {
		connectionPool.add(con);
	}

	private boolean tableExists() throws MyDAOException {
		Connection con = null;
		try {
			con = getConnection();
			DatabaseMetaData metaData = con.getMetaData();
			ResultSet rs = metaData.getTables(null, null, tableName, null);

			boolean answer = rs.next();

			rs.close();
			releaseConnection(con);

			return answer;

		} catch (SQLException e) {
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}
	}

	private void createTable() throws MyDAOException {
		Connection con = null;
		try {
			con = getConnection();
			Statement stmt = con.createStatement();
			stmt.executeUpdate("CREATE TABLE " + tableName + 
					" (roomId INT NOT NULL AUTO_INCREMENT,roomName VARCHAR(255) NOT NULL,"+
					" creatorName VARCHAR(255) NOT NULL,noPlayers INT,"+
					" availableSlots VARCHAR(255) NOT NULL,PRIMARY KEY(roomId))");
			stmt.close();

			releaseConnection(con);

		} catch (SQLException e) {
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}
	}

	public int createRoom(GameRoom room) throws MyDAOException {

		Connection con = null;
		try {
			con = getConnection();
			con.setAutoCommit(false);


			PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + tableName + 
					" (roomName, creatorName, noPlayers, availableSlots) VALUES (?,?,?,?)");

			pstmt.setString(1,room.getRoomName());
			pstmt.setString(2,room.getCreatorName());
			pstmt.setInt(3,room.getNoPlayers());
			pstmt.setString(4,room.getAvailableSlots());

			int count = pstmt.executeUpdate();
			if (count != 1) throw new SQLException("Insert updated "+count+" rows");

			pstmt.close();

			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
			int newId = rs.getInt("LAST_INSERT_ID()");

			con.commit();
			con.setAutoCommit(true);

			releaseConnection(con);

			room.setRoomId(newId);

			return newId;

		} catch (Exception e) {
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}
	}

	public ArrayList<GameRoom> showAllRooms() throws MyDAOException{

		Connection con = null;
		try {
			con = getConnection();

			PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + tableName);
			ResultSet rs = pstmt.executeQuery();

			ArrayList<GameRoom> gameRooms = new ArrayList<GameRoom>();
			if (!rs.next()) {
				gameRooms = null;
			} else {
				do{	
					GameRoom tempRoom = new GameRoom();
					tempRoom.setRoomId(rs.getInt("roomId"));
					tempRoom.setRoomName(rs.getString("roomName"));
					tempRoom.setCreatorName(rs.getString("creatorName"));
					tempRoom.setNoPlayers(rs.getInt("noPlayers"));
					tempRoom.setAvailableSlots(rs.getString("availableSlots"));
					gameRooms.add(tempRoom);
				}while(rs.next());
			}

			rs.close();
			pstmt.close();
			releaseConnection(con);
			return gameRooms;

		} catch (Exception e) {
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}	
	}

	public int getNoPlayers(int roomId) throws MyDAOException{

		Connection con = null;
		try{
			con = getConnection();

			PreparedStatement pstmt = con.prepareStatement("SELECT noPlayers from "+ tableName +
					" WHERE roomId = ?");
			pstmt.setInt(1, roomId);
			ResultSet rs = pstmt.executeQuery();

			int noPlayers;
			if (!rs.next()) {
				noPlayers = 0;
			} else {
				noPlayers = rs.getInt("noPlayers");
			}

			rs.close();
			pstmt.close();
			releaseConnection(con);
			return noPlayers;
		}catch(Exception e){
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}		
	}

	public int updateNoPlayers(int noPlayers, int roomId) throws MyDAOException{

		Connection con = null;
		try{
			con = getConnection();
			con.setAutoCommit(false);
			
			PreparedStatement pstmt = con.prepareStatement("UPDATE "+ tableName +
					" SET noPlayers = ? WHERE roomId = ?");
			pstmt.setInt(1, noPlayers);
			pstmt.setInt(2, roomId);
		
			int res = pstmt.executeUpdate();
			con.commit();
			con.setAutoCommit(true);
			pstmt.close();
			releaseConnection(con);

			return res;
		}catch(Exception e){
			try { 
				if (con != null) 
					con.close(); 
			} 
			catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}		
	}
	
	public String getSlots(int roomId) throws MyDAOException{

		Connection con = null;
		try{
			con = getConnection();

			PreparedStatement pstmt = con.prepareStatement("SELECT availableSlots from "+ tableName +
					" WHERE roomId = ?");
			pstmt.setInt(1, roomId);
			ResultSet rs = pstmt.executeQuery();

			String availableSlots;
			if (!rs.next()) {
				availableSlots = "";
			} else {
				availableSlots = rs.getString("availableSlots");
			}

			rs.close();
			pstmt.close();
			releaseConnection(con);
			return availableSlots;
		}catch(Exception e){
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}		
	}

	public int updateSlots(String availableSlots, int roomId) throws MyDAOException{

		Connection con = null;
		try{
			con = getConnection();
			con.setAutoCommit(false);
			
			PreparedStatement pstmt = con.prepareStatement("UPDATE "+ tableName +
					" SET availableSlots = ? WHERE roomId = ?");
			pstmt.setString(1, availableSlots);
			pstmt.setInt(2, roomId);
		
			int res = pstmt.executeUpdate();
			con.commit();
			con.setAutoCommit(true);
			pstmt.close();
			releaseConnection(con);

			return res;
		}catch(Exception e){
			try { 
				if (con != null) 
					con.close(); 
			} 
			catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}		
	}
	
}
