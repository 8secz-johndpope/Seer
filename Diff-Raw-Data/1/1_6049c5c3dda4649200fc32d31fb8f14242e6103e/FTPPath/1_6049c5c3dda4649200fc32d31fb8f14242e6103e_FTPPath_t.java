 package ch.cyberduck.core.ftp;
 
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
 
 import ch.cyberduck.core.*;
 import ch.cyberduck.core.i18n.Locale;
 import ch.cyberduck.core.io.*;
 import ch.cyberduck.ui.DateFormatterFactory;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.net.ftp.FTPFile;
 import org.apache.commons.net.ftp.FTPFileEntryParser;
 import org.apache.log4j.Logger;
 
 import com.enterprisedt.net.ftp.FTPException;
 import com.enterprisedt.net.ftp.FTPTransferType;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.text.MessageFormat;
 import java.util.Calendar;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * @version $Id$
  */
 public class FTPPath extends Path {
     private static Logger log = Logger.getLogger(FTPPath.class);
 
     private static final String DOS_LINE_SEPARATOR = "\r\n";
     private static final String MAC_LINE_SEPARATOR = "\r";
     private static final String UNIX_LINE_SEPARATOR = "\n";
 
     private static class Factory extends PathFactory<FTPSession> {
         @Override
         protected Path create(FTPSession session, String path, int type) {
             return new FTPPath(session, path, type);
         }
 
         @Override
         protected Path create(FTPSession session, String parent, String name, int type) {
             return new FTPPath(session, parent, name, type);
         }
 
         @Override
         protected Path create(FTPSession session, String parent, Local file) {
             return new FTPPath(session, parent, file);
         }
 
         @Override
         protected <T> Path create(FTPSession session, T dict) {
             return new FTPPath(session, dict);
         }
     }
 
     public static PathFactory factory() {
         return new Factory();
     }
 
     private final FTPSession session;
 
     protected FTPPath(FTPSession s, String parent, String name, int type) {
         super(parent, name, type);
         this.session = s;
     }
 
     protected FTPPath(FTPSession s, String path, int type) {
         super(path, type);
         this.session = s;
     }
 
     protected FTPPath(FTPSession s, String parent, Local file) {
         super(parent, file);
         this.session = s;
     }
 
     protected <T> FTPPath(FTPSession s, T dict) {
         super(dict);
         this.session = s;
     }
 
     @Override
     public FTPSession getSession() {
         return session;
     }
 
     @Override
     public AttributedList<Path> list() {
         final AttributedList<Path> children = new AttributedList<Path>();
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Listing directory {0}", "Status"),
                     this.getName()));
 
             this.getSession().setWorkdir(this);
             // Cached file parser determined from SYST response with the timezone set from the bookmark
             final FTPFileEntryParser parser = this.getSession().getFileParser();
             boolean success = this.parse(children, parser, this.getSession().getClient().stat(this.getAbsolute()));
             if(!success || children.isEmpty()) {
                 // STAT listing failed or empty
                 // Set transfer type for traditional data socket file listings
                 this.getSession().getClient().setTransferType(FTPTransferType.ASCII);
                 final BufferedReader mlsd = this.getSession().getClient().mlsd(this.getSession().getEncoding());
                 success = this.parse(children, mlsd);
                 // MLSD listing failed
                 if(null != mlsd) {
                     // Close MLSD data socket
                     this.getSession().getClient().finishDir();
                 }
                 if(!success) {
                     final BufferedReader lsa = this.getSession().getClient().list(this.getSession().getEncoding(), true);
                     success = this.parse(children, parser, lsa);
                     if(null != lsa) {
                         // Close LIST data socket
                         this.getSession().getClient().finishDir();
                     }
                     if(!success) {
                         // LIST -a listing failed
                         final BufferedReader ls = this.getSession().getClient().list(this.getSession().getEncoding(), false);
                         success = this.parse(children, parser, ls);
                         if(null != ls) {
                             // Close LIST data socket
                             this.getSession().getClient().finishDir();
                         }
                         if(!success) {
                             // LIST listing failed
                             log.error("No compatible file listing method found");
                         }
                     }
                 }
             }
             for(Path child : children) {
                 if(child.attributes().getType() == Path.SYMBOLIC_LINK_TYPE) {
                     try {
                         this.getSession().setWorkdir(child);
                         child.attributes().setType(Path.SYMBOLIC_LINK_TYPE | Path.DIRECTORY_TYPE);
                     }
                     catch(FTPException e) {
                         child.attributes().setType(Path.SYMBOLIC_LINK_TYPE | Path.FILE_TYPE);
                     }
                 }
             }
         }
         catch(IOException e) {
             log.warn("Listing directory failed:" + e.getMessage());
             children.attributes().setReadable(false);
         }
         return children;
     }
 
     /**
      * The "facts" for a file in a reply to a MLSx command consist of
      * information about that file.  The facts are a series of keyword=value
      * pairs each followed by semi-colon (";") characters.  An individual
      * fact may not contain a semi-colon in its name or value.  The complete
      * series of facts may not contain the space character.  See the
      * definition or "RCHAR" in section 2.1 for a list of the characters
      * that can occur in a fact value.  Not all are applicable to all facts.
      * <p/>
      * A sample of a typical series of facts would be: (spread over two
      * lines for presentation here only)
      * <p/>
      * size=4161;lang=en-US;modify=19970214165800;create=19961001124534;
      * type=file;x.myfact=foo,bar;
      * <p/>
      * This document defines a standard set of facts as follows:
      * <p/>
      * size       -- Size in octets
      * modify     -- Last modification time
      * create     -- Creation time
      * type       -- Entry type
      * unique     -- Unique id of file/directory
      * perm       -- File permissions, whether read, write, execute is
      * allowed for the login id.
      * lang       -- Language of the file name per IANA [11] registry.
      * media-type -- MIME media-type of file contents per IANA registry.
      * charset    -- Character set per IANA registry (if not UTF-8)
      *
      * @param response
      * @return
      */
     protected Map<String, Map<String, String>> parseFacts(String[] response) {
         Map<String, Map<String, String>> files = new HashMap<String, Map<String, String>>();
         for(String line : response) {
             files.putAll(this.parseFacts(line));
         }
         return files;
     }
 
     /**
      * @param line
      */
     protected Map<String, Map<String, String>> parseFacts(String line) {
         final Pattern p = Pattern.compile("\\s?(\\S+\\=\\S+;)*\\s(.*)");
         final Matcher result = p.matcher(line);
         Map<String, Map<String, String>> file = new HashMap<String, Map<String, String>>();
         if(result.matches()) {
             final String filename = result.group(2);
             final Map<String, String> facts = new HashMap<String, String>();
             for(String fact : result.group(1).split(";")) {
                 if(fact.contains("=")) {
                     facts.put(fact.split("=")[0].toLowerCase(), fact.split("=")[1].toLowerCase());
                 }
             }
             file.put(filename, facts);
         }
         else {
             log.warn("No match for " + line);
         }
         return file;
     }
 
     /**
      * Parse response of MLSD
      *
      * @param children
      * @param reader
      * @return
      * @throws IOException
      */
     private boolean parse(final AttributedList<Path> children, BufferedReader reader)
             throws IOException {
 
         if(null == reader) {
             // This is an empty directory
             return false;
         }
         boolean success = false;
         String line;
         while((line = reader.readLine()) != null) {
             final Map<String, Map<String, String>> file = this.parseFacts(line);
             if(file.isEmpty()) {
                 continue;
             }
             success = true; // At least one entry successfully parsed
             for(String name : file.keySet()) {
                 final Path parsed = PathFactory.createPath(this.getSession(), this.getAbsolute(), name, Path.FILE_TYPE);
                 parsed.setParent(this);
                 //                * size       -- Size in octets
                 //                * modify     -- Last modification time
                 //                * create     -- Creation time
                 //                * type       -- Entry type
                 //                * unique     -- Unique id of file/directory
                 //                * perm       -- File permissions, whether read, write, execute is
                 //                * allowed for the login id.
                 //                * lang       -- Language of the file name per IANA [11] registry.
                 //                * media-type -- MIME media-type of file contents per IANA registry.
                 //                * charset    -- Character set per IANA registry (if not UTF-8)
                 for(Map<String, String> facts : file.values()) {
                     if(!facts.containsKey("type")) {
                         continue;
                     }
                     if("dir".equals(facts.get("type").toLowerCase())) {
                         parsed.attributes().setType(Path.DIRECTORY_TYPE);
                     }
                     else if("file".equals(facts.get("type").toLowerCase())) {
                         parsed.attributes().setType(Path.FILE_TYPE);
                     }
                     else {
                         log.warn("Unsupported type: " + line);
                         continue;
                     }
                     if(facts.containsKey("sizd")) {
                         parsed.attributes().setSize(Long.parseLong(facts.get("sizd")));
                     }
                     if(facts.containsKey("size")) {
                         parsed.attributes().setSize(Long.parseLong(facts.get("size")));
                     }
                     if(facts.containsKey("unix.uid")) {
                         parsed.attributes().setOwner(facts.get("unix.uid"));
                     }
                     if(facts.containsKey("unix.owner")) {
                         parsed.attributes().setOwner(facts.get("unix.owner"));
                     }
                     if(facts.containsKey("unix.gid")) {
                         parsed.attributes().setGroup(facts.get("unix.gid"));
                     }
                     if(facts.containsKey("unix.group")) {
                         parsed.attributes().setGroup(facts.get("unix.group"));
                     }
                     if(facts.containsKey("unix.mode")) {
                         parsed.attributes().setPermission(new Permission(Integer.parseInt(facts.get("unix.mode"))));
                     }
                     if(facts.containsKey("modify")) {
                         parsed.attributes().setModificationDate(this.getSession().getClient().parseTimestamp(facts.get("modify")));
                     }
                     if(facts.containsKey("create")) {
                         parsed.attributes().setCreationDate(this.getSession().getClient().parseTimestamp(facts.get("create")));
                     }
                     children.add(parsed);
                 }
             }
         }
         return success;
     }
 
     /**
      * Parse all lines from the reader.
      *
      * @param parser
      * @param reader
      * @return An empty list if no parsable lines are found
      * @throws IOException
      */
     protected boolean parse(final AttributedList<Path> children, FTPFileEntryParser parser, BufferedReader reader)
             throws IOException {
         if(null == reader) {
             // This is an empty directory
             return false;
         }
         boolean success = false;
         String line;
         while((line = parser.readNextEntry(reader)) != null) {
             final FTPFile f = parser.parseFTPEntry(line);
             if(null == f) {
                 continue;
             }
             final String name = f.getName();
             if(!success) {
                 // Workaround for #2410. STAT only returns ls of directory itself
                 // Workaround for #2434. STAT of symbolic link directory only lists the directory itself.
                 if(this.getAbsolute().equals(name)) {
                     continue;
                 }
             }
             success = true; // At least one entry successfully parsed
             if(name.equals(".") || name.equals("..")) {
                 continue;
             }
             // The filename should never contain a delimiter
             final Path parsed = PathFactory.createPath(this.getSession(), this.getAbsolute(),
                     name.substring(name.lastIndexOf(DELIMITER) + 1), Path.FILE_TYPE);
             parsed.setParent(this);
             switch(f.getType()) {
                 case FTPFile.SYMBOLIC_LINK_TYPE:
                     parsed.setSymlinkTarget(this.getAbsolute(), f.getLink());
                     parsed.attributes().setType(Path.SYMBOLIC_LINK_TYPE);
                     break;
                 case FTPFile.DIRECTORY_TYPE:
                     parsed.attributes().setType(Path.DIRECTORY_TYPE);
                     break;
             }
             parsed.attributes().setSize(f.getSize());
             parsed.attributes().setOwner(f.getUser());
             parsed.attributes().setGroup(f.getGroup());
             if(this.getSession().isPermissionSupported(parser)) {
                 parsed.attributes().setPermission(new Permission(
                         new boolean[][]{
                                 {f.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION),
                                         f.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION),
                                         f.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION)
                                 },
                                 {f.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION),
                                         f.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION),
                                         f.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION)
                                 },
                                 {f.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION),
                                         f.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION),
                                         f.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION)
                                 }
                         }
                 ));
             }
             final Calendar timestamp = f.getTimestamp();
             if(timestamp != null) {
                 parsed.attributes().setModificationDate(timestamp.getTimeInMillis());
             }
             children.add(parsed);
         }
         return success;
     }
 
     @Override
     public void mkdir() {
         if(this.attributes().isDirectory()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Making directory {0}", "Status"),
                         this.getName()));
 
                 this.getSession().getClient().mkdir(this.getAbsolute());
                 if(Preferences.instance().getBoolean("queue.upload.changePermissions")) {
                     if(Preferences.instance().getBoolean("queue.upload.permissions.useDefault")) {
                         this.writeUnixPermissionImpl(new Permission(
                                 Preferences.instance().getInteger("queue.upload.permissions.folder.default")), false);
                     }
                 }
                 this.cache().put(this.getReference(), AttributedList.<Path>emptyList());
                 // The directory listing is no more current
                 this.getParent().invalidate();
             }
             catch(IOException e) {
                 this.error("Cannot create folder", e);
             }
         }
     }
 
     @Override
     public void rename(AbstractPath renamed) {
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Renaming {0} to {1}", "Status"),
                     this.getName(), renamed));
 
             this.getSession().getClient().rename(this.getAbsolute(), renamed.getAbsolute());
             // The directory listing is no more current
             renamed.getParent().invalidate();
             this.getParent().invalidate();
         }
         catch(IOException e) {
             if(attributes().isFile()) {
                 this.error("Cannot rename file", e);
             }
             if(attributes().isDirectory()) {
                 this.error("Cannot rename folder", e);
             }
         }
     }
 
     @Override
     public void readSize() {
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Getting size of {0}", "Status"),
                     this.getName()));
 
             if(attributes().isFile()) {
                 if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.AUTO.toString())) {
                     if(this.getTextFiletypePattern().matcher(this.getName()).matches()) {
                         this.getSession().getClient().setTransferType(FTPTransferType.ASCII);
                     }
                     else {
                         this.getSession().getClient().setTransferType(FTPTransferType.BINARY);
                     }
                 }
                 else if(Preferences.instance().getProperty("ftp.transfermode").equals(
                         FTPTransferType.BINARY.toString())) {
                     this.getSession().getClient().setTransferType(FTPTransferType.BINARY);
                 }
                 else if(Preferences.instance().getProperty("ftp.transfermode").equals(
                         FTPTransferType.ASCII.toString())) {
                     this.getSession().getClient().setTransferType(FTPTransferType.ASCII);
                 }
                 else {
                     throw new FTPException("Transfer type not set");
                 }
                 attributes().setSize(this.getSession().getClient().size(this.getAbsolute()));
             }
             if(-1 == attributes().getSize()) {
                 // Read the size from the directory listing
                 final AttributedList<AbstractPath> l = this.getParent().children();
                 if(l.contains(this.getReference())) {
                     attributes().setSize(l.get(this.getReference()).attributes().getSize());
                 }
             }
         }
         catch(IOException e) {
             this.error("Cannot read file attributes", e);
         }
     }
 
     @Override
     public void readTimestamp() {
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Getting timestamp of {0}", "Status"),
                     this.getName()));
 
             if(attributes().isFile()) {
                 // The "pathname" specifies an object in the NVFS which may be the object of a RETR command.
                 // Attempts to query the modification time of files that exist but are unable to be
                 // retrieved may generate an error-response
                 try {
                     attributes().setModificationDate(this.getSession().getClient().mdtm(this.getAbsolute()));
                 }
                 catch(FTPException ignore) {
                     // MDTM not supported; ignore
                     log.warn(ignore.getMessage());
                 }
             }
             if(-1 == attributes().getModificationDate()) {
                 // Read the timestamp from the directory listing
                 final AttributedList<AbstractPath> l = this.getParent().children();
                 if(l.contains(this.getReference())) {
                     attributes().setModificationDate(l.get(this.getReference()).attributes().getModificationDate());
                 }
             }
         }
         catch(IOException e) {
             this.error("Cannot read file attributes", e);
 
         }
     }
 
     @Override
     public void readUnixPermission() {
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Getting permission of {0}", "Status"),
                     this.getName()));
 
             // Read the permission from the directory listing
             final AttributedList<AbstractPath> l = this.getParent().children();
             if(l.contains(this.getReference())) {
                 attributes().setPermission(l.get(this.getReference()).attributes().getPermission());
             }
         }
         catch(IOException e) {
             this.error("Cannot read file attributes", e);
         }
     }
 
     @Override
     public void delete() {
         try {
             this.getSession().check();
             if(attributes().isFile() || attributes().isSymbolicLink()) {
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                         this.getName()));
 
                 this.getSession().getClient().delete(this.getAbsolute());
             }
             else if(attributes().isDirectory()) {
                 for(AbstractPath file : this.children()) {
                     if(!this.getSession().isConnected()) {
                         break;
                     }
                     if(file.attributes().isFile() || file.attributes().isSymbolicLink()) {
                         this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                                 file.getName()));
 
                         this.getSession().getClient().delete(file.getAbsolute());
                     }
                     else if(file.attributes().isDirectory()) {
                         file.delete();
                     }
                 }
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                         this.getName()));
 
                 this.getSession().getClient().rmdir(this.getAbsolute());
             }
             // The directory listing is no more current
             this.getParent().invalidate();
         }
         catch(IOException e) {
             if(attributes().isFile()) {
                 this.error("Cannot delete file", e);
             }
             if(attributes().isDirectory()) {
                 this.error("Cannot delete folder", e);
             }
         }
     }
 
     @Override
     public void writeOwner(String owner, boolean recursive) {
         String command = "chown";
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Changing owner of {0} to {1}", "Status"),
                     this.getName(), owner));
 
             if(attributes().isFile() && !attributes().isSymbolicLink()) {
                 this.getSession().getClient().site(command + " " + owner + " " + this.getAbsolute());
             }
             else if(attributes().isDirectory()) {
                 this.getSession().getClient().site(command + " " + owner + " " + this.getAbsolute());
                 if(recursive) {
                     for(AbstractPath child : this.children()) {
                         if(!this.getSession().isConnected()) {
                             break;
                         }
                         ((Path) child).writeOwner(owner, recursive);
                     }
                 }
             }
         }
         catch(IOException e) {
             this.error("Cannot change owner", e);
         }
     }
 
     @Override
     public void writeGroup(String group, boolean recursive) {
         String command = "chgrp";
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Changing group of {0} to {1}", "Status"),
                     this.getName(), group));
 
             if(attributes().isFile() && !attributes().isSymbolicLink()) {
                 this.getSession().getClient().site(command + " " + group + " " + this.getAbsolute());
             }
             else if(attributes().isDirectory()) {
                 this.getSession().getClient().site(command + " " + group + " " + this.getAbsolute());
                 if(recursive) {
                     for(AbstractPath child : this.children()) {
                         if(!this.getSession().isConnected()) {
                             break;
                         }
                         ((Path) child).writeGroup(group, recursive);
                     }
                 }
             }
         }
         catch(IOException e) {
             this.error("Cannot change group", e);
         }
     }
 
     @Override
     public void writeUnixPermission(Permission perm, boolean recursive) {
         try {
             this.getSession().check();
             this.writeUnixPermissionImpl(perm, recursive);
         }
         catch(IOException e) {
             this.error("Cannot change permissions", e);
         }
     }
 
     private void writeUnixPermissionImpl(Permission perm, boolean recursive) throws IOException {
         this.getSession().message(MessageFormat.format(Locale.localizedString("Changing permission of {0} to {1}", "Status"),
                 this.getName(), perm.getOctalString()));
         try {
             if(attributes().isFile() && !attributes().isSymbolicLink()) {
                 this.getSession().getClient().chmod(perm.getOctalString(), this.getAbsolute());
                 this.attributes().setPermission(perm);
             }
             else if(attributes().isDirectory()) {
                 this.getSession().getClient().chmod(perm.getOctalString(), this.getAbsolute());
                this.attributes().setPermission(perm);
                 if(recursive) {
                     for(AbstractPath child : this.children()) {
                         if(!this.getSession().isConnected()) {
                             break;
                         }
                         ((FTPPath) child).writeUnixPermissionImpl(perm, recursive);
                     }
                 }
             }
         }
         catch(FTPException ignore) {
             // CHMOD not supported
             log.warn(ignore.getMessage());
         }
         finally {
             //this.attributes().clear(false, false, true, false);
             ;// This will force a directory listing to parse the permissions again.
             //this.getParent().invalidate();
         }
     }
 
     @Override
     public void writeTimestamp(long created, long modified, long accessed) {
         try {
             this.writeModificationDateImpl(created, modified);
         }
         catch(IOException e) {
             this.error("Cannot change timestamp", e);
         }
     }
 
     private void writeModificationDateImpl(long created, long modified) throws IOException {
         this.getSession().message(MessageFormat.format(Locale.localizedString("Changing timestamp of {0} to {1}", "Status"),
                 this.getName(), DateFormatterFactory.instance().getShortFormat(modified)));
         try {
             this.getSession().getClient().mfmt(modified, created, this.getAbsolute());
             this.attributes().setModificationDate(modified);
             this.attributes().setCreationDate(created);
         }
         catch(FTPException ignore) {
             //MFMT not supported; ignore
             log.warn(ignore.getMessage());
         }
         finally {
             //this.attributes().clear(true, false, false, false);
             ;// This will force a directory listing to parse the timestamp again if MDTM is not supported.
             //this.getParent().invalidate();
         }
     }
 
     @Override
     protected void download(final BandwidthThrottle throttle, final StreamListener listener, final boolean check) {
         try {
             if(check) {
                 this.getSession().check();
             }
             if(attributes().isFile()) {
                 if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.AUTO.toString())) {
                     if(this.getTextFiletypePattern().matcher(this.getName()).matches()) {
                         this.downloadASCII(throttle, listener);
                     }
                     else {
                         this.downloadBinary(throttle, listener);
                     }
                 }
                 else if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.BINARY.toString())) {
                     this.downloadBinary(throttle, listener);
                 }
                 else if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.ASCII.toString())) {
                     this.downloadASCII(throttle, listener);
                 }
                 else {
                     throw new FTPException("Transfer mode not set");
                 }
             }
         }
         catch(IOException e) {
             this.error("Download failed", e);
         }
     }
 
     private void downloadBinary(final BandwidthThrottle throttle, final StreamListener listener) throws IOException {
         InputStream in = null;
         OutputStream out = null;
         try {
             this.getSession().getClient().setTransferType(FTPTransferType.BINARY);
             if(this.status().isResume()) {
                 if(!this.getSession().getClient().isFeatureSupported("REST STREAM")) {
                     this.status().setResume(false);
                 }
             }
             in = this.getSession().getClient().get(this.getAbsolute(), this.status().isResume() ? this.getLocal().attributes().getSize() : 0);
             out = this.getLocal().getOutputStream(this.status().isResume());
             try {
                 this.download(in, out, throttle, listener);
             }
             catch(ConnectionCanceledException e) {
                 // Interrupted by user
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
                 // Tell the server to abort the previous FTP service command and any associated transfer of data
                 this.getSession().getClient().abor();
                 this.status().setComplete(false);
                 throw e;
             }
             if(this.status().isComplete()) {
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
                 try {
                     this.getSession().getClient().validateTransfer();
                 }
                 catch(FTPException e) {
                     this.status().setComplete(false);
                     throw e;
                 }
             }
         }
         finally {
             IOUtils.closeQuietly(in);
             IOUtils.closeQuietly(out);
         }
     }
 
     private void downloadASCII(final BandwidthThrottle throttle, final StreamListener listener) throws IOException {
         InputStream in = null;
         OutputStream out = null;
         try {
             String lineSeparator = System.getProperty("line.separator"); //default value
             if(Preferences.instance().getProperty("ftp.line.separator").equals("unix")) {
                 lineSeparator = UNIX_LINE_SEPARATOR;
             }
             else if(Preferences.instance().getProperty("ftp.line.separator").equals("mac")) {
                 lineSeparator = MAC_LINE_SEPARATOR;
             }
             else if(Preferences.instance().getProperty("ftp.line.separator").equals("win")) {
                 lineSeparator = DOS_LINE_SEPARATOR;
             }
             this.getSession().getClient().setTransferType(FTPTransferType.ASCII);
             in = new FromNetASCIIInputStream(this.getSession().getClient().get(this.getAbsolute(), 0),
                     lineSeparator);
             out = new FromNetASCIIOutputStream(this.getLocal().getOutputStream(false),
                     lineSeparator);
             try {
                 this.download(in, out, throttle, listener);
             }
             catch(ConnectionCanceledException e) {
                 // Interrupted by user
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
                 // Tell the server to abort the previous FTP service command and any associated transfer of data
                 this.getSession().getClient().abor();
                 this.status().setComplete(false);
                 throw e;
             }
             if(this.status().isComplete()) {
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
                 try {
                     this.getSession().getClient().validateTransfer();
                 }
                 catch(FTPException e) {
                     this.status().setComplete(false);
                     throw e;
                 }
             }
         }
         finally {
             IOUtils.closeQuietly(in);
             IOUtils.closeQuietly(out);
         }
     }
 
     @Override
     protected void upload(BandwidthThrottle throttle, StreamListener listener, boolean check) {
         try {
             if(attributes().isFile()) {
                 if(check) {
                     this.getSession().check();
                 }
                 if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.AUTO.toString())) {
                     if(this.getTextFiletypePattern().matcher(this.getName()).matches()) {
                         this.uploadASCII(throttle, listener);
                     }
                     else {
                         this.uploadBinary(throttle, listener);
                     }
                 }
                 else if(Preferences.instance().getProperty("ftp.transfermode").equals(
                         FTPTransferType.BINARY.toString())) {
                     this.uploadBinary(throttle, listener);
                 }
                 else if(Preferences.instance().getProperty("ftp.transfermode").equals(
                         FTPTransferType.ASCII.toString())) {
                     this.uploadASCII(throttle, listener);
                 }
                 else {
                     throw new FTPException("Transfer mode not set");
                 }
             }
         }
         catch(IOException e) {
             this.error("Upload failed", e);
         }
     }
 
     private void uploadBinary(BandwidthThrottle throttle, StreamListener listener) throws IOException {
         InputStream in = null;
         OutputStream out = null;
         try {
             this.getSession().getClient().setTransferType(FTPTransferType.BINARY);
             in = this.getLocal().getInputStream();
             out = this.getSession().getClient().put(this.getAbsolute(), this.status().isResume());
             try {
                 this.upload(out, in, throttle, listener);
             }
             catch(ConnectionCanceledException e) {
                 // Interrupted by user
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
                 // Tell the server to abort the previous FTP service command and any associated transfer of data
                 this.getSession().getClient().abor();
                 this.status().setComplete(false);
                 throw e;
             }
             if(status().isComplete()) {
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
                 try {
                     this.getSession().getClient().validateTransfer();
                 }
                 catch(FTPException e) {
                     this.status().setComplete(false);
                     throw e;
                 }
             }
         }
         finally {
             IOUtils.closeQuietly(in);
             IOUtils.closeQuietly(out);
         }
     }
 
     private void uploadASCII(final BandwidthThrottle throttle, final StreamListener listener) throws IOException {
         InputStream in = null;
         OutputStream out = null;
         try {
             this.getSession().getClient().setTransferType(FTPTransferType.ASCII);
             in = new ToNetASCIIInputStream(this.getLocal().getInputStream());
             out = new ToNetASCIIOutputStream(this.getSession().getClient().put(this.getAbsolute(),
                     this.status().isResume()));
             try {
                 this.upload(out, in, throttle, listener);
             }
             catch(ConnectionCanceledException e) {
                 // Interrupted by user
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
                 // Tell the server to abort the previous FTP service command and any associated transfer of data
                 this.getSession().getClient().abor();
                 this.status().setComplete(false);
                 throw e;
             }
             if(this.status().isComplete()) {
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
                 try {
                     this.getSession().getClient().validateTransfer();
                 }
                 catch(FTPException e) {
                     this.status().setComplete(false);
                     throw e;
                 }
             }
         }
         finally {
             IOUtils.closeQuietly(in);
             IOUtils.closeQuietly(out);
         }
     }
 }
