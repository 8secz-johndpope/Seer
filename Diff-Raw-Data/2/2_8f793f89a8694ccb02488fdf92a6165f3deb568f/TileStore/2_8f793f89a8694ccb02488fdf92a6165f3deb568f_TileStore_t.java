 //$HeadURL$
 /*----------------------------------------------------------------------------
  This file is part of deegree, http://deegree.org/
  Copyright (C) 2001-2010 by:
  - Department of Geography, University of Bonn -
  and
  - lat/lon GmbH -
 
  This library is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the Free
  Software Foundation; either version 2.1 of the License, or (at your option)
  any later version.
  This library is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
  details.
  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation, Inc.,
  59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 
  Contact information:
 
  lat/lon GmbH
  Aennchenstr. 19, 53177 Bonn
  Germany
  http://lat-lon.de/
 
  Department of Geography, University of Bonn
  Prof. Dr. Klaus Greve
  Postfach 1147, 53001 Bonn
  Germany
  http://www.geographie.uni-bonn.de/deegree/
 
  Occam Labs UG (haftungsbeschränkt)
  Godesberger Allee 139, 53175 Bonn
  Germany
  http://www.occamlabs.de/
 
  e-mail: info@deegree.org
  ----------------------------------------------------------------------------*/
 package org.deegree.tile.persistence;
 
 import java.util.Iterator;
 
 import org.deegree.commons.config.Resource;
 import org.deegree.geometry.Envelope;
 import org.deegree.geometry.metadata.SpatialMetadata;
 import org.deegree.tile.Tile;
 
 /**
  * The <code>TileStore</code> interface defines a deegree resource that can be used to read tiles. It's planned to
  * extend the interface to provide write access as well (that's why it has been called store already).
  * 
  * <p>
  * TODO: specify transactional methods, think about what the WMTS protocol will need (probably the
  * <code>TileMatrixMetadata</code>) and provide access here.
  * </p>
  * 
  * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
  * @author last edited by: $Author: mschneider $
  * 
  * @version $Revision: 31882 $, $Date: 2011-09-15 02:05:04 +0200 (Thu, 15 Sep 2011) $
  */
 public interface TileStore extends Resource {
 
     /**
      * Returns the spatial extent of this tile store. Once instantiated, the extent of a tile store must not change,
      * even when it's transactional.
      * 
      * @return the envelope and crs of this tile store, never null.
      */
     SpatialMetadata getMetadata();
 
     /**
      * Creates tile stream according to the parameters.
      * 
      * @param envelope
      *            the extent of tiles needed, never null
      * @param resolution
      *            the desired minimum resolution of tiles, must be positive
     * @return an iterator of tiles for the given envelope and resolution, never null.
      */
     Iterator<Tile> getTiles( Envelope envelope, double resolution );
 
 }
