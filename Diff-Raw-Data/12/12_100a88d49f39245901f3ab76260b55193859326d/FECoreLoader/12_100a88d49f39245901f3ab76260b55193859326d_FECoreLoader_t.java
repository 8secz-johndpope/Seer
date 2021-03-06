 package com.ForgeEssentials.coremod;
 
 import java.util.Map;

import cpw.mods.fml.relauncher.IFMLCallHook;
 import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
 
 //In the event we need to mess with ASM and such, this is the place.
 //Kindly do not reference any FE classes outside the coremod package in this class.
 //Somehow or rather a nullpointer is thrown at load, @bspkrs please fix this, if you can.
 
public class FECoreLoader implements IFMLLoadingPlugin, IFMLCallHook{
 
 	@Override
 	public String[] getLibraryRequestClass() {
 		return new String[]{"com.ForgeEssentials.coremod.Downloader"};
 	}
 
 	@Override
 	public String[] getASMTransformerClass() {
 		// So far, so good. We don't have to use ASM and may not have to.
 		return null;
 	}
 
 	@Override
 	public String getModContainerClass() {
 		return "com.ForgeEssentials.coremod.FEModContainer";
 	}
 
 	@Override
 	public String getSetupClass() {
 		return "com.ForgeEssentials.coremod.FECoreLoader";
 	}
 
 	@Override
 	public void injectData(Map<String, Object> data) {
 		// We don't need this yet.
 		
 	}
 
	@Override
	public Void call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

 }
