 import java.sql.Connection;
 import java.sql.DriverManager;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.logging.Level;
 
 /**
  * MySQLSource.java - Used for accessing users and such from a mysql database
  * @author James
  */
 public class MySQLSource extends DataSource {
 
     private String driver, username, password, db;
 
     private Connection getConnection() {
         try {
             return DriverManager.getConnection(db + "?autoReconnect=true&user=" + username + "&password=" + password);
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to retreive connection", ex);
         }
         return null;
     }
 
     public void initialize() {
         PropertiesFile properties = new PropertiesFile("mysql.properties");
         driver = properties.getString("driver", "com.mysql.jdbc.Driver");
         username = properties.getString("user", "root");
         password = properties.getString("pass", "root");
         db = properties.getString("db", "jdbc:mysql://localhost:3306/minecraft");
 
         try {
             Class.forName(driver);
         } catch (ClassNotFoundException ex) {
             log.log(Level.SEVERE, "Unable to find class " + driver, ex);
         }
 
         loadGroups();
         loadKits();
         loadHomes();
         loadWarps();
         loadItems();
         loadWhitelist();
         loadReserveList();
     }
 
     public void loadGroups() {
         synchronized (groupLock) {
             groups = new ArrayList<Group>();
             Connection conn = null;
             PreparedStatement ps = null;
             ResultSet rs = null;
             try {
                 conn = getConnection();
                 ps = conn.prepareStatement("SELECT * FROM groups");
                 rs = ps.executeQuery();
                 while (rs.next()) {
                     Group group = new Group();
                     group.Administrator = rs.getBoolean("admin");
                     group.CanModifyWorld = rs.getBoolean("canmodifyworld");
                     group.Commands = rs.getString("commands").split(",");
                     group.DefaultGroup = rs.getBoolean("defaultgroup");
                     group.ID = rs.getInt("id");
                     group.IgnoreRestrictions = rs.getBoolean("ignoresrestrictions");
                     group.InheritedGroups = rs.getString("inheritedgroups").split(",");
                     group.Name = rs.getString("name");
                     group.Prefix = rs.getString("prefix");
                     groups.add(group);
                 }
             } catch (SQLException ex) {
                 log.log(Level.SEVERE, "Unable to retreive groups from group table", ex);
             } finally {
                 try {
                     if (ps != null) {
                         ps.close();
                     }
                     if (rs != null) {
                         rs.close();
                     }
                     if (conn != null) {
                         conn.close();
                     }
                 } catch (SQLException ex) {
                 }
             }
         }
     }
 
     public void loadKits() {
         synchronized (kitLock) {
             kits = new ArrayList<Kit>();
             Connection conn = null;
             PreparedStatement ps = null;
             ResultSet rs = null;
             try {
                 conn = getConnection();
                 ps = conn.prepareStatement("SELECT * FROM kits");
                 rs = ps.executeQuery();
                 while (rs.next()) {
                     Kit kit = new Kit();
                     kit.Delay = rs.getInt("delay");
                     kit.Group = rs.getString("group");
                     kit.ID = rs.getInt("id");
                     kit.Name = rs.getString("name");
                     kit.IDs = new HashMap<String, Integer>();
 
                     String[] ids = rs.getString("items").split(",");
                     for (String str : ids) {
                         String id = "";
                         int amount = 1;
                         if (str.contains(" ")) {
                             id = str.split(" ")[0];
                             amount = Integer.parseInt(str.split(" ")[1]);
                         } else {
                             id = str;
                         }
                         kit.IDs.put(id, amount);
                     }
                     kits.add(kit);
                 }
             } catch (SQLException ex) {
                 log.log(Level.SEVERE, "Unable to retreive kits from kit table", ex);
             } finally {
                 try {
                     if (ps != null) {
                         ps.close();
                     }
                     if (rs != null) {
                         rs.close();
                     }
                     if (conn != null) {
                         conn.close();
                     }
                 } catch (SQLException ex) {
                 }
             }
         }
     }
 
     public void loadHomes() {
         synchronized (homeLock) {
             homes = new ArrayList<Warp>();
             if (!etc.getInstance().saveHomes) {
                 return;
             }
             Connection conn = null;
             PreparedStatement ps = null;
             ResultSet rs = null;
             try {
                 conn = getConnection();
                 ps = conn.prepareStatement("SELECT * FROM homes");
                 rs = ps.executeQuery();
                 while (rs.next()) {
                     Location location = new Location();
                     location.x = rs.getDouble("x");
                     location.y = rs.getDouble("y");
                     location.z = rs.getDouble("z");
                     location.rotX = rs.getFloat("rotX");
                     location.rotY = rs.getFloat("rotY");
                     Warp home = new Warp();
                     home.ID = rs.getInt("id");
                     home.Location = location;
                     home.Name = rs.getString("name");
                     home.Group = rs.getString("group");
                     homes.add(home);
                 }
             } catch (SQLException ex) {
                 log.log(Level.SEVERE, "Unable to retreive homes from home table", ex);
             } finally {
                 try {
                     if (ps != null) {
                         ps.close();
                     }
                     if (rs != null) {
                         rs.close();
                     }
                     if (conn != null) {
                         conn.close();
                     }
                 } catch (SQLException ex) {
                 }
             }
         }
     }
 
     public void loadWarps() {
         synchronized (warpLock) {
             warps = new ArrayList<Warp>();
             Connection conn = null;
             PreparedStatement ps = null;
             ResultSet rs = null;
             try {
                 conn = getConnection();
                 ps = conn.prepareStatement("SELECT * FROM warps");
                 rs = ps.executeQuery();
                 while (rs.next()) {
                     Location location = new Location();
                     location.x = rs.getDouble("x");
                     location.y = rs.getDouble("y");
                     location.z = rs.getDouble("z");
                     location.rotX = rs.getFloat("rotX");
                     location.rotY = rs.getFloat("rotY");
                     Warp warp = new Warp();
                     warp.ID = rs.getInt("id");
                     warp.Location = location;
                     warp.Name = rs.getString("name");
                     warp.Group = rs.getString("group");
                     warps.add(warp);
                 }
             } catch (SQLException ex) {
                 log.log(Level.SEVERE, "Unable to retreive warps from warp table", ex);
             } finally {
                 try {
                     if (ps != null) {
                         ps.close();
                     }
                     if (rs != null) {
                         rs.close();
                     }
                     if (conn != null) {
                         conn.close();
                     }
                 } catch (SQLException ex) {
                 }
             }
         }
     }
 
     public void loadItems() {
         synchronized (itemLock) {
             items = new HashMap<String, Integer>();
             Connection conn = null;
             PreparedStatement ps = null;
             ResultSet rs = null;
             try {
                 conn = getConnection();
                 ps = conn.prepareStatement("SELECT * FROM items");
                 rs = ps.executeQuery();
                 while (rs.next()) {
                     items.put(rs.getString("name"), rs.getInt("itemid"));
                 }
             } catch (SQLException ex) {
                 log.log(Level.SEVERE, "Unable to retreive items from item table", ex);
             } finally {
                 try {
                     if (ps != null) {
                         ps.close();
                     }
                     if (rs != null) {
                         rs.close();
                     }
                     if (conn != null) {
                         conn.close();
                     }
                 } catch (SQLException ex) {
                 }
             }
         }
     }
 
     public void loadWhitelist() {
         synchronized (whiteListLock) {
             whiteList = new ArrayList<String>();
             Connection conn = null;
             PreparedStatement ps = null;
             ResultSet rs = null;
             try {
                 conn = getConnection();
                 ps = conn.prepareStatement("SELECT * FROM whitelist");
                 rs = ps.executeQuery();
                 while (rs.next()) {
                     whiteList.add(rs.getString(1));
                 }
             } catch (SQLException ex) {
                 log.log(Level.SEVERE, "Unable to retreive users from whitelist table", ex);
             } finally {
                 try {
                     if (ps != null) {
                         ps.close();
                     }
                     if (rs != null) {
                         rs.close();
                     }
                     if (conn != null) {
                         conn.close();
                     }
                 } catch (SQLException ex) {
                 }
             }
         }
     }
 
     public void loadReserveList() {
         synchronized (reserveListLock) {
             reserveList = new ArrayList<String>();
             Connection conn = null;
             PreparedStatement ps = null;
             ResultSet rs = null;
             try {
                 conn = getConnection();
                 ps = conn.prepareStatement("SELECT * FROM reservelist");
                 rs = ps.executeQuery();
                 while (rs.next()) {
                     reserveList.add(rs.getString(1));
                 }
             } catch (SQLException ex) {
                 log.log(Level.SEVERE, "Unable to retreive users from whitelist table", ex);
             } finally {
                 try {
                     if (ps != null) {
                         ps.close();
                     }
                     if (rs != null) {
                         rs.close();
                     }
                     if (conn != null) {
                         conn.close();
                     }
                 } catch (SQLException ex) {
                 }
             }
         }
     }
 
     //Users
     public void addPlayer(Player player) {
         Connection conn = null;
         PreparedStatement ps = null;
         ResultSet rs = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("INSERT INTO users (name, groups, prefix, commands, admin, canmodifyworld, ignoresrestrictions) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
             ps.setString(1, player.getName());
             ps.setString(2, id.combineSplit(0, player.getGroups(), ","));
             ps.setString(3, player.getPrefix());
             ps.setString(4, id.combineSplit(0, player.getCommands(), ","));
             ps.setBoolean(5, player.getAdmin());
             ps.setBoolean(6, player.canModifyWorld());
            ps.setBoolean(7, player.canIgnoreRestrictions());
             ps.executeUpdate();
 
             rs = ps.getGeneratedKeys();
             if (rs.next()) {
                 player.setSqlId(rs.getInt(1));
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to insert user into users table", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (rs != null) {
                     rs.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     public void modifyPlayer(Player player) {
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("UPDATE users SET groups = ?, prefix = ?, commands = ?, admin = ?, canmodifyworld = ?, ignoresrestrictions = ? WHERE id = ?");
             ps.setString(1, id.combineSplit(0, player.getGroups(), ","));
             ps.setString(2, player.getPrefix());
             ps.setString(3, id.combineSplit(0, player.getCommands(), ","));
             ps.setBoolean(4, player.getAdmin());
             ps.setBoolean(5, player.canModifyWorld());
            ps.setBoolean(6, player.canIgnoreRestrictions());
             ps.setInt(7, player.getSqlId());
             ps.executeUpdate();
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to update user in users table", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     public boolean doesPlayerExist(String player) {
         boolean exists = false;
         Connection conn = null;
         PreparedStatement ps = null;
         ResultSet rs = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
             ps.setString(1, player);
             rs = ps.executeQuery();
             if (rs.next()) {
                 exists = true;
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to check if user exists", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (rs != null) {
                     rs.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
         return exists;
     }
 
     //Groups
     public void addGroup(Group group) {
         throw new UnsupportedOperationException("Not supported yet.");
     }
 
     public void modifyGroup(Group group) {
         throw new UnsupportedOperationException("Not supported yet.");
     }
 
     //Kits
     public void addKit(Kit kit) {
         throw new UnsupportedOperationException("Not supported yet.");
     }
 
     public void modifyKit(Kit kit) {
         throw new UnsupportedOperationException("Not supported yet.");
     }
 
     //Homes
     public void addHome(Warp home) {
         Connection conn = null;
         PreparedStatement ps = null;
         ResultSet rs = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("INSERT INTO homes (name, x, y, z, rotX, rotY, `group`) VALUES(?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
             ps.setString(1, home.Name);
             ps.setDouble(2, home.Location.x);
             ps.setDouble(3, home.Location.y);
             ps.setDouble(4, home.Location.z);
             ps.setFloat(5, home.Location.rotX);
             ps.setFloat(6, home.Location.rotY);
             ps.setString(7, home.Group);
             ps.executeUpdate();
 
             rs = ps.getGeneratedKeys();
             if (rs.next()) {
                 home.ID = rs.getInt(1);
                 synchronized (homeLock) {
                     homes.add(home);
                 }
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to insert home into homes table", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (rs != null) {
                     rs.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     public void changeHome(Warp home) {
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("UPDATE homes SET x = ?, y = ?, z = ?, rotX = ?, rotY = ?, `group` = ? WHERE name = ?");
             ps.setDouble(1, home.Location.x);
             ps.setDouble(2, home.Location.y);
             ps.setDouble(3, home.Location.z);
             ps.setFloat(4, home.Location.rotX);
             ps.setFloat(5, home.Location.rotY);
             ps.setString(6, home.Group);
             ps.setString(7, home.Name);
             ps.executeUpdate();
 
             synchronized (homeLock) {
                 Warp toRem = null;
                 for (Warp h : homes) {
                     if (h.Name.equalsIgnoreCase(home.Name)) {
                         toRem = h;
                     }
                 }
                 if (toRem != null) {
                     homes.remove(toRem);
                 }
                 homes.add(home);
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to update home in homes table", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     //Warps
     public void addWarp(Warp warp) {
         Connection conn = null;
         PreparedStatement ps = null;
         ResultSet rs = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("INSERT INTO warps (name, x, y, z, rotX, rotY, `group`) VALUES(?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
             ps.setString(1, warp.Name);
             ps.setDouble(2, warp.Location.x);
             ps.setDouble(3, warp.Location.y);
             ps.setDouble(4, warp.Location.z);
             ps.setFloat(5, warp.Location.rotX);
             ps.setFloat(6, warp.Location.rotY);
             ps.setString(7, warp.Group);
             ps.executeUpdate();
 
             rs = ps.getGeneratedKeys();
             if (rs.next()) {
                 warp.ID = rs.getInt(1);
                 synchronized (warpLock) {
                     warps.add(warp);
                 }
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to insert warp into warps table", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (rs != null) {
                     rs.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     public void changeWarp(Warp warp) {
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("UPDATE warps SET x = ?, y = ?, z = ?, rotX = ?, rotY = ?, `group` = ? WHERE name = ?");
             ps.setDouble(1, warp.Location.x);
             ps.setDouble(2, warp.Location.y);
             ps.setDouble(3, warp.Location.z);
             ps.setFloat(4, warp.Location.rotX);
             ps.setFloat(5, warp.Location.rotY);
             ps.setString(6, warp.Group);
             ps.setString(7, warp.Name);
             ps.executeUpdate();
 
             synchronized (warpLock) {
                 Warp toRem = null;
                 for (Warp h : warps) {
                     if (h.Name.equalsIgnoreCase(warp.Name)) {
                         toRem = h;
                     }
                 }
                 if (toRem != null) {
                     warps.remove(toRem);
                 }
                 warps.add(warp);
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to update warp in warps table", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     public void removeWarp(Warp warp) {
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("DELETE FROM warps WHERE id = ?");
             ps.setDouble(1, warp.ID);
             ps.executeUpdate();
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to delete warp from warps table", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
         synchronized (warpLock) {
             warps.remove(warp);
         }
     }
 
     //Whitelist
     public void addToWhitelist(String name) {
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("INSERT INTO whitelist VALUES(?)");
             ps.setString(1, name);
             ps.executeUpdate();
             synchronized (whiteListLock) {
                 whiteList.add(name);
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to update whitelist", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     public void removeFromWhitelist(String name) {
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("DELETE FROM whitelist WHERE name = ?");
             ps.setString(1, name);
             ps.executeUpdate();
             synchronized (whiteListLock) {
                 whiteList.add(name);
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to update whitelist", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     //Reservelist
     public void addToReserveList(String name) {
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("INSERT INTO reservelist VALUES(?)");
             ps.setString(1, name);
             ps.executeUpdate();
             synchronized (reserveListLock) {
                 reserveList.add(name);
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to update reservelist", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     public void removeFromReserveList(String name) {
         Connection conn = null;
         PreparedStatement ps = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("DELETE FROM reservelist WHERE name = ?");
             ps.setString(1, name);
             ps.executeUpdate();
             synchronized (reserveListLock) {
                 reserveList.add(name);
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to update reservelist", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
     }
 
     public Player getPlayer(String name) {
         Player player = new Player();
         Connection conn = null;
         PreparedStatement ps = null;
         ResultSet rs = null;
         try {
             conn = getConnection();
             ps = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
             ps.setString(1, name);
             rs = ps.executeQuery();
             if (rs.next()) {
                 player.setSqlId(rs.getInt("id"));
                 player.setGroups(rs.getString("groups").split(","));
                 player.setCommands(rs.getString("commands").split(","));
                 player.setPrefix(rs.getString("prefix"));
                 player.setAdmin(rs.getBoolean("admin"));
                 player.setCanModifyWorld(rs.getBoolean("canmodifyworld"));
                 player.setIgnoreRestrictions(rs.getBoolean("ignoresrestrictions"));
                 player.setIps(rs.getString("ip").split(","));
             }
         } catch (SQLException ex) {
             log.log(Level.SEVERE, "Unable to retreive users from user table", ex);
         } finally {
             try {
                 if (ps != null) {
                     ps.close();
                 }
                 if (rs != null) {
                     rs.close();
                 }
                 if (conn != null) {
                     conn.close();
                 }
             } catch (SQLException ex) {
             }
         }
         return player;
     }
 }
