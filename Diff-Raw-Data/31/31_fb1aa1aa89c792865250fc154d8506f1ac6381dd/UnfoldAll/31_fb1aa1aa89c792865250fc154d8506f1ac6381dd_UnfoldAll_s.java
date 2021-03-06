 /*
  *  Freeplane - mind map editor
  *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
  *
  *  This program is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 2 of the License, or
  *  (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.freeplane.features.map;
 
 import java.awt.event.ActionEvent;
 import java.awt.event.InputEvent;
 import java.awt.event.MouseWheelEvent;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.freeplane.core.ui.AFreeplaneAction;
 import org.freeplane.core.ui.AMultipleNodeAction;
 import org.freeplane.core.ui.IMouseWheelEventHandler;
 import org.freeplane.features.mode.Controller;
 import org.freeplane.features.mode.ModeController;
 
 /**
  * @author foltin
  */
 public class UnfoldAll implements IMouseWheelEventHandler {
 	private class FoldAllAction extends AMultipleNodeAction {
 		private static final long serialVersionUID = 1L;
 
 		public FoldAllAction() {
 			super("FoldAllAction");
 		}
 
 		@Override
 		public void actionPerformed(final ActionEvent e, final NodeModel node) {
 			foldAll(node);
 		}
 	}
 
 	private class FoldOneLevelAction extends AMultipleNodeAction {
 		private static final long serialVersionUID = 1L;
 
 		public FoldOneLevelAction() {
 			super("FoldOneLevelAction");
 		}
 
 		@Override
 		public void actionPerformed(final ActionEvent e, final NodeModel node) {
 			foldOneStage(node);
 		}
 	}
 
 	private class UnfoldAllAction extends AMultipleNodeAction {
 		private static final long serialVersionUID = 1L;
 
 		public UnfoldAllAction() {
 			super("UnfoldAllAction");
 		}
 
 		@Override
 		public void actionPerformed(final ActionEvent e, final NodeModel node) {
 			unfoldAll(node);
 		}
 	}
 
 	private class UnfoldOneLevelAction extends AMultipleNodeAction {
 		private static final long serialVersionUID = 1L;
 
 		public UnfoldOneLevelAction() {
 			super("UnfoldOneLevelAction");
 		}
 
 		@Override
 		public void actionPerformed(final ActionEvent e, final NodeModel node) {
 			unfoldOneStage(node);
 		}
 	}
 
 // // 	final private Controller controller;
 
 	public UnfoldAll() {
 		super();
 		final ModeController modeController = Controller.getCurrentModeController();
 		modeController.getUserInputListenerFactory().addMouseWheelEventHandler(this);
 		for (final AFreeplaneAction annotatedAction : getAnnotatedActions()) {
 			modeController.addAction(annotatedAction);
 		}
 	}
 
 	private List<AMultipleNodeAction> getAnnotatedActions() {
 		final ArrayList<AMultipleNodeAction> result = new ArrayList<AMultipleNodeAction>();
 		result.add(new UnfoldAllAction());
 		result.add(new FoldAllAction());
 		result.add(new UnfoldOneLevelAction());
 		result.add(new FoldOneLevelAction());
 		return result;
 	}
 
 	protected void foldAll(final NodeModel node) {
 		final MapController modeController = Controller.getCurrentModeController().getMapController();
 		for (NodeModel child : modeController.childrenUnfolded(node)) {
 			foldAll(child);
 		}
 		setFolded(node, true);
 	}
 
 	/**
 	 * Unfolds every node that has only children which themselves have children.
 	 * As this function is a bit difficult to describe and perhaps not so
 	 * useful, it is currently not introduced into the menus.
 	 *
 	 * @param node
 	 *            node to start from.
 	 */
 	public void foldLastBranches(final NodeModel node) {
 		final MapController mapController = Controller.getCurrentModeController().getMapController();
 		boolean nodeHasChildWhichIsLeave = false;
 		for (final NodeModel child : mapController.childrenUnfolded(node)) {
 			if (child.getChildCount() == 0) {
 				nodeHasChildWhichIsLeave = true;
 			}
 		}
 		setFolded(node, nodeHasChildWhichIsLeave);
 		for (final NodeModel child : mapController.childrenUnfolded(node)) {
 			foldLastBranches(child);
 		}
 	}
 
 	protected void foldOneStage(final NodeModel node) {
 		foldStageN(node, getMaxDepth(node) - 1);
 	}
 
 	public void foldStageN(final NodeModel node, final int stage) {
		final int k = node.depth();
 		if (k < stage) {
 			setFolded(node, false);
 			final MapController mapController = Controller.getCurrentModeController().getMapController();
 			for (final NodeModel child : mapController.childrenUnfolded(node)) {
 				foldStageN(child, stage);
 			}
 		}
 		else {
 			foldAll(node);
 		}
 	}
 
 	protected int getMaxDepth(final NodeModel node) {
 		final MapController mapController = Controller.getCurrentModeController().getMapController();
 		if (mapController.isFolded(node) || !mapController.hasChildren(node)) {
			return node.depth();
 		}
 		int k = 0;
 		for (final NodeModel child : mapController.childrenUnfolded(node)) {
 			final int l = getMaxDepth(child);
 			if (l > k) {
 				k = l;
 			}
 		}
 		return k;
 	}
 
 	public int getMinDepth(final NodeModel node) {
 		final MapController mapController = Controller.getCurrentModeController().getMapController();
 		if (mapController.isFolded(node)) {
			return node.depth();
 		}
		if (!mapController.hasChildren(node)) {
 			return Integer.MAX_VALUE;
 		}
 		int k = Integer.MAX_VALUE;
 		for (final NodeModel child : mapController.childrenUnfolded(node)) {
 			final int l = getMinDepth(child);
 			if (l < k) {
 				k = l;
 			}
 		}
 		return k;
 	}
 
 	public boolean handleMouseWheelEvent(final MouseWheelEvent e) {
 		if ((e.getModifiers() & InputEvent.ALT_MASK) != 0) {
 			Controller controller = Controller.getCurrentController();
 			final NodeModel rootNode = controller.getMap().getRootNode();
 			if (e.getWheelRotation() > 0) {
 				unfoldOneStage(rootNode);
 			}
 			else {
 				final ModeController modeController = controller.getModeController();
 				modeController.getMapController().select(controller.getMap().getRootNode());
 				foldOneStage(rootNode);
 			}
 			return true;
 		}
 		return false;
 	}
 
 	private void setFolded(final NodeModel node, final boolean state) {
 		final MapController mapController = Controller.getCurrentModeController().getMapController();
 		if (! node.isRoot() && mapController.hasChildren(node) && (mapController.isFolded(node) != state)) {
 			mapController.setFolded(node, state);
 		}
 	}
 
 	public void unfoldAll(final NodeModel node) {
 		setFolded(node, false);
 		final MapController mapController = Controller.getCurrentModeController().getMapController();
 		for (final NodeModel child : mapController.childrenUnfolded(node)) {
 			unfoldAll(child);
 		}
 	}
 
 	protected void unfoldOneStage(final NodeModel node) {
 		int minDepth = getMinDepth(node);
 		if (minDepth < Integer.MAX_VALUE) {
 			minDepth++;
 		}
 		unfoldStageN(node, minDepth);
 	}
 
 	public void unfoldStageN(final NodeModel node, final int stage) {
		final int k = node.depth();
 		if (k < stage) {
 			setFolded(node, false);
 			final MapController mapController = Controller.getCurrentModeController().getMapController();
 			for (final NodeModel child : mapController.childrenUnfolded(node)) {
 				unfoldStageN(child, stage);
 			}
 		}
 		else {
 			foldAll(node);
 		}
 	}
 }
