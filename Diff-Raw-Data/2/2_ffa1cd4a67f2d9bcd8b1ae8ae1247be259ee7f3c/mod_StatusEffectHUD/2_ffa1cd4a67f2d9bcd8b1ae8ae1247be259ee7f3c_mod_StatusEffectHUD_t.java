 package net.minecraft.src;
 
 import java.util.Collection;
 import java.util.Iterator;
 
 import net.minecraft.client.Minecraft;
 import net.minecraft.client.gui.GuiChat;
 import net.minecraft.client.gui.ScaledResolution;
 import net.minecraft.potion.Potion;
 import net.minecraft.potion.PotionEffect;
 import net.minecraft.util.StatCollector;
 
 import org.lwjgl.opengl.GL11;
 
 import bspkrs.util.ModVersionChecker;
 import bspkrs.util.client.HUDUtils;
 
 public class mod_StatusEffectHUD extends BaseMod
 {
     protected float           zLevel               = 0.0F;
     private ScaledResolution  scaledResolution;
     @MLProp(info = "Set to true to allow checking for mod updates, false to disable")
     public static boolean     allowUpdateCheck     = true;
     @MLProp(info = "Valid alignment strings are topleft, topcenter, topright, middleleft, middlecenter, middleright, bottomleft, bottomcenter (not recommended), bottomright")
     public static String      alignMode            = "topleft";
     // @MLProp(info="Valid list mode strings are horizontal and vertical")
     // public static String listMode = "vertical";
     @MLProp(info = "Set to true to see the effect background box, false to disable")
     public static boolean     enableBackground     = false;
     @MLProp(info = "Set to true to show effect names, false to disable")
     public static boolean     enableEffectName     = true;
     @MLProp(info = "Valid color values are 0-9, a-f (color values can be found here: http://www.minecraftwiki.net/wiki/File:Colors.png)")
     public static String      effectNameColor      = "f";
     @MLProp(info = "Valid color values are 0-9, a-f (color values can be found here: http://www.minecraftwiki.net/wiki/File:Colors.png)")
     public static String      durationColor        = "f";
     @MLProp(info = "Horizontal offset from the edge of the screen (when using right alignments the x offset is relative to the right edge of the screen)")
     public static int         xOffset              = 2;
     @MLProp(info = "Vertical offset from the edge of the screen (when using bottom alignments the y offset is relative to the bottom edge of the screen)")
     public static int         yOffset              = 2;
     @MLProp(info = "Vertical offset used only for the bottomcenter alignment to avoid the vanilla HUD")
     public static int         yOffsetBottomCenter  = 41;
     @MLProp(info = "Set to true if you want the xOffset value to be applied when using a center alignment")
     public static boolean     applyXOffsetToCenter = false;
     @MLProp(info = "Set to true if you want the yOffset value to be applied when using a middle alignment")
     public static boolean     applyYOffsetToMiddle = false;
     @MLProp(info = "Set to true to show info when chat is open, false to disable info when chat is open\n\n**ONLY EDIT WHAT IS BELOW THIS**")
     public static boolean     showInChat           = true;
     
     private ModVersionChecker versionChecker;
     private final String      versionURL           = "https://dl.dropbox.com/u/20748481/Minecraft/1.4.6/statusEffectHUD.version";
     private final String      mcfTopic             = "http://www.minecraftforum.net/topic/1114612-";
     
     public mod_StatusEffectHUD()
     {
         if (allowUpdateCheck)
             versionChecker = new ModVersionChecker(getName(), getVersion(), versionURL, mcfTopic, ModLoader.getLogger());
     }
     
     @Override
     public String getName()
     {
         return "StatusEffectHUD";
     }
     
     @Override
     public String getVersion()
     {
        return "v1.7(1.4.6)";
     }
     
     @Override
     public void load()
     {
         if (allowUpdateCheck)
             versionChecker.checkVersionWithLogging();
         
         ModLoader.setInGameHook(this, true, false);
     }
     
     @Override
     public boolean onTickInGame(float f, Minecraft mc)
     {
         if ((mc.inGameHasFocus || mc.currentScreen == null || (mc.currentScreen instanceof GuiChat && showInChat)) && !mc.gameSettings.showDebugInfo && !mc.gameSettings.keyBindPlayerList.pressed)
         {
             scaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
             displayStatusEffects(mc);
         }
         
         if (allowUpdateCheck)
         {
             if (!versionChecker.isCurrentVersion())
                 for (String msg : versionChecker.getInGameMessage())
                     mc.thePlayer.addChatMessage(msg);
             allowUpdateCheck = false;
         }
         return true;
     }
     
     private int getX(int width)
     {
         if (alignMode.equalsIgnoreCase("topcenter") || alignMode.equalsIgnoreCase("middlecenter") || alignMode.equalsIgnoreCase("bottomcenter"))
             return scaledResolution.getScaledWidth() / 2 - width / 2 + (applyXOffsetToCenter ? xOffset : 0);
         else if (alignMode.equalsIgnoreCase("topright") || alignMode.equalsIgnoreCase("middleright") || alignMode.equalsIgnoreCase("bottomright"))
             return scaledResolution.getScaledWidth() - width - xOffset;
         else
             return xOffset;
     }
     
     private int getY(int rowCount, int height)
     {
         if (alignMode.equalsIgnoreCase("middleleft") || alignMode.equalsIgnoreCase("middlecenter") || alignMode.equalsIgnoreCase("middleright"))
             return (scaledResolution.getScaledHeight() / 2) - ((rowCount * height) / 2) + (applyYOffsetToMiddle ? yOffset : 0);
         else if (alignMode.equalsIgnoreCase("bottomleft") || alignMode.equalsIgnoreCase("bottomright"))
             return scaledResolution.getScaledHeight() - (rowCount * height) - yOffset;
         else if (alignMode.equalsIgnoreCase("bottomcenter"))
             return scaledResolution.getScaledHeight() - (rowCount * height) - yOffsetBottomCenter;
         else
             return yOffset;
     }
     
     private void displayStatusEffects(Minecraft mc)
     {
         Collection activeEffects = mc.thePlayer.getActivePotionEffects();
         int guiTexture = mc.renderEngine.getTexture("/gui/inventory.png");
         
         if (!activeEffects.isEmpty())
         {
             int yOffset = enableBackground ? 33 : enableEffectName ? 20 : 18;
             if (activeEffects.size() > 5 && enableBackground)
                 yOffset = 132 / (activeEffects.size() - 1);
             
             int yBase = getY(activeEffects.size(), yOffset);
             
             for (Iterator iteratorPotionEffect = activeEffects.iterator(); iteratorPotionEffect.hasNext(); yBase += yOffset)
             {
                 PotionEffect potionEffect = (PotionEffect) iteratorPotionEffect.next();
                 Potion potion = Potion.potionTypes[potionEffect.getPotionID()];
                 GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                 mc.renderEngine.bindTexture(guiTexture);
                 int xBase = getX(enableBackground ? 120 : 18 + 4 + mc.fontRenderer.getStringWidth("0:00"));
                 String potionName = "";
                 
                 if (enableEffectName)
                 {
                     potionName = StatCollector.translateToLocal(potion.getName());
                     
                     if (potionEffect.getAmplifier() == 1)
                     {
                         potionName = potionName + " II";
                     }
                     else if (potionEffect.getAmplifier() == 2)
                     {
                         potionName = potionName + " III";
                     }
                     else if (potionEffect.getAmplifier() == 3)
                     {
                         potionName = potionName + " IV";
                     }
                     
                     xBase = getX(enableBackground ? 120 : 18 + 4 + mc.fontRenderer.getStringWidth(potionName));
                 }
                 
                 String effectDuration = Potion.getDurationString(potionEffect);
                 
                 if (enableBackground)
                     HUDUtils.drawTexturedModalRect(xBase, yBase, 0, 166, 140, 32, zLevel);
                 
                 if (alignMode.toLowerCase().contains("right"))
                 {
                     xBase = getX(0);
                     if (potion.hasStatusIcon())
                     {
                         int potionStatusIcon = potion.getStatusIconIndex();
                         HUDUtils.drawTexturedModalRect(xBase + (enableBackground ? -24 : -18), yBase + (enableBackground ? 7 : 0), 0 + potionStatusIcon % 8 * 18, 166 + 32 + potionStatusIcon / 8 * 18, 18, 18, zLevel);
                     }
                     int stringWidth = mc.fontRenderer.getStringWidth(potionName);
                     mc.fontRenderer.drawStringWithShadow("\247" + effectNameColor + potionName, xBase + (enableBackground ? -10 : -4) - 18 - stringWidth, yBase + (enableBackground ? 6 : 0), 0xffffff);
                     stringWidth = mc.fontRenderer.getStringWidth(effectDuration);
                     mc.fontRenderer.drawStringWithShadow("\247" + durationColor + effectDuration, xBase + (enableBackground ? -10 : -4) - 18 - stringWidth, yBase + (enableBackground ? 6 : 0) + (enableEffectName ? 10 : 5), 0xffffff);
                 }
                 else
                 {
                     if (potion.hasStatusIcon())
                     {
                         int potionStatusIcon = potion.getStatusIconIndex();
                         HUDUtils.drawTexturedModalRect(xBase + (enableBackground ? 6 : 0), yBase + (enableBackground ? 7 : 0), 0 + potionStatusIcon % 8 * 18, 166 + 32 + potionStatusIcon / 8 * 18, 18, 18, zLevel);
                     }
                     mc.fontRenderer.drawStringWithShadow("\247" + effectNameColor + potionName, xBase + (enableBackground ? 10 : 4) + 18, yBase + (enableBackground ? 6 : 0), 0xffffff);
                     mc.fontRenderer.drawStringWithShadow("\247" + durationColor + effectDuration, xBase + (enableBackground ? 10 : 4) + 18, yBase + (enableBackground ? 6 : 0) + (enableEffectName ? 10 : 5), 0xffffff);
                 }
             }
         }
     }
 }
