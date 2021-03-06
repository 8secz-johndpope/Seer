 /****************************************************************************
  * Copyright (c) 2008-2014 Matthew Ballance and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Matthew Ballance - initial implementation
  ****************************************************************************/
 
 
 package net.sf.sveditor.ui.wizards.templates;
 
 import java.io.File;
 import java.io.InputStream;
 import java.lang.reflect.InvocationTargetException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import net.sf.sveditor.core.SVFileUtils;
 import net.sf.sveditor.svt.core.SvtCorePlugin;
 import net.sf.sveditor.svt.core.templates.DefaultTemplateParameterProvider;
 import net.sf.sveditor.svt.core.templates.DynamicTemplateParameterProvider;
 import net.sf.sveditor.svt.core.templates.ITemplateFileCreator;
 import net.sf.sveditor.svt.core.templates.TemplateProcessor;
 import net.sf.sveditor.svt.core.text.TagProcessor;
 import net.sf.sveditor.svt.ui.SvtUiPlugin;
 import net.sf.sveditor.ui.SVUiPlugin;
 
 import org.eclipse.core.resources.IContainer;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.jface.operation.IRunnableWithProgress;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.wizard.IWizardPage;
 import org.eclipse.ui.IWorkbench;
 import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
 
 public class SVTemplateWizard extends BasicNewResourceWizard {
 	public static final String							ID = SVUiPlugin.PLUGIN_ID + ".svMethodologyClass";
 	private SVTemplateSelectionPage						fTemplateSelectionPage;
 	private SVNameFilesPage								fNameFilesPage;
 	private SVTemplateParametersPage2					fParametersPage;
 //	private SVTemplateParameterPage						fParamsPage;
 	private Map<String, Object>							fOptions;
 	
 
 	public SVTemplateWizard() {
 		super();
 		fOptions = new HashMap<String, Object>();
 	}
 	
 	public void addPages() {
 		super.addPages();
 		
 		fTemplateSelectionPage = new SVTemplateSelectionPage();
 		fNameFilesPage = new SVNameFilesPage();
 		fParametersPage = new SVTemplateParametersPage2();
 		
 		Object sel = getSelection().getFirstElement();
		IResource r = null;
		
		if (sel != null) {
			if (sel instanceof IResource) {
				r = (IResource)sel;
			} else if (sel instanceof IAdaptable) {
				r = (IResource)((IAdaptable)sel).getAdapter(IResource.class);
			}
		}
		
		if (r != null) {
 			if (!(r instanceof IContainer)) {
 				r = r.getParent();
 			}
			
 			fNameFilesPage.setSourceFolder(r.getFullPath().toOSString());
 		}
			
 		addPage(fTemplateSelectionPage);
 		addPage(fNameFilesPage);
 		addPage(fParametersPage);
 	}
 	
 	@Override
 	public boolean canFinish() {
 		return super.canFinish();
 	}
 	
 	@Override
 	public IWizardPage getNextPage(IWizardPage page) {
 		IWizardPage next;
 		
 		next = super.getNextPage(page);
 	
 		if (next == fNameFilesPage) {
 			fNameFilesPage.setTemplate(fTemplateSelectionPage.getTemplate());
 		}
 		
 		if (next == fParametersPage) {
 			String src_folder_s = fNameFilesPage.getSourceFolder();
 			IProject src_project = null;
 			
 			if (src_folder_s != null) {
 				IContainer src_folder = SVFileUtils.getWorkspaceFolder(src_folder_s);
 				if (src_folder != null && src_folder.exists()) {
 					src_project = src_folder.getProject();
 				}
 			}
 			fParametersPage.setProject(src_project);
 			fParametersPage.setParameters(fTemplateSelectionPage.getTemplate().getParameters());
 			fParametersPage.setTemplateName(fTemplateSelectionPage.getTemplate().getName());
 		}
 		
 		
 		return next;
 	}
 	
 	@Override
 	public IWizardPage getPreviousPage(IWizardPage page) {
 		return super.getPreviousPage(page);
 	}
 	
 	public void init(IWorkbench workbench, IStructuredSelection selection) {
 		super.init(workbench, selection);
 		setNeedsProgressMonitor(true);
 		
 		SvtCorePlugin.getDefault().getTemplateRgy().load_extensions();
 	}
 
 	@Override
 	public boolean performFinish() {
 		final IContainer folder = SVFileUtils.getWorkspaceFolder(fNameFilesPage.getSourceFolder());
 		final TagProcessor tp = new TagProcessor();
 		tp.addParameterProvider(new DynamicTemplateParameterProvider());
 //		tp.addParameterProvider(fParamsPage.getTagProcessor(false));
 		// TODO:
 		tp.addParameterProvider(fNameFilesPage.getTagProcessor(false));
 		tp.addParameterProvider(fParametersPage.getTagProcessor());
 //		tp.addParameterProvider(fParametersPage.getTagProcessor(false));
 		tp.addParameterProvider(new DefaultTemplateParameterProvider(
 				SvtUiPlugin.getDefault().getGlobalTemplateParameters()));
 		// Add the global parameters, to allow users to 'bend' the rules a bit
 		// on referencing parameters that are not declared in the template
 		// manifest
 		tp.addParameterProvider(SvtUiPlugin.getDefault().getGlobalTemplateParameters());
 		
 
 		try {
 			getContainer().run(true, true, new IRunnableWithProgress() {
 
 				public void run(final IProgressMonitor monitor) 
 						throws InvocationTargetException, InterruptedException {
 					// TODO:
 					monitor.beginTask("Creating Files", 5 /*fParamsPage.getFileNames().size()*/);
 					TemplateProcessor templ_proc = new TemplateProcessor(new ITemplateFileCreator() {
 
 						public void createFile(String path, InputStream content, boolean executable) {
 							IFile file = folder.getFile(new Path(path));
 
 							monitor.worked(1);
 							try {
 								if (!file.getParent().exists()) {
 									// Create the folder path up to the file
 									List<IContainer> mk_list = new ArrayList<IContainer>();
 									IContainer c = file.getParent();
 									while (!c.exists()) {
 										mk_list.add(0, c);
 										c = c.getParent();
 									}
 									
 									for (IContainer cc : mk_list) {
 										((IFolder)cc).create(
 											true, true, new NullProgressMonitor());
 									}
 								}
 								if (file.exists()) {
 									file.setContents(content, true, true, new NullProgressMonitor());
 								} else {
 									file.create(content, true, new NullProgressMonitor());
 								}
 							} catch (CoreException e) {
 								e.printStackTrace();
 							}
 						
 							if (executable) {
 								File file_f = file.getLocation().toFile();
 								file_f.setExecutable(true);
 							}
 						}
 					});
 					templ_proc.process(fTemplateSelectionPage.getTemplate(), tp);
 					monitor.done();
 				}
 			});
 		} catch (InterruptedException e) {}
 		catch (InvocationTargetException e) {}
 		
 		return true;
 	}
 }
