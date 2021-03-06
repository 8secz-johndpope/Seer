 package ch.cyberduck.core.s3;
 
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
 import ch.cyberduck.core.Permission;
 import ch.cyberduck.core.cloud.CloudPath;
 import ch.cyberduck.core.i18n.Locale;
 import ch.cyberduck.core.io.BandwidthThrottle;
 import ch.cyberduck.ui.DateFormatterFactory;
 
 import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.log4j.Logger;
 import org.jets3t.service.ServiceException;
 import org.jets3t.service.StorageObjectsChunk;
 import org.jets3t.service.VersionOrDeleteMarkersChunk;
 import org.jets3t.service.acl.*;
 import org.jets3t.service.model.*;
 import org.jets3t.service.utils.ServiceUtils;
 
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.security.DigestInputStream;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.text.MessageFormat;
 import java.text.SimpleDateFormat;
 import java.util.*;
 import java.util.concurrent.*;
 
 /**
  * @version $Id$
  */
 public class S3Path extends CloudPath {
     private static Logger log = Logger.getLogger(S3Path.class);
 
     private static class Factory extends PathFactory<S3Session> {
         @Override
         protected Path create(S3Session session, String path, int type) {
             return new S3Path(session, path, type);
         }
 
         @Override
         protected Path create(S3Session session, String parent, String name, int type) {
             return new S3Path(session, parent, name, type);
         }
 
         @Override
         protected Path create(S3Session session, String parent, Local file) {
             return new S3Path(session, parent, file);
         }
 
         @Override
         protected <T> Path create(S3Session session, T dict) {
             return new S3Path(session, dict);
         }
     }
 
     public static PathFactory factory() {
         return new Factory();
     }
 
     private final S3Session session;
 
     protected S3Path(S3Session s, String parent, String name, int type) {
         super(parent, name, type);
         this.session = s;
     }
 
     protected S3Path(S3Session s, String path, int type) {
         super(path, type);
         this.session = s;
     }
 
     protected S3Path(S3Session s, String parent, Local file) {
         super(parent, file);
         this.session = s;
     }
 
     protected <T> S3Path(S3Session s, T dict) {
         super(dict);
         this.session = s;
     }
 
     @Override
     public S3Session getSession() {
         return session;
     }
 
     /**
      * Object details not contained in standard listing.
      *
      * @see #getDetails()
      */
     protected StorageObject _details;
 
     /**
      * @return
      * @throws ServiceException
      */
     protected StorageObject getDetails() throws IOException, ServiceException {
         final String container = this.getContainerName();
         if(null == _details || !_details.isMetadataComplete()) {
             try {
                 if(this.attributes().isDuplicate()) {
                     _details = this.getSession().getClient().getVersionedObjectDetails(this.attributes().getVersionId(),
                             container, this.getKey());
                 }
                 else {
                     _details = this.getSession().getClient().getObjectDetails(container, this.getKey());
                 }
             }
             catch(ServiceException e) {
                 // Anonymous services can only get a publicly-readable object's details
                 log.warn("Cannot read object details:" + e.getMessage());
             }
         }
         if(null == _details) {
             log.warn("Cannot read object details.");
             S3Object object = new S3Object(this.getKey());
             object.setBucketName(this.getContainerName());
             return object;
         }
         return _details;
     }
 
     /**
      * Versioning support. Copy a previous version of the object into the same bucket.
      * The copied object becomes the latest version of that object and all object versions are preserved.
      */
     @Override
     public void revert() {
         if(this.attributes().isFile()) {
             try {
                 final S3Object destination = new S3Object(this.getKey());
                 // Keep same storage class
                 destination.setStorageClass(this.attributes().getStorageClass());
                 // Apply non standard ACL
                 if(Acl.EMPTY.equals(this.attributes().getAcl())) {
                     this.readAcl();
                 }
                 destination.setAcl(this.convert(this.attributes().getAcl()));
                 this.getSession().getClient().copyVersionedObject(this.attributes().getVersionId(),
                         this.getContainerName(), this.getKey(), this.getContainerName(), destination, false);
                 // The directory listing is no more current
                 this.getParent().invalidate();
             }
             catch(ServiceException e) {
                 this.error("Cannot revert file", e);
             }
             catch(IOException e) {
                 this.error("Cannot revert file", e);
             }
         }
     }
 
     /**
      * @return The ACL of the bucket or object. Return AccessControlList.REST_CANNED_PRIVATE if
      *         reading fails.
      */
     @Override
     public void readAcl() {
         try {
             final Credentials credentials = this.getSession().getHost().getCredentials();
             if(credentials.isAnonymousLogin()) {
                 return;
             }
             final String container = this.getContainerName();
             if(this.isContainer()) {
                 // This method can be performed by anonymous services, but can only succeed if the
                 // bucket's existing ACL already allows write access by the anonymous user.
                 // In general, you can only access the ACL of a bucket if the ACL already in place
                 // for that bucket (in S3) allows you to do so.
                 this.attributes().setAcl(this.convert(this.getSession().getClient().getBucketAcl(container)));
             }
             else if(attributes().isFile() || attributes().isPlaceholder()) {
                 AccessControlList list;
                 if(this.getSession().isVersioning(container)) {
                     list = this.getSession().getClient().getVersionedObjectAcl(this.attributes().getVersionId(),
                             container, this.getKey());
                 }
                 else {
                     // This method can be performed by anonymous services, but can only succeed if the
                     // object's existing ACL already allows read access by the anonymous user.
                     list = this.getSession().getClient().getObjectAcl(container, this.getKey());
                 }
                 this.attributes().setAcl(this.convert(list));
             }
         }
         catch(ServiceException e) {
             this.error("Cannot read file attributes", e);
         }
         catch(IOException e) {
             this.error("Cannot read file attributes", e);
         }
     }
 
     protected Acl convert(final AccessControlList list) {
         if(log.isDebugEnabled()) {
             try {
                 log.debug(list.toXml());
             }
             catch(ServiceException e) {
                 log.error(e.getMessage());
             }
         }
         Acl acl = new Acl();
         for(GrantAndPermission grant : list.getGrantAndPermissions()) {
             Acl.Role role = new Acl.Role(grant.getPermission().toString());
             if(grant.getGrantee() instanceof CanonicalGrantee) {
                 acl.addAll(new Acl.CanonicalUser(grant.getGrantee().getIdentifier(),
                         ((CanonicalGrantee) grant.getGrantee()).getDisplayName(), false), role);
             }
             else if(grant.getGrantee() instanceof EmailAddressGrantee) {
                 acl.addAll(new Acl.EmailUser(grant.getGrantee().getIdentifier()), role);
             }
             else if(grant.getGrantee() instanceof GroupGrantee) {
                 acl.addAll(new Acl.GroupUser(grant.getGrantee().getIdentifier()), role);
             }
         }
         return acl;
     }
 
     /**
      * Format to RFC 1123 timestamp
      * Expires: Thu, 01 Dec 1994 16:00:00 GMT
      */
     private SimpleDateFormat rfc1123 =
             new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.ENGLISH);
 
     {
         rfc1123.setTimeZone(TimeZone.getDefault());
     }
 
     private static final String METADATA_HEADER_EXPIRES = "Expires";
 
     /**
      * Implements http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.21
      *
      * @param expiration
      */
     public void setExpiration(final Date expiration) {
         try {
             this.getSession().check();
             // You can also copy an object and update its metadata at the same time. Perform a
             // copy-in-place  (with the same bucket and object names for source and destination)
             // to update an object's metadata while leaving the object's data unchanged.
             final StorageObject target = this.getDetails();
             target.addMetadata(METADATA_HEADER_EXPIRES, rfc1123.format(expiration));
             this.getSession().getClient().updateObjectMetadata(this.getContainerName(), target);
         }
         catch(ServiceException e) {
             this.error("Cannot write file attributes", e);
         }
         catch(IOException e) {
             this.error("Cannot write file attributes", e);
         }
     }
 
     public static final String METADATA_HEADER_CACHE_CONTROL = "Cache-Control";
 
     /**
      * Implements http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9
      *
      * @param maxage Timespan in seconds from when the file is requested
      */
     public void setCacheControl(final String maxage) {
         try {
             this.getSession().check();
             // You can also copy an object and update its metadata at the same time. Perform a
             // copy-in-place  (with the same bucket and object nexames for source and destination)
             // to update an object's metadata while leaving the object's data unchanged.
             final StorageObject target = this.getDetails();
             if(StringUtils.isEmpty(maxage)) {
                 target.removeMetadata(METADATA_HEADER_CACHE_CONTROL);
             }
             else {
                 target.addMetadata(METADATA_HEADER_CACHE_CONTROL, maxage);
             }
             this.getSession().getClient().updateObjectMetadata(this.getContainerName(), target);
         }
         catch(ServiceException e) {
             this.error("Cannot write file attributes", e);
         }
         catch(IOException e) {
             this.error("Cannot write file attributes", e);
         }
     }
 
     @Override
     public void readMetadata() {
         if(attributes().isFile() || attributes().isPlaceholder()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Reading metadata of {0}", "Status"),
                         this.getName()));
 
                 final StorageObject target = this.getDetails();
                 HashMap<String, String> metadata = new HashMap<String, String>();
                 Map<String, Object> source = target.getModifiableMetadata();
                 for(String key : source.keySet()) {
                     metadata.put(key, source.get(key).toString());
                 }
                 this.attributes().setMetadata(metadata);
             }
             catch(ServiceException e) {
                 this.error("Cannot read file attributes", e);
             }
             catch(IOException e) {
                 this.error("Cannot read file attributes", e);
             }
         }
     }
 
     @Override
     public void writeMetadata(Map<String, String> meta) {
         if(attributes().isFile() || attributes().isPlaceholder()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Writing metadata of {0}", "Status"),
                         this.getName()));
 
                 final StorageObject target = this.getDetails();
                 target.replaceAllMetadata(new HashMap<String, Object>(meta));
                 // Apply non standard ACL
                 if(Acl.EMPTY.equals(this.attributes().getAcl())) {
                     this.readAcl();
                 }
                 target.setAcl(this.convert(this.attributes().getAcl()));
                 this.getSession().getClient().updateObjectMetadata(this.getContainerName(), target);
                 target.setMetadataComplete(false);
             }
             catch(ServiceException e) {
                 this.error("Cannot write file attributes", e);
             }
             catch(IOException e) {
                 this.error("Cannot write file attributes", e);
             }
             finally {
                 this.attributes().clear(false, false, false, true);
             }
         }
     }
 
     @Override
     public void readChecksum() {
         if(attributes().isFile()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Compute MD5 hash of {0}", "Status"),
                         this.getName()));
 
                 final StorageObject details = this.getDetails();
                 if(StringUtils.isNotEmpty(details.getMd5HashAsHex())) {
                     attributes().setChecksum(details.getMd5HashAsHex());
                 }
                 else {
                     log.debug("Setting ETag Header as checksum for:" + this.toString());
                     attributes().setChecksum(details.getETag());
                 }
             }
             catch(ServiceException e) {
                 this.error("Cannot read file attributes", e);
             }
             catch(IOException e) {
                 this.error("Cannot read file attributes", e);
             }
         }
     }
 
     @Override
     public void readSize() {
         if(attributes().isFile()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Getting size of {0}", "Status"),
                         this.getName()));
 
                 final StorageObject details = this.getDetails();
                 attributes().setSize(details.getContentLength());
             }
             catch(ServiceException e) {
                 this.error("Cannot read file attributes", e);
             }
             catch(IOException e) {
                 this.error("Cannot read file attributes", e);
             }
         }
     }
 
     @Override
     public void readTimestamp() {
         if(attributes().isFile()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Getting timestamp of {0}", "Status"),
                         this.getName()));
 
                 final StorageObject details = this.getDetails();
                 attributes().setModificationDate(details.getLastModifiedDate().getTime());
             }
             catch(ServiceException e) {
                 this.error("Cannot read file attributes", e);
             }
             catch(IOException e) {
                 this.error("Cannot read file attributes", e);
             }
         }
     }
 
     @Override
     protected void download(BandwidthThrottle throttle, final StreamListener listener, final boolean check) {
         if(attributes().isFile()) {
             OutputStream out = null;
             InputStream in = null;
             try {
                 if(check) {
                     this.getSession().check();
                 }
                 if(this.attributes().isDuplicate()) {
                     in = this.getSession().getClient().getVersionedObject(attributes().getVersionId(),
                             this.getContainerName(), this.getKey(),
                             null, // ifModifiedSince
                             null, // ifUnmodifiedSince
                             null, // ifMatch
                             null, // ifNoneMatch
                             this.status().isResume() ? this.status().getCurrent() : null, null).getDataInputStream();
                 }
                 else {
                     in = this.getSession().getClient().getObject(this.getContainerName(), this.getKey(),
                             null, // ifModifiedSince
                             null, // ifUnmodifiedSince
                             null, // ifMatch
                             null, // ifNoneMatch
                             this.status().isResume() ? this.status().getCurrent() : null, null).getDataInputStream();
                 }
                 out = this.getLocal().getOutputStream(this.status().isResume());
                 this.download(in, out, throttle, listener);
             }
             catch(ServiceException e) {
                 this.error("Download failed", e);
             }
             catch(IOException e) {
                 this.error("Download failed", e);
             }
             finally {
                 IOUtils.closeQuietly(in);
                 IOUtils.closeQuietly(out);
             }
         }
     }
 
     /**
      * Default size threshold for when to use multipart uploads.
      */
     private static final long DEFAULT_MULTIPART_UPLOAD_THRESHOLD = 5 * 1024 * 1024;
 
     /**
      * Default minimum part size for upload parts.
      */
     private static final int DEFAULT_MINIMUM_UPLOAD_PART_SIZE = 5 * 1024 * 1024;
 
     /**
      * The maximum allowed parts in a multipart upload.
      */
     public static final int MAXIMUM_UPLOAD_PARTS = 10000;
 
     @Override
     protected void upload(final BandwidthThrottle throttle, final StreamListener listener, boolean check) {
         try {
             if(attributes().isFile()) {
                 if(check) {
                     this.getSession().check();
                 }
                 S3Object object = new S3Object(this.getKey());
                 object.setContentType(this.getLocal().getMimeType());
                 if(Preferences.instance().getBoolean("s3.upload.metadata.md5")) {
                     try {
                         this.getSession().message(MessageFormat.format(
                                 Locale.localizedString("Compute MD5 hash of {0}", "Status"), this.getName()));
                         object.setMd5Hash(ServiceUtils.computeMD5Hash(this.getLocal().getInputStream()));
                     }
                     catch(NoSuchAlgorithmException e) {
                         log.error(e.getMessage());
                     }
                 }
                 Acl acl = Acl.EMPTY;
                 if(this.exists()) {
                     // Do not overwrite ACL for existing file.
                     if(this.attributes().getAcl().equals(Acl.EMPTY)) {
                         this.readAcl();
                     }
                     acl = this.attributes().getAcl();
                 }
                 else {
                     if(Preferences.instance().getBoolean("queue.upload.changePermissions")) {
                         Permission perm = Permission.EMPTY;
                         if(Preferences.instance().getBoolean("queue.upload.permissions.useDefault")) {
                             if(this.attributes().isFile()) {
                                 perm = new Permission(
                                         Preferences.instance().getInteger("queue.upload.permissions.file.default"));
                             }
                             if(this.attributes().isDirectory()) {
                                 perm = new Permission(
                                         Preferences.instance().getInteger("queue.upload.permissions.folder.default"));
                             }
                         }
                         else {
                             if(this.getLocal().exists()) {
                                 // Read permissions from local file
                                 perm = this.getLocal().attributes().getPermission();
                             }
                         }
                         if(perm.equals(Permission.EMPTY)) {
                             log.debug("Skip writing empty permissions for:" + this.toString());
                         }
                         else {
                             acl = this.getSession().getPublicAcl(this.getContainerName(),
                                     perm.getOtherPermissions()[Permission.READ],
                                     perm.getOtherPermissions()[Permission.WRITE]);
                         }
                     }
                 }
                 if(Acl.EMPTY.equals(acl)) {
                     // Owner gets FULL_CONTROL. No one else has access rights (default).
                     object.setAcl(AccessControlList.REST_CANNED_PRIVATE);
                 }
                 else if(acl.equals(this.getSession().getPrivateAcl(this.getContainerName()))) {
                     // Owner gets FULL_CONTROL. No one else has access rights (default).
                     object.setAcl(AccessControlList.REST_CANNED_PRIVATE);
                 }
                 else if(acl.equals(this.getSession().getPublicAcl(this.getContainerName(), true, false))) {
                     // Owner gets FULL_CONTROL and the anonymous principal is granted READ access
                     object.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
                 }
                 else if(acl.equals(this.getSession().getPublicAcl(this.getContainerName(), true, true))) {
                     // Owner gets FULL_CONTROL, the anonymous principal is granted READ and WRITE access
                     object.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ_WRITE);
                 }
                 else {
                     object.setAcl(this.convert(acl));
                 }
                 // Storage class
                 object.setStorageClass(Preferences.instance().getProperty("s3.storage.class"));
                 // Default metadata for new files
                 for(String m : Preferences.instance().getList("s3.metadata.default")) {
                     if(StringUtils.isBlank(m)) {
                         log.warn("Invalid header " + m);
                         continue;
                     }
                     if(!m.contains("=")) {
                         log.warn("Invalid header " + m);
                         continue;
                     }
                     int split = m.indexOf('=');
                     String name = m.substring(0, split);
                     if(StringUtils.isBlank(name)) {
                         log.warn("Missing key in " + m);
                         continue;
                     }
                     String value = m.substring(split + 1);
                     if(StringUtils.isEmpty(value)) {
                         log.warn("Missing value in " + m);
                         continue;
                     }
                     object.addMetadata(name, value);
                 }
 
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Uploading {0}", "Status"),
                         this.getName()));
 
                 if(this.getLocal().attributes().getSize() > DEFAULT_MULTIPART_UPLOAD_THRESHOLD) {
                     this.uploadMultipart(throttle, listener, object);
                 }
                 else {
                     // No Content-Range support
                     status().setResume(false);
                     this.uploadSingle(throttle, listener, object);
                 }
             }
         }
         catch(ServiceException e) {
             this.status().setComplete(false);
             this.error("Upload failed", e);
         }
         catch(IOException e) {
             this.error("Upload failed", e);
         }
     }
 
     /**
      * @param throttle
      * @param listener
      * @param object
      * @throws IOException
      * @throws ServiceException
      */
     private void uploadSingle(final BandwidthThrottle throttle, final StreamListener listener, S3Object object)
             throws IOException, ServiceException {
         final InputStream in;
         MessageDigest digest = null;
         if(!Preferences.instance().getBoolean("s3.upload.metadata.md5")) {
             // Content-MD5 not set. Need to verify ourselves instad of S3
             try {
                 digest = MessageDigest.getInstance("MD5");
             }
             catch(NoSuchAlgorithmException e) {
                 log.error(e.getMessage());
             }
         }
         if(null == digest) {
             log.warn("MD5 calculation disabled");
             in = this.getLocal().getInputStream();
         }
         else {
             in = new DigestInputStream(this.getLocal().getInputStream(), digest);
         }
         try {
             this.getSession().getClient().putObjectWithRequestEntityImpl(
                     this.getContainerName(), object, new InputStreamRequestEntity(in,
                             this.getLocal().attributes().getSize() - status().getCurrent(),
                             this.getLocal().getMimeType()) {
 
                         @Override
                         public void writeRequest(OutputStream out) throws IOException {
                             S3Path.this.upload(out, in, throttle, listener);
                         }
                     }, Collections.<String, String>emptyMap());
         }
         finally {
             IOUtils.closeQuietly(in);
         }
         if(null != digest) {
             this.getSession().message(MessageFormat.format(
                     Locale.localizedString("Compute MD5 hash of {0}", "Status"), this.getName()));
             // Obtain locally-calculated MD5 hash.
             String hexMD5 = ServiceUtils.toHex(digest.digest());
             this.getSession().getClient().verifyExpectedAndActualETagValues(hexMD5, object);
         }
     }
 
     /**
      * @param throttle
      * @param listener
      * @param object
      * @throws ConnectionCanceledException
      * @throws FileNotFoundException
      * @throws ServiceException
      */
     private void uploadMultipart(final BandwidthThrottle throttle, final StreamListener listener, S3Object object)
             throws IOException, ServiceException {
 
         final ThreadFactory threadFactory = new ThreadFactory() {
             private int threadCount = 1;
 
             public Thread newThread(Runnable r) {
                 Thread thread = new Thread(r);
                 thread.setName("multipart-" + threadCount++);
                 return thread;
             }
         };
 
         MultipartUpload multipart = null;
         if(status().isResume()) {
             // This operation lists in-progress multipart uploads. An in-progress multipart upload is a
             // multipart upload that has been initiated, using the Initiate Multipart Upload request, but has
             // not yet been completed or aborted.
             List<MultipartUpload> uploads = this.getSession().getClient().multipartListUploads(this.getContainerName());
             for(MultipartUpload upload : uploads) {
                 if(!upload.getBucketName().equals(this.getContainerName())) {
                     continue;
                 }
                 if(!upload.getObjectKey().equals(this.getKey())) {
                     continue;
                 }
                 if(log.isInfoEnabled()) {
                     log.info("Resume multipart upload:" + upload);
                 }
                 multipart = upload;
                 break;
             }
         }
         if(null == multipart) {
             log.info("No pending multipart upload found");
             status().setResume(false);
 
             // Initiate multipart upload with metadata
             Map<String, Object> metadata = object.getModifiableMetadata();
             metadata.put(this.getSession().getClient().getRestHeaderPrefix() + "storage-class",
                     Preferences.instance().getProperty("s3.storage.class"));
 
             multipart = this.getSession().getClient().multipartStartUpload(
                     this.getContainerName(), this.getKey(), metadata);
         }
 
         List<MultipartPart> completed;
         if(status().isResume()) {
             log.info("List completed parts of " + multipart.getUploadId());
             // This operation lists the parts that have been uploaded for a specific multipart upload.
             completed = this.getSession().getClient().multipartListParts(multipart);
         }
         else {
             completed = new ArrayList<MultipartPart>();
         }
 
         /**
          * At any point, at most
          * <tt>nThreads</tt> threads will be active processing tasks.
          */
         final ExecutorService pool = Executors.newFixedThreadPool(
                 Preferences.instance().getInteger("s3.upload.concurency"), threadFactory);
 
         try {
             List<Future<MultipartPart>> parts = new ArrayList<Future<MultipartPart>>();
 
             final long defaultPartSize = Math.max((this.getLocal().attributes().getSize() / MAXIMUM_UPLOAD_PARTS),
                     DEFAULT_MINIMUM_UPLOAD_PART_SIZE);
 
             long remaining = this.getLocal().attributes().getSize();
             long marker = 0;
 
             for(int partNumber = 1; remaining > 0; partNumber++) {
                 boolean skip = false;
                 if(status().isResume()) {
                     log.info("Determine if part " + partNumber + " can be skipped");
                     for(MultipartPart c : completed) {
                         if(c.getPartNumber().equals(partNumber)) {
                             log.info("Skip completed part number " + partNumber);
                             listener.bytesSent(c.getSize());
                             skip = true;
                             break;
                         }
                     }
                 }
 
                 // Last part can be less than 5 MB. Adjust part size.
                 final long length = Math.min(defaultPartSize, remaining);
 
                 if(!skip) {
                     // Submit to queue
                     parts.add(this.submitPart(throttle, listener, multipart, pool, partNumber, marker, length));
                 }
 
                 remaining -= length;
                 marker += length;
             }
             for(Future<MultipartPart> future : parts) {
                 try {
                     completed.add(future.get());
                 }
                 catch(InterruptedException e) {
                     log.error("Part upload failed:" + e.getMessage());
                     // Cancel future tasks
                     pool.shutdown();
                     throw new ConnectionCanceledException(e.getMessage());
                 }
                 catch(ExecutionException e) {
                     log.warn("Part upload failed:" + e.getMessage());
                     // Cancel future tasks
                     pool.shutdown();
                     if(e.getCause() instanceof ServiceException) {
                         throw (ServiceException) e.getCause();
                     }
                     if(e.getCause() instanceof IOException) {
                         throw (IOException) e.getCause();
                     }
                     throw new ConnectionCanceledException(e.getMessage());
                 }
             }
             if(status().isComplete()) {
                 this.getSession().getClient().multipartCompleteUpload(multipart, completed);
             }
         }
         finally {
             if(!status().isComplete()) {
                 // Cancel all previous parts
                 log.info("Cancel multipart upload:" + multipart.getUploadId());
                 this.getSession().getClient().multipartAbortUpload(multipart);
             }
         }
     }
 
     private Future<MultipartPart> submitPart(final BandwidthThrottle throttle, final StreamListener listener,
                                              final MultipartUpload multipart,
                                              final ExecutorService pool,
                                              final int partNumber,
                                              final long offset, final long length) throws ConnectionCanceledException {
         if(pool.isShutdown()) {
             throw new ConnectionCanceledException();
         }
         log.info("Submit part to queue:" + partNumber);
         return pool.submit(new Callable<MultipartPart>() {
             public MultipartPart call() throws IOException, ServiceException {
                 Map<String, String> requestParameters = new HashMap<String, String>();
                 requestParameters.put("uploadId", multipart.getUploadId());
                 requestParameters.put("partNumber", String.valueOf(partNumber));
 
                 final InputStream in;
                 MessageDigest digest = null;
                 if(!Preferences.instance().getBoolean("s3.upload.metadata.md5")) {
                     // Content-MD5 not set. Need to verify ourselves instad of S3
                     try {
                         digest = MessageDigest.getInstance("MD5");
                     }
                     catch(NoSuchAlgorithmException e) {
                         log.error(e.getMessage());
                     }
                 }
                 if(null == digest) {
                     log.warn("MD5 calculation disabled");
                     in = getLocal().getInputStream();
                 }
                 else {
                     in = new DigestInputStream(getLocal().getInputStream(), digest);
                 }
                 final S3Object part = new S3Object(getKey());
                 try {
                     getSession().getClient().putObjectWithRequestEntityImpl(
                             getContainerName(), part, new InputStreamRequestEntity(in,
                                     length,
                                     getLocal().getMimeType()) {
 
                                 @Override
                                 public void writeRequest(OutputStream out) throws IOException {
                                     S3Path.this.upload(out, in, throttle, listener, offset, length);
                                 }
                             }, requestParameters);
                 }
                 finally {
                     IOUtils.closeQuietly(in);
                 }
                 if(null != digest) {
                     // Obtain locally-calculated MD5 hash.
                     String hexMD5 = ServiceUtils.toHex(digest.digest());
                     getSession().getClient().verifyExpectedAndActualETagValues(hexMD5, part);
                 }
                 // Populate part with response data that is accessible via the object's metadata
                 return new MultipartPart(partNumber, part.getLastModifiedDate(),
                         part.getETag(), part.getContentLength());
             }
         });
     }
 
     @Override
     public AttributedList<Path> list(final AttributedList<Path> children) {
         if(this.attributes().isDirectory()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Listing directory {0}", "Status"),
                         this.getName()));
 
                 if(this.isRoot()) {
                     // List all buckets
                    for(S3Bucket bucket : this.getSession().getBuckets(false)) {
                         Path p = PathFactory.createPath(this.getSession(), this.getAbsolute(), bucket.getName(),
                                 Path.VOLUME_TYPE | Path.DIRECTORY_TYPE);
                         if(null != bucket.getOwner()) {
                             p.attributes().setOwner(bucket.getOwner().getDisplayName());
                             p.attributes().setGroup(bucket.getOwner().getId());
                         }
                         if(null != bucket.getCreationDate()) {
                             p.attributes().setCreationDate(bucket.getCreationDate().getTime());
                         }
                         children.add(p);
                     }
                 }
                 else {
                     final String container = this.getContainerName();
                     // Keys can be listed by prefix. By choosing a common prefix
                     // for the names of related keys and marking these keys with
                     // a special character that delimits hierarchy, you can use the list
                     // operation to select and browse keys hierarchically
                     String prefix = StringUtils.EMPTY;
                     if(!this.isContainer()) {
                         // estricts the response to only contain results that begin with the
                         // specified prefix. If you omit this optional argument, the value
                         // of Prefix for your query will be the empty string.
                         // In other words, the results will be not be restricted by prefix.
                         prefix = this.getKey() + String.valueOf(Path.DELIMITER);
                     }
                     // If this optional, Unicode string parameter is included with your request,
                     // then keys that contain the same string between the prefix and the first
                     // occurrence of the delimiter will be rolled up into a single result
                     // element in the CommonPrefixes collection. These rolled-up keys are
                     // not returned elsewhere in the response.
                     final String delimiter = String.valueOf(Path.DELIMITER);
                     children.addAll(this.listObjects(container, prefix, delimiter));
                     if(Preferences.instance().getBoolean("s3.revisions.enable")) {
                         if(this.getSession().isVersioning(container)) {
                             String priorLastKey = null;
                             String priorLastVersionId = null;
                             do {
                                 final VersionOrDeleteMarkersChunk chunk = this.getSession().getClient().listVersionedObjectsChunked(
                                         container, prefix, delimiter,
                                         Preferences.instance().getInteger("s3.listing.chunksize"),
                                         priorLastKey, priorLastVersionId, true);
                                 children.addAll(this.listVersions(container, Arrays.asList(chunk.getItems())));
                             }
                             while(priorLastKey != null);
                         }
                     }
 
                 }
                 this.getSession().setWorkdir(this);
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
         }
         return children;
     }
 
     protected AttributedList<Path> listObjects(String bucket, String prefix, String delimiter)
             throws IOException, ServiceException {
         final AttributedList<Path> children = new AttributedList<Path>();
         // Null if listing is complete
         String priorLastKey = null;
         do {
             // Read directory listing in chunks. List results are always returned
             // in lexicographic (alphabetical) order.
             StorageObjectsChunk chunk = this.getSession().getClient().listObjectsChunked(
                     bucket, prefix, delimiter,
                     Preferences.instance().getInteger("s3.listing.chunksize"), priorLastKey);
 
             final StorageObject[] objects = chunk.getObjects();
             for(StorageObject object : objects) {
                 final S3Path p = (S3Path) PathFactory.createPath(this.getSession(), bucket,
                         object.getKey(), Path.FILE_TYPE);
                 p.setParent(this);
                 p.attributes().setSize(object.getContentLength());
                 p.attributes().setModificationDate(object.getLastModifiedDate().getTime());
                 p.attributes().setOwner(this.getContainer().attributes().getOwner());
                 // Directory placholders
                 if(object.isDirectoryPlaceholder()) {
                     p.attributes().setType(Path.DIRECTORY_TYPE);
                     p.attributes().setPlaceholder(true);
                 }
                 else if(0 == object.getContentLength()) {
                     if("application/x-directory".equals(p.getDetails().getContentType())) {
                         p.attributes().setType(Path.DIRECTORY_TYPE);
                         p.attributes().setPlaceholder(true);
                     }
                 }
                 Object etag = object.getMetadataMap().get(StorageObject.METADATA_HEADER_ETAG);
                 if(null != etag) {
                     String s = etag.toString().replaceAll("\"", "");
                     p.attributes().setChecksum(s);
                     if(s.equals("d66759af42f282e1ba19144df2d405d0")) {
                         // Fix #5374 s3sync.rb interoperability
                         p.attributes().setType(Path.DIRECTORY_TYPE);
                         p.attributes().setPlaceholder(true);
                     }
                 }
                 p.attributes().setStorageClass(object.getStorageClass());
                 if(object instanceof S3Object) {
                     p.attributes().setVersionId(((S3Object) object).getVersionId());
                 }
                 children.add(p);
             }
             final String[] prefixes = chunk.getCommonPrefixes();
             for(String common : prefixes) {
                 if(common.equals(String.valueOf(Path.DELIMITER))) {
                     log.warn("Skipping prefix " + common);
                     continue;
                 }
                 final Path p = PathFactory.createPath(this.getSession(),
                         bucket, common, Path.DIRECTORY_TYPE);
                 p.setParent(this);
                 if(children.contains(p.getReference())) {
                     continue;
                 }
                 p.attributes().setOwner(this.getContainer().attributes().getOwner());
                 p.attributes().setPlaceholder(false);
                 children.add(p);
             }
             priorLastKey = chunk.getPriorLastKey();
         }
         while(priorLastKey != null);
         return children;
     }
 
     private List<Path> listVersions(String bucket, List<BaseVersionOrDeleteMarker> versionOrDeleteMarkers)
             throws IOException, ServiceException {
         Collections.sort(versionOrDeleteMarkers, new Comparator<BaseVersionOrDeleteMarker>() {
             public int compare(BaseVersionOrDeleteMarker o1, BaseVersionOrDeleteMarker o2) {
                 return o1.getLastModified().compareTo(o2.getLastModified());
             }
         });
         final List<Path> versions = new ArrayList<Path>();
         int i = 0;
         for(BaseVersionOrDeleteMarker object : versionOrDeleteMarkers) {
             if(object.isLatest()) {
                 // Latest version already in default listing
                 continue;
             }
             if(object.isDeleteMarker()) {
                 continue;
             }
             final S3Path path = (S3Path) PathFactory.createPath(this.getSession(),
                     bucket, object.getKey(), Path.FILE_TYPE);
             path.setParent(this);
             final S3Version version = (S3Version) object;
             // Versioning is enabled if non null.
             path.attributes().setVersionId(version.getVersionId());
             path.attributes().setRevision(++i);
             path.attributes().setDuplicate(true);
             if(0 == version.getSize()) {
                 final StorageObject details = path.getDetails();
                 if(details.isDirectoryPlaceholder()) {
                     // No need for versioning delimiters
                     continue;
                 }
             }
             path.attributes().setSize(version.getSize());
             path.attributes().setModificationDate(version.getLastModified().getTime());
             path.attributes().setOwner(this.getContainer().attributes().getOwner());
             path.attributes().setStorageClass(version.getStorageClass());
             versions.add(path);
         }
         return versions;
     }
 
     @Override
     public void mkdir() {
         if(this.attributes().isDirectory()) {
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Making directory {0}", "Status"),
                         this.getName()));
 
                 if(this.isContainer()) {
                     // Create bucket
                     if(!ServiceUtils.isBucketNameValidDNSName(this.getName())) {
                         throw new ServiceException(Locale.localizedString("Bucket name is not DNS compatible", "S3"));
                     }
                     this.getSession().getClient().createBucket(this.getContainerName(),
                             Preferences.instance().getProperty("s3.location"), AccessControlList.REST_CANNED_PUBLIC_READ);
                 }
                 else {
                     S3Object object = new S3Object(this.getKey() + Path.DELIMITER);
                     object.setBucketName(this.getContainerName());
                     // Set object explicitly to private access by default.
                     //AccessControlList acl = this.getAccessControlList(new Permission(
                     //        Preferences.instance().getProperty("queue.upload.permissions.folder.default")));
                     AccessControlList acl = AccessControlList.REST_CANNED_PRIVATE;
                     object.setAcl(acl);
                     object.setContentLength(0);
                     object.setContentType("application/x-directory");
                     this.getSession().getClient().putObject(this.getContainerName(), object);
                 }
                 this.cache().put(this.getReference(), AttributedList.<Path>emptyList());
                 // The directory listing is no more current
                 this.getParent().invalidate();
             }
             catch(ServiceException e) {
                 this.error("Cannot create folder {0}", e);
             }
             catch(IOException e) {
                 this.error("Cannot create folder {0}", e);
             }
         }
     }
 
     @Override
     public void writeAcl(Acl acl, boolean recursive) {
         try {
             AccessControlList list = this.convert(acl);
             this.writeAcl(list, recursive);
         }
         catch(ServiceException e) {
             this.error("Cannot change permissions", e);
         }
         catch(IOException e) {
             this.error("Cannot change permissions", e);
         }
     }
 
     /**
      * Convert ACL for writing to service.
      *
      * @param acl
      * @return
      * @throws IOException
      */
     protected AccessControlList convert(Acl acl) throws IOException {
         AccessControlList list = new AccessControlList();
         final StorageOwner owner = this.getSession().getBucket(this.getContainerName()).getOwner();
         list.setOwner(owner);
         for(Acl.UserAndRole userAndRole : acl.asList()) {
             if(!userAndRole.isValid()) {
                 continue;
             }
             if(userAndRole.getUser() instanceof Acl.EmailUser) {
                 list.grantPermission(new EmailAddressGrantee(userAndRole.getUser().getIdentifier()),
                         org.jets3t.service.acl.Permission.parsePermission(userAndRole.getRole().getName()));
             }
             else if(userAndRole.getUser() instanceof Acl.GroupUser) {
                 list.grantPermission(new GroupGrantee(userAndRole.getUser().getIdentifier()),
                         org.jets3t.service.acl.Permission.parsePermission(userAndRole.getRole().getName()));
             }
             else {
                 list.grantPermission(new CanonicalGrantee(userAndRole.getUser().getIdentifier()),
                         org.jets3t.service.acl.Permission.parsePermission(userAndRole.getRole().getName()));
             }
         }
         if(log.isDebugEnabled()) {
             try {
                 log.debug(list.toXml());
             }
             catch(ServiceException e) {
                 log.error(e.getMessage());
             }
         }
         return list;
     }
 
     /**
      * Write ACL to bucket or object.
      *
      * @param acl The updated access control list.
      */
     protected void writeAcl(AccessControlList acl, boolean recursive) throws IOException, ServiceException {
         try {
             if(null == acl.getOwner()) {
                 log.warn("Owner unknown. Cannot update ACL");
                 return;
             }
             if(this.isContainer()) {
                 this.getSession().getClient().putBucketAcl(this.getContainerName(), acl);
             }
             else if(attributes().isFile() || attributes().isPlaceholder()) {
                 this.getSession().getClient().putObjectAcl(this.getContainerName(), this.getKey(), acl);
             }
         }
         finally {
             this.attributes().clear(false, false, true, false);
         }
         if(attributes().isDirectory()) {
             if(recursive) {
                 for(AbstractPath child : this.children()) {
                     if(!this.getSession().isConnected()) {
                         break;
                     }
                     ((S3Path) child).writeAcl(acl, recursive);
                 }
             }
         }
     }
 
     /**
      * @param permission The permissions to apply
      * @return The updated access control list.
      */
     protected AccessControlList getAccessControlList(final Permission permission) throws IOException {
         final AccessControlList acl = new AccessControlList();
         final StorageOwner owner = this.getSession().getBucket(this.getContainerName()).getOwner();
         acl.setOwner(owner);
         final CanonicalGrantee grantee = new CanonicalGrantee(owner.getId());
         if(permission.getOwnerPermissions()[Permission.READ]) {
             acl.grantPermission(grantee, org.jets3t.service.acl.Permission.PERMISSION_READ);
         }
         if(permission.getOwnerPermissions()[Permission.WRITE]) {
             // when applied to a bucket, grants permission to create, overwrite, and delete any object in the bucket.
             // This permission is not supported for objects.
             if(this.isContainer()) {
                 acl.grantPermission(grantee, org.jets3t.service.acl.Permission.PERMISSION_WRITE);
             }
         }
         if(permission.getOtherPermissions()[Permission.READ]) {
             acl.grantPermission(GroupGrantee.ALL_USERS, org.jets3t.service.acl.Permission.PERMISSION_READ);
         }
         if(permission.getOtherPermissions()[Permission.WRITE]) {
             // when applied to a bucket, grants permission to create, overwrite, and delete any object in the bucket.
             // This permission is not supported for objects.
             if(this.isContainer()) {
                 acl.grantPermission(GroupGrantee.ALL_USERS, org.jets3t.service.acl.Permission.PERMISSION_WRITE);
             }
         }
         return acl;
     }
 
     @Override
     public void delete() {
         try {
             this.getSession().check();
             final String container = this.getContainerName();
             if(attributes().isFile()) {
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                         this.getName()));
                 this.delete(container, this.getKey(), this.attributes().getVersionId());
             }
             else if(attributes().isDirectory()) {
                 for(AbstractPath child : this.children()) {
                     if(!this.getSession().isConnected()) {
                         break;
                     }
                     child.delete();
                 }
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                         this.getName()));
                 if(this.isContainer()) {
                     this.getSession().getClient().deleteBucket(container);
                 }
                 else {
                     try {
                         // Because we normalize paths and remove a trailing delimiter we add it here again as the
                         // default directory placeholder formats has the format `/placeholder/' as a key.
                         this.delete(container, this.getKey() + Path.DELIMITER, this.attributes().getVersionId());
                         // Always returning 204 even if the key does not exist.
                         // Fallback to legacy directory placeholders with metadata instead of key with trailing delimiter
                         this.delete(container, this.getKey(), this.attributes().getVersionId());
                     }
                     catch(ServiceException e) {
                         // AWS might change their mind and return 404 at some point for non-existing keys
                         log.warn("Delete failed:" + e.getMessage());
                     }
                 }
             }
             // The directory listing is no more current
             this.getParent().invalidate();
         }
         catch(ServiceException e) {
             this.error("Cannot delete {0}", e);
         }
         catch(IOException e) {
             this.error("Cannot delete {0}", e);
         }
     }
 
     /**
      * @param container
      * @param key
      * @param version
      * @throws ConnectionCanceledException
      * @throws ServiceException
      */
     private void delete(String container, String key, String version) throws ConnectionCanceledException, ServiceException {
         if(StringUtils.isNotEmpty(version)) {
             if(this.getSession().isMultiFactorAuthentication(container)) {
                 LoginController c = LoginControllerFactory.instance(this.getSession());
                 final Credentials credentials = this.getSession().mfa(c);
                 this.getSession().getClient().deleteVersionedObjectWithMFA(version,
                         credentials.getUsername(),
                         credentials.getPassword(),
                         container, key);
             }
             else {
                 this.getSession().getClient().deleteVersionedObject(version, container, key);
             }
         }
         else {
             this.getSession().getClient().deleteObject(this.getContainerName(), key);
         }
     }
 
     @Override
     public void rename(AbstractPath renamed) {
         try {
             if(attributes().isVolume()) {
                 log.warn("Renaming buckets is not currently supported by S3");
                 return;
             }
             if(attributes().isFile() || attributes().isPlaceholder()) {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Renaming {0} to {1}", "Status"),
                         this.getName(), renamed));
 
                 final S3Object destination = new S3Object(((S3Path) renamed).getKey());
                 // Keep same storage class
                 destination.setStorageClass(this.attributes().getStorageClass());
                 // Apply non standard ACL
                 if(Acl.EMPTY.equals(this.attributes().getAcl())) {
                     this.readAcl();
                 }
                 destination.setAcl(this.convert(this.attributes().getAcl()));
                 // Moving the object retaining the metadata of the original.
                 this.getSession().getClient().moveObject(this.getContainerName(), this.getKey(), ((S3Path) renamed).getContainerName(),
                         destination, false);
                 // The directory listing of the target is no more current
                 renamed.getParent().invalidate();
                 // The directory listing of the source is no more current
                 this.getParent().invalidate();
             }
             if(attributes().isDirectory()) {
                 for(AbstractPath i : this.children()) {
                     if(!this.getSession().isConnected()) {
                         break;
                     }
                     i.rename(PathFactory.createPath(this.getSession(), renamed.getAbsolute(),
                             i.getName(), i.attributes().getType()));
                 }
             }
         }
         catch(ServiceException e) {
             this.error("Cannot rename {0}", e);
         }
         catch(IOException e) {
             this.error("Cannot rename {0}", e);
         }
     }
 
     @Override
     public void copy(AbstractPath copy) {
         if(((Path) copy).getSession().equals(this.getSession())) {
             // Copy on same server
             try {
                 this.getSession().check();
                 this.getSession().message(MessageFormat.format(Locale.localizedString("Copying {0} to {1}", "Status"),
                         this.getName(), copy));
 
                 if(this.attributes().isFile()) {
                     S3Object destination = new S3Object(((S3Path) copy).getKey());
                     // Keep same storage class
                     destination.setStorageClass(this.attributes().getStorageClass());
                     // Apply non standard ACL
                     if(Acl.EMPTY.equals(this.attributes().getAcl())) {
                         this.readAcl();
                     }
                     destination.setAcl(this.convert(this.attributes().getAcl()));
                     // Copying object applying the metadata of the original
                     this.getSession().getClient().copyObject(this.getContainerName(), this.getKey(),
                             ((S3Path) copy).getContainerName(), destination, false);
                 }
                 else if(this.attributes().isDirectory()) {
                     for(AbstractPath i : this.children()) {
                         if(!this.getSession().isConnected()) {
                             break;
                         }
                         S3Path destination = (S3Path) PathFactory.createPath(this.getSession(), copy.getAbsolute(),
                                 i.getName(), i.attributes().getType());
                         // Apply storage class of parent directory
                         ((S3Path) i).attributes().setStorageClass(this.attributes().getStorageClass());
                         i.copy(destination);
                     }
                 }
                 // The directory listing is no more current
                 copy.getParent().invalidate();
             }
             catch(ServiceException e) {
                 this.error("Cannot copy {0}");
             }
             catch(IOException e) {
                 this.error("Cannot copy {0}");
             }
         }
         else {
             // Copy to different host
             super.copy(copy);
         }
     }
 
     /**
      * Overwritten to provide publicy accessible URL of given object
      *
      * @return Using scheme from protocol
      */
     @Override
     public String toURL() {
         return this.toURL(this.getHost().getProtocol().getScheme());
     }
 
     /**
      * Overwritten to provide publicy accessible URL of given object
      *
      * @return Plain HTTP link
      */
     @Override
     public String toHttpURL() {
         return this.toURL(Protocol.S3.getScheme());
     }
 
     public String toURL(String scheme) {
         StringBuilder url = new StringBuilder(scheme);
         url.append("://");
         if(this.isRoot()) {
             url.append(this.getHost().getHostname());
         }
         else {
             String container = this.getContainerName();
             String hostname = this.getSession().getHostnameForContainer(container);
             if(hostname.startsWith(container)) {
                 url.append(hostname);
                 if(!this.isContainer()) {
                     url.append(encode(this.getKey()));
                 }
             }
             else {
                 url.append(this.getSession().getHost().getHostname());
                 url.append(encode(this.getAbsolute()));
             }
         }
         return url.toString();
     }
 
     /**
      * Query string authentication. Query string authentication is useful for giving HTTP or browser access to
      * resources that would normally require authentication. The signature in the query string secures the request
      *
      * @return A signed URL with a limited validity over time.
      */
     public DescriptiveUrl toSignedUrl() {
         return toSignedUrl(Preferences.instance().getInteger("s3.url.expire.seconds"));
     }
 
     /**
      * @param seconds Expire after seconds elapsed
      * @return
      */
     private DescriptiveUrl toSignedUrl(int seconds) {
         Calendar expiry = Calendar.getInstance();
         expiry.add(Calendar.SECOND, seconds);
         return new DescriptiveUrl(this.createSignedUrl(seconds),
                 MessageFormat.format(Locale.localizedString("{0} URL"), Locale.localizedString("Signed", "S3"))
                         + " (" + MessageFormat.format(Locale.localizedString("Expires on {0}", "S3") + ")",
                         DateFormatterFactory.instance().getShortFormat(expiry.getTimeInMillis()))
         );
     }
 
     /**
      * Query String Authentication generates a signed URL string that will grant
      * access to an S3 resource (bucket or object)
      * to whoever uses the URL up until the time specified.
      *
      * @return
      */
     public String createSignedUrl(int expiry) {
         if(this.attributes().isFile()) {
             try {
                 if(this.getSession().getHost().getCredentials().isAnonymousLogin()) {
                     log.info("Anonymous cannot create signed URL");
                     return null;
                 }
                 this.getSession().check();
                 // Determine expiry time for URL
                 Calendar cal = Calendar.getInstance();
                 cal.add(Calendar.SECOND, expiry);
                 long secondsSinceEpoch = cal.getTimeInMillis() / 1000;
 
                 // Generate URL
                 return this.getSession().getClient().createSignedUrl("GET",
                         this.getContainerName(), this.getKey(), null,
                         null, secondsSinceEpoch, false, this.getHost().getProtocol().isSecure(), false);
             }
             catch(ServiceException e) {
                 this.error("Cannot read file attributes", e);
             }
             catch(IOException e) {
                 this.error("Cannot read file attributes", e);
             }
         }
         return null;
     }
 
     /**
      * Generates a URL string that will return a Torrent file for an object in S3,
      * which file can be downloaded and run in a BitTorrent client.
      *
      * @return
      */
     public DescriptiveUrl toTorrentUrl() {
         if(this.attributes().isFile()) {
             try {
                 return new DescriptiveUrl(this.getSession().getClient().createTorrentUrl(this.getContainerName(),
                         this.getKey()));
             }
             catch(ConnectionCanceledException e) {
                 log.warn(e.getMessage());
             }
         }
         return new DescriptiveUrl(null, null);
     }
 
     @Override
     public Set<DescriptiveUrl> getHttpURLs() {
         Set<DescriptiveUrl> urls = super.getHttpURLs();
         // Always include HTTP URL
         urls.add(new DescriptiveUrl(this.toURL(Protocol.S3.getScheme()),
                 MessageFormat.format(Locale.localizedString("{0} URL"), Protocol.S3.getScheme().toUpperCase())));
         DescriptiveUrl hour = this.toSignedUrl(60 * 60);
         if(StringUtils.isNotBlank(hour.getUrl())) {
             urls.add(hour);
         }
         // Default signed URL expiring in 24 hours.
         DescriptiveUrl day = this.toSignedUrl(Preferences.instance().getInteger("s3.url.expire.seconds"));
         if(StringUtils.isNotBlank(day.getUrl())) {
             urls.add(day);
         }
         DescriptiveUrl week = this.toSignedUrl(7 * 24 * 60 * 60);
         if(StringUtils.isNotBlank(week.getUrl())) {
             urls.add(week);
         }
         DescriptiveUrl torrent = this.toTorrentUrl();
         if(StringUtils.isNotBlank(torrent.getUrl())) {
             urls.add(new DescriptiveUrl(torrent.getUrl(),
                     MessageFormat.format(Locale.localizedString("{0} URL"), Locale.localizedString("Torrent"))));
         }
         return urls;
 
     }
 
     @Override
     public boolean isUploadResumable() {
         return true;
     }
 }
