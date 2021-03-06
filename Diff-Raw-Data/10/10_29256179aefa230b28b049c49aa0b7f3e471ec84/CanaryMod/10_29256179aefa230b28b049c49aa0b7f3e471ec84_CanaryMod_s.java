 package net.canarymod;
 
 import net.canarymod.bansystem.BanManager;
 import net.canarymod.config.Configuration;
 import net.canarymod.database.Database.Type;
import net.canarymod.database.DatabaseFlatfile;
import net.canarymod.database.DatabaseMySql;
 import net.canarymod.help.HelpManager;
 import net.canarymod.hook.HookExecutor;
 import net.canarymod.permissionsystem.PermissionManager;
 import net.canarymod.plugin.PluginLoader;
 import net.canarymod.user.UserAndGroupsProvider;
 
 /**
  * The implementation of Canary, the new catch-all etc replacement, only much better :P
  * 
  * @author Chris Ksoll
  * @author Jos Kuijpers
  */
 public class CanaryMod extends Canary {
 
     /**
      * Creates a new CanaryMod
      */
     public CanaryMod() {
         Canary.instance = this;
         
         this.config = new Configuration();
         Type backend = Configuration.getServerConfig().getDatasourceType();
         if (backend == Type.FLATFILE) {
             this.database = new DatabaseFlatfile();
         } 
         else if (backend == Type.MYSQL) {
             this.database = new DatabaseMySql();
         } 
         else {
             //Uh oh ...
             Logman.logWarning("The specified datasource is invalid! Using Flatfile as default.");
             this.database = new DatabaseFlatfile();
         }
 
         // Initialize the subsystems
         this.permissionLoader = new PermissionManager();
         this.hookExecutor = new HookExecutor();
         this.helpManager = new HelpManager();
         this.banManager = new BanManager();
         //Initialize the plugin loader and scan for plugins
         this.loader = new PluginLoader();
         this.loader.scanPlugins();
     }
     
     public void initUserAndGroupsManager() {
         this.userAndGroupsProvider = new UserAndGroupsProvider();
     }
 }
