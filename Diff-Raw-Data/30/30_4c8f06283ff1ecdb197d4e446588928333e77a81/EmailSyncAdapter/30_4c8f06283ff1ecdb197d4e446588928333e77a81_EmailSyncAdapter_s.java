 /*
  * Copyright (C) 2008-2009 Marc Blank
  * Licensed to The Android Open Source Project.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.android.exchange.adapter;
 
 import com.android.email.mail.Address;
 import com.android.email.provider.EmailContent;
 import com.android.email.provider.EmailProvider;
 import com.android.email.provider.EmailContent.Account;
 import com.android.email.provider.EmailContent.AccountColumns;
 import com.android.email.provider.EmailContent.Attachment;
 import com.android.email.provider.EmailContent.Mailbox;
 import com.android.email.provider.EmailContent.Message;
 import com.android.email.provider.EmailContent.MessageColumns;
 import com.android.email.provider.EmailContent.SyncColumns;
 import com.android.email.service.MailService;
 import com.android.exchange.Eas;
 import com.android.exchange.EasSyncService;
 
 import android.content.ContentProviderOperation;
 import android.content.ContentResolver;
 import android.content.ContentUris;
 import android.content.ContentValues;
 import android.content.OperationApplicationException;
 import android.database.Cursor;
 import android.net.Uri;
 import android.os.RemoteException;
 import android.webkit.MimeTypeMap;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.GregorianCalendar;
 import java.util.TimeZone;
 
 /**
  * Sync adapter for EAS email
  *
  */
 public class EmailSyncAdapter extends AbstractSyncAdapter {
 
     private static final int UPDATES_READ_COLUMN = 0;
     private static final int UPDATES_MAILBOX_KEY_COLUMN = 1;
     private static final int UPDATES_SERVER_ID_COLUMN = 2;
     private static final int UPDATES_FLAG_COLUMN = 3;
     private static final String[] UPDATES_PROJECTION =
         {MessageColumns.FLAG_READ, MessageColumns.MAILBOX_KEY, SyncColumns.SERVER_ID,
             MessageColumns.FLAG_FAVORITE};
 
     String[] bindArguments = new String[2];
 
     ArrayList<Long> mDeletedIdList = new ArrayList<Long>();
     ArrayList<Long> mUpdatedIdList = new ArrayList<Long>();
 
     public EmailSyncAdapter(Mailbox mailbox, EasSyncService service) {
         super(mailbox, service);
     }
 
     @Override
     public boolean parse(InputStream is) throws IOException {
         EasEmailSyncParser p = new EasEmailSyncParser(is, this);
         return p.parse();
     }
 
     public class EasEmailSyncParser extends AbstractSyncParser {
 
         private static final String WHERE_SERVER_ID_AND_MAILBOX_KEY =
             SyncColumns.SERVER_ID + "=? and " + MessageColumns.MAILBOX_KEY + "=?";
 
         private String mMailboxIdAsString;
 
         ArrayList<Message> newEmails = new ArrayList<Message>();
         ArrayList<Long> deletedEmails = new ArrayList<Long>();
         ArrayList<ServerChange> changedEmails = new ArrayList<ServerChange>();
 
         public EasEmailSyncParser(InputStream in, EmailSyncAdapter adapter) throws IOException {
             super(in, adapter);
             mMailboxIdAsString = Long.toString(mMailbox.mId);
         }
 
         @Override
         public void wipe() {
             mContentResolver.delete(Message.CONTENT_URI,
                     Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
             mContentResolver.delete(Message.DELETED_CONTENT_URI,
                     Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
             mContentResolver.delete(Message.UPDATED_CONTENT_URI,
                     Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
         }
 
         public void addData (Message msg) throws IOException {
             ArrayList<Attachment> atts = new ArrayList<Attachment>();
 
             while (nextTag(Tags.SYNC_APPLICATION_DATA) != END) {
                 switch (tag) {
                     case Tags.EMAIL_ATTACHMENTS:
                     case Tags.BASE_ATTACHMENTS: // BASE_ATTACHMENTS is used in EAS 12.0 and up
                         attachmentsParser(atts, msg);
                         break;
                     case Tags.EMAIL_TO:
                         msg.mTo = Address.pack(Address.parse(getValue()));
                         break;
                     case Tags.EMAIL_FROM:
                         Address[] froms = Address.parse(getValue());
                         if (froms != null && froms.length > 0) {
                           msg.mDisplayName = froms[0].toFriendly();
                         }
                         msg.mFrom = Address.pack(froms);
                         break;
                     case Tags.EMAIL_CC:
                         msg.mCc = Address.pack(Address.parse(getValue()));
                         break;
                     case Tags.EMAIL_REPLY_TO:
                         msg.mReplyTo = Address.pack(Address.parse(getValue()));
                         break;
                     case Tags.EMAIL_DATE_RECEIVED:
                         String date = getValue();
                         // 2009-02-11T18:03:03.627Z
                         GregorianCalendar cal = new GregorianCalendar();
                         cal.set(Integer.parseInt(date.substring(0, 4)), Integer.parseInt(date
                                 .substring(5, 7)) - 1, Integer.parseInt(date.substring(8, 10)),
                                 Integer.parseInt(date.substring(11, 13)), Integer.parseInt(date
                                         .substring(14, 16)), Integer.parseInt(date
                                                 .substring(17, 19)));
                         cal.setTimeZone(TimeZone.getTimeZone("GMT"));
                         msg.mTimeStamp = cal.getTimeInMillis();
                         break;
                     case Tags.EMAIL_SUBJECT:
                         msg.mSubject = getValue();
                         break;
                     case Tags.EMAIL_READ:
                         msg.mFlagRead = getValueInt() == 1;
                         break;
                     case Tags.BASE_BODY:
                         bodyParser(msg);
                         break;
                     case Tags.EMAIL_FLAG:
                         msg.mFlagFavorite = flagParser();
                         break;
                     case Tags.EMAIL_BODY:
                         String text = getValue();
                         msg.mText = text;
                         break;
                     default:
                         skipTag();
                 }
             }
 
             if (atts.size() > 0) {
                 msg.mAttachments = atts;
             }
         }
 
         private void addParser(ArrayList<Message> emails) throws IOException {
             Message msg = new Message();
             msg.mAccountKey = mAccount.mId;
             msg.mMailboxKey = mMailbox.mId;
             msg.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;
 
             while (nextTag(Tags.SYNC_ADD) != END) {
                 switch (tag) {
                     case Tags.SYNC_SERVER_ID:
                         msg.mServerId = getValue();
                         break;
                     case Tags.SYNC_APPLICATION_DATA:
                         addData(msg);
                         break;
                     default:
                         skipTag();
                 }
             }
             emails.add(msg);
         }
 
         // For now, we only care about the "active" state
         private Boolean flagParser() throws IOException {
             Boolean state = false;
             while (nextTag(Tags.EMAIL_FLAG) != END) {
                 switch (tag) {
                     case Tags.EMAIL_FLAG_STATUS:
                         state = getValueInt() == 2;
                         break;
                     default:
                         skipTag();
                 }
             }
             return state;
         }
 
         private void bodyParser(Message msg) throws IOException {
             String bodyType = Eas.BODY_PREFERENCE_TEXT;
             String body = "";
             while (nextTag(Tags.EMAIL_BODY) != END) {
                 switch (tag) {
                     case Tags.BASE_TYPE:
                         bodyType = getValue();
                         break;
                     case Tags.BASE_DATA:
                         body = getValue();
                         break;
                     default:
                         skipTag();
                 }
             }
             // We always ask for TEXT or HTML; there's no third option
             if (bodyType.equals(Eas.BODY_PREFERENCE_HTML)) {
                 msg.mHtml = body;
             } else {
                 msg.mText = body;
             }
         }
 
         private void attachmentsParser(ArrayList<Attachment> atts, Message msg) throws IOException {
             while (nextTag(Tags.EMAIL_ATTACHMENTS) != END) {
                 switch (tag) {
                     case Tags.EMAIL_ATTACHMENT:
                     case Tags.BASE_ATTACHMENT:  // BASE_ATTACHMENT is used in EAS 12.0 and up
                         attachmentParser(atts, msg);
                         break;
                     default:
                         skipTag();
                 }
             }
         }
 
         private void attachmentParser(ArrayList<Attachment> atts, Message msg) throws IOException {
             String fileName = null;
             String length = null;
             String location = null;
 
             while (nextTag(Tags.EMAIL_ATTACHMENT) != END) {
                 switch (tag) {
                     // We handle both EAS 2.5 and 12.0+ attachments here
                     case Tags.EMAIL_DISPLAY_NAME:
                     case Tags.BASE_DISPLAY_NAME:
                         fileName = getValue();
                         break;
                     case Tags.EMAIL_ATT_NAME:
                     case Tags.BASE_FILE_REFERENCE:
                         location = getValue();
                         break;
                     case Tags.EMAIL_ATT_SIZE:
                     case Tags.BASE_ESTIMATED_DATA_SIZE:
                         length = getValue();
                         break;
                     default:
                         skipTag();
                 }
             }
 
             if ((fileName != null) && (length != null) && (location != null)) {
                 Attachment att = new Attachment();
                 att.mEncoding = "base64";
                 att.mSize = Long.parseLong(length);
                 att.mFileName = fileName;
                 att.mLocation = location;
                 att.mMimeType = getMimeTypeFromFileName(fileName);
                 atts.add(att);
                 msg.mFlagAttachment = true;
             }
         }
 
         /**
          * Try to determine a mime type from a file name, defaulting to application/x, where x
          * is either the extension or (if none) octet-stream
          * At the moment, this is somewhat lame, since many file types aren't recognized
          * @param fileName the file name to ponder
          * @return
          */
         // Note: The MimeTypeMap method currently uses a very limited set of mime types
         // A bug has been filed against this issue.
         public String getMimeTypeFromFileName(String fileName) {
             String mimeType;
             int lastDot = fileName.lastIndexOf('.');
             String extension = null;
             if ((lastDot > 0) && (lastDot < fileName.length() - 1)) {
                 extension = fileName.substring(lastDot + 1).toLowerCase();
             }
             if (extension == null) {
                 // A reasonable default for now.
                 mimeType = "application/octet-stream";
             } else {
                 mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                 if (mimeType == null) {
                     mimeType = "application/" + extension;
                 }
             }
             return mimeType;
         }
 
         private Cursor getServerIdCursor(String serverId, String[] projection) {
             bindArguments[0] = serverId;
             bindArguments[1] = mMailboxIdAsString;
             return mContentResolver.query(Message.CONTENT_URI, projection,
                     WHERE_SERVER_ID_AND_MAILBOX_KEY, bindArguments, null);
         }
 
         private void deleteParser(ArrayList<Long> deletes, int entryTag) throws IOException {
             while (nextTag(entryTag) != END) {
                 switch (tag) {
                     case Tags.SYNC_SERVER_ID:
                         String serverId = getValue();
                         // Find the message in this mailbox with the given serverId
                         Cursor c = getServerIdCursor(serverId, Message.ID_COLUMN_PROJECTION);
                         try {
                             if (c.moveToFirst()) {
                                 userLog("Deleting ", serverId);
                                 deletes.add(c.getLong(Message.ID_COLUMNS_ID_COLUMN));
                             }
                         } finally {
                             c.close();
                         }
                         break;
                     default:
                         skipTag();
                 }
             }
         }
 
         class ServerChange {
             long id;
             Boolean read;
             Boolean flag;
 
             ServerChange(long _id, Boolean _read, Boolean _flag) {
                 id = _id;
                 read = _read;
                 flag = _flag;
             }
         }
 
         private void changeParser(ArrayList<ServerChange> changes) throws IOException {
             String serverId = null;
             Boolean oldRead = false;
             Boolean read = null;
             Boolean oldFlag = false;
             Boolean flag = null;
             long id = 0;
             while (nextTag(Tags.SYNC_CHANGE) != END) {
                 switch (tag) {
                     case Tags.SYNC_SERVER_ID:
                         serverId = getValue();
                         Cursor c = getServerIdCursor(serverId, Message.LIST_PROJECTION);
                         try {
                             if (c.moveToFirst()) {
                                 userLog("Changing ", serverId);
                                 oldRead = c.getInt(Message.LIST_READ_COLUMN) == Message.READ;
                                 oldFlag = c.getInt(Message.LIST_FAVORITE_COLUMN) == 1;
                                 id = c.getLong(Message.LIST_ID_COLUMN);
                             }
                         } finally {
                             c.close();
                         }
                         break;
                     case Tags.EMAIL_READ:
                         read = getValueInt() == 1;
                         break;
                     case Tags.EMAIL_FLAG:
                         flag = flagParser();
                         break;
                     case Tags.SYNC_APPLICATION_DATA:
                         break;
                     default:
                         skipTag();
                 }
             }
             if (((read != null) && !oldRead.equals(read)) ||
                     ((flag != null) && !oldFlag.equals(flag))) {
                 changes.add(new ServerChange(id, read, flag));
             }
         }
 
         /* (non-Javadoc)
          * @see com.android.exchange.adapter.EasContentParser#commandsParser()
          */
         @Override
         public void commandsParser() throws IOException {
             while (nextTag(Tags.SYNC_COMMANDS) != END) {
                 if (tag == Tags.SYNC_ADD) {
                     addParser(newEmails);
                     incrementChangeCount();
                 } else if (tag == Tags.SYNC_DELETE || tag == Tags.SYNC_SOFT_DELETE) {
                     deleteParser(deletedEmails, tag);
                     incrementChangeCount();
                 } else if (tag == Tags.SYNC_CHANGE) {
                     changeParser(changedEmails);
                     incrementChangeCount();
                 } else
                     skipTag();
             }
         }
 
         @Override
         public void responsesParser() {
         }
 
         @Override
         public void commit() {
             int notifyCount = 0;
 
             // Use a batch operation to handle the changes
             // TODO New mail notifications?  Who looks for these?
             ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
             for (Message msg: newEmails) {
                 if (!msg.mFlagRead) {
                     notifyCount++;
                 }
                 msg.addSaveOps(ops);
             }
             for (Long id : deletedEmails) {
                 ops.add(ContentProviderOperation.newDelete(
                         ContentUris.withAppendedId(Message.CONTENT_URI, id)).build());
             }
             if (!changedEmails.isEmpty()) {
                 // Server wins in a conflict...
                 for (ServerChange change : changedEmails) {
                      ContentValues cv = new ContentValues();
                     if (change.read != null) {
                         cv.put(MessageColumns.FLAG_READ, change.read);
                     }
                     if (change.flag != null) {
                         cv.put(MessageColumns.FLAG_FAVORITE, change.flag);
                     }
                     ops.add(ContentProviderOperation.newUpdate(
                             ContentUris.withAppendedId(Message.CONTENT_URI, change.id))
                                 .withValues(cv)
                                 .build());
                 }
             }
             ops.add(ContentProviderOperation.newUpdate(
                     ContentUris.withAppendedId(Mailbox.CONTENT_URI, mMailbox.mId)).withValues(
                     mMailbox.toContentValues()).build());
 
             addCleanupOps(ops);
 
             // No commits if we're stopped
             synchronized (mService.getSynchronizer()) {
                 if (mService.isStopped()) return;
                 try {
                     mContentResolver.applyBatch(EmailProvider.EMAIL_AUTHORITY, ops);
                     userLog(mMailbox.mDisplayName, " SyncKey saved as: ", mMailbox.mSyncKey);
                 } catch (RemoteException e) {
                     // There is nothing to be done here; fail by returning null
                 } catch (OperationApplicationException e) {
                     // There is nothing to be done here; fail by returning null
                 }
             }
 
             if (notifyCount > 0) {
                 // Use the new atomic add URI in EmailProvider
                 // We could add this to the operations being done, but it's not strictly
                 // speaking necessary, as the previous batch preserves the integrity of the
                 // database, whereas this is purely for notification purposes, and is itself atomic
                 ContentValues cv = new ContentValues();
                 cv.put(EmailContent.FIELD_COLUMN_NAME, AccountColumns.NEW_MESSAGE_COUNT);
                 cv.put(EmailContent.ADD_COLUMN_NAME, notifyCount);
                 Uri uri = ContentUris.withAppendedId(Account.ADD_TO_FIELD_URI, mAccount.mId);
                 mContentResolver.update(uri, cv, null, null);
                 MailService.actionNotifyNewMessages(mContext, mAccount.mId);
             }
         }
     }
 
     @Override
     public String getCollectionName() {
         return "Email";
     }
 
     private void addCleanupOps(ArrayList<ContentProviderOperation> ops) {
         // If we've sent local deletions, clear out the deleted table
         for (Long id: mDeletedIdList) {
             ops.add(ContentProviderOperation.newDelete(
                     ContentUris.withAppendedId(Message.DELETED_CONTENT_URI, id)).build());
         }
         // And same with the updates
         for (Long id: mUpdatedIdList) {
             ops.add(ContentProviderOperation.newDelete(
                     ContentUris.withAppendedId(Message.UPDATED_CONTENT_URI, id)).build());
         }
     }
 
     @Override
     public void cleanup() {
         if (!mDeletedIdList.isEmpty() || !mUpdatedIdList.isEmpty()) {
             ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
             addCleanupOps(ops);
             try {
                 mContext.getContentResolver()
                     .applyBatch(EmailProvider.EMAIL_AUTHORITY, ops);
             } catch (RemoteException e) {
                 // There is nothing to be done here; fail by returning null
             } catch (OperationApplicationException e) {
                 // There is nothing to be done here; fail by returning null
             }
         }
     }
 
     private String formatTwo(int num) {
         if (num < 10) {
             return "0" + (char)('0' + num);
         } else
             return Integer.toString(num);
     }
 
     /**
      * Create date/time in RFC8601 format.  Oddly enough, for calendar date/time, Microsoft uses
      * a different format that excludes the punctuation (this is why I'm not putting this in a
      * parent class)
      */
     public String formatDateTime(Calendar calendar) {
         StringBuilder sb = new StringBuilder();
         //YYYY-MM-DDTHH:MM:SS.MSSZ
         sb.append(calendar.get(Calendar.YEAR));
         sb.append('-');
         sb.append(formatTwo(calendar.get(Calendar.MONTH) + 1));
         sb.append('-');
         sb.append(formatTwo(calendar.get(Calendar.DAY_OF_MONTH)));
         sb.append('T');
         sb.append(formatTwo(calendar.get(Calendar.HOUR_OF_DAY)));
         sb.append(':');
         sb.append(formatTwo(calendar.get(Calendar.MINUTE)));
         sb.append(':');
         sb.append(formatTwo(calendar.get(Calendar.SECOND)));
         sb.append(".000Z");
         return sb.toString();
     }
 
     @Override
     public boolean sendLocalChanges(Serializer s) throws IOException {
         ContentResolver cr = mContext.getContentResolver();
 
         // Find any of our deleted items
         Cursor c = cr.query(Message.DELETED_CONTENT_URI, Message.LIST_PROJECTION,
                 MessageColumns.MAILBOX_KEY + '=' + mMailbox.mId, null, null);
         boolean first = true;
         // We keep track of the list of deleted item id's so that we can remove them from the
         // deleted table after the server receives our command
         mDeletedIdList.clear();
         try {
             while (c.moveToNext()) {
                if (first) {
                     s.start(Tags.SYNC_COMMANDS);
                     first = false;
                 }
                 // Send the command to delete this message
                s.start(Tags.SYNC_DELETE)
                    .data(Tags.SYNC_SERVER_ID, c.getString(Message.LIST_SERVER_ID_COLUMN))
                    .end(); // SYNC_DELETE
                 mDeletedIdList.add(c.getLong(Message.LIST_ID_COLUMN));
             }
         } finally {
             c.close();
         }
 
         // Find our trash mailbox, since deletions will have been moved there...
         long trashMailboxId =
             Mailbox.findMailboxOfType(mContext, mMailbox.mAccountKey, Mailbox.TYPE_TRASH);
 
         // Do the same now for updated items
         c = cr.query(Message.UPDATED_CONTENT_URI, Message.LIST_PROJECTION,
                 MessageColumns.MAILBOX_KEY + '=' + mMailbox.mId, null, null);
 
         // We keep track of the list of updated item id's as we did above with deleted items
         mUpdatedIdList.clear();
         try {
             while (c.moveToNext()) {
                 long id = c.getLong(Message.LIST_ID_COLUMN);
                 // Say we've handled this update
                 mUpdatedIdList.add(id);
                 // We have the id of the changed item.  But first, we have to find out its current
                 // state, since the updated table saves the opriginal state
                 Cursor currentCursor = cr.query(ContentUris.withAppendedId(Message.CONTENT_URI, id),
                         UPDATES_PROJECTION, null, null, null);
                 try {
                     // If this item no longer exists (shouldn't be possible), just move along
                     if (!currentCursor.moveToFirst()) {
                          continue;
                     }

                     // If the message is now in the trash folder, it has been deleted by the user
                     if (currentCursor.getLong(UPDATES_MAILBOX_KEY_COLUMN) == trashMailboxId) {
                          if (first) {
                             s.start(Tags.SYNC_COMMANDS);
                             first = false;
                         }
                         // Send the command to delete this message
                        s.start(Tags.SYNC_DELETE)
                            .data(Tags.SYNC_SERVER_ID,
                                    currentCursor.getString(UPDATES_SERVER_ID_COLUMN))
                            .end(); // SYNC_DELETE
                         continue;
                     }
 
                     boolean flagChange = false;
                     boolean readChange = false;
 
                     int flag = 0;
 
                     // We can only send flag changes to the server in 12.0 or later
                     if (mService.mProtocolVersionDouble >= 12.0) {
                         flag = currentCursor.getInt(UPDATES_FLAG_COLUMN);
                         if (flag != c.getInt(Message.LIST_FAVORITE_COLUMN)) {
                             flagChange = true;
                         }
                     }
 
                     int read = currentCursor.getInt(UPDATES_READ_COLUMN);
                     if (read != c.getInt(Message.LIST_READ_COLUMN)) {
                         readChange = true;
                     }
 
                     if (!flagChange && !readChange) {
                         // In this case, we've got nothing to send to the server
                         continue;
                     }
 
                     if (first) {
                         s.start(Tags.SYNC_COMMANDS);
                         first = false;
                     }
                     // Send the change to "read" and "favorite" (flagged)
                     s.start(Tags.SYNC_CHANGE)
                         .data(Tags.SYNC_SERVER_ID, c.getString(Message.LIST_SERVER_ID_COLUMN))
                         .start(Tags.SYNC_APPLICATION_DATA);
                     if (readChange) {
                         s.data(Tags.EMAIL_READ, Integer.toString(read));
                     }
                     // "Flag" is a relatively complex concept in EAS 12.0 and above.  It is not only
                     // the boolean "favorite" that we think of in Gmail, but it also represents a
                     // follow up action, which can include a subject, start and due dates, and even
                     // recurrences.  We don't support any of this as yet, but EAS 12.0 and higher
                     // require that a flag contain a status, a type, and four date fields, two each
                     // for start date and end (due) date.
                     if (flagChange) {
                         if (flag != 0) {
                             // Status 2 = set flag
                             s.start(Tags.EMAIL_FLAG).data(Tags.EMAIL_FLAG_STATUS, "2");
                             // "FollowUp" is the standard type
                             s.data(Tags.EMAIL_FLAG_TYPE, "FollowUp");
                             long now = System.currentTimeMillis();
                             Calendar calendar =
                                 GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
                             calendar.setTimeInMillis(now);
                             // Flags are required to have a start date and end date (duplicated)
                             // First, we'll set the current date/time in GMT as the start time
                             String utc = formatDateTime(calendar);
                             s.data(Tags.TASK_START_DATE, utc).data(Tags.TASK_UTC_START_DATE, utc);
                             // And then we'll use one week from today for completion date
                             calendar.setTimeInMillis(now + 1*WEEKS);
                             utc = formatDateTime(calendar);
                             s.data(Tags.TASK_DUE_DATE, utc).data(Tags.TASK_UTC_DUE_DATE, utc);
                             s.end();
                         } else {
                             s.tag(Tags.EMAIL_FLAG);
                         }
                     }
                     s.end().end(); // SYNC_APPLICATION_DATA, SYNC_CHANGE
                 } finally {
                     currentCursor.close();
                 }
             }
         } finally {
             c.close();
         }
 
         if (!first) {
             s.end(); // SYNC_COMMANDS
         }
         return false;
     }
 }
