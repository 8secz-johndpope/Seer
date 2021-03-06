 /*
 Copyright (C) 2003 Morten O. Alver and Nizar N. Batada
 
 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.
 
 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA
 
 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
 
 */
 
 package net.sf.jabref;
 
 import java.awt.*;
 import java.awt.datatransfer.*;
 import java.awt.event.*;
 import java.io.*;
 import java.util.*;
 import javax.swing.*;
 import javax.swing.event.ListSelectionListener;
 import javax.swing.undo.*;
 import net.sf.jabref.collab.*;
 import net.sf.jabref.export.*;
 import net.sf.jabref.groups.*;
 import net.sf.jabref.external.*;
 import net.sf.jabref.imports.*;
 import net.sf.jabref.labelPattern.LabelPatternUtil;
 import net.sf.jabref.undo.*;
 import net.sf.jabref.wizard.text.gui.TextInputDialog;
 
 public class BasePanel extends /*JSplitPane*/JPanel implements ClipboardOwner, FileUpdateListener {
 
   boolean tmp = true;
 
     BasePanel ths = this;
     JSplitPane splitPane;
     //BibtexEntry testE = new BibtexEntry("tt");
     PreviewPanel[] previewPanel = new PreviewPanel[]
         {new PreviewPanel(Globals.prefs.get("preview0")),
         new PreviewPanel(Globals.prefs.get("preview1"))};
     int activePreview = 1;
     boolean previewActive = true;
 
     JabRefFrame frame;
     BibtexDatabase database;
     JabRefPreferences prefs;
     // The database shown in this panel.
     File file = null,
         fileToOpen = null; // The filename of the database.
     String fileMonitorHandle = null;
     boolean saving = false, updatedExternally = false;
     private String encoding = null;
 
     GridBagLayout gbl = new GridBagLayout();
     GridBagConstraints con = new GridBagConstraints();
 
     //Hashtable autoCompleters = new Hashtable();
     // Hashtable that holds as keys the names of the fields where
     // autocomplete is active, and references to the autocompleter objects.
 
     // The undo manager.
     public CountingUndoManager undoManager = new CountingUndoManager(ths);
     UndoAction undoAction = new UndoAction();
     RedoAction redoAction = new RedoAction();
 
     //ExampleFileFilter fileFilter;
     // File filter for .bib files.
 
     boolean baseChanged = false, nonUndoableChange = false;
     // Used to track whether the base has changed since last save.
 
     EntryTableModel tableModel = null;
     EntryTable entryTable = null;
 
     // The sidepane manager takes care of populating the sidepane.
     public SidePaneManager sidePaneManager;
 
     SearchManager2 searchManager;
     MedlineFetcher medlineFetcher;
     CiteSeerFetcher citeSeerFetcher;
     RightClickMenu rcm;
 
     BibtexEntry showing = null;
     // To indicate which entry is currently shown.
     HashMap entryEditors = new HashMap();
     // To contain instantiated entry editors. This is to save time
     // in switching between entries.
 
     //HashMap entryTypeForms = new HashMap();
     // Hashmap to keep track of which entries currently have open
     // EntryTypeForm dialogs.
 
     PreambleEditor preambleEditor = null;
     // Keeps track of the preamble dialog if it is open.
 
     StringDialog stringDialog = null;
     // Keeps track of the string dialog if it is open.
 
     /**
      * The group selector component for this database. Instantiated by the
      * SidePaneManager if necessary, or from this class if merging groups from a
      * different database.
      */
     GroupSelector groupSelector;
 
     boolean sortingBySearchResults = false,
         coloringBySearchResults = false,
         sortingByGroup = false,
         sortingByCiteSeerResults = false,
         coloringByGroup = false,
         previewEnabled = true;
 
     // MetaData parses, keeps and writes meta data.
     MetaData metaData;
     HashMap fieldExtras = new HashMap();
     //## keep track of all keys for duplicate key warning and unique key generation
     //private HashMap allKeys  = new HashMap();	// use a map instead of a set since i need to know how many of each key is inthere
 
     private boolean suppressOutput = false;
 
     private HashMap actions = new HashMap();
 
     public BasePanel(JabRefFrame frame, JabRefPreferences prefs) {
 	//super(JSplitPane.HORIZONTAL_SPLIT, true);
       database = new BibtexDatabase();
       metaData = new MetaData();
       this.frame = frame;
       this.prefs = prefs;
       setupActions();
       setupMainPanel();
     }
 
     public BasePanel(JabRefFrame frame, BibtexDatabase db, File file,
                      HashMap meta, JabRefPreferences prefs) {
 	//super(JSplitPane.HORIZONTAL_SPLIT, true);
       this.frame = frame;
       database = db;
       this.prefs = prefs;
       if (meta != null)
         parseMetaData(meta);
       else
         metaData = new MetaData();
       setupActions();
       setupMainPanel();
       /*if (prefs.getBoolean("autoComplete")) {
             db.setCompleters(autoCompleters);
             }*/
 
       this.file = file;
 
       // Register so we get notifications about outside changes to the file.
       if (file != null)
         try {
           fileMonitorHandle = Globals.fileUpdateMonitor.addUpdateListener(this,
               file);
         } catch (IOException ex) {
         }
     }
 
     public BibtexDatabase database() { return database; }
     public MetaData metaData() { return metaData; }
     public File file() { return file; }
     public JabRefFrame frame() { return frame; }
     public JabRefPreferences prefs() { return prefs; }
 
     public String getEncoding() { return encoding; }
     public void setEncoding(String encoding) { 
 	this.encoding = encoding;
     }
 
     public BibtexEntry[] getSelectedEntries() {
 	return entryTable.getSelectedEntries();
     }
 
     public void output(String s) {
 	//Util.pr("\""+s+"\""+(SwingUtilities.isEventDispatchThread()));
         if (!suppressOutput)
             frame.output(s);
     }
 
     private void setupActions() {
 
         actions.put("undo", undoAction);
         actions.put("redo", redoAction);
 
         // The action for opening an entry editor.
         actions.put("edit", new BaseAction() {
                 public void action() {
                   //(new Thread() {
                   //public void run() {
                   int clickedOn = -1;
                   // We demand that one and only one row is selected.
                   if (entryTable.getSelectedRowCount() == 1) {
                     clickedOn = entryTable.getSelectedRow();
                   }
                   if (clickedOn >= 0) {
                     String id = tableModel.getNameFromNumber(clickedOn);
                     BibtexEntry be = database.getEntryById(id);
                     showEntry(be);
                     
                     if (splitPane.getBottomComponent() != null)
                       new FocusRequester(splitPane.getBottomComponent());
                   }
                 }
 
             });
 
         // The action for saving a database.
         actions.put("save", new BaseAction() {
                 public void action() throws Throwable {
                     
                     if (file == null)
                         runCommand("saveAs");
                     else {
                         
                       if (updatedExternally || Globals.fileUpdateMonitor.hasBeenModified(fileMonitorHandle)) {
                           String[] opts = new String[] {Globals.lang("Review changes"), Globals.lang("Save"),
                             Globals.lang("Cancel") };
                           int answer = JOptionPane.showOptionDialog(frame, Globals.lang("File has been updated externally. "
                           +"What do you want to do?"), Globals.lang("File updated externally"),
                           JOptionPane.YES_NO_CANCEL_OPTION,  JOptionPane.QUESTION_MESSAGE,
                           null, opts, opts[0]);
                          /*  int choice = JOptionPane.showConfirmDialog(frame, Globals.lang("File has been updated externally. "
                             +"Are you sure you want to save?"), Globals.lang("File updated externally"),
                                                                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);*/
                         if (answer == JOptionPane.CANCEL_OPTION)
                             return;
                         else if (answer == JOptionPane.YES_OPTION) {
                             ChangeScanner scanner = new ChangeScanner(frame, ths); //, panel.database(), panel.metaData());
                             try {
                                 scanner.changeScan(file());
                                 setUpdatedExternally(false);
                                 SwingUtilities.invokeLater(new Runnable() {
                                     public void run() {
                                         sidePaneManager.hideAway("fileUpdate");
                                     }
                                 });
 
                             } catch (IOException ex) {
                                 ex.printStackTrace();
                             }
                             
                             return;
                         }
                       }
 
                       saving = true;
                       saveDatabase(file, false);
 
                       //Util.pr("Testing resolve string... BasePanel line 237");
                       //Util.pr("Resolve aq: "+database.resolveString("aq"));
                       //Util.pr("Resolve text: "+database.resolveForStrings("A text which refers to the string #aq# and #billball#, hurra."));
     
                       try {
                         Globals.fileUpdateMonitor.updateTimeStamp(fileMonitorHandle);
                       } catch (IllegalArgumentException ex) {
                         // This means the file has not yet been registered, which is the case
                         // when doing a "Save as". Maybe we should change the monitor so no
                         // exception is cast.
                       }
                       saving = false;
                       undoManager.markUnchanged();
                       // (Only) after a successful save the following
                       // statement marks that the base is unchanged
                       // since last save:
                       nonUndoableChange = false;
                       baseChanged = false;
                       updatedExternally = false;
                       frame.setTabTitle(ths, file.getName());
                       frame.output(Globals.lang("Saved database")+" '"
                                    +file.getPath()+"'.");
                     }
                 }
             });
 
         actions.put("saveAs", new BaseAction () {
                 public void action() throws Throwable {
 
                   String chosenFile = Globals.getNewFile(frame, prefs, new File(prefs.get("workingDirectory")), ".bib",
                                                          JFileChooser.SAVE_DIALOG, false);
 
                   if (chosenFile != null) {
                     file = new File(chosenFile);
                     if (!file.exists() ||
                         (JOptionPane.showConfirmDialog
                          (frame, "'"+file.getName()+"' "+Globals.lang("exists. Overwrite file?"),
                           Globals.lang("Save database"), JOptionPane.OK_CANCEL_OPTION)
                          == JOptionPane.OK_OPTION)) {
 
                       runCommand("save");
                       
                       // Register so we get notifications about outside changes to the file.
                       try {
                         fileMonitorHandle = Globals.fileUpdateMonitor.addUpdateListener(ths,file);
                       } catch (IOException ex) {
                         ex.printStackTrace();
                       }
 
                       prefs.put("workingDirectory", file.getParent());
                       frame.getFileHistory().newFile(file.getPath());
                     }
                     else
                       file = null;
                     }
                 }
             });
 
         actions.put("saveSelectedAs", new BaseAction () {
                 public void action() throws Throwable {
 
                   String chosenFile = Globals.getNewFile(frame, prefs, new File(prefs.get("workingDirectory")), ".bib",
                                                          JFileChooser.SAVE_DIALOG, false);
                   if (chosenFile != null) {
                     File expFile = new File(chosenFile);
                     if (!expFile.exists() ||
                         (JOptionPane.showConfirmDialog
                          (frame, "'"+expFile.getName()+"' "+
                           Globals.lang("exists. Overwrite file?"),
                           Globals.lang("Save database"), JOptionPane.OK_CANCEL_OPTION)
                          == JOptionPane.OK_OPTION)) {
                       saveDatabase(expFile, true);
                       //runCommand("save");
                       frame.getFileHistory().newFile(expFile.getPath());
                       frame.output(Globals.lang("Saved selected to")+" '"
                                    +expFile.getPath()+"'.");
                         }
                     }
                 }
             });
 
         // The action for copying selected entries.
         actions.put("copy", new BaseAction() {
                 public void action() {
                     BibtexEntry[] bes = entryTable.getSelectedEntries();
 
                     if ((bes != null) && (bes.length > 0)) {
                         TransferableBibtexEntry trbe
                             = new TransferableBibtexEntry(bes);
                         // ! look at ClipBoardManager
                         Toolkit.getDefaultToolkit().getSystemClipboard()
                             .setContents(trbe, ths);
                         output(Globals.lang("Copied")+" "+(bes.length>1 ? bes.length+" "
                                                            +Globals.lang("entries")
                                                            : "1 "+Globals.lang("entry")+"."));
                     } else {
                         // The user maybe selected a single cell.
                         int[] rows = entryTable.getSelectedRows(),
                             cols = entryTable.getSelectedColumns();
                         if ((cols.length == 1) && (rows.length == 1)) {
                             // Copy single value.
                             Object o = tableModel.getValueAt(rows[0], cols[0]);
                             if (o != null) {
                                 StringSelection ss = new StringSelection(o.toString());
                                 Toolkit.getDefaultToolkit().getSystemClipboard()
                                     .setContents(ss, ths);
 
                                 output(Globals.lang("Copied cell contents")+".");
                             }
                         }
                     }
                 }
             });
 
         actions.put("cut", new BaseAction() {
                 public void action() throws Throwable {
                     runCommand("copy");
                     BibtexEntry[] bes = entryTable.getSelectedEntries();
                     if ((bes != null) && (bes.length > 0)) {
                         // Create a CompoundEdit to make the action undoable.
                         NamedCompound ce = new NamedCompound
                             (bes.length > 1 ? "cut entries" : "cut entry");
                         // Loop through the array of entries, and delete them.
                         for (int i=0; i<bes.length; i++) {
                             database.removeEntry(bes[i].getId());
                             ensureNotShowing(bes[i]);
                             ce.addEdit(new UndoableRemoveEntry
                                        (database, bes[i], ths));
                         }
                         entryTable.clearSelection();
                         frame.output(Globals.lang("Cut_pr")+" "+
                                      (bes.length>1 ? bes.length
                                       +" "+ Globals.lang("entries")
                                       : Globals.lang("entry"))+".");
                         ce.end();
                         undoManager.addEdit(ce);
                         refreshTable();
                         markBaseChanged();
                     }
                 }
             });
 
         actions.put("delete", new BaseAction() {
                 public void action() {
                   boolean cancelled = false;
                   BibtexEntry[] bes = entryTable.getSelectedEntries();
                   int row0 = entryTable.getSelectedRow();
                   if ((bes != null) && (bes.length > 0)) {
                     //&& (database.getEntryCount() > 0) && (entryTable.getSelectedRow() < database.getEntryCount())) {
                       boolean goOn = showDeleteConfirmationDialog(bes.length);
                       if (!goOn) {
                           // This is a hack to avoid the action being called twice,
                           // feel free to fix it...
                           entryTable.clearSelection();
                       }
                       else {
                           // Create a CompoundEdit to make the action undoable.
                           NamedCompound ce = new NamedCompound
                               (bes.length > 1 ? Globals.lang("delete entries")
                                : Globals.lang("delete entry"));
                           // Loop through the array of entries, and delete them.
                           for (int i = 0; i < bes.length; i++) {
                               database.removeEntry(bes[i].getId());
                               ce.addEdit(new UndoableRemoveEntry(database, bes[i], ths));
                           }
                           frame.output(Globals.lang("Deleted") + " " +
                                        (bes.length > 1 ? bes.length
                                         + " " + Globals.lang("entries")
                                         : Globals.lang("entry")) + ".");
                           ce.end();
                           undoManager.addEdit(ce);
                           //entryTable.clearSelection();
                           refreshTable();
                           markBaseChanged();
 
                       }
                   }
                 }
 
 
 
             });
 
         // The action for pasting entries or cell contents.
         // Edited by Seb Wills <saw27@mrao.cam.ac.uk> on 14-Apr-04:
         //  - more robust detection of available content flavors (doesn't only look at first one offered)
         //  - support for parsing string-flavor clipboard contents which are bibtex entries.
         //    This allows you to (a) paste entire bibtex entries from a text editor, web browser, etc
         //                       (b) copy and paste entries between multiple instances of JabRef (since
         //         only the text representation seems to get as far as the X clipboard, at least on my system)
         actions.put("paste", new BaseAction() {
                 public void action() {
                     // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
                     Transferable content = Toolkit.getDefaultToolkit()
                         .getSystemClipboard().getContents(null);
                     if (content != null) {
                         BibtexEntry[] bes = null;
                         if (content.isDataFlavorSupported(TransferableBibtexEntry.entryFlavor)) {
                             // We have determined that the clipboard data is a set of entries.
                             try {
                                 bes = (BibtexEntry[])(content.getTransferData(TransferableBibtexEntry.entryFlavor));
                             } catch (UnsupportedFlavorException ex) {
                                 ex.printStackTrace();
                             } catch (IOException ex) {
                                 ex.printStackTrace();
                             }
                         } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                             // We have determined that no TransferableBibtexEntry is available, but
                             // there is a string, which we will handle according to context:
                             int[] rows = entryTable.getSelectedRows(),
                                 cols = entryTable.getSelectedColumns();
                             Util.pr(rows.length+" x "+cols.length);
                             if ((cols != null) && (cols.length == 1) && (cols[0] != 0)
                                 && (rows != null) && (rows.length == 1)) {
                                 // A single cell is highlighted, so paste the string straight into it without parsing
                                 try {
                                     tableModel.setValueAt((String)(content.getTransferData(DataFlavor.stringFlavor)), rows[0], cols[0]);
                                     refreshTable();
                                     markBaseChanged();
                                     output("Pasted cell contents");
                                 } catch (UnsupportedFlavorException ex) {
                                     ex.printStackTrace();
                                 } catch (IOException ex) {
                                     ex.printStackTrace();
                                 } catch (IllegalArgumentException ex) {
                                     output("Can't paste.");
                                 }
                             } else {
                               // no single cell is selected, so try parsing the clipboard contents as bibtex entries instead
                               try {
                                   BibtexParser bp = new BibtexParser
                                       (new java.io.StringReader( (String) (content.getTransferData(
                                       DataFlavor.stringFlavor))));
                                   BibtexDatabase db = bp.parse().getDatabase();
                                   Util.pr("Parsed " + db.getEntryCount() + " entries from clipboard text");
                                   if(db.getEntryCount()>0) {
                                       Set keySet = db.getKeySet();
                                       if (keySet != null) {
                                           // Copy references to the entries into a BibtexEntry array.
                                           // Could import directly from db, but going via bes allows re-use
                                           // of the same pasting code as used for TransferableBibtexEntries
                                           bes = new BibtexEntry[db.getEntryCount()];
                                           Iterator it = keySet.iterator();
                                           for (int i=0; it.hasNext();i++) {
                                               bes[i]=db.getEntryById((String) (it.next()));
                                           }
                                       }
                                   } /*else {
                                     String cont = (String)(content.getTransferData(DataFlavor.stringFlavor));
                                     Util.pr("----------------\n"+cont+"\n---------------------");
                                     TextAnalyzer ta = new TextAnalyzer(cont);
                                       output(Globals.lang("Unable to parse clipboard text as Bibtex entries."));
                                       }*/
                               } catch (UnsupportedFlavorException ex) {
                                   ex.printStackTrace();
                               } catch (Throwable ex) {
                                   ex.printStackTrace();
                               }
                             }
                         }
 
                         // finally we paste in the entries (if any), which either came from TransferableBibtexEntries
                         // or were parsed from a string
                         if ((bes != null) && (bes.length > 0)) {
                           NamedCompound ce = new NamedCompound
                               (bes.length > 1 ? "paste entries" : "paste entry");
                           for (int i=0; i<bes.length; i++) {
                             try {
                               BibtexEntry be = (BibtexEntry)(bes[i].clone());
                               // We have to clone the
                               // entries, since the pasted
                               // entries must exist
                               // independently of the copied
                               // ones.
                               be.setId(Util.createId(be.getType(), database));
                               database.insertEntry(be);
                               ce.addEdit(new UndoableInsertEntry
                                          (database, be, ths));
                             } catch (KeyCollisionException ex) {
                               Util.pr("KeyCollisionException... this shouldn't happen.");
                             }
                           }
                           ce.end();
                           undoManager.addEdit(ce);
                           tableModel.remap();
                           entryTable.clearSelection();
                           entryTable.revalidate();
                           output(Globals.lang("Pasted")+" "+
                                  (bes.length>1 ? bes.length+" "+
                                   Globals.lang("entries") : "1 "+Globals.lang("entry"))
                                  +".");
                           refreshTable();
                           markBaseChanged();
                         }
                       }
                       /*Util.pr(flavor.length+"");
                           Util.pr(flavor[0].toString());
                           Util.pr(flavor[1].toString());
                           Util.pr(flavor[2].toString());
                           Util.pr(flavor[3].toString());
                           Util.pr(flavor[4].toString());
                        */
 
                     }
 
 });
 
         actions.put("selectAll", new BaseAction() {
                 public void action() {
                     entryTable.selectAll();
                 }
             });
 
         // The action for opening the preamble editor
         actions.put("editPreamble", new BaseAction() {
                 public void action() {
                     if (preambleEditor == null) {
                         PreambleEditor form = new PreambleEditor
                             (frame, ths, database, prefs);
                         Util.placeDialog(form, frame);
                         form.setVisible(true);
                         preambleEditor = form;
                     } else {
                         preambleEditor.setVisible(true);
                     }
 
                 }
             });
 
         // The action for opening the string editor
         actions.put("editStrings", new BaseAction() {
                 public void action() {
                     if (stringDialog == null) {
                         StringDialog form = new StringDialog
                             (frame, ths, database, prefs);
                         Util.placeDialog(form, frame);
                         form.setVisible(true);
                         stringDialog = form;
                     } else {
                         stringDialog.setVisible(true);
                     }
 
                 }
             });
 
         // The action for toggling the groups interface
         actions.put("toggleGroups", new BaseAction() {
             public void action() {
               sidePaneManager.togglePanel("groups");
               frame.groupToggle.setSelected(sidePaneManager.isPanelVisible("groups"));
             }
         });
 
 	// The action for pushing citations to an open Lyx/Kile instance:
         actions.put("pushToLyX", new PushToLyx(ths));
 
             actions.put("pushToWinEdt",new BaseAction(){
               public void action(){
                     final int[] rows = entryTable.getSelectedRows();
                     final int numSelected = rows.length;
                     // Globals.logger("Pushing " +numSelected+(numSelected>1? " entries" : "entry") + " to WinEdt");
 
                     //Util.pr("tre");
 
                     if( numSelected > 0){
                       Thread pushThread = new Thread() {
                         public void run() {
                           String winEdt = prefs.get("winEdtPath");
                           //winEdt = "osascript";
                           try {
                             StringBuffer toSend = new StringBuffer("\"[InsText('\\cite{");
                             //StringBuffer toSend = new StringBuffer
                             //    ("-e 'tell application \"iTeXMac\" to insert \"\\\\cite{");
                             //if (tmp)
                             //  toSend = new StringBuffer
                             //    ("-e 'tell application \"TeXShop\" to set the selection of the front document to \"\\\\cite{");
                             String citeKey = "", message = "";
                             boolean first = true;
                             for (int i = 0; i < numSelected; i++) {
                               BibtexEntry bes = database.getEntryById(tableModel.
                                   getNameFromNumber(rows[
                                                     i]));
                               citeKey = (String) bes.getField(GUIGlobals.KEY_FIELD);
                               // if the key is empty we give a warning and ignore this entry
                               if (citeKey == null || citeKey.equals(""))
                                 continue;
                               if (first) {
                                 toSend.append(citeKey);
                                 first = false;
                               }
                               else
                                 toSend.append("," + citeKey);
 
                               if (i > 0)
                                 message += ", ";
                               message += (1 + rows[i]);
 
                             }
                             if (first)
                               output(Globals.lang("Please define BibTeX key first"));
                             else {
                               toSend.append("}');]\"");
                               //if (!tmp)
                               //  toSend.append("}\" in the text of the front document'");
                               //else
                               //  toSend.append("}\"'");
 
                               //tmp = !tmp;
 
                               System.out.println("Running command: "+winEdt + " " + toSend.toString());
                               Runtime.getRuntime().exec(winEdt + " " + toSend.toString());
                               output(
                                   Globals.lang("Pushed the citations for the following rows to")+"WinEdt: " +
                                   message);
                             }
                           }
 
                           catch (IOException excep) {
                             output(Globals.lang("Error")+": "+Globals.lang("Could not call executable")+" '"
                                    +winEdt+"'.");
                             excep.printStackTrace();
                           }
                         }
                       };
 
                       pushThread.start();
                     }
                   }
             });
 
         // The action for auto-generating keys.
         actions.put("makeKey", new AbstractWorker() {
 		int[] rows;
 		int numSelected;
 
 		// Run first, in EDT:
 		public void init() {
 
                     rows = entryTable.getSelectedRows() ;
                     numSelected = rows.length ;
 
                     if (numSelected == 0) { // None selected. Inform the user to select entries first.
                         JOptionPane.showMessageDialog(frame, Globals.lang("First select the entries you want keys to be generated for."),
                                                       Globals.lang("Autogenerate BibTeX key"), JOptionPane.INFORMATION_MESSAGE);
                         return ;
                     }
 		    frame.block();
 		    output(Globals.lang("Generating BibTeX key for")+" "+
                            numSelected+" "+(numSelected>1 ? Globals.lang("entries")
                                             : Globals.lang("entry"))+"...");
 		}
 
 		// Run second, on a different thread:
                 public void run() {
                     BibtexEntry bes = null ;
                     NamedCompound ce = new NamedCompound("autogenerate keys");
                     //BibtexEntry be;
                     Object oldValue;
                     for(int i = 0 ; i < numSelected ; i++){
                         bes = database.getEntryById(tableModel.getNameFromNumber(rows[i]));
                         oldValue = bes.getField(GUIGlobals.KEY_FIELD);
                         database.setCiteKeyForEntry(bes.getId(), null);
                         ce.addEdit(new UndoableKeyChange
                                    (database, bes.getId(), (String)oldValue, null));
                     }
                     
                     for(int i = 0 ; i < numSelected ; i++){
                         bes = database.getEntryById(tableModel.getNameFromNumber(rows[i]));
                         oldValue = bes.getField(GUIGlobals.KEY_FIELD);
                         //bes = frame.labelMaker.applyRule(bes, database) ;
                         bes = LabelPatternUtil.makeLabel(prefs.getKeyPattern(), database, bes);
                         ce.addEdit(new UndoableKeyChange
                                    (database, bes.getId(), (String)oldValue,
                                     (String)bes.getField(GUIGlobals.KEY_FIELD)));
                     }
                     ce.end();
                     undoManager.addEdit(ce);
 		}
 
 		// Run third, on EDT:
 		public void update() {
                     markBaseChanged() ;
                     refreshTable() ;
                     output(Globals.lang("Generated BibTeX key for")+" "+
                            numSelected+" "+(numSelected>1 ? Globals.lang("entries")
                                             : Globals.lang("entry")));
 		    frame.unblock();
                 }
             });
 
         actions.put("search", new BaseAction() {
                 public void action() {
                     //sidePaneManager.togglePanel("search");
                     sidePaneManager.ensureVisible("search");
                     //boolean on = sidePaneManager.isPanelVisible("search");
                     frame.searchToggle.setSelected(true);
                     if (true)
                       searchManager.startSearch();
                 }
             });
 
         actions.put("toggleSearch", new BaseAction() {
                 public void action() {
                     //sidePaneManager.togglePanel("search");
                     sidePaneManager.togglePanel("search");
                     boolean on = sidePaneManager.isPanelVisible("search");
                     frame.searchToggle.setSelected(on);
                     if (on)
                       searchManager.startSearch();
                 }
             });
 
         actions.put("incSearch", new BaseAction() {
                 public void action() {
                     sidePaneManager.ensureVisible("search");
                     frame.searchToggle.setSelected(true);
                     searchManager.startIncrementalSearch();
                 }
             });
 
         // The action for copying the selected entry's key.
         actions.put("copyKey", new BaseAction() {
                 public void action() {
                     BibtexEntry[] bes = entryTable.getSelectedEntries();
                     if ((bes != null) && (bes.length > 0)) {
                         //String[] keys = new String[bes.length];
                         Vector keys = new Vector();
                         // Collect all non-null keys.
                         for (int i=0; i<bes.length; i++)
                             if (bes[i].getField(Globals.KEY_FIELD) != null)
                                 keys.add(bes[i].getField(Globals.KEY_FIELD));
                         if (keys.size() == 0) {
                             output("None of the selected entries have BibTeX keys.");
                             return;
                         }
                         StringBuffer sb = new StringBuffer((String)keys.elementAt(0));
                         for (int i=1; i<keys.size(); i++) {
                             sb.append(',');
                             sb.append((String)keys.elementAt(i));
                         }
 
                         StringSelection ss = new StringSelection(sb.toString());
                         Toolkit.getDefaultToolkit().getSystemClipboard()
                             .setContents(ss, ths);
 
                         if (keys.size() == bes.length)
                             // All entries had keys.
                             output(Globals.lang((bes.length > 1) ? "Copied keys"
                                                 : "Copied key")+".");
                         else
                             output(Globals.lang("Warning")+": "+(bes.length-keys.size())
                                    +" "+Globals.lang("out of")+" "+bes.length+" "+
                                    Globals.lang("entries have undefined BibTeX key")+".");
                     }
                 }
             });
 
         // The action for copying a cite for the selected entry.
         actions.put("copyCiteKey", new BaseAction() {
                 public void action() {
                     BibtexEntry[] bes = entryTable.getSelectedEntries();
                     if ((bes != null) && (bes.length > 0)) {
                         //String[] keys = new String[bes.length];
                         Vector keys = new Vector();
                         // Collect all non-null keys.
                         for (int i=0; i<bes.length; i++)
                             if (bes[i].getField(Globals.KEY_FIELD) != null)
                                 keys.add(bes[i].getField(Globals.KEY_FIELD));
                         if (keys.size() == 0) {
                             output("None of the selected entries have BibTeX keys.");
                             return;
                         }
                         StringBuffer sb = new StringBuffer((String)keys.elementAt(0));
                         for (int i=1; i<keys.size(); i++) {
                             sb.append(',');
                             sb.append((String)keys.elementAt(i));
                         }
 
                         StringSelection ss = new StringSelection
                             ("\\cite{"+sb.toString()+"}");
                         Toolkit.getDefaultToolkit().getSystemClipboard()
                             .setContents(ss, ths);
 
                         if (keys.size() == bes.length)
                             // All entries had keys.
                             output(Globals.lang((bes.length > 1) ? "Copied keys"
                                                 : "Copied key")+".");
                         else
                             output(Globals.lang("Warning")+": "+(bes.length-keys.size())
                                    +" "+Globals.lang("out of")+" "+bes.length+" "+
                                    Globals.lang("entries have undefined BibTeX key")+".");
                     }
                 }
             });
 
           actions.put("mergeDatabase", new BaseAction() {
             public void action() {
 
                 final MergeDialog md = new MergeDialog(frame, Globals.lang("Append database"), true);
                 Util.placeDialog(md, ths);
                 md.setVisible(true);
                 if (md.okPressed) {
                   String chosenFile = Globals.getNewFile(frame, prefs, new File(prefs.get("workingDirectory")),
                                                          null, JFileChooser.OPEN_DIALOG, false);
                   /*JFileChooser chooser = (prefs.get("workingDirectory") == null) ?
                       new JabRefFileChooser((File)null) :
                       new JabRefFileChooser(new File(prefs.get("workingDirectory")));
                   chooser.addChoosableFileFilter( new OpenFileFilter() );//nb nov2
                   int returnVal = chooser.showOpenDialog(ths);*/
                   if(chosenFile == null)
                     return;
                   fileToOpen = new File(chosenFile);
 
                   // Run the actual open in a thread to prevent the program
                   // locking until the file is loaded.
                   if (fileToOpen != null) {
                     (new Thread() {
                       public void run() {
                         openIt(md.importEntries(), md.importStrings(),
                                md.importGroups(), md.importSelectorWords());
                       }
                     }).start();
                     frame.getFileHistory().newFile(fileToOpen.getPath());
                   }
                 }
               }
 
               void openIt(boolean importEntries, boolean importStrings,
                           boolean importGroups, boolean importSelectorWords) {
                 if ((fileToOpen != null) && (fileToOpen.exists())) {
                   try {
                     prefs.put("workingDirectory", fileToOpen.getPath());
                     // Should this be done _after_ we know it was successfully opened?
                     String encoding = Globals.prefs.get("defaultEncoding");
                     ParserResult pr = ImportFormatReader.loadDatabase(fileToOpen, encoding);
                     BibtexDatabase db = pr.getDatabase();
                     MetaData meta = new MetaData(pr.getMetaData());
                     NamedCompound ce = new NamedCompound("Append database");
 
                     if (importEntries) { // Add entries
                       Iterator i = db.getKeySet().iterator();
                       while (i.hasNext()) {
                         BibtexEntry be = (BibtexEntry)(db.getEntryById((String)i.next()).clone());
                         be.setId(Util.createNeutralId());
                         database.insertEntry(be);
                         ce.addEdit(new UndoableInsertEntry(database, be, ths));
                       }
                     }
 
                     if (importStrings) {
                       BibtexString bs;
                       int pos = 0;
                       Iterator i = db.getStringKeySet().iterator();
                       for (; i.hasNext();) {
                         bs = (BibtexString)(db.getString(i.next()).clone());
                         if (!database.hasStringLabel(bs.getName())) {
                             //pos = database.getStringCount();
                             database.addString(bs);
                             ce.addEdit(new UndoableInsertString(ths, database, bs));
                         }
                       }
                     }
 
                     if (importGroups) {
                       GroupTreeNode newGroups = meta.getGroups();
                       if (newGroups != null) {
                           
                           // ensure that there is always only one AllEntriesGroup
                           if (newGroups.getGroup() instanceof AllEntriesGroup)
                               newGroups.setGroup(new ExplicitGroup("Imported Groups"));
                           
 
                         groupSelector.addGroups(newGroups, ce);
                         groupSelector.revalidateGroups();
                       }
                     }
 
                     if (importSelectorWords) {
                       Iterator i=meta.iterator();
                       while (i.hasNext()) {
                         String s = (String)i.next();
                         if (s.startsWith(Globals.SELECTOR_META_PREFIX)) {
                           metaData.putData(s, meta.getData(s));
                         }
                       }
                     }
 
                     ce.end();
                     undoManager.addEdit(ce);
                     markBaseChanged();
                     refreshTable();
                     output("Imported from database '"+fileToOpen.getPath()+"':");
                     fileToOpen = null;
                   } catch (Throwable ex) {
                     ex.printStackTrace();
                     JOptionPane.showMessageDialog
                         (ths, ex.getMessage(),
                          "Open database", JOptionPane.ERROR_MESSAGE);
                   }
                 }
               }
             });
 
          actions.put("openFile", new BaseAction() {
            public void action() {
              (new Thread() {
                public void run() {
                  BibtexEntry[] bes = entryTable.getSelectedEntries();
                  String field = "ps";
                  if ( (bes != null) && (bes.length == 1)) {
                    Object link = bes[0].getField("ps");
                    if (bes[0].getField("pdf") != null) {
                      link = bes[0].getField("pdf");
                      field = "pdf";
                    }
                    String filepath = null;
                    if (link != null) {
                      filepath = link.toString();
                    }
                    else {
                      // see if we can fall back to a filename based on the bibtex key
                      String basefile;
                      Object key = bes[0].getField(Globals.KEY_FIELD);
                      if (key != null) {
                        basefile = key.toString();
                        String dir = prefs.get("pdfDirectory");
                        if (dir.endsWith(System.getProperty("file.separator"))) {
                          basefile = dir + basefile;
                        }
                        else {
                          basefile = dir + System.getProperty("file.separator") +
                              basefile;
                        }
                        final String[] typesToTry = new String[] {
                            "html", "ps", "pdf"};
                        for (int i = 0; i < typesToTry.length; i++) {
                          File f = new File(basefile + "." + typesToTry[i]);
                          Util.pr("Checking for " + f);
                          if (f.exists()) {
                            field = typesToTry[i];
                            filepath = f.getPath();
                            break;
                          }
                        }
                      }
                    }
 
                    if (filepath != null) {
                      //output(Globals.lang("Calling external viewer..."));
                      try {
                        Util.openExternalViewer(filepath, field, prefs);
                        output(Globals.lang("External viewer called") + ".");
                      }
                      catch (IOException ex) {
                        output(Globals.lang("Error") + ": " + ex.getMessage());
                      }
                    }
                    else
                      output(Globals.lang(
                          "No pdf or ps defined, and no file matching Bibtex key found") +
                             ".");
                  }
                  else
                    output(Globals.lang("No entries or multiple entries selected."));
                }
              }).start();
            }
          });
 
 
               actions.put("openUrl", new BaseAction() {
                       public void action() {
                           BibtexEntry[] bes = entryTable.getSelectedEntries();
                           String field = "doi";
                           if ((bes != null) && (bes.length == 1)) {
                               Object link = bes[0].getField("doi");
                               if (bes[0].getField("url") != null) {
                                 link = bes[0].getField("url");
                                 field = "url";
                               }
                               if (link != null) {
                                 //output(Globals.lang("Calling external viewer..."));
                                 try {
                                   Util.openExternalViewer(link.toString(), field, prefs);
                                   output(Globals.lang("External viewer called")+".");
                                 } catch (IOException ex) {
                                   output(Globals.lang("Error: check your External viewer settings in Preferences")+".");
                                 }
                               }
                               else
                                   output(Globals.lang("No url defined")+".");
                           } else
                             output(Globals.lang("No entries or multiple entries selected."));
                       }
               });
 
           actions.put("replaceAll", new BaseAction() {
                     public void action() {
                       ReplaceStringDialog rsd = new ReplaceStringDialog(frame);
                       rsd.show();
                       if (!rsd.okPressed())
                           return;
                       int counter = 0;
                       NamedCompound ce = new NamedCompound("Replace string");
                       if (!rsd.selOnly()) {
                           for (Iterator i=database.getKeySet().iterator();
                                i.hasNext();)
                               counter += rsd.replace(database.getEntryById((String)i.next()), ce);
                       } else {
                           BibtexEntry[] bes = entryTable.getSelectedEntries();
                           for (int i=0; i<bes.length; i++)
                               counter += rsd.replace(bes[i], ce);
                       }
                       output(Globals.lang("Replaced")+" "+counter+" "+
                              Globals.lang(counter==1?"occurence":"occurences")+".");
                       if (counter > 0) {
                           ce.end();
                           undoManager.addEdit(ce);
                           markBaseChanged();
                           refreshTable();
                       }
                   }
               });
 
               actions.put("dupliCheck", new BaseAction() {
                 public void action() {
                   DuplicateSearch ds = new DuplicateSearch(ths);
                   ds.start();
                 }
               });
 
               actions.put("strictDupliCheck", new BaseAction() {
                 public void action() {
                   StrictDuplicateSearch ds = new StrictDuplicateSearch(ths);
                   ds.start();
                 }
               });
 
               actions.put("plainTextImport", new BaseAction() {
                 public void action()
                 {
                   // get Type of new entry
                   EntryTypeDialog etd = new EntryTypeDialog(frame);
                   Util.placeDialog(etd, ths);
                   etd.show();
                   BibtexEntryType tp = etd.getChoice();
                   if (tp == null)
                     return;
 
                   String id = Util.createId(tp, database);
                   BibtexEntry bibEntry = new BibtexEntry(id, tp) ;
                   TextInputDialog tidialog = new TextInputDialog(frame,
                                                                  "import", true,
                                                                  bibEntry) ;
                   Util.placeDialog(tidialog, ths);
                   tidialog.show();
 
                   if (tidialog.okPressed())
                   {
                     insertEntry(bibEntry) ;
                   }
                 }
               });
 
               // The action starts the "import from plain text" dialog
               actions.put("importPlainText", new BaseAction() {
                       public void action()
                       {
                         BibtexEntry bibEntry = null ;
                         // try to get the first marked entry
                         BibtexEntry[] bes = entryTable.getSelectedEntries();
                         if ((bes != null) && (bes.length > 0))
                           bibEntry = bes[0] ;
 
                         if (bibEntry != null)
                         {
                           // Create an UndoableInsertEntry object.
                           undoManager.addEdit(new UndoableInsertEntry(database, bibEntry, ths));
 
                           TextInputDialog tidialog = new TextInputDialog(frame,
                                                                          "import", true,
                                                                          bibEntry) ;
                           Util.placeDialog(tidialog, ths);
                           tidialog.show();
 
                           if (tidialog.okPressed())
                           {
                             output(Globals.lang("changed ")+" '"
                                    +bibEntry.getType().getName().toLowerCase()+"' "
                                    +Globals.lang("entry")+".");
                             refreshTable();
                             int row = tableModel.getNumberFromName(bibEntry.getId());
 
                             entryTable.clearSelection();
                             entryTable.scrollTo(row);
                             markBaseChanged(); // The database just changed.
                             if (prefs.getBoolean("autoOpenForm"))
                             {
                                   showEntry(bibEntry);
                             }
                           }
                         }
                       }
                   });
 
               actions.put("markEntries", new BaseAction() {
                 public void action() {
 
                   NamedCompound ce = new NamedCompound("Mark entries");
                   BibtexEntry[] bes = entryTable.getSelectedEntries();
 		  if (bes == null)
 		      return;
                   for (int i=0; i<bes.length; i++) {
                     ce.addEdit(new UndoableFieldChange(bes[i], Globals.MARKED,
                                                        bes[i].getField(Globals.MARKED), "0"));
                     bes[i].setField(Globals.MARKED, "0");
                   }
                   ce.end();
                   undoManager.addEdit(ce);
                   markBaseChanged();
                   refreshTable();
                   output(Globals.lang("Marked selected")+" "+Globals.lang(bes.length>0?"entry":"entries"));
 
                 }
               });
 
               actions.put("unmarkEntries", new BaseAction() {
                 public void action() {
                     try {
                   NamedCompound ce = new NamedCompound("Unmark entries");
                   BibtexEntry[] bes = entryTable.getSelectedEntries();
 		  if (bes == null)
 		      return;
                   for (int i=0; i<bes.length; i++) {
                     ce.addEdit(new UndoableFieldChange(bes[i], Globals.MARKED,
                                                        bes[i].getField(Globals.MARKED), null));
                     bes[i].setField(Globals.MARKED, null);
                   }
                   ce.end();
                   undoManager.addEdit(ce);
                   markBaseChanged();
                   refreshTable();
                   output(Globals.lang("Unmarked selected")+" "+Globals.lang(bes.length>0?"entry":"entries"));
                     } catch (Throwable ex) { ex.printStackTrace(); }
                 }
               });
 
               actions.put("unmarkAll", new BaseAction() {
                 public void action() {
                   NamedCompound ce = new NamedCompound("Unmark all");
                   Set keySet = database.getKeySet();
                   for (Iterator i = keySet.iterator(); i.hasNext(); ) {
                     BibtexEntry be = database.getEntryById( (String) i.next());
                     ce.addEdit(new UndoableFieldChange(be, Globals.MARKED,
                                                        be.getField(Globals.MARKED), null));
                     be.setField(Globals.MARKED, null);
 
                   }
                   ce.end();
                   undoManager.addEdit(ce);
                   markBaseChanged();
                   refreshTable();
                 }
               });
 
               actions.put("togglePreview", new BaseAction() {
                       public void action() {
                           previewEnabled = !previewEnabled;
                           if (!previewEnabled)
                               hidePreview();
                           else {
                               //BibtexEntry[] bes = entryTable.getSelectedEntries();
                               //if ((bes != null) && (bes.length > 0))
                               updateViewToSelected();
                           }
                           frame.previewToggle.setSelected(previewEnabled);
                       }
                   });
 
               actions.put("switchPreview", new BaseAction() {
                       public void action() {
                           if (activePreview < previewPanel.length-1)
                               activePreview++;
                           else
                               activePreview = 0;
 
                           if (!previewEnabled)
                               hidePreview();
                           else {
                               //BibtexEntry[] bes = entryTable.getSelectedEntries();
                               //if ((bes != null) && (bes.length > 0))
                               updateViewToSelected();
                           }
                       }
                   });
 
               actions.put("manageSelectors", new BaseAction() {
                       public void action() {
                           ContentSelectorDialog2 csd = new ContentSelectorDialog2
                               (frame, ths, false, metaData, null);
                           Util.placeDialog(csd, frame);
                           csd.show();
                       }
                   });
 
 
               actions.put("exportToClipboard", new AbstractWorker() {
 		      String message = null;
 		      public void run() {
 			  if (entryTable.getSelectedRowCount() == 0) {
 			      message = Globals.lang("No entries selected")+".";
 			      getCallBack().update();
 			      return;
 			  }
 
 			  // Make a list of possible formats:
 			  Set formats = new HashSet();
 			  formats.add("bibtexml");
 			  formats.add("docbook");
 			  formats.add("html");
 			  formats.add("simplehtml");
 			  for (int i = 0; i < prefs.customExports.size(); i++) {
 			      formats.add((prefs.customExports.getElementAt(i))[0]);
 			  }
 			  JList list = new JList(formats.toArray());
 			  list.setBorder(BorderFactory.createEtchedBorder());
 			  list.setSelectionInterval(0,0);
 			  list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
 			  int answer = 
 			      JOptionPane.showOptionDialog(frame, list, Globals.lang("Select the format to copy entries in"),
 							   JOptionPane.YES_NO_OPTION,
 							   JOptionPane.QUESTION_MESSAGE, null, 
 							   new String[] {Globals.lang("Ok"), Globals.lang("Cancel")}, 
 							   Globals.lang("Ok"));
 			  
 			  if (answer == JOptionPane.NO_OPTION)
 			      return;
 			  
 			  String lfName = (String)(list.getSelectedValue());
 			  final boolean custom = (list.getSelectedIndex() >= Globals.STANDARD_EXPORT_COUNT);
 			  String dir = null;
 			  if (custom) {
 			      int index = list.getSelectedIndex()-Globals.STANDARD_EXPORT_COUNT;
 			      dir = (String)(prefs.customExports.getElementAt(index)[1]);
 			      File f = new File(dir);
 			      lfName = f.getName();
 			      lfName = lfName.substring(0, lfName.indexOf("."));
 			      // Remove file name - we want the directory only.
 			      dir = f.getParent()+System.getProperty("file.separator");
 			  }
 			  final String format = lfName,
 			      directory = dir;
 			  
 			  try {
 			      BibtexEntry[] bes = entryTable.getSelectedEntries();
 			      FileActions.exportToClipboard(database, bes, format, custom, directory, prefs);
 			      message = Globals.lang("Entries exported to clipboard")+": "+bes.length;
 			      
 			  } catch (Exception ex) {
 			      ex.printStackTrace();
 			  }
 
 		      }
 		      
 		      public void update() {
 			  output(message);
 		      }
 		     
 		  });
 
               actions.put("test", new AbstractWorker() {
 		      String filename = null, formatName = null;
 		      java.util.List entries = null;
 		      public void init() {
 			  output("importing");
 			  
 			  filename = Globals.getNewFile(frame,  prefs,
 							null, null,
 							JFileChooser.OPEN_DIALOG,
 							false);
 			  frame.block();
 		      }
 		      public void run() {
 			  if ((filename != null) && !(new File(filename)).exists()) {
 			      System.out.println("File not found");
 			      return;
 			  }
 			  try {
 			      Object[] o = Globals.importFormatReader.importUnknownFormat(filename);
 			      formatName = (String)o[0];
 			      entries = (java.util.List)o[1];
 			  } catch (IOException ex) {
 			      ex.printStackTrace();
 			  }
 		      }
 		      public void update() {
 			  if (entries != null) {
 			      frame.addBibEntries(entries, filename, true);
 			      output(Globals.lang("Imported entries")+": "+entries.size()
 				     +"  "+Globals.lang("Format used")+": "+formatName);
 			  } else
 			      output(Globals.lang("Could not find a suitable import format."));
 			  frame.unblock();
 		      }
 		  });
 
     }
 
     /**
      * This method is called from JabRefFrame is a database specific
      * action is requested by the user. Runs the command if it is
      * defined, or prints an error message to the standard error
      * stream.
      *
      * @param command The name of the command to run.
     */
     public void runCommand(String _command) throws Throwable {
       final String command = _command;
       //(new Thread() {
       //  public void run() {
           if (actions.get(command) == null)
             Util.pr("No action defined for'" + command + "'");
             else {
 		Object o = actions.get(command);
 		try {
 		    if (o instanceof BaseAction)
 			((BaseAction)o).action();
 		    else {
 			// This part uses Spin's features:
 			Worker wrk = ((AbstractWorker)o).getWorker();
 			// The Worker returned by getWorker() has been wrapped
 			// by Spin.off(), which makes its methods be run in
 			// a different thread from the EDT.
 			CallBack clb = ((AbstractWorker)o).getCallBack();
 
 			((AbstractWorker)o).init(); // This method runs in this same thread, the EDT.
 			// Useful for initial GUI actions, like printing a message.
 
 			// The CallBack returned by getCallBack() has been wrapped
 			// by Spin.over(), which makes its methods be run on
 			// the EDT.			
 			wrk.run(); // Runs the potentially time-consuming action
 			// without freezing the GUI. The magic is that THIS line
 			// of execution will not continue until run() is finished.
 			clb.update(); // Runs the update() method on the EDT.
 		    }
 		} catch (Throwable ex) {
 		    // If the action has blocked the JabRefFrame before crashing, we need to unblock it.
 		    // The call to unblock will simply hide the glasspane, so there is no harm in calling
 		    // it even if the frame hasn't been blocked.
 		    frame.unblock();
 		    ex.printStackTrace();
 		}
 	    }
       //  }
       //}).start();
     }
 
     private void saveDatabase(File file, boolean selectedOnly) throws SaveException {
         try {
             if (!selectedOnly)
               FileActions.saveDatabase(database, metaData, file,
                                          prefs, false, false, prefs.get("defaultEncoding"));
             else
                 FileActions.savePartOfDatabase(database, metaData, file,
                                                prefs, entryTable.getSelectedEntries(), prefs.get("defaultEncoding"));
         } catch (SaveException ex) {
             if (ex.specificEntry()) {
                 // Error occured during processing of
                 // be. Highlight it:
                 int row = tableModel.getNumberFromName
                     (ex.getEntry().getId()),
                     topShow = Math.max(0, row-3);
                 //Util.pr(""+row);
                 entryTable.setRowSelectionInterval(row, row);
                 entryTable.setColumnSelectionInterval
                     (0, entryTable.getColumnCount()-1);
                 entryTable.scrollTo(topShow);
                 showEntry(ex.getEntry());
             }
             else ex.printStackTrace();
 
             JOptionPane.showMessageDialog
                 (frame, Globals.lang("Could not save file")
                  +".\n"+ex.getMessage(),
                  Globals.lang("Save database"),
                  JOptionPane.ERROR_MESSAGE);
             throw new SaveException("rt");
         }
     }
 
 
     /**
      * This method is called from JabRefFrame when the user wants to
      * create a new entry. If the argument is null, the user is
      * prompted for an entry type.
      *
      * @param type The type of the entry to create.
      */
     public void newEntry(BibtexEntryType type) {
         if (type == null) {
             // Find out what type is wanted.
             EntryTypeDialog etd = new EntryTypeDialog(frame);
             // We want to center the dialog, to make it look nicer.
             Util.placeDialog(etd, frame);
             etd.setVisible(true);
             type = etd.getChoice();
         }
         if (type != null) { // Only if the dialog was not cancelled.
             String id = Util.createId(type, database);
             BibtexEntry be = new BibtexEntry(id, type);
             try {
                 database.insertEntry(be);
             // Create new Bibtex entry
             // Set owner field to default value
             if (prefs.getBoolean("useOwner"))
               be.setField(Globals.OWNER, prefs.get("defaultOwner") );
                 // Create an UndoableInsertEntry object.
                 undoManager.addEdit(new UndoableInsertEntry(database, be, ths));
                 output(Globals.lang("Added new")+" '"+type.getName().toLowerCase()+"' "
                        +Globals.lang("entry")+".");
                 refreshTable();
                 final int row = tableModel.getNumberFromName(id);
                 //Util.pr(""+row);
                 entryTable.clearSelection();                
                 markBaseChanged(); // The database just changed.
                 if (prefs.getBoolean("autoOpenForm")) {
                     showEntry(be);
                     /*
                     SwingUtilities.invokeLater(new Thread() {
                         public void run() {
                             entryTable.revalidate();
                             entryTable.setRowSelectionInterval(row, row);
                             entryTable.scrollTo(row);        
                         }
                     });*/
                     
                     
                     //EntryTypeForm etf = new EntryTypeForm(frame, ths, be, prefs);
                     //Util.placeDialog(etf, frame);
                     //etf.setVisible(true);
                     //entryTypeForms.put(id, etf);
                 }
             } catch (KeyCollisionException ex) {
                 Util.pr(ex.getMessage());
             }
         }
     }
 
 
     /**
      * This method is called from JabRefFrame when the user wants to
      * create a new entry.
      * @param bibEntry The new entry.
      */
     public void insertEntry(BibtexEntry bibEntry)
     {
       if (bibEntry != null)
       {
         try
         {
           database.insertEntry(bibEntry) ;
           if (prefs.getBoolean("useOwner"))
             // Set owner field to default value
             bibEntry.setField(Globals.OWNER, prefs.get("defaultOwner") );
             // Create an UndoableInsertEntry object.
             undoManager.addEdit(new UndoableInsertEntry(database, bibEntry, ths));
             output(Globals.lang("Added new")+" '"
                    +bibEntry.getType().getName().toLowerCase()+"' "
                    +Globals.lang("entry")+".");
             refreshTable();
             int row = tableModel.getNumberFromName(bibEntry.getId());
 
             entryTable.clearSelection();
             entryTable.scrollTo(row);
             markBaseChanged(); // The database just changed.
             if (prefs.getBoolean("autoOpenForm"))
             {
                   showEntry(bibEntry);
             }
         } catch (KeyCollisionException ex) { Util.pr(ex.getMessage()); }
       }
     }
 
     public void validateMainPanel() {
     }
 
     public void setupTable() {
         tableModel = new EntryTableModel(frame, this, database);
         entryTable = new EntryTable(tableModel, ths, frame.prefs);
         entryTable.getActionMap().put("cut", new AbstractAction() {
                 public void actionPerformed(ActionEvent e) {
                     try { runCommand("cut");
                     } catch (Throwable ex) {
                         ex.printStackTrace();
                     }
                 }
             });
         entryTable.getActionMap().put("copy", new AbstractAction() {
                 public void actionPerformed(ActionEvent e) {
                     try { runCommand("copy");
                     } catch (Throwable ex) {
                         ex.printStackTrace();
                     }
                 }
             });
         entryTable.getActionMap().put("paste", new AbstractAction() {
                 public void actionPerformed(ActionEvent e) {
                     try { runCommand("paste");
                     } catch (Throwable ex) {
                         ex.printStackTrace();
                     }
                 }
             });
 
         /*
         entryTable.getInputMap().put(prefs.getKey("Edit entry"), "Edit");
         entryTable.getActionMap().put("Edit", new AbstractAction() {
                 public void actionPerformed(ActionEvent e) {
                     Util.pr("eueo");
                     try { runCommand("edit");
                     } catch (Throwable ex) {
                         ex.printStackTrace();
                     }
                 }
             });
         */
 
         entryTable.addKeyListener(new KeyAdapter() {
 
                 public void keyPressed(KeyEvent e) {
                     if (e.getKeyCode() == KeyEvent.VK_ENTER){
                         e.consume();
                         try { runCommand("edit");
                         } catch (Throwable ex) {
                             ex.printStackTrace();
                         }
                     }
                     else if(e.getKeyCode() == KeyEvent.VK_DELETE){
                         try { runCommand("delete");
                         } catch (Throwable ex) {
                             ex.printStackTrace();
                         }
                     }
                     /*
                     if (((e.getKeyCode() == KeyEvent.VK_DOWN) || (e.getKeyCode() == KeyEvent.VK_UP))
                       && (e.getModifiers() == 0)) {
 
                       Util.pr(entryTable.getSelectedRow()+"");
                     }*/
                   }
             });
 
 
         // Set the right-click menu for the entry table.
         //rcm = new RightClickMenu(this, metaData);
         entryTable.setRightClickMenu(rcm);
         int pos = splitPane.getDividerLocation();
         splitPane.setTopComponent(entryTable.getPane());
         splitPane.setDividerLocation(pos);
         //splitPane.revalidate();
 
     }
 
     public void setupMainPanel() {
 
         splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
         splitPane.setDividerSize(GUIGlobals.SPLIT_PANE_DIVIDER_SIZE);
         // We replace the default FocusTraversalPolicy with a subclass
         // that only allows FieldEditor components to gain keyboard focus,
         // if there is an entry editor open.
         /*splitPane.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
                 protected boolean accept(Component c) {
                     Util.pr("jaa");
                     if (showing == null)
                         return super.accept(c);
                     else
                         return (super.accept(c) &&
                                 (c instanceof FieldEditor));
                 }
                 });*/
 
         setupTable();
         // If an entry is currently being shown, make sure it stays shown,
         // otherwise set the bottom component to null.
         if (showing == null) {
           splitPane.setBottomComponent(previewPanel[activePreview].getPane());
           if ((previewPanel[activePreview] != null) && previewPanel[activePreview].hasEntry()) {
             //splitPane.setDividerLocation(splitPane.getHeight()-GUIGlobals.PREVIEW_HEIGHT[activePreview]);
             final int prevSize = Math.min(splitPane.getHeight()/2, previewPanel[activePreview].getPreferredSize().height
                                           + GUIGlobals.PREVIEW_PANEL_PADDING);
             //            Util.pr(""+prevSize+" "+(splitPane.getHeight()/2)+" "+previewPanel[activePreview].getPreferredSize().height);
             splitPane.setDividerLocation(splitPane.getHeight() - prevSize);
             /*SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                 splitPane.setDividerLocation(splitPane.getHeight() - prevSize);
               }
             });*/
 
           } else
             splitPane.setBottomComponent(null);
         }
         else
             showEntry(showing);
 
         sidePaneManager = new SidePaneManager
             (frame, this, prefs, metaData);
         medlineFetcher = new MedlineFetcher(this, sidePaneManager);
         citeSeerFetcher = new CiteSeerFetcher(this, sidePaneManager);
         sidePaneManager.register("fetchMedline", medlineFetcher);
         //medlineAuthorFetcher = new MedlineAuthorFetcher(this, sidePaneManager);
         //sidePaneManager.register("fetchAuthorMedline", medlineAuthorFetcher);
         searchManager = new SearchManager2(frame, prefs, sidePaneManager);
         sidePaneManager.add("search", searchManager);
         sidePaneManager.register("CiteSeerProgress", citeSeerFetcher);
         sidePaneManager.populatePanel();
         setLayout(new BorderLayout());
         add(sidePaneManager.getPanel(), BorderLayout.WEST);
         add(splitPane, BorderLayout.CENTER);
         
 	//setLayout(gbl);
 	//con.fill = GridBagConstraints.BOTH;
 	//con.weighty = 1;
 	//con.weightx = 0;
 	//gbl.setConstraints(sidePaneManager.getPanel(), con);	
 	//con.weightx = 1;
 	//gbl.setConstraints(splitPane, con);
         //mainPanel.setDividerLocation(GUIGlobals.SPLIT_PANE_DIVIDER_LOCATION);
         //setDividerSize(GUIGlobals.SPLIT_PANE_DIVIDER_SIZE);
         //setResizeWeight(0);
 
         revalidate();
     }
 
     /**
      * This method is called after a database has been parsed. The
      * hashmap contains the contents of all comments in the .bib file
      * that started with the meta flag (GUIGlobals.META_FLAG).
      * In this method, the meta data are input to their respective
      * handlers.
      *
      * @param meta Metadata to input.
      */
     public void parseMetaData(HashMap meta) {
         metaData = new MetaData(meta);
 
     }
 
     public void refreshTable() {
         // This method is called by EntryTypeForm when a field value is
         // stored. The table is scheduled for repaint.
         entryTable.assureNotEditing();
         //entryTable.invalidate();
         BibtexEntry[] bes = entryTable.getSelectedEntries();
         tableModel.remap();
         if ((bes != null) && (bes.length > 0))
             highlightEntries(bes, 0);
         //entryTable.revalidate();r
         //entryTable.repaint();
     }
 
     public void updatePreamble() {
         if (preambleEditor != null)
             preambleEditor.updatePreamble();
     }
 
     public void assureStringDialogNotEditing() {
         if (stringDialog != null)
             stringDialog.assureNotEditing();
     }
 
     public void updateStringDialog() {
         if (stringDialog != null)
             stringDialog.refreshTable();
     }
 
     public void updateViewToSelected() {
       // First, if the entry editor is visible, we should update it to the selected entry.
 
       BibtexEntry be = null;
       BibtexEntry[] bes = entryTable.getSelectedEntries();
       if ((bes != null) && (bes.length > 0))
         be = bes[0];
       else if (showing != null)
         return;
 
       if (showing != null) {
         showEntry(be);
         return;
       }
 
       // If no entry editor is visible we must either instantiate a new preview panel or update the one we have.
       if (!previewEnabled || be==null) {
         splitPane.setBottomComponent(null);
         return; // Do nothing if previews are disabled.
       }
       if (previewPanel[activePreview] == null) {
         previewPanel[activePreview] = new PreviewPanel(be, prefs.get("preview"+activePreview));
 
       } else
         previewPanel[activePreview].setEntry(be);
 
       //splitPane.setDividerLocation(splitPane.getHeight()-GUIGlobals.PREVIEW_HEIGHT[activePreview]);
 
 
       //Util.pr(""+prevSize+" "+(splitPane.getHeight()/2)+" "+previewPanel[activePreview].getPreferredSize().height);
 
       //splitPane.setDividerLocation(splitPane.getHeight() - prevSize);
       //splitPane.resetToPreferredSizes();
       //previewPanel[activePreview].getPane().invalidate();
 
     splitPane.setBottomComponent(previewPanel[activePreview].getPane());
 
     int prevSize = Math.min(splitPane.getHeight()/2, previewPanel[activePreview].getPane().getPreferredSize().height
                             + GUIGlobals.PREVIEW_PANEL_PADDING);
   //  int prevSize = 130;
 
           splitPane.setDividerLocation(splitPane.getHeight() - prevSize);
 
 /*      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
 
         }
       });*/
 
       //previewPanel[activePreview].repaint();
       //throw new NullPointerException("..");
     }
 
     /**
      * Stores the source view in the entry editor, if one is open, has the source view
      * selected and the source has been edited.
      * @return boolean false if there is a validation error in the source panel, true otherwise.
      */
     public boolean entryEditorAllowsChange() {
       Component c = splitPane.getBottomComponent();
       if ((c != null) && (c instanceof EntryEditor)) {
         return ((EntryEditor)c).lastSourceAccepted();
       }
       else
         return true;
     }
 
     public void moveFocusToEntryEditor() {
       Component c = splitPane.getBottomComponent();
       if ((c != null) && (c instanceof EntryEditor)) {
         new FocusRequester(c);
       }
     }
 
     /**
      * Ensure that no preview is shown. Called when preview is turned off. Must chech if
      * a preview is in fact visible before doing anything rash.
      */
     public void hidePreview() {
       previewEnabled = false;
       //previewPanel[activePreview] = null;
       Component c = splitPane.getBottomComponent();
       if ((c != null) && !(c instanceof EntryEditor))
         splitPane.setBottomComponent(null);
     }
 
     public boolean isShowingEditor() {
       return ((splitPane.getBottomComponent() != null)
               && (splitPane.getBottomComponent() instanceof EntryEditor));
     }
 
     public void showEntry(final BibtexEntry be) {
         if (showing == be) {
             if (splitPane.getBottomComponent() == null) {
                 // This is the special occasion when showing is set to an
                 // entry, but no entry editor is in fact shown. This happens
                 // after Preferences dialog is closed, and it means that we
                 // must make sure the same entry is shown again. We do this by
                 // setting showing to null, and recursively calling this method.
                 showing = null;
                 showEntry(be);
             } else {
               // The correct entry is already being shown. Make sure the editor
               // is updated.
               ((EntryEditor)splitPane.getBottomComponent()).updateAllFields();
 
             }
             return;
 
         }
 
         EntryEditor form;
         int divLoc = -1, visPan = -1;
         if (showing != null)
             visPan = ((EntryEditor)splitPane.getBottomComponent()).
                 getVisiblePanel();
         if (showing != null)
             divLoc = splitPane.getDividerLocation();
 
         if (entryEditors.containsKey(be.getType().getName())) {
             // We already have an editor for this entry type.
             form = (EntryEditor)entryEditors.get
                 ((be.getType().getName()));
             form.switchTo(be);
             if (visPan >= 0)
               form.setVisiblePanel(visPan);
             splitPane.setBottomComponent(form);
             highlightEntry(be);
         } else {
             // We must instantiate a new editor for this type.
             form = new EntryEditor
                 (frame, ths, be, prefs);
             if (visPan >= 0)
                 form.setVisiblePanel(visPan);
             splitPane.setBottomComponent(form);            
             
             highlightEntry(be);
             entryEditors.put(be.getType().getName(), form);
            
         }
         if (divLoc > 0) {
           splitPane.setDividerLocation(divLoc);
         }
         else
             splitPane.setDividerLocation
                 (GUIGlobals.VERTICAL_DIVIDER_LOCATION);
         //new FocusRequester(form);
         //form.requestFocus();
 
         showing = be;
         setEntryEditorEnabled(true); // Make sure it is enabled.
     }
 
     /**
      * Closes the entry editor.
      * Set showing to null, and call updateViewToSelected.
      */
     public void hideEntryEditor() {
       BibtexEntry be = showing;
       showing = null;
       if (be != null) {
         if (entryTable.getSelectedRows().length == 0) {
           int row = tableModel.getNumberFromName(be.getId());
           entryTable.addRowSelectionInterval(row, row);
         }
         updateViewToSelected();
       }
       new FocusRequester(entryTable);
         /*splitPane.setBottomComponent(previewPanel);
         if (previewPanel != null)
           splitPane.setDividerLocation(splitPane.getHeight()-GUIGlobals.PREVIEW_HEIGHT);
 
 */
     }
 
     /**
      * This method selects the given entry, and scrolls it into view in the table.
      * If an entryEditor is shown, it is given focus afterwards.
      */
     public void highlightEntry(final BibtexEntry be) {
         SwingUtilities.invokeLater(new Thread() {
              public void run() {                                          
                  entryTable.revalidate();
                  int row = tableModel.getNumberFromName(be.getId());
                  //System.out.println("eee:"+row);
                  entryTable.setRowSelectionInterval(row, row);
                  entryTable.ensureVisible(row);
                  Component comp = splitPane.getBottomComponent();
                  if (comp instanceof EntryEditor)
                      comp.requestFocus();
              }
         });
     }
     
     /**
      * This method selects the given enties.
      * If an entryEditor is shown, it is given focus afterwards.
      */
     public void highlightEntries(final BibtexEntry[] bes, final int toScrollTo) {
         SwingUtilities.invokeLater(new Thread() {
              public void run() {    
                  int rowToScrollTo = 0;
                  entryTable.revalidate();
                  loop: for (int i=0; i<bes.length; i++) {                     
 		     if (bes[i] == null)
 			 continue loop;
 		     int row = tableModel.getNumberFromName(bes[i].getId());                    
 		     if (i==toScrollTo)
 			 rowToScrollTo = row;
 		     entryTable.addRowSelectionIntervalQuietly(row, row);
                  }
                  entryTable.ensureVisible(rowToScrollTo);
                  Component comp = splitPane.getBottomComponent();
                  if (comp instanceof EntryEditor)
                      comp.requestFocus();
              }
         });
     }
     
     /**
      * Closes the entry editor if it is showing the given entry.
      *
      * @param be a <code>BibtexEntry</code> value
      */
     public void ensureNotShowing(BibtexEntry be) {
         if (showing == be) {
             hideEntryEditor();
             showing = null;
         }
     }
 
     public void updateEntryEditorIfShowing() {
       if (isShowingEditor()) {
         EntryEditor editor = (EntryEditor)splitPane.getBottomComponent();
         if (editor.getType() != editor.entry.getType()) {
           // The entry has changed type, so we must get a new editor.
           showing = null;
           showEntry(editor.entry);
         } else
           editor.updateAllFields();
       }
     }
 
     /**
      * This method iterates through all existing entry editors in this
      * BasePanel, telling each to update all its instances of
      * FieldContentSelector. This is done to ensure that the list of words
      * in each selector is up-to-date after the user has made changes in
      * the Manage dialog.
      */
     public void updateAllContentSelectors() {
         for (Iterator i=entryEditors.keySet().iterator(); i.hasNext();) {
             EntryEditor ed = (EntryEditor)entryEditors.get(i.next());
             ed.updateAllContentSelectors();
         }
     }
 
     public void rebuildAllEntryEditors() {
         for (Iterator i=entryEditors.keySet().iterator(); i.hasNext();) {
             EntryEditor ed = (EntryEditor)entryEditors.get(i.next());
             ed.rebuildPanels();
         }
 
     }
 
     public void markBaseChanged() {
         baseChanged = true;
 
         // Put an asterix behind the file name to indicate the
         // database has changed.
         String oldTitle = frame.getTabTitle(this);
         if (!oldTitle.endsWith("*"))
             frame.setTabTitle(this, oldTitle+"*");
 
         // If the status line states that the base has been saved, we
         // remove this message, since it is no longer relevant. If a
         // different message is shown, we leave it.
         if (frame.statusLine.getText().startsWith("Saved database"))
             frame.output(" ");
     }
 
     public void markNonUndoableBaseChanged() {
         nonUndoableChange = true;
         markBaseChanged();
     }
 
     public synchronized void markChangedOrUnChanged() {
         if (undoManager.hasChanged()) {
             if (!baseChanged)
                 markBaseChanged();
         }
         else if (baseChanged && !nonUndoableChange) {
             baseChanged = false;
             if (file != null)
                 frame.setTabTitle(ths, file.getName());
             else
                 frame.setTabTitle(ths, Globals.lang("untitled"));
         }
     }
 
     /**
      * Shows either normal search results or group search, depending
      * on the searchValueField. This is done by reordering entries and
      * graying out non-hits.
      *
      * @param searchValueField Which field to show search for: Globals.SEARCH or
      * Globals.GROUPSEARCH.
      *
      */
     public void showSearchResults(String searchValueField, boolean reorder, boolean grayOut, boolean select) {
         //entryTable.scrollTo(0);
 
         entryTable.invalidate();
         if (searchValueField == Globals.SEARCH) {
           sortingBySearchResults = reorder;
           coloringBySearchResults = grayOut;
         }
         else if (searchValueField == Globals.GROUPSEARCH) {
           sortingByGroup = reorder;
           coloringByGroup = grayOut;
         }
 
         //tableModel.remap();
         entryTable.clearSelection();
         refreshTable();
 
         if (select) {
 
           selectResults(searchValueField);
 
         }
         else {
           entryTable.clearSelection();
         }
 
         if (reorder)
           entryTable.scrollTo(0);
 
         //entryTable.revalidate();
         //entryTable.repaint();
 
     }
 
     /**
      * Selects all entries with a non-zero value in the field
      * @param <code>String</code> field name.
      */
     public void selectResults(String field) {
       LinkedList intervals = new LinkedList();
       int prevStart = -1, prevToSel = 0;
       // First we build a list of intervals to select, without touching the table.
       for (int i = 0; i < entryTable.getRowCount(); i++) {
         String value = (String) (database.getEntryById
                                  (tableModel.getNameFromNumber(i)))
             .getField(field);
         if ( (value != null) && !value.equals("0")) {
           if (prevStart < 0)
             prevStart = i;
           prevToSel = i;
         }
         else if (prevStart >= 0) {
           intervals.add(new int[] {prevStart, prevToSel});
           prevStart = -1;
         }
       }
       // Then select those intervals, if any.
       if (intervals.size() > 0) {
         entryTable.setSelectionListenerEnabled(false);
         entryTable.clearSelection();
         for (Iterator i=intervals.iterator(); i.hasNext();) {
           int[] interval = (int[])i.next();
           entryTable.addRowSelectionInterval(interval[0], interval[1]);
         }
         entryTable.setSelectionListenerEnabled(true);
       }
     }
 
     /**
      * Selects a single entry, and scrolls the table to center it.
      *
      * @param pos Current position of entry to select.
      *
      */
     public void selectSingleEntry(int pos) {
         entryTable.clearSelection();
         entryTable.addRowSelectionInterval(pos, pos);
         entryTable.scrollToCenter(pos, 0);
     }
 
     public void stopShowingSearchResults() {
       sortingBySearchResults = false;
       coloringBySearchResults = false;
       /* entryTable.setShowingSearchResults(showingSearchResults,
         showingGroup);
        */
       entryTable.clearSelection();
       refreshTable();
       entryTable.requestFocus();
     }
 
     public void stopShowingGroup() {
       sortingByGroup = false;
       coloringByGroup = false;
 /*
       entryTable.setShowingSearchResults(showingSearchResults,
                                          showingGroup);*/
       entryTable.clearSelection();
       refreshTable();
     }
 
     public EntryTableModel getTableModel(){
                 return tableModel ;
     }
     
    public BibtexEntry[] getSelectedEntries() {
        return entryTable.getSelectedEntries();
    }

     public boolean isEntriesSelected() {
         return entryTable.getSelectedRows().length > 0;
     }
 
     public BibtexDatabase getDatabase(){
         return database ;
     }
 
     public void preambleEditorClosing() {
         preambleEditor = null;
     }
 
     public void stringsClosing() {
         stringDialog = null;
     }
 
     public void changeType(BibtexEntry entry, BibtexEntryType type) {
       changeType(new BibtexEntry[] {entry}, type);
     }
 
     public void changeType(BibtexEntryType type) {
       BibtexEntry[] bes = entryTable.getSelectedEntries();
       changeType(bes, type);
     }
 
     public void changeType(BibtexEntry[] bes, BibtexEntryType type) {
 
         if ((bes == null) || (bes.length == 0)) {
             output("First select the entries you wish to change type "+
                    "for.");
             return;
         }
         if (bes.length > 1) {
             int choice = JOptionPane.showConfirmDialog
                 (this, "Multiple entries selected. Do you want to change"
                  +"\nthe type of all these to '"+type.getName()+"'?",
                  "Change type", JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE);
             if (choice == JOptionPane.NO_OPTION)
                 return;
         }
 
         NamedCompound ce = new NamedCompound("change type");
         for (int i=0; i<bes.length; i++) {
             ce.addEdit(new UndoableChangeType(bes[i],
                                               bes[i].getType(),
                                               type));
             bes[i].setType(type);
         }
 
         output(Globals.lang("Changed type to")+" '"+type.getName()+"' "
                +Globals.lang("for")+" "+bes.length
                +" "+Globals.lang("entries")+".");
         ce.end();
         undoManager.addEdit(ce);
         refreshTable();
         markBaseChanged();
         updateEntryEditorIfShowing();
     }
 
     public boolean showDeleteConfirmationDialog(int numberOfEntries) {
         if (prefs.getBoolean("confirmDelete")) {
             String msg = Globals.lang("Really delete the selected")
                 + " " + Globals.lang("entry") + "?",
                 title = Globals.lang("Delete entry");
             if (numberOfEntries > 1) {
                 msg = Globals.lang("Really delete the selected")
                     + " " + numberOfEntries + " " + Globals.lang("entries") + "?";
                 title = Globals.lang("Delete multiple entries");
             }
 
             CheckBoxMessage cb = new CheckBoxMessage
                 (msg, Globals.lang("Disable this confirmation dialog"), false);
 
             int answer = JOptionPane.showConfirmDialog(frame, cb, title,
                                                        JOptionPane.YES_NO_OPTION,
                                                        JOptionPane.QUESTION_MESSAGE);
             if (cb.isSelected())
                 prefs.putBoolean("confirmDelete", false);
             return (answer == JOptionPane.YES_OPTION);
         } else return true;
 
     }
 
     class UndoAction extends BaseAction {
         public void action() {
             try {
                 String name = undoManager.getUndoPresentationName();
                 undoManager.undo();
                 markBaseChanged();
                 refreshTable();
                 frame.output(name);
             } catch (CannotUndoException ex) {
                 frame.output(Globals.lang("Nothing to undo")+".");
             }
             // After everything, enable/disable the undo/redo actions
             // appropriately.
             //updateUndoState();
             //redoAction.updateRedoState();
             markChangedOrUnChanged();
         }
     }
 
     class RedoAction extends BaseAction {
         public void action() {
             try {
                 String name = undoManager.getRedoPresentationName();
                 undoManager.redo();
                 markBaseChanged();
                 refreshTable();
                 frame.output(name);
             } catch (CannotRedoException ex) {
                 frame.output(Globals.lang("Nothing to redo")+".");
             }
             // After everything, enable/disable the undo/redo actions
             // appropriately.
             //updateRedoState();
             //undoAction.updateUndoState();
             markChangedOrUnChanged();
         }
     }
 
     // Method pertaining to the ClipboardOwner interface.
     public void lostOwnership(Clipboard clipboard, Transferable contents) {}
 
   public boolean previewEnabled() {
     return previewEnabled;
   }
 
   public void setEntryEditorEnabled(boolean enabled) {
     if ((showing != null) && (splitPane.getBottomComponent() instanceof EntryEditor)) {
           EntryEditor ed = (EntryEditor)splitPane.getBottomComponent();
           if (ed.isEnabled() != enabled)
             ed.setEnabled(enabled);
     }
   }
 
   public String fileMonitorHandle() { return fileMonitorHandle; }
 
     public void fileUpdated() {
       if (saving)
         return; // We are just saving the file, so this message is most likely due
       // to bad timing. If not, we'll handle it on the next polling.
       //Util.pr("File '"+file.getPath()+"' has been modified.");
       updatedExternally = true;
 
       // Adding the sidepane component is Swing work, so we must do this in the Swing
       // thread:
       Thread t = new Thread() {
         public void run() {
           FileUpdatePanel pan = new FileUpdatePanel(frame, ths, sidePaneManager, Globals.prefs);
           sidePaneManager.add("fileUpdate", pan);
         }
       };
       SwingUtilities.invokeLater(t);
 
     }
 
       public void fileRemoved() {
         Util.pr("File '"+file.getPath()+"' has been deleted.");
       }
 
 
       public void cleanUp() {
         if (fileMonitorHandle != null)
           Globals.fileUpdateMonitor.removeUpdateListener(fileMonitorHandle);
       }
 
   public void setUpdatedExternally(boolean b) {
     updatedExternally = b;
   }
   
   public void addEntryTableSelectionListener(ListSelectionListener listener) {
       entryTable.getSelectionModel().addListSelectionListener(listener);
   }
 }
