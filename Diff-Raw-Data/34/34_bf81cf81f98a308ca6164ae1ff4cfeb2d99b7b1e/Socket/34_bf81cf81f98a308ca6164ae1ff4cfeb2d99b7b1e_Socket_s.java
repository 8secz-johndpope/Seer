 package com.modcrafting.diablodrops.items;
 
 import org.bukkit.Bukkit;
 import org.bukkit.ChatColor;
 import org.bukkit.Material;
 import org.bukkit.inventory.meta.ItemMeta;
 import org.bukkit.inventory.meta.SkullMeta;
 import org.bukkit.material.MaterialData;
 
 import com.modcrafting.diablodrops.DiabloDrops;
 
 public class Socket extends Drop
 {
     public enum SkullType
     {
         SKELETON(0), WITHER(1), ZOMBIE(2), PLAYER(3), CREEPER(4);
         public int type;
 
         private SkullType(final int i)
         {
             type = i;
         }
 
         public byte getData()
         {
             return (byte) type;
         }
 
     }
 
     private static ChatColor color()
     {
         switch (DiabloDrops.getInstance().getSingleRandom().nextInt(3))
         {
             case 1:
                 return ChatColor.RED;
             case 2:
                 return ChatColor.BLUE;
             default:
                 return ChatColor.GREEN;
         }
     }
 
     public Socket(final Material mat)
     {
         super(mat, color(), "Socket Enhancement", ChatColor.GOLD
                 + "Put in the bottom of a furnace", ChatColor.GOLD
                 + "with another item in the top", ChatColor.GOLD
                 + "to add socket enhancements.");
         ItemMeta meta;
         if (hasItemMeta())
             meta = getItemMeta();
         else
             meta = Bukkit.getItemFactory().getItemMeta(mat);
         if (mat.equals(Material.SKULL_ITEM))
         {
             SkullMeta sk = (SkullMeta) meta;
            SkullType type = SkullType.values()[DiabloDrops.getInstance()
                    .getSingleRandom().nextInt(SkullType.values().length)];
             if (type.equals(SkullType.PLAYER))
             {
                 if (Bukkit.getServer().getOfflinePlayers().length > 0)
                 {
                     sk.setOwner(Bukkit.getServer().getOfflinePlayers()[DiabloDrops
                             .getInstance()
                             .getSingleRandom()
                             .nextInt(
                                     Bukkit.getServer().getOfflinePlayers().length)]
                             .getName());
                 }
                 else
                 {
                     if (DiabloDrops.getInstance().getSingleRandom()
                             .nextBoolean())
                     {
                         sk.setOwner("deathmarin");
                     }
                     else
                     {
                         sk.setOwner("ToppleTheNun");
                     }
                 }
             }
             MaterialData md = getData();
             md.setData(type.getData());
             setData(md);
         }
         setItemMeta(meta);
     }
 }
