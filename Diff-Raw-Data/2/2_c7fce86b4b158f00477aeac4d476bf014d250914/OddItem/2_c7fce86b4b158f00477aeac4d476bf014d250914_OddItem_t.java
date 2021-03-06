 /* This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package info.somethingodd.bukkit.OddItem;
 
 import info.somethingodd.bukkit.OddItem.bktree.BKTree;
 import org.bukkit.Material;
 import org.bukkit.entity.Player;
 import org.bukkit.inventory.ItemStack;
 
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.SortedSet;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 import java.util.concurrent.ConcurrentNavigableMap;
 
 /**
  * @author Gordon Pettey (petteyg359@gmail.com)
  */
 public final class OddItem extends OddItemBase {
     protected static ConcurrentNavigableMap<String, SortedSet<String>> items = null;
     protected static ConcurrentHashMap<String, OddItemGroup> groups = null;
     protected static ConcurrentMap<String, ItemStack> itemMap = null;
     protected static BKTree<String> bktree = null;
 
     protected static void clear() {
         items = null;
         groups = null;
         itemMap = null;
         bktree = null;
     }
 
     /**
      * Compares two ItemStack material and durability, ignoring quantity
      *
      * @param a ItemStack to compare
      * @param b ItemStack to compare
      * @return ItemStack are equal
      */
     public static Boolean compare(ItemStack a, ItemStack b) {
         return compare(a, b, true, false);
     }
 
     /**
      * Compares two ItemStack material, durability, and quantity
      *
      * @param a        ItemStack to compare
      * @param b        ItemStack to compare
      * @param quantity whether to compare quantity
      * @return ItemStack are equal
      */
     public static Boolean compare(ItemStack a, ItemStack b, Boolean quantity) {
         return compare(a, b, true, quantity);
     }
 
     /**
      * Compares two ItemStack
      *
      * @param a          ItemStack to compare
      * @param b          ItemStack to compare
      * @param quantity   whether to compare quantity
      * @param durability whether to compare durability
      * @return ItemStack are equal
      */
     public static Boolean compare(ItemStack a, ItemStack b, Boolean durability, Boolean quantity) {
        Boolean ret = a.getTypeId() == b.getTypeId();
         if (durability) ret &= (a.getDurability() == b.getDurability());
         if (ret && quantity) ret &= (a.getAmount() == b.getAmount());
         return ret;
     }
 
     /**
      * Gets all aliases for an item
      *
      * @param query name of item
      * @return names of aliases
      * @throws IllegalArgumentException exception if no such item exists
      */
     public static List<String> getAliases(String query) throws IllegalArgumentException {
         List<String> s = new ArrayList<String>();
         ItemStack i = itemMap.get(query);
         if (i == null)
             throw new IllegalArgumentException("no such item");
         String b = Integer.toString(i.getTypeId());
         int d = i.getDurability();
         if (d != 0)
             b += ";" + Integer.toString(i.getDurability());
         if (items.get(b) != null)
             s.addAll(items.get(b));
         if (d == 0 && items.get(b + ";0") != null)
             s.addAll(items.get(b + ";0"));
         return s;
     }
 
     /**
      * @return list of all groups
      */
     public static List<String> getGroups() {
         return getGroups("");
     }
 
     /**
      * Returns group names that start with string
      *
      * @param group name to look for
      * @return list of matching groups
      */
     public static List<String> getGroups(String group) {
         List<String> gs = new ArrayList<String>();
         for (String g : groups.keySet()) {
             if (group.equals("") || (g.length() >= group.length() && g.regionMatches(true, 0, group, 0, group.length())))
                 gs.add(g);
         }
         return gs;
     }
 
     /**
      * @param query item group name
      * @return OddItemGroup
      * @throws IllegalArgumentException exception if no such group exists
      */
     public static OddItemGroup getItemGroup(String query) throws IllegalArgumentException {
         if (groups.get(query) == null) throw new IllegalArgumentException("no such group");
         return groups.get(query);
     }
 
     /**
      * @return Set&lt;ItemStack&gt; all defined items
      */
     public static Set<ItemStack> getItemStacks() {
         return new HashSet<ItemStack>(itemMap.values());
     }
 
     /**
      * Returns an ItemStack of quantity 1 of alias query
      *
      * @param query item name
      * @return ItemStack
      * @throws IllegalArgumentException exception if item not found, message contains closest match
      */
     public static ItemStack getItemStack(String query) throws IllegalArgumentException {
         return getItemStack(query, 1);
     }
 
     /**
      * Returns an ItemStack of specific quantity of alias query
      *
      * @param query    item name
      * @param quantity quantity
      * @return ItemStack
      * @throws IllegalArgumentException exception if item not found, message contains closest match
      */
     public static ItemStack getItemStack(String query, Integer quantity) throws IllegalArgumentException {
         ItemStack i;
         if (query.startsWith("map")) {
             try {
                 i = new ItemStack(Material.MAP, 1, (query.contains(";") ? Short.valueOf(query.substring(query.indexOf(";") + 1)) : 0));
             } catch (NumberFormatException e) {
                 i = new ItemStack(Material.MAP, 1, (short) 0);
             }
         } else {
             i = itemMap.get(query);
         }
         if (i != null && !query.startsWith("map")) {
             i.setAmount(quantity);
             return i;
         }
         throw new IllegalArgumentException(bktree.findBestWordMatch(query));
     }
 
     /**
      * Returns whether player's inventory contains itemStack
      * @param player Player to use inventory
      * @param itemStack ItemStack to look for
      * @return itemStack is contained in inventory
      */
     public static Boolean contains(Player player, ItemStack itemStack) {
         return contains(player, itemStack, true);
     }
 
     /**
      * Returns whether player's inventory contains itemStack, possibly ignoring quantity
      * @param player Player to use inventory
      * @param itemStack ItemStack to look for
      * @param quantity whether to check quantity
      * @return itemStack is contained in inventory
      */
     public static Boolean contains(Player player, ItemStack itemStack, boolean quantity) {
         return contains(player, itemStack, true, quantity);
     }
 
     /**
      * Returns whether player's inventory contains itemStack, possibly ignoring durability and quantity
      * @param player Player to use inventory
      * @param itemStack ItemStack to look for
      * @param durability whether to check durability
      * @param quantity whether to check quantity
      * @return itemStack is contained in inventory
      */
     public static Boolean contains(Player player, ItemStack itemStack, boolean durability, boolean quantity) {
         ItemStack[] inventory = player.getInventory().getContents();
         for (int i = 0; i < inventory.length; i++) {
             if (compare(inventory[i], itemStack, durability, quantity)) return true;
         }
         return false;
     }
 
     /**
      * Removes itemStack from player's inventory
      *
      * @param player Player to remove itemStack from
      * @param itemStack ItemStack to remove
      * @return amount left over (i.e. {@link org.bukkit.entity.Player player} had less than itemStack.getAmount() available)
      */
     public static Integer removeItem(Player player, ItemStack itemStack) {
         ItemStack[] inventory = player.getInventory().getContents();
         int amount = itemStack.getAmount();
         for (int i = 0; i < inventory.length; i++) {
             if (compare(inventory[i], itemStack)) {
                 if (amount > inventory[i].getAmount()) {
                     amount -= inventory[i].getAmount();
                     inventory[i].setAmount(0);
                 } else if (amount > 0) {
                     inventory[i].setAmount(inventory[i].getAmount() - amount);
                     amount = 0;
                 } else {
                     inventory[i].setAmount(0);
                 }
             }
             if (amount == 0) break;
         }
         return amount;
     }
 
     /**
      * Removes itemStacks from players's inventory
      *
      * @param player     Player to remove itemStacks from
      * @param itemStacks ItemStacks to remove
      * @return amounts left over (i.e. player had less than itemStack.getAmount() available)
      */
     public static Integer[] removeItem(Player player, ItemStack[] itemStacks) {
         Integer[] amount = new Integer[itemStacks.length];
         for (int i = 0; i < itemStacks.length; i++) {
             amount[i] = removeItem(player, itemStacks[i]);
         }
         return amount;
     }
 }
