 package geogebra3D.euclidian3D;
 
 
 
 import geogebra.Matrix.GgbMatrix4x4;
 import geogebra.Matrix.GgbVector;
 import geogebra.euclidian.DrawableND;
 import geogebra.kernel.GeoElement;
 import geogebra.kernel.kernelND.GeoPointND;
 import geogebra.main.Application;
 import geogebra3D.euclidian3D.opengl.Renderer;
 import geogebra3D.kernel3D.GeoElement3D;
 import geogebra3D.kernel3D.GeoElement3DInterface;
 import geogebra3D.kernel3D.GeoPoint3D;
 
 import java.awt.Graphics2D;
 import java.text.DecimalFormat;
 import java.util.Comparator;
 import java.util.TreeSet;
 
 
 
 /**
  * 3D representation of a {@link GeoElement3D}
  * 
  * 
  * <h3> How to create the drawable of a new element </h3>
  * 
  * We'll call here our new element "GeoNew3D" and create a drawable3D linked to it:
  * <ul>
 
     <li> It extends {@link Drawable3DCurves} (for points, lines, ...) 
          or {@link Drawable3DSurfaces} (for planes, surfaces, ...)
          <p>
          <code>
          public class DrawNew3D extends ... {
          </code> 
 	</li>
     <li> Create new constructor
          <p>
          <code>
          public DrawNew3D(EuclidianView3D a_view3d, GeoNew3D a_new3D){ <br> &nbsp;&nbsp;
             super(a_view3d, a_new3D); <br> 
          }
          </code>
 	</li>
     <li> <b> NOTE: </b>  a Drawable3D uses the {@link GeoElement3D#getDrawingMatrix()} method to know where to draw itself
     </li>
     <li> Eclipse will add auto-generated methods :
          <ul>
          <li> getPickOrder() : for picking objects order ; use {@link #DRAW_PICK_ORDER_MAX} first
               <p>
               <code>
                   public int getPickOrder() { <br> &nbsp;&nbsp;
                         return DRAW_PICK_ORDER_MAX; <br> 
                   }
               </code>
          </li>
          <li> for {@link Drawable3DCurves} :
               <p>
               <code>
                 public void drawGeometry(EuclidianRenderer3D renderer) { <br> &nbsp;&nbsp;
             	       // call the geometry to be drawn <br>
             	}
             	<br>
             	public void drawGeometryHidden(EuclidianRenderer3D renderer) { <br> &nbsp;&nbsp;
             	       // for hidden part, let it empty first <br>
             	}
             	<br>
             	public void drawGeometryPicked(EuclidianRenderer3D renderer) { <br> &nbsp;&nbsp;
             	       // to show the object is picked, let it empty first <br>
             	}
               </code>
 		 </li>
          <li> for {@link Drawable3DSurfaces} :
               <p>
               <code>
             public void drawGeometry(EuclidianRenderer3D renderer) { <br> &nbsp;&nbsp;
                     // call the geometry to be drawn <br>
             }
             <br>
 	        void drawGeometryHiding(EuclidianRenderer3D renderer) { <br> &nbsp;&nbsp;
 	           // call the geometry that hides other objects <br>&nbsp;&nbsp;
                    // first sets it to :  <br>&nbsp;&nbsp;
                    drawGeometry(renderer);      <br>
 	        }
 	        <br>
 	        public void drawGeometryHidden(EuclidianRenderer3D renderer) { <br> &nbsp;&nbsp;
 	           // for hidden part, let it empty first   <br> 
 	        }
 	        <br>
 	        public void drawGeometryPicked(EuclidianRenderer3D renderer) { <br> &nbsp;&nbsp;
                    // to show the object is picked, let it empty first <br>
 	        }
 	      </code>
 	      </li>
 	      </ul>
 	</li>
 	</ul>
 	
 	<h3> See </h3> 
 	<ul>
 	<li> {@link EuclidianView3D#createDrawable(GeoElement)} 
 	     to make the drawable be created when the GeoElement is created 
 	</li>
 	</ul>
 
  * 
  * @author ggb3D
  * 
  *
  * 
  * 
  *
  */
 public abstract class Drawable3D extends DrawableND {
 
 	
 	private static final boolean DEBUG = false;
 
 	
 	//constants for rendering
 	/** objects that are picked are drawn with a thickness * PICKED_DILATATION*/
 	protected static final float PICKED_DILATATION = 1.3f;	
 	/** default radius for drawing 3D points*/
 	//protected static final float POINT3D_RADIUS = 1.2f;
 	/** points on a path are a little bit more bigger than others */
 	protected static final float POINT_ON_PATH_DILATATION = 1.01f;
 	/** default thickness of 3D lines, segments, ... */
 	//protected static final float LINE3D_THICKNESS = 0.5f;
 	/** default thickness of lines of a 3D grid ... */	
 	protected static final float GRID3D_THICKNESS = 0.005f;
 		
 	
 	
 	/** view3D */
 	private EuclidianView3D m_view3D; 
 	
 	
 	/** says if it has to be updated */
 	private boolean waitForUpdate;
 	
 	/** says if it has to be updated caused by the 3D view*/
 	private boolean viewChanged;
 	
 	/** says if the label has to be updated */
 	private boolean labelWaitForUpdate;
 	
 	/** says if this has to be reset */
 	protected boolean waitForReset;
 	
 	/** gl index of the geometry */
 	private int geomIndex = -1;
 	
 	//links to the GeoElement
 	private GeoElement geo; 
 	
 	/** label */
 	protected DrawLabel3D label;
 
 	//picking
 	//private boolean m_isPicked = false;	
 	/** max picking value, used for odering elements with openGL picking */
 	public float zPickMax; 
 	/** min picking value, used for odering elements with openGL picking */	
 	public float zPickMin; 
 	private static final float EPSILON_Z = 0.0001f;//0.0001f;//10000000; //limit to consider two objects to be at the same place
 	
 	//constants for picking : have to be from 0 to DRAW_PICK_ORDER_MAX-1, regarding to picking order
 	/** default value for picking order */
 	static final public int DRAW_PICK_ORDER_MAX = 3;
 	/** picking order value for 0-Dimensional objects (points) */
 	static final public int DRAW_PICK_ORDER_0D = 0; 
 	/** picking order value for 1-Dimensional objects (lines, segments, ...) */
 	static final public int DRAW_PICK_ORDER_1D = 1; 
 	/** picking order value for 2-Dimensional objects (polygons, planes, ...) */
 	static final public int DRAW_PICK_ORDER_2D = 2; 
 	
 	
 	
 
 	//type constants
 	/** type for drawing default (GeoList, ...) */
 	public static final int DRAW_TYPE_DEFAULT = 0;
 	/** type for drawing points */
 	public static final int DRAW_TYPE_POINTS = DRAW_TYPE_DEFAULT+1;
 	/** type for drawing lines, circles, etc. */
 	public static final int DRAW_TYPE_CURVES = DRAW_TYPE_POINTS+1;
 	/** type for drawing planes, polygons, etc. */
 	public static final int DRAW_TYPE_SURFACES = DRAW_TYPE_CURVES+1;
 	/** type for drawing polyhedrons, quadrics, etc. */
 	public static final int DRAW_TYPE_CLOSED_SURFACES = DRAW_TYPE_SURFACES+1;
 	/** number max of drawing types */
 	public static final int DRAW_TYPE_MAX = DRAW_TYPE_CLOSED_SURFACES+1;
 	
 	
 	
 	
 	///////////////////////////////////////////////////////////////////////////////
 	// constructors
 	
 	/**
 	 * construct the Drawable3D with a link to a_view3D
 	 * @param view3D the view linked to this
 	 */
 	public Drawable3D(EuclidianView3D view3D){
 		setView3D(view3D);
 		
 		label = new DrawLabel3D(view3D);
 	}
 	
 		
 	/** 
 	 * Call the {@link #update()} method.
 	 * @param a_view3D the {@link EuclidianView3D} using this Drawable3D
 	 * @param a_geo the {@link GeoElement3D} linked to this GeoElement3D
 	 */
 	public Drawable3D(EuclidianView3D a_view3D, GeoElement a_geo){
 		this(a_view3D);
 		setGeoElement(a_geo);
 		
 		waitForUpdate = true;
 		viewChanged = true;
 	}
 	
 	
 	protected void realtimeUpdate() {
 	}
 	
 	
 	///////////////////////////////////////////////////////////////////////////////
 	// update
 	
 
 	/** update this according to the {@link GeoElement3D} 
 	 *
 	 */
 	final public void update(){
 		
 
 		/*
 		if (waitForUpdate||viewChanged||labelWaitForUpdate)
 			Application.debug(getGeoElement()+"\n waitForUpdate="+waitForUpdate
 					+"\n viewChanged="+viewChanged
 					+"\n labelWaitForUpdate="+labelWaitForUpdate
 					+"\n reset="+labelWaitForReset);
 					*/
 
 
 		
 		if (viewChanged){
 			updateForView();
 			viewChanged = false;
 			//setLabelWaitForUpdate();//TODO remove that
 		}
 		
 		if (labelWaitForUpdate){
 			updateLabel();
 			labelWaitForUpdate = false;
 		}
 		
 		if (waitForUpdate){
 			if (updateForItSelf())
 				waitForUpdate = false;
 			setLabelWaitForUpdate();//TODO remove that
 		}
 		
 		realtimeUpdate();
 		
 		waitForReset = false;
 	}
 	
 	
 	
 	/**
 	 * update the label
 	 */
 	protected void updateLabel(){
 		
 		label.update(getGeoElement().getLabelDescription(), 10, 
 				getGeoElement().getObjectColor(),
 				getLabelPosition().copyVector(),
				getGeoElement().labelOffsetX,-getGeoElement().labelOffsetY);
 
 	}
 	
 	/**
 	 * update the drawable when view has changed
 	 * TODO separate translation/rotation/zoom of the view
 	 */
 	abstract protected void updateForView();
 	
 	/**
 	 * update the drawable when element has changed
 	 * @return true if the update is finished
 	 */
 	abstract protected boolean updateForItSelf();
 	
 	
 	/**
 	 * says that it has to be updated
 	 */
 	public void setWaitForUpdate(){
 		
 		waitForUpdate = true;
 	}
 	
 	/**
 	 * @return true if this wait for update
 	 */
 	public boolean waitForUpdate(){
 		return waitForUpdate;
 	}
 	
 	/**
 	 * says that the view has changed
 	 */
 	public void viewChanged(){
 		
 		viewChanged = true;
 	}
 	
 	
 	/**
 	 * says that the label has to be updated
 	 */
 	public void setLabelWaitForUpdate(){
 		
 		labelWaitForUpdate = true;
 	}
 	
 	/**
 	 * reset the drawable
 	 */
 	public void setWaitForReset(){
 	
 		waitForReset = true;
 		label.setWaitForReset();
 		setLabelWaitForUpdate();
 		setWaitForUpdate();
 		viewChanged();
 	}
 	
 	
 	
 	protected void removeGeometryIndex(int index){
 		if (!waitForReset)
 			getView3D().getRenderer().getGeometryManager().remove(index);
 
 	}
 	
 	protected void setGeometryIndex(int index){
 		removeGeometryIndex(geomIndex);
 		geomIndex = index;
 	}
 	
 	
 	protected int getGeometryIndex(){
 		return geomIndex;
 	}
 	
 	
 
 	
 	
 	/**
 	 * get the drawing matrix
 	 * 
 	 * @return the drawing matrix
 	 */
 	public GgbMatrix4x4 getMatrix(){
 		return ((GeoElement3DInterface) getGeoElement()).getDrawingMatrix();
 	}
 	
 	/**
 	 * get the label position
 	 * 
 	 * @return the label position
 	 */
 	public GgbVector getLabelPosition(){
 		return getGeoElement().getLabelPosition();
 	}
 	
 	/**
 	 * get the 3D view
 	 * 
 	 * @return the 3D view
 	 */	
 	protected EuclidianView3D getView3D(){
 		return m_view3D; 
 	}
 	
 	/**
 	 * set the 3D view
 	 * 
 	 * @param a_view3D the 3D view
 	 */		
 	protected void setView3D(EuclidianView3D a_view3D){
 		m_view3D=a_view3D; 
 	}
 	
 	/**
 	 * say if the Drawable3D is visible
 	 * 
 	 * @return the visibility
 	 */
 	protected boolean isVisible(){
 		if (createdByDrawList())
 			return isCreatedByDrawListVisible() && ((Drawable3D) getDrawListCreator()).isVisible();
 		
 		return (getGeoElement().hasDrawable3D() 
 				&& getGeoElement().isEuclidianVisible() 
 				&& getGeoElement().isDefined());  
 	}
 	
 
 	
 
 	
 
 	
 	
 	
 	/////////////////////////////////////////////////////////////////////////////
 	// drawing
 	
 
 	/**
 	 * draw the geometry for not hidden parts
 	 * @param renderer the 3D renderer where to draw
 	 */
 	abstract public void drawGeometry(Renderer renderer); 
 	
 	/**
 	 * draw the geometry for picked visibility
 	 * @param renderer the 3D renderer where to draw
 	 */
 	abstract public void drawGeometryPicked(Renderer renderer); 
 	
 	/**
 	 * draw the geometry to show the object is picked (highlighted)
 	 * @param renderer the 3D renderer where to draw
 	 */
 	abstract public void drawGeometryHidden(Renderer renderer); 
 	
 	/**
 	 * draw the geometry for hidden parts
 	 * @param renderer the 3D renderer where to draw
 	 */
 	abstract public void draw(Renderer renderer); 
 	
 	/**
 	 * sets the matrix, the pencil and draw the geometry for hidden parts
 	 * @param renderer the 3D renderer where to draw
 	 */	
 	abstract public void drawHidden(Renderer renderer); 
 	
 	
 	/**
 	 * sets the matrix, the pencil and draw the geometry for transparent parts
 	 * @param renderer the 3D renderer where to draw
 	 */	
 	abstract public void drawTransp(Renderer renderer); 
 	
 	
 	/**
 	 * sets the matrix, the pencil and draw the geometry for hiding parts
 	 * @param renderer the 3D renderer where to draw
 	 */	
 	abstract public void drawHiding(Renderer renderer); 
 	
 	
 	/**
 	 * sets the matrix, the pencil and draw the geometry for the {@link Renderer} to process picking
 	 * @param renderer the 3D renderer where to draw
 	 * @return this, or the DrawList that created it
 	 */			
 	public Drawable3D drawForPicking(Renderer renderer) {
 		
 		Drawable3D ret;
 		if (createdByDrawList())//if it is part of a DrawList3D, the list is picked
 			ret = (Drawable3D) getDrawListCreator();
 		else
 			ret = this;
 
 		if (getGeoElement() instanceof GeoElement3DInterface) //TODO remove this
 			if (!((GeoElement3DInterface) getGeoElement()).isPickable()){
 				return ret;
 			}
 		
 		if(!getGeoElement().isEuclidianVisible() || !isVisible())
 			return ret;	
 		
 		drawGeometry(renderer);
 
 		return ret;
 	}
 
 	
 	/** draws the label (if any)
      * @param renderer 3D renderer
      * */
 	public void drawLabel(Renderer renderer){
 		drawLabel(renderer, true, false);
 	}
 	
 	/** draws the label for picking it 
      * @param renderer 3D renderer
      * */
 	public void drawLabelForPicking(Renderer renderer){
 		drawLabel(renderer, false, true);		
 	}
     
     /** draws the label (if any)
      * @param renderer 3D renderer
      * @param colored says if the text has to be colored
      * @param forPicking says if this method is called for picking
      */
     private void drawLabel(Renderer renderer, boolean colored, boolean forPicking){
 
     	if (forPicking) 
     		if (getGeoElement() instanceof GeoElement3DInterface) //TODO remove this
     			if(!((GeoElement3DInterface) getGeoElement()).isPickable())
     				return;
     	
 		if(!isVisible())
 			return;
 		
     	if (!getGeoElement().isLabelVisible())
     		return;
     	
     	
     	if (colored)
     		renderer.setTextColor(getGeoElement().getObjectColor());
     	
     	label.draw(renderer);
 				
     }
 	
 	
 	
 	
 	
 	/////////////////////////////////////////////////////////////////////////////
 	// picking
 	
 	/** get picking order 
 	 * @return the picking order
 	 */
 	abstract public int getPickOrder();
 	
 	/** say if another object is pickable through this Drawable3D. 
 	 * @return if the Drawable3D is transparent
 	 */
 	abstract public boolean isTransparent();
 	
 	/** compare this to another Drawable3D with picking 
 	 * @param d the other Drawable3D
 	 * @param checkPickOrder say if the comparison has to look to pick order
 	 * @return 1 if this is in front, 0 if equality, -1 either*/
 	public int comparePickingTo(Drawable3D d, boolean checkPickOrder){
 		
 		//check if one is transparent and the other not
 		if ( (!this.isTransparent()) && (d.isTransparent()) )
 			return -1;
 		if ( (this.isTransparent()) && (!d.isTransparent()) )
 			return 1;
 		
 		
 		
 		//check if the two objects are "mixed"			
 		if ((this.zPickMin-d.zPickMin)*(this.zPickMax-d.zPickMax)<EPSILON_Z){
 			
 			if (DEBUG){
 				DecimalFormat df = new DecimalFormat("0.000000000");
 				Application.debug("mixed :\n"
 						+"zMin= "+df.format(this.zPickMin)+" | zMax= "+df.format(this.zPickMax)+" ("+this.getGeoElement().getLabel()+")\n"
 						+"zMin= "+df.format(d.zPickMin)+" | zMax= "+df.format(d.zPickMax)+" ("+d.getGeoElement().getLabel()+")\n");
 			}
 			
 			if (checkPickOrder){
 				if (this.getPickOrder()<d.getPickOrder())
 					return -1;
 				if (this.getPickOrder()>d.getPickOrder())
 					return 1;
 			}
 			
 			// check if one is on a path and the other not
 			if (this.getGeoElement().isGeoPoint() && this.getGeoElement().isGeoPoint()){
 				if ((((GeoPointND) this.getGeoElement()).hasPath())&&(!((GeoPointND) d.getGeoElement()).hasPath()))
 					return -1;
 				if ((!((GeoPointND) this.getGeoElement()).hasPath())&&(((GeoPointND) d.getGeoElement()).hasPath()))
 					return 1;			 
 			}
 
 
 			//check if one is the child of the other
 			if (this.getGeoElement().isChildOf(d.getGeoElement()))
 				return -1;
 			if (d.getGeoElement().isChildOf(d.getGeoElement()))
 				return 1;
 		
 		}
 
 		//finally check if one is before the other
 		if (this.zPickMax<d.zPickMax)
 			return -1;
 		if (this.zPickMax>d.zPickMax)
 			return 1;
 
 		//says that the two objects are equal for the comparator
 		if (DEBUG){
 			DecimalFormat df = new DecimalFormat("0.000000000");
 			Application.debug("equality :\n"
 					+"zMin= "+df.format(this.zPickMin)+" | zMax= "+df.format(this.zPickMax)+" ("+this.getGeoElement().getLabel()+")\n"
 					+"zMin= "+df.format(d.zPickMin)+" | zMax= "+df.format(d.zPickMax)+" ("+d.getGeoElement().getLabel()+")\n");
 		}
 		return 0;
 
 		
 	}
 	
 	
 	/** Comparator for Drawable3Ds */
 	static final public class drawableComparator implements Comparator<Drawable3D>{
 		public int compare(Drawable3D d1, Drawable3D d2) {
 			/*
 			Drawable3D d1 = (Drawable3D) arg1;
 			Drawable3D d2 = (Drawable3D) arg2;
 			*/
 			
 						
 			return d1.comparePickingTo(d2,false);
 
 
 		}
 	}
 	
 	/** Comparator for sets of Drawable3Ds */
 	static final public class setComparator implements Comparator<TreeSet<Drawable3D>>{
 		public int compare(TreeSet<Drawable3D> set1, TreeSet<Drawable3D> set2) {
 
 			/*
 			TreeSet set1 = (TreeSet) arg1;
 			TreeSet set2 = (TreeSet) arg2;
 			*/
 			
 			//check if one set is empty
 			if (set1.isEmpty())
 				return 1;
 			if (set2.isEmpty())
 				return -1;
 			
 			Drawable3D d1 = (Drawable3D) set1.first();
 			Drawable3D d2 = (Drawable3D) set2.first();
 			
 						
 			return d1.comparePickingTo(d2,true);
 
 
 		}
 	}		
 	
 	
 	/**
 	 * @return true if geo is highlighted, or if it is part of a list highlighted
 	 */
 	public boolean doHighlighting(){
 		if (getGeoElement().isSelected()
 				||
 				(getGeoElement().doHighlighting() && getView3D().hasMouse()))
 			return true;
 		if (createdByDrawList())
 			return ((Drawable3D) getDrawListCreator()).doHighlighting();
 		return false;
 	}
 	
 
 	/**
 	 * sets the color (for drawing) regarding geo's color, amplitude for blinking highlighting, 
 	 * and alpha value
 	 * @param amplitude
 	 * @param alpha
 	 */
 	protected void setHighlightingColor(int amplitude, float alpha){
 		if(doHighlighting())
 			getView3D().getRenderer().setColor(getView3D().getRenderer().getGeometryManager().getHigthlighting(getGeoElement().getObjectColor(),amplitude),alpha);
 		else
 			getView3D().getRenderer().setColor(getGeoElement().getObjectColor(),alpha);
 	}
 	
 	/////////////////////////////////////////////////////////////////////////////
 	// links to the GeoElement
 	
 
     
     /**
      * get the GeoElementInterface linked to the Drawable3D 
      * @return the GeoElement3DInterface linked to
      */  
     public GeoElement getGeoElement() {
         return geo;
     }   
     
     
     /**
      * set the GeoElement linked to the Drawable3D
      * @param a_geo the GeoElement
      */
     public void setGeoElement(GeoElement a_geo) {
         this.geo = a_geo;
         //((GeoElement3DInterface) a_geo).setDrawable3D(this);
     } 
     
     
     
     
     
 
     /////////////////////////////
     // TYPE
     
     /** return the type of the drawable
      * @return the type of the drawable
      */
     abstract public int getType();
     
     
 
     //////////////////////////////
     // FOR PREVIEWABLE INTERFACE
     
     /**
      * remove this from the draw list 3D
      */
 	public void disposePreview() {
 		getView3D().remove(this);		
 		
 	}
 
  	
 	/** unused for 3D
 	 * @param g2
 	 */
 	public void drawPreview(Graphics2D g2) {
 		
 	}
 
     
 }
 
 
 
 
