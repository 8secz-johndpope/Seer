 package org.eclipse.team.internal.ccvs.ui;
 
 /*
  * (c) Copyright IBM Corp. 2000, 2002.
  * All Rights Reserved.
  */
  
 import java.io.InputStream;
 import java.lang.reflect.InvocationTargetException;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.NullProgressMonitor;
 import org.eclipse.core.runtime.SubProgressMonitor;
 import org.eclipse.jface.action.Action;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.action.IMenuListener;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.action.IToolBarManager;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.action.Separator;
 import org.eclipse.jface.dialogs.ErrorDialog;
 import org.eclipse.jface.dialogs.ProgressMonitorDialog;
 import org.eclipse.jface.preference.IPreferenceStore;
 import org.eclipse.jface.text.Document;
 import org.eclipse.jface.text.ITextOperationTarget;
 import org.eclipse.jface.text.ITextViewer;
 import org.eclipse.jface.text.TextViewer;
 import org.eclipse.jface.viewers.ColumnWeightData;
 import org.eclipse.jface.viewers.ISelection;
 import org.eclipse.jface.viewers.ISelectionChangedListener;
 import org.eclipse.jface.viewers.IStructuredContentProvider;
 import org.eclipse.jface.viewers.IStructuredSelection;
 import org.eclipse.jface.viewers.ITableLabelProvider;
 import org.eclipse.jface.viewers.LabelProvider;
 import org.eclipse.jface.viewers.SelectionChangedEvent;
 import org.eclipse.jface.viewers.TableLayout;
 import org.eclipse.jface.viewers.TableViewer;
 import org.eclipse.jface.viewers.Viewer;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.BusyIndicator;
 import org.eclipse.swt.custom.SashForm;
 import org.eclipse.swt.custom.StyledText;
 import org.eclipse.swt.dnd.DND;
 import org.eclipse.swt.dnd.Transfer;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.team.ccvs.core.CVSTag;
 import org.eclipse.team.ccvs.core.CVSTeamProvider;
 import org.eclipse.team.ccvs.core.ICVSRemoteFile;
 import org.eclipse.team.ccvs.core.ILogEntry;
 import org.eclipse.team.core.ITeamProvider;
 import org.eclipse.team.core.TeamException;
 import org.eclipse.team.core.TeamPlugin;
 import org.eclipse.team.internal.ccvs.ui.actions.OpenRemoteFileAction;
 import org.eclipse.ui.IActionBars;
 import org.eclipse.ui.ISelectionListener;
 import org.eclipse.ui.IWorkbenchActionConstants;
 import org.eclipse.ui.IWorkbenchPart;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.actions.WorkspaceModifyOperation;
 import org.eclipse.ui.part.ResourceTransfer;
 import org.eclipse.ui.part.ViewPart;
 import org.eclipse.ui.texteditor.ITextEditorActionConstants;
 import org.eclipse.ui.texteditor.IUpdate;
 
 /**
  * The history view allows browsing of an array of resource revisions
  */
 public class HistoryView extends ViewPart implements ISelectionListener {
 	private IFile file;
 	private CVSTeamProvider provider;
 	
 	private TableViewer tableViewer;
 	private TextViewer textViewer;
 	
 	private OpenRemoteFileAction openAction;
 	private IAction toggleTextAction;
 	private TextViewerAction copyAction;
 	private TextViewerAction selectAllAction;
 	private Action addAction;
 	
 	private SashForm sashForm;
 	
 	//column constants
 	private static final int COL_REVISION = 0;
 	private static final int COL_TAGS = 1;
 	private static final int COL_DATE = 2;
 	private static final int COL_AUTHOR = 3;
 	private static final int COL_COMMENT = 4;
 
 	class HistoryLabelProvider extends LabelProvider implements ITableLabelProvider {
 		public Image getColumnImage(Object element, int columnIndex) {
 			return null;
 		}
 		public String getColumnText(Object element, int columnIndex) {
 			ILogEntry entry = (ILogEntry)element;
 			switch (columnIndex) {
 				case COL_REVISION:
 					String revision = entry.getRevision();
 					if (file == null) return revision;
 					try {
 						ICVSRemoteFile currentEdition = (ICVSRemoteFile)provider.getRemoteResource(file);
 						if (currentEdition != null && currentEdition.getRevision().equals(revision)) {
 							return "*" + revision;
 						}
 					} catch (TeamException e) {
 						ErrorDialog.openError(getViewSite().getShell(), null, null, e.getStatus());
 					}
 					return revision;
 				case COL_TAGS:
 					CVSTag[] tags = entry.getTags();
 					StringBuffer result = new StringBuffer();
 					for (int i = 0; i < tags.length; i++) {
 						result.append(tags[i].getName());
 						if (i < tags.length - 1) {
 							result.append(", ");
 						}
 					}
 					return result.toString();
 				case COL_DATE:
 					return entry.getDate();
 				case COL_AUTHOR:
 					return entry.getAuthor();
 				case COL_COMMENT:
 					String comment = entry.getComment();
 					int index = comment.indexOf("\n");
					switch (index) {
						case -1:
							return comment;
						case 0:
							return "[...]";
						default:
							return comment.substring(0, index - 1) + "[...]";
					}
 			}
 			return "";
 		}
 	}
 	
 	public static final String VIEW_ID = "org.eclipse.team.ccvs.ui.HistoryView";
 	
 	/**
 	 * Adds the action contributions for this view.
 	 */
 	protected void contributeActions() {
 		// Refresh (toolbar)
 		final Action refreshAction = new Action(Policy.bind("HistoryView.refresh"), CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_REFRESH)) {
 			public void run() {
 				BusyIndicator.showWhile(tableViewer.getTable().getDisplay(), new Runnable() {
 					public void run() {
 						tableViewer.refresh();
 					}
 				});
 			}
 		};
 		refreshAction.setToolTipText(Policy.bind("HistoryView.refresh"));
 		
 		// Double click open action
 		openAction = new OpenRemoteFileAction();
 		tableViewer.getTable().addListener(SWT.DefaultSelection, new Listener() {
 			public void handleEvent(Event e) {
 				openAction.selectionChanged(null, tableViewer.getSelection());
 				openAction.run(null);
 			}
 		});
 
 		addAction = new Action(Policy.bind("HistoryView.addToWorkspace")) {
 			public void run() {
 				try {
 					new ProgressMonitorDialog(getViewSite().getShell()).run(true, true, new WorkspaceModifyOperation() {
 						protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
 							if (file == null) return;
 							ISelection selection = tableViewer.getSelection();
 							if (!(selection instanceof IStructuredSelection)) return;
 							IStructuredSelection ss = (IStructuredSelection)selection;
 							Object o = ss.getFirstElement();
 							ILogEntry entry = (ILogEntry)o;
 							ICVSRemoteFile remoteFile = entry.getRemoteFile();
 							// Do the load. This just consists of setting the local contents. We don't
 							// actually want to change the base.
 							monitor.beginTask(null, 100);
 							try {
 								InputStream in = remoteFile.getContents(new SubProgressMonitor(monitor, 50));
 								file.setContents(in, false, true, new SubProgressMonitor(monitor, 50));				
 							} catch (TeamException e) {
 								throw new InvocationTargetException(e);
 							} catch (CoreException e) {
 								throw new InvocationTargetException(e);
 							} finally {
 								monitor.done();
 							}
 						}
 					});
 				} catch (InvocationTargetException e) {
 					Throwable t = e.getTargetException();
 					if (t instanceof TeamException) {
 						ErrorDialog.openError(getViewSite().getShell(), null, null, ((TeamException)t).getStatus());
 					} else if (t instanceof CoreException) {
 						IStatus status = ((CoreException)t).getStatus();
 						ErrorDialog.openError(getViewSite().getShell(), null, null, status);
 						CVSUIPlugin.log(status);
 					} else {
 						// To do
 					}
 				} catch (InterruptedException e) {
 					// Do nothing
 				}
 			}
 		};
 
 		// Toggle text visible action
 		final IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
 		toggleTextAction = new Action(Policy.bind("HistoryView.showComment")) {
 			public void run() {
 				if (sashForm.getMaximizedControl() != null) {
 					sashForm.setMaximizedControl(null);
 				} else {
 					sashForm.setMaximizedControl(tableViewer.getControl());
 				}
 				store.setValue(ICVSUIConstants.PREF_SHOW_COMMENTS, toggleTextAction.isChecked());
 			}
 		};
 		toggleTextAction.setChecked(store.getBoolean(ICVSUIConstants.PREF_SHOW_COMMENTS));
 		
 		// Contribute actions to popup menu
 		MenuManager menuMgr = new MenuManager();
 		Menu menu = menuMgr.createContextMenu(tableViewer.getTable());
 		menuMgr.addMenuListener(new IMenuListener() {
 			public void menuAboutToShow(IMenuManager menuMgr) {
 				fillTableMenu(menuMgr);
 			}
 		});
 		menuMgr.setRemoveAllWhenShown(true);
 		tableViewer.getTable().setMenu(menu);
 		getSite().registerContextMenu(menuMgr, tableViewer);
 
 		// Contribute toggle text visible to the toolbar drop-down
 		IActionBars actionBars = getViewSite().getActionBars();
 		IMenuManager actionBarsMenu = actionBars.getMenuManager();
 		actionBarsMenu.add(toggleTextAction);
 
 		// Create the local tool bar
 		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
 		tbm.add(refreshAction);
 		tbm.update(false);
 	
 		// Create actions for the text editor
 		copyAction = new TextViewerAction(textViewer, ITextOperationTarget.COPY);
 		copyAction.setText(Policy.bind("HistoryView.copy"));
 		actionBars.setGlobalActionHandler(ITextEditorActionConstants.COPY, copyAction);
 		
 		selectAllAction = new TextViewerAction(textViewer, ITextOperationTarget.SELECT_ALL);
 		selectAllAction.setText(Policy.bind("HistoryView.selectAll"));
 		actionBars.setGlobalActionHandler(ITextEditorActionConstants.SELECT_ALL, selectAllAction);
 
 		actionBars.updateActionBars();
 
 		menuMgr = new MenuManager();
 		menuMgr.setRemoveAllWhenShown(true);
 		menuMgr.addMenuListener(new IMenuListener() {
 			public void menuAboutToShow(IMenuManager menuMgr) {
 				fillTextMenu(menuMgr);
 			}
 		});
 		StyledText text = textViewer.getTextWidget();
 		menu = menuMgr.createContextMenu(text);
 		text.setMenu(menu);
 	}
 	
 	/**
 	 * Creates the columns for the history table.
 	 */
 	private void createColumns(Table table, TableLayout layout) {
 		SelectionListener headerListener = getColumnListener();
 		// revision
 		TableColumn col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("HistoryView.revision"));
 		col.addSelectionListener(headerListener);
 		layout.addColumnData(new ColumnWeightData(20, true));
 	
 		// tags
 		col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("HistoryView.tags"));
 		col.addSelectionListener(headerListener);
 		layout.addColumnData(new ColumnWeightData(20, true));
 	
 		// creation date
 		col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("HistoryView.date"));
 		col.addSelectionListener(headerListener);
 		layout.addColumnData(new ColumnWeightData(20, true));
 	
 		// author
 		col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("HistoryView.author"));
 		col.addSelectionListener(headerListener);
 		layout.addColumnData(new ColumnWeightData(20, true));
 	
 		//comment
 		col = new TableColumn(table, SWT.NONE);
 		col.setResizable(true);
 		col.setText(Policy.bind("HistoryView.comment"));
 		col.addSelectionListener(headerListener);
 		layout.addColumnData(new ColumnWeightData(50, true));
 	}
 	/*
 	 * Method declared on IWorkbenchPart
 	 */
 	public void createPartControl(Composite parent) {
 		sashForm = new SashForm(parent, SWT.VERTICAL);
 		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
 		tableViewer = createTable(sashForm);
 		textViewer = createText(sashForm);
 		sashForm.setWeights(new int[] { 70, 30 });
 		getSite().getPage().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
 		contributeActions();
 		if (!CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_SHOW_COMMENTS)) {
 			sashForm.setMaximizedControl(tableViewer.getControl());
 		}
 		// set F1 help
 		//WorkbenchHelp.setHelp(viewer.getControl(), new ViewContextComputer (this, IVCMHelpContextIds.RESOURCE_HISTORY_VIEW));
 		initDragAndDrop();
 	}
 	/**
 	 * Creates the group that displays lists of the available repositories
 	 * and team streams.
 	 *
 	 * @param the parent composite to contain the group
 	 * @return the group control
 	 */
 	protected TableViewer createTable(Composite parent) {
 		Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
 		table.setHeaderVisible(true);
 		table.setLinesVisible(true);
 		GridData data = new GridData(GridData.FILL_BOTH);
 		table.setLayoutData(data);
 	
 		TableLayout layout = new TableLayout();
 		table.setLayout(layout);
 		
 		createColumns(table, layout);
 	
 		TableViewer viewer = new TableViewer(table);
 		viewer.setContentProvider(new IStructuredContentProvider() {
 			public Object[] getElements(Object inputElement) {
 				if (!(inputElement instanceof ICVSRemoteFile)) return null;
 				ICVSRemoteFile remoteFile = (ICVSRemoteFile)inputElement;
 				try {
 					return remoteFile.getLogEntries(new NullProgressMonitor());
 				} catch (TeamException e) {
 					ErrorDialog.openError(getViewSite().getShell(), null, null, e.getStatus());
 					// Set a default title
 					setTitle(Policy.bind("HistoryView.title"));
 				}
 				return null;
 				
 			}
 			public void dispose() {
 			}
 			/**
 			 * The input has changed. Change the title of the view if necessary.
 			 */
 			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
 				if (!(newInput instanceof ICVSRemoteFile)) {
 					setTitle(Policy.bind("HistoryView.title"));
 					return;
 				}
 				ICVSRemoteFile newFile = (ICVSRemoteFile)newInput;
 				setTitle(Policy.bind("HistoryView.titleWithArgument", newFile.getName()));
 			}
 		});
 		viewer.setLabelProvider(new HistoryLabelProvider());
 		
 		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
 			public void selectionChanged(SelectionChangedEvent event) {
 				ISelection selection = event.getSelection();
 				if (selection == null || !(selection instanceof IStructuredSelection)) {
 					textViewer.setDocument(new Document(""));
 					return;
 				}
 				IStructuredSelection ss = (IStructuredSelection)selection;
 				if (ss.size() != 1) {
 					textViewer.setDocument(new Document(""));
 					return;
 				}
 				ILogEntry entry = (ILogEntry)ss.getFirstElement();
 				textViewer.setDocument(new Document(entry.getComment()));
 			}
 		});
 		
 		// By default, reverse sort by revision.
 		HistorySorter sorter = new HistorySorter(COL_REVISION);
 		sorter.setReversed(true);
 		viewer.setSorter(sorter);
 		
 		return viewer;
 	}
 	protected TextViewer createText(Composite parent) {
 		TextViewer result = new TextViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
 		result.addSelectionChangedListener(new ISelectionChangedListener() {
 			public void selectionChanged(SelectionChangedEvent event) {
 				copyAction.update();
 			}
 		});
 		return result;
 	}
 	public void dispose() {
 		getSite().getPage().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
 	}	
 	/**
 	 * Adds the listener that sets the sorter.
 	 */
 	private SelectionListener getColumnListener() {
 		/**
 	 	 * This class handles selections of the column headers.
 		 * Selection of the column header will cause resorting
 		 * of the shown tasks using that column's sorter.
 		 * Repeated selection of the header will toggle
 		 * sorting order (ascending versus descending).
 		 */
 		return new SelectionAdapter() {
 			/**
 			 * Handles the case of user selecting the
 			 * header area.
 			 * <p>If the column has not been selected previously,
 			 * it will set the sorter of that column to be
 			 * the current tasklist sorter. Repeated
 			 * presses on the same column header will
 			 * toggle sorting order (ascending/descending).
 			 */
 			public void widgetSelected(SelectionEvent e) {
 				// column selected - need to sort
 				int column = tableViewer.getTable().indexOf((TableColumn) e.widget);
 				HistorySorter oldSorter = (HistorySorter)tableViewer.getSorter();
 				if (oldSorter != null && column == oldSorter.getColumnNumber()) {
 					oldSorter.setReversed(!oldSorter.isReversed());
 					tableViewer.refresh();
 				} else {
 					tableViewer.setSorter(new HistorySorter(column));
 				}
 			}
 		};
 	}
 	/**
 	 * Returns the table viewer contained in this view.
 	 */
 	protected TableViewer getViewer() {
 		return tableViewer;
 	}
 	/**
 	 * Adds drag and drop support to the history view.
 	 */
 	void initDragAndDrop() {
 		int ops = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
 		Transfer[] transfers = new Transfer[] {ResourceTransfer.getInstance()};
 		tableViewer.addDropSupport(ops, transfers, new HistoryDropAdapter(tableViewer, this));
 	}
 	private void fillTableMenu(IMenuManager manager) {
 		if (tableViewer.getInput() == null) return;
 		// file actions go first (view file)
 		manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
 		if (file != null) {
 			// Add the "Add to Workspace" action if 1 revision is selected.
 			ISelection sel = tableViewer.getSelection();
 			if (!sel.isEmpty()) {
 				if (sel instanceof IStructuredSelection) {
 					if (((IStructuredSelection)sel).size() == 1) {
 						manager.add(addAction);
 					}
 				}
 			}
 		}
 		manager.add(new Separator("additions"));
 		manager.add(new Separator("additions-end"));
 	}
 	private void fillTextMenu(IMenuManager manager) {
 		manager.add(copyAction);
 		manager.add(selectAllAction);
 	}
 	/**
 	 * Makes the history view visible in the active perspective. If there
 	 * isn't a history view registered <code>null</code> is returned.
 	 * Otherwise the opened view part is returned.
 	 */
 	public static HistoryView openInActivePerspective() {
 		try {
 			return (HistoryView)CVSUIPlugin.getActivePage().showView(VIEW_ID);
 		} catch (PartInitException pe) {
 			return null;
 		}
 	}
 	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
 		/*if (CVSUIPlugin.getDefault().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_HISTORY_TRACKS_SELECTION)) {
 			if (selection == null) return;
 			if (!(selection instanceof IStructuredSelection)) return;
 			IStructuredSelection ss = (IStructuredSelection)selection;
 			if (ss.size() != 1) {
 				showHistory(null);
 				return;
 			}
 			Object first = ss.getFirstElement();
 			try {
 				IVersionHistory history = getHistory(first);
 				showHistory(history);
 			} catch (CoreException e) {
 				showHistory(null);
 			}
 		}*/
 	}
 	/** (Non-javadoc)
 	 * Method declared on IWorkbenchPart
 	 */
 	public void setFocus() {
 		if (tableViewer != null) {
 			Table control = tableViewer.getTable();
 			if (control != null && !control.isDisposed()) {
 				control.setFocus();
 			}
 		}
 	}
 	
 	/**
 	 * Shows the history for the given IResource in the view.
 	 * 
 	 * Only files are supported for now.
 	 */
 	public void showHistory(IResource resource) {
 		if (!(resource instanceof IFile)) return;
 		IFile file = (IFile)resource;
 		this.file = file;
 		ITeamProvider teamProvider = TeamPlugin.getManager().getProvider(file.getProject());
 		if (teamProvider == null) return;
 		if (!(teamProvider instanceof CVSTeamProvider)) return;
 		this.provider = (CVSTeamProvider)teamProvider;
 		try {
 			tableViewer.setInput(provider.getRemoteResource(file));
 		} catch (TeamException e) {
 			ErrorDialog.openError(getViewSite().getShell(), null, null, e.getStatus());
 		}
 	}
 	
 	/**
 	 * Shows the history for the given ICVSRemoteFile in the view.
 	 */
 	public void showHistory(ICVSRemoteFile file) {
 		this.file = null;
 		tableViewer.setInput(file);
 	}
 }
