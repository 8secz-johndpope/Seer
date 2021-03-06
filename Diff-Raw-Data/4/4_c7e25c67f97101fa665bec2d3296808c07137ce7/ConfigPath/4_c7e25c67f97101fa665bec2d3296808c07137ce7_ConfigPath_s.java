 package com.dumptruckman.chunky.config;
 
 /**
  * @author dumptruckman, SwearWord
  */
 public enum ConfigPath {
     LANGUAGE("settings.language_file", "english.yml", "# This is the language file you wish to use."),
     DATA_SAVE_PERIOD("settings.data.save_every", 30, "# This is often module data is written to the disk."),
     DEBUG("settings.debug_mode", false, "# Enables debugging mode"),
 
     USING_MYSQL("settings.mysql.using_mysql", true, "# True for MySQL, flat-files otherwise."),
     MYSQL_USERNAME("settings.mysql.username","root","# Username for MySQL database."),
     MYSQL_PASSWORD("settings.mysql.password","password","# Password for MySQL database."),
     MYSQL_HOST("settings.mysql.host","localhost","# Address for the MySQL server."),
     MYSQL_DATABASE("settings.mysql.database","minecraft","# Name of database to use."),
     MYSQL_PORT("settings.mysql.port","3306","# MySQL server port."),
 
     SWITCH_IDS("settings.switch_ids","25,54,61,62,64,69,70,71,72,77,96,84,93,94", "# Switchable blocks"),
     ITEM_USE_IDS("settings.item_use_ids","259,325,326,327,351","# Usable items"),
 
     PLAYER_CHUNK_LIMIT("player.chunk_limit.default", 10, "# The default number of chunks a player list allowed to claim."),
     PLAYER_CHUNK_LIMIT_CUSTOM("player.chunk_limit.custom", "", "# The default number of chunks a player list allowed to claim."),
     PLAYER_CHUNK_LIMIT_EXAMPLE("player.chunk_limit.custom.example", 25, "# This example creates a permission node: chunky.chunk_claim_limit.example", "# This allows players will that permission node to claim 25 chunks instead of the default."),
 
     UNOWNED_BUILD("unowned.build", false, "# Can player build on unowned chunks"),
     UNOWNED_DESTROY("unowned.destroy", false, "# Can player destroy on unowned chunks"),
    UNOWNED_ITEMUSE("unowned.destroy", false, "# Can player use items on unowned chunks"),
    UNOWNED_SWITCH("unowned.destroy", false, "# Can player switch on unowned chunks"),
     ;
 
     private String path;
     private Object def;
     private String[] comments;
 
     ConfigPath(String path, Object def, String...comments) {
         this.path = path;
         this.def = def;
         this.comments = comments;
     }
 
     /**
      * Retrieves the path for a config option
      * @return The path for a config option
      */
     public String getPath() {
         return path;
     }
 
     /**
      * Retrieves the default value for a config path
      * @return The default value for a config path
      */
     public Object getDefault() {
         return def;
     }
 
     /**
      * Retrieves the comment for a config path
      * @return The comments for a config path
      */
     public String[] getComments() {
         if (comments != null) {
             return comments;
         }
 
         String[] comments = new String[1];
         comments[0] = "";
         return comments;
     }
 }
