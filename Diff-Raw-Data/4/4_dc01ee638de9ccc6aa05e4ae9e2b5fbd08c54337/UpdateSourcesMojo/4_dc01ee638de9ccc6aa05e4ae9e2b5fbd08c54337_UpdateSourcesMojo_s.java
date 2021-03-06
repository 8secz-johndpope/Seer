 package org.exoplatform.crowdin.mojo;
 
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.Enumeration;
 import java.util.List;
 import java.util.Properties;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipInputStream;
 
 import org.apache.commons.configuration.PropertiesConfiguration;
 import org.apache.maven.plugin.MojoExecutionException;
 import org.apache.maven.plugin.MojoFailureException;
 import org.exoplatform.crowdin.model.CrowdinFile.Type;
 import org.exoplatform.crowdin.model.CrowdinFileFactory;
 import org.exoplatform.crowdin.model.CrowdinTranslation;
 import org.exoplatform.crowdin.utils.FileUtils;
 import org.exoplatform.crowdin.utils.PropsToXML;
 import org.exoplatform.crowdin.utils.ShellScriptUtils;
 
 /**
  * @goal update
  * @author Philippe Aristote
  */
 public class UpdateSourcesMojo extends AbstractCrowdinMojo {
 
   @Override
   public void executeMojo() throws MojoExecutionException, MojoFailureException {
     File zip = new File("target/all.zip");
     if (!zip.exists()) {
       try {
     	getHelper().setApprovedOnlyOption();
         getLog().info("Downloading Crowdin translation zip...");
         getHelper().downloadTranslations();
         getLog().info("Downloading done!");
       } catch (Exception e) {
         getLog().error("Error downloading the translations from Crowdin. Exception:\n" + e.getMessage());
       }      
     }
     extractZip(getStartDir(), zip.getPath());
     //get the translations status
     File status_trans=new File("report/translation_status.xml");
     BufferedWriter writer = null;
 	try
 	{
 		writer = new BufferedWriter( new FileWriter( status_trans));
 		writer.write( getHelper().getTranslationStatus());
 	}
 	catch ( IOException e)
 	{
 	}
 	finally
 	{
 		try
 		{
 			if ( writer != null)
 				writer.close( );
 		}
 		catch ( IOException e)
 		{
 		}
      }
 	
   }
 
   private void extractZip(String _destFolder, String _zipFile) {
     try {
       String destinationname = _destFolder;
       byte[] buf = new byte[1024];
       List<String> langs = Arrays.asList(getLangs().split(","));
       ZipInputStream zipinputstream = null;
       ZipEntry zipentry;
       zipinputstream = new ZipInputStream(new FileInputStream(_zipFile));
 
       zipentry = zipinputstream.getNextEntry();
       while (zipentry != null) {
         // for each entry to be extracted
         if (zipentry.isDirectory()) {
           zipentry = zipinputstream.getNextEntry();
           continue;
         }
         String zipentryName = zipentry.getName();
         zipentryName = CrowdinFileFactory.encodeMinusCharacterInPath(zipentryName, false);
         zipentryName = zipentryName.replace('/', File.separatorChar);
         zipentryName = zipentryName.replace('\\', File.separatorChar);
         String[] path = zipentryName.split(File.separator);
         String lang = path[0];
         String crowdinProj = path[1];
         String proj = path[2];
         
         // process only the languages specified
         if(!(langs.contains("all") || langs.contains(lang))) {
           zipentry = zipinputstream.getNextEntry();
           continue;
         }
         
         String cp = crowdinProj + File.separator + proj;
         Properties currentProj = getProperties().get(proj+"/");
         // ignore projects that is not managed by the plugin
         if (currentProj == null) {
           zipentry = zipinputstream.getNextEntry();
           continue;
         }        
         String key = zipentryName.substring(zipentryName.indexOf(cp) + cp.length() + 1);
         String value = currentProj.getProperty(key);
         if (value == null) {
           zipentry = zipinputstream.getNextEntry();
           continue;
         }
         zipentryName = zipentryName.substring(0, zipentryName.indexOf(proj) + proj.length());
         
         lang = CrowdinTranslation.encodeLanguageName(lang, false);
         
         String fileName = value.substring(value.lastIndexOf(File.separatorChar)+1);
         String name = fileName.substring(0, fileName.lastIndexOf("."));
         String extension = fileName.substring(fileName.lastIndexOf("."));
         if(name.lastIndexOf("_en")>0){
           name = name.substring(0, name.lastIndexOf("_en"));
         }
         
         if (key.contains("gadget") || value.contains("gadget")) {
           if ("default".equalsIgnoreCase(name)) {
             fileName = lang + extension;
           } else if (name.contains("_ALL")) {
             fileName = lang + "_ALL" + extension;
           }
         } else {
           fileName = name + "_" + lang + extension;
         }
         
         String parentDir = destinationname + proj + "/" + value.substring(0, value.lastIndexOf(File.separatorChar)+1);
         parentDir = parentDir.replace('/', File.separatorChar).replace('\\', File.separatorChar);
         String entryName = parentDir + fileName;
         Type resourceBundleType = (key.indexOf("gadget") >= 0) ? Type.GADGET : Type.PORTLET;
        
         File newFile = new File(entryName.substring(0, entryName.lastIndexOf(File.separatorChar)));
         newFile.mkdirs();        
 
         // Need improve, some portlets in CS use xml format for vi, ar locales
         boolean isXML = (entryName.indexOf(".xml")>0);
         
         if(isXML){
           // create the temporary properties file to be used for PropsToXML (use the file in Crowdin zip)
           entryName = entryName.replaceAll(".xml", ".properties");
           int n;
           FileOutputStream fileoutputstream;
           fileoutputstream = new FileOutputStream(entryName);
           while ((n = zipinputstream.read(buf, 0, 1024)) > -1) {
             fileoutputstream.write(buf, 0, n);
           }
           fileoutputstream.close();
           
           File propertiesFile = new File(entryName);
           PropsToXML.execShellCommand("native2ascii -encoding UTF8 " + propertiesFile.getPath() + " " + propertiesFile.getPath());
           PropsToXML.parse(propertiesFile.getPath(), resourceBundleType);
           propertiesFile.delete();
         } else {
           // identify the master properties file
           String masterFile = parentDir + name + extension;
           if(!new File(masterFile).exists()) masterFile = parentDir + name + "_en" + extension;
           if(!new File(masterFile).exists()) throw new FileNotFoundException("Cannot create or update " + entryName + " as the master file " + name + extension + " (or " + name + "_en" + extension + ")" + " does not exist!");
 
           // use the master file as a skeleton and fill in with translations from Crowdin
           PropertiesConfiguration config =  new PropertiesConfiguration(masterFile);
           PropertiesConfiguration.setDefaultListDelimiter('=');
           config.setEncoding("UTF-8");
 
           Properties props = new Properties();
           props.load(zipinputstream);
           Enumeration e = props.propertyNames();
           while (e.hasMoreElements()) {
             String propKey = (String) e.nextElement();
             config.setProperty(propKey, props.getProperty(propKey));
           }
           
           // if language is English, update master file and the English file if it exists (do not create new)
           if("en".equals(lang)) {
             config.save(masterFile);
             // perform post-processing for the output file
             //use shell script
             //ShellScriptUtils.execShellscript("scripts/per-file-processing.sh", masterFile);
             //use java
             FileUtils.replaceCharactersInFile(masterFile, "config/special_character_processing.properties", "UpdateSourceSpecialCharacters");
             
             if(new File(entryName).exists()) {
               config.save(entryName);
               //use shell script
               //ShellScriptUtils.execShellscript("scripts/per-file-processing.sh", entryName);
               //use java
               FileUtils.replaceCharactersInFile(entryName, "config/special_character_processing.properties", "UpdateSourceSpecialCharacters");
               
             }
           } else {
             // always create new (or update) for other languages
             config.save(entryName);
             //use shell script
             //ShellScriptUtils.execShellscript("scripts/per-file-processing.sh", entryName);
             //user java
             FileUtils.replaceCharactersInFile(entryName, "config/special_character_processing.properties", "UpdateSourceSpecialCharacters");
           
           }
         }
         
         zipinputstream.closeEntry();
         zipentry = zipinputstream.getNextEntry();
       }// while
 
       zipinputstream.close();
     } catch (Exception e) {
       e.printStackTrace();
     }
   }
 
 }
