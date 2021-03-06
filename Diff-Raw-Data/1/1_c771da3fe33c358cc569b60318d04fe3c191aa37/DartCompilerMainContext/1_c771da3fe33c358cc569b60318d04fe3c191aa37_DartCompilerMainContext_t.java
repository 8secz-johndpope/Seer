 // Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
 // for details. All rights reserved. Use of this source code is governed by a
 // BSD-style license that can be found in the LICENSE file.
 
 package com.google.dart.compiler;
 
 import com.google.dart.compiler.ast.DartUnit;
 import com.google.dart.compiler.ast.LibraryUnit;
 import com.google.dart.compiler.metrics.CompilerMetrics;
 import com.google.dart.compiler.parser.DartParser;
 import com.google.dart.compiler.resolver.ResolverErrorCode;
 
 import java.io.IOException;
 import java.io.Reader;
 import java.io.Writer;
 import java.net.URI;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicInteger;
 
 /**
  * An overall context for the Dart compiler providing an adapter and forwarding
  * mechanism for to both {@link DartArtifactProvider} and
  * {@link DartCompilerListener}. This is an internal compiler construct and as
  * such should not be instantiated or subclassed by those outside the compiler
  * itself.
  */
 final class DartCompilerMainContext implements DartCompilerListener, DartCompilerContext {
 
   private final LibrarySource lib;
   private final DartArtifactProvider provider;
   private final DartCompilerListener listener;
   private final AtomicInteger errorCount = new AtomicInteger(0);
   private final AtomicInteger warningCount = new AtomicInteger(0);
   private final AtomicInteger typeErrorCount = new AtomicInteger(0);
   private final AtomicBoolean filesHaveChanged = new AtomicBoolean();
   // declared volatile for thread-safety
   private volatile LibraryUnit appLibraryUnit = null;
 
   private final CompilerConfiguration compilerConfiguration;
 
   DartCompilerMainContext(LibrarySource lib, DartArtifactProvider provider,
       DartCompilerListener listener,
       CompilerConfiguration compilerConfiguration) {
     this.lib = lib;
     this.provider = provider;
     this.listener = listener;
     this.compilerConfiguration = compilerConfiguration;
   }
 
   @Override
   public void onError(DartCompilationError event) {
     if (event.getErrorCode().getSubSystem() == SubSystem.STATIC_TYPE) {
       incrementTypeErrorCount();
     } else if (shouldWarnOnNoSuchType() && event.getErrorCode() == ResolverErrorCode.NO_SUCH_TYPE) {
       incrementTypeErrorCount();
     } else if (event.getErrorCode().getErrorSeverity() == ErrorSeverity.ERROR) {
       incrementErrorCount();
     } else if (event.getErrorCode().getErrorSeverity() == ErrorSeverity.WARNING) {
       incrementWarningCount();
     }
     listener.onError(event);
   }
 
   @Override
   public LibraryUnit getApplicationUnit() {
     return getAppLibraryUnit();
   }
 
   @Override
   public LibraryUnit getAppLibraryUnit() {
     // use double-checked looking pattern with use of volatile
     if (appLibraryUnit == null) {
       synchronized (this) {
         if (appLibraryUnit == null) {
           try {
             appLibraryUnit =
                 DartParser.getSourceParser(lib, listener).preProcessLibraryDirectives(lib);
           } catch (IOException e) {
             onError(new DartCompilationError(lib, DartCompilerErrorCode.IO, e.getMessage()));
             return null;
           }
         }
       }
     }
     return appLibraryUnit;
   }
 
   @Override
   public Reader getArtifactReader(Source source, String part, String extension)
       throws IOException {
     return provider.getArtifactReader(source, part, extension);
   }
 
   @Override
   public URI getArtifactUri(DartSource source, String part, String extension) {
     return provider.getArtifactUri(source, part, extension);
   }
 
   @Override
   public Writer getArtifactWriter(Source source, String part, String extension)
       throws IOException {
     return provider.getArtifactWriter(source, part, extension);
   }
 
   public int getErrorCount() {
     return errorCount.get();
   }
 
   public int getWarningCount() {
     return warningCount.get();
   }
 
   public int getTypeErrorCount() {
     return typeErrorCount.get();
   }
 
   @Override
   public LibraryUnit getLibraryUnit(LibrarySource libSrc) {
     if (libSrc == lib) {
       return getApplicationUnit();
     }
     try {
       return DartParser.getSourceParser(libSrc, listener).preProcessLibraryDirectives(libSrc);
     } catch (IOException e) {
       onError(new DartCompilationError(libSrc, DartCompilerErrorCode.IO, e.getMessage()));
       return null;
     }
   }
 
   @Override
   public boolean isOutOfDate(Source source, Source base, String extension) {
     return provider.isOutOfDate(source, base, extension);
   }
 
   protected void incrementErrorCount() {
     errorCount.incrementAndGet();
   }
 
   protected void incrementWarningCount() {
     warningCount.incrementAndGet();
   }
 
   protected void incrementTypeErrorCount() {
     typeErrorCount.incrementAndGet();
   }
 
   @Override
   public CompilerMetrics getCompilerMetrics() {
     return compilerConfiguration.getCompilerMetrics();
   }
 
   public void setFilesHaveChanged() {
     filesHaveChanged.set(true);
   }
 
   public boolean getFilesHaveChanged() {
     return filesHaveChanged.get();
   }
 
   @Override
   public boolean shouldWarnOnNoSuchType() {
     return compilerConfiguration.shouldWarnOnNoSuchType();
   }
 
   @Override
   public CompilerConfiguration getCompilerConfiguration() {
     return compilerConfiguration;
   }
 
   /**
    * Return the system library corresponding to the specified "dart:<libname>" spec.
    */
   @Override
   public LibrarySource getSystemLibraryFor(String importSpec) {
     return compilerConfiguration.getSystemLibraryFor(importSpec);
   }
 
   @Override
   public void unitAboutToCompile(DartSource source, boolean diet) {
    listener.unitAboutToCompile(source, diet);
   }
 
   @Override
   public void unitCompiled(DartUnit unit) {
     listener.unitCompiled(unit);
   }
 }
