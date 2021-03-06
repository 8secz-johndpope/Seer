 package in.mDev.MiracleM4n.mChatSuite;
 
 import java.io.File;
 import java.util.Date;
 import java.util.HashMap;
 
 import in.mDev.MiracleM4n.mChatSuite.commands.MCommandSender;
 import in.mDev.MiracleM4n.mChatSuite.commands.MECommandSender;
 import in.mDev.MiracleM4n.mChatSuite.commands.MPCommandSender;
 
 import in.mDev.MiracleM4n.mChatSuite.configs.MCConfigListener;
 import in.mDev.MiracleM4n.mChatSuite.configs.MConfigListener;
 import in.mDev.MiracleM4n.mChatSuite.configs.MIConfigListener;
 import in.mDev.MiracleM4n.mChatSuite.configs.MLanguageListener;
 
 import in.mDev.MiracleM4n.mChatSuite.events.MBlockListener;
 import in.mDev.MiracleM4n.mChatSuite.events.MCustomListener;
 import in.mDev.MiracleM4n.mChatSuite.events.MEntityListener;
 import in.mDev.MiracleM4n.mChatSuite.events.MPlayerListener;
 
 import org.bukkit.Location;
 import org.bukkit.configuration.file.YamlConfiguration;
 import org.bukkit.entity.Player;
 import org.bukkit.event.Event;
 import org.bukkit.event.Event.Priority;
 import org.bukkit.plugin.PluginDescriptionFile;
 import org.bukkit.plugin.PluginManager;
 import org.bukkit.plugin.java.JavaPlugin;
 import org.bukkit.plugin.Plugin;
 
 import com.herocraftonline.dev.heroes.Heroes;
 
 import com.massivecraft.factions.Conf;
 
 import de.bananaco.permissions.info.InfoReader;
 import de.bananaco.permissions.worlds.WorldPermissionsManager;
 
 import org.anjocaido.groupmanager.GroupManager;
 import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
 
 import org.getspout.spoutapi.player.SpoutPlayer;
 
 import in.mDev.MiracleM4n.mChatSuite.GUI.*;
 
 import ru.tehkode.permissions.PermissionManager;
 import ru.tehkode.permissions.bukkit.PermissionsEx;
 
 public class mChatSuite extends JavaPlugin {
     // For External Use
     static mChatSuite mSuite;
 
     // Default Plugin Data
     public PluginManager pm;
     public PluginDescriptionFile pdfFile;
 
     // Listeners
     public static MPlayerListener pListener;
     public static MEntityListener eListener;
     public static MBlockListener bListener;
     public static MCommandSender mCSender;
     public static MECommandSender mECSender;
     public static MPCommandSender mPCSender;
     public static MConfigListener cListener;
     public static MIConfigListener mIListener;
     public static MCConfigListener mCListener;
     public static MCustomListener cusListener;
 
     // GUI
     public static Main mGUI;
     public static GUIEvent mGUIEvent;
     public static Pages mPages;
 
     // API
     @Deprecated
     public static mChatAPI API;
     @Deprecated
     mChatAPI mAPI;
 
     // Info API
     @Deprecated
     public static MInfoReader IReader;
     @Deprecated
     MInfoReader mIReader;
 
     // GroupManager
     WorldsHolder gmPermissionsWH;
     Boolean gmPermissionsB = false;
 
     // PermissionsEX
     PermissionManager pexPermissions;
     Boolean PEXB = false;
 
     // PermissionsBukkit
     Boolean PermissionBuB = false;
 
     // bPermissions
     WorldPermissionsManager bPermS;
     InfoReader bInfoR;
     Boolean bPermB = false;
 
     // mChannel
     public Boolean mChanB = false;
 
     // MobDisguise
     public Boolean mobD = false;
 
     // Register
     Boolean regB = false;
 
     // Factions
     Boolean factionsB = false;
 
     // Heroes
     Heroes heroes;
     Boolean heroesB = false;
 
     // Configuration
     public YamlConfiguration mConfig = null;
     public YamlConfiguration mIConfig = null;
     public YamlConfiguration mCConfig = null;
     public YamlConfiguration mELocale = null;
 
     // Configuration Files
     public File mConfigF = null;
     public File mIConfigF = null;
     public File mCConfigF = null;
     public File mELocaleF = null;
 
     // Optional mChatSuite only Info Support
     public Boolean useNewInfo = false;
 
     // Optional Old Nodular Style Formatting
     public Boolean useOldNodes = false;
 
     // Optional Leveled Nodes
     public Boolean useLeveledNodes = false;
 
     // API Only Boolean
     public Boolean mAPIOnly = false;
 
     // Alter Event Messages Boolean
     public Boolean alterEvents = true;
     public Boolean alterDMessages = true;
 
     // Add New Players Boolean
     public Boolean useAddDefault = false;
 
     // Info Related Variables
     public String mIDefaultGroup = "default";
 
     // Formatting
     public String varIndicator = "+";
     public String tabbedListFormat = "+p+dn+s";
     public String listCmdFormat = "+p+dn+s";
     public String chatFormat = "+p+dn+s&f: +m";
     public String nameFormat = "+p+dn+s&e";
     public String eventFormat = "+p+dn+s&e";
     public String meFormat = "* +p+dn+s&e +m";
     public String dateFormat = "HH:mm:ss";
 
     // Messages
     public String joinMessage = "has joined the game.";
     public String leaveMessage = "has left the game.";
     public String kickMessage = "has been kicked from the game +r.";
     public String deathInFire = "went up in flames.";
     public String deathOnFire = "burned to death.";
     public String deathLava = "tried to swim in lava.";
     public String deathInWall = "suffocated in a wall.";
     public String deathDrown = "drowned.";
     public String deathStarve = "starved to death.";
     public String deathCactus = "was pricked to death.";
     public String deathFall = "hit the ground too hard.";
     public String deathOutOfWorld = "fell out of the world.";
     public String deathGeneric = "died.";
     public String deathExplosion = "blew up.";
     public String deathMagic = "was killed by magic.";
     public String deathEntity = "was slain by +CName.";
     public String deathArrow = "was shot by +CName.";
     public String deathFireball = "was fireballed by +CName.";
     public String deathThrown = "was pummeled by +CName.";
     public String hMasterT = "The Great";
     public String hMasterF = "The Squire";
 
     // Booleans
     public Boolean spoutEnabled = true;
     public Boolean healthNotify = false;
     public Boolean healthAchievement = true;
     public Boolean spoutB = false;
     public Boolean mAFKHQ = true;
     public Boolean mChatEB = false;
     public Boolean useAFKList = false;
     public Boolean mChatPB = false;
     public Boolean spoutPM = false;
     public Boolean sJoinB = false;
     public Boolean sDeathB = false;
     public Boolean sQuitB = false;
     public Boolean sKickB = false;
 
     // Numbers
     public Integer AFKTimer = 30;
     public Integer AFKKickTimer = 120;
     public Integer sJoinI = 30;
     public Integer sDeathI = 30;
     public Integer sQuitI = 30;
     public Integer sKickI = 30;
 
     // Other Config Stuff
     public Double chatDistance = -1.0;
     public String cLVars = "default,Default";
     public String listVar = "group";
 
     // Timers
     long sTime1;
     long sTime2;
     float sDiff;
 
     // Maps
     public HashMap<String, Long> lastMove = new HashMap<String, Long>();
     public HashMap<String, Boolean> chatt = new HashMap<String, Boolean>();
     public HashMap<String, Boolean> isAFK = new HashMap<String, Boolean>();
     public HashMap<String, Location> AFKLoc = new HashMap<String, Location>();
     public HashMap<String, String> lastPMd = new HashMap<String, String>();
     public HashMap<String, Boolean> isConv = new HashMap<String, Boolean>();
     public HashMap<String, String> getInvite = new HashMap<String, String>();
     public HashMap<String, String> chatPartner = new HashMap<String, String>();
 
     @SuppressWarnings({"deprecation"})
     public void onEnable() {
         // 1st Startup Timer
         sTime1 = new Date().getTime();
 
         // Initialize Plugin Data
         pm = getServer().getPluginManager();
         pdfFile = getDescription();
         mSuite = this;
 
         // First we kill Essentials Chat
         killEss();
 
         // Initialize Configs
         if (new File("plugins/mChat/").isDirectory()) {
             mConfigF = new File("plugins/mChat/", "config.yml");
             mIConfigF = new File("plugins/mChat/", "info.yml");
             mCConfigF = new File("plugins/mChat/", "censor.yml");
             mELocaleF = new File("plugins/mChat/", "locale.yml");
         } else {
             mConfigF = new File(getDataFolder(), "config.yml");
             mIConfigF = new File(getDataFolder(), "info.yml");
             mCConfigF = new File(getDataFolder(), "censor.yml");
             mELocaleF = new File(getDataFolder(), "locale.yml");
         }
 
         mConfig = YamlConfiguration.loadConfiguration(mConfigF);
         mIConfig = YamlConfiguration.loadConfiguration(mIConfigF);
         mCConfig = YamlConfiguration.loadConfiguration(mCConfigF);
         mELocale = YamlConfiguration.loadConfiguration(mELocaleF);
 
         // Manage Config options
         mConfig.options().indent(4);
         mIConfig.options().indent(4);
         mCConfig.options().indent(4);
 
         // Initialize the API's
         API = new mChatAPI(this);
         mAPI = new mChatAPI(this);
         IReader = new MInfoReader(this);
         mIReader = new MInfoReader(this);
 
         // Initialize Listeners
         cListener = new MConfigListener(this);
         mIListener = new MIConfigListener(this);
         mCListener = new MCConfigListener(this);
 
         // Setup Configs
         setupConfigs();
 
         // Setup Plugins
         setupPlugins();
 
         // Initialize Delayed Listeners
         mCSender = new MCommandSender(this);
         mECSender = new MECommandSender(this);
         mPCSender = new MPCommandSender(this);
 
         if (!mAPIOnly) {
             if (spoutB) {
                 cusListener = new MCustomListener(this);
 
                 mGUI = new Main(this);
                 mGUIEvent = new GUIEvent(this);
                 mPages = new Pages(this);
             }
 
             pListener = new MPlayerListener(this);
             bListener = new MBlockListener(this);
             eListener = new MEntityListener(this);
         }
 
         // Setup Permissions
         setupPerms();
 
         // Register Events
         registerEvents();
 
         // Setup Tasks
         setupTasks();
 
         // Register Commands
         getCommand("mchat").setExecutor(mCSender);
 
         getCommand("mchatme").setExecutor(mECSender);
         getCommand("mchatwho").setExecutor(mECSender);
         getCommand("mchatlist").setExecutor(mECSender);
         getCommand("mchatafk").setExecutor(mECSender);
         getCommand("mchatafkother").setExecutor(mECSender);
 
         getCommand("pmchat").setExecutor(mPCSender);
         getCommand("pmchatreply").setExecutor(mPCSender);
         getCommand("pmchatinvite").setExecutor(mPCSender);
         getCommand("pmchataccept").setExecutor(mPCSender);
         getCommand("pmchatdeny").setExecutor(mPCSender);
         getCommand("pmchatleave").setExecutor(mPCSender);
 
         // Ping Stats                                       `
         Stats.init(this);
 
         // Add All Players To Info.yml
         if (useAddDefault)
             for (Player players : getServer().getOnlinePlayers())
                 if (mIConfig.get("users." + players.getName()) == null)
                     mIReader.addPlayer(players.getName(), mIDefaultGroup);
 
         if (mChatEB) {
             for (Player players : getServer().getOnlinePlayers()) {
                SpoutPlayer sPlayers = (SpoutPlayer) players;
                 isAFK.put(players.getName(), false);
                 chatt.put(players.getName(), false);
                 lastMove.put(players.getName(), new Date().getTime());
 
                if (spoutB)
                     sPlayers.setTitle(getAPI().ParsePlayerName(players));
             }
         }
 
         // Check for Automatic Factions Support
         setupFactions();
 
         // 2nd Startup Timer
         sTime2 = new Date().getTime();
 
         // Calculate Startup Timer
         sDiff = new Float (sTime2 - sTime1) / 1000;
 
         getAPI().log("[" + pdfFile.getName() + "] " + pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled! Took " + sDiff + " seconds.");
     }
 
     public void onDisable() {
         getServer().getScheduler().cancelTasks(this);
 
         Stats.unload();
 
         getAPI().log("[" + pdfFile.getName() + "] " + pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!");
     }
 
     void registerEvents() {
         if (!mAPIOnly) {
             pm.registerEvent(Event.Type.PLAYER_CHAT, pListener, Priority.Normal, this);
             pm.registerEvent(Event.Type.PLAYER_INTERACT, pListener, Priority.Normal, this);
             pm.registerEvent(Event.Type.SIGN_CHANGE, bListener, Priority.Normal, this);
             pm.registerEvent(Event.Type.PLAYER_JOIN, pListener, Priority.Normal, this);
 
             pm.registerEvent(Event.Type.PLAYER_MOVE, pListener, Priority.Normal, this);
             pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, pListener, Priority.Normal, this);
             pm.registerEvent(Event.Type.ENTITY_DAMAGE, eListener, Priority.Normal, this);
 
             pm.registerEvent(Event.Type.PLAYER_KICK, pListener, Priority.Normal, this);
             pm.registerEvent(Event.Type.PLAYER_QUIT, pListener, Priority.Normal, this);
 
             pm.registerEvent(Event.Type.ENTITY_DEATH, eListener, Priority.Normal, this);
 
             if (spoutB) {
                 pm.registerEvent(Event.Type.CUSTOM_EVENT, cusListener, Event.Priority.Normal, this);
                 pm.registerEvent(Event.Type.CUSTOM_EVENT, mGUIEvent, Event.Priority.Normal, this);
             }
         }
     }
 
     void setupPerms() {
         Plugin permTest;
 
         permTest = pm.getPlugin("PermissionsBukkit");
         if (permTest != null) {
             PermissionBuB = true;
             getAPI().log("[" + pdfFile.getName() + "] " + permTest.getDescription().getName() + " v" +  (permTest.getDescription().getVersion()) + " found hooking in.");
             return;
         }
 
         permTest = pm.getPlugin("bPermissions");
         if (permTest != null) {
             bPermB = true;
             bInfoR = de.bananaco.permissions.Permissions.getInfoReader();
             bPermS = de.bananaco.permissions.Permissions.getWorldPermissionsManager();
             getAPI().log("[" + pdfFile.getName() + "] " + permTest.getDescription().getName() + " v" +  (permTest.getDescription().getVersion()) + " found hooking in.");
             return;
         }
 
         permTest = pm.getPlugin("PermissionsEx");
         if (permTest != null) {
             pexPermissions = PermissionsEx.getPermissionManager();
             PEXB = true;
             getAPI().log("[" + pdfFile.getName() + "] " + permTest.getDescription().getName() + " v" +  (permTest.getDescription().getVersion()) + " found hooking in.");
             return;
         }
 
         permTest = pm.getPlugin("GroupManager");
         if (permTest != null) {
             gmPermissionsB = true;
             gmPermissionsWH = ((GroupManager) permTest).getWorldsHolder();
             getAPI().log("[" + pdfFile.getName() + "] " + permTest.getDescription().getName() + " v" +  (permTest.getDescription().getVersion()) + " found hooking in.");
             return;
         }
 
         getAPI().log("[" + pdfFile.getName() + "] No Permissions plugins were found defaulting to permissions.yml/info.yml.");
     }
 
     public Boolean setupPlugin(String pluginName) {
         Plugin plugin = pm.getPlugin(pluginName);
 
         if (plugin != null) {
             getAPI().log("[" + pdfFile.getName() + "] " +  plugin.getDescription().getName() + " " + (plugin.getDescription().getVersion()) + " found hooking in.");
             return true;
         }
 
         return false;
     }
 
     void setupPlugins() {
         // Setup mChannel
         mChanB = setupPlugin("mChannel");
 
         // Setup MobDisguise
         mobD = setupPlugin("MobDisguise");
 
         // Setup Register
         regB = setupPlugin("Register");
 
         // Setup Factions
         factionsB = setupPlugin("Factions");
 
         // Setup Heroes
         heroesB = setupPlugin("Heroes");
 
         if(heroesB)
             heroes = (Heroes) pm.getPlugin("Heroes");
 
         spoutB = setupPlugin("Spout");
 
         if (!spoutEnabled)
             spoutB = false;
     }
 
     void setupFactions() {
         if (factionsB)
             if (!(Conf.chatTagInsertIndex == 0))
                 getServer().dispatchCommand(getServer().getConsoleSender(), "f config chatTagInsertIndex 0");
     }
 
     void killEss() {
         Plugin plugin = pm.getPlugin("EssentialsChat");
 
         if (plugin != null)
             pm.disablePlugin(plugin);
     }
 
     public void setupConfigs() {
         cListener.checkConfig();
         cListener.loadConfig();
 
         mIListener.checkConfig();
 
         mCListener.loadConfig();
 
         getLocale().checkLocale();
     }
 
     public void loadConfigs() {
         cListener.load();
 
         mCListener.load();
 
         mIListener.load();
 
         getLocale().load();
     }
 
     void setupTasks() {
         getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
             public void run() {
                 if (!mChatEB)
                     return;
 
                 if (AFKTimer < 0)
                     return;
 
                 cListener.load();
 
                 for (Player player : getServer().getOnlinePlayers()) {
                     if (getAPI().checkPermissions(player, "mchat.bypass.afk"))
                         continue;
 
                     if (isAFK.get(player.getName()))
                         continue;
 
                     if (new Date().getTime() - (AFKTimer * 1000) > lastMove.get(player.getName())) {
                         getServer().dispatchCommand(getServer().getConsoleSender(), "mchatafkother " + player.getName() + " AutoAfk");
                     } else
                         isAFK.put(player.getName(), false);
                 }
             }
         }, 20L * 5, 20L * 5);
 
         getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
             public void run() {
                 if (!mChatEB)
                     return;
 
                 if (AFKKickTimer < 0)
                     return;
 
                 cListener.load();
 
                 for (Player player : getServer().getOnlinePlayers()) {
                     if (getAPI().checkPermissions(player, "mchat.bypass.afkkick"))
                         continue;
 
                     if (!isAFK.get(player.getName()))
                         continue;
 
                     if (new Date().getTime() - (AFKKickTimer * 1000) > lastMove.get(player.getName()))
                         player.kickPlayer("mAFK Kick");
                 }
             }
         }, 20L * 10, 20L * 10);
     }
 
     public static MLanguageListener getLocale() {
         return new MLanguageListener(mSuite, mSuite.mELocale);
     }
 
     // API
     public static mChatAPI getAPI() {
         return new mChatAPI(mSuite);
     }
 
     // InfoReader
     public static MInfoReader getInfoReader() {
         return new MInfoReader(mSuite);
     }
 }
