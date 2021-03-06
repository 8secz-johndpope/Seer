 /**
 
 TrakEM2 plugin for ImageJ(C).
 Copyright (C) 2005-2009 Albert Cardona and Rodney Douglas.
 
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
 
 
 import ij.ImagePlus;
 import ij.gui.GenericDialog;
 import ij.gui.Roi;
 import ij.plugin.filter.ThresholdToSelection;
 import ij.process.ByteProcessor;
 import ij.process.ColorProcessor;
 import ij.process.FloatProcessor;
 import ij.process.ImageProcessor;
 import ij.process.ShortProcessor;
 import ini.trakem2.Project;
 import ini.trakem2.imaging.PatchStack;
 import ini.trakem2.persistence.FSLoader;
 import ini.trakem2.persistence.Loader;
 import ini.trakem2.utils.Bureaucrat;
 import ini.trakem2.utils.IJError;
 import ini.trakem2.utils.M;
 import ini.trakem2.utils.ProjectToolbar;
 import ini.trakem2.utils.Search;
 import ini.trakem2.utils.Utils;
 import ini.trakem2.utils.Worker;
 
 import java.awt.Color;
 import java.awt.Composite;
 import java.awt.Dimension;
 import java.awt.Event;
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.awt.Polygon;
 import java.awt.Rectangle;
 import java.awt.Toolkit;
 import java.awt.event.KeyEvent;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.Area;
 import java.awt.geom.NoninvertibleTransformException;
 import java.awt.geom.Path2D;
 import java.awt.geom.Point2D;
 import java.awt.image.BufferedImage;
 import java.awt.image.DirectColorModel;
 import java.awt.image.MemoryImageSource;
 import java.awt.image.PixelGrabber;
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.concurrent.Future;
 
 import mpicbg.imglib.container.shapelist.ShapeList;
 import mpicbg.imglib.image.display.imagej.ImageJFunctions;
 import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
 import mpicbg.models.CoordinateTransformMesh;
 import mpicbg.trakem2.transform.AffineModel2D;
 import mpicbg.trakem2.transform.CoordinateTransform;
 import mpicbg.trakem2.transform.CoordinateTransformList;
 import mpicbg.trakem2.transform.TransformMesh;
 import mpicbg.trakem2.transform.TransformMeshMapping;
 import mpicbg.trakem2.transform.TransformMeshMappingWithMasks.ImageProcessorWithMasks;
 
 public final class Patch extends Displayable implements ImageData {
 
 	private int type = -1; // unknown
 	/** The channels that the currently existing awt image has ready for painting. */
 	private int channels = 0xffffffff;
 
 	/** To generate contrasted images non-destructively. */
 	private double min = 0;
 	private double max = 255;
 
 	private int o_width = 0, o_height = 0;
 
 	/** To be set after the first successful query on whether this file exists, from the Loader, via the setCurrentPath method. This works as a good path cache to avoid excessive calls to File.exists(), which shows up as a huge performance drag. */
 	private String current_path = null;
 	/** To be read from XML, or set when the file ImagePlus has been updated and the current_path points to something else. */
 	private String original_path = null;
 
 	/** The CoordinateTransform that transfers image data to mipmap image data. The AffineTransform is then applied to the mipmap image data. */
 	private CoordinateTransform ct = null;
 	
 	protected int meshResolution = project.getProperty("mesh_resolution", 32);
 	public int getMeshResolution(){ return meshResolution; }
 	
 	/**
 	 * Change the resolution of meshes used to render patches transformed by a
 	 * {@link CoordinateTransform}.  The method has to update bounding box
 	 * offsets introduced by the {@link CoordinateTransform} because the
 	 * bounding box has been calculated using the mesh.
 	 * 
 	 * @param meshResolution
 	 */
 	public void setMeshResolution( final int meshResolution )
 	{
 		if ( ct == null )
 			this.meshResolution = meshResolution;
 		else
 		{
 			Rectangle box = this.getCoordinateTransformBoundingBox();
 			this.at.translate( -box.x, -box.y );
 			this.meshResolution = meshResolution;
 			box = this.getCoordinateTransformBoundingBox();
 			this.at.translate( box.x, box.y );
 			width = box.width;
 			height = box.height;
 			updateInDatabase("transform+dimensions"); // the AffineTransform
 			updateBucket();
 		}
 	}
 
 	/** Create a new Patch and register the associated {@param filepath}
 	 * with the project's loader.
 	 * 
 	 * This method is intended for scripting, to avoid having to create a new Patch
 	 * and then call {@link Loader#addedPatchFrom(String, Patch)}, which is easy to forget.
 	 * 
 	 * @return the new Patch.
 	 * @throws Exception if the image cannot be loaded from the {@param filepath}, or it's an unsupported type such as a composite image or a hyperstack. */
 	static public final Patch createPatch(final Project project, final String filepath) throws Exception {
 		ImagePlus imp = project.getLoader().openImagePlus(filepath);
 		if (null == imp) throw new Exception("Cannot create Patch: the image cannot be opened from filepath " + filepath);
 		if (imp.isComposite()) throw new Exception("Cannot create Patch: composite images are not supported. Convert them to RGB first.");
 		if (imp.isHyperStack()) throw new Exception("Cannot create Patch: hyperstacks are not supported.");
 		Patch p = new Patch(project, new File(filepath).getName(), 0, 0, imp);
 		project.getLoader().addedPatchFrom(filepath, p);
 		return p;
 	}
 
 	/** Construct a Patch from an image;
 	 * most likely you will need to add the file path to the {@param imp}
 	 * by calling {@link Loader#addedPatchFrom(String, Patch)}, as in this example:
 	 * 
 	 * project.getLoader().addedPatchFrom("/path/to/file.png", thePatch); */
 	public Patch(Project project, String title, double x, double y, ImagePlus imp) {
 		super(project, title, x, y);
 		this.type = imp.getType();
 		// Color LUT in ImageJ is a nightmare of inconsistency. We set the COLOR_256 only for 8-bit images that are LUT images themselves; not for 16 or 32-bit images that may have a color LUT (which, by the way, ImageJ tiff encoder cannot save with the tif file.)
 		if (ImagePlus.GRAY8 == this.type && imp.getProcessor().isColorLut()) this.type = ImagePlus.COLOR_256;
 		this.min = imp.getProcessor().getMin();
 		this.max = imp.getProcessor().getMax();
 		checkMinMax();
 		this.o_width = imp.getWidth();
 		this.o_height = imp.getHeight();
 		this.width = (int)o_width;
 		this.height = (int)o_height;
 		project.getLoader().cache(this, imp);
 		addToDatabase();
 	}
 
 	/** Reconstruct a Patch from the database. The ImagePlus will be loaded when necessary. */
 	public Patch(Project project, long id, String title,
 		     float width, float height,
 		     int o_width, int o_height,
 		     int type, boolean locked, double min, double max, AffineTransform at) {
 		super(project, id, title, locked, at, width, height);
 		this.type = type;
 		this.min = min;
 		this.max = max;
 		this.width = width;
 		this.height = height;
 		this.o_width = o_width;
 		this.o_height = o_height;
 		checkMinMax();
 	}
 
 	/** Create a new Patch defining all necessary parameters; it is the responsibility
 	 * of the caller to ensure that the parameters are in agreement with the image
 	 * contained in the {@param file_path}. */
 	public Patch(Project project, String title,
 		     float width, float height,
 		     int o_width, int o_height,
 		     int type, float alpha,
 		     Color color, boolean locked,
 		     double min, double max,
 		     AffineTransform at,
 		     String file_path) {
 		this(project, project.getLoader().getNextId(), title, width, height, o_width, o_height, type, locked, min, max, at);
 		this.alpha = Math.max(0, Math.min(alpha, 1.0f));
 		this.color = null == color ? Color.yellow : color;
 		project.getLoader().addedPatchFrom(file_path, this);
 	}
 
 	/** Reconstruct from an XML entry. */
 	public Patch(Project project, long id, HashMap<String,String> ht_attributes, HashMap<Displayable,String> ht_links) {
 		super(project, id, ht_attributes, ht_links);
 		// cache path:
 		project.getLoader().addedPatchFrom(ht_attributes.get("file_path"), this);
 		boolean hasmin = false;
 		boolean hasmax = false;
 		// parse specific fields
 		String data;
 		if (null != (data = ht_attributes.get("type"))) this.type = Integer.parseInt(data);
 		if (null != (data = ht_attributes.get("min"))) {
 			this.min = Double.parseDouble(data);
 			hasmin = true;
 		}
 		if (null != (data = ht_attributes.get("max"))) {
 			this.max = Double.parseDouble(data);
 			hasmax = true;
 		}
 		if (null != (data = ht_attributes.get("o_width"))) this.o_width = Integer.parseInt(data);
 		if (null != (data = ht_attributes.get("o_height"))) this.o_height = Integer.parseInt(data);
 		if (null != (data = ht_attributes.get("pps"))) {
 			if (FSLoader.isRelativePath(data)) data = project.getLoader().getParentFolder() + data;
 			project.getLoader().setPreprocessorScriptPathSilently(this, data);
 		}
 		if (null != (data = ht_attributes.get("original_path"))) this.original_path = data;
 		if (null != (data = ht_attributes.get("mres"))) this.meshResolution = Integer.parseInt(data);
 		
 		if (0 == o_width || 0 == o_height) {
 			// The original image width and height are unknown.
 			try {
 				Utils.log2("Restoring original width/height from file for id=" + id);
 				// Use BioFormats to read the dimensions out of the original file's header
 				final Dimension dim = project.getLoader().getDimensions(this);
 				o_width = dim.width;
 				o_height = dim.height;
 			} catch (Exception e) {
 				Utils.log("Could not read source data width/height for patch " + this +"\n --> To fix it, close the project and add o_width=\"XXX\" o_height=\"YYY\"\n     to patch entry with oid=\"" + id + "\",\n     where o_width,o_height are the image dimensions as defined in the image file.");
 				// So set them to whatever is somewhat survivable for the moment
 				o_width = (int)width;
 				o_height = (int)height;
 				IJError.print(e);
 			}
 		}
 
 		if (hasmin && hasmax) {
 			checkMinMax();
 		} else {
 			if (ImagePlus.GRAY8 == type || ImagePlus.COLOR_RGB == type || ImagePlus.COLOR_256 == type) {
 				min = 0;
 				max = 255;
 			} else {
 				// Re-read:
 				final ImageProcessor ip = getImageProcessor();
 				if (null == ip) {
 					// Some values, to survive:
 					min = 0;
 					max = Patch.getMaxMax(this.type);
 					Utils.log("WARNING could not restore min and max from image file for Patch #" + this.id + ", and they are not present in the XML file.");
 				} else {
 					ip.resetMinAndMax(); // finds automatically reasonable values
 					setMinAndMax(ip.getMin(), ip.getMax());
 				}
 			}
 		}
 	}
 
 	/** The original width of the pixels in the source image file. */
 	public int getOWidth() { return o_width; }
 	/** The original height of the pixels in the source image file. */
 	public int getOHeight() { return o_height; }
 
 	/** Fetches the ImagePlus from the cache; <b>be warned</b>: the returned ImagePlus may have been flushed, removed and then recreated if the program had memory needs that required flushing part of the cache; use @getImageProcessor to get the pixels guaranteed not to be ever null. */
 	public ImagePlus getImagePlus() {
 		return this.project.getLoader().fetchImagePlus(this);
 	}
 
 	/** Fetches the ImageProcessor from the cache, which will never be flushed or its pixels set to null. If you keep many of these, you may end running out of memory: I advise you to call this method everytime you need the processor. */
 	public ImageProcessor getImageProcessor() {
 		return this.project.getLoader().fetchImageProcessor(this);
 	}
 
 	/** Recreate mipmaps and flush away any cached ones.
 	 * This method is essentially the same as patch.getProject().getLoader().update(patch);
 	 * which in turn it's the same as the following two calls:
 	 *     patch.getProject().getLoader().generateMipMaps(patch);
 	 *     patch.getProject().getLoader().decacheAWT(patch.getId());
 	 *
 	 * If you want to update lots of Patch instances in parallel, consider also
 	 *    project.getLoader().generateMipMaps(ArrayList patches, boolean overwrite);
 	 */
 	public Future<Boolean> updateMipMaps() {
 		return project.getLoader().regenerateMipMaps(this);
 	}
 
 	/** Update type, original dimensions and min,max from the ImagePlus.
 	 *  This is automatically done after a preprocessor script has modified the image. */
 	public void updatePixelProperties(final ImagePlus imp) {
 		readProps(imp);
 	}
 
 	/** Update type, original dimensions and min,max from the given ImagePlus. */
 	private void readProps(final ImagePlus imp) {
 		this.type = imp.getType();
 		if (imp.getWidth() != (int)this.o_width || imp.getHeight() != this.o_height) {
 			this.o_width = imp.getWidth();
 			this.o_height = imp.getHeight();
 			this.width = o_width;
 			this.height = o_height;
 			updateBucket();
 		}
 		ImageProcessor ip = imp.getProcessor();
 		this.min = ip.getMin();
 		this.max = ip.getMax();
 		final HashSet<String> keys = new HashSet<String>();
 		keys.add("type");
 		keys.add("dimensions");
 		keys.add("min_and_max");
 		updateInDatabase(keys);
 		//updateInDatabase(new HashSet<String>(Arrays.asList(new String[]{"type", "dimensions", "min_and_max"})));
 	}
 
 	/** Set a new ImagePlus for this Patch.
 	 * The original path and image remain untouched. Any later image is deleted and replaced by the new one.
 	 */
 	public String set(final ImagePlus new_imp) {
 		synchronized (this) {
 			if (null == new_imp) return null;
 			// 0 - set original_path to the current path if there is no original_path recorded:
 			if (isStack()) {
 				for (Patch p : getStackPatches()) {
 					if (null == p.original_path) original_path = p.project.getLoader().getAbsolutePath(p);
 				}
 			} else {
 				if (null == original_path) original_path = project.getLoader().getAbsolutePath(this);
 			}
 			// 1 - tell the loader to store the image somewhere, unless the image has a path already
 			final String path = project.getLoader().setImageFile(this, new_imp);
 			if (null == path) {
 				Utils.log2("setImageFile returned null!");
 				return null; // something went wrong
 			}
 			// 2 - update properties and mipmaps
 			if (isStack()) {
 				for (Patch p : getStackPatches()) {
 					p.readProps(new_imp);
 					project.getLoader().regenerateMipMaps(p);
 				}
 			} else {
 				readProps(new_imp);
 				project.getLoader().regenerateMipMaps(this);
 			}
 		}
 		Display.repaint(layer, this, 5);
 		return project.getLoader().getAbsolutePath(this);
 	}
 
 	/** Boundary checks on min and max, given the image type. */
 	private void checkMinMax() {
 		if (-1 == this.type) {
 			Utils.log("ERROR -1 == type for patch " + this);
 			return;
 		}
 		final double max_max = Patch.getMaxMax(this.type);
 		if (-1 == min && -1 == max) {
 			this.min = 0;
 			this.max = max_max;
 		}
 		switch (type) {
 			case ImagePlus.GRAY8:
 			case ImagePlus.COLOR_RGB:
 			case ImagePlus.COLOR_256:
 			     if (this.min < 0) {
 				     this.min = 0;
 				     Utils.log("WARNING set min to 0 for patch " + this + " of type " + type);
 			     }
 			     break;
 		}
 		if (this.max > max_max) {
 			this.max = max_max;
 			Utils.log("WARNING fixed max larger than maximum max for type " + type);
 		}
 		if (this.min > this.max) {
 			this.min = this.max;
 			Utils.log("WARNING fixed min larger than max for patch " + this);
 		}
 	}
 
 	/** The min and max values are stored with the Patch, so that the image can be flushed away but the non-destructive contrast settings preserved. */
 	public void setMinAndMax(double min, double max) {
 		this.min = min;
 		this.max = max;
 		checkMinMax();
 		updateInDatabase("min_and_max");
 		Utils.log2("Patch.setMinAndMax: min,max " + min + "," + max);
 	}
 
 	public double getMin() { return min; }
 	public double getMax() { return max; }
 
 	/** Returns the ImagePlus type of this Patch. */
 	public int getType() {
 		return type;
 	}
 
 	public Image createImage(ImagePlus imp) {
 		return adjustChannels(channels, true, imp);
 	}
 
 	public Image createImage() {
 		return adjustChannels(channels, true, null);
 	}
 
 	public int getChannelAlphas() {
 		return channels;
 	}
 
 	/** @param c contains the current Display 'channels' value (the transparencies of each channel). This method creates a new color image in which each channel (R, G, B) has the corresponding alpha (in fact, opacity) specified in the 'c'. This alpha is independent of the alpha of the whole Patch. The method updates the Loader cache with the newly created image. The argument 'imp' is optional: if null, it will be retrieved from the loader.<br />
 	 * For non-color images, a standard image is returned regardless of the @param c
 	 */
 	private Image adjustChannels(final int c, final boolean force, ImagePlus imp) {
 		if (null == imp) imp = project.getLoader().fetchImagePlus(this);
 		ImageProcessor ip = imp.getProcessor();
 		if (null == ip) return null; // fixing synch problems when deleting a Patch
 		Image awt = null;
 		if (ImagePlus.COLOR_RGB == type) {
 			if (imp.getType() != type ) {
 				ip = Utils.convertTo(ip, type, false); // all other types need not be converted, since there are no alphas anyway
 			}
 			if ((c&0x00ffffff) == 0x00ffffff && !force) {
 				// full transparency
 				awt = ip.createImage(); //imp.getImage();
 				// pixels array will be shared using ij138j and above
 			} else {
 				// modified from ij.process.ColorProcessor.createImage() by Wayne Rasband
 				int[] pixels = (int[])ip.getPixels();
 				float cr = ((c&0xff0000)>>16) / 255.0f;
 				float cg = ((c&0xff00)>>8) / 255.0f;
 				float cb = (c&0xff) / 255.0f;
 				int[] pix = new int[pixels.length];
 				int p;
 				for (int i=pixels.length -1; i>-1; i--) {
 					p = pixels[i];
 					pix[i] =  (((int)(((p&0xff0000)>>16) * cr))<<16)
 						+ (((int)(((p&0xff00)>>8) * cg))<<8)
 						+   (int) ((p&0xff) * cb);
 				}
 				int w = imp.getWidth();
 				MemoryImageSource source = new MemoryImageSource(w, imp.getHeight(), DCM, pix, 0, w);
 				source.setAnimated(true);
 				source.setFullBufferUpdates(true);
 				awt = Toolkit.getDefaultToolkit().createImage(source);
 			}
 		} else {
 			awt = ip.createImage();
 		}
 
 		//Utils.log2("ip's min, max: " + ip.getMin() + ", " + ip.getMax());
 
 		this.channels = c;
 
 		return awt;
 	}
 
 	static final public DirectColorModel DCM = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
 
 	/** Just throws the cached image away if the alpha of the channels has changed. */
 	private final void checkChannels(int channels, double magnification) {
 		if (this.channels != channels && (ImagePlus.COLOR_RGB == this.type || ImagePlus.COLOR_256 == this.type)) {
 			final int old_channels = this.channels;
 			this.channels = channels; // before, so if any gets recreated it's done right
 			project.getLoader().adjustChannels(this, old_channels);
 		}
 	}
 
 	/** Takes an image and scales its channels according to the values packed in this.channels.
 	 *  This method is intended for fixing RGB images which are loaded from jpegs (the mipmaps), and which
 	 *  have then the full colorization of the original image present in their pixels array.
 	 *  Otherwise the channel opacity scaling makes no sense.
 	 *  If 0xffffffff == this.channels the awt is returned as is.
 	 *  If the awt is null returns null.
 	 */
 	public final Image adjustChannels(final Image awt) {
 		if (0xffffffff == this.channels || null == awt) return awt;
 		BufferedImage bi = null;
 		// reuse if possible
 		if (awt instanceof BufferedImage) bi = (BufferedImage)awt;
 		else {
 			bi = new BufferedImage(awt.getWidth(null), awt.getHeight(null), BufferedImage.TYPE_INT_ARGB);
 			bi.getGraphics().drawImage(awt, 0, 0, null);
 		}
 		// extract channel values
 		final float cr = ((channels&0xff0000)>>16) / 255.0f;
 		final float cg = ((channels&0xff00)>>8   ) / 255.0f;
 		final float cb = ( channels&0xff         ) / 255.0f;
 		// extract pixels
 		Utils.log2("w, h: " + bi.getWidth() + ", " + bi.getHeight());
 		final int[] pixels = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, 1);
 		// scale them according to channel opacities
 		int p;
 		for (int i=0; i<pixels.length; i++) {
 			p = pixels[i];
 			pixels[i] =  (((int)(((p&0xff0000)>>16) * cr))<<16)
 				+ (((int)(((p&0xff00)>>8) * cg))<<8)
 				+   (int) ((p&0xff) * cb);
 		}
 		// replace pixels
 		bi.setRGB(0, 0, bi.getWidth(), bi.getHeight(), pixels, 0, 1);
 		return bi;
 	}
 
 	public void paintOffscreen(Graphics2D g, Rectangle srcRect, double magnification, boolean active, int channels, Layer active_layer) {
 		paint(g, fetchImage(magnification, channels, true), srcRect);
 	}
 
 	@Override
 	public void paint(Graphics2D g, Rectangle srcRect, double magnification, boolean active, int channels, Layer active_layer, List<Layer> _ignored) {
 		paint(g, fetchImage(magnification, channels, false), srcRect);
 	}
 
 	private final MipMapImage fetchImage(final double magnification, final int channels, final boolean wait_for_image) {
 		checkChannels(channels, magnification);
 
 		// Consider all possible scaling components: m00, m01
 		//                                           m10, m11
 		double sc = magnification * Math.max(Math.abs(at.getScaleX()),
 				                     Math.max(Math.abs(at.getScaleY()),
 							      Math.max(Math.abs(at.getShearX()),
 								       Math.abs(at.getShearY()))));
 		if (sc < 0) sc = magnification;
 		return wait_for_image ?
 			  project.getLoader().fetchDataImage(this, sc)
 			: project.getLoader().fetchImage(this, sc);
 	}
 	
 	private void paint( final Graphics2D g, final Image image, final Rectangle srcRect )
 	{
 		/*
 		 * infer scale: this scales the numbers of pixels according to patch
 		 * size which might not be the exact scale the image was sampled at
 		 */ 
 		final int iw = image.getWidth(null);
 		final int ih = image.getHeight(null);
 		paint( g, new MipMapImage( image, this.width / iw, this.height / ih ), srcRect );
 	}
 
 	private void paint(final Graphics2D g, final MipMapImage mipMap, final Rectangle srcRect ) {
 
 		AffineTransform atp = new AffineTransform();
 		
 		/*
 		 * Compensate for AWT considering coordinates at pixel corners
 		 * and TrakEM2 and mpicbg considering them at pixel centers.
 		 */
 		atp.translate( 0.5, 0.5 );
 		
 		atp.concatenate( this.at );
 		
 		atp.scale( mipMap.scaleX, mipMap.scaleY );
 		
 		/*
 		 * Compensate MipMap pixel access for AWT considering coordinates at
 		 * pixel corners and TrakEM2 and mpicbg considering them at pixel
 		 * centers.
 		 */
 		atp.translate( -0.5, -0.5 );
 		
 		
 		paintMipMap(g, mipMap, atp, srcRect);
 	}
 
 	/** Paint first whatever is available, then request that the proper image be loaded and painted. */
 	@Override
 	public void prePaint(final Graphics2D g, final Rectangle srcRect, final double magnification, final boolean active, final int channels, final Layer active_layer, final List<Layer> _ignored) {
 
 		AffineTransform atp = new AffineTransform();
 		
 		/*
 		 * Compensate for AWT considering coordinates at pixel corners
 		 * and TrakEM2 and mpicbg considering them at pixel centers.
 		 */
 		atp.translate( 0.5, 0.5 );
 		
 		atp.concatenate( this.at );
 		
 		checkChannels(channels, magnification);
 
 		// Consider all possible scaling components: m00, m01
 		//                                           m10, m11
 		double sc = magnification * Math.max(Math.abs(at.getScaleX()),
 				                     Math.max(Math.abs(at.getScaleY()),
 							      Math.max(Math.abs(at.getShearX()),
 								       Math.abs(at.getShearY()))));
 		if (sc < 0) sc = magnification;
 
 		MipMapImage mipMap = project.getLoader().getCachedClosestAboveImage(this, sc); // above or equal
 		if (null == mipMap) {
 			mipMap = project.getLoader().getCachedClosestBelowImage(this, sc); // below, not equal
 			if (null == mipMap) {
 				// fetch the smallest image possible
 				//image = project.getLoader().fetchAWTImage(this, Loader.getHighestMipMapLevel(this));
 				// fetch an image 1/4 of the necessary size
 				mipMap = project.getLoader().fetchImage(this, sc/4);
 			}
 			// painting a smaller image, will need to repaint with the proper one
 			if (!Loader.isSignalImage( mipMap.image ) ) {
 				// use the lower resolution image, but ask to repaint it on load
 				Loader.preload(this, sc, true);
 			}
 		}
 
 		atp.scale( mipMap.scaleX, mipMap.scaleY );
 		
 		/*
 		 * Compensate MipMap pixel access for AWT considering coordinates at
 		 * pixel corners and TrakEM2 and mpicbg considering them at pixel
 		 * centers.
 		 */
 		atp.translate( -0.5, -0.5 );
 		
 		paintMipMap(g, mipMap, atp, srcRect);
 	}
 	
 	private final void paintMipMap(final Graphics2D g, final MipMapImage mipMap,
 			final AffineTransform atp, final Rectangle srcRect)
 	{	
 		final Composite original_composite = g.getComposite();
 		// Fail gracefully for graphics cards that don't support custom composites, like ATI cards:
 		try {
 			g.setComposite( getComposite(getCompositeMode()) );
 			g.drawImage( mipMap.image, atp, null );
 		} catch (Throwable t) {
 			Utils.log(new StringBuilder("Cannot paint Patch with composite type ").append(compositeModes[getCompositeMode()]).append("\nReason:\n").append(t.toString()).toString());
 			g.drawImage( mipMap.image, atp, null );
 		}
 		g.setComposite( original_composite );
 	}
 
 	public boolean isDeletable() {
 		return 0 == width && 0 == height;
 	}
 
 	/** Remove only if linked to other Patches or to noone. */
 	public boolean remove(boolean check) {
 		if (check && !Utils.check("Really remove " + this.toString() + " ?")) return false;
 		if (isStack()) { // this Patch is part of a stack
 			GenericDialog gd = new GenericDialog("Stack!");
 			gd.addMessage("Really delete the entire stack?");
 			gd.addCheckbox("Delete layers if empty", true);
 			gd.showDialog();
 			if (gd.wasCanceled()) return false;
 			boolean delete_empty_layers = gd.getNextBoolean();
 			// gather all
 			HashMap<Double,Patch> ht = new HashMap<Double,Patch>();
 			getStackPatchesNR(ht);
 			Utils.log2("Removing stack patches: " + ht.size());
 			for (final Patch p : ht.values()) {
 				if (!p.isOnlyLinkedTo(this.getClass())) {
 					Utils.showMessage("At least one slice of the stack (z=" + p.getLayer().getZ() + ") is supporting other data.\nCan't delete.");
 					return false;
 				}
 			}
 			ArrayList<Layer> layers_to_remove = new ArrayList<Layer>();
 			for (final Patch p : ht.values()) {
 				if (!p.layer.remove(p) || !p.removeFromDatabase()) {
 					Utils.showMessage("Can't delete Patch " + p);
 					return false;
 				}
 				p.unlink();
 				p.removeLinkedPropertiesFromOrigins();
 				//no need//it.remove();
 				layers_to_remove.add(p.layer);
 				if (p.layer.isEmpty()) Display.close(p.layer);
 				else Display.repaint(p.layer);
 			}
 			if (delete_empty_layers) {
 				for (final Layer la : layers_to_remove) {
 					if (la.isEmpty()) {
 						project.getLayerTree().remove(la, false);
 						Display.close(la);
 					}
 				}
 			}
 			Search.remove(this);
 			return true;
 		} else {
 			if (isOnlyLinkedTo(Patch.class, this.layer) && layer.remove(this) && removeFromDatabase()) { // don't alow to remove linked patches (unless only linked to other patches in the same layer)
 				unlink();
 				removeLinkedPropertiesFromOrigins();
 				Search.remove(this);
 				return true;
 			} else {
 				Utils.showMessage("Patch: can't remove! The image is linked and thus supports other data).");
 				return false;
 			}
 		}
 	}
 
 	/** Returns true if this Patch holds direct links to at least one other image in a different layer. Doesn't check for total overlap. */
 	public final boolean isStack() {
 		if (null == hs_linked || hs_linked.isEmpty()) return false;
 		for (final Displayable d : hs_linked) {
 			if (d.getClass() == Patch.class && d.layer.getId() != this.layer.getId()) return true;
 		}
 		return false;
 	}
 
 	/** Retuns a virtual ImagePlus with a virtual stack if necessary. */
 	public PatchStack makePatchStack() {
 		// are we a stack?
 		final TreeMap<Double,Patch> ht = new TreeMap<Double,Patch>();
 		getStackPatchesNR(ht);
 		final Patch[] patch;
 		int currentSlice = 1; // from 1 to n, as in ImageStack
 		if (ht.size() > 1) {
 			patch = new Patch[ht.size()];
 			int i = 0;
 			for (final Patch p : ht.values()) { // sorted by z
 				patch[i] = p;
 				if (p.id == this.id) currentSlice = i+1;
 				i++;
 			}
 		} else {
 			patch = new Patch[]{ this };
 		}
 		return new PatchStack(patch, currentSlice);
 	}
 
 	public ArrayList<Patch> getStackPatches() {
 		final TreeMap<Double,Patch> ht = new TreeMap<Double,Patch>();
 		getStackPatchesNR(ht);
 		return new ArrayList<Patch>(ht.values()); // sorted by z
 	}
 
 	/** Non-recursive version to avoid stack overflows with "excessive" recursion (I hate java). */
 	private void getStackPatchesNR(final Map<Double,Patch> ht) {
 		final ArrayList<Patch> list1 = new ArrayList<Patch>();
 		list1.add(this);
 		final ArrayList<Patch> list2 = new ArrayList<Patch>();
 		while (list1.size() > 0) {
 			list2.clear();
 			for (Patch p : list1) {
 				if (null != p.hs_linked) {
 					for (Iterator<?> it = p.hs_linked.iterator(); it.hasNext(); ) {
 						Object ln = it.next();
 						if (ln.getClass() == Patch.class) {
 							Patch pa = (Patch)ln;
 							if (!ht.containsValue(pa)) {
 								ht.put(pa.layer.getZ(), pa);
 								list2.add(pa);
 							}
 						}
 					}
 				}
 			}
 			list1.clear();
 			list1.addAll(list2);
 		}
 	}
 
 	/** Opens and closes the tag and exports data. The image is saved in the directory provided in @param any as a String. */
 	@Override
 	public void exportXML(final StringBuilder sb_body, final String indent, final Object any) { // TODO the Loader should handle the saving of images, not this class.
 		String in = indent + "\t";
 		String path = null;
 		String path2 = null;
 		//Utils.log2("#########\np id=" + id + "  any is " + any);
 		if (null != any) {
 			path = any + title; // ah yes, automatic toString() .. it's like the ONLY smart logic at the object level built into java.
 			// save image without overwritting, and add proper extension (.zip)
 			path2 = project.getLoader().exportImage(this, path, false);
 			//Utils.log2("p id=" + id + "  path2: " + path2);
 			// path2 will be null if the file exists already
 		}
 		sb_body.append(indent).append("<t2_patch\n");
 		String rel_path = null;
 		if (null != path && path.equals(path2)) { // this happens when a DB project is exported. It may be a different path when it's a FS loader
 			//Utils.log2("p id=" + id + "  path==path2");
 			rel_path = path2;
 			int i_slash = rel_path.lastIndexOf('/'); // TrakEM2 uses paths that always have '/' and never '\', so using java.io.File.separatorChar would be an error.
 			if (i_slash > 0) {
 				i_slash = rel_path.lastIndexOf('/', i_slash -1);
 				if (-1 != i_slash) {
 					rel_path = rel_path.substring(i_slash+1);
 				}
 			}
 		} else {
 			//Utils.log2("Setting rel_path to " + path2);
 			rel_path = path2;
 		}
 		// For FSLoader projects, saving a second time will save images as null unless calling it
 		if (null == rel_path) {
 			//Utils.log2("path2 was null");
 			Object ob = project.getLoader().getPath(this);
 			path2 = null == ob ? null : (String)ob;
 			if (null == path2) {
 				//Utils.log2("ERROR: No path for Patch id=" + id + " and title: " + title);
 				rel_path = title; // at least some clue for recovery
 			} else {
 				rel_path = path2;
 			}
 		}
 
 		//Utils.log("Patch path is: " + rel_path);
 
 		super.exportXML(sb_body, in, any);
 		String[] RGB = Utils.getHexRGBColor(color);
 		int type = this.type;
 		if (-1 == this.type) {
 			Utils.log2("Retrieving type for p = " + this);
 			ImagePlus imp = project.getLoader().fetchImagePlus(this);
 			if (null != imp) type = imp.getType();
 		}
 		sb_body.append(in).append("type=\"").append(type /*null == any ? ImagePlus.GRAY8 : type*/).append("\"\n")
 		       .append(in).append("file_path=\"").append(rel_path).append("\"\n")
 		       .append(in).append("style=\"fill-opacity:").append(alpha).append(";stroke:#").append(RGB[0]).append(RGB[1]).append(RGB[2]).append(";\"\n")
 		       .append(in).append("o_width=\"").append(o_width).append("\"\n")
 		       .append(in).append("o_height=\"").append(o_height).append("\"\n")
 		;
 		if (null != original_path) {
 			sb_body.append(in).append("original_path=\"").append(original_path).append("\"\n");
 		}
 		sb_body.append(in).append("min=\"").append(min).append("\"\n");
 		sb_body.append(in).append("max=\"").append(max).append("\"\n");
 
 		String pps = getPreprocessorScriptPath();
 		if (null != pps) sb_body.append(in).append("pps=\"").append(project.getLoader().makeRelativePath(pps)).append("\"\n");
 
 		sb_body.append(in).append("mres=\"").append(meshResolution).append("\"\n");
 		
 		sb_body.append(indent).append(">\n");
 
 		if (null != ct) {
 			sb_body.append(ct.toXML(in)).append('\n');
 		}
 
 		super.restXML(sb_body, in, any);
 
 		sb_body.append(indent).append("</t2_patch>\n");
 	}
 
 	static private final double getMaxMax(final int type) {
 		int pow = 1;
 		switch (type) {
 			case ImagePlus.GRAY16: pow = 2; break; // TODO problems with unsigned short most likely
 			case ImagePlus.GRAY32: pow = 4; break;
 			default: return 255;
 		}
 		return Math.pow(256, pow) - 1;
 	}
 
 	static public void exportDTD(final StringBuilder sb_header, final HashSet<String> hs, final String indent) {
 		final String type = "t2_patch";
 		if (hs.contains(type)) return;
 		// The Patch itself:
 		sb_header.append(indent).append("<!ELEMENT t2_patch (").append(Displayable.commonDTDChildren()).append(",ict_transform,ict_transform_list)>\n");
 		Displayable.exportDTD(type, sb_header, hs, indent);
 		sb_header.append(indent).append(TAG_ATTR1).append(type).append(" file_path").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" original_path").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" type").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" ct").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" o_width").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" o_height").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" min").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" max").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" o_width").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" o_height").append(TAG_ATTR2)
 			 .append(indent).append(TAG_ATTR1).append(type).append(" pps").append(TAG_ATTR2) // preprocessor script
 			 .append(indent).append(TAG_ATTR1).append(type).append(" mres").append(TAG_ATTR2)
 		;
 	}
 
 	/** Performs a copy of this object, without the links, unlocked and visible, except for the image which is NOT duplicated. */
 	public Displayable clone(final Project pr, final boolean copy_id) {
 		final long nid = copy_id ? this.id : pr.getLoader().getNextId();
 		final Patch copy = new Patch(pr, nid, null != title ? title.toString() : null, width, height, o_width, o_height, type, false, min, max, (AffineTransform)at.clone());
 		copy.color = new Color(color.getRed(), color.getGreen(), color.getBlue());
 		copy.alpha = this.alpha;
 		copy.visible = true;
 		copy.channels = this.channels;
 		copy.min = this.min;
 		copy.max = this.max;
 		copy.ct = null == ct ? null : this.ct.copy();
 		copy.addToDatabase();
 		pr.getLoader().addedPatchFrom(this.project.getLoader().getAbsolutePath(this), copy);
 		copy.setAlphaMask(this.project.getLoader().fetchImageMask(this));
 
 		// Copy preprocessor scripts
 		String pspath = this.project.getLoader().getPreprocessorScriptPath(this);
 		if (null != pspath) pr.getLoader().setPreprocessorScriptPathSilently(copy, pspath);
 
 		return copy;
 	}
 
 	static public final class TransformProperties {
 		final public Rectangle bounds;
 		final public AffineTransform at;
 		final public CoordinateTransform ct;
 		final public int meshResolution;
 		final public int o_width, o_height;
 		final public Area area;
 
 		public TransformProperties(final Patch p) {
 			this.at = new AffineTransform(p.at);
 			this.ct = null == p.ct ? null : p.ct.copy();
 			this.meshResolution = p.getMeshResolution();
 			this.bounds = p.getBoundingBox(null);
 			this.o_width = p.o_width;
 			this.o_height = p.o_height;
 			this.area = p.getArea();
 		}
 	}
 
 	public Patch.TransformProperties getTransformPropertiesCopy() {
 		return new Patch.TransformProperties(this);
 	}
 
 
 	/** Override to cancel. */
 	public boolean linkPatches() {
 		Utils.log2("Patch class can't link other patches using Displayable.linkPatches()");
 		return false;
 	}
 
 	@Override
 	public void paintSnapshot(final Graphics2D g, final Layer layer, final List<Layer> layers, final Rectangle srcRect, final double mag) {
 		switch (layer.getParent().getSnapshotsMode()) {
 			case 0:
 				if (!project.getLoader().isSnapPaintable(this.id)) {
 					paintAsBox(g);
 				} else {
 					paint(g, srcRect, mag, false, this.channels, layer, layers);
 				}
 				return;
 			case 1:
 				paintAsBox(g);
 				return;
 			default: return; // case 2: // disabled, no paint
 		}
 	}
 
 	static protected void crosslink(final Collection<Displayable> patches, final boolean overlapping_only) {
 		if (null == patches) return;
 		final ArrayList<Patch> al = new ArrayList<Patch>();
 		for (Object ob : patches) if (ob instanceof Patch) al.add((Patch)ob); // ... 
 		final int len = al.size();
 		if (len < 2) return;
 		final Patch[] pa = new Patch[len];
 		al.toArray(pa);
 		// linking is reciprocal: need only call link() on one member of the pair
 		for (int i=0; i<pa.length; i++) {
 			for (int j=i+1; j<pa.length; j++) {
 				if (overlapping_only && !pa[i].intersects(pa[j])) continue;
 				pa[i].link(pa[j]);
 			}
 		}
 	}
 
 	/** Magnification-dependent counterpart to ImageProcessor.getPixel(x, y). Expects x,y in world coordinates. This method is intended for grabing an occasional pixel; to grab all pixels, see @getImageProcessor method.*/
 	public int getPixel(double mag, final int x, final int y) {
 		final int[] iArray = getPixel(x, y, mag);
 		if (ImagePlus.COLOR_RGB == this.type) {
 			return (iArray[0]<<16) + (iArray[1]<<8) + iArray[2];
 		}
 		return iArray[0];
 	}
 
 	/** Magnification-dependent counterpart to ImageProcessor.getPixel(x, y, iArray). Expects x,y in world coordinates.  This method is intended for grabing an occasional pixel; to grab all pixels, see @getImageProcessor method.*/
 	public int[] getPixel(double mag, final int x, final int y, final int[] iArray) {
 		final int[] ia = getPixel(x, y, mag);
 		if(null != iArray) {
 			iArray[0] = ia[0];
 			iArray[1] = ia[1];
 			iArray[2] = ia[2];
 			return iArray;
 		}
 		return ia;
 	}
 
 	/** Expects x,y in world coordinates.  This method is intended for grabing an occasional pixel; to grab all pixels, see @getImageProcessor method. */
 	public int[] getPixel(final int x, final int y, final double mag) {
 		if (project.getLoader().isUnloadable(this)) return new int[4];
 		final MipMapImage mipMap = project.getLoader().fetchImage(this, mag);
 		if (Loader.isSignalImage(mipMap.image)) return new int[4];
 		final int w = mipMap.image.getWidth(null);
 		final Point2D.Double pd = inverseTransformPoint(x, y);
 		final int x2 = (int)(pd.x / mipMap.scaleX);
 		final int y2 = (int)(pd.y / mipMap.scaleY);
 		final int[] pvalue = new int[4];
 		final PixelGrabber pg = new PixelGrabber( mipMap.image, x2, y2, 1, 1, pvalue, 0, w);
 		try {
 			pg.grabPixels();
 		} catch (InterruptedException ie) {
 			return pvalue;
 		}
 
 		approximateTransferPixel(pvalue);
 
 		return pvalue;
 	}
 
 	/** Transfer an 8-bit or RGB pixel to this image color space, interpolating;
 	 * the pvalue is modified in place.
 	 * For float images (GRAY32), the float value is packed into bits in pvalue[0],
 	 * and can be recovered with Float.intBitsToFloat(pvalue[0]). */
 	protected void approximateTransferPixel(final int[] pvalue) {
 		switch (type) {
 			case ImagePlus.COLOR_256: // mipmaps use RGB images internally, so I can't compute the index in the LUT
 			case ImagePlus.COLOR_RGB:
 				final int c = pvalue[0];
 				pvalue[0] = (c&0xff0000)>>16; // R
 				pvalue[1] = (c&0xff00)>>8;    // G
 				pvalue[2] = c&0xff;           // B
 				break;
 			case ImagePlus.GRAY8:
 				pvalue[0] = pvalue[0]&0xff;
 				break;
 			case ImagePlus.GRAY16:
 				pvalue[0] = pvalue[0]&0xff;
 				// correct range: from 8-bit of the mipmap to 16 bit
 				pvalue[0] = (int)(min + pvalue[0] * ( (max - min) / 256 ));
 				break;
 			case ImagePlus.GRAY32:
 				pvalue[0] = pvalue[0]&0xff;
 				// correct range: from 8-bit of the mipmap to 32 bit
 				// ... and encode, so that it will be decoded with Float.intBitsToFloat
 				pvalue[0] = Float.floatToIntBits((float)(min + pvalue[0] * ( (max - min) / 256 )));
 				break;
 		}
 	}
 
 	/** If this patch is part of a stack, the file path will contain the slice number attached to it, in the form -----#slice=10 for slice number 10. */
 	public final String getFilePath() {
 		if (null != current_path) return current_path;
 		return project.getLoader().getAbsolutePath(this);
 	}
 
 	/** Returns the absolute path to the image file, as read by the OS. */
 	public final String getImageFilePath() {
 		return project.getLoader().getImageFilePath(this);
 	}
 
 	/** Returns the value of the field current_path, which may be null. If not null, the value may contain the slice info in it if it's part of a stack. */
 	public final String getCurrentPath() { return current_path; }
 
 	/** Cache a proper, good, known path to the image wrapped by this Patch. */
 	public final void cacheCurrentPath(final String path) {
 		this.current_path = path;
 	}
 
 	/** Returns the value of the field original_path, which may be null. If not null, the value may contain the slice info in it if it's part of a stack. */
 	synchronized public String getOriginalPath() { return original_path; }
 
 	protected void setAlpha(float alpha, boolean update) {
 		if (isStack()) {
 			HashMap<Double,Patch> ht = new HashMap<Double,Patch>();
 			getStackPatchesNR(ht);
 			for (Patch pa : ht.values()) {
 				pa.alpha = alpha;
 				pa.updateInDatabase("alpha");
 				Display.repaint(pa.layer, pa, 5);
 			}
 			Display3D.setTransparency(this, alpha);
 		} else super.setAlpha(alpha, update);
 	}
 
 	public void debug() {
 		Utils.log2("Patch id=" + id + "\n\toriginal_path=" + original_path + "\n\tcurrent_path=" + current_path);
 	}
 
 	/** Revert the ImagePlus to the one stored in original_path, if any; will revert all linked patches if this is part of a stack. */
 	public boolean revert() {
 		synchronized (this) {
 			if (null == original_path) return false; // nothing to revert to
 			// 1 - check that original_path exists
 			if (!new File(original_path).exists()) {
 				Utils.log("CANNOT revert: Original file path does not exist: " + original_path + " for patch " + getTitle() + " #" + id);
 				return false;
 			}
 			// 2 - check that the original can be loaded
 			final ImagePlus imp = project.getLoader().fetchOriginal(this);
 			if (null == imp || null == set(imp)) {
 				Utils.log("CANNOT REVERT: original image at path " + original_path + " fails to load, for patch " + getType() + " #" + id);
 				return false;
 			}
 			// 3 - update path in loader, and cache imp for each stack slice id
 			if (isStack()) {
 				for (Patch p : getStackPatches()) {
 					p.project.getLoader().addedPatchFrom(p.original_path, p);
 					p.project.getLoader().cacheImagePlus(p.id, imp);
 					p.project.getLoader().regenerateMipMaps(p);
 				}
 			} else {
 				project.getLoader().addedPatchFrom(original_path, this);
 				project.getLoader().cacheImagePlus(id, imp);
 				project.getLoader().regenerateMipMaps(this);
 			}
 			// 4 - update screens
 		}
 		Display.repaint(layer, this, 0);
 		Utils.showStatus("Reverted patch " + getTitle(), false);
 		return true;
 	}
 
 	/** For reconstruction purposes, overwrites the present CoordinateTransform, if any, with the given one. */
 	public void setCoordinateTransformSilently(final CoordinateTransform ct) {
 		this.ct = ct;
 	}
 
 	/** Set a CoordinateTransform to this Patch.
 	 *  The resulting image of applying the coordinate transform does not need to be rectangular: an alpha mask will take care of the borders. You should call updateMipMaps() afterwards to update the mipmap images used for painting this Patch to the screen. */
 	public final void setCoordinateTransform(final CoordinateTransform ct) {
 		if (isLinked()) {
 			Utils.log("Cannot set coordinate transform: patch is linked!");
 			return;
 		}
 
 		if (null != this.ct) {
 			// restore image without the transform
 			final TransformMesh mesh = new TransformMesh(this.ct, meshResolution, o_width, o_height);
 			final Rectangle box = mesh.getBoundingBox();
 			this.at.translate(-box.x, -box.y);
 			updateInDatabase("transform+dimensions");
 		}
 
 		this.ct = ct;
 		updateInDatabase("ict_transform");
 
 		if (null == this.ct) {
 			width = o_width;
 			height = o_height;
 			updateBucket();
 			return;
 		}
 
 		// Adjust the AffineTransform to correct for bounding box displacement
 
 		final TransformMesh mesh = new TransformMesh(this.ct, meshResolution, o_width, o_height);
 		final Rectangle box = mesh.getBoundingBox();
 		this.at.translate(box.x, box.y);
 		width = box.width;
 		height = box.height;
 		updateInDatabase("transform+dimensions"); // the AffineTransform
 		updateBucket();
 
 		// Updating the mipmaps will call createTransformedImage below if ct is not null
 		/* DISABLED */ //updateMipMaps();
 	}
 	
 	/**
 	 * Append a {@link CoordinateTransform} to the current
 	 * {@link CoordinateTransformList}.  If there is no transform yet, it just
 	 * sets it.  If there is only one transform, it replaces it by a list
 	 * containing both, the existing first.
 	 */
 	@SuppressWarnings("unchecked")
 	public final void appendCoordinateTransform(final CoordinateTransform ct) {
 		if (null == this.ct)
 			setCoordinateTransform(ct);
 		else {
 			final CoordinateTransformList< CoordinateTransform > ctl;
 			if (this.ct instanceof CoordinateTransformList<?>)
 				ctl = (CoordinateTransformList< CoordinateTransform >)this.ct.copy();
 			else {
 				ctl = new CoordinateTransformList< CoordinateTransform >();
 				ctl.add(this.ct);
 			}
 			ctl.add(ct);
 			setCoordinateTransform(ctl);
 		}
 	}
 	
 	
 	/**
 	 * Pre-append a {@link CoordinateTransform} to the current
 	 * {@link CoordinateTransformList}.  If there is no transform yet, it just
 	 * sets it.  If there is only one transform, it replaces it by a list
 	 * containing both, the new one first.
 	 */
 	@SuppressWarnings("unchecked")
 	public final void preAppendCoordinateTransform(final CoordinateTransform ct) {
 		if (null == this.ct)
 			setCoordinateTransform(ct);
 		else {
 			final CoordinateTransformList< CoordinateTransform > ctl;
 			if (ct instanceof CoordinateTransformList<?>)
 				ctl = (CoordinateTransformList< CoordinateTransform >)ct.copy();
 			else {
 				ctl = new CoordinateTransformList< CoordinateTransform >();
 				ctl.add(ct);
 			}
 			ctl.add(this.ct);
 			setCoordinateTransform(ctl);
 		}
 	}
 	
 	/**
 	 * Get the bounding rectangle of the transformed image relative to the
 	 * original image.
 	 * 
 	 * TODO
 	 *   Currently, this is done in a very expensive way.  The
 	 *   {@linkplain TransformMesh} is built and its bounding rectangle is
 	 *   returned.  Think about just storing this rectangle in the
 	 *   {@linkplain Patch} instance.
 	 * 
 	 * @return
 	 */
 	public final Rectangle getCoordinateTransformBoundingBox() {
 		if (null==ct)
 			return new Rectangle(0,0,o_width,o_height);
 		final TransformMesh mesh = new TransformMesh(this.ct, meshResolution, o_width, o_height);
 		return mesh.getBoundingBox();
 	}
 
 	public final CoordinateTransform getCoordinateTransform() { return ct; }
 	
 	public final Patch.PatchImage createCoordinateTransformedImage() {
 		if (null == ct) return null;
 		
 		final ImageProcessor source = getImageProcessor();
 		
 		if (null == source) return null; // some error occurred
 
 		//Utils.log2("source image dimensions: " + source.getWidth() + ", " + source.getHeight());
 
 		final TransformMesh mesh = new TransformMesh(ct, meshResolution, o_width, o_height);
 		final Rectangle box = mesh.getBoundingBox();
 
 		/* We can calculate the exact size of the image to be rendered, so let's do it */
 //		project.getLoader().releaseToFit(o_width, o_height, type, 5);
 		final long b =
 			  2 * o_width * o_height		// outside and mask source
 			+ 2 * box.width * box.height	// outside and mask target
 			+ 5 * o_width * o_height		// image source
 			+ 5 * box.width * box.height;	// image target
 		project.getLoader().releaseToFit( b );
 
 		final TransformMeshMapping mapping = new TransformMeshMapping( mesh );
 		
 		final ImageProcessorWithMasks target = mapping.createMappedMaskedImageInterpolated( source, project.getLoader().fetchImageMask(this) );
 		
 //		// Set all non-white pixels to zero
 //		final byte[] pix = (byte[])target.outside.getPixels();
 //		for (int i=0; i<pix.length; i++)
 //			if ((pix[i]&0xff) != 255) pix[i] = 0;
 
 		//Utils.log2("New image dimensions: " + target.getWidth() + ", " + target.getHeight());
 		//Utils.log2("box: " + box);
 
 		return new PatchImage( target.ip, ( ByteProcessor )target.mask, target.outside, box, true );
 	}
 
 	static final public class PatchImage {
 		/** The image, coordinate-transformed if null != ct. */
 		final public ImageProcessor target;
 		/** The alpha mask, coordinate-transformed if null != ct. */
 		final public ByteProcessor mask;
 		/** The outside mask, coordinate-transformed if null != ct. */
 		final public ByteProcessor outside;
 		/** The bounding box of the image relative to the original, with x,y as the displacement relative to the pixels of the original image. */
 		final public Rectangle box;
 		/** Whether the image was generated with a CoordinateTransform or not. */
 		final public boolean coordinate_transformed;
 
 		private PatchImage( ImageProcessor target, ByteProcessor mask, ByteProcessor outside, Rectangle box, boolean coordinate_transformed ) {
 			this.target = target;
 			this.mask = mask;
 			this.outside = outside;
 			this.box = box;
 			this.coordinate_transformed = coordinate_transformed;
 		}
 		
 		/**
 		 * <p>Get the mask.  This is either:</p>
 		 * <ul>
 		 * <li>null for a non-transformed patch without a mask,</li>
 		 * <li>the mask of a non-transformed patch,</li>
 		 * <li>the transformed mask of a transformed patch (including outside
 		 * mask),</li>
 		 * <li>or the outside mask of a transformed patch without a mask,</li>
 		 * </ul>
 		 * 
 		 * @return
 		 */
 		final public ByteProcessor getMask()
 		{
 			return mask == null ? outside == null ? null : outside : mask;
 		}
 	}
 
 	/** Returns a PatchImage object containing the bottom-of-transformation-stack image and alpha mask, if any (except the AffineTransform, which is used for direct hw-accel screen rendering). */
 	public Patch.PatchImage createTransformedImage() {
 		final Patch.PatchImage pi = createCoordinateTransformedImage();
 		if (null != pi) return pi;
 		// else, a new one with the untransformed, original image (a duplicate):
 		final ImageProcessor ip = getImageProcessor();
 		if (null == ip) return null;
 		project.getLoader().releaseToFit(o_width, o_height, type, 3);
		return new PatchImage(ip.duplicate(), project.getLoader().fetchImageMask(this), null, new Rectangle(0, 0, o_width, o_height), false);
 	}
 
 	private boolean has_alpha = false;
 	private boolean alpha_path_checked = false;
 
 	/** Caching system to avoid repeated checks. No automatic memoization ... snif */
 	public final boolean hasAlphaMask() {
 		if (alpha_path_checked) return has_alpha;
 		// else, see if the path exists:
 		try {
 			has_alpha = new File(project.getLoader().getAlphaPath(this)).exists();
 		} catch (Exception e) {
 			IJError.print(e);
 		}
 		alpha_path_checked = true;
 		return has_alpha;
 	}
 
 	public boolean hasAlphaChannel() {
 		return null != ct || hasAlphaMask();
 	}
 
 	/** Must call updateMipMaps() afterwards. Set it to null to remove it. */
 	public void setAlphaMask(ByteProcessor bp) throws IllegalArgumentException {
 		if (null == bp) {
 			if (hasAlphaMask()) {
 				if (project.getLoader().removeAlphaMask(this)) {
 					alpha_path_checked = false;
 				}
 			}
 			return;
 		}
 
 		Utils.log2(o_width, o_height, width, height, bp.getWidth(), bp.getHeight());
 
 		// Check that the alpha mask represented by argument bp
 		// has the appropriate dimensions:
 		if (o_width != bp.getWidth() || o_height != bp.getHeight()) {
 			throw new IllegalArgumentException("Need a mask of identical dimensions as the original image.");
 		}
 
 		project.getLoader().storeAlphaMask(this, bp);
 		alpha_path_checked = false;
 	}
 
 	public void keyPressed(KeyEvent ke) {
 		Object source = ke.getSource();
 		if (! (source instanceof DisplayCanvas)) return;
 		DisplayCanvas dc = (DisplayCanvas)source;
 		final Roi roi = dc.getFakeImagePlus().getRoi();
 
 		switch (ke.getKeyCode()) {
 			case KeyEvent.VK_C:
 				// copy into ImageJ clipboard
 				int mod = ke.getModifiers();
 
 				// Ignoring masks: outside is already black, and ImageJ cannot handle alpha masks.
 				if (0 == (mod ^ (Event.SHIFT_MASK | Event.ALT_MASK))) {
 					// Place the source image, untransformed, into clipboard:
 					ImagePlus imp = getImagePlus();
 					if (null != imp) imp.copy(false);
 				} else if (0 == mod || (0 == (mod ^ Event.SHIFT_MASK))) {
 					CoordinateTransformList<CoordinateTransform> list = null;
 					if (null != ct) {
 						list = new CoordinateTransformList<CoordinateTransform>();
 						list.add(this.ct);
 					}
 					if (0 == mod) { //SHIFT is not down
 						AffineModel2D am = new AffineModel2D();
 						am.set(this.at);
 						if (null == list) list = new CoordinateTransformList<CoordinateTransform>();
 						list.add(am);
 					}
 					ImageProcessor ip;
 					if (null != list) {
 						TransformMesh mesh = new TransformMesh(list, meshResolution, o_width, o_height);
 						TransformMeshMapping mapping = new TransformMeshMapping(mesh);
 						ip = mapping.createMappedImageInterpolated(getImageProcessor());
 					} else {
 						ip = getImageProcessor();
 					}
 					new ImagePlus(this.title, ip).copy(false);
 				}
 				ke.consume();
 				break;
 			case KeyEvent.VK_F:
 				// fill mask with current ROI using 
 				Utils.log2("VK_F: roi is " + roi);
 				if (null != roi && M.isAreaROI(roi)) {
 					Bureaucrat.createAndStart(new Worker.Task("Filling image mask") {
 						public void exec() {
 							addAlphaMask(roi, ProjectToolbar.getForegroundColorValue());
 							try { updateMipMaps().get(); } catch (Throwable t) { IJError.print(t); } // wait
 							Display.repaint();
 						}
 					}, project);
 				}
 				// capturing:
 				ke.consume();
 				break;
 			default:
 				super.keyPressed(ke);
 				break;
 		}
 	}
 
 	@Override
 	Class<?> getInternalDataPackageClass() {
 		return DPPatch.class;
 	}
 
 	@Override
 	Object getDataPackage() {
 		return new DPPatch(this);
 	}
 
 	static private final class DPPatch extends Displayable.DataPackage {
 		final double min, max;
 		CoordinateTransform ct = null;
 		
 		DPPatch(final Patch patch) {
 			super(patch);
 			this.min = patch.min;
 			this.max = patch.max;
 			this.ct = null == ct ? null : patch.ct.copy();
 			// channels is visualization
 			// path is absolute
 			// type is dependent on path, so absolute
 			// o_width, o_height idem
 		}
 		final boolean to2(final Displayable d) {
 			super.to1(d);
 			final Patch p = (Patch) d;
 			boolean mipmaps = false;
 			if (p.min != min || p.max != max || p.ct != ct || (p.ct == ct && ct instanceof CoordinateTransformList<?>)) {
 				Utils.log2("mipmaps is true! " + (p.min != min)  + " " + (p.max != max) + " " + (p.ct != ct) + " " + (p.ct == ct && ct instanceof CoordinateTransformList<?>));
 				mipmaps = true;
 			}
 			p.min = min;
 			p.max = max;
 			p.ct = null == ct ? null : (CoordinateTransform) ct.copy();
 
 			if (mipmaps) {
 				p.project.getLoader().regenerateMipMaps(p);
 			}
 			return true;
 		}
 	}
 
 	/** Considers the alpha mask. */
 	public boolean contains(final int x_p, final int y_p) {
 		if (!hasAlphaChannel()) return super.contains(x_p, y_p);
 		// else, get pixel from image
 		if (project.getLoader().isUnloadable(this)) return super.contains(x_p, y_p);
 		final MipMapImage mipMap = project.getLoader().fetchImage(this, 0.12499); // TODO ideally, would ask for image within 256x256 dimensions, but that would need knowing the screen image dimensions beforehand, or computing it from the CoordinateTransform, which may be very costly.
 		if (Loader.isSignalImage(mipMap.image)) return super.contains(x_p, y_p);
 		final int w = mipMap.image.getWidth(null);
 		final Point2D.Double pd = inverseTransformPoint(x_p, y_p);
 		final int x2 = (int)(pd.x / mipMap.scaleX);
 		final int y2 = (int)(pd.y / mipMap.scaleY);
 		final int[] pvalue = new int[1];
 		final PixelGrabber pg = new PixelGrabber(mipMap.image, x2, y2, 1, 1, pvalue, 0, w);
 		try {
 			pg.grabPixels();
 		} catch (InterruptedException ie) {
 			return super.contains(x_p, y_p);
 		}
 		// Not true if alpha value is zero
 		return 0 != (pvalue[0] & 0xff000000);
 	}
 
 	/** After setting a preprocessor script, it is advisable that you call updateMipMaps() immediately. */
 	public void setPreprocessorScriptPath(final String path) {
 		final String old_path = project.getLoader().getPreprocessorScriptPath(this);
 
 		if (null == path && null == old_path) return;
 
 		project.getLoader().setPreprocessorScriptPath(this, path);
 
 		if (null != old_path || null != path) {
 			// Update dimensions
 			ImagePlus imp = getImagePlus(); // transformed by the new preprocessor script, if any
 			final int w = imp.getWidth();
 			final int h = imp.getHeight();
 			imp = null;
 			if (w != this.o_width || h != this.o_height) {
 				// replace source ImagePlus o_width,o_height
 				int old_o_width = this.o_width;
 				int old_o_height = this.o_height;
 				this.o_width = w;
 				this.o_height = h;
 
 				// scale width,height
 				double old_width = this.width;
 				double old_height = this.height;
 				this.width  *= ((double)this.o_width)  / old_o_width;
 				this.height *= ((double)this.o_height) / old_o_height;
 
 				// translate Patch to preserve the center
 				AffineTransform aff = new AffineTransform();
 				aff.translate((old_width - this.width) / 2, (old_height - this.height) / 2);
 				updateInDatabase("dimensions");
 				preTransform(aff, false);
 			}
 		}
 	}
 
 	/** Add the given roi, in world coords, to the alpha mask, using the given fill value. */
 	public void addAlphaMask(final Roi roi, int value) {
 		if (null == roi || !M.isAreaROI(roi)) return;
 		if (value < 0) value = 0;
 		if (value > 255) value = 255;
 		try {
 			// a roi local to the image bounding box
 			//final Area a = new Area(new Rectangle(0, 0, (int)o_width, (int)o_height));
 			//a.intersect(M.getArea(roi).createTransformedArea(Patch.this.at.createInverse()));
 
 			final Area a = M.areaInInts(M.getArea(roi).createTransformedArea(Patch.this.at.createInverse()));
 			if (M.isEmpty(a)) {
 				Utils.log("ROI does not intersect the active image!");
 				return;
 			}
 			if (!new Rectangle(0, 0, (int)width, (int)height).contains(a.getBounds())) {
 				// Crop most of the superfluous, leaving room for the buggy Area.intersect method to fail gracefully
 				// The cropping speeds up contains(x,y) calls for complex polygons
 				a.intersect(new Area(new Rectangle(-2, -2, (int)width+2, (int)height+2)));
 			}
 
 			ByteProcessor mask = project.getLoader().fetchImageMask(Patch.this);
 
 			// Use imglib to bypass all the problems with ShapeROI
 			// Create a Shape image with background and the Area on it with 'value'
 			final int background = (null != mask && 255 == value) ? 0 : 255;
 			final ShapeList<UnsignedByteType> shapeList = new ShapeList<UnsignedByteType>(new int[]{(int)width, (int)height, 1}, new UnsignedByteType(background));
 			shapeList.addShape(a, new UnsignedByteType(value), new int[]{0});
 			final mpicbg.imglib.image.Image<UnsignedByteType> shapeListImage = new mpicbg.imglib.image.Image<UnsignedByteType>(shapeList, shapeList.getBackground(), "mask");
 
 			ByteProcessor rmask = (ByteProcessor) ImageJFunctions.copyToImagePlus(shapeListImage, ImagePlus.GRAY8).getProcessor();
 
 			if (null != ct) {
 				// inverse the coordinate transform
 				final TransformMesh mesh = new TransformMesh(ct, meshResolution, o_width, o_height);
 				final TransformMeshMapping mapping = new TransformMeshMapping( mesh );
 				rmask = (ByteProcessor) mapping.createInverseMappedImageInterpolated(rmask);
 			}
 
 			if (null == mask) {
 				// There wasn't a mask, hence just set it
 				mask = rmask;
 			} else {
 				final byte[] b1 = (byte[]) mask.getPixels();
 				final byte[] b2 = (byte[]) rmask.getPixels();
 				// Whatever is not background in the new mask gets set on the old mask
 				for (int i=0; i<b1.length; i++) {
 					if (background == (b2[i]&0xff)) continue; // background pixel in new mask 
 					b1[i] = b2[i]; // replace old pixel with new pixel
 				}
 			}
 			setAlphaMask(mask);
 		} catch (NoninvertibleTransformException nite) { IJError.print(nite); }
 	}
 
 	public String getPreprocessorScriptPath() {
 		return project.getLoader().getPreprocessorScriptPath(this);
 	}
 
 	/** Returns an Area in world coords representing the inside of this Patch. The fully alpha pixels are considered outside. */
 	@Override
 	public Area getArea() {
 		if (hasAlphaMask()) {
 			// Read the mask as a ROI for the 0 pixels only and apply the AffineTransform to it:
 			ImageProcessor alpha_mask = project.getLoader().fetchImageMask(this);
 			if (null == alpha_mask) {
 				Utils.log2("Could not retrieve alpha mask for " + this);
 			} else {
 				if (null != ct) {
 					// must transform it
 					final TransformMesh mesh = new TransformMesh(ct, meshResolution, o_width, o_height);
 					final TransformMeshMapping mapping = new TransformMeshMapping( mesh );
 					alpha_mask = mapping.createMappedImage( alpha_mask ); // Without interpolation
 					// Keep in mind the affine of the Patch already contains the translation specified by the mesh bounds.
 				}
 				// Threshold all non-zero areas of the mask:
 				alpha_mask.setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE);
 				ImagePlus imp = new ImagePlus("", alpha_mask);
 				ThresholdToSelection tts = new ThresholdToSelection(); // TODO replace by our much faster method that scans by line, in AmiraImporter
 				tts.setup("", imp);
 				tts.run(alpha_mask);
 				Roi roi = imp.getRoi();
 				if (null == roi) {
 					// All pixels in the alpha mask have a value of zero
 					return new Area();
 				}
 				return M.getArea(roi).createTransformedArea(this.at);
 			}
 		}
 		// No alpha mask, or error in retrieving it:
 		final int[] x = new int[o_width + o_width + o_height + o_height];
 		final int[] y = new int[x.length];
 		int next = 0;
 		// Top edge:
 		for (int i=0; i<=o_width; i++, next++) { // len: o_width + 1
 			x[next] = i;
 			y[next] = 0;
 		}
 		// Right edge:
 		for (int i=1; i<=o_height; i++, next++) { // len: o_height
 			x[next] = o_width;
 			y[next] = i;
 		}
 		// bottom edge:
 		for (int i=o_width-1; i>-1; i--, next++) { // len: o_width
 			x[next] = i;
 			y[next] = o_height;
 		}
 		// left edge:
 		for (int i=o_height-1; i>0; i--, next++) { // len: o_height -1
 			x[next] = 0;
 			y[next] = i;
 		}
 
 		if (null != ct) {
 			final CoordinateTransformList<CoordinateTransform> t = new CoordinateTransformList<CoordinateTransform>();
 			t.add(ct);
 			final TransformMesh mesh = new TransformMesh(this.ct, meshResolution, o_width, o_height);
 			final Rectangle box = mesh.getBoundingBox();
 			final AffineTransform aff = new AffineTransform(this.at);
 			// Must correct for the inverse of the mesh translation, because the affine also includes the translation.
 			aff.translate(-box.x, -box.y);
 			final AffineModel2D affm = new AffineModel2D();
 			affm.set(aff);
 			t.add(affm);
 
 
 			/*
 			 * WORKS FINE, but for points that fall outside the mesh, they don't get transformed!
 			// Do it like Patch does it to generate the mipmap, with a mesh (and all the imprecisions of a mesh):
 			final CoordinateTransformList t = new CoordinateTransformList();
 			final TransformMesh mesh = new TransformMesh(this.ct, meshResolution, o_width, o_height);
 			final AffineTransform aff = new AffineTransform(this.at);
 			t.add(mesh);
 			final AffineModel2D affm = new AffineModel2D();
 			affm.set(aff);
 			t.add(affm);
 			*/
 
 			final float[] f = new float[]{x[0], y[0]};
 			t.applyInPlace(f);
 			final Path2D.Float path = new Path2D.Float(Path2D.Float.WIND_EVEN_ODD, x.length+1);
 			path.moveTo(f[0], f[1]);
 
 			for (int i=1; i<x.length; i++) {
 				f[0] = x[i];
 				f[1] = y[i];
 				t.applyInPlace(f);
 				path.lineTo(f[0], f[1]);
 			}
 			path.closePath(); // line to last call to moveTo
 
 			return new Area(path);
 		} else {
 			return new Area(new Polygon(x, y, x.length)).createTransformedArea(this.at);
 		}
 	}
 
 	/** Defaults to setMinAndMax = true. */
 	static public ImageProcessor makeFlatImage(final int type, final Layer layer, final Rectangle srcRect, final double scale, final Collection<Patch> patches, final Color background) {
 		return makeFlatImage(type, layer, srcRect, scale, patches, background, true);
 	}
 	
 	/** Creates an ImageProcessor of the specified type.
 	 *  @param type Any of ImagePlus.GRAY_8, GRAY_16, GRAY_32 or COLOR_RGB.
 	 *  @param srcRect the box in world coordinates to make an image out of.
 	 *  @param scale may be up to 1.0.
 	 *  @param patches The list of patches to paint. The first gets painted first (at the bottom).
 	 *  @param background The color with which to paint the outsides where no image paints into.
 	 *  @param setMinAndMax defines whether the min and max of each Patch is set before pasting the Patch.
 	 */
 	static public ImageProcessor makeFlatImage(final int type, final Layer layer, final Rectangle srcRect, final double scale, final Collection<Patch> patches, final Color background, final boolean setMinAndMax) {
 		final ImageProcessor ip;
 		final int W, H;
 		if (scale < 1) {
 			W = (int)(srcRect.width * scale);
 			H = (int)(srcRect.height * scale);
 		} else {
 			W = srcRect.width;
 			H = srcRect.height;
 		}
 		switch (type) {
 			case ImagePlus.GRAY8:
 				ip = new ByteProcessor(W, H);
 				break;
 			case ImagePlus.GRAY16:
 				ip = new ShortProcessor(W, H);
 				break;
 			case ImagePlus.GRAY32:
 				ip = new FloatProcessor(W, H);
 				break;
 			case ImagePlus.COLOR_RGB:
 				ip = new ColorProcessor(W, H);
 				break;
 			default:
 				Utils.logAll("Cannot create an image of type " + type + ".\nSupported types: 8-bit, 16-bit, 32-bit and RGB.");
 				return null;
 		}
 
 		// Fill with background
 		if (null != background && Color.black != background) {
 			ip.setColor(background);
 			ip.fill();
 		}
 
 		AffineModel2D sc = null;
 		if ( scale < 1.0 )
 		{
 			sc = new AffineModel2D();
 			sc.set( ( float )scale, 0, 0, ( float )scale, 0, 0 );
 		}
 		for ( final Patch p : patches )
 		{
 			// TODO patches seem to come in in inverse order---find out why
 			
 			// A list to represent all the transformations that the Patch image has to go through to reach the scaled srcRect image
 			final CoordinateTransformList< CoordinateTransform > list = new CoordinateTransformList< CoordinateTransform >();
 
 			final AffineTransform at = new AffineTransform();
 			at.translate( -srcRect.x, -srcRect.y );
 			at.concatenate( p.getAffineTransform() );
 			
 			// 1. The coordinate tranform of the Patch, if any
 			final CoordinateTransform ct = p.getCoordinateTransform();
 			if (null != ct) {
 				list.add(ct);
 				// Remove the translation in the patch_affine that the ct added to it
 				final Rectangle box = p.getCoordinateTransformBoundingBox();
 				at.translate( -box.x, -box.y );
 			}
 			
 			// 2. The affine transform of the Patch
 			final AffineModel2D patch_affine = new AffineModel2D();
 			patch_affine.set( at );
 			list.add( patch_affine );
 
 			// 3. The desired scaling
 			if (null != sc) patch_affine.preConcatenate( sc );
 
 			final CoordinateTransformMesh mesh = new CoordinateTransformMesh( list, p.meshResolution, p.getOWidth(), p.getOHeight() );
 			final mpicbg.ij.TransformMeshMapping<CoordinateTransformMesh> mapping = new mpicbg.ij.TransformMeshMapping<CoordinateTransformMesh>( mesh );
 			
 			// 4. Convert the patch to the required type
 			ImageProcessor pi = p.getImageProcessor();
 			if (setMinAndMax) {
 				pi = pi.duplicate();
 				pi.setMinAndMax(p.min, p.max);
 			}
 			switch ( type )
 			{
 			case ImagePlus.GRAY8:
 				pi = pi.convertToByte( true );
 				break;
 			case ImagePlus.GRAY16:
 				pi = pi.convertToShort( true );
 				break;
 			case ImagePlus.GRAY32:
 				pi = pi.convertToFloat();
 				break;
 			default: // ImagePlus.COLOR_RGB and COLOR_256
 				pi = pi.convertToRGB();
 				break;
 			}
 			
 			/* TODO for taking into account independent min/max setting for each patch,
 			 * we will need a mapping with an `intensity transfer function' to be implemented.
 			 */
 			mapping.mapInterpolated( pi, ip );
 		}
 
 		return ip;
 	}
 
 	/** Make the border have an alpha of zero. */
 	public boolean maskBorder(final int size) {
 		return maskBorder(size, size, size, size);
 	}
 	/** Make the border have an alpha of zero. */
 	public boolean maskBorder(final int left, final int top, final int right, final int bottom) {
 		int w = o_width - right - left;
 		int h = o_height - top - bottom;
 		if (w < 0 || h < 0 || left > o_width || top > o_height) {
 			Utils.log("Cannot cut border for patch " + this + " : border off image bounds.");
 			return false;
 		}
 		try {
 			ByteProcessor bp = project.getLoader().fetchImageMask(this);
 			if (null == bp) {
 				bp = new ByteProcessor(o_width, o_height);
 				bp.setRoi(new Roi(left, top, w, h));
 				bp.setValue(255);
 				bp.fill();
 			} else {
 				// make borders black
 				bp.setValue(0);
 				for (Roi r : new Roi[]{new Roi(0, 0, o_width, top),
 						       new Roi(0, top, left, o_height - top - bottom),
 						       new Roi(0, o_height - bottom, o_width, bottom),
 						       new Roi(o_width - right, top, right, o_height - top - bottom)}) {
 					bp.setRoi(r);
 					bp.fill();
 				}
 			}
 			setAlphaMask(bp);
 		} catch (Exception e) {
 			IJError.print(e);
 			return false;
 		}
 		return true;
 	}
 
 	/** Use this instead of getAreaAt which calls getArea which is ... dog slow for something like buckets. */
 	@Override
 	protected Area getAreaForBucket(final Layer layer) {
 		return new Area(getPerimeter());
 	}
 
 	@Override
 	protected boolean isRoughlyInside(final Layer layer, final Rectangle r) {
 		return layer == this.layer && r.intersects(getBoundingBox());
 	}
 }
