 package org.getspout.server;
 
 import java.io.File;
 import java.net.InetSocketAddress;
 import java.net.SocketAddress;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.UUID;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.getspout.api.ChatColor;
 import org.getspout.api.Server;
 import org.getspout.api.command.Command;
 import org.getspout.api.command.CommandException;
 import org.getspout.api.command.CommandRegistrationsFactory;
 import org.getspout.api.command.CommandSource;
 import org.getspout.api.command.RootCommand;
 import org.getspout.api.command.WrappedCommandException;
 import org.getspout.api.command.annotated.AnnotatedCommandRegistrationFactory;
 import org.getspout.api.command.annotated.SimpleAnnotatedCommandExecutorFactory;
 import org.getspout.api.command.annotated.SimpleInjector;
 import org.getspout.api.event.EventManager;
 import org.getspout.api.generator.WorldGenerator;
 import org.getspout.api.geo.World;
 import org.getspout.api.player.Player;
 import org.getspout.api.plugin.CommonPluginLoader;
 import org.getspout.api.plugin.CommonPluginManager;
 import org.getspout.api.plugin.Platform;
 import org.getspout.api.plugin.Plugin;
 import org.getspout.api.plugin.PluginManager;
 import org.getspout.api.plugin.security.CommonSecurityManager;
 import org.getspout.api.protocol.CommonPipelineFactory;
 import org.getspout.api.protocol.Session;
 import org.getspout.api.protocol.SessionRegistry;
 import org.getspout.server.command.AdministrationCommands;
 import org.getspout.server.io.StorageQueue;
 import org.getspout.server.net.SpoutSession;
 import org.getspout.server.net.SpoutSessionRegistry;
 import org.getspout.server.player.SpoutPlayer;
 import org.getspout.server.scheduler.SpoutScheduler;
 import org.getspout.server.util.thread.AsyncManager;
 import org.getspout.server.util.thread.ThreadAsyncExecutor;
 import org.getspout.server.util.thread.snapshotable.SnapshotManager;
 import org.getspout.server.util.thread.snapshotable.SnapshotableConcurrentLinkedHashMap;
 import org.getspout.server.util.thread.snapshotable.SnapshotableReference;
 import org.jboss.netty.bootstrap.ServerBootstrap;
 import org.jboss.netty.channel.Channel;
 import org.jboss.netty.channel.ChannelFactory;
 import org.jboss.netty.channel.ChannelPipelineFactory;
 import org.jboss.netty.channel.group.ChannelGroup;
 import org.jboss.netty.channel.group.DefaultChannelGroup;
 import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
 
 
 public class SpoutServer extends AsyncManager implements Server {
 	
 	private volatile int maxPlayers = 20;
 	
 	private volatile String primaryAddress;
 	
 	private volatile String[] allAddresses;
 	
 	private File pluginDirectory = new File("plugins");
 	
 	private File configDirectory = new File("config");
 	
 	private File updateDirectory = new File("update");
 	
 	private String logFile = "logs/log-%D";
 	
 	private String name = "Spout Server";
 	
 	private SnapshotManager snapshotManager = new SnapshotManager();
 	
 	/**
 	 * This list of players for the server
 	 */
 	private LinkedHashSet<SpoutPlayer> players = new LinkedHashSet<SpoutPlayer>();
 
 	/**
 	 * The security manager
 	 * TODO - need to integrate this
 	 */
 	private CommonSecurityManager securityManager = new CommonSecurityManager(0);
 	
 	/**
 	 * The plugin manager for the server
 	 */
 	private CommonPluginManager pluginManager = new CommonPluginManager(this, securityManager, 0.0);
 	
 	/**
 	* The console manager of this server.
 	*/
 	private final ConsoleManager consoleManager = new ConsoleManager(this, "jline");
 	
 	/**
 	 * The logger for this class.
 	 */
 	public static final Logger logger = Logger.getLogger("Minecraft");
 	
 	/**
 	 * A group containing all of the channels.
 	 */
 	private final ChannelGroup group = new DefaultChannelGroup();
 	
 	/**
 	 * The network executor service - Netty dispatches events to this thread
 	 * pool.
 	 */
 	private final ExecutorService executor = Executors.newCachedThreadPool();
 	
 	/**
 	 * A list of all the active {@link SpoutSession}s.
 	 */
 	private final SpoutSessionRegistry sessions = new SpoutSessionRegistry();
 	
 	public static final StorageQueue storeQueue = new StorageQueue();
 	
 	/**
 	 * The scheduler for the server
 	 */
 	private final SpoutScheduler scheduler = new SpoutScheduler(this);
 	
 	/**
 	 * If the server has a whitelist or not
 	 */
 	private volatile boolean whitelist = false;
 	
 	/**
 	 * loaded plugins
 	 */
	private Plugin[] plugins;
	
 	private SnapshotableConcurrentLinkedHashMap<String,SpoutWorld> loadedWorlds = new SnapshotableConcurrentLinkedHashMap<String, SpoutWorld>(snapshotManager, null);
 	
 	private SnapshotableReference<World> defaultWorld = new SnapshotableReference<World>(snapshotManager, null);
 	
 	/**
 	 * The root commnd for this server
 	 */
 	private final RootCommand rootCommand = new RootCommand(this);
 
 
 	public SpoutServer() {
 		super(1, new ThreadAsyncExecutor());
 		registerWithScheduler(scheduler);
 		init();
 		if (!getExecutor().startExecutor()) {
 			throw new IllegalStateException("SpoutServer's executor was already started");
 		}
 	}
 	
 	public static void main(String[] args) {
		//org.getspout.unchecked.server.SpoutServer.main(args);
		
 		SpoutServer server = new SpoutServer();
 		server.start();
 		
 		server.bind(new InetSocketAddress("localhost", 25565));
 		
 	}
 	
 	public void start() {
 
 		CommandRegistrationsFactory<Class<?>> commandRegFactory =
 				new AnnotatedCommandRegistrationFactory(new SimpleInjector(this),
 						new SimpleAnnotatedCommandExecutorFactory());
 
 		// Register commands
 		getRootCommand().addSubCommands(this, AdministrationCommands.class, commandRegFactory);
 
 		consoleManager.setupConsole();
 		
 		// Start loading plugins
 		loadPlugins();
 		enablePlugins();
 		scheduler.startMainThread();
 		
 
 	}
 	
 	
 
 	public void init() {
 		ChannelFactory factory = new NioServerSocketChannelFactory(executor, executor);
 		bootstrap.setFactory(factory);
 
 		ChannelPipelineFactory pipelineFactory = new CommonPipelineFactory(this);
 		bootstrap.setPipelineFactory(pipelineFactory);
 	}
 	
 	public void loadPlugins() {
 		
 		pluginManager.registerPluginLoader(CommonPluginLoader.class);
 		
 		pluginManager.clearPlugins();
 		
 		if (!pluginDirectory.exists()) 
 			pluginDirectory.mkdirs();
 		
		plugins = pluginManager.loadPlugins(pluginDirectory);
 		
 		for (Plugin plugin : plugins) {
 			try {
 				//Technically unsafe.  This should call the security manager
 				plugin.onLoad();
 			} catch (Exception ex) {
 				logger.log(Level.SEVERE, "Error loading {0}: {1}", new Object[] {plugin.getDescription().getName(), ex.getMessage()});
 				ex.printStackTrace();
 			}
 		}
 	}
 	
 	private void enablePlugins(){
		for(Plugin plugin : plugins){
 			pluginManager.enablePlugin(plugin);
 		}
 		
 	}
 
 	
 	/**
 	 * The {@link ServerBootstrap} used to initialize Netty.
 	 */
 	private final ServerBootstrap bootstrap = new ServerBootstrap();
 	
 	/**
 	 * Binds this server to the specified address.
 	 *
 	 * @param address The addresss.
 	 */
 	public void bind(SocketAddress address) {
 		logger.log(Level.INFO, "Binding to address: {0}...", address);
 		group.add(bootstrap.bind(address));
 	}
 
 	@Override
 	public String getName() {
 		return name;
 	}
 
 	@Override
 	public String getVersion() {
 		return getClass().getPackage().getImplementationVersion();
 	}
 
 	@Override
 	public int getMaxPlayers() {
 		return maxPlayers;
 	}
 
 	@Override
 	public String getAddress() {
 		return primaryAddress;
 	}
 	
 	@Override
 	public String[] getAllAddresses() {
 		return allAddresses;
 	}
 
 	@Override
 	public void broadcastMessage(String message) {
 		for (Player player : getOnlinePlayers()) {
 			player.sendMessage(message);
 		}
 		consoleManager.getCommandSource().sendMessage(message);
 	}
 
 	@Override
 	public PluginManager getPluginManager() {
 		return pluginManager;
 	}
 
 	@Override
 	public Logger getLogger() {
 		return logger;
 	}
 
 	@Override
 	public void processCommand(CommandSource source, String commandLine) {
 		try {
 			getRootCommand().execute(source, commandLine.split(" "), -1, false);
 		} catch (WrappedCommandException e) {
 			source.sendMessage(ChatColor.RED + "Internal error executing command!");
 			source.sendMessage(ChatColor.RED + "Error: " + e.getMessage() + "; See console for details.");
 			e.printStackTrace();
 		} catch (CommandException e) {
 			// TODO: Better exception handling!
 			source.sendMessage(ChatColor.RED + e.getMessage());
 		}
 	}
 
 	@Override
 	public File getUpdateFolder() {
 		if(!updateDirectory.exists()) updateDirectory.mkdirs();
 		return updateDirectory;
 	}
 
 	@Override
 	public Collection<Player> matchPlayer(String name) {
 		List<Player> result = new ArrayList<Player>();
 		for (Player player : getOnlinePlayers()) {
 			if (player.getName().startsWith(name)) {
 				result.add(player);
 			}
 		}
 		return result;
 	}
 
 	@Override
 	public World getWorld(String name) {
 		return loadedWorlds.getValue(name);
 	}
 
 	@Override
 	public World getWorld(UUID uid) {
 		for(SpoutWorld world : loadedWorlds.getValues()){
 			if(world.getUID().equals(uid)) return world;
 		}
 		return null;
 	}
 
 	@Override
 	public Collection<World> getWorlds() {		
 		Collection<World> w = new ArrayList<World>();
 		for(SpoutWorld world : loadedWorlds.getValues()){
 			w.add(world);
 		}
 		return w;
 	}
 
 	@Override
 	public void save(boolean worlds, boolean players) {
 		// TODO Auto-generated method stub
 		
 	}
 
 /*	@Override
 	public boolean registerRecipe(Recipe recipe) {
 		// TODO Auto-generated method stub
 		return false;
 	}*/
 
 	@Override
 	public int getSpawnProtectRadius() {
 		// TODO Auto-generated method stub
 		return 0;
 	}
 
 	@Override
 	public void setSpawnProtectRadius(int radius) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public boolean allowFlight() {
 		// TODO Auto-generated method stub
 		return false;
 	}
 
 	@Override
 	public File getWorldFolder() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public Command getRootCommand() {
 		return rootCommand;
 	}
 
 	@Override
 	public EventManager getEventManager() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public Platform getPlatform() {		
 		return Platform.SERVER;
 	}
 
 	@Override
 	public boolean isWhitelist() {
 		return whitelist;
 	}
 
 	@Override
 	public void setWhitelist(boolean whitelist) {
 		this.whitelist = whitelist;		
 	}
 
 	@Override
 	public void updateWhitelist() {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public String[] getWhitelistedPlayers() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public boolean unloadWorld(String name, boolean save) {		
 		return unloadWorld(loadedWorlds.getValue(name), save);
 	}
 
 	@Override
 	public boolean unloadWorld(World world, boolean save) {
 		if (world == null) {
 			return false;
 		} else {
 			boolean success = loadedWorlds.remove(world.getName(), (SpoutWorld)world);
 			if (success) {
 				if(save){
 					SpoutWorld w = (SpoutWorld) world;
 					if (!w.getExecutor().haltExecutor()) {
 						throw new IllegalStateException("Executor was already halted when halting was attempted");
 					}
 					//TODO Save the world, save the cheerleader
 				}
 				//Note: Worlds should not allow being saved twice and/or throw exceptions if accessed after unloading
 				//      Also, should blank out as much internal world data as possible, in case plugins retain references to unloaded worlds
 			}
 			return success;
 		}
 	}
 	
 	@Override
 	public World loadWorld(String name, WorldGenerator generator) {
 		// TODO - should include generator (and non-zero seed)
 		SpoutWorld world = new SpoutWorld(name, this, 0);
 		
 		World oldWorld = loadedWorlds.putIfAbsent(name, world);
 		
 		if (oldWorld != null) {
 			return oldWorld;
 		} else {
 			if (!world.getExecutor().startExecutor()) {
 				throw new IllegalStateException("Unable to start executor for new world");
 			}
 			return world;
 		}
 	}
 	
 	@Override
 	public boolean setDefaultWorld(World world) {
 		if (world == null) {
 			return false;
 		} else {
 			defaultWorld.set(world);
 			return true;
 		}
 	}
 
 	@Override
 	public World getDefaultWorld() {
 		World d = defaultWorld.get();
 		if (d == null || !loadedWorlds.get().containsKey(d.getName())) {
 			Map<String, SpoutWorld> l = loadedWorlds.get();
 			if (l.size() == 0) {
 				return null;
 			} else {
 				World first = l.values().iterator().next();
 				return first;
 			}
 		} else {
 			return d;
 		}
 	}
 
 	@Override
 	public boolean isOnlineMode() {
 		// TODO Auto-generated method stub
 		return false;
 	}
 
 	@Override
 	public Collection<String> getIPBans() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public void ban(String address) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public void unban(String address) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public List<String> getAllPlayers() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public Player[] getOnlinePlayers() {
 		return players.toArray(new Player[players.size()]);
 	}
 
 	@Override
 	public Player getPlayer(String name, boolean exact) {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public Collection<Player> getBannedPlayers() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public Collection<Player> getOps() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 
 	@Override
 	public Session newSession(Channel channel) {
 		return new SpoutSession(this, channel);
 	}
 
 	@Override
 	public ChannelGroup getChannelGroup() {
 		return group;
 	}
 
 	@Override
 	public SessionRegistry getSessionRegistry() {
 		return sessions;
 	}
 
 	@Override
 	public File getConfigDirectory() {
 		return configDirectory;
 	}
 
 	@Override
 	public String getLogFile() {
 		return logFile;
 	}
 	
 	@Override
 	public String[] getAllCommands() {
 		Set<String> result = getRootCommand().getChildNames();
 		return result.toArray(new String[result.size()]);
 	}
 
 	@Override
 	public void stop() {
 		group.close();
 		bootstrap.getFactory().releaseExternalResources();
 
 		// And finally kill the console
 		consoleManager.stop();
 		scheduler.stop();
 	}
 
 	@Override
 	public void copySnapshotRun() throws InterruptedException {
 		snapshotManager.copyAllSnapshots();
 	}
 
 	@Override
 	public void startTickRun(int stage, long delta) throws InterruptedException {
 	}
 	
 	@Override
 	public void haltRun() throws InterruptedException {
 		logger.info("Server halting");
 	}
 	
 	public SpoutScheduler getScheduler() {
 		return scheduler;
 	}
 
 	@Override
 	public File getConfigFolder() {
 		if(!configDirectory.exists()) configDirectory.mkdirs();
 		return configDirectory;
 	}
 
 }
