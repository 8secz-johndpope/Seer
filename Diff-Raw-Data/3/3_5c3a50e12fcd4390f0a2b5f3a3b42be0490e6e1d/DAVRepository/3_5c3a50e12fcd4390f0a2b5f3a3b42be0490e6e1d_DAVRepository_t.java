 /*
  * ====================================================================
  * Copyright (c) 2004 TMate Software Ltd.  All rights reserved.
  *
  * This software is licensed as described in the file COPYING, which
  * you should have received as part of this distribution.  The terms
  * are also available at http://tmate.org/svn/license.html.
  * If newer versions of this license are posted there, you may use a
  * newer version instead, at your option.
  * ====================================================================
  */
 
 package org.tmatesoft.svn.core.internal.io.dav;
 
 import java.io.OutputStream;
 import java.io.UnsupportedEncodingException;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVDateRevisionHandler;
 import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVEditorHandler;
 import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVFileRevisionHandler;
 import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVLocationsHandler;
 import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVLogHandler;
 import org.tmatesoft.svn.core.internal.io.dav.handlers.DAVProppatchHandler;
 import org.tmatesoft.svn.core.io.ISVNDirEntryHandler;
 import org.tmatesoft.svn.core.io.ISVNEditor;
 import org.tmatesoft.svn.core.io.ISVNFileRevisionHandler;
 import org.tmatesoft.svn.core.io.ISVNLocationEntryHandler;
 import org.tmatesoft.svn.core.io.ISVNLogEntryHandler;
 import org.tmatesoft.svn.core.io.ISVNReporterBaton;
 import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
 import org.tmatesoft.svn.core.io.SVNDirEntry;
 import org.tmatesoft.svn.core.io.SVNException;
 import org.tmatesoft.svn.core.io.SVNNodeKind;
 import org.tmatesoft.svn.core.io.SVNRepository;
 import org.tmatesoft.svn.core.io.SVNRepositoryLocation;
 import org.tmatesoft.svn.util.PathUtil;
 import org.tmatesoft.svn.util.TimeUtil;
 
 /**
  * @author Alexander Kitaev
  */
 class DAVRepository extends SVNRepository {
 
     private DAVConnection myConnection;
     
     protected DAVRepository(SVNRepositoryLocation location) {
         super(location);
     }
     
     public void testConnection() throws SVNException {
         try {
             openConnection();
         } finally {
             closeConnection();
         }
     }
     
     public long getLatestRevision() throws SVNException {
         DAVBaselineInfo info = null;
         try {
             openConnection();
             info = DAVUtil.getBaselineInfo(myConnection, getLocationPath(), -1, false, true, info);
         } finally {
             closeConnection();
         }
         if (info == null) {
             throw new SVNException("can't get baseline information for " + getLocationPath());
         }
         return info.revision;
     }
 
     public long getDatedRevision(Date date) throws SVNException {
     	date = date == null ? new Date(System.currentTimeMillis()) : date;
 		DAVDateRevisionHandler handler = new DAVDateRevisionHandler();
 		byte[] request = convertToBytes(DAVDateRevisionHandler.generateDateRevisionRequest(null, date));
     	try {
     		openConnection();
 			myConnection.doReport(getLocationPath(), request, handler);
     	} finally {
     		closeConnection();
     	}
     	return handler.getRevisionNumber();
     }
 
     public SVNNodeKind checkPath(String path, long revision) throws SVNException {
         DAVBaselineInfo info = null;
         SVNNodeKind kind = SVNNodeKind.NONE;
         try {
             openConnection();
             path = getFullPath(path);
             info = DAVUtil.getBaselineInfo(myConnection, path, revision, true, false, info);
             kind = info.isDirectory ? SVNNodeKind.DIR : SVNNodeKind.FILE;
         } catch (SVNException e) {            
         } finally {
             closeConnection();
         }
         return kind;
     }
     
     public Map getRevisionProperties(long revision, Map properties) throws SVNException {
         properties = properties == null ? new HashMap() : properties;
         try {
             openConnection();
             DAVResponse source = DAVUtil.getBaselineProperties(myConnection, getLocationPath(), revision, null);
             properties = DAVUtil.filterProperties(source, properties);
         } finally {
             closeConnection();
         }
         return properties;
     }
     
     public String getRevisionPropertyValue(long revision, String propertyName) throws SVNException {
         Map properties = getRevisionProperties(revision, null);
         return (String) properties.get(propertyName);
     }
 
     public long getFile(String path, long revision, final Map properties, OutputStream contents) throws SVNException {
         long fileRevision = revision;
         try {
             openConnection();
             path = getFullPath(path);
             if (revision != -2) {
                 DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, path, revision, false, true, null);
                 path = PathUtil.append(info.baselineBase, info.baselinePath);
                 fileRevision = info.revision; 
             }
             if (properties != null) {
             	myConnection.doPropfind(path, 0, null, null, new IDAVResponseHandler() {
 					public void handleDAVResponse(DAVResponse response) {
 						DAVUtil.filterProperties(response, properties);
 					}
             	});
             }
             if (contents != null) {
                 myConnection.doGet(path, contents);
             }
         } finally {
             closeConnection();
         }
         return fileRevision;
     }
 
     public long getDir(String path, long revision, final Map properties, final ISVNDirEntryHandler handler) throws SVNException {
         long dirRevision = revision;
         DAVElement[] entryProperties = {DAVElement.VERSION_NAME, DAVElement.GET_CONTENT_LENGTH, 
                 DAVElement.RESOURCE_TYPE, DAVElement.CREATION_DATE, DAVElement.CREATOR_DISPLAY_NAME};
         try {
             openConnection();
             path = getFullPath(path);
             if (revision != -2) {
                 DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, path, revision, false, true, null);
                 path = PathUtil.append(info.baselineBase, info.baselinePath);
                 dirRevision = info.revision; 
             }
             if (handler != null) {
                 final String parentPath = PathUtil.removeTrailingSlash(path);
                 myConnection.doPropfind(path, 1, null, entryProperties, new IDAVResponseHandler() {
                     public void handleDAVResponse(DAVResponse child) {
                         String href = PathUtil.removeTrailingSlash(child.getHref());
                         href = PathUtil.decode(href);
                         if (href.equals(PathUtil.decode(parentPath))) {
                             return;
                         }
                         // build direntry
                         String name = PathUtil.tail(href);
                         SVNNodeKind kind = SVNNodeKind.FILE;
                         Object revisionStr = child.getPropertyValue(DAVElement.VERSION_NAME);
                         long lastRevision = Long.parseLong(revisionStr.toString());
                         String sizeStr = (String) child.getPropertyValue(DAVElement.GET_CONTENT_LENGTH);
                         long size = sizeStr == null ? 0 : Long.parseLong(sizeStr);
                         if (child.getPropertyValue(DAVElement.RESOURCE_TYPE) == DAVElement.COLLECTION) {
                             kind = SVNNodeKind.DIR;
                         }
                         String author = (String) child.getPropertyValue(DAVElement.CREATOR_DISPLAY_NAME);
                         String dateStr = (String) child.getPropertyValue(DAVElement.CREATION_DATE);
                         Date date = TimeUtil.parseDate(dateStr);
                         boolean hasProperties = false;
                         for(Iterator props = child.properties(); props.hasNext();) {
                             DAVElement property = (DAVElement) props.next();
                             if (DAVElement.SVN_CUSTOM_PROPERTY_NAMESPACE.equals(property.getNamespace()) || 
                                     DAVElement.SVN_SVN_PROPERTY_NAMESPACE.equals(property)) {
                                 hasProperties = true;
                                 break;
                             }
                         }
                         SVNDirEntry dirEntry = new SVNDirEntry(name, kind, size, hasProperties, lastRevision, date, author);
                         
                         handler.handleDirEntry(dirEntry);
                     }
                 });
             }
             if (properties != null) {
             	myConnection.doPropfind(path, 0, null, null, new IDAVResponseHandler() {
 					public void handleDAVResponse(DAVResponse response) {
 						DAVUtil.filterProperties(response, properties);
 					}
             	});
             }
         } finally {
             closeConnection();
         }
         return dirRevision;
     }
 
     public int getFileRevisions(String path, long startRevision, long endRevision, ISVNFileRevisionHandler handler) throws SVNException {
 		String bcPath = getLocation().getPath();
 		try {
             openConnection();
             path = getFullPath(path);
             path = path.substring(getRepositoryRoot().length());
             DAVFileRevisionHandler davHandler = new DAVFileRevisionHandler(handler);
             byte[] request = convertToBytes(DAVFileRevisionHandler.generateFileRevisionsRequest(null, startRevision, endRevision, path));
 			long revision = -1;
 			if (isValidRevision(startRevision) && isValidRevision(endRevision)) {
 				revision = Math.max(startRevision, endRevision);				
 			}
 			DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, bcPath, revision, false, false, null);
 			bcPath = PathUtil.append(info.baselineBase, info.baselinePath);
 			myConnection.doReport(bcPath, request, davHandler);
             return davHandler.getEntriesCount();
 		} finally {
 			closeConnection();
 		}
     }
     
     public int log(String[] targetPaths, long startRevision, long endRevision,
             boolean changedPath, boolean strictNode, ISVNLogEntryHandler handler) throws SVNException {
         if (targetPaths == null || targetPaths.length == 0) {
             return 0;
         }
 		String path = getLocation().getPath();
         byte[] request = convertToBytes(DAVLogHandler.generateLogRequest(null, startRevision, endRevision,
         		changedPath, strictNode, targetPaths));
         DAVLogHandler davHandler = null;
 		try {
 			openConnection();
 			String[] fullPaths = new String[targetPaths.length];
 			for (int i = 0; i < targetPaths.length; i++) {
 				fullPaths[i] = getFullPath(targetPaths[i]);
 			}
 			path = PathUtil.getCommonRoot(fullPaths);
			if (!path.startsWith("/")) {
				path = "/".concat(path);
			}
             davHandler = new DAVLogHandler(handler); 
 			long revision = -1;
 			if (isValidRevision(startRevision) && isValidRevision(endRevision)) {
 				revision = Math.max(startRevision, endRevision);				
 			}
             DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, path, revision, false, false, null);
             path = PathUtil.append(info.baselineBase, info.baselinePath);
             
             myConnection.doReport(path, request, davHandler);
 		} finally {
 			closeConnection();
 		}
         if (davHandler != null) {
             return davHandler.getEntriesCount();
         }
         return -1;
     }
     
     private void openConnection() throws SVNException {
         lock();
         if (myConnection == null) {
             myConnection = new DAVConnection(getLocation());
         }
         myConnection.open(this);
     }
 
     private void closeConnection() {
         if (myConnection != null) {
             myConnection.close();
         }
         unlock();
     }
     
     public int getLocations(String path, long pegRevision, long[] revisions, ISVNLocationEntryHandler handler) throws SVNException {
         try {
             openConnection();
             String root = getLocation().getPath();
             if (path.startsWith("/")) {
                 path = PathUtil.removeLeadingSlash(path);
                 root = getLocationPath();
                 path = path.substring(root.length());
             }
             byte[] request = convertToBytes(DAVLocationsHandler.generateLocationsRequest(null, path, pegRevision, revisions));
             
             DAVLocationsHandler davHandler = new DAVLocationsHandler(handler);
             
             DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, root, pegRevision, false, false, null);            
             
             path = PathUtil.append(info.baselineBase, info.baselinePath);
             myConnection.doReport(path, request, davHandler);
             
             return davHandler.getEntriesCount();
         } finally {
             closeConnection();
         }
     }
 
     public void update(long revision, String target, boolean recursive, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
         byte[] request = convertToBytes(DAVEditorHandler.generateEditorRequest(null, getLocation().toString(), revision, target, null, recursive, false, false, reporter));
         try {
             openConnection();
             DAVEditorHandler handler = new DAVEditorHandler(editor, true);
 
             DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, getLocation().getPath(), revision, false, false, null);
             String path = PathUtil.append(info.baselineBase, info.baselinePath);
             DAVResponse response = DAVUtil.getResourceProperties(myConnection, path, null, DAVElement.STARTING_PROPERTIES, true);
             if (response != null) {
             	path = (String) response.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
             	myConnection.doReport(path, request, handler);
             }
         } finally {
             closeConnection();
         }
     }
 
     public void update(String url, long revision, String target, boolean recursive, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
         url = getCanonicalURL(url);
         if (url == null) {
             throw new SVNException(url + ": not valid URL");
         }
         byte[] request = convertToBytes(DAVEditorHandler.generateEditorRequest(null, getLocation().toString(), revision, target, url, recursive, true, false, reporter));
         try {
             openConnection();
             DAVEditorHandler handler = new DAVEditorHandler(editor, true);
 
             DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, getLocation().getPath(), revision, false, false, null);
             String path = PathUtil.append(info.baselineBase, info.baselinePath);
             DAVResponse response = DAVUtil.getResourceProperties(myConnection, path, null, DAVElement.STARTING_PROPERTIES, false);
             path = (String) response.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
 
             myConnection.doReport(path, request, handler);
         } finally {
             closeConnection();
         }
     }
 
     public void diff(String url, long revision, String target, boolean ignoreAncestry, boolean recursive, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
         url = getCanonicalURL(url);
         if (url == null) {
             throw new SVNException(url + ": not valid URL");
         }
         byte[] request = convertToBytes(DAVEditorHandler.generateEditorRequest(null, getLocation().toString(), revision, target, url, recursive, ignoreAncestry, false, reporter));
         try {
             openConnection();
             DAVEditorHandler handler = new DAVEditorHandler(editor, true);
 
             DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, getLocation().getPath(), revision, false, false, null);
             String path = PathUtil.append(info.baselineBase, info.baselinePath);
             DAVResponse response = DAVUtil.getResourceProperties(myConnection, path, null, DAVElement.STARTING_PROPERTIES, false);
             path = (String) response.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
 
             myConnection.doReport(path, request, handler);
         } finally {
             closeConnection();
         }
     }
 
     public void status(long revision, String target, boolean recursive, ISVNReporterBaton reporter, ISVNEditor editor) throws SVNException {
         byte[] request = convertToBytes(DAVEditorHandler.generateEditorRequest(null, getLocation().toString(), revision, target, null, recursive, false, false, reporter));
         try {
             openConnection();
             DAVEditorHandler handler = new DAVEditorHandler(editor, false);
 
             DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, getLocation().getPath(), revision, false, false, null);
             String path = PathUtil.append(info.baselineBase, info.baselinePath);
         	DAVResponse response = DAVUtil.getResourceProperties(myConnection, path, null, DAVElement.STARTING_PROPERTIES, true);
         	if (response != null) {
         		path = (String) response.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
         		myConnection.doReport(path, request, handler);
         	}
         } finally {
             closeConnection();
         }
     }
 
     public void setRevisionPropertyValue(long revision, String propertyName, String propertyValue) throws SVNException {
         assertValidRevision(revision);
 
         byte[] request = convertToBytes(DAVProppatchHandler.generatePropertyRequest(null, propertyName, propertyValue));
         try {
             openConnection();
             // 1. get vcc for root.
             
             DAVBaselineInfo info = DAVUtil.getBaselineInfo(myConnection, getLocation().getPath(), revision, false, false, null);
             String path = PathUtil.append(info.baselineBase, info.baselinePath);
 
             DAVResponse response = DAVUtil.getResourceProperties(myConnection, path, null, DAVElement.STARTING_PROPERTIES, false);
             path = (String) response.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
             
             // 2. get href from specific vcc with using "label"
             final String[] blPath = new String[1];
             DAVElement[] props = new DAVElement[] {DAVElement.AUTO_VERSION};
             myConnection.doPropfind(path, 0, Long.toString(revision), props, new IDAVResponseHandler() {
                 public void handleDAVResponse(DAVResponse r) {
                     blPath[0] = r.getHref();
                     if (r.getPropertyValue(DAVElement.AUTO_VERSION) != null) {
                         // there are problems with repository
                         blPath[0] = null;
                     }
                 }   
             });
             if (blPath[0] == null) {
                 throw new SVNException("repository auto-versioning is enabled, can't put unversioned property");
             }
             myConnection.doProppatch(blPath[0], request, null);
         } finally {
             closeConnection();
         }
     }
 
     public ISVNEditor getCommitEditor(String message, ISVNWorkspaceMediator mediator) throws SVNException {
         openConnection();
         ISVNEditor editor = new DAVCommitEditor(this, myConnection, message, mediator, new Runnable() {
             public void run() {
                 closeConnection();
             }
         });
         return editor;
 	}
     
     private String getFullPath(String path) {
         if (path != null && path.startsWith("/")) {
             // assume it is full path in repository
             // prepend root only.            
             return PathUtil.append(getRepositoryRoot(), path);
         }
         // it was a relative path relative to location path.
         // decode??
         path = PathUtil.append(getLocation().getPath(), path);
         if (path.charAt(0) != '/') {
             path = '/' + path;            
         }
         return path;
     }
     
     private static byte[] convertToBytes(StringBuffer buffer) {
     	if (buffer == null) {
     		return new byte[0];
     	}
     	try {
 			return buffer.toString().getBytes("UTF-8");
 		} catch (UnsupportedEncodingException e) {
 		}
 		return new byte[0];
     }
     
     private String getLocationPath() {
     	return DAVUtil.getCanonicalPath(getLocation().getPath(), null).toString();
     }
 
     void updateCredentials(String uuid, String root) {
         root = root == null ? getRepositoryRoot() : root;
         uuid = uuid == null ? getRepositoryUUID() : uuid;
         setRepositoryCredentials(uuid, root);
     }
 
 }
 
