 package net.canarymod.hook.world;
 
 
 import net.canarymod.api.world.blocks.Block;
 import net.canarymod.hook.CancelableHook;
 
 
 /**
 * RedstoneChange hook. Contains information about a liquid flowing from one block to another
  * @author Chris Ksoll
 *
  */
 public final class RedstoneChangeHook extends CancelableHook {
 
     private Block sourceBlock;
     private int oldLevel, newLevel;
 
     public RedstoneChangeHook(Block source, int oldLevel, int newLevel) {
         this.sourceBlock = source;
         this.oldLevel = oldLevel;
         this.newLevel = newLevel;
     }
 
     /**
      * Gets the {@link Block} the redstone is on
      * @return
      */
     public Block getSourceBlock() {
         return sourceBlock;
     }
 
     /**
      * Get the power level for the redstone before the change
      * @return
      */
    public int getOldLEvel() {
         return oldLevel;
     }
 
     /**
      * get the powerlevel for redstone that it would be after the change
      * @return
      */
     public int getNewLevel() {
         return newLevel;
     }

    /**
     * Override the new power level for the redstone
     * @param newLevel
     */
    public void setNewLevel(int newLevel) {
        this.newLevel = newLevel;
    }
 }
