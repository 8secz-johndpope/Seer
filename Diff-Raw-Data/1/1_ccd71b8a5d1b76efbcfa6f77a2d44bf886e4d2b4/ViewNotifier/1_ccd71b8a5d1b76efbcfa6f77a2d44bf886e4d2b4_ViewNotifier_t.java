 package org.basex.gui.view;
 
 import java.awt.Window;
 import org.basex.core.Context;
 import org.basex.core.Text;
 import org.basex.data.Nodes;
 import org.basex.gui.GUI;
 import org.basex.gui.dialog.Dialog;
 import org.basex.gui.dialog.DialogHelp;
 import org.basex.util.Array;
 
 /**
  * This class stores all views in a window and notifies views of global
  * changes.
  *
  * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
  * @author Christian Gruen
  */
 public final class ViewNotifier {
   /** Maximum history size. */
   public static final int MAXHIST = 20;
   /** History pointer. */
   public int hist;
   /** Reference to main window. */
   final GUI gui;
 
   /** Zoomed rectangle history. */
   private final Nodes[] marked = new Nodes[MAXHIST];
   /** Zoomed rectangle history. */
   private final Nodes[] cont = new Nodes[MAXHIST];
   /** Command history. */
   private final String[] queries = new String[MAXHIST];
   /** Attached views. */
   private View[] view = new View[0];
   /** Maximum history value. */
   private int maxhist;
 
   /**
    * Constructor.
    * @param main reference to the main window
    */
   public ViewNotifier(final GUI main) {
     gui = main;
   }
 
   /**
    * Adds a new view.
    * @param v view to be added
    */
   void add(final View v) {
     view = Array.add(view, v);
   }
 
   /**
    * Notifies all views of a data reference change.
    */
   public void init() {
     final Context ctx = gui.context;
     final boolean db = ctx.data != null;
     if(db) {
       cont[0] = ctx.current;
       marked[0] = new Nodes(ctx.data);
     } else {
       // close all dialogs (except help) together with database
       for(final Window w : gui.getOwnedWindows()) {
         if(w.isVisible() && w instanceof Dialog && !(w instanceof DialogHelp))
           ((Dialog) w).cancel();
       }
     }
 
     gui.context.focused = -1;
     hist = 0;
     maxhist = 0;
     for(final View v : view) v.refreshInit();
     gui.layoutViews();
     gui.setTitle(Text.TITLE + (db ? " - " + ctx.data.meta.name : ""));
   }
 
   /**
    * Notifies all views of a focus change.
    * @param pre focused pre value
    * @param vw the calling view
    */
   public void focus(final int pre, final View vw) {
     if(gui.context.focused == pre) return;
     gui.context.focused = pre;
     for(final View v : view) if(v != vw && v.visible()) v.refreshFocus();
     if(pre != -1) gui.status.setPath(ViewData.path(gui.context.data, pre));
   }
 
   /**
    * Notifies all views of a selection change.
    * @param mark marked nodes
    * @param vw the calling view
    */
   public void mark(final Nodes mark, final View vw) {
     final Context context = gui.context;
     context.marked = mark;
     for(final View v : view) if(v != vw && v.visible()) v.refreshMark();
     gui.filter.setEnabled(mark.size() != 0);
     gui.refreshControls();
   }
 
   /**
    * Notifies all views of a selection change.
    * The mode flag determines what happens:
    * <ul>
    * <li>0: set currently focused node as marked node</li>
    * <li>1: add currently focused node</li>
    * <li>2: toggle currently focused node</li>
    * </ul>
    * @param mode mark mode
    * @param vw the calling view
    */
   public void mark(final int mode, final View vw) {
     final int f = gui.context.focused;
     if(f == -1) return;
 
     final Context context = gui.context;
     Nodes nodes = context.marked;
     if(mode == 0) {
       nodes = new Nodes(f, context.data);
     } else if(mode == 1) {
       nodes.union(new int[] { f });
     } else {
       nodes.toggle(f);
     }
     mark(nodes, vw);
   }
 
   /**
    * Moves around in the internal history and notifies all views of
    * a context change.
    * @param forward move forward or backward
    */
   public void hist(final boolean forward) {
     // browse back/forward
     String query = "";
     if(forward) {
       if(hist == maxhist) return;
       query = queries[++hist];
     } else {
       if(hist == 0) return;
       marked[hist] = gui.context.marked;
       query = queries[--hist];
     }
 
     init(gui.context, cont[hist], marked[hist]);
 
     gui.input.setText(query);
     for(final View v : view) if(v.visible()) v.refreshContext(forward, false);
     gui.refreshControls();
   }
 
   /**
    * Notifies all views of a context change.
    * @param nodes new context set
    * @param quick quick switch
    * @param vw the calling view
    */
   public void context(final Nodes nodes, final boolean quick, final View vw) {
     final Context context = gui.context;
     final Nodes n = new Nodes(new int[0], context.data, context.marked.ftpos);
 
     if(!cont[hist].same(quick ? context.current : context.marked)) {
       checkHist();
       if(!quick) {
         // add new entry
         final String in = gui.input.getText();
         queries[hist] = in;
         marked[hist] = context.marked;
         cont[++hist] = nodes;
         queries[hist] = in;
         marked[hist] = n;
       } else {
         // check if current node set has already been cached
         // add new entry
         queries[hist] = "";
         marked[hist] = new Nodes(context.data);
         cont[++hist] = context.current;
       }
       maxhist = hist;
     }
     init(context, nodes, n);
 
     for(final View v : view) if(v != vw && v.visible())
       v.refreshContext(true, quick);
     gui.refreshControls();
   }
 
   /**
    * Notifies all views of updates in the data structure.
    */
   public void update() {
     hist = 0;
     maxhist = 0;
     final Context context = gui.context;
     final boolean noData = context.data == null || context.marked == null;
     context.marked = noData ? null : 
       new Nodes(new int[0], context.data, context.marked.ftpos);
     for(final View v : view) if(v.visible()) v.refreshUpdate();
     gui.refreshControls();
   }
 
   /**
    * Notifies all views of layout changes.
    */
   public void layout() {
     if(gui.help != null) gui.help.refresh();
     for(final View v : view) v.refreshLayout();
   }
 
   /**
    * Returns the last or next query string.
    * @param back back/forward flag
    * @return query string
    */
   public String tooltip(final boolean back) {
     return back ? hist > 0 ? hist > 1 ? queries[hist - 2] : "" : null :
       hist < maxhist ? queries[hist + 1] : null;
   }
 
   // PRIVATE METHODS ==========================================================
 
   /**
    * Initializes the current context and marked node set.
    * @param ctx context reference
    * @param curr context set
    * @param mark marked nodes
    */
   private void init(final Context ctx, final Nodes curr, final Nodes mark) {
     ctx.current = curr;
     ctx.marked = mark;
     ctx.focused = -1;
   }
 
   /**
    * Checks the history data arrays.
    */
   private void checkHist() {
     final int hl = queries.length;
     if(hist + 1 == hl) {
       Array.move(queries, 1, 0, hl - 1);
       Array.move(cont, 1, 0, hl - 1);
       Array.move(marked, 1, 0, hl - 1);
       hist--;
     }
   }
 }
