 /**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.apache.manifoldcf.crawler.connectors.sharedrive;
 
 import org.apache.manifoldcf.crawler.system.ManifoldCF;
 import java.io.FileOutputStream;
 import java.io.FileInputStream;
 import java.io.File;
 import java.io.IOException;
 import java.io.InterruptedIOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.net.MalformedURLException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Locale;
 import java.util.Date;
 
 import jcifs.smb.ACE;
 import jcifs.smb.NtlmPasswordAuthentication;
 import jcifs.smb.SmbException;
 import jcifs.smb.SmbFile;
 import jcifs.smb.SmbFileFilter;
 
 import org.apache.manifoldcf.agents.interfaces.RepositoryDocument;
 import org.apache.manifoldcf.agents.interfaces.ServiceInterruption;
 import org.apache.manifoldcf.core.interfaces.IThreadContext;
 import org.apache.manifoldcf.core.interfaces.IHTTPOutput;
 import org.apache.manifoldcf.core.interfaces.IPostParameters;
 import org.apache.manifoldcf.core.interfaces.ConfigParams;
 import org.apache.manifoldcf.core.interfaces.ManifoldCFException;
 import org.apache.manifoldcf.core.interfaces.IKeystoreManager;
 import org.apache.manifoldcf.core.interfaces.KeystoreManagerFactory;
 import org.apache.manifoldcf.core.interfaces.Configuration;
 import org.apache.manifoldcf.core.interfaces.ConfigurationNode;
 import org.apache.manifoldcf.crawler.interfaces.DocumentSpecification;
 import org.apache.manifoldcf.crawler.interfaces.IDocumentIdentifierStream;
 import org.apache.manifoldcf.crawler.interfaces.IProcessActivity;
 import org.apache.manifoldcf.crawler.interfaces.IFingerprintActivity;
 import org.apache.manifoldcf.core.interfaces.SpecificationNode;
 import org.apache.manifoldcf.crawler.interfaces.IVersionActivity;
 import org.apache.manifoldcf.crawler.system.Logging;
 import org.apache.manifoldcf.crawler.system.ManifoldCF;
 
 /** This is the "repository connector" for a smb/cifs shared drive file system.  It's a relative of the share crawler, and should have
 * comparable basic functionality.
 */
 public class SharedDriveConnector extends org.apache.manifoldcf.crawler.connectors.BaseRepositoryConnector
 {
   public static final String _rcsid = "@(#)$Id: SharedDriveConnector.java 996524 2010-09-13 13:38:01Z kwright $";
 
   // Activities we log
   public final static String ACTIVITY_ACCESS = "access";
 
   // These are the share connector nodes and attributes in the document specification
   public static final String NODE_STARTPOINT = "startpoint";
   public static final String NODE_INCLUDE = "include";
   public static final String NODE_EXCLUDE = "exclude";
   public static final String NODE_PATHNAMEATTRIBUTE = "pathnameattribute";
   public static final String NODE_PATHMAP = "pathmap";
   public static final String NODE_FILEMAP = "filemap";
   public static final String NODE_URIMAP = "urimap";
   public static final String NODE_SHAREACCESS = "shareaccess";
   public static final String NODE_SHARESECURITY = "sharesecurity";
   public static final String NODE_MAXLENGTH = "maxlength";
   public static final String NODE_ACCESS = "access";
   public static final String NODE_SECURITY = "security";
   public static final String ATTRIBUTE_PATH = "path";
   public static final String ATTRIBUTE_TYPE = "type";
   public static final String ATTRIBUTE_INDEXABLE = "indexable";
   public static final String ATTRIBUTE_FILESPEC = "filespec";
   public static final String ATTRIBUTE_VALUE = "value";
   public static final String ATTRIBUTE_TOKEN = "token";
   public static final String ATTRIBUTE_MATCH = "match";
   public static final String ATTRIBUTE_REPLACE = "replace";
   public static final String VALUE_DIRECTORY = "directory";
   public static final String VALUE_FILE = "file";
 
   // Properties this connector needs (that can only be configured once)
   public final static String PROPERTY_JCIFS_USE_NTLM_V1 = "org.apache.manifoldcf.crawler.connectors.jcifs.usentlmv1";
   
   // Static initialization of various system properties.  This hopefully takes place
   // before jcifs is loaded.
   static
   {
     System.setProperty("jcifs.smb.client.soTimeout","150000");
     System.setProperty("jcifs.smb.client.responseTimeout","120000");
     System.setProperty("jcifs.resolveOrder","LMHOSTS,DNS,WINS");
     System.setProperty("jcifs.smb.client.listCount","20");
     System.setProperty("jcifs.sm.client.dfs.strictView","true");
   }
   
   private String smbconnectionPath = null;
   private String server = null;
   private String domain = null;
   private String username = null;
   private String password = null;
   private boolean useSIDs = true;
 
   private NtlmPasswordAuthentication pa;
 
   /** Deny access token for default authority */
   private final static String defaultAuthorityDenyToken = "DEAD_AUTHORITY";
 
   /** Constructor.
   */
   public SharedDriveConnector()
   {
     // We need to know whether to operate in NTLMv2 mode, or in NTLM mode.
     String value = ManifoldCF.getProperty(PROPERTY_JCIFS_USE_NTLM_V1);
     if (value == null || value.toLowerCase().equals("false"))
     {
       System.setProperty("jcifs.smb.lmCompatibility","3");
       System.setProperty("jcifs.smb.client.useExtendedSecurity","true");
     }
     else
     {
       System.setProperty("jcifs.smb.lmCompatibility","0");
       System.setProperty("jcifs.smb.client.useExtendedSecurity","false");
     }
   }
 
   /** Establish a "session".  In the case of the jcifs connector, this just builds the appropriate smbconnectionPath string, and does the necessary checks. */
   protected void getSession()
     throws ManifoldCFException
   {
     if (smbconnectionPath == null)
     {
       // Get the server
       if (server == null || server.length() == 0)
         throw new ManifoldCFException("Missing parameter '"+SharedDriveParameters.server+"'");
 
       // make the smb connection to the server
       String authenticationString;
       if (domain == null || domain.length() == 0)
         domain = null;
       
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("Connecting to: " + "smb://" + ((domain==null)?"":domain)+";"+username+":<password>@" + server + "/");
 
       try
       {
         // use NtlmPasswordAuthentication so that we can reuse credential for DFS support
         pa = new NtlmPasswordAuthentication(domain,username,password);
         SmbFile smbconnection = new SmbFile("smb://" + server + "/",pa);
         smbconnectionPath = getFileCanonicalPath(smbconnection);
       }
       catch (MalformedURLException e)
       {
         Logging.connectors.error("Unable to access SMB/CIFS share: "+"smb://" + ((domain==null)?"":domain)+";"+username+":<password>@"+ server + "/\n" + e);
         throw new ManifoldCFException("Unable to access SMB/CIFS share: "+server, e, ManifoldCFException.REPOSITORY_CONNECTION_ERROR);
       }
     }
   }
 
   /** Return the list of activities that this connector supports (i.e. writes into the log).
   *@return the list.
   */
   @Override
   public String[] getActivitiesList()
   {
     return new String[]{ACTIVITY_ACCESS};
   }
 
   /** Close the connection.  Call this before discarding the repository connector.
   */
   @Override
   public void disconnect()
     throws ManifoldCFException
   {
     server = null;
     domain = null;
     username = null;
     password = null;
     pa = null;
     smbconnectionPath = null;
     super.disconnect();
   }
 
   /** Connect.
   *@param configParameters is the set of configuration parameters, which
   * in this case describe the root directory.
   */
   @Override
   public void connect(ConfigParams configParameters)
   {
     super.connect(configParameters);
 
     // Get the server
     server = configParameters.getParameter(SharedDriveParameters.server);
     domain   = configParameters.getParameter(SharedDriveParameters.domain);
     username = configParameters.getParameter(SharedDriveParameters.username);
     if (username == null)
       username = "";
     password = configParameters.getObfuscatedParameter(SharedDriveParameters.password);
     if (password == null)
       password = "";
     String useSIDsString = configParameters.getParameter(SharedDriveParameters.useSIDs);
     if (useSIDsString == null)
       useSIDsString = "true";
     useSIDs = "true".equals(useSIDsString);
 
     // Rejigger the username/domain to be sure we PASS in a domain and we do not include the domain attached to the user!
     // (This became essential at jcifs 1.3.0)
     int index = username.indexOf("@");
     if (index != -1)
     {
       // Strip off the domain from the user
       String userDomain = username.substring(index+1);
       if (domain == null || domain.length() == 0)
         domain = userDomain;
       username = username.substring(0,index);
     }
     index = username.indexOf("\\");
     if (index != -1)
     {
       String userDomain = username.substring(0,index);
       if (domain == null || domain.length() == 0)
         domain = userDomain;
       username = username.substring(index+1);
     }
   }
 
   /** Get the bin name string for a document identifier.  The bin name describes the queue to which the
   * document will be assigned for throttling purposes.  Throttling controls the rate at which items in a
   * given queue are fetched; it does not say anything about the overall fetch rate, which may operate on
   * multiple queues or bins.
   * For example, if you implement a web crawler, a good choice of bin name would be the server name, since
   * that is likely to correspond to a real resource that will need real throttle protection.
   *@param documentIdentifier is the document identifier.
   *@return the bin name.
   */
   @Override
   public String[] getBinNames(String documentIdentifier)
   {
     return new String[]{server};
   }
 
   /**
   * Convert a document identifier to a URI. The URI is the URI that will be
   * the unique key from the search index, and will be presented to the user
   * as part of the search results.
   *
   * @param documentIdentifier
   *            is the document identifier.
   * @return the document uri.
   */
   protected static String convertToURI(String documentIdentifier, MatchMap fileMap, MatchMap uriMap)
     throws ManifoldCFException
   {
     //
     // Note well: This MUST be a legal URI!!
     // e.g.
     // smb://10.33.65.1/Test Folder/PPT Docs/Dearman_University of Texas 20030220.ppt
     // file:////10.33.65.1/Test Folder/PPT Docs/Dearman_University of Texas 20030220.ppt
 
     String serverPath = documentIdentifier.substring("smb://".length());
 
     // The first mapping converts one server path to another.
     // If not present, we leave the original path alone.
     serverPath = fileMap.translate(serverPath);
 
     // The second mapping, if present, creates a URI, using certain rules.  If not present, the old standard IRI conversion is done.
     if (uriMap.getMatchCount() != 0)
     {
       // URI translation.
       // First step is to perform utf-8 translation and %-encoding.
       try
       {
         byte[] byteArray = serverPath.getBytes("utf-8");
         StringBuilder output = new StringBuilder();
         int i = 0;
         while (i < byteArray.length)
         {
           int x = ((int)byteArray[i++]) & 0xff;
           if (x >= 0x80 || (x >= 0 && x <= ' ') || x == ':' || x == '?' || x == '^' || x == '{' || x == '}' ||
             x == '%' || x == '#' || x == '`' || x == ';' || x == '@' || x == '&' || x == '=' || x == '+' ||
             x == '$' || x == ',')
           {
             output.append('%');
             String hexValue = Integer.toHexString((int)x).toUpperCase();
             if (hexValue.length() == 1)
               output.append('0');
             output.append(hexValue);
           }
           else
             output.append((char)x);
         }
 
         // Second step is to perform the mapping.  This strips off the server name and glues on the protocol and web server name, most likely.
         return uriMap.translate(output.toString());
       }
       catch (java.io.UnsupportedEncodingException e)
       {
         // Should not happen...
         throw new ManifoldCFException(e.getMessage(),e);
       }
     }
     else
     {
       // Convert to a URI that begins with file://///.  This used to be done according to the following IE7 specification:
       //   http://blogs.msdn.com/ie/archive/2006/12/06/file-uris-in-windows.aspx
       // However, two factors required change.  First, IE8 decided to no longer adhere to the same specification as IE7.
       // Second, the ingestion API does not (and will never) accept anything other than a well-formed URI.  Thus, file
       // specifications are ingested in a canonical form (which happens to be pretty much what this connector used prior to
       // 3.9.0), and the various clients are responsible for converting that form into something the browser will accept.
       try
       {
         StringBuilder output = new StringBuilder();
 
         int i = 0;
         while (i < serverPath.length())
         {
           int pos = serverPath.indexOf("/",i);
           if (pos == -1)
             pos = serverPath.length();
           String piece = serverPath.substring(i,pos);
           // Note well.  This does *not* %-encode some characters such as '#', which are legal in URI's but have special meanings!
           String replacePiece = java.net.URLEncoder.encode(piece,"utf-8");
           // Convert the +'s back to %20's
           int j = 0;
           while (j < replacePiece.length())
           {
             int plusPos = replacePiece.indexOf("+",j);
             if (plusPos == -1)
               plusPos = replacePiece.length();
             output.append(replacePiece.substring(j,plusPos));
             if (plusPos < replacePiece.length())
             {
               output.append("%20");
               plusPos++;
             }
             j = plusPos;
           }
 
           if (pos < serverPath.length())
           {
             output.append("/");
             pos++;
           }
           i = pos;
         }
         return "file://///"+output.toString();
       }
       catch (java.io.UnsupportedEncodingException e)
       {
         // Should not happen...
         throw new ManifoldCFException(e.getMessage(),e);
       }
     }
   }
 
   /** Request arbitrary connector information.
   * This method is called directly from the API in order to allow API users to perform any one of several connector-specific
   * queries.
   *@param output is the response object, to be filled in by this method.
   *@param command is the command, which is taken directly from the API request.
   *@return true if the resource is found, false if not.  In either case, output may be filled in.
   */
   @Override
   public boolean requestInfo(Configuration output, String command)
     throws ManifoldCFException
   {
     if (command.startsWith("folders/"))
     {
       String parentFolder = command.substring("folders/".length());
       try
       {
         String[] folders = getChildFolderNames(parentFolder);
         int i = 0;
         while (i < folders.length)
         {
           String folder = folders[i++];
           ConfigurationNode node = new ConfigurationNode("folder");
           node.setValue(folder);
           output.addChild(output.getChildCount(),node);
         }
       }
       catch (ManifoldCFException e)
       {
         ManifoldCF.createErrorNode(output,e);
       }
     }
     else if (command.startsWith("folder/"))
     {
       String folder = command.substring("folder/".length());
       try
       {
         String canonicalFolder = validateFolderName(folder);
         if (canonicalFolder != null)
         {
           ConfigurationNode node = new ConfigurationNode("folder");
           node.setValue(canonicalFolder);
           output.addChild(output.getChildCount(),node);
         }
       }
       catch (ManifoldCFException e)
       {
         ManifoldCF.createErrorNode(output,e);
       }
     }
     else
       return super.requestInfo(output,command);
     return true;
   }
   
   
   /** Given a document specification, get either a list of starting document identifiers (seeds),
   * or a list of changes (deltas), depending on whether this is a "crawled" connector or not.
   * These document identifiers will be loaded into the job's queue at the beginning of the
   * job's execution.
   * This method can return changes only (because it is provided a time range).  For full
   * recrawls, the start time is always zero.
   * Note that it is always ok to return MORE documents rather than less with this method.
   *@param spec is a document specification (that comes from the job).
   *@param startTime is the beginning of the time range to consider, inclusive.
   *@param endTime is the end of the time range to consider, exclusive.
   *@return the stream of local document identifiers that should be added to the queue.
   */
   @Override
   public IDocumentIdentifierStream getDocumentIdentifiers(DocumentSpecification spec, long startTime, long endTime)
     throws ManifoldCFException, ServiceInterruption
   {
     getSession();
     return new IdentifierStream(spec);
   }
 
 
   /** Get document versions given an array of document identifiers.
   * This method is called for EVERY document that is considered. It is
   * therefore important to perform as little work as possible here.
   *@param documentIdentifiers is the array of local document identifiers, as understood by this connector.
   *@param oldVersions is the corresponding array of version strings that have been saved for the document identifiers.
   *   A null value indicates that this is a first-time fetch, while an empty string indicates that the previous document
   *   had an empty version string.
   *@param activities is the interface this method should use to perform whatever framework actions are desired.
   *@param spec is the current document specification for the current job.  If there is a dependency on this
   * specification, then the version string should include the pertinent data, so that reingestion will occur
   * when the specification changes.  This is primarily useful for metadata.
   *@param jobMode is an integer describing how the job is being run, whether continuous or once-only.
   *@param usesDefaultAuthority will be true only if the authority in use for these documents is the default one.
   *@return the corresponding version strings, with null in the places where the document no longer exists.
   * Empty version strings indicate that there is no versioning ability for the corresponding document, and the document
   * will always be processed.
   */
   @Override
   public String[] getDocumentVersions(String[] documentIdentifiers, String[] oldVersions, IVersionActivity activities,
     DocumentSpecification spec, int jobMode, boolean usesDefaultAuthority)
     throws ManifoldCFException, ServiceInterruption
   {
     getSession();
     // Read the forced acls.  A null return indicates that security is disabled!!!
     // A zero-length return indicates that the native acls should be used.
     // All of this is germane to how we ingest the document, so we need to note it in
     // the version string completely.
     String[] acls = getForcedAcls(spec);
     String[] shareAcls = getForcedShareAcls(spec);
 
     String pathAttributeName = null;
     MatchMap matchMap = new MatchMap();
     MatchMap fileMap = new MatchMap();
     MatchMap uriMap = new MatchMap();
 
     int i = 0;
     while (i < spec.getChildCount())
     {
       SpecificationNode n = spec.getChild(i++);
       if (n.getType().equals(NODE_PATHNAMEATTRIBUTE))
         pathAttributeName = n.getAttributeValue(ATTRIBUTE_VALUE);
       else if (n.getType().equals(NODE_PATHMAP))
       {
         // Path mapping info also needs to be looked at, because it affects what is
         // ingested.
         String pathMatch = n.getAttributeValue(ATTRIBUTE_MATCH);
         String pathReplace = n.getAttributeValue(ATTRIBUTE_REPLACE);
         matchMap.appendMatchPair(pathMatch,pathReplace);
       }
       else if (n.getType().equals(NODE_FILEMAP))
       {
         String pathMatch = n.getAttributeValue(ATTRIBUTE_MATCH);
         String pathReplace = n.getAttributeValue(ATTRIBUTE_REPLACE);
         fileMap.appendMatchPair(pathMatch,pathReplace);
       }
       else if (n.getType().equals(NODE_URIMAP))
       {
         String pathMatch = n.getAttributeValue(ATTRIBUTE_MATCH);
         String pathReplace = n.getAttributeValue(ATTRIBUTE_REPLACE);
         uriMap.appendMatchPair(pathMatch,pathReplace);
       }
     }
 
     String[] rval = new String[documentIdentifiers.length];
     String documentIdentifier = null;
     i = 0;
     while (i < rval.length)
     {
       documentIdentifier = documentIdentifiers[i];
       try
       {
         if (Logging.connectors.isDebugEnabled())
           Logging.connectors.debug("JCIFS: getVersions(): documentIdentifiers[" + i + "] is: " + documentIdentifier);
         SmbFile file = new SmbFile(documentIdentifier,pa);
 
         // File has to exist AND have a non-null canonical path to be readable.  If the canonical path is
         // null, it means that the windows permissions are not right and directory/file is not readable!!!
         String newPath = getFileCanonicalPath(file);
         // We MUST check the specification here, otherwise a recrawl may not delete what it's supposed to!
         if (fileExists(file) && newPath != null && checkInclude(file,newPath,spec,activities))
         {
           if (fileIsDirectory(file))
           {
             // It's a directory. The version ID will be the
             // last modified date.
             long lastModified = fileLastModified(file);
             rval[i] = new Long(lastModified).toString();
 
           }
           else
           {
             // It's a file of acceptable length.
             // The ability to get ACLs, list files, and an inputstream under DFS all work now.
 
             // The format of this string changed on 11/8/2006 to be comformant with the standard way
             // acls and metadata descriptions are being stuffed into the version string across connectors.
 
             // The format of this string changed again on 7/3/2009 to permit the ingestion uri/iri to be included.
             // This was to support filename/uri mapping functionality.
 
             StringBuilder sb = new StringBuilder();
 
             // Parseable stuff goes first.  There's no metadata for jcifs, so this will just be the acls
             describeDocumentSecurity(sb,file,acls,shareAcls);
 
             // Include the path attribute name and value in the parseable area.
             if (pathAttributeName != null)
             {
               sb.append('+');
               pack(sb,pathAttributeName,'+');
               // Calculate path string; we'll include that wholesale in the version
               String pathAttributeValue = documentIdentifier;
               // 3/13/2008
               // In looking at what comes into the path metadata attribute by default, and cogitating a bit, I've concluded that
               // the smb:// and the server/domain name at the start of the path are just plain old noise, and should be stripped.
               // This changes a behavior that has been around for a while, so there is a risk, but a quick back-and-forth with the
               // SE's leads me to believe that this is safe.
 
               if (pathAttributeValue.startsWith("smb://"))
               {
                 int index = pathAttributeValue.indexOf("/","smb://".length());
                 if (index == -1)
                   index = pathAttributeValue.length();
                 pathAttributeValue = pathAttributeValue.substring(index);
               }
               // Now, translate
               pathAttributeValue = matchMap.translate(pathAttributeValue);
               pack(sb,pathAttributeValue,'+');
             }
             else
               sb.append('-');
 
             // Calculate the ingestion IRI/URI, and include that in the parseable area.
             String ingestionURI = convertToURI(documentIdentifier,fileMap,uriMap);
             pack(sb,ingestionURI,'+');
 
             // The stuff from here on down is non-parseable.
             // Get the file's modified date.
             long lastModified = fileLastModified(file);
             sb.append(new Long(lastModified).toString()).append(":")
               .append(new Long(fileLength(file)).toString());
             // Also include the specification-based answer for the question of whether fingerprinting is
             // going to be done.  Although we may not consider this to truly be "version" information, the
             // specification does affect whether anything is ingested or not, so it really is.  The alternative
             // is to fingerprint right here, in the version part of the world, but that's got a performance
             // downside, because it means that we'd have to suck over pretty much everything just to determine
             // what we wanted to ingest.
             boolean ifIndexable = wouldFileBeIncluded(newPath,spec,true);
             boolean ifNotIndexable = wouldFileBeIncluded(newPath,spec,false);
             if (ifIndexable == ifNotIndexable)
               sb.append("I");
             else
               sb.append(ifIndexable?"Y":"N");
             rval[i] = sb.toString();
           }
         }
         else
           rval[i] = null;
       }
       catch (jcifs.smb.SmbAuthException e)
       {
         Logging.connectors.warn("JCIFS: Authorization exception reading version information for "+documentIdentifier+" - skipping");
         rval[i] = null;
       }
       catch (MalformedURLException mue)
       {
         Logging.connectors.error("JCIFS: MalformedURLException thrown: "+mue.getMessage(),mue);
         throw new ManifoldCFException("MalformedURLException thrown: "+mue.getMessage(),mue);
       }
       catch (SmbException se)
       {
         processSMBException(se,documentIdentifier,"getting document version","fetching share security");
         rval[i] = null;
       }
       catch (java.net.SocketTimeoutException e)
       {
         long currentTime = System.currentTimeMillis();
         Logging.connectors.warn("JCIFS: Socket timeout reading version information for document "+documentIdentifier+": "+e.getMessage(),e);
         throw new ServiceInterruption("Timeout or other service interruption: "+e.getMessage(),e,currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
       catch (InterruptedIOException e)
       {
         throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
       }
       catch (IOException e)
       {
         long currentTime = System.currentTimeMillis();
         Logging.connectors.warn("JCIFS: I/O error reading version information for document "+documentIdentifier+": "+e.getMessage(),e);
         throw new ServiceInterruption("Timeout or other service interruption: "+e.getMessage(),e,currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
       i++;
     }
     return rval;
   }
 
 
   /**
   * Process a set of documents. This is the method that should cause each
   * document to be fetched, processed, and the results either added to the
   * queue of documents for the current job, and/or entered into the
   * incremental ingestion manager. The document specification allows this
   * class to filter what is done based on the job.
   *
   * @param documentIdentifiers
   *            is the set of document identifiers to process.
   * @param activities
   *            is the interface this method should use to queue up new
   *            document references and ingest documents.
   * @param spec
   *            is the document specification.
   * @param scanOnly
   *            is an array corresponding to the document identifiers. It is
   *            set to true to indicate when the processing should only find
   *            other references, and should not actually call the ingestion
   *            methods.
   */
   @Override
   public void processDocuments(String[] documentIdentifiers, String[] versions, IProcessActivity activities,
     DocumentSpecification spec, boolean[] scanOnly) throws ManifoldCFException, ServiceInterruption
   {
     getSession();
 
     byte[] transferBuffer = null;
 
     int i = 0;
     while (i < documentIdentifiers.length)
     {
       String documentIdentifier = documentIdentifiers[i];
       String version = versions[i];
 
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("JCIFS: Processing '"+documentIdentifier+"'");
       try
       {
 
         SmbFile file = new SmbFile(documentIdentifier,pa);
 
         if (fileExists(file))
         {
           if (fileIsDirectory(file))
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("JCIFS: '"+documentIdentifier+"' is a directory");
 
             // Queue up stuff for directory
             // DFS special support no longer needed, because JCifs now does the right thing.
 
             // This is the string we replace in the child canonical paths.
             // String matchPrefix = "";
             // This is what we replace it with, to get back to a DFS path.
             // String matchReplace = "";
 
             // DFS resolved.
 
             // Use a filter to actually do the work here.  This prevents large arrays from being
             // created when there are big directories.
             ProcessDocumentsFilter filter = new ProcessDocumentsFilter(activities,spec);
             fileListFiles(file,filter);
             filter.checkAndThrow();
           }
           else
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("JCIFS: '"+documentIdentifier+"' is a file");
 
             if (!scanOnly[i])
             {
               // We've already avoided queuing documents that we
               // don't want, based on file specifications.
               // We still need to check based on file data.
 
               // DFS support is now implicit in JCifs.
 
               long startFetchTime = System.currentTimeMillis();
               String fileName = getFileCanonicalPath(file);
              if (fileName != null && !file.isHidden())
               {
                 // manipulate path to include the DFS alias, not the literal path
                 // String newPath = matchPrefix + fileName.substring(matchReplace.length());
                 String newPath = fileName;
                 if (checkNeedFileData(newPath, spec))
                 {
                   if (Logging.connectors.isDebugEnabled())
                     Logging.connectors.debug("JCIFS: Local file data needed for '"+documentIdentifier+"'");
 
                   // Create a temporary file, and use that for the check and then the ingest
                   File tempFile = File.createTempFile("_sdc_",null);
                   try
                   {
                     FileOutputStream os = new FileOutputStream(tempFile);
                     try
                     {
 
                       // Now, make a local copy so we can fingerprint
                       InputStream inputStream = getFileInputStream(file);
                       try
                       {
                         // Copy!
                         if (transferBuffer == null)
                           transferBuffer = new byte[65536];
                         while (true)
                         {
                           int amt = inputStream.read(transferBuffer,0,transferBuffer.length);
                           if (amt == -1)
                             break;
                           os.write(transferBuffer,0,amt);
                         }
                       }
                       finally
                       {
                         inputStream.close();
                       }
                     }
                     finally
                     {
                       os.close();
                     }
 
 
                     if (checkIngest(tempFile, newPath, spec, activities))
                     {
                       if (Logging.connectors.isDebugEnabled())
                         Logging.connectors.debug("JCIFS: Decided to ingest '"+documentIdentifier+"'");
                       // OK, do ingestion itself!
                       InputStream inputStream = new FileInputStream(tempFile);
                       try
                       {
                         RepositoryDocument rd = new RepositoryDocument();
                         rd.setBinary(inputStream, tempFile.length());
                         rd.setFileName(file.getName());
                         rd.addField("lastModified", new Date(file.lastModified()).toString());
                         int index = 0;
                         index = setDocumentSecurity(rd,version,index);
                         index = setPathMetadata(rd,version,index);
                         StringBuilder ingestURI = new StringBuilder();
                         index = unpack(ingestURI,version,index,'+');
                         activities.ingestDocument(documentIdentifier, version, ingestURI.toString(), rd);
                       }
                       finally
                       {
                         inputStream.close();
                       }
 
                       // I put this record here deliberately for two reasons:
                       // (1) the other path includes ingestion time, and
                       // (2) if anything fails up to and during ingestion, I want THAT failure record to be written, not this one.
                       // So, really, ACTIVITY_ACCESS is a bit more than just fetch for JCIFS...
                       activities.recordActivity(new Long(startFetchTime),ACTIVITY_ACCESS,
                         new Long(tempFile.length()),documentIdentifier,"Success",null,null);
 
                     }
                     else
                     {
                       // We must actively remove the document here, because the getDocumentVersions()
                       // method has no way of signalling this, since it does not do the fingerprinting.
                       if (Logging.connectors.isDebugEnabled())
                         Logging.connectors.debug("JCIFS: Decided to remove '"+documentIdentifier+"'");
                       activities.deleteDocument(documentIdentifier, version);
                       // We should record the access here as well, since this is a non-exception way through the code path.
                       // (I noticed that this was not being recorded in the history while fixing 25477.)
                       activities.recordActivity(new Long(startFetchTime),ACTIVITY_ACCESS,
                         new Long(tempFile.length()),documentIdentifier,"Success",null,null);
                     }
                   }
                   finally
                   {
                     tempFile.delete();
                   }
                 }
                 else
                 {
                   if (Logging.connectors.isDebugEnabled())
                     Logging.connectors.debug("JCIFS: Local file data not needed for '"+documentIdentifier+"'");
 
                   // Presume that since the file was queued that it fulfilled the needed criteria.
                   // Go off and ingest the fast way.
 
                   // Ingest the document.
                   InputStream inputStream = getFileInputStream(file);
                   try
                   {
                     RepositoryDocument rd = new RepositoryDocument();
                     rd.setBinary(inputStream, fileLength(file));
                     rd.setFileName(file.getName());
                     rd.addField("lastModified", new Date(file.lastModified()).toString());
                     int index = 0;
                     index = setDocumentSecurity(rd,version,index);
                     index = setPathMetadata(rd,version,index);
                     StringBuilder ingestURI = new StringBuilder();
                     index = unpack(ingestURI,version,index,'+');
                     activities.ingestDocument(documentIdentifier, versions[i], ingestURI.toString(), rd);
                   }
                   finally
                   {
                     inputStream.close();
                   }
                   activities.recordActivity(new Long(startFetchTime),ACTIVITY_ACCESS,
                     new Long(fileLength(file)),documentIdentifier,"Success",null,null);
                 }
               }
               else
               {
                Logging.connectors.debug("JCIFS: Skipping file because canonical path is null, or because file is hidden");
                 activities.recordActivity(null,ACTIVITY_ACCESS,
                  null,documentIdentifier,"Skip","Null canonical path or hidden file",null);
               }
             }
           }
         }
       }
       catch (MalformedURLException mue)
       {
         Logging.connectors.error("MalformedURLException tossed",mue);
         activities.recordActivity(null,ACTIVITY_ACCESS,
           null,documentIdentifier,"Error","Malformed URL: "+mue.getMessage(),null);
         throw new ManifoldCFException("MalformedURLException tossed: "+mue.getMessage(),mue);
       }
       catch (jcifs.smb.SmbAuthException e)
       {
         Logging.connectors.warn("JCIFS: Authorization exception reading document/directory "+documentIdentifier+" - skipping");
         activities.recordActivity(null,ACTIVITY_ACCESS,
           null,documentIdentifier,"Skip","Authorization: "+e.getMessage(),null);
         // We call the delete even if it's a directory; this is harmless.
         activities.deleteDocument(documentIdentifier, version);
       }
       catch (SmbException se)
       {
         // At least some of these are transport errors, and should be treated as service
         // interruptions.
         long currentTime = System.currentTimeMillis();
         Throwable cause = se.getRootCause();
         if (cause != null && (cause instanceof jcifs.util.transport.TransportException))
         {
           // See if it's an interruption
           jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException)cause;
           if (te.getRootCause() != null && te.getRootCause() instanceof java.lang.InterruptedException)
             throw new ManifoldCFException(te.getRootCause().getMessage(),te.getRootCause(),ManifoldCFException.INTERRUPTED);
 
           Logging.connectors.warn("JCIFS: Timeout processing document/directory "+documentIdentifier+": retrying...",se);
           activities.recordActivity(null,ACTIVITY_ACCESS,
             null,documentIdentifier,"Retry","Transport: "+cause.getMessage(),null);
           throw new ServiceInterruption("Timeout or other service interruption: "+cause.getMessage(),cause,currentTime + 300000L,
             currentTime + 12 * 60 * 60000L,-1,false);
         }
         if (se.getMessage().indexOf("busy") != -1)
         {
           Logging.connectors.warn("JCIFS: 'Busy' response when processing document/directory for "+documentIdentifier+": retrying...",se);
           activities.recordActivity(null,ACTIVITY_ACCESS,
             null,documentIdentifier,"Retry","Busy: "+se.getMessage(),null);
           throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
             currentTime + 3 * 60 * 60000L,-1,false);
         }
         else if (se.getMessage().indexOf("handle is invalid") != -1)
         {
           Logging.connectors.warn("JCIFS: 'Handle is invalid' response when processing document/directory for "+documentIdentifier+": retrying...",se);
           activities.recordActivity(null,ACTIVITY_ACCESS,
             null,documentIdentifier,"Retry","Expiration: "+se.getMessage(),null);
           throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
             currentTime + 3 * 60 * 60000L,-1,false);
         }
         else if (se.getMessage().indexOf("parameter is incorrect") != -1)
         {
           Logging.connectors.warn("JCIFS: 'Parameter is incorrect' response when processing document/directory for "+documentIdentifier+": retrying...",se);
           activities.recordActivity(null,ACTIVITY_ACCESS,
             null,documentIdentifier,"Retry","Expiration: "+se.getMessage(),null);
           throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
             currentTime + 3 * 60 * 60000L,-1,false);
         }
         else if (se.getMessage().indexOf("no longer available") != -1)
         {
           Logging.connectors.warn("JCIFS: 'No longer available' response when processing document/directory for "+documentIdentifier+": retrying...",se);
           activities.recordActivity(null,ACTIVITY_ACCESS,
             null,documentIdentifier,"Retry","Expiration: "+se.getMessage(),null);
           throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
             currentTime + 3 * 60 * 60000L,-1,false);
         }
         else if (se.getMessage().indexOf("cannot find") != -1 || se.getMessage().indexOf("cannot be found") != -1)
         {
           if (Logging.connectors.isDebugEnabled())
             Logging.connectors.debug("JCIFS: Skipping document/directory "+documentIdentifier+" because it cannot be found");
           activities.recordActivity(null,ACTIVITY_ACCESS,
             null,documentIdentifier,"Not found",null,null);
           activities.deleteDocument(documentIdentifier, version);
         }
         else if (se.getMessage().indexOf("is denied") != -1)
         {
           Logging.connectors.warn("JCIFS: Access exception reading document/directory "+documentIdentifier+" - skipping");
           // We call the delete even if it's a directory; this is harmless and it cleans up the jobqueue row.
           activities.recordActivity(null,ACTIVITY_ACCESS,
             null,documentIdentifier,"Skip","Authorization: "+se.getMessage(),null);
           activities.deleteDocument(documentIdentifier, version);
         }
         else
         {
           Logging.connectors.error("JCIFS: SmbException tossed processing "+documentIdentifier,se);
           activities.recordActivity(null,ACTIVITY_ACCESS,
             null,documentIdentifier,"Error","Unknown: "+se.getMessage(),null);
           throw new ManifoldCFException("SmbException tossed: "+se.getMessage(),se);
         }
       }
       catch (java.net.SocketTimeoutException e)
       {
         long currentTime = System.currentTimeMillis();
         Logging.connectors.warn("JCIFS: Socket timeout processing "+documentIdentifier+": "+e.getMessage(),e);
         activities.recordActivity(null,ACTIVITY_ACCESS,
           null,documentIdentifier,"Retry","Socket timeout: "+e.getMessage(),null);
         throw new ServiceInterruption("Timeout or other service interruption: "+e.getMessage(),e,currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
       catch (InterruptedIOException e)
       {
         throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
       }
       catch (IOException e)
       {
         long currentTime = System.currentTimeMillis();
         Logging.connectors.warn("JCIFS: IO error processing "+documentIdentifier+": "+e.getMessage(),e);
         activities.recordActivity(null,ACTIVITY_ACCESS,
           null,documentIdentifier,"Retry","IO Error: "+e.getMessage(),null);
         throw new ServiceInterruption("Timeout or other service interruption: "+e.getMessage(),e,currentTime + 300000L,
           currentTime + 3 * 60 * 60000L,-1,false);
       }
 
       i++;
     }
 
   }
 
 
 
   /** This method calculates an ACL string based on whether there are forced acls and also based on
   * the acls in place for a file.
   */
   protected void describeDocumentSecurity(StringBuilder description, SmbFile file, String[] forcedacls,
     String[] forcedShareAcls)
     throws ManifoldCFException, IOException
   {
     String[] shareAllowAcls;
     String[] shareDenyAcls;
     String[] allowAcls;
     String[] denyAcls;
 
     int j;
     int allowCount;
     int denyCount;
     ACE[] aces;
 
     if (forcedShareAcls!=null)
     {
       description.append("+");
 
       if (forcedShareAcls.length==0)
       {
         // Do the share acls first.  Note that the smbfile passed in has been dereferenced,
         // so if this is a DFS path, we will be looking up the permissions on the share
         // that is actually used to contain the file.  However, there's no guarantee that the
         // url generated from the original share will work to get there; the permissions on
         // the original share may prohibit users that the could nevertheless see the document
         // if they went in the direct way.
 
 
         // Grab the share permissions.
         aces = getFileShareSecurity(file, useSIDs);
 
         if (aces == null)
         {
           if (Logging.connectors.isDebugEnabled())
             Logging.connectors.debug("JCIFS: Share has no ACL for '"+getFileCanonicalPath(file)+"'");
 
           // "Public" share: S-1-1-0
           shareAllowAcls = new String[]{"S-1-1-0"};
           shareDenyAcls = new String[]{defaultAuthorityDenyToken};
         }
         else
         {
           if (Logging.connectors.isDebugEnabled())
             Logging.connectors.debug("JCIFS: Found "+Integer.toString(aces.length)+" share access tokens for '"+getFileCanonicalPath(file)+"'");
 
           // We are interested in the read permission, and take
           // a keen interest in allow/deny
           allowCount = 0;
           denyCount = 0;
           j = 0;
           while (j < aces.length)
           {
             ACE ace = aces[j++];
             if ((ace.getAccessMask() & ACE.FILE_READ_DATA) != 0)
             {
               if (ace.isAllow())
                 allowCount++;
               else
                 denyCount++;
             }
           }
 
           shareAllowAcls = new String[allowCount];
           shareDenyAcls = new String[denyCount+1];
           j = 0;
           allowCount = 0;
           denyCount = 0;
           shareDenyAcls[denyCount++] = defaultAuthorityDenyToken;
           while (j < aces.length)
           {
             ACE ace = aces[j++];
             if ((ace.getAccessMask() & ACE.FILE_READ_DATA) != 0)
             {
               if (ace.isAllow())
                 shareAllowAcls[allowCount++] = useSIDs ? ace.getSID().toString() : ace.getSID().getAccountName();
               else
                 shareDenyAcls[denyCount++] = useSIDs ? ace.getSID().toString() : ace.getSID().getAccountName();
             }
           }
         }
       }
       else
       {
         shareAllowAcls = forcedShareAcls;
         if (forcedShareAcls.length == 0)
           shareDenyAcls = new String[0];
         else
           shareDenyAcls = new String[]{defaultAuthorityDenyToken};
       }
       java.util.Arrays.sort(shareAllowAcls);
       java.util.Arrays.sort(shareDenyAcls);
       // Stuff the acls into the description string.
       packList(description,shareAllowAcls,'+');
       packList(description,shareDenyAcls,'+');
     }
     else
       description.append('-');
 
     if (forcedacls!=null)
     {
       description.append("+");
 
       if (forcedacls.length==0)
       {
         aces = getFileSecurity(file, useSIDs);
         if (aces == null)
         {
           if (Logging.connectors.isDebugEnabled())
             Logging.connectors.debug("JCIFS: Document has no ACL for '"+getFileCanonicalPath(file)+"'");
 
           // Document is "public", meaning we want S-1-1-0 and the deny token
           allowAcls = new String[]{"S-1-1-0"};
           denyAcls = new String[]{defaultAuthorityDenyToken};
         }
         else
         {
           if (Logging.connectors.isDebugEnabled())
             Logging.connectors.debug("JCIFS: Found "+Integer.toString(aces.length)+" document access tokens for '"+getFileCanonicalPath(file)+"'");
 
           // We are interested in the read permission, and take
           // a keen interest in allow/deny
           allowCount = 0;
           denyCount = 0;
           j = 0;
           while (j < aces.length)
           {
             ACE ace = aces[j++];
             if ((ace.getAccessMask() & ACE.FILE_READ_DATA) != 0)
             {
               if (ace.isAllow())
                 allowCount++;
               else
                 denyCount++;
             }
           }
 
           allowAcls = new String[allowCount];
           denyAcls = new String[denyCount+1];
           j = 0;
           allowCount = 0;
           denyCount = 0;
           denyAcls[denyCount++] = defaultAuthorityDenyToken;
           while (j < aces.length)
           {
             ACE ace = aces[j++];
             if ((ace.getAccessMask() & ACE.FILE_READ_DATA) != 0)
             {
               if (ace.isAllow())
                 allowAcls[allowCount++] = useSIDs ? ace.getSID().toString() : ace.getSID().getAccountName();
               else
                 denyAcls[denyCount++] = useSIDs ? ace.getSID().toString() : ace.getSID().getAccountName();
             }
           }
         }
       }
       else
       {
         allowAcls = forcedacls;
         if (forcedacls.length == 0)
           denyAcls = new String[0];
         else
           denyAcls = new String[]{defaultAuthorityDenyToken};
       }
       java.util.Arrays.sort(allowAcls);
       java.util.Arrays.sort(denyAcls);
       packList(description,allowAcls,'+');
       packList(description,denyAcls,'+');
     }
     else
       description.append('-');
 
   }
 
 
   protected static void processSMBException(SmbException se, String documentIdentifier, String activity, String operation)
     throws ManifoldCFException, ServiceInterruption
   {
     // At least some of these are transport errors, and should be treated as service
     // interruptions.
     long currentTime = System.currentTimeMillis();
     Throwable cause = se.getRootCause();
     if (cause != null && (cause instanceof jcifs.util.transport.TransportException))
     {
       // See if it's an interruption
       jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException)cause;
       if (te.getRootCause() != null && te.getRootCause() instanceof java.lang.InterruptedException)
         throw new ManifoldCFException(te.getRootCause().getMessage(),te.getRootCause(),ManifoldCFException.INTERRUPTED);
       Logging.connectors.warn("JCIFS: Timeout "+activity+" for "+documentIdentifier+": retrying...",se);
       // Transport exceptions no longer abort when they give up, so we can't get notified that there is a problem.
 
       throw new ServiceInterruption("Timeout or other service interruption: "+cause.getMessage(),cause,currentTime + 300000L,
         currentTime + 12 * 60 * 60000L,-1,false);
     }
     if (se.getMessage().indexOf("busy") != -1)
     {
       Logging.connectors.warn("JCIFS: 'Busy' response when "+activity+" for "+documentIdentifier+": retrying...",se);
       // Busy exceptions just skip the document and keep going
       throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     else if (se.getMessage().indexOf("handle is invalid") != -1)
     {
       Logging.connectors.warn("JCIFS: 'Handle is invalid' response when "+activity+" for "+documentIdentifier+": retrying...",se);
       // Invalid handle errors treated like "busy"
       throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     else if (se.getMessage().indexOf("parameter is incorrect") != -1)
     {
       Logging.connectors.warn("JCIFS: 'Parameter is incorrect' response when "+activity+" for "+documentIdentifier+": retrying...",se);
       // Invalid handle errors treated like "busy"
       throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     else if (se.getMessage().indexOf("no longer available") != -1)
     {
       Logging.connectors.warn("JCIFS: 'No longer available' response when "+activity+" for "+documentIdentifier+": retrying...",se);
       // No longer available == busy
       throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     else if(se.getMessage().indexOf("No process is on the other end of the pipe") != -1)
     {
       Logging.connectors.warn("JCIFS: 'No process is on the other end of the pipe' response when "+activity+" for "+documentIdentifier+": retrying...",se);
       // 'No process is on the other end of the pipe' skip the document and keep going
       throw new ServiceInterruption("Timeout or other service interruption: "+se.getMessage(),se,currentTime + 300000L,
         currentTime + 3 * 60 * 60000L,-1,false);
     }
     else if (se.getMessage().indexOf("cannot find") != -1 || se.getMessage().indexOf("cannot be found") != -1)
     {
       return;
     }
     else if (se.getMessage().indexOf("is denied") != -1)
     {
       Logging.connectors.warn("JCIFS: Access exception when "+activity+" for "+documentIdentifier+" - skipping");
       return;
     }
     else if (se.getMessage().indexOf("Incorrect function") != -1)
     {
       Logging.connectors.error("JCIFS: Server does not support a required operation ("+operation+"?) for "+documentIdentifier);
       throw new ManifoldCFException("Server does not support a required operation ("+operation+", possibly?) accessing document "+documentIdentifier,se);
     }
     else
     {
       Logging.connectors.error("SmbException thrown "+activity+" for "+documentIdentifier,se);
       throw new ManifoldCFException("SmbException thrown: "+se.getMessage(),se);
     }
   }
 
   protected static int setDocumentSecurity(RepositoryDocument rd, String version, int startPosition)
   {
     if (startPosition < version.length() && version.charAt(startPosition++) == '+')
     {
       // Unpack share allow and share deny
       ArrayList shareAllowAcls = new ArrayList();
       startPosition = unpackList(shareAllowAcls,version,startPosition,'+');
       ArrayList shareDenyAcls = new ArrayList();
       startPosition = unpackList(shareDenyAcls,version,startPosition,'+');
       String[] shareAllow = new String[shareAllowAcls.size()];
       String[] shareDeny = new String[shareDenyAcls.size()];
       int i = 0;
       while (i < shareAllow.length)
       {
         shareAllow[i] = (String)shareAllowAcls.get(i);
         i++;
       }
       i = 0;
       while (i < shareDeny.length)
       {
         shareDeny[i] = (String)shareDenyAcls.get(i);
         i++;
       }
 
       // set share acls
       rd.setShareACL(shareAllow);
       rd.setShareDenyACL(shareDeny);
     }
     if (startPosition < version.length() && version.charAt(startPosition++) == '+')
     {
       // Unpack allow and deny acls
       ArrayList allowAcls = new ArrayList();
       startPosition = unpackList(allowAcls,version,startPosition,'+');
       ArrayList denyAcls = new ArrayList();
       startPosition = unpackList(denyAcls,version,startPosition,'+');
       String[] allow = new String[allowAcls.size()];
       String[] deny = new String[denyAcls.size()];
       int i = 0;
       while (i < allow.length)
       {
         allow[i] = (String)allowAcls.get(i);
         i++;
       }
       i = 0;
       while (i < deny.length)
       {
         deny[i] = (String)denyAcls.get(i);
         i++;
       }
 
       // set native file acls
       rd.setACL(allow);
       rd.setDenyACL(deny);
     }
     return startPosition;
   }
 
   protected static int setPathMetadata(RepositoryDocument rd, String version, int index)
     throws ManifoldCFException
   {
     if (version.length() > index && version.charAt(index++) == '+')
     {
       StringBuilder pathAttributeNameBuffer = new StringBuilder();
       StringBuilder pathAttributeValueBuffer = new StringBuilder();
       index = unpack(pathAttributeNameBuffer,version,index,'+');
       index = unpack(pathAttributeValueBuffer,version,index,'+');
       String pathAttributeName = pathAttributeNameBuffer.toString();
       String pathAttributeValue = pathAttributeValueBuffer.toString();
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("JCIFS: Path attribute name is '"+pathAttributeName+"'");
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("JCIFS: Path attribute value is '"+pathAttributeValue+"'");
       rd.addField(pathAttributeName,pathAttributeValue);
     }
     else
       Logging.connectors.debug("JCIFS: Path attribute name is null");
     return index;
   }
 
   /** Check status of connection.
   */
   @Override
 
   public String check()
     throws ManifoldCFException
   {
     getSession();
     String serverURI = smbconnectionPath;
     SmbFile server = null;
     try
     {
       server = new SmbFile(serverURI,pa);
     }
     catch (MalformedURLException e1)
     {
       return "Malformed URL: '"+serverURI+"': "+e1.getMessage();
     }
     try
     {
       // check to make sure it's a server or a folder
       int type = getFileType(server);
       if (type==SmbFile.TYPE_SERVER || type==SmbFile.TYPE_SHARE
         || type==SmbFile.TYPE_FILESYSTEM)
       {
         try
         {
           server.connect();
           if (!server.exists())
             return "Server or path does not exist";
         }
         catch (java.net.SocketTimeoutException e)
         {
           return "Timeout connecting to server: "+e.getMessage();
         }
         catch (InterruptedIOException e)
         {
           throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
         }
         catch (IOException e)
         {
           return "Couldn't connect to server: "+e.getMessage();
         }
         return super.check();
       }
       else
         return "URI is not a server URI: '"+serverURI+"'";
     }
     catch (SmbException e)
     {
       return "Could not connect: "+e.getMessage();
     }
   }
 
   // Protected methods
 
   /** Check if a file or directory should be included, given a document specification.
   *@param file is the file object.
   *@param fileName is the canonical file name.
   *@param documentSpecification is the specification.
   *@return true if it should be included.
   */
   protected boolean checkInclude(SmbFile file, String fileName, DocumentSpecification documentSpecification, IFingerprintActivity activities)
     throws ManifoldCFException, ServiceInterruption
   {
     if (Logging.connectors.isDebugEnabled())
       Logging.connectors.debug("JCIFS: In checkInclude for '"+fileName+"'");
 
     // This method does not attempt to do any fingerprinting.  Instead, it will opt to include any
     // file that may depend on fingerprinting, and exclude everything else.  The actual setup for
     // the fingerprinting test is in checkNeedFileData(), while the actual code that determines in vs.
     // out using the file data is in checkIngest().
     try
     {
       String pathPart;
       String filePart;
       boolean isDirectory = fileIsDirectory(file);
       if (isDirectory)
       {
 
         pathPart = fileName;
         filePart = null;
       }
       else
       {
         int lastSlash = fileName.lastIndexOf("/");
         if (lastSlash == -1)
         {
           pathPart = "";
           filePart = fileName;
         }
         else
         {
           // Pathpart has to include the slash
           pathPart = fileName.substring(0,lastSlash+1);
           filePart = fileName.substring(lastSlash+1);
         }
       }
 
       // If it's a file, make sure the maximum length is not exceeded
       int i;
       if (!isDirectory)
       {
         long fileLength = fileLength(file);
         if (!activities.checkLengthIndexable(fileLength))
           return false;
         long maxFileLength = Long.MAX_VALUE;
         i = 0;
         while (i < documentSpecification.getChildCount())
         {
           SpecificationNode sn = documentSpecification.getChild(i++);
           if (sn.getType().equals(NODE_MAXLENGTH))
           {
             try
             {
               String value = sn.getAttributeValue(ATTRIBUTE_VALUE);
               if (value != null && value.length() > 0)
                 maxFileLength = new Long(value).longValue();
             }
             catch (NumberFormatException e)
             {
               throw new ManifoldCFException("Bad number: "+e.getMessage(),e);
             }
           }
         }
         if (fileLength > maxFileLength)
           return false;
       }
 
       // Scan until we match a startpoint
       i = 0;
       while (i < documentSpecification.getChildCount())
       {
         SpecificationNode sn = documentSpecification.getChild(i++);
         if (sn.getType().equals(NODE_STARTPOINT))
         {
           // Prepend the server URL to the path, since that's what pathpart will have.
           String path = mapToIdentifier(sn.getAttributeValue(ATTRIBUTE_PATH));
 
           // Compare with filename
           if (Logging.connectors.isDebugEnabled())
             Logging.connectors.debug("JCIFS: Matching startpoint '"+path+"' against actual '"+pathPart+"'");
           int matchEnd = matchSubPath(path,pathPart);
           if (matchEnd == -1)
           {
             Logging.connectors.debug("JCIFS: No match");
             continue;
           }
 
           Logging.connectors.debug("JCIFS: Startpoint found!");
 
           // If this is the root, it's always included.
           if (matchEnd == fileName.length())
           {
             Logging.connectors.debug("JCIFS: Startpoint: always included");
             return true;
           }
 
           // matchEnd is the start of the rest of the path (after the match) in fileName.
           // We need to walk through the rules and see whether it's in or out.
           int j = 0;
           while (j < sn.getChildCount())
           {
             SpecificationNode node = sn.getChild(j++);
             String flavor = node.getType();
             if (flavor.equals(NODE_INCLUDE) || flavor.equals(NODE_EXCLUDE))
             {
               String type = node.getAttributeValue(ATTRIBUTE_TYPE);
               if (type == null)
                 type = "";
               String indexable = node.getAttributeValue(ATTRIBUTE_INDEXABLE);
               if (indexable == null)
                 indexable = "";
               String match = node.getAttributeValue(ATTRIBUTE_FILESPEC);
 
               // Check if there's a match against the filespec
               if (Logging.connectors.isDebugEnabled())
                 Logging.connectors.debug("JCIFS: Checking '"+match+"' against '"+fileName.substring(matchEnd-1)+"'");
               boolean isMatch = checkMatch(fileName,matchEnd-1,match);
               boolean isKnown = true;
 
               // Check the directory/file criteria
               if (isMatch)
               {
                 Logging.connectors.debug("JCIFS: Match found.");
                 isMatch = type.length() == 0 ||
                   (type.equals(VALUE_DIRECTORY) && isDirectory) ||
                   (type.equals(VALUE_FILE) && !isDirectory);
               }
               else
                 Logging.connectors.debug("JCIFS: No match!");
 
               // Check the indexable criteria
               if (isMatch)
               {
                 if (indexable.length() != 0)
                 {
                   // Directories are never considered indexable.
                   // But if this is not a directory, things become ambiguous.
                   boolean isIndexable;
                   if (isDirectory)
                   {
                     isIndexable = false;
                     isMatch = (indexable.equals("yes") && isIndexable) ||
                       (indexable.equals("no") && !isIndexable);
                   }
                   else
                     isKnown = false;
 
                 }
               }
 
               if (isKnown)
               {
                 if (isMatch)
                 {
                   if (flavor.equals(NODE_INCLUDE))
                     return true;
                   else
                     return false;
                 }
               }
               else
               {
                 // Not known
                 // What we do depends on whether this is an include rule or an exclude one.
                 // We want to err on the side of inclusion, which means for include rules
                 // we return true, and for exclude rules we simply continue.
                 if (flavor.equals(NODE_INCLUDE))
                   return true;
                 // Continue
               }
             }
           }
 
         }
       }
       return false;
     }
     catch (jcifs.smb.SmbAuthException e)
     {
       Logging.connectors.warn("JCIFS: Authorization exception checking inclusion for "+fileName+" - skipping");
       return false;
     }
     catch (SmbException se)
     {
       processSMBException(se, fileName, "checking inclusion", "canonical path mapping");
       return false;
     }
     catch (java.net.SocketTimeoutException e)
     {
       throw new ManifoldCFException("Couldn't map to canonical path: "+e.getMessage(),e);
     }
     catch (InterruptedIOException e)
     {
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("Couldn't map to canonical path: "+e.getMessage(),e);
     }
     finally
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("JCIFS: Leaving checkInclude for '"+fileName+"'");
     }
 
   }
 
   /** Pretend that a file is either indexable or not, and return whether or not it would be ingested.
   * This is only ever called for files.
   *@param fileName is the canonical file name.
   *@param documentSpecification is the specification.
   *@param pretendIndexable should be set to true if the document's contents would be fingerprinted as "indexable",
   *       or false otherwise.
   *@return true if the file would be ingested given the parameters.
   */
   protected boolean wouldFileBeIncluded(String fileName, DocumentSpecification documentSpecification,
     boolean pretendIndexable)
     throws ManifoldCFException
   {
     if (Logging.connectors.isDebugEnabled())
       Logging.connectors.debug("JCIFS: In wouldFileBeIncluded for '"+fileName+"', pretendIndexable="+(pretendIndexable?"true":"false"));
 
     // This file was flagged as needing file data.  However, that doesn't tell us *for what* we need it.
     // So we need to redo the decision tree, but this time do everything completely.
 
     try
     {
       String pathPart;
       String filePart;
       boolean isDirectory = false;
 
       int lastSlash = fileName.lastIndexOf("/");
       if (lastSlash == -1)
       {
         pathPart = "";
         filePart = fileName;
       }
       else
       {
         pathPart = fileName.substring(0,lastSlash+1);
         filePart = fileName.substring(lastSlash+1);
       }
 
       // Scan until we match a startpoint
       int i = 0;
       while (i < documentSpecification.getChildCount())
       {
         SpecificationNode sn = documentSpecification.getChild(i++);
         if (sn.getType().equals(NODE_STARTPOINT))
         {
           // Prepend the server URL to the path, since that's what pathpart will have.
           String path = mapToIdentifier(sn.getAttributeValue(ATTRIBUTE_PATH));
 
           // Compare with filename
           int matchEnd = matchSubPath(path,pathPart);
           if (matchEnd == -1)
           {
             continue;
           }
 
           // matchEnd is the start of the rest of the path (after the match) in fileName.
           // We need to walk through the rules and see whether it's in or out.
           int j = 0;
           while (j < sn.getChildCount())
           {
             SpecificationNode node = sn.getChild(j++);
             String flavor = node.getType();
             if (flavor.equals(NODE_INCLUDE) || flavor.equals(NODE_EXCLUDE))
             {
               String type = node.getAttributeValue(ATTRIBUTE_TYPE);
               if (type == null)
                 type = "";
               String indexable = node.getAttributeValue(ATTRIBUTE_INDEXABLE);
               if (indexable == null)
                 indexable = "";
               String match = node.getAttributeValue(ATTRIBUTE_FILESPEC);
 
               // Check if there's a match against the filespec
               boolean isMatch = checkMatch(fileName,matchEnd-1,match);
 
               // Check the directory/file criteria
               if (isMatch)
               {
                 isMatch = type.length() == 0 ||
                   (type.equals(VALUE_DIRECTORY) && isDirectory) ||
                   (type.equals(VALUE_FILE) && !isDirectory);
               }
 
               // Check the indexable criteria
               if (isMatch)
               {
                 if (indexable.length() != 0)
                 {
                   // Directories are never considered indexable.
                   // But if this is not a directory, things become ambiguous.
                   boolean isIndexable;
                   if (isDirectory)
                     isIndexable = false;
                   else
                   {
                     isIndexable = pretendIndexable;
                   }
 
                   isMatch = (indexable.equals("yes") && isIndexable) ||
                     (indexable.equals("no") && !isIndexable);
 
 
                 }
               }
 
               if (isMatch)
               {
                 if (flavor.equals(NODE_INCLUDE))
                   return true;
                 else
                   return false;
               }
             }
           }
 
         }
       }
       return false;
     }
     catch (java.net.SocketTimeoutException e)
     {
       throw new ManifoldCFException("Couldn't map to canonical path: "+e.getMessage(),e);
     }
     catch (InterruptedIOException e)
     {
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("Couldn't map to canonical path: "+e.getMessage(),e);
     }
     finally
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("JCIFS: Leaving wouldFileBeIncluded for '"+fileName+"'");
     }
   }
 
   /** Check to see whether we need the contents of the file for anything.  We do this by assuming that
   * the file is indexable, and assuming that it's not, and seeing if the same thing would happen.
   *@param fileName is the name of the file.
   *@param documentSpecification is the document specification.
   *@return true if the file needs to be fingerprinted.
   */
   protected boolean checkNeedFileData(String fileName, DocumentSpecification documentSpecification)
     throws ManifoldCFException
   {
     return wouldFileBeIncluded(fileName,documentSpecification,true) != wouldFileBeIncluded(fileName,documentSpecification,false);
   }
 
   /** Check if a file should be ingested, given a document specification and a local copy of the
   * file.  It is presumed that only files that passed checkInclude() and were also flagged as needing
   * file data by checkNeedFileData() will be checked by this method.
   *@param localFile is the file.
   *@param fileName is the JCIFS file name.
   *@param documentSpecification is the specification.
   *@param activities are the activities available to determine indexability.
   *@return true if the file should be ingested.
   */
   protected boolean checkIngest(File localFile, String fileName, DocumentSpecification documentSpecification, IFingerprintActivity activities)
     throws ManifoldCFException, ServiceInterruption
   {
     if (Logging.connectors.isDebugEnabled())
       Logging.connectors.debug("JCIFS: In checkIngest for '"+fileName+"'");
 
     // This file was flagged as needing file data.  However, that doesn't tell us *for what* we need it.
     // So we need to redo the decision tree, but this time do everything completely.
 
     try
     {
       String pathPart;
       String filePart;
       boolean isDirectory = false;
 
       int lastSlash = fileName.lastIndexOf("/");
       if (lastSlash == -1)
       {
         pathPart = "";
         filePart = fileName;
       }
       else
       {
         pathPart = fileName.substring(0,lastSlash+1);
         filePart = fileName.substring(lastSlash+1);
       }
 
       // Scan until we match a startpoint
       int i = 0;
       while (i < documentSpecification.getChildCount())
       {
         SpecificationNode sn = documentSpecification.getChild(i++);
         if (sn.getType().equals(NODE_STARTPOINT))
         {
           // Prepend the server URL to the path, since that's what pathpart will have.
           String path = mapToIdentifier(sn.getAttributeValue(ATTRIBUTE_PATH));
 
           // Compare with filename
           int matchEnd = matchSubPath(path,pathPart);
           if (matchEnd == -1)
           {
             continue;
           }
 
           // matchEnd is the start of the rest of the path (after the match) in fileName.
           // We need to walk through the rules and see whether it's in or out.
           int j = 0;
           while (j < sn.getChildCount())
           {
             SpecificationNode node = sn.getChild(j++);
             String flavor = node.getType();
             if (flavor.equals(NODE_INCLUDE) || flavor.equals(NODE_EXCLUDE))
             {
               String type = node.getAttributeValue(ATTRIBUTE_TYPE);
               if (type == null)
                 type = "";
               String indexable = node.getAttributeValue(ATTRIBUTE_INDEXABLE);
               if (indexable == null)
                 indexable = "";
               String match = node.getAttributeValue(ATTRIBUTE_FILESPEC);
 
               // Check if there's a match against the filespec
               boolean isMatch = checkMatch(fileName,matchEnd-1,match);
 
               // Check the directory/file criteria
               if (isMatch)
               {
                 isMatch = type.length() == 0 ||
                   (type.equals(VALUE_DIRECTORY) && isDirectory) ||
                   (type.equals(VALUE_FILE) && !isDirectory);
               }
 
               // Check the indexable criteria
               if (isMatch)
               {
                 if (indexable.length() != 0)
                 {
                   // Directories are never considered indexable.
                   // But if this is not a directory, things become ambiguous.
                   boolean isIndexable;
                   if (isDirectory)
                     isIndexable = false;
                   else
                   {
                     isIndexable = activities.checkDocumentIndexable(localFile);
                   }
 
                   isMatch = (indexable.equals("yes") && isIndexable) ||
                     (indexable.equals("no") && !isIndexable);
 
 
                 }
               }
 
               if (isMatch)
               {
                 if (flavor.equals(NODE_INCLUDE))
                   return true;
                 else
                   return false;
               }
             }
           }
 
         }
       }
       return false;
     }
     catch (jcifs.smb.SmbAuthException e)
     {
       Logging.connectors.warn("JCIFS: Authorization exception checking ingestion for "+fileName+" - skipping");
       return false;
     }
     catch (SmbException se)
     {
       processSMBException(se, fileName, "checking ingestion", "reading document");
       return false;
     }
     catch (java.net.SocketTimeoutException e)
     {
       throw new ManifoldCFException("Couldn't map to canonical path: "+e.getMessage(),e);
     }
     catch (InterruptedIOException e)
     {
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("Couldn't map to canonical path: "+e.getMessage(),e);
     }
     finally
     {
       if (Logging.connectors.isDebugEnabled())
         Logging.connectors.debug("JCIFS: Leaving checkIngest for '"+fileName+"'");
     }
 
   }
 
   /** Match a sub-path.  The sub-path must match the complete starting part of the full path, in a path
   * sense.  The returned value should point into the file name beyond the end of the matched path, or
   * be -1 if there is no match.
   *@param subPath is the sub path.
   *@param fullPath is the full path.
   *@return the index of the start of the remaining part of the full path, or -1.
   */
   protected static int matchSubPath(String subPath, String fullPath)
   {
     if (subPath.length() > fullPath.length())
       return -1;
     if (fullPath.startsWith(subPath) == false)
       return -1;
     int rval = subPath.length();
     if (fullPath.length() == rval)
       return rval;
     char x = fullPath.charAt(rval);
     if (x == File.separatorChar)
       rval++;
     return rval;
   }
 
   /** Check a match between two strings with wildcards.
   *@param sourceMatch is the expanded string (no wildcards)
   *@param sourceIndex is the starting point in the expanded string.
   *@param match is the wildcard-based string.
   *@return true if there is a match.
   */
   protected static boolean checkMatch(String sourceMatch, int sourceIndex, String match)
   {
     // Note: The java regex stuff looks pretty heavyweight for this purpose.
     // I've opted to try and do a simple recursive version myself, which is not compiled.
     // Basically, the match proceeds by recursive descent through the string, so that all *'s cause
     // recursion.
     boolean caseSensitive = false;
 
     return processCheck(caseSensitive, sourceMatch, sourceIndex, match, 0);
   }
 
   /** Recursive worker method for checkMatch.  Returns 'true' if there is a path that consumes both
   * strings in their entirety in a matched way.
   *@param caseSensitive is true if file names are case sensitive.
   *@param sourceMatch is the source string (w/o wildcards)
   *@param sourceIndex is the current point in the source string.
   *@param match is the match string (w/wildcards)
   *@param matchIndex is the current point in the match string.
   *@return true if there is a match.
   */
   protected static boolean processCheck(boolean caseSensitive, String sourceMatch, int sourceIndex,
     String match, int matchIndex)
   {
     // Logging.connectors.debug("Matching '"+sourceMatch+"' position "+Integer.toString(sourceIndex)+
     //      " against '"+match+"' position "+Integer.toString(matchIndex));
 
     // Match up through the next * we encounter
     while (true)
     {
       // If we've reached the end, it's a match.
       if (sourceMatch.length() == sourceIndex && match.length() == matchIndex)
         return true;
       // If one has reached the end but the other hasn't, no match
       if (match.length() == matchIndex)
         return false;
       if (sourceMatch.length() == sourceIndex)
       {
         if (match.charAt(matchIndex) != '*')
           return false;
         matchIndex++;
         continue;
       }
       char x = sourceMatch.charAt(sourceIndex);
       char y = match.charAt(matchIndex);
       if (!caseSensitive)
       {
         if (x >= 'A' && x <= 'Z')
           x -= 'A'-'a';
         if (y >= 'A' && y <= 'Z')
           y -= 'A'-'a';
       }
       if (y == '*')
       {
         // Wildcard!
         // We will recurse at this point.
         // Basically, we want to combine the results for leaving the "*" in the match string
         // at this point and advancing the source index, with skipping the "*" and leaving the source
         // string alone.
         return processCheck(caseSensitive,sourceMatch,sourceIndex+1,match,matchIndex) ||
           processCheck(caseSensitive,sourceMatch,sourceIndex,match,matchIndex+1);
       }
       if (y == '?' || x == y)
       {
         sourceIndex++;
         matchIndex++;
       }
       else
         return false;
     }
   }
 
   /** Grab forced acl out of document specification.
   *@param spec is the document specification.
   *@return the acls.
   */
   protected static String[] getForcedAcls(DocumentSpecification spec)
   {
     HashMap map = new HashMap();
     int i = 0;
     boolean securityOn = true;
     while (i < spec.getChildCount())
     {
       SpecificationNode sn = spec.getChild(i++);
       if (sn.getType().equals(NODE_ACCESS))
       {
         String token = sn.getAttributeValue(ATTRIBUTE_TOKEN);
         map.put(token,token);
       }
       else if (sn.getType().equals(NODE_SECURITY))
       {
         String value = sn.getAttributeValue(ATTRIBUTE_VALUE);
         if (value.equals("on"))
           securityOn = true;
         else if (value.equals("off"))
           securityOn = false;
       }
     }
     if (!securityOn)
       return null;
 
     String[] rval = new String[map.size()];
     Iterator iter = map.keySet().iterator();
     i = 0;
     while (iter.hasNext())
     {
       rval[i++] = (String)iter.next();
     }
     return rval;
   }
 
   /** Grab forced share acls out of document specification.
   *@param spec is the document specification.
   *@return the acls.
   */
   protected static String[] getForcedShareAcls(DocumentSpecification spec)
   {
     HashMap map = new HashMap();
     int i = 0;
     boolean securityOn = true;
     while (i < spec.getChildCount())
     {
       SpecificationNode sn = spec.getChild(i++);
       if (sn.getType().equals(NODE_SHAREACCESS))
       {
         String token = sn.getAttributeValue(ATTRIBUTE_TOKEN);
         map.put(token,token);
       }
       else if (sn.getType().equals(NODE_SHARESECURITY))
       {
         String value = sn.getAttributeValue(ATTRIBUTE_VALUE);
         if (value.equals("on"))
           securityOn = true;
         else if (value.equals("off"))
           securityOn = false;
       }
     }
     if (!securityOn)
       return null;
     String[] rval = new String[map.size()];
     Iterator iter = map.keySet().iterator();
     i = 0;
     while (iter.hasNext())
     {
       rval[i++] = (String)iter.next();
     }
     return rval;
   }
 
   /** Map a "path" specification to a full identifier.
   */
   protected String mapToIdentifier(String path)
     throws IOException
   {
     String smburi = smbconnectionPath;
     String uri = smburi + path + "/";
     return getFileCanonicalPath(new SmbFile(uri,pa));
   }
 
   // These methods allow me to experiment with cluster-mandated error handling on an entirely local level.  They correspond to individual SMBFile methods.
 
   /** Get canonical path */
   protected static String getFileCanonicalPath(SmbFile file)
   {
     return file.getCanonicalPath();
   }
 
   /** Check for file/directory existence */
   protected static boolean fileExists(SmbFile file)
     throws SmbException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     SmbException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.exists();
       }
       catch (SmbException e)
       {
         // If it's an interruption, throw it right away.
         Throwable cause = e.getRootCause();
         if (cause != null && (cause instanceof jcifs.util.transport.TransportException))
         {
           // See if it's an interruption
           jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException)cause;
           if (te.getRootCause() != null && te.getRootCause() instanceof java.lang.InterruptedException)
             throw e;
         }
 
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while checking if file exists: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentSmbExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** Check if file is a directory */
   protected static boolean fileIsDirectory(SmbFile file)
     throws SmbException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     SmbException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.isDirectory();
       }
       catch (SmbException e)
       {
         // If it's an interruption, throw it right away.
         Throwable cause = e.getRootCause();
         if (cause != null && (cause instanceof jcifs.util.transport.TransportException))
         {
           // See if it's an interruption
           jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException)cause;
           if (te.getRootCause() != null && te.getRootCause() instanceof java.lang.InterruptedException)
             throw e;
         }
 
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while seeing if file is a directory: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentSmbExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** Get last modified date for file */
   protected static long fileLastModified(SmbFile file)
     throws SmbException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     SmbException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.lastModified();
       }
       catch (SmbException e)
       {
         // If it's an interruption, throw it right away.
         Throwable cause = e.getRootCause();
         if (cause != null && (cause instanceof jcifs.util.transport.TransportException))
         {
           // See if it's an interruption
           jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException)cause;
           if (te.getRootCause() != null && te.getRootCause() instanceof java.lang.InterruptedException)
             throw e;
         }
 
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while getting file last-modified date: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentSmbExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** Get file length */
   protected static long fileLength(SmbFile file)
     throws SmbException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     SmbException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.length();
       }
       catch (SmbException e)
       {
         // If it's an interruption, throw it right away.
         Throwable cause = e.getRootCause();
         if (cause != null && (cause instanceof jcifs.util.transport.TransportException))
         {
           // See if it's an interruption
           jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException)cause;
           if (te.getRootCause() != null && te.getRootCause() instanceof java.lang.InterruptedException)
             throw e;
         }
 
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while getting file length: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentSmbExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** List files */
   protected static SmbFile[] fileListFiles(SmbFile file, SmbFileFilter filter)
     throws SmbException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     SmbException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.listFiles(filter);
       }
       catch (SmbException e)
       {
         // If it's an interruption, throw it right away.
         Throwable cause = e.getRootCause();
         if (cause != null && (cause instanceof jcifs.util.transport.TransportException))
         {
           // See if it's an interruption
           jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException)cause;
           if (te.getRootCause() != null && te.getRootCause() instanceof java.lang.InterruptedException)
             throw e;
         }
 
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while listing files: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentSmbExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** Get input stream for file */
   protected static InputStream getFileInputStream(SmbFile file)
     throws IOException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     IOException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.getInputStream();
       }
       catch (java.net.SocketTimeoutException e)
       {
         throw e;
       }
       catch (InterruptedIOException e)
       {
         throw e;
       }
       catch (IOException e)
       {
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while getting file input stream: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentIOExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** Get file security */
   protected static ACE[] getFileSecurity(SmbFile file, boolean useSIDs)
     throws IOException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     IOException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.getSecurity(!useSIDs);
       }
       catch (java.net.SocketTimeoutException e)
       {
         throw e;
       }
       catch (InterruptedIOException e)
       {
         throw e;
       }
       catch (IOException e)
       {
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while getting file security: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentIOExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** Get share security */
   protected static ACE[] getFileShareSecurity(SmbFile file, boolean useSIDs)
     throws IOException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     IOException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.getShareSecurity(!useSIDs);
       }
       catch (java.net.SocketTimeoutException e)
       {
         throw e;
       }
       catch (InterruptedIOException e)
       {
         throw e;
       }
       catch (IOException e)
       {
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while getting share security: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentIOExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** Get file type */
   protected static int getFileType(SmbFile file)
     throws SmbException
   {
     int totalTries = 0;
     int retriesRemaining = 3;
     SmbException currentException = null;
     while (retriesRemaining > 0 && totalTries < 5)
     {
       retriesRemaining--;
       totalTries++;
       try
       {
         return file.getType();
       }
       catch (SmbException e)
       {
         // If it's an interruption, throw it right away.
         Throwable cause = e.getRootCause();
         if (cause != null && (cause instanceof jcifs.util.transport.TransportException))
         {
           // See if it's an interruption
           jcifs.util.transport.TransportException te = (jcifs.util.transport.TransportException)cause;
           if (te.getRootCause() != null && te.getRootCause() instanceof java.lang.InterruptedException)
             throw e;
         }
 
         Logging.connectors.warn("JCIFS: Possibly transient exception detected on attempt "+Integer.toString(totalTries)+" while getting file type: "+e.getMessage(),e);
         if (currentException != null)
         {
           // Compare exceptions.  If they differ, reset the retry count.
           if (!equivalentSmbExceptions(currentException,e))
             retriesRemaining = 3;
         }
         currentException = e;
       }
     }
     throw currentException;
   }
 
   /** Check if two SmbExceptions are equivalent */
   protected static boolean equivalentSmbExceptions(SmbException e1, SmbException e2)
   {
     // The thing we want to compare is the message.  This is a little risky in that if there are (for example) object addresses in the message, the comparison will always fail.
     // However, I don't think we expect any such thing in this case.
     String e1m = e1.getMessage();
     String e2m = e2.getMessage();
     if (e1m == null)
       e1m = "";
     if (e2m == null)
       e2m = "";
     return e1m.equals(e2m);
   }
 
   /** Check if two IOExceptions are equivalent */
   protected static boolean equivalentIOExceptions(IOException e1, IOException e2)
   {
     // The thing we want to compare is the message.  This is a little risky in that if there are (for example) object addresses in the message, the comparison will always fail.
     // However, I don't think we expect any such thing in this case.
     String e1m = e1.getMessage();
     String e2m = e2.getMessage();
     if (e1m == null)
       e1m = "";
     if (e2m == null)
       e2m = "";
     return e1m.equals(e2m);
   }
 
   /** Document identifier stream.
   */
   protected class IdentifierStream implements IDocumentIdentifierStream
   {
     protected String[] ids = null;
     protected int currentIndex = 0;
 
     public IdentifierStream(DocumentSpecification spec)
       throws ManifoldCFException
     {
       try
       {
         // Walk the specification for the "startpoint" types.  Amalgamate these into a list of strings.
         // Presume that all roots are startpoint nodes
         int i = 0;
         int j = 0;
         while (i < spec.getChildCount())
         {
           SpecificationNode n = spec.getChild(i);
           if (n.getType().equals(NODE_STARTPOINT))
             j++;
           i++;
         }
         ids = new String[j];
         i = 0;
         j = 0;
         while (i < ids.length)
         {
           SpecificationNode n = spec.getChild(i);
           if (n.getType().equals(NODE_STARTPOINT))
           {
             // The id returned MUST be in canonical form!!!
             ids[j] = mapToIdentifier(n.getAttributeValue(ATTRIBUTE_PATH));
 
             if (Logging.connectors.isDebugEnabled())
             {
               Logging.connectors.debug("Seed = '"+ids[j]+"'");
             }
             j++;
           }
           i++;
         }
       }
       catch (java.net.SocketTimeoutException e)
       {
         throw new ManifoldCFException("Couldn't map to canonical path: "+e.getMessage(),e);
       }
       catch (InterruptedIOException e)
       {
         throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
       }
       catch (IOException e)
 
       {
         throw new ManifoldCFException("Could not get a canonical path: "+e.getMessage(),e);
       }
     }
 
     /** Get the next identifier.
     *@return the next document identifier, or null if there are no more.
     */
     public String getNextIdentifier()
       throws ManifoldCFException, ServiceInterruption
     {
       if (currentIndex == ids.length)
         return null;
       return ids[currentIndex++];
     }
 
     /** Close the stream.
     */
     public void close()
       throws ManifoldCFException
     {
       ids = null;
     }
 
   }
 
   // UI support methods.
   //
   // These support methods come in two varieties.  The first bunch is involved in setting up connection configuration information.  The second bunch
   // is involved in presenting and editing document specification information for a job.  The two kinds of methods are accordingly treated differently,
   // in that the first bunch cannot assume that the current connector object is connected, while the second bunch can.  That is why the first bunch
   // receives a thread context argument for all UI methods, while the second bunch does not need one (since it has already been applied via the connect()
   // method, above).
     
   /** Output the configuration header section.
   * This method is called in the head section of the connector's configuration page.  Its purpose is to add the required tabs to the list, and to output any
   * javascript methods that might be needed by the configuration editing HTML.
   *@param threadContext is the local thread context.
   *@param out is the output to which any HTML should be sent.
   *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
   *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
   */
   @Override
   public void outputConfigurationHeader(IThreadContext threadContext, IHTTPOutput out,
     Locale locale, ConfigParams parameters, List<String> tabsArray)
     throws ManifoldCFException, IOException
   {
     tabsArray.add(Messages.getString(locale,"SharedDriveConnector.Server"));
     out.print(
 "<script type=\"text/javascript\">\n"+
 "<!--\n"+
 "function checkConfigForSave()\n"+
 "{\n"+
 "  if (editconnection.server.value == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.NeedAServerName") + "\");\n"+
 "    SelectTab(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.Server2") + "\");\n"+
 "    editconnection.server.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "\n"+
 "  if (editconnection.server.value.indexOf(\"/\") != -1)\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.ServerNameCannotIncludePathInformation") + "\");\n"+
 "    SelectTab(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.Server2") + "\");\n"+
 "    editconnection.server.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "		\n"+
 "  if (editconnection.username.value == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.NeedAUserName") + "\");\n"+
 "    SelectTab(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.Server2") + "\");\n"+
 "    editconnection.username.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "\n"+
 "  return true;\n"+
 "}\n"+
 "\n"+
 "//-->\n"+
 "</script>\n"
     );
   }
   
   /** Output the configuration body section.
   * This method is called in the body section of the connector's configuration page.  Its purpose is to present the required form elements for editing.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags.  The name of the
   * form is "editconnection".
   *@param threadContext is the local thread context.
   *@param out is the output to which any HTML should be sent.
   *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
   *@param tabName is the current tab name.
   */
   @Override
   public void outputConfigurationBody(IThreadContext threadContext, IHTTPOutput out,
     Locale locale, ConfigParams parameters, String tabName)
     throws ManifoldCFException, IOException
   {
     String server   = parameters.getParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.server);
     if (server==null) server = "";
     String domain = parameters.getParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.domain);
     if (domain==null) domain = "";
     String username = parameters.getParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.username);
     if (username==null) username = "";
     String password = parameters.getObfuscatedParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.password);
     if (password==null) password = "";
     String resolvesids = parameters.getParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.useSIDs);
     if (resolvesids==null) resolvesids = "true";
 
     // "Server" tab
     if (tabName.equals(Messages.getString(locale,"SharedDriveConnector.Server")))
     {
       out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.Server3") + "</nobr></td>\n"+
 "    <td class=\"value\"><input type=\"text\" size=\"32\" name=\"server\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(server)+"\"/></td>\n"+
 "  </tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.AuthenticationDomain") + "</nobr></td>\n"+
 "    <td class=\"value\"><input type=\"text\" size=\"32\" name=\"domain\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(domain)+"\"/></td>\n"+
 "  </tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.UserName") + "</nobr></td>\n"+
 "    <td class=\"value\"><input type=\"text\" size=\"32\" name=\"username\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(username)+"\"/></td>\n"+
 "  </tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.Password") + "</nobr></td>\n"+
 "    <td class=\"value\"><input type=\"password\" size=\"32\" name=\"password\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(password)+"\"/></td>\n"+
 "  </tr>\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.UseSIDSForSecurity") + "</nobr></td>\n"+
 "    <td class=\"value\"><input type=\"hidden\" name=\"resolvesidspresent\" value=\"true\"/><input type=\"checkbox\" value=\"true\" name=\"resolvesids\" "+("true".equals(resolvesids)?"checked=\"true\"":"")+"/></td>\n"+
 "  </tr>\n"+
 "</table>\n"
       );
     }
     else
     {
       out.print(
 "<input type=\"hidden\" name=\"server\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(server)+"\"/>\n"+
 "<input type=\"hidden\" name=\"domain\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(domain)+"\"/>\n"+
 "<input type=\"hidden\" name=\"username\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(username)+"\"/>\n"+
 "<input type=\"hidden\" name=\"password\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(password)+"\"/>\n"+
 "<input type=\"hidden\" name=\"resolvesidspresent\" value=\"true\"/>\n"+
 "<input type=\"hidden\" name=\"resolvesids\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(resolvesids)+"\"/>\n"
       );
     }
   }
   
   /** Process a configuration post.
   * This method is called at the start of the connector's configuration page, whenever there is a possibility that form data for a connection has been
   * posted.  Its purpose is to gather form information and modify the configuration parameters accordingly.
   * The name of the posted form is "editconnection".
   *@param threadContext is the local thread context.
   *@param variableContext is the set of variables available from the post, including binary file post information.
   *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
   *@return null if all is well, or a string error message if there is an error that should prevent saving of the connection (and cause a redirection to an error page).
   */
   @Override
   public String processConfigurationPost(IThreadContext threadContext, IPostParameters variableContext,
     Locale locale, ConfigParams parameters)
     throws ManifoldCFException
   {
     String server = variableContext.getParameter("server");
     if (server != null)
       parameters.setParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.server,server);
 	
     String domain = variableContext.getParameter("domain");
     if (domain != null)
       parameters.setParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.domain,domain);
 	
     String username = variableContext.getParameter("username");
     if (username != null)
       parameters.setParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.username,username);
 		
     String password = variableContext.getParameter("password");
     if (password != null)
       parameters.setObfuscatedParameter(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveParameters.password,password);
     
     String resolvesidspresent = variableContext.getParameter("resolvesidspresent");
     if (resolvesidspresent != null)
     {
       parameters.setParameter(SharedDriveParameters.useSIDs,"false");
       String resolvesids = variableContext.getParameter("resolvesids");
       if (resolvesids != null)
         parameters.setParameter(SharedDriveParameters.useSIDs, resolvesids);
     }
     return null;
   }
   
   /** View configuration.
   * This method is called in the body section of the connector's view configuration page.  Its purpose is to present the connection information to the user.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *@param threadContext is the local thread context.
   *@param out is the output to which any HTML should be sent.
   *@param parameters are the configuration parameters, as they currently exist, for this connection being configured.
   */
   @Override
   public void viewConfiguration(IThreadContext threadContext, IHTTPOutput out, Locale locale, ConfigParams parameters)
     throws ManifoldCFException, IOException
   {
     out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr>\n"+
 "    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.Parameters") + "</nobr></td>\n"+
 "    <td class=\"value\" colspan=\"3\">\n"
     );
     Iterator iter = parameters.listParameters();
     while (iter.hasNext())
     {
       String param = (String)iter.next();
       String value = parameters.getParameter(param);
       if (param.length() >= "password".length() && param.substring(param.length()-"password".length()).equalsIgnoreCase("password"))
       {
         out.print(
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"=********</nobr><br/>\n"
         );
       }
       else if (param.length() >="keystore".length() && param.substring(param.length()-"keystore".length()).equalsIgnoreCase("keystore"))
       {
         IKeystoreManager kmanager = KeystoreManagerFactory.make("",value);
         out.print(
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"=&lt;"+Integer.toString(kmanager.getContents().length)+ Messages.getBodyString(locale,"SharedDriveConnector.certificate") + "&gt;</nobr><br/>\n"
         );
       }
       else
       {
         out.print(
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(param)+"="+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(value)+"</nobr><br/>\n"
         );
       }
     }
     out.print(
 "    </td>\n"+
 "  </tr>\n"+
 "</table>\n"
     );
   }
   
   /** Output the specification header section.
   * This method is called in the head section of a job page which has selected a repository connection of the current type.  Its purpose is to add the required tabs
   * to the list, and to output any javascript methods that might be needed by the job editing HTML.
   *@param out is the output to which any HTML should be sent.
   *@param ds is the current document specification for this job.
   *@param tabsArray is an array of tab names.  Add to this array any tab names that are specific to the connector.
   */
   @Override
   public void outputSpecificationHeader(IHTTPOutput out, Locale locale, DocumentSpecification ds, List<String> tabsArray)
     throws ManifoldCFException, IOException
   {
     tabsArray.add(Messages.getString(locale,"SharedDriveConnector.Paths"));
     tabsArray.add(Messages.getString(locale,"SharedDriveConnector.Security"));
     tabsArray.add(Messages.getString(locale,"SharedDriveConnector.Metadata"));
     tabsArray.add(Messages.getString(locale,"SharedDriveConnector.ContentLength"));
     tabsArray.add(Messages.getString(locale,"SharedDriveConnector.FileMapping"));
     tabsArray.add(Messages.getString(locale,"SharedDriveConnector.URLMapping"));
     out.print(
 "<script type=\"text/javascript\">\n"+
 "//<!--\n"+
 "\n"+
 "function checkSpecification()\n"+
 "{\n"+
 "  if (editjob.specmaxlength.value != \"\" && !isInteger(editjob.specmaxlength.value))\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.NeedAValidNumberForMaximumDocumentLength") + "\");\n"+
 "    editjob.specmaxlength.focus();\n"+
 "    return false;\n"+
 "  }\n"+
 "  return true;\n"+
 "}\n"+
 "\n"+
 "function SpecOp(n, opValue, anchorvalue)\n"+
 "{\n"+
 "  eval(\"editjob.\"+n+\".value = \\\"\"+opValue+\"\\\"\");\n"+
 "  postFormSetAnchor(anchorvalue);\n"+
 "}\n"+
 "\n"+
 "function SpecAddToPath(anchorvalue)\n"+
 "{\n"+
 "  if (editjob.pathaddon.value == \"\" && editjob.pathtypein.value == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.SelectAFolderOrTypeInAPathFirst") + "\");\n"+
 "    editjob.pathaddon.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  if (editjob.pathaddon.value != \"\" && editjob.pathtypein.value != \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.EitherSelectAFolderORTypeInAPath") + "\");\n"+
 "    editjob.pathaddon.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  SpecOp(\"pathop\",\"AddToPath\",anchorvalue);\n"+
 "}\n"+
 "\n"+
 "function SpecAddSpec(suffix,anchorvalue)\n"+
 "{\n"+
 "  if (eval(\"editjob.specfile\"+suffix+\".value\") == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.EnterAFileSpecificationFirst") + "\");\n"+
 "    eval(\"editjob.specfile\"+suffix+\".focus()\");\n"+
 "    return;\n"+
 "  }\n"+
 "  SpecOp(\"pathop\"+suffix,\"Add\",anchorvalue);\n"+
 "}\n"+
 "\n"+
 "function SpecInsertSpec(postfix,anchorvalue)\n"+
 "{\n"+
 "  if (eval(\"editjob.specfile_i\"+postfix+\".value\") == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.EnterAFileSpecificationFirst") + "\");\n"+
 "    eval(\"editjob.specfile_i\"+postfix+\".focus()\");\n"+
 "    return;\n"+
 "  }\n"+
 "  SpecOp(\"specop\"+postfix,\"Insert Here\",anchorvalue);\n"+
 "}\n"+
 "\n"+
 "function SpecAddToken(anchorvalue)\n"+
 "{\n"+
 "  if (editjob.spectoken.value == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.NullAccessTokensNotAllowed") + "\");\n"+
 "    editjob.spectoken.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  SpecOp(\"accessop\",\"Add\",anchorvalue);\n"+
 "}\n"+
 "\n"+
 "function SpecAddMapping(anchorvalue)\n"+
 "{\n"+
 "  if (editjob.specmatch.value == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.MatchStringCannotBeEmpty") + "\");\n"+
 "    editjob.specmatch.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  if (!isRegularExpression(editjob.specmatch.value))\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.MatchStringMustBeValidRegularExpression") + "\");\n"+
 "    editjob.specmatch.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  SpecOp(\"specmappingop\",\"Add\",anchorvalue);\n"+
 "}\n"+
 "\n"+
 "function SpecAddFMap(anchorvalue)\n"+
 "{\n"+
 "  if (editjob.specfmapmatch.value == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.MatchStringCannotBeEmpty") + "\");\n"+
 "    editjob.specfmapmatch.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  if (!isRegularExpression(editjob.specfmapmatch.value))\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.MatchStringMustBeValidRegularExpression") + "\");\n"+
 "    editjob.specfmapmatch.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  SpecOp(\"specfmapop\",\"Add\",anchorvalue);\n"+
 "}\n"+
 "\n"+
 "function SpecAddUMap(anchorvalue)\n"+
 "{\n"+
 "  if (editjob.specumapmatch.value == \"\")\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.MatchStringCannotBeEmpty") + "\");\n"+
 "    editjob.specumapmatch.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  if (!isRegularExpression(editjob.specumapmatch.value))\n"+
 "  {\n"+
 "    alert(\"" + Messages.getBodyJavascriptString(locale,"SharedDriveConnector.MatchStringMustBeValidRegularExpression") + "\");\n"+
 "    editjob.specumapmatch.focus();\n"+
 "    return;\n"+
 "  }\n"+
 "  SpecOp(\"specumapop\",\"Add\",anchorvalue);\n"+
 "}\n"+
 "\n"+
 "//-->\n"+
 "</script>\n"
     );
   }
   
   /** Output the specification body section.
   * This method is called in the body section of a job page which has selected a repository connection of the current type.  Its purpose is to present the required form elements for editing.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html>, <body>, and <form> tags.  The name of the
   * form is "editjob".
   *@param out is the output to which any HTML should be sent.
   *@param ds is the current document specification for this job.
   *@param tabName is the current tab name.
   */
   @Override
   public void outputSpecificationBody(IHTTPOutput out, Locale locale, DocumentSpecification ds, String tabName)
     throws ManifoldCFException, IOException
   {
     int i;
     int k;
 
     // "Content Length" tab
     i = 0;
     String maxLength = null;
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_MAXLENGTH))
         maxLength = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE);
     }
     if (maxLength == null)
       maxLength = "";
 
     if (tabName.equals(Messages.getString(locale,"SharedDriveConnector.ContentLength")))
     {
       out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.MaximumDocumentLength") + "</nobr></td>\n"+
 "    <td class=\"value\"><input type=\"text\" name=\"specmaxlength\" size=\"10\" value=\""+maxLength+"\"/></td>\n"+
 "  </tr>\n"+
 "</table>\n"
       );
     }
     else
     {
       out.print(
 "<input type=\"hidden\" name=\"specmaxlength\" value=\""+maxLength+"\"/>\n"
       );
     }
 
     // Check for Paths tab
     if (tabName.equals(Messages.getString(locale,"SharedDriveConnector.Paths")))
     {
       out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
       );
       // Now, loop through paths.  There will be a row in the current table for each one.
       // The row will contain a delete button on the left.  On the right will be the startpoint itself at the top,
       // and underneath it the table where the filter criteria are edited.
       i = 0;
       k = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i++);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_STARTPOINT))
         {
           String pathDescription = "_"+Integer.toString(k);
           String pathOpName = "pathop"+pathDescription;
           String startPath = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_PATH);
           out.print(
 "  <tr>\n"+
 "    <td class=\"value\">\n"+
 "      <a name=\""+"path_"+Integer.toString(k)+"\">\n"+
 "        <input type=\"button\" value=\"Delete\" alt=\""+Messages.getAttributeString(locale,"SharedDriveConnector.DeletePath")+Integer.toString(k)+"\" onClick='Javascript:SpecOp(\""+pathOpName+"\",\"Delete\",\"path_"+Integer.toString(k)+"\")'/>\n"+
 "      </a>&nbsp;\n"+
 "    </td>\n"+
 "    <td class=\"value\">\n"+
 "      <table class=\"displaytable\">\n"+
 "        <tr>\n"+
 "          <td class=\"value\">\n"+
 "            <input type=\"hidden\" name=\""+"specpath"+pathDescription+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_PATH))+"\"/>\n"+
 "            <input type=\"hidden\" name=\""+pathOpName+"\" value=\"\"/>\n"+
 "            <nobr>"+((startPath.length() == 0)?"(root)":org.apache.manifoldcf.ui.util.Encoder.bodyEscape(startPath))+"</nobr>\n"+
 "          </td>\n"+
 "        </tr>\n"+
 "        <tr>\n"+
 "          <td class=\"boxcell\">\n"+
 "            <table class=\"displaytable\">\n"
           );
           // Now go through the include/exclude children of this node, and display one line per node, followed
           // an "add" line.
           int j = 0;
           while (j < sn.getChildCount())
           {
             SpecificationNode excludeNode = sn.getChild(j);
             String instanceDescription = "_"+Integer.toString(k)+"_"+Integer.toString(j);
             String instanceOpName = "specop" + instanceDescription;
 
             String nodeFlavor = excludeNode.getType();
             String nodeType = excludeNode.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TYPE);
             if (nodeType == null)
               nodeType = "";
             String filespec = excludeNode.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_FILESPEC);
             String indexable = excludeNode.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_INDEXABLE);
             if (indexable == null)
               indexable = "";
             out.print(
 "              <tr>\n"+
 "                <td class=\"value\">\n"+
 "                    <input type=\"button\" value=\"Insert\" onClick='Javascript:SpecInsertSpec(\""+instanceDescription+"\",\"filespec_"+Integer.toString(k)+"_"+Integer.toString(j+1)+"\")' alt=\""+Messages.getAttributeString(locale,"SharedDriveConnector.InsertNewMatchForPath")+Integer.toString(k)+" before position #"+Integer.toString(j)+"\"/>\n"+
 "                </td>\n"+
 "                <td class=\"value\">\n"+
 "                  <nobr>\n"+
 "                    <select name=\""+"specfl_i"+instanceDescription+"\">\n"+
 "                      <option value=\"include\">" + Messages.getBodyString(locale,"SharedDriveConnector.Include") + "</option>\n"+
 "                      <option value=\"exclude\">" + Messages.getBodyString(locale,"SharedDriveConnector.Exclude") + "</option>\n"+
 "                    </select>&nbsp;\n"+
 "                    <select name=\""+"spectin_i"+instanceDescription+"\">\n"+
 "                      <option value=\"\" selected=\"selected\">" + Messages.getBodyString(locale,"SharedDriveConnector.AnyFileOrDirectory") + "</option>\n"+
 "                      <option value=\"file\">" + Messages.getBodyString(locale,"SharedDriveConnector.files") + "</option>\n"+
 "                      <option value=\"indexable-file\">" + Messages.getBodyString(locale,"SharedDriveConnector.indexableFiles") + "</option>\n"+
 "                      <option value=\"unindexable-file\">" + Messages.getBodyString(locale,"SharedDriveConnector.unindexableFiles") + "</option>\n"+
 "                      <option value=\"directory\">" + Messages.getBodyString(locale,"SharedDriveConnector.directorys") + "</option>\n"+
 "                    </select>&nbsp;" + Messages.getBodyString(locale,"SharedDriveConnector.matching") + "&nbsp;\n"+
 "                    <input type=\"text\" size=\"20\" name=\""+"specfile_i"+instanceDescription+"\" value=\"\"/>\n"+
 "                  </nobr>\n"+
 "                </td>\n"+
 "\n"+
 "              </tr>\n"+
 "              <tr>\n"+
 "                <td class=\"value\">\n"+
 "                  <a name=\""+"filespec_"+Integer.toString(k)+"_"+Integer.toString(j)+"\">\n"+
 "                    <input type=\"button\" value=\"Delete\" onClick='Javascript:SpecOp(\""+"specop"+instanceDescription+"\",\"Delete\",\"filespec_"+Integer.toString(k)+"_"+Integer.toString(j)+"\")' alt=\""+Messages.getAttributeString(locale,"SharedDriveConnector.DeletePath")+Integer.toString(k)+Messages.getAttributeString(locale,"SharedDriveConnector.matchSpec")+Integer.toString(j)+"\"/>\n"+
 "                  </a>\n"+
 "                </td>\n"+
 "                <td class=\"value\">\n"+
 "                  <nobr>\n"+
 "                    <input type=\"hidden\" name=\""+"specop"+instanceDescription+"\" value=\"\"/>\n"+
 "                    <input type=\"hidden\" name=\""+"specfl"+instanceDescription+"\" value=\""+nodeFlavor+"\"/>\n"+
 "                    <input type=\"hidden\" name=\""+"specty"+instanceDescription+"\" value=\""+nodeType+"\"/>\n"+
 "                    <input type=\"hidden\" name=\""+"specin"+instanceDescription+"\" value=\""+indexable+"\"/>\n"+
 "                    <input type=\"hidden\" name=\""+"specfile"+instanceDescription+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(filespec)+"\"/>\n"+
 "                    "+Integer.toString(j+1)+".&nbsp;"+(nodeFlavor.equals("include")?"Include":"")+""+(nodeFlavor.equals("exclude")?"Exclude":"")+""+(indexable.equals("yes")?"&nbsp;indexable":"")+""+(indexable.equals("no")?"&nbsp;un-indexable":"")+""+(nodeType.equals("file")?"&nbsp;file(s)":"")+""+(nodeType.equals("directory")?"&nbsp;directory(s)":"")+""+(nodeType.equals("")?"&nbsp;file(s)&nbsp;or&nbsp;directory(s)":"")+"&nbsp;matching&nbsp;"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(filespec)+"\n"+
 "                  </nobr>\n"+
 "                </td>\n"+
 "              </tr>\n"
             );
             j++;
           }
           if (j == 0)
           {
             out.print(
 "              <tr><td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoRulesDefined") + "</td></tr>\n"
             );
           }
           out.print(
 "              <tr><td class=\"lightseparator\" colspan=\"2\"><hr/></td></tr>\n"+
 "              <tr>\n"+
 "                <td class=\"value\">\n"+
 "                  <input type=\"hidden\" name=\""+"specchildcount"+pathDescription+"\" value=\""+Integer.toString(j)+"\"/>\n"+
 "                  <a name=\""+"filespec_"+Integer.toString(k)+"_"+Integer.toString(j)+"\">\n"+
 "                    <input type=\"button\" value=\"Add\" onClick='Javascript:SpecAddSpec(\""+pathDescription+"\",\"filespec_"+Integer.toString(k)+"_"+Integer.toString(j+1)+"\")' alt=\""+Messages.getAttributeString(locale,"SharedDriveConnector.AddNewMatchForPath")+Integer.toString(k)+"\"/>\n"+
 "                  </a>\n"+
 "                </td>\n"+
 "                <td class=\"value\">\n"+
 "                  <nobr>\n"+
 "                    <select name=\""+"specfl"+pathDescription+"\">\n"+
 "                      <option value=\"include\">" + Messages.getBodyString(locale,"SharedDriveConnector.Include") + "</option>\n"+
 "                      <option value=\"exclude\">" + Messages.getBodyString(locale,"SharedDriveConnector.Exclude") + "</option>\n"+
 "                    </select>&nbsp;\n"+
 "                    <select name=\""+"spectin"+pathDescription+"\">\n"+
 "                      <option value=\"\">" + Messages.getBodyString(locale,"SharedDriveConnector.AnyFileOrDirectory") + "</option>\n"+
 "                      <option value=\"file\">" + Messages.getBodyString(locale,"SharedDriveConnector.files") + "</option>\n"+
 "                      <option value=\"indexable-file\">" + Messages.getBodyString(locale,"SharedDriveConnector.indexableFiles") + "</option>\n"+
 "                      <option value=\"unindexable-file\">" + Messages.getBodyString(locale,"SharedDriveConnector.unindexableFiles") + "</option>\n"+
 "                      <option value=\"directory\">" + Messages.getBodyString(locale,"SharedDriveConnector.directorys") + "</option>\n"+
 "                    </select>&nbsp;" + Messages.getBodyString(locale,"SharedDriveConnector.matching") + "&nbsp;\n"+
 "                    <input type=\"text\" size=\"20\" name=\""+"specfile"+pathDescription+"\" value=\"\"/>\n"+
 "                  </nobr>\n"+
 "                </td>\n"+
 "              </tr>\n"+
 "            </table>\n"+
 "          </td>\n"+
 "        </tr>\n"+
 "      </table>\n"+
 "    </td>\n"+
 "  </tr>\n"
           );
           k++;
         }
       }
       if (k == 0)
       {
         out.print(
 "  <tr>\n"+
 "    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoStartingPointsDefined") + "</td>\n"+
 "  </tr>\n"
         );
       }
       out.print(
 "  <tr><td class=\"lightseparator\" colspan=\"2\"><hr/></td></tr>\n"+
 "  <tr>\n"+
 "    <td class=\"value\" colspan=\"2\">\n"+
 "      <nobr>\n"+
 "        <input type=\"hidden\" name=\"pathcount\" value=\""+Integer.toString(k)+"\"/>\n"+
 "        <a name=\""+"path_"+Integer.toString(k)+"\">\n"
       );
 	
       String pathSoFar = (String)currentContext.get("specpath");
       if (pathSoFar == null)
         pathSoFar = "";
 
       // Grab next folder/project list
       try
       {
         String[] childList;
         childList = getChildFolderNames(pathSoFar);
         if (childList == null)
         {
           // Illegal path - set it back
           pathSoFar = "";
           childList = getChildFolderNames("");
           if (childList == null)
             throw new ManifoldCFException("Can't find any children for root folder");
         }
         out.print(
 "          <input type=\"hidden\" name=\"specpath\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pathSoFar)+"\"/>\n"+
 "          <input type=\"hidden\" name=\"pathop\" value=\"\"/>\n"+
 "          <input type=\"button\" value=\"Add\" alt=\"" + Messages.getAttributeString(locale,"SharedDriveConnector.AddPath") + "\" onClick='Javascript:SpecOp(\"pathop\",\"Add\",\"path_"+Integer.toString(k+1)+"\")'/>\n"+
 "          &nbsp;"+((pathSoFar.length()==0)?"(root)":org.apache.manifoldcf.ui.util.Encoder.bodyEscape(pathSoFar))+"\n"
         );
         if (pathSoFar.length() > 0)
         {
           out.print(
 "          <input type=\"button\" value=\"-\" alt=\"" + Messages.getAttributeString(locale,"SharedDriveConnector.RemoveFromPath") + "\" onClick='Javascript:SpecOp(\"pathop\",\"Up\",\"path_"+Integer.toString(k)+"\")'/>\n"
           );
         }
         if (childList.length > 0)
         {
           out.print(
 "          <nobr>\n"+
 "            <input type=\"button\" value=\"+\" alt=\"" + Messages.getAttributeString(locale,"SharedDriveConnector.AddPath") + "\" onClick='Javascript:SpecAddToPath(\"path_"+Integer.toString(k)+"\")'/>&nbsp;\n"+
 "            <select multiple=\"false\" name=\"pathaddon\" size=\"4\">\n"+
 "              <option value=\"\" selected=\"selected\">" + Messages.getBodyString(locale,"SharedDriveConnector.PickAFolder") + "</option>\n"
           );
           int j = 0;
           while (j < childList.length)
           {
             String folder = org.apache.manifoldcf.ui.util.Encoder.attributeEscape(childList[j]);
             out.print(
 "              <option value=\""+folder+"\">"+folder+"</option>\n"
             );
             j++;
           }
           out.print(
 "            </select>" + Messages.getBodyString(locale,"SharedDriveConnector.orTypeAPath") +
 "            <input type=\"text\" name=\"pathtypein\" size=\"16\" value=\"\"/>\n"+
 "          </nobr>\n"
           );
         }
       }
       catch (ManifoldCFException e)
       {
         e.printStackTrace();
         out.println(org.apache.manifoldcf.ui.util.Encoder.bodyEscape(e.getMessage()));
       }
       out.print(
 "        </a>\n"+
 "      </nobr>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "</table>\n"
       );
     }
     else
     {
       // Generate hiddens for the pathspec tab
       i = 0;
       k = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i++);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_STARTPOINT))
         {
           String pathDescription = "_"+Integer.toString(k);
           String startPath = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_PATH);
           out.print(
 "<input type=\"hidden\" name=\""+"specpath"+pathDescription+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(startPath)+"\"/>\n"
           );
           // Now go through the include/exclude children of this node.
           int j = 0;
           while (j < sn.getChildCount())
           {
             SpecificationNode excludeNode = sn.getChild(j);
             String instanceDescription = "_"+Integer.toString(k)+"_"+Integer.toString(j);
 
             String nodeFlavor = excludeNode.getType();
             String nodeType = excludeNode.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TYPE);
             if (nodeType == null)
               nodeType = "";
             String filespec = excludeNode.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_FILESPEC);
             String indexable = excludeNode.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_INDEXABLE);
             if (indexable == null)
               indexable = "";
             out.print(
 "<input type=\"hidden\" name=\""+"specfl"+instanceDescription+"\" value=\""+nodeFlavor+"\"/>\n"+
 "<input type=\"hidden\" name=\""+"specty"+instanceDescription+"\" value=\""+nodeType+"\"/>\n"+
 "<input type=\"hidden\" name=\""+"specin"+instanceDescription+"\" value=\""+indexable+"\"/>\n"+
 "<input type=\"hidden\" name=\""+"specfile"+instanceDescription+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(filespec)+"\"/>\n"
             );
             j++;
           }
           k++;
           out.print(
 "<input type=\"hidden\" name=\""+"specchildcount"+pathDescription+"\" value=\""+Integer.toString(j)+"\"/>\n"
           );
         }
       }
       out.print(
 "<input type=\"hidden\" name=\"pathcount\" value=\""+Integer.toString(k)+"\"/>\n"
       );
     }
 
 
     // Security tab
 
     // Find whether security is on or off
     i = 0;
     boolean securityOn = true;
     boolean shareSecurityOn = true;
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_SECURITY))
       {
         String securityValue = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE);
         if (securityValue.equals("off"))
           securityOn = false;
         else if (securityValue.equals("on"))
           securityOn = true;
       }
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_SHARESECURITY))
       {
         String securityValue = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE);
         if (securityValue.equals("off"))
           shareSecurityOn = false;
         else if (securityValue.equals("on"))
           shareSecurityOn = true;
       }
     }
 
     if (tabName.equals(Messages.getString(locale,"SharedDriveConnector.Security")))
     {
       out.print(
 "<table class=\"displaytable\">\n"+
 "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.FileSecurity") + "</nobr></td>\n"+
 "    <td colspan=\"3\" class=\"value\">\n"+
 "      <nobr>\n"+
 "        <input type=\"radio\" name=\"specsecurity\" value=\"on\" "+(securityOn?"checked=\"true\"":"")+" />" + Messages.getBodyString(locale,"SharedDriveConnector.Enabled") + "&nbsp;\n"+
 "        <input type=\"radio\" name=\"specsecurity\" value=\"off\" "+((securityOn==false)?"checked=\"true\"":"")+" />" + Messages.getBodyString(locale,"SharedDriveConnector.Disabled") + "\n"+
 "      </nobr>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n"+
 "\n"
       );
       // Finally, go through forced ACL
       i = 0;
       k = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i++);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_ACCESS))
         {
           String accessDescription = "_"+Integer.toString(k);
           String accessOpName = "accessop"+accessDescription;
           String token = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TOKEN);
           out.print(
 "  <tr>\n"+
 "    <td class=\"description\" colspan=\"1\">\n"+
 "      <input type=\"hidden\" name=\""+accessOpName+"\" value=\"\"/>\n"+
 "      <input type=\"hidden\" name=\""+"spectoken"+accessDescription+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(token)+"\"/>\n"+
 "      <a name=\""+"token_"+Integer.toString(k)+"\">\n"+
 "        <input type=\"button\" value=\"Delete\" alt=\""+Messages.getAttributeString(locale,"SharedDriveConnector.DeleteToken")+Integer.toString(k)+"\" onClick='Javascript:SpecOp(\""+accessOpName+"\",\"Delete\",\"token_"+Integer.toString(k)+"\")'/>\n"+
 "      </a>\n"+
 "    </td>\n"+
 "    <td class=\"value\" colspan=\"3\">\n"+
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(token)+"</nobr>\n"+
 "    </td>\n"+
 "  </tr>\n"
           );
           k++;
         }
       }
       if (k == 0)
       {
         out.print(
 "  <tr>\n"+
 "    <td class=\"message\" colspan=\"4\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoFileAccessTokensPresent") + "</td>\n"+
 "  </tr>\n"
         );
       }
       out.print(
 "  <tr><td class=\"lightseparator\" colspan=\"4\"><hr/></td></tr>\n"+
 "  <tr>\n"+
 "    <td class=\"description\" colspan=\"1\">\n"+
 "      <input type=\"hidden\" name=\"tokencount\" value=\""+Integer.toString(k)+"\"/>\n"+
 "      <input type=\"hidden\" name=\"accessop\" value=\"\"/>\n"+
 "      <a name=\""+"token_"+Integer.toString(k)+"\">\n"+
 "        <input type=\"button\" value=\"Add\" alt=\"" + Messages.getAttributeString(locale,"SharedDriveConnector.AddToken") + "\" onClick='Javascript:SpecAddToken(\"token_"+Integer.toString(k+1)+"\")'/>\n"+
 "      </a>\n"+
 "    </td>\n"+
 "    <td class=\"value\" colspan=\"3\">\n"+
 "      <nobr><input type=\"text\" size=\"30\" name=\"spectoken\" value=\"\"/></nobr>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.ShareSecurity") + "</nobr></td>\n"+
 "    <td colspan=\"3\" class=\"value\">\n"+
 "      <nobr>\n"+
 "        <input type=\"radio\" name=\"specsharesecurity\" value=\"on\" "+(shareSecurityOn?"checked=\"true\"":"")+" />Enabled&nbsp;\n"+
 "        <input type=\"radio\" name=\"specsharesecurity\" value=\"off\" "+((shareSecurityOn==false)?"checked=\"true\"":"")+" />Disabled\n"+
 "      </nobr>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "\n"+
 "</table>\n"
       );
     }
     else
     {
       out.print(
 "<input type=\"hidden\" name=\"specsecurity\" value=\""+(securityOn?"on":"off")+"\"/>\n"
       );
       // Finally, go through forced ACL
       i = 0;
       k = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i++);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_ACCESS))
         {
           String accessDescription = "_"+Integer.toString(k);
           String token = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TOKEN);
           out.print(
 "<input type=\"hidden\" name=\""+"spectoken"+accessDescription+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(token)+"\"/>\n"
           );
           k++;
         }
       }
       out.print(
 "<input type=\"hidden\" name=\"tokencount\" value=\""+Integer.toString(k)+"\"/>\n"+
 "<input type=\"hidden\" name=\"specsharesecurity\" value=\""+(shareSecurityOn?"on":"off")+"\"/>\n"
       );
     }
 
 
 
     // Metadata tab
 
     // Find the path-value metadata attribute name
     // Find the path-value mapping data
     i = 0;
     String pathNameAttribute = "";
     org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap matchMap = new org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap();
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHNAMEATTRIBUTE))
       {
         pathNameAttribute = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE);
       }
       else if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHMAP))
       {
         String pathMatch = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH);
         String pathReplace = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE);
         matchMap.appendMatchPair(pathMatch,pathReplace);
       }
     }
 
     if (tabName.equals(Messages.getString(locale,"SharedDriveConnector.Metadata")))
     {
       out.print(
 "<input type=\"hidden\" name=\"specmappingcount\" value=\""+Integer.toString(matchMap.getMatchCount())+"\"/>\n"+
 "<input type=\"hidden\" name=\"specmappingop\" value=\"\"/>\n"+
 "<table class=\"displaytable\">\n"+
 "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"+
 "    <td class=\"description\" colspan=\"1\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.PathAttributeName") + "</nobr></td>\n"+
 "    <td class=\"value\" colspan=\"3\">\n"+
 "      <input type=\"text\" name=\"specpathnameattribute\" size=\"20\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pathNameAttribute)+"\"/>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n"+
 "\n"
       );
       i = 0;
       while (i < matchMap.getMatchCount())
       {
         String matchString = matchMap.getMatchString(i);
         String replaceString = matchMap.getReplaceString(i);
         out.print(
 "  <tr>\n"+
 "    <td class=\"value\">\n"+
 "      <input type=\"hidden\" name=\""+"specmappingop_"+Integer.toString(i)+"\" value=\"\"/>\n"+
 "      <a name=\""+"mapping_"+Integer.toString(i)+"\">\n"+
 "        <input type=\"button\" onClick='Javascript:SpecOp(\"specmappingop_"+Integer.toString(i)+"\",\"Delete\",\"mapping_"+Integer.toString(i)+"\")' alt=\""+Messages.getAttributeString(locale,"SharedDriveConnector.DeleteMapping")+Integer.toString(i)+"\" value=\"Delete\"/>\n"+
 "      </a>\n"+
 "    </td>\n"+
 "    <td class=\"value\"><input type=\"hidden\" name=\""+"specmatch_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString)+"\"/>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString)+"</td>\n"+
 "    <td class=\"value\">==></td>\n"+
 "    <td class=\"value\"><input type=\"hidden\" name=\""+"specreplace_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString)+"\"/>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString)+"</td>\n"+
 "  </tr>\n"
         );
         i++;
       }
       if (i == 0)
       {
         out.print(
 "  <tr><td colspan=\"4\" class=\"message\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoMappingsSpecified") + "</td></tr>\n"
         );
       }
       out.print(
 "  <tr><td class=\"lightseparator\" colspan=\"4\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"+
 "    <td class=\"value\">\n"+
 "      <a name=\""+"mapping_"+Integer.toString(i)+"\">\n"+
 "        <input type=\"button\" onClick='Javascript:SpecAddMapping(\"mapping_"+Integer.toString(i+1)+"\")' alt=\"" + Messages.getAttributeString(locale,"SharedDriveConnector.AddToMappings") + "\" value=\"Add\"/>\n"+
 "      </a>\n"+
 "    </td>\n"+
 "    <td class=\"value\">" + Messages.getBodyString(locale,"SharedDriveConnector.MatchRegexp") + "<input type=\"text\" name=\"specmatch\" size=\"32\" value=\"\"/></td>\n"+
 "    <td class=\"value\">==></td>\n"+
 "    <td class=\"value\">" + Messages.getBodyString(locale,"SharedDriveConnector.ReplaceString") + "<input type=\"text\" name=\"specreplace\" size=\"32\" value=\"\"/></td>\n"+
 "  </tr>\n"+
 "</table>\n"
       );
     }
     else
     {
       out.print(
 "<input type=\"hidden\" name=\"specpathnameattribute\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(pathNameAttribute)+"\"/>\n"+
 "<input type=\"hidden\" name=\"specmappingcount\" value=\""+Integer.toString(matchMap.getMatchCount())+"\"/>\n"
       );
       i = 0;
       while (i < matchMap.getMatchCount())
       {
         String matchString = matchMap.getMatchString(i);
         String replaceString = matchMap.getReplaceString(i);
         out.print(
 "<input type=\"hidden\" name=\""+"specmatch_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString)+"\"/>\n"+
 "<input type=\"hidden\" name=\""+"specreplace_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString)+"\"/>\n"
         );
         i++;
       }
     }
 	
     // File and URL Mapping tabs
 	
     // Find the filename mapping data
     // Find the URL mapping data
     org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap fileMap = new org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap();
     org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap uriMap = new org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap();
     i = 0;
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_FILEMAP))
       {
         String pathMatch = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH);
         String pathReplace = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE);
         fileMap.appendMatchPair(pathMatch,pathReplace);
       }
       else if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_URIMAP))
       {
         String pathMatch = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH);
         String pathReplace = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE);
         uriMap.appendMatchPair(pathMatch,pathReplace);
       }
     }
 
     if (tabName.equals(Messages.getString(locale,"SharedDriveConnector.FileMapping")))
     {
       out.print(
 "<input type=\"hidden\" name=\"specfmapcount\" value=\""+Integer.toString(fileMap.getMatchCount())+"\"/>\n"+
 "<input type=\"hidden\" name=\"specfmapop\" value=\"\"/>\n"+
 "<table class=\"displaytable\">\n"+
 "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n"
       );
       i = 0;
       while (i < fileMap.getMatchCount())
       {
         String matchString = fileMap.getMatchString(i);
         String replaceString = fileMap.getReplaceString(i);
         out.print(
 "  <tr>\n"+
 "    <td class=\"value\">\n"+
 "      <input type=\"hidden\" name=\""+"specfmapop_"+Integer.toString(i)+"\" value=\"\"/>\n"+
 "      <a name=\""+"fmap_"+Integer.toString(i)+"\">\n"+
 "        <input type=\"button\" onClick='Javascript:SpecOp(\"specfmapop_"+Integer.toString(i)+"\",\"Delete\",\"fmap_"+Integer.toString(i)+"\")' alt=\""+Messages.getAttributeString(locale,"SharedDriveConnector.DeleteFileMapping")+Integer.toString(i)+"\" value=\"Delete\"/>\n"+
 "      </a>\n"+
 "    </td>\n"+
 "    <td class=\"value\"><input type=\"hidden\" name=\""+"specfmapmatch_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString)+"\"/>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString)+"</td>\n"+
 "    <td class=\"value\">==></td>\n"+
 "    <td class=\"value\"><input type=\"hidden\" name=\""+"specfmapreplace_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString)+"\"/>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString)+"</td>\n"+
 "  </tr>\n"
         );
         i++;
       }
       if (i == 0)
       {
         out.print(
 "  <tr><td colspan=\"4\" class=\"message\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoFileMappingsSpecified") + "</td></tr>\n"
         );
       }
       out.print(
 "  <tr><td class=\"lightseparator\" colspan=\"4\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"+
 "    <td class=\"value\">\n"+
 "      <a name=\""+"fmap_"+Integer.toString(i)+"\">\n"+
 "        <input type=\"button\" onClick='Javascript:SpecAddFMap(\"fmap_"+Integer.toString(i+1)+"\")' alt=\"" + Messages.getAttributeString(locale,"SharedDriveConnector.AddToFileMappings") + "\" value=\"Add\"/>\n"+
 "      </a>\n"+
 "    </td>\n"+
 "    <td class=\"value\">" + Messages.getBodyString(locale,"SharedDriveConnector.MatchRegexp") + "<input type=\"text\" name=\"specfmapmatch\" size=\"32\" value=\"\"/></td>\n"+
 "    <td class=\"value\">==></td>\n"+
 "    <td class=\"value\">" + Messages.getBodyString(locale,"SharedDriveConnector.ReplaceString") + "<input type=\"text\" name=\"specfmapreplace\" size=\"32\" value=\"\"/></td>\n"+
 "  </tr>\n"+
 "</table>\n"
       );
     }
     else
     {
       out.print(
 "<input type=\"hidden\" name=\"specfmapcount\" value=\""+Integer.toString(fileMap.getMatchCount())+"\"/>\n"
       );
       i = 0;
       while (i < fileMap.getMatchCount())
       {
         String matchString = fileMap.getMatchString(i);
         String replaceString = fileMap.getReplaceString(i);
         out.print(
 "<input type=\"hidden\" name=\""+"specfmapmatch_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString)+"\"/>\n"+
 "<input type=\"hidden\" name=\""+"specfmapreplace_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString)+"\"/>\n"
         );
         i++;
       }
     }
 	
     if (tabName.equals(Messages.getString(locale,"SharedDriveConnector.URLMapping")))
     {
       out.print(
 "<input type=\"hidden\" name=\"specumapcount\" value=\""+Integer.toString(uriMap.getMatchCount())+"\"/>\n"+
 "<input type=\"hidden\" name=\"specumapop\" value=\"\"/>\n"+
 "<table class=\"displaytable\">\n"+
 "  <tr><td class=\"separator\" colspan=\"4\"><hr/></td></tr>\n"
       );
       i = 0;
       while (i < uriMap.getMatchCount())
       {
         String matchString = uriMap.getMatchString(i);
         String replaceString = uriMap.getReplaceString(i);
         out.print(
 "  <tr>\n"+
 "    <td class=\"value\">\n"+
 "      <input type=\"hidden\" name=\""+"specumapop_"+Integer.toString(i)+"\" value=\"\"/>\n"+
 "      <a name=\""+"umap_"+Integer.toString(i)+"\">\n"+
 "        <input type=\"button\" onClick='Javascript:SpecOp(\"specumapop_"+Integer.toString(i)+"\",\"Delete\",\"umap_"+Integer.toString(i)+"\")' alt=\""+Messages.getAttributeString(locale,"SharedDriveConnector.DeleteUrlMapping")+Integer.toString(i)+"\" value=\"Delete\"/>\n"+
 "      </a>\n"+
 "    </td>\n"+
 "    <td class=\"value\">\n"+
 "      <input type=\"hidden\" name=\""+"specumapmatch_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString)+"\"/>\n"+
 "      "+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString)+"\n"+
 "    </td>\n"+
 "    <td class=\"value\">==></td>\n"+
 "    <td class=\"value\">\n"+
 "      <input type=\"hidden\" name=\""+"specumapreplace_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString)+"\"/>\n"+
 "      "+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString)+"\n"+
 "    </td>\n"+
 "  </tr>\n"
         );
         i++;
       }
       if (i == 0)
       {
         out.print(
 "  <tr><td colspan=\"4\" class=\"message\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoURLMappingsSpecifiedWillProduceAFileIRI") + "</td></tr>\n"
         );
       }
       out.print(
 "  <tr><td class=\"lightseparator\" colspan=\"4\"><hr/></td></tr>\n"+
 "      \n"+
 "  <tr>\n"+
 "    <td class=\"value\">\n"+
 "      <a name=\""+"umap_"+Integer.toString(i)+"\">\n"+
 "        <input type=\"button\" onClick='Javascript:SpecAddUMap(\"umap_"+Integer.toString(i+1)+"\")' alt=\"" + Messages.getAttributeString(locale,"SharedDriveConnector.AddToURLMappings") + "\" value=\"Add\"/>\n"+
 "      </a>\n"+
 "    </td>\n"+
 "    <td class=\"value\">" + Messages.getBodyString(locale,"SharedDriveConnector.MatchRegexp") + "<input type=\"text\" name=\"specumapmatch\" size=\"32\" value=\"\"/></td>\n"+
 "    <td class=\"value\">==></td>\n"+
 "    <td class=\"value\">" + Messages.getBodyString(locale,"SharedDriveConnector.ReplaceString") + "<input type=\"text\" name=\"specumapreplace\" size=\"32\" value=\"\"/></td>\n"+
 "  </tr>\n"+
 "</table>\n"
       );
     }
     else
     {
       out.print(
 "<input type=\"hidden\" name=\"specumapcount\" value=\""+Integer.toString(uriMap.getMatchCount())+"\"/>\n"
       );
       i = 0;
       while (i < uriMap.getMatchCount())
       {
         String matchString = uriMap.getMatchString(i);
         String replaceString = uriMap.getReplaceString(i);
         out.print(
 "<input type=\"hidden\" name=\""+"specumapmatch_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(matchString)+"\"/>\n"+
 "<input type=\"hidden\" name=\""+"specumapreplace_"+Integer.toString(i)+"\" value=\""+org.apache.manifoldcf.ui.util.Encoder.attributeEscape(replaceString)+"\"/>\n"
         );
         i++;
       }
     }
   }
   
   /** Process a specification post.
   * This method is called at the start of job's edit or view page, whenever there is a possibility that form data for a connection has been
   * posted.  Its purpose is to gather form information and modify the document specification accordingly.
   * The name of the posted form is "editjob".
   *@param variableContext contains the post data, including binary file-upload information.
   *@param ds is the current document specification for this job.
   *@return null if all is well, or a string error message if there is an error that should prevent saving of the job (and cause a redirection to an error page).
   */
   @Override
   public String processSpecificationPost(IPostParameters variableContext, Locale locale, DocumentSpecification ds)
     throws ManifoldCFException
   {
     String x = variableContext.getParameter("pathcount");
     if (x != null)
     {
       // Delete all path specs first
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_STARTPOINT))
           ds.removeChild(i);
         else
           i++;
       }
 
       // Find out how many children were sent
       int pathCount = Integer.parseInt(x);
       // Gather up these
       i = 0;
       while (i < pathCount)
       {
         String pathDescription = "_"+Integer.toString(i);
         String pathOpName = "pathop"+pathDescription;
         x = variableContext.getParameter(pathOpName);
         if (x != null && x.equals("Delete"))
         {
           // Skip to the next
           i++;
           continue;
         }
         // Path inserts won't happen until the very end
         String path = variableContext.getParameter("specpath"+pathDescription);
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_STARTPOINT);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_PATH,path);
 
         // Now, get the number of children
         String y = variableContext.getParameter("specchildcount"+pathDescription);
         int childCount = Integer.parseInt(y);
         int j = 0;
         int w = 0;
         while (j < childCount)
         {
           String instanceDescription = "_"+Integer.toString(i)+"_"+Integer.toString(j);
           // Look for an insert or a delete at this point
           String instanceOp = "specop"+instanceDescription;
           String z = variableContext.getParameter(instanceOp);
           String flavor;
           String type;
           String indexable;
           String match;
           SpecificationNode sn;
           if (z != null && z.equals("Delete"))
           {
             // Process the deletion as we gather
             j++;
             continue;
           }
           if (z != null && z.equals("Insert Here"))
           {
             // Process the insertion as we gather.
             flavor = variableContext.getParameter("specfl_i"+instanceDescription);
             indexable = "";
             type = "";
             String xxx = variableContext.getParameter("spectin_i"+instanceDescription);
             if (xxx.equals("file") || xxx.equals("directory"))
               type = xxx;
             else if (xxx.equals("indexable-file"))
             {
               indexable = "yes";
               type = "file";
             }
             else if (xxx.equals("unindexable-file"))
             {
               indexable = "no";
               type = "file";
             }
 
             match = variableContext.getParameter("specfile_i"+instanceDescription);
             sn = new SpecificationNode(flavor);
             if (type != null && type.length() > 0)
               sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TYPE,type);
             if (indexable != null && indexable.length() > 0)
               sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_INDEXABLE,indexable);
             sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_FILESPEC,match);
             node.addChild(w++,sn);
           }
           flavor = variableContext.getParameter("specfl"+instanceDescription);
           type = variableContext.getParameter("specty"+instanceDescription);
           match = variableContext.getParameter("specfile"+instanceDescription);
           indexable = variableContext.getParameter("specin"+instanceDescription);
           sn = new SpecificationNode(flavor);
           if (type != null && type.length() > 0)
             sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TYPE,type);
           if (indexable != null && indexable.length() > 0)
             sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_INDEXABLE,indexable);
           sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_FILESPEC,match);
           node.addChild(w++,sn);
           j++;
         }
         if (x != null && x.equals("Add"))
         {
           // Process adds to the end of the rules in-line
           String match = variableContext.getParameter("specfile"+pathDescription);
           String indexable = "";
           String type = "";
           String xxx = variableContext.getParameter("spectin"+pathDescription);
           if (xxx.equals("file") || xxx.equals("directory"))
             type = xxx;
           else if (xxx.equals("indexable-file"))
           {
             indexable = "yes";
             type = "file";
           }
           else if (xxx.equals("unindexable-file"))
           {
             indexable = "no";
             type = "file";
           }
 
           String flavor = variableContext.getParameter("specfl"+pathDescription);
           SpecificationNode sn = new SpecificationNode(flavor);
           if (type != null && type.length() > 0)
             sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TYPE,type);
           if (indexable != null && indexable.length() > 0)
             sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_INDEXABLE,indexable);
           sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_FILESPEC,match);
           node.addChild(w,sn);
         }
 
         ds.addChild(ds.getChildCount(),node);
         i++;
       }
 
       // See if there's a global add operation
       String op = variableContext.getParameter("pathop");
       if (op != null && op.equals("Add"))
       {
         String path = variableContext.getParameter("specpath");
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_STARTPOINT);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_PATH,path);
         ds.addChild(ds.getChildCount(),node);
 
         // Now add in the defaults; these will be "include all directories" and "include all indexable files".
         SpecificationNode sn = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_INCLUDE);
         sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TYPE,"file");
         sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_INDEXABLE,"yes");
         sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_FILESPEC,"*");
         node.addChild(node.getChildCount(),sn);
         sn = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_INCLUDE);
         sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TYPE,"directory");
         sn.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_FILESPEC,"*");
         node.addChild(node.getChildCount(),sn);
       }
       else if (op != null && op.equals("Up"))
       {
         // Strip off end
         String path = variableContext.getParameter("specpath");
         int k = path.lastIndexOf("/");
         if (k == -1)
           path = "";
         else
           path = path.substring(0,k);
         currentContext.save("specpath",path);
       }
       else if (op != null && op.equals("AddToPath"))
       {
         String path = variableContext.getParameter("specpath");
         String addon = variableContext.getParameter("pathaddon");
         String typein = variableContext.getParameter("pathtypein");
         if (addon != null && addon.length() > 0)
         {
           if (path.length() == 0)
             path = addon;
           else
             path += "/" + addon;
         }
         else if (typein != null && typein.length() > 0)
         {
           String trialPath = path;
           if (trialPath.length() == 0)
             trialPath = typein;
           else
             trialPath += "/" + typein;
           // Validate trial path
           try
           {
             trialPath = validateFolderName(trialPath);
             if (trialPath != null)
               path = trialPath;
           }
           catch (ManifoldCFException e)
           {
             // Effectively, this just means we can't add a typein to the path right now.
           }
         }
         currentContext.save("specpath",path);
       }
     }
 
     x = variableContext.getParameter("specmaxlength");
     if (x != null)
     {
       // Delete max length entry
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_MAXLENGTH))
           ds.removeChild(i);
         else
           i++;
       }
       if (x.length() > 0)
       {
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_MAXLENGTH);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE,x);
         ds.addChild(ds.getChildCount(),node);
       }
     }
 
     x = variableContext.getParameter("specsecurity");
     if (x != null)
     {
       // Delete all security entries first
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_SECURITY))
           ds.removeChild(i);
         else
           i++;
       }
 
       SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_SECURITY);
       node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE,x);
       ds.addChild(ds.getChildCount(),node);
 
     }
 
     x = variableContext.getParameter("tokencount");
     if (x != null)
     {
       // Delete all file specs first
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_ACCESS))
           ds.removeChild(i);
         else
           i++;
       }
 
       int accessCount = Integer.parseInt(x);
       i = 0;
       while (i < accessCount)
       {
         String accessDescription = "_"+Integer.toString(i);
         String accessOpName = "accessop"+accessDescription;
         x = variableContext.getParameter(accessOpName);
         if (x != null && x.equals("Delete"))
         {
           // Next row
           i++;
           continue;
         }
         // Get the stuff we need
         String accessSpec = variableContext.getParameter("spectoken"+accessDescription);
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_ACCESS);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TOKEN,accessSpec);
         ds.addChild(ds.getChildCount(),node);
         i++;
       }
 
       String op = variableContext.getParameter("accessop");
       if (op != null && op.equals("Add"))
       {
         String accessspec = variableContext.getParameter("spectoken");
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_ACCESS);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TOKEN,accessspec);
         ds.addChild(ds.getChildCount(),node);
       }
     }
 
     x = variableContext.getParameter("specsharesecurity");
     if (x != null)
     {
       // Delete all security entries first
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_SHARESECURITY))
           ds.removeChild(i);
         else
           i++;
       }
 
       SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_SHARESECURITY);
       node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE,x);
       ds.addChild(ds.getChildCount(),node);
 
     }
 
     String xc = variableContext.getParameter("specpathnameattribute");
     if (xc != null)
     {
       // Delete old one
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHNAMEATTRIBUTE))
           ds.removeChild(i);
         else
           i++;
       }
       if (xc.length() > 0)
       {
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHNAMEATTRIBUTE);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE,xc);
         ds.addChild(ds.getChildCount(),node);
       }
     }
 
     xc = variableContext.getParameter("specmappingcount");
     if (xc != null)
     {
       // Delete old spec
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHMAP))
           ds.removeChild(i);
         else
           i++;
       }
 
       // Now, go through the data and assemble a new list.
       int mappingCount = Integer.parseInt(xc);
 
       // Gather up these
       i = 0;
       while (i < mappingCount)
       {
         String pathDescription = "_"+Integer.toString(i);
         String pathOpName = "specmappingop"+pathDescription;
         xc = variableContext.getParameter(pathOpName);
         if (xc != null && xc.equals("Delete"))
         {
           // Skip to the next
           i++;
           continue;
         }
         // Inserts won't happen until the very end
         String match = variableContext.getParameter("specmatch"+pathDescription);
         String replace = variableContext.getParameter("specreplace"+pathDescription);
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHMAP);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH,match);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE,replace);
         ds.addChild(ds.getChildCount(),node);
         i++;
       }
 
       // Check for add
       xc = variableContext.getParameter("specmappingop");
       if (xc != null && xc.equals("Add"))
       {
         String match = variableContext.getParameter("specmatch");
         String replace = variableContext.getParameter("specreplace");
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHMAP);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH,match);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE,replace);
         ds.addChild(ds.getChildCount(),node);
       }
     }
 	
     xc = variableContext.getParameter("specfmapcount");
     if (xc != null)
     {
       // Delete old spec
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_FILEMAP))
           ds.removeChild(i);
         else
           i++;
       }
 
       // Now, go through the data and assemble a new list.
       int mappingCount = Integer.parseInt(xc);
 
       // Gather up these
       i = 0;
       while (i < mappingCount)
       {
         String pathDescription = "_"+Integer.toString(i);
         String pathOpName = "specfmapop"+pathDescription;
         xc = variableContext.getParameter(pathOpName);
         if (xc != null && xc.equals("Delete"))
         {
           // Skip to the next
           i++;
           continue;
         }
         // Inserts won't happen until the very end
         String match = variableContext.getParameter("specfmapmatch"+pathDescription);
         String replace = variableContext.getParameter("specfmapreplace"+pathDescription);
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_FILEMAP);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH,match);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE,replace);
         ds.addChild(ds.getChildCount(),node);
         i++;
       }
 
       // Check for add
       xc = variableContext.getParameter("specfmapop");
       if (xc != null && xc.equals("Add"))
       {
         String match = variableContext.getParameter("specfmapmatch");
         String replace = variableContext.getParameter("specfmapreplace");
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_FILEMAP);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH,match);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE,replace);
         ds.addChild(ds.getChildCount(),node);
       }
     }
 
     xc = variableContext.getParameter("specumapcount");
     if (xc != null)
     {
       // Delete old spec
       int i = 0;
       while (i < ds.getChildCount())
       {
         SpecificationNode sn = ds.getChild(i);
         if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_URIMAP))
           ds.removeChild(i);
         else
           i++;
       }
 
       // Now, go through the data and assemble a new list.
       int mappingCount = Integer.parseInt(xc);
 
       // Gather up these
       i = 0;
       while (i < mappingCount)
       {
         String pathDescription = "_"+Integer.toString(i);
         String pathOpName = "specumapop"+pathDescription;
         xc = variableContext.getParameter(pathOpName);
         if (xc != null && xc.equals("Delete"))
         {
           // Skip to the next
           i++;
           continue;
         }
         // Inserts won't happen until the very end
         String match = variableContext.getParameter("specumapmatch"+pathDescription);
         String replace = variableContext.getParameter("specumapreplace"+pathDescription);
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_URIMAP);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH,match);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE,replace);
         ds.addChild(ds.getChildCount(),node);
         i++;
       }
 
       // Check for add
       xc = variableContext.getParameter("specumapop");
       if (xc != null && xc.equals("Add"))
       {
         String match = variableContext.getParameter("specumapmatch");
         String replace = variableContext.getParameter("specumapreplace");
         SpecificationNode node = new SpecificationNode(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_URIMAP);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH,match);
         node.setAttribute(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE,replace);
         ds.addChild(ds.getChildCount(),node);
       }
     }
     return null;
   }
   
   /** View specification.
   * This method is called in the body section of a job's view page.  Its purpose is to present the document specification information to the user.
   * The coder can presume that the HTML that is output from this configuration will be within appropriate <html> and <body> tags.
   *@param out is the output to which any HTML should be sent.
   *@param ds is the current document specification for this job.
   */
   @Override
   public void viewSpecification(IHTTPOutput out, Locale locale, DocumentSpecification ds)
     throws ManifoldCFException, IOException
   {
     out.print(
 "<table class=\"displaytable\">\n"
     );
     int i = 0;
     boolean seenAny = false;
     while (i < ds.getChildCount())
     {
       SpecificationNode spn = ds.getChild(i++);
       if (spn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_STARTPOINT))
       {
         if (seenAny == false)
         {
           seenAny = true;
         }
         out.print(
 "  <tr>\n"+
 "    <td class=\"description\">\n"+
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(spn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_PATH))+":"+"</nobr>\n"+
 "    </td>\n"+
 "    <td class=\"value\">\n"
         );
         int j = 0;
         while (j < spn.getChildCount())
         {
           SpecificationNode sn = spn.getChild(j++);
           // This is "include" or "exclude"
           String nodeFlavor = sn.getType();
           // This is the file/directory name match
           String filespec = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_FILESPEC);
           // This has a value of null, "", "file", or "directory".
           String nodeType = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TYPE);
           if (nodeType == null)
             nodeType = "";
           // This has a value of null, "", "yes", or "no".
           String ingestableFlag = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_INDEXABLE);
           if (ingestableFlag == null)
             ingestableFlag = "";
           out.print(
 "      <nobr>\n"+
 "        "+Integer.toString(j)+".\n"+
 "        "+(nodeFlavor.equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_INCLUDE)?"Include":"")+"\n"+
 "        "+(nodeFlavor.equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_EXCLUDE)?"Exclude":"")+"\n"+
 "        "+(ingestableFlag.equals("yes")?"&nbsp;indexable":"")+"\n"+
 "        "+(ingestableFlag.equals("no")?"&nbsp;un-indexable":"")+"\n"+
 "        "+(nodeType.equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.VALUE_FILE)?"&nbsp;file(s)":"")+"\n"+
 "        "+(nodeType.equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.VALUE_DIRECTORY)?"&nbsp;directory(s)":"")+"\n"+
 "        "+(nodeType.equals("")?"&nbsp;file(s)&nbsp;or&nbsp;directory(s)":"")+"&nbsp;matching&nbsp;\n"+
 "        "+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(filespec)+"\n"+
 "      </nobr>\n"+
 "      <br/>\n"
           );
         }
         out.print(
 "    </td>\n"+
 "  </tr>\n"
         );
       }
     }
     if (seenAny == false)
     {
       out.print(
 "  <tr><td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoDocumentsSpecified") + "</td></tr>\n"
       );
     }
     out.print(
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "\n"
     );
     // Find whether security is on or off
     i = 0;
     boolean securityOn = true;
     boolean shareSecurityOn = true;
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_SECURITY))
       {
         String securityValue = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE);
         if (securityValue.equals("off"))
           securityOn = false;
         else if (securityValue.equals("on"))
           securityOn = true;
       }
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_SHARESECURITY))
       {
         String securityValue = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE);
         if (securityValue.equals("off"))
           shareSecurityOn = false;
         else if (securityValue.equals("on"))
           shareSecurityOn = true;
       }
     }
     out.print(
 "\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.FileSecurity") + "</nobr></td>\n"+
 "    <td class=\"value\"><nobr>"+(securityOn?Messages.getBodyString(locale,"SharedDriveConnector.Enabled"):Messages.getBodyString(locale,"SharedDriveConnector.Disabled"))+"</nobr></td>\n"+
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
     );
     // Go through looking for access tokens
     seenAny = false;
     i = 0;
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_ACCESS))
       {
         if (seenAny == false)
         {
           out.print(
 "  <tr><td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.FileAccessTokens") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"
           );
           seenAny = true;
         }
         String token = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_TOKEN);
         out.print(
 "      <nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(token)+"</nobr><br/>\n"
         );
       }
     }
 
     if (seenAny)
     {
       out.print(
 "    </td>\n"+
 "  </tr>\n"
       );
     }
     else
     {
       out.print(
 "  <tr><td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoFileAccessTokensSpecified") + "</td></tr>\n"
       );
     }
     out.print(
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "    \n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.ShareSecurity") + "</nobr></td>\n"+
 "    <td class=\"value\"><nobr>"+(shareSecurityOn?Messages.getBodyString(locale,"SharedDriveConnector.Enabled"):Messages.getBodyString(locale,"SharedDriveConnector.Disabled"))+"</nobr></td>\n"+
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"
     );
     // Find the path-name metadata attribute name
     i = 0;
     String pathNameAttribute = "";
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHNAMEATTRIBUTE))
       {
         pathNameAttribute = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE);
       }
     }
     out.print(
 "  <tr>\n"
     );
     if (pathNameAttribute.length() > 0)
     {
       out.print(
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.PathNameMetadataAttribute") + "</nobr></td>\n"+
 "    <td class=\"value\"><nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(pathNameAttribute)+"</nobr></td>\n"
       );
     }
     else
     {
       out.print(
 "    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoPathNameMetadataAttributeSpecified") + "</td>\n"
       );
     }
     out.print(
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"+
 "\n"
     );
     
     // Find the path-value mapping data
     i = 0;
     org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap matchMap = new org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap();
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_PATHMAP))
       {
         String pathMatch = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH);
         String pathReplace = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE);
         matchMap.appendMatchPair(pathMatch,pathReplace);
       }
     }
     if (matchMap.getMatchCount() > 0)
     {
       out.print(
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.PathValueMapping") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"+
 "      <table class=\"displaytable\">\n"
       );
       i = 0;
       while (i < matchMap.getMatchCount())
       {
         String matchString = matchMap.getMatchString(i);
         String replaceString = matchMap.getReplaceString(i);
         out.print(
 "        <tr>\n"+
 "          <td class=\"value\"><nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString)+"</nobr></td>\n"+
 "          <td class=\"value\">==></td>\n"+
 "          <td class=\"value\"><nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString)+"</nobr></td>\n"+
 "        </tr>\n"
         );
         i++;
       }
       out.print(
 "      </table>\n"+
 "    </td>\n"
       );
     }
     else
     {
       out.print(
 "    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoMappingsSpecified") + "</td>\n"
       );
     }
     out.print(
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"
     );
     // Find the file name mapping data
     i = 0;
     org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap fileMap = new org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap();
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_FILEMAP))
       {
         String pathMatch = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH);
         String pathReplace = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE);
         fileMap.appendMatchPair(pathMatch,pathReplace);
       }
     }
     if (fileMap.getMatchCount() > 0)
     {
       out.print(
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.FileNameMapping") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"+
 "      <table class=\"displaytable\">\n"
       );
       i = 0;
       while (i < fileMap.getMatchCount())
       {
         String matchString = fileMap.getMatchString(i);
         String replaceString = fileMap.getReplaceString(i);
         out.print(
 "        <tr>\n"+
 "          <td class=\"value\"><nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString)+"</nobr></td>\n"+
 "          <td class=\"value\">==></td>\n"+
 "          <td class=\"value\"><nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString)+"</nobr></td>\n"+
 "        </tr>\n"
         );
         i++;
       }
       out.print(
 "      </table>\n"+
 "    </td>\n"
       );
     }
     else
     {
       out.print(
 "    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoFileNameMappingsSpecified") + "</td>\n"
       );
     }
     out.print(
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"
     );
 
     // Find the url mapping data
     i = 0;
     org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap uriMap = new org.apache.manifoldcf.crawler.connectors.sharedrive.MatchMap();
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_URIMAP))
       {
         String pathMatch = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_MATCH);
         String pathReplace = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_REPLACE);
         uriMap.appendMatchPair(pathMatch,pathReplace);
       }
     }
     if (uriMap.getMatchCount() > 0)
     {
       out.print(
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.URLMappingColon") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"+
 "      <table class=\"displaytable\">\n"
       );
       i = 0;
       while (i < uriMap.getMatchCount())
       {
         String matchString = uriMap.getMatchString(i);
         String replaceString = uriMap.getReplaceString(i);
         out.print(
 "        <tr>\n"+
 "          <td class=\"value\"><nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(matchString)+"</nobr></td>\n"+
 "          <td class=\"value\">==></td>\n"+
 "          <td class=\"value\"><nobr>"+org.apache.manifoldcf.ui.util.Encoder.bodyEscape(replaceString)+"</nobr></td>\n"+
 "        </tr>\n"
         );
         i++;
       }
       out.print(
 "      </table>\n"+
 "    </td>\n"
       );
     }
     else
     {
       out.print(
 "    <td class=\"message\" colspan=\"2\">" + Messages.getBodyString(locale,"SharedDriveConnector.NoURLMappingsSpecifiedWillProduceAFileIRI") + "</td>\n"
       );
     }
     out.print(
 "  </tr>\n"+
 "\n"+
 "  <tr><td class=\"separator\" colspan=\"2\"><hr/></td></tr>\n"+
 "\n"+
 "  <tr>\n"+
 "    <td class=\"description\"><nobr>" + Messages.getBodyString(locale,"SharedDriveConnector.MaximumDocumentLength") + "</nobr></td>\n"+
 "    <td class=\"value\">\n"+
 "      <nobr>\n"
     );
     // Find the path-value mapping data
     i = 0;
     String maxLength = null;
     while (i < ds.getChildCount())
     {
       SpecificationNode sn = ds.getChild(i++);
       if (sn.getType().equals(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.NODE_MAXLENGTH))
       {
         maxLength = sn.getAttributeValue(org.apache.manifoldcf.crawler.connectors.sharedrive.SharedDriveConnector.ATTRIBUTE_VALUE);
       }
     }
     if (maxLength == null || maxLength.length() == 0)
       maxLength = "Unlimited";
     out.print(
 "        "+maxLength+"\n"+
 "      </nobr>\n"+
 "    </td>\n"+
 "  </tr>\n"+
 "</table>\n"
     );
   }
 
   /* The following are additional methods used by the UI */
 
   /**
   * given a server uri, return all shares
   *
   * @param serverURI -
   * @return an array of SmbFile
   */
   public SmbFile[] getShareNames(String serverURI)
     throws ManifoldCFException
   {
     getSession();
     SmbFile server = null;
     try
     {
       server = new SmbFile(serverURI,pa);
     }
     catch (MalformedURLException e1)
     {
       throw new ManifoldCFException("MalformedURLException tossed",e1);
     }
     SmbFile[] shares = null;
     try
     {
       // check to make sure it's a server
       if (getFileType(server)==SmbFile.TYPE_SERVER)
       {
         shares = fileListFiles(server,new ShareFilter());
       }
     }
     catch (SmbException e)
     {
       throw new ManifoldCFException("SmbException tossed: "+e.getMessage(),e);
     }
     return shares;
   }
 
   /**
   * Given a folder path, determine if the folder is in fact legal and accessible (and is a folder).
   * @param folder is the relative folder from the network root
   * @return the canonical folder name if valid, or null if not.
   * @throws ManifoldCFException
   */
   public String validateFolderName(String folder) throws ManifoldCFException
   {
     getSession();
     //create new connection by appending to the old connection
     String smburi = smbconnectionPath;
     String uri = smburi;
     if (folder.length() > 0) {
       uri = smburi + folder + "/";
     }
 
     SmbFile currentDirectory = null;
     try
     {
       currentDirectory = new SmbFile(uri,pa);
     }
     catch (MalformedURLException e1)
     {
       throw new ManifoldCFException("validateFolderName: Can't get parent file: " + uri,e1);
     }
 
     try
     {
       currentDirectory.connect();
       if (fileIsDirectory(currentDirectory) == false)
         return null;
       String newCanonicalPath = currentDirectory.getCanonicalPath();
       String rval = newCanonicalPath.substring(smburi.length());
       if (rval.endsWith("/"))
         rval = rval.substring(0,rval.length()-1);
       return rval;
     }
     catch (SmbException se)
     {
       try
       {
         processSMBException(se, folder, "checking folder", "getting canonical path");
         return null;
       }
       catch (ServiceInterruption si)
       {
         throw new ManifoldCFException("Service interruption: "+si.getMessage(),si);
       }
     }
     catch (MalformedURLException e)
     {
       throw new ManifoldCFException("MalformedURLException tossed: "+e.getMessage(),e);
     }
     catch (java.net.SocketTimeoutException e)
     {
       throw new ManifoldCFException("IOException tossed: "+e.getMessage(),e);
     }
     catch (InterruptedIOException e)
     {
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("IOException tossed: "+e.getMessage(),e);
     }
 
   }
 
   /**
   * given a smb uri, return all children directories
   *
   * @param folder is the relative folder from the network root
   * @return array of child folder names
   * @throws ManifoldCFException
   */
   public String[] getChildFolderNames(String folder) throws ManifoldCFException
   {
     getSession();
     //create new connection by appending to the old connection
     String smburi = smbconnectionPath;
     String uri = smburi;
     if (folder.length() > 0) {
       uri = smburi + folder + "/";
     }
 
     SmbFile currentDirectory = null;
     try
     {
       currentDirectory = new SmbFile(uri,pa);
     }
     catch (MalformedURLException e1)
     {
       throw new ManifoldCFException("getChildFolderNames: Can't get parent file: " + uri,e1);
     }
 
     // add DFS support
     SmbFile[] children = null;
     try
     {
       currentDirectory.connect();
       children = currentDirectory.listFiles(new DirectoryFilter());
     }
     catch (SmbException se)
     {
       try
       {
         processSMBException(se, folder, "getting child folder names", "listing files");
         children = new SmbFile[0];
       }
       catch (ServiceInterruption si)
       {
         throw new ManifoldCFException("Service interruption: "+si.getMessage(),si);
       }
     }
     catch (MalformedURLException e)
     {
       throw new ManifoldCFException("MalformedURLException tossed: "+e.getMessage(),e);
     }
     catch (java.net.SocketTimeoutException e)
     {
       throw new ManifoldCFException("IOException tossed: "+e.getMessage(),e);
     }
     catch (InterruptedIOException e)
     {
       throw new ManifoldCFException("Interrupted: "+e.getMessage(),e,ManifoldCFException.INTERRUPTED);
     }
     catch (IOException e)
     {
       throw new ManifoldCFException("IOException tossed: "+e.getMessage(),e);
     }
 
     // populate a String array
     String[] directories = new String[children.length];
     for (int i=0;i < children.length;i++){
       String directoryName = children[i].getName();
       // strip the trailing slash
       directoryName = directoryName.replaceAll("/","");
       directories[i] = directoryName;
     }
 
     java.util.Arrays.sort(directories);
     return directories;
   }
 
   /**
   * inner class which returns only shares. used by listfiles(SmbFileFilter)
   *
   * @author James Maupin
   */
 
   class ShareFilter implements SmbFileFilter
   {
     /* (non-Javadoc)
     * @see jcifs.smb.SmbFileFilter#accept(jcifs.smb.SmbFile)
     */
     public boolean accept(SmbFile arg0) throws SmbException
     {
       if (getFileType(arg0)==SmbFile.TYPE_SHARE){
         return true;
       } else {
         return false;
       }
     }
   }
 
   /**
   * inner class which returns only directories. used by listfiles(SmbFileFilter)
   *
   * @author James Maupin
   */
 
   class DirectoryFilter implements SmbFileFilter
   {
     /* (non-Javadoc)
     * @see jcifs.smb.SmbFileFilter#accept(jcifs.smb.SmbFile)
     */
     public boolean accept(SmbFile arg0) throws SmbException {
       int type = getFileType(arg0);
       if (type==SmbFile.TYPE_SHARE || (type==SmbFile.TYPE_FILESYSTEM && fileIsDirectory(arg0))){
         return true;
       } else {
         return false;
       }
     }
   }
 
   /** This is the filter class that actually receives the files in batches.  We do it this way
   * so that the client won't run out of memory loading a huge directory.
   */
   protected class ProcessDocumentsFilter implements SmbFileFilter
   {
 
     /** This is the activities object, where matching references will be logged */
     protected IProcessActivity activities;
     /** Document specification */
     protected DocumentSpecification spec;
     /** Exceptions that we saw.  These are saved here so that they can be rethrown when done */
     protected ManifoldCFException lcfException = null;
     protected ServiceInterruption serviceInterruption = null;
 
     /** Constructor */
     public ProcessDocumentsFilter(IProcessActivity activities, DocumentSpecification spec)
     {
       this.activities = activities;
       this.spec = spec;
     }
 
     /** Decide if we accept the file.  This is where we will actually do the work. */
     public boolean accept(SmbFile f) throws SmbException
     {
       if (lcfException != null || serviceInterruption != null)
         return false;
 
       try
       {
         int type = f.getType();
         if (type != SmbFile.TYPE_SERVER && type != SmbFile.TYPE_FILESYSTEM && type != SmbFile.TYPE_SHARE)
           return false;
         String canonicalPath = getFileCanonicalPath(f);
         if (canonicalPath != null)
         {
           // manipulate path to include the DFS alias, not the literal path
           // String newPath = matchPrefix + canonicalPath.substring(matchReplace.length());
           String newPath = canonicalPath;
 
           // Check against the current specification.  This is a nicety to avoid queuing
           // documents that we will immediately turn around and remove.  However, if this
           // check was not here, everything should still function, provided the getDocumentVersions()
           // method does the right thing.
           if (checkInclude(f, newPath, spec, activities))
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("JCIFS: Recorded path is '" + newPath + "' and is included.");
             activities.addDocumentReference(newPath);
           }
           else
           {
             if (Logging.connectors.isDebugEnabled())
               Logging.connectors.debug("JCIFS: Recorded path '"+newPath+"' is excluded!");
           }
         }
         else
           Logging.connectors.debug("JCIFS: Excluding a child file because canonical path is null");
 
 
         return false;
       }
       catch (ManifoldCFException e)
       {
         if (lcfException == null)
           lcfException = e;
         return false;
       }
       catch (ServiceInterruption e)
       {
         if (serviceInterruption == null)
           serviceInterruption = e;
         return false;
       }
     }
 
     /** Check for exception, and throw if there is one */
     public void checkAndThrow()
       throws ServiceInterruption, ManifoldCFException
     {
       if (lcfException != null)
         throw lcfException;
       if (serviceInterruption != null)
         throw serviceInterruption;
     }
   }
 
 }
