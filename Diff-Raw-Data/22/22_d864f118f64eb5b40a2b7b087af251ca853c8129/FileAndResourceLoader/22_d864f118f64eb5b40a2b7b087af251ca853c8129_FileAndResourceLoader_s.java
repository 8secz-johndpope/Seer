 package com.gu.conf;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.log4j.Logger;
 
 import java.io.*;
 import java.net.URL;
 import java.util.Properties;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class FileAndResourceLoader {
 
     private static final Logger LOG = Logger.getLogger(FileAndResourceLoader.class);
     private static final Pattern protocolMatcher = Pattern.compile("(?:file|classpath):(?://)?(.*)");
 
     public boolean exists(String location) {
         if (location.startsWith("file:")) {
             location = stripProtocol(location);
         }
 
         return new File(location).exists();
     }
 
     private String stripProtocol(String location) {
         Matcher match = protocolMatcher.matcher(location);
         if (match.matches()) {
             return match.group(1);
         }
 
         return location;
     }
 
     private InputStream getFile(String filename) throws IOException {
         if (!exists(filename)) {
             LOG.error("File does not exist trying to load properties from " + filename);
            throw new FileNotFoundException(filename);
         }
 
         return new BufferedInputStream(new FileInputStream(stripProtocol(filename)));
     }
 
     private InputStream getResource(String resource) throws IOException {
         ClassLoader classloader = FileAndResourceLoader.class.getClassLoader();
         URL url = classloader.getResource(stripProtocol(resource));
 
         InputStream inputStream;
         try {
              inputStream = url.openStream();
         } catch (IOException ioe) {
             LOG.info("Cannot open resource trying to load properties from " + resource);
            throw ioe;
         }
 
         return new BufferedInputStream(inputStream);
     }
 
     public Properties getPropertiesFrom(String descriptor) throws IOException {
         Properties properties = new Properties();
         InputStream inputStream = null;
         try {
             if (descriptor.startsWith("file:")) {
                 inputStream = getFile(descriptor);
             } else if (descriptor.startsWith("classpath:")) {
                 inputStream = getResource(descriptor);
             } else {
                 LOG.error("Unknown protocol trying to load properties from " + descriptor);
             }
 
            properties.load(inputStream);
        } finally {
             IOUtils.closeQuietly(inputStream);
         }
 
         return properties;
     }
 }
