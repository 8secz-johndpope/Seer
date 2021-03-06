 /**
 
 TrakEM2 plugin for ImageJ(C).
 Copyright (C) 2005, 2006, 2007 Albert Cardona and Rodney Douglas.
 
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
 
 
 import ij.IJ;
 import ij.gui.OvalRoi;
 import ij.gui.Roi;
 import ij.gui.ShapeRoi;
 import ij.gui.PolygonRoi;
 import ij.gui.Toolbar;
 import ij.gui.GenericDialog;
 import ij.io.FileSaver;
 import ij.process.ByteProcessor;
 import ij.process.ImageProcessor;
 import ij.process.FloatPolygon;
 import ij.ImagePlus;
 import ij.ImageStack;
 import ij.measure.Calibration;
 import ij.measure.ResultsTable;
 
 import ini.trakem2.Project;
 import ini.trakem2.persistence.DBObject;
 import ini.trakem2.utils.ProjectToolbar;
 import ini.trakem2.utils.IJError;
 import ini.trakem2.utils.Utils;
 import ini.trakem2.utils.M;
 import ini.trakem2.render3d.Perimeter2D;
 import ini.trakem2.vector.VectorString3D;
 
 import java.awt.Color;
 import java.awt.Rectangle;
 import java.awt.Point;
 import java.awt.Polygon;
 import java.awt.event.MouseEvent;
 import java.awt.event.KeyEvent;
 import java.util.*;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.Area;
 import java.awt.geom.GeneralPath;
 import java.awt.geom.NoninvertibleTransformException;
 import java.awt.geom.PathIterator;
 import java.awt.geom.Point2D;
 import java.awt.image.BufferedImage;
 import java.awt.image.DataBufferByte;
 import java.awt.Shape;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.Composite;
 import java.awt.AlphaComposite;
 import java.io.File;
 
 import marchingcubes.MCTriangulator;
 import isosurface.Triangulator;
 import amira.AmiraMeshEncoder;
 import amira.AmiraParameters;
 
 import javax.vecmath.Point3f;
 
 /** A list of brush painted areas similar to a set of labelfields in Amira.
  * 
  * For each layer where painting has been done, there is an entry in the ht_areas HashMap that contains the layer's id as a Long, and a java.awt.geom.Area object.
  * All Area objects are local to this AreaList's AffineTransform.
  */
 public class AreaList extends ZDisplayable {
 
 	/** Contains the table of layer ids and their associated Area object.*/
 	private HashMap ht_areas = new HashMap();
 
 	/** Flag to signal dynamic loading from the database for the Area of a given layer id in the ht_areas HashMap. */
 	static private Object UNLOADED = new Object();
 
 	/** Flag to repaint faster even if the object is selected. */
 	static private boolean brushing = false;
 
 	/** Paint as outlines (false) or as solid areas (true; default, with a default alpha of 0.4f).*/
 	private boolean fill_paint = true;
 
 	public AreaList(Project project, String title, double x, double y) {
 		super(project, title, x, y);
 		this.alpha = 0.4f;
 		addToDatabase();
 	}
 
 	/** Reconstruct from XML. */
 	public AreaList(Project project, long id, HashMap ht_attributes, HashMap ht_links) {
 		super(project, id, ht_attributes, ht_links);
 		// read the fill_paint
 		Object ob_data = ht_attributes.get("fill_paint");
 		try {
 			if (null != ob_data) this.fill_paint = "true".equals(((String)ob_data).trim().toLowerCase()); // fails: //Boolean.getBoolean((String)ob_data);
 		} catch (Exception e) {
 			Utils.log("AreaList: could not read fill_paint value from XML:" + e);
 		}
 	}
 
 	/** Reconstruct from the database. */
 	public AreaList(Project project, long id, String title, double width, double height, float alpha, boolean visible, Color color, boolean locked, ArrayList al_ul, AffineTransform at) { // al_ul contains Long() wrapping layer ids
 		super(project, id, title, locked, at, width, height);
 		this.alpha = alpha;
 		this.visible = visible;
 		this.color = color;
 		for (Iterator it = al_ul.iterator(); it.hasNext(); ) {
 			ht_areas.put(it.next(), AreaList.UNLOADED); // assumes al_ul contains only Long instances wrapping layer_id long values
 		}
 	}
 
 	public void paint(final Graphics2D g, final double magnification, final boolean active, final int channels, final Layer active_layer) {
 		Object ob = ht_areas.get(new Long(active_layer.getId()));
 		if (null == ob) return;
 		if (AreaList.UNLOADED == ob) {
 			ob = loadLayer(active_layer.getId());
 			if (null == ob) return;
 		}
 		final Area area = (Area)ob;
 		g.setColor(this.color);
 		//arrange transparency
 		Composite original_composite = null;
 		if (alpha != 1.0f) {
 			original_composite = g.getComposite();
 			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
 		}
 
 		if (fill_paint) g.fill(area.createTransformedArea(this.at));
 		else 		g.draw(area.createTransformedArea(this.at));  // the contour only
 
 		// If adding, check
 		if (null != last) {
 			try {
 				final Area tmp = last.getTmpArea();
 				if (null != tmp) {
 					if (fill_paint) g.fill(tmp.createTransformedArea(this.at));
 					else            g.draw(tmp.createTransformedArea(this.at)); // won't be perfect except on mouse release
 				}
 			} catch (Exception e) {}
 		}
 
 		//Transparency: fix alpha composite back to original.
 		if (null != original_composite) {
 			g.setComposite(original_composite);
 		}
 	}
 
 	public void transformPoints(Layer layer, double dx, double dy, double rot, double xo, double yo) {
 		Utils.log("AreaList.transformPoints: not implemented yet.");
 	}
 
 	/** Returns the layer of lowest Z coordinate Layer where this ZDisplayable has a point in. */
 	public Layer getFirstLayer() {
 		double min_z = Double.MAX_VALUE;
 		Layer first_layer = null;
 		for (Iterator it = ht_areas.keySet().iterator(); it.hasNext(); ) {
 			Layer la = this.layer_set.getLayer(((Long)it.next()).longValue());
 			double z = la.getZ();
 			if (z < min_z) {
 				min_z = z;
 				first_layer = la;
 			}
 		}
 		return first_layer;
 	}
 
 	/** Returns the layer of highest Z coordinate Layer where this ZDisplayable has a point in. */
 	public Layer getLastLayer() {
 		double max_z = -Double.MAX_VALUE;
 		Layer last_layer = null;
 		for (Iterator it = ht_areas.keySet().iterator(); it.hasNext(); ) {
 			Layer la = this.layer_set.getLayer(((Long)it.next()).longValue());
 			double z = la.getZ();
 			if (z > max_z) {
 				max_z = z;
 				last_layer = la;
 			}
 		}
 		return last_layer;
 	} // I do REALLY miss Lisp macros. Writting the above two methods in a lispy way would make the java code unreadable
 
 	public void linkPatches() {
 		unlinkAll(Patch.class);
 		// cheap way: intersection of the patches' bounding box with the area
 		Rectangle r = new Rectangle();
 		for (Iterator it = ht_areas.entrySet().iterator(); it.hasNext(); ) {
 			Map.Entry entry = (Map.Entry)it.next();
 			Layer la = this.layer_set.getLayer(((Long)entry.getKey()).longValue());
 			Area area = (Area)entry.getValue();
 			area = area.createTransformedArea(this.at);
 			for (Iterator dit = la.getDisplayables(Patch.class).iterator(); dit.hasNext(); ) {
 				Displayable d = (Displayable)dit.next();
 				r = d.getBoundingBox(r);
 				if (area.intersects(r)) {
 					link(d, true);
 				}
 			}
 		}
 	}
 
 	/** Returns whether the point x,y is contained in this object at the given Layer. */
 	public boolean contains(final Layer layer, final int x, final int y) {
 		Object ob = ht_areas.get(new Long(layer.getId()));
 		if (null == ob) return false;
 		if (AreaList.UNLOADED == ob) {
 			ob = loadLayer(layer.getId());
 			if (null == ob) return false;
 		}
 		Area area = (Area)ob;
 		if (!this.at.isIdentity()) area = area.createTransformedArea(this.at);
 		return area.contains(x, y);
 	}
 
 	public boolean intersects(final Layer layer, final Rectangle r) {
 		Object ob = ht_areas.get(layer.getId());
 		if (null == ob) return false;
 		if (AreaList.UNLOADED == ob) {
 			ob = loadLayer(layer.getId());
 			if (null == ob) return false;
 		}
 		final Area a = ((Area)ob).createTransformedArea(this.at);
 		return a.intersects(r.x, r.y, r.width, r.height);
 	}
 
 	public boolean intersects(final Layer layer, final Area area) {
 		Object ob = ht_areas.get(layer.getId());
 		if (null == ob) return false;
 		if (AreaList.UNLOADED == ob) {
 			ob = loadLayer(layer.getId());
 			if (null == ob) return false;
 		}
 		final Area a = ((Area)ob).createTransformedArea(this.at);
 		a.intersect(area);
 		final Rectangle b = a.getBounds();
 		return 0 != b.width && 0 != b.height;
 	}
 
 	/** Returns the bounds of this object as it shows in the given layer. */
 	public Rectangle getBounds(final Rectangle r, final Layer layer) {
 		if (null == layer) return super.getBounds(r, null);
 		final Area area = (Area)ht_areas.get(layer.getId());
 		if (null == area) {
 			if (null == r) return new Rectangle();
 			r.x = 0;
 			r.y = 0;
 			r.width = 0;
 			r.height = 0;
 			return r;
 		}
 		final Rectangle b = area.createTransformedArea(this.at).getBounds();
 		if (null == r) return b;
 		r.setBounds(b.x, b.y, b.width, b.height);
 		return r;
 	}
 
 	public boolean isDeletable() {
 		if (0 == ht_areas.size()) return true;
 		return false;
 	}
 
 	private boolean is_new = false;
 
 	public void mousePressed(MouseEvent me, int x_p, int y_p, double mag) {
 		long lid = Display.getFrontLayer(this.project).getId(); // isn't this.layer pointing to the current layer always?
 		Object ob = ht_areas.get(new Long(lid));
 		Area area = null;
 		if (null == ob) {
 			area = new Area();
 			ht_areas.put(new Long(lid), area);
 			is_new = true;
 			this.width = layer_set.getLayerWidth(); // will be set properly at mouse release
 			this.height = layer_set.getLayerHeight(); // without this, the first brush slash doesn't get painted because the isOutOfRepaintingClip returns true
 		} else {
 			if (AreaList.UNLOADED == ob) {
 				ob = loadLayer(lid);
 				if (null == ob) return;
 			}
 			area = (Area)ob;
 		}
 
 		// transform the x_p, y_p to the local coordinates
 		if (!this.at.isIdentity()) {
 			final Point2D.Double p = inverseTransformPoint(x_p, y_p);
 			x_p = (int)p.x;
 			y_p = (int)p.y;
 		}
 
 		if (me.isShiftDown()) {
 			// fill in a hole if the clicked point lays within one
 			Polygon pol = null;
 			if (area.contains(x_p, y_p)) {
 				if (me.isAltDown()) {
 					// fill-remove
 					pol = findPath(area, x_p, y_p); // no null check, exists for sure
 					area.subtract(new Area(pol));
 				}
 			} else if (!me.isAltDown()) {
 				// fill-add
 				pol = findPath(area, x_p, y_p);
 				if (null != pol) {
 					area.add(new Area(pol)); // may not exist
 				}
 			}
 			if (null != pol) {
 				final Rectangle r_pol = transformRectangle(pol.getBounds());
 				Display.repaint(Display.getFrontLayer(), r_pol, 1);
 				updateInDatabase("points=" + lid);
 			}
 		} else {
 			if (null != last) last.quit();
 			last = new BrushThread(area, mag);
 			brushing = true;
 		}
 	}
 	public void mouseDragged(MouseEvent me, int x_p, int y_p, int x_d, int y_d, int x_d_old, int y_d_old) {
 		// nothing, the BrushThread handles it
 		//irrelevant//if (ProjectToolbar.getToolId() == ProjectToolbar.PEN) brushing = true;
 	}
 	public void mouseReleased(MouseEvent me, int x_p, int y_p, int x_d, int y_d, int x_r, int y_r) {
 		if (!brushing) {
 			// nothing changed
 			//Utils.log("AreaList mouseReleased: no brushing");
 			return;
 		}
 		brushing = false;
 		if (null != last) {
 			last.quit();
 			last = null;
 		}
 		long lid = Display.getFrontLayer(this.project).getId();
 		Object ob = ht_areas.get(new Long(lid));
 		Area area = null;
 		if (null != ob) {
 			area = (Area)ob;
 		}
 		// check if empty. If so, remove
 		Rectangle bounds = area.getBounds(); // TODO this can fail if the layer changes suddenly while painting
 		if (0 == bounds.width && 0 == bounds.height) {
 			ht_areas.remove(new Long(lid));
 			//Utils.log("removing empty area");
 		}
 
 		final boolean translated = calculateBoundingBox(); // will reset all areas' top-left coordinates, and update the database if necessary
 		if (translated) {
 			// update for all, since the bounding box has changed
 			updateInDatabase("all_points");
 		} else {
 			// update the points for the current layer only
 			updateInDatabase("points=" + lid);
 		}
 
 		// Repaint instead the last rectangle, to erase the circle
 		if (null != r_old) {
 			Display.repaint(Display.getFrontLayer(), r_old, 3, false);
 			r_old = null;
 		}
 		// repaint the navigator and snapshot
 		Display.repaint(Display.getFrontLayer(), this);
 	}
 
 	/** Calculate box, make this width,height be that of the box, and translate all areas to fit in. @param lid is the currently active Layer. */ //This is the only road to sanity for ZDisplayable objects.
 	public boolean calculateBoundingBox() {
 		// forget it if this has been done once already, for at the moment it would work only for translations, not any other types of transforms. TODO: need to fix this somehow, generates repainting problems.
 		//if (this.at.getType() != AffineTransform.TYPE_TRANSLATION) return false; // meaning, there's more bits in the type than just the translation
 		// check preconditions
 		if (0 == ht_areas.size()) return false;
 		Area[] area = new Area[ht_areas.size()];
 		Map.Entry[] entry = new Map.Entry[area.length];
 		ht_areas.entrySet().toArray(entry);
 		ht_areas.values().toArray(area);
 		Rectangle[] b = new Rectangle[area.length];
 		Rectangle box = null;
 		for (int i=0; i<area.length; i++) {
 			b[i] = area[i].getBounds();
 			if (null == box) box = (Rectangle)b[i].clone();
 			else box.add(b[i]);
 		}
 		if (null == box) return false; // empty AreaList
 		final AffineTransform atb = new AffineTransform();
 		atb.translate(-box.x, -box.y); // make local to overall box, so that box starts now at 0,0
 		for (int i=0; i<area.length; i++) {
 			entry[i].setValue(area[i].createTransformedArea(atb));
 		}
 		//this.translate(box.x, box.y);
 		this.at.translate(box.x, box.y);
 		this.width = box.width;
 		this.height = box.height;
 		updateInDatabase("transform+dimensions");
 		if (null != layer_set) layer_set.updateBucket(this);
 		if (0 != box.x || 0 != box.y) {
 			return true;
 		}
 		return false;
 	}
 
 	private BrushThread last = null;
 	static private Rectangle r_old = null;
 
 	/** Modeled after the ij.gui.RoiBrush class from ImageJ. */
 	private class BrushThread extends Thread {
 		/** The area to paint into when done or when removing. */
 		final private Area target_area;
 		/** The temporary area to paint to, which is only different than target_area when adding. */
 		final private Area area;
 		/** The list of all painted points. */
 		private final ArrayList<Point> points = new ArrayList<Point>();
 		/** The last point on which a paint event was done. */
 		private Point previous_p = null;
 		private boolean paint = true;
 		private int brush_size; // the diameter
 		private Area brush;
 		final private int leftClick=16, alt=9;
 		final private DisplayCanvas dc = Display.getFront().getCanvas();
 		final private int flags = dc.getModifiers();
 		private boolean adding = (0 == (flags & alt));
 
 		BrushThread(Area area, double mag) {
 			super("BrushThread");
 			setPriority(Thread.NORM_PRIORITY);
 			// if adding areas, make it be a copy, to be added on mouse release
 			// (In this way, the receiving Area is small and can be operated on fast)
 			if (adding) {
 				this.target_area = area;
 				this.area = new Area();
 			} else {
 				this.target_area = area;
 				this.area = area;
 			}
 
 			brush_size = ProjectToolbar.getBrushSize();
 			brush = makeBrush(brush_size, mag);
 			if (null == brush) return;
 			start();
 		}
 		final void quit() {
 			this.paint = false;
 			// Make interpolated points effect add or subtract operations
 			synchronized (this) {
 				if (points.size() < 2) {
 					// merge the temporary Area, if any, with the general one
 					if (adding) this.target_area.add(area);
 					return;
 				}
 
 				try {
 
 				// paint the regions between points
 				// A cheap way would be to just make a rectangle between both points, with thickess radius.
 				// A better, expensive way is to fit a spline first, then add each one as a circle.
 				// The spline way is wasteful, but way more precise and beautiful. Since there's only one repaint, it's not excessively slow.
 				int[] xp = new int[points.size()];
 				int[] yp = new int[xp.length];
 				int j = 0;
 				for (final Point p : points) {
 					xp[j] = p.x;
 					yp[j] = p.y;
 					j++;
 				}
 				points.clear();
 
 				PolygonRoi proi = new PolygonRoi(xp, yp, xp.length, Roi.POLYLINE);
 				proi.fitSpline();
 				FloatPolygon fp = proi.getFloatPolygon();
 				proi = null;
 
 				double[] xpd = new double[fp.npoints];
 				double[] ypd = new double[fp.npoints];
 				// Fails: fp contains float[], which for some reason cannot be copied into double[]
 				//System.arraycopy(fp.xpoints, 0, xpd, 0, xpd.length);
 				//System.arraycopy(fp.ypoints, 0, ypd, 0, ypd.length);
 				for (int i=0; i<xpd.length; i++) {
 					xpd[i] = fp.xpoints[i];
 					ypd[i] = fp.ypoints[i];
 				}
 				fp = null;
 
 				try {
 					// VectorString2D resampling doesn't work
 					VectorString3D vs = new VectorString3D(xpd, ypd, new double[xpd.length], false);
 					double delta = ((double)brush_size) / 10;
 					if (delta < 1) delta = 1;
 					vs.resample(delta);
 					xpd = vs.getPoints(0);
 					ypd = vs.getPoints(1);
 					vs = null;
 				} catch (Exception e) { IJError.print(e); }
 
 
 				final AffineTransform atb = new AffineTransform();
 
 				final AffineTransform inv_at = at.createInverse();
 
 				if (adding) {
 					adding = false;
 					for (int i=0; i<xpd.length; i++) {
 						atb.setToTranslation((int)xpd[i], (int)ypd[i]); // always integers
 						atb.preConcatenate(inv_at);
 						area.add(slashInInts(brush.createTransformedArea(atb)));
 					}
 					this.target_area.add(area);
 				} else {
 					// subtract
 					for (int i=0; i<xpd.length; i++) {
 						atb.setToTranslation((int)xpd[i], (int)ypd[i]); // always integers
 						atb.preConcatenate(inv_at);
 						target_area.subtract(slashInInts(brush.createTransformedArea(atb)));
 					}
 				}
 
 				} catch (Exception ee) {
 					IJError.print(ee);
 				}
 			}
 		}
 		final Area getTmpArea() {
 			if (area != target_area) return area;
 			return null;
 		}
 		/** For best smoothness, each mouse dragged event should be captured!*/
 		public void run() {
 			// create brush
 			Point p;
 			final AffineTransform atb = new AffineTransform();
 			while (paint) {
 				// detect mouse up
 				if (0 == (flags & leftClick)) {
 					quit();
 					return;
 				}
 				p = dc.getCursorLoc(); // as offscreen coords
 				if (p.equals(previous_p) /*|| (null != previous_p && p.distance(previous_p) < brush_size/5) */) {
 					try { Thread.sleep(1); } catch (InterruptedException ie) {}
 					continue;
 				}
 				// bring to offscreen position of the mouse
 				atb.translate(p.x, p.y);
 				// capture bounds while still in offscreen coordinates
 				final Rectangle r = new Rectangle();
 				final Area slash = createSlash(atb, r);
 				if(null == slash) continue;
 
 				if (0 == (flags & alt)) {
 					// no modifiers, just add
 					area.add(slash);
 				} else {
 					// with alt down, substract
 					area.subtract(slash);
 				}
 				points.add(p);
 				previous_p = p;
 
 				final Rectangle copy = (Rectangle)r.clone();
 				if (null != r_old) r.add(r_old);
 				r_old = copy;
 
 				Display.repaint(Display.getFrontLayer(), 3, r, false, false); // repaint only the last added slash
 
 				// reset
 				atb.setToIdentity();
 			}
 		}
 
 		/** Sets the bounds of the created slash, in offscreen coords, to r if r is not null. */
 		private Area createSlash(final AffineTransform atb, final Rectangle r) {
 				Area slash = brush.createTransformedArea(atb); // + int transform, no problem
 				if (null != r) r.setRect(slash.getBounds());
 				// bring to the current transform, if any
 				if (!at.isIdentity()) {
 					try {
 						slash = slash.createTransformedArea(at.createInverse());
 					} catch (NoninvertibleTransformException nite) {
 						IJError.print(nite);
 						return null;
 					}
 				}
 				// avoid problems with floating-point points, for example inability to properly fill areas or delete them.
 				return slashInInts(slash);
 		}
 
 		private final Area slashInInts(final Area area) {
 			int[] x = new int[400];
 			int[] y = new int[400];
 			int next = 0;
 			for (PathIterator pit = area.getPathIterator(null); !pit.isDone(); ) {
 				if (x.length == next) {
 					int[] x2 = new int[x.length + 200];
 					int[] y2 = new int[y.length + 200];
 					System.arraycopy(x, 0, x2, 0, x.length);
 					System.arraycopy(y, 0, y2, 0, y.length);
 					x = x2;
 					y = y2;
 				}
 				final float[] coords = new float[6];
 				int seg_type = pit.currentSegment(coords);
 				switch (seg_type) {
 					case PathIterator.SEG_MOVETO:
 					case PathIterator.SEG_LINETO:
 						x[next] = (int)coords[0];
 						y[next] = (int)coords[1];
 						break;
 					case PathIterator.SEG_CLOSE:
 						break;
 					default:
 						Utils.log2("WARNING: AreaList.slashInInts unhandled seg type.");
 						break;
 				}
 				pit.next();
 				if (pit.isDone()) break; // the loop
 				next++;
 			}
 			// resize back (now next is the length):
 			if (x.length == next) {
 				int[] x2 = new int[next];
 				int[] y2 = new int[next];
 				System.arraycopy(x, 0, x2, 0, next);
 				System.arraycopy(y, 0, y2, 0, next);
 				x = x2;
 				y = y2;
 			}
 			return new Area(new Polygon(x, y, next));
 		}
 
 		/** This method could get tones of improvement, which should be pumped upstream into ImageJ's RoiBrush class which is creating it at every while(true) {} iteration!!!
 		 * The returned area has its coordinates centered around 0,0
 		 */
 		private Area makeBrush(int diameter, double mag) {
 			if (diameter < 1) return null;
 			if (mag >= 1) return new Area(new OvalRoi(-diameter/2, -diameter/2, diameter, diameter).getPolygon());
 			// else, create a smaller brush and transform it up, i.e. less precise, less points to store -but precision matches what the eye sees, and allows for much better storage -less points.
 			int screen_diameter = (int)(diameter * mag);
 			if (0 == screen_diameter) return null; // can't paint at this mag with this diameter
 
 			Area brush = new Area(new OvalRoi(-screen_diameter/2, -screen_diameter/2, screen_diameter, screen_diameter).getPolygon());
 			// scale to world coordinates
 			AffineTransform at = new AffineTransform();
 			at.scale(1/mag, 1/mag);
 			return brush.createTransformedArea(at);
 
 
 			// smooth out edges
 			/*
 			Polygon pol = new OvalRoi(-diameter/2, -diameter/2, diameter, diameter).getPolygon();
 			Polygon pol2 = new Polygon();
 			// cheap and fast: skip every other point, since all will be square angles
 			for (int i=0; i<pol.npoints; i+=2) {
 				pol2.addPoint(pol.xpoints[i], pol.ypoints[i]);
 			}
 			return new Area(pol2);
 			// the above works nice, but then the fill and fill-remove don't work properly (there are traces in the edges)
 			// Needs a workround: before adding/substracting, enlarge the polygon to have square edges
 			*/
 		}
 	}
 
 	static public void exportDTD(StringBuffer sb_header, HashSet hs, String indent) {
 		String type = "t2_area_list";
 		if (hs.contains(type)) return;
 		hs.add(type);
 		sb_header.append(indent).append("<!ELEMENT t2_area_list (").append(Displayable.commonDTDChildren()).append(",t2_area)>\n");
 		Displayable.exportDTD(type, sb_header, hs, indent); // all ATTLIST of a Displayable
 		sb_header.append(indent).append("<!ATTLIST t2_area_list fill_paint NMTOKEN #REQUIRED>\n");
 		sb_header.append(indent).append("<!ELEMENT t2_area (t2_path)>\n")
 			 .append(indent).append("<!ATTLIST t2_area layer_id NMTOKEN #REQUIRED>\n")
 			 .append(indent).append("<!ELEMENT t2_path EMPTY>\n")
 			 .append(indent).append("<!ATTLIST t2_path d NMTOKEN #REQUIRED>\n")
 		;
 	}
 
 	public void exportXML(StringBuffer sb_body, String indent, Object any) {
 		sb_body.append(indent).append("<t2_area_list\n");
 		final String in = indent + "\t";
 		super.exportXML(sb_body, in, any);
 		sb_body.append(in).append("fill_paint=\"").append(fill_paint).append("\"\n");
 		String[] RGB = Utils.getHexRGBColor(color);
 		sb_body.append(in).append("style=\"stroke:none;fill-opacity:").append(alpha).append(";fill:#").append(RGB[0]).append(RGB[1]).append(RGB[2]).append(";\"\n");
 		sb_body.append(indent).append(">\n");
 		for (Iterator it = ht_areas.entrySet().iterator(); it.hasNext(); ) {
 			Map.Entry entry = (Map.Entry)it.next();
 			long lid = ((Long)entry.getKey()).longValue();
 			Area area = (Area)entry.getValue();
 			sb_body.append(in).append("<t2_area layer_id=\"").append(lid).append("\">\n");
 			exportArea(sb_body, in + "\t", area);
 			sb_body.append(in).append("</t2_area>\n");
 		}
 		super.restXML(sb_body, in, any);
 		sb_body.append(indent).append("</t2_area_list>\n");
 	}
 
 	/** Exports the given area as a list of SVG path elements with integers only. Only reads SEG_MOVETO, SEG_LINETO and SEG_CLOSE elements, all others ignored (but could be just as easily saved in the SVG path). */
 	private void exportArea(StringBuffer sb, String indent, Area area) {
 		// I could add detectors for straight lines and thus avoid saving so many points.
 		for (PathIterator pit = area.getPathIterator(null); !pit.isDone(); ) {
 			float[] coords = new float[6];
 			int seg_type = pit.currentSegment(coords);
 			int x0=0, y0=0;
 			switch (seg_type) {
 				case PathIterator.SEG_MOVETO:
 					//Utils.log2("SEG_MOVETO: " + coords[0] + "," + coords[1]); // one point
 					x0 = (int)coords[0];
 					y0 = (int)coords[1];
 					sb.append(indent).append("<t2_path d=\"M ").append(x0).append(" ").append(y0);
 					break;
 				case PathIterator.SEG_LINETO:
 					//Utils.log2("SEG_LINETO: " + coords[0] + "," + coords[1]); // one point
 					sb.append(" L ").append((int)coords[0]).append(" ").append((int)coords[1]);
 					break;
 				case PathIterator.SEG_CLOSE:
 					//Utils.log2("SEG_CLOSE");
 					// make a line to the first point
 					//sb.append(" L ").append(x0).append(" ").append(y0);
 					sb.append(" z\" />\n");
 					break;
 				default:
 					Utils.log2("WARNING: AreaList.exportArea unhandled seg type.");
 					break;
 			}
 			pit.next();
 			if (pit.isDone()) {
 				//Utils.log2("finishing");
 				return;
 			}
 		}
 	}
 	/** Exports the given area as a list of SVG path elements with integers only. Only reads SEG_MOVETO, SEG_LINETO and SEG_CLOSE elements, all others ignored (but could be just as easily saved in the SVG path). */
 	private void exportAreaT2(final StringBuffer sb, final String indent, final Area area) {
 		// I could add detectors for straight lines and thus avoid saving so many points.
 		for (PathIterator pit = area.getPathIterator(null); !pit.isDone(); ) {
 			float[] coords = new float[6];
 			int seg_type = pit.currentSegment(coords);
 			int x0=0, y0=0;
 			switch (seg_type) {
 				case PathIterator.SEG_MOVETO:
 					x0 = (int)coords[0];
 					y0 = (int)coords[1];
 					sb.append(indent).append("(path '(M ").append(x0).append(' ').append(y0);
 					break;
 				case PathIterator.SEG_LINETO:
 					sb.append(" L ").append((int)coords[0]).append(' ').append((int)coords[1]);
 					break;
 				case PathIterator.SEG_CLOSE:
 					// no need to make a line to the first point
 					sb.append(" z))\n");
 					break;
 				default:
 					Utils.log2("WARNING: AreaList.exportArea unhandled seg type.");
 					break;
 			}
 			pit.next();
 			if (pit.isDone()) {
 				return;
 			}
 		}
 	}
 
 	/** Returns an ArrayList of ArrayList of Point as value with all paths for the Area of the given layer_id. */
 	public ArrayList getPaths(long layer_id) {
 		Object ob = ht_areas.get(new Long(layer_id));
 		if (null == ob) return null;
 		if (AreaList.UNLOADED == ob) {
 			ob = loadLayer(layer_id);
 			if (null == ob) return null;
 		}
 		Area area = (Area)ob;
 		ArrayList al_paths = new ArrayList();
 		ArrayList al_points = null;
 		for (PathIterator pit = area.getPathIterator(null); !pit.isDone(); ) {
 			float[] coords = new float[6];
 			int seg_type = pit.currentSegment(coords);
 			switch (seg_type) {
 				case PathIterator.SEG_MOVETO:
 					al_points = new ArrayList();
 					al_points.add(new Point((int)coords[0], (int)coords[1]));
 					break;
 				case PathIterator.SEG_LINETO:
 					al_points.add(new Point((int)coords[0], (int)coords[1]));
 					break;
 				case PathIterator.SEG_CLOSE:
 					al_paths.add(al_points);
 					al_points = null;
 					break;
 				default:
 					Utils.log2("WARNING: AreaList.getPaths() unhandled seg type.");
 					break;
 			}
 			pit.next();
 			if (pit.isDone()) {
 				break;
 			}
 		}
 		return al_paths;
 	}
 
 	/** Returns a table of Long layer ids versus the ArrayList that getPaths(long) returns for it.*/
 	public HashMap getAllPaths() {
 		HashMap ht = new HashMap();
 		for (Iterator it = ht_areas.entrySet().iterator(); it.hasNext(); ) {
 			Map.Entry entry = (Map.Entry)it.next();
 			ht.put(entry.getKey(), getPaths(((Long)entry.getKey()).longValue()));
 		}
 		return ht;
 	}
 
 	/** These methods below prepended with double-underscore to mean: they are public, but should not be used except by the TMLHandler. I could put a caller detector but parsing XML code doesn't need any more overhead.*/
 
 	/** For reconstruction from XML. */
 	public void __startReconstructing(long lid) {
 		ht_areas.put("CURRENT", new Long(lid));
 		ht_areas.put(new Long(lid), new GeneralPath(GeneralPath.WIND_EVEN_ODD));
 	}
 	/** For reconstruction from XML. */
 	public void __addPath(String svg_path) {
 		GeneralPath gp = (GeneralPath)ht_areas.get(ht_areas.get("CURRENT"));
 		svg_path = svg_path.trim();
 		while (-1 != svg_path.indexOf("  ")) {
 			svg_path = svg_path.replaceAll("  "," "); // make all spaces be single
 		}
 		final char[] data = new char[svg_path.length()];
 		svg_path.getChars(0, data.length, data, 0);
 		parse(gp, data);
 	}
 
 	/** Assumes first char is 'M' and last char is a 'z'*/
 	private void parse(final GeneralPath gp, final char[] data) {
 		if ('z' != data[data.length-1]) {
 			Utils.log("AreaList: no closing z, ignoring sub path");
 			return;
 		}
 		data[data.length-1] = 'L'; // replacing the closing z for an L, since we read backwards
 		final int[] xy = new int[2];
 		int i_L = -1;
 		// find first L
 		for (int i=0; i<data.length; i++) {
 			if ('L' == data[i]) {
 				i_L = i;
 				break;
 			}
 		}
 		readXY(data, i_L, xy);
 		final int x0 = xy[0];
 		final int y0 = xy[1];
 		gp.moveTo(x0, y0);
 		int first = i_L+1;
 		while (-1 != (first = readXY(data, first, xy))) {
 			gp.lineTo(xy[0], xy[1]);
 		}
 
 		// close loop
 		gp.lineTo(x0, y0); //TODO unnecessary?
 		gp.closePath();
 	}
 
 	/** Assumes all read chars will be digits except for the separator (single white space char), and won't fail (but generate ugly results) when any char is not a digit. */
 	private final int readXY(final char[] data, int first, final int[] xy) { // final method: inline
 		if (first >= data.length) return -1;
 		int last = first;
 		char c = data[first];
 		while ('L' != c) {
 			last++;
 			if (data.length == last) return -1;
 			c = data[last];
 		}
 		first = last +2; // the first digit position after the found L, which will be the next first.
 
 		// skip the L and the white space separating <y> and L
 		last -= 2;
 		if (last < 0) return -1;
 		c = data[last];
 
 		// the 'y'
 		xy[1] = 0;
 		int pos = 1;
 		while (' ' != c) {
 			xy[1] += (((int)c) -48) * pos; // digit zero is char with int value 48
 			last--;
 			c = data[last];
 			pos *= 10;
 		}
 
 		// skip separating space
 		last--;
 
 		// the 'x'
 		c = data[last];
 		pos = 1;
 		xy[0] = 0;
 		while (' ' != c) {
 			xy[0] += (((int)c) -48) * pos;
 			last--;
 			c = data[last];
 			pos *= 10;
 		}
 		return first;
 	}
 	/** For reconstruction from XML. */
 	public void __endReconstructing() {
 		Object ob = ht_areas.get("CURRENT");
 		if (null != ob) {
 			GeneralPath gp = (GeneralPath)ht_areas.get((Long)ob);
 			ht_areas.put(ob, new Area(gp));
 			ht_areas.remove("CURRENT");
 		}
 	}
 
 	/** Detect if a point in offscreen coords is not in the area, but lays inside one of its path, which is returned as a Polygon. Otherwise returns null. The given x,y must be already in the Area's coordinate system. */
 	private Polygon findPath(Area area, int x, int y) {
 		Polygon pol = new Polygon();
 		for (PathIterator pit = area.getPathIterator(null); !pit.isDone(); ) {
 			float[] coords = new float[6];
 			int seg_type = pit.currentSegment(coords);
 			switch (seg_type) {
 				case PathIterator.SEG_MOVETO:
 				case PathIterator.SEG_LINETO:
 					pol.addPoint((int)coords[0], (int)coords[1]);
 					break;
 				case PathIterator.SEG_CLOSE:
 					if (pol.contains(x, y)) return pol;
 					// else check next
 					pol = new Polygon();
 					break;
 				default:
 					Utils.log2("WARNING: unhandled seg type.");
 					break;
 			}
 			pit.next();
 			if (pit.isDone()) {
 				break;
 			}
 		}
 		return null;
 	}
 
 	public void fillHoles(final Layer la) {
 		Object o = ht_areas.get(la.getId());
 		if (UNLOADED == o) o = loadLayer(la.getId());
 		if (null == o) return;
 		Area area = (Area) o;
 
 		Polygon pol = new Polygon();
 		for (PathIterator pit = area.getPathIterator(null); !pit.isDone(); ) {
 			float[] coords = new float[6];
 			int seg_type = pit.currentSegment(coords);
 			switch (seg_type) {
 				case PathIterator.SEG_MOVETO:
 				case PathIterator.SEG_LINETO:
 					pol.addPoint((int)coords[0], (int)coords[1]);
 					break;
 				case PathIterator.SEG_CLOSE:
 					area.add(new Area(pol));
 					// prepare next:
 					pol = new Polygon();
 					break;
 				default:
 					Utils.log2("WARNING: unhandled seg type.");
 					break;
 			}
 			pit.next();
 			if (pit.isDone()) {
 				break;
 			}
 		}
 	}
 
 	public boolean paintsAt(final Layer layer) {
 		if (!super.paintsAt(layer)) return false;
 		return null != ht_areas.get(new Long(layer.getId()));
 	}
 
 	/** Dynamic loading from the database. */
 	private Area loadLayer(final long layer_id) {
 		Area area = project.getLoader().fetchArea(this.id, layer_id);
 		if (null == area) return null;
 		ht_areas.put(new Long(layer_id), area);
 		return area;
 	}
 
 	public void adjustProperties() {
 		GenericDialog gd = makeAdjustPropertiesDialog(); // in superclass
 		gd.addCheckbox("Paint as outlines", !fill_paint);
 		gd.addCheckbox("Apply paint mode to all AreaLists", false);
 		gd.showDialog();
 		if (gd.wasCanceled()) return;
 		// superclass processing
 		final Displayable.DoEdit prev = processAdjustPropertiesDialog(gd);
 		// local proccesing
 		final boolean fp = !gd.getNextBoolean();
 		final boolean to_all = gd.getNextBoolean();
 		if (to_all) {
 			for (Iterator it = this.layer_set.getZDisplayables().iterator(); it.hasNext(); ) {
 				Object ob = it.next();
 				if (ob instanceof AreaList) {
 					AreaList ali = (AreaList)ob;
 					ali.fill_paint = fp;
 					ali.updateInDatabase("fill_paint");
 					Display.repaint(this.layer_set, ali, 2);
 				}
 			}
 		} else {
 			if (this.fill_paint != fp) {
 				prev.add("fill_paint", fp);
 				this.fill_paint = fp;
 				updateInDatabase("fill_paint");
 			}
 		}
 
 		// Add current step, with the same modified keys
 		DoEdit current = new DoEdit(this).init(prev);
 		if (isLinked()) current.add(new Displayable.DoTransforms().addAll(getLinkedGroup(null)));
 		getLayerSet().addEditStep(current);
 	}
 
 	public boolean isFillPaint() { return this.fill_paint; }
 
 	/** Merge all arealists contained in the ArrayList to the first one found, and remove the others from the project, and only if they belong to the same LayerSet. Returns the merged AreaList object. */
 	static public AreaList merge(final ArrayList al) {
 		AreaList base = null;
 		final ArrayList list = (ArrayList)al.clone();
 		for (Iterator it = list.iterator(); it.hasNext(); ) {
 			Object ob = it.next();
 			if (ob.getClass() != AreaList.class) it.remove();
 			if (null == base) {
 				base = (AreaList)ob;
 				if (null == base.layer_set) {
 					Utils.log2("AreaList.merge: null LayerSet for base AreaList.");
 					return null;
 				}
 				it.remove();
 			}
 			// check that it belongs to the same layer set as the base
 			AreaList ali = (AreaList)ob;
 			if (base.layer_set != ali.layer_set) it.remove();
 		}
 		if (list.size() < 1) return null; // nothing to fuse
 		if (!Utils.check("Merging AreaList has no undo. Continue?")) return null;
 		for (Iterator it = list.iterator(); it.hasNext(); ) {
 			// add to base
 			AreaList ali = (AreaList)it.next();
 			base.add(ali);
 			// remove from project
 			ali.project.removeProjectThing(ali, false, true, 1); // the node from the Project Tree
 			Utils.log2("Merged AreaList " + ali + " to base " + base);
 		}
 		// update
 		base.calculateBoundingBox();
 		// relink
 		base.linkPatches();
 
 		return base;
 	}
 
 	/** For each area that ali contains, add it to the corresponding area here.*/
 	private void add(AreaList ali) {
 		for (Iterator it = ali.ht_areas.entrySet().iterator(); it.hasNext(); ) {
 			Map.Entry entry = (Map.Entry)it.next();
 			Object ob_area = entry.getValue();
 			long lid = ((Long)entry.getKey()).longValue();
 			if (UNLOADED == ob_area) ob_area = ali.loadLayer(lid);
 			Area area = (Area)ob_area;
 			area = area.createTransformedArea(ali.at);
 			// now need to inverse transform it by this.at
 			try {
 				area = area.createTransformedArea(this.at.createInverse());
 			} catch (NoninvertibleTransformException nte) {
 				IJError.print(nte);
 				// do what?
 			}
 			Object this_area = this.ht_areas.get(entry.getKey());
 			if (UNLOADED == this_area) { this_area = loadLayer(lid); }
 			if (null == this_area) this.ht_areas.put(entry.getKey(), (Area)area.clone());
 			else ((Area)this_area).add(area);
 			updateInDatabase("points=" + ((Long)entry.getKey()).intValue());
 		}
 	}
 
 	/** How many layers does this object paint to. */
 	public int getNAreas() { return ht_areas.size(); }
 
 	public Area getArea(Layer la) {
 		if (null == la) return null;
 		return getArea(la.getId());
 	}
 	public Area getArea(long layer_id) {
 		Object ob = ht_areas.get(new Long(layer_id));
 		if (null != ob) {
 			if (UNLOADED == ob) ob = loadLayer(layer_id);
 			return (Area)ob;
 		}
 		return null;
 	}
 
 
 
 	/** Performs a deep copy of this object, without the links. */
 	public Displayable clone(final Project pr, final boolean copy_id) {
 		final ArrayList al_ul = new ArrayList();
 		for (Iterator it = ht_areas.keySet().iterator(); it.hasNext(); ) { // TODO WARNING the layer ids are wrong if the project is different or copy_id is false! Should lookup closest layer by Z ...
 			al_ul.add(new Long(((Long)it.next()).longValue())); // clones of the Long that wrap layer ids
 		}
 		final long nid = copy_id ? this.id : pr.getLoader().getNextId();
 		final AreaList copy = new AreaList(pr, nid, null != title ? title.toString() : null, width, height, alpha, this.visible, new Color(color.getRed(), color.getGreen(), color.getBlue()), this.visible, al_ul, (AffineTransform)this.at.clone());
 		for (Iterator it = copy.ht_areas.entrySet().iterator(); it.hasNext(); ) {
 			Map.Entry entry = (Map.Entry)it.next();
 			entry.setValue(((Area)this.ht_areas.get(entry.getKey())).clone());
 		}
 		return copy;
 	}
 
 	/** Will make the assumption that all layers have the same thickness as the first one found.
 	 *  @param scale The scaling of the entire universe, to limit the overall box
 	 *  @param resample The optimization parameter for marching cubes (i.e. a value of 2 will scale down to half, then apply marching cubes, then scale up by 2 the vertices coordinates).
 	 *  @return The List of triangles involved, specified as three consecutive vertices. A list of Point3f vertices.
 	 */
 	public List generateTriangles(double scale, int resample) {
 		// in the LayerSet, layers are ordered by Z already.
 		try {
 		if (resample <=0 ) {
 			resample = 1;
 			Utils.log2("Fixing zero or negative resampling value to 1.");
 		}
 		int n = getNAreas();
 		if (0 == n) return null;
 		final Rectangle r = getBoundingBox();
 		// remove translation from a copy of this Displayable's AffineTransform
 		final AffineTransform at_translate = new AffineTransform();
 		at_translate.translate(-r.x, -r.y);
 		final AffineTransform at2 = (AffineTransform)this.at.clone();
 		at2.preConcatenate(at_translate);
 		// incorporate resampling scaling into the transform
 		final AffineTransform atK = new AffineTransform();
 		//Utils.log("resample: " + resample + "  scale: " + scale);
 		final double K = (1.0 / resample) * scale; // 'scale' is there to limit gigantic universes
 		final Calibration cal = layer_set.getCalibrationCopy();
 		atK.scale(K, K);
 		at2.preConcatenate(atK);
 		//
 		ImageStack stack = null;
 		float z_first = 0;
 		double thickness = 1;
 		final int w = (int)Math.ceil(r.width * K);
 		final int h = (int)Math.ceil(r.height * K);
 
 		final TreeMap<Double,Layer> assigned = new TreeMap<Double,Layer>();
 
 		for (Iterator it = layer_set.getLayers().iterator(); it.hasNext(); ) {
 			if (0 == n) break; // no more areas to paint
 			final Layer la = (Layer)it.next();
 			Area area = getArea(la);
 			if (null != area) {
 				if (null == stack) {
 					//Utils.log2("0 - creating stack with  w,h : " + w + ", " + h);
 					stack = new ImageStack(w, h);
 					z_first = (float)la.getZ(); // z of the first layer
 					thickness = la.getThickness();
 					assigned.put((double)z_first, la);
 				} else {
 					assigned.put(z_first + thickness * stack.getSize(), la);
 					// the layer is added to the stack below
 				}
 				final ImageProcessor ip = new ByteProcessor(w, h);
 				//ip.setColor(Color.white);
 				ip.setValue(255); // same thing as ip.setColor
 				//final AffineTransform atK = new AffineTransform();
 				//atK.scale(K, K);
 				area = area.createTransformedArea(at2); //atK);
 				ShapeRoi roi = new ShapeRoi(area);
 				ip.setRoi(roi);
 				ip.fill(roi.getMask()); // argh, should be automatic!
 				stack.addSlice(la.getZ() + "", ip);
 
 
 				n--;
 
 			} else if (null != stack) {
 				// add a black slice
 				stack.addSlice(la.getZ() + "", new ByteProcessor(w, h));
 			}
 		}
 		// zero-pad stack
 		// No need anymore: MCTriangulator does it on its own now
 		stack = zeroPad(stack);
 
 		// Still, the MCTriangulator does NOT zero pad properly
 
 		ImagePlus imp = new ImagePlus("", stack);
 		imp.getCalibration().pixelWidth = cal.pixelWidth * scale;
 		imp.getCalibration().pixelHeight = cal.pixelHeight * scale;
 		imp.getCalibration().pixelDepth = thickness * scale; // no need to factor in resampling
 		//debug:
 		//imp.show();
 		//Utils.log2("Stack dimensions: " + imp.getWidth() + ", " + imp.getHeight() + ", " + imp.getStack().getSize());
 		// end of generating byte[] arrays
 		// Now marching cubes
 		final Triangulator tri = new MCTriangulator();
 		final List list = tri.getTriangles(imp, 0, new boolean[]{true, true, true}, 1);
 		// now translate all coordinates by x,y,z (it would be nice to simply assign them to a mesh object)
 		final float dx = (float)(r.x * scale * cal.pixelWidth);
 		final float dy = (float)(r.y * scale * cal.pixelHeight);
 		final float dz = (float)((z_first - thickness) * scale * cal.pixelWidth); // the z of the first layer found, corrected for both scale and the zero padding
 		final float rs = resample / (float)scale;
 		final float z_correction = (float)(thickness * scale * cal.pixelWidth);
 		for (final Iterator it = list.iterator(); it.hasNext(); ) {
 			final Point3f p = (Point3f)it.next();
 			// fix back the resampling (but not the universe scale, which has already been considered)
 			p.x *= rs; //resample / scale; // a resampling of '2' means 0.5  (I love inverted worlds..)
 			p.y *= rs; //resample / scale;
 			p.z *= cal.pixelWidth;
 			//Z was not resampled
 			// translate to the x,y,z coordinate of the object in space
 			p.x += dx - rs; // minus rs, as an offset for zero-padding
 			p.y += dy - rs;
 			p.z += dz + z_correction; // translate one complete section up. I don't fully understand why I need this, but this is correct.
 		}
 
 		// TODO: should capture vertices whose Z coordinate falls within a layer thickness, and translate that to the real layer Z and thickness (because now it's using the first layer thickness only).
 		// TODO: even before this, should enable interpolation when desired, since we have images anyway.
 		// TODO: and even better, when there is only one island per section, give the option to enable VectorStrin2D mesh creation like profiles.
 
 
 		// Check if any layer has an improper Z assigned. The assigned gives a list of entries sorted by Z
 		double last_real_z = 0;
 		HashMap<Integer,ArrayList<Point3f>> map = null;
 
 		for (final Map.Entry<Double,Layer> e : assigned.entrySet()) {
 			final double fake_z = e.getKey();
 			final double real_z = e.getValue().getZ();
 			final double fake_thickness = thickness;
 			final double real_thickness = e.getValue().getThickness();
 
 			if ( ! M.equals(fake_z, real_z) ) {
 				if (null == map) {
 					map = new HashMap<Integer,ArrayList<Point3f>>();
 					for (final Iterator it = list.iterator(); it.hasNext(); ) {
 						final Point3f p = (Point3f) it.next();
 						int lz = (int)( 0.0005 + (p.z - z_first) / thickness );
 						ArrayList<Point3f> az = map.get(lz);
 						if (null == az) {
 							az = new ArrayList<Point3f>();
 							map.put(lz, az);
 						}
 						az.add(p);
 					}
 
 					for (final Integer lz : new java.util.TreeSet<Integer>(map.keySet())) {
 						Utils.log2("Key: " + lz);
 					}
 				}
 				// must fix: and also accumulate the difference in the offset, for subsequent calls.
 				// find all coords between fake_z and fake_z + fake_thickness, and stretch them proportionally to real_z and real_z + real_thickness
 				int lz = (int)( 0.0005 + (fake_z - z_first) / thickness );
 				ArrayList<Point3f> az = map.get(lz);
 				if (null == az) {
 					Utils.log2("Something is WRONG: null az for lz = " + lz);
 					continue;
 				}
 				for (final Point3f p : az) {
 					p.z = (float) (real_z + real_thickness * ( (p.z - fake_z - z_first) / thickness ));
 				}
 
 				/*
 				for (final Iterator it = list.iterator(); it.hasNext(); ) {
 					final Point3f p = (Point3f) it.next();
 					if (p.z >= fake_z && p.z <= fake_z + thickness) {
 						// bring to real z + the proportion of the real thickness
 						p.z = offset + real_z + real_thickness * ( (p.z - fake_z - z_first) / thickness );
 					}
 				}
 				*/
 			}
 		}
 
 		return list;
 
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 		return null;
 	}
 
 	static private ImageStack zeroPad(final ImageStack stack) {
 		int w = stack.getWidth();
 		int h = stack.getHeight();
 		// enlarge all processors
 		ImageStack st = new ImageStack(w+2, h+2);
 		for (int i=1; i<=stack.getSize(); i++) {
 			ImageProcessor ip = new ByteProcessor(w+2, h+2);
 			ip.insert(stack.getProcessor(i), 1, 1);
 			st.addSlice(Integer.toString(i), ip);
 		}
 		ByteProcessor bp = new ByteProcessor(w+2, h+2);
 		// insert slice at 0
 		st.addSlice("0", bp, 0);
 		// append slice at the end
 		st.addSlice(Integer.toString(stack.getSize()+1), bp);
 
 		return st;
 	}
 
 	/** Directly place an Area for the specified layer. Keep in mind it will be added in this AreaList coordinate space, not the overall LayerSet coordinate space. Does not make it local, you should call calculateBoundingBox() after setting an area. */
 	public void setArea(final long layer_id, final Area area) {
 		if (null == area) return;
 		ht_areas.put(layer_id, area);
 		updateInDatabase("points=" + layer_id);
 	}
 
 	/** Add a copy of an Area object to the existing, if any, area object at Layer with layer_id as given, or if not existing, just set the copy as it. The area is expected in this AreaList coordinate space. Does not make it local, you should call calculateBoundingBox when done. */
 	public void addArea(final long layer_id, final Area area) {
 		if (null == area) return;
 		Area a = getArea(layer_id);
 		if (null == a) ht_areas.put(layer_id, new Area(area));
 		else a.add(area);
 		updateInDatabase("points=" + layer_id);
 	}
 
 	/** Adds the given ROI, which is expected in world/LayerSet coordinates, to the area present at Layer with id layer_id, or set it if none present yet. */
 	public void add(final long layer_id, final ShapeRoi roi) throws NoninvertibleTransformException{
 		if (null == roi) return;
 		Area a = getArea(layer_id);
 		Area asr = Utils.getArea(roi).createTransformedArea(this.at.createInverse());
 		if (null == a) {
 			ht_areas.put(layer_id, asr);
 		} else {
 			a.add(asr);
 		}
 		calculateBoundingBox();
 		updateInDatabase("points=" + layer_id);
 	}
 	/** Subtracts the given ROI, which is expected in world/LayerSet coordinates, to the area present at Layer with id layer_id, or set it if none present yet. */
 	public void subtract(final long layer_id, final ShapeRoi roi) throws NoninvertibleTransformException {
 		if (null == roi) return;
 		Area a = getArea(layer_id);
 		if (null == a) return;
 		a.subtract(Utils.getArea(roi).createTransformedArea(this.at.createInverse()));
 		calculateBoundingBox();
 		updateInDatabase("points=" + layer_id);
 	}
 
 	/** Subtracts the given ROI, and then creates a new AreaList with identical properties and the content of the subtracted part. Returns null if there is no intersection between sroi and the Area for layer_id. */
 	public AreaList part(final long layer_id, final ShapeRoi sroi) throws NoninvertibleTransformException {
 		// The Area to subtract, in world coordinates:
 		Area sub = Utils.getArea(sroi);
 		// The area to subtract from:
 		Area a = getArea(layer_id);
 		if (null == a || Utils.isEmpty(a)) return null;
 		// The intersection:
 		Area inter = a.createTransformedArea(this.at);
 		inter.intersect(sub);
 		if (Utils.isEmpty(inter)) return null;
 
 		// Subtract from this:
 		this.subtract(layer_id, sroi);
 
 		// Create new AreaList with the intersection area, and add it to the same LayerSet as this:
 		AreaList ali = new AreaList(this.project, this.title, 0, 0);
 		ali.color = new Color(color.getRed(), color.getGreen(), color.getBlue());
 		ali.visible = this.visible;
 		ali.alpha = this.alpha;
 		ali.addArea(layer_id, inter);
 		this.layer_set.add(ali); // needed to call updateBucket
 		ali.calculateBoundingBox();
 
 		return ali;
 	}
 
 	public void keyPressed(KeyEvent ke) {
 		Object source = ke.getSource();
 		if (! (source instanceof DisplayCanvas)) return;
 		DisplayCanvas dc = (DisplayCanvas)source;
 		Layer la = dc.getDisplay().getLayer();
 		int keyCode = ke.getKeyCode();
 
 		try {
 			switch (keyCode) {
 				case KeyEvent.VK_C: // COPY
 					Area area = (Area) ht_areas.get(la.getId());
 					if (null != area) {
 						DisplayCanvas.setCopyBuffer(AreaList.class, area.createTransformedArea(this.at));
 					}
 					ke.consume();
 					return;
 				case KeyEvent.VK_V: // PASTE
 					// Casting a null is fine, and addArea survives a null.
 					Area a = (Area) DisplayCanvas.getCopyBuffer(AreaList.class);
 					if (null != a) {
 						addArea(la.getId(), a.createTransformedArea(this.at.createInverse()));
 						calculateBoundingBox();
 					}
 					ke.consume();
 					return;
 				case KeyEvent.VK_F: // fill all holes
 					fillHoles(la);
 					ke.consume();
 					return;
 			}
 		} catch (Exception e) {
 			IJError.print(e);
 		} finally {
 			if (ke.isConsumed()) {
 				Display.repaint(la, getBoundingBox(), 5);
 				linkPatches();
 				return;
 			}
 		}
 
 		Roi roi = dc.getFakeImagePlus().getRoi();
 		if (null == roi) return;
 		// Check ROI
 		if (!Utils.isAreaROI(roi)) {
 			Utils.log("AreaList only accepts region ROIs, not lines.");
 			return;
 		}
 		ShapeRoi sroi = new ShapeRoi(roi);
 		long layer_id = la.getId();
 		try {
 			switch (keyCode) {
 				case KeyEvent.VK_A:
 					add(layer_id, sroi);
 					ke.consume();
 					break;
 				case KeyEvent.VK_D: // VK_S is for 'save' always
 					subtract(layer_id, sroi);
 					ke.consume();
 					break;
 				case KeyEvent.VK_K: // knive
 					AreaList p = part(layer_id, sroi);
 					if (null != p) {
 						project.getProjectTree().addSibling(this, p);
 					}
 					ke.consume();
 			}
 			Display.repaint(la, getBoundingBox(), 5);
 			linkPatches();
 		} catch (NoninvertibleTransformException e) {
 			Utils.log("Could not add ROI to area at layer " + dc.getDisplay().getLayer() + " : " + e);
 		}
 	}
 
 	/** Measure the volume (in voxels) of this AreaList,
 	 *  and the surface area, the latter estimated as the number of voxels that
 	 *  make the outline.
 	 *  
 	 *  If the width and height of this AreaList cannot be fit in memory, scaled down versions will be used,
 	 *  and thus the results will be approximate.
 	 *
 	 *  */
 	public String getInfo() {
 		if (0 == ht_areas.size()) return "Empty AreaList " + this.toString();
 		Rectangle box = getBoundingBox(null);
 		float scale = 1.0f;
 		while (!getProject().getLoader().releaseToFit(2 * (long)(scale * (box.width * box.height)) + 1000000)) { // factor of 2, because a mask will be involved
 			scale /= 2;
 		}
 		double volume = 0;
 		double surface = 0;
 		final int w = (int)Math.ceil(box.width * scale);
 		final int h = (int)Math.ceil(box.height * scale);
 		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
 		Graphics2D g = bi.createGraphics();
 		//DataBufferByte buffer = (DataBufferByte)bi.getRaster().getDataBuffer();
 		byte[] pixels = ((DataBufferByte)bi.getRaster().getDataBuffer()).getData(); // buffer.getData();
 
 		// prepare suitable transform
 		AffineTransform aff = (AffineTransform)this.at.clone();
 		AffineTransform aff2 = new AffineTransform();
 		//  A - remove translation
 		aff2.translate(-box.x, -box.y);
 		aff.preConcatenate(aff2);
 		//  B - scale
 		if (1.0f != scale) {
 			aff2.setToIdentity();
 			aff2.translate(box.width/2, box.height/2);
 			aff2.scale(scale, scale);
 			aff2.translate(-box.width/2, -box.height/2);
 			aff.preConcatenate(aff2);
 		}
 		// for each area, measure its area and its perimeter
 		for (Iterator it = ht_areas.entrySet().iterator(); it.hasNext(); ) {
 			// fetch Area
 			Map.Entry entry = (Map.Entry)it.next();
 			Object ob_area = entry.getValue();
 			long lid = ((Long)entry.getKey()).longValue();
 			if (UNLOADED == ob_area) ob_area = loadLayer(lid);
 			Area area2 = ((Area)ob_area).createTransformedArea(aff);
 			// paint the area, filling mode
 			g.setColor(Color.white);
 			g.fill(area2);
 			double n_pix = 0;
 			// count white pixels
 			for (int i=0; i<pixels.length; i++) {
 				if (255 == (pixels[i]&0xff)) n_pix++;
 				// could set the pixel to 0, but I have no idea if that holds properly (or is fast at all) in automatically accelerated images
 			}
 			// debug: show me
 			// new ImagePlus("lid=" + lid, bi).show();
 			//
 
 			double thickness = layer_set.getLayer(lid).getThickness();
 			volume += n_pix * thickness;
 			// reset board (filling all, to make sure there are no rounding surprises)
 			g.setColor(Color.black);
 			g.fillRect(0, 0, w, h);
 			// now measure length of perimeter
 			ArrayList al_paths = getPaths(lid);
 			double length = 0;
 			for (Iterator ipath = al_paths.iterator(); ipath.hasNext(); ) {
 				ArrayList path = (ArrayList)ipath.next();
 				Point p2 = (Point)path.get(0);
 				for (int i=path.size()-1; i>-1; i--) {
 					Point p1 = (Point)path.get(i);
 					length += p1.distance(p2);
 					p1 = p2;
 				}
 			}
 			surface += length * thickness;
 		}
 		// cleanup
 		pixels = null;
 		g = null;
 		bi.flush();
 		// correct scale
 		volume /= scale;
 		surface /= scale;
 		// remove pretentious after-comma digits on return:
 		return new StringBuffer("Volume: ").append(IJ.d2s(volume, 2)).append(" (cubic pixels)\nLateral surface: ").append(IJ.d2s(surface, 2)).append(" (square pixels)\n").toString();
 	}
 
 	/** @param area is expected in world coordinates. */
 	public boolean intersects(final Area area, final double z_first, final double z_last) {
 		for (Iterator<Map.Entry> it = ht_areas.entrySet().iterator(); it.hasNext(); ) {
 			Map.Entry entry = it.next();
 			Layer layer = layer_set.getLayer(((Long)entry.getKey()).longValue());
 			if (layer.getZ() >= z_first && layer.getZ() <= z_last) {
 				Area a = ((Area)entry.getValue()).createTransformedArea(this.at);
 				a.intersect(area);
 				Rectangle r = a.getBounds();
 				if (0 != r.width && 0 != r.height) return true;
 			}
 		}
 		return false;
 	}
 
 	/** Export all given AreaLists as one per pixel value, what is called a "labels" file; a file dialog is offered to save the image as a tiff stack. */
 	static public void exportAsLabels(final java.util.List<Displayable> list, final ij.gui.Roi roi, final float scale, int first_layer, int last_layer, final boolean visible_only, final boolean to_file, final boolean as_amira_labels) {
 		// survive everything:
 		if (null == list || 0 == list.size()) {
 			Utils.log("Null or empty list.");
 			return;
 		}
 		if (scale < 0 || scale > 1) {
 			Utils.log("Improper scale value. Must be 0 < scale <= 1");
 			return;
 		}
 
 		// Current AmiraMeshEncoder supports ByteProcessor only: 256 labels max, including background at zero.
 		if (as_amira_labels && list.size() > 255) {
 			Utils.log("Saving ONLY first 255 AreaLists!\nDiscarded:");
 			StringBuffer sb = new StringBuffer();
 			for (final Displayable d : list.subList(256, list.size())) {
 				sb.append("    ").append(d.getProject().getShortMeaningfulTitle(d)).append('\n');
 			}
 			Utils.log(sb.toString());
 			ArrayList<Displayable> li = new ArrayList<Displayable>(list);
 			list.clear();
 			list.addAll(li.subList(0, 256));
 		}
 
 		String path = null;
 		if (to_file) {
 			String ext = as_amira_labels ? ".am" : ".tif";
 			File f = Utils.chooseFile("labels", ext);
 			if (null == f) return;
 			path = f.getAbsolutePath().replace('\\','/');
 		}
 
 		LayerSet layer_set = list.get(0).getLayerSet();
 		if (first_layer > last_layer) {
 			int tmp = first_layer;
 			first_layer = last_layer;
 			last_layer = tmp;
 			if (first_layer < 0) first_layer = 0;
 			if (last_layer >= layer_set.size()) last_layer = layer_set.size()-1;
 		}
 		// Create image according to roi and scale
 		int width, height;
 		Rectangle broi = null;
 		if (null == roi) {
 			width = (int)(layer_set.getLayerWidth() * scale);
 			height = (int)(layer_set.getLayerHeight() * scale);
 		} else {
 			broi = roi.getBounds();
 			width = (int)(broi.width * scale);
 			height = (int)(broi.height * scale);
 		}
 
 		// Compute highest label value, which affects of course the stack image type
 		TreeSet<Integer> label_values = new TreeSet<Integer>();
 		for (final Displayable d : list) {
 			String label = d.getProperty("label");
 			if (null != label) label_values.add(Integer.parseInt(label));
 		}
 		int lowest = 0;
 		int highest = 0;
 		if (label_values.size() > 0) {
 			lowest = label_values.first();
 			highest = label_values.last();
 		}
 		int n_non_labeled = list.size() - label_values.size();
 		int max_label_value = highest + n_non_labeled;
 
 		final ImageStack stack = new ImageStack(width, height);
 		// processor type:
 		int type = ImagePlus.GRAY8;
 		if (max_label_value > 255) { // 0 is background, and 255 different arealists
 			type = ImagePlus.GRAY16;
 			if (max_label_value > 65535) { // 0 is background, and 65535 different arealists
 				type = ImagePlus.GRAY32;
 			}
 		}
 		Calibration cal = layer_set.getCalibration();
 
 		String amira_params = null;
 		if (as_amira_labels) {
 			final StringBuffer sb = new StringBuffer("CoordType \"uniform\"\nMaterials {\nExterior {\n Id 0,\nColor 0 0 0\n}\n");
 			final float[] c = new float[3];
 			int value = 0;
 			for (final Displayable d : list) {
 				value++; // 0 is background
 				d.getColor().getRGBColorComponents(c);
 				String s = d.getProject().getShortMeaningfulTitle(d);
 				s = s.replace('-', '_').replaceAll(" #", " id");
 				sb.append(Utils.makeValidIdentifier(s)).append(" {\n")
 				  .append("Id ").append(value).append(",\n")
 				  .append("Color ").append(c[0]).append(' ').append(c[1]).append(' ').append(c[2]).append("\n}\n");
 			}
 			sb.append("}\n");
 			amira_params = sb.toString();
 		}
 
 		int count = 1;
 		final float len = last_layer - first_layer + 1;
 
 		for (Layer la : layer_set.getLayers().subList(first_layer, last_layer+1)) {
 			Utils.showProgress(count/len);
 			count++;
 			ImageProcessor ip = Utils.createProcessor(type, width, height);
 			if (!(ip instanceof ByteProcessor)) {
 				ip.setMinAndMax(lowest, highest);
 			}
 			// paint here all arealist that paint to the layer 'la'
			int value = 0;
 			for (final Displayable d : list) {
				value++; // zero is background
				int label = value;
				String slabel = d.getProperty("label");
				if (null != slabel) {
					label = Integer.parseInt(slabel);
				} else {
					label = (++highest);
				}
				ip.setValue(label);
 				if (visible_only && !d.isVisible()) continue;
 				AreaList ali = (AreaList)d;
 				Area area = ali.getArea(la);
				if (null == area) continue;
 				// Transform: the scale and the roi
 				AffineTransform aff = new AffineTransform();
 				// reverse order of transformations:
 				/* 3 - To scale: */ if (1 != scale) aff.scale(scale, scale);
 				/* 2 - To roi coordinates: */ if (null != broi) aff.translate(-broi.x, -broi.y);
 				/* 1 - To world coordinates: */ aff.concatenate(ali.at);
 				ShapeRoi sroi = new ShapeRoi(aff.createTransformedShape(area));
 				ip.setRoi(sroi);
 				ip.fill(sroi.getMask());
 			}
 			stack.addSlice(la.getZ() * cal.pixelWidth + "", ip);
 		}
 		Utils.showProgress(1);
 		// Save via file dialog:
 		ImagePlus imp = new ImagePlus("Labels", stack); 
 		if (as_amira_labels) imp.setProperty("Info", amira_params);
 		imp.setCalibration(layer_set.getCalibrationCopy());
 		if (to_file) {
 			if (as_amira_labels) {
 				AmiraMeshEncoder ame = new AmiraMeshEncoder(path);
 				if (!ame.open()) {
 					Utils.log("Could not write to file " + path);
 					return;
 				}
 				if (!ame.write(imp)) {
 					Utils.log("Error in writing Amira file!");
 					return;
 				}
 			} else {
 				new FileSaver(imp).saveAsTiff(path);
 			}
 		} else imp.show();
 	}
 
 	public ResultsTable measure(ResultsTable rt) {
 		if (0 == ht_areas.size()) return rt;
 		if (null == rt) rt = Utils.createResultsTable("AreaList results", new String[]{"id", "volume", "name-id"});
 		rt.incrementCounter();
 		rt.addLabel("units", "cubic " + layer_set.getCalibration().getUnit());
 		rt.addValue(0, this.id);
 		rt.addValue(1, measureVolume());
 		rt.addValue(2, getNameId());
 		return rt;
 	}
 
 	public double measureVolume() {
 		if (0 == ht_areas.size()) return 0;
 		Rectangle box = getBoundingBox(null);
 		float scale = 1.0f;
 		while (!getProject().getLoader().releaseToFit(2 * (long)(scale * (box.width * box.height)) + 1000000)) { // factor of 2, because a mask will be involved
 			scale /= 2;
 		}
 		double volume = 0;
 		double surface = 0;
 		final int w = (int)Math.ceil(box.width * scale);
 		final int h = (int)Math.ceil(box.height * scale);
 		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
 		Graphics2D g = bi.createGraphics();
 		//DataBufferByte buffer = (DataBufferByte)bi.getRaster().getDataBuffer();
 		byte[] pixels = ((DataBufferByte)bi.getRaster().getDataBuffer()).getData(); // buffer.getData();
 
 		// prepare suitable transform
 		AffineTransform aff = (AffineTransform)this.at.clone();
 		AffineTransform aff2 = new AffineTransform();
 		//  A - remove translation
 		aff2.translate(-box.x, -box.y);
 		aff.preConcatenate(aff2);
 		//  B - scale
 		if (1.0f != scale) {
 			aff2.setToIdentity();
 			aff2.translate(box.width/2, box.height/2);
 			aff2.scale(scale, scale);
 			aff2.translate(-box.width/2, -box.height/2);
 			aff.preConcatenate(aff2);
 		}
 		Calibration cal = layer_set.getCalibration();
 		double pixelWidth = cal.pixelWidth;
 		double pixelHeight = cal.pixelHeight;
 		// for each area, measure its area and its perimeter
 		for (Iterator it = ht_areas.entrySet().iterator(); it.hasNext(); ) {
 			// fetch Area
 			Map.Entry entry = (Map.Entry)it.next();
 			Object ob_area = entry.getValue();
 			long lid = ((Long)entry.getKey()).longValue();
 			if (UNLOADED == ob_area) ob_area = loadLayer(lid);
 			Area area2 = ((Area)ob_area).createTransformedArea(aff);
 			// paint the area, filling mode
 			g.setColor(Color.white);
 			g.fill(area2);
 			double n_pix = 0;
 			// count white pixels
 			for (int i=0; i<pixels.length; i++) {
 				if (255 == (pixels[i]&0xff)) n_pix++;
 				// could set the pixel to 0, but I have no idea if that holds properly (or is fast at all) in automatically accelerated images
 			}
 			double thickness = layer_set.getLayer(lid).getThickness();
 			volume += n_pix * thickness * pixelWidth * pixelHeight * pixelWidth; // the last one is NOT pixelDepth because layer thickness and Z are in pixels
 			// reset board (filling all, to make sure there are no rounding surprises)
 			g.setColor(Color.black);
 			g.fillRect(0, 0, w, h);
 		}
 		// cleanup
 		pixels = null;
 		g = null;
 		bi.flush();
 		// correct scale
 		return volume /= (scale * scale); // above, calibration is fixed while computing. Scale only corrects for the 2D plane.
 	}
 
 	@Override
 	Class getInternalDataPackageClass() {
 		return DPAreaList.class;
 	}
 
 	@Override
 	Object getDataPackage() {
 		// The width,height,links,transform and list of areas
 		return new DPAreaList(this);
 	}
 
 	static private final class DPAreaList extends Displayable.DataPackage {
 		final protected HashMap ht;
 		DPAreaList(final AreaList ali) {
 			super(ali);
 			this.ht = new HashMap();
 			for (final Object entry : ali.ht_areas.entrySet()) {
 				Map.Entry e = (Map.Entry)entry;
 				Object area = e.getValue();
 				if (area.getClass() == Area.class) area = new Area((Area)area);
 				this.ht.put(e.getKey(), area);
 			}
 		}
 		final boolean to2(final Displayable d) {
 			super.to1(d);
 			final AreaList ali = (AreaList)d;
 			ali.ht_areas.clear();
 			for (final Object entry : ht.entrySet()) {
 				final Map.Entry e = (Map.Entry)entry;
 				Object area = e.getValue();
 				if (area.getClass() == Area.class) area = new Area((Area)area);
 				ali.ht_areas.put(e.getKey(), area);
 			}
 			return true;
 		}
 	}
 }
