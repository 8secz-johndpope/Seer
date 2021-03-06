 /* -*- tab-width: 4 -*-
  *
  * Electric(tm) VLSI Design System
  *
  * File: ErrorLogger.java
  *
  * Copyright (c) 2004 Sun Microsystems and Free Software
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
 import com.sun.electric.database.prototype.PortProto;
 import com.sun.electric.database.topology.NodeInst;
 import com.sun.electric.database.hierarchy.Cell;
 import com.sun.electric.database.hierarchy.Export;
 import com.sun.electric.tool.user.ui.WindowFrame;
 import com.sun.electric.tool.user.ui.WindowContent;
 
 import javax.swing.tree.DefaultMutableTreeNode;
 import javax.swing.*;
 import java.util.*;
 import java.awt.geom.Point2D;
 import java.awt.event.ActionListener;
 import java.awt.event.ActionEvent;
 
 /**
  * Holds a log of errors:
  * <p>ErrorLogger errorLogger = ErrorLogger.newInstance(String s): get new logger for s
  * <p>ErrorLog errorLog = errorLogger.logError(string msg, cell c, int k):
  * Create a new log with message 'msg', for cell 'c', with sortKey 'k'.
  * <p>Various methods for adding highlights to errorLog:
  * <pre>
  *   addGeom(Geometric g, boolean s, int l, NodeInst [] p)
  *                                          add geom "g" to error (show if "s" nonzero)
  *   addExport(Export p, boolean s)         add export "pp" to error
  *   addLine(x1, y1, x2, y2)                add line to error
  *   addPoly(POLYGON *p)                    add polygon to error
  *   addPoint(x, y)                         add point to error
  * </pre>
  * <p>To end logging, call errorLogger.termLogging(boolean explain).
  */
 public class ErrorLogger implements ActionListener {
 
     private static final int ERRORTYPEGEOM      = 1;
     private static final int ERRORTYPEEXPORT    = 2;
     private static final int ERRORTYPELINE      = 3;
     private static final int ERRORTYPETHICKLINE = 4;
     private static final int ERRORTYPEPOINT     = 5;
 
     private static class ErrorHighlight
     {
         int         type;
         Geometric   geom;
         PortProto   pp;
         boolean     showgeom;
         double      x1, y1;
         double      x2, y2;
         double      cX, cY;
         int         pathlen;
         NodeInst [] path;
 
         /**
          * Method to describe an object on an error.
          */
         public String describe()
         {
             String msg;
             if (geom instanceof NodeInst) msg = "Node " + geom.describe(); else
                 msg = "Arc " + geom.describe();
             for(int i=0; i<pathlen; i++)
             {
                 NodeInst ni = path[i];
                 msg += " in " + ni.getParent().getLibrary().getName() +
                     ":" + ni.getParent().noLibDescribe() + ":" + ni.describe();
             }
             return msg;
         }
     };
 
     /**
      * Create a Log of a single Error
      */
     public static class ErrorLog {
         private String message;
         private Cell   cell;
         private int    sortKey;
         private int    index;
         private List   highlights;
 
         private ErrorLog(String message, Cell cell, int sortKey) {
             this.message = message;
             this.cell = cell;
             this.sortKey = sortKey;
             index = 0;
             highlights = new ArrayList();
         }
 
         /**
          * Method to add "geom" to the error in "errorlist".  Also adds a
          * hierarchical traversal path "path" (which is "pathlen" long).
          */
         public void addGeom(Geometric geom, boolean showit, int pathlen, NodeInst [] path)
         {
             ErrorHighlight eh = new ErrorHighlight();
             eh.type = ERRORTYPEGEOM;
             eh.geom = geom;
             eh.showgeom = showit;
             eh.pathlen = pathlen;
             if (pathlen > 0) eh.path = path;
             highlights.add(eh);
         }
 
         /**
          * Method to add "pp" to the error in "errorlist".
          */
         public void addExport(Export pp, boolean showit)
         {
             ErrorHighlight eh = new ErrorHighlight();
             eh.type = ERRORTYPEEXPORT;
             eh.pp = pp;
             eh.showgeom = showit;
             eh.pathlen = 0;
             highlights.add(eh);
         }
 
         /**
          * Method to add line (x1,y1)=>(x2,y2) to the error in "errorlist".
          */
         public void addLine(double x1, double y1, double x2, double y2)
         {
             ErrorHighlight eh = new ErrorHighlight();
             eh.type = ERRORTYPELINE;
             eh.x1 = x1;
             eh.y1 = y1;
             eh.x2 = x2;
             eh.y2 = y2;
             eh.pathlen = 0;
             highlights.add(eh);
         }
 
         /**
          * Method to add polygon "poly" to the error in "errorlist".
          */
         public void addPoly(Poly poly, boolean thick)
         {
             Point2D [] points = poly.getPoints();
             Point2D center = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
             for(int i=0; i<points.length; i++)
             {
                 int prev = i-1;
                 if (i == 0) prev = points.length-1;
                 ErrorHighlight eh = new ErrorHighlight();
                 if (thick) eh.type = ERRORTYPETHICKLINE; else
                     eh.type = ERRORTYPELINE;
                 eh.x1 = points[prev].getX();
                 eh.y1 = points[prev].getY();
                 eh.x2 = points[i].getX();
                 eh.y2 = points[i].getY();
                 eh.cX = center.getX();
                 eh.cY = center.getY();
                 eh.pathlen = 0;
                 highlights.add(eh);
             }
         }
 
         /**
          * Method to add point (x,y) to the error in "errorlist".
          */
         public void addPoint(double x, double y)
         {
             ErrorHighlight eh = new ErrorHighlight();
             eh.type = ERRORTYPEPOINT;
             eh.x1 = x;
             eh.y1 = y;
             eh.pathlen = 0;
             highlights.add(eh);
         }
 
         /**
          * Method to return the number of objects associated with error "e".  Only
          * returns "geom" objects
          */
         public int getNumGeoms()
         {
             int total = 0;
             for(Iterator it = highlights.iterator(); it.hasNext(); )
             {
                 ErrorHighlight eh = (ErrorHighlight)it.next();
                 if (eh.type == ERRORTYPEGEOM) total++;
             }
             return total;
         }
 
         /**
          * Method to return a specific object in a list of objects on this ErrorLog.
          * Returns null at the end of the list.
          */
         public ErrorHighlight getErrorGeom(int index)
         {
             int total = 0;
             for(Iterator it = highlights.iterator(); it.hasNext(); )
             {
                 ErrorHighlight eh = (ErrorHighlight)it.next();
                 if (eh.type != ERRORTYPEGEOM) continue;
                 if (total == index) return eh;
                 total++;
             }
             return null;
         }
 
         /**
          * Method to describe error "elv".
          */
         public String describeError() { return message; }
 
         /**
          * Method to return the error message associated with the current error.
          * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
          * with associated geometry modules (if nonzero).
          */
         public String reportError(boolean showhigh, Geometric [] gPair)
         {
             // if two highlights are requested, find them
             if (gPair != null)
             {
                 Geometric geom1 = null, geom2 = null;
                 for(Iterator it = highlights.iterator(); it.hasNext(); )
                 {
                     ErrorHighlight eh = (ErrorHighlight)it.next();
                     if (eh.type == ERRORTYPEGEOM)
                     {
                         if (geom1 == null) geom1 = eh.geom; else
                             if (geom2 == null) geom2 = eh.geom;
                     }
                 }
 
                 // return geometry if requested
                 if (geom1 != null) gPair[0] = geom1;
                 if (geom2 != null) gPair[1] = geom2;
             }
 
             // show the error
             if (showhigh)
             {
                 Highlight.clear();
 
                 // validate the cell (it may have been deleted)
                 if (cell != null)
                 {
                     if (!cell.isLinked())
                     {
                         return "(cell deleted): " + message;
                     }
 
                     // make sure it is shown
                     boolean found = false;
                     for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
                     {
                         WindowFrame wf = (WindowFrame)it.next();
                         WindowContent content = wf.getContent();
                         if (content.getCell() == cell)
                         {
                             // already displayed.  should force window "wf" to front? yes
                             wf.getFrame().toFront();
                             found = true;
                             break;
                         }
                     }
                     if (!found)
                     {
                         // make a new window for the cell
                         WindowFrame wf = WindowFrame.createEditWindow(cell);
                     }
                 }
 
                 // first show the geometry associated with this error
                 for(Iterator it = highlights.iterator(); it.hasNext(); )
                 {
                     ErrorHighlight eh = (ErrorHighlight)it.next();
                     switch (eh.type)
                     {
                         case ERRORTYPEGEOM:
                             if (!eh.showgeom) break;
                             Highlight.addElectricObject(eh.geom, cell);
                             break;
                         case ERRORTYPEEXPORT:
     						Highlight.addText(eh.pp, cell, null, null);
 //						if (havegeoms == 0) infstr = initinfstr(); else
 //							addtoinfstr(infstr, '\n');
 //						havegeoms++;
 //						formatinfstr(infstr, x_("CELL=%s TEXT=0%lo;0%lo;-"),
 //							describenodeproto(eh->pp->parent), (INTBIG)eh->pp->subnodeinst->geom,
 //								(INTBIG)eh->pp);
                             break;
                         case ERRORTYPELINE:
                             Highlight.addLine(new Point2D.Double(eh.x1, eh.y1), new Point2D.Double(eh.x2, eh.y2), cell);
                             break;
                         case ERRORTYPETHICKLINE:
                             Highlight.addThickLine(new Point2D.Double(eh.x1, eh.y1), new Point2D.Double(eh.x2, eh.y2), new Point2D.Double(eh.cX, eh.cY), cell);
                             break;
                         case ERRORTYPEPOINT:
                             double consize = 5;
                             Highlight.addLine(new Point2D.Double(eh.x1-consize, eh.y1-consize), new Point2D.Double(eh.x1+consize, eh.y1+consize), cell);
                             Highlight.addLine(new Point2D.Double(eh.x1-consize, eh.y1+consize), new Point2D.Double(eh.x1+consize, eh.y1-consize), cell);
                             break;
                     }
 
 //				// set the hierarchical path
 //				if (eh.type == ERRORTYPEGEOM && eh.showgeom && eh.pathlen > 0)
 //				{
 //					cell = geomparent(eh.geom);
 //					for(w=el_topwindowpart; w != NOWINDOWPART; w = w->nextwindowpart)
 //						if (w->curnodeproto == cell) break;
 //					if (w != NOWINDOWPART)
 //					{
 //						for(int j=eh.pathlen-1; j>=0; j--)
 //						{
 //							sethierarchicalparent(cell, eh.path[j], w, 0, 0);
 //							cell = eh.path[j].getParent();
 //						}
 //					}
 //				}
                 }
 
                 Highlight.finished();
             }
 
             // return the error message
             return message;
         }
 
     }
 
 
     /** Current Logger */               private static ErrorLogger currentLogger;
     /** List of all loggers */          private static List allLoggers = new ArrayList();
 
 	private static boolean alreadyExplained = false;
 
     private int trueNumErrors;
     private int errorLimit;
     private List allErrors;
     private int currentErrorNumber;
     private boolean limitExceeded;
     private String errorSystem;
     private boolean terminated;
     private boolean persistent; // cannot be deleted
 
     private ErrorLogger() {}
 
     /**
      * Create a new ErrorLogger instance, with persistent set false, so
      * it can be deleted from tree
      */
     public static synchronized ErrorLogger newInstance(String system)
     {
         return newInstance(system, false);
     }
 
     /**
      * Create a new ErrorLogger instance. If persistent is true, it cannot be
      * delete from the explorer tree (although deletes will clear it, thereby removing
      * it until another error is logged to it). This is useful for tools that report
      * errors incrementally, such as the Network tool.
      * @param system the name of the system logging errors
      * @param persistent if true, this error tree cannot be deleted
      * @return a new ErrorLogger for logging errors
      */
     public static ErrorLogger newInstance(String system, boolean persistent)
     {
         ErrorLogger logger = new ErrorLogger();
         logger.allErrors = new ArrayList();
         logger.trueNumErrors = 0;
         logger.limitExceeded = false;
         logger.currentErrorNumber = -1;
         logger.errorSystem = system;
         logger.errorLimit = User.getErrorLimit();
         logger.terminated = false;
         logger.persistent = persistent;
         synchronized(allLoggers) {
             if (currentLogger == null) currentLogger = logger;
             allLoggers.add(logger);
         }
         return logger;
     }
 
     /**
      * Factory method to create an error message and log.
      * with the given text "message" applying to cell "cell".
      * Returns a pointer to the message (0 on error) which can be used to add highlights.
      */
     public synchronized ErrorLog logError(String message, Cell cell, int sortKey)
     {
         if (terminated && !persistent) {
             System.out.println("WARNING: "+errorSystem+" already terminated, should not log new error");
         }
 
         trueNumErrors++;
 
         // if too many errors, don't save it
         if (errorLimit > 0 && numErrors() >= errorLimit)
         {
             if (!limitExceeded)
             {
                 System.out.println("WARNING: more than " + errorLimit + " errors found, ignoring the rest");
                 limitExceeded = true;
             }
             return null;
         }
 
         // create a new ErrorLog object
         ErrorLog el = new ErrorLog(message, cell, sortKey);
 
         // store information about the error
         el.message = message;
         el.cell = cell;
         el.sortKey = sortKey;
         el.highlights = new ArrayList();
 
         // add the ErrorLog into the global list
         allErrors.add(el);
         currentErrorNumber = allErrors.size()-1;
 
         if (persistent) WindowFrame.wantToRedoErrorTree();
         return el;
     }
 
 	/**
 	 * Method to remove all logged errors from this errorlog.
 	 */
 	public synchronized void clearAllErrors() { allErrors.clear(); }
 
     /** Get the current logger */
     public synchronized static ErrorLogger getCurrent() {
         synchronized(allLoggers) {
             if (currentLogger == null) return newInstance("Unknown");
             return currentLogger;
         }
     }
 
     /** Delete this logger */
     public synchronized void delete() {
 
         if (persistent) {
             // just clear errors
             allErrors.clear();
             trueNumErrors = 0;
             currentErrorNumber = -1;
             WindowFrame.wantToRedoErrorTree();
             return;
         }
 
         synchronized(allLoggers) {
             allLoggers.remove(this);
             if (currentLogger == this) {
                 if (allLoggers.size() > 0) currentLogger = (ErrorLogger)allLoggers.get(0);
                 else currentLogger = null;
             }
         }
         WindowFrame.wantToRedoErrorTree();
     }
 
     public String describe() {
         synchronized(allLoggers) {
             if (currentLogger == this) return errorSystem + " [Current]";
         }
         return errorSystem;
     }
 
     /**
      * Method called when all errors are logged.  Initializes pointers for replay of errors.
      */
     public synchronized void termLogging(boolean explain)
     {
         // enumerate the errors
         int errs = 0;
         for(Iterator it = allErrors.iterator(); it.hasNext(); )
         {
             ErrorLog el = (ErrorLog)it.next();
             el.index = ++errs;
         }
 
         if (errs == 0) {
             delete();
             return;
         }
 
 //		if (db_errorchangedroutine != 0) (*db_errorchangedroutine)();
 
         if (errs > 0 && explain)
         {
             //System.out.println(errorSystem+" FOUND "+numErrors()+" ERRORS");
             if (!alreadyExplained)
             {
 				alreadyExplained = true;
 	            System.out.println("Type > and < to step through errors, or open the ERRORS view in the explorer");
             }
         }
         WindowFrame.wantToRedoErrorTree();
         synchronized(allLoggers) {
             currentLogger = this;
         }
 
         terminated = true;
     }
 
 
     /**
      * Method to sort the errors by their "key" (a value provided to "logerror()").
      * Obviously, this should be called after all errors have been reported.
      */
     public synchronized void sortErrors()
     {
         Collections.sort(allErrors, new ErrorLogOrder());
     }
 
     private static class ErrorLogOrder implements Comparator
     {
         public int compare(Object o1, Object o2)
         {
             ErrorLog el1 = (ErrorLog)o1;
             ErrorLog el2 = (ErrorLog)o2;
             return el1.sortKey - el2.sortKey;
         }
     }
 
     /**
      * Method to return the number of logged errors.
      */
     public synchronized int numErrors()
     {
         return trueNumErrors;
     }
 
     /**
      * Method to advance to the next error and report it.
      */
     public static String reportNextError()
     {
         return reportNextError(true, null);
     }
 
     /**
      * Method to advance to the next error and report it.
      */
     public static String reportNextError(boolean showhigh, Geometric [] gPair)
     {
         ErrorLogger logger;
         synchronized(allLoggers) {
             if (currentLogger == null) return "No errors to report";
             logger = currentLogger;
         }
         return logger.reportNextError_(showhigh, gPair);
     }
 
     private synchronized String reportNextError_(boolean showHigh, Geometric [] gPair) {
         if (currentErrorNumber < allErrors.size()-1)
         {
             currentErrorNumber++;
         } else
         {
             if (allErrors.size() <= 0) return "No "+errorSystem+" errors";
             currentErrorNumber = 0;
         }
         return reportError(currentErrorNumber, showHigh, gPair);
     }
 
     /**
      * Method to back up to the previous error and report it.
      */
     public static String reportPrevError()
     {
         ErrorLogger logger;
         synchronized(allLoggers) {
             if (currentLogger == null) return "No errors to report";
             logger = currentLogger;
         }
         return logger.reportPrevError_();
     }
 
     private synchronized String reportPrevError_() {
         if (currentErrorNumber > 0)
         {
             currentErrorNumber--;
         } else
         {
             if (allErrors.size() <= 0) return "No "+errorSystem+" errors";
             currentErrorNumber = allErrors.size() - 1;
         }
         return reportError(currentErrorNumber, true, null);
     }
 
     /**
      * Report an error
      * @param errorNumber
      * @param showHigh
      * @param gPair
      * @return
      */
     private synchronized String reportError(int errorNumber, boolean showHigh, Geometric [] gPair) {
 
         if (errorNumber < 0 || (errorNumber >= allErrors.size())) {
             return errorSystem + ": no such error "+(errorNumber+1)+", only "+numErrors()+" errors.";
         }
         ErrorLog el = (ErrorLog)allErrors.get(errorNumber);
         String message = el.reportError(showHigh, gPair);
         return (errorSystem + " error " + (errorNumber+1) + " of " + allErrors.size() + ": " + message);
     }
 
     /**
      * Method to tell the number of logged errors.
      * @return the number of "ErrorLog" objects logged.
      */
     public synchronized int getNumErrors() { return allErrors.size(); }
 
     /**
      * Method to list all logged errors.
      * @return an Iterator over all of the "ErrorLog" objects.
      */
     public synchronized Iterator getErrors() {
         List copy = new ArrayList();
         for (Iterator it = allErrors.iterator(); it.hasNext(); ) {
             copy.add(it.next());
         }
         return copy.iterator();
     }
 
     public synchronized void deleteError(ErrorLog error) {
         if (!allErrors.contains(error)) {
             System.out.println(errorSystem+ ": Does not contain error to delete");
         }
         allErrors.remove(error);
         trueNumErrors--;
         if (currentErrorNumber >= allErrors.size()) currentErrorNumber = 0;
     }
 
 
     // ----------------------------- Explorer Tree Stuff ---------------------------
 
     /**
      * A static object is used so that its open/closed tree state can be maintained.
      */
     private static String errorNode = "ERRORS";
 
     public static DefaultMutableTreeNode getExplorerTree()
     {
         DefaultMutableTreeNode explorerTree = new DefaultMutableTreeNode(errorNode);
         synchronized(allLoggers) {
             for (Iterator eit = allLoggers.iterator(); eit.hasNext(); ) {
                 ErrorLogger logger = (ErrorLogger)eit.next();
                 if (logger.getNumErrors() == 0) continue;
                 DefaultMutableTreeNode loggerNode = new DefaultMutableTreeNode(logger);
                 for (Iterator it = logger.allErrors.iterator(); it.hasNext();)
                 {
                     ErrorLog el = (ErrorLog)it.next();
                     DefaultMutableTreeNode node = new DefaultMutableTreeNode(el);
                     loggerNode.add(node);
                 }
                 explorerTree.add(loggerNode);
             }
         }
         return explorerTree;
     }
 
     public JPopupMenu getPopupMenu() {
         JPopupMenu p = new JPopupMenu();
         JMenuItem m;
         m = new JMenuItem("Delete"); m.addActionListener(this); p.add(m);
         m = new JMenuItem("Set Current"); m.addActionListener(this); p.add(m);
         return p;
     }
 
     public void actionPerformed(ActionEvent e) {
         if (e.getSource() instanceof JMenuItem) {
             JMenuItem m = (JMenuItem)e.getSource();
             if (m.getText().equals("Delete")) delete();
             if (m.getText().equals("Set Current")) {
                 synchronized(allLoggers) { currentLogger = this; }
                 WindowFrame.wantToRedoErrorTree();
             }
         }
     }
 }
