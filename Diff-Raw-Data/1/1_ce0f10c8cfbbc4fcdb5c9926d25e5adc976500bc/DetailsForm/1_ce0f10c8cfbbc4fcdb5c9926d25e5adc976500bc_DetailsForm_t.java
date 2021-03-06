 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 package com.futurice.formrssreader;
 
 import com.futurice.tantalum3.Closure;
 import com.futurice.tantalum3.log.Log;
 import com.futurice.tantalum3.net.StaticWebCache;
 import com.futurice.tantalum3.net.xml.RSSItem;
 import com.futurice.tantalum3.rms.ImageTypeHandler;
 import javax.microedition.io.ConnectionNotFoundException;
 import javax.microedition.lcdui.*;
 
 /**
  *
  * @author vand
  */
 public final class DetailsForm extends Form implements CommandListener {
 
     private RSSReader rssReader;
     private RSSItem selectedItem;
     private static StaticWebCache imageCache = new StaticWebCache('1', new ImageTypeHandler());
     private Command openLinkCommand = new Command("Open link", Command.OK, 0);
     private Command backCommand = new Command("Back", Command.BACK, 0);
 
     public DetailsForm(RSSReader rssReader, String title) {
         super(title);
         this.rssReader = rssReader;
         this.addCommand(openLinkCommand);
         this.addCommand(backCommand);
         this.setCommandListener(this);
     }
 
     public void commandAction(Command command, Displayable d) {
         if (command == openLinkCommand) {
             openLink();
         } else if (command == backCommand) {
             ListForm.getInstance().showList();
         }
     }
 
     private void openLink() {
         try {
             boolean needsToClose = rssReader.platformRequest(ListForm.getInstance().getDetailsView().getSelectedItem().getLink());
             if (needsToClose) {
                 rssReader.exitMIDlet();
             }
         } catch (ConnectionNotFoundException connectionNotFoundException) {
             //#debug
             Log.l.log("Error opening link", ListForm.getInstance().getDetailsView().getSelectedItem().getLink(), connectionNotFoundException);
             rssReader.showError("Could not open link");
         }
     }
 
     public void setSelectedItem(RSSItem selectedItem) {
         this.selectedItem = selectedItem;
     }
 
     public RSSItem getSelectedItem() {
         return selectedItem;
     }
 
     public void paint() {
         this.deleteAll();
 
         final StringItem titleStringItem = new StringItem(null, selectedItem.getTitle(), StringItem.PLAIN);
         titleStringItem.setFont(ListForm.FONT_TITLE);
         titleStringItem.setLayout(Item.LAYOUT_NEWLINE_AFTER);
 
         final StringItem dateStringItem = new StringItem(null, selectedItem.getPubDate(), StringItem.PLAIN);
         dateStringItem.setFont(ListForm.FONT_DATE);
         dateStringItem.setLayout(Item.LAYOUT_NEWLINE_AFTER);
 
         final StringItem descriptionStringItem = new StringItem(null, selectedItem.getDescription(), StringItem.PLAIN);
         descriptionStringItem.setFont(ListForm.FONT_DESCRIPTION);
         descriptionStringItem.setLayout(Item.LAYOUT_NEWLINE_AFTER);
 
         this.append(titleStringItem);
         this.append(dateStringItem);
         this.append(descriptionStringItem);
 
         if (selectedItem.getThumbnail() != null) {
             final Image image = (Image) imageCache.synchronousRAMCacheGet(selectedItem.getThumbnail());
             if (image != null) {
                 DetailsForm.this.appendImageItem();
             } else if (!selectedItem.isLoadingImage()) {
                 //request the thumbnail image, if not already loading
                 selectedItem.setLoadingImage(true);
                 imageCache.get(selectedItem.getThumbnail(), new Closure() {
 
                     public void run() {
                         DetailsForm.this.appendImageItem();
                         selectedItem.setLoadingImage(false);
                     }
                 });
             }
         }
     }
 
     public void appendImageItem() {
         final Image image = (Image) imageCache.synchronousRAMCacheGet(selectedItem.getThumbnail());
         final ImageItem imageItem = new ImageItem(null, image, Item.LAYOUT_CENTER, "");
         this.append(imageItem);
     }
 }
