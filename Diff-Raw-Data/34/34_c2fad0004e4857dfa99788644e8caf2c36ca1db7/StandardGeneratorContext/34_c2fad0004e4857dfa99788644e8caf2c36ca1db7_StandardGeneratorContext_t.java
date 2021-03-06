 /*
  * Copyright 2007 Google Inc.
  * 
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  * 
  * http://www.apache.org/licenses/LICENSE-2.0
  * 
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 package com.google.gwt.dev.shell;
 
 import com.google.gwt.core.ext.Generator;
 import com.google.gwt.core.ext.GeneratorContext;
 import com.google.gwt.core.ext.PropertyOracle;
 import com.google.gwt.core.ext.TreeLogger;
 import com.google.gwt.core.ext.UnableToCompleteException;
 import com.google.gwt.core.ext.linker.Artifact;
 import com.google.gwt.core.ext.linker.ArtifactSet;
 import com.google.gwt.core.ext.linker.GeneratedResource;
 import com.google.gwt.core.ext.linker.impl.StandardGeneratedResource;
 import com.google.gwt.core.ext.typeinfo.JClassType;
 import com.google.gwt.core.ext.typeinfo.TypeOracle;
 import com.google.gwt.dev.cfg.PublicOracle;
 import com.google.gwt.dev.javac.CompilationState;
 import com.google.gwt.dev.javac.CompilationUnit;
 import com.google.gwt.dev.javac.impl.FileCompilationUnit;
 import com.google.gwt.dev.util.DiskCache;
 import com.google.gwt.dev.util.Util;
 import com.google.gwt.dev.util.collect.HashSet;
 import com.google.gwt.dev.util.collect.IdentityHashMap;
 
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.SortedSet;
 
 /**
  * Manages generators and generated units during a single compilation.
  */
 public class StandardGeneratorContext implements GeneratorContext {
 
   /**
    * Extras added to {@link CompilationUnit}.
    */
   interface Generated {
     void abort();
 
     void commit();
 
     String getTypeName();
   }
 
   /**
    * This compilation unit acts as a normal compilation unit as well as a buffer
    * into which generators can write their source. A controller should ensure
    * that source isn't requested until the generator has finished writing it.
    * This version is backed by {@link StandardGeneratorContext#diskCache}.
    */
   private static class GeneratedUnit extends CompilationUnit implements
       Generated {
 
     /**
      * A token to retrieve this object's bytes from the disk cache.
      */
     private long cacheToken;
 
     private long creationTime;
 
     private StringWriter sw;
 
     private final String typeName;
 
     public GeneratedUnit(StringWriter sw, String typeName) {
       this.typeName = typeName;
       this.sw = sw;
     }
 
     public void abort() {
       sw = null;
     }
 
     /**
      * Finalizes the source and adds this compilation unit to the host.
      */
     public void commit() {
       cacheToken = diskCache.writeString(sw.toString());
       sw = null;
       creationTime = System.currentTimeMillis();
     }
 
     @Override
     public String getDisplayLocation() {
       return "transient source for " + typeName;
     }
 
     @Override
     public long getLastModified() {
       return creationTime;
     }
 
     @Override
     public String getSource() {
       if (sw != null) {
         throw new IllegalStateException("source not committed");
       }
       return diskCache.readString(cacheToken);
     }
 
     @Override
     public String getTypeName() {
       return typeName;
     }
 
     @Override
     public boolean isGenerated() {
       return true;
     }
 
     @Override
     public boolean isSuperSource() {
       return false;
     }
   }
 
   /**
    * This compilation unit acts as a normal compilation unit as well as a buffer
    * into which generators can write their source. A controller should ensure
    * that source isn't requested until the generator has finished writing it.
    * This version is backed by an explicit generated file.
    */
   private static class GeneratedUnitWithFile extends FileCompilationUnit
       implements Generated {
 
     private PrintWriter pw;
    private String strongHash; // cache so that refreshes work correctly
 
     public GeneratedUnitWithFile(File file, PrintWriter pw, String packageName) {
       super(file, packageName);
       this.pw = pw;
     }
 
     public void abort() {
       pw.close();
       pw = null;
     }
 
     public void commit() {
       pw.close();
       pw = null;
      strongHash = Util.computeStrongName(getSource().getBytes());
     }
 
     @Override
     public String getSource() {
       if (pw != null) {
         throw new IllegalStateException("source not committed");
       }
       return super.getSource();
     }
 
    /**
     * The old source is not preserved across refreshes. We use a strongHash to
     * avoid the memory overhead of storing the source.
     */
    @Override
    public String getStrongHash() {
      return strongHash;
    }

     @Override
     public boolean isGenerated() {
       return true;
     }
 
     @Override
     public boolean isSuperSource() {
       return false;
     }
   }
 
   /**
    * Manages a resource that is in the process of being created by a generator.
    */
   private static class PendingResource {
 
     private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
     private final String partialPath;
     private final File pendingFile;
 
     public PendingResource(File outDir, String partialPath) {
       this.partialPath = partialPath;
       this.pendingFile = new File(outDir, partialPath);
     }
 
     public void commit(TreeLogger logger) throws UnableToCompleteException {
       logger = logger.branch(TreeLogger.TRACE, "Writing generated resource '"
           + pendingFile.getAbsolutePath() + "'", null);
 
       Util.writeBytesToFile(logger, pendingFile, baos.toByteArray());
     }
 
     public File getFile() {
       return pendingFile;
     }
 
     public OutputStream getOutputStream() {
       return baos;
     }
 
     public String getPartialPath() {
       return partialPath;
     }
   }
 
   private static DiskCache diskCache;
 
   private final ArtifactSet allGeneratedArtifacts;
 
   private final Set<CompilationUnit> committedGeneratedCups = new HashSet<CompilationUnit>();
 
   private final CompilationState compilationState;
 
   private Class<? extends Generator> currentGenerator;
 
   private final File genDir;
 
   private final File generatorResourcesDir;
 
   private ArtifactSet newlyGeneratedArtifacts = new ArtifactSet();
 
   private final Set<String> newlyGeneratedTypeNames = new HashSet<String>();
 
   private final Map<OutputStream, PendingResource> pendingResourcesByOutputStream = new IdentityHashMap<OutputStream, PendingResource>();
 
   private final PropertyOracle propOracle;
 
   private final PublicOracle publicOracle;
 
   private final Map<PrintWriter, Generated> uncommittedGeneratedCupsByPrintWriter = new IdentityHashMap<PrintWriter, Generated>();
 
   /**
    * Normally, the compiler host would be aware of the same types that are
    * available in the supplied type oracle although it isn't strictly required.
    */
   public StandardGeneratorContext(CompilationState compilationState,
       PropertyOracle propOracle, PublicOracle publicOracle, File genDir,
       File generatorResourcesDir, ArtifactSet allGeneratedArtifacts) {
     this.compilationState = compilationState;
     this.propOracle = propOracle;
     this.publicOracle = publicOracle;
     this.genDir = genDir;
     this.generatorResourcesDir = generatorResourcesDir;
     this.allGeneratedArtifacts = allGeneratedArtifacts;
     if (genDir == null && diskCache == null) {
       diskCache = new DiskCache();
     }
   }
 
   /**
    * Commits a pending generated type.
    */
   public final void commit(TreeLogger logger, PrintWriter pw) {
     Generated gcup = uncommittedGeneratedCupsByPrintWriter.get(pw);
     if (gcup != null) {
       gcup.commit();
       uncommittedGeneratedCupsByPrintWriter.remove(pw);
       committedGeneratedCups.add((CompilationUnit) gcup);
     } else {
       logger.log(TreeLogger.WARN,
           "Generator attempted to commit an unknown PrintWriter", null);
     }
   }
 
   /**
    * Adds an Artifact to the ArtifactSet if one has been provided to the
    * context.
    */
   public void commitArtifact(TreeLogger logger, Artifact<?> artifact)
       throws UnableToCompleteException {
     allGeneratedArtifacts.replace(artifact);
     newlyGeneratedArtifacts.add(artifact);
   }
 
   public GeneratedResource commitResource(TreeLogger logger, OutputStream os)
       throws UnableToCompleteException {
 
     // Find the pending resource using its output stream as a key.
     PendingResource pendingResource = pendingResourcesByOutputStream.get(os);
     if (pendingResource != null) {
       // Actually write the bytes to disk.
       pendingResource.commit(logger);
 
       // Add the GeneratedResource to the ArtifactSet
       GeneratedResource toReturn = new StandardGeneratedResource(
           currentGenerator, pendingResource.getPartialPath(),
           pendingResource.getFile());
       commitArtifact(logger, toReturn);
 
       /*
        * The resource is now no longer pending, so remove it from the map. If
        * the commit above throws an exception, it's okay to leave the entry in
        * the map because it will be reported later as not having been committed,
        * which is accurate.
        */
       pendingResourcesByOutputStream.remove(os);
 
       return toReturn;
     } else {
       logger.log(TreeLogger.WARN,
           "Generator attempted to commit an unknown OutputStream", null);
       throw new UnableToCompleteException();
     }
   }
 
   /**
    * Call this whenever generators are known to not be running to clear out
    * uncommitted compilation units and to force committed compilation units to
    * be parsed and added to the type oracle.
    * 
    * @return any newly generated artifacts since the last call
    */
   public final ArtifactSet finish(TreeLogger logger)
       throws UnableToCompleteException {
 
     abortUncommittedResources(logger);
 
     // Process pending generated types.
     List<String> genTypeNames = new ArrayList<String>();
 
     try {
       TreeLogger branch;
       if (!committedGeneratedCups.isEmpty()) {
         // Assimilate the new types into the type oracle.
         //
         String msg = "Assimilating generated source";
         branch = logger.branch(TreeLogger.DEBUG, msg, null);
 
         TreeLogger subBranch = null;
         if (branch.isLoggable(TreeLogger.DEBUG)) {
           subBranch = branch.branch(TreeLogger.DEBUG,
               "Generated source files...", null);
         }
 
         for (CompilationUnit gcup : committedGeneratedCups) {
           String qualifiedTypeName = gcup.getTypeName();
           genTypeNames.add(qualifiedTypeName);
           if (subBranch != null) {
             subBranch.log(TreeLogger.DEBUG, gcup.getDisplayLocation(), null);
           }
         }
 
         compilationState.addGeneratedCompilationUnits(logger,
             committedGeneratedCups);
       }
 
       // Make sure all generated types can be found in TypeOracle.
       TypeOracle typeOracle = getTypeOracle();
       for (String genTypeName : genTypeNames) {
         if (typeOracle.findType(genTypeName) == null) {
           String msg = "Unable to find recently-generated type '" + genTypeName;
           logger.log(TreeLogger.ERROR, msg, null);
           throw new UnableToCompleteException();
         }
       }
       return newlyGeneratedArtifacts;
     } finally {
 
       // Remind the user if there uncommitted cups.
       if (!uncommittedGeneratedCupsByPrintWriter.isEmpty()) {
         String msg = "For the following type(s), generated source was never committed (did you forget to call commit()?)";
         logger = logger.branch(TreeLogger.WARN, msg, null);
 
         for (Generated unit : uncommittedGeneratedCupsByPrintWriter.values()) {
           logger.log(TreeLogger.WARN, unit.getTypeName(), null);
         }
       }
 
       uncommittedGeneratedCupsByPrintWriter.clear();
       committedGeneratedCups.clear();
       newlyGeneratedTypeNames.clear();
       newlyGeneratedArtifacts = new ArtifactSet();
     }
   }
 
   public final PropertyOracle getPropertyOracle() {
     return propOracle;
   }
 
   public final TypeOracle getTypeOracle() {
     return compilationState.getTypeOracle();
   }
 
   public void setCurrentGenerator(Class<? extends Generator> currentGenerator) {
     this.currentGenerator = currentGenerator;
   }
 
   public final PrintWriter tryCreate(TreeLogger logger, String packageName,
       String simpleTypeName) {
     String typeName;
     if (packageName.length() == 0) {
       typeName = simpleTypeName;
     } else {
       typeName = packageName + '.' + simpleTypeName;
     }
     // Is type already known to the host?
     JClassType existingType = getTypeOracle().findType(packageName,
         simpleTypeName);
     if (existingType != null) {
       logger.log(TreeLogger.DEBUG, "Type '" + typeName
           + "' already exists and will not be re-created ", null);
       return null;
     }
 
     // Has anybody tried to create this type during this iteration?
     if (newlyGeneratedTypeNames.contains(typeName)) {
       final String msg = "A request to create type '"
           + typeName
           + "' was received while the type itself was being created; this might be a generator or configuration bug";
       logger.log(TreeLogger.WARN, msg, null);
       return null;
     }
 
     // The type isn't there, so we can let the caller create it. Remember that
     // it is pending so another attempt to create the same type will fail.
     Generated gcup;
     PrintWriter pw;
     if (this.genDir == null) {
       StringWriter sw = new StringWriter();
       pw = new PrintWriter(sw, true);
       gcup = new GeneratedUnit(sw, typeName);
     } else {
       File dir = new File(genDir, packageName.replace('.', File.separatorChar));
       dir.mkdirs();
       File srcFile = new File(dir, simpleTypeName + ".java");
       if (srcFile.exists()) {
         srcFile.delete();
       }
       try {
         FileOutputStream fos = new FileOutputStream(srcFile);
         // Critical to set the encoding here, or UTF chars get whacked.
         OutputStreamWriter osw = new OutputStreamWriter(fos,
             Util.DEFAULT_ENCODING);
         pw = new PrintWriter(osw);
         gcup = new GeneratedUnitWithFile(srcFile, pw, packageName);
       } catch (IOException e) {
         throw new RuntimeException("Error writing out generated unit at '"
             + srcFile.getAbsolutePath() + "'", e);
       }
     }
     uncommittedGeneratedCupsByPrintWriter.put(pw, gcup);
     newlyGeneratedTypeNames.add(typeName);
     return pw;
   }
 
   public OutputStream tryCreateResource(TreeLogger logger, String partialPath)
       throws UnableToCompleteException {
 
     logger = logger.branch(TreeLogger.DEBUG,
         "Preparing pending output resource '" + partialPath + "'", null);
 
     // Disallow null or empty names.
     if (partialPath == null || partialPath.trim().equals("")) {
       logger.log(TreeLogger.ERROR,
           "The resource name must be a non-empty string", null);
       throw new UnableToCompleteException();
     }
 
     // Disallow absolute paths.
     File f = new File(partialPath);
     if (f.isAbsolute()) {
       logger.log(
           TreeLogger.ERROR,
           "Resource paths are intended to be relative to the compiled output directory and cannot be absolute",
           null);
       throw new UnableToCompleteException();
     }
 
     // Disallow backslashes (to promote consistency in calling code).
     if (partialPath.indexOf('\\') >= 0) {
       logger.log(
           TreeLogger.ERROR,
           "Resource paths must contain forward slashes (not backslashes) to denote subdirectories",
           null);
       throw new UnableToCompleteException();
     }
 
     // Check for public path collision.
     if (publicOracle.findPublicFile(partialPath) != null) {
       logger.log(TreeLogger.WARN, "Cannot create resource '" + partialPath
           + "' because it already exists on the public path", null);
       return null;
     }
 
     // See if the file is already committed.
     SortedSet<GeneratedResource> resources = allGeneratedArtifacts.find(GeneratedResource.class);
     for (GeneratedResource resource : resources) {
       if (partialPath.equals(resource.getPartialPath())) {
         return null;
       }
     }
 
     // See if the file is pending.
     for (Iterator<PendingResource> iter = pendingResourcesByOutputStream.values().iterator(); iter.hasNext();) {
       PendingResource pendingResource = iter.next();
       if (pendingResource.getPartialPath().equals(partialPath)) {
         // It is already pending.
         logger.log(TreeLogger.WARN, "The file '" + partialPath
             + "' is already a pending resource", null);
         return null;
       }
     }
 
     // Record that this file is pending.
     PendingResource pendingResource = new PendingResource(
         generatorResourcesDir, partialPath);
     OutputStream os = pendingResource.getOutputStream();
     pendingResourcesByOutputStream.put(os, pendingResource);
 
     return os;
   }
 
   private void abortUncommittedResources(TreeLogger logger) {
     if (pendingResourcesByOutputStream.isEmpty()) {
       // Nothing to do.
       return;
     }
 
     // Warn the user about uncommitted resources.
     logger = logger.branch(
         TreeLogger.WARN,
         "The following resources will not be created because they were never committed (did you forget to call commit()?)",
         null);
 
     try {
       for (Iterator<PendingResource> iter = pendingResourcesByOutputStream.values().iterator(); iter.hasNext();) {
         PendingResource pendingResource = iter.next();
         logger.log(TreeLogger.WARN,
             pendingResource.getFile().getAbsolutePath(), null);
       }
     } finally {
       pendingResourcesByOutputStream.clear();
     }
   }
 }
