 /*
  * $Id$
  *
  * Created: [6/07/2004]
  *
  * Contributors: Paul Legato - fix for expunge() method
  *
  * Copyright (c) 2004, Ben Fortuna
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  *  o Redistributions of source code must retain the above copyright
  * notice, this list of conditions and the following disclaimer.
  *
  *  o Redistributions in binary form must reproduce the above copyright
  * notice, this list of conditions and the following disclaimer in the
  * documentation and/or other materials provided with the distribution.
  *
  *  o Neither the name of Ben Fortuna nor the names of any other contributors
  * may be used to endorse or promote products derived from this software
  * without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
  * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 package net.fortuna.mstor;
 
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 import java.util.Map;
 
 import javax.mail.Flags;
 import javax.mail.Folder;
 import javax.mail.FolderNotFoundException;
 import javax.mail.Message;
 import javax.mail.MessagingException;
 import javax.mail.event.ConnectionEvent;
 import javax.mail.event.ConnectionListener;
 
 import net.fortuna.mstor.data.MboxFile;
 import net.fortuna.mstor.data.MetaFolderImpl;
 import net.fortuna.mstor.util.Cache;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 /**
  * A folder implementation for the mstor javamail provider.
  * @author benfortuna
  */
 public class MStorFolder extends Folder {
 
     private static final String DIR_EXTENSION = ".sbd";
 
     private static Log log = LogFactory.getLog(MStorFolder.class);
 
     /**
      * Indicates whether this folder holds messages
      * or other folders.
      */
     private int type;
 
     /**
      * Indicates whether this folder is open.
      */
     private boolean open;
 
     /**
      * The file this folder is associated with.
      */
     private File file;
 
     /**
      * An mbox file where the folder holds messages.
      * This variable is not applicable (and therefore not
      * initialised) for folders that hold other folders.
      */
     private MboxFile mbox;
 
     /**
      * Additional metadata for an mstor folder that is not
      * provided by the standard mbox format.
      */
     private MetaFolder meta;
 
     /**
      * A cache for messages.
      */
     private Map messageCache;
     
     private MStorStore mStore;
 
     /**
      * Constructs a new mstor folder with metadata enabled.
      * @param store
      * @param file
      */
     public MStorFolder(MStorStore store, File file) {
         this(store, file, true);
     }
 
     /**
      * Constructs a new mstor folder instance.
      * @param store
      * @param file
      * @param metaEnabled
      */
     public MStorFolder(MStorStore store, File file, final boolean metaEnabled) {
         super(store);
         this.mStore = store;
         this.file = file;
         if (file.isDirectory()) {
             type = HOLDS_FOLDERS;
         }
         else {
             type = HOLDS_FOLDERS | HOLDS_MESSAGES;
         }
         // automatically close (release resources) when the
         // store is closed..
         store.addConnectionListener(new ConnectionListener() {
         	/* (non-Javadoc)
 			 * @see javax.mail.event.ConnectionListener#closed(javax.mail.event.ConnectionEvent)
 			 */
 			public final void closed(final ConnectionEvent e) {
 				try {
 					if (isOpen()) {
 						close(false);
 					}
 				}
 				catch (MessagingException me) {
 					log.error("Error closing folder [" + this + "]", me);
 				}
 			}
 			
 			/* (non-Javadoc)
 			 * @see javax.mail.event.ConnectionListener#disconnected(javax.mail.event.ConnectionEvent)
 			 */
 			public final void disconnected(final ConnectionEvent e) {
 			}
 			
 			/* (non-Javadoc)
 			 * @see javax.mail.event.ConnectionListener#opened(javax.mail.event.ConnectionEvent)
 			 */
 			public final void opened(final ConnectionEvent e) {
 			}
         });
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getName()
      */
     public String getName() {
         return file.getName();
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getFullName()
      */
     public String getFullName() {
         return file.getPath();
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getParent()
      */
     public Folder getParent() throws MessagingException {
         return new MStorFolder(mStore, file.getParentFile());
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#exists()
      */
     public boolean exists() throws MessagingException {
         return file.exists();
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#list(java.lang.String)
      */
     public Folder[] list(String pattern) throws MessagingException {
         if ((getType() & HOLDS_FOLDERS) == 0) {
             throw new MessagingException("Invalid folder type");
         }
 
         List folders = new ArrayList();
 
         File[] files = null;
 
         if (file.isDirectory()) {
             files = file.listFiles();
         }
         else {
             files = new File(file.getAbsolutePath() + DIR_EXTENSION).listFiles();
         }
 
         for (int i = 0; files != null && i < files.length; i++) {
             if (!files[i].getName().endsWith(MetaFolderImpl.FILE_EXTENSION)
                     && !files[i].getName().endsWith(DIR_EXTENSION)
                     && (files[i].isDirectory() || files[i].length() == 0 || MboxFile.isValid(files[i]))) {
 //                && ((type & Folder.HOLDS_MESSAGES) == 0
 //                    || !files[i].isDirectory())) {
                 folders.add(new MStorFolder(mStore, files[i]));
             }
         }
 
         return (Folder[]) folders.toArray(new Folder[folders.size()]);
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getSeparator()
      */
     public char getSeparator() throws MessagingException {
         if (!exists()) {
             throw new FolderNotFoundException(this);
         }
 
         return File.separatorChar;
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getType()
      */
     public int getType() throws MessagingException {
         if (!exists()) {
             throw new FolderNotFoundException(this);
         }
 
         return type;
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#create(int)
      */
     public boolean create(int type) throws MessagingException {
         if (file.exists()) {
             throw new MessagingException("Folder already exists");
         }
 
         // debugging..
         log.debug("Creating folder [" + file.getAbsolutePath() + "]");
 
         if ((type & HOLDS_MESSAGES) > 0) {
             this.type = type;
 
             try {
                 file.getParentFile().mkdirs();
                 return file.createNewFile();
             }
             catch (IOException ioe) {
                 throw new MessagingException("Unable to create folder [" + file + "]", ioe);
             }
         }
         else if ((type & HOLDS_FOLDERS) > 0) {
             this.type = type;
             return file.mkdirs();
         }
         else {
             throw new MessagingException("Invalid folder type");
         }
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#hasNewMessages()
      */
     public boolean hasNewMessages() throws MessagingException {
         // TODO Auto-generated method stub
         return false;
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getFolder(java.lang.String)
      */
     public Folder getFolder(String name) throws MessagingException {
         File file = null;
 
         // if path is absolute don't use relative file..
         if (name.startsWith("/")) {
             file = new File(name);
         }
         // default folder..
 //        else if ("".equals(getName())) {
         // if a folder doesn't hold messages (ie. default
         // folder) we don't have a separate subdirectory
         // for sub-folders..
         else if ((getType() & HOLDS_MESSAGES) == 0) {
             file = new File(this.file, name);
         }
         else {
             file = new File(this.file.getAbsolutePath() + DIR_EXTENSION, name);
         }
 
         // we need to initialise the metadata name in case
         // the folder does not exist..
         return new MStorFolder(mStore, file);
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#delete(boolean)
      */
     public boolean delete(boolean recurse) throws MessagingException {
         if (isOpen()) {
             throw new IllegalStateException("Folder not closed");
         }
 
         if ((getType() & HOLDS_FOLDERS) > 0) {
             if (recurse) {
                 Folder[] subfolders = list();
 
                 for (int i = 0; i < subfolders.length; i++) {
                     subfolders[i].delete(recurse);
                 }
             }
             else if (list().length > 0) {
                 // cannot delete if has subfolders..
                 return false;
             }
         }
 
         File metafile = new File(file.getAbsolutePath() + MetaFolderImpl.FILE_EXTENSION);
         metafile.delete();
 
         // attempt to delete the directory/file..
         return file.delete();
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#renameTo(javax.mail.Folder)
      */
     public boolean renameTo(Folder folder) throws MessagingException {
         if (!exists()) {
             throw new FolderNotFoundException(this);
         }
 
         if (isOpen()) {
             throw new IllegalStateException("Folder not closed");
         }
 
         return file.renameTo(new File(file.getParent(), folder.getName()));
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#open(int)
      */
     public void open(int mode) throws MessagingException {
    	if (!exists()) {
    		throw new FolderNotFoundException(this, "Folder does not exist");
    	}
         if (isOpen()) {
             throw new IllegalStateException("Folder not closed");
         }
 
         if ((getType() & HOLDS_MESSAGES) > 0) {
             if (mode == READ_WRITE) {
                 mbox = new MboxFile(file, MboxFile.READ_WRITE);
             }
             else {
                 mbox = new MboxFile(file, MboxFile.READ_ONLY);
             }
         }
 
         this.mode = mode;
         open = true;
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#close(boolean)
      */
     public final void close(final boolean expunge) throws MessagingException {
         if (!isOpen()) {
             throw new IllegalStateException("Folder not open");
         }
 
         if (expunge) {
             expunge();
         }
 
         try {
             mbox.close();
             mbox = null;
         }
         catch (IOException ioe) {
             throw new MessagingException("Error ocurred closing mbox file", ioe);
         }
 
         open = false;
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#isOpen()
      */
     public boolean isOpen() {
         return open;
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getPermanentFlags()
      */
     public Flags getPermanentFlags() {
         // TODO Auto-generated method stub
         return null;
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getMessageCount()
      */
     public int getMessageCount() throws MessagingException {
         if (!exists()) {
             throw new FolderNotFoundException(this);
         }
 
         if ((getType() & HOLDS_MESSAGES) == 0) {
             throw new MessagingException("Invalid folder type");
         }
 
         if (!isOpen()) {
             return -1;
         }
         else {
             try {
                 return mbox.getMessageCount();
             }
             catch (IOException ioe) {
                 throw new MessagingException("Error ocurred reading message count", ioe);
             }
         }
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#getMessage(int)
      */
     public Message getMessage(int index) throws MessagingException {
         if (!exists()) {
             throw new FolderNotFoundException(this);
         }
 
         if (!isOpen()) {
             throw new IllegalStateException("Folder not open");
         }
 
         if (index <= 0 || index > getMessageCount()) {
             throw new IndexOutOfBoundsException("Message does not exist");
         }
 
         if ((getType() & HOLDS_MESSAGES) == 0) {
             throw new MessagingException("Invalid folder type");
         }
 
         MStorMessage message = (MStorMessage) getMessageCache().get(String.valueOf(index));
 
         if (message == null) {
             try {
                 // javamail uses 1-based indexing for messages..
                 message = new MStorMessage(this, mbox.getMessageAsStream(index - 1), index);
                 if (mStore.isMetaEnabled()) {
                     message.setMeta(getMeta().getMessage(message));
                 }
                 getMessageCache().put(String.valueOf(index), message);
             }
             catch (IOException ioe) {
                 throw new MessagingException("Error ocurred reading message [" + index + "]", ioe);
             }
         }
 
         return message;
     }
 
     /**
      * Appends the specified messages to this folder.
      * NOTE: The specified message array is destroyed upon processing
      * to alleviate memory concerns with large messages. You should ensure
      * the messages specified in this array are referenced elsewhere if you
      * want to retain them.
      */
     /* (non-Javadoc)
      * @see javax.mail.Folder#appendMessages(javax.mail.Message[])
      */
     public final void appendMessages(final Message[] messages) throws MessagingException {
         if (!exists()) {
             throw new FolderNotFoundException(this);
         }
 
         Date received = new Date();
         ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
         
         for (int i=0; i<messages.length; i++) {
             try {
                 out.reset();
                 messages[i].writeTo(out);
                 mbox.appendMessage(out.toByteArray());
 
                 // create metadata..
                 if (mStore.isMetaEnabled()) {
                     MetaMessage meta = getMeta().getMessage(messages[i]);
                     if (meta != null) {
                         meta.setReceived(received);
                         meta.setFlags(messages[i].getFlags());
                         meta.setHeaders(messages[i].getAllHeaders());
                     }
                 }
                
                // prune messages as we go to allow for garbage
                // collection..
                messages[i] = null;
             }
             catch (IOException ioe) {
                 log.debug("Error appending message [" + i + "]", ioe);
                 throw new MessagingException("Error appending message [" + i + "]", ioe);
             }
         }
 
         // save metadata..
         if (mStore.isMetaEnabled()) {
             try {
                 getMeta().save();
             }
             catch (IOException ioe) {
                 log.error("Error ocurred saving metadata", ioe);
             }
         }
     }
 
     /* (non-Javadoc)
      * @see javax.mail.Folder#expunge()
      */
     public Message[] expunge() throws MessagingException {
         if (!exists()) {
             throw new FolderNotFoundException(this);
         }
 
         if (!isOpen()) {
             throw new IllegalStateException("Folder not open");
         }
         
         if (Folder.READ_ONLY == getMode()) {
         	throw new MessagingException("Folder is read-only");
         }
 
         int count = getDeletedMessageCount();
 
         Message[] messages = getMessages();
 
         List deletedList = new ArrayList();
 
         for (int i=0; i<messages.length && deletedList.size() < count; i++) {
             if (messages[i].isSet(Flags.Flag.DELETED)) {
                 deletedList.add(messages[i]);
             }
         }
 
         MStorMessage[] deleted = (MStorMessage[]) deletedList.toArray(new MStorMessage[deletedList.size()]);
 
         int[] indices = new int[deleted.length];
 
         for (int i=0; i<deleted.length; i++) {
             // have to subtract one, because the raw storage array is 0-based, but
             // the message numbers are 1-based
             indices[i] = deleted[i].getMessageNumber() - 1;
         }
 
         try {
             mbox.purge(indices);
         }
         catch (IOException ioe) {
             throw new MessagingException("Error purging mbox file", ioe);
         }
         
         if (mStore.isMetaEnabled()) {
             getMeta().removeMessages(indices);
         }
 
         for (int i=0; i<deleted.length; i++) {
             deleted[i].setExpunged(true);
         }
 
         return deleted;
     }
 
     /**
      * @return Returns the messageCache.
      */
     private Map getMessageCache() {
         if (messageCache == null) {
             messageCache = new Cache();
         }
 
         return messageCache;
     }
 
     /**
      * @return Returns the metadata for this folder.
      */
     protected MetaFolder getMeta() {
         if (meta == null) {
             meta = new MetaFolderImpl(new File(getFullName() + MetaFolderImpl.FILE_EXTENSION));
         }
 
         return meta;
     }
 
     /**
      * Check if this folder is open.
      * @throws MessagingException thrown if the folder is not open
      */
     private void checkOpen() throws MessagingException {
         if (!isOpen()) {
             throw new MessagingException("Folder not open");
         }
     }
 }
