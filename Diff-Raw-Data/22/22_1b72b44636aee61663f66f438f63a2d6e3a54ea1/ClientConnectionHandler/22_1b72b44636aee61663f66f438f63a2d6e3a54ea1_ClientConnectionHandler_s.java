 package com.ForgeEssentials.client.network;
 
 import com.ForgeEssentials.client.core.PlayerInfoClient;
 import com.ForgeEssentials.client.core.ProxyClient;
 
 import net.minecraft.server.MinecraftServer;
 import net.minecraft.src.INetworkManager;
 import net.minecraft.src.NetHandler;
 import net.minecraft.src.NetLoginHandler;
 import net.minecraft.src.Packet1Login;
 import cpw.mods.fml.client.FMLClientHandler;
 import cpw.mods.fml.common.FMLCommonHandler;
 import cpw.mods.fml.common.Side;
 import cpw.mods.fml.common.asm.SideOnly;
 import cpw.mods.fml.common.network.IConnectionHandler;
 import cpw.mods.fml.common.network.Player;
 
 @SideOnly(Side.CLIENT)
 public class ClientConnectionHandler implements IConnectionHandler
 {
 
 	@Override // SERVER IGNORE!!
 	public void playerLoggedIn(Player player, NetHandler netHandler, INetworkManager manager)
 	{
 		
 	}
 
 	@Override // SERVER! IGNORE!!!
 	public String connectionReceived(NetLoginHandler netHandler, INetworkManager manager)
 	{
 		// TODO Auto-generated method stub
 		return null;
 	}
 
	@Override  // client
 	public void connectionOpened(NetHandler netClientHandler, String server, int port, INetworkManager manager)
 	{
		// TODO Auto-generated method stub
 	}
 
	@Override // client
 	public void connectionOpened(NetHandler netClientHandler, MinecraftServer server, INetworkManager manager)
 	{
		// TODO Auto-generated method stub
 	}
 
 	@Override // both
 	public void connectionClosed(INetworkManager manager)
 	{
 		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
 			ProxyClient.info = null;
 	}
 
 	@Override // client
 	public void clientLoggedIn(NetHandler clientHandler, INetworkManager manager, Packet1Login login)
 	{
		ProxyClient.info = new PlayerInfoClient();
 	}
 
 }
