 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: ExportChanges.java
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
 
 import com.sun.electric.database.geometry.DBMath;
 import com.sun.electric.database.geometry.Poly;
 import com.sun.electric.database.hierarchy.Cell;
 import com.sun.electric.database.hierarchy.Export;
 import com.sun.electric.database.network.Netlist;
 import com.sun.electric.database.prototype.ArcProto;
 import com.sun.electric.database.prototype.PortProto;
 import com.sun.electric.database.text.TextUtils;
 import com.sun.electric.database.topology.NodeInst;
 import com.sun.electric.database.topology.PortInst;
 import com.sun.electric.database.topology.Connection;
 import com.sun.electric.database.variable.ElectricObject;
 import com.sun.electric.database.variable.FlagSet;
 import com.sun.electric.technology.Technology;
 import com.sun.electric.technology.technologies.Generic;
 import com.sun.electric.tool.Job;
 import com.sun.electric.tool.user.dialogs.NewExport;
 import com.sun.electric.tool.user.ui.EditWindow;
 import com.sun.electric.tool.user.ui.TopLevel;
 import com.sun.electric.tool.user.ui.WindowFrame;
 import com.sun.electric.tool.user.menus.MenuCommands;
 
 import java.awt.geom.Point2D;
 import java.awt.geom.Rectangle2D;
 import java.util.Iterator;
 import java.util.List;
 import java.util.ArrayList;
 import java.util.Comparator;
 import java.util.Collections;
 import javax.swing.JOptionPane;
 
 /**
  * This class has all of the pulldown menu commands in Electric.
  */
 public final class ExportChanges
 {
 	// ---------------------- THE EXPORT MENU -----------------
 
     /**
 	 * This method implements the command to create a new Export.
 	 */
 	public static void newExportCommand()
 	{
  		NewExport dialog = new NewExport(TopLevel.getCurrentJFrame(), true);
 		dialog.setVisible(true);
 	}
 
 	private static class ExportList
 	{
 		Export pp;
 		int equiv;
 		int busList;
 	};
 
 	public static void describeExports(boolean summarize)
 	{
 		Cell cell = WindowFrame.needCurCell();
 		if (cell == null) return;
 		Netlist netlist = cell.getUserNetlist();
 
 		// compute the associated cell to check
 		Cell wnp = cell.contentsView();
 		if (wnp == null) wnp = cell.iconView();
 		if (wnp == cell) wnp = null;
 
 		// count the number of exports
 		if (cell.getNumPorts() == 0)
 		{
 			System.out.println("There are no exports on cell " + cell.describe());
 			return;
 		}
 
 		// make a list of exports
 		List exports = new ArrayList();
 		for(Iterator it = cell.getPorts(); it.hasNext(); )
 		{
 			ExportList el = new ExportList();
 			el.pp = (Export)it.next();
 			el.equiv = -1;
 			el.busList = -1;
 			exports.add(el);
 		}
 
 		// sort exports by name within type
 		Collections.sort(exports, new ExportSortedByNameAndType());
 
 		// if summarizing, make associations that combine exports
 		int num_found = exports.size();
 		if (summarize)
 		{
 			// make associations among electrically equivalent exports
 			for(int j=0; j<num_found; j++)
 			{
 				int eqJ = ((ExportList)exports.get(j)).equiv;
 				int blJ = ((ExportList)exports.get(j)).busList;
 				if (eqJ != -1 || blJ != -1) continue;
 				Export ppJ = ((ExportList)exports.get(j)).pp;
 				for(int k=j+1; k<num_found; k++)
 				{
 					int eqK = ((ExportList)exports.get(k)).equiv;
 					int blK = ((ExportList)exports.get(k)).busList;
 					if (eqK != -1 || blK != -1) continue;
 					Export ppK = ((ExportList)exports.get(k)).pp;
 					if (ppJ.getCharacteristic() != ppK.getCharacteristic()) break;
 					if (!netlist.sameNetwork(ppJ.getOriginalPort().getNodeInst(), ppJ.getOriginalPort().getPortProto(),
 						ppK.getOriginalPort().getNodeInst(), ppK.getOriginalPort().getPortProto())) continue;
 					((ExportList)exports.get(k)).equiv = j;
 					((ExportList)exports.get(j)).equiv = -2;
 				}
 			}
 
 			// make associations among bussed exports
 			for(int j=0; j<num_found; j++)
 			{
 				int eqJ = ((ExportList)exports.get(j)).equiv;
 				int blJ = ((ExportList)exports.get(j)).busList;
 				if (eqJ != -1 || blJ != -1) continue;
 				Export ppJ = ((ExportList)exports.get(j)).pp;
 				String ptJ = ppJ.getName();
 				int sqPosJ = ptJ.indexOf('[');
 				if (sqPosJ < 0) continue;
 				for(int k=j+1; k<num_found; k++)
 				{
 					int eqK = ((ExportList)exports.get(k)).equiv;
 					int blK = ((ExportList)exports.get(k)).busList;
 					if (eqK != -1 || blK != -1) continue;
 					Export ppK = ((ExportList)exports.get(k)).pp;
 					if (ppJ.getCharacteristic() != ppK.getCharacteristic()) break;
 
 					String ptK = ppK.getName();
 					int sqPosK = ptK.indexOf('[');
 					if (sqPosJ != sqPosK) continue;
 					if (ptJ.substring(0, sqPosJ).equalsIgnoreCase(ptK.substring(0, sqPosK)))
 					{
 						((ExportList)exports.get(k)).busList = j;
 						((ExportList)exports.get(j)).busList = -2;
 					}
 				}
 			}
 		}
 
 		// describe each export
 		System.out.println("----- Exports on cell " + cell.describe() + " -----");
 		FlagSet arcMark = ArcProto.getFlagSet(1);
 		for(int j=0; j<num_found; j++)
 		{
 			ExportList el = (ExportList)exports.get(j);
 			Export pp = el.pp;
 			if (el.equiv >= 0 || el.busList >= 0) continue;
 
 			// reset flags for arcs that can connect
 			for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
 			{
 				Technology tech = (Technology)it.next();
 				for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
 				{
 					ArcProto ap = (ArcProto)aIt.next();
 					ap.clearBit(arcMark);
 				}
 			}
 
 			String infstr = "";
 			String activity = pp.getCharacteristic().getFullName();
 			int m = j+1;
 			for( ; m<num_found; m++)
 			{
 				if (((ExportList)exports.get(m)).equiv == j) break;
 			}
 			double lx = 0, hx = 0, ly = 0, hy = 0;
 			if (m < num_found)
 			{
 				// many exports that are electrically equivalent
 				infstr += activity + " exports ";
 				for(int k=j; k<num_found; k++)
 				{
 					if (j != k && ((ExportList)exports.get(k)).equiv != j) continue;
 					if (j != k) infstr += ", ";
 					Export opp = ((ExportList)exports.get(k)).pp;
 					infstr += "'" + opp.getName() + "'";
 					Poly poly = opp.getOriginalPort().getPoly();
 					double x = poly.getCenterX();
 					double y = poly.getCenterY();
 					if (j == k)
 					{
 						lx = hx = x;   ly = hy = y;
 					} else
 					{
 						if (x < lx) lx = x;
 						if (x > hx) hx = x;
 						if (y < ly) ly = y;
 						if (y > hy) hy = y;
 					}
 					ArcProto [] arcList = opp.getBasePort().getConnections();
 					for(int a=0; a<arcList.length; a++)
 						arcList[a].setBit(arcMark);
 				}
 				infstr += " at (" + lx + "<=X<=" + hx + ", " + ly + "<=Y<=" + hy + "), electrically connected to";
 				infstr = addPossibleArcConnections(infstr, arcMark);
 			} else
 			{
 				m = j + 1;
 				for( ; m<num_found; m++)
 				{
 					if (((ExportList)exports.get(m)).busList == j) break;
 				}
 				if (m < num_found)
 				{
 					// many exports from the same bus
 					int tot = 0;
 					for(int k=j; k<num_found; k++)
 					{
 						if (j != k && ((ExportList)exports.get(k)).busList != j) continue;
 						tot++;
 						Export opp = ((ExportList)exports.get(k)).pp;
 						Poly poly = opp.getOriginalPort().getPoly();
 						double x = poly.getCenterX();
 						double y = poly.getCenterY();
 						if (j == k)
 						{
 							lx = hx = x;   ly = hy = y;
 						} else
 						{
 							if (x < lx) lx = x;
 							if (x > hx) hx = x;
 							if (y < ly) ly = y;
 							if (y > hy) hy = y;
 						}
 						ArcProto [] arcList = opp.getBasePort().getConnections();
 						for(int a=0; a<arcList.length; a++)
 							arcList[a].setBit(arcMark);
 					}
 
 					List sortedBusList = new ArrayList();
 					sortedBusList.add(((ExportList)exports.get(j)).pp);
 					for(int k=j+1; k<num_found; k++)
 					{
 						ExportList elK = (ExportList)exports.get(k);
 						if (elK.busList == j) sortedBusList.add(elK.pp);
 					}
 
 					// sort the bus by indices
 					Collections.sort(sortedBusList, new ExportSortedByBusIndex());
 
 					boolean first = true;
 					for(Iterator it = sortedBusList.iterator(); it.hasNext(); )
 					{
 						Export ppS = (Export)it.next();
 						String pt1 = ppS.getName();
 						int openPos = pt1.indexOf('[');
 						if (first)
 						{
 							infstr += activity + " ports '" + pt1.substring(0, openPos) + "[";
 							first = false;
 						} else
 						{
 							infstr += ",";
 						}
 						int closePos = pt1.lastIndexOf(']');
 						infstr += pt1.substring(openPos+1, closePos);
 					}
 					infstr += "]' at (" + lx + "<=X<=" + hx + ", " + ly + "<=Y<=" + hy + "), same bus, connects to";
 					infstr = addPossibleArcConnections(infstr, arcMark);
 				} else
 				{
 					// isolated export
 					Poly poly = pp.getOriginalPort().getPoly();
 					double x = poly.getCenterX();
 					double y = poly.getCenterY();
 					infstr += activity + " export '" + pp.getName() + "' at (" + x + ", " + y + ") connects to";
 					ArcProto [] arcList = pp.getBasePort().getConnections();
 					for(int a=0; a<arcList.length; a++)
 						arcList[a].setBit(arcMark);
 					infstr = addPossibleArcConnections(infstr, arcMark);
 
 					// check for the export in the associated cell
 					if (wnp != null)
 					{
 						if (pp.getEquivalentPort(wnp) == null)
 							infstr += " *** no equivalent in " + wnp.describe();
 					}
 				}
 			}
 
 			TextUtils.printLongString(infstr);
 		}
 		if (wnp != null)
 		{
 			for(Iterator it = wnp.getPorts(); it.hasNext(); )
 			{
 				Export pp = (Export)it.next();
 				if (pp.getEquivalentPort(cell) == null)
 					System.out.println("*** Export " + pp.getName() + ", found in cell " + wnp.describe() + ", is missing here");
 			}
 		}
 		arcMark.freeFlagSet();
 	}
 
 	/*
 	 * Helper routine to add all marked arc prototypes to the infinite string.
 	 * Marking is done by having the "temp1" field be nonzero.
 	 */
 	private static String addPossibleArcConnections(String infstr, FlagSet arcMark)
 	{
 		int i = 0;
 		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
 		{
 			Technology tech = (Technology)it.next();
 			for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
 			{
 				ArcProto ap = (ArcProto)aIt.next();
 				if (!ap.isBit(arcMark)) i++;
 			}
 		}
 		if (i == 0) infstr += " EVERYTHING"; else
 		{
 			i = 0;
 			for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
 			{
 				Technology tech = (Technology)it.next();
 				if (tech == Generic.tech) continue;
 				for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
 				{
 					ArcProto ap = (ArcProto)aIt.next();
 					if (!ap.isBit(arcMark)) continue;
 					if (i != 0) infstr += ",";
 					i++;
 					infstr += " " + ap.getName();
 				}
 			}
 		}
 		return infstr;
 	}
 
 	private static class ExportSortedByNameAndType implements Comparator
 	{
 		public int compare(Object o1, Object o2)
 		{
 			ExportList el1 = (ExportList)o1;
 			ExportList el2 = (ExportList)o2;
 			Export e1 = el1.pp;
 			Export e2 = el2.pp;
 			PortProto.Characteristic ch1 = e1.getCharacteristic();
 			PortProto.Characteristic ch2 = e2.getCharacteristic();
 			if (ch1 != ch2) return ch1.getOrder() - ch2.getOrder();
 			String s1 = e1.getName();
 			String s2 = e2.getName();
 			return TextUtils.nameSameNumeric(s1, s2);
 		}
 	}
 
 	private static class ExportSortedByBusIndex implements Comparator
 	{
 		public int compare(Object o1, Object o2)
 		{
 			Export e1 = (Export)o1;
 			Export e2 = (Export)o2;
 			String s1 = e1.getName();
 			String s2 = e2.getName();
 			return TextUtils.nameSameNumeric(s1, s2);
 		}
 	}
 
    private static class PortInstsSortedByBusIndex implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            PortInst p1 = (PortInst)o1;
            PortInst p2 = (PortInst)o2;
            String s1 = p1.getPortProto().getName();
            String s2 = p2.getPortProto().getName();
            return TextUtils.nameSameNumeric(s1, s2);
        }
    }

 	/****************************** EXPORT CHANGES ******************************/
 
 	/**
 	 * Method to re-export all unwired/unexported ports on cell instances in the current Cell.
 	 */
 	public static void reExportAll()
 	{
 		// make sure there is a current cell
 		Cell cell = WindowFrame.needCurCell();
 		if (cell == null) return;
 
 		// disallow port action if lock is on
 		if (CircuitChanges.cantEdit(cell, null, true)) return;
 
 		ReExport job = new ReExport(cell, null, false, false);
 	}
 
 	/**
 	 * Method to re-export all unwired/unexported ports on cell instances in the current Cell.
 	 * Only works in the currently highlighted area.
 	 */
 	public static void reExportHighlighted(boolean includeWiredPorts)
 	{
 		// make sure there is a current cell
 		Cell cell = WindowFrame.needCurCell();
 		if (cell == null) return;
 
 		// disallow port action if lock is on
 		if (CircuitChanges.cantEdit(cell, null, true)) return;
 
         EditWindow wnd = EditWindow.getCurrent();
 		Rectangle2D bounds = wnd.getHighlighter().getHighlightedArea(null);
 		if (bounds == null)
 		{
 			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
 				"Must select area before re-exporting the highlighted objects",
 					"Re-export failed", JOptionPane.ERROR_MESSAGE);
 			return;
 		}
 		ReExport job = new ReExport(cell, bounds, false, includeWiredPorts);
 	}
 
     public static void reExportSelected(boolean includeWiredPorts)
     {
         // make sure there is a current cell
         Cell cell = WindowFrame.needCurCell();
         if (cell == null) return;
 
         List portsToExport = new ArrayList();
         List highs = MenuCommands.getSelectedObjects(true, false);
         for(Iterator it = highs.iterator(); it.hasNext(); )
         {
             NodeInst ni = (NodeInst)it.next();
             for(Iterator eIt = ni.getPortInsts(); eIt.hasNext(); )
             {
                 portsToExport.add(eIt.next());
             }
             // remove exported ports
             for (Iterator exit = ni.getExports(); exit.hasNext(); ) {
                 Export e = (Export)exit.next();
                 portsToExport.remove(e.getOriginalPort());
             }
         }
 
         if (portsToExport.size() > 0) {
             ExportPorts job = new ExportPorts(cell, portsToExport, includeWiredPorts);
         } else {
             System.out.println("No ports on selected objects to export");
         }
     }
 
     private static class ExportPorts extends Job
     {
         Cell cell;
         List portsToExport;
         boolean includeWiredPorts;
 
         protected ExportPorts(Cell cell, List portsToExport, boolean includeWiredPorts) {
             super("Export ports on Selected", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
             this.cell = cell;
             this.portsToExport = portsToExport;
             this.includeWiredPorts = includeWiredPorts;
             startJob();
         }
 
         public boolean doIt() {
             // disallow port action if lock is on
             if (CircuitChanges.cantEdit(cell, null, true)) return false;
 
            // sort ports by name
            Collections.sort(portsToExport, new PortInstsSortedByBusIndex());
            for (Iterator it = portsToExport.iterator(); it.hasNext(); ) {
                PortInst pi = (PortInst)it.next();
                System.out.println(pi.getPortProto().getName());
            }

             int exported = 0;
             for (Iterator it = portsToExport.iterator(); it.hasNext(); ) {
                 PortInst pi = (PortInst)it.next();
                 if (!includeWiredPorts) {
                     Iterator connIt = pi.getConnections();
                     if (connIt.hasNext()) continue;
                 }
                 Export ex = Export.newInstance(cell, pi, pi.getPortProto().getName());
                 if (ex == null) {
                     System.out.println("ERROR trying to export "+pi.describe());
                 } else {
                     exported++;
                 }
             }
             System.out.println(exported+" exports created");
             return true;
         }
     }
 
 	/**
 	 * Method to re-export all unwired/unexported ports on cell instances in the current Cell.
 	 * Only works for power and ground ports.
 	 */
 	public static void reExportPowerAndGround()
 	{
 		// make sure there is a current cell
 		Cell cell = WindowFrame.needCurCell();
 		if (cell == null) return;
 
 		// disallow port action if lock is on
 		if (CircuitChanges.cantEdit(cell, null, true)) return;
 
 		ReExport job = new ReExport(cell, null, true, false);
 	}
 
 	private static class ReExport extends Job
 	{
 		Cell cell;
 		Rectangle2D bounds;
 		boolean pAndG;
         boolean includeWiredPorts;
 
 		protected ReExport(Cell cell, Rectangle2D bounds, boolean pAndG, boolean includeWiredPorts)
 		{
 			super("Re-export ports", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
 			this.cell = cell;
 			this.bounds = bounds;
 			this.pAndG = pAndG;
             this.includeWiredPorts = includeWiredPorts;
 			startJob();
 		}
 
 		public boolean doIt()
 		{
 			FlagSet portMarked = PortProto.getFlagSet(1);
 
 			// look at every node in this cell
 			int total = 0;
 			for(Iterator it = cell.getNodes(); it.hasNext(); )
 			{
 				NodeInst ni = (NodeInst)it.next();
 
 				// only look for cells, not primitives
 				if (!(ni.getProto() instanceof Cell)) continue;
 
 				// ignore recursive references (showing icon in contents)
 				if (ni.isIconOfParent()) continue;
 
 				// clear marks on the ports of this node
 				for(Iterator pIt = ni.getProto().getPorts(); pIt.hasNext(); )
 				{
 					PortProto pp = (PortProto)pIt.next();
 					pp.clearBit(portMarked);
 				}
 
 				// mark the connected and exports
                 if (!includeWiredPorts) {
                     for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
                     {
                         Connection con = (Connection)pIt.next();
                         con.getPortInst().getPortProto().setBit(portMarked);
                     }
                 }
 				for(Iterator pIt = ni.getExports(); pIt.hasNext(); )
 				{
 					Export e = (Export)pIt.next();
 					e.getOriginalPort().getPortProto().setBit(portMarked);
 				}
 
 				// initialize for queueing creation of new exports
 				List queuedExports = new ArrayList();
 
 				// now export the remaining ports
 				for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
 				{
 					PortInst pi = (PortInst)pIt.next();
 					if (pi.getPortProto().isBit(portMarked)) continue;
 
 					// if Power and Ground is requested, make sure this is so
 					if (pAndG)
 					{
 						PortProto pp = pi.getPortProto();
 						if (!pp.isPower() && !pp.isGround()) continue;
 					}
 
 					// if a highlighted area is specified, make sure this is in it
 					if (bounds != null)
 					{
 						Poly portPoly = pi.getPoly();
 						if (!bounds.contains(portPoly.getCenterX(), portPoly.getCenterY())) continue;
 					}
 					queuedExports.add(pi);
 				}
 				Collections.sort(queuedExports, new PortInstSorted());
 
 				// now create the exports
 				for(Iterator pIt = queuedExports.iterator(); pIt.hasNext(); )
 				{
 					PortInst pi = (PortInst)pIt.next();
 					String portName = ElectricObject.uniqueObjectName(pi.getPortProto().getName(), cell, PortProto.class);
 					Export newPp = Export.newInstance(cell, pi, portName);
 					if (newPp != null)
 					{
 						newPp.setTextDescriptor(pi.getPortProto().getTextDescriptor());
 						newPp.copyVars(pi.getPortProto());
 						total++;
 					}
 				}
 			}
 			if (total == 0) System.out.println("No ports to export"); else
 				System.out.println(total + " ports exported");
 			portMarked.freeFlagSet();
 			return true;
 		}
 
 		static class PortInstSorted implements Comparator
 		{
 			public int compare(Object o1, Object o2)
 			{
 				PortInst pi1 = (PortInst)o1;
 				PortInst pi2 = (PortInst)o2;
 				String s1 = pi1.getPortProto().getName();
 				String s2 = pi2.getPortProto().getName();
 				return s1.compareToIgnoreCase(s2);
 			}
 		}
 	}
 
 	/**
 	 * Method to delete the currently selected exports.
 	 */
 	public static void deleteExport()
 	{
 		// make sure there is a current cell
 		Cell cell = WindowFrame.needCurCell();
 		if (cell == null) return;
 
 		// disallow port action if lock is on
 		if (CircuitChanges.cantEdit(cell, null, true)) return;
 
 		List exportsToDelete = new ArrayList();
         EditWindow wnd = EditWindow.getCurrent();
 		List highs = wnd.getHighlighter().getHighlightedText(true);
 		for(Iterator it = highs.iterator(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 			if (h.getVar() != null) continue;
 			if (h.getName() != null) continue;
 			if (h.getElectricObject() instanceof Export)
 			{
 				Export pp = (Export)h.getElectricObject();
 				exportsToDelete.add(pp);
 			}
 		}
 		if (exportsToDelete.size() == 0)
 		{
 			System.out.println("There are no selected exports to delete");
 			return;
 		}
 		DeleteExports job = new DeleteExports(cell, exportsToDelete);
 	}
 
 	/**
 	 * Method to delete all exports on the highlighted objects.
 	 */
 	public static void deleteExportsOnSelected()
 	{
 		// make sure there is a current cell
 		Cell cell = WindowFrame.needCurCell();
 		if (cell == null) return;
 
 		// disallow port action if lock is on
 		if (CircuitChanges.cantEdit(cell, null, true)) return;
 
 		List exportsToDelete = new ArrayList();
         EditWindow wnd = EditWindow.getCurrent();
 		List highs = wnd.getHighlighter().getHighlightedEObjs(true, false);
 		for(Iterator it = highs.iterator(); it.hasNext(); )
 		{
 			NodeInst ni = (NodeInst)it.next();
 			for(Iterator eIt = ni.getExports(); eIt.hasNext(); )
 			{
 				exportsToDelete.add(eIt.next());
 			}
 		}
 		if (exportsToDelete.size() == 0)
 		{
 			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
 				"There are no exports on the highlighted objects",
 					"Re-export failed", JOptionPane.ERROR_MESSAGE);
 			return;
 		}
 		DeleteExports job = new DeleteExports(cell, exportsToDelete);
 	}
 
 	/**
 	 * Method to delete all exports in the highlighted area.
 	 */
 	public static void deleteExportsInArea()
 	{
 		// make sure there is a current cell
 		Cell cell = WindowFrame.needCurCell();
 		if (cell == null) return;
 
 		// disallow port action if lock is on
 		if (CircuitChanges.cantEdit(cell, null, true)) return;
 
 		List exportsToDelete = new ArrayList();
         EditWindow wnd = EditWindow.getCurrent();
 		Rectangle2D bounds = wnd.getHighlighter().getHighlightedArea(null);
 		if (bounds == null)
 		{
 			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
 				"Must select something before deleting the highlighted exports",
 					"Export delete failed", JOptionPane.ERROR_MESSAGE);
 			return;
 		}
 		for(Iterator it = cell.getNodes(); it.hasNext(); )
 		{
 			NodeInst ni = (NodeInst)it.next();
 			for(Iterator eIt = ni.getExports(); eIt.hasNext(); )
 			{
 				Export e = (Export)eIt.next();
 				PortInst pi = e.getOriginalPort();
 				Poly poly = pi.getPoly();
 				if (bounds.contains(poly.getCenterX(), poly.getCenterY()))
 					exportsToDelete.add(e);
 			}
 		}
 		if (exportsToDelete.size() == 0)
 		{
 			JOptionPane.showMessageDialog(TopLevel.getCurrentJFrame(),
 				"There are no exports in the highlighted area",
 					"Re-export failed", JOptionPane.ERROR_MESSAGE);
 			return;
 		}
 		DeleteExports job = new DeleteExports(cell, exportsToDelete);
 	}
 
 	/**
 	 * Method to move the currently selected export from one node to another.
 	 */
 	public static void moveExport()
 	{
 		Export source = null;
 		PortInst dest = null;
         EditWindow wnd = EditWindow.getCurrent();
 		for(Iterator it = wnd.getHighlighter().getHighlights().iterator(); it.hasNext(); )
 		{
 			Highlight h = (Highlight)it.next();
 			boolean used = false;
 			if (h.getType() == Highlight.Type.EOBJ)
 			{
 				if (h.getElectricObject() instanceof PortInst)
 				{
 					if (dest != null)
 					{
 						System.out.println("Must select only one node-port as a destination of the move");
 						return;
 					}
 					dest = (PortInst)h.getElectricObject();
 					used = true;
 				}
 			} else if (h.getType() == Highlight.Type.TEXT)
 			{
 				if (h.getVar() == null && h.getName() == null && h.getElectricObject() instanceof Export)
 				{
 					source = (Export)h.getElectricObject();
 					used = true;
 				}
 			}
 			if (!used)
 			{
 				System.out.println("Moving exports: select one export to move, and one node-port as its destination");
 				return;
 			
 			}
 		}
 		if (source == null || dest == null)
 		{
 			System.out.println("First select one export to move, and one node-port as its destination");
 			return;
 		}
 		MoveExport job = new MoveExport(source, dest);
 	}
 
 	/**
 	 * Method to rename the currently selected export.
 	 */
 	public static void renameExport()
 	{
         EditWindow wnd = EditWindow.getCurrent();
 		Highlight h = wnd.getHighlighter().getOneHighlight();
 		if (h.getVar() != null || h.getName() != null || !(h.getElectricObject() instanceof Export))
 		{
 			System.out.println("Must select an export name before renaming it");
 			return;
 		}
 		Export pp = (Export)h.getElectricObject();
 		String response = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Rename export", pp.getName());
 		if (response == null) return;
 		RenameExport job = new RenameExport(pp, response);
 	}
 
 	private static class DeleteExports extends Job
 	{
         Cell cell;
 		List exportsToDelete;
 
 		protected DeleteExports(Cell cell, List exportsToDelete)
 		{
 			super("Delete exports", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
             this.cell = cell;
 			this.exportsToDelete = exportsToDelete;
 			startJob();
 		}
 
 		public boolean doIt()
 		{
 			int total = 0;
 			for(Iterator it = exportsToDelete.iterator(); it.hasNext(); )
 			{
 				Export e = (Export)it.next();
 				e.kill();
 				total++;
 			}
 			if (total == 0) System.out.println("No exports deleted"); else
 				System.out.println(total + " exports deleted");
 
 			return true;
 		}
 	}
 
 	private static class MoveExport extends Job
 	{
 		Export source;
 		PortInst dest;
 
 		protected MoveExport(Export source, PortInst dest)
 		{
 			super("Move export", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
 			this.source = source;
 			this.dest = dest;
 			startJob();
 		}
 
 		public boolean doIt()
 		{
 			source.move(dest);
 			return true;
 		}
 	}
 
 	/**
 	 * Class to rename a cell in a new thread.
 	 */
 	private static class RenameExport extends Job
 	{
 		Export pp;
 		String newName;
 
 		protected RenameExport(Export pp, String newName)
 		{
 			super("Rename Export" + pp.getName(), User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
 			this.pp = pp;
 			this.newName = newName;
 			startJob();
 		}
 
 		public boolean doIt()
 		{
 			pp.rename(newName);
 			return true;
 		}
 	}
 
 	private static class ShownPorts
 	{
 		Point2D   loc;
 		PortProto pp;
 		int       angle;
 	};
 
 	/**
 	 * Method to show all exports in the current cell.
 	 */
 	public static void showExports()
 	{
 		showPortsAndExports(null);
 	}
 
 	/**
 	 * Method to show all ports on the selected nodes in the current cell.
 	 */
 	public static void showPorts()
 	{
         EditWindow wnd = EditWindow.getCurrent();
 		List nodes = wnd.getHighlighter().getHighlightedEObjs(true, false);
 		if (nodes == null || nodes.size() == 0)
 		{
 			System.out.println("No nodes are highlighted");
 			return;
 		}
 		showPortsAndExports(nodes);
 	}
 
 	private static void showPortsAndExports(List nodes)
 	{
 		EditWindow wnd = EditWindow.needCurrent();
 		if (wnd == null) return;
 		Cell cell = wnd.getCell();
 		if (cell == null)
 		{
 			System.out.println("No cell in this window");
 			return;
 		}
 		int total = cell.getNumPorts();
 		if (nodes != null)
 		{
 			total = 0;
 			for(Iterator it = nodes.iterator(); it.hasNext(); )
 			{
 				NodeInst ni = (NodeInst)it.next();
 				total += ni.getNumPortInsts();
 			}
 		}
 		Rectangle2D displayable = wnd.displayableBounds();
 		double digitIndentX = displayable.getWidth() / 15;
 		double digitIndentY = displayable.getHeight() / 15;
 
 		// allocate space for the port information
 		Point2D [] labelLocs = new Point2D.Double[total];
 		ShownPorts [] portList = new ShownPorts[total];
 		int numPerSide = (total + 3) / 4;
 		int leftSideCount, topSideCount, rightSideCount, botSideCount;
 		leftSideCount = topSideCount = rightSideCount = botSideCount = numPerSide;
 		if (leftSideCount + topSideCount + rightSideCount + botSideCount > total)
 			botSideCount--;
 		if (leftSideCount + topSideCount + rightSideCount + botSideCount > total)
 			topSideCount--;
 		if (leftSideCount + topSideCount + rightSideCount + botSideCount > total)
 			rightSideCount--;
 		int fill = 0;
 		for(int i=0; i<leftSideCount; i++)
 		{
 			labelLocs[fill++] = new Point2D.Double(displayable.getMinX() + digitIndentX,
 				displayable.getHeight() / (leftSideCount+1) * (i+1) + displayable.getMinY());
 		}
 		for(int i=0; i<topSideCount; i++)
 		{
 			labelLocs[fill++] = new Point2D.Double(displayable.getWidth() / (topSideCount+1) * (i+1) + displayable.getMinX(),
 				displayable.getMaxY() - digitIndentY);
 		}
 		for(int i=0; i<rightSideCount; i++)
 		{
 			labelLocs[fill++] = new Point2D.Double(displayable.getMaxX() - digitIndentX,
 				displayable.getMaxY() - displayable.getHeight() / (rightSideCount+1) * (i+1));
 		}
 		for(int i=0; i<botSideCount; i++)
 		{
 			labelLocs[fill++] = new Point2D.Double(displayable.getMaxX() - displayable.getWidth() / (botSideCount+1) * (i+1),
 				displayable.getMinY() + digitIndentY);
 		}
 //		for(int i=0; i<total; i++)
 //		{
 //			if ((w->state&INPLACEEDIT) != 0)
 //				xform(labelLocs[i], &labelLocs[i], w->intocell);
 //		}
 
 		// associate ports with display locations
 		total = 0;
 		int ignored = 0;
 		if (nodes == null)
 		{
 			// handle exports on the cell
 			for(Iterator it = cell.getPorts(); it.hasNext(); )
 			{
 				Export pp = (Export)it.next();
 				Poly poly = pp.getOriginalPort().getPoly();
 
 				Point2D ptOut = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
 //				if ((w->state&INPLACEEDIT) != 0)
 //					xform(xout, yout, &xout, &yout, w->outofcell);
 				if (ptOut.getX() < displayable.getMinX() || ptOut.getX() > displayable.getMaxX() ||
 					ptOut.getY() < displayable.getMinY() || ptOut.getY() > displayable.getMaxY())
 				{
 					ignored++;
 					continue;
 				}
 
 				portList[total] = new ShownPorts();
 				portList[total].loc = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
 				portList[total].pp = pp;
 				total++;
 			}
 		} else
 		{
 			// handle ports on the selected nodes
 			for(Iterator it = nodes.iterator(); it.hasNext(); )
 			{
 				NodeInst ni = (NodeInst)it.next();
 				for(Iterator pIt = ni.getPortInsts(); pIt.hasNext(); )
 				{
 					PortInst pi = (PortInst)pIt.next();
 					Poly poly = pi.getPoly();
 
 					Point2D ptOut = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
 //					if ((w->state&INPLACEEDIT) != 0)
 //						xform(xout, yout, &xout, &yout, w->outofcell);
 					if (ptOut.getX() < displayable.getMinX() || ptOut.getX() > displayable.getMaxX() ||
 						ptOut.getY() < displayable.getMinY() || ptOut.getY() > displayable.getMaxY())
 					{
 						ignored++;
 						continue;
 					}
 
 					portList[total] = new ShownPorts();
 					portList[total].loc = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
 					portList[total].pp = pi.getPortProto();
 					total++;
 				}
 			}
 		}
 
 		// build a sorted list of ports around the center
 		double x = 0, y = 0;
 		for(int i=0; i<total; i++)
 		{
 			x += portList[i].loc.getX();
 			y += portList[i].loc.getY();
 		}
 		Point2D center = new Point2D.Double(x / total, y / total);
 		for(int i=0; i<total; i++)
 		{
 			if (center.getX() == portList[i].loc.getX() && center.getY() == portList[i].loc.getY())
 				portList[i].angle = 0; else
 					portList[i].angle = -DBMath.figureAngle(center, portList[i].loc);
 		}
 
 		List portLabels = new ArrayList();
 		for(int i=0; i<total; i++)
 			portLabels.add(portList[i]);
 		Collections.sort(portLabels, new SortPortAngle());
 		total = 0;
 		for(Iterator it = portLabels.iterator(); it.hasNext(); )
 			portList[total++] = (ShownPorts)it.next();
 
 		// figure out the best rotation offset
 		double bestDist = 0;
 		int bestOff = 0;
 		for(int i=0; i<total; i++)
 		{
 			double dist = 0;
 			for(int j=0; j<total; j++)
 				dist += labelLocs[j].distance(portList[(j+i)%total].loc);
 			if (dist < bestDist || i == 0)
 			{
 				bestOff = i;
 				bestDist = dist;
 			}
 		}
 
 		// show the ports
         Highlighter highlighter = wnd.getHighlighter();
 		highlighter.clear();
 		if (nodes != null)
 		{
 			for(Iterator it = nodes.iterator(); it.hasNext(); )
 			{
 				NodeInst ni = (NodeInst)it.next();
 				highlighter.addElectricObject(ni, cell);
 			}
 		}
 		for(int i=0; i<total; i++)
 		{
 			int index = (bestOff + i) % total;
 			highlighter.addMessage(cell, portList[index].pp.getName(), labelLocs[i]);
 			highlighter.addLine(labelLocs[i], portList[index].loc, cell);
 		}
 		highlighter.finished();
 		if (total == 0)
 			System.out.println("No exported ports to show");
 		if (ignored > 0)
 			System.out.println("Could not display " + ignored + " ports (outside of the window)");
 	}
 
 	private static class SortPortAngle implements Comparator
 	{
 		public int compare(Object o1, Object o2)
 		{
 			ShownPorts s1 = (ShownPorts)o1;
 			ShownPorts s2 = (ShownPorts)o2;
 			return s1.angle - s2.angle;
 		}
 	}
 }
