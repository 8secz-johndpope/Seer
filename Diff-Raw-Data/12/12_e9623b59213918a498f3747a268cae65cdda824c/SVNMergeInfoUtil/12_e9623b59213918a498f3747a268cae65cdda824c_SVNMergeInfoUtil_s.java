 /*
  * ====================================================================
  * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
  *
  * This software is licensed as described in the file COPYING, which
  * you should have received as part of this distribution.  The terms
  * are also available at http://svnkit.com/license.html.
  * If newer versions of this license are posted there, you may use a
  * newer version instead, at your option.
  * ====================================================================
  */
 package org.tmatesoft.svn.core.internal.util;
 
 import java.io.File;
 import java.io.OutputStream;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import org.tmatesoft.svn.core.internal.util.SVNHashMap;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 
 import org.tmatesoft.svn.core.SVNCommitInfo;
 import org.tmatesoft.svn.core.SVNErrorCode;
 import org.tmatesoft.svn.core.SVNErrorMessage;
 import org.tmatesoft.svn.core.SVNException;
 import org.tmatesoft.svn.core.SVNMergeRange;
 import org.tmatesoft.svn.core.SVNMergeRangeList;
 import org.tmatesoft.svn.core.SVNProperty;
 import org.tmatesoft.svn.core.SVNPropertyValue;
 import org.tmatesoft.svn.core.internal.wc.ISVNCommitPathHandler;
 import org.tmatesoft.svn.core.internal.wc.SVNCommitUtil;
 import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
 import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
 import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
 import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
 import org.tmatesoft.svn.core.io.ISVNEditor;
 import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
 
 /**
  * @version 1.1.2
  * @author  TMate Software Ltd.
  */
 public class SVNMergeInfoUtil {
 
 	public static Map elideMergeInfoCatalog(Map mergeInfoCatalog) throws SVNException {
 	    Map adjustedMergeInfoCatalog = new TreeMap();
 	    for (Iterator pathsIter = mergeInfoCatalog.keySet().iterator(); pathsIter.hasNext();) {
 	        String path = (String) pathsIter.next();
 	        String adjustedPath = path;
 	        if (path.startsWith("/")) {
 	            adjustedPath = path.substring(1);
 	        }
 	        adjustedMergeInfoCatalog.put(adjustedPath, mergeInfoCatalog.get(path));
 	    }
 	    mergeInfoCatalog = adjustedMergeInfoCatalog;
 	    ElideMergeInfoCatalogHandler handler = new ElideMergeInfoCatalogHandler(mergeInfoCatalog);
 	    ElideMergeInfoEditor editor = new ElideMergeInfoEditor(mergeInfoCatalog);
 	    SVNCommitUtil.driveCommitEditor(handler, mergeInfoCatalog.keySet(), editor, -1);
 	    List elidablePaths = handler.getElidablePaths();
 	    for (Iterator elidablePathsIter = elidablePaths.iterator(); elidablePathsIter.hasNext();) {
             String elidablePath = (String) elidablePathsIter.next();
             mergeInfoCatalog.remove(elidablePath);
         }
         
 	    adjustedMergeInfoCatalog = new TreeMap();
         for (Iterator pathsIter = mergeInfoCatalog.keySet().iterator(); pathsIter.hasNext();) {
             String path = (String) pathsIter.next();
             String adjustedPath = path;
             if (!path.startsWith("/")) {
                 adjustedPath = "/" + adjustedPath;
             }
             adjustedMergeInfoCatalog.put(adjustedPath, mergeInfoCatalog.get(path));
         }
 	    return adjustedMergeInfoCatalog;
 	}
 	
     public static Map adjustMergeInfoSourcePaths(Map mergeInfo, String walkPath, Map wcMergeInfo) {
         mergeInfo = mergeInfo == null ? new TreeMap() : mergeInfo;
 		for (Iterator paths = wcMergeInfo.keySet().iterator(); paths.hasNext();) {
             String srcMergePath = (String) paths.next();
             SVNMergeRangeList rangeList = (SVNMergeRangeList) wcMergeInfo.get(srcMergePath); 
             mergeInfo.put(SVNPathUtil.getAbsolutePath(SVNPathUtil.append(srcMergePath, walkPath)), rangeList);
         }
 		return mergeInfo;
 	}
 	
 	public static boolean removeEmptyRangeLists(Map mergeInfo) {
 		boolean removedSomeRanges = false;
 		if (mergeInfo != null) {
 			for (Iterator mergeInfoIter = mergeInfo.entrySet().iterator(); mergeInfoIter.hasNext();) {
 				Map.Entry mergeInfoEntry = (Map.Entry) mergeInfoIter.next();
 				SVNMergeRangeList rangeList = (SVNMergeRangeList) mergeInfoEntry.getValue();
 				if (rangeList.isEmpty()) {
 					mergeInfoIter.remove();
 					removedSomeRanges = true;
 				}
 			}
 		}
 		return removedSomeRanges;
 	}
 	
     public static Map mergeMergeInfos(Map originalSrcsToRangeLists, Map changedSrcsToRangeLists) throws SVNException {
         originalSrcsToRangeLists = originalSrcsToRangeLists == null ? new TreeMap() : originalSrcsToRangeLists;
         changedSrcsToRangeLists = changedSrcsToRangeLists == null ? Collections.EMPTY_MAP : changedSrcsToRangeLists;
         String[] paths1 = (String[]) originalSrcsToRangeLists.keySet().toArray(new String[originalSrcsToRangeLists.size()]);
         String[] paths2 = (String[]) changedSrcsToRangeLists.keySet().toArray(new String[changedSrcsToRangeLists.size()]);
         int i = 0;
         int j = 0;
         while (i < paths1.length && j < paths2.length) {
             String path1 = paths1[i];
             String path2 = paths2[j];
             int res = path1.compareTo(path2);
             if (res == 0) {
                 SVNMergeRangeList rangeList1 = (SVNMergeRangeList) originalSrcsToRangeLists.get(path1);
                 SVNMergeRangeList rangeList2 = (SVNMergeRangeList) changedSrcsToRangeLists.get(path2);
                 rangeList1 = rangeList1.merge(rangeList2);
                 originalSrcsToRangeLists.put(path1, rangeList1);
                 i++;
                 j++;
             } else if (res < 0) {
                 i++;
             } else {
                 originalSrcsToRangeLists.put(path2, changedSrcsToRangeLists.get(path2));
                 j++;
             }
         }
         
         for (; j < paths2.length; j++) {
             String path = paths2[j];
             originalSrcsToRangeLists.put(path, changedSrcsToRangeLists.get(path));
         }
         return originalSrcsToRangeLists;
     }
     
     public static String combineMergeInfoProperties(String propValue1, String propValue2) throws SVNException {
         Map srcsToRanges1 = parseMergeInfo(new StringBuffer(propValue1), null);
         Map srcsToRanges2 = parseMergeInfo(new StringBuffer(propValue2), null);
         srcsToRanges1 = mergeMergeInfos(srcsToRanges1, srcsToRanges2);
         return formatMergeInfoToString(srcsToRanges1);
     }
     
     public static String combineForkedMergeInfoProperties(String fromPropValue, String workingPropValue, 
             String toPropValue) throws SVNException {
         Map leftDeleted = new TreeMap();
         Map leftAdded = new TreeMap();
         Map fromMergeInfo = parseMergeInfo(new StringBuffer(fromPropValue), null);
         diffMergeInfoProperties(leftDeleted, leftAdded, null, fromMergeInfo, workingPropValue, null);
         
         Map rightDeleted = new TreeMap();
         Map rightAdded = new TreeMap();
         diffMergeInfoProperties(rightDeleted, rightAdded, fromPropValue, null, toPropValue, null);
         leftDeleted = mergeMergeInfos(leftDeleted, rightDeleted);
         leftAdded = mergeMergeInfos(leftAdded, rightAdded);
         fromMergeInfo = mergeMergeInfos(fromMergeInfo, leftAdded);
         Map result = removeMergeInfo(leftDeleted, fromMergeInfo);
         return formatMergeInfoToString(result);
     }
     
    public static void diffMergeInfoProperties(Map deleted, Map added, String fromPropValue, Map fromMergeInfo, 
            String toPropValue, Map toMergeInfo) throws SVNException {
        if (fromPropValue.equals(toPropValue)) {
             return;
         } 
        fromMergeInfo = fromMergeInfo == null ? parseMergeInfo(new StringBuffer(fromPropValue), null) 
                                              : fromMergeInfo;
        toMergeInfo = toMergeInfo == null ? parseMergeInfo(new StringBuffer(toPropValue), null) 
                                          : toMergeInfo;
         diffMergeInfo(deleted, added, fromMergeInfo, toMergeInfo, false);
     }
     
     public static void diffMergeInfo(Map deleted, Map added, Map from, Map to, 
             boolean considerInheritance) {
         from = from == null ? Collections.EMPTY_MAP : from;
         to = to == null ? Collections.EMPTY_MAP : to;
         if (!from.isEmpty() && to.isEmpty()) {
             dupMergeInfo(from, deleted);
         } else if (from.isEmpty() && !to.isEmpty()) {
             dupMergeInfo(to, added);
         } else if (!from.isEmpty() && !to.isEmpty()) {
             walkMergeInfoHashForDiff(deleted, added, from, to, considerInheritance);
         }
     }
     
     public static Map dupMergeInfo(Map srcsToRangeLists, Map target) {
         if (srcsToRangeLists == null) {
             return null;
         }
         target = target == null ? new TreeMap() : target;
         for (Iterator paths = srcsToRangeLists.keySet().iterator(); paths.hasNext();) {
             String path = (String) paths.next();
             SVNMergeRangeList rangeList = (SVNMergeRangeList) srcsToRangeLists.get(path);
             target.put(path, rangeList.dup());
         }
         return target;
     }
     
     public static Map parseMergeInfo(StringBuffer mergeInfo, Map srcPathsToRangeLists) throws SVNException {
         srcPathsToRangeLists = srcPathsToRangeLists == null ? new TreeMap() : srcPathsToRangeLists;
         if (mergeInfo.length() == 0) {
             return srcPathsToRangeLists;
         }
 
         while (mergeInfo.length() > 0) {
             int ind = mergeInfo.indexOf(":");
             if (ind == -1) {
                 SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                         "Pathname not terminated by ':'");
                 SVNErrorManager.error(err);
             }
             String path = mergeInfo.substring(0, ind);
             mergeInfo = mergeInfo.delete(0, ind + 1);
             SVNMergeRange[] ranges = parseRevisionList(mergeInfo, path);
             if (mergeInfo.length() != 0 && mergeInfo.charAt(0) != '\n') {
                 SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                         "Could not find end of line in range list line in ''{0}''", mergeInfo);
                 SVNErrorManager.error(err);
             }
             if (mergeInfo.length() > 0) {
                 mergeInfo = mergeInfo.deleteCharAt(0);
             }
             Arrays.sort(ranges);
             srcPathsToRangeLists.put(path, new SVNMergeRangeList(ranges));
         }
         
         return srcPathsToRangeLists;
     }
 
     /**
      * Each element of the resultant array is formed like this:
      * %s:%ld-%ld,.. where the first %s is a merge src path 
      * and %ld-%ld is startRev-endRev merge range.
      */
     public static String[] formatMergeInfoToArray(Map srcsToRangeLists) {
         srcsToRangeLists = srcsToRangeLists == null ? Collections.EMPTY_MAP : srcsToRangeLists;
         String[] pathRanges = new String[srcsToRangeLists.size()];
         int k = 0;
         for (Iterator paths = srcsToRangeLists.keySet().iterator(); paths.hasNext();) {
             String path = (String) paths.next();
             SVNMergeRangeList rangeList = (SVNMergeRangeList) srcsToRangeLists.get(path);
             String output = path + ':' + rangeList;  
             pathRanges[k++] = output;
         }
         return pathRanges;
     }
 
     public static String formatMergeInfoToString(Map srcsToRangeLists) {
         String[] infosArray = formatMergeInfoToArray(srcsToRangeLists);
         String result = "";
         for (int i = 0; i < infosArray.length; i++) {
             result += infosArray[i];
             if (i < infosArray.length - 1) {
                 result += '\n';
             }
         }
         return result;
     }
 
     public static boolean shouldElideMergeInfo(Map parentMergeInfo, Map childMergeInfo, String pathSuffix) {
         boolean elides = false;
         if (childMergeInfo != null) {
             if (childMergeInfo.isEmpty()) {
                 if (parentMergeInfo == null || parentMergeInfo.isEmpty()) {
                     elides = true;
                 }
             } else if (!(parentMergeInfo == null || parentMergeInfo.isEmpty())) {
                 Map pathTweakedMergeInfo = parentMergeInfo;
                 if (pathSuffix != null) {
                     pathTweakedMergeInfo = new TreeMap();
                     for (Iterator paths = parentMergeInfo.keySet().iterator(); paths.hasNext();) {
                         String mergeSrcPath = (String) paths.next();
                         pathTweakedMergeInfo.put(SVNPathUtil.getAbsolutePath(SVNPathUtil.append(mergeSrcPath, 
                                 pathSuffix)), parentMergeInfo.get(mergeSrcPath));
                     }
                 } 
                 elides = mergeInfoEquals(pathTweakedMergeInfo, childMergeInfo, true);
             }
         }
         return elides;
     }
     
     public static void elideMergeInfo(Map parentMergeInfo, Map childMergeInfo, File path, 
             String pathSuffix, SVNWCAccess access) throws SVNException {
         boolean elides = shouldElideMergeInfo(parentMergeInfo, childMergeInfo, pathSuffix);
         if (elides) {
             SVNPropertiesManager.setProperty(access, path, SVNProperty.MERGE_INFO, null, true);
         }
     }
     
     public static boolean mergeInfoEquals(Map mergeInfo1, Map mergeInfo2, 
             boolean considerInheritance) {
         mergeInfo1 = mergeInfo1 == null ? Collections.EMPTY_MAP : mergeInfo1;
         mergeInfo2 = mergeInfo2 == null ? Collections.EMPTY_MAP : mergeInfo2;
         
         if (mergeInfo1.size() == mergeInfo2.size()) {
             Map deleted = new SVNHashMap();
             Map added = new SVNHashMap();
             diffMergeInfo(deleted, added, mergeInfo1, mergeInfo2, considerInheritance);
             return deleted.isEmpty() && added.isEmpty();
         }
         return false;
     }
     
     public static String[] findMergeSources(long revision, Map mergeInfo) {
         LinkedList mergeSources = new LinkedList();
         for (Iterator paths = mergeInfo.keySet().iterator(); paths.hasNext();) {
             String path = (String) paths.next();
             SVNMergeRangeList rangeList = (SVNMergeRangeList) mergeInfo.get(path);
             if (rangeList.includes(revision)) {
                 mergeSources.add(path);
             }
         }
         return (String[]) mergeSources.toArray(new String[mergeSources.size()]);
     }
     
     public static Map getInheritableMergeInfo(Map mergeInfo, String path, long startRev, long endRev) {
         Map inheritableMergeInfo = new TreeMap();
         if (mergeInfo != null) {
             for (Iterator paths = mergeInfo.keySet().iterator(); paths.hasNext();) {
                 String mergeSrcPath = (String) paths.next();
                 SVNMergeRangeList rangeList = (SVNMergeRangeList) mergeInfo.get(mergeSrcPath);
                 SVNMergeRangeList inheritableRangeList = null;
                 if (path == null || path.equals(mergeSrcPath)) {
                     inheritableRangeList = rangeList.getInheritableRangeList(startRev, endRev);
                 } else {
                     inheritableRangeList = rangeList.dup();
                 }
                 inheritableMergeInfo.put(mergeSrcPath, inheritableRangeList);
             }
         }
         return inheritableMergeInfo;
     }
     
     public static Map removeMergeInfo(Map eraser, Map whiteBoard) {
         Map mergeInfo = new TreeMap();
         walkMergeInfoHashForDiff(mergeInfo, null, whiteBoard, eraser, true);
         return mergeInfo;
     }
     
     public static Map intersectMergeInfo(Map mergeInfo1, Map mergeInfo2) {
         Map mergeInfo = new TreeMap();
         for (Iterator pathsIter = mergeInfo1.keySet().iterator(); pathsIter.hasNext();) {
             String path = (String) pathsIter.next();
             SVNMergeRangeList rangeList1 = (SVNMergeRangeList) mergeInfo1.get(path);
             SVNMergeRangeList rangeList2 = (SVNMergeRangeList) mergeInfo2.get(path);
             if (rangeList2 != null) {
                 rangeList2 = rangeList2.intersect(rangeList1);
                 if (!rangeList2.isEmpty()) {
                     mergeInfo.put(path, rangeList2);
                 }
             }
         }
         return mergeInfo;
     }
     
     private static SVNMergeRange[] parseRevisionList(StringBuffer mergeInfo, String path) throws SVNException {
         Collection ranges = new LinkedList();
         while (mergeInfo.length() > 0 && mergeInfo.charAt(0) != '\n' && 
                 Character.isWhitespace(mergeInfo.charAt(0))) {
             mergeInfo = mergeInfo.deleteCharAt(0);
         }
         if (mergeInfo.length() == 0 || mergeInfo.charAt(0) == '\n') {
             SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                     "Mergeinfo for ''{0}'' maps to an empty revision range", path);
             SVNErrorManager.error(err);
         }
         
         SVNMergeRange lastRange = null;
         while (mergeInfo.length() > 0 && mergeInfo.charAt(0) != '\n') {
             long startRev = parseRevision(mergeInfo);
             if (mergeInfo.length() > 0 && mergeInfo.charAt(0) != '\n' && 
                 mergeInfo.charAt(0) != '-' && mergeInfo.charAt(0) != ',' && 
                 mergeInfo.charAt(0) != '*') {
                 SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                         "Invalid character ''{0}'' found in revision list", 
                         new Character(mergeInfo.charAt(0)));
                 SVNErrorManager.error(err);
             }
             
             SVNMergeRange range = new SVNMergeRange(startRev - 1, startRev, true);
             if (mergeInfo.length() > 0 && mergeInfo.charAt(0) == '-') {
                 mergeInfo = mergeInfo.deleteCharAt(0);
                 long endRev = parseRevision(mergeInfo);
                 if (startRev > endRev) {
                     SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                             "Unable to parse reversed revision range ''{0}-{1}''",
                             new Object[] { new Long(startRev), new Long(endRev) });
                     SVNErrorManager.error(err);
                 } else if (startRev == endRev) {
                     SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                             "Unable to parse revision range ''{0}-{1}'' with same start and end revisions",
                             new Object[] { new Long(startRev), new Long(endRev) });
                     SVNErrorManager.error(err);
                 }
                 range.setEndRevision(endRev);
             }
             
             if (mergeInfo.length() == 0 || mergeInfo.charAt(0) == '\n') {
                 lastRange = combineWithAdjacentLastRange(ranges, lastRange, range, false);
                 return (SVNMergeRange[]) ranges.toArray(new SVNMergeRange[ranges.size()]);
             } else if (mergeInfo.length() > 0 && mergeInfo.charAt(0) == ',') {
                 lastRange = combineWithAdjacentLastRange(ranges, lastRange, range, false);
                 mergeInfo = mergeInfo.deleteCharAt(0);
             } else if (mergeInfo.length() > 0 && mergeInfo.charAt(0) == '*') {
                 range.setInheritable(false);
                 mergeInfo = mergeInfo.deleteCharAt(0);
                 if (mergeInfo.length() == 0 || mergeInfo.charAt(0) == ',' || 
                         mergeInfo.charAt(0) == '\n') {
                     lastRange = combineWithAdjacentLastRange(ranges, lastRange, range, false);
                     if (mergeInfo.length() > 0 && mergeInfo.charAt(0) == ',') {
                         mergeInfo = mergeInfo.deleteCharAt(0);
                     } else {
                         return (SVNMergeRange[]) ranges.toArray(new SVNMergeRange[ranges.size()]);
                     }
                 } else {
                     SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                                                                  "Invalid character ''{0}'' found in range list", 
                                                                  mergeInfo.length() > 0 ?  mergeInfo.charAt(0) + "" : "");
                     SVNErrorManager.error(err);
                 }
             } else {
                 SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                                                              "Invalid character ''{0}'' found in range list", 
                                                              mergeInfo.length() > 0 ?  mergeInfo.charAt(0) + "" : "");
                 SVNErrorManager.error(err);
             }
         }
         
         if (mergeInfo.length() == 0 || mergeInfo.charAt(0) != '\n' ) {
             SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, "Range list parsing ended before hitting newline");
             SVNErrorManager.error(err);
         }
         
         return (SVNMergeRange[]) ranges.toArray(new SVNMergeRange[ranges.size()]);
     }
 
     private static long parseRevision(StringBuffer mergeInfo) throws SVNException {
         int ind = 0;
         while (ind < mergeInfo.length() && Character.isDigit(mergeInfo.charAt(ind))) {
             ind++;
         }
         
         if (ind == 0) {
             SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REVISION_NUMBER_PARSE_ERROR, 
                                                          "Invalid revision number found parsing ''{0}''", 
                                                          mergeInfo.toString());
             SVNErrorManager.error(err);
         }
         
         String numberStr = mergeInfo.substring(0, ind);
         long rev = -1;
         try {
             rev = Long.parseLong(numberStr);
         } catch (NumberFormatException e) {
             SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REVISION_NUMBER_PARSE_ERROR, 
                                                          "Invalid revision number found parsing ''{0}''", 
                                                          mergeInfo.toString());
             SVNErrorManager.error(err);
         }
 
         if (rev < 0) {
             SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REVISION_NUMBER_PARSE_ERROR, 
                                                          "Negative revision number found parsing ''{0}''", 
                                                          mergeInfo.toString());
             SVNErrorManager.error(err);
         }
         
         mergeInfo = mergeInfo.delete(0, ind);
         return rev;
     }
     
     private static void walkMergeInfoHashForDiff(Map deleted, Map added, Map from, Map to, 
             boolean considerInheritance) {
         for (Iterator paths = from.keySet().iterator(); paths.hasNext();) {
             String path = (String) paths.next();
             SVNMergeRangeList fromRangeList = (SVNMergeRangeList) from.get(path);
             SVNMergeRangeList toRangeList = (SVNMergeRangeList) to.get(path);
             if (toRangeList != null) {
                 SVNMergeRangeList deletedRangeList = fromRangeList.diff(toRangeList, 
                                                                         considerInheritance);
                 SVNMergeRangeList addedRangeList = toRangeList.diff(fromRangeList, 
                                                                     considerInheritance);
                 if (deleted != null && deletedRangeList.getSize() > 0) {
                     deleted.put(path, deletedRangeList);
                 }
                 if (added != null && addedRangeList.getSize() > 0) {
                     added.put(path, addedRangeList);
                 }
             } else if (deleted != null) {
                 deleted.put(path, fromRangeList.dup());
             }
         }
         
         if (added == null) {
             return;
         }
         
         for (Iterator paths = to.keySet().iterator(); paths.hasNext();) {
             String path = (String) paths.next();
             SVNMergeRangeList toRangeList = (SVNMergeRangeList) to.get(path);
             if (!from.containsKey(path)) {
                 added.put(path, toRangeList.dup());
             }
         }        
     }
     
     private static SVNMergeRange combineWithAdjacentLastRange(Collection result, SVNMergeRange lastRange, 
             SVNMergeRange mRange, boolean dupMRange) throws SVNException {
         SVNMergeRange pushedMRange = mRange;
         if (lastRange != null) {
             if (lastRange.getStartRevision() <= mRange.getEndRevision() && 
                     mRange.getStartRevision() <= lastRange.getEndRevision()) {
                 
                 if (mRange.getStartRevision() < lastRange.getEndRevision()) {
                     SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                             "Parsing of overlapping revision ranges ''{0}'' and ''{1}'' is not supported",
                             new Object[] { lastRange.toString(), mRange.toString() });
                     SVNErrorManager.error(err);
                 } else if (lastRange.isInheritable() == mRange.isInheritable()) {
                     lastRange.setEndRevision(mRange.getEndRevision());
                     return lastRange;
                 }
             } else if (lastRange.getStartRevision() > mRange.getStartRevision()) {
                   SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.MERGE_INFO_PARSE_ERROR, 
                           "Unable to parse unordered revision ranges ''{0}'' and ''{1}''", 
                           new Object[] { lastRange.toString(), mRange.toString() });
                   SVNErrorManager.error(err);
             }
         }
         
         if (dupMRange) {
             pushedMRange = mRange.dup();
         }
         result.add(pushedMRange);
         lastRange = pushedMRange;
         return lastRange;
     }
 
 	private static class ElideMergeInfoCatalogHandler implements ISVNCommitPathHandler {
         private Map myMergeInfoCatalog;
         private List myElidablePaths;
         
         public ElideMergeInfoCatalogHandler(Map mergeInfoCatalog) {
             myMergeInfoCatalog = mergeInfoCatalog;
             myElidablePaths = new LinkedList();
         }
         
         public boolean handleCommitPath(String path, ISVNEditor editor) throws SVNException {
             ElideMergeInfoEditor elideEditor = (ElideMergeInfoEditor) editor;
 	        String inheritedMergeInfoPath = elideEditor.getInheritedMergeInfoPath();
 	        if (inheritedMergeInfoPath == null || "/".equals(path)) {
 	            return false;
 	        }
 	        String pathSuffix = SVNPathUtil.getPathAsChild(inheritedMergeInfoPath, path);
 	        if (pathSuffix == null) {
 	            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "path suffix is null");
 	            SVNErrorManager.error(err);
 	        }
 	        boolean elides = shouldElideMergeInfo((Map) myMergeInfoCatalog.get(inheritedMergeInfoPath), 
 	                (Map) myMergeInfoCatalog.get(path), pathSuffix);
 	        if (elides) {
 	            myElidablePaths.add(path);
 	        }
 	        return false;
 	    }
         
         public List getElidablePaths() {
             return myElidablePaths;
         }
 	}
 	
 	private static class ElideMergeInfoEditor implements ISVNEditor {
 
 	    private Map myMergeInfoCatalog;
 	    private ElideMergeInfoCatalogDirBaton myCurrentDirBaton;
 	    
 	    public ElideMergeInfoEditor(Map mergeInfoCatalog) {
 	        myMergeInfoCatalog = mergeInfoCatalog;
 	    }
 	    
         public void abortEdit() throws SVNException {
         }
 
         public void absentDir(String path) throws SVNException {
         }
 
         public void absentFile(String path) throws SVNException {
         }
 
         public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
         }
 
         public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
         }
 
         public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
         }
 
         public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
         }
 
         public void closeDir() throws SVNException {
         }
 
         public SVNCommitInfo closeEdit() throws SVNException {
             return null;
         }
 
         public void closeFile(String path, String textChecksum) throws SVNException {
         }
 
         public void deleteEntry(String path, long revision) throws SVNException {
         }
 
         public void openDir(String path, long revision) throws SVNException {
             if (!path.startsWith("/")) {
                 path = "/" + path;
             }
             
             ElideMergeInfoCatalogDirBaton dirBaton = new ElideMergeInfoCatalogDirBaton();
             if (myMergeInfoCatalog.get(path) != null) {
                 dirBaton.myInheritedMergeInfoPath = path;
             } else {
                 dirBaton.myInheritedMergeInfoPath = myCurrentDirBaton.myInheritedMergeInfoPath;
             }
             myCurrentDirBaton = dirBaton;
         }
 
         public void openFile(String path, long revision) throws SVNException {
         }
 
         public void openRoot(long revision) throws SVNException {
             myCurrentDirBaton = new ElideMergeInfoCatalogDirBaton();
         }
 
         public void targetRevision(long revision) throws SVNException {
         }
 
         public void applyTextDelta(String path, String baseChecksum) throws SVNException {
         }
 
         public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
             return SVNFileUtil.DUMMY_OUT;
         }
 
         public void textDeltaEnd(String path) throws SVNException {
         }
 
         public String getInheritedMergeInfoPath() {
             return myCurrentDirBaton.myInheritedMergeInfoPath;
         }
         
         private class ElideMergeInfoCatalogDirBaton {
             private String myInheritedMergeInfoPath;
         }
 	    
 	}
 
 }
