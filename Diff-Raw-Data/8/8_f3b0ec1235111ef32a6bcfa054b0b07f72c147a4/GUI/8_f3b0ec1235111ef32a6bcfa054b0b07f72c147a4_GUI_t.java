 package org.basex.gui;
 
 import static org.basex.core.Text.*;
 import static org.basex.gui.GUIConstants.*;
 import java.awt.BorderLayout;
 import java.awt.Dimension;
 import java.awt.GraphicsEnvironment;
 import java.awt.Toolkit;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import javax.swing.Box;
 import javax.swing.JComponent;
 import javax.swing.JFrame;
 import javax.swing.JMenuItem;
 import javax.swing.JPopupMenu;
 import javax.swing.SwingConstants;
 import javax.swing.WindowConstants;
 import javax.swing.border.CompoundBorder;
 import javax.swing.border.EmptyBorder;
 import javax.swing.border.EtchedBorder;
 import org.basex.core.BaseXException;
 import org.basex.core.CommandParser;
 import org.basex.core.Context;
 import org.basex.core.Command;
 import org.basex.core.Prop;
 import org.basex.core.cmd.Find;
 import org.basex.core.cmd.Set;
 import org.basex.core.cmd.XQuery;
 import org.basex.data.Data;
 import org.basex.data.Namespaces;
 import org.basex.data.Nodes;
 import org.basex.data.Result;
 import org.basex.gui.dialog.Dialog;
 import org.basex.gui.dialog.DialogHelp;
 import org.basex.gui.layout.BaseXBack;
 import org.basex.gui.layout.BaseXButton;
 import org.basex.gui.layout.BaseXCombo;
 import org.basex.gui.layout.BaseXLabel;
 import org.basex.gui.layout.BaseXLayout;
 import org.basex.gui.layout.TableLayout;
 import org.basex.gui.view.ViewContainer;
 import org.basex.gui.view.ViewNotifier;
 import org.basex.gui.view.explore.ExploreView;
 import org.basex.gui.view.folder.FolderView;
 import org.basex.gui.view.info.InfoView;
 import org.basex.gui.view.map.MapView;
 import org.basex.gui.view.plot.PlotView;
 import org.basex.gui.view.table.TableView;
 import org.basex.gui.view.text.TextView;
 import org.basex.gui.view.tree.TreeView;
 import org.basex.gui.view.xquery.XQueryView;
 import org.basex.io.ArrayOutput;
 import org.basex.query.QueryException;
 import org.basex.util.Performance;
 import org.basex.util.Token;
 import org.basex.util.Util;
 
 /**
  * This class is the main window of the GUI. It is the central instance
  * for user interactions.
  *
  * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
  * @author Christian Gruen
  */
 public final class GUI extends AGUI {
   /** View Manager. */
   public final ViewNotifier notify;
 
   /** Status line. */
   public final GUIStatus status;
   /** Input field. */
   public final GUIInput input;
   /** Filter button. */
   public final BaseXButton filter;
   /** Search view. */
   public final XQueryView query;
   /** Info view. */
   public final InfoView info;
 
   /** Painting flag; if activated, interactive operations are skipped. */
   public boolean painting;
   /** Updating flag; if activated, operations accessing the data are skipped. */
   public boolean updating;
   /** Fullscreen flag. */
   public boolean fullscreen;
   /** Help dialog. */
   public DialogHelp help;
 
   /** Content panel, containing all views. */
   final ViewContainer views;
   /** History button. */
   final BaseXButton hist;
   /** Current input Mode. */
   final BaseXCombo mode;
   /** Result panel. */
   final GUIMenu menu;
   /** Button panel. */
   final BaseXBack buttons;
   /** Query panel. */
   final BaseXBack nav;
 
   /** Text view. */
   private final TextView text;
   /** Top panel. */
   private final BaseXBack top;
   /** Execution Button. */
   private final BaseXButton go;
   /** Control panel. */
   private final BaseXBack control;
   /** Results label. */
   private final BaseXLabel hits;
   /** Buttons. */
   private final GUIToolBar toolbar;
 
   /** Current command. */
   private Command command;
   /** Menu panel height. */
   private int menuHeight;
   /** Fullscreen Window. */
   private JFrame fullscr;
   /** Thread counter. */
   private int threadID;
 
   /**
    * Default constructor.
    * @param ctx context reference
    * @param gprops gui properties
    */
   public GUI(final Context ctx, final GUIProp gprops) {
     super(ctx, gprops);
 
     // set window size
     final Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
     final int[] ps = gprop.nums(GUIProp.GUILOC);
     final int[] sz = gprop.nums(GUIProp.GUISIZE);
     final int x = Math.max(0, Math.min(scr.width - sz[0], ps[0]));
     final int y = Math.max(0, Math.min(scr.height - sz[1], ps[1]));
     setBounds(x, y, sz[0], sz[1]);
     if(gprop.is(GUIProp.MAXSTATE)) {
       setExtendedState(MAXIMIZED_HORIZ);
       setExtendedState(MAXIMIZED_VERT);
       setExtendedState(MAXIMIZED_BOTH);
     }
 
     top = new BaseXBack(new BorderLayout());
 
     // add header
     control = new BaseXBack(new BorderLayout());
 
     // add menu bar
     menu = new GUIMenu(this);
     if(gprop.is(GUIProp.SHOWMENU)) setJMenuBar(menu);
 
     buttons = new BaseXBack(new BorderLayout());
     toolbar = new GUIToolBar(TOOLBAR, this);
     buttons.add(toolbar, BorderLayout.WEST);
 
     hits = new BaseXLabel(" ");
     hits.setFont(hits.getFont().deriveFont(18f));
     BaseXLayout.setWidth(hits, 150);
     hits.setHorizontalAlignment(SwingConstants.RIGHT);
 
     BaseXBack b = new BaseXBack();
     b.add(hits);
 
     buttons.add(b, BorderLayout.EAST);
     if(gprop.is(GUIProp.SHOWBUTTONS)) control.add(buttons, BorderLayout.CENTER);
 
     nav = new BaseXBack(new BorderLayout(5, 0)).border(2, 2, 0, 2);
 
     mode = new BaseXCombo(this, BUTTONSEARCH, BUTTONXQUERY, BUTTONCMD);
     mode.setSelectedIndex(2);
 
     mode.addActionListener(new ActionListener() {
       @Override
       public void actionPerformed(final ActionEvent e) {
         final int s = mode.getSelectedIndex();
         if(s == gprop.num(GUIProp.SEARCHMODE) || !mode.isEnabled()) return;
 
         gprop.set(GUIProp.SEARCHMODE, s);
         input.setText("");
         refreshControls();
       }
     });
     nav.add(mode, BorderLayout.WEST);
 
     input = new GUIInput(this);
 
     hist = new BaseXButton(this, "hist", HELPHIST);
     hist.addActionListener(new ActionListener() {
       @Override
       public void actionPerformed(final ActionEvent e) {
         final JPopupMenu pop = new JPopupMenu();
         final ActionListener al = new ActionListener() {
           @Override
           public void actionPerformed(final ActionEvent ac) {
             input.setText(ac.getActionCommand());
             input.requestFocusInWindow();
             pop.setVisible(false);
           }
         };
         final int i = context.data == null ? 2 :
           gprop.num(GUIProp.SEARCHMODE);
         final String[] hs = i == 0 ? gprop.strings(GUIProp.SEARCH) : i == 1 ?
             gprop.strings(GUIProp.XQUERY) : gprop.strings(GUIProp.COMMANDS);
         for(final String en : hs) {
           final JMenuItem jmi = new JMenuItem(en);
           jmi.addActionListener(al);
           pop.add(jmi);
         }
         pop.show(hist, 0, hist.getHeight());
       }
     });
 
     b = new BaseXBack(new BorderLayout(5, 0));
     b.add(hist, BorderLayout.WEST);
     b.add(input, BorderLayout.CENTER);
     nav.add(b, BorderLayout.CENTER);
 
     go = new BaseXButton(this, "go", HELPGO);
     go.addActionListener(new ActionListener() {
       @Override
       public void actionPerformed(final ActionEvent e) {
         execute();
       }
     });
 
     filter = BaseXButton.command(GUICommands.FILTER, this);
 
     b = new BaseXBack(new TableLayout(1, 3));
     b.add(go);
     b.add(Box.createHorizontalStrut(1));
     b.add(filter);
     nav.add(b, BorderLayout.EAST);
 
     if(gprop.is(GUIProp.SHOWINPUT)) control.add(nav, BorderLayout.SOUTH);
     top.add(control, BorderLayout.NORTH);
 
     // create views
     notify = new ViewNotifier(this);
     text = new TextView(notify);
     query = new XQueryView(notify);
     info = new InfoView(notify);
 
     // create panels for closed and opened database mode
     views = new ViewContainer(this, text, query, info,
         new FolderView(notify), new PlotView(notify), new TableView(notify),
         new MapView(notify), new TreeView(notify), new ExploreView(notify)
     );
 
     top.add(views, BorderLayout.CENTER);
     setContentBorder();
 
     // add status bar
     status = new GUIStatus(this);
     if(gprop.is(GUIProp.SHOWSTATUS)) top.add(status, BorderLayout.SOUTH);
 
     setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
     add(top);
 
     setVisible(true);
     views.updateViews();
     refreshControls();
 
     // start logo animation as thread
     new Thread() {
       @Override
       public void run() {
         views.run();
       }
     }.start();
 
     input.requestFocusInWindow();
   }
 
   @Override
   public void dispose() {
     query.confirm();
     final boolean max = getExtendedState() == MAXIMIZED_BOTH;
     gprop.set(GUIProp.MAXSTATE, max);
     if(!max) {
       gprop.set(GUIProp.GUILOC, new int[] { getX(), getY() });
       gprop.set(GUIProp.GUISIZE, new int[] { getWidth(), getHeight() });
     }
     super.dispose();
     gprop.write();
     context.close();
   }
 
   /**
    * Executes the input of the {@link GUIInput} bar.
    */
   void execute() {
     final String in = input.getText().trim();
     final boolean db = context.data != null;
    final boolean cmd = gprop.num(GUIProp.SEARCHMODE) == 2 || !db; 
 
     if(cmd || in.startsWith("!")) {
       // run as command: command mode or exclamation mark as first character
       final int i = cmd ? 0 : 1;
       if(in.length() > i) {
        long t = 0;
        if(db) t = context.data.meta.time;
         try {
           for(final Command c : new CommandParser(in.substring(i),
               context).parse()) {
             if(!exec(c, c instanceof XQuery)) break;
             // [CG] fix for 894:, maybe just for update commands...
            if(t != 0 && t != context.data.meta.time) notify.init();
           }
         } catch(final QueryException ex) {
           if(!info.visible()) GUICommands.SHOWINFO.execute(this);
           info.setInfo(ex.getMessage(), null, null, false);
           info.reset();
         }
       }
     } else if(gprop.num(GUIProp.SEARCHMODE) == 1 || in.startsWith("/")) {
       xquery(in, true);
     } else {
       execute(new XQuery(Find.find(in, context, gprop.is(GUIProp.FILTERRT))),
           true);
     }
   }
 
   /**
    * Launches a query. Adds the default namespace, if available.
    * @param qu query to be run
    * @param main main window
    */
   public void xquery(final String qu, final boolean main) {
     // check and add default namespace
     final Namespaces ns = context.data.ns;
     String in = qu.trim().isEmpty() ? "()" : qu;
     final int u = ns.uri(Token.EMPTY, 0);
     if(u != 0) in = Util.info("declare default element namespace \"%\"; %",
         ns.uri(u), in);
     execute(new XQuery(in), main);
   }
 
   /**
    * Launches the specified command in a thread. The command is ignored
    * if an update operation takes place.
    * @param cmd command to be launched
    * @param main call from main window
    */
   public void execute(final Command cmd, final boolean main) {
     if(updating) return;
     new Thread() {
       @Override
       public void run() { exec(cmd, main); }
     }.start();
   }
 
   /**
    * Executes the specified command.
    * @param c command to be executed
    * @param main call from the main input field
    * @return success flag
    */
  boolean exec(final Command c, final boolean main) {
     final int thread = ++threadID;
 
     // wait when command is still running
     while(command != null) {
       command.stop();
       Performance.sleep(50);
       if(threadID != thread) return true;
     }
 
     cursor(CURSORWAIT);
     try {
       // cache some variables before executing the command
       final Performance perf = new Performance();
       final Nodes current = context.current;
       final Data data = context.data;
       command = c;
 
       // execute command and cache result
       final ArrayOutput ao =
         new ArrayOutput().max(context.prop.num(Prop.MAXTEXT));
       final boolean up = c.writing(context);
       updating = up;
 
       boolean ok = true;
       String inf = null;
       try {
         c.execute(context, ao);
         inf = c.info();
       } catch(final BaseXException ex) {
         ok = false;
         inf = ex.getMessage();
         if(!ok && inf.equals(PROGERR)) {
           // command was interrupted..
           command = null;
           return false;
         }
       } finally {
         updating = false;
       }
       final String time = perf.getTimer();
 
       // show query info
       info.setInfo(inf, c, time, ok);
       info.reset();
 
       // show feedback in query editor
       boolean feedback = main;
       if(!main && query.visible() && c instanceof XQuery) {
         query.info(inf, ok);
         feedback = true;
       }
 
       // check if query feedback was evaluated in the query view
       if(!ok) {
         // display error in info view
         if(!feedback && !info.visible()) GUICommands.SHOWINFO.execute(this);
       } else {
         // get query result
         final Result result = c.result();
         final Nodes nodes = result instanceof Nodes &&
           ((Nodes) result).size() != 0 ? (Nodes) result : null;
 
         // treat text view different to other views
         // [CG] fix for empty sequences
         if(ok && nodes == null) {
           // display text view
           if(!text.visible()) GUICommands.SHOWTEXT.execute(this);
           text.setText(ao, c);
         }
 
         final Data ndata = context.data;
         Nodes marked = context.marked;
 
         if(ndata != data) {
           // database reference has changed - notify views
           notify.init();
           // [LK] check if context really changed - an empty
           // updating function i.e.
           // sets up==true, but does not change the current context
         } else if(up) {
           // update command
           notify.update();
         } else if(result != null) {
           final Nodes nd = context.current;
           if(nd != null && !nd.sameAs(current) || gprop.is(GUIProp.FILTERRT)) {
             // refresh context if at least one node was found
             if(nodes != null) {
               notify.context((Nodes) result, gprop.is(GUIProp.FILTERRT), null);
             }
           } else if(marked != null) {
             // refresh highlight
             if(nodes != null) {
               // use query result
               marked = nodes;
             } else if(marked.size() != 0) {
               // remove old highlight
               marked = new Nodes(data);
             }
             // refresh views
             notify.mark(marked, null);
             if(thread != threadID) {
               command = null;
               return true;
             }
           }
         }
         // show number of hits
         setHits(result == null ? 0 : result.size());
 
         // show status info
         status.setText(Util.info(PROCTIME, time));
       }
     } catch(final Exception ex) {
       // unexpected error
       Util.stack(ex);
       Dialog.error(this, Util.info(PROCERR, c,
           !ex.toString().isEmpty() ? ex.toString() : ex.getMessage()));
       updating = false;
     }
 
     cursor(CURSORARROW, true);
     command = null;
     return true;
   }
 
  /**
   * Stops the current process.
   */
  public void stop() {
    if(command != null) command.stop();
    cursor(CURSORARROW, true);
    command = null;
  }
 
  /**
   * Sets a property and displays the command in the info view.
   * @param pr property to be set
   * @param val value
   */
  public void set(final Object[] pr, final Object val) {
    if(!context.prop.sameAs(pr, val)) {
      final Set cmd = new Set(pr, val);
      cmd.run(context);
      info.setInfo(cmd.info(), cmd, null, true);
    }
  }
 
   /**
    * Sets the border of the content area.
    */
   private void setContentBorder() {
     final int n = control.getComponentCount();
     final int n2 = top.getComponentCount();
 
     if(n == 0 && n2 == 2) {
       views.border(0, 0, 0, 0);
     } else {
       views.setBorder(new CompoundBorder(new EmptyBorder(3, 1, 3, 1),
           new EtchedBorder()));
     }
   }
 
   /**
    * Refreshes the layout.
    */
   public void updateLayout() {
     init(gprop);
     notify.layout();
   }
 
   /**
    * Updates the control panel.
    * @param comp component to be updated
    * @param show true if component is visible
    * @param layout component layout
    */
   void updateControl(final JComponent comp, final boolean show,
       final String layout) {
 
     if(comp == status) {
       if(!show) top.remove(comp);
       else top.add(comp, layout);
     } else if(comp == menu) {
       if(!show) menuHeight = menu.getHeight();
       final int s = show ? menuHeight : 0;
       BaseXLayout.setHeight(menu, s);
       menu.setSize(menu.getWidth(), s);
     } else { // buttons, input
       if(!show) control.remove(comp);
       else control.add(comp, layout);
     }
     setContentBorder();
     (fullscr == null ? getRootPane() : fullscr).validate();
     refreshControls();
   }
 
   /**
    * Updates the view layout.
    */
   public void layoutViews() {
     views.updateViews();
     refreshControls();
     repaint();
   }
 
   /**
    * Refreshes the menu and the buttons.
    */
   public void refreshControls() {
     final Nodes marked = context.marked;
     if(marked != null) setHits(marked.size());
 
     filter.setEnabled(marked != null && marked.size() != 0);
 
     final boolean inf = gprop.is(GUIProp.SHOWINFO);
     context.prop.set(Prop.QUERYINFO, inf);
     context.prop.set(Prop.XMLPLAN, inf);
 
     final Data data = context.data;
     final boolean db = data != null;
     final int t = mode.getSelectedIndex();
     final int s = !db ? 2 : gprop.num(GUIProp.SEARCHMODE);
 
     input.help(s == 0 ? data.fs != null ? HELPSEARCHFS : HELPSEARCHXML :
       s == 1 ? HELPXPATH : HELPCMD);
     mode.setEnabled(db);
     go.setEnabled(s == 2 || !gprop.is(GUIProp.EXECRT));
 
     if(s != t) {
       mode.setSelectedIndex(s);
       input.setText("");
       input.requestFocusInWindow();
     }
 
     toolbar.refresh();
     menu.refresh();
 
     final int i = context.data == null ? 2 : gprop.num(GUIProp.SEARCHMODE);
     final String[] hs = i == 0 ? gprop.strings(GUIProp.SEARCH) : i == 1 ?
         gprop.strings(GUIProp.XQUERY) : gprop.strings(GUIProp.COMMANDS);
     hist.setEnabled(hs.length != 0);
   }
 
   /**
    * Sets hits information.
    * @param n number of hits
    */
   private void setHits(final long n) {
     hits.setText(n + " " + HITS);
   }
 
   /**
    * Turns fullscreen mode on/off.
    */
   void fullscreen() {
     fullscreen ^= true;
     fullscreen(fullscreen);
   }
 
   /**
    * Turns fullscreen mode on/off.
    * @param full fullscreen mode
    */
   public void fullscreen(final boolean full) {
     if(full ^ fullscr == null) {
       if(!gprop.is(GUIProp.SHOWMENU)) GUICommands.SHOWMENU.execute(this);
       return;
     }
 
     if(full) {
       control.remove(buttons);
       control.remove(nav);
       getRootPane().remove(menu);
       top.remove(status);
       remove(top);
       fullscr = new JFrame();
       fullscr.setIconImage(getIconImage());
       fullscr.setTitle(getTitle());
       fullscr.setUndecorated(true);
       fullscr.setJMenuBar(menu);
       fullscr.add(top);
       fullscr.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
     } else {
       fullscr.removeAll();
       fullscr.dispose();
       fullscr = null;
       if(!gprop.is(GUIProp.SHOWBUTTONS))
         control.add(buttons, BorderLayout.CENTER);
       if(!gprop.is(GUIProp.SHOWINPUT)) control.add(nav, BorderLayout.SOUTH);
       if(!gprop.is(GUIProp.SHOWSTATUS)) top.add(status, BorderLayout.SOUTH);
       setJMenuBar(menu);
       add(top);
     }
 
     gprop.set(GUIProp.SHOWMENU, !full);
     gprop.set(GUIProp.SHOWBUTTONS, !full);
     gprop.set(GUIProp.SHOWINPUT, !full);
     gprop.set(GUIProp.SHOWSTATUS, !full);
     fullscreen = full;
 
     GraphicsEnvironment.getLocalGraphicsEnvironment().
       getDefaultScreenDevice().setFullScreenWindow(fullscr);
     setContentBorder();
     refreshControls();
     updateControl(menu, !full, BorderLayout.NORTH);
     setVisible(!full);
   }
 }
