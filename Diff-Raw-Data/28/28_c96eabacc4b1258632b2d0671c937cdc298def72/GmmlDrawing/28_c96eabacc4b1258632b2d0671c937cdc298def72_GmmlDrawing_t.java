 package graphics;
 
 import gmmlVision.GmmlVision;
 
 import java.awt.Rectangle;
 import java.awt.geom.Point2D;
 import java.awt.geom.Rectangle2D;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.KeyEvent;
 import org.eclipse.swt.events.KeyListener;
 import org.eclipse.swt.events.MouseEvent;
 import org.eclipse.swt.events.MouseListener;
 import org.eclipse.swt.events.MouseMoveListener;
 import org.eclipse.swt.events.MouseTrackListener;
 import org.eclipse.swt.events.PaintEvent;
 import org.eclipse.swt.events.PaintListener;
 import org.eclipse.swt.graphics.GC;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.Point;
 import org.eclipse.swt.graphics.RGB;
 import org.eclipse.swt.widgets.Canvas;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Shell;
 
 import visualization.Visualization;
 import visualization.VisualizationManager;
 import visualization.VisualizationManager.VisualizationEvent;
 import visualization.VisualizationManager.VisualizationListener;
 import data.GmmlData;
 import data.GmmlDataObject;
 import data.GmmlEvent;
 import data.GmmlListener;
 import data.LineStyle;
 import data.LineType;
 import data.ObjectType;
 import data.OrientationType;
 import data.ShapeType;
 
 /**
  * This class implements and handles a drawing.
  * GmmlGraphics objects are stored in the drawing and can be 
  * visualized. The class also provides methods for mouse  and key
  * event handling.
  */
 public class GmmlDrawing extends Canvas implements MouseListener, MouseMoveListener, 
 PaintListener, MouseTrackListener, KeyListener, GmmlListener, VisualizationListener
 {	
 	private static final long serialVersionUID = 1L;
 		
 	/**
 	 * All objects that are visible on this mapp, including the handles
 	 * but excluding the legend, mappInfo and selectionBox objects
 	 */
 	private ArrayList<GmmlDrawingObject> drawingObjects;
 	public ArrayList<GmmlDrawingObject> getDrawingObjects() { return drawingObjects; }
 	
 	/**
 	 * The {@link GmmlDrawingObject} that is pressed last mouseDown event}
 	 */
 	GmmlDrawingObject pressedObject	= null;	
 	
 	/**
 	 * The {@link GmmlGraphics} that is directly selected since last mouseDown event
 	 */
 	public GmmlGraphics selectedGraphics = null;
 	
 	/**
 	 * {@link GmmlInfoBox} object that contains information about this pathway,
 	 * currently only used for information in {@link gmmlVision.GmmlPropertyTable}
 	 * (TODO: has to be implemented to behave the same as any GmmlGraphics object
 	 * when displayed on the drawing)
 	 */
 	GmmlInfoBox infoBox;
 	GmmlData data;
 	public GmmlData getGmmlData () { return data; }
 	
 	GmmlSelectionBox s; 
 		
 	private boolean editMode;
 	/**
 	 * Checks if this drawing is in edit mode
 	 * @return false if in edit mode, true if not
 	 */
 	public boolean isEditMode() { return editMode; }
 	
 	/**
 	 * Maps the contents of a pathway to a GmmlDrawing
 	 */	
 	public void fromGmmlData(GmmlData _data)
 	{		
 		data = _data;
 			
 		for (GmmlDataObject o : data.dataObjects)
 		{
 			switch (o.getObjectType())
 			{
 				case ObjectType.BRACE: new GmmlBrace(this, o); break;
 				case ObjectType.GENEPRODUCT: new GmmlGeneProduct(this, o); break;
 				case ObjectType.SHAPE: new GmmlShape(this, o); break;
 				case ObjectType.LINE: new GmmlLine(this, o); break;
 				case ObjectType.MAPPINFO: 
 					GmmlInfoBox mi = new GmmlInfoBox(this, o);
 					addObject(mi); 
 					setMappInfo(mi); 
 					break;				
 				case ObjectType.LABEL: new GmmlLabel(this, o); break;					
 			}
 						
 		}
 		setSize(getMappInfo().getBoardSize());
 		data.fireObjectModifiedEvent(new GmmlEvent(null, GmmlEvent.MODIFIED_GENERAL));
 		data.addListener(this);
 	}
 
 	private int newGraphics = NEWNONE;
 	/**
 	 * Method to set the new graphics type that has to be added next time the user clicks on the
 	 * drawing. 
 	 * @param type One of the NEWXX fields of this class, where XX stands for the type of graphics to draw
 	 */
 	public void setNewGraphics(int type) { newGraphics = type; }
 	
 	private Rectangle dirtyRect = null;
 	/**
 	 * Adds object boundaries to the 'dirty rectangle', which marks the area that needs to be redrawn
 	 * @param g	drawing object of which the boundaries have to be added
 	 */
 	public void addDirtyRect(Rectangle r)
 	{
 		if(r == null) { //In case r is null, add whole drawing
 			org.eclipse.swt.graphics.Rectangle b = getBounds();
 			r = new Rectangle(b.x, b.y, b.width, b.height);
 		}
 		if(dirtyRect == null)
 			dirtyRect = r;
 		else
 			dirtyRect.add(r);	
 	}
 	
 	/**
 	 * Redraw parts marked dirty
 	 * reset dirty rect afterwards
 	 */
 	public void redrawDirtyRect()
 	{
 		if (dirtyRect != null)
 			redraw (dirtyRect.x, dirtyRect.y, dirtyRect.width + 1, dirtyRect.height + 1, false);
 		dirtyRect = null;
 	}
 	
 	/**
 	 *Constructor for this class
 	 */	
 	public GmmlDrawing(Composite parent, int style)
 	{
 		super (parent, style);
 		
 		drawingObjects	= new ArrayList<GmmlDrawingObject>();
 		
 		s = new GmmlSelectionBox(this);
 		
 		addMouseListener(this);
 		addMouseMoveListener(this);
 		addPaintListener (this);
 		addMouseTrackListener(this);
 		addKeyListener(this);
 		VisualizationManager.addListener(this);
 	}
 		
 	/**
 	 * Sets the {@link MappInfo} containing information on the pathway
 	 * @param mappInfo
 	 */
 	public void setMappInfo(GmmlInfoBox mappInfo)
 	{
 		this.infoBox = mappInfo;
		infoBox.getGmmlData().addListener(this);
 	}
 
 	/**
 	 * Gets the {@link MappInfo} containing information on the pathway
 	 * @return
 	 */
 	public GmmlInfoBox getMappInfo() { return infoBox; }
 		
 	/**
 	 * Adds an element to the drawing
 	 * @param o the element to add
 	 */
 	public void addObject(GmmlDrawingObject o)
 	{
 		if(!drawingObjects.contains(o)) { //Don't add duplicates!
 			drawingObjects.add(o);
 		}
 		
 	}
 
 	/**
 	 * Get the gene identifiers of all genes in this pathway
 	 * @return	{@link ArrayList<String>} containing an identifier for every gene on the mapp
 	 */
 	public ArrayList<String> getMappIds()
 	{
 		ArrayList<String> mappIds = new ArrayList<String>();
 		for(GmmlDrawingObject o : drawingObjects)
 		{
 			if(o instanceof GmmlGeneProduct)
 			{
 				mappIds.add(((GmmlGeneProduct)o).getID());
 			}
 		}
 		return mappIds;
 	}
 	
 	/**
 	 * Get the systemcodes of all genes in this pathway
 	 * @return	{@link ArrayList<String>} containing a systemcode for every gene on the mapp
 	 */
 	public ArrayList<String> getSystemCodes()
 	{
 		ArrayList<String> systemCodes = new ArrayList<String>();
 		for(GmmlDrawingObject o : drawingObjects)
 		{
 			if(o instanceof GmmlGeneProduct)
 			{
 				systemCodes.add(((GmmlGeneProduct)o).getSystemCode());
 			}
 		}
 		return systemCodes;
 	}
 	
 	/**
 	 * Set this drawing to editmode
 	 * @param editMode	true if editmode has to be enabled, false if disabled (view mode)
 	 */
 	public void setEditMode(boolean editMode)
 	{
 		this.editMode = editMode;
 		if(!editMode)
 		{
 			clearSelection();
 		}
 		GmmlVision.getWindow().showLegend(!editMode);	
 		redraw();
 	}
 	
 	private double zoomFactor = 1;
 	/**
 	 * Get the current zoomfactor used
 	 * @return	the current zoomfactor
 	 */
 	public double getZoomFactor() { return zoomFactor; }
 	
 	/**
 	 * Sets the drawings zoom in percent
 	 * @param pctZoomFactor zoomfactor in percent
 	 */
 	public void setPctZoom(double pctZoomFactor)
 	{
 		double factor = 0.01*pctZoomFactor/zoomFactor;
 		zoomFactor = pctZoomFactor / 100;
 		setSize((int)(getSize().x * factor), (int)(getSize().y * factor));
 		
 		// iterate over all graphics to adjust them
 		for(GmmlDrawingObject o : drawingObjects)
 		{
 			if (o instanceof GmmlGraphics)
 			{
 				GmmlGraphics g = (GmmlGraphics) o;
 				g.adjustToZoom(factor);		
 			}
 		}
 		
 		redraw();
 	}
 
 	public void setPressedObject(GmmlDrawingObject o) {
 		pressedObject = o;
 	}
 	
 	int previousX;
 	int previousY;
 	boolean isDragging;
 	/**
 	 * handles mouse movement
 	 */
 	public void mouseMove(MouseEvent e)
 	{
 		// Dispose the tooltip if shown
 		if(tip != null)
 		{
 			if(!tip.isDisposed()) tip.dispose();
 		}
 		// If draggin, drag the pressed object
 		if (pressedObject != null && isDragging)
 		{
 			pressedObject.moveBy(e.x - previousX, e.y - previousY);
 			
 			previousX = e.x;
 			previousY = e.y;
 			redrawDirtyRect();
 		}
 	}
 	/**
 	 * Handles mouse Pressed input
 	 */
 	//TODO: Variable ctrlPressed is not set to false when editting labels.
 	//		Probably the keyRelease event is not picked up.
     public boolean firstClick = true;
 	public void mouseDown(MouseEvent e)
 	{		
 		setFocus();
 		if (editMode)
 		{
 			if (newGraphics != NEWNONE && (ctrlPressed || firstClick))
 			{
 				newObject(new Point(e.x, e.y));
 				if (!ctrlPressed && !firstClick)
 				{
 					GmmlVision.getWindow().deselectNewItemActions();
 				}
 				firstClick =false;
 			}
 			else
 			{
 				editObject(new Point(e.x, e.y));
 			}
 		}
 		else
 		{
 			mouseDownViewMode(e);
 		}
 
 	}
 	
 	/**
 	 * Handles mouse Released input
 	 */
 	public void mouseUp(MouseEvent e)
 	{
 		if(isDragging)
 		{
 			if(s.isSelecting()) { //If we were selecting, stop it
 				s.stopSelecting();
 			}
 			redrawDirtyRect();
 		}
 		isDragging = false;
 	}
 	
 	/**
 	 * Handles mouse entered input
 	 */
 	public void mouseDoubleClick(MouseEvent e) {	}
 
 	/**
 	 * Paints all components in the drawing.
 	 * This method is called automatically in the 
 	 * painting process
 	 */
 	public void paintControl (PaintEvent e)
 	{		
 		Image image = (Image)getData("double-buffer-image");
 		// create an image for double-buffering, if it doesn't exist 
 		// or the component has been resized
 		if(image == null
 				|| image.getBounds().width != getSize().x
 				|| image.getBounds().height != getSize().y)
 		{
 			image = new Image(getDisplay(), getSize().x, getSize().y);
 			setData("double-buffer-image", image);
 		}
 
 		GC buffer = new GC(image);
 		buffer.setBackground(e.display.getSystemColor(SWT.COLOR_WHITE));
 		buffer.fillRectangle(e.x, e.y, e.width, e.height);
 		
 		buffer.setAntialias(SWT.ON);
 		
 		Rectangle2D.Double r = new Rectangle.Double(e.x, e.y, e.width, e.height);
 		    	
 		Collections.sort(drawingObjects);
 		
 		Visualization v = VisualizationManager.getCurrent();
 		for(GmmlDrawingObject o : drawingObjects)
 		{
 			if(o.intersects(r))
 			{
 				if(checkDrawAllowed(o)) {
 					o.draw (e, buffer);
 				}
 				
 				if(v != null && o instanceof GmmlGraphics) {
 						try {
 							v.drawToObject((GmmlGraphics) o, e, buffer);
 						} catch(Exception ex) {
 							GmmlVision.log.error(
 									"Unable to apply visualization " + v + " on " + o, ex);
 							ex.printStackTrace();
 						}
 				}
 				if(o instanceof GmmlGeneProduct) ((GmmlGeneProduct)o).drawHighlight(e, buffer);
 			}
 		}
 		
 		e.gc.drawImage(image, 0, 0);
 		buffer.dispose();
 	}
 
 	boolean checkDrawAllowed(GmmlDrawingObject o) {
 		if(isEditMode()) return true;
 		else return !(	o instanceof GmmlHandle ||
 						(o == s && !isDragging)
 					);
 	}
 
 	/**
 	 * deselect all elements on the drawing
 	 */
 	private void clearSelection()
 	{
 		for(GmmlDrawingObject o : drawingObjects) o.deselect(); //Deselect all objects
 		s.reset();
 	}
 
 	/**
 	 * Handles event when on mouseDown in case the drawing is in view mode
 	 * (does nothing yet)
 	 * @param e	the mouse event to handle
 	 */
 	private void mouseDownViewMode(MouseEvent e) 
 	{
 		Point2D p2d = new Point2D.Double(e.x, e.y);
 
 		pressedObject = findPressedObject(p2d);
 		
 		if (pressedObject != null)
 			doClickSelect(p2d);
 		else
 			startSelecting(p2d);
 	}
 	
 	/**
 	 * Initializes selection, resetting the selectionbox
 	 * and then setting it to the position specified
 	 * @param p - the point to start with the selection
 	 */
 	private void startSelecting(Point2D p)
 	{
 		previousX = (int)p.getX();
 		previousY = (int)p.getY();
 		isDragging = true;
 		
 		clearSelection();
 		s.reset(p.getX(), p.getY());
 		s.startSelecting();
 		pressedObject = s.getCornerHandle();
 	}
 		
 	/**
 	 * Resets highlighting, unhighlights all GmmlDrawingObjects
 	 */
 	public void resetHighlight() 
 	{
 		for(GmmlDrawingObject o : drawingObjects) o.unhighlight();
 		redraw();
 	}
 	
 	/**
 	 * Called by MouseDown, when we're in editting mode and we're not adding new objects
 	 * prepares for dragging the object
 	 */
 	private void editObject(Point p)
 	{
 		Point2D p2d = new Point2D.Double(p.x, p.y);
 		
 		pressedObject = findPressedObject(p2d);
 		
 		// if we clicked on an object
 		if (pressedObject != null)
 		{
 			// if our object is an handle, select also it's parent.
 			if(pressedObject instanceof GmmlHandle)
 			{
 				((GmmlHandle)pressedObject).parent.select();
 			} else {
 				doClickSelect(p2d);
 			}
 			
 			// start dragging
 			previousX = p.x;
 			previousY = p.y;
 			
 			isDragging = true;		
 		}
 		else
 		{
 			// start dragging selectionbox	
 			startSelecting(p2d);
 		}		
 	}
 
 	GmmlDrawingObject findPressedObject(Point2D p2d) {
 		Collections.sort(drawingObjects);
 		GmmlDrawingObject probj = null;
 		for (GmmlDrawingObject o : drawingObjects)
 		{
 			if (o.isContain(p2d))
 			{
 				// select this object, unless it is an invisible gmmlHandle
 				if (o instanceof GmmlHandle && !((GmmlHandle)o).isVisible()) 
 					;
 				else 
 					probj = o;
 			}
 		}
 		return probj;
 	}
 	
 	void doClickSelect(Point2D p2d) {
 		//Ctrl pressed, add/remove from selection
 		if(ctrlPressed) 
 		{
 			if(pressedObject instanceof GmmlSelectionBox) {
 				//Object inside selectionbox clicked, pass to selectionbox
 				s.objectClicked(p2d);
 			}
 			else if(pressedObject.isSelected()) { //Already in selection: remove
 				s.removeFromSelection(pressedObject);
 			} else {
 				s.addToSelection(pressedObject); //Not in selection: add
 			}
 			pressedObject = null; //Disable dragging
 		} 
 		else //Ctrl not pressed
 		{
 			//If pressedobject is not selectionbox:
 			//Clear current selection and select pressed object
 			if(!(pressedObject instanceof GmmlSelectionBox))
 			{
 				clearSelection();
 				s.addToSelection(pressedObject);
 			} else { //Check if clicked object inside selectionbox
 				if(s.getChild(p2d) == null) clearSelection();
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
 	public static final RGB stdRGB = new RGB(0, 0, 0);
 
 	/**
 	 * Add a new object to the drawing
 	 * {@see GmmlDrawing#setNewGraphics(int)}
 	 * @param p	The point where the user clicked on the drawing to add a new graphics
 	 */
 	private void newObject(Point e)
 	{
 		GmmlDataObject gdata = null;
 		GmmlGraphics g = null;
 		GmmlHandle h = null;
 		GmmlLine l = null;
 		switch(newGraphics) {
 		case NEWNONE:
 			return;
 		case NEWLINE:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.SOLID);
 			gdata.setLineType (LineType.LINE);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();
 			isDragging = true;
 			break;
 		case NEWLINEARROW:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.SOLID);
 			gdata.setLineType (LineType.ARROW);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();
 			isDragging = true;
 			break;
 		case NEWLINEDASHED:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.DASHED);
 			gdata.setLineType (LineType.LINE);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();
 			isDragging = true;
 			break;
 		case NEWLINEDASHEDARROW:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.DASHED);
 			gdata.setLineType (LineType.ARROW);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();
 			isDragging = true;
 			break;
 		case NEWLABEL:
 			gdata = new GmmlDataObject(ObjectType.LABEL);
 			gdata.setCenterX(e.x);
 			gdata.setCenterY(e.y);
 			gdata.setWidth((GmmlLabel.INITIAL_WIDTH * zoomFactor));
 			gdata.setHeight((GmmlLabel.INITIAL_HEIGHT * zoomFactor));
 			gdata.setFontSize (GmmlLabel.INITIAL_FONTSIZE);
 			g = new GmmlLabel (this, gdata);
 			((GmmlLabel)g).createTextControl();
 			h = null;
 			break;
 		case NEWARC:
 			gdata = new GmmlDataObject(ObjectType.SHAPE);
 			gdata.setShapeType(ShapeType.ARC);
 			gdata.setCenterX (e.x);
 			gdata.setCenterY (e.y);
 			gdata.setWidth(1);
 			gdata.setHeight(1);
 			gdata.setColor(stdRGB);
 			gdata.setRotation (0);
 			g = new GmmlShape(this, gdata);
 			h = ((GmmlShape)g).handleSE;
 			isDragging = true;
 			break;
 		case NEWBRACE:
 			gdata = new GmmlDataObject(ObjectType.BRACE);
 			gdata.setCenterX(e.x);
 			gdata.setCenterY(e.y);
 			gdata.setWidth(1);
 			gdata.setHeight(1);
 			gdata.setOrientation(OrientationType.RIGHT);
 			gdata.setColor(stdRGB);
 			g = new GmmlBrace(this, gdata);
 			h = ((GmmlBrace)g).handleSE;
 			isDragging = true;
 			break;
 		case NEWGENEPRODUCT:
 			gdata = new GmmlDataObject(ObjectType.GENEPRODUCT);
 			gdata.setCenterX(e.x);
 			gdata.setCenterY(e.y);
 			gdata.setWidth(GmmlGeneProduct.INITIAL_WIDTH * zoomFactor);
 			gdata.setHeight(GmmlGeneProduct.INITIAL_HEIGHT * zoomFactor);
 			gdata.setGeneID("Gene");
 			gdata.setXref("");
 			gdata.setColor(stdRGB);
 			g = new GmmlGeneProduct (this, gdata);
 			h = null;
 			break;
 		case NEWRECTANGLE:
 			gdata = new GmmlDataObject(ObjectType.SHAPE);
 			gdata.setShapeType(ShapeType.RECTANGLE);
 			gdata.setCenterX (e.x);
 			gdata.setCenterY (e.y);
 			gdata.setWidth(1);
 			gdata.setHeight(1);
 			gdata.setColor(stdRGB);
 			gdata.setRotation (0);
 			g = new GmmlShape(this, gdata);
 			h = ((GmmlShape)g).handleSE;
 			isDragging = true;
 			break;
 		case NEWOVAL:
 			gdata = new GmmlDataObject(ObjectType.SHAPE);
 			gdata.setShapeType(ShapeType.OVAL);
 			gdata.setCenterX (e.x);
 			gdata.setCenterY (e.y);
 			gdata.setWidth(50 * zoomFactor);
 			gdata.setHeight(50 * zoomFactor);
 			gdata.setColor(stdRGB);
 			gdata.setRotation (0);
 			g = new GmmlShape(this, gdata);
 			h = ((GmmlShape)g).handleSE;
 			isDragging = true;
 			break;
 		case NEWTBAR:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.SOLID);
 			gdata.setLineType (LineType.TBAR);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();						
 			isDragging = true;
 			break;
 		case NEWRECEPTORROUND:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.SOLID);
 			gdata.setLineType (LineType.RECEPTOR_ROUND);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();						
 			isDragging = true;
 			break;
 		case NEWRECEPTORSQUARE:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.SOLID);
 			gdata.setLineType (LineType.RECEPTOR_SQUARE);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();						
 			isDragging = true;
 			break;
 		case NEWLIGANDROUND:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.SOLID);
 			gdata.setLineType (LineType.LIGAND_ROUND);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();						
 			isDragging = true;
 			break;
 		case NEWLIGANDSQUARE:
 			gdata = new GmmlDataObject(ObjectType.LINE);
 			gdata.setStartX(e.x);
 			gdata.setStartY(e.y);
 			gdata.setEndX(e.x);
 			gdata.setEndY(e.y);	
 			gdata.setColor (stdRGB);
 			gdata.setLineStyle (LineStyle.SOLID);
 			gdata.setLineType (LineType.LIGAND_SQUARE);
 			g = l = new GmmlLine(this, gdata);
 			h = l.getHandleEnd();						
 			isDragging = true;
 			break;
 		}
 				
 		clearSelection();
 		s.addToSelection(g);
 		pressedObject = h;
 		
 		previousX = e.x;
 		previousY = e.y;
 		
 		if(gdata != null) {
 			gdata.setParent(GmmlVision.getGmmlData());
 			GmmlVision.getGmmlData().fireObjectModifiedEvent(new GmmlEvent(gdata, GmmlEvent.ADDED));
 		}
 		
 	}
 	
 
 	public static final int DRAW_ORDER_HANDLE = 0;
 	public static final int DRAW_ORDER_SELECTIONBOX = 1;
 	public static final int DRAW_ORDER_SELECTED = 2;
 	public static final int DRAW_ORDER_GENEPRODUCT = 3;
 	public static final int DRAW_ORDER_LABEL = 4;
 	public static final int DRAW_ORDER_LINE = 5;
 	public static final int DRAW_ORDER_ARC = 6;
 	public static final int DRAW_ORDER_BRACE = 7;
 	public static final int DRAW_ORDER_LINESHAPE = 8;
 	public static final int DRAW_ORDER_SHAPE = 9;
 	public static final int DRAW_ORDER_MAPPINFO = 10;
 	public static final int DRAW_ORDER_DEFAULT = 11;
 	
 	public void mouseEnter(MouseEvent e) {}
 
 	public void mouseExit(MouseEvent e) {}
 
 	Shell tip;
 	
 	/**
 	 * Responsible for drawing a tooltip displaying expression data when 
 	 * hovering over a geneproduct
 	 */
 	public void mouseHover(MouseEvent e) {
 		Visualization v = VisualizationManager.getCurrent();
 		if(v != null && v.usesToolTip()) {
 			Point2D p = new Point2D.Double(e.x, e.y);
 			
 			GmmlDrawingObject o = findPressedObject(p);
 			if(o != null && o instanceof GmmlGraphics) {
 				System.out.println(o);
 				tip = v.getToolTip(e.display, (GmmlGraphics)o);
 				if(tip == null) return;
 				Point mp = toDisplay(e.x, e.y);
 				tip.setLocation(mp.x + 15, mp.y + 15);
 	            tip.setVisible(true);
 			}
 		}
 	}
 
 	private boolean ctrlPressed;
 	private void ctrlPressed() 	{ ctrlPressed = true; 	}
 	private void ctrlReleased() 	{ ctrlPressed = false; 	}
 	
 	public void keyPressed(KeyEvent e) { 
 		if(e.keyCode == SWT.CTRL) ctrlPressed();
 	}
 
 	public void keyReleased(KeyEvent e) {
 		ArrayList<GmmlDrawingObject> toRemove = new ArrayList<GmmlDrawingObject>();
 		
 		if(e.keyCode == SWT.CTRL) ctrlReleased();
 		if(e.keyCode == SWT.DEL) {
 			for(GmmlDrawingObject o : drawingObjects)
 			{
 				if(!o.isSelected() || o == s || o == infoBox) continue; //Object not selected, skip
 				toRemove.add(o);
 				if(o instanceof GmmlGraphics) //Also add handles
 				{
 					for(GmmlHandle h : ((GmmlGraphics)o).getHandles()) toRemove.add(h);
 				}
 			}
 			removeDrawingObjects(toRemove);
 		}
 		if(e.keyCode == SWT.HOME) {
 			System.out.println("================");
 			Collections.sort(drawingObjects);
 			for(GmmlDrawingObject o : drawingObjects) {
 				System.out.println(o.toString() + "\t" + o.isSelected() + "\t" + o.drawingOrder + "\t");
 				if(o instanceof GmmlGraphics) {
 					System.out.println("\t is GmmlGraphics\t" + ((GmmlGraphics)o).getGmmlData().getObjectType());
 				}
 			}
 		}
 	}
 	
 	/**
 	 * Removes the {@link GmmlDrawingObject}s in the {@link ArrayList} from the drawing
 	 * @param toRemove	The {@link ArrayList<GmmlDrawingObject>} containing the objects to be removed
 	 */
 	public void removeDrawingObjects(ArrayList<GmmlDrawingObject>toRemove)
 	{
 		for(GmmlDrawingObject o : toRemove)
 		{
 			drawingObjects.remove(o); //Remove from drawing
 			s.removeFromSelection(o); //Remove from selection
 			if(o instanceof GmmlGraphics) {
 				GmmlGraphics g = (GmmlGraphics)o;
 				GmmlVision.getGmmlData().fireObjectModifiedEvent(new GmmlEvent(g.getGmmlData(), GmmlEvent.DELETED));
 				g.getGmmlData().removeListener(this);
 			}
 			
 		}
 		s.fitToSelection();
 	}
 
 	public void gmmlObjectModified(GmmlEvent e) {
 		switch (e.getType())
 		{
 			case GmmlEvent.DELETED:
 				GmmlVision.getGmmlData().dataObjects.remove((GmmlDataObject)e.getAffectedData());
 				addDirtyRect(null); // mark everything dirty
 				break;
 			case GmmlEvent.ADDED:
 				GmmlVision.getGmmlData().dataObjects.add((GmmlDataObject)e.getAffectedData());
 				addDirtyRect(null); // mark everything dirty
 				break;
 			case GmmlEvent.WINDOW:
 				setSize((int)infoBox.getGmmlData().getBoardWidth(), 
 						(int)infoBox.getGmmlData().getBoardHeight());
 		}
 		redrawDirtyRect();
 	}
 	
 	List<GmmlDataObject> clipboard = null;
 	
 	public void copyToClipboard()
 	{
 		List<GmmlDataObject> result = new ArrayList<GmmlDataObject>();
 		for (GmmlDrawingObject g : drawingObjects)
 		{
 			if (g.isSelected() && g instanceof GmmlGraphics)
 			{
 				result.add(((GmmlGraphics)g).gdata.copy(null));
 			}
 		}
 		if (result.size() > 0)
 		{
 			clipboard = result;
 		}
 		else
 		{
 			clipboard = null;
 		}
 	}
 	
 	public void pasteFromClipboad()
 	{
 		if (clipboard != null)
 		{
 			for (GmmlDataObject o : clipboard)
 			{
 				o.setStartX(o.getStartX() + 10);
 				o.setStartY(o.getStartY() + 10);
 				o.setEndX(o.getEndX() + 10);
 				o.setEndY(o.getEndY() + 10);
 				o.setLeft(o.getLeft() + 10);
 				o.setTop(o.getTop() + 10);
 				data.dataObjects.add(o);
 				GmmlVision.getGmmlData().fireObjectModifiedEvent(new GmmlEvent(o, GmmlEvent.ADDED));
 				// TODO: firing of event doesn't lead to creation of GmmlGrahics.
 				// modify structure to make GmmlGraphicsObject get created automatically
 				// after an "ADD" event is fired.
 			}
 		}
 	}
 
 	public void visualizationEvent(VisualizationEvent e) {
 		switch(e.type) {
 		case(VisualizationEvent.COLORSET_MODIFIED):
 		case(VisualizationEvent.VISUALIZATION_SELECTED):
 		case(VisualizationEvent.VISUALIZATION_MODIFIED):
 		case(VisualizationEvent.PLUGIN_MODIFIED):
 			redraw();
 		}
 	}	
 } // end of class
