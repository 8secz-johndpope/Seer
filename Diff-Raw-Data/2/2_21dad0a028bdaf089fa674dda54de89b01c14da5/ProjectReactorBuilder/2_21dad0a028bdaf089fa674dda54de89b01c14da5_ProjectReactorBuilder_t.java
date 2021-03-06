 /*
  * Sonar Runner - Batch
  * Copyright (C) 2011 SonarSource
  * dev@sonar.codehaus.org
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
  */
 package org.sonar.runner.batch;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.collect.Lists;
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.io.filefilter.*;
 import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.sonar.api.batch.bootstrap.ProjectDefinition;
 import org.sonar.api.batch.bootstrap.ProjectReactor;
 
 import java.io.File;
 import java.io.FileFilter;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.util.*;
 import java.util.Map.Entry;
 
 /**
  * Class that creates a Sonar project definition based on a set of properties.
  *
  * @since 1.5
  */
 class ProjectReactorBuilder {
 
   private static final Logger LOG = LoggerFactory.getLogger(ProjectReactorBuilder.class);
 
   private static final String PROPERTY_PROJECT_BASEDIR = "sonar.projectBaseDir";
   private static final String PROPERTY_PROJECT_CONFIG_FILE = "sonar.projectConfigFile";
   private static final String PROPERTY_PROJECT_KEY = "sonar.projectKey";
   private static final String PROPERTY_PROJECT_NAME = "sonar.projectName";
   private static final String PROPERTY_PROJECT_DESCRIPTION = "sonar.projectDescription";
   private static final String PROPERTY_PROJECT_VERSION = "sonar.projectVersion";
   private static final String PROPERTY_MODULES = "sonar.modules";
 
   /**
    * New properties, to be consistent with Sonar naming conventions
    *
    * @since 1.5
    */
   private static final String PROPERTY_SOURCES = "sonar.sources";
   private static final String PROPERTY_TESTS = "sonar.tests";
   private static final String PROPERTY_BINARIES = "sonar.binaries";
   private static final String PROPERTY_LIBRARIES = "sonar.libraries";
 
   /**
    * Old deprecated properties, replaced by the same ones preceded by "sonar."
    */
   private static final String PROPERTY_OLD_SOURCES = "sources";
   private static final String PROPERTY_OLD_TESTS = "tests";
   private static final String PROPERTY_OLD_BINARIES = "binaries";
   private static final String PROPERTY_OLD_LIBRARIES = "libraries";
   private static final Map<String, String> DEPRECATED_PROPS_TO_NEW_PROPS = new HashMap<String, String>() {
     {
       put(PROPERTY_OLD_SOURCES, PROPERTY_SOURCES);
       put(PROPERTY_OLD_TESTS, PROPERTY_TESTS);
       put(PROPERTY_OLD_BINARIES, PROPERTY_BINARIES);
       put(PROPERTY_OLD_LIBRARIES, PROPERTY_LIBRARIES);
     }
   };
 
   /**
    * @since 1.4
    */
   private static final String PROPERTY_WORK_DIRECTORY = "sonar.working.directory";
   private static final String DEF_VALUE_WORK_DIRECTORY = ".sonar";
 
   /**
    * Array of all mandatory properties required for a project without child.
    */
   private static final String[] MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT = {
     PROPERTY_PROJECT_BASEDIR, PROPERTY_PROJECT_KEY, PROPERTY_PROJECT_NAME, PROPERTY_PROJECT_VERSION, PROPERTY_SOURCES
   };
 
   /**
    * Array of all mandatory properties required for a project with children.
    */
   private static final String[] MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT = {PROPERTY_PROJECT_BASEDIR, PROPERTY_PROJECT_KEY, PROPERTY_PROJECT_NAME, PROPERTY_PROJECT_VERSION};
 
   /**
    * Array of all mandatory properties required for a child project before its properties get merged with its parent ones.
    */
   private static final String[] MANDATORY_PROPERTIES_FOR_CHILD = {PROPERTY_PROJECT_KEY, PROPERTY_PROJECT_NAME};
 
   /**
    * Properties that must not be passed from the parent project to its children.
    */
   private static final List<String> NON_HERITED_PROPERTIES_FOR_CHILD = Lists.newArrayList(PROPERTY_PROJECT_BASEDIR, PROPERTY_MODULES, PROPERTY_PROJECT_DESCRIPTION);
 
   private Properties properties;
   private File rootProjectWorkDir;
 
   ProjectReactorBuilder(Properties properties) {
     this.properties = properties;
   }
 
   ProjectReactor build() {
     ProjectDefinition rootProject = defineProject(properties, null);
     rootProjectWorkDir = rootProject.getWorkDir();
     defineChildren(rootProject);
     cleanAndCheckProjectDefinitions(rootProject);
     return new ProjectReactor(rootProject);
   }
 
   private ProjectDefinition defineProject(Properties properties, ProjectDefinition parent) {
     File baseDir = new File(properties.getProperty(PROPERTY_PROJECT_BASEDIR));
     if (properties.containsKey(PROPERTY_MODULES)) {
       checkMandatoryProperties(properties, MANDATORY_PROPERTIES_FOR_MULTIMODULE_PROJECT);
     } else {
       checkMandatoryProperties(properties, MANDATORY_PROPERTIES_FOR_SIMPLE_PROJECT);
     }
     File workDir;
     if (parent == null) {
       validateDirectories(properties, baseDir, properties.getProperty(PROPERTY_PROJECT_KEY));
       workDir = initRootProjectWorkDir(baseDir);
     } else {
       workDir = initModuleWorkDir(properties);
     }
 
     ProjectDefinition definition = ProjectDefinition.create().setProperties(properties)
       .setBaseDir(baseDir)
       .setWorkDir(workDir);
     return definition;
   }
 
   @VisibleForTesting
   protected File initRootProjectWorkDir(File baseDir) {
     String workDir = properties.getProperty(PROPERTY_WORK_DIRECTORY);
     if (StringUtils.isBlank(workDir)) {
       return new File(baseDir, DEF_VALUE_WORK_DIRECTORY);
     }
 
     File customWorkDir = new File(workDir);
     if (customWorkDir.isAbsolute()) {
       return customWorkDir;
     }
     return new File(baseDir, customWorkDir.getPath());
   }
 
   @VisibleForTesting
   protected File initModuleWorkDir(Properties properties) {
     String cleanKey = StringUtils.deleteWhitespace(properties.getProperty(PROPERTY_PROJECT_KEY));
     cleanKey = StringUtils.replace(cleanKey, ":", "_");
     return new File(rootProjectWorkDir, cleanKey);
   }
 
   private void defineChildren(ProjectDefinition parentProject) {
     Properties parentProps = parentProject.getProperties();
     if (parentProps.containsKey(PROPERTY_MODULES)) {
       for (String module : Utils.getListFromProperty(parentProps, PROPERTY_MODULES)) {
         Properties moduleProps = extractModuleProperties(module, parentProps);
         ProjectDefinition childProject = loadChildProject(parentProject, moduleProps, module);
         // check the uniqueness of the child key
         checkUniquenessOfChildKey(childProject, parentProject);
         // the child project may have children as well
         defineChildren(childProject);
         // and finally add this child project to its parent
         parentProject.addSubProject(childProject);
       }
     }
   }
 
   private ProjectDefinition loadChildProject(ProjectDefinition parentProject, Properties moduleProps, String moduleId) {
     setProjectKeyAndNameIfNotDefined(moduleProps, moduleId);
 
     final File baseDir;
     if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
       baseDir = getFileFromPath(moduleProps.getProperty(PROPERTY_PROJECT_BASEDIR), parentProject.getBaseDir());
       setProjectBaseDir(baseDir, moduleProps, moduleId);
       try {
         if (!parentProject.getBaseDir().getCanonicalFile().equals(baseDir.getCanonicalFile())) {
           tryToFindAndLoadPropsFile(baseDir, moduleProps, moduleId);
         }
       } catch (IOException e) {
         throw new IllegalStateException("Error when resolving baseDir", e);
       }
     } else if (moduleProps.containsKey(PROPERTY_PROJECT_CONFIG_FILE)) {
       baseDir = loadPropsFile(parentProject, moduleProps, moduleId);
     } else {
       baseDir = new File(parentProject.getBaseDir(), moduleId);
       setProjectBaseDir(baseDir, moduleProps, moduleId);
       tryToFindAndLoadPropsFile(baseDir, moduleProps, moduleId);
     }
 
     // and finish
     checkMandatoryProperties(moduleProps, MANDATORY_PROPERTIES_FOR_CHILD);
     validateDirectories(moduleProps, baseDir, moduleId);
 
     mergeParentProperties(moduleProps, parentProject.getProperties());
 
     prefixProjectKeyWithParentKey(moduleProps, parentProject.getKey());
 
     return defineProject(moduleProps, parentProject);
   }
 
   /**
    * @return baseDir
    */
   protected File loadPropsFile(ProjectDefinition parentProject, Properties moduleProps, String moduleId) {
     File propertyFile = getFileFromPath(moduleProps.getProperty(PROPERTY_PROJECT_CONFIG_FILE), parentProject.getBaseDir());
     if (propertyFile.isFile()) {
       Properties propsFromFile = toProperties(propertyFile);
       for (Entry<Object, Object> entry : propsFromFile.entrySet()) {
         moduleProps.put(entry.getKey(), entry.getValue());
       }
       File baseDir = null;
       if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
         baseDir = getFileFromPath(moduleProps.getProperty(PROPERTY_PROJECT_BASEDIR), propertyFile.getParentFile());
       } else {
         baseDir = propertyFile.getParentFile();
       }
       setProjectBaseDir(baseDir, moduleProps, moduleId);
       return baseDir;
     } else {
       throw new IllegalStateException("The properties file of the module '" + moduleId + "' does not exist: " + propertyFile.getAbsolutePath());
     }
   }
 
   private void tryToFindAndLoadPropsFile(File baseDir, Properties moduleProps, String moduleId) {
     File propertyFile = new File(baseDir, "sonar-project.properties");
     if (propertyFile.isFile()) {
       Properties propsFromFile = toProperties(propertyFile);
       for (Entry<Object, Object> entry : propsFromFile.entrySet()) {
         moduleProps.put(entry.getKey(), entry.getValue());
       }
       if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
         File overwrittenBaseDir = getFileFromPath(moduleProps.getProperty(PROPERTY_PROJECT_BASEDIR), propertyFile.getParentFile());
         setProjectBaseDir(overwrittenBaseDir, moduleProps, moduleId);
       }
     }
   }
 
   @VisibleForTesting
   protected static Properties toProperties(File propertyFile) {
     Properties propsFromFile = new Properties();
     FileInputStream fileInputStream = null;
     try {
       fileInputStream = new FileInputStream(propertyFile);
       propsFromFile.load(fileInputStream);
     } catch (IOException e) {
       throw new IllegalStateException("Impossible to read the property file: " + propertyFile.getAbsolutePath(), e);
     } finally {
       IOUtils.closeQuietly(fileInputStream);
     }
     return propsFromFile;
   }
 
   @VisibleForTesting
   protected static void setProjectKeyAndNameIfNotDefined(Properties childProps, String moduleId) {
     if (!childProps.containsKey(PROPERTY_PROJECT_KEY)) {
       childProps.put(PROPERTY_PROJECT_KEY, moduleId);
     }
     if (!childProps.containsKey(PROPERTY_PROJECT_NAME)) {
       childProps.put(PROPERTY_PROJECT_NAME, moduleId);
     }
   }
 
   @VisibleForTesting
   protected static void checkUniquenessOfChildKey(ProjectDefinition childProject, ProjectDefinition parentProject) {
     for (ProjectDefinition definition : parentProject.getSubProjects()) {
       if (definition.getKey().equals(childProject.getKey())) {
         throw new IllegalStateException("Project '" + parentProject.getKey() + "' can't have 2 modules with the following key: " + childProject.getKey());
       }
     }
   }
 
   @VisibleForTesting
   protected static void prefixProjectKeyWithParentKey(Properties childProps, String parentKey) {
     String childKey = childProps.getProperty(PROPERTY_PROJECT_KEY);
     childProps.put(PROPERTY_PROJECT_KEY, parentKey + ":" + childKey);
   }
 
   private static void setProjectBaseDir(File baseDir, Properties childProps, String moduleId) {
     if (!baseDir.isDirectory()) {
       throw new IllegalStateException("The base directory of the module '" + moduleId + "' does not exist: " + baseDir.getAbsolutePath());
     }
     childProps.put(PROPERTY_PROJECT_BASEDIR, baseDir.getAbsolutePath());
   }
 
   @VisibleForTesting
   protected static void checkMandatoryProperties(Properties props, String[] mandatoryProps) {
     replaceDeprecatedProperties(props);
     StringBuilder missing = new StringBuilder();
     for (String mandatoryProperty : mandatoryProps) {
       if (!props.containsKey(mandatoryProperty)) {
         if (missing.length() > 0) {
           missing.append(", ");
         }
         missing.append(mandatoryProperty);
       }
     }
     String projectKey = props.getProperty(PROPERTY_PROJECT_KEY);
     if (missing.length() != 0) {
       throw new IllegalStateException("You must define the following mandatory properties for '" + (projectKey == null ? "Unknown" : projectKey) + "': " + missing);
     }
   }
 
   private static void validateDirectories(Properties props, File baseDir, String projectId) {
     if (!props.containsKey(PROPERTY_MODULES)) {
       // SONARPLUGINS-2285 Not an aggregator project so we can validate that paths are correct if defined
 
       // We need to resolve patterns that may have been used in "sonar.libraries"
       for (String pattern : Utils.getListFromProperty(props, PROPERTY_LIBRARIES)) {
         File[] files = getLibraries(baseDir, pattern);
         if (files == null || files.length == 0) {
           LOG.error("Invalid value of " + PROPERTY_LIBRARIES + " for " + projectId);
           throw new IllegalStateException("No files nor directories matching '" + pattern + "' in directory " + baseDir);
         }
       }
 
       // Check sonar.tests
       String[] testDirs = Utils.getListFromProperty(props, PROPERTY_TESTS);
       checkExistenceOfDirectories(projectId, baseDir, testDirs, PROPERTY_TESTS);
 
       // Check sonar.binaries
       String[] binDirs = Utils.getListFromProperty(props, PROPERTY_BINARIES);
       checkExistenceOfDirectories(projectId, baseDir, binDirs, PROPERTY_BINARIES);
     }
   }
 
   @VisibleForTesting
   protected static void cleanAndCheckProjectDefinitions(ProjectDefinition project) {
     if (project.getSubProjects().isEmpty()) {
       cleanAndCheckModuleProperties(project);
     } else {
       cleanAndCheckAggregatorProjectProperties(project);
 
       // clean modules properties as well
       for (ProjectDefinition module : project.getSubProjects()) {
         cleanAndCheckProjectDefinitions(module);
       }
     }
   }
 
   @VisibleForTesting
   protected static void cleanAndCheckModuleProperties(ProjectDefinition project) {
     Properties properties = project.getProperties();
 
     // We need to check the existence of source directories
     String[] sourceDirs = Utils.getListFromProperty(properties, PROPERTY_SOURCES);
     checkExistenceOfDirectories(project.getKey(), project.getBaseDir(), sourceDirs, PROPERTY_SOURCES);
 
     // And we need to resolve patterns that may have been used in "sonar.libraries"
     List<String> libPaths = Lists.newArrayList();
     for (String pattern : Utils.getListFromProperty(properties, PROPERTY_LIBRARIES)) {
       for (File file : getLibraries(project.getBaseDir(), pattern)) {
         libPaths.add(file.getAbsolutePath());
       }
     }
     properties.remove(PROPERTY_LIBRARIES);
     properties.put(PROPERTY_LIBRARIES, StringUtils.join(libPaths, ","));
   }
 
   @VisibleForTesting
   protected static void cleanAndCheckAggregatorProjectProperties(ProjectDefinition project) {
     Properties properties = project.getProperties();
 
     // SONARPLUGINS-2295
     String[] sourceDirs = Utils.getListFromProperty(properties, PROPERTY_SOURCES);
     for (String path : sourceDirs) {
       File sourceFolder = getFileFromPath(path, project.getBaseDir());
       if (sourceFolder.isDirectory()) {
         LOG.warn("/!\\ A multi-module project can't have source folders, so '{}' won't be used for the analysis. " +
           "If you want to analyse files of this folder, you should create another sub-module and move them inside it.",
           sourceFolder.toString());
       }
     }
 
     // "aggregator" project must not have the following properties:
     properties.remove(PROPERTY_SOURCES);
     properties.remove(PROPERTY_TESTS);
     properties.remove(PROPERTY_BINARIES);
     properties.remove(PROPERTY_LIBRARIES);
 
     // and they don't need properties related to their modules either
     Properties clone = (Properties) properties.clone();
     List<String> moduleIds = Lists.newArrayList(Utils.getListFromProperty(properties, PROPERTY_MODULES));
     for (Entry<Object, Object> entry : clone.entrySet()) {
       String key = (String) entry.getKey();
       if (isKeyPrefixedByModuleId(key, moduleIds)) {
         properties.remove(key);
       }
     }
   }
 
   /**
    * Replaces the deprecated properties by the new ones, and logs a message to warn the users.
    */
   @VisibleForTesting
   protected static void replaceDeprecatedProperties(Properties props) {
     for (Entry<String, String> entry : DEPRECATED_PROPS_TO_NEW_PROPS.entrySet()) {
       String key = entry.getKey();
       if (props.containsKey(key)) {
         String newKey = entry.getValue();
         LOG.warn("/!\\ The '{}' property is deprecated and is replaced by '{}'. Don't forget to update your files.", key, newKey);
         String value = props.getProperty(key);
         props.remove(key);
         props.put(newKey, value);
       }
     }
 
   }
 
   @VisibleForTesting
   protected static void mergeParentProperties(Properties childProps, Properties parentProps) {
     List<String> moduleIds = Lists.newArrayList(Utils.getListFromProperty(parentProps, PROPERTY_MODULES));
     for (Map.Entry<Object, Object> entry : parentProps.entrySet()) {
       String key = (String) entry.getKey();
       if (!childProps.containsKey(key)
         && !NON_HERITED_PROPERTIES_FOR_CHILD.contains(key)
         && !isKeyPrefixedByModuleId(key, moduleIds)) {
         childProps.put(entry.getKey(), entry.getValue());
       }
     }
   }
 
   private static boolean isKeyPrefixedByModuleId(String key, List<String> moduleIds) {
     for (String moduleId : moduleIds) {
       if (key.startsWith(moduleId + ".")) {
         return true;
       }
     }
     return false;
   }
 
   @VisibleForTesting
   protected static Properties extractModuleProperties(String module, Properties properties) {
     Properties moduleProps = new Properties();
     String propertyPrefix = module + ".";
     int prefixLength = propertyPrefix.length();
     for (Map.Entry<Object, Object> entry : properties.entrySet()) {
       String key = (String) entry.getKey();
       if (key.startsWith(propertyPrefix)) {
         moduleProps.put(key.substring(prefixLength), entry.getValue());
       }
     }
     return moduleProps;
   }
 
   @VisibleForTesting
   protected static void checkExistenceOfDirectories(String moduleRef, File baseDir, String[] sourceDirs, String propName) {
     for (String path : sourceDirs) {
       File sourceFolder = getFileFromPath(path, baseDir);
       if (!sourceFolder.isDirectory()) {
         LOG.error("Invalid value of " + propName + " for " + moduleRef);
         throw new IllegalStateException("The folder '" + path + "' does not exist for '" + moduleRef +
           "' (base directory = " + baseDir.getAbsolutePath() + ")");
       }
     }
 
   }
 
   /**
    * Returns files matching specified pattern.
    */
   @VisibleForTesting
   protected static File[] getLibraries(File baseDir, String pattern) {
     final int i = Math.max(pattern.lastIndexOf('/'), pattern.lastIndexOf('\\'));
     final String dirPath, filePattern;
     if (i == -1) {
       dirPath = ".";
       filePattern = pattern;
     } else {
       dirPath = pattern.substring(0, i);
       filePattern = pattern.substring(i + 1);
     }
     List<IOFileFilter> filters = new ArrayList<IOFileFilter>();
    if (pattern.indexOf('*')>=0) {
       filters.add(FileFileFilter.FILE);
     }
     filters.add(new WildcardFileFilter(filePattern));
     File dir = resolvePath(baseDir, dirPath);
     File[] files = dir.listFiles((FileFilter) new AndFileFilter(filters));
     if (files == null) {
       files = new File[0];
     }
     return files;
   }
 
   private static File resolvePath(File baseDir, String path) {
     File file = new File(path);
     if (!file.isAbsolute()) {
       try {
         file = new File(baseDir, path).getCanonicalFile();
       } catch (IOException e) {
         throw new IllegalStateException("Unable to resolve path \"" + path + "\"", e);
       }
     }
     return file;
   }
 
   /**
    * Returns the file denoted by the given path, may this path be relative to "baseDir" or absolute.
    */
   @VisibleForTesting
   protected static File getFileFromPath(String path, File baseDir) {
     File propertyFile = new File(path.trim());
     if (!propertyFile.isAbsolute()) {
       propertyFile = new File(baseDir, propertyFile.getPath());
     }
     return propertyFile;
   }
 
 }
