 package com.ForgeEssentials.commands;
 
 import com.ForgeEssentials.AreaSelector.Point;
 import com.ForgeEssentials.api.permissions.PermQueryPlayer;
 import com.ForgeEssentials.api.permissions.PermissionsHandler;
 import com.ForgeEssentials.core.Localization;
 import com.ForgeEssentials.core.PlayerInfo;
 import com.ForgeEssentials.core.commands.ForgeEssentialsCommandBase;
 import com.ForgeEssentials.util.DataStorage;
 import com.ForgeEssentials.util.OutputHandler;
 
 import net.minecraft.src.EntityPlayer;
 import net.minecraft.src.EntityPlayerMP;
 import net.minecraft.src.ICommandSender;
 import net.minecraft.src.NBTTagCompound;
 
 public class CommandWarp extends ForgeEssentialsCommandBase
 {
 	public CommandWarp()
 	{
 		super();
 		//TODO This needs to be false, but since you can set permissions yet...
 		PermissionsHandler.registerPermission(getCommandPerm() + "admin", true);
 	}
 
 	@Override
 	public String getCommandName()
 	{
 		return "warp";
 	}
 	
 	//TODO implement dimention changing!!
 
 	@Override
 	public void processCommandPlayer(EntityPlayer sender, String[] args)
 	{
 		NBTTagCompound warpdata = DataStorage.getData("warpdata");
 		if(args.length == 0)
 		{
 			sender.sendChatToPlayer(Localization.get("command.warp.list"));
 			String msg = "";
 			for(Object temp : warpdata.getTags())
 			{
 				NBTTagCompound warp = (NBTTagCompound) temp;
 				msg = warp.getName() + ", " + msg;
 			}
 			sender.sendChatToPlayer(msg);
 		}
 		else if(args.length == 1)
 		{
 			if(warpdata.hasKey(args[0].toLowerCase()))
 			{
 				if(PermissionsHandler.checkPermAllowed(new PermQueryPlayer(sender, getCommandPerm() + "." + args[0].toLowerCase())))
 				{
 					NBTTagCompound warp = warpdata.getCompoundTag(args[0].toLowerCase());
 					((EntityPlayerMP) sender).playerNetServerHandler.setPlayerLocation(warp.getDouble("X"), warp.getDouble("Y"), warp.getDouble("Z"), warp.getFloat("Yaw"), warp.getFloat("Pitch"));
 				}
 				else
 				{
					OutputHandler.chatError(sender, Localization.get("message.error.permdenied"));
 				}
 			}
 			else
 			{
 				OutputHandler.chatError(sender, Localization.get("command.warp.notfound"));
 			}
 		}
 		else if(args.length == 2)
 		{
 			if(PermissionsHandler.checkPermAllowed(new PermQueryPlayer(sender, getCommandPerm() + "admin")))
 			{
 				if(args[0].equalsIgnoreCase("set"))
 				{
 					if(warpdata.hasKey(args[1].toLowerCase()))
 					{
 						OutputHandler.chatError(sender, Localization.get("command.warp.alreadyexists"));
 					}
 					else
 					{
 						NBTTagCompound warp = new NBTTagCompound();
 							warp.setDouble("X", sender.posX);
 							warp.setDouble("Y", sender.posY);
 							warp.setDouble("Z", sender.posZ);
 							warp.setFloat("Yaw", sender.rotationYaw);
 							warp.setFloat("Pitch", sender.rotationPitch);
 						warpdata.setCompoundTag(args[1].toLowerCase(), warp);
 						
 						OutputHandler.chatConfirmation(sender, Localization.get("message.done"));
 					}
 				}
 				else if(args[0].equalsIgnoreCase("del"))
 				{
 					if(warpdata.hasKey(args[1].toLowerCase()))
 					{
 						warpdata.removeTag(args[1].toLowerCase());
 						OutputHandler.chatConfirmation(sender, Localization.get("message.done"));
 					}
 					else
 					{
 						OutputHandler.chatError(sender, Localization.get("command.warp.notfound"));
 					}
 				}
 				else
 				{
 					OutputHandler.chatError(sender, Localization.get("message.error.badsyntax") + getSyntaxPlayer(sender));
 				}
 			}
 			else
 			{
				OutputHandler.chatError(sender, Localization.get("message.error.permdenied"));
 			}
 		}
 		else
 		{
 			
 		}
 		
 		DataStorage.setData("warpdata", warpdata);
 	}
 
 	@Override
 	public void processCommandConsole(ICommandSender sender, String[] args)
 	{
 	}
 
 	@Override
 	public boolean canConsoleUseCommand()
 	{
 		return false;
 	}
 
 	@Override
 	public boolean canPlayerUseCommand(EntityPlayer player)
 	{
 		return true;
 	}
 
 	@Override
 	public String getCommandPerm()
 	{
 		return "ForgeEssentials.BasicCommands." + getCommandName();
 	}
 
 }
