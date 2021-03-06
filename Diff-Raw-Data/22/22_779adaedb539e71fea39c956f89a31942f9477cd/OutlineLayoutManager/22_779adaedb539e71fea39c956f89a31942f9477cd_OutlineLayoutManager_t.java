 /**
  * Copyright (C) 2000, 2001 Maynard Demmon, maynard@organic.com
  * All rights reserved.
  * 
  * Redistribution and use in source and binary forms, with or 
  * without modification, are permitted provided that the 
  * following conditions are met:
  * 
  *  - Redistributions of source code must retain the above copyright 
  *    notice, this list of conditions and the following disclaimer. 
  * 
  *  - Redistributions in binary form must reproduce the above 
  *    copyright notice, this list of conditions and the following 
  *    disclaimer in the documentation and/or other materials provided 
  *    with the distribution. 
  * 
  *  - Neither the names "Java Outline Editor", "JOE" nor the names of its 
  *    contributors may be used to endorse or promote products derived 
  *    from this software without specific prior written permission. 
  * 
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
  * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
  * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
  * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
  * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
  * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
  * POSSIBILITY OF SUCH DAMAGE.
  */
  
 package com.organic.maynard.outliner;
 
 import java.awt.*;
 import java.awt.event.*;
 import java.util.*;
 import javax.swing.*;
 import javax.swing.text.Caret;
 
 import javax.swing.plaf.*;
 import javax.swing.plaf.metal.*;
 
 public class OutlineLayoutManager implements LayoutManager, AdjustmentListener {
 	
 	static {
 		javax.swing.FocusManager.setCurrentManager(new OutlinerFocusManager());
 	}
 	
 	public OutlinerPanel panel = null;
 	public JScrollBar scrollBar = new JScrollBar();
 
 	// Event Listeners
 	private TextKeyListener textListener = new TextKeyListener();
 	private IconKeyListener iconListener = new IconKeyListener();
 	private IndicatorMouseListener indicatorMouseListener = new IndicatorMouseListener();
 	protected InternalDragAndDropListener dndListener = new InternalDragAndDropListener();
 		
 	// Widgit Cache
 	public static final int CACHE_SIZE = Preferences.getPreferenceInt(Preferences.RENDERER_WIDGIT_CACHE_SIZE).cur;
 	public OutlinerCellRendererImpl[] textAreas = new OutlinerCellRendererImpl[CACHE_SIZE];
 	
 	// GUI Components for handling offscreen focus events.
 	private OutlinerCellRendererImpl hiddenCell = new OutlinerCellRendererImpl();
 	
 	public OutlinerCellRendererImpl getHiddenCell() {return hiddenCell;}
 	public void setHiddenCell(OutlinerCellRendererImpl hiddenCell) {this.hiddenCell = hiddenCell;}
 	
 
 	// The Constructors
 	public OutlineLayoutManager(OutlinerPanel panel) {
 		this.panel = panel;
 
 		// Setup and Add the ScrollBar
 		scrollBar.addAdjustmentListener(this); 
 		panel.add(scrollBar);
 		scrollBar.setVisible(true);
 		
 		// Initialize the Widgits
 		for (int i = 0; i < CACHE_SIZE; i++) {
 			OutlinerCellRendererImpl renderer = new OutlinerCellRendererImpl();
 			panel.add(renderer);
 			renderer.addKeyListener(textListener);
 			renderer.addMouseListener(textListener);
 			renderer.addMouseListener(dndListener);
 			textAreas[i] = renderer;
 			
 			panel.add(renderer.button);
 			renderer.button.addKeyListener(iconListener);
 			renderer.button.addMouseListener(iconListener);
 			renderer.button.addMouseListener(dndListener);
 			
 			panel.add(renderer.lineNumber);
 			renderer.lineNumber.addKeyListener(iconListener);
 			renderer.lineNumber.addMouseListener(iconListener);
 			renderer.lineNumber.addMouseListener(dndListener);
 			
 			panel.add(renderer.iComment);
 			renderer.iComment.addMouseListener(indicatorMouseListener);
 
 			panel.add(renderer.iEditable);
 			renderer.iEditable.addMouseListener(indicatorMouseListener);
 
 			panel.add(renderer.iMoveable);
 			renderer.iMoveable.addMouseListener(indicatorMouseListener);
 		}
 		
 		// Initialized the hidden components
 		hiddenCell.addKeyListener(textListener);
 		hiddenCell.addMouseListener(textListener);
 		panel.add(hiddenCell);
 		hiddenCell.button.addKeyListener(iconListener);
 		hiddenCell.button.addMouseListener(iconListener);
 		panel.add(hiddenCell.button);
 	}
 	
 	public void destroy() {
 		panel = null;
 		scrollBar.removeAdjustmentListener(this);
 		scrollBar = null;
 		
 		for (int i = 0; i < CACHE_SIZE; i++) {
 			textAreas[i].button.removeMouseListener(dndListener);
 			textAreas[i].button.removeKeyListener(iconListener);
 			textAreas[i].button.removeMouseListener(iconListener);
 			textAreas[i].button.destroy();
 
 			textAreas[i].lineNumber.removeMouseListener(dndListener);
 			textAreas[i].lineNumber.removeKeyListener(iconListener);
 			textAreas[i].lineNumber.removeMouseListener(iconListener);
 			textAreas[i].lineNumber.destroy();
 			
 			textAreas[i].iComment.removeMouseListener(dndListener);
 			textAreas[i].iComment.destroy();
 
 			textAreas[i].iEditable.removeMouseListener(dndListener);
 			textAreas[i].iEditable.destroy();
 
 			textAreas[i].iMoveable.removeMouseListener(dndListener);
 			textAreas[i].iMoveable.destroy();
 
 			textAreas[i].removeMouseListener(dndListener);
 			textAreas[i].removeKeyListener(textListener);
 			textAreas[i].removeMouseListener(textListener);
 			textAreas[i].destroy();
 		}
 		
 		textAreas = null;
 		
 		hiddenCell.destroy();
 		hiddenCell = null;
 		
 		dndListener.destroy();
 		dndListener = null;
 		
 		textListener.destroy();
 		textListener = null;
 		
 		iconListener.destroy();
 		iconListener = null;
 		
 		nodeToDrawFrom = null;
 	}
 
 	// Node to Draw From
 	private Node nodeToDrawFrom = null;
 	private int ioNodeToDrawFrom = 0;
 	
 	public void setNodeToDrawFrom(Node nodeToDrawFrom, int ioNodeToDrawFrom) {
 		//System.out.println("Node to Draw From: " + nodeToDrawFrom.getValue() + ":" + ioNodeToDrawFrom);
 		this.nodeToDrawFrom = nodeToDrawFrom;
 		this.ioNodeToDrawFrom = ioNodeToDrawFrom;
 	}
 	
 	public void updateNodeToDrawFrom() {
 		ioNodeToDrawFrom = panel.doc.tree.visibleNodes.indexOf(nodeToDrawFrom);
 	}
 	
 	public Node getNodeToDrawFrom() {return this.nodeToDrawFrom;}
 	public int getIndexOfNodeToDrawFrom() {return ioNodeToDrawFrom;}
 
 
 	// Drawing Direction
 	private static final int UP = 1;
 	private static final int DOWN = 2;
 	private int drawingDirection = DOWN;
 
 	
 	// Main Drawing Methods
 	private int numNodesDrawn = 1;
 	private int ioFirstVisNode = 0;
 	private int ioLastVisNode = 0;
 	
 	private boolean partialCellDrawn = false;
 	private static Point startPoint = new Point(0,0);
 	
 	public void redraw() {
 		draw();
 		setFocus(panel.doc.tree.getEditingNode(), panel.doc.tree.getComponentFocus());
 	}
 	
 	public void draw(Node nodeThatMustBeVis, int focusElement) {
 		draw(nodeThatMustBeVis, panel.doc.tree.visibleNodes.indexOf(nodeThatMustBeVis), focusElement);
 	}
 		
 	public void draw(Node nodeThatMustBeVis, int ioNodeThatMustBeVis, int focusElement) {
 		if (ioNodeThatMustBeVis <= ioFirstVisNode) {
 			drawingDirection = DOWN;
 			setNodeToDrawFrom(nodeThatMustBeVis, ioNodeThatMustBeVis);
 		} else if (ioNodeThatMustBeVis >= ioLastVisNode) {
 			drawingDirection = UP;
 			setNodeToDrawFrom(nodeThatMustBeVis, ioNodeThatMustBeVis);
 		}
 		
 		draw();
 		setFocus(nodeThatMustBeVis,focusElement);
 	}
 	
 	//public static int drawCount = 0;
 	
 	public void draw() {
 		//System.out.println("Draw Called: " + drawCount++);
 		numNodesDrawn = 0;
 		
 		// Pre-store some values from the preferences so we don't have to get them once for every renderer.
 		OutlinerCellRendererImpl.pIndent = Preferences.getPreferenceInt(Preferences.INDENT).cur;
 		OutlinerCellRendererImpl.pVerticalSpacing = Preferences.getPreferenceInt(Preferences.VERTICAL_SPACING).cur;
 		OutlinerCellRendererImpl.pShowLineNumbers = Preferences.getPreferenceBoolean(Preferences.SHOW_LINE_NUMBERS).cur;
 		
 		if (panel.doc.settings.useDocumentSettings) {
 			OutlinerCellRendererImpl.pApplyFontStyleForComments = panel.doc.settings.applyFontStyleForComments.cur;
 			OutlinerCellRendererImpl.pApplyFontStyleForEditability = panel.doc.settings.applyFontStyleForEditability.cur;
 			OutlinerCellRendererImpl.pApplyFontStyleForMoveability = panel.doc.settings.applyFontStyleForMoveability.cur;
 		} else {
 			OutlinerCellRendererImpl.pApplyFontStyleForComments = Preferences.getPreferenceBoolean(Preferences.APPLY_FONT_STYLE_FOR_COMMENTS).cur;
 			OutlinerCellRendererImpl.pApplyFontStyleForEditability = Preferences.getPreferenceBoolean(Preferences.APPLY_FONT_STYLE_FOR_EDITABILITY).cur;
 			OutlinerCellRendererImpl.pApplyFontStyleForMoveability = Preferences.getPreferenceBoolean(Preferences.APPLY_FONT_STYLE_FOR_MOVEABILITY).cur;
 		}
 		
 		OutlinerCellRendererImpl.pCommentColor = Preferences.getPreferenceColor(Preferences.TEXTAREA_COMMENT_COLOR).cur;				
 		OutlinerCellRendererImpl.pForegroundColor = Preferences.getPreferenceColor(Preferences.TEXTAREA_FOREGROUND_COLOR).cur;
 		OutlinerCellRendererImpl.pBackgroundColor = Preferences.getPreferenceColor(Preferences.TEXTAREA_BACKGROUND_COLOR).cur;
 		OutlinerCellRendererImpl.pSelectedChildColor = Preferences.getPreferenceColor(Preferences.SELECTED_CHILD_COLOR).cur;
 		OutlinerCellRendererImpl.pLineNumberColor = Preferences.getPreferenceColor(Preferences.LINE_NUMBER_COLOR).cur;
 		OutlinerCellRendererImpl.pLineNumberSelectedColor = Preferences.getPreferenceColor(Preferences.LINE_NUMBER_SELECTED_COLOR).cur;
 		OutlinerCellRendererImpl.pLineNumberSelectedChildColor = Preferences.getPreferenceColor(Preferences.LINE_NUMBER_SELECTED_CHILD_COLOR).cur;
 					
 		// Pre-compute some values so we don't have to do them once for every renderer.
 		OutlinerCellRendererImpl.moveableOffset = Preferences.getPreferenceInt(Preferences.LEFT_MARGIN).cur; // equiv to left margin
 		OutlinerCellRendererImpl.editableOffset = OutlinerCellRendererImpl.moveableOffset + OutlineMoveableIndicator.BUTTON_WIDTH;
 		OutlinerCellRendererImpl.commentOffset = OutlinerCellRendererImpl.editableOffset + OutlineEditableIndicator.BUTTON_WIDTH;
 		OutlinerCellRendererImpl.lineNumberOffset = OutlinerCellRendererImpl.commentOffset + OutlineCommentIndicator.BUTTON_WIDTH;
 		
 		OutlinerCellRendererImpl.bestHeightComparison = 
 		Math.max(
 			Math.max(
 				Math.max(
 					Math.max(
 						OutlineMoveableIndicator.BUTTON_HEIGHT, 
 					OutlineLineNumber.LINE_NUMBER_HEIGHT), 
 				OutlineCommentIndicator.BUTTON_HEIGHT), 
 			OutlineEditableIndicator.BUTTON_HEIGHT), 
 		OutlineMoveableIndicator.BUTTON_HEIGHT);
 
		OutlinerCellRendererImpl.textAreaWidth = 
			panel.getParent().getWidth()
 			 - OutlinerCellRendererImpl.lineNumberOffset 
 			 - OutlineLineNumber.LINE_NUMBER_WIDTH 
 			 - OutlineButton.BUTTON_WIDTH
 			 - Preferences.getPreferenceInt(Preferences.RIGHT_MARGIN).cur 
			 - scrollBar.getWidth();			 
 
 		// Draw the visible components
 
 			// Hide all the nodes from the previous draw
 			for (int i = 0; i < CACHE_SIZE; i++) {
 				if (textAreas[i].node != null) {
 					textAreas[i].node.setVisible(false);
 				}
 			}
 
 			startPoint.x = OutlinerCellRendererImpl.lineNumberOffset + OutlineLineNumber.LINE_NUMBER_WIDTH;
 			
 			switch (drawingDirection) {
 				case DOWN:
 					drawDown();
 					break;
 				default:
 					drawUp();
 			}
 		
 		// Draw the hidden component so that things work when we scroll away from the editing node.
 		startPoint.x = this.left;
 		startPoint.y = this.bottom + 16;
 		getHiddenCell().drawDown(startPoint, panel.doc.tree.getEditingNode());
 		
 		// Update the scrollbar
 		drawBlock = true;
 		scrollBar.setValues(ioFirstVisNode, numNodesDrawn, 0, panel.doc.tree.visibleNodes.size());
 		drawBlock = false;
 		
 		return;
 	}
 
 	private void drawDown() {
 		// Now Draw as many nodes as neccessary.
 		startPoint.y = Preferences.getPreferenceInt(Preferences.TOP_MARGIN).cur;
 		
 		Node node = getNodeToDrawFrom();
 		if (node == null) {return;}
 		
 		int nodeIndex = ioNodeToDrawFrom;
 		ioFirstVisNode = ioNodeToDrawFrom;
 		
 		// Pre-compute some values
 		int effectiveBottom = bottom - Preferences.getPreferenceInt(Preferences.BOTTOM_MARGIN).cur;
 		
 		// Increment the LineCountKey
 		node.getTree().incrementLineCountKey();
 		
 		OutlinerCellRendererImpl renderer;
 		while (true) {
 			renderer = textAreas[numNodesDrawn];
 			renderer.drawDown(startPoint, node);
 			renderer.setVisible(true);
 			renderer.node.setVisible(true);
 			numNodesDrawn++;
 			
 			// Make sure we don't draw past the bottom. And don't count nodes that are partially drawn.
 			if (startPoint.y > effectiveBottom) {
 				renderer.node.setVisible(false);
 				partialCellDrawn = true;
 				break;
 			}
 
 			// Make sure we dont' try to draw more nodes than the cache size
 			if (numNodesDrawn == CACHE_SIZE) {
 				break;
 			}
 			
 			// Get the Next Node to Draw
 			nodeIndex++;
 			if (nodeIndex == panel.doc.tree.visibleNodes.size()) {
 				break;
 			}
 			node = panel.doc.tree.visibleNodes.get(nodeIndex);
 		}
 
 		// Hide any drawing elements that were not used.
 		for (int i = numNodesDrawn; i < CACHE_SIZE; i++) {
 			textAreas[i].setVisible(false);
 		}
 
 		// Record Indexes and get things ready for the scrollbar
 		if (partialCellDrawn) {
 			numNodesDrawn--;
 			partialCellDrawn = false;
 		}
 
 		ioLastVisNode = ioFirstVisNode + (numNodesDrawn - 1);
 		if (ioLastVisNode >= panel.doc.tree.visibleNodes.size()) {
 			ioLastVisNode = panel.doc.tree.visibleNodes.size() - 1;
 		}
 	}
 	
 	private void drawUp() {
 		// Now Draw as many nodes as neccessary.
 		startPoint.y = this.bottom - Preferences.getPreferenceInt(Preferences.BOTTOM_MARGIN).cur;
 
 		Node node = getNodeToDrawFrom();
 		if (node == null) {return;}
 
 		int nodeIndex = ioNodeToDrawFrom;
 		ioLastVisNode = ioNodeToDrawFrom;
 
 		// Pre-compute some values
 		int effectiveTop = top + Preferences.getPreferenceInt(Preferences.TOP_MARGIN).cur;
 
 		// Increment the LineCountKey
 		node.getTree().incrementLineCountKey();
 
 		Node newNodeToDrawFrom = null;
 		int ioNewNodeToDrawFrom = nodeIndex;
 		int offset = 0;
 		
 		OutlinerCellRendererImpl renderer;
 		while (true) {
 			renderer = textAreas[numNodesDrawn];
 			renderer.drawUp(startPoint, node);
 			renderer.setVisible(true);
 			renderer.node.setVisible(true);
 			numNodesDrawn++;
 
 			// Make sure we don't draw past the top. And don't count nodes that are partially drawn.
 			if (startPoint.y < effectiveTop) {
 				renderer.node.setVisible(false);
 				partialCellDrawn = true;
 				break;
 			}
 
 			newNodeToDrawFrom = node;
 			ioNewNodeToDrawFrom = nodeIndex;
 			offset = startPoint.y;
 			
 			// Make sure we dont' try to draw more nodes than the cache size
 			if (numNodesDrawn == CACHE_SIZE) {
 				break;
 			}
 
 			// Get the Next Node to Draw
 			nodeIndex--;
 			if (nodeIndex == -1) {
 				break;
 			}
 			node = panel.doc.tree.visibleNodes.get(nodeIndex);
 		}
 
 		// Hide any drawing elements that were not used.
 		for (int i = numNodesDrawn; i < CACHE_SIZE; i++) {
 			textAreas[i].setVisible(false);
 		}
 
 		// Record some values for the extra draw down.
 		int ioExtraNodeToDrawFrom = ioNodeToDrawFrom + 1;
 		
 		// Shift up so we are always drawing from the top
 		setNodeToDrawFrom(newNodeToDrawFrom, ioNewNodeToDrawFrom);
 
 		int shiftAmount = effectiveTop - offset;
 		for (int i = 0; i < numNodesDrawn; i++) {
 			textAreas[i].verticalShift(shiftAmount);
 		}
 		
 		// Record Indexes and get things ready for the scrollbar
 		if (partialCellDrawn) {
 			numNodesDrawn--;
 			partialCellDrawn = false;
 		}
 		
 		ioFirstVisNode = ioLastVisNode - (numNodesDrawn - 1);
 		if (ioLastVisNode >= panel.doc.tree.visibleNodes.size()) {
 			ioLastVisNode = panel.doc.tree.visibleNodes.size() - 1;
 		}
 
 		drawingDirection = DOWN;
 		
 		// Do the extraDrawDown
 		drawDownExtraNodes(ioExtraNodeToDrawFrom);
 	}
 
 	private void drawDownExtraNodes(int nodeIndex) {
 		Node node = null;
 		try {
 			node = panel.doc.tree.visibleNodes.get(nodeIndex);
 		} catch (IndexOutOfBoundsException e) {
 			return;
 		}
 
 		if (node == null) {
 			return;
 		}
 		
 		startPoint.x = OutlinerCellRendererImpl.lineNumberOffset + OutlineLineNumber.LINE_NUMBER_WIDTH;
 		startPoint.y = textAreas[0].getLocation().y + textAreas[0].getBestHeight() + Preferences.getPreferenceInt(Preferences.VERTICAL_SPACING).cur;
 		
 		// Pre-compute some values
 		int effectiveBottom = bottom - Preferences.getPreferenceInt(Preferences.BOTTOM_MARGIN).cur;
 		
 		OutlinerCellRendererImpl renderer;
 		while (true) {
 			renderer = textAreas[numNodesDrawn];
 			renderer.drawDown(startPoint, node);
 			renderer.setVisible(true);
 			renderer.node.setVisible(true);
 			numNodesDrawn++;
 			
 			// Make sure we don't draw past the bottom. And don't count nodes that are partially drawn.
 			if (startPoint.y > effectiveBottom) {
 				renderer.node.setVisible(false);
 				partialCellDrawn = true;
 				break;
 			}
 
 			// Make sure we dont' try to draw more nodes than the cache size
 			if (numNodesDrawn == CACHE_SIZE) {
 				break;
 			}
 			
 			// Get the Next Node to Draw
 			nodeIndex++;
 			if (nodeIndex == panel.doc.tree.visibleNodes.size()) {
 				break;
 			}
 			node = panel.doc.tree.visibleNodes.get(nodeIndex);
 		}
 
 		// Hide any drawing elements that were not used.
 		// If we start using this method anywhere but from drawDown then we may want this code back in.
 		//for (int i = numNodesDrawn; i < CACHE_SIZE; i++) {
 		//	textAreas[i].setVisible(false);
 		//}
 
 		// Record Indexes and get things ready for the scrollbar
 		if (partialCellDrawn) {
 			numNodesDrawn--;
 			partialCellDrawn = false;
 		}
 		
 		ioLastVisNode = ioFirstVisNode + (numNodesDrawn - 1);		
 		if (ioLastVisNode >= panel.doc.tree.visibleNodes.size()) {
 			ioLastVisNode = panel.doc.tree.visibleNodes.size() - 1;
 		}
 	}
 	
 	
 	// Get UI Components
 	public OutlinerCellRendererImpl getUIComponent(Node node) {
 		for (int i = 0; i < CACHE_SIZE; i++) {
 			if ((textAreas[i].node == node) && node.isVisible()) {
 				return textAreas[i];
 			}			
 		}
 		
 		if (getHiddenCell().node == node) {
 			return getHiddenCell();
 		}
 		
 		return null;
 	}
 	
 	// Focus Methods
 	public static final int TEXT = 0;
 	public static final int ICON = 1;
 
 	public void setFocus(Node node, int type) {
 		OutlinerCellRendererImpl renderer = getUIComponent(node);
 		
 		if (renderer == null) {
 			System.out.println("Focus Exception: No renderer found for node.");
 			return;
 		}
 		
 		switch (type) {
 			case TEXT:
 				renderer.requestFocus();
 				renderer.getCaret().setVisible(true); // This fixes a problem where the caret becomes invisible when it shouldn't be.
 				
 				// Restore the Caret Position and text selection.
 				try {
 					renderer.setCaretPosition(panel.doc.tree.getCursorMarkPosition());
 					renderer.moveCaretPosition(panel.doc.tree.getCursorPosition());
 				} catch (Exception e) {
 					System.out.println("Focus Exception: " + e);
 				}
 				break;
 			case ICON:
 				renderer.button.requestFocus();
 				break;
 			default:
 				System.out.println("Focus Error: Not ICON or TEXT");
 		}
 	}
 
 
 	// AdjustmentListener Interface
 	private boolean drawBlock = false;
 	
 	public void adjustmentValueChanged(AdjustmentEvent e) {
 		if (drawBlock) {return;}
 		
 		// Explicit call to draw and focus, so that we can scroll away from our current component focus.
 		setNodeToDrawFrom(panel.doc.tree.visibleNodes.get(e.getValue()), e.getValue());		
 		drawingDirection = DOWN;
 		redraw();
 	}
 	
 	
 	// LayoutManager Interface
 	private int top = 0;
 	private int bottom = 0;
 	private int left = 0;
 	private int right = 0;
 	
 	public void layoutContainer(Container container) {
 		Insets insets = panel.getInsets();
 		this.top = insets.top;
 		this.bottom = panel.getSize().height - insets.bottom;
 		this.left = insets.left;
 		this.right = panel.getSize().width - insets.right;
 		
 		// Update the scrollbar size
 		Dimension d = scrollBar.getPreferredSize();
 		scrollBar.setBounds(right - d.width, top, d.width, bottom - top);
 	}
 
 	public Dimension minimumLayoutSize(Container parent) {return new Dimension(0,32);}
 	public Dimension preferredLayoutSize(Container parent) {return parent.getParent().getSize();} // Need to get parent because we've got the DummyJScrollPane between us and our parent window which has the size we want.
 	public void addLayoutComponent(String name, Component comp) {}
 	public void removeLayoutComponent(Component comp) {}
 }
