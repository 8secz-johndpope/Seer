 /*******************************************************************************
  * Copyright (c) 2000, 2006 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *     Sebastian Davids <sdavids@gmx.de> - layout tweaks
 *     Matt McCutchen <hashproduct+eclipse@gmail.com> - Bug 180358 [Apply Patch] Cursor jumps to beginning of filename field on keystroke
  *******************************************************************************/
 package org.eclipse.compare.internal.patch;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
 import com.ibm.icu.text.MessageFormat;
 
 import org.eclipse.compare.internal.ICompareContextIds;
 import org.eclipse.compare.internal.Utilities;
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IFolder;
 import org.eclipse.core.resources.IProject;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.core.runtime.IPath;
 import org.eclipse.core.runtime.Path;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.IDialogConstants;
 import org.eclipse.jface.dialogs.IDialogSettings;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.viewers.DoubleClickEvent;
 import org.eclipse.jface.viewers.IDoubleClickListener;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.ISelectionChangedListener;
 import org.eclipse.jface.viewers.SelectionChangedEvent;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.jface.viewers.TreeSelection;
 import org.eclipse.jface.viewers.TreeViewer;
 import org.eclipse.jface.wizard.IWizardPage;
 import org.eclipse.jface.wizard.WizardPage;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.dnd.Clipboard;
 import org.eclipse.swt.dnd.TextTransfer;
 import org.eclipse.swt.events.ModifyEvent;
 import org.eclipse.swt.events.ModifyListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.ShellAdapter;
 import org.eclipse.swt.events.ShellEvent;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.FileDialog;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.model.WorkbenchContentProvider;
 import org.eclipse.ui.model.WorkbenchLabelProvider;
 import org.eclipse.ui.views.navigator.ResourceSorter;
 
 
 
 /* package */ class InputPatchPage extends WizardPage {
 
 	// constants
 	protected static final int SIZING_TEXT_FIELD_WIDTH= 250;
 	protected static final int COMBO_HISTORY_LENGTH= 5;
 	
 	// dialog store id constants
 	private final static String PAGE_NAME= "PatchWizardPage1"; //$NON-NLS-1$  
 	private final static String STORE_PATCH_FILES_ID= PAGE_NAME+".PATCH_FILES"; //$NON-NLS-1$
 	private final static String STORE_INPUT_METHOD_ID= PAGE_NAME+".INPUT_METHOD"; //$NON-NLS-1$
 	private final static String STORE_WORKSPACE_PATH_ID= PAGE_NAME+".WORKSPACE_PATH"; //$NON-NLS-1$
 	
 	//patch input constants
 	protected final static int CLIPBOARD= 1;
 	protected final static int FILE= 2;
 	protected final static int WORKSPACE= 3;
 
 	protected final static String INPUTPATCHPAGE_NAME= "InputPatchPage"; //$NON-NLS-1$
 
 	static final char SEPARATOR= System.getProperty("file.separator").charAt(0); //$NON-NLS-1$
 
 	private boolean fShowError= false;
 	private String fPatchSource;
 	private boolean fPatchRead= false;
 	private PatchWizard fPatchWizard;
 	private ActivationListener fActivationListener= new ActivationListener();
 
 	// SWT widgets
 	private Button fUseClipboardButton;
 	private Combo fPatchFileNameField;
 	private Button fPatchFileBrowseButton;
 	private Button fUsePatchFileButton;
 	private Button fUseWorkspaceButton;
 	private Label fWorkspaceSelectLabel;
 	private TreeViewer fTreeViewer;
 	
 	class ActivationListener extends ShellAdapter {
 		public void shellActivated(ShellEvent e) {
 			// allow error messages if the selected input actually has something selected in it
 			fShowError=true;
 			switch(getInputMethod()) {
 			case FILE:
 				fShowError= (fPatchFileNameField.getText() != "");  //$NON-NLS-1$
 				break;
 			    
 			case WORKSPACE:
 				fShowError= (!fTreeViewer.getSelection().isEmpty());
 				break;
 			}
 			updateWidgetEnablements();
 		}
 	}
 	
 	InputPatchPage(PatchWizard pw) {
 		super(INPUTPATCHPAGE_NAME, PatchMessages.InputPatchPage_title, null);
 		fPatchWizard= pw;
 		setMessage(PatchMessages.InputPatchPage_message);
 	}
 	
 	/*
 	 * Get a path from the supplied text widget.
 	 * @return org.eclipse.core.runtime.IPath
 	 */
 	protected IPath getPathFromText(Text textField) {
 		return (new Path(textField.getText())).makeAbsolute();
 	}
 
 	/* package */ String getPatchName() {
 		if (getInputMethod() == CLIPBOARD)
 			return PatchMessages.InputPatchPage_Clipboard;
 		return getPatchFilePath();
 	}
 	
 	public void createControl(Composite parent) {
 				
 		Composite composite= new Composite(parent, SWT.NULL);
 		composite.setLayout(new GridLayout());
 		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
 		setControl(composite);
 
 		initializeDialogUnits(parent);
 		
 		buildPatchFileGroup(composite);
 		
 		// by default, whatever was used last was selected or 
 		// default to File if nothing has been selected
 		restoreWidgetValues();
 		
 		// see if there are any better options presently selected (i.e workspace
 		// or clipboard)
 		adjustToCurrentTarget();
 		
 		// No error for dialog opening
 		fShowError= false;
 		clearErrorMessage();
 		updateWidgetEnablements();
 		
 		Shell shell= getShell();
 		shell.addShellListener(fActivationListener);
 		
 		Dialog.applyDialogFont(composite);
 		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, ICompareContextIds.PATCH_INPUT_WIZARD_PAGE);
 	}
 
 	/**
 	 * Returns the next page depending on what type of patch is being applied:
 	 * 	 i) If the patch is a Workspace patch then it will proceed right to the PreviewPatchPage
 	 *  ii) If the patch is a single project patch then it will proceed to the PatchTargetPage, which
 	 *      allows the user to specify where to root the patch 
 	 * @return PreviewPatchPage if multi-project patch, PatchTargetPage if single project patch
 	 */
 	public IWizardPage getNextPage() {
 
 		WorkspacePatcher patcher= ((PatchWizard) getWizard()).getPatcher();
 		
 		// Read in the patch
 		readInPatch();
 		
 		FileDiff[] diffs= patcher.getDiffs();
 		if (diffs == null || diffs.length == 0) {
 			String format= PatchMessages.InputPatchPage_NoDiffsFound_format;	
 			String message= MessageFormat.format(format, new String[] { fPatchSource });
 			MessageDialog.openInformation(null,
 				PatchMessages.InputPatchPage_PatchErrorDialog_title, message); 
 			return this;
 		}
 
 		// guess prefix count
 		int guess= 0; // guessPrefix(diffs);
 		patcher.setStripPrefixSegments(guess);
 
 		// If this is a workspace patch we don't need to set a target as the targets will be figured out from 
 		// all of the projects that make up the patch and continue on to final preview page 
 		// else go on to target selection page
 		if (patcher.isWorkspacePatch()) {
 			return fPatchWizard.getPage(PreviewPatchPage2.PREVIEWPATCHPAGE_NAME);
 		}
 
 		return super.getNextPage();
 	}
 
 	/*
 	 * Reads in the patch contents
 	 */
 	public void readInPatch(){
 		WorkspacePatcher patcher= ((PatchWizard) getWizard()).getPatcher();
 		// Create a reader for the input
 		Reader reader= null;
 		try {
 			int inputMethod= getInputMethod();
 			if (inputMethod == CLIPBOARD) {
 				Control c= getControl();
 				if (c != null) {
 					Clipboard clipboard= new Clipboard(c.getDisplay());
 					Object o= clipboard.getContents(TextTransfer.getInstance());
 					clipboard.dispose();
 					if (o instanceof String)
 						reader= new StringReader((String)o);
 				}
 				fPatchSource= PatchMessages.InputPatchPage_Clipboard_title;
 			} else if (inputMethod==FILE) {
 				String patchFilePath= getPatchFilePath();
 				if (patchFilePath != null) {
 					try {
 						reader= new FileReader(patchFilePath);
 					} catch (FileNotFoundException ex) {
 						MessageDialog.openError(null,
 							PatchMessages.InputPatchPage_PatchErrorDialog_title,	
 							PatchMessages.InputPatchPage_PatchFileNotFound_message); 
 					}
 				}
 				fPatchSource= PatchMessages.InputPatchPage_PatchFile_title;
 			} else if (inputMethod==WORKSPACE) {
 				// Get the selected patch file (tree will only allow for one selection)
 				IResource[] resources= Utilities.getResources(fTreeViewer.getSelection());
 				IResource patchFile= resources[0];
 				if (patchFile != null) {
 					try {
 						reader= new FileReader(patchFile.getRawLocation().toFile());
 					} catch (FileNotFoundException ex) {
 						MessageDialog.openError(null, PatchMessages.InputPatchPage_PatchErrorDialog_title, PatchMessages.InputPatchPage_PatchFileNotFound_message);
 					} catch (NullPointerException nex) {
 						//in case the path doesn't exist
 						MessageDialog.openError(null, PatchMessages.InputPatchPage_PatchErrorDialog_title, PatchMessages.InputPatchPage_PatchFileNotFound_message);
 					}
 				}
 				fPatchSource= PatchMessages.InputPatchPage_WorkspacePatch_title;
 			}
 			
 			// parse the input
 			if (reader != null) {
 				try {
 					patcher.parse(new BufferedReader(reader));
 					//report back to the patch wizard that the patch has been read in
 					fPatchWizard.patchReadIn();
 					fPatchRead=true;
 				} catch (IOException ex) {
 					MessageDialog.openError(null,
 						PatchMessages.InputPatchPage_PatchErrorDialog_title, 
 						PatchMessages.InputPatchPage_ParseError_message); 
 				}
 			}
 		} finally {
 			if (reader != null) {
 				try {
 					reader.close();
 				} catch (IOException x) {
 					// silently ignored
 				}
 			}
 		}
 	}
 	
 	/* (non-JavaDoc)
 	 * Method declared in IWizardPage.
 	 */
 	public boolean canFlipToNextPage() {
 		// we can't call getNextPage to determine if flipping is allowed since computing
 		// the next page is quite expensive. So we say yes if the page is complete.
 		return isPageComplete();
 	}
 	
 	private void setEnablePatchFile(boolean enable) {
 		fPatchFileNameField.setEnabled(enable);
 		fPatchFileBrowseButton.setEnabled(enable);
 	}
 
 	private void setEnableWorkspacePatch(boolean enable) {
 		fWorkspaceSelectLabel.setEnabled(enable);
 		fTreeViewer.getTree().setEnabled(enable);
 	}
 
 	/*
 	 *	Create the group for selecting the patch file
 	 */
 	private void buildPatchFileGroup(Composite parent) {
 
 		final Composite composite= new Composite(parent, SWT.NULL);
 		GridLayout gridLayout= new GridLayout();
 		gridLayout.numColumns= 3;
 		composite.setLayout(gridLayout);
 		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
 
 		// 1st row
 		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
 		gd.horizontalSpan= 3;
 		fUseClipboardButton= new Button(composite, SWT.RADIO);
 		fUseClipboardButton.setText(PatchMessages.InputPatchPage_UseClipboardButton_text);
 		fUseClipboardButton.setLayoutData(gd);
 
 		// 2nd row
 		fUsePatchFileButton= new Button(composite, SWT.RADIO);
 		fUsePatchFileButton.setText(PatchMessages.InputPatchPage_FileButton_text);
 
 		fPatchFileNameField= new Combo(composite, SWT.BORDER);
 		gd= new GridData(GridData.FILL_HORIZONTAL);
 		gd.widthHint= SIZING_TEXT_FIELD_WIDTH;
 		fPatchFileNameField.setLayoutData(gd);
 
 		fPatchFileBrowseButton= new Button(composite, SWT.PUSH);
 		fPatchFileBrowseButton.setText(PatchMessages.InputPatchPage_ChooseFileButton_text);
 		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
 		int widthHint= convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
 		Point minSize= fPatchFileBrowseButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
 		data.widthHint= Math.max(widthHint, minSize.x);
 		fPatchFileBrowseButton.setLayoutData(data);
 
 		//3rd row
 		fUseWorkspaceButton= new Button(composite, SWT.RADIO);
 		fUseWorkspaceButton.setText(PatchMessages.InputPatchPage_UseWorkspaceButton_text);
 		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
 		fUseWorkspaceButton.setLayoutData(gd);
 
 		addWorkspaceControls(parent);
 
 		// Add listeners
 		fUseClipboardButton.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				if (!fUseClipboardButton.getSelection())
 					return;
 				
 				clearErrorMessage();
 				fShowError= true;
 				int state= getInputMethod();
 				setEnablePatchFile(state == FILE);
 				setEnableWorkspacePatch(state == WORKSPACE);
 				updateWidgetEnablements();
 			}
 		});
 		
 		fUsePatchFileButton.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				if (!fUsePatchFileButton.getSelection())
 					return;
 				//If there is anything typed in at all
 				clearErrorMessage();
 				fShowError= (fPatchFileNameField.getText() != ""); //$NON-NLS-1$
 				int state= getInputMethod();
 				setEnablePatchFile(state==FILE);
 				setEnableWorkspacePatch(state==WORKSPACE);
 				updateWidgetEnablements();
 			}
 		});
 		fPatchFileNameField.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				updateWidgetEnablements();
 			}
 		});
 		fPatchFileNameField.addModifyListener(new ModifyListener() {
 			public void modifyText(ModifyEvent e) {
 				clearErrorMessage();
 				fShowError= true;
 				updateWidgetEnablements();
 			}
 		});
 		fPatchFileBrowseButton.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				clearErrorMessage();
 				fShowError= true;
 				handlePatchFileBrowseButtonPressed();
 				updateWidgetEnablements();
 			}
 		});
 		fUseWorkspaceButton.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				if (!fUseWorkspaceButton.getSelection())
 					return;
 				clearErrorMessage();
 				// If there is anything typed in at all
 				fShowError= (!fTreeViewer.getSelection().isEmpty());
 				int state= getInputMethod();
 				setEnablePatchFile(state == FILE);
 				setEnableWorkspacePatch(state == WORKSPACE);
 				updateWidgetEnablements();
 			}
 		});
 
 		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
 			public void selectionChanged(SelectionChangedEvent event) {
 				clearErrorMessage();
 				updateWidgetEnablements();
 			}
 		});
 		
 		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
 			public void doubleClick(DoubleClickEvent event) {
 				ISelection selection= event.getSelection();
 				if (selection instanceof TreeSelection) {
 					TreeSelection treeSel= (TreeSelection) selection;
 					Object res= treeSel.getFirstElement();
 					if (res != null) {
 						if (res instanceof IProject || res instanceof IFolder) {
 							if (fTreeViewer.getExpandedState(res))
 								fTreeViewer.collapseToLevel(res, 1);
 							else
 								fTreeViewer.expandToLevel(res, 1);
 						} else if (res instanceof IFile)
 							fPatchWizard.showPage(getNextPage());
 					}
 				}
 			}
 		});
 	}
 
 	private void addWorkspaceControls(Composite composite) {
 
 		Composite newComp= new Composite(composite, SWT.NONE);
 		GridLayout layout= new GridLayout(1, false);
 		layout.marginLeft= 16; // align w/ lable of check button
 		newComp.setLayout(layout);
 		newComp.setLayoutData(new GridData(GridData.FILL_BOTH));
 			
 		fWorkspaceSelectLabel= new Label(newComp, SWT.LEFT);
 		fWorkspaceSelectLabel.setText(PatchMessages.InputPatchPage_WorkspaceSelectPatch_text);
 		
 		fTreeViewer= new TreeViewer(newComp, SWT.BORDER);
 		fTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
 
 		fTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
 		fTreeViewer.setContentProvider(new WorkbenchContentProvider());
 		fTreeViewer.setSorter(new ResourceSorter(ResourceSorter.NAME));
 		fTreeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
 	}
 
 
 	/**
 	 * Updates the enable state of this page's controls.
 	 */
 	private void updateWidgetEnablements() {
 		
 		String error= null;
 
 		boolean gotPatch= false;
 		int inputMethod= getInputMethod();
 		if (inputMethod==CLIPBOARD) {
 			Control c= getControl();
 			if (c != null) {
 				Clipboard clipboard= new Clipboard(c.getDisplay());
 				Object o= clipboard.getContents(TextTransfer.getInstance());
 				clipboard.dispose();
 				if (o instanceof String) {
 					String s= ((String) o).trim();
 					if (s.length() > 0)
 						gotPatch= true;
 					else
 						error= PatchMessages.InputPatchPage_ClipboardIsEmpty_message; 
 				} else
 					error= PatchMessages.InputPatchPage_NoTextInClipboard_message;					 
 			} else
 				error= PatchMessages.InputPatchPage_CouldNotReadClipboard_message;
 		} else if (inputMethod==FILE) {
 			String path= fPatchFileNameField.getText();
 			if (path != null && path.length() > 0) {
 				File file= new File(path);
 				gotPatch= file.exists() && file.isFile() && file.length() > 0;
 				if (!gotPatch)
 					error= PatchMessages.InputPatchPage_CannotLocatePatch_message + path; 
 			} else {
 				error= PatchMessages.InputPatchPage_NoFileName_message; 
 			}
 		} else if (inputMethod == WORKSPACE) {
 			//Get the selected patch file (tree will only allow for one selection)
 			IResource[] resources= Utilities.getResources(fTreeViewer.getSelection());
 			if (resources != null && resources.length > 0) {
 				IResource patchFile= resources[0];
 				if (patchFile != null && patchFile.getType() == IResource.FILE) {
 					File actualFile= patchFile.getRawLocation().toFile();
 					gotPatch= actualFile.exists()&&actualFile.isFile()&&actualFile.length() > 0;
 					if (!gotPatch)
 						error= PatchMessages.InputPatchPage_FileSelectedNotPatch_message;
 				}
 			} else {
 				error= PatchMessages.InputPatchPage_NoFileName_message;
 			}
 		}
 
 		setPageComplete(gotPatch);
 
 		if (fShowError)
 			setErrorMessage(error);
 	}
 	
 	protected void handlePatchFileBrowseButtonPressed() {
 		FileDialog dialog= new FileDialog(getShell(), SWT.NONE);
 		dialog.setText(PatchMessages.InputPatchPage_SelectPatchFileDialog_title);		 
 		String patchFilePath= getPatchFilePath();
 		if (patchFilePath != null) {
 			int lastSegment= patchFilePath.lastIndexOf(SEPARATOR);
 			if (lastSegment > 0) {
 				patchFilePath= patchFilePath.substring(0, lastSegment);
 			}
 		}
 		dialog.setFilterPath(patchFilePath);
 		String res= dialog.open();
 		if (res == null)
 			return;
 		
 		patchFilePath= dialog.getFileName();
 		IPath filterPath= new Path(dialog.getFilterPath());
 		IPath path= filterPath.append(patchFilePath).makeAbsolute();	
 		patchFilePath= path.toOSString();
 		//fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, filterPath.toOSString());
 		
 		fPatchFileNameField.setText(patchFilePath);
 		//setSourceName(patchFilePath);
 	}
 	
 	/**
 	 * Sets the source name of the import to be the supplied path.
 	 * Adds the name of the path to the list of items in the
 	 * source combo and selects it.
 	 *
 	 * @param path the path to be added
 	 */
 	protected void setSourceName(String path) {
 	
 		if (path.length() > 0) {
 	
 			String[] currentItems= fPatchFileNameField.getItems();
 			int selectionIndex= -1;
 			for (int i= 0; i < currentItems.length; i++)
 				if (currentItems[i].equals(path))
 					selectionIndex= i;
 			
 			if (selectionIndex < 0) {	// not found in history
 				int oldLength= currentItems.length;
 				String[] newItems= new String[oldLength + 1];
 				System.arraycopy(currentItems, 0, newItems, 0, oldLength);
 				newItems[oldLength]= path;
 				fPatchFileNameField.setItems(newItems);
 				selectionIndex= oldLength;
 			}
 			fPatchFileNameField.select(selectionIndex);
 	
 			//resetSelection();
 		}
 	}
 	
 	/**
 	 *	The Finish button was pressed. Try to do the required work now and answer
 	 *	a boolean indicating success. If false is returned then the wizard will
 	 *	not close.
 	 *
 	 * @return boolean
 	 */
 	public boolean finish() {
 //		if (!ensureSourceIsValid())
 //			return false;
 	
 		saveWidgetValues();
 	
 //		Iterator resourcesEnum= getSelectedResources().iterator();
 //		List fileSystemObjects= new ArrayList();
 //		while (resourcesEnum.hasNext()) {
 //			fileSystemObjects.add(
 //				((FileSystemElement) resourcesEnum.next()).getFileSystemObject());
 //		}
 //	
 //		if (fileSystemObjects.size() > 0)
 //			return importResources(fileSystemObjects);
 //	
 //		MessageDialog
 //			.openInformation(
 //				getContainer().getShell(),
 //				DataTransferMessages.getString("DataTransfer.information"), //$NON-NLS-1$
 //				DataTransferMessages.getString("FileImport.noneSelected")); //$NON-NLS-1$
 //	
 //		return false;
 
 		return true;
 	}
 	
 	/**
 	 *	Use the dialog store to restore widget values to the values that they held
 	 *	last time this wizard was used to completion
 	 */
 	private void restoreWidgetValues() {
 
 		int inputMethod= FILE;
 
 		IDialogSettings settings= getDialogSettings();
 		if (settings != null) {
 
 			try {
 				inputMethod= settings.getInt(STORE_INPUT_METHOD_ID);
 			} catch (NumberFormatException ex) {
 				//OK - no value stored in settings; just use CLIPBOARD
 			}
 
 			// set filenames history
 			String[] sourceNames= settings.getArray(STORE_PATCH_FILES_ID);
 			if (sourceNames != null)
 				for (int i= 0; i < sourceNames.length; i++)
 					if (sourceNames[i] != null && sourceNames[i].length() > 0)
 						fPatchFileNameField.add(sourceNames[i]);
 			
 			// set patch file path
 			String patchFilePath= settings.get(STORE_PATCH_FILES_ID);
 			if (patchFilePath != null)
 				setSourceName(patchFilePath);
 			
 			// If the previous apply patch was used with a clipboard, we need to check
 			// if there is a valid patch on the clipboard. This will be done in adjustToCurrentTarget()
 			// so just set it to FILE now and, if there exists a patch on the clipboard, then clipboard
 			// will be selected automatically
 			if (inputMethod == CLIPBOARD){
 				inputMethod= FILE;
 				fPatchFileNameField.deselectAll();
 			}
 			
 			//set the workspace patch selection
 			String workspaceSetting= settings.get(STORE_WORKSPACE_PATH_ID);
 			if (workspaceSetting != null && workspaceSetting.length() > 0) {
 				// See if this resource still exists in the workspace
 				try {
 					IPath path= new Path(workspaceSetting);
 					IFile targetFile= ResourcesPlugin.getWorkspace().getRoot().getFile(path);
 					if (fTreeViewer != null && targetFile.exists()){
 						fTreeViewer.expandToLevel(targetFile, 0);
 						fTreeViewer.setSelection(new StructuredSelection(targetFile));
 					}
 				} catch (RuntimeException e) {
 					// Ignore. The setting was invalid
 				} 
 			} else {
 				//check to see if the current input is set to workspace - if it is switch it
 				//back to clipboard since there is no corresponding element to go along with 
 				//the tree viewer
 				if (inputMethod == WORKSPACE)
 					inputMethod= FILE;
 			}
 		}
 
 		// set radio buttons state
 		setInputButtonState(inputMethod);
 	}
 	
 	/**
 	 * 	Since Finish was pressed, write widget values to the dialog store so that they
 	 *	will persist into the next invocation of this wizard page
 	 */
 	void saveWidgetValues() {
 		IDialogSettings settings= getDialogSettings();
 		if (settings != null) {
 
 			settings.put(STORE_INPUT_METHOD_ID, getInputMethod());
 			settings.put(STORE_PATCH_FILES_ID, getPatchFilePath());
 			
 			// update source names history
 			String[] sourceNames= settings.getArray(STORE_PATCH_FILES_ID);
 			if (sourceNames == null)
 				sourceNames= new String[0];
 	
 			sourceNames= addToHistory(sourceNames, getPatchFilePath());
 			settings.put(STORE_PATCH_FILES_ID, sourceNames);
 			
 			// save the workspace selection
 			settings.put(STORE_WORKSPACE_PATH_ID, getWorkspacePath());
 			
 		}
 	}
 	
 	private String getWorkspacePath() {
 		if (fTreeViewer != null){
 			IResource[] resources= Utilities.getResources(fTreeViewer.getSelection());
 			if (resources.length > 0) {
 				IResource patchFile= resources[0];
 				return patchFile.getFullPath().toString();
 			}
 			
 		}
 		return ""; //$NON-NLS-1$
 	}
 
 	// static helpers
 	
 	/**
 	 * Checks to see if the file that has been selected for Apply Patch
 	 * is actually a patch
 	 * @return true if the file selected to run Apply Patch on in the workspace is a patch file
 	 * or if the clipboard contains a patch
 	 */
 	private boolean adjustToCurrentTarget() {
 		// readjust selection if there is a patch selected in the workspace or on the clipboard
 		// check workspace first
 		IResource patchTarget= fPatchWizard.getTarget();
 		if (patchTarget instanceof IFile) {
 			Reader reader= null;
 			try {
 				try {
 					reader= new FileReader(patchTarget.getRawLocation().toFile());
 					if (isPatchFile(reader)) {
 						// set choice to workspace
 						setInputButtonState(WORKSPACE);
 						if (fTreeViewer != null && patchTarget.exists()) {
 							fTreeViewer.expandToLevel(patchTarget, 0);
 							fTreeViewer.setSelection(new StructuredSelection(patchTarget));
 						}
 						return true;
 					}
 				} catch (FileNotFoundException ex) {
 					// silently ignored
 				} catch (NullPointerException nex) {
 					// silently ignored
 				}
 
 			} finally {
 				if (reader != null) {
 					try {
 						reader.close();
 					} catch (IOException x) {
 						// silently ignored
 					}
 				}
 			}
 		} else {
 			// check out clipboard contents
 			Reader reader= null;
 			Control c= getControl();
 			if (c != null) {
 				Clipboard clipboard= new Clipboard(c.getDisplay());
 				Object o= clipboard.getContents(TextTransfer.getInstance());
 				clipboard.dispose();
 				try {
 					if (o instanceof String) {
 						reader= new StringReader((String) o);
 						if (isPatchFile(reader)) {
 							setInputButtonState(CLIPBOARD);
 							return true;
 						}
 					}
 				} finally {
 					if (reader != null) {
 						try {
 							reader.close();
 						} catch (IOException x) {
 							// silently ignored
 						}
 					}
 				}
 			}
 		}
 		return false;
 	} 
 	
 	
 
 	private boolean isPatchFile(Reader reader) {
 		WorkspacePatcher patcher= ((PatchWizard) getWizard()).getPatcher();
 
 		try {
 			patcher.parse(new BufferedReader(reader));
 		} catch (IOException ex) {
 			return false;
 		}
 
 		FileDiff[] diffs= patcher.getDiffs();
 		if (diffs == null || diffs.length == 0)
 			return false;
 		return true;
 	}
 	
 	/*
 	 * Clears the dialog message box
 	 */
 	private void clearErrorMessage(){
 		setErrorMessage(null);
 	}
 	
 	private void setInputButtonState(int state) {
 
 		switch (state) {
 		case CLIPBOARD:
 			fUseClipboardButton.setSelection(true);
 			fUsePatchFileButton.setSelection(false);
 			fUseWorkspaceButton.setSelection(false);
 			break;
 
 		case FILE:
 			fUseClipboardButton.setSelection(false);
 			fUsePatchFileButton.setSelection(true);
 			fUseWorkspaceButton.setSelection(false);
 			break;
 
 		case WORKSPACE:
 			fUseClipboardButton.setSelection(false);
 			fUsePatchFileButton.setSelection(false);
 			fUseWorkspaceButton.setSelection(true);
 			break;
 		}
 
 		setEnablePatchFile(state == FILE);
 		setEnableWorkspacePatch(state == WORKSPACE);
 	}
 
 	protected int getInputMethod() {
 		if (fUseClipboardButton.getSelection())
 			return CLIPBOARD;
 		if (fUsePatchFileButton.getSelection())
 			return FILE;
 		return WORKSPACE;
 	}
 
 	private String getPatchFilePath() {
 		if (fPatchFileNameField != null)
 			return fPatchFileNameField.getText();
 		return ""; //$NON-NLS-1$
 	} 
 
 	/*
 	 * Adds an entry to a history, while taking care of duplicate history items
 	 * and excessively long histories. The assumption is made that all histories
 	 * should be of length <code>COMBO_HISTORY_LENGTH</code>.
 	 *
 	 * @param history the current history
 	 * @param newEntry the entry to add to the history
 	 */
 	protected static String[] addToHistory(String[] history, String newEntry) {
 		java.util.ArrayList l= new java.util.ArrayList(java.util.Arrays.asList(history));
 
 		l.remove(newEntry);
 		l.add(0,newEntry);
 	
 		// since only one new item was added, we can be over the limit
 		// by at most one item
 		if (l.size() > COMBO_HISTORY_LENGTH)
 			l.remove(COMBO_HISTORY_LENGTH);
 		
 		return (String[]) l.toArray(new String[l.size()]);
 	}
 
 	public boolean isPatchRead() {
 		return fPatchRead;
 	}
 }
 
