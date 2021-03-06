 package hudson.maven.reporters;
 
 import hudson.maven.MavenBuildProxy;
 import hudson.maven.MavenModule;
 import hudson.maven.MavenReporter;
 import hudson.maven.MavenReporterDescriptor;
 import hudson.maven.MojoInfo;
 import hudson.maven.MavenBuild;
 import hudson.model.BuildListener;
 import hudson.util.InvocationInterceptor;
 import hudson.FilePath;
 import org.apache.maven.artifact.Artifact;
 import org.apache.maven.artifact.installer.ArtifactInstallationException;
 import org.apache.maven.project.MavenProject;
 
 import java.io.IOException;
 import java.io.File;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Set;
 import java.util.HashSet;
 import java.lang.reflect.Proxy;
 import java.lang.reflect.Method;
 import java.lang.reflect.InvocationHandler;
 
 /**
  * Archives artifacts of the build.
  *
  * <p>
  * Archive will be created in two places. One is inside the build directory,
  * to be served from Hudson. The other is to the local repository of the master,
  * so that artifacts can be shared in maven builds happening in other slaves.
  *
  * @author Kohsuke Kawaguchi
  */
 public class MavenArtifactArchiver extends MavenReporter {
     /**
      * Accumulates {@link File}s that are created from assembly plugins.
      * Note that some of them might be attached.
      */
     private transient List<File> assemblies;
 
     @Override
     public boolean preBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
 //        System.out.println("Zeroing out at "+MavenArtifactArchiver.this);
         assemblies = new ArrayList<File>();
         return true;
     }
 
     @Override
     public boolean preExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener) throws InterruptedException, IOException {
         if(mojo.is("org.apache.maven.plugins","maven-assembly-plugin","assembly")) {
             try {
                 // watch out for AssemblyArchiver.createArchive that returns a File object, pointing to the archives created by the assembly plugin.
                 mojo.intercept("assemblyArchiver",new InvocationInterceptor() {
                     public Object invoke(Object proxy, Method method, Object[] args, InvocationHandler delegate) throws Throwable {
                         Object ret = delegate.invoke(proxy, method, args);
                         if(method.getName().equals("createArchive") && method.getReturnType()==File.class) {
 //                            System.out.println("Discovered "+ret+" at "+MavenArtifactArchiver.this);
                            assemblies.add((File)ret);
                         }
                         return ret;
                     }
                 });
             } catch (NoSuchFieldException e) {
                 listener.getLogger().println("[HUDSON] Failed to monitor the execution of the assembly plugin: "+e.getMessage());
             }
         }
         return true;
     }
 
     public boolean postBuild(MavenBuildProxy build, MavenProject pom, final BuildListener listener) throws InterruptedException, IOException {
         // artifacts that are known to Maven.
         Set<File> mavenArtifacts = new HashSet<File>();
 
         if(pom.getFile()!=null) {// goals like 'clean' runs without loading POM, apparently.
             // record POM
             final MavenArtifact pomArtifact = new MavenArtifact(
                 pom.getGroupId(), pom.getArtifactId(), pom.getVersion(), null, "pom", pom.getFile().getName());
             mavenArtifacts.add(pom.getFile());
             pomArtifact.archive(build,pom.getFile(),listener);
 
             // record main artifact (if packaging is POM, this doesn't exist)
             final MavenArtifact mainArtifact = MavenArtifact.create(pom.getArtifact());
             if(mainArtifact!=null) {
                 File f = pom.getArtifact().getFile();
                 mavenArtifacts.add(f);
                 mainArtifact.archive(build, f,listener);
             }
 
             // record attached artifacts
             final List<MavenArtifact> attachedArtifacts = new ArrayList<MavenArtifact>();
             for( Artifact a : (List<Artifact>)pom.getAttachedArtifacts() ) {
                 MavenArtifact ma = MavenArtifact.create(a);
                 if(ma!=null) {
                     mavenArtifacts.add(a.getFile());
                     ma.archive(build,a.getFile(),listener);
                     attachedArtifacts.add(ma);
                 }
             }
 
             // record the action
             build.executeAsync(new MavenBuildProxy.BuildCallable<Void,IOException>() {
                 public Void call(MavenBuild build) throws IOException, InterruptedException {
                     MavenArtifactRecord mar = new MavenArtifactRecord(build,pomArtifact,mainArtifact,attachedArtifacts);
                     build.addAction(mar);
 
                     mar.recordFingerprints();
 
                     return null;
                 }
             });
         }
 
         // do we have any assembly artifacts?
 //        System.out.println("Considering "+assemblies+" at "+MavenArtifactArchiver.this);
 //        new Exception().fillInStackTrace().printStackTrace();
         for (File assembly : assemblies) {
             if(mavenArtifacts.contains(assembly))
                 continue;   // looks like this is already archived
             FilePath target = build.getArtifactsDir().child(assembly.getName());
             listener.getLogger().println("[HUDSON] Archiving "+ assembly+" to "+target);
             new FilePath(assembly).copyTo(target);
             // TODO: fingerprint
         }
 
         return true;
     }
 
     public DescriptorImpl getDescriptor() {
         return DescriptorImpl.DESCRIPTOR;
     }
 
     public static final class DescriptorImpl extends MavenReporterDescriptor {
         public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
 
         private DescriptorImpl() {
             super(MavenArtifactArchiver.class);
         }
 
         public String getDisplayName() {
             return Messages.MavenArtifactArchiver_DisplayName();
         }
 
         public MavenReporter newAutoInstance(MavenModule module) {
             return new MavenArtifactArchiver();
         }
     }
 
     private static final long serialVersionUID = 1L;
 }
