 /* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
    modeler, Finite element mesher, Plugin architecture.
 
     Copyright (C) 2005, by EADS CRC
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
 
 package org.jcae.mesh.oemm;
 
 import java.io.Serializable;
 import org.apache.log4j.Logger;
 
 /**
  * This class represents an empty OEMM.
  * 
  * An OEMM is a pointer-based octree, but cells do not contain any data.
  * Only its spatial structure is considered, and it is assumed that the whole
  * tree can reside in memory.  This class defines the octree structure and
  * how to traverse it.
  *
  * References:
  * External Memory Management and Simplification of Huge Meshes
  * P. Cignoni, C. Montani, C. Rocchini, R. Scopigno
  * http://vcg.isti.cnr.it/publications/papers/oemm_tvcg.pdf
  */
 public class OEMM implements Serializable
 {
 	private static final long serialVersionUID = -745324615207484210L;
 
 	private static Logger logger = Logger.getLogger(OEMM.class);	
 	
 	/**
 	 * Maximal tree depth.
 	 */
 	public static final int MAXLEVEL = 30;
 
 	/**
 	 * Root cell size.
 	 */
 	private static final int gridSize = 1 << MAXLEVEL;
 	
 	/**
 	 * Top-level directory.
 	 */
 	private transient String topDir;
 
 	/**
 	 * Number of leaves.
 	 */
 	private transient int nr_leaves = 0;
 
 	/**
 	 * Total number of cells.
 	 */
 	private transient int nr_cells = 0;
 
 	/**
 	 * Tree depth.
 	 */
 	private transient int depth = 0;
 	
 	/**
 	 * Double/integer conversion.  First three values contain coordinates
 	 * of bottom-left corner, and the last one is a scale factor.
 	 * Any coordinate can then been converted from double to integer by this
 	 * formula:
 	 * <pre>
 	 *  I[i] = (D[i] - x0[i]) * x0[3];
 	 * </pre>
 	 * and inverse conversion is
 	 * <pre>
 	 *  D[i] = x0[i] + I[i] / x0[3];
 	 * </pre>
 	 */
 	public double [] x0 = new double[4];
 
 	/**
 	 * Maximal width in a dimension.
 	 */
 	public double xdelta;
 	
 	/**
 	 * Root cell.
 	 */
 	protected transient OEMMNode root = null;
 	
 	/**
 	 * Array of leaves.
 	 */
 	public transient OEMMNode [] leaves;
 	
 	/**
 	 * Create an empty OEMM.
 	 */
 	public OEMM(String dir)
 	{
 		topDir = dir;
 	}
 	
 	/**
 	 * Create an empty OEMM with a given depth.
 	 */
 	public OEMM(int l)
 	{
 		depth = l;
 		if (depth > MAXLEVEL)
 		{
 			logger.error("Max. level too high");
 			depth = MAXLEVEL;
 		}
 		else if (depth < 1)
 		{
 			logger.error("Max. level too low");
 			depth = 1;
 		}
 	}
 	
 	/**
 	 * Remove all cells from a tree.
 	 */
 	public final void clearNodes()
 	{
 		nr_cells = 0;
 		nr_leaves = 0;
 		leaves = null;
 		root = null;
 	}
 
 	/**
 	 * Sets object bounding box.  This method computes {@link #x0} and
 	 * {@link #xdelta}.
 	 *
 	 * @param bbox  bounding box
 	 */
 	public final void setBoundingBox(double [] bbox)
 	{
 		clearNodes();
 		xdelta = Double.MIN_VALUE;
 		for (int i = 0; i < 3; i++)
 		{
 			double delta = bbox[i+3] - bbox[i];
 			if (delta > xdelta)
 				xdelta = delta;
 			x0[i] = bbox[i];
 		}
 		// Enlarge bounding box by 1% to avoid rounding errors
 		for (int i = 0; i < 3; i++)
 			x0[i] -= 0.005*xdelta;
 		xdelta *= 1.01;
 		x0[3] = ((double) gridSize) / xdelta;
 		logger.debug("Lower left corner : ("+x0[0]+", "+x0[1]+", "+x0[2]+")   Bounding box length: "+xdelta);
 	}
 
 	/**
 	 * Returns top-level directory.
 	 *
 	 * @return top-level directory
 	 */
 	public final String getDirectory()
 	{
 		return topDir;
 	}
 	
 	/**
 	 * Sets top-level directory.
 	 *
 	 * @param dir  top-level directory
 	 */
 	public final void setDirectory(String dir)
 	{
 		topDir = dir;
 	}
 	
 	/**
 	 * Returns file name containing {@link OEMM} data structure.
 	 *
 	 * @return file name
 	 */
 	public final String getFileName()
 	{
 		return topDir+java.io.File.separator+"oemm";
 	}
 
 	/**
 	 * Returns number of leaves.
 	 *
 	 * @return number of leaves
 	 */
 	public final int getNumberOfLeaves()
 	{
 		return nr_leaves;
 	}
 
 	/**
 	 * Returns size of deepest cell.
 	 *
 	 * @return size of deepest cell
 	 */
 	protected final int minCellSize()
 	{
 		return (1 << (MAXLEVEL + 1 - depth));
 	}
 
 	/**
 	 * Returns size of cells at a given height.  By convention, height is set
 	 * to 0 for bottom leaves.
 	 *
 	 * @param h  cell height
 	 * @return size of cells at given height
 	 */
	private final int cellSizeByHeight(int h)
 	{
 		if (h < depth)
 			return (1 << (MAXLEVEL + 1 - depth + h));
 		else
 			return gridSize;
 	}
 
 	/**
 	 * Prints tree stats.
 	 */
 	public final void printInfos()
 	{
 		logger.info("Number of leaves: "+nr_leaves);
 		logger.info("Number of octants: "+nr_cells);
 		logger.info("Depth: "+depth);
 	}
 	
 	/**
 	 * Converts from double coordinates to integer coordinates.
 	 * @param p    double coordinates.
 	 * @param ijk  integer coordinates.
 	 */
 	public final void double2int(double [] p, int [] ijk)
 	{
 		for (int i = 0; i < 3; i++)
 			ijk[i] = (int) ((p[i] - x0[i]) * x0[3]);
 	}
 	
 	/**
 	 * Converts from integer coordinates to double coordinates.
 	 * @param ijk  integer coordinates.
 	 * @param p    double coordinates.
 	 */
 	public final void int2double(int [] ijk, double [] p)
 	{
 		for (int i = 0; i < 3; i++)
 			p[i] = x0[i] + ijk[i] / x0[3];
 	}
 	
 	/**
 	 * Traverses the whole OEMM structure.
 	 *
 	 * @param proc    procedure called on each octant.
 	 * @return  <code>true</code> if the whole structure has been traversed,
 	 *          <code>false</code> if traversal aborted.
 	 */
 	public final boolean walk(TraversalProcedure proc)
 	{
 		if (logger.isDebugEnabled())
 			logger.debug("walk: init "+proc.getClass().getName());
 		int s = gridSize;
 		int l = 0;
 		int i0 = 0;
 		int j0 = 0;
 		int k0 = 0;
 		int [] posStack = new int[depth];
 		posStack[l] = 0;
 		OEMMNode [] octreeStack = new OEMMNode[depth];
 		octreeStack[l] = root;
 		proc.init(this);
 		while (true)
 		{
 			int res = 0;
 			int visit = octreeStack[l].isLeaf ? TraversalProcedure.LEAF : TraversalProcedure.PREORDER;
 			if (logger.isDebugEnabled())
 				logger.debug("Found "+(octreeStack[l].isLeaf ? "LEAF" : "PREORDER")+Integer.toHexString(s)+" "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0)+" "+octreeStack[l]);
 			res = proc.action(this, octreeStack[l], posStack[l], visit);
 			logger.debug("  Res; "+res);
 			if (res == TraversalProcedure.ABORT)
 				return false;
 			if (!octreeStack[l].isLeaf && res == TraversalProcedure.OK)
 			{
 				s >>= 1;
 				assert s > 0;
 				l++;
 				assert l < depth;
 				for (int i = 0; i < 8; i++)
 				{
 					if (null != octreeStack[l-1].child[i])
 					{
 						octreeStack[l] = octreeStack[l-1].child[i];
 						posStack[l] = i;
 						break;
 					}
 					else
 						logger.debug("Empty node skipped: pos="+i);
 				}
 				if ((posStack[l] & 1) != 0)
 					i0 += s;
 				if ((posStack[l] & 2) != 0)
 					j0 += s;
 				if ((posStack[l] & 4) != 0)
 					k0 += s;
 			}
 			else
 			{
 				while (l > 0)
 				{
 					posStack[l]++;
 					if ((posStack[l] & 1) != 0)
 						i0 += s;
 					else
 					{
 						i0 -= s;
 						if (posStack[l] == 2 || posStack[l] == 6)
 							j0 += s;
 						else
 						{
 							j0 -= s;
 							if (posStack[l] == 4)
 								k0 += s;
 							else
 								k0 -= s;
 						}
 					}
 					if (posStack[l] == 8)
 					{
 						s <<= 1;
 						l--;
 						if (logger.isDebugEnabled())
 							logger.debug("Found POSTORDER: "+Integer.toHexString(s)+" "+Integer.toHexString(i0)+" "+Integer.toHexString(j0)+" "+Integer.toHexString(k0)+" "+octreeStack[l]);
 						res = proc.action(this, octreeStack[l], posStack[l], TraversalProcedure.POSTORDER);
 						logger.debug("  Res; "+res);
 					}
 					else
 					{
 						if (null != octreeStack[l-1].child[posStack[l]])
 							break;
 						if (logger.isDebugEnabled())
 							logger.debug("Empty node skipped: pos="+posStack[l]);
 					}
 				}
 				if (l == 0)
 					break;
 				octreeStack[l] = octreeStack[l-1].child[posStack[l]];
 			}
 		}
 		assert i0 == 0;
 		assert j0 == 0;
 		assert k0 == 0;
 		proc.finish(this);
 		return true;
 	}
 	
 	/*         k=0          k=1
 	 *      .-------.    .-------.
 	 *      | 2 | 3 |    | 6 | 7 |
 	 *   j  |---+---|    |---+---|
 	 *      | 0 | 1 |    | 4 | 5 |
 	 *      `-------'    `-------'
 	 *          i          
 	 */
 	/**
 	 * Returns local index of cell containing a given point.
 	 * @param size  size of child cells
 	 * @param ijk   integer coordinates of desired point
 	 */
 	protected static final int indexSubOctree(int size, int [] ijk)
 	{
 		int ret = 0;
 		if (size == 0)
 			throw new RuntimeException("Exceeded maximal number of levels for octrees... Aborting");
 		for (int i = 0; i < 3; i++)
 		{
 			if ((ijk[i] & size) != 0)
 				ret |= 1 << i;
 		}
 		return ret;
 	}
 	
 	/**
 	 * Builds an octant containing a given point if it does not already exist.
 	 *
 	 * @param ijk     integer coordinates of an interior node
 	 * @return  the octant of the smallest size containing this point.
 	 *          It is created if it does not exist.
 	 */
 	public final OEMMNode build(int [] ijk)
 	{
 		return search(minCellSize(), ijk, true, null);
 	}
 	
 	/**
 	 * Inserts an octant into the tree structure if it does not already exist.
 	 *
 	 * @param current     node being inserted.
 	 */
 	public final void insert(OEMMNode current)
 	{
 		int [] ijk = new int[3];
 		ijk[0] = current.i0;
 		ijk[1] = current.j0;
 		ijk[2] = current.k0;
 		search(current.size, ijk, true, current);
 	}
 	
 	/**
 	 * Returns the octant of an OEMM structure containing a given point.
 	 *
 	 * @param ijk     integer coordinates of an interior node
 	 * @return  the octant of the smallest size containing this point.
 	 */
 	public final OEMMNode search(int [] ijk)
 	{
 		return search(0, ijk, false, null);
 	}
 	
 	/**
 	 * Returns the octant of an OEMM structure containing a given point.
 	 *
 	 * @param size     the returned octant must have this size.  If this value is 0,
 	 *                 the deepest octant is returned.
 	 * @param ijk      integer coordinates of an interior node
 	 * @param create   if set to <code>true</code>, cells are created if needed.  Otherwise
 	 *                 the desired octant must exist.
 	 * @return  the octant of the desired size containing this point.
 	 */
 	private final OEMMNode search(int size, int [] ijk, boolean create, OEMMNode node)
 	{
 		if (root == null)
 		{
 			if (!create)
 				throw new RuntimeException("Element not found... Aborting ");
 			createRootNode(node);
 			if (size == gridSize)
 			{
 				root.isLeaf = true;
 				nr_leaves++;
 				if (depth == 0)
 					depth++;
 			}
 		}
 		OEMMNode current = root;
 		int level = 0;
 		int s = current.size;
 		while (s > size)
 		{
 			if (current.isLeaf && !create)
 				return current;
 			s >>= 1;
 			level++;
 			assert s > 0;
 			int ind = indexSubOctree(s, ijk);
 			if (null == current.child[ind])
 			{
 				if (!create)
 					throw new RuntimeException("Element not found... Aborting "+current+" "+Integer.toHexString(s)+" "+ind);
 				if (level >= depth)
 					depth = level + 1;
 				if (depth > MAXLEVEL)
 					throw new RuntimeException("Too many octree levels... Aborting");
 				if (s == size && node != null)
 					current.child[ind] = node;
 				else
 					current.child[ind] = new OEMMNode(s, ijk);
 				current.child[ind].parent = current;
 				current.isLeaf = false;
 				nr_cells++;
 				if (s == size)
 					nr_leaves++;
 			}
 			current = current.child[ind];
 		}
 		return current;
 	}
 
 	/**
 	 * Returns the octant of an OEMM structure containing a given point.
 	 *
 	 * @param ijk     integer coordinates of an interior node
 	 * @return  the octant of the smallest size containing this point.
 	 */
 	private void createRootNode(OEMMNode node)
 	{
 		if (node != null && node.size == gridSize)
 		{
 			// This happens only when OEMM has only one leaf
 			// and is read from disk, root has to be set to
 			// this leaf.
 			root = node;
 		}
 		else
 			root = new OEMMNode(gridSize, 0, 0, 0);
 		nr_cells++;
 	}
 
 	/**
 	 * Merges all children of a given cell.
 	 *
 	 * @param node   cell to be merged
 	 */
 	protected final void mergeChildren(OEMMNode node)
 	{
 		assert !node.isLeaf;
 		for (int ind = 0; ind < 8; ind++)
 		{
 			if (node.child[ind] != null)
 			{
 				assert node.child[ind].isLeaf;
 				node.child[ind] = null;
 				nr_leaves--;
 				nr_cells--;
 			}
 		}
 		node.isLeaf = true;
 		nr_leaves++;
 	}
 
 	/**
 	 * Returns the adjacent node located at a given point with the
 	 * same size.
 	 *
 	 * @param fromNode start node
 	 * @param ijk      integer coordinates of lower-left corner
 	 * @return  the octant of the desired size containing this point.
 	 */
 	public static final OEMMNode searchAdjacentNode(OEMMNode fromNode, int [] ijk)
 	{
 		int i1 = ijk[0];
 		if (i1 < 0 || i1 >= gridSize)
 			return null;
 		int j1 = ijk[1];
 		if (j1 < 0 || j1 >= gridSize)
 			return null;
 		int k1 = ijk[2];
 		if (k1 < 0 || k1 >= gridSize)
 			return null;
 		//  Neighbor octant is within OEMM bounds
 		//  First climb tree until an octant enclosing this
 		//  point is encountered.
 		OEMMNode ret = fromNode;
 		int i2, j2, k2;
 		do
 		{
 			if (null == ret.parent)
 				break;
 			ret = ret.parent;
 			int mask = ~(ret.size - 1);
 			i2 = i1 & mask;
 			j2 = j1 & mask;
 			k2 = k1 & mask;
 		}
 		while (i2 != ret.i0 || j2 != ret.j0 || k2 != ret.k0);
 		//  Now find the deepest matching octant.
 		int s = ret.size;
 		while (s > fromNode.size)
 		{
 			s >>= 1;
 			assert s > 0;
 			int ind = indexSubOctree(s, ijk);
 			if (null == ret.child[ind])
 				return null;
 			ret = ret.child[ind];
 		}
 		assert (i1 == ret.i0 && j1 == ret.j0 && k1 == ret.k0);
 		return ret;
 	}
 	
 	/**
 	 * Returns coordinates of all cell corners.
 	 *
 	 * @param onlyLeaves  if set to <code>true</code>, only leaf cells are
 	 * considered, otherwise all cells are considered.
 	 * @return  an array containing corners coordinates
 	 */
 	public double [] getCoords(boolean onlyLeaves)
 	{
 		CoordProcedure proc = new CoordProcedure(onlyLeaves, nr_cells, nr_leaves);
 		walk(proc);
 		return proc.coord;
 	}
 	
 	private final class CoordProcedure extends TraversalProcedure
 	{
 		public final double [] coord;
 		private int index;
 		private boolean onlyLeaves;
 		public CoordProcedure(boolean b, int nC, int nL)
 		{
 			onlyLeaves = b;
 			if (onlyLeaves)
 				coord = new double[72*nL];
 			else
 				coord = new double[72*nC];
 		}
 		public final int action(OEMM oemm, OEMMNode current, int octant, int visit)
 		{
 			if (visit != PREORDER && visit != LEAF)
 				return OK;
 			if (onlyLeaves && !current.isLeaf)
 				return OK;
 			int [] ii = { current.i0, current.j0, current.k0 };
 			double [] p = new double[3];
 			double [] p2 = new double[3];
 			int2double(ii, p);
 			ii[0] += current.size;
 			int2double(ii, p2);
 			double ds = p2[0] - p[0];
 			double offset = 0.0;
 			for (int i = 0; i < 2; i++)
 			{
 				//  0xy
 				coord[index]   = p[0];
 				coord[index+1] = p[1];
 				coord[index+2] = p[2]+offset;
 				index += 3;
 				coord[index]   = p[0]+ds;
 				coord[index+1] = p[1];
 				coord[index+2] = p[2]+offset;
 				index += 3;
 				coord[index]   = p[0]+ds;
 				coord[index+1] = p[1]+ds;
 				coord[index+2] = p[2]+offset;
 				index += 3;
 				coord[index]   = p[0];
 				coord[index+1] = p[1]+ds;
 				coord[index+2] = p[2]+offset;
 				index += 3;
 				//  0xz
 				coord[index]   = p[0];
 				coord[index+1] = p[1]+offset;
 				coord[index+2] = p[2];
 				index += 3;
 				coord[index]   = p[0];
 				coord[index+1] = p[1]+offset;
 				coord[index+2] = p[2]+ds;
 				index += 3;
 				coord[index]   = p[0]+ds;
 				coord[index+1] = p[1]+offset;
 				coord[index+2] = p[2]+ds;
 				index += 3;
 				coord[index]   = p[0]+ds;
 				coord[index+1] = p[1]+offset;
 				coord[index+2] = p[2];
 				index += 3;
 				//  0yz
 				coord[index]   = p[0]+offset;
 				coord[index+1] = p[1];
 				coord[index+2] = p[2];
 				index += 3;
 				coord[index]   = p[0]+offset;
 				coord[index+1] = p[1]+ds;
 				coord[index+2] = p[2];
 				index += 3;
 				coord[index]   = p[0]+offset;
 				coord[index+1] = p[1]+ds;
 				coord[index+2] = p[2]+ds;
 				index += 3;
 				coord[index]   = p[0]+offset;
 				coord[index+1] = p[1];
 				coord[index+2] = p[2]+ds;
 				index += 3;
 				offset += ds;
 			}
 			return OK;
 		}
 	}
 	
 }
