 package com.byluroid.eclipse.maven.android;
 
 import java.io.File;
 import java.util.List;
 
 import org.apache.maven.model.Plugin;
 import org.apache.maven.plugin.MojoExecution;
 import org.apache.maven.project.MavenProject;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.jdt.core.IClasspathEntry;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.JavaCore;
 import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
 import org.maven.ide.eclipse.jdt.IJavaProjectConfigurator;
 import org.maven.ide.eclipse.project.IMavenProjectFacade;
 import org.maven.ide.eclipse.project.configurator.AbstractBuildParticipant;
 import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
 import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;
 
 import com.android.ide.eclipse.adt.AndroidConstants;
 
 public class AndroidDevelopmentToolsProjectConfigurator extends AbstractProjectConfigurator implements IJavaProjectConfigurator {
 
 	static final String ANDROID_GEN_PATH = "gen";
 	static final String ANDROID_PLUGIN_GROUP_ID = "com.jayway.maven.plugins.android.generation2";
 	static final String ANDROID_PLUGIN_ARTIFACT_ID = "maven-android-plugin";
 
 	@Override
 	@SuppressWarnings("restriction")
 	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		if (getAndroidPlugin(request.getMavenProject()) != null) {
 			IProject project = request.getProject();
 			if (!project.hasNature(AndroidConstants.NATURE)) {
 				addNature(project, AndroidConstants.NATURE, monitor);
 			}
 		}
 	}
 
 	public void configureClasspath(IMavenProjectFacade facade,  IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
 		if(getAndroidPlugin(facade.getMavenProject()) != null) {
 			IJavaProject javaProject = JavaCore.create(facade.getProject());
 			// set output location to target/android-classes so APK blob is not including in APK resources
 			javaProject.setOutputLocation(javaProject.getPath().append("target").append("android-classes"), monitor);
 		}
 	}
 
 	@SuppressWarnings("restriction")
 	public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath, IProgressMonitor monitor) throws CoreException {
		if (getAndroidPlugin(request.getMavenProject()) != null) {
 			IJavaProject javaProject = JavaCore.create(request.getProject());
 
 			// add gen source folder if it does not already exist
 			if (AndroidMavenPluginUtil.getGenSourceEntry(classpath.getEntries()) == null) {
 				IPath path = javaProject.getPath().append(ANDROID_GEN_PATH);
 				final File genFolder = path.toFile();
 				if(!genFolder.exists()) {
 					genFolder.mkdirs();
 					request.getProject().refreshLocal(IProject.DEPTH_INFINITE, monitor);
 				}
 
 				classpath.addSourceEntry(path, javaProject.getOutputLocation(), true);
 			}
 		}
 	}
 
 	@Override
     public AbstractBuildParticipant getBuildParticipant(MojoExecution execution) {
 		if(execution.getGoal().equals("compile")) {
 			return new AndroidMavenBuildParticipant(execution);
 		}
 	    return super.getBuildParticipant(execution);
     }
 
 	private Plugin getAndroidPlugin(MavenProject mavenProject) {
 		List<Plugin> plugins = mavenProject.getBuildPlugins();
 
 		for (Plugin plugin : plugins) {
 			if (ANDROID_PLUGIN_GROUP_ID.equals(plugin.getGroupId()) && ANDROID_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
 				return plugin;
 			}
 		}
 
 		return null;
 	}
 
 }
