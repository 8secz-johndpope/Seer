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
 import ini.trakem2.imaging.VirtualStack; //import ij.VirtualStack; // only after 1.38q
 import ij.io.*;
 import ij.process.ImageProcessor;
 import ij.process.FloatProcessor;
 import ij.process.ColorProcessor;
 import ij.gui.YesNoCancelDialog;
 import ini.trakem2.Project;
 import ini.trakem2.ControlWindow;
 import ini.trakem2.display.Ball;
 import ini.trakem2.display.DLabel;
 import ini.trakem2.display.Display;
 import ini.trakem2.display.Displayable;
 import ini.trakem2.display.Layer;
 import ini.trakem2.display.LayerSet;
 import ini.trakem2.display.Patch;
 import ini.trakem2.display.Pipe;
 import ini.trakem2.display.Profile;
 import ini.trakem2.display.YesNoDialog;
 import ini.trakem2.display.ZDisplayable;
 import ini.trakem2.tree.Attribute;
 import ini.trakem2.tree.LayerThing;
 import ini.trakem2.tree.ProjectAttribute;
 import ini.trakem2.tree.ProjectThing;
 import ini.trakem2.tree.TemplateThing;
 import ini.trakem2.tree.TemplateAttribute;
 import ini.trakem2.tree.Thing;
 import ini.trakem2.tree.TrakEM2MLParser;
 import ini.trakem2.tree.DTDParser;
 import ini.trakem2.utils.*;
 import ini.trakem2.io.*;
 
 import java.awt.Color;
 import java.awt.Cursor;
 import java.awt.Font;
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.awt.Point;
 import java.awt.Rectangle;
 import java.awt.Toolkit;
 import java.awt.image.BufferedImage;
 import java.awt.image.IndexColorModel;
 import java.awt.image.ColorModel;
 import java.awt.geom.Area;
 import java.awt.geom.AffineTransform;
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.DataOutputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileReader;
 import java.io.FilenameFilter;
 import java.io.IOException;
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
 public class FSLoader extends Loader {
 
 	/** Largest id seen so far. */
 	private long max_id = -1;
 	private final Hashtable<Long,String> ht_paths = new Hashtable<Long,String>();
 	/** For saving and overwriting. */
 	private String project_file_path = null;
 	/** Path to the directory hosting the file image pyramids. */
 	private String dir_mipmaps = null;
 	/** Path to the directory the user provided when creating the project. */
 	private String dir_storage = null;
 
 	public FSLoader() {
 		super(); // register
 		super.v_loaders.remove(this); //will be readded on successful open
 	}
 
 	public FSLoader(final String storage_folder) {
 		this();
 		if (null == storage_folder) this.dir_storage = super.getStorageFolder(); // home dir
 		else this.dir_storage = storage_folder;
 		if (!this.dir_storage.endsWith("/")) this.dir_storage += "/";
 		createMipMapsDir(this.dir_storage);
 	}
 
 	/** Create a new FSLoader copying some key parameters such as preprocessor plugin, and storage and mipmap folders.*/
 	public FSLoader(final Loader source) {
 		this();
 		this.dir_storage = source.getStorageFolder(); // can never be null
 		this.dir_mipmaps = source.getMipMapsFolder();
 		if (null == this.dir_mipmaps) createMipMapsDir(this.dir_storage);
 		setPreprocessor(source.getPreprocessor());
 	}
 
 	public String getProjectXMLPath() {
 		if (null == project_file_path) return null;
 		return project_file_path.toString(); // a copy of it
 	}
 
 	public String getStorageFolder() {
 		if (null == dir_storage) return super.getStorageFolder(); // the user's home
 		return dir_storage.toString(); // a copy
 	}
 
 	/** Returns TMLHandler.getProjectData() . If the path is null it'll be asked for. */
 	public Object[] openFSProject(String path) {
 		// clean path of double-slashes, safely (and painfully)
		if (null != path) {
			path = path.replace('\\','/');
			path = path.trim();
			int itwo = path.indexOf("//");
			while (-1 != itwo) {
				if (0 == itwo) continue; // samba disk
				if (5 == itwo  && "http:".equals(path.substring(0, 5))) continue;
				path = path.substring(0, itwo) + path.substring(itwo+1);
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
 
 			data = handler.getProjectData();
 		} else if (this.project_file_path.toLowerCase().endsWith(".t2")) {
 			data = Decoder.read(this.project_file_path, this);
 		}
 
 		if (null == data) {
 			Utils.showMessage("Error when parsing the project .xml file.");
 			return null;
 		}
 		// else, good
 		super.v_loaders.add(this);
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
 		Utils.showStatus("");
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
 	}
 
 	/** Get the next unique id, not shared by any other object within the same project. */
 	public long getNextId() {
 		long nid = -1;
 		synchronized (db_lock) {
 			lock();
 			nid = max_id + 1;
 			max_id = nid;
 			unlock();
 		}
 		return nid;
 	}
 
 	public double[][][] fetchBezierArrays(long id) { // profiles are loaded in full from the XML file
 		return null;
 	}
 
 	public ArrayList fetchPipePoints(long id) {
 		return null;
 	}
 
 	public ArrayList fetchBallPoints(long id) {
 		return null;
 	}
 
 	public Area fetchArea(long area_list_id, long layer_id) { // loaded in full from XML file
 		return null;
 	}
 
 	public ImagePlus fetchImagePlus(final Patch p) {
 		return (ImagePlus)fetchImage(p, Layer.IMAGEPLUS);
 	}
 
 	/** Fetch the ImageProcessor in a synchronized manner, so that there are no conflicts in retrieving the ImageProcessor for a specific stack slice, for example. */
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
 				releaseMemory(); // ensure there is a minimum % of free memory
 				if (-1 != i_sl) {
 					slice = path.substring(i_sl);
 					// set path proper
 					path = path.substring(0, i_sl);
 				}
 
 				plock = getOrMakePatchLoadingLock(p, 0);
 				unlock();
 			} catch (Exception e) {
 				IJError.print(e);
 				return null;
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
 				// not needed // releaseToFit2(n_bytes);
 				max_memory -= n_bytes;
 				unlock();
 			}
 
 			imp = openImage(path);
 			preProcess(imp);
 
 			synchronized (db_lock) {
 				try {
 					lock();
 					max_memory += n_bytes;
 
 					if (null == imp) {
 						Utils.log("FSLoader.fetchImagePlus: no image exists for patch  " + p + "  at path " + path);
 						removePatchLoadingLock(plock);
 						unlock();
 						plock.unlock();
 						return null;
 					}
 					// update all clients of the stack, if any
 					if (null != slice) {
 						String rel_path = getPath(p); // possibly relative
 						int r_isl = rel_path.lastIndexOf("-----#slice");
 						if (-1 != r_isl) rel_path = rel_path.substring(0, r_isl); // should always happen
 						for (Iterator it = ht_paths.entrySet().iterator(); it.hasNext(); ) {
 							Map.Entry entry = (Map.Entry)it.next();
 							String str = (String)entry.getValue(); // this is like calling getPath(p)
 							//Utils.log2("processing " + str);
 							if (0 != str.indexOf(rel_path)) {
 								Utils.log2("SKIP str is: " + str + "\t but path is: " + rel_path);
 								continue; // get only those whose path is identical, of course!
 							}
 							int isl = str.lastIndexOf("-----#slice=");
 							if (-1 != isl) {
 								//int i_slice = Integer.parseInt(str.substring(isl + 12));
 								long lid = ((Long)entry.getKey()).longValue(); // really, J jate java
 								imps.put(lid, imp);
 								imp.setSlice(Integer.parseInt(str.substring(isl + 12)));
 								// kludge, but what else short of a gigantic hashtable
 								try {
 									final Patch pa = (Patch)p.getLayerSet().findDisplayable(lid);
 									pa.putMinAndMax(imp);
 								} catch (Exception e) {
 									e.printStackTrace();
 								}
 							}
 						}
 						// set proper active slice
 						int ia = Integer.parseInt(slice.substring(12));
 						imp.setSlice(ia);
 						if (Layer.IMAGEPROCESSOR == format) ip = imp.getStack().getProcessor(ia); // otherwise creates one new for nothing
 					} else {
 						// for non-stack images
 						p.putMinAndMax(imp); // non-destructive contrast: min and max
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
 
 	public Object[] fetchLabel(DLabel label) {
 		return null;
 	}
 
 	/** Does NOT return the original image, but the working. The original image is not stored if a working image exists. */
 	synchronized public ImagePlus fetchOriginal(Patch patch) {
 		Utils.showMessage("The original image was NOT stored.");
 		return fetchImagePlus(patch);
 	}
 
 	public void prepare(Layer layer) {
 		//Utils.log2("FSLoader.prepare(Layer): not implemented.");
 		super.prepare(layer);
 	}
 
 	/* GENERIC, from DBObject calls. Records the id of the object in the Hashtable ht_dbo.
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
 		if (ob.getClass().equals(Patch.class)) {
 			try {
 				Patch p = (Patch)ob;
 				if (key.equals("tiff_working")) {
 					String path = getAbsolutePath(p);
 					String slice = null;
 					// path can be null if the image is pasted, or from a copy
 					if (null != path) {
 						int i_sl = path.lastIndexOf("-----#slice=");
 						if (-1 != i_sl) {
 							slice = path.substring(i_sl);
 							path = path.substring(0, i_sl);
 						}
 					}
 					boolean overwrite = null != path;
 					if (overwrite) {
 						//Utils.printCaller(this, 10);
 						YesNoCancelDialog yn = new YesNoCancelDialog(IJ.getInstance(), "Overwrite?", "Overwrite '" + p.getTitle() + "'\nat " + path + "' ?");
 						if (yn.cancelPressed()) {
 							return false;
 						} else if (!yn.yesPressed()) { // so, 'no' button pressed
 							overwrite = false;
 						}
 					}
 					if (overwrite) {
 						// save as a separate image and update path
 						path = path.substring(0, path.lastIndexOf('/') + 1) + p.getTitle();
 						if (path.length() -4 == path.lastIndexOf(".tif")) {
 							path = path.substring(0, path.length() -4);
 						}
 						// remove previous time stamp, if any
 						int i = 0;
 						for (i=path.length()-1; i>-1; i--) {
 							if (Character.isDigit(path.charAt(i))) continue;
 							break;
 						}
 						if (-1 != i && path.length() - i >= 12) { // otherwise it may not be a time stamp
 							path = path.substring(0, i+1);
 						} else {
 							path += "_";
 						}
 						path += System.currentTimeMillis() + ".tif";
 						path = super.exportImage(p, path, true);
 						Utils.log2("Saved original " + ht_paths.get(p.getId()) + "\n\t at: " + path);
 						// make path relative if possible
 						if (null != project_file_path) { // project may be unsaved
 							File fxml = new File(project_file_path);
 							String parent_dir = fxml.getParent().replace('\\', '/');
 							if (!parent_dir.endsWith("/")) parent_dir += "/";
 							if (0 == path.indexOf((String)ht_paths.get(p.getId()))) {
 								path = path.substring(path.length());
 							}
 						}
 						// update paths' hashtable
 						if (null == slice) {
 							updatePatchPath(p, path);
 						} else {
 							// update all slices
 							for (Iterator it = ht_paths.entrySet().iterator(); it.hasNext(); ) {
 								Map.Entry entry = (Map.Entry)it.next();
 								String str = (String)entry.getValue();
 								int isl = str.lastIndexOf("-----#slice=");
 								if (-1 != isl) {
 									entry.setValue(path + str.substring(isl));
 								}
 							}
 						}
 						return true;
 					} else {
 						// else 'yes' button pressed: overwrite image file
 						Utils.log2("overwriting image at " + path);
 						exportImage(p, path, true);
 					}
 				}
 			} catch (Exception e) {
 				IJError.print(e);
 				return false;
 			}
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
 			if (ob instanceof Patch) {
 				// STRATEGY change: images are not owned by the FSLoader.
 				Patch p = (Patch)ob;
 				removeMipMaps(p);
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
 				unlock();
 				flushMipMaps(p.getId()); // locks on its own
 				return true;
 			}
 			unlock();
 		}
 		return true;
 	}
 
 	/** With slice info appended at the end; only if it exists, otherwise null. */
 	public String getAbsolutePath(final Patch patch) {
 		Object ob = ht_paths.get(patch.getId());
 		if (null == ob) return null;
 		String path = (String)ob;
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
 		// if path is absolute, try to make it relative
 		path = makeRelativePath(path);
 		// store
 		ht_paths.put(patch.getId(), path);
 		//Utils.log2("Updated patch path " + ht_paths.get(patch.getId()) + " for patch " + patch);
 	}
 
 	/** Overriding superclass method; if a path for the given Patch exists in the ht_paths Hashtable, it will be returned (meaning, the image exists already); if not, then an attempt is made to save it in the given path. The overwrite flag is used in the second case, to avoid creating a new image every time the Patch pixels are edited. The original image is never overwritten in any case. */
 	public String exportImage(Patch patch, String path, boolean overwrite) {
 		Object ob = ht_paths.get(patch.getId());
 		if (null == ob) {
 			// means, the image has no related source file
 			if (null == path) {
 				if (null == project_file_path) {
 					this.project_file_path = save(patch.getProject());
 					if (null == project_file_path) {
 						return null;
 					}
 				}
 				path = makePatchesDir(new File(project_file_path)) + "/" + patch.getTitle().replace(' ', '_');
 			}
 			path = super.exportImage(patch, path, overwrite);
 			// record
 			Utils.log2("patch: " + patch + "  and path: " + path);
 			updatePatchPath(patch, path);
 			return path;
 		} else {
 			//Utils.printCaller(this, 7);
 			//Utils.log2("FSLoader.exportImage: path is " + ob.toString());
 			return (String)ob; // the path of the source image file
 		}
 	}
 
 	/** Overwrites the XML file. If some images do not exist in the file system, a directory with the same name of the XML file plus an "_images" tag appended will be created and images saved there. */
 	public String save(final Project project) {
 		Utils.logAll(Utils.now() + " Saving " + project);
 		if (null == project_file_path) {
 			String xml_path = super.saveAs(project);
 			if (null == xml_path) return null;
 			else {
 				this.project_file_path = xml_path;
 				ControlWindow.updateTitle(project);
 				return this.project_file_path;
 			}
 		} else {
 			File fxml = new File(project_file_path);
 			return super.export(project, fxml, false);
 		}
 	}
 
 	public String saveAs(Project project) {
 		String path = super.saveAs(project);
 		if (null != path) {
 			// update the xml path to point to the new one
 			this.project_file_path = path;
 			Utils.log2("After saveAs, new xml path is: " + path);
 		}
 		ControlWindow.updateTitle(project);
 		return path;
 	}
 
 	/** Returns the stored path for the given Patch image, which may be relative.*/
 	public String getPath(final Patch patch) {
 		final Object ob = ht_paths.get(patch.getId());
 		return (String)ob; // casting a null is ok
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
 	protected Patch importStackAsPatches(final Project project, final Layer first_layer, final ImagePlus imp_stack, final boolean as_copy, final String filepath) {
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
 
 		int pos_x = (int)first_layer.getLayerWidth()/2 - imp_stack.getWidth()/2;
 		int pos_y = (int)first_layer.getLayerHeight()/2 - imp_stack.getHeight()/2;
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
 			addedPatchFrom(patch_path, patch);
 			if (!as_copy) {
 				cache(patch, imp_stack); // uses the entire stack, shared among all Patch instances
 			}
 			if (isMipMapsEnabled()) generateMipMaps(patch);
 			if (null != previous_patch) patch.link(previous_patch);
 			layer.add(patch);
 			previous_patch = patch;
 			Utils.showProgress(i * (1.0 / n));
 		}
 		Utils.showProgress(1.0);
 		// update calibration
 		//if ( TODO
 		// return the last patch
 		return previous_patch;
 	}
 
 	/** Specific options for the Loader which exist as attributes to the Project XML node. */
 	public void parseXMLOptions(final Hashtable ht_attributes) {
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
 				Utils.log2("Asking user Yes/No to generate mipmaps on the background."); // tip for headless runners whose program gets "stuck"
 				YesNoDialog yn = new YesNoDialog(IJ.getInstance(), "Generate mipmaps", "Generate mipmaps in the background for all images?");
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
 		}
 		// fix
 		if (null != this.dir_mipmaps && !this.dir_mipmaps.endsWith("/")) this.dir_mipmaps += "/";
 		Utils.log2("mipmaps folder is " + this.dir_mipmaps);
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
 			return false;
 		}
 		synchronized (gm_lock) {
 			gm_lock();
 			if (hs_regenerating_mipmaps.contains(patch)) {
 				// already being done
 				gm_unlock();
 				Utils.log2("already being done");
 				return false;
 			}
 			hs_regenerating_mipmaps.add(patch);
 			gm_unlock();
 		}
 		boolean ok = true;
 		try {
 			final ImageProcessor ip = fetchImageProcessor(patch);
 			final String filename = new File(path).getName() + "." + patch.getId() + ".jpg";
 			int w = ip.getWidth();
 			int h = ip.getHeight();
 			// sigma = sqrt(2^level - 0.5^2)
 			//    where 0.5 is the estimated sigma for a full-scale image
 			//  which means sigma = 0.75 for the full-scale image (has level 0)
 			// prepare a 0.75 sigma image from the original
 			final ColorModel cm = ip.getColorModel();
 			int k = 0; // the scale level. Proper scale is: 1 / pow(2, k)
 				   //   but since we scale 50% relative the previous, it's always 0.75
 			if (ImagePlus.COLOR_RGB == patch.getType()) {
 				// TODO releaseToFit proper
 				releaseToFit(w * h * 4 * 5);
 				ColorProcessor cp = (ColorProcessor)ip;
 				FloatProcessor red = cp.toFloat(0, new FloatProcessor(w, h));
 				FloatProcessor green = cp.toFloat(1, new FloatProcessor(w, h));
 				FloatProcessor blue = cp.toFloat(2, new FloatProcessor(w, h));
 				while (w >= 64 && h >= 64) { // not smaller than 32x32
 					// 1 - blur the previous image to 0.75 sigma
 					red = new FloatProcessor(w, h, ImageFilter.computeGaussianFastMirror(new FloatArray2D((float[])red.getPixels(), w, h), 0.75f).data, cm);
 					green = new FloatProcessor(w, h, ImageFilter.computeGaussianFastMirror(new FloatArray2D((float[])green.getPixels(), w, h), 0.75f).data, cm);
 					blue = new FloatProcessor(w, h, ImageFilter.computeGaussianFastMirror(new FloatArray2D((float[])blue.getPixels(), w, h), 0.75f).data, cm);
 					// 2 - prepare values for the next scaled image
 					w /= 2;
 					h /= 2;
 					k++;
 					// 3 - check that the target folder for the desired scale exists
 					String target_dir = getScaleDir(dir_mipmaps, k);
 					if (null == target_dir) continue;
 					// 4 - generate scaled image
 					red = (FloatProcessor)red.resize(w, h);
 					green = (FloatProcessor)green.resize(w, h);
 					blue = (FloatProcessor)blue.resize(w, h);
 					// compose ColorProcessor
 					int[] pix = new int[w * h];
 					byte[] r = (byte[])red.convertToByte(false).getPixels();
 					byte[] g = (byte[])green.convertToByte(false).getPixels();
 					byte[] b = (byte[])blue.convertToByte(false).getPixels();
 					for (int i=0; i<pix.length; i++) {
 						pix[i] = (r[i]<<16) + (g[i]<<8) + b[i];
 					}
 					ColorProcessor cp2 = new ColorProcessor(w, h, pix);
 					cp2.setMinAndMax(patch.getMin(), patch.getMax());
 					// 5 - save as jpeg
 					ini.trakem2.io.ImageSaver.saveAsJpeg(cp2, dir_mipmaps + k + "/" + filename, 0.85f, false);
 				}
 			} else {
 				// TODO releaseToFit proper
 				releaseToFit(w * h * 4 * 5);
 				FloatProcessor fp = (FloatProcessor)ip.convertToFloat();
 				final boolean as_grey = ImagePlus.COLOR_256 != patch.getType();
 				while (w >= 64 && h >= 64) { // not smaller than 32x32
 					// 1 - blur the previous image to 0.75 sigma
 					fp = new FloatProcessor(w, h, ImageFilter.computeGaussianFastMirror(new FloatArray2D((float[])fp.getPixels(), w, h), 0.75f).data, cm);
 					// 2 - prepare values for the next scaled image
 					w /= 2;
 					h /= 2;
 					k++;
 					// 3 - check that the target folder for the desired scale exists
 					String target_dir = getScaleDir(dir_mipmaps, k);
 					if (null == target_dir) continue;
 					// 4 - generate scaled image
 					fp = (FloatProcessor)fp.resize(w, h);
 					// 5 - save as 8-bit jpeg
 					ImageProcessor ip2 = Utils.convertTo(fp, patch.getType(), false); // no scaling, since the conversion to float above didn't change the range. This is needed because of the min and max
 					ip2.setMinAndMax(patch.getMin(), patch.getMax());
 					ip2.setColorModel(cm); // the LUT
 					ini.trakem2.io.ImageSaver.saveAsJpeg(ip2, dir_mipmaps + k + "/" + filename, 0.85f, as_grey);
 				}
 			}
 		} catch (Exception e) {
 			IJError.print(e);
 			ok = false; //can't return, need to unlock Patch first
 		}
 		synchronized (gm_lock) {
 			gm_lock();
 			hs_regenerating_mipmaps.remove(patch);
 			gm_unlock();
 		}
 		return ok;
 	}
 
 	/** Generate image pyramids and store them into files under the dir_mipmaps for each Patch object in the Project. The method is multithreaded, using as many processors as available to the JVM.*/
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
 							// check if all the files exists. If one doesn't, then overwrite all anyway
 							int w = (int)pa[k].getWidth();
 							int h = (int)pa[k].getHeight();
 							int level = 0;
 							final String filename = new File(getAbsolutePath(pa[k])).getName() + "." + pa[k].getId() + ".jpg";
 							while (w >= 64 && h >= 64) {
 								w /= 2;
 								h /= 2;
 								level++;
 								if (!new File(dir_mipmaps + level + "/" + filename).exists()) {
 									ow = true;
 									break;
 								}
 							}
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
 		Bureaucrat burro = new Bureaucrat(worker, ((Patch)al.get(0)).getProject());
 		burro.goHaveBreakfast();
 		return burro;
 	}
 
 	private final String getScaleDir(final String dir_mipmaps, final int level) {
 		// synch, so that multithreaded generateMipMaps won't collide trying to create dirs
 		synchronized (db_lock) {
 			lock();
 			final String path = dir_mipmaps + level;
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
 				mawts.removePyramid(id); // does not remove level 0 awts (i.e. the 100% images)
 			} catch (Exception e) { e.printStackTrace(); }
 			unlock();
 		}
 	}
 
 	/** Gets data from the Patch and forks a new Thread to do the file removal. */
 	public void removeMipMaps(final Patch p) {
 		if (null == dir_mipmaps) return;
 		try {
 			// remove the files
 			final int width = (int)p.getWidth();
 			final int height = (int)p.getHeight();
 			String path = getAbsolutePath(p);
 			if (null == path) return; // missing file
 			final String filename = new File(path).getName() + "." + p.getId() + ".jpg";
 			new Thread() {
 				public void run() {
 
 			int w = width;
 			int h = height;
 			int k = 0; // the level
 			while (w >= 64 && h >= 64) { // not smaller than 32x32
 				w /= 2;
 				h /= 2;
 				k++;
 				File f = new File(dir_mipmaps + k + "/" + filename);
 				if (f.exists()) {
 					try {
 						if (!f.delete()) {
 							Utils.log2("Could not remove file " + f.getAbsolutePath());
 						}
 					} catch (Exception e) {
 						IJError.print(e);
 					}
 				}
 			}
 
 				}
 			}.start();
 		} catch (Exception e) {
 			IJError.print(e);
 		}
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
 				final double dim = patch.getWidth() < patch.getHeight() ? patch.getWidth() : patch.getHeight();
 				// find max level that keeps dim over 64 pixels
 				int lev = 1;
 				while (true) {
 					if ((dim * (1 / Math.pow(2, lev))) < 64) {
 						lev--; // the previous one
 						break;
 					}
 					lev++;
 				}
 				if (level > lev) return lev;
 				return level;
 			} else {
 				while (true) {
 					File f = new File(dir_mipmaps + level + "/" + filename);
 					if (f.exists()) {
 						return level;
 					}
 					// stop at 50% images (there are no mipmaps for level 0)
 					if (level <= 1) {
 						return 0;
 					}
 					// try the next level
 					level--;
 				}
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
 		final int level = getMipMapLevel(magnification);
 		if (isURL(dir_mipmaps)) return true; // just assume that it does
 		if (new File(dir_mipmaps + level + "/" + new File(getAbsolutePath(p)).getName() + "." + p.getId() + ".jpg").exists()) return true;
 		return false;
 	}
 
 	/** Loads the file containing the scaled image corresponding to the given level (or the maximum possible level, if too large) and returns it as an awt.Image, or null if not found. Will also regenerate the mipmaps, i.e. recreate the pre-scaled jpeg images if they are missing. */
 	protected Image fetchMipMapAWT(final Patch patch, final int level) {
 		if (null == dir_mipmaps) return null;
 		try {
 			// TODO should wait if the file is currently being generated
 			//  (it's somewhat handled by a double-try to open the jpeg image)
 
 			final int max_level = getHighestMipMapLevel(patch);
 
 			final String filepath = getFileName(patch);
 			if (null == filepath) return null;
 			String path = dir_mipmaps + ( level > max_level ? max_level : level ) + "/" + filepath + "." + patch.getId() + ".jpg";
 			Image img = null;
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
 			if (null != img) return img;
 
 			// if we got so far ... try to regenerate the mipmaps
 			if (!mipmaps_regen) return null;
 
 			//Utils.log2("getMipMapAwt: imp is " + imp + " for path " +  dir_mipmaps + level + "/" + new File(getAbsolutePath(patch)).getName() + "." + patch.getId() + ".jpg");
 
 			// Regenerate in the case of not asking for an image under 64x64
 			double scale = 1 / Math.pow(2, level);
 			if (level > 0 && (patch.getWidth() * scale >= 64 || patch.getHeight() * scale >= 64) && isMipMapsEnabled()) {
 				// regenerate
 				Worker worker = new Worker("Regenerating mipmaps") {
 					public void run() {
 						this.setAsBackground(true);
 						this.startedWorking();
 						try {
 							generateMipMaps(patch);
 						} catch (Exception e) {
 							IJError.print(e);
 						}
 
 						Display.repaint(patch.getLayer(), patch, 0);
 
 						this.finishedWorking();
 					}
 				};
 				Bureaucrat burro = new Bureaucrat(worker, patch.getProject());
 				burro.goHaveBreakfast();
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
 				String path = (String)ht_paths.get(p.getId());
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
 }
