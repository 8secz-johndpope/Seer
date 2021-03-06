 package org.eclipse.jdt.internal.debug.ui.actions;
 
 /**********************************************************************
 Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 This file is made available under the terms of the Common Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/cpl-v10.html
 **********************************************************************/
 
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.jdt.internal.debug.ui.launcher.RuntimeClasspathViewer;
 import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
 import org.eclipse.jdt.launching.JavaRuntime;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.widgets.DirectoryDialog;
 
 /**
  * Adds an external folder to the runtime class path.
  */
 public class AddExternalFolderAction extends OpenDialogAction {
 
 	public AddExternalFolderAction(RuntimeClasspathViewer viewer, String dialogSettingsPrefix) {
 		super(ActionMessages.getString("AddExternalFolderAction.Add_External_Folder_1"), viewer, dialogSettingsPrefix); //$NON-NLS-1$
 	}	
 
 	/**
 	 * Prompts for a folder to add.
 	 * 
 	 * @see IAction#run()
 	 */	
 	public void run() {
 							
 		String lastUsedPath= getDialogSetting(LAST_PATH_SETTING);
 		if (lastUsedPath == null) {
 			lastUsedPath= ""; //$NON-NLS-1$
 		}
 		DirectoryDialog dialog= new DirectoryDialog(getShell(), SWT.MULTI);
 		dialog.setText(ActionMessages.getString("AddExternalFolderAction.Folder_Selection_3")); //$NON-NLS-1$
 		dialog.setFilterPath(lastUsedPath);
 		String res= dialog.open();
 		if (res == null) {
 			return;
 		}
 			
 		IPath filterPath= new Path(dialog.getFilterPath());
 		IRuntimeClasspathEntry[] elems= new IRuntimeClasspathEntry[1];
		IPath path= filterPath.append(res).makeAbsolute();	
 		elems[0]= JavaRuntime.newArchiveRuntimeClasspathEntry(path);
 
 		setDialogSetting(LAST_PATH_SETTING, filterPath.toOSString());
 		
 		getViewer().addEntries(elems);
 	}
 
 	/**
 	 * @see SelectionListenerAction#updateSelection(IStructuredSelection)
 	 */
 	protected boolean updateSelection(IStructuredSelection selection) {
 		return getViewer().isEnabled();
 	}
 	
 }
