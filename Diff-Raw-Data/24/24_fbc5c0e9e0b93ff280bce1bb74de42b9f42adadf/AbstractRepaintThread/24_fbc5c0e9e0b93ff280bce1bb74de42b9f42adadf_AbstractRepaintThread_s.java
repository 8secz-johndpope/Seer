 /**
 
 TrakEM2 plugin for ImageJ(C).
 Copyright (C) 2007 Albert Cardona and Rodney Douglas.
 
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
 
 import java.awt.Component;
 import java.awt.Rectangle;
 import java.util.LinkedList;
 import java.util.ArrayList;
 import javax.swing.SwingUtilities;
 import ini.trakem2.utils.Lock;
 import ini.trakem2.utils.Utils;
 import ini.trakem2.utils.IJError;
 
 public abstract class AbstractRepaintThread extends Thread {
 
 	final private Lock lock_event = new Lock();
 	final private Lock lock_paint = new Lock();
 	private boolean quit = false;
 	final protected AbstractOffscreenThread off;
 	private final java.util.List<PaintEvent> events = new LinkedList<PaintEvent>();
 	private final Component target;
 
 	public AbstractRepaintThread(final Component target, final String name, final AbstractOffscreenThread off) {
 		super(name);
 		this.target = target;
 		this.off = off;
 		setPriority(Thread.NORM_PRIORITY);
 		try { setDaemon(true); } catch (Exception e) { e.printStackTrace(); }
 		start();
 	}
 
 	private class PaintEvent {
 		final Rectangle clipRect;
 		final boolean update_graphics;
 		PaintEvent(final Rectangle clipRect, final boolean update_graphics) {
 			this.clipRect = clipRect;
 			this.update_graphics = update_graphics; // java is sooo verbose... this class is just a tuple!
 		}
 	}
 
 	/** Queue a new request for painting, updating offscreen graphics. */
 	public final void paint(final Rectangle clipRect) {
 		paint(clipRect, true);
 	}
 
 	/** Queue a new request for painting. */
 	public void paint(final Rectangle clipRect, final boolean update_graphics) {
 		//if (update_graphics) Utils.printCaller(this, 12);
 		// queue the event
 		synchronized (lock_event) {
 			lock_event.lock();
 			events.add(new PaintEvent(clipRect, update_graphics));
 			lock_event.unlock();
 		}
 		// signal a repaint request
 		synchronized (lock_paint) {
 			lock_paint.notifyAll();
 		}
 	}
 
 	/** Will gracefully kill this thread by breaking its infinite wait-for-event loop, and also call cancel on all registered offscreen threads. */
 	public void quit() {
 		this.quit = true;
 		// notify and finish
 		synchronized (lock_paint) {
 			lock_paint.notifyAll();
 		}
 	}
 
 	public void run() {
 		while (!quit) {
 			try {
 				// wait until anyone issues a repaint event
 				if (0 == events.size()) {
 					synchronized (lock_paint) {
 						try { lock_paint.wait(); } catch (InterruptedException ie) {}
 					}
 				}
 
 				if (quit) {
 					off.interrupt();
 					return; // finish
 				}
 
 				// wait a bit to catch fast subsequent events
 				// 	10 miliseconds
 				try { Thread.sleep(10); } catch (InterruptedException ie) {}
 
 				// obtain all events up to now and clear the event queue
 				PaintEvent[] pe = null;
 				synchronized (lock_event) {
 					lock_event.lock();
 					pe = new PaintEvent[events.size()];
 					pe = events.toArray(pe);
 					events.clear();
 					lock_event.unlock();
 				}
 				if (0 == pe.length) {
 					Utils.log2("No repaint events (?)");
 					continue;
 				}
 
 				// obtain repaint parameters from merged events
 				Rectangle clipRect = pe[0].clipRect;
 				boolean update_graphics = pe[0].update_graphics;
 				for (int i=1; i<pe.length; i++) {
 					if (null != clipRect) {
 						if (null == pe[i].clipRect) clipRect = null; // all
 						else clipRect.add(pe[i].clipRect);
 					} // else 'null' clipRect means repaint the entire canvas
 					if (!update_graphics) update_graphics = pe[i].update_graphics;
 					else if (null == clipRect) break;
 				}
 
 				// issue an offscreen thread if necessary
 				if (update_graphics) {
 					handleUpdateGraphics(target, clipRect);
 				}
 
 				// repaint
 				if (null == clipRect) target.repaint(0, 0, 0, target.getWidth(), target.getHeight()); // using super.repaint() causes infinite thread loops in the IBM-1.4.2-ppc
 				else target.repaint(0, clipRect.x, clipRect.y, clipRect.width, clipRect.height);
 			} catch (Exception e) {
 				e.printStackTrace();
 			}
 		}
 	}
 
 	/** Child classes need to extend this method for handling the need of recreating offscreen images. */
 	abstract protected void handleUpdateGraphics(Component target, Rectangle clipRect);
 
	/** Waits until all offscreen threads are finished. */
 	public void waitForOffs() {
		try { off.join(); } catch (InterruptedException e) {}
 	}
 }
