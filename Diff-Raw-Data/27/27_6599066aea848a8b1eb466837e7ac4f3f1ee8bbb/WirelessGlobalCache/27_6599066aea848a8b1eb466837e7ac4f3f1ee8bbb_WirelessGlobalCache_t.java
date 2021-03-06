 package net.licks92.WirelessRedstone;
 
 import java.util.ArrayList;
 import java.util.List;
 
 import org.bukkit.Bukkit;
 import org.bukkit.Location;
 import org.bukkit.scheduler.BukkitTask;
 
 import net.licks92.WirelessRedstone.Channel.IWirelessPoint;
 import net.licks92.WirelessRedstone.Channel.WirelessChannel;
 import net.licks92.WirelessRedstone.Channel.WirelessReceiver;
 
 /**
  * This is the place where we'll store the global cache for Wireless Redstone.
  * 
  * This cache should be updated regularly, of course every addition, removal of signs
  * should update this global cache.
  * 
  * The locations of signs of each channel is in a cache, which is located in each WirelessChannel
  * object.
  * 
  * @since 1.9b
  * 
  * @author Licks
  */
 public class WirelessGlobalCache
 {
 	private ArrayList<IWirelessPoint> allSigns;
 	private ArrayList<Location> allReceiverLocations;
 	private WirelessRedstone plugin;
	@SuppressWarnings("unused") //Eclipse Motherfucker of course it is used
 	private BukkitTask refreshingTask;
 	
 	/**
 	 * Initialize the global cache.
 	 * 
 	 * @param updateFrequency - In seconds, the frequency for refreshing the cache.
 	 */
 	public WirelessGlobalCache(WirelessRedstone plugin, int updateFrequency)
 	{
 		this.plugin = plugin;
 		
 		//At plugin startup we have to directly update the cache.
 		update(false);
 		
 		int timeInTicks = updateFrequency * 20; //There are 20 ticks in a second.
 		
 		//After creating the cache at startup
 		refreshingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable()
 		{
 			@Override
 			public void run()
 			{
 				
 			}
 		}, timeInTicks, timeInTicks);
 	}
 	
 	/**
 	 * @param async - If true, the update will be done asynchronsoulsy.
 	 */
 	private void updateAllReceiverLocations(boolean async)
 	{
 		if(async)
 			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
 			{
 				public void run()
 				{
 					ArrayList<Location> returnlist = new ArrayList<Location>();
 					for (WirelessChannel channel : WirelessRedstone.config.getAllChannels())
 					{
 						try
 						{
 							for (WirelessReceiver point : channel.getReceivers())
 							{
 								returnlist.add(point.getLocation());
 							}
 						}
 						catch (Exception e)
 						{
 							
 						}
 					}
 					allReceiverLocations = returnlist;
 				}
 			});
 		else
 		{
 			ArrayList<Location> returnlist = new ArrayList<Location>();
 			for (WirelessChannel channel : WirelessRedstone.config.getAllChannels())
 			{
 				try
 				{
 					for (WirelessReceiver point : channel.getReceivers())
 					{
 						returnlist.add(point.getLocation());
 					}
 				}
 				catch (Exception e)
 				{
 					
 				}
 			}
 			allReceiverLocations = returnlist;
 		}
 			
 	}
 	
 	/**
 	 * @param async - If true, the update will be done asynchronsoulsy.
 	 */
 	private void updateAllSigns(boolean async)
 	{
 		if(async)
 			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
 			{
 				public void run()
 				{
 					ArrayList<IWirelessPoint> returnlist = new ArrayList<IWirelessPoint>();
 					for (WirelessChannel channel : WirelessRedstone.config.getAllChannels())
 					{
 						try
 						{
 							for (IWirelessPoint point : channel.getReceivers())
 							{
 								returnlist.add(point);
 							}
 
 							for (IWirelessPoint point : channel.getTransmitters())
 							{
 								returnlist.add(point);
 							}
 							
 							for (IWirelessPoint point : channel.getScreens())
 							{
 								returnlist.add(point);
 							}
 						}
 						catch (Exception e)
 						{
 
 						}
 					}
 					allSigns = returnlist;
 				}
 			});
 		else
 		{
 			ArrayList<IWirelessPoint> returnlist = new ArrayList<IWirelessPoint>();
 			for (WirelessChannel channel : WirelessRedstone.config.getAllChannels())
 			{
 				try
 				{
 					for (IWirelessPoint point : channel.getReceivers())
 					{
 						returnlist.add(point);
 					}
 
 					for (IWirelessPoint point : channel.getTransmitters())
 					{
 						returnlist.add(point);
 					}
 					
 					for (IWirelessPoint point : channel.getScreens())
 					{
 						returnlist.add(point);
 					}
 				}
 				catch (Exception e)
 				{
 
 				}
 			}
 			allSigns = returnlist;
 		}
 	}
 	
 	/**
 	 * Update the global cache of WirelessRedstone asynchronously.
 	 */
 	public void update()
 	{
 		update(true);
 	}
 	
 	/**
 	 * Update the global cache.
 	 * 
 	 * @param async - If true, the cache will be update asynchronously.
 	 */
 	public void update(boolean async)
 	{
 		updateAllReceiverLocations(async);
 		updateAllSigns(async);
 	}
 	
 	/**
 	 * @return A list of all the receiver locations.
 	 */
 	public List<Location> getAllReceiverLocations()
 	{
 		return allReceiverLocations;
 	}
 	
 	/**
 	 * @return A list which contains all the wireless points in the server.
 	 */
 	public List<IWirelessPoint> getAllSigns()
 	{
 		return allSigns;
 	}
 }
