 //
 // Contour2D.java
 //
 
 /*
 VisAD system for interactive analysis and visualization of numerical
 data.  Copyright (C) 1996 - 2002 Bill Hibbard, Curtis Rueden, Tom
 Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
 Tommy Jasmin.
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public
 License as published by the Free Software Foundation; either
 version 2 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Library General Public License for more details.
 
 You should have received a copy of the GNU Library General Public
 License along with this library; if not, write to the Free
 Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 MA 02111-1307, USA
 */
 
 package visad;
 
 import java.applet.*;
 import java.awt.*;
 import java.awt.event.*;
 import visad.util.Util;
 
 /**
    Contour2D is a class equipped with a 2-D contouring function.<P>
 */
 public class Contour2D {
 
   // Applet variables
   protected Contour2D con;
   protected int whichlabels = 0;
   protected boolean showgrid;
   protected int rows, cols, scale;
   protected int[] num1, num2, num3, num4;
   protected float[][] vx1, vy1, vx2, vy2, vx3, vy3, vx4, vy4;
 
   /**
    * Compute contour lines for a 2-D array.  If the interval is negative,
    * then contour lines less than base will be drawn as dashed lines.
    * The contour lines will be computed for all V such that:<br>
    *           lowlimit <= V <= highlimit<br>
    *     and   V = base + n*interval  for some integer n<br>
    * Note that the input array, g, should be in column-major (FORTRAN) order.
    *
    * @param    g         the 2-D array to contour.
    * @param    nr        size of 2-D array in rows
    * @param    nc        size of 2-D array in columns.
    * @param    interval  contour interval
    * @param    lowlimit  the lower limit on values to contour.
    * @param    highlimit the upper limit on values to contour.
    * @param    base      base value to start contouring at.
    * @param    vx1       array to put contour line vertices (x value)
    * @param    vy1       array to put contour line vertices (y value)
    * @param    maxv1     size of vx1, vy1 arrays
    * @param    numv1     pointer to int to return number of vertices in vx1,vy1
    * @param    vx2       array to put 'hidden' contour line vertices (x value)
    * @param    vy2       array to put 'hidden' contour line vertices (y value)
    * @param    maxv2     size of vx2, vy2 arrays
    * @param    numv2     pointer to int to return number of vertices in vx2,vy2
    * @param    vx3       array to put contour label vertices (x value)
    * @param    vy3       array to put contour label vertices (y value)
    * @param    maxv3     size of vx3, vy3 arrays
    * @param    numv3     pointer to int to return number of vertices in vx3,vy3
    * @param    vx4       array to put contour label vertices, inverted (x value)
    * @param    vy4       array to put contour label vertices, inverted (y value)
    *                     <br>** see note for VxB and VyB in PlotDigits.java **
    * @param    maxv4     size of vx4, vy4 arrays
    * @param    numv4     pointer to int to return number of vertices in vx4,vy4
    */
   public static void contour( float g[], int nr, int nc, float interval,
                       float lowlimit, float highlimit, float base,
                       float vx1[][], float vy1[][], float[][] vz1, int maxv1, int[] numv1,
                       float vx2[][], float vy2[][], float[][] vz2, int maxv2, int[] numv2,
                       float vx3[][], float vy3[][], float[][] vz3, int maxv3, int[] numv3,
                       float vx4[][], float vy4[][], float[][] vz4, int maxv4, int[] numv4,
                       byte[][] auxValues, byte[][] auxLevels1,
                       byte[][] auxLevels2, byte[][] auxLevels3, boolean[] swap,
                       boolean fill, float[][] tri, byte[][] tri_color,
                       float[][][] grd_normals, float[][] tri_normals,
                       byte[][] interval_colors, float[][][][] lbl_vv, byte[][][][] lbl_cc,
                       float[][][] lbl_loc, double scale_ratio, double label_size,
                       Gridded3DSet spatial_set)
                           throws VisADException
   {
     boolean[] dashes = {false};
     float[] intervals =
       intervalToLevels(interval, lowlimit, highlimit, base, dashes);
     boolean dash = dashes[0];
 
     contour( g, nr, nc, intervals,
              lowlimit, highlimit, base, dash,
              vx1, vy1, vz1, maxv1, numv1,
              vx2, vy2, vz2, maxv2, numv2,
              vx3, vy3, vz3, maxv3, numv3,
              vx4, vy4, vz4, maxv4, numv4,
              auxValues, auxLevels1,
              auxLevels2, auxLevels3, swap,
              fill, tri, tri_color,
              grd_normals, tri_normals,
              interval_colors, lbl_vv, lbl_cc, lbl_loc, scale_ratio, label_size,
              spatial_set);
   }
 
   /**
    * Returns an array of contour values and an indication on whether to use
    * dashed lines below the base value.
    *
    * @param interval            The contouring interval.  Must be non-zero.
    *                            If the interval is negative, then contour lines
    *                            less than the base will be drawn as dashed
    *                            lines.  Must not be NaN.
    * @param low                 The minimum contour value.  The returned array
    *                            will not contain a value below this.  Must not
    *                            be NaN.
    * @param high                The maximum contour value.  The returned array
    *                            will not contain a value above this.  Must not
    *                            be NaN.
    * @param ba                  The base contour value.  The returned values
    *                            will be integer multiples of the interval
    *                            away from this this value. Must not be NaN.
    * dash                       Whether or not contour lines less than the base
    *                            should be drawn as dashed lines.  This is a
    *                            computed and returned value.
    * @throws VisADException     The contour interval is zero or too small.
    */
   public static float[] intervalToLevels(float interval, float low,
                                          float high, float ba, boolean[] dash)
         throws VisADException {
     float[] levs = null;
 
     if (interval == 0.0) {
       throw new VisADException("Contour interval cannot be zero");
     }
 
     dash[0] = false;
     if (interval < 0) {
         dash[0] = true;
         interval = -interval;
     }
 
     // compute list of contours
     // compute nlo and nhi, for low and high contour values in the box
     long nlo = Math.round((Math.ceil((low - ba) / Math.abs(interval))));
     long nhi = Math.round((Math.floor((high - ba) / Math.abs(interval))));
 
     // how many contour lines are needed.
     int numc = (int) (nhi - nlo) + 1;
     if (numc < 1) return levs;
     if (numc > 4000) {
       throw new VisADException("Contour interval too small");
     }
 
     try {
       levs = new float[numc];
     } catch (OutOfMemoryError e) {
       throw new VisADException("Contour interval too small");
     }
 
     for(int i = 0; i < numc; i++) {
       levs[i] = ba + (nlo + i) * interval;
     }
 
     return levs;
   }
 
   /**
    * Compute contour lines for a 2-D array.  If the interval is negative,
    * then contour lines less than base will be drawn as dashed lines.
    * The contour lines will be computed for all V such that:<br>
    *           lowlimit <= V <= highlimit<br>
    *     and   V = base + n*interval  for some integer n<br>
    * Note that the input array, g, should be in column-major (FORTRAN) order.
    *
    * @param    g         the 2-D array to contour.
    * @param    nr        size of 2-D array in rows
    * @param    nc        size of 2-D array in columns.
    * @param    values    the values to be plotted
    * @param    lowlimit  the lower limit on values to contour.
    * @param    highlimit the upper limit on values to contour.
    * @param    base      base value to start contouring at.
    * @param    dash      boolean to dash contours below base or not
    * @param    vx1       array to put contour line vertices (x value)
    * @param    vy1       array to put contour line vertices (y value)
    * @param    maxv1     size of vx1, vy1 arrays
    * @param    numv1     pointer to int to return number of vertices in vx1,vy1
    * @param    vx2       array to put 'hidden' contour line vertices (x value)
    * @param    vy2       array to put 'hidden' contour line vertices (y value)
    * @param    maxv2     size of vx2, vy2 arrays
    * @param    numv2     pointer to int to return number of vertices in vx2,vy2
    * @param    vx3       array to put contour label vertices (x value)
    * @param    vy3       array to put contour label vertices (y value)
    * @param    maxv3     size of vx3, vy3 arrays
    * @param    numv3     pointer to int to return number of vertices in vx3,vy3
    * @param    vx4       array to put contour label vertices, inverted (x value)
    * @param    vy4       array to put contour label vertices, inverted (y value)
    *                     <br>** see note for VxB and VyB in PlotDigits.java **
    * @param    maxv4     size of vx4, vy4 arrays
    * @param    numv4     pointer to int to return number of vertices in vx4,vy4
    */
   public static void contour( float g[], int nr, int nc, float[] values,
                       float lowlimit, float highlimit, float base, boolean dash,
                       float vx1[][], float vy1[][], float[][] vz1, int maxv1, int[] numv1,
                       float vx2[][], float vy2[][], float[][] vz2, int maxv2, int[] numv2,
                       float vx3[][], float vy3[][], float[][] vz3, int maxv3, int[] numv3,
                       float vx4[][], float vy4[][], float[][] vz4, int maxv4, int[] numv4,
                       byte[][] auxValues, byte[][] auxLevels1,
                       byte[][] auxLevels2, byte[][] auxLevels3, boolean[] swap,
                       boolean fill, float[][] tri, byte[][] tri_color,
                       float[][][] grd_normals, float[][] tri_normals,
                       byte[][] interval_colors, float[][][][] lbl_vv, byte[][][][] lbl_cc,
                       float[][][] lbl_loc, double scale_ratio, double label_size,
                       Gridded3DSet spatial_set)
                           throws VisADException {
 /*
 System.out.println("interval = " + values[0] + " lowlimit = " + lowlimit +
                    " highlimit = " + highlimit + " base = " + base);
 boolean any = false;
 boolean anymissing = false;
 boolean anynotmissing = false;
 */
 
 //System.out.println("contour: swap = " + swap[0] + " " + swap[1] + " " + swap[2]);
 
     dash = (fill == true) ? false : dash;
     PlotDigits plot = new PlotDigits();
     int ir, ic;
     int nrm, ncm;
     int numc, il;
     int lr, lc, lc2, lrr, lr2, lcc;
     float xd, yd ,xx, yy;
     float xdd, ydd;
 //  float clow, chi;
     float gg;
     int maxsize = maxv1+maxv2;
     float[] vx = new float[maxsize];
     float[] vy = new float[maxsize];
 
     // WLH 21 April 2000
     // int[] ipnt = new int[2*maxsize];
     int[] ipnt = new int[nr*nc+4];
 
     int nump, ip;
     int numv;
 
 /* DRM 1999-05-18, CTR 29 Jul 1999: values could be null */
     float[] myvals = null;
     if (values != null) {
       myvals = (float[]) values.clone();
       java.util.Arrays.sort(myvals);
     }
     int low;
     int hi;
 
     int t;
 
     byte[][] auxLevels = null;
     int naux = (auxValues != null) ? auxValues.length : 0;
     if (naux > 0) {
       if (auxLevels1 == null || auxLevels1.length != naux ||
           auxLevels2 == null || auxLevels2.length != naux ||
           auxLevels3 == null || auxLevels3.length != naux) {
         throw new SetException("Contour2D.contour: "
                               +"auxLevels length doesn't match");
       }
       for (int i=0; i<naux; i++) {
         if (auxValues[i].length != g.length) {
           throw new SetException("Contour2D.contour: "
                                 +"auxValues lengths don't match");
         }
       }
       auxLevels = new byte[naux][maxsize];
     }
     else {
       if (auxLevels1 != null || auxLevels2 != null || auxLevels3 != null) {
         throw new SetException("Contour2D.contour: "
                               +"auxValues null but auxLevels not null");
       }
     }
 
     // initialize vertex counts
     numv1[0] = 0;
     numv2[0] = 0;
     numv3[0] = 0;
     numv4[0] = 0;
 
     if (values == null) return; // WLH 24 Aug 99
 
 /*  DRM: 1999-05-19 - Not needed since dash is a boolean
     // check for bad contour interval
     if (interval==0.0) {
       throw new DisplayException("Contour2D.contour: interval cannot be 0");
     }
     if (!dash) {
       // draw negative contour lines as dashed lines
       interval = -interval;
       idash = 1;
     }
     else {
       idash = 0;
     }
 */
 
     nrm = nr-1;
     ncm = nc-1;
 
     xdd = ((nr-1)-0.0f)/(nr-1.0f); // = 1.0
     ydd = ((nc-1)-0.0f)/(nc-1.0f); // = 1.0
     /**-TDR xd = xdd - 0.0001f;
            yd = ydd - 0.0001f;  gap too big **/
     xd = xdd - 0.000005f;
     yd = ydd - 0.000005f;
 
     /*
      * set up mark array
      * mark= 0 if avail for label center,
      *       2 if in label, and
      *       1 if not available and not in label
      *
      * lr and lc give label size in grid boxes
      * lrr and lcc give unavailable radius
      */
     if (swap[0]) {
       lr = 1+(nr-2)/10;
       lc = 1+(nc-2)/50;
     }
     else {
       lr = 1+(nr-2)/50;
       lc = 1+(nc-2)/10;
     }
     lc2 = lc/2;
     lr2 = lr/2;
     lrr = 1+(nr-2)/8;
     lcc = 1+(nc-2)/8;
 
     // allocate mark array
     char[] mark = new char[nr * nc];
 
     // initialize mark array to zeros
     for (int i=0; i<nr * nc; i++) mark[i] = 0;
 
     // set top and bottom rows to 1
     for (ic=0;ic<nc;ic++) {
       for (ir=0;ir<lr;ir++) {
         mark[ (ic) * nr + (ir) ] = 1;
         mark[ (ic) * nr + (nr-ir-2) ] = 1;
       }
     }
 
     // set left and right columns to 1
     for (ir=0;ir<nr;ir++) {
       for (ic=0;ic<lc;ic++) {
          mark[ (ic) * nr + (ir) ] = 1;
          mark[ (nc-ic-2) * nr + (ir) ] = 1;
       }
     }
     numv = nump = 0;
 
 
     //- color fill arrays
     byte[][]   color_bin = interval_colors;
     byte[][][] o_flags   = new byte[nrm][ncm][];
     short[][]  n_lines   = new short[nrm][ncm];
     short[][]  ctrLow    = new short[nrm][ncm];
    
     ContourStripSet ctrSet =
       new ContourStripSet(nrm, myvals, swap, scale_ratio, label_size, nr, nc, spatial_set);
 
     // compute contours
     for (ir=0; ir<nrm; ir++) {
       xx = xdd*ir+0.0f; // = ir
       for (ic=0; ic<ncm; ic++) {
         float ga, gb, gc, gd;
         float gv, gn, gx;
         float tmp1, tmp2;
 
         // WLH 21 April 2000
         // if (numv+8 >= maxsize || nump+4 >= 2*maxsize) {
         if (numv+8 >= maxsize) {
           // allocate more space
           maxsize = 2 * maxsize;
 /* WLH 21 April 2000
           int[] tt = ipnt;
           ipnt = new int[2 * maxsize];
           System.arraycopy(tt, 0, ipnt, 0, nump);
 */
           float[] tx = vx;
           float[] ty = vy;
           vx = new float[maxsize];
           vy = new float[maxsize];
           System.arraycopy(tx, 0, vx, 0, numv);
           System.arraycopy(ty, 0, vy, 0, numv);
           if (naux > 0) {
             byte[][] ta = auxLevels;
             auxLevels = new byte[naux][maxsize];
             for (int i=0; i<naux; i++) {
               System.arraycopy(ta[i], 0, auxLevels[i], 0, numv);
             }
           }
         }
 
         // save index of first vertex in this grid box
         ipnt[nump++] = numv;
 
         yy = ydd*ic+0.0f; // = ic
 /*
 ga = ( g[ (ic) * nr + (ir) ] );
 gb = ( g[ (ic) * nr + (ir+1) ] );
 gc = ( g[ (ic+1) * nr + (ir) ] );
 gd = ( g[ (ic+1) * nr + (ir+1) ] );
 boolean miss = false;
 if (ga != ga || gb != gb || gc != gc || gd != gd) {
   miss = true;
   System.out.println("ic, ir = " + ic + "  " + ir + " gabcd = " +
                      ga + " " + gb + " " + gc + " " + gd);
 }
 */
 /*
 if (ga != ga || gb != gb || gc != gc || gd != gd) {
   if (!anymissing) {
     anymissing = true;
     System.out.println("missing");
   }
 }
 else {
   if (!anynotmissing) {
     anynotmissing = true;
     System.out.println("notmissing");
   }
 }
 */
         // get 4 corner values, skip box if any are missing
         ga = ( g[ (ic) * nr + (ir) ] );
         // test for missing
         if (ga != ga) continue;
         gb = ( g[ (ic) * nr + (ir+1) ] );
         // test for missing
         if (gb != gb) continue;
         gc = ( g[ (ic+1) * nr + (ir) ] );
         // test for missing
         if (gc != gc) continue;
         gd = ( g[ (ic+1) * nr + (ir+1) ] );
         // test for missing
         if (gd != gd) continue;
 
         byte[] auxa = null;
         byte[] auxb = null;
         byte[] auxc = null;
         byte[] auxd = null;
         if (naux > 0) {
           auxa = new byte[naux];
           auxb = new byte[naux];
           auxc = new byte[naux];
           auxd = new byte[naux];
           for (int i=0; i<naux; i++) {
             auxa[i] = auxValues[i][(ic) * nr + (ir)];
             auxb[i] = auxValues[i][(ic) * nr + (ir+1)];
             auxc[i] = auxValues[i][(ic+1) * nr + (ir)];
             auxd[i] = auxValues[i][(ic+1) * nr + (ir+1)];
           }
         }
 
         // find average, min, and max of 4 corner values
         gv = (ga+gb+gc+gd)/4.0f;
 
         // gn = MIN4(ga,gb,gc,gd);
         tmp1 = ( (ga) < (gb) ? (ga) : (gb) );
         tmp2 = ( (gc) < (gd) ? (gc) : (gd) );
         gn = ( (tmp1) < (tmp2) ? (tmp1) : (tmp2) );
 
         // gx = MAX4(ga,gb,gc,gd);
         tmp1 = ( (ga) > (gb) ? (ga) : (gb) );
         tmp2 = ( (gc) > (gd) ? (gc) : (gd) );
         gx = ( (tmp1) > (tmp2) ? (tmp1) : (tmp2) );
 
 
 /*  remove for new signature, replace with code below
         // compute clow and chi, low and high contour values in the box
         tmp1 = (gn-base) / interval;
         clow = base + interval * (( (tmp1) >= 0 ? (int) ((tmp1) + 0.5)
                                                 : (int) ((tmp1)-0.5) )-1);
         while (clow<gn) {
           clow += interval;
         }
 
         tmp1 = (gx-base) / interval;
         chi = base + interval * (( (tmp1) >= 0 ? (int) ((tmp1) + 0.5)
                                                : (int) ((tmp1)-0.5) )+1);
         while (chi>gx) {
           chi -= interval;
         }
 
         // how many contour lines in the box:
         tmp1 = (chi-clow) / interval;
         numc = 1+( (tmp1) >= 0 ? (int) ((tmp1) + 0.5) : (int) ((tmp1)-0.5) );
 
         // gg is current contour line value
         gg = clow;
 */
 
         low = 0;
         hi = myvals.length - 1;
         if (myvals[low] > gx || myvals[hi] < gn) // no contours
         {
             numc = 1;
         }
         else   // some inside the box
         {
             for (int i = 0; i < myvals.length; i++)
             {
                  if (i == 0 && myvals[i] >= gn) { low = i; }
                  else if (myvals[i] >= gn && myvals[i-1] < gn) { low = i; }
                  else if (myvals[i] > gx && myvals[i-1] < gn) { hi = i; }
             }
             numc = hi - low + 1;
         }
         gg = myvals[low];
 
 /*
 if (!any && numc > 0) {
   System.out.println("numc = " + numc + " clow = " + myvals[low] +
                      " chi = " + myvals[hi]);
 any = true;
 */
 
         o_flags[ir][ic]  = new byte[2*numc]; //- case flags
         n_lines[ir][ic]  = 0;  //- number of contour line segments
         ctrLow[ir][ic]   = (short)hi;
 
         for (il=0; il<numc; il++) {
 
           gg = myvals[low+il];
 
 
           // WLH 21 April 2000
           // if (numv+8 >= maxsize || nump+4 >= 2*maxsize) {
           if (numv+8 >= maxsize) {
             // allocate more space
             maxsize = 2 * maxsize;
 /* WLH 21 April 2000
             int[] tt = ipnt;
             ipnt = new int[2 * maxsize];
             System.arraycopy(tt, 0, ipnt, 0, nump);
 */
             float[] tx = vx;
             float[] ty = vy;
             vx = new float[maxsize];
             vy = new float[maxsize];
             System.arraycopy(tx, 0, vx, 0, numv);
             System.arraycopy(ty, 0, vy, 0, numv);
             if (naux > 0) {
               byte[][] ta = auxLevels;
               auxLevels = new byte[naux][maxsize];
               for (int i=0; i<naux; i++) {
                 System.arraycopy(ta[i], 0, auxLevels[i], 0, numv);
               }
             }
           }
 
 
 
           float gba, gca, gdb, gdc;
           int ii;
 
           // make sure gg is within contouring limits
           if (gg < gn) continue;
           if (gg > gx) break;
           if (gg < lowlimit) continue;
           if (gg > highlimit) break;
 
           // compute orientation of lines inside box
           ii = 0;
           if (gg > ga) ii = 1;
           if (gg > gb) ii += 2;
           if (gg > gc) ii += 4;
           if (gg > gd) ii += 8;
           if (ii > 7) ii = 15 - ii;
           if (ii <= 0) continue;
 
           if ((low+il) < ctrLow[ir][ic]) ctrLow[ir][ic] = (short)(low+il);
 
           // DO LABEL HERE
           if (( mark[ (ic) * nr + (ir) ] )==0) {
             int kc, kr, mc, mr, jc, jr;
             float xk, yk, xm, ym, value;
 
             // Insert a label
 
             // BOX TO AVOID
             kc = ic-lc2-lcc;
             kr = ir-lr2-lrr;
             mc = kc+2*lcc+lc-1;
             mr = kr+2*lrr+lr-1;
             // OK here
             for (jc=kc;jc<=mc;jc++) {
               if (jc >= 0 && jc < nc) {
                 for (jr=kr;jr<=mr;jr++) {
                   if (jr >= 0 && jr < nr) {
                     if (( mark[ (jc) * nr + (jr) ] ) != 2) {
                       mark[ (jc) * nr + (jr) ] = 1;
                     }
                   }
                 }
               }
             }
 
             // BOX TO HOLD LABEL
             kc = ic-lc2;
             kr = ir-lr2;
             mc = kc+lc-1;
             mr = kr+lr-1;
             for (jc=kc;jc<=mc;jc++) {
               if (jc >= 0 && jc < nc) {
                 for (jr=kr;jr<=mr;jr++) {
                   if (jr >= 0 && jr < nr) {
                     mark[ (jc) * nr + (jr) ] = 2;
                   }
                 }
               }
             }
 
             xk = xdd*kr+0.0f;
             yk = ydd*kc+0.0f;
             xm = xdd*(mr+1.0f)+0.0f;
             ym = ydd*(mc+1.0f)+0.0f;
             value = gg;
 
            if (numv4[0]+1000 >= maxv4) {
               // allocate more space
              maxv4 = 2 * (numv4[0]+1000);
               float[][] tx = new float[][] {vx4[0]};
               float[][] ty = new float[][] {vy4[0]};
               vx4[0] = new float[maxv4];
               vy4[0] = new float[maxv4];
               System.arraycopy(tx[0], 0, vx4[0], 0, numv4[0]);
               System.arraycopy(ty[0], 0, vy4[0], 0, numv4[0]);
             }
 
            if (numv3[0]+1000 >= maxv3) {
               // allocate more space
              maxv3 = 2 * (numv3[0]+1000);
               float[][] tx = new float[][] {vx3[0]};
               float[][] ty = new float[][] {vy3[0]};
               vx3[0] = new float[maxv3];
               vy3[0] = new float[maxv3];
               System.arraycopy(tx[0], 0, vx3[0], 0, numv3[0]);
               System.arraycopy(ty[0], 0, vy3[0], 0, numv3[0]);
               if (naux > 0) {
                 byte[][] ta = auxLevels3;
                 for (int i=0; i<naux; i++) {
                   byte[] taa = auxLevels3[i];
                   auxLevels3[i] = new byte[maxv3];
                   System.arraycopy(taa, 0, auxLevels3[i], 0, numv3[0]);
                 }
               }
             }
 
             plot.plotdigits( value, xk, yk, xm, ym, maxsize, swap);
             System.arraycopy(plot.Vx, 0, vx3[0], numv3[0], plot.NumVerts);
             System.arraycopy(plot.Vy, 0, vy3[0], numv3[0], plot.NumVerts);
             if (naux > 0) {
               for (int i=0; i<naux; i++) {
                 for (int j=numv3[0]; j<numv3[0]+plot.NumVerts; j++) {
                   auxLevels3[i][j] = auxa[i];
                 }
               }
             }
             numv3[0] += plot.NumVerts;
             System.arraycopy(plot.VxB, 0, vx4[0], numv4[0], plot.NumVerts);
             System.arraycopy(plot.VyB, 0, vy4[0], numv4[0], plot.NumVerts);
             numv4[0] += plot.NumVerts;
           }
 
           switch (ii) {
             case 1:
               gba = gb-ga;
               gca = gc-ga;
 
               if (naux > 0) {
                 float ratioba = (gg-ga)/gba;
                 float ratioca = (gg-ga)/gca;
                 for (int i=0; i<naux; i++) {
                   t = (int) ( (1.0f - ratioba) * ((auxa[i] < 0) ?
                         ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                       ratioba * ((auxb[i] < 0) ?
                         ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) );
                   auxLevels[i][numv] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
                   t = (int) ( (1.0f - ratioca) * ((auxa[i] < 0) ?
                         ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                       ratioca * ((auxc[i] < 0) ?
                         ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) );
                   auxLevels[i][numv+1] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
 /* MEM_WLH
                   auxLevels[i][numv] = auxa[i] + (auxb[i]-auxa[i]) * ratioba;
                   auxLevels[i][numv+1] = auxa[i] + (auxc[i]-auxa[i]) * ratioca;
 */
                 }
               }
 
               if (( (gba) < 0 ? -(gba) : (gba) ) < 0.0000001) {
                 vx[numv] = xx;
               }
               else {
                 vx[numv] = xx+xd*(gg-ga)/gba;
               }
               vy[numv] = yy;
               numv++;
               if (( (gca) < 0 ? -(gca) : (gca) ) < 0.0000001) {
                  vy[numv] = yy;
               }
               else {
                 vy[numv] = yy+yd*(gg-ga)/gca;
               }
               vx[numv] = xx;
               numv++;
               o_flags[ir][ic][n_lines[ir][ic]] = (byte)ii;
               n_lines[ir][ic]++;
               if (vx[numv-2]==vx[numv-1] || vy[numv-2]==vy[numv-1]) {
                 vx[numv-2] += 0.00001f;
                 vy[numv-1] += 0.00001f;
               }
               break;
 
             case 2:
               gba = gb-ga;
               gdb = gd-gb;
 
               if (naux > 0) {
                 float ratioba = (gg-ga)/gba;
                 float ratiodb = (gg-gb)/gdb;
                 for (int i=0; i<naux; i++) {
                   t = (int) ( (1.0f - ratioba) * ((auxa[i] < 0) ?
                         ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                       ratioba * ((auxb[i] < 0) ?
                         ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) );
                   auxLevels[i][numv] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
                   t = (int) ( (1.0f - ratiodb) * ((auxb[i] < 0) ?
                         ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) +
                       ratiodb * ((auxd[i] < 0) ?
                         ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                   auxLevels[i][numv+1] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
 /* MEM_WLH
                   auxLevels[i][numv] = auxa[i] + (auxb[i]-auxa[i]) * ratioba;
                   auxLevels[i][numv+1] = auxb[i] + (auxd[i]-auxb[i]) * ratiodb;
 */
                 }
               }
 
               if (( (gba) < 0 ? -(gba) : (gba) ) < 0.0000001)
                 vx[numv] = xx;
               else
                 vx[numv] = xx+xd*(gg-ga)/gba;
               vy[numv] = yy;
               numv++;
               if (( (gdb) < 0 ? -(gdb) : (gdb) ) < 0.0000001)
                 vy[numv] = yy;
               else
                 vy[numv] = yy+yd*(gg-gb)/gdb;
               vx[numv] = xx+xd;
               numv++;
               o_flags[ir][ic][n_lines[ir][ic]] = (byte)ii;
               n_lines[ir][ic]++;
               if (vx[numv-2]==vx[numv-1] || vy[numv-2]==vy[numv-1]) {
                 vx[numv-2] -= 0.00001f;
                 vy[numv-1] += 0.00001f;
               }
               break;
 
             case 3:
               gca = gc-ga;
               gdb = gd-gb;
 
               if (naux > 0) {
                 float ratioca = (gg-ga)/gca;
                 float ratiodb = (gg-gb)/gdb;
                 for (int i=0; i<naux; i++) {
                   t = (int) ( (1.0f - ratioca) * ((auxa[i] < 0) ?
                         ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                       ratioca * ((auxc[i] < 0) ?
                         ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) );
                   auxLevels[i][numv] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
                   t = (int) ( (1.0f - ratiodb) * ((auxb[i] < 0) ?
                         ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) +
                       ratiodb * ((auxd[i] < 0) ?
                         ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                   auxLevels[i][numv+1] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
 /* MEM_WLH
                   auxLevels[i][numv] = auxa[i] + (auxc[i]-auxa[i]) * ratioca;
                   auxLevels[i][numv+1] = auxb[i] + (auxd[i]-auxb[i]) * ratiodb;
 */
                 }
               }
 
               if (( (gca) < 0 ? -(gca) : (gca) ) < 0.0000001)
                 vy[numv] = yy;
               else
                 vy[numv] = yy+yd*(gg-ga)/gca;
               vx[numv] = xx;
               numv++;
               if (( (gdb) < 0 ? -(gdb) : (gdb) ) < 0.0000001)
                 vy[numv] = yy;
               else
                 vy[numv] = yy+yd*(gg-gb)/gdb;
               vx[numv] = xx+xd;
               numv++;
               o_flags[ir][ic][n_lines[ir][ic]] = (byte)ii;
               n_lines[ir][ic]++;
               break;
 
             case 4:
               gca = gc-ga;
               gdc = gd-gc;
 
               if (naux > 0) {
                 float ratioca = (gg-ga)/gca;
                 float ratiodc = (gg-gc)/gdc;
                 for (int i=0; i<naux; i++) {
                   t = (int) ( (1.0f - ratioca) * ((auxa[i] < 0) ?
                         ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                       ratioca * ((auxc[i] < 0) ?
                         ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) );
                   auxLevels[i][numv] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
                   t = (int) ( (1.0f - ratiodc) * ((auxc[i] < 0) ?
                         ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) +
                       ratiodc * ((auxd[i] < 0) ?
                         ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                   auxLevels[i][numv+1] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
 /* MEM_WLH
                   auxLevels[i][numv] = auxa[i] + (auxc[i]-auxa[i]) * ratioca;
                   auxLevels[i][numv+1] = auxc[i] + (auxd[i]-auxc[i]) * ratiodc;
 */
                 }
               }
 
               if (( (gca) < 0 ? -(gca) : (gca) ) < 0.0000001)
                 vy[numv] = yy;
               else
                 vy[numv] = yy+yd*(gg-ga)/gca;
               vx[numv] = xx;
               numv++;
               if (( (gdc) < 0 ? -(gdc) : (gdc) ) < 0.0000001)
                 vx[numv] = xx;
               else
                 vx[numv] = xx+xd*(gg-gc)/gdc;
               vy[numv] = yy+yd;
               numv++;
               o_flags[ir][ic][n_lines[ir][ic]] = (byte)ii;
               n_lines[ir][ic]++;
               if (vx[numv-2]==vx[numv-1] || vy[numv-2]==vy[numv-1]) {
                 vx[numv-1] += 0.00001f;
                 vy[numv-2] -= 0.00001f;
               }
               break;
 
             case 5:
               gba = gb-ga;
               gdc = gd-gc;
 
               if (naux > 0) {
                 float ratioba = (gg-ga)/gba;
                 float ratiodc = (gg-gc)/gdc;
                 for (int i=0; i<naux; i++) {
                   t = (int) ( (1.0f - ratioba) * ((auxa[i] < 0) ?
                         ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                       ratioba * ((auxb[i] < 0) ?
                         ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) );
                   auxLevels[i][numv] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
                   t = (int) ( (1.0f - ratiodc) * ((auxc[i] < 0) ?
                         ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) +
                       ratiodc * ((auxd[i] < 0) ?
                         ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                   auxLevels[i][numv+1] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
 /* MEM_WLH
                   auxLevels[i][numv] = auxa[i] + (auxb[i]-auxa[i]) * ratioba;
                   auxLevels[i][numv+1] = auxc[i] + (auxd[i]-auxc[i]) * ratiodc;
 */
                 }
               }
 
               if (( (gba) < 0 ? -(gba) : (gba) ) < 0.0000001)
                 vx[numv] = xx;
               else
                 vx[numv] = xx+xd*(gg-ga)/gba;
               vy[numv] = yy;
               numv++;
               if (( (gdc) < 0 ? -(gdc) : (gdc) ) < 0.0000001)
                 vx[numv] = xx;
               else
                 vx[numv] = xx+xd*(gg-gc)/gdc;
               vy[numv] = yy+yd;
               numv++;
               o_flags[ir][ic][n_lines[ir][ic]] = (byte)ii;
               n_lines[ir][ic]++;
               break;
 
             case 6:
               gba = gb-ga;
               gdc = gd-gc;
               gca = gc-ga;
               gdb = gd-gb;
 
               if (naux > 0) {
                 float ratioba = (gg-ga)/gba;
                 float ratiodc = (gg-gc)/gdc;
                 float ratioca = (gg-ga)/gca;
                 float ratiodb = (gg-gb)/gdb;
                 for (int i=0; i<naux; i++) {
                   t = (int) ( (1.0f - ratioba) * ((auxa[i] < 0) ?
                         ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                       ratioba * ((auxb[i] < 0) ?
                         ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) );
                   auxLevels[i][numv] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
 /* MEM_WLH
                   auxLevels[i][numv] = auxa[i] + (auxb[i]-auxa[i]) * ratioba;
 */
                   if ( (gg>gv) ^ (ga<gb) ) {
                     t = (int) ( (1.0f - ratioca) * ((auxa[i] < 0) ?
                           ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                         ratioca * ((auxc[i] < 0) ?
                           ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) );
                     auxLevels[i][numv+1] = (byte)
                       ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256)));
                     t = (int) ( (1.0f - ratiodb) * ((auxb[i] < 0) ?
                           ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) +
                         ratiodb * ((auxd[i] < 0) ?
                           ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                     auxLevels[i][numv+2] = (byte)
                       ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256)));
 /* MEM_WLH
                     auxLevels[i][numv+1] = auxa[i] + (auxc[i]-auxa[i]) * ratioca;
                     auxLevels[i][numv+2] = auxb[i] + (auxd[i]-auxb[i]) * ratiodb;
 */
                   }
                   else {
                     t = (int) ( (1.0f - ratiodb) * ((auxb[i] < 0) ?
                           ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) +
                         ratiodb * ((auxd[i] < 0) ?
                           ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                     auxLevels[i][numv+1] = (byte)
                       ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256)));
                     t = (int) ( (1.0f - ratioca) * ((auxa[i] < 0) ?
                           ((float) auxa[i]) + 256.0f : ((float) auxa[i]) ) +
                         ratioca * ((auxc[i] < 0) ?
                           ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) );
                     auxLevels[i][numv+2] = (byte)
                       ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256)));
 /* MEM_WLH
                     auxLevels[i][numv+1] = auxb[i] + (auxd[i]-auxb[i]) * ratiodb;
                     auxLevels[i][numv+2] = auxa[i] + (auxc[i]-auxa[i]) * ratioca;
 */
                   }
                   t = (int) ( (1.0f - ratiodc) * ((auxc[i] < 0) ?
                         ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) +
                       ratiodc * ((auxd[i] < 0) ?
                         ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                   auxLevels[i][numv+3] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
 /* MEM_WLH
                   auxLevels[i][numv+3] = auxc[i] + (auxd[i]-auxc[i]) * ratiodc;
 */
                 }
               }
 
               if (( (gba) < 0 ? -(gba) : (gba) ) < 0.0000001)
                 vx[numv] = xx;
               else
                 vx[numv] = xx+xd*(gg-ga)/gba;
               vy[numv] = yy;
               numv++;
               // here's a brain teaser
               if ( (gg>gv) ^ (ga<gb) ) {  // (XOR)
                 if (( (gca) < 0 ? -(gca) : (gca) ) < 0.0000001)
                   vy[numv] = yy;
                 else
                   vy[numv] = yy+yd*(gg-ga)/gca;
                 vx[numv] = xx;
                 numv++;
                 o_flags[ir][ic][n_lines[ir][ic]] = (byte)1 + (byte)32;
                 n_lines[ir][ic]++;
                 if (( (gdb) < 0 ? -(gdb) : (gdb) ) < 0.0000001)
                   vy[numv] = yy;
                 else
                   vy[numv] = yy+yd*(gg-gb)/gdb;
                 vx[numv] = xx+xd;
                 o_flags[ir][ic][n_lines[ir][ic]] = (byte)7 + (byte)32;
                 n_lines[ir][ic]++;
                 numv++;
               }
               else {
                 if (( (gdb) < 0 ? -(gdb) : (gdb) ) < 0.0000001)
                   vy[numv] = yy;
                 else
                   vy[numv] = yy+yd*(gg-gb)/gdb;
                 vx[numv] = xx+xd;
                 numv++;
                 o_flags[ir][ic][n_lines[ir][ic]] = (byte)2 + (byte)32;
                 n_lines[ir][ic]++;
                 if (( (gca) < 0 ? -(gca) : (gca) ) < 0.0000001)
                   vy[numv] = yy;
                 else
                   vy[numv] = yy+yd*(gg-ga)/gca;
                 vx[numv] = xx;
                 numv++;
                 o_flags[ir][ic][n_lines[ir][ic]] = (byte)4 + (byte)32;
                 n_lines[ir][ic]++;
               }
               if (( (gdc) < 0 ? -(gdc) : (gdc) ) < 0.0000001)
                 vx[numv] = xx;
               else
                 vx[numv] = xx+xd*(gg-gc)/gdc;
               vy[numv] = yy+yd;
               numv++;
               break;
 
             case 7:
               gdb = gd-gb;
               gdc = gd-gc;
 
               if (naux > 0) {
                 float ratiodb = (gg-gb)/gdb;
                 float ratiodc = (gg-gc)/gdc;
                 for (int i=0; i<naux; i++) {
                   t = (int) ( (1.0f - ratiodb) * ((auxb[i] < 0) ?
                         ((float) auxb[i]) + 256.0f : ((float) auxb[i]) ) +
                       ratiodb * ((auxd[i] < 0) ?
                         ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                   auxLevels[i][numv] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
                   t = (int) ( (1.0f - ratiodc) * ((auxc[i] < 0) ?
                         ((float) auxc[i]) + 256.0f : ((float) auxc[i]) ) +
                       ratiodc * ((auxd[i] < 0) ?
                         ((float) auxd[i]) + 256.0f : ((float) auxd[i]) ) );
                   auxLevels[i][numv+1] = (byte)
                     ( (t < 0) ? 0 : ((t > 255) ? -1 : ((t < 128) ? t : t - 256) ) );
 /* MEM_WLH
                   auxLevels[i][numv] = auxb[i] + (auxb[i]-auxb[i]) * ratiodb;
                   auxLevels[i][numv+1] = auxc[i] + (auxd[i]-auxc[i]) * ratiodc;
 */
                 }
               }
 
               if (( (gdb) < 0 ? -(gdb) : (gdb) ) < 0.0000001)
                 vy[numv] = yy;
               else
                 vy[numv] = yy+yd*(gg-gb)/gdb;
               vx[numv] = xx+xd;
               numv++;
               if (( (gdc) < 0 ? -(gdc) : (gdc) ) < 0.0000001)
                 vx[numv] = xx;
               else
                 vx[numv] = xx+xd*(gg-gc)/gdc;
               vy[numv] = yy+yd;
               numv++;
               o_flags[ir][ic][n_lines[ir][ic]] = (byte)ii;
               n_lines[ir][ic]++;
               if (vx[numv-2]==vx[numv-1] || vy[numv-2]==vy[numv-1]) {
                 vx[numv-1] -= 0.00001f;
                 vy[numv-2] -= 0.00001f;
               }
               break;
           } // switch
 
 
           // If contour level is negative, make dashed line
           if (gg < base && dash) {           /* DRM: 1999-05-19 */
             float vxa, vya, vxb, vyb;
             vxa = vx[numv-2];
             vya = vy[numv-2];
             vxb = vx[numv-1];
             vyb = vy[numv-1];
             vx[numv-2] = (3.0f*vxa+vxb) * 0.25f;
             vy[numv-2] = (3.0f*vya+vyb) * 0.25f;
             vx[numv-1] = (vxa+3.0f*vxb) * 0.25f;
             vy[numv-1] = (vya+3.0f*vyb) * 0.25f;
           }
 /*
 if ((20.0 <= vy[numv-2] && vy[numv-2] < 22.0) ||
     (20.0 <= vy[numv-1] && vy[numv-1] < 22.0)) {
   System.out.println("vy = " + vy[numv-1] + " " + vy[numv-2] +
                      " ic, ir = " + ic + " " + ir);
 }
 */
           if (ii == 6) { //- add last two pairs
             ctrSet.add(vx, vy, numv-4, numv-3, low+il);
             ctrSet.add(vx, vy, numv-2, numv-1, low+il);
           }
           else {
             ctrSet.add(vx, vy, numv-2, numv-1, low+il);
           }
 
         }  // for il       -- NOTE:  gg incremented in for statement
       }  // for ic
     }  // for ir
 
 
 /**-------------------  Color Fill -------------------------*/
     if (fill) {
       fillGridBox(g, n_lines, vx, vy, xd, xdd, yd, ydd, nr, nrm, nc, ncm,
                   ctrLow, tri, tri_color, o_flags, myvals, color_bin,
                   grd_normals, tri_normals);
     }
 
 
 //---TDR, build Contour Strips
 
     float[][][] vvv          = new float[2][][];
     byte[][][] new_colors    = new byte[2][][];
     ctrSet.getLineColorArrays(vx, vy, auxLevels, vvv, new_colors, lbl_vv, lbl_cc, lbl_loc);
 
     vx1[0]   = vvv[0][0];
     vy1[0]   = vvv[0][1];
     vz1[0]   = vvv[0][2];
     vx2[0]   = vvv[1][0];
     vy2[0]   = vvv[1][1];
     vz2[0]   = vvv[1][2];
     numv1[0] = vvv[0][0].length;
     numv2[0] = vvv[1][0].length;
 
     int n_lbls = lbl_vv[0].length;
     if (n_lbls > 0) {
       vx3[0]   = lbl_vv[0][0][0];
       vy3[0]   = lbl_vv[0][0][1];
       vz3[0]   = lbl_vv[0][0][2];
       vx4[0]   = lbl_vv[1][0][0];
       vy4[0]   = lbl_vv[1][0][1];
       vz4[0]   = lbl_vv[1][0][2];
       numv3[0] = lbl_vv[0][0][0].length;
       numv4[0] = lbl_vv[1][0][0].length;
     }
 
     if (auxLevels != null) {
       int clr_dim = auxValues.length;
       auxLevels1[0] = new_colors[0][0];
       auxLevels1[1] = new_colors[0][1];
       auxLevels1[2] = new_colors[0][2];
       if (clr_dim == 4) auxLevels1[3] = new_colors[0][3];
       auxLevels2[0] = new_colors[1][0];
       auxLevels2[1] = new_colors[1][1];
       auxLevels2[2] = new_colors[1][2];
       if (clr_dim == 4) auxLevels2[3] = new_colors[1][3];
       if (n_lbls > 0) {
         auxLevels3[0] = lbl_cc[0][0][0];
         auxLevels3[1] = lbl_cc[0][0][1];
         auxLevels3[2] = lbl_cc[0][0][2];
         if (clr_dim == 4) auxLevels3[3] = lbl_cc[0][0][3];
       }
     }
   }
 
   public static void fillGridBox(float[] g, short[][] n_lines,
                                  float[] vx, float[] vy,
                                  float xd, float xdd, float yd, float ydd,
                                  int nr, int nrm, int nc, int ncm,
                                  short[][] ctrLow,
                                  float[][] tri, byte[][] tri_color,
                                  byte[][][] o_flags, float[] values,
                                  byte[][] color_bin,
                                  float[][][] grd_normals, float[][] tri_normals)
   {
     float xx, yy;
     int[] numv = new int[1];
     numv[0] = 0;
     
     int n_tri = 0;
     for (int ir=0; ir<nrm; ir++) {
       for (int ic=0; ic<ncm; ic++) {
         if (n_lines[ir][ic] == 0) {
           n_tri +=2;
         }
         else {
           n_tri += (4 + (n_lines[ir][ic]-1)*2);
         }
         boolean any = false;
         if (o_flags[ir][ic] != null) {
           for (int k=0; k<o_flags[ir][ic].length; k++) {
             if (o_flags[ir][ic][k] > 32) any = true;
           }
           if (any) n_tri += 4;
         }
       }
     }
     tri[0] = new float[n_tri*3];
     tri[1] = new float[n_tri*3];
     for (int kk=0; kk<color_bin.length; kk++) {
       tri_color[kk] = new byte[n_tri*3];
     }
     tri_normals[0]  = new float[3*n_tri*3];
 
     int[] t_idx = new int[1];
     t_idx[0] = 0;
     int[] n_idx = new int[1];
     n_idx[0] = 0;
 
     for (int ir=0; ir<nrm; ir++) {
       xx = xdd*ir+0.0f;
       for (int ic=0; ic<ncm; ic++) {
         float ga, gb, gc, gd;
         yy = ydd*ic+0.0f;
 
         // get 4 corner values, skip box if any are missing
         ga = ( g[ (ic) * nr + (ir) ] );
         // test for missing
         if (ga != ga) continue;
         gb = ( g[ (ic) * nr + (ir+1) ] );
         // test for missing
         if (gb != gb) continue;
         gc = ( g[ (ic+1) * nr + (ir) ] );
         // test for missing
         if (gc != gc) continue;
         gd = ( g[ (ic+1) * nr + (ir+1) ] );
         // test for missing
         if (gd != gd) continue;
 
         numv[0] += n_lines[ir][ic]*2;
 
         fillGridBox(new float[] {ga, gb, gc, gd}, n_lines[ir][ic], vx, vy,
                     xx, yy, xd, yd, ic, ir, ctrLow[ir][ic],
                     tri, t_idx, tri_color, numv[0], o_flags[ir][ic],
                     values, color_bin, grd_normals, n_idx, tri_normals);
       }
     }
   }
          
 
   public static void fillGridBox(float[] corners,
                                  int numc, float[] vx, float[] vy,
                                  float xx, float yy, float xd, float yd,
                                  int nc, int nr, short ctrLow,
                                  float[][] tri, int[] t_idx, byte[][] tri_color,
                                  int numv, byte[] o_flags,
                                  float[] values, byte[][] color_bin,
                                  float[][][] grd_normals, int[] n_idx, 
                                  float[][] tri_normals_a)
         {
           float[] tri_normals = tri_normals_a[0];
           int n_tri           = 4 + (numc-1)*2;
           int[] cnt_tri       = new int[1];
           cnt_tri[0]          = 0;
           int il              = 0;
           int color_length    = color_bin.length;
           float[] vec         = new float[2];
           float[] vec_last    = new float[2];
           float[] vv1         = new float[2];
           float[] vv2         = new float[2];
           float[] vv1_last    = new float[2];
           float[] vv2_last    = new float[2];
           float[][] vv        = new float[2][2];
           float[][] vv_last   = new float[2][2];
           float[] vv3         = new float[2];
 
           int dir             = 1;
           int start           = numv-2;
           int o_start         = numc-1;
           int x_min_idx       = 0;
           int o_idx           = 0;
           byte o_flag         = o_flags[o_idx];
           int ydir            = 1;
           boolean special     = false;
           int[] closed        = {0};
           boolean up;
           boolean right;
           float dist_sqrd     = 0;
 
           int v_idx           = start + dir*il*2;
 
           //-- color level at corners
           //------------------------------
           byte[][] crnr_color = new byte[4][color_length];
           boolean[] crnr_out  = new boolean[] {true, true, true, true};
           boolean all_out     = true;
           for (int tt = 0; tt < corners.length; tt++) {
             int cc = 0;
             int kk = 0;
             for (kk = 0; kk < (values.length - 1); kk++) {
               if ((corners[tt] >= values[kk]) &&
                   (corners[tt] < values[kk+1])) {
                 cc  = kk;
                 all_out = false;
                 crnr_out[tt] = false;
               }
             }
             for (int ii=0; ii<color_length; ii++) {
               crnr_color[tt][ii] = color_bin[ii][cc];
             }
           }
 
           int tt = t_idx[0]; //- initialize triangle vertex counter
 
 
           dir     = 1;
           start   = numv - numc*2;
           o_start = 0;
           v_idx   = start + dir*il*2;
           up      = false;
           right   = false;
           float[] x_avg = new float[2];
           float[] y_avg = new float[2];
  
           if (numc > 1)
           { //-- first/next ctr line midpoints
             int idx  = v_idx;
             x_avg[0] = (vx[idx] + vx[idx+1])/2;
             y_avg[0] = (vy[idx] + vy[idx+1])/2;
             idx      = v_idx + 2;
             x_avg[1] = (vx[idx] + vx[idx+1])/2;
             y_avg[1] = (vy[idx] + vy[idx+1])/2;
             if ((x_avg[1] - x_avg[0]) > 0) up    = true;
             if ((y_avg[1] - y_avg[0]) > 0) right = true;
           }
           else if ( numc == 1 ) 
           { //- default values for logic below
             x_avg[0] = 0f;
             y_avg[0] = 0f;
             x_avg[1] = 1f;
             y_avg[1] = 1f;
           }
           else if ( numc == 0 ) //- empty grid box (no contour lines)
           {
             if (all_out) return;
             n_tri = 2;
           
             tri_normals[n_idx[0]++] = grd_normals[nc][nr][0];
             tri_normals[n_idx[0]++] = grd_normals[nc][nr][1];
             tri_normals[n_idx[0]++] = grd_normals[nc][nr][2];
             for (int ii=0; ii<color_length; ii++) {
               tri_color[ii][tt] = crnr_color[0][ii];
             }
             tri[0][tt]   = xx;
             tri[1][tt++] = yy;
 
             tri_normals[n_idx[0]++] = grd_normals[nc][nr+1][0];
             tri_normals[n_idx[0]++] = grd_normals[nc][nr+1][1];
             tri_normals[n_idx[0]++] = grd_normals[nc][nr+1][2];
             for (int ii=0; ii<color_length; ii++) {
               tri_color[ii][tt] = crnr_color[0][ii];
             }
             tri[0][tt]   = xx + xd;
             tri[1][tt++] = yy;
 
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][0];
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][1];
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][2];
             for (int ii=0; ii<color_length; ii++) {
               tri_color[ii][tt] = crnr_color[0][ii];
             }
             tri[0][tt]   = xx + xd;
             tri[1][tt++] = yy + yd;
 
             t_idx[0] = tt;
             cnt_tri[0]++;
 
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][0];
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][1];
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][2];
             for (int ii=0; ii<color_length; ii++) {
               tri_color[ii][tt] = crnr_color[0][ii];
             }
             tri[0][tt]   = xx + xd;
             tri[1][tt++] = yy + yd;
 
             tri_normals[n_idx[0]++] = grd_normals[nc][nr][0];
             tri_normals[n_idx[0]++] = grd_normals[nc][nr][1];
             tri_normals[n_idx[0]++] = grd_normals[nc][nr][2];
             for (int ii=0; ii<color_length; ii++) {
               tri_color[ii][tt] = crnr_color[0][ii];
             }
             tri[0][tt]   = xx;
             tri[1][tt++] = yy;
 
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr][0];
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr][1];
             tri_normals[n_idx[0]++] = grd_normals[nc+1][nr][2];
             for (int ii=0; ii<color_length; ii++) {
               tri_color[ii][tt] = crnr_color[0][ii];
             }
             tri[0][tt]   = xx;
             tri[1][tt++] = yy + yd;
             t_idx[0] = tt;
 
             cnt_tri[0]++;
             return;
           }//-- end no contour lines
 
 
           //--If any case 6 (saddle point), handle with special logic
           for (int iii=0; iii<o_flags.length; iii++) {
             if (o_flags[iii] > 32) {
               fillCaseSix(xx, yy, xd, yd, v_idx, dir, o_flags, ctrLow,
                           vx, vy, nc, nr, crnr_color, crnr_out,
                           tri, t_idx, color_bin, tri_color, color_length,
                           grd_normals, n_idx, tri_normals, closed, cnt_tri);
               return;
             }
           }
 
           //-- start making triangles for color fill
           //---------------------------------------------
 
           if (o_flag == 1 || o_flag == 4 || o_flag == 2 || o_flag == 7) {
             boolean opp = false;
             float dy = 0;
             float dx = 0;
             float dist_0 = 0;
             float dist_1 = 0;
 
             /**  compare midpoints distances for first/next
                  contour lines
             -------------------------------------------------*/
             if (o_flag == 1)  {
               dy = (y_avg[1] - (yy));
               dx = (x_avg[1] - (xx));
               dist_1 = dy*dy + dx*dx;
               dy = (y_avg[0] - (yy));
               dx = (x_avg[0] - (xx));
               dist_0 = dy*dy + dx*dx;
             }
             if (o_flag == 2) {
               dy = (y_avg[1] - (yy));
               dx = (x_avg[1] - (xx + xd));
               dist_1 = dy*dy + dx*dx;
               dy = (y_avg[0] - (yy));
               dx = (x_avg[0] - (xx + xd));
               dist_0 = dy*dy + dx*dx;
             }
             if (o_flag == 4) {
               dy = (y_avg[1] - (yy + yd));
               dx = (x_avg[1] - (xx));
               dist_1 = dy*dy + dx*dx;
               dy = (y_avg[0] - (yy + yd));
               dx = (x_avg[0] - (xx));
               dist_0 = dy*dy + dx*dx;
             }
             if (o_flag == 7) {
               dy = (y_avg[1] - (yy + yd));
               dx = (x_avg[1] - (xx + xd));
               dist_1 = dy*dy + dx*dx;
               dy = (y_avg[0] - (yy + yd));
               dx = (x_avg[0] - (xx + xd));
               dist_0 = dy*dy + dx*dx;
             }
             if (dist_1 < dist_0) opp = true;
             if (opp) {
               fillToOppCorner(xx, yy, xd, yd, v_idx, o_flag, cnt_tri, dir,
                               vx, vy, nc, nr, crnr_color, crnr_out,
                               tri, t_idx, tri_color,
                               grd_normals, n_idx, tri_normals, closed);
             }
             else {
               fillToNearCorner(xx, yy, xd, yd, v_idx, o_flag, cnt_tri, dir,
                                vx, vy, nc, nr, crnr_color, crnr_out,
                                tri, t_idx, tri_color,
                                grd_normals, n_idx, tri_normals, closed);
             }
           }
           else if (o_flags[o_idx] == 3)
           {
             int flag = 1;
             if (right) flag = -1;
             fillToSide(xx, yy, xd, yd, v_idx, o_flag, flag, cnt_tri, dir,
                        vx, vy, nc, nr, crnr_color, crnr_out,
                        tri, t_idx, tri_color,
                        grd_normals, n_idx, tri_normals, closed);
           }
           else if (o_flags[o_idx] == 5)
           {
             int flag = 1;
             if (!up) flag = -1;
             fillToSide(xx, yy, xd, yd, v_idx, o_flag, flag, cnt_tri, dir,
                        vx, vy, nc, nr, crnr_color, crnr_out,
                        tri, t_idx, tri_color,
                        grd_normals, n_idx, tri_normals, closed);
           }
 
 
           byte last_o  = o_flags[o_idx];
           int cc_start = (dir > 0) ? (ctrLow-1) : (ctrLow+(numc-1));
 
           //- move to next contour line
           //--------------------------------
           il++;
           for ( il = 1; il < numc; il++ )
           {
             v_idx          = start + dir*il*2;
             o_idx          = o_start + dir*il;
             int v_idx_last = v_idx - 2*dir;
             int cc         = cc_start + dir*il;
 
             if (o_flags[o_idx] != last_o) //- contour line case change
             {
               byte[] side_s      = new byte[2];
               byte[] last_side_s = new byte[2];
               byte[] b_arg       = new byte[2];
               boolean flip;
      
               getBoxSide(vx, vy, xx, xd, yy, yd, v_idx, dir, o_flags[o_idx], b_arg);
               side_s[0] = b_arg[0];
               side_s[1] = b_arg[1];
               getBoxSide(vx, vy, xx, xd, yy, yd, v_idx_last, dir, last_o, b_arg);
               last_side_s[0] = b_arg[0];
               last_side_s[1] = b_arg[1];
 
               if (side_s[0] == last_side_s[0]) {
                 flip = false;
               }
               else if (side_s[0] == last_side_s[1]) {
                 flip = true;
               }
               else if (side_s[1] == last_side_s[0]) {
                 flip = true;
               }
               else if (side_s[1] == last_side_s[1]) {
                 flip = false;
               }
               else {
                 if(((side_s[0]+last_side_s[0]) & 1) == 1) {
                   flip = false;
                 }
                 else {
                   flip = true;
                 }
               }
 
               if (!flip) {
                 vv1[0]      = vx[v_idx];
                 vv1[1]      = vy[v_idx];
                 vv2[0]      = vx[v_idx+dir];
                 vv2[1]      = vy[v_idx+dir];
 
                 vv[0][0]    = vx[v_idx];
                 vv[1][0]    = vy[v_idx];
                 vv[0][1]    = vx[v_idx+dir];
                 vv[1][1]    = vy[v_idx+dir];
               }
               else {
                 vv1[0]      = vx[v_idx+dir];
                 vv1[1]      = vy[v_idx+dir];
                 vv2[0]      = vx[v_idx];
                 vv2[1]      = vy[v_idx];
 
                 vv[0][0]    = vx[v_idx+dir];
                 vv[1][0]    = vy[v_idx+dir];
                 vv[0][1]    = vx[v_idx];
                 vv[1][1]    = vy[v_idx];
               }
               vv1_last[0]   = vx[v_idx_last];
               vv1_last[1]   = vy[v_idx_last];
               vv2_last[0]   = vx[v_idx_last+dir];
               vv2_last[1]   = vy[v_idx_last+dir];
 
               vv_last[0][0] = vx[v_idx_last];
               vv_last[1][0] = vy[v_idx_last];
               vv_last[0][1] = vx[v_idx_last+dir];
               vv_last[1][1] = vy[v_idx_last+dir];
 
 
               //--- fill between contour lines
               fillToLast(xx, yy, xd, yd, nc, nr, vv1, vv2, vv1_last, vv2_last,
                          tri, cnt_tri, t_idx, tri_color, color_bin,
                          cc, color_length, grd_normals, n_idx, tri_normals);
 
               byte[] b_arg2 = new byte[2];
               getBoxSide(vv[0], vv[1], xx, xd, yy, yd, 0, dir, o_flags[o_idx], b_arg);
               getBoxSide(vv_last[0], vv_last[1], xx, xd, yy, yd, 0, dir, last_o, b_arg2);
               
 
               for (int kk = 0; kk < 2; kk++) { //- close off corners
                 float[] vvx = new float[2];
                 float[] vvy = new float[2];
                 byte side   = 0;
                 byte last_s = 0;
 
                 side = b_arg[kk];
                 last_s = b_arg2[kk];
 
                 if ( side != last_s ) {
                   if ((side == 0 && last_s == 3) ||
                       (side == 3 && last_s == 0))
                   {  //- case 1
                     fillToNearCorner(xx, yy, xd, yd, 0, (byte)1, cnt_tri, 1,
                       new float[] {vv[0][kk], vv_last[0][kk]},
                       new float[] {vv[1][kk], vv_last[1][kk]}, nc, nr,
                       crnr_color, crnr_out, tri, t_idx, tri_color,
                       grd_normals, n_idx, tri_normals, closed);
                   }
                   if ((side == 0 && last_s == 1) ||
                       (side == 1 && last_s == 0))
                   {  //- case 2
                     fillToNearCorner(xx, yy, xd, yd, 0, (byte)2, cnt_tri, 1,
                       new float[] {vv[0][kk], vv_last[0][kk]},
                       new float[] {vv[1][kk], vv_last[1][kk]}, nc, nr,
                       crnr_color, crnr_out, tri, t_idx, tri_color,
                       grd_normals, n_idx, tri_normals, closed);
                   }
                   if ((side == 2 && last_s == 3) ||
                       (side == 3 && last_s == 2))
                   {  //- case 4
                     fillToNearCorner(xx, yy, xd, yd, 0, (byte)4, cnt_tri, 1,
                       new float[] {vv[0][kk], vv_last[0][kk]},
                       new float[] {vv[1][kk], vv_last[1][kk]}, nc, nr,
                       crnr_color, crnr_out, tri, t_idx, tri_color,
                       grd_normals, n_idx, tri_normals, closed);
                   }
                   if ((side == 2 && last_s == 1) ||
                       (side == 1 && last_s == 2))
                   {  //- case 7
                     fillToNearCorner(xx, yy, xd, yd, 0, (byte)7, cnt_tri, 1,
                       new float[] {vv[0][kk], vv_last[0][kk]},
                       new float[] {vv[1][kk], vv_last[1][kk]}, nc, nr,
                       crnr_color, crnr_out, tri, t_idx, tri_color,
                       grd_normals, n_idx, tri_normals, closed);
                   }
                   if ((side == 2 && last_s == 0) ||
                       (side == 0 && last_s == 2))
                   {  //- case 5
                     if (side == 0) {
                       vvx[0] = vv[0][kk];
                       vvy[0] = vv[1][kk];
                       vvx[1] = vv_last[0][kk];
                       vvy[1] = vv_last[1][kk];
                     }
                     else {
                       vvx[0] = vv_last[0][kk];
                       vvy[0] = vv_last[1][kk];
                       vvx[1] = vv[0][kk];
                       vvy[1] = vv[1][kk];
                     }
                     int flag = -1;
                     if (((closed[0] & 4)==0)&&((closed[0] & 1)==0)) flag = 1;
                     fillToSide(xx, yy, xd, yd, 0, (byte)5, flag, cnt_tri, 1,
                        vvx, vvy, nc, nr,
                        crnr_color, crnr_out, tri, t_idx, tri_color,
                        grd_normals, n_idx, tri_normals, closed);
                   }
                   if ((side == 1 && last_s == 3) ||
                       (side == 3 && last_s == 1))
                   {  //- case 3
                     if (side == 3) {
                       vvx[0] = vv[0][kk];
                       vvy[0] = vv[1][kk];
                       vvx[1] = vv_last[0][kk];
                       vvy[1] = vv_last[1][kk];
                     }
                     else {
                       vvx[0] = vv_last[0][kk];
                       vvy[0] = vv_last[1][kk];
                       vvx[1] = vv[0][kk];
                       vvy[1] = vv[1][kk];
                     }
                     int flag = -1;
                     if (((closed[0] & 4)==0)&&((closed[0] & 8)==0)) flag = 1;
                     fillToSide(xx, yy, xd, yd, 0, (byte)3, flag, cnt_tri, 1,
                        vvx, vvy, nc, nr,
                        crnr_color, crnr_out, tri, t_idx, tri_color,
                        grd_normals, n_idx, tri_normals, closed);
                   }
                 }
               }
             }
             else {
               vv1[0] = vx[v_idx];
               vv1[1] = vy[v_idx];
               vv2[0] = vx[v_idx+dir];
               vv2[1] = vy[v_idx+dir];
               vv1_last[0] = vx[v_idx_last];
               vv1_last[1] = vy[v_idx_last];
               vv2_last[0] = vx[v_idx_last+dir];
               vv2_last[1] = vy[v_idx_last+dir];
 
               fillToLast(xx, yy, xd, yd, nc, nr, vv1, vv2, vv1_last, vv2_last,
                          tri, cnt_tri, t_idx, tri_color, color_bin,
                          cc, color_length, grd_normals, n_idx, tri_normals);
             }
             last_o = o_flags[o_idx];
           }//---- contour loop
   
           /*- last or first/last contour line
           ------------------------------------*/
           int flag_set = 0;
           if ((last_o == 1)||(last_o == 2)||(last_o == 4)||(last_o == 7))
           {
             if(last_o == 1) flag_set = (closed[0] & 1);
             if(last_o == 2) flag_set = (closed[0] & 2);
             if(last_o == 4) flag_set = (closed[0] & 4);
             if(last_o == 7) flag_set = (closed[0] & 8);
           
             if (flag_set > 0) {
               fillToOppCorner(xx, yy, xd, yd, v_idx, last_o, cnt_tri, dir,
                               vx, vy, nc, nr, crnr_color, crnr_out,
                               tri, t_idx, tri_color,
                               grd_normals, n_idx, tri_normals, closed);
             }
             else {
               fillToNearCorner(xx, yy, xd, yd, v_idx, last_o, cnt_tri, dir,
                                vx, vy, nc, nr, crnr_color, crnr_out,
                                tri, t_idx, tri_color,
                                grd_normals, n_idx, tri_normals, closed);
             }
           }
           else if (last_o == 3)
           {
             int flag = -1;
             if (closed[0]==3) flag = 1;
             fillToSide(xx, yy, xd, yd, v_idx, last_o, flag, cnt_tri, dir,
                        vx, vy, nc, nr, crnr_color, crnr_out,
                        tri, t_idx, tri_color,
                        grd_normals, n_idx, tri_normals, closed);
           }
           else if (last_o == 5)
           {
             int flag = 1;
             if (closed[0]==5) flag = -1;
             fillToSide(xx, yy, xd, yd, v_idx, last_o, flag, cnt_tri, dir,
                        vx, vy, nc, nr, crnr_color, crnr_out,
                        tri, t_idx, tri_color,
                        grd_normals, n_idx, tri_normals, closed);
           }
 
         }//--- end fillGridBox
 
   private static void getBoxSide(float[] vx, float[] vy,
                                  float xx, float xd, float yy, float yd,
                                  int v_idx, int dir, byte o_flag, byte[] side)
   {
     /*
     if (vy[v_idx] == yy)          side[0]  = 0; // a-b
     if (vy[v_idx] == (yy + yd))   side[0]  = 2; // c-d
     if (vx[v_idx] == xx)          side[0]  = 3; // a-c
     if (vx[v_idx] == (xx + xd))   side[0]  = 1; // b-d
     */
 
     for (int kk = 0; kk < 2; kk++) {
       int ii = v_idx + kk*dir;
       switch (o_flag) {
         case 1:
           side[kk] = 3;
           if (vy[ii] == yy) side[kk] = 0;
           break;
         case 2:
           side[kk] = 1;
           if (vy[ii] == yy) side[kk] = 0;
           break;
         case 4:
           side[kk] = 3;
           if (vy[ii] == (yy + yd)) side[kk] = 2;
           break;
         case 7:
           side[kk] = 1;
           if (vy[ii] == (yy + yd)) side[kk] = 2;
           break;
         case 3:
           side[kk] = 1;
           if (vx[ii] == xx) side[kk] = 3;
           break;
         case 5:
           side[kk] = 0;
           if (vy[ii] == (yy + yd)) side[kk] = 2;
           break;
       }
     }
   }
 
   private static void interpNormals(float vx, float vy, float xx, float yy,
                                     int nc, int nr, float xd, float yd,
                                     float[][][] grd_normals, int[] n_idx,
                                     float[] tri_normals)
 
   {
     int side = -1;
     float[] nn = new float[3];
 
     if (vy == yy)             side   = 0; // a-b
     if (vy == (yy + yd))      side   = 2; // c-d
     if (vx == xx)             side   = 3; // a-c
     if (vx == (xx + xd))      side   = 1; // b-d
 
     float dx = vx - xx;
     float dy = vy - yy;
 
     switch (side)
     { 
       case 0:
         nn[0] = ((grd_normals[nc][nr+1][0] - grd_normals[nc][nr][0])/xd)*dx +
                                              grd_normals[nc][nr][0];
         nn[1] = ((grd_normals[nc][nr+1][1] - grd_normals[nc][nr][1])/xd)*dx +
                                              grd_normals[nc][nr][1];
         nn[2] = ((grd_normals[nc][nr+1][2] - grd_normals[nc][nr][2])/xd)*dx +
                                              grd_normals[nc][nr][2];
         break;
       case 3:
         nn[0] = ((grd_normals[nc+1][nr][0] - grd_normals[nc][nr][0])/yd)*dy +
                                              grd_normals[nc][nr][0];
         nn[1] = ((grd_normals[nc+1][nr][1] - grd_normals[nc][nr][1])/yd)*dy +
                                              grd_normals[nc][nr][1];
         nn[2] = ((grd_normals[nc+1][nr][2] - grd_normals[nc][nr][2])/yd)*dy +
                                              grd_normals[nc][nr][2];
         break;
       case 1:
         nn[0] = ((grd_normals[nc+1][nr+1][0] - grd_normals[nc][nr+1][0])/yd)*dy +
                                                grd_normals[nc][nr+1][0];
         nn[1] = ((grd_normals[nc+1][nr+1][1] - grd_normals[nc][nr+1][1])/yd)*dy +
                                                grd_normals[nc][nr+1][1];
         nn[2] = ((grd_normals[nc+1][nr+1][2] - grd_normals[nc][nr+1][2])/yd)*dy +
                                                grd_normals[nc][nr+1][2];
         break;
       case 2:
         nn[0] = ((grd_normals[nc+1][nr+1][0] - grd_normals[nc+1][nr][0])/xd)*dx +
                                                grd_normals[nc+1][nr][0];
         nn[1] = ((grd_normals[nc+1][nr+1][1] - grd_normals[nc+1][nr][1])/xd)*dx +
                                                grd_normals[nc+1][nr][1];
         nn[2] = ((grd_normals[nc+1][nr+1][2] - grd_normals[nc+1][nr][2])/xd)*dx +
                                                grd_normals[nc+1][nr][2];
         break;
       default:
         System.out.println("interpNormals, bad side: "+side);
     }
     //- re-normalize
     float mag = (float) Math.sqrt(nn[0]*nn[0] + nn[1]*nn[1] + nn[2]*nn[2]);
     nn[0] /= mag;
     nn[1] /= mag;
     nn[2] /= mag;
     tri_normals[n_idx[0]++] = nn[0];
     tri_normals[n_idx[0]++] = nn[1];
     tri_normals[n_idx[0]++] = nn[2];
   }
 
   private static void fillToLast(float xx, float yy, float xd, float yd, 
           int nc, int nr,
           float[] vv1, float[] vv2,
           float[] vv1_last, float[] vv2_last,
           float[][] tri, int[] cnt_tri, int[] t_idx,
           byte[][] tri_color, byte[][] color_bin, int cc, int color_length,
           float[][][] grd_normals, int[] n_idx,
           float[] tri_normals)
   {
     int tt = t_idx[0];
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = color_bin[ii][cc];
     }
     tri[0][tt]       = vv1[0];
     tri[1][tt]       = vv1[1];
     interpNormals(tri[0][tt],
                   tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = color_bin[ii][cc];
     }
     tri[0][tt]       = vv2[0];
     tri[1][tt]       = vv2[1];
     interpNormals(tri[0][tt],
                   tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = color_bin[ii][cc];
     }
     tri[0][tt]       = vv1_last[0];
     tri[1][tt]       = vv1_last[1];
     interpNormals(tri[0][tt],
                   tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
     cnt_tri[0]++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = color_bin[ii][cc];
     }
     tri[0][tt] = vv1_last[0];
     tri[1][tt] = vv1_last[1];
     interpNormals(tri[0][tt],
                   tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = color_bin[ii][cc];
     }
     tri[0][tt] = vv2_last[0];
     tri[1][tt] = vv2_last[1];
     interpNormals(tri[0][tt],
                   tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = color_bin[ii][cc];
     }
     tri[0][tt] = vv2[0];
     tri[1][tt] = vv2[1];
     interpNormals(tri[0][tt],
                   tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
     t_idx[0] = tt;
     cnt_tri[0]++;
   }
 
   private static void fillCaseSix(float xx, float yy, float xd, float yd,
           int v_idx, int dir, byte[] o_flags, short ctrLow,
           float[] vx, float[] vy, int nc, int nr,
           byte[][] crnr_color, boolean[] crnr_out,
           float[][] tri, int[] t_idx,
           byte[][] color_bin, byte[][] tri_color, int color_length,
           float[][][] grd_normals, int[] n_idx,
           float[] tri_normals, int[] closed, int[] cnt_tri)
   {
     int n1 = 0;
     int n2 = 0;
     int n4 = 0;
     int n7 = 0;
 
     for (int kk = 0; kk < o_flags.length; kk++) {
       if ((o_flags[kk] - 32)==1 || o_flags[kk]==1) n1++;
       if ((o_flags[kk] - 32)==2 || o_flags[kk]==2) n2++;
       if ((o_flags[kk] - 32)==4 || o_flags[kk]==4) n4++;
       if ((o_flags[kk] - 32)==7 || o_flags[kk]==7) n7++;
     }
     float[][] vv1     = new float[2][n1*2];
     float[][] vv2     = new float[2][n2*2];
     float[][] vv4     = new float[2][n4*2];
     float[][] vv7     = new float[2][n7*2];
     int[] clr_idx1    = new int[n1];
     int[] clr_idx2    = new int[n2];
     int[] clr_idx4    = new int[n4];
     int[] clr_idx7    = new int[n7];
     float[] vvv1      = new float[2];
     float[] vvv2      = new float[2];
     float[] vvv1_last = new float[2];
     float[] vvv2_last = new float[2];
 
     n1 = 0;
     n2 = 0;
     n4 = 0;
     n7 = 0;
     int ii  = v_idx;
     int cc  = ctrLow - 1;
     int cnt = 0;
     int[] cases = {1, 2, 7, 4};  //- corner cases, clockwise around box
 
     for (int kk = 0; kk < o_flags.length; kk++) {
       if (o_flags[kk] > 32) cnt++;
 
       if ((o_flags[kk] - 32)==1 || o_flags[kk]==1) {
         clr_idx1[n1]   = cc;
         vv1[0][2*n1]   = vx[ii];
         vv1[1][2*n1]   = vy[ii];
         vv1[0][2*n1+1] = vx[ii+1];
         vv1[1][2*n1+1] = vy[ii+1];
         n1++;
       }
       else if ((o_flags[kk] - 32)==2 || o_flags[kk]==2) {
         clr_idx2[n2]   = cc;
         vv2[0][2*n2]   = vx[ii];
         vv2[1][2*n2]   = vy[ii];
         vv2[0][2*n2+1] = vx[ii+1];
         vv2[1][2*n2+1] = vy[ii+1];
         n2++;
       }
       else if ((o_flags[kk] - 32)==4 || o_flags[kk]==4) {
         clr_idx4[n4]   = cc;
         vv4[0][2*n4]   = vx[ii];
         vv4[1][2*n4]   = vy[ii];
         vv4[0][2*n4+1] = vx[ii+1];
         vv4[1][2*n4+1] = vy[ii+1];
         n4++;
       }
       else if ((o_flags[kk] - 32)==7 || o_flags[kk]==7) {
         clr_idx7[n7]   = cc;
         vv7[0][2*n7]   = vx[ii];
         vv7[1][2*n7]   = vy[ii];
         vv7[0][2*n7+1] = vx[ii+1];
         vv7[1][2*n7+1] = vy[ii+1];
         n7++;
       }
       if (o_flags[kk] < 32) {
         cc += 1;
       }
       else if (cnt == 2) {
         cnt = 0;
         cc++;
       }
       ii += 2;
     }
 
     int[] clr_idx = null;
     float[] vvx   = null;
     float[] vvy   = null;
     float[] x_avg = new float[2];
     float[] y_avg = new float[2];
     float dist_0  = 0;
     float dist_1  = 0;
     float xxx     = 0;
     float yyy     = 0;
     float dx      = 0;
     float dy      = 0;
     int nn        = 0;
     int pt        = 0;
     int n_pt      = 0;
     int s_idx     = 0;
     int ns_idx    = 0;
     byte[] tmp        = null;
     byte[] cntr_color = null;
     int cntr_clr  = Integer.MIN_VALUE;
 
     float[][] edge_points = new float[2][8];
     boolean[] edge_point_a_corner =
       {false, false, false, false, false, false, false, false};
     boolean[] edge_point_out =
       {false, false, false, false, false, false, false, false};
     boolean this_crnr_out = false;
     int     n_crnr_out = 0;
     
     //- fill corners
     for (int kk = 0; kk < cases.length; kk++) {
       switch(cases[kk]) {
         case 1:
           nn = n1;
           clr_idx = clr_idx1;
           vvx = vv1[0];
           vvy = vv1[1];
           xxx = xx;
           yyy = yy;
           pt     = 0;
           n_pt   = 7;
           s_idx  = 0;
           ns_idx = 1;
           tmp = crnr_color[0];
           this_crnr_out = crnr_out[0];
           break;
         case 2:
           nn = n2;
           clr_idx = clr_idx2;
           vvx = vv2[0];
           vvy = vv2[1];
           xxx = xx + xd;
           yyy = yy;
           pt     = 1;
           n_pt   = 2;
           s_idx  = 0;
           ns_idx = 1;
           tmp = crnr_color[1];
           this_crnr_out = crnr_out[1];
           break;
         case 4:
           nn = n4;
           clr_idx = clr_idx4;
           vvx = vv4[0];
           vvy = vv4[1];
           xxx = xx;
           yyy = yy + yd;
           pt     = 5;
           n_pt   = 6;
           s_idx  = 1;
           ns_idx = 0;
           tmp = crnr_color[2];
           this_crnr_out = crnr_out[2];
           break;
         case 7:
           nn = n7;
           clr_idx = clr_idx7;
           vvx = vv7[0];
           vvy = vv7[1];
           xxx = xx + xd;
           yyy = yy + yd;
           pt     = 3;
           n_pt   = 4;
           s_idx  = 0;
           ns_idx = 1;
           tmp = crnr_color[3];
           this_crnr_out = crnr_out[3];
           break;
       }
 
       if ( nn == 0 ) {
         edge_points[0][pt]   = xxx;
         edge_points[1][pt]   = yyy;
         edge_points[0][n_pt] = xxx;
         edge_points[1][n_pt] = yyy;
         cntr_color           = tmp;
         edge_point_a_corner[pt]   = true;
         edge_point_a_corner[n_pt] = true;
         edge_point_out[pt] = this_crnr_out;
         edge_point_out[n_pt] = this_crnr_out;
         if (this_crnr_out) n_crnr_out++;
       }
       else if ( nn == 1) {
         fillToNearCorner(xx, yy, xd, yd, 0, (byte)cases[kk], cnt_tri, dir,
                          vvx, vvy, nc, nr, crnr_color, crnr_out,
                          tri, t_idx, tri_color,
                          grd_normals, n_idx, tri_normals, closed);
 
         edge_points[0][pt]   = vvx[s_idx];
         edge_points[1][pt]   = vvy[s_idx];
         edge_points[0][n_pt] = vvx[ns_idx];
         edge_points[1][n_pt] = vvy[ns_idx];
         if(clr_idx[0] > cntr_clr) cntr_clr = clr_idx[0];
       }
       else {
         int il   = 0;
         int idx  = 0;
         x_avg[0] = (vvx[idx] + vvx[idx+1])/2;
         y_avg[0] = (vvy[idx] + vvy[idx+1])/2;
         idx      = idx + 2;
         x_avg[1] = (vvx[idx] + vvx[idx+1])/2;
         y_avg[1] = (vvy[idx] + vvy[idx+1])/2;
 
         dy = (y_avg[1] - (yyy));
         dx = (x_avg[1] - (xxx));
         dist_1 = dy*dy + dx*dx;
         dy = (y_avg[0] - (yyy));
         dx = (x_avg[0] - (xxx));
         dist_0 = dy*dy + dx*dx;
 
         boolean cornerFirst = false;
         if ( dist_1 > dist_0) cornerFirst = true;
 
         if (cornerFirst) {
           fillToNearCorner(xx, yy, xd, yd, 0, (byte)cases[kk], cnt_tri, dir,
                            vvx, vvy, nc, nr, crnr_color, crnr_out,
                            tri, t_idx, tri_color,
                            grd_normals, n_idx, tri_normals, closed);
         }
         else {
           edge_points[0][pt]   = vvx[s_idx];
           edge_points[1][pt]   = vvy[s_idx];
           edge_points[0][n_pt] = vvx[ns_idx];
           edge_points[1][n_pt] = vvy[ns_idx];
           if(clr_idx[0] > cntr_clr) cntr_clr = clr_idx[0];
         }
         for (il = 1; il < nn; il++) {
           idx =  dir*il*2;
           int idx_last = idx - 2*dir;
 
           vvv1[0]      = vvx[idx];
           vvv1[1]      = vvy[idx];
           vvv2[0]      = vvx[idx+dir];
           vvv2[1]      = vvy[idx+dir];
           vvv1_last[0] = vvx[idx_last];
           vvv1_last[1] = vvy[idx_last];
           vvv2_last[0] = vvx[idx_last+dir];
           vvv2_last[1] = vvy[idx_last+dir];
 
           fillToLast(xx, yy, xd, yd, nc, nr, vvv1, vvv2, vvv1_last, vvv2_last,
                      tri, cnt_tri, t_idx, tri_color, color_bin,
                      clr_idx[il], color_length, grd_normals, n_idx, tri_normals);
 
           if (!cornerFirst && il == (nn-1)) {
             fillToNearCorner(xx, yy, xd, yd, idx, (byte)cases[kk], cnt_tri, dir,
                              vvx, vvy, nc, nr, crnr_color, crnr_out,
                              tri, t_idx, tri_color,
                              grd_normals, n_idx, tri_normals, closed);
           }
           if (cornerFirst && il == (nn-1)) {
             edge_points[0][pt]   = vvx[idx+s_idx];
             edge_points[1][pt]   = vvy[idx+s_idx];
             edge_points[0][n_pt] = vvx[idx+ns_idx];
             edge_points[1][n_pt] = vvy[idx+ns_idx];
             if(clr_idx[il] > cntr_clr) cntr_clr = clr_idx[il];
           }
         }
       }
     }
 
     //- fill remainder with tris all sharing center point
     //----------------------------------------------------
     if (n_crnr_out == 2) { //- don't fill center region
       return;
     }
 
     if (cntr_color == null) { //- All corners were closed off
       cntr_color = new byte[color_length];
       for (int c=0; c<color_length; c++) {
         cntr_color[c] = color_bin[c][cntr_clr];
       }
     }
     int tt = t_idx[0];
     for (int kk = 0; kk < edge_points[0].length; kk++) {
       pt   = kk;
       n_pt = kk+1;
       if (kk == (edge_points[0].length - 1)) {
         pt   = 0;
         n_pt = 7;
       }
 
       if (edge_point_a_corner[pt] || edge_point_a_corner[n_pt]) {
         if(edge_point_out[pt] || edge_point_out[n_pt]) continue;
       }
 
       for (int c=0; c<color_length; c++) {
         tri_color[c][tt] = cntr_color[c];
       }
       tri[0][tt]       = edge_points[0][pt];
       tri[1][tt]       = edge_points[1][pt];
       interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                     xd, yd, grd_normals, n_idx, tri_normals);
       tt++;
 
       for (int c=0; c<color_length; c++) {
         tri_color[c][tt] = cntr_color[c];
       }
       tri[0][tt]       = edge_points[0][n_pt];
       tri[1][tt]       = edge_points[1][n_pt];
       interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                     xd, yd, grd_normals, n_idx, tri_normals);
       tt++;
 
       //- center point
       for (int c=0; c<color_length; c++) {
         tri_color[c][tt] = cntr_color[c];
       }
       tri[0][tt]       = xx + xd*0.5f;
       tri[1][tt]       = yy + yd*0.5f;
       //- center normal, interpolate from corners
       float[] nrm = {0f, 0f, 0f};
       for (int ic=0; ic<2; ic++) {
         for (int ir=0; ir<2; ir++) {
           nrm[0] += 0.25*grd_normals[nc+ic][nr+ir][0];
           nrm[1] += 0.25*grd_normals[nc+ic][nr+ir][1];
           nrm[2] += 0.25*grd_normals[nc+ic][nr+ir][2];
         }
       }
       //- renormalize normal
       float mag = (float) 
         Math.sqrt(nrm[0]*nrm[0] + nrm[1]*nrm[1] + nrm[2]*nrm[2]);
       nrm[0] /= mag;
       nrm[1] /= mag;
       nrm[2] /= mag;
       tri_normals[n_idx[0]++] = nrm[0];
       tri_normals[n_idx[0]++] = nrm[1];
       tri_normals[n_idx[0]++] = nrm[2];
       tt++;
 
       cnt_tri[0]++;
     }
     t_idx[0] = tt;
   }
 
   private static void fillToNearCorner(float xx, float yy, float xd, float yd,
           int v_idx, byte o_flag, int[] cnt, int dir,
           float[] vx, float[] vy, int nc, int nr,
           byte[][] crnr_color, boolean[] crnr_out,
           float[][] tri, int[] t_idx,
           byte[][] tri_color,
           float[][][] grd_normals, int[] n_idx,
           float[] tri_normals, int[] closed)
   {
     float cx  = 0;
     float cy  = 0;
     int   cc  = 0;
 
     int color_length = crnr_color[0].length;
     int cnt_tri = cnt[0];
     int tt = t_idx[0];
 
     switch(o_flag) {
       case 1:
         cc = 0;
         closed[0] = closed[0] | 1;
         if (crnr_out[cc]) return;
         cx = xx;
         cy = yy;
         tri_normals[n_idx[0]++] = grd_normals[nc][nr][0];
         tri_normals[n_idx[0]++] = grd_normals[nc][nr][1];
         tri_normals[n_idx[0]++] = grd_normals[nc][nr][2];
         break;
       case 4:
         cc = 2;
         closed[0] = closed[0] | 4;
         if (crnr_out[cc]) return;
         cx = xx;
         cy = yy + yd;
         tri_normals[n_idx[0]++] = grd_normals[nc+1][nr][0];
         tri_normals[n_idx[0]++] = grd_normals[nc+1][nr][1];
         tri_normals[n_idx[0]++] = grd_normals[nc+1][nr][2];
         break;
       case 2:
         cc = 1;
         closed[0] = closed[0] | 2;
         if (crnr_out[cc]) return;
         cx = xx + xd;
         cy = yy;
         tri_normals[n_idx[0]++] = grd_normals[nc][nr+1][0];
         tri_normals[n_idx[0]++] = grd_normals[nc][nr+1][1];
         tri_normals[n_idx[0]++] = grd_normals[nc][nr+1][2];
         break;
       case 7:
         cc = 3;
         closed[0] = closed[0] | 8;
         if (crnr_out[cc]) return;
         cx = xx + xd;
         cy = yy + yd;
         tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][0];
         tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][1];
         tri_normals[n_idx[0]++] = grd_normals[nc+1][nr+1][2];
         break;
     }
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt]       = cx;
     tri[1][tt]       = cy;
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt]       = vx[v_idx];
     tri[1][tt]       = vy[v_idx];
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt]       = vx[v_idx+dir];
     tri[1][tt]       = vy[v_idx+dir];
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     cnt_tri++;
     cnt[0] = cnt_tri;
     t_idx[0] = tt;
   }
 
   private static void fillToOppCorner(float xx, float yy, float xd, float yd,
          int v_idx, byte o_flag, int[] cnt, int dir,
          float[] vx, float[] vy, int nc, int nr,
          byte[][] crnr_color, boolean[] crnr_out,
          float[][] tri, int[] t_idx, byte[][] tri_color,
          float[][][] grd_normals, int[] n_idx, float[] tri_normals, int[] closed)
   {
     float cx1 = 0;
     float cx2 = 0;
     float cx3 = 0;
     float cy1 = 0;
     float cy2 = 0;
     float cy3 = 0;
     int    cc = 0;
     int[][] grd = new int[3][2];
     int color_length = crnr_color[0].length;
 
     switch (o_flag) {
       case 1:
         closed[0] = closed[0] | 14;
         if (crnr_out[1] || crnr_out[2] || crnr_out[3]) return;
         cx1 = xx + xd;
         cy1 = yy;
         cx2 = xx + xd;
         cy2 = yy + yd;
         cx3 = xx;
         cy3 = yy + yd;
         cc  = 3;
         grd[0][0] = 1;
         grd[0][1] = 0;
         grd[1][0] = 1;
         grd[1][1] = 1;
         grd[2][0] = 0;
         grd[2][1] = 1;
         break;
       case 2:
         closed[0] = closed[0] | 13;
         if (crnr_out[0] || crnr_out[2] || crnr_out[3]) return;
         cx1 = xx;
         cy1 = yy;
         cx2 = xx;
         cy2 = yy + yd;
         cx3 = xx + xd;
         cy3 = yy + yd;
         cc  = 2;
         grd[0][0] = 0;
         grd[0][1] = 0;
         grd[1][0] = 0;
         grd[1][1] = 1;
         grd[2][0] = 1;
         grd[2][1] = 1;
         break;
       case 4:
         closed[0] = closed[0] | 11;
         if (crnr_out[0] || crnr_out[1] || crnr_out[3]) return;
         cx1 = xx;
         cy1 = yy;
         cx2 = xx + xd;
         cy2 = yy;
         cx3 = xx + xd;
         cy3 = yy + yd;
         cc  = 1;
         grd[0][0] = 0;
         grd[0][1] = 0;
         grd[1][0] = 1;
         grd[1][1] = 0;
         grd[2][0] = 1;
         grd[2][1] = 1;
         break;
       case 7:
         closed[0] = closed[0] | 7;
         if (crnr_out[0] || crnr_out[1] || crnr_out[2]) return;
         cx1 = xx + xd;
         cy1 = yy;
         cx2 = xx;
         cy2 = yy;
         cx3 = xx;
         cy3 = yy + yd;
         cc  = 0;
         grd[0][0] = 1;
         grd[0][1] = 0;
         grd[1][0] = 0;
         grd[1][1] = 0;
         grd[2][0] = 0;
         grd[2][1] = 1;
         break;
     }
 
     int cnt_tri = cnt[0];
     int tt = t_idx[0];
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt]       = cx1;
     tri[1][tt]       = cy1;
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[0][1]][nr+grd[0][0]][0];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[0][1]][nr+grd[0][0]][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[0][1]][nr+grd[0][0]][2];
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt]       = cx2;
     tri[1][tt]       = cy2;
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][0];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][2];
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     if (dir > 0) {
       tri[0][tt] = vx[v_idx];
       tri[1][tt] = vy[v_idx];
     }
     else {
       tri[0][tt] = vx[v_idx+dir];
       tri[1][tt] = vy[v_idx+dir];
     }
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
     cnt_tri++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = cx3;
     tri[1][tt] = cy3;
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[2][1]][nr+grd[2][0]][0];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[2][1]][nr+grd[2][0]][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[2][1]][nr+grd[2][0]][2];
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = cx2;
     tri[1][tt] = cy2;
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][0];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][2];
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     if (dir > 0) {
       tri[0][tt]   = vx[v_idx+dir];
       tri[1][tt]   = vy[v_idx+dir];
     }
     else {
       tri[0][tt]   = vx[v_idx];
       tri[1][tt]   = vy[v_idx];
     }
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
     cnt_tri++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = cx2;
     tri[1][tt] = cy2;
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][0];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+grd[1][1]][nr+grd[1][0]][2];
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = vx[v_idx];
     tri[1][tt] = vy[v_idx];
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt]   = vx[v_idx+dir];
     tri[1][tt]   = vy[v_idx+dir];
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
     cnt_tri++;
 
     cnt[0] = cnt_tri;
     t_idx[0] = tt;
   }
 
   private static void fillToSide(float xx, float yy, float xd, float yd,
          int v_idx, byte o_flag, int flag, int[] cnt, int dir,
          float[] vx, float[] vy, int nc, int nr, 
          byte[][] crnr_color, boolean[] crnr_out,
          float[][] tri, int[] t_idx, byte[][] tri_color,
          float[][][] grd_normals, int[] n_idx, float[] tri_normals,
          int[] closed)
   {
     int cnt_tri = cnt[0];
     int tt = t_idx[0];
     float cx1 = 0;
     float cy1 = 0;
     float cx2 = 0;
     float cy2 = 0;
     int    cc = 0;
     int[][] grd = new int[2][2];
     int color_length = crnr_color[0].length;
 
     switch (o_flag)
     {
       case 3:
         switch (flag) {
           case  1:
             closed[0] = closed[0] | 12;
             if(crnr_out[2] || crnr_out[3]) return;
             cx1 = xx;
             cy1 = yy + yd;
             cx2 = xx + xd;
             cy2 = yy + yd;
             cc  = 3;
             grd[0][0] = 0;
             grd[0][1] = 1;
             grd[1][0] = 1;
             grd[1][1] = 1;
             break;
           case -1:
             closed[0] = closed[0] | 3;
             if(crnr_out[0] || crnr_out[1]) return;
             cx1 = xx;
             cy1 = yy;
             cx2 = xx + xd;
             cy2 = yy;
             cc  = 0;
             grd[0][0] = 0;
             grd[0][1] = 0;
             grd[1][0] = 1;
             grd[1][1] = 0;
             break;
         }
         break;
 
       case 5:
         switch (flag) {
           case 1:
             closed[0] = closed[0] | 5;
             if(crnr_out[0] || crnr_out[2]) return;
             cx1 = xx;
             cy1 = yy;
             cx2 = xx;
             cy2 = yy + yd;
             cc  = 0;
             grd[0][0] = 0;
             grd[0][1] = 0;
             grd[1][0] = 0;
             grd[1][1] = 1;
             break;
           case -1:
             closed[0] = closed[0] | 10;
             if(crnr_out[1] || crnr_out[3]) return;
             cx1 = xx + xd;
             cy1 = yy;
             cx2 = xx + xd;
             cy2 = yy + yd;
             grd[0][0] = 1;
             grd[0][1] = 0;
             grd[1][0] = 1;
             grd[1][1] = 1;
             cc  = 3;
             break;
         }
         break;
         
     }
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = cx1;
     tri[1][tt] = cy1;
     int i = grd[0][0];
     int j = grd[0][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][0];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][2];
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = vx[v_idx];
     tri[1][tt] = vy[v_idx];
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = vx[v_idx+dir];
     tri[1][tt] = vy[v_idx+dir];
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
     cnt_tri++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = cx1;
     tri[1][tt] = cy1;
     i = grd[0][0];
     j = grd[0][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][0];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][2];
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     if ( dir > 0 ) {
       tri[0][tt] = vx[v_idx+dir];
       tri[1][tt] = vy[v_idx+dir];
     }
     else {
       tri[0][tt] = vx[v_idx];
       tri[1][tt] = vy[v_idx];
     }
     t_idx[0] = tt;
     interpNormals(tri[0][tt], tri[1][tt], xx, yy, nc, nr,
                   xd, yd, grd_normals, n_idx, tri_normals);
     tt++;
 
     for (int ii=0; ii<color_length; ii++) {
       tri_color[ii][tt] = crnr_color[cc][ii];
     }
     tri[0][tt] = cx2;
     tri[1][tt] = cy2;
     i = grd[1][0];
     j = grd[1][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][0];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][1];
     tri_normals[n_idx[0]++] = grd_normals[nc+j][nr+i][2];
     tt++;
     cnt_tri++;
    
     cnt[0] = cnt_tri;
     t_idx[0] = tt;
   }
 
   // APPLET SECTION
 
   /*** run 'appletviewer contour.html' to test the Contour2D class.
   public void init() {
     this.addMouseListener(this);
     con = new Contour2D();
     con.rows = 0;
     con.cols = 0;
     con.scale = 0;
     float intv = 0;
     int mxv1 = 0;
     int mxv2 = 0;
     int mxv3 = 0;
     int mxv4 = 0;
     try {
       String temp = new String("true");
       con.showgrid = temp.equalsIgnoreCase(getParameter("showgrid"));
       con.rows = Integer.parseInt(getParameter("rows"));
       con.cols = Integer.parseInt(getParameter("columns"));
       con.scale = Integer.parseInt(getParameter("scale"));
       intv = Double.valueOf(getParameter("interval")).floatValue();
       mxv1 = Integer.parseInt(getParameter("capacity1"));
       mxv2 = Integer.parseInt(getParameter("capacity2"));
       mxv3 = Integer.parseInt(getParameter("capacity3"));
       mxv4 = Integer.parseInt(getParameter("capacity4"));
     }
     catch (Exception e) {
       System.out.println("Contour2D.paint: applet parameter error: "+e);
       System.exit(1);
     }
     float[] g = new float[con.rows*con.cols];
     float mr = con.rows/2;
     float mc = con.cols/2;
     for (int i=0; i<con.rows; i++) {
       for (int j=0; j<con.cols; j++) {
         g[con.rows*j+i] = (float) Math.sqrt((i-mr)*(i-mr) + (j-mc)*(j-mc));
       }
     }
     float low = 0;
     float high = 100;
     float base = 1;
     con.num1 = new int[1];
     con.num2 = new int[1];
     con.num3 = new int[1];
     con.num4 = new int[1];
     con.vx1 = new float[1][mxv1];
     con.vy1 = new float[1][mxv1];
     con.vx2 = new float[1][mxv2];
     con.vy2 = new float[1][mxv2];
     con.vx3 = new float[1][mxv3];
     con.vy3 = new float[1][mxv3];
     con.vx4 = new float[1][mxv4];
     con.vy4 = new float[1][mxv4];
 
     float[][] tri = new float[2][];
     byte[][] tri_color = new byte[3][];
     float[][][] grd_normals = new float[3][][];
     float[][] tri_normals = new float[1][];
     byte[][] interval_colors = new byte[3][];
     float[][][][] lbl_vv     = new float[4][][][];
     byte[][][][] lbl_cc     = new byte[4][][][];
     float[][][]  lbl_loc    = new float[3][][];
     double       scale_ratio = 1;
     double       label_size  = 1;
     Gridded3DSet gset = null;
     try {
       boolean[] swap = {false, false, false};
       float[] intervals = {.25f, .5f, 1.0f, 2.0f, 2.5f, 5.f, 10.f};
 //    con.contour(g, con.rows, con.cols, intervals, low, high, base, true,
       con.contour(g, con.rows, con.cols, intv, low, high, base,
                   con.vx1, con.vy1, mxv1, con.num1,
                   con.vx2, con.vy2, mxv2, con.num2,
                   con.vx3, con.vy3, mxv3, con.num3,
                   con.vx4, con.vy4, mxv4, con.num4,
                   null, null, null, null, swap, false, tri, tri_color,
                   grd_normals, tri_normals, interval_colors, 
                   lbl_vv, lbl_cc, lbl_loc, scale_ratio, label_size, gset);
     }
     catch (VisADException VE) {
       System.out.println("Contour2D.init: "+VE);
       System.exit(1);
     }
   }
 
   public void mouseClicked(MouseEvent e) {
     // cycle between hidden contours, labels, and backwards labels
     con.whichlabels = (con.whichlabels+1)%5;
     Graphics g = getGraphics();
     if (g != null) {
       paint(g);
       g.dispose();
     }
   }
 
   public void mousePressed(MouseEvent e) {;}
   public void mouseReleased(MouseEvent e) {;}
   public void mouseEntered(MouseEvent e) {;}
   public void mouseExited(MouseEvent e) {;}
 
   public void paint(Graphics gr) {
     // draw grid dots if option is set
     if (con.showgrid) {
       gr.setColor(Color.blue);
       for (int i=0; i<con.cols; i++) {
         for (int j=0; j<con.rows; j++) {
           gr.drawRect(con.scale*i, con.scale*j, 2, 2);
         }
       }
     }
     // draw main contour lines
     gr.setColor(Color.black);
     for (int i=0; i<con.num1[0]; i+=2) {
       int v1 = (int) (con.scale*con.vy1[0][i]);
       int v2 = (int) (con.scale*con.vx1[0][i]);
       int v3 = (int) (con.scale*con.vy1[0][(i+1)%con.num1[0]]);
       int v4 = (int) (con.scale*con.vx1[0][(i+1)%con.num1[0]]);
       gr.drawLine(v1, v2, v3, v4);
     }
     for (int ix=-1; ix<1; ix++) {
       if (ix<0) gr.setColor(Color.white); else gr.setColor(Color.black);
       switch ((con.whichlabels+ix+5)%5) {
         case 0: // hidden contours are exposed
           for (int i=0; i<con.num2[0]; i+=2) {
             int v1 = (int) (con.scale*con.vy2[0][i]);
             int v2 = (int) (con.scale*con.vx2[0][i]);
             int v3 = (int) (con.scale*con.vy2[0][(i+1)%con.num2[0]]);
             int v4 = (int) (con.scale*con.vx2[0][(i+1)%con.num2[0]]);
             gr.drawLine(v1, v2, v3, v4);
           }
           break;
         case 1: // numbers cover hidden contours
           for (int i=0; i<con.num3[0]; i+=2) {
             int v1 = (int) (con.scale*con.vy3[0][i]);
             int v2 = (int) (con.scale*con.vx3[0][i]);
             int v3 = (int) (con.scale*con.vy3[0][(i+1)%con.num3[0]]);
             int v4 = (int) (con.scale*con.vx3[0][(i+1)%con.num3[0]]);
             gr.drawLine(v1, v2, v3, v4);
           }
           break;
         case 2: // numbers cover hidden contours, backwards
           for (int i=0; i<con.num4[0]; i+=2) {
             int v1 = (int) (con.scale*con.vy4[0][i]);
             int v2 = (int) (con.scale*con.vx3[0][i]);
             int v3 = (int) (con.scale*con.vy4[0][(i+1)%con.num4[0]]);
             int v4 = (int) (con.scale*con.vx3[0][(i+1)%con.num3[0]]);
             gr.drawLine(v1, v2, v3, v4);
           }
           break;
         case 3: // numbers cover hidden contours, upside-down
           for (int i=0; i<con.num3[0]; i+=2) {
             int v1 = (int) (con.scale*con.vy3[0][i]);
             int v2 = (int) (con.scale*con.vx4[0][i]);
             int v3 = (int) (con.scale*con.vy3[0][(i+1)%con.num3[0]]);
             int v4 = (int) (con.scale*con.vx4[0][(i+1)%con.num4[0]]);
             gr.drawLine(v1, v2, v3, v4);
           }
           break;
         case 4: // numbers cover hidden contours, upside-down & backwards
           for (int i=0; i<con.num3[0]; i+=2) {
             int v1 = (int) (con.scale*con.vy4[0][i]);
             int v2 = (int) (con.scale*con.vx4[0][i]);
             int v3 = (int) (con.scale*con.vy4[0][(i+1)%con.num4[0]]);
             int v4 = (int) (con.scale*con.vx4[0][(i+1)%con.num4[0]]);
             gr.drawLine(v1, v2, v3, v4);
           }
       } // end switch
     }
   }
   ****/
 
 } // end class
 
 class ContourStripSet {
 
   int     mxsize;
   float[] levels;
   int     n_levs;
   int     nr;
   int     nc;
   Gridded3DSet spatial_set;
 
   java.util.Vector[] vecArray;
   java.util.Vector vec;
 
   PlotDigits[] plot_s;
   float[][] plot_min_max;
   boolean[] swap;
 
   ContourStripSet(int size, float[] levels, boolean[] swap, 
                   double scale_ratio, double label_size, int nr, int nc,
                   Gridded3DSet spatial_set) 
     throws VisADException {
 
     this.mxsize  = 40*size;
     this.levels  = levels;
     n_levs       = levels.length;
     vecArray     = new java.util.Vector[n_levs];
     plot_s       = new PlotDigits[n_levs];
     plot_min_max = new float[n_levs][2];
     float fac    = (float) ((0.15*(1.0/scale_ratio))*label_size);
     this.nr      = nr;
     this.nc      = nc;
     this.swap    = swap;
     this.spatial_set = spatial_set;
 
     for (int kk = 0; kk < n_levs; kk++) {
       vecArray[kk]    = new java.util.Vector();
       PlotDigits plot = new PlotDigits();
       plot.Number     = levels[kk];
       plot.plotdigits(levels[kk], 0f, 0f, fac*1, fac*1, 400,
                       new boolean[] {false, false, false});
 
       float[][] tmp = new float[2][];
       tmp[0] = plot.Vx;
       tmp[1] = plot.Vy;
       plot.Vx = tmp[1];
       plot.Vy = tmp[0];
       tmp[0] = plot.VxB;
       tmp[1] = plot.VyB;
       plot.VxB = tmp[1];
       plot.VyB = tmp[0];
  
       float vx_min  =  Float.MAX_VALUE;
       float vy_min  =  Float.MAX_VALUE;
       float vx_max  = -Float.MAX_VALUE;
       float vy_max  = -Float.MAX_VALUE;
       float vxB_min =  Float.MAX_VALUE;
       float vyB_min =  Float.MAX_VALUE;
       float vxB_max = -Float.MAX_VALUE;
       float vyB_max = -Float.MAX_VALUE;
       for ( int ii = 0; ii < plot.NumVerts; ii++) {
         if (plot.Vx[ii]  < vx_min) vx_min = plot.Vx[ii];
         if (plot.Vy[ii]  < vy_min) vy_min = plot.Vy[ii];
         if (plot.Vx[ii]  > vx_max) vx_max = plot.Vx[ii];
         if (plot.Vy[ii]  > vy_max) vy_max = plot.Vy[ii];
         if (plot.VxB[ii] < vxB_min) vxB_min = plot.VxB[ii];
         if (plot.VyB[ii] < vyB_min) vyB_min = plot.VyB[ii];
         if (plot.VxB[ii] > vxB_max) vxB_max = plot.VxB[ii];
         if (plot.VyB[ii] > vyB_max) vyB_max = plot.VyB[ii];
       }
       float t_x  = (vx_max-vx_min)/2   + vx_min;
       float t_y  = (vy_max-vy_min)/2   + vy_min;
       float t_xB = (vxB_max-vxB_min)/2 + vxB_min;
       float t_yB = (vyB_max-vyB_min)/2 + vyB_min;
 
       for (int ii = 0; ii < plot.NumVerts; ii++) {
         plot.Vx[ii]  -= t_x;
         plot.Vy[ii]  -= t_y;
         plot.VxB[ii] -= t_xB;
         plot.VyB[ii] -= t_yB;
       }
       plot_s[kk] = plot;
       if (swap[0] == false) {
         plot_min_max[kk][0] = vy_min;
         plot_min_max[kk][1] = vy_max;
       }
       else {
         plot_min_max[kk][0] = vx_min;
         plot_min_max[kk][1] = vx_max;
       }
         plot_min_max[kk][0] = vx_min;
         plot_min_max[kk][1] = vx_max;
     }
   }
 
   void add(float[] vx, float[] vy, int idx0, int idx1, float level) {
     int lev_idx = 0;
     for (int kk = 0; kk < n_levs; kk++) {
       if (level == levels[kk]) lev_idx = kk;
     }
     add(vx, vy, idx0, idx1, lev_idx);
   }
 
   void add(float[] vx, float[] vy, int idx0, int idx1, int lev_idx)
   {
     vec = vecArray[lev_idx];
     int n_strip = vec.size();
 
     if (n_strip == 0) {
       ContourStrip c_strp =
         new ContourStrip(mxsize, lev_idx, idx0, idx1, plot_s[lev_idx], this);
       vec.addElement(c_strp);
     }
     else {
       int[] found_array = new int[2];
       int found = 0;
       for (int kk = 0; kk < n_strip; kk++) {
         ContourStrip c_strp  = (ContourStrip)vec.elementAt(kk);
         if (c_strp.addPair(vx, vy, idx0, idx1)) {
           found_array[found] = kk;
           found++;
         }
       }
       if (found==2) {
         ContourStrip c_strpA = (ContourStrip)vec.elementAt(found_array[0]);
         ContourStrip c_strpB = (ContourStrip)vec.elementAt(found_array[1]);
         ContourStrip c_strp  = c_strpA.merge(c_strpB);
 
         vec.addElement(c_strp);
         vec.removeElement(c_strpA);
         vec.removeElement(c_strpB);
       }
       else if (found == 0) {
         ContourStrip c_strp =
           new ContourStrip(mxsize, lev_idx, idx0, idx1, plot_s[lev_idx], this);
         vec.addElement(c_strp);
       }
     }
   }
 
   void getLineColorArrays(float[] vx, float[] vy, byte[][] colors, int lev_idx,
                           float[][][] out_vv, byte[][][] out_bb,
                           float[][][][] out_vvL, byte[][][][] out_bbL, float[][][] out_loc)
   {
     int n_strips = vecArray[lev_idx].size();
 
     float[][][][] la    = new float[n_strips][2][][];
     byte[][][][]  ca    = new byte[n_strips][2][][];
     float[][][][][] laL = new float[n_strips][4][][][];
     byte[][][][][]  caL = new byte[n_strips][4][][][];
     float[][][][]  locL = new float[n_strips][3][][];
 
     for (int kk=0; kk<n_strips; kk++) {
       ContourStrip cs = (ContourStrip)vecArray[lev_idx].elementAt(kk);
       cs.getLabeledLineColorArray(vx, vy, colors, la[kk], ca[kk], laL[kk], caL[kk], locL[kk]);
     }
 
     //-- contour/contour label gap line arrays
     for (int tt = 0; tt < 2; tt++) {
       int len = 0;
       for (int mm = 0; mm < n_strips; mm++) {
         if (la[mm][tt] != null) {
           len += la[mm][tt][0].length;
         }
       }
       out_vv[tt] = new float[3][len];
       int cnt = 0;
       for (int mm = 0; mm < n_strips; mm++) {
         if (la[mm][tt] != null) {
           System.arraycopy(la[mm][tt][0], 0, out_vv[tt][0], cnt, la[mm][tt][0].length);
           System.arraycopy(la[mm][tt][1], 0, out_vv[tt][1], cnt, la[mm][tt][1].length);
           System.arraycopy(la[mm][tt][2], 0, out_vv[tt][2], cnt, la[mm][tt][1].length);
           cnt += la[mm][tt][0].length;
         }
       }
 
       int clr_dim = 0;
       if (colors != null) {
       clr_dim = colors.length;
       len = 0;
       for (int mm = 0; mm < n_strips; mm++) {
         if (ca[mm][tt] != null) {
           len += ca[mm][tt][0].length;
         }
       }
       out_bb[tt]  = new byte[clr_dim][len];
       cnt = 0;
       for (int mm = 0; mm < n_strips; mm++) {
         if (ca[mm][tt] != null) {
           for (int cc = 0; cc < clr_dim; cc++) {
             System.arraycopy(ca[mm][tt][cc], 0, out_bb[tt][cc], cnt, ca[mm][tt][cc].length);
           }
           cnt += ca[mm][tt][0].length;
         }
       }
       }
     }
 
     //-- label, vx3/vx4, line arrays
     int n_lbl = 0;
     for (int mm = 0; mm < n_strips; mm++) {
       if (laL[mm][0] != null) {
         n_lbl += laL[mm][0].length;
       }
     }
     out_vvL[0] = new float[n_lbl][][];
     out_vvL[1] = new float[n_lbl][][];
     out_vvL[2] = new float[n_lbl][][];
     out_vvL[3] = new float[n_lbl][][];
     out_bbL[0] = new byte[n_lbl][][];
     out_bbL[1] = new byte[n_lbl][][];
     out_bbL[2] = new byte[n_lbl][][];
     out_bbL[3] = new byte[n_lbl][][];
     out_loc[0] = new float[n_lbl][];
     out_loc[1] = new float[n_lbl][];
     out_loc[2] = new float[n_lbl][];
 
     for (int tt = 0; tt < 4; tt++) {
       n_lbl = 0;
       for (int kk = 0; kk < n_strips; kk++) {
         if (laL[kk][tt] != null) {
           for (int mm = 0; mm < laL[kk][tt].length; mm++ ) {
             out_vvL[tt][n_lbl] = laL[kk][tt][mm];
             if (caL[kk][tt] != null) {
             out_bbL[tt][n_lbl] = caL[kk][tt][mm];
             }
             n_lbl++;
           }
         }
       }
     }
     for (int tt = 0; tt < 3; tt++) {
     n_lbl = 0;
     for (int kk = 0; kk < n_strips; kk++) {
       if (locL[kk][0] != null) {
         for (int mm = 0; mm < locL[kk][tt].length; mm++) {
           out_loc[tt][n_lbl] = locL[kk][tt][mm];
           n_lbl++;
         }
       }
     }
     }
   }
 
   void getLineColorArrays(float[] vx, float[] vy, byte[][] colors,
                           float[][][] out_vv, byte[][][] out_bb,
                           float[][][][] out_vvL, byte[][][][] out_bbL, float[][][] out_loc) {
 
     float[][][][] tmp    = new float[n_levs][2][][];
     byte[][][][] btmp    = new byte[n_levs][2][][];
     float[][][][][] tmpL = new float[n_levs][4][][][];
     byte[][][][][] btmpL = new byte[n_levs][4][][][];
     float[][][][] tmpLoc = new float[n_levs][3][][];
 
     int n_lbl = 0;
     for (int kk=0; kk<n_levs; kk++) {
       getLineColorArrays(vx, vy, colors, kk, tmp[kk], btmp[kk], tmpL[kk], btmpL[kk], tmpLoc[kk]);
       n_lbl += tmpL[kk][0].length;
     }
       
     for (int tt = 0; tt < 2; tt++)
     {
       int len = 0;
       for (int kk=0; kk<n_levs; kk++) {
         len += tmp[kk][tt][0].length;
       }
       out_vv[tt] = new float[3][len];
       int cnt = 0;
       for (int kk = 0; kk < n_levs; kk++) {
         System.arraycopy(tmp[kk][tt][0], 0, out_vv[tt][0], cnt, tmp[kk][tt][0].length);
         System.arraycopy(tmp[kk][tt][1], 0, out_vv[tt][1], cnt, tmp[kk][tt][0].length);
         System.arraycopy(tmp[kk][tt][2], 0, out_vv[tt][2], cnt, tmp[kk][tt][0].length);
         cnt += tmp[kk][tt][0].length;
       }
       int clr_dim = 0;
       if (colors != null) {
       clr_dim = colors.length;
       len = 0;
       for (int kk=0; kk<n_levs; kk++) {
         len += btmp[kk][tt][0].length;
       }
       out_bb[tt]  = new byte[clr_dim][len];
       cnt = 0;
       for (int kk = 0; kk < n_levs; kk++) {
         for (int cc = 0; cc < clr_dim; cc++) {
           System.arraycopy(btmp[kk][tt][cc], 0, out_bb[tt][cc], cnt, btmp[kk][tt][cc].length);
         }
         cnt += btmp[kk][tt][0].length;
       }
       }
     }
 
     for (int tt = 0; tt < 4; tt++) 
     {
       out_vvL[tt] = new float[n_lbl][][]; 
       int cnt = 0;
       for (int kk = 0; kk<n_levs; kk++) {
         for ( int ll = 0; ll < tmpL[kk][tt].length; ll++) {
           out_vvL[tt][cnt] = tmpL[kk][tt][ll];
           cnt++;
         }
       }
       out_bbL[tt] = new byte[n_lbl][][];
       cnt = 0;
       for (int kk = 0; kk<n_levs; kk++) {
         for ( int ll = 0; ll < tmpL[kk][tt].length; ll++) {
           out_bbL[tt][cnt] = btmpL[kk][tt][ll];
           cnt++;
         }
       }
     }
 
     for (int tt = 0; tt < 3; tt++)
     {
       out_loc[tt] = new float[n_lbl][];
       int cnt = 0;
       for (int kk = 0; kk <n_levs; kk++) {
         if (tmpLoc[kk][tt] != null) {
           for (int ll = 0; ll < tmpLoc[kk][tt].length; ll++) {
             out_loc[tt][cnt] = tmpLoc[kk][tt][ll];
             cnt++;
           }
         }
       }
     }
   }
 }
 
 class ContourStrip {
 
   int[] idx_array;
   int     low_idx;
   int      hi_idx;
   int     lev_idx;
 
   PlotDigits plot;
   ContourStripSet css;
   float lbl_half;
 
   ContourStrip(int mxsize, int lev_idx, int idx0, int idx1, PlotDigits plot,
                ContourStripSet css) {
     idx_array    = new int[mxsize];
     this.lev_idx = lev_idx;
     this.plot    = plot;
 
     low_idx = mxsize/2;
     hi_idx  = low_idx + 1;
     idx_array[low_idx] = idx0;
     idx_array[hi_idx]  = idx1;
     this.css = css;
     this.lbl_half = (css.plot_min_max[lev_idx][1] - css.plot_min_max[lev_idx][0])/2;
     this.lbl_half += this.lbl_half*0.30;
   }
 
   ContourStrip(int[] idx_array, int lev_idx, PlotDigits plot,
                ContourStripSet css) {
     this.lev_idx   = lev_idx;
     int mxsize         = idx_array.length + 400;
     this.idx_array = new int[mxsize];
     this.plot      = plot;
     low_idx = mxsize/2 - (idx_array.length)/2;
     hi_idx  = (low_idx + idx_array.length)-1;
     System.arraycopy(idx_array, 0, this.idx_array, low_idx, idx_array.length);
     this.css = css;
     this.lbl_half = (css.plot_min_max[lev_idx][1] - css.plot_min_max[lev_idx][0])/2;
     this.lbl_half +=  this.lbl_half*0.30;
   }
 
   boolean addPair(float[] vx, float[] vy, int idx0, int idx1) {
 
    float vx0  = vx[idx0];
    float vy0  = vy[idx0];
    float vx1  = vx[idx1];
    float vy1  = vy[idx1];
 
 
    float vx_s = vx[idx_array[low_idx]];
    float vy_s = vy[idx_array[low_idx]];
    float dist = (vx0-vx_s)*(vx0-vx_s)+(vy0-vy_s)*(vy0-vy_s);
 
    if (dist <= 0.00001) {
      if (low_idx < 2) {
        int[] tmp = new int[idx_array.length + 200];
        System.arraycopy(idx_array, 0, tmp, 100, idx_array.length);
        idx_array = tmp;
        tmp = null;
        low_idx += 100;
        hi_idx  += 100;
      }
      low_idx -= 1;
      idx_array[low_idx] = idx0;
      low_idx -= 1;
      idx_array[low_idx] = idx1;
      return true;
    }
    dist = (vx1-vx_s)*(vx1-vx_s)+(vy1-vy_s)*(vy1-vy_s);
    if (dist <= 0.00001) {
      if (low_idx < 2) {
        int[] tmp = new int[idx_array.length + 200];
        System.arraycopy(idx_array, 0, tmp, 100, idx_array.length);
        idx_array = tmp;
        tmp = null;
        low_idx += 100;
        hi_idx  += 100;
      }
      low_idx -= 1;
      idx_array[low_idx] = idx1;
      low_idx -= 1;
      idx_array[low_idx] = idx0;
      return true;
    }
 
    vx_s = vx[idx_array[hi_idx]];
    vy_s = vy[idx_array[hi_idx]];
    dist = (vx0-vx_s)*(vx0-vx_s)+(vy0-vy_s)*(vy0-vy_s);
    if (dist <= 0.00001) {
      if (hi_idx > idx_array.length-2) {
        int[] tmp = new int[idx_array.length + 200];
        System.arraycopy(idx_array, 0, tmp, 100, idx_array.length);
        idx_array = tmp;
        tmp = null;
        low_idx += 100;
        hi_idx  += 100;
      }
      hi_idx += 1;
      idx_array[hi_idx] = idx0;
      hi_idx += 1;
      idx_array[hi_idx] = idx1;
      return true;
    }
    dist = (vx1-vx_s)*(vx1-vx_s)+(vy1-vy_s)*(vy1-vy_s);
    if (dist <= 0.00001) {
      if (hi_idx > idx_array.length-2) {
        int[] tmp = new int[idx_array.length + 200];
        System.arraycopy(idx_array, 0, tmp, 100, idx_array.length);
        idx_array = tmp;
        tmp = null;
        low_idx += 100;
        hi_idx  += 100;
      }
      hi_idx += 1;
      idx_array[hi_idx] = idx1;
      hi_idx += 1;
      idx_array[hi_idx] = idx0;
      return true;
    }
    return false;
   }
 
   void getLabeledLineColorArray(float[] vx, float[] vy, byte[][] colors,
                                 float[][][] out_vv, byte[][][] out_colors,
                                 float[][][][] out_vvL, byte[][][][] out_colorsL,
                                 float[][][]  lbl_loc)
   {
     float[][] vv = null;
     float[][] vv_grid = getLineArray(vx, vy);
 
     try {
       vv = css.spatial_set.gridToValue(vv_grid);
     }
     catch (VisADException e) {
       System.out.println(e.getMessage());
     }
 
     byte[][]  bb      = getColorArray(colors);
     int clr_dim       = 0;
     if (colors != null) clr_dim = colors.length;
     int n_lbl         = 1;
 
     out_vvL[0]        = null;
     out_colorsL[0]    = null;
     out_vvL[1]        = null;
     out_colorsL[1]    = null;
     lbl_loc[0]        = null;
 
     out_vvL[2]        = null;
     out_colorsL[2]    = null;
     out_vvL[3]        = null;
     out_colorsL[3]    = null;
     lbl_loc[1]        = null;
     lbl_loc[2]        = null;
 
     out_vv[0]         = vv;
     out_colors[0]     = bb;
     out_vv[1]         = null;
     out_colors[1]     = null;
 
     if (vv[0].length > 20 && ((lev_idx & 1) == 1))
     {
       int loc         = (vv[0].length)/2;
       int start_break = 0;
       int stop_break  = 0;
       int n_pairs_b   = 1;
       int n_pairs_f   = 1;
       boolean found   = false;
       float ctr_dist;
       int pos = loc;
       while(!found) {
         pos -= 2;
         if (pos < 0 || pos > (vv[0].length-1)) return;
         float dx = vv[0][pos] - vv[0][loc];
         float dy = vv[1][pos] - vv[1][loc];
         float dz = vv[2][pos] - vv[2][loc];
         ctr_dist = (float)Math.sqrt((double)(dx*dx + dy*dy + dz*dz));
         if (ctr_dist > (float)Math.abs((double)lbl_half)) {
           found = true;
         }
         else {
           n_pairs_b++;
         }
       }
 
       pos = loc;
       found = false;
       while(!found) {
         pos += 2;
         if (pos < 0 || pos > (vv[0].length-1)) return;
         float dx = vv[0][pos] - vv[0][loc];
         float dy = vv[1][pos] - vv[1][loc];
         float dz = vv[2][pos] - vv[2][loc];
         ctr_dist = (float)Math.sqrt((double)(dx*dx + dy*dy + dz*dz));
         if (ctr_dist > (float)Math.abs((double)lbl_half)) {
           found = true;
         }
         else {
           n_pairs_f++;
         }
       }
 
       int n_skip    = (n_pairs_b+n_pairs_f)*2;
       if ((loc & 1) == 1) { //- odd
         start_break = loc - (1+(n_pairs_b-1)*2);
         stop_break  = loc + (2+(n_pairs_f-1)*2);
       }
       else { //-even
         start_break = loc - (2+(n_pairs_b-1)*2);
         stop_break  = loc + (1+(n_pairs_f-1)*2);
       }
 
       float[] vx_tmp    = new float[plot.NumVerts];
       float[] vy_tmp    = new float[plot.NumVerts];
       System.arraycopy(plot.Vx, 0, vx_tmp, 0, plot.NumVerts);
       System.arraycopy(plot.Vy, 0, vy_tmp, 0, plot.NumVerts);
       float[] vxB_tmp   = new float[plot.NumVerts];
       float[] vyB_tmp   = new float[plot.NumVerts];
       System.arraycopy(plot.VxB, 0, vxB_tmp, 0, plot.NumVerts);
       System.arraycopy(plot.VyB, 0, vyB_tmp, 0, plot.NumVerts);
 
       byte[][] lbl_clr = null;
       if (bb != null) {
         lbl_clr  = new byte[clr_dim][plot.NumVerts];
       }
 
       boolean rotate = true;
       float[][] lbl_dcoords = null;
       if (rotate)
       {
         float[][] norm = null;
         Gridded3DSet cg3d = (Gridded3DSet) css.spatial_set;
         try {
           norm = cg3d.getNormals(new float[][] {{vv_grid[0][loc]}, {vv_grid[1][loc]}});
         }
         catch (VisADException e) {
           System.out.println(e.getMessage());
         }
 
         if (norm[2][0] < 0) {
           norm[0][0] = -norm[0][0];
           norm[1][0] = -norm[1][0];
           norm[2][0] = -norm[2][0];
         }
 
         float del_x;
         float del_y;
         float del_z;
         del_z   = vv[2][stop_break] - vv[2][start_break];
         del_y   = vv[1][stop_break] - vv[1][start_break];
         del_x   = vv[0][stop_break] - vv[0][start_break];
         float mag     = (float) Math.sqrt(del_y*del_y + del_x*del_x + del_z*del_z);
         float[] ctr_u = new float[] {del_x/mag, del_y/mag, del_z/mag};
         if (ctr_u[0] < 0) {
           ctr_u[0] = -ctr_u[0];
           ctr_u[1] = -ctr_u[1];
           ctr_u[2] = -ctr_u[2];
         }
 
         float[] norm_x_ctr = new float[] {
             norm[1][0]*ctr_u[2]-norm[2][0]*ctr_u[1],
           -(norm[0][0]*ctr_u[2]-norm[2][0]*ctr_u[0]),
             norm[0][0]*ctr_u[1]-norm[1][0]*ctr_u[0]};
 
         mag = (float) Math.sqrt(norm_x_ctr[0]*norm_x_ctr[0] +
                                 norm_x_ctr[1]*norm_x_ctr[1] +
                                 norm_x_ctr[2]*norm_x_ctr[2]);
 
         norm_x_ctr[0] = norm_x_ctr[0]/mag;
         norm_x_ctr[1] = norm_x_ctr[1]/mag;
         norm_x_ctr[2] = norm_x_ctr[2]/mag;
 
         if (Math.abs((double)norm[2][0]) <= 0.00001) {
           if (norm_x_ctr[2] < 0 ) {
             norm_x_ctr[0] = -norm_x_ctr[0];
             norm_x_ctr[1] = -norm_x_ctr[1];
             norm_x_ctr[2] = -norm_x_ctr[2];
           }
         }
         else {
           if (norm_x_ctr[1] < 0 ) {
             norm_x_ctr[0] = -norm_x_ctr[0];
             norm_x_ctr[1] = -norm_x_ctr[1];
             norm_x_ctr[2] = -norm_x_ctr[2];
           }
         }
 
         lbl_dcoords = new float[3][plot.NumVerts];
         for (int kk = 0; kk < plot.NumVerts; kk++) {
           lbl_dcoords[0][kk] = vx_tmp[kk]*ctr_u[0] + vyB_tmp[kk]*norm_x_ctr[0]; 
           lbl_dcoords[1][kk] = vx_tmp[kk]*ctr_u[1] + vyB_tmp[kk]*norm_x_ctr[1];
           lbl_dcoords[2][kk] = vx_tmp[kk]*ctr_u[2] + vyB_tmp[kk]*norm_x_ctr[2];
         }
         for (int kk = 0; kk < plot.NumVerts; kk++) {
           lbl_dcoords[0][kk] += vv[0][loc];
           lbl_dcoords[1][kk] += vv[1][loc];
           lbl_dcoords[2][kk] += vv[2][loc];
         }
 
       }
 
       //-- translate to label plot location --------------
       for (int kk = 0; kk < plot.NumVerts; kk++) {
         vx_tmp[kk]      += vv[0][loc];
         vy_tmp[kk]      += vv[1][loc];
         vxB_tmp[kk]     += vv[0][loc];
         vyB_tmp[kk]     += vv[1][loc];
         if (bb != null) {
           lbl_clr[0][kk]   = bb[0][loc];
           lbl_clr[1][kk]   = bb[1][loc];
           lbl_clr[2][kk]   = bb[2][loc];
         }
       }
 
       out_vvL[0]       = new float[n_lbl][][];
       out_colorsL[0]   = new byte[n_lbl][][];
       out_vvL[1]       = new float[n_lbl][][];
       out_colorsL[1]   = new byte[n_lbl][][];
       lbl_loc[0]       = new float[n_lbl][7];
       lbl_loc[0][0][0] = vv[0][loc];
       lbl_loc[0][0][1] = vv[1][loc];
       lbl_loc[0][0][2] = vv[2][loc];
       out_vv[0]        = new float[3][vv[0].length - n_skip];
       out_vv[1]        = new float[3][n_skip];
       if (bb != null) {
         out_colors[0]    = new byte[clr_dim][bb[0].length - n_skip];
         out_colors[1]    = new byte[clr_dim][n_skip];
       }
 
       int s_pos        = 0;
       int d_pos        = 0;
       int cnt          = start_break;
 
       System.arraycopy(vv[0], s_pos, out_vv[0][0], d_pos, cnt);
       System.arraycopy(vv[1], s_pos, out_vv[0][1], d_pos, cnt);
       System.arraycopy(vv[2], s_pos, out_vv[0][2], d_pos, cnt);
       if (bb != null) {
         for (int cc=0; cc<clr_dim; cc++) {
           System.arraycopy(bb[cc], s_pos, out_colors[0][cc], d_pos, cnt);
         }
       }
 
       s_pos         = start_break;
       d_pos         = 0;
       cnt           = n_skip;
 
       System.arraycopy(vv[0], s_pos, out_vv[1][0], d_pos, cnt);
       System.arraycopy(vv[1], s_pos, out_vv[1][1], d_pos, cnt);
       System.arraycopy(vv[2], s_pos, out_vv[1][2], d_pos, cnt);
       if (bb != null) {
         for (int cc=0; cc<clr_dim; cc++) {
           System.arraycopy(bb[cc], s_pos, out_colors[1][cc], d_pos, cnt);
         }
       }
 
       s_pos         = stop_break+1;
       d_pos         = start_break;
       cnt           = vv[0].length - s_pos;
       
       System.arraycopy(vv[0], s_pos, out_vv[0][0], d_pos, cnt);
       System.arraycopy(vv[1], s_pos, out_vv[0][1], d_pos, cnt);
       System.arraycopy(vv[2], s_pos, out_vv[0][2], d_pos, cnt);
       if (bb != null) {
         for (int cc=0; cc<clr_dim; cc++) {
           System.arraycopy(bb[cc], s_pos, out_colors[0][cc], d_pos, cnt);
         }
       }
      
 
       //--- expanding/contracting left-right segments
 
       out_vvL[2]     = new float[n_lbl][3][2];
       out_vvL[3]     = new float[n_lbl][3][2];
       lbl_loc[1]     = new float[n_lbl][3];
       lbl_loc[2]     = new float[n_lbl][3];
       if (bb != null) {
         out_colorsL[2] = new byte[n_lbl][clr_dim][2];
         out_colorsL[3] = new byte[n_lbl][clr_dim][2];
       }
       //- left
       s_pos = start_break;
       d_pos = 0;
       cnt   = 2;
       lbl_loc[1][0][0] = vv[0][s_pos];
       lbl_loc[1][0][1] = vv[1][s_pos];
       lbl_loc[1][0][2] = vv[2][s_pos];
 
       //- unit left
       float dx = vv[0][loc] - vv[0][s_pos];
       float dy = vv[1][loc] - vv[1][s_pos];
       float dz = vv[2][loc] - vv[2][s_pos];
       float dd = (float)Math.sqrt((double)(dx*dx + dy*dy + dz*dz));
       dx = dx/dd;
       dy = dy/dd;
       dz = dz/dd;
       float mm = dd - (float)Math.abs((double)lbl_half);
       dx *= mm;
       dy *= mm;
       dz *= mm;
       out_vvL[2][0][0][0] = vv[0][s_pos];
       out_vvL[2][0][1][0] = vv[1][s_pos];
       out_vvL[2][0][2][0] = vv[2][s_pos];
       out_vvL[2][0][0][1] = vv[0][s_pos] + dx;
       out_vvL[2][0][1][1] = vv[1][s_pos] + dy;
       out_vvL[2][0][2][1] = vv[2][s_pos] + dz;
       lbl_loc[0][0][3]    = lbl_half;
       lbl_loc[0][0][4]    = dd;
       if (bb != null) {
         for (int cc = 0; cc < clr_dim; cc++) {
           System.arraycopy(bb[cc], s_pos, out_colorsL[2][0][cc], d_pos, cnt);
         }
       }
 
       //- right
       s_pos = stop_break - 1;
       d_pos = 0;
       cnt   = 2;
       lbl_loc[2][0][0] = vv[0][stop_break];
       lbl_loc[2][0][1] = vv[1][stop_break];
       lbl_loc[2][0][2] = vv[2][stop_break];
 
       //- unit right
       dx = vv[0][loc] - vv[0][stop_break];
       dy = vv[1][loc] - vv[1][stop_break];
       dz = vv[2][loc] - vv[2][stop_break];
       dd = (float)Math.sqrt((double)(dx*dx + dy*dy + dz*dz));
       dx = dx/dd;
       dy = dy/dd;
       dz = dz/dd;
       mm = dd - (float)Math.abs((double)lbl_half);
       dx *= mm;
       dy *= mm;
       dz *= mm;
       out_vvL[3][0][0][0] = vv[0][stop_break];
       out_vvL[3][0][1][0] = vv[1][stop_break];
       out_vvL[3][0][2][0] = vv[2][stop_break];
       out_vvL[3][0][0][1] = vv[0][stop_break] + dx;
       out_vvL[3][0][1][1] = vv[1][stop_break] + dy;
       out_vvL[3][0][2][1] = vv[2][stop_break] + dz;
       lbl_loc[0][0][5]    = lbl_half;
       lbl_loc[0][0][6]    = dd;
       if (bb != null) {
         for (int cc = 0; cc < clr_dim; cc++) {
           System.arraycopy(bb[cc], s_pos, out_colorsL[3][0][cc], d_pos, cnt);
         }
       }
       //----- end expanding/contracting line segments
 
 
       //--- label vertices
       out_vvL[0][0]     = new float[3][];
       out_vvL[0][0][0]  = lbl_dcoords[0];
       out_vvL[0][0][1]  = vy_tmp;
       out_colorsL[0][0] = lbl_clr;
       out_vvL[1][0]     = new float[3][];
       out_vvL[1][0][0]  = vxB_tmp;
       out_vvL[1][0][1]  = lbl_dcoords[1];
       out_colorsL[1][0] = lbl_clr;
       out_vvL[0][0][2] = lbl_dcoords[2];
       out_vvL[1][0][2] = lbl_dcoords[2];
     }
     else { //- no label
       out_vv[0]       = vv;
       out_colors[0]   = bb;
       out_vv[1]       = null;
       out_colors[1]   = null;
       return;
     }
   }
 
   float[][] getLineArray(float[] vx, float[] vy) {
     float[] vvx = new float[(hi_idx-low_idx)+1];
     float[] vvy = new float[vvx.length];
 
     int ii = 0;
     for (int kk = low_idx; kk <= hi_idx; kk++) {
       vvx[ii] = vx[idx_array[kk]];
       vvy[ii] = vy[idx_array[kk]];
       ii++;
     }
     return new float[][] {vvx, vvy};
   }
 
   byte[][] getColorArray(byte[][] colors) {
     if (colors == null) return null;
     int clr_dim = colors.length;
     int clr_len = (hi_idx-low_idx)+1;
     byte[][] new_colors = new byte[clr_dim][clr_len];
 
     int ii = 0;
     for (int kk = low_idx; kk <= hi_idx; kk++) {
       for (int cc = 0; cc < clr_dim; cc++) {
         new_colors[cc][ii] = colors[cc][idx_array[kk]];
       }
       ii++;
     }
     return new_colors;
   }
 
   ContourStrip merge(ContourStrip c_strp)
   {
     if (this.lev_idx != c_strp.lev_idx) {
       System.out.println("Contour2D.ContourStrip.merge: !BIG ATTENTION!");
     }
 
     int new_length;
     int[] new_idx_array = null;
     int[] thisLo = new int[2];
     int[] thisHi = new int[2];
     int[] thatLo = new int[2];
     int[] thatHi = new int[2];
     
 
     thisLo[0] = idx_array[low_idx];
     thisLo[1] = idx_array[low_idx+1];
     thisHi[0] = idx_array[hi_idx];
     thisHi[1] = idx_array[hi_idx-1];
     thatLo[0] = c_strp.idx_array[c_strp.low_idx];
     thatLo[1] = c_strp.idx_array[c_strp.low_idx+1];
     thatHi[0] = c_strp.idx_array[c_strp.hi_idx];
     thatHi[1] = c_strp.idx_array[c_strp.hi_idx-1];
 
     if (((thisLo[0] == thatLo[0])||(thisLo[0] == thatLo[1]))||
         ((thisLo[1] == thatLo[0])||(thisLo[1] == thatLo[1])))
     {
       new_length = (hi_idx-low_idx)+1 +
                    (c_strp.hi_idx-c_strp.low_idx)+1;
       new_length -= 2;
       new_idx_array = new int[new_length];
 
       int ii = 0;
       for (int kk = hi_idx; kk >= low_idx; kk--) {
         new_idx_array[ii] = idx_array[kk];
         ii++;
       }
       for (int kk = c_strp.low_idx+2; kk <= c_strp.hi_idx; kk++) {
         new_idx_array[ii] = c_strp.idx_array[kk];
         ii++;
       }
     }
     else if (((thisLo[0] == thatHi[0])||(thisLo[0] == thatHi[1]))||
              ((thisLo[1] == thatHi[0])||(thisLo[1] == thatHi[1]))) 
     {
       new_length = (hi_idx-low_idx)+1 +
                    (c_strp.hi_idx-c_strp.low_idx)+1;
       new_length -= 2;
       new_idx_array = new int[new_length];
 
       int ii = 0;
       for (int kk = hi_idx; kk >= low_idx; kk--) {
         new_idx_array[ii] = idx_array[kk];
         ii++;
       }
       for (int kk = c_strp.hi_idx-2; kk >= c_strp.low_idx; kk--) {
         new_idx_array[ii] = c_strp.idx_array[kk];
         ii++;
       }
     }
     else if (((thisHi[0] == thatHi[0])||(thisHi[0] == thatHi[1]))||
              ((thisHi[1] == thatHi[0])||(thisHi[1] == thatHi[1])))
     {
       new_length = (hi_idx-low_idx)+1 +
                    (c_strp.hi_idx-c_strp.low_idx)+1;
       new_length -= 2;
       new_idx_array = new int[new_length];
      
       int ii = 0;
       for (int kk = low_idx; kk <= hi_idx; kk++) {
         new_idx_array[ii] = idx_array[kk];
         ii++;
       }
       for (int kk = c_strp.hi_idx-2; kk >= c_strp.low_idx; kk--) {
         new_idx_array[ii] = c_strp.idx_array[kk];
         ii++;
       }
     }
     else if (((thisHi[0] == thatLo[0])||(thisHi[0] == thatLo[1]))||
              ((thisHi[1] == thatLo[0])||(thisHi[1] == thatLo[1])))
     {
       new_length = (hi_idx-low_idx)+1 +
                    (c_strp.hi_idx-c_strp.low_idx)+1;
       new_length -= 2;
       new_idx_array = new int[new_length];
 
       int ii = 0;
       for (int kk = low_idx; kk <= hi_idx; kk++) {
         new_idx_array[ii] = idx_array[kk];
         ii++;
       }
       for (int kk = c_strp.low_idx+2; kk <= c_strp.hi_idx; kk++) {
         new_idx_array[ii] = c_strp.idx_array[kk];
         ii++;
       }
     }
     else {
       return null;
     }
 
     return new ContourStrip(new_idx_array, lev_idx, plot, css);
   }
   
   public String toString() {
     return "("+idx_array[low_idx]+","+idx_array[low_idx+1]+"), ("+
                                   idx_array[hi_idx]+","+idx_array[hi_idx-1]+")";
   }
 }
