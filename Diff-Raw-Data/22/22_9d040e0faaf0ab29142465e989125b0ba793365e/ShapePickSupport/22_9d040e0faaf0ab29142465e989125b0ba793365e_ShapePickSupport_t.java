 /*
  * Copyright (c) 2005, the JUNG Project and the Regents of the University 
  * of California
  * All rights reserved.
  *
  * This software is open-source under the BSD license; see either
  * "license.txt" or
  * http://jung.sourceforge.net/license.txt for a description.
  * Created on Mar 11, 2005
  *
  */
 package edu.uci.ics.jung.visualization.picking;
 
 import java.awt.Color;
 import java.awt.Shape;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.GeneralPath;
 import java.awt.geom.PathIterator;
 import java.awt.geom.Point2D;
 import java.awt.geom.Rectangle2D;
 import java.util.ConcurrentModificationException;
 
 import edu.uci.ics.graph.util.Pair;
 import edu.uci.ics.jung.visualization.GraphElementAccessor;
 import edu.uci.ics.jung.visualization.VisualizationServer;
 import edu.uci.ics.jung.visualization.decorators.EdgeContext;
 import edu.uci.ics.jung.visualization.layout.Layout;
 
 /**
  * ShapePickSupport provides access to Vertices and Edges based on
  * their actual shapes. 
  * 
  * @author Tom Nelson - RABA Technologies
  *
  */
 public class ShapePickSupport<V, E> implements GraphElementAccessor<V,E> {
 
     protected float pickSize;
     protected VisualizationServer<V,E> vv;
     
     /**
      * Create an instance.
      * The HasGraphLayout is used as the source of the current
      * Graph Layout. The HasShapes
      * is used to access the VertexShapes and the EdgeShapes
      * @param hasGraphLayout source of the current layout.
      * @param hasShapeFunctions source of Vertex and Edge shapes.
      * @param pickSize how large to make the pick footprint for line edges
      */
     public ShapePickSupport(VisualizationServer<V,E> vv, float pickSize) {
     	this.vv = vv;
         this.pickSize = pickSize;
     }
     
     public ShapePickSupport(float pickSize) {
         this.pickSize = pickSize;
     }
             
     /**
      * Create an instance.
      * The pickSize footprint defaults to 2.
      */
     public ShapePickSupport(VisualizationServer<V,E> vv) {
         this.vv = vv;
         this.pickSize = 2;
     }
     
     /**
      * Create an instance.
      * The pickSize footprint defaults to 2.
      */
     public ShapePickSupport() {
         this(2);
     }
 
     /** 
      * Iterates over Vertices, checking to see if x,y is contained in the
      * Vertex's Shape. If (x,y) is contained in more than one vertex, use
      * the vertex whose center is closest to the pick point.
      * @see edu.uci.ics.jung.visualization.picking.PickSupport#getVertex(double, double)
      */
     public V getVertex(Layout<V, E> layout, double x, double y) {
 
         V closest = null;
         double minDistance = Double.MAX_VALUE;
         while(true) {
             try {
                 for(V v : layout.getGraph().getVertices()) {
 
                     Shape shape = vv.getRenderContext().getVertexShapeFunction().transform(v);
                     // transform the vertex location to screen coords
                     Point2D p = vv.layoutTransform(layout.transform(v));
                     if(p == null) continue;
                     AffineTransform xform = 
                         AffineTransform.getTranslateInstance(p.getX(), p.getY());
                     shape = xform.createTransformedShape(shape);
                     // see if this vertex center is closest to the pick point
                     // among any other containing vertices
                     if(shape.contains(x, y)) {
 
                         Rectangle2D bounds = shape.getBounds2D();
                         double dx = bounds.getCenterX() - x;
                         double dy = bounds.getCenterY() - y;
                         double dist = dx * dx + dy * dy;
                         if (dist < minDistance) {
                             minDistance = dist;
                             closest = v;
                         }
                     }
                 }
                 break;
             } catch(ConcurrentModificationException cme) {}
         }
         return closest;
     }
 
     /**
      * return an edge whose shape intersects the 'pickArea' footprint of the passed
      * x,y, coordinates.
      */
     public E getEdge(Layout<V, E> layout, double x, double y) {
 
         // as a Line has no area, we can't always use edgeshape.contains(point) so we
         // make a small rectangular pickArea around the point and check if the
         // edgeshape.intersects(pickArea)
         Rectangle2D pickArea = 
             new Rectangle2D.Float((float)x-pickSize/2,(float)y-pickSize/2,pickSize,pickSize);
         E closest = null;
         double minDistance = Double.MAX_VALUE;
         while(true) {
             try {
                 for(E e : layout.getGraph().getEdges()) {
 
                     Pair<V> pair = layout.getGraph().getEndpoints(e);
                     V v1 = pair.getFirst();
                     V v2 = pair.getSecond();
                     boolean isLoop = v1.equals(v2);
                     Point2D p1 = vv.layoutTransform(layout.transform(v1));
                     Point2D p2 = vv.layoutTransform(layout.transform(v2));
                     if(p1 == null || p2 == null) continue;
                     float x1 = (float) p1.getX();
                     float y1 = (float) p1.getY();
                     float x2 = (float) p2.getX();
                     float y2 = (float) p2.getY();
 
                     // translate the edge to the starting vertex
                     AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);
 
                     Shape edgeShape = 
                     	vv.getRenderContext().getEdgeShapeFunction().transform(new EdgeContext<V,E>(vv.getGraphLayout().getGraph(),e));
                     if(isLoop) {
                         // make the loops proportional to the size of the vertex
                         Shape s2 = vv.getRenderContext().getVertexShapeFunction().transform(v2);
                         Rectangle2D s2Bounds = s2.getBounds2D();
                         xform.scale(s2Bounds.getWidth(),s2Bounds.getHeight());
                         // move the loop so that the nadir is centered in the vertex
                         xform.translate(0, -edgeShape.getBounds2D().getHeight()/2);
                     } else {
                         float dx = x2 - x1;
                         float dy = y2 - y1;
                         // rotate the edge to the angle between the vertices
                         double theta = Math.atan2(dy,dx);
                         xform.rotate(theta);
                         // stretch the edge to span the distance between the vertices
                         float dist = (float) Math.sqrt(dx*dx + dy*dy);
                         xform.scale(dist, 1.0f);
                     }
 
                     // transform the edge to its location and dimensions
                     edgeShape = xform.createTransformedShape(edgeShape);
 
                     vv.getRenderContext().getGraphicsContext().setPaint(Color.red);
                     vv.getRenderContext().getGraphicsContext().draw(edgeShape);
                     // because of the transform, the edgeShape is now a GeneralPath
                     // see if this edge is the closest of any that intersect
                     if(edgeShape.intersects(pickArea)) {
                         float cx=0;
                         float cy=0;
                         float[] f = new float[6];
                        PathIterator pi = new GeneralPath(edgeShape).getPathIterator(null);
                         if(pi.isDone()==false) {
                             pi.next();
                             pi.currentSegment(f);
                             cx = f[0];
                             cy = f[1];
                             if(pi.isDone()==false) {
                                 pi.currentSegment(f);
                                 cx = f[0];
                                 cy = f[1];
                             }
                         }
                         float dx = (float) (cx - x);
                         float dy = (float) (cy - y);
                         float dist = dx * dx + dy * dy;
                         if (dist < minDistance) {
                             minDistance = dist;
                             closest = e;
                         }
                     }
 		        }
 		        break;
 		    } catch(ConcurrentModificationException cme) {}
 		}
 		return closest;
     }
 }
