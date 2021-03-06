 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: SimpleWirer.java
  *
  * Copyright (c) 2003 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Electric(tm) is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Electric(tm); see the file COPYING.  If not, write to
  * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  * Boston, Mass 02111-1307, USA.
  */
 
 package com.sun.electric.tool.routing;
 
 import com.sun.electric.database.hierarchy.Cell;
 import com.sun.electric.database.prototype.ArcProto;
 import com.sun.electric.database.prototype.PortProto;
 import com.sun.electric.database.prototype.NodeProto;
 import com.sun.electric.database.topology.PortInst;
 import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
 import com.sun.electric.technology.technologies.Generic;
 import com.sun.electric.tool.user.User;
 
 import java.awt.geom.Point2D;
 import java.util.List;
 import java.util.Iterator;
 import java.util.Set;
 import java.util.HashSet;
 
 /**
  * User: gainsley
  * Date: Feb 25, 2004
  * Time: 1:13:50 PM
  *
  * A Simple wiring tool for the user to draw wires
  */
 public class SimpleWirer extends InteractiveRouter {
 
     /* ----------------------- Router Methods ------------------------------------- */
 
     public String toString() { return "SimpleWirer"; }
 
 
     protected boolean planRoute(Route route, Cell cell, RouteElement endRE, Point2D clicked) {
 
         RouteElement startRE = route.getEnd();
 
         // first, find location of corner of L if routing will be an L shape
         Point2D cornerLoc = null;
         Point2D startLoc = startRE.getLocation();
         Point2D endLoc = endRE.getLocation();
         if (startLoc.getX() == endLoc.getX() || startLoc.getY() == endLoc.getY()) {
             // single arc
             cornerLoc = endLoc;
         } else {
             Point2D pin1 = new Point2D.Double(startLoc.getX(), endLoc.getY());
             Point2D pin2 = new Point2D.Double(endLoc.getX(), startLoc.getY());
             // find which pin to use
             int clickedQuad = findQuadrant(endLoc, clicked);
             int pin1Quad = findQuadrant(endLoc, pin1);
             int pin2Quad = findQuadrant(endLoc, pin2);
             int oppositeQuad = (clickedQuad + 2) % 4;
             if (pin2Quad == clickedQuad)
             {
                 pin1 = pin2;                // same quad as pin2, use pin2
             } else if (pin1Quad == clickedQuad)
             {
                 pin1 = pin1;                // same quad as pin1, use pin1
             } else if (pin1Quad == oppositeQuad)
             {
                 pin1 = pin2;                // near to pin2 quad, use pin2
             }
             // else it is near to pin1 quad, use pin1
             cornerLoc = pin1;
         }
 
         // find port protos of startRE and endRE, and find connecting arc type
         PortProto startPort = startRE.getPortProto();
         PortProto endPort = endRE.getPortProto();
         ArcProto useArc = getArcToUse(startPort, endPort);
         // never use universal arcs unless the user has selected them
         if (useArc == null) {
             // use universal if selected
             if (User.tool.getCurrentArcProto() == Generic.tech.universal_arc)
                 useArc = Generic.tech.universal_arc;
             else {
                 // route vertically
                 VerticalRoute vroute = new VerticalRoute(startRE, endRE);
                 if (!vroute.specifyRoute()) {
                     System.out.println("Don't know how to connect (using Technology "+cell.getTechnology()+"):\n   "+startRE+"\n   "+endRE);
                     return false;
                 }
                 vroute.buildRoute(route, cell, cornerLoc);
                 return true;
             }
         }
 
         route.add(endRE);
         route.setEnd(endRE);
 
         // startRE and endRE can be connected with an arc.  If one of them is a bisectArcPin,
         // and can be replaced by the other, just replace it and we're done.
         if (isElementReplacable(startRE, endRE)) {
             Router.replaceRouteElementArcPin(route, startRE, endRE);
             route.remove(startRE);
             return true;
         } else if (isElementReplacable(endRE, startRE)) {
             Router.replaceRouteElementArcPin(route, endRE, startRE);
             route.remove(endRE);
             route.setEnd(startRE);
             return true;
         }
 
         // find arc width to use
         double width = getArcWidthToUse(startRE, useArc);
         double width2 = getArcWidthToUse(endRE, useArc);
         if (width2 > width) width = width2;
 
         // this router only draws horizontal and vertical arcs
         // if either X or Y coords are the same, create a single arc
         if (startLoc.getX() == endLoc.getX() || startLoc.getY() == endLoc.getY()) {
             // single arc
             RouteElement arcRE = RouteElement.newArc(cell, useArc, width, startRE, endRE, null);
             route.add(arcRE);
         } else {
             // otherwise, create new pin and two arcs for corner
             // make new pin of arc type
             PrimitiveNode pn = ((PrimitiveArc)useArc).findOverridablePinProto();
             RouteElement pinRE = RouteElement.newNode(cell, pn, pn.getPort(0), cornerLoc,
                    pn.getDefWidth(), pn.getDefHeight());
             RouteElement arcRE1 = RouteElement.newArc(cell, useArc, width, startRE, pinRE, null);
             RouteElement arcRE2 = RouteElement.newArc(cell, useArc, width, pinRE, endRE, null);
             route.add(pinRE);
             route.add(arcRE1);
             route.add(arcRE2);
         }
 
         return true;
     }
 
     /**
      * Checks to see if re is a bisect arc pin, and if it can be replaced with replacement
      * @return true if re is replacable by replacement
      */
     private boolean isElementReplacable(RouteElement re, RouteElement replacement) {
         if (!re.isBisectArcPin()) return false;
         // if replacement is an existing port inst, see if re's location lies within port
         if (re.getLocation().equals(replacement.getConnPoint())) return true;
         return false;
     }
 
     /**
      * Determines what route quadrant pt is compared to refPoint.
      * A route can be drawn vertically or horizontally so this
      * method will return a number between 0 and 3, inclusive,
      * where quadrants are defined based on the angle relationship
      * of refPoint to pt.  Imagine a circle with <i>refPoint</i> as
      * the center and <i>pt</i> a point on the circumference of the
      * circle.  Then theta is the angle described by the arc refPoint->pt,
      * and quadrants are defined as:
      * <code>
      * <p>quadrant :     angle (theta)
      * <p>0 :            -45 degrees to 45 degrees
      * <p>1 :            45 degress to 135 degrees
      * <p>2 :            135 degrees to 225 degrees
      * <p>3 :            225 degrees to 315 degrees (-45 degrees)
      *
      * @param refPoint reference point
      * @param pt variable point
      * @return which quadrant <i>pt</i> is in.
      */
     protected static int findQuadrant(Point2D refPoint, Point2D pt) {
         // find angle
         double angle = Math.atan((pt.getY()-refPoint.getY())/(pt.getX()-refPoint.getX()));
         if (pt.getX() < refPoint.getX()) angle += Math.PI;
         if ((angle > -Math.PI/4) && (angle <= Math.PI/4))
             return 0;
         else if ((angle > Math.PI/4) && (angle <= Math.PI*3/4))
             return 1;
         else if ((angle > Math.PI*3/4) &&(angle <= Math.PI*5/4))
             return 2;
         else
             return 3;
     }
 }
 
