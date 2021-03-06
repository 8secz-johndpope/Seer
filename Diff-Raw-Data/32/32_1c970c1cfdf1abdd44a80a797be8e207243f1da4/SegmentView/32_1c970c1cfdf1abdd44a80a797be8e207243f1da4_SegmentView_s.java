 package com.spartansoftwareinc.vistatec.rwb.segment;
 
 import com.spartansoftwareinc.plugins.PluginManager;
 import com.spartansoftwareinc.vistatec.rwb.ContextMenu;
 import com.spartansoftwareinc.vistatec.rwb.its.ITSMetadata;
 import com.spartansoftwareinc.vistatec.rwb.its.LanguageQualityIssue;
 import com.spartansoftwareinc.vistatec.rwb.its.Provenance;
 import com.spartansoftwareinc.vistatec.rwb.rules.DataCategoryFlag;
 import com.spartansoftwareinc.vistatec.rwb.rules.RuleConfiguration;
 import com.spartansoftwareinc.vistatec.rwb.rules.RuleListener;
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Dimension;
 import java.awt.FontMetrics;
 import java.awt.event.ActionEvent;
 import java.awt.event.KeyEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.io.IOException;
 import java.util.EventObject;
 import javax.swing.AbstractAction;
 import javax.swing.AbstractCellEditor;
 import javax.swing.BorderFactory;
 import javax.swing.JLabel;
 import javax.swing.JScrollPane;
 import javax.swing.JTable;
 import javax.swing.KeyStroke;
 import javax.swing.ListSelectionModel;
 import javax.swing.UIManager;
 import javax.swing.event.CellEditorListener;
 import javax.swing.event.ChangeEvent;
 import javax.swing.event.ListSelectionEvent;
 import javax.swing.event.ListSelectionListener;
 import javax.swing.event.TableColumnModelEvent;
 import javax.swing.event.TableColumnModelListener;
 import javax.swing.table.DefaultTableCellRenderer;
 import javax.swing.table.TableCellEditor;
 import javax.swing.table.TableCellRenderer;
 import javax.swing.table.TableColumnModel;
 import javax.swing.table.TableRowSorter;
 import net.sf.okapi.common.resource.TextContainer;
 import org.apache.log4j.Logger;
 
 /**
  * Table view containing the source and target segments extracted from the
  * opened file. Indicates attached LTS metadata as flags.
  */
 public class SegmentView extends JScrollPane implements RuleListener {
     private static Logger LOG = Logger.getLogger(SegmentView.class);
 
     protected SegmentController segmentController;
     protected JTable sourceTargetTable;
     private ListSelectionModel tableSelectionModel;
     private SegmentAttributeView attrView;
     private TableColumnModel tableColumnModel;
     protected TableRowSorter sort;
 
     protected RuleConfiguration ruleConfig;
     protected PluginManager pluginManager;
 
     public SegmentView(SegmentAttributeView attr, SegmentController segController) throws IOException, InstantiationException, InstantiationException, IllegalAccessException {
         attrView = attr;
         segmentController = segController;
         UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createLineBorder(Color.BLUE, 2));
         initializeTable();
         ruleConfig = new RuleConfiguration(this);
         pluginManager = new PluginManager();
         pluginManager.discover(pluginManager.getPluginDir());
     }
 
     public void initializeTable() {
         sourceTargetTable = new JTable(segmentController.getSegmentTableModel());
         sourceTargetTable.getTableHeader().setReorderingAllowed(false);
 
         ListSelectionListener selectSegmentHandler = new ListSelectionListener() {
             @Override
             public void valueChanged(ListSelectionEvent lse) {
                 selectedSegment();
             }
         };
         tableSelectionModel = sourceTargetTable.getSelectionModel();
         tableSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         tableSelectionModel.addListSelectionListener(selectSegmentHandler);
 
         DefaultTableCellRenderer segNumAlign = new DefaultTableCellRenderer();
         segNumAlign.setHorizontalAlignment(JLabel.LEFT);
         segNumAlign.setVerticalAlignment(JLabel.TOP);
         sourceTargetTable.setDefaultRenderer(Integer.class, segNumAlign);
         sourceTargetTable.setDefaultRenderer(DataCategoryFlag.class,
                 new DataCategoryFlagRenderer());
         sourceTargetTable.setDefaultRenderer(String.class,
                 new SegmentTextRenderer());
 
         tableColumnModel = sourceTargetTable.getColumnModel();
         tableColumnModel.getSelectionModel().addListSelectionListener(
                 selectSegmentHandler);
         tableColumnModel.getColumn(0).setMinWidth(15);
         tableColumnModel.getColumn(0).setPreferredWidth(20);
         tableColumnModel.getColumn(0).setMaxWidth(50);
 
         tableColumnModel.getColumn(segmentController.getSegmentTargetColumnIndex())
                 .setCellEditor(new SegmentEditor());
         int flagMinWidth = 15, flagPrefWidth = 15, flagMaxWidth = 20;
         for (int i = SegmentTableModel.NONFLAGCOLS;
              i < SegmentTableModel.NONFLAGCOLS+SegmentTableModel.NUMFLAGS; i++) {
             tableColumnModel.getColumn(i).setMinWidth(flagMinWidth);
             tableColumnModel.getColumn(i).setPreferredWidth(flagPrefWidth);
             tableColumnModel.getColumn(i).setMaxWidth(flagMaxWidth);
         }
 
         tableColumnModel.addColumnModelListener(new TableColumnModelListener() {
 
             @Override
             public void columnAdded(TableColumnModelEvent tcme) {}
 
             @Override
             public void columnRemoved(TableColumnModelEvent tcme) {}
 
             @Override
             public void columnMoved(TableColumnModelEvent tcme) {}
 
             @Override
             public void columnMarginChanged(ChangeEvent ce) {
                 updateRowHeights();
             }
 
             @Override
             public void columnSelectionChanged(ListSelectionEvent lse) {}
         });
 
         sourceTargetTable.addMouseListener(new SegmentPopupMenuListener());
         setViewportView(sourceTargetTable);
     }
 
     public void clearTable() {
         sourceTargetTable.clearSelection();
         sourceTargetTable.setRowSorter(null);
         setViewportView(null);
     }
 
     public void reloadTable() {
         clearTable();
         attrView.treeView.clearTree();
         segmentController.fireTableDataChanged();
         addFilters();
         // Adjust the segment number column width
         tableColumnModel.getColumn(
                 segmentController.getSegmentNumColumnIndex())
                 .setPreferredWidth(this.getFontMetrics(this.getFont())
                 .stringWidth(" " + segmentController.getNumSegments()));
         updateRowHeights();
         setViewportView(sourceTargetTable);
     }
 
     public void requestFocusTable() {
         sourceTargetTable.requestFocus();
     }
 
     public void addFilters() {
         sort = new TableRowSorter(segmentController.getSegmentTableModel());
         sourceTargetTable.setRowSorter(sort);
         sort.setRowFilter(ruleConfig);
     }
 
     protected void updateRowHeights() {
         setViewportView(null);
 
         SegmentTextCell segmentCell = new SegmentTextCell();
         segmentCell.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
         for (int row = 0; row < sourceTargetTable.getRowCount(); row++) {
             FontMetrics font = sourceTargetTable.getFontMetrics(sourceTargetTable.getFont());
             int rowHeight = font.getHeight();
             for (int col = 1; col < 4; col++) {
                 int width = sourceTargetTable.getColumnModel().getColumn(col).getWidth();
                 if (col == 1) {
                     String text = segmentController.getSegment(row).getSource().getCodedText();
                     segmentCell.setText(text);
                 } else if (col == 2) {
                     String text = segmentController.getSegment(row).getTarget().getCodedText();
                     segmentCell.setText(text);
                 } else if (col == 3) {
                     String text = segmentController.getSegment(row).getOriginalTarget().getCodedText();
                     segmentCell.setText(text);
                 }
                 // Need to set width to force text area to calculate a pref height
                 segmentCell.setSize(new Dimension(width, sourceTargetTable.getRowHeight(row)));
                 rowHeight = Math.max(rowHeight, segmentCell.getPreferredSize().height);
             }
             sourceTargetTable.setRowHeight(row, rowHeight);
         }
         setViewportView(sourceTargetTable);
     }
 
     public Segment getSelectedSegment() {
         Segment selectedSeg = null;
         if (sourceTargetTable.getSelectedRow() >= 0) {
             selectedSeg = segmentController.getSegment(sort.convertRowIndexToModel(
                 sourceTargetTable.getSelectedRow()));
         }
         return selectedSeg;
     }
 
     public void selectedSegment() {
         Segment seg = getSelectedSegment();
         if (seg != null) {
             attrView.setSelectedSegment(seg);
             int colIndex = sourceTargetTable.getSelectedColumn();
             if (colIndex >= SegmentTableModel.NONFLAGCOLS) {
                 int adjustedFlagIndex = colIndex - SegmentTableModel.NONFLAGCOLS;
                 ITSMetadata its = ruleConfig.getTopDataCategory(seg, adjustedFlagIndex);
                 if (its != null) {
                     attrView.setSelectedMetadata(its);
                 }
             }
         }
     }
 
     public void notifyAddedLQI(LanguageQualityIssue lqi, Segment seg) {
         attrView.addLQIMetadata(lqi);
     }
 
     public void notifyModifiedLQI(LanguageQualityIssue lqi, Segment seg) {
         attrView.setSelectedMetadata(lqi);
         attrView.setSelectedSegment(seg);
         segmentController.updateSegment(seg);
         int selectedRow = sourceTargetTable.getSelectedRow();
         reloadTable();
         sourceTargetTable.setRowSelectionInterval(selectedRow, selectedRow);
     }
 
     public void notifyAddedProv(Provenance prov) {
         attrView.addProvMetadata(prov);
     }
 
     public void notifyDeletedSegments() {
         attrView.deletedSegments();
     }
 
     /**
      * Rule configuration methods.
      */
     public RuleConfiguration getRuleConfig() {
         return this.ruleConfig;
     }
 
     @Override
     public void enabledRule(String ruleLabel, boolean enabled) {
         reloadTable();
     }
 
     @Override
     public void allSegments(boolean enabled) {
         reloadTable();
     }
 
     @Override
     public void allMetadataSegments(boolean enabled) {
         reloadTable();
     }
 
     public PluginManager getPluginManager() {
         return this.pluginManager;
     }
 
     /**
      * TableCellRenderer for source/target text in the SegmentTableView.
      */
     public class SegmentTextRenderer implements TableCellRenderer {
 
         @Override
         public Component getTableCellRendererComponent(JTable jtable, Object o,
             boolean isSelected, boolean hasFocus, int row, int col) {
             SegmentTextCell renderTextPane = new SegmentTextCell();
             if (segmentController.getNumSegments() > row) {
                 Segment seg = segmentController.getSegment(sort.convertRowIndexToModel(row));
                 TextContainer tc = null;
                 if (segmentController.getSegmentSourceColumnIndex() == col) {
                     tc = seg.getSource();
                 } else if (segmentController.getSegmentTargetColumnIndex() == col) {
                     tc = seg.getTarget();
                 } else if (segmentController.getSegmentTargetOriginalColumnIndex() == col) {
                     tc = seg.getOriginalTarget();
                 }
                 if (tc != null) {
                     renderTextPane.setTextContainer(tc, false);
                 }
                 renderTextPane.setBackground(isSelected ? jtable.getSelectionBackground() : jtable.getBackground());
                 renderTextPane.setForeground(isSelected ? jtable.getSelectionForeground() : jtable.getForeground());
                 renderTextPane.setBorder(hasFocus ? UIManager.getBorder("Table.focusCellHighlightBorder") : jtable.getBorder());
             }
 
             return renderTextPane;
         }
     }
 
     public class DataCategoryFlagRenderer extends JLabel implements TableCellRenderer {
 
         public DataCategoryFlagRenderer() {
             setOpaque(true);
         }
 
         @Override
         public Component getTableCellRendererComponent(JTable jtable, Object obj, boolean isSelected, boolean hasFocus, int row, int col) {
             DataCategoryFlag flag = (DataCategoryFlag) obj;
             setBackground(flag.getFill());
             setBorder(hasFocus ?
                     UIManager.getBorder("Table.focusCellHighlightBorder") :
                     flag.getBorder());
             setText(flag.getText());
             setHorizontalAlignment(CENTER);
             return this;
         }
     }
 
     public class SegmentEditor extends AbstractCellEditor implements TableCellEditor {
 
         protected SegmentTextCell editorComponent;
         protected SegmentCellEditorListener editListener;
 
         public SegmentEditor() {
             editListener = new SegmentCellEditorListener();
             addCellEditorListener(editListener);
         }
 
         @Override
         public Component getTableCellEditorComponent(JTable jtable, Object value,
             boolean isSelected, int row, int col) {
             Segment seg = segmentController.getSegment(sort.convertRowIndexToModel(row));
             editListener.setBeginEdit(seg, seg.getTarget().getCodedText());
             editorComponent = new SegmentTextCell(seg.getTarget(), false);
             editorComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "finish");
             editorComponent.getActionMap().put("finish", new AbstractAction() {
                 @Override
                 public void actionPerformed(ActionEvent e) {
                     fireEditingStopped();
                 }
             });
             return editorComponent;
         }
 
         @Override
         public Object getCellEditorValue() {
             return editorComponent.getTextContainer();
         }
 
         @Override
         public boolean isCellEditable(EventObject anEvent) {
             if (anEvent instanceof MouseEvent) {
                 return ((MouseEvent)anEvent).getClickCount() >= 2;
             }
             return true;
         }
     }
 
     public class SegmentCellEditorListener implements CellEditorListener {
         private Segment seg;
         private String codedText;
 
         public void setBeginEdit(Segment seg, String codedText) {
             this.seg = seg;
             this.codedText = codedText;
         }
 
         @Override
         public void editingStopped(ChangeEvent ce) {
             if (!this.seg.getTarget().getCodedText().equals(codedText)) {
                 segmentController.updateSegment(seg);
                 reloadTable();
             }
         }
 
         @Override
         public void editingCanceled(ChangeEvent ce) {
             // TODO: Cancel not supported.
         }
     }
 
     public class SegmentPopupMenuListener extends MouseAdapter {
         @Override
         public void mousePressed(MouseEvent e) {
             if (e.isPopupTrigger()) {
                Segment seg = null;
                int r = sourceTargetTable.rowAtPoint(e.getPoint());
                if (r >= 0 && r < sourceTargetTable.getRowCount()) {
                    sourceTargetTable.setRowSelectionInterval(r, r);
                    seg = segmentController.getSegment(r);
                }
 
                if (seg != null) {
                    ContextMenu menu = new ContextMenu(seg);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
             }
         }
     }
 }
