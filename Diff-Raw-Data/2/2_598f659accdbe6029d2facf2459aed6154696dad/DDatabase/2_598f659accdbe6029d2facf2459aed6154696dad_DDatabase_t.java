 package com.legit2.Demigods;
 
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.util.HashMap;
 import java.util.Map.Entry;
 
 import org.bukkit.OfflinePlayer;
 import org.bukkit.entity.Player;
 
 import com.legit2.Demigods.Utilities.DCharUtil;
 import com.legit2.Demigods.Utilities.DDataUtil;
 import com.legit2.Demigods.Utilities.DObjUtil;
 import com.legit2.Demigods.Utilities.DPlayerUtil;
 import com.legit2.Demigods.Utilities.DUtil;
 
 public class DDatabase
 {
 	/*
 	 *  initializeDatabase() : Loads the MySQL or SQLite database.
 	 */
 	public static void initializeDatabase()
 	{
 		// Check if MySQL is enabled in the configuration and if so, attempts to connect.
 		if(DConfig.getSettingBoolean("mysql"))
 		{
 			DMySQL.createConnection();
 			DMySQL.initializeMySQL();
 			loadAllData();
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 	}
 
 	/*
 	 *  uninitializeDatabase() : Unloads the MySQL or SQLite database.
 	 */
 	public static void uninitializeDatabase()
 	{
 		saveAllData();
 		
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{
 			DMySQL.uninitializeMySQL();
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 	}
 
 	/*
 	 *  addPlayerToDB() : Adds the player to the database.
 	 */
 	public static void addPlayerToDB(OfflinePlayer player) throws SQLException
 	{
 		// Define variables
 		Long firstLoginTime = System.currentTimeMillis();
 
 		// Next we add them to the Database if needed
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{	
 			int playerID = DPlayerUtil.getPlayerID(player);
 			String playerName = player.getName();
 			
 			String addQuery = "INSERT INTO " + DMySQL.player_table + " (player_id, player_name, player_characters, player_kills, player_deaths, player_firstlogin, player_lastlogin) VALUES (" + playerID + ",'" + playerName + "', NULL, 0, 0," + firstLoginTime + "," + firstLoginTime +");";
 			DMySQL.runQuery(addQuery);
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 	}
 	
 	/*
 	 *  removePlayerFromDB() : Removes the player from the database.
 	 */
 	public static void removePlayerFromDB(OfflinePlayer player) throws SQLException
 	{
 		// Next we add them to the Database if needed
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{	
 			// TODO: Remove player from MySQL
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 	}
 	
 	/*
 	 *  addPlayerToDB() : Adds the player to the database.
 	 */
 	public static void addCharToDB(OfflinePlayer player, int charID) throws SQLException
 	{
 		// Next we add them to the Database if needed
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{	
 			int playerID = DPlayerUtil.getPlayerID(player);
 			boolean charActive = DCharUtil.isActive(charID);
 			String charName = DCharUtil.getName(charID);
 			String charDeity = DCharUtil.getDeity(charID);
 			String charAlliance = DCharUtil.getAlliance(charID);
 			boolean charImmortal = DCharUtil.getImmortal(charID);
 			int charHP = DCharUtil.getHP(charID);
 			float charExp = DCharUtil.getExp(charID);
 			int charFavor = DCharUtil.getFavor(charID);
 			int charDevotion = DCharUtil.getDevotion(charID);
 			int charAscensions = DCharUtil.getAscensions(charID);
 			double charLastX = 0.0;
 			double charLastY = 0.0;
 			double charLastZ = 0.0;
 			String charLastW = "";
 			
 			String addQuery = 
 					"INSERT INTO " + DMySQL.character_table +
 					"(char_id,player_id,char_active,char_name,char_deity,char_alliance,char_immortal,char_hp,char_exp,char_favor,char_devotion,char_ascensions,char_lastX,char_lastY,char_lastZ,char_lastW)" + 
 					"VALUES (" +
 						charID + "," +
 						playerID + "," +
 						charActive + "," +
 						"'" + charName + "'," +
 						"'" + charDeity + "'," +
 						"'" + charAlliance + "'," +
 						charImmortal + "," +
 						charHP + "," +
 						charExp + "," +
 						charFavor + "," +
 						charDevotion + "," +
 						charAscensions + "," +
 						charLastX + "," +
 						charLastY + "," +
 						charLastZ + "," +
 						"'" + charLastW + "'" +
 					");";
 			
 			DMySQL.runQuery(addQuery);
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 	}
 
 	/*
 	 *  getPlayerInfo() : Grabs the player info from MySQL/FlatFile and returns (ResultSet)result.
 	 */
 	public static ResultSet getPlayerInfo(String username) throws SQLException
 	{
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{
 			// TODO: Return player info from MySQL
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 
 		return null;
 	}
 	
 	/*
 	 *  loadAllData() : Loads all data from database into HashMaps.
 	 */
 	public static void loadAllData()
 	{
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{	
 			DUtil.info("Loading Demigods data...");
 
 			// Define variables
 			int playerCount = 0;
 			int characterCount = 0;
 			long startStopwatch = System.currentTimeMillis();
 
 			// Define SELECT queries
 			String selectPlayer = "SELECT * FROM " + DMySQL.player_table + " LEFT JOIN " + DMySQL.playerdata_table + " ON " + DMySQL.player_table + ".player_id = " + DMySQL.playerdata_table + ".player_id;";
 			ResultSet playerResult = DMySQL.runQuery(selectPlayer);
 			
 			try 
 			{
 				while(playerResult.next())
 				{
 					playerCount++;
 					
 					OfflinePlayer player = DPlayerUtil.definePlayer(playerResult.getString("player_name"));
 					int playerID = playerResult.getInt("player_id");
 					
 					// Load the main player data
 					DDataUtil.addPlayer(player, playerID);
					DDataUtil.savePlayerData(player, "player_id", playerResult.getInt("player_id"));
 					DDataUtil.savePlayerData(player, "player_characters", playerResult.getString("player_characters"));
 					DDataUtil.savePlayerData(player, "player_kills", playerResult.getInt("player_kills"));
 					DDataUtil.savePlayerData(player, "player_deaths", playerResult.getInt("player_deaths"));
 					DDataUtil.savePlayerData(player, "player_firstlogin", playerResult.getLong("player_firstlogin"));
 				
 					// Load other player data
 					if(playerResult.getString("datakey") != null)
 					{
 						if(playerResult.getString("datakey").contains("boolean_"))
 						{
 							DDataUtil.savePlayerData(player, playerResult.getString("datakey"), playerResult.getBoolean("datavalue"));
 						}
 						else
 						{
 							DDataUtil.savePlayerData(player, playerResult.getString("datakey"), playerResult.getString("datavalue"));
 						}
 					}
 				
 					String selectCharacter = "SELECT * FROM " + DMySQL.character_table + " LEFT JOIN " + DMySQL.chardata_table + " ON " + DMySQL.character_table + ".char_id = " + DMySQL.chardata_table + ".char_id AND " + DMySQL.character_table + ".player_id=" + playerID + ";";
 					ResultSet charResult = DMySQL.runQuery(selectCharacter);
 
 
 					while(charResult.next())
 					{
 						characterCount++;
 						
 						int charID = charResult.getInt("char_id");
 						
 						// Load the main character data
 						DDataUtil.addChar(charID);
 						DDataUtil.saveCharData(charID, "char_owner", charResult.getString("player_id"));
 						DDataUtil.saveCharData(charID, "char_name", charResult.getString("char_name"));
 						DDataUtil.saveCharData(charID, "char_active", charResult.getString("char_active"));
 						DDataUtil.saveCharData(charID, "char_deity", charResult.getString("char_deity"));
 						DDataUtil.saveCharData(charID, "char_alliance", charResult.getString("char_alliance"));
 						DDataUtil.saveCharData(charID, "char_immortal", charResult.getBoolean("char_immortal"));
 						DDataUtil.saveCharData(charID, "char_hp", charResult.getInt("char_hp"));
 						DDataUtil.saveCharData(charID, "char_exp", charResult.getInt("char_exp"));
 						DDataUtil.saveCharData(charID, "char_lastX", charResult.getDouble("char_lastX"));
 						DDataUtil.saveCharData(charID, "char_lastY", charResult.getDouble("char_lastY"));
 						DDataUtil.saveCharData(charID, "char_lastZ", charResult.getDouble("char_lastZ"));
 						DDataUtil.saveCharData(charID, "char_lastW", charResult.getString("char_lastW"));
 						DDataUtil.saveCharData(charID, "char_favor", charResult.getInt("char_favor"));
 						DDataUtil.saveCharData(charID, "char_devotion", charResult.getInt("char_devotion"));
 						DDataUtil.saveCharData(charID, "char_ascensions", charResult.getInt("char_ascensions"));
 						
 						// Load other character data
 						if(charResult.getString("datakey") != null)
 						{
 							if(charResult.getString("datakey").contains("boolean_"))
 							{
 								DDataUtil.saveCharData(charID, charResult.getString("datakey"), charResult.getBoolean("datavalue"));
 							}
 							else
 							{
 								DDataUtil.saveCharData(charID, charResult.getString("datakey"), charResult.getString("datavalue"));
 							}
 						}
 					}
 				}
 			}
 			catch(SQLException e)
 			{
 				// There was an error with the SQL.
 				DUtil.severe("Error while loading Demigods data. (ERR: 1001)");
 				e.printStackTrace();
 			}
 			
 			// Stop the timer
 			long stopStopwatch = System.currentTimeMillis();
 			double totalTime = (double) (stopStopwatch - startStopwatch);
 			
 			// Send data load success message
 			if(DConfig.getSettingBoolean("data_debug")) DUtil.info("Loaded data for " + playerCount + " players and " + characterCount + " characters in " + totalTime/1000 + " seconds.");
 			else DUtil.info("Loaded data for " + playerCount + " players and " + characterCount + " characters.");
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 	}
 	
 	/*
 	 *  saveAllData() : Saves all HashMap data to database.
 	 */
 	public static boolean saveAllData()
 	{
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{	
 			// Define variables
 			int playerCount = 0;
 			long startTimer = System.currentTimeMillis();
 			
 			// Save plugin-specific data
 			savePlugin();
 			long stopTimer = System.currentTimeMillis();
 			double totalTime = (double) (stopTimer - startTimer);
 			if(DConfig.getSettingBoolean("data_debug")) DUtil.info("Demigods plugin data saved in " + totalTime/1000 + " seconds.");
 			else DUtil.info("Demigods plugin data saved.");
 					
 			for(Player player : DUtil.getOnlinePlayers())
 			{
 				if(savePlayer(player)) playerCount++;
 			}
 
 			// Stop the timer
 			stopTimer = System.currentTimeMillis();
 			totalTime = (double) (stopTimer - startTimer);
 
 			// Send save success message
 			if(DConfig.getSettingBoolean("data_debug")) DUtil.info("Success! Saved " + playerCount + " of " + DMySQL.getRows(DMySQL.runQuery("SELECT * FROM " + DMySQL.player_table + ";")) + " players in " + totalTime/1000 + " seconds.");
 			else DUtil.info("Success! Saved " + playerCount + " of " + DMySQL.getRows(DMySQL.runQuery("SELECT * FROM " + DMySQL.player_table + ";")) + " players.");
 			return true;
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 		return false;
 	}
 	
 	/*
 	 *  savePlayerData() : Saves all HashMap data for (OfflinePlayer)player to database.
 	 */
 	public static boolean savePlayer(OfflinePlayer player)
 	{
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{			
 			int playerID = DPlayerUtil.getPlayerID(player);
 
 			// Clear tables first
 			DMySQL.runQuery("DELETE FROM " + DMySQL.playerdata_table + " WHERE player_id=" + playerID);
 
 			// Save their player-specific data
 			HashMap<String, Object> allPlayerData = DDataUtil.getAllPlayerData(player);				
 		
 			// Define player-specific variables
 			String playerChars = (String) allPlayerData.get("player_characters");
 			int playerKills = DObjUtil.toInteger(allPlayerData.get("player_kills"));
 			int playerDeaths = DObjUtil.toInteger(allPlayerData.get("player_deaths"));
 			Long playerLastLogin = (Long) allPlayerData.get("player_lastlogin");
 			
 			// Update main player table
 			DMySQL.runQuery("UPDATE " + DMySQL.player_table + " SET player_characters='" + playerChars + "',player_kills=" + playerKills + ",player_deaths=" + playerDeaths + ",player_lastlogin=" + playerLastLogin + " WHERE player_id=" + playerID + ";");
 			
 			// Save miscellaneous player data
 			DMySQL.runQuery("DELETE FROM " + DMySQL.playerdata_table + " WHERE player_id=" + playerID + ";");
 			for(Entry<String, Object> playerData : allPlayerData.entrySet()) if(!playerData.getKey().contains("player_")) DMySQL.runQuery("INSERT INTO " + DMySQL.playerdata_table + " (player_id, datakey, datavalue) VALUES(" + playerID + ",'" + playerData.getKey() + "','" + playerData.getValue() + "');");
 			
 				
 			// Save their character-specific data now
 			HashMap<Integer, HashMap<String, Object>> playerCharData = DDataUtil.getAllPlayerChars(player);
 			for(Entry<Integer, HashMap<String, Object>> playerChar : playerCharData.entrySet())
 			{
 				// Define character-specific variables
 				int charID = playerChar.getKey();
 				boolean charImmortal = DObjUtil.toBoolean(playerCharData.get(charID).get("char_immortal"));
 				int charHP = DObjUtil.toInteger(playerCharData.get(charID).get("char_hp"));
 				float charExp = DObjUtil.toFloat(playerCharData.get(charID).get("char_exp"));
 				int charFavor = DObjUtil.toInteger(playerCharData.get(charID).get("char_favor"));
 				int charDevotion = DObjUtil.toInteger(playerCharData.get(charID).get("char_devotion"));
 				int charAscensions = DObjUtil.toInteger(playerCharData.get(charID).get("char_ascensions"));
 				Double charLastX = (Double) playerCharData.get(charID).get("char_lastx");
 				Double charLastY = (Double) playerCharData.get(charID).get("char_lasty");
 				Double charLastZ = (Double) playerCharData.get(charID).get("char_lastz");
 				String charLastW = (String) playerCharData.get(charID).get("char_lastw");
 				
 				// Update main character table
 				DMySQL.runQuery("UPDATE " + DMySQL.character_table + " SET char_immortal=" + charImmortal + ",char_hp=" + charHP + ",char_exp=" + charExp + ",char_favor=" + charFavor + ",char_devotion=" + charDevotion + ",char_ascensions=" + charAscensions + ",char_lastX=" + charLastX + ",char_lastY=" + charLastY + ",char_lastZ=" + charLastZ + ",char_lastW='" + charLastW + "'  WHERE char_id=" + charID + ";");
 
 				// Save miscellaneous character data
 				HashMap<String, Object> charData = playerChar.getValue();
 				DMySQL.runQuery("DELETE FROM " + DMySQL.chardata_table + " WHERE char_id=" + charID + ";");
 				for(Entry<String, Object> character : charData.entrySet()) if(!character.getKey().contains("char_")) DMySQL.runQuery("INSERT INTO " + DMySQL.chardata_table + " (char_id, datakey, datavalue) VALUES(" + charID + ",'" + character.getKey() + "','" + character.getValue() + "');");
 			}
 			return true;
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 		return false;
 	}
 	
 	/*
 	 *  savePluginData() : Saves all HashMap data for the plugin to the database.
 	 */
 	public static boolean savePlugin()
 	{
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{			
 			// Clear tables first
 			DMySQL.runQuery("TRUNCATE TABLE " + DMySQL.plugindata_table + ";");
 
 			// Save their player-specific data
 			HashMap<String, HashMap<String, Object>> allPluginData = DDataUtil.getAllPluginData();				
 
 			// Save data
 			for(Entry<String, HashMap<String, Object>> pluginData : allPluginData.entrySet())
 			{
 				String dataID = pluginData.getKey();
 				
 				for(Entry<String, Object> data : pluginData.getValue().entrySet())
 				if(!pluginData.getKey().contains("temp_"))
 				{
 					String dataKey = data.getKey();
 					Object dataValue = data.getValue();
 					
 					DMySQL.runQuery("INSERT INTO " + DMySQL.plugindata_table + " (data_id, datakey, datavalue) VALUES('" + dataID + "','" + dataKey + "','" + dataValue + "');");
 				}
 			}
 					
 			return true;
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 		return false;
 	}
 	
 	/*
 	 *  removePlayer() : Removes the player completely from the database.
 	 */
 	public static boolean removePlayer(OfflinePlayer player)
 	{
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{			
 			int playerID = DPlayerUtil.getPlayerID(player);
 			
 			DMySQL.runQuery("DELETE FROM " + DMySQL.player_table + " WHERE player_id=" + playerID + ";");
 			DMySQL.runQuery("DELETE FROM " + DMySQL.playerdata_table + " WHERE player_id=" + playerID + ";");
 			DMySQL.runQuery("DELETE FROM " + DMySQL.character_table + " WHERE player_id=" + playerID + ";");
 			
 			return true;
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 		return false;
 	}
 	
 	/*
 	 *  removeChar() : Removes the character completely from the database.
 	 */
 	public static boolean removeChar(int charID)
 	{
 		if(DConfig.getSettingBoolean("mysql") && DMySQL.checkConnection())
 		{						
 			DMySQL.runQuery("DELETE FROM " + DMySQL.character_table + " WHERE char_id=" + charID + ";");
 			DMySQL.runQuery("DELETE FROM " + DMySQL.chardata_table + " WHERE char_id=" + charID + ";");
 			
 			return true;
 		}
 		else if(DConfig.getSettingBoolean("sqlite"))
 		{
 			// TODO: SQLite
 		}
 		return false;
 	}
 }
