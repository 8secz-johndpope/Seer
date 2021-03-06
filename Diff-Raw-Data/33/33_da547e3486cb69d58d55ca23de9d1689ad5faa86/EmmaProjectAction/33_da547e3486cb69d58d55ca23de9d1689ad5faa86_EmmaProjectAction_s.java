 package hudson.plugins.emma;
 
 import hudson.model.Action;
import hudson.model.Build;
import hudson.model.Project;
 import hudson.model.Result;
 import org.kohsuke.stapler.StaplerRequest;
 import org.kohsuke.stapler.StaplerResponse;
 
 import java.io.IOException;
 
 /**
  * Project view extension by Emma plugin.
  * 
  * @author Kohsuke Kawaguchi
  */
 public final class EmmaProjectAction implements Action {
    public final Project<?,?> project;
 
    public EmmaProjectAction(Project project) {
         this.project = project;
     }
 
     public String getIconFileName() {
         return "graph.gif";
     }
 
     public String getDisplayName() {
         return "Coverage Trend";
     }
 
     public String getUrlName() {
         return "emma";
     }
 
     /**
      * Gets the most recent {@link EmmaBuildAction} object.
      */
     public EmmaBuildAction getLastResult() {
        for( Build<?,?> b = project.getLastBuild(); b!=null; b=b.getPreviousBuild()) {
             if(b.getResult()== Result.FAILURE)
                 continue;
             EmmaBuildAction r = b.getAction(EmmaBuildAction.class);
             if(r!=null)
                 return r;
         }
         return null;
     }
 
     public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (getLastResult() != null)
           getLastResult().doGraph(req,rsp);
     }
 }
