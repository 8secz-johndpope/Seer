 /*
  * $Id$
  *
  * Copyright (c) 2004-2005 by the TeXlapse Team.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  */
 package net.sourceforge.texlipse.builder;
 
 import net.sourceforge.texlipse.TexlipsePlugin;
 import net.sourceforge.texlipse.properties.TexlipseProperties;
 import net.sourceforge.texlipse.viewer.ViewerManager;
 
 import org.eclipse.core.resources.IContainer;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.Platform;
 import org.eclipse.core.runtime.QualifiedName;
 import org.eclipse.jface.dialogs.MessageDialogWithToggle;
 import org.eclipse.swt.widgets.Shell;
 
 
 /**
  * Build tex-file(s) using latex or pslatex or pdflatex.
  * 
  * @author Kimmo Karlsson
  * @author Tor Arne Vestbø
  * @author Boris von Loesch
  */
 public class TexBuilder extends AbstractBuilder {
 
     private ProgramRunner latex;
     private ProgramRunner bibtex;
     private ProgramRunner makeIndex;
     private ProgramRunner makeIndexNomencl;
     private String output;
     private boolean stopped;
     
     public TexBuilder(int i, String outputFormat) {
         super(i);
         output = outputFormat;
         latex = null;
         bibtex = null;
         makeIndex = null;
         isValid();
     }
     
     /**
      * Check if the needed program runners are operational.
      * Update runners from registry if necessary.
      * @return true, if this builder is ready for operation, false otherwise
      */
     public boolean isValid() {
         if (latex == null || !latex.isValid()) {
             latex = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_TEX, output);
         }
         if (bibtex == null || !bibtex.isValid()) {
             bibtex = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_BIB, TexlipseProperties.OUTPUT_FORMAT_AUX);
         }
         if (makeIndex == null || !makeIndex.isValid()) {
             makeIndex = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_IDX, TexlipseProperties.OUTPUT_FORMAT_IDX);
         }
         if (makeIndexNomencl == null || !makeIndexNomencl.isValid()) {
             makeIndexNomencl = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_NOMENCL, TexlipseProperties.OUTPUT_FORMAT_NOMENCL);
         }
          return latex != null && latex.isValid()
             && bibtex != null && bibtex.isValid()
             && makeIndex != null && makeIndex.isValid();
     }
     
     /**
      * @return output format of the latex processor
      */
     public String getOutputFormat() {
         return latex.getOutputFormat();
     }
     
     /**
      * @return sequence
      */
     public String getSequence() {
         return latex.getProgramName();
     }
     
     public void stopRunners() {
         latex.stop();
         bibtex.stop();
         makeIndex.stop();
         makeIndexNomencl.stop();
         stopped = true;
     }
     
     /**
      * Opens a messabox where the user is asked wether he wants to continue the build
      * @param project
      * @return
      * @throws CoreException
      */
     private boolean askUserForContinue(IProject project) throws CoreException{
         Object c = project.getSessionProperty(new QualifiedName(null, "AlwaysContinueBuilding"));
         if (c == null) {
             // ask the user if he wants to continue
             final Shell shell = TexlipsePlugin.getCurrentWorkbenchPage().getActiveEditor().getSite().getShell();
             final StringBuffer toggle = new StringBuffer();
             final StringBuffer returnCode = new StringBuffer();
 
             shell.getDisplay().syncExec(new Runnable() {
                public void run() {
                    final MessageDialogWithToggle mess = MessageDialogWithToggle.openOkCancelConfirm(
                            TexlipsePlugin.getCurrentWorkbenchPage().getActiveEditor().getSite().getShell(), 
                            TexlipsePlugin.getResourceString("builderErrorDuringBuildTitle"), 
                            TexlipsePlugin.getResourceString("builderErrorDuringBuild"), 
                            TexlipsePlugin.getResourceString("builderErrorDuringBuildToggle"), false, null, null);
                    if (mess.getToggleState()) {
                        toggle.append(true);
                    }
                    if (mess.getReturnCode() == MessageDialogWithToggle.CANCEL)
                        returnCode.append(false);
                 } 
             });
             
             if (toggle.length() > 0) {
                 if (returnCode.length() > 0) {
                     project.setSessionProperty(new QualifiedName(null, "AlwaysContinueBuilding"), new Boolean(false));
                 } else {
                     project.setSessionProperty(new QualifiedName(null, "AlwaysContinueBuilding"), new Boolean(true));
                 }
             }
             if (returnCode.length() > 0) {
                 return false;
             }
         } else {
             //we have a saved state
             Boolean b = (Boolean) c;
             if (b.booleanValue() == true)
                 return true;
             else
                 return false;
         }
         return true;
     }
     
     /**
      * Run latex and optionally bibtex to produce a dvi file.
      * @throws CoreException if the build fails at any point
      */
     public void buildResource(IResource resource) throws CoreException {
         boolean error = false;
 		stopped = false;
         // Make sure we close the output document first 
     	// (using DDE on Win32)
     	if (Platform.getOS().equals(Platform.OS_WIN32)) {
     		monitor.subTask("Closing output document");    	
     		ViewerManager.closeOutputDocument();
     		monitor.worked(5);    		
     	}
     	  		
     	monitor.subTask("Building document");
         try {
             latex.run(resource);
         } catch (BuilderCoreException ex) {
             //Don't stop here, we will ask the user later
            //TODO: Error managment
             error = true;
         }
         monitor.worked(10);
         if (stopped)
             return;
         IProject project = resource.getProject();
         String runBib = (String) TexlipseProperties.getSessionProperty(project, TexlipseProperties.SESSION_BIBTEX_RERUN);
         Boolean bibChange = (Boolean) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBFILES_CHANGED);
         IResource runIdx = findIndex(project, resource);
         IResource runNomencl = findNomencl(project, resource);
         
         // if bibtex is not used, maybe the references need to be updated in the main document
         String rerun = (String) TexlipseProperties.getSessionProperty(resource.getProject(), TexlipseProperties.SESSION_LATEX_RERUN);
         
         // if bibtex is used, the bibliography might be changed
         String[] bibs = (String[]) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBFILE_PROPERTY);
         
         if (bibs != null && bibs.length > 0 && (runBib != null || bibChange != null)) {
             
/*            if (error) {
                 if (askUserForContinue(project) == false) {
                     throw new BuilderCoreException(TexlipsePlugin.stat("Errors during build. See the problems dialog."));
                 }
            }*/
             
             bibtex.run(resource);
             if (stopped)
                 return;
             monitor.worked(10);
             
             TexlipseProperties.setSessionProperty(project, TexlipseProperties.SESSION_BIBTEX_RERUN, null);
             TexlipseProperties.setSessionProperty(project, TexlipseProperties.BIBFILES_CHANGED, null);
             
             if (runIdx != null) {
                 makeIndex.run(resource);
                 if (stopped)
                     return;
                 monitor.worked(10);
             }
             
             if (runNomencl != null)
             {
                 // Running makeindex to build nomenclature index
                 // when %input.nlo file is detected
                 makeIndexNomencl.run(resource);
                 if (stopped)
                     return;
                 monitor.worked(10);
             }
               
             try {
                 latex.run(resource);
             } catch (BuilderCoreException ex) {
                //if (!error)
                //    throw ex;
             }
             if (stopped)
                 return;
             monitor.worked(10);
             try {
                 latex.run(resource);
             } catch (BuilderCoreException ex) {
                //if (!error)
                //    throw ex;
             }
             if (stopped)
                 return;
             monitor.worked(10);
             
         } else if (rerun != null || runIdx != null || runNomencl != null) {
 
/*            if (error) {
                 if (askUserForContinue(project) == false) {
                     throw new BuilderCoreException(TexlipsePlugin.stat("Errors during build. See the problems dialog."));
                 }
            }*/
             
             if (runIdx != null) {
                 makeIndex.run(resource);
                 if (stopped)
                     return;
                 monitor.worked(10);
             }
             
             if (runNomencl != null)
             {
                 // Running makeindex to build nomenclature index
                 // when %input.nlo file is detected
                 makeIndexNomencl.run(resource);
                 if (stopped)
                     return;
                 monitor.worked(10);
             }
             
             try {
                 latex.run(resource);
             } catch (BuilderCoreException ex) {
                //if (!error)
                    //throw ex;
             }
             if (stopped)
                 return;
             monitor.worked(10);
             
             TexlipseProperties.setSessionProperty(resource.getProject(), TexlipseProperties.SESSION_LATEX_RERUN, null);
         }
     }
 
     /**
      * Find a handle to the index file of this project.
      * @param project the current project
      * @param source buildable resource inside project 
      * @return handle to index file or null if not found.
      *         Returns null also if the index file is older than the current output file
      */
     private IResource findIndex(IProject project, IResource source) {
         
         IContainer srcDir = TexlipseProperties.getProjectSourceDir(project);
         if (srcDir == null) {
             srcDir = project;
         }
         
         String name = source.getName();
         String idxName = name.substring(0, name.length() - source.getFileExtension().length()) + TexlipseProperties.INPUT_FORMAT_IDX;
         IResource idxFile = srcDir.findMember(idxName);
         if (idxFile == null) {
             return null;
         }
         
         IResource outFile = TexlipseProperties.getProjectOutputFile(project);
         if (outFile.getLocalTimeStamp() > idxFile.getLocalTimeStamp()) {
             return null;
         }
         
         return idxFile;
     }
     
     /**
      * Find a handle to the index file of this project.
      * @param project the current project
      * @param source buildable resource inside project 
      * @return handle to index file or null if not found.
      *         Returns null also if the index file is older than the current output file
      */
     private IResource findNomencl(IProject project, IResource source) {
         
         IContainer srcDir = TexlipseProperties.getProjectSourceDir(project);
         if (srcDir == null) {
             srcDir = project;
         }
 
         String name = source.getName();
         String nomenclName = name.substring(0, name.length() - source.getFileExtension().length()) + TexlipseProperties.INPUT_FORMAT_NOMENCL;
         IResource idxFile = srcDir.findMember(nomenclName);
     
         if (idxFile == null) {
             return null;
         }
         
         IResource outFile = TexlipseProperties.getProjectOutputFile(project);
         if (outFile.getLocalTimeStamp() > idxFile.getLocalTimeStamp()) {
             return null;
         }
         
         return idxFile;
     }
 }
