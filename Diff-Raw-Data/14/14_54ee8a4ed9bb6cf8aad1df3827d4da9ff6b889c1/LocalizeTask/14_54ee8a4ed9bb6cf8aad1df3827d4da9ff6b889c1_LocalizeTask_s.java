 /*
  * Copyright 2013 Maxime Falaize.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 package com.mfalaize.ant;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Locale;
 import java.util.ResourceBundle;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import org.apache.commons.io.FileUtils;
 import org.apache.tools.ant.BuildException;
 import org.apache.tools.ant.DirectoryScanner;
 import org.apache.tools.ant.Project;
 import org.apache.tools.ant.taskdefs.MatchingTask;
 
 /**
  * Ant task to translate files such as HTML files easily from the build process.
  * It replaces all occurrences of the
 * <code>$loc{key}</code> pattern by the corresponding in properties or java
  * files.
  * <p>
  * The task extends of {@link MatchingTask} which allows you to use commons
  * include and exclude tags to select your processed files.
  *
  * @author Maxime Falaize
  * @version 1.0
  * @since 1.0
  */
 public class LocalizeTask extends MatchingTask {
 
     /**
      * The pattern matches strings such as
     * <code>$loc{key}</code>.
      */
    private static final String LOCALIZE_PATTERN = "(\\$loc\\{\\s*)([\\w\\._-]+)(\\s*\\})";
     /**
      * The project directory. Default is the current working directory.
      */
     private File projectDirectory = new File(".");
     /**
      * The base name of the resource bundle, a fully qualified class name.
     * Default to html.
      */
    private String resourceBundleBaseName = "html";
     /**
      * Encoding of the files to translate. Default is UTF-8.
      */
     private String encoding = "UTF-8";
     /**
      * The directory which contains all of the files to translate. Default to
      * the {@link projectDirectory}.
      */
     private File translateDirectory = projectDirectory;
     /**
      * The directory which will contain all of the files generated by this ant
      * task. Default to the {@link projectDirectory}.
      */
     private File targetDirectory = projectDirectory;
 
     public void setProjectDirectory(File projectDirectory) {
         this.projectDirectory = projectDirectory;
     }
 
     public void setResourceBundleBaseName(String resourceBundleBaseName) {
         this.resourceBundleBaseName = resourceBundleBaseName;
     }
 
     public void setEncoding(String encoding) {
         this.encoding = encoding;
     }
 
     public void setTranslateDirectory(File translateDirectory) {
         this.translateDirectory = translateDirectory;
     }
 
     public void setTargetDirectory(File targetDirectory) {
         this.targetDirectory = targetDirectory;
     }
 
     /**
      * Get the list of available
      * <code>ResourceBundle</code> in the project.
      *
      * @return The list of <code>ResourceBundle</code> available in the project
      * to build.
      * @throws BuildException when the <code>ResourceBundle</code> cannot be
      * find.
      */
     private List<ResourceBundle> getAvailableResourceBundles() throws BuildException {
         List<ResourceBundle> list = new ArrayList<ResourceBundle>();
 
         for (Locale locale : Locale.getAvailableLocales()) {
             try {
                 ResourceBundle resourceBundle = ResourceBundle.getBundle(resourceBundleBaseName, locale);
 
                 if (locale.equals(resourceBundle.getLocale())) {
                     list.add(resourceBundle);
                 } else {
                     log(String.format("No resource bundle exists for the locale %s. Continue...", locale.getLanguage()), Project.MSG_VERBOSE);
                 }
             } catch (Throwable ex) {
                 throw new BuildException(ex);
             }
         }
 
         return list;
     }
 
     @Override
     public void execute() throws BuildException {
         for (ResourceBundle resourceBundle : getAvailableResourceBundles()) {
             // Create the directory in which store all the files translated for this resourceBundle
             File directory = new File(targetDirectory + File.separator + resourceBundle.getLocale().getLanguage());
             if (!directory.mkdirs()) {
                 log(String.format("Cannot create directory %s.", directory.getAbsolutePath()), Project.MSG_ERR);
                 continue;
             } else {
                 log(String.format("Creating directory for locale %s.", resourceBundle.getLocale().getLanguage()), Project.MSG_VERBOSE);
             }
 
             DirectoryScanner ds = getDirectoryScanner(translateDirectory);
             String[] files = ds.getIncludedFiles();
 
             log(String.format("Translating %d files for locale %s...", files.length, resourceBundle.getLocale().getLanguage()));
             for (int i = 0; i < files.length; i++) {
                 File file = new File(translateDirectory + File.separator + files[i]);
                 log(String.format("Translating %s...", files[i]), Project.MSG_VERBOSE);
                 try {
                     String fileString = FileUtils.readFileToString(file, encoding);
 
                     Pattern pattern = Pattern.compile(LOCALIZE_PATTERN);
 
                     Matcher matcher = pattern.matcher(fileString);
                     while (matcher.find()) {
                         String key = matcher.group(2);
                         fileString = matcher.replaceFirst(resourceBundle.getString(key));
                         log(String.format("Replacing occurrence of %s by %s.", matcher.group(), resourceBundle.getString(key)), Project.MSG_DEBUG);
                         matcher = pattern.matcher(fileString);
                     }
 
                     String newFilePath = directory.getAbsolutePath() + File.separator + org.apache.tools.ant.util.FileUtils.getRelativePath(projectDirectory, file);
                     FileUtils.writeStringToFile(new File(newFilePath), fileString, encoding);
                 } catch (Throwable ex) {
                     log(String.format("An exception occured : %s", ex.getMessage()), Project.MSG_ERR);
                     throw new BuildException(ex);
                 }
             }
         }
     }
 }
