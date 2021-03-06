 package ch.cyberduck.core;
 
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
 
 import glguerin.io.FileForker;
 import glguerin.io.Pathname;
 import glguerin.io.imp.mac.macosx.MacOSXForker;
 
 import com.apple.cocoa.application.NSWorkspace;
 import com.apple.cocoa.foundation.*;
 
 import org.apache.log4j.Logger;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Calendar;
 import java.util.Date;
 import java.util.TimeZone;
 
 /**
  * @version $Id$
  */
 public class Local extends File {
     private static Logger log = Logger.getLogger(Local.class);
 
     static {
         try {
             NSBundle bundle = NSBundle.mainBundle();
             String lib = bundle.resourcePath() + "/Java/" + "libLocal.dylib";
             log.info("Locating libLocal.dylib at '" + lib + "'");
             System.load(lib);
         }
         catch (UnsatisfiedLinkError e) {
             log.error("Could not load the alias resolving library:" + e.getMessage());
         }
     }
 
     public Local(File parent, String name) {
         super(NSPathUtilities.stringByExpandingTildeInPath(parent.getAbsolutePath()), name);
     }
 
     public Local(String parent, String name) {
         super(NSPathUtilities.stringByExpandingTildeInPath(parent), name);
     }
 
     public Local(String path) {
         super(NSPathUtilities.stringByExpandingTildeInPath(path));
     }
 
     public boolean createNewFile() throws IOException {
         if (super.createNewFile()) {
             this.setProgress(0);
         }
         return false;
     }
 
     /**
      * @return the extension if any or null
      */
     public String getExtension() {
         String name = this.getName();
         int index = name.lastIndexOf(".");
         if (index != -1) {
             return name.substring(index + 1, name.length());
         }
         return null;
     }
 
     public String getAbsolute() {
         return super.getAbsolutePath();
     }
 
 	private Object lock = new Object();
 		
     public void setProgress(int progress) {
         if (Preferences.instance().getBoolean("queue.download.updateIcon")) {
 			synchronized(lock) {
 	            if (-1 == progress) {
 	                this.removeResourceFork();
 	            }
 	            else {
 	                this.setIconFromFile(this.getAbsolute(), "download" + progress + ".icns");
 	            }
 	        }
 		}
 		NSWorkspace.sharedWorkspace().noteFileSystemChangedAtPath(this.getAbsolute());
     }
 
     private void removeResourceFork() {
         try {
             this.removeCustomIcon();
             FileForker forker = new MacOSXForker();
             forker.usePathname(new Pathname(this.getAbsoluteFile()));
             forker.makeForkOutputStream(true, false).close();
         }
         catch (IOException e) {
             log.error("Failed to remove resource fork from file:" + e.getMessage());
         }
     }
 
     private void setIconFromFile(String icon) {
         this.setIconFromFile(this.getAbsolute(), icon);
     }
 
     /**
      * @param icon the absolute path to the image file to use as an icon
      */
     private native void setIconFromFile(String path, String icon);
 
     private void removeCustomIcon() {
         this.removeCustomIcon(this.getAbsolute());
     }
 
     private native void removeCustomIcon(String path);
 
     public Permission getPermission() {
         NSDictionary fileAttributes = NSPathUtilities.fileAttributes(this.getAbsolutePath(), true);
         return new Permission(((Integer) fileAttributes.objectForKey(NSPathUtilities.FilePosixPermissions)).intValue());
     }
 
     public void setPermission(Permission p) {
         boolean success = NSPathUtilities.setFileAttributes(this.getAbsolutePath(),
                 new NSDictionary(new Integer(p.getDecimalCode()),
                         NSPathUtilities.FilePosixPermissions));
         log.debug("Setting permissions on local file suceeded:" + success);
     }
 
     /**
      *
      * @return The timestamp as a calendar with the current timezone
      */
     public Calendar getTimestampAsCalendar() {
         Calendar c = Calendar.getInstance(TimeZone.getDefault());
         c.setTime(this.getTimestamp());
         if (Preferences.instance().getBoolean("queue.sync.ignore.millisecond"))
             c.clear(Calendar.MILLISECOND);
         if (Preferences.instance().getBoolean("queue.sync.ignore.second"))
             c.clear(Calendar.SECOND);
         if (Preferences.instance().getBoolean("queue.sync.ignore.minute"))
             c.clear(Calendar.MINUTE);
         if (Preferences.instance().getBoolean("queue.sync.ignore.hour"))
             c.clear(Calendar.HOUR);
         return c;
     }
 
     private static final NSGregorianDateFormatter longDateFormatter
             = new NSGregorianDateFormatter((String) NSUserDefaults.standardUserDefaults().objectForKey(
             NSUserDefaults.TimeDateFormatString), false);
     private static final NSGregorianDateFormatter shortDateFormatter
             = new NSGregorianDateFormatter((String) NSUserDefaults.standardUserDefaults().objectForKey(
             NSUserDefaults.ShortTimeDateFormatString), false);
 
     /**
      * Modification date represented as NSUserDefaults.TimeDateFormatString
      * @return the modification date of this file or null if there is a problem converting the time to a string
      */
     public String getTimestampAsString() {
         try {
             return longDateFormatter.stringForObjectValue(
                     new NSGregorianDate((double) this.getTimestamp().getTime() / 1000, NSDate.DateFor1970));
         }
         catch (NSFormatter.FormattingException e) {
             e.printStackTrace();
         }
         return null;
     }
 
     /**
      * Modification date represented as NSUserDefaults.ShortTimeDateFormatString
      * @return The teimestamp as a short string or null if there is a problem convertign the time to a string
      */
     public String getTimestampAsShortString() {
         try {
             return shortDateFormatter.stringForObjectValue(
                     new NSGregorianDate((double) this.getTimestamp().getTime() / 1000, NSDate.DateFor1970));
         }
         catch (NSFormatter.FormattingException e) {
             e.printStackTrace();
         }
         return null;
     }
 
     /**
      *
      * @return
      */
     public Date getTimestamp() {
         return new Date(super.lastModified());
     }
 
     public long getSize() {
         if (this.isDirectory()) {
             return 0;
         }
         return super.length();
     }
 
     public int hashCode() {
         return this.getAbsolutePath().hashCode();
     }
 
     public boolean equals(Object other) {
         if (other instanceof Local) {
            return this.getAbsolutePath().equals(((Local) other).getAbsolutePath());// && this.attributes.getTimestamp().equals(((Local)other).attributes.getTimestamp());
         }
         return false;
     }
 }
