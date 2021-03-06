 /* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
    modeler, Finite element mesher, Plugin architecture.
 
     Copyright (C) 2004,2005,2006, by EADS CRC
     Copyright (C) 2007, by EADS France
 
     This library is free software; you can redistribute it and/or
     modify it under the terms of the GNU Lesser General Public
     License as published by the Free Software Foundation; either
     version 2.1 of the License, or (at your option) any later version.
 
     This library is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
     Lesser General Public License for more details.
 
     You should have received a copy of the GNU Lesser General Public
     License along with this library; if not, write to the Free Software
     Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */
 
 package org.jcae.mesh.amibe.ds;
 
 import java.util.Collection;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.Set;
 import java.util.NoSuchElementException;
 import org.jcae.mesh.amibe.metrics.Matrix3D;
 import org.apache.log4j.Logger;
 
 /*
  * This class is derived from Jonathan Richard Shewchuk's work
  * on AbstractTriangle, see
  *       http://www.cs.cmu.edu/~quake/triangle.html
  * His data structure is very compact, and similar ideas were
  * developed here, but due to Java constraints, this version is a
  * little bit less efficient than its C counterpart.
  *
  * Geometrical primitives and basic routines have been written from
  * scratch, but are in many cases very similar to those defined by
  * Shewchuk since data structures are almost equivalent and there
  * are few ways to achieve the same operations.
  *
  * Other ideas come from Bamg, written by Frederic Hecht
  *       http://www-rocq1.inria.fr/gamma/cdrom/www/bamg/eng.htm
  */
 
 /**
  * A handle to abstract edge instances.
  *
  * <p>
  *   Jonathan Richard Shewchuk
  *   <a href="http://www.cs.cmu.edu/~quake/triangle.html">explains</a>
  *   why triangle-based data structures are more efficient than their
  *   edge-based counterparts.  But mesh operations make heavy use of edges,
  *   and informations about adges are not stored in this data structure in
  *   order to be compact.
  * </p>
  *
  * <p>
  *   A triangle is composed of three edges, so a triangle and a number
  *   between 0 and 2 can represent an edge.  This <code>VirtualHalfEdge</code>
  *   class plays this role, it defines an <em>oriented triangle</em>, or
  *   in other words an oriented edge.  Instances of this class are tied to
  *   their underlying {@link AbstractTriangle} instances, so modifications are not
  *   local to this class!
  * </p>
  *
  * <p>
  *   The main goal of this class is to ease mesh traversal.
  *   Consider the <code>ot</code> {@link VirtualHalfEdge} with a null localNumber of
  *   {@link AbstractTriangle} <code>t</code> below.
  * </p>
  * <pre>
  *                        V2
  *     V5 _________________,_________________ V3
  *        \    &lt;----      / \     &lt;----     /
  *         \     0     _ /   \      1    _ /
  *          \\  t0     ///  /\\\   t1    //
  *           \\1     2///1   0\\\2     0//   t.vertex = { V0, V1, V2 }
  *            \V     //V   t   \\V     //   t0.vertex = { V2, V1, V3 }
  *             \     /           \     /    t1.vertex = { V5, V0, V2 }
  *              \   /      2      \   /     t2.vertex = { V0, V4, V1 }
  *               \ /     ----&gt;     \ /
  *             V0 +-----------------+ V1
  *                 \     &lt;----     /
  *                  \      1    _ /
  *                   \\   t2    //
  *                    \\2     0//
  * </pre>
  * The following methods can be applied to <code>ot</code>:
  * <pre>
  *    ot.next();        // Moves (t,0) to (t,1)
  *    ot.prev();        // Moves (t,0) to (t,2)
  *    ot.sym();         // Moves (t,0) to (t1,2)
  *    ot.nextOrigin();  // Moves (t,0) to (t2,1)
  * </pre>
  * For convenience, following methods are also defined in VirtualHalfEdge2D:
  * <pre>
  *    ot.prevOrigin();  // Moves (t,0) to (t1,0)
  *    ot.nextDest();    // Moves (t,0) to (t1,1)
  *    ot.prevDest();    // Moves (t,0) to (t0,2)
  *    ot.nextApex();    // Moves (t,0) to (t0,0)
  *    ot.prevApex();    // Moves (t,0) to (t2,0)
  * </pre>
  *
  * <p>
  * When an <code>VirtualHalfEdge</code> is traversing the mesh, its reference
  * is not modified, but its instance variables are updated.  In order
  * to prevent object allocations, we try to reuse <code>VirtualHalfEdge</code>
  * objects as much as we can.
  * </p>
  */
 public class VirtualHalfEdge extends AbstractHalfEdge
 {
 	private static Logger logger = Logger.getLogger(VirtualHalfEdge.class);
 	
 	private static final int [] next3 = { 1, 2, 0 };
 	private static final int [] prev3 = { 2, 0, 1 };
 	
 	private final double [] tempD = new double[3];
 	private final double [] tempD1 = new double[3];
 	private final double [] tempD2 = new double[3];
 	
 	//  Complex algorithms require several VirtualHalfEdge, they are
 	//  allocated here to prevent allocation/deallocation overhead.
 	private static VirtualHalfEdge [] work = new VirtualHalfEdge[4];
 	static {
 		for (int i = 0; i < 4; i++)
 			work[i] = new VirtualHalfEdge();
 	}
 	
 	/*
 	 * Vertices can be accessed through
 	 *        origin = tri.vertex[next3[localNumber]]
 	 *   destination = tri.vertex[prev3[localNumber]]
 	 *          apex = tri.vertex[localNumber]
 	 * Adjacent triangle is tri.adj[localNumber].tri and its localNumber
 	 * is ((tri.adjPos[0] >> (2*localNumber)) & 3)
 	 */
 	protected Triangle tri = null;
 	protected int localNumber = 0;
 	protected int attributes = 0;
 	
 	// Section: constructors
 	
 	/**
 	 * Sole constructor.
 	 */
 	public VirtualHalfEdge()
 	{
 	}
 	
 	/**
 	 * Create an object to handle data about a triangle.
 	 *
 	 * @param t  geometrical triangle.
 	 * @param o  a number between 0 and 2 determining an edge.
 	 */
 	public VirtualHalfEdge(Triangle t, int o)
 	{
 		tri = t;
 		localNumber = o;
 		pullAttributes();
 	}
 	
 	// Section: accessors
 	
 	/**
 	 * Return the triangle tied to this object.
 	 *
 	 * @return the triangle tied to this object.
 	 */
 	@Override
 	public final Triangle getTri()
 	{
 		return tri;
 	}
 	
 	/**
 	 * Return the edge local number.
 	 *
 	 * @return the edge local number.
 	 */
 	@Override
 	public final int getLocalNumber()
 	{
 		return localNumber;
 	}
 	
 	/**
 	 * Set the triangle tied to this object, and resets localNumber.
 	 *
 	 * @param t  the triangle tied to this object.
 	 */
 	public final void bind(Triangle t)
 	{
 		tri = t;
 		localNumber = 0;
 		pullAttributes();
 	}
 	
 	/**
 	 * Set the triangle tied to this object, and the localNumber.
 	 *
 	 * @param t  the triangle tied to this object.
 	 * @param l  the local number.
 	 */
 	public final void bind(Triangle t, int l)
 	{
 		tri = t;
 		localNumber = l;
 		pullAttributes();
 	}
 	
 	// Section: attributes handling
 	
 	/**
 	 * Check if some attributes of this oriented triangle are set.
 	 *
 	 * @param attr  the attributes to check
 	 * @return <code>true</code> if this VirtualHalfEdge has all these
 	 * attributes set, <code>false</code> otherwise.
 	 */
 	@Override
 	public final boolean hasAttributes(int attr)
 	{
 		return (attributes & attr) != 0;
 	}
 	
 	/**
 	 * Set attributes of this oriented triangle.
 	 *
 	 * @param attr  the attribute of this oriented triangle.
 	 */
 	@Override
 	public final void setAttributes(int attr)
 	{
 		attributes |= attr;
 		pushAttributes();
 	}
 	
 	/**
 	 * Reset attributes of this oriented triangle.
 	 *
 	 * @param attr   the attributes of this oriented triangle to clear out.
 	 */
 	@Override
 	public final void clearAttributes(int attr)
 	{
 		attributes &= ~attr;
 		pushAttributes();
 	}
 	
 	// Adjust tri.adjPos after attributes is modified.
 	protected final void pushAttributes()
 	{
 		tri.setEdgeAttributes(localNumber, attributes);
 	}
 	
 	// Adjust attributes after tri.adjPos is modified.
 	protected final void pullAttributes()
 	{
 		attributes = tri.getEdgeAttributes(localNumber);
 	}
 	
 	/**
 	 * Checks whether an edge can be modified.
 	 *
 	 * @return <code>false</code> if edge is a boundary or outside the mesh,
 	 * <code>true</code> otherwise.
 	 */
 	public final boolean isMutable()
 	{
 		return !(hasAttributes(BOUNDARY) || hasAttributes(NONMANIFOLD) || hasAttributes(OUTER));
 	}
 	
 	// Section: geometrical primitives
 	
 	//  These geometrical primitives have 3 signatures:
 	//      fct()     transforms current object.
 	//      fct(that) copies current instance into 'that' and transforms it
 	//      fct(this, that)   applies fct to 'this' and stores result
 	//                        in an already allocated object 'that'.
 	//  This is definitely not an OO approach, but it is much more
 	//  efficient by preventing useless memory allocations.
 	//  They do not return any value to make clear that calling
 	//  these routines requires extra care.
 	
 	/**
 	 * Move to symmetric edge.
 	 * Copy into another instance, move it to its symmetric edge and
 	 * return this copy.
 	 *
 	 * @param  that  instance where transformed edge is stored
 	 * @return   argument after its transformation
 	 */
 	@Override
 	public final AbstractHalfEdge sym(AbstractHalfEdge that)
 	{
 		VirtualHalfEdge dest = (VirtualHalfEdge) that;
 		dest.tri = (Triangle) tri.getAdj(localNumber);
 		dest.localNumber = tri.getAdjLocalNumber(localNumber);
 		dest.pullAttributes();
 		return dest;
 	}
 	
 	/**
 	 * Move to symmetric edge.
 	 * @return  current instance after its transformation
 	 */
 	@Override
 	public final AbstractHalfEdge sym()
 	{
 		int neworient = tri.getAdjLocalNumber(localNumber);
 		tri = (Triangle) tri.getAdj(localNumber);
 		localNumber = neworient;
 		pullAttributes();
 		return this;
 	}
 	
 	/**
 	 * Move counterclockwise to following edge.
 	 * Copy into another instance, move it counterclockwise to
 	 * following edge and return this copy.
 	 *
 	 * @param  that  instance where transformed edge is stored
 	 * @return   argument after its transformation
 	 */
 	@Override
 	public final AbstractHalfEdge next(AbstractHalfEdge that)
 	{
 		VirtualHalfEdge dest = (VirtualHalfEdge) that;
 		dest.tri = tri;
 		dest.localNumber = next3[localNumber];
 		dest.pullAttributes();
 		return dest;
 	}
 	
 	/**
 	 * Move counterclockwise to following edge.
 	 * @return  current instance after its transformation
 	 */
 	@Override
 	public final AbstractHalfEdge next()
 	{
 		localNumber = next3[localNumber];
 		pullAttributes();
 		return this;
 	}
 	
 	/**
 	 * Move counterclockwise to previous edge.
 	 * Copy into another instance, move it counterclockwise to
 	 * previous edge and return this copy.
 	 *
 	 * @param  that  instance where transformed edge is stored
 	 * @return   argument after its transformation
 	 */
 	@Override
 	public final AbstractHalfEdge prev(AbstractHalfEdge that)
 	{
 		VirtualHalfEdge dest = (VirtualHalfEdge) that;
 		dest.tri = tri;
 		dest.localNumber = prev3[localNumber];
 		dest.pullAttributes();
 		return dest;
 	}
 	
 	/**
 	 * Move counterclockwise to previous edge.
 	 * @return  current instance after its transformation
 	 */
 	@Override
 	public final AbstractHalfEdge prev()
 	{
 		localNumber = prev3[localNumber];
 		pullAttributes();
 		return this;
 	}
 	
 	/**
 	 * Move counterclockwise to the following edge which has the same origin.
 	 * Copy into another instance, move it counterclockwise to
 	 * the following edge which has the same origin and return this copy.
 	 *
 	 * @param  that  instance where transformed edge is stored
 	 * @return   argument after its transformation
 	 */
 	@Override
 	public final AbstractHalfEdge nextOrigin(AbstractHalfEdge that)
 	{
 		return prev(that).sym();
 	}
 	
 	/**
 	 * Move counterclockwise to the following edge with same origin.
 	 * @return  current instance after its transformation
 	 */
 	@Override
 	public final AbstractHalfEdge nextOrigin()
 	{
 		return prev().sym();
 	}
 	
 	/**
 	 * Move counterclockwise to the following edge with same origin.
 	 * If a boundary is reached, loop backward until another
 	 * boundary is found and start again from there.
 	 * Note: outer triangles are taken into account in this loop, because
 	 * this is sometimes needed, as in VirtualHalfEdge2D.removeDegenerated().
 	 * They have to be explicitly filtered out by testing hasAttributes(OUTER).
 	 */
 	@Override
 	public final AbstractHalfEdge nextOriginLoop()
 	{
 		if (hasAttributes(OUTER) && hasAttributes(BOUNDARY | NONMANIFOLD))
 		{
 			// Loop clockwise to another boundary
 			// and start again from there.
 			do
 			{
 				sym();
 				next();
 			}
 			while (!hasAttributes(OUTER));
 		}
 		else
 			nextOrigin();
 		return this;
 	}
 	
 	// Static methods for VirtualHalfEdge instances, only the most useful methods are defined
 	
 	/**
 	 * Copy a <code>VirtualHalfEdge</code> instance into another <code>VirtualHalfEdge</code>
 	 * instance.
 	 *
 	 * @param src   <code>VirtualHalfEdge</code> being duplicated
 	 * @param dest  already allocated <code>VirtualHalfEdge</code> where data are
 	 *              copied
 	 */
 	protected static final void copyOTri(VirtualHalfEdge src, VirtualHalfEdge dest)
 	{
 		dest.tri = src.tri;
 		dest.localNumber = src.localNumber;
 		dest.attributes = src.attributes;
 	}
 	
 	/**
 	 * Copy a <code>VirtualHalfEdge</code> instance and move to its symmetric edge.
 	 *
 	 * @param o     source <code>VirtualHalfEdge</code>
 	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
 	 *              copied
 	 */
 	protected static final void symOTri(VirtualHalfEdge o, VirtualHalfEdge that)
 	{
 		that.tri = (Triangle) o.tri.getAdj(o.localNumber);
 		that.localNumber = o.tri.getAdjLocalNumber(o.localNumber);
 		that.pullAttributes();
 	}
 	
 	/**
 	 * Copy a <code>VirtualHalfEdge</code> instance and move it counterclockwise to
 	 * following edge.
 	 *
 	 * @param o     source <code>VirtualHalfEdge</code>
 	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
 	 *              copied
 	 */
 	protected static final void nextOTri(VirtualHalfEdge o, VirtualHalfEdge that)
 	{
 		that.tri = o.tri;
 		that.localNumber = next3[o.localNumber];
 		that.pullAttributes();
 	}
 	
 	/**
 	 * Copy a <code>VirtualHalfEdge</code> instance and move it counterclockwise to
 	 * previous edge.
 	 *
 	 * @param o     source <code>VirtualHalfEdge</code>
 	 * @param that  already allocated <code>VirtualHalfEdge</code> where data are
 	 *              copied
 	 */
 	protected static final void prevOTri(VirtualHalfEdge o, VirtualHalfEdge that)
 	{
 		that.tri = o.tri;
 		that.localNumber = prev3[o.localNumber];
 		that.pullAttributes();
 	}
 	
 	// Section: vertex handling
 	
 	/**
 	 * Returns the start vertex of this edge.
 	 *
 	 * @return the start vertex of this edge.
 	 */
 	@Override
 	public Vertex origin()
 	{
 		return tri.vertex[next3[localNumber]];
 	}
 	
 	/**
 	 * Returns the end vertex of this edge.
 	 *
 	 * @return the end vertex of this edge.
 	 */
 	@Override
 	public Vertex destination()
 	{
 		return tri.vertex[prev3[localNumber]];
 	}
 	
 	/**
 	 * Returns the apex of this edge.
 	 *
 	 * @return the apex of this edge.
 	 */
 	@Override
 	public Vertex apex()
 	{
 		return tri.vertex[localNumber];
 	}
 	
 	//  The following 3 methods change the underlying triangle.
 	//  So they also modify all VirtualHalfEdge bound to this one.
 	/**
 	 * Sets the start vertex of this edge.
 	 *
 	 * @param v  the start vertex of this edge.
 	 */
 	public final void setOrigin(Vertex v)
 	{
 		tri.vertex[next3[localNumber]] = v;
 	}
 	
 	/**
 	 * Sets the end vertex of this edge.
 	 *
 	 * @param v  the end vertex of this edge.
 	 */
 	public final void setDestination(Vertex v)
 	{
 		tri.vertex[prev3[localNumber]] = v;
 	}
 	
 	/**
 	 * Sets the apex of this edge.
 	 *
 	 * @param v  the apex of this edge.
 	 */
 	public final void setApex(Vertex v)
 	{
 		tri.vertex[localNumber] = v;
 	}
 	
 	// Section: adjacency
 	
 	/**
 	 * Sets adjacency relations between two triangles.
 	 *
 	 * @param sym  the triangle bond to this one.
 	 */
 	@Override
 	public final void glue(AbstractHalfEdge sym)
 	{
 		VHglue((VirtualHalfEdge) sym);
 	}
 	private void VHglue(VirtualHalfEdge sym)
 	{
 		tri.setAdj(localNumber, sym.tri);
 		tri.setAdjLocalNumber(localNumber, sym.localNumber);
 		sym.tri.setAdj(sym.localNumber, tri);
 		sym.tri.setAdjLocalNumber(sym.localNumber, localNumber);
 	}
 	
 	/**
 	 * Gets adjacency relation for an edge
 	 *
 	 * @return the triangle bond to this one if this edge is manifold, or an Object otherwise.
 	 */
 	@Override
 	public final Object getAdj()
 	{
 		return tri.getAdj(localNumber);
 	}
 	
 	/**
 	 * Gets adjacency list for non-manifold edges. 
 	 */
 	@SuppressWarnings("unchecked")
 	@Override
 	public final Map<Triangle, Integer> getAdjNonManifold()
 	{
 		assert hasAttributes(NONMANIFOLD) && !hasAttributes(OUTER);
 		// By convention, adjacency list is stored in a virtual triangle.
 		// Save current state to restore it before returning.
 		Triangle t = tri;
 		int l = localNumber;
 		sym();
 		next();
 		Map<Triangle, Integer> ret = (Map<Triangle, Integer>) tri.getAdj(localNumber);
 		bind(t, l);
 		return ret;
 	}
 
 	/**
 	 * Sets adjacency relation for an edge
 	 *
 	 * @param link  the triangle bond to this one if this edge is manifold, or an Object otherwise.
 	 */
 	@Override
 	public final void setAdj(Object link)
 	{
 		tri.setAdj(localNumber, link);
 	}
 	
 	// Section: 3D geometrical routines
 	
 	/**
 	 * Compute the normal of an edge, in the triangle plane.
 	 * This vector is not normalized, it has the same length as
 	 * this edge.  The result is stored in the tempD temporary array.
 	 * @see #getTempVector
 	 * @return the area of this triangle.
 	 * Warning: this method uses tempD, tempD1 and tempD2 temporary arrays.
 	 */
 	public double computeNormal3DT()
 	{
 		double [] p0 = origin().getUV();
 		double [] p1 = destination().getUV();
 		double [] p2 = apex().getUV();
 		tempD1[0] = p1[0] - p0[0];
 		tempD1[1] = p1[1] - p0[1];
 		tempD1[2] = p1[2] - p0[2];
 		tempD[0] = p2[0] - p0[0];
 		tempD[1] = p2[1] - p0[1];
 		tempD[2] = p2[2] - p0[2];
 		Matrix3D.prodVect3D(tempD1, tempD, tempD2);
 		double norm = Matrix3D.norm(tempD2);
 		if (norm != 0.0)
 		{
 			tempD2[0] /= norm;
 			tempD2[1] /= norm;
 			tempD2[2] /= norm;
 		}
 		Matrix3D.prodVect3D(tempD1, tempD2, tempD);
 		return 0.5*norm;
 	}
 	
 	/**
 	 * Compute the normal of this triangle.  The result is stored in
 	 * the tempD temporary array.
 	 * @see #getTempVector
 	 * @return the area of this triangle.
 	 * Warning: this method uses tempD, tempD1 and tempD2 temporary arrays.
 	 */
 	public double computeNormal3D()
 	{
 		double [] p0 = origin().getUV();
 		double [] p1 = destination().getUV();
 		double [] p2 = apex().getUV();
 		tempD1[0] = p1[0] - p0[0];
 		tempD1[1] = p1[1] - p0[1];
 		tempD1[2] = p1[2] - p0[2];
 		tempD2[0] = p2[0] - p0[0];
 		tempD2[1] = p2[1] - p0[1];
 		tempD2[2] = p2[2] - p0[2];
 		Matrix3D.prodVect3D(tempD1, tempD2, tempD);
 		double norm = Matrix3D.norm(tempD);
 		if (norm != 0.0)
 		{
 			tempD[0] /= norm;
 			tempD[1] /= norm;
 			tempD[2] /= norm;
 		}
 		return 0.5*norm;
 	}
 	
 	/**
 	 * Return the area of this triangle.
 	 * @return the area of this triangle.
 	 * Warning: this method uses tempD, tempD1 and tempD2 temporary arrays.
 	 */
 	@Override
 	public double area()
 	{
 		double [] p0 = origin().getUV();
 		double [] p1 = destination().getUV();
 		double [] p2 = apex().getUV();
 		tempD1[0] = p1[0] - p0[0];
 		tempD1[1] = p1[1] - p0[1];
 		tempD1[2] = p1[2] - p0[2];
 		tempD2[0] = p2[0] - p0[0];
 		tempD2[1] = p2[1] - p0[1];
 		tempD2[2] = p2[2] - p0[2];
 		Matrix3D.prodVect3D(tempD1, tempD2, tempD);
 		return 0.5 * Matrix3D.norm(tempD);
 	}
 	
 	/**
 	 * Return the temporary array TempD.
 	 */
 	public double [] getTempVector()
 	{
 		return tempD;
 	}
 	
 	// Section: algorithms
 	
 	/**
 	 * Checks the dihedral angle of an edge.
 	 *
 	 * @param minCos  if the dot product of the normals to adjacent
 	 *    triangles is lower than monCos, then <code>-1.0</code> is
 	 *    returned.
 	 * @return the minimum quality of the two trianglles generated
 	 *    by swapping this edge.
 	 */
 	public final double checkSwap3D(double minCos)
 	{
 		double invalid = -1.0;
 		// Check if there is an adjacent edge
 		if (hasAttributes(OUTER | BOUNDARY | NONMANIFOLD))
 			return invalid;
 		// Check for coplanarity
 		symOTri(this, work[0]);
 		computeNormal3D();
 		double [] n1 = getTempVector();
 		work[0].computeNormal3D();
 		double [] n2 = work[0].getTempVector();
 		if (Matrix3D.prodSca(n1, n2) < minCos)
 			return invalid;
 		// Check for quality improvement
 		Vertex o = origin();
 		Vertex d = destination();
 		Vertex a = apex();
 		Vertex n = work[0].apex();
 		// Check for inverted triangles
 		o.outer3D(n, a, n2);
 		double s3 = 0.5 * Matrix3D.prodSca(n1, n2);
 		if (s3 <= 0.0)
 			return invalid;
 		d.outer3D(a, n, n2);
 		double s4 = 0.5 * Matrix3D.prodSca(n1, n2);
 		if (s4 <= 0.0)
 			return invalid;
 		double p1 = o.distance3D(d) + d.distance3D(a) + a.distance3D(o);
 		double s1 = area();
 		double p2 = d.distance3D(o) + o.distance3D(n) + n.distance3D(d);
 		double s2 = work[0].area();
 		// No need to multiply by 12.0 * Math.sqrt(3.0)
 		double Qbefore = Math.min(s1/p1/p1, s2/p2/p2);
 		
 		double p3 = o.distance3D(n) + n.distance3D(a) + a.distance3D(o);
 		double p4 = d.distance3D(a) + a.distance3D(n) + n.distance3D(d);
 		double Qafter = Math.min(s3/p3/p3, s4/p4/p4);
 		if (Qafter > Qbefore)
 			return Qafter;
 		return invalid;
 	}
 	
 	/**
 	 * Swaps an edge.
 	 *
 	 * This routine swaps an edge (od) to (na), updates
 	 * adjacency relations and backward links between vertices and
 	 * triangles.  Current object is transformed from (oda) to (ona)
 	 * and not (nao), because this helps turning around o, eg.
 	 * at the end of {@link org.jcae.mesh.amibe.patch.VirtualHalfEdge2D#split3}.
 	 *        
 	 *          d                    d
 	 *          .                    .
 	 *         /|\                  / \
 	 *        / | \                /   \   
 	 *       /  |  \              /     \
 	 *    a +   |   + n  ---&gt;  a +-------+ n
 	 *       \  |  /              \     /
 	 *        \ | /                \   /
 	 *         \|/                  \ /
 	 *          '                    '
 	 *          o                    o
 	 * @return swapped edge
 	 * @throws IllegalArgumentException if edge is on a boundary or belongs
 	 * to an outer triangle.
 	 * @see Mesh#edgeSwap
 	 */
 	@Override
 	protected final AbstractHalfEdge swap()
 	{
 		VHswap();
 		return this;
 	}
 	private final void VHswap()
 	{
 		if (hasAttributes(OUTER | BOUNDARY | NONMANIFOLD))
 			throw new IllegalArgumentException("Cannot swap "+this);
 		Vertex o = origin();
 		Vertex d = destination();
 		Vertex a = apex();
 		/*
 		 *            d                    d
 		 *            .                    .
 		 *           /|\                  / \
 		 *       a1 / | \ a4         a1  /   \ a4
 		 *         /  |  \              /     \
 		 *      a +   |   + n  --->  a +-------+ n
 		 *         \  |  /              \     /
 		 *       a2 \ | / a3         a2  \   / a3
 		 *           \|/                  \ /
 		 *            '                    '
 		 *            o                    o
 		 */
 		// T1 = (oda)  --> (ona)
 		// T2 = (don)  --> (dan)
 		copyOTri(this, work[0]);        // (oda)
 		symOTri(this, work[1]);         // (don)
 		symOTri(this, work[2]);         // (don)
 		Vertex n = work[1].apex();
 		//  Clear SWAPPED flag for all edges of the 2 triangles
 		for (int i = 0; i < 3; i++)
 		{
 			work[0].clearAttributes(SWAPPED);
 			work[1].clearAttributes(SWAPPED);
 			work[0].next();
 			work[1].next();
 		}
 		work[1].next();                 // (ond)
 		int attr3 = work[1].attributes;
		if (work[1].hasAttributes(NONMANIFOLD))
		{
			// work[1] is listed in adjacency list and
			// has to be replaced by work[0]
			work[1].replaceEdgeLinks(work[0]);
		}
 		work[1].sym();                  // a3 = (no*)
 		work[1].VHglue(work[0]);
 		work[0].attributes = attr3;
 		work[0].pushAttributes();
 		work[0].next();                 // (dao)
 		copyOTri(work[0], work[1]);     // (dao)
 		int attr1 = work[1].attributes;
		if (work[1].hasAttributes(NONMANIFOLD))
		{
			// work[1] is listed in adjacency list and
			// has to be replaced by work[2]
			work[1].replaceEdgeLinks(work[2]);
		}
 		work[0].sym();                  // a1 = (ad*)
 		work[2].VHglue(work[0]);
 		work[2].attributes = attr1;
 		work[2].pushAttributes();
 		work[2].next();                 // (ond)
 		work[2].VHglue(work[1]);
 		//  Mark new edge
 		work[1].attributes = 0;
 		work[2].attributes = 0;
 		work[1].setAttributes(SWAPPED);
 		work[2].setAttributes(SWAPPED);
 		//  Adjust vertices
 		work[2].setOrigin(a);           // (and)
 		work[1].setOrigin(n);           // (nao)
 		//  Fix links to triangles
 		o.setLink(tri);
 		d.setLink(work[2].tri);
 		pullAttributes();
 	}
 	
 	/**
 	 * Checks that triangles are not inverted if origin vertex is moved.
 	 *
 	 * @param newpt  the new position to be checked.
 	 * @return <code>false</code> if the new position produces
 	 *    an inverted triangle, <code>true</code> otherwise.
 	 * Warning: this method uses work[0] and work[1] temporary arrays.
 	 */
 	@Override
 	public final boolean checkNewRingNormals(double [] newpt)
 	{
 		Vertex o = origin();
 		if (o.getLink() instanceof Triangle)
 			return checkNewRingNormalsSameFan(newpt, null, null);
 		for (Triangle start: (Triangle []) o.getLink())
 		{
 			work[1].bind(start);
 			if (work[1].destination() == o)
 				work[1].next();
 			else if (work[1].apex() == o)
 				work[1].prev();
 			assert work[1].origin() == o;
 			if (!work[1].checkNewRingNormalsSameFan(newpt, null, null))
 				return false;
 		}
 		return true;
 	}
 	
 	/*
 	 * Warning: this method uses work[0] temporary array.
 	 */
 	private final boolean checkNewRingNormalsSameFan(double [] newpt, Triangle t1, Triangle t2)
 	{
 		Vertex d = destination();
 		copyOTri(this, work[0]);
 		do
 		{
 			if (work[0].tri != t1 && work[0].tri != t2 && !work[0].hasAttributes(OUTER))
 			{
 				double [] x1 = work[0].destination().getUV();
 				work[0].next();
 				double area  = work[0].computeNormal3DT();
 				double [] nu = work[0].getTempVector();
 				work[0].prev();
 				for (int i = 0; i < 3; i++)
 					tempD1[i] = newpt[i] - x1[i];
 				// Two triangles are removed when an edge is contracted.
 				// So normally triangle areas should increase.  If they
 				// decrease significantly, there may be a problem.
 				if (Matrix3D.prodSca(tempD1, nu) >= - area)
 					return false;
 			}
 			work[0].nextOriginLoop();
 		}
 		while (work[0].destination() != d);
 		return true;
 	}
 	
 	/**
 	 * Check whether an edge can be contracted.
 	 *
 	 * @param n the resulting vertex
 	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise.
 	 * @see Mesh#canCollapseEdge
 	 * Warning: this method uses work[0], work[1] and work[2] temporary arrays.
 	 */
 	@Override
 	protected final boolean canCollapse(AbstractVertex n)
 	{
 		// Be consistent with collapse()
 		if (hasAttributes(OUTER))
 			return false;
 		if (logger.isDebugEnabled())
 			logger.debug("can contract? ("+origin()+" "+destination()+") into "+n);
 		double [] xn = ((Vertex) n).getUV();
 		if ((origin().getLink() instanceof Triangle) && (destination().getLink() instanceof Triangle))
 		{
 			// Mesh is locally manifold.  This is the most common
 			// case, do not create an HashSet to store only two
 			// triangles.
 			Triangle t1 = tri;
 			symOTri(this, work[1]);
 			Triangle t2 = work[1].tri;
 			// Check that origin vertex can be moved
 			if (!checkNewRingNormalsSameFan(xn, t1, t2))
 				return false;
 			// Check that destination vertex can be moved
 			if (!work[1].checkNewRingNormalsSameFan(xn, t1, t2))
 				return false;
 			//  Topology check.
 			return canCollapseTopology();
 		}
 
 		// At least one vertex is non manifold.  Store all triangles
 		// which will be removed in an HashSet so that they are
 		// ignored when checking for degenerated triangles.
 		Collection<Triangle> ignored = new HashSet<Triangle>();
 		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
 		{
 			VirtualHalfEdge f = (VirtualHalfEdge) it.next();
 			ignored.add(f.tri);
 			symOTri(f, work[1]);
 			ignored.add(work[1].tri);
 		}
 		
 		// Check that origin vertex can be moved
 		if (!checkNewRingNormalsNonManifoldVertex(xn, ignored))
 			return false;
 		// Check that destination vertex can be moved
 		symOTri(this, work[2]);
 		if (!work[2].checkNewRingNormalsNonManifoldVertex(xn, ignored))
 			return false;
 		ignored.clear();
 
 		//  Topology check.
 		//  See in AbstractHalfEdgeTest.buildMeshTopo() why this
 		//  check is needed.
 		//  When edge is non manifold, we do not use Vertex.getNeighboursNodes()
 		//  because checks have to be performed by fans.
 		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
 		{
 			VirtualHalfEdge f = (VirtualHalfEdge) it.next();
 			if (!f.canCollapseTopology())
 				return false;
 		}
 		return true;
 	}
 	/*
 	 * Warning: this method uses work[0] and work[1] temporary arrays.
 	 */
 	private final boolean checkNewRingNormalsNonManifoldVertex(double [] newpt, Collection<Triangle> ignored)
 	{
 		Vertex o = origin();
 		if (o.getLink() instanceof Triangle)
 			return checkNewRingNormalsSameFanNonManifoldVertex(newpt, ignored);
 		for (Triangle start: (Triangle []) o.getLink())
 		{
 			work[1].bind(start);
 			if (work[1].destination() == o)
 				work[1].next();
 			else if (work[1].apex() == o)
 				work[1].prev();
 			assert work[1].origin() == o;
 			if (!work[1].checkNewRingNormalsSameFanNonManifoldVertex(newpt, ignored))
 				return false;
 		}
 		return true;
 	}
 	/*
 	 * Warning: this method uses work[0] temporary array.
 	 */
 	private final boolean checkNewRingNormalsSameFanNonManifoldVertex(double [] newpt, Collection<Triangle> ignored)
 	{
 		// Loop around origin.  We need to copy current instance
 		// into work[0] because loop may be interrupted.
 		copyOTri(this, work[0]);
 		Vertex d = destination();
 		do
 		{
 			if (!ignored.contains(work[0].tri) && !work[0].hasAttributes(OUTER))
 			{
 				double [] x1 = work[0].destination().getUV();
 				work[0].next();
 				double area  = work[0].computeNormal3DT();
 				double [] nu = work[0].getTempVector();
 				work[0].prev();
 				for (int i = 0; i < 3; i++)
 					tempD1[i] = newpt[i] - x1[i];
 				// Two triangles are removed when an edge is contracted.
 				// So normally triangle areas should increase.  If they
 				// decrease significantly, there may be a problem.
 				if (Matrix3D.prodSca(tempD1, nu) >= - area)
 					return false;
 			}
 			work[0].nextOriginLoop();
 		}
 		while (work[0].destination() != d);
 		return true;
 	}
 	
 	/**
 	 * Topology check.
 	 * See in AbstractHalfEdgeTest.buildMeshTopo() why this
 	 * check is needed.
 	 * Warning: this method uses work[0] temporary array.
 	 */
 	private final boolean canCollapseTopology()
 	{
 		Collection<Vertex> neighbours = new HashSet<Vertex>();
 		// We need to copy current instance into work[0]
 		// because second loop may be interrupted.
 		copyOTri(this, work[0]);
 		Vertex d = work[0].destination();
 		do
 		{
 			// Warning: mesh.outerVertex is intentionnally not filtered out
 			neighbours.add(work[0].destination());
 			work[0].nextOriginLoop();
 		}
 		while (work[0].destination() != d);
 		work[0].sym();
 		int cnt = 0;
 		d = work[0].destination();
 		do
 		{
 			// Warning: mesh.outerVertex is intentionnally not filtered out
 			if (neighbours.contains(work[0].destination()))
 			{
 				if (cnt > 1)
 					return false;
 				cnt++;
 			}
 			work[0].nextOriginLoop();
 		}
 		while (work[0].destination() != d);
 		return true;
 	}
 	
 	/**
 	 * Contract an edge.
 	 *
 	 * @param m mesh
 	 * @param n the resulting vertex
 	 * @return edge starting from <code>n</code> and pointing to original apex
 	 * @throws IllegalArgumentException if edge belongs to an outer triangle,
 	 * because there would be no valid return value.  User must then run this
 	 * method against symmetric edge, this is not done automatically.
 	 * @see Mesh#edgeCollapse
 	 */
 	@Override
 	protected final AbstractHalfEdge collapse(AbstractMesh m, AbstractVertex n)
 	{
 		if (hasAttributes(OUTER))
 			throw new IllegalArgumentException("Cannot contract "+this);
 		Vertex o = origin();
 		Vertex d = destination();
 		Vertex v = (Vertex) n;
 		assert o.isWritable() && d.isWritable(): "Cannot contract "+this;
 		if (logger.isDebugEnabled())
 			logger.debug("contract ("+o+" "+d+")");
 		//  Replace o by n in all incident triangles
 		if (o.getLink() instanceof Triangle)
 			replaceEndpointsSameFan(v);
 		else
 			replaceEndpointsNonManifold(o, v);
 		//  Replace d by n in all incident triangles
 		symOTri(this, work[2]);
 		if (d.getLink() instanceof Triangle)
 			work[2].replaceEndpointsSameFan(v);
 		else
 			replaceEndpointsNonManifold(d, v);
 		//  Set v links
 		deepCopyVertexLinks(o, d, v);
 		if (logger.isDebugEnabled())
 			logger.debug("new point: "+v);
 		
 		if (!hasAttributes(NONMANIFOLD))
 			return VHcollapseSameFan((Mesh) m, v, true);
 		// Edge is non-manifold
 		assert work[2].hasAttributes(OUTER);
 		// VHcollapseSameFan may modify LinkedHashMap structure
 		// used by fanIterator(), we need a copy.
 		LinkedHashMap<Triangle, Integer> copy = new LinkedHashMap<Triangle, Integer>();
 		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
 		{
 			VirtualHalfEdge h = (VirtualHalfEdge) it.next();
 			copy.put(h.tri, int3[h.localNumber]);
 		}
 		Triangle ret = null;
 		int num = -1;
 		for (Map.Entry<Triangle, Integer> entry: copy.entrySet())
 		{
 			Triangle t = entry.getKey();
 			int l = entry.getValue().intValue();
 			work[2].bind(t, l);
 			assert !work[2].hasAttributes(OUTER);
 			if (t == tri)
 			{
 				work[2].VHcollapseSameFan((Mesh) m, v, false);
 				ret = work[2].tri;
 				num = work[2].localNumber;
 			}
 			else
 				work[2].VHcollapseSameFan((Mesh) m, v, false);
 		}
 		assert ret != null;
 		bind(ret, num);
 		return this;
 	}
 
 	/*
 	 * Warning: this method uses work[0] and work[1] temporary arrays.
 	 */
 	private VirtualHalfEdge VHcollapseSameFan(Mesh m, Vertex n, boolean manifold)
 	{
 		/*
 		 *           V1                       V1
 		 *  V3+-------+-------+ V4   V3 +------+------+ V4
 		 *     \ t3  / \ t4  /           \  t3 | t4  / 
 		 *      \   /   \   /              \   |   /
 		 *       \ / t1  \ /                 \ | /  
 		 *      o +-------+ d   ------>      n +
 		 *       / \ t2  / \                 / | \
 		 *      /   \   /   \              /   |   \
 		 *     / t5  \ / t6  \           /  t5 | t6  \
 		 *    +-------+-------+         +------+------+
 		 *  V5        V2       V6     V5       V2      V6
 		 */
 		// this = (odV1)
 		//  Update adjacency links.  For clarity, o and d are
 		//  written instead of n.
 		Triangle t1 = tri;
 		sym();
 		Triangle t2 = tri;
 		sym();
 		next();                 // (dV1o)
 		int attr4 = attributes;
 		symOTri(this, work[0]); // (V1dV4)
 		next();                 // (V1od)
 		int attr3 = attributes;
 		symOTri(this, work[1]); // (oV1V3)
 		if (work[1].hasAttributes(NONMANIFOLD))
 		{
 			// this is listed in adjacency list and
 			// has to be replaced by s
 			replaceEdgeLinks(work[0]);
 			work[1].VHglue(work[0]);
 		}
 		else if (work[0].hasAttributes(NONMANIFOLD))
 		{
 			// this is listed in adjacency list and
 			// has to be replaced by s
 			prev();         // (dV1o)
 			replaceEdgeLinks(work[1]);
 			work[0].VHglue(work[1]);
 			next();         // (V1od)
 		}
 		else
 		{
 			work[0].VHglue(work[1]);
 		}
 		work[0].attributes |= attr3;
 		work[1].attributes |= attr4;
 		work[0].pushAttributes();
 		work[1].pushAttributes();
 		if (!work[0].hasAttributes(OUTER))
 		{
 			Triangle t34 = work[1].tri;
 			if (t34.isOuter())
 				t34 = work[0].tri;
 			assert !t34.isOuter() : work[0]+"\n"+work[1];
 			replaceVertexLinks(work[1].destination(), t1, t2, t34);
 			replaceVertexLinks(n, t1, t2, t34);
 		}
 		next();                         // (odV1)
 		sym();                          // (doV2)
 		if (manifold && !hasAttributes(OUTER))
 		{
 			next();                 // (oV2d)
 			int attr5 = attributes;
 			symOTri(this, work[0]); // (V2oV5)
 			next();                 // (V2do)
 			int attr6 = attributes;
 			symOTri(this, work[1]); // (dV2V6)
 			if (work[1].hasAttributes(NONMANIFOLD))
 			{
 				replaceEdgeLinks(work[0]);
 				work[1].VHglue(work[0]);
 			}
 			else if (work[0].hasAttributes(NONMANIFOLD))
 			{
 				prev();         // (oV2d)
 				replaceEdgeLinks(work[1]);
 				work[0].VHglue(work[1]);
 				next();         // (doV2)
 			}
 			else
 				work[0].VHglue(work[1]);
 			work[0].attributes |= attr6;
 			work[1].attributes |= attr5;
 			work[0].pushAttributes();
 			work[1].pushAttributes();
 			if (!hasAttributes(OUTER))
 			{
 				Triangle t56 = work[0].tri;
 				if (t56.isOuter())
 					t56 = work[1].tri;
 				assert !t56.isOuter();
 				replaceVertexLinks(work[0].origin(), t1, t2, t56);
 				replaceVertexLinks(n, t1, t2, t56);
 			}
 			next();                 // (doV2)
 		}
 		else
 		{
 			assert hasAttributes(OUTER);
 		}
 		symOTri(this, work[0]);         // (odV1)
 		clearAttributes(MARKED);
 		pushAttributes();
 		m.remove(tri);
 		// By convention, edge is moved into (oV1V3), but this may change.
 		// We have to move before removing adjacency relations.
 		nextOTri(work[0], this);        // (dV1o)
 		sym();                          // (V1dV4)
 		sym();                          // (oV1V3)
 		work[0].clearAttributes(MARKED);
 		work[0].pushAttributes();
 		m.remove(work[0].tri);
 		return this;
 	}
 	
 	private void replaceEndpointsSameFan(Vertex n)
 	{
 		Vertex d = destination();
 		do
 		{
 			setOrigin(n);
 			nextOriginLoop();
 		}
 		while (destination() != d);
 	}
 	/*
 	 * Warning: this method uses work[0] temporary array.
 	 */
 	private static final void replaceEndpointsNonManifold(Vertex o, Vertex n)
 	{
 		Triangle [] oList = (Triangle []) o.getLink();
 		for (Triangle t: oList)
 		{
 			work[0].bind(t);
 			if (work[0].origin() != o)
 				work[0].next();
 			if (work[0].origin() != o)
 				work[0].next();
 			assert work[0].origin() == o : ""+o+" not in "+work[0];
 			work[0].replaceEndpointsSameFan(n);
 		}
 	}
 	private static void replaceVertexLinks(Vertex o, Triangle oldT1, Triangle oldT2, Triangle newT)
 	{
 		if (o.getLink() instanceof Triangle)
 			o.setLink(newT);
 		else
 		{
 			Triangle [] tArray = (Triangle []) o.getLink();
 			for (int i = 0; i < tArray.length; i++)
 			{
 				if (tArray[i] == oldT1 || tArray[i] == oldT2)
 				{
 					logger.debug("replaceVertexLinks: "+tArray[i]+" --> "+newT);
 					tArray[i] = newT;
 				}
 			}
 		}
 	}
 	/*
 	 * Warning: this method uses work[0] and work[1] temporary arrays.
 	 */
 	private static void deepCopyVertexLinks(Vertex o, Vertex d, Vertex v)
 	{
 		boolean ot = o.getLink() instanceof Triangle;
 		boolean dt = d.getLink() instanceof Triangle;
 		//  Prepare vertex links first
 		if (ot && dt)
 		{
 			v.setLink(d.getLink());
 		}
 		else if (ot)
 		{
 			Triangle [] dList = (Triangle []) d.getLink();
 			Triangle [] nList = new Triangle[dList.length];
 			System.arraycopy(dList, 0, nList, 0, dList.length);
 			v.setLink(nList);
 		}
 		else if (dt)
 		{
 			Triangle [] oList = (Triangle []) o.getLink();
 			Triangle [] nList = new Triangle [oList.length];
 			System.arraycopy(oList, 0, nList, 0, oList.length);
 			v.setLink(nList);
 		}
 		else
 		{
 			// Vertex.setLinkFan() cannot be called here because fans from
 			// o and d have to be merged.
 			Triangle [] oList = (Triangle []) o.getLink();
 			Triangle [] dList = (Triangle []) d.getLink();
 			Triangle [] nList = new Triangle[oList.length+dList.length];
 			System.arraycopy(oList, 0, nList, 0, oList.length);
 			System.arraycopy(dList, 0, nList, oList.length, dList.length);
 			ArrayList<Triangle> res = new ArrayList<Triangle>();
 			Set<Triangle> allTriangles = new HashSet<Triangle>();
 			// o and d have already been replaced by v
 			for (Triangle t: nList)
 			{
 				if (!allTriangles.contains(t))
 					res.add(t);
 				allTriangles.add(t);
 				work[0].bind(t);
 				if (work[0].origin() != v)
 					work[0].next();
 				if (work[0].origin() != v)
 					work[0].next();
 				if (work[0].origin() == v)
 				{
 					// Add all triangles of the same fan to allTriangles
 					boolean found = false;
 					Vertex end = work[0].destination();
 					do
 					{
 						work[0].nextOriginLoop();
 						allTriangles.add(work[0].tri);
 						if (work[0].destination() == v)
 						{
 							found = true;
 							copyOTri(work[0], work[1]);
 						}
 					}
 					while (work[0].destination() != end);
 					if (found)
 					{
 						work[1].next();
 						end = work[1].destination();
 						do
 						{
 							work[1].nextOriginLoop();
 							allTriangles.add(work[1].tri);
 						}
 						while (work[1].destination() != end);
 					}
 				}
 				boolean found = false;
 				if (work[0].destination() == v)
 				{
 					found = true;
 					work[0].next();
 				}
 				else if (work[0].apex() == v)
 				{
 					found = true;
 					work[0].prev();
 				}
 				if (found)
 				{
 					// Add all triangles of the same fan to allTriangles
 					found = false;
 					Vertex end = work[0].destination();
 					do
 					{
 						work[0].nextOriginLoop();
 						allTriangles.add(work[0].tri);
 						if (work[0].destination() == v)
 						{
 							found = true;
 							copyOTri(work[0], work[1]);
 						}
 					}
 					while (work[0].destination() != end);
 					if (found)
 					{
 						work[1].next();
 						end = work[1].destination();
 						do
 						{
 							work[1].nextOriginLoop();
 							allTriangles.add(work[1].tri);
 						}
 						while (work[1].destination() != end);
 					}
 				}
 			}
 			v.setLink(new Triangle[res.size()]);
 			res.toArray((Triangle[]) v.getLink());
 		}
 	}
 	private void replaceEdgeLinks(VirtualHalfEdge that)
 	{
 		// Current instance is a non-manifold edge which has been
 		// replaced by 'that'.  Replace all occurrences in adjacency
 		// list.
 		final Map<Triangle, Integer> list = getAdjNonManifold();
 		Integer I = list.get(tri);
 		assert I != null && I.intValue() == localNumber;
 		list.remove(tri);
 		list.put(that.tri, int3[that.localNumber]);
 	}
 	
 	/**
 	 * Split an edge.  This is the opposite of collapse.
 	 *
 	 * @param m  mesh
 	 * @param n  the resulting vertex
 	 * @see Mesh#vertexSplit
 	 */
 	@Override
 	protected final AbstractHalfEdge split(AbstractMesh m, AbstractVertex n)
 	{
 		if (logger.isDebugEnabled())
 			logger.debug("split edge ("+origin()+" "+destination()+") by adding vertex "+n);
 		Vertex v = (Vertex) n;
 		if (!hasAttributes(NONMANIFOLD))
 		{
 			v.setLink(tri);
 			VHsplitSameFan((Mesh) m, v);
 			return this;
 		}
 		// Set vertex links
 		ArrayList<Triangle> link = new ArrayList<Triangle>();
 		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
 		{
 			VirtualHalfEdge f = (VirtualHalfEdge) it.next();
 			link.add(f.tri);
 		}
 		v.setLink(new Triangle[link.size()]);
 		link.toArray((Triangle[]) v.getLink());
 		link.clear();
 		for (Iterator<AbstractHalfEdge> it = fanIterator(); it.hasNext(); )
 		{
 			VirtualHalfEdge f = (VirtualHalfEdge) it.next();
 			f.VHsplitSameFan((Mesh) m, v);
 		}
 		return this;
 	}
 	/*
 	 * Warning: this method uses work[0], work[1] and work[2] temporary arrays.
 	 */
 	private void VHsplitSameFan(Mesh m, Vertex n)
 	{
 		if (hasAttributes(OUTER))
 			throw new IllegalArgumentException("Cannot split "+this);
 
 		/*
 		 *            V1                             V1
 		 *            /'\                            /|\
 		 *          /     \                        /  |  \
 		 *        /      h1 \                    /    |    \
 		 *      /             \                /    n1|   h1 \
 		 *    /       t1        \            /   t1   |  t3    \
 		 * o +-------------------+ d ---> o +---------+---------+ d
 		 *    \       t2        /            \   t4   |  t2    /
 		 *      \             /                \    n2|   h2 /
 		 *        \      h2 /                    \    |    /
 		 *          \     /                        \  |  /
 		 *            \,/                            \|/
 		 *            V2                             V2
 		 */
 		splitVertexAddOneTriangle(m, n);
 		symOTri(this, work[0]);
 		work[0].splitVertexAddOneTriangle(m, n);
 		
 		Triangle t1 = tri;
 		// t1 is still glued to t2, it has to be glued to t4, and t3 to t2.
 		nextOTri(this, work[1]);        // (nV1o)
 		work[1].sym();                  // (V1nd)
 		work[1].next();                 // (ndV1)
 		Triangle t3 = work[1].tri;
 
 		symOTri(this, work[0]);         // (dnV2)
 		work[0].VHglue(work[1]);
 		Triangle t2 = work[0].tri;
 		work[0].next();                 // (nV2d)
 		work[0].sym();                  // (V2no)
 		work[0].next();                 // (noV2)
 		VHglue(work[0]);
 		Triangle t4 = work[0].tri;
 		if (t2.isOuter())
 		{
 			// Remove links between t2 and t4
 			work[0].prev();         // (V2no)
 			symOTri(work[0], work[1]);    // (nV2d)
 			work[0].setAdj(null);
 			work[1].setAdj(null);
 			// Move work[1] so that d == work[1].destination()
 			work[1].next();         // (V2dn)
 		}
 
 		Triangle t14 = (t1.isOuter() ? t4 : t1);
 		Triangle t23 = (t2.isOuter() ? t3 : t2);
 		//  Update vertex links
 		replaceVertexLinks(n, t1, t2, t14);
 		replaceVertexLinks(work[1].destination(), t1, t2, t23);
 		replaceVertexLinks(origin(), t1, t2, t14);
 	}
 	
 	/*
 	 * Warning: this method uses work[1] and work[2] temporary arrays.
 	 */
 	private final void splitVertexAddOneTriangle(Mesh m, Vertex n)
 	{
 		/*
 		 *            V1                             V1
 		 *            /'\                            /|\
 		 *          /     \                        /  |  \
 		 *        /      w1 \                    /  w1| w2 \
 		 *      /             \                /      |      \
 		 *    /       t1        \            /   t1   |  t3    \
 		 * o +-------------------+ d ---> o +---------+---------+ d
 		 */
 		TriangleVH t1 = (TriangleVH) tri;
 		TriangleVH t3 = (TriangleVH) m.createTriangle(t1);
 		m.add(t3);
 		
 		if (!hasAttributes(OUTER))
 		{
 			nextOTri(this, work[2]);
 			symOTri(work[2], work[1]);
 			work[2].bind(t3, work[2].localNumber);
 			work[1].VHglue(work[2]);
 		}
 
 		next();                         // (nV1o)
 		work[1].bind(t3, localNumber);  // (dV1n)
 
 		// Update Triangle links
 		tri = t1;
 		work[1].tri = t3;
 
 		// Update vertices
 		setOrigin(n);
 		work[1].setApex(n);
 
 		// If h1 is non-manifold, update adjacency list
 		if (work[1].hasAttributes(NONMANIFOLD))
 			replaceEdgeLinks(work[1]);
 
 		// Inner edge
 		work[1].next();                 // (dV1n)
 		VHglue(work[1]);
 
 		// Clear BOUNDARY and NONMANIFOLD flags on inner edges
 		work[1].clearAttributes(BOUNDARY | NONMANIFOLD);
 		clearAttributes(BOUNDARY | NONMANIFOLD);
 		prev();                         // (onV1)
 	}
 	
 	private final Iterator<AbstractHalfEdge> identityFanIterator()
 	{
 		final VirtualHalfEdge current = this;
 		logger.debug("Manifold fan iterator");
 		return new Iterator<AbstractHalfEdge>()
 		{
 			private boolean next = true;
 			public boolean hasNext()
 			{
 				return next;
 			}
 			public AbstractHalfEdge next()
 			{
 				if (!next)
 					throw new NoSuchElementException();
 				next = false;
 				return current;
 			}
 			public void remove()
 			{
 			}
 		};
 	}
 	
 	@Override
 	public final Iterator<AbstractHalfEdge> fanIterator()
 	{
 		if (!hasAttributes(NONMANIFOLD))
 			return identityFanIterator();
 		final Map<Triangle, Integer> list = getAdjNonManifold();
 		return new Iterator<AbstractHalfEdge>()
 		{
 			VirtualHalfEdge ret = new VirtualHalfEdge();
 			private Iterator<Map.Entry<Triangle, Integer>> it = list.entrySet().iterator();
 			public boolean hasNext()
 			{
 				return it.hasNext();
 			}
 			public AbstractHalfEdge next()
 			{
 				Map.Entry<Triangle, Integer> entry = it.next();
 				ret.bind(entry.getKey(), entry.getValue().intValue());
 				return ret;
 			}
 			public void remove()
 			{
 			}
 		};
 	}
 
 	private final String showAdj(int num)
 	{
 		if (!(tri.getAdj(num) instanceof Triangle))
 			return "N/A";
 		Triangle t = (Triangle) tri.getAdj(num);
 		StringBuilder ret = new StringBuilder();
 		if (t == null)
 			ret.append("null");
 		else
 			ret.append(t.hashCode()+"["+tri.getAdjLocalNumber(num)+"]");
 		return ret.toString();
 	}
 	
 	@Override
 	public String toString()
 	{
 		StringBuilder r = new StringBuilder("Local number: "+localNumber);
 		r.append("\nTri hashcode: "+tri.hashCode());
 		r.append("\nGroup: "+tri.getGroupId());
 		r.append("\nAdjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2));
 		r.append("\nAttributes: "+Integer.toHexString(tri.getEdgeAttributes(0))+" "+Integer.toHexString(tri.getEdgeAttributes(1))+" "+Integer.toHexString(tri.getEdgeAttributes(2))+" => "+Integer.toHexString(attributes));
 		r.append("\nVertices:");
 		r.append("\n  Origin: "+origin());
 		r.append("\n  Destination: "+destination());
 		r.append("\n  Apex: "+apex());
 		return r.toString();
 	}
 
 }
