 package openccsensors.client.sensorperipheral;
 
import java.util.ArrayList;
import java.util.HashMap;

 import net.minecraft.src.ModelBase;
import net.minecraft.src.ModelBox;
 import net.minecraft.src.ModelRenderer;
 import cpw.mods.fml.common.Side;
 import cpw.mods.fml.common.asm.SideOnly;
 
 @SideOnly(Side.CLIENT)
 public class ModelSensor extends ModelBase
 {
     /** The board on a sign that has the writing on it. */
     public ModelRenderer sensorBase = (new ModelRenderer(this, 0, 0)).setTextureSize(64, 64);
     public ModelRenderer sensorAxel;
     public ModelRenderer sensorDishCenter;
     public ModelRenderer sensorDishLeft;
     public ModelRenderer sensorDishRight;
    public HashMap<Integer,ModelRenderer> sensorIcons;
     public ModelSensor()
     {
         this.sensorBase.addBox(-8.0F, -8.0F, -8.0F, 16, 4, 16, 0.0F);
         this.sensorAxel = new ModelRenderer(this,0,0).setTextureSize(64, 64);
         this.sensorAxel.addBox(-0.5F, 0.0F, -0.5F, 1, 4, 1, 0.0F);
         this.sensorAxel.setRotationPoint(0.0F, -4.0F, 0.0F);
         
         this.sensorDishCenter = new ModelRenderer(this,0,24).setTextureSize(64, 64);
         this.sensorDishCenter.addBox(0, 0, 0, 7, 6, 1);
         this.sensorDishCenter.setRotationPoint(-3.0F, 4.0F, -0.5F);
         
         this.sensorDishLeft = new ModelRenderer(this,32,24).setTextureSize(64, 64);
         this.sensorDishLeft.addBox(0.0F, 0.0F, 0.0F, 4, 6, 1, 0.0F);
         this.sensorDishLeft.setRotationPoint(0.0F, 0.0F, 1.0F);
         this.sensorDishLeft.rotateAngleY = -(7.0F/6.0F)*(float)(Math.PI);
         
         this.sensorDishRight = new ModelRenderer(this,48,24).setTextureSize(64, 64);
         this.sensorDishRight.addBox(3.5F, 0.0F, 0.0F, 4, 6, 1, 0.0F);
         this.sensorDishRight.setRotationPoint(3.5F, 0.0F, 2.0F);
         this.sensorDishRight.rotateAngleY = (float) (Math.PI/6);
         
         this.sensorAxel.addChild(sensorDishCenter);
         this.sensorDishCenter.addChild(sensorDishLeft);
         this.sensorDishCenter.addChild(sensorDishRight);   
        sensorIcons = new HashMap<Integer, ModelRenderer>();
        
     }
 
     /**
      * Renders the sign model through TileEntitySignRenderer
      */
     public void renderSensor(float degrees)
     {
         this.sensorBase.render(0.0625F);
         this.sensorAxel.rotateAngleY = (degrees*(float)Math.PI/180F)%360;
         this.sensorAxel.render(0.0625F);
     }
     
     public void renderIcon(int iconIndex)
     {
     	iconIndex--;
    	ModelRenderer currentIconRenderer;
    	if (!sensorIcons.containsKey(iconIndex))
    	{
    		currentIconRenderer = new ModelRenderer(this, 16*(iconIndex%16), 16*((int)Math.floor(iconIndex/16.0F+0.5F))).setTextureSize(256, 256);
    		currentIconRenderer.addBox(-8.0F, 3.6F, -20.0F, 16, 0, 16, 0.0F);
    		sensorIcons.put(iconIndex, currentIconRenderer);
    	}
    	else
    	{
    		currentIconRenderer = sensorIcons.get(iconIndex);
    	}
    	
    	currentIconRenderer.rotateAngleX = (float) (Math.PI);
    	currentIconRenderer.render(0.0625F);
     }
 }
