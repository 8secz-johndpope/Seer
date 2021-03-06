 /*******************************************************************************
  * Copyright (c) 2000, 2003 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials 
  * are made available under the terms of the Common Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/cpl-v10.html
  * 
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.team.internal.ui.wizards;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.IDialogConstants;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.viewers.CheckStateChangedEvent;
 import org.eclipse.jface.viewers.CheckboxTableViewer;
 import org.eclipse.jface.viewers.ICheckStateListener;
 import org.eclipse.jface.viewers.TableLayout;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.FileDialog;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.team.core.RepositoryProvider;
 import org.eclipse.team.internal.ui.IHelpContextIds;
 import org.eclipse.team.internal.ui.Policy;
 import org.eclipse.ui.help.WorkbenchHelp;
 import org.eclipse.ui.model.WorkbenchContentProvider;
 import org.eclipse.ui.model.WorkbenchLabelProvider;
 
 public class ExportProjectSetMainPage extends TeamWizardPage {
 	Text fileText;
 	String file = ""; //$NON-NLS-1$
 	Button browseButton;
 	List selectedProjects = new ArrayList();
 
 	CheckboxTableViewer tableViewer;
 	Table table;
 
 	class ProjectContentProvider extends WorkbenchContentProvider {
 		public Object[] getElements(Object element) {
 			if (element instanceof IProject[]) return (IProject[]) element;
 			return null;
 		}
 	};
 
 	public ExportProjectSetMainPage(String pageName, String title, ImageDescriptor titleImage) {
 		super(pageName, title, titleImage);
 	}
 
 	/*
 	 * @see IDialogPage#createControl(Composite)
 	 */
 	public void createControl(Composite parent) {
 		Composite composite = createComposite(parent, 1);
 		initializeDialogUnits(composite);
 
 		// set F1 help
 		WorkbenchHelp.setHelp(composite, IHelpContextIds.EXPORT_PROJECT_SET_PAGE);
 				
 		createLabel(composite, Policy.bind("ExportProjectSetMainPage.Select_the_projects_to_include_in_the_project_set__2")); //$NON-NLS-1$
 
 		table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
 		tableViewer = new CheckboxTableViewer(table);
 		table.setLayout(new TableLayout());
 		GridData data = new GridData(GridData.FILL_BOTH);
 		data.heightHint = 300;
 		table.setLayoutData(data);
 		tableViewer.setContentProvider(new ProjectContentProvider());
 		tableViewer.setLabelProvider(new WorkbenchLabelProvider());
 		tableViewer.addCheckStateListener(new ICheckStateListener() {
 			public void checkStateChanged(CheckStateChangedEvent event) {
 				IProject project = (IProject)event.getElement();
 				if (event.getChecked()) {
 					selectedProjects.add(project);
 				} else {
 					selectedProjects.remove(project);
 				}
 				updateEnablement();
 			}
 		});
 		createLabel(composite, Policy.bind("ExportProjectSetMainPage.Project_Set_File_Name__3")); //$NON-NLS-1$
 
 		Composite inner = new Composite(composite, SWT.NULL);
 		inner.setLayoutData(new GridData(GridData.FILL_BOTH));
 		GridLayout layout = new GridLayout();
 		layout.numColumns = 3;
 		layout.marginHeight = 0;
 		layout.marginWidth = 0;
 		inner.setLayout(layout);
 
 		createLabel(inner, Policy.bind("ExportProjectSetMainPage.&File_name__1")); //$NON-NLS-1$
 		fileText = createTextField(inner);
 		if (file != null) fileText.setText(file);
 		fileText.addListener(SWT.Modify, new Listener() {
 			public void handleEvent(Event event) {
 				file = fileText.getText();
 				updateEnablement();
 			}
 		});
 
 		browseButton = new Button(inner, SWT.PUSH);
 		browseButton.setText(Policy.bind("ExportProjectSetMainPage.Browse_4")); //$NON-NLS-1$
 		data = new GridData();
 		data.horizontalAlignment = GridData.FILL;
 		data.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
 		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
 		data.widthHint = Math.max(widthHint, browseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
 		browseButton.setLayoutData(data);
 		browseButton.addListener(SWT.Selection, new Listener() {
 			public void handleEvent(Event event) {
 				FileDialog d = new FileDialog(getShell(), SWT.SAVE);
 				d.setFilterExtensions(new String[] {"*.psf"}); //$NON-NLS-1$
 				d.setFilterNames(new String[] {Policy.bind("ExportProjectSetMainPage.Project_Set_Files_3")}); //$NON-NLS-1$
 				d.setFileName(Policy.bind("ExportProjectSetMainPage.default")); //$NON-NLS-1$
 				d.setFilterPath(new File(".").getAbsolutePath()); //$NON-NLS-1$
 				String f = d.open();
 				if (f != null) {
 					fileText.setText(f);
 					file = f;
 				}
 			}
 		});
 
 		initializeProjects();
 		setControl(composite);
 		updateEnablement();
         Dialog.applyDialogFont(parent);
 	}
 
 	private void initializeProjects() {
 		List projectList = new ArrayList();
 		IProject[] workspaceProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
 		for (int i = 0; i < workspaceProjects.length; i++) {
 			if (RepositoryProvider.getProvider(workspaceProjects[i]) != null) {
 				projectList.add(workspaceProjects[i]);
 			}
 		}
 		tableViewer.setInput((IProject[]) projectList.toArray(new IProject[projectList.size()]));
 		// Check any necessary projects
 		if (selectedProjects != null) {
 			tableViewer.setCheckedElements((IProject[])selectedProjects.toArray(new IProject[selectedProjects.size()]));
 		}
 	}
 	private void updateEnablement() {
 		boolean complete;
 		if (selectedProjects.size() == 0) {
 			setMessage(null);
 			complete = false;
 		} else if (file.length() == 0) {
 			setMessage(null);
 			complete = false;
 		} else {
 			File f = new File(file);
 			if (f.isDirectory()) {
 				setMessage(Policy.bind("ExportProjectSetMainPage.You_have_specified_a_folder_5"), ERROR); //$NON-NLS-1$
 				complete = false;
 			} else {
 				complete = true;
 			}
 		}
 		if (complete) {
 			setMessage(null);
 		}
 		setPageComplete(complete);
 	}
 
 	public String getFileName() {
 		return file;
 	}
 	public void setFileName(String file) {
 		if (file != null) {
 			this.file = file;
 		}
 	}
 	
 	public IProject[] getSelectedProjects() {
 		return (IProject[])selectedProjects.toArray(new IProject[selectedProjects.size()]);
 	}
 	public void setSelectedProjects(IProject[] selectedProjects) {
 		this.selectedProjects.addAll(Arrays.asList(selectedProjects));
 	}
 	public void setVisible(boolean visible) {
 		super.setVisible(visible);
 		if (visible) {
 			fileText.setFocus();
 		}
 	}
 }
