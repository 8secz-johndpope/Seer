 package num.complexwiring.machine.basic;
 
 import net.minecraft.inventory.ISidedInventory;
 import net.minecraft.item.ItemStack;
 import net.minecraft.nbt.NBTTagCompound;
 import net.minecraft.nbt.NBTTagList;
 import net.minecraft.network.packet.Packet;
 import net.minecraft.tileentity.TileEntityFurnace;
 import num.complexwiring.api.base.TileEntityInventoryBase;
 import num.complexwiring.api.vec.Vector3;
 import num.complexwiring.core.InventoryHelper;
 import num.complexwiring.core.PacketHandler;
 import num.complexwiring.recipe.OrelyzerRecipe;
 import num.complexwiring.recipe.RecipeManager;
 
 import java.io.DataInputStream;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Random;
 
 public class TileEntityBasicOrelyzer extends TileEntityInventoryBase implements ISidedInventory {
 
     private static final int[] SLOTS_OUTPUT = new int[]{2, 3};
     private static final int[] SLOTS_TOP = new int[]{0};
     private static final int[] SLOTS_BOTTOM = new int[]{2, 3, 1};
 
     private Random random = new Random();
 
     private OrelyzerRecipe recipe;
     private ArrayList<ItemStack> recipeOutput;
     private int recipeNeedTime = 0;
     public int processTime = 0;
     public int burnTime = 0;
     private int fuelBurnTime = 0;
 
     public TileEntityBasicOrelyzer() {
         super(4, EnumBasicMachine.ORELYZER.getFullUnlocalizedName());
         recipeOutput = new ArrayList<ItemStack>();
     }
 
     public void update(){
         super.update();
 
         if (!worldObj.isRemote) {
             if(recipe == null) {
                 if(canBeProcessed()) {
                     startProcessing();
                 }
             }
             if(recipe != null) {
                 if(burnTime > 0){
                     burnTime--;
                     processTime++;
 
                     if(processTime >= recipeNeedTime) {
                         endProcessing();
                     }
                 }
                 if(burnTime == 0) takeFuel();
             }
             if (ticks % 4 == 0) {
                 PacketHandler.sendPacket(getDescriptionPacket(), worldObj, Vector3.get(this));
             }
         }
         //TODO: DO NOT LOAD IT ALL THE TIME!
         worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
         onInventoryChanged();
 
     }
 
     public boolean canBeProcessed(){
         if (RecipeManager.get(getStackInSlot(0)) == null) {
             return false;
         } else {
             recipe = RecipeManager.get(getStackInSlot(0));
             takeFuel();
             return true;
         }
     }
 
     public void takeFuel(){
         if(burnTime == 0){
             if (getStackInSlot(1) != null) {
                 fuelBurnTime = getFuelBurnTime(inventory[1]);
                 if(fuelBurnTime != 0){
                     burnTime = fuelBurnTime;
                     inventory[1].stackSize--;
                     if (getStackInSlot(1).stackSize == 0) {
                         inventory[1] = inventory[1].getItem().getContainerItemStack(inventory[1]);
                     }
                 }
             } else {
                 recipe = null;
                 recipeOutput.clear();
                 recipeNeedTime = 0;
                 fuelBurnTime = 0;
                 processTime = 0;
             }
         }
     }
 
     public void startProcessing(){
         if(recipe != null &&  burnTime > 0){
             recipeNeedTime = recipe.getNeededPower();
             recipeOutput = recipe.getCompleteOutput(random);
 
             inventory[0].stackSize--;
 
             if (getStackInSlot(0).stackSize <= 0) {
                 setInventorySlotContents(0, null);
             }
         }
     }
 
     public void endProcessing(){
         if (recipeOutput != null && recipeOutput.size() > 0) {
             for (ItemStack output : recipeOutput) {
                 if (output != null && output.stackSize != 0) {
                     for (int i : SLOTS_OUTPUT) {
                         if (getStackInSlot(i) == null) {
                             setInventorySlotContents(i, output);
                             break;
                         } else {
                             int adding = InventoryHelper.canMerge(getStackInSlot(i), output);
                             if (adding > 0) {
                                 getStackInSlot(i).stackSize = getStackInSlot(i).stackSize + adding;
                                 output.splitStack(adding);
                                 if (output.stackSize == 0) break;
                             }
 
                         }
                     }
                 }
             }
         }
         recipe = null;
         recipeOutput.clear();
         recipeNeedTime = 0;
         processTime = 0;
     }
 
     public int getProcessedTimeScaled(int scale) {
         if (processTime == 0 || recipeNeedTime == 0) {
             return 0;
         }
         return processTime * scale / recipeNeedTime;
     }
 
     public int getBurnTimeScaled(int scale) {
         if (burnTime == 0 || fuelBurnTime == 0) {
             return 0;
         }
         return burnTime * scale / fuelBurnTime;
     }
 
     public int getFuelBurnTime(ItemStack is) {
         if (is != null) {
             return TileEntityFurnace.getItemBurnTime(is) / 8;
         }
         return 0;
     }
 
     @Override
     public int[] getAccessibleSlotsFromSide(int slot) {
         return slot == 0 ? SLOTS_BOTTOM : (slot == 1 ? SLOTS_TOP : SLOTS_OUTPUT);
     }
 
     @Override
     public boolean canInsertItem(int slot, ItemStack is, int side) {
        if(slot == 2 || slot == 3) return false;

         return isItemValidForSlot(slot, is);
     }
 
     @Override
     public boolean canExtractItem(int slot, ItemStack is, int side) {
         return (slot == 2 || slot == 3);
     }
 
     @Override
     public void writeToNBT(NBTTagCompound nbt) {
         super.writeToNBT(nbt);
 
        nbt.setShort("recipe", (short) RecipeManager.toRecipeID(recipe));
         nbt.setShort("burnTime", (short) burnTime);
         nbt.setShort("processTime", (short) processTime);
         nbt.setShort("recipeTime", (short) recipeNeedTime);
         nbt.setShort("fuelBurnTime", (short) fuelBurnTime);
 
         if (recipeOutput != null) {
             NBTTagList outputNBT = new NBTTagList();
             for (ItemStack is : recipeOutput) {
                 if (is != null) {
                     NBTTagCompound itemNBT = new NBTTagCompound();
                     is.writeToNBT(itemNBT);
                     outputNBT.appendTag(itemNBT);
                 }
             }
             nbt.setTag("recipeOutput", outputNBT);
         }
     }
 
     @Override
     public void readFromNBT(NBTTagCompound nbt) {
         super.readFromNBT(nbt);
 
        recipe = RecipeManager.fromRecipeID(nbt.getShort("recipe"));
         burnTime = nbt.getShort("burnTime");
         processTime = nbt.getShort("processTime");
         recipeNeedTime = nbt.getShort("recipeTime");
         fuelBurnTime = nbt.getShort("fuelBurnTime");
 
         recipeOutput.clear();
         NBTTagList outputNBT = nbt.getTagList("currentOutput");
         for (int i = 0; i < outputNBT.tagCount(); i++) {
             NBTTagCompound itemNBT = (NBTTagCompound) outputNBT.tagAt(i);
             recipeOutput.add(ItemStack.loadItemStackFromNBT(itemNBT));
         }
     }
 
     @Override
     public Packet getDescriptionPacket() {
         return PacketHandler.getPacket(this, EnumBasicMachine.ORELYZER.ordinal());
     }
 
     @Override
     public void handlePacket(DataInputStream is) throws IOException {
     }
 
 }
