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
import databean.Player;

public class PlayerDAO {

	private List<Connection> connectionPool = new ArrayList<Connection>();

	private String jdbcDriver;
	private String jdbcURL;
	private String tableName;

	public PlayerDAO(String jdbcDriver,
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
			stmt.executeUpdate("CREATE TABLE " + tableName + " (playerId INT NOT NULL AUTO_INCREMENT,playerName VARCHAR(255) NOT NULL,"+
					" ip VARCHAR(255) NOT NULL,port VARCHAR(255) NOT NULL,gameRoomId VARCHAR(255) NOT NULL, PRIMARY KEY(playerId))");
			stmt.close();

			releaseConnection(con);

		} catch (SQLException e) {
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}
	}

	public int createPlayer(Player player) throws MyDAOException {

		Connection con = null;
		try {
			con = getConnection();
			con.setAutoCommit(false);


			PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + tableName + 
					" (playerName,ip,port,gameRoomId) VALUES (?,?,?,?)");

			pstmt.setString(1,player.getPlayerName());
			pstmt.setString(2,player.getIp());
			pstmt.setString(3,player.getPort());
			pstmt.setInt(4,player.getGameRoomId());

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

			player.setPlayerId(newId);

			return newId;

		} catch (Exception e) {
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}
	}

	public ArrayList<Player> showPlayers(int gameRoomId) throws MyDAOException{
		
		Connection con = null;
		try {
			con = getConnection();

			PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + tableName+ " WHERE gameRoomId = ?");
			pstmt.setInt(1, gameRoomId);
			ResultSet rs = pstmt.executeQuery();

			ArrayList<Player> players = new ArrayList<Player>();
			if (!rs.next()) {
				players = null;
			} else {
				do{	
					Player tempPlayer = new Player();
					tempPlayer.setPlayerId(rs.getInt("playerId"));
					tempPlayer.setPlayerName(rs.getString("playerName"));
					tempPlayer.setIp(rs.getString("ip"));
					tempPlayer.setPort(rs.getString("port"));
					players.add(tempPlayer);
				}while(rs.next());
			}

			rs.close();
			pstmt.close();
			releaseConnection(con);
			return players;

		} catch (Exception e) {
			try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
			throw new MyDAOException(e);
		}	
	}
	
	public int deletePlayer(int playerId) throws MyDAOException{

		Connection con = null;
		try{
			con = getConnection();
			con.setAutoCommit(false);
			
			PreparedStatement pstmt = con.prepareStatement("DELETE FROM "+ tableName +
					" WHERE playerId = ?");
			pstmt.setInt(1, playerId);
	
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
