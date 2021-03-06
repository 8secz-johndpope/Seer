 /*
  * [The "BSD licence"]
  * Copyright (c) 2010 Ben Gruver (JesusFreke)
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in the
  *    documentation and/or other materials provided with the distribution.
  * 3. The name of the author may not be used to endorse or promote products
  *    derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
  * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
 
 package org.jf.baksmali;
 
 import org.jf.baksmali.Adaptors.ClassDefinition;
 import org.jf.dexlib.Code.Analysis.ClassPath;
 import org.jf.dexlib.DexFile;
 import org.jf.dexlib.ClassDefItem;
 
 import java.io.*;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class baksmali {
     public static boolean noParameterRegisters = false;
     public static boolean useLocalsDirective = false;
     public static boolean useSequentialLabels = false;
     public static boolean outputDebugInfo = true;
     public static boolean addCodeOffsets = false;
     public static boolean deodex = false;
     public static boolean verify = false;
     public static int registerInfo = 0;
     public static String bootClassPath;
 
     public static void disassembleDexFile(String dexFilePath, DexFile dexFile, boolean deodex, String outputDirectory,
                                           String[] classPathDirs, String bootClassPath, String extraBootClassPath,
                                           boolean noParameterRegisters, boolean useLocalsDirective,
                                           boolean useSequentialLabels, boolean outputDebugInfo, boolean addCodeOffsets,
                                           int registerInfo, boolean verify)
     {
         baksmali.noParameterRegisters = noParameterRegisters;
         baksmali.useLocalsDirective = useLocalsDirective;
         baksmali.useSequentialLabels = useSequentialLabels;
         baksmali.outputDebugInfo = outputDebugInfo;
         baksmali.addCodeOffsets = addCodeOffsets;
         baksmali.deodex = deodex;
         baksmali.registerInfo = registerInfo;
         baksmali.bootClassPath = bootClassPath;
         baksmali.verify = verify;
 
         if (registerInfo != 0 || deodex || verify) {
             try {
                 String[] extraBootClassPathArray = null;
                 if (extraBootClassPath != null && extraBootClassPath.length() > 0) {
                     assert extraBootClassPath.charAt(0) == ':';
                     extraBootClassPathArray = extraBootClassPath.substring(1).split(":");
                 }
 
                 if (dexFile.isOdex() && bootClassPath == null) {
                     //ext.jar is a special case - it is typically the 2nd jar in the boot class path, but it also
                    //depends on classes in framework.jar (typically the 3rd jar in the BCP). If the user didn't
                    //specify a -c option, we should add framework.jar to the boot class path by default, so that it
                    //"just works"
                     if (extraBootClassPathArray == null && isExtJar(dexFilePath)) {
                        extraBootClassPathArray = new String[] {"framework.jar"};
                     }
                     ClassPath.InitializeClassPathFromOdex(classPathDirs, extraBootClassPathArray, dexFilePath, dexFile);
                 } else {
                     String[] bootClassPathArray = null;
                     if (bootClassPath != null) {
                         bootClassPathArray = bootClassPath.split(":");
                     }
                     ClassPath.InitializeClassPath(classPathDirs, bootClassPathArray, extraBootClassPathArray,
                             dexFilePath, dexFile);
                 }
             } catch (Exception ex) {
                 System.err.println("\n\nError occured while loading boot class path files. Aborting.");
                 ex.printStackTrace(System.err);
                 System.exit(1);
             }
         }
 
         File outputDirectoryFile = new File(outputDirectory);
         if (!outputDirectoryFile.exists()) {
             if (!outputDirectoryFile.mkdirs()) {
                 System.err.println("Can't create the output directory " + outputDirectory);
                 System.exit(1);
             }
         }
 
         for (ClassDefItem classDefItem: dexFile.ClassDefsSection.getItems()) {
             /**
              * The path for the disassembly file is based on the package name
              * The class descriptor will look something like:
              * Ljava/lang/Object;
              * Where the there is leading 'L' and a trailing ';', and the parts of the
              * package name are separated by '/'
              */
 
             String classDescriptor = classDefItem.getClassType().getTypeDescriptor();
 
             //validate that the descriptor is formatted like we expect
             if (classDescriptor.charAt(0) != 'L' ||
                 classDescriptor.charAt(classDescriptor.length()-1) != ';') {
                 System.err.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
                 continue;
             }
 
             //trim off the leading L and trailing ;
             classDescriptor = classDescriptor.substring(1, classDescriptor.length()-1);
 
             //trim off the leading 'L' and trailing ';', and get the individual package elements
             String[] pathElements = classDescriptor.split("/");
 
             //build the path to the smali file to generate for this class
             StringBuilder smaliPath = new StringBuilder(outputDirectory);
             for (String pathElement: pathElements) {
                 smaliPath.append(File.separatorChar);
                 smaliPath.append(pathElement);
             }
             smaliPath.append(".smali");
 
             File smaliFile = new File(smaliPath.toString());
 
             //create and initialize the top level string template
             ClassDefinition classDefinition = new ClassDefinition(classDefItem);
 
             //write the disassembly
             Writer writer = null;
             try
             {
                 File smaliParent = smaliFile.getParentFile();
                 if (!smaliParent.exists()) {
                     if (!smaliParent.mkdirs()) {
                         System.err.println("Unable to create directory " + smaliParent.toString() + " - skipping class");
                         continue;
                     }
                 }
 
                 if (!smaliFile.exists()){
                     if (!smaliFile.createNewFile()) {
                         System.err.println("Unable to create file " + smaliFile.toString() + " - skipping class");
                         continue;
                     }
                 }
 
                 BufferedWriter bufWriter = new BufferedWriter(new FileWriter(smaliFile));
 
                 writer = new IndentingWriter(bufWriter);
                 classDefinition.writeTo((IndentingWriter)writer);
             } catch (Exception ex) {
                 System.err.println("\n\nError occured while disassembling class " + classDescriptor.replace('/', '.') + " - skipping class");
                 ex.printStackTrace();
             }
             finally
             {
                 if (writer != null) {
                     try {
                         writer.close();
                     } catch (Throwable ex) {
                         System.err.println("\n\nError occured while closing file " + smaliFile.toString());
                         ex.printStackTrace();
                     }
                 }
             }
 
             //TODO: GROT
             if (classDefinition.hadValidationErrors()) {
                 System.exit(1);
             }
         }
     }
 
     private static final Pattern extJarPattern = Pattern.compile("(?:^|\\\\|/)ext.(?:jar|odex)$");
     private static boolean isExtJar(String dexFilePath) {
         Matcher m = extJarPattern.matcher(dexFilePath);
         return m.find();
     }
 }
