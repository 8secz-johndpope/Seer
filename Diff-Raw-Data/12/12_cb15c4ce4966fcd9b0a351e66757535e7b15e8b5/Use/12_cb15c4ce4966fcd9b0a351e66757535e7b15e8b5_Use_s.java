 package cmd;
 import obj.Item;
 import obj.Player;
 import iface.Command;
 
 public class Use implements Command {
    private String itemName;
 
    public void exec (Player p) {
       Item item = p.getItem(itemName);
 
      if (item != null && item.getNumUses() > 0)
         item.use(p);
       else 
         System.out.println("You can't use this item anymore.");
    }
 
    public void construct(String params[]){
 	   if (params.length > 0)
          setName(params[0]);
    }
    
    public void setDir (String dir) {}
 
    public void setName (String itemToBeUsed) {
       itemName = itemToBeUsed;
    }
    
    public String toString () {
       return "Usage: Use <item_name>\nUses an item in the inventory. For example, if you have a gun, it will shoot the gun. Some items have a limited number of uses.";
    }
 }
 
 
