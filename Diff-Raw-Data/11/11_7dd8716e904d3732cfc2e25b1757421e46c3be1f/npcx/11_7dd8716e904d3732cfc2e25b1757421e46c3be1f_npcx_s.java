 package net.gamerservices.npcx;
 import org.bukkit.plugin.PluginManager;
 import java.util.HashMap;
 import org.bukkit.plugin.PluginDescriptionFile;
 import org.bukkit.plugin.java.JavaPlugin;
 import org.bukkit.event.player.PlayerChatEvent;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandSender;
 import org.bukkit.entity.LivingEntity;
 import org.bukkit.entity.Player;
 import org.bukkit.Location;
 import org.bukkit.Server;
 import org.bukkit.event.*;
 import org.bukkit.Material;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.entity.CreatureType;
 import java.util.Properties;
 import java.util.Random;
 import java.util.Timer;
 import java.util.logging.Level;
 import org.bukkit.event.Event.Type;
 import java.util.logging.Logger;
 import redecouverte.npcspawner.BasicHumanNpc;
 import redecouverte.npcspawner.BasicHumanNpcList;
 import redecouverte.npcspawner.NpcEntityTargetEvent;
 import redecouverte.npcspawner.NpcEntityTargetEvent.NpcTargetReason;
 import redecouverte.npcspawner.NpcSpawner;
 import org.bukkit.event.Event.Priority;
 
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.sql.*;
 
 public class npcx extends JavaPlugin {
 
 	private static final Logger logger = Logger.getLogger("Minecraft");
 	private final String FILE_PROPERTIES = "npcx.properties";
 	private final String PROP_DBHOST = "db-host";
 	private final String PROP_DBUSER = "db-user";
 	private final String PROP_DBPASS = "db-pass";
 	private final String PROP_DBNAME = "db-name";
 	private final String PROP_DBPORT = "db-port";
 	private final String PROP_WORLD = "world";
 	private final String PROP_UPDATE = "update";
 	private Connection conn = null;
 	private npcxEListener mEntityListener;
 	private npcxPListener mPlayerListener;
 	public HashMap<String, myPlayer> players = new HashMap<String, myPlayer>();
 	public HashMap<String, myNPC> npcs = new HashMap<String, myNPC>();
 	public HashMap<String, mySpawngroup> spawngroups = new HashMap<String, mySpawngroup>();
 	private Properties prop;
 	public BasicHumanNpcList npclist = new BasicHumanNpcList();
 	private String dsn;
 	private File propfile;
 	private File propfolder;
 	private String dbhost;
 	private String update;
 	private String dbuser;
 	private String dbpass;
 	private String dbname;
 	private String dbport;
 	private String world;
 	private Timer tick = new Timer();
 	
 	
 	
 	@Override
 	public void onLoad() {
 		// TODO Auto-generated method stub
 		propfolder = getDataFolder();
 		if (!propfolder.exists())
 		{
 			try
 			{
 				propfolder.mkdir();
 				System.out.println("npcx : config folder generation ended");
 			} catch (Exception e)
 			{
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 		}
 		
 		propfile = new File(propfolder.getAbsolutePath() + File.separator + FILE_PROPERTIES);
 		if (!propfile.exists())
 		{
 			try
 			{
 				propfile.createNewFile();
 				prop = new Properties();
 				prop.setProperty(PROP_DBHOST, "localhost");
 				prop.setProperty(PROP_DBUSER, "npcx");
 				prop.setProperty(PROP_DBPASS, "p4ssw0rd!");
 				prop.setProperty(PROP_DBNAME, "npcx");
 				prop.setProperty(PROP_DBPORT, "3306");
 				prop.setProperty(PROP_UPDATE, "true");
 				
 				prop.setProperty(PROP_WORLD, "world");
 				BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(propfile.getAbsolutePath()));
 				prop.store(stream, "Default generated settings, please ensure mysqld matches");
 				System.out.println("npcx : properties file generation ended");
 				
 			} catch(IOException e)
 			{
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 					
 			loadSettings();
 			System.out.println("npcx : initial setup ended");
 		
 		}
 			
 		loadSettings();
 		think();
 		
 	}
 	public void onNPCDeath(BasicHumanNpc npc)
 	{
 		
 		npc.parent.spawngroup.activecountdown = 100;
 		NpcSpawner.RemoveBasicHumanNpc(npc);
 		
 	}
 	
 	public void think()
 	{
 		tick.schedule(new Tick(this), 1 * 400);
 		// check npc logic
 		for (BasicHumanNpc npc : npclist.values())
 		{
 			npc.think();
 			
 		}
 		// check spawngroups
 		
 		for (mySpawngroup spawngroup : spawngroups.values())
 		{
 			
 			if (spawngroup.activecountdown > 0)
 			{
 				spawngroup.activecountdown--;
 				
 				if (spawngroup.activecountdown == 1)
 				{
 					spawngroup.active = false;					
 				}
 				
 			}
 
 			
 			if (!spawngroup.active)
 			{
 					
 				//System.out.println("npcx : found inactive spawngroup ("+ spawngroup.id +") with :[" + spawngroup.npcs.size() + "]");
 				int count = 0;
 				Random generator = new Random();
 				Object[] values = spawngroup.npcs.values().toArray();
 				
 				if (values.length > 0)
 				{
 				
 				myNPC npc = (myNPC) values[generator.nextInt(values.length)];
 				
 					try
 					{
 				
 					// is there at least one player in game?
 					if (this.getServer().getOnlinePlayers().length > 0)
 					{
 						if (!spawngroup.active)
 						{
 							//System.out.println("npcx : made spawngroup active");
 							BasicHumanNpc hnpc = NpcSpawner.SpawnBasicHumanNpc(npc.id, npc.name, this.getServer().getWorld(this.world), spawngroup.x, spawngroup.y, spawngroup.z,0 , 0);
 			                npc.npc = hnpc;
 			                npc.spawngroup = spawngroup;
 			                hnpc.parent = npc;
 							this.npclist.put(npc.id, hnpc);
 							spawngroup.active = true;
 						}
 					}
 					} catch (Exception e)
 					{
 						
 					}
 				}
 				
 			}
 		}
 		
 		
 	
 	}
 	
 	public void loadSettings()
 	{
 		// Loads configuration settings from the properties files
 		System.out.println("npcx : load settings begun");
 		
 		Properties config = new Properties();
 		BufferedInputStream stream;
 		// Access the defined properties file
 		try {
 			stream = new BufferedInputStream(new FileInputStream(propfolder.getAbsolutePath() + File.separator + FILE_PROPERTIES));
 			
 			try {
 				
 				// Load the configuration
 				config.load(stream);
 				dbhost = config.getProperty("db-host");
 				dbuser = config.getProperty("db-user");
 				dbpass = config.getProperty("db-pass");
 				dbname = config.getProperty("db-name");
 				dbport = config.getProperty("db-port");
 				world = config.getProperty("world");
 				this.world = world;
 				update = config.getProperty("update");
 				
 				dsn = "jdbc:mysql://" + dbhost + ":" + dbport + "/" + dbname;
 				System.out.println(dsn);
 				
 			} catch (IOException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 			
 		} catch (FileNotFoundException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
 		System.out.println("npcx : loadsettings() ended");
 	}
 	
 	@Override
 	public void onDisable() {
 		// TODO Auto-generated method stub
 		 try {
 	            PluginDescriptionFile pdfFile = this.getDescription();
 	            logger.log(Level.INFO, pdfFile.getName() + " version " + pdfFile.getVersion() + " disabled.");
 	        } catch (Exception e) {
 	            logger.log(Level.WARNING, "npcx : error: " + e.getMessage() + e.getStackTrace().toString());
 	            e.printStackTrace();
 	            return;
 	        }
 	}
 
 	public String dbGetNPCname(String string)
 	{
 		try
 		{
 			Class.forName ("com.mysql.jdbc.Driver").newInstance ();
 	        conn = DriverManager.getConnection (dsn, dbuser, dbpass);
 	        Statement s11 = conn.createStatement ();
 	        s11.executeQuery ("SELECT name FROM npc WHERE id ="+string);
 	        ResultSet rs11 = s11.getResultSet ();
 	        
 	        while (rs11.next ())
 	        {
 	        	String name = rs11.getString ("name");
 	        	return name;
 	        	
 	        }
 		} catch (Exception e)
 		{
 	        return "dummy";
 	
 		}
         return "dummy";
 	}
 	
 	@Override
 	public void onEnable() {
 		// TODO Auto-generated method stub
 		 try {	
 			 	System.out.println("npcx : registering monitored events");
 
 			 	PluginManager pm = getServer().getPluginManager();
 
 	            mEntityListener = new npcxEListener(this);
 	            mPlayerListener = new npcxPListener(this);
 	            pm.registerEvent(Type.ENTITY_TARGET, mEntityListener, Priority.Normal, this);
 	            pm.registerEvent(Type.ENTITY_DAMAGED, mEntityListener, Priority.Normal, this);
 	            pm.registerEvent(Type.PLAYER_JOIN, mPlayerListener, Priority.Normal, this);
 	            pm.registerEvent(Type.PLAYER_QUIT, mPlayerListener, Priority.Normal, this);
 	            pm.registerEvent(Type.PLAYER_CHAT, mPlayerListener, Priority.Normal, this);
 	            
 	            try 
 	            {
 	            
 			 	System.out.println("npcx : initialising database connection");
 			 	Class.forName ("com.mysql.jdbc.Driver").newInstance ();
 	            conn = DriverManager.getConnection (dsn, dbuser, dbpass);
 	            	            
 	            Properties config = new Properties();
 	    		BufferedInputStream stream = new BufferedInputStream(new FileInputStream(propfolder.getAbsolutePath() + File.separator + FILE_PROPERTIES));
 	            
 	            // Load the configuration
 				config.load(stream);
 				update = config.getProperty("update");
 	            
 	            if (update.matches("true"))
 	            {
 	            	System.out.println("npcx : DB WIPE");
 	            	
 		            /*
 		             * One time Database creation / TODO: Auto Upgrades
 		             * 
 		             * 
 		             * */
 		            Statement s2 = conn.createStatement ();
 		            String droptable = "DROP TABLE IF EXISTS npc; ";
 		            String npctable = "CREATE TABLE npc ( id INT UNSIGNED NOT NULL AUTO_INCREMENT, PRIMARY KEY (id),name CHAR(40),category CHAR(40))";
 		            s2.executeUpdate(droptable);
 		            s2.executeUpdate(npctable);
 		            
 		            String droptable0 = "DROP TABLE IF EXISTS pathgroup; ";
 		            String npctable0 = "CREATE TABLE pathgroup ( id INT UNSIGNED NOT NULL AUTO_INCREMENT, PRIMARY KEY (id),name CHAR(40),category CHAR(40))";
 		            s2.executeUpdate(droptable0);
 		            s2.executeUpdate(npctable0);
 		            
 		            String droptable1 = "DROP TABLE IF EXISTS pathgroup_entries; ";
 		            String npctable1 = "CREATE TABLE pathgroup_entries ( id INT UNSIGNED NOT NULL AUTO_INCREMENT, PRIMARY KEY (id),s int,pathgroup int, name CHAR(40),x CHAR(40),y CHAR(40),z CHAR(40))";
 		            s2.executeUpdate(droptable1);
 		            s2.executeUpdate(npctable1);
 		            
 		            String droptable2 = "DROP TABLE IF EXISTS spawngroup; ";
 		            String spawngrouptable = "CREATE TABLE spawngroup ( id INT UNSIGNED NOT NULL AUTO_INCREMENT, PRIMARY KEY (id),name CHAR(40),world CHAR(40),category CHAR(40),x CHAR(40), y CHAR(40), z CHAR(40))";
 		            s2.executeUpdate(droptable2);
 		            
 		            s2.executeUpdate(spawngrouptable);
 		            
 		            String droptable3 = "DROP TABLE IF EXISTS spawngroup_entries; ";
 		            String sgetable = "CREATE TABLE spawngroup_entries ( id INT UNSIGNED NOT NULL AUTO_INCREMENT, PRIMARY KEY (id),spawngroupid int,npcid int)";
 		            s2.executeUpdate(droptable3);
 		            
 		            s2.executeUpdate(sgetable);
 		            s2.close();
 		            System.out.println("npcx : finished table configuration");
 		            dbhost = config.getProperty("db-host");
 					dbuser = config.getProperty("db-user");
 					dbpass = config.getProperty("db-pass");
 					dbname = config.getProperty("db-name");
 					dbport = config.getProperty("db-port");
 					world = config.getProperty("world");
 					config.setProperty(PROP_DBHOST,dbhost);
 					config.setProperty(PROP_DBUSER,dbuser);
 					config.setProperty(PROP_DBPASS,dbpass);
 					config.setProperty(PROP_DBNAME,dbname);
 					config.setProperty(PROP_DBPORT,dbport);
 					config.setProperty(PROP_WORLD,world);
 		            config.setProperty(PROP_UPDATE,"false");
 		            File propfolder = getDataFolder();
 		            File propfile = new File(propfolder.getAbsolutePath() + File.separator + FILE_PROPERTIES);
 		            propfile.createNewFile();
 		            
 		            BufferedOutputStream stream1 = new BufferedOutputStream(new FileOutputStream(propfile.getAbsolutePath()));
 					config.store(stream1, "Default generated settings, please ensure mysqld matches");
 					
 	            }
 	            
 	            // Load Spawngroups
 	            
 	            
 	      	            
 	            Statement s1 = conn.createStatement ();
 	            s1.executeQuery ("SELECT id, name, category,x,y,z FROM spawngroup");
 	            ResultSet rs1 = s1.getResultSet ();
 	            int count1 = 0;
 	            System.out.println("npcx : loading spawngroups");
 	            while (rs1.next ())
 	            {
 	            	
 	            	// load spawngroup into cache
 	                int idVal = rs1.getInt ("id");
 	                String nameVal = rs1.getString ("name");
 	                String catVal = rs1.getString ("category");
 	                
 	                //BasicHumanNpc hnpc = NpcSpawner.SpawnBasicHumanNpc(args[2], args[2], player.getWorld(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
 	                mySpawngroup spawngroup = new mySpawngroup();
 	                spawngroup.name = nameVal;
 	                System.out.println("npcx : + " + nameVal);
 	                spawngroup.id = idVal;
 	                spawngroup.x = Double.parseDouble(rs1.getString ("x"));
 	                spawngroup.y = Double.parseDouble(rs1.getString ("y"));
 	                spawngroup.z = Double.parseDouble(rs1.getString ("z"));
 	                spawngroup.world = getServer().getWorld("world");
 	                this.spawngroups.put(Integer.toString(idVal), spawngroup);
 	                
 	                Statement s11 = conn.createStatement ();
 		            s11.executeQuery ("SELECT spawngroupid,npcid FROM spawngroup_entries WHERE spawngroupid ="+idVal);
 		            ResultSet rs11 = s11.getResultSet ();
 		            
 		            while (rs11.next ())
 		            {
 		            	myNPC npc = new myNPC();
 		            	npc.spawngroup = spawngroup;
 		            	npc.id = rs11.getString ("npcid");
 		            	npc.name = dbGetNPCname(npc.id);
 		            	System.out.println("npcx : + npc.name + " + rs11.getString ("npcid"));
 		            	
 		            	spawngroup.npcs.put(rs11.getString ("npcid"), npc);
 		            	
 		            }
 	                
 	               /* System.out.println (
 	                        "id = " + idVal
 	                        + ", name = " + nameVal
 	                        + ", category = " + catVal);*/
 	                ++count1;
 	                
 	                
 	            }
 	            rs1.close ();
 	            s1.close ();
 	            System.out.println (count1 + " spawngroups loaded");
 
 	            
 	            //this.HumanNPCList = new BasicHumanNpcList();
 			 	System.out.println("npcx : caching npcs");
 			 	
 	            } catch (Exception e)
 	            {
 	            	e.printStackTrace();
 	            }
 			 	
 	            
 	            
 	            PluginDescriptionFile pdfFile = this.getDescription();
 	            logger.log(Level.INFO, pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled.");
 	        } catch (Exception e) {
 	            logger.log(Level.WARNING, "npcx : error: " + e.getMessage() + e.getStackTrace().toString());
 	            e.printStackTrace();
 	            return;
 	        }
 	}
 	
 	@Override
     public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
 
         try {
 	           
             if (!command.getName().toLowerCase().equals("npcx")) {
             	
                 return false;
             }
             if (!(sender instanceof Player)) {
 
                 return false;
             }
 
             if (args.length < 1) {
                 return false;
             }
 
             String subCommand = args[0].toLowerCase();
         	//debug: logger.log(Level.WARNING, "npcx : " + command.getName().toLowerCase() + "(" + subCommand + ")");
 
             Player player = (Player) sender;
             Location l = player.getLocation();
             
             if (subCommand.equals("spawngroup"))
             {
             	
             	// Overview:
             	// A spawngroup is like a container. It contains many npcs and any one of them could spawn randomly.
             	// If you placed just one npc in the group only one npc would spawn. This allows you to create 'rare' npcs
             	
             	// Spawngroups need to be assigned to a location with 'spawngroup place' Once assigned that group
             	// will spawn in that location and remain stationary
             	
             	// If a path is assigned to the spawn group, the npc will follow the path continuously after spawning 
             	// at the location of 'spawngroup place'
             	
             	// todo: functionality
             	// creates a new spawngroup with name
             	// adds an npc to a spawngroup with a chance
             	// makes the spawngroup spawn at your location
             	// assigns a path to the spawngroup
             	
             	if (args.length < 2) {
             		player.sendMessage("Insufficient arguments /npcx spawngroup create|delete spawngroupname");
                 	player.sendMessage("Insufficient arguments /npcx spawngroup add groupid npcid");
                 	player.sendMessage("Insufficient arguments /npcx spawngroup place spawngroupname");
                 	player.sendMessage("Insufficient arguments /npcx spawngroup pathgroup pathgroupname");
                 	
                 	player.sendMessage("Insufficient arguments /npcx spawngroup list");
                 	return false;
             		
             		
             		
                 }
             	
             	if (args[1].equals("create")) {
             		if (args.length < 3) {
             			player.sendMessage("Insufficient arguments /npcx spawngroup create spawngroupname");
                     	
             		} else {
             			player.sendMessage("Created spawngroup: " + args[2]);
             			
             			Statement s2 = conn.createStatement ();
             			double x = player.getLocation().getX();
             			double y = player.getLocation().getY();
             			double z = player.getLocation().getZ();
             			
             			
             			String addspawngroup = "INSERT INTO spawngroup (name,x,y,z) VALUES ('" + args[2] + "','"+ x +"','"+ y +"','"+ z +"');";
             			Statement stmt = conn.createStatement();
             			stmt.execute(addspawngroup, Statement.RETURN_GENERATED_KEYS);
             			ResultSet keyset = stmt.getGeneratedKeys();
             			int key = 0;
             			if ( keyset.next() ) {
             			    // Retrieve the auto generated key(s).
             			    key = keyset.getInt(1);
             			    
             			}
         	            player.sendMessage("Spawngroup ["+ key + "] now active at your position");
             			mySpawngroup sg = new mySpawngroup();
             			sg.id = key;
             			sg.name = args[2];
             			sg.x = x;
             			sg.y = y;
             			sg.z = z;
             			sg.world = player.getWorld();
             			
             			this.spawngroups.put(Integer.toString(key),sg);
             			System.out.println("npcx : + cached new spawngroup("+ args[2] + ")");
         	            s2.close();
         	            
             		}
         			
         		}
             	
             	
             	if (args[1].equals("add")) {
             		if (args.length < 4) {
             			player.sendMessage("Insufficient arguments /npcx spawngroup add spawngroup npcid");
                     	
             		} else {
             			player.sendMessage("Added to spawngroup " + args[2] + "<"+ args[3]+ ".");
             			
             			// add to database
             			Statement s2 = conn.createStatement ();
             			String addspawngroup = "INSERT INTO spawngroup_entries (spawngroupid,npcid) VALUES ('" + args[2] + "','" + args[3] + "');";
             			s2.executeUpdate(addspawngroup);
         	            player.sendMessage("NPC ["+ args[3] + "] added to group ["+ args[2] + "]");
             			
         	            // add to cached spawngroup
         	            for (mySpawngroup sg : this.spawngroups.values())
         	            {
         	            	if (sg.id == Integer.parseInt(args[2]))
         	            	{
         	            		myNPC npc = new myNPC();
         	            		npc.name = dbGetNPCname(args[3]);
         	            		npc.spawngroup = sg;
         	            		npc.id = args[3];
         	            		System.out.println("npcx : + cached new spawngroup entry("+ args[3] + ")");
         	            		sg.npcs.put(args[3], npc);
         	            		
         	            	}
         	            }
         	            
         	            
         	            mySpawngroup sg = new mySpawngroup();
             			
             			// close db
         	            s2.close();
         	            
             		}
         			
         		}
             	
             	
             	if (args[1].equals("list")) {
             		player.sendMessage("Spawngroups:");
             		
             		Statement s = conn.createStatement ();
             		   s.executeQuery ("SELECT id, name, category FROM spawngroup");
             		   ResultSet rs = s.getResultSet ();
             		   int count = 0;
             		   while (rs.next ())
             		   {
             		       int idVal = rs.getInt ("id");
             		       String nameVal = rs.getString ("name");
             		       String catVal = rs.getString ("category");
             		       player.sendMessage(
             		               "id = " + idVal
             		               + ", name = " + nameVal
             		               + ", category = " + catVal);
             		       ++count;
             		   }
             		   rs.close ();
             		   s.close ();
             		   player.sendMessage (count + " rows were retrieved");
             		
             	
         			
         		}
         		
             }
             
             if (subCommand.equals("pathgroup"))
             {
             	if (args.length < 2) {
             		// todo: need to implement npc types here ie: 0 = default 1 = banker 2 = merchant
             		// todo: need to implement '/npcx npc edit' here
                 	player.sendMessage("Insufficient arguments /npcx pathgroup create|delete name");
 
                 	// todo needs to force the player to provide a search term to not spam them with lots of results in the event of a huge npc list
                 	player.sendMessage("Insufficient arguments /npcx pathgroup list");
                 	
                	
                     return false;
                 }
             	
             	if (args[1].equals("create")) {
             		if (args.length < 3) {
             			player.sendMessage("Insufficient arguments /npcx pathgroup create name");
                     	
             		} else {
             			player.sendMessage("Created pathgroup: " + args[2]);
                     	
             			Statement s2 = conn.createStatement ();
             			String addspawngroup = "INSERT INTO pathgroup (name) VALUES ('" + args[2] + "');";
         	            s2.executeUpdate(addspawngroup);
         	            
         	            s2.close();
         	            
             		}
         			
         		}
             }
             
             if (subCommand.equals("npc"))
             {
             	// Overview:
             	// NPCs are just that, definitions of the mob you want to appear in game. There can be multiple of the same
             	// npc in many spawngroups, for example if you wanted a custom npc called 'Thief' to spawn in several locations
             	// you would put the npc into many spawn groups
             	
             	// In the future these npcs will support npctypes which determines how the npc will respond to right click, attack, etc events
             	// ie for: bankers, normal npcs, merchants etc
             	
             	// Also loottables will be assignable
             	
             	// todo: functionality
             	// creates a new npc with name       
             	
             	
             	if (args.length < 2) {
             		// todo: need to implement npc types here ie: 0 = default 1 = banker 2 = merchant
             		// todo: need to implement '/npcx npc edit' here
                 	player.sendMessage("Insufficient arguments /npcx npc create|delete name");
 
                 	// todo needs to force the player to provide a search term to not spam them with lots of results in the event of a huge npc list
                 	player.sendMessage("Insufficient arguments /npcx npc list");
                 	
                 	// spawns the npc temporarily at your current spot for testing
                 	player.sendMessage("Insufficient arguments /npcx npc test name");
                 	
                     return false;
                 }
             	
             	if (args[1].equals("create")) {
             		if (args.length < 3) {
             			player.sendMessage("Insufficient arguments /npcx npc create npcname");
                     	
             		} else {
             			player.sendMessage("Created npc: " + args[2]);
                     	
             			Statement s2 = conn.createStatement ();
             			String addspawngroup = "INSERT INTO npc (name) VALUES ('" + args[2] + "');";
         	            s2.executeUpdate(addspawngroup);
         	            
         	            s2.close();
         	            
             		}
         			
         		}
             	
             	if (args[1].equals("list")) {
             		player.sendMessage("Npcs:");
             		
             		Statement s = conn.createStatement ();
             		   s.executeQuery ("SELECT id, name, category FROM npc");
             		   ResultSet rs = s.getResultSet ();
             		   int count = 0;
             		   while (rs.next ())
             		   {
             		       int idVal = rs.getInt ("id");
             		       String nameVal = rs.getString ("name");
             		       String catVal = rs.getString ("category");
             		       player.sendMessage(
             		               "id = " + idVal
             		               + ", name = " + nameVal
             		               + ", category = " + catVal);
             		       ++count;
             		   }
             		   rs.close ();
             		   s.close ();
             		   player.sendMessage (count + " rows were retrieved");
             		
             	
         			
         		}
             	
             	if (args[1].equals("spawn")) {
 	            		player.sendMessage("Spawning new NPC: " + args[2]);
 	                    // temporary
 	            		 BasicHumanNpc hnpc = NpcSpawner.SpawnBasicHumanNpc(args[2], args[2], player.getWorld(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
 		                 
 	            		 this.npclist.put(args[2], hnpc);
 		                
 	            		
 	            		try {
 		            		if (args.length < 4)
 		            		{
 		            			if (args[3].equals("1"))
 		            			{
 		            				
 		            				
 		            				
 		            				
 				                    ItemStack is = new ItemStack(Material.IRON_SWORD);
 				                    is.setAmount(1);
 				                    hnpc.getBukkitEntity().setItemInHand(is);
 				                    
 				                    /*
 				                    ItemStack ic = new ItemStack(Material.IRON_CHESTPLATE);
 				                    ic.setAmount(1);
 				                    hnpc.getBukkitEntity().getInventory().setChestplate(ic);
 
 				                    ItemStack ih = new ItemStack(Material.IRON_HELMET);
 				                    ih.setAmount(1);
 				                    hnpc.getBukkitEntity().getInventory().setHelmet(ih);
 				                    
 				                    ItemStack il = new ItemStack(Material.IRON_LEGGINGS);
 				                    il.setAmount(1);
 				                    hnpc.getBukkitEntity().getInventory().setLeggings(il);
 				                    
 				                    ItemStack ib = new ItemStack(Material.IRON_BOOTS);
 				                    ib.setAmount(1);
 				                    hnpc.getBukkitEntity().getInventory().setBoots(ib);
 				                    
 				                    */
 				                    
 		            			}
 		            			
 		            		}
 	            		} catch (Exception e)
 	            		{
 	            			
 		                    
 	            			
 	            		}
 	            		return true;
 	                   
                 }
 
             }
             
             if (subCommand.equals("pathgroup"))
             {
             	// Overview:
             	// Path groups are containers of path locations assigned to SpawnGroups. They are used to determine the route of an npc after it spawns
             	// When a path group is created, it is given several orderIDs beginning with 1, the npc will move from 
             	// point to point in the order of the orderID
             	
             	// In the future this will allow different types of pathgroups which will effect the way the npc traverses the order
             	// ie random, byID and circular
             	
             	// todo: functionality
             	// creates a new path group
             	// assigns a location to a path group
             	if (args.length < 3) {
                 	player.sendMessage("Insufficient arguments /npcx pathgroup create|delete name");
                 	player.sendMessage("Insufficient arguments /npcx pathgroup add pathgroupname orderinteger");
                     return false;
                 }
             }
 
             
             /*
              * 
              * For reference: NPC Spawner Code (thanks to the npc lib)
 
             // create npc-id npc-name
             if (subCommand.equals("create")) {
                 if (args.length < 3) {
                 	player.sendMessage("Insufficient arguments /npcx create id name ");
                     return false;
                 }
 
                 if (this.npclist.get(args[1]) != null) {
                     player.sendMessage("This npc-id is already in use.");
                     return true;
                 }
 
             	player.sendMessage("Creating npc....");
 
                 BasicHumanNpc hnpc = NpcSpawner.SpawnBasicHumanNpc(args[1], args[2], player.getWorld(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
                 this.npclist.put(args[1], hnpc);
                 
                 ItemStack is = new ItemStack(Material.BOOKSHELF);
                 is.setAmount(1);
                 hnpc.getBukkitEntity().setItemInHand(is);
 
 
             // attackme npc-id
             } else if (subCommand.equals("attackme")) {
 
                 if (args.length < 2) {
                     return false;
                 }
 
                 BasicHumanNpc npc = this.npclist.get(args[1]);
                 if (npc != null) {
                     npc.attackLivingEntity(player);
                     return true;
                 }
 
             // move npc-id
             } else if (subCommand.equals("move")) {
                 if (args.length < 2) {
                     return false;
                 }
 
                 BasicHumanNpc npc = this.npclist.get(args[1]);
                 if (npc != null) {
                     npc.moveTo(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
                     return true;
                 }
 
             // spawnpig
             } else if (subCommand.equals("spawnpig")) {
                 NpcSpawner.SpawnMob(MobType.PIG, player.getWorld(), l.getX(), l.getY(), l.getZ());
             }
             
             */
 
 
         } catch (Exception e) {
             sender.sendMessage("An error occured.");
             logger.log(Level.WARNING, "npcx: error: " + e.getMessage() + e.getStackTrace().toString());
             e.printStackTrace();
             return true;
         }
 
         return true;
     }
 
 }
