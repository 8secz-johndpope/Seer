 package ch.cyberduck.core.gdocs;
 
 /*
  * Copyright (c) 2002-2010 David Kocher. All rights reserved.
  *
  * http://cyberduck.ch/
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * Bug fixes, suggestions and comments should be sent to:
  * dkocher@cyberduck.ch
  */
 
 import ch.cyberduck.core.*;
 import ch.cyberduck.core.i18n.Locale;
 import ch.cyberduck.core.io.BandwidthThrottle;
 import ch.cyberduck.core.serializer.Deserializer;
 import ch.cyberduck.core.serializer.Serializer;
 import ch.cyberduck.core.threading.ThreadPool;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.log4j.Logger;
 
 import com.google.gdata.client.CoreErrorDomain;
 import com.google.gdata.client.DocumentQuery;
 import com.google.gdata.client.GoogleAuthTokenFactory;
 import com.google.gdata.client.Service;
 import com.google.gdata.client.http.HttpGDataRequest;
 import com.google.gdata.client.spreadsheet.SpreadsheetService;
 import com.google.gdata.data.*;
 import com.google.gdata.data.acl.AclEntry;
 import com.google.gdata.data.acl.AclFeed;
 import com.google.gdata.data.acl.AclRole;
 import com.google.gdata.data.acl.AclScope;
 import com.google.gdata.data.docs.*;
 import com.google.gdata.data.media.MediaMultipart;
 import com.google.gdata.data.media.MediaSource;
 import com.google.gdata.data.media.MediaStreamSource;
 import com.google.gdata.util.ContentType;
 import com.google.gdata.util.NotImplementedException;
 import com.google.gdata.util.ServiceException;
 
 import javax.mail.MessagingException;
 import java.io.*;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.text.MessageFormat;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 
 public class GDPath extends Path {
     private static Logger log = Logger.getLogger(GDPath.class);
 
     private static class Factory extends PathFactory<GDSession> {
         @Override
         protected Path create(GDSession session, String path, int type) {
             return new GDPath(session, path, type);
         }
 
         @Override
         protected Path create(GDSession session, String parent, String name, int type) {
             return new GDPath(session, parent, name, type);
         }
 
         @Override
         protected Path create(GDSession session, String parent, Local file) {
             return new GDPath(session, parent, file);
         }
 
         @Override
         protected <T> Path create(GDSession session, T dict) {
             return new GDPath(session, dict);
         }
     }
 
     public static PathFactory factory() {
         return new Factory();
     }
 
     @Override
     protected void init(Deserializer dict) {
         String resourceIdObj = dict.stringForKey("ResourceId");
         if(resourceIdObj != null) {
             this.setResourceId(resourceIdObj);
         }
         String exportUriObj = dict.stringForKey("ExportUri");
         if(exportUriObj != null) {
             this.setExportUri(exportUriObj);
         }
         String documentTypeObj = dict.stringForKey("DocumentType");
         if(documentTypeObj != null) {
             this.setDocumentType(documentTypeObj);
         }
         super.init(dict);
     }
 
     @Override
     protected <S> S getAsDictionary(Serializer dict) {
         if(resourceId != null) {
             dict.setStringForKey(resourceId, "ResourceId");
         }
         if(exportUri != null) {
             dict.setStringForKey(exportUri, "ExportUri");
         }
         if(documentType != null) {
             dict.setStringForKey(documentType, "DocumentType");
         }
         return super.<S>getAsDictionary(dict);
     }
 
     private final GDSession session;
 
     protected GDPath(GDSession s, String parent, String name, int type) {
         super(parent, name, type);
         this.session = s;
     }
 
     protected GDPath(GDSession s, String path, int type) {
         super(path, type);
         this.session = s;
     }
 
     protected GDPath(GDSession s, String parent, Local file) {
         super(parent, file);
         this.session = s;
     }
 
     protected <T> GDPath(GDSession s, T dict) {
         super(dict);
         this.session = s;
     }
 
     /**
      * Arbitrary file type not converted to Google Docs.
      */
     private static final String DOCUMENT_FILE_TYPE = "file";
 
     /**
      * Kind of document or folder.
      */
     private String documentType;
 
     public String getDocumentType() {
         if(null == documentType) {
             if(attributes().isDirectory()) {
                 return FolderEntry.LABEL;
             }
             // Arbitrary file type not converted to Google Docs.
             return DOCUMENT_FILE_TYPE;
         }
         return documentType;
     }
 
     public void setDocumentType(String documentType) {
         this.documentType = documentType;
     }
 
     /**
      * URL from where the document can be downloaded.
      */
     private String exportUri;
 
     /**
      * @return Download URL without export format.
      */
     public String getExportUri() {
         if(StringUtils.isBlank(exportUri)) {
             log.warn("Refetching Export URI for " + this.toString());
             AttributedList<AbstractPath> l = this.getParent().children();
             if(l.contains(this.getReference())) {
                 exportUri = ((GDPath) l.get(this.getReference())).getExportUri();
             }
             else {
                 log.error("Missing Export URI for " + this.toString());
             }
         }
         return exportUri;
     }
 
     public void setExportUri(String exportUri) {
         this.exportUri = exportUri;
     }
 
     /**
      * Resource ID. Contains both the document type and document ID.
      * For folders this is <code>folder:0BwoD_34YE1B4ZDFiZmMwNTAtMGFiMy00MmQ1LTg1NTQtNmFiYWFkNTg2MTQ3</code>
      */
     private String resourceId;
 
     public String getResourceId() {
         if(StringUtils.isBlank(resourceId)) {
             log.warn("Refetching Resource ID for " + this.toString());
             AttributedList<AbstractPath> l = this.getParent().children();
             if(l.contains(this.getReference())) {
                 resourceId = ((GDPath) l.get(this.getReference())).getResourceId();
             }
             else {
                 log.error("Missing Resource ID for " + this.toString());
             }
         }
         return resourceId;
     }
 
     public void setResourceId(String resourceId) {
         this.resourceId = resourceId;
     }
 
     private String documentUri;
 
     /**
      * @return The URL to the document editable in the web browser
      */
     public String getDocumentUri() {
         return documentUri;
     }
 
     public void setDocumentUri(String documentUri) {
         this.documentUri = documentUri;
     }
 
     private String getDocumentId() {
         // Removing document type from resourceId gives us the documentId
         return StringUtils.removeStart(this.getResourceId(), this.getDocumentType() + ":");
     }
 
     /**
      * @return Includes the protocol and hostname only
      */
     protected StringBuilder getFeed() {
         final StringBuilder feed = new StringBuilder(this.getSession().getHost().getProtocol().getScheme()).append("://");
         feed.append(this.getSession().getHost().getHostname());
         feed.append("/feeds/default/private/full/");
         return feed;
     }
 
     protected String getResourceFeed() throws MalformedURLException {
         return this.getFeed().append(this.getResourceId()).toString();
     }
 
     protected String getFolderFeed() throws MalformedURLException {
         final StringBuilder feed = this.getFeed();
         if(this.isRoot()) {
             return feed.append("folder%3Aroot/contents").toString();
         }
         return feed.append("folder%3A").append(this.getDocumentId()).append("/contents").toString();
     }
 
     protected String getAclFeed() throws MalformedURLException {
         final StringBuilder feed = new StringBuilder(this.getResourceFeed());
         return feed.append("/acl").toString();
     }
 
     public String getRevisionsFeed() throws MalformedURLException {
         final StringBuilder feed = new StringBuilder(this.getResourceFeed());
         return feed.append("/revisions").toString();
     }
 
     @Override
     public void readSize() {
         ;
     }
 
     @Override
     public void readTimestamp() {
         throw new UnsupportedOperationException();
     }
 
     @Override
     public void readAcl() {
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Getting permission of {0}", "Status"),
                     this.getName()));
             Acl acl = new Acl();
             AclFeed feed = this.getSession().getClient().getFeed(new URL(this.getAclFeed()), AclFeed.class);
             for(AclEntry entry : feed.getEntries()) {
                 AclScope scope = entry.getScope();
                 AclScope.Type type = scope.getType();
                 AclRole role = entry.getRole();
                 if(type.equals(AclScope.Type.USER)) {
                     // Only editable if not owner of document. Changing owner is not supported.
                     boolean editable = !role.getValue().equals(AclRole.OWNER.getValue());
                     acl.addAll(new Acl.EmailUser(scope.getValue(), editable),
                             new Acl.Role(role.getValue(), editable));
                 }
                 else if(type.equals(AclScope.Type.DOMAIN)) {
                     // Google Apps Domain grant.
                     acl.addAll(new Acl.DomainUser(scope.getValue()), new Acl.Role(role.getValue()));
                 }
                 else if(type.equals(AclScope.Type.GROUP)) {
                     // Google Group email grant
                     acl.addAll(new Acl.GroupUser(scope.getValue(), true), new Acl.Role(role.getValue()));
                 }
                 else if(type.equals(AclScope.Type.DEFAULT)) {
                     // Value of scope is null. Default access for non authenticated
                     // users. Publicly shared with all users.
                     acl.addAll(new Acl.CanonicalUser(AclScope.Type.DEFAULT.name(), Locale.localizedString("Public"), false),
                             new Acl.Role(role.getValue()));
                 }
                 else {
                     log.warn("Unsupported scope:" + type);
                 }
             }
             this.attributes().setAcl(acl);
         }
         catch(IOException e) {
             this.error("Cannot read file attributes", e);
         }
         catch(ServiceException e) {
             this.error("Cannot read file attributes", e);
         }
     }
 
     @Override
     public void writeAcl(Acl acl, boolean recursive) {
         try {
             // Delete all previous ACLs before inserting updated set.
             AclFeed feed = this.getSession().getClient().getFeed(new URL(this.getAclFeed()), AclFeed.class);
             for(AclEntry entry : feed.getEntries()) {
                 if(entry.getRole().toString().equals(AclRole.OWNER.toString())) {
                     // Do not remove owner of document
                     continue;
                 }
                 entry.delete();
             }
             for(Acl.User user : acl.keySet()) {
                 if(!user.isValid()) {
                     continue;
                 }
                 if(!user.isEditable()) {
                     continue;
                 }
                 // The API supports sharing permissions on multiple levels. These values
                 // correspond to the <gAcl:scope> type attribute
                 AclScope scope = null;
                 if(user instanceof Acl.EmailUser) {
                     // a user's email address. Creating an ACL entry that shares a document or folder with users will notify 
                     // relevant users via email that they have new access to the document or folder
                     scope = new AclScope(AclScope.Type.USER, user.getIdentifier());
                 }
                 else if(user instanceof Acl.GroupUser) {
                     // a Google Group email address
                     scope = new AclScope(AclScope.Type.GROUP, user.getIdentifier());
                 }
                 else if(user instanceof Acl.DomainUser) {
                     // a Google Apps domain.
                     scope = new AclScope(AclScope.Type.DOMAIN, user.getIdentifier());
                 }
                 else if(user instanceof Acl.CanonicalUser) {
                     if(user.getIdentifier().equals(AclScope.Type.DEFAULT.name())) {
                         // Publicly shared with all users
                         scope = new AclScope(AclScope.Type.DEFAULT, null);
                     }
                 }
                 if(null == scope) {
                     log.warn("Unsupported scope:" + user);
                     continue;
                 }
                 for(Acl.Role role : acl.get(user)) {
                     if(!role.isValid()) {
                         continue;
                     }
                     AclEntry entry = new AclEntry();
                     entry.setScope(scope);
                     entry.setRole(new AclRole(role.getName()));
                     // Insert updated ACL entry for scope
                     this.getSession().getClient().insert(new URL(this.getAclFeed()), entry);
                 }
             }
         }
         catch(IOException e) {
             this.error("Cannot change permissions", e);
         }
         catch(ServiceException e) {
             this.error("Cannot change permissions", e);
         }
         finally {
             this.attributes().clear(false, false, true, false);
         }
         if(attributes().isDirectory()) {
             if(recursive) {
                 // All child objects of the folder reflect will reflect the new
                 // sharing permission regardless.
             }
         }
     }
 
     @Override
     public GDSession getSession() {
         return session;
     }
 
     @Override
     protected void download(BandwidthThrottle throttle, StreamListener listener, boolean check) {
         if(attributes().isFile()) {
             OutputStream out = null;
             InputStream in = null;
             try {
                 if(check) {
                     this.getSession().check();
                 }
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Downloading {0}", "Status"),
                         this.getName()));
 
                 MediaContent mc = new MediaContent();
                 StringBuilder uri = new StringBuilder(this.getExportUri());
                 final String type = this.getDocumentType();
                 final GoogleAuthTokenFactory.UserToken token
                         = (GoogleAuthTokenFactory.UserToken) this.getSession().getClient().getAuthTokenFactory().getAuthToken();
                 try {
                     if(type.equals(SpreadsheetEntry.LABEL)) {
                         // Authenticate against the Spreadsheets API to obtain an auth token
                         SpreadsheetService spreadsheet = new SpreadsheetService(this.getSession().getUserAgent());
                         final Credentials credentials = this.getSession().getHost().getCredentials();
                         spreadsheet.setUserCredentials(credentials.getUsername(), credentials.getPassword());
                         // Substitute the spreadsheets token for the docs token
                         this.getSession().getClient().setUserToken(
                                 ((GoogleAuthTokenFactory.UserToken) spreadsheet.getAuthTokenFactory().getAuthToken()).getValue());
                     }
                     if(StringUtils.isNotEmpty(getExportFormat(type))) {
                         uri.append("&exportFormat=").append(getExportFormat(type));
                     }
                     mc.setUri(uri.toString());
                     MediaSource ms = this.getSession().getClient().getMedia(mc);
                     in = ms.getInputStream();
                     if(null == in) {
                         throw new IOException("Unable opening data stream");
                     }
                     out = this.getLocal().getOutputStream(this.status().isResume());
                     this.download(in, out, throttle, listener);
                 }
                 finally {
                     // Restore docs token for our DocList client
                     this.getSession().getClient().setUserToken(token.getValue());
                 }
             }
             catch(IOException e) {
                 this.error("Download failed", e);
             }
             catch(ServiceException e) {
                 this.error("Download failed", e);
             }
             finally {
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
             }
         }
     }
 
     /**
      * Google Apps Premier domains can upload files of arbitrary type. Uploading an arbitrary file is
      * the same as uploading documents (with and without metadata), except there is no
      * restriction on the file's Content-Type. Unlike normal document uploads, arbitrary
      * file uploads preserve their original format/extension, meaning there is no loss in
      * fidelity when the file is stored in Google Docs.
      * <p/>
      * By default, uploaded document files will be converted to a native Google Docs format.
      * For example, an .xls upload will create a Google Spreadsheet. To keep the file as an Excel
      * spreadsheet (and therefore upload the file as an arbitrary file), specify the convert=false
      * parameter to preserve the original format. The convert parameter is true by default for
      * document files. The parameter will be ignored for types that cannot be
      * converted (e.g. .exe, .mp3, .mov, etc.).
      *
      * @param throttle The bandwidth limit
      * @param listener The stream listener to notify about bytes received and sent
      * @param check    Check for open connection and open if needed before transfer
      */
     @Override
     protected void upload(BandwidthThrottle throttle, StreamListener listener, boolean check) {
         try {
             if(attributes().isFile()) {
                 if(check) {
                     this.getSession().check();
                 }
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Uploading {0}", "Status"),
                         this.getName()));
 
                 InputStream in = null;
                 OutputStream out = null;
                 try {
                     final String mime = this.getLocal().getMimeType();
                     final MediaStreamSource source = new MediaStreamSource(this.getLocal().getInputStream(), mime,
                             new DateTime(this.attributes().getModificationDate()),
                             this.getLocal().attributes().getSize());
                     if(this.exists()) {
                         // First, fetch entry using the resourceId
                         URL url = new URL(this.getResourceFeed());
                         final DocumentListEntry updated = this.getSession().getClient().getEntry(url, DocumentListEntry.class);
                         updated.setMediaSource(source);
                         updated.updateMedia(true);
                     }
                     else {
                         final MediaContent content = new MediaContent();
                         content.setMediaSource(source);
                         content.setMimeType(new ContentType(mime));
                         content.setLength(this.getLocal().attributes().getSize());
                         final DocumentListEntry document = new DocumentListEntry();
                         document.setContent(content);
                         document.setTitle(new PlainTextConstruct(this.getName()));
 
                         this.getSession().message(MessageFormat.format(Locale.localizedString("Uploading {0}", "Status"),
                                 this.getName()));
                         status().setResume(false);
 
                         String feed = ((GDPath) this.getParent()).getFolderFeed();
                         StringBuilder url = new StringBuilder(feed);
                         if(this.isOcrSupported()) {
                             // Image file type
                             url.append("?ocr=").append(Preferences.instance().getProperty("google.docs.upload.ocr"));
                         }
                         else if(this.isConversionSupported()) {
                             // Convertible to Google Docs file type
                             url.append("?convert=").append(Preferences.instance().getProperty("google.docs.upload.convert"));
                         }
                         Service.GDataRequest request = null;
                         try {
                             // Write as MIME multipart containing the entry and media.  Use the
                             // content type from the multipart since this contains auto-generated
                             // boundary attributes.
                             final MediaMultipart multipart = new MediaMultipart(document, document.getMediaSource());
                             request = this.getSession().getClient().createRequest(
                                     Service.GDataRequest.RequestType.INSERT, new URL(url.toString()),
                                     new ContentType(multipart.getContentType()));
                             if(request instanceof HttpGDataRequest) {
                            // No internal buffering of request with a known content length
                            // Size is incorect because of additional MIME header
 //                                ((HttpGDataRequest)request).getConnection().setFixedLengthStreamingMode(
 //                                        (int) this.getLocal().attributes().getSize()
 //                                );
                                // Use chunked upload with default chunk size.
                                ((HttpGDataRequest) request).getConnection().setChunkedStreamingMode(0);
                             }
                             out = request.getRequestStream();
 
                             final PipedOutputStream pipe = new PipedOutputStream();
                             in = new PipedInputStream(pipe);
                             ThreadPool.instance().execute(new Runnable() {
                                 public void run() {
                                     try {
                                         multipart.writeTo(pipe);
                                         pipe.flush();
                                         pipe.close();
                                     }
                                     catch(IOException e) {
                                         log.error(e.getMessage());
                                     }
                                     catch(MessagingException e) {
                                         log.error(e.getMessage());
                                     }
                                 }
                             });
                             this.upload(out, in, throttle, listener);
                             // Parse response for HTTP error message.
                             try {
                                 request.execute();
                             }
                             catch(ServiceException e) {
                                 this.status().setComplete(false);
                                 throw e;
                             }
                         }
                         catch(MessagingException e) {
                             throw new ServiceException(
                                     CoreErrorDomain.ERR.cantWriteMimeMultipart, e);
                         }
                         finally {
                             if(request != null) {
                                 request.end();
                             }
                         }
                     }
                 }
                 finally {
                     IOUtils.closeQuietly(in);
                     IOUtils.closeQuietly(out);
                 }
             }
         }
         catch(ServiceException e) {
             this.error("Upload failed", e);
         }
         catch(IOException e) {
             this.error("Upload failed", e);
         }
     }
 
     /**
      * @return True for image formats supported by OCR
      */
     protected boolean isOcrSupported() {
         return this.getMimeType().endsWith("png") || this.getMimeType().endsWith("jpeg")
                 || this.getMimeType().endsWith("gif");
     }
 
     /**
      * @return True if the document, spreadsheet or presentation format is recognized by Google Docs.
      */
     protected boolean isConversionSupported() {
         // The convert parameter will be ignored for types that cannot be converted. Therefore we
         // can always return true.
         return true;
     }
 
     @Override
     public AttributedList<Path> list() {
         final AttributedList<Path> children = new AttributedList<Path>();
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Listing directory {0}", "Status"),
                     this.getName()));
 
             this.getSession().setWorkdir(this);
 
             children.addAll(this.list(new DocumentQuery(new URL(this.getFolderFeed()))));
         }
         catch(ServiceException e) {
             log.warn("Listing directory failed:" + e.getMessage());
             children.attributes().setReadable(false);
             if(this.cache().isEmpty()) {
                 this.error(e.getMessage(), e);
             }
         }
         catch(IOException e) {
             log.warn("Listing directory failed:" + e.getMessage());
             children.attributes().setReadable(false);
             if(this.cache().isEmpty()) {
                 this.error(e.getMessage(), e);
             }
         }
         return children;
     }
 
     /**
      * @param query
      * @return
      * @throws ServiceException
      * @throws IOException
      */
     private AttributedList<Path> list(DocumentQuery query) throws ServiceException, IOException {
         final AttributedList<Path> children = new AttributedList<Path>();
         DocumentListFeed pager = this.getSession().getClient().getFeed(query, DocumentListFeed.class);
         do {
             for(final DocumentListEntry entry : pager.getEntries()) {
                 log.debug("Resource:" + entry.getResourceId());
                 boolean include = false;
                 for(Person person : entry.getAuthors()) {
                     log.debug("Author of document " + entry.getResourceId() + ":" + person.getEmail());
                     if(person.getEmail().equals(this.getSession().getHost().getCredentials().getUsername())) {
                         include = true;
                         break;
                     }
                 }
                 if(!include) {
                     log.warn("Skip document with different owner:" + entry);
                     continue;
                 }
                 final String type = entry.getType();
                 GDPath path = new GDPath(this.getSession(), this.getAbsolute(), entry.getTitle().getPlainText(),
                         FolderEntry.LABEL.equals(type) ? Path.DIRECTORY_TYPE : Path.FILE_TYPE);
                 path.setParent(this);
                 path.setDocumentType(type);
                 // Download URL
                 path.setExportUri(((OutOfLineContent) entry.getContent()).getUri());
                 // Link to Google Docs Editor
                 path.setDocumentUri(entry.getDocumentLink().getHref());
                 path.setResourceId(entry.getResourceId());
                 // Add unique document ID as checksum
                 path.attributes().setChecksum(entry.getEtag());
                 if(null != entry.getMediaSource()) {
                     path.attributes().setSize(entry.getMediaSource().getContentLength());
                 }
                 if(entry.getQuotaBytesUsed() > 0) {
                     path.attributes().setSize(entry.getQuotaBytesUsed());
                 }
                 final DateTime lastViewed = entry.getLastViewed();
                 if(lastViewed != null) {
                     path.attributes().setAccessedDate(lastViewed.getValue());
                 }
                 for(Person person : entry.getAuthors()) {
                     path.attributes().setOwner(person.getEmail());
                 }
                 final DateTime updated = entry.getUpdated();
                 if(updated != null) {
                     path.attributes().setModificationDate(updated.getValue());
                 }
                 if(children.contains(path.getReference())) {
                     // Google Docs allows files to be named the same. Not really a duplicate.
                     path.attributes().setDuplicate(true);
                     path.setReference(null);
                 }
                 // Add to listing
                 children.add(path);
                 if(path.attributes().isFile()) {
                     // Fetch revisions
                     if(Preferences.instance().getBoolean("google.docs.revisions.enable")) {
                         try {
                             final List<RevisionEntry> revisions = this.getSession().getClient().getFeed(
                                     new URL(path.getRevisionsFeed()), RevisionFeed.class).getEntries();
                             Collections.sort(revisions, new Comparator<RevisionEntry>() {
                                 public int compare(RevisionEntry o1, RevisionEntry o2) {
                                     return o1.getUpdated().compareTo(o2.getUpdated());
                                 }
                             });
                             int i = 0;
                             for(RevisionEntry revisionEntry : revisions) {
                                 GDPath revision = new GDPath(this.getSession(), revisionEntry.getTitle().getPlainText(),
                                         FolderEntry.LABEL.equals(type) ? Path.DIRECTORY_TYPE : Path.FILE_TYPE);
                                 revision.setParent(this);
                                 revision.setDocumentType(type);
                                 revision.setExportUri(((OutOfLineContent) revisionEntry.getContent()).getUri());
                                 final long size = ((OutOfLineContent) revisionEntry.getContent()).getLength();
                                 if(size > 0) {
                                     revision.attributes().setSize(size);
                                 }
                                 revision.attributes().setOwner(revisionEntry.getModifyingUser().getName());
                                 revision.attributes().setModificationDate(revisionEntry.getUpdated().getValue());
                                 // Versioning is enabled if non null.
                                 revision.attributes().setVersionId(revisionEntry.getVersionId());
                                 revision.attributes().setChecksum(revisionEntry.getEtag());
                                 revision.attributes().setRevision(++i);
                                 revision.attributes().setDuplicate(true);
                                 // Add to listing
                                 children.add(revision);
                             }
                         }
                         catch(NotImplementedException e) {
                             log.error("No revisions available:" + e.getMessage());
                         }
                     }
                 }
             }
             Link next = pager.getNextLink();
             if(null == next) {
                 // No link to next page.
                 break;
             }
             // More pages available
             pager = this.getSession().getClient().getFeed(new URL(next.getHref()), DocumentListFeed.class);
         }
         while(pager.getEntries().size() > 0);
         return children;
     }
 
     @Override
     public String getMimeType() {
         if(attributes().isFile()) {
             final String exportFormat = getExportFormat(this.getDocumentType());
             if(StringUtils.isNotEmpty(exportFormat)) {
                 return getMimeType(exportFormat);
             }
         }
         return super.getMimeType();
     }
 
     @Override
     public String getExtension() {
         if(attributes().isFile()) {
             final String exportFormat = getExportFormat(this.getDocumentType());
             if(StringUtils.isNotEmpty(exportFormat)) {
                 return exportFormat;
             }
         }
         return super.getExtension();
     }
 
     @Override
     public String getName() {
         if(attributes().isFile()) {
             final String exportFormat = getExportFormat(this.getDocumentType());
             if(StringUtils.isNotEmpty(exportFormat)) {
                 if(!super.getName().endsWith(exportFormat)) {
                     return super.getName() + "." + exportFormat;
                 }
             }
         }
         return super.getName();
     }
 
     /**
      * @param type The document type
      * @return
      */
     protected static String getExportFormat(String type) {
         if(type.equals(DocumentEntry.LABEL)) {
             return Preferences.instance().getProperty("google.docs.export.document");
         }
         if(type.equals(PresentationEntry.LABEL)) {
             return Preferences.instance().getProperty("google.docs.export.presentation");
         }
         if(type.equals(SpreadsheetEntry.LABEL)) {
             return Preferences.instance().getProperty("google.docs.export.spreadsheet");
         }
         if(type.equals(DOCUMENT_FILE_TYPE)) {
             // For files not converted to Google Docs.
             // DOCUMENT_FILE_TYPE
             log.debug("No output format conversion for document type:" + type);
             return null;
         }
         log.warn("Unknown document type:" + type);
         return null;
     }
 
     @Override
     public void mkdir() {
         if(this.attributes().isDirectory()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Making directory {0}", "Status"),
                         this.getName()));
 
                 DocumentListEntry folder = new FolderEntry();
                 folder.setTitle(new PlainTextConstruct(this.getName()));
                 try {
                     this.getSession().getClient().insert(new URL(((GDPath) this.getParent()).getFolderFeed()), folder);
                 }
                 catch(ServiceException e) {
                     throw new IOException(e.getMessage());
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
     public void readUnixPermission() {
         throw new UnsupportedOperationException();
     }
 
     @Override
     public void writeUnixPermission(Permission perm, boolean recursive) {
         throw new UnsupportedOperationException();
     }
 
     @Override
     public void writeTimestamp(long created, long modified, long accessed) {
         throw new UnsupportedOperationException();
     }
 
     @Override
     public void delete() {
         try {
             if(this.attributes().isDuplicate()) {
                 log.warn("Cannot delete revision " + this.attributes().getRevision());
                 return;
             }
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                     this.getName()));
             try {
                 this.getSession().getClient().delete(
                         new URL(this.getResourceFeed()), this.attributes().getChecksum());
             }
             catch(ServiceException e) {
                 throw new IOException(e.getMessage());
             }
             catch(MalformedURLException e) {
                 throw new IOException(e.getMessage());
             }
             // The directory listing is no more current
             this.getParent().invalidate();
         }
         catch(IOException e) {
             if(this.attributes().isFile()) {
                 this.error("Cannot delete file", e);
             }
             if(this.attributes().isDirectory()) {
                 this.error("Cannot delete folder", e);
             }
         }
     }
 
     @Override
     public void rename(AbstractPath renamed) {
         try {
             this.getSession().check();
             this.getSession().message(MessageFormat.format(Locale.localizedString("Renaming {0} to {1}", "Status"),
                     this.getName(), renamed));
 
             DocumentListEntry moved = new DocumentListEntry();
             moved.setId("https://docs.google.com/feeds/id/" + this.getResourceId());
             if(this.getParent().equals(renamed.getParent())) {
                 // Rename file
                 moved.setTitle(new PlainTextConstruct(renamed.getName()));
                 try {
                     // Move into new folder
                     this.getSession().getClient().update(new URL(this.getResourceFeed()), moved, this.attributes().getChecksum());
                 }
                 catch(ServiceException e) {
                     throw new IOException(e.getMessage());
                 }
                 catch(MalformedURLException e) {
                     throw new IOException(e.getMessage());
                 }
             }
             else {
                 try {
                     // Move into new folder
                     final DocumentListEntry update
                             = this.getSession().getClient().insert(new URL(((GDPath) renamed.getParent()).getFolderFeed()), moved);
                     // Move out of previous folder
                     this.getSession().getClient().delete(new URL((((GDPath) this.getParent()).getFolderFeed()) +
                             "/" + this.getResourceId()), update.getEtag());
                 }
                 catch(ServiceException e) {
                     throw new IOException(e.getMessage());
                 }
                 catch(MalformedURLException e) {
                     throw new IOException(e.getMessage());
                 }
             }
             // The directory listing is no more current
             this.getParent().invalidate();
             renamed.getParent().invalidate();
         }
         catch(IOException e) {
             if(this.attributes().isFile()) {
                 this.error("Cannot rename file", e);
             }
             if(this.attributes().isDirectory()) {
                 this.error("Cannot rename folder", e);
             }
         }
     }
 
     @Override
     public void touch() {
         if(this.attributes().isFile()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Uploading {0}", "Status"),
                         this.getName()));
 
                 DocumentListEntry file = new DocumentEntry();
                 file.setTitle(new PlainTextConstruct(this.getName()));
                 try {
                     this.getSession().getClient().insert(new URL(((GDPath) this.getParent()).getFolderFeed()), file);
                 }
                 catch(ServiceException e) {
                     throw new IOException(e.getMessage());
                 }
                 // The directory listing is no more current
                 this.getParent().invalidate();
             }
             catch(IOException e) {
                 this.error("Cannot create file", e);
             }
         }
     }
 
     @Override
     public String toHttpURL() {
         return this.getDocumentUri();
     }
 }
