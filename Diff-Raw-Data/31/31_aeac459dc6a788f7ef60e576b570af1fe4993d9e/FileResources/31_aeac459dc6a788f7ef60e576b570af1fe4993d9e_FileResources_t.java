 package net.thucydides.core.resources;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 
 import org.apache.commons.lang.StringUtils;
 
 /**
  * Utility class used to copy resources from a classpath to a target directory.
  */
 public final class FileResources {
 
     private static final int BUFFER_SIZE = 4096;
 
     private String resourceDirectoryRoot;
 
     public static FileResources from(final String resourceDirectoryRoot) {
         return new FileResources(resourceDirectoryRoot);
     }
 
     private FileResources(final String resourceDirectoryRoot) {
         this.resourceDirectoryRoot = resourceDirectoryRoot;
     }
 
     public String findTargetSubdirectoryFrom(final String sourceResource) {
         int directoryRootStartsAt = StringUtils.lastIndexOf(sourceResource,
                 resourceDirectoryRoot);
         int relativePathStartsAt = directoryRootStartsAt
                 + resourceDirectoryRoot.length() + 1;
         String relativePath = sourceResource.substring(relativePathStartsAt);
         relativePath = stripLeadingSeparatorFrom(relativePath);
         return directoryIn(relativePath);
     }
 
     public String stripLeadingSeparatorFrom(final String path) {
         if (path.startsWith("/") || path.startsWith("\\")) {
             return path.substring(1);
         } else {
             return path;
         }
     }
 
     public String findTargetFileFrom(final String sourceResource) {
         int directoryRootStartsAt = StringUtils.lastIndexOf(sourceResource,
                 resourceDirectoryRoot);
         int relativePathStartsAt = directoryRootStartsAt
                 + resourceDirectoryRoot.length() + 1;
         String relativePath = sourceResource.substring(relativePathStartsAt);
 
         return filenameIn(relativePath);
     }
 
     public void copyResourceTo(final String sourceResource, final File targetDirectory)
             throws IOException {
 
         String targetFile = findTargetFileFrom(sourceResource);
         String targetRelativeDirectory = findTargetSubdirectoryFrom(sourceResource);
 
         File destinationDirectory = targetDirectory;
 
         if (targetRelativeDirectory.length() > 0) {
            destinationDirectory = new File(targetDirectory, targetRelativeDirectory);
         }

         if (new File(sourceResource).isDirectory()) {
             File fullTargetDirectory = new File(destinationDirectory, targetFile);
             fullTargetDirectory.mkdirs();
         } else {
            copyFileFromClasspathToTargetDirectory(sourceResource, destinationDirectory);
         }
     }
 
     private void copyFileFromClasspathToTargetDirectory(
             final String resourcePath, final File targetDirectory)
             throws IOException {
 
         FileOutputStream out = null;
         InputStream in = null;
         try {
             File resourceOnClasspath = new File(resourcePath);
 
             if (resourceOnClasspath.exists()) {
                 in = new FileInputStream(resourceOnClasspath);
             } else {
                 in = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
             }
             File destinationFile = new File(targetDirectory,
                     resourceOnClasspath.getName());
             if (destinationFile.getParent() != null) {
                 new File(destinationFile.getParent()).mkdirs();
             }
 
             out = new FileOutputStream(destinationFile);
 
             copyData(in, out);
         } finally {
             closeSafely(out, in);
         }
     }
 
     private void copyData(final InputStream in, final OutputStream out)
             throws IOException {
         byte[] buffer = new byte[BUFFER_SIZE];
         int bytesRead;
         while ((bytesRead = in.read(buffer)) != -1) {
             out.write(buffer, 0, bytesRead);
         }
     }
 
     private void closeSafely(final OutputStream out, final InputStream in)
             throws IOException {
         if (in != null) {
             in.close();
         }
         if (out != null) {
             out.close();
         }
     }
 
     private String directoryIn(final String path) {
 
         if (path.contains("/")) {
             int filenameStartsAt = StringUtils.lastIndexOf(path,"/");
             return path.substring(0, filenameStartsAt);
 
         } else if (path.contains("\\")) {
             int filenameStartsAt = StringUtils.lastIndexOf(path,"\\");
             return path.substring(0, filenameStartsAt);
         } else {
             return "";
         }
     }
 
     private String filenameIn(final String path) {
 
         if (path.contains("/")) {
             int filenameStartsAt = StringUtils.lastIndexOf(path,"/");
             return path.substring(filenameStartsAt + 1);
 
         } else if (path.contains("\\")) {
             int filenameStartsAt = StringUtils.lastIndexOf(path,"\\");
             return path.substring(filenameStartsAt + 1);
         } else {
             return path;
         }
     }
 }
