 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: Utils.java
  *
  * Copyright (c) 2010 Sun Microsystems and Static Free Software
  *
  * Electric(tm) is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 3 of the License, or
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
 package com.sun.electric.tool.erc.wellcheck;
 
 import java.awt.geom.Point2D;
 import java.awt.geom.Rectangle2D;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Random;
 import java.util.Set;
 import java.util.TreeSet;
 
 import com.sun.electric.database.geometry.DBMath;
 import com.sun.electric.database.geometry.GenMath.MutableBoolean;
 import com.sun.electric.database.topology.RTNode;
 import com.sun.electric.technology.PrimitiveNode;
 import com.sun.electric.tool.erc.ERCWellCheck2.WellBound;
 import com.sun.electric.tool.erc.ERCWellCheck2.WellBoundRecord;
 import com.sun.electric.tool.erc.wellcheck.WellCon.WellConComparator;
 
 /**
  * @author fschmidt
  * 
  */
 public class Utils {
 
 	public enum WorkDistributionStrategy {
 		cluster, random, bucket;
 	}
 
 	public static final boolean GATHERSTATISTICS = false;
 	public static final boolean INCREMENTALGROWTH = false;
 	public static final WorkDistributionStrategy WORKDISTRIBUTION = WorkDistributionStrategy.bucket;
 	public static List<WellBoundRecord> wellBoundSearchOrder;
 	public static int numObjSearches;
 
 	/**
 	 * Method to tell whether this function describes an element which can be
 	 * used as substrate tap (ERC)
 	 * 
 	 * @return
 	 */
 	public static boolean canBeSubstrateTap(PrimitiveNode.Function fun) {
 		return fun == PrimitiveNode.Function.SUBSTRATE || fun == PrimitiveNode.Function.RESPWELL;
 	}
 
 	/**
 	 * Method to tell whether this function describes an element which can be
 	 * used as well tap (ERC)
 	 * 
 	 * @return
 	 */
 	public static boolean canBeWellTap(PrimitiveNode.Function fun) {
 		return fun == PrimitiveNode.Function.WELL || fun == PrimitiveNode.Function.RESNWELL;
 	}
 
 	public static void spreadWellSeed(double cX, double cY, NetValues wellNum, RTNode rtree, int threadIndex) {
 		RTNode allFound = null;
 		Point2D ctr = new Point2D.Double(cX, cY);
 		Rectangle2D searchArea = new Rectangle2D.Double(cX, cY, 0, 0);
 		MutableBoolean keepSearching = new MutableBoolean(true);
 		Rectangle2D[] sides = new Rectangle2D[4];
 		for (int i = 0; i < 4; i++)
 			sides[i] = new Rectangle2D.Double(0, 0, 0, 0);
 		int numSides = 1;
 		sides[0].setRect(searchArea);
 		while (keepSearching.booleanValue()) {
 			if (INCREMENTALGROWTH) {
 				// grow the search area incrementally
 				double lX = sides[0].getMinX(), hX = sides[0].getMaxX();
 				double lY = sides[0].getMinY(), hY = sides[0].getMaxY();
 				for (int i = 1; i < numSides; i++) {
 					lX = Math.min(lX, sides[i].getMinX());
 					hX = Math.max(hX, sides[i].getMinX());
 					lY = Math.min(lY, sides[i].getMinY());
 					hY = Math.max(hY, sides[i].getMinY());
 				}
 				double newLX = lX, newHX = hX;
 				double newLY = lY, newHY = hY;
 				boolean anySearchesGood = false;
 				for (int i = 0; i < numSides; i++) {
 					allFound = searchInArea(sides[i], wellNum, rtree, allFound, ctr, keepSearching,
 							threadIndex);
 					if (keepSearching.booleanValue())
 						anySearchesGood = true;
 					newLX = Math.min(newLX, sides[i].getMinX());
 					newHX = Math.max(newHX, sides[i].getMinX());
 					newLY = Math.min(newLY, sides[i].getMinY());
 					newHY = Math.max(newHY, sides[i].getMinY());
 				}
 				keepSearching.setValue(anySearchesGood);
 
 				// compute new bounds
 				numSides = 0;
 				if (newLX < lX)
 					sides[numSides++].setRect(newLX, newLY, lX - newLX, newHY - newLY);
 				if (newHX > hX)
 					sides[numSides++].setRect(hX, newLY, newHX - hX, newHY - newLY);
 				if (newLY < lY)
 					sides[numSides++].setRect(newLX, newLY, newHX - newLX, lY - newLY);
 				if (newHY > hY)
 					sides[numSides++].setRect(newLX, hY, newHX - newLX, newHY - hY);
 			} else {
 				// just keep growing the search area
 				allFound = searchInArea(searchArea, wellNum, rtree, allFound, ctr, keepSearching, threadIndex);
 			}
 		}
 	}
 
 	public static RTNode searchInArea(Rectangle2D searchArea, NetValues wellNum, RTNode rtree,
 			RTNode allFound, Point2D ctr, MutableBoolean keepSearching, int threadIndex) {
 		keepSearching.setValue(false);
 		for (RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext();) {
 			WellBound wb = (WellBound) sea.next();
 			if (GATHERSTATISTICS)
 				numObjSearches++;
 			// ignore if this object is already properly connected
 			if (wb.getNetID() != null && wb.getNetID().getIndex() == wellNum.getIndex())
 				continue;
 
 			// see if this object actually touches something in the set
 			if (allFound == null) {
 				// start from center of contact
 				if (!wb.getBounds().contains(ctr))
 					continue;
 			} else {
 				boolean touches = false;
 				for (RTNode.Search subSea = new RTNode.Search(wb.getBounds(), allFound, true); subSea
 						.hasNext();) {
 					WellBound subWB = (WellBound) subSea.next();
 					if (DBMath.rectsIntersect(subWB.getBounds(), wb.getBounds())) {
 						touches = true;
 						break;
 					}
 				}
 				if (!touches)
 					continue;
 			}
 
 			// this touches what is gathered so far: add to it
 			synchronized (wb) {
 				if (wb.getNetID() != null)
 					wellNum.merge(wb.getNetID());
 				else
 					wb.setNetID(wellNum);
 			}
 			if (GATHERSTATISTICS)
 				wellBoundSearchOrder.add(new WellBoundRecord(wb, threadIndex));
 			// expand the next search area by this added bound
 			Rectangle2D.union(searchArea, wb.getBounds(), searchArea);
 			if (allFound == null)
 				allFound = RTNode.makeTopLevel();
 			allFound = RTNode.linkGeom(null, allFound, wb);
 			keepSearching.setValue(true);
 		}
 		return allFound;
 	}
 
 	public static WellCon[] getStarters(int numberOfThreads, List<WellCon> wellCons) {
 		if (numberOfThreads > wellCons.size()) {
 			throw new IndexOutOfBoundsException();
 		}
 		Random rand = new Random(System.currentTimeMillis());
 		WellCon[] startValues = new WellCon[numberOfThreads];
 		Set<Integer> usedStarters = new HashSet<Integer>();
 		usedStarters.add(-1);
 		for (int i = 0; i < numberOfThreads; i++) {
 			int startValue = -1;
 			while (usedStarters.contains(startValue)) {
 				startValue = rand.nextInt(wellCons.size());
 			}
 			usedStarters.add(startValue);
 			startValues[i] = wellCons.get(startValue);
 		}
 		return startValues;
 	}
 
 	public static Set<WellCon> sortWellConList(WellCon base, List<WellCon> pool) {
		Set<WellCon> result = new TreeSet<WellCon>(new WellConComparator(base));
 
 		for (WellCon con : pool) {
 			result.add(con);
 		}
 
 		return result;
 	}
 
 	private static Map<List<?>, Integer> safeRemoveIdx = new HashMap<List<?>, Integer>();
 
 	public static <T> T safeListRemove(List<T> list, int index) {
 		synchronized (list) {
 			synchronized (safeRemoveIdx) {
 				Integer idx = safeRemoveIdx.get(list);
 				idx = idx != null ? idx : 0;
 				safeRemoveIdx.put(list, idx + 1);
 
 				return list.remove(index - idx);
 			}
 		}
 	}
 
 }
