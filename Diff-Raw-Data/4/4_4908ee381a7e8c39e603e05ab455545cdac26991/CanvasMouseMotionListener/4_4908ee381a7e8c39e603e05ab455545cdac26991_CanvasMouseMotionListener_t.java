 
 package net.sf.freecol.client.gui;
 
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseMotionListener;
 import java.lang.reflect.InvocationTargetException;
 
 import javax.swing.SwingUtilities;
 
 import net.sf.freecol.client.ClientOptions;
 import net.sf.freecol.common.model.Map;
 import net.sf.freecol.common.model.PathNode;
 import net.sf.freecol.common.model.Tile;
 
 
 /**
  * Listens to the mouse being moved at the level of the Canvas.
  */
 public final class CanvasMouseMotionListener implements MouseMotionListener {
     public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
     public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
     public static final String  REVISION = "$Revision$";
 
     private final Canvas canvas;
     private final GUI gui;
     private final Map map;
     private ScrollThread scrollThread;
 
     //private static final int SCROLLSPACE = 3;
     private static final int SCROLLSPACE = 100;
 
     
 
     /**
      * The constructor to use.
      * 
      * @param canvas The component this object gets created for.
      * @param g The GUI that holds information such as screen resolution.
      * @param m The Map that is currently being drawn on the
      * Canvas (by the GUI).
      */
     public CanvasMouseMotionListener(Canvas canvas, GUI g, Map m) {
         this.canvas = canvas;
         gui = g;
         map = m;
         scrollThread = null;
     }
     
 
     /**
      * Invoked when the mouse has been moved.
      * @param e The MouseEvent that holds all the information.
      */
     public void mouseMoved(MouseEvent e) {
         /*if (e.getComponent().isEnabled()) {
             scroll(e.getX(), e.getY());
         } else if (scrollThread != null) {
             scrollThread.stopScrolling();
             scrollThread = null;
         }*/
        if (scrollThread != null) {
            scrollThread.stopScrolling();
            scrollThread = null;
        }
     }
     
     private void scroll(int x, int y) {
         if (!canvas.getClient().getClientOptions().getBoolean(ClientOptions.MAP_SCROLL_ON_DRAG)) {
             return;
         }
         /*if (y < canvas.getMenuBarHeight()) {
             if (scrollThread != null) {
                 scrollThread.stopScrolling();
                 scrollThread = null;
             }
             return;
         } else if (y < canvas.getMenuBarHeight() + SCROLLSPACE) {
             y -= canvas.getMenuBarHeight();
         }*/
         int direction;
         if ((x < SCROLLSPACE) && (y < SCROLLSPACE)) {
             // Upper-Left
             direction = Map.NW;
         } else if ((x >= gui.getWidth() - SCROLLSPACE) && (y < SCROLLSPACE)) {
             // Upper-Right
             direction = Map.NE;
         } else if ((x >= gui.getWidth() - SCROLLSPACE) && (y >= gui.getHeight() - SCROLLSPACE)) {
             // Bottom-Right
             direction = Map.SE;
         } else if ((x < SCROLLSPACE) && (y >= gui.getHeight() - SCROLLSPACE)) {
             // Bottom-Left
             direction = Map.SW;
         } else if (y < SCROLLSPACE) {
             // Top
             direction = Map.N;
         } else if (x >= gui.getWidth() - SCROLLSPACE) {
             // Right
             direction = Map.E;
         } else if (y >= gui.getHeight() - SCROLLSPACE) {
             // Bottom
             direction = Map.S;
         } else if (x < SCROLLSPACE) {
             // Left
             direction = Map.W;
         } else {
             // Center
             if (scrollThread != null) {
                 scrollThread.stopScrolling();
                 scrollThread = null;
             }
             return;
         }
 
         if (scrollThread != null) {
             scrollThread.setDirection(direction);
         } else {
             scrollThread = new ScrollThread(map, gui);
             scrollThread.start();
         }
     }
 
     /**
      * Invoked when the mouse has been dragged.
      * @param e The MouseEvent that holds all the information.
      */
     public void mouseDragged(MouseEvent e) {
         Map.Position p = gui.convertToMapCoordinates(e.getX(), e.getY());
         
         if (e.getComponent().isEnabled()) {
             scroll(e.getX(), e.getY());
         } else if (scrollThread != null) {
             scrollThread.stopScrolling();
             scrollThread = null;
         }
         
         if (p == null || !map.isValid(p)) {
             return;
         }
 
         Tile tile = map.getTile(p);
         if (tile != null) {
             if (gui.getActiveUnit() == null) {
                 gui.stopDrag();
             } else if (gui.getActiveUnit().getTile() != tile) {
                 if (gui.isDragStarted()) {
                     PathNode dragPath = gui.getActiveUnit().findPath(tile);
                     // ONLY FOR DEBUGGING: PathNode dragPath = map.findPath(gui.getActiveUnit(), gui.getActiveUnit().getTile(), tile, (Unit) gui.getActiveUnit().getLocation());
                     gui.setDragPath(dragPath);
                 }
             } else {
                 if (!gui.isDragStarted()) {
                     gui.startDrag();
                 } else {
                     gui.setDragPath(null);
                 }
             }
         }
     }
     
     
     /**
      * Scrolls the view of the Map by moving its focus.
      */
     private class ScrollThread extends Thread {
         private final Map map;
         private final GUI gui;
         private int direction;
         private boolean cont;
 
         /**
          * The constructor to use.
          * @param m The Map that needs to be scrolled.
          * @param g The GUI that holds information such as screen resolution.
          */
         public ScrollThread(Map m, GUI g) {
             map = m;
             gui = g;
             cont = true;
         }
 
         /**
          * Sets the direction in which this ScrollThread will scroll.
          * @param d The direction in which this ScrollThread will scroll.
          */
         public void setDirection(int d) {
             direction = d;
         }
 
         /**
          * Makes this ScrollThread stop doing what it is supposed to do.
          */
         public void stopScrolling() {
             cont = false;
         }
 
         /**
          * Performs the actual scrolling.
          */
         public void run() {
             do {
                 try {
                     sleep(100);
                 } catch (InterruptedException e) {}
 
                 try {
                     SwingUtilities.invokeAndWait(new Runnable() {
                         public void run() {
                             int x, y;
                             Tile t = map.getTileOrNull(gui.getFocus().getX(), gui.getFocus().getY());
                             if (t == null) {
                                 return;
                             }
 
                             t = map.getNeighbourOrNull(direction, t);
                             if (t == null) {
                                 return;
                             }
 
                             if (gui.isMapNearTop(t.getY()) && gui.isMapNearTop(gui.getFocus().getY())) {
                                 if (t.getY() > gui.getFocus().getY()) {
                                     y = t.getY();
                                     do {
                                         y += 2;
                                     } while (gui.isMapNearTop(y));
                                 } else {
                                     y = gui.getFocus().getY();
                                 }
                             } else if (gui.isMapNearBottom(t.getY()) && gui.isMapNearBottom(gui.getFocus().getY())) {
                                 if (t.getY() < gui.getFocus().getY()) {
                                     y = t.getY();
                                     do {
                                         y -= 2;
                                     } while (gui.isMapNearBottom(y));
                                 } else {
                                     y = gui.getFocus().getY();
                                 }
                             } else {
                                 y = t.getY();
                             }
 
                             if (gui.isMapNearLeft(t.getX(), t.getY()) && gui.isMapNearLeft(gui.getFocus().getX(), gui.getFocus().getY())) {
                                 if (t.getX() > gui.getFocus().getX()) {
                                     x = t.getX();
                                     do {
                                         x++;
                                     } while (gui.isMapNearLeft(x, y));
                                 } else {
                                     x = gui.getFocus().getX();
                                 }
                             } else if (gui.isMapNearRight(t.getX(), t.getY()) && gui.isMapNearRight(gui.getFocus().getX(), gui.getFocus().getY())) {
                                 if (t.getX() < gui.getFocus().getX()) {
                                     x = t.getX();
                                     do {
                                         x--;
                                     } while (gui.isMapNearRight(x, y));
                                 } else {
                                     x = gui.getFocus().getX();
                                 }
                             } else {
                                 x = t.getX();
                             }
 
                             gui.setFocus(x, y);
                         }
                     });
                 } catch (InvocationTargetException e) {
                     cont = false;
                 } catch (InterruptedException e) {
                     cont = false;
                 }
             } while (cont);
         }
     }    
 }
