 /*
    See README.markdown for more information
    
    ForcegenChunks - Bukkit chunk preloader
    Copyright (C) 2011 john@pointysoftware.net
 
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software Foundation,
    Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
  */
 
 package net.pointysoftware.forcegenchunks;
 
 import java.util.ArrayList;
 import java.util.regex.Pattern;
 import java.util.regex.Matcher;
 
 import org.bukkit.plugin.Plugin;
 import org.bukkit.plugin.PluginManager;
 import org.bukkit.plugin.java.JavaPlugin;
 
 import org.bukkit.Chunk;
 import org.bukkit.Server;
 import org.bukkit.World;
 import org.bukkit.Location;
 import org.bukkit.entity.Player;
 
 import org.bukkit.ChatColor;
 
 import org.bukkit.command.Command;
 import org.bukkit.command.CommandSender;
 
 import org.bukkit.scheduler.BukkitScheduler;
 
 public class ForcegenChunks extends JavaPlugin implements Runnable
 {
     private final static String VERSION = "1.2";
     // see comment in freeLoadedChunks
     private final static int MAX_UNLOAD_REQUESTS = 20;
     private class ChunkXZ
     {
         private int x, z, unloadRequests = 0;
         private World world;
         ChunkXZ(int x, int z, World world) { this.x = x; this.z = z; this.world = world; }
         public int getX() { return x; }
         public int getZ() { return z; }
         public boolean tryUnload()
         {
             if (this.unloadRequests >= MAX_UNLOAD_REQUESTS)
                 return true;
             
             if (this.world.isChunkLoaded(this.x, this.z))
             {
                 this.world.unloadChunkRequest(x, z, true);
                 this.unloadRequests++;
                 return false;
             }
             else
             {
                 return true;
             }
         }
     }
     
     // *very* simple class the parse arguments with quoting
     private class NiceArgsParseIntException extends Throwable
     {
         private String argName, badValue;
         NiceArgsParseIntException(String argName, String badValue)
         {
             this.argName = argName;
             this.badValue = badValue;
         }
         public String getName() { return this.argName; }
         public String getBadValue() { return this.badValue; }
     }
     private class NiceArgsParseException extends Throwable {}
     private class NiceArgs
     {
         private ArrayList<String> cleanArgs;
         private int[] parsedInts;
         NiceArgs(String[] args) throws NiceArgsParseException
         {
             String allargs = "";
             for (int x = 0; x < args.length; x++)
                 allargs += (allargs.length() > 0 ? " " : "") + args[x];
 
             cleanArgs = new ArrayList<String>();
 
             // Matches any list of items delimited by spaces. An item can have quotes around it to escape spaces
             // inside said quotes. Also honors escape sequences
             // E.g. arg1 "arg2 stillarg2" arg3 "arg4 \"bob\" stillarg4" arg5\ stillarg5
             Matcher m = Pattern.compile("\\s*(?:\\\"((?:[^\\\"\\\\]|\\\\.)*)\\\"|((?:[^\\s\\\\\\\"]|\\\\(?:.|$))+))(?:\\s|$)").matcher(allargs);
             while (m.regionStart() < m.regionEnd())
             {
                 if (m.lookingAt())
                 {
                     cleanArgs.add((m.group(1) == null ? m.group(2) : m.group(1)).replaceAll("\\\\(.|$)", "$1"));
                     m.region(m.end(), m.regionEnd());
                 }
                 else
                     throw new NiceArgsParseException();
             }
         }
         public int length() { return this.cleanArgs.size(); }
         public String get(int x) { return this.cleanArgs.get(x); }
         public int getInt(int i, String argName) throws NiceArgsParseIntException
         {
             try
                 { return Integer.parseInt(this.cleanArgs.get(i)); }
             catch (NumberFormatException e)
                 { throw new NiceArgsParseIntException(argName, this.cleanArgs.get(i)); }
         }
     }
     
     // Max size of each block chunk to load at a time
     // A size of 12 would result in 16*16=256 blocks loaded per tick
     private static final int BLOCKSIZE = 16;
 
     private ArrayList<ChunkXZ> ourChunks = new ArrayList<ChunkXZ>();
     private int taskId = 0;
     private World world;
     private int xStart;
     private int xEnd;
     private int zStart;
     private int zEnd;
     private int xNext;
     private int zNext;
     private int radius;
     private int xCenter;
     private int zCenter;
     private int maxLoadedChunks;
     // If the generation is done but we're waiting on chunks
     private boolean waiting;
     private CommandSender commandSender;
     
     public void onEnable()
     {
         replyMsg("v"+VERSION+" Loaded");
     }
     
     // Send the command initiator and the console
     // a message, but don't send duplicates if the
     // console is the command initator.
     private void replyMsg(String str)
     {
         this.replyMsg(str, this.commandSender, false);
     }
     private void replyMsg(String str, CommandSender sender)
     {
         this.replyMsg(str, sender, true);
     }
     private void replyMsg(String str, CommandSender sender, boolean senderOnly)
     {
         Player p = null;
         try
         {
             if (sender != null)
                 p = (Player)sender;
         } catch (ClassCastException e) {}
         
         if (p != null)
             p.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "ForcegenChunks" + ChatColor.DARK_GRAY + "]" + ChatColor.WHITE + " " + str);
         if (p == null || !senderOnly) System.out.println("[ForcegenChunks] " + ChatColor.stripColor(str));
     }
 
     public void onDisable()
     {
         if (this.taskId != 0 && !this.waiting)
         {
             replyMsg("Plugin unloaded, aborting generation.");
             this.endTask();
         }
         // this can cause zombie chunks since we can no longer wait.
         // But it's better than force unloading them and all the bugs
         // that would cause (lighting errors - unloading chunks players
         // are on/near... much worse than a minor transient memory leak)
         this.freeLoadedChunks();
     }
 
     
 
     public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] rawargs)
     {
         boolean isPlayer = true;
         try { Player p = (Player)sender; }
         catch (ClassCastException e) { isPlayer = false; }
         
         NiceArgs args;
         try
         {
             args = new NiceArgs(rawargs);
         }
         catch (NiceArgsParseException e)
         {
             replyMsg("Error - Mismatched/errant quotes in arguments. You can escape quotes in world names with backslashes, e.g. \\\"", sender);
             return true;
         }
         
         boolean bCircular = commandLabel.compareToIgnoreCase("forcegencircle") == 0;
         if (bCircular || commandLabel.compareToIgnoreCase("forcegenchunks") == 0 || commandLabel.compareToIgnoreCase("forcegen") == 0)
         {
             if (!sender.isOp())
             {
                 replyMsg("Requires op status.", sender);
                 return true;
             }
             if (this.taskId != 0 && !this.waiting)
             {
                 replyMsg("Generation already in progress.", sender);
                 return true;
             }
             if     ((bCircular && (args.length() != 1 && args.length() != 2 && args.length() != 4 && args.length() != 5))
                 || (!bCircular && (args.length() != 5 && args.length() != 6)))
             {
                 return false;
             }
             
             World world = null;
             int maxLoadedChunks = -1;
             int xCenter = 0, zCenter = 0, xStart, zStart, xEnd, zEnd, radius = 0;
             try
             {
                 if (bCircular)
                 {
                     radius = args.getInt(0, "radius");
 
                     if (radius < 1)
                     {
                         replyMsg("Radius must be > 1", sender);
                         return true;
                     }
                     
                     if (isPlayer && args.length() < 4)
                     {
                         // Use player's location to center circle
                         Chunk c = ((Player)sender).getLocation().getBlock().getChunk();
                         world = c.getWorld();
                         xCenter = c.getX();
                         zCenter = c.getZ();
                     }
                     else
                     {
                         if (args.length() < 4)
                         {
                             replyMsg("You're not a player, so you need to specify a world name and location.", sender);
                             return true;
                         }
                         world = getServer().getWorld(args.get(1));
                         if (world == null)
                         {
                             replyMsg("World \"" + ChatColor.GOLD + args.get(1) + ChatColor.WHITE + "\" does not exist.", sender);
                             return true;
                         }
                         xCenter = args.getInt(2, "xCenter");
                         zCenter = args.getInt(3, "zCenter");
                     }
                     xStart = xCenter - radius;
                     xEnd = xCenter + radius;
                     zStart = zCenter - radius;
                     zEnd = zCenter + radius;
                     if (args.length() == 2) maxLoadedChunks = args.getInt(1, "maxLoadedChunks");
                     else if (args.length() == 5) maxLoadedChunks = args.getInt(4, "maxLoadedChunks");
                 }
                 else
                 {
                     world = getServer().getWorld(args.get(0));
                     if (world == null)
                     {
                         replyMsg("World \"" + ChatColor.GOLD + args.get(0) + ChatColor.WHITE + "\" does not exist.", sender);
                         return true;
                     }
                     xStart = args.getInt(1, "xStart");
                     zStart = args.getInt(2, "zStart");
                     xEnd   = args.getInt(3, "xEnd");
                     zEnd   = args.getInt(4, "zEnd");
                     if (args.length() == 6) maxLoadedChunks = args.getInt(5, "maxLoadedChunks");
                 }
             }
             catch (NiceArgsParseIntException e)
             {
                 replyMsg("Error: " + e.getName() + " argument must be a number, not \"" + e.getBadValue() + "\"", sender);
                 return true;
             }
             
             int loaded = world.getLoadedChunks().length;
             if (maxLoadedChunks < 0) maxLoadedChunks = loaded + 800;
             else if (maxLoadedChunks < loaded + 200)
             {
                 replyMsg("maxLoadedChunks too low, there are already " + loaded + " chunks loaded - need a value of at least " + (loaded + 200), sender);
                 return true;
             }
             
             if (xEnd - xStart < 1 || zEnd - zStart < 1)
             {
                 replyMsg("xEnd and zEnd must be greater than xStart and zStart respectively.", sender);
                 return true;
             }
 
             this.generateChunks(world, xStart, xEnd, zStart, zEnd, maxLoadedChunks, radius, xCenter, zCenter, sender);
         }
         else if (commandLabel.compareToIgnoreCase("cancelforcegenchunks") == 0 || commandLabel.compareToIgnoreCase("cancelforcegen") == 0)
         {
             if (this.waiting || this.taskId == 0)
             {
                 replyMsg("There is no chunk generation in progress", sender);
                 return true;
             }
             else
             {
                 if (isPlayer && this.commandSender != sender)
                     replyMsg("Generation canceled", sender);
                 replyMsg("Generation canceled by " + (isPlayer ? ("player " + ChatColor.GOLD + ((Player)sender).getName() + ChatColor.WHITE) : "the console") + ", waiting for remaining chunks to unload.");
                 this.cancelGeneration();
             }
         }
         return true;
     }
 
     public boolean generateChunks(World world, int xStart, int xEnd, int zStart, int zEnd, int maxLoadedChunks, int radius, int xCenter, int zCenter, CommandSender commandSender)
     {
         if (!this.waiting && this.taskId != 0) return false;
 
         // The generation routine adds 2 to the edges of the generation cells
         // so bump these borders in by two (if possible) to avoid generating
         // more than requested chunks. It's still possible to generate extra
         // chunks if the height or width of the requested block is < 5
         if (xEnd - xStart > 2) xEnd -= 2;
         if (xEnd - xStart > 2) xStart += 2;
         if (zEnd - zStart > 2) zEnd -= 2;
         if (zEnd - zStart > 2) zStart += 2;
 
         this.maxLoadedChunks = maxLoadedChunks;
         this.world = world;
         this.xStart = xStart;
         this.xNext = xStart;
         this.zNext = zStart;
         this.xEnd = xEnd;
         this.zStart = zStart;
         this.zEnd = zEnd;
         this.xCenter = xCenter;
         this.zCenter = zCenter;
         this.radius = radius;
         this.commandSender = commandSender;
         this.waiting = false;
         this.taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 50, 50);
         
         boolean isPlayer = true;
         try { Player p = (Player)commandSender; }
         catch (ClassCastException e) { isPlayer = false; }
         
         int num = (xEnd - xStart + 1) * (zEnd - zStart + 1);
         replyMsg((isPlayer ? ("Player " + ChatColor.GOLD + ((Player)commandSender).getName() + ChatColor.WHITE) : "The console") + " started generation of " + num + " Chunks (" + (num * 16) + " blocks).");
         return true;
     }
 
     private int freeLoadedChunks()
     {
         // requesting an unloaded chunk wont always cause it to unload,
         // causing misc chunks to pile up, so we keep issueing unload requests
         // until the chunk disappears. These chunks might never even unload
         // (IE they're part of the spawn radius or some such), so after we
         // reach MAX_UNLOAD_REQUESTS we assume the chunk has some reason
         // to live.
         for (int i = ourChunks.size() - 1; i >= 0; i--)
         {
             if (ourChunks.get(i).tryUnload())
                 ourChunks.remove(i);
         }
         return ourChunks.size();
     }
     
     public void cancelGeneration()
     {
         if (this.taskId != 0 && !this.waiting)
         {
             this.zNext = this.zEnd + 1;
             this.waiting = true;
         }
     }
     
     // use cancelGeneration to stop generation, this should only be used internally
     private void endTask()
     {
         if (this.taskId != 0)
             getServer().getScheduler().cancelTask(this.taskId);
         this.taskId = 0;
         this.commandSender = null;
         this.waiting = false;
     }
 
     public void run()
     {
         if (this.taskId == 0) return; // Prevent inappropriate calls
 
         int remainingChunks = this.freeLoadedChunks();
 
         int loaded = world.getLoadedChunks().length;
 
         if (this.zNext > this.zEnd)
         {
             if (!this.waiting)
             {
                replyMsg("Finished generating, " + loaded + " chunks currently loaded. Waiting for " + remainingChunks + "to finish unloading...");
                 this.waiting = true;
             }
             if (remainingChunks == 0)
             {
                 replyMsg("All outstanding chunks cleaned up, have a nice day! (Currently " + loaded + " chunks loaded)");
                 this.endTask();
             }
             return;
         }
 
         if (loaded > this.maxLoadedChunks)
         {
             replyMsg("More than " + this.maxLoadedChunks + " chunks loaded (" + loaded + "), waiting for some to finish unloading");
             return;
         }
 
         int x1 = this.xNext - 2;
         int x2 = Math.min(x1 + this.BLOCKSIZE - 1, this.xEnd + 2);
         int z1 = this.zNext - 2;
         int z2 = Math.min(z1 + this.BLOCKSIZE - 1, this.zEnd + 2);
 
         replyMsg("Generating " + ((x2 - x1 + 1) * (z2 - z1 + 1)) + " chunk region from ["+x1+","+z1+"] to ["+x2+","+z2+"], " + loaded + " currently loaded.");
 
         for (int nx = x1; nx <= x2; nx++)
         {
             for (int nz = z1; nz <= z2; nz++)
             {
                 if (!world.isChunkLoaded(nx, nz))
                 {
                     // If we're doing a circular generation, this block might be skipped
                     if ((radius > 0) && (radius < Math.sqrt((double)(Math.pow(Math.abs(nx - xCenter),2) + Math.pow(Math.abs(nz - zCenter),2)))))
                             continue;
                     // Keep tracks of chunks we caused to load so we can unload them
                     ourChunks.add(new ChunkXZ(nx, nz, world));
                     world.loadChunk(nx, nz, true);
                 }
             }
         }
 
         //loaded = world.getLoadedChunks().length;
         //replyMsg("... now loaded: " + loaded);
         this.xNext = x2 + 1;
 
         if (this.xNext > this.xEnd)
         {
             this.xNext = this.xStart;
             this.zNext = z2 + 1;
         }
     }
 }
