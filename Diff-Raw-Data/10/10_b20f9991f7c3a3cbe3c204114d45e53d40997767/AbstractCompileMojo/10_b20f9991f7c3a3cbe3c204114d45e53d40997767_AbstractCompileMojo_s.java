 /*
  * Copyright (C) 2011 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.codehaus.gmavenplus.mojo;
 
 import org.codehaus.gmavenplus.model.Version;
 import org.codehaus.gmavenplus.util.ReflectionUtils;
 import java.io.File;
 import java.lang.reflect.InvocationTargetException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.security.CodeSource;
 import java.util.*;
 
 
 /**
  * The base compile mojo, which all compile mojos extend.
  *
  * @author Keegan Witt
  */
 public abstract class AbstractCompileMojo extends AbstractGroovySourcesMojo {
 
     /**
      * The location for the compiled classes.
      *
      * @parameter default-value="${project.build.outputDirectory}"
      */
     protected File outputDirectory;
 
     /**
      * The location for the compiled test classes.
      *
      * @parameter default-value="${project.build.testOutputDirectory}"
      */
     protected File testOutputDirectory;
 
     /**
      * The encoding of source files.
      *
      * @parameter default-value="${project.build.sourceEncoding}"
      */
     protected String sourceEncoding;
 
     // if plugin only runs on 1.5, then can assume 1.5
     /**
      * The Groovy compiler bytecode compatibility ("1.4" or "1.5").
      *
      * @parameter default-value="1.5"
      */
 //    protected String targetBytecode;
 
     /**
      * Whether Groovy compiler should be set to debug.
      *
      * @parameter default-value="false"
      */
     protected boolean debug;
 
     /**
      * Whether Groovy compiler should be set to verbose.
      *
      * @parameter default-value="false"
      */
     protected boolean verbose;
 
     /**
      * Groovy compiler warning level.  Should be one of:
      * <ul>
      *   <li>"0" (None)</li>
      *   <li>"1" (Likely Errors)</li>
      *   <li>"2" (Possible Errors)</li>
      *   <li>"3" (Paranoia)</li>
      * </ul>
      *
      * @parameter default-value="0"@
      */
     protected int warningLevel;
 
     /**
      * Groovy compiler error tolerance
      * (the number of non-fatal errors (per unit) that should be tolerated before compilation is aborted).
      *
      * @parameter default-value="0"
      */
     protected int tolerance;
 
     /**
      * Allow setting whether to support invokeDynamic (requires Java 7 or greater).
      *
      * @parameter property="invokeDynamic" default-value="false"
      */
     private boolean invokeDynamic;
 
     /**
      * Performs compilation of compile mojos.
      *
      * @param sourcesToCompile The sources to compile
      * @param classpath The classpath to use for compilation
      * @param mavenBuildOutputDirectory Maven's build output directory
      * @param compileOutputDirectory The directory to write the compiled class files to
      * @throws ClassNotFoundException When a class needed for compilation cannot be found
      * @throws InstantiationException When a class needed for compilation cannot be instantiated
      * @throws IllegalAccessException When a method needed for compilation cannot be accessed
      * @throws InvocationTargetException When a reflection invocation needed for compilation cannot be completed
      * @throws java.net.MalformedURLException When a classpath element provides a malformed URL
      */
     @SuppressWarnings("unchecked")
     protected synchronized void doCompile(final Set<File> sourcesToCompile, final List classpath, final String mavenBuildOutputDirectory, final File compileOutputDirectory)
             throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, MalformedURLException {
         // get classes we need with reflection
         Class<?> compilerConfigurationClass = Class.forName("org.codehaus.groovy.control.CompilerConfiguration");
         Class<?> compilationUnitClass = Class.forName("org.codehaus.groovy.control.CompilationUnit");
         Class<?> groovyClassLoaderClass = Class.forName("groovy.lang.GroovyClassLoader");
 
         // set up compile options
         Object compilerConfiguration = ReflectionUtils.invokeConstructor(ReflectionUtils.findConstructor(compilerConfigurationClass));
         ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "setDebug", boolean.class), compilerConfiguration, debug);
         ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "setVerbose", boolean.class), compilerConfiguration, verbose);
         ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "setWarningLevel", int.class), compilerConfiguration, warningLevel);
         ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "setTolerance", int.class), compilerConfiguration, tolerance);
         if (Version.parseFromString(getGroovyVersion()).compareTo(new Version(1, 5, 0)) >= 0) {
             // if plugin only runs on 1.5, then can assume 1.5
 //            ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "setTargetBytecode", String.class), compilerConfiguration, targetBytecode);
             // compilerConfiguration.setTargetBytecode("1.5");
             ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "setTargetBytecode", String.class), compilerConfiguration, "1.5");
         }
         if (sourceEncoding != null) {
             ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "setSourceEncoding", String.class), compilerConfiguration, sourceEncoding);
         }
         ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "setTargetDirectory", String.class), compilerConfiguration, compileOutputDirectory.getAbsolutePath());
         if (Version.parseFromString(getGroovyVersion()).compareTo(new Version(2, 0, 0, "beta-3")) >= 0 && invokeDynamic) {
             if (isGroovyIndy()) {
                 Map<java.lang.String, java.lang.Boolean> optimizationOptions = (Map<String, Boolean>) ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilerConfigurationClass, "getOptimizationOptions"), compilerConfiguration);
                 optimizationOptions.put("indy", true);
                 optimizationOptions.put("int", false);
             } else {
                 getLog().warn("Requested to use InvokeDynamic option but the version of Groovy on the project classpath doesn't support it.  Ignoring invokeDynamic option.");
             }
         }
 
         // append project classpath to groovyClassLoader and transformLoader
         ClassLoader parent = ClassLoader.getSystemClassLoader();
         Object groovyClassLoader = ReflectionUtils.invokeConstructor(ReflectionUtils.findConstructor(groovyClassLoaderClass, ClassLoader.class, compilerConfigurationClass), parent, compilerConfiguration);
         Object transformLoader = ReflectionUtils.invokeConstructor(ReflectionUtils.findConstructor(groovyClassLoaderClass, ClassLoader.class), getClass().getClassLoader());
         getLog().debug("Classpath: ");
         ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(groovyClassLoaderClass, "addClasspath", String.class), groovyClassLoader, mavenBuildOutputDirectory);
         getLog().debug("    " + mavenBuildOutputDirectory);
         if (classpath != null) {
             for (Object classpathElement : classpath) {
                 ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(groovyClassLoaderClass, "addURL", URL.class), groovyClassLoader, new File((String) classpathElement).toURI().toURL());
                 ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(groovyClassLoaderClass, "addURL", URL.class), transformLoader, new File((String) classpathElement).toURI().toURL());
                 getLog().debug("    " + classpathElement);
             }
         }
 
         // add Groovy sources
         Object compilationUnit = ReflectionUtils.invokeConstructor(ReflectionUtils.findConstructor(compilationUnitClass, compilerConfigurationClass, CodeSource.class, groovyClassLoaderClass, groovyClassLoaderClass), compilerConfiguration, null, groovyClassLoader, transformLoader);
         getLog().debug("Adding Groovy to compile:");
         for (File source : sourcesToCompile) {
             getLog().debug("    " + source);
             ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilationUnitClass, "addSource", File.class), compilationUnit, source);
         }
 
         // compile the classes
         ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilationUnitClass, "compile"), compilationUnit);
 
         // log compiled classes
         List classes = (List) ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(compilationUnitClass, "getClasses"), compilationUnit);
        getLog().info("Compiled " + classes.size() + " classes.");
     }
 
 }
