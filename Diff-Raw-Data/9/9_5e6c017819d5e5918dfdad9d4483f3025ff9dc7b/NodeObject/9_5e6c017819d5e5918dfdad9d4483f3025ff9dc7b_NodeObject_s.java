 package de.uniluebeck.itm.spyglass.drawing.primitive;
 
 import java.util.Date;
 
 import org.eclipse.swt.graphics.Color;
 import org.eclipse.swt.graphics.GC;
 import org.eclipse.swt.graphics.Point;
 
 import de.uniluebeck.itm.spyglass.drawing.DrawingObject;
 import de.uniluebeck.itm.spyglass.gui.view.DrawingArea;
 import de.uniluebeck.itm.spyglass.positions.AbsoluteRectangle;
 import de.uniluebeck.itm.spyglass.positions.PixelPosition;
 import de.uniluebeck.itm.spyglass.util.StringFormatter;
 
 // --------------------------------------------------------------------------------
 /**
  * Instances of this class represent a node.
  * 
  * @author Sebastian Ebers
  * 
  */
 public class NodeObject extends DrawingObject {
 	
 	private final int nodeID;
 	
 	private String denotation;
 	
 	private StringFormatter description;
 	
 	private volatile boolean isExtended;
 	
 	private int lineWidth;
 	
 	private AbsoluteRectangle boundingBox;
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * Constructor
 	 * 
 	 * @param nodeID
 	 *            the identifier of the node which is visualized
 	 * @param denotation
 	 *            the denotation of the visualization
 	 */
 	public NodeObject(final int nodeID, final String denotation) {
 		this(nodeID, denotation, null, false);
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * Constructor
 	 * 
 	 * @param nodeID
 	 *            the identifier of the node which is visualized
 	 * @param denotation
 	 *            the denotation of the visualization
 	 * @param description
 	 *            a detailed description (only shown in extended mode)
 	 * @param isExtended
 	 *            indicates if the extended mode is active
 	 */
 	public NodeObject(final int nodeID, final String denotation, final StringFormatter description,
 			final boolean isExtended) {
 		this(nodeID, denotation, description, isExtended, new int[] { 255, 0, 0 }, 1);
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * Constructor
 	 * 
 	 * @param nodeID
 	 *            the identifier of the node which is visualized
 	 * @param denotation
 	 *            the denotation of the visualization
 	 * @param description
 	 *            a detailed description (only shown in extended mode)
 	 * @param isExtended
 	 *            indicates if the extended mode is active
 	 * @param lineColorRGB
 	 *            the line colo's RGB values
 	 * @param lineWidth
 	 *            the line's width
 	 */
 	public NodeObject(final int nodeID, final String denotation, final StringFormatter description,
 			final boolean isExtended, final int[] lineColorRGB, final int lineWidth) {
 		super();
 		super.setId((int) new Date().getTime());
 		this.nodeID = nodeID;
 		this.denotation = denotation;
 		this.description = description;
 		this.isExtended = isExtended;
 		this.lineWidth = lineWidth;
 		this.setColor(lineColorRGB[0], lineColorRGB[1], lineColorRGB[2]);
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * Updates the parameters
 	 * 
 	 * @param denotation
 	 *            the denotation of the visualization
 	 * @param description
 	 *            a detailed description (only shown in extended mode)
 	 * @param isExtended
 	 *            indicates if the extended mode is active
 	 * @param lineColorRGB
 	 *            the line colo's RGB values
 	 * @param lineWidth
 	 *            the line's width
 	 */
 	public void update(final String denotation, final StringFormatter description,
 			final boolean isExtended, final int[] lineColorRGB, final int lineWidth) {
 		this.denotation = denotation;
 		this.description = description;
 		this.isExtended = isExtended;
 		this.lineWidth = lineWidth;
 		this.setColor(lineColorRGB[0], lineColorRGB[1], lineColorRGB[2]);
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @return the denotation
 	 */
 	public String getDenotation() {
 		return denotation;
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @param denotation
 	 *            the denotation to set
 	 */
 	public void setDenotation(final String denotation) {
 		this.denotation = denotation;
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @return the description
 	 */
 	public StringFormatter getDescription() {
 		return description;
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @param description
 	 *            the description to set
 	 */
 	public void setDescription(final StringFormatter description) {
 		this.description = description;
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @return the isExtended
 	 */
 	public boolean isExtended() {
 		return isExtended;
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @param isExtended
 	 *            the isExtended to set
 	 */
 	public void setExtended(final boolean isExtended) {
 		this.isExtended = isExtended;
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @return the lineWidth
 	 */
 	public int getLineWidth() {
 		return lineWidth;
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @param lineWidth
 	 *            the lineWidth to set
 	 */
 	public void setLineWidth(final int lineWidth) {
 		this.lineWidth = lineWidth;
 	}
 	
 	// --------------------------------------------------------------------------------
 	/**
 	 * @return the nodeID
 	 */
 	public int getNodeID() {
 		return nodeID;
 	}
 	
 	@Override
 	public void draw(final DrawingArea drawingArea, final GC gc) {
 		
 		// set the colors and the with of the rectangle's line
 		final Color color = new Color(null, this.getColorR(), this.getColorG(), this.getColorB());
 		final Color bg = new Color(null, this.getBgColorR(), this.getBgColorG(), this.getBgColorB());
 		gc.setForeground(color);
 		gc.setBackground(bg);
 		gc.setLineWidth(lineWidth);
 		
 		final String descriptionString = (description == null) ? "sorry, no additional\r\ninformation available!"
 				: description.toString();
 		
 		// create the string to be displayed
 		final String string = (isExtended) ? denotation + "\r\n" + descriptionString : denotation;
 		
 		// determine the size of the rectangle which represents the node
 		final Point size = gc.textExtent(string);
 		final int width = size.x + lineWidth;
 		final int height = size.y + lineWidth;
 		
 		// get the node's position in the drawing area
 		final PixelPosition upperLeft = drawingArea.absPoint2PixelPoint(this.getPosition());
 		
		setBoundingBox(new AbsoluteRectangle(upperLeft.x, upperLeft.y, width, height));
 		gc.fillRectangle(upperLeft.x, upperLeft.y, width, height);
 		gc.drawRectangle(upperLeft.x, upperLeft.y, width, height);
 		gc.drawText(string, upperLeft.x + lineWidth, upperLeft.y + lineWidth);
 		color.dispose();
 		bg.dispose();
 	}
 	
 	@Override
 	public AbsoluteRectangle getBoundingBox() {
 		return boundingBox;
 	}
 	
 	@Override
 	public void setBoundingBox(final AbsoluteRectangle box) {
 		boundingBox = box;
 	}
 	
 }
