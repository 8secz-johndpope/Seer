 package org.dynmap.hdmap;
 
 import static org.dynmap.JSONUtils.s;
 
 import org.dynmap.DynmapWorld;
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 
 import org.dynmap.Client;
 import org.dynmap.Color;
 import org.dynmap.ConfigurationNode;
 import org.dynmap.DynmapChunk;
 import org.dynmap.DynmapCore;
 import org.dynmap.DynmapCore.CompassMode;
 import org.dynmap.Log;
 import org.dynmap.MapManager;
 import org.dynmap.MapTile;
 import org.dynmap.MapType;
 import org.dynmap.MapType.ImageFormat;
 import org.dynmap.TileHashManager;
 import org.dynmap.debug.Debug;
 import org.dynmap.renderer.RenderPatch;
 import org.dynmap.renderer.RenderPatchFactory.SideVisible;
 import org.dynmap.utils.BlockStep;
 import org.dynmap.hdmap.HDBlockModels.CustomBlockModel;
 import org.dynmap.hdmap.TexturePack.BlockTransparency;
 import org.dynmap.hdmap.TexturePack.HDTextureMap;
 import org.dynmap.utils.DynmapBufferedImage;
 import org.dynmap.utils.FileLockManager;
 import org.dynmap.utils.LightLevels;
 import org.dynmap.utils.DynLongHashMap;
 import org.dynmap.utils.MapChunkCache;
 import org.dynmap.utils.MapIterator;
 import org.dynmap.utils.Matrix3D;
 import org.dynmap.utils.PatchDefinition;
 import org.dynmap.utils.TileFlags;
 import org.dynmap.utils.Vector3D;
 import org.json.simple.JSONObject;
 
 public class IsoHDPerspective implements HDPerspective {
     private String name;
     /* View angles */
     public double azimuth;  /* Angle in degrees from looking north (0), east (90), south (180), or west (270) */
     public double inclination;  /* Angle in degrees from horizontal (0) to vertical (90) */
     public double scale;    /* Scale - tile pixel widths per block */
     public double maxheight;
     public double minheight;
     /* Coordinate space for tiles consists of a plane (X, Y), corresponding to the projection of each tile on to the
      * plane of the bottom of the world (X positive to the right, Y positive to the top), with Z+ corresponding to the
      * height above this plane on a vector towards the viewer).  Logically, this makes the parallelogram representing the
      * space contributing to the tile have consistent tile-space X,Y coordinate pairs for both the top and bottom faces
      * Note that this is a classic right-hand coordinate system, while minecraft's world coordinates are left handed 
      * (X+ is south, Y+ is up, Z+ is east). 
      */
     /* Transformation matrix for taking coordinate in world-space (x, y, z) and finding coordinate in tile space (x, y, z) */
     private Matrix3D world_to_map;
     private Matrix3D map_to_world;
     
     /* Scaled models for non-cube blocks */
     private HDBlockModels.HDScaledBlockModels scalemodels;
     private int modscale;
     
     /* dimensions of a map tile */
     public static final int tileWidth = 128;
     public static final int tileHeight = 128;
 
     /* Maximum and minimum inclinations */
     public static final double MAX_INCLINATION = 90.0;
     public static final double MIN_INCLINATION = 20.0;
     
     /* Maximum and minimum scale */
     public static final double MAX_SCALE = 64;
     public static final double MIN_SCALE = 1;
     
     private boolean need_biomedata = false;
     private boolean need_rawbiomedata = false;
 
     private static final int REDSTONE_BLKTYPEID = 55;
     private static final int FENCEGATE_BLKTYPEID = 107;
     
     private enum ChestData {
         SINGLE_WEST, SINGLE_SOUTH, SINGLE_EAST, SINGLE_NORTH, LEFT_WEST, LEFT_SOUTH, LEFT_EAST, LEFT_NORTH, RIGHT_WEST, RIGHT_SOUTH, RIGHT_EAST, RIGHT_NORTH
     };
     private static final BlockStep [] semi_steps = { BlockStep.Y_PLUS, BlockStep.X_MINUS, BlockStep.X_PLUS, BlockStep.Z_MINUS, BlockStep.Z_PLUS };
 
     private class OurPerspectiveState implements HDPerspectiveState {
         int blocktypeid = 0;
         int blockdata = 0;
         int blockrenderdata = -1;
         int lastblocktypeid = 0;
         Vector3D top, bottom, direction;
         int px, py;
         BlockStep laststep = BlockStep.Y_MINUS;
         
         BlockStep stepx, stepy, stepz;
 
         /* Section-level raytrace variables */
         int sx, sy, sz;
         double sdt_dx, sdt_dy, sdt_dz;
         double st_next_x, st_next_y, st_next_z;
         /* Raytrace state variables */
         double dx, dy, dz;
         int x, y, z;
         double dt_dx, dt_dy, dt_dz, t;
         int n;
         int x_inc, y_inc, z_inc;        
         double t_next_y, t_next_x, t_next_z;
         boolean nonairhit;
         /* Subblock tracer state */
         int mx, my, mz;
         double xx, yy, zz;
         double mdt_dx;
         double mdt_dy;
         double mdt_dz;
         double togo;
         double mt_next_x, mt_next_y, mt_next_z;
         int subalpha;
         double mt;
         double mtend;
         int mxout, myout, mzout;
         /* Patch state and work variables */
         Vector3D v0 = new Vector3D();
         Vector3D vS = new Vector3D();
         Vector3D d_cross_uv = new Vector3D();
         double patch_t[] = new double[HDBlockModels.getMaxPatchCount()];
         double patch_u[] = new double[HDBlockModels.getMaxPatchCount()];
         double patch_v[] = new double[HDBlockModels.getMaxPatchCount()];
         BlockStep patch_step[] = new BlockStep[HDBlockModels.getMaxPatchCount()];
         int patch_id[] = new int[HDBlockModels.getMaxPatchCount()];
         int cur_patch = -1;
         double cur_patch_u;
         double cur_patch_v;
        double cur_patch_t;
         
         int[] subblock_xyz = new int[3];
         MapIterator mapiter;
         boolean isnether;
         boolean skiptoair;
         int worldheight;
         int heightmask;
         LightLevels llcache[];
         
         /* Cache for custom model patch lists */
         private DynLongHashMap custom_meshes;
 
         public OurPerspectiveState(MapIterator mi, boolean isnether) {
             mapiter = mi;
             this.isnether = isnether;
             worldheight = mapiter.getWorldHeight();
             int shift;
             for(shift = 0; (1<<shift) < worldheight; shift++) {}
             heightmask = (1<<shift) - 1;
             llcache = new LightLevels[4];
             for(int i = 0; i < llcache.length; i++)
                 llcache[i] = new LightLevels();
             custom_meshes = new DynLongHashMap();
         }
         private final void updateSemitransparentLight(LightLevels ll) {
         	int emitted = 0, sky = 0;
         	for(int i = 0; i < semi_steps.length; i++) {
         	    BlockStep s = semi_steps[i];
         		mapiter.stepPosition(s);
         		int v = mapiter.getBlockEmittedLight();
         		if(v > emitted) emitted = v;
         		v = mapiter.getBlockSkyLight();
         		if(v > sky) sky = v;
         		mapiter.unstepPosition(s);
         	}
         	ll.sky = sky;
         	ll.emitted = emitted;
         }
         /**
          * Update sky and emitted light 
          */
         private final void updateLightLevel(int blktypeid, LightLevels ll) {
             /* Look up transparency for current block */
             BlockTransparency bt = HDTextureMap.getTransparency(blktypeid);
             switch(bt) {
             	case TRANSPARENT:
             		ll.sky = mapiter.getBlockSkyLight();
             		ll.emitted = mapiter.getBlockEmittedLight();
             		break;
             	case OPAQUE:
         			if(HDTextureMap.getTransparency(lastblocktypeid) != BlockTransparency.SEMITRANSPARENT) {
                 		mapiter.unstepPosition(laststep);  /* Back up to block we entered on */
                 		if(mapiter.getY() < worldheight) {
                 		    ll.sky = mapiter.getBlockSkyLight();
                 		    ll.emitted = mapiter.getBlockEmittedLight();
                 		} else {
                 		    ll.sky = 15;
                 		    ll.emitted = 0;
                 		}
                 		mapiter.stepPosition(laststep);
         			}
         			else {
                 		mapiter.unstepPosition(laststep);  /* Back up to block we entered on */
                 		updateSemitransparentLight(ll);
                 		mapiter.stepPosition(laststep);
         			}
         			break;
             	case SEMITRANSPARENT:
             		updateSemitransparentLight(ll);
             		break;
         		default:
                     ll.sky = mapiter.getBlockSkyLight();
                     ll.emitted = mapiter.getBlockEmittedLight();
                     break;
             }
         }
         /**
          * Get light level - only available if shader requested it
          */
         public final void getLightLevels(LightLevels ll) {
             updateLightLevel(blocktypeid, ll);
         }
         /**
          * Get sky light level - only available if shader requested it
          */
         public final void getLightLevelsAtStep(BlockStep step, LightLevels ll) {
             if(((step == BlockStep.Y_MINUS) && (y == 0)) ||
                     ((step == BlockStep.Y_PLUS) && (y == worldheight))) {
                 getLightLevels(ll);
                 return;
             }
             BlockStep blast = laststep;
             mapiter.stepPosition(step);
             laststep = blast;
             updateLightLevel(mapiter.getBlockTypeID(), ll);
             mapiter.unstepPosition(step);
             laststep = blast;
         }
         /**
          * Get current block type ID
          */
         public final int getBlockTypeID() { return blocktypeid; }
         /**
          * Get current block data
          */
         public final int getBlockData() { return blockdata; }
         /**
          * Get current block render data
          */
         public final int getBlockRenderData() { return blockrenderdata; }
         /**
          * Get direction of last block step
          */
         public final BlockStep getLastBlockStep() { return laststep; }
         /**
          * Get perspective scale
          */
         public final double getScale() { return scale; }
         /**
          * Get start of current ray, in world coordinates
          */
         public final Vector3D getRayStart() { return top; }
         /**
          * Get end of current ray, in world coordinates
          */
         public final Vector3D getRayEnd() { return bottom; }
         /**
          * Get pixel X coordinate
          */
         public final int getPixelX() { return px; }
         /**
          * Get pixel Y coordinate
          */
         public final int getPixelY() { return py; }
         /**
          * Get map iterator
          */
         public final MapIterator getMapIterator() { return mapiter; }
         /**
          * Return submodel alpha value (-1 if no submodel rendered)
          */
         public int getSubmodelAlpha() {
             return subalpha;
         }
         /**
          * Initialize raytrace state variables
          */
         private void raytrace_init() {
             /* Compute total delta on each axis */
             dx = Math.abs(direction.x);
             dy = Math.abs(direction.y);
             dz = Math.abs(direction.z);
             /* Compute parametric step (dt) per step on each axis */
             dt_dx = 1.0 / dx;
             dt_dy = 1.0 / dy;
             dt_dz = 1.0 / dz;
             /* Initialize parametric value to 0 (and we're stepping towards 1) */
             t = 0;
             /* Compute number of steps and increments for each */
             n = 1;
 
             /* Initial section coord */
             sx = fastFloor(top.x/16.0);
             sy = fastFloor(top.y/16.0);
             sz = fastFloor(top.z/16.0);
             /* Compute parametric step (dt) per step on each axis */
             sdt_dx = 16.0 / dx;
             sdt_dy = 16.0 / dy;
             sdt_dz = 16.0 / dz;
             
             /* If perpendicular to X axis */
             if (dx == 0) {
                 x_inc = 0;
                 st_next_x = Double.MAX_VALUE;
                 stepx = BlockStep.X_PLUS;
                 mxout = modscale;
             }
             /* If bottom is right of top */
             else if (bottom.x > top.x) {
                 x_inc = 1;
                 n += fastFloor(bottom.x) - x;
                 st_next_x = (fastFloor(top.x/16.0) + 1 - (top.x/16.0)) * sdt_dx;
                 stepx = BlockStep.X_PLUS;
                 mxout = modscale;
             }
             /* Top is right of bottom */
             else {
                 x_inc = -1;
                 n += x - fastFloor(bottom.x);
                 st_next_x = ((top.x/16.0) - fastFloor(top.x/16.0)) * sdt_dx;
                 stepx = BlockStep.X_MINUS;
                 mxout = -1;
             }
             /* If perpendicular to Y axis */
             if (dy == 0) {
                 y_inc = 0;
                 st_next_y = Double.MAX_VALUE;
                 stepy = BlockStep.Y_PLUS;
                 myout = modscale;
             }
             /* If bottom is above top */
             else if (bottom.y > top.y) {
                 y_inc = 1;
                 n += fastFloor(bottom.y) - y;
                 st_next_y = (fastFloor(top.y/16.0) + 1 - (top.y/16.0)) * sdt_dy;
                 stepy = BlockStep.Y_PLUS;
                 myout = modscale;
             }
             /* If top is above bottom */
             else {
                 y_inc = -1;
                 n += y - fastFloor(bottom.y);
                 st_next_y = ((top.y/16.0) - fastFloor(top.y/16.0)) * sdt_dy;
                 stepy = BlockStep.Y_MINUS;
                 myout = -1;
             }
             /* If perpendicular to Z axis */
             if (dz == 0) {
                 z_inc = 0;
                 st_next_z = Double.MAX_VALUE;
                 stepz = BlockStep.Z_PLUS;
                 mzout = modscale;
             }
             /* If bottom right of top */
             else if (bottom.z > top.z) {
                 z_inc = 1;
                 n += fastFloor(bottom.z) - z;
                 st_next_z = (fastFloor(top.z/16.0) + 1 - (top.z/16.0)) * sdt_dz;
                 stepz = BlockStep.Z_PLUS;
                 mzout = modscale;
             }
             /* If bottom left of top */
             else {
                 z_inc = -1;
                 n += z - fastFloor(bottom.z);
                 st_next_z = ((top.z/16.0) - fastFloor(top.z/16.0)) * sdt_dz;
                 stepz = BlockStep.Z_MINUS;
                 mzout = -1;
             }
             /* Walk through scene */
             laststep = BlockStep.Y_MINUS; /* Last step is down into map */
             nonairhit = false;
             skiptoair = isnether;
         }
         private int generateFenceBlockData(MapIterator mapiter, int blkid) {
             int blockdata = 0;
             int id;
             /* Check north */
             id = mapiter.getBlockTypeIDAt(BlockStep.X_MINUS);
             if((id == blkid) || (id == FENCEGATE_BLKTYPEID) || 
                     ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {    /* Fence? */
                 blockdata |= 1;
             }
             /* Look east */
             id = mapiter.getBlockTypeIDAt(BlockStep.Z_MINUS);
             if((id == blkid) || (id == FENCEGATE_BLKTYPEID) ||
                     ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {    /* Fence? */
                 blockdata |= 2;
             }
             /* Look south */
             id = mapiter.getBlockTypeIDAt(BlockStep.X_PLUS);
             if((id == blkid) || (id == FENCEGATE_BLKTYPEID) ||
                     ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {    /* Fence? */
                 blockdata |= 4;
             }
             /* Look west */
             id = mapiter.getBlockTypeIDAt(BlockStep.Z_PLUS);
             if((id == blkid) || (id == FENCEGATE_BLKTYPEID) ||
                     ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {    /* Fence? */
                 blockdata |= 8;
             }
             return blockdata;
         }
         /**
          * Generate chest block to drive model selection:
          *   0 = single facing west
          *   1 = single facing south
          *   2 = single facing east
          *   3 = single facing north
          *   4 = left side facing west
          *   5 = left side facing south
          *   6 = left side facing east
          *   7 = left side facing north
          *   8 = right side facing west
          *   9 = right side facing south
          *   10 = right side facing east
          *   11 = right side facing north
          * @param mapiter
          * @return
          */
         private int generateChestBlockData(MapIterator mapiter, int blktype) {
             int blkdata = mapiter.getBlockData();   /* Get block data */
             ChestData cd = ChestData.SINGLE_WEST;   /* Default to single facing west */
             switch(blkdata) {   /* First, use orientation data */
                 case 2: /* East (now north) */
                     if(mapiter.getBlockTypeIDAt(BlockStep.X_MINUS) == blktype) { /* Check north */
                         cd = ChestData.LEFT_EAST;
                     }
                     else if(mapiter.getBlockTypeIDAt(BlockStep.X_PLUS) == blktype) {    /* Check south */
                         cd = ChestData.RIGHT_EAST;
                     }
                     else {
                         cd = ChestData.SINGLE_EAST;
                     }
                     break;
                 case 4: /* North */
                     if(mapiter.getBlockTypeIDAt(BlockStep.Z_MINUS) == blktype) { /* Check east */
                         cd = ChestData.RIGHT_NORTH;
                     }
                     else if(mapiter.getBlockTypeIDAt(BlockStep.Z_PLUS) == blktype) {    /* Check west */
                         cd = ChestData.LEFT_NORTH;
                     }
                     else {
                         cd = ChestData.SINGLE_NORTH;
                     }
                     break;
                 case 5: /* South */
                     if(mapiter.getBlockTypeIDAt(BlockStep.Z_MINUS) == blktype) { /* Check east */
                         cd = ChestData.LEFT_SOUTH;
                     }
                     else if(mapiter.getBlockTypeIDAt(BlockStep.Z_PLUS) == blktype) {    /* Check west */
                         cd = ChestData.RIGHT_SOUTH;
                     }
                     else {
                         cd = ChestData.SINGLE_SOUTH;
                     }
                     break;
                 case 3: /* West */
                 default:
                     if(mapiter.getBlockTypeIDAt(BlockStep.X_MINUS) == blktype) { /* Check north */
                         cd = ChestData.RIGHT_WEST;
                     }
                     else if(mapiter.getBlockTypeIDAt(BlockStep.X_PLUS) == blktype) {    /* Check south */
                         cd = ChestData.LEFT_WEST;
                     }
                     else {
                         cd = ChestData.SINGLE_WEST;
                     }
                     break;
             }
             return cd.ordinal();
         }
         /**
          * Generate redstone wire model data:
          *   0 = NSEW wire
          *   1 = NS wire
          *   2 = EW wire
          *   3 = NE wire
          *   4 = NW wire
          *   5 = SE wire
          *   6 = SW wire
          *   7 = NSE wire
          *   8 = NSW wire
          *   9 = NEW wire
          *   10 = SEW wire
          *   11 = none
          * @param mapiter
          * @return
          */
         private int generateRedstoneWireBlockData(MapIterator mapiter) {
             /* Check adjacent block IDs */
             int ids[] = { mapiter.getBlockTypeIDAt(BlockStep.Z_PLUS),  /* To west */
                 mapiter.getBlockTypeIDAt(BlockStep.X_PLUS),            /* To south */
                 mapiter.getBlockTypeIDAt(BlockStep.Z_MINUS),           /* To east */
                 mapiter.getBlockTypeIDAt(BlockStep.X_MINUS) };         /* To north */
             int flags = 0;
             for(int i = 0; i < 4; i++)
                 if(ids[i] == REDSTONE_BLKTYPEID)
                     flags |= (1<<i);
             switch(flags) {
                 case 0: /* Nothing nearby */
                     return 11;
                 case 15: /* NSEW */
                     return 0;   /* NSEW graphic */
                 case 2: /* S */
                 case 8: /* N */
                 case 10: /* NS */
                     return 1;   /* NS graphic */
                 case 1: /* W */
                 case 4: /* E */
                 case 5: /* EW */
                     return 2;   /* EW graphic */
                 case 12: /* NE */
                     return 3;
                 case 9: /* NW */
                     return 4;
                 case 6: /* SE */
                     return 5;
                 case 3: /* SW */
                     return 6;
                 case 14: /* NSE */
                     return 7;
                 case 11: /* NSW */
                     return 8;
                 case 13: /* NEW */
                     return 9;
                 case 7: /* SEW */
                     return 10;
             }
             return 0;
         }
         /**
          * Generate block render data for glass pane and iron fence.
          *  - bit 0 = X-minus axis
          *  - bit 1 = Z-minus axis
          *  - bit 2 = X-plus axis
          *  - bit 3 = Z-plus axis
          *  
          * @param mapiter - iterator
          * @param typeid - ID of our material (test is for adjacent material OR nontransparent)
          * @return
          */
         private int generateIronFenceGlassBlockData(MapIterator mapiter, int typeid) {
             int blockdata = 0;
             int id;
             /* Check north */
             id = mapiter.getBlockTypeIDAt(BlockStep.X_MINUS);
             if((id == typeid) || ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {
                 blockdata |= 1;
             }
             /* Look east */
             id = mapiter.getBlockTypeIDAt(BlockStep.Z_MINUS);
             if((id == typeid) || ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {
                 blockdata |= 2;
             }
             /* Look south */
             id = mapiter.getBlockTypeIDAt(BlockStep.X_PLUS);
             if((id == typeid) || ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {
                 blockdata |= 4;
             }
             /* Look west */
             id = mapiter.getBlockTypeIDAt(BlockStep.Z_PLUS);
             if((id == typeid) || ((id > 0) && (HDTextureMap.getTransparency(id) == BlockTransparency.OPAQUE))) {
                 blockdata |= 8;
             }
             return blockdata;
         }
         /**
          * Generate render data for doors
          *  - bit 3 = top half (1) or bottom half (0)
          *  - bit 2 = right hinge (0), left hinge (1)
          *  - bit 1,0 = 00=west,01=north,10=east,11=south
          * @param mapiter - iterator
          * @param typeid - ID of our material
          * @return
          */
         private int generateDoorBlockData(MapIterator mapiter, int typeid) {
             int blockdata = 0;
             int topdata = mapiter.getBlockData();   /* Get block data */
             int bottomdata = 0;
             if((topdata & 0x08) != 0) { /* We're door top */
                 blockdata |= 0x08;  /* Set top bit */
                 mapiter.stepPosition(BlockStep.Y_MINUS);
                 bottomdata = mapiter.getBlockData();
                 mapiter.unstepPosition(BlockStep.Y_MINUS);
             }
             else {  /* Else, we're bottom */
                 bottomdata = topdata;
                 mapiter.stepPosition(BlockStep.Y_PLUS);
                 topdata = mapiter.getBlockData();
                 mapiter.unstepPosition(BlockStep.Y_PLUS);
             }
             boolean onright = false;
             if((topdata & 0x01) == 1) { /* Right hinge */
                 blockdata |= 0x4; /* Set hinge bit */
                 onright = true;
             }
             blockdata |= (bottomdata & 0x3);    /* Set side bits */
             /* If open, rotate data appropriately */
             if((bottomdata & 0x4) > 0) {
                 if(onright) {   /* Hinge on right? */
                     blockdata = (blockdata & 0x8) | 0x0 | ((blockdata-1) & 0x3);
                 }
                 else {
                     blockdata = (blockdata & 0x8) | 0x4 | ((blockdata+1) & 0x3);
                 }
             }
             return blockdata;
         }
 
         private final boolean containsID(int id, int[] linkids) {
             for(int i = 0; i < linkids.length; i++)
                 if(id == linkids[i])
                     return true;
             return false;
         }
         private int generateWireBlockData(MapIterator mapiter, int[] linkids) {
             int blockdata = 0;
             int id;
             /* Check north */
             id = mapiter.getBlockTypeIDAt(BlockStep.X_MINUS);
             if(containsID(id, linkids)) {
                 blockdata |= 1;
             }
             /* Look east */
             id = mapiter.getBlockTypeIDAt(BlockStep.Z_MINUS);
             if(containsID(id, linkids)) {
                 blockdata |= 2;
             }
             /* Look south */
             id = mapiter.getBlockTypeIDAt(BlockStep.X_PLUS);
             if(containsID(id, linkids)) {
                 blockdata |= 4;
             }
             /* Look west */
             id = mapiter.getBlockTypeIDAt(BlockStep.Z_PLUS);
             if(containsID(id, linkids)) {
                 blockdata |= 8;
             }
             return blockdata;
         }
 
         private final boolean handleSubModel(short[] model, HDShaderState[] shaderstate, boolean[] shaderdone) {
             boolean firststep = true;
             
             while(!raytraceSubblock(model, firststep)) {
                 boolean done = true;
                 for(int i = 0; i < shaderstate.length; i++) {
                     if(!shaderdone[i])
                         shaderdone[i] = shaderstate[i].processBlock(this);
                     done = done && shaderdone[i];
                 }
                 /* If all are done, we're out */
                 if(done)
                     return true;
                 nonairhit = true;
                 firststep = false;
             }
             return false;
         }
         
         private final boolean handlePatches(RenderPatch[] patches, HDShaderState[] shaderstate, boolean[] shaderdone) {
             int hitcnt = 0;
             /* Loop through patches : compute intercept values for each */
             for(int i = 0; i < patches.length; i++) {
                 PatchDefinition pd = (PatchDefinition)patches[i];
                 /* Compute origin of patch */
                 v0.x = (double)x + pd.x0;
                 v0.y = (double)y + pd.y0;
                 v0.z = (double)z + pd.z0;
                 /* Compute cross product of direction and V vector */
                 d_cross_uv.set(direction);
                 d_cross_uv.crossProduct(pd.v);
                 /* Compute determinant - inner product of this with U */
                 double det = pd.u.innerProduct(d_cross_uv);
                 /* If parallel to surface, no intercept */
                 switch(pd.sidevis) {
                     case TOP:
                         if (det < 0.000001) {
                             continue;
                         }
                         break;
                     case BOTTOM:
                         if (det > -0.000001) {
                             continue;
                         }
                         break;
                     case BOTH:
                     case FLIP:
                         if((det > -0.000001) && (det < 0.000001)) {
                             continue;
                         }
                         break;
                 }
                 double inv_det = 1.0 / det; /* Calculate inverse determinant */
                 /* Compute distance from patch to ray origin */
                 vS.set(top);
                 vS.subtract(v0);
                 /* Compute u - slope times inner product of offset and cross product */
                 double u = inv_det * vS.innerProduct(d_cross_uv);
                 if((u <= pd.umin) || (u >= pd.umax)) {
                     continue;
                 }
                 /* Compute cross product of offset and U */
                 vS.crossProduct(pd.u);
                 /* Compute V using slope times inner product of direction and cross product */
                 double v = inv_det * direction.innerProduct(vS);
                 if((v <= pd.vmin) || (v >= pd.vmax) || ((u + v) >= pd.uplusvmax)) {
                     continue;
                 }
                 /* Compute parametric value of intercept */
                 double t = inv_det * pd.v.innerProduct(vS);
                 if (t > 0.000001) { /* We've got a hit */
                     patch_t[hitcnt] = t;
                     patch_u[hitcnt] = u;
                     patch_v[hitcnt] = v;
                     patch_id[hitcnt] = pd.textureindex;
                     if(det > 0) {
                         patch_step[hitcnt] = pd.step.opposite();
                     }
                     else {
                         if (pd.sidevis == SideVisible.FLIP) {
                             patch_u[hitcnt] = 1 - u;
                         }
                         patch_step[hitcnt] = pd.step;
                     }
                     hitcnt++;
                 }
             }
             /* If no hits, we're done */
             if(hitcnt == 0) {
                 return false;
             }
             BlockStep old_laststep = laststep;  /* Save last step */
             
             for(int i = 0; i < hitcnt; i++) {
                 /* Find closest hit (lowest parametric value) */
                 double best_t = Double.MAX_VALUE;
                 int best_patch = 0;
                 for(int j = 0; j < hitcnt; j++) {
                     if(patch_t[j] < best_t) {
                         best_patch = j;
                         best_t = patch_t[j];
                     }
                 }
                 cur_patch = patch_id[best_patch]; /* Mark this as current patch */
                 cur_patch_u = patch_u[best_patch];
                 cur_patch_v = patch_v[best_patch];
                 laststep = patch_step[best_patch];
                cur_patch_t = best_t;
                 /* Process the shaders */
                 boolean done = true;
                 for(int j = 0; j < shaderstate.length; j++) {
                     if(!shaderdone[j])
                         shaderdone[j] = shaderstate[j].processBlock(this);
                     done = done && shaderdone[j];
                 }
                 cur_patch = -1;
                 /* If all are done, we're out */
                 if(done) {
                     laststep = old_laststep;
                     return true;
                 }
                 nonairhit = true;
                 /* Now remove patch and repeat */
                 patch_t[best_patch] = Double.MAX_VALUE;
             }
             laststep = old_laststep;
             
             return false;
         }
         
         private static final int FENCE_ALGORITHM = 1;
         private static final int CHEST_ALGORITHM = 2;
         private static final int REDSTONE_ALGORITHM = 3;
         private static final int GLASS_IRONFENCE_ALG = 4;
         private static final int WIRE_ALGORITHM = 5;
         private static final int DOOR_ALGORITHM = 6;
         /**
          * Process visit of ray to block
          */
         private final boolean visit_block(MapIterator mapiter, HDShaderState[] shaderstate, boolean[] shaderdone) {
             lastblocktypeid = blocktypeid;
             blocktypeid = mapiter.getBlockTypeID();
             if(skiptoair) {	/* If skipping until we see air */
                 if(blocktypeid == 0) {	/* If air, we're done */
                 	skiptoair = false;
                 }
             }
             else if(nonairhit || (blocktypeid != 0)) {
                 blockdata = mapiter.getBlockData();            	
                 switch(HDBlockModels.getLinkAlgID(blocktypeid)) {
                     case FENCE_ALGORITHM:   /* Fence algorithm */
                         blockrenderdata = generateFenceBlockData(mapiter, blocktypeid);
                         break;
                     case CHEST_ALGORITHM:
                         blockrenderdata = generateChestBlockData(mapiter, blocktypeid);
                         break;
                     case REDSTONE_ALGORITHM:
                         blockrenderdata = generateRedstoneWireBlockData(mapiter);
                         break;
                     case GLASS_IRONFENCE_ALG:
                         blockrenderdata = generateIronFenceGlassBlockData(mapiter, blocktypeid);
                         break;
                     case WIRE_ALGORITHM:
                         blockrenderdata = generateWireBlockData(mapiter, HDBlockModels.getLinkIDs(blocktypeid));
                         break;
                     case DOOR_ALGORITHM:
                         blockrenderdata = generateDoorBlockData(mapiter, blocktypeid);
                         break;
                     case 0:
                     default:
                     	blockrenderdata = -1;
                     	break;
                 }
                 RenderPatch[] patches = scalemodels.getPatchModel(blocktypeid,  blockdata,  blockrenderdata);
                 /* If no patches, see if custom model */
                 if(patches == null) {
                     CustomBlockModel cbm = scalemodels.getCustomBlockModel(blocktypeid,  blockdata);
                     if(cbm != null) {   /* If found, see if cached already */
                         patches = this.getCustomMesh();
                         if(patches == null) {
                             patches = cbm.getMeshForBlock(mapiter);
                             this.setCustomMesh(patches);
                         }
                     }
                 }
                 /* Look up to see if block is modelled */
                 if(patches != null) {
                     return handlePatches(patches, shaderstate, shaderdone);
                 }
                 short[] model = scalemodels.getScaledModel(blocktypeid, blockdata, blockrenderdata);
                 if(model != null) {
                     return handleSubModel(model, shaderstate, shaderdone);
                 }
                 else {
                     boolean done = true;
                     subalpha = -1;
                     for(int i = 0; i < shaderstate.length; i++) {
                         if(!shaderdone[i]) {
                             try {
                                 shaderdone[i] = shaderstate[i].processBlock(this);
                             } catch (Exception ex) {
                                 Log.severe("Error while shading tile: perspective=" + IsoHDPerspective.this.name + ", shader=" + shaderstate[i].getShader().getName() + ", coord=" + mapiter.getX() + "," + mapiter.getY() + "," + mapiter.getZ() + ", blockid=" + mapiter.getBlockTypeID() + ":" + mapiter.getBlockData() + ", lighting=" + mapiter.getBlockSkyLight() + ":" + mapiter.getBlockEmittedLight() + ", biome=" + mapiter.getBiome().toString(), ex);
                                 shaderdone[i] = true;
                             }
                         }
                         done = done && shaderdone[i];
                     }
                     /* If all are done, we're out */
                     if(done)
                         return true;
                     nonairhit = true;
                 }
             }
             return false;
         }
         /**
          * Trace ray, based on "Voxel Tranversal along a 3D line"
          */
         private void raytrace(MapChunkCache cache, MapIterator mapiter, HDShaderState[] shaderstate, boolean[] shaderdone) {
             /* Initialize raytrace state variables */
             raytrace_init();
 
             /* Skip sections until we hit a non-empty one */
             while(cache.isEmptySection(sx, sy, sz)) {
                 /* If Y step is next best */
                 if((st_next_y <= st_next_x) && (st_next_y <= st_next_z)) {
                     sy += y_inc;
                     t = st_next_y;
                     st_next_y += sdt_dy;
                     laststep = stepy;
                     if(sy < 0)
                         return;
                 }
                 /* If X step is next best */
                 else if((st_next_x <= st_next_y) && (st_next_x <= st_next_z)) {
                     sx += x_inc;
                     t = st_next_x;
                     st_next_x += sdt_dx;
                     laststep = stepx;
                 }
                 /* Else, Z step is next best */
                 else {
                     sz += z_inc;
                     t = st_next_z;
                     st_next_z += sdt_dz;
                     laststep = stepz;
                 }
             }
             raytrace_section_init();
             
             if(y < 0)
                 return;
             
             mapiter.initialize(x, y, z);
             
 //            System.out.println("xyz=" + x + ',' + y +',' + z + " t=" + t + ", tnext=" + t_next_x + ","+ t_next_y + "," + t_next_z);
             
             for (; n > 0; --n) {
         		if(visit_block(mapiter, shaderstate, shaderdone)) {
                     return;
                 }
                 /* If Y step is next best */
                 if((t_next_y <= t_next_x) && (t_next_y <= t_next_z)) {
                     y += y_inc;
                     t = t_next_y;
                     t_next_y += dt_dy;
                     laststep = stepy;
                     mapiter.stepPosition(laststep);
                     /* If outside 0-(height-1) range */
                     if((y & (~heightmask)) != 0) return;
                 }
                 /* If X step is next best */
                 else if((t_next_x <= t_next_y) && (t_next_x <= t_next_z)) {
                     x += x_inc;
                     t = t_next_x;
                     t_next_x += dt_dx;
                     laststep = stepx;
                     mapiter.stepPosition(laststep);
                 }
                 /* Else, Z step is next best */
                 else {
                     z += z_inc;
                     t = t_next_z;
                     t_next_z += dt_dz;
                     laststep = stepz;
                     mapiter.stepPosition(laststep);
                 }
             }
         }
 
         private void raytrace_section_init() {
             t = t - 0.000001;
             double xx = top.x + t * direction.x;
             double yy = top.y + t * direction.y;
             double zz = top.z + t * direction.z;
             x = fastFloor(xx);  
             y = fastFloor(yy);  
             z = fastFloor(zz);
             t_next_x = st_next_x;
             t_next_y = st_next_y;
             t_next_z = st_next_z;
             n = 1;
             if(t_next_x != Double.MAX_VALUE) {
                 if(stepx == BlockStep.X_PLUS) {
                     t_next_x = t + (x + 1 - xx) * dt_dx;
                     n += fastFloor(bottom.x) - x;
                 }
                 else {
                     t_next_x = t + (xx - x) * dt_dx;
                     n += x - fastFloor(bottom.x);
                 }
             }
             if(t_next_y != Double.MAX_VALUE) {
                 if(stepy == BlockStep.Y_PLUS) {
                     t_next_y = t + (y + 1 - yy) * dt_dy;
                     n += fastFloor(bottom.y) - y;
                 }
                 else {
                     t_next_y = t + (yy - y) * dt_dy;
                     n += y - fastFloor(bottom.y);
                 }
             }
             if(t_next_z != Double.MAX_VALUE) {
                 if(stepz == BlockStep.Z_PLUS) {
                     t_next_z = t + (z + 1 - zz) * dt_dz;
                     n += fastFloor(bottom.z) - z;
                 }
                 else {
                     t_next_z = t + (zz - z) * dt_dz;
                     n += z - fastFloor(bottom.z);
                 }
             }
         }
 
         private boolean raytraceSubblock(short[] model, boolean firsttime) {
             if(firsttime) {
             	mt = t + 0.00000001;
             	xx = top.x + mt * direction.x;  
             	yy = top.y + mt * direction.y;  
             	zz = top.z + mt * direction.z;
             	mx = (int)((xx - fastFloor(xx)) * modscale);
             	my = (int)((yy - fastFloor(yy)) * modscale);
             	mz = (int)((zz - fastFloor(zz)) * modscale);
             	mdt_dx = dt_dx / modscale;
             	mdt_dy = dt_dy / modscale;
             	mdt_dz = dt_dz / modscale;
             	mt_next_x = t_next_x;
             	mt_next_y = t_next_y;
             	mt_next_z = t_next_z;
             	if(mt_next_x != Double.MAX_VALUE) {
             		togo = ((t_next_x - t) / mdt_dx);
             		mt_next_x = mt + (togo - fastFloor(togo)) * mdt_dx;
             	}
             	if(mt_next_y != Double.MAX_VALUE) {
             		togo = ((t_next_y - t) / mdt_dy);
             		mt_next_y = mt + (togo - fastFloor(togo)) * mdt_dy;
             	}
             	if(mt_next_z != Double.MAX_VALUE) {
             		togo = ((t_next_z - t) / mdt_dz);
             		mt_next_z = mt + (togo - fastFloor(togo)) * mdt_dz;
             	}
             	mtend = Math.min(t_next_x, Math.min(t_next_y, t_next_z));
             }
             subalpha = -1;
             boolean skip = !firsttime;	/* Skip first block on continue */
             while(mt <= mtend) {
             	if(!skip) {
             		try {
             			int blkalpha = model[modscale*modscale*my + modscale*mz + mx];
             			if(blkalpha > 0) {
             				subalpha = blkalpha;
             				return false;
             			}
             		} catch (ArrayIndexOutOfBoundsException aioobx) {	/* We're outside the model, so miss */
             			return true;
             		}
             	}
             	else {
             		skip = false;
             	}
         		
                 /* If X step is next best */
                 if((mt_next_x <= mt_next_y) && (mt_next_x <= mt_next_z)) {
                     mx += x_inc;
                     mt = mt_next_x;
                     mt_next_x += mdt_dx;
                     laststep = stepx;
                     if(mx == mxout) {
                         return true;
                     }
                 }
                 /* If Y step is next best */
                 else if((mt_next_y <= mt_next_x) && (mt_next_y <= mt_next_z)) {
                     my += y_inc;
                     mt = mt_next_y;
                     mt_next_y += mdt_dy;
                     laststep = stepy;
                     if(my == myout) {
                         return true;
                     }
                 }
                 /* Else, Z step is next best */
                 else {
                     mz += z_inc;
                     mt = mt_next_z;
                     mt_next_z += mdt_dz;
                     laststep = stepz;
                     if(mz == mzout) {
                         return true;
                     }
                 }
             }
             return true;
         }
         public final int[] getSubblockCoord() {
             if(cur_patch >= 0) {    /* If patch hit */
                double tt = cur_patch_t;
                 double xx = top.x + tt * direction.x;  
                 double yy = top.y + tt * direction.y;  
                 double zz = top.z + tt * direction.z;
                 subblock_xyz[0] = (int)((xx - fastFloor(xx)) * modscale);
                 subblock_xyz[1] = (int)((yy - fastFloor(yy)) * modscale);
                 subblock_xyz[2] = (int)((zz - fastFloor(zz)) * modscale);
             }
             else if(subalpha < 0) {
                 double tt = t + 0.0000001;
                 double xx = top.x + tt * direction.x;  
                 double yy = top.y + tt * direction.y;  
                 double zz = top.z + tt * direction.z;
                 subblock_xyz[0] = (int)((xx - fastFloor(xx)) * modscale);
                 subblock_xyz[1] = (int)((yy - fastFloor(yy)) * modscale);
                 subblock_xyz[2] = (int)((zz - fastFloor(zz)) * modscale);
             }
             else {
                 subblock_xyz[0] = mx;
                 subblock_xyz[1] = my;
                 subblock_xyz[2] = mz;
             }
             return subblock_xyz;
         }
         /**
          * Get current texture index
          */
         public int getTextureIndex() {
             return cur_patch;
         }
         /**
          * Get current U of patch intercept
          */
         public double getPatchU() {
             return cur_patch_u;
         }
         /**
          * Get current V of patch intercept
          */
         public double getPatchV() {
             return cur_patch_v;
         }
         /**
          * Light level cache
          * @param index of light level (0-3)
          */
         public final LightLevels getCachedLightLevels(int idx) {
             return llcache[idx];
         }
         /**
          * Get custom mesh for block, if defined (null if not)
          */
         public final RenderPatch[] getCustomMesh() {
             long key = this.mapiter.getBlockKey();  /* Get key for current block */
             return (RenderPatch[])custom_meshes.get(key);
         }
         /**
          * Save custom mesh for block
          */
         public final void setCustomMesh(RenderPatch[] mesh) {
             long key = this.mapiter.getBlockKey();  /* Get key for current block */
             custom_meshes.put(key,  mesh);
         }
     }
     
     public IsoHDPerspective(DynmapCore core, ConfigurationNode configuration) {
         name = configuration.getString("name", null);
         if(name == null) {
             Log.severe("Perspective definition missing name - must be defined and unique");
             return;
         }
         azimuth = configuration.getDouble("azimuth", 135.0);    /* Get azimuth (default to classic kzed POV */
         /* Fix azimuth so that we respect new north, if that is requested (newnorth = oldeast) */
         if(MapManager.mapman.getCompassMode() == CompassMode.NEWNORTH) {
             azimuth = (azimuth + 90.0); if(azimuth >= 360.0) azimuth = azimuth - 360.0;
         }
         inclination = configuration.getDouble("inclination", 60.0);
         if(inclination > MAX_INCLINATION) inclination = MAX_INCLINATION;
         if(inclination < MIN_INCLINATION) inclination = MIN_INCLINATION;
         scale = configuration.getDouble("scale", MIN_SCALE);
         if(scale < MIN_SCALE) scale = MIN_SCALE;
         if(scale > MAX_SCALE) scale = MAX_SCALE;
         /* Get max and min height */
         maxheight = configuration.getInteger("maximumheight", -1);
         minheight = configuration.getInteger("minimumheight", 0);
         if(minheight < 0) minheight = 0;
 
         /* Generate transform matrix for world-to-tile coordinate mapping */
         /* First, need to fix basic coordinate mismatches before rotation - we want zero azimuth to have north to top
          * (world -X -> tile +Y) and east to right (world -Z to tile +X), with height being up (world +Y -> tile +Z)
          */
         Matrix3D transform = new Matrix3D(0.0, 0.0, -1.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0);
         /* Next, rotate world counterclockwise around Z axis by azumuth angle */
         transform.rotateXY(180-azimuth);
         /* Next, rotate world by (90-inclination) degrees clockwise around +X axis */
         transform.rotateYZ(90.0-inclination);
         /* Finally, shear along Z axis to normalize Z to be height above map plane */
         transform.shearZ(0, Math.tan(Math.toRadians(90.0-inclination)));
         /* And scale Z to be same scale as world coordinates, and scale X and Y based on setting */
         transform.scale(scale, scale, Math.sin(Math.toRadians(inclination)));
         world_to_map = transform;
         /* Now, generate map to world tranform, by doing opposite actions in reverse order */
         transform = new Matrix3D();
         transform.scale(1.0/scale, 1.0/scale, 1/Math.sin(Math.toRadians(inclination)));
         transform.shearZ(0, -Math.tan(Math.toRadians(90.0-inclination)));
         transform.rotateYZ(-(90.0-inclination));
         transform.rotateXY(-180+azimuth);
         Matrix3D coordswap = new Matrix3D(0.0, -1.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0);
         transform.multiply(coordswap);
         map_to_world = transform;
         /* Scaled models for non-cube blocks */
         modscale = (int)Math.ceil(scale);
         scalemodels = HDBlockModels.getModelsForScale(modscale);;
     }   
 
     @Override
     public List<TileFlags.TileCoord> getTileCoords(DynmapWorld world, int x, int y, int z) {
         HashSet<TileFlags.TileCoord> tiles = new HashSet<TileFlags.TileCoord>();
         Vector3D block = new Vector3D();
         block.x = x;
         block.y = y;
         block.z = z;
         Vector3D corner = new Vector3D();
         /* Loop through corners of the cube */
         for(int i = 0; i < 2; i++) {
             double inity = block.y;
             for(int j = 0; j < 2; j++) {
                 double initz = block.z;
                 for(int k = 0; k < 2; k++) {
                     world_to_map.transform(block, corner);  /* Get map coordinate of corner */
                     tiles.add(new TileFlags.TileCoord(fastFloor(corner.x/tileWidth), fastFloor(corner.y/tileHeight)));
                     block.z += 1;
                 }
                 block.z = initz;
                 block.y += 1;
             }
             block.y = inity;
             block.x += 1;
         }
         return new ArrayList<TileFlags.TileCoord>(tiles);
     }
 
     @Override
     public List<TileFlags.TileCoord> getTileCoords(DynmapWorld world, int minx, int miny, int minz, int maxx, int maxy, int maxz) {
         ArrayList<TileFlags.TileCoord> tiles = new ArrayList<TileFlags.TileCoord>();
         Vector3D blocks[] = new Vector3D[] { new Vector3D(), new Vector3D() };
         blocks[0].x = minx - 1;
         blocks[0].y = miny - 1;
         blocks[0].z = minz - 1;
         blocks[1].x = maxx + 1;
         blocks[1].y = maxy + 1;
         blocks[1].z = maxz + 1;
         
         Vector3D corner = new Vector3D();
         Vector3D tcorner = new Vector3D();
         int mintilex = Integer.MAX_VALUE;
         int maxtilex = Integer.MIN_VALUE;
         int mintiley = Integer.MAX_VALUE;
         int maxtiley = Integer.MIN_VALUE;
         /* Loop through corners of the prism */
         for(int i = 0; i < 2; i++) {
             corner.x = blocks[i].x;
             for(int j = 0; j < 2; j++) {
                 corner.y = blocks[j].y;
                 for(int k = 0; k < 2; k++) {
                     corner.z = blocks[k].z;
                     world_to_map.transform(corner, tcorner);  /* Get map coordinate of corner */
                     int tx = fastFloor(tcorner.x/tileWidth);
                     int ty = fastFloor(tcorner.y/tileWidth);
                     if(mintilex > tx) mintilex = tx;
                     if(maxtilex < tx) maxtilex = tx;
                     if(mintiley > ty) mintiley = ty;
                     if(maxtiley < ty) maxtiley = ty;
                 }
             }
         }
         /* Now, add the tiles for the ranges - not perfect, but it works (some extra tiles on corners possible) */
         for(int i = mintilex; i <= maxtilex; i++) {
             for(int j = mintiley-1; j <= maxtiley; j++) {   /* Extra 1 - TODO: figure out why needed... */ 
                 tiles.add(new TileFlags.TileCoord(i, j));
             }
         }
         return tiles;
     }
 
     @Override
     public MapTile[] getAdjecentTiles(MapTile tile) {
         HDMapTile t = (HDMapTile) tile;
         DynmapWorld w = t.getDynmapWorld();
         int x = t.tx;
         int y = t.ty;
         return new MapTile[] {
             new HDMapTile(w, this, x - 1, y - 1),
             new HDMapTile(w, this, x + 1, y - 1),
             new HDMapTile(w, this, x - 1, y + 1),
             new HDMapTile(w, this, x + 1, y + 1),
             new HDMapTile(w, this, x, y - 1),
             new HDMapTile(w, this, x + 1, y),
             new HDMapTile(w, this, x, y + 1),
             new HDMapTile(w, this, x - 1, y) };
     }
 
     private static class Rectangle {
         double r0x, r0z;   /* Coord of corner of rectangle */
         double s1x, s1z;   /* Side vector for one edge */
         double s2x, s2z;    /* Side vector for other edge */
         public Rectangle(Vector3D v1, Vector3D v2, Vector3D v3) {
             r0x = v1.x;
             r0z = v1.z;
             s1x = v2.x - v1.x;
             s1z = v2.z - v1.z;
             s2x = v3.x - v1.x;
             s2z = v3.z - v1.z;
         }
         public Rectangle() {
         }
         public void setSquare(double rx, double rz, double s) {
             this.r0x = rx;
             this.r0z = rz;
             this.s1x = s;
             this.s1z = 0;
             this.s2x = 0;
             this.s2z = s;
         }
         double getX(int idx) {
             return r0x + (((idx & 1) == 0)?0:s1x) + (((idx & 2) != 0)?0:s2x);
         }
         double getZ(int idx) {
             return r0z + (((idx & 1) == 0)?0:s1z) + (((idx & 2) != 0)?0:s2z);
         }
         /**
          * Test for overlap of projection of one vector on to anoter
          */
         boolean testoverlap(double rx, double rz, double sx, double sz, Rectangle r) {
             double rmin_dot_s0 = Double.MAX_VALUE;
             double rmax_dot_s0 = Double.MIN_VALUE;
             /* Project each point from rectangle on to vector: find lowest and highest */
             for(int i = 0; i < 4; i++) {
                 double r_x = r.getX(i) - rx;  /* Get relative positon of second vector start to origin */
                 double r_z = r.getZ(i) - rz;
                 double r_dot_s0 = r_x*sx + r_z*sz;   /* Projection of start of vector */
                 if(r_dot_s0 < rmin_dot_s0) rmin_dot_s0 = r_dot_s0;
                 if(r_dot_s0 > rmax_dot_s0) rmax_dot_s0 = r_dot_s0;
             }
             /* Compute dot products */
             double s0_dot_s0 = sx*sx + sz*sz; /* End of our side */
             if((rmax_dot_s0 < 0.0) || (rmin_dot_s0 > s0_dot_s0))
                 return false;
             else
                 return true;
         }
         /**
          * Test if two rectangles intersect
          * Based on separating axis theorem
          */
         boolean testRectangleIntesectsRectangle(Rectangle r) {
             /* Test if projection of each edge of one rectangle on to each edge of the other yields overlap */
             if(testoverlap(r0x, r0z, s1x, s1z, r) && testoverlap(r0x, r0z, s2x, s2z, r) && 
                     testoverlap(r0x+s1x, r0z+s1z, s2x, s2z, r) && testoverlap(r0x+s2x, r0z+s2z, s1x, s1z, r) && 
                     r.testoverlap(r.r0x, r.r0z, r.s1x, r.s1z, this) && r.testoverlap(r.r0x, r.r0z, r.s2x, r.s2z, this) &&
                     r.testoverlap(r.r0x+r.s1x, r.r0z+r.s1z, r.s2x, r.s2z, this) && r.testoverlap(r.r0x+r.s2x, r.r0z+r.s2z, r.s1x, r.s1z, this)) {
                 return true;
             }
             else {
                 return false;
             }
         }
         public String toString() {
             return "{ " + r0x + "," + r0z + "}x{" + (r0x+s1x) + ","+ + (r0z+s1z) + "}x{" + (r0x+s2x) + "," + (r0z+s2z) + "}";
         }
     }
     
     @Override
     public List<DynmapChunk> getRequiredChunks(MapTile tile) {
         if (!(tile instanceof HDMapTile))
             return Collections.emptyList();
         
         HDMapTile t = (HDMapTile) tile;
         int min_chunk_x = Integer.MAX_VALUE;
         int max_chunk_x = Integer.MIN_VALUE;
         int min_chunk_z = Integer.MAX_VALUE;
         int max_chunk_z = Integer.MIN_VALUE;
         
         /* Make corners for volume: 0 = bottom-lower-left, 1 = top-lower-left, 2=bottom-upper-left, 3=top-upper-left
          * 4 = bottom-lower-right, 5 = top-lower-right, 6 = bottom-upper-right, 7 = top-upper-right */  
         Vector3D corners[] = new Vector3D[8];
         double dx = -scale, dy = -scale;    /* Add 1 block on each axis */
         for(int x = t.tx, idx = 0; x <= (t.tx+1); x++) {
             dy = -scale;
             for(int y = t.ty; y <= (t.ty+1); y++) {
                 for(int z = 0; z <= 1; z++) {
                     corners[idx] = new Vector3D();
                     corners[idx].x = x*tileWidth + dx; corners[idx].y = y*tileHeight + dy; corners[idx].z = z*t.getDynmapWorld().worldheight;
                     map_to_world.transform(corners[idx]);
                     /* Compute chunk coordinates of corner */
                     int cx = fastFloor(corners[idx].x / 16);
                     int cz = fastFloor(corners[idx].z / 16);
                     /* Compute min/max of chunk coordinates */
                     if(min_chunk_x > cx) min_chunk_x = cx;
                     if(max_chunk_x < cx) max_chunk_x = cx;
                     if(min_chunk_z > cz) min_chunk_z = cz;
                     if(max_chunk_z < cz) max_chunk_z = cz;
                     idx++;
                 }
                 dy = scale;
             }
             dx = scale;
         }
         /* Make rectangles of X-Z projection of each side of the tile volume, 0 = top, 1 = bottom, 2 = left, 3 = right,
          * 4 = upper, 5 = lower */
         Rectangle rect[] = new Rectangle[6];
         rect[0] = new Rectangle(corners[1], corners[3], corners[5]);
         rect[1] = new Rectangle(corners[0], corners[2], corners[4]);
         rect[2] = new Rectangle(corners[0], corners[1], corners[2]);
         rect[3] = new Rectangle(corners[4], corners[5], corners[6]);
         rect[4] = new Rectangle(corners[2], corners[3], corners[6]);
         rect[5] = new Rectangle(corners[0], corners[1], corners[4]);
         
         /* Now, need to walk through the min/max range to see which chunks are actually needed */
         ArrayList<DynmapChunk> chunks = new ArrayList<DynmapChunk>();
         Rectangle chunkrect = new Rectangle();
         for(int x = min_chunk_x; x <= max_chunk_x; x++) {
             for(int z = min_chunk_z; z <= max_chunk_z; z++) {
                 chunkrect.setSquare(x*16, z*16, 16);
                 boolean hit = false;
                 /* Check to see if square of chunk intersects any of our rectangle sides */
                 for(int rctidx = 0; (!hit) && (rctidx < rect.length); rctidx++) {
                     if(chunkrect.testRectangleIntesectsRectangle(rect[rctidx])) {
                         hit = true;
                     }
                 }
                 if(hit) {
                     DynmapChunk chunk = new DynmapChunk(x, z);
                     chunks.add(chunk);
                 }
             }
         }
         return chunks;
     }
 
     @Override
     public boolean render(MapChunkCache cache, HDMapTile tile, String mapname) {
         Color rslt = new Color();
         MapIterator mapiter = cache.getIterator(0, 0, 0);
         /* Build shader state object for each shader */
         HDShaderState[] shaderstate = MapManager.mapman.hdmapman.getShaderStateForTile(tile, cache, mapiter, mapname);
         int numshaders = shaderstate.length;
         if(numshaders == 0)
             return false;
         /* Check if nether world */
         boolean isnether = tile.getDynmapWorld().isNether();
         /* Create buffered image for each */
         DynmapBufferedImage im[] = new DynmapBufferedImage[numshaders];
         DynmapBufferedImage dayim[] = new DynmapBufferedImage[numshaders];
         int[][] argb_buf = new int[numshaders][];
         int[][] day_argb_buf = new int[numshaders][];
         boolean isjpg[] = new boolean[numshaders];
         int bgday[] = new int[numshaders];
         int bgnight[] = new int[numshaders];
         
         for(int i = 0; i < numshaders; i++) {
             HDLighting lighting = shaderstate[i].getLighting();
             im[i] = DynmapBufferedImage.allocateBufferedImage(tileWidth, tileHeight);
             argb_buf[i] = im[i].argb_buf;
             if(lighting.isNightAndDayEnabled()) {
                 dayim[i] = DynmapBufferedImage.allocateBufferedImage(tileWidth, tileHeight);
                 day_argb_buf[i] = dayim[i].argb_buf;
             }
             isjpg[i] = shaderstate[i].getMap().getImageFormat() != ImageFormat.FORMAT_PNG;
             bgday[i] = shaderstate[i].getMap().getBackgroundARGBDay();
             bgnight[i] = shaderstate[i].getMap().getBackgroundARGBNight();
         }
         
         /* Create perspective state object */
         OurPerspectiveState ps = new OurPerspectiveState(mapiter, isnether);        
         
         ps.top = new Vector3D();
         ps.bottom = new Vector3D();
         ps.direction = new Vector3D();
         double xbase = tile.tx * tileWidth;
         double ybase = tile.ty * tileHeight;
         boolean shaderdone[] = new boolean[numshaders];
         boolean rendered[] = new boolean[numshaders];
         double height = maxheight;
         if(height < 0) {    /* Not set - assume world height - 1 */
             if (isnether)
                 height = 127;
             else
                 height = tile.getDynmapWorld().worldheight - 1;
         }
         
         for(int x = 0; x < tileWidth; x++) {
             ps.px = x;
             for(int y = 0; y < tileHeight; y++) {
                 ps.top.x = ps.bottom.x = xbase + x + 0.5;    /* Start at center of pixel at Y=height+0.5, bottom at Y=-0.5 */
                 ps.top.y = ps.bottom.y = ybase + y + 0.5;
                 ps.top.z = height + 0.5; ps.bottom.z = minheight - 0.5;
                 map_to_world.transform(ps.top);            /* Transform to world coordinates */
                 map_to_world.transform(ps.bottom);
                 ps.direction.set(ps.bottom);
                 ps.direction.subtract(ps.top);
                 ps.py = y;
                 for(int i = 0; i < numshaders; i++) {
                     shaderstate[i].reset(ps);
                 }
                 try {
                     ps.raytrace(cache, mapiter, shaderstate, shaderdone);
                 } catch (Exception ex) {
                     Log.severe("Error while raytracing tile: perspective=" + this.name + ", coord=" + mapiter.getX() + "," + mapiter.getY() + "," + mapiter.getZ() + ", blockid=" + mapiter.getBlockTypeID() + ":" + mapiter.getBlockData() + ", lighting=" + mapiter.getBlockSkyLight() + ":" + mapiter.getBlockEmittedLight() + ", biome=" + mapiter.getBiome().toString(), ex);
                 }
                 for(int i = 0; i < numshaders; i++) {
                     if(shaderdone[i] == false) {
                         shaderstate[i].rayFinished(ps);
                     }
                     else {
                         shaderdone[i] = false;
                         rendered[i] = true;
                     }
                     shaderstate[i].getRayColor(rslt, 0);
                     int c_argb = rslt.getARGB();
                     if(c_argb != 0) rendered[i] = true;
                     if(isjpg[i] && (c_argb == 0)) {
                         argb_buf[i][(tileHeight-y-1)*tileWidth + x] = bgnight[i];
                     }
                     else {
                         argb_buf[i][(tileHeight-y-1)*tileWidth + x] = c_argb;
                     }
                     if(day_argb_buf[i] != null) {
                         shaderstate[i].getRayColor(rslt, 1);
                         c_argb = rslt.getARGB();
                         if(isjpg[i] && (c_argb == 0)) {
                             day_argb_buf[i][(tileHeight-y-1)*tileWidth + x] = bgday[i];
                         }
                         else {
                             day_argb_buf[i][(tileHeight-y-1)*tileWidth + x] = c_argb;
                         }
                     }
                 }
             }
         }
 
         boolean renderone = false;
         /* Test to see if we're unchanged from older tile */
         TileHashManager hashman = MapManager.mapman.hashman;
         for(int i = 0; i < numshaders; i++) {
             long crc = hashman.calculateTileHash(argb_buf[i]);
             boolean tile_update = false;
             String prefix = shaderstate[i].getMap().getPrefix();
 
             MapType.ImageFormat fmt = shaderstate[i].getMap().getImageFormat();
             String fname = tile.getFilename(prefix, fmt);
             File f = new File(tile.getDynmapWorld().worldtilepath, fname);
             FileLockManager.getWriteLock(f);
             try {
                 if((!f.exists()) || (crc != hashman.getImageHashCode(tile.getKey(prefix), null, tile.tx, tile.ty))) {
                     /* Wrap buffer as buffered image */
                     if(rendered[i]) {   
                         Debug.debug("saving image " + f.getPath());
                         if(!f.getParentFile().exists())
                             f.getParentFile().mkdirs();
                         try {
                             FileLockManager.imageIOWrite(im[i].buf_img, fmt, f);
                         } catch (IOException e) {
                             Debug.error("Failed to save image: " + f.getPath(), e);
                         } catch (java.lang.NullPointerException e) {
                             Debug.error("Failed to save image (NullPointerException): " + f.getPath(), e);
                         }
                     }
                     else {
                         f.delete();
                     }
                     MapManager.mapman.pushUpdate(tile.getDynmapWorld(), new Client.Tile(fname));
                     hashman.updateHashCode(tile.getKey(prefix), null, tile.tx, tile.ty, crc);
                     tile.getDynmapWorld().enqueueZoomOutUpdate(f);
                     tile_update = true;
                     renderone = true;
                 }
                 else {
                     Debug.debug("skipping image " + f.getPath() + " - hash match");
                     if(!rendered[i]) {   
                         f.delete();
                         hashman.updateHashCode(tile.getKey(prefix), null, tile.tx, tile.ty, -1);
                         tile.getDynmapWorld().enqueueZoomOutUpdate(f);
                     }
                 }
             } finally {
                 FileLockManager.releaseWriteLock(f);
                 DynmapBufferedImage.freeBufferedImage(im[i]);
             }
             MapManager.mapman.updateStatistics(tile, prefix, true, tile_update, !rendered[i]);
             /* Handle day image, if needed */
             if(dayim[i] != null) {
                 fname = tile.getDayFilename(prefix, fmt);
                 f = new File(tile.getDynmapWorld().worldtilepath, fname);
                 FileLockManager.getWriteLock(f);
                 tile_update = false;
                 try {
                     if((!f.exists()) || (crc != hashman.getImageHashCode(tile.getKey(prefix), "day", tile.tx, tile.ty))) {
                         /* Wrap buffer as buffered image */
                         if(rendered[i]) {
                             Debug.debug("saving image " + f.getPath());
                             if(!f.getParentFile().exists())
                                 f.getParentFile().mkdirs();
                             try {
                                 FileLockManager.imageIOWrite(dayim[i].buf_img, fmt, f);
                             } catch (IOException e) {
                                 Debug.error("Failed to save image: " + f.getPath(), e);
                             } catch (java.lang.NullPointerException e) {
                                 Debug.error("Failed to save image (NullPointerException): " + f.getPath(), e);
                             }
                         }
                         else {
                             f.delete();
                         }
                         MapManager.mapman.pushUpdate(tile.getDynmapWorld(), new Client.Tile(fname));
                         hashman.updateHashCode(tile.getKey(prefix), "day", tile.tx, tile.ty, crc);
                         tile.getDynmapWorld().enqueueZoomOutUpdate(f);
                         tile_update = true;
                         renderone = true;
                     }
                     else {
                         Debug.debug("skipping image " + f.getPath() + " - hash match");
                         if(!rendered[i]) {   
                             hashman.updateHashCode(tile.getKey(prefix), "day", tile.tx, tile.ty, -1);
                             tile.getDynmapWorld().enqueueZoomOutUpdate(f);
                             f.delete();
                         }
                     }
                 } finally {
                     FileLockManager.releaseWriteLock(f);
                     DynmapBufferedImage.freeBufferedImage(dayim[i]);
                 }
                 MapManager.mapman.updateStatistics(tile, prefix+"_day", true, tile_update, !rendered[i]);
             }
         }
         return renderone;
     }
 
     @Override
     public boolean isBiomeDataNeeded() {
         return need_biomedata;
     }
 
     @Override
     public boolean isRawBiomeDataNeeded() { 
          return need_rawbiomedata;
      }
 
     public boolean isHightestBlockYDataNeeded() {
         return false;
     }
     
     public boolean isBlockTypeDataNeeded() {
         return true;
     }
     
     public double getScale() {
         return scale;
     }
 
     public int getModelScale() {
         return modscale;
     }
 
     @Override
     public String getName() {
         return name;
     }
 
     private static String[] directions = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
     @Override
     public void addClientConfiguration(JSONObject mapObject) {
         s(mapObject, "perspective", name);
         s(mapObject, "azimuth", azimuth);
         s(mapObject, "inclination", inclination);
         s(mapObject, "scale", scale);
         s(mapObject, "worldtomap", world_to_map.toJSON());
         s(mapObject, "maptoworld", map_to_world.toJSON());
         int dir = ((360 + (int)(22.5+azimuth)) / 45) % 8;
         if(MapManager.mapman.getCompassMode() != CompassMode.PRE19)
             dir = (dir + 6) % 8;
         s(mapObject, "compassview", directions[dir]);
 
     }
     
     private static final int fastFloor(double f) {
         return ((int)(f + 1000000000.0)) - 1000000000;
     }
 }
