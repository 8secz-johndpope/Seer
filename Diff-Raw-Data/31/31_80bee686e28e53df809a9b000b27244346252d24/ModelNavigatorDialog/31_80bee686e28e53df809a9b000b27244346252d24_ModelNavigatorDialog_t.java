 /* vim: set ts=2: */
 /**
  * Copyright (c) 2006 The Regents of the University of California.
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *   1. Redistributions of source code must retain the above copyright
  *      notice, this list of conditions, and the following disclaimer.
  *   2. Redistributions in binary form must reproduce the above
  *      copyright notice, this list of conditions, and the following
  *      disclaimer in the documentation and/or other materials provided
  *      with the distribution.
  *   3. Redistributions must acknowledge that this software was
  *      originally developed by the UCSF Computer Graphics Laboratory
  *      under support by the NIH National Center for Research Resources,
  *      grant P41-RR01081.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS BE LIABLE
  * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
  * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
  * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
  * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
  * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
  * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  */
 package edu.ucsf.rbvi.structureViz2.internal.ui;
 
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Frame;
 import java.awt.event.ActionEvent;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 
 import javax.swing.AbstractAction;
 import javax.swing.JDialog;
 import javax.swing.JMenu;
 import javax.swing.JMenuBar;
 import javax.swing.JMenuItem;
 import javax.swing.JOptionPane;
 import javax.swing.JScrollPane;
 import javax.swing.JSeparator;
 import javax.swing.JTree;
 import javax.swing.border.Border;
 import javax.swing.border.LineBorder;
 import javax.swing.event.TreeExpansionEvent;
 import javax.swing.event.TreeExpansionListener;
 import javax.swing.event.TreeSelectionEvent;
 import javax.swing.event.TreeSelectionListener;
 import javax.swing.event.TreeWillExpandListener;
 import javax.swing.tree.DefaultMutableTreeNode;
 import javax.swing.tree.DefaultTreeCellRenderer;
 import javax.swing.tree.ExpandVetoException;
 import javax.swing.tree.TreePath;
 import javax.swing.tree.TreeSelectionModel;
 
 import edu.ucsf.rbvi.structureViz2.internal.model.ChimeraManager;
 import edu.ucsf.rbvi.structureViz2.internal.model.ChimeraModel;
 import edu.ucsf.rbvi.structureViz2.internal.model.ChimeraResidue;
 import edu.ucsf.rbvi.structureViz2.internal.model.ChimeraStructuralObject;
 import edu.ucsf.rbvi.structureViz2.internal.model.ChimeraTreeModel;
 import edu.ucsf.rbvi.structureViz2.internal.model.StructureManager;
 
 /**
  * The ModelNavigatorDialog class is the class that implements the main interface for structureViz.
  */
// TODO: [Bug] Something goes wrong when the tree has been collapsed with selected residues and is
// expanded again
 public class ModelNavigatorDialog extends JDialog implements TreeSelectionListener,
 		TreeExpansionListener, TreeWillExpandListener {
 
 	private static final long serialVersionUID = -1695555416220985117L;
 	// private boolean status;
 	// These must be > ChimeraResidue.FULL_NAME
 	private static final int COMMAND = 10;
 	private static final int EXIT = 11;
 	private static final int REFRESH = 12;
 	private static final int CLEAR = 13;
 	private static final int ALIGNBYMODEL = 14;
 	private static final int ALIGNBYCHAIN = 15;
 	private static final int FINDCLASH = 16;
 	private static final int FINDHBOND = 17;
 	private static final int FUNCTIONALRESIDUES = 18;
 	private static final int COLLAPSEALL = 19;
 	private static final int EXPANDMODELS = 20;
 	private static final int EXPANDCHAINS = 21;
 	private static final int GENERATENETWORK = 22;
 	private static final int SYNCCOLORS = 24;
 	private static final int SELECT = 23;
 	private boolean ignoreDialogSelection = false;
 	private int residueDisplay = ChimeraResidue.THREE_LETTER;
 	private boolean isCollapsing = false;
 	private TreePath collapsingPath = null;
 	// private boolean isExpanding = false;
 	private List<JMenuItem> selectionDependentMenus = null;
 
 	private StructureManager structureManager;
 	private ChimeraManager chimeraManager;
 
 	// Dialog components
 	private JTree navigationTree;
 	private ChimeraTreeModel treeModel;
 	private JMenu alignMenu;
 
 	// public static ModelNavigatorDialog LaunchModelNavigator(Frame parent,
 	// StructureManager structureManager) {
 	// ModelNavigatorDialog mnDialog = new ModelNavigatorDialog(parent,
 	// structureManager);
 	//
 	// if (parent != null) {
 	//
 	// }
 	// mnDialog.setVisible(true);
 	// return mnDialog;
 	// }
 
 	/**
 	 * Create a new ModelNavigatorDialog.
 	 * 
 	 * @param parent
 	 *            the Frame that acts as a parent for the dialog
 	 * @param object
 	 *            the Chimera interface object associated with this dialog
 	 */
 	public ModelNavigatorDialog(Frame parent, StructureManager structureManager) {
 		super(parent);
 		setTitle("Cytoscape Molecular Structure Navigator");
 		this.structureManager = structureManager;
 		this.chimeraManager = structureManager.getChimeraManager();
 		initComponents();
 		if (parent != null) {
 			setLocationRelativeTo(parent);
 		}
 		// status = false;
 	}
 
 	/**
 	 * We lost our connection to Chimera! Tell the user and exit.
 	 */
 	public void lostChimera() {
 		JOptionPane.showMessageDialog(this, "Lost connection to Chimera! structureViz must exit",
 				"Lost connection", JOptionPane.ERROR_MESSAGE);
 	}
 
 	/**
 	 * Call this method when something significant has changed in the model such as a new model
 	 * opened or closed
 	 */
 	public void modelChanged() {
 		// Something significant changed in the model (new open/closed
 		// structure?)
 		ignoreDialogSelection = true;
 		treeModel.reload();
 		int modelCount = chimeraManager.getChimeraModelsCount(false);
 		if (modelCount > 1)
 			alignMenu.setEnabled(true);
 		else
 			alignMenu.setEnabled(false);
 		structureManager.chimeraSelectionChanged();
 		ignoreDialogSelection = false;
 		pack();
 	}
 
 	/**
 	 * This method is called when a tree is expanded
 	 * 
 	 * @param e
 	 *            the TreeExpansionEvent
 	 */
 	public void treeExpanded(TreeExpansionEvent e) {
 		TreePath ePath = e.getPath();
 		// Get the path we are expanding
 		DefaultMutableTreeNode node = (DefaultMutableTreeNode) ePath.getLastPathComponent();
 		if (!(node.getUserObject() instanceof ChimeraStructuralObject))
 			return;
 		ChimeraStructuralObject nodeInfo = (ChimeraStructuralObject) node.getUserObject();
 		// Check and see if our object is selected
 		if (!nodeInfo.isSelected()) {
 			// Its not -- deselect
 			navigationTree.removeSelectionPath(ePath);
 		}
 		// Get the selected children of that path
 		List<ChimeraStructuralObject> children = nodeInfo.getChildren();
 		// Add them to our selection
 		if (children != null) {
 			for (ChimeraStructuralObject o : children) {
 				if (o.isSelected()) {
 					TreePath path = (TreePath) o.getUserData();
 					navigationTree.addSelectionPath(path);
 				}
 			}
 		}
 	}
 
 	/**
 	 * This method is called when a tree is collapsed
 	 * 
 	 * @param e
 	 *            the TreeExpansionEvent
 	 */
 	public void treeCollapsed(TreeExpansionEvent e) {
 		// Sort of a hack. By default when a tree is collapsed,
 		// the selection passes to the parent. We don't what to
 		// do that, because it prevents us from remembering
 		// our residue selections, which may be important.
 		// So, we need to set a flag.
 
 		// Get the path we are collapsing
 		collapsingPath = e.getPath();
 		DefaultMutableTreeNode node = (DefaultMutableTreeNode) collapsingPath
 				.getLastPathComponent();
 		ChimeraStructuralObject nodeInfo = (ChimeraStructuralObject) node.getUserObject();
 
 		// Is the object we're collapsing already selected?
 		if (!nodeInfo.isSelected()) {
 			// No, see if it has selected children
 			if (nodeInfo.hasSelectedChildren()) {
 				// It does, we need to disable selection
 				isCollapsing = true;
 			}
 		}
 	}
 
 	/**
 	 * This method is called before the tree is actually collapsed
 	 * 
 	 * @param e
 	 *            the TreeExpansionEvent
 	 */
 	public void treeWillCollapse(TreeExpansionEvent e) throws ExpandVetoException {
 		TreePath path = e.getPath();
 		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
 		if (!ChimeraStructuralObject.class.isInstance(node.getUserObject()))
 			throw new ExpandVetoException(e);
 		return;
 	}
 
 	/**
 	 * This method is called before the tree is actually expanded
 	 * 
 	 * @param e
 	 *            the TreeExpansionEvent
 	 */
 	public void treeWillExpand(TreeExpansionEvent e) throws ExpandVetoException {
 		return;
 	}
 
 	/**
 	 * This method is called when a path in the tree is selected or deselected
 	 * 
 	 * @param e
 	 *            the TreeSelectionsionEvent
 	 */
 	public void valueChanged(TreeSelectionEvent e) {
 
 		// System.out.println("TreeSelectionEvent: "+e);
 
 		// Get the paths that are changing
 		TreePath[] cPaths = e.getPaths();
 		if (cPaths == null)
 			return;
 
 		if (isCollapsing) {
 			// System.out.println("  isCollapsing: "+cPaths[0]);
 			if (cPaths[0] == collapsingPath) {
 				isCollapsing = false;
 				navigationTree.removeSelectionPath(collapsingPath);
 			}
 			return;
 		}
 
 		// System.out.println("dialog selection changed");
 		for (int i = 0; i < cPaths.length; i++) {
 			DefaultMutableTreeNode node = (DefaultMutableTreeNode) cPaths[i].getLastPathComponent();
 			if (!ChimeraStructuralObject.class.isInstance(node.getUserObject()))
 				continue;
 			ChimeraStructuralObject nodeInfo = (ChimeraStructuralObject) node.getUserObject();
 			if (!e.isAddedPath(cPaths[i])) {
 				// nodeInfo.setSelected(false);
 				structureManager.removeChimSelection(nodeInfo);
 			} else {
 				// nodeInfo.setSelected(true);
 				structureManager.addChimSelection(nodeInfo);
 			}
 			// System.out.println("  Path: "+((DefaultMutableTreeNode)
 			// cPaths[i].getLastPathComponent()));
 		}
 		// Update selection in Chimera
 		if (!ignoreDialogSelection) {
 			structureManager.updateChimeraSelection();
 			// Update selection in Cytoscape
 			structureManager.updateCytoscapeSelection();
 		}
 		// enable only menu items if at least one Chimera object is selected
 		enableMenuItems(structureManager.getChimSelectionCount());
 
 	}
 
 	/**
 	 * This method is called to update the selected items in the navigation tree.
 	 * 
 	 * @param selectionList
 	 *            the List of ChimeraStructuralObjects to be selected
 	 */
 	public void updateSelection(List<ChimeraStructuralObject> selectionList) {
 		// System.out.println("Model Navigator Panel: updateSelection ("+selectionList+")");
 		List<TreePath> pathList = new ArrayList<TreePath>();
 		this.ignoreDialogSelection = true;
 		for (ChimeraStructuralObject selectedObject : selectionList) {
 			pathList.add((TreePath) selectedObject.getUserData());
 		}
 		// Need to clear currently selected objects
 		resetSelectionState(pathList);
 		int row = navigationTree.getMaxSelectionRow();
 		navigationTree.scrollRowToVisible(row);
 		this.ignoreDialogSelection = false;
 	}
 
 	/**
 	 * This method is called to clear the selection state of all ChimeraStructuralObjects. It is
 	 * accomplished by iterating over all models and recursively decending through the chains to the
 	 * residues.
 	 */
 	private void resetSelectionState(List<TreePath> setPaths) {
 		navigationTree.removeSelectionPaths(navigationTree.getSelectionPaths());
 		structureManager.clearSelectionList();
 		// navigationTree.removeSelectionPaths(clearPaths.toArray(new
 		// TreePath[1]));
 		if (setPaths != null && setPaths.size() > 0) {
 			navigationTree.addSelectionPaths(setPaths.toArray(new TreePath[1]));
 		} else {
 			// TODO: [Optional] Collapse tree when no selection
 			// collapseAll();
 		}
 	}
 
 	/*************************************************
 	 * Private methods *
 	 *************************************************/
 
 	/**
 	 * This method initializes all of the graphical components in the dialog.
 	 */
 	private void initComponents() {
 		selectionDependentMenus = new ArrayList<JMenuItem>();
 		// int modelCount =
 		// structureManager.getChimeraManager().getChimeraModelsCount();
 
 		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
 
 		// Initialize the menus
 		JMenuBar menuBar = new JMenuBar();
 
 		// Chimera menu
 		JMenu chimeraMenu = new JMenu("Chimera");
 		alignMenu = new JMenu("Align structures");
 		{
 			// Should this be "model vs. model", "chain vs. chain", and
 			// "chain vs. model"?
 			addMenuItem(alignMenu, "by model", ALIGNBYMODEL, null);
 			addMenuItem(alignMenu, "by chain", ALIGNBYCHAIN, null);
 		}
 
 		int totalModels = chimeraManager.getChimeraModelsCount(false);
 
 		if (totalModels > 1)
 			alignMenu.setEnabled(true);
 		else
 			alignMenu.setEnabled(false);
 		chimeraMenu.add(alignMenu);
 
 		addMenuItem(chimeraMenu, "Focus all", COMMAND, "focus");
 
 		JMenu presetMenu = new JMenu("Presets");
 		if (buildPresetMenu(presetMenu))
 			chimeraMenu.add(presetMenu);
 
 		chimeraMenu.add(new JSeparator());
 
 		JMenu clashMenu = new JMenu("Clash detection");
 		JMenuItem item = addMenuItem(clashMenu, "Find all clashes", FINDCLASH,
 				"findclash sel continuous true");
 		selectionDependentMenus.add(item);
 		item = addMenuItem(clashMenu, "Find clashes within models", FINDCLASH,
 				"findclash sel test model continuous true");
 		selectionDependentMenus.add(item);
 		addMenuItem(clashMenu, "Clear clashes and contacts", COMMAND, "~findclash");
 		chimeraMenu.add(clashMenu);
 
 		JMenu contactMenu = new JMenu("Contact detection");
 		item = addMenuItem(contactMenu, "Find all contacts", FINDCLASH,
 				"findclash sel overlapCutoff -0.4 hbondAllowance 0.0");
 		selectionDependentMenus.add(item);
 		item = addMenuItem(contactMenu, "Find contacts within models", FINDCLASH,
 				"findclash sel test model overlapCutoff -0.4 hbondAllowance 0.0");
 		selectionDependentMenus.add(item);
 		addMenuItem(contactMenu, "Clear clashes and contacts", COMMAND, "~findclash");
 		chimeraMenu.add(contactMenu);
 
 		JMenu hBondMenu = new JMenu("Hydrogen bond detection");
 		JMenu fHBondMenu = new JMenu("Find hydrogen bonds");
 		item = addMenuItem(fHBondMenu, "Between models", FINDHBOND,
 				"findhbond selRestrict any intermodel true intramodel false");
 		selectionDependentMenus.add(item);
 		item = addMenuItem(fHBondMenu, "Within models", FINDHBOND,
 				"findhbond selRestrict any intermodel false intramodel true");
 		selectionDependentMenus.add(item);
 		item = addMenuItem(fHBondMenu, "Both", FINDHBOND,
 				"findhbond selRestrict any intermodel true intramodel true");
 		selectionDependentMenus.add(item);
 		hBondMenu.add(fHBondMenu);
 		addMenuItem(hBondMenu, "Clear hydrogen bonds", COMMAND, "~findhbond");
 		chimeraMenu.add(hBondMenu);
 
 		chimeraMenu.add(new JSeparator());
 		item = addMenuItem(chimeraMenu, "Residue network generation", GENERATENETWORK, null);
 		selectionDependentMenus.add(item);
 		item = addMenuItem(chimeraMenu, "Color synchronization", SYNCCOLORS, null);
 		selectionDependentMenus.add(item);
 
 		chimeraMenu.add(new JSeparator());
 
 		addMenuItem(chimeraMenu, "Exit", EXIT, null);
 		menuBar.add(chimeraMenu);
 
 		// View menu
 		JMenu viewMenu = new JMenu("View");
 		addMenuItem(viewMenu, "Collapse model tree", COLLAPSEALL, null);
 		addMenuItem(viewMenu, "Expand all models", EXPANDMODELS, null);
 		addMenuItem(viewMenu, "Expand all chains", EXPANDCHAINS, null);
 
 		addMenuItem(viewMenu, "Refresh model tree", REFRESH, null);
 
 		JMenu viewResidues = new JMenu("Show residues as..");
 		addMenuItem(viewResidues, "single letter", ChimeraResidue.SINGLE_LETTER, null);
 		addMenuItem(viewResidues, "three letters", ChimeraResidue.THREE_LETTER, null);
 		addMenuItem(viewResidues, "full name", ChimeraResidue.FULL_NAME, null);
 		viewMenu.add(viewResidues);
 		menuBar.add(viewMenu);
 
 		// Select menu
 		JMenu selectMenu = new JMenu("Select");
 		addMenuItem(selectMenu, "Protein", COMMAND, "select protein");
 		addMenuItem(selectMenu, "Nucleic acid", COMMAND, "select nucleic acid");
 		addMenuItem(selectMenu, "Ligand", COMMAND, "select ligand");
 		addMenuItem(selectMenu, "Ions", COMMAND, "select ions");
 		addMenuItem(selectMenu, "Solvent", COMMAND, "select solvent");
 		JMenu secondaryMenu = new JMenu("Secondary structure");
 		addMenuItem(secondaryMenu, "Helix", COMMAND, "select helix");
 		addMenuItem(secondaryMenu, "Strand", COMMAND, "select strand");
 		addMenuItem(secondaryMenu, "Coil", COMMAND, "select coil");
 		selectMenu.add(secondaryMenu);
 		addMenuItem(selectMenu, "Functional residues", FUNCTIONALRESIDUES, null);
 		addMenuItem(selectMenu, "Invert selection", COMMAND, "select invert");
 		addMenuItem(selectMenu, "Clear selection", CLEAR, null);
 		menuBar.add(selectMenu);
 
 		setJMenuBar(menuBar);
 
 		// Initialize the tree
 		navigationTree = new JTree();
 		treeModel = new ChimeraTreeModel(chimeraManager, navigationTree);
 
 		navigationTree.setModel(treeModel);
 		navigationTree.getSelectionModel().setSelectionMode(
 				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
 
 		navigationTree.addTreeSelectionListener(this);
 		navigationTree.addTreeExpansionListener(this);
 		navigationTree.addTreeWillExpandListener(this);
 		navigationTree.setShowsRootHandles(false);
 
 		navigationTree.setCellRenderer(new ObjectRenderer());
 
 		navigationTree.addMouseListener(new DialogPopupMenuListener(structureManager,
 				navigationTree));
 
 		JScrollPane treeView = new JScrollPane(navigationTree);
 
 		setContentPane(treeView);
 
 		enableMenuItems(0);
 	}
 
 	/**
 	 * Add a menu item to a menu
 	 * 
 	 * @param menu
 	 *            the menu to add the item to
 	 * @param label
 	 *            the label for the menu
 	 * @param type
 	 *            the command type
 	 * @param command
 	 *            the command to execute when this item is selected
 	 * @return the JMenuItem that was created
 	 */
 	private JMenuItem addMenuItem(JMenu menu, String label, int type, String command) {
 		JMenuItem menuItem = new JMenuItem(label);
 		{
 			MenuActionListener va = new MenuActionListener(this, type, command);
 			menuItem.addActionListener(va);
 		}
 		menu.add(menuItem);
 		return menuItem;
 	}
 
 	/**
 	 * Build the preset menu by inquiring the list of available presets from Chimera
 	 * 
 	 * @param menu
 	 *            the menu to add the preset items to
 	 * @return false if there are no presets
 	 */
 	private boolean buildPresetMenu(JMenu menu) {
 		List<String> presetList = chimeraManager.getPresets();
 		if (presetList == null || presetList.size() == 0)
 			return false;
 
 		Collections.sort(presetList);
 
 		// OK, now we have the list, create the menu list
 		for (String label : presetList) {
 			String[] com = label.split("[(]");
 			JMenuItem menuItem = new JMenuItem(label);
 			{
 				String command = "preset apply " + com[0];
 				MenuActionListener va = new MenuActionListener(this, SELECT, command);
 				menuItem.addActionListener(va);
 			}
 			menu.add(menuItem);
 		}
 		return true;
 	}
 
 	private void collapseAll() {
 		int row = navigationTree.getRowCount() - 1;
 		while (row >= 1) {
 			navigationTree.collapseRow(row);
 			row--;
 		}
 		return;
 	}
 
 	private void expandModels() {
 		expandTree(2);
 		return;
 	}
 
 	private void expandChains() {
 		expandTree(3);
 		return;
 	}
 
 	private void expandTree(int depth) {
 		int row = navigationTree.getRowCount() - 1;
 		while (row >= 1) {
 			TreePath path = navigationTree.getPathForRow(row);
 			Object[] objArray = path.getPath();
 			if (objArray.length == depth)
 				navigationTree.expandRow(row);
 			row--;
 		}
 		return;
 	}
 
 	private void enableMenuItems(int count) {
 		boolean enable = true;
 		if (count == 0)
 			enable = false;
 		for (JMenuItem item : selectionDependentMenus) {
 			item.setEnabled(enable);
 		}
 	}
 
 	// Embedded classes
 
 	/**
 	 * The MenuActionListener class is used to attach a listener to menu items to handle commands
 	 */
 	private class MenuActionListener extends AbstractAction {
 		int type;
 		String command = null;
 		ModelNavigatorDialog dialog;
 
 		/**
 		 * Create a new MenuActionListener
 		 * 
 		 * @param type
 		 *            the listener type (COMMAND, CLEAR, EXIT, REFRESH, ALIGN, or the residue
 		 *            display type
 		 * @param command
 		 *            the command to execute when this menu is selected
 		 */
 		public MenuActionListener(ModelNavigatorDialog dialog, int type, String command) {
 			this.type = type;
 			this.command = command;
 			this.dialog = dialog;
 		}
 
 		/**
 		 * This method is called when a menu item is selected
 		 * 
 		 * @param ev
 		 *            the ActionEvent for this
 		 */
 		public void actionPerformed(ActionEvent ev) {
 			final int selectedObjectsCount = structureManager.getChimSelectionCount();
 			if (type == COMMAND) {
 				// System.out.println("Command: "+command);
 				chimeraManager.sendChimeraCommand(command, false);
 			} else if (type == SELECT) {
 				chimeraManager.select(command);
 			} else if (type == CLEAR) {
 				chimeraManager.select("~select");
 				collapseAll();
 				navigationTree.removeSelectionPaths(navigationTree.getSelectionPaths());
 			} else if (type == EXIT) {
 				structureManager.exitChimera();
 			} else if (type == FUNCTIONALRESIDUES) {
 				structureManager.selectFunctResidues(chimeraManager.getChimeraModels());
 				// modelChanged();
 			} else if (type == REFRESH) {
 				structureManager.updateModels();
 			} else if (type == COLLAPSEALL) {
 				collapseAll();
 			} else if (type == EXPANDMODELS) {
 				expandModels();
 			} else if (type == EXPANDCHAINS) {
 				expandModels();
 				expandChains();
 			} else if (type == ALIGNBYMODEL) {
 				structureManager.launchAlignDialog(false);
 				modelChanged();
 			} else if (type == ALIGNBYCHAIN) {
 				structureManager.launchAlignDialog(true);
 				modelChanged();
 			} else if (type == FINDCLASH) {
 				if (selectedObjectsCount > 0) {
 					chimeraManager.select(command);
 				} else {
 					JOptionPane.showMessageDialog(dialog,
 							"You must select something to find clashes", "Nothing Selected",
 							JOptionPane.ERROR_MESSAGE);
 				}
 			} else if (type == FINDHBOND) {
 				if (selectedObjectsCount > 0) {
 					chimeraManager.select(command);
 				} else {
 					JOptionPane.showMessageDialog(dialog,
 							"You must select something to find hydrogen bonds", "Nothing Selected",
 							JOptionPane.ERROR_MESSAGE);
 				}
 			} else if (type == GENERATENETWORK) {
 				if (selectedObjectsCount > 0) {
 					structureManager.launchStructureNetworkDialog();
 				} else {
 					JOptionPane.showMessageDialog(dialog,
 							"You must select something to create a network", "Nothing Selected",
 							JOptionPane.ERROR_MESSAGE);
 				}
 			} else if (type == SYNCCOLORS) {
 				if (selectedObjectsCount > 0) {
 					structureManager.getRINManager().syncColors();
 				} else {
 					JOptionPane.showMessageDialog(dialog,
 							"You must select something to syncronize colors", "Nothing Selected",
 							JOptionPane.ERROR_MESSAGE);
 				}
 			} else {
 				residueDisplay = type;
 				treeModel.setResidueDisplay(type);
 				modelChanged();
 			}
 		}
 	}
 
 	/**
 	 * The ObjectRenderer class is used to provide special rendering capabilities for each row of
 	 * the tree. We use this to provide a colored border around the model rows of the tree that
 	 * matches the Chimera model color and to set the color of collapsed models and chains that have
 	 * selected children.
 	 */
 	private class ObjectRenderer extends DefaultTreeCellRenderer {
 
 		/**
 		 * Create a new ObjectRenderer
 		 */
 		public ObjectRenderer() {
 		}
 
 		/**
 		 * This is the method actually called to render the tree cell
 		 * 
 		 * @see DefaultTreeCellRenderer
 		 */
 		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
 				boolean expanded, boolean leaf, int row, boolean hasFocus) {
 			ChimeraStructuralObject chimeraObj = null;
 			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
 			Object object = node.getUserObject();
 			Class objClass = object.getClass();
 			boolean selectIt = sel;
 
 			// Is this a Chimera class?
 			if (ChimeraStructuralObject.class.isInstance(object)) {
 				// Yes, get the object
 				chimeraObj = (ChimeraStructuralObject) object;
 				if (selectIt && !chimeraObj.isSelected())
 					selectIt = false;
 			} else {
 				// No, we don't want to select the root
 				selectIt = false;
 			}
 
 			// Call the DefaultTreeCellRender's method to do most of the work
 			super.getTreeCellRendererComponent(tree, value.toString(), selectIt, expanded, leaf,
 					row, hasFocus);
 
 			// Initialize our border setting
 			setBorder(null);
 			if (chimeraObj != null) {
 				// System.out.println("Sel = "+sel+", "+chimeraObj+".isSelected = "+chimeraObj.isSelected());
 				// Finally, if we're selected, but the underlying object
 				// isn't selected, change the background paint
 				if (sel == false && chimeraObj.hasSelectedChildren() && expanded == false) {
 					Color bg = Color.blue;
 					setForeground(bg);
 				}
 				// If we're a model, use the model color as a border
 				if (chimeraObj.getClass() == ChimeraModel.class) {
 					Color color = ((ChimeraModel) object).getModelColor();
 					if (color != null) {
 						Border border = new LineBorder(color);
 						setBorder(border);
 					}
 				}
 			}
 			return this;
 		}
 	}
 }
