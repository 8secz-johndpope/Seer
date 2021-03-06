 // Blacklist_p.java 
 // -----------------------
 // part of YaCy
 // (C) by Michael Peter Christen; mc@yacy.net
 // first published on http://www.anomic.de
 // Frankfurt, Germany, 2004
 //
 // This File is contributed by Alexander Schier
 //
 // $LastChangedDate$
 // $LastChangedRevision$
 // $LastChangedBy$
 //
 // This program is free software; you can redistribute it and/or modify
 // it under the terms of the GNU General Public License as published by
 // the Free Software Foundation; either version 2 of the License, or
 // (at your option) any later version.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
 // You must compile this file with
 // javac -classpath .:../classes Blacklist_p.java
 // if the shell's current path is HTROOT
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.net.MalformedURLException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import de.anomic.data.listManager;
 import de.anomic.http.httpRequestHeader;
 import de.anomic.index.indexAbstractReferenceBlacklist;
 import de.anomic.index.indexReferenceBlacklist;
 import de.anomic.plasma.plasmaSwitchboard;
 import de.anomic.server.serverObjects;
 import de.anomic.server.serverSwitch;
 import de.anomic.server.logging.serverLog;
 import de.anomic.yacy.yacyURL;
 
 public class Blacklist_p {
     private final static String EDIT             = "edit_";
     private final static String DISABLED         = "disabled_";
     private final static String BLACKLIST        = "blackLists_";
     private final static String BLACKLIST_MOVE   = "blackListsMove_";
     private final static String BLACKLIST_SHARED = "BlackLists.Shared";
     
     private final static String BLACKLIST_FILENAME_FILTER = "^.*\\.black$";
 
     public static serverObjects respond(final httpRequestHeader header, final serverObjects post, final serverSwitch<?> env) {
         
         // initialize the list manager
         listManager.switchboard = (plasmaSwitchboard) env;
         listManager.listsPath = new File(listManager.switchboard.getRootPath(),listManager.switchboard.getConfig("listManager.listsPath", "DATA/LISTS"));
         
         // getting the list of supported blacklist types
         final String supportedBlacklistTypesStr = indexAbstractReferenceBlacklist.BLACKLIST_TYPES_STRING;
         final String[] supportedBlacklistTypes = supportedBlacklistTypesStr.split(",");
         
         // loading all blacklist files located in the directory
         List<String> dirlist = listManager.getDirListing(listManager.listsPath, BLACKLIST_FILENAME_FILTER);
         
         String blacklistToUse = null;
         final serverObjects prop = new serverObjects();
         prop.putHTML("blacklistEngine", plasmaSwitchboard.urlBlacklist.getEngineInfo());
 
         // do all post operations
         if (post != null) {
            
             final String action = post.get("action", "");
             
             if(post.containsKey("testList")) {
             	prop.put("testlist", "1");
             	String urlstring = post.get("testurl", "");
             	if(!urlstring.startsWith("http://")) urlstring = "http://"+urlstring;
                 yacyURL testurl = null;
 				try {
 					testurl = new yacyURL(urlstring, null);
 				} catch (final MalformedURLException e) { testurl = null; }
 				if(testurl != null) {
 					prop.putHTML("testlist_url",testurl.toString());
 					if(plasmaSwitchboard.urlBlacklist.isListed(indexReferenceBlacklist.BLACKLIST_CRAWLER, testurl))
 						prop.put("testlist_listedincrawler", "1");
 					if(plasmaSwitchboard.urlBlacklist.isListed(indexReferenceBlacklist.BLACKLIST_DHT, testurl))
 						prop.put("testlist_listedindht", "1");
 					if(plasmaSwitchboard.urlBlacklist.isListed(indexReferenceBlacklist.BLACKLIST_NEWS, testurl))
 						prop.put("testlist_listedinnews", "1");
 					if(plasmaSwitchboard.urlBlacklist.isListed(indexReferenceBlacklist.BLACKLIST_PROXY, testurl))
 						prop.put("testlist_listedinproxy", "1");
 					if(plasmaSwitchboard.urlBlacklist.isListed(indexReferenceBlacklist.BLACKLIST_SEARCH, testurl))
 						prop.put("testlist_listedinsearch", "1");
 					if(plasmaSwitchboard.urlBlacklist.isListed(indexReferenceBlacklist.BLACKLIST_SURFTIPS, testurl))
 						prop.put("testlist_listedinsurftips", "1");
 				}
 				else prop.put("testlist_url","not valid");
             }
         	if (post.containsKey("selectList")) {
                 blacklistToUse = post.get("selectedListName"); 
                 if (blacklistToUse != null && blacklistToUse.length() == 0) blacklistToUse = null;
             }
             if (post.containsKey("createNewList")) {
                 /* ===========================================================
                  * Creation of a new blacklist
                  * =========================================================== */
                 
                 blacklistToUse = post.get("newListName");
                 if (blacklistToUse.trim().length() == 0) {
                     prop.put("LOCATION","");
                     return prop;
                 }   
                    
                 // Check if blacklist name only consists of "legal" characters.
                 // This is mainly done to prevent files from being written to other directories
                 // than the LISTS directory.
                 if (!blacklistToUse.matches("^[\\p{L}\\d\\+\\-_]+[\\p{L}\\d\\+\\-_.]*(\\.black){0,1}$")) {
                     prop.put("error", 1);
                     prop.putHTML("error_name", blacklistToUse);
                     blacklistToUse = null;
                 } else {
                 
                     if (!blacklistToUse.endsWith(".black")) blacklistToUse += ".black";
 
                     if (!dirlist.contains(blacklistToUse)) {
                         try {
                             final File newFile = new File(listManager.listsPath, blacklistToUse);
                             newFile.createNewFile();
 
                             // share the newly created blacklist
                             listManager.updateListSet(BLACKLIST_SHARED, blacklistToUse);
 
                             // activate it for all known blacklist types
                             for (int blTypes = 0; blTypes < supportedBlacklistTypes.length; blTypes++) {
                                 listManager.updateListSet(supportedBlacklistTypes[blTypes] + ".BlackLists", blacklistToUse);
                             }                            
                         } catch (final IOException e) {/* */}
                     } else {
                         prop.put("error", 2);
                         prop.putHTML("error_name", blacklistToUse);
                         blacklistToUse = null;
                     }
                     
                     // reload Blacklists
                     dirlist = listManager.getDirListing(listManager.listsPath, BLACKLIST_FILENAME_FILTER);
                 }
                 
             } else if (post.containsKey("deleteList")) {
                 /* ===========================================================
                  * Delete a blacklist
                  * =========================================================== */                
                 
                 blacklistToUse = post.get("selectedListName");
                 if (blacklistToUse == null || blacklistToUse.trim().length() == 0) {
                     prop.put("LOCATION","");
                     return prop;
                 }                   
                 
                 final File BlackListFile = new File(listManager.listsPath, blacklistToUse);
                 if(!BlackListFile.delete()) {
                     serverLog.logWarning("Blacklist", "file "+ BlackListFile +" could not be deleted!");
                 }
 
                 for (int blTypes=0; blTypes < supportedBlacklistTypes.length; blTypes++) {
                     listManager.removeFromListSet(supportedBlacklistTypes[blTypes] + ".BlackLists",blacklistToUse);
                 }                
                 
                 // remove it from the shared list
                 listManager.removeFromListSet(BLACKLIST_SHARED, blacklistToUse);
                 blacklistToUse = null;
                 
                 // reload Blacklists
                 dirlist = listManager.getDirListing(listManager.listsPath, BLACKLIST_FILENAME_FILTER);
 
             } else if (post.containsKey("activateList")) {
 
                 /* ===========================================================
                  * Activate/Deactivate a blacklist
                  * =========================================================== */                   
                 
                 blacklistToUse = post.get("selectedListName");
                 if (blacklistToUse == null || blacklistToUse.trim().length() == 0) {
                     prop.put("LOCATION", "");
                     return prop;
                 }                   
                 
                 for (int blTypes=0; blTypes < supportedBlacklistTypes.length; blTypes++) {                    
                     if (post.containsKey("activateList4" + supportedBlacklistTypes[blTypes])) {
                         listManager.updateListSet(supportedBlacklistTypes[blTypes] + ".BlackLists",blacklistToUse);                        
                     } else {
                         listManager.removeFromListSet(supportedBlacklistTypes[blTypes] + ".BlackLists",blacklistToUse);                        
                     }                    
                 }                     
 
                 listManager.reloadBlacklists();                
                 
             } else if (post.containsKey("shareList")) {
 
                 /* ===========================================================
                  * Share a blacklist
                  * =========================================================== */                   
                 
                 blacklistToUse = post.get("selectedListName");
                 if (blacklistToUse == null || blacklistToUse.trim().length() == 0) {
                     prop.put("LOCATION", "");
                     return prop;
                 }                   
                 
                 if (listManager.listSetContains(BLACKLIST_SHARED, blacklistToUse)) { 
                     // Remove from shared BlackLists
                     listManager.removeFromListSet(BLACKLIST_SHARED, blacklistToUse);
                 } else { // inactive list -> enable
                     listManager.updateListSet(BLACKLIST_SHARED, blacklistToUse);
                 }
             } else if (action.equals("deleteBlacklistEntry")) {
                 
                 /* ===========================================================
                  * Delete an entry from a blacklist
                  * =========================================================== */
                 
                 blacklistToUse = post.get("currentBlacklist");
                 String temp = null;
                 
                 final String[] selectedBlacklistEntries = post.getAll("selectedEntry.*");
                 
                 if (selectedBlacklistEntries.length > 0) {
                     for (int i = 0; i < selectedBlacklistEntries.length; i++) {
                         temp = deleteBlacklistEntry(blacklistToUse,
                                 selectedBlacklistEntries[i], header, supportedBlacklistTypes);
                         if (temp != null) {
                             prop.put("LOCATION", temp);
                             return prop;
                         }
                     }
                 }
 
             } else if (post.containsKey("addBlacklistEntry")) {
                 
                 /* ===========================================================
                  * Add new entry to blacklist
                  * =========================================================== */
 
                 blacklistToUse = post.get("currentBlacklist");
                 
                 final String temp = addBlacklistEntry(post.get("currentBlacklist"),
                         post.get("newEntry"), header, supportedBlacklistTypes);
                 if (temp != null) {
                     prop.put("LOCATION", temp);
                     return prop;
                 }
                 
             } else if (action.equals("moveBlacklistEntry")) {
                 
                 /* ===========================================================
                  * Move an entry from one blacklist to another
                  * =========================================================== */
                 
                 blacklistToUse = post.get("currentBlacklist");
                 String targetBlacklist = post.get("targetBlacklist");
                 String temp = null;
                 
                 final String[] selectedBlacklistEntries = post.getAll("selectedEntry.*");
                 
                if (selectedBlacklistEntries.length > 0 && !targetBlacklist.equals(blacklistToUse)) {
                     for (int i = 0; i < selectedBlacklistEntries.length; i++) {
 
                         temp = addBlacklistEntry(targetBlacklist,
                                 selectedBlacklistEntries[i], header, supportedBlacklistTypes);
                         if (temp != null) {
                             prop.put("LOCATION", temp);
                             return prop;
                         }
 
                         temp = deleteBlacklistEntry(blacklistToUse,
                                 selectedBlacklistEntries[i], header, supportedBlacklistTypes);
                         if (temp != null) {
                             prop.put("LOCATION", temp);
                             return prop;
                             
                         }
                     }
                 }
 
             } else if (action.equals("editBlacklistEntry")) {
                 
                 /* ===========================================================
                  * Edit entry of a blacklist
                  * =========================================================== */
                 
                 blacklistToUse = post.get("currentBlacklist");
                 
                 final String[] editedBlacklistEntries = post.getAll("editedBlacklistEntry.*");
         
                 // if edited entry has been posted, save changes
                 if (editedBlacklistEntries.length > 0) {
       
                     final String[] selectedBlacklistEntries = post.getAll("selectedBlacklistEntry.*");
                     
                     if (selectedBlacklistEntries.length != editedBlacklistEntries.length) {
                         prop.put("LOCATION", "");
                         return prop;
                     }
                     
                     String temp = null;
 
                     for (int i = 0; i < selectedBlacklistEntries.length; i++) {
 
                         if (!selectedBlacklistEntries[i].equals(editedBlacklistEntries[i])) {
                             temp = deleteBlacklistEntry(blacklistToUse, selectedBlacklistEntries[i], header, supportedBlacklistTypes);
                             if (temp != null) {
                                 prop.put("LOCATION", temp);
                                 return prop;
                             }
 
                             temp = addBlacklistEntry(blacklistToUse, editedBlacklistEntries[i], header, supportedBlacklistTypes);
                             if (temp != null) {
                                 prop.put("LOCATION", temp);
                                 return prop;
                             }
                         }
                     }
                     
                     prop.putHTML(DISABLED + EDIT + "currentBlacklist", blacklistToUse);
                     
                 // else return entry to be edited
                 } else {
                     final String[] selectedEntries = post.getAll("selectedEntry.*");
                    if (selectedEntries != null && blacklistToUse != null) {
                         for (int i = 0; i < selectedEntries.length; i++) {
                             prop.putHTML(DISABLED + EDIT + "editList_" + i + "_item", selectedEntries[i]);
                             prop.put(DISABLED + EDIT + "editList_" + i + "_count", i);
                         }
                         prop.putHTML(DISABLED + EDIT + "currentBlacklist", blacklistToUse);
                         prop.put(DISABLED + "edit", "1");   
                         prop.put(DISABLED + EDIT + "editList", selectedEntries.length);
                     }
                 }
             }
 
         }
 
         // if we have not chosen a blacklist until yet we use the first file
         if (blacklistToUse == null && dirlist != null && dirlist.size() > 0) {
             blacklistToUse = dirlist.get(0);
         }
 
         // Read the blacklist items from file
         if (blacklistToUse != null) {
             int entryCount = 0;
             final List<String> list = listManager.getListArray(new File(listManager.listsPath, blacklistToUse));
             
             // sort them
             final String[] sortedlist = new String[list.size()];
             Arrays.sort(list.toArray(sortedlist));
             
             // display them
             boolean dark = true;
             for (int j=0;j<sortedlist.length;++j){
                 final String nextEntry = sortedlist[j];
                 
                 if (nextEntry.length() == 0) continue;
                 if (nextEntry.startsWith("#")) continue;
                 prop.put(DISABLED + EDIT + "Itemlist_" + entryCount + "_dark", dark ? "1" : "0");
                 dark = !dark;
                 prop.putHTML(DISABLED + EDIT + "Itemlist_" + entryCount + "_item", nextEntry);
                 prop.put(DISABLED + EDIT + "Itemlist_" + entryCount + "_count", entryCount);
                 entryCount++;
             }
             prop.put(DISABLED + EDIT + "Itemlist", entryCount);
         }
         
         // List BlackLists
         int blacklistCount = 0;
         int blacklistMoveCount = 0;
         if (dirlist != null) {
 
             for (String element : dirlist) {
                 prop.putXML(DISABLED + BLACKLIST + blacklistCount + "_name", element);
                 prop.put(DISABLED + BLACKLIST + blacklistCount + "_selected", "0");
 
                 if (element.equals(blacklistToUse)) { //current List
                     prop.put(DISABLED + BLACKLIST + blacklistCount + "_selected", "1");
 
                     for (int blTypes=0; blTypes < supportedBlacklistTypes.length; blTypes++) {
                         prop.putXML(DISABLED + "currentActiveFor_" + blTypes + "_blTypeName",supportedBlacklistTypes[blTypes]);
                         prop.put(DISABLED + "currentActiveFor_" + blTypes + "_checked",
                                 listManager.listSetContains(supportedBlacklistTypes[blTypes] + ".BlackLists", element) ? "0" : "1");
                     }
                     prop.put(DISABLED + "currentActiveFor", supportedBlacklistTypes.length);
 
                 } else {
                     prop.putXML(DISABLED + EDIT + BLACKLIST_MOVE + blacklistMoveCount + "_name", element);
                     blacklistMoveCount++;
                 }
                 
                 if (listManager.listSetContains(BLACKLIST_SHARED, element)) {
                     prop.put(DISABLED + BLACKLIST + blacklistCount + "_shared", "1");
                 } else {
                     prop.put(DISABLED + BLACKLIST + blacklistCount + "_shared", "0");
                 }
 
                 int activeCount = 0;
                 for (int blTypes=0; blTypes < supportedBlacklistTypes.length; blTypes++) {
                     if (listManager.listSetContains(supportedBlacklistTypes[blTypes] + ".BlackLists", element)) {
                         prop.putHTML(DISABLED + BLACKLIST + blacklistCount + "_active_" + activeCount + "_blTypeName", supportedBlacklistTypes[blTypes]);
                         activeCount++;
                     }                
                 }          
                 prop.put(DISABLED + BLACKLIST + blacklistCount + "_active", activeCount);
                 blacklistCount++;
             }
         }
         prop.put(DISABLED + "blackLists", blacklistCount);
         prop.put(DISABLED + EDIT + "blackListsMove", blacklistMoveCount);
         
         prop.putXML(DISABLED + "currentBlacklist", (blacklistToUse==null) ? "" : blacklistToUse);
         prop.putXML(DISABLED + EDIT + "currentBlacklist", (blacklistToUse==null) ? "" : blacklistToUse);
         prop.put("disabled", (blacklistToUse == null) ? "1" : "0");
         return prop;
     }
     
     /**
      * This method adds a new entry to the chosen blacklist.
      * @param blacklistToUse the name of the blacklist the entry is to be added to
      * @param newEntry the entry that is to be added
      * @param header
      * @param supportedBlacklistTypes
      * @return null if no error occured, else a String to put into LOCATION
      */
     private static String addBlacklistEntry(final String blacklistToUse, String newEntry, 
             final httpRequestHeader header, final String[] supportedBlacklistTypes) {
 
         if (blacklistToUse == null || blacklistToUse.trim().length() == 0) {
             return "";
         }
 
         if (newEntry == null || newEntry.trim().length() == 0) {
             return header.get("PATH") + "?selectList=&selectedListName=" + blacklistToUse;
         }
 
         // TODO: ignore empty entries
 
         if (newEntry.startsWith("http://") ){
             newEntry = newEntry.substring(7);
         }
 
         int pos = newEntry.indexOf("/");
         if (pos < 0) {
             // add default empty path pattern
             pos = newEntry.length();
             newEntry = newEntry + "/.*";
         }
 
         // append the line to the file
         PrintWriter pw = null;
         try {
             pw = new PrintWriter(new FileWriter(new File(listManager.listsPath, blacklistToUse), true));
             pw.println(newEntry);
             pw.close();
         } catch (final IOException e) {
             e.printStackTrace();
         } finally {
             if (pw != null) try { pw.close(); } catch (final Exception e){ serverLog.logWarning("Blacklist", "could not close stream to "+ blacklistToUse +"! "+ e.getMessage());}
         }
 
         // add to blacklist
         for (int blTypes=0; blTypes < supportedBlacklistTypes.length; blTypes++) {
             if (listManager.listSetContains(supportedBlacklistTypes[blTypes] + ".BlackLists",blacklistToUse)) {
                 plasmaSwitchboard.urlBlacklist.add(supportedBlacklistTypes[blTypes],newEntry.substring(0, pos), newEntry.substring(pos + 1));
             }
         }
 
         return null;
     }
     
     /**
      * This method deletes a blacklist entry.
      * @param blacklistToUse the name of the blacklist the entry is to be deleted from
      * @param oldEntry the entry that is to be deleted
      * @param header
      * @param supportedBlacklistTypes
      * @return null if no error occured, else a String to put into LOCATION
      */
     private static String deleteBlacklistEntry(final String blacklistToUse, String oldEntry, 
             final httpRequestHeader header, final String[] supportedBlacklistTypes) {
 
         if (blacklistToUse == null || blacklistToUse.trim().length() == 0) {
             return "";
         }
 
         if (oldEntry == null || oldEntry.trim().length() == 0) {
             return header.get("PATH") + "?selectList=&selectedListName=" + blacklistToUse;
         }
 
         // load blacklist data from file
         final ArrayList<String> list = listManager.getListArray(new File(listManager.listsPath, blacklistToUse));
 
         // delete the old entry from file
         if (list != null) {
             for (int i=0; i < list.size(); i++) {
                 if ((list.get(i)).equals(oldEntry)) {
                     list.remove(i);
                     break;
                 }
             }
             listManager.writeList(new File(listManager.listsPath, blacklistToUse), list.toArray(new String[list.size()]));
         }
 
         // remove the entry from the running blacklist engine
         int pos = oldEntry.indexOf("/");
         if (pos < 0) {
             // add default empty path pattern
             pos = oldEntry.length();
             oldEntry = oldEntry + "/.*";
         }
         for (int blTypes=0; blTypes < supportedBlacklistTypes.length; blTypes++) {
             if (listManager.listSetContains(supportedBlacklistTypes[blTypes] + ".BlackLists",blacklistToUse)) {
                 plasmaSwitchboard.urlBlacklist.remove(supportedBlacklistTypes[blTypes],oldEntry.substring(0, pos), oldEntry.substring(pos + 1));
             }
         }
         
         return null;
     }
 
 }
