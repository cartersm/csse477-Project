import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import com.google.gson.stream.JsonWriter;

/**
 * A helper class to connect to and manage the Ghostbusters database. Modified
 * from
 * <a href="http://www.ccs.neu.edu/home/kathleen/classes/cs3200/DBDemo.java">
 * This demo</a>.
 * 
 * @author cartersm
 *
 */
public class DBHelper {
	public static final String GHOSTS_TABLE_NAME = "GHOSTS";
	public static final String HAUNTS_TABLE_NAME = "HAUNTS";

	private final String userName = "root";
	private final String password = "root";
	private final String serverName = "localhost";
	private final int portNumber = 3306;
	private final String dbName = "test";
	private Connection conn = null;

	/**
	 * Get a new database connection
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		Connection conn = null;
		Properties connectionProps = new Properties();
		connectionProps.put("user", this.userName);
		connectionProps.put("password", this.password);

		conn = DriverManager.getConnection(
				"jdbc:mysql://" + this.serverName + ":" + this.portNumber + "/" + this.dbName, connectionProps);

		return conn;
	}

	public boolean executeUpdate(String command) throws SQLException {
		Statement stmt = null;
		if (this.conn != null) {
			try {
				stmt = this.conn.createStatement();
				stmt.executeUpdate(command);
				return true;
			} finally {
				if (stmt != null) {
					stmt.close();
				}
			}
		}
		return false;
	}

	public File executeQuery(String command) throws SQLException, IOException {
		Statement stmt = null;
		if (this.conn != null) {
			try {
				stmt = this.conn.createStatement();
				ResultSet results = stmt.executeQuery(command);
				ResultSetMetaData rsmd = results.getMetaData();
				final File file = new File("/tmp/" + UUID.randomUUID() + ".json");
				// Borrowed from http://stackoverflow.com/questions/18960446/how-to-convert-a-java-resultset-into-json
				JsonWriter writer = new JsonWriter(new FileWriter(file));
				
				while (results.next()) {
					writer.beginObject();
					for (int idx = 1; idx <= rsmd.getColumnCount(); idx++) {
						writer.name(rsmd.getColumnLabel(idx));
						writer.value(results.getString(idx));
					}
					writer.endObject();
				}
				writer.flush();
				writer.close();
				return file;

			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			} finally {
				if (stmt != null) {
					stmt.close();
				}
			}
		}
		return null;
	}

	public void start() {
		// Connect to MySQL
		try {
			this.conn = this.getConnection();
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
				String createGhostsString = "CREATE TABLE" + GHOSTS_TABLE_NAME + " ( " + "ID INTEGER NOT NULL, "
						+ "NAME varchar(40) NOT NULL, " + "TYPE varchar(40) NOT NULL, " + "PRIMARY KEY (ID))";
				this.executeUpdate(createGhostsString);
			}
			results = metadata.getTables(null, null, HAUNTS_TABLE_NAME, null);
			if (!results.next()) {
				String createHauntsString = "CREATE TABLE" + HAUNTS_TABLE_NAME + " ( " + "ID INTEGER NOT NULL, "
						+ "NAME varchar(40) NOT NULL, " + "PRIMARY KEY (ID))";
				this.executeUpdate(createHauntsString);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}
}
