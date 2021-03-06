 package demonmodders.Crymod.Client;
 
 import java.util.EnumSet;
 
 import org.lwjgl.opengl.GL11;
 
 import net.minecraft.client.Minecraft;
 import net.minecraft.src.FontRenderer;
 import net.minecraft.src.Gui;
import net.minecraft.src.GuiChat;
 import net.minecraft.src.ScaledResolution;
 
 import cpw.mods.fml.client.FMLClientHandler;
 import cpw.mods.fml.common.ITickHandler;
 import cpw.mods.fml.common.TickType;
 import demonmodders.Crymod.Common.Karma.PlayerKarma;
 import demonmodders.Crymod.Common.PlayerInfo.PlayerInfo;
 
 public class ClientTickHandler extends Gui implements ITickHandler {
 
 	private ClientTickHandler() {}
 	
 	private static final ClientTickHandler instance = new ClientTickHandler();
 	
 	public static ClientTickHandler instance() {
 		return instance;
 	}
 	
 	private final Minecraft mc = FMLClientHandler.instance().getClient();
 	
 	private PlayerInfo clientPlayerInfo = null;
 	
 	public void setClientPlayerInfo(PlayerInfo info) {
 		clientPlayerInfo = info;
 	}
 	
 	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {	}
 
 	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
 		ScaledResolution scaler = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
         int width = scaler.getScaledWidth();
         int height = scaler.getScaledHeight();
 		mc.entityRenderer.setupOverlayRendering();
 		GL11.glColor3f(1, 1, 1);
        if (type.contains(TickType.RENDER) && mc.theWorld != null && clientPlayerInfo != null && (mc.currentScreen == null || mc.currentScreen instanceof GuiChat)) {
 			GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.renderEngine.getTexture("/crymodResource/tex/gui.png"));
 			int barXStart = width / 2 - 182 / 2;
 			
 			// background of the bar
 			drawTexturedModalRect(barXStart, 10, 0, 0, 182, 5);
 
 			float karma = clientPlayerInfo.getKarma().getKarma();
 			
 			// the bar itself render
 			if (karma != 0) {
 				int rescaledKarmaWidth = (int)(Math.abs(karma) / (float)PlayerKarma.MAX_KARMA_VALUE * (float)91);
 				if (rescaledKarmaWidth > 91) {
 					rescaledKarmaWidth = 91;
 				}
 				
 				int barTextureXStart = karma > 0 ? 91 : 91 - rescaledKarmaWidth;
 				
 				drawTexturedModalRect(barXStart + barTextureXStart, 10, barTextureXStart, 5, rescaledKarmaWidth, 5);
 			}
 			
 			String karmaString = String.valueOf((int)karma);
 			
 			int xPos = width / 2 - mc.fontRenderer.getStringWidth(karmaString) / 2;
 			int yPos = 2;
 			
 			FontRenderer fr = mc.fontRenderer;
 			fr.drawString(karmaString, xPos + 1, yPos, 0x000000);
 			fr.drawString(karmaString, xPos - 1, yPos, 0x000000);
 			fr.drawString(karmaString, xPos, yPos + 1, 0x000000);
 			fr.drawString(karmaString, xPos, yPos - 1, 0x000000);
 			
 			if (karma >= 0) {
 				fr.drawString(karmaString, xPos, yPos, 0x4444FF);
 			} else {
 				fr.drawString(karmaString, xPos, yPos, 0xFF0000);
 			}
 			
 			fr.drawString(String.valueOf(clientPlayerInfo.getFlyTime()), 10, 10, 0xffffff);
 		}
 	}
 
 	@Override
 	public EnumSet<TickType> ticks() {
 		return EnumSet.of(TickType.RENDER);
 	}
 
 	@Override
 	public String getLabel() {
 		return "SummoningModClientTicker";
 	}
 
 }
