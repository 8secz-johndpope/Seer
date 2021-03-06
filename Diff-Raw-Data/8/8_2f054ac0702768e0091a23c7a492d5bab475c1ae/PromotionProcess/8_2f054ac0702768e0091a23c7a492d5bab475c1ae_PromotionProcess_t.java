 package hudson.plugins.promoted_builds;
 
 import antlr.ANTLRException;
 import hudson.BulkChange;
 import hudson.Extension;
 import hudson.Util;
 import hudson.model.AbstractBuild;
 import hudson.model.AbstractProject;
 import hudson.model.Action;
 import hudson.model.AutoCompletionCandidates;
 import hudson.model.Cause;
 import hudson.model.Cause.LegacyCodeCause;
 import hudson.model.DependencyGraph;
 import hudson.model.Describable;
 import hudson.model.Descriptor;
 import hudson.model.Descriptor.FormException;
 import hudson.model.Failure;
 import hudson.model.FreeStyleProject;
 import hudson.model.Hudson;
 import hudson.model.ItemGroup;
 import hudson.model.JDK;
 import hudson.model.Job;
 import hudson.model.Label;
 import hudson.model.ParametersAction;
 import hudson.model.PermalinkProjectAction.Permalink;
 import hudson.model.Queue.Item;
 import hudson.model.Run;
 import hudson.model.Saveable;
 import hudson.model.labels.LabelAtom;
 import hudson.model.labels.LabelExpression;
 import hudson.tasks.BuildStep;
 import hudson.tasks.BuildStepDescriptor;
 import hudson.tasks.Builder;
 import hudson.tasks.Publisher;
 import hudson.util.DescribableList;
 import hudson.util.FormValidation;
 import jenkins.model.Jenkins;
 import net.sf.json.JSONObject;
 import org.kohsuke.stapler.QueryParameter;
 import org.kohsuke.stapler.StaplerRequest;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Set;
 import java.util.concurrent.Future;
 import java.util.logging.Logger;
 import java.util.regex.Pattern;
 
 /**
  * A dummy {@link AbstractProject} to carry out promotion operations.
  *
  * @author Kohsuke Kawaguchi
  */
 public final class PromotionProcess extends AbstractProject<PromotionProcess,Promotion> implements Saveable, Describable<PromotionProcess> {
 
     /**
      * {@link PromotionCondition}s. All have to be met for a build to be promoted.
      */
     public final DescribableList<PromotionCondition,PromotionConditionDescriptor> conditions =
             new DescribableList<PromotionCondition, PromotionConditionDescriptor>(this);
     /**
      * The icon that represents this promotion process. This is the name of
      * the GIF icon that can be found in ${rootURL}/plugin/promoted-builds/icons/16x16/
      * and ${rootURL}/plugin/promoted-builds/icons/32x32/, e.g. <code>"star-gold"</code>.
      */
     public String icon;
     
     /**
      * The label that promotion process can be run on.
      */
     public String assignedLabel;
     
     private List<BuildStep> buildSteps = new ArrayList<BuildStep>();
 
     /*package*/ PromotionProcess(JobPropertyImpl property, String name) {
         super(property, name);
     }
 
     /*package*/ PromotionProcess(ItemGroup parent, String name) {
         super(parent, name);
     }
 
     /**
      * Creates unconnected {@link PromotionProcess} instance from the JSON configuration.
      * This is mostly only useful for capturing its configuration in XML format.
      */
     public static PromotionProcess fromJson(StaplerRequest req, JSONObject o) throws FormException, IOException {
         String name = o.getString("name");
         try {
             Hudson.checkGoodName(name);
         } catch (Failure f) {
             throw new Descriptor.FormException(f.getMessage(), name);
         }
         PromotionProcess p = new PromotionProcess(null,name);
         BulkChange bc = new BulkChange(p);
         try {
             p.configure(req, o); // apply configuration. prevent it from trying to save to disk while we do this
         } finally {
             bc.abort();
         }
         return p;
     }
 
     @Override
     public void doSetName(String name) {
         super.doSetName(name);
     }
 
     /*package*/ void configure(StaplerRequest req, JSONObject c) throws Descriptor.FormException, IOException {
         // apply configuration
         conditions.rebuild(req,c.optJSONObject("conditions"), PromotionCondition.all());
 
         buildSteps = (List)Descriptor.newInstancesFromHeteroList(
                 req, c, "buildStep", (List) PromotionProcess.getAll());
         icon = c.getString("icon");
         if (c.optBoolean("hasAssignedLabel")) {
             assignedLabel = Util.fixEmptyAndTrim(c.optString("assignedLabelString"));
         } else {
             assignedLabel = null;
         }
         save();
     }
 
     /**
      * Returns the root project value.
      *
      * @return the root project value.
      */
     @Override
     public AbstractProject getRootProject() {
     	return getParent().getOwner().getRootProject();
     }
     
     @Override
     public JobPropertyImpl getParent() {
         return (JobPropertyImpl)super.getParent();
     }
 
     /**
      * Gets the owner {@link AbstractProject} that configured {@link JobPropertyImpl} as
      * a job property.
      */
     public AbstractProject<?,?> getOwner() {
         return getParent().getOwner();
     }
 
     /**
      * Get the promotion condition by referencing it fully qualified class name
      */
     public PromotionCondition getPromotionCondition(String promotionClassName) {
         for (PromotionCondition condition : conditions) {
             if (condition.getClass().getName().equals(promotionClassName)) {
                 return condition;
             }
         }
 
         return null;
     }
     
     public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
         // TODO: extract from the buildsSteps field? Or should I separate builders and publishers?
         return new DescribableList<Publisher,Descriptor<Publisher>>(this);
     }
 
     protected Class<Promotion> getBuildClass() {
         return Promotion.class;
     }
 
     public List<BuildStep> getBuildSteps() {
         return buildSteps;
     }
 
     /**
      * Gets the textual representation of the assigned label as it was entered by the user.
      */
     @Override
     public String getAssignedLabelString() {
         if (assignedLabel == null) return null;
         try {
             LabelExpression.parseExpression(assignedLabel);
             return assignedLabel;
         } catch (ANTLRException e) {
             // must be old label or host name that includes whitespace or other unsafe chars
             return LabelAtom.escape(assignedLabel);
         }
     }
    
     @Override public Label getAssignedLabel() {
         // Really would like to run on the exact node that the promoted build ran on,
         // not just the same label.. but at least this works if job is tied to one node:
         if (assignedLabel == null) return getOwner().getAssignedLabel();
 
         return Hudson.getInstance().getLabel(assignedLabel);
     }
 
     @Override public JDK getJDK() {
         return getOwner().getJDK();
     }
 
     /**
      * Gets the customWorkspace of the owner project.
      *
      * Support for FreeStyleProject only.
      * @return customWorkspace
      */
     public String getCustomWorkspace() {
         AbstractProject<?, ?> p = getOwner();
         if (p instanceof FreeStyleProject)
             return ((FreeStyleProject) p).getCustomWorkspace();
         return null;
     }
     
     /**
      * Get the icon name, without the extension. It will always return a non null
      * and non empty string, as <code>"star-gold"</code> is used for compatibility
      * for older promotions configurations.
      * 
      * @return the icon name
      */
     public String getIcon() {
     	return getIcon(icon);
     }
 
     /**
      * Handle compatibility with pre-1.8 configs.
      * 
      * @param sIcon
      *      the name of the icon used by this promotion; if null or empty,
      *      we return the gold icon for compatibility with previous releases
      * @return the icon file name for this promotion
      */
     private static String getIcon(String sIcon) {
     	if ((sIcon == null) || sIcon.equals(""))
             return "star-gold";
     	else
             return sIcon;
     }
 
     /**
      * Get the badges of conditions that were passed for this promotion for the build
      */
     public List<PromotionBadge> getMetQualifications(AbstractBuild<?,?> build) {
         List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
         for (PromotionCondition cond : conditions) {
             PromotionBadge b = cond.isMet(this, build);
 
             if (b != null)
                 badges.add(b);
         }
         return badges;
     }
 
     /**
      * Get the conditions that have not been met for this promotion for the build
      */
     public List<PromotionCondition> getUnmetConditions(AbstractBuild<?,?> build) {
         List<PromotionCondition> unmetConditions = new ArrayList<PromotionCondition>();
 
         for (PromotionCondition cond : conditions) {
             if (cond.isMet(this, build) == null)
                 unmetConditions.add(cond);
         }
 
         return unmetConditions;
     }
 
     /**
      * Checks if all the conditions to promote a build is met.
      *
      * @return
      *      null if promotion conditions are not met.
      *      otherwise returns a list of badges that record how the promotion happened.
      */
     public Status isMet(AbstractBuild<?,?> build) {
         List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
         for (PromotionCondition cond : conditions) {
             PromotionBadge b = cond.isMet(this, build);
             if(b==null)
                 return null;
             badges.add(b);
         }
         return new Status(this,badges);
     }
 
     /**
      * @deprecated
      *      Use {@link #considerPromotion2(AbstractBuild)}
      */
     public boolean considerPromotion(AbstractBuild<?,?> build) throws IOException {
         return considerPromotion2(build)!=null;
     }
 
     /**
      * Checks if the build is promotable, and if so, promote it.
      *
      * @return
      *      null if the build was not promoted, otherwise Future that kicks in when the build is completed.
      */
     public Future<Promotion> considerPromotion2(AbstractBuild<?,?> build) throws IOException {
        if (!isActive())
            return null;    // not active

         PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
 
         // if it's already promoted, no need to do anything.
         if(a!=null && a.contains(this))
             return null;
 
         LOGGER.fine("Considering the promotion of "+build+" via "+getName());
         Status qualification = isMet(build);
         if(qualification==null)
             return null; // not this time
 
         LOGGER.fine("Promotion condition of "+build+" is met: "+qualification);
         Future<Promotion> f = promote2(build, new LegacyCodeCause(), qualification); // TODO: define promotion cause
         if (f==null)
             LOGGER.warning(build+" qualifies for a promotion but the queueing failed.");
         return f;
     }
 
     public void promote(AbstractBuild<?,?> build, Cause cause, PromotionBadge... badges) throws IOException {
         promote2(build,cause,new Status(this,Arrays.asList(badges)));
     }
 
     /**
      * @deprecated
      *      Use {@link #promote2(AbstractBuild, Cause, Status)}
      */
     public void promote(AbstractBuild<?,?> build, Cause cause, Status qualification) throws IOException {
         promote2(build,cause,qualification);
     }
     
     /**
      * Promote the given build by using the given qualification.
      *
      * @param cause
      *      Why the build is promoted?
      * @return
      *      Future to track the completion of the promotion.
      */
     public Future<Promotion> promote2(AbstractBuild<?,?> build, Cause cause, Status qualification) throws IOException {
         PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
         // build is qualified for a promotion.
         if(a!=null) {
             a.add(qualification);
         } else {
             build.addAction(new PromotedBuildAction(build,qualification));
             build.save();
         }
 
         // schedule promotion activity.
         return scheduleBuild2(build,cause);
     }
 
     /**
      * @deprecated
      *      You need to be using {@link #scheduleBuild(AbstractBuild)}
      */
     public boolean scheduleBuild() {
         return super.scheduleBuild();
     }
 
     public boolean scheduleBuild(AbstractBuild<?,?> build) {
         return scheduleBuild(build,new LegacyCodeCause());
     }
 
     /**
      * @deprecated
      *      Use {@link #scheduleBuild2(AbstractBuild, Cause)}
      */
     public boolean scheduleBuild(AbstractBuild<?,?> build, Cause cause) {
         return scheduleBuild2(build,cause)!=null;
     }
 
     public Future<Promotion> scheduleBuild2(AbstractBuild<?,?> build, Cause cause) {
         assert build.getProject()==getOwner();
 
         // Get the parameters, if any, used in the target build and make these
         // available as part of the promotion steps
         List<ParametersAction> parameters = build.getActions(ParametersAction.class);
 
         // Create list of actions to pass to scheduled build
         List<Action> actions = new ArrayList<Action>();
         actions.addAll(parameters);
         actions.add(new PromotionTargetAction(build));
 
         // remember what build we are promoting
         return super.scheduleBuild2(0, cause, actions.toArray(new Action[actions.size()]));
     }
 
     public boolean isInQueue(AbstractBuild<?,?> build) {
         for (Item item : Hudson.getInstance().getQueue().getItems(this))
             if (item.getAction(PromotionTargetAction.class).resolve(this)==build)
                 return true;
         return false;
     }
 
 //
 // these are dummy implementations to implement abstract methods.
 // need to think about what the implications are.
 //
     public boolean isFingerprintConfigured() {
         throw new UnsupportedOperationException();
     }
 
     protected void buildDependencyGraph(DependencyGraph graph) {
         throw new UnsupportedOperationException();
     }
 
     public static List<Descriptor<? extends BuildStep>> getAll() {
         List<Descriptor<? extends BuildStep>> list = new ArrayList<Descriptor<? extends BuildStep>>();
         addTo(Builder.all(), list);
         addTo(Publisher.all(), list);
         return list;
     }
 
     private static void addTo(List<? extends Descriptor<? extends BuildStep>> source, List<Descriptor<? extends BuildStep>> list) {
         for (Descriptor<? extends BuildStep> d : source) {
             if (d instanceof BuildStepDescriptor) {
                 BuildStepDescriptor bsd = (BuildStepDescriptor) d;
                 if(bsd.isApplicable(PromotionProcess.class))
                     list.add(d);
             }
         }
     }
 
     public Permalink asPermalink() {
         return new Permalink() {
             @Override
             public String getDisplayName() {
                 return Messages.PromotionProcess_PermalinkDisplayName(PromotionProcess.this.getDisplayName());
             }
 
             @Override
             public String getId() {
                 return PromotionProcess.this.getName();
             }
 
             @Override
             public Run<?, ?> resolve(Job<?, ?> job) {
                 String id = getId();
                 for( Run<?,?> build : job.getBuilds() ) {
                     PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
                     if(a!=null && a.contains(id))
                         return build;
                 }
                 return null;
             }
         };
     }
 
     public DescriptorImpl getDescriptor() {
         return (DescriptorImpl)Jenkins.getInstance().getDescriptorOrDie(getClass());
     }
 
     /**
      * We don't directly bind {@link PromotionProcess} to the UI,
      * so instead claim that our URL is the same as our parent.
      */
     @Override
     public String getShortUrl() {
         return "";
     }
 
    public boolean isActive() {
        return !isDisabled();
    }

     @Extension
     public static class DescriptorImpl extends Descriptor<PromotionProcess> {
         @Override
         public String getDisplayName() {
             return "Promotion Process";
         }
 
         public FormValidation doCheckLabelString(@QueryParameter String value) {
             if (Util.fixEmpty(value)==null)
                 return FormValidation.ok(); // nothing typed yet
             try {
                 Label.parseExpression(value);
             } catch (ANTLRException e) {
                 return FormValidation.error(e,
                         Messages.JobPropertyImpl_LabelString_InvalidBooleanExpression(e.getMessage()));
             }
             // TODO: if there's an atom in the expression that is empty, report it
             if (Hudson.getInstance().getLabel(value).isEmpty())
                 return FormValidation.warning(Messages.JobPropertyImpl_LabelString_NoMatch());
             return FormValidation.ok();
         }
 
         public AutoCompletionCandidates doAutoCompleteAssignedLabelString(@QueryParameter String value) {
             AutoCompletionCandidates c = new AutoCompletionCandidates();
             Set<Label> labels = Hudson.getInstance().getLabels();
             List<String> queries = new AutoCompleteSeeder(value).getSeeds();
 
             for (String term : queries) {
                 for (Label l : labels) {
                     if (l.getName().startsWith(term)) {
                         c.add(l.getName());
                     }
                 }
             }
             return c;
         }
 
         /**
          * Utility class for taking the current input value and computing a list
          * of potential terms to match against the list of defined labels.
          */
         static class AutoCompleteSeeder {
 
             private String source;
             private Pattern quoteMatcher = Pattern.compile("(\\\"?)(.+?)(\\\"?+)(\\s*)");
 
             AutoCompleteSeeder(String source) {
                 this.source = source;
             }
 
             List<String> getSeeds() {
                 ArrayList<String> terms = new ArrayList();
                 boolean trailingQuote = source.endsWith("\"");
                 boolean leadingQuote = source.startsWith("\"");
                 boolean trailingSpace = source.endsWith(" ");
 
                 if (trailingQuote || (trailingSpace && !leadingQuote)) {
                     terms.add("");
                 } else {
                     if (leadingQuote) {
                         int quote = source.lastIndexOf('"');
                         if (quote == 0) {
                             terms.add(source.substring(1));
                         } else {
                             terms.add("");
                         }
                     } else {
                         int space = source.lastIndexOf(' ');
                         if (space > -1) {
                             terms.add(source.substring(space + 1));
                         } else {
                             terms.add(source);
                         }
                     }
                 }
 
                 return terms;
             }
         }
 
         // exposed for Jelly
         public List<PromotionConditionDescriptor> getApplicableConditions(AbstractProject<?,?> p) {
             return p==null ? PromotionCondition.all() : PromotionCondition.getApplicableTriggers(p);
         }
 
         public List<PromotionConditionDescriptor> getApplicableConditions(Object context) {
             return PromotionCondition.all();
         }
 
         // exposed for Jelly
         public List<Descriptor<? extends BuildStep>> getApplicableBuildSteps() {
             return PromotionProcess.getAll();
         }
 
         // exposed for Jelly
         public final Class<PromotionProcess> promotionProcessType = PromotionProcess.class;
 
         public FormValidation doCheckName(@QueryParameter String name) {
             name = Util.fixEmptyAndTrim(name);
             if (name == null) {
                 return FormValidation.error(Messages.JobPropertyImpl_ValidateRequired());
             }
 
             try {
                 Hudson.checkGoodName(name);
             } catch (Failure f) {
                 return FormValidation.error(f.getMessage());
             }
 
             return FormValidation.ok();
         }
     }
 
     private static final Logger LOGGER = Logger.getLogger(PromotionProcess.class.getName());
 
 }
