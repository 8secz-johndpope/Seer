 /******************************************************************************* 
  * Copyright (c) 2000, 2003 IBM Corporation and others. 
  * All rights reserved. This program and the accompanying materials! 
  * are made available under the terms of the Common Public License v1.0 
  * which accompanies this distribution, and is available at 
  * http://www.eclipse.org/legal/cpl-v10.html 
  * 
  * Contributors: 
  *        IBM Corporation - initial API and implementation 
  ******************************************************************************/package org.eclipse.jface.viewers;
 
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 
 import org.eclipse.core.runtime.Platform;
 
 import org.eclipse.swt.dnd.DragSource;
 import org.eclipse.swt.dnd.DragSourceListener;
 import org.eclipse.swt.dnd.DropTarget;
 import org.eclipse.swt.dnd.DropTargetListener;
 import org.eclipse.swt.dnd.Transfer;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.events.SelectionListener;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Item;
 import org.eclipse.swt.widgets.Widget;
 
 import org.eclipse.jface.util.Assert;
 import org.eclipse.jface.util.IOpenEventListener;
 import org.eclipse.jface.util.ListenerList;
 import org.eclipse.jface.util.OpenStrategy;
 import org.eclipse.jface.util.SafeRunnable;
 
 /**
  * Abstract base implementation for structure-oriented viewers (trees, lists, tables).
  * Supports custom sorting, filtering, and rendering.
  * <p>
  * Any number of viewer filters can be added to this viewer (using <code>addFilter</code>). 
  * When the viewer receives an update, it asks each of its filters if it is out of date,
  * and refilters elements as required.
  * </p>
  * @see ViewerFilter
  * @see ViewerSorter
  */
 public abstract class StructuredViewer extends ContentViewer {
 
 	/**
 	 * A map from the viewer's model elements to SWT widgets.
 	 * (key type: <code>Object</code>, value type: <code>Widget</code>).
 	 * <code>null</code> means that the element map is disabled.
 	 */
 	private CustomHashtable elementMap;
 
 	/**
 	 * The comparer to use for comparing elements,
 	 * or <code>null</code> to use the default <code>equals</code>
 	 * and <code>hashCode</code> methods on the element itself.
 	 */
 	private IElementComparer comparer;
 		
 	/**
 	 * This viewer's sorter.
 	 * <code>null</code> means there is no sorter.
 	 */
 	private ViewerSorter sorter;
 
 	/**
 	 * This viewer's filters (element type: <code>ViewerFilter</code>).
 	 * <code>null</code> means there are no filters.
 	 */
 	private List filters;
 
 	/**
 	 * Indicates whether a selection change is in progress on this
 	 * viewer.
 	 *
 	 * @see #setSelection(ISelection, boolean)
 	 */
 	private boolean inChange;
 
 	/**
 	 * Used while a selection change is in progress on this viewer
 	 * to indicates whether the selection should be restored.
 	 *
 	 * @see #setSelection(ISelection, boolean)
 	 */
 	private boolean restoreSelection;
 
 	/**
 	 * List of double-click state listeners (element type: <code>IDoubleClickListener</code>).
 	 * @see #fireDoubleClick
 	 */
 	private ListenerList doubleClickListeners = new ListenerList(1);
 	/**
 	 * List of open listeners (element type: <code>ISelectionActivateListener</code>).
 	 * @see #fireOpen
 	 */
 	private ListenerList openListeners = new ListenerList(1);
 	/**
 	 * List of post selection listeners (element type: <code>ISelectionActivateListener</code>).
 	 * @see #firePostSelectionChanged
 	 */
 	private ListenerList postSelectionChangedListeners = new ListenerList(1);
 	
 	/**
 	 * The safe runnable used to call the label provider.
 	 */
 	private	UpdateItemSafeRunnable safeUpdateItem = new UpdateItemSafeRunnable();
 	
 	class UpdateItemSafeRunnable extends SafeRunnable {
 		Object object;
 		Widget widget;
 		boolean fullMap;
 		boolean exception = false;
 		public void run() {
 			if(exception) return;
 			doUpdateItem(widget,object,fullMap);
 		}
 		public void handleException(Throwable e) {
 			super.handleException(e);
 			//If and unexpected exception happens, remove it
 			//to make sure the application keeps running.
 			exception = true;
 		}		
 	} 		
 	
 /**
  * Creates a structured element viewer. The viewer has no input, 
  * no content provider, a default label provider, no sorter, and no filters.
  */
 protected StructuredViewer() {
 }
 /**
  * Adds a listener for double-clicks in this viewer.
  * Has no effect if an identical listener is already registered.
  *
  * @param listener a double-click listener
  */
 public void addDoubleClickListener(IDoubleClickListener listener) {
 	doubleClickListeners.add(listener);
 }
 /**
  * Adds a listener for selection-open in this viewer.
  * Has no effect if an identical listener is already registered.
  *
  * @param listener a double-click listener
  */
 public void addOpenListener(IOpenListener listener) {
 	openListeners.add(listener);
 }
 /**
  * Adds a listener for post selection in this viewer.
  * It is equivalent to selection changed if the selection
  * was triggered by the mouse but it has a delay if the selection
  * is triggered by the keyboard arrows.
  * Has no effect if an identical listener is already registered.
  *
  * @param listener a double-click listener
  */
 public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
 	postSelectionChangedListeners.add(listener);
 }
 /**
  * Adds support for dragging items out of this viewer via
  * a user drag-and-drop operation.
  *
  * @param operations a bitwise OR of the supported drag and drop operation
  *  types (<code>DROP_COPY</code>, <code>DROP_LINK</code>, and <code>DROP_MOVE</code>)
  * @param transferTypes the transfer types that are supported by the drag operation
  * @param listener the callback that will be invoked to set the drag
  *  data and to cleanup after the drag and drop operation finishes
  * @see org.eclipse.swt.dnd.DND
  */
 public void addDragSupport(
 	int operations, 
 	Transfer[] transferTypes, 
 	DragSourceListener listener) {
 
 	Control myControl = getControl();
 	final DragSource dragSource = new DragSource(myControl, operations);
 	dragSource.setTransfer(transferTypes);
 	dragSource.addDragListener(listener);
 }
 /**
  * Adds support for dropping items into this viewer via
  * a user drag-and-drop operation.
  *
  * @param operations a bitwise OR of the supported drag and drop operation
  *  types (<code>DROP_COPY</code>, <code>DROP_LINK</code>, and <code>DROP_MOVE</code>)
  * @param transferTypes the transfer types that are supported by the drop operation
  * @param listener the callback that will be invoked after the drag and drop operation finishes
  * @see org.eclipse.swt.dnd.DND
  */
 public void addDropSupport(int operations, Transfer[] transferTypes, final DropTargetListener listener) {
 	Control control = getControl();
 	DropTarget dropTarget = new DropTarget(control, operations);
 	dropTarget.setTransfer(transferTypes);
 	dropTarget.addDropListener(listener);
 }
 /**
  * Adds the given filter to this viewer,
  * and triggers refiltering and resorting of the elements.
  *
  * @param filter a viewer filter
  */
 public void addFilter(ViewerFilter filter) {
 	if (filters == null)
 		filters = new ArrayList();
 	filters.add(filter);
 	refresh();
 }
 /**
  * Associates the given element with the given widget.
  * Sets the given item's data to be the element, and maps
  * the element to the item in the element map (if enabled).
  *
  * @param element the element
  * @param item the widget
  */
 protected void associate(Object element, Item item) {
 	Object data = item.getData();
 	if (data != element) {
 		if (data != null)
 			disassociate(item);
 		item.setData(element);
 	}
 	// Always map the element, even if data == element,
 	// since unmapAllElements() can leave the map inconsistent
 	// See bug 2741 for details.
 	mapElement(element, item);
 }
 /**
  * Disassociates the given SWT item from its corresponding element.
  * Sets the item's data to <code>null</code> and removes the element
  * from the element map (if enabled).
  *
  * @param item the widget
  */
 protected void disassociate(Item item) {
 	Object element = item.getData();
 	Assert.isNotNull(element);
 	item.setData(null);
 	unmapElement(element, item);
 }
 /**
  * Returns the widget in this viewer's control which 
  * represents the given element if it is the viewer's input.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param the element
  * @return the corresponding widget, or <code>null</code> if none
  */
 protected abstract Widget doFindInputItem(Object element);
 /**
  * Returns the widget in this viewer's control which represent the given element.
  * This method searchs all the children of the input element.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param the element
  * @return the corresponding widget, or <code>null</code> if none
  */
 protected abstract Widget doFindItem(Object element);
 /**
  * Copies the attributes of the given element into the given SWT item.
  * The element map is updated according to the value of <code>fullMap</code>.
  * If <code>fullMap</code> is <code>true</code> then the current mapping
  * from element to widgets is removed and the new mapping is added. 
  * If fullmap is <code>false</code> then only the new map gets installed.
  * Installing only the new map is necessary in cases where only
  * the order of elements changes but not the set of elements.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param item
  * @param element the element
  * @param fullMap <code>true</code> if mappings are added and removed,
  *   and <code>false</code> if only the new map gets installed
  */
 protected abstract void doUpdateItem(
 	Widget item,
 	Object element,
 	boolean fullMap);
 
 /**
  * Compares two elements for equality.
  * Uses the element comparer if one has been set, otherwise
  * uses the default <code>equals</code> method on the elements themselves.
  * 
  * @param elementA the first element
  * @param elementB the second element
  * @return whether elementA is equal to elementB
  */	
 protected boolean equals(Object elementA, Object elementB) {
 	if (comparer == null)
 		return elementA == null ? elementB == null : elementA.equals(elementB);
 	else
 		return elementA == null ? elementB == null : comparer.equals(elementA, elementB);
 }
 
 /**
  * Returns the result of running the given elements through the filters.
  *
  * @param elements the elements to filter
  * @return only the elements which all filters accept
  */
 protected Object[] filter(Object[] elements) {
 	if (filters != null) {
 		ArrayList filtered = new ArrayList(elements.length);
 		Object root = getRoot();
 		for (int i = 0; i < elements.length; i++) {
 			boolean add = true;
 			for (int j = 0; j < filters.size(); j++) {
 				add = ((ViewerFilter) filters.get(j)).select(this, root, elements[i]);
 				if (!add)
 					break;
 			}
 			if (add)
 				filtered.add(elements[i]);
 		}
 		return filtered.toArray();
 	}
 	return elements;
 }
 /**
  * Finds the widget which represents the given element.
  * <p>
  * The default implementation of this method tries first to
  * find the widget for the given element assuming that it is
  * the viewer's input; this is done by calling <code>doFindInputItem</code>.
  * If it is not found there, it is looked up in the internal element
  * map provided that this feature has been enabled.
  * If the element map is disabled, the widget is found via
  * <code>doFindInputItem</code>.
  * </p>
  *
  * @param element the element
  * @return the corresponding widget, or <code>null</code> if none
  */
 protected final Widget findItem(Object element) {
 	Widget result= doFindInputItem(element);
 	if (result != null)
 		return result;
 	// if we have an element map use it, otherwise search for the item.
 	if (elementMap != null) 
 		return (Widget)elementMap.get(element); 
 	return doFindItem(element);
 }
 /**
  * Notifies any double-click listeners that a double-click has been received.
  * Only listeners registered at the time this method is called are notified.
  *
  * @param event a double-click event
  *
  * @see IDoubleClickListener#doubleClick
  */
 protected void fireDoubleClick(final DoubleClickEvent event) {
 	Object[] listeners = doubleClickListeners.getListeners();
 	for (int i = 0; i < listeners.length; ++i) {
 		final IDoubleClickListener l = (IDoubleClickListener)listeners[i];
 		Platform.run(new SafeRunnable() {
 			public void run() {
 				l.doubleClick(event);
 			}
 			public void handleException(Throwable e) {
 				super.handleException(e);
 				//If and unexpected exception happens, remove it
 				//to make sure the workbench keeps running.
 				removeDoubleClickListener(l);
 			}
 		});			
 	}
 }
 /**
  * Notifies any open event listeners that a open event has been received.
  * Only listeners registered at the time this method is called are notified.
  *
  * @param event a double-click event
  *
 * @see IOpenListener#doubleClick
  */
 protected void fireOpen(final OpenEvent event) {
 	Object[] listeners = openListeners.getListeners();
 	for (int i = 0; i < listeners.length; ++i) {
 		final IOpenListener l = (IOpenListener)listeners[i];
 		Platform.run(new SafeRunnable() {
 			public void run() {
 				l.open(event);
 			}
 			public void handleException(Throwable e) {
 				super.handleException(e);
 				//If and unexpected exception happens, remove it
 				//to make sure the workbench keeps running.
 				removeOpenListener(l);
 			}
 		});			
 	}
 }
 /**
  * Notifies any post selection listeners that a post selection event has been received.
  * Only listeners registered at the time this method is called are notified.
  *
  * @param event a selection changed event
  * 
  * @see addPostSelectionChangedListener
  */
 protected void firePostSelectionChanged(final SelectionChangedEvent event) {
 	Object[] listeners = postSelectionChangedListeners.getListeners();
 	for (int i = 0; i < listeners.length; ++i) {
 		final ISelectionChangedListener l = (ISelectionChangedListener)listeners[i];
 		Platform.run(new SafeRunnable() {
 			public void run() {
 				l.selectionChanged(event);
 			}
 			public void handleException(Throwable e) {
 				super.handleException(e);
 				//If and unexpected exception happens, remove it
 				//to make sure the workbench keeps running.
 				removeSelectionChangedListener(l);
 			}
 		});		
 	}
 }
 
 /**
  * Returns the comparator to use for comparing elements,
  * or <code>null</code> if none has been set.
  * 
  * @param comparator the comparator to use for comparing elements
  *   or <code>null</code> 
  */
 public IElementComparer getComparer() {
 	return comparer;
 }
 
 /**
  * Returns the filtered array of children of the given element.
  * The resulting array must not be modified,
  * as it may come directly from the model's internal state.
  *
  * @param parent the parent element
  * @return a filtered array of child elements
  */
 protected Object[] getFilteredChildren(Object parent) {
 	Object[] result = getRawChildren(parent);
 	if (filters != null) {
 		for (Iterator iter = filters.iterator(); iter.hasNext();) {
 			ViewerFilter f = (ViewerFilter) iter.next();
 			result = f.filter(this, parent, result);
 		}
 	}
 	return result;
 }
 /**
  * Returns this viewer's filters.
  *
  * @return an array of viewer filters
  */
 public ViewerFilter[] getFilters() {
 	if (filters == null)
 		return new ViewerFilter[0];
 	ViewerFilter[] result = new ViewerFilter[filters.size()];
 	filters.toArray(result);
 	return result;
 }
 /**
  * Returns the item at the given display-relative coordinates, or
  * <code>null</code> if there is no item at that location.
  * <p>
  * The default implementation of this method returns <code>null</code>.
  * </p>
  *
  * @param x horizontal coordinate
  * @param y vertical coordinate
  * @return the item, or <code>null</code> if there is no item at the
  *  given coordinates
  */
 protected Item getItem(int x, int y) {
 	return null;
 }
 /**
  * Returns the children of the given parent without sorting and 
  * filtering them.  
  * The resulting array must not be modified,
  * as it may come directly from the model's internal state.
  * <p>
  * Returns an empty array if the given parent is <code>null</code>.
  * </p>
  *
  * @param parent the parent element
  * @return the child elements
  */
 protected Object[] getRawChildren(Object parent) {
 	Object[] result = null;
 	if (parent != null) {
 		IStructuredContentProvider cp = (IStructuredContentProvider) getContentProvider();
 		if (cp != null) {
 			result = cp.getElements(parent);
 			Assert.isNotNull(result);
 		}
 	}
 	return (result != null) ? result : new Object[0];
 }
 /**
  * Returns the root element.
  * <p>
  * The default implementation of this framework method forwards to
  * <code>getInput</code>.
  * Override if the root element is different from the viewer's input
  * element.
  * </p>
  *
  * @return the root element, or <code>null</code> if none
  */
 protected Object getRoot() {
 	return getInput();
 }
 /**
  * The <code>StructuredViewer</code> implementation of this method
  * returns the result as an <code>IStructuredSelection</code>.
  * <p>
  * Subclasses do not typically override this method, but implement
  * <code>getSelectionFromWidget(List)</code> instead.
  * <p>
  */
 public ISelection getSelection() {
 	Control control = getControl();
 	if (control == null || control.isDisposed()) {
 		return StructuredSelection.EMPTY;
 	}
 	List list = getSelectionFromWidget();
 	return new StructuredSelection(list);
 }
 /**
  * Retrieves the selection, as a <code>List</code>, from the underlying widget.
  *
  * @return the list of selected elements
  */
 protected abstract List getSelectionFromWidget();
 /**
  * Returns the sorted and filtered set of children of the given element.
  * The resulting array must not be modified,
  * as it may come directly from the model's internal state.
  *
  * @param parent the parent element
  * @return a sorted and filtered array of child elements
  */
 protected Object[] getSortedChildren(Object parent) {
 	Object[] result = getFilteredChildren(parent);
 	if (sorter != null) {
 		// be sure we're not modifying the original array from the model
 		result = (Object[]) result.clone();
 		sorter.sort(this, result);
 	}
 	return result;
 }
 /**
  * Returns this viewer's sorter, or <code>null</code> if it does not have one.
  *
  * @return a viewer sorter, or <code>null</code> if none
  */
 public ViewerSorter getSorter() {
 	return sorter;
 }
 /**
  * Handles a double-click select event from the widget.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param event the SWT selection event
  */
 protected void handleDoubleSelect(SelectionEvent event) {
 	// handle case where an earlier selection listener disposed the control.
 	Control control = getControl();
 	if (control != null && !control.isDisposed()) {
 		ISelection selection = getSelection();
 		updateSelection(selection);
 		fireDoubleClick(new DoubleClickEvent(this, selection));
 	}
 }
 /**
  * Handles an open event from the OpenStrategy.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param event the SWT selection event
  */
 protected void handleOpen(SelectionEvent event) {
 	Control control = getControl();
 	if (control != null && !control.isDisposed()) {
 		ISelection selection = getSelection();
 		fireOpen(new OpenEvent(this, selection));
 	}
 }
 /**
  * Handles an invalid selection.
  * <p>
  * This framework method is called if a model change picked up by a viewer
  * results in an invalid selection. For instance if an element contained
  * in the selection has been removed from the viewer, the viewer is free
  * to either remove the element from the selection or to pick another element
  * as its new selection.
  * The default implementation of this method calls <code>updateSelection</code>.
  * Subclasses may override it to implement a different strategy for picking
  * a new selection when the old selection becomes invalid.
  * </p>
  *
  * @param invalidSelection the selection before the viewer was updated
  * @param newSelection the selection after the update, or <code>null</code> if none
  */
 protected void handleInvalidSelection(ISelection invalidSelection, ISelection newSelection) {
 	updateSelection(newSelection);
 	SelectionChangedEvent event = new SelectionChangedEvent(this, newSelection);
 	firePostSelectionChanged(event);
 }
 /**
  * The <code>StructuredViewer</code> implementation of this <code>ContentViewer</code> method calls <code>update</code>
  * if the event specifies that the label of a given element has changed, otherwise it calls super.
  * Subclasses may reimplement or extend.
  * </p>
  */
 protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
 	Object[] elements = event.getElements();
 	if (elements != null) {
 		update(elements, null);
 	}
 	else {
 		super.handleLabelProviderChanged(event);
 	}
 }
 /**
  * Handles a select event from the widget.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param event the SWT selection event
  */
 protected void handleSelect(SelectionEvent event) {
 	// handle case where an earlier selection listener disposed the control.
 	Control control = getControl();
 	if (control != null && !control.isDisposed()) {
 		updateSelection(getSelection());
 	}
 }
 /**
  * Handles a post select event from the widget.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param event the SWT selection event
  */
 protected void handlePostSelect(SelectionEvent e) {
 	SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
 	firePostSelectionChanged(event);
 }
 /* (non-Javadoc)
  * Method declared on Viewer.
  */
 protected void hookControl(Control control) {
 	super.hookControl(control);
 	OpenStrategy handler = new OpenStrategy(control);
 	handler.addSelectionListener(new SelectionListener() {
 		public void widgetSelected(SelectionEvent e) {
 			handleSelect(e);
 		}
 		public void widgetDefaultSelected(SelectionEvent e) {
 			handleDoubleSelect(e);
 		}
 	});
 	handler.addPostSelectionListener(new SelectionAdapter() {
 		public void widgetSelected(SelectionEvent e) {
 			handlePostSelect(e);
 		}
 	});
 	handler.addOpenListener(new IOpenEventListener() {
 		public void handleOpen(SelectionEvent e) {
 			StructuredViewer.this.handleOpen(e);
 		}
 	});
 }	
 /**
  * Returns whether this viewer has any filters.
  */
 protected boolean hasFilters() {
 	return filters != null && filters.size() > 0;
 }
 /**
  * Refreshes this viewer starting at the given element.
  *
  * @param element the element
  */
 protected abstract void internalRefresh(Object element);
 
 /**
  * Refreshes this viewer starting at the given element.
  * Labels are updated as described in <code>refresh(boolean updateLabels)</code>.
  * <p>
  * The default implementation simply calls <code>internalRefresh(element)</code>,
  * ignoring <code>updateLabels</code>.
  * <p>
  * If this method is overridden to do the actual refresh, then
  * <code>internalRefresh(Object element)</code> should simply 
  * call <code>internalRefresh(element, true)</code>.
  * 
  * @param element the element
  * @param updateLabels <code>true</code> to update labels for existing elements,
  * <code>false</code> to only update labels as needed, assuming that labels
  * for existing elements are unchanged.
  * 
  * @since 2.0
  */
 protected void internalRefresh(Object element, boolean updateLabels) {
 	internalRefresh(element);
 }
 
 /**
  * Adds the element item pair to the element map
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param element the element
  * @param item the corresponding widget
  */
 protected void mapElement(Object element, Widget item) {
 	if (elementMap != null)
 		elementMap.put(element, item);
 }
 /**
  * Determines whether a change to the given property of the given element
  * would require refiltering and/or resorting.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param element the element
  * @param property the property
  * @return <code>true</code> if refiltering is required, and <code>false</code>
  *   otherwise
  */
 protected boolean needsRefilter(Object element, String property) {
 	if (sorter != null && sorter.isSorterProperty(element, property))
 		return true;
 
 	if (filters != null) {
 		for (int i = 0, n = filters.size(); i < n; ++i) {
 			ViewerFilter filter = (ViewerFilter) filters.get(i);
 			if (filter.isFilterProperty(element, property))
 				return true;
 		}
 	}
 	return false;
 }
 /**
  * Attempts to preserves the current selection across a run of
  * the given code.
  * <p>
  * The default implementation of this method:
  * <ul>
  *  <li>discovers the old selection (via <code>getSelection</code>)</li>
  *  <li>runs the given runnable</li>
  *  <li>attempts to restore the old selection
  *    (using <code>setSelectionToWidget</code></li>
  *  <li>rediscovers the resulting selection (via <code>getSelection</code>)</li>
  *  <li>calls <code>handleInvalidSelection</code> if the selection
  *    did not take</li>
  *  <li>calls <code>postUpdateHook</code></li>
  * </ul>
  * </p>
  *
  * @param updateCode the code to run
  */
 protected void preservingSelection(Runnable updateCode) {
 	
 	ISelection oldSelection= null;
 	try {
 		// preserve selection
 		oldSelection= getSelection();
 		inChange= restoreSelection= true;
 		
 		// perform the update
 		updateCode.run();
 				
 	} finally {
 		inChange= false;
 		
 		// restore selection
 		if (restoreSelection)
 			setSelectionToWidget(oldSelection, false);
 		
 		// send out notification if old and new differ
 		ISelection newSelection= getSelection();
 		if (! newSelection.equals(oldSelection))
 			handleInvalidSelection(oldSelection, newSelection);
 	}
 }
 /*
  * Non-Javadoc.
  * Method declared on Viewer.
  */
 public void refresh() {
 	refresh(getRoot());
 }
 
 /**
  * Refreshes this viewer with information freshly obtained from this
  * viewer's model.  If <code>updateLabels</code> is <code>true</code>
  * then labels for otherwise unaffected elements are updated as well.
  * Otherwise, it assumes labels for existing elements are unchanged,
  * and labels are only obtained as needed (for example, for new elements).
  * <p>
  * Calling <code>refresh(true)</code> has the same effect as <code>refresh()</code>.
  * <p>
  * Note that the implementation may still obtain labels for existing elements
  * even if <code>updateLabels</code> is false.  The intent is simply to allow
  * optimization where possible.
  * 
  * @param updateLabels <code>true</code> to update labels for existing elements,
  * <code>false</code> to only update labels as needed, assuming that labels
  * for existing elements are unchanged.
  * 
  * @since 2.0
  */
 public void refresh(boolean updateLabels) {
 	refresh(getRoot(), updateLabels);
 }
 
 /**
  * Refreshes this viewer starting with the given element.
  * <p>
  * Unlike the <code>update</code> methods, this handles structural changes
  * to the given element (e.g. addition or removal of children).
  * If only the given element needs updating, it is more efficient to use
  * the <code>update</code> methods.
  * </p>
  *
  * @param element the element
  */
 public void refresh(final Object element) {
 	preservingSelection(new Runnable() {
 		public void run() {
 			internalRefresh(element);
 		}
 	});
 }
 
 /**
  * Refreshes this viewer starting with the given element.
  * Labels are updated as described in <code>refresh(boolean updateLabels)</code>.
  * <p>
  * Unlike the <code>update</code> methods, this handles structural changes
  * to the given element (e.g. addition or removal of children).
  * If only the given element needs updating, it is more efficient to use
  * the <code>update</code> methods.
  * </p>
  * 
  * @param element the element
  * @param updateLabels <code>true</code> to update labels for existing elements,
  * <code>false</code> to only update labels as needed, assuming that labels
  * for existing elements are unchanged.
  * 
  * @since 2.0
  */
 public void refresh(final Object element, final boolean updateLabels) {
 	preservingSelection(new Runnable() {
 		public void run() {
 			internalRefresh(element, updateLabels);
 		}
 	});
 }
 
 /**
  * Refreshes the given TableItem with the given element.
  * Calls <code>doUpdateItem(..., false)</code>.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  */
 protected final void refreshItem(Widget widget, Object element) {
 	safeUpdateItem.widget = widget;
 	safeUpdateItem.object = element;
 	safeUpdateItem.fullMap = false;
 	Platform.run(safeUpdateItem);
 }
 /**
  * Removes the given open listener from this viewer.
  * Has no affect if an identical listener is not registered.
  *
  * @param listener a double-click listener
  */
 public void removeOpenListener(IOpenListener listener) {
 	openListeners.remove(listener);
 }
 /**
  * Removes the given post selection listener from this viewer.
  * Has no affect if an identical listener is not registered.
  *
  * @param listener a double-click listener
  */
 public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
 	postSelectionChangedListeners.remove(listener);
 }
 /**
  * Removes the given double-click listener from this viewer.
  * Has no affect if an identical listener is not registered.
  *
  * @param listener a double-click listener
  */
 public void removeDoubleClickListener(IDoubleClickListener listener) {
 	doubleClickListeners.remove(listener);
 }
 /**
  * Removes the given filter from this viewer, 
  * and triggers refiltering and resorting of the elements if required.
  * Has no effect if the identical filter is not registered.
  *
  * @param filter a viewer filter
  */
 public void removeFilter(ViewerFilter filter) {
 	Assert.isNotNull(filter);
 	if (filters != null) {
 		// Note: can't use List.remove(Object). Use identity comparison instead.
 		for (Iterator i = filters.iterator(); i.hasNext();) {
 			Object o = i.next();
 			if (o == filter) {
 				i.remove();
 				refresh();
 				if (filters.size() == 0)
 					filters = null;
 				return;
 			}
 		}
 	}
 }
 /**
  * Discards this viewer's filters and triggers refiltering and resorting
  * of the elements.
  */
 public void resetFilters() {
 	if (filters != null) {
 		filters = null;
 		refresh();
 	}
 }
 /**
  * Ensures that the given element is visible, scrolling the viewer if necessary.
  * The selection is unchanged.
  *
  * @param element the element to reveal
  */
 public abstract void reveal(Object element);
 /**
  * The <code>StructuredViewer</code> implementation of this method
  * checks to ensure that the content provider is an <code>IStructuredContentProvider</code>.
  */
 public void setContentProvider(IContentProvider provider) {
 	Assert.isTrue(provider instanceof IStructuredContentProvider);
 	super.setContentProvider(provider);
 }
 /* (non-Javadoc)
  * Method declared in Viewer.
  * This implementatation additionaly unmaps all the elements.
  */
 public final void setInput(Object input) {
 		
 	try {
 //		fInChange= true;
 		
 		unmapAllElements();
 
 		super.setInput(input);
 
 	} finally {
 //		fInChange= false;
 	}
 }
 /**
  * The <code>StructuredViewer</code> implementation of this method does the following.
  * <p>
  * If the new selection differs from the current
  * selection the hook <code>updateSelection</code> is called.
  * </p>
  * <p>
  * If <code>setSelection</code> is called from
  * within <code>preserveSelection</code>, the call to <code>updateSelection</code>
  * is delayed until the end of <code>preserveSelection</code>.
  * </p>
  * <p>
  * Subclasses do not typically override this method, but implement <code>setSelectionToWidget</code> instead.
  * </p>
  */
 public void setSelection(ISelection selection, boolean reveal) {
 	Control control = getControl();
 	if (control == null || control.isDisposed()) {
 		return;
 	}
 	if (!inChange) {
 		setSelectionToWidget(selection, reveal);
 		ISelection sel = getSelection();
 		updateSelection(sel);
 		firePostSelectionChanged(new SelectionChangedEvent(this, sel));
 	} else {
 		restoreSelection = false;
 		setSelectionToWidget(selection, reveal);
 	}
 }
 /**
  * Parlays the given list of selected elements into selections
  * on this viewer's control.
  * <p>
  * Subclasses should override to set their selection based on the
  * given list of elements.
  * </p>
  * 
  * @param l list of selected elements (element type: <code>Object</code>)
  *   or <code>null</code> if the selection is to be cleared
  * @param reveal <code>true</code> if the selection is to be made
  *   visible, and <code>false</code> otherwise
  */
 protected abstract void setSelectionToWidget(List l, boolean reveal);
 /**
  * Converts the selection to a <code>List</code> and calls <code>setSelectionToWidget(List, boolean)</code>.
  * The selection is expected to be an <code>IStructuredSelection</code> of elements.
  * If not, the selection is cleared.
  * <p>
  * Subclasses do not typically override this method, but implement <code>setSelectionToWidget(List, boolean)</code> instead.
  *
  * @param selection an IStructuredSelection of elements
  * @param reveal <code>true</code> to reveal the first element in the selection, or <code>false</code> otherwise
  */
 protected void setSelectionToWidget(ISelection selection, boolean reveal) {
 	if (selection instanceof IStructuredSelection)
 		setSelectionToWidget(((IStructuredSelection) selection).toList(), reveal);
 	else
 		setSelectionToWidget((List) null, reveal);
 }
 /**
  * Sets this viewer's sorter and triggers refiltering and resorting
  * of this viewer's element. Passing <code>null</code> turns sorting off.
  *
  * @param sorter a viewer sorter, or <code>null</code> if none
  */
 public void setSorter(ViewerSorter sorter) {
 	if (this.sorter != sorter) {
 		this.sorter = sorter;
 		refresh();
 	}
 }
 /**
  * Configures whether this structured viewer uses an internal hash table to
  * speeds up the mapping between elements and SWT items.
  * This must be called before the viewer is given an input (via <code>setInput</code>).
  *
  * @param enable <code>true</code> to enable hash lookup, and <code>false</code> to disable it
  */
 public void setUseHashlookup(boolean enable) {
 	Assert.isTrue(getInput() == null, "Can only enable the hash look up before input has been set");//$NON-NLS-1$
 	if (enable) {
 		elementMap= new CustomHashtable(comparer);
 	} else {
 		elementMap= null;
 	}	
 }
 
 /**
  * Sets the comparator to use for comparing elements,
  * or <code>null</code> to use the default <code>equals</code> and
  * <code>hashCode</code> methods on the elements themselves.
  * 
  * @param comparator the comparator to use for comparing elements
  *   or <code>null</code> 
  */
 public void setComparer(IElementComparer comparer) {
 	this.comparer = comparer;
 	if (elementMap != null) {
 		elementMap = new CustomHashtable(elementMap, comparer);
 	}
 }
 
 /**
  * Hook for testing.
  */
 public Widget testFindItem(Object element) {
 	return findItem(element);
 }
 /**
  * Removes all elements from the map
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  */
 protected void unmapAllElements() {
 	if (elementMap != null) {
 		elementMap = new CustomHashtable(comparer);
 	}
 }
 /**
  * Removes the given element from the internal element to widget map.
  * Does nothing if mapping is disabled.
  * If mapping is enabled, the given element must be present.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param element the element
  */
 protected void unmapElement(Object element) {
 	if (elementMap != null) {
 		elementMap.remove(element);
 	}
 }
 /**
  * Removes the given association from the internal element to widget map.
  * Does nothing if mapping is disabled, or if the given element does not map
  * to the given item.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * </p>
  *
  * @param element the element
  * @since 2.0
  */
 protected void unmapElement(Object element, Widget item) {
 	// double-check that the element actually maps to the given item before unmapping it
 	if (elementMap != null && elementMap.get(element) == item) {
 		// call unmapElement for backwards compatibility
 		unmapElement(element);
 	}
 }
 
 /**
  * Updates the given elements' presentation when one or more of their properties change.
  * Only the given elements are updated.  
  * <p>
  * This does not handle structural changes (e.g. addition or removal of elements),
  * and does not update any other related elements (e.g. child elements).
  * To handle structural changes, use the <code>refresh</code> methods instead.
  * </p>
  * <p>
  * This should be called when an element has changed in the model, in order to have the viewer
  * accurately reflect the model.  This method only affects the viewer, not the model.
  * </p>
  * <p>
  * Specifying which properties are affected may allow the viewer to optimize the update.
  * For example, if the label provider is not affected by changes to any of these properties,
  * an update may not actually be required. Specifing <code>properties</code> as 
  * <code>null</code> forces a full update of the given elements.
  * </p>
  * <p>
  * If the viewer has a sorter which is affected by a change to one of the properties,
  * the elements' positions are updated to maintain the sort order.
  * </p>
  * <p>
  * If the viewer has a filter which is affected by a change to one of the properties,
  * elements may appear or disappear if the change affects whether or not they are 
  * filtered out.
  * </p>
  *
  * @param elements the elements
  * @param properties the properties that have changed, or <code>null</code> to indicate unknown
  */
 public void update(Object[] elements, String[] properties) {
 	for (int i = 0; i < elements.length; ++i)
 		update(elements[i], properties);
 }
 /**
  * Updates the given element's presentation when one or more of its properties changes.
  * Only the given element is updated.
  * <p>
  * This does not handle structural changes (e.g. addition or removal of elements),
  * and does not update any other related elements (e.g. child elements).
  * To handle structural changes, use the <code>refresh</code> methods instead.
  * </p>
  * <p>
  * This should be called when an element has changed in the model, in order to have the viewer
  * accurately reflect the model.  This method only affects the viewer, not the model.
  * </p>
  * <p>
  * Specifying which properties are affected may allow the viewer to optimize the update.
  * For example, if the label provider is not affected by changes to any of these properties,
  * an update may not actually be required. Specifing <code>properties</code> as 
  * <code>null</code> forces a full update of the element.
  * </p>
  * <p>
  * If the viewer has a sorter which is affected by a change to one of the properties,
  * the element's position is updated to maintain the sort order.
  * </p>
  * <p>
  * If the viewer has a filter which is affected by a change to one of the properties,
  * the element may appear or disappear if the change affects whether or not the element
  * is filtered out.
  * </p>
  *
  * @param element the element
  * @param properties the properties that have changed, or <code>null</code> to indicate unknown
  */
 public void update(Object element, String[] properties) {
 	Assert.isNotNull(element);
 	Widget item = findItem(element);
 	if (item == null)
 		return;
 		
 	boolean needsRefilter = false;
 	if (properties != null) {
 		for (int i = 0; i < properties.length; ++i) {
 			needsRefilter = needsRefilter(element, properties[i]);
 			if (needsRefilter)
 				break;
 		}
 	}
 	if (needsRefilter) {
 		refresh();
 		return;
 	}
 
 	boolean needsUpdate;
 	if (properties == null) {
 		needsUpdate = true;
 	}
 	else {
 		needsUpdate = false;
 		IBaseLabelProvider labelProvider = getLabelProvider();
 		for (int i = 0; i < properties.length; ++i) {
 			needsUpdate = labelProvider.isLabelProperty(element, properties[i]);
 			if (needsUpdate)
 				break;
 		}
 	}
 	if (needsUpdate) {
 		updateItem(item, element);
 	}
 }
 /**
  * Copies attributes of the given element into the given widget.
  * <p>
  * This method is internal to the framework; subclassers should not call
  * this method.
  * Calls <code>doUpdateItem(widget, element, true)</code>.
  * </p>
  *
  * @param widget the widget
  * @param element the element
  */
 protected final void updateItem(Widget widget, Object element) {
 	safeUpdateItem.widget = widget;
 	safeUpdateItem.object = element;
 	safeUpdateItem.fullMap = true;
 	Platform.run(safeUpdateItem);
 }
 /**
  * Updates the selection of this viewer.
  * <p>
  * This framework method should be called when the selection 
  * in the viewer widget changes.
  * </p>
  * <p>
  * The default implementation of this method notifies all 
  * selection change listeners recorded in an internal state variable.
  * Overriding this method is generally not required; 
  * however, if overriding in a subclass, 
  * <code>super.updateSelection</code> must be invoked.
  * </p>
  * @param selection the selection, or <code>null</code> if none
  */
 protected void updateSelection(ISelection selection) {
 	SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
 	fireSelectionChanged(event);
 }
 /**
  * Returns whether this structured viewer is configured to use an internal
  * map to speed up the mapping between elements and SWT items.
  * <p>
  * The default implementation of this framework method checks whether the
  * internal map has been initialized.
  * </p>
  *
  * @return <code>true</code> if the element map is enabled, and <code>false</code> if disabled
  */
 protected boolean usingElementMap() {
 	return elementMap != null;
 }    
 
 
 }
