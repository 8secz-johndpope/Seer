 /*
  * ====================================================================
  * Copyright (c) 2004 TMate Software Ltd. All rights reserved.
  * 
  * This software is licensed as described in the file COPYING, which you should
  * have received as part of this distribution. The terms are also available at
  * http://tmate.org/svn/license.html. If newer versions of this license are
  * posted there, you may use a newer version instead, at your option.
  * ====================================================================
  */
 
 package org.tmatesoft.svn.core.internal.io.svn;
 
 import java.io.IOException;
 import java.io.OutputStream;
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 import org.tmatesoft.svn.core.ISVNDirEntryHandler;
 import org.tmatesoft.svn.core.ISVNLogEntryHandler;
 import org.tmatesoft.svn.core.SVNDirEntry;
 import org.tmatesoft.svn.core.SVNException;
 import org.tmatesoft.svn.core.SVNLock;
 import org.tmatesoft.svn.core.SVNLogEntry;
 import org.tmatesoft.svn.core.SVNLogEntryPath;
 import org.tmatesoft.svn.core.SVNNodeKind;
 import org.tmatesoft.svn.core.SVNProperty;
 import org.tmatesoft.svn.core.SVNRevisionProperty;
 import org.tmatesoft.svn.core.SVNURL;
 import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
 import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
 import org.tmatesoft.svn.core.io.ISVNEditor;
 import org.tmatesoft.svn.core.io.ISVNFileRevisionHandler;
 import org.tmatesoft.svn.core.io.ISVNLocationEntryHandler;
 import org.tmatesoft.svn.core.io.ISVNLockHandler;
 import org.tmatesoft.svn.core.io.ISVNReporter;
 import org.tmatesoft.svn.core.io.ISVNReporterBaton;
 import org.tmatesoft.svn.core.io.ISVNSession;
 import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
 import org.tmatesoft.svn.core.io.SVNFileRevision;
 import org.tmatesoft.svn.core.io.SVNLocationEntry;
 import org.tmatesoft.svn.core.io.SVNRepository;
 import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
 import org.tmatesoft.svn.core.io.diff.SVNDiffWindowBuilder;
 import org.tmatesoft.svn.util.SVNDebugLog;
 
 /**
  * @version 1.0
  * @author TMate Software Ltd.
  */
 public class SVNRepositoryImpl extends SVNRepository implements ISVNReporter {
 
     private SVNConnection myConnection;
     private String myRealm;
     private String myExternalUserName;
 
     protected SVNRepositoryImpl(SVNURL location, ISVNSession options) {
         super(location, options);
     }
 
     public void testConnection() throws SVNException {
         try {
             openConnection();
         } finally {
             closeConnection();
         }
     }
     
     public void setLocation(SVNURL url, boolean forceReconnect) throws SVNException {
         if (!url.equals(myConnection)) {
             super.setLocation(url, true);
         } else {
             super.setLocation(url, forceReconnect);
         }
         myRealm = null;
     }
 
     public long getLatestRevision() throws SVNException {
         Object[] buffer = new Object[] { "get-latest-rev" };
         try {
             openConnection();
             write("(w())", buffer);
             authenticate();
             buffer = read("[(N)]", buffer);
         } finally {
             closeConnection();
         }
         return SVNReader.getLong(buffer, 0);
     }
 
     public long getDatedRevision(Date date) throws SVNException {
         if (date == null) {
             date = new Date(System.currentTimeMillis());
         }
         Object[] buffer = new Object[] { "get-dated-rev", date };
         try {
             openConnection();
             write("(w(s))", buffer);
             authenticate();
             buffer = read("[(N)]", buffer);
         } finally {
             closeConnection();
         }
         return SVNReader.getLong(buffer, 0);
     }
 
     public Map getRevisionProperties(long revision, Map properties)
             throws SVNException {
         assertValidRevision(revision);
         if (properties == null) {
             properties = new HashMap();
         }
         Object[] buffer = new Object[] { "rev-proplist",
                 getRevisionObject(revision) };
         try {
             openConnection();
             write("(w(n))", buffer);
             authenticate();
             buffer[0] = properties;
             read("[((*P))]", buffer);
         } finally {
             closeConnection();
         }
         return properties;
     }
 
     public String getRevisionPropertyValue(long revision, String propertyName)
             throws SVNException {
         assertValidRevision(revision);
         Object[] buffer = new Object[] { "rev-prop", getRevisionObject(revision), propertyName };
         try {
             openConnection();
             write("(w(ns))", buffer);
             authenticate();
             buffer = read("[((?S))]", buffer);
         } finally {
             closeConnection();
         }
         return (String) buffer[0];
     }
 
     public SVNNodeKind checkPath(String path, long revision)
             throws SVNException {
         try {
             openConnection();
             path = getRepositoryPath(path);
             Object[] buffer = new Object[] { "check-path", path,
                     getRevisionObject(revision) };
             write("(w(s(n)))", buffer);
             authenticate();
             read("[(W)]", buffer);
 
             return SVNNodeKind.parseKind((String) buffer[0]);
         } finally {
             closeConnection();
         }
     }
 
     public int getLocations(String path, long pegRevision, long[] revisions,
             ISVNLocationEntryHandler handler) throws SVNException {
         assertValidRevision(pegRevision);
         for (int i = 0; i < revisions.length; i++) {
             assertValidRevision(revisions[i]);
         }
         int count = 0;
         try {
             openConnection();
             path = getRepositoryPath(path);
             Object[] buffer = new Object[] { "get-locations", path,
                     getRevisionObject(pegRevision), revisions };
             write("(w(sn(*n)))", buffer);
             authenticate();
             while (true) {
                 try {
                     read("(NS)", buffer);
                 } catch (SVNException e) {
                     break;
                 }
                 count++;
                 if (handler != null) {
                     long revision = SVNReader.getLong(buffer, 0);
                     String location = SVNReader.getString(buffer, 1);
                     if (location != null) {
                         handler.handleLocationEntry(new SVNLocationEntry(
                                 revision, location));
                     }
                 }
             }
             read("x", buffer);
             read("[()]", buffer);
         } finally {
             closeConnection();
         }
         return count;
     }
 
     public long getFile(String path, long revision, Map properties,
             OutputStream contents) throws SVNException {
         Long rev = revision > 0 ? new Long(revision) : null;
         try {
             openConnection();
             Object[] buffer = new Object[] { "get-file",
                     getRepositoryPath(path), rev,
                     Boolean.valueOf(properties != null),
                     Boolean.valueOf(contents != null) };
             write("(w(s(n)ww))", buffer);
             authenticate();
             buffer[2] = properties;
             buffer = read("[((?S)N(*P))]", buffer);
             if (properties != null) {
                 properties.put(SVNProperty.REVISION, buffer[1].toString());
                 properties.put(SVNProperty.CHECKSUM, buffer[0].toString());
             }
             if (contents != null) {
                 Object[] buffer2 = new Object[] { contents };
                 read("*I", buffer2);
                 read("[()]", buffer2);
             }
             return SVNReader.getLong(buffer, 1);
         } finally {
             closeConnection();
         }
     }
 
     public long getDir(String path, long revision, Map properties, ISVNDirEntryHandler handler) throws SVNException {
         Long rev = getRevisionObject(revision);
         // convert path to path relative to repos root.
         try {
             openConnection();
             path = getRepositoryPath(path);
             Object[] buffer = new Object[] { "get-dir", path, rev,
                     Boolean.valueOf(properties != null),
                     Boolean.valueOf(handler != null) };
             write("(w(s(n)ww))", buffer);
             authenticate();
 
             buffer[1] = properties;
             buffer = read("[(N(*P)", buffer);
             revision = buffer[0] != null ? SVNReader.getLong(buffer, 0) : revision;
             if (handler != null) {
                 buffer[0] = handler;
                 read("(*D)))", buffer);
             } else {
                 read("()))", null);
             }
         } finally {
             closeConnection();
         }
         return revision;
     }
 
     public SVNDirEntry getDir(String path, long revision, boolean includeComment, final Collection entries) throws SVNException {
         Long rev = getRevisionObject(revision);
         ISVNDirEntryHandler handler = new ISVNDirEntryHandler() {
             public void handleDirEntry(SVNDirEntry dirEntry) {
                 entries.add(dirEntry);
             }            
         };
         // convert path to path relative to repos root.
         SVNDirEntry parentEntry = null;
         try {
             openConnection();
             path = getRepositoryPath(path);
             // get parent
             Object[] buffer = new Object[] { "stat", path, getRevisionObject(revision) };
             write("(w(s(n)))", buffer);
             authenticate();
             read("[((?F))]", buffer);
             parentEntry = (SVNDirEntry) buffer[0];
             parentEntry.setPath(path);
             parentEntry.setName("");
 
             // get entries.
             buffer = new Object[] { "get-dir", path, rev, Boolean.FALSE, Boolean.TRUE };
             write("(w(s(n)ww))", buffer);
             authenticate();
             buffer = read("[(N(*P)", buffer);
             revision = buffer[0] != null ? SVNReader.getLong(buffer, 0) : revision;
             if (handler != null) {
                 buffer[0] = handler;
                 read("(*D)))", buffer);
             } else {
                 read("()))", null);
             }
             // get comments.
             if (includeComment) {
                 Map messages = new HashMap();
                 for(Iterator ents = entries.iterator(); ents.hasNext();) {
                     SVNDirEntry entry = (SVNDirEntry) ents.next();
                     Long key = getRevisionObject(entry.getRevision());
                     if (messages.containsKey(key)) {
                         entry.setCommitMessage((String) messages.get(key));
                         continue;
                     }
                     buffer = new Object[] { "rev-prop", key, SVNRevisionProperty.LOG};
                     write("(w(ns))", buffer);
                     authenticate();
                     buffer = read("[((?S))]", buffer);
                     messages.put(key, buffer[0]);
                     entry.setCommitMessage((String) buffer[0]);
                 }
             }
         } finally {
             closeConnection();
         }
         return parentEntry;
     }
 
     public int getFileRevisions(String path, long sRevision, long eRevision,
             ISVNFileRevisionHandler handler) throws SVNException {
         Long srev = getRevisionObject(sRevision);
         Long erev = getRevisionObject(eRevision);
         int count = 0;
         try {
             openConnection();
             Object[] buffer = new Object[] { "get-file-revs",
                     getRepositoryPath(path), srev, erev };
             write("(w(s(n)(n)))", buffer);
             authenticate();
             buffer = new Object[5];
             while (true) {
                 SVNFileRevision fileRevision = null;
                 boolean skipDelta = false;
                 try {
                     buffer = read("(SN(*P)(*Z)?S", buffer);
                     if (buffer[4] != null && ((String) buffer[4]).length() == 0) {
                         buffer[4] = null;
                         skipDelta = true;
                     } else {
                         read(")", null);
                     }
                     count++;
                 } catch (SVNException e) {
                     read("x", buffer);
                     read("[()]", buffer);
                     return count;
                 }
                 String name = null;
                 if (handler != null) {
                     name = (String) buffer[0];
                     long revision = SVNReader.getLong(buffer, 1);
                     Map properties = SVNReader.getMap(buffer, 2);
                     Map propertiesDelta = SVNReader.getMap(buffer, 3);
                     if (name != null) {
                         fileRevision = new SVNFileRevision(name, revision,
                                 properties, propertiesDelta);
                     }
                     buffer[2] = null;
                     buffer[3] = null;
                 }
                 if (handler != null && fileRevision != null) {
                     handler.openRevision(fileRevision);
                 }
                 if (skipDelta) {
                     if (handler != null) {
                         handler.closeRevision(name == null ? path : name);
                     }
                     continue;
                 }
                 SVNDiffWindowBuilder builder = SVNDiffWindowBuilder.newInstance();
                 boolean windowRead = false;
                 while (true) {
                     byte[] line = (byte[]) read("?W?B", buffer)[1];
                     if (line == null) {
                         // may be failure
                         read("[]", buffer);
                         break;
                     } else if (line.length == 0) {
                         // empty line, delta end.
                         break;
                     }
                     // apply delta here.
                     if (!windowRead) {
                         if (handler != null) {
                             handler.applyTextDelta(name == null ? path : name);
                             windowRead = true;
                         }
                     }
                     builder.accept(line, 0);
                     SVNDiffWindow window = builder.getDiffWindow();
                     if (window != null) {
                         OutputStream os = handler.textDeltaChunk(name == null ? path : name, window);
                         if (os != null) {
                             try {
                                 os.write(builder.getInstructionsData());
                             } catch (IOException e) {
                                 SVNErrorManager.error(e.getMessage());
                             }
                         }
                         builder.reset(SVNDiffWindowBuilder.OFFSET);
                         long length = window.getNewDataLength();
                         while (length > 0) {
                             byte[] contents = (byte[]) myConnection.read("B", null)[0];
                             length -= contents.length;
                             try {
                                 if (os != null) {
                                     os.write(contents);
                                 }
                             } catch (IOException th) {
                                 SVNDebugLog.logInfo(th);
                             }
                         }
                         try {
                             if (os != null) {
                                 os.close();
                             }
                         } catch (IOException th) {
                             SVNDebugLog.logInfo(th);
                         }
                     }
                 }
                 if (windowRead) {
                     handler.textDeltaEnd(name == null ? path : name);
                 }
                 if (handler != null) {
                     handler.closeRevision(name == null ? path : name);
                 }
             }
         } finally {
             closeConnection();
         }
     }
 
     public long log(String[] targetPaths, long startRevision, long endRevision,
             boolean changedPaths, boolean strictNode, long limit,
             ISVNLogEntryHandler handler) throws SVNException {
         long count = 0;
         try {
             openConnection();
             Object[] buffer = new Object[] { "log",
                     getRepositoryPaths(targetPaths),
                     getRevisionObject(startRevision),
                     getRevisionObject(endRevision),
                     Boolean.valueOf(changedPaths), Boolean.valueOf(strictNode),
                     limit > 0 ? new Long(limit) : null };
             write("(w((*s)(n)(n)wwn))", buffer);
             authenticate();
             while (true) {
                 try {
                     read("((", buffer);
                     Map changedPathsMap = null;
                     if (changedPaths) {
                         changedPathsMap = handler != null ? new HashMap()
                                 : null;
                         while (true) {
                             try {
                                 read("(SW(?S?N))", buffer);
                                 if (changedPathsMap != null) {
                                     String path = SVNReader
                                             .getString(buffer, 0);
                                     if (path != null && !"".equals(path.trim())) {
                                         String type = SVNReader.getString(
                                                 buffer, 1);
                                         String copyPath = SVNReader.getString(
                                                 buffer, 2);
                                         long copyRev = SVNReader.getLong(
                                                 buffer, 3);
                                         changedPathsMap.put(path,
                                                 new SVNLogEntryPath(path, type
                                                         .charAt(0), copyPath,
                                                         copyRev));
                                     }
                                 }
                             } catch (SVNException e) {
                                 break;
                             }
                         }
                     }
                     read(")N(?S)(?S)(?S))", buffer);
                     count++;
                     if (handler != null && (limit <= 0 || count <= limit)) {
                         long revision = SVNReader.getLong(buffer, 0);
                         String author = SVNReader.getString(buffer, 1);
                         Date date = SVNReader.getDate(buffer, 2);
                         String message = SVNReader.getString(buffer, 3);
                         // remove all
                             handler.handleLogEntry(new SVNLogEntry(
                                     changedPathsMap, revision, author, date,
                                     message));
                     }
                 } catch (SVNException e) {
                     read("x", buffer);
                     if (limit <= 0 || (limit > 0 && count <= limit)) {
                         read("[()]", buffer);
                     }
                     return count;
                 }
             }
         } finally {
             closeConnection();
         }
     }
 
     public void update(long revision, String target, boolean recursive,
             ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
         target = target == null ? "" : target;
         Object[] buffer = new Object[] { "update", getRevisionObject(revision),
                 target, Boolean.valueOf(recursive) };
         try {
             openConnection();
             write("(w((n)sw))", buffer);
             authenticate();
             reporter.report(this);
             authenticate();
             read("*E", new Object[] { editor });
         } finally {
             closeConnection();
         }
     }
 
     public void update(SVNURL url, long revision, String target,
             boolean recursive, ISVNReporterBaton reporter, ISVNEditor editor)
             throws SVNException {
         target = target == null ? "" : target;
         if (url == null) {
             throw new SVNException(url + ": not valid URL");
         }
         Object[] buffer = new Object[] { "switch", getRevisionObject(revision),
                 target, Boolean.valueOf(recursive), url.toString() };
         try {
             openConnection();
             write("(w((n)sws))", buffer);
             authenticate();
             reporter.report(this);
             authenticate();
             read("*E", new Object[] { editor });
         } finally {
             closeConnection();
         }
     }
 
     public void diff(SVNURL url, long revision, String target,
             boolean ignoreAncestry, boolean recursive,
             ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
         diff(url, revision, revision, target, ignoreAncestry, recursive,
                 reporter, editor);
     }
 
     public void diff(SVNURL url, long tRevision, long revision, String target,
             boolean ignoreAncestry, boolean recursive,
             ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
         target = target == null ? "" : target;
         if (url == null) {
             throw new SVNException(url + ": not valid URL");
         }
         Object[] buffer = new Object[] { "diff", getRevisionObject(tRevision),
                 target, Boolean.valueOf(recursive),
                 Boolean.valueOf(ignoreAncestry), url.toString() };
         try {
             openConnection();
             write("(w((n)swws))", buffer);
             authenticate();
             reporter.report(this);
             authenticate();
             read("*E", new Object[] { editor });
         } finally {
             closeConnection();
         }
     }
 
     public void status(long revision, String target, boolean recursive,
             ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
         target = target == null ? "" : target;
         Object[] buffer = new Object[] { "status", target,
                 Boolean.valueOf(recursive), getRevisionObject(revision) };
         try {
             openConnection();
             write("(w(sw(n)))", buffer);
             authenticate();
             reporter.report(this);
             authenticate();
             read("*E", new Object[] { editor });
         } finally {
             closeConnection();
         }
     }
 
     public void setRevisionPropertyValue(long revision, String propertyName,
             String propertyValue) throws SVNException {
         assertValidRevision(revision);
         Object[] buffer = new Object[] { "change-rev-prop",
                 getRevisionObject(revision), propertyName, propertyValue };
         try {
             openConnection();
             write("(w(nss))", buffer);
             authenticate();
             read("[()]", buffer);
         } finally {
             closeConnection();
         }
     }
 
     public ISVNEditor getCommitEditor(String logMessage, Map locks,
             boolean keepLocks, final ISVNWorkspaceMediator mediator)
             throws SVNException {
         try {
             openConnection();
             if (locks != null) {
                 write("(w(s(*l)w))", new Object[] { "commit", logMessage,
                         locks, Boolean.valueOf(keepLocks) });
             } else {
                 write("(w(s))", new Object[] { "commit", logMessage });
             }
             authenticate();
             read("[()]", null);
             return new SVNCommitEditor(this, myConnection,
                     new Runnable() {
                         public void run() {
                             closeConnection();
                         }
                     });
         } catch (SVNException e) {
             closeConnection();
             throw e;
         }
     }
 
     public SVNLock getLock(String path) throws SVNException {
         try {
             openConnection();
             path = getRepositoryPath(path);
             Object[] buffer = new Object[] { "get-lock", path };
             write("(w(s))", buffer);
             authenticate();
             read("[((?L))]", buffer);
             return (SVNLock) buffer[0];
         } finally {
             closeConnection();
         }
     }
 
     public SVNLock[] getLocks(String path) throws SVNException {
         try {
             openConnection();
             path = getRepositoryPath(path);
             Object[] buffer = new Object[] { "get-locks", path };
             write("(w(s))", buffer);
             authenticate();
             read("[((*L))]", buffer);
             Collection lockObjects = (Collection) buffer[0];
             return lockObjects == null ? new SVNLock[0] : (SVNLock[]) lockObjects.toArray(new SVNLock[lockObjects.size()]);
         } finally {
             closeConnection();
         }
     }
 
     public void lock(Map pathsToRevisions, String comment, boolean force, ISVNLockHandler handler) throws SVNException {
         try {
             openConnection();
             Object[] buffer = new Object[] { "lock-many", comment, Boolean.valueOf(force) };
             write("(w((s)w(", buffer);
             buffer = new Object[2];
             for (Iterator paths = pathsToRevisions.keySet().iterator(); paths.hasNext();) {
                 buffer[0] = paths.next();
                 buffer[1] = pathsToRevisions.get(buffer[0]);
                 write("(s(n))", buffer);
             }
             write(")))", buffer);
             try {
                 authenticate();
             } catch (SVNException e) {
                 if (e.getMessage() != null && e.getMessage().toLowerCase().indexOf("unknown command") > 0) {
                     lock12(pathsToRevisions, comment, force, handler);
                     return;
                 }
                 throw e;
             }
             for (Iterator paths = pathsToRevisions.keySet().iterator(); paths.hasNext();) {
                 String path = (String) paths.next();
                 SVNLock lock = null;
                 SVNException error = null;
                 try {
                    read("[(L)]", buffer);
                     lock = (SVNLock) buffer[0];
                     path = lock.getPath();
                 } catch (SVNException e) {
                     // try to read 'x'
                     try {
                         read("x", buffer);
                         read("[()]", buffer);
                         return;
                     } catch (SVNException e2) {
                         //
                     }
                     path = getRepositoryPath(path);
                     error = e;
                 }
                 if (handler != null) {
                     handler.handleLock(path, lock, error);
                 }
             }
             read("x", buffer);
             read("[()]", buffer);
         } finally {
             closeConnection();
         }
     }
 
     private void lock12(Map pathsToRevisions, String comment, boolean force, ISVNLockHandler handler) throws SVNException {
         for(Iterator paths = pathsToRevisions.keySet().iterator(); paths.hasNext();) {
             String path = (String) paths.next();
             Long revision = (Long) pathsToRevisions.get(path);
             path = getRepositoryPath(path);
             Object[] buffer = new Object[] { "lock", path, comment, Boolean.valueOf(force), revision };
             write("(w(s(s)w(n)))", buffer);
             authenticate();
             SVNException error = null;
             try {
                 read("[(L)]", buffer);
             } catch (SVNException e) {
                 error = e;
             }
             if (handler != null) {
                 SVNLock lock = (SVNLock) buffer[0];
                 handler.handleLock(path, lock, error);
             }
         }
     }
 
     public void unlock(Map pathToTokens, boolean force, ISVNLockHandler handler) throws SVNException {
         try {
             openConnection();
             Object[] buffer = new Object[] { "unlock-many", Boolean.valueOf(force) };
             write("(w(w(", buffer);
             buffer = new Object[2];
             for (Iterator paths = pathToTokens.keySet().iterator(); paths.hasNext();) {
                 buffer[0] = paths.next();
                 buffer[1] = pathToTokens.get(buffer[0]);
                 write("(s(s))", buffer);
             }
             write(")))", buffer);
             try {
                 authenticate();
             } catch (SVNException e) {
                 if (e.getMessage() != null && e.getMessage().toLowerCase().indexOf("unknown command") > 0) {
                     unlock12(pathToTokens, force, handler);
                     return;
                 }
                 throw e;
             }
             for (Iterator paths = pathToTokens.keySet().iterator(); paths.hasNext();) {
                 String path = (String) paths.next();
                 String id = (String) pathToTokens.get(path);
                 SVNException error = null;
                 try {
                     read("[(S)]", buffer);
                     path = (String) buffer[0];
                 } catch (SVNException e) {
                     // try to read 'x'
                     try {
                         read("x", buffer);
                         read("[()]", buffer);
                         return;
                     } catch (SVNException e2) {
                         //
                     }
                     path = getRepositoryPath(path);
                     error = e;
                 }
                 if (handler != null) {
                     handler.handleUnlock(path, new SVNLock(path, id, null, null, null, null), error);
                 }
             }
             read("x", buffer);
             read("[()]", buffer);
         } finally {
             closeConnection();
         }
     }
 
     private void unlock12(Map pathToTokens, boolean force, ISVNLockHandler handler) throws SVNException {
         for (Iterator paths = pathToTokens.keySet().iterator(); paths.hasNext();) {
             String path = (String) paths.next();
             String id = (String) pathToTokens.get(path);
             path = getRepositoryPath(path);
             if (id  == null) {
                 Object[] buffer = new Object[] { "get-lock", path };
                 write("(w(s))", buffer);
                 authenticate();
                 read("[((?L))]", buffer);
                 SVNLock lock = (SVNLock) buffer[0];
                 if (lock == null) {
                     lock = new SVNLock(path, "", null, null, null, null);
                     handler.handleUnlock(path, lock, new SVNException("svn: No lock on path '" + path + "'"));
                     continue;
                 }
                 id = lock.getID();
             }
             Object[] buffer = new Object[] { "unlock", path, id, Boolean.valueOf(force) };
             write("(w(s(s)w))", buffer);
             authenticate();
             SVNException error = null;
             try {
                 read("[()]", buffer);
             } catch (SVNException e) {
                 error = e;
             }
             if (handler != null) {
                 SVNLock lock = new SVNLock(path, id, null, null, null, null);
                 handler.handleUnlock(path, lock, error);
             }
         }
     }
 
     public SVNDirEntry info(String path, long revision) throws SVNException {
         try {
             openConnection();
             path = getRepositoryPath(path);
             Object[] buffer = new Object[] { "stat", path,
                     getRevisionObject(revision) };
             write("(w(s(n)))", buffer);
             authenticate();
             read("[((?F))]", buffer);
             SVNDirEntry entry = (SVNDirEntry) buffer[0];
             if (entry != null) {
                 entry.setName(SVNPathUtil.tail(path));
             }
             return (SVNDirEntry) buffer[0];
         } finally {
             closeConnection();
         }
     }
 
     void updateCredentials(String uuid, SVNURL rootURL) throws SVNException {
         if (getRepositoryRoot(false) != null) {
             return;
         }
         setRepositoryCredentials(uuid, rootURL);
     }
 
     private void openConnection() throws SVNException {
         lock();
         if (getOptions().keepConnection(this) && myConnection != null) {
             return;
         }
         ISVNConnector connector = SVNRepositoryFactoryImpl.getConnectorFactory().createConnector(this);
         myConnection = new SVNConnection(connector, this);
         try {
             myConnection.open(this);
             authenticate();
         } finally {
             myRealm = myConnection.getRealm();
         }
     }
 
     public String getRealm() {
         return myRealm;
     }
 
     void authenticate() throws SVNException {
         if (myConnection != null) {
             myConnection.authenticate(this);
         }
     }
 
     private void closeConnection() {
         if (getOptions().keepConnection(this)) {
             unlock();
             return;
         }
         if (myConnection != null) {
             try {
                 myConnection.close();
             } catch (SVNException e) {
                 //
             } finally {
                 myConnection = null;
             }
         }
         unlock();
     }
 
     private void write(String template, Object[] values) throws SVNException {
         if (myConnection == null) {
             throw new SVNException("connection is closed, can't write");
         }
         myConnection.write(template, values);
     }
 
     private Object[] read(String template, Object[] values) throws SVNException {
         if (myConnection == null) {
             throw new SVNException("connection is closed, can't read");
         }
         return myConnection.read(template, values);
     }
 
     /*
      * ISVNReporter methods
      */
 
     public void setPath(String path, String lockToken, long revision,  boolean startEmpty) throws SVNException {
         assertValidRevision(revision);
         if (lockToken == null) {
             write("(w(snw))", new Object[] { "set-path", path,
                     getRevisionObject(revision), Boolean.valueOf(startEmpty) });
         } else {
             write("(w(snw(s)))", new Object[] { "set-path", path,
                     getRevisionObject(revision), Boolean.valueOf(startEmpty),
                     lockToken });
         }
     }
 
     public void deletePath(String path) throws SVNException {
         write("(w(s))", new Object[] { "delete-path", path });
     }
 
     public void linkPath(SVNURL url, String path,
             String lockToken, long revison, boolean startEmpty)
             throws SVNException {
         assertValidRevision(revison);
         if (lockToken == null) {
             write("(w(ssnw))", new Object[] { "link-path", path,
                     url.toString(), getRevisionObject(revison),
                     Boolean.valueOf(startEmpty) });
         } else {
             write("(w(ssnw(s)))", new Object[] { "link-path", path,
                     url.toString(), getRevisionObject(revison),
                     Boolean.valueOf(startEmpty), lockToken });
         }
     }
 
     public void finishReport() throws SVNException {
         write("(w())", new Object[] { "finish-report" });
     }
 
     public void abortReport() throws SVNException {
         write("(w())", new Object[] { "abort-report" });
     }
 
     private String[] getRepositoryPaths(String[] paths) throws SVNException {
         if (paths == null || paths.length == 0) {
             return paths;
         }
         String[] fullPaths = new String[paths.length];
         for (int i = 0; i < paths.length; i++) {
             fullPaths[i] = getRepositoryPath(paths[i]);
         }
         return fullPaths;
     }
 
     // all paths are uri-decoded.
     //
     // get repository path (path starting with /, relative to repository root).
     // get full path (path starting with /, relative to host).
     // get relative path (repository path, now relative to repository location, not starting with '/').
 
     public void setExternalUserName(String userName) {
         myExternalUserName = userName;
     }
 
     public String getExternalUserName() {
         return myExternalUserName;
     }
 
     public void closeSession() throws SVNException {
         if (myConnection != null) {
             try {
                 myConnection.close();
             } catch (SVNException e) {
                 //
             } finally {
                 myConnection = null;
             }
         }
     }
 }
