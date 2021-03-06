 /*
  *    Geotools - OpenSource mapping toolkit
  *    (C) 2002, Centre for Computational Geography
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
  *
  *    You should have received a copy of the GNU Lesser General Public
  *    License along with this library; if not, write to the Free Software
  *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  *    
  */
 
 package org.geotools.filter;
 
 import com.vividsolutions.jts.geom.*;
 
 import org.geotools.data.*;
 import org.geotools.feature.*;
 
 /**
  * Implements a geometry filter.
  *
  * <p>This filter implements a relationship - of some sort - between two 
  * geometry expressions.
  *
  * Note that this comparison does not attempt to restict its expressions to be
  * meaningful.  This means that it considers itself a valid filter as long as
  * it contains two <b>geometry</b> sub-expressions.  It is also slightly less 
  * restrictive than the OGC Filter specification because it does not require
  * that one sub-expression be an geometry attribute and the other be a geometry
  * literal.</p>
  *
  * <p>In other words, you may use this filter to compare two geometries in the
  * same feature, such as: attributeA inside attributeB?  You may also compare
  * two literal geometries, although this is fairly meaningless, since it
  * could be reduced (ie. it is always either true or false).  This approach
  * is very similar to that taken in the FilterCompare class.</p>
  *
 * @version $Id: GeometryFilter.java,v 1.2 2002/07/12 12:53:40 loxnard Exp $
  * @author Rob Hranac, TOPP
  */
 public class GeometryFilter extends AbstractFilter {
 
     /** Holds the 'left' value of this comparison filter. */
     protected Expression leftGeometry = null;
 
     /** Holds the 'right' value of this comparison filter. */
     protected Expression rightGeometry = null;
 
 
     /**
      * Constructor with filter type.
      *
      * @param filterType The type of comparison.
      * @throws IllegalFilterException Non-geometry type.
      */
     public GeometryFilter (short filterType) 
         throws IllegalFilterException {
         
         if (isGeometryFilter(filterType)) {
             this.filterType = filterType;
         }
         else {
             throw new IllegalFilterException("Attempted to create geometry filter with non-geometry type.");
         }
     }
 
 
     /**
      * Adds the 'left' value to this filter.
      *
      * @param leftGeometry Expression for 'left' value.
      * @throws IllegalFilterException Filter is not internally consistent.
      */
     public void addLeftGeometry(Expression leftGeometry)
         throws IllegalFilterException {
         
         // Checks if this is geometry filter or not and handles appropriately
         if (ExpressionDefault.isGeometryExpression(leftGeometry.getType())  ||
             permissiveConstruction) {
             this.leftGeometry = leftGeometry;
         }
         else {
             throw new IllegalFilterException("Attempted to add (left) non-geometry expression to geometry filter.");
         }
 
     }
 
     /**
      * Adds the 'right' value to this filter.
      *
      * @param rightGeometry Expression for 'right' value.
      * @throws IllegalFilterException Filter is not internally consistent.
      */
     public void addRightGeometry(Expression rightGeometry)
         throws IllegalFilterException {
         
         // Checks if this is math filter or not and handles appropriately
         if (ExpressionDefault.isGeometryExpression(rightGeometry.getType()) ||
             permissiveConstruction) {
             this.rightGeometry = rightGeometry;
         }
         else {
             throw new IllegalFilterException("Attempted to add (right) non-geometry expression to geometry filter.");
         }
         
     }
 
     /**
      * Determines whether or not a given feature is 'inside' this filter.
      *
      * @param feature Specified feature to examine.
      * @return Flag confirming whether or not this feature is inside the filter.
      */
     public boolean contains(Feature feature) {
         
         // Checks for error condition
         if (leftGeometry == null || rightGeometry == null) {
             return false;
         }
 
         // Handles all normal geometry cases
         else if (filterType == GEOMETRY_EQUALS) {
             return ((Geometry) leftGeometry.getValue(feature)).
                 equals((Geometry) rightGeometry.getValue(feature));
         }
         else if (filterType == GEOMETRY_DISJOINT) {
             return ((Geometry) leftGeometry.getValue(feature)).
                 disjoint((Geometry) rightGeometry.getValue(feature));
         }
         else if (filterType == GEOMETRY_INTERSECTS) {
             return ((Geometry) leftGeometry.getValue(feature)).
                 intersects((Geometry) rightGeometry.getValue(feature));
         }
         else if (filterType == GEOMETRY_CROSSES) {
             return ((Geometry) leftGeometry.getValue(feature)).
                 crosses((Geometry) rightGeometry.getValue(feature));
         }
         else if (filterType == GEOMETRY_WITHIN) {
             return ((Geometry) leftGeometry.getValue(feature)).
                 within((Geometry) rightGeometry.getValue(feature));
         }
         else if (filterType == GEOMETRY_CONTAINS) {
             return ((Geometry) leftGeometry.getValue(feature)).
                 contains((Geometry) rightGeometry.getValue(feature));
         }
         else if (filterType == GEOMETRY_OVERLAPS) {
             return ((Geometry) leftGeometry.getValue(feature)).
                 overlaps((Geometry) rightGeometry.getValue(feature));
         }
         else if (filterType == GEOMETRY_BEYOND) {
             return ((Geometry) leftGeometry.getValue(feature)).
                 within((Geometry) rightGeometry.getValue(feature));
         }
         else if (filterType == GEOMETRY_BBOX) {
             return ((Geometry) leftGeometry.getValue(feature)).
                contains((Geometry) rightGeometry.getValue(feature));
         }
 
         // Note that this is a pretty permissive logic
         //  if the type has somehow been mis-set (can't happen externally)
         //  then true is returned in all cases
         else {
             return true;
         }
     
     }
 
     /**
      * Return this filter as a string.
      *
      * @return String representation of this geometry filter.
      */
     public String toString() {
         String operator = null;
 
         // Handles all normal geometry cases
         if (filterType == GEOMETRY_EQUALS) {
             operator = " equals ";
         }
         else if (filterType == GEOMETRY_DISJOINT) {
             operator = " disjoint ";
         }
         else if (filterType == GEOMETRY_INTERSECTS) {
             operator = " intersects ";
         }
         else if (filterType == GEOMETRY_CROSSES) {
             operator = " crosses ";
         }
         else if (filterType == GEOMETRY_WITHIN) {
             operator = " within ";
         }
         else if (filterType == GEOMETRY_CONTAINS) {
             operator = " contains ";
         }
         else if (filterType == GEOMETRY_OVERLAPS) {
             operator = " overlaps ";
         }
         else if (filterType == GEOMETRY_BEYOND) {
             operator = " beyond ";
         }
         else if (filterType == GEOMETRY_BBOX) {
             operator = " bbox ";
         }
         
         return "[ " + leftGeometry.toString() + operator + rightGeometry.toString() + " ]";        
     }
             
 }
