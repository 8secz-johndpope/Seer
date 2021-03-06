 /*******************************************************************************
  * Copyright (c) 2000, 2006 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     IBM Corporation - initial API and implementation
 *     Tom Schindl <tom.schindl@bestsolution.at> - concept of ViewerRow
  *******************************************************************************/
 
 package org.eclipse.jface.viewers;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.List;
 
 import org.eclipse.core.runtime.Assert;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.TableEditor;
 import org.eclipse.swt.events.MouseAdapter;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.graphics.Rectangle;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Item;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.swt.widgets.TableItem;
 import org.eclipse.swt.widgets.Widget;
 
 /**
  * A concrete viewer based on a SWT <code>Table</code> control.
  * <p>
  * This class is not intended to be subclassed outside the viewer framework. It
  * is designed to be instantiated with a pre-existing SWT table control and
  * configured with a domain-specific content provider, table label provider,
  * element filter (optional), and element sorter (optional).
  * </p>
  * <p>
  * Label providers for table viewers must implement either the
  * <code>ITableLabelProvider</code> or the <code>ILabelProvider</code>
  * interface (see <code>TableViewer.setLabelProvider</code> for more details).
  * </p>
  * <p>
  * As of 3.1 the TableViewer now supports the SWT.VIRTUAL flag. If the
  * underlying table is SWT.VIRTUAL, the content provider may implement
  * {@link ILazyContentProvider} instead of {@link IStructuredContentProvider}.
  * Note that in this case, the viewer does not support sorting or filtering.
  * Also note that in this case, the Widget based APIs may return null if the
  * element is not specified or not created yet.
  * </p>
  * <p>
  * Users of SWT.VIRTUAL should also avoid using getItems() from the Table within
  * the TreeViewer as this does not necessarily generate a callback for the
  * TreeViewer to populate the items. It also has the side effect of creating all
  * of the items thereby eliminating the performance improvements of SWT.VIRTUAL.
  * </p>
  * 
  * @see SWT#VIRTUAL
  * @see #doFindItem(Object)
  * @see #internalRefresh(Object, boolean)
  */
 public class TableViewer extends ColumnViewer {
 
 	private class VirtualManager {
 
 		/**
 		 * The currently invisible elements as provided by the content provider
 		 * or by addition. This will not be populated by an
 		 * ILazyStructuredContentProvider as an ILazyStructuredContentProvider
 		 * is only queried on the virtual callback.
 		 */
 		private Object[] cachedElements = new Object[0];
 
 		/**
 		 * Create a new instance of the receiver.
 		 * 
 		 */
 		public VirtualManager() {
 			addTableListener();
 		}
 
 		/**
 		 * Add the listener for SetData on the table
 		 */
 		private void addTableListener() {
 			table.addListener(SWT.SetData, new Listener() {
 				/*
 				 * (non-Javadoc)
 				 * 
 				 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
 				 */
 				public void handleEvent(Event event) {
 					TableItem item = (TableItem) event.item;
 					final int index = table.indexOf(item);
 					Object element = resolveElement(index);
 					if (element == null) {
 						// Didn't find it so make a request
 						// Keep looking if it is not in the cache.
 						IContentProvider contentProvider = getContentProvider();
 						// If we are building lazily then request lookup now
 						if (contentProvider instanceof ILazyContentProvider) {
 							((ILazyContentProvider) contentProvider)
 									.updateElement(index);
 							return;
 						}
 					}
 
 					associate(element, item);
 					updateItem(item, element);
 				}
 
 			});
 		}
 
 		/**
 		 * Get the element at index.Resolve it lazily if this is available.
 		 * 
 		 * @param index
 		 * @return Object or <code>null</code> if it could not be found
 		 */
 		protected Object resolveElement(int index) {
 
 			Object element = null;
 			if (index < cachedElements.length) {
 				element = cachedElements[index];
 			}
 
 			return element;
 		}
 
 		/**
 		 * A non visible item has been added.
 		 * 
 		 * @param element
 		 * @param index
 		 */
 		public void notVisibleAdded(Object element, int index) {
 
 			int requiredCount = getTable().getItemCount() + 1;
 
 			Object[] newCache = new Object[requiredCount];
 			System.arraycopy(cachedElements, 0, newCache, 0, index);
 			if (index < cachedElements.length) {
 				System.arraycopy(cachedElements, index, newCache, index + 1,
 						cachedElements.length - index);
 			}
 			newCache[index] = element;
 			cachedElements = newCache;
 
 			getTable().setItemCount(requiredCount);
 		}
 
 		/**
 		 * The elements with the given indices need to be removed from the
 		 * cache.
 		 * 
 		 * @param indices
 		 */
 		public void removeIndices(int[] indices) {
 			if (indices.length == 1) {
 				removeIndicesFromTo(indices[0], indices[0]);
 			}
 			int requiredCount = getTable().getItemCount() - indices.length;
 
 			Arrays.sort(indices);
 			Object[] newCache = new Object[requiredCount];
 			int indexInNewCache = 0;
 			int nextToSkip = 0;
 			for (int i = 0; i < cachedElements.length; i++) {
 				if (nextToSkip < indices.length && i == indices[nextToSkip]) {
 					nextToSkip++;
 				} else {
 					newCache[indexInNewCache++] = cachedElements[i];
 				}
 			}
 			cachedElements = newCache;
 		}
 
 		/**
 		 * The elements between the given indices (inclusive) need to be removed
 		 * from the cache.
 		 * 
 		 * @param from
 		 * @param to
 		 */
 		public void removeIndicesFromTo(int from, int to) {
 			int indexAfterTo = to + 1;
 			Object[] newCache = new Object[cachedElements.length
 					- (indexAfterTo - from)];
 			System.arraycopy(cachedElements, 0, newCache, 0, from);
 			if (indexAfterTo < cachedElements.length) {
 				System.arraycopy(cachedElements, indexAfterTo, newCache, from,
 						cachedElements.length - indexAfterTo);
 			}
 		}
 
 		/**
 		 * @param element
 		 * @return the index of the element in the cache, or null
 		 */
 		public int find(Object element) {
 			return Arrays.asList(cachedElements).indexOf(element);
 		}
 
 		/**
 		 * @param count
 		 */
 		public void adjustCacheSize(int count) {
 			if (count == cachedElements.length) {
 				return;
 			} else if (count < cachedElements.length) {
 				Object[] newCache = new Object[count];
 				System.arraycopy(cachedElements, 0, newCache, 0, count);
 				cachedElements = newCache;
 			} else {
 				Object[] newCache = new Object[count];
 				System.arraycopy(cachedElements, 0, newCache, 0,
 						cachedElements.length);
 				cachedElements = newCache;
 			}
 		}
 
 	}
 
 	private VirtualManager virtualManager;
 
 	/**
 	 * Internal table viewer implementation.
 	 */
 	private TableEditorImpl tableViewerImpl;
 
 	/**
 	 * This viewer's table control.
 	 */
 	private Table table;
 
 	/**
 	 * This viewer's table editor.
 	 */
 	private TableEditor tableEditor;
 
 	/**
 	 * Creates a table viewer on a newly-created table control under the given
 	 * parent. The table control is created using the SWT style bits
 	 * <code>MULTI, H_SCROLL, V_SCROLL,</code> and <code>BORDER</code>. The
 	 * viewer has no input, no content provider, a default label provider, no
 	 * sorter, and no filters. The table has no columns.
 	 * 
 	 * @param parent
 	 *            the parent control
 	 */
 	public TableViewer(Composite parent) {
 		this(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
 	}
 
 	/**
 	 * Creates a table viewer on a newly-created table control under the given
 	 * parent. The table control is created using the given style bits. The
 	 * viewer has no input, no content provider, a default label provider, no
 	 * sorter, and no filters. The table has no columns.
 	 * 
 	 * @param parent
 	 *            the parent control
 	 * @param style
 	 *            SWT style bits
 	 */
 	public TableViewer(Composite parent, int style) {
 		this(new Table(parent, style));
 	}
 
 	/**
 	 * Creates a table viewer on the given table control. The viewer has no
 	 * input, no content provider, a default label provider, no sorter, and no
 	 * filters.
 	 * 
 	 * @param table
 	 *            the table control
 	 */
 	public TableViewer(Table table) {
 		this.table = table;
 		hookControl(table);
 		tableEditor = new TableEditor(table);
 		initTableViewerImpl();
 		initializeVirtualManager(table.getStyle());
 	}
 
 	/**
 	 * Initialize the virtual manager to manage the virtual state if the table
 	 * is VIRTUAL. If not use the default no-op version.
 	 * 
 	 * @param style
 	 */
 	private void initializeVirtualManager(int style) {
 		if ((style & SWT.VIRTUAL) == 0) {
 			return;
 		}
 
 		virtualManager = new VirtualManager();
 	}
 
 	/**
 	 * Adds the given elements to this table viewer. If this viewer does not
 	 * have a sorter, the elements are added at the end in the order given;
 	 * otherwise the elements are inserted at appropriate positions.
 	 * <p>
 	 * This method should be called (by the content provider) when elements have
 	 * been added to the model, in order to cause the viewer to accurately
 	 * reflect the model. This method only affects the viewer, not the model.
 	 * </p>
 	 * 
 	 * @param elements
 	 *            the elements to add
 	 */
 	public void add(Object[] elements) {
 		assertElementsNotNull(elements);
 		Object[] filtered = filter(elements);
 
 		for (int i = 0; i < filtered.length; i++) {
 			Object element = filtered[i];
 			int index = indexForElement(element);
 			createItem(element, index);
 		}
 	}
 
 	/**
 	 * Create a new TableItem at index if required.
 	 * 
 	 * @param element
 	 * @param index
 	 * 
 	 * @since 3.1
 	 */
 	private void createItem(Object element, int index) {
 		if (virtualManager == null) {
 			updateItem(createNewRowPart(SWT.NONE, index).getItem(), element);
 		} else {
 			virtualManager.notVisibleAdded(element, index);
 
 		}
 	}
 
 	/**
 	 * Adds the given element to this table viewer. If this viewer does not have
 	 * a sorter, the element is added at the end; otherwise the element is
 	 * inserted at the appropriate position.
 	 * <p>
 	 * This method should be called (by the content provider) when a single
 	 * element has been added to the model, in order to cause the viewer to
 	 * accurately reflect the model. This method only affects the viewer, not
 	 * the model. Note that there is another method for efficiently processing
 	 * the simultaneous addition of multiple elements.
 	 * </p>
 	 * 
 	 * @param element
 	 *            the element to add
 	 */
 	public void add(Object element) {
 		add(new Object[] { element });
 	}
 
 	/**
 	 * Cancels a currently active cell editor. All changes already done in the
 	 * cell editor are lost.
 	 */
 	public void cancelEditing() {
 		tableViewerImpl.cancelEditing();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindInputItem(java.lang.Object)
 	 */
 	protected Widget doFindInputItem(Object element) {
 		if (equals(element, getRoot())) {
 			return getTable();
 		}
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindItem(java.lang.Object)
 	 */
 	protected Widget doFindItem(Object element) {
 
 		TableItem[] children = table.getItems();
 		for (int i = 0; i < children.length; i++) {
 			TableItem item = children[i];
 			Object data = item.getData();
 			if (data != null && equals(data, element)) {
 				return item;
 			}
 		}
 
 		return null;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#doUpdateItem(org.eclipse.swt.widgets.Widget,
 	 *      java.lang.Object, boolean)
 	 */
 	protected void doUpdateItem(Widget widget, Object element, boolean fullMap) {
 		if (widget instanceof TableItem) {
 			final TableItem item = (TableItem) widget;
 
 			// remember element we are showing
 			if (fullMap) {
 				associate(element, item);
 			} else {
 				Object data = item.getData();
 				if (data != null) {
 					unmapElement(data, item);
 				}
 				item.setData(element);
 				mapElement(element, item);
 			}
 
 			int columnCount = table.getColumnCount();
 			if (columnCount == 0)
 				columnCount = 1;// If there are no columns do the first one
 
 			// Also enter loop if no columns added. See 1G9WWGZ: JFUIF:WINNT -
 			// TableViewer with 0 columns does not work
 			for (int column = 0; column < columnCount || column == 0; column++) {
 				ViewerColumn columnViewer = getViewerColumn(column);
 				columnViewer.refresh(updateCell(getRowPartFromItem(item),
 						column));
 
 				// As it is possible for user code to run the event
 				// loop check here.
 				if (item.isDisposed()) {
 					unmapElement(element, item);
 					return;
 				}
 
 			}
 
 		}
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.ColumnViewer#getColumnViewerOwner(int)
 	 */
 	protected Widget getColumnViewerOwner(int columnIndex) {
 		if (columnIndex < 0 || columnIndex > getTable().getColumnCount()) {
 			return null;
 		}
 
 		if (getTable().getColumnCount() == 0)// Hang it off the table if it
 			return getTable();
 
 		return getTable().getColumn(columnIndex);
 	}
 
 	/**
 	 * Set the TableColumnViewerPart at columnIndex to be viewerPart.
 	 * 
 	 * @param viewerPart
 	 * @param columnIndex
 	 */
 	public void setColumnPart(ViewerColumn viewerPart, int columnIndex) {
 		TableColumn column = getTable().getColumn(columnIndex);
 		column.setData(ViewerColumn.COLUMN_VIEWER_KEY, viewerPart);
 	}
 
 	/**
 	 * Starts editing the given element.
 	 * 
 	 * @param element
 	 *            the element
 	 * @param column
 	 *            the column number
 	 */
 	public void editElement(Object element, int column) {
 		tableViewerImpl.editElement(element, column);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.ColumnViewer#getCellEditors()
 	 */
 	public CellEditor[] getCellEditors() {
 		return tableViewerImpl.getCellEditors();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.ColumnViewer#getCellModifier()
 	 */
 	public ICellModifier getCellModifier() {
 		return tableViewerImpl.getCellModifier();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.ColumnViewer#getColumnProperties()
 	 */
 	public Object[] getColumnProperties() {
 		return tableViewerImpl.getColumnProperties();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.Viewer#getControl()
 	 */
 	public Control getControl() {
 		return table;
 	}
 
 	/**
 	 * Returns the element with the given index from this table viewer. Returns
 	 * <code>null</code> if the index is out of range.
 	 * <p>
 	 * This method is internal to the framework.
 	 * </p>
 	 * 
 	 * @param index
 	 *            the zero-based index
 	 * @return the element at the given index, or <code>null</code> if the
 	 *         index is out of range
 	 */
 	public Object getElementAt(int index) {
 		if (index >= 0 && index < table.getItemCount()) {
 			TableItem i = table.getItem(index);
 			if (i != null) {
 				return i.getData();
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * The table viewer implementation of this <code>Viewer</code> framework
 	 * method returns the label provider, which in the case of table viewers
 	 * will be an instance of either <code>ITableLabelProvider</code> or
 	 * <code>ILabelProvider</code>. If it is an
 	 * <code>ITableLabelProvider</code>, then it provides a separate label
 	 * text and image for each column. If it is an <code>ILabelProvider</code>,
 	 * then it provides only the label text and image for the first column, and
 	 * any remaining columns are blank.
 	 */
 	public IBaseLabelProvider getLabelProvider() {
 		return super.getLabelProvider();
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#getSelectionFromWidget()
 	 */
 	protected List getSelectionFromWidget() {
 		if (virtualManager != null) {
 			return getVirtualSelection();
 		}
 		Widget[] items = table.getSelection();
 		ArrayList list = new ArrayList(items.length);
 		for (int i = 0; i < items.length; i++) {
 			Widget item = items[i];
 			Object e = item.getData();
 			if (e != null) {
 				list.add(e);
 			}
 		}
 		return list;
 	}
 
 	/**
 	 * Get the virtual selection. Avoid calling SWT whenever possible to prevent
 	 * extra widget creation.
 	 * 
 	 * @return List of Object
 	 */
 
 	private List getVirtualSelection() {
 
 		List result = new ArrayList();
 		int[] selectionIndices = getTable().getSelectionIndices();
 		if (getContentProvider() instanceof ILazyContentProvider) {
 			ILazyContentProvider lazy = (ILazyContentProvider) getContentProvider();
 			for (int i = 0; i < selectionIndices.length; i++) {
 				int selectionIndex = selectionIndices[i];
 				lazy.updateElement(selectionIndex);// Start the update
 				Object element = getTable().getItem(selectionIndex).getData();
 				// Only add the element if it got updated.
 				// If this is done deferred the selection will
 				// be incomplete until selection is finished.
 				if (element != null) {
 					result.add(element);
 				}
 			}
 		} else {
 			for (int i = 0; i < selectionIndices.length; i++) {
 				Object element = null;
 				// See if it is cached
 				int selectionIndex = selectionIndices[i];
 				if (selectionIndex < virtualManager.cachedElements.length) {
 					element = virtualManager.cachedElements[selectionIndex];
 				}
 				if (element == null) {
 					// Not cached so try the item's data
 					TableItem item = getTable().getItem(selectionIndex);
 					element = item.getData();
 				}
 				if (element != null) {
 					result.add(element);
 				}
 			}
 
 		}
 		return result;
 	}
 
 	/**
 	 * Returns this table viewer's table control.
 	 * 
 	 * @return the table control
 	 */
 	public Table getTable() {
 		return table;
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.ContentViewer#hookControl(org.eclipse.swt.widgets.Control)
 	 */
 	protected void hookControl(Control control) {
 		super.hookControl(control);
 		Table tableControl = (Table) control;
 		tableControl.addMouseListener(new MouseAdapter() {
 			public void mouseDown(MouseEvent e) {
 				tableViewerImpl.handleMouseDown(e);
 			}
 		});
 	}
 
 	/*
 	 * Returns the index where the item should be inserted.
 	 */
 	protected int indexForElement(Object element) {
 		ViewerComparator comparator = getComparator();
 		if (comparator == null) {
 			return table.getItemCount();
 		}
 		int count = table.getItemCount();
 		int min = 0, max = count - 1;
 		while (min <= max) {
 			int mid = (min + max) / 2;
 			Object data = table.getItem(mid).getData();
 			int compare = comparator.compare(this, data, element);
 			if (compare == 0) {
 				// find first item > element
 				while (compare == 0) {
 					++mid;
 					if (mid >= count) {
 						break;
 					}
 					data = table.getItem(mid).getData();
 					compare = comparator.compare(this, data, element);
 				}
 				return mid;
 			}
 			if (compare < 0) {
 				min = mid + 1;
 			} else {
 				max = mid - 1;
 			}
 		}
 		return min;
 	}
 
 	/**
 	 * Initializes the table viewer implementation.
 	 */
 	private void initTableViewerImpl() {
 		tableViewerImpl = new TableEditorImpl(this) {
 			Rectangle getBounds(Item item, int columnNumber) {
 				return ((TableItem) item).getBounds(columnNumber);
 			}
 
 			int getColumnCount() {
 				return getTable().getColumnCount();
 			}
 
 			Item[] getSelection() {
 				return getTable().getSelection();
 			}
 
 			void setEditor(Control w, Item item, int columnNumber) {
 				tableEditor.setEditor(w, (TableItem) item, columnNumber);
 			}
 
 			void setSelection(StructuredSelection selection, boolean b) {
 				TableViewer.this.setSelection(selection, b);
 			}
 
 			void showSelection() {
 				getTable().showSelection();
 			}
 
 			void setLayoutData(CellEditor.LayoutData layoutData) {
 				tableEditor.grabHorizontal = layoutData.grabHorizontal;
 				tableEditor.horizontalAlignment = layoutData.horizontalAlignment;
 				tableEditor.minimumWidth = layoutData.minimumWidth;
 			}
 
 			void handleDoubleClickEvent() {
 				Viewer viewer = getViewer();
 				fireDoubleClick(new DoubleClickEvent(viewer, viewer
 						.getSelection()));
 				fireOpen(new OpenEvent(viewer, viewer.getSelection()));
 			}
 		};
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object,
 	 *      java.lang.Object)
 	 */
 	protected void inputChanged(Object input, Object oldInput) {
 		getControl().setRedraw(false);
 		try {
 			// refresh() attempts to preserve selection, which we want here
 			refresh();
 		} finally {
 			getControl().setRedraw(true);
 		}
 	}
 
 	/**
 	 * Inserts the given element into this table viewer at the given position.
 	 * If this viewer has a sorter, the position is ignored and the element is
 	 * inserted at the correct position in the sort order.
 	 * <p>
 	 * This method should be called (by the content provider) when elements have
 	 * been added to the model, in order to cause the viewer to accurately
 	 * reflect the model. This method only affects the viewer, not the model.
 	 * </p>
 	 * 
 	 * @param element
 	 *            the element
 	 * @param position
 	 *            a 0-based position relative to the model, or -1 to indicate
 	 *            the last position
 	 */
 	public void insert(Object element, int position) {
 		tableViewerImpl.applyEditorValue();
 		if (getComparator() != null || hasFilters()) {
 			add(element);
 			return;
 		}
 		if (position == -1) {
 			position = table.getItemCount();
 		}
 
 		createItem(element, position);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#internalRefresh(java.lang.Object)
 	 */
 	protected void internalRefresh(Object element) {
 		internalRefresh(element, true);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#internalRefresh(java.lang.Object,
 	 *      boolean)
 	 */
 	protected void internalRefresh(Object element, boolean updateLabels) {
 		tableViewerImpl.applyEditorValue();
 		if (element == null || equals(element, getRoot())) {
 			if (virtualManager == null) {
 				internalRefreshAll(updateLabels);
 			} else {
 				internalVirtualRefreshAll();
 			}
 		} else {
 			Widget w = findItem(element);
 			if (w != null) {
 				updateItem(w, element);
 			}
 		}
 	}
 
 	/**
 	 * Refresh all with virtual elements.
 	 * 
 	 * @since 3.1
 	 */
 	private void internalVirtualRefreshAll() {
 
 		Object root = getRoot();
 		IContentProvider contentProvider = getContentProvider();
 
 		// Invalidate for lazy
 		if (!(contentProvider instanceof ILazyContentProvider)
 				&& (contentProvider instanceof IStructuredContentProvider)) {
 			// Don't cache if the root is null but cache if it is not lazy.
 			if (root != null) {
 				virtualManager.cachedElements = ((IStructuredContentProvider) getContentProvider())
 						.getElements(root);
 				getTable().setItemCount(virtualManager.cachedElements.length);
 			}
 		}
 		getTable().clearAll();
 	}
 
 	/**
 	 * Refresh all of the elements of the table. update the labels if
 	 * updatLabels is true;
 	 * 
 	 * @param updateLabels
 	 * 
 	 * @since 3.1
 	 */
 	private void internalRefreshAll(boolean updateLabels) {
 		// the parent
 
 		// in the code below, it is important to do all disassociates
 		// before any associates, since a later disassociate can undo an
 		// earlier associate
 		// e.g. if (a, b) is replaced by (b, a), the disassociate of b to
 		// item 1 could undo
 		// the associate of b to item 0.
 
 		Object[] children = getSortedChildren(getRoot());
 		TableItem[] items = getTable().getItems();
 		int min = Math.min(children.length, items.length);
 		for (int i = 0; i < min; ++i) {
 
 			TableItem item = items[i];
 
 			// if the element is unchanged, update its label if appropriate
 			if (equals(children[i], item.getData())) {
 				if (updateLabels) {
 					updateItem(item, children[i]);
 				} else {
 					// associate the new element, even if equal to the old
 					// one,
 					// to remove stale references (see bug 31314)
 					associate(children[i], item);
 				}
 			} else {
 				// updateItem does an associate(...), which can mess up
 				// the associations if the order of elements has changed.
 				// E.g. (a, b) -> (b, a) first replaces a->0 with b->0, then
 				// replaces b->1 with a->1, but this actually removes b->0.
 				// So, if the object associated with this item has changed,
 				// just disassociate it for now, and update it below.
 				item.setText(""); //$NON-NLS-1$
 				item.setImage(new Image[Math.max(1, table.getColumnCount())]);// Clear
 				// all
 				// images
 				disassociate(item);
 			}
 		}
 		// dispose of all items beyond the end of the current elements
 		if (min < items.length) {
 			for (int i = items.length; --i >= min;) {
 
 				disassociate(items[i]);
 			}
 			if (virtualManager != null) {
 				virtualManager.removeIndicesFromTo(min, items.length - 1);
 			}
 			table.remove(min, items.length - 1);
 		}
 		// Workaround for 1GDGN4Q: ITPUI:WIN2000 - TableViewer icons get
 		// scrunched
 		if (table.getItemCount() == 0) {
 			table.removeAll();
 		}
 		// Update items which were removed above
 		for (int i = 0; i < min; ++i) {
 
 			TableItem item = items[i];
 			if (item.getData() == null) {
 				updateItem(item, children[i]);
 			}
 		}
 		// add any remaining elements
 		for (int i = min; i < children.length; ++i) {
 			createItem(children[i], i);
 		}
 	}
 
 	/**
 	 * Removes the given elements from this table viewer.
 	 * 
 	 * @param elements
 	 *            the elements to remove
 	 */
 	private void internalRemove(final Object[] elements) {
 		Object input = getInput();
 		for (int i = 0; i < elements.length; ++i) {
 			if (equals(elements[i], input)) {
 				setInput(null);
 				return;
 			}
 		}
 		// use remove(int[]) rather than repeated TableItem.dispose() calls
 		// to allow SWT to optimize multiple removals
 		int[] indices = new int[elements.length];
 		int count = 0;
 		for (int i = 0; i < elements.length; ++i) {
 			Widget w = findItem(elements[i]);
 			if (w == null && virtualManager != null) {
 				int index = virtualManager.find(elements[i]);
 				if (index != -1) {
 					indices[count++] = index;
 				}
 			} else if (w instanceof TableItem) {
 				TableItem item = (TableItem) w;
 				disassociate(item);
 				indices[count++] = table.indexOf(item);
 			}
 		}
 		if (count < indices.length) {
 			System.arraycopy(indices, 0, indices = new int[count], 0, count);
 		}
 		if (virtualManager != null) {
 			virtualManager.removeIndices(indices);
 		}
 		table.remove(indices);
 
 		// Workaround for 1GDGN4Q: ITPUI:WIN2000 - TableViewer icons get
 		// scrunched
 		if (table.getItemCount() == 0) {
 			table.removeAll();
 		}
 	}
 
 	/**
 	 * Returns whether there is an active cell editor.
 	 * 
 	 * @return <code>true</code> if there is an active cell editor, and
 	 *         <code>false</code> otherwise
 	 */
 	public boolean isCellEditorActive() {
 		return tableViewerImpl.isCellEditorActive();
 	}
 
 	/**
 	 * Removes the given elements from this table viewer. The selection is
 	 * updated if required.
 	 * <p>
 	 * This method should be called (by the content provider) when elements have
 	 * been removed from the model, in order to cause the viewer to accurately
 	 * reflect the model. This method only affects the viewer, not the model.
 	 * </p>
 	 * 
 	 * @param elements
 	 *            the elements to remove
 	 */
 	public void remove(final Object[] elements) {
 		assertElementsNotNull(elements);
 		if (elements.length == 0) {
 			return;
 		}
 		preservingSelection(new Runnable() {
 			public void run() {
 				internalRemove(elements);
 			}
 		});
 	}
 
 	/**
 	 * Removes the given element from this table viewer. The selection is
 	 * updated if necessary.
 	 * <p>
 	 * This method should be called (by the content provider) when a single
 	 * element has been removed from the model, in order to cause the viewer to
 	 * accurately reflect the model. This method only affects the viewer, not
 	 * the model. Note that there is another method for efficiently processing
 	 * the simultaneous removal of multiple elements.
 	 * </p>
 	 * <strong>NOTE:</strong> removing an object from a virtual table will
 	 * decrement the itemCount.
 	 * 
 	 * @param element
 	 *            the element
 	 */
 	public void remove(Object element) {
 		remove(new Object[] { element });
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#reveal(java.lang.Object)
 	 */
 	public void reveal(Object element) {
 		Assert.isNotNull(element);
 		Widget w = findItem(element);
 		if (w instanceof TableItem) {
 			getTable().showItem((TableItem) w);
 		}
 	}
 
 	/**
 	 * Sets the cell editors of this table viewer.
 	 * 
 	 * @param editors
 	 *            the list of cell editors
 	 */
 	public void setCellEditors(CellEditor[] editors) {
 		tableViewerImpl.setCellEditors(editors);
 	}
 
 	/**
 	 * Sets the cell modifier of this table viewer.
 	 * 
 	 * @param modifier
 	 *            the cell modifier
 	 */
 	public void setCellModifier(ICellModifier modifier) {
 		tableViewerImpl.setCellModifier(modifier);
 	}
 
 	/**
 	 * Sets the column properties of this table viewer. The properties must
 	 * correspond with the columns of the table control. They are used to
 	 * identify the column in a cell modifier.
 	 * 
 	 * @param columnProperties
 	 *            the list of column properties
 	 */
 	public void setColumnProperties(String[] columnProperties) {
 		tableViewerImpl.setColumnProperties(columnProperties);
 	}
 
 	/**
 	 * The table viewer implementation of this <code>Viewer</code> framework
 	 * method ensures that the given label provider is an instance of either
 	 * <code>ITableLabelProvider</code> or <code>ILabelProvider</code>.
 	 * <p>
 	 * If the label provider is an {@link ITableLabelProvider}, then it
 	 * provides a separate label text and image for each column. Implementers of
 	 * <code>ITableLabelProvider</code> may also implement
 	 * {@link ITableColorProvider} and/or {@link ITableFontProvider} to provide
 	 * colors and/or fonts.
 	 * </p>
 	 * <p>
 	 * If the label provider is an <code>ILabelProvider</code>, then it
 	 * provides only the label text and image for the first column, and any
 	 * remaining columns are blank. Implementers of <code>ILabelProvider</code>
 	 * may also implement {@link IColorProvider} and/or {@link IFontProvider} to
 	 * provide colors and/or fonts.
 	 * </p>
 	 * <p>
 	 * If the label provider implements the mixin interface ITooltipProvider, it
 	 * can provide custom tooltips.
 	 * </p>
 	 */
 	public void setLabelProvider(IBaseLabelProvider labelProvider) {
 		Assert.isTrue(labelProvider instanceof ITableLabelProvider
 				|| labelProvider instanceof ILabelProvider
 				|| labelProvider instanceof CellLabelProvider);
 		clearColumnParts();// Clear before refresh
 		super.setLabelProvider(labelProvider);
 	}
 
 	/**
 	 * Clear the viewer parts for the columns
 	 */
 	private void clearColumnParts() {
 		TableColumn[] columns = getTable().getColumns();
 		if (columns.length == 0)
 			getTable().setData(ViewerColumn.COLUMN_VIEWER_KEY, null);
 		else {
 			for (int i = 0; i < columns.length; i++) {
 				columns[i].setData(ViewerColumn.COLUMN_VIEWER_KEY, null);
 
 			}
 		}
 
 	}
 
 	/**
 	 * <p>
 	 * Sets a new selection for this viewer and optionally makes it visible. The
 	 * TableViewer implmentation of this method is ineffecient for the
 	 * ILazyContentProvider as lookup is done by indices rather than elements
 	 * and may require population of the entire table in worse case.
 	 * </p>
 	 * <p>
 	 * Use Table#setSelection(int[] indices) and Table#showSelection() if you
 	 * wish to set selection more effeciently when using a ILazyContentProvider.
 	 * </p>
 	 * 
 	 * @param selection
 	 *            the new selection
 	 * @param reveal
 	 *            <code>true</code> if the selection is to be made visible,
 	 *            and <code>false</code> otherwise
 	 * @see Table#setSelection(int[])
 	 * @see Table#showSelection()
 	 */
 	public void setSelection(ISelection selection, boolean reveal) {
 		super.setSelection(selection, reveal);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#setSelectionToWidget(java.util.List,
 	 *      boolean)
 	 */
 	protected void setSelectionToWidget(List list, boolean reveal) {
 
 		if (list == null) {
 			table.deselectAll();
 			return;
 		}
 
 		if (virtualManager != null) {
 			virtualSetSelectionToWidget(list, reveal);
 			return;
 		}
 
 		int size = list.size();
 		TableItem[] items = new TableItem[size];
 		int count = 0;
 		for (int i = 0; i < size; ++i) {
 			Object o = list.get(i);
 			Widget w = findItem(o);
 			if (w instanceof TableItem) {
 				TableItem item = (TableItem) w;
 				items[count++] = item;
 			}
 		}
 		if (count < size) {
 			System.arraycopy(items, 0, items = new TableItem[count], 0, count);
 		}
 		table.setSelection(items);
 
 		if (reveal) {
 			table.showSelection();
 		}
 
 	}
 
 	/**
 	 * Set the selection on a virtual table
 	 * 
 	 * @param list
 	 *            The elements to set
 	 * @param reveal
 	 *            Whether or not reveal the first item.
 	 */
 	private void virtualSetSelectionToWidget(List list, boolean reveal) {
 		int size = list.size();
 		int[] indices = new int[list.size()];
 
 		TableItem firstItem = null;
 		int count = 0;
 		HashSet virtualElements = new HashSet();
 		for (int i = 0; i < size; ++i) {
 			Object o = list.get(i);
 			Widget w = findItem(o);
 			if (w instanceof TableItem) {
 				TableItem item = (TableItem) w;
 				indices[count++] = getTable().indexOf(item);
 				if (firstItem == null) {
 					firstItem = item;
 				}
 			} else {
 				virtualElements.add(o);
 			}
 		}
 
 		if (getContentProvider() instanceof ILazyContentProvider) {
 			ILazyContentProvider provider = (ILazyContentProvider) getContentProvider();
 
 			// Now go through it again until all is done or we are no longer
 			// virtual
 			// This may create all items so it is not a good
 			// idea in general.
 			// Use #setSelection (int [] indices,boolean reveal) instead
 			for (int i = 0; virtualElements.size() > 0
 					&& i < getTable().getItemCount(); i++) {
 				provider.updateElement(i);
 				TableItem item = getTable().getItem(i);
 				if (virtualElements.contains(item.getData())) {
 					indices[count++] = i;
 					virtualElements.remove(item.getData());
 					if (firstItem == null) {
 						firstItem = item;
 					}
 				}
 			}
 		} else {
 
 			if (count != list.size()) {// As this is expensive skip it if all
 				// have been found
 				// If it is not lazy we can use the cache
 				for (int i = 0; i < virtualManager.cachedElements.length; i++) {
 					Object element = virtualManager.cachedElements[i];
 					if (virtualElements.contains(element)) {
 						TableItem item = getTable().getItem(i);
 						item.getText();// Be sure to fire the update
 						indices[count++] = i;
 						virtualElements.remove(element);
 						if (firstItem == null) {
 							firstItem = item;
 						}
 					}
 				}
 			}
 		}
 
 		if (count < size) {
 			System.arraycopy(indices, 0, indices = new int[count], 0, count);
 		}
 		table.setSelection(indices);
 
 		if (reveal && firstItem != null) {
 			table.showItem(firstItem);
 		}
 	}
 
 	/**
 	 * Set the item count of the receiver.
 	 * 
 	 * @param count
 	 *            the new table size.
 	 * 
 	 * @since 3.1
 	 */
 	public void setItemCount(int count) {
 		int oldCount = getTable().getItemCount();
 		if (count < oldCount) {
 			// need to disassociate elements that are being disposed
 			for (int i = count; i < oldCount; i++) {
 				TableItem item = getTable().getItem(i);
 				if (item.getData() != null) {
 					disassociate(item);
 				}
 			}
 		}
 		getTable().setItemCount(count);
 		if (virtualManager != null) {
 			virtualManager.adjustCacheSize(count);
 		}
 		getTable().redraw();
 	}
 
 	/**
 	 * Replace the entries starting at index with elements. This method assumes
 	 * all of these values are correct and will not call the content provider to
 	 * verify. <strong>Note that this method will create a TableItem for all of
 	 * the elements provided</strong>.
 	 * 
 	 * @param element
 	 * @param index
 	 * @see ILazyContentProvider
 	 * 
 	 * @since 3.1
 	 */
 	public void replace(Object element, int index) {
 		TableItem item = getTable().getItem(index);
 		refreshItem(item, element);
 	}
 
 	/**
 	 * Clear the table item at the specified index
 	 * 
 	 * @param index
 	 *            the index of the table item to be cleared
 	 * 
 	 * @since 3.1
 	 */
 	public void clear(int index) {
 		TableItem item = getTable().getItem(index);
 		if (item.getData() != null) {
 			disassociate(item);
 		}
 		table.clear(index);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#getRawChildren(java.lang.Object)
 	 */
 	protected Object[] getRawChildren(Object parent) {
 
 		Assert.isTrue(!(getContentProvider() instanceof ILazyContentProvider),
 				"Cannot get raw children with an ILazyContentProvider");//$NON-NLS-1$
 		return super.getRawChildren(parent);
 
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#assertContentProviderType(org.eclipse.jface.viewers.IContentProvider)
 	 */
 	protected void assertContentProviderType(IContentProvider provider) {
 		Assert.isTrue(provider instanceof IStructuredContentProvider
 				|| provider instanceof ILazyContentProvider);
 	}
 
 	/*
 	 * (non-Javadoc)
 	 * 
 	 * @see org.eclipse.jface.viewers.StructuredViewer#getRowPartFromItem(org.eclipse.swt.widgets.Widget)
 	 */
 	protected ViewerRow getRowPartFromItem(Widget item) {
 		ViewerRow part = (ViewerRow) item.getData(ViewerRow.ROWPART_KEY);
 
 		if (part == null) {
 			part = new TableViewerRow(((TableItem) item));
 		}
 
 		return part;
 	}
 
 	/**
 	 * Create a new row with style at index
 	 * 
 	 * @param style
 	 * @param rowIndex
 	 * @return ViewerRow
 	 */
 	private ViewerRow createNewRowPart(int style, int rowIndex) {
 		TableItem item;
 
 		if (rowIndex >= 0) {
 			item = new TableItem(table, style, rowIndex);
 		} else {
 			item = new TableItem(table, style);
 		}
 
 		return getRowPartFromItem(item);
 	}
 
 	/**
 	 * Returns the item at the given display-relative coordinates, or
 	 * <code>null</code> if there is no item at that location.
 	 * <p>
 	 * The default implementation of this method returns <code>null</code>.
 	 * </p>
 	 * 
 	 * @param x
 	 *            horizontal coordinate
 	 * @param y
 	 *            vertical coordinate
 	 * @return the item, or <code>null</code> if there is no item at the given
 	 *         coordinates
 	 */
 	protected Item getItem(int x, int y) {
		return table.getItem(new Point(x, y));
 	}
 
 }
