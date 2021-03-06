 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: Highlight.java
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
 package com.sun.electric.tool.user;
 
 import com.sun.electric.database.geometry.Geometric;
 import com.sun.electric.database.geometry.Poly;
 import com.sun.electric.database.geometry.DBMath;
 import com.sun.electric.database.hierarchy.Cell;
 import com.sun.electric.database.hierarchy.Export;
 import com.sun.electric.database.hierarchy.Nodable;
 import com.sun.electric.database.network.JNetwork;
 import com.sun.electric.database.network.Netlist;
 import com.sun.electric.database.prototype.NodeProto;
 import com.sun.electric.database.prototype.ArcProto;
 import com.sun.electric.database.prototype.PortProto;
 import com.sun.electric.database.text.Name;
 import com.sun.electric.database.topology.NodeInst;
 import com.sun.electric.database.topology.ArcInst;
 import com.sun.electric.database.topology.PortInst;
 import com.sun.electric.database.topology.Connection;
 import com.sun.electric.database.variable.Variable;
 import com.sun.electric.database.variable.FlagSet;
 import com.sun.electric.database.variable.ElectricObject;
 import com.sun.electric.technology.Technology;
 import com.sun.electric.technology.PrimitiveNode;
 import com.sun.electric.technology.SizeOffset;
 import com.sun.electric.technology.Layer;
 import com.sun.electric.technology.technologies.Artwork;
 import com.sun.electric.technology.technologies.Generic;
 import com.sun.electric.tool.user.ui.WaveformWindow;
 import com.sun.electric.tool.user.ui.EditWindow;
 import com.sun.electric.tool.user.ui.WindowFrame;
 import com.sun.electric.tool.user.ui.WindowContent;
 
 import java.awt.*;
 import java.awt.font.GlyphVector;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.Point2D;
 import java.awt.geom.Rectangle2D;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Set;
 import java.util.HashSet;
 import java.util.ArrayList;
 
 /*
  * Class for highlighting of objects on the display.
  * <P>
  * These are the types of highlighting that can occur:
  * <UL>
  * <LI>EOBJ: an ElectricObject is selected (NodeInst, ArcInst, or PortInst).
  *   <UL>
  *   <LI>Fills in "eobj" and the parent "cell".
  *   <LI>If selecting a NodeInst, may fill-in "point" if an outline node is being edited.
  *   </UL>
  * <LI>TEXT: text is selected.
  *   <UL>
  *   <LI>Fills in "eobj" and the parent "cell".
  *   <LI>If "var" is valid, this is a variable on a NodeInst, ArcInst, Export, PortInst, or Cell.
  *   <LI>If "var" is null and "name" is valid, it is the name of a NodeInst or ArcInst.
  *   <LI>If "var" and "name" are null and "eobj" is an Export, it is that Export.
  *   <LI>If "var" and "name" are null, this is a Cell instance name.
  *   </UL>
  * <LI>BBOX: a rectangular area is selected.  Fills in "bounds" and the parent "cell".
  * <LI>LINE: a line is selected.  Fills in "pt1", "pt2" and the parent "cell".
  * <LI>MESSAGE: a random piece of text is displayed (not from the database.  Fills in "pt1", "msg" and the parent "cell".
  * </UL>
  */
 public class Highlight
 {
 	/**
 	 * Type is a typesafe enum class that describes the nature of the highlight.
 	 */
 	public static class Type
 	{
 		private final String name;
 		private int order;
 		private static int ordering = 1;
 
 		private Type(String name) { this.name = name;   this.order = ordering++; }
 
 		/**
 		 * Returns an ordering of this Type.
 		 * The ordering is used in the multi-object Get Info dialog.
 		 * @return an ordering of this Type.
 		 */
 		public int getOrder() { return order; }
 
 		/**
 		 * Returns a printable version of this Type.
 		 * @return a printable version of this Type.
 		 */
 		public String toString() { return name; }
 
 		/** Describes a highlighted ElectricObject. */			public static final Type EOBJ = new Type("electricObject");
 		/** Describes highlighted text. */						public static final Type TEXT = new Type("text");
 		/** Describes a highlighted area. */					public static final Type BBOX = new Type("area");
 		/** Describes a highlighted line. */					public static final Type LINE = new Type("line");
 		/** Describes a thick highlighted line. */				public static final Type THICKLINE = new Type("thick line");
 		/** Describes a non-database text. */					public static final Type MESSAGE = new Type("message");
 	}
 
 	/** The type of the highlighting. */						private Type type;
 	/** The highlighted object. */								private ElectricObject eobj;
 	/** The Cell containing the selection. */					private Cell cell;
 	/** The highlighted outline point (only for NodeInst). */	private int point;
 	/** The highlighted variable. */							private Variable var;
 	/** The highlighted Name. */								private Name name;
 	/** The highlighted area. */								private Rectangle2D bounds;
 	/** The highlighted line. */								private Point2D pt1, pt2;
 	/** The center point about which thick lines revolve. */	private Point2D center;
 	/** The highlighted message. */								private String msg;
 
 	/** Screen offset for display of highlighting. */			private static int highOffX, highOffY;
 	/** the highlighted objects. */								private static List highlightList = new ArrayList();
 	/** the stack of highlights. */								private static List highlightStack = new ArrayList();
     /** last list of highlighted objects */                     private static List lastHighlightsList = new ArrayList();
 
     /** List of HighlightListeners */                           private static List highlightListeners = new ArrayList();
 
 	private static final int EXACTSELECTDISTANCE = 5;
 	private static final int CROSSSIZE = 3;
 
 	private Highlight(Type type)
 	{
 		this.type = type;
 		this.eobj = null;
 		this.cell = null;
 		this.point = -1;
 		this.var = null;
 		this.name = null;
 		this.bounds = null;
 		this.pt1 = null;
 		this.pt2 = null;
 		this.msg = null;
 	}
 
 	/**
 	 * Method to clear the list of highlighted objects.
 	 */
 	public static synchronized void clear()
 	{
 		highlightList.clear();
 		highOffX = highOffY = 0;
 	}
 
 	/**
 	 * Method to indicate that changes to highlighting are finished.
 	 * Call this after any change to highlighting.
 	 */
 	public static synchronized void finished()
 	{
         // only do something if highlights changed
         boolean changed = false;
         if (highlightList.size() != lastHighlightsList.size()) {
             changed = true;
         } else {
             // check actual list
             for (int i=0; i<highlightList.size(); i++) {
                 if (highlightList.get(i) != lastHighlightsList.get(i)) {
                     changed = true;
                     break;
                 }
             }
         }
         if (!changed) return;
 
         // set lastHighlightList to current list
         lastHighlightsList.clear();
         lastHighlightsList.add(highlightList);
 
 		// see if arcs of a single type were selected
 		boolean mixedArc = false;
 		ArcProto foundArcProto = null;
 		for(Iterator it = getHighlights(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 			if (h.getType() == Type.EOBJ)
 			{
 				ElectricObject eobj = h.getElectricObject();
 				if (eobj instanceof ArcInst)
 				{
 					ArcProto ap = ((ArcInst)eobj).getProto();
 					if (foundArcProto == null)
 					{
 						foundArcProto = ap;
 					} else
 					{
 						if (foundArcProto != ap) mixedArc = true;
 					}
 				}
 			}
 		}
 		if (foundArcProto != null && !mixedArc) User.tool.setCurrentArcProto(foundArcProto);
 
         // notify all listeners that highlights have changed (changes committed).
         fireHighlightChanged();
 	}
 
     /** Add a Highlight listener */
     public static synchronized void addHighlightListener(HighlightListener l) {
         highlightListeners.add(l);
     }
 
     /** Remove a Highlight listener */
     public static synchronized void removeHighlightListener(HighlightListener l) {
         highlightListeners.remove(l);
     }
 
     /** Notify listeners that highlights have changed */
     private static synchronized void fireHighlightChanged() {
         for (Iterator it = highlightListeners.iterator(); it.hasNext(); ) {
             HighlightListener l = (HighlightListener)it.next();
             l.highlightChanged();
         }
     }
 	/**
 	 * Method to add an ElectricObject to the list of highlighted objects.
 	 * @param eobj the ElectricObject to add to the list of highlighted objects.
 	 * @param cell the Cell in which the ElectricObject resides.
 	 * @return the newly created Highlight object.
 	 */
 	public static synchronized Highlight addElectricObject(ElectricObject eobj, Cell cell)
 	{
 		Highlight h = new Highlight(Type.EOBJ);
 		h.eobj = eobj;
 		h.cell = cell;
 
 		highlightList.add(h);
 		return h;
 	}
 
 	/**
 	 * Method to push the current highlight list onto a stack.
 	 */
 	public static synchronized void pushHighlight()
 	{
 		// make a copy of the highlighted list
 		List pushable = new ArrayList();
 		for(Iterator it = highlightList.iterator(); it.hasNext(); )
 			pushable.add(it.next());
 		highlightStack.add(pushable);
 	}
 
 	/**
 	 * Method to pop the current highlight list from the stack.
 	 */
 	public static synchronized void popHighlight()
 	{
 		int stackSize = highlightStack.size();
 		if (stackSize <= 0)
 		{
 			System.out.println("There is no highlighting saved on the highlight stack");
 			return;
 		}
 
 		// get the stacked highlight
 		List popable = (List)highlightStack.get(stackSize-1);
 		highlightStack.remove(stackSize-1);
 
 		// validate each highlight as it is added
 		clear();
 		for(Iterator it = popable.iterator(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 			if (h.type == Type.EOBJ)
 			{
 				if (h.cell.objInCell(h.eobj))
 				{
 					Highlight newH = addElectricObject(h.eobj, h.cell);
 					newH.setPoint(h.point);
 				}
 			} else if (h.type == Type.TEXT)
 			{
 				if (h.cell.objInCell(h.eobj))
 				{
 					Highlight newH = Highlight.addText(h.eobj, h.cell, h.var, h.name);
 				}
 			} else if (h.type == Type.BBOX)
 			{
 				Highlight newH = addArea(h.bounds, h.cell);
 			} else if (h.type == Type.LINE)
 			{
 				Highlight newH = addLine(h.pt1, h.pt2, h.cell);
 			} else if (h.type == Type.THICKLINE)
 			{
 				Highlight newH = addThickLine(h.pt1, h.pt2, h.center, h.cell);
 			} else if (h.type == Type.MESSAGE)
 			{
 				Highlight newH = addMessage(h.cell, h.msg, h.center);
 			}
 		}
 		finished();
 	}
 	/**
 	 * Method to add a text selection to the list of highlighted objects.
 	 * @param cell the Cell in which this area resides.
 	 * @param var the Variable associated with the text (text is then a visual of that variable).
 	 * @param name the Name associated with the text (for the name of Nodes and Arcs).
 	 * @return the newly created Highlight object.
 	 */
 	public static synchronized Highlight addText(ElectricObject eobj, Cell cell, Variable var, Name name)
 	{
 		Highlight h = new Highlight(Type.TEXT);
 		h.eobj = eobj;
 		h.cell = cell;
 		h.var = var;
 		h.name = name;
 
 		highlightList.add(h);
 		return h;
 	}
 
 	/**
 	 * Method to add a message display to the list of highlighted objects.
 	 * @param cell the Cell in which this area resides.
 	 * @param message the String to display.
 	 * @param loc the location of the string (in database units).
 	 * @return the newly created Highlight object.
 	 */
 	public static synchronized Highlight addMessage(Cell cell, String message, Point2D loc)
 	{
 		Highlight h = new Highlight(Type.MESSAGE);
 		h.msg = message;
 		h.cell = cell;
 		h.pt1 = loc;
 
 		highlightList.add(h);
 		return h;
 	}
 
 	/**
 	 * Method to add an area to the list of highlighted objects.
 	 * @param area the Rectangular area to add to the list of highlighted objects.
 	 * @param cell the Cell in which this area resides.
 	 * @return the newly created Highlight object.
 	 */
 	public static synchronized Highlight addArea(Rectangle2D area, Cell cell)
 	{
 		Highlight h = new Highlight(Type.BBOX);
 		h.bounds = new Rectangle2D.Double();
 		h.bounds.setRect(area);
 		h.cell = cell;
 
 		highlightList.add(h);
 		return h;
 	}
 
 	/**
 	 * Method to add a line to the list of highlighted objects.
 	 * @param start the start point of the line to add to the list of highlighted objects.
 	 * @param end the end point of the line to add to the list of highlighted objects.
 	 * @param cell the Cell in which this line resides.
 	 * @return the newly created Highlight object.
 	 */
 	public static synchronized Highlight addLine(Point2D start, Point2D end, Cell cell)
 	{
 		Highlight h = new Highlight(Type.LINE);
 		h.pt1 = new Point2D.Double(start.getX(), start.getY());
 		h.pt2 = new Point2D.Double(end.getX(), end.getY());
 		h.cell = cell;
 
 		highlightList.add(h);
 		return h;
 	}
 
 	/**
 	 * Method to add a line to the list of highlighted objects.
 	 * @param start the start point of the line to add to the list of highlighted objects.
 	 * @param end the end point of the line to add to the list of highlighted objects.
 	 * @param cell the Cell in which this line resides.
 	 * @return the newly created Highlight object.
 	 */
 	public static synchronized Highlight addThickLine(Point2D start, Point2D end, Point2D center, Cell cell)
 	{
 		Highlight h = new Highlight(Type.THICKLINE);
 		h.pt1 = new Point2D.Double(start.getX(), start.getY());
 		h.pt2 = new Point2D.Double(end.getX(), end.getY());
 		h.center = new Point2D.Double(center.getX(), center.getY());
 		h.cell = cell;
 
 		highlightList.add(h);
 		return h;
 	}
 
 	/**
 	 * Method to add a network to the list of highlighted objects.
 	 * Many arcs may be highlighted as a result.
 	 * @param net the network to highlight.
 	 * @param cell the Cell in which this line resides.
 	 */
 	public static void addNetwork(JNetwork net, Cell cell)
 	{
 		Netlist netlist = cell.getUserNetlist();
 
 		// show all arcs on the network
 		for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
 		{
 			ArcInst ai = (ArcInst)aIt.next();
 			int width = netlist.getBusWidth(ai);
 			for(int i=0; i<width; i++)
 			{
 				JNetwork oNet = netlist.getNetwork(ai, i);
 				if (oNet == net)
 				{
 					Highlight.addElectricObject(ai, cell);
 					break;
 				}
 			}
 		}
 
 		// show all exports on the network
 		for(Iterator pIt = cell.getPorts(); pIt.hasNext(); )
 		{
 			Export pp = (Export)pIt.next();
 			int width = netlist.getBusWidth(pp);
 			for(int i=0; i<width; i++)
 			{
 				JNetwork oNet = netlist.getNetwork(pp, i);
 				if (oNet == net)
 				{
 					Highlight.addText(pp, cell, null, null);
 					break;
 				}
 			}
 		}
 	}
 
     /**
      * Removes a Highlight object from the current set of highlights.
      * @param h the Highlight to remove
      */
     public static synchronized void remove(Highlight h) {
         highlightList.remove(h);
     }
 
 	/**
 	 * Method to return the type of this Highlight (EOBJ, TEXT, BBOX, LINE, or MESSAGE).
 	 * @return the type of this Highlight.
 	 */
 	public Type getType() { return type; }
 
 	/**
 	 * Method to return the ElectricObject associated with this Highlight object.
 	 * @return the ElectricObject associated with this Highlight object.
 	 */
 	public ElectricObject getElectricObject() { return eobj; }
 
 	/**
 	 * Method to set the ElectricObject associated with this Highlight object.
 	 * @param eobj the ElectricObject associated with this Highlight object.
 	 */
 	private void setElectricObject(ElectricObject eobj) { this.eobj = eobj; }
 
 	/**
 	 * Method to return the Cell associated with this Highlight object.
 	 * @return the Cell associated with this Highlight object.
 	 */
 	public Cell getCell() { return cell; }
 
 	/**
 	 * Method to set the Cell associated with this Highlight object.
 	 * @param cell the Cell associated with this Highlight object.
 	 */
 	private void setCell(Cell cell) { this.cell = cell; }
 
 	/**
 	 * Method to return the outline point associated with this Highlight object.
 	 * @return the outline point associated with this Highlight object.
 	 */
 	public int getPoint() { return point; }
 
 	/**
 	 * Method to set an outline point to be displayed with this Highlight.
 	 * @param point the outline point to show with this Highlight (must be a NodeInst highlight).
 	 */
 	public void setPoint(int point) { this.point = point; }
 
 	/**
 	 * Method to return the bounds associated with this Highlight object.
 	 * Bounds are used for area definitions and also for text.
 	 * @return the bounds associated with this Highlight object.
 	 */
 	public Rectangle2D getBounds() { return bounds; }
 
 	/**
 	 * Method to return the Name associated with this Highlight object.
 	 * @return the Name associated with this Highlight object.
 	 */
 	public Name getName() { return name; }
 
 	/**
 	 * Method to set the Name associated with this Highlight object.
 	 * @param name the Name associated with this Highlight object.
 	 */
 	private void setName(Name name) { this.name = name; }
 
 	/**
 	 * Method to return the Variable associated with this Highlight object.
 	 * @return the Variable associated with this Highlight object.
 	 */
 	public Variable getVar() { return var; }
 
 	/**
 	 * Method to set the Variable associated with this Highlight object.
 	 * @param var the Variable associated with this Highlight object.
 	 */
 	public void setVar(Variable var) { this.var = var; }
 
 	/**
 	 * Method to return the "from point" associated with this Highlight object.
 	 * This only applies to Highlights of type LINE.
 	 * @return the from point associated with this Highlight object.
 	 */
 	public Point2D getFromPoint() { return pt1; }
 
 	/**
 	 * Method to return the "to point" associated with this Highlight object.
 	 * This only applies to Highlights of type LINE.
 	 * @return the to point associated with this Highlight object.
 	 */
 	public Point2D getToPoint() { return pt2; }
 
 	/**
 	 * Method to return the number of highlighted objects.
 	 * @return the number of highlighted objects.
 	 */
 	public static synchronized int getNumHighlights() { return highlightList.size(); }
 
 	/**
 	 * Method to return an Iterator over the highlighted objects.
 	 * @return an Iterator over the highlighted objects.
 	 */
 	public static synchronized Iterator getHighlights() {
         ArrayList highlightsCopy = new ArrayList(highlightList);
         return highlightsCopy.iterator();
     }
 
 	/**
 	 * Method to load a list of Highlights into the highlighting.
 	 * @param newHighlights a List of Highlight objects.
 	 */
 	public static synchronized void setHighlightList(List newHighlights)
 	{
 		for(Iterator it = newHighlights.iterator(); it.hasNext(); )
 		{
 			highlightList.add(it.next());
 		}
 	}
 
 	/**
 	 * Method to return a List of all highlighted ElectricObjects.
 	 * @param wantNodes true if NodeInsts should be included in the list.
 	 * @param wantArcs true if ArcInsts should be included in the list.
 	 * @return a list with the highlighted ElectricObjects.
 	 */
 	public static List getHighlighted(boolean wantNodes, boolean wantArcs)
 	{
 		// now place the objects in the list
 		List highlightedGeoms = new ArrayList();
 		for(Iterator it = getHighlights(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 
 			if (h.getType() == Type.EOBJ)
 			{
 				ElectricObject eobj = h.getElectricObject();
 				if (!wantNodes)
 				{
 					if (eobj instanceof NodeInst || eobj instanceof PortInst) continue;
 				}
 				if (!wantArcs && eobj instanceof ArcInst) continue;
 				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
 
 				if (highlightedGeoms.contains(eobj)) continue;
 				highlightedGeoms.add(eobj);
 			}
 			if (h.getType() == Type.BBOX)
 			{
 				List inArea = findAllInArea(h.getCell(), false, false, false, false, false, false, h.getBounds(), null);
 				for(Iterator ait = inArea.iterator(); ait.hasNext(); )
 				{
 					Highlight ah = (Highlight)ait.next();
 					if (ah.getType() != Type.EOBJ) continue;
 					ElectricObject eobj = ah.getElectricObject();
 					if (!wantNodes)
 					{
 						if (eobj instanceof NodeInst || eobj instanceof PortInst) continue;
 					}
 					if (!wantArcs && eobj instanceof ArcInst) continue;
 					if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
 					highlightedGeoms.add(eobj);
 				}
 			}
 			if (h.getType() == Type.TEXT)
 			{
 				if (h.nodeMovesWithText())
 				{
 					ElectricObject eobj = h.getElectricObject();
 					if (eobj instanceof Export) eobj = ((Export)eobj).getOriginalPort().getNodeInst();
 					highlightedGeoms.add(eobj);
 				}
 			}
 		}
 		return highlightedGeoms;
 	}
 
 	/**
 	 * Method to return a set of the currently selected networks.
 	 * @return a set of the currently selected networks.
 	 * If there are no selected networks, the list is empty.
 	 */
 	public static synchronized Set getHighlightedNetworks()
 	{
 		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
 		if (wf.getContent() instanceof WaveformWindow)
 		{
 			WaveformWindow ww = (WaveformWindow)wf.getContent();
 			return ww.getHighlightedNetworks();
 		}
 		Set nets = new HashSet();
 		Cell cell = WindowFrame.getCurrentCell();
 		if (cell != null)
 		{
 			Netlist netlist = cell.getUserNetlist();
 			for(Iterator it = highlightList.iterator(); it.hasNext(); )
 			{
 				Highlight h = (Highlight)it.next();
 				if (h.type == Type.EOBJ)
 				{
 					ElectricObject eObj = h.getElectricObject();
 					if (eObj instanceof PortInst)
 					{
 						PortInst pi = (PortInst)eObj;
 						JNetwork net = netlist.getNetwork(pi);
 						if (net != null) nets.add(net); else
 						{
 							// if port is isolated, grab all nets
 							if (pi.getPortProto().isIsolated())
 							{
 								for(Iterator aIt = pi.getNodeInst().getConnections(); aIt.hasNext(); )
 								{
 									Connection con = (Connection)aIt.next();
 									ArcInst ai = con.getArc();
 									net = netlist.getNetwork(ai, 0);
 									if (net != null) nets.add(net);
 								}
 							}
 						}
 					} else if (eObj instanceof NodeInst)
 					{
 						NodeInst ni = (NodeInst)eObj;
						PortInst pi = ni.getOnlyPortInst();
						if (pi != null)
						{
							JNetwork net = netlist.getNetwork(pi);
							if (net != null) nets.add(net);
						}
 					} else if (eObj instanceof ArcInst)
 					{
 						ArcInst ai = (ArcInst)eObj;
 						int width = netlist.getBusWidth(ai);
 						for(int i=0; i<width; i++)
 						{
 							JNetwork net = netlist.getNetwork((ArcInst)eObj, i);
 							if (net != null) nets.add(net);
 						}
 					}
 				} else if (h.type == Type.TEXT)
 				{
 					if (h.getVar() == null && h.getName() == null &&
 						h.getElectricObject() instanceof Export)
 					{
 						Export pp = (Export)h.getElectricObject();
 						int width = netlist.getBusWidth(pp);
 						for(int i=0; i<width; i++)
 						{
 							JNetwork net = netlist.getNetwork(pp, i);
 							if (net != null) nets.add(net);
 						}
 					}
 				}
 			}
 		}		
 		return nets;
 	}
 
 	/**
 	 * Method to tell whether this Highlight is text that stays with its node.
 	 * The two possibilities are (1) text on invisible pins
 	 * (2) export names, when the option to move exports with their labels is requested.
 	 * @return true if this Highlight is text that should move with its node.
 	 */
 	public boolean nodeMovesWithText()
 	{
 		if (type != Type.TEXT) return false;
 		if (var != null)
 		{
 			/* moving variable text */
 			if (!(eobj instanceof NodeInst)) return false;
 			NodeInst ni = (NodeInst)eobj;
 			if (ni.isInvisiblePinWithText()) return true;
 		} else
 		{
 			/* moving export text */
 			if (!(eobj instanceof Export)) return false;
 			Export pp = (Export)eobj;
 			if (pp.getOriginalPort().getNodeInst().getProto() == Generic.tech.invisiblePinNode) return true;
 			if (User.isMoveNodeWithExport()) return true;
 		}
 		return false;
 	}
 
 	/**
 	 * Method to return a List of all highlighted text.
 	 * @param unique true to request that the text objects be unique,
 	 * and not attached to another object that is highlighted.
 	 * For example, if a node and an export on that node are selected,
 	 * the export text will not be included if "unique" is true.
 	 * @return a list with the Highlight objects that point to text.
 	 */
 	public static synchronized List getHighlightedText(boolean unique)
 	{
 		// now place the objects in the list
 		List highlightedText = new ArrayList();
 		for(Iterator it = getHighlights(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 
 			if (h.getType() == Type.TEXT)
 			{
 				if (highlightedText.contains(h)) continue;
 
 				// if this text is on a selected object, don't include the text
 				if (unique)
 				{
 					ElectricObject eobj = h.getElectricObject();
 					ElectricObject onObj = null;
 					if (h.getVar() != null)
 					{
 						if (eobj instanceof Export)
 						{
 							onObj = ((Export)eobj).getOriginalPort().getNodeInst();
 						} else if (eobj instanceof PortInst)
 						{
 							onObj = ((PortInst)eobj).getNodeInst();
 						} else if (eobj instanceof Geometric)
 						{
 							onObj = eobj;
 						}
 					} else
 					{
 						if (h.getName() != null)
 						{
 							if (eobj instanceof Geometric) onObj = eobj;
 						} else
 						{
 							if (eobj instanceof Export)
 							{
 								onObj = ((Export)eobj).getOriginalPort().getNodeInst();
 							} else
 							{
 								if (eobj instanceof NodeInst) onObj = eobj;
 							}
 						}
 					}
 	
 					// now see if the object is in the list
 					if (eobj != null)
 					{
 						boolean found = false;
 						for(Iterator fIt = getHighlights(); fIt.hasNext(); )
 						{
 							Highlight oH = (Highlight)fIt.next();
 							if (oH.getType() != Type.EOBJ) continue;
 							ElectricObject fobj = oH.getElectricObject();
 							if (fobj instanceof PortInst) fobj = ((PortInst)fobj).getNodeInst();
 							if (fobj == onObj) { found = true;   break; }
 						}
 						if (found) continue;
 					}
 				}
 
 				// add this text
 				highlightedText.add(h);
 			}
 		}
 		return highlightedText;
 	}
 
 	/**
 	 * Method to return the bounds of the highlighted objects.
 	 * @param wnd the window in which to get bounds.
 	 * @return the bounds of the highlighted objects (null if nothing is highlighted).
 	 */
 	public static synchronized Rectangle2D getHighlightedArea(EditWindow wnd)
 	{
 		// initially no area
 		Rectangle2D bounds = null;
 
 		// look at all highlighted objects
 		for(Iterator it = getHighlights(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 
 			// find the bounds of this highlight
 			Rectangle2D highBounds = null;
 			if (h.getType() == Type.EOBJ)
 			{
 				ElectricObject eobj = h.getElectricObject();
 				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
 				if (eobj instanceof Geometric)
 				{
 					Geometric geom = (Geometric)eobj;
 					highBounds = geom.getBounds();
 				}
 			} else if (h.getType() == Type.TEXT)
 			{
 				if (wnd != null)
 				{
 					Poly poly = h.getElectricObject().computeTextPoly(wnd, h.getVar(), h.getName());
 					if (poly != null) highBounds = poly.getBounds2D();
 				}
 			} else if (h.getType() == Type.BBOX)
 			{
 				highBounds = h.getBounds();
 			} else if (h.getType() == Type.LINE || h.getType() == Type.THICKLINE)
 			{
 				double cX = (h.pt1.getX() + h.pt2.getX()) / 2;
 				double cY = (h.pt1.getY() + h.pt2.getY()) / 2;
 				double sX = Math.abs(h.pt1.getX() - h.pt2.getX());
 				double sY = Math.abs(h.pt1.getY() - h.pt2.getY());
 				highBounds = new Rectangle2D.Double(cX, cY, sX, sY);
 			} else if (h.getType() == Type.MESSAGE)
 			{
 				highBounds = new Rectangle2D.Double(h.pt1.getX(), h.pt1.getY(), 0, 0);
 			}
 
 			// combine this highlight's bounds with the overall one
 			if (highBounds != null)
 			{
 				if (bounds == null)
 				{
 					bounds = new Rectangle2D.Double();
 					bounds.setRect(highBounds);
 				} else
 				{
 					Rectangle2D.union(bounds, highBounds, bounds);
 				}
 			}
 		}
 
 		// return the overall bounds
 		return bounds;
 	}
 
 	/**
 	 * Method to return the only highlighted object.
 	 * If there is not one highlighted object, an error is issued.
 	 * @return the highlighted object (null if error).
 	 */
 	public static synchronized Highlight getOneHighlight()
 	{
 		if (getNumHighlights() == 0)
 		{
 			System.out.println("Must select an object first");
 			return null;
 		}
 		if (getNumHighlights() > 1)
 		{
 			System.out.println("Must select only one object");
 			return null;
 		}
 		Highlight h = (Highlight)getHighlights().next();
 		return h;
 	}
 
 	/**
 	 * Method to return the only highlighted object.
 	 * If there is not one highlighted object, an error is issued.
 	 * @return the highlighted object (null if error).
 	 */
 	public static synchronized ElectricObject getOneElectricObject(Class type)
 	{
 		Highlight high = getOneHighlight();
 		if (high == null) return null;
 		if (high.getType() != Highlight.Type.EOBJ)
 		{
             System.out.println("Must first select an object");
             return null;
         }
         ElectricObject eobj = high.getElectricObject();
 		if (type == NodeInst.class)
 		{
 			if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
 		}
 		if (type != eobj.getClass())
 		{
 			
             System.out.println("Wrong type of object is selected");
             System.out.println(" (Wanted "+type.toString()+" but got "+eobj.getClass().toString()+")");
             return null;
 		}
 		return eobj;
 	}
 
 	/**
 	 * Method to set a screen offset for the display of highlighting.
 	 * @param offX the X offset (in pixels) of the highlighting.
 	 * @param offY the Y offset (in pixels) of the highlighting.
 	 */
 	public static synchronized void setHighlightOffset(int offX, int offY)
 	{
 		highOffX = offX;
 		highOffY = offY;
 	}
 
     /**
      * Method to return the screen offset for the display of highlighting
      * @return a Point2D containing the x and y offset.
      */
     public static synchronized Point2D getHighlightOffset()
     {
         return new Point2D.Double(highOffX, highOffY);
     }
 
 	/**
 	 * Method to add everything in an area to the selection.
 	 * @param wnd the window being examined.
 	 * @param minSelX the low X coordinate of the area in database units.
 	 * @param maxSelX the high X coordinate of the area in database units.
 	 * @param minSelY the low Y coordinate of the area in database units.
 	 * @param maxSelY the high Y coordinate of the area in database units.
 	 * @param invertSelection is true to invert the selection (remove what is already highlighted and add what is new).
 	 * @param findSpecial is true to find hard-to-select objects.
 	 */
 	public static synchronized void selectArea(EditWindow wnd, double minSelX, double maxSelX, double minSelY, double maxSelY,
 		boolean invertSelection, boolean findSpecial)
 	{
 		Rectangle2D searchArea = new Rectangle2D.Double(minSelX, minSelY, maxSelX - minSelX, maxSelY - minSelY);
 		List underCursor = findAllInArea(wnd.getCell(), false, false, false, false, findSpecial, true, searchArea, wnd);
 		if (invertSelection)
 		{
 			for(Iterator it = underCursor.iterator(); it.hasNext(); )
 			{
 				Highlight newHigh = (Highlight)it.next();
 				boolean found = false;
 				for(int i=0; i<highlightList.size(); i++)
 				{
 					Highlight oldHigh = (Highlight)highlightList.get(i);
 					if (newHigh.sameThing(oldHigh))
 					{
 						highlightList.remove(i);
 						found = true;
 						break;
 					}
 				}
 				if (found) continue;
 				highlightList.add(newHigh);
 			}
 		} else
 		{
 			setHighlightList(underCursor);
 		}
 	}
 
 	/**
 	 * Method to tell whether a point is over this Highlight.
 	 * @param wnd the window being examined.
 	 * @param x the X screen coordinate of the point.
 	 * @param y the Y screen coordinate of the point.
 	 * @return true if the point is over this Highlight.
 	 */
 	public static synchronized boolean overHighlighted(EditWindow wnd, int x, int y)
 	{
 		for(Iterator it = getHighlights(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 			Highlight.Type style = h.getType();
 			if (style == Highlight.Type.TEXT)
 			{
 				Point2D start = wnd.screenToDatabase((int)x, (int)y);
 				Poly poly = h.getElectricObject().computeTextPoly(wnd, h.getVar(), h.getName());
 				if (poly.isInside(start)) return true;
 			} else if (style == Highlight.Type.EOBJ)
 			{
 				Point2D slop = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE*2, EXACTSELECTDISTANCE*2);
 				double directHitDist = slop.getX();
 				Point2D start = wnd.screenToDatabase((int)x, (int)y);
 				Rectangle2D searchArea = new Rectangle2D.Double(start.getX(), start.getY(), 0, 0);
 
 				ElectricObject eobj = h.getElectricObject();
 				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
 				if (eobj instanceof Geometric)
 				{
 					Highlight got = checkOutObject((Geometric)eobj, true, false, true, searchArea, wnd, directHitDist, false);
 					if (got == null) continue;
 					ElectricObject hObj = got.getElectricObject();
 					ElectricObject hReal = hObj;
 					if (hReal instanceof PortInst) hReal = ((PortInst)hReal).getNodeInst();
 					for(Iterator sIt = getHighlights(); sIt.hasNext(); )
 					{
 						Highlight alreadyHighlighted = (Highlight)sIt.next();
 						if (alreadyHighlighted.getType() != got.getType()) continue;
 						ElectricObject aHObj = alreadyHighlighted.getElectricObject();
 						ElectricObject aHReal = aHObj;
 						if (aHReal instanceof PortInst) aHReal = ((PortInst)aHReal).getNodeInst();
 						if (hReal == aHReal)
 						{
 							// found it: adjust the port/point
 							if (hObj != aHObj || alreadyHighlighted.getPoint() != got.getPoint())
 							{
 								alreadyHighlighted.setElectricObject(got.getElectricObject());
 								alreadyHighlighted.setPoint(got.getPoint());
 								wnd.repaintContents(null);
 							}
 							break;
 						}
 					}
 					return true;
 				}
 			}
 		}
 		return false;	
 	}
 
 	/** for drawing solid lines */		private static final BasicStroke solidLine = new BasicStroke(0);
     /** for drawing dotted lines */		private static final BasicStroke dottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1}, 0);
     /** for drawing dashed lines */		private static final BasicStroke dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {10}, 0);
 
 	/**
 	 * Method to display this Highlight in a window.
 	 * @param wnd the window in which to draw this highlight.
 	 * @param g the Graphics associated with the window.
 	 */
 	public void showHighlight(EditWindow wnd, Graphics g)
 	{
 		g.setColor(new Color(User.getColorHighlight()));
 		if (type == Type.BBOX)
 		{
 			Point2D [] points = new Point2D.Double[5];
 			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			points[4] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			drawOutlineFromPoints(wnd, g, points, highOffX, highOffY, false, null);
 			return;
 		}
 		if (type == Type.LINE)
 		{
 			Point2D [] points = new Point2D.Double[2];
 			points[0] = new Point2D.Double(pt1.getX(), pt1.getY());
 			points[1] = new Point2D.Double(pt2.getX(), pt2.getY());
 			drawOutlineFromPoints(wnd, g, points, highOffX, highOffY, false, null);
 			return;
 		}
 		if (type == Type.THICKLINE)
 		{
 			Point2D [] points = new Point2D.Double[2];
 			points[0] = new Point2D.Double(pt1.getX(), pt1.getY());
 			points[1] = new Point2D.Double(pt2.getX(), pt2.getY());
 			drawOutlineFromPoints(wnd, g, points, highOffX, highOffY, false, center);
 			return;
 		}
 		if (type == Type.TEXT)
 		{
 			Point2D [] points = describeHighlightText(wnd, getElectricObject(), getVar(), getName());
 			if (points == null) return;
 			Point2D [] linePoints = new Point2D[2];
 			for(int i=0; i<points.length; i += 2)
 			{
 				linePoints[0] = points[i];
 				linePoints[1] = points[i+1];
 				drawOutlineFromPoints(wnd, g, linePoints, highOffX, highOffY, false, null);
 			}
 			return;
 		}
 		if (type == Type.MESSAGE)
 		{
 			Point loc = wnd.databaseToScreen(pt1.getX(), pt1.getY());
 			g.drawString(msg, loc.x, loc.y);
 		}
 
 		// highlight ArcInst
 		if (eobj instanceof ArcInst)
 		{
 			ArcInst ai = (ArcInst)eobj;
 
 			// construct the polygons that describe the basic arc
 			Poly poly = ai.makePoly(ai.getLength(), ai.getWidth() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
 			if (poly == null) return;
 			drawOutlineFromPoints(wnd, g, poly.getPoints(), highOffX, highOffY, false, null);
 
 			if (getNumHighlights() == 1)
 			{
 				// this is the only thing highlighted: give more information about constraints
 				String constraints = "X";
 				if (ai.isRigid()) constraints = "R"; else
 				{
 					if (ai.isFixedAngle())
 					{
 						if (ai.isSlidable()) constraints = "FS"; else
 							constraints = "F";
 					} else if (ai.isSlidable()) constraints = "S";
 				}
 				Point p = wnd.databaseToScreen(ai.getTrueCenterX(), ai.getTrueCenterY());
 				Font font = wnd.getFont(null);
 				if (font != null)
 				{
 					GlyphVector gv = wnd.getGlyphs(constraints, font);
 					Rectangle2D glyphBounds = gv.getVisualBounds();
 					g.drawString(constraints, (int)(p.x - glyphBounds.getWidth()/2 + highOffX),
 						(int)(p.y + font.getSize()/2 + highOffY));
 				}
 			}
 			return;
 		}
 
 		// highlight NodeInst
 		PortProto pp = null;
 		ElectricObject realEObj = eobj;
 		if (realEObj instanceof PortInst)
 		{
 			pp = ((PortInst)realEObj).getPortProto();
 			realEObj = ((PortInst)realEObj).getNodeInst();
 		}
 		if (realEObj instanceof NodeInst)
 		{
 			NodeInst ni = (NodeInst)realEObj;
 			NodeProto np = ni.getProto();
 			AffineTransform trans = ni.rotateOutAboutTrueCenter();
 			boolean drewOutline = false;
 			if (np instanceof PrimitiveNode)
 			{
 				// special case for outline nodes
 				if (np.isHoldsOutline()) 
 				{
 					Point2D [] outline = ni.getTrace();
 					if (outline != null)
 					{
 						int numPoints = outline.length;
 						Point2D [] pointList = new Point2D.Double[numPoints];
 						for(int i=0; i<numPoints; i++)
 						{
 							pointList[i] = new Point2D.Double(ni.getTrueCenterX() + outline[i].getX(),
 								ni.getTrueCenterY() + outline[i].getY());
 						}
 						trans.transform(pointList, 0, pointList, 0, numPoints);
 						drawOutlineFromPoints(wnd, g, pointList, 0, 0, true, null);
 						drewOutline = true;
 					}
 				}
 			}
 
 			// setup outline of node with standard offset
 			int offX = highOffX;
 			int offY = highOffY;
 			if (!drewOutline)
 			{
 				SizeOffset so = ni.getSizeOffset();
 				double nodeLowX = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
 				double nodeHighX = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
 				double nodeLowY = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
 				double nodeHighY = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();
 				if (nodeLowX == nodeHighX && nodeLowY == nodeHighY)
 				{
 					float x = (float)nodeLowX;
 					float y = (float)nodeLowY;
 					float size = 3 / (float)wnd.getScale();
 					Point c1 = wnd.databaseToScreen(x+size, y);
 					Point c2 = wnd.databaseToScreen(x-size, y);
 					Point c3 = wnd.databaseToScreen(x, y+size);
 					Point c4 = wnd.databaseToScreen(x, y-size);
 					drawLine(g, wnd, c1.x + offX, c1.y + offY, c2.x + offX, c2.y + offY);
 					drawLine(g, wnd, c3.x + offX, c3.y + offY, c4.x + offX, c4.y + offY);
 				} else
 				{
 					double nodeX = (nodeLowX + nodeHighX) / 2;
 					double nodeY = (nodeLowY + nodeHighY) / 2;
 					Poly poly = new Poly(nodeX, nodeY, nodeHighX-nodeLowX, nodeHighY-nodeLowY);
 					poly.transform(trans);
 					drawOutlineFromPoints(wnd, g, poly.getPoints(), offX, offY, false, null);
 				}
 			}
 
 			// draw the selected point
 			if (point >= 0)
 			{
 				Point2D [] points = ni.getTrace();
 				if (points != null)
 				{
 					boolean showWrap = ni.traceWraps();
 					double x = ni.getAnchorCenterX() + points[point].getX();
 					double y = ni.getAnchorCenterY() + points[point].getY();
 					Point2D thisPt = new Point2D.Double(x, y);
 					trans.transform(thisPt, thisPt);
 					Point cThis = wnd.databaseToScreen(thisPt);
 					int size = 3;
 					drawLine(g, wnd, cThis.x + size + offX, cThis.y + size + offY, cThis.x - size + offX, cThis.y - size + offY);
 					drawLine(g, wnd, cThis.x + size + offX, cThis.y - size + offY, cThis.x - size + offX, cThis.y + size + offY);
 
 					// draw two connected lines
 					Point2D prevPt = null, nextPt = null;
 					int prevPoint = point - 1;
 					if (prevPoint < 0 && showWrap) prevPoint = points.length - 1;
 					if (prevPoint >= 0)
 					{
 						prevPt = new Point2D.Double(ni.getAnchorCenterX() + points[prevPoint].getX(),
 							ni.getAnchorCenterY() + points[prevPoint].getY());
 						trans.transform(prevPt, prevPt);
 						if (prevPt.getX() == thisPt.getX() && prevPt.getY() == thisPt.getY()) prevPoint = -1; else
 						{
 							Point cPrev = wnd.databaseToScreen(prevPt);
 							drawLine(g, wnd, cThis.x + offX, cThis.y + offY, cPrev.x, cPrev.y);
 						}
 					}
 					int nextPoint = point + 1;
 					if (nextPoint >= points.length)
 					{
 						if (showWrap) nextPoint = 0; else
 							nextPoint = -1;
 					}
 					if (nextPoint >= 0)
 					{
 						nextPt = new Point2D.Double(ni.getAnchorCenterX() + points[nextPoint].getX(),
 							ni.getAnchorCenterY() + points[nextPoint].getY());
 						trans.transform(nextPt, nextPt);
 						if (nextPt.getX() == thisPt.getX() && nextPt.getY() == thisPt.getY()) nextPoint = -1; else
 						{
 							Point cNext = wnd.databaseToScreen(nextPt);
 							drawLine(g, wnd, cThis.x + offX, cThis.y + offY, cNext.x, cNext.y);
 						}
 					}
 
 					// draw arrows on the lines
 					if (offX == 0 && offY == 0 && points.length > 2)
 					{
 						double arrowLen = Double.MAX_VALUE;
 						if (prevPoint >= 0) arrowLen = Math.min(thisPt.distance(prevPt), arrowLen);
 						if (nextPoint >= 0) arrowLen = Math.min(thisPt.distance(nextPt), arrowLen);
 						arrowLen /= 10;
 						if (prevPoint >= 0)
 						{
 							Point2D prevCtr = new Point2D.Double((prevPt.getX()+thisPt.getX()) / 2,
 								(prevPt.getY()+thisPt.getY()) / 2);
 							double prevAngle = DBMath.figureAngleRadians(prevPt, thisPt);
 							Point2D prevArrow1 = new Point2D.Double(prevCtr.getX() + Math.cos(prevAngle+Math.PI*0.75) * arrowLen,
 								prevCtr.getY() + Math.sin(prevAngle+Math.PI*0.75) * arrowLen);
 							Point2D prevArrow2 = new Point2D.Double(prevCtr.getX() + Math.cos(prevAngle-Math.PI*0.75) * arrowLen,
 								prevCtr.getY() + Math.sin(prevAngle-Math.PI*0.75) * arrowLen);
 							Point cPrevCtr = wnd.databaseToScreen(prevCtr);
 							Point cPrevArrow1 = wnd.databaseToScreen(prevArrow1);
 							Point cPrevArrow2 = wnd.databaseToScreen(prevArrow2);
 							drawLine(g, wnd, cPrevCtr.x, cPrevCtr.y, cPrevArrow1.x, cPrevArrow1.y);
 							drawLine(g, wnd, cPrevCtr.x, cPrevCtr.y, cPrevArrow2.x, cPrevArrow2.y);
 						}
 
 						if (nextPoint >= 0)
 						{
 							Point2D nextCtr = new Point2D.Double((nextPt.getX()+thisPt.getX()) / 2,
 								(nextPt.getY()+thisPt.getY()) / 2);
 							double nextAngle = DBMath.figureAngleRadians(thisPt, nextPt);
 							Point2D nextArrow1 = new Point2D.Double(nextCtr.getX() + Math.cos(nextAngle+Math.PI*0.75) * arrowLen,
 								nextCtr.getY() + Math.sin(nextAngle+Math.PI*0.75) * arrowLen);
 							Point2D nextArrow2 = new Point2D.Double(nextCtr.getX() + Math.cos(nextAngle-Math.PI*0.75) * arrowLen,
 								nextCtr.getY() + Math.sin(nextAngle-Math.PI*0.75) * arrowLen);
 							Point cNextCtr = wnd.databaseToScreen(nextCtr);
 							Point cNextArrow1 = wnd.databaseToScreen(nextArrow1);
 							Point cNextArrow2 = wnd.databaseToScreen(nextArrow2);
 							drawLine(g, wnd, cNextCtr.x, cNextCtr.y, cNextArrow1.x, cNextArrow1.y);
 							drawLine(g, wnd, cNextCtr.x, cNextCtr.y, cNextArrow2.x, cNextArrow2.y);
 						}
 					}
 
 					// do not offset the node, just this point
 					offX = offY = 0;
 				}
 			}
 
 			// draw the selected port
 			if (pp != null)
 			{
 				g.setColor(new Color(User.getColorPortHighlight()));
 				Poly poly = ni.getShapeOfPort(pp);
 				boolean opened = true;
 				if (poly.getStyle() == Poly.Type.FILLED || poly.getStyle() == Poly.Type.CLOSED) opened = false;
 				if (poly.getStyle() == Poly.Type.CIRCLE || poly.getStyle() == Poly.Type.THICKCIRCLE ||
 					poly.getStyle() == Poly.Type.DISC)
 				{
 					Point2D [] points = poly.getPoints();
 					double sX = points[0].distance(points[1]) * 2;
 					Point2D [] pts = Artwork.fillEllipse(points[0], sX, sX, 0, 360);
 					drawOutlineFromPoints(wnd, g, pts, offX, offY, opened, null);
 				} else if (poly.getStyle() == Poly.Type.CIRCLEARC)
 				{
 					Point2D [] points = poly.getPoints();
 					double [] angles = ni.getArcDegrees();
 					double sX = points[0].distance(points[1]) * 2;
 					Point2D [] pts = Artwork.fillEllipse(points[0], sX, sX, angles[0], angles[1]);
 					drawOutlineFromPoints(wnd, g, pts, offX, offY, opened, null);
 				} else
 				{
 					drawOutlineFromPoints(wnd, g, poly.getPoints(), offX, offY, opened, null);
 				}
 				g.setColor(new Color(User.getColorHighlight()));
 
                 // show name of port
                 if (!(np instanceof PrimitiveNode) && (g instanceof Graphics2D))
 				{
 					// only show name if port is wired (because all other situations already show the port)
 					boolean wired = false;
 					for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
 					{
 						Connection con = (Connection)cIt.next();
 						if (con.getPortInst().getPortProto() == pp) { wired = true;   break; }
 					}
 					if (wired)
 					{
 	                    Font font = new Font(User.getDefaultFont(), Font.PLAIN, (int)(1.5*EditWindow.getDefaultFontSize()));
     	                GlyphVector v = wnd.getGlyphs(pp.getName(), font);
         	            Point2D point = wnd.databaseToScreen(poly.getCenterX(), poly.getCenterY());
             	        ((Graphics2D)g).drawGlyphVector(v, (float)point.getX()+offX, (float)point.getY()+offY);
 					}
                 }
 
 				// handle verbose highlighting of nodes
 				Netlist netlist = cell.getUserNetlist();
 				Nodable no = Netlist.getNodableFor(ni, 0);
 				PortProto epp = pp.getEquivalent();
 				if (epp == null) epp = pp;
 				int busWidth = pp.getNameKey().busWidth();
 
 				FlagSet markObj = Geometric.getFlagSet(1);
 				for(Iterator it = cell.getNodes(); it.hasNext(); )
 					((NodeInst)it.next()).clearBit(markObj);
 				for(Iterator it = cell.getArcs(); it.hasNext(); )
 				{
 					ArcInst ai = (ArcInst)it.next();
 					ai.clearBit(markObj);
 					if (!netlist.sameNetwork(no, epp, ai)) continue;
 
 					ai.setBit(markObj);
 					ai.getHead().getPortInst().getNodeInst().setBit(markObj);
 					ai.getTail().getPortInst().getNodeInst().setBit(markObj);
 				}
 
 				// draw lines along all of the arcs on the network
 				Graphics2D g2 = (Graphics2D)g;
 				g2.setStroke(dashedLine);
 				for(Iterator it = cell.getArcs(); it.hasNext(); )
 				{
 					ArcInst ai = (ArcInst)it.next();
 					if (!ai.isBit(markObj)) continue;
 					Point c1 = wnd.databaseToScreen(ai.getHead().getLocation());
 					Point c2 = wnd.databaseToScreen(ai.getTail().getLocation());
 					drawLine(g, wnd, c1.x, c1.y, c2.x, c2.y);
 				}
 
 				// draw dots in all connected nodes
 				for(Iterator it = cell.getNodes(); it.hasNext(); )
 				{
 					NodeInst oNi = (NodeInst)it.next();
 					if (oNi == ni) continue;
 					if (!oNi.isBit(markObj)) continue;
 
 					Point c = wnd.databaseToScreen(oNi.getTrueCenter());
 					g.fillOval(c.x-4, c.y-4, 8, 8);
 
 					// connect the center dots to the input arcs
 					Point2D nodeCenter = oNi.getTrueCenter();
 					for(Iterator pIt = oNi.getConnections(); pIt.hasNext(); )
 					{
 						Connection con = (Connection)pIt.next();
 						ArcInst ai = con.getArc();
 						if (!ai.isBit(markObj)) continue;
 						Point2D arcEnd = con.getLocation();
 						if (arcEnd.getX() != nodeCenter.getX() || arcEnd.getY() != nodeCenter.getY())
 						{
 							Point c1 = wnd.databaseToScreen(arcEnd);
 							Point c2 = wnd.databaseToScreen(nodeCenter);
 							g2.setStroke(dottedLine);
 							drawLine(g, wnd, c1.x, c1.y, c2.x, c2.y);
 						}
 					}
 				}
 				g2.setStroke(solidLine);
 				markObj.freeFlagSet();
 			}
 		}
 	}
 
 	/**
 	 * Method to convert this Highlight to a series of points that describes the text.
 	 * It is assumed that this Highlight describes text.
 	 */
 	public static Point2D [] describeHighlightText(EditWindow wnd, ElectricObject eObj, Variable var, Name name)
 	{
 		Poly poly = eObj.computeTextPoly(wnd, var, name);
 		if (poly == null) return null;
 		Rectangle2D bounds = poly.getBounds2D();
 		Poly.Type style = poly.getStyle();
 		style = Poly.rotateType(style, eObj);
 		if (style == Poly.Type.TEXTCENT)
 		{
 			Point2D [] points = new Point2D.Double[4];
 			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTBOT)
 		{
 			Point2D [] points = new Point2D.Double[6];
 			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			points[4] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			points[5] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTTOP)
 		{
 			Point2D [] points = new Point2D.Double[6];
 			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[4] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[5] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTLEFT)
 		{
 			Point2D [] points = new Point2D.Double[6];
 			points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[3] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[4] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[5] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTRIGHT)
 		{
 			Point2D [] points = new Point2D.Double[6];
 			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[4] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[5] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTTOPLEFT)
 		{
 			Point2D [] points = new Point2D.Double[4];
 			points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[3] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTBOTLEFT)
 		{
 			Point2D [] points = new Point2D.Double[4];
 			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[2] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTTOPRIGHT)
 		{
 			Point2D [] points = new Point2D.Double[4];
 			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
 			points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTBOTRIGHT)
 		{
 			Point2D [] points = new Point2D.Double[4];
 			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
 			points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
 			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
 			return points;
 		}
 		if (style == Poly.Type.TEXTBOX)
 		{
 			Point2D [] points = new Point2D.Double[12];
 			if (eObj instanceof Geometric)
 			{
 				bounds = ((Geometric)eObj).getBounds();
 			}
 			double lX = bounds.getMinX();
 			double hX = bounds.getMaxX();
 			double lY = bounds.getMinY();
 			double hY = bounds.getMaxY();
 			points[0] = new Point2D.Double(lX, lY);
 			points[1] = new Point2D.Double(hX, hY);
 			points[2] = new Point2D.Double(lX, hY);
 			points[3] = new Point2D.Double(hX, lY);
 			double shrinkX = (hX - lX) / 5;
 			double shrinkY = (hY - lY) / 5;
 			points[4] = new Point2D.Double(lX+shrinkX, lY);
 			points[5] = new Point2D.Double(hX-shrinkX, lY);
 			points[6] = new Point2D.Double(lX+shrinkX, hY);
 			points[7] = new Point2D.Double(hX-shrinkX, hY);
 			points[8] = new Point2D.Double(lX, lY+shrinkY);
 			points[9] = new Point2D.Double(lX, hY-shrinkY);
 			points[10] = new Point2D.Double(hX, lY+shrinkY);
 			points[11] = new Point2D.Double(hX, hY-shrinkY);
 			return points;
 		}
 		return null;
 	}
 
 	/**
 	 * Method to handle a click in a window and select the appropriate objects.
 	 * @param pt the coordinates of the click (in database units).
 	 * @param wnd the window being examined.
 	 * @param exclusively true if the currently selected object must remain selected.
 	 * This happens during "outline edit" when the node doesn't change, just the point on it.
 	 * @param another true to find another object under the point (when there are multiple ones).
 	 * @param invert true to invert selection (add if not selected, remove if already selected).
 	 * @param findPort true to also show the closest port on a selected node.
 	 * @param findPoint true to also show the closest point on a selected outline node.
 	 * @param findSpecial true to select hard-to-find objects.
 	 * @param findText true to select text objects.
 	 * The name of an unexpanded cell instance is always hard-to-select.
 	 * Other objects are set this way by the user (although the cell-center is usually set this way).
 	 */
 	public static int findObject(Point2D pt, EditWindow wnd, boolean exclusively,
 		boolean another, boolean invert, boolean findPort, boolean findPoint, boolean findSpecial, boolean findText)
 	{
 		// initialize
 		double bestdist = Double.MAX_VALUE;
 		boolean looping = false;
 		
 		// search the relevant objects in the circuit
 		Cell cell = wnd.getCell();
		Rectangle2D bounds = new Rectangle2D.Double(pt.getX()-0.5, pt.getY()-0.5, 1, 1);
 		List underCursor = findAllInArea(cell, exclusively, another, findPort, findPoint, findSpecial, findText, bounds, wnd);
 
 		// if nothing under the cursor, stop now
 		if (underCursor.size() == 0)
 		{
 			if (!invert)
 			{
 				clear();
 				finished();
 			}
 			return 0;
 		}
 
 		// multiple objects under the cursor: see if looping through them
 		if (underCursor.size() > 1 && another)
 		{
 			for(int j=0; j<highlightList.size(); j++)
 			{
 				Highlight oldHigh = (Highlight)highlightList.get(j);
 				for(int i=0; i<underCursor.size(); i++)
 				{
 					if (oldHigh.sameThing((Highlight)underCursor.get(i)))
 					{
 						// found the same thing: loop
 						if (invert)
 						{
 							highlightList.remove(j);
 						} else
 						{
 							clear();
 						}
 						if (i < underCursor.size()-1)
 						{
 							highlightList.add(underCursor.get(i+1));
 						} else
 						{
 							highlightList.add(underCursor.get(0));
 						}
 						finished();
 						return 1;
 					}
 				}
 			}
 		}
 
 		// just use the first in the list
 		if (invert)
 		{
 			Highlight newHigh = (Highlight)underCursor.get(0);
 			for(int i=0; i<highlightList.size(); i++)
 			{
 				if (newHigh.sameThing((Highlight)highlightList.get(i)))
 				{
 					highlightList.remove(i);
 					finished();
 					return 1;
 				}
 			}
 			highlightList.add(newHigh);
 			finished();
 		} else
 		{
 			clear();
 			highlightList.add(underCursor.get(0));
 			finished();
 		}
 
 //		// reevaluate if this is code
 //		if ((curhigh->status&HIGHTYPE) == HIGHTEXT && curhigh->fromvar != NOVARIABLE &&
 //			curhigh->fromvarnoeval != NOVARIABLE &&
 //				curhigh->fromvar != curhigh->fromvarnoeval)
 //					curhigh->fromvar = evalvar(curhigh->fromvarnoeval, 0, 0);
 		return 1;
 	}
 
 	/**
 	 * Returns a printable version of this Highlight.
 	 * @return a printable version of this Highlight.
 	 */
 	public String toString() { return "Highlight "+type; }
 
 	// ************************************* SUPPORT *************************************
 
 	/**
 	 * Method to search a Cell for all objects at a point.
 	 * @param cell the cell to search.
 	 * @param exclusively true if the currently selected object must remain selected.
 	 * This happens during "outline edit" when the node doesn't change, just the point on it.
 	 * @param another true to find another object under the point (when there are multiple ones).
 	 * @param findPort true to also show the closest port on a selected node.
 	 * @param findPoint true to also show the closest point on a selected outline node.
 	 * @param findSpecial true to select hard-to-find objects.
 	 * @param findText true to select text objects.
 	 * The name of an unexpanded cell instance is always hard-to-select.
 	 * Other objects are set this way by the user (although the cell-center is usually set this way).
 	 * @param bounds the area of the search (in database units).
 	 * @param wnd the window being examined (null to ignore window scaling).
 	 * @return a list of Highlight objects.
 	 * The list is ordered by importance, so the deault action is to select the first entry.
 	 */
 	public static List findAllInArea(Cell cell, boolean exclusively, boolean another, boolean findPort,
 		 boolean findPoint, boolean findSpecial, boolean findText, Rectangle2D bounds, EditWindow wnd)
 	{
 		// make a list of things under the cursor
 		List list = new ArrayList();
 
 		boolean areaMustEnclose = User.isDraggingMustEncloseObjects();
 
 		// this is the distance from an object that is necessary for a "direct hit"
 		double directHitDist = 0;
 		if (wnd != null)
 		{
 			Point2D extra = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE, EXACTSELECTDISTANCE);
			directHitDist = extra.getX();
 		}
 
 		// look for text if a window was given
 		if (findText && wnd != null)
 		{
 			// start by examining all text on this Cell
 			if (User.isTextVisibilityOnCell())
 			{
 				Poly [] polys = cell.getAllText(findSpecial, wnd);
 				if (polys != null)
 				{
 					for(int i=0; i<polys.length; i++)
 					{
 						Poly poly = polys[i];
 						if (poly.setExactTextBounds(wnd, cell)) continue;
 
                         // ignore areaMustEnclose if bounds is size 0,0
 						if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
 						{
 							if (!poly.isInside(bounds)) continue;
 						} else
 						{
 							if (poly.polyDistance(bounds) >= directHitDist) continue;
 						}
 						Highlight h = new Highlight(Type.TEXT);
 						h.setElectricObject(cell);
 						h.setCell(cell);
 						h.setVar(poly.getVariable());
 						list.add(h);
 					}
 				}
 			}
 
 			// next examine all text on nodes in the cell
 			for(Iterator it = cell.getNodes(); it.hasNext(); )
 			{
 				NodeInst ni = (NodeInst)it.next();
 				AffineTransform trans = ni.rotateOut();
 				EditWindow subWnd = wnd;
 				Poly [] polys = ni.getAllText(findSpecial, wnd);
 				if (polys == null) continue;
 				for(int i=0; i<polys.length; i++)
 				{
 					Poly poly = polys[i];
 					poly.transform(trans);
 					if (poly.setExactTextBounds(wnd, ni)) continue;
 
                     // ignore areaMustEnclose if bounds is size 0,0
 					if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
 					{
 						if (!poly.isInside(bounds)) continue;
 					} else
 					{
 						double hitdist = poly.polyDistance(bounds);
 						if (hitdist >= directHitDist) continue;
 					}
 					Highlight h = new Highlight(Type.TEXT);
 					if (poly.getPort() != null)
 					{
 						PortProto pp = poly.getPort();
 						h.setElectricObject(pp);
 						for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
 						{
 							PortInst pi = (PortInst)pIt.next();
 							if (pi.getPortProto() == pp)
 							{
 								h.setElectricObject(pi);
 								break;
 							}
 						}
 					} else
 						h.setElectricObject(ni);
 					h.setCell(cell);
 					h.setVar(poly.getVariable());
 					h.setName(poly.getName());
 					list.add(h);
 				}
 			}
 
 			// next examine all text on arcs in the cell
 			for(Iterator it = cell.getArcs(); it.hasNext(); )
 			{
 				ArcInst ai = (ArcInst)it.next();
 				if (User.isTextVisibilityOnArc())
 				{
 					Poly [] polys = ai.getAllText(findSpecial, wnd);
 					if (polys == null) continue;
 					for(int i=0; i<polys.length; i++)
 					{
 						Poly poly = polys[i];
 						if (poly.setExactTextBounds(wnd, ai)) continue;
 
                         // ignore areaMustEnclose if bounds is size 0,0
                         if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
 						{
 							if (!poly.isInside(bounds)) continue;
 						} else
 						{
 							if (poly.polyDistance(bounds) >= directHitDist) continue;
 						}
 						Highlight h = new Highlight(Type.TEXT);
 						h.setElectricObject(ai);
 						h.setCell(cell);
 						h.setVar(poly.getVariable());
 						h.setName(poly.getName());
 						list.add(h);
 					}
 				}
 			}
 		}
 
 		if (exclusively)
 		{
 			// special case: only review what is already highlighted
 			for(Iterator sIt = getHighlights(); sIt.hasNext(); )
 			{
 				Highlight h = (Highlight)sIt.next();
 				if (h.getType() != Type.EOBJ) continue;
 				ElectricObject eobj = h.getElectricObject();
 				if (eobj instanceof PortInst) eobj = ((PortInst)eobj).getNodeInst();
 				if (eobj instanceof NodeInst)
 				{
 					h = checkOutObject((Geometric)eobj, findPort, findPoint, findSpecial, bounds, wnd, Double.MAX_VALUE, areaMustEnclose);
 					if (h != null) list.add(h);
 				}
 			}
 			return list;
 		}
 
 		// determine proper area to search
 		Rectangle2D searchArea = new Rectangle2D.Double(bounds.getMinX() - directHitDist,
 			bounds.getMinY() - directHitDist, bounds.getWidth()+directHitDist*2, bounds.getHeight()+directHitDist*2);
 
 		// now do 3 phases of examination: cells, arcs, then primitive nodes
 		for(int phase=0; phase<3; phase++)
 		{
 			// ignore cells if requested
 			if (phase == 0 && !findSpecial && !User.isEasySelectionOfCellInstances()) continue;
 
 			// examine everything in the area
 			for(Iterator it = cell.searchIterator(searchArea); it.hasNext(); )
 			{
 				Geometric geom = (Geometric)it.next();
 
 				Highlight h;
 				switch (phase)
 				{
 					case 0:			// check primitive nodes
 						if (!(geom instanceof NodeInst)) break;
 						if (((NodeInst)geom).getProto() instanceof Cell) break;
 						h = checkOutObject(geom, findPort, findPoint, findSpecial, bounds, wnd, directHitDist, areaMustEnclose);
 						if (h != null) list.add(h);
 						break;
 					case 1:			// check Cell instances
 						if (!(geom instanceof NodeInst)) break;
 						if (((NodeInst)geom).getProto() instanceof PrimitiveNode) break;
 						h = checkOutObject(geom, findPort, findPoint, findSpecial, bounds, wnd, directHitDist, areaMustEnclose);
 						if (h != null) list.add(h);
 						break;
 					case 2:			// check arcs
 						if (!(geom instanceof ArcInst)) break;
 						h = checkOutObject(geom, findPort, findPoint, findSpecial, bounds, wnd, directHitDist, areaMustEnclose);
 						if (h != null) list.add(h);
 						break;
 				}
 			}
 		}
 		return list;
 	}
 
 	/**
 	 * Method to determine whether an object is in a bounds.
 	 * @param geom the Geometric being tested for selection.
 	 * @param findPort true if a port should be selected with a NodeInst.
 	 * @param findPoint true if a point should be selected with an outline NodeInst.
 	 * @param findSpecial true if hard-to-select and other special selection is being done.
 	 * @param bounds the selected area or point.
 	 * @param wnd the window being examined (null to ignore window scaling).
 	 * @param directHitDist the slop area to forgive when searching (a few pixels in screen space, transformed to database units).
 	 * @param areaMustEnclose true if the object must be completely inside of the selection area.
 	 * @return a Highlight that defines the object, or null if the point is not over any part of this object.
 	 */
 	private static Highlight checkOutObject(Geometric geom, boolean findPort, boolean findPoint, boolean findSpecial, Rectangle2D bounds,
 		EditWindow wnd, double directHitDist, boolean areaMustEnclose)
 	{
         // ignore areaMustEnclose if bounds is size 0,0
         if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
 		{
 			Rectangle2D geomBounds = geom.getBounds();
 			Poly poly = new Poly(geomBounds);
 			if (!poly.isInside(bounds)) return null;
 		}
 
 		if (geom instanceof NodeInst)
 		{
 			// examine a node object
 			NodeInst ni = (NodeInst)geom;
 
 			// do not "find" hard-to-find nodes if "findSpecial" is not set
 			boolean hardToSelect = ni.isHardSelect();
 			boolean ignoreCells = !User.isEasySelectionOfCellInstances();
 			if ((ni.getProto() instanceof Cell) && ignoreCells) hardToSelect = true;
 			if (!findSpecial && hardToSelect) return null;
 
 			// do not include primitives that have all layers invisible
 //			if (ni.getProto() instanceof PrimitiveNode && (ni->proto->userbits&NINVISIBLE) != 0) return;
 
 			// do not "find" Invisible-Pins if they have text or exports
 			if (ni.isInvisiblePinWithText())
 				return null;
 
 			// get the distance to the object
 			double dist = distToNode(bounds, ni, wnd);
 
 			// direct hit
 			if (dist < directHitDist)
 			{
 				Highlight h = new Highlight(Type.EOBJ);
 				ElectricObject eobj = geom;
 
 				// add the closest port
 				if (findPort)
 				{
 					double bestDist = Double.MAX_VALUE;
 					PortInst bestPort = null;
 					for(Iterator it = ni.getPortInsts(); it.hasNext(); )
 					{
 						PortInst pi = (PortInst)it.next();
 						Poly poly = pi.getPoly();
 						dist = poly.polyDistance(bounds);
 						if (dist < bestDist)
 						{
 							bestDist = dist;
 							bestPort = pi;
 						}
 					}
 					if (bestPort != null) eobj = bestPort;
 				}
 
 				// add the closest point
 				if (findPoint)
 				{
 					Point2D [] points = ni.getTrace();
 					Point2D cursor = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
 					if (points != null)
 					{
 						double bestDist = Double.MAX_VALUE;
 						int bestPoint = -1;
 						AffineTransform trans = ni.rotateOutAboutTrueCenter();
 						for(int i=0; i<points.length; i++)
 						{
 							Point2D pt = new Point2D.Double(ni.getAnchorCenterX() + points[i].getX(),
 								ni.getAnchorCenterY() + points[i].getY());
 							trans.transform(pt, pt);
 							dist = pt.distance(cursor);
 							if (dist < bestDist)
 							{
 								bestDist = dist;
 								bestPoint = i;
 							}
 						}
 						if (bestPoint >= 0) h.setPoint(bestPoint);
 					}
 				}
 				h.setElectricObject(eobj);
 				h.setCell(geom.getParent());
 				return h;
 			}
 		} else
 		{
 			// examine an arc object
 			ArcInst ai = (ArcInst)geom;
 
 			// do not "find" hard-to-find arcs if "findSpecial" is not set
 			if (!findSpecial && ai.isHardSelect()) return null;
 
 			// do not include arcs that have all layers invisible
 //			if ((ai->proto->userbits&AINVISIBLE) != 0) return;
 
 			// get distance to arc
 			double dist = distToArc(bounds, ai, wnd);
 
 			// direct hit
 			if (dist < directHitDist)
 			{
 				Highlight h = new Highlight(Type.EOBJ);
 				h.setElectricObject(geom);
 				h.setCell(geom.getParent());
 				return h;
 			}
 		}
 		return null;
 	}
 
 	/**
 	 * Method to return the distance from a bound to a NodeInst.
 	 * @param bounds the bounds in question.
 	 * @param ni the NodeInst.
 	 * @param wnd the window being examined (null to ignore text/window scaling).
 	 * @return the distance from the bounds to the NodeInst.
 	 * Negative values are direct hits.
 	 */
 	private static double distToNode(Rectangle2D bounds, NodeInst ni, EditWindow wnd)
 	{
 		AffineTransform trans = ni.rotateOut();
 
 		NodeProto np = ni.getProto();
 		Poly nodePoly = null;
 		if (np instanceof PrimitiveNode)
 		{
 			// special case for MOS transistors: examine the gate/active tabs
 			NodeProto.Function fun = np.getFunction();
 			if (fun == NodeProto.Function.TRANMOS || fun == NodeProto.Function.TRAPMOS || fun == NodeProto.Function.TRADMOS)
 			{
 				Technology tech = np.getTechnology();
 				Poly [] polys = tech.getShapeOfNode(ni, wnd);
 				double bestDist = Double.MAX_VALUE;
 				for(int box=0; box<polys.length; box++)
 				{
 					Poly poly = polys[box];
 					Layer layer = poly.getLayer();
 					if (layer == null) continue;
 					Layer.Function lf = layer.getFunction();
 					if (!lf.isPoly() && !lf.isDiff()) continue;
 					poly.transform(trans);
                     // ignore areaMustEnclose if bounds is size 0,0
 //					if (areaMustEnclose && (bounds.getHeight() > 0 || bounds.getWidth() > 0))
 //					{
 //						if (!poly.isInside(bounds)) continue;
 //					} else
 //					{
 //						if (poly.polyDistance(bounds) >= directHitDist) continue;
 //					}
 					double dist = poly.polyDistance(bounds);
 					if (dist < bestDist) bestDist = dist;
 				}
 				return bestDist;
 			}
 
 			// special case for 1-polygon primitives: check precise distance to cursor
 			if (np.isEdgeSelect())
 			{
 				Technology tech = np.getTechnology();
 				Poly [] polys = tech.getShapeOfNode(ni, wnd);
 				double bestDist = Double.MAX_VALUE;
 				for(int box=0; box<polys.length; box++)
 				{
 					Poly poly = polys[box];
 					poly.transform(trans);
 					double dist = poly.polyDistance(bounds);
 					if (dist < bestDist) bestDist = dist;
 				}
 				return bestDist;
 			}
 
 			// get the bounds of the node in a polygon
 			SizeOffset so = ni.getSizeOffset();
 			double lX = ni.getAnchorCenterX() - ni.getXSize()/2 + so.getLowXOffset();
 			double hX = ni.getAnchorCenterX() + ni.getXSize()/2 - so.getHighXOffset();
 			double lY = ni.getAnchorCenterY() - ni.getYSize()/2 + so.getLowYOffset();
 			double hY = ni.getAnchorCenterY() + ni.getYSize()/2 - so.getHighYOffset();
 			nodePoly = new Poly((lX + hX) / 2, (lY + hY) / 2, hX-lX, hY-lY);
 		} else
 		{
 			// cell instance
 			Cell subCell = (Cell)np;
 			Rectangle2D instBounds = subCell.getBounds();
 			nodePoly = new Poly(ni.getAnchorCenterX() + instBounds.getCenterX(),
 				ni.getAnchorCenterY() + instBounds.getCenterY(), instBounds.getWidth(), instBounds.getHeight());
 		}
 
 		AffineTransform pureTrans = ni.rotateOut();
 		nodePoly.transform(pureTrans);
 		nodePoly.setStyle(Poly.Type.FILLED);
 		double dist = nodePoly.polyDistance(bounds);
 		return dist;
 	}
 
 	/**
 	 * Method to return the distance from a bounds to an ArcInst.
 	 * @param bounds the bounds in question.
 	 * @param ai the ArcInst.
 	 * @param wnd the window being examined.
 	 * @return the distance from the bounds to the ArcInst.
 	 * Negative values are direct hits or intersections.
 	 */
 	private static double distToArc(Rectangle2D bounds, ArcInst ai, EditWindow wnd)
 	{
 		ArcProto ap = ai.getProto();
 
 		// if arc is selectable precisely, check distance to cursor
 		if (ap.isEdgeSelect())
 		{
 			Technology tech = ap.getTechnology();
 			Poly [] polys = tech.getShapeOfArc(ai, wnd);
 			double bestDist = Double.MAX_VALUE;
 			for(int box=0; box<polys.length; box++)
 			{
 				Poly poly = polys[box];
 				double dist = poly.polyDistance(bounds);
 				if (dist < bestDist) bestDist = dist;
 			}
 			return bestDist;
 		}
 
 		// standard distance to the arc
 		double wid = ai.getWidth() - ai.getProto().getWidthOffset();
 		if (DBMath.doublesEqual(wid, 0)) wid = 1;
 //		if (curvedarcoutline(ai, poly, FILLED, wid))
 			Poly poly = ai.makePoly(ai.getLength(), wid, Poly.Type.FILLED);
 		return poly.polyDistance(bounds);
 	}
 
 	/**
 	 * Method to draw an array of points as highlighting.
 	 * @param wnd the window in which drawing is happening.
 	 * @param g the Graphics for the window.
 	 * @param points the array of points being drawn.
 	 * @param offX the X offset of the drawing.
 	 * @param offY the Y offset of the drawing.
 	 * @param opened true if the points are drawn "opened".
 	 * False to close the polygon.
 	 */
 	private static void drawOutlineFromPoints(EditWindow wnd, Graphics g, Point2D [] points, int offX, int offY, boolean opened, Point2D thickCenter)
 	{
         Dimension screen = wnd.getScreenSize();
 		boolean onePoint = true;
 		Point firstP = wnd.databaseToScreen(points[0].getX(), points[0].getY());
 		for(int i=1; i<points.length; i++)
 		{
 			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
 			if (DBMath.doublesEqual(p.getX(), firstP.getX()) &&
 				DBMath.doublesEqual(p.getY(), firstP.getY())) continue;
 			onePoint = false;
 			break;
 		}
 		if (onePoint)
 		{
 			drawLine(g, wnd, firstP.x + offX-CROSSSIZE, firstP.y + offY, firstP.x + offX+CROSSSIZE, firstP.y + offY);
 			drawLine(g, wnd, firstP.x + offX, firstP.y + offY-CROSSSIZE, firstP.x + offX, firstP.y + offY+CROSSSIZE);
 			return;
 		}
 
 		// find the center
 		int cX = 0, cY = 0;
 		if (thickCenter != null)
 		{
 			Point lp = wnd.databaseToScreen(thickCenter.getX(), thickCenter.getY());
 			cX = lp.x;
 			cY = lp.y;
 		}
 
 		for(int i=0; i<points.length; i++)
 		{
 			int lastI = i-1;
 			if (lastI < 0)
 			{
 				if (opened) continue;
 				lastI = points.length - 1;
 			}
 			Point lp = wnd.databaseToScreen(points[lastI].getX(), points[lastI].getY());
 			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
 			int fX = lp.x + offX;   int fY = lp.y + offY;
 			int tX = p.x + offX;    int tY = p.y + offY;
 			drawLine(g, wnd, fX, fY, tX, tY);
 			if (thickCenter != null)
 			{
 				if (fX < cX) fX--; else fX++;
 				if (fY < cY) fY--; else fY++;
 				if (tX < cX) tX--; else tX++;
 				if (tY < cY) tY--; else tY++;
 				drawLine(g, wnd, fX, fY, tX, tY);
 			}
 		}
 	}
 
     /**
      * Implementing clipping here speeds things up a lot if there are
      * many large highlights off-screen
      */ 
     private static void drawLine(Graphics g, EditWindow wnd, int x1, int y1, int x2, int y2)
     {
         Dimension size = wnd.getScreenSize();
         if (((x1 >= 0) && (x1 <= size.getWidth())) || ((x2 >= 0) && (x2 <= size.getWidth())) ||
             ((y1 >= 0) && (y1 <= size.getHeight())) || ((y2 >= 0) && (y2 <= size.getHeight()))) {
                 g.drawLine(x1, y1, x2, y2);
         }
     }
 
 	/**
 	 * Method to tell whether two Highlights are the same.
 	 * @param other the Highlight to compare to this one.
 	 * @return true if the two refer to the same thing.
 	 */
 	private boolean sameThing(Highlight other)
 	{
 		if (type != other.getType()) return false;
 		if (type == Type.BBOX || type == Type.LINE || type == Type.THICKLINE) return false;
 
 		if (type == Type.EOBJ)
 		{
 			ElectricObject realEObj = eobj;
 			if (realEObj instanceof PortInst) realEObj = ((PortInst)realEObj).getNodeInst();
 			ElectricObject realOtherEObj = other.getElectricObject();
 			if (realOtherEObj instanceof PortInst) realOtherEObj = ((PortInst)realOtherEObj).getNodeInst();
 			if (realEObj != realOtherEObj) return false;
 		} else if (type == Type.TEXT)
 		{
 			if (eobj != other.getElectricObject()) return false;
 			if (cell != other.getCell()) return false;
 			if (var != other.getVar()) return false;
 			if (name != other.getName()) return false;
 		}
 		return true;
 	}
 
 }
