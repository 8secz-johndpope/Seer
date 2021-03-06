 /*
  * To change this template, choose Tools | Templates
  * and open the template in the editor.
  */
 
 package org.catacombae.dmg.sparsebundle;
 
 import java.io.IOException;
 import java.io.RandomAccessFile;
 import java.io.Reader;
 import java.nio.channels.FileLock;
 import org.catacombae.plist.PlistNode;
 import org.catacombae.plist.XmlPlist;
 import org.catacombae.util.Util;
 
 /**
  *
  * @author erik
  */
 class Info extends BundleMember {
     private long bandSize;
     private long size;
 
     public Info(RandomAccessFile plistFile, FileLock plistFileLock)
             throws IOException {
         super(plistFile, plistFileLock);
         refresh();
     }
 
     public long getBandSize() { return bandSize; }
     public long getSize() { return size; }
 
     /**
      * Re-reads the contents of the .plist file and updates the cached data.
      *
      * @throws IOException if there is an I/O error, or the data in the plist is
      * invalid.
      */
     protected void refresh() throws IOException {
         long fileLength = file.length();
         if(fileLength > Integer.MAX_VALUE)
             throw new ArrayIndexOutOfBoundsException("Info.plist is " +
                     "unreasonably large and doesn't fit in memory.");
 
         byte[] plistData = new byte[(int) fileLength];
         int bytesRead = file.read(plistData);
         if(bytesRead != fileLength)
             throw new IOException("Failed to read entire file. Read " +
                     bytesRead + "/" + fileLength + " bytes.");
 
         XmlPlist plist = new XmlPlist(plistData, true);
         PlistNode dictNode = plist.getRootNode().cd("dict");
         if(dictNode == null) {
             throw new IOException("Malformed Info.plist file: No 'dict' " +
                     "element at root.");
         }
 
         final String cfBundleInfoDictionaryVersionKey =
                 "CFBundleInfoDictionaryVersion";
         final String bandSizeKey =
                 "band-size";
         final String bundleBackingstoreVersionKey =
                 "bundle-backingstore-version";
         final String diskImageBundleTypeKey =
                 "diskimage-bundle-type";
         final String sizeKey =
                 "size";
 
         Reader cfBundleInfoDictionaryVersionReader =
                 dictNode.getKeyValue(cfBundleInfoDictionaryVersionKey);
         Reader bandSizeReader =
                 dictNode.getKeyValue(bandSizeKey);
         Reader bundleBackingstoreVersionReader =
                 dictNode.getKeyValue(bundleBackingstoreVersionKey);
         Reader diskImageBundleTypeReader =
                 dictNode.getKeyValue(diskImageBundleTypeKey);
         Reader sizeReader =
                 dictNode.getKeyValue(sizeKey);
 
         if(cfBundleInfoDictionaryVersionReader == null)
             throw new IOException("Could not find '" +
                     cfBundleInfoDictionaryVersionKey + "' key in Info.plist " +
                     "file.");
         if(bandSizeReader == null)
             throw new IOException("Could not find '" + bandSizeKey + "' key " +
                     "in Info.plist file.");
         if(bundleBackingstoreVersionReader == null)
             throw new IOException("Could not find '" +
                     bundleBackingstoreVersionKey + "' key in Info.plist file.");
         if(diskImageBundleTypeReader == null)
             throw new IOException("Could not find '" + diskImageBundleTypeKey +
                     "' key in Info.plist file.");
         if(sizeReader == null)
             throw new IOException("Could not find '" + sizeKey + "' key in " +
                     "Info.plist file.");
 
         // We ignore the value of the dictionary version.
         //String cfBundleInfoDictionaryVersionString =
         //        Util.readFully(cfBundleInfoDictionaryVersionReader);
         String bandSizeString =
                 Util.readFully(bandSizeReader);
         String bundleBackingstoreVersionString =
                 Util.readFully(bundleBackingstoreVersionReader);
         String diskImageBundleTypeString =
                 Util.readFully(diskImageBundleTypeReader);
         String sizeString =
                 Util.readFully(sizeReader);
 
         if(!diskImageBundleTypeString.equals(
                 "com.apple.diskimage.sparsebundle")) {
             throw new IOException("Unexpected value for '" +
                     diskImageBundleTypeKey + "': " + diskImageBundleTypeString);
 
         }
 
         if(!bundleBackingstoreVersionString.equals("1")) {
             throw new IOException("Unknown backing store version: " +
                     bundleBackingstoreVersionString);
 
         }
 
         final long bandSizeLong;
         try {
             bandSizeLong = Long.parseLong(bandSizeString);
         } catch(NumberFormatException nfe) {
             throw new IOException("Illegal numeric value for " + bandSizeKey +
                    ": " + bandSizeString, nfe);
         }
 
         final long sizeLong;
         try {
             sizeLong = Long.parseLong(sizeString);
         } catch(NumberFormatException nfe) {
             throw new IOException("Illegal numeric value for " + sizeKey +
                    ": " + sizeString, nfe);
         }
 
         this.bandSize = bandSizeLong;
         this.size = sizeLong;
     }
 }
