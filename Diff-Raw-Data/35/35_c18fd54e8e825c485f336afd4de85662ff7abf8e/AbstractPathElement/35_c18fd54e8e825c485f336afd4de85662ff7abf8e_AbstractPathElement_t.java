 /* ==========================================
  * JGraphT : a free Java graph-theory library
  * ==========================================
  *
  * Project Info:  http://jgrapht.sourceforge.net/
  * Project Lead:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
  *
  * (C) Copyright 2003-2006, by Barak Naveh and Contributors.
  *
  * This library is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or
  * (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this library; if not, write to the Free Software Foundation,
  * Inc.,
  * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
  */
 /* -------------------------
  * AbstractPathElement.java
  * -------------------------
 * (C) Copyright 2006, by France Telecom
  *
 * Original Author:  Guillaume Boulmier and Contributors.
  * Contributor(s):   John V. Sichi
  *
  * $Id$
  *
  * Changes
  * -------
  * 05-Jan-2006 : Initial revision (GB);
  * 14-Jan-2006 : Added support for generics (JVS);
  *
  */
 package org._3pq.jgrapht.alg;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 
 import org._3pq.jgrapht.Edge;
 
 /**
  * A new path is created from a path concatenated to an edge. It's like a linked
  * list. <br>
  * 
  * The empty path is composed only of one vertex. <br>
  * In this case the path has no previous path element. <br>.
  *
  *<p>
  *
  * NOTE jvs 14-Jan-2006: This is currently an internal data structure for use
  * in algorithms.  If we want to promote it to public, we should first clean it
  * up and move it to the parent package, making a Path a first-class concept.
  */
 abstract class AbstractPathElement<V, E extends Edge<V>> {
 
     /**
      * Number of hops of the path.
      */
     protected int nHops;
 
     /**
      * Edge reaching the target vertex of the path.
      */
     protected E prevEdge;
 
     /**
      * Previous path element.
      */
     protected AbstractPathElement<V,E> prevPathElement;
 
     /**
      * Target vertex.
      */
     private V vertex;
 
     /**
      * Creates a path element by concatenation of an edge to a path element.
      * 
      * @param pathElement
      * @param edge
      *            edge reaching the end vertex of the path element created.
      */
     protected AbstractPathElement(
         AbstractPathElement<V,E> pathElement, E edge) {
         this.vertex = edge.oppositeVertex(pathElement.getVertex());
         this.prevEdge = edge;
         this.prevPathElement = pathElement;
 
         this.nHops = pathElement.getHopCount() + 1;
     }
 
     /**
      * Creates an empty path element.
      * 
      * @param vertex
      *            end vertex of the path element.
      */
     protected AbstractPathElement(V vertex) {
         this.vertex = vertex;
         this.prevEdge = null;
         this.prevPathElement = null;
 
         this.nHops = 0;
     }
 
     /**
      * Returns the path as a list of edges.
      * 
      * @return list of <code>Edge</code>.
      */
     public List<E> createEdgeListPath() {
         List<E> path = new ArrayList<E>();
         AbstractPathElement<V,E> pathElement = this;
         // while start vertex is not reached.
         while (pathElement.getPrevEdge() != null) {
 
             path.add(pathElement.getPrevEdge());
 
             pathElement = pathElement.getPrevPathElement();
         }
 
         Collections.reverse(path);
 
         return path;
     }
 
     /**
      * Returns the number of hops (or number of edges) of the path.
      * 
      * @return .
      */
     public int getHopCount() {
         return this.nHops;
     }
 
     /**
      * Returns the edge reaching the target vertex of the path.
      * 
      * @return <code>null</code> if the path is empty.
      */
     public E getPrevEdge() {
         return this.prevEdge;
     }
 
     /**
      * Returns the previous path element.
      * 
      * @return <code>null</code> is the path is empty.
      */
     public AbstractPathElement<V,E> getPrevPathElement() {
         return this.prevPathElement;
     }
 
     /**
      * Returns the target vertex of the path.
      * 
      * @return .
      */
     public V getVertex() {
         return this.vertex;
     }
 
 }
 
