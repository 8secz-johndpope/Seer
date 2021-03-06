 package ch.cyberduck.ui.cocoa;
 
 /*
  *  Copyright (c) 2005 David Kocher. All rights reserved.
  *  http://cyberduck.ch/
  *
  *  This program is free software; you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation; either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  Bug fixes, suggestions and comments should be sent to:
  *  dkocher@cyberduck.ch
  */
 
 import com.apple.cocoa.application.*;
 import com.apple.cocoa.foundation.*;
 
 import ch.cyberduck.core.*;
 import ch.cyberduck.ui.cocoa.threading.WindowMainAction;
 
 import org.apache.log4j.Logger;
 
 import java.util.Iterator;
 
 /**
  * @version $Id$
  */
 public abstract class CDTransferPrompt extends CDSheetController implements TransferPrompt {
     private static Logger log = Logger.getLogger(CDTransferPrompt.class);
 
     public static TransferPrompt create(CDWindowController parent, final Transfer transfer) {
         if(transfer instanceof DownloadTransfer) {
             return new CDDownloadPrompt(parent, transfer);
         }
         if(transfer instanceof UploadTransfer) {
             return new CDUploadPrompt(parent, transfer);
         }
         if(transfer instanceof SyncTransfer) {
             return new CDSyncPrompt(parent, transfer);
         }
         throw new IllegalArgumentException(transfer.toString());
     }
 
     public CDTransferPrompt(final CDWindowController parent, final Transfer transfer) {
         super(parent);
         this.transfer = transfer;
     }
 
     protected String getBundleName() {
         return "Prompt";
     }
 
     public void awakeFromNib() {
         this.transfer.getSession().addProgressListener(l);
         this.reloadData();
         if(browserView.numberOfRows() > 0) {
             browserView.selectRow(0, false);
         }
     }
 
     public void invalidate() {
         this.transfer.getSession().removeProgressListener(l);
         super.invalidate();
     }
 
     /**
      *
      */
     private ProgressListener l = new ProgressListener() {
         public void message(final String msg) {
             CDMainApplication.invoke(new WindowMainAction(CDTransferPrompt.this) {
                 public void run() {
                     // Update the status label at the bottom of the browser window
                     statusLabel.setAttributedStringValue(new NSAttributedString(msg,
                             TRUNCATE_MIDDLE_ATTRIBUTES));
                 }
             });
         }
     };
 
     /**
      *
      */
     protected TransferAction action
             = TransferAction.forName(Preferences.instance().getProperty("queue.prompt.action.default"));
 
     /**
      * @return
      */
     public TransferAction getAction() {
         return action;
     }
 
     /**
      *
      */
     protected Transfer transfer;
 
     public void callback(final int returncode) {
         log.debug("callback:" + returncode);
         if(returncode == CANCEL_OPTION) { // Abort
             action = TransferAction.ACTION_CANCEL;
         }
     }
 
     /**
      * Reload the files in the prompt dialog
      */
     public void reloadData() {
         log.debug("reloadData");
         statusIndicator.startAnimation(null);
         browserView.reloadData();
         statusIndicator.stopAnimation(null);
         statusLabel.setAttributedStringValue(new NSAttributedString(
                 browserView.numberOfRows() + " " + NSBundle.localizedString("files", ""),
                 TRUNCATE_MIDDLE_ATTRIBUTES));
     }
 
     public TransferAction prompt() {
         log.debug("prompt:" + transfer);
         for(Iterator iter = transfer.getRoots().iterator(); iter.hasNext(); ) {
             Path next = (Path) iter.next();
             if(browserModel.filter().accept(next)) {
                 browserModel.add(next);
             }
         }
         this.transfer.fireTransferPaused();
         this.beginSheet();
         this.transfer.fireTransferResumed();
         return action;
     }
 
     // ----------------------------------------------------------
     // Outlets
     // ----------------------------------------------------------
 
     private static final NSAttributedString UNKNOWN_STRING = new NSAttributedString(
             NSBundle.localizedString("Unknown", ""),
             TRUNCATE_MIDDLE_ATTRIBUTES);
 
     /**
      * A browsable listing of duplicate files and folders
      */
     protected NSOutlineView browserView; // IBOutlet
     protected CDTransferPromptModel browserModel;
     protected CDTableDelegate browserViewDelegate;
 
     public void setBrowserView(final NSOutlineView view) {
         this.browserView = view;
         this.browserView.setHeaderView(null);
         this.browserView.setDataSource(this.browserModel);
         this.browserView.setDelegate(this.browserViewDelegate = new CDAbstractTableDelegate() {
 
             /**
              * @see NSOutlineView.Delegate
              */
             public String outlineViewToolTipForCell(NSOutlineView view, NSCell cell, NSMutableRect rect, NSTableColumn tableColumn,
                                                     Path item, NSPoint mouseLocation) {
                 return this.tooltipForPath(item);
             }
 
             public void enterKeyPressed(final Object sender) {
                 ;
             }
 
             public void deleteKeyPressed(final Object sender) {
                 ;
             }
 
             public void tableColumnClicked(NSTableView view, NSTableColumn tableColumn) {
                 ;
             }
 
             public void tableRowDoubleClicked(final Object sender) {
                 ;
             }
 
             public void selectionDidChange(NSNotification notification) {
                 if(browserView.selectedRow() != -1) {
                     Path p = (Path) browserView.itemAtRow(browserView.selectedRow());
                     localURLField.setAttributedStringValue(new NSAttributedString(
                             p.getLocal().getAbsolute(),
                             TRUNCATE_MIDDLE_ATTRIBUTES));
                     localURLField.setHidden(false);
 
                     if(transfer.exists(p.getLocal())) {
                         if(p.getLocal().attributes.getSize() == -1) {
                             localSizeField.setAttributedStringValue(UNKNOWN_STRING);
                         }
                         else {
                             localSizeField.setAttributedStringValue(new NSAttributedString(
                                     Status.getSizeAsString(p.getLocal().attributes.getSize()),
                                     TRUNCATE_MIDDLE_ATTRIBUTES));
                         }
                         localSizeField.setHidden(false);
                         if(p.getLocal().attributes.getModificationDate() == -1) {
                             localModificationField.setAttributedStringValue(UNKNOWN_STRING);
                         }
                         else {
                             localModificationField.setAttributedStringValue(new NSAttributedString(
                                     CDDateFormatter.getLongFormat(p.getLocal().attributes.getModificationDate(),
                                             p.getHost().getTimezone()),
                                     TRUNCATE_MIDDLE_ATTRIBUTES));
                         }
                         localModificationField.setHidden(false);
                     }
                     else {
                         localSizeField.setHidden(true);
                         localModificationField.setHidden(true);
                     }
 
                     remoteURLField.setAttributedStringValue(new NSAttributedString(
                             p.getHost().toURL() + p.getAbsolute(),
                             TRUNCATE_MIDDLE_ATTRIBUTES));
                     remoteURLField.setHidden(false);
 
                     if(transfer.exists(p)) {
                         if(p.attributes.getSize() == -1) {
                             remoteSizeField.setAttributedStringValue(UNKNOWN_STRING);
                         }
                         else {
                             remoteSizeField.setAttributedStringValue(new NSAttributedString(
                                     Status.getSizeAsString(p.attributes.getSize()),
                                     TRUNCATE_MIDDLE_ATTRIBUTES));
                         }
                         remoteSizeField.setHidden(false);
                         if(p.attributes.getModificationDate() == -1) {
                             remoteModificationField.setAttributedStringValue(UNKNOWN_STRING);
                         }
                         else {
                             remoteModificationField.setAttributedStringValue(new NSAttributedString(
                                     CDDateFormatter.getLongFormat(p.attributes.getModificationDate(),
                                             p.getHost().getTimezone()),
                                     TRUNCATE_MIDDLE_ATTRIBUTES));
                         }
                         remoteModificationField.setHidden(false);
                     }
                     else {
                         remoteSizeField.setHidden(true);
                         remoteModificationField.setHidden(true);
                     }
                 }
                 else {
                     hideLocalDetails(true);
                     hideRemoteDetails(true);
                 }
             }
 
             private void hideRemoteDetails(boolean hidden) {
                 remoteURLField.setHidden(hidden);
                 remoteSizeField.setHidden(hidden);
                 remoteModificationField.setHidden(hidden);
             }
 
             private void hideLocalDetails(boolean hidden) {
                 localURLField.setHidden(hidden);
                 localSizeField.setHidden(hidden);
                 localModificationField.setHidden(hidden);
             }
 
             /**
              * @see NSOutlineView.Delegate
              */
             public void outlineViewWillDisplayCell(NSOutlineView outlineView, NSCell cell,
                                                    NSTableColumn tableColumn, Path item) {
                 String identifier = (String) tableColumn.identifier();
                 if(item != null) {
                     if(identifier.equals(CDTransferPromptModel.INCLUDE_COLUMN)) {
                         cell.setEnabled(transfer.isSelectable(item));
                     }
                     if(identifier.equals(CDTransferPromptModel.FILENAME_COLUMN)) {
                         ((CDOutlineCell) cell).setIcon(CDIconCache.instance().iconForPath(item, 16));
                     }
                     if(cell instanceof NSTextFieldCell) {
                         if(!transfer.isIncluded(item)) {
                             ((NSTextFieldCell) cell).setTextColor(NSColor.disabledControlTextColor());
                         }
                         else {
                             ((NSTextFieldCell) cell).setTextColor(NSColor.controlTextColor());
                         }
                     }
                 }
             }
         });
         this.browserView.setRowHeight(17f);
         // selection properties
         this.browserView.setAllowsMultipleSelection(true);
         this.browserView.setAllowsEmptySelection(true);
         this.browserView.setAllowsColumnResizing(true);
         this.browserView.setAllowsColumnSelection(false);
         this.browserView.setAllowsColumnReordering(true);
         this.browserView.setUsesAlternatingRowBackgroundColors(Preferences.instance().getBoolean("browser.alternatingRows"));
         if(Preferences.instance().getBoolean("browser.horizontalLines") && Preferences.instance().getBoolean("browser.verticalLines")) {
             this.browserView.setGridStyleMask(NSTableView.SolidHorizontalGridLineMask | NSTableView.SolidVerticalGridLineMask);
         }
         else if(Preferences.instance().getBoolean("browser.verticalLines")) {
             this.browserView.setGridStyleMask(NSTableView.SolidVerticalGridLineMask);
         }
         else if(Preferences.instance().getBoolean("browser.horizontalLines")) {
             this.browserView.setGridStyleMask(NSTableView.SolidHorizontalGridLineMask);
         }
         else {
             this.browserView.setGridStyleMask(NSTableView.GridNone);
         }
         NSSelector setResizableMaskSelector
                 = new NSSelector("setResizingMask", new Class[]{int.class});
         {
             NSTableColumn c = new NSTableColumn();
             c.headerCell().setStringValue(NSBundle.localizedString("Filename", "A column in the browser"));
             c.setIdentifier(CDTransferPromptModel.FILENAME_COLUMN);
             c.setMinWidth(100f);
             c.setWidth(220f);
             c.setMaxWidth(800f);
             if(setResizableMaskSelector.implementedByClass(NSTableColumn.class)) {
                 c.setResizingMask(NSTableColumn.AutoresizingMask | NSTableColumn.UserResizingMask);
             }
             else {
                 c.setResizable(true);
             }
             c.setEditable(false);
             c.setDataCell(new CDOutlineCell());
             this.browserView.addTableColumn(c);
             this.browserView.setOutlineTableColumn(c);
         }
         {
             NSTableColumn c = new NSTableColumn();
             c.setIdentifier(CDTransferPromptModel.SIZE_COLUMN);
             c.headerCell().setStringValue("");
             c.setMinWidth(50f);
             c.setWidth(80f);
             c.setMaxWidth(100f);
             if(setResizableMaskSelector.implementedByClass(NSTableColumn.class)) {
                 c.setResizingMask(NSTableColumn.AutoresizingMask | NSTableColumn.UserResizingMask);
             }
             else {
                 c.setResizable(true);
             }
             c.setEditable(false);
             c.setDataCell(new NSTextFieldCell());
             this.browserView.addTableColumn(c);
         }
         {
             NSTableColumn c = new NSTableColumn();
             c.setIdentifier(CDTransferPromptModel.WARNING_COLUMN);
             c.headerCell().setStringValue("");
             c.setMinWidth(20f);
             c.setWidth(20f);
             c.setMaxWidth(20f);
             if(setResizableMaskSelector.implementedByClass(NSTableColumn.class)) {
                 c.setResizingMask(NSTableColumn.AutoresizingMask);
             }
             else {
                 c.setResizable(true);
             }
             c.setEditable(false);
             c.setDataCell(new NSImageCell());
             c.dataCell().setAlignment(NSText.CenterTextAlignment);
             this.browserView.addTableColumn(c);
         }
         {
             NSTableColumn c = new NSTableColumn();
             c.setIdentifier(CDTransferPromptModel.INCLUDE_COLUMN);
             c.headerCell().setStringValue("");
             c.setMinWidth(20f);
             c.setWidth(20f);
             c.setMaxWidth(20f);
             if(setResizableMaskSelector.implementedByClass(NSTableColumn.class)) {
                 c.setResizingMask(NSTableColumn.AutoresizingMask);
             }
             else {
                 c.setResizable(true);
             }
             c.setEditable(false);
             NSButtonCell cell = new NSButtonCell();
             cell.setTitle("");
             cell.setControlSize(NSCell.SmallControlSize);
             cell.setButtonType(NSButtonCell.SwitchButton);
             cell.setAllowsMixedState(false);
             cell.setTarget(this);
             c.setDataCell(cell);
             c.dataCell().setAlignment(NSText.CenterTextAlignment);
             this.browserView.addTableColumn(c);
         }
         this.browserView.sizeToFit();
     }
 
     private NSTextField remoteURLField; // IBOutlet
 
     public void setRemoteURLField(final NSTextField f) {
         this.remoteURLField = f;
         this.remoteURLField.setHidden(true);
     }
 
     private NSTextField remoteSizeField; // IBOutlet
 
     public void setRemoteSizeField(final NSTextField f) {
         this.remoteSizeField = f;
         this.remoteSizeField.setHidden(true);
     }
 
     private NSTextField remoteModificationField; // IBOutlet
 
     public void setRemoteModificationField(final NSTextField f) {
         this.remoteModificationField = f;
         this.remoteModificationField.setHidden(true);
     }
 
     private NSTextField localURLField; // IBOutlet
 
     public void setLocalURLField(final NSTextField f) {
         this.localURLField = f;
         this.localURLField.setHidden(true);
     }
 
     private NSTextField localSizeField; // IBOutlet
 
     public void setLocalSizeField(final NSTextField f) {
         this.localSizeField = f;
         this.localSizeField.setHidden(true);
     }
 
     private NSTextField localModificationField; // IBOutlet
 
     public void setLocalModificationField(final NSTextField f) {
         this.localModificationField = f;
         this.localModificationField.setHidden(true);
     }
 
     private NSProgressIndicator statusIndicator; // IBOutlet
 
     public void setStatusIndicator(final NSProgressIndicator f) {
         this.statusIndicator = f;
         this.statusIndicator.setDisplayedWhenStopped(false);
     }
 
     private NSTextField statusLabel; // IBOutlet
 
     public void setStatusLabel(final NSTextField f) {
         this.statusLabel = f;
     }
 
     protected NSPopUpButton actionPopup; // IBOutlet
 
     public void setActionPopup(final NSPopUpButton actionPopup) {
         this.actionPopup = actionPopup;
         this.actionPopup.removeAllItems();
 
         final TransferAction defaultAction
                 = TransferAction.forName(Preferences.instance().getProperty("queue.prompt.action.default"));
 
         final TransferAction[] actions = new TransferAction[]{
                 transfer.isResumable() ? TransferAction.ACTION_RESUME : null,
                 TransferAction.ACTION_OVERWRITE,
                 TransferAction.ACTION_RENAME,
                 TransferAction.ACTION_SKIP};
 
         for(int i = 0; i < actions.length; i++) {
             if(null == actions[i]) {
                 continue; //Not resumeable
             }
             this.actionPopup.addItem(actions[i].getLocalizableString());
             this.actionPopup.lastItem().setRepresentedObject(actions[i]);
             if(actions[i].equals(defaultAction)) {
                 this.actionPopup.selectItem(actionPopup.lastItem());
             }
         }
        this.action = (TransferAction)this.actionPopup.selectedItem().representedObject();
         this.actionPopup.setTarget(this);
         this.actionPopup.setAction(new NSSelector("actionPopupClicked", new Class[]{NSPopUpButton.class}));
     }
 
     public void actionPopupClicked(NSPopUpButton sender) {
         final TransferAction selected = (TransferAction) sender.selectedItem().representedObject();
 
         if(this.action.equals(selected)) {
             return;
         }
 
         Preferences.instance().setProperty("queue.prompt.action.default", selected.toString());
 
         this.action = selected;
         this.browserView.reloadData();
     }
 }
