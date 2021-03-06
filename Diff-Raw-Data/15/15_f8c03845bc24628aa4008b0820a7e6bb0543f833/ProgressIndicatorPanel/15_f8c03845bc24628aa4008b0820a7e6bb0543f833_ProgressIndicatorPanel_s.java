 /*
  * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
  * written by Rasto Levrinc.
  *
  * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
  *
  * DRBD Management Console is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License as published
  * by the Free Software Foundation; either version 2, or (at your option)
  * any later version.
  *
  * DRBD Management Console is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with drbd; see the file COPYING.  If not, write to
  * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
  */
 
 /**
  * 18:12 17/02/2005
  * Romain Guy <romain.guy@jext.org>
  * Subject to the BSD license.
  */
 
 package drbd.gui;
 
 import drbd.utilities.Tools;
 import drbd.utilities.MyButton;
 
 import java.util.Map;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.ArrayList;
 import java.util.LinkedList;
 
 import java.awt.Color;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.RenderingHints;
 import java.awt.event.MouseEvent;
 import java.awt.event.MouseListener;
 import java.awt.event.KeyEvent;
 import java.awt.event.KeyListener;
 import java.awt.font.FontRenderContext;
 import java.awt.font.TextLayout;
 import java.awt.Font;
 import java.awt.geom.Rectangle2D;
 
 import javax.swing.JComponent;
 import javax.swing.ImageIcon;
 
 import EDU.oswego.cs.dl.util.concurrent.Mutex;
 /**
  * An infinite progress panel displays a rotating figure and
  * a message to notice the user of a long, duration unknown
  * task. The shape and the text are drawn upon a white veil
  * which alpha level (or shield value) lets the underlying
  * component shine through. This panel is meant to be used
  * asa <i>glass pane</i> in the window performing the long
  * operation.
  * <br /><br />
  * On the contrary to regular glass panes, you don't need to
  * set it visible or not by yourself. Once you've started the
  * animation all the mouse events are intercepted by this
  * panel, preventing them from being forwared to the
  * underlying components.
  * <br /><br />
  * The panel can be controlled by the <code>start()</code>,
  * <code>stop()</code> and <code>interrupt()</code> methods.
  * <br /><br />
  * Example:
  * <br /><br />
  * <pre>ProgressIndicatorPanel pane = new ProgressIndicatorPanel();
  * frame.setGlassPane(pane);
  * pane.start()</pre>
  * <br /><br />
  * Several properties can be configured at creation time. The
  * message and its font can be changed at runtime. Changing the
  * font can be done using <code>setFont()</code> and
  * <code>setForeground()</code>.
  */
 
 public class ProgressIndicatorPanel extends JComponent
                                     implements MouseListener, KeyListener {
     /** Serial version UID. */
     private static final long serialVersionUID = 1L;
     /** The animation thread is responsible for fade in/out and rotation. */
     private Thread animation  = null;
     /** Notifies whether the animation is running or not. */
     private boolean started    = false;
     /** Alpha level of the veil, used for fade in/out. */
     private int alphaLevel = 0;
     /** Duration of the veil's fade in/out. */
     private int rampDelay  = 1000;
     /** Ramp delay stop. */
     private static final int rampDelayStop  = 1000;
     /** Alpha level of the veil. */
     private float shield     = 0.80f;
     /** Message displayed below the circular shape. */
     private final Map<String, Integer> texts =
                                         new LinkedHashMap<String, Integer>();
     /** List of failed commands. */
     private final List<String> failuresMap = new LinkedList<String>();
     /** Amount of frames per seconde. Lowers this to save CPU. */
     private float fps        = 15.0f;
     /** Rendering hints to set anti aliasing. */
     private RenderingHints hints = null;
     /** Lock for the animator. */
     private final Mutex mAnimatorLock = new Mutex();
     /** Lock for ramp. */
     private final Mutex mRampLock = new Mutex();
     /** Lock for manipulating the text array. */
     private final Mutex mTextsLock = new Mutex();
     /** Cancel button icon. */
     private static final ImageIcon CANCEL_ICON =
                                 Tools.createImageIcon(Tools.getDefault(
                                         "ProgressIndicatorPanel.CancelIcon"));
     /** Cancel button. TODO: not used. */
     private final MyButton cancelButton = new MyButton(
                 Tools.getString("ProgressIndicatorPanel.Cancel"), CANCEL_ICON);
     /** Animator thread. */
     private Animator animator;
 
     /** Old width of the whole frame. */
     private int oldWidth  = getWidth();
     /** Old height of the whole frame. */
     private int oldHeight = getHeight();
     /** Beginning position of the bar. */
     private int barPos = -15;
     /** Maximum alpha level. */
     private static final int MAX_ALPHA_LEVEL = 255;
 
     /**
      * Creates a new progress panel with default values:<br />
      * <ul>
      * <li>Veil's alpha level is 70%</li>
      * <li>15 frames per second</li>
      * <li>Fade in/out last 300 ms</li>
      * </ul>.
      */
     public ProgressIndicatorPanel() {
         this(0.70f);
     }
 
     /**
      * Creates a new progress panel with default values:<br />
      * <ul>
      * <li>15 frames per second</li>
      * <li>Fade in/out last 300 ms</li>
      * </ul>.
      * @param shieldA The alpha level between 0.0 and 1.0 of the colored
      *                shield (or veil).
      */
     public ProgressIndicatorPanel(final float shieldA) {
         this(shieldA, 15.0f);
     }
 
     /**
      * Creates a new progress panel with default values:<br />
      * <ul>
      * <li>Fade in/out last 300 ms</li>
      * </ul>.
      * @param shieldA The alpha level between 0.0 and 1.0 of the colored
      *                shield (or veil).
      * @param fpsA The number of frames per second. Lower this value to
      *             decrease CPU usage.
      */
     public ProgressIndicatorPanel(final float shieldA, final float fpsA) {
         this(shieldA, fpsA, 300);
     }
 
     /**
      * Creates a new progress panel.
      * @param shield The alpha level between 0.0 and 1.0 of the colored
      *               shield (or veil).
      * @param fps The number of frames per second. Lower this value to
      *            decrease CPU usage.
      * @param rampDelay The duration, in milli seconds, of the fade in and
      *                  the fade out of the veil.
      */
     public ProgressIndicatorPanel(final float shield,
                                   final float fps,
                                   final int rampDelay) {
         super();
         this.rampDelay = rampDelay >= 0 ? rampDelay : 0;
         this.shield    = shield >= 0.0f ? shield : 0.0f;
         this.fps       = fps > 0.0f ? fps : 15.0f;
 
         this.hints = new RenderingHints(RenderingHints.KEY_RENDERING,
                                         RenderingHints.VALUE_RENDER_QUALITY);
         this.hints.put(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
         this.hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         cancelButton.setBounds(0,
                                0,
                                cancelButton.getPreferredSize().width,
                                cancelButton.getPreferredSize().height);
     }
 
     /**
      * Is called upan a failure.
      */
     public final void failure(final String text) {
         failuresMap.add(text);
         start(text);
         stop(text);
     }
 
     /**
      * Starts the waiting animation by fading the veil in, then
      * rotating the shapes. This method handles the visibility
      * of the glass pane.
      */
     public final void start(final String text) {
         try {
             mAnimatorLock.acquire();
         } catch (java.lang.InterruptedException e) {
             Thread.currentThread().interrupt();
             return;
         }
         try {
             mTextsLock.acquire();
         } catch (java.lang.InterruptedException e) {
             Thread.currentThread().interrupt();
             return;
         }
         texts.put(text, MAX_ALPHA_LEVEL);
 
         if (texts.size() > 1) {
             mTextsLock.release();
             try {
                 mRampLock.acquire();
             } catch (java.lang.InterruptedException e) {
                 Thread.currentThread().interrupt();
             }
             animator.setRampUp(true);
             mRampLock.release();
             mAnimatorLock.release();
             return;
         }
         mTextsLock.release();
         Tools.getGUIData().getMainMenu().turnOff();
         cancelButton.setEnabled(true);
         addMouseListener(this);
         addKeyListener(this);
         if (!isVisible()) {
             setVisible(true);
         }
         animator = new Animator();
         animation = new Thread(animator);
         animation.start();
         mAnimatorLock.release();
     }
 
     /**
      * Stops the waiting animation by stopping the rotation
      * of the circular shape and then by fading out the veil.
      * This methods sets the panel invisible at the end.
      */
     public final void stop(final String text) {
         try {
            mTextsLock.acquire();
         } catch (java.lang.InterruptedException e) {
             Thread.currentThread().interrupt();
         }
         try {
            mAnimatorLock.acquire();
         } catch (java.lang.InterruptedException e) {
             Thread.currentThread().interrupt();
         }
         if (!texts.containsKey(text)) {
             Tools.appWarning("progress indicator already stopped for: "
                              + text);
            mAnimatorLock.release();
             mTextsLock.release();
             return;
         }
         texts.put(text, 250);
         for (final String t : texts.keySet()) {
             final int a = texts.get(t).intValue();
             if (a == MAX_ALPHA_LEVEL) {
                 /* at least one is going up */
                mAnimatorLock.release();
                 mTextsLock.release();
                 return;
             }
         }
         mTextsLock.release();
         try {
             mRampLock.acquire();
         } catch (java.lang.InterruptedException e) {
             Thread.currentThread().interrupt();
         }
         animator.setRampUp(false);
         mRampLock.release();
         removeMouseListener(ProgressIndicatorPanel.this);
         removeKeyListener(ProgressIndicatorPanel.this);
         Tools.getGUIData().getMainMenu().turnOn();
         mAnimatorLock.release();
     }
 
     /**
      * Interrupts the animation, whatever its state is. You
      * can use it when you need to stop the animation without
      * running the fade out phase.
      * This methods sets the panel invisible at the end.
      */
     public final void interrupt() {
         if (animation != null) {
             animation.interrupt();
             animation = null;
 
             removeMouseListener(this);
             removeKeyListener(this);
             setVisible(false);
            System.out.println("interrupt");
         }
     }
 
     /**
      * Paints the glass pane with info and progress indicator.
      */
     public final void paintComponent(final Graphics g) {
         if (started) {
             final int width  = getWidth();
 
             double maxY = 0.0;
 
             final Graphics2D g2 = (Graphics2D) g;
             g2.setRenderingHints(hints);
 
             g2.setColor(new Color(200, 221, 242,
                                   (int) (alphaLevel * shield)));
             g2.fillRect(0,
                         0,
                         getWidth(),
                         Tools.getGUIData().getTerminalPanelPos());
             if (barPos < getWidth()) {
                 final int he = Tools.getGUIData().getTerminalPanelPos() - 25;
                 g2.setColor(new Color(250, 133, 34,
                                       (int) (alphaLevel * shield * 0.5)));
                 g2.fillRect(barPos, 25, 30, he);
                 g2.fillRect(getWidth() - barPos, 25, 30, he);
             }
             barPos += 30;
             if (barPos >= getWidth()) {
                 barPos = -15;
             }
 
 
             try {
                 mTextsLock.acquire();
             } catch (java.lang.InterruptedException e) {
                 Thread.currentThread().interrupt();
             }
             if (!texts.isEmpty()) {
                 final FontRenderContext context = g2.getFontRenderContext();
                 Font font = new Font(getFont().getName(),
                                      getFont().getStyle(),
                                      (int) (getFont().getSize() * 1.75));
 
                 maxY = getHeight() / 2 - 30;
 
                 int y = 0;
                 for (String text : texts.keySet()) {
                     if (text == null || text.length() == 0) {
                         continue;
                     }
                     if ("OH MY GOD!!!".equals(text)) {
                         /* unlikely */
                         font = new Font(getFont().getName(),
                                          getFont().getStyle(),
                                          (getFont().getSize() * 8));
                     }
                     final int alpha = texts.get(text).intValue();
                     final TextLayout layout = new TextLayout(
                                                     text,
                                                     new Font(font.getName(),
                                                              font.getStyle(),
                                                              font.getSize()),
                                                     context);
                     final Rectangle2D bounds = layout.getBounds();
                     Color f;
                     if (failuresMap.contains(text)) {
                         f = new Color(255, 0, 0);
                     } else {
                         f = new Color(0, 0, 0); //getForeground();
                     }
                     g2.setColor(new Color(f.getRed(),
                                           f.getGreen(),
                                           f.getBlue(),
                                           alpha));
                     layout.draw(g2,
                                 (float) (width - bounds.getWidth()) / 2,
                                 (float) (maxY + layout.getLeading() + 2
                                 * layout.getAscent()) + y);
                     y = (int) (y + (((float) 27 / MAX_ALPHA_LEVEL) * alpha));
                 }
             }
             mTextsLock.release();
             super.paintComponent(g2);
         }
 
     }
 
     /**
      * Animation thread.
      */
     private class Animator implements Runnable {
         /** Whether the alpha level goes up or down. */
         private volatile boolean rampUp;
 
         /**
          * Prepares a new <code>Animator</code> object.
          */
         protected Animator() {
             rampUp = true;
         }
 
         /**
          * Sets the rump up.
          */
         public void setRampUp(final boolean rampUp) {
             this.rampUp = rampUp;
         }
 
         /**
          * Runs the thread.
          */
         public void run() {
             long start = System.currentTimeMillis();
             if (rampDelay == 0) {
                 alphaLevel = rampUp ? MAX_ALPHA_LEVEL : 0;
             }
 
             started = true;
             try {
                 mRampLock.acquire();
             } catch (java.lang.InterruptedException e) {
                 Thread.currentThread().interrupt();
             }
             mRampLock.release();
 
             while (true) {
                 try {
                     mRampLock.acquire();
                 } catch (java.lang.InterruptedException e) {
                     Thread.currentThread().interrupt();
                 }
                 final boolean lRampUp = rampUp;
                 mRampLock.release();
                 if (getWidth() != oldWidth || getHeight() != oldHeight) {
                     oldWidth  = getWidth();
                     oldHeight = getHeight();
                 }
 
                 repaint();
                 final long time = System.currentTimeMillis();
                 if (lRampUp) {
                     alphaLevel +=
                           (int) (MAX_ALPHA_LEVEL * (time - start) / rampDelay);
                     if (alphaLevel >= MAX_ALPHA_LEVEL) {
                         alphaLevel = MAX_ALPHA_LEVEL;
                     }
                 } else {
                     alphaLevel -=
                       (int) (MAX_ALPHA_LEVEL * (time - start) / rampDelayStop);
                     if (alphaLevel <= 0) {
                         alphaLevel = 0;
                         try {
                             mTextsLock.acquire();
                         } catch (java.lang.InterruptedException e) {
                             Thread.currentThread().interrupt();
                         }
                         if (texts.size() <= 0) {
                             mTextsLock.release();
                             break;
                         } else {
                             mTextsLock.release();
                         }
                     }
                 }
 
 
                 final ArrayList<String> toRemove = new ArrayList<String>();
                 try {
                     mTextsLock.acquire();
                 } catch (java.lang.InterruptedException e) {
                     Thread.currentThread().interrupt();
                 }
                 for (final String text : texts.keySet()) {
                     int alpha = texts.get(text).intValue();
                     if (alpha < MAX_ALPHA_LEVEL) {
                         int delay = 1000;
                         if (failuresMap.contains(text)) {
                             delay = 1000;
                         }
                         alpha -=
                             (int) (MAX_ALPHA_LEVEL * (time - start) / delay);
                         if (alpha < 0) {
                             toRemove.add(text);
                         } else {
                             texts.put(text, alpha);
                         }
                     }
                 }
                 for (final String text : toRemove) {
                     texts.remove(text);
                     failuresMap.remove(text);
                 }
 
                 mTextsLock.release();
                 try {
                     Thread.sleep((int) (1000 / fps));
                 } catch (InterruptedException ie) {
                     Thread.currentThread().interrupt();
                 }
                 start = time;
                 Thread.yield();
             }
             started = false;
         }
     }
 
     /**
      * Returns the animation thread.
      */
     public final Thread getThread() {
         return animation;
     }
 
     /**
      * Mouse clicked.
      */
     public final void mouseClicked(final MouseEvent e) {
         /* do nothing */
     }
 
     /**
      * Mouse pressed.
      */
     public final void mousePressed(final MouseEvent e) {
         /* do nothing */
     }
 
     /**
      * Mouse released.
      */
     public final void mouseReleased(final MouseEvent e) {
         /* do nothing */
     }
 
     /**
      * Mouse entered.
      */
     public final void mouseEntered(final MouseEvent e) {
         /* do nothing */
     }
 
     /**
      * Mouse exited.
      */
     public final void mouseExited(final MouseEvent e) {
         /* do nothing */
     }
 
     /**
      * Key pressed.
      */
     public final void keyPressed(final KeyEvent e) {
         /* do nothing */
     }
 
     /**
      * Key released.
      */
     public final void keyReleased(final KeyEvent e) {
         /* do nothing */
     }
 
     /**
      * Key typed.
      */
     public final void keyTyped(final KeyEvent e) {
         /* do nothing */
     }
 }
