 /*
  * (c) Copyright 2010-2011 AgileBirds
  *
  * This file is part of OpenFlexo.
  *
  * OpenFlexo is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * OpenFlexo is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with OpenFlexo. If not, see <http://www.gnu.org/licenses/>.
  *
  */
 package org.openflexo.fib.view.widget;
 
 import java.awt.BorderLayout;
 import java.awt.Component;
 import java.awt.EventQueue;
 import java.awt.event.ActionEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.lang.reflect.InvocationTargetException;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Enumeration;
 import java.util.List;
 import java.util.Vector;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import javax.swing.JPanel;
 import javax.swing.JScrollPane;
 import javax.swing.JTable;
 import javax.swing.ListSelectionModel;
 import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
 import javax.swing.event.ListSelectionEvent;
 import javax.swing.event.ListSelectionListener;
 import javax.swing.event.TableModelEvent;
 import javax.swing.event.TableModelListener;
 import javax.swing.table.TableColumn;
 
 import org.jdesktop.swingx.JXTable;
 import org.openflexo.antar.expr.NotSettableContextException;
 import org.openflexo.antar.expr.NullReferenceException;
 import org.openflexo.antar.expr.TypeMismatchException;
 import org.openflexo.fib.controller.FIBController;
 import org.openflexo.fib.controller.FIBSelectable;
 import org.openflexo.fib.controller.FIBTableDynamicModel;
 import org.openflexo.fib.model.FIBTable;
 import org.openflexo.fib.model.FIBTableAction;
 import org.openflexo.fib.view.FIBWidgetView;
 import org.openflexo.fib.view.widget.table.FIBTableActionListener;
 import org.openflexo.fib.view.widget.table.FIBTableModel;
 import org.openflexo.fib.view.widget.table.FIBTableWidgetFooter;
 
 /**
  * Widget allowing to display/edit a list of values
  * 
  * @author sguerin
  */
 public class FIBTableWidget extends FIBWidgetView<FIBTable, JTable, Collection<?>> implements TableModelListener, FIBSelectable,
 		ListSelectionListener {
 
 	private static final Logger logger = Logger.getLogger(FIBTableWidget.class.getPackage().getName());
 
 	private JXTable _table;
 	private final JPanel _dynamicComponent;
 	private final FIBTable _fibTable;
 	private FIBTableModel _tableModel;
 	// private ListSelectionModel _listSelectionModel;
 	private JScrollPane scrollPane;
 
 	private FIBTableWidgetFooter footer;
 
 	private Vector<Object> selection;
 
 	private Object selectedObject;
 
 	public FIBTableWidget(FIBTable fibTable, FIBController controller) {
 		super(fibTable, controller);
 		_fibTable = fibTable;
 		_dynamicComponent = new JPanel();
 		_dynamicComponent.setOpaque(false);
 		_dynamicComponent.setLayout(new BorderLayout());
 
 		footer = new FIBTableWidgetFooter(this);
 		buildTable();
 	}
 
 	public FIBTable getTable() {
 		return _fibTable;
 	}
 
 	public FIBTableWidgetFooter getFooter() {
 		return footer;
 	}
 
 	public FIBTableModel getTableModel() {
 		if (_tableModel == null) {
 			_tableModel = new FIBTableModel(_fibTable, this, getController());
 		}
 		return _tableModel;
 	}
 
 	/*public JLabel getLabel()
 	{
 	    if (_label == null) {
 	        _label = new JLabel(_propertyModel.label + " : ", SwingConstants.CENTER);
 	        _label.setText(FlexoLocalization.localizedForKey(_propertyModel.label, _label));
 	        _label.setBackground(InspectorCst.BACK_COLOR);
 	        _label.setFont(DEFAULT_LABEL_FONT);
 	        if (_propertyModel.help != null && !_propertyModel.help.equals(""))
 	            _label.setToolTipText(_propertyModel.help);
 	    }
 	    return _label;
 	}*/
 
 	@Override
 	public synchronized boolean updateWidgetFromModel() {
 		List<?> valuesBeforeUpdating = getTableModel().getValues();
 		Object wasSelected = getSelectedObject();
 
 		boolean returned = false;
 
 		// logger.info("----------> updateWidgetFromModel() for " + getTable().getName());
 		if (_fibTable.getEnable().isSet() && _fibTable.getEnable().isValid()) {
 			Boolean enabledValue = true;
 			try {
 				enabledValue = _fibTable.getEnable().getBindingValue(getBindingEvaluationContext());
 			} catch (TypeMismatchException e) {
 				e.printStackTrace();
 			} catch (NullReferenceException e) {
 				e.printStackTrace();
 			} catch (InvocationTargetException e) {
 				e.printStackTrace();
 			}
 			_table.setEnabled(enabledValue != null && enabledValue);
 		}
 		if (notEquals(getValue(), getTableModel().getValues())) {
 
 			returned = true;
 
 			// boolean debug = false;
 			// if (getWidget().getName() != null && getWidget().getName().equals("PatternRoleTable")) debug=true;
 
 			// if (debug) System.out.println("valuesBeforeUpdating: "+valuesBeforeUpdating);
 			// if (debug) System.out.println("wasSelected: "+wasSelected);
 
 			if (_table.isEditing()) {
 				if (logger.isLoggable(Level.FINE)) {
 					logger.fine(getComponent().getName() + " - Table is currently editing at col:" + _table.getEditingColumn() + " row:"
 							+ _table.getEditingRow());
 				}
 				_table.getCellEditor().cancelCellEditing();
 			} else {
 				if (logger.isLoggable(Level.FINE)) {
 					logger.fine(getComponent().getName() + " - Table is NOT currently edited ");
 				}
 			}
 
 			if (logger.isLoggable(Level.FINE)) {
 				logger.fine(getComponent().getName() + " updateWidgetFromModel() with " + getValue() + " dataObject=" + getDataObject());
 			}
 
 			if (getValue() == null) {
 				getTableModel().setValues(Collections.emptyList());
 			}
 			if (getValue() instanceof List && !getValue().equals(valuesBeforeUpdating)) {
 				getTableModel().setValues(getValue());
 			}
 			footer.setModel(getDataObject());
 		}
 
 		// We restore value if and only if we represent same table
 		if (equals(getTableModel().getValues(), valuesBeforeUpdating) && wasSelected != null) {
 			returned = true;
 			setSelectedObject(wasSelected);
 		} else if (areSameValuesOrderIndifferent(getTableModel().getValues(), valuesBeforeUpdating)) {
 			// Same values, only order differs, in this case, still select right object
 			returned = true;
 			setSelectedObject(wasSelected);
 		} else {
 			try {
 				if (getComponent().getSelected().isValid()
 						&& getComponent().getSelected().getBindingValue(getBindingEvaluationContext()) != null) {
 					Object newSelectedObject = getComponent().getSelected().getBindingValue(getBindingEvaluationContext());
 					if (returned = notEquals(newSelectedObject, getSelectedObject())) {
 						setSelectedObject(newSelectedObject);
 					}
 				}
 
 				else if (getComponent().getAutoSelectFirstRow()) {
 					if (getTableModel().getValues() != null && getTableModel().getValues().size() > 0) {
 						returned = true;
						// Take care to this option as it may cause many issues
						// A better solution is to remove this option and let the selection manager manage such feature
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								getListSelectionModel().addSelectionInterval(0, 0);
							}
						});
 						// addToSelection(getTableModel().getValues().get(0));
 					}
 				}
 			} catch (TypeMismatchException e) {
 				e.printStackTrace();
 			} catch (NullReferenceException e) {
 				e.printStackTrace();
 			} catch (InvocationTargetException e) {
 				e.printStackTrace();
 			}
 		}
 
 		return returned;
 	}
 
 	public ListSelectionModel getListSelectionModel() {
 		return _table.getSelectionModel();
 	}
 
 	public void setSelectedObject(Object object/*, boolean notify*/) {
 		if (getValue() == null) {
 			return;
 		}
 		if (object == getSelectedObject()) {
 			logger.fine("FIBTableWidget: ignore setSelectedObject");
 			return;
 		}
 		logger.fine("FIBTable: setSelectedObject with object " + object + " current is " + getSelectedObject());
 		if (object != null) {
 			int index = getTableModel().getValues().indexOf(object);
 			if (index > -1) {
 				index = _table.convertRowIndexToView(index);
 				// if (!notify) _table.getSelectionModel().removeListSelectionListener(getTableModel());
 				getListSelectionModel().setSelectionInterval(index, index);
 				// if (!notify) _table.getSelectionModel().addListSelectionListener(getTableModel());
 			}
 		} else {
 			clearSelection();
 		}
 	}
 
 	public void clearSelection() {
 		getListSelectionModel().clearSelection();
 	}
 
 	@Override
 	public synchronized boolean updateModelFromWidget() {
 		return false;
 	}
 
 	/**
 	 * Implements
 	 * 
 	 * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
 	 * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
 	 */
 	@Override
 	public void tableChanged(TableModelEvent e) {
 		if (e instanceof FIBTableModel.ModelObjectHasChanged) {
 			FIBTableModel.ModelObjectHasChanged event = (FIBTableModel.ModelObjectHasChanged) e;
 			if (logger.isLoggable(Level.FINE)) {
 				logger.fine("Model has changed from " + event.getOldValues() + " to " + event.getNewValues());
 			}
 		} else if (e instanceof FIBTableModel.RowMoveForObjectEvent) {
 			if (logger.isLoggable(Level.FINE)) {
 				logger.fine("Reselect object, and then the edited cell");
 			}
 			FIBTableModel.RowMoveForObjectEvent event = (FIBTableModel.RowMoveForObjectEvent) e;
 			getListSelectionModel().removeListSelectionListener(this);
 			getListSelectionModel().addSelectionInterval(event.getNewRow(), event.getNewRow());
 			getListSelectionModel().addListSelectionListener(this);
 			_table.setEditingColumn(_table.convertColumnIndexToView(event.getColumn()));
 			_table.setEditingRow(_table.convertRowIndexToView(event.getNewRow()));
 		}
 	}
 
 	@Override
 	public JPanel getJComponent() {
 		return _dynamicComponent;
 	}
 
 	@Override
 	public JTable getDynamicJComponent() {
 		return _table;
 	}
 
 	@Override
 	public FIBTableDynamicModel createDynamicModel() {
 		return new FIBTableDynamicModel(null, getComponent());
 	}
 
 	@Override
 	public FIBTableDynamicModel getDynamicModel() {
 		return (FIBTableDynamicModel) super.getDynamicModel();
 	}
 
 	@Override
 	public void updateLanguage() {
 		super.updateLanguage();
 		updateTable();
 		for (FIBTableAction a : getWidget().getActions()) {
 			if (getWidget().getLocalize()) {
 				getLocalized(a.getName());
 			}
 		}
 	}
 
 	public void updateTable() {
 		// logger.info("!!!!!!!! updateTable()");
 
 		deleteTable();
 
 		if (_tableModel != null) {
 			_tableModel.removeTableModelListener(this);
 			_tableModel.delete();
 			_tableModel = null;
 		}
 
 		buildTable();
 
 		/*logger.info("!!!!!!!!  getDataObject()="+getDataObject());
 		logger.info("!!!!!!!!  getValue()="+getValue());
 		logger.info("!!!!!!!!  getDynamicModel().data="+getDynamicModel().data);
 		logger.info("!!!!!!!!  getComponent().getData()="+getComponent().getData());*/
 
 		updateDataObject(getDataObject());
 	}
 
 	@Override
 	public synchronized void delete() {
 		// TODO: re-implement this properly and check that all listeners are properly removed.
 		getFooter().delete();
 		deleteTable();
 		getTableModel().removeTableModelListener(this);
 		super.delete();
 	}
 
 	private void deleteTable() {
 		if (_table != null) {
 			_table.removeFocusListener(this);
 		}
 		if (getListSelectionModel() != null) {
 			getListSelectionModel().removeListSelectionListener(this);
 		}
 		if (scrollPane != null && _fibTable.getCreateNewRowOnClick()) {
 			for (MouseListener l : scrollPane.getMouseListeners()) {
 				scrollPane.removeMouseListener(l);
 			}
 		}
 		for (MouseListener l : _table.getMouseListeners()) {
 			_table.removeMouseListener(l);
 		}
 	}
 
 	private void buildTable() {
 		getTableModel().addTableModelListener(this);
 
 		_table = new JXTable(getTableModel()) {
 
 			@Override
 			protected void resetDefaultTableCellRendererColors(Component renderer, int row, int column) {
 			}
 
 		};
 		_table.setVisibleRowCount(0);
 		_table.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
 		_table.setAutoCreateRowSorter(true);
 		_table.setFillsViewportHeight(true);
 		_table.setShowHorizontalLines(false);
 		_table.setShowVerticalLines(false);
 		_table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
 		_table.addFocusListener(this);
 
 		for (int i = 0; i < getTableModel().getColumnCount(); i++) {
 			TableColumn col = _table.getColumnModel().getColumn(i);
 			// FlexoLocalization.localizedForKey(getController().getLocalizer(),getTableModel().columnAt(i).getTitle());
 			col.setWidth(getTableModel().getDefaultColumnSize(i));
 			col.setPreferredWidth(getTableModel().getDefaultColumnSize(i));
 			if (getTableModel().getColumnResizable(i)) {
 				col.setResizable(true);
 			} else {
 				// L'idee, c'est d'etre vraiment sur ;-) !
 				col.setWidth(getTableModel().getDefaultColumnSize(i));
 				col.setMinWidth(getTableModel().getDefaultColumnSize(i));
 				col.setMaxWidth(getTableModel().getDefaultColumnSize(i));
 				col.setResizable(false);
 			}
 			if (getTableModel().columnAt(i).requireCellRenderer()) {
 				col.setCellRenderer(getTableModel().columnAt(i).getCellRenderer());
 			}
 			if (getTableModel().columnAt(i).requireCellEditor()) {
 				col.setCellEditor(getTableModel().columnAt(i).getCellEditor());
 			}
 		}
 		if (_fibTable.getRowHeight() != null) {
 			_table.setRowHeight(_fibTable.getRowHeight());
 		}
 		if (getTable().getVisibleRowCount() != null) {
 			_table.setVisibleRowCount(getTable().getVisibleRowCount());
 			if (_table.getRowHeight() == 0) {
 				_table.setRowHeight(18);
 			}
 		}
 
 		_table.setSelectionMode(_fibTable.getSelectionMode().getMode());
 		// _table.getTableHeader().setReorderingAllowed(false);
 
 		_table.getSelectionModel().addListSelectionListener(this);
 
 		// _listSelectionModel = _table.getSelectionModel();
 		// _listSelectionModel.addListSelectionListener(this);
 
 		scrollPane = new JScrollPane(_table);
 		scrollPane.setOpaque(false);
 		if (_fibTable.getCreateNewRowOnClick()) {
 			_table.addMouseListener(new MouseAdapter() {
 				@Override
 				public void mouseClicked(MouseEvent e) {
 					if (_table.getCellEditor() != null) {
 						_table.getCellEditor().stopCellEditing();
 						e.consume();
 					}
 					if (_fibTable.getCreateNewRowOnClick()) {
 						if (!e.isConsumed() && e.getClickCount() == 2) {
 							// System.out.println("OK, on essaie de gerer un new par double click");
 							Enumeration<FIBTableActionListener> en = getFooter().getAddActionListeners();
 							while (en.hasMoreElements()) {
 								FIBTableActionListener action = en.nextElement();
 								if (action.isAddAction()) {
 									action.actionPerformed(new ActionEvent(_table, ActionEvent.ACTION_PERFORMED, null, EventQueue
 											.getMostRecentEventTime(), e.getModifiers()));
 									break;
 								}
 							}
 						}
 					}
 				}
 			});
 		}
 		/*_table.addMouseListener(new MouseAdapter() {
 			@Override
 			public void mouseClicked(MouseEvent e)
 			{
 				getController().fireMouseClicked(getDynamicModel(),e.getClickCount());
 			}
 		});*/
 
 		_dynamicComponent.removeAll();
 		_dynamicComponent.add(scrollPane, BorderLayout.CENTER);
 
 		if (_fibTable.getShowFooter()) {
 			_dynamicComponent.add(getFooter(), BorderLayout.SOUTH);
 		}
 
 		_dynamicComponent.revalidate();
 		_dynamicComponent.repaint();
 	}
 
 	@Override
 	public boolean synchronizedWithSelection() {
 		return getWidget().getBoundToSelectionManager();
 	}
 
 	public boolean isLastFocusedSelectable() {
 		return getController().getLastFocusedSelectable() == this;
 	}
 
 	@Override
 	public boolean mayRepresent(Object o) {
 		if (getValue() != null) {
 			return getValue().contains(o);
 		}
 		return false;
 	}
 
 	@Override
 	public void valueChanged(ListSelectionEvent e) {
 
 		// Ignore extra messages.
 		if (e.getValueIsAdjusting()) {
 			return;
 		}
 
 		if (logger.isLoggable(Level.FINE)) {
 			logger.fine("valueChanged() selected index=" + getListSelectionModel().getMinSelectionIndex());
 		}
 
 		int i = getListSelectionModel().getMinSelectionIndex();
 		int leadIndex = getListSelectionModel().getLeadSelectionIndex();
 		if (!getListSelectionModel().isSelectedIndex(leadIndex)) {
 			leadIndex = getListSelectionModel().getAnchorSelectionIndex();
 		}
 		while (!getListSelectionModel().isSelectedIndex(leadIndex) && i <= getListSelectionModel().getMaxSelectionIndex()) {
 			leadIndex = i;
 			i++;
 		}
 		if (leadIndex > -1) {
 			leadIndex = _table.convertRowIndexToModel(leadIndex);
 		}
 		selectedObject = getTableModel().elementAt(leadIndex);
 
 		Vector<Object> oldSelection = selection;
 		selection = new Vector<Object>();
 		for (i = getListSelectionModel().getMinSelectionIndex(); i <= getListSelectionModel().getMaxSelectionIndex(); i++) {
 			if (getListSelectionModel().isSelectedIndex(i)) {
 				selection.add(getTableModel().elementAt(_table.convertRowIndexToModel(i)));
 			}
 		}
 
 		getDynamicModel().setSelected(selectedObject);
 		getDynamicModel().setSelection(selection);
 		notifyDynamicModelChanged();
 		footer.handleSelectionChanged();
 		if (getComponent().getSelected().isValid()) {
 			logger.fine("Sets SELECTED binding with " + selectedObject);
 			try {
 				getComponent().getSelected().setBindingValue(selectedObject, getBindingEvaluationContext());
 			} catch (TypeMismatchException e1) {
 				e1.printStackTrace();
 			} catch (NullReferenceException e1) {
 				e1.printStackTrace();
 			} catch (InvocationTargetException e1) {
 				e1.printStackTrace();
 			} catch (NotSettableContextException e1) {
 				e1.printStackTrace();
 			}
 		}
 
 		updateFont();
 
 		if (!ignoreNotifications) {
 			getController().updateSelection(this, oldSelection, selection);
 		}
 
 		/*SwingUtilities.invokeLater(new Runnable() {
 			
 			public void run()
 			{
 				System.out.println((isFocused() ? "LEADER" : "SECONDARY")+" Le grand vainqueur est "+selectedObject);
 				System.out.println((isFocused() ? "LEADER" : "SECONDARY")+" La selection est "+selection);
 			}
 		});*/
 
 	}
 
 	private boolean ignoreNotifications = false;
 
 	@Override
 	public Object getSelectedObject() {
 		return selectedObject;
 	}
 
 	@Override
 	public Vector<Object> getSelection() {
 		return selection;
 	}
 
 	@Override
 	public void objectAddedToSelection(Object o) {
 		int index = getTableModel().getValues().indexOf(o);
 		if (index > -1) {
 			ignoreNotifications = true;
 			try {
 				index = _table.convertRowIndexToView(index);
 				getListSelectionModel().addSelectionInterval(index, index);
 			} catch (IndexOutOfBoundsException e) {
 				logger.warning("Unexpected " + e);
 			}
 			ignoreNotifications = false;
 		}
 	}
 
 	@Override
 	public void objectRemovedFromSelection(Object o) {
 		int index = getTableModel().getValues().indexOf(o);
 		if (index > -1) {
 			ignoreNotifications = true;
 			try {
 				index = _table.convertRowIndexToView(index);
 			} catch (IndexOutOfBoundsException e) {
 				logger.warning("Unexpected " + e);
 			}
 			getListSelectionModel().removeSelectionInterval(index, index);
 			ignoreNotifications = false;
 		}
 	}
 
 	@Override
 	public void selectionResetted() {
 		ignoreNotifications = true;
 		getListSelectionModel().clearSelection();
 		ignoreNotifications = false;
 	}
 
 	@Override
 	public void addToSelection(Object o) {
 		int index = getTableModel().getValues().indexOf(o);
 		if (index > -1) {
 			index = _table.convertRowIndexToView(index);
 			getListSelectionModel().addSelectionInterval(index, index);
 		}
 	}
 
 	@Override
 	public void removeFromSelection(Object o) {
 		int index = getTableModel().getValues().indexOf(o);
 		if (index > -1) {
 			index = _table.convertRowIndexToView(index);
 			getListSelectionModel().removeSelectionInterval(index, index);
 		}
 	}
 
 	@Override
 	public void resetSelection() {
 		getListSelectionModel().clearSelection();
 	}
 
 	private static boolean areSameValuesOrderIndifferent(List<?> l1, List<?> l2) {
 		if (l1 == null || l2 == null) {
 			return false;
 		}
 		if (l1.size() != l2.size()) {
 			return false;
 		}
 		Comparator<Object> comparator = new Comparator<Object>() {
 			@Override
 			public int compare(Object o1, Object o2) {
 				return o1.hashCode() - o2.hashCode();
 			}
 		};
 		List<Object> sortedL1 = new ArrayList<Object>(l1);
 		Collections.sort(sortedL1, comparator);
 		List<Object> sortedL2 = new ArrayList<Object>(l2);
 		Collections.sort(sortedL2, comparator);
 		for (int i = 0; i < sortedL1.size(); i++) {
 			if (!sortedL1.get(i).equals(sortedL2.get(i))) {
 				return false;
 			}
 		}
 		return true;
 	}
 }
