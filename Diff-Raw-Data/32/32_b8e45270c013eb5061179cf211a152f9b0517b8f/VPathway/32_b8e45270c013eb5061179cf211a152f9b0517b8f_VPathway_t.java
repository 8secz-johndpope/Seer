 // PathVisio,
 // a tool for data visualization and analysis using Biological Pathways
 // Copyright 2006-2007 BiGCaT Bioinformatics
 //
 // Licensed under the Apache License, Version 2.0 (the "License"); 
 // you may not use this file except in compliance with the License. 
 // You may obtain a copy of the License at 
 // 
 // http://www.apache.org/licenses/LICENSE-2.0 
 //  
 // Unless required by applicable law or agreed to in writing, software 
 // distributed under the License is distributed on an "AS IS" BASIS, 
 // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 // See the License for the specific language governing permissions and 
 // limitations under the License.
 //
 package org.pathvisio.view;
 
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.Graphics2D;
 import java.awt.Point;
 import java.awt.Rectangle;
 import java.awt.RenderingHints;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.Point2D;
 import java.awt.geom.Rectangle2D;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.swing.Action;
 import javax.swing.KeyStroke;
 
 import org.pathvisio.debug.Logger;
 import org.pathvisio.model.GroupStyle;
 import org.pathvisio.model.ObjectType;
 import org.pathvisio.model.Pathway;
 import org.pathvisio.model.PathwayElement;
 import org.pathvisio.model.PathwayEvent;
 import org.pathvisio.model.PathwayListener;
 import org.pathvisio.model.GraphLink.GraphIdContainer;
 import org.pathvisio.model.Pathway.StatusFlagEvent;
 import org.pathvisio.model.PathwayElement.MAnchor;
 import org.pathvisio.model.PathwayElement.MPoint;
 import org.pathvisio.preferences.GlobalPreference;
 import org.pathvisio.view.SelectionBox.SelectionListener;
 import org.pathvisio.view.ViewActions.KeyMoveAction;
 
 /**
  * This class implements and handles a drawing. Graphics objects are stored in
  * the drawing and can be visualized. The class also provides methods for mouse
  * and key event handling.
  */
 public class VPathway implements PathwayListener
 {
 	static final int ZORDER_SELECTIONBOX = Integer.MAX_VALUE;
 	static final int ZORDER_HANDLE = Integer.MAX_VALUE - 1;
 	static final int ZORDER_POINT = Integer.MAX_VALUE - 2;
 
 	private static final long serialVersionUID = 1L;
 
 	static final double M_PASTE_OFFSET = 10 * 15;
 
 	static final int SMALL_INCREMENT = 2;
 
 	private boolean selectionEnabled = true;
 
 	private Pathway temporaryCopy = null;
 	
 	/**
 	 * Retuns true if snap to anchors is enabled
 	 */
 	public boolean isSnapToAnchors() {
 		return GlobalPreference.getValueBoolean(GlobalPreference.SNAP_TO_ANCHOR);
 	}
 
 	/**
 	 * Returns true if the selection capability of this VPathway is enabled
 	 */
 	public boolean getSelectionEnabled()
 	{
 		return selectionEnabled;
 	}
 
 	/**
 	 * You can disable the selection capability of this VPathway by passing
 	 * false. This is not used within Pathvisio, but it is meant for embedding
 	 * VPathway in other applications, where selections may not be needed.
 	 */
 	public void setSelectionEnabled(boolean value)
 	{
 		selectionEnabled = value;
 	}
 
 	private VPathwayWrapper parent; // may be null
 
 	/**
 	 * All objects that are visible on this mapp, including the handles but
 	 * excluding the legend, mappInfo and selectionBox objects
 	 */
 	private ArrayList<VPathwayElement> drawingObjects;
 
 	public ArrayList<VPathwayElement> getDrawingObjects()
 	{
 		return drawingObjects;
 	}
 
 	/**
 	 * The {@link VPathwayElement} that is pressed last mouseDown event}
 	 */
 	VPathwayElement pressedObject = null;
 
 	/**
 	 * The {@link Graphics} that is directly selected since last mouseDown event
 	 */
 	public Graphics selectedGraphics = null;
 
 	/**
 	 * {@link InfoBox} object that contains information about this pathway,
 	 * currently only used for information in {@link gmmlVision.PropertyPanel}
 	 * (TODO: has to be implemented to behave the same as any Graphics object
 	 * when displayed on the drawing)
 	 */
 	InfoBox infoBox;
 
 	private Pathway data;
 
 	/**
 	 * @deprecated. use getPathwayModel() instead
 	 */
 	public Pathway getGmmlData()
 	{
 		return data;
 	}
 	
 	public Pathway getPathwayModel()
 	{
 		return data;
 	}
 
 	SelectionBox selection;
 
 	private boolean editMode = true;
 
 	/**
 	 * Checks if this drawing is in edit mode
 	 * 
 	 * @return false if in edit mode, true if not
 	 */
 	public boolean isEditMode()
 	{
 		return editMode;
 	}
 
 	/**
 	 * Constructor for this class.
 	 * 
 	 * @param parent
 	 *            Optional gui-specific wrapper for this VPathway
 	 */
 	public VPathway(VPathwayWrapper parent)
 	{
 		this.parent = parent == null ? new VPathwayWrapperBase() : parent;
 
 		drawingObjects = new ArrayList<VPathwayElement>();
 
 		selection = new SelectionBox(this);
 
 		registerKeyboardActions();
 	}
 
 	public void redraw()
 	{
 		if (parent != null)
 			parent.redraw();
 	}
 
 	public VPathwayWrapper getWrapper()
 	{
 		return parent;
 	}
 
 	/**
 	 * Map the contents of a single data object to this VPathway
 	 */
 	private Graphics fromGmmlDataObject(PathwayElement o)
 	{
 		Graphics result = null;
 		switch (o.getObjectType())
 		{
 		case ObjectType.DATANODE:
 			result = new GeneProduct(this, o);
 			break;
 		case ObjectType.SHAPE:
 			result = new Shape(this, o);
 			break;
 		case ObjectType.LINE:
 			result = new Line(this, o);
 			break;
 		case ObjectType.MAPPINFO:
 			InfoBox mi = new InfoBox(this, o);
 			result = mi;
 			break;
 		case ObjectType.LABEL:
 			result = new Label(this, o);
 			break;
 		case ObjectType.GROUP:
 			result = new Group(this, o);
 			break;
 		}
 		return result;
 	}
 
 	/**
 	 * used by undo manager.
 	 */
 	public void replacePathway(Pathway originalState)
 	{
 		if(data.hasChanged() != originalState.hasChanged()) {
 			data.fireStatusFlagEvent(new StatusFlagEvent(originalState.hasChanged()));
 		}
 		clearSelection();
 		drawingObjects = new ArrayList<VPathwayElement>();
 		List<SelectionListener> selectionListeners = selection.getListeners();
 		selection = new SelectionBox(this);
 		for(SelectionListener l : selectionListeners) {
 			selection.addListener(l);
 		}
 		pressedObject = null;
 		selectedGraphics = null;
 		data = null;
 		pointsMtoV = new HashMap<MPoint, VPoint>();
 		fromGmmlData(originalState);
 	}
 
 	/**
 	 * Maps the contents of a pathway to this VPathway
 	 */
 	public void fromGmmlData(Pathway _data)
 	{
 		Logger.log.trace("Create view structure");
 
 		data = _data;
 		for (PathwayElement o : data.getDataObjects())
 		{
 			fromGmmlDataObject(o);
 		}
 		double[] calcSize = data.getMappInfo().getMBoardSize();
 		int width = (int) vFromM(calcSize[0]);
 		int height = (int) vFromM(calcSize[1]);
 		parent.setVSize(width, height);
 
 		// data.fireObjectModifiedEvent(new PathwayEvent(null,
 		// PathwayEvent.MODIFIED_GENERAL));
 		fireVPathwayEvent(new VPathwayEvent(this, VPathwayEvent.MODEL_LOADED));
 		data.addListener(this);
 		undoManager.setPathway(data);
 		Logger.log.trace("Done creating view structure");
 	}
 
 	Template newTemplate = null;
 	
 	/**
 	 * Method to set the template that provides the new graphics type that has 
 	 * to be added next time the user clicks on the drawing.
 	 * 
 	 * @param shape A template that provides the elements to be added
 	 */
 	public void setNewTemplate(Template t) {
 		newTemplate = t;
 	}
 	
 	private Rectangle dirtyRect = null;
 
 	/**
 	 * Adds object boundaries to the 'dirty rectangle', which marks the area
 	 * that needs to be redrawn
 	 */
 	public void addDirtyRect(Rectangle r)
 	{
 		if (r == null)
 		{ // In case r is null, add whole drawing
 			if (parent != null)
 			{
 				r = parent.getVBounds();
 			}
 		}
 		if (dirtyRect == null)
 			dirtyRect = r;
 		else
 			dirtyRect.add(r);
 	}
 
 	public void addDirtyRect(Rectangle2D r)
 	{
 		addDirtyRect(r.getBounds());
 	}
 
 	/**
 	 * Redraw parts marked dirty reset dirty rect afterwards
 	 */
 	public void redrawDirtyRect()
 	{
 		if (dirtyRect != null && parent != null)
 			parent.redraw(dirtyRect);
 		dirtyRect = null;
 	}
 
 	/**
 	 * Sets the MappInfo containing information on the pathway
 	 * 
 	 * @param mappInfo
 	 */
 	public void setMappInfo(InfoBox mappInfo)
 	{
 		this.infoBox = mappInfo;
 		infoBox.getPathwayElement().addListener(this);
 	}
 
 	/**
 	 * Gets the MappInfo containing information on the pathway
 	 */
 	public InfoBox getMappInfo()
 	{
 		return infoBox;
 	}
 
 	/**
 	 * Adds an element to the drawing
 	 * 
 	 * @param o
 	 *            the element to add
 	 */
 	public void addObject(VPathwayElement o)
 	{
 		if (!drawingObjects.contains(o))
 		{ // Don't add duplicates!
 			drawingObjects.add(o);
 		}
 	}
 
 	/**
 	 * Gets the view representation {@link Graphics} of the given model element
 	 * {@link PathwayElement}
 	 * 
 	 * @param e
 	 * @return the {@link Graphics} representing the given
 	 *         {@link PathwayElement} or <code>null</code> if no view is
 	 *         available
 	 */
 	public Graphics getPathwayElementView(PathwayElement e)
 	{
 		// TODO: store Graphics in a hashmap to improve speed
 		for (VPathwayElement ve : drawingObjects)
 		{
 			if (ve instanceof Graphics)
 			{
 				Graphics ge = (Graphics) ve;
 				if (ge.getPathwayElement() == e)
 					return ge;
 			}
 		}
 		return null;
 	}
 
 	HashMap<MPoint, VPoint> pointsMtoV = new HashMap<MPoint, VPoint>();
 
 	protected VPoint getPoint(MPoint mPoint)
 	{
 		return pointsMtoV.get(mPoint);
 	}
 
 	public VPoint newPoint(MPoint mPoint, Line line)
 	{
 		VPoint p = pointsMtoV.get(mPoint);
 		if (p == null) {
 			p = new VPoint(this, mPoint, line);
 			pointsMtoV.put(mPoint, p);
 		}
 		return p;
 	}
 
 	/**
 	 * Get the gene identifiers of all genes in this pathway
 	 * 
 	 * @return List containing an identifier for every gene on the mapp
 	 * @deprecated get this info from Pathway directly
 	 */
 	public ArrayList<String> getMappIds()
 	{
 		ArrayList<String> mappIds = new ArrayList<String>();
 		for (VPathwayElement o : drawingObjects)
 		{
 			if (o instanceof GeneProduct)
 			{
 				mappIds.add(((GeneProduct) o).getID());
 			}
 		}
 		return mappIds;
 	}
 
 	/**
 	 * Set this drawing to editmode
 	 * 
 	 * @param editMode
 	 *            true if editmode has to be enabled, false if disabled (view
 	 *            mode)
 	 */
 	public void setEditMode(boolean editMode)
 	{
 		this.editMode = editMode;
 		if (!editMode)
 		{
 			clearSelection();
 		}
 
 		redraw();
 		int type = editMode ? VPathwayEvent.EDIT_MODE_ON
 				: VPathwayEvent.EDIT_MODE_OFF;
 		fireVPathwayEvent(new VPathwayEvent(this, type));
 	}
 
 	private double zoomFactor = 1.0 / 15.0;
 
 	/**
 	 * Get the current zoomfactor used. 1/15 means 100%, 15 gpml unit = 1 pixel
 	 * 2/15 means 200%, 7.5 gpml unit = 1 pixel
 	 * 
 	 * The 15/1 ratio is there because of the Visual Basic legacy of GenMAPP
 	 * 
 	 * To distinguish between model coordinates and view coordinates, we prefix
 	 * all coordinates with either v or m (or V or M). For example:
 	 * 
 	 * mTop = gdata.getMTop(); vTop = GeneProduct.getVTop();
 	 * 
 	 * Calculations done on M's and V's should always match. The only way to
 	 * convert is to use the functions mFromV and vFromM.
 	 * 
 	 * Correct: mRight = mLeft + mWidth; Wrong: mLeft += vDx; Fixed: mLeft +=
 	 * mFromV(vDx);
 	 * 
 	 * @return the current zoomfactor
 	 */
 	public double getZoomFactor()
 	{
 		return zoomFactor;
 	}
 
 	/**
 	 * same as getZoomFactor, but in %
 	 * 
 	 * @return
 	 */
 	public double getPctZoom()
 	{
 		return zoomFactor * 100 * 15.0;
 	}
 
 	/**
 	 * Sets the drawings zoom in percent
 	 * 
 	 * @param pctZoomFactor
 	 *            zoomfactor in percent
 	 * @see getFitZoomFactor() for fitting the pathway inside the viewport.
 	 */
 	public void setPctZoom(double pctZoomFactor)
 	{
 		zoomFactor = pctZoomFactor / 100.0 / 15.0;
 		int width = getVWidth();
 		int height = getVHeight();
 		for(VPathwayElement vpe : drawingObjects) {
 			vpe.zoomChanged();
 		}
 		if (parent != null)
 		{
 			parent.setVSize(width, height);
 			redraw();
 		}
 	}
 
 	/**
 	 * Calculate the zoom factor that would
 	 * make the pathway fit in the viewport. 
 	 */
 	public double getFitZoomFactor()
 	{
 		double result;
 		Dimension drawingSize = getWrapper().getVSize();
 		Dimension viewportSize = getWrapper().getViewportSize();
 		result = (int) Math.min(getPctZoom()
 				* (double) viewportSize.width / drawingSize.width,
 				getPctZoom() * (double) viewportSize.height
 						/ drawingSize.height);
 		return result;
 	}
 	
 	public void setPressedObject(VPathwayElement o)
 	{
 		pressedObject = o;
 	}
 
 
 	private LinkAnchor currentLinkAnchor;
 	
 	/**
 	 * @arg p2d point where mouse is at
 	 */
 	private void linkPointToObject(Point2D p2d, Handle g)
 	{
 		if (dragUndoState == DRAG_UNDO_CHANGE_START)
 		{
 			dragUndoState = DRAG_UNDO_CHANGED;
 		}
 		hideLinkAnchors();
 		
 		List<LinkProvider> objects = getLinkProvidersAt(p2d);
 		VPoint p = (VPoint) g.parent;
 		GraphIdContainer idc = null;
 		for (LinkProvider lp : objects)
 		{
 			lp.showLinkAnchors();
 			LinkAnchor la = lp.getLinkAnchorAt(p2d);
 			if(la != null) {
 				//Set graphRef
 				la.link(p.getMPoint());
 				idc = la.getGraphIdContainer();
 				if(currentLinkAnchor != null) {
 					currentLinkAnchor.unhighlight();
 				}
 				la.highlight();
 				currentLinkAnchor = la;
 				break;
 			}
 		}
 		if(idc == null) {
 			p.getMPoint().unlink();
 			if(currentLinkAnchor != null) {
 				currentLinkAnchor.unhighlight();
 			}
 		}
 	}
 
 	private void hideLinkAnchors() {
 		for(VPathwayElement pe : getDrawingObjects()) {
 			if(pe instanceof LinkProvider) {
 				((LinkProvider)pe).hideLinkAnchors();
 			}
 		}
 	}
 	
 	private boolean snapToAngle;
 	
 	/**
 	 * Check whether line movement and rotations should snap 
 	 * to angle.
 	 * @see GlobalPreference#SNAP_TO_ANGLE for the global setting
 	 * @see GlobalPreference#SNAP_TO_ANGLE_STEP for the angle step to be used
 	 * @return
 	 */
 	public boolean isSnapToAngle() {
 		return snapToAngle;
 	}
 
 	int vPreviousX;
 
 	int vPreviousY;
 
 	boolean isDragging;
 
 	/**
 	 * handles mouse movement
 	 */
 	public void mouseMove(MouseEvent ve)
 	{
 		snapToAngle = ve.isKeyDown(MouseEvent.M_SHIFT);
 		
 		// If draggin, drag the pressed object
 		// And only when the right button isn't clicked
 		if (pressedObject != null && isDragging && !ve.isKeyDown(java.awt.event.MouseEvent.BUTTON3_DOWN_MASK))
 		{
 			if (dragUndoState == DRAG_UNDO_CHANGE_START)
 			{
 				dragUndoState = DRAG_UNDO_CHANGED;
 			}
 			double vdx = ve.getX() - vPreviousX;
 			double vdy = ve.getY() - vPreviousY;
 			if (pressedObject instanceof Handle)
 			{
 				((Handle) (pressedObject)).vMoveTo(ve.getX(), ve.getY());
 			} else
 			{
 				pressedObject.vMoveBy(vdx, vdy);
 			}
 
 			vPreviousX = ve.getX();
 			vPreviousY = ve.getY();
 
 			if (pressedObject instanceof Handle
 					&& newTemplate == null
 					&& ((Handle) pressedObject).parent instanceof VPoint)
 			{
 				linkPointToObject(new Point2D.Double(ve.getX(), ve.getY()),
 						(Handle) pressedObject);
 			}
 			redrawDirtyRect();
 		}
 		// // Reset hover timer
 		// if (hoverTimer == null) {
 		// hoverTimer = new HoverTimer();
 		// hoverTimer.start();
 		// }
 		// hoverTimer.reset(ve);
 	}
 
 	// Disable for 1.0 release (no tooltips needed)
 	// TODO: stop this thread on closing VPathway
 	// TODO: convert pathway elements to Component and get
 	// tooltips for free!
 	// HoverTimer hoverTimer;
 	//
 	// class HoverTimer extends Thread
 	// {
 	// volatile long timer = 0;
 	//
 	// MouseEvent ve;
 	//
 	// volatile boolean interrupted;
 	// volatile boolean hovered;
 	//		
 	// public synchronized void reset(MouseEvent ve) {
 	// this.ve = ve;
 	// timer = System.currentTimeMillis();
 	// hovered = false;
 	// }
 	//
 	// public void run()
 	// {
 	// timer = System.currentTimeMillis();
 	// while (!isInterrupted())
 	// {
 	// try
 	// {
 	// Thread.sleep(5);
 	// } catch (InterruptedException e)
 	// {
 	// return;
 	// }
 	// if(System.currentTimeMillis() - timer > 750) {
 	// doHover();
 	// }
 	// }
 	// };
 	//
 	// void doHover()
 	// {
 	// if(!hovered) {
 	// fireVPathwayEvent(new VPathwayEvent(VPathway.this, getObjectsAt(ve
 	// .getLocation()), ve, VPathwayEvent.ELEMENT_HOVER));
 	// hovered = true;
 	// }
 	// }
 	// }
 
 	/**
 	 * Handles movement of objects with the arrow keys
 	 * 
 	 * @param ks
 	 */
 	public void moveByKey(KeyStroke ks, int increment)
 	{
 		List<Graphics> selectedGraphics = getSelectedGraphics();
 
 		if (selectedGraphics.size() > 0)
 		{
 
 			switch (ks.getKeyCode())
 			{
 			case 37:
 				undoManager.newAction("Move object");
 				selection.vMoveBy(-increment, 0);
 				break;
 			case 39:
 				undoManager.newAction("Move object");
 				selection.vMoveBy(increment, 0);
 				break;
 			case 38:
 				undoManager.newAction("Move object");
 				selection.vMoveBy(0, -increment);
 				break;
 			case 40:
 				undoManager.newAction("Move object");
 				selection.vMoveBy(0, increment);
 			}
 		}
 		redrawDirtyRect();
 	}
 
 	public void selectObject(VPathwayElement o)
 	{
 		clearSelection();
 		selection.addToSelection(o);
 	}
 
 	/**
 	 * Handles mouse Pressed input
 	 */
 	public void mouseDown(MouseEvent e)
 	{
 		vDragStart = new Point(e.getX(), e.getY());
 		temporaryCopy = (Pathway) data.clone();
 		// setFocus();
 		if (editMode)
 		{
 			if (newTemplate != null)
 			{
 				newObject(new Point(e.getX(), e.getY()));
 				// SwtGui.getCurrent().getWindow().deselectNewItemActions();
 			} else
 			{
 				editObject(new Point(e.getX(), e.getY()), e);
 			}
 		} else
 		{
 			mouseDownViewMode(e);
 		}
 		if (pressedObject != null)
 		{
 			fireVPathwayEvent(new VPathwayEvent(this, pressedObject, e,
 					VPathwayEvent.ELEMENT_CLICKED_DOWN));
 		}
 	}
 
 	/**
 	 * Handles mouse Released input
 	 */
 	public void mouseUp(MouseEvent e)
 	{
 		if (isDragging)
 		{
 			if (dragUndoState == DRAG_UNDO_CHANGED)
 			{
 				assert (temporaryCopy != null);
 				// further specify the type of undo event,
 				// depending on the type of object being dragged
 				String message = "Drag Object";
 				if (pressedObject instanceof Handle)
 				{
 					if (((Handle) pressedObject).getDirection() == Handle.DIRECTION_ROT)
 					{
 						message = "Rotate Object";
 					} else
 					{
 						message = "Resize Object";
 					}
 				}
 				undoManager.newAction(new UndoAction(message, temporaryCopy));
 				temporaryCopy = null;
 			}
 			resetHighlight();
 			hideLinkAnchors();
 			if (selection.isSelecting())
 			{ // If we were selecting, stop it
 				selection.stopSelecting();
 			}
 			// check if we placed a new object by clicking or dragging
 			// if it was a click, give object the initial size.
 			else if (newObject != null
 					&& Math.abs(vDragStart.x - e.getX()) <= MIN_DRAG_LENGTH
 					&& Math.abs(vDragStart.y - e.getY()) <= MIN_DRAG_LENGTH)
 			{
 				newObject.setInitialSize();
 			}
 			newObject = null;
 			setNewTemplate(null);
 			redrawDirtyRect();
 		}
 		isDragging = false;
 		dragUndoState = DRAG_UNDO_NOT_RECORDING;
 		if (pressedObject != null)
 		{
 			fireVPathwayEvent(new VPathwayEvent(this, pressedObject, e,
 					VPathwayEvent.ELEMENT_CLICKED_UP));
 		}
 	}
 
 	/**
 	 * Handles mouse entered input
 	 */
 	public void mouseDoubleClick(MouseEvent e)
 	{
 		VPathwayElement o = getObjectAt(e.getLocation());
 		if (o != null)
 		{
 			fireVPathwayEvent(new VPathwayEvent(this, o,
 					VPathwayEvent.ELEMENT_DOUBLE_CLICKED));
 		}
 	}
 
 	public void draw(Graphics2D g2d)
 	{
 		draw(g2d, null, true);
 	}
 
 	public void draw(Graphics2D g2d, Rectangle area)
 	{
 		draw(g2d, area, true);
 	}
 	
 	/**
 	 * Paints all components in the drawing. This method is called automatically
 	 * in the painting process
 	 * 
 	 * @param g2d
 	 *            Graphics2D object the pathway should be drawn onto
 	 * @param area
 	 *            area that should be updated, null if you want to update the
 	 *            entire pathway
 	 * @param erase
 	 *            true if the background should be erased
 	 */
 	public void draw(Graphics2D g2d, Rectangle area, boolean erase)
 	{
 		if (area == null)
 		{
 			area = g2d.getClipBounds();
 			if (area == null)
 			{
 				Dimension size = parent.getViewportSize(); //Draw the visible area
 				area = new Rectangle(0, 0, size.width, size.height);
 			}
 		}
 		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
 				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
 		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
 				RenderingHints.VALUE_ANTIALIAS_ON);
 
 		if (erase)
 		{
 			g2d.setColor(java.awt.Color.WHITE);
 			g2d.fillRect(area.x, area.y, area.width, area.height);
 		}
 
 		g2d.clip(area);
 		g2d.setColor(java.awt.Color.BLACK);
 		Collections.sort(drawingObjects);
 		for (VPathwayElement o : drawingObjects)
 		{
 			if (o.vIntersects(area))
 			{
 				if (checkDrawAllowed(o))
 				{
 					o.draw((Graphics2D) g2d.create());
 					fireVPathwayEvent(new VPathwayEvent(this, o,
 							(Graphics2D) g2d.create(),
 							VPathwayEvent.ELEMENT_DRAWN));
 				}
 			}
 		}
 	}
 
 	boolean checkDrawAllowed(VPathwayElement o)
 	{
 		if (isEditMode())
 			return true;
 		else
 			return !(o instanceof Handle || (o == selection && !isDragging));
 	}
 
 	/**
 	 * deselect all elements on the drawing and resets the selectionbox.
 	 * Equivalent to {@link #clearSelection(0, 0)}
 	 */
 	private void clearSelection()
 	{
 		clearSelection(0, 0);
 	}
 
 	/**
 	 * deselect all elements on the drawing and resets the selectionbox to the
 	 * given coordinates Equivalent to
 	 * {@link SelectionBox#reset(double, double))}
 	 */
 	private void clearSelection(double x, double y)
 	{
 		for (VPathwayElement e : drawingObjects)
 			e.deselect();
 		selection.reset(x, y);
 	}
 
 	/**
 	 * Handles event when on mouseDown in case the drawing is in view mode (does
 	 * nothing yet)
 	 * 
 	 * @param e
 	 *            the mouse event to handle
 	 */
 	private void mouseDownViewMode(MouseEvent e)
 	{
 		Point2D p2d = new Point2D.Double(e.getX(), e.getY());
 
 		pressedObject = getObjectAt(p2d);
 
 		if (pressedObject != null)
 			doClickSelect(p2d, e);
 		else
 			startSelecting(p2d);
 	}
 
 	/**
 	 * Initializes selection, resetting the selectionbox and then setting it to
 	 * the position specified
 	 * 
 	 * @param vp -
 	 *            the point to start with the selection
 	 */
 	private void startSelecting(Point2D vp)
 	{
 		if (!selectionEnabled)
 			return;
 
 		vPreviousX = (int) vp.getX();
 		vPreviousY = (int) vp.getY();
 		isDragging = true;
 		dragUndoState = DRAG_UNDO_NOT_RECORDING;
 
 		clearSelection(vp.getX(), vp.getY());
 		selection.startSelecting();
 		pressedObject = selection.getCornerHandle();
 	}
 
 	/**
 	 * Resets highlighting, unhighlights all GmmlDrawingObjects
 	 */
 	public void resetHighlight()
 	{
 		for (VPathwayElement o : drawingObjects)
 			o.unhighlight();
 		redrawDirtyRect();
 	}
 
 	/**
 	 * Called by MouseDown, when we're in editting mode and we're not adding new
 	 * objects prepares for dragging the object
 	 */
 	private void editObject(Point p, MouseEvent e)
 	{
 		Point2D p2d = new Point2D.Double(p.x, p.y);
 
 		pressedObject = getObjectAt(p2d);
 
 		// if we clicked on an object
 		if (pressedObject != null)
 		{
 			// if our object is an handle, select also it's parent.
 			if (pressedObject instanceof Handle)
 			{
 				VPathwayElement parent = ((Handle) pressedObject).parent;
 				parent.select();
 				//Special treatment for anchor
 				if(parent instanceof VAnchor) {
 					doClickSelect(p2d, e);
 				}
 			} else
 			{
 				doClickSelect(p2d, e);
 			}
 
 			// start dragging
 			vPreviousX = p.x;
 			vPreviousY = p.y;
 
 			isDragging = true;
 			dragUndoState = DRAG_UNDO_CHANGE_START;
 		} else
 		{
 			// start dragging selectionbox
 			startSelecting(p2d);
 		}
 	}
 
 	/**
 	 * Find the object at a particular location on the drawing
 	 * 
 	 * if you want to get more than one
 	 * 
 	 * @see #getObjectsAt(Point2D)
 	 */
 	public VPathwayElement getObjectAt(Point2D p2d)
 	{
 		Collections.sort(drawingObjects);
 		VPathwayElement probj = null;
 		for (VPathwayElement o : drawingObjects)
 		{
 			if (o.vContains(p2d))
 			{
 				// select this object, unless it is an invisible gmmlHandle
 				if (o instanceof Handle) {
 					if (((Handle) o).isVisible()) {
 						probj = o; //For the rest, only visible handles
 					}
 				} else {
 					probj = o;
 				}
 			}
 		}
 		return probj;
 	}
 
 	/**
 	 * Find all objects at a particular location on the drawing
 	 * 
 	 * if you only need the top object,
 	 * 
 	 * @see #getObjectAt(Point2D)
 	 */
 	public List<VPathwayElement> getObjectsAt(Point2D p2d)
 	{
 		List<VPathwayElement> result = new ArrayList<VPathwayElement>();
 		for (VPathwayElement o : drawingObjects)
 		{
 			if (o.vContains(p2d))
 			{
 				// select this object, unless it is an invisible gmmlHandle
 				if (o instanceof Handle && !((Handle) o).isVisible())
 					;
 				else
 					result.add(o);
 			}
 		}
 		return result;
 	}
 
 	private List<LinkProvider> getLinkProvidersAt(Point2D p) {
 		List<LinkProvider> result = new ArrayList<LinkProvider>();
 		for (VPathwayElement o : drawingObjects)
 		{
 			if (o instanceof LinkProvider && o.getVBounds().contains(p))
 			{
 				// select this object, unless it is an invisible gmmlHandle
 				result.add((LinkProvider)o);
 			}
 		}
 		return result;
 	}
 	
 	void doClickSelect(Point2D p2d, MouseEvent e)
 	{
 		if (!selectionEnabled)
 			return;
 
 		// Shift pressed, add/remove from selection
 		boolean modifierPressed = e.isKeyDown(MouseEvent.M_SHIFT);
 		if (modifierPressed)
 		{
 			if (pressedObject instanceof SelectionBox)
 			{
 				// Object inside selectionbox clicked, pass to selectionbox
 				selection.objectClicked(p2d);
 			} else if (pressedObject.isSelected())
 			{ // Already in selection:
 				// remove
 				selection.removeFromSelection(pressedObject);
 			} else
 			{
 				selection.addToSelection(pressedObject); // Not in selection:
 				// add
 			}
 			pressedObject = selection; // Set dragging to selectionbox
 		} else
 		// Shift not pressed
 		{
 			// If pressedobject is not selectionbox:
 			// Clear current selection and select pressed object
 			if (!(pressedObject instanceof SelectionBox))
 			{
 				clearSelection();
 				//If the object is a handle, select the parent instead
 				if(pressedObject instanceof Handle) {
 					selection.addToSelection(((Handle)pressedObject).parent);
 				} else {
 					selection.addToSelection(pressedObject);
 				}
 			} else
 			{ // Check if clicked object inside selectionbox
 				if (selection.getChild(p2d) == null)
 					clearSelection();
 			}
 		}
 		redrawDirtyRect();
 	}
 
 	public static final int NEWNONE = -1;
 
 	public static final int NEWLINE = 0;
 
 	public static final int NEWLABEL = 1;
 
 	public static final int NEWARC = 2;
 
 	public static final int NEWBRACE = 3;
 
 	public static final int NEWGENEPRODUCT = 4;
 
 	public static final int NEWLINEDASHED = 5;
 
 	public static final int NEWLINEARROW = 6;
 
 	public static final int NEWLINEDASHEDARROW = 7;
 
 	public static final int NEWRECTANGLE = 8;
 
 	public static final int NEWOVAL = 9;
 
 	public static final int NEWTBAR = 10;
 
 	public static final int NEWRECEPTORROUND = 11;
 
 	public static final int NEWLIGANDROUND = 12;
 
 	public static final int NEWRECEPTORSQUARE = 13;
 
 	public static final int NEWLIGANDSQUARE = 14;
 
 	public static final int NEWLINEMENU = 15;
 
 	public static final int NEWLINESHAPEMENU = 16;
 
 	public static final Color stdRGB = new Color(0, 0, 0);
 
 	/**
 	 * pathvisio distinguishes between placing objects with a click or with a
 	 * drag. If you don't move the cursor in between the mousedown and mouseup
 	 * event, the object is placed with a default initial size.
 	 * 
 	 * vDragStart is used to determine the mousemovement during the click.
 	 */
 	private Point vDragStart;
 
 	/**
 	 * dragUndoState determines what should be done when you release the mouse
 	 * button after dragging an object.
 	 * 
 	 * if it is DRAG_UNDO_NOT_RECORDING, it's not necessary to record an event.
 	 * This is the case when we were dragging a selection rectangle, or a new
 	 * object (in which case the change event was already recorded)
 	 * 
 	 * in other cases, it is set to DRAG_UNDO_CHANGE_START at the start of the
 	 * drag. If additional move events occur, the state is changed to
 	 * DRAG_UNDO_CHANGED. The latter will lead to recording of the undo event.
 	 */
 	static final int DRAG_UNDO_NOT_RECORDING = 0;
 
 	static final int DRAG_UNDO_CHANGE_START = 1;
 
 	static final int DRAG_UNDO_CHANGED = 2;
 
 	int dragUndoState = DRAG_UNDO_NOT_RECORDING;
 
 	/** newly placed object, is set to null again when mouse button is released */
 	private PathwayElement newObject = null;
 
 	/** minimum drag length for it to be considered a drag and not a click */
 	private static final int MIN_DRAG_LENGTH = 3;
 
 	/**
 	 * Add a new object to the drawing {@see VPathway#setNewGraphics(int)}
 	 * 
 	 * @param pwy
 	 *            The point where the user clicked on the drawing to add a new
 	 *            graphics
 	 */
 	private void newObject(Point ve)
 	{
 		undoManager.newAction("New Object");
 		double mx = mFromV((double) ve.x);
 		double my = mFromV((double) ve.y);
 
 		PathwayElement[] newObjects = newTemplate.addElements(data, mx, my);
 		newObject = newTemplate.getDragElement(this) == null ? null : newObjects[0];
 		
 		isDragging = true;
 		dragUndoState = DRAG_UNDO_NOT_RECORDING;
 		
 		selectObject(lastAdded);
 		pressedObject = newTemplate.getDragElement(this);
 
 		vPreviousX = ve.x;
 		vPreviousY = ve.y;
 
 		fireVPathwayEvent(new VPathwayEvent(this, lastAdded,
 				VPathwayEvent.ELEMENT_ADDED));
 	}
 
 	public void mouseEnter(MouseEvent e)
 	{
 	}
 
 	public void mouseExit(MouseEvent e)
 	{
 	}
 
 	/**
 	 * Select all objects of the given class
 	 * 
 	 * @param c
 	 *            The class of the objects to be selected
 	 */
 	void selectObjects(Class<?> c)
 	{
 		clearSelection();
 		selection.startSelecting();
 		for (VPathwayElement vpe : getDrawingObjects())
 		{
 			if (c == null || c.isInstance(vpe))
 			{
 				selection.addToSelection(vpe);
 			}
 		}
 		selection.stopSelecting();
 		redrawDirtyRect();
 	}
 
 	void selectAll()
 	{
 		selectObjects(null);
 	}
 
 	// private void insertPressed()
 	// {
 	// Set<VPathwayElement> objects = new HashSet<VPathwayElement>();
 	// objects.addAll(selection.getSelection());
 	// for (VPathwayElement o : objects)
 	// {
 	// if (o instanceof Line)
 	// {
 	// PathwayElement g = ((Line)o).getGmmlData();
 	// PathwayElement[] gNew = g.splitLine();
 	//							
 	// removeDrawingObject(o); //Remove the old line
 	//				
 	// //Clear refs on middle point (which is new)
 	// gNew[0].getMEnd().setGraphRef(null);
 	// gNew[1].getMStart().setGraphRef(null);
 	//				
 	// gNew[1].setGraphId(data.getUniqueId());
 	// data.add(gNew[0]);
 	// Line l1 = (Line)lastAdded;
 	// data.add(gNew[1]);
 	// Line l2 = (Line)lastAdded;
 	//				
 	// l1.getEnd().link(l2.getStart());
 	// }
 	// }
 	// selection.addToSelection(lastAdded);
 	// }
 
 	/**
 	 * Responds to ctrl-G. First checks for current status of selection with
 	 * respect to grouping. If selection is already grouped (members of the same
 	 * parent group), then the highest-level (parent) group is removed along
 	 * with all references to the group. If the selection is not a uniform
 	 * group, then a new group is created and each member or groups of members
 	 * is set to reference the new group.
 	 * 
 	 * @param selection
 	 */
 	public void toggleGroup(List<Graphics> selection)
 	{
 		boolean selectionGrouped = true;
 		String topRef = null;
 
 		/** 
 		 * Check group status of current selection
 		 */
 		for (Graphics g : selection)
 		{
 			PathwayElement pe = g.getPathwayElement();
 			String ref = pe.getGroupRef();
 			String id = pe.getGroupId();
 			// If not a group
 			if (id == null && selectionGrouped)
 			{
 				// and not a member of a group
 				if (ref == null)
 				{
 					// then selection needs to be grouped
 					selectionGrouped = false;
 				}
 				// and is a member of a group
 				else
 				{
 					// Identify highest-level, non-null group reference
 					String checkRef = ref;
 					while (checkRef != null)
 					{
 						ref = checkRef;
 						PathwayElement refGroup = data.getGroupById(checkRef);
 						checkRef = refGroup.getGroupRef();
 					}
 					// Set first identified highest-level group ref
 					if (topRef == null)
 					{
 						topRef = ref;
 					}
 
 					// Check other identified refs against first identified
 					// If not equal
 					if (!ref.equals(topRef))
 					{
 						// then selection includes elements in distinct,
 						// non-nested groups; therefore, currently Ungrouped
 						selectionGrouped = false;
 					}
 				}
 			}
 		}
 
 		/**
 		 * Group or ungroup based on current group status
 		 */
 		// If selection is already grouped, then ungroup.
 		if (selectionGrouped)
 		{
			clearSelection();

 			VPathwayElement topVPE = null;
 			// Ungroup all elements asociated with topRef
 			for (VPathwayElement vpe : this.getDrawingObjects())
 			{
 				if (vpe instanceof Graphics)
 				{
 					PathwayElement pe = ((Graphics) vpe).getPathwayElement();
 
 					// remove all references to highest-level group
 					// from children datanodes and nested groups
 					if (topRef.equals(pe.getGroupRef()))
 					{
 						pe.setGroupRef(null);
						this.selection.addToSelection(vpe);
 					}
 
 					// remove highest-level group itself
 					if (vpe instanceof Group)
 					{
 						if (topRef.equals(pe.getGroupId()))
 						{
 							// Cannot remove object within getDrawingObjects()
 							// loop, so just save vpe of highest-level group for
 							// deletion later (see below)
 							topVPE = vpe;
 						}
 					}
 				}
 			}
 			
 			// remove highest-level group
 			this.removeDrawingObject(topVPE, true);
 			
 			// clear id from hash map
 			data.removeGroupId(topRef);
 
 		} 
 		// If selection is not grouped, then group.
 		else
 		{
 			PathwayElement group = PathwayElement.createPathwayElement(ObjectType.GROUP);
 			data.add(group);
 			group.setTextLabel("new group");
 			group.setGroupStyle(GroupStyle.NONE);
 			String id = group.createGroupId();
 			
 			for (Graphics g : selection)
 			{
 				PathwayElement pe = g.getPathwayElement();
 				String ref = pe.getGroupRef();
 
 				// If not a member of a group, then set group ref
 				if (ref == null)
 				{
 					pe.setGroupRef(id);
 				} 
 			}
 			
 			// Parent group should not reference self
 			group.setGroupRef(null);
			Graphics vg = getPathwayElementView(group);
			if(vg != null) {
				clearSelection();
				selectObject(vg);
			}
 		}
 	}
 
 	public void keyPressed(KeyEvent e)
 	{
 		// Use registerKeyboardActions
 	}
 
 	public static final KeyStroke KEY_SELECT_DATA_NODES = KeyStroke
 			.getKeyStroke(java.awt.event.KeyEvent.VK_D,
 					java.awt.Event.CTRL_MASK);
 
 	public static final KeyStroke KEY_MOVERIGHT = KeyStroke.getKeyStroke(
 			java.awt.event.KeyEvent.VK_RIGHT, 0);
 
 	public static final KeyStroke KEY_MOVELEFT = KeyStroke.getKeyStroke(
 			java.awt.event.KeyEvent.VK_LEFT, 0);
 
 	public static final KeyStroke KEY_MOVEUP = KeyStroke.getKeyStroke(
 			java.awt.event.KeyEvent.VK_UP, 0);
 
 	public static final KeyStroke KEY_MOVEDOWN = KeyStroke.getKeyStroke(
 			java.awt.event.KeyEvent.VK_DOWN, 0);
 
 	public static final KeyStroke KEY_MOVERIGHT_SHIFT = KeyStroke.getKeyStroke(
 			java.awt.event.KeyEvent.VK_RIGHT, java.awt.Event.SHIFT_MASK);
 
 	public static final KeyStroke KEY_MOVELEFT_SHIFT = KeyStroke.getKeyStroke(
 			java.awt.event.KeyEvent.VK_LEFT, java.awt.Event.SHIFT_MASK);
 
 	public static final KeyStroke KEY_MOVEUP_SHIFT = KeyStroke.getKeyStroke(
 			java.awt.event.KeyEvent.VK_UP, java.awt.Event.SHIFT_MASK);
 
 	public static final KeyStroke KEY_MOVEDOWN_SHIFT = KeyStroke.getKeyStroke(
 			java.awt.event.KeyEvent.VK_DOWN, java.awt.Event.SHIFT_MASK);
 
 	/**
 	 * Get the view actions, a class where several actions related to the view
 	 * are stored (delete, select) and where other actions can be registered to
 	 * a group (e.g. a group that will be disabled when edit-mode is turned off)
 	 * 
 	 * @return an instance of the {@link ViewActions} class
 	 */
 	public ViewActions getViewActions()
 	{
 		return viewActions;
 	}
 
 	/**
 	 * Several {@link Action}s related to the view
 	 */
 	private ViewActions viewActions;
 
 	// Convenience method to register an action that has an accelerator key
 	private void registerKeyboardAction(Action a)
 	{
 		KeyStroke key = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
 		if (key == null)
 			throw new RuntimeException("Action " + a
 					+ " must have value ACCELERATOR_KEY set");
 		parent.registerKeyboardAction(key, a);
 	}
 
 	private void registerKeyboardActions()
 	{
 		viewActions = new ViewActions(this);
 
 		registerKeyboardAction(viewActions.copy);
 		registerKeyboardAction(viewActions.paste);
 		parent.registerKeyboardAction(KEY_SELECT_DATA_NODES,
 				viewActions.selectDataNodes);
 		registerKeyboardAction(viewActions.toggleGroup);
 		registerKeyboardAction(viewActions.selectAll);
 		registerKeyboardAction(viewActions.delete);
 		registerKeyboardAction(viewActions.undo);
 		registerKeyboardAction(viewActions.addAnchor);
 		registerKeyboardAction(viewActions.orderBringToFront);
 		registerKeyboardAction(viewActions.orderSendToBack);
 		registerKeyboardAction(viewActions.orderUp);
 		registerKeyboardAction(viewActions.orderDown);
 		registerKeyboardAction(viewActions.showUnlinked);
 		parent.registerKeyboardAction(KEY_MOVERIGHT, new KeyMoveAction(
 				KEY_MOVERIGHT));
 		parent.registerKeyboardAction(KEY_MOVERIGHT_SHIFT, new KeyMoveAction(
 				KEY_MOVERIGHT_SHIFT));
 		parent.registerKeyboardAction(KEY_MOVELEFT, new KeyMoveAction(
 				KEY_MOVELEFT));
 		parent.registerKeyboardAction(KEY_MOVELEFT_SHIFT, new KeyMoveAction(
 				KEY_MOVELEFT_SHIFT));
 		parent
 				.registerKeyboardAction(KEY_MOVEUP, new KeyMoveAction(
 						KEY_MOVEUP));
 		parent.registerKeyboardAction(KEY_MOVEUP_SHIFT, new KeyMoveAction(
 				KEY_MOVEUP_SHIFT));
 		parent.registerKeyboardAction(KEY_MOVEDOWN, new KeyMoveAction(
 				KEY_MOVEDOWN));
 		parent.registerKeyboardAction(KEY_MOVEDOWN_SHIFT, new KeyMoveAction(
 				KEY_MOVEDOWN_SHIFT));
 	}
 
 	public void keyReleased(KeyEvent e)
 	{
 		// use registerKeyboardActions
 	}
 
 	/**
 	 * Removes the GmmlDrawingObjects in the ArrayList from the drawing<BR>
 	 * Does not remove the model representation!
 	 * 
 	 * @param toRemove
 	 *            The List containing the objects to be removed
 	 */
 	public void removeDrawingObjects(List<VPathwayElement> toRemove)
 	{
 		removeDrawingObjects(toRemove, false);
 	}
 
 	/**
 	 * Removes the GmmlDrawingObjects in the ArrayList from the drawing
 	 * 
 	 * @param toRemove
 	 *            The List containing the objects to be removed
 	 * @param removeFromModel
 	 *            Whether to remove the model representation or not
 	 */
 	public void removeDrawingObjects(List<VPathwayElement> toRemove,
 			boolean removeFromModel)
 	{
 		for (VPathwayElement o : toRemove)
 		{
 			removeDrawingObject(o, removeFromModel);
 		}
 		selection.fitToSelection();
 	}
 
 	public void removeDrawingObject(VPathwayElement toRemove,
 			boolean removeFromModel)
 	{
 		toRemove.destroy(); // Object will remove itself from the drawing
 		if (removeFromModel)
 		{
 			if (toRemove instanceof Graphics)
 			{
 				data.remove(((Graphics) toRemove).getPathwayElement());
 			}
 		}
 		selection.removeFromSelection(toRemove); // Remove from selection
 		redrawDirtyRect();
 	}
 
 	Graphics lastAdded = null;
 
 	public void gmmlObjectModified(PathwayEvent e)
 	{
 		switch (e.getType())
 		{
 		case PathwayEvent.MODIFIED_GENERAL:
 			checkBoardSize(e.getAffectedData());
 			break;
 		case PathwayEvent.DELETED:
 			Graphics deleted = getPathwayElementView(e.getAffectedData());
 			if (deleted != null)
 			{
 				deleted.markDirty();
 				removeDrawingObject(deleted, false);
 			}
 			break;
 		case PathwayEvent.ADDED:
 			lastAdded = fromGmmlDataObject(e.getAffectedData());
 			if (lastAdded != null) {
 				lastAdded.markDirty();
 			}
 			break;
 		case PathwayEvent.WINDOW:
 			int width = (int) vFromM(infoBox.getPathwayElement()
 					.getMBoardWidth());
 			int height = (int) vFromM(infoBox.getPathwayElement()
 					.getMBoardHeight());
 			if (parent != null)
 			{
 				parent.setVSize(width, height);
 			}
 			break;
 		}
 		redrawDirtyRect();
 	}
 
 	/**
 	 * Calculate the board size. Calls {@link VPathwayElement#getVBounds()} for every
 	 * element and adds all results together to obtain the board size
 	 */
 	public Dimension calculateVSize() {
 		Rectangle2D bounds = new Rectangle2D.Double();
 		for(VPathwayElement e : drawingObjects) {
 			bounds.add(e.getVBounds());
 		}
 		return new Dimension((int)bounds.getWidth() + 10, (int)bounds.getHeight() + 10);
 	}
 		
 	/**
 	 * Checks whether the board size is still large enough for the given {@link PathwayElement}
 	 * and increases the size if not
 	 * @param elm The element to check the board size for
 	 */
 	void checkBoardSize(PathwayElement elm)
 	{
 		double increase = mFromV(25);
 		Dimension size = parent.getVSize();
 		double mw = mFromV(size.width);
 		double mh = mFromV(size.height);
 
 		double mx = mw;
 		double my = mh;
 
 		switch (elm.getObjectType())
 		{
 		case ObjectType.LINE:
 			mx = Math.max(elm.getMEndX(), elm.getMStartX());
 			my = Math.max(elm.getMEndY(), elm.getMStartY());
 			break;
 		case ObjectType.MAPPINFO:
 			mx = elm.getMLeft() + mFromV(200); //Initial size for mappinfo (has zero size in model);
 			my = elm.getMTop() + mFromV(200);
 			break;
 		default:
 			mx = elm.getMLeft() + elm.getMWidth();
 			my = elm.getMTop() + elm.getMHeight();
 			break;
 		}
 
 		if (mw < mx || mh < my)
 		{
 			if (mw < mx)
 				mw = mx + increase;
 			if (mh < my)
 				mh = my + increase;
 			parent.setVSize(new Dimension((int) vFromM(mw), (int) vFromM(mh)));
 		}
 	}
 
 	/**
 	 * Makes a copy of all GmmlDataObjects in current selection, and puts them
 	 * in the global clipboard.
 	 * 
 	 */
 	public void copyToClipboard()
 	{
 		List<PathwayElement> result = new ArrayList<PathwayElement>();
 		for (VPathwayElement g : drawingObjects)
 		{
 			if (g.isSelected() && g instanceof Graphics
 					&& !(g instanceof SelectionBox))
 			{
 				result.add(((Graphics) g).gdata.copy());
 			}
 		}
 		if (result.size() > 0)
 		{
 			getWrapper().copyToClipboard(getPathwayModel(), result);
 		}
 	}
 
 	/**
 	 * Aligns selected objects based on user-selected align type
 	 * 
 	 * @param alignType
 	 */
 	public void alignSelected(AlignType alignType)
 	{
 		List<Graphics> selectedGraphics = getSelectedGraphics();
 
 		if (selectedGraphics.size() > 0)
 		{
 			Rectangle2D vBoundsFirst; // The bounds of the model to view
 			// translated shape
 
 			switch (alignType)
 			{
 			case CENTERX:
 				undoManager.newAction("Align horizontally on center");
 				Collections.sort(selectedGraphics, new YComparator());
 
 				vBoundsFirst = selectedGraphics.get(0).getVShape(true)
 						.getBounds2D();
 
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					Graphics g = selectedGraphics.get(i);
 					Rectangle2D vBounds = g.getVShape(true).getBounds2D();
 
 					selectedGraphics.get(i)
 							.vMoveBy(
 									vBoundsFirst.getCenterX()
 											- vBounds.getCenterX(), 0);
 				}
 				break;
 			case CENTERY:
 				undoManager.newAction("Align vertically on center");
 				Collections.sort(selectedGraphics, new XComparator());
 
 				vBoundsFirst = selectedGraphics.get(0).getVShape(true)
 						.getBounds2D();
 
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					Graphics g = selectedGraphics.get(i);
 					Rectangle2D vBounds = g.getVShape(true).getBounds2D();
 
 					selectedGraphics.get(i).vMoveBy(0,
 							vBoundsFirst.getCenterY() - vBounds.getCenterY());
 				}
 				break;
 			case LEFT:
 				undoManager.newAction("Align on left side");
 				Collections.sort(selectedGraphics, new YComparator());
 
 				vBoundsFirst = selectedGraphics.get(0).getVShape(true)
 						.getBounds2D();
 
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					Graphics g = selectedGraphics.get(i);
 					Rectangle2D vBounds = g.getVShape(true).getBounds2D();
 
 					selectedGraphics.get(i).vMoveBy(
 							vBoundsFirst.getX() - vBounds.getX(), 0);
 				}
 				break;
 			case RIGHT:
 				undoManager.newAction("Align on right side");
 				Collections.sort(selectedGraphics, new YComparator());
 
 				vBoundsFirst = selectedGraphics.get(0).getVShape(true)
 						.getBounds2D();
 
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					Graphics g = selectedGraphics.get(i);
 					Rectangle2D vBounds = g.getVShape(true).getBounds2D();
 
 					selectedGraphics.get(i).vMoveBy(
 							vBoundsFirst.getMaxX() - vBounds.getMaxX(), 0);
 				}
 				break;
 			case TOP:
 				undoManager.newAction("Align on top side");
 				Collections.sort(selectedGraphics, new YComparator());
 
 				vBoundsFirst = selectedGraphics.get(0).getVShape(true)
 						.getBounds2D();
 
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					Graphics g = selectedGraphics.get(i);
 					Rectangle2D vBounds = g.getVShape(true).getBounds2D();
 
 					selectedGraphics.get(i).vMoveBy(0,
 							vBoundsFirst.getY() - vBounds.getY());
 				}
 				break;
 			case BOTTOM:
 				undoManager.newAction("Align on bottom side");
 				Collections.sort(selectedGraphics, new YComparator());
 				Collections.reverse(selectedGraphics);
 
 				vBoundsFirst = selectedGraphics.get(0).getVShape(true)
 						.getBounds2D();
 
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					Graphics g = selectedGraphics.get(i);
 					Rectangle2D vBounds = g.getVShape(true).getBounds2D();
 
 					selectedGraphics.get(i).vMoveBy(0,
 							vBoundsFirst.getMaxY() - vBounds.getMaxY());
 				}
 				break;
 			case WIDTH:
 			case HEIGHT:
 				undoManager.newAction("Set common " + (alignType == AlignType.WIDTH ? "width" : "height"));
 				scaleSelected(alignType);
 				break;
 			}
 			selection.fitToSelection();
 			redrawDirtyRect();
 		}
 	}
 
 	/**
 	 * Move a set of graphics to the top in the z-order stack
 	 */
 	public void moveGraphicsTop (List<Graphics> gs)
 	{
 		Collections.sort (gs, new ZComparator());
 		int base = getPathwayModel().getMaxZOrder() + 1;
 		for (Graphics g : gs)
 		{
 			g.gdata.setZOrder(base++);
 		}
 	}
 	
 	/**
 	 * Move a set of graphics to the bottom in the z-order stack
 	 */
 	public void moveGraphicsBottom (List<Graphics> gs)
 	{
 		Collections.sort (gs, new ZComparator());
 		int base = getPathwayModel().getMinZOrder() - gs.size() - 1;
 		for (Graphics g : gs)
 		{
 			g.gdata.setZOrder(base++);
 		}
 	}
 	
 	/**
 	 * Looks for overlapping graphics with a higher z-order
 	 * and moves g on top of that.
 	 */
 	public void moveGraphicsUp (List<Graphics> gs)
 	{
 		//TODO: Doesn't really work very well with multiple selections
 		for (Graphics g : gs)
 		{
 			// make sure there is enough space between g and the next
 			autoRenumberZOrder();
 			
 			int order = g.gdata.getZOrder();
 			Graphics nextGraphics = null;
 			int nextZ = order;
 			for (Graphics i : getOverlappingGraphics(g))
 			{
 				int iorder = i.gdata.getZOrder();
 				if (nextGraphics == null && iorder > nextZ)
 				{
 					nextZ = iorder;
 					nextGraphics = i;
 				}
 				else if (nextGraphics != null && iorder < nextZ && iorder > order)
 				{
 					nextZ = iorder;
 					nextGraphics = i;
 				}
 			}
 			g.gdata.setZOrder (nextZ + 1);
 		}
 	}
 
 	/** 
 	 * makes sure there is always a minimum spacing of two between 
 	 * two consecutive elements, so that we can freely move items in between
 	 */
 	private void autoRenumberZOrder()
 	{
 		List<Graphics> elts = new ArrayList<Graphics>(); 
 		for (VPathwayElement vp : drawingObjects)
 		{
 			if (vp instanceof Graphics)
 			{
 				elts.add ((Graphics)vp);
 			}
 		}
 		if (elts.size() < 2) return; // nothing to renumber
 		Collections.sort (elts, new ZComparator());
 		
 		final int SPACING = 2;
 		
 		int waterLevel = elts.get(0).gdata.getZOrder();
 		for (int i = 1; i < elts.size(); ++i)
 		{
 			Graphics curr = elts.get (i);
 			if (curr.gdata.getZOrder() - waterLevel < SPACING)
 			{
 				curr.gdata.setZOrder(waterLevel + SPACING);
 			}
 			waterLevel = curr.gdata.getZOrder();
 		}
 	}
 
 		/**
 	 * Looks for overlapping graphics with a lower z-order
 	 * and moves g on under that.
 	 */
 	public void moveGraphicsDown (List<Graphics> gs)
 	{
 		//TODO: Doesn't really work very well with multiple selections
 		for (Graphics g : gs)
 		{
 			// make sure there is enough space between g and the previous
 			autoRenumberZOrder();
 	
 			int order = g.gdata.getZOrder();
 			Graphics nextGraphics = null;
 			int nextZ = order;
 			for (Graphics i : getOverlappingGraphics(g))
 			{
 				int iorder = i.gdata.getZOrder();
 				if (nextGraphics == null && iorder < nextZ)
 				{
 					nextZ = iorder;
 					nextGraphics = i;
 				}
 				else if (nextGraphics != null && iorder > nextZ && iorder < order)
 				{
 					nextZ = iorder;
 					nextGraphics = i;
 				}
 			}
 			g.gdata.setZOrder (nextZ - 1);
 		}
 	}
 	/**
 	 * return a list of Graphics that overlap g.
 	 * Note that the intersection of bounding rectangles is used,
 	 * so the returned list is only an approximation for rounded shapes. 
 	 */
 	public List<Graphics> getOverlappingGraphics(Graphics g)
 	{
 		List<Graphics> result = new ArrayList<Graphics>();
 		Rectangle2D r1 = g.getVBounds();
 
 		for (VPathwayElement ve : drawingObjects)
 		{
 			if (ve instanceof Graphics && ve != g)
 			{
 				Graphics i = (Graphics)ve;
 				if (r1.intersects(ve.getVBounds()))
 				{
 					result.add (i);
 				}
 			}
 		}
 		return result;
 	}
 	
 	/**
 	 * Stacks selected objects based on user-selected stack type
 	 * 
 	 * @param stackType
 	 */
 	public void stackSelected(StackType stackType)
 	{
 		List<Graphics> selectedGraphics = getSelectedGraphics();
 
 		if (selectedGraphics.size() > 0)
 		{
 			switch (stackType)
 			{
 			case CENTERX:
 				undoManager.newAction("Stack Center Vertically");
 				Collections.sort(selectedGraphics, new YComparator());
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					// Get the current and previous graphics objects
 					Graphics eCurr = selectedGraphics.get(i);
 					Graphics ePrev = selectedGraphics.get(i - 1);
 
 					// Get the bounds of the model to view translated shapes
 					Rectangle2D vBoundsPrev = ePrev.getVShape(true)
 							.getBounds2D();
 					Rectangle2D vBoundsCurr = eCurr.getVShape(true)
 							.getBounds2D();
 
 					eCurr.vMoveBy(vBoundsPrev.getCenterX()
 							- vBoundsCurr.getCenterX(), 0);
 
 					eCurr
 							.vMoveBy(0, vBoundsPrev.getMaxY()
 									- vBoundsCurr.getY());
 
 					/*
 					 * selectedGraphics.get(i).getGmmlData().setMCenterX(
 					 * selectedGraphics.get(i - 1).getGmmlData()
 					 * .getMCenterX());
 					 * selectedGraphics.get(i).getGmmlData().setMTop(
 					 * selectedGraphics.get(i - 1).getGmmlData().getMTop() +
 					 * selectedGraphics.get(i - 1).getGmmlData() .getMHeight());
 					 */
 				}
 				break;
 			case CENTERY:
 				undoManager.newAction("Stack Center Horizontally");
 				Collections.sort(selectedGraphics, new XComparator());
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					// Get the current and previous graphics objects
 					Graphics eCurr = selectedGraphics.get(i);
 					Graphics ePrev = selectedGraphics.get(i - 1);
 
 					// Get the bounds of the model to view translated shapes
 					Rectangle2D vBoundsPrev = ePrev.getVShape(true)
 							.getBounds2D();
 					Rectangle2D vBoundsCurr = eCurr.getVShape(true)
 							.getBounds2D();
 
 					selectedGraphics.get(i).vMoveBy(
 							vBoundsPrev.getMaxX() - vBoundsCurr.getX(), 0);
 
 					selectedGraphics.get(i)
 							.vMoveBy(
 									0,
 									vBoundsPrev.getCenterY()
 											- vBoundsCurr.getCenterY());
 
 					/*
 					 * selectedGraphics.get(i).getGmmlData().setMCenterY(
 					 * selectedGraphics.get(i - 1).getGmmlData()
 					 * .getMCenterY());
 					 * selectedGraphics.get(i).getGmmlData().setMLeft(
 					 * selectedGraphics.get(i - 1).getGmmlData() .getMLeft() +
 					 * selectedGraphics.get(i - 1).getGmmlData() .getMWidth());
 					 */
 				}
 				break;
 			case LEFT:
 				undoManager.newAction("Stack Left");
 				Collections.sort(selectedGraphics, new YComparator());
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					// Get the current and previous graphics objects
 					Graphics eCurr = selectedGraphics.get(i);
 					Graphics ePrev = selectedGraphics.get(i - 1);
 
 					// Get the bounds of the model to view translated shapes
 					Rectangle2D vBoundsPrev = ePrev.getVShape(true)
 							.getBounds2D();
 					Rectangle2D vBoundsCurr = eCurr.getVShape(true)
 							.getBounds2D();
 
 					eCurr.vMoveBy(vBoundsPrev.getX() - vBoundsCurr.getX(), 0);
 
 					eCurr
 							.vMoveBy(0, vBoundsPrev.getMaxY()
 									- vBoundsCurr.getY());
 
 					// selectedGraphics.get(i).getGmmlData().setMLeft(
 					// selectedGraphics.get(i - 1).getGmmlData()
 					// .getMLeft());
 					// selectedGraphics.get(i).getGmmlData().setMTop(
 					// selectedGraphics.get(i - 1).getGmmlData().getMTop()
 					// + selectedGraphics.get(i - 1).getGmmlData()
 					// .getMHeight());
 				}
 				break;
 			case RIGHT:
 				undoManager.newAction("Stack Right");
 				Collections.sort(selectedGraphics, new YComparator());
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					// Get the current and previous graphics objects
 					Graphics eCurr = selectedGraphics.get(i);
 					Graphics ePrev = selectedGraphics.get(i - 1);
 
 					// Get the bounds of the model to view translated shapes
 					Rectangle2D vBoundsPrev = ePrev.getVShape(true)
 							.getBounds2D();
 					Rectangle2D vBoundsCurr = eCurr.getVShape(true)
 							.getBounds2D();
 
 					eCurr.vMoveBy(
 							vBoundsPrev.getMaxX() - vBoundsCurr.getMaxX(), 0);
 
 					eCurr
 							.vMoveBy(0, vBoundsPrev.getMaxY()
 									- vBoundsCurr.getY());
 
 					// selectedGraphics.get(i).getGmmlData().setMLeft(
 					// selectedGraphics.get(i - 1).getGmmlData()
 					// .getMLeft()
 					// + selectedGraphics.get(i - 1).getGmmlData()
 					// .getMWidth()
 					// - selectedGraphics.get(i).getGmmlData()
 					// .getMWidth());
 					// selectedGraphics.get(i).getGmmlData().setMTop(
 					// selectedGraphics.get(i - 1).getGmmlData().getMTop()
 					// + selectedGraphics.get(i - 1).getGmmlData()
 					// .getMHeight());
 
 				}
 				break;
 			case TOP:
 				undoManager.newAction("Stack Top");
 				Collections.sort(selectedGraphics, new XComparator());
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					// Get the current and previous graphics objects
 					Graphics eCurr = selectedGraphics.get(i);
 					Graphics ePrev = selectedGraphics.get(i - 1);
 
 					// Get the bounds of the model to view translated shapes
 					Rectangle2D vBoundsPrev = ePrev.getVShape(true)
 							.getBounds2D();
 					Rectangle2D vBoundsCurr = eCurr.getVShape(true)
 							.getBounds2D();
 
 					selectedGraphics.get(i).vMoveBy(
 							vBoundsPrev.getMaxX() - vBoundsCurr.getX(), 0);
 
 					selectedGraphics.get(i).vMoveBy(0,
 							vBoundsPrev.getY() - vBoundsCurr.getY());
 
 					// selectedGraphics.get(i).getGmmlData()
 					// .setMTop(
 					// selectedGraphics.get(i - 1).getGmmlData()
 					// .getMTop());
 					// selectedGraphics.get(i).getGmmlData().setMLeft(
 					// selectedGraphics.get(i - 1).getGmmlData()
 					// .getMLeft()
 					// + selectedGraphics.get(i - 1).getGmmlData()
 					// .getMWidth());
 				}
 				break;
 			case BOTTOM:
 				undoManager.newAction("Stack Bottom");
 				Collections.sort(selectedGraphics, new XComparator());
 				for (int i = 1; i < selectedGraphics.size(); i++)
 				{
 					// Get the current and previous graphics objects
 					Graphics eCurr = selectedGraphics.get(i);
 					Graphics ePrev = selectedGraphics.get(i - 1);
 
 					// Get the bounds of the model to view translated shapes
 					Rectangle2D vBoundsPrev = ePrev.getVShape(true)
 							.getBounds2D();
 					Rectangle2D vBoundsCurr = eCurr.getVShape(true)
 							.getBounds2D();
 
 					selectedGraphics.get(i).vMoveBy(
 							vBoundsPrev.getMaxX() - vBoundsCurr.getX(), 0);
 
 					selectedGraphics.get(i).vMoveBy(0,
 							vBoundsPrev.getMaxY() - vBoundsCurr.getMaxY());
 
 					// selectedGraphics.get(i).getGmmlData().setMTop(
 					// selectedGraphics.get(i - 1).getGmmlData().getMTop()
 					// + selectedGraphics.get(i - 1).getGmmlData()
 					// .getMHeight()
 					// - selectedGraphics.get(i).getGmmlData()
 					// .getMHeight());
 					// selectedGraphics.get(i).getGmmlData().setMLeft(
 					// selectedGraphics.get(i - 1).getGmmlData()
 					// .getMLeft()
 					// + selectedGraphics.get(i - 1).getGmmlData()
 					// .getMWidth());
 				}
 				break;
 			}
 			selection.fitToSelection();
 			redrawDirtyRect();
 		}
 	}
 
 	/**
 	 * Scales selected objects either by max width or max height
 	 * 
 	 * @param alignType
 	 */
 	public void scaleSelected(AlignType alignType)
 	{
 
 		List<Graphics> selectedGraphics = getSelectedGraphics();
 		double maxW = 0;
 		double maxH = 0;
 
 		if (selectedGraphics.size() > 0)
 		{
 			Graphics gMax = null;
 
 			switch (alignType)
 			{
 			case WIDTH:
 				for (Graphics g : selectedGraphics)
 				{
 					Rectangle2D r = g.getVShape(true).getBounds2D();
 					double w = Math.abs(r.getWidth());
 					if (w > maxW)
 					{
 						gMax = g;
 						maxW = w;
 					}
 				}
 				for (Graphics g : selectedGraphics)
 				{
 					if (g == gMax)
 						continue;
 
 					Rectangle2D r = g.getVShape(true).getBounds2D();
 					double oldWidth = r.getWidth();
 					if (oldWidth < 0)
 					{
 						r.setRect(r.getX(), r.getY(), -(maxW), r.getHeight());
 						g.setVScaleRectangle(r);
 						g.vMoveBy((oldWidth + maxW) / 2, 0);
 					} else
 					{
 						r.setRect(r.getX(), r.getY(), maxW, r.getHeight());
 						g.setVScaleRectangle(r);
 						g.vMoveBy((oldWidth - maxW) / 2, 0);
 					}
 				}
 				break;
 			case HEIGHT:
 				for (Graphics g : selectedGraphics)
 				{
 					Rectangle2D r = g.getVShape(true).getBounds2D();
 					double h = Math.abs(r.getHeight());
 					if (h > maxH)
 					{
 						gMax = g;
 						maxH = h;
 					}
 				}
 				for (Graphics g : selectedGraphics)
 				{
 					if (g == gMax)
 						continue;
 
 					Rectangle2D r = g.getVShape(true).getBounds2D();
 					double oldHeight = r.getHeight();
 					if (oldHeight < 0)
 					{
 						r.setRect(r.getX(), r.getY(), r.getWidth(), -(maxH));
 						g.setVScaleRectangle(r);
 						g.vMoveBy(0, (maxH + oldHeight) / 2);
 					} else
 					{
 						r.setRect(r.getX(), r.getY(), r.getWidth(), maxH);
 						g.setVScaleRectangle(r);
 						g.vMoveBy(0, (oldHeight - maxH) / 2);
 					}
 				}
 				break;
 			}
 			redrawDirtyRect();
 		}
 	}
 
 	/**
 	 * Get all elements of the class Graphics that are currently selected
 	 * 
 	 * @return
 	 */
 	public List<Graphics> getSelectedGraphics()
 	{
 		List<Graphics> result = new ArrayList<Graphics>();
 		for (VPathwayElement g : drawingObjects)
 		{
 			if (g.isSelected() && g instanceof Graphics
 					&& !(g instanceof SelectionBox))
 			{
 				result.add((Graphics) g);
 			}
 		}
 		return result;
 	}
 	
 	/**
 	 * Get all selected elements (includes non-Graphics, e.g. Handles)
 	 * @return
 	 */
 	public Set<VPathwayElement> getSelectedPathwayElements() {
 		return selection.getSelection();
 	}
 
 	private void generatePasteId(String oldId, Set<String> idset, Map<String, String> idmap,
 			Set<String> newids)
 	{
 		if (oldId != null)
 		{
 			String x;
 			do
 			{
 				/*
 				 * generate a unique id. at the same time, check that it is not
 				 * equal to one of the unique ids that we generated since the
 				 * start of this method
 				 */
 				x = data.getUniqueId(idset);
 			} while (newids.contains(x));
 			newids.add(x); // make sure we don't generate this one
 			// again
 
 			idmap.put(oldId, x);
 		}
 	}
 
 	public void paste(List<PathwayElement> elements) {
 		paste(elements, 0);
 	}
 	
 	public void paste(List<PathwayElement> elements, int shift)
 	{
 		undoManager.newAction("Paste");
 		clearSelection();
 		Map<String, String> idmap = new HashMap<String, String>();
 		Set<String> newids = new HashSet<String>();
 	
 		/*
 		 * Step 1: generate new unique ids for copied items
 		 */
 		for (PathwayElement o : elements)
 		{
 			String id = o.getGraphId();
 			String groupId = o.getGroupId();
 			generatePasteId(id, data.getGraphIds(), idmap, newids);
 			generatePasteId(groupId, data.getGroupIds(), idmap, newids);
 			
 			//For a line, also process the point ids
 			if(o.getObjectType() == ObjectType.LINE) {
 				for(MPoint mp : o.getMPoints())
 					generatePasteId(mp.getGraphId(), data.getGraphIds(), idmap, newids);
 				for(MAnchor ma : o.getMAnchors())
 					generatePasteId(ma.getGraphId(), data.getGraphIds(), idmap, newids);
 			}
 		}
 		/*
 		 * Step 2: do the actual copying
 		 */
 		for (PathwayElement o : elements)
 		{
 			if (o.getObjectType() == ObjectType.INFOBOX)
 			{
 				// these object types we skip,
 				// because they have to be unique in a pathway
 				continue;
 			}
 
 			if (o.getObjectType() == ObjectType.BIOPAX)
 			{
 				// Merge the copied biopax elements with existing
 				data.mergeBiopax(o);
 				continue;
 			}
 
 			lastAdded = null;
 			o.setMStartX(o.getMStartX() + shift * M_PASTE_OFFSET);
 			o.setMStartY(o.getMStartY() + shift * M_PASTE_OFFSET);
 			o.setMEndX(o.getMEndX() + shift * M_PASTE_OFFSET);
 			o.setMEndY(o.getMEndY() + shift * M_PASTE_OFFSET);
 			o.setMLeft(o.getMLeft() + shift * M_PASTE_OFFSET);
 			o.setMTop(o.getMTop() + shift * M_PASTE_OFFSET);
 			
 			// make another copy to preserve clipboard contents for next
 			// paste
 			PathwayElement p = o.copy();
 
 			// set new unique id
 			if (p.getGraphId() != null)
 			{
 				p.setGraphId(idmap.get(p.getGraphId()));
 			}
 			for(MPoint mp : p.getMPoints()) {
 				mp.setGraphId(idmap.get(mp.getGraphId()));
 			}
 			for(MAnchor ma : p.getMAnchors()) {
 				ma.setGraphId(idmap.get(ma.getGraphId()));
 			}
 			// set new group id
 			String gid = p.getGroupId();
 			if (gid != null)
 			{
 				p.setGroupId(idmap.get(gid));
 			}
 			// update graphref
 			String y = p.getStartGraphRef();
 			if (y != null)
 			{
 				if (idmap.containsKey(y))
 				{
 					p.setStartGraphRef(idmap.get(y));
 				} else
 				{
 					p.setStartGraphRef(null);
 				}
 			}
 			y = p.getEndGraphRef();
 			if (y != null)
 			{
 				if (idmap.containsKey(y))
 				{
 					p.setEndGraphRef(idmap.get(y));
 				} else
 				{
 					p.setEndGraphRef(null);
 				}
 			}
 			// update groupref
 			String groupRef = p.getGroupRef();
 			if (groupRef != null)
 			{
 				if (idmap.containsKey(groupRef))
 				{
 					p.setGroupRef(idmap.get(groupRef));
 				} else
 				{
 					p.setGroupRef(null);
 				}
 			}
 			
 			data.add(p); // causes lastAdded to be set
 			lastAdded.select();
 			selection.addToSelection(lastAdded);
 		}
 	}
 
 	public void pasteFromClipboard()
 	{
 		if (isEditMode())
 		{ // Only paste in edit mode
 			parent.pasteFromClipboard();
 		}
 	}
 
 	private List<VPathwayListener> listeners = new ArrayList<VPathwayListener>();
 
 	private List<VPathwayListener> removeListeners = new ArrayList<VPathwayListener>();
 
 	public void addVPathwayListener(VPathwayListener l)
 	{
 		if (!listeners.contains(l))
 			listeners.add(l);
 	}
 
 	public void removeVPathwayListener(VPathwayListener l)
 	{
 		removeListeners.add(l);
 	}
 
 	/**
 	 * Adds a {@link SelectionListener} to the SelectionBox of this VPathway
 	 * 
 	 * @param l
 	 *            The SelectionListener to add
 	 */
 	public void addSelectionListener(SelectionListener l)
 	{
 		selection.addListener(l);
 	}
 
 	/**
 	 * Removes a {@link SelectionListener} from the SelectionBox of this
 	 * VPathway
 	 * 
 	 * @param l
 	 *            The SelectionListener to remove
 	 */
 	public void removeSelectionListener(SelectionListener l)
 	{
 		selection.removeListener(l);
 	}
 
 	private void cleanupListeners()
 	{
 		// Do not remove immediately, to prevent ConcurrentModificationException
 		// when the listener removes itself
 		listeners.removeAll(removeListeners);
 		removeListeners.clear();
 	}
 
 	protected void fireVPathwayEvent(VPathwayEvent e)
 	{
 		cleanupListeners();
 		for (VPathwayListener l : listeners)
 		{
 			l.vPathwayEvent(e);
 		}
 	}
 
 	/**
 	 * helper method to convert view coordinates to model coordinates
 	 */
 	public double mFromV(double v)
 	{
 		return v / zoomFactor;
 	}
 
 	/**
 	 * helper method to convert model coordinates to view coordinates
 	 */
 	public double vFromM(double m)
 	{
 		return m * zoomFactor;
 	}
 
 	private AffineTransform vFromM = new AffineTransform();
 	
 	public java.awt.Shape vFromM(java.awt.Shape s) {
 		vFromM.setToScale(zoomFactor, zoomFactor);
 		return vFromM.createTransformedShape(s);
 	}
 	/**
 	 * Get width of entire Pathway view (taking into account zoom)
 	 */
 	public int getVWidth()
 	{
 		return data == null ? 0 : (int) vFromM(data.getMappInfo()
 				.getMBoardWidth());
 	}
 
 	/**
 	 * Get height of entire Pathway view (taking into account zoom)
 	 */
 	public int getVHeight()
 	{
 		return data == null ? 0 : (int) vFromM(data.getMappInfo()
 				.getMBoardHeight());
 	}
 
 	// AP20070716
 	public class YComparator implements Comparator<Graphics>
 	{
 		public int compare(Graphics g1, Graphics g2)
 		{
 			if (g1.getVCenterY() == g2.getVCenterY())
 				return 0;
 			else if (g1.getVCenterY() < g2.getVCenterY())
 				return -1;
 			else
 				return 1;
 		}
 	}
 
 	public class XComparator implements Comparator<Graphics>
 	{
 		public int compare(Graphics g1, Graphics g2)
 		{
 			if (g1.getVCenterX() == g2.getVCenterX())
 				return 0;
 			else if (g1.getVCenterX() < g2.getVCenterX())
 				return -1;
 			else
 				return 1;
 		}
 	}
 
 	public class ZComparator implements Comparator<Graphics>
 	{
 		public int compare(Graphics g1, Graphics g2)
 		{
 			return g1.gdata.getZOrder() - g2.gdata.getZOrder();
 		}
 	}
 	
 	UndoManager undoManager = new UndoManager();
 
 	/**
 	 * returns undoManager owned by this instance of VPathway.
 	 */
 	public UndoManager getUndoManager()
 	{
 		return undoManager;
 	}
 
 	/*
 	 * To be called only by undo.
 	 */
 	/*
 	 * public void setUndoManager(UndoManager value) { undoManager = value; }
 	 */
 	public void undo()
 	{
 		undoManager.undo();
 	}
 }
