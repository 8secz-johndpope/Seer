 package org.overture.ide.ui.editor.core;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.jface.text.IDocument;
 import org.eclipse.jface.text.IRegion;
 import org.eclipse.jface.text.reconciler.DirtyRegion;
 import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
 import org.overture.ide.core.VdmCore;
 
 import org.overture.ide.core.parser.ISourceParser;
 import org.overture.ide.core.parser.SourceParserManager;
 import org.overture.ide.core.resources.IVdmProject;
 
 
 public class VdmReconcilingStrategy implements IReconcilingStrategy
 {
 
 	private VdmDocument currentDocument;
 
 	// private ContentOutline outline =null;
 
 	public VdmReconcilingStrategy()
 	{
 		// IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench()
 		// .getActiveWorkbenchWindow();
 
 		// if (activeWorkbenchWindow != null)
 		// {
 		// IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
 		// if (activePage != null)
 		// {
 		// IViewPart outlineCandidate =
 		// activePage.findView(IPageLayout.ID_OUTLINE);
 		// if (outlineCandidate instanceof ContentOutline)
 		// {
 		// outline = (ContentOutline) outlineCandidate;
 		// }
 		// }
 		// }
 	}
 
 	public void reconcile(IRegion partition)
 	{
 		if (VdmCore.DEBUG)
 		{
 			// System.out.println("reconcile(IRegion partition)");
 			// System.out.println("File: "
 			// + (currentDocument).getFile().toString());
 			// if(outline != null)
 			// {
 			// VdmContentOutlinePage page = (VdmContentOutlinePage)
 			// outline.getCurrentPage();
 			//				
 			// }
 		}
 		try
 		{
 
 			IVdmProject vdmProject = (IVdmProject) currentDocument.getProject()
 					.getAdapter(IVdmProject.class);
 
 			if (currentDocument.getSourceUnit() != null && vdmProject != null)
 			{
 
 				ISourceParser parser = SourceParserManager.getInstance()
 						.getSourceParser(vdmProject);
 
 				if (parser != null)
 				{
 					parser.parse(currentDocument.getSourceUnit(),
 							currentDocument.get());
 				}
				//Setting type checked to false after some alteration
				vdmProject.getModel().setChecked(false);
 			}
 
 		} catch (CoreException e)
 		{
 			if (VdmCore.DEBUG)
 				e.printStackTrace();
 		}
 	}
 
 	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion)
 	{
 
 	}
 
 	public void setDocument(IDocument document)
 	{
 		if (document instanceof VdmDocument)
 			currentDocument = (VdmDocument) document;
 	}
 
 }
