 /*******************************************************************************
  * Copyright (c) 2003 - 2005 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 package org.eclipse.mylar.bugzilla.ui.editor;
 
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Iterator;
 import java.util.StringTokenizer;
 
 import javax.security.auth.login.LoginException;
 
 import org.eclipse.compare.CompareConfiguration;
 import org.eclipse.compare.CompareUI;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.runtime.jobs.Job;
 import org.eclipse.jface.action.GroupMarker;
 import org.eclipse.jface.action.IMenuListener;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.action.Separator;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.resource.JFaceColors;
 import org.eclipse.jface.viewers.SelectionChangedEvent;
 import org.eclipse.jface.viewers.StructuredSelection;
 import org.eclipse.mylar.bugzilla.core.Attribute;
 import org.eclipse.mylar.bugzilla.core.BugPost;
 import org.eclipse.mylar.bugzilla.core.BugReport;
 import org.eclipse.mylar.bugzilla.core.BugzillaException;
 import org.eclipse.mylar.bugzilla.core.BugzillaPlugin;
 import org.eclipse.mylar.bugzilla.core.BugzillaRepository;
 import org.eclipse.mylar.bugzilla.core.Comment;
 import org.eclipse.mylar.bugzilla.core.IBugzillaBug;
 import org.eclipse.mylar.bugzilla.core.IBugzillaConstants;
 import org.eclipse.mylar.bugzilla.core.Operation;
 import org.eclipse.mylar.bugzilla.core.compare.BugzillaCompareInput;
 import org.eclipse.mylar.bugzilla.ui.OfflineView;
 import org.eclipse.mylar.bugzilla.ui.actions.RefreshBugzillaReportsAction;
 import org.eclipse.mylar.bugzilla.ui.favorites.actions.AddToFavoritesAction;
 import org.eclipse.mylar.bugzilla.ui.outline.BugzillaOutlineNode;
 import org.eclipse.mylar.bugzilla.ui.outline.BugzillaReportSelection;
 import org.eclipse.mylar.core.MylarPlugin;
 import org.eclipse.mylar.tasks.ui.views.TaskListView;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.StyledText;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.List;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.ui.IEditorInput;
 import org.eclipse.ui.IEditorSite;
 import org.eclipse.ui.ISharedImages;
 import org.eclipse.ui.IWorkbenchActionConstants;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.actions.WorkspaceModifyOperation;
 import org.eclipse.ui.internal.Workbench;
 
 
 /**
  * An editor used to view a bug report that exists on a server. It uses a
  * <code>BugReport</code> object to store the data.
  */
 public class ExistingBugEditor extends AbstractBugEditor
 {
 
 	protected BugzillaCompareInput compareInput;
 	protected Button compareButton;
 	protected Button[] radios;
 	protected Combo[] radioOptions;
 	protected List keyWordsList;
 	protected Text keywordsText;
 	protected Text addCommentsText;
 	protected BugReport bug;
 
 
     public String getNewCommentText(){
         return addCommentsTextBox.getText();
     }
     
 	/**
 	 * Creates a new <code>ExistingBugEditor</code>.
 	 */
 	public ExistingBugEditor() {
 		super();
 		
 		// Set up the input for comparing the bug report to the server
 		CompareConfiguration config = new CompareConfiguration();
 		config.setLeftEditable(false);
 		config.setRightEditable(false);
 		config.setLeftLabel("Local Bug Report");
 		config.setRightLabel("Remote Bug Report");
 		config.setLeftImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT));
 		config.setRightImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT));
 		compareInput = new BugzillaCompareInput(config);
 	}
 
 	@Override
 	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
 		if (!(input instanceof ExistingBugEditorInput))
 			throw new PartInitException("Invalid Input: Must be ExistingBugEditorInput");
 		ExistingBugEditorInput ei = (ExistingBugEditorInput) input;
 		setSite(site);
 		setInput(input);
 		bugzillaInput = ei;
 		model = BugzillaOutlineNode.parseBugReport(bugzillaInput.getBug());
 		bug = ei.getBug();
 		restoreBug();
 		isDirty = false;
 		updateEditorTitle();
 	}
 
 	/**
 	 * This overrides the existing implementation in order to add
 	 * an "add to favorites" option to the context menu.
 	 *  
 	 * @see org.eclipse.mylar.bugzilla.ui.AbstractBugEditor#createContextMenu()
 	 */
 	@Override
 	protected void createContextMenu() {
 		contextMenuManager = new MenuManager("#BugEditor");
 		contextMenuManager.setRemoveAllWhenShown(true);
 		contextMenuManager.addMenuListener(new IMenuListener() {
 			public void menuAboutToShow(IMenuManager manager) {
 				manager.add(new AddToFavoritesAction(ExistingBugEditor.this));
 				manager.add(new Separator());
 				manager.add(cutAction);
 				manager.add(copyAction);
 				manager.add(pasteAction);
 				manager.add(new Separator());
 				manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
 				if (currentSelectedText == null || 
 					currentSelectedText.getSelectionText().length() == 0) {
 				
 					copyAction.setEnabled(false);
 				}
 				else {
 					copyAction.setEnabled(true);
 				}
 			}
 		});
 		getSite().registerContextMenu("#BugEditor", contextMenuManager,
 				getSite().getSelectionProvider());
 	}
 
 	@Override
 	protected void addRadioButtons(Composite buttonComposite) {
 		int i = 0;
 		Button selected = null;
 		radios = new Button[bug.getOperations().size()];
 		radioOptions = new Combo[bug.getAttributes().size()];
 		for (Iterator<Operation> it = bug.getOperations().iterator(); it.hasNext(); ) {
 			Operation o = it.next();
 			radios[i] = new Button(buttonComposite, SWT.RADIO);
 			radios[i].setFont(TEXT_FONT);
 			GridData radioData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
 			if (!o.hasOptions())
 				radioData.horizontalSpan = 4;
 			else
 				radioData.horizontalSpan = 3;
 			radioData.heightHint = 20;
 			String opName = o.getOperationName();
 			opName = opName.replaceAll("</.*>", "");
 			opName = opName.replaceAll("<.*>", "");
 			radios[i].setText(opName);
 			radios[i].setLayoutData(radioData);
 			radios[i].setBackground(background);
 			radios[i].addSelectionListener(new RadioButtonListener());
 			radios[i].addListener(SWT.FocusIn, new GenericListener());
 			
 			if (o.hasOptions()) {
 				radioData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
 				radioData.horizontalSpan = 1;
 				radioData.heightHint = 20;
 				radioData.widthHint = 80;
 				radioOptions[i] = new Combo(
 						buttonComposite,
 						SWT.NO_BACKGROUND
 							| SWT.MULTI
 							| SWT.V_SCROLL
 							| SWT.READ_ONLY);
 				radioOptions[i].setFont(TEXT_FONT);
 				radioOptions[i].setLayoutData(radioData);
 				radioOptions[i].setBackground(background);
 				
 				Object [] a = o.getOptionNames().toArray();
 				Arrays.sort(a);
 				for (int j = 0; j < a.length; j++) {
 					radioOptions[i].add((String) a[j]);
 				}
 				radioOptions[i].select(0);
 				radioOptions[i].addSelectionListener(new RadioButtonListener());
 			}
 			
 			if (i == 0 || o.isChecked()) {
 				if(selected != null)
 					selected.setSelection(false);
 				selected = radios[i];
 				radios[i].setSelection(true);
 				if(o.hasOptions() && o.getOptionSelection() != null){
 					int j = 0;
 					for(String s: radioOptions[i].getItems()){
 						if(s.compareTo(o.getOptionSelection()) == 0){
 							radioOptions[i].select(j);
 						}
 						j++;
 					}
 				}
 				bug.setSelectedOperation(o);
 			}
 			
 			i++;
 		}
 	}
 
 	@Override
 	protected void addActionButtons(Composite buttonComposite) {
 		super.addActionButtons(buttonComposite);
 
 		compareButton = new Button(buttonComposite, SWT.NONE);
 		compareButton.setFont(TEXT_FONT);
 		GridData compareButtonData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
 		compareButtonData.widthHint = 100;
 		compareButtonData.heightHint = 20;
 		compareButton.setText("Compare");
 		compareButton.setLayoutData(compareButtonData);
 		compareButton.addListener(SWT.Selection, new Listener() {
 			public void handleEvent(Event e) {
 				OpenCompareEditorJob compareJob = new OpenCompareEditorJob("Comparing bug with remote server...");
 				compareJob.schedule();
 			}
 		});
 		compareButton.addListener(SWT.FocusIn, new GenericListener());
 	}
 
 	/**
 	 * @return Returns the compareInput.
 	 */
 	public BugzillaCompareInput getCompareInput() {
 		return compareInput;
 	}
 	
 	@Override
 	public IBugzillaBug getBug() {
 		return bug;
 	}
 
 	@Override
 	protected String getTitleString() {
 		return bug.getLabel() + ": " + checkText(bug.getAttribute("Summary").getNewValue());
 	}
 
 	@Override
 	protected void submitBug() {
 		final BugPost form = new BugPost();
 		
 		// set the url for the bug to be submitted to
 		setURL(form, "process_bug.cgi");
 
 		// go through all of the attributes and add them to the bug post
 		for (Iterator<Attribute> it = bug.getAttributes().iterator(); it.hasNext(); ) {
 			Attribute a = it.next();
 			if (a != null && a.getParameterName() != null && a.getParameterName().compareTo("") != 0 && !a.isHidden()) {
 				String value = a.getNewValue();
 				
 				// add the attribute to the bug post
 				form.add(a.getParameterName(), checkText(value));
 			}
 			else if(a != null && a.getParameterName() != null && a.getParameterName().compareTo("") != 0 && a.isHidden()) {
 				// we have a hidden attribute and we should send it back.
 				form.add(a.getParameterName(), a.getValue());
 			}
 		}
 		
 		// make sure that the comment is broken up into 80 character lines
 		bug.setNewNewComment(formatText(bug.getNewNewComment()));
 				
 		// add the summary to the bug post
 		form.add("short_desc", bug.getAttribute("Summary").getNewValue());
 
 		// add the operation to the bug post
 		Operation o = bug.getSelectedOperation();
 		if (o == null)
 			form.add("knob", "none");
 		else {
 			form.add("knob", o.getKnobName());
 			if(o.hasOptions()) {
 				String sel = o.getOptionValue(o.getOptionSelection());
 				form.add(o.getOptionName(), sel);
 			}
 		}
 		form.add("form_name", "process_bug");
 		
 
 		// add the new comment to the bug post if there is some text in it
 		if(bug.getNewNewComment().length() != 0) {
 			form.add("comment", bug.getNewNewComment());
 		}
 		
 		final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
 			protected void execute(final IProgressMonitor monitor) throws CoreException {
 				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable(){
 					public void run() {
 						try {
 							form.post();
 							
 							// TODO what do we do if the editor is closed
 							if(ExistingBugEditor.this != null && !ExistingBugEditor.this.isDisposed()){
 								changeDirtyStatus(false);
 								BugzillaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(ExistingBugEditor.this, true);
 							}
 							OfflineView.removeReport(bug);
 						} catch (BugzillaException e) {
 						    BugzillaPlugin.getDefault().logAndShowExceptionDetailsDialog(e, "occurred while posting the bug.", "I/O Error");
 						}
 						catch (LoginException e) {
 							MessageDialog.openError(null, "Login Error",
 									"Bugzilla could not post your bug since your login name or password is incorrect.\nPlease check your settings in the bugzilla preferences. ");
 						}	
 					}
 					
 				});
 			}
 		};
 		
 		Job job = new Job("Submitting Bug"){
 
 			@Override
 			protected IStatus run(IProgressMonitor monitor) {
 				try{
 					op.run(monitor);
 				} catch (Exception e){
 					MylarPlugin.log(e, "Failed to submit bug");
 					return new Status(Status.ERROR, "org.eclipse.mylar.bugzilla.ui", Status.ERROR, "Failed to submit bug", e);
 				}
 				
 				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable(){
 					public void run() {
 						if(TaskListView.getDefault() != null && 
 								TaskListView.getDefault().getViewer() != null){
 							new RefreshBugzillaReportsAction().run();
 						}
 					}
 				});
 				return Status.OK_STATUS;
 			}
 			
 		};
 		
 		job.schedule();
 	}
 
 	@Override
 	protected void createDescriptionLayout() {
 		// Description Area
 		Composite descriptionComposite = new Composite(infoArea, SWT.NONE);
 		GridLayout descriptionLayout = new GridLayout();
 		descriptionLayout.numColumns = 4;
 		descriptionComposite.setLayout(descriptionLayout);
 		descriptionComposite.setBackground(background);
 		GridData descriptionData = new GridData(GridData.FILL_BOTH);
 		descriptionData.horizontalSpan = 1;
 		descriptionData.grabExcessVerticalSpace = false;
 		descriptionComposite.setLayoutData(descriptionData);
 		// End Description Area
 		
 		StyledText t = newLayout(descriptionComposite, 4, "Description:", HEADER);
 		t.addListener(SWT.FocusIn, new DescriptionListener());
 		t = newLayout(descriptionComposite, 4, bug.getDescription(), VALUE);
 		t.setFont(COMMENT_FONT);
 		t.addListener(SWT.FocusIn, new DescriptionListener());
         
         texts.add(textsindex, t);
         textHash.put(bug.getDescription(), t);
         textsindex++; 
         
 	}
 
 	@Override
 	protected void createCommentLayout() {
 			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
 			
 			// Additional (read-only) Comments Area
 			Composite addCommentsComposite = new Composite(infoArea, SWT.NONE);
 			GridLayout addCommentsLayout = new GridLayout();
 			addCommentsLayout.numColumns = 4;
 			addCommentsComposite.setLayout(addCommentsLayout);
 			addCommentsComposite.setBackground(background);
 			GridData addCommentsData = new GridData(GridData.FILL_BOTH);
 			addCommentsData.horizontalSpan = 1;
 			addCommentsData.grabExcessVerticalSpace = false;
 			addCommentsComposite.setLayoutData(addCommentsData);
 			//	End Additional (read-only) Comments Area
 			
             StyledText t = null;
 			for (Iterator<Comment> it = bug.getComments().iterator(); it.hasNext(); ) {
 				Comment comment = it.next();
 				String commentHeader = "Additional comment #" + comment.getNumber() + " from "
 						+ comment.getAuthorName() + " on "
 						+ df.format(comment.getCreated());
 				t = newLayout(addCommentsComposite, 4, commentHeader, HEADER);
 				t.addListener(SWT.FocusIn, new CommentListener(comment));
 				t = newLayout(addCommentsComposite, 4, comment.getText(), VALUE);
 				t.setFont(COMMENT_FONT);
 				t.addListener(SWT.FocusIn, new CommentListener(comment));
                                 
                 //code for outline
                 texts.add(textsindex, t);
                 textHash.put(comment, t);
                 textsindex++;
 			}
 	
 			// Additional Comments Text
 			Composite addCommentsTitleComposite =
 				new Composite(addCommentsComposite, SWT.NONE);
 			GridLayout addCommentsTitleLayout = new GridLayout();
 			addCommentsTitleLayout.horizontalSpacing = 0;
 			addCommentsTitleLayout.marginWidth = 0;
 			addCommentsTitleComposite.setLayout(addCommentsTitleLayout);
 			addCommentsTitleComposite.setBackground(background);
 			GridData addCommentsTitleData =
 				new GridData(GridData.HORIZONTAL_ALIGN_FILL);
 			addCommentsTitleData.horizontalSpan = 4;
 			addCommentsTitleData.grabExcessVerticalSpace = false;
 			addCommentsTitleComposite.setLayoutData(addCommentsTitleData);
 			newLayout(
 				addCommentsTitleComposite,
 				4,
 				"Additional Comments:",
 				HEADER).addListener(SWT.FocusIn, new NewCommentListener());
 			addCommentsText =
 				new Text(
 					addCommentsComposite,
 					SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
 			addCommentsText.setFont(COMMENT_FONT);
 			GridData addCommentsTextData =
 				new GridData(GridData.HORIZONTAL_ALIGN_FILL);
 			addCommentsTextData.horizontalSpan = 4;
 			addCommentsTextData.widthHint = DESCRIPTION_WIDTH;
 			addCommentsTextData.heightHint = DESCRIPTION_HEIGHT;
 			
 			addCommentsText.setLayoutData(addCommentsTextData);
 			addCommentsText.setText(bug.getNewComment());
 			addCommentsText.addListener(SWT.KeyUp, new Listener() {
 				
 				public void handleEvent(Event event) {
 					String sel = addCommentsText.getText();
 					if (!(bug.getNewNewComment().equals(sel))) {
 						bug.setNewNewComment(sel);
 						changeDirtyStatus(true);
 					}
					validateInput();
 				}
 			});
 			addCommentsText.addListener(SWT.FocusIn, new NewCommentListener());
 			// End Additional Comments Text
 	
             addCommentsTextBox = addCommentsText;
             
 			this.createSeparatorSpace(addCommentsComposite);
 		}
 	
 	@Override
 	protected void addKeywordsList(String keywords, Composite attributesComposite) {
 		newLayout(attributesComposite, 1, "Keywords:", PROPERTY);
 		keywordsText = new Text(attributesComposite, SWT.BORDER);
 		keywordsText.setFont(TEXT_FONT);
 		keywordsText.setEditable(false);
 		keywordsText.setForeground(foreground);
 		keywordsText.setBackground(JFaceColors.getErrorBackground(display));
 		GridData keywordsData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
 		keywordsData.horizontalSpan = 2;
 		keywordsData.widthHint = 200;
 		keywordsText.setLayoutData(keywordsData);
 		keywordsText.setText(keywords);
 		keywordsText.addListener(SWT.FocusIn, new GenericListener());
 		keyWordsList = new List(attributesComposite, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
 		keyWordsList.setFont(TEXT_FONT);
 		GridData keyWordsTextData =
 			new GridData(GridData.HORIZONTAL_ALIGN_FILL);
 		keyWordsTextData.horizontalSpan = 1;
 		keyWordsTextData.widthHint = 125;
 		keyWordsTextData.heightHint = 40;
 		keyWordsList.setLayoutData(keyWordsTextData);
 		
 		// initialize the keywords list with valid values
 		java.util.List<String> keywordList = bug.getKeywords();
 		if (keywordList != null) {
 			for (Iterator<String> it = keywordList.iterator(); it.hasNext(); ) {
 				String keyword = it.next();
 				keyWordsList.add(keyword);
 			}
 			
 			// get the selected keywords for the bug
 			StringTokenizer st = new StringTokenizer(keywords, ",", false);
 			ArrayList<Integer> indicies = new ArrayList<Integer>();
 			while (st.hasMoreTokens()) {
 				String s = st.nextToken().trim();
 				int index = keyWordsList.indexOf(s);
 				if (index != -1)
 					indicies.add(new Integer(index));
 			}
 	
 			// select the keywords that were selected for the bug
 			int length = indicies.size();
 			int[] sel = new int[length];
 			for (int i = 0; i < length; i++) {
 				sel[i] = indicies.get(i).intValue();
 			}
 			keyWordsList.select(sel);
 		}
 		
 		keyWordsList.addSelectionListener(new KeywordListener());
 		keyWordsList.addListener(SWT.FocusIn, new GenericListener());
 	}
 
 	@Override
 	protected void updateBug() {
 			
 		// go through all of the attributes and update the main values to the new ones
 		for (Iterator<Attribute> it = bug.getAttributes().iterator(); it.hasNext(); ) {
 			Attribute a = it.next();
 			a.setValue(a.getNewValue());
 		}
 				
 		// Update some other fields as well.
 		bug.setNewComment(bug.getNewNewComment());
 			
 	}
 
 	@Override
 	protected void restoreBug() {
 		
 		// go through all of the attributes and restore the new values to the main ones
 		for (Iterator<Attribute> it = bug.getAttributes().iterator(); it.hasNext(); ) {
 			Attribute a = it.next();
 			a.setNewValue(a.getValue());
 		}
 		
 		// Restore some other fields as well.
 		bug.setNewNewComment(bug.getNewComment());
 	}
 
 	/**
 	 * This job opens a compare editor to compare the current state of the bug
 	 * in the editor with the bug on the server.
 	 */
 	protected class OpenCompareEditorJob extends Job {
 		
 			public OpenCompareEditorJob(String name) {
 				super(name);
 			}
 		
 			@Override
 			protected IStatus run(IProgressMonitor monitor) {
 				final BugReport serverBug;
 				try {
 					serverBug = BugzillaRepository.getInstance().getBug(bug.getId());
 					// If no bug was found on the server, throw an exception so that the
 					// user gets the same message that appears when there is a problem reading the server.
 					if (serverBug == null) 
 						throw new Exception();
 				} catch (Exception e) {
 					Workbench.getInstance().getDisplay().asyncExec(new Runnable() {
 						public void run() {
 							MessageDialog.openInformation(Workbench.getInstance().getActiveWorkbenchWindow().getShell(),
 									"Could not open bug.", "Bug #" + bug.getId() + " could not be read from the server.");
 						}
 					});
 					return new Status(IStatus.OK, IBugzillaConstants.PLUGIN_ID, IStatus.OK, "Could not get the bug report from the server.", null);
 				}
 				Workbench.getInstance().getDisplay().asyncExec(new Runnable() {
 					public void run() {
 						compareInput.setTitle("Bug #" + bug.getId());
 						compareInput.setLeft(bug);
 						compareInput.setRight(serverBug);
 						CompareUI.openCompareEditor(compareInput);
 					}
 				});
 				return new Status(IStatus.OK, IBugzillaConstants.PLUGIN_ID, IStatus.OK, "", null);
 			}
 		
 		}
 	
 	/**
 	 * Class to handle the selection change of the keywords.
 	 */
 	protected class KeywordListener implements SelectionListener {
 	
 		/*
 		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
 		 */
 		public void widgetSelected(SelectionEvent arg0) {
 			changeDirtyStatus(true);
 			
 			// get the selected keywords and create a string to submit
 			StringBuffer keywords = new StringBuffer();
 			String [] sel = keyWordsList.getSelection();
 			
 			// allow unselecting 1 keyword when it is the only one selected
 			if(keyWordsList.getSelectionCount() == 1) {
 				int index = keyWordsList.getSelectionIndex();
 				String keyword = keyWordsList.getItem(index);
 				if (bug.getAttribute("Keywords").getNewValue().equals(keyword))
 					keyWordsList.deselectAll();
 			}
 			
 			for(int i = 0; i < keyWordsList.getSelectionCount(); i++) {
 				keywords.append(sel[i]);
 				if (i != keyWordsList.getSelectionCount()-1) {
 					keywords.append(",");
 				}
 			}
 			bug.getAttribute("Keywords").setNewValue(keywords.toString());
 			
 			// update the keywords text field
 			keywordsText.setText(keywords.toString());
 		}
 	
 		/* 
 		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
 		 */
 		public void widgetDefaultSelected(SelectionEvent arg0) {	
 			// no need to listen to this
 		}
 	
 	}
 
 	/**
 	 * A listener for selection of the description field.
 	 */
 	protected class DescriptionListener implements Listener {
 		public void handleEvent(Event event) {
 			fireSelectionChanged(new SelectionChangedEvent(selectionProvider, new StructuredSelection(new BugzillaReportSelection(bug.getId(), bug.getServer(), "Description", true, bug.getSummary()))));
 		}
 	}
 	
 	/**
 	 * A listener for selection of a comment.
 	 */
 	protected class CommentListener implements Listener {
 		
 		/** The comment that this listener is for. */
 		private Comment comment;
 
 		/**
 		 * Creates a new <code>CommentListener</code>.
 		 * @param comment The comment that this listener is for.
 		 */
 		public CommentListener(Comment comment) {
 			this.comment = comment;
 		}
 		
 		public void handleEvent(Event event) {
 			fireSelectionChanged(new SelectionChangedEvent(selectionProvider, new StructuredSelection(new BugzillaReportSelection(bug.getId(), bug.getServer(), comment.getCreated().toString(), comment, bug.getSummary()))));
 		}
 	}
 	
 	/**
 	 * A listener for selection of the textbox where a new comment is entered
 	 * in.
 	 */
 	protected class NewCommentListener implements Listener {
 		public void handleEvent(Event event) {
 			fireSelectionChanged(new SelectionChangedEvent(selectionProvider, new StructuredSelection(new BugzillaReportSelection(bug.getId(), bug.getServer(), "New Comment", false, bug.getSummary()))));
 		}
 	}
 	
 	/**
 	 * Class to handle the selection change of the radio buttons.
 	 */
 	protected class RadioButtonListener implements SelectionListener {
 
 		public void widgetDefaultSelected(SelectionEvent e) {
 			widgetSelected(e);
 		}
 
 		public void widgetSelected(SelectionEvent e) {
 			Button selected = null;
 			for (int i = 0; i < radios.length; i++) {
 				if (radios[i].getSelection())
 					selected = radios[i];
 			}
 			// determine the operation to do to the bug
 			for (int i = 0; i < radios.length; i++) {
 				if (radios[i] != e.widget && radios[i] != selected){
 					radios[i].setSelection(false);
 				}
 				
 				if (e.widget == radios[i]) {
 					Operation o = bug.getOperation(radios[i].getText());
 					bug.setSelectedOperation(o);
 					ExistingBugEditor.this.changeDirtyStatus(true);
 				}
 				else if(e.widget == radioOptions[i]) {
 					Operation o = bug.getOperation(radios[i].getText());
 					o.setOptionSelection(radioOptions[i].getItem(radioOptions[i].getSelectionIndex()));
 					
 					if(bug.getSelectedOperation() != null)
 						bug.getSelectedOperation().setChecked(false);
 					o.setChecked(true);
 					
 					bug.setSelectedOperation(o);
 					radios[i].setSelection(true);
 		            if(selected != null && selected != radios[i]){
 		                selected.setSelection(false);
 		            }
 		            ExistingBugEditor.this.changeDirtyStatus(true);
 				}
 			}
			validateInput();
		}
	}

	private void validateInput() {
		Operation o = bug.getSelectedOperation();
		if(o != null && o.getKnobName().compareTo("resolve") == 0 && (addCommentsText.getText() == null || addCommentsText.getText().equals(""))){
			submitButton.setEnabled(false);
		} else{
			submitButton.setEnabled(true);
 		}
 	}
 	
 	@Override
 	public void handleSummaryEvent() {
 		String sel = summaryText.getText();
 		Attribute a = getBug().getAttribute("Summary");
 		if (!(a.getNewValue().equals(sel))) {
 			a.setNewValue(sel);
 			changeDirtyStatus(true);
 		}
 	}
 }
