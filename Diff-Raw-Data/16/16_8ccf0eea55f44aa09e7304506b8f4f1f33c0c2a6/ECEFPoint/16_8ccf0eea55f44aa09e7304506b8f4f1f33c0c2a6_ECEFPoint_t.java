 // **********************************************************************
 // 
 // <copyright>
 // 
 //  BBN Technologies, a Verizon Company
 //  10 Moulton Street
 //  Cambridge, MA 02138
 //  (617) 873-8000
 // 
 //  Copyright (C) BBNT Solutions LLC. All rights reserved.
 // 
 // </copyright>
 // **********************************************************************
 // 
 // $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/proj/coords/ECEFPoint.java,v $
 // $RCSfile: ECEFPoint.java,v $
// $Revision: 1.2 $
// $Date: 2004/09/23 19:27:27 $
 // $Author: dietrick $
 // 
 // **********************************************************************
 
 
 // **********************************************************************
 // 
 // Based on coordinate conversion utilities in GeoTools
 //
 // Note: Height calculations are present, but commented out.
 // 
 // **********************************************************************
 
 
package  com.bbn.openmap.proj.coords;
 
 import java.io.DataInputStream;
 import java.io.DataOutputStream;
 import java.io.IOException;
 
import com.bbn.openmap.LatLonPoint;
import com.bbn.openmap.MoreMath;

 /**
  * From: http://www.commlinx.com.au/Datum%20Transformation%20Description.html :<P>
  * The Cartesian coordinate frame of reference used in GPS/GLONASS is
  * called Earth-Centered, Earth-Fixed (ECEF). ECEF uses
  * three-dimensional XYZ coordinates (in meters) to describe the
  * location of a GPS user or satellite. The term "Earth- Centered"
  * comes from the fact that the origin of the axis (0,0,0) is located
  * at the mass center of gravity (determined through years of tracking
  * satellite trajectories). The term "Earth-Fixed" implies that the
  * axes are fixed with respect to the earth (that is, they rotate with
  * the earth). The Z-axis pierces the North Pole, and the XY-axis
  * defines the equatorial plane (Figure 1).<P>
 
  * ECEF coordinates are expressed in a reference system that is
  * related to mapping representations. Because the earth has a complex
  * shape, a simple, yet accurate, method to approximate the earth's
  * shape is required. The use of a reference ellipsoid allows for the
  * conversion of the ECEF coordinates to the more commonly used
  * geodetic-mapping coordinates of Latitude, Longitude, and Altitude
  * (LLA).
  */
 public class ECEFPoint {
 
     protected static double EQUIVALENT_TOLERANCE = 0.001;
     
     protected double x_ = 0.0;
     protected double y_ = 0.0;
     protected double z_ = 0.0;
 
     /**
      * Construct a default ECEFPoint.
      */
     public ECEFPoint() {
     }
 
     /**
      * Construct an ECEFPoint
      *
      * @param double x,y,z
      */
     public ECEFPoint(double x, double y, double z) {
         setECEF(x, y, z);
     }
 
     /**
      * Construct an ECEFPoint
      *
      * @param ECEFPoint pt
      */
     public ECEFPoint(ECEFPoint pt) {
         x_ = pt.x_;
         y_ = pt.y_;
         z_ = pt.z_;
     }
 
     /**
      * Construct an ECEFPoint
      *
      * @param float x,y,z
      */
     public ECEFPoint(float x, float y, float z) {
         this((double)x, (double)y, (double)z);
     }
 
     public static ECEFPoint LLtoECEF(LatLonPoint llpoint) {
         return LLtoECEF(llpoint, new ECEFPoint());
     }
 
 
     public static ECEFPoint LLtoECEF(LatLonPoint llpoint, ECEFPoint ecef) {
         if (ecef == null) {
             ecef = new ECEFPoint();
         }
 
         ecef.setLatLon(llpoint);
         return ecef;
     }
 
     /**
      * Returns a string representation of the object.
      * @return String representation
      */
     public String toString() {
         return  "ECEFPoint[x=" + x_ + ",y=" + y_ + ",z=" + z_ + "]";
     }
 
     /**
      * Set x.
      */
     public void setx(double x) {
         x_ = x;
     }
 
     /**
      * Set y.
      */
     public void sety(double y) {
         y_ = y;
     }
 
     /**
      * Set z.
      */
     public void setz(double z) {
         z_ = z;
     }
 
     /**
      * Set x y z.
      */
     private void setECEF(double x, double y, double z) {
         x_ = x;
         y_ = y;
         z_ = z;
     }
 
     /**
      * Set ECEFPoint.
      */
     private void setECEF(ECEFPoint pt) {
         x_ = pt.x_;
         y_ = pt.y_;
         z_ = pt.z_;
     }
 
     /**
      * Get x
      */
     public double getx() {
         return  x_;
     }
 
     /**
      * Get y
      */
     public double gety() {
         return  y_;
     }
 
     /**
      * Get z
      */
     public double getz() {
         return  z_;
     }
 
     /**
      * Determines whether two ECEFPoints are equal.
      * @param obj Object
      * @return Whether the two points are equal 
      */
     public boolean equals(Object obj) {
         if (obj instanceof ECEFPoint) {
             ECEFPoint pt = (ECEFPoint)obj;
            return  (MoreMath.approximately_equal(x_, pt.x_, EQUIVALENT_TOLERANCE) && 
            			MoreMath.approximately_equal(y_, pt.y_, EQUIVALENT_TOLERANCE) && 
					MoreMath.approximately_equal(z_, pt.z_, EQUIVALENT_TOLERANCE));
         }
         return  false;
     }
 
     /**
      * Write object.
      * @param s DataOutputStream
      */
     public void write(DataOutputStream s) throws IOException {
         // Write my information
         s.writeDouble(x_);
         s.writeDouble(y_);
         s.writeDouble(z_);
     }
 
     /**
      * Read object.  
      * @param s DataInputStream
      */
     public void read(DataInputStream s) throws IOException {
         setECEF(s.readDouble(), s.readDouble(), s.readDouble());
     }
     
     /**
      * Set an ECEFPoint from a LatLonPoint
      *
      * @param LatLonPoint pt
      */
     public void setLatLon(LatLonPoint pt) {
         setLatLon(pt.getLatitude(), pt.getLongitude());
     }
     
     /**
      * Set an ECEFPoint from a LatLon
      *
      * @param float lat, lon
      */
     public void setLatLon(float lat, float lon) {
 
         final double a = 6378137.0; //WGS84 semimajor (meters)
         final double b = 6356752.3142; //WGS84 semiminor (meters)
         final double a2 = a*a;
         final double b2 = b*b;
         final double e2  = (a2 - b2) / a2;
         
         final double L = Math.toRadians(lon); // Longitude
         final double P = Math.toRadians(lat);  // Latitude
         final double h = 0;                                  // Height above the ellipsoid (m)
             
         final double cosLat = Math.cos(P);
         final double sinLat = Math.sin(P);
         final double rn     = a / Math.sqrt(1 - e2 * (sinLat*sinLat));
             
         final double x = (rn + h) * cosLat * Math.cos(L); // X: Toward prime meridian
         final double y = (rn + h) * cosLat * Math.sin(L); // Y: Toward East
         final double z = (rn * (1-e2) + h) * sinLat;      // Z: Toward North
 
         this.setECEF(x, y, z);    
     }
     
     /**
      * Return a LatLonPoint
      */
     public LatLonPoint getLatLon() {
         
         final double a = 6378137.0; //WGS84 semimajor (meters)
         final double b = 6356752.3142; //WGS84 semiminor (meters)
         final double a2 = a*a;
         final double b2 = b*b;
         final double e2  = (a2 - b2) / a2;
         final double ep2 = (a2 - b2) / b2;
 
         /**
          * Cosine of 67.5 degrees.
          */
         final double COS_67P5 = 0.38268343236508977;
     
         /**
          * Toms region 1 constant.
          */
         final double AD_C = 1.0026000;
 
 
         final double x = x_; // Toward prime meridian
         final double y = y_; // Toward East
         final double z = z_; // Toward North
             
         // Note: The Java version of 'atan2' work correctly for x==0.
         //       No need for special handling like in the C version.
         //       No special handling neither for latitude. Formulas
         //       below are generic enough, considering that 'atan'
         //       work correctly with infinities (1/0).
             
         // Note: Variable names follow the notation used in Toms, Feb 1996
         final double      W2 = x*x + y*y;                       // square of distance from Z axis
         final double      W  = Math.sqrt(W2);                   // distance from Z axis
         final double      T0 = z * AD_C;                        // initial estimate of vertical component
         final double      S0 = Math.sqrt(T0*T0 + W2);           // initial estimate of horizontal component
         final double  sin_B0 = T0 / S0;                         // sin(B0), B0 is estimate of Bowring aux variable
         final double  cos_B0 = W / S0;                          // cos(B0)
         final double sin3_B0 = sin_B0 * sin_B0 * sin_B0;        // cube of sin(B0)
         final double      T1 = z + b * ep2 * sin3_B0;           // corrected estimate of vertical component
         final double     sum = W - a*e2*(cos_B0*cos_B0*cos_B0); // numerator of cos(phi1)
         final double     S1 = Math.sqrt(T1*T1 + sum * sum);    // corrected estimate of horizontal component
         final double sin_p1 = T1 / S1;                         // sin(phi1), phi1 is estimated latitude
         final double cos_p1 = sum / S1;                        // cos(phi1)
             
         final double longitude = Math.toDegrees(Math.atan2(y      , x     ));
         final double  latitude = Math.toDegrees(Math.atan(sin_p1 / cos_p1));
         // final double    height;
             
         // if (computeHeight) {
         //    final double rn = a/Math.sqrt(1-e2*(sin_p1*sin_p1)); // Earth radius at location
         //    if      (cos_p1 >= +COS_67P5) height = W / +cos_p1 - rn;
         //    else if (cos_p1 <= -COS_67P5) height = W / -cos_p1 - rn;
         //    else                          height = z / sin_p1 + rn*(e2 - 1.0);
         // }
 
         
         LatLonPoint dst = new LatLonPoint((float) latitude, (float) longitude); 
         return dst;
     }
 }
