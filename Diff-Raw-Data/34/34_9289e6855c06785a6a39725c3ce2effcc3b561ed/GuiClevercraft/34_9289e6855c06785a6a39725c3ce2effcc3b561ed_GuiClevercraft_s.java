 package net.minecraft.src;
 
 import java.util.*;
 import java.util.Map.Entry;
 
 import org.lwjgl.input.Mouse;
 import org.lwjgl.opengl.GL11;
 
 public class GuiClevercraft extends GuiContainer {
 	
 	private static InventoryBasic inventory = new InventoryBasic("tmp", 72);
 	private float field_35312_g;
     private boolean field_35313_h;
     private boolean field_35314_i;
     private boolean mouseOverRecipe;
     
 	public GuiClevercraft(EntityPlayer entityplayer)
     {
         super(new ContainerClevercraft(entityplayer));
         field_35312_g = 0.0F;
         field_35313_h = false;
         allowUserInput = true;
         mouseOverRecipe = false;
         ySize = 208;
     }
 	
 	protected void func_35309_a(Slot slot, int i, int j, boolean flag)
 	{
 		super.func_35309_a(slot, i, j, flag);
 		if(slot != null)
 		{
 			ItemStack itemstack1 = mc.thePlayer.inventory.getItemStack();
 			ItemStack itemstack2 = slot.getStack();
 			if(slot.inventory != inventory)
 			{
 				ContainerClevercraft container = (ContainerClevercraft)inventorySlots;
 				container.populateContainer();
 				this.updateScreen();
 			}
 		}
 	}
 	
 	static InventoryBasic getInventory()
     {
         return inventory;
     }
 	
 	public void initGui()
     {
 		super.initGui();
     	controlList.clear();
     }
 	
 	public void drawScreen(int i, int j, float f)
     {
         boolean flag = Mouse.isButtonDown(0);
        int k = field_40216_e;
        int l = field_40215_f;
         int i1 = k + 155;
         int j1 = l + 17;
         int k1 = i1 + 14;
         int l1 = j1 + 88 + 2;
         if(!field_35314_i && flag && i >= i1 && j >= j1 && i < k1 && j < l1)
         {
             field_35313_h = true;
         }
         if(!flag)
         {
             field_35313_h = false;
         }
         field_35314_i = flag;
         if(field_35313_h)
         {
             field_35312_g = (float)(j - (j1 + 8)) / ((float)(l1 - j1) - 16F);
             if(field_35312_g < 0.0F)
             {
                 field_35312_g = 0.0F;
             }
             if(field_35312_g > 1.0F)
             {
                 field_35312_g = 1.0F;
             }
             ((ContainerClevercraft)inventorySlots).func_35374_a(field_35312_g);
         }
         super.drawScreen(i, j, f);
         //----
         mouseOverRecipe = false;
         for(int j2 = 0; j2 < inventorySlots.inventorySlots.size(); j2++)
         {
             Slot slot1 = (Slot)inventorySlots.inventorySlots.get(j2);
             if(slot1 != null && slot1 instanceof SlotClevercraft && getIsMouseOverSlot(slot1, i, j))
             {
             	SlotClevercraft slotclever1 = (SlotClevercraft)slot1;
             	ItemStack itemstack = null;
             	if(slotclever1.collatedRecipe != null)
             	{
             		int y = 0;
             		mouseOverRecipe = true;
             		for (Map.Entry<Integer, Integer[]> entry : slotclever1.collatedRecipe.entrySet())
             		{
             			Integer vals[] = entry.getValue();
             			itemstack = new ItemStack(entry.getKey(), vals[0], vals[1]);
            			itemRenderer.renderItemIntoGUI(fontRenderer, mc.renderEngine, itemstack, field_40216_e-24, field_40215_f+28+y);
                		itemRenderer.renderItemOverlayIntoGUI(fontRenderer, mc.renderEngine, itemstack, field_40216_e-24, field_40215_f+28+y);
                 		y += 18;
             		}
             	}
             }
         }
         //----
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glDisable(2896 /*GL_LIGHTING*/);
     }
 	
 	private boolean getIsMouseOverSlot(Slot slot, int i, int j)
     {
        int k = field_40216_e;
        int l = field_40215_f;
         i -= k;
         j -= l;
         return i >= slot.xDisplayPosition - 1 && i < slot.xDisplayPosition + 16 + 1 && j >= slot.yDisplayPosition - 1 && j < slot.yDisplayPosition + 16 + 1;
     }
 	
 	protected void drawGuiContainerForegroundLayer()
     {
         fontRenderer.drawString("Crafting Table II", 8, 6, 0x404040);
     }
 
     protected void drawGuiContainerBackgroundLayer(float f, int i, int j)
     {
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         int k = mc.renderEngine.getTexture("/gui/crafttableii.png");
         mc.renderEngine.bindTexture(k);
        int l = field_40216_e;
        int i1 = field_40215_f;
         drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
         int j1 = l + 155;
         int k1 = i1 + 17;
         int l1 = k1 + 88 + 2;
         drawTexturedModalRect(l + 154, i1 + 17 + (int)((float)(l1 - k1 - 17) * field_35312_g), 0, 208, 16, 16);
         
         if(mouseOverRecipe)
         {
         	drawTexturedModalRect(l-25, i1, 176, 0, 18, ySize);
         }
     }
     
     public void handleMouseInput()
     {
         super.handleMouseInput();
         int i = Mouse.getEventDWheel();
         if(i != 0)
         {
             int j = (((ContainerClevercraft)inventorySlots).itemList.size() / 8 - 8) + 1;
             if(i > 0)
             {
                 i = 1;
             }
             if(i < 0)
             {
                 i = -1;
             }
             field_35312_g -= (double)i / (double)j;
             if(field_35312_g < 0.0F)
             {
                 field_35312_g = 0.0F;
             }
             if(field_35312_g > 1.0F)
             {
                 field_35312_g = 1.0F;
             }
             ((ContainerClevercraft)inventorySlots).func_35374_a(field_35312_g);
         }
     }
 }
