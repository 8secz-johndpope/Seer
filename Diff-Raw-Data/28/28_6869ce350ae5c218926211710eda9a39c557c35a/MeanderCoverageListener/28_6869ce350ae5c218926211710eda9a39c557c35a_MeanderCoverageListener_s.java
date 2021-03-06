 package org.codemap.plugin.eclemma;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IResourceVisitor;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.jdt.core.ICompilationUnit;
 import org.eclipse.jdt.core.IJavaElement;
 import org.eclipse.jdt.core.IJavaProject;
 import org.eclipse.jdt.core.IPackageFragmentRoot;
 import org.eclipse.jdt.core.JavaCore;
 import org.eclipse.jdt.core.JavaModelException;
 
 import ch.akuhn.util.Pair;
 import ch.deif.meander.Colors;
 import ch.deif.meander.Location;
 import ch.deif.meander.viz.MapVisualization;
 import ch.unibe.softwaremap.ProjectMap;
 import ch.unibe.softwaremap.SoftwareMap;
 
 import com.mountainminds.eclemma.core.CoverageTools;
 import com.mountainminds.eclemma.core.analysis.IJavaCoverageListener;
 import com.mountainminds.eclemma.core.analysis.IJavaElementCoverage;
 import com.mountainminds.eclemma.core.analysis.IJavaModelCoverage;
 
 public class MeanderCoverageListener implements IJavaCoverageListener {
 
 	protected final class CoverageResourceVisitor implements IResourceVisitor {
 		private final List<Pair<String, Double>> identifiers;
 
 		protected CoverageResourceVisitor(List<Pair<String, Double>> identifiers) {
 			this.identifiers = identifiers;
 		}
 
 		@Override
 		public boolean visit(IResource resource) throws CoreException {
 			IJavaElement javaElement = JavaCore.create(resource);
 			if (javaElement == null) return true;
 			if (javaElement.getElementType() != IJavaElement.COMPILATION_UNIT) return true;
 			ICompilationUnit compilationUnit = (ICompilationUnit) javaElement.getAdapter(ICompilationUnit.class);
 			return visit(compilationUnit);
 		}
 
 		private boolean visit(ICompilationUnit compilationUnit) {
 			String identifier = compilationUnit.getHandleIdentifier();
 			IJavaElementCoverage coverageInfo = CoverageTools.getCoverageInfo(compilationUnit);
 			if (coverageInfo == null) {
 				// for interfaces we do not have coverage information
 				return false;
 			}
 			identifiers.add(new Pair<String, Double>(identifier, coverageInfo.getLineCounter().getRatio()));
 //			System.out.println(identifier + " " + coverageInfo.getLineCounter().getRatio());
 			return false;
 		}
 	}
 
 	@Override
 	public void coverageChanged() {
 		IJavaModelCoverage coverage = CoverageTools.getJavaModelCoverage();
 		List<IJavaProject> projects = Arrays.asList(coverage.getInstrumentedProjects());
 		boolean isEmptyCoverage = true;
 		for(IJavaProject each: projects) {
 			if (! each.isOpen()) continue;
 			isEmptyCoverage = false;
 //			System.out.println("coverage changed for: " + each.getHandleIdentifier());
 //			IJavaElementCoverage coverageInfo = CoverageTools.getCoverageInfo(each);
 //			System.out.println(coverageInfo.getMethodCounter().getRatio());
 			
 			List<Pair<String, Double>> coverageInfo = compilationUnitCoverage(each);
			CoverageMapModifier coverageMod = new CoverageMapModifier(coverageInfo, EclemmaOverlay.showCoverageAction);
 			ProjectMap mapForProject = SoftwareMap.core().mapForProject(each.getProject());
 			coverageMod.addTo(mapForProject);
 		}
 		if(! isEmptyCoverage) {
 			SoftwareMap.core().updateMap();
 		}
 			
 	}
 
 	private List<Pair<String, Double>> compilationUnitCoverage(IJavaProject project) {
 		List<Pair<String, Double>> identifiers = new ArrayList<Pair<String,Double>>();
 		try {
 			for(IPackageFragmentRoot root: project.getPackageFragmentRoots()) {
 				IResource resource = root.getResource();
 				if (resource == null) continue;
 				resource.accept(new CoverageResourceVisitor(identifiers));
 			}
 		} catch (JavaModelException e) {
 			e.printStackTrace();
 		} catch (CoreException e) {
 			e.printStackTrace();
 		}
 		return identifiers;
 	}
 }
