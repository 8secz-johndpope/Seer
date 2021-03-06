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
 // $Source: /cvs/distapps/openmap/src/openmap/com/bbn/openmap/proj/Planet.java,v $
 // $RCSfile: Planet.java,v $
// $Revision: 1.1.1.1 $
// $Date: 2003/02/14 21:35:49 $
 // $Author: dietrick $
 // 
 // **********************************************************************
 
 
 package com.bbn.openmap.proj;
 
 import com.bbn.openmap.MoreMath;
 
 /**
  * Planet datums and parameters.
  * These values are taken from John Snyder's <i>Map Projections --A Working
  * Manual</i>
  * You should add datums as needed, consult the ellips.dat file.
  */
 public class Planet {
 
     // Solar system id's.  Add new ones as needed.
     final public static transient int Earth = 3;
     final public static transient int Mars = 4;
 
 
     // WGS84 / GRS80 datums
     final public static transient float wgs84_earthPolarRadiusMeters = 6356752.3142f;
     final public static transient float wgs84_earthEquatorialRadiusMeters = 6378137.0f;
     final public static transient float wgs84_earthFlat =
 	1 - (wgs84_earthPolarRadiusMeters/wgs84_earthEquatorialRadiusMeters);// 1 - (minor/major) = 1/298.257
     final public static transient float wgs84_earthEccen =
 	(float)Math.sqrt(2*wgs84_earthFlat - (wgs84_earthFlat*wgs84_earthFlat));// sqrt(2*f - f^2) = 0.081819221f
 
     final public static transient float wgs84_earthEquatorialCircumferenceMeters =
 	MoreMath.TWO_PI*wgs84_earthEquatorialRadiusMeters;
     final public static transient float wgs84_earthEquatorialCircumferenceKM =
 	wgs84_earthEquatorialCircumferenceMeters/1000f;
     final public static transient float wgs84_earthEquatorialCircumferenceMiles =
 	wgs84_earthEquatorialCircumferenceKM*0.62137119f;//HACK use UNIX units?
     final public static transient float wgs84_earthEquatorialCircumferenceNMiles =
	wgs84_earthEquatorialCircumferenceKM*0.5399568f;//HACK use UNIX units?

 
     // Mars
     final public static transient float marsEquatorialRadius = 3393400.0f;// meters
     final public static transient float marsEccen = 0.101929f;// eccentricity e
     final public static transient float marsFlat = 0.005208324f;// 1-(1-e^2)^1/2
 
 
     // International 1974
     final public static transient float international1974_earthPolarRadiusMeters = 6356911.946f;
     final public static transient float international1974_earthEquatorialRadiusMeters = 6378388f;
     final public static transient float international1974_earthFlat =
 	1 - (international1974_earthPolarRadiusMeters/
 		international1974_earthEquatorialRadiusMeters);// 1 - (minor/major) = 1/297
 
 
     // Extra scale constant for better viewing of maps (do not use this to
     // calculate anything but points to be viewed!)
     public transient static int defaultPixelsPerMeter = 3272;// 3384: mattserver/Map.C, 3488: dcw
 
 
     // cannot construct
     private Planet () {}
 }
