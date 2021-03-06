 package hudson.plugins.clearcase;
 
 import hudson.FilePath;
 import hudson.Launcher;
 import hudson.Util;
 import hudson.model.AbstractBuild;
 import hudson.model.AbstractProject;
 import hudson.model.BuildListener;
 import hudson.model.Hudson;
 import hudson.model.Item;
 import hudson.model.Run;
 import hudson.model.TaskListener;
 import hudson.model.listeners.ItemListener;
 import hudson.plugins.clearcase.action.ChangeLogAction;
 import hudson.plugins.clearcase.action.CheckOutAction;
 import hudson.plugins.clearcase.action.PollAction;
 import hudson.plugins.clearcase.action.SaveChangeLogAction;
 import hudson.plugins.clearcase.util.BuildVariableResolver;
 import hudson.plugins.clearcase.util.EventRecordFilter;
 import hudson.scm.ChangeLogSet;
 import hudson.scm.SCM;
 import hudson.util.StreamTaskListener;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Date;
 import java.util.List;
 import java.util.Map;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 
 /**
  * Abstract class for ClearCase SCM.
  * The class contains the logic around checkout and polling, the deriving
  * classes only have to implement the specific checkout and polling logic.
  */
 public abstract class AbstractClearCaseScm extends SCM {
 
     public static final String CLEARCASE_VIEWNAME_ENVSTR = "CLEARCASE_VIEWNAME";
     public static final String CLEARCASE_VIEWPATH_ENVSTR = "CLEARCASE_VIEWPATH";
 
     private final String viewName;
     private final String mkviewOptionalParam;
     private final boolean filteringOutDestroySubBranchEvent;
     protected transient String normalizedViewName;
     
     public AbstractClearCaseScm(final String viewName, String mkviewOptionalParam, 
             boolean filterOutDestroySubBranchEvent) {
         this.viewName = viewName;
         this.mkviewOptionalParam = mkviewOptionalParam;
         this.filteringOutDestroySubBranchEvent = filterOutDestroySubBranchEvent;
         
         createAndRegisterListener();
     }
 
 
     
     /**
      * Create a CheckOutAction that will be used by the checkout method.
      * @param launcher the command line launcher
      * @return an action that can check out code from a ClearCase repository.
      */
     protected abstract CheckOutAction createCheckOutAction(ClearToolLauncher launcher);
     
     /**
      * Create a PollAction that will be used by the pollChanges() method.
      * @param launcher the command line launcher
      * @return an action that can poll if there are any changes a ClearCase repository.
      */
     protected abstract PollAction createPollAction(ClearToolLauncher launcher);
 
     /**
      * Create a SaveChangeLog action that is used to save a change log
      * @param launcher the command line launcher
      * @return an action that can save a change log to the Hudson changlog file
      */
     protected abstract SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher);
 
     /**
      * Create a ChangeLogAction that will be used to get the change logs for a CC repository
      * @param launcher the command line launcher
      * @param build the current build
      * @return an action that returns the change logs for a CC repository
      */
     protected abstract ChangeLogAction createChangeLogAction(ClearToolLauncher launcher, AbstractBuild<?, ?> build,Launcher baseLauncher);
     
     /**
      * Return string array containing the branch names that should be used when polling for changes.
      * @return a string array, can not be empty
      */
     public abstract String[] getBranchNames();
     
     /**
      * Return string array containing the paths in the view that should be used when polling for changes.
      * @param viewPath the file path for the view
      * @return string array that will be used by the lshistory command
      */
     public abstract String[] getViewPaths(FilePath viewPath) throws IOException, InterruptedException;
 
     @Override
     public boolean supportsPolling() {
         return true;
     }
     
     @Override
     public boolean requiresWorkspaceForPolling() {
         return true;
     }
 
     @Override
     public FilePath getModuleRoot(FilePath workspace) {
         if (normalizedViewName == null) {
             return super.getModuleRoot(workspace);
         } else {
             return workspace.child(normalizedViewName);
         }
     }
 
     public String getViewName() {
         if (viewName == null) {
             return "${USER_NAME}_${JOB_NAME}_${NODE_NAME}_view";
         } else {
             return viewName;
         }
     }
 
     /**
      * Returns a normalized view name that will be used in cleartool commands.
      * It will replace ${JOB_NAME} with the name of the job, * ${USER_NAME} with
      * the name of the user. This way it will be easier to add new jobs without trying
      * to find an unique view name. It will also replace invalid chars from a view name.
      * @param project the project to get the name from
      * @return a string containing no invalid chars.
      */
     public String getNormalizedViewName(AbstractBuild<?, ?> build,Launcher launcher) {
         if (normalizedViewName == null) {
             normalizedViewName = viewName;
             if (build != null) {
                 normalizedViewName = Util.replaceMacro(viewName, new BuildVariableResolver(build,launcher));
             }
             normalizedViewName = normalizedViewName.replaceAll("[\\s\\\\\\/:\\?\\*\\|]+", "_");
         }
         return normalizedViewName;
     }
 
     /**
      * Returns the user configured optional params that will be used in when creating a new view.
      * @return string containing optional mkview parameters.
      */
     public String getMkviewOptionalParam() {
         return mkviewOptionalParam;
     }
     
     /**
      * Returns if the "Destroyed branch" event should be filtered out or not.
      * For more information about the boolean, see the full discussion at 
      * http://www.nabble.com/ClearCase-build-triggering-td17507838i20.html
      * "Usually, CC admins have a CC trigger, fires on an uncheckout event, 
      * that destroys empty branches."
      * @return true if the "Destroyed branch" event should be filtered out or not; false otherwise 
      */
     public boolean isFilteringOutDestroySubBranchEvent() {
         return filteringOutDestroySubBranchEvent;
     }
 
     /**
      * Adds the env variable for the ClearCase SCMs.
      * CLEARCASE_VIEWNAME - The name of the clearcase view.
      * CLEARCASE_VIEWPATH - The absolute path to the clearcase view.
      */
     @Override
     public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
         if (normalizedViewName != null) {
                         
             env.put(CLEARCASE_VIEWNAME_ENVSTR, normalizedViewName);
         
             String workspace = env.get("WORKSPACE");
             if (workspace != null) {
                 env.put(CLEARCASE_VIEWPATH_ENVSTR, workspace + File.separator + normalizedViewName);
             }
         }
     }
 
     @Override
     public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
         ClearToolLauncher clearToolLauncher = createClearToolLauncher(listener, workspace, launcher);
 
         
         // Create actions
         CheckOutAction checkoutAction = createCheckOutAction(clearToolLauncher);
         ChangeLogAction changeLogAction = createChangeLogAction(clearToolLauncher, build,launcher);
         SaveChangeLogAction saveChangeLogAction = createSaveChangeLogAction(clearToolLauncher);
 
         EventRecordFilter filter = new EventRecordFilter();
         filter.setFilterOutDestroySubBranchEvent(isFilteringOutDestroySubBranchEvent());
         
         // Checkout code
         checkoutAction.checkout(launcher, workspace, getNormalizedViewName(build,launcher));
         
         // Gather change log
         List<? extends ChangeLogSet.Entry> changelogEntries = null;        
         if (build.getPreviousBuild() != null) {
             Date lastBuildTime = build.getPreviousBuild().getTimestamp().getTime();
             changelogEntries = changeLogAction.getChanges(filter, lastBuildTime, getNormalizedViewName(build,launcher), getBranchNames(), getViewPaths(workspace.child(getNormalizedViewName(build,launcher))));
         }        
 
         // Save change log
         if ((changelogEntries == null) || (changelogEntries.isEmpty())) {
             // no changes
             return createEmptyChangeLog(changelogFile, listener, "changelog");
         } else {
             saveChangeLogAction.saveChangeLog(changelogFile, changelogEntries);
         }        
                 
         return true;
     }
 
     @Override
     public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
         Run lastBuild = project.getLastBuild();
         if (lastBuild == null) {
             return true;
         } else {
             Date buildTime = lastBuild.getTimestamp().getTime();
             
             EventRecordFilter filter = new EventRecordFilter();
             filter.setFilterOutDestroySubBranchEvent(isFilteringOutDestroySubBranchEvent());
             
             PollAction pollAction = createPollAction(createClearToolLauncher(listener, workspace, launcher));
             String normalizedViewName = getNormalizedViewName((AbstractBuild) lastBuild,launcher);
             return pollAction.getChanges(filter, buildTime, normalizedViewName, getBranchNames(), getViewPaths(workspace.child(normalizedViewName)));
         }
     }
     
     /**
      * Creates a Hudson clear tool launcher.
      * @param listener listener to write command output to 
      * @param workspace the workspace for the job
      * @param launcher actual launcher to launch commands with
      * @return a clear tool launcher that uses Hudson for launching commands 
      */
     protected ClearToolLauncher createClearToolLauncher(TaskListener listener, FilePath workspace, Launcher launcher) {
         return new HudsonClearToolLauncher(PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(), 
                 getDescriptor().getDisplayName(), listener, workspace, launcher);
     }
     
     protected ClearTool createClearTool(ClearToolLauncher launcher) {
         return new ClearToolSnapshot(launcher, mkviewOptionalParam);
     }
     
     /**
      * Register listeners for Hudson events. At the moment we listen to onDeleted and try to remove 
      * the ClearCase view that was created for this job.
      * @param viewName	the name of the view
      */
 	protected void createAndRegisterListener() {
 		Hudson hudson = Hudson.getInstance();
 		if (hudson == null) {
 			// Probably a JUnit test run?
 			Logger.getLogger(AbstractClearCaseScm.class.getName()).log(Level.INFO, "Failed to get Hudson instance");
 			
 			return;
 		}
 		hudson.getJobListeners().add(new ItemListener() {
         	@Override
         	public void onDeleted(Item item) {
         		if (item instanceof AbstractProject) {
        			AbstractProject<?, ?> project = (AbstractProject<?, ?>)item;
         			if (project.getScm() instanceof AbstractClearCaseScm) {        				
         				//TaskListener listener = TaskListener.NULL;
         				StreamTaskListener listener = new StreamTaskListener(System.out);
         				Launcher launcher = Hudson.getInstance().createLauncher(listener);
         				ClearTool ct = createClearTool(createClearToolLauncher(listener, project.getWorkspace().getParent().getParent(), launcher));
         				try {
 							ct.rmview(getNormalizedViewName(null, launcher));
 						} catch (Exception e) {
 							Logger.getLogger(AbstractClearCaseScm.class.getName()).log(Level.WARNING, "Failed to remove ClearCase view", e);
 						}
         			}
         		}
         	}
         });
 	}
 }
