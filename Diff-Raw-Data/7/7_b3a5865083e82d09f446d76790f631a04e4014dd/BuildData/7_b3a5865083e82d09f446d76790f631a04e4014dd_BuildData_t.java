 package hudson.plugins.git.util;
 
 import hudson.Util;
 import hudson.model.AbstractBuild;
 import hudson.model.Action;
 import hudson.model.Api;
 import hudson.plugins.git.Branch;
 import hudson.plugins.git.Revision;
 
 import java.io.Serializable;
 import java.util.HashMap;
 import java.util.IdentityHashMap;
 import java.util.Map;
 
 import org.kohsuke.stapler.export.Exported;
 import org.kohsuke.stapler.export.ExportedBean;
 import org.spearce.jgit.lib.ObjectId;
 
 import static hudson.Util.fixNull;
 
 /**
  * Captures the Git related information for a build.
  *
  * <p>
  * This object is added to {@link AbstractBuild#getActions()} and
  * remember the Git related information of that build.
  */
 @ExportedBean(defaultVisibility = 999)
 public class BuildData implements Action, Serializable, Cloneable {
     private static final long serialVersionUID = 1L;
 
     /**
      * Map of branch name -> build (Branch name to last built SHA1).
      */
     public Map<String, Build> buildsByBranchName = new HashMap<String, Build>();
 
     /**
      * The last build that we did.
      */
     public Build              lastBuild;
 
 
     public String getDisplayName() {
         return "Git Build Data";
     }
     public String getIconFileName() {
         return "/plugin/git/icons/git-32x32.png";
     }
     public String getUrlName() {
         return "git";
     }
 
     public Object readResolve() {
         Map<String,Build> newBuildsByBranchName = new HashMap<String,Build>();
         
         for (Map.Entry<String, Build> buildByBranchName : buildsByBranchName.entrySet()) {
             String branchName = fixNull(buildByBranchName.getKey());
             Build build = buildByBranchName.getValue();
             newBuildsByBranchName.put(branchName, build);
         }
 
         this.buildsByBranchName = newBuildsByBranchName;
 
         return this;
     }
     
     /**
      * Return true if the history shows this SHA1 has been built.
      * False otherwise.
      * @param sha1
      * @return
      */
     public boolean hasBeenBuilt(ObjectId sha1) {
     	try {
             for(Build b : buildsByBranchName.values()) {
                 if(b.revision.getSha1().equals(sha1))
                     return true;
             }
 
             return false;
     	}
     	catch(Exception ex) {
             return false;
     	}
     }
 
     public void saveBuild(Build build) {
     	lastBuild = build;
     	for(Branch branch : build.revision.getBranches()) {
             buildsByBranchName.put(fixNull(branch.getName()), build);
     	}
     }
 
     public Build getLastBuildOfBranch(String branch) {
        return buildsByBranchName.get(branch);
     }
 
     @Exported
     public Revision getLastBuiltRevision() {
         return lastBuild==null?null:lastBuild.revision;
     }
 
     @Exported
     public Map<String,Build> getBuildsByBranchName() {
         return buildsByBranchName;
     }
 
     @Override
     public BuildData clone() {
         BuildData clone;
         try {
             clone = (BuildData) super.clone();
         }
         catch (CloneNotSupportedException e) {
             throw new RuntimeException("Error cloning BuildData", e);
         }
 
         IdentityHashMap<Build, Build> clonedBuilds = new IdentityHashMap<Build, Build>();
 
         clone.buildsByBranchName = new HashMap<String, Build>();
         for (Map.Entry<String, Build> buildByBranchName : buildsByBranchName.entrySet()) {
             String branchName = buildByBranchName.getKey();
             if (branchName == null) {
                 branchName = "";
             }
             Build build = buildByBranchName.getValue();
             Build clonedBuild = clonedBuilds.get(build);
             if (clonedBuild == null) {
                 clonedBuild = build.clone();
                 clonedBuilds.put(build, clonedBuild);
             }
             clone.buildsByBranchName.put(branchName, clonedBuild);
         }
 
         if (lastBuild != null) {
             clone.lastBuild = clonedBuilds.get(lastBuild);
             if (clone.lastBuild == null) {
                 clone.lastBuild = lastBuild.clone();
                 clonedBuilds.put(lastBuild, clone.lastBuild);
             }
         }
 
         return clone;
     }
 
     public Api getApi() {
         return new Api(this);
     }
 }
