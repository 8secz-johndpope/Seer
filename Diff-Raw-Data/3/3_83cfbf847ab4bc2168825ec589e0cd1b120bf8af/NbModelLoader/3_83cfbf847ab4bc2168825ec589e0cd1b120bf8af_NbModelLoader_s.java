 package org.netbeans.gradle.project.model;
 
import org.netbeans.gradle.project.model.issue.ModelLoadIssue;
 import java.io.IOException;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.List;
 import org.gradle.tooling.ProjectConnection;
 import org.netbeans.api.progress.ProgressHandle;
 import org.netbeans.gradle.model.util.CollectionUtils;
 import org.netbeans.gradle.project.NbGradleProject;
 
 public interface NbModelLoader {
     public static final class Result {
         private final NbGradleModel mainModel;
         private final List<NbGradleModel> otherModels;
         private final List<ModelLoadIssue> issues;
 
         public Result(NbGradleModel mainModel, Collection<NbGradleModel> otherModels) {
             this(mainModel, otherModels, Collections.<ModelLoadIssue>emptyList());
         }
 
         public Result(
                 NbGradleModel mainModel,
                 Collection<NbGradleModel> otherModels,
                 Collection<? extends ModelLoadIssue> issues) {
             if (mainModel == null) throw new NullPointerException("mainModel");
             if (otherModels == null) throw new NullPointerException("otherModels");
             if (issues == null) throw new NullPointerException("issues");
 
             this.mainModel = mainModel;
             this.otherModels = CollectionUtils.copyNullSafeList(otherModels);
             this.issues = CollectionUtils.copyNullSafeList(issues);
         }
 
         public NbGradleModel getMainModel() {
             return mainModel;
         }
 
         public List<NbGradleModel> getOtherModels() {
             return otherModels;
         }
 
         public List<ModelLoadIssue> getIssues() {
             return issues;
         }
     }
 
     public Result loadModels(NbGradleProject project, ProjectConnection connection, ProgressHandle progress) throws IOException;
 }
