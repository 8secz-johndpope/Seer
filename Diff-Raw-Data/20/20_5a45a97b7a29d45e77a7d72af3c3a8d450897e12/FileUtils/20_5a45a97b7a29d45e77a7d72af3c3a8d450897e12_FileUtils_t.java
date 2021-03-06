 /*
  * This file is part of the DITA Open Toolkit project hosted on
  * Sourceforge.net. See the accompanying license.txt file for 
  * applicable licenses.
  */
 
 /*
  * (c) Copyright IBM Corp. 2005 All Rights Reserved.
  */
 package org.dita.dost.util;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Properties;
 import java.util.StringTokenizer;
 
 import org.dita.dost.log.DITAOTJavaLogger;
 import org.dita.dost.log.MessageUtils;
 import org.dita.dost.module.DebugAndFilterModule;
 
 /**
  * Class description goes here.
  * 
  * @author Wu, Zhi Qiang
  */
 public class FileUtils {
 	/**
 	 * Default Constructor
 	 *
 	 */
 	private FileUtils(){
 	}
 
 	/**
 	 * Return if the file is a html file by extension
 	 * @param lcasefn
 	 * @return
 	 */
 	public static boolean isHTMLFile(String lcasefn) {
 		return (lcasefn.endsWith(Constants.FILE_EXTENSION_HTML) || lcasefn
 				.endsWith(Constants.FILE_EXTENSION_HTM));
 	}
 
 	/**
 	 * Return if the file is a dita file by extension
 	 * @param lcasefn
 	 * @return
 	 */
 	public static boolean isDITAFile(String lcasefn) {
 		return isDITATopicFile(lcasefn) || isDITAMapFile(lcasefn);
 	}
 
 	/**
 	 * Return if the file is a dita topic file by extension
 	 * @param lcasefn
 	 * @return
 	 */
 	public static boolean isDITATopicFile(String lcasefn) {
 		return lcasefn.endsWith(Constants.FILE_EXTENSION_DITA)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_XML);
 	}
 
 	/**
 	 * Return if the file is a dita map file by extension
 	 * @param lcasefn
 	 * @return
 	 */
 	public static boolean isDITAMapFile(String lcasefn) {
 		return lcasefn.endsWith(Constants.FILE_EXTENSION_DITAMAP);
 	}
 
 	/**
 	 * Return if the file is a supported image file by extension
 	 * @param lcasefn
 	 * @return
 	 */
 	public static boolean isSupportedImageFile(String lcasefn) {
 		return lcasefn.endsWith(Constants.FILE_EXTENSION_JPG)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_GIF)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_EPS)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_JPEG)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_PNG)
				|| lcasefn.endsWith(Constants.FILE_EXTENSION_SVG)
				|| lcasefn.endsWith(Constants.FILE_EXTENSION_TIFF)
				|| lcasefn.endsWith(Constants.FILE_EXTENSION_TIF);
 	}
 
 	/**
 	 * Return if the file is a topic file by extension
 	 * @param lcasefn
 	 * @return
 	 */
 	public static boolean isTopicFile(String lcasefn) {
 		if(StringUtils.isEmptyString(lcasefn)){
 			return false;
 		}
 		return lcasefn.endsWith(Constants.FILE_EXTENSION_DITA)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_XML);
 	}
 
 	/**
 	 * Return if the file is a valid target file by extension
 	 * @param lcasefn
 	 * @return
 	 */
 	public static boolean isValidTarget(String lcasefn) {
 		DITAOTJavaLogger logger = new DITAOTJavaLogger();
 		Properties params = new Properties();
 		
 		if(lcasefn.indexOf(Constants.STRING_BLANK)!=-1){
 			params.put("%1", lcasefn);
 			logger.logWarn(MessageUtils.getMessage("DOTJ027W", params).toString());
 		}
 		
 		return lcasefn.endsWith(Constants.FILE_EXTENSION_DITA)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_DITAMAP)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_XML)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_JPG)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_GIF)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_EPS)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_HTML)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_JPEG)
 				|| lcasefn.endsWith(Constants.FILE_EXTENSION_PNG)
				|| lcasefn.endsWith(Constants.FILE_EXTENSION_SVG)
				|| lcasefn.endsWith(Constants.FILE_EXTENSION_TIFF)
				|| lcasefn.endsWith(Constants.FILE_EXTENSION_TIF);
 	}
 
 	/**
 	 * Get the path of topicFile relative to the input map.
 	 * 
 	 * @param mapFilePathName
 	 * @param topicFilePathName
 	 * @return
 	 */
 	public static String getRelativePathFromMap(String mapFilePathName,
 			String topicFilePathName) {
 		StringBuffer upPathBuffer = new StringBuffer(Constants.INT_128);
 		StringBuffer downPathBuffer = new StringBuffer(Constants.INT_128);
 		StringTokenizer mapTokenizer = new StringTokenizer(
 				removeRedundantNames(mapFilePathName.replaceAll(Constants.DOUBLE_BACK_SLASH,Constants.SLASH),
 						Constants.SLASH),
 				Constants.SLASH);
 		StringTokenizer topicTokenizer = new StringTokenizer(
 				removeRedundantNames(topicFilePathName.replaceAll(Constants.DOUBLE_BACK_SLASH,Constants.SLASH),
 						Constants.SLASH),
 				Constants.SLASH);
 
 		while (mapTokenizer.countTokens() > 1
 				&& topicTokenizer.countTokens() > 1) {
 			String mapToken = mapTokenizer.nextToken();
 			String topicToken = topicTokenizer.nextToken();
 
 			if (!(mapToken.equals(topicToken))) {
 				if(mapToken.endsWith(Constants.COLON) ||
 						topicToken.endsWith(Constants.COLON)){
 					//the two files are in different disks under Windows
 					return topicFilePathName;
 				}
 				upPathBuffer.append("..");
 				upPathBuffer.append(Constants.SLASH);
 				downPathBuffer.append(topicToken);
 				downPathBuffer.append(Constants.SLASH);
 				break;
 			}
 		}
 
 		while (mapTokenizer.countTokens() > 1) {
 			mapTokenizer.nextToken();
 
 			upPathBuffer.append("..");
 			upPathBuffer.append(Constants.SLASH);
 		}
 
 		while (topicTokenizer.hasMoreTokens()) {
 			downPathBuffer.append(topicTokenizer.nextToken());
 			if (topicTokenizer.hasMoreTokens()) {
 				downPathBuffer.append(Constants.SLASH);
 			}
 		}
 
 		return upPathBuffer.append(downPathBuffer).toString();
 	}
 	
 	/**
 	 * Get path2Project from the relative path of a file
 	 * @param relativePath
 	 */
 	public static String getPathtoProject (String relativePath){
 		StringTokenizer tokenizer = new StringTokenizer(relativePath, Constants.SLASH);
 		StringBuffer buffer = new StringBuffer();
 		if (tokenizer.countTokens() == 1){
 			return null;
 		}else{
 			while(tokenizer.countTokens() > 1){
 				tokenizer.nextToken();
 				buffer.append("..");
 				buffer.append(Constants.SLASH);
 			}
 			return buffer.toString();
 		}
 	}
 
 	/**
 	 * Normalize topic path base on current directory and href value, by 
 	 * replacing "\\" and "\" with File.separator, and removing ".", ".."
 	 * from the file path, with no change to substring behind "#".  
 	 * 
 	 * @param rootPath
 	 * @param relativePath
 	 * @return
 	 */
 	public static String resolveTopic(String rootPath, String relativePath) {
 		String begin = relativePath;
 		String end = "";
 	
 		if (relativePath.indexOf("#") != -1) {
 			begin = relativePath.substring(0, relativePath.indexOf('#'));
 			end = relativePath.substring(relativePath.indexOf('#'));
 		}
 		
 		return normalizeDirectory(rootPath, begin) + end;
 	}
 	
 	/**
 	 * Normalize topic path base on current directory and href value, by 
 	 * replacing "\\" and "\" with File.separator, and removing ".", "..","#"
 	 * from the file path.  
 	 * 
 	 * @param rootPath
 	 * @param relativePath
 	 * @return
 	 */
 	public static String resolveFile(String rootPath, String relativePath) {
 		String begin = relativePath;
 	
 		if (relativePath.indexOf("#") != -1) {
 			begin = relativePath.substring(0, relativePath.indexOf('#'));
 		}
 		
 		return normalizeDirectory(rootPath, begin);
 	}
 	
 	/**
 	 * Normalize the input file path, by replacing all the '\\', '/' with
 	 * File.seperator, and removing '..' from the directory.
 	 * 
 	 * Note: the substring behind "#" will be removed. 
 	 * @param basedir
 	 * @param filepath
 	 */
 	public static String normalizeDirectory(String basedir, String filepath) {
 		String normilizedPath = null;
 		int index = filepath.indexOf(Constants.SHARP);
 		String pathname = (index == -1) ? filepath : filepath.substring(0, index);
 
 		/*
 		 * normilize file path using java.io.File
 		 */
 		normilizedPath = new File(basedir, pathname).getPath();
 		
 		if (basedir == null || basedir.length() == 0) {
 			return normilizedPath;
 		}
 
 		return FileUtils.removeRedundantNames(normilizedPath);
 	}
 	
 	/**
 	 * Removing redundant names ".." and "." from the given path.
 	 * 
 	 * @param path
 	 * @return
 	 */
 	public static String removeRedundantNames(String path) {
 		return removeRedundantNames(path, File.separator);
     }
 	
 	
 	/**
 	 * Removing redundant names ".." and "." from the given path.
 	 * 
 	 * @param path
 	 * @param separator
 	 * @return
 	 */
 	public static String removeRedundantNames(String path, String separator) {
         StringTokenizer tokenizer = null;
         StringBuffer buff = new StringBuffer(path.length());
         List dirs = new LinkedList();
         Iterator iter = null;
         int dirNum = 0;
         int i = 0;
     
         /*
          * remove "." from the directory.
          */
         tokenizer = new StringTokenizer(path, separator);
         while (tokenizer.hasMoreTokens()) {
             String token = tokenizer.nextToken();
             if (!(".".equals(token))) {
 			    dirs.add(token);
             }
         }
     
         /*
          * remove ".." and the dir name before it.
          */
         dirNum = dirs.size();
         while (i < dirNum) {
         	if (i > 0) {
         	  String lastDir = (String) dirs.get(i - 1);
         	  String dir = (String) dirs.get(i);
         	  if ("..".equals(dir) && !("..".equals(lastDir))) {
                   dirs.remove(i);
                   dirs.remove(i - 1);
                   dirNum = dirs.size();
                   i = i - 1;
                   continue;
         	  }
         	}
         	
         	i++;
         }
     
         /*
          * restore the directory.
          */
         if (path.startsWith(separator)) {
         	buff.append(separator);
         }
         iter = dirs.iterator();
         while (iter.hasNext()) {
             buff.append(iter.next());
             if (iter.hasNext()) {
                 buff.append(separator);
             }
         }
     
         return buff.toString();
     }
 	
 	
 	
 	
 
     /** 
      * Return if the path is absolute
      * @param path
      * @return
      */
     public static boolean isAbsolutePath (String path) {
     	if (path == null || Constants.STRING_EMPTY.equals(path.trim())) {
     		return false;
     	}
     	
         if (Constants.FILE_SEPARATOR.equals ("/")) {
             return path.startsWith ("/");
         }
         
         if (Constants.FILE_SEPARATOR.equals ("\\") && path.length() > 2) {         
         	return path.matches("[a-zA-Z]:\\\\.*");
         }
 
         return false;
     }
 
     /**
      * Copy file from src to target, overwrite if needed
      * @param src
      * @param target
      */
     public static void copyFile(File src, File target) {
 		FileInputStream fis = null;
 		FileOutputStream fos = null;
 		byte[] buffer = new byte[Constants.INT_1024 * Constants.INT_4];
 		int len;
 		
 		try {
 			fis = new FileInputStream(src);
 			fos = new FileOutputStream(target);
 			while ((len = fis.read(buffer)) != -1) {
 				fos.write(buffer, 0, len);
 			}
 			fos.flush();
 		} catch (IOException ex) {
 			DITAOTJavaLogger logger = new DITAOTJavaLogger();
 			logger.logWarn("Failed to copy file from '" + src + "' to '"
 					+ target + "'");
 			logger.logException(ex);
 			
 		} finally {
 			try {
 				if (fis != null){
 					fis.close();
 				}
 				if (fos != null){
 					fos.close();
 				}
 			} catch (Exception e) {
 				DITAOTJavaLogger logger = new DITAOTJavaLogger();
 				logger.logException(e);
 			}
 		}
 	}
     
     /**
      * Replace the file extension
      * @param attValue
      * @return
      */
     public static String replaceExtName(String attValue){
     	String fileName;
         int fileExtIndex;
         int index;
     	
     	index = attValue.indexOf(Constants.SHARP);
 		
     	if (attValue.startsWith(Constants.SHARP)){
     		return attValue;
     	} else if (index != -1){
     		fileName = attValue.substring(0,index); 
     		fileExtIndex = fileName.lastIndexOf(Constants.DOT);
     		return (fileExtIndex != -1)
     			? fileName.substring(0, fileExtIndex) + DebugAndFilterModule.extName + attValue.substring(index)
     			: attValue;
     	} else {
     		fileExtIndex = attValue.lastIndexOf(Constants.DOT);
     		return (fileExtIndex != -1)
     			? (attValue.substring(0, fileExtIndex) + DebugAndFilterModule.extName) 
     			: attValue;
     	}
     }
     
     /**
      * Check whether a file exists on the local file systmem.
      * @param filename
      * @return boolean  true if the file exists, false otherwise
      */
     public static boolean fileExists (String filename){  //Eric
     	
     	filename = filename.indexOf(Constants.SHARP) != -1 
 		? filename.substring(0, filename.indexOf(Constants.SHARP))
 		: filename;
 		   
     	
     	if (new File(filename).exists()){
     		return true;
     	}
     	
     	return false;
     }
 
     
 }
