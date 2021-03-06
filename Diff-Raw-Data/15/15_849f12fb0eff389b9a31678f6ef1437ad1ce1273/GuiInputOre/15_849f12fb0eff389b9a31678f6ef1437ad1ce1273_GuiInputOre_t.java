 package org.yogpstop.tof;
 
 import net.minecraft.block.Block;
 import net.minecraft.client.Minecraft;
 import net.minecraft.client.gui.GuiButton;
 import net.minecraft.client.gui.GuiScreen;
 import net.minecraft.client.gui.GuiTextField;
 import net.minecraft.util.StatCollector;
 
 public class GuiInputOre extends GuiScreen {
 	private GuiTextField blockid;
 	private GuiTextField meta;
 
 	@Override
 	public void initGui() {
 		this.buttonList.add(new GuiButton(-1, this.width / 2 - 150, this.height - 26, 140, 20, StatCollector.translateToLocal("gui.done")));
 		this.buttonList.add(new GuiButton(-2, this.width / 2 + 10, this.height - 26, 140, 20, StatCollector.translateToLocal("gui.cancel")));
 		this.blockid = new GuiTextField(this.fontRenderer, this.width / 2 - 50, 50, 100, 20);
 		this.meta = new GuiTextField(this.fontRenderer, this.width / 2 - 50, 80, 100, 20);
 		this.blockid.setFocused(true);
 	}
 
 	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
 	public void actionPerformed(GuiButton par1) {
 		switch (par1.id) {
 		case -1:
 			short bid;
 			int metaid;
 			try {
 				bid = Short.parseShort(this.blockid.getText());
 			} catch (Exception e) {
 				this.blockid.setText(StatCollector.translateToLocal("tof.error"));
 				return;
 			}
 			try {
 				metaid = Integer.parseInt(this.meta.getText());
 			} catch (Exception e) {
 				this.meta.setText(StatCollector.translateToLocal("tof.error"));
 				return;
 			}
 			for (int i = 0; i < TimesOreForge.setting.size(); i++) {
 				if (bid == TimesOreForge.setting.get(i).blockID && metaid == TimesOreForge.setting.get(i).meta) {
 					Minecraft.getMinecraft().displayGuiScreen(
 							new GuiError(this, StatCollector.translateToLocal("tof.alreadyerror"), TimesOreForge.getname(bid, metaid)));
 					return;
 				}
 			}
 			if (bid == Block.stone.blockID) {
 				Minecraft.getMinecraft().displayGuiScreen(
 						new GuiError(this, StatCollector.translateToLocal("tof.alreadyerror"), TimesOreForge.getname(bid, metaid)));
 				return;
 			}
 			Minecraft.getMinecraft().displayGuiScreen(
 					new GuiYesNoOnAdd(StatCollector.translateToLocal("tof.addblocksure"), TimesOreForge.getname(bid, metaid), bid, metaid));
 			break;
 		case -2:
 			Minecraft.getMinecraft().displayGuiScreen(new GuiSetting());
 			break;
 		}
 	}
 
 	@Override
 	public void updateScreen() {
 		this.meta.updateCursorCounter();
 		this.blockid.updateCursorCounter();
		if (!this.mc.thePlayer.isEntityAlive() || this.mc.thePlayer.isDead) {
			this.mc.thePlayer.closeScreen();
		}
 	}
 
 	@Override
 	protected void keyTyped(char par1, int par2) {
 		if (this.blockid.isFocused()) {
 			this.blockid.textboxKeyTyped(par1, par2);
 		} else if (this.meta.isFocused()) {
 			this.meta.textboxKeyTyped(par1, par2);
 		}
		if (par2 == 1 || par1 == this.mc.gameSettings.keyBindInventory.keyCode) {
			this.mc.thePlayer.closeScreen();
		}
 	}
 
 	@Override
 	protected void mouseClicked(int par1, int par2, int par3) {
 		super.mouseClicked(par1, par2, par3);
 		this.blockid.mouseClicked(par1, par2, par3);
 		this.meta.mouseClicked(par1, par2, par3);
 	}
 
 	@Override
 	public void drawScreen(int i, int j, float k) {
 		drawDefaultBackground();
 		String title = StatCollector.translateToLocal("tof.selectblock");
 		this.fontRenderer.drawStringWithShadow(title, (this.width - this.fontRenderer.getStringWidth(title)) / 2, 8, 0xFFFFFF);
 		this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("tof.blockid"),
 				this.width / 2 - 60 - this.fontRenderer.getStringWidth(StatCollector.translateToLocal("tof.blockid")), 50, 0xFFFFFF);
 		this.fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("tof.meta"),
 				this.width / 2 - 60 - this.fontRenderer.getStringWidth(StatCollector.translateToLocal("tof.meta")), 80, 0xFFFFFF);
 		this.fontRenderer.drawString(StatCollector.translateToLocal("tof.tipsmeta"), 16, 110, 0xFFFFFF);
 		this.blockid.drawTextBox();
 		this.meta.drawTextBox();
 		super.drawScreen(i, j, k);
 	}
 
 }
