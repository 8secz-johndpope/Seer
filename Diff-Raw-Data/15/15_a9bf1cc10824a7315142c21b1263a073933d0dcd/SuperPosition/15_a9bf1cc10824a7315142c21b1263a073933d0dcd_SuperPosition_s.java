 
 package org.biojava3.structure.quaternary.geometry;
 
 import javax.vecmath.AxisAngle4d;
 import javax.vecmath.Matrix4d;
 import javax.vecmath.Point3d;
 import javax.vecmath.Quat4d;
 import javax.vecmath.Vector3d;
 
 import org.biojava.bio.structure.jama.EigenvalueDecomposition;
 import org.biojava.bio.structure.jama.Matrix;
 
 /**
  *
  * @author Peter
  */
 public final class SuperPosition {
     
     public static Matrix4d superpose(Point3d[] x, Point3d[] y) {
         //superpose x onto y
         Point3d[] ref = clonePoint3dArray(y);
         
         Point3d ytrans = centroid(ref);
         ytrans.negate();
         translate(ytrans, ref);
         
         center(x);
         
         // calculate quaternion from relative orientation
         Quat4d q = quaternionOrientation(x, ref);
         
         Matrix4d rotTrans = new Matrix4d();
         rotTrans.set(q);
         
         // set translational component of transformation matrix
         ytrans.negate();
         rotTrans.setTranslation(new Vector3d(ytrans));
         
         // tranform coordinates
         transform(rotTrans, x);
         
         // TODO should include translation into transformation matrix
 
         return rotTrans;
     }
     
     public static Matrix4d superposeWithTranslation(Point3d[] x, Point3d[] y) {
         //superpose x onto y
              
         // translate to origin
     	Point3d[] xref = clonePoint3dArray(x);
     	Point3d xtrans = centroid(xref);
     	xtrans.negate();
     	translate(xtrans, xref);
 
     	Point3d[] yref = clonePoint3dArray(y);
         Point3d ytrans = centroid(yref);
         ytrans.negate();
         translate(ytrans, yref); 
         
         // calculate rotational component (rotation around origin)
         Quat4d q = quaternionOrientation(xref, yref);
         Matrix4d rotTrans = new Matrix4d();
         rotTrans.set(q);   
  
         // combine with x -> origin translation
         Matrix4d trans = new Matrix4d();
         trans.setIdentity();
         trans.setTranslation(new Vector3d(xtrans));
         rotTrans.mul(rotTrans, trans);
 
         // combine with origin -> y translation
         ytrans.negate();  
         Matrix4d transInverse = new Matrix4d(); 
         transInverse.setIdentity();     
         transInverse.setTranslation(new Vector3d(ytrans));
         rotTrans.mul(transInverse, rotTrans);
         
         // transform x coordinates onto y coordinate frame
         transform(rotTrans, x);
 
         return rotTrans;
     }
     
     public static Matrix4d superposeAtOrigin(Point3d[] x, Point3d[] y) {
         Quat4d q = quaternionOrientation(x, y);
         
         Matrix4d rotTrans = new Matrix4d();
         rotTrans.set(q);
         
         return rotTrans;
     }
 
     public static Matrix4d superposeAtOrigin(Point3d[] x, Point3d[] y, AxisAngle4d axisAngle) {
         Quat4d q = quaternionOrientation(x, y);    
         Matrix4d rotTrans = new Matrix4d();
         rotTrans.set(q);
         axisAngle.set(q);
         Vector3d axis = new Vector3d(axisAngle.x, axisAngle.y, axisAngle.z);
         if (axis.lengthSquared() < 1.0E-6) {
         	axisAngle.x = 0;
             axisAngle.y = 0;
             axisAngle.z = 1;
             axisAngle.angle = 0;
         } else {
         	axis.normalize();
         	axisAngle.x = axis.x;
         	axisAngle.y = axis.y;
         	axisAngle.z = axis.z;
         }
         transform(rotTrans, x);
//		System.out.println(rotTrans);
        
         return rotTrans;
     }
     
     
     
     
     public static double rmsd(Point3d[] x, Point3d[] y) {
         double sum = 0.0f;
         for (int i = 0; i < x.length; i++) {
             sum += x[i].distanceSquared(y[i]);
         }
         return (double)Math.sqrt(sum/x.length);
     }
 
     public static double rmsdMin(Point3d[] x, Point3d[] y) {
         double sum = 0.0f;
         for (int i = 0; i < x.length; i++) {
             double minDist = Double.MAX_VALUE;
             for (int j = 0; j < y.length; j++) {
                minDist = Math.min(minDist, x[i].distanceSquared(y[j]));
             }
             sum += minDist;
         }
         return (double)Math.sqrt(sum/x.length);
     }
 
     public static double GTSlikeScore(Point3d[] x, Point3d[] y) {
         int contacts = 0;
 
         for (Point3d px: x) {
             double minDist = Double.MAX_VALUE;
             
             for (Point3d py: y) {
                minDist = Math.min(minDist, px.distanceSquared(py));
             }
             
             if (minDist > 64) continue;
             contacts++;
 
             if (minDist > 16) continue;
             contacts++;
 
             if (minDist > 4) continue;
             contacts++;
 
             if (minDist > 1) continue;
             contacts++;
         }
 
        return contacts*25/x.length;
     }
     
     public static int contacts(Point3d[] x, Point3d[] y, double maxDistance) {
         int contacts = 0;
         for (int i = 0; i < x.length; i++) {
             double minDist = Double.MAX_VALUE;
             for (int j = 0; j < y.length; j++) {
                minDist = Math.min(minDist, x[i].distanceSquared(y[j]));
             }
             if (minDist < maxDistance*maxDistance) {
                 contacts++;
             }
         }
         return contacts;
     }
     
     public static void transform(Matrix4d rotTrans, Point3d[] x) {
         for (Point3d p: x) {
             rotTrans.transform(p);
         }
     }
     
     public static void translate(Point3d trans, Point3d[] x) {
         for (Point3d p: x) {
             p.add(trans);
         }
     }
     
     public static void center(Point3d[] x) {
         Point3d center = centroid(x);
         center.negate();
         translate(center, x);
     }
     
     public static Point3d centroid(Point3d[] x) {
         Point3d center = new Point3d();
         for (Point3d p: x) {
             center.add(p);
         }
         center.scale(1.0/x.length);
         return center;
     }
     
     private static Quat4d quaternionOrientation(Point3d[] a, Point3d[] b)  {
         Matrix m = calcFormMatrix(a, b);
         EigenvalueDecomposition eig = m.eig();
         double[][] v = eig.getV().getArray();
         Quat4d q = new Quat4d(v[1][3], v[2][3], v[3][3], v[0][3]);
         q.normalize();
         q.conjugate();
         return q;
     }
     
     private static Matrix calcFormMatrix(Point3d[] a, Point3d[] b) {
         double xx=0.0, xy=0.0, xz=0.0, yx=0.0, yy=0.0, yz=0.0, zx=0.0, zy=0.0, zz=0.0;
         
         for (int i = 0; i < a.length; i++) {
             xx += a[i].x * b[i].x;
             xy += a[i].x * b[i].y;
             xz += a[i].x * b[i].z;
             yx += a[i].y * b[i].x;
             yy += a[i].y * b[i].y;
             yz += a[i].y * b[i].z;
             zx += a[i].z * b[i].x;
             zy += a[i].z * b[i].y;
             zz += a[i].z * b[i].z;
         }
         
         double[][] f = new double[4][4];
         f[0][0] = xx + yy + zz;
         f[0][1] = zy - yz;
         f[1][0] = f[0][1];
         f[1][1] = xx - yy - zz;
         f[0][2] = xz - zx;
         f[2][0] = f[0][2];
         f[1][2] = xy + yx;
         f[2][1] = f[1][2];
         f[2][2] = yy - zz - xx;
         f[0][3] = yx - xy;
         f[3][0] = f[0][3];
         f[1][3] = zx + xz;
         f[3][1] = f[1][3];
         f[2][3] = yz + zy;
         f[3][2] = f[2][3];
         f[3][3] = zz - xx - yy;
         
         return new Matrix(f);
     }
     
     public static Point3d[] clonePoint3dArray(Point3d[] x) {
         Point3d[] clone = new Point3d[x.length];
         for (int i = 0; i < x.length; i++) {
            clone[i] = new Point3d(x[i]);
         }
         return clone;
     }
 }
