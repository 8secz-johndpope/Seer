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
 
 import org.apache.log4j.Logger;
 
 import java.io.IOException;
 import java.util.Iterator;
 import java.util.List;
 
 /**
  * @version $Id$
  */
 public class CDBookmarkTableDataSource extends CDController {
     private static Logger log = Logger.getLogger(CDBookmarkTableDataSource.class);
 
     public static final String ICON_COLUMN = "ICON";
     public static final String BOOKMARK_COLUMN = "BOOKMARK";
     public static final String STATUS_COLUMN = "STATUS";
     // virtual column to implement keyboard selection
     protected static final String TYPEAHEAD_COLUMN = "TYPEAHEAD";
 
     protected CDBrowserController controller;
 
     public CDBookmarkTableDataSource(CDBrowserController controller, BookmarkCollection source) {
         this.controller = controller;
         this.source = source;
     }
 
     private BookmarkCollection source;
 
     public void setSource(final BookmarkCollection source) {
         this.source = source;
         this.setFilter(null);
     }
 
     private HostFilter filter;
 
     /**
      * Display only a subset of all bookmarks
      *
      * @param filter
      */
     public void setFilter(HostFilter filter) {
         this.filter = filter;
         this.filtered = null;
     }
 
     /**
      * Subset of the original source
      */
     private BookmarkCollection filtered;
 
     /**
      * @return The filtered collection currently to be displayed within the constraints
      *         given by the comparision with the HostFilter
      * @see HostFilter
      */
     protected BookmarkCollection getSource() {
         if(null == filter) {
             return source;
         }
         if(null == filtered) {
             filtered = new BookmarkCollection() {
                 public boolean allowsAdd() {
                     return source.allowsAdd();
                 }
 
                 public boolean allowsDelete() {
                     return source.allowsDelete();
                 }
 
                 public boolean allowsEdit() {
                     return source.allowsEdit();
                 }
             };
             for(Iterator<Host> i = source.iterator(); i.hasNext();) {
                 final Host bookmark = i.next();
                 if(filter.accept(bookmark)) {
                     filtered.add(bookmark);
                 }
             }
             filtered.addListener(new AbstractCollectionListener<Host>() {
                 public void collectionItemAdded(Host item) {
                     source.add(item);
                 }
 
                 public void collectionItemRemoved(Host item) {
                     source.remove(item);
                 }
             });
         }
         return filtered;
     }
 
     /**
      * @see NSTableView.DataSource
      */
     public int numberOfRowsInTableView(NSTableView view) {
         return this.getSource().size();
     }
 
     /**
      * @see NSTableView.DataSource
      */
     public Object tableViewObjectValueForLocation(NSTableView view, NSTableColumn tableColumn, int row) {
         if(row < this.numberOfRowsInTableView(view)) {
             final String identifier = (String) tableColumn.identifier();
             final Host host = this.getSource().get(row);
             if(identifier.equals(ICON_COLUMN)) {
                 return CDIconCache.instance().iconForName(host.getProtocol().disk(),
                         Preferences.instance().getInteger("bookmark.icon.size"));
             }
             if(identifier.equals(BOOKMARK_COLUMN)) {
                 return host;
             }
             if(identifier.equals(STATUS_COLUMN)) {
                 if(controller.hasSession()) {
                     final Session session = controller.getSession();
                     if(host.equals(session.getHost())) {
                         if(session.isConnected()) {
                             return NSImage.imageNamed("statusGreen.tiff");
                         }
                         if(session.isOpening()) {
                             return NSImage.imageNamed("statusYellow.tiff");
                         }
                     }
                 }
                 return null;
             }
             if(identifier.equals(TYPEAHEAD_COLUMN)) {
                 return host.getNickname();
             }
             throw new IllegalArgumentException("Unknown identifier: " + identifier);
         }
         log.warn("tableViewObjectValueForLocation:" + row + " == null");
         return null;
     }
 
     /**
      * @see NSTableView.DataSource
      */
     public int tableViewValidateDrop(NSTableView view, NSDraggingInfo info, int index, int operation) {
         if(!this.getSource().allowsEdit()) {
             // Do not allow drags for non writable collections
             return NSDraggingInfo.DragOperationNone;
         }
         if(info.draggingPasteboard().availableTypeFromArray(new NSArray(NSPasteboard.StringPboardType)) != null) {
             Object o = info.draggingPasteboard().propertyListForType(NSPasteboard.StringPboardType);
             if(o != null) {
                 if(Protocol.isURL(o.toString())) {
                     view.setDropRowAndDropOperation(index, NSTableView.DropAbove);
                     return NSDraggingInfo.DragOperationCopy;
                 }
             }
             return NSDraggingInfo.DragOperationNone;
         }
         if(info.draggingPasteboard().availableTypeFromArray(new NSArray(NSPasteboard.FilenamesPboardType)) != null) {
             Object o = info.draggingPasteboard().propertyListForType(NSPasteboard.FilenamesPboardType);
             if(o != null) {
                 NSArray elements = (NSArray) o;
                 for(int i = 0; i < elements.count(); i++) {
                     String file = (String) elements.objectAtIndex(i);
                     if(file.indexOf(".duck") != -1) {
                         //allow file drags if bookmark file even if list is empty
                         return NSDraggingInfo.DragOperationCopy;
                     }
                 }
                 if(index > -1 && index < view.numberOfRows()) {
                     //only allow other files if there is at least one bookmark
                     view.setDropRowAndDropOperation(index, NSTableView.DropOn);
                     return NSDraggingInfo.DragOperationCopy;
                 }
             }
         }
         if(info.draggingPasteboard().availableTypeFromArray(new NSArray(NSPasteboard.FilesPromisePboardType)) != null) {
             if(index > -1 && index < view.numberOfRows()) {
                 view.setDropRowAndDropOperation(index, NSTableView.DropAbove);
                 // We accept any file promise within the bounds
                 return NSDraggingInfo.DragOperationMove;
             }
         }
         return NSDraggingInfo.DragOperationNone;
     }
 
     /**
      * @param info contains details on this dragging operation.
      * @param row  The proposed location is row and action is operation.
      *             The data source should incorporate the data from the dragging pasteboard at this time.
      * @see NSTableView.DataSource
      *      Invoked by view when the mouse button is released over a table view that previously decided to allow a drop.
      */
     public boolean tableViewAcceptDrop(NSTableView view, NSDraggingInfo info, int row, int operation) {
         log.debug("tableViewAcceptDrop:" + row);
        final BookmarkCollection source = this.getSource();
         if(info.draggingPasteboard().availableTypeFromArray(new NSArray(NSPasteboard.StringPboardType)) != null) {
             Object o = info.draggingPasteboard().propertyListForType(NSPasteboard.StringPboardType);
             if(o != null) {
                 final Host h = Host.parse(o.toString());
                 source.add(row, h);
                 view.selectRow(row, false);
                 view.scrollRowToVisible(row);
                 return true;
             }
             return false;
         }
         if(info.draggingPasteboard().availableTypeFromArray(
                 new NSArray(NSPasteboard.FilenamesPboardType)) != null) {
             // We get a drag from another application e.g. Finder.app proposing some files
             NSArray filesList = (NSArray) info.draggingPasteboard().propertyListForType(
                     NSPasteboard.FilenamesPboardType);// get the filenames from pasteboard
             // If regular files are dropped, these will be uploaded to the dropped bookmark location
             final List<Path> roots = new Collection<Path>();
             Session session = null;
             for(int i = 0; i < filesList.count(); i++) {
                 String filename = (String) filesList.objectAtIndex(i);
                 if(filename.endsWith(".duck")) {
                     // Adding a previously exported bookmark file from the Finder
                     if(row < 0) {
                         row = 0;
                     }
                     if(row > view.numberOfRows()) {
                         row = view.numberOfRows();
                     }
                     try {
                         source.add(row, new Host(new Local(filename)));
                         view.selectRow(row, false);
                         view.scrollRowToVisible(row);
                     }
                     catch(IOException e) {
                         log.error(e.getMessage());
                         return false;
                     }
                 }
                 else {
                     // The bookmark this file has been dropped onto
                    Host h = source.get(row);
                     if(null == session) {
                         session = SessionFactory.createSession(h);
                     }
                     // Upload to the remote host this bookmark points to
                     roots.add(PathFactory.createPath(session, h.getDefaultPath(), new Local(filename)));
                 }
             }
             if(!roots.isEmpty()) {
                 final Transfer q = new UploadTransfer(roots);
                 // If anything has been added to the queue, then process the queue
                 if(q.numberOfRoots() > 0) {
                     CDTransferController.instance().startTransfer(q);
                 }
             }
             return true;
         }
         if(info.draggingPasteboard().availableTypeFromArray(
                 new NSArray(NSPasteboard.FilesPromisePboardType)) != null) {
             for(int i = 0; i < promisedDragBookmarks.length; i++) {
                 source.remove(source.indexOf(promisedDragBookmarks[i]));
                 source.add(row, promisedDragBookmarks[i]);
                 view.selectRow(row, false);
                 view.scrollRowToVisible(row);
             }
             return true;
         }
         return false;
     }
 
     /**
      * @see NSDraggingSource
      * @see "http://www.cocoabuilder.com/archive/message/2005/10/5/118857"
      */
     public void finishedDraggingImage(NSImage image, NSPoint point, int operation) {
         log.debug("finishedDraggingImage:" + operation);
         if(NSDraggingInfo.DragOperationDelete == operation) {
             controller.deleteBookmarkButtonClicked(null);
         }
         NSPasteboard.pasteboardWithName(NSPasteboard.DragPboard).declareTypes(null, null);
     }
 
     /**
      * @param local
      * @return
      * @see NSDraggingSource
      */
     public int draggingSourceOperationMaskForLocal(boolean local) {
         log.debug("draggingSourceOperationMaskForLocal:" + local);
         if(local) {
             return NSDraggingInfo.DragOperationMove | NSDraggingInfo.DragOperationCopy;
         }
         return NSDraggingInfo.DragOperationCopy | NSDraggingInfo.DragOperationDelete;
     }
 
     /**
      * The files dragged from the favorits drawer to the Finder --> bookmark files
      */
     private Host[] promisedDragBookmarks;
 
     /**
      * @param rows is the list of row numbers that will be participating in the drag.
      * @return To refuse the drag, return false. To start a drag, return true and place
      *         the drag data onto pboard (data, owner, and so on).
      * @see NSTableView.DataSource
      *      Invoked by view after it has been determined that a drag should begin, but before the drag has been started.
      *      The drag image and other drag-related information will be set up and provided by the table view once this call
      *      returns with true.
      */
     public boolean tableViewWriteRowsToPasteboard(NSTableView view, NSArray rows, NSPasteboard pboard) {
         log.debug("tableViewWriteRowsToPasteboard:" + rows);
         if(rows.count() > 0) {
             this.promisedDragBookmarks = new Host[rows.count()];
             for(int i = 0; i < rows.count(); i++) {
                 promisedDragBookmarks[i] =
                        new Host(this.getSource().get(((Number) rows.objectAtIndex(i)).intValue()).getAsDictionary());
             }
             NSEvent event = NSApplication.sharedApplication().currentEvent();
             if(event != null) {
                 NSPoint dragPosition = view.convertPointFromView(event.locationInWindow(), null);
                 NSRect imageRect = new NSRect(new NSPoint(dragPosition.x() - 16, dragPosition.y() - 16), new NSSize(32, 32));
                 // Writing a promised file of the host as a bookmark file to the clipboard
                 view.dragPromisedFilesOfTypes(new NSArray("duck"), imageRect, this, true, event);
                 return true;
             }
         }
         return false;
     }
 
     /**
      * @return the names (not full paths) of the files that the receiver promises to create at dropDestination.
      *         This method is invoked when the drop has been accepted by the destination and the destination,
      *         in the case of another Cocoa application, invokes the NSDraggingInfo method
      *         namesOfPromisedFilesDroppedAtDestination.
      *         For long operations, you can cache dropDestination and defer the creation of the files until the
      *         finishedDraggingImage method to avoid blocking the destination application.
      * @see NSTableView.DataSource
      */
     public NSArray namesOfPromisedFilesDroppedAtDestination(java.net.URL dropDestination) {
         log.debug("namesOfPromisedFilesDroppedAtDestination:" + dropDestination);
         final NSMutableArray promisedDragNames = new NSMutableArray();
         try {
             for(int i = 0; i < promisedDragBookmarks.length; i++) {
                 // utf-8 is just a wild guess
                 Local file = new Local(java.net.URLDecoder.decode(dropDestination.getPath(), "utf-8"),
                         promisedDragBookmarks[i].getNickname() + ".duck");
                 promisedDragBookmarks[i].setFile(file);
                 promisedDragBookmarks[i].write();
                 // Adding the filename that is promised to be created at the dropDestination
                 promisedDragNames.addObject(file.getName());
             }
         }
         catch(java.io.UnsupportedEncodingException e) {
             log.error(e.getMessage());
         }
         catch(IOException e) {
             log.error(e.getMessage());
         }
         return promisedDragNames;
     }
 }
