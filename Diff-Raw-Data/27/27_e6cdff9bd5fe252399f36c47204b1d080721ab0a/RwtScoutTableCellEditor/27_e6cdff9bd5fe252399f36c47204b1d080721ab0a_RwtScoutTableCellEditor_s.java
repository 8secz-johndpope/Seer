 /*******************************************************************************
  * Copyright (c) 2011 BSI Business Systems Integration AG.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     BSI Business Systems Integration AG - initial API and implementation
  *******************************************************************************/
 package org.eclipse.scout.rt.ui.rap.basic.table.celleditor;
 
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.atomic.AtomicBoolean;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicReference;
 import java.util.concurrent.locks.Lock;
 import java.util.concurrent.locks.ReentrantLock;
 
 import org.eclipse.jface.viewers.CellEditor;
 import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
 import org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent;
 import org.eclipse.jface.viewers.ICellModifier;
 import org.eclipse.jface.viewers.TableViewer;
 import org.eclipse.jface.viewers.ViewerCell;
 import org.eclipse.scout.commons.CompareUtility;
 import org.eclipse.scout.rt.client.ui.basic.table.ITable;
 import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
 import org.eclipse.scout.rt.client.ui.basic.table.TableUtility;
 import org.eclipse.scout.rt.client.ui.basic.table.columns.IBooleanColumn;
 import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
 import org.eclipse.scout.rt.client.ui.form.fields.GridData;
 import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
 import org.eclipse.scout.rt.client.ui.form.fields.stringfield.IStringField;
 import org.eclipse.scout.rt.ui.rap.basic.table.IRwtScoutTable;
 import org.eclipse.scout.rt.ui.rap.basic.table.IRwtScoutTableForPatch;
 import org.eclipse.scout.rt.ui.rap.core.basic.IRwtScoutComposite;
 import org.eclipse.scout.rt.ui.rap.core.form.IRwtScoutForm;
 import org.eclipse.scout.rt.ui.rap.extension.UiDecorationExtensionPoint;
 import org.eclipse.scout.rt.ui.rap.form.fields.IPopupSupport;
 import org.eclipse.scout.rt.ui.rap.form.fields.IPopupSupport.IPopupSupportListener;
 import org.eclipse.scout.rt.ui.rap.keystroke.RwtKeyStroke;
 import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.events.TraverseEvent;
 import org.eclipse.swt.events.TraverseListener;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.layout.FillLayout;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Control;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Listener;
 import org.eclipse.swt.widgets.Table;
 import org.eclipse.swt.widgets.TableColumn;
 import org.eclipse.swt.widgets.Widget;
 
 /**
  * <h3>RwtScoutTableCellEditor</h3> ...
  * 
  * @author imo
  * @since 3.7.0 June 2011
  */
 public class RwtScoutTableCellEditor {
   private static final String DUMMY_VALUE = "Dummy";
 
   private final IRwtScoutTableForPatch m_uiTableComposite;
   private final Listener m_rowHeightListener;
 
   private P_FocusLostListener m_focusLostListener;
 
   public RwtScoutTableCellEditor(IRwtScoutTableForPatch uiTableComposite) {
     m_focusLostListener = new P_FocusLostListener();
     m_uiTableComposite = uiTableComposite;
     m_rowHeightListener = new Listener() {
       private static final long serialVersionUID = 1L;
 
       @Override
       public void handleEvent(Event event) {
         event.height = Math.max(event.height, UiDecorationExtensionPoint.getLookAndFeel().getLogicalGridLayoutRowHeight());
       }
     };
   }
 
   //(re)install cell editors
   public void initializeUi() {
     TableViewer viewer = m_uiTableComposite.getUiTableViewer();
     String[] columnPropertyNames = new String[viewer.getTable().getColumnCount()];
     CellEditor[] oldEditors = viewer.getCellEditors();
     CellEditor[] newEditors = new CellEditor[columnPropertyNames.length];
     boolean hasEditors = false;
     for (int i = 0; i < columnPropertyNames.length; i++) {
       TableColumn rwtCol = viewer.getTable().getColumn(i);
       IColumn<?> scoutCol = (IColumn<?>) rwtCol.getData(IRwtScoutTableForPatch.KEY_SCOUT_COLUMN);
       if (scoutCol != null) {
         columnPropertyNames[i] = "" + scoutCol.getColumnIndex();
         if (scoutCol.isEditable()) {
           hasEditors = true;
           newEditors[i] = new P_RwtCellEditor(viewer.getTable());
         }
       }
       else {
         columnPropertyNames[i] = "";
       }
     }
     viewer.setCellModifier(new P_RwtCellModifier());
     viewer.setColumnProperties(columnPropertyNames);
     viewer.setCellEditors(newEditors);
     if (oldEditors != null && oldEditors.length > 0) {
       for (CellEditor editor : oldEditors) {
         if (editor != null) {
           editor.dispose();
         }
       }
     }
     //increase row height when editors are present
     if (hasEditors) {
       //TODO rap viewer.getTable().addListener(SWT.MeasureItem, m_rowHeightListener);
     }
     else {
       //TODO rap viewer.getTable().removeListener(SWT.MeasureItem, m_rowHeightListener);
     }
   }
 
   protected Control getEditorControl(Composite parent, ITableRow scoutRow, IColumn<?> scoutCol) {
     //no caching
     Control swtEditorControl = null;
     IRwtScoutComposite<? extends IFormField> editorComposite = createEditorComposite(parent, scoutRow, scoutCol);
     if (editorComposite != null) {
       decorateEditorComposite(editorComposite, scoutRow, scoutCol);
       swtEditorControl = editorComposite.getUiContainer();
     }
     return swtEditorControl;
   }
 
   @SuppressWarnings("unchecked")
   protected IRwtScoutComposite<? extends IFormField> createEditorComposite(Composite parent, final ITableRow scoutRow, final IColumn<?> scoutCol) {
     final AtomicReference<IFormField> fieldRef = new AtomicReference<IFormField>();
     if (scoutRow != null && scoutCol != null) {
       Runnable t = new Runnable() {
         @Override
         public void run() {
           fieldRef.set(m_uiTableComposite.getScoutObject().getUIFacade().prepareCellEditFromUI(scoutRow, scoutCol));
           synchronized (fieldRef) {
             fieldRef.notifyAll();
           }
         }
       };
       synchronized (fieldRef) {
         m_uiTableComposite.getUiEnvironment().invokeScoutLater(t, 2345);
         try {
           fieldRef.wait(2345);
         }
         catch (InterruptedException e) {
           //nop
         }
       }
     }
     IFormField formField = fieldRef.get();
     if (formField == null) {
       return null;
     }
 
     IRwtScoutComposite uiScoutFormField;
     if (formField instanceof IStringField && ((IStringField) formField).isMultilineText()) {
       // for fields to be presented as popup dialog
       uiScoutFormField = createEditorCompositesPopup(parent, formField, scoutRow, scoutCol);
     }
     else {
       uiScoutFormField = m_uiTableComposite.getUiEnvironment().createFormField(parent, formField);
     }
 
     // If the SWT field uses a @{Shell} to edit its value, the focus on the table gets lost while the shell is open.
     // To prevent the cell editor from being closed, the focus lost listener must be uninstalled for the time the shell is open.
     if (uiScoutFormField instanceof IPopupSupport) {
       ((IPopupSupport) uiScoutFormField).addPopupEventListener(new IPopupSupportListener() {
 
         @Override
         public void handleEvent(int eventType) {
           if (eventType == IPopupSupportListener.TYPE_OPENING) {
             getFocusLostListener().suspend();
           }
           else if (eventType == IPopupSupportListener.TYPE_CLOSED) {
             getFocusLostListener().resume();
           }
         }
       });
     }
     return uiScoutFormField;
   }
 
   protected IRwtScoutComposite<? extends IFormField> createEditorCompositesPopup(Composite parent, IFormField formField, final ITableRow scoutRow, final IColumn<?> scoutCol) {
     // uninstall focus lost listener as new shell is used for popup
     getFocusLostListener().suspend();
 
     // overwrite layout properties
     GridData gd = formField.getGridData();
     gd.h = 1;
     gd.w = IFormField.FULL_WIDTH;
     gd.weightY = 1;
     gd.weightX = 1;
     formField.setGridDataInternal(gd);
 
     TableColumn swtCol = getRwtColumn(scoutCol);
     final P_RwtCellEditor cellEditor = (P_RwtCellEditor) m_uiTableComposite.getUiTableViewer().getCellEditors()[getRwtColumnIndex(swtCol)];
 
     int prefWidth = gd.widthInPixel;
     int minWidth = swtCol.getWidth();
     int prefHeight = gd.heightInPixel;
     int minHeight = Math.max(105, m_uiTableComposite.getUiTableViewer().getTable().getItemHeight());
 
     prefHeight = Math.max(prefHeight, minHeight);
     prefWidth = Math.max(prefWidth, minWidth);
 
     // create placeholder field to represent the cell editor
     Composite cellEditorComposite = new Composite(parent, SWT.NONE);
 
     // create popup dialog to wrap the form field
     final RwtScoutFormFieldPopup formFieldDialog = new RwtScoutFormFieldPopup(cellEditorComposite);
     formFieldDialog.setPrefHeight(prefHeight);
     formFieldDialog.setPrefWidth(prefWidth);
     formFieldDialog.setMinHeight(minHeight);
     formFieldDialog.setMinWidth(minWidth);
     formFieldDialog.createUiField(parent, formField, m_uiTableComposite.getUiEnvironment());
 
     // register custom cell modifier to touch the field in order to write its value back to the model
     final ICellModifier defaultCellModifier = m_uiTableComposite.getUiTableViewer().getCellModifier();
     m_uiTableComposite.getUiTableViewer().setCellModifier(new P_RwtCellModifier() {
 
       @Override
       public void modify(Object element, String property, Object value) {
         formFieldDialog.touch();
         super.modify(element, property, value);
       }
     });
     // register custom focus delegate to request the field's focus
     final IFocusDelegate defaultFocusDelegate = cellEditor.getFocusDelegate();
     cellEditor.setFocusDelegate(new IFocusDelegate() {
 
       @Override
       public void doSetFocus() {
         IRwtScoutForm rwtScoutForm = formFieldDialog.getInnerRwtScoutForm();
         if (rwtScoutForm != null) {
           requestFocus(rwtScoutForm.getUiContainer());
         }
       }
     });
 
     // listener to receive events about the popup's state
     final IFormFieldPopupEventListener popupListener = new IFormFieldPopupEventListener() {
 
       @Override
       public void handleEvent(FormFieldPopupEvent event) {
         if ((event.getType() & FormFieldPopupEvent.TYPE_OK) > 0) {
           // save cell editor
           cellEditor.stopCellEditing();
         }
         else if ((event.getType() & FormFieldPopupEvent.TYPE_CANCEL) > 0) {
           // cancel cell editor
           cellEditor.cancelCellEditing();
         }
 
         // traversal control
         if ((event.getType() & FormFieldPopupEvent.TYPE_FOCUS_BACK) > 0) {
           enqueueEditNextTableCell(scoutRow, scoutCol, false);
         }
         else if ((event.getType() & FormFieldPopupEvent.TYPE_FOCUS_NEXT) > 0) {
           enqueueEditNextTableCell(scoutRow, scoutCol, true);
         }
       }
     };
     formFieldDialog.addEventListener(popupListener);
 
     // register listener to intercept the cell editor's events in order to properly close the popup.
     // This is crucial if the editor is deactivated programmatically or if another cell is activated.
     // In contrast to SWT, in RWT the next cell is activated prior to receiving the shell closed event.
     cellEditor.addDeactivateListener(new IDeactivateListener() {
 
       @Override
       public void canceled(ColumnViewerEditorDeactivationEvent event) {
         restoreDefault();
         closePopup(FormFieldPopupEvent.TYPE_OK);
       }
 
       @Override
       public void saved(ColumnViewerEditorDeactivationEvent event) {
         restoreDefault();
         closePopup(FormFieldPopupEvent.TYPE_CANCEL);
       }
 
       private void restoreDefault() {
         // restore default focus delegate
         cellEditor.setFocusDelegate(defaultFocusDelegate);
         // restore default cell modifier
         m_uiTableComposite.getUiTableViewer().setCellModifier(defaultCellModifier);
         // remove this listener on the cell editor
         cellEditor.removeDeactivateListener(this);
       }
 
       private void closePopup(int popupEvent) {
         if (formFieldDialog.isClosed()) {
           return;
         }
         // remove popup listener to not receive events on the dialog's state because the cell editor is already closing
         formFieldDialog.removeEventListener(popupListener);
         // close the popup
         formFieldDialog.closePopup(popupEvent);
       }
     });
 
     return formFieldDialog;
   }
 
   private TableColumn getRwtColumn(IColumn<?> scoutCol) {
     for (TableColumn swtCol : m_uiTableComposite.getUiTableViewer().getTable().getColumns()) {
       IColumn<?> candidate = (IColumn<?>) swtCol.getData(IRwtScoutTable.KEY_SCOUT_COLUMN);
       if (candidate != null && CompareUtility.equals(candidate.getColumnId(), scoutCol.getColumnId())) {
         return swtCol;
       }
     }
     return null;
   }
 
   private int getRwtColumnIndex(TableColumn swtCol) {
     Table table = m_uiTableComposite.getUiTableViewer().getTable();
     for (int i = 0; i < table.getColumnCount(); i++) {
       if (table.getColumn(i) == swtCol) {
         return i;
       }
     }
     return -1;
   }
 
   protected void decorateEditorComposite(IRwtScoutComposite<? extends IFormField> editorComposite, final ITableRow scoutRow, final IColumn<?> scoutCol) {
   }
 
   protected void saveEditorFromUi() {
     Runnable t = new Runnable() {
       @Override
       public void run() {
         m_uiTableComposite.getScoutObject().getUIFacade().completeCellEditFromUI();
       }
     };
     m_uiTableComposite.getUiEnvironment().invokeScoutLater(t, 0);
   }
 
   protected void cancelEditorFromUi() {
     Runnable t = new Runnable() {
       @Override
       public void run() {
         m_uiTableComposite.getScoutObject().getUIFacade().cancelCellEditFromUI();
       }
     };
     m_uiTableComposite.getUiEnvironment().invokeScoutLater(t, 0);
   }
 
   protected void enqueueEditNextTableCell(final ITableRow row, final IColumn<?> col, final boolean forward) {
     if (row == null || col == null) {
       return;
     }
     m_uiTableComposite.getUiEnvironment().invokeScoutLater(new Runnable() {
       @Override
       public void run() {
         if (m_uiTableComposite.getUiEnvironment() == null) {
           return;
         }
         ITable table = m_uiTableComposite.getScoutObject();
         TableUtility.editNextTableCell(table, row, col, forward, new TableUtility.ITableCellEditorFilter() {
           @Override
           public boolean accept(ITableRow rowx, IColumn<?> colx) {
             return !(colx instanceof IBooleanColumn);
           }
         });
       }
     }, 0L);
   }
 
   protected IColumn<?> getScoutColumn(String property) {
     if (property != null && property.matches("[0-9]+")) {
       int colIndex = Integer.parseInt(property);
       return m_uiTableComposite.getScoutObject().getColumnSet().getColumn(colIndex);
     }
     return null;
   }
 
   public P_FocusLostListener getFocusLostListener() {
     return m_focusLostListener;
   }
 
   private boolean requestFocus(Control control) {
     if (control == null || control.isDisposed()) {
       return false;
     }
     if (control.setFocus()) {
       return true;
     }
 
     if (control instanceof Composite) {
       for (Control child : ((Composite) control).getChildren()) {
         if (requestFocus(child)) {
           return true;
         }
       }
     }
     return false;
   }
 
   private class P_RwtCellModifier implements ICellModifier {
 
     @Override
     public void modify(Object element, String property, Object value) {
       saveEditorFromUi();
     }
 
     @Override
     public Object getValue(Object element, String property) {
       return DUMMY_VALUE;
     }
 
     @Override
     public boolean canModify(Object element, String property) {
       final ITable table = m_uiTableComposite.getScoutObject();
       final ITableRow row = (ITableRow) element;
       final IColumn<?> column = getScoutColumn(property);
       //make a safe model call
       final AtomicBoolean b = new AtomicBoolean();
       synchronized (b) {
         Runnable r = new Runnable() {
           @Override
           public void run() {
             // try first
             synchronized (b) {
               try {
                 if (table != null && row != null && column != null) {
                   if (column instanceof IBooleanColumn) {
                     b.set(false);
                   }
                   else {
                     b.set(table.isCellEditable(row, column));
                   }
                 }
               }
               catch (Throwable ex) {
                 //fast access: ignore
               }
               b.notifyAll();
             }
           }
         };
         m_uiTableComposite.getUiEnvironment().invokeScoutLater(r, 2345);
         try {
           b.wait(2345);
         }
         catch (InterruptedException e) {
           //nop
         }
       }
       return b.get();
     }
   }
 
   private class P_RwtCellEditor extends CellEditor {
     private static final long serialVersionUID = 1L;
 
     private Composite m_container;
     private Object m_value;
     private ITableRow m_editScoutRow;
     private IColumn<?> m_editScoutCol;
     private IFocusDelegate m_focusDelegate;
     private ConcurrentHashMap<IDeactivateListener, Object> m_deactivateListeners;
 
     protected P_RwtCellEditor(Table parent) {
       super(parent);
       m_focusDelegate = new P_FocusDelegate();
       m_deactivateListeners = new ConcurrentHashMap<IDeactivateListener, Object>();
     }
 
     @Override
     protected Control createControl(Composite/*Table*/parent) {
       m_container = new Composite(parent, SWT.NONE) {
         private static final long serialVersionUID = 1L;
 
         /*
          * disable inner components preferred sizes
          */
         @Override
         public Point computeSize(int wHint, int hHint, boolean changed) {
           return new Point(wHint, hHint);
         }
       };
       m_container.setLayout(new FillLayout());
       m_uiTableComposite.getUiEnvironment().addKeyStroke(m_container, new RwtKeyStroke(SWT.ESC) {
         @Override
         public void handleUiAction(Event e) {
           e.doit = false;
           fireCancelEditor();
         }
       }, false);
       m_uiTableComposite.getUiEnvironment().addKeyStroke(m_container, new RwtKeyStroke(SWT.CR) {
         @Override
         public void handleUiAction(Event e) {
           e.doit = false;
           fireApplyEditorValue();
           deactivate();
         }
       }, false);
       return m_container;
     }
 
     @Override
     protected void doSetFocus() {
       if (m_focusDelegate != null) {
         m_focusDelegate.doSetFocus();
       }
     }
 
     @Override
     protected Object doGetValue() {
       return m_value;
     }
 
     @Override
     protected void doSetValue(Object value) {
       m_value = value;
     }
 
     @Override
     public void activate(ColumnViewerEditorActivationEvent e) {
       getFocusLostListener().install();
       getFocusLostListener().suspend(); // is only resumed if editor gets the focus in @{link P_RwtCellEditor#doSetFocus()}. Otherwise, a focus lost event might be consumed and the editor closed
 
       m_editScoutRow = null;
       m_editScoutCol = null;
       if (e.getSource() instanceof ViewerCell) {
         ViewerCell cell = (ViewerCell) e.getSource();
         TableViewer viewer = m_uiTableComposite.getUiTableViewer();
         TableColumn rwtCol = viewer.getTable().getColumn(cell.getColumnIndex());
         IColumn<?> scoutCol = (IColumn<?>) rwtCol.getData(IRwtScoutTableForPatch.KEY_SCOUT_COLUMN);
         ITableRow scoutRow = (ITableRow) cell.getElement();
         //no edit on boolean column when mouse was clicked
         if (e.sourceEvent instanceof MouseEvent) {
           if (scoutCol instanceof IBooleanColumn) {
             return;
           }
         }
         if (scoutRow != null && scoutCol != null) {
           m_editScoutRow = scoutRow;
           m_editScoutCol = scoutCol;
           @SuppressWarnings("unused")
           Control control = getEditorControl(m_container, scoutRow, scoutCol);
         }
         m_container.layout(true, true);
         m_container.setVisible(true);
       }
     }
 
     @Override
     protected void deactivate(ColumnViewerEditorDeactivationEvent e) {
       getFocusLostListener().uninstall();
 
       // notify cell editor close listeners
       for (IDeactivateListener listener : m_deactivateListeners.keySet()) {
         if (e.eventType == ColumnViewerEditorDeactivationEvent.EDITOR_SAVED) {
           listener.saved(e);
         }
         else {
           listener.canceled(e);
         }
       }
 
       m_editScoutRow = null;
       m_editScoutCol = null;
       for (Control c : m_container.getChildren()) {
         c.dispose();
       }
       super.deactivate(e);
       if (e.eventType == ColumnViewerEditorDeactivationEvent.EDITOR_CANCELED) {
         cancelEditorFromUi();
       }
     }
 
     public void stopCellEditing() {
       fireApplyEditorValue();
       deactivate();
     }
 
     public void cancelCellEditing() {
       fireCancelEditor();
       deactivate();
     }
 
     public IFocusDelegate getFocusDelegate() {
       return m_focusDelegate;
     }
 
     public void setFocusDelegate(IFocusDelegate focusDelegate) {
       m_focusDelegate = focusDelegate;
     }
 
     public void addDeactivateListener(IDeactivateListener listener) {
       m_deactivateListeners.put(listener, new Object());
     }
 
     public void removeDeactivateListener(IDeactivateListener listener) {
       m_deactivateListeners.remove(listener);
     }
 
     private class P_FocusDelegate implements IFocusDelegate {
 
       @Override
       public void doSetFocus() {
         requestFocus(m_container);
         Control focusControl = m_container.getDisplay().getFocusControl();
         if (focusControl != null && RwtUtility.isAncestorOf(m_container, focusControl)) {
           focusControl.addTraverseListener(new TraverseListener() {
             private static final long serialVersionUID = 1L;
 
             @Override
             public void keyTraversed(TraverseEvent e) {
               switch (e.detail) {
                 case SWT.TRAVERSE_ESCAPE:
                     case SWT.TRAVERSE_RETURN: {
                     e.doit = false;
                     break;
                   }
                   case SWT.TRAVERSE_TAB_NEXT: {
                     e.doit = false;
                     ITableRow scoutRow = m_editScoutRow;
                     IColumn<?> scoutCol = m_editScoutCol;
                     fireApplyEditorValue();
                     deactivate();
                     enqueueEditNextTableCell(scoutRow, scoutCol, true);
                     break;
                   }
                   case SWT.TRAVERSE_TAB_PREVIOUS: {
                     e.doit = false;
                     ITableRow scoutRow = m_editScoutRow;
                     IColumn<?> scoutCol = m_editScoutCol;
                     fireApplyEditorValue();
                     deactivate();
                     enqueueEditNextTableCell(scoutRow, scoutCol, false);
                     break;
                   }
                 }
               }
           });
         }
         getFocusLostListener().resume(); // because listener was suspended after activation
       }
     }
   }
 
   /**
    * Hysteresis listener that commits the cell editor when the table has first received focus and then lost it. That is
    * because cell editors in SWT are not closed automatically if the table looses the focus.
    */
   private class P_FocusLostListener implements Listener {
     private static final long serialVersionUID = 1L;
 
     private final Lock m_suspendLock = new ReentrantLock();
     private AtomicInteger m_suspendCounter = new AtomicInteger();
 
     /**
      * Uninstalls this listener on the table widget
      */
     public void uninstall() {
       m_uiTableComposite.getUiEnvironment().getDisplay().removeFilter(SWT.FocusIn, this);
       m_suspendCounter.set(0);
     }
 
     /**
      * Installs this listener on the table widget
      */
     public void install() {
       m_uiTableComposite.getUiEnvironment().getDisplay().addFilter(SWT.FocusIn, this);
       m_suspendCounter.set(0);
     }
 
     /**
      * <p>
      * To resume listening for focus lost events.
      * </p>
      * <p>
      * Please note that this request is put onto a stack meaning that you have to call
      * {@link P_FocusLostListener#resume()} as many times as you called {@link P_FocusLostListener#suspend()} to resume
      * listening for focus lost events.
      * </p>
      * <p>
      * <small>Counterpart of {@link P_FocusLostListener#suspend()}.</small>
      * </p>
      */
     public void resume() {
       m_suspendLock.lock();
       try {
         if (m_suspendCounter.decrementAndGet() < 0) { // negative values are not allowed
           m_suspendCounter.set(0);
         }
       }
       finally {
         m_suspendLock.unlock();
       }
     }
 
     /**
      * <p>
      * To suspend listening for focus lost events.
      * </p>
      * <p>
      * Please note that this request is put onto a stack meaning that you have to call
      * {@link P_FocusLostListener#resume()} as many times as you called {@link P_FocusLostListener#suspend()} to resume
      * listening for focus lost events.
      * </p>
      * <p>
      * <small>Counterpart of {@link P_FocusLostListener#resume()}.</small>
      * </p>
      */
     public void suspend() {
       m_suspendLock.lock();
       try {
         m_suspendCounter.incrementAndGet();
       }
       finally {
         m_suspendLock.unlock();
       }
     }
 
     public boolean isSuspended() {
       return m_suspendCounter.get() > 0;
     }
 
     @Override
     public void handleEvent(Event event) {
       if (isSuspended()) {
         return;
       }
 
       Widget w = event.widget;
       if (w == null || !(w instanceof Control) || w.isDisposed()) {
         return;
       }
       TableViewer viewer = m_uiTableComposite.getUiTableViewer();
       if (!viewer.isCellEditorActive()) {
         return;
       }
 
       Control candidate = (Control) w;
       Control tableControl = m_uiTableComposite.getUiTableViewer().getControl();
 
       if (!RwtUtility.isAncestorOf(tableControl, candidate)) {
         for (CellEditor editor : viewer.getCellEditors()) {
           if (editor != null && editor.isActivated() && editor instanceof P_RwtCellEditor) {
             ((P_RwtCellEditor) editor).stopCellEditing();
             break;
           }
         }
       }
     }
   }
 
   /**
    * Delegate to process focus events on cell editor
    */
   private interface IFocusDelegate {
     public void doSetFocus();
   }
 
   /**
    * Listener to get notified about deactivation event
    */
   private interface IDeactivateListener {
     public void canceled(ColumnViewerEditorDeactivationEvent event);
 
     public void saved(ColumnViewerEditorDeactivationEvent event);
   }
 }
