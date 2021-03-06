 package hudson.tasks;
 
 import hudson.Launcher;
 import hudson.maven.MavenModuleSetBuild;
 import hudson.matrix.MatrixAggregatable;
 import hudson.matrix.MatrixAggregator;
 import hudson.matrix.MatrixBuild;
 import hudson.matrix.MatrixRun;
 import hudson.model.AbstractBuild;
 import hudson.model.AbstractProject;
 import hudson.model.BuildListener;
 import hudson.model.DependecyDeclarer;
 import hudson.model.DependencyGraph;
 import hudson.model.Descriptor;
 import hudson.model.Hudson;
 import hudson.model.Item;
 import hudson.model.Items;
 import hudson.model.Job;
 import hudson.model.Project;
 import hudson.model.Result;
 import hudson.model.listeners.ItemListener;
 import hudson.util.FormFieldValidator;
 import net.sf.json.JSONObject;
 import org.kohsuke.stapler.DataBoundConstructor;
 import org.kohsuke.stapler.StaplerRequest;
 import org.kohsuke.stapler.StaplerResponse;
 
 import javax.servlet.ServletException;
 import java.io.IOException;
 import java.io.PrintStream;
 import java.util.Collection;
 import java.util.List;
 import java.util.StringTokenizer;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 /**
  * Triggers builds of other projects.
  *
  * @author Kohsuke Kawaguchi
  */
 public class BuildTrigger extends Publisher implements DependecyDeclarer, MatrixAggregatable {
 
     /**
      * Comma-separated list of other projects to be scheduled.
      */
     private String childProjects;
 
     /**
      * Threshold status to trigger other builds.
      *
      * For compatibility reasons, this field could be null, in which case
      * it should read as "SUCCESS".
      */
     private final Result threshold;
 
     @DataBoundConstructor
     public BuildTrigger(String childProjects, boolean evenIfUnstable) {
         this(childProjects,evenIfUnstable ? Result.UNSTABLE : Result.SUCCESS);
     }
 
     public BuildTrigger(String childProjects, Result threshold) {
        if(childProjects==null)
            throw new IllegalArgumentException();
         this.childProjects = childProjects;
         this.threshold = threshold;
     }
 
     public BuildTrigger(List<AbstractProject> childProjects, Result threshold) {
         this(Items.toNameList(childProjects),threshold);
     }
 
     public String getChildProjectsValue() {
         return childProjects;
     }
 
     public Result getThreshold() {
         if(threshold==null)
             return Result.SUCCESS;
         else
             return threshold;
     }
 
     public List<AbstractProject> getChildProjects() {
         return Items.fromNameList(childProjects,AbstractProject.class);
     }
 
     /**
      * Checks if this trigger has the exact same set of children as the given list.
      */
     public boolean hasSame(Collection<? extends AbstractProject> projects) {
         List<AbstractProject> children = getChildProjects();
         return children.size()==projects.size() && children.containsAll(projects);
     }
 
     public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
         if (build instanceof MatrixRun)
             return true;    // when configured for a matrix project, run against MatrixBuild, not MatrixRun
         if (build instanceof MavenModuleSetBuild)
             return true;    // because Maven sets up dependencies on its own, that triggers any downstreams that we configured.
 
         return execute(build, listener);
     }
 
     private boolean execute(AbstractBuild build, BuildListener listener) {
         if(!build.getResult().isWorseThan(getThreshold())) {
             PrintStream logger = listener.getLogger();
             for (AbstractProject p : getChildProjects()) {
                 if(p.isDisabled()) {
                     logger.println(Messages.BuildTrigger_Disabled(p.getName()));
                     continue;
                 }
 
                 // this is not completely accurate, as a new build might be triggered
                 // between these calls
                 String name = p.getName()+" #"+p.getNextBuildNumber();
                 if(p.scheduleBuild()) {
                     logger.println(Messages.BuildTrigger_Triggering(name));
                 } else {
                     logger.println(Messages.BuildTrigger_InQueue(name));
                 }
             }
         }
 
         return true;
     }
 
     public void buildDependencyGraph(AbstractProject owner, DependencyGraph graph) {
         graph.addDependency(owner,getChildProjects());
     }
 
     @Override
     public boolean needsToRunAfterFinalized() {
         return true;
     }
 
     public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
         return new MatrixAggregator(build, launcher, listener) {
             @Override
             public boolean endBuild() throws InterruptedException, IOException {
                 return execute(build,listener);
             }
         };
     }
 
     /**
      * Called from {@link Job#renameTo(String)} when a job is renamed.
      *
      * @return true
      *      if this {@link BuildTrigger} is changed and needs to be saved.
      */
     public boolean onJobRenamed(String oldName, String newName) {
         // quick test
         if(!childProjects.contains(oldName))
             return false;
 
         boolean changed = false;
 
         // we need to do this per string, since old Project object is already gone.
         String[] projects = childProjects.split(",");
         for( int i=0; i<projects.length; i++ ) {
             if(projects[i].trim().equals(oldName)) {
                 projects[i] = newName;
                 changed = true;
             }
         }
 
         if(changed) {
             StringBuilder b = new StringBuilder();
             for (String p : projects) {
                 if(b.length()>0)    b.append(',');
                 b.append(p);
             }
             childProjects = b.toString();
         }
 
         return changed;
     }
 
     public Descriptor<Publisher> getDescriptor() {
         return DESCRIPTOR;
     }
 
 
     public static final Descriptor<Publisher> DESCRIPTOR = new DescriptorImpl();
 
     public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
         public DescriptorImpl() {
             super(BuildTrigger.class);
 
             Hudson.getInstance().getJobListeners().add(new ItemListener() {
                 @Override
                 public void onRenamed(Item item, String oldName, String newName) {
                     // update BuildTrigger of other projects that point to this object.
                     // can't we generalize this?
                     for( Project p : Hudson.getInstance().getProjects() ) {
                         BuildTrigger t = (BuildTrigger) p.getPublishers().get(BuildTrigger.DESCRIPTOR);
                         if(t!=null) {
                             if(t.onJobRenamed(oldName,newName)) {
                                 try {
                                     p.save();
                                 } catch (IOException e) {
                                     LOGGER.log(Level.WARNING, "Failed to persist project setting during rename from "+oldName+" to "+newName,e);
                                 }
                             }
                         }
                     }
                 }
             });
         }
 
         public String getDisplayName() {
             return Messages.BuildTrigger_DisplayName();
         }
 
         public String getHelpFile() {
             return "/help/project-config/downstream.html";
         }
 
         public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
             return new BuildTrigger(
                 formData.getString("childProjects"),
                 formData.has("evenIfUnstable") && formData.getBoolean("evenIfUnstable"));
         }
 
         @Override
         public boolean isApplicable(Class<? extends AbstractProject> jobType) {
             return true;
         }
 
         public boolean showEvenIfUnstableOption(Class<? extends AbstractProject> jobType) {
             // UGLY: for promotion process, this option doesn't make sense. 
             return !jobType.getName().contains("PromotionProcess");
         }
 
         /**
          * Form validation method.
          */
         public void doCheck( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
             new FormFieldValidator(req,rsp,true) {
                 protected void check() throws IOException, ServletException {
                     String list = request.getParameter("value");
 
                     StringTokenizer tokens = new StringTokenizer(list,",");
                     while(tokens.hasMoreTokens()) {
                         String projectName = tokens.nextToken().trim();
                         Item item = Hudson.getInstance().getItemByFullName(projectName,Item.class);
                         if(item==null) {
                             error(Messages.BuildTrigger_NoSuchProject(projectName,AbstractProject.findNearest(projectName).getName()));
                             return;
                         }
                         if(!(item instanceof AbstractProject)) {
                             error(Messages.BuildTrigger_NotBuildable(projectName));
                             return;
                         }
                     }
 
                     ok();
                 }
             }.process();
         }
     }
 
     private static final Logger LOGGER = Logger.getLogger(BuildTrigger.class.getName());
 }
