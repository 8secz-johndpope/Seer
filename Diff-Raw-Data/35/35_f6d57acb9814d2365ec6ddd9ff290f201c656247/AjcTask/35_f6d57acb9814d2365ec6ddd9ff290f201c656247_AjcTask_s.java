 /* *******************************************************************
  * Copyright (c) 2001-2001 Xerox Corporation, 
  *               2002 Palo Alto Research Center, Incorporated (PARC).
  * All rights reserved. 
  * This program and the accompanying materials are made available 
  * under the terms of the Common Public License v1.0 
  * which accompanies this distribution and is available at 
  * http://www.eclipse.org/legal/cpl-v10.html 
  *  
  * Contributors: 
  *     Xerox/PARC     initial implementation 
  * ******************************************************************/
 
 
 package org.aspectj.tools.ant.taskdefs;
 
 import org.apache.tools.ant.BuildException;
 import org.apache.tools.ant.Location;
 import org.apache.tools.ant.Project;
 import org.apache.tools.ant.taskdefs.Copy;
 import org.apache.tools.ant.taskdefs.Execute;
 import org.apache.tools.ant.taskdefs.Expand;
 import org.apache.tools.ant.taskdefs.Javac;
 import org.apache.tools.ant.taskdefs.LogStreamHandler;
 import org.apache.tools.ant.taskdefs.MatchingTask;
 import org.apache.tools.ant.taskdefs.Zip;
 import org.apache.tools.ant.types.Commandline;
 import org.apache.tools.ant.types.CommandlineJava;
 import org.apache.tools.ant.types.FileSet;
 import org.apache.tools.ant.types.Path;
 import org.apache.tools.ant.types.PatternSet;
 import org.apache.tools.ant.types.Reference;
 import org.apache.tools.ant.types.ZipFileSet;
 import org.aspectj.bridge.IMessage;
 import org.aspectj.bridge.IMessageHandler;
 import org.aspectj.bridge.IMessageHolder;
 import org.aspectj.bridge.MessageHandler;
 import org.aspectj.tools.ajc.Main;
 import org.aspectj.tools.ajc.Main.MessagePrinter;
 import org.aspectj.util.FileUtil;
 import org.aspectj.util.LangUtil;
 
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.StringTokenizer;
 
 
 /**
  * This runs the AspectJ 1.1 compiler, 
  * supporting all the command-line options.
  * It can also complete the output in
  * the destination directory or output jar
  * by copying non-.class files from all input jars
  * or copying resources from source root directories.
  * When copying anything to the output jar, 
  * this will pass the AspectJ
  * compiler a path to a different temporary output jar file,
  * the contents of which will be copied along with any
  * resources to the actual output jar.
  * When not forking, things will be copied as needed 
  * for each iterative compile,
  * but when forking things are only copied at the 
  * completion of a successful compile.
  * <p>
  * See the development environment guide for 
  * usage documentation.
  * 
  * @since AspectJ 1.1, Ant 1.5
  */
 public class AjcTask extends MatchingTask {
 
     /**
      * Extract javac arguments to ajc,
      * and add arguments to make ajc behave more like javac
      * in copying resources.
      * <p>
      * Pass ajc-specific options using compilerarg sub-element:
      * <pre>
      * &lt;javac srcdir="src">
      *     &lt;compilerarg compiler="..." line="-argfile src/args.lst"/>
      * &lt;javac>
      * </pre>
      * Some javac arguments are not supported in this component (yet):
      * <pre>
      * String memoryInitialSize;
      * boolean includeAntRuntime = true;
      * boolean includeJavaRuntime = false;
      * </pre>
      * Other javac arguments are not supported in ajc 1.1:
      * <pre>
      * boolean optimize;
      * String forkedExecutable;
      * FacadeTaskHelper facade;
      * boolean depend;
      * String debugLevel;
      * Path compileSourcepath;
      * </pre>
      * @param javac the Javac command to implement (not null)
      * @param ajc the AjcTask to adapt (not null)
      * @param destDir the File class destination directory (may be null)
      * @return null if no error, or String error otherwise
      */
     public static String setupAjc(AjcTask ajc, Javac javac, File destDir) {        
         if (null == ajc) {
             return "null ajc";
         } else if (null == javac) {
             return "null javac";
         } else if (null == destDir) {
             destDir = javac.getDestdir();
             if (null == destDir) {
                 destDir = new File(".");
             }
         }
         // no null checks b/c AjcTask handles null input gracefully
         ajc.setProject(javac.getProject());
         ajc.setLocation(javac.getLocation());
         ajc.setTaskName("javac-iajc");
         
         ajc.setDebug(javac.getDebug());
         ajc.setDeprecation(javac.getDeprecation());
         ajc.setFailonerror(javac.getFailonerror());
         final boolean fork = javac.isForkedJavac();
         ajc.setFork(fork);        
         if (fork) {
             ajc.setMaxmem(javac.getMemoryMaximumSize());
         }
         ajc.setNowarn(javac.getNowarn()); 
         ajc.setListFileArgs(javac.getListfiles());
         ajc.setVerbose(javac.getVerbose());               
         ajc.setTarget(javac.getTarget());        
         ajc.setSource(javac.getSource());        
         ajc.setEncoding(javac.getEncoding());        
         ajc.setDestdir(destDir);        
         ajc.setBootclasspath(javac.getBootclasspath());
         ajc.setExtdirs(javac.getExtdirs());
         ajc.setClasspath(javac.getClasspath());        
         // ignore srcDir -- all files picked up in recalculated file list
 //      ajc.setSrcDir(javac.getSrcdir());        
         ajc.addFiles(javac.getFileList());
         // mimic javac's behavior in copying resources,
         ajc.setSourceRootCopyFilter("**/CVS/*,**/*.java,**/*.aj");
         // arguments can override the filter, add to paths, override options
         ajc.readArguments(javac.getCurrentCompilerArgs());
         
         return null;
     }
     
    /**
      * Find aspectjtools.jar in the system classpath.
      * @return readable File for aspectjtools.jar, or null if not found.
      */            
     public static File findAspectjtoolsJar() {
         Path classpath = Path.systemClasspath;
         String[] paths = classpath.list();
         for (int i = 0; i < paths.length; i++) {
             String path = paths[i];
             if (-1 != paths[i].indexOf("aspectjtools.jar")) {
                 File result = new File(path);
                 if (result.canRead() && result.isFile()) {
                     return result;
                 }
             }
         }
         return null;
     }
 
     /** 
      * Maximum length (in chars) of command line 
      * before converting to an argfile when forking 
      */
     private static final int MAX_COMMANDLINE = 4096;
     
     private static final File DEFAULT_DESTDIR = new File(".");
     
     /** valid -X[...] options other than -Xlint variants */
     private static final List VALID_XOPTIONS;
 
 	/** valid warning (-warn:[...]) variants */
     private static final List VALID_WARNINGS;
     
 	/** valid debugging (-g:[...]) variants */
     private static final List VALID_DEBUG;
 
 	/** 
 	 * -Xlint variants (error, warning, ignore)
 	 * @see org.aspectj.weaver.Lint 
 	 */
     private static final List VALID_XLINT;
         
     static {
         String[] xs = new String[] 
             {   "serializableAspects", "incrementalFile"
             	//, "targetNearSource", "OcodeSize",
                  };
         VALID_XOPTIONS = Collections.unmodifiableList(Arrays.asList(xs));
 
         xs = new String[]
         	{"constructorName", "packageDefaultMethod", "deprecation",
         		"maskedCatchBlocks", "unusedLocals", "unusedArguments",
         		"unusedImports", "syntheticAccess", "assertIdentifier" };
         VALID_WARNINGS = Collections.unmodifiableList(Arrays.asList(xs));
 
         xs = new String[] {"none", "lines", "vars", "source" };
         VALID_DEBUG = Collections.unmodifiableList(Arrays.asList(xs));
         
         
         xs = new String[] { "error", "warning", "ignore"};
         VALID_XLINT = Collections.unmodifiableList(Arrays.asList(xs));
         
     }
 	// ---------------------------- state and Ant interface thereto
     private boolean verbose;
     private boolean listFileArgs;
     private boolean failonerror;
     private boolean fork;
     private String maxMem;
 	
 	// ------- single entries dumped into cmd
     protected GuardedCommand cmd;
     private int cmdLength;
 	
 	// ------- lists resolved in addListArgs() at execute() time
     private Path srcdir;
     private Path injars;
     private Path classpath;
     private Path bootclasspath;
     private Path extdirs;
     private Path aspectpath;
     private Path argfiles;
     private List ignored;
     private Path sourceRoots;
     // ----- added by adapter - integrate better?
     private List /* File */ adapterFiles;
     private String[] adapterArguments;
 
     private IMessageHolder messageHolder;
 
     // -------- resource-copying
     /** true if copying injar non-.class files to the output jar */
     private boolean copyInjars;
     
     /** non-null if copying all source root files but the filtered ones */
     private String sourceRootCopyFilter;
     
     /** directory sink for classes */
     private File destDir;
     
     /** zip file sink for classes */
     private File outjar;
 
     /** 
      * When possibly copying resources to the output jar,
      * pass ajc a fake output jar to copy from,
      * so we don't change the modification time of the output jar
      * when copying injars into the actual outjar.
      */
     private File tmpOutjar;
 
     private boolean executing;
 
     // also note MatchingTask grabs source files...
     
     public AjcTask() {
     	reset();
     }
 
 	/** to use this same Task more than once */
     public void reset() {
         // need declare for "all fields initialized in ..."
         verbose = false;
         failonerror = false;
     	cmd = new GuardedCommand();
     	srcdir = null;
     	injars = null;
     	classpath = null;
     	aspectpath = null;
     	argfiles = null;
     	ignored = new ArrayList();
 		sourceRoots = null;
         copyInjars = false;
         sourceRootCopyFilter = null;
         destDir = DEFAULT_DESTDIR;
         outjar = null;
         tmpOutjar = null;
         executing = false;
         adapterFiles = new ArrayList();
     }
 
     protected void ignore(String ignored) {
         this.ignored.add(ignored + " at " + getLocation());
     }
     
     //---------------------- option values
 
     // used by entries with internal commas
     protected String validCommaList(String list, List valid, String label) {
     	return validCommaList(list, valid, label, valid.size());
     }
     
     protected String validCommaList(String list, List valid, String label, int max) {
     	StringBuffer result = new StringBuffer();
     	StringTokenizer st = new StringTokenizer(list, ",");
 		int num = 0;
     	while (st.hasMoreTokens()) {
 			String token = st.nextToken().trim();
 			num++;
 			if (num > max) {
 				ignore("too many entries for -" 
 					+ label 
 					+ ": " 
 					+ token); 
 				break;
 			}
 			if (!valid.contains(token)) {
 				ignore("bad commaList entry for -" 
 					+ label 
 					+ ": " 
 					+ token); 
 			} else {
 				if (0 < result.length()) {
 					result.append(",");
 				}
 				result.append(token);
 			}
     	}
     	return (0 == result.length() ? null : result.toString());
     }
     
     public void setIncremental(boolean incremental) {  
         cmd.addFlag("-incremental", incremental);
     }
 
     public void setHelp(boolean help) {  
         cmd.addFlag("-help", help);
     }
 
     public void setVersion(boolean version) {  
     	cmd.addFlag("-version", version);
     }
 
     public void setXNoweave(boolean noweave) {  
         cmd.addFlag("-XnoWeave", noweave);
     }
 
     public void setNowarn(boolean nowarn) {  
         cmd.addFlag("-nowarn", nowarn);
     }
 
     public void setDeprecation(boolean deprecation) {  
         cmd.addFlag("-deprecation", deprecation);
     }
 
     public void setWarn(String warnings) {
     	warnings = validCommaList(warnings, VALID_WARNINGS, "warn");
         cmd.addFlag("-warn:" + warnings, (null != warnings));
     }
 
     public void setDebug(boolean debug) {
         cmd.addFlag("-g", debug);
     }
     
     public void setDebugLevel(String level) {
     	level = validCommaList(level, VALID_DEBUG, "g");
         cmd.addFlag("-g:" + level, (null != level));
     }
 
     public void setEmacssym(boolean emacssym) {
         cmd.addFlag("-emacssym", emacssym);
     }
 
 	/** 
 	 * -Xlint - set default level of -Xlint messages to warning
 	 * (same as </code>-Xlint:warning</code>)
 	 */
 	public void setXlintwarnings(boolean xlintwarnings) {
         cmd.addFlag("-Xlint", xlintwarnings);
 	}
 	
 	/** -Xlint:{error|warning|info} - set default level for -Xlint messages
 	 * @param xlint the String with one of error, warning, ignored 
 	 */
     public void setXlint(String xlint) {
     	xlint = validCommaList(xlint, VALID_XLINT, "Xlint", 1);
         cmd.addFlag("-Xlint:" + xlint, (null != xlint));
     }
 
 	/** 
 	 * -Xlintfile {lint.properties} - enable or disable specific forms 
 	 * of -Xlint messages based on a lint properties file
 	 *  (default is 
 	 * <code>org/aspectj/weaver/XLintDefault.properties</code>)
 	 * @param xlintFile the File with lint properties
 	 */
     public void setXlintfile(File xlintFile) { 
         cmd.addFlagged("-Xlintfile", xlintFile.getAbsolutePath());
     }
 
     public void setPreserveAllLocals(boolean preserveAllLocals) {  
         cmd.addFlag("-preserveAllLocals", preserveAllLocals);
     }
 
     public void setNoImportError(boolean noImportError) {  
         cmd.addFlag("-noImportError", noImportError);
     }
 
     public void setEncoding(String encoding) {   
         cmd.addFlagged("-encoding", encoding);
     }
 
     public void setLog(File file) {
         cmd.addFlagged("-log", file.getAbsolutePath());        
     }
     
     public void setProceedOnError(boolean proceedOnError) {  
         cmd.addFlag("-proceedOnError", proceedOnError);
     }
 
     public void setVerbose(boolean verbose) {  
         cmd.addFlag("-verbose", verbose);
         this.verbose = verbose;
     }
     
     public void setListFileArgs(boolean listFileArgs) { 
         this.listFileArgs = listFileArgs;
     }
 
     public void setReferenceInfo(boolean referenceInfo) {  
         cmd.addFlag("-referenceInfo", referenceInfo);
     }
 
     public void setProgress(boolean progress) {  
         cmd.addFlag("-progress", progress);
     }
 
     public void setTime(boolean time) {  
         cmd.addFlag("-time", time);
     }
 
     public void setNoExit(boolean noExit) {  
         cmd.addFlag("-noExit", noExit);
     }
 
     public void setFailonerror(boolean failonerror) {  
         this.failonerror = failonerror;
     }
 
     public void setFork(boolean fork) {  
         this.fork = fork;
     }
     
     public void setMaxmem(String maxMem) {
         this.maxMem = maxMem;
     }
 
 	// ----------------
     public void setTagFile(File file) {
         cmd.addFlagged(Main.CommandController.TAG_FILE_OPTION,
 	        file.getAbsolutePath());        
     }
     
     public void setOutjar(File file) {
         if (DEFAULT_DESTDIR != destDir) {
             String e = "specifying both output jar ("
                 + file 
                 + ") and destination dir ("
                 + destDir
                 + ")";
             throw new BuildException(e);
         }
         outjar = file;
     }
 
     public void setDestdir(File dir) {
         if (null != outjar) {
             String e = "specifying both output jar ("
                 + outjar 
                 + ") and destination dir ("
                 + dir
                 + ")";
             throw new BuildException(e);
         }
         cmd.addFlagged("-d", dir.getAbsolutePath());
         destDir = dir;        
     }
     
     public void setTarget(String either11or12) {
     	if ("1.1".equals(either11or12)) {
     		cmd.addFlagged("-target", "1.1");
     	} else if ("1.2".equals(either11or12)) {
     		cmd.addFlagged("-target", "1.2");
     	} else if (null != either11or12){
     		ignore("-target " + either11or12);
     	}   		
     }
     
     /** 
      * Language compliance level.
      * If not set explicitly, eclipse default holds.
      * @param either13or14 either "1.3" or "1.4"
      */
     public void setCompliance(String either13or14) {
     	if ("1.3".equals(either13or14)) {
     		cmd.addFlag("-1.3", true);
     	} else if ("1.4".equals(either13or14)) {
     		cmd.addFlag("-1.4", true);
         } else if (null != either13or14) {
     		ignore(either13or14 + "[compliance]");
     	}   		
     }
     
     /** 
      * Source compliance level.
      * If not set explicitly, eclipse default holds.
      * @param either13or14 either "1.3" or "1.4"
      */
     public void setSource(String either13or14) {
     	if ("1.3".equals(either13or14)) {
     		cmd.addFlagged("-source", "1.3");
     	} else if ("1.4".equals(either13or14)) {
     		cmd.addFlagged("-source", "1.4");
     	} else if (null != either13or14) {
     		ignore("-source " + either13or14);
     	}   		
     }
     /**
      * Flag to copy all non-.class contents of injars
      * to outjar after compile completes.
      * Requires both injars and outjar.
      * @param doCopy
      */
     public void setCopyInjars(boolean doCopy){
         this.copyInjars = doCopy;
     }
     /**
      * Option to copy all files from
      * all source root directories
      * except those specified here.
      * If this is specified and sourceroots are specified,
      * then this will copy all files except 
      * those specified in the filter pattern.
      * Requires sourceroots.
      * 
      * @param filter a String acceptable as an excludes
      *        filter for an Ant Zip fileset.
      */
     public void setSourceRootCopyFilter(String filter){
         this.sourceRootCopyFilter = filter;
     }
 
     public void setX(String input) {  // ajc-only eajc-also docDone
         StringTokenizer tokens = new StringTokenizer(input, ",", false);
         while (tokens.hasMoreTokens()) {
             String token = tokens.nextToken().trim();
             if (1 < token.length()) {
                 if (VALID_XOPTIONS.contains(token)) {
                     cmd.addFlag("-X" + token, true); 
                 } else {
                     ignore("-X" + token);
                 }
             }
         }
     }
 
     /** direct API for testing */
     void setMessageHolder(IMessageHolder holder) {
         this.messageHolder = holder;
     }
     
     /** 
      * Setup custom message handling.
      * @param className the String fully-qualified-name of a class
      *          reachable from this object's class loader,
      *          implementing IMessageHolder, and 
      *          having a public no-argument constructor.
      * @throws BuildException if unable to create instance of className
      */
     public void setMessageHolderClass(String className) {
         try {
             Class mclass = Class.forName(className);
             IMessageHolder holder = (IMessageHolder) mclass.newInstance();
             setMessageHolder(holder);
         } catch (Throwable t) {
             String m = "unable to instantiate message holder: " + className;
             throw new BuildException(m, t);
         }
     }
 
     //---------------------- Path lists
 
     /**
      * Add path elements to source path and return result.
      * Elements are added even if they do not exist.
      * @param source the Path to add to - may be null
      * @param toAdd the Path to add - may be null
      * @return the (never-null) Path that results
      */
     protected Path incPath(Path source, Path toAdd) {
         if (null == source) {
             source = new Path(project); 
         }
         if (null != toAdd) {
             source.append(toAdd);
         }
         return source;
     }
 
     public void setSourcerootsref(Reference ref) {
         createSourceRoots().setRefid(ref);
     }
     
     public void setSourceRoots(Path roots) {
         sourceRoots = incPath(sourceRoots, roots);
     }
 
     public Path createSourceRoots() {
         if (sourceRoots == null) {
             sourceRoots = new Path(project);
         }
         return sourceRoots.createPath();
     }        
 	
     public void setInjarsref(Reference ref) {
         createInjars().setRefid(ref);
     }
     
     public void setInjars(Path path) {
         injars = incPath(injars, path);
     }
 
     public Path createInjars() {
         if (injars == null) {
             injars = new Path(project);
         }
         return injars.createPath();
     }        
     
     public void setClasspath(Path path) {
         classpath = incPath(classpath, path);
     }
 
     public void setClasspathref(Reference classpathref) {
         createClasspath().setRefid(classpathref);
     }
         
     public Path createClasspath() {
         if (bootclasspath == null) {
             bootclasspath = new Path(project);
         }
         return bootclasspath.createPath();
     }        
 
     public void setBootclasspath(Path path) {
         bootclasspath = incPath(bootclasspath, path);  
     }
     
     public void setBootclasspathref(Reference bootclasspathref) {
         createBootclasspath().setRefid(bootclasspathref);
     }
     
     public Path createBootclasspath() {
         if (classpath == null) {
             classpath = new Path(project);
         }
         return classpath.createPath();
     }        
     
     public void setExtdirs(Path path) {
         extdirs = incPath(extdirs, path);
     }
 
     public void setExtdirsref(Reference ref) {
         createExtdirs().setRefid(ref);
     }
         
     public Path createExtdirs() {
         if (extdirs == null) {
             extdirs = new Path(project);
         }
         return extdirs.createPath();
     }        
    
 
     public void setAspectpathref(Reference ref) {
         createAspectpath().setRefid(ref);
     }
 
     public void setAspectpath(Path path) {
         aspectpath = incPath(aspectpath, path);
     }
 
     public Path createAspectpath() {
         if (aspectpath == null) {
             aspectpath = new Path(project);
         }
         return aspectpath.createPath();
     }        
 
     public void setSrcDir(Path path) {
         srcdir = incPath(srcdir, path);
     }
 
     public Path createSrc() {
         return createSrcdir();
     }
 
     public Path createSrcdir() {
         if (srcdir == null) {
             srcdir = new Path(project);
         }
         return srcdir.createPath();
     }
 
     public void setArgfilesref(Reference ref) {
         createArgfiles().setRefid(ref);
     }
     
     public void setArgfiles(Path path) { // ajc-only eajc-also docDone
         argfiles = incPath(argfiles, path);
     }
 
     public Path createArgfiles() {
         if (argfiles == null) {
             argfiles = new Path(project);
         }
         return argfiles.createPath();
     }
                 
     // ------------------------------ run
   
     /**
      * Compile using ajc per settings.
      * @exception BuildException if the compilation has problems
      *             or if there were compiler errors and failonerror is true.
      */
     public void execute() throws BuildException {
         if (executing) {
             throw new IllegalStateException("already executing");
         } else {
             executing = true;
         }
         try {
         	if (0 < ignored.size()) {
 				for (Iterator iter = ignored.iterator(); iter.hasNext();) {
 					log("ignored: " + iter.next(), Project.MSG_INFO);					
 				}
         	}
             // when copying resources, use temp jar for class output
             // then copy temp jar contents and resources to output jar
             if (null != outjar) {
                 if (copyInjars || (null != sourceRootCopyFilter)) {
                     String path = outjar.getAbsolutePath();
                     int len = FileUtil.zipSuffixLength(path);
                     if (len < 1) {
                         log("not copying injars - weird outjar: " + path);
                     } else {
                         path = path.substring(0, path.length()-len) + ".tmp.jar";
                         tmpOutjar = new File(path);
                     }
                 }
                 if (null == tmpOutjar) {                
                     cmd.addFlagged("-outjar", outjar.getAbsolutePath());        
                 } else {
                     cmd.addFlagged("-outjar", tmpOutjar.getAbsolutePath());        
                 }
             }
             
             addListArgs();
 	        if (verbose || listFileArgs) { // XXX if listFileArgs, only do that
                 String[] args = cmd.extractArguments();
 	        	log("ajc " + Arrays.asList(args), Project.MSG_VERBOSE);
 	        }
             if (!fork) {
                 executeInSameVM();
             } else { // when forking, Adapter handles failonerror
                 executeInOtherVM();
             }
         } catch (BuildException e) {
             throw e;
         } catch (Throwable x) {
         	System.err.println(Main.renderExceptionForUser(x));        	
             throw new BuildException("IGNORE -- See " 
             	+ LangUtil.unqualifiedClassName(x) 
             	+ " rendered to System.err");
         } finally {
             executing = false;
             if (null != tmpOutjar) {
                 tmpOutjar.delete();
             }
         }        
     }   
 
     /**
      * Run the compile in the same VM by
      * loading the compiler (Main), 
      * setting up any message holders,
      * doing the compile,
      * and converting abort/failure and error messages
      * to BuildException, as appropriate.
      * @throws BuildException if abort or failure messages
      *         or if errors and failonerror.
      * 
      */
     protected void executeInSameVM() {
         if (null != maxMem) {
             log("maxMem ignored unless forked: " + maxMem, Project.MSG_WARN);
         }
         IMessageHolder holder = messageHolder;
         int numPreviousErrors;
         if (null == holder) {
             MessageHandler mhandler = new MessageHandler(true);
             final IMessageHandler delegate 
                 = verbose ? MessagePrinter.VERBOSE: MessagePrinter.TERSE;
             mhandler.setInterceptor(delegate);
             if (!verbose) {
                 mhandler.ignore(IMessage.INFO);
             }
             holder = mhandler;
             numPreviousErrors = 0;
         } else {
             numPreviousErrors = holder.numMessages(IMessage.ERROR, true);
         }
         Main main = new Main();
         main.setHolder(holder);
         main.setCompletionRunner(new Runnable() {
             public void run() {
                 doCompletionTasks();
             }
         });
         main.runMain(cmd.extractArguments(), false);
         if (failonerror) {
             int errs = holder.numMessages(IMessage.ERROR, false);
             errs -= numPreviousErrors;
             if (0 < errs) {
                 // errors should already be printed by interceptor
                 throw new BuildException(errs + " errors"); 
             }
         } 
         // Throw BuildException if there are any fail or abort
         // messages.
         // The BuildException message text has a list of class names
         // for the exceptions found in the messages, or the
         // number of fail/abort messages found if there were
         // no exceptions for any of the fail/abort messages.
         // The interceptor message handler should have already
         // printed the messages, including any stack traces.
         {
             IMessage[] fails = holder.getMessages(IMessage.FAIL, true);
             if (!LangUtil.isEmpty(fails)) {
                 StringBuffer sb = new StringBuffer();
                 String prefix = "fail due to ";
                 for (int i = 0; i < fails.length; i++) {
                     Throwable t = fails[i].getThrown();
                     if (null != t) {
                         sb.append(prefix);
                         sb.append(LangUtil.unqualifiedClassName(t.getClass()));
                        prefix = ", ";
                    }
                 }
                 if (0 < sb.length()) {
                    sb.append(" rendered in messages above.");
                } else {
                    sb.append(fails.length 
                              + " fails/aborts (no exceptions)");
                 }
                throw new BuildException(sb.toString());
             }
         }
     }
     
     /**
      * Execute in a separate VM.
      * Differences from normal same-VM execution:
      * <ul>
      * <li>ignores any message holder {class} set</li>
      * <li>No resource-copying between interative runs</li>
      * <li>failonerror fails when process interface fails 
      *     to return negative values</li>
      * </ul>
      * @param args String[] of the complete compiler command to execute
      * 
      * @see DefaultCompilerAdapter#executeExternalCompile(String[], int)
      * @throws BuildException if ajc aborts (negative value)
      *         or if failonerror and there were compile errors.
      */
     protected void executeInOtherVM() {
         if (null != messageHolder) {
             log("message holder ignored when forking: "
                 + messageHolder.getClass().getName(), Project.MSG_WARN);
         }
         CommandlineJava javaCmd = new CommandlineJava();
         javaCmd.setClassname(org.aspectj.tools.ajc.Main.class.getName());
         Path vmClasspath = javaCmd.createClasspath(getProject());
         File aspectjtools = findAspectjtoolsJar();
         if (null != aspectjtools) {
             vmClasspath.createPathElement().setLocation(aspectjtools);
         }
         if (null != maxMem) {
             javaCmd.setMaxmemory(maxMem);
         }
         File tempFile = cmd.limitTo(MAX_COMMANDLINE, getLocation());
         try {
             String[] javaArgs = javaCmd.getCommandline();
             String[] args = cmd.extractArguments();
             String[] both = new String[javaArgs.length + args.length];
             System.arraycopy(javaArgs,0,both,0,javaArgs.length);
             System.arraycopy(args,0,both,javaArgs.length,args.length);
             int result = execInOtherVM(both);
             if (0 > result) {
                 throw new BuildException("failure[" + result + "] running ajc");
             } else if (failonerror && (0 < result)) {
                 throw new BuildException("compile errors: " + result);                
             }
             // when forking, do completion only at end and when successful
             doCompletionTasks();
         } finally {
             if (null != tempFile) {
                 tempFile.delete();
             }
         }
     }
     
     /**
      * Execute in another process using the same JDK
      * and the base directory of the project. XXX correct?
      * @param args
      * @return
      */
     protected int execInOtherVM(String[] args) {
         try {
             Project project = getProject();
             LogStreamHandler handler = new LogStreamHandler(this,
                                  Project.MSG_INFO, Project.MSG_WARN);
             Execute exe = new Execute(handler);
             exe.setAntRun(project);
             exe.setWorkingDirectory(project.getBaseDir());
             exe.setCommandline(args);
             exe.execute();
             return exe.getExitValue();
         } catch (IOException e) {
             String m = "Error executing command " + Arrays.asList(args);
             throw new BuildException(m, e, location);
         }
     }
 
     // ------------------------------ setup and reporting
     /** @return null if path null or empty, String rendition otherwise */
     void addFlaggedPathToCommand(String flag, Path path) {
         if ((null != path) && (0 < path.size())) {
             cmd.addFlagged(flag, path.toString());
         }
     }
     
     /** 
      * Add to cmd any plural arguments, which might be set
      * repeatedly.
      */
 	protected void addListArgs() throws BuildException {
         addFlaggedPathToCommand("-classpath", classpath);       
         addFlaggedPathToCommand("-bootclasspath", bootclasspath);
         addFlaggedPathToCommand("-extdirs", extdirs);
         addFlaggedPathToCommand("-aspectpath", aspectpath);
         addFlaggedPathToCommand("-injars", injars);
         addFlaggedPathToCommand("-sourceroots", sourceRoots);
         
         if (argfiles != null) {
             String[] files = argfiles.list();
             for (int i = 0; i < files.length; i++) {
                 File argfile = project.resolveFile(files[i]);
                 if (check(argfile, files[i], false, location)) {
 		            cmd.addFlagged("-argfile", argfile.getAbsolutePath());
                 }
             }
         }
         if (srcdir != null) {
             // todo: ignore any srcdir if any argfiles and no explicit includes
             String[] dirs = srcdir.list();
             for (int i = 0; i < dirs.length; i++) {
                 File dir = project.resolveFile(dirs[i]);
                 check(dir, dirs[i], true, location);
                 // relies on compiler to prune non-source files
                 String[] files = getDirectoryScanner(dir).getIncludedFiles();
                 for (int j = 0; j < files.length; j++) {
                     File file = new File(dir, files[j]);
                     if (FileUtil.hasSourceSuffix(file)) {
                         cmd.addFile(file);
                     }
                 }
             }
         }
         if (0 < adapterFiles.size()) {
             for (Iterator iter = adapterFiles.iterator(); iter.hasNext();) {
                 File file = (File) iter.next();
                 if (file.canRead() && FileUtil.hasSourceSuffix(file)) {
                     cmd.addFile(file);
                 } else {
                     log("skipping file: " + file, Project.MSG_WARN);
                 }
             }
         }
 	}    
 
     /** 
      * Throw BuildException unless file is valid.
      * @param file the File to check
      * @param name the symbolic name to print on error
      * @param isDir if true, verify file is a directory
      * @param loc the Location used to create sensible BuildException
      * @return
      * @throws BuildException unless file valid
      */
     protected final boolean check(File file, String name,
                                   boolean isDir, Location loc) {
         loc = loc != null ? loc : location;
         if (file == null) {
             throw new BuildException(name + " is null!", loc);
         }
         if (!file.exists()) {
             throw new BuildException(file + " doesn't exist!", loc);
         }
         if (isDir ^ file.isDirectory()) {
             String e = file + " should" + (isDir ? "" : "n't")  +
                 " be a directory!";
             throw new BuildException(e, loc);
         }
         return true;
     }
     
     /** 
      * Called when compile or incremental compile is completing,
      * this completes the output jar or directory
      * by copying resources if requested.
      * Note: this is a callback run synchronously by the compiler.
      * That means exceptions thrown here are caught by Main.run(..)
      * and passed to the message handler.
      */
     protected void doCompletionTasks() {
         if (!executing) {
             throw new IllegalStateException("should be executing");
         }
         if (null != outjar) {
             completeOutjar();
         } else {
             completeDestdir();
         }
     }
     
     /** 
      * Complete the destination directory
      * by copying resources from the source root directories
      * (if the filter is specified)
      * and non-.class files from the input jars 
      * (if XCopyInjars is enabled).
      */
     private void completeDestdir() {
         if (!copyInjars && (null == sourceRootCopyFilter)) {
             return;
         } else if (!destDir.canWrite()) {
             String s = "unable to copy resources to destDir: " + destDir;
             throw new BuildException(s);
         }
         final Project project = getProject();
         if (copyInjars) {
             String taskName = getTaskName() + " - unzip";
             String[] paths = injars.list();
             if (!LangUtil.isEmpty(paths)) {
                 PatternSet patternSet = new PatternSet();
                 patternSet.setProject(project);        
                 patternSet.setIncludes("**/*");
                 patternSet.setExcludes("**/*.class");  
                 for (int i = 0; i < paths.length; i++) {
                     Expand unzip = new Expand();
                     unzip.setProject(project);
                     unzip.setTaskName(taskName);
                     unzip.setDest(destDir);
                     unzip.setSrc(new File(paths[i]));
                     unzip.addPatternset(patternSet);
                     unzip.execute();
                 }
             }
         }
         if ((null != sourceRootCopyFilter) && (null != sourceRoots)) {
             String[] paths = sourceRoots.list();
             if (!LangUtil.isEmpty(paths)) {
                 Copy copy = new Copy();
                 copy.setProject(project);
                 copy.setTodir(destDir);
                 for (int i = 0; i < paths.length; i++) {
                     FileSet fileSet = new FileSet();
                     fileSet.setDir(new File(paths[i]));
                     fileSet.setIncludes("**/*");
                     fileSet.setExcludes(sourceRootCopyFilter);  
                     copy.addFileset(fileSet);
                 }
                 copy.execute();
             }
         }        
     }
     
     /** 
      * Complete the output jar
      * by copying resources from the source root directories
      * if the filter is specified.
      * and non-.class files from the input jars if enabled.
      */
     private void completeOutjar() {
         if (((null == tmpOutjar) || !tmpOutjar.canRead()) 
             || (!copyInjars && (null == sourceRootCopyFilter))) {
             return;
         }
         Zip zip = new Zip();
         Project project = getProject();
         zip.setProject(project);        
         zip.setTaskName(getTaskName() + " - zip");
         zip.setDestFile(outjar);
         ZipFileSet zipfileset = new ZipFileSet();
         zipfileset.setProject(project);        
         zipfileset.setSrc(tmpOutjar);
         zipfileset.setIncludes("**/*.class");
         zip.addZipfileset(zipfileset);
         if (copyInjars) {
             String[] paths = injars.list();
             if (!LangUtil.isEmpty(paths)) {
                 for (int i = 0; i < paths.length; i++) {
                     File jarFile = new File(paths[i]);
                     zipfileset = new ZipFileSet();
                     zipfileset.setProject(project);
                     zipfileset.setSrc(jarFile);
                     zipfileset.setIncludes("**/*");
                     zipfileset.setExcludes("**/*.class");  
                     zip.addZipfileset(zipfileset);
                 }
             }
         }
         if ((null != sourceRootCopyFilter) && (null != sourceRoots)) {
             String[] paths = sourceRoots.list();
             if (!LangUtil.isEmpty(paths)) {
                 for (int i = 0; i < paths.length; i++) {
                     File srcRoot = new File(paths[i]);
                     FileSet fileset = new FileSet();
                     fileset.setProject(project);
                     fileset.setDir(srcRoot);
                     fileset.setIncludes("**/*");
                     fileset.setExcludes(sourceRootCopyFilter);  
                     zip.addFileset(fileset);
                 }
             }
         }        
         zip.execute();
     }
     
     // -------------------------- compiler adapter interface extras
 
     /**
      * Add specified source files.
      */
     void addFiles(File[] paths) {
         for (int i = 0; i < paths.length; i++) {
             addFile(paths[i]);
         }
     }
     /**
      * Add specified source file.
      */
     void addFile(File path) {
         if (null != path) {
             adapterFiles.add(path);
         }
     }
 
     /**
      * Read arguments in as if from a command line, 
      * mainly to support compiler adapter compilerarg subelement. 
      * 
      * @param args the String[] of arguments to read
      */
     void readArguments(String[] args) { // XXX slow, stupid, unmaintainable
         if ((null == args) || (0 == args.length)) {
             return;
         }
         /** String[] wrapper with increment, error reporting */
         class Args {
             final String[] args;
             int index = 0;
             Args(String[] args) {
                 this.args = args; // not null or empty
             }
             boolean hasNext() {
                 return index < args.length;
             }
             String next() {
                 String err = null;
                 if (!hasNext()) {
                     err = "need arg for flag " + args[args.length-1];
                 } else {
                     String s = args[index++];
                     if (null == s) {
                         err = "null value";                                            
                     } else {
                         s = s.trim();
                         if (0 == s.trim().length()) {
                             err = "no value";                                            
                         } else {
                             return s;
                         }
                     }
                 }
                 err += " at [" + index + "] of " + Arrays.asList(args);
                 throw new BuildException(err);
             }
         } // class Args
 
         Args in = new Args(args);
         String flag;
         while (in.hasNext()) {
             flag = in.next();
             if ("-1.3".equals(flag)) {
                 setCompliance(flag);
             } else if ("-1.4".equals(flag)) {
                 setCompliance(flag);
             } else if ("-argfile".equals(flag)) {
                 setArgfiles(new Path(project, in.next()));
             } else if ("-aspectpath".equals(flag)) {
                 setAspectpath(new Path(project, in.next()));
             } else if ("-classpath".equals(flag)) {
                 setClasspath(new Path(project, in.next()));
             } else if ("-Xcopyinjars".equals(flag)) {
                 setCopyInjars(true);
             } else if ("-g".equals(flag)) {
                 setDebug(true);
             } else if (flag.startsWith("-g:")) {
                 setDebugLevel(flag.substring(2));
             } else if ("-deprecation".equals(flag)) {
                 setDeprecation(true);
             } else if ("-d".equals(flag)) {
                 setDestdir(new File(in.next()));
             } else if ("-emacssym".equals(flag)) {
                 setEmacssym(true);
             } else if ("-encoding".equals(flag)) {
                 setEncoding(in.next());
             } else if ("-Xfailonerror".equals(flag)) {
                 setFailonerror(true);
             } else if ("-fork".equals(flag)) {
                 setFork(true);
             } else if ("-help".equals(flag)) {
                 setHelp(true);
             } else if ("-incremental".equals(flag)) {
                 setIncremental(true);
             } else if ("-injars".equals(flag)) {
                 setInjars(new Path(project, in.next()));
             } else if ("-Xlistfileargs".equals(flag)) {
                 setListFileArgs(true);
             } else if ("-Xmaxmem".equals(flag)) {
                 setMaxmem(in.next());
             } else if ("-Xmessageholderclass".equals(flag)) {
                 setMessageHolderClass(in.next());
             } else if ("-noexit".equals(flag)) {
                 setNoExit(true);
             } else if ("-noimport".equals(flag)) {
                 setNoExit(true);
             } else if ("-noExit".equals(flag)) {
                 setNoExit(true);
             } else if ("-noImportError".equals(flag)) {
                 setNoImportError(true);
             } else if ("-noWarn".equals(flag)) {
                 setNowarn(true);
             } else if ("-noexit".equals(flag)) {
                 setNoExit(true);
             } else if ("-outjar".equals(flag)) {
                 setOutjar(new File(in.next()));
             } else if ("-preserveAllLocals".equals(flag)) {
                 setPreserveAllLocals(true);
             } else if ("-proceedOnError".equals(flag)) {
                 setProceedOnError(true);
             } else if ("-progress".equals(flag)) {
                 setProgress(true);
             } else if ("-referenceInfo".equals(flag)) {
                 setReferenceInfo(true);
             } else if ("-source".equals(flag)) {
                 setSource(in.next());
             } else if ("-Xsourcerootcopyfilter".equals(flag)) {
                 setSourceRootCopyFilter(in.next());
             } else if ("-sourceroots".equals(flag)) {
                 setSourceRoots(new Path(project, in.next()));
             } else if ("-Xsrcdir".equals(flag)) {
                 setSrcDir(new Path(project, in.next()));
             } else if ("-Xtagfile".equals(flag)) {
                 setTagFile(new File(in.next()));
             } else if ("-target".equals(flag)) {
                 setTarget(in.next());
             } else if ("-time".equals(flag)) {
                 setTime(true);
             } else if ("-time".equals(flag)) {
                 setTime(true);
             } else if ("-verbose".equals(flag)) {
                 setVerbose(true);
             } else if ("-version".equals(flag)) {
                 setVersion(true);
             } else if ("-warn".equals(flag)) {
                 setWarn(in.next());
             } else if ("-Xlint".equals(flag)) {
                 setXlintwarnings(true);
             } else if (flag.startsWith("-Xlint:")) {
                 setXlint(flag.substring(7));
             } else if ("-Xlintfile".equals(flag)) {
                 setXlintfile(new File(in.next()));
             } else if ("-Xnoweave".equals(flag)) {
                 setXNoweave(true);
             } else if (flag.startsWith("@")) {
                 File file = new File(flag.substring(1));
                 if (file.canRead()) {
                     setArgfiles(new Path(project, file.getPath()));
                 } else {
                     ignore(flag);            
                 }
             } else {
                 File file = new File(flag);
                 if (file.isFile() 
                 	&& file.canRead() 
                 	&& FileUtil.hasSourceSuffix(file)) {
                     addFile(file);
                 } else {
                     ignore(flag);
                 }
             }
         }
     }
 }
 
 /**
  * Commandline wrapper that 
  * only permits addition of non-empty values,
  * counts size as it goes,
  * and converts to argfile form if necessary.
  */
 class GuardedCommand {
     Commandline command;
     int size;
 
     static boolean isEmpty(String s) {
         return ((null == s) || (0 == s.trim().length()));
     }
 
     GuardedCommand() {
         command = new Commandline();
     }
     
     void addFlag(String flag, boolean doAdd) {
         if (doAdd && !isEmpty(flag)) {
             command.createArgument().setValue(flag);
             size += 1 + flag.length();
         }
     }
     
     void addFlagged(String flag, String argument) {
         if (!isEmpty(flag) && !isEmpty(argument)) {
             command.addArguments(new String[] {flag, argument});
             size += 1 + flag.length() + argument.length();
         }
     }
     
     void addFile(File file) {
         if (null != file) {
             String path = file.getAbsolutePath();
             addFlag(path, true);
         }
     }
     
     String[] extractArguments() {
         return command.getArguments();
     }
 
      /**
      * Adjust command for size if necessary by creating
      * an argument file, which should be deleted by the client
      * after the compiler run has completed.
      * @param max the int maximum length of the command line (in char)
      * @return the temp File for the arguments (if generated), 
      *         for deletion when done.
      * @throws IllegalArgumentException if max is negative
      */
     protected File limitTo(int max, Location location) { // XXX sync         
         if (max < 0) {
             throw new IllegalArgumentException("negative max: " + max);
         }
         if (size < max) {
             return null;
         }
         // limit interference, but not thread-safe
         String[] args = extractArguments();
         File tmpFile = null;
         PrintWriter out = null;
         // adapted from DefaultCompilerAdapter.executeExternalCompile
         try {
             String userDirName = System.getProperty("user.dir");
             File userDir = new File(userDirName);
             tmpFile = File.createTempFile("argfile", "", userDir);
             out = new PrintWriter(new FileWriter(tmpFile));
             for (int i = 0; i < args.length; i++) {
                 out.println(args[i]);
             }
             out.flush();
             this.command = new Commandline();
             addFlagged("-argfile", tmpFile.getAbsolutePath());
             return tmpFile;
         } catch (IOException e) {
             throw new BuildException("Error creating temporary file", 
                                      e, location);
         } finally {
             if (out != null) {
                 try {out.close();} catch (Throwable t) {}
             }
         }
     }     
 }
 
