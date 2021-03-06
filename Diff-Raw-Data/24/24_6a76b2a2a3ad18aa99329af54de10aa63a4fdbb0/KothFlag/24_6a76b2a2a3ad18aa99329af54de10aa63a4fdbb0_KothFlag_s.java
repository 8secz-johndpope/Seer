 package net.dmulloy2.ultimatearena.arenas.objects;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import net.dmulloy2.ultimatearena.UltimateArena;
 import net.dmulloy2.ultimatearena.arenas.KOTHArena;
 import net.dmulloy2.ultimatearena.util.FormatUtil;
 import net.dmulloy2.ultimatearena.util.Util;
 
 import org.bukkit.Location;
 import org.bukkit.entity.Player;
 
 public class KothFlag extends ArenaFlag
 {
 	private ArenaPlayer leader;
 	private KOTHArena marena;
 	public KothFlag(KOTHArena arena, Location loc, final UltimateArena plugin)
 	{
 		super(arena, loc, plugin);
 		this.marena = arena;
 	}
 	
 	@Override
 	public void checkNear(List<ArenaPlayer> arenaplayers) 
 	{
 		int amt = 0;
 		ArenaPlayer capturer = null;
 		List<Player> players = new ArrayList<Player>();
 		for (int i = 0; i < arenaplayers.size(); i++)
 		{
 			ArenaPlayer apl = arenaplayers.get(i);
 			Player pl = apl.getPlayer();
 			if (pl != null)
 			{
 				if (Util.pointDistance(pl.getLocation(), getLoc()) < 3.0 && pl.getHealth() > 0) 
 				{
 					players.add(pl);
 					amt++;
 					capturer = apl;
 				}
 			}
 		}
 		
 		if (amt == 1) 
 		{
 			if (capturer != null) 
 			{
 				Player pl = capturer.getPlayer();
 				capturer.setPoints(capturer.getPoints() + 1);
 				
 				pl.sendMessage(plugin.getPrefix() + 
 						FormatUtil.format("&7You have capped for &d1 &7point! (&d{0}&7/&d{1}&7)", capturer.getPoints(), marena.MAXPOWER));
 				
 				leadChange();
 			}
 		}
 	}
 	
 	private void leadChange()
 	{
 		// Build kills map
 		HashMap<String, Integer> pointsMap = new HashMap<String, Integer>();
 		for (int i = 0; i < marena.getArenaPlayers().size(); i++)
 		{
 			ArenaPlayer ap = marena.getArenaPlayers().get(i);
 			if (ap != null && ! ap.isOut())
 			{
 				pointsMap.put(ap.getUsername(), ap.getPoints());
 			}
 		}
 		
 		final List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<Map.Entry<String, Integer>>(pointsMap.entrySet());
 		Collections.sort(
 		sortedEntries, new Comparator<Map.Entry<String, Integer>>()
 		{
 			@Override
 			public int compare(final Entry<String, Integer> entry1, final Entry<String, Integer> entry2)
 			{
 				return -entry1.getValue().compareTo(entry2.getValue());
 			}
 		});
 		
 		int pos = 1;
 		for (Map.Entry<String, Integer> entry : sortedEntries)
 		{
 			if (pos > 1)
 				return;
 			
 			String string = entry.getKey();
 			ArenaPlayer apl = plugin.getArenaPlayer(Util.matchPlayer(string));
 			if (apl != null)
 			{
 				if (leader == null || ! apl.getUsername().equals(leader.getUsername()))
 				{
 					marena.tellPlayers("&b{0} has taken the lead!", apl.getUsername());
 					this.leader = apl;
 				}
 				pos++;
 			}
 		}
 	}
 }
