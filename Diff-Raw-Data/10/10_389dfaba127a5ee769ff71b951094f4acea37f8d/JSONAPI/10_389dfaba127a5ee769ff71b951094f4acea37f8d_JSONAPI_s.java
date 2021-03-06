 package com.ramblingwood.minecraft.jsonapi;
 
 import java.io.BufferedReader;
 import java.io.DataInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.UnsupportedEncodingException;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.ArrayList;
 import java.util.Hashtable;
 import java.util.logging.FileHandler;
 import java.util.logging.Handler;
 import java.util.logging.Logger;
 import java.util.logging.SimpleFormatter;
 
 import org.bukkit.ChatColor;
 import org.bukkit.Server;
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandSender;
 import org.bukkit.command.ConsoleCommandSender;
 import org.bukkit.event.Event;
 import org.bukkit.event.Event.Priority;
 import org.bukkit.event.player.PlayerChatEvent;
 import org.bukkit.event.player.PlayerJoinEvent;
 import org.bukkit.event.player.PlayerListener;
 import org.bukkit.event.player.PlayerQuitEvent;
 import org.bukkit.plugin.PluginDescriptionFile;
 import org.bukkit.plugin.PluginLoader;
 import org.bukkit.plugin.PluginManager;
 import org.bukkit.plugin.java.JavaPlugin;
 
 import com.ramblingwood.minecraft.jsonapi.streams.ConsoleHandler;
 
 
 /**
 *
 * @author alecgorge
 */
 public class JSONAPI extends JavaPlugin  {
 	public PluginLoader pluginLoader;
 	// private Server server;
 	public JSONServer jsonServer;
 	public JSONSocketServer jsonSocketServer;
 	public JSONWebSocketServer jsonWebSocketServer;
 	
 	public boolean logging = false;
 	public String logFile = "false";
 	public String salt = "";
 	public int port = 20059;
 	public ArrayList<String> whitelist = new ArrayList<String>();
 	public ArrayList<String> method_noauth_whitelist = new ArrayList<String>();
 	
 	private Logger log = Logger.getLogger("Minecraft");
 	private Logger outLog = Logger.getLogger("JSONAPI");
 	private PluginManager pm;
 	private Handler handler;
 	
 	// for dynamic access
 	public static JSONAPI instance;
 
 	
 	protected void initalize(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
 		this.pluginLoader = pluginLoader;
 		// server = instance;
 	}
 	
 	public JSONAPI () {
 		super();
 		JSONAPI.instance = this;		
 	}
 	
 	public JSONServer getJSONServer () {
 		return jsonServer;
 	}
 	
 	public void registerMethod(String method) {
 		getJSONServer().getCaller().loadString("["+method+"]");
 	}
 	
 	public void registerMethods(String method) {
 		getJSONServer().getCaller().loadString(method);
 	}
 	
 	private JSONAPIPlayerListener l = new JSONAPIPlayerListener(this);	
 
 	public void onEnable() {
 		try {
 			Hashtable<String, String> auth = new Hashtable<String, String>();
 			
 			if(getDataFolder().exists()) {
 				getDataFolder().createNewFile();
 			}
 			// System.out.println(getDataFolder().getAbsolutePath()+"\\JSONAPI.properties");
 			PropertiesFile options = new PropertiesFile(new File(getDataFolder().getAbsolutePath()+File.separator+"JSONAPI.properties").getAbsolutePath());
 			logging = options.getBoolean("log-to-console", true);
 			logFile = options.getString("log-to-file", "false");
 			String ipWhitelist = options.getString("ip-whitelist", "false");
 			salt = options.getString("salt", "");
 			
 			String reconstituted = "";
 			if(!ipWhitelist.trim().equals("false")) {
 				String[] ips = ipWhitelist.split(",");
 				StringBuffer t = new StringBuffer();
 				for(String ip : ips) {
 					t.append(ip.trim()+",");
 					whitelist.add(ip);
 				}
 				reconstituted = t.toString();
 			}
 			
 			outLog = Logger.getLogger("JSONAPI");
 			if(!logging) {
 				for(Handler h : outLog.getHandlers()) {
 					outLog.removeHandler(h);
 				}
 			}
 			if(!logFile.equals("false")) {
 				FileHandler fh = new FileHandler(logFile);
 				fh.setFormatter(new SimpleFormatter());
 				outLog.addHandler(fh);
 			}
 			
 			port = options.getInt("port", 20059);
 
 			try {
 				FileInputStream fstream;
 				File authfile = new File(getDataFolder().getAbsolutePath()+File.separator+"JSONAPIAuthentication.txt");
 				try {
 					fstream = new FileInputStream(authfile);
 				}
 				catch (FileNotFoundException e) {
 					authfile.createNewFile();
 					fstream = new FileInputStream(authfile);
 				}
 
 				DataInputStream in = new DataInputStream(fstream);
 				BufferedReader br = new BufferedReader(new InputStreamReader(in));
 				String line;
 				
 				while ((line = br.readLine()) != null)   {
 					if(!line.startsWith("#")) {
 						String[] parts = line.trim().split(":");
 						if(parts.length == 2) {
 							auth.put(parts[0], parts[1]);
 						}
 					}
 				}
 				
 				br.close();
 				in.close();
 			} catch (Exception e) {
 				e.printStackTrace();
 			}
 			
 			try {
 				FileInputStream fstream;
 				File authfile = new File(getDataFolder().getAbsolutePath()+File.separator+"JSONAPIMethodNoAuthWhitelist.txt");
 				try {
 					fstream = new FileInputStream(authfile);
 				}
 				catch (FileNotFoundException e) {
 					authfile.createNewFile();
 					fstream = new FileInputStream(authfile);
 				}
 
 				DataInputStream in = new DataInputStream(fstream);
 				BufferedReader br = new BufferedReader(new InputStreamReader(in));
 				String line;
 				
 				while ((line = br.readLine()) != null)   {
 					if(!line.trim().startsWith("#")) {
 						method_noauth_whitelist.add(line.trim());
 					}
 				}
 				
 				br.close();
 				in.close();
 			} catch (Exception e) {
 				e.printStackTrace();
 			}
 			
 			
 			if(auth.size() == 0) {
 				log.severe("[JSONAPI] No valid logins for JSONAPI. Check JSONAPIAuthentication.txt");
 				return;
 			}
 			
 			log.info("[JSONAPI] Logging to file: "+logFile);
 			log.info("[JSONAPI] Logging to console: "+String.valueOf(logging));
 			log.info("[JSONAPI] IP Whitelist = "+(reconstituted.equals("") ? "None, all requests are allowed." : reconstituted));
 
 			jsonServer = new JSONServer(auth, this);
 			
 			// add console stream support
 			handler = new ConsoleHandler(jsonServer);
 			log.addHandler(handler);
 
 			jsonSocketServer = new JSONSocketServer(port + 1, jsonServer);
 			jsonWebSocketServer = new JSONWebSocketServer(port + 2, jsonServer);
 			jsonWebSocketServer.start();
 			
 			initialiseListeners();
 		}
 		catch( IOException ioe ) {
 			log.severe( "[JSONAPI] Couldn't start server!\n");
 			ioe.printStackTrace();
 			//System.exit( -1 );
 		}		
 	}
 	
 	@Override
 	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
 		if (sender instanceof ConsoleCommandSender) {
 			if (cmd.getName().equals("reloadjsonapi")) {
 				if (sender instanceof ConsoleCommandSender) {
 					log.info("Reloading JSONAPI");
 					onDisable();
 					onEnable();
 				}
 				return true;
 			}
 			else if (cmd.getName().equals("jsonapi-list")) {
 				if (sender instanceof ConsoleCommandSender) {
 					for(String key : jsonServer.getCaller().methods.keySet()) {
 						StringBuilder sb = new StringBuilder((key.trim().equals("") ? "Default Namespace" : key.trim()) +": ");
 						for(String m : jsonServer.getCaller().methods.get(key).keySet()) {
 							sb.append(jsonServer.getCaller().methods.get(key).get(m).getName()).append(", ");
 						}
 						sender.sendMessage(sb.substring(0, sb.length()-2).toString()+"\n");
 					}
 					
 				}
 				return true;
 			}
         }
 		return false;
 	}
 	
 	
 	@Override
 	public void onDisable(){
 		if(jsonServer != null) {
			jsonServer.stop();
			jsonSocketServer.stop();
			jsonWebSocketServer.stop();
 			log.removeHandler(handler);
 		}
 	}
 	
 	private void initialiseListeners() {
 		pm = getServer().getPluginManager();
 			
 		pm.registerEvent(Event.Type.PLAYER_CHAT, l, Priority.Normal, this);
 		// 	pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, l, Priority.Normal, this);
 		pm.registerEvent(Event.Type.PLAYER_QUIT, l, Priority.Normal, this);
 		pm.registerEvent(Event.Type.PLAYER_JOIN, l, Priority.Normal, this);
 	}
 	
 	/**
 	 * From a password, a number of iterations and a salt,
 	 * returns the corresponding digest
 	 * @param iterationNb int The number of iterations of the algorithm
 	 * @param password String The password to encrypt
 	 * @param salt byte[] The salt
 	 * @return byte[] The digested password
 	 * @throws NoSuchAlgorithmException If the algorithm doesn't exist
 	 */
 	public static String SHA256(String password) throws NoSuchAlgorithmException {
 		MessageDigest digest = MessageDigest.getInstance("SHA-256");
 		digest.reset();
 		byte[] input = null;
 		try {
 			input = digest.digest(password.getBytes("UTF-8"));
 			StringBuffer hexString = new StringBuffer();
 			for(int i = 0; i< input.length; i++) {
 				String hex = Integer.toHexString(0xFF & input[i]);
 				if (hex.length() == 1) {
 					hexString.append('0');
 				}
 				hexString.append(hex);
 			}
 			return hexString.toString();
 		} catch (UnsupportedEncodingException e) {
 			e.printStackTrace();
 		}
 		return "UnsupportedEncodingException";
 	}
 	
 	public void disable() {
 		jsonServer.stop();
 	}
 	
 	public static class JSONAPIPlayerListener extends PlayerListener {
 		JSONAPI p;
 		
 		// This controls the accessibility of functions / variables from the main class.
 		public JSONAPIPlayerListener(JSONAPI plugin) {
 			p = plugin;
 		}
 		
 		public void onPlayerChat(PlayerChatEvent event) {
 			p.jsonServer.logChat(event.getPlayer().getName(),event.getMessage());			
 		}
 		
 		public void onPlayerJoin(PlayerJoinEvent event) {
 			p.jsonServer.logConnected(event.getPlayer().getName());
 		}
 
 		public void onPlayerQuit(PlayerQuitEvent event) {
 			p.jsonServer.logDisconnected(event.getPlayer().getName());
 		}		
 	}
 }
