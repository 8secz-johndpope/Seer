 package com.iBank.Database;
 
 import java.io.File;
 import java.sql.Connection;
 import java.sql.DriverManager;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 
 /**
  * Sqlite Implementation
  * @author steffengy
  *
  */
 public class SQLite {
 	private Connection connection = null;
 	private boolean success = false;
 	public SQLite(File db){
 		String sDbUrl = "jdbc:sqlite:" + db.getPath();
 		try{
 		connection = DriverManager.getConnection(sDbUrl);
 		success = true;
 		}catch(Exception e) {
 			System.out.println("[iBank] SQLite Connection to "+sDbUrl+" failed!"+e);
 		}
 	}
 	public ResultSet query(String query) {
 		try{
 		Statement statement = connection.createStatement();
 		ResultSet resultSet = statement.executeQuery(query);  
 		return resultSet;
 		}catch(Exception e) {
 			System.out.println("[iBank] Error in query "+query);
 		}
 		return null;
 	}
 	/**
 	 * Returns the id
 	 * @param query The command
 	 * @return
 	 */
 	public int insert(String query) {
 		try{
 			Statement statement = connection.createStatement();
 			statement.execute(query);
 			return statement.getGeneratedKeys().getInt(1);
 			}catch(Exception e) {
 				System.out.println("[iBank] Error in insert "+query+" "+e);
 				return -1;
 			}
 	}
 	public boolean existsTable(String name) {
 		try{
 		ResultSet tables = connection.getMetaData().getTables(null, null, name, null);
 		return tables.next();
 		}catch(Exception e) {
 			return false;
 		}
 	}
 	/**
 	 * Returns if an exception was thrown
 	 * @return
 	 */
 	public boolean success() {
 		return success;
 	}
 	/**
 	 * Executes a command
 	 * @param query
 	 */
 	public void execute(String query) {
 		try{
 			Statement statement = connection.createStatement();
 			statement.execute(query); 
 			}catch(Exception e) {
 				System.out.println("[iBank] Error in query "+query);
 			}
 	}
 	public void commit() {
 		try {
 			connection.commit();
 		} catch (SQLException e) { }
 	}
 	public void close() {
 		try {
 			connection.close();
		} catch (SQLException e) { }
 	}
 }
