 package org.jetbrains.jps.cabal;
 
import org.jetbrains.annotations.NotNull;

 import java.io.File;
 import java.io.IOException;
 
 
 public class CabalJspInterface{
    @NotNull
     private File myCabalFile;
 
    CabalJspInterface(@NotNull File cabalFile) {
         myCabalFile = cabalFile;
     }
 
     private Process runCommand(String command) throws IOException {
         return new ProcessWrapper(myCabalFile.getParentFile().getCanonicalPath()).
                             getProcess("cabal", command);
     }
 
     public Process configure() throws IOException {
         return runCommand("configure");
     }
 
     public Process build() throws IOException {
         return runCommand("build");
     }
 
     public Process clean() throws IOException {
         return runCommand("clean");
     }
 
 
 }
