 /*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
 
     This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
     Grid Operating System, see <http://www.xtreemos.eu> for more details.
     The XtreemOS project has been developed with the financial support of the
     European Commission's IST program under contract #FP6-033576.
 
     XtreemFS is free software: you can redistribute it and/or modify it under
     the terms of the GNU General Public License as published by the Free
     Software Foundation, either version 2 of the License, or (at your option)
     any later version.
 
     XtreemFS is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
     GNU General Public License for more details.
 
     You should have received a copy of the GNU General Public License
     along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
  */
 /*
  * AUTHORS: Björn Kolbeck(ZIB), Nele Andersen (ZIB)
  */
 
 package org.xtreemfs.common.clients.simplescrubber;
 
 import java.io.FileInputStream;
 import java.net.InetSocketAddress;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.EmptyStackException;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Stack;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import org.xtreemfs.common.TimeSync;
 import org.xtreemfs.common.clients.io.RandomAccessFile;
 import org.xtreemfs.common.logging.Logging;
 import org.xtreemfs.common.util.ONCRPCServiceURL;
 import org.xtreemfs.common.util.OutputUtils;
 import org.xtreemfs.common.uuids.ServiceUUID;
 import org.xtreemfs.common.uuids.UUIDResolver;
 import org.xtreemfs.dir.client.DIRClient;
 import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
 import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
 import org.xtreemfs.foundation.pinky.SSLOptions;
 import org.xtreemfs.interfaces.Constants;
 import org.xtreemfs.interfaces.DirectoryEntry;
 import org.xtreemfs.interfaces.DirectoryEntrySet;
 import org.xtreemfs.interfaces.Service;
 import org.xtreemfs.interfaces.ServiceSet;
 import org.xtreemfs.interfaces.UserCredentials;
 import org.xtreemfs.interfaces.StringSet;
 import org.xtreemfs.mrc.client.MRCClient;
 import org.xtreemfs.osd.OSDRequestDispatcher;
 import org.xtreemfs.utils.CLIParser;
 import org.xtreemfs.utils.CLIParser.CliOption;
 import org.xtreemfs.utils.DefaultDirConfig;
 
 /**
  *
  * @author bjko
  */
 public class Scrubber implements FileInfo.FileScrubbedListener {
 
     public static String                       latestScrubAttr    = "scrubber.latestscrub";
 
     public static final UserCredentials       credentials;
 
     static {
         StringSet groupIDs = new StringSet();
         groupIDs.add("root");
         credentials = new UserCredentials("root", groupIDs, "");
     }
 
     private static final int DEFAULT_NUM_THREADS = 10;
 
     private static final String                DEFAULT_DIR_CONFIG = "/etc/xos/xtreemfs/default_dir";
 
     private final String volumeName;
 
     private final RPCNIOSocketClient rpcClient;
 
     private final DIRClient          dirClient;
 
     private final MRCClient          mrcClient;
 
     private final boolean            checkOnly;
 
     private final ExecutorService    tPool;
 
     private long  lastFileComplete;
 
     private long  lastBytesScrubbed;
 
     private final Stack<String>            directories;
 
     private final Stack<String>            files;
 
     private final List<String>             defects;
 
     private final Object                   completeLock;
 
     private int   returnCode;
 
     private int   numInFlight;
 
     private final int numThrs;
 
    private boolean hasFinished;

     private String currentDirName = null;
 
     public Scrubber(RPCNIOSocketClient rpcClient, DIRClient dirClient, MRCClient mrcClient,
             String volume, boolean checkOnly, int numThrs) {
         this.rpcClient = rpcClient;
         this.dirClient = dirClient;
         this.mrcClient = mrcClient;
         this.volumeName = volume;
         this.checkOnly = checkOnly;
         this.numThrs = numThrs;
 
         directories = new Stack<String>();
         files = new Stack<String>();
         tPool = Executors.newFixedThreadPool(numThrs);
         numInFlight = 0;
         completeLock = new Object();
         defects = new LinkedList();
        hasFinished = false;
     }
 
     public int scrub() {
 
         System.out.println("");
         directories.push(volumeName);
         fillQueue();
         synchronized (completeLock) {
             try {
                if (!hasFinished)
                    completeLock.wait();
             } catch (InterruptedException ex) {
             }
         }
         tPool.shutdownNow();
 
         return returnCode;
     }
 
     private void fillQueue() {
         try {
             synchronized (directories) {
                 while (numInFlight < numThrs) {
                     try {
                         String filename = files.pop();
                         try {
                             RandomAccessFile file = new RandomAccessFile("rw", mrcClient.getDefaultServerAddress(),
                                 filename, rpcClient, credentials);
                             FileInfo fi = new FileInfo(file, this);
                             tPool.submit(fi);
                             numInFlight++;
                         } catch (Exception ex) {
                             ex.printStackTrace();
                             finish(1);
                             return;
                         }
                     } catch (EmptyStackException ex) {
                         //fetch next dir
                         fetchNextDir();
                     }
                 }
             }
         } catch (EmptyStackException ex) {
             //no more entries, finished!
 
             try {
                 //mark volume as scrubbed
                 RPCResponse r = mrcClient.setxattr(null, credentials, volumeName, latestScrubAttr, Long.toString(TimeSync.getLocalSystemTime()), 0);
                 r.get();
                 r.freeBuffers();
             } catch (Exception ex2) {
                 System.out.println("\nWarning: cannot mark volume as successfully scrubbed: "+ex2);
             }
 
             finish(0);
             return;
         }
     }
 
     private void finish(int returnCode) {
         synchronized (directories) {
             if (numInFlight == 0) {
                 synchronized (completeLock) {
                     this.returnCode = returnCode;
                    this.hasFinished = true;
                     completeLock.notifyAll();
                 }
             }
         }
     }
 
     @Override
     public void fileScrubbed(RandomAccessFile file, long bytesScrubbed, boolean isOk, String errorMessage) {
         if (!isOk) {
             defects.add(file.getFileId()+" "+file.getPath()+": "+errorMessage);
         }
         //update statistics
 
         long total = 0;
         synchronized (directories) {
             lastBytesScrubbed += bytesScrubbed;
             System.out.format("scrubbed %-42s with %15s - total %15s\n\u001b[100D\u001b[A",file.getFileId(),OutputUtils.formatBytes(bytesScrubbed),OutputUtils.formatBytes(lastBytesScrubbed));
             numInFlight--;
             fillQueue();
         }
         
 
     }
 
     private void fetchNextDir() {
         currentDirName = directories.pop();
 
         try {
             RPCResponse<DirectoryEntrySet> r = mrcClient.readdir(null, credentials, currentDirName);
             DirectoryEntrySet ls = r.get();
             r.freeBuffers();
 
             for (DirectoryEntry e : ls) {
                 if ((e.getStbuf().getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFREG) != 0) {
                     //regular file
                     files.push(currentDirName+"/"+e.getName());
                 } else if ((e.getStbuf().getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFDIR) != 0) {
                     directories.push(currentDirName+"/"+e.getName());
                 }
             }
         } catch (Exception ex) {
             ex.printStackTrace();
             throw new EmptyStackException();
         }
 
 
     }
 
 
     public static void main(String[] args) throws Exception {
 
         Logging.start(Logging.LEVEL_WARN);
 
         System.out.println("XtreemFS scrubber version "+OSDRequestDispatcher.VERSION+" (file system data integrity check)\n");
 
         Map<String, CliOption> options = new HashMap<String, CliOption>();
         List<String> arguments = new ArrayList<String>(1);
         options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
         options.put("dir", new CliOption(CliOption.OPTIONTYPE.URL));
         options.put("chk", new CliOption(CliOption.OPTIONTYPE.SWITCH));
         options.put("thrs", new CliOption(CliOption.OPTIONTYPE.NUMBER));
         options.put("c", new CliOption(CliOption.OPTIONTYPE.STRING));
         options.put("cp", new CliOption(CliOption.OPTIONTYPE.STRING));
         options.put("t", new CliOption(CliOption.OPTIONTYPE.STRING));
         options.put("tp", new CliOption(CliOption.OPTIONTYPE.STRING));
         options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
 
         CLIParser.parseCLI(args, options, arguments);
 
         if (arguments.size() != 1 || options.get("h").switchValue == true) {
             usage();
             return;
         }
 
         InetSocketAddress dirAddr = null;
         boolean useSSL = false;
         String serviceCredsFile = null;
         String serviceCredsPass = null;
         String trustedCAsFile = null;
         String trustedCAsPass = null;
 
         ONCRPCServiceURL dirURL = options.get("dir").urlValue;
 
         // parse security info if protocol is 'https'
         if (dirURL != null && "oncrpcs".equals(dirURL.getProtocol())) {
             useSSL = true;
             serviceCredsFile = options.get("c").stringValue;
             serviceCredsPass = options.get("cp").stringValue;
             trustedCAsFile = options.get("t").stringValue;
             trustedCAsPass = options.get("tp").stringValue;
         }
 
         // read default settings
         if (dirURL == null) {
 
             DefaultDirConfig cfg = new DefaultDirConfig(DEFAULT_DIR_CONFIG);
             cfg.read();
 
             dirAddr = cfg.getDirectoryService();
             useSSL = cfg.isSslEnabled();
             serviceCredsFile = cfg.getServiceCredsFile();
             serviceCredsPass = cfg.getServiceCredsPassphrase();
             trustedCAsFile = cfg.getTrustedCertsFile();
             trustedCAsPass = cfg.getTrustedCertsPassphrase();
         } else
             dirAddr = new InetSocketAddress(dirURL.getHost(), dirURL.getPort());
 
         boolean checkOnly = options.get("chk").switchValue;
 
         int numThreads = DEFAULT_NUM_THREADS;
         if (options.get("thrs").numValue != null)
             numThreads = options.get("thrs").numValue.intValue();
 
 
         String volume = arguments.get(0);
         boolean isVolUUID = false;
         if (volume.startsWith("uuid:")) {
             volume = volume.substring("uuid:".length());
             isVolUUID = true;
         }
 
         SSLOptions sslOptions = useSSL ? new SSLOptions(new FileInputStream(serviceCredsFile),
             serviceCredsPass, SSLOptions.PKCS12_CONTAINER, new FileInputStream(trustedCAsFile),
             trustedCAsPass, SSLOptions.JKS_CONTAINER, false) : null;
 
         // resolve volume MRC
         RPCNIOSocketClient rpcClient = new RPCNIOSocketClient(sslOptions, 30000, 5*60000);
         rpcClient.start();
         rpcClient.waitForStartup();
         DIRClient dirClient = new DIRClient(rpcClient,dirAddr);
         TimeSync.initialize(dirClient, 100000, 50).waitForStartup();
 
 
         RPCResponse<ServiceSet> resp = dirClient.xtreemfs_service_get_by_type(null,Constants.SERVICE_TYPE_VOLUME);
         ServiceSet result = resp.get();
         resp.freeBuffers();
 
         Service volReg = null;
 
         if (isVolUUID) {
             for (Service reg : result) {
                 if (reg.getUuid().equals(volume))
                     volReg = reg;
             }
         } else {
             for (Service reg : result) {
                 if (reg.getName().equals(volume))
                     volReg = reg;
             }
         }
 
         if (volReg == null) {
             System.err.println("volume '" + arguments.get(0) + "' could not be found at Directory Service '"
                 + dirURL + "'");
             System.exit(3);
         }
         volume = volReg.getName();
 
         String mrc = volReg.getData().get("mrc");
         if (mrc == null) {
             System.err.println("volume '" + arguments.get(0) + "' has no valid ");
             System.exit(3);
         }
 
         UUIDResolver.start(dirClient, 60 * 60, 10 * 60 * 60);
 
         ServiceUUID mrcUUID = new ServiceUUID(mrc);
         InetSocketAddress mrcAddress = mrcUUID.getAddress();
 
         int exitCode = 1;
         try {
 
             Scrubber scrubber = new Scrubber(rpcClient, dirClient, new MRCClient(rpcClient, mrcAddress), volume, checkOnly, numThreads);
             exitCode = scrubber.scrub();
             if (exitCode == 0)
                 System.out.println("\n\nsuccessfully scrubbed volume '"+volume+"'");
             else
                 System.out.println("\n\nscrubbing volume '"+volume+"' FAILED!");
 
             System.exit(exitCode);
         } catch (Exception e) {
             e.printStackTrace();
         }
 
         TimeSync.close();
         UUIDResolver.shutdown();
         rpcClient.shutdown();
 
     }
 
     private static void usage() {
         System.out.println("usage: xtfs_scrub [options] <volume_name> | uuid:<volume_uuid>");
         System.out.println("  -dir uri  directory service to use (e.g. 'http://localhost:32638')");
         System.out.println("            If no URI is specified, URI and security settings are taken from '"
             + DEFAULT_DIR_CONFIG + "'");
         System.out
                 .println("            In case of a secured URI ('https://...'), it is necessary to also specify SSL credentials:");
         System.out
                 .println("              -c  <creds_file>         a PKCS#12 file containing user credentials");
         System.out
                 .println("              -cp <creds_passphrase>   a pass phrase to decrypt the the user credentials file");
         System.out
                 .println("              -t  <trusted_CAs>        a PKCS#12 file containing a set of certificates from trusted CAs");
         System.out
                 .println("              -tp <trusted_passphrase> a pass phrase to decrypt the trusted CAs file");
         System.out
                 .println("  -chk      check only (do not update file sizes on the MRC in case of inconsistencies)");
         System.out.println("  -thrs  n  number of concurrent file scrub threads (default=" + DEFAULT_NUM_THREADS + ")");
         System.out.println("  -h        show usage info");
     }
 
 }
