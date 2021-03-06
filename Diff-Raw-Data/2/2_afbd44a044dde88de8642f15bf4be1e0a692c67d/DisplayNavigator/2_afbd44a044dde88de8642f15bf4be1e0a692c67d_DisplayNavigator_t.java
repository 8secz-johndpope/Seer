 /**
 
 TrakEM2 plugin for ImageJ(C).
 Copyright (C) 2005, 2006 Albert Cardona and Rodney Douglas.
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
 
 You may contact Albert Cardona at acardona at ini.phys.ethz.ch
 Institute of Neuroinformatics, University of Zurich / ETH, Switzerland.
 **/
 
 package ini.trakem2.display;
 
 //import java.awt.Canvas;
 import javax.swing.JPanel;
 import java.awt.Image;
 import java.awt.Color;
 import java.awt.Component;
 import java.awt.Dimension;
 import java.awt.Graphics;
 import java.awt.image.BufferedImage;
 import java.awt.image.VolatileImage;
 import java.awt.Graphics2D;
 import java.awt.BasicStroke;
 import java.awt.Rectangle;
 import java.awt.event.MouseListener;
 import java.awt.event.MouseMotionListener;
 import java.awt.event.MouseEvent;
 import java.util.Iterator;
 import java.util.ArrayList;
 import java.util.HashSet;
 import ini.trakem2.utils.*;
 import java.awt.geom.AffineTransform;
 import java.awt.GraphicsConfiguration;
 
 public final class DisplayNavigator extends JPanel implements MouseListener, MouseMotionListener {
 
 	private Display display;
 	private Layer layer;
 	private HashSet hs_painted = new HashSet();
 	static private final int FIXED_WIDTH = 250;
 	private int height;
 	private BufferedImage image = null;
 	private boolean redraw_displayables = true;
 	private double scale;
 	private Rectangle srcRect;
 	private int x_p, y_p;
 	private int new_x_old=0, new_y_old=0;
 
 	private final Object updating_ob = new Object();
 	private boolean updating = false;
 
 	private VolatileImage volatileImage;
 	private boolean invalid_volatile = false;
 
 	DisplayNavigator(Display display, double layer_width, double layer_height) { // contorsions to avoid java bugs ( a.k.a. the 'this' is not functional until the object in question has finished initialization.
 		this.display = display;
 		this.layer = display.getLayer();
 		this.scale = FIXED_WIDTH / layer_width;
 		this.height = (int)(layer_height * scale);
 		//Utils.log("fixed_w, h: " + FIXED_WIDTH +","+ height + "   layer_width,height: " + layer_width + "," + layer_height);
 		Dimension d = new Dimension(FIXED_WIDTH, height);
 		setPreferredSize(d);
 		setMinimumSize(d);
 		setMaximumSize(d);
 		addMouseMotionListener(this);
 		addMouseListener(this);
 		addKeyListener(display.getCanvas());
 	}
 
 	/** Fixes size if changed. Multithreaded. */
 	public void repaint() {
 		if (null == display || null == display.getCanvas() || null == display.getLayer() || display.getCanvas().isDragging()) return;
 		// fixing null at start up (because the JPanel becomes initialized and repainted before returning to my subclass constructor! Stupid java!)
 		if (null == display) return;
 
 		//check if layer has changed
 		if (this.layer != display.getLayer()) {
 			this.layer = display.getLayer();
 			this.hs_painted.clear();
 		}
 
 		scale = FIXED_WIDTH / display.getLayer().getLayerWidth();
 		int height = (int)(display.getLayer().getLayerHeight() * scale);
 		if (height != this.height) {
 			Dimension d = new Dimension(FIXED_WIDTH, height);
 			setPreferredSize(d);
 			setMinimumSize(d);
 			setMaximumSize(d); //this triple set *should* update the values in the super class JPanel
 			redraw_displayables = true;
 			invalid_volatile = true;
 			this.height = height;
 		}
 		//Utils.log2("w,h: " + FIXED_WIDTH + "," + height + ",   scale: " + scale);
 		// magic cocktel:
 		//this.invalidate();
 		//this.validate();  // possible cause of infinite loops with infinite threads
 		RT.paint(null, redraw_displayables);
 	}
 
 	public void repaint(boolean update_graphics) {
 		redraw_displayables = update_graphics;
 		invalid_volatile = true;
 		repaint();
 	}
 
 	/** Only its bounding box. */ // TODO problems: when the object has been moved, it leaves a trail (no repainting of the old position). So this is for now only useful for the setVisible (where the object doesn't move)
 	public void repaint(Displayable d) {
 		if (display.getCanvas().isDragging()) return;
 		redraw_displayables = true;
 		invalid_volatile = true;
 		final Rectangle r = d.getBoundingBox(null);
 		r.x = (int)(r.x * scale);
 		r.y = (int)(r.y * scale);
 		r.width = (int)Math.ceil(r.width * scale);
 		r.height = (int)Math.ceil(r.height * scale);
 		RT.paint(r, redraw_displayables);
 	}
 
 	/** Overridden to multithread. TrakEM2 does not call this method directly ever. */
 	public void repaint(int x, int y, int width, int height) {
 		if (display.getCanvas().isDragging()) return;
 		RT.paint(new Rectangle(x, y, width, height), redraw_displayables);
 	}
 
 	/** Box is given in offscreen canvas coords. */
 	public void repaint(Rectangle box) {
		if (null == box || display.getCanvas().isDragging()) return;
 		// bring box to the scale
 		Rectangle b = new Rectangle((int)(box.x * scale), (int)(box.y * scale), (int)Math.ceil(box.width * scale), (int)Math.ceil(box.height * scale));
 		RT.paint(b, redraw_displayables);
 	}
 
 	/* // saved as not overridden to make sure there are no infinite thread loops when calling super in buggy JVMs
 	public void repaint(long ms, int x, int y, int width, int height) {
 		RT.paint(new Rectangle(x, y, width, height));
 	}
 	*/
 
 	public void update(Graphics g) {
 		paint(g);
 	}
 
 
 	private int snapshots_mode = 0;
 
 	private final class UpdateGraphicsThread extends AbstractOffscreenThread {
 
 		private final Rectangle clipRect;
 
 		UpdateGraphicsThread(final Rectangle clipRect) {
 			super("T2-Navigator-UpdateGraphics");
 			this.clipRect = clipRect;
 			synchronized (updating_ob) {
 				while (updating) {
 					try {
 						if (quit) return;
 						updating_ob.wait();
 					} catch (InterruptedException ie) {}
 				}
 				updating = true;
 				quit = false;
 				updating = false;
 				updating_ob.notifyAll();
 			}
 			Thread.yield();
 			setPriority(Thread.NORM_PRIORITY);
 			final int snapshots_mode = layer.getParent().getSnapshotsMode();
 			if (null != DisplayNavigator.this.image && 2 == snapshots_mode && DisplayNavigator.this.snapshots_mode == snapshots_mode) {
 				DisplayNavigator.this.redraw_displayables = false;
 				RT.paintFromOff(clipRect, this.start);
 				return;
 			} else {
 				DisplayNavigator.this.snapshots_mode = snapshots_mode;
 			}
 			// else recreate offscreen data:
 			start();
 		}
 
 		/** paint all snapshots, scaled, to an offscreen awt.Image */
 		public void run() {
 			try { Thread.sleep(10); } catch (InterruptedException ie) {}
 			if (quit) return;
 			final BufferedImage target = new BufferedImage(FIXED_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
 
 			final int snapshots_mode = layer.getParent().getSnapshotsMode();
 
 			try {
 				if (quit) {
 					target.flush();
 					return;
 				}
 
 				//g2d.getRenderingHints().put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
 				//g2d.getRenderingHints().put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
 				//Rectangle clipRect = g.getClipBounds();
 
 				final Graphics2D g = target.createGraphics();
 				// paint background as black
 				g.setColor(Color.black);
 				g.fillRect(0, 0, DisplayNavigator.super.getWidth(), DisplayNavigator.super.getHeight());
 
 				// check if disabled
 				if (2 != snapshots_mode) {
 					// set a scaled stroke, or 0.4 if too small
 					if (scale >= 0.4D) g.setStroke(new BasicStroke((float)scale));
 					else g.setStroke(new BasicStroke(0.4f));
 
 					g.scale(scale, scale);
 
 					final ArrayList al = display.getLayer().getDisplayables();
 					final int size = al.size();
 					boolean zd_done = false;
 					for (int i=0; i<size; i++) {
 						if (quit) {
 							g.dispose();
 							target.flush();
 							return;
 						}
 						final Displayable d = (Displayable)al.get(i);
 						//if (d.isOutOfRepaintingClip(clip, scale)) continue; // needed at least for the visibility
 						if (!d.isVisible()) continue; // TODO proper clipRect for this navigator image may be necessary (lots of changes needed in the lines above reltive to filling the black background, etc)
 						final Class c = d.getClass();
 						if (!zd_done && DLabel.class == c) {
 							zd_done = true;
 							// paint ZDisplayables before the labels (i.e. text labels on top)
 							final Iterator itz = display.getLayer().getParent().getZDisplayables().iterator();
 							while (itz.hasNext()) {
 								if (quit) {
 									g.dispose();
 									target.flush();
 									return;
 								}
 								ZDisplayable zd = (ZDisplayable)itz.next();
 								if (!zd.isVisible()) continue;
 								zd.paintSnapshot(g, scale);
 							}
 							// paint the label too!
 							d.paint(g, scale, false, 1, DisplayNavigator.this.layer);
 						} else if (Patch.class == c) {
 							if (0 == snapshots_mode) {
 								// paint fully
 								final Patch p = (Patch)d;
 								final Image img = d.getProject().getLoader().getCachedClosestAboveImage(p, scale);
 								if (null != img) {
 									if (d.isVisible()) d.paint(g, scale, false, p.getChannelAlphas(), DisplayNavigator.this.layer);
 									hs_painted.add(d);
 								} else  {
 									d.paintAsBox(g);
 								}
 							} else {
 								// paint as outlines
 								d.paintAsBox(g);
 							}
 						} else {
 							if (d.isVisible()) d.paint(g, scale, false, 1, DisplayNavigator.this.layer);
 						}
 					}
 					if (!zd_done) { // if no labels, ZDisplayables haven't been painted
 						zd_done = true;
 						// paint ZDisplayables before the labels
 						final Iterator itz = display.getLayer().getParent().getZDisplayables().iterator();
 						while (itz.hasNext()) {
 							if (quit) {
 								g.dispose();
 								target.flush();
 								return;
 							}
 							ZDisplayable zd = (ZDisplayable)itz.next();
 							if (!zd.isVisible()) continue;
 							zd.paintSnapshot(g, scale);
 						}
 					}
 				}
 				// finally, when done, call repaint (like sending an event)
 
 				// block only while modifying the image pointer
 				synchronized (updating_ob) {
 					while (updating) {
 						try { updating_ob.wait(); } catch (InterruptedException ie) {}
 					}
 					updating = true;
 
 					height = DisplayNavigator.super.getHeight();
 					DisplayNavigator.this.image = target;
 					redraw_displayables = false;
 
 					updating = false;
 					updating_ob.notifyAll();
 				}
 				RT.paintFromOff(clipRect, this.start);
 			} catch (Exception e) {
 				IJError.print(e);
 			}
 		}
 	}
 
 	private void renderVolatileImage(final BufferedImage bufferedImage) {
 		do {
 			final int w = FIXED_WIDTH, h = this.height;
 			final GraphicsConfiguration gc = getGraphicsConfiguration();
 			if (invalid_volatile || volatileImage == null || volatileImage.getWidth() != w 
 					|| volatileImage.getHeight() != h
 					|| volatileImage.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
 				if (volatileImage != null) {
 					volatileImage.flush();
 				}
 				volatileImage = gc.createCompatibleVolatileImage(w, h);
 				volatileImage.setAccelerationPriority(1.0f);
 				invalid_volatile = false;
 			}
 			// 
 			// Now paint the BufferedImage into the accelerated image
 			//
 			final Graphics2D g = volatileImage.createGraphics();
 			g.drawImage(bufferedImage, 0, 0, FIXED_WIDTH, this.height, null);
 
 			// paint red rectangle indicating srcRect
 			final Rectangle srcRect = display.getCanvas().getSrcRect();
 			g.setColor(Color.red);
 			g.setStroke(new BasicStroke(2.0f));
 			int gw = (int)(srcRect.width * scale) -2;
 			int gh = (int)(srcRect.height * scale) -2;
 			if (gw < 5) gw = 5;
 			if (gh < 5) gh = 5;
 			g.drawRect((int)(srcRect.x * scale) +1, (int)(srcRect.y * scale) +1, gw, gh);
 
 		} while (volatileImage.contentsLost());
 	}
 
 	private void render(final Graphics g) {
 		final Graphics2D g2d = (Graphics2D) g.create();
 		g2d.setRenderingHints(DisplayCanvas.rhints);
 		do {
 			if (invalid_volatile || null == volatileImage
 			 || volatileImage.validate(getGraphicsConfiguration()) != VolatileImage.IMAGE_OK)
 			{
 				renderVolatileImage(image);
 			}
 			g2d.drawImage(volatileImage, 0, 0, null);
 		} while (volatileImage.contentsLost());
 
 		g2d.dispose();
 	}
 
 	public void paint(final Graphics g) {
 		final Graphics2D g2d = (Graphics2D)g;
 		synchronized (updating_ob) {
 			while (updating) { try { updating_ob.wait(); } catch (InterruptedException ie) {} }
 			updating = true;
 			/*
 			if (null != image) {
 				g.drawImage(image, 0, 0, FIXED_WIDTH, this.height, null);
 			}
 			*/
 			render(g);
 
 			updating = false;
 			updating_ob.notifyAll();
 		}
 	}
 
 	/** Handles repaint event requests and the generation of offscreen threads. */
 	private final AbstractRepaintThread RT = new AbstractRepaintThread(this, "T2-Navigator-Repainter") {
 		protected void handleUpdateGraphics(Component target, Rectangle clipRect) {
 			try {
 				// Signal previous offscreen threads to quit
 				cancelOffs();
 				// issue new offscreen thread
 				final UpdateGraphicsThread off = new UpdateGraphicsThread(clipRect);
 				// store to be canceled if necessary
 				add(off);
 			} catch (Exception e) {
 				IJError.print(e);
 			}
 		}
 	};
 
 	private boolean drag = false;
 
 	public void mousePressed(final MouseEvent me) {
 		x_p = me.getX();
 		y_p = me.getY();
 		this.srcRect = (Rectangle)display.getCanvas().getSrcRect().clone();
 		// prevent dragging unless mouse is inside the red box
 		if (srcRect.contains((int)(x_p / scale), (int)(y_p / scale))) {
 			drag = true;
 		}
 	}
 
 	public void mouseDragged(final MouseEvent me) {
 		if (!drag) return;
 		// prevent action if the srcRect takes over the whole area
 		if (this.srcRect.width == display.getLayer().getLayerWidth() && this.srcRect.height == display.getLayer().getLayerHeight()) {
 			return;
 		}
 		int x_d = me.getX();
 		int y_d = me.getY();
 		// prevent dragging beyond screen
 		if (x_d > this.getWidth() || x_d < 0 || y_d > this.getHeight() || y_d < 0) {
 			return;
 		}
 		int new_x = srcRect.x + (int)((x_d - x_p) / scale);
 		int new_y = srcRect.y + (int)((y_d - y_p) / scale);
 		if (new_x < 0) new_x = 0;
 		if (new_y < 0) new_y = 0;
 		if (new_x + srcRect.width > (int)(this.getWidth() / scale)) new_x = (int)(this.getWidth() / scale - srcRect.width);
 		if (new_y + srcRect.height > (int)(this.getHeight() / scale)) new_y = (int)(this.getHeight() / scale - srcRect.height);
 		if (new_x_old == new_x && new_y_old == new_y) {
 			// avoid repaints
 			return;
 		}
 		new_x_old = new_x;
 		new_y_old = new_y;
 		DisplayCanvas canvas = display.getCanvas();
 		canvas.setSrcRect(new_x, new_y, this.srcRect.width, this.srcRect.height);
 		canvas.repaint(true);
 		invalid_volatile = true;
 		this.repaint();
 	}
 
 	public void mouseReleased(MouseEvent me) { drag = false; }
 	public void mouseEntered(MouseEvent me) {}
 	public void mouseExited (MouseEvent me) {}
 	public void mouseClicked(MouseEvent me) {}
 	public void mouseMoved(MouseEvent me) {}
 
 	/** Release resources. */
 	public void destroy() {
 		synchronized (updating_ob) {
 			while (updating) try { updating_ob.wait(); } catch (InterruptedException ie) {}
 			updating = true;
 			RT.quit();
 			updating = false;
 			updating_ob.notifyAll();
 		}
 		Thread.yield();
 		synchronized (updating_ob) {
 			while (updating) try { updating_ob.wait(); } catch (InterruptedException ie) {}
 			updating = true;
 			if (null != image) {
 				image.flush();
 				image = null;
 			}
 			updating = false;
 			updating_ob.notifyAll();
 		}
 	}
 
 	/** Returns true if the given Displayable has been painted as an image and false if as a box or not at all. */
 	public boolean isPainted(Displayable d) {
 		return hs_painted.contains(d);
 	}
 }
