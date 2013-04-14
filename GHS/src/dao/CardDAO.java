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

import databean.Card;

public class CardDAO {

	private List<Connection> connectionPool = new ArrayList<Connection>();

	private String jdbcDriver;
	private String jdbcURL;
	private String tableName;
	
	public CardDAO(String jdbcDriver,
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
            stmt.executeUpdate("CREATE TABLE " + tableName + " (cardId INT NOT NULL AUTO_INCREMENT,easy VARCHAR(255) NOT NULL,"+
            " medium VARCHAR(255) NOT NULL,hard VARCHAR(255) NOT NULL, PRIMARY KEY(cardId))");
            stmt.close();
        	
        	releaseConnection(con);

        } catch (SQLException e) {
            try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
        	throw new MyDAOException(e);
        }
    }
	
	public int create(Card card) throws MyDAOException {
		Connection con = null;
        try {
        	con = getConnection();
        	con.setAutoCommit(false);
        	
             
        	PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + tableName + " (cardId,easy,medium,hard) VALUES (?,?,?,?)");
        	
        	pstmt.setInt(1,card.getCardId());
        	pstmt.setString(2,card.getEasy());
        	pstmt.setString(3,card.getMedium());
        	pstmt.setString(4,card.getHard());
        	
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
        	
        	card.setCardId(newId);
        	
        	return newId;
        	
        } catch (Exception e) {
            try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
        	throw new MyDAOException(e);
        }
	}

	
	public Card read(int cardId) throws MyDAOException {
		Connection con = null;
        try {
        	con = getConnection();

        	PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + tableName + " WHERE cardId=?");
        	pstmt.setInt(1,cardId);
        	ResultSet rs = pstmt.executeQuery();
        	
        	Card card;
        	if (!rs.next()) {
        		card = null;
        	} else {
        		card = new Card();
        		card.setCardId(rs.getInt("cardId"));
        		card.setEasy(rs.getString("easy"));
        		card.setMedium(rs.getString("medium"));
        		card.setHard(rs.getString("hard"));
        		
        	}
        	
        	rs.close();
        	pstmt.close();
        	releaseConnection(con);
            return card;
            
        } catch (Exception e) {
            try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
        	throw new MyDAOException(e);
        }
	}

}
