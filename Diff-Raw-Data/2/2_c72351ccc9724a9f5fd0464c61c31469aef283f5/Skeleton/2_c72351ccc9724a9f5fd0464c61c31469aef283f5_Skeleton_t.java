 /*
  *  This file is part of the Haven & Hearth game client.
  *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
  *                     Björn Johannessen <johannessen.bjorn@gmail.com>
  *
  *  Redistribution and/or modification of this file is subject to the
  *  terms of the GNU Lesser General Public License, version 3, as
  *  published by the Free Software Foundation.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  Other parts of this source tree adhere to other copying
  *  rights. Please see the file `COPYING' in the root directory of the
  *  source tree for details.
  *
  *  A copy the GNU Lesser General Public License is distributed along
  *  with the source tree of which this file is a part in the file
  *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
  *  Software Foundation's website at <http://www.fsf.org/>, or write
  *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  *  Boston, MA 02111-1307 USA
  */
 
 package haven;
 
 import java.util.*;
 import javax.media.opengl.*;
 
 public class Skeleton {
     public final Map<String, Bone> bones = new HashMap<String, Bone>();
     public final Bone[] blist; /* Topologically sorted */
     public final Pose bindpose;
 
     public Skeleton(Collection<Bone> bones) {
 	Set<Bone> bset = new HashSet<Bone>(bones);
 	blist = new Bone[bones.size()];
 	int idx = 0;
 	for(Bone b : bones)
 	    this.bones.put(b.name, b);
 	while(!bset.isEmpty()) {
 	    boolean f = false;
 	    for(Iterator<Bone> i = bset.iterator(); i.hasNext();) {
 		Bone b = i.next();
 		boolean has;
 		if(b.parent == null) {
 		    has = true;
 		} else {
 		    has = false;
 		    for(Bone p : blist) {
 			if(p == b.parent) {
 			    has = true;
 			    break;
 			}
 		    }
 		}
 		if(has) {
 		    blist[b.idx = idx++] = b;
 		    i.remove();
 		    f = true;
 		}
 	    }
 	    if(!f)
 		throw(new RuntimeException("Cyclical bone hierarchy"));
 	}
 	bindpose = mkbindpose();
     }
     
     public static class Bone {
 	public String name;
 	public Coord3f ipos, irax;
 	public float irang;
 	public Bone parent;
 	public int idx;
 	
 	public Bone(String name, Coord3f ipos, Coord3f irax, float irang) {
 	    this.name = name;
 	    this.ipos = ipos;
 	    this.irax = irax;
 	    this.irang = irang;
 	}
     }
     
     private static float[] rotasq(float[] q, float[] axis, float angle) {
 	float m = (float)Math.sin(angle / 2.0);
 	q[0] = (float)Math.cos(angle / 2.0);
 	q[1] = m * axis[0]; q[2] = m * axis[1]; q[3] = m * axis[2];
 	return(q);
     }
     
     private static float[] qqmul(float[] d, float[] a, float[] b) {
 	float aw = a[0], ax = a[1], ay = a[2], az = a[3];
 	float bw = b[0], bx = b[1], by = b[2], bz = b[3];
 	d[0] = (aw * bw) - (ax * bx) - (ay * by) - (az * bz);
 	d[1] = (aw * bx) + (ax * bw) + (ay * bz) - (az * by);
 	d[2] = (aw * by) - (ax * bz) + (ay * bw) + (az * bx);
 	d[3] = (aw * bz) + (ax * by) - (ay * bx) + (az * bw);
 	return(d);
     }
     
     private static float[] vqrot(float[] d, float[] v, float[] q) {
 	float vx = v[0], vy = v[1], vz = v[2];
 	float qw = q[0], qx = q[1], qy = q[2], qz = q[3];
 	/* I dearly wonder how the JIT's common-subexpression
 	 * eliminator does on these. */
 	d[0] = (qw * qw * vx) + (2 * qw * qy * vz) - (2 * qw * qz * vy) + (qx * qx * vx) +
 	    (2 * qx * qy * vy) + (2 * qx * qz * vz) - (qz * qz * vx) - (qy * qy * vx);
 	d[1] = (2 * qx * qy * vx) + (qy * qy * vy) + (2 * qy * qz * vz) + (2 * qw * qz * vx) -
 	    (qz * qz * vy) + (qw * qw * vy) - (2 * qw * qx * vz) - (qx * qx * vy);
 	d[2] = (2 * qx * qz * vx) + (2 * qy * qz * vy) + (qz * qz * vz) - (2 * qw * qy * vx) -
 	    (qy * qy * vz) + (2 * qw * qx * vy) - (qx * qx * vz) + (qw * qw * vz);
 	return(d);
     }
     
     private static float[] vset(float[] d, float[] s) {
 	d[0] = s[0];
 	d[1] = s[1];
 	d[2] = s[2];
 	return(d);
     }
     
     private static float[] qset(float[] d, float[] s) {
 	d[0] = s[0];
 	d[1] = s[1];
 	d[2] = s[2];
 	d[3] = s[3];
 	return(d);
     }
     
     private static float[] vinv(float[] d, float[] s) {
 	d[0] = -s[0];
 	d[1] = -s[1];
 	d[2] = -s[2];
 	return(d);
     }
     
     private static float[] qinv(float[] d, float[] s) {
 	/* Assumes |s| = 1.0 */
 	d[0] = s[0];
 	d[1] = -s[1];
 	d[2] = -s[2];
 	d[3] = -s[3];
 	return(d);
     }
     
     private static float[] vvadd(float[] d, float[] a, float[] b) {
 	float ax = a[0], ay = a[1], az = a[2];
 	float bx = b[0], by = b[1], bz = b[2];
 	d[0] = ax + bx;
 	d[1] = ay + by;
 	d[2] = az + bz;
 	return(d);
     }
     
     private static float[] qqslerp(float[] d, float[] a, float[] b, float t) {
 	float aw = a[0], ax = a[1], ay = a[2], az = a[3];
 	float bw = b[0], bx = b[1], by = b[2], bz = b[3];
 	if((aw == bw) && (ax == bx) && (ay == by) && (az == bz))
 	    return(qset(d, a));
 	float cos = (aw * bw) + (ax * bx) + (ay * by) + (az * bz);
 	if(cos < 0) {
 	    bw = -bw; bx = -bx; by = -by; bz = -bz;
 	    cos = -cos;
 	}
 	float d0, d1;
 	if(cos > 0.9f) {
 	    /* Reasonable threshold? Is this function even critical
 	     * for performance? */
 	    d0 = 1.0f - t; d1 = t;
 	} else {
	    float da = (float)Math.acos(Utils.clip(cos, 0.0, 1.0));
 	    float nf = 1.0f / (float)Math.sin(da);
 	    d0 = (float)Math.sin((1.0f - t) * da) * nf;
 	    d1 = (float)Math.sin(t * da) * nf;
 	}
 	d[0] = (d0 * aw) + (d1 * bw);
 	d[1] = (d0 * ax) + (d1 * bx);
 	d[2] = (d0 * ay) + (d1 * by);
 	d[3] = (d0 * az) + (d1 * bz);
 	return(d);
     }
 
     public Pose mkbindpose() {
 	Pose p = new Pose();
 	for(int i = 0; i < blist.length; i++) {
 	    Bone b = blist[i];
 	    p.lpos[i][0] = b.ipos.x; p.lpos[i][1] = b.ipos.y; p.lpos[i][2] = b.ipos.z;
 	    rotasq(p.lrot[i], b.irax.to3a(), b.irang);
 	}
 	p.gbuild();
 	return(p);
     }
 	
     public class Pose {
 	public float[][] lpos, gpos;
 	public float[][] lrot, grot;
 	private Pose from = null;
 	public int seq = 0;
 	
 	private Pose() {
 	    int nb = blist.length;
 	    lpos = new float[nb][3];
 	    gpos = new float[nb][3];
 	    lrot = new float[nb][4];
 	    grot = new float[nb][4];
 	}
 	
 	public Pose(Pose from) {
 	    this();
 	    this.from = from;
 	}
 	
 	public Skeleton skel() {
 	    return(Skeleton.this);
 	}
 	
 	public void reset() {
 	    for(int i = 0; i < blist.length; i++) {
 		vset(lpos[i], from.lpos[i]);
 		qset(lrot[i], from.lrot[i]);
 	    }
 	}
 
 	public void gbuild() {
 	    int nb = blist.length;
 	    for(int i = 0; i < nb; i++) {
 		Bone b = blist[i];
 		if(b.parent == null) {
 		    gpos[i][0] = lpos[i][0];
 		    gpos[i][1] = lpos[i][1];
 		    gpos[i][2] = lpos[i][2];
 		    grot[i][0] = lrot[i][0];
 		    grot[i][1] = lrot[i][1];
 		    grot[i][2] = lrot[i][2];
 		    grot[i][3] = lrot[i][3];
 		} else {
 		    int pi = b.parent.idx;
 		    qqmul(grot[i], grot[pi], lrot[i]);
 		    vqrot(gpos[i], lpos[i], grot[pi]);
 		    vvadd(gpos[i], gpos[i], gpos[pi]);
 		}
 	    }
 	    seq++;
 	}
 	
 	public void boneoff(int bone, float[] offtrans) {
 	    /* It would be nice if these "new float"s get
 	     * stack-allocated. */
 	    float[] rot = new float[4], xlate = new float[3];
 	    rot = qqmul(rot, grot[bone], qinv(rot, bindpose.grot[bone]));
 	    xlate = vvadd(xlate, gpos[bone], vqrot(xlate, vinv(xlate, bindpose.gpos[bone]), rot));
 	    offtrans[3] = 0; offtrans[7] = 0; offtrans[11] = 0; offtrans[15] = 1;
 	    offtrans[12] = xlate[0]; offtrans[13] = xlate[1]; offtrans[14] = xlate[2];
 	    /* I must admit I don't /quite/ understand why the
 	     * rotation needs to be inverted... */
 	    float w = -rot[0], x = rot[1], y = rot[2], z = rot[3];
 	    float xw = x * w * 2, xx = x * x * 2, xy = x * y * 2, xz = x * z * 2;
 	    float yw = y * w * 2, yy = y * y * 2, yz = y * z * 2;
 	    float zw = z * w * 2, zz = z * z * 2;
 	    offtrans[ 0] = 1 - (yy + zz);
 	    offtrans[ 5] = 1 - (xx + zz);
 	    offtrans[10] = 1 - (xx + yy);
 	    offtrans[ 1] = xy - zw;
 	    offtrans[ 2] = xz + yw;
 	    offtrans[ 4] = xy + zw;
 	    offtrans[ 6] = yz - xw;
 	    offtrans[ 8] = xz - yw;
 	    offtrans[ 9] = yz + xw;
 	}
 	
 	public final Rendered debug = new Rendered() {
 		public void draw(GOut g) {
 		    GL gl = g.gl;
 		    g.matsel(null);
 		    gl.glDisable(GL.GL_LIGHTING);
 		    gl.glDisable(GL.GL_DEPTH_TEST);
 		    gl.glBegin(GL.GL_LINES);
 		    for(int i = 0; i < blist.length; i++) {
 			if(blist[i].parent != null) {
 			    int pi = blist[i].parent.idx;
 			    gl.glColor3f(1.0f, 0.0f, 0.0f);
 			    gl.glVertex3f(gpos[pi][0], gpos[pi][1], gpos[pi][2]);
 			    gl.glColor3f(0.0f, 1.0f, 0.0f);
 			    gl.glVertex3f(gpos[i][0], gpos[i][1], gpos[i][2]);
 			}
 		    }
 		    gl.glEnd();
 		    gl.glEnable(GL.GL_LIGHTING);
 		    gl.glEnable(GL.GL_DEPTH_TEST);
 		}
 	    
 		public boolean setup(RenderList rl) {
 		    return(true);
 		}
 	    };
     }
     
     public class PoseMod {
 	public float[][] lpos, lrot;
 	
 	public PoseMod() {
 	    int nb = blist.length;
 	    lpos = new float[nb][3];
 	    lrot = new float[nb][4];
 	    for(int i = 0; i < nb; i++)
 		lrot[i][0] = 1;
 	}
 	
 	public void reset() {
 	    for(int i = 0; i < blist.length; i++) {
 		lpos[i][0] = 0; lpos[i][1] = 0; lpos[i][2] = 0;
 		lrot[i][0] = 1; lrot[i][1] = 0; lrot[i][2] = 0; lrot[i][3] = 0;
 	    }
 	}
 	
 	public void rot(int bone, float ang, float ax, float ay, float az) {
 	    float[] x = {ax, ay, az};
 	    qqmul(lrot[bone], lrot[bone], rotasq(new float[4], x, ang));
 	}
 
 	public void apply(Pose p) {
 	    for(int i = 0; i < blist.length; i++) {
 		vvadd(p.lpos[i], p.lpos[i], lpos[i]);
 		qqmul(p.lrot[i], p.lrot[i], lrot[i]);
 	    }
 	}
     }
     
     public static class Res extends Resource.Layer {
 	public final Skeleton s;
 	
 	public Res(Resource res, byte[] buf) {
 	    res.super();
 	    Map<String, Bone> bones = new HashMap<String, Bone>();
 	    Map<Bone, String> pm = new HashMap<Bone, String>();
 	    int[] off = {0};
 	    while(off[0] < buf.length) {
 		String bnm = Utils.strd(buf, off);
 		Coord3f pos = new Coord3f((float)Utils.floatd(buf, off[0]), (float)Utils.floatd(buf, off[0] + 5), (float)Utils.floatd(buf, off[0] + 10));
 		off[0] += 15;
 		Coord3f rax = new Coord3f((float)Utils.floatd(buf, off[0]), (float)Utils.floatd(buf, off[0] + 5), (float)Utils.floatd(buf, off[0] + 10)).norm();
 		off[0] += 15;
 		float rang = (float)Utils.floatd(buf, off[0]);
 		off[0] += 5;
 		String bp = Utils.strd(buf, off);
 		Bone b = new Bone(bnm, pos, rax, rang);
 		if(bones.put(bnm, b) != null)
 		    throw(new RuntimeException("Duplicate bone name: " + b.name));
 		pm.put(b, bp);
 	    }
 	    for(Bone b : bones.values()) {
 		String bp = pm.get(b);
 		if(bp.length() == 0) {
 		    b.parent = null;
 		} else {
 		    if((b.parent = bones.get(bp)) == null)
 			throw(new Resource.LoadException("Parent bone " + bp + " not found for " + b.name, getres()));
 		}
 	    }
 	    s = new Skeleton(bones.values());
 	}
 	
 	public void init() {}
     }
 }
