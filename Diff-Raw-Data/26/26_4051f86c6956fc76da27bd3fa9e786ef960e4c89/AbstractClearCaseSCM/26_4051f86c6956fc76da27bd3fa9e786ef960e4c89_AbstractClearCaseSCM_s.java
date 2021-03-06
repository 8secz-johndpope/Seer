 /**
  * The MIT License
  *
  * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
  *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  * THE SOFTWARE.
  */
 package hudson.plugins.clearcase;
 
 import static hudson.Util.fixEmptyAndTrim;
 import hudson.EnvVars;
 import hudson.Extension;
 import hudson.FilePath;
 import hudson.Functions;
 import hudson.Launcher;
 import hudson.Util;
 import hudson.model.AbstractBuild;
 import hudson.model.AbstractProject;
 import hudson.model.BuildListener;
 import hudson.model.Computer;
 import hudson.model.Executor;
 import hudson.model.FreeStyleProject;
 import hudson.model.Node;
 import hudson.model.Result;
 import hudson.model.Run;
 import hudson.model.StringParameterValue;
 import hudson.model.TaskListener;
 import hudson.model.listeners.RunListener;
 import hudson.plugins.clearcase.changelog.ClearCaseChangeLogSet;
 import hudson.plugins.clearcase.checkout.CheckoutAction;
 import hudson.plugins.clearcase.cleartool.CTLauncher;
 import hudson.plugins.clearcase.cleartool.ClearTool;
 import hudson.plugins.clearcase.cleartool.ClearToolDynamic;
 import hudson.plugins.clearcase.cleartool.ClearToolSnapshot;
 import hudson.plugins.clearcase.history.Filter;
 import hudson.plugins.clearcase.history.HistoryAction;
 import hudson.plugins.clearcase.history.Filter.DefaultFilter;
 import hudson.plugins.clearcase.history.Filter.DestroySubBranchFilter;
 import hudson.plugins.clearcase.history.Filter.FileFilter;
 import hudson.plugins.clearcase.log.ClearCaseLogger;
 import hudson.plugins.clearcase.log.ClearToolLogAction;
 import hudson.plugins.clearcase.log.ClearToolLogFile;
 import hudson.plugins.clearcase.objects.ClearCaseConfiguration;
 import hudson.plugins.clearcase.objects.View;
 import hudson.plugins.clearcase.util.CCParametersAction;
 import hudson.plugins.clearcase.util.ClearToolError;
 import hudson.plugins.clearcase.util.Tools;
 import hudson.scm.ChangeLogSet;
 import hudson.scm.PollingResult;
 import hudson.scm.SCM;
 import hudson.scm.SCMRevisionState;
 import hudson.util.StreamTaskListener;
 
 import java.io.File;
 import java.io.IOException;
 import java.lang.reflect.Field;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * Abstract class for ClearCase SCM. The class contains the logic around
  * checkout and polling, the deriving classes only have to implement the
  * specific checkout and polling logic.
  * 
  * @author Robin Jarry
  *              2009-12-10 : Changed getModuleRoot() by adding the getViewRoot() method
  *              2010-01-06 : Properly implemented the processWorkspaceBeforeDeletion() method
  *              2010-01-18 : Added shiny log flags
  *              2010-01-28 : Refactoring of the config.jelly files to make them match the fields names
  */
 public abstract class AbstractClearCaseSCM extends SCM {
 
     public static final String CLEARCASE_VIEWNAME_ENVSTR = "CLEARCASE_VIEWNAME";
     public static final String CLEARCASE_VIEWPATH_ENVSTR = "CLEARCASE_VIEWPATH";
     
     /*******************************
      **** FIELDS *******************
      *******************************/
 
     protected transient EnvVars env;
     private transient String normalizedViewName;
     private transient List<String> lsHistoryPaths;
     protected transient View view;
     private transient FilePath originalWorkspace;
 
     private final boolean useDynamicView;
     private final String viewDrive;
     private final String viewName;
     private final String loadRules;
     /**
      * user configured optional params that will be used in when
      * creating a new view.
      */
     private final String mkviewOptionalParam;   
     private final String excludedRegions;
     /**
      * Tells if the "Destroyed branch" event should be filtered out or not.
      * For more information about the boolean, see the full discussion at
      * http://www.nabble.com/ClearCase-build-triggering-td17507838i20.html
      * "Usually, CC admins have a CC trigger, firing on an uncheckout event, that
      * destroys empty branches."
      */
     private final boolean filteringOutDestroySubBranchEvent;
     private final boolean useUpdate;
     private final int multiSitePollBuffer;
     private final String clearcaseConfig;
     private final boolean doNotUpdateConfigSpec;
     
     private final String customWorkspace;
     
 
 	/*******************************
      **** CONSTRUCTOR **************
      *******************************/
 
     public AbstractClearCaseSCM(String viewName, 
                                 String mkviewOptionalParam, 
                                 boolean filteringOutDestroySubBranchEvent, 
                                 boolean useUpdate, 
                                 String excludedRegions, 
                                 String loadRules, 
                                 boolean useDynamicView,
                                 String viewDrive, 
                                 int multiSitePollBuffer,
                                 String clearcaseConfig,
                                 boolean doNotUpdateConfigSpec,
                                 String customWorkspace) {
         super();
         this.viewName = viewName;
         this.mkviewOptionalParam = mkviewOptionalParam;
         this.filteringOutDestroySubBranchEvent = filteringOutDestroySubBranchEvent;
         this.useUpdate = useUpdate;
         this.excludedRegions = excludedRegions;
         this.loadRules = loadRules;
         this.useDynamicView = useDynamicView;
         this.viewDrive = viewDrive;
         this.multiSitePollBuffer = multiSitePollBuffer;
         this.clearcaseConfig = clearcaseConfig;
         this.doNotUpdateConfigSpec = doNotUpdateConfigSpec;
         this.customWorkspace = fixEmptyAndTrim(customWorkspace);
     }
     
     /*******************************
      **** CHECKOUT *****************
      *******************************/
     
     /** override method {@link hudson.scm.SCM#checkout()} */
     @Override
     public boolean checkout(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher l,
             FilePath workspace, BuildListener listener, File changelogFile) throws IOException,
             InterruptedException
     {
         try {
             /* In this plugin, the commands are displayed in a separate console "cleartool output"
              * because cleartool gets sometimes very verbose and it pollutes the build log.
              * 
              * By default, Hudson prints every command invoked in the console no matter 
              * what the user wants. This is done through the getListener().getLogger().printLn() 
              * method from the Launcher class. 
              * 
              * As there is no way to modify this behaviour, I had to create a new launcher 
              * with a NULL TaskListener so that when Hudson prints something, it goes to 
              * the trash instead of poping in the middle of the build log */
             Launcher launcher = Executor.currentExecutor().getOwner().getNode().createLauncher(
                     TaskListener.NULL);
             
             File ctLogFile = ClearToolLogFile.getCleartoolLogFile(build);
             ClearCaseLogger logger = new ClearCaseLogger(listener, ctLogFile);
             
             logger.log("### Begin source code retrieval ###");
 
             setEnv(build.getEnvironment(listener));
             
             // resolves the env variables and sets the normalizedViewName
             publishEnvVars(workspace, getEnv());
             
             Pattern pattern = Pattern.compile("(\\$\\{.+?\\})");
             Matcher matcher = pattern.matcher(getNormalizedViewName());
             if (matcher.find()) {
                 String message = "Illegal characters found in view name : %s. " +
                         "An environment variable may have not been resolved.";
                 throw new ClearToolError(String.format(message, matcher.group()));
             }
             
             ClearCaseConfiguration config = fetchClearCaseConfig(Computer.currentComputer().getName());
             
             logger.log("Using ClearCase configuration: " + config.getName());
             logger.log("ClearCase executable: " + config.getCleartoolExe());
             String storageLocation = Util.fixEmptyAndTrim(config.getStgloc());
             if (storageLocation != null) {
                 logger.log("View storage: " + storageLocation);
             }
             
             View view = getView();
             view.setViewPath(getExtendedViewPath(workspace));
             
             ClearTool cleartool = createClearTool(config.getCleartoolExe(), workspace, build
                     .getBuiltOn().getRootPath(), launcher, env, ctLogFile);
 
             CheckoutAction checkoutAction = createCheckoutAction(cleartool, logger, view, storageLocation);
             
             build.addAction(new ClearToolLogAction());
             
             // Checkout source files
             checkoutAction.checkout(workspace, listener);
             
             /* This is a nasty hack for allowing other plugins 
              * to work when using clearcase views.
              * 
              * The source code is always inside the view with a path 
              * that looks like this one : /view/view_tag/vobs/vob_tag/folder_1
              * 
              * The orginal Hudson option "custom workspace" 
              * cannot be the solution because, if the view doesn't exist 
              * or is not started when the job is started, Hudson will raise 
              * an error trying to create the path...
              * 
              * This is why we only substitute the "workspace" variable 
              * AFTER the checkout action has succeded
              */
             if (customWorkspace != null) {
                 /* First, we store the original value of the workspace 
                  * so we can restore it once the build is completed */
                 originalWorkspace = workspace;
                 
                 /* We make the workspace field accessible through Java reflection */
                 Field workspaceField = AbstractBuild.class.getDeclaredField("workspace");
                 workspaceField.setAccessible(true);
                 
                 /* resolve variables and relative paths */
                 String customWorkspace = env.expand(this.customWorkspace);
                 customWorkspace = Tools.convertPathForOS(customWorkspace, Tools.isWindows(workspace));
                 
                if (!new FilePath(workspace.getChannel(), customWorkspace).exists()) {
                     // relative path, we resolve it against the root of the view
                     customWorkspace = Tools.joinPaths(env.get(CLEARCASE_VIEWPATH_ENVSTR), 
                             customWorkspace, Tools.fileSep(workspace));
                 }
                 
                 /* Then, modify the workspace of the build so that 
                  * the other plugins can use our value. */
                 workspaceField.set(build, customWorkspace);
                 env.put("WORKSPACE", customWorkspace);
                 workspace = new FilePath(workspace.getChannel(), customWorkspace);
                 logger.log("Workspace changed to " + workspace.getRemote());
             }
             
            publishBuildVariables(build);
            
             List<String> pathsForLsHistory = getLsHistoryPaths(cleartool);
             if (!pathsForLsHistory.isEmpty()) {
                 HistoryAction historyAction = createHistoryAction(cleartool);
 //                SaveChangeLogAction saveChangeLogAction = createSaveChangeLogAction(cleartool);
                 
                 // Gather change log
                 ClearCaseChangeLogSet<? extends ChangeLogSet.Entry> changes = null;
                 if (build.getPreviousBuild() != null) {
                     Run<?, ?> prevBuild = build.getPreviousBuild();
                     Date lastBuildTime;
                     long lastBuildMilliSecs = prevBuild.getTimestamp().getTimeInMillis();
                     
                     if (multiSitePollBuffer != 0) {
                         lastBuildMilliSecs = lastBuildMilliSecs - (1000 * 60 * multiSitePollBuffer);
                     }
                     lastBuildTime = new Date(lastBuildMilliSecs);
 
                     String sinceStr = Tools.fmtDuration(System.currentTimeMillis() - lastBuildMilliSecs);
                     logger.log("Retrieving changes since last build (" + sinceStr 
                             + ")...");
                     
                     changes = historyAction.getChanges(build, lastBuildTime, view,
                             getBranchNames(), getLsHistoryPaths(cleartool));
                 }
                 // Save change log
                 if ((changes == null) || changes.isEmptySet()) {
                     // no changes
                     if(!super.createEmptyChangeLog(changelogFile, listener, "changelog")) {
                         logger.log("Empty changelog could not be saved");
                     }
                 } else {
                     changes.saveToFile(changelogFile);
                 }
             }
 
             logger.log("=== End source code retrieval ===");
             
         } catch (ClearToolError cte) {
             if (cte.getCause() instanceof InterruptedException 
                     || cte.getCause() instanceof IOException) {
                 cte.printStackTrace(listener.getLogger());
             } else {
                 listener.getLogger().println(cte.toString());
             }
             build.setResult(Result.FAILURE);
             return false;
             
         } catch (Exception e) {
             e.printStackTrace(listener.getLogger());
             build.setResult(Result.FAILURE);
             return false;
         }
         
         return true;
     }
 
 
     
 
     /*******************************
      **** OVERRIDE *****************
      *******************************/
     
     /** override method {@link hudson.scm.SCM#supportsPolling()} */
     @Override
     public boolean supportsPolling() {
         return true;
     }
 
     /** override method {@link hudson.scm.SCM#requiresWorkspaceForPolling()} */
     @Override
     public boolean requiresWorkspaceForPolling() {
         return true;
     }
 
     
     public void publishEnvVars(FilePath workspace, EnvVars env) {
         if (getNormalizedViewName() != null) {
             env.put(CLEARCASE_VIEWNAME_ENVSTR, getNormalizedViewName());
         }
         if (workspace != null) {
             env.put(CLEARCASE_VIEWPATH_ENVSTR, getExtendedViewPath(workspace));
         }
     }
 
     /** override method {@link hudson.scm.SCM#processWorkspaceBeforeDeletion()} */
     @Override
     public boolean processWorkspaceBeforeDeletion(AbstractProject<?, ?> project,
             FilePath workspace, Node node) throws IOException, InterruptedException 
     {
         if (this.useDynamicView) {
             return true;
         }
         
         boolean isCustomWorkspace = false;
         Logger logger = Logger.getLogger(this.getClass().getName());
         
         if (project.getRootProject() instanceof FreeStyleProject){
             FreeStyleProject free = (FreeStyleProject) project.getRootProject();
             isCustomWorkspace = free.getCustomWorkspace() != null;
         }
         /* Robin Jarry 2010-01-20
          *
          * PROPER CLEANUP OF WORKSPACE
          * - scan all directories in the workspace
          * - call a "cleartool lsview -full -prop <dir_name>" for each dir
          * - if the command returns an error, it's not a view
          * - else it is a view, call the "rmview -force <dir_name>"
          *
          * Robin Jarry 2010-10-11
          * 
          * Changed the algorithm so that it scans all the past builds, 
          * therefore, cleaning views on slaves too.
          */
 
         if (isCustomWorkspace){
             logger.log(Level.WARNING, "The project " + project.getName() + 
                     " uses a custom workspace location. Its workspace cannot be deleted.");
             // if the job has been configured to use a custom workspace, we forbid its deletion 
             return false;
         } else {
             StreamTaskListener listener = new StreamTaskListener(System.out, null);
             
             if ("".equals(node.getNodeName())) {
                 /* 
                  * we are calling "wipeout workspace" on the master, 
                  * the wipeout command needs to be called
                  * on every workspace of every build of the project 
                  */
                 for (AbstractBuild<?, ?> build : project.getBuilds()) {
                     FilePath ws = build.getWorkspace();
                     Node nod = build.getBuiltOn();
                     if (ws != null && ws.listDirectories() != null && !ws.listDirectories().isEmpty()) { 
                         try {
                             /* 
                              * if the node hosting the workspace is connected 
                              * we remove all the views from its workspace 
                              */
                             EnvVars env = build.getEnvironment(listener);
                             ClearCaseConfiguration config = fetchClearCaseConfig(nod.getNodeName());
                             Launcher launch = nod.createLauncher(listener);
                             ClearTool ct = createClearTool(config.getCleartoolExe(), ws, 
                                     nod.getRootPath(), launch, env, null);
                             wipeoutWorkspace(ct, nod, ws);
                         } catch (Exception e) {
                             String message = String.format("Failed to clean views from %s on %s", 
                                     ws.getRemote(), nod.getDisplayName());
                             logger.log(Level.WARNING, message, e);
                             continue;
                         }
                     }
                 }
             } else {
                 /* 
                  * we are on a node, this method is invoked automatically by hudson
                  * for cleanup purposes : we only wipeout the provided workspace 
                  */
                 AbstractBuild<?, ?> validBuild = null;
                 EnvVars env = null;
                 for (AbstractBuild<?, ?> build : project.getBuilds()) {
                     try {
                         validBuild = build;
                         env = build.getEnvironment(listener);
                         break;
                     } catch (Exception e) {
                         continue;
                     }
                 }
                 
                 if (workspace != null && validBuild != null && env != null) {
                     try {
                         ClearCaseConfiguration config = fetchClearCaseConfig(node.getNodeName());
                         Launcher launch = node.createLauncher(listener);
                         ClearTool ct = createClearTool(config.getCleartoolExe(), workspace, 
                                 node.getRootPath(), launch, env, null);
                         
                         wipeoutWorkspace(ct, node, workspace);
                     } catch (Exception e) {
                         String message = String.format("Failed to clean views from %s on %s", 
                                                 workspace.getRemote(), node.getDisplayName());
                         logger.log(Level.WARNING, message, e);
                         return false;
                     }
                 }
             }
             return true;
         }
     }
 
 
     /** implementation of abstract method from {@link hudson.scm.SCM} */
     @Override
     protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project,
             Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline)
             throws IOException, InterruptedException 
     {
         try {
             if (pollForChanges(project, launcher, workspace, listener)) {
                 return PollingResult.SIGNIFICANT;
             } else {
                 return PollingResult.NO_CHANGES;
             }
         } catch (ClearToolError e) {
             listener.getLogger().println(e.toString());
            /* 
             * there was an error during the polling
             * do NOT trigger the build as it will make unwanted failed builds
             */
             return PollingResult.NO_CHANGES;
         }
     }
     
     /** implementation of abstract method from {@link hudson.scm.SCM} */
     @Override
     public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher,
             TaskListener listener) throws IOException, InterruptedException {
         /* not implemented */
         return null;
     }
     
     /*******************************
      **** ABSTRACT *****************
      *******************************/
     
     /**
      * Create a CheckOutAction that will be used by the checkout method.
      * 
      * @return an action that can check out code from a ClearCase repository.
      */
     protected abstract CheckoutAction createCheckoutAction(ClearTool ct, ClearCaseLogger logger, 
             View view, String stgloc);
 
     /**
      * Create a HistoryAction that will be used by the pollChanges() and
      * checkout() method.
      * 
      * @param variableResolver
      * @param launcher
      *            the command line launcher
      * @return an action that can poll if there are any changes a ClearCase
      *         repository.
      */
     protected abstract HistoryAction createHistoryAction(ClearTool ct);
 
     /**
      * Return a set of strings containing the branch names that should be used when
      * polling for changes.
      * 
      * @return Return a set of strings, can not be empty
      */
     public abstract List<String> getBranchNames();
 
     protected abstract List<String> getViewPathsForLSHistory(ClearTool ct)
             throws IOException, InterruptedException, ClearToolError;
 
     /*******************************
      **** METHODS ******************
      *******************************/
     
     protected abstract View createView(String viewTag);
     
     public ClearTool createClearTool(String executable, FilePath workspace, FilePath nodeRoot,
             Launcher launcher, EnvVars env, File logFile)
     {
         CTLauncher ctLauncher = new CTLauncher(executable, workspace, nodeRoot, launcher, env,
                 logFile);
         if (this.useDynamicView) {
             FilePath viewPath = new FilePath(workspace.getChannel(), this.viewDrive);
             return new ClearToolDynamic(ctLauncher, viewPath);
         } else {
             return new ClearToolSnapshot(ctLauncher);
         }
     }
     
     /**
      * Returns a normalized view name that will be used in cleartool commands.
      * It will replace ${JOB_NAME} with the name of the job, ${USERNAME} with
      * the name of the user. This way it will be easier to add new jobs without
      * trying to find an unique view name. It will also replace invalid chars
      * from a view name. 
      * 
      * @param env
      *            the environment against which to resolve the variables in the view pattern
      * @return a string containing no invalid chars.
      */
     private String generateNormalizedViewName(EnvVars env) {
         String normViewName = env.expand(getViewName());
         normViewName = normViewName.replaceAll("[\\s\\\\\\/:\\?\\*\\|]+", "_");
         return normViewName;
     }
 
     private void wipeoutWorkspace(ClearTool cleartool, Node node, FilePath workspace) 
             throws IOException, InterruptedException 
     {
         Logger logger = Logger.getLogger(this.getClass().getName());
 
         String message = String.format("Scanning %s on %s for ClearCase views.", 
                                         workspace.getRemote(), node.getDisplayName());
         logger.log(Level.INFO, message);
 
         for (FilePath f : workspace.listDirectories()) {
             View view;
             String uuid;
             
             try {
                 view = cleartool.getViewInfo(f.getName());
                 uuid = cleartool.getSnapshotViewUuid(f);
             } catch (ClearToolError e) {
                 // the name is not registered as a view tag
                 continue;
             } catch (IOException e) {
                 // there was no view.dat in the folder
                 // no way of retrieving the uuid of the view we do not delete it
                 continue;
             }
             
             if (!view.isDynamic() && view.getUuid().equalsIgnoreCase(uuid)) {
                 try {
                     cleartool.rmview(view);
                     logger.log(Level.INFO, "Removed ClearCase view: " + view.getName());
                 } catch (ClearToolError e) {
                     logger.log(Level.WARNING, "Failed to remove ClearCase view: " 
                                                + view.getName(), e);
                 }
             }
         }
     }
     
     
     
     
 
     protected List<Filter> configureFilters(ClearTool ct) {
         List<Filter> filters = new ArrayList<Filter>();
         filters.add(new DefaultFilter());
         String[] excludedStrings = getExcludedRegionsNormalized();
         boolean windows = Tools.isWindows(ct.getWorkspace());
         
         
         if (excludedStrings != null && excludedStrings.length > 0) {
             for (String s : excludedStrings) {
                 if (!s.equals("")) {
                     filters.add(new FileFilter(FileFilter.Type.DoesNotContainRegxp, s));
                 }
             }
         }
         List<String> loadRules = getLsHistoryPaths(ct);
         if (loadRules != null && !loadRules.isEmpty()) {
             filters.add(new FileFilter(FileFilter.Type.ContainsRegxp, 
                                        Tools.createFileFilterPattern(loadRules, windows)));
         }
         if (isFilteringOutDestroySubBranchEvent()) {
             filters.add(new DestroySubBranchFilter());
         }
         return filters;
     }
     
     
     
    private boolean pollForChanges(AbstractProject<?, ?> project, Launcher l, FilePath workspace,
             TaskListener listener) throws IOException, InterruptedException, ClearToolError {
 
         Run<?, ?> lastBuild = project.getLastBuild();
         if (lastBuild == null) {
             throw new ClearToolError("No previous build has been found, " +
                 "please launch the build manually.");
         }
         Date buildTime = lastBuild.getTimestamp().getTime();
 
         CCParametersAction params = lastBuild.getAction(CCParametersAction.class);
         if (params == null) {
             throw new ClearToolError("No previous build has been found, " +
                 "please launch the build manually.");
         }
         String prevBuildViewName = params.getParameter(CLEARCASE_VIEWNAME_ENVSTR).value;
         String prevBuildViewPath = params.getParameter(CLEARCASE_VIEWPATH_ENVSTR).value;
         if (prevBuildViewName == null || prevBuildViewPath == null) {
             throw new ClearToolError("No previous build has been found, " +
                 "please launch the build manually.");
         }
         
         if (getMultiSitePollBuffer() != 0) {
             long lastBuildMilliSecs = lastBuild.getTimestamp().getTimeInMillis();
             buildTime = new Date(lastBuildMilliSecs - (1000 * 60 * getMultiSitePollBuffer()));
         }
         EnvVars env = lastBuild.getEnvironment(listener);
         if (getEnv() == null) this.setEnv(env);
         
         String nodeName = Computer.currentComputer().getName();
         FilePath nodeRoot = Computer.currentComputer().getNode().getRootPath();
         ClearCaseConfiguration config = fetchClearCaseConfig(nodeName);
        
         /* In this plugin, the commands are displayed in a separate console "cleartool output"
          * because cleartool gets sometimes very verbose and it pollutes the build log.
          * 
          * By default, Hudson prints every command invoked in the console no matter 
          * what the user wants. This is done through the getListener().getLogger().printLn() 
          * method from the Launcher class. 
          * 
          * As there is no way to modify this behaviour, I had to create a new launcher 
          * with a NULL TaskListener so that when Hudson prints something, it goes to 
          * the trash instead of poping in the middle of the build log */
        Launcher launcher = Executor.currentExecutor().getOwner().getNode().createLauncher(
                TaskListener.NULL);
         ClearTool ct = createClearTool(config.getCleartoolExe(), workspace, nodeRoot, 
                 launcher, env, null);
         HistoryAction historyAction = createHistoryAction(ct);
 
         View prevBuildView = createView(prevBuildViewName);
         prevBuildView.setViewPath(prevBuildViewPath);
         
         if (prevBuildView.isDynamic()) {
             ct.startView(prevBuildView);
         } else {
             if (!new FilePath(workspace.getChannel(), prevBuildViewPath).exists()) {
                 // the snapshot view is not created yet or has been deleted
                 throw new ClearToolError("No snapshot view found in the workspace, " +
                 		"please launch the build manually.");
             }
         }
         return historyAction.pollChanges(buildTime, prevBuildView, getBranchNames(), getViewPaths(workspace));
     }
     
     /**
      * Retrieves the user selected clearcase configuration from the base descriptor. 
      * 
      * If none was specified, it tries to return a config matching the name of the node
      * on which the build is taking place.
      * 
      * If no matching config can be found, it returns the default one.
      * 
      * @param nodeName  
      *              the name of the node on which the build is taking place. 
      *              {@code null} if it is the master. 
      */
     public ClearCaseConfiguration fetchClearCaseConfig(String nodeName) {
         if (clearcaseConfig == null || 
                 ClearCaseBaseSCMDescriptor.DEFAULT_CONFIG.equalsIgnoreCase(clearcaseConfig)) {
             return ClearCaseBaseSCM.BASE_DESCRIPTOR.getConfiguration(nodeName);
         } else {
             return ClearCaseBaseSCM.BASE_DESCRIPTOR.getConfiguration(clearcaseConfig);
         }
         
     }
     
     /*******************************
      **** ADVANCED GETTERS *********
      *******************************/
 
     public View getView() {
         if (view == null) {
             view = createView(getNormalizedViewName());
         }
         return view;
     }
     
     public String[] getExcludedRegionsNormalized() {
         return excludedRegions == null ? null : excludedRegions.split("[\\r\\n]+");
     }
     
     /**
      * Makes sure the view drive is properly formated with *NO* ending slash or backslash
      * 
      * @author Robin Jarry
      * @param workspace 
      */
     protected String getViewRoot(FilePath workspace){
         String viewRoot = getViewDrive();
         while (viewRoot.endsWith("/") || viewRoot.endsWith("\\")) {
             viewRoot = viewRoot.substring(0, viewRoot.length() - 1);
         }
         return viewRoot;
     }
     
     public String getExtendedViewPath(FilePath workspace) {
         if (getNormalizedViewName() == null) {
             return workspace.getRemote();
         } else {
             if (isUseDynamicView()) {
                 FilePath root = new FilePath(new File(getViewRoot(workspace)));
                 return root.child(getNormalizedViewName()).getRemote();
             } else {
                 return workspace.child(getNormalizedViewName()).getRemote();
             }
         }
     }
     
     /**
      * Return a set of strings containing the paths in the view that should be used
      * when polling for changes.
      * 
      * any leading file separator character is automatically removed from the load rules
      * 
      * @return A set of strings that will be used by the lshistory command and for
      *         constructing the config spec, etc.
      */
     public List<String> getViewPaths(FilePath workspace) {
         List<String> rules = new ArrayList<String>();
         boolean windows = Tools.isWindows(workspace);
         Pattern regexp = Pattern.compile("(load )?(.+)");
         Matcher matcher = regexp.matcher(this.loadRules);
         
         while (matcher.find()){
             String rule = Tools.convertPathForOS(matcher.group(2), windows);
             while (rule.startsWith("/") || rule.startsWith("\\")) {
                 rule = rule.substring(1).trim();
             }
             rules.add(rule);
         }
         
         return rules;
     }
     
     private List<String> getLsHistoryPaths(ClearTool ct) {
         if (this.lsHistoryPaths == null) {
             try {
                 this.lsHistoryPaths = this.getViewPathsForLSHistory(ct);
             } catch (Exception e) {
                 this.lsHistoryPaths = null;
             }
         }
         return this.lsHistoryPaths;
     }
     
 
     
     /*******************************
      **** GETTERS ******************
      *******************************/
     
     public String getNormalizedViewName() {
         if (normalizedViewName == null && this.getEnv() != null) {
             normalizedViewName = generateNormalizedViewName(this.getEnv());
         }
         return normalizedViewName;
     }
 
 
     public List<String> getLsHistoryPaths() {
         return lsHistoryPaths;
     }
 
     public boolean isUseDynamicView() {
         return useDynamicView;
     }
 
     public String getViewName() {
         if (viewName == null) {
             if (Functions.isWindows()) {
                 return "${COMPUTERNAME}_${JOB_NAME}_hudson";
             } else {
                 return "${HOSTNAME}_${JOB_NAME}_hudson";
             }
         } else {
             return viewName;
         }
     }
 
     public boolean isUseUpdate() {
         return useUpdate;
     }
 
     public String getExcludedRegions() {
         return excludedRegions;
     }
 
     public String getLoadRules() {
         return loadRules;
     }
 
     public String getViewDrive() {
         if (viewDrive == null) {
             if (Functions.isWindows()) {
                 return "M:\\";
             } else {
                 return "/view";
             }
         } else {
             return viewDrive;
         }
     }
 
     public int getMultiSitePollBuffer() {
         return multiSitePollBuffer;
     }
 
     public boolean isFilteringOutDestroySubBranchEvent() {
         return filteringOutDestroySubBranchEvent;
     }
 
 
     public String getMkviewOptionalParam() {
         return mkviewOptionalParam;
     }
     
     public String getClearcaseConfig() {
         return clearcaseConfig;
     }
 
     public boolean isDoNotUpdateConfigSpec() {
         return this.doNotUpdateConfigSpec;
     }
     
     public EnvVars getEnv() {
         return env;
     }
 
     public void setEnv(EnvVars env) {
         this.env = env;
     }
     
     public FilePath getOriginalWorkspace() {
         return originalWorkspace;
     }
 
     public String getCustomWorkspace() {
         return customWorkspace;
     }
 
     /*******************************
      **** UTILITIES ****************
      *******************************/
     
     protected void publishBuildVariables(AbstractBuild<?, ?> build) {
         String name = getNormalizedViewName();
         String path = null;
         if (build.getWorkspace() != null) {
             // bug fix : when calling the build.getWorkspace() on a custom workspace
             // sometimes, the method returns null.
             path = getExtendedViewPath(build.getWorkspace());
         }
         List<StringParameterValue> parameters = Tools.getCCParameters(build);
         if (name != null) {
             parameters.add(new StringParameterValue(CLEARCASE_VIEWNAME_ENVSTR, name));
         }
         if (path != null) {
             parameters.add(new StringParameterValue(CLEARCASE_VIEWPATH_ENVSTR, path));
         }
     }
     
     /******************************
      **** BUILD LISTENER CLASS ****
      ******************************/
     
     @SuppressWarnings("rawtypes")
     @Extension
     public static class RestoreWorkspaceListener extends RunListener<AbstractBuild> {
 
         public RestoreWorkspaceListener() {
             this(AbstractBuild.class);
         }
         
         protected RestoreWorkspaceListener(Class<AbstractBuild> targetType) {
             super(targetType);
         }
         
         @Override
         public void onCompleted(AbstractBuild build, TaskListener listener) {
             SCM scm = build.getProject().getScm();
             if (scm instanceof AbstractClearCaseSCM) {
                 try {
                     File ctLogFile = ClearToolLogFile.getCleartoolLogFile(build);
                     ClearCaseLogger logger = new ClearCaseLogger(listener, ctLogFile);
                     FilePath originalWs = ((AbstractClearCaseSCM) scm).getOriginalWorkspace();
                     String customWs = ((AbstractClearCaseSCM) scm).getCustomWorkspace();
                     if (originalWs != null && customWs != null) {
                         Field workspaceField = AbstractBuild.class.getDeclaredField("workspace");
                         workspaceField.setAccessible(true);
                         workspaceField.set(build, originalWs.getRemote());
                         build.getEnvironment(listener).put("WORKSPACE", originalWs.getRemote());
                         logger.log("Workspace restored to " + originalWs.getRemote());
                     }
                 } catch (Exception e) {
                     /* pass */;
                 }
             }
         }
         
     }
 }
