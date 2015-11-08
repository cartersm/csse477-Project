import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * A helper class to connect to and manage the Ghostbusters database. Modified
 * from
 * <a href="http://www.ccs.neu.edu/home/kathleen/classes/cs3200/DBDemo.java">
 * This demo</a>.
 * 
 * @author cartersm
 *
 */
public class DBHelper extends DBDemo {
	public static final String GHOSTS_TABLE_NAME = "GHOSTS";
	public static final String HAUNTS_TABLE_NAME = "HAUNTS";

	public void start() {
		// Connect to MySQL
		Connection conn = null;
		try {
			conn = this.getConnection();
			System.out.println("Connected to database");
		} catch (SQLException e) {
			System.out.println("ERROR: Could not connect to the database");
			e.printStackTrace();
			return;
		}
		
		// Create tables if they don't exist
		try {
			DatabaseMetaData metadata = conn.getMetaData();
			ResultSet results = metadata.getTables(null, null, GHOSTS_TABLE_NAME, null);
			if (!results.next()) {
				String createGhostsString = 
						"CREATE TABLE" + GHOSTS_TABLE_NAME + " ( " +
						"ID INTEGER NOT NULL, " + 
						"NAME varchar(40) NOT NULL, " + 
						"TYPE varchar(40) NOT NULL, " + 
						"PRIMARY KEY (ID))";
				this.executeUpdate(conn, createGhostsString);
			}			
			results = metadata.getTables(null, null, HAUNTS_TABLE_NAME, null);
			if (!results.next()) {
				String createHauntsString = 
						"CREATE TABLE" + HAUNTS_TABLE_NAME + " ( " +
						"ID INTEGER NOT NULL, " + 
						"NAME varchar(40) NOT NULL, " + 
						"PRIMARY KEY (ID))";
				this.executeUpdate(conn, createHauntsString);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}
}
