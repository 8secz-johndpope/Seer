 /*******************************************************************************
  * Copyright (c) 2010, 2011, 2012 Ericsson
  *
  * All rights reserved. This program and the accompanying materials are
  * made available under the terms of the Eclipse Public License v1.0 which
  * accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *   Francois Chouinard - Initial API and implementation
  *   Patrick Tasse - Factored out from events view
  *   Francois Chouinard - Replaced Table by TmfVirtualTable
  *   Patrick Tasse - Filter implementation (inspired by www.eclipse.org/mat)
  *******************************************************************************/
 
 package org.eclipse.linuxtools.tmf.ui.viewers.events;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.regex.Pattern;
 import java.util.regex.PatternSyntaxException;
 
 import org.eclipse.core.resources.IFile;
 import org.eclipse.core.resources.IMarker;
 import org.eclipse.core.resources.IResource;
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.core.runtime.IStatus;
 import org.eclipse.core.runtime.Status;
 import org.eclipse.core.runtime.jobs.Job;
 import org.eclipse.jface.action.Action;
 import org.eclipse.jface.action.IAction;
 import org.eclipse.jface.action.IMenuListener;
 import org.eclipse.jface.action.IMenuManager;
 import org.eclipse.jface.action.MenuManager;
 import org.eclipse.jface.action.Separator;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.InputDialog;
 import org.eclipse.jface.dialogs.MessageDialog;
 import org.eclipse.jface.resource.FontDescriptor;
 import org.eclipse.jface.resource.JFaceResources;
 import org.eclipse.jface.resource.LocalResourceManager;
 import org.eclipse.linuxtools.internal.tmf.ui.Activator;
 import org.eclipse.linuxtools.internal.tmf.ui.Messages;
 import org.eclipse.linuxtools.tmf.core.component.ITmfDataProvider;
 import org.eclipse.linuxtools.tmf.core.component.TmfComponent;
 import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
 import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
 import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
 import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
 import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
 import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
 import org.eclipse.linuxtools.tmf.core.filter.ITmfFilter;
 import org.eclipse.linuxtools.tmf.core.filter.model.ITmfFilterTreeNode;
 import org.eclipse.linuxtools.tmf.core.filter.model.TmfFilterAndNode;
 import org.eclipse.linuxtools.tmf.core.filter.model.TmfFilterMatchesNode;
 import org.eclipse.linuxtools.tmf.core.filter.model.TmfFilterNode;
 import org.eclipse.linuxtools.tmf.core.request.ITmfDataRequest.ExecutionType;
 import org.eclipse.linuxtools.tmf.core.request.TmfDataRequest;
 import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
 import org.eclipse.linuxtools.tmf.core.signal.TmfExperimentUpdatedSignal;
 import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
 import org.eclipse.linuxtools.tmf.core.signal.TmfTimeSynchSignal;
 import org.eclipse.linuxtools.tmf.core.signal.TmfTraceUpdatedSignal;
 import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
 import org.eclipse.linuxtools.tmf.core.trace.ITmfLocation;
 import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
 import org.eclipse.linuxtools.tmf.ui.viewers.events.TmfEventsCache.CachedEvent;
 import org.eclipse.linuxtools.tmf.ui.views.colors.ColorSetting;
 import org.eclipse.linuxtools.tmf.ui.views.colors.ColorSettingsManager;
 import org.eclipse.linuxtools.tmf.ui.views.colors.IColorSettingsListener;
 import org.eclipse.linuxtools.tmf.ui.views.filter.FilterManager;
 import org.eclipse.linuxtools.tmf.ui.widgets.rawviewer.TmfRawEventViewer;
 import org.eclipse.linuxtools.tmf.ui.widgets.virtualtable.ColumnData;
 import org.eclipse.linuxtools.tmf.ui.widgets.virtualtable.TmfVirtualTable;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.SashForm;
 import org.eclipse.swt.custom.TableEditor;
 import org.eclipse.swt.events.FocusAdapter;
 import org.eclipse.swt.events.FocusEvent;
 import org.eclipse.swt.events.KeyAdapter;
 import org.eclipse.swt.events.KeyEvent;
 import org.eclipse.swt.events.MouseAdapter;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.Font;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.graphics.Rectangle;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.layout.GridData;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.swt.widgets.MessageBox;
 import org.eclipse.swt.widgets.Shell;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.swt.widgets.TableItem;
 import org.eclipse.swt.widgets.Text;
 import org.eclipse.ui.PlatformUI;
 import org.eclipse.ui.ide.IGotoMarker;
 import org.eclipse.ui.themes.ColorUtil;
 
 /**
  * <b><u>TmfEventsTable</u></b>
  */
 public class TmfEventsTable extends TmfComponent implements IGotoMarker, IColorSettingsListener,
 ITmfEventsFilterProvider {
 
     private static final Image BOOKMARK_IMAGE = Activator.getDefault().getImageFromPath(
             "icons/elcl16/bookmark_obj.gif"); //$NON-NLS-1$
     private static final Image SEARCH_IMAGE = Activator.getDefault().getImageFromPath("icons/elcl16/search.gif"); //$NON-NLS-1$
     private static final Image SEARCH_MATCH_IMAGE = Activator.getDefault().getImageFromPath(
             "icons/elcl16/search_match.gif"); //$NON-NLS-1$
     private static final Image SEARCH_MATCH_BOOKMARK_IMAGE = Activator.getDefault().getImageFromPath(
             "icons/elcl16/search_match_bookmark.gif"); //$NON-NLS-1$
     private static final Image FILTER_IMAGE = Activator.getDefault()
             .getImageFromPath("icons/elcl16/filter_items.gif"); //$NON-NLS-1$
     private static final Image STOP_IMAGE = Activator.getDefault().getImageFromPath("icons/elcl16/stop.gif"); //$NON-NLS-1$
     private static final String SEARCH_HINT = Messages.TmfEventsTable_SearchHint;
     private static final String FILTER_HINT = Messages.TmfEventsTable_FilterHint;
     private static final int MAX_CACHE_SIZE = 1000;
 
     public interface Key {
         String SEARCH_TXT = "$srch_txt"; //$NON-NLS-1$
         String SEARCH_OBJ = "$srch_obj"; //$NON-NLS-1$
         String FILTER_TXT = "$fltr_txt"; //$NON-NLS-1$
         String FILTER_OBJ = "$fltr_obj"; //$NON-NLS-1$
         String TIMESTAMP = "$time"; //$NON-NLS-1$
         String RANK = "$rank"; //$NON-NLS-1$
         String FIELD_ID = "$field_id"; //$NON-NLS-1$
         String BOOKMARK = "$bookmark"; //$NON-NLS-1$
     }
 
     public static enum HeaderState {
         SEARCH, FILTER
     }
 
     interface Direction {
         int FORWARD = +1;
         int BACKWARD = -1;
     }
 
     // ------------------------------------------------------------------------
     // Table data
     // ------------------------------------------------------------------------
 
     protected Composite fComposite;
     protected SashForm fSashForm;
     protected TmfVirtualTable fTable;
     protected TmfRawEventViewer fRawViewer;
     protected ITmfTrace<?> fTrace;
     protected boolean fPackDone = false;
     protected HeaderState fHeaderState = HeaderState.SEARCH;
     protected long fSelectedRank = 0;
 
     // Filter data
     protected long fFilterMatchCount;
     protected long fFilterCheckCount;
     protected FilterThread fFilterThread;
     protected final Object fFilterSyncObj = new Object();
     protected SearchThread fSearchThread;
     protected final Object fSearchSyncObj = new Object();
     protected List<ITmfEventsFilterListener> fEventsFilterListeners = new ArrayList<ITmfEventsFilterListener>();
 
     // Bookmark map <Rank, MarkerId>
     protected Map<Long, Long> fBookmarksMap = new HashMap<Long, Long>();
     protected IFile fBookmarksFile;
     protected long fPendingGotoRank = -1;
 
     // SWT resources
     protected LocalResourceManager fResourceManager = new LocalResourceManager(JFaceResources.getResources());
     protected Color fGrayColor;
     protected Color fGreenColor;
     protected Font fBoldFont;
 
     // Table column names
     static private final String[] COLUMN_NAMES = new String[] { Messages.TmfEventsTable_TimestampColumnHeader,
         Messages.TmfEventsTable_SourceColumnHeader, Messages.TmfEventsTable_TypeColumnHeader,
         Messages.TmfEventsTable_ReferenceColumnHeader, Messages.TmfEventsTable_ContentColumnHeader };
 
     static private final ColumnData[] COLUMN_DATA = new ColumnData[] { new ColumnData(COLUMN_NAMES[0], 100, SWT.LEFT),
         new ColumnData(COLUMN_NAMES[1], 100, SWT.LEFT), new ColumnData(COLUMN_NAMES[2], 100, SWT.LEFT),
         new ColumnData(COLUMN_NAMES[3], 100, SWT.LEFT), new ColumnData(COLUMN_NAMES[4], 100, SWT.LEFT) };
 
     // Event cache
     private final TmfEventsCache fCache;
     private boolean fCacheUpdateBusy = false;
     private boolean fCacheUpdatePending = false;
     private boolean fCacheUpdateCompleted = false;
     private final Object fCacheUpdateSyncObj = new Object();
 
     private boolean fDisposeOnClose;
 
     // ------------------------------------------------------------------------
     // Constructor
     // ------------------------------------------------------------------------
 
     public TmfEventsTable(final Composite parent, final int cacheSize) {
         this(parent, cacheSize, COLUMN_DATA);
     }
 
     public TmfEventsTable(final Composite parent, int cacheSize, final ColumnData[] columnData) {
         super("TmfEventsTable"); //$NON-NLS-1$
 
         fComposite = new Composite(parent, SWT.NONE);
         final GridLayout gl = new GridLayout(1, false);
         gl.marginHeight = 0;
         gl.marginWidth = 0;
         gl.verticalSpacing = 0;
         fComposite.setLayout(gl);
 
         fSashForm = new SashForm(fComposite, SWT.HORIZONTAL);
         fSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
 
         // Create a virtual table
         final int style = SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION;
         fTable = new TmfVirtualTable(fSashForm, style);
 
         // Set the table layout
         final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
         fTable.setLayoutData(layoutData);
 
         // Some cosmetic enhancements
         fTable.setHeaderVisible(true);
         fTable.setLinesVisible(true);
 
         // Set the columns
         setColumnHeaders(columnData);
 
         // Set the default column field ids if this is not a subclass
         if (Arrays.equals(columnData, COLUMN_DATA)) {
             fTable.getColumns()[0].setData(Key.FIELD_ID, ITmfEvent.EVENT_FIELD_TIMESTAMP);
             fTable.getColumns()[1].setData(Key.FIELD_ID, ITmfEvent.EVENT_FIELD_SOURCE);
             fTable.getColumns()[2].setData(Key.FIELD_ID, ITmfEvent.EVENT_FIELD_TYPE);
             fTable.getColumns()[3].setData(Key.FIELD_ID, ITmfEvent.EVENT_FIELD_REFERENCE);
             fTable.getColumns()[4].setData(Key.FIELD_ID, ITmfEvent.EVENT_FIELD_CONTENT);
         }
 
         // Set the frozen row for header row
         fTable.setFrozenRowCount(1);
 
         // Create the header row cell editor
         createHeaderEditor();
 
         // Handle the table item selection
         fTable.addSelectionListener(new SelectionAdapter() {
             @Override
             public void widgetSelected(final SelectionEvent e) {
                 final TableItem[] selection = fTable.getSelection();
                 if (selection.length > 0) {
                     final TableItem selectedTableItem = selection[0];
                     if (selectedTableItem != null) {
                         if (selectedTableItem.getData(Key.RANK) instanceof Long) {
                             fSelectedRank = (Long) selectedTableItem.getData(Key.RANK);
                             fRawViewer.selectAndReveal((Long) selectedTableItem.getData(Key.RANK));
                         }
                         if (selectedTableItem.getData(Key.TIMESTAMP) instanceof TmfTimestamp) {
                             final TmfTimestamp ts = (TmfTimestamp) selectedTableItem.getData(Key.TIMESTAMP);
                             broadcast(new TmfTimeSynchSignal(TmfEventsTable.this, ts));
                         }
                     }
                 }
             }
         });
 
         cacheSize = Math.max(cacheSize, Display.getDefault().getBounds().height / fTable.getItemHeight());
         cacheSize = Math.min(cacheSize, MAX_CACHE_SIZE);
         fCache = new TmfEventsCache(cacheSize, this);
 
         // Handle the table item requests
         fTable.addListener(SWT.SetData, new Listener() {
 
             @Override
             public void handleEvent(final Event event) {
 
                 final TableItem item = (TableItem) event.item;
                 int index = event.index - 1; // -1 for the header row
 
                 if (event.index == 0) {
                     setHeaderRowItemData(item);
                     return;
                 }
 
                 if (fTable.getData(Key.FILTER_OBJ) != null) {
                     if ((event.index == 1) || (event.index == (fTable.getItemCount() - 1))) {
                         setFilterStatusRowItemData(item);
                         return;
                     }
                     index = index - 1; // -1 for top filter status row
                 }
 
                 final CachedEvent cachedEvent = fCache.getEvent(index);
                 if (cachedEvent != null) {
                     setItemData(item, cachedEvent.event, cachedEvent.rank);
                     return;
                 }
 
                 // Else, fill the cache asynchronously (and off the UI thread)
                 event.doit = false;
             }
         });
 
         fTable.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseDoubleClick(final MouseEvent event) {
                 if (event.button != 1) {
                     return;
                 }
                 // Identify the selected row
                 final Point point = new Point(event.x, event.y);
                 final TableItem item = fTable.getItem(point);
                 if (item != null) {
                     final Rectangle imageBounds = item.getImageBounds(0);
                     imageBounds.width = BOOKMARK_IMAGE.getBounds().width;
                     if (imageBounds.contains(point)) {
                         final Long rank = (Long) item.getData(Key.RANK);
                         if (rank != null) {
                             toggleBookmark(rank);
                         }
                     }
                 }
             }
         });
 
         final Listener tooltipListener = new Listener () {
             Shell tooltipShell = null;
             @Override
             public void handleEvent(final Event event) {
                 switch (event.type) {
                     case SWT.MouseHover:
                         final TableItem item = fTable.getItem(new Point(event.x, event.y));
                         if (item == null) {
                             return;
                         }
                         final Long rank = (Long) item.getData(Key.RANK);
                         if (rank == null) {
                             return;
                         }
                         final String tooltipText = (String) item.getData(Key.BOOKMARK);
                         final Rectangle bounds = item.getImageBounds(0);
                         bounds.width = BOOKMARK_IMAGE.getBounds().width;
                         if (!bounds.contains(event.x,event.y)) {
                             return;
                         }
                         if ((tooltipShell != null) && !tooltipShell.isDisposed()) {
                             tooltipShell.dispose();
                         }
                         tooltipShell = new Shell(fTable.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
                         tooltipShell.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                         final FillLayout layout = new FillLayout();
                         layout.marginWidth = 2;
                         tooltipShell.setLayout(layout);
                         final Label label = new Label(tooltipShell, SWT.WRAP);
                         String text = rank.toString() + (tooltipText != null ? ": " + tooltipText : ""); //$NON-NLS-1$ //$NON-NLS-2$
                         label.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
                         label.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                         label.setText(text);
                         label.addListener(SWT.MouseExit, this);
                         label.addListener(SWT.MouseDown, this);
                         label.addListener(SWT.MouseWheel, this);
                         final Point size = tooltipShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                        /*
                         * Bug in Linux.  The coordinates of the event have an origin that excludes the table header but
                         * the method toDisplay() expects coordinates relative to an origin that includes the table header.
                         */
                        int y = event.y;
                        if (System.getProperty("os.name").contains("Linux")) { //$NON-NLS-1$ //$NON-NLS-2$
                            y += fTable.getHeaderHeight();
                        }
                        Point pt = fTable.toDisplay(event.x, y);
                         pt.x += BOOKMARK_IMAGE.getBounds().width;
                         pt.y += size.y;
                         tooltipShell.setBounds(pt.x, pt.y, size.x, size.y);
                         tooltipShell.setVisible(true);
                         break;
                     case SWT.Dispose:
                     case SWT.KeyDown:
                     case SWT.MouseMove:
                     case SWT.MouseExit:
                     case SWT.MouseDown:
                     case SWT.MouseWheel:
                         if (tooltipShell != null) {
                             tooltipShell.dispose();
                             tooltipShell = null;
                         }
                         break;
                 }
             }
         };
 
         fTable.addListener(SWT.MouseHover, tooltipListener);
         fTable.addListener(SWT.Dispose, tooltipListener);
         fTable.addListener(SWT.KeyDown, tooltipListener);
         fTable.addListener(SWT.MouseMove, tooltipListener);
         fTable.addListener(SWT.MouseExit, tooltipListener);
         fTable.addListener(SWT.MouseDown, tooltipListener);
         fTable.addListener(SWT.MouseWheel, tooltipListener);
 
         // Create resources
         createResources();
 
         ColorSettingsManager.addColorSettingsListener(this);
 
         fTable.setItemCount(1); // +1 for header row
 
         fRawViewer = new TmfRawEventViewer(fSashForm, SWT.H_SCROLL | SWT.V_SCROLL);
 
         fRawViewer.addSelectionListener(new Listener() {
             @Override
             public void handleEvent(final Event e) {
                 if (e.data instanceof Long) {
                     final long rank = (Long) e.data;
                     int index = (int) rank;
                     if (fTable.getData(Key.FILTER_OBJ) != null) {
                         index = fCache.getFilteredEventIndex(rank) + 1; // +1 for top filter status row
                     }
                     fTable.setSelection(index + 1); // +1 for header row
                     fSelectedRank = rank;
                 } else if (e.data instanceof ITmfLocation<?>) {
                     // DOES NOT WORK: rank undefined in context from seekLocation()
                     // ITmfLocation<?> location = (ITmfLocation<?>) e.data;
                     // TmfContext context = fTrace.seekLocation(location);
                     // fTable.setSelection((int) context.getRank());
                     return;
                 } else {
                     return;
                 }
                 final TableItem[] selection = fTable.getSelection();
                 if ((selection != null) && (selection.length > 0)) {
                     final TmfTimestamp ts = (TmfTimestamp) fTable.getSelection()[0].getData(Key.TIMESTAMP);
                     if (ts != null) {
                         broadcast(new TmfTimeSynchSignal(TmfEventsTable.this, ts));
                     }
                 }
             }
         });
 
         fSashForm.setWeights(new int[] { 1, 1 });
         fRawViewer.setVisible(false);
 
         createPopupMenu();
     }
 
     protected void createPopupMenu() {
         final IAction showTableAction = new Action(Messages.TmfEventsTable_ShowTableActionText) {
             @Override
             public void run() {
                 fTable.setVisible(true);
                 fSashForm.layout();
             }
         };
 
         final IAction hideTableAction = new Action(Messages.TmfEventsTable_HideTableActionText) {
             @Override
             public void run() {
                 fTable.setVisible(false);
                 fSashForm.layout();
             }
         };
 
         final IAction showRawAction = new Action(Messages.TmfEventsTable_ShowRawActionText) {
             @Override
             public void run() {
                 fRawViewer.setVisible(true);
                 fSashForm.layout();
                 final int index = fTable.getSelectionIndex();
                 if (index >= +1) {
                     fRawViewer.selectAndReveal(index - 1);
                 }
             }
         };
 
         final IAction hideRawAction = new Action(Messages.TmfEventsTable_HideRawActionText) {
             @Override
             public void run() {
                 fRawViewer.setVisible(false);
                 fSashForm.layout();
             }
         };
 
         final IAction showSearchBarAction = new Action(Messages.TmfEventsTable_ShowSearchBarActionText) {
             @Override
             public void run() {
                 fHeaderState = HeaderState.SEARCH;
                 fTable.refresh();
             }
         };
 
         final IAction showFilterBarAction = new Action(Messages.TmfEventsTable_ShowFilterBarActionText) {
             @Override
             public void run() {
                 fHeaderState = HeaderState.FILTER;
                 fTable.refresh();
             }
         };
 
         final IAction clearFiltersAction = new Action(Messages.TmfEventsTable_ClearFiltersActionText) {
             @Override
             public void run() {
                 stopFilterThread();
                 stopSearchThread();
                 clearFilters();
             }
         };
 
         class ToggleBookmarkAction extends Action {
             long fRank;
 
             public ToggleBookmarkAction(final String text, final long rank) {
                 super(text);
                 fRank = rank;
             }
 
             @Override
             public void run() {
                 toggleBookmark(fRank);
             }
         }
 
         final MenuManager tablePopupMenu = new MenuManager();
         tablePopupMenu.setRemoveAllWhenShown(true);
         tablePopupMenu.addMenuListener(new IMenuListener() {
             @Override
             public void menuAboutToShow(final IMenuManager manager) {
                 if (fTable.getSelectionIndex() == 0) {
                     // Right-click on header row
                     if (fHeaderState == HeaderState.FILTER) {
                         tablePopupMenu.add(showSearchBarAction);
                     } else {
                         tablePopupMenu.add(showFilterBarAction);
                     }
                     return;
                 }
                 final Point point = fTable.toControl(Display.getDefault().getCursorLocation());
                 final TableItem item = fTable.getSelection().length > 0 ? fTable.getSelection()[0] : null;
                 if (item != null) {
                     final Rectangle imageBounds = item.getImageBounds(0);
                     imageBounds.width = BOOKMARK_IMAGE.getBounds().width;
                     if (point.x <= (imageBounds.x + imageBounds.width)) {
                         // Right-click on left margin
                         final Long rank = (Long) item.getData(Key.RANK);
                         if ((rank != null) && (fBookmarksFile != null)) {
                             if (fBookmarksMap.containsKey(rank)) {
                                 tablePopupMenu.add(new ToggleBookmarkAction(
                                         Messages.TmfEventsTable_RemoveBookmarkActionText, rank));
                             } else {
                                 tablePopupMenu.add(new ToggleBookmarkAction(
                                         Messages.TmfEventsTable_AddBookmarkActionText, rank));
                             }
                         }
                         return;
                     }
                 }
                 // Right-click on table
                 if (fTable.isVisible() && fRawViewer.isVisible()) {
                     tablePopupMenu.add(hideTableAction);
                     tablePopupMenu.add(hideRawAction);
                 } else if (!fTable.isVisible()) {
                     tablePopupMenu.add(showTableAction);
                 } else if (!fRawViewer.isVisible()) {
                     tablePopupMenu.add(showRawAction);
                 }
                 tablePopupMenu.add(new Separator());
                 tablePopupMenu.add(clearFiltersAction);
                 final ITmfFilterTreeNode[] savedFilters = FilterManager.getSavedFilters();
                 if (savedFilters.length > 0) {
                     final MenuManager subMenu = new MenuManager(Messages.TmfEventsTable_ApplyPresetFilterMenuName);
                     for (final ITmfFilterTreeNode node : savedFilters) {
                         if (node instanceof TmfFilterNode) {
                             final TmfFilterNode filter = (TmfFilterNode) node;
                             subMenu.add(new Action(filter.getFilterName()) {
                                 @Override
                                 public void run() {
                                     stopFilterThread();
                                     fFilterMatchCount = 0;
                                     fFilterCheckCount = 0;
                                     fCache.applyFilter(filter);
                                     fTable.clearAll();
                                     fTable.setData(Key.FILTER_OBJ, filter);
                                     fTable.setItemCount(3); // +1 for header row, +2 for top and bottom filter status rows
                                     startFilterThread();
                                     fireFilterApplied(filter);
                                 }
                             });
                         }
                     }
                     tablePopupMenu.add(subMenu);
                 }
                 appendToTablePopupMenu(tablePopupMenu, item);
             }
         });
 
         final MenuManager rawViewerPopupMenu = new MenuManager();
         rawViewerPopupMenu.setRemoveAllWhenShown(true);
         rawViewerPopupMenu.addMenuListener(new IMenuListener() {
             @Override
             public void menuAboutToShow(final IMenuManager manager) {
                 if (fTable.isVisible() && fRawViewer.isVisible()) {
                     rawViewerPopupMenu.add(hideTableAction);
                     rawViewerPopupMenu.add(hideRawAction);
                 } else if (!fTable.isVisible()) {
                     rawViewerPopupMenu.add(showTableAction);
                 } else if (!fRawViewer.isVisible()) {
                     rawViewerPopupMenu.add(showRawAction);
                 }
                 appendToRawPopupMenu(tablePopupMenu);
             }
         });
 
         Menu menu = tablePopupMenu.createContextMenu(fTable);
         fTable.setMenu(menu);
 
         menu = rawViewerPopupMenu.createContextMenu(fRawViewer);
         fRawViewer.setMenu(menu);
     }
 
     protected void appendToTablePopupMenu(final MenuManager tablePopupMenu, final TableItem selectedItem) {
         // override to append more actions
     }
 
     protected void appendToRawPopupMenu(final MenuManager rawViewerPopupMenu) {
         // override to append more actions
     }
 
     @Override
     public void dispose() {
         stopSearchThread();
         stopFilterThread();
         ColorSettingsManager.removeColorSettingsListener(this);
         fComposite.dispose();
         if ((fTrace != null) && fDisposeOnClose) {
             fTrace.dispose();
         }
         fResourceManager.dispose();
         super.dispose();
     }
 
     public void setLayoutData(final Object layoutData) {
         fComposite.setLayoutData(layoutData);
     }
 
     public TmfVirtualTable getTable() {
         return fTable;
     }
 
     /**
      * @param columnData
      *
      * FIXME: Add support for column selection
      */
     protected void setColumnHeaders(final ColumnData[] columnData) {
         fTable.setColumnHeaders(columnData);
     }
 
     protected void setItemData(final TableItem item, final ITmfEvent event, final long rank) {
         final ITmfEventField[] fields = extractItemFields(event);
         final String[] content = new String[fields.length];
         for (int i = 0; i < fields.length; i++) {
             content[i] = fields[i].getValue() != null ? fields[i].getValue().toString() : ""; //$NON-NLS-1$
         }
         item.setText(content);
         item.setData(Key.TIMESTAMP, new TmfTimestamp(event.getTimestamp()));
         item.setData(Key.RANK, rank);
 
         boolean bookmark = false;
         final Long markerId = fBookmarksMap.get(rank);
         if (markerId != null) {
             bookmark = true;
             try {
                 final IMarker marker = fBookmarksFile.findMarker(markerId);
                 item.setData(Key.BOOKMARK, marker.getAttribute(IMarker.MESSAGE));
             } catch (final CoreException e) {
                 displayException(e);
             }
         } else {
             item.setData(Key.BOOKMARK, null);
         }
 
         boolean searchMatch = false;
         boolean searchNoMatch = false;
         final ITmfFilter searchFilter = (ITmfFilter) fTable.getData(Key.SEARCH_OBJ);
         if (searchFilter != null) {
             if (searchFilter.matches(event)) {
                 searchMatch = true;
             } else {
                 searchNoMatch = true;
             }
         }
 
         final ColorSetting colorSetting = ColorSettingsManager.getColorSetting(event);
         if (searchNoMatch) {
             item.setForeground(colorSetting.getDimmedForegroundColor());
             item.setBackground(colorSetting.getDimmedBackgroundColor());
         } else {
             item.setForeground(colorSetting.getForegroundColor());
             item.setBackground(colorSetting.getBackgroundColor());
         }
 
         if (searchMatch) {
             if (bookmark) {
                 item.setImage(SEARCH_MATCH_BOOKMARK_IMAGE);
             } else {
                 item.setImage(SEARCH_MATCH_IMAGE);
             }
         } else if (bookmark) {
             item.setImage(BOOKMARK_IMAGE);
         } else {
             item.setImage((Image) null);
         }
     }
 
     protected void setHeaderRowItemData(final TableItem item) {
         String txtKey = null;
         if (fHeaderState == HeaderState.SEARCH) {
             item.setImage(SEARCH_IMAGE);
             txtKey = Key.SEARCH_TXT;
         } else if (fHeaderState == HeaderState.FILTER) {
             item.setImage(FILTER_IMAGE);
             txtKey = Key.FILTER_TXT;
         }
         item.setForeground(fGrayColor);
         for (int i = 0; i < fTable.getColumns().length; i++) {
             final TableColumn column = fTable.getColumns()[i];
             final String filter = (String) column.getData(txtKey);
             if (filter == null) {
                 if (fHeaderState == HeaderState.SEARCH) {
                     item.setText(i, SEARCH_HINT);
                 } else if (fHeaderState == HeaderState.FILTER) {
                     item.setText(i, FILTER_HINT);
                 }
                 item.setForeground(i, fGrayColor);
                 item.setFont(i, fTable.getFont());
             } else {
                 item.setText(i, filter);
                 item.setForeground(i, fGreenColor);
                 item.setFont(i, fBoldFont);
             }
         }
     }
 
     protected void setFilterStatusRowItemData(final TableItem item) {
         for (int i = 0; i < fTable.getColumns().length; i++) {
             if (i == 0) {
                 if ((fTrace == null) || (fFilterCheckCount == fTrace.getNbEvents())) {
                     item.setImage(FILTER_IMAGE);
                 } else {
                     item.setImage(STOP_IMAGE);
                 }
                 item.setText(0, fFilterMatchCount + "/" + fFilterCheckCount); //$NON-NLS-1$
             } else {
                 item.setText(i, ""); //$NON-NLS-1$
             }
         }
         item.setData(Key.TIMESTAMP, null);
         item.setData(Key.RANK, null);
         item.setForeground(null);
         item.setBackground(null);
     }
 
     protected void createHeaderEditor() {
         final TableEditor tableEditor = fTable.createTableEditor();
         tableEditor.horizontalAlignment = SWT.LEFT;
         tableEditor.verticalAlignment = SWT.CENTER;
         tableEditor.grabHorizontal = true;
         tableEditor.minimumWidth = 50;
 
         // Handle the header row selection
         fTable.addMouseListener(new MouseAdapter() {
             int columnIndex;
             TableColumn column;
             TableItem item;
 
             @Override
             public void mouseDown(final MouseEvent event) {
                 if (event.button != 1) {
                     return;
                 }
                 // Identify the selected row
                 final Point point = new Point(event.x, event.y);
                 item = fTable.getItem(point);
 
                 // Header row selected
                 if ((item != null) && (fTable.indexOf(item) == 0)) {
 
                     // Icon selected
                     if (item.getImageBounds(0).contains(point)) {
                         if (fHeaderState == HeaderState.SEARCH) {
                             fHeaderState = HeaderState.FILTER;
                         } else if (fHeaderState == HeaderState.FILTER) {
                             fHeaderState = HeaderState.SEARCH;
                         }
                         fTable.refresh();
                         return;
                     }
 
                     // Identify the selected column
                     columnIndex = -1;
                     for (int i = 0; i < fTable.getColumns().length; i++) {
                         final Rectangle rect = item.getBounds(i);
                         if (rect.contains(point)) {
                             columnIndex = i;
                             break;
                         }
                     }
 
                     if (columnIndex == -1) {
                         return;
                     }
 
                     column = fTable.getColumns()[columnIndex];
 
                     String txtKey = null;
                     if (fHeaderState == HeaderState.SEARCH) {
                         txtKey = Key.SEARCH_TXT;
                     } else if (fHeaderState == HeaderState.FILTER) {
                         txtKey = Key.FILTER_TXT;
                     }
 
                     // The control that will be the editor must be a child of the Table
                     final Text newEditor = (Text) fTable.createTableEditorControl(Text.class);
                     final String headerString = (String) column.getData(txtKey);
                     if (headerString != null) {
                         newEditor.setText(headerString);
                     }
                     newEditor.addFocusListener(new FocusAdapter() {
                         @Override
                         public void focusLost(final FocusEvent e) {
                             final boolean changed = updateHeader(newEditor.getText());
                             if (changed) {
                                 applyHeader();
                             }
                         }
                     });
                     newEditor.addKeyListener(new KeyAdapter() {
                         @Override
                         public void keyPressed(final KeyEvent e) {
                             if (e.character == SWT.CR) {
                                 updateHeader(newEditor.getText());
                                 applyHeader();
                             } else if (e.character == SWT.ESC) {
                                 tableEditor.getEditor().dispose();
                             }
                         }
                     });
                     newEditor.selectAll();
                     newEditor.setFocus();
                     tableEditor.setEditor(newEditor, item, columnIndex);
                 }
             }
 
             /*
              * returns true is value was changed
              */
             private boolean updateHeader(final String text) {
                 String objKey = null;
                 String txtKey = null;
                 if (fHeaderState == HeaderState.SEARCH) {
                     objKey = Key.SEARCH_OBJ;
                     txtKey = Key.SEARCH_TXT;
                 } else if (fHeaderState == HeaderState.FILTER) {
                     objKey = Key.FILTER_OBJ;
                     txtKey = Key.FILTER_TXT;
                 }
                 if (text.trim().length() > 0) {
                     try {
                         final String regex = TmfFilterMatchesNode.regexFix(text);
                         Pattern.compile(regex);
                         if (regex.equals(column.getData(txtKey))) {
                             tableEditor.getEditor().dispose();
                             return false;
                         }
                         final TmfFilterMatchesNode filter = new TmfFilterMatchesNode(null);
                         String fieldId = (String) column.getData(Key.FIELD_ID);
                         if (fieldId == null) {
                             fieldId = column.getText();
                         }
                         filter.setField(fieldId);
                         filter.setRegex(regex);
                         column.setData(objKey, filter);
                         column.setData(txtKey, regex);
                     } catch (final PatternSyntaxException ex) {
                         tableEditor.getEditor().dispose();
                         MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                                 ex.getDescription(), ex.getMessage());
                         return false;
                     }
                 } else {
                     if (column.getData(txtKey) == null) {
                         tableEditor.getEditor().dispose();
                         return false;
                     }
                     column.setData(objKey, null);
                     column.setData(txtKey, null);
                 }
                 return true;
             }
 
             private void applyHeader() {
                 stopSearchThread();
                 if (fHeaderState == HeaderState.SEARCH) {
                     final TmfFilterAndNode filter = new TmfFilterAndNode(null);
                     for (final TableColumn column : fTable.getColumns()) {
                         final Object filterObj = column.getData(Key.SEARCH_OBJ);
                         if (filterObj instanceof ITmfFilterTreeNode) {
                             filter.addChild((ITmfFilterTreeNode) filterObj);
                         }
                     }
                     if (filter.getChildrenCount() > 0) {
                         fTable.setData(Key.SEARCH_OBJ, filter);
                         fTable.refresh();
                         searchNext();
                         fireSearchApplied(filter);
                     } else {
                         fTable.setData(Key.SEARCH_OBJ, null);
                         fTable.refresh();
                         fireSearchApplied(null);
                     }
                 } else if (fHeaderState == HeaderState.FILTER) {
                     stopFilterThread();
                     fFilterMatchCount = 0;
                     fFilterCheckCount = 0;
                     final TmfFilterAndNode filter = new TmfFilterAndNode(null);
                     for (final TableColumn column : fTable.getColumns()) {
                         final Object filterObj = column.getData(Key.FILTER_OBJ);
                         if (filterObj instanceof ITmfFilterTreeNode) {
                             filter.addChild((ITmfFilterTreeNode) filterObj);
                         }
                     }
                     if (filter.getChildrenCount() > 0) {
                         fCache.applyFilter(filter);
                         fTable.clearAll();
                         fTable.setData(Key.FILTER_OBJ, filter);
                         fTable.setItemCount(3); // +1 for header row, +2 for top and bottom filter status rows
                         startFilterThread();
                         fireFilterApplied(filter);
                     } else {
                         fCache.clearFilter();
                         stopFilterThread();
                         fTable.clearAll();
                         fTable.setData(Key.FILTER_OBJ, null);
                         if (fTrace != null) {
                             fTable.setItemCount((int) fTrace.getNbEvents() + 1); // +1 for header row
                         } else {
                             fTable.setItemCount(1); // +1 for header row
                         }
                         fireFilterApplied(null);
                     }
                 }
 
                 tableEditor.getEditor().dispose();
             }
         });
 
         fTable.addKeyListener(new KeyAdapter() {
             @Override
             public void keyPressed(final KeyEvent e) {
                 e.doit = false;
                 if (e.character == SWT.ESC) {
                     stopFilterThread();
                     stopSearchThread();
                     fTable.refresh();
                 } else if (e.character == SWT.DEL) {
                     stopFilterThread();
                     stopSearchThread();
                     if (fHeaderState == HeaderState.SEARCH) {
                         for (final TableColumn column : fTable.getColumns()) {
                             column.setData(Key.SEARCH_OBJ, null);
                             column.setData(Key.SEARCH_TXT, null);
                         }
                         fTable.setData(Key.SEARCH_OBJ, null);
                         fTable.refresh();
                         fireSearchApplied(null);
                     } else if (fHeaderState == HeaderState.FILTER) {
                         clearFilters();
                     }
                 } else if (e.character == SWT.CR) {
                     if ((e.stateMask & SWT.SHIFT) == 0) {
                         searchNext();
                     } else {
                         searchPrevious();
                     }
                 }
             }
         });
     }
 
     protected void fireFilterApplied(final ITmfFilter filter) {
         for (final ITmfEventsFilterListener listener : fEventsFilterListeners) {
             listener.filterApplied(filter, fTrace);
         }
     }
 
     protected void fireSearchApplied(final ITmfFilter filter) {
         for (final ITmfEventsFilterListener listener : fEventsFilterListeners) {
             listener.searchApplied(filter, fTrace);
         }
     }
 
     protected void startFilterThread() {
         synchronized (fFilterSyncObj) {
             if (fFilterThread != null) {
                 fFilterThread.cancel();
             }
             final ITmfFilterTreeNode filter = (ITmfFilterTreeNode) fTable.getData(Key.FILTER_OBJ);
             fFilterThread = new FilterThread(filter);
             fFilterThread.start();
         }
     }
 
     protected void stopFilterThread() {
         synchronized (fFilterSyncObj) {
             if (fFilterThread != null) {
                 fFilterThread.cancel();
             }
         }
     }
 
     protected void clearFilters() {
         if (fTable.getData(Key.FILTER_OBJ) == null) {
             return;
         }
         fCache.clearFilter();
         fTable.clearAll();
         for (final TableColumn column : fTable.getColumns()) {
             column.setData(Key.FILTER_OBJ, null);
             column.setData(Key.FILTER_TXT, null);
         }
         fTable.setData(Key.FILTER_OBJ, null);
         if (fTrace != null) {
             fTable.setItemCount((int) fTrace.getNbEvents() + 1); // +1 for header row
         } else {
             fTable.setItemCount(1); // +1 for header row
         }
         fFilterMatchCount = 0;
         fFilterCheckCount = 0;
         if (fSelectedRank >= 0) {
             fTable.setSelection((int) fSelectedRank + 1); // +1 for header row
         } else {
             fTable.setSelection(0);
         }
         fireFilterApplied(null);
     }
 
     protected class FilterThread extends Thread {
         private final ITmfFilterTreeNode filter;
         private TmfEventRequest<ITmfEvent> request;
         private boolean refreshBusy = false;
         private boolean refreshPending = false;
         private final Object syncObj = new Object();
 
         public FilterThread(final ITmfFilterTreeNode filter) {
             super("Filter Thread"); //$NON-NLS-1$
             this.filter = filter;
         }
 
         @SuppressWarnings("unchecked")
         @Override
         public void run() {
             if (fTrace == null) {
                 return;
             }
             final int nbRequested = (int) (fTrace.getNbEvents() - fFilterCheckCount);
             if (nbRequested <= 0) {
                 return;
             }
             request = new TmfEventRequest<ITmfEvent>(ITmfEvent.class, TmfTimeRange.ETERNITY, (int) fFilterCheckCount,
                     nbRequested, fTrace.getCacheSize(), ExecutionType.BACKGROUND) {
                 @Override
                 public void handleData(final ITmfEvent event) {
                     super.handleData(event);
                     if (request.isCancelled()) {
                         return;
                     }
                     if (filter.matches(event)) {
                         final long rank = fFilterCheckCount;
                         final int index = (int) fFilterMatchCount;
                         fFilterMatchCount++;
                         fCache.storeEvent(event.clone(), rank, index);
                         refreshTable();
                     } else if ((fFilterCheckCount % 100) == 0) {
                         refreshTable();
                     }
                     fFilterCheckCount++;
                 }
             };
             ((ITmfDataProvider<ITmfEvent>) fTrace).sendRequest(request);
             try {
                 request.waitForCompletion();
             } catch (final InterruptedException e) {
             }
             refreshTable();
         }
 
         public void refreshTable() {
             synchronized (syncObj) {
                 if (refreshBusy) {
                     refreshPending = true;
                     return;
                 } else {
                     refreshBusy = true;
                 }
             }
             Display.getDefault().asyncExec(new Runnable() {
                 @Override
                 public void run() {
                     if (request.isCancelled()) {
                         return;
                     }
                     if (fTable.isDisposed()) {
                         return;
                     }
                     fTable.setItemCount((int) fFilterMatchCount + 3); // +1 for header row, +2 for top and bottom filter status rows
                     fTable.refresh();
                     synchronized (syncObj) {
                         refreshBusy = false;
                         if (refreshPending) {
                             refreshPending = false;
                             refreshTable();
                         }
                     }
                 }
             });
         }
 
         public void cancel() {
             if (request != null) {
                 request.cancel();
             }
         }
     }
 
     protected void searchNext() {
         synchronized (fSearchSyncObj) {
             if (fSearchThread != null) {
                 return;
             }
             final ITmfFilterTreeNode searchFilter = (ITmfFilterTreeNode) fTable.getData(Key.SEARCH_OBJ);
             if (searchFilter == null) {
                 return;
             }
             final int selectionIndex = fTable.getSelectionIndex();
             int startIndex;
             if (selectionIndex > 0) {
                 startIndex = selectionIndex; // -1 for header row, +1 for next event
             } else {
                 // header row is selected, start at top event
                 startIndex = Math.max(0, fTable.getTopIndex() - 1); // -1 for header row
             }
             final ITmfFilterTreeNode eventFilter = (ITmfFilterTreeNode) fTable.getData(Key.FILTER_OBJ);
             if (eventFilter != null)
              {
                 startIndex = Math.max(0, startIndex - 1); // -1 for top filter status row
             }
             fSearchThread = new SearchThread(searchFilter, eventFilter, startIndex, fSelectedRank, Direction.FORWARD);
             fSearchThread.schedule();
         }
     }
 
     protected void searchPrevious() {
         synchronized (fSearchSyncObj) {
             if (fSearchThread != null) {
                 return;
             }
             final ITmfFilterTreeNode searchFilter = (ITmfFilterTreeNode) fTable.getData(Key.SEARCH_OBJ);
             if (searchFilter == null) {
                 return;
             }
             final int selectionIndex = fTable.getSelectionIndex();
             int startIndex;
             if (selectionIndex > 0) {
                 startIndex = selectionIndex - 2; // -1 for header row, -1 for previous event
             } else {
                 // header row is selected, start at precedent of top event
                 startIndex = fTable.getTopIndex() - 2; // -1 for header row, -1 for previous event
             }
             final ITmfFilterTreeNode eventFilter = (ITmfFilterTreeNode) fTable.getData(Key.FILTER_OBJ);
             if (eventFilter != null)
              {
                 startIndex = startIndex - 1; // -1 for top filter status row
             }
             fSearchThread = new SearchThread(searchFilter, eventFilter, startIndex, fSelectedRank, Direction.BACKWARD);
             fSearchThread.schedule();
         }
     }
 
     protected void stopSearchThread() {
         fPendingGotoRank = -1;
         synchronized (fSearchSyncObj) {
             if (fSearchThread != null) {
                 fSearchThread.cancel();
                 fSearchThread = null;
             }
         }
     }
 
     protected class SearchThread extends Job {
         protected ITmfFilterTreeNode searchFilter;
         protected ITmfFilterTreeNode eventFilter;
         protected int startIndex;
         protected int direction;
         protected long rank;
         protected long foundRank = -1;
         protected TmfDataRequest<ITmfEvent> request;
 
         public SearchThread(final ITmfFilterTreeNode searchFilter, final ITmfFilterTreeNode eventFilter, final int startIndex,
                 final long currentRank, final int direction) {
             super(Messages.TmfEventsTable_SearchingJobName);
             this.searchFilter = searchFilter;
             this.eventFilter = eventFilter;
             this.startIndex = startIndex;
             this.rank = currentRank;
             this.direction = direction;
         }
 
         @SuppressWarnings("unchecked")
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
             if (fTrace == null) {
                 return Status.OK_STATUS;
             }
             final Display display = Display.getDefault();
             if (startIndex < 0) {
                 rank = (int) fTrace.getNbEvents() - 1;
             } else if (startIndex >= (fTable.getItemCount() - (eventFilter == null ? 1 : 3))) {
                 // for top and bottom
                 // filter status rows
                 rank = 0;
             } else {
                 int idx = startIndex;
                 while (foundRank == -1) {
                     final CachedEvent event = fCache.peekEvent(idx);
                     if (event == null) {
                         break;
                     }
                     rank = event.rank;
                     if (searchFilter.matches(event.event) && ((eventFilter == null) || eventFilter.matches(event.event))) {
                         foundRank = event.rank;
                         break;
                     }
                     if (direction == Direction.FORWARD) {
                         idx++;
                     } else {
                         idx--;
                     }
                 }
                 if (foundRank == -1) {
                     if (direction == Direction.FORWARD) {
                         rank++;
                         if (rank > (fTrace.getNbEvents() - 1)) {
                             rank = 0;
                         }
                     } else {
                         rank--;
                         if (rank < 0) {
                             rank = (int) fTrace.getNbEvents() - 1;
                         }
                     }
                 }
             }
             final int startRank = (int) rank;
             boolean wrapped = false;
             while (!monitor.isCanceled() && (foundRank == -1) && (fTrace != null)) {
                 int nbRequested = (direction == Direction.FORWARD ? Integer.MAX_VALUE : Math.min((int) rank + 1, fTrace.getCacheSize()));
                 if (direction == Direction.BACKWARD) {
                     rank = Math.max(0, rank - fTrace.getCacheSize() + 1);
                 }
                 request = new TmfDataRequest<ITmfEvent>(ITmfEvent.class, (int) rank, nbRequested) {
                     long currentRank = rank;
 
                     @Override
                     public void handleData(final ITmfEvent event) {
                         super.handleData(event);
                         if (searchFilter.matches(event) && ((eventFilter == null) || eventFilter.matches(event))) {
                             foundRank = currentRank;
                             if (direction == Direction.FORWARD) {
                                 done();
                                 return;
                             }
                         }
                         currentRank++;
                     }
                 };
                 ((ITmfDataProvider<ITmfEvent>) fTrace).sendRequest(request);
                 try {
                     request.waitForCompletion();
                     if (request.isCancelled()) {
                         return Status.OK_STATUS;
                     }
                 } catch (final InterruptedException e) {
                     synchronized (fSearchSyncObj) {
                         fSearchThread = null;
                     }
                     return Status.OK_STATUS;
                 }
                 if (foundRank == -1) {
                     if (direction == Direction.FORWARD) {
                         if (rank == 0) {
                             synchronized (fSearchSyncObj) {
                                 fSearchThread = null;
                             }
                             return Status.OK_STATUS;
                         } else {
                             nbRequested = (int) rank;
                             rank = 0;
                             wrapped = true;
                         }
                     } else {
                         rank--;
                         if (rank < 0) {
                             rank = (int) fTrace.getNbEvents() - 1;
                             wrapped = true;
                         }
                         if ((rank <= startRank) && wrapped) {
                             synchronized (fSearchSyncObj) {
                                 fSearchThread = null;
                             }
                             return Status.OK_STATUS;
                         }
                     }
                 }
             }
             int index = (int) foundRank;
             if (eventFilter != null) {
                 index = fCache.getFilteredEventIndex(foundRank);
             }
             final int selection = index + 1 + (eventFilter != null ? +1 : 0); // +1 for header row, +1 for top filter status row
 
             display.asyncExec(new Runnable() {
                 @Override
                 public void run() {
                     if (monitor.isCanceled()) {
                         return;
                     }
                     if (fTable.isDisposed()) {
                         return;
                     }
                     fTable.setSelection(selection);
                     fSelectedRank = foundRank;
                     synchronized (fSearchSyncObj) {
                         fSearchThread = null;
                     }
                 }
             });
             return Status.OK_STATUS;
         }
 
         @Override
         protected void canceling() {
             request.cancel();
             synchronized (fSearchSyncObj) {
                 fSearchThread = null;
             }
         }
     }
 
     protected void createResources() {
         fGrayColor = fResourceManager.createColor(ColorUtil.blend(fTable.getBackground().getRGB(), fTable
                 .getForeground().getRGB()));
         fGreenColor = fTable.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
         fBoldFont = fResourceManager.createFont(FontDescriptor.createFrom(fTable.getFont()).setStyle(SWT.BOLD));
     }
 
     protected void packColumns() {
         if (fPackDone) {
             return;
         }
         for (final TableColumn column : fTable.getColumns()) {
             final int headerWidth = column.getWidth();
             column.pack();
             if (column.getWidth() < headerWidth) {
                 column.setWidth(headerWidth);
             }
         }
         fPackDone = true;
     }
 
     /**
      * @param event
      * @return
      *
      *         FIXME: Add support for column selection
      */
     protected ITmfEventField[] extractItemFields(final ITmfEvent event) {
         ITmfEventField[] fields = new TmfEventField[0];
         if (event != null) {
             final String timestamp = event.getTimestamp().toString();
             final String source = event.getSource();
             final String type = event.getType().getName();
             final String reference = event.getReference();
             final ITmfEventField content = event.getContent();
             final String value = (content.getValue() != null) ? content.getValue().toString() : content.toString();
             fields = new TmfEventField[] {
                     new TmfEventField(ITmfEvent.EVENT_FIELD_TIMESTAMP, timestamp),
                     new TmfEventField(ITmfEvent.EVENT_FIELD_SOURCE, source),
                     new TmfEventField(ITmfEvent.EVENT_FIELD_TYPE, type),
                     new TmfEventField(ITmfEvent.EVENT_FIELD_REFERENCE, reference),
                     new TmfEventField(ITmfEvent.EVENT_FIELD_CONTENT, value)
             };
         }
         return fields;
     }
 
     public void setFocus() {
         fTable.setFocus();
     }
 
     /**
      * @param trace
      * @param disposeOnClose
      *            true if the trace should be disposed when the table is disposed
      */
     public void setTrace(final ITmfTrace<?> trace, final boolean disposeOnClose) {
         if ((fTrace != null) && fDisposeOnClose) {
             fTrace.dispose();
         }
         fTrace = trace;
         fPackDone = false;
         fSelectedRank = 0;
         fDisposeOnClose = disposeOnClose;
 
         // Perform the updates on the UI thread
         fTable.getDisplay().syncExec(new Runnable() {
             @Override
             public void run() {
                 fTable.removeAll();
                 fCache.setTrace(fTrace); // Clear the cache
                 if (fTrace != null) {
                     if (!fTable.isDisposed() && (fTrace != null)) {
                         if (fTable.getData(Key.FILTER_OBJ) == null) {
                             fTable.setItemCount((int) fTrace.getNbEvents() + 1); // +1 for header row
                         } else {
                             stopFilterThread();
                             fFilterMatchCount = 0;
                             fFilterCheckCount = 0;
                             fTable.setItemCount(3); // +1 for header row, +2 for top and bottom filter status rows
                             startFilterThread();
                         }
                     }
                     fRawViewer.setTrace(fTrace);
                 }
             }
         });
     }
 
     // ------------------------------------------------------------------------
     // Event cache
     // ------------------------------------------------------------------------
 
     public void cacheUpdated(final boolean completed) {
         synchronized (fCacheUpdateSyncObj) {
             if (fCacheUpdateBusy) {
                 fCacheUpdatePending = true;
                 fCacheUpdateCompleted = completed;
                 return;
             } else {
                 fCacheUpdateBusy = true;
             }
         }
         // Event cache is now updated. Perform update on the UI thread
         if (!fTable.isDisposed()) {
             fTable.getDisplay().asyncExec(new Runnable() {
                 @Override
                 public void run() {
                     if (!fTable.isDisposed()) {
                         fTable.refresh();
                         packColumns();
                     }
                     if (completed) {
                         populateCompleted();
                     }
                     synchronized (fCacheUpdateSyncObj) {
                         fCacheUpdateBusy = false;
                         if (fCacheUpdatePending) {
                             fCacheUpdatePending = false;
                             cacheUpdated(fCacheUpdateCompleted);
                         }
                     }
                 }
             });
         }
     }
 
     protected void populateCompleted() {
         // Nothing by default;
     }
 
     // ------------------------------------------------------------------------
     // Bookmark handling
     // ------------------------------------------------------------------------
 
     public void addBookmark(final IFile bookmarksFile) {
         fBookmarksFile = bookmarksFile;
         final TableItem[] selection = fTable.getSelection();
         if (selection.length > 0) {
             final TableItem tableItem = selection[0];
             if (tableItem.getData(Key.RANK) != null) {
                 final StringBuffer defaultMessage = new StringBuffer();
                 for (int i = 0; i < fTable.getColumns().length; i++) {
                     if (i > 0)
                      {
                         defaultMessage.append(", "); //$NON-NLS-1$
                     }
                     defaultMessage.append(tableItem.getText(i));
                 }
                 final InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                         Messages.TmfEventsTable_AddBookmarkDialogTitle, Messages.TmfEventsTable_AddBookmarkDialogText,
                         defaultMessage.toString(), null);
                 if (dialog.open() == Dialog.OK) {
                     final String message = dialog.getValue();
                     try {
                         final IMarker bookmark = bookmarksFile.createMarker(IMarker.BOOKMARK);
                         if (bookmark.exists()) {
                             bookmark.setAttribute(IMarker.MESSAGE, message.toString());
                             final long rank = (Long) tableItem.getData(Key.RANK);
                             final int location = (int) rank;
                             bookmark.setAttribute(IMarker.LOCATION, (Integer) location);
                             fBookmarksMap.put(rank, bookmark.getId());
                             fTable.refresh();
                         }
                     } catch (final CoreException e) {
                         displayException(e);
                     }
                 }
             }
         }
 
     }
 
     public void removeBookmark(final IMarker bookmark) {
         for (final Entry<Long, Long> entry : fBookmarksMap.entrySet()) {
             if (entry.getValue().equals(bookmark.getId())) {
                 fBookmarksMap.remove(entry.getKey());
                 fTable.refresh();
                 return;
             }
         }
     }
 
     private void toggleBookmark(final long rank) {
         if (fBookmarksFile == null) {
             return;
         }
         if (fBookmarksMap.containsKey(rank)) {
             final Long markerId = fBookmarksMap.remove(rank);
             fTable.refresh();
             try {
                 final IMarker bookmark = fBookmarksFile.findMarker(markerId);
                 if (bookmark != null) {
                     bookmark.delete();
                 }
             } catch (final CoreException e) {
                 displayException(e);
             }
         } else {
             addBookmark(fBookmarksFile);
         }
     }
 
     public void refreshBookmarks(final IFile bookmarksFile) {
         fBookmarksFile = bookmarksFile;
         if (bookmarksFile == null) {
             fBookmarksMap.clear();
             fTable.refresh();
             return;
         }
         try {
             fBookmarksMap.clear();
             for (final IMarker bookmark : bookmarksFile.findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO)) {
                 final int location = bookmark.getAttribute(IMarker.LOCATION, -1);
                 if (location != -1) {
                     final long rank = location;
                     fBookmarksMap.put(rank, bookmark.getId());
                 }
             }
             fTable.refresh();
         } catch (final CoreException e) {
             displayException(e);
         }
     }
 
     @Override
     public void gotoMarker(final IMarker marker) {
         final int rank = marker.getAttribute(IMarker.LOCATION, -1);
         if (rank != -1) {
             int index = rank;
             if (fTable.getData(Key.FILTER_OBJ) != null) {
                 index = fCache.getFilteredEventIndex(rank) + 1; // +1 for top filter status row
             } else if (rank >= fTable.getItemCount()) {
                 fPendingGotoRank = rank;
             }
             fTable.setSelection(index + 1); // +1 for header row
         }
     }
 
     // ------------------------------------------------------------------------
     // Listeners
     // ------------------------------------------------------------------------
 
     /*
      * (non-Javadoc)
      *
      * @see org.eclipse.linuxtools.tmf.ui.views.colors.IColorSettingsListener#colorSettingsChanged(org.eclipse.linuxtools.tmf.ui.views.colors.ColorSetting[])
      */
     @Override
     public void colorSettingsChanged(final ColorSetting[] colorSettings) {
         fTable.refresh();
     }
 
     @Override
     public void addEventsFilterListener(final ITmfEventsFilterListener listener) {
         if (!fEventsFilterListeners.contains(listener)) {
             fEventsFilterListeners.add(listener);
         }
     }
 
     @Override
     public void removeEventsFilterListener(final ITmfEventsFilterListener listener) {
         fEventsFilterListeners.remove(listener);
     }
 
     // ------------------------------------------------------------------------
     // Signal handlers
     // ------------------------------------------------------------------------
 
     @TmfSignalHandler
     public void experimentUpdated(final TmfExperimentUpdatedSignal signal) {
         if ((signal.getExperiment() != fTrace) || fTable.isDisposed()) {
             return;
         }
         // Perform the refresh on the UI thread
         Display.getDefault().asyncExec(new Runnable() {
             @Override
             public void run() {
                 if (!fTable.isDisposed() && (fTrace != null)) {
                     if (fTable.getData(Key.FILTER_OBJ) == null) {
                         fTable.setItemCount((int) fTrace.getNbEvents() + 1); // +1 for header row
                         if ((fPendingGotoRank != -1) && ((fPendingGotoRank + 1) < fTable.getItemCount())) { // +1 for header row
                             fTable.setSelection((int) fPendingGotoRank + 1); // +1 for header row
                             fPendingGotoRank = -1;
                         }
                     } else {
                         startFilterThread();
                     }
                 }
                 if (!fRawViewer.isDisposed() && (fTrace != null)) {
                     fRawViewer.refreshEventCount();
                 }
             }
         });
     }
 
     @TmfSignalHandler
     public void traceUpdated(final TmfTraceUpdatedSignal signal) {
         if ((signal.getTrace() != fTrace) || fTable.isDisposed()) {
             return;
         }
         // Perform the refresh on the UI thread
         Display.getDefault().asyncExec(new Runnable() {
             @Override
             public void run() {
                 if (!fTable.isDisposed() && (fTrace != null)) {
                     if (fTable.getData(Key.FILTER_OBJ) == null) {
                         fTable.setItemCount((int) fTrace.getNbEvents() + 1); // +1 for header row
                         if ((fPendingGotoRank != -1) && ((fPendingGotoRank + 1) < fTable.getItemCount())) { // +1 for header row
                             fTable.setSelection((int) fPendingGotoRank + 1); // +1 for header row
                             fPendingGotoRank = -1;
                         }
                     } else {
                         startFilterThread();
                     }
                 }
                 if (!fRawViewer.isDisposed() && (fTrace != null)) {
                     fRawViewer.refreshEventCount();
                 }
             }
         });
     }
 
     @SuppressWarnings("unchecked")
     @TmfSignalHandler
     public void currentTimeUpdated(final TmfTimeSynchSignal signal) {
         if ((signal.getSource() != this) && (fTrace != null) && (!fTable.isDisposed())) {
 
             // Create a request for one event that will be queued after other ongoing requests. When this request is completed
             // do the work to select the actual event with the timestamp specified in the signal. This procedure prevents
             // the method fTrace.getRank() from interfering and delaying ongoing requests.
             final TmfDataRequest<ITmfEvent> subRequest = new TmfDataRequest<ITmfEvent>(ITmfEvent.class, 0, 1, ExecutionType.FOREGROUND) {
 
                 TmfTimestamp ts = new TmfTimestamp(signal.getCurrentTime());
 
                 @Override
                 public void handleData(final ITmfEvent event) {
                     super.handleData(event);
                 }
 
                 @Override
                 public void handleCompleted() {
                     super.handleCompleted();
                     if (fTrace == null) {
                         return;
                     }
 
                     // Verify if the event is within the trace range and adjust if necessary
                     ITmfTimestamp timestamp = ts;
                     if (timestamp.compareTo(fTrace.getStartTime(), true) == -1) {
                         timestamp = fTrace.getStartTime();
                     }
                     if (timestamp.compareTo(fTrace.getEndTime(), true) == 1) {
                         timestamp = fTrace.getEndTime();
                     }
 
                     // Get the rank of the selected event in the table
                     final ITmfContext context = fTrace.seekEvent(timestamp);
                     final long rank = context.getRank();
                     fSelectedRank = rank;
 
                     fTable.getDisplay().asyncExec(new Runnable() {
                         @Override
                         public void run() {
                             // Return if table is disposed
                             if (fTable.isDisposed()) {
                                 return;
                             }
 
                             int index = (int) rank;
                             if (fTable.isDisposed()) {
                                 return;
                             }
                             if (fTable.getData(Key.FILTER_OBJ) != null)
                              {
                                 index = fCache.getFilteredEventIndex(rank) + 1; // +1 for top filter status row
                             }
                             fTable.setSelection(index + 1); // +1 for header row
                             fRawViewer.selectAndReveal(rank);
                         }
                     });
                 }
             };
 
             ((ITmfDataProvider<ITmfEvent>) fTrace).sendRequest(subRequest);
         }
     }
 
     // ------------------------------------------------------------------------
     // Error handling
     // ------------------------------------------------------------------------
 
     /**
      * Display an exception in a message box
      *
      * @param e the exception
      */
     private void displayException(final Exception e) {
         final MessageBox mb = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
         mb.setText(e.getClass().getName());
         mb.setMessage(e.getMessage());
         mb.open();
     }
 
 }
