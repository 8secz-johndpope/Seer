 package net.sf.eclipsefp.haskell.core.partitioned.alex;
 
 import net.sf.eclipsefp.haskell.partitioned.AlexRunner;
 import net.sf.eclipsefp.haskell.partitioned.ProcessorError;
 import net.sf.eclipsefp.haskell.util.FileUtil;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IResourceVisitor;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IPath;
 
 
 public class FullBuildVisitor implements IResourceVisitor {
 
   public boolean visit( final IResource resource ) throws CoreException {
     if( AlexBuilder.mustBeVisited( resource ) ) {
       AlexRunner runner = new AlexRunner();
       for( ProcessorError s: runner.run( resource.getLocation() ) ) {
         AlexBuilder.createMarker( resource, s );
       }
       // Set derived file as derived
       IPath derivedPath = resource.getProjectRelativePath().removeFileExtension().addFileExtension( FileUtil.EXTENSION_HS );
       resource.getProject().getFile( derivedPath ).setDerived( true, null );
     }
     return true;
   }
 
 }
