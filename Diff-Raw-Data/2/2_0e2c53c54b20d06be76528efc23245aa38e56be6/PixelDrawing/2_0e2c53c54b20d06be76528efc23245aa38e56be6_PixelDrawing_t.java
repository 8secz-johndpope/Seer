 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: PixelDrawing.java
  *
  * Copyright (c) 2003 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Electric(tm) is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Electric(tm); see the file COPYING.  If not, write to
  * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  * Boston, Mass 02111-1307, USA.
  */
 package com.sun.electric.tool.user.ui;
 
 import com.sun.electric.database.geometry.DBMath;
 import com.sun.electric.database.geometry.EGraphics;
 import com.sun.electric.database.geometry.Poly;
 import com.sun.electric.database.hierarchy.Cell;
 import com.sun.electric.database.hierarchy.Export;
 import com.sun.electric.database.hierarchy.View;
 import com.sun.electric.database.prototype.ArcProto;
 import com.sun.electric.database.prototype.NodeProto;
 import com.sun.electric.database.prototype.PortProto;
 import com.sun.electric.database.topology.ArcInst;
 import com.sun.electric.database.topology.Connection;
 import com.sun.electric.database.topology.NodeInst;
 import com.sun.electric.database.topology.PortInst;
 import com.sun.electric.database.variable.FlagSet;
 import com.sun.electric.database.variable.TextDescriptor;
 import com.sun.electric.database.text.TextUtils;
 import com.sun.electric.technology.Layer;
 import com.sun.electric.technology.PrimitiveArc;
 import com.sun.electric.technology.PrimitiveNode;
 import com.sun.electric.technology.Technology;
 import com.sun.electric.technology.technologies.Generic;
 import com.sun.electric.tool.user.User;
 
 import java.awt.Color;
 import java.awt.Dimension;
 import java.awt.Font;
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.awt.Point;
 import java.awt.Rectangle;
 import java.awt.RenderingHints;
 import java.awt.font.FontRenderContext;
 import java.awt.font.GlyphVector;
 import java.awt.font.LineMetrics;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.Point2D;
 import java.awt.geom.Rectangle2D;
 import java.awt.image.BufferedImage;
 import java.awt.image.DataBufferByte;
 import java.awt.image.DataBufferInt;
 import java.awt.image.Raster;
 import java.awt.image.WritableRaster;
 import java.util.HashMap;
 import java.util.Iterator;
 
 
 /**
  * This class manages an offscreen display for an associated EditWindow.
  * It renders an Image for copying to the display.
  * <P>
  * Every offscreen display consists of two parts: the transparent layers and the opaque image.
  * To tell how a layer is displayed, look at the "transparentLayer" field of its "EGraphics" object.
  * When this is nonzero, the layer is drawn transparent.
  * When this is zero, use the "red, green, blue" fields for the opaque color.
  * <P>
  * The opaque image is a full-color Image that is the size of the EditWindow.
  * Any layers that are marked "opaque" are drawin in full color in the image.
  * Colors are not combined in the opaque image: every color placed in it overwrites the previous color.
  * For this reason, opaque colors are often stipple patterns, so that they won't completely obscure other
  * opaque layers.
  * <P>
  * The transparent layers are able to combine with each other.
  * Typically, the more popular layers are made transparent (metal, poly, active, etc.)
  * For every transparent layer, there is a 1-bit deep bitmap that is the size of the EditWindow.
  * The bitmap is an array of "byte []" pointers, one for every Y coordinate in the EditWindow.
  * Each array contains the bits for that row, packed 8 per byte.
  * All of this information is in the "layerBitMaps" field, which is triply indexed.
  * <P>
  * Thus, to find bit (x,y) of transparent layer T, first lookup the appropriate transparent layer,
  * ("layerBitMaps[T]").
  * Then, for that layer, find the array of bytes for the appropriate row
  * (by indexing the the Y coordinate into the rowstart array, "layerBitMaps[T][y]").
  * Next, figure out which byte has the bit (by dividing the X coordinate by 8: "layerBitMaps[T][y][x>>3]").
  * Finally, determine which bit to use (by using the low 3 bits of the X coordinate,
  * layerBitMaps[T][y][x>>3] & (1 << (x&7)) ).
  * <P>
  * Transparent layers are not allocated until needed.  Thus, if there are 5 possible transparent layers,
  * but only 2 are used, then only two bitplanes will be created.
  * <P>
  * Each technology declares the number of possible transparent layers that it can generate.
  * In addition, it must provide a color map for describing every combination of transparent layer.
  * This map is, of course, 2-to-the-number-of-possible-transparent-layers long.
  * <P>
  * When all rendering is done, the full-color image is composited with the transparent layers to produce
  * the final image.
  * This is done by scanning the full-color image for any entries that were not filled-in.
  * These are then replaced by the transparent color at that point.
  * The transparent color is computed by looking at the bits in every transparent bitmap and
  * constructing an index.  This is looked-up in the color table and the appropriate color is used.
  * If no transparent layers are set, the background color is used.
  * <P>
  * There are a number of efficiencies implemented here.
  * <UL>
  * <LI><B>Setting bits directly into the offscreen memory</B>.
  * Although Java's Swing package has a rendering model, it was found to be 3 times slower than
  * setting bits directly inot the offscreen memory.</LI>
  * <LI><B>Tiny nodes and arcs are approximated</B>.
  * When a node or arc will be only 1 or 2 pixels in size on the screen, it is not necessary
  * to actually compute the edges of all of its parts.  Instead, a single pixel of color is placed.
  * The color is taken from all of the layers that compose the node or arc.
  * This optimization adds another factor of 2 to the speed of display.</LI>
  * <LI><B>Expanded cell contents are cached</B>.
  * When a cell is expanded, and its contents is drawn, the contents are preserved so that they
  * need be rendered only once.  Subsequent instances of that expanded cell are able to be instantly drawn.
  * There are a number of extra considerations here:
  *   <UL>
  *   <LI>Cell instances can appear in any orientation.  Therefore, the cache of drawn cells must
  *   include the orientation.</LI>
  *   <LI>Cell instances may appear at different levels of the hierarchy.  Therefore, it is not
  *   sufficient to merely remember their location on the screen and copy them.  An instance may have been
  *   rendered at one level of hierarchy, and other items at that same level then rendered over it.
  *   It is then no longer possible to copy those bits when the instance appears again at another place
  *   in the hierarchy because it has been altered by neighboring circuitry.  The same problem happens
  *   when cell instances overlap.  Therefore, it is necessary to render each expanded cell instance
  *   into its own offscreen map.  To do this, a new "EditWindow" with associated "PixelDrawing"
  *   object are created for each cached cell.</LI>
  *   <LI>Subpixel alignment may not be the same for each cached instance.  This turns out not to be
  *   a problem, because at such zoomed-out scales, it is impossible to see individual objects anyway.</LI>
  *   <LI>Large cell instances should not be cached.  When zoomed-in, an expanded cell instance could
  *   be many megabytes in size, and only a portion of it appears on the screen.  Therefore, large cell
  *   instances are not cached, but drawn directly.  It is assumed that there will be few such instances.
  *   The rule currently is that any cell whose width is greater than half of the display size AND whose
  *   height is greater than half of the display size is too large to cache.</LI>
  *   <LI>If an instance only appears once, it is not cached.  This requires a preprocessing step to scan
  *   the hierarchy and count the number of times that a particular cell-transformation is used.  During
  *   rendering, if the count is only 1, it is not cached.</LI>
  *   <LI>Texture patterns don't line-up.  When drawing texture pattern to the final buffer, it is easy
  *   to use the screen coordinates to index the pattern map, causeing all of them to line-up.
  *   Any two adjoining objects that use the same pattern will have their patterns line-up smoothly.
  *   However, when caching cell instances, it is not possible to know where the contents will be placed
  *   on the screen, and so the texture patterns rendered into the cache cannot be aligned globally.
  *   To solve this, there are additional bitmaps created for every Patterned-Opaque-Layer (POL).
  *   When rendering on a layer that is patterned and opaque, the bitmap is dynamically allocated
  *   and filled (all bits are filled on the bitmap, not just those in the pattern).
  *   When combining lower-level cell images with higher-level ones, these POLs are copied, too.
  *   When compositing at the top level, however, the POLs are converted back to patterns, and they now line-up.</LI>
  *   </UL>
  * </UL>
  * 
  */
 public class PixelDrawing
 {
 	/** Text smaller than this will not be drawn. */		public static final int MINIMUMTEXTSIZE = 5;
 
 	private static class PolySeg
 	{
 		int fx,fy, tx,ty, direction, increment;
 		PolySeg nextedge;
 		PolySeg nextactive;
 	}
 
 	// statistics stuff
 	private static final boolean TAKE_STATS = false;
 	private static int tinyCells, tinyPrims, totalCells, totalPrims, tinyArcs, totalArcs, offscreensCreated, offscreensUsed;
 
 	/**
 	 * This class holds information about expanded cell instances.
 	 * For efficiency, Electric remembers the bits in an expanded cell instance
 	 * and uses them when another expanded instance appears elsewhere.
 	 * Of course, the orientation of the instance matters, so each combination of
 	 * cell and orientation forms a "cell cache".  The Cell Cache is stored in the
 	 * "wnd" field (which has its own PixelDrawing object).
 	 */
 	private static class ExpandedCellInfo
 	{
 		int instanceCount;
 		EditWindow wnd;
 	}
 
 	/** the EditWindow being drawn */						private EditWindow wnd;
 	/** the size of the EditWindow */						private Dimension sz;
     /** the area of the cell to draw, in DB units */        private Rectangle2D drawBounds;
 
 	// the full-depth image
     /** the offscreen opaque image of the window */			private Image img;
 	/** opaque layer of the window */						private int [] opaqueData;
 	/** size of the opaque layer of the window */			private int total;
 	/** the background color of the offscreen image */		private int backgroundColor;
 	/** the "unset" color of the offscreen image */			private int backgroundValue;
 
 	// the transparent bitmaps
 	/** the offscreen maps for transparent layers */		private byte [][][] layerBitMaps;
 	/** row pointers for transparent layers */				private byte [][] compositeRows;
 	/** the number of transparent layers */					private int numLayerBitMaps;
 	/** the number of bytes per row in offscreen maps */	private int numBytesPerRow;
 	/** the number of offscreen transparent maps made */	private int numLayerBitMapsCreated;
 	/** the technology of the window */						private Technology curTech;
 
 	// the patterned opaque bitmaps
 	private static class PatternedOpaqueLayer
 	{
 		byte [][] bitMap;
 		byte [] compositeRow;
 	}
 	/** the map from layers to Patterned Opaque bitmaps */	private HashMap patternedOpaqueLayers;
 	/** the top-level window being rendered */				private boolean renderedWindow;
 
 	/** whether to occasionally update the display. */		private boolean periodicRefresh;
 	/** keeps track of when to update the display. */		private int objectCount;
 	/** keeps track of when to update the display. */		private long lastRefreshTime;
 
 	/** the size of the top-level EditWindow */				private static Dimension topSz;
 	/** the last Technology that had transparent layers */	private static Technology techWithLayers = null;
 	/** list of cell expansions. */							private static HashMap expandedCells = null;
 	/** TextDescriptor for empty window text. */			private static TextDescriptor noCellTextDescriptor = null;
 	/** zero rectangle */									private static final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
 	private static EGraphics blackGraphics = new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 0,0,0, 1.0,1,
 		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
 	private static EGraphics portGraphics = new EGraphics(EGraphics.SOLID, EGraphics.SOLID, 0, 255,0,0, 1.0,1,
 		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
 
 
     private static final boolean DEBUGRENDERTIMING = false;
     private static long renderTextTime;
     private static long renderPolyTime;
 
     // ************************************* TOP LEVEL *************************************
 
 	/**
 	 * Constructor creates an offscreen PixelDrawing object for a given EditWindow.
 	 * @param wnd the EditWindow associated with this PixelDrawing.
 	 */
 	public PixelDrawing(EditWindow wnd)
 	{
 		this.wnd = wnd;
 		this.sz = wnd.getScreenSize();
 
 		// allocate pointer to the opaque image
 		img = new BufferedImage(sz.width, sz.height, BufferedImage.TYPE_INT_RGB);
 		WritableRaster raster = ((BufferedImage)img).getRaster();
 		DataBufferInt dbi = (DataBufferInt)raster.getDataBuffer();
 		opaqueData = dbi.getData();
 		total = sz.height * sz.width;
 		numBytesPerRow = (sz.width + 7) / 8;
 		backgroundColor = User.getColorBackground() & 0xFFFFFF;
 		backgroundValue = backgroundColor | 0xFF000000;
 		patternedOpaqueLayers = new HashMap();
 		renderedWindow = true;
 
 		curTech = null;
 		initForTechnology();
 
 		// initialize the data
 		clearImage(false);
 	}
 
 	/**
 	 * Method to override the background color.
 	 * Must be called before "drawImage()".
 	 * This is used by printing, which forces the background to be white.
 	 * @param bg the background color to use.
 	 */
 	public void setBackgroundColor(Color bg)
 	{
 		backgroundColor = bg.getRGB() & 0xFFFFFF;
 	}
 
 	/**
 	 * Method for obtaining the rendered image after "drawImage" has finished.
 	 * @return an Image for this edit window.
 	 */
 	public Image getImage() { return img; }
 
 	/**
 	 * This is the entrypoint for rendering.
 	 * It displays a cell in this offscreen window.
 	 * @param expandBounds the area in the cell to expand fully.
 	 * If null, draw the cell normally; otherwise expand all the way in that area.
 	 * The rendered Image can then be obtained with "getImage()".
 	 */
 	public void drawImage(Rectangle2D expandBounds)
 	{
 		Cell cell = wnd.getCell();
         drawBounds = wnd.getDisplayedBounds();
 		long startTime = 0;
 //		if (TAKE_STATS)
 //		{
 //			startTime = System.currentTimeMillis();
 //			tinyCells = tinyPrims = totalCells = totalPrims = tinyArcs = totalArcs = offscreensCreated = offscreensUsed = 0;
 //		}
 
 		// initialize the cache of expanded cell displays
 		expandedCells = new HashMap();
 
 		// remember the true window size (since recursive calls may cache individual cells that are smaller)
 		topSz = sz;
 
 		// initialize rendering into the offscreen image
 		clearImage(true);
 
 		if (cell == null)
 		{
 			if (noCellTextDescriptor == null)
 			{
 				noCellTextDescriptor = new TextDescriptor(null);
 				noCellTextDescriptor.setAbsSize(18);
 				noCellTextDescriptor.setBold(true);
 			}
 			Rectangle rect = new Rectangle(sz);
 			blackGraphics.setColor(new Color(User.getColorText()));
 			drawText(rect, Poly.Type.TEXTBOX, noCellTextDescriptor, "No cell in this window", null, blackGraphics);
 		} else
 		{
 			// determine which cells should be cached (must have at least 2 instances)
 			countCell(cell, DBMath.MATID);
 
 			// now render it all
 			drawCell(cell, expandBounds, DBMath.MATID, true);
 		}
 
 		// merge transparent image into opaque one
 		synchronized(img) { composite(); };
 
 //		if (TAKE_STATS)
 //		{
 //			long endTime = System.currentTimeMillis();
 //			System.out.println("Took "+com.sun.electric.database.text.TextUtils.getElapsedTime(endTime-startTime));
 //			System.out.println("   "+tinyCells+" out of "+totalCells+" cells are tiny; "+tinyPrims+" out of "+totalPrims+
 //				" primitives are tiny; "+tinyArcs+" out of "+totalArcs+" arcs are tiny");
 //			int numExpandedCells = 0;
 //			int numCellsExpandedOnce = 0;
 //			for(Iterator it = expandedCells.keySet().iterator(); it.hasNext(); )
 //			{
 //				String c = (String)it.next();
 //				ExpandedCellInfo count = (ExpandedCellInfo)expandedCells.get(c);
 //				if (count != null)
 //				{
 //					numExpandedCells++;
 //					if (count.instanceCount == 1) numCellsExpandedOnce++;
 //				}
 //			}
 //			System.out.println("   Of "+numExpandedCells+" cell cache possibilities, "+numCellsExpandedOnce+
 //				" were used only once and not cached");
 //			if (offscreensCreated > 0)
 //				System.out.println("   Remaining "+offscreensCreated+" cell caches were used an average of "+
 //					((double)offscreensUsed/offscreensCreated)+" times");
 //		}
 	}
 
 	// ************************************* INTERMEDIATE CONTROL LEVEL *************************************
 
 	/**
 	 * Method to erase the offscreen data in this PixelDrawing.
 	 * This is called before any rendering is done.
 	 * @param periodicRefresh true to periodically refresh the display if it takes too long.
 	 */
 	public void clearImage(boolean periodicRefresh)
 	{
 		// pickup new technology if it changed
 		initForTechnology();
 
 		// erase the transparent bitmaps
 		for(int i=0; i<numLayerBitMaps; i++)
 		{
 			byte [][] layerBitMap = layerBitMaps[i];
 			if (layerBitMap == null) continue;
 			for(int y=0; y<sz.height; y++)
 			{
 				byte [] row = layerBitMap[y];
 				for(int x=0; x<numBytesPerRow; x++)
 					row[x] = 0;
 			}
 		}
 
 		// erase the patterned opaque layer bitmaps
 		for(Iterator it = patternedOpaqueLayers.entrySet().iterator(); it.hasNext(); )
 		{
 			PatternedOpaqueLayer pol = (PatternedOpaqueLayer)it.next();
 			byte [][] layerBitMap = pol.bitMap;
 			for(int y=0; y<sz.height; y++)
 			{
 				byte [] row = layerBitMap[y];
 				for(int x=0; x<numBytesPerRow; x++)
 					row[x] = 0;
 			}
 		}
 
 		// erase opaque image
 		for(int i=0; i<total; i++) opaqueData[i] = backgroundValue;
 
 		this.periodicRefresh = periodicRefresh;
 		if (periodicRefresh)
 		{
 			objectCount = 0;
 			lastRefreshTime = System.currentTimeMillis();
 		}
 	}
 
 	/**
 	 * Method to complete rendering by combining the transparent and opaque imagery.
 	 * This is called after all rendering is done.
 	 * @return the offscreen Image with the final display.
 	 */
 	public Image composite()
 	{
 		// merge in the transparent layers
 		if (numLayerBitMapsCreated > 0)
 		{
 			Color [] colorMap = curTech.getColorMap();
 			for(int y=0; y<sz.height; y++)
 			{
 				for(int i=0; i<numLayerBitMaps; i++)
 				{
 					byte [][] layerBitMap = layerBitMaps[i];
 					if (layerBitMap == null) compositeRows[i] = null; else
 					{
 						compositeRows[i] = layerBitMap[y];
 					}
 				}
 				int baseIndex = y * sz.width;
 				for(int x=0; x<sz.width; x++)
 				{
 					int index = baseIndex + x;
 					int pixelValue = opaqueData[index];
 					
 					// the value of Alpha starts at 0xFF, which means "background"
 					// opaque drawing typically sets it to 0, which means "filled"
 					// Text drawing can antialias by setting the edge values in the range 0-254
 					//    where the lower the value, the more saturated the color (so 0 means all color, 254 means little color)
 					int alpha = (pixelValue >> 24) & 0xFF;
 					if (alpha != 0)
 					{
 						// aggregate the transparent bitplanes at this pixel
 						int bits = 0;
 						int entry = x >> 3;
 						int maskBit = 1 << (x & 7);
 						for(int i=0; i<numLayerBitMaps; i++)
 						{
 							if (compositeRows[i] == null) continue;
 							int byt = compositeRows[i][entry];
 							if ((byt & maskBit) != 0) bits |= (1<<i);
 						}
 
 						// determine the transparent color to draw
 						int newColor = backgroundColor;
 						if (bits != 0)
 						{
 							// set a transparent color
 							newColor = colorMap[bits].getRGB() & 0xFFFFFF;
 						}
 
 						// if alpha blending, merge with the opaque data
 						if (alpha != 0xFF)
 						{
 							newColor = alphaBlend(pixelValue, newColor, alpha);
 						}
 						opaqueData[index] = newColor;
 					}
 				}
 			}
 		} else
 		{
 			// nothing in transparent layers: make sure background color is right
 			for(int i=0; i<total; i++)
 			{
 				int pixelValue = opaqueData[i];
 				if (pixelValue == backgroundValue) opaqueData[i] = backgroundColor; else
 				{
 					if ((pixelValue&0xFF000000) != 0)
 					{
 						int alpha = (pixelValue >> 24) & 0xFF;
 						opaqueData[i] = alphaBlend(pixelValue, backgroundColor, alpha);
 					}
 				}
 			}
 		}
 		return img;
 	}
 
 	private void initForTechnology()
 	{
 		// allocate pointers to the overlappable layers
 		Technology tech = Technology.getCurrent();
 		if (tech == null) return;
 		if (tech == curTech) return;
 		int transLayers = tech.getNumTransparentLayers();
 		if (transLayers != 0)
 		{
 			techWithLayers = curTech = tech;
 		}
 		if (curTech == null) curTech = techWithLayers;
 
 		numLayerBitMaps = curTech.getNumTransparentLayers();
 		layerBitMaps = new byte[numLayerBitMaps][][];
 		compositeRows = new byte[numLayerBitMaps][];
 		for(int i=0; i<numLayerBitMaps; i++) layerBitMaps[i] = null;
 		numLayerBitMapsCreated = 0;
 	}
 
 	// ************************************* HIERARCHY TRAVERSAL *************************************
 
 	/**
 	 * Method to draw the contents of a cell, transformed through "prevTrans".
 	 */
 	private void drawCell(Cell cell, Rectangle2D expandBounds, AffineTransform prevTrans, boolean topLevel)
 	{
         renderPolyTime = 0;
         renderTextTime = 0;
 
 		// draw all arcs
 		for(Iterator arcs = cell.getArcs(); arcs.hasNext(); )
 		{
 			drawArc((ArcInst)arcs.next(), prevTrans);
 		}
 
 		// draw all nodes
 		for(Iterator nodes = cell.getNodes(); nodes.hasNext(); )
 		{
 			drawNode((NodeInst)nodes.next(), prevTrans, topLevel, expandBounds);
 		}
 
 		// show cell variables if at the top level
 		if (topLevel && User.isTextVisibilityOnCell())
 		{
 			// show displayable variables on the instance
 			int numPolys = cell.numDisplayableVariables(true);
 			Poly [] polys = new Poly[numPolys];
 			cell.addDisplayableVariables(CENTERRECT, polys, 0, wnd, true);
 			drawPolys(polys, DBMath.MATID);
 		}
 
         if (DEBUGRENDERTIMING) {
             System.out.println("Total time to render polys: "+TextUtils.getElapsedTime(renderPolyTime));
             System.out.println("Total time to render text: "+TextUtils.getElapsedTime(renderTextTime));
         }
 	}
 
 	/**
 	 * Method to draw a NodeInst into the offscreen image.
 	 * @param ni the NodeInst to draw.
 	 * @param trans the transformation of the NodeInst to the display.
 	 * @param topLevel true if this is the top-level of display (not in a subcell).
      * @param expandBounds bounds in which to draw nodes fully expanded
 	 */
 	public void drawNode(NodeInst ni, AffineTransform trans, boolean topLevel, Rectangle2D expandBounds)
 	{
 		NodeProto np = ni.getProto();
 		AffineTransform localTrans = ni.rotateOut(trans);
 
 		// see if the node is completely clipped from the screen
 		Point2D ctr = ni.getTrueCenter();
 		trans.transform(ctr, ctr);
 		double halfWidth = Math.max(ni.getXSize(), ni.getYSize()) / 2;
 		Rectangle2D databaseBounds = wnd.getDisplayedBounds();
 		double ctrX = ctr.getX();
 		double ctrY = ctr.getY();
 		if (ctrX + halfWidth < databaseBounds.getMinX()) return;
 		if (ctrX - halfWidth > databaseBounds.getMaxX()) return;
 		if (ctrY + halfWidth < databaseBounds.getMinY()) return;
 		if (ctrY - halfWidth > databaseBounds.getMaxY()) return;
 
 		// if the node is tiny, just approximate it with a single dot
 		if (np instanceof Cell) totalCells++; else totalPrims++;
 		if (!np.isCanBeZeroSize() && halfWidth * wnd.getScale() < 1)
 		{
 			if (np instanceof Cell) tinyCells++; else
 			{
 				tinyPrims++;
 
 				// draw a tiny primitive by setting a single dot from each layer
 				int x = wnd.databaseToScreenX(ctrX);
 				int y = wnd.databaseToScreenY(ctrY);
 				if (x >= 0 && x < sz.width && y >= 0 && y < sz.height)
 				{
 					drawTinyLayers(((PrimitiveNode)np).layerIterator(), x, y);
 				}
 			}
 			return;
 		}
 
 		// draw the node
 		if (np instanceof Cell)
 		{
 			// cell instance
 			Cell subCell = (Cell)np;
 			boolean expanded = ni.isExpanded();
 			if (expandBounds != null)
 			{
 				if (ni.getBounds().intersects(expandBounds))
 				{
 					expanded = true;
 					AffineTransform transRI = ni.rotateIn();
 					AffineTransform transTI = ni.translateIn();
 					transRI.preConcatenate(transTI);
 					Rectangle2D subBounds = new Rectangle2D.Double();
 					subBounds.setRect(expandBounds);
 					DBMath.transformRect(subBounds, transRI);
 					expandBounds = subBounds;
 				}
 			}
 
 			// two ways to draw a cell instance
 			if (expanded)
 			{
 				// show the contents of the cell
 				AffineTransform subTrans = ni.translateOut(localTrans);
 
 				if (expandedCellCached(subCell, subTrans, expandBounds)) return;
 
 				// just draw it directly
 				drawCell(subCell, expandBounds, subTrans, false);
 				showCellPorts(ni, trans, Color.BLACK);
 			} else
 			{
 				// draw the black box of the instance
 				drawUnexpandedCell(ni, trans);
 				showCellPorts(ni, trans, null);
 			}
 
 			// draw any displayable variables on the instance
 			if (User.isTextVisibilityOnNode())
 			{
 				int numPolys = ni.numDisplayableVariables(true);
 				Poly [] polys = new Poly[numPolys];
 				Rectangle2D rect = ni.getBounds();
 				ni.addDisplayableVariables(rect, polys, 0, wnd, true);
 				drawPolys(polys, localTrans);
 			}
 		} else
 		{
 			// primitive
 			if (topLevel || !ni.isVisInside())
 			{
 				EditWindow nodeWnd = wnd;
 				PrimitiveNode prim = (PrimitiveNode)np;
 				if (!User.isTextVisibilityOnNode()) nodeWnd = null;
 				if (prim == Generic.tech.invisiblePinNode)
 				{
 					if (!User.isTextVisibilityOnAnnotation()) nodeWnd = null;
 				}
 				Technology tech = prim.getTechnology();
 				Poly [] polys = tech.getShapeOfNode(ni, nodeWnd);
 				drawPolys(polys, localTrans);
 			}
 		}
 
 		// draw any exports from the node
 		if (topLevel && User.isTextVisibilityOnExport())
 		{
 			int exportDisplayLevel = User.getExportDisplayLevel();
 			Iterator it = ni.getExports();
 			blackGraphics.setColor(new Color(User.getColorText()));
 			while (it.hasNext())
 			{
 				Export e = (Export)it.next();
 				Poly poly = e.getNamePoly();
 				Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
 				if (exportDisplayLevel == 2)
 				{
 					// draw port as a cross
 					drawCross(poly, blackGraphics);
 				} else
 				{
 					// draw port as text
 					TextDescriptor descript = poly.getTextDescriptor();
 					Poly.Type type = descript.getPos().getPolyType();
 					String portName = e.getName();
 					if (exportDisplayLevel == 1)
 					{
 						// use shorter port name
 						portName = e.getShortName();
 					}
 					Point pt = wnd.databaseToScreen(poly.getCenterX(), poly.getCenterY());
 					Rectangle textRect = new Rectangle(pt);
 					type = Poly.rotateType(type, ni);
 					drawText(textRect, type, descript, portName, null, blackGraphics);
 				}
 
 				// draw variables on the export
 				int numPolys = e.numDisplayableVariables(true);
 				if (numPolys > 0)
 				{
 					Poly [] polys = new Poly[numPolys];
 					e.addDisplayableVariables(rect, polys, 0, wnd, true);
 					drawPolys(polys, localTrans);
 				}
 			}
 		}
 	}
 
 	/**
 	 * Method to render an ArcInst into the offscreen image.
 	 * @param ai the ArcInst to draw.
 	 * @param trans the transformation of the ArcInst to the display.
 	 */
 	public void drawArc(ArcInst ai, AffineTransform trans)
 	{
         // see if the arc is completely clipped from the screen
 		Rectangle2D arcBounds = ai.getBounds();
         Rectangle2D dbBounds = new Rectangle2D.Double(arcBounds.getX(), arcBounds.getY(), arcBounds.getWidth(), arcBounds.getHeight());
         Poly p = new Poly(dbBounds);
         p.transform(trans);
         dbBounds = p.getBounds2D();
         // java doesn't think they intersect if they contain no area, so bloat width or height if zero
         if (dbBounds.getWidth() == 0 || dbBounds.getHeight() == 0) {
             dbBounds = new Rectangle2D.Double(dbBounds.getX(), dbBounds.getY(),
                     dbBounds.getWidth() + 0.001, dbBounds.getHeight() + 0.001);
         }
        if ((drawBounds != null) && (!drawBounds.intersects(dbBounds))) {
             return;
         }
 /*
         if (drawBounds != null) {
             if (!drawBounds.contains(dbBounds.getX(), dbBounds.getY()) &&
                 !drawBounds.contains(dbBounds.getX(), dbBounds.getY()+dbBounds.getHeight()) &&
                 !drawBounds.contains(dbBounds.getX()+dbBounds.getWidth(), dbBounds.getY()) &&
                 !drawBounds.contains(dbBounds.getX()+dbBounds.getWidth(), dbBounds.getY()+dbBounds.getHeight()))
                 return;
         }
 */
 
         double arcSize = Math.max(arcBounds.getWidth(), arcBounds.getHeight());
 		// if the arc it tiny, just approximate it with a single dot
 		totalArcs++;
 		if (arcSize * wnd.getScale() < 2)
 		{
 			tinyArcs++;
 
 			// draw a tiny arc by setting a single dot from each layer
 			Point2D ctr = ai.getTrueCenter();
 			trans.transform(ctr, ctr);
 			int x = wnd.databaseToScreenX(ctr.getX());
 			int y = wnd.databaseToScreenY(ctr.getY());
 			if (x >= 0 && x < sz.width && y >= 0 && y < sz.height)
 			{
 				PrimitiveArc prim = (PrimitiveArc)ai.getProto();
 				drawTinyLayers(prim.layerIterator(), x, y);
 			}
 			return;
 		}
 
 		// draw the arc
 		ArcProto ap = ai.getProto();
 		Technology tech = ap.getTechnology();
 		EditWindow arcWnd = wnd;
 		if (!User.isTextVisibilityOnArc()) arcWnd = null;
 		Poly [] polys = tech.getShapeOfArc(ai, arcWnd);
 		drawPolys(polys, trans);
 	}
 
 	private void showCellPorts(NodeInst ni, AffineTransform trans, Color col)
 	{
 		// show the ports that are not further exported or connected
 		FlagSet fs = PortProto.getFlagSet(1);
 		for(Iterator it = ni.getProto().getPorts(); it.hasNext(); )
 		{
 			PortProto pp = (PortProto)it.next();
 			pp.clearBit(fs);
 		}
 		for(Iterator it = ni.getConnections(); it.hasNext();)
 		{
 			Connection con = (Connection) it.next();
 			PortInst pi = con.getPortInst();
 			pi.getPortProto().setBit(fs);
 		}
 		for(Iterator it = ni.getExports(); it.hasNext();)
 		{
 			Export exp = (Export) it.next();
 			PortInst pi = exp.getOriginalPort();
 			pi.getPortProto().setBit(fs);
 		}
 		int portDisplayLevel = User.getPortDisplayLevel();
 		for(Iterator it = ni.getProto().getPorts(); it.hasNext(); )
 		{
 			PortProto pp = (PortProto)it.next();
 			if (pp.isBit(fs)) continue;
 	
 			Poly portPoly = ni.getShapeOfPort(pp);
 			if (portPoly == null) continue;
 			portPoly.transform(trans);
 			Color portColor = col;
 			if (portColor == null) portColor = pp.colorOfPort();
 			portGraphics.setColor(portColor);
 			if (portDisplayLevel == 2)
 			{
 				// draw port as a cross
 				drawCross(portPoly, portGraphics);
 			} else
 			{
 				// draw port as text
 				if (User.isTextVisibilityOnPort())
 				{
 					TextDescriptor descript = portPoly.getTextDescriptor();
 					Poly.Type type = descript.getPos().getPolyType();
 					String portName = pp.getName();
 					if (portDisplayLevel == 1)
 					{
 						// use shorter port name
 						portName = pp.getShortName();
 					}
 					Point pt = wnd.databaseToScreen(portPoly.getCenterX(), portPoly.getCenterY());
 					Rectangle rect = new Rectangle(pt);
 					drawText(rect, type, descript, portName, null, portGraphics);
 				}
 			}
 		}
 		fs.freeFlagSet();
 	}
 
 	private void drawUnexpandedCell(NodeInst ni, AffineTransform trans)
 	{
 		NodeProto np = ni.getProto();
 
 		// draw the instance outline
 		Poly poly = new Poly(ni.getTrueCenterX(), ni.getTrueCenterY(), ni.getXSize(), ni.getYSize());
 		AffineTransform localPureTrans = ni.rotateOutAboutTrueCenter(trans);
 		poly.transform(localPureTrans);
 		Point2D [] points = poly.getPoints();
 		for(int i=0; i<points.length; i++)
 		{
 			int lastI = i - 1;
 			if (lastI < 0) lastI = points.length - 1;
 			Point from = wnd.databaseToScreen(points[lastI]);
 			Point to = wnd.databaseToScreen(points[i]);
 			blackGraphics.setColor(new Color(User.getColorInstanceOutline()));
 			drawLine(from, to, null, blackGraphics, 0);
 		}
 
 		// draw the instance name
 		if (User.isTextVisibilityOnInstance())
 		{
 			Rectangle2D bounds = poly.getBounds2D();
 			Rectangle rect = wnd.databaseToScreen(bounds);
 			TextDescriptor descript = ni.getProtoTextDescriptor();
 			blackGraphics.setColor(new Color(User.getColorText()));
 			drawText(rect, Poly.Type.TEXTBOX, descript, np.describe(), null, blackGraphics);
 		}
 	}
 
 	private void drawTinyLayers(Iterator layerIterator, int x, int y)
 	{
 		for(Iterator it = layerIterator; it.hasNext(); )
 		{
 			Layer layer = (Layer)it.next();
 			if (layer == null) continue;
 			int layerNum = -1;
 			int col = 0;
 			EGraphics graphics = layer.getGraphics();
 			if (graphics != null)
 			{
 				if (graphics.isPatternedOnDisplay())
 				{
 					int [] pattern = graphics.getPattern();
 					if (pattern != null)
 					{
 						int pat = pattern[y&15];
 						if (pat == 0 || (pat & (0x8000 >> (x&15))) == 0) continue;
 					}
 				}
 				layerNum = graphics.getTransparentLayer() - 1;
 				col = graphics.getColor().getRGB() & 0xFFFFFF;
 			}
 			if (layerNum >= numLayerBitMaps) continue;
 			byte [][] layerBitMap = getLayerBitMap(layerNum);
 
 			// set the bit
 			if (layerBitMap == null)
 			{
 				opaqueData[y * sz.width + x] = col;
 			} else
 			{
 				layerBitMap[y][x>>3] |= (1 << (x&7));
 			}
 		}
 	}
 
 	// ************************************* CELL CACHING *************************************
 
 	/**
 	 * @return true if the cell is properly handled and need no further processing.
 	 * False to render the contents recursively.
 	 */
 	private boolean expandedCellCached(Cell subCell, AffineTransform subTrans, Rectangle2D bounds)
 	{
 		// if there is no global for remembering cached cells, do not cache
 		if (expandedCells == null) return false;
 
 		// do not cache icons: they can be redrawn each time
 		if (subCell.getView() == View.ICON) return false;
 
 		// find this cell-transformation combination in the global list of cached cells
 		String expandedName = makeExpandedName(subCell, subTrans);
 		ExpandedCellInfo expandedCellCount = (ExpandedCellInfo)expandedCells.get(expandedName);
 		if (expandedCellCount != null)
 		{
 			// if this combination is not used multiple times, do not cache it
 			if (expandedCellCount.instanceCount < 2) return false;
 		}
 
 		// compute where this cell lands on the screen
 		Rectangle2D cellBounds = new Rectangle2D.Double();
 		cellBounds.setRect(subCell.getBounds());
 		Poly poly = new Poly(cellBounds);
 		poly.transform(subTrans);
 		Rectangle screenBounds = new Rectangle(wnd.databaseToScreen(poly.getBounds2D()));
 		if (screenBounds.width <= 0 || screenBounds.height <= 0) return true;
 
 		// do not cache if the cell is too large (creates immense offscreen buffers)
 		if (screenBounds.width >= topSz.width/2 && screenBounds.height >= topSz.height/2)
 			return false;
 
 		// if this is the first use, create the offscreen buffer
 		if (expandedCellCount == null)
 		{
 			expandedCellCount = new ExpandedCellInfo();
 			expandedCellCount.instanceCount = 0;
 			expandedCellCount.wnd = null;
 			expandedCells.put(expandedName, expandedCellCount);
 		}
 
 		// render into the offscreen buffer (on the first time)
 		EditWindow renderedCell = expandedCellCount.wnd;
 		if (renderedCell == null)
 		{
 			renderedCell = EditWindow.CreateElectricDoc(subCell, null);
 			expandedCellCount.wnd = renderedCell;
 			renderedCell.setScreenSize(new Dimension(screenBounds.width, screenBounds.height));
 			renderedCell.setScale(wnd.getScale());
 
 			renderedCell.getOffscreen().clearImage(true);
 			Point2D cellCtr = new Point2D.Double(cellBounds.getCenterX(), cellBounds.getCenterY());
 			subTrans.transform(cellCtr, cellCtr);
 			renderedCell.setOffset(cellCtr);
 
 			// render the contents of the expanded cell into its own offscreen cache
 			renderedCell.getOffscreen().renderedWindow = false;
 			renderedCell.getOffscreen().drawCell(subCell, bounds, subTrans, false);
 			offscreensCreated++;
 		}
 
 		// copy out of the offscreen buffer into the main buffer
 		copyBits(renderedCell, screenBounds);
 		offscreensUsed++;
 		return true;
 	}
 
 	/**
 	 * Recursive method to count the number of times that a cell-transformation is used
 	 */
 	private void countCell(Cell cell, AffineTransform prevTrans)
 	{
 		// look for subcells
 		for(Iterator nodes = cell.getNodes(); nodes.hasNext(); )
 		{
 			NodeInst ni = (NodeInst)nodes.next();
 			if (!(ni.getProto() instanceof Cell)) continue;
 			countNode(ni, prevTrans);
 		}
 	}
 
 	/**
 	 * Recursive method to count the number of times that a cell-transformation is used
 	 */
 	private void countNode(NodeInst ni, AffineTransform trans)
 	{
 		NodeProto np = ni.getProto();
 		Cell subCell = (Cell)np;
 
 		// if the node is tiny, it will be approximated
 		double halfWidth = Math.max(ni.getXSize(), ni.getYSize()) / 2;
 		if (halfWidth * wnd.getScale() < 1) return;
 
 		// see if the node is completely clipped from the screen
 		Point2D ctr = ni.getTrueCenter();
 		trans.transform(ctr, ctr);
 		Rectangle2D databaseBounds = wnd.getDisplayedBounds();
 		double ctrX = ctr.getX();
 		double ctrY = ctr.getY();
 		if (ctrX + halfWidth < databaseBounds.getMinX()) return;
 		if (ctrX - halfWidth > databaseBounds.getMaxX()) return;
 		if (ctrY + halfWidth < databaseBounds.getMinY()) return;
 		if (ctrY - halfWidth > databaseBounds.getMaxY()) return;
 
 		// only interested in expanded instances
 		if (!ni.isExpanded()) return;
 
 		// transform into the subcell
 		AffineTransform localTrans = ni.rotateOut(trans);
 		AffineTransform subTrans = ni.translateOut(localTrans);
 
 		// compute where this cell lands on the screen
 		Rectangle2D cellBounds = subCell.getBounds();
 		Poly poly = new Poly(cellBounds);
 		poly.transform(subTrans);
 		Rectangle screenBounds = wnd.databaseToScreen(poly.getBounds2D());
 		if (screenBounds.width <= 0 || screenBounds.height <= 0) return;
 		if (screenBounds.width < sz.width/2 || screenBounds.height <= sz.height/2)
 		{
 			// construct the cell name that combines with the transformation
 			String expandedName = makeExpandedName(subCell, subTrans);
 			ExpandedCellInfo expansionCount = (ExpandedCellInfo)expandedCells.get(expandedName);
 			if (expansionCount == null)
 			{
 				expansionCount = new ExpandedCellInfo();
 				expansionCount.instanceCount = 1;
 				expandedCells.put(expandedName, expansionCount);
 			} else
 			{
 				expansionCount.instanceCount++;
 				return;
 			}
 		}
 
 		// now recurse
 		countCell(subCell, subTrans);
 	}
 
 	/**
 	 * Method to construct a string that describes a transformation of a cell.
 	 * Appends the upper-left 2x2 part of the transformation matrix to the cell name.
 	 * Scaling by 100 and making it an integer ensures that similar transformation
 	 * matrices get merged into one name.
 	 */
 	private String makeExpandedName(Cell subCell, AffineTransform subTrans)
 	{
 		int t00 = (int)(subTrans.getScaleX() * 100);
 		int t01 = (int)(subTrans.getShearX() * 100);
 		int t10 = (int)(subTrans.getShearY() * 100);
 		int t11 = (int)(subTrans.getScaleY() * 100);
 		String expandedName = subCell.describe() + " " + t00 + " " + t01 + " " + t10 + " " + t11;
 		return expandedName;
 	}
 
 	/**
 	 * Method to copy the offscreen bits for a cell into the offscreen bits for the entire screen.
 	 */
 	private void copyBits(EditWindow renderedCell, Rectangle screenBounds)
 	{
 		PixelDrawing srcOffscreen = renderedCell.getOffscreen();
 		if (srcOffscreen == null) return;
 //		if (srcOffscreen.layerBitMaps == null)
 //		{
 //			System.out.println("Null bitmaps, at "+screenBounds);
 //			return;
 //		}
 		Dimension dim = srcOffscreen.sz;
 
 		// copy the opaque and transparent layers
 		for(int srcY=0; srcY<dim.height; srcY++)
 		{
 			int destY = srcY + screenBounds.y;
 			if (destY < 0 || destY >= sz.height) continue;
 			int srcBase = srcY * dim.width;
 			int destBase = destY * sz.width;
 
 			for(int srcX=0; srcX<dim.width; srcX++)
 			{
 				int destX = srcX + screenBounds.x;
 				if (destX < 0 || destX >= sz.width) continue;
 				int srcColor = srcOffscreen.opaqueData[srcBase + srcX];
 				if (srcColor != backgroundValue)
 					opaqueData[destBase + destX] = srcColor;
 				for(int i=0; i<numLayerBitMaps; i++)
 				{
 					byte [][] srcLayerBitMap = srcOffscreen.layerBitMaps[i];
 					if (srcLayerBitMap == null) continue;
 					byte [] srcRow = srcLayerBitMap[srcY];
 
 					byte [][] destLayerBitMap = getLayerBitMap(i);
 					byte [] destRow = destLayerBitMap[destY];
 					if ((srcRow[srcX>>3] & (1<<(srcX&7))) != 0)
 						destRow[destX>>3] |= (1 << (destX&7));
 				}
 			}
 		}
 
 		// copy the patterned opaque layers
 		for(Iterator it = srcOffscreen.patternedOpaqueLayers.keySet().iterator(); it.hasNext(); )
 		{
 			Layer layer = (Layer)it.next();
 			PatternedOpaqueLayer polSrc = (PatternedOpaqueLayer)srcOffscreen.patternedOpaqueLayers.get(layer);
 			byte [][] srcLayerBitMap = polSrc.bitMap;
 			if (srcLayerBitMap == null) continue;
 
 			if (renderedWindow)
 			{
 				// this is the top-level of display: convert patterned opaque to patterns
 				EGraphics desc = layer.getGraphics();
 				int col = desc.getColor().getRGB() & 0xFFFFFF;
 				int [] pattern = desc.getPattern();
 
 				// setup pattern for this row
 				for(int srcY=0; srcY<dim.height; srcY++)
 				{
 					int destY = srcY + screenBounds.y;
 					if (destY < 0 || destY >= sz.height) continue;
 					int destBase = destY * sz.width;
 					int pat = pattern[destY&15];
 					if (pat == 0) continue;
 					byte [] srcRow = srcLayerBitMap[srcY];
 					for(int srcX=0; srcX<dim.width; srcX++)
 					{
 						int destX = srcX + screenBounds.x;
 						if (destX < 0 || destX >= sz.width) continue;
 						if ((srcRow[srcX>>3] & (1<<(srcX&7))) != 0)
 						{
 							if ((pat & (0x8000 >> (destX&15))) != 0)
 								opaqueData[destBase + destX] = col;
 						}
 					}
 				}
 			} else
 			{
 				// a lower level being copied to a low level: just copy the patterned opaque layers
 				PatternedOpaqueLayer polDest = (PatternedOpaqueLayer)patternedOpaqueLayers.get(layer);
 				if (polDest == null)
 				{
 					polDest = new PatternedOpaqueLayer();
 					polDest.bitMap = new byte[sz.height][];
 					for(int y=0; y<sz.height; y++)
 					{
 						byte [] row = new byte[numBytesPerRow];
 						for(int x=0; x<numBytesPerRow; x++) row[x] = 0;
 						polDest.bitMap[y] = row;
 					}
 					patternedOpaqueLayers.put(layer, polDest);
 				}
 				byte [][] destLayerBitMap = polDest.bitMap;
 				for(int srcY=0; srcY<dim.height; srcY++)
 				{
 					int destY = srcY + screenBounds.y;
 					if (destY < 0 || destY >= sz.height) continue;
 					int destBase = destY * sz.width;
 					byte [] srcRow = srcLayerBitMap[srcY];
 					byte [] destRow = destLayerBitMap[destY];
 					for(int srcX=0; srcX<dim.width; srcX++)
 					{
 						int destX = srcX + screenBounds.x;
 						if (destX < 0 || destX >= sz.width) continue;
 						if ((srcRow[srcX>>3] & (1<<(srcX&7))) != 0)
 							destRow[destX>>3] |= (1 << (destX&7));
 					}
 				}
 			}
 		}
 	}
 
 	// ************************************* RENDERING POLY SHAPES *************************************
 
 	/**
 	 * Method to draw polygon "poly", transformed through "trans".
 	 */
 	private void drawPolys(Poly [] polys, AffineTransform trans)
 	{
 		if (polys == null) return;
 		for(int i = 0; i < polys.length; i++)
 		{
 			// get the polygon and transform it
 			Poly poly = polys[i];
 			if (poly == null) continue;
 			Layer layer = poly.getLayer();
 			EGraphics graphics = null;
 			if (layer != null)
 			{
 				if (!layer.isVisible()) continue;
 				graphics = layer.getGraphics();
 			}
 
 			// transform the bounds
 			poly.transform(trans);
 
 			// render the polygon
             long startTime = System.currentTimeMillis();
 			renderPoly(poly, graphics);
             renderPolyTime += (System.currentTimeMillis() - startTime);
 
 			// handle refreshing
 			if (periodicRefresh)
 			{
 				objectCount++;
 				if (objectCount > 100)
 				{
 					objectCount = 0;
 					long currentTime = System.currentTimeMillis();
 					if (currentTime - lastRefreshTime > 1000)
 					{
 						lastRefreshTime = currentTime;
 						wnd.repaint();
 					}
 				}
 			}
 		}
 	}
 
 	private byte [][] getLayerBitMap(int layerNum)
 	{
 		if (layerNum < 0) return null;
 
 		byte [][] layerBitMap = layerBitMaps[layerNum];
 		if (layerBitMap == null)
 		{
 			 // allocate this bitplane dynamically
 			layerBitMap = new byte[sz.height][];
 			for(int y=0; y<sz.height; y++)
 			{
 				byte [] row = new byte[numBytesPerRow];
 				for(int x=0; x<numBytesPerRow; x++) row[x] = 0;
 				layerBitMap[y] = row;
 			}
 			layerBitMaps[layerNum] = layerBitMap;
 			numLayerBitMapsCreated++;
 		}
 		return layerBitMap;
 	}
 
 	/**
 	 * Render a Poly to the offscreen buffer.
 	 */
 	private void renderPoly(Poly poly, EGraphics graphics)
 	{
 		int layerNum = -1;
 		if (graphics != null) layerNum = graphics.getTransparentLayer() - 1;
 		if (layerNum >= numLayerBitMaps) return;
 		byte [][] layerBitMap = getLayerBitMap(layerNum);
 		Poly.Type style = poly.getStyle();
 
 		// only do this for lower-level (cached cells)
 		if (!renderedWindow)
 		{
 			// for fills, handle patterned opaque layers specially
 			if (style == Poly.Type.FILLED || style == Poly.Type.DISC)
 			{
 				// see if it is opaque
 				if (layerBitMap == null)
 				{
 					// see if it is patterned
 					if (graphics.isPatternedOnDisplay())
 					{
 						Layer layer = poly.getLayer();
 						PatternedOpaqueLayer pol = (PatternedOpaqueLayer)patternedOpaqueLayers.get(layer);
 						if (pol == null)
 						{
 							pol = new PatternedOpaqueLayer();
 							pol.bitMap = new byte[sz.height][];
 							for(int y=0; y<sz.height; y++)
 							{
 								byte [] row = new byte[numBytesPerRow];
 								for(int x=0; x<numBytesPerRow; x++) row[x] = 0;
 								pol.bitMap[y] = row;
 							}
 							patternedOpaqueLayers.put(layer, pol);
 						}
 						layerBitMap = pol.bitMap;
 						graphics = null;
 					}
 				}
 			}
 		}
 
 		// now draw it
 		Point2D [] points = poly.getPoints();
 		if (style == Poly.Type.FILLED)
 		{
 			Rectangle2D bounds = poly.getBox();
 			if (bounds != null)
 			{
 				// convert coordinates
 				int lX = wnd.databaseToScreenX(bounds.getMinX());
 				int hX = wnd.databaseToScreenX(bounds.getMaxX());
 				int hY = wnd.databaseToScreenY(bounds.getMinY());
 				int lY = wnd.databaseToScreenY(bounds.getMaxY());
 
 				// do clipping
 				if (lX < 0) lX = 0;
 				if (hX >= sz.width) hX = sz.width-1;
 				if (lY < 0) lY = 0;
 				if (hY >= sz.height) hY = sz.height-1;
 				if (lX > hX || lY > hY) return;
 
 				// draw the box
 				drawBox(lX, hX, lY, hY, layerBitMap, graphics);
 				return;
 			}
 			Point [] intPoints = new Point[points.length];
 			for(int i=0; i<points.length; i++)
 				intPoints[i] = wnd.databaseToScreen(points[i]);
 			Point [] clippedPoints = clipPoly(intPoints, 0, sz.width-1, 0, sz.height-1);
 			drawPolygon(clippedPoints, layerBitMap, graphics);
 			return;
 		}
 		if (style == Poly.Type.CROSSED)
 		{
 			Point pt0a = wnd.databaseToScreen(points[0]);
 			Point pt1a = wnd.databaseToScreen(points[1]);
 			Point pt2a = wnd.databaseToScreen(points[2]);
 			Point pt3a = wnd.databaseToScreen(points[3]);
 			Point pt0b = new Point(pt0a);   Point pt0c = new Point(pt0a);
 			Point pt1b = new Point(pt1a);   Point pt1c = new Point(pt1a);
 			Point pt2b = new Point(pt2a);   Point pt2c = new Point(pt2a);
 			Point pt3b = new Point(pt3a);   Point pt3c = new Point(pt3a);
 			drawLine(pt0a, pt1a, layerBitMap, graphics, 0);
 			drawLine(pt1b, pt2a, layerBitMap, graphics, 0);
 			drawLine(pt2b, pt3a, layerBitMap, graphics, 0);
 			drawLine(pt3b, pt0b, layerBitMap, graphics, 0);
 			drawLine(pt0c, pt2c, layerBitMap, graphics, 0);
 			drawLine(pt1c, pt3c, layerBitMap, graphics, 0);
 			return;
 		}
 		if (style.isText())
 		{
 			Rectangle2D bounds = poly.getBounds2D();
 			Rectangle rect = wnd.databaseToScreen(bounds);
 			TextDescriptor descript = poly.getTextDescriptor();
 			String str = poly.getString();
 			drawText(rect, style, descript, str, layerBitMap, graphics);
 			return;
 		}
 		if (style == Poly.Type.CLOSED || style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 ||
 			style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3)
 		{
 			int lineType = 0;
 			if (style == Poly.Type.OPENEDT1) lineType = 1; else
 			if (style == Poly.Type.OPENEDT2) lineType = 2; else
 			if (style == Poly.Type.OPENEDT3) lineType = 3;
 
 			for(int j=1; j<points.length; j++)
 			{
 				Point pt1 = wnd.databaseToScreen(points[j-1]);
 				Point pt2 = wnd.databaseToScreen(points[j]);
 				drawLine(pt1, pt2, layerBitMap, graphics, lineType);
 			}
 			if (style == Poly.Type.CLOSED)
 			{
 				Point pt1 = wnd.databaseToScreen(points[points.length-1]);
 				Point pt2 = wnd.databaseToScreen(points[0]);
 				drawLine(pt1, pt2, layerBitMap, graphics, lineType);
 			}
 			return;
 		}
 		if (style == Poly.Type.VECTORS)
 		{
 			for(int j=0; j<points.length; j+=2)
 			{
 				Point pt1 = wnd.databaseToScreen(points[j]);
 				Point pt2 = wnd.databaseToScreen(points[j+1]);
 				drawLine(pt1, pt2, layerBitMap, graphics, 0);
 			}
 			return;
 		}
 		if (style == Poly.Type.CIRCLE)
 		{
 			Point center = wnd.databaseToScreen(points[0]);
 			Point edge = wnd.databaseToScreen(points[1]);
 			drawCircle(center, edge, layerBitMap, graphics);
 			return;
 		}
 		if (style == Poly.Type.THICKCIRCLE)
 		{
 			Point center = wnd.databaseToScreen(points[0]);
 			Point edge = wnd.databaseToScreen(points[1]);
 			drawThickCircle(center, edge, layerBitMap, graphics);
 			return;
 		}
 		if (style == Poly.Type.DISC)
 		{
 			Point center = wnd.databaseToScreen(points[0]);
 			Point edge = wnd.databaseToScreen(points[1]);
 			drawDisc(center, edge, layerBitMap, graphics);
 			return;
 		}
 		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
 		{
 			Point center = wnd.databaseToScreen(points[0]);
 			Point edge1 = wnd.databaseToScreen(points[1]);
 			Point edge2 = wnd.databaseToScreen(points[2]);
 			drawCircleArc(center, edge1, edge2, style == Poly.Type.THICKCIRCLEARC, layerBitMap, graphics);
 			return;
 		}
 		if (style == Poly.Type.CROSS)
 		{
 			// draw the cross
 			drawCross(poly, graphics);
 			return;
 		}
 		if (style == Poly.Type.BIGCROSS)
 		{
 			// draw the big cross
 			Point center = wnd.databaseToScreen(points[0]);
 			int size = 5;
 			drawLine(new Point(center.x-size, center.y), new Point(center.x+size, center.y), layerBitMap, graphics, 0);
 			drawLine(new Point(center.x, center.y-size), new Point(center.x, center.y+size), layerBitMap, graphics, 0);
 			return;
 		}
 	}
 
 	// ************************************* BOX DRAWING *************************************
 
 	/**
 	 * Method to draw a box on the off-screen buffer.
 	 */
 	private void drawBox(int lX, int hX, int lY, int hY, byte [][] layerBitMap, EGraphics desc)
 	{
 		// get color and pattern information
 		int col = 0;
 		int [] pattern = null;
 		if (desc != null)
 		{
 			col = desc.getColor().getRGB() & 0xFFFFFF;
 			if (desc.isPatternedOnDisplay())
 				pattern = desc.getPattern();
 		}
 
 		// different code for patterned and solid
 		if (pattern == null)
 		{
 			// solid fill
 			if (layerBitMap == null)
 			{
 				// solid fill in opaque area
 				for(int y=lY; y<=hY; y++)
 				{
 					int baseIndex = y * sz.width + lX;
 					for(int x=lX; x<=hX; x++)
 						opaqueData[baseIndex++] = col;
 				}
 			} else
 			{
 				// solid fill in transparent layers
 				for(int y=lY; y<=hY; y++)
 				{
 					byte [] row = layerBitMap[y];
 					for(int x=lX; x<=hX; x++)
 						row[x>>3] |= (1 << (x&7));
 				}
 			}
 		} else
 		{
 			// patterned fill
 			if (layerBitMap == null)
 			{
 				// patterned fill in opaque area
 				for(int y=lY; y<=hY; y++)
 				{
 					// setup pattern for this row
 					int pat = pattern[y&15];
 					if (pat == 0) continue;
 
 					int baseIndex = y * sz.width;
 					for(int x=lX; x<=hX; x++)
 					{
 						if ((pat & (0x8000 >> (x&15))) != 0)
 							opaqueData[baseIndex + x] = col;
 					}
 				}
 			} else
 			{
 				// patterned fill in transparent layers
 				for(int y=lY; y<=hY; y++)
 				{
 					// setup pattern for this row
 					int pat = pattern[y&15];
 					if (pat == 0) continue;
 
 					byte [] row = layerBitMap[y];
 					for(int x=lX; x<=hX; x++)
 					{
 						if ((pat & (0x8000 >> (x&15))) != 0)
 							row[x>>3] |= (1 << (x&7));
 					}
 				}
 			}
 		}
 	}
 
 	// ************************************* LINE DRAWING *************************************
 
 	/**
 	 * Method to draw a line on the off-screen buffer.
 	 */
 	private void drawLine(Point pt1, Point pt2, byte [][] layerBitMap, EGraphics desc, int texture)
 	{
 		// first clip the line
 		if (clipLine(pt1, pt2, 0, sz.width-1, 0, sz.height-1)) return;
 
 		// now draw with the proper line type
 		switch (texture)
 		{
 			case 0: drawSolidLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, desc);       break;
 			case 1: drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, desc, 0x88);   break;
 			case 2: drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, desc, 0xE7);   break;
 			case 3: drawThickLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, desc);       break;
 		}
 	}
 
 	private void drawCross(Poly poly, EGraphics graphics)
 	{
 		Point2D [] points = poly.getPoints();
 		Point center = wnd.databaseToScreen(points[0]);
 		int size = 3;
 		drawLine(new Point(center.x-size, center.y), new Point(center.x+size, center.y), null, graphics, 0);
 		drawLine(new Point(center.x, center.y-size), new Point(center.x, center.y+size), null, graphics, 0);
 	}
 
 	private void drawSolidLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, EGraphics desc)
 	{
 		// get color and pattern information
 		int col = 0;
 		int [] pattern = null;
 		if (desc != null)
 		{
 			col = desc.getColor().getRGB() & 0xFFFFFF;
 			if (desc.isPatternedOnDisplay())
 				pattern = desc.getPattern();
 		}
 
 		// initialize the Bresenham algorithm
 		int dx = Math.abs(x2-x1);
 		int dy = Math.abs(y2-y1);
 		if (dx > dy)
 		{
 			// initialize for lines that increment along X
 			int incr1 = 2 * dy;
 			int incr2 = 2 * (dy - dx);
 			int d = incr2;
 			int x, y, xend, yend, yincr;
 			if (x1 > x2)
 			{
 				x = x2;   y = y2;   xend = x1;   yend = y1;
 			} else
 			{
 				x = x1;   y = y1;   xend = x2;   yend = y2;
 			}
 			if (yend < y) yincr = -1; else yincr = 1;
 			if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
 				layerBitMap[y][x>>3] |= (1 << (x&7));
 
 			// draw line that increments along X
 			while (x < xend)
 			{
 				x++;
 				if (d < 0) d += incr1; else
 				{
 					y += yincr;   d += incr2;
 				}
 				if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
 					layerBitMap[y][x>>3] |= (1 << (x&7));
 			}
 		} else
 		{
 			// initialize for lines that increment along Y
 			int incr1 = 2 * dx;
 			int incr2 = 2 * (dx - dy);
 			int d = incr2;
 			int x, y, xend, yend, xincr;
 			if (y1 > y2)
 			{
 				x = x2;   y = y2;   xend = x1;   yend = y1;
 			} else
 			{
 				x = x1;   y = y1;   xend = x2;   yend = y2;
 			}
 			if (xend < x) xincr = -1; else xincr = 1;
 			if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
 				layerBitMap[y][x>>3] |= (1 << (x&7));
 
 			// draw line that increments along X
 			while (y < yend)
 			{
 				y++;
 				if (d < 0) d += incr1; else
 				{
 					x += xincr;   d += incr2;
 				}
 				if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
 					layerBitMap[y][x>>3] |= (1 << (x&7));
 			}
 		}
 	}
 
 	private void drawPatLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, EGraphics desc, int pattern)
 	{
 		// get color and pattern information
 		int col = 0;
 		if (desc != null) col = desc.getColor().getRGB() & 0xFFFFFF;
 
 		// initialize counter for line style
 		int i = 0;
 
 		// initialize the Bresenham algorithm
 		int dx = Math.abs(x2-x1);
 		int dy = Math.abs(y2-y1);
 		if (dx > dy)
 		{
 			// initialize for lines that increment along X
 			int incr1 = 2 * dy;
 			int incr2 = 2 * (dy - dx);
 			int d = incr2;
 			int x, y, xend, yend, yincr;
 			if (x1 > x2)
 			{
 				x = x2;   y = y2;   xend = x1;   yend = y1;
 			} else
 			{
 				x = x1;   y = y1;   xend = x2;   yend = y2;
 			}
 			if (yend < y) yincr = -1; else yincr = 1;
 			if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
 				layerBitMap[y][x>>3] |= (1 << (x&7));
 
 			// draw line that increments along X
 			while (x < xend)
 			{
 				x++;
 				if (d < 0) d += incr1; else
 				{
 					y += yincr;   d += incr2;
 				}
 				if (i == 7) i = 0; else i++;
 				if ((pattern & (1 << i)) == 0) continue;
 				if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
 					layerBitMap[y][x>>3] |= (1 << (x&7));
 			}
 		} else
 		{
 			// initialize for lines that increment along Y
 			int incr1 = 2 * dx;
 			int incr2 = 2 * (dx - dy);
 			int d = incr2;
 			int x, y, xend, yend, xincr;
 			if (y1 > y2)
 			{
 				x = x2;   y = y2;   xend = x1;   yend = y1;
 			} else
 			{
 				x = x1;   y = y1;   xend = x2;   yend = y2;
 			}
 			if (xend < x) xincr = -1; else xincr = 1;
 			if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
 				layerBitMap[y][x>>3] |= (1 << (x&7));
 
 			// draw line that increments along X
 			while (y < yend)
 			{
 				y++;
 				if (d < 0) d += incr1; else
 				{
 					x += xincr;   d += incr2;
 				}
 				if (i == 7) i = 0; else i++;
 				if ((pattern & (1 << i)) == 0) continue;
 				if (layerBitMap == null) opaqueData[y * sz.width + x] = col; else
 					layerBitMap[y][x>>3] |= (1 << (x&7));
 			}
 		}
 	}
 
 	private void drawThickLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, EGraphics desc)
 	{
 		// get color and pattern information
 		int col = 0;
 		if (desc != null) col = desc.getColor().getRGB() & 0xFFFFFF;
 
 		// initialize the Bresenham algorithm
 		int dx = Math.abs(x2-x1);
 		int dy = Math.abs(y2-y1);
 		if (dx > dy)
 		{
 			// initialize for lines that increment along X
 			int incr1 = 2 * dy;
 			int incr2 = 2 * (dy - dx);
 			int d = incr2;
 			int x, y, xend, yend, yincr;
 			if (x1 > x2)
 			{
 				x = x2;   y = y2;   xend = x1;   yend = y1;
 			} else
 			{
 				x = x1;   y = y1;   xend = x2;   yend = y2;
 			}
 			if (yend < y) yincr = -1; else yincr = 1;
 			drawThickPoint(x, y, layerBitMap, col);
 
 			// draw line that increments along X
 			while (x < xend)
 			{
 				x++;
 				if (d < 0) d += incr1; else
 				{
 					y += yincr;
 					d += incr2;
 				}
 				drawThickPoint(x, y, layerBitMap, col);
 			}
 		} else
 		{
 			// initialize for lines that increment along Y
 			int incr1 = 2 * dx;
 			int incr2 = 2 * (dx - dy);
 			int d = incr2;
 			int x, y, xend, yend, xincr;
 			if (y1 > y2)
 			{
 				x = x2;   y = y2;   xend = x1;   yend = y1;
 			} else
 			{
 				x = x1;   y = y1;   xend = x2;   yend = y2;
 			}
 			if (xend < x) xincr = -1; else xincr = 1;
 			drawThickPoint(x, y, layerBitMap, col);
 
 			// draw line that increments along X
 			while (y < yend)
 			{
 				y++;
 				if (d < 0) d += incr1; else
 				{
 					x += xincr;
 					d += incr2;
 				}
 				drawThickPoint(x, y, layerBitMap, col);
 			}
 		}
 	}
 
 	// ************************************* POLYGON DRAWING *************************************
 
 	/**
 	 * Method to draw a polygon on the off-screen buffer.
 	 */
 	private void drawPolygon(Point [] points, byte [][] layerBitMap, EGraphics desc)
 	{
 		// get color and pattern information
 		int col = 0;
 		int [] pattern = null;
 		if (desc != null)
 		{
 			col = desc.getColor().getRGB() & 0xFFFFFF;
 			if (desc.isPatternedOnDisplay())
 				pattern = desc.getPattern();
 		}
 
 		// fill in internal structures
 		PolySeg edgelist = null;
 		PolySeg [] polySegs = new PolySeg[points.length];
 		for(int i=0; i<points.length; i++)
 		{
 			polySegs[i] = new PolySeg();
 			if (i == 0)
 			{
 				polySegs[i].fx = points[points.length-1].x;
 				polySegs[i].fy = points[points.length-1].y;
 			} else
 			{
 				polySegs[i].fx = points[i-1].x;
 				polySegs[i].fy = points[i-1].y;
 			}
 			polySegs[i].tx = points[i].x;   polySegs[i].ty = points[i].y;
 		}
 		for(int i=0; i<points.length; i++)
 		{
 			// draw the edge lines to make the polygon clean
 			if (pattern != null && desc.isOutlinedOnDisplay())
 				drawSolidLine(polySegs[i].fx, polySegs[i].fy, polySegs[i].tx, polySegs[i].ty, layerBitMap, desc);
 
 			// compute the direction of this edge
 			int j = polySegs[i].ty - polySegs[i].fy;
 			if (j > 0) polySegs[i].direction = 1; else
 				if (j < 0) polySegs[i].direction = -1; else
 					polySegs[i].direction = 0;
 
 			// compute the X increment of this edge
 			if (j == 0) polySegs[i].increment = 0; else
 			{
 				polySegs[i].increment = polySegs[i].tx - polySegs[i].fx;
 				if (polySegs[i].increment != 0) polySegs[i].increment =
 					(polySegs[i].increment * 65536 - j + 1) / j;
 			}
 			polySegs[i].tx <<= 16;   polySegs[i].fx <<= 16;
 
 			// make sure "from" is above "to"
 			if (polySegs[i].fy > polySegs[i].ty)
 			{
 				j = polySegs[i].tx;
 				polySegs[i].tx = polySegs[i].fx;
 				polySegs[i].fx = j;
 				j = polySegs[i].ty;
 				polySegs[i].ty = polySegs[i].fy;
 				polySegs[i].fy = j;
 			}
 
 			// insert this edge into the edgelist, sorted by ascending "fy"
 			if (edgelist == null)
 			{
 				edgelist = polySegs[i];
 				polySegs[i].nextedge = null;
 			} else
 			{
 				// insert by ascending "fy"
 				if (edgelist.fy > polySegs[i].fy)
 				{
 					polySegs[i].nextedge = edgelist;
 					edgelist = polySegs[i];
 				} else for(PolySeg a = edgelist; a != null; a = a.nextedge)
 				{
 					if (a.nextedge == null ||
 						a.nextedge.fy > polySegs[i].fy)
 					{
 						// insert after this
 						polySegs[i].nextedge = a.nextedge;
 						a.nextedge = polySegs[i];
 						break;
 					}
 				}
 			}
 		}
 
 		// scan polygon and render
 		int ycur = 0;
 		PolySeg active = null;
 		while (active != null || edgelist != null)
 		{
 			if (active == null)
 			{
 				active = edgelist;
 				active.nextactive = null;
 				edgelist = edgelist.nextedge;
 				ycur = active.fy;
 			}
 
 			// introduce edges from edge list into active list
 			while (edgelist != null && edgelist.fy <= ycur)
 			{
 				// insert "edgelist" into active list, sorted by "fx" coordinate
 				if (active.fx > edgelist.fx ||
 					(active.fx == edgelist.fx && active.increment > edgelist.increment))
 				{
 					edgelist.nextactive = active;
 					active = edgelist;
 					edgelist = edgelist.nextedge;
 				} else for(PolySeg a = active; a != null; a = a.nextactive)
 				{
 					if (a.nextactive == null ||
 						a.nextactive.fx > edgelist.fx ||
 							(a.nextactive.fx == edgelist.fx &&
 								a.nextactive.increment > edgelist.increment))
 					{
 						// insert after this
 						edgelist.nextactive = a.nextactive;
 						a.nextactive = edgelist;
 						edgelist = edgelist.nextedge;
 						break;
 					}
 				}
 			}
 
 			// generate regions to be filled in on current scan line
 			int wrap = 0;
 			PolySeg left = active;
 			for(PolySeg edge = active; edge != null; edge = edge.nextactive)
 			{
 				wrap = wrap + edge.direction;
 				if (wrap == 0)
 				{
 					int j = (left.fx + 32768) >> 16;
 					int k = (edge.fx + 32768) >> 16;
 
 					if (pattern != null)
 					{
 						int pat = pattern[ycur & 15];
 						if (pat != 0)
 						{
 							if (layerBitMap == null)
 							{
 								int baseIndex = ycur * sz.width;
 								for(int x=j; x<=k; x++)
 								{
 									if ((pat & (1 << (15-(x&15)))) != 0)
 										opaqueData[baseIndex + x] = col;
 								}
 							} else
 							{
 								byte [] row = layerBitMap[ycur];
 								for(int x=j; x<=k; x++)
 								{
 									if ((pat & (1 << (15-(x&15)))) != 0)
 										row[x>>3] |= (1 << (x&7));
 								}
 							}
 						}
 					} else
 					{
 						if (layerBitMap == null)
 						{
 							int baseIndex = ycur * sz.width;
 							for(int x=j; x<=k; x++)
 							{
 								opaqueData[baseIndex + x] = col;
 							}
 						} else
 						{
 							byte [] row = layerBitMap[ycur];
 							for(int x=j; x<=k; x++)
 							{
 								row[x>>3] |= (1 << (x&7));
 							}
 						}
 					}
 					left = edge.nextactive;
 				}
 			}
 			ycur++;
 
 			// update edges in active list
 			PolySeg lastedge = null;
 			for(PolySeg edge = active; edge != null; edge = edge.nextactive)
 			{
 				if (ycur >= edge.ty)
 				{
 					if (lastedge == null) active = edge.nextactive;
 						else lastedge.nextactive = edge.nextactive;
 				} else
 				{
 					edge.fx += edge.increment;
 					lastedge = edge;
 				}
 			}
 		}
 	}
 
 	// ************************************* TEXT DRAWING *************************************
 
 	/**
 	 * Method to draw a text on the off-screen buffer
 	 */
 	private void drawText(Rectangle rect, Poly.Type style, TextDescriptor descript, String s, byte [][] layerBitMap, EGraphics desc)
 	{
 		// quit if string is null
 		int len = s.length();
 		if (len == 0) return;
 
 		// get parameters
 		int col = 0;
 		if (desc != null) col = desc.getColor().getRGB() & 0xFFFFFF;
 
 		// get text description
 		int size = EditWindow.getDefaultFontSize();
 		int fontStyle = Font.PLAIN;
 		String fontName = User.getDefaultFont();
 		boolean italic = false;
 		boolean bold = false;
 		boolean underline = false;
 		int rotation = 0;
 		if (descript != null)
 		{
 			size = descript.getTrueSize(wnd);
 			if (size < MINIMUMTEXTSIZE) return;
 			italic = descript.isItalic();
 			bold = descript.isBold();
 			underline = descript.isUnderline();
 			int fontIndex = descript.getFace();
 			if (fontIndex != 0)
 			{
 				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontIndex);
 				if (af != null) fontName = af.getName();
 			}
 			rotation = descript.getRotation().getIndex();
 		}
 
 		// get box information for limiting text size
 		int boxedWidth = -1, boxedHeight = -1;
 		if (style == Poly.Type.TEXTBOX)
 		{
 			boxedWidth = (int)rect.getWidth();
 			boxedHeight = (int)rect.getHeight();
             // clip if not within bounds
             Rectangle2D dbBounds = wnd.screenToDatabase(rect);
             if (drawBounds != null && !drawBounds.intersects(dbBounds)) return;
 		}
 
         // create RenderInfo
         long startTime = System.currentTimeMillis();
         RenderTextInfo renderInfo = new RenderTextInfo();
         if (!renderInfo.buildInfo(s, fontName, size, italic, bold, underline, rect, style, rotation))
             return;
 
         // check if text is on-screen
         Rectangle2D dbBounds = wnd.screenToDatabase(renderInfo.bounds);
         if (drawBounds != null && !drawBounds.intersects(dbBounds))
             return;
 
 		// render the text
 		Raster ras = renderText(renderInfo);
         renderTextTime += (System.currentTimeMillis() - startTime);
         //System.out.println("Rendered text: "+s);
 		if (ras == null) return;
 		Point pt = getTextCorner(ras.getWidth(), ras.getHeight(), style, rect, rotation);
 		int atX = pt.x;
 		int atY = pt.y;
 		DataBufferByte dbb = (DataBufferByte)ras.getDataBuffer();
 		byte [] samples = dbb.getData();
 
 		int sx, ex;
 		int rasWidth = ras.getWidth();
 		int rasHeight = ras.getHeight();
 		switch (rotation)
 		{
 			case 0:			// no rotation
 				if (atX < 0) sx = -atX; else sx = 0;
 				if (atX+rasWidth >= sz.width) ex = sz.width-1 - atX; else
 					ex = rasWidth;
 				for(int y=0; y<rasHeight; y++)
 				{
 					int trueY = atY + y;
 					if (trueY < 0 || trueY >= sz.height) continue;
 
 					// setup pointers for filling this row
 					byte [] row = null;
 					int baseIndex = 0;
 					if (layerBitMap == null) baseIndex = trueY * sz.width; else
 						row = layerBitMap[trueY];
 					int samp = y * rasWidth + sx;
 					for(int x=sx; x<ex; x++)
 					{
 						int trueX = atX + x;
 						int alpha = samples[samp++] & 0xFF;
 						if (alpha == 0) continue;
 						if (layerBitMap == null)
 						{
 							// drawing opaque
 							int fullIndex = baseIndex + trueX;
 							int pixelValue = opaqueData[fullIndex];
 							int oldAlpha = (pixelValue >> 24) & 0xFF;
 							int color = col;
 							if (oldAlpha == 0)
 							{
 								// blend with opaque
 								if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
 							} else if (oldAlpha == 0xFF)
 							{
 								// blend with background
 								if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
 							}
 							opaqueData[fullIndex] = color;
 						} else
 						{
 							// draw in a transparent layer
 							if (alpha >= 128) row[trueX>>3] |= (1 << (trueX&7));
 						}
 					}
 				}
 				break;
 			case 1:			// 90 degrees counterclockwise
 				if (atX-rasHeight < 0) sx = rasHeight-atX; else sx = 0;
 				if (atX >= sz.height) ex = sz.height - atX; else
 					ex = rasHeight;
 				for(int y=0; y<rasWidth; y++)
 				{
 					int trueY = atY + y;
 					if (trueY < 0 || trueY >= sz.height) continue;
 
 					// setup pointers for filling this row
 					byte [] row = null;
 					int baseIndex = 0;
 					if (layerBitMap == null) baseIndex = trueY * sz.width; else
 						row = layerBitMap[trueY];
 					for(int x=sx; x<ex; x++)
 					{
 						int trueX = atX + x;
 						int alpha = samples[x * rasWidth + (rasWidth-y-1)] & 0xFF;
 						if (alpha == 0) continue;
 						if (layerBitMap == null)
 						{
 							// drawing opaque
 							int fullIndex = baseIndex + trueX;
 							int pixelValue = opaqueData[fullIndex];
 							int oldAlpha = (pixelValue >> 24) & 0xFF;
 							int color = col;
 							if (oldAlpha == 0)
 							{
 								// blend with opaque
 								if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
 							} else if (oldAlpha == 0xFF)
 							{
 								// blend with background
 								if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
 							}
 							opaqueData[fullIndex] = color;
 						} else
 						{
 							if (alpha >= 128) row[trueX>>3] |= (1 << (trueX&7));
 						}
 					}
 				}
 				break;
 			case 2:			// 180 degrees
 				atX -= rasWidth;
 				atY -= rasHeight;
 				if (atX < 0) sx = -atX; else sx = 0;
 				if (atX+rasWidth >= sz.width) ex = sz.width-1 - atX; else
 					ex = rasWidth;
 
 				for(int y=0; y<rasHeight; y++)
 				{
 					int trueY = atY + y;
 					if (trueY < 0 || trueY >= sz.height) continue;
 
 					// setup pointers for filling this row
 					byte [] row = null;
 					int baseIndex = 0;
 					if (layerBitMap == null) baseIndex = trueY * sz.width; else
 						row = layerBitMap[trueY];
 					for(int x=sx; x<ex; x++)
 					{
 						int trueX = atX + x;
 						int alpha = samples[(rasHeight-y-1) * rasWidth + (rasWidth-x-1)] & 0xFF;
 						if (alpha == 0) continue;
 						if (layerBitMap == null)
 						{
 							// drawing opaque
 							int fullIndex = baseIndex + trueX;
 							int pixelValue = opaqueData[fullIndex];
 							int oldAlpha = (pixelValue >> 24) & 0xFF;
 							int color = col;
 							if (oldAlpha == 0)
 							{
 								// blend with opaque
 								if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
 							} else if (oldAlpha == 0xFF)
 							{
 								// blend with background
 								if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
 							}
 							opaqueData[fullIndex] = color;
 						} else
 						{
 							if (alpha >= 128) row[trueX>>3] |= (1 << (trueX&7));
 						}
 					}
 				}
 				break;
 			case 3:			// 90 degrees clockwise
 				if (atX-rasHeight < 0) sx = rasHeight-atX; else sx = 0;
 				if (atX >= sz.height) ex = sz.height - atX; else
 					ex = rasHeight;
 				for(int y=0; y<rasWidth; y++)
 				{
 					int trueY = atY + y;
 					if (trueY < 0 || trueY >= sz.height) continue;
 
 					// setup pointers for filling this row
 					byte [] row = null;
 					int baseIndex = 0;
 					if (layerBitMap == null) baseIndex = trueY * sz.width; else
 						row = layerBitMap[trueY];
 					for(int x=sx; x<ex; x++)
 					{
 						int trueX = atX + x;
 						int alpha = samples[(rasHeight-x-1) * rasWidth + y] & 0xFF;
 						if (alpha == 0) continue;
 						if (layerBitMap == null)
 						{
 							// drawing opaque
 							int fullIndex = baseIndex + trueX;
 							int pixelValue = opaqueData[fullIndex];
 							int oldAlpha = (pixelValue >> 24) & 0xFF;
 							int color = col;
 							if (oldAlpha == 0)
 							{
 								// blend with opaque
 								if (alpha != 0xFF) color = alphaBlend(color, pixelValue, alpha);
 							} else if (oldAlpha == 0xFF)
 							{
 								// blend with background
 								if (alpha < 255) color = (color & 0xFFFFFF) | (alpha << 24);
 							}
 							opaqueData[fullIndex] = color;
 						} else
 						{
 							if (alpha >= 128) row[trueX>>3] |= (1 << (trueX&7));
 						}
 					}
 				}
 				break;
 		}
 	}
 
 	private int alphaBlend(int color, int backgroundColor, int alpha)
 	{
 		int red = (color >> 16) & 0xFF;
 		int green = (color >> 8) & 0xFF;
 		int blue = color & 0xFF;
 		int inverseAlpha = 254 - alpha;
 		int redBack = (backgroundColor >> 16) & 0xFF;
 		int greenBack = (backgroundColor >> 8) & 0xFF;
 		int blueBack = backgroundColor & 0xFF;
 		red = ((red * alpha) + (redBack * inverseAlpha)) / 255;
 		green = ((green * alpha) + (greenBack * inverseAlpha)) / 255;
 		blue = ((blue * alpha) + (blueBack * inverseAlpha)) / 255;
 		color = (red << 16) | (green << 8) + blue;
 		return color;
 	}
 
     private static class RenderTextInfo {
         private Font font;
         private GlyphVector gv;
         private LineMetrics lm;
         private Point2D anchorPoint;
         private Rectangle2D rasBounds;              // the raster bounds of the unrotated text, in pixels (screen units)
         private Rectangle2D bounds;                 // the real bounds of the rotated, anchored text (in screen units)
         private boolean underline;
 
         private boolean buildInfo(String msg, String fontName, int tSize, boolean italic, boolean bold, boolean underline,
                           Rectangle probableBoxedBounds, Poly.Type style, int rotation) {
             font = getFont(msg, fontName, tSize, italic, bold, underline);
             this.underline = underline;
 
             // convert the text to a GlyphVector
             FontRenderContext frc = new FontRenderContext(null, true, true);
             gv = font.createGlyphVector(frc, msg);
             lm = font.getLineMetrics(msg, frc);
 
             // figure bounding box of text
             //Rectangle rasRect = gv.getOutline(0, (float)(lm.getAscent()-lm.getLeading())).getBounds();
             Rectangle2D rasRect = gv.getLogicalBounds();
             int width = (int)rasRect.getWidth();
 
             int height = (int)(lm.getHeight()+0.5);
             if (width <= 0 || height <= 0) return false;
             int fontStyle = font.getStyle();
 
             int boxedWidth = (int)probableBoxedBounds.getWidth();
             int boxedHeight = (int)probableBoxedBounds.getHeight();
             // if text is to be "boxed", make sure it fits
             if (boxedWidth > 0 && boxedHeight > 0)
             {
                 if (width > boxedWidth || height > boxedHeight)
                 {
                     double scale = Math.min((double)boxedWidth / width, (double)boxedHeight / height);
                     font = new Font(fontName, fontStyle, (int)(tSize*scale));
                     if (font != null)
                     {
                         // convert the text to a GlyphVector
                         gv = font.createGlyphVector(frc, msg);
                         lm = font.getLineMetrics(msg, frc);
                         //rasRect = gv.getOutline(0, (float)(lm.getAscent()-lm.getLeading())).getBounds();
                         rasRect = gv.getLogicalBounds();
                         height = (int)(lm.getHeight()+0.5);
                         if (height <= 0) return false;
                         width = (int)rasRect.getWidth();
                     }
                 }
             }
             if (underline) height++;
             rasBounds = new Rectangle2D.Double(0, (float)lm.getAscent()-lm.getLeading(), width, height);
 
             anchorPoint = getTextCorner(width, height, style, probableBoxedBounds, rotation);
             if (rotation == 1 || rotation == 3) {
                 bounds = new Rectangle2D.Double(anchorPoint.getX(), anchorPoint.getY(), height, width);
             } else {
                 bounds = new Rectangle2D.Double(anchorPoint.getX(), anchorPoint.getY(), width, height);
             }
 
             return true;
         }
     }
 
 	/**
 	 * Method to return the coordinates of the lower-left corner of text in this window.
 	 * @param gv the GlyphVector describing the text.
 	 * @param style the anchor information for the text.
 	 * @param rect the bounds of the polygon containing the text.
 	 * @param rotation the rotation of the text (0=normal, 1=90 counterclockwise, 2=180, 3=90 clockwise).
 	 * @return the coordinates of the lower-left corner of the text.
 	 */
 	private static Point getTextCorner(int rasterWidth, int rasterHeight, Poly.Type style, Rectangle rect, int rotation)
 	{
 		// adjust to place text in the center
 		int textWidth = rasterWidth;
 		int textHeight = rasterHeight;
 		int offX = 0, offY = 0;
 		if (style == Poly.Type.TEXTCENT)
 		{
 			offX = -textWidth/2;
 			offY = -textHeight/2;
 		} else if (style == Poly.Type.TEXTTOP)
 		{
 			offX = -textWidth/2;
 		} else if (style == Poly.Type.TEXTBOT)
 		{
 			offX = -textWidth/2;
 			offY = -textHeight;
 		} else if (style == Poly.Type.TEXTLEFT)
 		{
 			offY = -textHeight/2;
 		} else if (style == Poly.Type.TEXTRIGHT)
 		{
 			offX = -textWidth;
 			offY = -textHeight/2;
 		} else if (style == Poly.Type.TEXTTOPLEFT)
 		{
 		} else if (style == Poly.Type.TEXTBOTLEFT)
 		{
 			offY = -textHeight;
 		} else if (style == Poly.Type.TEXTTOPRIGHT)
 		{
 			offX = -textWidth;
 		} else if (style == Poly.Type.TEXTBOTRIGHT)
 		{
 			offX = -textWidth;
 			offY = -textHeight;
 		} if (style == Poly.Type.TEXTBOX)
 		{
 //			if (textWidth > rect.getWidth())
 //			{
 //				// text too big for box: scale it down
 //				textScale *= rect.getWidth() / textWidth;
 //			}
 			offX = -textWidth/2;
 			offY = -textHeight/2;
 		}
 		if (rotation != 0)
 		{
 			int saveOffX = offX;
 			switch (rotation)
 			{
 				case 1:
 					offX = offY;
 					offY = saveOffX;
 					break;
 				case 2:
 					offX = -offX;
 					offY = -offY;
 					break;
 				case 3:
 					offX = offY;
 					offY = saveOffX;
 					break;
 			}
 		}
 		int cX = (int)rect.getCenterX() + offX;
 		int cY = (int)rect.getCenterY() + offY;
 		return new Point(cX, cY);
 	}
 
 	/**
 	 * Method to convert text to an array of pixels.
 	 * This is used for text rendering, as well as for creating "layout text" which is placed as geometry in the circuit.
 	 * @param msg the string of text to be converted.
 	 * @param font the name of the font to use.
 	 * @param tSize the size of the font to use.
 	 * @param italic true to make the text italic.
 	 * @param bold true to make the text bold.
 	 * @param underline true to underline the text.
 	 * @param boxedWidth the maximum width of the text (it is scaled down to fit).
 	 * @param boxedHeight the maximum height of the text (it is scaled down to fit).
 	 * @return a Raster with the text bits.
 	 */
 	public static Raster renderText(String msg, String font, int tSize, boolean italic, boolean bold, boolean underline, int boxedWidth, int boxedHeight)
 	{
         RenderTextInfo renderInfo = new RenderTextInfo();
         if (!renderInfo.buildInfo(msg, font, tSize, italic, bold, underline, new Rectangle(boxedWidth, boxedHeight), null, 0))
             return null;
         return renderText(renderInfo);
     }
 
     private static Raster renderText(RenderTextInfo renderInfo) {
 
         Font theFont = renderInfo.font;
         if (theFont == null) return null;
 
         int width = (int)renderInfo.rasBounds.getWidth();
         int height = (int)renderInfo.rasBounds.getHeight();
         GlyphVector gv = renderInfo.gv;
         LineMetrics lm = renderInfo.lm;
 
 		BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
 
 		// now render it
 		Graphics2D g2 = (Graphics2D)textImage.getGraphics();
 		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
 
 		g2.setColor(new Color(255,255,255));
 		g2.drawGlyphVector(gv, (float)-renderInfo.rasBounds.getX(), (float)(lm.getAscent()-lm.getLeading()));
 		if (renderInfo.underline)
 		{
 			g2.drawLine(0, height-1, width, height-1);
 		}
 
 		// return the bits
 		return textImage.getData();
 	}
 
     public static Font getFont(String msg, String font, int tSize, boolean italic, boolean bold, boolean underline) {
         // get the font
         int fontStyle = Font.PLAIN;
         if (italic) fontStyle |= Font.ITALIC;
         if (bold) fontStyle |= Font.BOLD;
         Font theFont = new Font(font, fontStyle, tSize);
         if (theFont == null)
         {
             System.out.println("Could not find font "+font+" to render text: "+msg);
             return null;
         }
         return theFont;
     }
 
 	// ************************************* CIRCLE DRAWING *************************************
 
 	/**
 	 * Method to draw a circle on the off-screen buffer
 	 */
 	private void drawCircle(Point center, Point edge, byte [][] layerBitMap, EGraphics desc)
 	{
 		// get parameters
 		int radius = (int)center.distance(edge);
 		int col = 0;
 		if (desc != null) col = desc.getColor().getRGB() & 0xFFFFFF;
 
 		// set redraw area
 		int left = center.x - radius;
 		int right = center.x + radius + 1;
 		int top = center.y - radius;
 		int bottom = center.y + radius + 1;
 
 		int x = 0;   int y = radius;
 		int d = 3 - 2 * radius;
 		if (left >= 0 && right < sz.width && top >= 0 && bottom < sz.height)
 		{
 			// no clip version is faster
 			while (x <= y)
 			{
 				if (layerBitMap == null)
 				{
 					int baseIndex = (center.y + y) * sz.width;
 					opaqueData[baseIndex + (center.x+x)] = col;
 					opaqueData[baseIndex + (center.x-x)] = col;
 
 					baseIndex = (center.y - y) * sz.width;
 					opaqueData[baseIndex + (center.x+x)] = col;
 					opaqueData[baseIndex + (center.x-x)] = col;
 
 					baseIndex = (center.y + x) * sz.width;
 					opaqueData[baseIndex + (center.x+y)] = col;
 					opaqueData[baseIndex + (center.x-y)] = col;
 
 					baseIndex = (center.y - x) * sz.width;
 					opaqueData[baseIndex + (center.x+y)] = col;
 					opaqueData[baseIndex + (center.x-y)] = col;
 				} else
 				{
 					byte [] row = layerBitMap[center.y + y];
 					row[(center.x+x)>>3] |= (1 << ((center.x+x)&7));
 					row[(center.x-x)>>3] |= (1 << ((center.x-x)&7));
 
 					row = layerBitMap[center.y - y];
 					row[(center.x+x)>>3] |= (1 << ((center.x+x)&7));
 					row[(center.x-x)>>3] |= (1 << ((center.x-x)&7));
 
 					row = layerBitMap[center.y + x];
 					row[(center.x+y)>>3] |= (1 << ((center.x+y)&7));
 					row[(center.x-y)>>3] |= (1 << ((center.x-y)&7));
 
 					row = layerBitMap[center.y - x];
 					row[(center.x+y)>>3] |= (1 << ((center.x+y)&7));
 					row[(center.x-y)>>3] |= (1 << ((center.x-y)&7));
 				}
 
 				if (d < 0) d += 4*x + 6; else
 				{
 					d += 4 * (x-y) + 10;
 					y--;
 				}
 				x++;
 			}
 		} else
 		{
 			// clip version
 			while (x <= y)
 			{
 				int thisy = center.y + y;
 				if (thisy >= 0 && thisy < sz.height)
 				{
 					int thisx = center.x + x;
 					if (thisx >= 0 && thisx < sz.width)
 						drawPoint(thisx, thisy, layerBitMap, col);
 					thisx = center.x - x;
 					if (thisx >= 0 && thisx < sz.width)
 						drawPoint(thisx, thisy, layerBitMap, col);
 				}
 
 				thisy = center.y - y;
 				if (thisy >= 0 && thisy < sz.height)
 				{
 					int thisx = center.x + x;
 					if (thisx >= 0 && thisx < sz.width)
 						drawPoint(thisx, thisy, layerBitMap, col);
 					thisx = center.x - x;
 					if (thisx >= 0 && thisx < sz.width)
 						drawPoint(thisx, thisy, layerBitMap, col);
 				}
 
 				thisy = center.y + x;
 				if (thisy >= 0 && thisy < sz.height)
 				{
 					int thisx = center.x + y;
 					if (thisx >= 0 && thisx < sz.width)
 						drawPoint(thisx, thisy, layerBitMap, col);
 					thisx = center.x - y;
 					if (thisx >= 0 && thisx < sz.width)
 						drawPoint(thisx, thisy, layerBitMap, col);
 				}
 
 				thisy = center.y - x;
 				if (thisy >= 0 && thisy < sz.height)
 				{
 					int thisx = center.x + y;
 					if (thisx >= 0 && thisx < sz.width)
 						drawPoint(thisx, thisy, layerBitMap, col);
 					thisx = center.x - y;
 					if (thisx >= 0 && thisx < sz.width)
 						drawPoint(thisx, thisy, layerBitMap, col);
 				}
 
 				if (d < 0) d += 4*x + 6; else
 				{
 					d += 4 * (x-y) + 10;
 					y--;
 				}
 				x++;
 			}
 		}
 	}
 
 	/**
 	 * Method to draw a thick circle on the off-screen buffer
 	 */
 	private void drawThickCircle(Point center, Point edge, byte [][] layerBitMap, EGraphics desc)
 	{
 		// get parameters
 		int radius = (int)center.distance(edge);
 		int col = 0;
 		if (desc != null) col = desc.getColor().getRGB() & 0xFFFFFF;
 
 		int x = 0;   int y = radius;
 		int d = 3 - 2 * radius;
 		while (x <= y)
 		{
 			int thisy = center.y + y;
 			if (thisy >= 0 && thisy < sz.height)
 			{
 				int thisx = center.x + x;
 				if (thisx >= 0 && thisx < sz.width)
 					drawThickPoint(thisx, thisy, layerBitMap, col);
 				thisx = center.x - x;
 				if (thisx >= 0 && thisx < sz.width)
 					drawThickPoint(thisx, thisy, layerBitMap, col);
 			}
 
 			thisy = center.y - y;
 			if (thisy >= 0 && thisy < sz.height)
 			{
 				int thisx = center.x + x;
 				if (thisx >= 0 && thisx < sz.width)
 					drawThickPoint(thisx, thisy, layerBitMap, col);
 				thisx = center.x - x;
 				if (thisx >= 0 && thisx < sz.width)
 					drawThickPoint(thisx, thisy, layerBitMap, col);
 			}
 
 			thisy = center.y + x;
 			if (thisy >= 0 && thisy < sz.height)
 			{
 				int thisx = center.x + y;
 				if (thisx >= 0 && thisx < sz.width)
 					drawThickPoint(thisx, thisy, layerBitMap, col);
 				thisx = center.x - y;
 				if (thisx >= 0 && thisx < sz.width)
 					drawThickPoint(thisx, thisy, layerBitMap, col);
 			}
 
 			thisy = center.y - x;
 			if (thisy >= 0 && thisy < sz.height)
 			{
 				int thisx = center.x + y;
 				if (thisx >= 0 && thisx < sz.width)
 					drawThickPoint(thisx, thisy, layerBitMap, col);
 				thisx = center.x - y;
 				if (thisx >= 0 && thisx < sz.width)
 					drawThickPoint(thisx, thisy, layerBitMap, col);
 			}
 
 			if (d < 0) d += 4*x + 6; else
 			{
 				d += 4 * (x-y) + 10;
 				y--;
 			}
 			x++;
 		}
 	}
 
 	// ************************************* DISC DRAWING *************************************
 
 	/**
 	 * Method to draw a scan line of the filled-in circle of radius "radius"
 	 */
 	private void drawDiscRow(int thisy, int startx, int endx, byte [][] layerBitMap, int col, int [] pattern)
 	{
 		if (thisy < 0 || thisy >= sz.height) return;
 		if (startx < 0) startx = 0;
 		if (endx >= sz.width) endx = sz.width - 1;
 		if (pattern != null)
 		{
 			int pat = pattern[thisy & 15];
 			if (pat != 0)
 			{
 				if (layerBitMap == null)
 				{
 					int baseIndex = thisy * sz.width;
 					for(int x=startx; x<=endx; x++)
 					{
 						if ((pat & (1 << (15-(x&15)))) != 0)
 							opaqueData[baseIndex + x] = col;
 					}
 				} else
 				{
 					byte [] row = layerBitMap[thisy];
 					for(int x=startx; x<=endx; x++)
 					{
 						if ((pat & (1 << (15-(x&15)))) != 0)
 							row[x>>3] |= (1 << (x&7));
 					}
 				}
 			}
 		} else
 		{
 			if (layerBitMap == null)
 			{
 				int baseIndex = thisy * sz.width;
 				for(int x=startx; x<=endx; x++)
 				{
 					opaqueData[baseIndex + x] = col;
 				}
 			} else
 			{
 				byte [] row = layerBitMap[thisy];
 				for(int x=startx; x<=endx; x++)
 				{
 					row[x>>3] |= (1 << (x&7));
 				}
 			}
 		}
 	}
 
 	/**
 	 * Method to draw a filled-in circle of radius "radius" on the off-screen buffer
 	 */
 	private void drawDisc(Point center, Point edge, byte [][] layerBitMap, EGraphics desc)
 	{
 		// get parameters
 		int radius = (int)center.distance(edge);
 		int col = 0;
 		int [] pattern = null;
 		if (desc != null)
 		{
 			col = desc.getColor().getRGB() & 0xFFFFFF;
 			if (desc.isPatternedOnDisplay())
 			{
 				pattern = desc.getPattern();
 				if (desc.isOutlinedOnDisplay())
 				{
 					drawCircle(center, edge, layerBitMap, desc);				
 				}
 			}
 		}
 
 		// set redraw area
 		int left = center.x - radius;
 		int right = center.x + radius + 1;
 		int top = center.y - radius;
 		int bottom = center.y + radius + 1;
 
 		if (radius == 1)
 		{
 			// just fill the area for discs this small
 			if (left < 0) left = 0;
 			if (right >= sz.width) right = sz.width - 1;
 			for(int y=top; y<bottom; y++)
 			{
 				if (y < 0 || y >= sz.height) continue;
 				for(int x=left; x<right; x++)
 					drawPoint(x, y, layerBitMap, col);
 			}
 			return;
 		}
 
 		int x = 0;   int y = radius;
 		int d = 3 - 2 * radius;
 		while (x <= y)
 		{
 			drawDiscRow(center.y+y, center.x-x, center.x+x, layerBitMap, col, pattern);
 			drawDiscRow(center.y-y, center.x-x, center.x+x, layerBitMap, col, pattern);
 			drawDiscRow(center.y+x, center.x-y, center.x+y, layerBitMap, col, pattern);
 			drawDiscRow(center.y-x, center.x-y, center.x+y, layerBitMap, col, pattern);
 
 			if (d < 0) d += 4*x + 6; else
 			{
 				d += 4 * (x-y) + 10;
 				y--;
 			}
 			x++;
 		}
 	}
 
 	// ************************************* ARC DRAWING *************************************
 
 	private boolean [] arcOctTable = new boolean[9];
 	private Point      arcCenter;
 	private int        arcRadius;
 	private int        arcCol;
 	private byte [][]  arcLayerBitMap;
 	private boolean    arcThick;
 
 	private int arcFindOctant(int x, int y)
 	{
 		if (x > 0)
 		{
 			if (y >= 0)
 			{
 				if (y >= x) return 7;
 				return 8;
 			}
 			if (x >= -y) return 1;
 			return 2;
 		}
 		if (y > 0)
 		{
 			if (y > -x) return 6;
 			return 5;
 		}
 		if (y > x) return 4;
 		return 3;
 	}
 
 	private Point arcXformOctant(int x, int y, int oct)
 	{
 		switch (oct)
 		{
 			case 1 : return new Point(-y,  x);
 			case 2 : return new Point( x, -y);
 			case 3 : return new Point(-x, -y);
 			case 4 : return new Point(-y, -x);
 			case 5 : return new Point( y, -x);
 			case 6 : return new Point(-x,  y);
 			case 7 : return new Point( x,  y);
 			case 8 : return new Point( y,  x);
 		}
 		return null;
 	}
 
 	private void arcDoPixel(int x, int y)
 	{
 		if (x < 0 || x >= sz.width || y < 0 || y >= sz.height) return;
 		if (arcThick)
 		{
 			drawThickPoint(x, y, arcLayerBitMap, arcCol);
 		} else
 		{
 			drawPoint(x, y, arcLayerBitMap, arcCol);
 		}
 	}
 
 	private void arcOutXform(int x, int y)
 	{
 		if (arcOctTable[1]) arcDoPixel( y + arcCenter.x, -x + arcCenter.y);
 		if (arcOctTable[2]) arcDoPixel( x + arcCenter.x, -y + arcCenter.y);
 		if (arcOctTable[3]) arcDoPixel(-x + arcCenter.x, -y + arcCenter.y);
 		if (arcOctTable[4]) arcDoPixel(-y + arcCenter.x, -x + arcCenter.y);
 		if (arcOctTable[5]) arcDoPixel(-y + arcCenter.x,  x + arcCenter.y);
 		if (arcOctTable[6]) arcDoPixel(-x + arcCenter.x,  y + arcCenter.y);
 		if (arcOctTable[7]) arcDoPixel( x + arcCenter.x,  y + arcCenter.y);
 		if (arcOctTable[8]) arcDoPixel( y + arcCenter.x,  x + arcCenter.y);
 	}
 
 	private void arcBresCW(Point pt, Point pt1)
 	{
 		int d = 3 - 2 * pt.y + 4 * pt.x;
 		while (pt.x < pt1.x && pt.y > pt1.y)
 		{
 			arcOutXform(pt.x, pt.y);
 			if (d < 0) d += 4 * pt.x + 6; else
 			{
 				d += 4 * (pt.x-pt.y) + 10;
 				pt.y--;
 			}
 			pt.x++;
 		}
 
 		// get to the end
 		for ( ; pt.x < pt1.x; pt.x++) arcOutXform(pt.x, pt.y);
 		for ( ; pt.y > pt1.y; pt.y--) arcOutXform(pt.x, pt.y);
 		arcOutXform(pt1.x, pt1.y);
 	}
 
 	private void arcBresMidCW(Point pt)
 	{
 		int d = 3 - 2 * pt.y + 4 * pt.x;
 		while (pt.x < pt.y)
 		{
 			arcOutXform(pt.x, pt.y);
 			if (d < 0) d += 4 * pt.x + 6; else
 			{
 				d += 4 * (pt.x-pt.y) + 10;
 				pt.y--;
 			}
 			pt.x++;
 	   }
 	   if (pt.x == pt.y) arcOutXform(pt.x, pt.y);
 	}
 
 	private void arcBresMidCCW(Point pt)
 	{
 		int d = 3 + 2 * pt.y - 4 * pt.x;
 		while (pt.x > 0)
 		{
 			arcOutXform(pt.x, pt.y);
 			if (d > 0) d += 6-4 * pt.x; else
 			{
 				d += 4 * (pt.y-pt.x) + 10;
 				pt.y++;
 			}
 			pt.x--;
 	   }
 	   arcOutXform(0, arcRadius);
 	}
 
 	private void arcBresCCW(Point pt, Point pt1)
 	{
 		int d = 3 + 2 * pt.y + 4 * pt.x;
 		while(pt.x > pt1.x && pt.y < pt1.y)
 		{
 			// not always correct
 			arcOutXform(pt.x, pt.y);
 			if (d > 0) d += 6 - 4 * pt.x; else
 			{
 				d += 4 * (pt.y-pt.x) + 10;
 				pt.y++;
 			}
 			pt.x--;
 		}
 
 		// get to the end
 		for ( ; pt.x > pt1.x; pt.x--) arcOutXform(pt.x, pt.y);
 		for ( ; pt.y < pt1.y; pt.y++) arcOutXform(pt.x, pt.y);
 		arcOutXform(pt1.x, pt1.y);
 	}
 
 	/**
 	 * draws an arc centered at (centerx, centery), clockwise,
 	 * passing by (x1,y1) and (x2,y2)
 	 */
 	private void drawCircleArc(Point center, Point p1, Point p2, boolean thick, byte [][] layerBitMap, EGraphics desc)
 	{
 		// ignore tiny arcs
 		if (p1.x == p2.x && p1.y == p2.y) return;
 
 		// get parameters
 		arcLayerBitMap = layerBitMap;
 		arcCol = 0;
 		if (desc != null) arcCol = desc.getColor().getRGB() & 0xFFFFFF;
 
 		arcCenter = center;
 		int pa_x = p2.x - arcCenter.x;
 		int pa_y = p2.y - arcCenter.y;
 		int pb_x = p1.x - arcCenter.x;
 		int pb_y = p1.y - arcCenter.y;
 		arcRadius = (int)arcCenter.distance(p2);
 		int alternate = (int)arcCenter.distance(p1);
 		int start_oct = arcFindOctant(pa_x, pa_y);
 		int end_oct   = arcFindOctant(pb_x, pb_y);
 		arcThick = thick;
 
 		// move the point
 		if (arcRadius != alternate)
 		{
 			int diff = arcRadius-alternate;
 			switch (end_oct)
 			{
 				case 6:
 				case 7: /*  y >  x */ pb_y += diff;  break;
 				case 8: /*  x >  y */
 				case 1: /*  x > -y */ pb_x += diff;  break;
 				case 2: /* -y >  x */
 				case 3: /* -y > -x */ pb_y -= diff;  break;
 				case 4: /* -y < -x */
 				case 5: /*  y < -x */ pb_x -= diff;  break;
 			}
 		}
 
 		for(int i=1; i<9; i++) arcOctTable[i] = false;
 
 		if (start_oct == end_oct)
 		{
 			arcOctTable[start_oct] = true;
 			Point pa = arcXformOctant(pa_x, pa_y, start_oct);
 			Point pb = arcXformOctant(pb_x, pb_y, start_oct);
 
 			if ((start_oct&1) != 0) arcBresCW(pa, pb);
 			else                    arcBresCCW(pa, pb);
 			arcOctTable[start_oct] = false;
 		} else
 		{
 			arcOctTable[start_oct] = true;
 			Point pt = arcXformOctant(pa_x, pa_y, start_oct);
 			if ((start_oct&1) != 0) arcBresMidCW(pt);
 			else			    	arcBresMidCCW(pt);
 			arcOctTable[start_oct] = false;
 
 			arcOctTable[end_oct] = true;
 			pt = arcXformOctant(pb_x, pb_y, end_oct);
 			if ((end_oct&1) != 0) arcBresMidCCW(pt);
 			else			      arcBresMidCW(pt);
 			arcOctTable[end_oct] = false;
 
 			if (MODP(start_oct+1) != end_oct)
 			{
 				if (MODP(start_oct+1) == MODM(end_oct-1))
 				{
 					arcOctTable[MODP(start_oct+1)] = true;
 				} else
 				{
 					for(int i = MODP(start_oct+1); i != end_oct; i = MODP(i+1))
 						arcOctTable[i] = true;
 				}
 				arcBresMidCW(new Point(0, arcRadius));
 			}
 		}
 	}
 
 	private int MODM(int x) { return (x<1) ? x+8 : x; }
 	private int MODP(int x) { return (x>8) ? x-8 : x; }
 
 	// ************************************* RENDERING SUPPORT *************************************
 
 	private void drawPoint(int x, int y, byte [][] layerBitMap, int col)
 	{
 		if (layerBitMap == null)
 		{
 			opaqueData[y * sz.width + x] = col;
 		} else
 		{
 			layerBitMap[y][x>>3] |= (1 << (x&7));
 		}
 	}
 
 	private void drawThickPoint(int x, int y, byte [][] layerBitMap, int col)
 	{
 		if (layerBitMap == null)
 		{
 			opaqueData[y * sz.width + x] = col;
 			if (x > 0)
 				opaqueData[y * sz.width + (x-1)] = col;
 			if (x < sz.width-1)
 				opaqueData[y * sz.width + (x+1)] = col;
 			if (y > 0)
 				opaqueData[(y-1) * sz.width + (x+1)] = col;
 			if (y < sz.height-1)
 				opaqueData[(y+1) * sz.width + (x+1)] = col;
 		} else
 		{
 			layerBitMap[y][x>>3] |= (1 << (x&7));
 			if (x > 0)
 				layerBitMap[y][(x-1)>>3] |= (1 << (x&7));
 			if (x < sz.width-1)
 				layerBitMap[y][(x+1)>>3] |= (1 << (x&7));
 			if (y > 0)
 				layerBitMap[y-1][x>>3] |= (1 << (x&7));
 			if (y < sz.height-1)
 				layerBitMap[y+1][x>>3] |= (1 << (x&7));
 		}
 	}
 
 	// ************************************* CLIPPING *************************************
 
 	// clipping directions
 	private static final int LEFT   = 1;
 	private static final int RIGHT  = 2;
 	private static final int BOTTOM = 4;
 	private static final int TOP    = 8;
 
 	/**
 	 * Method to clip a line from (fx,fy) to (tx,ty) in the rectangle lx <= X <= hx and ly <= Y <= hy.
 	 * Returns true if the line is not visible.
 	 */
 	private boolean clipLine(Point from, Point to, int lx, int hx, int ly, int hy)
 	{
 		for(;;)
 		{
 			// compute code bits for "from" point
 			int fc = 0;
 			if (from.x < lx) fc |= LEFT; else
 				if (from.x > hx) fc |= RIGHT;
 			if (from.y < ly) fc |= BOTTOM; else
 				if (from.y > hy) fc |= TOP;
 
 			// compute code bits for "to" point
 			int tc = 0;
 			if (to.x < lx) tc |= LEFT; else
 				if (to.x > hx) tc |= RIGHT;
 			if (to.y < ly) tc |= BOTTOM; else
 				if (to.y > hy) tc |= TOP;
 
 			// look for trivial acceptance or rejection
 			if (fc == 0 && tc == 0) return false;
 			if (fc == tc || (fc & tc) != 0) return true;
 
 			// make sure the "from" side needs clipping
 			if (fc == 0)
 			{
 				int t = from.x;   from.x = to.x;   to.x = t;
 				t = from.y;   from.y = to.y;   to.y = t;
 				t = fc;       fc = tc;         tc = t;
 			}
 
 			if ((fc&LEFT) != 0)
 			{
 				if (to.x == from.x) return true;
 				int t = (to.y - from.y) * (lx - from.x) / (to.x - from.x);
 				from.y += t;
 				from.x = lx;
 			}
 			if ((fc&RIGHT) != 0)
 			{
 				if (to.x == from.x) return true;
 				int t = (to.y - from.y) * (hx - from.x) / (to.x - from.x);
 				from.y += t;
 				from.x = hx;
 			}
 			if ((fc&BOTTOM) != 0)
 			{
 				if (to.y == from.y) return true;
 				int t = (to.x - from.x) * (ly - from.y) / (to.y - from.y);
 				from.x += t;
 				from.y = ly;
 			}
 			if ((fc&TOP) != 0)
 			{
 				if (to.y == from.y) return true;
 				int t = (to.x - from.x) * (hy - from.y) / (to.y - from.y);
 				from.x += t;
 				from.y = hy;
 			}
 		}
 	}
 
 	private Point [] clipPoly(Point [] points, int lx, int hx, int ly, int hy)
 	{
 		// see if any points are outside
 		int count = points.length;
 		int pre = 0;
 		for(int i=0; i<count; i++)
 		{
 			if (points[i].x < lx) pre |= LEFT; else
 				if (points[i].x > hx) pre |= RIGHT;
 			if (points[i].y < ly) pre |= BOTTOM; else
 				if (points[i].y > hy) pre |= TOP;
 		}
 		if (pre == 0) return points;
 
 		// get polygon
 		Point [] in = new Point[count*2];
 		for(int i=0; i<count*2; i++)
 		{
 			in[i] = new Point();
 			if (i < count) in[i].setLocation(points[i]);
 		}
 		Point [] out = new Point[count*2];
 		for(int i=0; i<count*2; i++)
 			out[i] = new Point();
 
 		// clip on all four sides
 		Point [] a = in;
 		Point [] b = out;
 
 		if ((pre & LEFT) != 0)
 		{
 			count = clipEdge(a, count, b, LEFT, lx);
 			Point [] swap = a;   a = b;   b = swap;
 		}
 		if ((pre & RIGHT) != 0)
 		{
 			count = clipEdge(a, count, b, RIGHT, hx);
 			Point [] swap = a;   a = b;   b = swap;
 		}
 		if ((pre & TOP) != 0)
 		{
 			count = clipEdge(a, count, b, TOP, hy);
 			Point [] swap = a;   a = b;   b = swap;
 		}
 		if ((pre & BOTTOM) != 0)
 		{
 			count = clipEdge(a, count, b, BOTTOM, ly);
 			Point [] swap = a;   a = b;   b = swap;
 		}
 
 		// remove redundant points from polygon
 		pre = 0;
 		for(int i=0; i<count; i++)
 		{
 			if (i > 0 && a[i-1].x == a[i].x && a[i-1].y == a[i].y) continue;
 			b[pre].x = a[i].x;   b[pre].y = a[i].y;
 			pre++;
 		}
 
 		// closed polygon: remove redundancy on wrap-around
 		while (pre != 0 && b[0].x == b[pre-1].x && b[0].y == b[pre-1].y) pre--;
 		count = pre;
 
 		// copy the polygon back if it in the wrong place
 		Point [] retArr = new Point[count];
 		for(int i=0; i<count; i++)
 			retArr[i] = b[i];
 		return retArr;
 	}
 
 	/**
 	 * Method to clip polygon "in" against line "edge" (1:left, 2:right,
 	 * 4:bottom, 8:top) and place clipped result in "out".
 	 */
 	private int clipEdge(Point [] in, int inCount, Point [] out, int edge, int value)
 	{
 		// look at all the lines
 		Point first = new Point();
 		Point second = new Point();
 		int firstx = 0, firsty = 0;
 		int outcount = 0;
 		for(int i=0; i<inCount; i++)
 		{
 			int pre = i - 1;
 			if (i == 0) pre = inCount-1;
 			first.setLocation(in[pre]);
 			second.setLocation(in[i]);
 			if (clipSegment(first, second, edge, value)) continue;
 			int x1 = first.x;     int y1 = first.y;
 			int x2 = second.x;    int y2 = second.y;
 			if (outcount != 0)
 			{
 				if (x1 != out[outcount-1].x || y1 != out[outcount-1].y)
 				{
 					out[outcount].x = x1;  out[outcount++].y = y1;
 				}
 			} else { firstx = x1;  firsty = y1; }
 			out[outcount].x = x2;  out[outcount++].y = y2;
 		}
 		if (outcount != 0 && (out[outcount-1].x != firstx || out[outcount-1].y != firsty))
 		{
 			out[outcount].x = firstx;   out[outcount++].y = firsty;
 		}
 		return outcount;
 	}
 
 	/**
 	 * Method to do clipping on the vector from (x1,y1) to (x2,y2).
 	 * If the vector is completely invisible, true is returned.
 	 */
 	private boolean clipSegment(Point p1, Point p2, int codebit, int value)
 	{
 		int x1 = p1.x;   int y1 = p1.y;
 		int x2 = p2.x;   int y2 = p2.y;
 
 		int c1 = 0, c2 = 0;
 		if (codebit == LEFT)
 		{
 			if (x1 < value) c1 = codebit;
 			if (x2 < value) c2 = codebit;
 		} else if (codebit == BOTTOM)
 		{
 			if (y1 < value) c1 = codebit;
 			if (y2 < value) c2 = codebit;
 		} else if (codebit == RIGHT)
 		{
 			if (x1 > value) c1 = codebit;
 			if (x2 > value) c2 = codebit;
 		} else if (codebit == TOP)
 		{
 			if (y1 > value) c1 = codebit;
 			if (y2 > value) c2 = codebit;
 		}
 
 		if (c1 == c2) return c1 != 0;
 		boolean flip = false;
 		if (c1 == 0)
 		{
 			int t = x1;   x1 = x2;   x2 = t;
 			t = y1;   y1 = y2;   y2 = t;
 			flip = true;
 		}
 		if (codebit == LEFT || codebit == RIGHT)
 		{
 			int t = (y2-y1) * (value-x1) / (x2-x1);
 			y1 += t;
 			x1 = value;
 		} else if (codebit == BOTTOM || codebit == TOP)
 		{
 			int t = (x2-x1) * (value-y1) / (y2-y1);
 			x1 += t;
 			y1 = value;
 		}
 		if (flip)
 		{
 			p1.x = x2;   p1.y = y2;
 			p2.x = x1;   p2.y = y1;
 		} else
 		{
 			p1.x = x1;   p1.y = y1;
 			p2.x = x2;   p2.y = y2;
 		}
 		return false;
 	}
 
 }
