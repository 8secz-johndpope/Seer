 /*
  * $Id$
  *
  * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
  * Santa Clara, California 95054, U.S.A. All rights reserved.
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  * 
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  * 
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */
 
 package org.jdesktop.swingx;
 
 import java.awt.Component;
 import java.awt.ComponentOrientation;
 import java.awt.Container;
 import java.awt.Cursor;
 import java.awt.Dimension;
 import java.awt.Point;
 import java.awt.Rectangle;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.print.PrinterException;
 import java.beans.PropertyChangeEvent;
 import java.beans.PropertyChangeListener;
 import java.lang.reflect.Field;
 import java.text.DateFormat;
 import java.text.NumberFormat;
 import java.util.Collections;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Vector;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import javax.swing.AbstractAction;
 import javax.swing.AbstractButton;
 import javax.swing.Action;
 import javax.swing.ActionMap;
 import javax.swing.DefaultCellEditor;
 import javax.swing.Icon;
 import javax.swing.ImageIcon;
 import javax.swing.JCheckBox;
 import javax.swing.JComponent;
 import javax.swing.JLabel;
 import javax.swing.JScrollPane;
 import javax.swing.JTable;
 import javax.swing.JViewport;
 import javax.swing.KeyStroke;
 import javax.swing.ListSelectionModel;
 import javax.swing.ScrollPaneConstants;
 import javax.swing.SizeSequence;
 import javax.swing.UIDefaults;
 import javax.swing.UIManager;
 import javax.swing.border.Border;
 import javax.swing.border.EmptyBorder;
 import javax.swing.event.ChangeEvent;
 import javax.swing.event.ChangeListener;
 import javax.swing.event.ListSelectionEvent;
 import javax.swing.event.TableColumnModelEvent;
 import javax.swing.event.TableModelEvent;
 import javax.swing.table.DefaultTableCellRenderer;
 import javax.swing.table.JTableHeader;
 import javax.swing.table.TableCellEditor;
 import javax.swing.table.TableCellRenderer;
 import javax.swing.table.TableColumn;
 import javax.swing.table.TableColumnModel;
 import javax.swing.table.TableModel;
 
 import org.jdesktop.swingx.action.BoundAction;
 import org.jdesktop.swingx.decorator.ComponentAdapter;
 import org.jdesktop.swingx.decorator.FilterPipeline;
 import org.jdesktop.swingx.decorator.Highlighter;
 import org.jdesktop.swingx.decorator.HighlighterPipeline;
 import org.jdesktop.swingx.decorator.PatternHighlighter;
 import org.jdesktop.swingx.decorator.PipelineEvent;
 import org.jdesktop.swingx.decorator.PipelineListener;
 import org.jdesktop.swingx.decorator.RowSizing;
 import org.jdesktop.swingx.decorator.SearchHighlighter;
 import org.jdesktop.swingx.decorator.Selection;
 import org.jdesktop.swingx.decorator.Sorter;
 import org.jdesktop.swingx.icon.ColumnControlIcon;
 import org.jdesktop.swingx.plaf.LookAndFeelAddons;
 import org.jdesktop.swingx.table.ColumnControlButton;
 import org.jdesktop.swingx.table.ColumnFactory;
 import org.jdesktop.swingx.table.DefaultTableColumnModelExt;
 import org.jdesktop.swingx.table.TableColumnExt;
 import org.jdesktop.swingx.table.TableColumnModelExt;
 
 /**
  * <p>
  * A JXTable is a JTable with built-in support for row sorting, filtering, and
  * highlighting, column visibility and a special popup control on the column
  * header for quick access to table configuration. You can instantiate a JXTable
  * just as you would a JTable, using a TableModel. However, a JXTable
  * automatically wraps TableColumns inside a TableColumnExt instance.
  * TableColumnExt supports visibility, sortability, and prototype values for
  * column sizing, none of which are available in TableColumn. You can retrieve
  * the TableColumnExt instance for a column using {@link #getColumnExt(Object)}
  * or {@link #getColumnExt(int colnumber)}.
  * 
  * <p>
  * A JXTable is, by default, sortable by clicking on column headers; each
  * subsequent click on a header reverses the order of the sort, and a sort arrow
  * icon is automatically drawn on the header. Sorting can be disabled using
  * {@link #setSortable(boolean)}. Sorting on columns is handled by a Sorter
  * instance which contains a Comparator used to compare values in two rows of a
  * column. You can replace the Comparator for a given column by using
  * <code>getColumnExt("column").getSorter().setComparator(customComparator)</code>
  * 
  * <p>
  * Columns can be hidden or shown by setting the visible property on the
  * TableColumnExt using {@link TableColumnExt#setVisible(boolean)}. Columns can
  * also be shown or hidden from the column control popup.
  * 
  * <p>
  * The column control popup is triggered by an icon drawn to the far right of
  * the column headers, above the table's scrollbar (when installed in a
  * JScrollPane). The popup allows the user to select which columns should be
  * shown or hidden, as well as to pack columns and turn on horizontal scrolling.
  * To show or hide the column control, use the
  * {@link #setColumnControlVisible(boolean show)}method.
  * 
  * <p>
  * Rows can be filtered from a JXTable using a Filter class and a
  * FilterPipeline. One assigns a FilterPipeline to the table using
  * {@link #setFilters(FilterPipeline)}. Filtering hides, but does not delete or
  * permanently remove rows from a JXTable. Filters are used to provide sorting
  * to the table--rows are not removed, but the table is made to believe rows in
  * the model are in a sorted order.
  * 
  * <p>
  * One can automatically highlight certain rows in a JXTable by attaching
  * Highlighters in the {@link #setHighlighters(HighlighterPipeline)}method. An
  * example would be a Highlighter that colors alternate rows in the table for
  * readability; AlternateRowHighlighter does this. Again, like Filters,
  * Highlighters can be chained together in a HighlighterPipeline to achieve more
  * interesting effects.
  * 
  * <p>
  * You can resize all columns, selected columns, or a single column using the
  * methods like {@link #packAll()}. Packing combines several other aspects of a
  * JXTable. If horizontal scrolling is enabled using
  * {@link #setHorizontalScrollEnabled(boolean)}, then the scrollpane will allow
  * the table to scroll right-left, and columns will be sized to their preferred
  * size. To control the preferred sizing of a column, you can provide a
  * prototype value for the column in the TableColumnExt using
  * {@link TableColumnExt#setPrototypeValue(Object)}. The prototype is used as
  * an indicator of the preferred size of the column. This can be useful if some
  * data in a given column is very long, but where the resize algorithm would
  * normally not pick this up.
  * 
  * <p>
  * Last, you can also provide searches on a JXTable using the Searchable property.
  * 
  * <p>
  * Keys/Actions registered with this component:
  * 
  * <ul>
  * <li> "find" - open an appropriate search widget for searching cell content. The
  *   default action registeres itself with the SearchFactory as search target.
  * <li> "print" - print the table
  * <li> {@link JXTable#HORIZONTAL_ACTION_COMMAND} - toggle the horizontal scrollbar
  * <li> {@link JXTable#PACKSELECTED_ACTION_COMMAND} - resize the selected column to fit the widest
  *  cell content 
  * <li> {@link JXTable#PACKALL_ACTION_COMMAND} - resize all columns to fit the widest
  *  cell content in each column
  * 
  * </ul>
  * 
  * <p>
  * Key bindings.
  * 
  * <ul>
  * <li> "control F" - bound to actionKey "find".
  * </ul>
  * 
  * <p>
  * Client Properties.
  * 
  * <ul>
  * <li> {@link JXTable#MATCH_HIGHLIGHTER} - set to Boolean.TRUE to 
  *  use a SearchHighlighter to mark a cell as matching.
  * </ul>
  * 
  * @author Ramesh Gupta
  * @author Amy Fowler
  * @author Mark Davidson
  * @author Jeanette Winzenburg
  */
 public class JXTable extends JTable { 
     public static final String EXECUTE_BUTTON_ACTIONCOMMAND = "executeButtonAction";
 
     /**
      * Constant string for horizontal scroll actions, used in JXTable's Action
      * Map.
      */
     public static final String HORIZONTALSCROLL_ACTION_COMMAND = 
         ColumnControlButton.COLUMN_CONTROL_MARKER + "horizontalScroll";
 
     /** Constant string for packing all columns, used in JXTable's Action Map. */
     public static final String PACKALL_ACTION_COMMAND = 
         ColumnControlButton.COLUMN_CONTROL_MARKER + "packAll";
 
     /**
      * Constant string for packing selected columns, used in JXTable's Action
      * Map.
      */
     public static final String PACKSELECTED_ACTION_COMMAND = 
         ColumnControlButton.COLUMN_CONTROL_MARKER + "packSelected";
 
     /** The prefix marker to find component related properties in the resourcebundle. */
     public static final String UIPREFIX = "JXTable.";
 
     /** key for client property to use SearchHighlighter as match marker. */
     public static final String MATCH_HIGHLIGHTER = AbstractSearchable.MATCH_HIGHLIGHTER;
 
     static {
         // Hack: make sure the resource bundle is loaded
         LookAndFeelAddons.getAddon();
     }
 
     /** The FilterPipeline for the table. */
     protected FilterPipeline filters;
 
     /** The HighlighterPipeline for the table. */
     protected HighlighterPipeline highlighters;
 
     /** The ComponentAdapter for model data access. */
     protected ComponentAdapter dataAdapter;
 
     /** The handler for mapping view/model coordinates of row selection. */
     private Selection selection;
 
     /** flag to indicate if table is interactively sortable. */
     private boolean sortable;
 
     /** future - enable/disable autosort on cell updates not used */
 //    private boolean automaticSortDisabled;
 
     /** Listens for changes from the filters. */
     private PipelineListener pipelineListener;
 
     /** Listens for changes from the highlighters. */
     private ChangeListener highlighterChangeListener;
 
     /** the factory to use for column creation and configuration. */
     private ColumnFactory columnFactory;
 
     /** The default number of visible rows (in a ScrollPane). */
     private int visibleRowCount = 18;
 
     private RowSizing rowSizing;
 
     private Field rowModelField;
 
     private boolean rowHeightEnabled;
 
     /**
      * flag to indicate if the column control is visible.
      */
     private boolean columnControlVisible;
     /**
      * ScrollPane's original vertical scroll policy. If the columnControl is
      * visible the policy is set to ALWAYS.
      */
     private int verticalScrollPolicy;
 
     /**
      * A button that allows the user to select which columns to display, and
      * which to hide
      */
     private JComponent columnControlButton;
 
     /**
      * Mouse/Motion/Listener keeping track of mouse moved in cell coordinates.
      */
     private RolloverProducer rolloverProducer;
 
     /**
      * RolloverController: listens to cell over events and repaints
      * entered/exited rows.
      */
     private LinkController linkController;
 
     /** field to store the autoResizeMode while interactively setting 
      *  horizontal scrollbar to visible.
      */
     private int oldAutoResizeMode;
 
     /** temporary hack: rowheight will be internally adjusted to font size 
      *  on instantiation and in updateUI if 
      *  the height has not been set explicitly by the application.
      */
     protected boolean isXTableRowHeightSet;
 
     protected Searchable searchable;
 
     /** Instantiates a JXTable with a default table model, no data. */
     public JXTable() {
         init();
     }
 
     /**
      * Instantiates a JXTable with a specific table model.
      * 
      * @param dm
      *            The model to use.
      */
     public JXTable(TableModel dm) {
         super(dm);
         init();
     }
 
     /**
      * Instantiates a JXTable with a specific table model.
      * 
      * @param dm
      *            The model to use.
      */
     public JXTable(TableModel dm, TableColumnModel cm) {
         super(dm, cm);
         init();
     }
 
     /**
      * Instantiates a JXTable with a specific table model, column model, and
      * selection model.
      * 
      * @param dm
      *            The table model to use.
      * @param cm
      *            The colomn model to use.
      * @param sm
      *            The list selection model to use.
      */
     public JXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
         super(dm, cm, sm);
         init();
     }
 
     /**
      * Instantiates a JXTable for a given number of columns and rows.
      * 
      * @param numRows
      *            Count of rows to accomodate.
      * @param numColumns
      *            Count of columns to accomodate.
      */
     public JXTable(int numRows, int numColumns) {
         super(numRows, numColumns);
         init();
     }
 
     /**
      * Instantiates a JXTable with data in a vector or rows and column names.
      * 
      * @param rowData
      *            Row data, as a Vector of Objects.
      * @param columnNames
      *            Column names, as a Vector of Strings.
      */
     public JXTable(Vector rowData, Vector columnNames) {
         super(rowData, columnNames);
         init();
     }
 
     /**
      * Instantiates a JXTable with data in a array or rows and column names.
      * 
      * @param rowData
      *            Row data, as a two-dimensional Array of Objects (by row, for
      *            column).
      * @param columnNames
      *            Column names, as a Array of Strings.
      */
     public JXTable(Object[][] rowData, Object[] columnNames) {
         super(rowData, columnNames);
         init();
     }
 
     /** Initializes the table for use. */
     protected void init() {
         setSortable(true);
         // guarantee getFilters() to return != null
         setFilters(null);
         initActionsAndBindings();
         // instantiate row height depending on font size
         updateRowHeightUI(false);
     }
 
     /**
      * Property to enable/disable rollover support. This can be enabled to show
      * "live" rollover behaviour, f.i. the cursor over LinkModel cells. Default
      * is disabled. If using a RolloverHighlighter on the table, this should be
      * set to true.
      * 
      * @param rolloverEnabled
      */
     public void setRolloverEnabled(boolean rolloverEnabled) {
         boolean old = isRolloverEnabled();
         if (rolloverEnabled == old)
             return;
         if (rolloverEnabled) {
             rolloverProducer = createRolloverProducer();
             addMouseListener(rolloverProducer);
             addMouseMotionListener(rolloverProducer);
             getLinkController().install(this);
 
         } else {
             removeMouseListener(rolloverProducer);
             removeMouseMotionListener(rolloverProducer);
             rolloverProducer = null;
             getLinkController().release();
         }
         firePropertyChange("rolloverEnabled", old, isRolloverEnabled());
     }
 
     protected LinkController getLinkController() {
         if (linkController == null) {
             linkController = createLinkController();
         }
         return linkController;
     }
 
     protected LinkController createLinkController() {
         return new LinkController();
     }
 
 
     /**
      * creates and returns the RolloverProducer to use.
      * 
      * @return
      */
     protected RolloverProducer createRolloverProducer() {
         RolloverProducer r = new RolloverProducer() {
             protected void updateRolloverPoint(JComponent component,
                     Point mousePoint) {
                 JXTable table = (JXTable) component;
                 int col = table.columnAtPoint(mousePoint);
                 int row = table.rowAtPoint(mousePoint);
                 if ((col < 0) || (row < 0)) {
                     row = -1;
                     col = -1;
                 }
                 rollover.x = col;
                 rollover.y = row;
             }
 
         };
         return r;
     }
 
     /**
      * Returns the rolloverEnabled property.
      * 
      * @return <code>true</code> if rollover is enabled
      */
     public boolean isRolloverEnabled() {
         return rolloverProducer != null;
     }
 
     /**
      * If the default editor for LinkModel.class is of type LinkRenderer enables
      * link visiting with the given linkVisitor. As a side-effect the rollover
      * property is set to true.
      * 
      * @param linkVisitor
      */
     public void setDefaultLinkVisitor(ActionListener linkVisitor) {
         TableCellEditor editor = getDefaultEditor(LinkModel.class);
         if (editor instanceof LinkRenderer) {
             ((LinkRenderer) editor).setVisitingDelegate(linkVisitor);
         }
         TableCellRenderer renderer = getDefaultRenderer(LinkModel.class);
         if (renderer instanceof LinkRenderer) {
             ((LinkRenderer) renderer).setVisitingDelegate(linkVisitor);
         }
         setRolloverEnabled(true);
     }
 
     /**
      * listens to rollover properties. 
      * Repaints effected component regions.
      * Updates link cursor.
      * 
      * @author Jeanette Winzenburg
      */
     public static class LinkController implements PropertyChangeListener {
 
         private Cursor oldCursor;
         private JXTable table;
 
         public void propertyChange(PropertyChangeEvent evt) {
             if (RolloverProducer.ROLLOVER_KEY.equals(evt.getPropertyName())) {
                rollover((JXTable) evt.getSource(), (Point) evt
                             .getOldValue(), (Point) evt.getNewValue());
             } 
             else if (RolloverProducer.CLICKED_KEY.equals(evt.getPropertyName())) {
                 click((JXTable) evt.getSource(), (Point) evt.getOldValue(),
                         (Point) evt.getNewValue());
             }
         }
 
         public void install(JXTable table) {
           release();  
           this.table = table;
           table.addPropertyChangeListener(this);
           registerExecuteButtonAction();
         }
         
         public void release() {
             if (table == null) return;
             table.removePropertyChangeListener(this);
             unregisterExecuteButtonAction();
         }
 
 //    --------------------------- JTable rollover
         
         private void rollover(JXTable table, Point oldLocation, Point newLocation) {
             if (oldLocation != null) {
                 Rectangle r = table.getCellRect(oldLocation.y, oldLocation.x, false);
                 r.x = 0;
                 r.width = table.getWidth();
                 table.repaint(r);
             }
             if (newLocation != null) {
                 Rectangle r = table.getCellRect(newLocation.y, newLocation.x, false);
                 r.x = 0;
                 r.width = table.getWidth();
                 table.repaint(r);
             }
             setLinkCursor(table, newLocation);
         }
 
         private void click(JXTable list, Point oldLocation, Point newLocation) {
             if (!isLinkColumn(list, newLocation)) return;
             if (list.isCellEditable(newLocation.y, newLocation.x)) return;
             TableCellRenderer renderer = list.getCellRenderer(newLocation.y, newLocation.x);
             Component comp = list.prepareRenderer(renderer, newLocation.y,  newLocation.x);
             if (comp instanceof AbstractButton) {
                 ((AbstractButton) comp).doClick();
                 list.repaint();
             }
         }
 
         private void setLinkCursor(JXTable table, Point location) {
             if (isLinkColumn(table, location)) {
                 if (oldCursor == null) {
                     oldCursor = table.getCursor();
                     table.setCursor(Cursor
                             .getPredefinedCursor(Cursor.HAND_CURSOR));
                 }
             } else {
                 if (oldCursor != null) {
                     table.setCursor(oldCursor);
                     oldCursor = null;
                 }
             }
 
         }
         private boolean isLinkColumn(JXTable table, Point location) {
             if (location == null || location.x < 0) return false;
             return (table.getColumnClass(location.x) == LinkModel.class);
         }
 
 
         private void unregisterExecuteButtonAction() {
             table.getActionMap().put(EXECUTE_BUTTON_ACTIONCOMMAND, null);
             KeyStroke space = KeyStroke.getKeyStroke("released SPACE");
             table.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(space , null);
         }
 
         private void registerExecuteButtonAction() {
             table.getActionMap().put(EXECUTE_BUTTON_ACTIONCOMMAND, createExecuteButtonAction());
             KeyStroke space = KeyStroke.getKeyStroke("released SPACE");
             table.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(space , EXECUTE_BUTTON_ACTIONCOMMAND);
             
         }
 
         private Action createExecuteButtonAction() {
             Action action = new AbstractAction() {
 
                 public void actionPerformed(ActionEvent e) {
                     AbstractButton button = getClickableRendererComponent();
                     if (button != null) {
                         button.doClick();
                         table.repaint();
                     }
                 }
 
                 @Override
                 public boolean isEnabled() {
                     return isClickable();
                 }
 
                 private boolean isClickable() {
                     return getClickableRendererComponent() != null;
                 }
                 
                 private AbstractButton getClickableRendererComponent() {
                     if (table == null || !table.isEnabled() || !table.hasFocus()) return null;
                     int leadRow = table.getSelectionModel().getLeadSelectionIndex();
                     int leadColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
                     if (leadRow < 0 || leadColumn < 0 || table.isCellEditable(leadRow, leadColumn)) return null;
                     TableCellRenderer renderer = table.getCellRenderer(leadRow, leadColumn);
                     Component rendererComp = table.prepareRenderer(renderer, leadRow, leadColumn);
                     return rendererComp instanceof AbstractButton ? (AbstractButton) rendererComp : null;
                 }
                 
             };
             return action;
         }
 
     }
 
     
 //--------------------------------- ColumnControl
     
     /**
      * overridden to addionally configure the upper right corner of an enclosing
      * scrollpane with the ColumnControl.
      */
     protected void configureEnclosingScrollPane() {
         super.configureEnclosingScrollPane();
         configureColumnControl();
         configureViewportBackground();
     }
 
     /**
      * set's the viewports background to this.background.<p> 
      * 
      * PENDING: need to
      * repeat on background changes to this!
      * 
      */
     protected void configureViewportBackground() {
         Container p = getParent();
         if (p instanceof JViewport) {
             p.setBackground(getBackground());
         }
     }
 
     /**
      * configure the upper right corner of an enclosing scrollpane with/o the
      * ColumnControl, depending on setting of columnControl visibility flag.<p>
      * 
      * PENDING: should choose corner depending on component orientation.
      */
     private void configureColumnControl() {
         Container p = getParent();
         if (p instanceof JViewport) {
             Container gp = p.getParent();
             if (gp instanceof JScrollPane) {
                 JScrollPane scrollPane = (JScrollPane) gp;
                 // Make certain we are the viewPort's view and not, for
                 // example, the rowHeaderView of the scrollPane -
                 // an implementor of fixed columns might do this.
                 JViewport viewport = scrollPane.getViewport();
                 if (viewport == null || viewport.getView() != this) {
                     return;
                 }
                 if (isColumnControlVisible()) {
                     verticalScrollPolicy = scrollPane
                             .getVerticalScrollBarPolicy();
                     scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER,
                             getColumnControl());
 
                     scrollPane
                             .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                 } else {
                     if (verticalScrollPolicy != 0) {
                         // Fix #155-swingx: reset only if we had force always before
                         // PENDING: JW - doesn't cope with dynamically changing the policy
                         // shouldn't be much of a problem because doesn't happen too often?? 
                         scrollPane.setVerticalScrollBarPolicy(verticalScrollPolicy);
                     }
                     try {
                         scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER,
                                 null);
                     } catch (Exception ex) {
                         // Ignore spurious exception thrown by JScrollPane. This
                         // is a Swing bug!
                     }
 
                 }
             }
         }
     }
 
     /**
      * Hack around core swing JScrollPane bug: can't cope with
      * corners when changing component orientation at runtime.
      * overridden to re-configure the columnControl.
      */
     @Override
     public void setComponentOrientation(ComponentOrientation o) {
         super.setComponentOrientation(o);
         configureColumnControl();
     }
 
     /**
      * returns visibility flag of column control.
      * <p>
      * 
      * Note: if the table is not inside a JScrollPane the column control is not
      * shown even if this returns true. In this case it's the responsibility of
      * the client code to actually show it.
      * 
      * @return
      */
     public boolean isColumnControlVisible() {
         return columnControlVisible;
     }
 
     /**
      * returns the component for column control.
      * 
      * @return
      */
     public JComponent getColumnControl() {
         if (columnControlButton == null) {
             columnControlButton = new ColumnControlButton(this,
                     new ColumnControlIcon());
         }
         return columnControlButton;
     }
 
     /**
      * bound property to flag visibility state of column control.
      * 
      * @param showColumnControl
      */
     public void setColumnControlVisible(boolean showColumnControl) {
         boolean old = columnControlVisible;
         this.columnControlVisible = showColumnControl;
         configureColumnControl();
         firePropertyChange("columnControlVisible", old, columnControlVisible);
     }
 
     
 //--------------------- actions
     
     /**
      * A small class which dispatches actions. TODO: Is there a way that we can
      * make this static? JW: I hate those if constructs... we are in OO-land!
      */
     private class Actions extends UIAction {
         Actions(String name) {
             super(name);
         }
 
         public void actionPerformed(ActionEvent evt) {
             if ("print".equals(getName())) {
                 try {
                     print();
                 } catch (PrinterException ex) {
                     // REMIND(aim): should invoke pluggable application error
                     // handler
                     ex.printStackTrace();
                 }
             } else if ("find".equals(getName())) {
                 find();
             }
         }
 
     }
 
 
     private void initActionsAndBindings() {
         // Register the actions that this class can handle.
         ActionMap map = getActionMap();
         map.put("print", new Actions("print"));
         map.put("find", new Actions("find"));
         map.put(PACKALL_ACTION_COMMAND, createPackAllAction());
         map.put(PACKSELECTED_ACTION_COMMAND, createPackSelectedAction());
         map.put(HORIZONTALSCROLL_ACTION_COMMAND, createHorizontalScrollAction());
         // JW: this should be handled by the LF!
         KeyStroke findStroke = KeyStroke.getKeyStroke("control F");
         getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(findStroke, "find");
     }
 
     /** Creates an Action for horizontal scrolling. */
     private Action createHorizontalScrollAction() {
         String actionName = getUIString(HORIZONTALSCROLL_ACTION_COMMAND);
         BoundAction action = new BoundAction(actionName,
                 HORIZONTALSCROLL_ACTION_COMMAND);
         action.setStateAction();
         action.registerCallback(this, "setHorizontalScrollEnabled");
         action.setSelected(isHorizontalScrollEnabled());
         return action;
     }
 
     private String getUIString(String key) {
         String text = UIManager.getString(UIPREFIX + key);
         return text != null ? text : key;
     }
 
     /** Creates an Action for packing selected columns. */
     private Action createPackSelectedAction() {
         String text = getUIString(PACKSELECTED_ACTION_COMMAND);
         BoundAction action = new BoundAction(text, PACKSELECTED_ACTION_COMMAND);
         action.registerCallback(this, "packSelected");
         action.setEnabled(getSelectedColumnCount() > 0);
         return action;
     }
 
     /** Creates an Action for packing all columns. */
     private Action createPackAllAction() {
         String text = getUIString(PACKALL_ACTION_COMMAND);
         BoundAction action = new BoundAction(text, PACKALL_ACTION_COMMAND);
         action.registerCallback(this, "packAll");
         return action;
     }
 
     
 //------------------ bound action callback methods
     
     /**
      * This resizes all columns to fit the viewport; if horizontal scrolling is
      * enabled, all columns will get their preferred width. This can be
      * triggered by the "packAll" BoundAction on the table as well.
      */
     public void packAll() {
         packTable(getDefaultPackMargin());
     }
 
     /**
      * This resizes selected columns to fit the viewport; if horizontal
      * scrolling is enabled, selected columns will get their preferred width.
      * This can be triggered by the "packSelected" BoundAction on the table as
      * well.
      */
     public void packSelected() {
         int selected = getSelectedColumn();
         if (selected >= 0) {
             packColumn(selected, getDefaultPackMargin());
         }
     }
 
     /**
      * Controls horizontal scrolling in the viewport, and works in coordination
      * with column sizing.
      * 
      * @param enabled
      *            If true, the scrollpane will allow the table to scroll
      *            horizontally, and columns will resize to their preferred
      *            width. If false, columns will resize to fit the viewport.
      */
     public void setHorizontalScrollEnabled(boolean enabled) {
         if (enabled == (isHorizontalScrollEnabled()))
             return;
         if (enabled) {
             oldAutoResizeMode = getAutoResizeMode();
             setAutoResizeMode(AUTO_RESIZE_OFF);
         } else {
             setAutoResizeMode(oldAutoResizeMode);
         }
     }
 
     /** Returns the current setting for horizontal scrolling. */
     private boolean isHorizontalScrollEnabled() {
         return getAutoResizeMode() == AUTO_RESIZE_OFF;
     }
 
     /** Returns the default margin for packing columns. */
     private int getDefaultPackMargin() {
         return 4;
     }
 
     /** Notifies the table that a new column has been selected. 
      *  overridden to update the enabled state of the packSelected
      *  action.
      */
     public void columnSelectionChanged(ListSelectionEvent e) {
         super.columnSelectionChanged(e);
         if (e.getValueIsAdjusting())
             return;
         Action packSelected = getActionMap().get(PACKSELECTED_ACTION_COMMAND);
         if ((packSelected != null)) {
             packSelected.setEnabled(!((ListSelectionModel) e.getSource())
                     .isSelectionEmpty());
         }
     }
 
     /** 
      * overridden to update the show horizontal scrollbar action's
      * selected state. 
      */
     public void setAutoResizeMode(int mode) {
         super.setAutoResizeMode(mode);
         Action showHorizontal = getActionMap().get(
                 HORIZONTALSCROLL_ACTION_COMMAND);
         if (showHorizontal instanceof BoundAction) {
             ((BoundAction) showHorizontal)
                     .setSelected(isHorizontalScrollEnabled());
         }
     }
 
 
 //------------------------ override super because of filter-awareness
     
     /**
      * Returns the row count in the table; if filters are applied, this is the
      * filtered row count.
      */
     @Override public int getRowCount() {
         // RG: If there are no filters, call superclass version rather than
         // accessing model directly
         return filters == null ?
                 super.getRowCount() : filters.getOutputSize();
     }
 
     public boolean isHierarchical(int column) {
         return false;
     }
 
     /**
      * Convert row index from view coordinates to model coordinates accounting
      * for the presence of sorters and filters.
      * 
      * @param row
      *            row index in view coordinates
      * @return row index in model coordinates
      */
     public int convertRowIndexToModel(int row) {
         return getFilters().convertRowIndexToModel(row);
     }
 
     /**
      * Convert row index from model coordinates to view coordinates accounting
      * for the presence of sorters and filters.
      * 
      * @param row
      *            row index in model coordinates
      * @return row index in view coordinates
      */
     public int convertRowIndexToView(int row) {
         return getFilters().convertRowIndexToView(row);
     }
 
     /**
      * {@inheritDoc}
      */
     public Object getValueAt(int row, int column) {
         return getModel().getValueAt(convertRowIndexToModel(row), 
                 convertColumnIndexToModel(column));
     }
 
     /**
      * {@inheritDoc}
      */
     public void setValueAt(Object aValue, int row, int column) {
         getModel().setValueAt(aValue, convertRowIndexToModel(row),
                 convertColumnIndexToModel(column));
     }
 
     /**
      * {@inheritDoc}
      */
     public boolean isCellEditable(int row, int column) {
         return getModel().isCellEditable(convertRowIndexToModel(row),
                 convertColumnIndexToModel(column));
     }
 
     /**
      * {@inheritDoc}
      */
     public void setModel(TableModel newModel) {
         // JW: need to look here? is done in tableChanged as well. 
         getSelection().lock();
         super.setModel(newModel);
     }
 
     /** 
      * additionally updates filtered state.
      * {@inheritDoc}
      */
     public void tableChanged(TableModelEvent e) {
         if (getSelectionModel().getValueIsAdjusting()) {
             // this may happen if the uidelegate/editor changed selection
             // and adjusting state
             // before firing a editingStopped
             // need to enforce update of model selection
             getSelectionModel().setValueIsAdjusting(false);
         }
         // JW: make Selection deaf ... super doesn't know about row
         // mapping and sets rowSelection in model coordinates
         // causing complete confusion.
         getSelection().lock();
         super.tableChanged(e);
         updateSelectionAndRowModel(e);
         use(filters);
     }
 
     /**
      * reset model selection coordinates in Selection after
      * model events.
      * 
      * @param e
      */
     private void updateSelectionAndRowModel(TableModelEvent e) {
         // JW: c&p from JTable
         // JW: still missing: checkLeadAnchor (#172-swingx)
         // super checkLeadAnchor is subtly buggy in lead/anchor update
         // because it calls model.getRowCount() instead of getRowCount!!
         if (e.getType() == TableModelEvent.INSERT) {
             int start = e.getFirstRow();
             int end = e.getLastRow();
             if (start < 0) {
                 start = 0;
             }
             if (end < 0) {
                 end = getModel().getRowCount() - 1;
             }
 
             // Adjust the selection to account for the new rows.
             int length = end - start + 1;
             getSelection().insertIndexInterval(start, length, true);
             getRowSizing().insertIndexInterval(start, length, getRowHeight());
 
         } else if (e.getType() == TableModelEvent.DELETE) {
             int start = e.getFirstRow();
             int end = e.getLastRow();
             if (start < 0) {
                 start = 0;
             }
             if (end < 0) {
                 end = getModel().getRowCount() - 1;
             }
 
             int deletedCount = end - start + 1;
             int previousRowCount = getModel().getRowCount() + deletedCount;
             // Adjust the selection to account for the new rows
             getSelection().removeIndexInterval(start, end);
             getRowSizing().removeIndexInterval(start, deletedCount);
 
         } else if (getSelectionModel().isSelectionEmpty()) {
             // JW: this is incomplete! see #167-swingx
             // possibly got a dataChanged or structureChanged
             // super will have cleared selection
             getSelection().clearModelSelection();
             getRowSizing().clearModelSizes();
             updateViewSizeSequence();
              
         }
 
     }
 
     /**
      * Called if individual row height mapping need to be updated.
      * This implementation guards against unnessary access of 
      * super's private rowModel field.
      */
     protected void updateViewSizeSequence() {
         SizeSequence sizeSequence = null;
         if (isRowHeightEnabled()) {
             sizeSequence = getSuperRowModel();
         }
         getRowSizing().setViewSizeSequence(sizeSequence, getRowHeight());
     }
     
     private Selection getSelection() {
         if (selection == null) {
             selection = new Selection(filters, getSelectionModel());
         }
         return selection;
     }
 
 
 //----------------------------- filters
     
     /** Returns the FilterPipeline for the table. */
     public FilterPipeline getFilters() {
         // PENDING: this is guaranteed to be != null because
         // init calls setFilters(null) which enforces an empty
         // pipeline
         return filters;
     }
 
     /**
      * setModel() and setFilters() may be called in either order.
      * 
      * @param pipeline
      */
     private void use(FilterPipeline pipeline) {
         if (pipeline != null) {
             // check JW: adding listener multiple times (after setModel)?
             if (initialUse(pipeline)) {
                 pipeline.addPipelineListener(getFilterPipelineListener());
                 pipeline.assign(getComponentAdapter());
             } else {
                 pipeline.flush();
             }
         }
     }
 
     /**
      * @return true is not yet used in this JXTable, false otherwise
      */
     private boolean initialUse(FilterPipeline pipeline) {
         if (pipelineListener == null) return true;
         PipelineListener[] l = pipeline.getPipelineListeners();
         for (int i = 0; i < l.length; i++) {
             if (pipelineListener.equals(l[i]))
                 return false;
         }
         return true;
     }
 
     /** Sets the FilterPipeline for filtering table rows. */
     public void setFilters(FilterPipeline pipeline) {
         FilterPipeline old = getFilters();
         Sorter sorter = null;
         if (old != null) {
             old.removePipelineListener(pipelineListener);
             sorter = old.getSorter();
         }
         if (pipeline == null) {
             pipeline = new FilterPipeline();
         }
         filters = pipeline;
         filters.setSorter(sorter);
         // JW: first assign to prevent (short?) illegal internal state
         // #173-swingx
         use(filters);
         getRowSizing().setFilters(filters);
         getSelection().setFilters(filters);
     }
 
 
     /** returns the listener for changes in filters. */
     protected PipelineListener getFilterPipelineListener() {
         if (pipelineListener == null) {
             pipelineListener = createPipelineListener();
         }
         return pipelineListener;
     }
 
     /** creates the listener for changes in filters. */
     protected PipelineListener createPipelineListener() {
         PipelineListener l = new PipelineListener() {
             public void contentsChanged(PipelineEvent e) {
                 updateOnFilterContentChanged();
             }
         };
         return l;
     }
 
 
     /** 
      * method called on change notification from filterpipeline.
      */
     protected void updateOnFilterContentChanged() {
         revalidate();
         repaint();
     }
 
 
 //-------------------------------- sorting 
 
     /**
      * Sets &quot;sortable&quot; property indicating whether or not this table
      * supports sortable columns. If <code>sortable</code> is
      * <code>true</code> then sorting will be enabled on all columns whose
      * <code>sortable</code> property is <code>true</code>. If
      * <code>sortable</code> is <code>false</code> then sorting will be
      * disabled for all columns, regardless of each column's individual
      * <code>sorting</code> property. The default is <code>true</code>.
      * 
      * @see TableColumnExt#isSortable()
      * @param sortable
      *            boolean indicating whether or not this table supports sortable
      *            columns
      */
     public void setSortable(boolean sortable) {
         if (sortable == isSortable())
             return;
         this.sortable = sortable;
         if (!isSortable()) resetSorter();
         firePropertyChange("sortable", !sortable, sortable);
     }
 
     /** Returns true if the table is sortable. */
     public boolean isSortable() {
         return sortable;
     }
 
 
     private void setInteractiveSorter(Sorter sorter) {
         // this check is for the sake of the very first call after instantiation
         if (filters == null)
             return;
         getFilters().setSorter(sorter);
 
     }
 
     private Sorter getInteractiveSorter() {
         // this check is for the sake of the very first call after instantiation
         if (filters == null)
             return null;
         return getFilters().getSorter();
     }
 
     /**
      * Removes the interactive sorter.
      * Used by headerListener.
      * 
      */
     protected void resetSorter() {
         // JW PENDING: think about notification instead of manual repaint.
         setInteractiveSorter(null);
         getTableHeader().repaint();
     }
 
     public void columnRemoved(TableColumnModelEvent e) {
         // JW - old problem: need access to removed column
         // to get hold of removed modelIndex
         // to remove interactive sorter if any
         // no way
         // int modelIndex = convertColumnIndexToModel(e.getFromIndex());
         updateSorterAfterColumnRemoved();
         super.columnRemoved(e);
     }
 
     /**
      * guarantee that the interactive sorter is removed if its column
      * is removed.
      * 
      */
     private void updateSorterAfterColumnRemoved() {
         // bloody hack: get sorter and check if there's a column with it
         // available
         Sorter sorter = getInteractiveSorter();
         if (sorter != null) {
             int sorterColumn = sorter.getColumnIndex();
             List columns = getColumns(true);
             for (Iterator iter = columns.iterator(); iter.hasNext();) {
                 TableColumn column = (TableColumn) iter.next();
                 if (column.getModelIndex() == sorterColumn)
                     return;
             }
             // didn't find a column with the sorter's index - remove
             resetSorter();
         }
     }
 
     /**
      * 
      * request to sort the column at columnIndex in view coordinates. if there
      * is already an interactive sorter for this column it's sort order is
      * reversed. Otherwise the columns sorter is used as is.
      * Used by headerListener.
      * 
      */
     protected void setSorter(int columnIndex) {
         if (!isSortable())
             return;
         Sorter sorter = getInteractiveSorter();
 
         if ((sorter != null)
             && (sorter.getColumnIndex() == convertColumnIndexToModel(columnIndex))) {
             sorter.toggle();
         } else {
             TableColumnExt column = getColumnExt(columnIndex);
             getFilters().setSorter(column != null ? column.getSorter() : null);
         }
     }
 
     /**
      * Returns the interactive sorter if it is set from the given column.
      * Used by ColumnHeaderRenderer.getTableCellRendererComponent().
      * 
      * @param columnIndex the column index in view coordinates.
      * @return the interactive sorter if matches the column or null.
      */
     public Sorter getSorter(int columnIndex) {
         Sorter sorter = getInteractiveSorter();
 
         return sorter == null ? null
                 : sorter.getColumnIndex() == convertColumnIndexToModel(columnIndex) ? sorter
                         : null;
     }
 
     
 //---------------------- enhanced TableColumn/Model support    
     /**
      * Remove all columns, make sure to include hidden.
      * 
      */
     protected void removeColumns() {
         /**
          * @todo promote this method to superclass, and change
          *       createDefaultColumnsFromModel() to call this method
          */
         List columns = getColumns(true);
         for (Iterator iter = columns.iterator(); iter.hasNext();) {
             getColumnModel().removeColumn((TableColumn) iter.next());
 
         }
     }
 
     /**
      * returns a list of all visible TableColumns.
      * 
      * @return
      */
     public List getColumns() {
         return Collections.list(getColumnModel().getColumns());
     }
 
     /**
      * returns a list of TableColumns including hidden if the parameter is set
      * to true.
      * 
      * @param includeHidden
      * @return
      */
     public List getColumns(boolean includeHidden) {
         if (includeHidden && (getColumnModel() instanceof TableColumnModelExt)) {
             return ((TableColumnModelExt) getColumnModel())
                     .getColumns(includeHidden);
         }
         return getColumns();
     }
 
     /**
      * returns the number of TableColumns including hidden if the parameter is set 
      * to true.
      * 
      * @param includeHidden
      * @return
      */
     public int getColumnCount(boolean includeHidden) {
         if (getColumnModel() instanceof TableColumnModelExt) {
             return ((TableColumnModelExt) getColumnModel())
                     .getColumnCount(includeHidden);
         }
         return getColumnCount();
     }
 
     /**
      * reorders the columns in the sequence given array. Logical names that do
      * not correspond to any column in the model will be ignored. Columns with
      * logical names not contained are added at the end.
      * 
      * @param columnNames
      *            array of logical column names
      */
     public void setColumnSequence(Object[] identifiers) {
         List columns = getColumns(true);
         Map map = new HashMap();
         for (Iterator iter = columns.iterator(); iter.hasNext();) {
             // PENDING: handle duplicate identifiers ...
             TableColumn column = (TableColumn) iter.next();
             map.put(column.getIdentifier(), column);
             getColumnModel().removeColumn(column);
         }
         for (int i = 0; i < identifiers.length; i++) {
             TableColumn column = (TableColumn) map.get(identifiers[i]);
             if (column != null) {
                 getColumnModel().addColumn(column);
                 columns.remove(column);
             }
         }
         for (Iterator iter = columns.iterator(); iter.hasNext();) {
             TableColumn column = (TableColumn) iter.next();
             getColumnModel().addColumn(column);
         }
     }
 
     /**
      * Returns the <code>TableColumnExt</code> object for the column in the
      * table whose identifier is equal to <code>identifier</code>, when
      * compared using <code>equals</code>. The returned TableColumn is
      * guaranteed to be part of the current ColumnModel but may be hidden, that
      * is
      * 
      * <pre> <code>
      * TableColumnExt column = table.getColumnExt(id);
      * if (column != null) {
      *     int viewIndex = table.convertColumnIndexToView(column.getModelIndex());
      *     assertEquals(column.isVisible(), viewIndex &gt;= 0);
      * }
      * </code> </pre>
      * 
      * @param identifier
      *            the identifier object
      * 
      * @return the <code>TableColumnExt</code> object that matches the
      *         identifier or null if none is found.
      */
     public TableColumnExt getColumnExt(Object identifier) {
         if (getColumnModel() instanceof TableColumnModelExt) {
             return ((TableColumnModelExt) getColumnModel())
                     .getColumnExt(identifier);
         } else {
             // PENDING: not tested!
             try {
                 TableColumn column = getColumn(identifier);
                 if (column instanceof TableColumnExt) {
                     return (TableColumnExt) column;
                 }
             } catch (Exception e) {
                 // TODO: handle exception
             }
         }
         return null;
     }
 
     /**
      * Returns the <code>TableColumnExt</code> object for the column in the
      * table whose column index is equal to <code>viewColumnIndex</code>
      * 
      * @param viewColumnIndex
      *            index of the column with the object in question
      * 
      * @return the <code>TableColumnExt</code> object that matches the column
      *         index
      * @exception IllegalArgumentException
      *                if no <code>TableColumn</code> has this identifier
      */
     public TableColumnExt getColumnExt(int viewColumnIndex) {
         return (TableColumnExt) getColumnModel().getColumn(viewColumnIndex);
     }
 
     public void createDefaultColumnsFromModel() {
         TableModel model = getModel();
         if (model != null) {
             // Create new columns from the data model info
             // Note: it's critical to create the new columns before
             // deleting the old ones. Why?
             // JW PENDING: the reason is somewhere in the early forums - search!
             int modelColumnCount = model.getColumnCount();
             TableColumn newColumns[] = new TableColumn[modelColumnCount];
             for (int i = 0; i < newColumns.length; i++) {
                 newColumns[i] = createAndConfigureColumn(model, i);
             }
             // Remove any current columns
             removeColumns();
             // Now add the new columns to the column model
             for (int i = 0; i < newColumns.length; i++) {
                 addColumn(newColumns[i]);
             }
         }
     }
 
 
     protected TableColumn createAndConfigureColumn(TableModel model,
             int modelColumn) {
         return getColumnFactory().createAndConfigureTableColumn(model,
                 modelColumn);
     }
 
     protected ColumnFactory getColumnFactory() {
         if (columnFactory == null) {
             columnFactory = ColumnFactory.getInstance();
         }
         return columnFactory;
     }
 
 
 
     
 //----------------------- delegating methods?? from super    
     /**
      * Returns the margin between columns.
      * 
      * @return the margin between columns
      */
     public int getColumnMargin() {
         return getColumnModel().getColumnMargin();
     }
 
     /**
      * Sets the margin between columns.
      * 
      * @param value
      *            margin between columns; must be greater than or equal to zero.
      */
     public void setColumnMargin(int value) {
         getColumnModel().setColumnMargin(value);
     }
 
     /**
      * Returns the selection mode used by this table's selection model.
      * 
      * @return the selection mode used by this table's selection model
      */
     public int getSelectionMode() {
         return getSelectionModel().getSelectionMode();
     }
 
 //----------------------- Search support 
 
 
     /** Opens the find widget for the table. */
     private void find() {
         SearchFactory.getInstance().showFindInput(this, getSearchable());
     }
 
     /**
      * 
      * @returns a not-null Searchable for this editor.  
      */
     public Searchable getSearchable() {
         if (searchable == null) {
             searchable = new TableSearchable();
         }
         return searchable;
     }
 
     /**
      * sets the Searchable for this editor. If null, a default 
      * searchable will be used.
      * 
      * @param searchable
      */
     public void setSearchable(Searchable searchable) {
         this.searchable = searchable;
     }
 
     public class TableSearchable extends AbstractSearchable {
 
         private SearchHighlighter searchHighlighter;
         
 
         @Override
         protected void findMatchAndUpdateState(Pattern pattern, int startRow,
                 boolean backwards) {
             SearchResult matchRow = null;
             if (backwards) {
                 // CHECK: off-one end still needed?
                 // Probably not - the findXX don't have side-effects any longer
                 // hmmm... still needed: even without side-effects we need to
                 // guarantee calling the notfound update at the very end of the
                 // loop.
                 for (int r = startRow; r >= -1 && matchRow == null; r--) {
                     matchRow = findMatchBackwardsInRow(pattern, r);
                     updateState(matchRow);
                 }
             } else {
                 for (int r = startRow; r <= getSize() && matchRow == null; r++) {
                     matchRow = findMatchForwardInRow(pattern, r);
                     updateState(matchRow);
                 }
             }
            // KEEP - JW: Needed to update if loop wasn't entered!
             // the alternative is to go one off in the loop. Hmm - which is
             // preferable?
             // updateState(matchRow);
 
         }
 
         /**
          * called if sameRowIndex && !hasEqualRegEx. Matches the cell at
          * row/lastFoundColumn against the pattern. PRE: lastFoundColumn valid.
          * 
          * @param pattern
          * @param row
          * @return
          */
         protected SearchResult findExtendedMatch(Pattern pattern, int row) {
             return findMatchAt(pattern, row, lastSearchResult.foundColumn);
         }
 
         /**
          * Searches forward through columns of the given row. Starts at
          * lastFoundColumn or first column if lastFoundColumn < 0. returns an
          * appropriate SearchResult if a matching cell is found in this row or
          * null if no match is found. A row index out off range results in a
          * no-match.
          * 
          * @param pattern
          * @param row
          *            the row to search
          * @return
          */
         private SearchResult findMatchForwardInRow(Pattern pattern, int row) {
             int startColumn = (lastSearchResult.foundColumn < 0) ? 0 : lastSearchResult.foundColumn;
             if (isValidIndex(row)) {
                 for (int column = startColumn; column < getColumnCount(); column++) {
                     SearchResult result = findMatchAt(pattern, row, column);
                     if (result != null)
                         return result;
                 }
             }
             return null;
         }
 
         /**
          * Searches forward through columns of the given row. Starts at
          * lastFoundColumn or first column if lastFoundColumn < 0. returns an
          * appropriate SearchResult if a matching cell is found in this row or
          * null if no match is found. A row index out off range results in a
          * no-match.
          * 
          * @param pattern
          * @param row
          *            the row to search
          * @return
          */
         private SearchResult findMatchBackwardsInRow(Pattern pattern, int row) {
             int startColumn = (lastSearchResult.foundColumn < 0) ? getColumnCount() - 1
                     : lastSearchResult.foundColumn;
             if (isValidIndex(row)) {
                 for (int column = startColumn; column >= 0; column--) {
                     SearchResult result = findMatchAt(pattern, row, column);
                     if (result != null)
                         return result;
                 }
             }
             return null;
         }
 
         /**
          * Matches the cell content at row/col against the given Pattern.
          * Returns an appropriate SearchResult if matching or null if no
          * matching
          * 
          * @param pattern
          * @param row
          *            a valid row index in view coordinates
          * @param column
          *            a valid column index in view coordinates
          * @return
          */
         protected SearchResult findMatchAt(Pattern pattern, int row, int column) {
             Object value = getValueAt(row, column);
             if (value != null) {
                 Matcher matcher = pattern.matcher(value.toString());
                 if (matcher.find()) {
                     return createSearchResult(matcher, row, column);
                 }
             }
             return null;
         }
 
         /**
          * Called if startIndex is different from last search, reset the column
          * to -1 and make sure a backwards/forwards search starts at last/first
          * row, respectively.
          * 
          * @param startIndex
          * @param backwards
          * @return
          */
         protected int adjustStartPosition(int startIndex, boolean backwards) {
             lastSearchResult.foundColumn = -1;
             return super.adjustStartPosition(startIndex, backwards);
         }
 
         /**
          * Moves the internal start for matching as appropriate and returns the
          * new startIndex to use. Called if search was messaged with the same
          * startIndex as previously.
          * 
          * @param startRow
          * @param backwards
          * @return
          */
         @Override
         protected int moveStartPosition(int startRow, boolean backwards) {
             if (backwards) {
                 lastSearchResult.foundColumn--;
                 if (lastSearchResult.foundColumn < 0) {
                     startRow--;
                 }
             } else {
                 lastSearchResult.foundColumn++;
                 if (lastSearchResult.foundColumn >= getColumnCount()) {
                     lastSearchResult.foundColumn = -1;
                     startRow++;
                 }
             }
             return startRow;
         }
 
         /**
          * Checks if the startIndex is a candidate for trying a re-match.
          * 
          * 
          * @param startIndex
          * @return true if the startIndex should be re-matched, false if not.
          */
         protected boolean isEqualStartIndex(final int startIndex) {
             return super.isEqualStartIndex(startIndex)
                     && isValidColumn(lastSearchResult.foundColumn);
         }
 
         /**
          * checks if row is in range: 0 <= row < getRowCount().
          * 
          * @param column
          * @return
          */
         private boolean isValidColumn(int column) {
             return column >= 0 && column < getColumnCount();
         }
 
 
         @Override
         protected int getSize() {
             return getRowCount();
         }
 
         @Override
         protected void moveMatchMarker() {
             int row = lastSearchResult.foundRow;
             int column = lastSearchResult.foundColumn;
             Pattern pattern = lastSearchResult.pattern;
            if ((row < 0) || (column < 0)) {
                if (markByHighlighter()) {
                    getSearchHighlighter().setPattern(null);
                }
                return;
            }
             if (markByHighlighter()) {
                 Rectangle cellRect = getCellRect(row, column, true);
                 if (cellRect != null) {
                     scrollRectToVisible(cellRect);
                 }
                 ensureInsertedSearchHighlighters();
                 // TODO (JW) - cleanup SearchHighlighter state management
                getSearchHighlighter().setPattern(pattern);
                int modelColumn = convertColumnIndexToModel(column);
                getSearchHighlighter().setHighlightCell(row, modelColumn);
             } else { // use selection
                 changeSelection(row, column, false, false);
                 if (!getAutoscrolls()) {
                     // scrolling not handled by moving selection
                     Rectangle cellRect = getCellRect(row, column, true);
                     if (cellRect != null) {
                         scrollRectToVisible(cellRect);
                     }
                 }
             }
         }
 
         private boolean markByHighlighter() {
             return Boolean.TRUE.equals(getClientProperty(MATCH_HIGHLIGHTER));
         }
 
         private SearchHighlighter getSearchHighlighter() {
             if (searchHighlighter == null) {
                 searchHighlighter = createSearchHighlighter();
             }
             return searchHighlighter;
         }
 
         private void ensureInsertedSearchHighlighters() {
             if (getHighlighters() == null) {
                 setHighlighters(new HighlighterPipeline(
                         new Highlighter[] { getSearchHighlighter() }));
             } else if (!isInPipeline(getSearchHighlighter())) {
                 getHighlighters().addHighlighter(getSearchHighlighter());
             }
         }
 
         private boolean isInPipeline(PatternHighlighter searchHighlighter) {
             Highlighter[] inPipeline = getHighlighters().getHighlighters();
             if ((inPipeline.length > 0) && 
                (searchHighlighter.equals(inPipeline[inPipeline.length -1]))) {
                 return true;
             }
             getHighlighters().removeHighlighter(searchHighlighter);
             return false;
         }
 
         protected SearchHighlighter createSearchHighlighter() {
             return new SearchHighlighter();
         }
 
     }
 //-------------------------------- sizing support
     
     /** ? */
     public void setVisibleRowCount(int visibleRowCount) {
         this.visibleRowCount = visibleRowCount;
     }
 
     /** ? */
     public int getVisibleRowCount() {
         return visibleRowCount;
     }
 
     public Dimension getPreferredScrollableViewportSize() {
         Dimension prefSize = super.getPreferredScrollableViewportSize();
 
         // JTable hardcodes this to 450 X 400, so we'll calculate it
         // based on the preferred widths of the columns and the
         // visibleRowCount property instead...
 
         if (prefSize.getWidth() == 450 && prefSize.getHeight() == 400) {
             TableColumnModel columnModel = getColumnModel();
             int columnCount = columnModel.getColumnCount();
 
             int w = 0;
             for (int i = 0; i < columnCount; i++) {
                 TableColumn column = columnModel.getColumn(i);
                 initializeColumnPreferredWidth(column);
                 w += column.getPreferredWidth();
             }
             prefSize.width = w;
             JTableHeader header = getTableHeader();
             // remind(aim): height is still off...???
             int rowCount = getVisibleRowCount();
             prefSize.height = rowCount * getRowHeight()
                     + (header != null ? header.getPreferredSize().height : 0);
             setPreferredScrollableViewportSize(prefSize);
         }
         return prefSize;
     }
 
     /**
      * Packs all the columns to their optimal size. Works best with auto
      * resizing turned off.
      * 
      * Contributed by M. Hillary (Issue #60)
      * 
      * @param margin
      *            the margin to apply to each column.
      */
     public void packTable(int margin) {
         for (int c = 0; c < getColumnCount(); c++)
             packColumn(c, margin, -1);
     }
 
     /**
      * Packs an indivudal column in the table. Contributed by M. Hillary (Issue
      * #60)
      * 
      * @param column
      *            The Column index to pack in View Coordinates
      * @param margin
      *            The Margin to apply to the column width.
      */
     public void packColumn(int column, int margin) {
         packColumn(column, margin, -1);
     }
 
     /**
      * Packs an indivual column in the table to less than or equal to the
      * maximum witdth. If maximun is -1 then the column is made as wide as it
      * needs. Contributed by M. Hillary (Issue #60)
      * 
      * @param column
      *            The Column index to pack in View Coordinates
      * @param margin
      *            The margin to apply to the column
      * @param max
      *            The maximum width the column can be resized to. -1 mean any
      *            size.
      */
     public void packColumn(int column, int margin, int max) {
         getColumnFactory().packColumn(this, getColumnExt(column), margin, max);
     }
 
     /**
      * Initialize the preferredWidth of the specified column based on the
      * column's prototypeValue property. If the column is not an instance of
      * <code>TableColumnExt</code> or prototypeValue is <code>null</code>
      * then the preferredWidth is left unmodified.
      * 
      * @see org.jdesktop.swingx.table.TableColumnExt#setPrototypeValue
      * @param column
      *            TableColumn object representing view column
      */
     protected void initializeColumnPreferredWidth(TableColumn column) {
         if (column instanceof TableColumnExt) {
             getColumnFactory().configureColumnWidths(this,
                     (TableColumnExt) column);
         }
     }
 
     
 //----------------------------------- uniform data model access
     
     protected ComponentAdapter getComponentAdapter() {
         if (dataAdapter == null) {
             dataAdapter = new TableAdapter(this);
         }
         return dataAdapter;
     }
 
     
     protected static class TableAdapter extends ComponentAdapter {
         private final JXTable table;
 
         /**
          * Constructs a <code>TableDataAdapter</code> for the specified target
          * component.
          * 
          * @param component
          *            the target component
          */
         public TableAdapter(JXTable component) {
             super(component);
             table = component;
         }
 
         /**
          * Typesafe accessor for the target component.
          * 
          * @return the target component as a {@link javax.swing.JTable}
          */
         public JXTable getTable() {
             return table;
         }
 
 
         public String getColumnName(int columnIndex) {
             TableColumn column = getColumnByModelIndex(columnIndex);
             return column == null ? "" : column.getHeaderValue().toString();
         }
 
         protected TableColumn getColumnByModelIndex(int modelColumn) {
             List columns = table.getColumns(true);
             for (Iterator iter = columns.iterator(); iter.hasNext();) {
                 TableColumn column = (TableColumn) iter.next();
                 if (column.getModelIndex() == modelColumn) {
                     return column;
                 }
             }
             return null;
         }
 
         
         public String getColumnIdentifier(int columnIndex) {
             
             TableColumn column = getColumnByModelIndex(columnIndex);
             Object identifier = column != null ? column.getIdentifier() : null;
             return identifier != null ? identifier.toString() : null;
         }
         
         public int getColumnCount() {
             return table.getModel().getColumnCount();
         }
 
         public int getRowCount() {
             return table.getModel().getRowCount();
         }
 
         /**
          * {@inheritDoc}
          */
         public Object getValueAt(int row, int column) {
             return table.getModel().getValueAt(row, column);
         }
 
         public void setValueAt(Object aValue, int row, int column) {
             table.getModel().setValueAt(aValue, row, column);
         }
 
         public boolean isCellEditable(int row, int column) {
             return table.getModel().isCellEditable(row, column);
         }
 
         
         
         public boolean isTestable(int column) {
             return getColumnByModelIndex(column) != null;
         }
 //-------------------------- accessing view state/values
         
         public Object getFilteredValueAt(int row, int column) {
             return getValueAt(table.convertRowIndexToModel(row), column);
 //            return table.getValueAt(row, modelToView(column)); // in view coordinates
         }
 
         /**
          * {@inheritDoc}
          */
         public boolean isSelected() {
             return table.isCellSelected(row, column);
         }
         /**
          * {@inheritDoc}
          */
         public boolean hasFocus() {
             boolean rowIsLead = (table.getSelectionModel()
                     .getLeadSelectionIndex() == row);
             boolean colIsLead = (table.getColumnModel().getSelectionModel()
                     .getLeadSelectionIndex() == column);
             return table.isFocusOwner() && (rowIsLead && colIsLead);
         }
 
         /**
          * {@inheritDoc}
          */
         public int modelToView(int columnIndex) {
             return table.convertColumnIndexToView(columnIndex);
         }
 
         /**
          * {@inheritDoc}
          */
         public int viewToModel(int columnIndex) {
             return table.convertColumnIndexToModel(columnIndex);
         }
 
 
     }
 
  
 //--------------------- managing renderers/editors
     
     /** Returns the HighlighterPipeline assigned to the table, null if none. */
     public HighlighterPipeline getHighlighters() {
         return highlighters;
     }
 
     /**
      * Assigns a HighlighterPipeline to the table. bound property.
      */
     public void setHighlighters(HighlighterPipeline pipeline) {
         HighlighterPipeline old = getHighlighters();
         if (old != null) {
             old.removeChangeListener(getHighlighterChangeListener());
         }
         highlighters = pipeline;
         if (highlighters != null) {
             highlighters.addChangeListener(getHighlighterChangeListener());
         }
         firePropertyChange("highlighters", old, getHighlighters());
         repaint();
     }
 
     /**
      * returns the ChangeListener to use with highlighters. Creates one if
      * necessary.
      * 
      * @return != null
      */
     private ChangeListener getHighlighterChangeListener() {
         if (highlighterChangeListener == null) {
             highlighterChangeListener = new ChangeListener() {
 
                 public void stateChanged(ChangeEvent e) {
                     repaint();
 
                 }
 
             };
         }
         return highlighterChangeListener;
     }
 
 
     
     /**
      * Returns the decorated <code>Component</code> used as a stamp to render
      * the specified cell. Overrides superclass version to provide support for
      * cell decorators. 
      * 
      * Adjusts component orientation (guaranteed to happen before applying 
      * Highlighters).
      * see - https://swingx.dev.java.net/issues/show_bug.cgi?id=145
      * 
      * @param renderer
      *            the <code>TableCellRenderer</code> to prepare
      * @param row
      *            the row of the cell to render, where 0 is the first row
      * @param column
      *            the column of the cell to render, where 0 is the first column
      * @return the decorated <code>Component</code> used as a stamp to render
      *         the specified cell
      * @see org.jdesktop.swingx.decorator.Highlighter
      */
     public Component prepareRenderer(TableCellRenderer renderer, int row,
             int column) {
         Component stamp = super.prepareRenderer(renderer, row, column);
         adjustComponentOrientation(stamp);
         if (highlighters == null) {
             return stamp; // no need to decorate renderer with highlighters
         } else {
             // PENDING - JW: code duplication - 
             // add method to access component adapter with row/column
             // set as needed!
             ComponentAdapter adapter = getComponentAdapter();
             adapter.row = row;
             adapter.column = column;
             return highlighters.apply(stamp, adapter);
         }
     }
 
     
     /**
      * Overridden to adjust the editor's component orientation if 
      * appropriate.
      */
     @Override
     public Component prepareEditor(TableCellEditor editor, int row, int column) {
         Component comp =  super.prepareEditor(editor, row, column);
         adjustComponentOrientation(comp);
         return comp;
     }
 
     /**
      * adjusts the Component's orientation to JXTable's CO if appropriate.
      * Here: always.
      * 
      * @param stamp
      */
     protected void adjustComponentOrientation(Component stamp) {
         if (stamp.getComponentOrientation().equals(getComponentOrientation())) return;
         stamp.applyComponentOrientation(getComponentOrientation());
     }
 
     /**
      * Returns a new instance of the default renderer for the specified class.
      * This differs from <code>getDefaultRenderer()</code> in that it returns
      * a <b>new </b> instance each time so that the renderer may be set and
      * customized on a particular column.
      * 
      * PENDING: must not return null!
      * 
      * @param columnClass
      *            Class of value being rendered
      * @return TableCellRenderer instance which renders values of the specified
      *         type
      */
     public TableCellRenderer getNewDefaultRenderer(Class columnClass) {
         TableCellRenderer renderer = getDefaultRenderer(columnClass);
         if (renderer != null) {
             try {
                 return (TableCellRenderer) renderer.getClass().newInstance();
             } catch (Exception e) {
                 e.printStackTrace();
             }
         }
         return null;
     }
 
     /** ? */
     protected void createDefaultEditors() {
         super.createDefaultEditors();
         setLazyEditor(LinkModel.class, "org.jdesktop.swingx.LinkRenderer");
     }
 
     /**
      * Creates default cell renderers for objects, numbers, doubles, dates,
      * booleans, icons, and links.
      * THINK: delegate to TableCellRenderers?
      * 
      */
     protected void createDefaultRenderers() {
         // super.createDefaultRenderers();
         // This duplicates JTable's functionality in order to make the renderers
         // available in getNewDefaultRenderer(); If JTable's renderers either
         // were public, or it provided a factory for *new* renderers, this would
         // not be needed
 
         defaultRenderersByColumnClass = new UIDefaults();
 
         // Objects
         setLazyRenderer(Object.class,
                 "javax.swing.table.DefaultTableCellRenderer");
 
         // Numbers
         setLazyRenderer(Number.class,
                 "org.jdesktop.swingx.JXTable$NumberRenderer");
 
         // Doubles and Floats
         setLazyRenderer(Float.class,
                 "org.jdesktop.swingx.JXTable$DoubleRenderer");
         setLazyRenderer(Double.class,
                 "org.jdesktop.swingx.JXTable$DoubleRenderer");
 
         // Dates
         setLazyRenderer(Date.class, "org.jdesktop.swingx.JXTable$DateRenderer");
 
         // Icons and ImageIcons
         setLazyRenderer(Icon.class, "org.jdesktop.swingx.JXTable$IconRenderer");
         setLazyRenderer(ImageIcon.class,
                 "org.jdesktop.swingx.JXTable$IconRenderer");
 
         // Booleans
         setLazyRenderer(Boolean.class,
                 "org.jdesktop.swingx.JXTable$BooleanRenderer");
 
         // Other
         setLazyRenderer(LinkModel.class, "org.jdesktop.swingx.LinkRenderer");
     }
 
 
     /** ? */
     private void setLazyValue(Hashtable h, Class c, String s) {
         h.put(c, new UIDefaults.ProxyLazyValue(s));
     }
 
     /** ? */
     private void setLazyRenderer(Class c, String s) {
         setLazyValue(defaultRenderersByColumnClass, c, s);
     }
 
     /** ? */
     private void setLazyEditor(Class c, String s) {
         setLazyValue(defaultEditorsByColumnClass, c, s);
     }
 
     /*
      * Default Type-based Renderers: JTable's default table cell renderer
      * classes are private and JTable:getDefaultRenderer() returns a *shared*
      * cell renderer instance, thus there is no way for us to instantiate a new
      * instance of one of its default renderers. So, we must replicate the
      * default renderer classes here so that we can instantiate them when we
      * need to create renderers to be set on specific columns.
      */
     public static class NumberRenderer extends DefaultTableCellRenderer {
         public NumberRenderer() {
             super();
             setHorizontalAlignment(JLabel.TRAILING);
         }
     }
 
     public static class DoubleRenderer extends NumberRenderer {
         NumberFormat formatter;
 
         public DoubleRenderer() {
             super();
         }
 
         public void setValue(Object value) {
             if (formatter == null) {
                 formatter = NumberFormat.getInstance();
             }
             setText((value == null) ? "" : formatter.format(value));
         }
     }
 
     public static class DateRenderer extends DefaultTableCellRenderer {
         DateFormat formatter;
 
         public DateRenderer() {
             super();
         }
 
         public void setValue(Object value) {
             if (formatter == null) {
                 formatter = DateFormat.getDateInstance();
             }
             setText((value == null) ? "" : formatter.format(value));
         }
     }
 
     public static class IconRenderer extends DefaultTableCellRenderer {
         public IconRenderer() {
             super();
             setHorizontalAlignment(JLabel.CENTER);
         }
 
         public void setValue(Object value) {
             setIcon((value instanceof Icon) ? (Icon) value : null);
         }
     }
 
     /*
      * re- c&p'd from 1.5 JTable. 
      */
     public static class BooleanRenderer extends JCheckBox implements // , UIResource
             TableCellRenderer     {
         private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
 
         public BooleanRenderer() {
             super();
             setHorizontalAlignment(JLabel.CENTER);
             setBorderPainted(true);
         }
 
         public Component getTableCellRendererComponent(JTable table,
                 Object value, boolean isSelected, boolean hasFocus, int row,
                 int column) {
             if (isSelected) {
                 setForeground(table.getSelectionForeground());
                 super.setBackground(table.getSelectionBackground());
             } else {
                 setForeground(table.getForeground());
                 setBackground(table.getBackground());
             }
             setSelected((value != null && ((Boolean) value).booleanValue()));
 
             if (hasFocus) {
                 setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
             } else {
                 setBorder(noFocusBorder);
             }
 
             return this;
         }
     }
 
 // ---------------------------- updateUI support
     
     /**
      * bug fix: super doesn't update all renderers/editors.
      */
     public void updateUI() {
         super.updateUI();
         // JW PENDING: update columnControl
         if (columnControlButton != null) {
             columnControlButton.updateUI();
         }
         for (Enumeration defaultEditors = defaultEditorsByColumnClass
                 .elements(); defaultEditors.hasMoreElements();) {
             updateEditorUI(defaultEditors.nextElement());
         }
 
         for (Enumeration defaultRenderers = defaultRenderersByColumnClass
                 .elements(); defaultRenderers.hasMoreElements();) {
             updateRendererUI(defaultRenderers.nextElement());
         }
         Enumeration columns = getColumnModel().getColumns();
         if (getColumnModel() instanceof TableColumnModelExt) {
             columns = Collections
                     .enumeration(((TableColumnModelExt) getColumnModel())
                             .getAllColumns());
         }
         while (columns.hasMoreElements()) {
             TableColumn column = (TableColumn) columns.nextElement();
             updateEditorUI(column.getCellEditor());
             updateRendererUI(column.getCellRenderer());
             updateRendererUI(column.getHeaderRenderer());
         }
         updateRowHeightUI(true);
         configureViewportBackground();
     }
 
     /** ? */
     private void updateRowHeightUI(boolean respectRowSetFlag) {
         if (respectRowSetFlag && isXTableRowHeightSet)
             return;
         int minimumSize = getFont().getSize() + 6;
         int uiSize = UIManager.getInt(UIPREFIX + "rowHeight");
         setRowHeight(Math.max(minimumSize, uiSize != 0 ? uiSize : 18));
         isXTableRowHeightSet = false;
     }
 
     /** Changes the row height for all rows in the table. */
     public void setRowHeight(int rowHeight) {
         super.setRowHeight(rowHeight);
         if (rowHeight > 0) {
             isXTableRowHeightSet = true;
         }
         updateViewSizeSequence();
 
     }
 
     
     public void setRowHeight(int row, int rowHeight) {
         if (!isRowHeightEnabled()) return;
         super.setRowHeight(row, rowHeight);
         updateViewSizeSequence();
         resizeAndRepaint();
     }
 
     /**
      * sets enabled state of individual rowHeight support. The default 
      * is false.
      * Enabling the support envolves reflective access
      * to super's private field rowModel which may fail due to security
      * issues. If failing the support is not enabled.
      * 
      * PENDING: should we throw an Exception if the enabled fails? 
      * Or silently fail - depends on runtime context, 
      * can't do anything about it.
      * 
      * @param enabled
      */
     public void setRowHeightEnabled(boolean enabled) {
         boolean old = isRowHeightEnabled();
         if (old == enabled) return;
         if (enabled && !canEnableRowHeight()) return;
         rowHeightEnabled = enabled;
         if (!enabled) {
             adminSetRowHeight(getRowHeight());
         }
         firePropertyChange("rowHeightEnabled", old, rowHeightEnabled);
     }
     
     private boolean canEnableRowHeight() {
         return getRowModelField() != null;
     }
 
     public boolean isRowHeightEnabled() {
         return rowHeightEnabled;
     }
 
     private SizeSequence getSuperRowModel() {
         try {
             Field field = getRowModelField();
             if (field != null) {
                 return (SizeSequence) field.get(this);
             }
         } catch (SecurityException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         } catch (IllegalArgumentException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         } catch (IllegalAccessException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
         return null;
     }
 
     /**
      * @return
      * @throws NoSuchFieldException
      */
     private Field getRowModelField() {
         if (rowModelField == null) {
             try {
                 rowModelField = JTable.class.getDeclaredField("rowModel");
                 rowModelField.setAccessible(true);
             } catch (SecurityException e) {
                 rowModelField = null;
                 e.printStackTrace();
             } catch (NoSuchFieldException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
             }
         }
         return rowModelField;
     }
     
     /**
      * 
      * @return
      */
     protected RowSizing getRowSizing() {
         if (rowSizing == null) {
             rowSizing = new RowSizing(filters);
         }
         return rowSizing;
     }
 
     /**
      * calling setRowHeight for internal reasons.
      * Keeps the isXTableRowHeight unchanged.
      */
     protected void adminSetRowHeight(int rowHeight) {
         boolean heightSet = isXTableRowHeightSet;
         setRowHeight(rowHeight); 
         isXTableRowHeightSet = heightSet;
     }
 
 
     private void updateEditorUI(Object value) {
         // maybe null or proxyValue
         if (!(value instanceof TableCellEditor))
             return;
         // super handled this
         if ((value instanceof JComponent)
                 || (value instanceof DefaultCellEditor))
             return;
         // custom editors might balk about fake rows/columns
         try {
             Component comp = ((TableCellEditor) value)
                     .getTableCellEditorComponent(this, null, false, -1, -1);
             if (comp instanceof JComponent) {
                 ((JComponent) comp).updateUI();
             }
         } catch (Exception e) {
             // ignore - can't do anything
         }
     }
 
     /** ? */
     private void updateRendererUI(Object value) {
         // maybe null or proxyValue
         if (!(value instanceof TableCellRenderer))
             return;
         // super handled this
         if (value instanceof JComponent)
             return;
         // custom editors might balk about fake rows/columns
         try {
             Component comp = ((TableCellRenderer) value)
                     .getTableCellRendererComponent(this, null, false, false,
                             -1, -1);
             if (comp instanceof JComponent) {
                 ((JComponent) comp).updateUI();
             }
         } catch (Exception e) {
             // ignore - can't do anything
         }
     }
 
 
     
 //---------------------------- overriding super factory methods and buggy
     /**
      * workaround bug in JTable. (Bug Parade ID #6291631 - negative y is mapped
      * to row 0).
      */
     public int rowAtPoint(Point point) {
         if (point.y < 0)
             return -1;
         return super.rowAtPoint(point);
     }
 
     
     /** ? */
     protected JTableHeader createDefaultTableHeader() {
         return new JXTableHeader(columnModel);
     }
 
     /** ? */
     protected TableColumnModel createDefaultColumnModel() {
         return new DefaultTableColumnModelExt();
     }
 
     
 }
