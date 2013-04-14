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

public class GenreDAO {

	private List<Connection> connectionPool = new ArrayList<Connection>();

	private String jdbcDriver;
	private String jdbcURL;
	private String tableName;
	
	public GenreDAO(String jdbcDriver,
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
            stmt.executeUpdate("CREATE TABLE " + tableName + " (genreName VARCHAR(255) NOT NULL,"+
            " cardId INT NOT NULL, PRIMARY KEY(genreName,cardId))");
            stmt.close();
        	
        	releaseConnection(con);

        } catch (SQLException e) {
            try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
        	throw new MyDAOException(e);
        }
    }
	
	public void create(String genreName, int cardId) throws MyDAOException {
		Connection con = null;
        try {
        	con = getConnection();
        	con.setAutoCommit(false);
        	     
        	PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + tableName + " (genreName,cardId) VALUES (?,?)");
        	
        	//pstmt.setInt(1,genre.getGenreId());
        	pstmt.setString(1,genreName);
        	pstmt.setInt(2,cardId);
        	
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
        	
        	//genre.setGenreId(newId);
        	
        } catch (Exception e) {
            try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
        	throw new MyDAOException(e);
        }
	}
	
	public List<Integer> read(String genreName) throws MyDAOException {
		
		Connection con = null;
        try {
        	con = getConnection();

        	PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + tableName + " WHERE genreName = ?");
        	pstmt.setString(1,genreName);
        	ResultSet rs = pstmt.executeQuery();
        	
        	ArrayList<Integer> cardIds = new ArrayList<Integer>();
        	if (!rs.next()) {
        		cardIds = null;
        	} else {
        		do{	
        			cardIds.add(rs.getInt("cardId"));
        		}while(rs.next());
        		 		
        	}
        	
        	rs.close();
        	pstmt.close();
        	releaseConnection(con);
            return cardIds;
            
        } catch (Exception e) {
            try { if (con != null) con.close(); } catch (SQLException e2) { /* ignore */ }
        	throw new MyDAOException(e);
        }
	}


}
