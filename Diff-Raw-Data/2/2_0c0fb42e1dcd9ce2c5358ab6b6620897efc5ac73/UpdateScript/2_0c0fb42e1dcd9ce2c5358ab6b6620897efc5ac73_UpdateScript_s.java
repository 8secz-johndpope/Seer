 package net.ocheyedan.ply.script;
 
 import net.ocheyedan.ply.FileUtil;
 import net.ocheyedan.ply.Output;
 import net.ocheyedan.ply.PlyUtil;
 import net.ocheyedan.ply.SystemExit;
 import net.ocheyedan.ply.props.Context;
 import net.ocheyedan.ply.props.Filter;
 import net.ocheyedan.ply.props.Props;
 
 import java.io.*;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLConnection;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 /**
  * User: blangel
  * Date: 2/2/12
  * Time: 9:41 AM
  *
  * Used to update ply itself.
  */
 public final class UpdateScript {
 
     public static void main(String[] args) {
         String updateUrl = Props.getValue(Context.named("ply"), "update.url");
         if (updateUrl.isEmpty()) {
             Output.print("^error^ No ^b^ply.update.url^r^ property specified, cannot update ply.");
             System.exit(1);
         }
         Map<String, List<String>> updateInstructions = null;
         try {
             updateInstructions = downloadUpdateInstr(updateUrl);
         } catch (SystemExit se) {
             System.exit(se.exitCode);
         }
         if ((updateInstructions == null) || !updateInstructions.containsKey("VERSIONS")) {
             Output.print("^error^ Could not download the update instructions file. Cannot update ply.");
             System.exit(1);
         }
         String currentVersion = System.getProperty("ply.version");
         if ((currentVersion == null) || currentVersion.isEmpty() || !updateInstructions.containsKey(currentVersion)) {
             Output.print("^error^ Current ply version [ ^b^%s^r^ ] not supported for automatic update. Please manually update by reinstalling ply.",
                     currentVersion);
             System.exit(1);
         }
         update(updateInstructions, currentVersion);
     }
 
     static void update(Map<String, List<String>> updateInstructions, String currentVersion) {
         List<String> versions = updateInstructions.get("VERSIONS");
         int currentVersionIndex = versions.indexOf(currentVersion);
         int numberOfUpdates = (versions.size() - 1) - currentVersionIndex;
         if (numberOfUpdates == 0) {
             Output.print("No updates found, ply is up to date^green^!^r^");
             return;
         } else {
             Output.print("Found %d updates to ply.", numberOfUpdates);
         }
         String plyHome = PlyUtil.INSTALL_DIRECTORY;
         File plyHomeDir;
         if ((plyHome == null) || !(plyHomeDir = new File(plyHome)).exists()) {
             Output.print("^error^ Could not find the PLY_HOME environment variable. Cannot update ply.");
             throw new SystemExit(1);
         }
         File backupTar = backup(plyHomeDir, currentVersion);
         try {
             for (int i = currentVersionIndex; i < versions.size(); i++) {
                 String version = versions.get(i);
                 List<String> instructions = updateInstructions.get(version);
                 update(version, instructions);
             }
             Output.print("Successfully updated ply to most recent version [ ^b^%s^r^ ]^green^!^r^", versions.get(versions.size() - 1));
         } catch (Exception e) {
             Output.print(e);
             restore(plyHomeDir, backupTar, currentVersion);
             throw new SystemExit(1, e);
         }
     }
 
     /**
      * Updates {@code version} by executing each instruction within {@code instructions}
      * @param version to update
      * @param instructions the instructions necessary for the update
      */
     static void update(String version, List<String> instructions) {
         Output.print("^info^ Updating ^b^%s^r^", version);
         for (String instruction : instructions) {
             if (instruction.startsWith("OUTPUT=")) {
                 String output = instruction.substring("OUTPUT=".length());
                 Output.print("^dbug^ %s", output);
             } else {
                 String filteredInstruction = Filter.filter(Context.named("ply"), instruction, Props.get());
                 if (!instruction.equals(filteredInstruction)) {
                     Output.print("^dbug^ Filtered instruction to ^b^%s^r^", filteredInstruction);
                 }
                 executeInstruction(filteredInstruction);
             }
         }
     }
 
     /**
      * Executes {@code instruction}
      * @param instruction to execute
      */
     static void executeInstruction(String instruction) {
         ProcessBuilder builder = createProcess(instruction);
         try {
             Process tar = builder.start();
             tar.waitFor();
         } catch (IOException ioe) {
             Output.print(ioe);
             throw new SystemExit(1, ioe);
         } catch (InterruptedException ie) {
             Output.print(ie);
             throw new SystemExit(1, ie);
         }
     }
 
     /**
      * Tars the contents of {@code plyHomeDir} and returns the file
      * @param plyHomeDir to tar
      * @param currentVersion the current version of ply, used in the backup tar name
      * @return the tarred file
      */
     static File backup(File plyHomeDir, String currentVersion) {
         Output.print("^dbug^ Backing up current ply installation.");
         String tmpDir = System.getProperty("java.io.tmpdir");
         File outputFile = FileUtil.fromParts(tmpDir, "ply-" + currentVersion + ".tar");
         ProcessBuilder builder = createProcess(String.format("tar cvzf %s %s", outputFile.getPath(), plyHomeDir.getPath()));
         try {
             Process tar = builder.start();
             tar.waitFor();
         } catch (IOException ioe) {
             Output.print(ioe);
             throw new SystemExit(1, ioe);
         } catch (InterruptedException ie) {
             Output.print(ie);
             throw new SystemExit(1, ie);
         }
         return outputFile;
     }
 
     /**
      * Un-tars {@code backupTar} to {@code plyHomeDir}
      * @param plyHomeDir the directory in which to un-tar {@code backupTar}
      * @param backupTar the backup tar which to un-tar to {@code plyHomeDir}
      * @param currentVersion for which to revert
      */
     static void restore(File plyHomeDir, File backupTar, String currentVersion) {
         Output.print("Restoring ply to version ^b^%s^r^", currentVersion);
         ProcessBuilder builder = createProcess(String.format("tar xvzf %s %s", backupTar.getPath(), plyHomeDir.getPath()));
         try {
             Process tar = builder.start();
             tar.waitFor();
         } catch (IOException ioe) {
             Output.print(ioe);
             throw new SystemExit(1, ioe);
         } catch (InterruptedException ie) {
             Output.print(ie);
             throw new SystemExit(1, ie);
         }
     }
 
     static ProcessBuilder createProcess(String execution) {
         String shell = Props.getValue(Context.named("scripts-sh"), "shell");
         String shellArgs = Props.getValue(Context.named("scripts-sh"), "shell.args");
         if (shell.isEmpty()) {
             Output.print("^error^ No ^b^shell^r^ property defined (^b^ply set shell=xxxx in scripts-sh^r^).");
             throw new SystemExit(1);
         }
         String[] args = new String[3];
         args[0] = shell;
         args[1] = shellArgs;
        args[3] = execution;
         return new ProcessBuilder(args).redirectErrorStream(true);
     }
 
     /**
      * Downloads the update instructions file from {@literal ply.update.url} and parses it into a mapping
      * from version to update-instructions for the next version.
      * @param updateUrl the url from which to download the update instructions file
      * @return a mapping of version to the update-instructions to move it to the next version
      *         additionally a mapping from {@literal VERSIONS} to the ordered list of versions
      * @throws SystemExit on error
      */
     static Map<String, List<String>> downloadUpdateInstr(String updateUrl) throws SystemExit {
         Map<String, List<String>> updateInstructions = new HashMap<String, List<String>>();
         InputStream stream = null;
         try {
             Output.print("^info^ Downloading update instructions.");
             URL url = new URL(updateUrl);
             URLConnection connection = url.openConnection();
             connection.setConnectTimeout(1000);
             stream = connection.getInputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(stream));
             String line;
             List<String> versions = new ArrayList<String>();
             List<String> instructions = null;
             while ((line = in.readLine()) != null) {
                 line = line.trim();
                 if (line.isEmpty() ||   line.startsWith("#")) {
                     continue; // ignore comments and empty lines
                 }
                 if (line.startsWith("VERSION=")) {
                     String version = line.substring("VERSION=".length());
                     versions.add(version);
                     instructions = new ArrayList<String>();
                     updateInstructions.put(version, instructions);
                 } else {
                     if (instructions == null) {
                         Output.print("^error^ The update instructions file is in an invalid format. Cannot update ply.");
                         throw new SystemExit(1);
                     }
                     instructions.add(line);
                 }
             }
             updateInstructions.put("VERSIONS", versions);
         } catch (MalformedURLException murle) {
             Output.print("^error^ Invalid url specified in ^b^ply.update.url^r^ property: %s", updateUrl);
             Output.print(murle);
             throw new SystemExit(1, murle);
         } catch (FileNotFoundException fnfe) {
             fnfe = new FileNotFoundException("Unable to download " + fnfe.getMessage());
             Output.print(fnfe);
             throw new SystemExit(1, fnfe);
         } catch (IOException ioe) {
             Output.print(ioe);
             throw new SystemExit(1, ioe);
         } finally {
             if (stream != null) {
                 try {
                     stream.close();
                 } catch (IOException ioe) {
                     Output.print(ioe);
                     throw new SystemExit(1, ioe);
                 }
             }
         }
         return updateInstructions;
     }
 
 }
