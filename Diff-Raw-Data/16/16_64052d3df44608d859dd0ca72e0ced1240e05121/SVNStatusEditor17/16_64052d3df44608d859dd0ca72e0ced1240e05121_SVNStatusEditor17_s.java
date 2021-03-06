 /*
  * ====================================================================
  * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
  *
  * This software is licensed as described in the file COPYING, which
  * you should have received as part of this distribution.  The terms
  * are also available at http://svnkit.com/license.html.
  * If newer versions of this license are posted there, you may use a
  * newer version instead, at your option.
  * ====================================================================
  */
 package org.tmatesoft.svn.core.internal.wc17;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.StringTokenizer;
 
 import org.tmatesoft.svn.core.SVNCommitInfo;
 import org.tmatesoft.svn.core.SVNDepth;
 import org.tmatesoft.svn.core.SVNErrorCode;
 import org.tmatesoft.svn.core.SVNException;
 import org.tmatesoft.svn.core.SVNLock;
 import org.tmatesoft.svn.core.SVNNodeKind;
 import org.tmatesoft.svn.core.SVNProperty;
 import org.tmatesoft.svn.core.SVNURL;
 import org.tmatesoft.svn.core.internal.util.SVNHashMap;
 import org.tmatesoft.svn.core.internal.util.SVNHashSet;
 import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
 import org.tmatesoft.svn.core.internal.wc.SVNExternal;
 import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
 import org.tmatesoft.svn.core.internal.wc.SVNFileType;
 import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
 import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
 import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
 import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
 import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo;
 import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
 import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
 import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbRepositoryInfo.RepositoryInfoField;
 import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
 import org.tmatesoft.svn.core.wc.ISVNOptions;
 import org.tmatesoft.svn.core.wc.ISVNStatusFileProvider;
 import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
 import org.tmatesoft.svn.core.wc.SVNConflictDescription;
 import org.tmatesoft.svn.core.wc.SVNStatus;
 import org.tmatesoft.svn.core.wc.SVNStatusType;
 
 /**
  * @version 1.3
  * @author TMate Software Ltd.
  */
 public class SVNStatusEditor17 {
 
     protected SVNWCContext myWCContext;
     protected File myPath;
 
     protected boolean myIsReportAll;
     protected boolean myIsNoIgnore;
     protected SVNDepth myDepth;
 
     protected ISVNStatusHandler myStatusHandler;
 
     protected Map myExternalsMap;
     protected Collection myGlobalIgnores;
 
     protected SVNURL myRepositoryRoot;
     protected Map myRepositoryLocks;
     protected long myTargetRevision;
     protected String myWCRootPath;
     protected ISVNStatusFileProvider myFileProvider;
     protected ISVNStatusFileProvider myDefaultFileProvider;
     protected boolean myIsGetExcluded;
 
     protected SVNExternalsStore myExternalsStore;
 
     public SVNStatusEditor17(File path, SVNWCContext wcContext, ISVNOptions options, boolean noIgnore, boolean reportAll, SVNDepth depth, SVNExternalsStore externalsStore, ISVNStatusHandler handler) {
 
         myWCContext = wcContext;
         myPath = path;
         myIsNoIgnore = noIgnore;
         myIsReportAll = reportAll;
         myDepth = depth;
         myStatusHandler = handler;
         myExternalsMap = new SVNHashMap();
         myGlobalIgnores = getGlobalIgnores(options);
         myTargetRevision = -1;
         myDefaultFileProvider = new DefaultSVNStatusFileProvider();
         myFileProvider = myDefaultFileProvider;
 
         myIsGetExcluded = false;
 
         myExternalsStore = externalsStore;
 
     }
 
     public SVNCommitInfo closeEdit() throws SVNException {
 
         final SVNNodeKind localKind = SVNFileType.getNodeKind(SVNFileType.getType(myPath));
         final SVNNodeKind kind = myWCContext.getNodeKind(myPath, false);
 
         if (kind == SVNNodeKind.FILE && localKind == SVNNodeKind.FILE) {
             getDirStatus(SVNFileUtil.getFileDir(myPath), null, null, SVNFileUtil.getFileName(myPath), myGlobalIgnores, myDepth, myIsReportAll, true, true, myStatusHandler);
         } else if (kind == SVNNodeKind.DIR && localKind == SVNNodeKind.DIR) {
             getDirStatus(myPath, null, null, null, myGlobalIgnores, myDepth, myIsReportAll, myIsNoIgnore, false, myStatusHandler);
         } else {
             getDirStatus(SVNFileUtil.getFileDir(myPath), null, null, SVNFileUtil.getFileName(myPath), myGlobalIgnores, myDepth, myIsReportAll, myIsNoIgnore, true, myStatusHandler);
         }
 
         return null;
     }
 
     public long getTargetRevision() {
         return myTargetRevision;
     }
 
     public void targetRevision(long revision) {
         myTargetRevision = revision;
     }
 
     public void setFileProvider(ISVNStatusFileProvider filesProvider) {
         myFileProvider = filesProvider;
     }
 
     public SVNDepth getDepth() {
         return myDepth;
     }
 
     protected ISVNStatusHandler getDefaultHandler() {
         return myStatusHandler;
     }
 
     protected boolean isReportAll() {
         return myIsReportAll;
     }
 
     protected boolean isNoIgnore() {
         return myIsNoIgnore;
     }
 
     private static Collection getGlobalIgnores(ISVNOptions options) {
         if (options != null) {
             String[] ignores = options.getIgnorePatterns();
             if (ignores != null) {
                 Collection patterns = new SVNHashSet();
                 for (int i = 0; i < ignores.length; i++) {
                     patterns.add(ignores[i]);
                 }
                 return patterns;
             }
         }
         return Collections.EMPTY_SET;
     }
 
     private static class DefaultSVNStatusFileProvider implements ISVNStatusFileProvider {
 
         public Map getChildrenFiles(File parent) {
             File[] children = SVNFileListUtil.listFiles(parent);
             if (children != null) {
                 Map map = new SVNHashMap();
                 for (int i = 0; i < children.length; i++) {
                     map.put(SVNFileUtil.getFileName(children[i]), children[i]);
                 }
                 return map;
             }
             return Collections.EMPTY_MAP;
         }
     }
 
     protected void getDirStatus(File localAbsPath, SVNURL parentReposRootUrl, File parentReposRelPath, String selected, Collection ignorePatterns, SVNDepth depth, boolean getAll, boolean noIgnore,
             boolean skipThisDir, ISVNStatusHandler handler) throws SVNException {
 
         myWCContext.checkCancelled();
 
         depth = depth == SVNDepth.UNKNOWN ? SVNDepth.INFINITY : depth;
         final Map<String, File> childrenFiles = myFileProvider.getChildrenFiles(localAbsPath);
         final ISVNWCDb db = myWCContext.getDb();
 
         /* Load list of childnodes. */
         final Map<String, File> nodes = new HashMap<String, File>();
         {
             final List<String> childNodes = db.readChildren(localAbsPath);
             if (childNodes != null && childNodes.size() > 0) {
                 for (String childNode : childNodes) {
                     final File file = SVNFileUtil.createFilePath(childNode);
                     nodes.put(SVNFileUtil.getFileName(file), file);
                 }
             }
         }
 
         final WCDbInfo dirInfo = db.readInfo(localAbsPath, InfoField.status, InfoField.reposRelPath, InfoField.reposRootUrl, InfoField.depth);
         SVNWCDbStatus dirStatus = dirInfo.status;
         File dirReposRelPath = dirInfo.reposRelPath;
         SVNURL dirReposRootUrl = dirInfo.reposRootUrl;
         SVNDepth dirDepth = dirInfo.depth;
 
         if (dirReposRelPath == null) {
             if (dirReposRootUrl != null) {
                 dirReposRootUrl = parentReposRootUrl;
                 dirReposRelPath = SVNFileUtil.createFilePath(parentReposRelPath, SVNFileUtil.getFileName(localAbsPath));
             } else if (dirStatus != SVNWCDbStatus.Deleted && dirStatus != SVNWCDbStatus.Added) {
                 final WCDbRepositoryInfo baseReposInfo = db.scanBaseRepository(localAbsPath, RepositoryInfoField.relPath, RepositoryInfoField.rootUrl);
                 dirReposRelPath = baseReposInfo.relPath;
                 dirReposRootUrl = baseReposInfo.rootUrl;
             } else {
                 dirReposRelPath = null;
                 dirReposRootUrl = null;
             }
         }
 
         final Map<String, File> allChildren = new HashMap();
         final Map<String, File> conflicts = new HashMap();
         Collection<String> patterns = null;
 
         if (selected == null) {
             /* Create a hash containing all children */
             allChildren.putAll(childrenFiles);
             allChildren.putAll(nodes);
             final List<String> victims = db.readConflictVictims(localAbsPath);
             if (victims != null && victims.size() > 0) {
                 for (String confict : victims) {
                     File file = SVNFileUtil.createFilePath(confict);
                     conflicts.put(SVNFileUtil.getFileName(file), file);
                 }
             }
             /* Optimize for the no-tree-conflict case */
             if (conflicts.size() > 0) {
                 allChildren.putAll(conflicts);
             }
         } else {
             final File selectedAbsPath = SVNFileUtil.createFilePath(localAbsPath, selected);
             allChildren.put(selected, selectedAbsPath);
             final SVNConflictDescription tc = db.opReadTreeConflict(selectedAbsPath);
             /* Note this path if a tree conflict is present. */
             if (tc != null) {
                 conflicts.put(selected, selectedAbsPath);
             }
         }
 
         handleExternals(localAbsPath, dirDepth);
 
         if (selected == null) {
             /* Handle "this-dir" first. */
             if (!skipThisDir) {
                 sendStatusStructure(localAbsPath, parentReposRootUrl, parentReposRelPath, SVNNodeKind.DIR, false, getAll, handler);
             }
             /* If the requested depth is empty, we only need status on this-dir. */
             if (depth == SVNDepth.EMPTY) {
                 return;
             }
         }
 
         /*
          * Add empty status structures for each of the unversioned things. This
          * also catches externals; not sure whether that's good or bad, but it's
          * what's happening right now.
          */
         for (String key : allChildren.keySet()) {
             final File nodeAbsPath = SVNFileUtil.createFilePath(localAbsPath, key);
             final File dirent = childrenFiles.get(key);
             final SVNFileType direntFileType = dirent != null ? SVNFileType.getType(dirent) : null;
             final SVNNodeKind direntNodeKind = dirent != null ? SVNFileType.getNodeKind(direntFileType) : SVNNodeKind.NONE;
             boolean direntIsSpecial = dirent != null ? direntFileType == SVNFileType.SYMLINK : false;
 
             if (nodes.containsKey(key)) {
                 /* Versioned node */
                 WCDbInfo node = db.readInfo(nodeAbsPath, InfoField.status, InfoField.kind);
                 boolean hidden = db.isNodeHidden(nodeAbsPath);
 
                 /*
                  * Hidden looks in the parent stubs, which should not be
                  * necessary later. Also skip excluded/absent/not-present
                  * working nodes, which only have an implied status via their
                  * parent.
                  */
                 if (!hidden && node.status != SVNWCDbStatus.Excluded && node.status != SVNWCDbStatus.Absent && node.status != SVNWCDbStatus.NotPresent) {
                     if (depth == SVNDepth.FILES && node.kind == SVNWCDbKind.Dir)
                         continue;
                     /* Handle this entry (possibly recursing). */
                     handleDirEntry(nodeAbsPath, node.status, node.kind, dirReposRootUrl, dirReposRelPath, direntNodeKind, direntIsSpecial, ignorePatterns, depth == SVNDepth.INFINITY ? depth
                             : SVNDepth.EMPTY, getAll, noIgnore, handler);
                     continue;
                 }
             }
 
             if (conflicts.containsKey(key)) {
                 /* Tree conflict */
                 if (ignorePatterns != null && patterns == null) {
                     patterns = collectIgnorePatterns(localAbsPath, ignorePatterns);
                 }
                 sendUnversionedItem(nodeAbsPath, direntNodeKind, patterns, noIgnore, handler);
                 continue;
             }
 
             /* Unversioned node */
             if (dirent == null) {
                 continue; /* Selected node, but not found */
             }
 
             if (depth == SVNDepth.FILES && SVNFileType.getNodeKind(direntFileType) == SVNNodeKind.DIR) {
                 continue;
             }
 
             if (SVNWCContext.isAdminDirectory(key)) {
                 continue;
             }
 
             if (ignorePatterns != null && patterns == null)
                 patterns = collectIgnorePatterns(localAbsPath, ignorePatterns);
 
             sendUnversionedItem(nodeAbsPath, direntNodeKind, patterns, noIgnore || selected != null, handler);
         }
 
     }
 
     private void sendStatusStructure(File localAbsPath, SVNURL parentReposRootUrl, File parentReposRelPath, SVNNodeKind pathKind, boolean pathSpecial, boolean getAll, ISVNStatusHandler handler)
             throws SVNException {
 
         final ISVNWCDb db = myWCContext.getDb();
 
         /* Check for a repository lock. */
         SVNLock repositoryLock = null;
         if (myRepositoryLocks != null) {
             final WCDbInfo info = db.readInfo(localAbsPath, InfoField.status, InfoField.reposRelPath, InfoField.haveBase);
             SVNWCDbStatus status = info.status;
             File reposRelpath = info.reposRelPath;
             boolean haveBase = info.haveBase;
 
             /* A switched path can be deleted: check the right relpath */
             if (status == SVNWCDbStatus.Deleted && haveBase) {
                 final WCDbRepositoryInfo reposInfo = db.scanBaseRepository(localAbsPath, RepositoryInfoField.relPath);
                 reposRelpath = reposInfo.relPath;
             }
 
             if (reposRelpath == null && parentReposRelPath != null) {
                 reposRelpath = SVNFileUtil.createFilePath(parentReposRelPath, SVNFileUtil.getFileName(localAbsPath));
             }
 
             if (reposRelpath != null) {
                 /*
                  * repos_lock still uses the deprecated filesystem absolute path
                  * format
                  */
                repositoryLock = (SVNLock) myRepositoryLocks.get(SVNURL.parseURIEncoded("/" + reposRelpath.toString()).getPath());
             }
 
         }
 
         SVNStatus status = myWCContext.assembleStatus(localAbsPath, parentReposRootUrl, parentReposRelPath, pathKind, pathSpecial, getAll, repositoryLock);
         if (status != null && handler != null) {
             handler.handleStatus(status);
         }
 
     }
 
     private void sendUnversionedItem(File nodeAbsPath, SVNNodeKind pathKind, Collection<String> patterns, boolean noIgnore, ISVNStatusHandler handler) throws SVNException {
         boolean isIgnored = isIgnored(SVNFileUtil.getFileName(nodeAbsPath), patterns);
         boolean isExternal = isExternal(nodeAbsPath);
         SVNStatus status = myWCContext.assembleUnversioned(nodeAbsPath, pathKind, isIgnored);
         if (status != null) {
             if (isExternal) {
                 status.setContentsStatus(SVNStatusType.STATUS_EXTERNAL);
             }
             /*
              * We can have a tree conflict on an unversioned path, i.e. an
              * incoming delete on a locally deleted path during an update. Don't
              * ever ignore those!
              */
             if (status.isConflicted()) {
                 isIgnored = false;
             }
             if (handler != null && (noIgnore || !isIgnored || isExternal || status.getRemoteLock() != null)) {
                 handler.handleStatus(status);
             }
         }
 
     }
 
     private boolean isExternal(File nodeAbsPath) {
         if (!myExternalsMap.containsKey(nodeAbsPath)) {
             // check if path is external parent.
             for (Iterator paths = myExternalsMap.keySet().iterator(); paths.hasNext();) {
                 String externalPath = (String) paths.next();
                 if (externalPath.startsWith(nodeAbsPath + "/")) {
                     return true;
                 }
             }
             return false;
         }
         return true;
     }
 
     private boolean isIgnored(String name, Collection<String> patterns) {
         for (Iterator ps = patterns.iterator(); ps.hasNext();) {
             String pattern = (String) ps.next();
             if (DefaultSVNOptions.matches(pattern, name)) {
                 return true;
             }
         }
         return false;
     }
 
     private void handleDirEntry(File localAbsPath, SVNWCDbStatus status, SVNWCDbKind dbKind, SVNURL dirReposRootUrl, File dirReposRelPath, SVNNodeKind pathKind, boolean pathSpecial,
             Collection ignorePatterns, SVNDepth depth, boolean getAll, boolean noIgnore, ISVNStatusHandler handler) throws SVNException {
 
         assert (status != null);
 
         /* We are looking at a directory on-disk. */
         if (pathKind == SVNNodeKind.DIR && dbKind == SVNWCDbKind.Dir) {
             /*
              * Descend only if the subdirectory is a working copy directory
              * (which we've discovered because we got a THIS_DIR entry. And only
              * descend if DEPTH permits it, of course.
              */
             if (status != SVNWCDbStatus.Obstructed && status != SVNWCDbStatus.ObstructedAdd && status != SVNWCDbStatus.ObstructedDelete
                     && (depth == SVNDepth.UNKNOWN || depth == SVNDepth.IMMEDIATES || depth == SVNDepth.INFINITY)) {
                 getDirStatus(localAbsPath, dirReposRootUrl, dirReposRelPath, null, ignorePatterns, depth, getAll, noIgnore, false, handler);
             } else {
                 /*
                  * ENTRY is a child entry (file or parent stub). Or we have a
                  * directory entry but DEPTH is limiting our recursion.
                  */
                 sendStatusStructure(localAbsPath, dirReposRootUrl, dirReposRelPath, SVNNodeKind.DIR, false, getAll, handler);
             }
         } else {
             /* This is a file/symlink on-disk. */
             sendStatusStructure(localAbsPath, dirReposRootUrl, dirReposRelPath, pathKind, pathSpecial, getAll, handler);
         }
     }
 
     private void handleExternals(File localAbsPath, SVNDepth depth) throws SVNException {
         String externals = myWCContext.getProperty(localAbsPath, SVNProperty.EXTERNALS);
         if (externals != null) {
             if (SVNWCContext.isAncestor(myPath, localAbsPath)) {
                 storeExternals(localAbsPath, externals, externals, depth);
             }
             /*
              * Now, parse the thing, and copy the parsed results into our
              * "global" externals hash.
              */
             SVNExternal[] externalsInfo = SVNExternal.parseExternals(localAbsPath, externals);
             for (int i = 0; i < externalsInfo.length; i++) {
                 SVNExternal external = externalsInfo[i];
                 myExternalsMap.put(SVNFileUtil.createFilePath(localAbsPath, external.getPath()), external);
             }
         }
     }
 
     private void storeExternals(File localAbsPath, String oldValue, String newValue, SVNDepth depth) {
         myExternalsStore.addExternal(localAbsPath, oldValue, newValue);
         myExternalsStore.addDepth(localAbsPath, depth);
     }
 
     private Collection<String> collectIgnorePatterns(File localAbsPath, Collection<String> ignores) throws SVNException {
         /* ### assert we are passed a directory? */
         /* Then add any svn:ignore globs to the PATTERNS array. */
         final String localIgnores = myWCContext.getProperty(localAbsPath, SVNProperty.IGNORE);
         if (localIgnores != null) {
             final List<String> patterns = new ArrayList<String>();
             patterns.addAll(ignores);
             for (StringTokenizer tokens = new StringTokenizer(localIgnores, "\r\n"); tokens.hasMoreTokens();) {
                 String token = tokens.nextToken().trim();
                 if (token.length() > 0) {
                     patterns.add(token);
                 }
             }
             return patterns;
         }
         return ignores;
     }
 
     public void setRepositoryInfo(SVNURL repositoryRoot, HashMap<String, SVNLock> repositoryLocks) {
         myRepositoryRoot = repositoryRoot;
         myRepositoryLocks = repositoryLocks;
     }
 
     protected SVNStatus17 internalStatus(File localAbsPath) throws SVNException {
 
         SVNWCDbKind node_kind;
         File parent_repos_relpath;
         SVNURL parent_repos_root_url;
         SVNWCDbStatus node_status = null;
 
         assert (SVNWCDb.isAbsolute(localAbsPath));
 
         SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath));
 
         try {
             WCDbInfo info = myWCContext.getDb().readInfo(localAbsPath, InfoField.status, InfoField.kind);
             node_status = info.status;
             node_kind = info.kind;
         } catch (SVNException e) {
             if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                 throw e;
             }
             node_kind = SVNWCDbKind.Unknown;
         }
 
         if (node_status == SVNWCDbStatus.Obstructed || node_status == SVNWCDbStatus.ObstructedAdd || node_status == SVNWCDbStatus.ObstructedDelete || node_status == SVNWCDbStatus.NotPresent) {
             node_kind = SVNWCDbKind.Unknown;
         }
 
         if (node_kind != SVNWCDbKind.Unknown) {
             /* Check for hidden in the parent stub */
             boolean hidden = myWCContext.getDb().isNodeHidden(localAbsPath);
 
             if (hidden)
                 node_kind = SVNWCDbKind.Unknown;
         }
 
         if (node_kind == SVNWCDbKind.Unknown)
             return myWCContext.assembleUnversioned17(localAbsPath, kind, false /* is_ignored */);
 
         if (SVNFileUtil.getFileDir(localAbsPath) != null) {
 
             File parent_abspath = SVNFileUtil.getFileDir(localAbsPath);
 
             try {
                 WCDbInfo parent_info = myWCContext.getDb().readInfo(parent_abspath, InfoField.status, InfoField.reposRelPath, InfoField.reposRootUrl);
                 SVNWCDbStatus parent_status = parent_info.status;
                 parent_repos_relpath = parent_info.reposRelPath;
                 parent_repos_root_url = parent_info.reposRootUrl;
 
                 if (parent_repos_relpath == null && parent_status != SVNWCDbStatus.Added && parent_status != SVNWCDbStatus.Deleted) {
 
                     final WCDbRepositoryInfo baseRepos = myWCContext.getDb().scanBaseRepository(localAbsPath, RepositoryInfoField.relPath, RepositoryInfoField.rootUrl);
                     parent_repos_relpath = baseRepos.relPath;
                     parent_repos_root_url = baseRepos.rootUrl;
                 }
 
             } catch (SVNException e) {
                 if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND
                     || e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_WORKING_COPY
                     // || SVN_WC__ERR_IS_NOT_CURRENT_WC(err)
                 ) {
                     parent_repos_root_url = null;
                     parent_repos_relpath = null;
                 } else {
                     throw e;
                 }
             }
 
         } else {
             parent_repos_root_url = null;
             parent_repos_relpath = null;
         }
 
         return myWCContext.assembleStatus17(localAbsPath, parent_repos_root_url, parent_repos_relpath, kind, false, true /* get_all */, null /* repos_lock */);
 
     }
 
 }
