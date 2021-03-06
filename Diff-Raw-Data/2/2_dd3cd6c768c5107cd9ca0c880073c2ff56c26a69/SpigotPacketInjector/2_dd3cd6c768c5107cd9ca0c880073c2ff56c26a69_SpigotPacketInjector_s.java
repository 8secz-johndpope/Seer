 package com.comphenix.protocol.injector.spigot;
 
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.Collections;
 import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 
 import org.bukkit.Bukkit;
 import org.bukkit.Server;
 import org.bukkit.entity.Player;
 import org.bukkit.plugin.Plugin;
 
 import net.sf.cglib.proxy.Callback;
 import net.sf.cglib.proxy.CallbackFilter;
 import net.sf.cglib.proxy.Enhancer;
 import net.sf.cglib.proxy.MethodInterceptor;
 import net.sf.cglib.proxy.MethodProxy;
 import net.sf.cglib.proxy.NoOp;
 
 import com.comphenix.protocol.Packets;
 import com.comphenix.protocol.concurrency.IntegerSet;
 import com.comphenix.protocol.error.ErrorReporter;
 import com.comphenix.protocol.events.PacketContainer;
 import com.comphenix.protocol.events.PacketEvent;
 import com.comphenix.protocol.injector.ListenerInvoker;
 import com.comphenix.protocol.injector.PlayerLoggedOutException;
 import com.comphenix.protocol.injector.packet.PacketInjector;
 import com.comphenix.protocol.injector.player.NetworkObjectInjector;
 import com.comphenix.protocol.injector.player.PlayerInjectionHandler;
 import com.comphenix.protocol.reflect.FuzzyReflection;
 import com.comphenix.protocol.reflect.MethodInfo;
 import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
 import com.comphenix.protocol.utility.MinecraftReflection;
 import com.google.common.collect.MapMaker;
 
 /**
  * Offload all the work to Spigot, if possible. 
  * 
  * @author Kristian
  */
 public class SpigotPacketInjector implements SpigotPacketListener {
 	// Lazily retrieve the spigot listener class
 	private static volatile Class<?> spigotListenerClass;
 	private static volatile boolean classChecked;
 	
 	// Retrieve the entity player from a PlayerConnection
 	private static Field playerConnectionPlayer;
 	
 	// Packets that are not to be processed by the filters
 	private Set<Object> ignoredPackets = Collections.newSetFromMap(new MapMaker().weakKeys().<Object, Boolean>makeMap());
 	
 	/**
 	 * The amount of ticks to wait before removing all traces of a player.
 	 */
 	private static final int CLEANUP_DELAY = 100;
 	
 	/**
 	 * Retrieve the spigot packet listener class.
 	 * @return The listener class.
 	 */
 	private static Class<?> getSpigotListenerClass() {
 		if (!classChecked) {
 			try {
 				spigotListenerClass = SpigotPacketInjector.class.getClassLoader().loadClass("org.spigotmc.netty.PacketListener");
 			} catch (ClassNotFoundException e) {
 				return null;
 			} finally {
 				// We've given it a try now
 				classChecked = true;
 			}
 		}
 		return spigotListenerClass;
 	}
 	
 	/**
 	 * Retrieve the register packet listener method.
 	 * @return The method used to register a packet listener.
 	 */
 	private static Method getRegisterMethod() {
 		Class<?> clazz = getSpigotListenerClass();
 		
 		if (clazz != null) {
 			try {
 				return clazz.getMethod("register", clazz, Plugin.class);
 			} catch (SecurityException e) {
 				// If this happens, then ... we're doomed
 				throw new RuntimeException("Reflection is not allowed.", e);
 			} catch (NoSuchMethodException e) {
 				throw new IllegalStateException("Cannot find register() method in " + clazz, e);
 			}
 		}
 		
 		// Also bad
 		throw new IllegalStateException("Spigot could not be found!");
 	}
 	
 	/**
 	 * Determine if there is a Spigot packet listener.
 	 * @return Spigot packet listener.
 	 */
 	public static boolean canUseSpigotListener() {
 		return getSpigotListenerClass() != null;
 	}
 	
 	// The listener we will register on Spigot.
 	// Unfortunately, due to the use of PlayerConnection, INetworkManager and Packet, we're
 	// unable to reference it directly. But with CGLib, it shouldn't cost us much.
 	private Object dynamicListener;
 	
 	// Reference to ProtocolLib
 	private Plugin plugin;
 	
 	// Different sending filters
 	private IntegerSet queuedFilters;
 	private IntegerSet reveivedFilters;
 
 	// NetworkManager to injector and player
 	private ConcurrentMap<Object, NetworkObjectInjector> networkManagerInjector = new ConcurrentHashMap<Object, NetworkObjectInjector>();
 	
 	// Player to injector
 	private ConcurrentMap<Player, NetworkObjectInjector> playerInjector = new ConcurrentHashMap<Player, NetworkObjectInjector>();
 	
 	// Responsible for informing the PL packet listeners
 	private ListenerInvoker invoker;
 	private ErrorReporter reporter;
 	private Server server;
 	private ClassLoader classLoader;
 	
 	/**
 	 * Create a new spigot injector.
 	 */
 	public SpigotPacketInjector(ClassLoader classLoader, ErrorReporter reporter, ListenerInvoker invoker, Server server) {
 		this.classLoader = classLoader;
 		this.reporter = reporter;
 		this.invoker = invoker;
 		this.server = server;
 		this.queuedFilters = new IntegerSet(Packets.MAXIMUM_PACKET_ID + 1);
 		this.reveivedFilters = new IntegerSet(Packets.MAXIMUM_PACKET_ID + 1);
 	}
 	
 	/**
 	 * Register the Spigot packet injector.
 	 * @param plugin - the parent plugin.
 	 * @return TRUE if we registered the plugin, FALSE otherwise.
 	 */
 	public boolean register(Plugin plugin) {
 		if (hasRegistered())
 			return false;
 		
 		// Save the plugin too
 		this.plugin = plugin;
 		
 		final Callback[] callbacks = new Callback[3];
 		final boolean[] found = new boolean[3];
 		
 		// Packets received from the clients
 		callbacks[0] = new MethodInterceptor() {
 			@Override
 			public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
 				return SpigotPacketInjector.this.packetReceived(args[0], args[1], args[2]);
 			}
 		};
 		// Packet sent/queued
 		callbacks[1] = new MethodInterceptor() {
 			@Override
 			public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
 				return SpigotPacketInjector.this.packetQueued(args[0], args[1], args[2]);
 			}
 		};
 		
 		// Don't care for everything else
 		callbacks[2] = NoOp.INSTANCE;
 		
 		Enhancer enhancer = new Enhancer();
 		enhancer.setClassLoader(classLoader);
 		enhancer.setSuperclass(getSpigotListenerClass());
 		enhancer.setCallbacks(callbacks);
 		enhancer.setCallbackFilter(new CallbackFilter() {
 			@Override
 			public int accept(Method method) {
 				// We'll be pretty stringent
 				if (matchMethod("packetReceived", method)) {
 					found[0] = true;
 					return 0;
 				} else if (matchMethod("packetQueued", method)) {
 					found[1] = true;
 					return 1;
 				} else {
 					found[2] = true;
 					return 2;
 				}
 			}
 		});
 		dynamicListener = enhancer.create();
 		
 		// Verify methods
 		if (!found[0])
 			throw new IllegalStateException("Unable to find a valid packet receiver in Spigot.");
 		if (!found[1])
 			throw new IllegalStateException("Unable to find a valid packet queue in Spigot.");
 		
 		// Lets register it too
 		try {
 			getRegisterMethod().invoke(null, dynamicListener, plugin);
 		} catch (Exception e) {
 			throw new RuntimeException("Cannot register Spigot packet listener.", e);
 		}
 		
 		// If we succeed
 		return true;
 	}
 	
 	/**
 	 * Determine if the given method is a valid packet receiver or queued method.
 	 * @param methodName - the expected name of the method.
 	 * @param method - the method we're testing.
 	 * @return TRUE if this is a correct method, FALSE otherwise.
 	 */
 	private boolean matchMethod(String methodName, Method method) {
 		return FuzzyMethodContract.newBuilder().
 			nameExact(methodName).
 			parameterCount(3).
 			parameterSuperOf(MinecraftReflection.getNetHandlerClass(), 1).
 			parameterSuperOf(MinecraftReflection.getPacketClass(), 2).
 			returnTypeExact(MinecraftReflection.getPacketClass()).
 			build().
 			isMatch(MethodInfo.fromMethod(method), null);
 	}
 	
 	/**
 	 * Determine if the Spigot packet listener has been registered.
 	 * @return TRUE if it has, FALSE otherwise.
 	 */
 	public boolean hasRegistered() {
 		return dynamicListener != null;
 	}
 	
 	/**
 	 * Retrieve the dummy player injection handler.
 	 * @return Dummy player injection handler.
 	 */
 	public PlayerInjectionHandler getPlayerHandler() {
 		return new DummyPlayerHandler(this, queuedFilters);
 	}
 	
 	/**
 	 * Retrieve the dummy packet injection handler.
 	 * @return Dummy packet injection handler.
 	 */
 	public PacketInjector getPacketInjector() {
 		return new DummyPacketInjector(this, reveivedFilters);
 	}
 	
 	/**
 	 * Retrieve the currently registered injector for the given player.
 	 * @param player - injected player.
 	 * @return The injector.
 	 */
 	NetworkObjectInjector getInjector(Player player) {
 		return playerInjector.get(player);
 	}
 	
 	/**
 	 * Retrieve or create a registered injector for the given network manager and connection.
 	 * @param networkManager - a INetworkManager object.
 	 * @param connection - a Connection (PlayerConnection, PendingConnection) object.
 	 * @return The created NetworkObjectInjector with a temporary player.
 	 */
 	NetworkObjectInjector getInjector(Object networkManager, Object connection) {
 		NetworkObjectInjector dummyInjector = networkManagerInjector.get(networkManager);
 		
 		if (dummyInjector == null) {
 			// Inject the network manager
 			try {
 				NetworkObjectInjector created = new NetworkObjectInjector(classLoader, reporter, null, invoker, null);
 				
 				if (MinecraftReflection.isLoginHandler(connection)) {
 					created.initialize(connection);
 					created.setPlayer(created.createTemporaryPlayer(server));
 				} else if (MinecraftReflection.isServerHandler(connection)) {
 					// Get the player instead
 					if (playerConnectionPlayer == null)
 						playerConnectionPlayer = FuzzyReflection.fromObject(connection).
 								getFieldByType("player", MinecraftReflection.getEntityPlayerClass());
 					Object entityPlayer = playerConnectionPlayer.get(connection);
 					
 					created.initialize(MinecraftReflection.getBukkitEntity(entityPlayer));
 					
 				} else {
 					throw new IllegalArgumentException("Unregonized connection in NetworkManager.");
 				}
 				
 				dummyInjector = saveInjector(networkManager, created);
 				
 			} catch (IllegalAccessException e) {
 				throw new RuntimeException("Cannot create dummy injector.", e);
 			}
 		}
 		
 		return dummyInjector;
 	}
 
 	/**
 	 * Save a given player injector for later.
 	 * @param networkManager - the associated network manager.
 	 * @param created - the created network object creator.
 	 * @return Any other network injector that came before us.
 	 */
 	private NetworkObjectInjector saveInjector(Object networkManager, NetworkObjectInjector created) {
 		// Concurrency - use the same injector!
 		NetworkObjectInjector result = networkManagerInjector.putIfAbsent(networkManager, created);
 	
 		if (result == null) {
 			result = created;
 		}
 		
 		// Save the player as well
 		playerInjector.put(created.getPlayer(), created);
 		return result;
 	}
 	
 	@Override
 	public Object packetReceived(Object networkManager, Object connection, Object packet) {
 		Integer id = invoker.getPacketID(packet);
 		
 		if (id != null && reveivedFilters.contains(id)) {
 			// Check for ignored packets
 			if (ignoredPackets.remove(packet)) {
 				return packet;
 			}
 			
 			Player sender = getInjector(networkManager, connection).getUpdatedPlayer();
 			PacketContainer container = new PacketContainer(id, packet);
 			PacketEvent event = packetReceived(container, sender);
 			
 			if (!event.isCancelled())
 				return event.getPacket().getHandle();
 			else
 				return null; // Cancel
 		}
 		// Don't change anything
 		return packet;
 	}
 
 	@Override
 	public Object packetQueued(Object networkManager, Object connection, Object packet) {
 		Integer id = invoker.getPacketID(packet);
 		
 		if (id != null & queuedFilters.contains(id)) {
 			// Check for ignored packets
 			if (ignoredPackets.remove(packet)) {
 				return packet;
 			}
 			
 			Player reciever = getInjector(networkManager, connection).getUpdatedPlayer();
 			PacketContainer container = new PacketContainer(id, packet);
 			PacketEvent event = packetQueued(container, reciever);
 
 			if (!event.isCancelled())
 				return event.getPacket().getHandle();
 			else
 				return null; // Cancel
 		}
 		// Don't change anything
 		return packet;
 	}
 
 	/**
 	 * Called to inform the event listeners of a queued packet.
 	 * @param packet - the packet that is to be sent.
 	 * @param reciever - the reciever of this packet.
 	 * @return The packet event that was used.
 	 */
 	PacketEvent packetQueued(PacketContainer packet, Player reciever) {
 		PacketEvent event = PacketEvent.fromServer(this, packet, reciever);
 		
 		invoker.invokePacketSending(event);
 		return event;
 	}
 	
 	/**
 	 * Called to inform the event listeners of a received packet.
 	 * @param packet - the packet that has been receieved.
 	 * @param sender - the client packet.
 	 * @return The packet event that was used.
 	 */
 	PacketEvent packetReceived(PacketContainer packet, Player sender) {
 		PacketEvent event = PacketEvent.fromClient(this, packet, sender);
 		
 		invoker.invokePacketRecieving(event);
 		return event;
 	}
 	
 	/**
 	 * Called when a player has logged in properly.
 	 * @param player - the player that has logged in.
 	 */
 	void injectPlayer(Player player) {
 		try {
 			NetworkObjectInjector dummy = new NetworkObjectInjector(classLoader, reporter, player, invoker, null);
 			dummy.initializePlayer(player);
 			
 			// Save this player for the network manager
 			NetworkObjectInjector realInjector = networkManagerInjector.get(dummy.getNetworkManager());
 			
 			if (realInjector != null) {
 				// Update all future references
 				realInjector.setUpdatedPlayer(player);
 				playerInjector.put(player, realInjector);
 			} else {
 				// Ah - in that case, save this injector
 				saveInjector(dummy.getNetworkManager(), dummy);
 			}
 			
 		} catch (IllegalAccessException e) {
 			throw new RuntimeException("Cannot inject " + player);
 		}
 	}
 
 	/**
 	 * Uninject the given player.
 	 * @param player - the player to uninject.
 	 */
 	void uninjectPlayer(Player player) {
 		final NetworkObjectInjector injector = getInjector(player);
 		
		if (player != null) {
 			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
 				@Override
 				public void run() {
 					// Clean up
 					playerInjector.remove(injector.getPlayer());
 					playerInjector.remove(injector.getUpdatedPlayer());
 					networkManagerInjector.remove(injector);
 				}
 			}, CLEANUP_DELAY);
 		}
 	}
 
 	/**
 	 * Invoked when a plugin wants to sent a packet.
 	 * @param reciever - the packet receiver.
 	 * @param packet - the packet to transmit.
 	 * @param filters - whether or not to invoke the packet listeners.
 	 * @throws InvocationTargetException If anything went wrong.
 	 */
 	void sendServerPacket(Player reciever, PacketContainer packet, boolean filters) throws InvocationTargetException {
 		NetworkObjectInjector networkObject = getInjector(reciever);
 		
 		// If TRUE, process this packet like any other
 		if (filters)
 			ignoredPackets.remove(packet.getHandle());
 		else
 			ignoredPackets.add(packet.getHandle());
 			
 		if (networkObject != null)
 			networkObject.sendServerPacket(packet.getHandle(), filters);
 		else
 			throw new PlayerLoggedOutException("Player " + reciever + " has logged out");
 	}
 
 	/**
 	 * Invoked when a plugin wants to simulate receiving a packet.
 	 * @param player - the supposed sender.
 	 * @param mcPacket - the packet to receieve.
 	 * @throws IllegalAccessException Reflection is not permitted.
 	 * @throws InvocationTargetException Minecraft threw an exception.
 	 */
 	void processPacket(Player player, Object mcPacket) throws IllegalAccessException, InvocationTargetException {
 		NetworkObjectInjector networkObject = getInjector(player);
 		
 		// We will always ignore this packet
 		ignoredPackets.add(mcPacket);
 		
 		if (networkObject != null)
 			networkObject.processPacket(mcPacket);
 		else
 			throw new PlayerLoggedOutException("Player " + player + " has logged out");
 	}
 }
