import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
	private final String serverName = "cartersm-w530.rose-hulman.edu";
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
				int rows = stmt.executeUpdate(command);
				return rows > 0;
			} finally {
				if (stmt != null) {
					stmt.close();
				}
			}
		}
		return false;
	}

	public File executeQuery(String command, String rootDirectory) throws SQLException, IOException {
		Statement stmt = null;
		if (this.conn != null) {
			final String fileName = UUID.randomUUID().toString();
			final File file = File.createTempFile(fileName, ".json");
			try {
				stmt = this.conn.createStatement();
				ResultSet results = stmt.executeQuery(command);
				ResultSetMetaData rsmd = results.getMetaData();

				// Borrowed from http://stackoverflow.com/questions/18960446/how-to-convert-a-java-resultset-into-json
				JsonWriter writer = new JsonWriter(new PrintWriter(file));
				int count = 0;
				int numRows = 0;
				boolean hasRows = results.last();
				if (hasRows) {
					numRows = results.getRow();
				} 
				results.beforeFirst();
				
				writer.beginObject();
				writer.name("elements");
				writer.beginArray();
				
				while (results.next()) {
					writer.beginObject();
					for (int idx = 1; idx <= rsmd.getColumnCount(); idx++) {
						writer.name(rsmd.getColumnLabel(idx));
						writer.value(results.getString(idx));
					}
					writer.endObject();
					count++;
				}
				if (count == 0) {
					writer.beginObject();
					writer.endObject();
				}
				writer.endArray();
				writer.endObject();
				
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
				String createGhostsString = "CREATE TABLE " + GHOSTS_TABLE_NAME 
						+ " ( " + "id INTEGER NOT NULL AUTO_INCREMENT, "
						+ "name varchar(40) NOT NULL, " 
						+ "type varchar(40) NOT NULL, " 
						+ "PRIMARY KEY (id));";
				this.executeUpdate(createGhostsString);
			}
			results = metadata.getTables(null, null, HAUNTS_TABLE_NAME, null);
			if (!results.next()) {
				String createHauntsString = "CREATE TABLE " + HAUNTS_TABLE_NAME 
						+ " ( " + "id INTEGER NOT NULL AUTO_INCREMENT, "
						+ "name varchar(40) NOT NULL, " 
						+ "PRIMARY KEY (id))";
				this.executeUpdate(createHauntsString);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		DBHelper helper = new DBHelper();
		helper.start();
//		helper.executeUpdate("DROP TABLE IF EXISTS GHOSTS;");
//		helper.executeUpdate("DROP TABLE IF EXISTS HAUNTS;");
	}
}
