 /**
 
 TrakEM2 plugin for ImageJ(C).
 Copyright (C) 2005, 2006 Albert Cardona and Rodney Douglas.
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 /s published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 
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
 
 package ini.trakem2.persistence;
 
 import ij.IJ;
 import ij.ImagePlus;
 import ij.VirtualStack;
 import ij.io.*;
 import ij.process.ByteProcessor;
 import ij.process.ImageProcessor;
 import ij.process.FloatProcessor;
 import ij.process.ColorProcessor;
 import ini.trakem2.Project;
 import ini.trakem2.ControlWindow;
 import ini.trakem2.display.DLabel;
 import ini.trakem2.display.Display;
 import ini.trakem2.display.Layer;
 import ini.trakem2.display.Patch;
 import ini.trakem2.display.YesNoDialog;
 import ini.trakem2.utils.*;
 import ini.trakem2.io.*;
 import ini.trakem2.imaging.FloatProcessorT2;
 
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.awt.image.BufferedImage;
 import java.awt.image.IndexColorModel;
 import java.awt.image.ColorModel;
 import java.awt.image.PixelGrabber;
 import java.awt.RenderingHints;
 import java.awt.geom.Area;
 import java.awt.geom.AffineTransform;
 import java.io.BufferedInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FilenameFilter;
 import java.io.InputStream;
 import java.util.*;
 
 import javax.swing.JMenuItem;
 import javax.swing.JMenu;
 import java.awt.event.ActionListener;
 import java.awt.event.ActionEvent;
 import java.awt.event.KeyEvent;
 import javax.swing.KeyStroke;
 
 import org.xml.sax.InputSource;
 
 import javax.xml.parsers.SAXParserFactory;
 import javax.xml.parsers.SAXParser;
 
 import mpi.fruitfly.math.datastructures.FloatArray2D;
 import mpi.fruitfly.registration.ImageFilter;
 import mpi.fruitfly.general.MultiThreading;
 
 import java.util.concurrent.atomic.AtomicInteger;
 
 
 /** A class to rely on memory only; except images which are rolled from a folder or their original location and flushed when memory is needed for more. Ideally there would be a given folder for storing items temporarily of permanently as the "project folder", but I haven't implemented it. */
 public final class FSLoader extends Loader {
 
 	/** Largest id seen so far. */
 	private long max_id = -1;
 	private final HashMap<Long,String> ht_paths = new HashMap<Long,String>();
 	/** For saving and overwriting. */
 	private String project_file_path = null;
 	/** Path to the directory hosting the file image pyramids. */
 	private String dir_mipmaps = null;
 	/** Path to the directory the user provided when creating the project. */
 	private String dir_storage = null;
 	/** Path to the directory hosting the alpha masks. */
 	private String dir_masks = null;
 
 	/** Path to dir_storage + "trakem2.images/" */
 	private String dir_image_storage = null;
 
 	/** Queue and execute Runnable tasks. */
 	static private Dispatcher dispatcher = new Dispatcher();
 
 	private Vector<Patch> touched_mipmaps = new Vector<Patch>();
 
 	/** Used to open a project from an existing XML file. */
 	public FSLoader() {
 		super(); // register
 		super.v_loaders.remove(this); //will be readded on successful open
 	}
 
 	/** Used to create a new project, NOT from an XML file. */
 	public FSLoader(final String storage_folder) {
 		this();
 		if (null == storage_folder) this.dir_storage = super.getStorageFolder(); // home dir
 		else this.dir_storage = storage_folder;
 		if (!this.dir_storage.endsWith("/")) this.dir_storage += "/";
 		if (!Loader.canReadAndWriteTo(dir_storage)) {
 			Utils.log("WARNING can't read/write to the storage_folder at " + dir_storage);
 		} else {
 			createMipMapsDir(this.dir_storage);
 			crashDetector();
 		}
 	}
 
 	/** Create a new FSLoader copying some key parameters such as preprocessor plugin, and storage and mipmap folders. Used for creating subprojects. */
 	public FSLoader(final Loader source) {
 		this();
 		this.dir_storage = source.getStorageFolder(); // can never be null
 		this.dir_mipmaps = source.getMipMapsFolder();
 		if (null == this.dir_mipmaps) createMipMapsDir(this.dir_storage);
 		setPreprocessor(source.getPreprocessor());
 	}
 
 	/** Store a hidden file in trakem2.mipmaps directory that means: "the project is open", which is deleted when the project is closed. If the file is present on opening a project, it means the project has not been closed properly, and some mipmaps may be wrong. */
 	private void crashDetector() {
 		if (null == dir_mipmaps) {
 			Utils.log2("Could NOT create crash detection system: null dir_mipmaps.");
 			return;
 		}
 		File f = new File(dir_mipmaps + ".open.t2");
 		Utils.log2("Crash detector file is " + dir_mipmaps + ".open.t2");
 		try {
 			if (f.exists()) {
 				// crashed!
 				askAndExecMipmapRegeneration("TrakEM detected a crash!");
 			} else {
 				if (!f.createNewFile() && !dir_mipmaps.startsWith("http:")) {
 					Utils.showMessage("WARNING: could NOT create crash detection system:\nCannot write to mipmaps folder.");
 				} else {
 					Utils.log2("Created crash detection system.");
 				}
 			}
 		} catch (Exception e) {
 			Utils.log2("Crash detector error:" + e);
 			IJError.print(e);
 		}
 	}
 
 	public String getProjectXMLPath() {
 		if (null == project_file_path) return null;
 		return project_file_path.toString(); // a copy of it
 	}
 
 	public String getStorageFolder() {
 		if (null == dir_storage) return super.getStorageFolder(); // the user's home
 		return dir_storage.toString(); // a copy
 	}
 
 	/** Returns a folder proven to be writable for images can be stored into. */
 	public String getImageStorageFolder() {
 		if (null == dir_image_storage) {
 			String s = getStorageFolder() + "trakem2.images/";
 			File f = new File(s);
 			if (f.exists() && f.isDirectory() && f.canWrite()) {
 				dir_image_storage = s;
 				return dir_image_storage;
 			}
 			else {
 				try {
 					f.mkdirs();
 					dir_image_storage = s;
 				} catch (Exception e) {
 					e.printStackTrace();
 					return getStorageFolder(); // fall back
 				}
 			}
 		}
 		return dir_image_storage;
 	}
 
 	/** Returns TMLHandler.getProjectData() . If the path is null it'll be asked for. */
 	public Object[] openFSProject(String path, final boolean open_displays) {
 		// clean path of double-slashes, safely (and painfully)
 		if (null != path) {
 			path = path.replace('\\','/');
 			path = path.trim();
 			int itwo = path.indexOf("//");
 			while (-1 != itwo) {
 				if (0 == itwo /* samba disk */
 				||  (5 == itwo && "http:".equals(path.substring(0, 5)))) {
 					// do nothing
 				} else {
 					path = path.substring(0, itwo) + path.substring(itwo+1);
 				}
 				itwo = path.indexOf("//", itwo+1);
 			}
 		}
 		//
 		if (null == path) {
 			String user = System.getProperty("user.name");
 			OpenDialog od = new OpenDialog("Select Project", OpenDialog.getDefaultDirectory(), null);
 			String file = od.getFileName();
 			if (null == file || file.toLowerCase().startsWith("null")) return null;
 			String dir = od.getDirectory().replace('\\', '/');
 			if (!dir.endsWith("/")) dir += "/";
 			this.project_file_path = dir + file;
 			Utils.log2("project file path 1: " + this.project_file_path);
 		} else {
 			this.project_file_path = path;
 			Utils.log2("project file path 2: " + this.project_file_path);
 		}
 		Utils.log2("Loader.openFSProject: path is " + path);
 		// check if any of the open projects uses the same file path, and refuse to open if so:
 		if (null != FSLoader.getOpenProject(project_file_path, this)) {
 			Utils.showMessage("The project is already open.");
 			return null;
 		}
 
 		Object[] data = null;
 
 		// parse file, according to expected format as indicated by the extension:
 		if (this.project_file_path.toLowerCase().endsWith(".xml")) {
 			InputStream i_stream = null;
 			TMLHandler handler = new TMLHandler(this.project_file_path, this);
 			if (handler.isUnreadable()) {
 				handler = null;
 			} else {
 				try {
 					SAXParserFactory factory = SAXParserFactory.newInstance();
 					factory.setValidating(true);
 					SAXParser parser = factory.newSAXParser();
 					if (isURL(this.project_file_path)) {
 						i_stream = new java.net.URL(this.project_file_path).openStream();
 					} else {
 						i_stream = new BufferedInputStream(new FileInputStream(this.project_file_path));
 					}
 					InputSource input_source = new InputSource(i_stream);
 					setMassiveMode(true);
 					parser.parse(input_source, handler);
 				} catch (java.io.FileNotFoundException fnfe) {
 					Utils.log("ERROR: File not found: " + path);
 					handler = null;
 				} catch (Exception e) {
 					IJError.print(e);
 					handler = null;
 				} finally {
 					setMassiveMode(false);
 					if (null != i_stream) {
 						try {
 							i_stream.close();
 						} catch (Exception e) {
 							IJError.print(e);
 						}
 					}
 				}
 			}
 			if (null == handler) {
 				Utils.showMessage("Error when reading the project .xml file.");
 				return null;
 			}
 
 			data = handler.getProjectData(open_displays);
 		}
 
 		if (null == data) {
 			Utils.showMessage("Error when parsing the project .xml file.");
 			return null;
 		}
 		// else, good
 		super.v_loaders.add(this);
 		crashDetector();
 		return data;
 	}
 
 	// Only one thread at a time may access this method.
 	synchronized static private final Project getOpenProject(final String project_file_path, final Loader caller) {
 		if (null == v_loaders) return null;
 		final Loader[] lo = (Loader[])v_loaders.toArray(new Loader[0]); // atomic way to get the list of loaders
 		for (int i=0; i<lo.length; i++) {
 			if (lo[i].equals(caller)) continue;
 			if (lo[i] instanceof FSLoader && ((FSLoader)lo[i]).project_file_path.equals(project_file_path)) {
 				return Project.findProject(lo[i]);
 			}
 		}
 		return null;
 	}
 
 	static public final Project getOpenProject(final String project_file_path) {
 		return getOpenProject(project_file_path, null);
 	}
 
 	public boolean isReady() {
 		return null != ht_paths;
 	}
 
 	public void destroy() {
 		super.destroy();
 		Utils.showStatus("", false);
 		// delete mipmap files that where touched and not cleared as saved (i.e. the project was not saved)
 		for (final Patch p : touched_mipmaps) {
 			File f = new File(getAbsolutePath(p));
 			Utils.log2("File f is " + f);
 			if (f.exists()) {
 				Utils.log2("Removing mipmaps for " + p);
 				// Cannot run in the dispatcher: is a daemon, and would be interrupted.
 				removeMipMaps(f.getName() + "." + p.getId() + ".jpg", (int)p.getWidth(), (int)p.getHeight()); // needs the dispatcher!
 			}
 		}
 		//
 		dispatcher.quit();
 		// remove empty trakem2.mipmaps folder if any
 		if (null != dir_mipmaps && !dir_mipmaps.equals(dir_storage)) {
 			File f = new File(dir_mipmaps);
 			if (f.isDirectory() && 0 == f.list(new FilenameFilter() {
 				public boolean accept(File fdir, String name) {
 					File file = new File(dir_mipmaps + name);
 					if (file.isHidden() || '.' == name.charAt(0)) return false;
 					return true;
 				}
 			}).length) {
 				try { f.delete(); } catch (Exception e) { Utils.log("Could not remove empty trakem2.mipmaps directory."); }
 			}
 		}
 		// remove crash detector
 		File f = new File(dir_mipmaps + ".open.t2");
 		try {
 			if (!f.delete()) {
 				Utils.log2("WARNING: could not delete crash detector file .open.t2 from trakem2.mipmaps folder at " + dir_mipmaps);
 			}
 		} catch (Exception e) {
 			Utils.log2("WARNING: crash detector file trakem.mipmaps/.open.t2 may NOT have been deleted.");
 			IJError.print(e);
 		}
 	}
 
 	/** Get the next unique id, not shared by any other object within the same project. */
 	public long getNextId() {
 		long nid = -1;
 		synchronized (db_lock) {
 			lock();
 			nid = ++max_id;
 			unlock();
 		}
 		return nid;
 	}
 
 	/** Loaded in full from XML file */
 	public double[][][] fetchBezierArrays(long id) {
 		return null;
 	}
 
 	/** Loaded in full from XML file */
 	public ArrayList fetchPipePoints(long id) {
 		return null;
 	}
 
 	/** Loaded in full from XML file */
 	public ArrayList fetchBallPoints(long id) {
 		return null;
 	}
 
 	/** Loaded in full from XML file */
 	public Area fetchArea(long area_list_id, long layer_id) { 
 		return null;
 	}
 
 	/* Note that the min and max is not set -- it's your burden to call setMinAndMax(p.getMin(), p.getMax()) on the returned ImagePlust.getProcessor().
 	 * or just use the Patch.getImageProcessor() method which does it for you. */
 	public ImagePlus fetchImagePlus(final Patch p) {
 		return (ImagePlus)fetchImage(p, Layer.IMAGEPLUS);
 	}
 
 	/** Fetch the ImageProcessor in a synchronized manner, so that there are no conflicts in retrieving the ImageProcessor for a specific stack slice, for example.
 	 * Note that the min and max is not set -- it's your burden to call setMinAndMax(p.getMin(), p.getMax()) on the returned ImageProcessor,
 	 * or just use the Patch.getImageProcessor() method which does it for you. */
 	public ImageProcessor fetchImageProcessor(final Patch p) {
 		return (ImageProcessor)fetchImage(p, Layer.IMAGEPROCESSOR);
 	}
 
 	/** So far accepts Layer.IMAGEPLUS and Layer.IMAGEPROCESSOR as format. */
 	public Object fetchImage(final Patch p, final int format) {
 		ImagePlus imp = null;
 		ImageProcessor ip = null;
 		String slice = null;
 		String path = null;
 		long n_bytes = 0;
 		PatchLoadingLock plock = null;
 		synchronized (db_lock) {
 			lock();
 			imp = imps.get(p.getId());
 			try {
 				path = getAbsolutePath(p);
 				int i_sl = -1;
 				if (null != path) i_sl = path.lastIndexOf("-----#slice=");
 				if (-1 != i_sl) {
 					// activate proper slice
 					if (null != imp) {
 						// check that the stack is large enough (user may have changed it)
 						final int ia = Integer.parseInt(path.substring(i_sl + 12));
 						if (ia <= imp.getNSlices()) {
 							if (null == imp.getStack() || null == imp.getStack().getPixels(ia)) {
 								// reload (happens when closing a stack that was opened before importing it, and then trying to paint, for example)
 								imps.remove(p.getId());
 								imp = null;
 							} else {
 								imp.setSlice(ia);
 								switch (format) {
 									case Layer.IMAGEPROCESSOR:
 										ip = imp.getStack().getProcessor(ia);
 										unlock();
 										return ip;
 									case Layer.IMAGEPLUS:
 										unlock();
 										return imp;
 									default:
 										Utils.log("FSLoader.fetchImage: Unknown format " + format);
 										return null;
 								}
 							}
 						} else {
 							unlock();
 							return null; // beyond bonds!
 						}
 					}
 				}
 				// for non-stack images
 				if (null != imp) {
 					unlock();
 					switch (format) {
 						case Layer.IMAGEPROCESSOR:
 							return imp.getProcessor();
 						case Layer.IMAGEPLUS:
 							return imp;
 						default:
 							Utils.log("FSLoader.fetchImage: Unknown format " + format);
 							return null;
 					}
 				}
 				if (-1 != i_sl) {
 					slice = path.substring(i_sl);
 					// set path proper
 					path = path.substring(0, i_sl);
 				}
 
 				releaseMemory(); // ensure there is a minimum % of free memory
 				plock = getOrMakePatchLoadingLock(p, 0);
 			} catch (Exception e) {
 				IJError.print(e);
 				return null;
 			} finally {
 				unlock();
 			}
 		}
 
 
 		synchronized (plock) {
 			plock.lock();
 
 			imp = imps.get(p.getId());
 			if (null != imp) {
 				// was loaded by a different thread
 				plock.unlock();
 				switch (format) {
 					case Layer.IMAGEPROCESSOR:
 						return imp.getProcessor();
 					case Layer.IMAGEPLUS:
 						return imp;
 					default:
 						Utils.log("FSLoader.fetchImage: Unknown format " + format);
 						return null;
 				}
 			}
 
 			// going to load:
 
 
 			// reserve memory:
 			synchronized (db_lock) {
 				lock();
 				n_bytes = estimateImageFileSize(p, 0);
 				max_memory -= n_bytes;
 				unlock();
 			}
 
 			releaseToFit(n_bytes);
 			imp = openImage(path);
 
 			preProcess(imp);
 
 			synchronized (db_lock) {
 				try {
 					lock();
 					max_memory += n_bytes;
 
 					if (null == imp) {
 						if (!hs_unloadable.contains(p)) {
 							Utils.log("FSLoader.fetchImagePlus: no image exists for patch  " + p + "  at path " + path);
 							hs_unloadable.add(p);
 						}
 						removePatchLoadingLock(plock);
 						unlock();
 						plock.unlock();
 						return null;
 					}
 					// update all clients of the stack, if any
 					if (null != slice) {
 						String rel_path = getPath(p); // possibly relative
 						final int r_isl = rel_path.lastIndexOf("-----#slice");
 						if (-1 != r_isl) rel_path = rel_path.substring(0, r_isl); // should always happen
 						for (Iterator<Map.Entry<Long,String>> it = ht_paths.entrySet().iterator(); it.hasNext(); ) {
 							final Map.Entry<Long,String> entry = it.next();
 							final String str = entry.getValue(); // this is like calling getPath(p)
 							//Utils.log2("processing " + str);
 							if (0 != str.indexOf(rel_path)) {
 								//Utils.log2("SKIP str is: " + str + "\t but path is: " + rel_path);
 								continue; // get only those whose path is identical, of course!
 							}
 							final int isl = str.lastIndexOf("-----#slice=");
 							if (-1 != isl) {
 								//int i_slice = Integer.parseInt(str.substring(isl + 12));
 								final long lid = entry.getKey();
 								imps.put(lid, imp);
 							}
 						}
 						// set proper active slice
 						final int ia = Integer.parseInt(slice.substring(12));
 						imp.setSlice(ia);
 						if (Layer.IMAGEPROCESSOR == format) ip = imp.getStack().getProcessor(ia); // otherwise creates one new for nothing
 					} else {
 						// for non-stack images
 						// OBSOLETE and wrong //p.putMinAndMax(imp); // non-destructive contrast: min and max -- WRONG, it's destructive for ColorProcessor and ByteProcessor!
 							// puts the Patch min and max values into the ImagePlus processor.
 						imps.put(p.getId(), imp);
 						if (Layer.IMAGEPROCESSOR == format) ip = imp.getProcessor();
 					}
 					// imp is cached, so:
 					removePatchLoadingLock(plock);
 
 				} catch (Exception e) {
 					IJError.print(e);
 				}
 				unlock();
 				plock.unlock();
 				switch (format) {
 					case Layer.IMAGEPROCESSOR:
 						return ip; // not imp.getProcessor because after unlocking the slice may have changed for stacks.
 					case Layer.IMAGEPLUS:
 						return imp;
 					default:
 						Utils.log("FSLoader.fetchImage: Unknown format " + format);
 						return null;
 
 				}
 			}
 		}
 	}
 
 	/** Returns the alpha mask image from a file, or null if none stored. */
 	@Override
 	public ByteProcessor fetchImageMask(final Patch p) {
 		// Else, see if there is a file for the Patch:
 		final String path = getAlphaPath(p);
 		if (null == path) return null;
 		// Open the mask image, which should be a compressed float tif.
 		final ImagePlus imp = opener.openImage(path);
 		if (null == imp) {
 			Utils.log2("No mask found or could not open mask image for patch " + p + " from " + path);
 			return null;
 		}
 		return (ByteProcessor)imp.getProcessor().convertToByte(false);
 	}
 
 	@Override
 	public String getAlphaPath(final Patch p) {
 		final String filename = getInternalFileName(p);
 		if (null == filename) {
 			Utils.log2("null filepath!");
 			return null;
 		}
 		final String dir = getMasksFolder();
 		return new StringBuffer(dir).append(filename).append('.').append(p.getId()).append(".zip").toString();
 	}
 
 	@Override
 	public void storeAlphaMask(final Patch p, final ByteProcessor fp) {
 		// would fail if user deletes the trakem2.masks/ folder from the storage folder after having set dir_masks. But that is his problem.
 		new FileSaver(new ImagePlus("mask", fp)).saveAsZip(getAlphaPath(p));
 	}
 
 	public final String getMasksFolder() {
 		if (null == dir_masks) createMasksFolder();
 		return dir_masks;
 	}
 
 	synchronized private final void createMasksFolder() {
 		if (null == dir_masks) dir_masks = getStorageFolder() + "trakem2.masks/";
 		final File f = new File(dir_masks);
 		if (f.exists() && f.isDirectory()) return;
 		try {
 			f.mkdir();
 		} catch (Exception e) {
 			IJError.print(e);
 		}
 	}
 
 	/** Remove the file containing the given Patch's alpha mask. */
 	public final boolean removeAlphaMask(final Patch p) {
 		try {
 			File f = new File(getAlphaPath(p));
 			if (f.exists()) {
 				return f.delete();
 			}
 			return true;
 		} catch (Exception e) {
 			IJError.print(e);
 		}
 		return false;
 	}
 
 	/** Loaded in full from XML file */
 	public Object[] fetchLabel(DLabel label) {
 		return null;
 	}
 
 	/** Loads and returns the original image, which is not cached, or returns null if it's not different than the working image. */
 	synchronized public ImagePlus fetchOriginal(final Patch patch) {
 		String original_path = patch.getOriginalPath();
 		if (null == original_path) return null;
 		// else, reserve memory and open it:
 		long n_bytes = estimateImageFileSize(patch, 0);
 		// reserve memory:
 		synchronized (db_lock) {
 			lock();
 			max_memory -= n_bytes;
 			unlock();
 		}
 		try {
 			return openImage(original_path);
 		} catch (Throwable t) {
 			IJError.print(t);
 		} finally {
 			synchronized (db_lock) {
 				lock();
 				max_memory += n_bytes;
 				unlock();
 			}
 		}
 		return null;
 	}
 
 	public void prepare(Layer layer) {
 		//Utils.log2("FSLoader.prepare(Layer): not implemented.");
 		super.prepare(layer);
 	}
 
 	/* GENERIC, from DBObject calls. Records the id of the object in the HashMap ht_dbo.
 	 * Always returns true. Does not check if another object has the same id.
 	 */
 	public boolean addToDatabase(final DBObject ob) {
 		synchronized (db_lock) {
 			lock();
 			setChanged(true);
 			final long id = ob.getId();
 			if (id > max_id) {
 				max_id = id;
 			}
 			unlock();
 		}
 		return true;
 	}
 
 	public boolean updateInDatabase(final DBObject ob, final String key) {
 		setChanged(true);
 		if (ob.getClass() == Patch.class) {
 			Patch p = (Patch)ob;
 			if (key.equals("tiff_working")) return null != setImageFile(p, fetchImagePlus(p));
 		}
 		return true;
 	}
 
 	public boolean removeFromDatabase(final DBObject ob) {
 		synchronized (db_lock) {
 			lock();
 			setChanged(true);
 			// remove from the hashtable
 			final long loid = ob.getId();
 			Utils.log2("removing " + Project.getName(ob.getClass()) + " " + ob);
 			if (ob.getClass() == Patch.class) {
 				// STRATEGY change: images are not owned by the FSLoader.
 				Patch p = (Patch)ob;
 				if (!ob.getProject().getBooleanProperty("keep_mipmaps")) removeMipMaps(p);
 				ht_paths.remove(p.getId()); // after removeMipMaps !
 				mawts.removeAndFlush(loid);
 				final ImagePlus imp = imps.remove(loid);
 				if (null != imp) {
 					if (imp.getStackSize() > 1) {
 						if (null == imp.getProcessor()) {}
 						else if (null == imp.getProcessor().getPixels()) {}
 						else Loader.flush(imp); // only once
 					} else {
 						Loader.flush(imp);
 					}
 				}
 				cannot_regenerate.remove(p);
 				unlock();
 				flushMipMaps(p.getId()); // locks on its own
 				touched_mipmaps.remove(p);
 				return true;
 			}
 			unlock();
 		}
 		return true;
 	}
 
 	/** Returns the absolute path to a file that contains the given ImagePlus image - which may be the path as described in the ImagePlus FileInfo object itself, or a totally new file.
 	 *  If the Patch p current image path is different than its original image path, then the file is overwritten if it exists already.
 	 */
 	public String setImageFile(final Patch p, final ImagePlus imp) {
 		if (null == imp) return null;
 		try {
 			String path = getAbsolutePath(p);
 			String slice = null;
 			//
 			// path can be null if the image is pasted, or from a copy, or totally new
 			if (null != path) {
 				int i_sl = path.lastIndexOf("-----#slice=");
 				if (-1 != i_sl) {
 					slice = path.substring(i_sl);
 					path = path.substring(0, i_sl);
 				}
 			} else {
 				// no path, inspect image FileInfo's path if the image has no changes
 				if (!imp.changes) {
 					final FileInfo fi = imp.getOriginalFileInfo();
 					if (null != fi && null != fi.directory && null != fi.fileName) {
 						final String fipath = fi.directory.replace('\\', '/') + "/" + fi.fileName;
 						if (new File(fipath).exists()) {
 							// no need to save a new image, it exists and has no changes
 							updatePaths(p, fipath, null != slice);
 							cacheAll(p, imp);
 							Utils.log2("Reusing image file: path exists for fileinfo at " + fipath);
 							return fipath;
 						}
 					}
 				}
 			}
 			if (null != path) {
 				final String starting_path = path;
 				// Save as a separate image in a new path within the storage folder
 
 				String filename = path.substring(path.lastIndexOf('/') +1);
 
 				//Utils.log2("filename 1: " + filename);
 
 				// remove .tif extension if there
 				if (filename.endsWith(".tif")) filename = filename.substring(0, filename.length() -3); // keep the dot
 
 				//Utils.log2("filename 2: " + filename);
 
 				// check if file ends with a tag of form ".id1234." where 1234 is p.getId()
 				final String tag = ".id" + p.getId() + ".";
 				if (!filename.endsWith(tag)) filename += tag.substring(1); // without the starting dot, since it has one already
 				// reappend extension
 				filename += "tif";
 
 				//Utils.log2("filename 3: " + filename);
 
 				path = getImageStorageFolder() + filename;
 
 				if (path.equals(p.getOriginalPath())) {
 					// Houston, we have a problem: a user reused a non-original image
 					File file = null;
 					int i = 1;
 					final int itag = path.lastIndexOf(tag);
 					do {
 						path = path.substring(0, itag) + "." + i + tag + "tif";
 						i++;
 						file = new File(path);
 					} while (file.exists());
 				}
 
 				//Utils.log2("path to use: " + path);
 
 				final String path2 = super.exportImage(p, imp, path, true);
 
 				//Utils.log2("path exported to: " + path2);
 
 				// update paths' hashtable
 				if (null != path2) {
 					updatePaths(p, path2, null != slice);
 					cacheAll(p, imp);
 					hs_unloadable.remove(p);
 					return path2;
 				} else {
 					Utils.log("WARNING could not save image at " + path);
 					// undo
 					updatePaths(p, starting_path, null != slice);
 					return null;
 				}
 			}
 		} catch (Exception e) {
 			IJError.print(e);
 		}
 		return null;
 	}
 
 	/**
 	 * TODO
 	 *   Never used.  Was this planned to be what we do no with DBObject.getUniqueId()?
 	 */
 	private final String makeFileTitle(final Patch p) {
 		String title = p.getTitle();
 		if (null == title) return "image-" + p.getId();
 		title = asSafePath(title);
 		if (0 == title.length()) return "image-" + p.getId();
 		return title;
 	}
 
 	/** Associate patch with imp, and all slices as well if any. */
 	private void cacheAll(final Patch p, final ImagePlus imp) {
 		if (p.isStack()) {
 			for (Patch pa : p.getStackPatches()) {
 				cache(pa, imp);
 			}
 		} else {
 			cache(p, imp);
 		}
 	}
 
 	/** For the Patch and for any associated slices if the patch is part of a stack. */
 	private void updatePaths(final Patch patch, final String path, final boolean is_stack) {
 		synchronized (db_lock) {
 			lock();
 			try {
 				// ensure the old path is cached in the Patch, to get set as the original if there is no original.
 				if (is_stack) {
 					for (Patch p : patch.getStackPatches()) {
 						long pid = p.getId();
 						String str = ht_paths.get(pid);
 						int isl = str.lastIndexOf("-----#slice=");
 						updatePatchPath(p, path + str.substring(isl));
 					}
 				} else {
 					Utils.log2("path to set: " + path);
 					Utils.log2("path before: " + ht_paths.get(patch.getId()));
 					updatePatchPath(patch, path);
 					Utils.log2("path after: " + ht_paths.get(patch.getId()));
 				}
 			} catch (Throwable e) {
 				IJError.print(e);
 			} finally {
 				unlock();
 			}
 		}
 	}
 
 	/** With slice info appended at the end; only if it exists, otherwise null. */
 	public String getAbsolutePath(final Patch patch) {
 		String abs_path = patch.getCurrentPath();
 		if (null != abs_path) return abs_path;
 		// else, compute, set and return it:
 		String path = ht_paths.get(patch.getId());
 		if (null == path) return null;
 		// substract slice info if there
 		int i_sl = path.lastIndexOf("-----#slice=");
 		String slice = null;
 		if (-1 != i_sl) {
 			slice = path.substring(i_sl);
 			path = path.substring(0, i_sl);
 		}
 		if (isRelativePath(path)) {
 			// path is relative: preprend the parent folder of the xml file
 			path = getParentFolder() + path;
 			if (!isURL(path) && !new File(path).exists()) {
 				Utils.log("Path for patch " + patch + " does not exist: " + path);
 				return null;
 			}
 			// else assume that it exists
 		}
 		// reappend slice info if existent
 		if (null != slice) path += slice;
 		// set it
 		patch.cacheCurrentPath(path);
 		return path;
 	}
 
 	public static final boolean isURL(final String path) {
 		return null != path && 0 == path.indexOf("http://");
 	}
 
 	public static final boolean isRelativePath(final String path) {
 		if (((!IJ.isWindows() && 0 != path.indexOf('/')) || (IJ.isWindows() && 1 != path.indexOf(":/"))) && 0 != path.indexOf("http://") && 0 != path.indexOf("//")) { // "//" is for Samba networks (since the \\ has been converted to // before)
 			return true;
 		}
 		return false;
 	}
 
 	/** All backslashes are converted to slashes to avoid havoc in MSWindows. */
 	public void addedPatchFrom(String path, final Patch patch) {
 		if (null == path) {
 			Utils.log("Null path for patch: " + patch);
 			return;
 		}
 		updatePatchPath(patch, path);
 	}
 
 	/** This method has the exclusivity in calling ht_paths.put, because it ensures the path won't have escape characters. */
 	private final void updatePatchPath(final Patch patch, String path) { // reversed order in purpose, relative to addedPatchFrom
 		// avoid W1nd0ws nightmares
 		path = path.replace('\\', '/'); // replacing with chars, in place
 		// remove double slashes that a user may have slipped in
 		final int start = isURL(path) ? 6 : (IJ.isWindows() ? 3 : 1);
 		while (-1 != path.indexOf("//", start)) {
 			// avoid the potential C:// of windows and the starting // of a samba network
 			path = path.substring(0, start) + path.substring(start).replace("//", "/");
 		}
 		// cache path as absolute
 		patch.cacheCurrentPath(isRelativePath(path) ? getParentFolder() + path : path);
 		// if path is absolute, try to make it relative
 		path = makeRelativePath(path);
 		// store
 		ht_paths.put(patch.getId(), path);
 		//Utils.log2("Updated patch path " + ht_paths.get(patch.getId()) + " for patch " + patch);
 	}
 
 	/** Takes a String and returns a copy with the following conversions: / to -, space to _, and \ to -. */
 	public String asSafePath(final String name) {
 		return name.trim().replace('/', '-').replace(' ', '_').replace('\\','-');
 	}
 
 	/** Overwrites the XML file. If some images do not exist in the file system, a directory with the same name of the XML file plus an "_images" tag appended will be created and images saved there. */
 	public String save(final Project project) {
 		String result = null;
 		if (null == project_file_path) {
 			String xml_path = super.saveAs(project, null, false);
 			if (null == xml_path) return null;
 			else {
 				this.project_file_path = xml_path;
 				ControlWindow.updateTitle(project);
 				result = this.project_file_path;
 			}
 		} else {
 			File fxml = new File(project_file_path);
 			result = super.export(project, fxml, false);
 		}
 		if (null != result) {
 			Utils.logAll(Utils.now() + " Saved " + project);
 			touched_mipmaps.clear();
 		}
 		return result;
 	}
 
 	public String saveAs(Project project) {
 		String path = super.saveAs(project, null, false);
 		if (null != path) {
 			// update the xml path to point to the new one
 			this.project_file_path = path;
 			Utils.log2("After saveAs, new xml path is: " + path);
 		}
 		ControlWindow.updateTitle(project);
 		return path;
 	}
 
 	/** Meant for programmatic access, such as calls to project.saveAs(path, overwrite) which call exactly this method. */
 	public String saveAs(final String path, final boolean overwrite) {
 		if (null == path) {
 			Utils.log("Cannot save on null path.");
 			return null;
 		}
 		String path2 = path;
 		if (!path2.endsWith(".xml")) path2 += ".xml";
 		File fxml = new File(path2);
 		if (!fxml.canWrite()) {
 			// write to storage folder instead
 			String path3 = path2;
 			path2 = getStorageFolder() + fxml.getName();
 			Utils.logAll("WARNING can't write to " + path3 + "\n  --> will write instead to " + path2);
 			fxml = new File(path2);
 		}
 		if (!overwrite) {
 			int i = 1;
 			while (fxml.exists()) {
 				String parent = fxml.getParent().replace('\\','/');
 				if (!parent.endsWith("/")) parent += "/";
 				String name = fxml.getName();
 				name = name.substring(0, name.length() - 4);
 				path2 =  parent + name + "-" +  i + ".xml";
 				fxml = new File(path2);
 				i++;
 			}
 		}
 		Project project = Project.findProject(this);
 		path2 = super.saveAs(project, path2, false);
 		if (null != path2) {
 			project_file_path = path2;
 			Utils.logAll("After saveAs, new xml path is: " + path2);
 			ControlWindow.updateTitle(project);
 			touched_mipmaps.clear();
 		}
 		return path2;
 	}
 
 	/** Returns the stored path for the given Patch image, which may be relative and may contain slice information appended.*/
 	public String getPath(final Patch patch) {
 		return ht_paths.get(patch.getId());
 	}
 
 	/** Takes the given path and tries to makes it relative to this instance's project_file_path, if possible. Otherwise returns the argument as is. */
 	private String makeRelativePath(String path) {
 		if (null == project_file_path) {
 			//unsaved project
 			return path;
 		}
 		if (null == path) {
 			return null;
 		}
 		// fix W1nd0ws paths
 		path = path.replace('\\', '/'); // char-based, no parsing problems
 		// remove slice tag
 		String slice = null;
 		int isl = path.lastIndexOf("-----#slice");
 		if (-1 != isl) {
 			slice = path.substring(isl);
 			path = path.substring(0, isl);
 		}
 		//
 		if (isRelativePath(path)) {
 			// already relative
 			if (-1 != isl) path += slice;
 			return path;
 		}
 		// the long and verbose way, to be cross-platform. Should work with URLs just the same.
 		File xf = new File(project_file_path);
 		File fpath = new File(path);
 		if (fpath.getParentFile().equals(xf.getParentFile())) {
 			path = path.substring(xf.getParent().length());
 			// remove prepended file separator, if any, to label the path as relative
 			if (0 == path.indexOf('/')) path = path.substring(1);
 		} else if (fpath.equals(xf.getParentFile())) {
 			return "";
 		}
 		if (-1 != isl) path += slice;
 		//Utils.log("made relative path: " + path);
 		return path;
 	}
 
 	/** Adds a "Save" and "Save as" menu items. */
 	public void setupMenuItems(final JMenu menu, final Project project) {
 		ActionListener listener = new ActionListener() {
 			public void actionPerformed(ActionEvent ae) {
 				String command = ae.getActionCommand();
 				if (command.equals("Save")) {
 					save(project);
 				} else if (command.equals("Save as...")) {
 					saveAs(project);
 				}
 			}
 		};
 		JMenuItem item;
 		item = new JMenuItem("Save"); item.addActionListener(listener); menu.add(item);
 		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0, true));
 		item = new JMenuItem("Save as..."); item.addActionListener(listener); menu.add(item);
 	}
 
 	/** Returns the last Patch. */
 	protected Patch importStackAsPatches(final Project project, final Layer first_layer, final int x, final int y, final ImagePlus imp_stack, final boolean as_copy, final String filepath) {
 		Utils.log2("FSLoader.importStackAsPatches filepath=" + filepath);
 		String target_dir = null;
 		if (as_copy) {
 			DirectoryChooser dc = new DirectoryChooser("Folder to save images");
 			target_dir = dc.getDirectory();
 			if (null == target_dir) return null; // user canceled dialog
 			if (target_dir.length() -1 != target_dir.lastIndexOf('/')) {
 				target_dir += "/";
 			}
 		}
 
 		final boolean virtual = imp_stack.getStack().isVirtual();
 
 		int pos_x = Integer.MAX_VALUE != x ? x : (int)first_layer.getLayerWidth()/2 - imp_stack.getWidth()/2;
 		int pos_y = Integer.MAX_VALUE != y ? y : (int)first_layer.getLayerHeight()/2 - imp_stack.getHeight()/2;
 		final double thickness = first_layer.getThickness();
 		final String title = Utils.removeExtension(imp_stack.getTitle()).replace(' ', '_');
 		Utils.showProgress(0);
 		Patch previous_patch = null;
 		final int n = imp_stack.getStackSize();
 		for (int i=1; i<=n; i++) {
 			Layer layer = first_layer;
 			double z = first_layer.getZ() + (i-1) * thickness;
 			if (i > 1) layer = first_layer.getParent().getLayer(z, thickness, true); // will create new layer if not found
 			if (null == layer) {
 				Utils.log("Display.importStack: could not create new layers.");
 				return null;
 			}
 			String patch_path = null;
 
 			ImagePlus imp_patch_i = null;
 			if (virtual) { // because we love inefficiency, every time all this is done again
 				VirtualStack vs = (VirtualStack)imp_stack.getStack();
 				String vs_dir = vs.getDirectory().replace('\\', '/');
 				if (!vs_dir.endsWith("/")) vs_dir += "/";
 				String iname = vs.getFileName(i);
 				patch_path = vs_dir + iname;
 				releaseMemory();
 				Utils.log2(i + " : " + patch_path);
 				imp_patch_i = openImage(patch_path);
 			} else {
 				ImageProcessor ip = imp_stack.getStack().getProcessor(i);
 				if (as_copy) ip = ip.duplicate();
 				imp_patch_i = new ImagePlus(title + "__slice=" + i, ip);
 			}
 			preProcess(imp_patch_i);
 
 			String label = imp_stack.getStack().getSliceLabel(i);
 			if (null == label) label = "";
 			Patch patch = null;
 			if (as_copy) {
 				patch_path = target_dir + imp_patch_i.getTitle() + ".zip";
 				ini.trakem2.io.ImageSaver.saveAsZip(imp_patch_i, patch_path);
 				patch = new Patch(project, label + " " + title + " " + i, pos_x, pos_y, imp_patch_i);
 			} else if (virtual) {
 				patch = new Patch(project, label, pos_x, pos_y, imp_patch_i);
 			} else {
 				patch_path = filepath + "-----#slice=" + i;
 				//Utils.log2("path is "+ patch_path);
 				final AffineTransform atp = new AffineTransform();
 				atp.translate(pos_x, pos_y);
 				patch = new Patch(project, getNextId(), label + " " + title + " " + i, imp_stack.getWidth(), imp_stack.getHeight(), imp_stack.getType(), false, imp_stack.getProcessor().getMin(), imp_stack.getProcessor().getMax(), atp);
 				patch.addToDatabase();
 				//Utils.log2("type is " + imp_stack.getType());
 			}
 			Utils.log2("B: " + i + " : " + patch_path);
 			addedPatchFrom(patch_path, patch);
 			if (!as_copy) {
 				if (virtual) cache(patch, imp_patch_i); // each slice separately
 				else cache(patch, imp_stack); // uses the entire stack, shared among all Patch instances
 			}
 			if (isMipMapsEnabled()) generateMipMaps(patch);
 			if (null != previous_patch) patch.link(previous_patch);
 			layer.add(patch);
 			previous_patch = patch;
 			Utils.showProgress(i * (1.0 / n));
 		}
 		Utils.showProgress(1.0);
 
 		// update calibration
 		// TODO
 
 		// return the last patch
 		return previous_patch;
 	}
 
 	/** Specific options for the Loader which exist as attributes to the Project XML node. */
 	public void parseXMLOptions(final HashMap ht_attributes) {
 		Object ob = ht_attributes.remove("preprocessor");
 		if (null != ob) {
 			setPreprocessor((String)ob);
 		}
 		// Adding some logic to support old projects which lack a storage folder and a mipmaps folder
 		// and also to prevent errors such as those created when manualy tinkering with the XML file
 		// or renaming directories, etc.
 		ob = ht_attributes.remove("storage_folder");
 		if (null != ob) {
 			String sf = ((String)ob).replace('\\', '/');
 			if (isRelativePath(sf)) {
 				sf = getParentFolder() + sf;
 			}
 			if (isURL(sf)) {
 				// can't be an URL
 				Utils.log2("Can't have an URL as the path of a storage folder.");
 			} else {
 				File f = new File(sf);
 				if (f.exists() && f.isDirectory()) {
 					this.dir_storage = sf;
 				} else {
 					Utils.log2("storage_folder was not found or is invalid: " + ob);
 				}
 			}
 		}
 		if (null == this.dir_storage) {
 			// select the directory where the xml file lives.
 			this.dir_storage = getParentFolder();
 			if (null == this.dir_storage || isURL(this.dir_storage)) this.dir_storage = null;
 			if (null == this.dir_storage && ControlWindow.isGUIEnabled()) {
 				Utils.log2("Asking user for a storage folder in a dialog."); // tip for headless runners whose program gets "stuck"
 				DirectoryChooser dc = new DirectoryChooser("REQUIRED: select a storage folder");
 				this.dir_storage = dc.getDirectory();
 			}
 			if (null == this.dir_storage) {
 				IJ.showMessage("TrakEM2 requires a storage folder.\nTemporarily your home directory will be used.");
 				this.dir_storage = System.getProperty("user.home").replace('\\', '/');
 			}
 		}
 		// fix
 		if (null != this.dir_storage && !this.dir_storage.endsWith("/")) this.dir_storage += "/";
 		Utils.log2("storage folder is " + this.dir_storage);
 		//
 		ob = ht_attributes.remove("mipmaps_folder");
 		if (null != ob) {
 			String mf = ((String)ob).replace('\\', '/');
 			if (isRelativePath(mf)) {
 				mf = getParentFolder() + mf;
 			}
 			if (isURL(mf)) {
 				this.dir_mipmaps = mf;
 				// TODO must disable input somehow, so that images are not edited.
 			} else {
 				File f = new File(mf);
 				if (f.exists() && f.isDirectory()) {
 					this.dir_mipmaps = mf;
 				} else {
 					Utils.log2("mipmaps_folder was not found or is invalid: " + ob);
 				}
 			}
 		}
 		if (null == this.dir_mipmaps) {
 			// create a new one inside the dir_storage, which can't be null
 			createMipMapsDir(dir_storage);
 			if (null != this.dir_mipmaps && ControlWindow.isGUIEnabled() && null != IJ.getInstance()) {
 				askAndExecMipmapRegeneration(null);
 			}
 		}
 		// fix
 		if (null != this.dir_mipmaps && !this.dir_mipmaps.endsWith("/")) this.dir_mipmaps += "/";
 		Utils.log2("mipmaps folder is " + this.dir_mipmaps);
 	}
 
 	private void askAndExecMipmapRegeneration(final String msg) {
 		Utils.log2("Asking user Yes/No to generate mipmaps on the background."); // tip for headless runners whose program gets "stuck"
 		YesNoDialog yn = new YesNoDialog(IJ.getInstance(), "Generate mipmaps", (null != msg ? msg  + "\n" : "") + "Generate mipmaps in the background for all images?");
 		if (yn.yesPressed()) {
 			final Loader lo = this;
 			new Thread() {
 				public void run() {
 					try {
 						// wait while parsing the rest of the XML file
 						while (!v_loaders.contains(lo)) {
 							Thread.sleep(1000);
 						}
 						Project pj = Project.findProject(lo);
 						lo.generateMipMaps(pj.getRootLayerSet().getDisplayables(Patch.class));
 					} catch (Exception e) {}
 				}
 			}.start();
 		}
 	}
 
 	/** Specific options for the Loader which exist as attributes to the Project XML node. */
 	public void insertXMLOptions(StringBuffer sb_body, String indent) {
 		if (null != preprocessor) sb_body.append(indent).append("preprocessor=\"").append(preprocessor).append("\"\n");
 		if (null != dir_mipmaps) sb_body.append(indent).append("mipmaps_folder=\"").append(makeRelativePath(dir_mipmaps)).append("\"\n");
 		if (null != dir_storage) sb_body.append(indent).append("storage_folder=\"").append(makeRelativePath(dir_storage)).append("\"\n");
 	}
 
 	/** Return the path to the folder containing the project XML file. */
 	private final String getParentFolder() {
 		return this.project_file_path.substring(0, this.project_file_path.lastIndexOf('/')+1);
 	}
 
 	/* ************** MIPMAPS **********************/
 
 	/** Returns the path to the directory hosting the file image pyramids. */
 	public String getMipMapsFolder() {
 		return dir_mipmaps;
 	}
 
 
 	/*
 	static private IndexColorModel thresh_cm = null;
 
 	static private final IndexColorModel getThresholdLUT() {
 		if (null == thresh_cm) {
 			// An array of all black pixels (value 0) except at 255, which is white (value 255).
 			final byte[] c = new byte[256];
 			c[255] = (byte)255;
 			thresh_cm = new IndexColorModel(8, 256, c, c, c);
 		}
 		return thresh_cm;
 	}
 	*/
 
 	/** Returns the array of pixels, whose type depends on the bi.getType(); for example, for a BufferedImage.TYPE_BYTE_INDEXED, returns a byte[]. */
 	static public final Object grabPixels(final BufferedImage bi) {
 		final PixelGrabber pg = new PixelGrabber(bi, 0, 0, bi.getWidth(), bi.getHeight(), false);
 		try {
 			pg.grabPixels();
 			return pg.getPixels();
 		} catch (InterruptedException e) {
 			IJError.print(e);
 		}
 		return null;
 	}
 
 	private final BufferedImage createCroppedAlpha(final BufferedImage alpha, final BufferedImage outside) {
 		if (null == outside) return alpha;
 
 		final int width = outside.getWidth();
 		final int height = outside.getHeight();
 
 		// Create an outside image, thresholded: only pixels of 255 remain as 255, the rest is set to 0.
 		/* // DOESN'T work: creates a mask with "black" as 254 (???), and white 255 (correct).
 		final BufferedImage thresholded = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, getThresholdLUT());
 		thresholded.createGraphics().drawImage(outside, 0, 0, null);
 		*/
 
 		// So, instead: grab the pixels, fix them manually
 		// The cast to byte[] works because "outside" and "alpha" are TYPE_BYTE_INDEXED.
 		final byte[] o = (byte[])grabPixels(outside); 
 		if (null == o) return null;
 		final byte[] a = null == alpha ? o : (byte[])grabPixels(alpha);
 
 		// Set each non-255 pixel in outside to 0 in alpha:
 		for (int i=0; i<o.length; i++) {
 			if ( (o[i]&0xff) < 255) a[i] = 0;
 		}
 
 		// Put the pixels back into an image:
 		final BufferedImage thresholded = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, Loader.GRAY_LUT);
 		thresholded.getRaster().setDataElements(0, 0, width, height, a);
 
 		return thresholded;
 	}
 
 	static public final BufferedImage convertToBufferedImage(final ByteProcessor bp) {
 		bp.setMinAndMax(0, 255);
 		final Image img = bp.createImage();
 		if (img instanceof BufferedImage) return (BufferedImage)img;
 		//else:
 		final BufferedImage bi = new BufferedImage(bp.getWidth(), bp.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, Loader.GRAY_LUT);
 		bi.createGraphics().drawImage(bi, 0, 0, null);
 		return bi;
 	}
 
 	/** Scale a BufferedImage.TYPE_BYTE_INDEXED into another of the same type but dimensions target_width,target_height. */
 	static private final BufferedImage scaleAndFlush(final Image img, final int target_width, final int target_height, final boolean area_averaging, final Object interpolation_hint) {
 		final BufferedImage bi = new BufferedImage(target_width, target_height, BufferedImage.TYPE_BYTE_INDEXED, Loader.GRAY_LUT);
 		if (area_averaging) {
 			bi.createGraphics().drawImage(img.getScaledInstance(target_width, target_height, Image.SCALE_AREA_AVERAGING), 0, 0, null);
 		} else {
 			final Graphics2D g = bi.createGraphics();
 			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation_hint);
 			g.drawImage(img, 0, 0, target_width, target_height, null); // draws it scaled to target area w*h
 		}
 		// Release native resources
 		img.flush();
 
 		return bi;
 	}
 
 	/** Image to BufferedImage. Can be used for hardware-accelerated resizing, since the whole awt is painted to a target w,h area in the returned new BufferedImage. Does not accept LUT images: only ARGB or GRAY. */
 	private final BufferedImage[] IToBI(final Image awt, final int w, final int h, final Object interpolation_hint, final BufferedImage alpha, final BufferedImage outside) {
 		BufferedImage bi;
 		final boolean area_averaging = interpolation_hint.getClass() == Integer.class && Loader.AREA_AVERAGING == ((Integer)interpolation_hint).intValue();
 		final boolean must_scale = (w != awt.getWidth(null) || h != awt.getHeight(null));
 
 		if (null != alpha || null != outside) bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
 		else bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
 		final Graphics2D g = bi.createGraphics();
 		if (area_averaging) {
 			final Image img = awt.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING); // Creates ALWAYS an RGB image, so must repaint back to a single-channel image, avoiding unnecessary blow up of memory.
 			g.drawImage(img, 0, 0, null);
 		} else {
 			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation_hint);
 			g.drawImage(awt, 0, 0, w, h, null); // draws it scaled
 		}
 		BufferedImage ba = alpha;
 		BufferedImage bo = outside;
 		if (null != alpha && must_scale) {
 			ba = scaleAndFlush(alpha, w, h, area_averaging, interpolation_hint);
 		}
 		if (null != outside && must_scale) {
 			bo = scaleAndFlush(outside, w, h, area_averaging, interpolation_hint);
 		}
 
 		BufferedImage the_alpha = ba;
 		if (null != alpha) {
 			if (null != outside) {
 				the_alpha = createCroppedAlpha(ba, bo);
 			}
 		} else if (null != outside) {
 			the_alpha = createCroppedAlpha(null, bo);
 		}
 		if (null != the_alpha) {
 			bi.getAlphaRaster().setRect(the_alpha.getRaster());
 			//bi.getAlphaRaster().setPixels(0, 0, w, h, (float[])new ImagePlus("", the_alpha).getProcessor().convertToFloat().getPixels());
 			the_alpha.flush();
 		}
 
 		//Utils.log2("bi is: " + bi.getType() + " BufferedImage.TYPE_INT_ARGB=" + BufferedImage.TYPE_INT_ARGB);
 
 		/*
 		FloatProcessor fp_alpha = null;
 		fp_alpha = (FloatProcessor) new ByteProcessor(ba).convertToFloat();
 		// Set all non-white pixels to zero (eliminate shadowy border caused by interpolation)
 		final float[] pix = (float[])fp_alpha.getPixels();
 		for (int i=0; i<pix.length; i++)
 			if (Math.abs(pix[i] - 255) > 0.001f) pix[i] = 0;
 		bi.getAlphaRaster().setPixels(0, 0, w, h, (float[])fp_alpha.getPixels());
 		*/
 
 		return new BufferedImage[]{bi, ba, bo};
 	}
 
 	private final Object getHint(final int mode) {
 		switch (mode) {
 			case Loader.BICUBIC:
 				return RenderingHints.VALUE_INTERPOLATION_BICUBIC;
 			case Loader.BILINEAR:
 				return RenderingHints.VALUE_INTERPOLATION_BILINEAR;
 			case Loader.AREA_AVERAGING:
 				return new Integer(mode);
 			case Loader.NEAREST_NEIGHBOR:
 			default:
 				return RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
 		}
 	}
 
 	/** WARNING will resize the FloatProcessorT2 source in place, unlike ImageJ standard FloatProcessor class. */
 	static final private byte[] gaussianBlurResizeInHalf(final FloatProcessorT2 source, final int source_width, final int source_height, final int target_width, final int target_height) {
 		source.setPixels(source_width, source_height, ImageFilter.computeGaussianFastMirror(new FloatArray2D((float[])source.getPixels(), source_width, source_height), 0.75f).data);
 		source.resizeInPlace(target_width, target_height);
 		return (byte[])source.convertToByte(false).getPixels(); // no scaling
 	}
 	
 	/** WARNING will resize the FloatProcessorT2 source in place, unlike ImageJ standard FloatProcessor class. */
 	static final private byte[] meanResizeInHalf(final FloatProcessorT2 source, final int sourceWidth, final int sourceHeight, final int targetWidth, final int targetHeight) {
 		final float[] sourceData = source.getFloatPixels();
 		final float[] targetData = new float[targetWidth * targetHeight];
 		int rs = 0;
 		for (int r = 0; r < targetData.length; r += targetWidth) {
 			int xs = -1;
 			for (int x = 0; x < targetWidth; ++x)
 				targetData[r + x] = sourceData[rs + ++xs] + sourceData[rs + ++xs];
 			rs += sourceWidth;
 			xs = -1;
 			for (int x = 0; x < targetWidth; ++x) {
 				targetData[r + x] += sourceData[rs + ++xs] + sourceData[rs + ++xs];
 				targetData[r + x] /= 4;
 			}
 			rs += sourceWidth;
 		}
 		source.setPixels(targetWidth, targetHeight, targetData);
 		return (byte[])source.convertToByte(false).getPixels();
 	}
 
 	
 	/** Given an image and its source file name (without directory prepended), generate
 	 * a pyramid of images until reaching an image not smaller than 32x32 pixels.<br />
 	 * Such images are stored as jpeg 85% quality in a folder named trakem2.mipmaps.<br />
 	 * The Patch id and a ".jpg" extension will be appended to the filename in all cases.<br />
 	 * Any equally named files will be overwritten.
 	 */
 	public boolean generateMipMaps(final Patch patch) {
 		//Utils.log2("mipmaps for " + patch);
 		if (null == dir_mipmaps) createMipMapsDir(null);
 		if (null == dir_mipmaps || isURL(dir_mipmaps)) return false;
 		final String path = getAbsolutePath(patch);
 		if (null == path) {
 			Utils.log2("generateMipMaps: cannot find path for Patch " + patch);
 			cannot_regenerate.add(patch);
 			return false;
 		}
 		synchronized (gm_lock) {
 			gm_lock();
 			if (hs_regenerating_mipmaps.contains(patch)) {
 				// already being done
 				gm_unlock();
 				Utils.log2("Already being done: " + patch);
 				return false;
 			}
 			hs_regenerating_mipmaps.add(patch);
 			gm_unlock();
 		}
 
 		/** Record Patch as modified */
 		touched_mipmaps.add(patch);
 		
 		/** Remove serialized features, if any */
 		removeSerializedFeatures(patch);
 
 		/** Remove serialized pointmatches, if any */
 		removeSerializedPointMatches(patch);
 
 		String srmode = patch.getProject().getProperty("image_resizing_mode");
 		int resizing_mode = GAUSSIAN;
 		if (null != srmode) resizing_mode = Loader.getMode(srmode);
 
 		try {
 			// Now:
 			// 1 - Ask the Patch to apply a coordinate transform, or rather, create a function that gets the coordinate transform from the Patch and applies it to the 'ip'.
 			// 2 - Then (1) should return both the transformed image and the alpha mask
 
 			ImageProcessor ip;
 			ByteProcessor alpha_mask = null;
 			ByteProcessor outside_mask = null;
 			final boolean coordinate_transformed;
 			int type = patch.getType();
 
 			// Obtain an image which may be coordinate-transformed, and an alpha mask.
 			Patch.PatchImage pai = patch.createTransformedImage();
 			ip = pai.target;
 			alpha_mask = pai.mask; // can be null
 			outside_mask = pai.outside; // can be null
 			coordinate_transformed = pai.coordinate_transformed;
 			pai = null;
 			
 			final String filename = new StringBuffer(new File(path).getName()).append('.').append(patch.getId()).append(".jpg").toString();
 			int w = ip.getWidth();
 			int h = ip.getHeight();
 
 			// sigma = sqrt(2^level - 0.5^2)
 			//    where 0.5 is the estimated sigma for a full-scale image
 			//  which means sigma = 0.75 for the full-scale image (has level 0)
 			// prepare a 0.75 sigma image from the original
 			ColorModel cm = ip.getColorModel();
 			int k = 0; // the scale level. Proper scale is: 1 / pow(2, k)
 				   //   but since we scale 50% relative the previous, it's always 0.75
 
 			// Set for the level 0 image, which is a duplicate of the one on the cache in any case
 			ip.setMinAndMax(patch.getMin(), patch.getMax());
 
 
 			// Proper support for LUT images: treat them as RGB
 			if (ip.isColorLut()) {
 				ip = ip.convertToRGB();
 				cm = null;
 				type = ImagePlus.COLOR_RGB;
 			}
 
 			if (ImagePlus.COLOR_RGB == type) {
 				// TODO releaseToFit proper
 				releaseToFit(w * h * 4 * 5);
 				final ColorProcessor cp = (ColorProcessor)ip;
 				final FloatProcessorT2 red = new FloatProcessorT2(w, h, 0, 255);   cp.toFloat(0, red);
 				final FloatProcessorT2 green = new FloatProcessorT2(w, h, 0, 255); cp.toFloat(1, green);
 				final FloatProcessorT2 blue = new FloatProcessorT2(w, h, 0, 255);  cp.toFloat(2, blue);
 				FloatProcessorT2 alpha;
 				final FloatProcessorT2 outside;
 				if (null != alpha_mask) {
 					alpha = new FloatProcessorT2((FloatProcessor)alpha_mask.convertToFloat());
 				} else {
 					alpha = null;
 				}
 				if (null != outside_mask) {
 					outside = new FloatProcessorT2((FloatProcessor)outside_mask.convertToFloat());
 					if ( null == alpha ) {
 						alpha = outside;
 						alpha_mask = outside_mask;
 					}
 				} else {
 					outside = null;
 				}
 				
 				// sw,sh are the dimensions of the image to blur
 				//  w,h are the dimensions to scale the blurred image to
 				int sw = w,
 				    sh = h;
 
 				final String target_dir0 = getLevelDir(dir_mipmaps, 0);
 				// No alpha channel:
 				//  - use gaussian resizing
 				//  - use standard ImageJ java.awt.Image creation
 				
 				// Generate level 0 first:
 				// TODO Add alpha information into the int[] pixel array or make the image visible some ohter way
 				if (!(null == alpha ? ini.trakem2.io.ImageSaver.saveAsJpeg(cp, target_dir0 + filename, 0.85f, false)
 						   : ini.trakem2.io.ImageSaver.saveAsJpegAlpha(createARGBImage(w, h, embedAlpha((int[])cp.getPixels(), (byte[])alpha_mask.getPixels(), null == outside ? null : (byte[])outside_mask.getPixels())), target_dir0 + filename, 0.85f))) {
 					cannot_regenerate.add(patch);
 				} else {
 					do {
 						// 1 - Prepare values for the next scaled image
 						sw = w;
 						sh = h;
 						w /= 2;
 						h /= 2;
 						k++;
 						// 2 - Check that the target folder for the desired scale exists
 						final String target_dir = getLevelDir(dir_mipmaps, k);
 						if (null == target_dir) continue;
 						// 3 - Blur the previous image to 0.75 sigma, and scale it
 						final byte[] r = gaussianBlurResizeInHalf(red, sw, sh, w, h);   // will resize 'red' FloatProcessor in place.
 						final byte[] g = gaussianBlurResizeInHalf(green, sw, sh, w, h); // idem
 						final byte[] b = gaussianBlurResizeInHalf(blue, sw, sh, w, h);  // idem
 						final byte[] a = null == alpha ? null : gaussianBlurResizeInHalf(alpha, sw, sh, w, h); // idem
 						if ( null != outside ) {
 							final byte[] o;
 							if (alpha != outside)
 								o = gaussianBlurResizeInHalf(outside, sw, sh, w, h); // idem
 							else
 								o = a;
 							// Remove all not completely inside pixels from the alphamask
 							// If there was no alpha mask, alpha is the outside itself
 							for (int i=0; i<o.length; i++) {
 								if ( (o[i]&0xff) != 255 ) a[i] = 0; // TODO I am sure there is a bitwise operation to do this in one step. Some thing like: a[i] &= 127;
 							}
 						}
 
 						// 4 - Compose ColorProcessor
 						final int[] pix = new int[w * h];
 						if (null == alpha) {
 							for (int i=0; i<pix.length; i++) {
 								pix[i] = 0xff000000 | ((r[i]&0xff)<<16) | ((g[i]&0xff)<<8) | (b[i]&0xff);
 							}
 							final ColorProcessor cp2 = new ColorProcessor(w, h, pix);
 							// 5 - Save as jpeg
 							if (!ini.trakem2.io.ImageSaver.saveAsJpeg(cp2, target_dir + filename, 0.85f, false)) {
 								cannot_regenerate.add(patch);
 								break;
 							}
 						} else {
 							// LIKELY no need to set alpha raster later in createARGBImage ... TODO
 							for (int i=0; i<pix.length; i++) {
 								pix[i] = ((a[i]&0xff)<<24) | ((r[i]&0xff)<<16) | ((g[i]&0xff)<<8) | (b[i]&0xff);
 							}
							final BufferedImage bi_save = createARGBImage(w, h, pix);
							if (!ini.trakem2.io.ImageSaver.saveAsJpegAlpha(bi_save, target_dir + filename, 0.85f)) {
 								cannot_regenerate.add(patch);
								bi_save.flush();
 								break;
 							}
							bi_save.flush();
 						}
 					} while (w >= 32 && h >= 32); // not smaller than 32x32
 				}
 			} else {
 				// Greyscale:
 				releaseToFit(w * h * 4 * 5);
 				final boolean as_grey = !ip.isColorLut();
 				if (as_grey && null == cm) {
 					cm = GRAY_LUT;
 				}
 
 				if (Loader.GAUSSIAN == resizing_mode) {
 					FloatProcessor fp = (FloatProcessor) ip.convertToFloat();
 					int sw=w, sh=h;
 
 					FloatProcessor alpha;
 					FloatProcessor outside;
 					if (null != alpha_mask) {
 						alpha = new FloatProcessorT2((FloatProcessor)alpha_mask.convertToFloat());
 					} else {
 						alpha = null;
 					}
 					if (null != outside_mask) {
 						outside = new FloatProcessorT2((FloatProcessor)outside_mask.convertToFloat());
 						if (null == alpha) {
 							alpha = outside;
 							alpha_mask = outside_mask;
 						}
 					} else {
 						outside = null;
 					}
 
 					do {
 						// 0 - blur the previous image to 0.75 sigma
 						if (0 != k) { // not doing so at the end because it would add one unnecessary blurring
 							fp = new FloatProcessorT2(sw, sh, ImageFilter.computeGaussianFastMirror(new FloatArray2D((float[])fp.getPixels(), sw, sh), 0.75f).data, cm);
 							if (null != alpha) {
 								alpha = new FloatProcessorT2(sw, sh, ImageFilter.computeGaussianFastMirror(new FloatArray2D((float[])alpha.getPixels(), sw, sh), 0.75f).data, null);
 								if (alpha != outside && outside != null) {
 									outside = new FloatProcessorT2(sw, sh, ImageFilter.computeGaussianFastMirror(new FloatArray2D((float[])outside.getPixels(), sw, sh), 0.75f).data, null);
 								}
 							}
 						}
 						// 1 - check that the target folder for the desired scale exists
 						final String target_dir = getLevelDir(dir_mipmaps, k);
 						if (null == target_dir) continue;
 						// 2 - generate scaled image
 						if (0 != k) {
 							fp = (FloatProcessor)fp.resize(w, h);
 							if (ImagePlus.GRAY8 == type) {
 								fp.setMinAndMax(0, 255); // the min and max was expanded into 0,255 range at convertToFloat for 8-bit images, so the only limit to be added now to the FloatProcessor is that of the 8-bit range. The latter is done automatically for FloatProcessor class, but FloatProcessorT2 doesn't, to avoid the expensive (and here superfluous) operation of looping through all pixels in the findMinAndMax method.
 							} else {
 								fp.setMinAndMax(patch.getMin(), patch.getMax()); // Must be done: the resize doesn't preserve the min and max!
 							}
 							if (null != alpha) {
 								alpha = (FloatProcessor)alpha.resize(w, h);
 								if (alpha != outside && null != outside) {
 									outside = (FloatProcessor)outside.resize(w, h);
 								}
 							}
 						}
 						if (null != alpha) {
 							// 3 - save as jpeg with alpha
 							final byte[] a = (byte[])alpha.convertToByte(false).getPixels();
 							if (null != outside) {
 								final byte[] o;
 								if (alpha != outside) {
 									o = (byte[])outside.convertToByte(false).getPixels();
 								} else {
 									o = a;
 								}
 								// Remove all not completely inside pixels from the alpha mask
 								// If there was no alpha mask, alpha is the outside itself
 								for (int i=0; i<o.length; i++) {
 									if ( (o[i]&0xff) != 255 ) a[i] = 0; // TODO I am sure there is a bitwise operation to do this in one step. Some thing like: a[i] &= 127;
 								}
 							}
 							final int[] pix = embedAlpha((int[])fp.convertToRGB().getPixels(), a);
 
							final BufferedImage bi_save = createARGBImage(w, h, pix);
							if (!ini.trakem2.io.ImageSaver.saveAsJpegAlpha(bi_save, target_dir + filename, 0.85f)) {
 								cannot_regenerate.add(patch);
								bi_save.flush();
 								break;
 							}
							bi_save.flush();
 						} else {
 							// 3 - save as 8-bit jpeg
 							final ImageProcessor ip2 = Utils.convertTo(fp, type, false); // no scaling, since the conversion to float above didn't change the range. This is needed because of the min and max
 							if (!coordinate_transformed) ip2.setMinAndMax(patch.getMin(), patch.getMax()); // Must be done, it's a new ImageProcessor
 							if (null != cm) ip2.setColorModel(cm); // the LUT
 
 							if (!ini.trakem2.io.ImageSaver.saveAsJpeg(ip2, target_dir + filename, 0.85f, as_grey)) {
 								cannot_regenerate.add(patch);
 								break;
 							}
 						}
 
 						// 4 - prepare values for the next scaled image
 						sw = w;
 						sh = h;
 						w /= 2;
 						h /= 2;
 						k++;
 					} while (w >= 32 && h >= 32); // not smaller than 32x32
 
 				} else {
 					//final StopWatch timer = new StopWatch();
 
 					// use java hardware-accelerated resizing
 					Image awt = ip.createImage();
 
 					BufferedImage balpha = null == alpha_mask ? null : convertToBufferedImage(alpha_mask);
 					BufferedImage boutside = null == outside_mask ? null : convertToBufferedImage(outside_mask);
 
 					BufferedImage bi = null;
 					final Object hint = getHint(resizing_mode);
 
 					ip = null;
 
 					do {
 						// check that the target folder for the desired scale exists
 						final String target_dir = getLevelDir(dir_mipmaps, k);
 						if (null == target_dir) continue;
 						// obtain half image
 						//   for level 0 and others, when awt is not a BufferedImage or needs to be reduced in size (to new w,h)
 						final BufferedImage[] res = IToBI(awt, w, h, hint, balpha, boutside);
 						bi = res[0];
 						balpha = res[1];
 						boutside = res[2];
 						// prepare next iteration
 						if (awt != bi) awt.flush();
 						awt = bi;
 						w /= 2;
 						h /= 2;
 						k++;
 						// save this iteration
 						if ( ( (null != balpha || null != boutside) &&
 						      !ini.trakem2.io.ImageSaver.saveAsJpegAlpha(bi, target_dir + filename, 0.85f))
 						   || ( null == balpha && null == boutside && !ini.trakem2.io.ImageSaver.saveAsJpeg(bi, target_dir + filename, 0.85f, as_grey))) {
 							cannot_regenerate.add(patch);
 							break;
 						}
 					} while (w >= 32 && h >= 32);
 					bi.flush();
 
 					//timer.cumulative();
 				}
 			}
 
 			return true;
 		} catch (Throwable e) {
 			IJError.print(e);
 			cannot_regenerate.add(patch);
 			return false;
 		} finally {
 			// gets executed even when returning from the catch statement or within the try/catch block
 			synchronized (gm_lock) {
 				gm_lock();
 				hs_regenerating_mipmaps.remove(patch);
 				gm_unlock();
 			}
 		}
 	}
 
 	/** Remove the file, if it exists, with serialized features for patch.
 	 * Returns true when no such file or on success; false otherwise. */
 	public boolean removeSerializedFeatures(final Patch patch) {
 		final File f = new File(new StringBuffer(dir_storage).append("features.ser/features_").append(patch.getUniqueIdentifier()).append(".ser").toString());
 		if (f.exists()) {
 			try {
 				f.delete();
 				return true;
 			} catch (Exception e) {
 				IJError.print(e);
 				return false;
 			}
 		} else return true;
 	}
 
 	/** Remove the file, if it exists, with serialized point matches for patch.
 	 * Returns true when no such file or on success; false otherwise. */
 	public boolean removeSerializedPointMatches(final Patch patch) {
 		boolean success = true;
 		final File d = new File(new StringBuffer(dir_storage).append("pointmatches.ser").toString());
 		if (d.exists()&&d.isDirectory()) {
 			final String[] files = d.list();
 			if ( files != null )
 			{
 				for ( final String f : files ) {
 					if ( f.matches( ".*_" + patch.getUniqueIdentifier() + "(_|\\.).*" ) ) {
 						try {
 							new File( d.getAbsolutePath() + "/" + f ).delete();
 						} catch (Exception e) {
 							IJError.print(e);
 							success = false;
 						}
 					}
 				}
 			}
 		}
 		return success;
 	}
 
 	/** Generate image pyramids and store them into files under the dir_mipmaps for each Patch object in the Project. The method is multithreaded, using as many processors as available to the JVM.
 	 *
 	 * @param al : the list of Patch instances to generate mipmaps for.
 	 * @param overwrite : whether to overwrite any existing mipmaps, or save only those that don't exist yet for whatever reason. This flag provides the means for minimal effort mipmap regeneration.)
 	 * */
 	public Bureaucrat generateMipMaps(final ArrayList al, final boolean overwrite) {
 		if (null == al || 0 == al.size()) return null;
 		if (null == dir_mipmaps) createMipMapsDir(null);
 		if (isURL(dir_mipmaps)) {
 			Utils.log("Mipmaps folder is an URL, can't save files into it.");
 			return null;
 		}
 		final Worker worker = new Worker("Generating MipMaps") {
 			public void run() {
 				this.setAsBackground(true);
 				this.startedWorking();
 				try {
 
 				final Worker wo = this;
 
 				Utils.log2("starting mipmap generation ..");
 
 				final int size = al.size();
 				final Patch[] pa = new Patch[size];
 				final Thread[] threads = MultiThreading.newThreads();
 				al.toArray(pa);
 				final AtomicInteger ai = new AtomicInteger(0);
 
 				for (int ithread = 0; ithread < threads.length; ++ithread) {
 					threads[ithread] = new Thread(new Runnable() {
 						public void run() {
 
 				for (int k = ai.getAndIncrement(); k < size; k = ai.getAndIncrement()) {
 					if (wo.hasQuitted()) {
 						return;
 					}
 					wo.setTaskName("Generating MipMaps " + (k+1) + "/" + size);
 					try {
 						boolean ow = overwrite;
 						if (!overwrite) {
 							// check if all the files exist. If one doesn't, then overwrite all anyway
 							int w = (int)pa[k].getWidth();
 							int h = (int)pa[k].getHeight();
 							int level = 0;
 							final String filename = new File(getAbsolutePath(pa[k])).getName() + "." + pa[k].getId() + ".jpg";
 							do {
 								w /= 2;
 								h /= 2;
 								level++;
 								if (!new File(dir_mipmaps + level + "/" + filename).exists()) {
 									ow = true;
 									break;
 								}
 							} while (w >= 32 && h >= 32);
 						}
 						if (!ow) continue;
 						if ( ! generateMipMaps(pa[k]) ) {
 							// some error ocurred
 							Utils.log2("Could not generate mipmaps for patch " + pa[k]);
 						}
 					} catch (Exception e) {
 						IJError.print(e);
 					}
 				}
 
 						}
 					});
 				}
 				MultiThreading.startAndJoin(threads);
 
 				} catch (Exception e) {
 					IJError.print(e);
 				}
 
 				this.finishedWorking();
 			}
 		};
 		return Bureaucrat.createAndStart(worker, ((Patch)al.get(0)).getProject());
 	}
 
 	private final String getLevelDir(final String dir_mipmaps, final int level) {
 		// synch, so that multithreaded generateMipMaps won't collide trying to create dirs
 		synchronized (db_lock) {
 			lock();
 			final String path = new StringBuffer(dir_mipmaps).append(level).append('/').toString();
 			if (isURL(dir_mipmaps)) {
 				unlock();
 				return path;
 			}
 			final File file = new File(path);
 			if (file.exists() && file.isDirectory()) {
 				unlock();
 				return path;
 			}
 			// else, create it
 			try {
 				file.mkdir();
 				unlock();
 				return path;
 			} catch (Exception e) {
 				IJError.print(e);
 			}
 			unlock();
 		}
 		return null;
 	}
 
 	/** If parent path is null, it's asked for.*/
 	public boolean createMipMapsDir(String parent_path) {
 		if (null == parent_path) {
 			// try to create it in the same directory where the XML file is
 			if (null != dir_storage) {
 				File f = new File(dir_storage + "trakem2.mipmaps");
 				if (!f.exists()) {
 					try {
 						if (f.mkdir()) {
 							this.dir_mipmaps = f.getAbsolutePath().replace('\\', '/');
 							if (!dir_mipmaps.endsWith("/")) this.dir_mipmaps += "/";
 							return true;
 						}
 					} catch (Exception e) {}
 				} else if (f.isDirectory()) {
 					this.dir_mipmaps = f.getAbsolutePath().replace('\\', '/');
 					if (!dir_mipmaps.endsWith("/")) this.dir_mipmaps += "/";
 					return true;
 				}
 				// else can't use it
 			} else if (null != project_file_path) {
 				final File fxml = new File(project_file_path);
 				final File fparent = fxml.getParentFile();
 				if (null != fparent && fparent.isDirectory()) {
 					File f = new File(fparent.getAbsolutePath().replace('\\', '/') + "/" + fxml.getName() + ".mipmaps");
 					try {
 						if (f.mkdir()) {
 							this.dir_mipmaps = f.getAbsolutePath().replace('\\', '/');
 							if (!dir_mipmaps.endsWith("/")) this.dir_mipmaps += "/";
 							return true;
 						}
 					} catch (Exception e) {}
 				}
 			}
 			// else,  ask for a new folder
 			final DirectoryChooser dc = new DirectoryChooser("Select MipMaps parent directory");
 			parent_path = dc.getDirectory();
 			if (null == parent_path) return false;
 			if (!parent_path.endsWith("/")) parent_path += "/";
 		}
 		// examine parent path
 		final File file = new File(parent_path);
 		if (file.exists()) {
 			if (file.isDirectory()) {
 				// all OK
 				this.dir_mipmaps = parent_path + "trakem2.mipmaps/";
 				try {
 					File f = new File(this.dir_mipmaps);
 					if (!f.exists()) {
 						f.mkdir();
 					}
 				} catch (Exception e) {
 					IJError.print(e);
 					return false;
 				}
 			} else {
 				Utils.showMessage("Selected parent path is not a directory. Please choose another one.");
 				return createMipMapsDir(null);
 			}
 		} else {
 			Utils.showMessage("Parent path does not exist. Please select a new one.");
 			return createMipMapsDir(null);
 		}
 		return true;
 	}
 
 	/** Remove all mipmap images from the cache, and optionally set the dir_mipmaps to null. */
 	public void flushMipMaps(boolean forget_dir_mipmaps) {
 		if (null == dir_mipmaps) return;
 		synchronized (db_lock) {
 			lock();
 			if (forget_dir_mipmaps) this.dir_mipmaps = null;
 			mawts.removeAllPyramids(); // does not remove level 0 awts (i.e. the 100% images)
 			unlock();
 		}
 	}
 
 	/** Remove from the cache all images of level larger than zero corresponding to the given patch id. */
 	public void flushMipMaps(final long id) {
 		if (null == dir_mipmaps) return;
 		synchronized (db_lock) {
 			lock();
 			try {
 				//mawts.removePyramid(id); // does not remove level 0 awts (i.e. the 100% images)
 				// Need to remove ALL now, since level 0 is also included as a mipmap:
 				for (final Image img : mawts.remove(id)) {
 					if (null != img) img.flush();
 				}
 			} catch (Exception e) { e.printStackTrace(); }
 			unlock();
 		}
 	}
 
 	/** Gets data from the Patch and queues a new task to do the file removal in a separate task manager thread. */
 	public void removeMipMaps(final Patch p) {
 		if (null == dir_mipmaps) return;
 		try {
 			final int width = (int)p.getWidth();
 			final int height = (int)p.getHeight();
 			final String path = getAbsolutePath(p);
 			if (null == path) return; // missing file
 			final String filename = new File(path).getName() + "." + p.getId() + ".jpg";
 			// cue the task in a dispatcher:
 			dispatcher.exec(new Runnable() { public void run() { // copy-paste as a replacement for (defmacro ... we luv java
 				removeMipMaps(filename, width, height);
 			}});
 		} catch (Exception e) {
 			IJError.print(e);
 		}
 	}
 
 	private void removeMipMaps(final String filename, final int width, final int height) {
 		int w = width;
 		int h = height;
 		int k = 0; // the level
 		do {
 			final File f = new File(dir_mipmaps + k + "/" + filename);
 			if (f.exists()) {
 				try {
 					if (!f.delete()) {
 						Utils.log2("Could not remove file " + f.getAbsolutePath());
 					}
 				} catch (Exception e) {
 					IJError.print(e);
 				}
 			}
 			w /= 2;
 			h /= 2;
 			k++;
 		} while (w >= 32 && h >= 32); // not smaller than 32x32
 	}
 
 	/** Checks whether this Loader is using a directory of image pyramids for each Patch or not. */
 	public boolean isMipMapsEnabled() {
 		return null != dir_mipmaps;
 	}
 
 	/** Return the closest level to @param level that exists as a file.
 	 *  If no valid path is found for the patch, returns ERROR_PATH_NOT_FOUND.
 	 */
 	public int getClosestMipMapLevel(final Patch patch, int level) {
 		if (null == dir_mipmaps) return 0;
 		try {
 			final String path = getAbsolutePath(patch);
 			if (null == path) return ERROR_PATH_NOT_FOUND;
 			final String filename = new File(path).getName() + ".jpg";
 			if (isURL(dir_mipmaps)) {
 				if (level <= 0) return 0;
 				// choose the smallest dimension
 				// find max level that keeps dim over 32 pixels
 				final int lev = getHighestMipMapLevel(Math.min(patch.getWidth(), patch.getHeight()));
 				if (level > lev) return lev;
 				return level;
 			} else {
 				do {
 					final File f = new File(new StringBuffer(dir_mipmaps).append(level).append('/').append(filename).toString());
 					if (f.exists()) {
 						return level;
 					}
 					// try the next level
 					level--;
 				} while (level >= 0);
 			}
 		} catch (Exception e) {
 			IJError.print(e);
 		}
 		return 0;
 	}
 
 	/** A temporary list of Patch instances for which a pyramid is being generated. */
 	final private HashSet hs_regenerating_mipmaps = new HashSet();
 
 	/** A lock for the generation of mipmaps. */
 	final private Object gm_lock = new Object();
 	private boolean gm_locked = false;
 
 	protected final void gm_lock() {
 		//Utils.printCaller(this, 7);
 		while (gm_locked) { try { gm_lock.wait(); } catch (InterruptedException ie) {} }
 		gm_locked = true;
 	}
 	protected final void gm_unlock() {
 		//Utils.printCaller(this, 7);
 		if (gm_locked) {
 			gm_locked = false;
 			gm_lock.notifyAll();
 		}
 	}
 
 	/** Checks if the mipmap file for the Patch and closest upper level to the desired magnification exists. */
 	public boolean checkMipMapFileExists(final Patch p, final double magnification) {
 		if (null == dir_mipmaps) return false;
 		final int level = getMipMapLevel(magnification, maxDim(p));
 		if (isURL(dir_mipmaps)) return true; // just assume that it does
 		if (new File(dir_mipmaps + level + "/" + new File(getAbsolutePath(p)).getName() + "." + p.getId() + ".jpg").exists()) return true;
 		return false;
 	}
 
 	final HashSet<Patch> cannot_regenerate = new HashSet<Patch>();
 
 	/** Loads the file containing the scaled image corresponding to the given level (or the maximum possible level, if too large) and returns it as an awt.Image, or null if not found. Will also regenerate the mipmaps, i.e. recreate the pre-scaled jpeg images if they are missing. Does not frees memory on its own. */
 	protected Image fetchMipMapAWT(final Patch patch, final int level) {
 		if (null == dir_mipmaps) {
 			Utils.log2("null dir_mipmaps");
 			return null;
 		}
 		try {
 			// TODO should wait if the file is currently being generated
 			//  (it's somewhat handled by a double-try to open the jpeg image)
 
 			final int max_level = getHighestMipMapLevel(patch);
 
 			//Utils.log2("level is: " + max_level);
 
 			final String filepath = getInternalFileName(patch);
 			if (null == filepath) {
 				Utils.log2("null filepath!");
 				return null;
 			}
 			final String path = new StringBuffer(dir_mipmaps).append( level > max_level ? max_level : level ).append('/').append(filepath).append('.').append(patch.getId()).append(".jpg").toString();
 			Image img = null;
 
 			if (patch.hasAlphaChannel()) {
 				img = ImageSaver.openJpegAlpha(path);
 			} else {
 				switch (patch.getType()) {
 					case ImagePlus.GRAY16:
 					case ImagePlus.GRAY8:
 					case ImagePlus.GRAY32:
 						img = ImageSaver.openGreyJpeg(path);
 						break;
 					default:
 						IJ.redirectErrorMessages();
 						ImagePlus imp = opener.openImage(path); // considers URL as well
 						if (null != imp) return patch.createImage(imp); // considers c_alphas
 						//img = patch.adjustChannels(Toolkit.getDefaultToolkit().createImage(path)); // doesn't work
 						//img = patch.adjustChannels(ImageSaver.openColorJpeg(path)); // doesn't work
 						//Utils.log2("color jpeg path: "+ path);
 						//Utils.log2("exists ? " + new File(path).exists());
 						break;
 				}
 			}
 			if (null != img) return img;
 
 
 			// if we got so far ... try to regenerate the mipmaps
 			if (!mipmaps_regen) {
 				return null;
 			}
 
 			// check that REALLY the file doesn't exist.
 			if (cannot_regenerate.contains(patch)) {
 				Utils.log("Cannot regenerate mipmaps for patch " + patch);
 				return null;
 			}
 
 			//Utils.log2("getMipMapAwt: imp is " + imp + " for path " +  dir_mipmaps + level + "/" + new File(getAbsolutePath(patch)).getName() + "." + patch.getId() + ".jpg");
 
 			// Regenerate in the case of not asking for an image under 32x32
 			double scale = 1 / Math.pow(2, level);
 			if (level >= 0 && patch.getWidth() * scale >= 32 && patch.getHeight() * scale >= 32 && isMipMapsEnabled()) {
 				// regenerate
 				synchronized (gm_lock) {
 					gm_lock();
 
 					if (hs_regenerating_mipmaps.contains(patch)) {
 						// already being done
 						gm_unlock();
 						return null;
 					}
 					// else, start it
 					Worker worker = new Worker("Regenerating mipmaps") {
 						public void run() {
 							this.setAsBackground(true);
 							this.startedWorking();
 							try {
 								generateMipMaps(patch);
 								flushMipMaps(patch.getId());
 							} catch (Exception e) {
 								IJError.print(e);
 							}
 
 							Display.repaint(patch.getLayer(), patch, 0);
 
 							this.finishedWorking();
 						}
 					};
 					Bureaucrat burro = Bureaucrat.create(worker, patch.getProject());
 					burro.goHaveBreakfast();
 
 					gm_unlock();
 				}
 				return null;
 			}
 		} catch (Exception e) {
 			IJError.print(e);
 		}
 		return null;
 	}
 
 	/** Compute the number of bytes that the ImagePlus of a Patch will take. Assumes a large header of 1024 bytes. If the image is saved as a grayscale jpeg the returned bytes will be 5 times as expected, because jpeg images are opened as int[] and then copied to a byte[] if all channels have the same values for all pixels. */ // The header is unnecessary because it's read, but not stored except for some of its variables; it works here as a safety buffer space.
 	public long estimateImageFileSize(final Patch p, final int level) {
 		if (level > 0) {
 			// jpeg image to be loaded:
 			final double scale = 1 / Math.pow(2, level);
 			return (long)(p.getWidth() * scale * p.getHeight() * scale * 5 + 1024);
 		}
 		long size = (long)(p.getWidth() * p.getHeight());
 		int bytes_per_pixel = 1;
 		final int type = p.getType();
 		switch (type) {
 			case ImagePlus.GRAY32:
 				bytes_per_pixel = 5; // 4 for the FloatProcessor, and 1 for the pixels8 to make an image
 				break;
 			case ImagePlus.GRAY16:
 				bytes_per_pixel = 3; // 2 for the ShortProcessor, and 1 for the pixels8
 			case ImagePlus.COLOR_RGB:
 				bytes_per_pixel = 4;
 				break;
 			case ImagePlus.GRAY8:
 			case ImagePlus.COLOR_256:
 				bytes_per_pixel = 1;
 				// check jpeg, which can only encode RGB (taken care of above) and 8-bit and 8-bit color images:
 				String path = ht_paths.get(p.getId());
 				if (null != path && path.endsWith(".jpg")) bytes_per_pixel = 5; //4 for the int[] and 1 for the byte[]
 				break;
 			default:
 				bytes_per_pixel = 5; // conservative
 				break;
 		}
 
 		return size * bytes_per_pixel + 1024;
 	}
 
 	public String makeProjectName() {
 		if (null == project_file_path || 0 == project_file_path.length()) return super.makeProjectName();
 		final String name = new File(project_file_path).getName();
 		final int i_dot = name.lastIndexOf('.');
 		if (-1 == i_dot) return name;
 		if (0 == i_dot) return super.makeProjectName();
 		return name.substring(0, i_dot);
 	}
 
 
 	/** Returns the path where the imp is saved to: the storage folder plus a name. */
 	public String handlePathlessImage(final ImagePlus imp) {
 		final FileInfo fi = imp.getOriginalFileInfo();
 		if (null == fi.fileName || fi.fileName.equals("")) {
 			fi.fileName = "img_" + System.currentTimeMillis() + ".tif";
 		}
 		if (!fi.fileName.endsWith(".tif")) fi.fileName += ".tif";
 		fi.directory = dir_storage;
 		if (imp.getNSlices() > 1) {
 			new FileSaver(imp).saveAsTiffStack(dir_storage + fi.fileName);
 		} else {
 			new FileSaver(imp).saveAsTiff(dir_storage + fi.fileName);
 		}
 		Utils.log2("Saved a copy into the storage folder:\n" + dir_storage + fi.fileName);
 		return dir_storage + fi.fileName;
 	}
 
 	/** Generates layer-wise mipmaps with constant tile width and height. The mipmaps include only images.
 	 *  Mipmaps area generated all the way down until the entire canvas fits within one single tile.
 	 */
 	public Bureaucrat generateLayerMipMaps(final Layer[] la, final int starting_level) {
 		// hard-coded dimensions for layer mipmaps.
 		final int WIDTH = 512;
 		final int HEIGHT = 512;
 		//
 		// Each tile needs some coding system on where it belongs. For example in its file name, such as <layer_id>_Xi_Yi
 		// 
 		// Generate the starting level mipmaps, and then the others from it by gaussian or whatever is indicated in the project image_resizing_mode property.
 		return null;
 	}
 }
