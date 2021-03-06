 package org.dynmap.bukkit;
 
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.ListIterator;
 
 import org.bukkit.World;
 import org.bukkit.Chunk;
 import org.bukkit.block.Biome;
 import org.bukkit.entity.Entity;
 import org.bukkit.ChunkSnapshot;
 import org.dynmap.DynmapChunk;
 import org.dynmap.DynmapCore;
 import org.dynmap.DynmapWorld;
 import org.dynmap.Log;
 import org.dynmap.common.BiomeMap;
 import org.dynmap.utils.MapChunkCache;
 import org.dynmap.utils.MapIterator;
 import org.dynmap.utils.MapIterator.BlockStep;
 import org.getspout.spoutapi.block.SpoutChunk;
 
 /**
  * Container for managing chunks - dependent upon using chunk snapshots, since rendering is off server thread
  */
 public class NewMapChunkCache implements MapChunkCache {
     private static boolean init = false;
     private static Method poppreservedchunk = null;
     private static Method gethandle = null;
     private static Method removeentities = null;
     private static Method getworldhandle = null;    
     private static Field  chunkbiome = null;
     private static boolean use_spout = false;
 
     private World w;
     private DynmapWorld dw;
     private Object craftworld;
     private List<DynmapChunk> chunks;
     private ListIterator<DynmapChunk> iterator;
     private int x_min, x_max, z_min, z_max;
     private int x_dim;
     private boolean biome, biomeraw, highesty, blockdata;
     private HiddenChunkStyle hidestyle = HiddenChunkStyle.FILL_AIR;
     private List<VisibilityLimit> visible_limits = null;
     private List<VisibilityLimit> hidden_limits = null;
     private boolean do_generate = false;
     private boolean do_save = false;
     private boolean isempty = true;
     private ChunkSnapshot[] snaparray; /* Index = (x-x_min) + ((z-z_min)*x_dim) */
     private byte[][] swampcnt;
     private BiomeMap[][] biomemap;
     private boolean[][] isSectionNotEmpty; /* Indexed by snapshot index, then by section index */
     
     private int chunks_read;    /* Number of chunks actually loaded */
     private int chunks_attempted;   /* Number of chunks attempted to load */
     private long total_loadtime;    /* Total time loading chunks, in nanoseconds */
     
     private long exceptions;
     
     private static final BlockStep unstep[] = { BlockStep.X_MINUS, BlockStep.Y_MINUS, BlockStep.Z_MINUS,
         BlockStep.X_PLUS, BlockStep.Y_PLUS, BlockStep.Z_PLUS };
 
     private static BiomeMap[] biome_to_bmap;
     
     /**
      * Iterator for traversing map chunk cache (base is for non-snapshot)
      */
     public class OurMapIterator implements MapIterator {
         private int x, y, z, chunkindex, bx, bz, off;  
         private ChunkSnapshot snap;
         private BlockStep laststep;
         private int typeid = -1;
         private int blkdata = -1;
         private final int worldheight;
         private final int x_base;
         private final int z_base;
         
         OurMapIterator(int x0, int y0, int z0) {
             x_base = x_min << 4;
             z_base = z_min << 4;
             if(biome)
                 biomePrep();
             initialize(x0, y0, z0);
             worldheight = w.getMaxHeight();
         }
         public final void initialize(int x0, int y0, int z0) {
             this.x = x0;
             this.y = y0;
             this.z = z0;
             this.chunkindex = ((x >> 4) - x_min) + (((z >> 4) - z_min) * x_dim);
             this.bx = x & 0xF;
             this.bz = z & 0xF;
             this.off = bx + (bz << 4);
             try {
                 snap = snaparray[chunkindex];
             } catch (ArrayIndexOutOfBoundsException aioobx) {
                 snap = EMPTY;
                 exceptions++;
             }
             laststep = BlockStep.Y_MINUS;
             typeid = blkdata = -1;
         }
         public final int getBlockTypeID() {
             if(typeid < 0)
                 typeid = snap.getBlockTypeId(bx, y, bz);
             return typeid;
         }
         public final int getBlockData() {
             if(blkdata < 0)
                 blkdata = snap.getBlockData(bx, y, bz);
             return blkdata;
         }
         public int getBlockSkyLight() {
             return snap.getBlockSkyLight(bx, y, bz);
         }
         public final int getBlockEmittedLight() {
             return snap.getBlockEmittedLight(bx, y, bz);
         }
         private void biomePrep() {
             if(swampcnt != null)
                 return;
             int x_size = x_dim << 4;
             int z_size = (z_max - z_min + 1) << 4;
             swampcnt = new byte[x_size][];
             biomemap = new BiomeMap[x_size][];
             for(int i = 0; i < x_size; i++) {
                 swampcnt[i] = new byte[z_size];
                 biomemap[i] = new BiomeMap[z_size];
             }
             for(int i = 0; i < x_size; i++) {
                 initialize(i + x_base, 64, z_base);
                 for(int j = 0; j < z_size; j++) {
                     Biome bb = snap.getBiome(bx, bz);
                     if(bb == null)
                         biomemap[i][j] = BiomeMap.NULL;
                     else
                         biomemap[i][j] = biome_to_bmap[bb.ordinal()];
                     if(biomemap[i][j] == BiomeMap.SWAMPLAND) {
                         for(int ii = i-1; ii < i+2; ii++) {
                             for(int jj = j-1; jj < j+2; jj++) {
                                 if(ii < 0) continue;
                                 if(jj < 0) continue;
                                 if(ii >= x_size) continue;
                                 if(jj >= z_size) continue;
                                 swampcnt[ii][jj]++;
                             }
                         }
                     }
                     stepPosition(BlockStep.Z_PLUS);
                 }
             }
         }
         
         public final BiomeMap getBiome() {
             try {
                 return biomemap[x - x_base][z - z_base];
             } catch (Exception ex) {
                 return BiomeMap.NULL;
             }
         }
         
         public final int  countSmoothedSwampBiomes() {
             try {
                 return swampcnt[x - x_base][z - z_base];
             } catch (Exception ex) {
                 return 0;
             }
         }
         
         public final int  countSmoothedSwampBiomes(int sx, int sz, int scale) {
             try {
                 int xx = x - x_base;
                 int zz = z - z_base;
                 sx <<= 1;
                 sz <<= 1;
                 int s0 = swampcnt[xx][zz];
                 int w;
                 int tot;
                 if(sx < scale) {
                     w = scale - sx;
                     tot = (w * swampcnt[xx-1][zz]) + ((scale - w) * s0);
                 }
                 else if(sx > scale) {
                     w = sx - scale;
                     tot = (w * swampcnt[xx+1][zz]) + ((scale - w) * s0);
                 }
                 else {
                     tot = scale * s0;
                 }
                 if(sz < scale) {
                     w = scale - sz;
                     tot += (w * swampcnt[xx][zz-1]) + ((scale - w) * s0);
                 }
                 else if(sz > scale) {
                     w = sz - scale;
                     tot += (w * swampcnt[xx][zz+1]) + ((scale - w) * s0);
                 }
                 else {
                     tot += scale * s0;
                 }
                 return tot;
             } catch (Exception ex) {
                 return 0;
             }
         }
         
         public final double getRawBiomeTemperature() {
             return snap.getRawBiomeTemperature(bx, bz);
         }
         public final double getRawBiomeRainfall() {
             return snap.getRawBiomeRainfall(bx, bz);
         }
         /**
          * Step current position in given direction
          */
         public final void stepPosition(BlockStep step) {
             switch(step.ordinal()) {
                 case 0:
                     x++;
                     bx++;
                     off++;
                     if(bx == 16) {  /* Next chunk? */
                         try {
                             bx = 0;
                             off -= 16;
                             chunkindex++;
                             snap = snaparray[chunkindex];
                         } catch (ArrayIndexOutOfBoundsException aioobx) {
                             snap = EMPTY;
                             exceptions++;
                         }
                     }
                     break;
                 case 1:
                     y++;
                     break;
                 case 2:
                     z++;
                     bz++;
                     off+=16;
                     if(bz == 16) {  /* Next chunk? */
                         try {
                             bz = 0;
                             off -= 256;
                             chunkindex += x_dim;
                             snap = snaparray[chunkindex];
                         } catch (ArrayIndexOutOfBoundsException aioobx) {
                             snap = EMPTY;
                             exceptions++;
                         }
                     }
                     break;
                 case 3:
                     x--;
                     bx--;
                     off--;
                     if(bx == -1) {  /* Next chunk? */
                         try {
                             bx = 15;
                             off += 16;
                             chunkindex--;
                             snap = snaparray[chunkindex];
                         } catch (ArrayIndexOutOfBoundsException aioobx) {
                             snap = EMPTY;
                             exceptions++;
                         }
                     }
                     break;
                 case 4:
                     y--;
                     break;
                 case 5:
                     z--;
                     bz--;
                     off-=16;
                     if(bz == -1) {  /* Next chunk? */
                         try {
                             bz = 15;
                             off += 256;
                             chunkindex -= x_dim;
                             snap = snaparray[chunkindex];
                         } catch (ArrayIndexOutOfBoundsException aioobx) {
                             snap = EMPTY;
                             exceptions++;
                         }
                     }
                     break;
             }
             laststep = step;
             typeid = -1;
             blkdata = -1;
         }
         /**
          * Unstep current position to previous position
          */
         public BlockStep unstepPosition() {
             BlockStep ls = laststep;
             stepPosition(unstep[ls.ordinal()]);
             return ls;
         }
         /**
          * Unstep current position in oppisite director of given step
          */
         public void unstepPosition(BlockStep s) {
             stepPosition(unstep[s.ordinal()]);
         }
         public final void setY(int y) {
             if(y > this.y)
                 laststep = BlockStep.Y_PLUS;
             else
                 laststep = BlockStep.Y_MINUS;
             this.y = y;
             typeid = -1;
             blkdata = -1;
         }
         public final int getX() {
             return x;
         }
         public final int getY() {
             return y;
         }
         public final int getZ() {
             return z;
         }
         public final int getBlockTypeIDAt(BlockStep s) {
             if(s == BlockStep.Y_MINUS) {
                 if(y > 0)
                     return snap.getBlockTypeId(bx, y-1, bz);
             }
             else if(s == BlockStep.Y_PLUS) {
                 if(y < (worldheight-1))
                     return snap.getBlockTypeId(bx, y+1, bz);
             }
             else {
                 BlockStep ls = laststep;
                 stepPosition(s);
                 int tid = snap.getBlockTypeId(bx, y, bz);
                 unstepPosition();
                 laststep = ls;
                 return tid;
             }
             return 0;
         }
         public BlockStep getLastStep() {
             return laststep;
         }
         @Override
         public int getWorldHeight() {
             return worldheight;
         }
         @Override
         public long getBlockKey() {
             return (((chunkindex * worldheight) + y) << 8) | (bx << 4) | bz;
         }
         @Override
         public final boolean isEmptySection() {
             if(isSectionNotEmpty[chunkindex] == null)
                 initSectionData(chunkindex);
              return !isSectionNotEmpty[chunkindex][y >> 4];
         }
      }
 
     private class OurEndMapIterator extends OurMapIterator {
 
         OurEndMapIterator(int x0, int y0, int z0) {
             super(x0, y0, z0);
         }
         public final int getBlockSkyLight() {
             return 15;
         }
     }
     /**
      * Chunk cache for representing unloaded chunk (or air)
      */
     private static class EmptyChunk implements ChunkSnapshot {
         /* Need these for interface, but not used */
         public int getX() { return 0; }
         public int getZ() { return 0; }
         public String getWorldName() { return ""; }
         public long getCaptureFullTime() { return 0; }
         
         public final int getBlockTypeId(int x, int y, int z) {
             return 0;
         }
         public final int getBlockData(int x, int y, int z) {
             return 0;
         }
         public final int getBlockSkyLight(int x, int y, int z) {
             return 15;
         }
         public final int getBlockEmittedLight(int x, int y, int z) {
             return 0;
         }
         public final int getHighestBlockYAt(int x, int z) {
             return 0;
         }
         public Biome getBiome(int x, int z) {
             return null;
         }
         public double getRawBiomeTemperature(int x, int z) {
             return 0.0;
         }
         public double getRawBiomeRainfall(int x, int z) {
             return 0.0;
         }
         public boolean isSectionEmpty(int sy) {
             return true;
         }
         public int getTopNonEmptySection() {
             return -1;
         }
     }
 
     /**
      * Chunk cache for representing generic stone chunk
      */
     private static class PlainChunk implements ChunkSnapshot {
         private int fillid;
         PlainChunk(int fillid) { this.fillid = fillid; }
         /* Need these for interface, but not used */
         public int getX() { return 0; }
         public int getZ() { return 0; }
         public String getWorldName() { return ""; }
         public Biome getBiome(int x, int z) { return null; }
         public double getRawBiomeTemperature(int x, int z) { return 0.0; }
         public double getRawBiomeRainfall(int x, int z) { return 0.0; }
         public long getCaptureFullTime() { return 0; }
         
         public final int getBlockTypeId(int x, int y, int z) {
             if(y < 64) return fillid;
             return 0;
         }
         public final int getBlockData(int x, int y, int z) {
             return 0;
         }
         public final int getBlockSkyLight(int x, int y, int z) {
             if(y < 64)
                 return 0;
             return 15;
         }
         public final int getBlockEmittedLight(int x, int y, int z) {
             return 0;
         }
         public final int getHighestBlockYAt(int x, int z) {
             return 64;
         }
         public boolean isSectionEmpty(int sy) {
             return (sy < 4);
         }
         public int getTopNonEmptySection() {
             return 3;
         }
     }
     
     private static class SpoutChunkSnapshot implements ChunkSnapshot {
         private ChunkSnapshot chunk;
         private short[] customids; /* (X << 11) | (Z << 7) | Y */ 
         
         SpoutChunkSnapshot(ChunkSnapshot chunk, short[] customids) {
             this.chunk = chunk;
             this.customids = customids.clone();
         }
         /* Need these for interface, but not used */
         public final int getX() { return chunk.getX(); }
         public final int getZ() { return chunk.getZ(); }
         public final String getWorldName() { return chunk.getWorldName(); }
         public final Biome getBiome(int x, int z) { return chunk.getBiome(x, z); }
         public final double getRawBiomeTemperature(int x, int z) { return chunk.getRawBiomeTemperature(x, z); }
         public final double getRawBiomeRainfall(int x, int z) { return chunk.getRawBiomeRainfall(x, z); }
         public final long getCaptureFullTime() { return chunk.getCaptureFullTime(); }
         
         public final int getBlockTypeId(int x, int y, int z) {
             int id = customids[(x << 11) | (z << 7) | y];
             if(id != 0) return id;
             return chunk.getBlockTypeId(x, y, z);
         }
         public final int getBlockData(int x, int y, int z) {
             return chunk.getBlockData(x, y, z);
         }
         public final int getBlockSkyLight(int x, int y, int z) {
             return chunk.getBlockSkyLight(x, y, z);
         }
         public final int getBlockEmittedLight(int x, int y, int z) {
             return chunk.getBlockEmittedLight(x, y, z);
         }
         public final int getHighestBlockYAt(int x, int z) {
             return chunk.getHighestBlockYAt(x, z);
         }
         public boolean isSectionEmpty(int sy) {
             //TODO return chunk.isSectionEmpty(sy);
             return false;
         }
         public int getTopNonEmptySection() {
             return 0;
             //TODO return chunk.getTopNonEmptySection();
         }
     }
 
     private static final EmptyChunk EMPTY = new EmptyChunk();
     private static final PlainChunk STONE = new PlainChunk(1);
     private static final PlainChunk OCEAN = new PlainChunk(9);
 
     /**
      * Construct empty cache
      */
     @SuppressWarnings({ "rawtypes", "unchecked" })
     public NewMapChunkCache() {
         if(!init) {
             /* Get CraftWorld.popPreservedChunk(x,z) - reduces memory bloat from map traversals (optional) */
             try {
                 Class c = Class.forName("org.bukkit.craftbukkit.CraftWorld");
                 poppreservedchunk = c.getDeclaredMethod("popPreservedChunk", new Class[] { int.class, int.class });
                 /* getHandle() */
                 getworldhandle = c.getDeclaredMethod("getHandle", new Class[0]);
             } catch (ClassNotFoundException cnfx) {
             } catch (NoSuchMethodException nsmx) {
             }
             /* Get CraftChunk.getChunkSnapshot(boolean,boolean,boolean) and CraftChunk.getHandle() */
             try {
                 Class c = Class.forName("org.bukkit.craftbukkit.CraftChunk");
                 gethandle = c.getDeclaredMethod("getHandle", new Class[0]);
             } catch (ClassNotFoundException cnfx) {
             } catch (NoSuchMethodException nsmx) {
             }
             /* Get Chunk.removeEntities() */
             try {
                 Class c = Class.forName("net.minecraft.server.Chunk");
                 removeentities = c.getDeclaredMethod("removeEntities", new Class[0]);
             } catch (ClassNotFoundException cnfx) {
             } catch (NoSuchMethodException nsmx) {
             }
                         
             /* Get CraftChunkSnapshot.biome field */
             try {
                 Class c = Class.forName("org.bukkit.craftbukkit.CraftChunkSnapshot");
                 chunkbiome = c.getDeclaredField("biome");
                 chunkbiome.setAccessible(true);
             } catch (ClassNotFoundException cnfx) {
             } catch (NoSuchFieldException nsmx) {
             }
             use_spout = DynmapPlugin.plugin.hasSpout();
             
             init = true;
         }
     }
     public void setChunks(BukkitWorld dw, List<DynmapChunk> chunks) {
         this.dw = dw;
         this.w = dw.getWorld();
         if((getworldhandle != null) && (craftworld == null)) {
             try {
                 craftworld = getworldhandle.invoke(w);   /* World.getHandle() */
             } catch (Exception x) {
             }
         }
         this.chunks = chunks;
         /* Compute range */
         if(chunks.size() == 0) {
             this.x_min = 0;
             this.x_max = 0;
             this.z_min = 0;
             this.z_max = 0;
             x_dim = 1;            
         }
         else {
             x_min = x_max = chunks.get(0).x;
             z_min = z_max = chunks.get(0).z;
             for(DynmapChunk c : chunks) {
                 if(c.x > x_max)
                     x_max = c.x;
                 if(c.x < x_min)
                     x_min = c.x;
                 if(c.z > z_max)
                     z_max = c.z;
                 if(c.z < z_min)
                     z_min = c.z;
             }
             x_dim = x_max - x_min + 1;            
         }
     
         snaparray = new ChunkSnapshot[x_dim * (z_max-z_min+1)];
         isSectionNotEmpty = new boolean[x_dim * (z_max-z_min+1)][];
     }
     
     private ChunkSnapshot checkSpoutData(Chunk c, ChunkSnapshot ss) {
         if(c instanceof SpoutChunk) {
             SpoutChunk sc = (SpoutChunk)c;
             short[] custids = sc.getCustomBlockIds();
             if(custids != null) {
                 return new SpoutChunkSnapshot(ss, custids);
             }
         }
         return ss;
     }
 
     public int loadChunks(int max_to_load) {
         long t0 = System.nanoTime();
         int cnt = 0;
         if(iterator == null)
             iterator = chunks.listIterator();
 
         DynmapCore.setIgnoreChunkLoads(true);
         //boolean isnormral = w.getEnvironment() == Environment.NORMAL;
         // Load the required chunks.
         while((cnt < max_to_load) && iterator.hasNext()) {
             DynmapChunk chunk = iterator.next();
             boolean vis = true;
             if(visible_limits != null) {
                 vis = false;
                 for(VisibilityLimit limit : visible_limits) {
                     if((chunk.x >= limit.x0) && (chunk.x <= limit.x1) && (chunk.z >= limit.z0) && (chunk.z <= limit.z1)) {
                         vis = true;
                         break;
                     }
                 }
             }
             if(vis && (hidden_limits != null)) {
                 for(VisibilityLimit limit : hidden_limits) {
                     if((chunk.x >= limit.x0) && (chunk.x <= limit.x1) && (chunk.z >= limit.z0) && (chunk.z <= limit.z1)) {
                         vis = false;
                         break;
                     }
                 }
             }
             /* Check if cached chunk snapshot found */
             ChunkSnapshot ss = DynmapPlugin.plugin.sscache.getSnapshot(w.getName(), chunk.x, chunk.z, blockdata, biome, biomeraw, highesty); 
             if(ss != null) {
                 if(!vis) {
                     if(hidestyle == HiddenChunkStyle.FILL_STONE_PLAIN)
                         ss = STONE;
                     else if(hidestyle == HiddenChunkStyle.FILL_OCEAN)
                         ss = OCEAN;
                     else
                         ss = EMPTY;
                 }
                 snaparray[(chunk.x-x_min) + (chunk.z - z_min)*x_dim] = ss;
                 continue;
             }
             chunks_attempted++;
             boolean wasLoaded = w.isChunkLoaded(chunk.x, chunk.z);
             boolean didload = w.loadChunk(chunk.x, chunk.z, false);
             boolean didgenerate = false;
             /* If we didn't load, and we're supposed to generate, do it */
             if((!didload) && do_generate && vis)
                 didgenerate = didload = w.loadChunk(chunk.x, chunk.z, true);
             /* If it did load, make cache of it */
             if(didload) {
                 if(!vis) {
                     if(hidestyle == HiddenChunkStyle.FILL_STONE_PLAIN)
                         ss = STONE;
                     else if(hidestyle == HiddenChunkStyle.FILL_OCEAN)
                         ss = OCEAN;
                     else
                         ss = EMPTY;
                 }
                 else {
                     Chunk c = w.getChunkAt(chunk.x, chunk.z);
                     if(blockdata || highesty) {
                         ss = c.getChunkSnapshot(highesty, biome, biomeraw);
                         if(use_spout) {
                             ss = checkSpoutData(c, ss);
                         }
                     }
                     else
                         ss = w.getEmptyChunkSnapshot(chunk.x, chunk.z, biome, biomeraw);
                     if(ss != null) {
                         DynmapPlugin.plugin.sscache.putSnapshot(w.getName(), chunk.x, chunk.z, ss, blockdata, biome, biomeraw, highesty);
                     }
                 }
                 snaparray[(chunk.x-x_min) + (chunk.z - z_min)*x_dim] = ss;
             }
             if ((!wasLoaded) && didload) {
                 chunks_read++;
                 /* It looks like bukkit "leaks" entities - they don't get removed from the world-level table
                  * when chunks are unloaded but not saved - removing them seems to do the trick */
                 if(!(didgenerate && do_save)) {
                     boolean did_remove = false;
                     Chunk cc = w.getChunkAt(chunk.x, chunk.z);
                     if((gethandle != null) && (removeentities != null)) {
                         try {
                             Object chk = gethandle.invoke(cc);
                             if(chk != null) {
                                 removeentities.invoke(chk);
                                 did_remove = true;
                             }
                         } catch (InvocationTargetException itx) {
                         } catch (IllegalArgumentException e) {
                         } catch (IllegalAccessException e) {
                         }
                     }
                     if(!did_remove) {
                         if(cc != null) {
                             for(Entity e: cc.getEntities())
                                 e.remove();
                         }
                     }
                 }
                 /* Since we only remember ones we loaded, and we're synchronous, no player has
                  * moved, so it must be safe (also prevent chunk leak, which appears to happen
                  * because isChunkInUse defined "in use" as being within 256 blocks of a player,
                  * while the actual in-use chunk area for a player where the chunks are managed
                  * by the MC base server is 21x21 (or about a 160 block radius).
                  * Also, if we did generate it, need to save it */
                 w.unloadChunk(chunk.x, chunk.z, didgenerate && do_save, false);
                 /* And pop preserved chunk - this is a bad leak in Bukkit for map traversals like us */
                 try {
                     if(poppreservedchunk != null)
                         poppreservedchunk.invoke(w, chunk.x, chunk.z);
                 } catch (Exception x) {
                     Log.severe("Cannot pop preserved chunk - " + x.toString());
                 }
             }
             cnt++;
         }
         DynmapCore.setIgnoreChunkLoads(false);
 
         if(iterator.hasNext() == false) {   /* If we're done */
             isempty = true;
             /* Fill missing chunks with empty dummy chunk */
             for(int i = 0; i < snaparray.length; i++) {
                 if(snaparray[i] == null)
                     snaparray[i] = EMPTY;
                 else if(snaparray[i] != EMPTY)
                     isempty = false;
             }
         }
         total_loadtime += System.nanoTime() - t0;
 
         return cnt;
     }
     /**
      * Test if done loading
      */
     public boolean isDoneLoading() {
         if(iterator != null)
             return !iterator.hasNext();
         return false;
     }
     /**
      * Test if all empty blocks
      */
     public boolean isEmpty() {
         return isempty;
     }
     /**
      * Unload chunks
      */
     public void unloadChunks() {
         if(snaparray != null) {
             for(int i = 0; i < snaparray.length; i++) {
                 snaparray[i] = null;
             }
             snaparray = null;
         }
     }
     /**
      * Get block ID at coordinates
      */
     public int getBlockTypeID(int x, int y, int z) {
         ChunkSnapshot ss = snaparray[((x>>4) - x_min) + ((z>>4) - z_min) * x_dim];
         return ss.getBlockTypeId(x & 0xF, y, z & 0xF);
     }
     /**
      * Get block data at coordiates
      */
     public byte getBlockData(int x, int y, int z) {
         ChunkSnapshot ss = snaparray[((x>>4) - x_min) + ((z>>4) - z_min) * x_dim];
         return (byte)ss.getBlockData(x & 0xF, y, z & 0xF);
     }
     /* Get sky light level
      */
     public int getBlockSkyLight(int x, int y, int z) {
         ChunkSnapshot ss = snaparray[((x>>4) - x_min) + ((z>>4) - z_min) * x_dim];
         return ss.getBlockSkyLight(x & 0xF, y, z & 0xF);
     }
     /* Get emitted light level
      */
     public int getBlockEmittedLight(int x, int y, int z) {
         ChunkSnapshot ss = snaparray[((x>>4) - x_min) + ((z>>4) - z_min) * x_dim];
         return ss.getBlockEmittedLight(x & 0xF, y, z & 0xF);
     }
     public BiomeMap getBiome(int x, int z) {
         ChunkSnapshot ss = snaparray[((x>>4) - x_min) + ((z>>4) - z_min) * x_dim];
         Biome b = ss.getBiome(x & 0xF, z & 0xF);
         return (b != null)?biome_to_bmap[b.ordinal()]:null;
     }
     public double getRawBiomeTemperature(int x, int z) {
         ChunkSnapshot ss = snaparray[((x>>4) - x_min) + ((z>>4) - z_min) * x_dim];
         return ss.getRawBiomeTemperature(x & 0xF, z & 0xF);
     }
     public double getRawBiomeRainfall(int x, int z) {
         ChunkSnapshot ss = snaparray[((x>>4) - x_min) + ((z>>4) - z_min) * x_dim];
         return ss.getRawBiomeRainfall(x & 0xF, z & 0xF);
     }
     private void initSectionData(int idx) {
        isSectionNotEmpty[idx] = new boolean[w.getMaxHeight() >> 16];
         int maxy = 0;
         if(snaparray[idx] != EMPTY) {
             /* Get max height */
             for(int i = 0; i < 16; i++) {
                 for(int j = 0; j < 16; j++) {
                     maxy = Math.max(maxy, snaparray[idx].getHighestBlockYAt(i, j));
                 }
             }
             for(int i = 0; i < isSectionNotEmpty[idx].length; i++) {
                 if((i << 4) < maxy) { /* Below top? */
                     isSectionNotEmpty[idx][i] = true;
                 }
             }
         }
     }
     public boolean isEmptySection(int x, int y, int z) {
         int idx = ((x>>4) - x_min) + ((z>>4) - z_min) * x_dim;
         if(isSectionNotEmpty[idx] == null) {
             initSectionData(idx);
         }
         return isSectionNotEmpty[idx][y >> 4];
     }
 
     /**
      * Get cache iterator
      */
     public MapIterator getIterator(int x, int y, int z) {
         if(w.getEnvironment().toString().equals("THE_END"))
             return new OurEndMapIterator(x, y, z);
         return new OurMapIterator(x, y, z);
     }
     /**
      * Set hidden chunk style (default is FILL_AIR)
      */
     public void setHiddenFillStyle(HiddenChunkStyle style) {
         this.hidestyle = style;
     }
     /**
      * Set autogenerate - must be done after at least one visible range has been set
      */
     public void setAutoGenerateVisbileRanges(DynmapWorld.AutoGenerateOption generateopt) {
         if((generateopt != DynmapWorld.AutoGenerateOption.NONE) && ((visible_limits == null) || (visible_limits.size() == 0))) {
             Log.severe("Cannot setAutoGenerateVisibleRanges() without visible ranges defined");
             return;
         }
         this.do_generate = (generateopt != DynmapWorld.AutoGenerateOption.NONE);
         this.do_save = (generateopt == DynmapWorld.AutoGenerateOption.PERMANENT);
     }
     /**
      * Add visible area limit - can be called more than once 
      * Needs to be set before chunks are loaded
      * Coordinates are block coordinates
      */
     public void setVisibleRange(VisibilityLimit lim) {
         VisibilityLimit limit = new VisibilityLimit();
         if(lim.x0 > lim.x1) {
             limit.x0 = (lim.x1 >> 4); limit.x1 = ((lim.x0+15) >> 4);
         }
         else {
             limit.x0 = (lim.x0 >> 4); limit.x1 = ((lim.x1+15) >> 4);
         }
         if(lim.z0 > lim.z1) {
             limit.z0 = (lim.z1 >> 4); limit.z1 = ((lim.z0+15) >> 4);
         }
         else {
             limit.z0 = (lim.z0 >> 4); limit.z1 = ((lim.z1+15) >> 4);
         }
         if(visible_limits == null)
             visible_limits = new ArrayList<VisibilityLimit>();
         visible_limits.add(limit);
     }
     /**
      * Add hidden area limit - can be called more than once 
      * Needs to be set before chunks are loaded
      * Coordinates are block coordinates
      */
     public void setHiddenRange(VisibilityLimit lim) {
         VisibilityLimit limit = new VisibilityLimit();
         if(lim.x0 > lim.x1) {
             limit.x0 = (lim.x1 >> 4); limit.x1 = ((lim.x0+15) >> 4);
         }
         else {
             limit.x0 = (lim.x0 >> 4); limit.x1 = ((lim.x1+15) >> 4);
         }
         if(lim.z0 > lim.z1) {
             limit.z0 = (lim.z1 >> 4); limit.z1 = ((lim.z0+15) >> 4);
         }
         else {
             limit.z0 = (lim.z0 >> 4); limit.z1 = ((lim.z1+15) >> 4);
         }
         if(hidden_limits == null)
             hidden_limits = new ArrayList<VisibilityLimit>();
         hidden_limits.add(limit);
     }
     @Override
     public boolean setChunkDataTypes(boolean blockdata, boolean biome, boolean highestblocky, boolean rawbiome) {
         this.biome = biome;
         this.biomeraw = rawbiome;
         this.highesty = highestblocky;
         this.blockdata = blockdata;
         return true;
     }
     @Override
     public DynmapWorld getWorld() {
         return dw;
     }
     @Override
     public int getChunksLoaded() {
         return chunks_read;
     }
     @Override
     public int getChunkLoadsAttempted() {
         return chunks_attempted;
     }
     @Override
     public long getTotalRuntimeNanos() {
         return total_loadtime;
     }
     @Override
     public long getExceptionCount() {
         return exceptions;
     }
     
     static {
         Biome[] b = Biome.values();
         BiomeMap[] bm = BiomeMap.values();
         biome_to_bmap = new BiomeMap[b.length];
         for(int i = 0; i < b.length; i++) {
             String bs = b[i].toString();
             for(int j = 0; j < bm.length; j++) {
                 if(bm[j].toString().equals(bs)) {
                     biome_to_bmap[b[i].ordinal()] = bm[j];
                     break;
                 }
             }
         }
     }
 }
