 package bndtools.internal.ui;
 
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Map;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.IPackageFragment;
 import org.eclipse.jdt.core.IPackageFragmentRoot;
 import org.eclipse.jdt.core.JavaModelException;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.viewers.IDecoration;
 import org.eclipse.jface.viewers.ILightweightLabelDecorator;
 import org.eclipse.jface.viewers.LabelProvider;
 import org.eclipse.ui.plugin.AbstractUIPlugin;
 import org.osgi.framework.Version;
 
 import bndtools.Central;
 import bndtools.Plugin;
 import bndtools.internal.decorator.ExportedPackageDecoratorJob;
 
 /**
  * A decorator for {@link IPackageFragment}s that adds an icon if the package is exported by the bundle manifest.
  * 
  * @author duckAsteroid
  */
 public class PackageDecorator extends LabelProvider implements ILightweightLabelDecorator {
 
    private final ImageDescriptor exportedIcon = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/plus-decorator.png");
    private final ImageDescriptor excludedIcon = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/excluded_ovr.gif");
 
     public void decorate(Object element, IDecoration decoration) {
         if (element instanceof IPackageFragment) {
 
             IPackageFragment pkg = (IPackageFragment) element;
             String pkgName = pkg.getElementName();
             boolean isSourcePkg = isSourcePackage(pkg);
 
             IJavaProject javaProject = pkg.getJavaProject();
             IProject project = javaProject.getProject();
 
             Map<String, ? extends Collection<Version>> exports = Central.getExportedPackageModel(project);
             Collection<String> contained = Central.getContainedPackageModel(project);
             if (exports != null) {
                 Collection<Version> versions = exports.get(pkgName);
                 if (versions != null) {
                    decoration.addOverlay(exportedIcon);
                     if (versions.isEmpty())
                         decoration.addSuffix(" " + Collections.singletonList(Version.emptyVersion).toString());
                     else
                         decoration.addSuffix(" " + versions.toString());
                 }
 
                 if (isSourcePkg && (contained == null || !contained.contains(pkgName))) {
                    decoration.addOverlay(excludedIcon);
                     decoration.addSuffix(" <excluded>");
                 }
             } else {
                 ExportedPackageDecoratorJob.scheduleForProject(project);
             }
         }
     }
 
     private boolean isSourcePackage(IPackageFragment pkg) {
         try {
             return pkg.getKind() == IPackageFragmentRoot.K_SOURCE;
         } catch (JavaModelException e) {
             return false;
         }
     }
 }
