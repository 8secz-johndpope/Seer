 package com.mcf.davidee.guilib;
 
 import java.util.Arrays;
 
 import cpw.mods.fml.common.Mod;
 import cpw.mods.fml.common.Mod.EventHandler;
 import cpw.mods.fml.common.ModMetadata;
 import cpw.mods.fml.common.event.FMLPreInitializationEvent;
 
@Mod(modid = "guilib", name = "GUI Library", version = "1.0.4")
 public class GuiLibrary {
 
 	
 	@EventHandler 
 	public void preInit(FMLPreInitializationEvent event) {
 		ModMetadata modMeta = event.getModMetadata();
 		modMeta.authorList = Arrays.asList(new String[] { "Davidee" });
 		modMeta.autogenerated = false;
 		modMeta.credits = "Thanks to Mojang, Forge, and all your support.";
 		modMeta.description = "A small library for creating minecraft GUIs.";
 		modMeta.url = "http://www.minecraftforum.net/topic/1909236-/";
 	}
 	
 }
