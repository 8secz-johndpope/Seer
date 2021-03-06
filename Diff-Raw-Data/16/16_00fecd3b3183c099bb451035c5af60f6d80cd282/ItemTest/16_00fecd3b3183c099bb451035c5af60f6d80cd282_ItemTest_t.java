 package model.item;
 
 import model.common.Barcode;
 import org.joda.time.DateTime;
 import org.junit.BeforeClass;
 import org.junit.Before;
 import org.junit.Test;
 import static org.junit.Assert.*;
 
 public class ItemTest {
 
     public static Item item;
     @BeforeClass
     public static void setup(){
         item = new Item();
        item.setBarcode(Barcode.newFromId("111"));
         item.setProductId(-1);
         item.setEntryDate(new DateTime());
         item.setExitDate(new DateTime());
         item.setExpirationDate(new DateTime());
     }
 
     @Test
     public void testItemCreation(){
         assertEquals("Id should be -1", -1, item.getId());
         assertEquals("Item is not saved", false, item.isSaved());
         assertEquals("Item is not valid", false, item.isValid());
         assertEquals("Item should be saveable because its not valid",
                 false, item.save().getStatus());
         //Technically this should be false but we havnt wired up Barcodes and other classes yet
         assertEquals("Item should pass validation", true, item.validate().getStatus());
         assertEquals("Item should save", true, item.save().getStatus());
         assertEquals("Id should be 0", 0, item.getId());
         assertEquals("Item is saved", true, item.isSaved());
         assertEquals("Item is Valid", true, item.isValid());
         assertNotSame("Vault returns a copy", item, ItemVault.get(item.getId()));
         assertEquals("Vault copy and local copy have same ids", item.getId(), ItemVault.get(item.getId()).getId());
     }
 
     @Test
     public void testItemModification(){
         Item itemCopy = ItemVault.get(item.getId());
         itemCopy.setProductId(0);
         assertTrue("Local modification doesn't change item in vault", itemCopy.getProductId()
                 != ItemVault.get(item.getId()).getProductId());
         assertEquals("Item should be saveable because its not valid",
                 false, itemCopy.save().getStatus());
         assertEquals("Item should pass validation", true, itemCopy.validate().getStatus());
         assertEquals("Item should save", true, itemCopy.save().getStatus());
         assertEquals("Vault should not have created a new item", 1, ItemVault.size());
     }
 }
