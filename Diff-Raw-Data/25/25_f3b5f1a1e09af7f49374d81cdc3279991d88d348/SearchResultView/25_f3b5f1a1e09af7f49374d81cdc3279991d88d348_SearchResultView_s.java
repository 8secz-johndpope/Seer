 /*
  * (c) Copyright IBM Corp. 2000, 2002.
  * All Rights Reserved.
  */
 package org.eclipse.search.internal.ui;
 
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IResource;
 
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.action.IToolBarManager;
 import org.eclipse.jface.operation.IRunnableWithProgress;
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.util.Assert;
 import org.eclipse.jface.util.IPropertyChangeListener;
 import org.eclipse.jface.util.PropertyChangeEvent;
 import org.eclipse.jface.viewers.IBaseLabelProvider;
 import org.eclipse.jface.viewers.ILabelProvider;
 import org.eclipse.jface.viewers.ISelection;
 
 import org.eclipse.ui.IActionBars;
 import org.eclipse.ui.IMemento;
 import org.eclipse.ui.IViewSite;
 import org.eclipse.ui.IWorkbenchActionConstants;
 import org.eclipse.ui.PartInitException;
 import org.eclipse.ui.help.WorkbenchHelp;
 import org.eclipse.ui.part.CellEditorActionHandler;
 import org.eclipse.ui.part.ViewPart;
 
 import org.eclipse.search.ui.IActionGroupFactory;
 import org.eclipse.search.ui.IContextMenuContributor;
 import org.eclipse.search.ui.IGroupByKeyComputer;
 import org.eclipse.search.ui.ISearchResultView;
 
 
 public class SearchResultView extends ViewPart implements ISearchResultView {
 
 
 	private static Map fgLabelProviders= new HashMap(5);;
 	
 	private SearchResultViewer fViewer;
 	private Map fResponse;
 	private IMemento fMemento;
 	private IPropertyChangeListener fPropertyChangeListener;
 	private CellEditorActionHandler fCellEditorActionHandler;
 	private SelectAllAction fSelectAllAction;
 
 	/*
 	 * Implements method from IViewPart.
 	 */
 	public void init(IViewSite site, IMemento memento) throws PartInitException {
 		super.init(site, memento);
 		fMemento= memento;
 	}
 
 	/*
 	 * Implements method from IViewPart.
 	 */
 	public void saveState(IMemento memento) {
 		if (fViewer == null) {
 			// part has not been created
 			if (fMemento != null) //Keep the old state;
 				memento.putMemento(fMemento);
 			return;
 		}
 		fViewer.saveState(memento);
 	}	
 
 	/**
 	 * Creates the search list inner viewer.
 	 */
 	public void createPartControl(Composite parent) {
 		Assert.isTrue(fViewer == null);
 		fViewer= new SearchResultViewer(this, parent);
 		if (fMemento != null)
 			fViewer.restoreState(fMemento);
 		fMemento= null;
 		SearchManager.getDefault().addSearchChangeListener(fViewer);
 		fViewer.init();
 
 		// Add selectAll action handlers.
 		fCellEditorActionHandler = new CellEditorActionHandler(getViewSite().getActionBars());
 		fSelectAllAction= new SelectAllAction(fViewer);
 		fCellEditorActionHandler.setSelectAllAction(fSelectAllAction);
 
 		fillActionBars(getViewSite().getActionBars());
 		
 		fPropertyChangeListener= new IPropertyChangeListener() {
 			public void propertyChange(PropertyChangeEvent event) {
 				if (SearchPreferencePage.POTENTIAL_MATCH_FG_COLOR.equals(event.getProperty()) || SearchPreferencePage.EMPHASIZE_POTENTIAL_MATCHES.equals(event.getProperty()))
 					if (fViewer != null)
						fViewer.updatedPotentialMatchBgColor();
 			}
 		};
 		
 		SearchPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyChangeListener);
 		
 		WorkbenchHelp.setHelp(fViewer.getControl(), ISearchHelpContextIds.SEARCH_VIEW);
 	}
 	
 	/**
 	 * Returns the search result viewer.
 	 */
 	public SearchResultViewer getViewer() {
 		return fViewer;
 	}
 	
 	//---- IWorkbenchPart ------------------------------------------------------
 
 
 	public void setFocus() {
 		fViewer.getControl().setFocus();
 	}
 	
 	public void dispose() {
 		if (fViewer != null) {
 			SearchManager.getDefault().removeSearchChangeListener(fViewer);
 			fViewer= null;
 		}
 		if (fPropertyChangeListener != null)
 			SearchPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyChangeListener);
 		if (fCellEditorActionHandler != null) {
 			fCellEditorActionHandler.dispose();
 			fCellEditorActionHandler= null;
 		}
 		super.dispose();
 	}
 	
 	protected void setTitle(String title) {
 		super.setTitle(title);
 	}
 	
 	protected void setTitleToolTip(String text) {
 		super.setTitleToolTip(text);
 	}
 	
 	//---- Adding Action to Toolbar -------------------------------------------
 	
 	private void fillActionBars(IActionBars actionBars) {
 		IToolBarManager toolBar= actionBars.getToolBarManager();
 		fillToolBar(toolBar);
 		actionBars.updateActionBars();
 		
 		// Add selectAll action handlers.
 		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.SELECT_ALL, fSelectAllAction);
 	}
 
 	private void fillToolBar(IToolBarManager tbm) {
 		fViewer.fillToolBar(tbm);
 	}	
 
 	ILabelProvider getLabelProvider(String pageId) {
 		if (pageId != null)
 			return (ILabelProvider)fgLabelProviders.get(pageId);
 		return null;
 	}
 
 	public ILabelProvider getLabelProvider() {
 		if (fViewer == null)
 			return null;
 		IBaseLabelProvider labelProvider= fViewer.getLabelProvider();
 		if (labelProvider == null)
 			return null;
 		
 		return ((SearchResultLabelProvider)labelProvider).getLabelProvider();
 	}
 
 	private void setContextMenuContributor(final IContextMenuContributor contributor) {
 		// Make sure we are doing it in the right thread.
 		getDisplay().syncExec(new Runnable() {
 			public void run() {
 				getViewer().setContextMenuTarget(contributor);
 			}
 		});
 	}
 
 	private void setGotoMarkerAction(final IAction gotoMarkerAction) {
 		// Make sure we are doing it in the right thread.
 		getDisplay().syncExec(new Runnable() {
 			public void run() {
 				getViewer().setGotoMarkerAction(gotoMarkerAction);
 			}
 		});
 	}
 
 
 	Display getDisplay() {
 		return fViewer.getControl().getDisplay();
 	}	
 
 
 	//---- ISearchResultView --------------------------------------------------
 
 
 	/*
 	 * Implements method from ISearchResultView
 	 */
 	public ISelection getSelection() {
 		return fViewer.getSelection();
 	}
 
 	/*
 	 * Implements method from ISearchResultView
 	 */
 	public void searchStarted(
 				IActionGroupFactory		groupFactory,
 				String					singularLabel,
 				String					pluralLabelPattern,
 				ImageDescriptor			imageDescriptor,
 				String					pageId,
 				ILabelProvider			labelProvider,
 				IAction					gotoAction,
 				IGroupByKeyComputer		groupByKeyComputer,
 				IRunnableWithProgress	operation) {
 
 
 		Assert.isNotNull(pageId);
 		Assert.isNotNull(pluralLabelPattern);
 		Assert.isNotNull(gotoAction);		
 
 		fResponse= new HashMap(500);
 		setGotoMarkerAction(gotoAction);
 
 		ILabelProvider oldLabelProvider= (ILabelProvider)fgLabelProviders.get(pageId);
 		if (oldLabelProvider != null)
 			oldLabelProvider.dispose();
 		fgLabelProviders.put(pageId, labelProvider);
 
 		SearchManager.getDefault().addNewSearch(		
 			new Search(
 				pageId,
 				singularLabel,
 				pluralLabelPattern,
 				null,
 				imageDescriptor,
 				fViewer.getGotoMarkerAction(),
 				groupFactory,
 				groupByKeyComputer,
 				operation));
 	};
 
 	/**
 	 * Implements method from ISearchResultView
 	 * @deprecated	As of build > 20011107, replaced by the new version with additonal parameter
 	 */
 	public void searchStarted(
 				String					pageId,
 				String					label,
 				ImageDescriptor			imageDescriptor,
 				IContextMenuContributor contributor,
 				ILabelProvider			labelProvider,
 				IAction					gotoAction,
 				IGroupByKeyComputer		groupByKeyComputer,
 				IRunnableWithProgress	operation) {
 		
 		searchStarted(pageId, null, label, imageDescriptor, contributor, labelProvider, gotoAction, groupByKeyComputer, operation);
 	};
 
 	/**
 	 * Implements method from ISearchResultView
 	 * @deprecated	As of build > 20020514
 	 */
 	public void searchStarted(
 				String					pageId,
 				String					singularLabel,
 				String					pluralLabelPattern,
 				ImageDescriptor			imageDescriptor,
 				IContextMenuContributor contributor,
 				ILabelProvider			labelProvider,
 				IAction					gotoAction,
 				IGroupByKeyComputer		groupByKeyComputer,
 				IRunnableWithProgress	operation) {
 
 
 		Assert.isNotNull(pageId);
 		Assert.isNotNull(pluralLabelPattern);
 		Assert.isNotNull(gotoAction);		
 
 		fResponse= new HashMap(500);
 		setGotoMarkerAction(gotoAction);
 
 		ILabelProvider oldLabelProvider= (ILabelProvider)fgLabelProviders.get(pageId);
 		if (oldLabelProvider != null)
 			oldLabelProvider.dispose();
 		fgLabelProviders.put(pageId, labelProvider);
 
 		SearchManager.getDefault().addNewSearch(		
 			new Search(
 				pageId,
 				singularLabel,
 				pluralLabelPattern,
 				null,
 				imageDescriptor,
 				fViewer.getGotoMarkerAction(),
 				contributor,
 				groupByKeyComputer,
 				operation));
 	};
 
 	/*
 	 * Implements method from ISearchResultView
 	 */
 	public void addMatch(String description, Object groupByKey, IResource resource, IMarker marker) {
 		SearchResultViewEntry entry= (SearchResultViewEntry)fResponse.get(groupByKey);
 		if (entry == null) {
 			entry= new SearchResultViewEntry(groupByKey, resource);
 			fResponse.put(groupByKey, entry);
 		}
 		entry.add(marker);
 	}
 
 
 	/*
 	 * Implements method from ISearchResultView
 	 */
 	public void searchFinished() {
 		SearchManager.getDefault().setCurrentResults(new ArrayList(fResponse.values()));
 		fResponse= null;
 	}
 }
