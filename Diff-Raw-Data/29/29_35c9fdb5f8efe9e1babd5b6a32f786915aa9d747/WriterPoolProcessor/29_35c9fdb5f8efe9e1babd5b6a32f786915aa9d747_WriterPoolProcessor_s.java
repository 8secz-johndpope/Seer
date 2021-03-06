 /* WriterPoolProcessor
  *
  * $Id$
  *
  * Created on July 19th, 2006
  *
  * Copyright (C) 2006 Internet Archive.
  *
  * This file is part of the Heritrix web crawler (crawler.archive.org).
  *
  * Heritrix is free software; you can redistribute it and/or modify
  * it under the terms of the GNU Lesser Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or
  * any later version.
  *
  * Heritrix is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Lesser Public License for more details.
  *
  * You should have received a copy of the GNU Lesser Public License
  * along with Heritrix; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 package org.archive.modules.writer;
 
 import static org.archive.modules.ModuleAttributeConstants.A_DNS_SERVER_IP_LABEL;
 import static org.archive.modules.fetcher.FetchStatusCodes.S_DNS_SUCCESS;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.net.InetAddress;
 import java.net.UnknownHostException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.logging.Logger;
 
 import org.archive.checkpointing.RecoverAction;
 import org.archive.io.DefaultWriterPoolSettings;
 import org.archive.io.WriterPool;
 import org.archive.io.WriterPoolMember;
 import org.archive.io.WriterPoolSettings;
 import org.archive.modules.CrawlMetadata;
 import org.archive.modules.ProcessResult;
 import org.archive.modules.Processor;
 import org.archive.modules.ProcessorURI;
 import org.archive.modules.deciderules.recrawl.IdenticalDigestDecideRule;
 import org.archive.modules.net.CrawlHost;
 import org.archive.modules.net.ServerCache;
 import org.archive.modules.net.ServerCacheUtil;
 import org.archive.spring.ConfigPath;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.context.Lifecycle;
 
 /**
  * Abstract implementation of a file pool processor.
  * Subclass to implement for a particular {@link WriterPoolMember} instance.
  * @author Parker Thompson
  * @author stack
  */
 public abstract class WriterPoolProcessor extends Processor 
 implements Lifecycle {
     
     private static final Logger logger = 
         Logger.getLogger(WriterPoolProcessor.class.getName());
 
     /**
      * Whether to gzip-compress files when writing to disk; 
      * by default true, meaning do-compress. 
      */
     boolean compress = true; 
     public boolean getCompress() {
         return compress;
     }
     public void setCompress(boolean compress) {
         this.compress = compress;
     }
     
     /**
      * File prefix. The text supplied here will be used as a prefix naming
      * writer files. For example if the prefix is 'IAH', then file names will
      * look like IAH-20040808101010-0001-HOSTNAME.arc.gz ...if writing ARCs (The
      * prefix will be separated from the date by a hyphen).
      */
     String prefix = WriterPoolMember.DEFAULT_PREFIX; 
     public String getPrefix() {
         return prefix;
     }
     public void setPrefix(String prefix) {
         this.prefix = prefix;
     }
 
     /**
      * Where to save files. Supply absolute or relative path. If relative, files
      * will be written relative to the order.disk-path setting. If more than one
      * path specified, we'll round-robin dropping files to each. This setting is
      * safe to change midcrawl (You can remove and add new dirs as the crawler
      * progresses).
      */
 //    final public static Key<List<String>> PATH = 
 //        Key.makeFinal(Collections.singletonList("crawl-store"));
 
 
     /**
      * Suffix to tag onto files. If value is '${HOSTNAME}', will use hostname
      * for suffix. If empty, no suffix will be added.
      */
     String suffix = WriterPoolMember.DEFAULT_SUFFIX; 
     public String getSuffix() {
         return suffix;
     }
     public void setSuffix(String suffix) {
         this.suffix = suffix;
     }
     
     /**
      * Max size of each file.
      */
     long maxFileSizeBytes = 100000000L;
     public long getMaxFileSizeBytes() {
         return maxFileSizeBytes;
     }
     public void setMaxFileSizeBytes(long maxFileSizeBytes) {
         this.maxFileSizeBytes = maxFileSizeBytes;
     }
     
     /**
      * Maximum active files in pool. This setting cannot be varied over the life
      * of a crawl.
      */
     int poolMaxActive = WriterPool.DEFAULT_MAX_ACTIVE;
     public int getPoolMaxActive() {
         return poolMaxActive;
     }
     public void setPoolMaxActive(int poolMaxActive) {
         this.poolMaxActive = poolMaxActive;
     }
 
     /**
      * Maximum time to wait on pool element (milliseconds). This setting cannot
      * be varied over the life of a crawl.
      */
     int poolMaxWait = WriterPool.DEFAULT_MAXIMUM_WAIT;
     public int getPoolMaxWait() {
         return poolMaxWait;
     }
     public void setPoolMaxWait(int poolMaxWait) {
         this.poolMaxWait = poolMaxWait;
     }
     
     /**
      * Whether to skip the writing of a record when URI history information is
      * available and indicates the prior fetch had an identical content digest.
      * Note that subclass settings may provide more fine-grained control on
      * how identical digest content is handled; for those controls to have
      * effect, this setting must not be 'true' (causing content to be 
      * skipped entirely). 
      * Default is false.
      */
     boolean skipIdenticalDigests = false; 
     public boolean getSkipIdenticalDigests() {
         return skipIdenticalDigests;
     }
     public void setSkipIdenticalDigests(boolean skipIdenticalDigests) {
         this.skipIdenticalDigests = skipIdenticalDigests;
     }
 
     /**
      * ProcessorURI annotation indicating no record was written.
      */
     protected static final String ANNOTATION_UNWRITTEN = "unwritten";
 
     /**
      * Total file bytes to write to disk. Once the size of all files on disk has
      * exceeded this limit, this processor will stop the crawler. A value of
      * zero means no upper limit.
      */
     long maxTotalBytesToWrite = 0L;
     public long getMaxTotalBytesToWrite() {
         return maxTotalBytesToWrite;
     }
     public void setMaxTotalBytesToWrite(long maxTotalBytesToWrite) {
         this.maxTotalBytesToWrite = maxTotalBytesToWrite;
     }
 
     public CrawlMetadata getMetadataProvider() {
         return (CrawlMetadata) kp.get("metadataProvider");
     }
     @Autowired
     public void setMetadataProvider(CrawlMetadata provider) {
         kp.put("metadataProvider",provider);
     }
 
     protected ServerCache serverCache;
     public ServerCache getServerCache() {
         return this.serverCache;
     }
     @Autowired
     public void setServerCache(ServerCache serverCache) {
         this.serverCache = serverCache;
     }
 
    protected ConfigPath directory = new ConfigPath("writer sbudirectory", ".");
     public ConfigPath getDirectory() {
         return directory;
     }
     public void setDirectory(ConfigPath directory) {
         this.directory = directory;
     }
     
     /**
      * Reference to pool.
      */
     transient private WriterPool pool = null;
     
     /**
      * Total number of bytes written to disc.
      */
     private long totalBytesWritten = 0;
 
     private WriterPoolSettings settings;
     private AtomicInteger serial = new AtomicInteger();
     
 
     /**
      * @param name Name of this processor.
      * @param description Description for this processor.
      */
     public WriterPoolProcessor() {
         super();
     }
 
 
     public synchronized void start() {
         if (isRunning()) {
             return;
         }
         super.start(); 
         this.settings = makeWriterPoolSettings();
         setupPool(serial);
     }
     
     public void stop() {
         if (!isRunning()) {
             return;
         }
         super.stop(); 
         this.pool.close();
         this.settings = null; 
     }
     
     
     protected AtomicInteger getSerialNo() {
         return ((WriterPool)getPool()).getSerialNo();
     }
 
     /**
      * Set up pool of files.
      */
     protected abstract void setupPool(final AtomicInteger serial);
 
     
     protected ProcessResult checkBytesWritten() {
         long max = getMaxTotalBytesToWrite();
         if (max <= 0) {
             return ProcessResult.PROCEED;
         }
         if (max <= this.totalBytesWritten) {
             return ProcessResult.FINISH; // FIXME: Specify reason
 //            controller.requestCrawlStop(CrawlStatus.FINISHED_WRITE_LIMIT);
         }
         return ProcessResult.PROCEED;
     }
     
     /**
      * Whether the given ProcessorURI should be written to archive files.
      * Annotates ProcessorURI with a reason for any negative answer.
      * 
      * @param curi ProcessorURI
      * @return true if URI should be written; false otherwise
      */
     protected boolean shouldWrite(ProcessorURI curi) {
         if (getSkipIdenticalDigests()
             && IdenticalDigestDecideRule.hasIdenticalDigest(curi)) {
             curi.getAnnotations().add(ANNOTATION_UNWRITTEN 
                     + ":identicalDigest");
             return false;
         }
         
         boolean retVal;
         String scheme = curi.getUURI().getScheme().toLowerCase();
         // TODO: possibly move this sort of isSuccess() test into ProcessorURI
         if (scheme.equals("dns")) {
             retVal = curi.getFetchStatus() == S_DNS_SUCCESS;
         } else if (scheme.equals("http") || scheme.equals("https")) {
             retVal = curi.getFetchStatus() > 0 && curi.getHttpMethod() != null;
         } else if (scheme.equals("ftp")) {
             retVal = curi.getFetchStatus() == 200;
         } else {
             curi.getAnnotations().add(ANNOTATION_UNWRITTEN
                     + ":scheme");
             return false;
         }
         
         if (retVal == false) {
             // status not deserving writing
             curi.getAnnotations().add(ANNOTATION_UNWRITTEN + ":status");
             return false;
         }
         
         return true; 
     }
     
     /**
      * Return IP address of given URI suitable for recording (as in a
      * classic ARC 5-field header line).
      * 
      * @param curi ProcessorURI
      * @return String of IP address
      */
     protected String getHostAddress(ProcessorURI curi) {
         // special handling for DNS URIs: want address of DNS server
         if (curi.getUURI().getScheme().toLowerCase().equals("dns")) {
             return (String)curi.getData().get(A_DNS_SERVER_IP_LABEL);
         }
         // otherwise, host referenced in URI
         CrawlHost h = ServerCacheUtil.getHostFor(serverCache, curi.getUURI());
         if (h == null) {
             throw new NullPointerException("Crawlhost is null for " +
                 curi + " " + curi.getVia());
         }
         InetAddress a = h.getIP();
         if (a == null) {
             throw new NullPointerException("Address is null for " +
                 curi + " " + curi.getVia() + ". Address " +
                 ((h.getIpFetched() == CrawlHost.IP_NEVER_LOOKED_UP)?
                      "was never looked up.":
                      (System.currentTimeMillis() - h.getIpFetched()) +
                          " ms ago."));
         }
         return h.getIP().getHostAddress();
     }
 
     
     public void checkpoint(File checkpointDir, List<RecoverAction> actions) 
     throws IOException {
         int serial = getSerialNo().get();
         if (this.pool.getNumActive() > 0) {
             // If we have open active Archive files, up the serial number
             // so after checkpoint, we start at one past current number and
             // so the number we serialize, is one past current serialNo.
             // All this serial number manipulation should be fine in here since
             // we're paused checkpointing (Revisit if this assumption changes).
             serial = getSerialNo().incrementAndGet();
         }
 
         // Close all ARCs on checkpoint.
         try {
             this.pool.close();
         } finally {
             // Reopen on checkpoint.
             this.serial = new AtomicInteger(serial);
             setupPool(this.serial);
         }
     }
   
     private void writeObject(ObjectOutputStream out) throws IOException {
         out.defaultWriteObject();
     }
     
     
     private void readObject(ObjectInputStream stream) 
     throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         this.setupPool(serial);
     }
 
     protected WriterPool getPool() {
         return pool;
     }
 
     protected void setPool(WriterPool pool) {
         this.pool = pool;
     }
 
     protected long getTotalBytesWritten() {
         return totalBytesWritten;
     }
 
     protected void setTotalBytesWritten(long totalBytesWritten) {
         this.totalBytesWritten = totalBytesWritten;
     }
 	
     
     protected abstract List<String> getMetadata();
 
     protected abstract List<String> getStorePaths();
     
     private List<File> getOutputDirs() {
         List<String> list = getStorePaths();
         ArrayList<File> results = new ArrayList<File>();
         for (String path: list) {
             File f = new File(
                     path.startsWith("/") ? null : getDirectory().getFile(), 
                     path);
             if (!f.exists()) {
                 try {
                     f.mkdirs();
                 } catch (Exception e) {
                     e.printStackTrace();
                     continue;
                 }
             }
             results.add(f);
         }
         return results;        
     }
     
     
     protected WriterPoolSettings getWriterPoolSettings() {
         return settings;
     }
     
     private WriterPoolSettings makeWriterPoolSettings() {
         DefaultWriterPoolSettings result = new DefaultWriterPoolSettings();
         result.setMaxSize(getMaxFileSizeBytes());
         result.setMetadata(getMetadata());
         result.setOutputDirs(getOutputDirs());
         result.setPrefix(getPrefix());
         
         String sfx = getSuffix();
         sfx = sfx.trim();
         if (sfx.contains(WriterPoolMember.HOSTNAME_VARIABLE)) {            String str = "localhost.localdomain";
             try {
                 str = InetAddress.getLocalHost().getHostName();
             } catch (UnknownHostException ue) {
                 logger.severe("Failed getHostAddress for this host: " + ue);
             }
             sfx = sfx.replace(WriterPoolMember.HOSTNAME_VARIABLE, str);
         }
         
         result.setSuffix(sfx);
         result.setCompressed(getCompress());
         return result;
     }
 
 
 
     
     @Override
     protected void innerProcess(ProcessorURI puri) {
         throw new AssertionError();
     }
 
 
     @Override
     protected abstract ProcessResult innerProcessResult(ProcessorURI uri);
 
     protected boolean shouldProcess(ProcessorURI uri) {
         if (!(uri instanceof ProcessorURI)) {
             return false;
         }
         
         ProcessorURI curi = (ProcessorURI)uri;
         // If failure, or we haven't fetched the resource yet, return
         if (curi.getFetchStatus() <= 0) {
             return false;
         }
         
         // If no recorded content at all, don't write record.
         long recordLength = curi.getContentSize();
         if (recordLength <= 0) {
             // getContentSize() should be > 0 if any material (even just
             // HTTP headers with zero-length body is available.
             return false;
         }
         
         return true;
     }
 
 
 }
