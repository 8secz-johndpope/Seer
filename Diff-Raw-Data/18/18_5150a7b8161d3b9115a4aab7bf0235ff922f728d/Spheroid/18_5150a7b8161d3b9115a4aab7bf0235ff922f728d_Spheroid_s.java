 /*
  * Geotools - OpenSource mapping toolkit
  * (C) 2002, Centre for Computational Geography
  * (C) 2001, Institut de Recherche pour le Dveloppement
  *
  *    This library is free software; you can redistribute it and/or
  *    modify it under the terms of the GNU Lesser General Public
  *    License as published by the Free Software Foundation; either
  *    version 2.1 of the License, or (at your option) any later version.
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
  *
  * Contacts:
  *     UNITED KINGDOM: James Macgill
  *             mailto:j.macgill@geog.leeds.ac.uk
  *
  *     FRANCE: Surveillance de l'Environnement Assiste par Satellite
  *             Institut de Recherche pour le Dveloppement / US-Espace
  *             mailto:seasnet@teledetection.fr
  *
  *     CANADA: Observatoire du Saint-Laurent
  *             Institut Maurice-Lamontagne
  *             mailto:osl@osl.gc.ca
  *
  *    This package contains documentation from OpenGIS specifications.
  *    OpenGIS consortium's work is fully acknowledged here.
  */
 package org.geotools.cs;
 
 // Geotools dependencies
 import org.geotools.units.Unit;
 
 
 /**
  * A ellipsoid which is spherical. This ellipsoid implements a faster
  * {@link #orthodromicDistance} method.
  *
 * @version $Id: Spheroid.java,v 1.1 2002/07/11 23:56:07 desruisseaux Exp $
  * @author Martin Desruisseaux
  */
 final class Spheroid extends Ellipsoid {
     
     /**
      * Constructs a new sphere using the specified radius.
      *
      * @param name          Name of this sphere.
      * @param radius        The equatorial and polar radius.
      * @param ivfDefinitive <code>true</code> if the inverse flattening is definitive.
      * @param unit          The units of the radius value.
      */
     protected Spheroid(CharSequence name, double radius, boolean ivfDefinitive, Unit unit) {
         super(name, check("radius", radius), radius, Double.POSITIVE_INFINITY, ivfDefinitive, unit);
     }
     
     /**
     * Returns an <em>estimation</em> of orthodromic distance between two
     * geographic coordinates. The orthodromic distance is the shortest
     * distance between two points on a sphere's surface.  The orthodromic
     * path is always on a great circle. Another possible distance measurement
     * is the loxodromic distance, which is a longer distance on a path with
     * a constant direction on the compass.
      *
      * @param  x1 Longitude of first point (in degrees).
      * @param  y1 Latitude of first point (in degrees).
      * @param  x2 Longitude of second point (in degrees).
      * @param  y2 Latitude of second point (in degrees).
     * @return The orthodromic distance (in the units of this ellipsoid).
      */
     public double orthodromicDistance(double x1, double y1, double x2, double y2) {
         /*
          * The calculation of orthodromic distance on an ellipsoidal surface is complex,
          * subject to rounding errors and has no solution near the poles. In some situation
          * we use a calculation based on a spherical shape of the earth.  A Fortran program
          * which calculates orthodromic distances on an ellipsoidal surface can be downloaded
          * from the NOAA site:
          *
          *            ftp://ftp.ngs.noaa.gov/pub/pcsoft/for_inv.3d/source/
          */
         y1 = Math.toRadians(y1);
         y2 = Math.toRadians(y2);
         final double dx = Math.toRadians(Math.abs(x2-x1) % 360);
         double rho = Math.sin(y1)*Math.sin(y2) + Math.cos(y1)*Math.cos(y2)*Math.cos(dx);
         assert Math.abs(rho) < 1.0000001 : rho;
         if (rho>+1) rho=+1; // Catch rounding error.
         if (rho<-1) rho=-1; // Catch rounding error.
         final double distance = Math.acos(rho)*getSemiMajorAxis();
         /*
          * Compare the distance with the orthodromic distance using ellipsoidal
          * computation. This should be close to the same.
          */
         try {
             double delta;
             assert (delta = Math.abs(super.orthodromicDistance(x1, Math.toDegrees(y1), 
                                                                x2, Math.toDegrees(y2))-distance))
                                                                < getSemiMajorAxis()/1E+9 : delta;
         } catch (ArithmeticException exception) {
             // The ellipsoidal model do not converge. Give up the assertion test.
         }
         return distance;
     }
 }
