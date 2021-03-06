 /*
  * $Id$
  *
  * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
  * Santa Clara, California 95054, U.S.A. All rights reserved.
  */
 
 package org.jdesktop.swingx;
 
 import java.awt.BorderLayout;
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Container;
 import java.awt.Dimension;
 import java.awt.Font;
 import java.awt.KeyboardFocusManager;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.FocusAdapter;
 import java.awt.event.FocusEvent;
 import java.text.Format;
 import java.text.NumberFormat;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.List;
 import java.util.Random;
 import java.util.logging.Logger;
 
 import javax.swing.AbstractAction;
 import javax.swing.AbstractButton;
 import javax.swing.Action;
 import javax.swing.BorderFactory;
 import javax.swing.Box;
 import javax.swing.JButton;
 import javax.swing.JComponent;
 import javax.swing.JFormattedTextField;
 import javax.swing.JFrame;
 import javax.swing.JScrollPane;
 import javax.swing.JTable;
 import javax.swing.JToolBar;
 import javax.swing.JViewport;
 import javax.swing.KeyStroke;
 import javax.swing.SortOrder;
 import javax.swing.SwingUtilities;
 import javax.swing.RowSorter.SortKey;
 import javax.swing.event.TableColumnModelEvent;
 import javax.swing.table.DefaultTableModel;
 import javax.swing.table.TableCellEditor;
 import javax.swing.table.TableCellRenderer;
 import javax.swing.table.TableColumn;
 import javax.swing.table.TableModel;
 
 import org.jdesktop.swingx.JXTable.NumberEditor;
 import org.jdesktop.swingx.action.AbstractActionExt;
 import org.jdesktop.swingx.decorator.AbstractHighlighter;
 import org.jdesktop.swingx.decorator.ComponentAdapter;
 import org.jdesktop.swingx.decorator.HighlightPredicate;
 import org.jdesktop.swingx.decorator.Highlighter;
 import org.jdesktop.swingx.decorator.HighlighterFactory;
 import org.jdesktop.swingx.hyperlink.LinkModel;
 import org.jdesktop.swingx.hyperlink.LinkModelAction;
 import org.jdesktop.swingx.renderer.CheckBoxProvider;
 import org.jdesktop.swingx.renderer.DefaultTableRenderer;
 import org.jdesktop.swingx.renderer.HyperlinkProvider;
 import org.jdesktop.swingx.search.SearchFactory;
 import org.jdesktop.swingx.sort.DefaultSortController;
 import org.jdesktop.swingx.table.ColumnFactory;
 import org.jdesktop.swingx.table.DatePickerCellEditor;
 import org.jdesktop.swingx.table.NumberEditorExt;
 import org.jdesktop.swingx.table.TableColumnExt;
 import org.jdesktop.swingx.treetable.FileSystemModel;
 import org.jdesktop.test.AncientSwingTeam;
 import org.junit.Test;
 
 /**
  * Split from old JXTableUnitTest - contains "interactive"
  * methods only. <p>
  * 
  * PENDING: too many frames to fit all on screen - either split into different
  * tests or change positioning algo to start on top again if hidden. <p>
  * @author Jeanette Winzenburg
  */
 public class JXTableVisualCheck extends JXTableUnitTest {
     private static final Logger LOG = Logger.getLogger(JXTableVisualCheck.class
             .getName());
     public static void main(String args[]) {
       JXTableVisualCheck test = new JXTableVisualCheck();
       try {
 //        test.runInteractiveTests();
           test.runInteractiveTests("interactive.*Sort.*");
 //          test.runInteractiveTests("interactive.*Header.*");
 //          test.runInteractiveTests("interactive.*ColumnProp.*");
 //          test.runInteractiveTests("interactive.*Multiple.*");
 //          test.runInteractiveTests("interactive.*RToL.*");
 //          test.runInteractiveTests("interactive.*Scrollable.*");
 //          test.runInteractiveTests("interactive.*isable.*");
           
 //          test.runInteractiveTests("interactive.*Policy.*");
 //        test.runInteractiveTests("interactive.*Rollover.*");
 //        test.runInteractiveTests("interactive.*UpdateUI.*");
 //        test.runInteractiveTests("interactiveColumnHighlighting");
       } catch (Exception e) {
           System.err.println("exception when executing interactive tests:");
           e.printStackTrace();
       }
   }
 
     
     @Override
     protected void setUp() throws Exception {
         super.setUp();
         // super has LF specific tests...
         setSystemLF(true);
 //        setLookAndFeel("Nimbus");
     }
 
     /**
      * Quick check of sort order cycle (including pathologicals)
      */
     public void interactiveSortOrderCycle() {
         final JXTable table = new JXTable(new AncientSwingTeam());
         JXFrame frame = wrapWithScrollingInFrame(table, new JTable(table.getModel()), "sort cycles");
         Action three = new AbstractAction("three-cylce") {
             
             @Override
             public void actionPerformed(ActionEvent e) {
                table.getSortController().setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
             }
         };
         addAction(frame, three);
         Action two = new AbstractAction("two-cylce") {
             
             @Override
             public void actionPerformed(ActionEvent e) {
                table.getSortController().setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING);
             }
         };
         addAction(frame, two);
         Action one = new AbstractAction("one-cylce") {
             
             @Override
             public void actionPerformed(ActionEvent e) {
                table.getSortController().setSortOrderCycle(SortOrder.DESCENDING);
             }
         };
         addAction(frame, one);
         Action none = new AbstractAction("empty-cylce") {
             
             @Override
             public void actionPerformed(ActionEvent e) {
                table.getSortController().setSortOrderCycle();
             }
         };
         addAction(frame, none);
         show(frame);
     }
 
     public void interactiveMultiColumnSort() {
         DefaultTableModel model = createMultiSortModel();
         JXTable table = new JXTable(model);
         table.setVisibleRowCount(model.getRowCount());
         JXFrame frame = wrapWithScrollingInFrame(table, "multi-column-sort");
         final DefaultSortController<?> rowSorter = (DefaultSortController<?>) table.getRowSorter();
         final List<SortKey> sortKeys = new ArrayList<SortKey>();
         for (int i = 0; i < rowSorter.getMaxSortKeys(); i++) {
             sortKeys.add(new SortKey(i, SortOrder.ASCENDING));
         }
         Action setSortKeys = new AbstractAction("sortKeys") {
             
             @Override
             public void actionPerformed(ActionEvent e) {
                 rowSorter.setSortKeys(sortKeys);
             }
         };
         addAction(frame, setSortKeys);
         Action reset = new AbstractAction("resetSort") {
             
             @Override
             public void actionPerformed(ActionEvent e) {
                 rowSorter.setSortKeys(null);
                 
             }
         };
         rowSorter.setSortable(0, false);
         addAction(frame, reset);
         show(frame);
     }
 
 
     /**
      * @return
      */
     private DefaultTableModel createMultiSortModel() {
         String[] first = { "animal", "plant" };
         String[] second = {"insect", "mammal", "spider" };
         String[] third = {"red", "green", "yellow", "blue" };
         Integer[] age = { 1, 5, 12, 20, 100 };
         Object[][] rows = new Object[][] { first, second, third, age };
         DefaultTableModel model = new DefaultTableModel(20, 4) {
 
             @Override
             public Class<?> getColumnClass(int columnIndex) {
                 return columnIndex == getColumnCount() - 1 ? 
                         Integer.class : super.getColumnClass(columnIndex);
             }
             
         };
         for (int i = 0; i < rows.length; i++) {
             setValues(model, rows[i], i);
         }
         return model;
     }
     /**
      * @param model
      * @param first
      * @param i
      */
     private void setValues(DefaultTableModel model, Object[] first, int column) {
         Random seed = new Random();
         for (int row = 0; row < model.getRowCount(); row++) {
             int random = seed.nextInt(first.length);
             model.setValueAt(first[random], row, column);
         }
     }
 
     /**
      * Issue #908-swingx: move updateUI responsibility into column.
      * 
      */
     public void interactiveUpdateUIEditors() {
         DefaultTableModel model = new DefaultTableModel(5, 5) {
 
             @Override
             public Class<?> getColumnClass(int columnIndex) {
                 if (getValueAt(0, columnIndex) == null)
                     return super.getColumnClass(columnIndex);
                 return getValueAt(0, columnIndex).getClass();
             }
             
         };
         for (int i = 0; i < model.getRowCount(); i++) {
             model.setValueAt(new Date(), i, 0);
             model.setValueAt(true, i, 1);
         }
         JXTable table = new JXTable(model);
         TableCellEditor editor = new DatePickerCellEditor();
         table.getColumn(0).setCellEditor(editor);
         table.getColumn(4).setCellRenderer(new DefaultTableRenderer(new CheckBoxProvider()));
         showWithScrollingInFrame(table, "toggle ui - must update editors/renderers");
     }
     /**
      * Issue #550-swingx: xtable must not reset columns' pref/size on 
      * structureChanged if autocreate is false.
      * 
      *  
      */
     public void interactiveColumnWidthOnStructureChanged() {
         final JXTable table = new JXTable(new AncientSwingTeam());
         table.setAutoCreateColumnsFromModel(false);
         table.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
         table.setColumnControlVisible(true);
         // min/max is respected
 //        mini.setMaxWidth(5);
 //        mini.setMinWidth(5);
         Action structureChanged = new AbstractAction("fire structure changed") {
 
             public void actionPerformed(ActionEvent e) {
                 table.tableChanged(null);
             }
             
         };
         JXFrame frame = showWithScrollingInFrame(table, "structure change must not re-size columns");
         addAction(frame, structureChanged);
         show(frame);
     }
     
 
     /**
      * Issue #675-swingx: esc doesn't reach rootpane.
      * 
      * Verify that the escape is intercepted only if editing.
      * BUT: (core behaviour) starts editing in table processKeyBinding. So every
      * second is not passed on.
      */
     public void interactiveDialogCancelOnEscape() {
         Action cancel = new AbstractActionExt("cancel") {
 
             public void actionPerformed(ActionEvent e) {
                 LOG.info("performed: cancel action");
                 
             }
             
         };
         final JButton field = new JButton(cancel);
         JXTable xTable = new JXTable(10, 3);
         JTable table = new JTable(xTable.getModel());
         JXFrame frame = wrapWithScrollingInFrame(xTable, table, "escape passed to rootpane (if editing)");
         frame.setCancelButton(field);
         frame.add(field, BorderLayout.SOUTH);
         frame.setVisible(true);
     }
     
 
     
     /**
      * Issue #508/547-swingx: clean up of pref scrollable.
      * Visual check: column init on model change.
      *
      */
      public void interactivePrefScrollable() {
         final DefaultTableModel tableModel = new DefaultTableModel(30, 7);
         final AncientSwingTeam ancientSwingTeam = new AncientSwingTeam();
         final JXTable table = new JXTable(tableModel);
         table.setColumnControlVisible(true);
         table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
         final JXFrame frame = showWithScrollingInFrame(table, "initial sizing");
         addMessage(frame, "initial size: " + table.getPreferredScrollableViewportSize());
         Action action = new AbstractActionExt("toggle model") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setModel(table.getModel() == tableModel ? ancientSwingTeam : tableModel);
                 frame.pack();
             }
             
         };
         addAction(frame, action);
         frame.pack();
     }
 
      /**
      * Issue #508/547-swingx: clean up of pref scrollable.
       * Visual check: dynamic logical scroll sizes
       * Toggle visual row/column count.
       */
      public void interactivePrefScrollableDynamic() {
          final AncientSwingTeam ancientSwingTeam = new AncientSwingTeam();
          final JXTable table = new JXTable(ancientSwingTeam);
          table.setColumnControlVisible(true);
          table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
          final JXFrame frame = wrapWithScrollingInFrame(table, "Dynamic pref scrollable");
          Action action = new AbstractActionExt("vis row") {
              
              public void actionPerformed(ActionEvent e) {
                  int visRowCount = table.getVisibleRowCount() + 5;
                  if (visRowCount > 30) {
                      visRowCount = 10;
                  }
                  table.setVisibleRowCount(visRowCount);
                  frame.pack();
              }
              
          };
          addAction(frame, action);
          Action columnAction = new AbstractActionExt("vis column") {
              
              public void actionPerformed(ActionEvent e) {
                  int visColumnCount = table.getVisibleColumnCount();
                  if (visColumnCount > 8) {
                      visColumnCount = -1;
                  } else if (visColumnCount < 0 ) {
                      visColumnCount = 2;
                  } else {
                      visColumnCount += 2;
                  }
                  table.setVisibleColumnCount(visColumnCount);
                  frame.pack();
              }
              
          };
          addAction(frame, columnAction);
          frame.setVisible(true);
          frame.pack();
      }
 
 
  
     /**
      * Issue #393-swingx: localized NumberEditor.
      * 
      * Playing ... looks working :-)
      *
      *  
      */
     public void interactiveFloatingPointEditor(){
         DefaultTableModel model = new DefaultTableModel(
                 new String[] {"Double-core", "Double-ext", "Integer-core", "Integer-ext", "Object"}, 10) {
 
             @Override
             public Class<?> getColumnClass(int columnIndex) {
                 if ((columnIndex == 0) || (columnIndex == 1)) {
                     return Double.class;
                 }
                 if ((columnIndex == 2) || (columnIndex == 3)){
                     return Integer.class;
                 }
                 return Object.class;
             }
             
         };
         final JXTable table = new JXTable(model);
         table.setSurrendersFocusOnKeystroke(true);
         table.setValueAt(10.2, 0, 0);
         table.setValueAt(10.2, 0, 1);
         table.setValueAt(10, 0, 2);
         table.setValueAt(10, 0, 3);
         
         NumberEditor numberEditor = new NumberEditor();
         table.getColumn(0).setCellEditor(numberEditor);
         table.getColumn(2).setCellEditor(numberEditor);
         showWithScrollingInFrame(table, "Extended NumberEditors (col 1/3)");
     }
 
     /**
      *  Issue #??-swingx: default number editor shows 3 digits only.
      *  
      *  Compare with plain JFromattedTextField and default NumberFormat - same. 
      *  To see, type a number with fractional digits > 3 in the first text field
      *  and press commit or transfer focus away. 
      */
     public void interactiveFloatingPointEditorDigits(){
         DefaultTableModel model = new DefaultTableModel(
                 new String[] {"Double-default", "Double-customMaxDigits"}, 10) {
 
             @Override
             public Class<?> getColumnClass(int columnIndex) {
                 if ((columnIndex == 0) || (columnIndex == 1)) {
                     return Double.class;
                 }
                 if ((columnIndex == 2) || (columnIndex == 3)){
                     return Integer.class;
                 }
                 return Object.class;
             }
             
         };
         final JXTable table = new JXTable(model);
         table.setSurrendersFocusOnKeystroke(true);
         table.setValueAt(10.2, 0, 0);
         table.setValueAt(10.2, 0, 1);
         NumberFormat moreFractionalDigits = NumberFormat.getInstance();
         moreFractionalDigits.setMaximumFractionDigits(20);
         NumberEditorExt numberEditor = new NumberEditorExt(moreFractionalDigits);
         table.getColumn(1).setCellEditor(numberEditor);
         JXFrame frame = showWithScrollingInFrame(table, "Extended NumberEditors (col 1/3)");
         Format format = NumberFormat.getInstance();
         final JFormattedTextField field = new JFormattedTextField(format);
         field.setColumns(10);
         final JFormattedTextField target = new JFormattedTextField(format);
         target.setColumns(10);
         field.addActionListener(new ActionListener() {
 
             public void actionPerformed(ActionEvent e) {
                 target.setValue(field.getValue());
                 LOG.info("value: " + field.getValue());
             }
             
         });
         FocusAdapter focusAdapter = new FocusAdapter() {
 
             @Override
             public void focusLost(FocusEvent e) {
                 LOG.info("field value: " + field.getValue());
                 LOG.info("table value: " + table.getValueAt(0, 1));
             }
             
         };
         field.addFocusListener(focusAdapter);
         table.addFocusListener(focusAdapter);
         addStatusComponent(frame, field);
         addStatusComponent(frame, target);
     }
     /**
      * Issue #417-swingx: disable default find.
      *
      * Possible alternative to introducing disable api as suggested in the
      * issue report: disable the action? Move the action up the hierarchy to
      * the parent actionmap? Maybe JX specific parent?
      *  
      */
     public void interactiveDisableFind() {
         final JXTable table = new JXTable(sortableTableModel);
         Action findAction = new AbstractActionExt() {
 
             public void actionPerformed(ActionEvent e) {
                 SearchFactory.getInstance().showFindDialog(table, table.getSearchable());
                 
             }
             
             @Override
             public boolean isEnabled() {
                 return false;
             }
             
         };
         table.getActionMap().put("find", findAction);
         showWithScrollingInFrame(table, "disable finding");
     }
     
     /**
      * visually check if we can bind the CCB's action to a keystroke.
      * 
      * Working, but there's a visual glitch if opened by keystroke: 
      * the popup is not trailing aligned to the CCB. And the 
      * CCB must be visible, otherwise there's an IllegalStateException
      * because the popup tries to position itself relative to the CCB.
      *
      */
     public void interactiveKeybindingColumnControl() {
         JXTable table = new JXTable(sortableTableModel);
         // JW: currently the CCB must be visible
 //        table.setColumnControlVisible(true);
         Action openCCPopup = ((AbstractButton) table.getColumnControl()).getAction();
         String actionKey = "popupColumnControl";
         table.getActionMap().put(actionKey, openCCPopup);
         KeyStroke keyStroke = KeyStroke.getKeyStroke("F2");
         table.getInputMap(JXTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, actionKey);
         showWithScrollingInFrame(table, "Press F2 to open column control");
     }
     /**
      * calculate reasonable table rowHeight withouth "white pixel" in editor.
      * Compare table and xtable
      */
     public void interactiveCompareRowHeight() {
         JXTable xtable = new JXTable(sortableTableModel);
         xtable.setShowGrid(false, false);
         JTable table = new JTable(sortableTableModel);
         table.setShowHorizontalLines(false);
         table.setShowVerticalLines(false);
         table.setRowMargin(0);
         table.getColumnModel().setColumnMargin(0);
         JXFrame frame = wrapWithScrollingInFrame(xtable, table, "compare default rowheight of xtable vs. table");
         frame.setVisible(true);
     }
     
     /**
      * visually check if terminateEditOnFocusLost, autoStartEdit
      * work as expected.
      *
      */
     public void interactiveToggleEditProperties() {
         final JXTable table = new JXTable(10, 2);
         JXFrame frame = wrapWithScrollingInFrame(table, new JButton("something to focus"), 
                 "JXTable: toggle terminate/autoStart on left (right is dummy) ");
         Action toggleTerminate = new AbstractAction("toggleTerminate") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setTerminateEditOnFocusLost(!table.isTerminateEditOnFocusLost());
                 
             }
             
         };
         addAction(frame, toggleTerminate);
         Action toggleAutoStart = new AbstractAction("toggleAutoStart") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setAutoStartEditOnKeyStroke(!table.isAutoStartEditOnKeyStroke());
                 
             }
             
         };
         addAction(frame, toggleAutoStart);
         frame.setVisible(true);
         
     }
 
     /**
      * Expose sorted column. 
      * Example how to guarantee one column sorted at all times.
      */
     public void interactiveAlwaysSorted() {
         final JXTable table = new JXTable(sortableTableModel) {
 
             @Override
             public void columnRemoved(TableColumnModelEvent e) {
                 super.columnRemoved(e);
                 if (!hasVisibleSortedColumn()) {
                     toggleSortOrder(0);
                 }
             }
 
             private boolean hasVisibleSortedColumn() {
                 TableColumn column = getSortedColumn();
                 if (column instanceof TableColumnExt) {
                     return ((TableColumnExt) column).isVisible();
                 }
                 // JW: this path is not tested, don't really expect
                 // non-ext column types, though JXTable must 
                 // cope with them
                 return column != null;
             }
 
             
         };
         table.setColumnControlVisible(true);
         JXFrame frame = wrapWithScrollingInFrame(table, "Always sorted");
         frame.setVisible(true);
         
     }
    
     /**
      * Issue #282-swingx: compare disabled appearance of
      * collection views.
      *
      */
     public void interactiveDisabledCollectionViews() {
         final JXTable table = new JXTable(new AncientSwingTeam());
         table.setEnabled(false);
         final JXList list = new JXList(new String[] {"one", "two", "and something longer"});
         list.setEnabled(false);
         final JXTree tree = new JXTree(new FileSystemModel());
         tree.setEnabled(false);
         JComponent box = Box.createHorizontalBox();
         box.add(new JScrollPane(table));
         box.add(new JScrollPane(list));
         box.add(new JScrollPane(tree));
         JXFrame frame = wrapInFrame(box, "disabled collection views");
         AbstractAction action = new AbstractAction("toggle disabled") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setEnabled(!table.isEnabled());
                 list.setEnabled(!list.isEnabled());
                 tree.setEnabled(!tree.isEnabled());
             }
             
         };
         addAction(frame, action);
         frame.setVisible(true);
         
     }
 
     /**
      * Issue #281-swingx: header should be auto-repainted on changes to
      * header title, value.
      * 
      *
      */
     public void interactiveUpdateHeader() {
         final JXTable table = new JXTable(10, 2);
         JXFrame frame = wrapWithScrollingInFrame(table, "update header");
         Action action = new AbstractAction("update headervalue") {
             int count;
             public void actionPerformed(ActionEvent e) {
                 table.getColumn(0).setHeaderValue("A" + count++);
                 
             }
             
         };
         addAction(frame, action);
         action = new AbstractAction("update column title") {
             int count;
             public void actionPerformed(ActionEvent e) {
                 table.getColumnExt(0).setTitle("A" + count++);
                 
             }
             
         };
         addAction(frame, action);
         frame.setVisible(true);
         
     }
     
     /**
      * Issue #281-swingx, Issue #334-swing: 
      * header should be auto-repainted on changes to
      * header title, value. Must update size if appropriate.
      * 
      * still open: core #4292511 - autowrap not really working
      *
      */
     public void interactiveUpdateHeaderAndSizeRequirements() {
         
         final String[] alternate = { 
                 "simple", 
 //                "<html><b>This is a test of a large label to see if it wraps </font></b>",
 //                "simple", 
                  "<html><center>Line 1<br>Line 2</center></html>" 
                 };
         final JXTable table = new JXTable(10, 2);
         
         JXFrame frame = wrapWithScrollingInFrame(table, "update header");
         Action action = new AbstractAction("update headervalue") {
             boolean first;
             public void actionPerformed(ActionEvent e) {
                 table.getColumn(1).setHeaderValue(first ? alternate[0] : alternate[1]);
                 first = !first;
                 
             }
             
         };
         addAction(frame, action);
         frame.setVisible(true);
         
     }
     
     /**
      * Issue #??-swingx: column auto-sizing support.
      *
      */
     public void interactiveTestExpandsToViewportWidth() {
         final JXTable table = new JXTable();
         ColumnFactory factory = new ColumnFactory() {
             @Override
             public void configureTableColumn(TableModel model, TableColumnExt columnExt) {
                  super.configureTableColumn(model, columnExt);
                  if (model.getColumnClass(columnExt.getModelIndex()) == Integer.class) {
                      // to see the effect: excess width is distributed relative
                      // to the difference between maxSize and prefSize
                      columnExt.setMaxWidth(200);
                  } else {
                  
                      columnExt.setMaxWidth(1024);
                  }
             }
             
         };
         table.setColumnFactory(factory);
         table.setColumnControlVisible(true);
         table.setModel(sortableTableModel);
         table.setHorizontalScrollEnabled(true);
         table.packAll();
         JXFrame frame = wrapWithScrollingInFrame(table, "expand to width");
         Action toggleModel = new AbstractAction("toggle model") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setModel(table.getModel() == sortableTableModel ? 
                         new DefaultTableModel(20, 4) : sortableTableModel);
                 
             }
             
         };
         addAction(frame, toggleModel);
         frame.setSize(table.getPreferredSize().width - 50, 300);
         frame.setVisible(true);
         LOG.info("table: " + table.getWidth());
         LOG.info("Viewport: " + table.getParent().getWidth());
     }
     
     /** 
      * Issue ??: Anchor lost after receiving a structure changed.
      * Lead/anchor no longer automatically initialized - no visual clue
      * if table is focused. 
      *
      */
     public void interactiveTestToggleTableModelU6() {
         final DefaultTableModel tableModel = createAscendingModel(0, 20);
         final JTable table = new JTable(tableModel);
         // JW: need to explicitly set _both_ anchor and lead to >= 0
         // need to set anchor first
         table.getSelectionModel().setAnchorSelectionIndex(0);
         table.getSelectionModel().setLeadSelectionIndex(0);
         table.getColumnModel().getSelectionModel().setAnchorSelectionIndex(0);
         table.getColumnModel().getSelectionModel().setLeadSelectionIndex(0);
         Action toggleAction = new AbstractAction("Toggle TableModel") {
 
             public void actionPerformed(ActionEvent e) {
                 TableModel model = table.getModel();
                 table.setModel(model.equals(tableModel) ? sortableTableModel : tableModel);
             }
             
         };
         JXFrame frame = wrapWithScrollingInFrame(table, "JTable - anchor lost after structure changed");
         addAction(frame, toggleAction);
         frame.setVisible(true);
         frame.pack();
         SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 // sanity - focus is on table
                 LOG.info("isFocused? " + table.hasFocus());
                 LOG.info("who has focus? " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner());
             }
         });
     }
 
     /**
      * Issue #186-swingxProblem with lead/selection and buttons as editors:
      * - move focus (using arrow keys) to first editable boolean  
      * - press space to toggle boolean
      * - move focus to next row (same column)
      * - press space to toggle boolean
      * - move back to first row (same column)
      * - press space: boolean is toggled and (that's the problem) 
      *  lead selection is moved to next row.
      *  No problem in JTable.
      *
      */
     public void interactiveTestCompareTableBoolean() {
         JXTable xtable = new JXTable(createModelWithBooleans());
         JTable table = new JTable(createModelWithBooleans()); 
         JXFrame frame = wrapWithScrollingInFrame(xtable, table, "Compare boolean renderer JXTable <--> JTable");
         frame.setVisible(true);
     }
 
     private TableModel createModelWithBooleans() {
         String[] columnNames = { "text only", "Bool editable", "Bool not-editable" };
         
         DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
             @Override
             public Class<?> getColumnClass(int column) {
                 return getValueAt(0, column).getClass();
             }
 
             @Override
             public boolean isCellEditable(int row, int column) {
                     return !getColumnName(column).contains("not");
             }
             
         };
         for (int i = 0; i < 4; i++) {
                 model.addRow(new Object[] {"text only " + i, Boolean.TRUE, Boolean.TRUE });
         }
         return model;
     }
 
 
     /**
      * Issue #89-swingx: ColumnControl not updated with ComponentOrientation.
      *
      */
     public void interactiveRToLTableWithColumnControl() {
         final JXTable table = new JXTable(createAscendingModel(0, 20));
          JScrollPane pane = new JScrollPane(table);
         final JXFrame frame = wrapInFrame(pane, "RToLScrollPane");
         addComponentOrientationToggle(frame);
         Action toggleColumnControl = new AbstractAction("toggle column control") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setColumnControlVisible(!table.isColumnControlVisible());
                 
             }
             
         };
         addAction(frame, toggleColumnControl);
         frame.setVisible(true);
     }
     
 
     
     
     /**
      * Issue #179: Sorter does not use collator if cell content is
      *  a String.
      *
      */
     public void interactiveTestLocaleSorter() {
         
         Object[][] rowData = new Object[][] {
                 new Object[] { Boolean.TRUE, "aa" },
                 new Object[] { Boolean.FALSE, "AB" },
                 new Object[] { Boolean.FALSE, "AC" },
                 new Object[] { Boolean.TRUE, "BA" },
                 new Object[] { Boolean.FALSE, "BB" },
                 new Object[] { Boolean.TRUE, "BC" } };
         String[] columnNames = new String[] { "Critical", "Task" };
         DefaultTableModel model =  new DefaultTableModel(rowData, columnNames);
 //        {
 //            public Class getColumnClass(int column) {
 //                return column == 1 ? String.class : super.getColumnClass(column);
 //            }
 //        };
         final JXTable table = new JXTable(model);
         table.toggleSortOrder(1);
         JFrame frame = wrapWithScrollingInFrame(table, "locale sorting");
         frame.setVisible(true);
     }   
     
 
     /** 
      * Issue #155-swingx: vertical scrollbar policy lost.
      *
      */
     public void interactiveTestColumnControlConserveVerticalScrollBarPolicyAlways() {
         final JXTable table = new JXTable();
         Action toggleAction = new AbstractAction("Toggle Control") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setColumnControlVisible(!table.isColumnControlVisible());
                 
             }
             
         };
         table.setModel(new DefaultTableModel(10, 5));
         // initial state of column control visibility doesn't seem to matter
 //      table.setColumnControlVisible(true);
         final JScrollPane scrollPane1 = new JScrollPane(table);
         scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
         
         final JXFrame frame = wrapInFrame(scrollPane1, "JXTable Vertical ScrollBar Policy - always");
         addAction(frame, toggleAction);
         Action packAction = new AbstractAction("Pack frame") {
 
             public void actionPerformed(ActionEvent e) {
                 frame.remove(scrollPane1);
                 frame.add(scrollPane1);
             }
             
         };
         addAction(frame, packAction);
         frame.setVisible(true);
     }
 
 
     /** 
      * Issue #155-swingx: vertical scrollbar policy lost.
      *
      */
     public void interactiveTestColumnControlConserveVerticalScrollBarPolicyNever() {
         final JXTable table = new JXTable();
         Action toggleAction = new AbstractAction("Toggle Control") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setColumnControlVisible(!table.isColumnControlVisible());
                 
             }
             
         };
         table.setModel(new DefaultTableModel(10, 5));
         // initial state of column control visibility doesn't seem to matter
 //        table.setColumnControlVisible(true);
         final JScrollPane scrollPane1 = new JScrollPane(table);
         scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
         
         final JXFrame frame = wrapInFrame(scrollPane1, "JXTable Vertical ScrollBar Policy - never");
         addAction(frame, toggleAction);
         Action packAction = new AbstractAction("Pack frame") {
 
             public void actionPerformed(ActionEvent e) {
                 frame.remove(scrollPane1);
                 frame.add(scrollPane1);
             }
             
         };
         addAction(frame, packAction);
         frame.setVisible(true);
     }
 
     /** 
      * Issue #11: Column control not showing with few rows.
      *
      */
     public void interactiveTestColumnControlFewRows() {
         final JXTable table = new JXTable();
         Action toggleAction = new AbstractAction("Toggle Control") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setColumnControlVisible(!table.isColumnControlVisible());
                 
             }
             
         };
         table.setModel(new DefaultTableModel(10, 5));
         table.setColumnControlVisible(true);
         JXFrame frame = wrapWithScrollingInFrame(table, "JXTable ColumnControl with few rows");
         addAction(frame, toggleAction);
         frame.setVisible(true);
     }
 
     /** 
      * check behaviour outside scrollPane
      *
      */
     public void interactiveTestColumnControlWithoutScrollPane() {
         final JXTable table = new JXTable();
         Action toggleAction = new AbstractAction("Toggle Control") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setColumnControlVisible(!table.isColumnControlVisible());
                 
             }
             
         };
         toggleAction.putValue(Action.SHORT_DESCRIPTION, "does nothing visible - no scrollpane");
         table.setModel(new DefaultTableModel(10, 5));
         table.setColumnControlVisible(true);
         JXFrame frame = wrapInFrame(table, "JXTable: Toggle ColumnControl outside ScrollPane");
         addAction(frame, toggleAction);
         frame.setVisible(true);
     }
 
     /** 
      * check behaviour of moving into/out of scrollpane.
      *
      */
     public void interactiveTestToggleScrollPaneWithColumnControlOn() {
         final JXTable table = new JXTable();
         table.setModel(new DefaultTableModel(10, 5));
         table.setColumnControlVisible(true);
         final JXFrame frame = wrapInFrame(table, "JXTable: Toggle ScrollPane with Columncontrol on");
         Action toggleAction = new AbstractAction("Toggle ScrollPane") {
 
             public void actionPerformed(ActionEvent e) {
                 Container parent = table.getParent();
                 boolean inScrollPane = parent instanceof JViewport;
                 if (inScrollPane) {
                     JScrollPane scrollPane = (JScrollPane) table.getParent().getParent();
                     frame.getContentPane().remove(scrollPane);
                     frame.getContentPane().add(table);
                 } else {
                   parent.remove(table);
                   parent.add(new JScrollPane(table));
                 }
                 frame.pack();
                               
             }
             
         };
         addAction(frame, toggleAction);
         frame.setVisible(true);
     }
 
     /** 
      *  TableColumnExt: user friendly resizable  
      * 
      */
     public void interactiveTestColumnResizable() {
         final JXTable table = new JXTable(sortableTableModel);
         table.setColumnControlVisible(true);
         final TableColumnExt priorityColumn = table.getColumnExt("First Name");
         JXFrame frame = wrapWithScrollingInFrame(table, "JXTable: Column with Min=Max not resizable");
         Action action = new AbstractAction("Toggle MinMax of FirstName") {
 
             public void actionPerformed(ActionEvent e) {
                 // user-friendly resizable flag
                 if (priorityColumn.getMinWidth() == priorityColumn.getMaxWidth()) {
                     priorityColumn.setMinWidth(50);
                     priorityColumn.setMaxWidth(150);
                 } else {
                     priorityColumn.setMinWidth(100);
                     priorityColumn.setMaxWidth(100);
                 }
             }
             
         };
         addAction(frame, action);
         frame.setVisible(true);
     }
     
     /**
      */
     public void interactiveTestToggleSortable() {
         final JXTable table = new JXTable(sortableTableModel);
         table.setColumnControlVisible(true);
         Action toggleSortableAction = new AbstractAction("Toggle Sortable") {
 
             public void actionPerformed(ActionEvent e) {
                 table.setSortable(!table.isSortable());
                 
             }
             
         };
         JXFrame frame = wrapWithScrollingInFrame(table, "ToggleSortingEnabled Test");
         addAction(frame, toggleSortableAction);
         frame.setVisible(true);  
         
     }
     public void interactiveTestTableSizing1() {
         JXTable table = new JXTable();
         table.setAutoCreateColumnsFromModel(false);
         table.setModel(tableModel);
         installLinkRenderer(table);
         table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
 
         TableColumnExt columns[] = new TableColumnExt[tableModel.getColumnCount()];
         for (int i = 0; i < columns.length; i++) {
             columns[i] = new TableColumnExt(i);
             table.addColumn(columns[i]);
         }
         columns[0].setPrototypeValue(new Integer(0));
         columns[1].setPrototypeValue("Simple String Value");
         columns[2].setPrototypeValue(new Integer(1000));
         columns[3].setPrototypeValue(Boolean.TRUE);
         columns[4].setPrototypeValue(new Date(100));
         columns[5].setPrototypeValue(new Float(1.5));
         columns[6].setPrototypeValue(new LinkModel("Sun Micro", "_blank",
                                               tableModel.linkURL));
         columns[7].setPrototypeValue(new Integer(3023));
         columns[8].setPrototypeValue("John Doh");
         columns[9].setPrototypeValue("23434 Testcase St");
         columns[10].setPrototypeValue(new Integer(33333));
         columns[11].setPrototypeValue(Boolean.FALSE);
 
         table.setVisibleRowCount(12);
 
         JFrame frame = wrapWithScrollingInFrame(table, "TableSizing1 Test");
         frame.setVisible(true);
     }
 
 
     private void installLinkRenderer(JXTable table) {
         LinkModelAction<?> action = new LinkModelAction<LinkModel>() {
 
             @Override
             public void actionPerformed(ActionEvent e) {
                 LOG.info("activated link: " + getTarget());
             }
              
         };
         TableCellRenderer linkRenderer = new DefaultTableRenderer(
                 new HyperlinkProvider(action, LinkModel.class));
         table.setDefaultRenderer(LinkModel.class, linkRenderer);
     }
 
     public void interactiveTestEmptyTableSizing() {
         JXTable table = new JXTable(0, 5);
         table.setColumnControlVisible(true);
         JFrame frame = wrapWithScrollingInFrame(table, "Empty Table (0 rows)");
         frame.setVisible(true);
         
     }
     public void interactiveTestTableSizing2() {
         JXTable table = new JXTable();
         table.setAutoCreateColumnsFromModel(false);
         table.setModel(tableModel);
         installLinkRenderer(table);
 
         TableColumnExt columns[] = new TableColumnExt[6];
         int viewIndex = 0;
         for (int i = columns.length - 1; i >= 0; i--) {
             columns[viewIndex] = new TableColumnExt(i);
             table.addColumn(columns[viewIndex++]);
         }
         columns[5].setHeaderValue("String Value");
         columns[5].setPrototypeValue("9999");
         columns[4].setHeaderValue("String Value");
         columns[4].setPrototypeValue("Simple String Value");
         columns[3].setHeaderValue("Int Value");
         columns[3].setPrototypeValue(new Integer(1000));
         columns[2].setHeaderValue("Bool");
         columns[2].setPrototypeValue(Boolean.FALSE);
         //columns[2].setSortable(false);
         columns[1].setHeaderValue("Date");
         columns[1].setPrototypeValue(new Date(0));
         //columns[1].setSortable(false);
         columns[0].setHeaderValue("Float");
         columns[0].setPrototypeValue(new Float(5.5));
 
         table.setRowHeight(24);
         table.setRowMargin(2);
         JFrame frame = wrapWithScrollingInFrame(table, "TableSizing2 Test");
         frame.setVisible(true);
     }
 
 
     public void interactiveTestFocusedCellBackground() {
         TableModel model = new AncientSwingTeam() {
             @Override
             public boolean isCellEditable(int row, int column) {
                 return column != 0;
             }
         };
         JXTable xtable = new JXTable(model);
         xtable.setBackground(HighlighterFactory.NOTEPAD);
         JTable table = new JTable(model);
         table.setBackground(new Color(0xF5, 0xFF, 0xF5)); // ledger
         JFrame frame = wrapWithScrollingInFrame(xtable, table, "Unselected focused background: JXTable/JTable");
         frame.setVisible(true);
     }
 
 
     public void interactiveTestTableViewProperties() {
         JXTable table = new JXTable(tableModel);
         installLinkRenderer(table);
         table.setIntercellSpacing(new Dimension(15, 15));
         table.setRowHeight(48);
         JFrame frame = wrapWithScrollingInFrame(table, "TableViewProperties Test");
         frame.setVisible(true);
     }
 
     
     public void interactiveColumnHighlighting() {
         final JXTable table = new JXTable(new AncientSwingTeam());
         
         table.getColumnExt("Favorite Color").setHighlighters(new AbstractHighlighter() {
             @Override
             protected Component doHighlight(Component renderer, ComponentAdapter adapter) {
                 Color color = (Color) adapter.getValue();
                 
                 if (renderer instanceof JComponent) {
                     ((JComponent) renderer).setBorder(BorderFactory.createLineBorder(color));
                 }
                 
                 return renderer;
             }
         });
         
         JFrame frame = wrapWithScrollingInFrame(table, "Column Highlighter Test");
         JToolBar bar = new JToolBar();
         bar.add(new AbstractAction("Toggle") {
             boolean state = false;
             
             public void actionPerformed(ActionEvent e) {
                 if (state) {
                     table.getColumnExt("No.").setHighlighters(new Highlighter[0]);
                     state = false;
                 } else {
                     table.getColumnExt("No.").addHighlighter(
                         new AbstractHighlighter(new HighlightPredicate() {
                             public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
                                 return adapter.getValue().toString().contains("8");
                             }
                         }) {
                             @Override
                             protected Component doHighlight(Component renderer, ComponentAdapter adapter) {
                                 Font f = renderer.getFont().deriveFont(Font.ITALIC);
                                 renderer.setFont(f);
                                 
                                 return renderer;
                             }
                         }
                     );
                     state = true;
                 }
             }
         });
         frame.add(bar, BorderLayout.NORTH);
         frame.setVisible(true);
     }
     
     /**
      * dummy
      */
     @Test
     public void testDummy() {
     }   
 
 }
