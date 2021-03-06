 /*
  *    GeoTools - The Open Source Java GIS Toolkit
  *    http://geotools.org
  *
  *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
  *
  *    This library is free software; you can redistribute it and/or
  *    modify it under the terms of the GNU Lesser General Public
  *    License as published by the Free Software Foundation;
  *    version 2.1 of the License.
  *
  *    This library is distributed in the hope that it will be useful,
  *    but WITHOUT ANY WARRANTY; without even the implied warranty of
  *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *    Lesser General Public License for more details.
  */
 package org.geotools.gui.swing;
 
 import java.awt.AlphaComposite;
 import java.awt.Color;
 import java.awt.Cursor;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.Point;
 import java.awt.Rectangle;
 import java.awt.event.ComponentAdapter;
 import java.awt.event.ComponentEvent;
 import java.awt.event.MouseEvent;
 import java.awt.image.BufferedImage;
 import java.io.IOException;
 import java.util.HashMap;
 import java.util.Map;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.NoninvertibleTransformException;
 import java.awt.geom.Rectangle2D;
 import javax.swing.JPanel;
 import javax.swing.event.MouseInputAdapter;
 import org.geotools.geometry.DirectPosition2D;
 import org.geotools.geometry.jts.ReferencedEnvelope;
 import org.geotools.gui.swing.event.MapMouseListener;
 import org.geotools.gui.swing.tool.CursorTool;
 import org.geotools.gui.swing.tool.MapTool;
 import org.geotools.gui.swing.tool.MapToolManager;
 import org.geotools.map.MapContext;
 import org.geotools.map.event.MapLayerListEvent;
 import org.geotools.map.event.MapLayerListListener;
 import org.geotools.renderer.GTRenderer;
 import org.geotools.renderer.label.LabelCacheImpl;
 import org.geotools.renderer.lite.LabelCache;
 import org.geotools.renderer.lite.StreamingRenderer;
 import org.opengis.geometry.Envelope;
 import org.opengis.referencing.crs.CoordinateReferenceSystem;
 
 /**
  * A simple map display container that works with a GTRenderer and
  * MapContext to display features. Supports the use of tool classes
  * to implement, for example, mouse-controlled zooming and panning.
  * 
  * Based on original code by Ian Turton. This version does not yet
  * support selection and highlighting of features.
  * 
  * @author Michael Bedward
  * @author Ian Turton
  */
 public class JMapPane extends JPanel implements MapLayerListListener {
     
     /**
      * Default width of the margin (pixels) between the edge of the 
      * map pane and the drawing area
      */
     public static final int DEFAULT_BORDER_WIDTH = 10;
     
     /**
      * Encapsulates XOR box drawing logic used with mouse dragging
      */
     private class DragBox extends MouseInputAdapter {
         private Point startPos;
         private Rectangle rect = new Rectangle();
         private boolean dragged = false;
         
         @Override
         public void mousePressed(MouseEvent e) {
             startPos = new Point(e.getPoint());
         }
         
         @Override
         public void mouseDragged(MouseEvent e) {
             Graphics2D g2D = (Graphics2D) JMapPane.this.getGraphics();
             g2D.setColor(Color.WHITE);
             g2D.setXORMode(Color.RED);
             if (dragged) {
                 g2D.drawRect(rect.x, rect.y, rect.width, rect.height);
             }
             
             rect.setFrameFromDiagonal(startPos, e.getPoint());
             g2D.drawRect(rect.x, rect.y, rect.width, rect.height);
 
             dragged = true;
         }
         
         @Override
         public void mouseReleased(MouseEvent e) {
             if (dragged) {
                 Graphics2D g2D = (Graphics2D) JMapPane.this.getGraphics();
                 g2D.setColor(Color.WHITE);
                 g2D.setXORMode(Color.RED);
                 g2D.drawRect(rect.x, rect.y, rect.width, rect.height);
                 dragged = false;
             }
         }
     }
 
     private MapContext context;
     private GTRenderer renderer;
     private LabelCache labelCache;
     private MapToolManager toolManager;
     
     private AffineTransform worldToScreen;
     private AffineTransform screenToWorld;
     
     private Rectangle curPaintArea;
     private int margin;
 
     private BufferedImage baseImage;
     private Point imageOrigin;
     boolean redrawBaseImage;
     private boolean needNewBaseImage;
     private boolean baseImageMoved;
     
 
     /** 
      * Constructor - creates an instance of JMapPane with no map 
      * context or renderer initially
      */
     public JMapPane() {
         this(null, null);
     }
 
     /**
      * Constructor - creates an instance of JMapPane with the given
      * renderer and map context.
      * 
      * @param renderer a renderer object
      * @param context an instance of MapContext
      */
     public JMapPane(GTRenderer renderer, MapContext context) {
         margin = DEFAULT_BORDER_WIDTH;        
         imageOrigin = new Point(margin, margin);
         redrawBaseImage = true;
         baseImageMoved = false;
         
         setRenderer(renderer);
         setContext(context);
 
         toolManager = new MapToolManager(this);
         DragBox dragBox = new DragBox();
         this.addMouseListener(dragBox);
         this.addMouseMotionListener(dragBox);
         
         this.addMouseListener(toolManager);
         this.addMouseMotionListener(toolManager);
         this.addMouseWheelListener(toolManager);
         
         addComponentListener(new ComponentAdapter() {
             @Override
             public void componentResized(ComponentEvent ev) {
                 needNewBaseImage = true;
             }
         });
     }
 
     /**
      * Set the current cursor tool
      * 
      * @param tool the tool to set; null means no active cursor tool
      */
     public void setCursorTool(CursorTool tool) {
         if (tool == null) {
             toolManager.setNoCursorTool();
             this.setCursor(Cursor.getDefaultCursor());
        } else {
            tool.setMapPane(this);
            this.setCursor(tool.getCursor());
            toolManager.setCursorTool(tool);
         }
     }
     
     /**
      * Add a general (non-cursor) tool to the map pane's set
      * of active tools
      * 
      * @todo this is an idea that might be going nowhere !
      * 
      */
     public void addTool(MapTool tool) {
         if (tool == null) {
             throw new IllegalArgumentException("tool must not be null");
         }
         
         tool.setMapPane(this);
         toolManager.addTool(tool);
     }
     
     /**
      * Add an object that wants to receive JMapPaneMouseEvents
      * from this pane
      */
     public void addMouseListener(MapMouseListener listener) {
         toolManager.addMouseListener(listener);
     }
 
     /**
      * Getet the current renderer
      */
     public GTRenderer getRenderer() {
         return renderer;
     }
 
     /**
      * Set the renderer
      */
     public void setRenderer(GTRenderer renderer) {
        Map hints;
         if (renderer instanceof StreamingRenderer) {
             hints = renderer.getRendererHints();
             if (hints == null) {
                 hints = new HashMap();
             }
             if (hints.containsKey(StreamingRenderer.LABEL_CACHE_KEY)) {
                 labelCache = (LabelCache) hints.get(StreamingRenderer.LABEL_CACHE_KEY);
             } else {
                 labelCache = new LabelCacheImpl();
                 hints.put(StreamingRenderer.LABEL_CACHE_KEY, labelCache);
             }
             renderer.setRendererHints(hints);
         }
 
         this.renderer = renderer;
 
         if (this.context != null) {
             this.renderer.setContext(this.context);
         }
     }
 
     /**
      * Get the map context associated with this map pane
      */
     public MapContext getContext() {
         return context;
     }
 
     /**
      * Set the map context for this map pane
      */
     public void setContext(MapContext context) {
         if (this.context != null) {
             this.context.removeMapLayerListListener(this);
         }
 
         this.context = context;
 
         if (context != null) {
             this.context.addMapLayerListListener(this);
         }
 
         if (renderer != null) {
             renderer.setContext(this.context);
         }
     }
 
     /**
      * Return a (copy of) the currently displayed map area or
      * null if none is set
      */
     public ReferencedEnvelope getMapArea() {
         ReferencedEnvelope env = null;
         
         if (context != null) {
             env = context.getAreaOfInterest();
         }
         
         return env;
     }
 
     /**
      * Set the map area to display. Does nothing if the MapContext and its
      * CoordinateReferenceSystem have not been set.
      * <p>
      * Note: This method does <b>not</b> check that the requested area overlaps
      * the bounds of the current map layers.
      * 
      * @param mapArea Area of the map to display (you can use a geoapi Envelope implementations 
      * such as ReferenedEnvelope or Envelope2D)
      */
     public void setMapArea(Envelope env) {
         if (context != null) {
             CoordinateReferenceSystem crs = context.getCoordinateReferenceSystem();
             if (crs != null) {
                 // overriding the env arg's crs (if any)
                 ReferencedEnvelope refEnv = new ReferencedEnvelope(
                         env.getMinimum(0), env.getMaximum(0),
                         env.getMinimum(1), env.getMaximum(1),
                         crs);
                 
                 context.setAreaOfInterest(refEnv);
                 setTransforms(refEnv, curPaintArea);
                 labelCache.clear();
                 repaint();
             }
         }
     }
     
     /**
      * Reset the map area to include the full extent of all
      * layers and redraw the display
      */
     public void reset() {
         try {
             setMapArea(context.getLayerBounds());
         } catch(IOException ex) {
             ex.printStackTrace();
         }
     }
 
     /**
      * Get the width of the current margin between the
      * edge of the map pane and the drawing area.
      * 
      * @return margin width in pixels
      * @see #DEFAULT_BORDER_WIDTH
      */
     public int getMargin() {
         return margin;
     }
     
     /**
      * Set the width of the margin between the edge of the 
      * map pane and the drawing area. Causes the display to
      * be redrawn.
      * 
      * @param w border width in pixels (values < 0 are ignored)
      * @see #DEFAULT_BORDER_WIDTH
      */
     public void setMargin(int w) {
         if (w >= 0 && w != margin) {
             margin = w;
             repaint();
         }
     }
     
     /**
      * Get a (copy of) the screen to world coordinate transform
      * being used by this map pane.
      */
     public AffineTransform getScreenToWorldTransform() {
         if (screenToWorld != null) {
             return new AffineTransform(screenToWorld);
         } else {
             return null;
         }
     }
     
     /**
      * Get a (copy of) the world to screen coordinate transform
      * being used by this map pane. This method can be 
      * used to determine the current drawing scale...
      * <pre>{@code \u0000
      * double scale = mapPane.getScreenToWorldTransform().getScaleX();
      * }</pre>
      */
     public AffineTransform getWorldToScreenTransform() {
         if (worldToScreen != null) {
             return new AffineTransform(worldToScreen);
         } else {
             return null;
         }
     }
     
     /**
      * Move the image currently displayed by the map pane from
      * its current origin (x,y) to (x+dx, y+dy). This method
      * allows dragging the map without the overhead of redrawing
      * the features during the drag. For example, it is used by
      * {@link org.geotools.gui.swing.tool.JMapPanePanTool}.
      * 
      * @param dx the x offset in pixels
      * @param dy the y offset in pixels.
      */
     public void moveImage(int dx, int dy) {
         imageOrigin.translate(dx, dy);
         redrawBaseImage = false;
         baseImageMoved = true;
         repaint();
     }
     
     /**
      * Called by the system to draw the layers currently visible layers.
      * Client code should not use this method directly; instead call
      * repaint().
      */
     @Override
     protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         
         if (context == null || renderer == null) {
             return;
         }
 
         Rectangle paintArea = this.getVisibleRect();
         paintArea.grow(-margin, -margin);
         
         if (needNewBaseImage) {
             baseImage = new BufferedImage(paintArea.width, paintArea.height, BufferedImage.TYPE_INT_ARGB);
             curPaintArea = paintArea;
             needNewBaseImage = false;
             redrawBaseImage = true;
             setTransforms(context.getAreaOfInterest(), curPaintArea);
             labelCache.clear(); 
         }
         
 
         ReferencedEnvelope mapAOI = context.getAreaOfInterest();
         if (mapAOI == null) {
             return;
         }
 
         if (redrawBaseImage) {
             if (baseImageMoved) {
                 afterImageMove(mapAOI, paintArea);
                 baseImageMoved = false;
                 labelCache.clear();
             }
             clearBaseImage();
             Graphics2D baseGr = baseImage.createGraphics();
             renderer.paint(baseGr, paintArea, mapAOI, worldToScreen);
             imageOrigin.setLocation(margin, margin);
         } 
         
         ((Graphics2D) g).drawImage(baseImage, imageOrigin.x, imageOrigin.y, this);
         redrawBaseImage = true;
     }
 
     /**
      * Called after the base image has been dragged. Sets the new map area and
      * transforms
      * @param mapAOI pre-move map area
      * @param paintArea drawing area
      */
     protected void afterImageMove(ReferencedEnvelope mapAOI, Rectangle paintArea) {
         int dx = imageOrigin.x - margin;
         int dy = imageOrigin.y - margin;
         DirectPosition2D newPos = new DirectPosition2D(dx, dy);
         screenToWorld.transform(newPos, newPos);
         mapAOI.translate(mapAOI.getMinimum(0) - newPos.x, mapAOI.getMaximum(1) - newPos.y);
         setTransforms(mapAOI, paintArea);
     }
 
     /**
      * Called when a new map layer has been added
      */
     public void layerAdded(MapLayerListEvent event) {
         if (context.getLayers().length == 1) { // the first one
             repaint();
         }
     }
 
     /**
      * Called when a map layer has been removed
      */
     public void layerRemoved(MapLayerListEvent event) {
         repaint();
     }
 
     /**
      * Called when a map layer has changed, e.g. features added
      * to a displayed feature collection
      */
     public void layerChanged(MapLayerListEvent event) {
         repaint();
     }
 
     /**
      * Called when the bounds of a map layer have changed
      */
     public void layerMoved(MapLayerListEvent event) {
         repaint();
     }
 
     /**
      * Calculate the affine transforms used to convert between
      * world and pixel coordinates. The calculations here are very
      * basic and assume a cartesian reference system.
      * 
      * @param mapEnv the current map extent (map units)
      * @param paintArea the current map pane extent (pixels)
      */
     private void setTransforms(ReferencedEnvelope mapEnv, Rectangle paintArea) {
         double xscale = paintArea.getWidth() / mapEnv.getWidth();
         double yscale = paintArea.getHeight() / mapEnv.getHeight();
         
         double scale = Math.min(xscale, yscale);
         double xoff = mapEnv.getMinimum(0) * scale;
         double yoff = mapEnv.getMaximum(1) * scale;
         worldToScreen = new AffineTransform(scale, 0, 0, -scale, -xoff, yoff);
         try {
             screenToWorld = worldToScreen.createInverse();
         } catch (NoninvertibleTransformException ex) {
             ex.printStackTrace();
         }
     }
     
     /**
      * Erase the base image. This is much faster than recreating a new BufferedImage
      * object each time we need to redraw the image
      */
     private void clearBaseImage() {
         Graphics2D g2D = baseImage.createGraphics();
         g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
         Rectangle2D.Double rect = new Rectangle2D.Double(
                 0, 0, baseImage.getWidth(), baseImage.getHeight());
         g2D.fill(rect);
     }
 
 }
