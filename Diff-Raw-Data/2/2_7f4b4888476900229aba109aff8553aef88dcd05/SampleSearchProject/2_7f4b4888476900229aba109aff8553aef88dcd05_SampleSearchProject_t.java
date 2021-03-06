 package mmrnmhrm.core.search;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.dltk.core.IScriptProject;
 
 import melnorme.utilbox.core.ExceptionAdapter;
 import mmrnmhrm.tests.BaseDeeTest;
 import mmrnmhrm.tests.DeeCoreTestResources;
 
 public class SampleSearchProject extends DeeCoreTestResources {
 	
	public static final String SAMPLEPROJNAME = "sampleSearchProject";
 	
 	public static final SampleSearchProject defaultInstance; 
 	
 	static {
 		try {
 			defaultInstance = new SampleSearchProject();
 		} catch (Exception e) {
 			throw ExceptionAdapter.unchecked(e);
 		}
 	}
 	
 	
 	public final IScriptProject scriptProject;
 	
 	public SampleSearchProject() throws CoreException {
 		scriptProject = BaseDeeTest.createAndOpenDeeProject(SAMPLEPROJNAME);
 		fillSampleProj();
 	}
 	
 	protected void fillSampleProj() throws CoreException {
 		IProject project = scriptProject.getProject();
 		createSrcFolderFromDeeCoreResource(SAMPLEPROJNAME + "/srcA", project.getFolder("srcA"));
 		createSrcFolderFromDeeCoreResource(SAMPLEPROJNAME + "/srcB", project.getFolder("srcB"));
 	}
 	
 }
