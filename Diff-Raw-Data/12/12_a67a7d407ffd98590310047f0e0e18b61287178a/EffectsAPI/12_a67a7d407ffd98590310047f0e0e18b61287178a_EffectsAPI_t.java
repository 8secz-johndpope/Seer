 package com.modcrafting.diablodrops.effects;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 import org.bukkit.ChatColor;
 import org.bukkit.Material;
 import org.bukkit.entity.LivingEntity;
 import org.bukkit.entity.Player;
 import org.bukkit.event.entity.EntityDamageByEntityEvent;
 import org.bukkit.event.entity.EntityDamageEvent;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.inventory.meta.ItemMeta;
 import org.bukkit.potion.PotionEffect;
 import org.bukkit.potion.PotionEffectType;
 
 public class EffectsAPI
 {
     public static String ATTACK = "attack";
     public static String DEFENSE = "defense";
     public static String FREEZE = "freeze";
     public static String SHRINK = "shrink";
     public static String LIGHTNING = "lightning";
     public static String FIRE = "fire";
     public static String ENTOMB = "entomb";
     public static String LEECH = "leech";
 
     public static void addEffect(final LivingEntity damaged,
             final LivingEntity damager, final String s,
             final EntityDamageByEntityEvent event)
     {
         String[] args = s.split(" ");
         if (args.length <= 1)
             return;
         Integer level = null;
         try
         {
             level = Integer.valueOf(args[0]);
         }
         catch (NumberFormatException e)
         {
             level = 0;
         }
         if (args[1].equalsIgnoreCase(ATTACK))
         {
             // Add to strike damage
             int damage = event.getDamage() + level;
             if (damage >= 0)
             {
                 event.setDamage(damage);
             }
             else
             {
                 event.setDamage(0);
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(DEFENSE))
         {
             int damage = event.getDamage() - level;
             if (damage >= 0)
             {
                 event.setDamage(damage);
             }
             else
             {
                 event.setDamage(0);
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(SHRINK) && (damaged != null))
         {
             // turn into baby
             EffectsUtil.makeBaby(damaged);
             return;
         }
         else if (args[1].equalsIgnoreCase(LIGHTNING))
         {
             // strike lightning
             if ((level > 0) && (damaged != null))
             {
                 EffectsUtil.strikeLightning(damaged.getLocation(),
                         Math.abs(level));
             }
             else if ((level < 0) && (damager != null))
             {
                 EffectsUtil.strikeLightning(damager.getLocation(),
                         Math.abs(level));
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(FIRE))
         {
             // Set entity on fire
             if ((level > 0) && (damaged != null))
             {
                 EffectsUtil.setOnFire(damaged, Math.abs(level));
             }
             else if ((level < 0) && (damager != null))
             {
                 EffectsUtil.setOnFire(damager, Math.abs(level));
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(ENTOMB))
         {
             if ((level > 0) && (damaged != null))
             {
                 EffectsUtil.entomb(damaged.getLocation(), Math.abs(level));
             }
             else if ((level < 0) && (damager != null))
             {
                 EffectsUtil.entomb(damager.getLocation(), Math.abs(level));
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(LEECH) && (damager != null)
                 && (damager != null))
         {
             if (level > 0)
             {
                 int chng = level - damaged.getHealth();
                 if ((chng < damaged.getMaxHealth()) && (chng > 0))
                 {
                     damaged.setHealth(chng);
                 }
                 else
                 {
                     damaged.setHealth(0);
                 }
                 chng = level + damager.getHealth();
                 if ((chng < damager.getMaxHealth()) && (chng > 0))
                 {
                     damager.setHealth(chng);
                 }
                 else
                 {
                     damager.setHealth(damager.getMaxHealth());
                 }
             }
             else if (level < 0)
             {
                 int chng = Math.abs(level) + damaged.getHealth();
                 if ((chng < damaged.getMaxHealth()) && (chng > 0))
                 {
                     damager.setHealth(chng);
                 }
                 else
                 {
                     damager.setHealth(damager.getMaxHealth());
                 }
                 chng = Math.abs(level) - damager.getHealth();
                 if ((chng < damager.getMaxHealth()) && (chng > 0))
                 {
                     damaged.setHealth(chng);
                 }
                 else
                 {
                     damaged.setHealth(0);
                 }
             }
             return;
         }
         else
         {
             for (PotionEffectType potionEffect : PotionEffectType.values())
                 if ((potionEffect != null)
                         && potionEffect.getName().equalsIgnoreCase(args[1]))
                     if ((level > 0) && (damaged != null))
                     {
                         damaged.addPotionEffect(new PotionEffect(potionEffect,
                                 Math.abs(level) * 100, Math.abs(level) - 1));
                     }
                     else if ((level < 0) && (damager != null))
                     {
                         damager.addPotionEffect(new PotionEffect(potionEffect,
                                 Math.abs(level) * 100, Math.abs(level) - 1));
                     }
             return;
         }
     }
 
     public static void addEffect(final LivingEntity struck,
             final LivingEntity striker, final String string,
             final EntityDamageEvent event)
     {
 
         String[] args = string.split(" ");
         if (args.length <= 1)
             return;
         Integer level = null;
         try
         {
             level = Integer.valueOf(args[0]);
         }
         catch (NumberFormatException e)
         {
             level = 0;
         }
         if (args[1].equalsIgnoreCase(ATTACK))
         {
             // Add to strike damage
             int damage = event.getDamage() + level;
             if (damage >= 0)
             {
                 event.setDamage(damage);
             }
             else
             {
                 event.setDamage(0);
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(DEFENSE))
         {
             int damage = event.getDamage() - level;
             if (damage >= 0)
             {
                 event.setDamage(damage);
             }
             else
             {
                 event.setDamage(0);
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(SHRINK) && (struck != null))
         {
             // turn into baby
             EffectsUtil.makeBaby(struck);
             return;
         }
         else if (args[1].equalsIgnoreCase(LIGHTNING))
         {
             // strike lightning
             if ((level > 0) && (struck != null))
             {
                 EffectsUtil.strikeLightning(struck.getLocation(),
                         Math.abs(level));
             }
             else if ((level < 0) && (striker != null))
             {
                 EffectsUtil.strikeLightning(striker.getLocation(),
                         Math.abs(level));
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(FIRE))
         {
             // Set entity on fire
             if ((level > 0) && (struck != null))
             {
                 EffectsUtil.setOnFire(struck, Math.abs(level));
             }
             else if ((level < 0) && (striker != null))
             {
                 EffectsUtil.setOnFire(striker, Math.abs(level));
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(ENTOMB))
         {
             if ((level > 0) && (struck != null))
             {
                 EffectsUtil.entomb(struck.getLocation(), Math.abs(level));
             }
             else if ((level < 0) && (striker != null))
             {
                 EffectsUtil.entomb(striker.getLocation(), Math.abs(level));
             }
             return;
         }
         else if (args[1].equalsIgnoreCase(LEECH) && (striker != null)
                 && (struck != null))
         {
             if (level > 0)
             {
                 int chng = level - struck.getHealth();
                 if ((chng < struck.getMaxHealth()) && (chng > 0))
                 {
                     struck.setHealth(chng);
                 }
                 else
                 {
                     struck.setHealth(0);
                 }
                 chng = level + striker.getHealth();
                 if ((chng < striker.getMaxHealth()) && (chng > 0))
                 {
                     striker.setHealth(chng);
                 }
                 else
                 {
                     striker.setHealth(striker.getMaxHealth());
                 }
             }
             else if (level < 0)
             {
                 int chng = Math.abs(level) + struck.getHealth();
                 if ((chng < struck.getMaxHealth()) && (chng > 0))
                 {
                     striker.setHealth(chng);
                 }
                 else
                 {
                     striker.setHealth(striker.getMaxHealth());
                 }
                 chng = Math.abs(level) - striker.getHealth();
                 if ((chng < striker.getMaxHealth()) && (chng > 0))
                 {
                     struck.setHealth(chng);
                 }
                 else
                 {
                     struck.setHealth(0);
                 }
             }
             return;
         }
         else
         {
             for (PotionEffectType potionEffect : PotionEffectType.values())
                 if ((potionEffect != null)
                         && potionEffect.getName().equalsIgnoreCase(args[1]))
                     if ((level > 0) && (struck != null))
                     {
                         struck.addPotionEffect(new PotionEffect(potionEffect,
                                 Math.abs(level) * 100, Math.abs(level) - 1));
                     }
                     else if ((level < 0) && (striker != null))
                     {
                         striker.addPotionEffect(new PotionEffect(potionEffect,
                                 Math.abs(level) * 100, Math.abs(level) - 1));
                     }
             return;
         }
     }
 
     public static void handlePluginEffects(final LivingEntity damaged,
             final LivingEntity damager, final EntityDamageByEntityEvent event)
     {
         if (damager instanceof Player)
         {
             Player striker = (Player) damager;
             List<ItemStack> strikerEquipment = new ArrayList<ItemStack>();
             strikerEquipment.add(striker.getItemInHand());
             for (String s : listEffects(strikerEquipment))
             {
                 addEffect(damaged, damager, s, event);
             }
         }
         if (damaged instanceof Player)
         {
             Player struck = (Player) damaged;
             List<ItemStack> struckEquipment = new ArrayList<ItemStack>();
             struckEquipment.addAll(Arrays.asList(struck.getInventory()
                     .getArmorContents()));
             for (String s : listEffects(struckEquipment))
             {
                 addEffect(damager, damaged, s, event);
             }
         }
     }
 
     /**
      * Handles any effects caused by an EntityDamageEvent
      * 
      * @param entityStruck
      *            Entity damaged by event
      * @param entityStriker
      *            Entity that caused the damage
      * @param event
      *            EntityDamageEvent that requires effects
      */
     public static void handlePluginEffects(final LivingEntity entityStruck,
             final LivingEntity entityStriker, final EntityDamageEvent event)
     {
         if (entityStriker instanceof Player)
         {
             Player striker = (Player) entityStriker;
             List<ItemStack> strikerEquipment = new ArrayList<ItemStack>();
             strikerEquipment.add(striker.getItemInHand());
             for (String s : listEffects(strikerEquipment))
             {
                 addEffect(entityStruck, entityStriker, s, event);
             }
         }
         if (entityStruck instanceof Player)
         {
             Player struck = (Player) entityStruck;
             List<ItemStack> struckEquipment = new ArrayList<ItemStack>();
             struckEquipment.addAll(Arrays.asList(struck.getInventory()
                     .getArmorContents()));
             for (String s : listEffects(struckEquipment))
             {
                 addEffect(entityStriker, entityStruck, s, event);
             }
         }
     }
 
     public static List<String> listEffects(final List<ItemStack> equipment)
     {
         Set<ItemStack> toolSet = new HashSet<ItemStack>();
         for (ItemStack is : equipment)
             if ((is != null) && !is.getType().equals(Material.AIR))
             {
                 toolSet.add(new ItemStack(is));
             }
         List<String> effects = new ArrayList<String>();
         for (ItemStack tool : toolSet)
         {
            if (!tool.hasItemMeta())
                continue;
            ItemMeta meta = tool.getItemMeta();
             if ((meta.getLore() == null) || meta.getLore().isEmpty())
                 continue;
             for (String string : meta.getLore())
             {
                 string = ChatColor.stripColor(string).replace("%", "")
                         .replace("+", "");
                 effects.add(string);
             }
         }
         return effects;
     }
 }
