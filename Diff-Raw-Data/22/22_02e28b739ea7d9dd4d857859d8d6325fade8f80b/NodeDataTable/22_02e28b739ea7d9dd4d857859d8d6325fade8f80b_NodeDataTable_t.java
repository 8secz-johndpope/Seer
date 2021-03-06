 /*
 Copyright 2008 WebAtlas
 Authors : Mathieu Bastian, Mathieu Jacomy, Julian Bilcke, Eduardo Ramos
 Website : http://www.gephi.org
 
 This file is part of Gephi.
 
 Gephi is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Gephi is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.gephi.ui.datatable;
 
 import java.awt.Point;
 import java.awt.Rectangle;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyAdapter;
 import java.awt.event.KeyEvent;
 import java.awt.event.MouseEvent;
 import java.lang.reflect.InvocationTargetException;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.regex.Pattern;
 import java.util.regex.PatternSyntaxException;
 import javax.swing.JMenuItem;
 import javax.swing.JPopupMenu;
 import javax.swing.SwingUtilities;
 import javax.swing.event.ListSelectionEvent;
 import javax.swing.event.ListSelectionListener;
 import javax.swing.event.PopupMenuEvent;
 import javax.swing.event.PopupMenuListener;
 import javax.swing.event.TreeModelListener;
 import javax.swing.tree.TreeModel;
 import javax.swing.tree.TreeNode;
 import javax.swing.tree.TreePath;
 import org.gephi.data.attributes.api.AttributeColumn;
 import org.gephi.data.attributes.api.AttributeOrigin;
 import org.gephi.data.properties.PropertiesColumn;
 import org.gephi.graph.api.HierarchicalGraph;
 import org.gephi.graph.api.ImmutableTreeNode;
 import org.gephi.graph.api.Node;
 import org.netbeans.swing.etable.QuickFilter;
 import org.netbeans.swing.outline.DefaultOutlineModel;
 import org.netbeans.swing.outline.Outline;
 import org.netbeans.swing.outline.OutlineModel;
 import org.netbeans.swing.outline.RenderDataProvider;
 import org.netbeans.swing.outline.RowModel;
 import org.openide.awt.MouseUtils;
 import org.openide.util.Exceptions;
 import org.openide.util.Lookup;
 import org.gephi.datalaboratory.api.DataLaboratoryHelper;
 import org.gephi.datalaboratory.spi.nodes.NodesManipulator;
 import org.gephi.tools.api.EditWindowController;
 
 /**
  *
  * @author Mathieu Bastian
  */
 public class NodeDataTable {
 
     private final boolean popupAllowed = true;
     private Outline outlineTable;
     private QuickFilter quickFilter;
     private Pattern pattern;
     private DataTablesModel dataTablesModel;
     Node[] selectedNodes;
 
     public NodeDataTable() {
         outlineTable = new Outline();
 
         quickFilter = new QuickFilter() {
 
             public boolean accept(Object value) {
                 if (value == null) {
                     return false;
                 }
                 if (value instanceof ImmutableTreeNode) {
                     String label = ((ImmutableTreeNode) value).getNode().getNodeData().getLabel();
                     if (label != null) {
                         return pattern.matcher(label).find();
                     }
                     return false;
                 }
                 return pattern.matcher(value.toString()).find();
             }
         };
 
         outlineTable.addMouseListener(new PopupAdapter());
         outlineTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
 
             public void valueChanged(ListSelectionEvent e) {
                 EditWindowController edc = Lookup.getDefault().lookup(EditWindowController.class);
                 if (edc.isOpen()) {
                     if (outlineTable.getSelectedRow() != -1) {
                         Node node = getNodeFromRow(outlineTable.getSelectedRow());
                         edc.editNode(node);
                     } else {
                         edc.disableEdit();
                     }
                 }
             }
         });
         outlineTable.addKeyListener(new KeyAdapter() {
 
             @Override
             public void keyReleased(KeyEvent e) {
                 if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                     Node[] selectedNodes = getNodesFromSelectedRows();
                     if (selectedNodes.length > 0) {
                         DataLaboratoryHelper dlh = Lookup.getDefault().lookup(DataLaboratoryHelper.class);
                         NodesManipulator del = dlh.getDeleteNodesManipulator();
                         if (del != null) {
                             del.setup(selectedNodes, null);
                             if (del.canExecute()) {
                                 dlh.executeManipulator(del);
                             }
                         }
                     }
                 }
             }
         });
     }
 
     public Outline getOutlineTable() {
         return outlineTable;
     }
 
     public boolean setFilter(String regularExpr, int columnIndex) {
         try {
             pattern = Pattern.compile(regularExpr, Pattern.CASE_INSENSITIVE);
         } catch (PatternSyntaxException e) {
             return false;
         }
         outlineTable.setQuickFilter(columnIndex, quickFilter);
         return true;
     }
 
     public void refreshModel(HierarchicalGraph graph, AttributeColumn[] cols, final DataTablesModel dataTablesModel) {
         if (selectedNodes == null) {
             selectedNodes = getNodesFromSelectedRows();
         }
         NodeTreeModel nodeTreeModel = new NodeTreeModel(graph.wrapToTreeNode());
         final OutlineModel mdl = DefaultOutlineModel.createOutlineModel(nodeTreeModel, new NodeRowModel(cols), true);
         outlineTable.setRootVisible(false);
         outlineTable.setRenderDataProvider(new NodeRenderer());
 
         try {
             SwingUtilities.invokeAndWait(new Runnable() {
 
                 public void run() {
                     outlineTable.setModel(mdl);
                     NodeDataTable.this.dataTablesModel = dataTablesModel;
                     setNodesSelection(selectedNodes);//Keep row selection before refreshing.
                     selectedNodes = null;
                 }
             });
         } catch (InterruptedException ex) {
             Exceptions.printStackTrace(ex);
         } catch (InvocationTargetException ex) {
             Exceptions.printStackTrace(ex);
         }
     }
 
     public void setNodesSelection(Node[] nodes) {
         this.selectedNodes = nodes;//Keep this selection request to be able to do it if the table is first refreshed later.
         HashSet<Node> nodesSet = new HashSet<Node>();
         nodesSet.addAll(Arrays.asList(nodes));
         outlineTable.clearSelection();
         for (int i = 0; i < outlineTable.getRowCount(); i++) {
             if (nodesSet.contains(getNodeFromRow(i))) {
                 outlineTable.addRowSelectionInterval(i, i);
             }
         }
     }
 
     public void scrollToFirstNodeSelected() {
         int row = outlineTable.getSelectedRow();
         if (row != -1) {
            Rectangle rect = outlineTable.getCellRect(row, 0, true);
             outlineTable.scrollRectToVisible(rect);
         }
     }
 
     public boolean hasData() {
         return outlineTable.getRowCount() > 0;
     }
 
     private static class NodeTreeModel implements TreeModel {
 
         private ImmutableTreeNode root;
 
         public NodeTreeModel(ImmutableTreeNode root) {
             this.root = root;
         }
 
         public Object getRoot() {
             return root;
         }
 
         public Object getChild(Object parent, int index) {
             TreeNode node = (TreeNode) parent;
             return node.getChildAt(index);
         }
 
         public int getChildCount(Object parent) {
 
             TreeNode node = (TreeNode) parent;
             return node.getChildCount();
 
         }
 
         public boolean isLeaf(Object node) {
 
             TreeNode n = (TreeNode) node;
             return n.isLeaf();
 
         }
 
         public void valueForPathChanged(TreePath path, Object newValue) {
         }
 
         public int getIndexOfChild(Object parent, Object child) {
             if (parent == null || child == null) {
                 return -1;
             }
             TreeNode node = (TreeNode) parent;
             return node.getIndex((TreeNode) child);
         }
 
         public void addTreeModelListener(TreeModelListener l) {
         }
 
         public void removeTreeModelListener(TreeModelListener l) {
         }
     }
 
     private static class NodeRowModel implements RowModel {
 
         private NodeDataColumn[] columns;
 
         public NodeRowModel(AttributeColumn[] attributeColumns) {
 
             ArrayList<NodeDataColumn> cols = new ArrayList<NodeDataColumn>();
             for (AttributeColumn c : attributeColumns) {
                 cols.add(new AttributeNodeDataColumn(c));
             }
             columns = cols.toArray(new NodeDataColumn[0]);
         }
 
         public int getColumnCount() {
             return columns.length;
         }
 
         public Object getValueFor(Object node, int column) {
             ImmutableTreeNode treeNode = (ImmutableTreeNode) node;
             return columns[column].getValueFor(treeNode);
         }
 
         public Class getColumnClass(int column) {
             return columns[column].getColumnClass();
         }
 
         public boolean isCellEditable(Object node, int column) {
             return columns[column].isEditable();
         }
 
         public void setValueFor(Object node, int column, Object value) {
             columns[column].setValueFor((ImmutableTreeNode) node, value);
         }
 
         public String getColumnName(int column) {
             return columns[column].getColumnName();
         }
     }
 
     private static interface NodeDataColumn {
 
         public Class getColumnClass();
 
         public String getColumnName();
 
         public Object getValueFor(ImmutableTreeNode node);
 
         public void setValueFor(ImmutableTreeNode node, Object value);
 
         public boolean isEditable();
     }
 
     private static class AttributeNodeDataColumn implements NodeDataColumn {
 
         private AttributeColumn column;
 
         public AttributeNodeDataColumn(AttributeColumn column) {
             this.column = column;
         }
 
         public Class getColumnClass() {
             return String.class;//Treat all columns as Strings. Also fix the fact that the table implementation does not allow to edit Character cells.
         }
 
         public String getColumnName() {
             return column.getTitle();
         }
 
         public Object getValueFor(ImmutableTreeNode node) {
             Node graphNode = node.getNode();
             if (graphNode.getId() == -1) {
                 return null;
             }
             Object value = graphNode.getNodeData().getAttributes().getValue(column.getIndex());
             return value != null ? value.toString() : null;//Show values as Strings like in Edit window and other parts of the program to be consistent
         }
 
         public void setValueFor(ImmutableTreeNode node, Object value) {
             Node graphNode = node.getNode();
             String str = (String) value;//Treat all columns as Strings
            try {
                value = column.getType().parse(str);
            } catch (Exception ex) {
                value=null;//Could not parse
            }
            graphNode.getNodeData().getAttributes().setValue(column.getIndex(), value);
         }
 
         public boolean isEditable() {
             return column.getOrigin().equals(AttributeOrigin.DATA) || (column.getOrigin().equals(AttributeOrigin.PROPERTY) && column.getIndex() != PropertiesColumn.NODE_ID.getIndex());
         }
     }
 
     private static class NodeRenderer implements RenderDataProvider {
 
         @Override
         public java.awt.Color getBackground(Object o) {
             return null;
         }
 
         @Override
         public String getDisplayName(Object o) {
             return ((ImmutableTreeNode) o).getNode().getNodeData().getLabel();
         }
 
         @Override
         public java.awt.Color getForeground(Object o) {
             return null;
         }
 
         @Override
         public javax.swing.Icon getIcon(Object o) {
             return null;
         }
 
         @Override
         public String getTooltipText(Object o) {
             return "";
         }
 
         @Override
         public boolean isHtmlDisplayName(Object o) {
             return false;
         }
     }
 
     private class PopupAdapter extends MouseUtils.PopupMouseAdapter {
 
         PopupAdapter() {
         }
 
         protected void showPopup(MouseEvent e) {
             int selRow = outlineTable.rowAtPoint(e.getPoint());
 
             if (selRow != -1) {
                 if (!outlineTable.getSelectionModel().isSelectedIndex(selRow)) {
                     outlineTable.getSelectionModel().clearSelection();
                     outlineTable.getSelectionModel().setSelectionInterval(selRow, selRow);
                 }
                 Point p = e.getPoint();
                 if (popupAllowed) {
                     JPopupMenu pop = createPopup(p);
                     showPopup(p.x, p.y, pop);
                 } else {
                     outlineTable.getSelectionModel().clearSelection();
                 }
             }
             e.consume();
         }
 
         private void showPopup(int xpos, int ypos, final JPopupMenu popup) {
             if ((popup != null) && (popup.getSubElements().length > 0)) {
                 final PopupMenuListener p = new PopupMenuListener() {
 
                     public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                     }
 
                     public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                         popup.removePopupMenuListener(this);
                         outlineTable.requestFocus();
                     }
 
                     public void popupMenuCanceled(PopupMenuEvent e) {
                     }
                 };
                 popup.addPopupMenuListener(p);
                 popup.show(outlineTable, xpos, ypos);
             }
         }
 
         private JPopupMenu createPopup(Point p) {
             final Node[] selectedNodes = getNodesFromSelectedRows();
             final Node clickedNode = getNodeFromRow(outlineTable.rowAtPoint(p));
             JPopupMenu contextMenu = new JPopupMenu();
             DataLaboratoryHelper dlh = Lookup.getDefault().lookup(DataLaboratoryHelper.class);
             Integer lastManipulatorType = null;
             for (NodesManipulator nm : dlh.getNodesManipulators()) {
                 nm.setup(selectedNodes, clickedNode);
                 if (lastManipulatorType == null) {
                     lastManipulatorType = nm.getType();
                 }
                 if (lastManipulatorType != nm.getType()) {
                     contextMenu.addSeparator();
                 }
                 lastManipulatorType = nm.getType();
                 contextMenu.add(createMenuItemFromNodesManipulator(nm));
             }
             return contextMenu;
         }
 
         private JMenuItem createMenuItemFromNodesManipulator(final NodesManipulator nm) {
             JMenuItem menuItem = new JMenuItem();
             menuItem.setText(nm.getName());
             if (nm.getDescription() != null && !nm.getDescription().isEmpty()) {
                 menuItem.setToolTipText(nm.getDescription());
             }
             menuItem.setIcon(nm.getIcon());
             if (nm.canExecute()) {
                 menuItem.addActionListener(new ActionListener() {
 
                     public void actionPerformed(ActionEvent e) {
                         DataLaboratoryHelper dlh = Lookup.getDefault().lookup(DataLaboratoryHelper.class);
                         dlh.executeManipulator(nm);
                     }
                 });
             } else {
                 menuItem.setEnabled(false);
             }
             return menuItem;
         }
     }
 
     private Node getNodeFromRow(int rowIndex) {
         int row = outlineTable.convertRowIndexToModel(rowIndex);
         TreePath tp = outlineTable.getLayoutCache().getPathForRow(row);
         if (tp == null) {
             return null;
         }
         ImmutableTreeNode immutableTreeNode = (ImmutableTreeNode) tp.getLastPathComponent();
         return immutableTreeNode.getNode();
     }
 
     public Node[] getNodesFromSelectedRows() {
         int[] selectedRows = outlineTable.getSelectedRows();
         Node[] node = new Node[selectedRows.length];
         for (int i = 0; i < node.length; i++) {
             node[i] = getNodeFromRow(selectedRows[i]);
         }
         return node;
     }
 }
