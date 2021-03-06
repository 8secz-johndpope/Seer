 /*******************************************************************************
  * Copyright (c) 2000, 2005 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
  *******************************************************************************/
 package org.eclipse.search.internal.ui.text;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.StringReader;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.regex.Pattern;
 import java.util.regex.PatternSyntaxException;
 
 import org.eclipse.core.runtime.IAdaptable;
 import org.eclipse.core.runtime.IStatus;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.resources.IWorkspaceRoot;
 import org.eclipse.core.resources.ResourcesPlugin;
 
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.CLabel;
 import org.eclipse.swt.events.ModifyEvent;
 import org.eclipse.swt.events.ModifyListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Label;
 
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.DialogPage;
 import org.eclipse.jface.dialogs.ErrorDialog;
 import org.eclipse.jface.dialogs.IDialogSettings;
 import org.eclipse.jface.resource.JFaceColors;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.IStructuredSelection;
 
 import org.eclipse.jface.text.Assert;
 import org.eclipse.jface.text.ITextSelection;
 
 import org.eclipse.ui.IEditorPart;
 import org.eclipse.ui.IEditorRegistry;
 import org.eclipse.ui.IFileEditorInput;
 import org.eclipse.ui.IWorkbenchPage;
 import org.eclipse.ui.IWorkingSet;
 import org.eclipse.ui.IWorkingSetManager;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.contentassist.ContentAssistHandler;
 
 import org.eclipse.search.ui.IReplacePage;
 import org.eclipse.search.ui.ISearchPage;
 import org.eclipse.search.ui.ISearchPageContainer;
 import org.eclipse.search.ui.ISearchQuery;
 import org.eclipse.search.ui.ISearchResultPage;
 import org.eclipse.search.ui.ISearchResultViewPart;
 import org.eclipse.search.ui.NewSearchUI;
 
 import org.eclipse.search.internal.core.text.FileNamePatternSearchScope;
 import org.eclipse.search.internal.core.text.PatternConstructor;
 import org.eclipse.search.internal.ui.ISearchHelpContextIds;
 import org.eclipse.search.internal.ui.Messages;
 import org.eclipse.search.internal.ui.ScopePart;
 import org.eclipse.search.internal.ui.SearchMessages;
 import org.eclipse.search.internal.ui.SearchPlugin;
 import org.eclipse.search.internal.ui.util.FileTypeEditor;
 import org.eclipse.search.internal.ui.util.SWTUtil;
 
 import org.eclipse.search2.internal.ui.text2.IScopeDescription;
 import org.eclipse.search2.internal.ui.text2.RetrieverQuery;
import org.eclipse.search2.internal.ui.text2.SearchMatchInformationProviderRegistry;
import org.eclipse.search2.internal.ui.text2.SelectedResourcesScopeDescription;
 import org.eclipse.search2.internal.ui.text2.WindowWorkingSetScopeDescription;
 import org.eclipse.search2.internal.ui.text2.WorkingSetScopeDescription;
 import org.eclipse.search2.internal.ui.text2.WorkspaceScopeDescription;
 
 public class TextSearchPage extends DialogPage implements ISearchPage, IReplacePage {
 
     private static final int HISTORY_SIZE= 12;
 	public static final String EXTENSION_POINT_ID= "org.eclipse.search.internal.ui.text.TextSearchPage"; //$NON-NLS-1$
 
 	// Dialog store id constants
 	private static final String PAGE_NAME= "TextSearchPage"; //$NON-NLS-1$
 	private static final String STORE_CASE_SENSITIVE= "CASE_SENSITIVE"; //$NON-NLS-1$
 	private static final String STORE_IS_REG_EX_SEARCH= "REG_EX_SEARCH"; //$NON-NLS-1$
 	private static final String STORE_SEARCH_DERIVED = "SEARCH_DERIVED"; //$NON-NLS-1$
 	private static final String STORE_HISTORY= "HISTORY"; //$NON-NLS-1$
 	private static final String STORE_HISTORY_SIZE= "HISTORY_SIZE"; //$NON-NLS-1$
 
 	private List fPreviousSearchPatterns= new ArrayList(20);
 
 	private boolean fFirstTime= true;
 	private boolean fIsCaseSensitive;
 	private boolean fIsRegExSearch;
 	private boolean fSearchDerived;
 	
 	private Combo fPattern;
 	private Button fIsCaseSensitiveCheckbox;
 	private Combo fExtensions;
 	private Button fIsRegExCheckbox;
 	private CLabel fStatusLabel;
 	private Button fSearchDerivedCheckbox;
 
 	private ISearchPageContainer fContainer;
 	private FileTypeEditor fFileTypeEditor;
 
 	private ContentAssistHandler fReplaceContentAssistHandler;
     
     // mstodo remove before release
     private Button fUseNewTextUICheckbox;
     private boolean fUseNewTextUI;
     private static final String STORE_USE_NEW_TEXT_UI = "use-new-text-ui";
 
 	private static class SearchPatternData {
 		public final boolean isCaseSensitive;
 		public final boolean isRegExSearch;
 		public final String textPattern;
 		public final String[] fileNamePatterns;
 		public final int scope;
 		public final IWorkingSet[] workingSets;
 		
 		public SearchPatternData(String textPattern, boolean isCaseSensitive, boolean isRegExSearch, String[] fileNamePatterns, int scope, IWorkingSet[] workingSets) {
 			Assert.isNotNull(fileNamePatterns);
 			this.isCaseSensitive= isCaseSensitive;
 			this.isRegExSearch= isRegExSearch;
 			this.textPattern= textPattern;
 			this.fileNamePatterns= fileNamePatterns;
 			this.scope= scope;
 			this.workingSets= workingSets; // can be null
 		}
 		
 		public void store(IDialogSettings settings) {
 			settings.put("ignoreCase", !isCaseSensitive); //$NON-NLS-1$
 			settings.put("isRegExSearch", isRegExSearch); //$NON-NLS-1$
 			settings.put("textPattern", textPattern); //$NON-NLS-1$
 			settings.put("fileNamePatterns", fileNamePatterns); //$NON-NLS-1$
 			settings.put("scope", scope); //$NON-NLS-1$
 			if (workingSets != null) {
 				String[] wsIds= new String[workingSets.length];
 				for (int i= 0; i < workingSets.length; i++) {
 					wsIds[i]= workingSets[i].getId();
 				}
 				settings.put("workingSets", wsIds); //$NON-NLS-1$
 			} else {
 				settings.put("workingSets", new String[0]); //$NON-NLS-1$
 			}
 
 		}
 		
 		public static SearchPatternData create(IDialogSettings settings) {
 			String textPattern= settings.get("textPattern"); //$NON-NLS-1$
 			String[] wsIds= settings.getArray("workingSets"); //$NON-NLS-1$
 			IWorkingSet[] workingSets= null;
 			if (wsIds != null && wsIds.length > 0) {
 				IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
 				workingSets= new IWorkingSet[wsIds.length];
 				for (int i= 0; workingSets != null && i < wsIds.length; i++) {
 					workingSets[i]= workingSetManager.getWorkingSet(wsIds[i]);
 					if (workingSets[i] == null) {
 						workingSets= null;
 					}
 				}
 			}
 			String[] fileNamePatterns= settings.getArray("fileNamePatterns"); //$NON-NLS-1$
 			if (fileNamePatterns == null) {
 				fileNamePatterns= new String[0];
 			}
 			try {
 				int scope= settings.getInt("scope"); //$NON-NLS-1$
 				boolean isRegExSearch= settings.getBoolean("isRegExSearch"); //$NON-NLS-1$
 				boolean ignoreCase= settings.getBoolean("ignoreCase"); //$NON-NLS-1$
 
 				return	new SearchPatternData(textPattern, !ignoreCase, isRegExSearch, fileNamePatterns, scope, workingSets);
 			} catch (NumberFormatException e) {
 				return null;
 			}
 		}
 
 	}
 	//---- Action Handling ------------------------------------------------
 	
 	public boolean performAction() {
         if (fUseNewTextUI) {
             NewSearchUI.runQueryInBackground(getNewSearchQuery());
         }
         else {
             NewSearchUI.runQueryInBackground(getSearchQuery());
         }
 		return true;
 	}
 	
 	/* (non-Javadoc)
 	 * @see org.eclipse.search.ui.IReplacePage#performReplace()
 	 */
 	public boolean performReplace() {
 		
 		ISearchQuery searchQuery= getSearchQuery();
 		
 		IStatus status= NewSearchUI.runQueryInForeground(getContainer().getRunnableContext(), searchQuery);
 		if (status.matches(IStatus.CANCEL)) {
 			return false;
 		}
 		
 		if (!status.isOK()) {
 			ErrorDialog.openError(getShell(), SearchMessages.TextSearchPage_replace_searchproblems_title, SearchMessages.TextSearchPage_replace_searchproblems_message, status); 
 		}
 		
 		
 		Display.getCurrent().asyncExec(new Runnable() {
 			public void run() {
 				ISearchResultViewPart view= NewSearchUI.activateSearchResultView();
 				if (view != null) {
 					ISearchResultPage page= view.getActivePage();
 					if (page instanceof FileSearchPage) {
 						FileSearchPage filePage= (FileSearchPage) page;
 						Object[] elements= filePage.getInput().getElements();
 						IFile[] files= new IFile[elements.length];
 						System.arraycopy(elements, 0, files, 0, files.length);
 						new ReplaceAction2(filePage, files).run();
 					}
 				}
 			}
 		});
 		return true;
 	}
 
 	private ISearchQuery getSearchQuery() {
 		
 		SearchPatternData patternData= getPatternData();
 	
 		// Setup search scope
 		FileNamePatternSearchScope scope= null;
 		switch (getContainer().getSelectedScope()) {
 			case ISearchPageContainer.WORKSPACE_SCOPE:
 				scope= FileNamePatternSearchScope.newWorkspaceScope(fSearchDerived);
 				break;
 			case ISearchPageContainer.SELECTION_SCOPE:
 				scope= getSelectedResourcesScope();
 				break;
 			case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
 				scope= getEnclosingProjectScope();
 				break;
 			case ISearchPageContainer.WORKING_SET_SCOPE:
 				IWorkingSet[] workingSets= getContainer().getSelectedWorkingSets();
 				String desc= Messages.format(SearchMessages.WorkingSetScope, ScopePart.toString(workingSets)); 
 				scope= FileNamePatternSearchScope.newSearchScope(desc, workingSets, fSearchDerived);
 		}
 		String[] fileExtensions= patternData.fileNamePatterns;
 		for (int i= 0; i < fileExtensions.length; i++) {
 			scope.addFileNamePattern(fileExtensions[i]);
 		}
 		return new FileSearchQuery(scope,  getSearchOptions(), patternData.textPattern);
 	}
 
     private ISearchQuery getNewSearchQuery() {
         // create query
         RetrieverQuery query= new RetrieverQuery(getActivePage());
 
         // extract data
         SearchPatternData patternData= getPatternData();
 
         // check if we have a string matcher pattern.
         String pattern= patternData.textPattern;
         boolean isRegex= patternData.isRegExSearch;
         if (!isRegex) {
             String literal= PatternConstructor.appendAsRegEx(false, pattern, new StringBuffer()).toString();
             String strMatcher= PatternConstructor.appendAsRegEx(true, pattern, new StringBuffer()).toString();
             if (!literal.equals(strMatcher)) {
                 isRegex= true;
                 pattern= strMatcher;
             }
         }
         
         // Setup search scope
         IScopeDescription scope= null;
         switch (getContainer().getSelectedScope()) {
             case ISearchPageContainer.SELECTION_SCOPE:
                 scope= getSelectedResourcesScopeDescription();
                 break;
             case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
                 scope= getEnclosingProjectScopeDescription();
                 break;
             case ISearchPageContainer.WORKING_SET_SCOPE:
                 scope= getWorkingSetScopeDescription();
                 break;
             case ISearchPageContainer.WORKSPACE_SCOPE:
             default:
                 scope= new WorkspaceScopeDescription();
                 break;
         }
 
         query.setSearchString(pattern);
         query.setIsCaseSensitive(patternData.isCaseSensitive);
         query.setIsRegularExpression(isRegex);
 
         query.setFilePatterns(patternData.fileNamePatterns);
         query.setSearchScope(scope, fSearchDerived);
         return query;
     }
 
     private IWorkbenchPage getActivePage() {
         return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
     }
 
 	private String getPattern() {
 		return fPattern.getText();
 	}
 	
 	private SearchPatternData findInPrevious(String pattern) {
 		for (Iterator iter= fPreviousSearchPatterns.iterator(); iter.hasNext();) {
 			SearchPatternData element= (SearchPatternData) iter.next();
 			if (pattern.equals(element.textPattern)) {
 				return element;
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Return search pattern data and update previous searches.
 	 * An existing entry will be updated.
 	 * @return the search pattern data
 	 */
 	private SearchPatternData getPatternData() {
 		SearchPatternData match= findInPrevious(fPattern.getText());
 		if (match != null) {
 			fPreviousSearchPatterns.remove(match);
 		}
 		match= new SearchPatternData(
 					getPattern(),
 					isCaseSensitive(),
 					fIsRegExCheckbox.getSelection(),
 					getExtensions(),
 					getContainer().getSelectedScope(),
 					getContainer().getSelectedWorkingSets());
 		fPreviousSearchPatterns.add(0, match);
 		return match;
 	}
 
 	private String[] getPreviousExtensions() {
 		List extensions= new ArrayList(fPreviousSearchPatterns.size());
 		int size= fPreviousSearchPatterns.size();
 		for (int i= 0; i < size; i++) {
 			SearchPatternData data= (SearchPatternData) fPreviousSearchPatterns.get(i);
 			String text= FileTypeEditor.typesToString(data.fileNamePatterns);
 			if (!extensions.contains(text))
 				extensions.add(text);
 		}
 		return (String[]) extensions.toArray(new String[extensions.size()]);
 	}
 
 	private String[] getPreviousSearchPatterns() {
 		int size= fPreviousSearchPatterns.size();
 		String [] patterns= new String[size];
 		for (int i= 0; i < size; i++)
 			patterns[i]= ((SearchPatternData) fPreviousSearchPatterns.get(i)).textPattern;
 		return patterns;
 	}
 	
 	private String getSearchOptions() {
 		StringBuffer result= new StringBuffer();
 		if (!isCaseSensitive())
 			result.append('i');
 
 		if (fIsRegExSearch)
 			result.append('r');
 
 		return result.toString();	
 	}
 	
 	private String[] getExtensions() {
 		return fFileTypeEditor.getFileTypes();
 	}
 
 	private boolean isCaseSensitive() {
 		return fIsCaseSensitiveCheckbox.getSelection();
 	}
 
 	/*
 	 * Implements method from IDialogPage
 	 */
 	public void setVisible(boolean visible) {
 		if (visible && fPattern != null) {
 			if (fFirstTime) {
 				fFirstTime= false;
 				// Set item and text here to prevent page from resizing
 				fPattern.setItems(getPreviousSearchPatterns());
 				fExtensions.setItems(getPreviousExtensions());
				if (fExtensions.getItemCount() == 0) {
					loadFilePatternDefaults();
				}
 				if (!initializePatternControl()) {
 					fPattern.select(0);
 					fExtensions.setText("*"); //$NON-NLS-1$
 					handleWidgetSelected();
 				}
 			}
 			fPattern.setFocus();
 		}
 		updateOKStatus();
 		super.setVisible(visible);
 	}
 	
 	final void updateOKStatus() {
 		boolean regexStatus= validateRegex();
 		boolean hasFilePattern= fExtensions.getText().length() > 0;
 		getContainer().setPerformActionEnabled(regexStatus && hasFilePattern);
 	}
 
 	//---- Widget creation ------------------------------------------------
 
 	public void createControl(Composite parent) {
 		initializeDialogUnits(parent);
 		readConfiguration();
 		
 		Composite result= new Composite(parent, SWT.NONE);
 		result.setFont(parent.getFont());
 		GridLayout layout= new GridLayout(2, false);
 		result.setLayout(layout);
 		
 		addTextPatternControls(result);
 		
 		Label separator= new Label(result, SWT.NONE);
 		separator.setVisible(false);
 		GridData data= new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
 		data.heightHint= convertHeightInCharsToPixels(1) / 3;
 		separator.setLayoutData(data);
 		
 		addFileNameControls(result);
 
 		setControl(result);
 		Dialog.applyDialogFont(result);
 		PlatformUI.getWorkbench().getHelpSystem().setHelp(result, ISearchHelpContextIds.TEXT_SEARCH_PAGE);
 }
 	
 	private boolean validateRegex() {
 		if (fIsRegExCheckbox.getSelection()) {
 			try {
 				Pattern.compile(fPattern.getText());
 			} catch (PatternSyntaxException e) {
 				String locMessage= e.getLocalizedMessage();
 				int i= 0;
 				while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
 					i++;
 				}
 				statusMessage(true, locMessage.substring(0, i)); // only take first line
 				return false;
 			}
 			statusMessage(false, ""); //$NON-NLS-1$
 		} else {
 			statusMessage(false, SearchMessages.SearchPage_containingText_hint); 
 		}
 		return true;
 	}
 
 	private void addTextPatternControls(Composite group) {
 		// grid layout with 2 columns
 
 		// Info text		
 		Label label= new Label(group, SWT.LEAD);
 		label.setText(SearchMessages.SearchPage_containingText_text); 
 		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
 		label.setFont(group.getFont());
 
 		// Pattern combo
 		fPattern= new Combo(group, SWT.SINGLE | SWT.BORDER);
 		// Not done here to prevent page from resizing
 		// fPattern.setItems(getPreviousSearchPatterns());
 		fPattern.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				handleWidgetSelected();
 				updateOKStatus();
 			}
 		});
 		// add some listeners for regex syntax checking
 		fPattern.addModifyListener(new ModifyListener() {
 			public void modifyText(ModifyEvent e) {
 				updateOKStatus();
 			}
 		});
 		fPattern.setFont(group.getFont());
 		GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
 		data.widthHint= convertWidthInCharsToPixels(50);
 		fPattern.setLayoutData(data);
 		
 		fIsCaseSensitiveCheckbox= new Button(group, SWT.CHECK);
 		fIsCaseSensitiveCheckbox.setText(SearchMessages.SearchPage_caseSensitive); 
 		fIsCaseSensitiveCheckbox.setSelection(!fIsCaseSensitive);
 		fIsCaseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				fIsCaseSensitive= fIsCaseSensitiveCheckbox.getSelection();
 			}
 		});
 		fIsCaseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
 		fIsCaseSensitiveCheckbox.setFont(group.getFont());
 
 		// Text line which explains the special characters
 		fStatusLabel= new CLabel(group, SWT.LEAD);
 		fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
 		fStatusLabel.setFont(group.getFont());
 		fStatusLabel.setAlignment(SWT.LEFT);
 		fStatusLabel.setText(SearchMessages.SearchPage_containingText_hint); 
 
 		// RegEx checkbox
 		fIsRegExCheckbox= new Button(group, SWT.CHECK);
 		fIsRegExCheckbox.setText(SearchMessages.SearchPage_regularExpression); 
 		fIsRegExCheckbox.setSelection(fIsRegExSearch);
 		setContentAssistsEnablement(fIsRegExSearch);
 		fIsRegExCheckbox.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				fIsRegExSearch= fIsRegExCheckbox.getSelection();
 				updateOKStatus();
 
 				writeConfiguration();
 				setContentAssistsEnablement(fIsRegExSearch);
 			}
 		});
 		fIsRegExCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
 		fIsRegExCheckbox.setFont(group.getFont());
 	}
 
 	private void handleWidgetSelected() {
 		int selectionIndex= fPattern.getSelectionIndex();
 		if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size())
 			return;
 		
 		SearchPatternData patternData= (SearchPatternData) fPreviousSearchPatterns.get(selectionIndex);
 		if (!fPattern.getText().equals(patternData.textPattern))
 			return;
 		fIsCaseSensitiveCheckbox.setSelection(patternData.isCaseSensitive);
 		fIsRegExCheckbox.setSelection(patternData.isRegExSearch);
 		fPattern.setText(patternData.textPattern);
 		fFileTypeEditor.setFileTypes(patternData.fileNamePatterns);
 		if (patternData.workingSets != null)
 			getContainer().setSelectedWorkingSets(patternData.workingSets);
 		else
 			getContainer().setSelectedScope(patternData.scope);
 	}
 
 	private boolean initializePatternControl() {
 		ISelection selection= getSelection();
 		if (selection instanceof ITextSelection && !selection.isEmpty()) {
 			String text= ((ITextSelection) selection).getText();
 			if (text != null) {
 				fPattern.setText(insertEscapeChars(text));
 				
 				if (getPreviousExtensions().length > 0) {
 					fExtensions.setText(getPreviousExtensions()[0]);
 				} else {
 					String extension= getExtensionFromEditor();
 					if (extension != null)
 						fExtensions.setText(extension);
 					else 
 						fExtensions.setText("*"); //$NON-NLS-1$
 				}
 				return true;
 			}
 		}
 		return false;
 	}
 	
	private void loadFilePatternDefaults() {
		SearchMatchInformationProviderRegistry registry= SearchPlugin.getDefault().getSearchMatchInformationProviderRegistry();
		String[] defaults= registry.getDefaultFilePatterns();
		fExtensions.setItems(defaults);
		fExtensions.setText(defaults[0]);
	}

 	private String insertEscapeChars(String text) {
 		if (text == null || text.equals("")) //$NON-NLS-1$
 			return ""; //$NON-NLS-1$
 		StringBuffer sbIn= new StringBuffer(text);
 		BufferedReader reader= new BufferedReader(new StringReader(text));
 		int lengthOfFirstLine= 0;
 		try {
 			lengthOfFirstLine= reader.readLine().length();
 		} catch (IOException ex) {
 			return ""; //$NON-NLS-1$
 		}
 		StringBuffer sbOut= new StringBuffer(lengthOfFirstLine + 5);
 		int i= 0;
 		while (i < lengthOfFirstLine) {
 			char ch= sbIn.charAt(i);
 			if (ch == '*' || ch == '?' || ch == '\\')
 				sbOut.append("\\"); //$NON-NLS-1$
 			sbOut.append(ch);
 			i++;
 		}
 		return sbOut.toString();
 	}
 
 	private String getExtensionFromEditor() {
 		IEditorPart ep= SearchPlugin.getActivePage().getActiveEditor();
 		if (ep != null) {
 			Object elem= ep.getEditorInput();
 			if (elem instanceof IFileEditorInput) {
 				String extension= ((IFileEditorInput)elem).getFile().getFileExtension();
 				if (extension == null)
 					return ((IFileEditorInput)elem).getFile().getName();
 				return "*." + extension; //$NON-NLS-1$
 			}
 		}
 		return null;
 	}
 
 	private void addFileNameControls(Composite group) {
 		// grid layout with 2 columns
 		
 		// Line with label, combo and button
 		Label label= new Label(group, SWT.LEAD);
 		label.setText(SearchMessages.SearchPage_fileNamePatterns_text); 
 		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
 		label.setFont(group.getFont());
 		
 		fExtensions= new Combo(group, SWT.SINGLE | SWT.BORDER);
 		fExtensions.addModifyListener(new ModifyListener() {
 			public void modifyText(ModifyEvent e) {
 				updateOKStatus();
 			}
 		});
 		GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
 		data.widthHint= convertWidthInCharsToPixels(50);
 		fExtensions.setLayoutData(data);
 		fExtensions.setFont(group.getFont());
 		
 		Button button= new Button(group, SWT.PUSH);
 		button.setText(SearchMessages.SearchPage_browse); 
 		GridData gridData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
 		gridData.widthHint= SWTUtil.getButtonWidthHint(button);	
 		button.setLayoutData(gridData);
 		button.setFont(group.getFont());
 		
 		IEditorRegistry editorRegistry= SearchPlugin.getDefault().getWorkbench().getEditorRegistry();
 		fFileTypeEditor= new FileTypeEditor(editorRegistry, fExtensions, button);
 		
 		// Text line which explains the special characters
 		Label description= new Label(group, SWT.LEAD);
 		description.setText(SearchMessages.SearchPage_fileNamePatterns_hint); 
 		description.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
 		description.setFont(group.getFont());
 		
 		fSearchDerivedCheckbox= new Button(group, SWT.CHECK);
 		fSearchDerivedCheckbox.setText(SearchMessages.TextSearchPage_searchDerived_label); 
 		
 		fSearchDerivedCheckbox.setSelection(fSearchDerived);
 		fSearchDerivedCheckbox.addSelectionListener(new SelectionAdapter() {
 			public void widgetSelected(SelectionEvent e) {
 				fSearchDerived= fSearchDerivedCheckbox.getSelection();
 				writeConfiguration();
 			}
 		});
 		fSearchDerivedCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
 		fSearchDerivedCheckbox.setFont(group.getFont());
         
         fUseNewTextUICheckbox= new Button(group, SWT.CHECK);
         fUseNewTextUICheckbox.setText("Work in progress: show search results in new Text Search UI"); 
         fUseNewTextUICheckbox.setSelection(fUseNewTextUI);
         fUseNewTextUICheckbox.addSelectionListener(new SelectionAdapter() {
             public void widgetSelected(SelectionEvent e) {
                 fUseNewTextUI= fUseNewTextUICheckbox.getSelection();
                 writeConfiguration();
             }
         });
         fUseNewTextUICheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
         fUseNewTextUICheckbox.setFont(group.getFont());
 	}
 
 	/**
 	 * Sets the search page's container.
 	 * @param container the container to set
 	 */
 	public void setContainer(ISearchPageContainer container) {
 		fContainer= container;
 	}
 	
 	private ISearchPageContainer getContainer() {
 		return fContainer;
 	}
 	
 	private ISelection getSelection() {
 		return fContainer.getSelection();
 	}
 	
 	private FileNamePatternSearchScope getEnclosingProjectScope() {
 		String[] enclosingProjectName= getContainer().getSelectedProjectNames();
 		if (enclosingProjectName == null) {
 			return FileNamePatternSearchScope.newWorkspaceScope(fSearchDerived);
 		}
 		
 		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
 		IResource[] res= new IResource[enclosingProjectName.length];
 		for (int i= 0; i < res.length; i++) {
 			res[i]= root.getProject(enclosingProjectName[i]);
 		}
 		String name;
 		int elementCount= res.length;
 		if (elementCount > 1)
 			name= Messages.format(SearchMessages.EnclosingProjectsScope, enclosingProjectName[0]); 
 		else if (elementCount == 1)
 			name= Messages.format(SearchMessages.EnclosingProjectScope, enclosingProjectName[0]); 
 		else 
 			name= Messages.format(SearchMessages.EnclosingProjectScope, "");  //$NON-NLS-1$
 		
 		return FileNamePatternSearchScope.newSearchScope(name, res, fSearchDerived);
 	}
 	
     private IScopeDescription getEnclosingProjectScopeDescription() {
         String[] enclosingProjectName= getContainer().getSelectedProjectNames();
         if (enclosingProjectName == null) {
             return new WorkspaceScopeDescription();
         }
         
         IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
         IResource[] res= new IResource[enclosingProjectName.length];
         for (int i= 0; i < res.length; i++) {
             res[i]= root.getProject(enclosingProjectName[i]);
         }
         if (res.length == 0) {
             return new SelectedResourcesScopeDescription();
         }
         return getScopeDescription(res);
     }
     
     private IScopeDescription getScopeDescription(IResource[] res) {
     	return new SelectedResourcesScopeDescription(res, false);
     }
     
     private IScopeDescription getWorkingSetScopeDescription() {
         IWorkingSet[] wss= getContainer().getSelectedWorkingSets();
         if (wss.length == 0) {
             return new WorkspaceScopeDescription();
         }
         if (wss.length == 1) {
             if (wss[0].equals(getActivePage().getAggregateWorkingSet())) {
                 return new WindowWorkingSetScopeDescription();
             }
         }
        return new WorkingSetScopeDescription(wss);
     }
 
 	private FileNamePatternSearchScope getSelectedResourcesScope() {
 		HashSet resources= new HashSet();
 		ISelection selection= getSelection();
 		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
 			Iterator iter= ((IStructuredSelection) selection).iterator();
 			while (iter.hasNext()) {
 				Object curr= iter.next();
 				if (curr instanceof IWorkingSet) {
 					IWorkingSet workingSet= (IWorkingSet) curr;
 					if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
 						return FileNamePatternSearchScope.newWorkspaceScope(fSearchDerived);
 					}
 					IAdaptable[] elements= workingSet.getElements();
 					for (int i= 0; i < elements.length; i++) {
 						IResource resource= (IResource)elements[i].getAdapter(IResource.class);
 						if (resource != null && resource.isAccessible()) {
 							resources.add(resource);
 						}
 					}
 				} else if (curr instanceof IAdaptable) {
 					IResource resource= (IResource) ((IAdaptable)curr).getAdapter(IResource.class);
 					if (resource != null && resource.isAccessible()) {
 						resources.add(resource);
 					}
 				}
 			}
 		}
 		IResource[] arr= (IResource[]) resources.toArray(new IResource[resources.size()]);
 		return FileNamePatternSearchScope.newSearchScope(SearchMessages.SelectionScope, arr, fSearchDerived);
 	}
 
     private IScopeDescription getSelectedResourcesScopeDescription() {
         HashSet resources= new HashSet();
         ISelection selection= getSelection();
         if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
             Iterator iter= ((IStructuredSelection) selection).iterator();
             while (iter.hasNext()) {
                 Object curr= iter.next();
                 if (curr instanceof IWorkingSet) {
                     IWorkingSet workingSet= (IWorkingSet) curr;
                     if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
                         return new WorkspaceScopeDescription();
                     }
                     IAdaptable[] elements= workingSet.getElements();
                     for (int i= 0; i < elements.length; i++) {
                         IResource resource= (IResource)elements[i].getAdapter(IResource.class);
                         if (resource != null && resource.isAccessible()) {
                             resources.add(resource);
                         }
                     }
                 } else if (curr instanceof IAdaptable) {
                     IResource resource= (IResource) ((IAdaptable)curr).getAdapter(IResource.class);
                     if (resource != null && resource.isAccessible()) {
                         resources.add(resource);
                     }
                 }
             }
         }
         return getScopeDescription((IResource[]) resources.toArray(new IResource[resources.size()]));
     }
 
 
 	//--------------- Configuration handling --------------
 	
     /* (non-Javadoc)
 	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
 	 */
 	public void dispose() {
 		writeConfiguration();
 		super.dispose();
 	}
 	
 	/**
 	 * Returns the page settings for this Text search page.
 	 * 
 	 * @return the page settings to be used
 	 */
 	private IDialogSettings getDialogSettings() {
 		return SearchPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
 	}
 		
 	
 	/**
 	 * Initializes itself from the stored page settings.
 	 */
 	private void readConfiguration() {
 		IDialogSettings s= getDialogSettings();
 		fIsCaseSensitive= s.getBoolean(STORE_CASE_SENSITIVE);
 		fIsRegExSearch= s.getBoolean(STORE_IS_REG_EX_SEARCH);
 		fSearchDerived= s.getBoolean(STORE_SEARCH_DERIVED);
         fUseNewTextUI= s.getBoolean(STORE_USE_NEW_TEXT_UI);
 		
 		try {
 			int historySize= s.getInt(STORE_HISTORY_SIZE);
 			for (int i= 0; i < historySize; i++) {
 				IDialogSettings histSettings= s.getSection(STORE_HISTORY + i);
 				if (histSettings != null) {
 					SearchPatternData data= SearchPatternData.create(histSettings);
 					if (data != null) {
 						fPreviousSearchPatterns.add(data);
 					}
 				}
 			}
 		} catch (NumberFormatException e) {
 			// ignore
 		}
 	}
 	
 	/**
 	 * Stores it current configuration in the dialog store.
 	 */
 	private void writeConfiguration() {
 		IDialogSettings s= getDialogSettings();
 		s.put(STORE_CASE_SENSITIVE, fIsCaseSensitive);
 		s.put(STORE_IS_REG_EX_SEARCH, fIsRegExSearch);
 		s.put(STORE_SEARCH_DERIVED, fSearchDerived);
         s.put(STORE_USE_NEW_TEXT_UI, fUseNewTextUI);
 		
 		int historySize= Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
 		s.put(STORE_HISTORY_SIZE, historySize);
 		for (int i= 0; i < historySize; i++) {
 			IDialogSettings histSettings= s.addNewSection(STORE_HISTORY + i);
 			SearchPatternData data= ((SearchPatternData) fPreviousSearchPatterns.get(i));
 			data.store(histSettings);
 		}
 	}
 	
 	private void setContentAssistsEnablement(boolean enable) {
 		if (enable) {
 			if (fReplaceContentAssistHandler == null) {
 				fReplaceContentAssistHandler= ContentAssistHandler.createHandlerForCombo(fPattern, ReplaceDialog2.createContentAssistant(true));
 			}
 			fReplaceContentAssistHandler.setEnabled(true);
 		} else {
 			if (fReplaceContentAssistHandler == null)
 				return;
 			fReplaceContentAssistHandler.setEnabled(false);
 		}
 	}
 
 	private void statusMessage(boolean error, String message) {
 		fStatusLabel.setText(message);
 		if (error)
 			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
 		else
 			fStatusLabel.setForeground(null);
 	}
 
 }	
