 /**
  * 
  */
 package mpicbg.trakem2.align;
 
 import java.awt.Color;
 import java.awt.Rectangle;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.NoninvertibleTransformException;
 import java.awt.geom.Area;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 
 import mpicbg.ij.FeatureTransform;
 import mpicbg.ij.SIFT;
 import mpicbg.imagefeatures.Feature;
 import mpicbg.imagefeatures.FloatArray2DSIFT;
 import mpicbg.models.AbstractAffineModel2D;
 import mpicbg.models.AffineModel2D;
 import mpicbg.models.NotEnoughDataPointsException;
 import mpicbg.models.Point;
 import mpicbg.models.PointMatch;
 import mpicbg.models.SimilarityModel2D;
 import mpicbg.models.Tile;
 import mpicbg.models.Transforms;
 import mpicbg.trakem2.transform.CoordinateTransform;
 import mpicbg.trakem2.transform.CoordinateTransformList;
 import mpicbg.trakem2.transform.MovingLeastSquaresTransform;
 import mpicbg.trakem2.transform.RigidModel2D;
 import mpicbg.trakem2.transform.TranslationModel2D;
 import mpicbg.models.NoninvertibleModelException;
 import mpicbg.trakem2.transform.InvertibleCoordinateTransform;
 
 import ij.IJ;
 import ij.ImagePlus;
 import ij.gui.GenericDialog;
 import ini.trakem2.display.Display;
 import ini.trakem2.display.Displayable;
 import ini.trakem2.display.Layer;
 import ini.trakem2.display.LayerSet;
 import ini.trakem2.display.Patch;
 import ini.trakem2.display.Selection;
 import ini.trakem2.display.VectorData;
 import ini.trakem2.display.VectorDataTransform;
 import ini.trakem2.persistence.DBObject;
 import ini.trakem2.utils.Worker;
 import ini.trakem2.utils.Bureaucrat;
 import ini.trakem2.utils.IJError;
 import ini.trakem2.utils.M;
 import ini.trakem2.utils.Utils;
 
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Future;
 
 /**
  * Methods collection to be called from the GUI for alignment tasks.
  *
  */
 final public class AlignTask
 {
 	static protected boolean tilesAreInPlace = false;
 	static protected boolean largestGraphOnly = false;
 	static protected boolean hideDisconnectedTiles = false;
 	static protected boolean deleteDisconnectedTiles = false;
 	static protected boolean deform = false;
 	
 	final static public Bureaucrat alignSelectionTask ( final Selection selection )
 	{
 		Worker worker = new Worker("Aligning selected images", false, true) {
 			public void run() {
 				startedWorking();
 				try {
 					alignSelection( selection );
 					Display.repaint(selection.getLayer());
 				} catch (Throwable e) {
 					IJError.print(e);
 				} finally {
 					finishedWorking();
 				}
 			}
 			public void cleanup() {
 				if (!selection.isEmpty())
 					selection.getLayer().getParent().undoOneStep();
 			}
 		};
 		return Bureaucrat.createAndStart( worker, selection.getProject() );
 	}
 
 
 	final static public void alignSelection( final Selection selection )
 	{
 		List< Patch > patches = new ArrayList< Patch >();
 		for ( Displayable d : selection.getSelected() )
 			if ( d instanceof Patch ) patches.add( ( Patch )d );
 
 		List< Patch > fixedPatches = new ArrayList< Patch >();
 
 		// Add active Patch, if any, as the nail
 		Displayable active = selection.getActive();
 		if ( null != active && active instanceof Patch )
 			fixedPatches.add( (Patch)active );
 
 		// Add all locked Patch instances to fixedPatches
 		for (final Patch patch : patches)
 			if ( patch.isLocked() )
 				fixedPatches.add( patch );
 
 		alignPatches( patches, fixedPatches );
 	}
 
 	final static public Bureaucrat alignPatchesTask ( final List< Patch > patches , final List< Patch > fixedPatches )
 	{
 		if ( 0 == patches.size())
 		{
 			Utils.log("Can't align zero patches.");
 			return null;
 		}
 		Worker worker = new Worker("Aligning images", false, true) {
 			public void run() {
 				startedWorking();
 				try {
 					alignPatches( patches, fixedPatches );
 					Display.repaint();
 				} catch (Throwable e) {
 					IJError.print(e);
 				} finally {
 					finishedWorking();
 				}
 			}
 			public void cleanup() {
 				patches.get(0).getLayer().getParent().undoOneStep();
 			}
 		};
 		return Bureaucrat.createAndStart( worker, patches.get(0).getProject() );
 	}
 
 	/**
 	 * @param patches: the list of Patch instances to align, all belonging to the same Layer.
 	 * @param fixed: the list of Patch instances to keep locked in place, if any.
 	 */
 	final static public void alignPatches( final List< Patch > patches , final List< Patch > fixedPatches )
 	{
 		if ( patches.size() < 2 )
 		{
 			Utils.log("No images to align.");
 			return;
 		}
 
 		for ( final Patch patch : fixedPatches )
 		{
 			if ( !patches.contains( patch ) )
 			{
 				Utils.log("The list of fixed patches contains at least one Patch not included in the list of patches to align!");
 				return;
 			}
 		}
 
 		//final Align.ParamOptimize p = Align.paramOptimize;
 		final GenericDialog gd = new GenericDialog( "Align Tiles" );
 		Align.paramOptimize.addFields( gd );
 		
 		gd.addMessage( "Miscellaneous:" );
 		gd.addCheckbox( "tiles are rougly in place", tilesAreInPlace );
 		gd.addCheckbox( "consider largest graph only", largestGraphOnly );
 		gd.addCheckbox( "hide tiles from non-largest graph", hideDisconnectedTiles );
 		gd.addCheckbox( "delete tiles from non-largest graph", deleteDisconnectedTiles );
 		
 		gd.showDialog();
 		if ( gd.wasCanceled() ) return;
 		
 		Align.paramOptimize.readFields( gd );
 		tilesAreInPlace = gd.getNextBoolean();
 		largestGraphOnly = gd.getNextBoolean();
 		hideDisconnectedTiles = gd.getNextBoolean();
 		deleteDisconnectedTiles = gd.getNextBoolean();
 		
 		final Align.ParamOptimize p = Align.paramOptimize.clone();
 
 		alignPatches( p, patches, fixedPatches, tilesAreInPlace, largestGraphOnly, hideDisconnectedTiles, deleteDisconnectedTiles );
 	}
 
 	/** Montage each layer independently, with SIFT.
 	 *  Does NOT register layers to each other.
 	 *  Considers visible Patches only. */
 	final static public Bureaucrat montageLayersTask(final List<Layer> layers) {
 		if (null == layers || layers.isEmpty()) return null;
 		return Bureaucrat.createAndStart(new Worker.Task("Montaging layers", true) {
 			public void exec() {
 				//final Align.ParamOptimize p = Align.paramOptimize;
 				final GenericDialog gd = new GenericDialog( "Montage Layers" );
 				Align.paramOptimize.addFields( gd );
 				
 				gd.addMessage( "Miscellaneous:" );
 				gd.addCheckbox( "tiles are rougly in place", tilesAreInPlace );
 				gd.addCheckbox( "consider largest graph only", largestGraphOnly );
 				gd.addCheckbox( "hide tiles from non-largest graph", hideDisconnectedTiles );
 				gd.addCheckbox( "delete tiles from non-largest graph", deleteDisconnectedTiles );
 				
 				gd.showDialog();
 				if ( gd.wasCanceled() ) return;
 				
 				Align.paramOptimize.readFields( gd );
 				tilesAreInPlace = gd.getNextBoolean();
 				largestGraphOnly = gd.getNextBoolean();
 				hideDisconnectedTiles = gd.getNextBoolean();
 				deleteDisconnectedTiles = gd.getNextBoolean();
 				
 				final Align.ParamOptimize p = Align.paramOptimize.clone();
 				montageLayers(p, layers, tilesAreInPlace, largestGraphOnly, hideDisconnectedTiles, deleteDisconnectedTiles );
 			}
 		}, layers.get(0).getProject());
 	}
 
 	final static public void montageLayers(
 			final Align.ParamOptimize p,
 			final List<Layer> layers,
 			final boolean tilesAreInPlace,
 			final boolean largestGraphOnly,
 			final boolean hideDisconnectedTiles,
 			final boolean deleteDisconnectedTiles ) {
 		int i = 0;
 		for (final Layer layer : layers) {
 			if (Thread.currentThread().isInterrupted()) return;
 			Collection<Displayable> patches = layer.getDisplayables(Patch.class, true);
 			if (patches.isEmpty()) continue;
 			for (final Displayable patch : patches) {
 				if (patch.isLinked() && !patch.isOnlyLinkedTo(Patch.class)) {
 					Utils.log("Cannot montage layer " + layer + "\nReason: at least one Patch is linked to non-image data: " + patch);
 					continue;
 				}
 			}
 			Utils.log("====\nMontaging layer " + layer);
 			Utils.showProgress(((double)i)/layers.size());
 			i++;
 			alignPatches(p, new ArrayList<Patch>((Collection<Patch>)(Collection)patches), new ArrayList<Patch>(), tilesAreInPlace, largestGraphOnly, hideDisconnectedTiles, deleteDisconnectedTiles );
 			Display.repaint(layer);
 		}
 	}
 
 	final static private class InverseICT implements mpicbg.models.InvertibleCoordinateTransform {
 		final mpicbg.models.InvertibleCoordinateTransform ict;
 		/** Sets this to the inverse of ict. */
 		InverseICT(final mpicbg.models.InvertibleCoordinateTransform ict) {
 			this.ict = ict;
 		}
 		public final float[] apply(final float[] p) {
 			float[] q = p.clone();
 			applyInPlace(q);
 			return q;
 		}
 		public final float[] applyInverse(final float[] p) {
 			float[] q = p.clone();
 			applyInverseInPlace(q);
 			return q;
 		}
 		public final void applyInPlace(final float[] p) {
 			try {
 				ict.applyInverseInPlace(p);
			} catch (NoninvertibleModelException e) {
				Utils.log2("Point outside mesh: " + p[0] + ", " + p[1]);
			}
 		}
 		public final void applyInverseInPlace(final float[] p) {
 			ict.applyInPlace(p);
 		}
 		public final InvertibleCoordinateTransform createInverse() {
 			return null;
 		}
 	}
 
 	final static public void transformPatchesAndVectorData(final Layer layer, final AffineTransform a) {
 		AlignTask.transformPatchesAndVectorData((Collection<Patch>)(Collection)layer.getDisplayables(Patch.class),
 			new Runnable() { public void run() {
 				layer.apply( Patch.class, a );
 			}});
 	}
 
 	/*
 	final static public Map<Long,Patch.TransformProperties> createTransformPropertiesTable(final Collection<Patch> patches) {
 		final Map<Long,Patch.TransformProperties> tp = new HashMap<Long,Patch.TransformProperties>();
 		// Parallelize! This operation can be insanely expensive
 		final int nproc = Runtime.getRuntime().availableProcessors();
 		final ExecutorService exec = Utils.newFixedThreadPool(nproc, "AlignTask-createTransformPropertiesTable");
 		final LinkedList<Future> tasks = new LinkedList<Future>();
 		final Thread current = Thread.currentThread();
 		final AtomicInteger counter = new AtomicInteger(0);
 		Utils.log2("0/" + patches.size());
 		try {
 		for (final Patch patch : patches) {
 			tasks.add(exec.submit(new Runnable() {
 				public void run() {
 					Patch.TransformProperties props = patch.getTransformPropertiesCopy();
 					synchronized (tp) {
 						tp.put(patch.getId(), props);
 					}
 					// report
 					final int i = counter.incrementAndGet();
 					if (0 == i % 16) {
 						final String msg = new StringBuilder().append(i).append('/').append(patches.size()).toString();
 						Utils.log2(msg);
 						Utils.showStatus(msg);
 					}
 				}
 			}));
 			// When reaching 2*nproc, wait for nproc to complete
 			if (0 == tasks.size() % (nproc+nproc)) {
 				if (current.isInterrupted()) return tp;
 				int i = 0;
 				while (i < nproc) {
 					try { tasks.removeFirst().get(); } catch (Exception e) { IJError.print(e); }
 					i++;
 				}
 			}
 		}
 		// Wait for remaining tasks
 		Utils.wait(tasks);
 		Utils.log2(patches.size() + "/" + patches.size() + " -- done!");
 		} catch (Throwable t) {
 			IJError.print(t);
 		} finally {
 			exec.shutdownNow();
 		}
 
 		return tp;
 	}
 	*/
 
 	static public final class ReferenceData {
 		/** Patch id vs transform */
 		final Map<Long,Patch.TransformProperties> tp;
 		/** A map of Displayable vs a map of Layer id vs list of Patch ids in that Layer that lay under the Patch, sorted by stack index. */
 		final Map<Displayable,Map<Long,TreeMap<Integer,Long>>> underlying;
 		/** A list of the Layer ids form which at least one Patch was used to determine a transform of part of a VectorData instance. I.e. the visited layers. */
 		final Set<Long> src_layer_lids_used;
 		ReferenceData(final Map<Long,Patch.TransformProperties> tp, Map<Displayable,Map<Long,TreeMap<Integer,Long>>> underlying, Set<Long> src_layer_lids_used) {
 			this.tp = tp;
 			this.underlying = underlying;
 			this.src_layer_lids_used = src_layer_lids_used;
 		}
 	}
 
 	/** Creates a map only for visible patches that intersect vdata.
 	 *  @param src_vdata represents the VectorData instances in original form, of the original project and layer set.
 	 *  @param tgt_data if not null, it must have the same size as src_data and their elements correspond one-to-one (as in, tgt element a clone of src element at the same index).
 	 *  @param lids_to_operate The id of the layers on which any operation will be done
 	 *  tgt_data enables transformVectorData to apply the transforms to copies of the src_vdata in another project. */
 	final static public ReferenceData createTransformPropertiesTable(final List<Displayable> src_vdata, final List<Displayable> tgt_vdata, final Set<Long> lids_to_operate) {
 		if (src_vdata.isEmpty()) return null;
 		final Map<Long,Patch.TransformProperties> tp = new HashMap<Long,Patch.TransformProperties>();
 		// A map of Displayable vs a map of Layer id vs list of Patch ids in that Layer that lay under the Patch, sorted by stack index
 		final Map<Displayable,Map<Long,TreeMap<Integer,Long>>> underlying = new HashMap<Displayable,Map<Long,TreeMap<Integer,Long>>>();
 
 		// The set of layers used
 		final Set<Long> src_layer_lids_used = new HashSet<Long>();
 
 		// Parallelize! This operation can be insanely expensive
 		final int nproc = Runtime.getRuntime().availableProcessors();
 		final ExecutorService exec = Utils.newFixedThreadPool(nproc, "AlignTask-createTransformPropertiesTable");
 		final List<Future<?>> dtasks = new ArrayList<Future<?>>();
 		final List<Future<?>> ltasks = new ArrayList<Future<?>>();
 		final Thread current = Thread.currentThread();
 
 		try {
 			for (int i=src_vdata.size()-1; i>-1; i--) {
 				final Displayable src_d = src_vdata.get(i);
 				if (!(src_d instanceof VectorData)) continue; // filter out
 				final Displayable tgt_d = null == tgt_vdata ? src_d : tgt_vdata.get(i); // use src_d if tgt_vdata is null
 				// Some checking
 				if (!(tgt_d instanceof VectorData)) {
 					Utils.log("WARNING ignoring provided tgt_vdata " + tgt_d + " which is NOT a VectorData instance!");
 					continue;
 				}
 				if (src_d.getClass() != tgt_d.getClass()) {
 					Utils.log("WARNING src_d and tgt_d are instances of different classes:\n  src_d :: " + src_d + "\n  tgt_d :: " + tgt_d);
 				}
 
 				dtasks.add(exec.submit(new Runnable() {
 					public void run() {
 						final Map<Long,TreeMap<Integer,Long>> under = new HashMap<Long,TreeMap<Integer,Long>>();
 						synchronized (underlying) {
 							underlying.put(tgt_d, under);
 						}
 
 						if (current.isInterrupted()) return;
 
 						// Iterate the layers in which this VectorData has any data AND which have to be transformed
 						for (final Long olid : src_d.getLayerIds()) {
 							final long lid = olid.longValue();
 
 							if (!lids_to_operate.contains(lid)) continue; // layer with id 'lid' is not affected
 
 							final Layer la = src_d.getLayerSet().getLayer(lid);
 
 							final Area a = src_d.getAreaAt(la);
 							if (null == a || a.isEmpty()) {
 								continue; // does not paint in the layer
 							}
 
 							// The list of patches that lay under VectorData d, sorted by their stack index in the layer
 							final TreeMap<Integer,Long> stacked_patch_ids = new TreeMap<Integer,Long>();
 							synchronized (under) {
 								under.put(lid, stacked_patch_ids);
 							}
 
 							final boolean[] layer_visited = new boolean[]{false};
 
 							// Iterate source patches
 							for (final Patch patch : (Collection<Patch>)(Collection)la.getDisplayables(Patch.class, a, true)) { // pick visible patches only
 								if (current.isInterrupted()) return;
 
 								try {
 									ltasks.add(exec.submit(new Runnable() {
 										public void run() {
 											if (current.isInterrupted()) return;
 											synchronized (patch) {
 												Patch.TransformProperties props;
 												synchronized (tp) {
 													props = tp.get(patch.getId());
 												}
 												if (null == props) {
 													props = patch.getTransformPropertiesCopy();
 													// Cache the props
 													synchronized (tp) {
 														tp.put(patch.getId(), props);
 													}
 												}
 												// Cache this patch as under the VectorData d
 												synchronized (stacked_patch_ids) {
 													stacked_patch_ids.put(la.indexOf(patch), patch.getId()); // sorted by stack index
 													//Utils.log("Added patch for layer " + la + " with stack index " + la.indexOf(patch) + ", patch " + patch);
 												}
 
 												if (!layer_visited[0]) {
 													// synch may fail to avoid adding it twice
 													// but it's ok since it's a Set anyway
 													layer_visited[0] = true;
 													synchronized (src_layer_lids_used) {
 														src_layer_lids_used.add(la.getId());
 													}
 												}
 											}
 										}
 									}));
 								} catch (Throwable t) {
 									IJError.print(t);
 									return;
 								}
 							}
 						}
 					}
 				}));
 			}
 			Utils.wait(dtasks);
 			Utils.wait(ltasks);
 
 		} catch (Throwable t) {
 			IJError.print(t);
 		} finally {
 			exec.shutdownNow();
 		}
 
 		return new ReferenceData(tp, underlying, src_layer_lids_used);
 	}
 
 	/** For registering within the same project instance. */
 	final static public void transformPatchesAndVectorData(final Collection<Patch> patches, final Runnable alignment) {
 		if (patches.isEmpty()) {
 			Utils.log("No patches to align!");
 			return;
 		}
 		// 1 - Collect all VectorData to transform
 		final LayerSet ls = patches.iterator().next().getLayerSet();
 		final List<Displayable> vdata = ls.getDisplayables(); // from all layers
 		vdata.addAll(ls.getZDisplayables()); // no lazy seqs, no filter functions ... ole!
 		for (final Iterator<Displayable> it = vdata.iterator(); it.hasNext(); ) {
 			if (it.next() instanceof VectorData) continue;
 			it.remove();
 		}
 		// 2 - Store current transformation of each Patch under any VectorData
 		final Set<Long> lids = new HashSet<Long>();
 		for (final Patch p : patches) lids.add(p.getLayer().getId());
 		final ReferenceData rd = createTransformPropertiesTable(vdata, null, lids);
 		// 3 - Align:
 		alignment.run();
 		// TODO check that alignTiles doesn't change the dimensions/origin of the LayerSet! That would invalidate the table of TransformProperties
 		// 4 - Transform VectorData instances to match the position of the Patch instances over which they were defined
 		if (null != rd && !vdata.isEmpty()) transformVectorData(rd, vdata, ls);
 	}
 
 	final static public void transformVectorData
 		(final ReferenceData rd, 			/* The transformations of patches before alignment. */
 		 final Collection<Displayable> vdata, 		/* The VectorData instances to transform along with images. */
 		 final LayerSet target_layerset) 		/* The LayerSet in which the vdata and the transformed images exist. */
 	{
 		final ExecutorService exec = Utils.newFixedThreadPool("AlignTask-transformVectorData");
 		final Collection<Future<?>> fus = new ArrayList<Future<?>>();
 
 		final HashMap<Long,Layer> lidm = new HashMap<Long,Layer>();
 		for (final Long lid : rd.src_layer_lids_used) {
 			Layer la = target_layerset.getLayer(lid.longValue());
 			if (null == la) {
 				Utils.log("ERROR layer with id " + lid + " NOT FOUND in target layerset!");
 				continue;
 			}
 			lidm.put(lid, la);
 		}
 
 		for (final Map.Entry<Displayable,Map<Long,TreeMap<Integer,Long>>> ed : rd.underlying.entrySet()) {
 			final Displayable d = ed.getKey(); // The VectorData instance to transform
 			// Process Displayables concurrently:
 			fus.add(exec.submit(new Runnable() { public void run() {
 				for (final Map.Entry<Long,TreeMap<Integer,Long>> el : ed.getValue().entrySet()) {
 					// The entry has the id of the layer and the stack-index-ordered list of Patch that intersect VectorData d in that Layer
 					final Layer layer = lidm.get(el.getKey());
 					if (null == layer) {
 						Utils.log("ERROR layer with id " + el.getKey() + " NOT FOUND in target layerset!");
 						continue;
 					}
 					//Utils.log("Editing Displayable " + d + " at layer " + layer);
 					final ArrayList<Long> pids = new ArrayList<Long>(el.getValue().values()); // list of Patch ids affecting VectorData/Displayable d
 					Collections.reverse(pids); // so now Patch ids are sorted from top to bottom
 					// The area already processed in the layer
 					final Area used_area = new Area();
 					// The map of areas vs transforms for each area to apply to the VectorData, to its data within the layer only
 					final VectorDataTransform vdt = new VectorDataTransform(layer);
 					// The list of transforms to apply to each VectorData
 					for (final long pid : pids) {
 						// Find the Patch with id 'pid' in Layer 'la' of the target LayerSet:
 						final DBObject ob = layer.findById(pid);
 						if (null == ob || !(ob instanceof Patch)) {
 							Utils.log("ERROR layer with id " + layer.getId() + " DOES NOT CONTAIN a Patch with id " + pid);
 							continue;
 						}
 						final Patch patch = (Patch)ob;
 						final Patch.TransformProperties props = rd.tp.get(pid); // no need to synch, read only from now on
 						if (null == props) {
 							Utils.log("ERROR: could not find any Patch.TransformProperties for patch " + patch);
 							continue;
 						}
 						final Area a = new Area(props.area);
 						a.subtract(used_area);
 						if (M.isEmpty(a)) {
 							continue; // skipping fully occluded Patch
 						}
 						// Accumulate:
 						used_area.add(props.area);
 
 						// For the remaining area within this Layer, define a transform
 						// Generate a CoordinateTransformList that includes:
 						// 1 - an inverted transform from Patch coords to world coords
 						// 2 - the CoordinateTransform of the Patch, if any
 						// 3 - the AffineTransform of the Patch
 						//
 						// The idea is to first send the data from world to pixel space of the Patch, using the old transfroms,
 						// and then from pixel space of the Patch to world, using the new transforms.
 				
 
 						final CoordinateTransformList tlist = new CoordinateTransformList();
 						// 1. Inverse of the old affine: from world into the old patch mipmap
 						final mpicbg.models.AffineModel2D aff_inv = new mpicbg.models.AffineModel2D();
 						try {
 							aff_inv.set(props.at.createInverse());
 						} catch (NoninvertibleTransformException nite) {
 							Utils.log("ERROR: could not invert the affine transform for Patch " + patch);
 							IJError.print(nite);
 							continue;
 						}
 						tlist.add(aff_inv);
 
 						// 2. Inverse of the old coordinate transform of the Patch: from old mipmap to pixels in original image
 						if (null != props.ct) {
 							// The props.ct is a CoordinateTransform, not necessarily an InvertibleCoordinateTransform
 							// So the mesh is necessary to ensure the invertibility
 							mpicbg.trakem2.transform.TransformMesh mesh = new mpicbg.trakem2.transform.TransformMesh(props.ct, 32, props.o_width, props.o_height);
 							/* // Apparently not needed; the inverse affine in step 1 took care of it.
 							 * // (the affine of step 1 includes the mesh translation)
 							Rectangle box = mesh.getBoundingBox();
 							AffineModel2D aff = new AffineModel2D();
 							aff.set(new AffineTransform(1, 0, 0, 1, box.x, box.y));
 							tlist.add(aff);
 							*/
 							tlist.add(new InverseICT(mesh));
 						}
 
 						// 3. New coordinate transform of the Patch: from original image to new mipmap
 						final mpicbg.trakem2.transform.CoordinateTransform ct = patch.getCoordinateTransform();
 						if (null != ct) {
 							tlist.add(ct);
 							mpicbg.trakem2.transform.TransformMesh mesh = new mpicbg.trakem2.transform.TransformMesh(ct, 32, patch.getOWidth(), patch.getOHeight());
 							// correct for mesh bounds -- Necessary because it comes from the other side, and the removal of the translation here is re-added by the affine in step 4!
 							Rectangle box = mesh.getBoundingBox();
 							AffineModel2D aff = new AffineModel2D();
 							aff.set(new AffineTransform(1, 0, 0, 1, -box.x, -box.y));
 							tlist.add(aff);
 						}
 
 						// 4. New affine transform of the Patch: from mipmap to world
 						final mpicbg.models.AffineModel2D new_aff = new mpicbg.models.AffineModel2D();
 						new_aff.set(patch.getAffineTransform());
 						tlist.add(new_aff);
 
 						/*
 						// TODO Consider caching the tlist for each Patch, or for a few thousand of them maximum.
 						//      But it could blow up memory astronomically.
 
 						// The old part:
 						final mpicbg.models.InvertibleCoordinateTransformList old = new mpicbg.models.InvertibleCoordinateTransformList();
 						if (null != props.ct) {
 							mpicbg.trakem2.transform.TransformMesh mesh = new mpicbg.trakem2.transform.TransformMesh(props.ct, 32, props.o_width, props.o_height);
 							old.add(mesh);
 						}
 						final mpicbg.models.AffineModel2D old_aff = new mpicbg.models.AffineModel2D();
 						old_aff.set(props.at);
 						old.add(old_aff);
 
 						tlist.add(new InverseICT(old));
 
 						// The new part:
 						final mpicbg.models.AffineModel2D new_aff = new mpicbg.models.AffineModel2D();
 						new_aff.set(patch.getAffineTransform());
 						tlist.add(new_aff);
 						final mpicbg.trakem2.transform.CoordinateTransform ct = patch.getCoordinateTransform();
 						if (null != ct) tlist.add(ct);
 						*/
 
 						vdt.add(a, tlist);
 					}
 
 					// Apply the map of area vs tlist for the data section of d within the layer:
 					try {
 						((VectorData)d).apply(vdt);
 					} catch (Exception t) {
 						Utils.log("ERROR transformation failed for " + d + " at layer " + layer);
 						IJError.print(t);
 					}
 				}
 			}}));
 		}
 
 		Utils.wait(fus);
 		Display.repaint();
 	}
 
 	final static public void alignPatches(
 			final Align.ParamOptimize p,
 			final List< Patch > patches,
 			final List< Patch > fixedPatches,
 			final boolean tilesAreInPlace,
 			final boolean largestGraphOnly,
 			final boolean hideDisconnectedTiles,
 			final boolean deleteDisconnectedTiles )
 	{
 		final List< AbstractAffineTile2D< ? > > tiles = new ArrayList< AbstractAffineTile2D< ? > >();
 		final List< AbstractAffineTile2D< ? > > fixedTiles = new ArrayList< AbstractAffineTile2D< ? > > ();
 		Align.tilesFromPatches( p, patches, fixedPatches, tiles, fixedTiles );
 
 		transformPatchesAndVectorData(patches, new Runnable() {
 			public void run() {
 				alignTiles( p, tiles, fixedTiles, tilesAreInPlace, largestGraphOnly, hideDisconnectedTiles, deleteDisconnectedTiles );
 				Display.repaint();
 			}
 		});
 	}
 
 	final static public void alignTiles(
 			final Align.ParamOptimize p,
 			final List< AbstractAffineTile2D< ? > > tiles,
 			final List< AbstractAffineTile2D< ? > > fixedTiles,
 			final boolean tilesAreInPlace,
 			final boolean largestGraphOnly,
 			final boolean hideDisconnectedTiles,
 			final boolean deleteDisconnectedTiles )
 	{
 		final List< AbstractAffineTile2D< ? >[] > tilePairs = new ArrayList< AbstractAffineTile2D< ? >[] >();
 		if ( tilesAreInPlace )
 			AbstractAffineTile2D.pairOverlappingTiles( tiles, tilePairs );
 		else
 			AbstractAffineTile2D.pairTiles( tiles, tilePairs );
 		
 		Align.connectTilePairs( p, tiles, tilePairs, Runtime.getRuntime().availableProcessors() );
 		
 		if ( Thread.currentThread().isInterrupted() ) return;
 		
 		List< Set< Tile< ? > > > graphs = AbstractAffineTile2D.identifyConnectedGraphs( tiles );
 		
 		final List< AbstractAffineTile2D< ? > > interestingTiles;
 		if ( largestGraphOnly )
 		{
 			/* find largest graph. */
 			
 			Set< Tile< ? > > largestGraph = null;
 			for ( Set< Tile< ? > > graph : graphs )
 				if ( largestGraph == null || largestGraph.size() < graph.size() )
 					largestGraph = graph;
 			
 			interestingTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 			for ( Tile< ? > t : largestGraph )
 				interestingTiles.add( ( AbstractAffineTile2D< ? > )t );
 			
 			if ( hideDisconnectedTiles )
 				for ( AbstractAffineTile2D< ? > t : tiles )
 					if ( !interestingTiles.contains( t ) )
 						t.getPatch().setVisible( false );
 			if ( deleteDisconnectedTiles )
 				for ( AbstractAffineTile2D< ? > t : tiles )
 					if ( !interestingTiles.contains( t ) )
 						t.getPatch().remove( false );
 		}
 		else
 		{
 			interestingTiles = tiles;
 			
 			/**
 			 * virtually interconnect disconnected intersecting graphs
 			 * 
 			 * TODO Not yet tested---Do we need these virtual connections?
 			 */
 			
 //			if ( graphs.size() > 1 && tilesAreInPlace )
 //			{
 //				for ( AbstractAffineTile2D< ? >[] tilePair : tilePairs )
 //					for ( Set< Tile< ? > > graph : graphs )
 //						if ( graph.contains( tilePair[ 0 ] ) && !graph.contains( tilePair[ 1 ] ) )
 //							tilePair[ 0 ].makeVirtualConnection( tilePair[ 1 ] );
 //			}
 		}
 			
 		if ( Thread.currentThread().isInterrupted() ) return;
 		
 		Align.optimizeTileConfiguration( p, interestingTiles, fixedTiles );
 		
 		for ( AbstractAffineTile2D< ? > t : interestingTiles )
 			t.getPatch().setAffineTransform( t.getModel().createAffine() );
 		
 		Utils.log( "Montage done." );
 	}
 	
 	
 	final static public Bureaucrat alignMultiLayerMosaicTask( final Layer l )
 	{
 		Worker worker = new Worker( "Aligning multi-layer mosaic", false, true )
 		{
 			public void run()
 			{
 				startedWorking();
 				try { alignMultiLayerMosaic( l ); }
 				catch ( Throwable e ) { IJError.print( e ); }
 				finally { finishedWorking(); }
 			}
 		};
 		return Bureaucrat.createAndStart(worker, l.getProject());
 	}
 	
 	
 	/**
 	 * Align a multi-layer mosaic.
 	 * 
 	 * @param l the current layer
 	 */
 	final public static void alignMultiLayerMosaic( final Layer l )
 	{
 		/* layer range and misc */
 		
 		final List< Layer > layers = l.getParent().getLayers();
 		final String[] layerTitles = new String[ layers.size() ];
 		for ( int i = 0; i < layers.size(); ++i )
 			layerTitles[ i ] = l.getProject().findLayerThing(layers.get( i )).toString();
 		
 		final GenericDialog gd1 = new GenericDialog( "Align Multi-Layer Mosaic : Layer Range" );
 		
 		gd1.addMessage( "Layer Range:" );
 		final int sel = l.getParent().indexOf(l);
 		gd1.addChoice( "first :", layerTitles, layerTitles[ sel ] );
 		gd1.addChoice( "last :", layerTitles, layerTitles[ sel ] );
 		
 		gd1.addMessage( "Miscellaneous:" );
 		gd1.addCheckbox( "tiles are rougly in place", tilesAreInPlace );
 		gd1.addCheckbox( "consider largest graph only", largestGraphOnly );
 		gd1.addCheckbox( "hide tiles from non-largest graph", hideDisconnectedTiles );
 		gd1.addCheckbox( "delete tiles from non-largest graph", deleteDisconnectedTiles );
 		gd1.addCheckbox( "deform layers", deform );
 		
 		gd1.showDialog();
 		if ( gd1.wasCanceled() ) return;
 		
 		final int first = gd1.getNextChoiceIndex();
 		final int last = gd1.getNextChoiceIndex();
 		final int d = first < last ? 1 : -1;
 		
 		tilesAreInPlace = gd1.getNextBoolean();
 		largestGraphOnly = gd1.getNextBoolean();
 		hideDisconnectedTiles = gd1.getNextBoolean();
 		deleteDisconnectedTiles = gd1.getNextBoolean();
 		deform = gd1.getNextBoolean();
 		
 		/* intra-layer parameters */
 		
 		final GenericDialog gd2 = new GenericDialog( "Align Multi-Layer Mosaic : Intra-Layer" );
 
 		Align.paramOptimize.addFields( gd2 );
 		
 		gd2.showDialog();
 		if ( gd2.wasCanceled() ) return;
 		
 		Align.paramOptimize.readFields( gd2 );
 		
 		
 		/* cross-layer parameters */
 		
 		final GenericDialog gd3 = new GenericDialog( "Align Multi-Layer Mosaic : Cross-Layer" );
 
 		Align.param.addFields( gd3 );
 		
 		gd3.showDialog();
 		if ( gd3.wasCanceled() ) return;
 		
 		Align.param.readFields( gd3 );
 		
 		Align.ParamOptimize p = Align.paramOptimize.clone();
 		Align.Param cp = Align.param.clone();
 		Align.ParamOptimize pcp = p.clone();
 		pcp.desiredModelIndex = cp.desiredModelIndex;
 
 		final List< Layer > layerRange = new ArrayList< Layer >();
 		for ( int i = first; i != last + d; i += d )
 			layerRange.add( layers.get( i ) );
 
 		alignMultiLayerMosaicTask( layerRange, cp, p, pcp, tilesAreInPlace, largestGraphOnly, hideDisconnectedTiles, deleteDisconnectedTiles, deform );
 	}
 	
 	final static private boolean alignGraphs(
 			final Align.Param p,
 			final Layer layer1,
 			final Layer layer2,
 			final Set< Tile< ? > > graph1,
 			final Set< Tile< ? > > graph2 )
 	{
 		final Align.Param cp = p.clone();
 		
 		final Selection selection1 = new Selection( null );
 		for ( final Tile< ? > tile : graph1 )
 			selection1.add( ( ( AbstractAffineTile2D< ? > )tile ).getPatch() );
 		final Rectangle graph1Box = selection1.getBox();
 		
 		final Selection selection2 = new Selection( null );
 		for ( final Tile< ? > tile : graph2 )
 			selection2.add( ( ( AbstractAffineTile2D< ? > )tile ).getPatch() );
 		final Rectangle graph2Box = selection2.getBox();
 		
 		final int maxLength = Math.max( Math.max( Math.max( graph1Box.width, graph1Box.height ), graph2Box.width ), graph2Box.height );
 		//final float scale = ( float )cp.sift.maxOctaveSize / maxLength;
 		/* rather ad hoc but we cannot just scale this to maxOctaveSize */
 		cp.sift.maxOctaveSize = Math.min( maxLength, 2 * p.sift.maxOctaveSize );
 		/* make sure that, despite rounding issues from scale, it is >= image size */
 		final float scale = ( float )( cp.sift.maxOctaveSize - 1 ) / maxLength;
 		
 		//cp.maxEpsilon *= scale;
 		
 		final FloatArray2DSIFT sift = new FloatArray2DSIFT( cp.sift );
 		final SIFT ijSIFT = new SIFT( sift );
 		final ArrayList< Feature > features1 = new ArrayList< Feature >();
 		final ArrayList< Feature > features2 = new ArrayList< Feature >();
 		final ArrayList< PointMatch > candidates = new ArrayList< PointMatch >();
 		final ArrayList< PointMatch > inliers = new ArrayList< PointMatch >();
 		
 		long s = System.currentTimeMillis();
 		
 		ijSIFT.extractFeatures(
 				layer1.getProject().getLoader().getFlatImage( layer1, graph1Box, scale, 0xffffffff, ImagePlus.GRAY8, Patch.class, selection1.getSelected( Patch.class ), false, Color.GRAY ).getProcessor(),
 				features1 );
 		Utils.log( features1.size() + " features extracted for graphs in layer \"" + layer1.getTitle() + "\" (took " + ( System.currentTimeMillis() - s ) + " ms)." );
 		
 		ijSIFT.extractFeatures(
 				layer2.getProject().getLoader().getFlatImage( layer2, graph2Box, scale, 0xffffffff, ImagePlus.GRAY8, Patch.class, selection2.getSelected( Patch.class ), false, Color.GRAY ).getProcessor(),
 				features2 );
 		Utils.log( features2.size() + " features extracted for graphs in layer \"" + layer1.getTitle() + "\" (took " + ( System.currentTimeMillis() - s ) + " ms)." );
 		
 		boolean modelFound = false;
 		if ( features1.size() > 0 && features2.size() > 0 )
 		{
 			s = System.currentTimeMillis();
 			
 			FeatureTransform.matchFeatures(
 				features1,
 				features2,
 				candidates,
 				cp.rod );
 
 			final AbstractAffineModel2D< ? > model;
 			switch ( cp.expectedModelIndex )
 			{
 			case 0:
 				model = new TranslationModel2D();
 				break;
 			case 1:
 				model = new RigidModel2D();
 				break;
 			case 2:
 				model = new SimilarityModel2D();
 				break;
 			case 3:
 				model = new AffineModel2D();
 				break;
 			default:
 				return false;
 			}
 
 			boolean again = false;
 			try
 			{
 				do
 				{
 					again = false;
 					modelFound = model.filterRansac(
 								candidates,
 								inliers,
 								1000,
 								cp.maxEpsilon,
 								cp.minInlierRatio,
 								cp.minNumInliers,
 								3 );
 					if ( modelFound && cp.rejectIdentity )
 					{
 						final ArrayList< Point > points = new ArrayList< Point >();
 						PointMatch.sourcePoints( inliers, points );
 						if ( Transforms.isIdentity( model, points, cp.identityTolerance ) )
 						{
 							IJ.log( "Identity transform for " + inliers.size() + " matches rejected." );
 							candidates.removeAll( inliers );
 							inliers.clear();
 							again = true;
 						}
 					}
 				}
 				while ( again );
 			}
 			catch ( NotEnoughDataPointsException e )
 			{
 				modelFound = false;
 			}
 			
 			if ( modelFound )
 			{
 				Utils.log( "Model found for graphs in layer \"" + layer1.getTitle() + "\" and \"" + layer2.getTitle() + "\":\n  correspondences  " + inliers.size() + " of " + candidates.size() + "\n  average residual error  " + ( model.getCost() / scale ) + " px\n  took " + ( System.currentTimeMillis() - s ) + " ms" );
 				final AffineTransform b = new AffineTransform();
 				b.translate( graph2Box.x, graph2Box.y );
 				b.scale( 1.0f / scale, 1.0f / scale );
 				b.concatenate( model.createAffine() );
 				b.scale( scale, scale );
 				b.translate( -graph1Box.x, -graph1Box.y);
 				
 				for ( Displayable d : selection1.getSelected( Patch.class ) )
 					d.preTransform( b, false );
 				Display.repaint( layer1 );
 			}
 			else
 				IJ.log( "No model found for graphs in layer \"" + layer1.getTitle() + "\" and \"" + layer2.getTitle() + "\":\n  correspondence candidates  " + candidates.size() + "\n  took " + ( System.currentTimeMillis() - s ) + " ms" );
 		}
 		
 		return modelFound;
 	}
 
 
 	public static final void alignMultiLayerMosaicTask(
 			final List< Layer > layerRange,
 			final Align.Param cp,
 			final Align.ParamOptimize p,
 			final Align.ParamOptimize pcp,
 			final boolean tilesAreInPlace,
 			final boolean largestGraphOnly,
 			final boolean hideDisconnectedTiles,
 			final boolean deleteDisconnectedTiles,
 			final boolean deform )
 	{
 
 		/* register */
 		
 		final List< AbstractAffineTile2D< ? > > allTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 		final List< AbstractAffineTile2D< ? > > allFixedTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 		final List< AbstractAffineTile2D< ? > > previousLayerTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 		final HashMap< Patch, PointMatch > tileCenterPoints = new HashMap< Patch, PointMatch >();
 		
 		List< Patch > fixedPatches = new ArrayList< Patch >();
 		final Displayable active = Display.getFront().getActive();
 		if ( active != null && active instanceof Patch )
 			fixedPatches.add( ( Patch )active );
 		
 		Layer previousLayer = null;
 		
 		for ( final Layer layer : layerRange )
 		{
 			/* align all tiles in the layer */
 			
 			final List< Patch > patches = new ArrayList< Patch >();
 			for ( Displayable a : layer.getDisplayables( Patch.class ) )
 				if ( a instanceof Patch ) patches.add( ( Patch )a );
 			final List< AbstractAffineTile2D< ? > > currentLayerTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 			final List< AbstractAffineTile2D< ? > > fixedTiles = new ArrayList< AbstractAffineTile2D< ? > > ();
 			Align.tilesFromPatches( p, patches, fixedPatches, currentLayerTiles, fixedTiles );
 			
 			alignTiles( p, currentLayerTiles, fixedTiles, tilesAreInPlace, false, false, false ); // Will consider graphs and hide/delete tiles when all cross-layer graphs are found.
 			if (Thread.currentThread().isInterrupted()) return;
 			
 			/* connect to the previous layer */
 			
 			
 			/* generate tiles with the cross-section model from the current layer tiles */
 			/* ------------------------------------------------------------------------ */
 			/* TODO step back and make tiles bare containers for a patch and a model such that by changing the model the tile can be reused */
 			final HashMap< Patch, AbstractAffineTile2D< ? > > currentLayerPatchTiles = new HashMap< Patch, AbstractAffineTile2D<?> >();
 			for ( final AbstractAffineTile2D< ? > t : currentLayerTiles )
 				currentLayerPatchTiles.put( t.getPatch(), t );
 			
 			final List< AbstractAffineTile2D< ? > > csCurrentLayerTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 			final List< AbstractAffineTile2D< ? > > csFixedTiles = new ArrayList< AbstractAffineTile2D< ? > > ();
 			Align.tilesFromPatches( cp, patches, fixedPatches, csCurrentLayerTiles, csFixedTiles );
 			
 			final HashMap< Tile< ? >, AbstractAffineTile2D< ? > > tileTiles = new HashMap< Tile< ? >, AbstractAffineTile2D<?> >();
 			for ( final AbstractAffineTile2D< ? > t : csCurrentLayerTiles )
 				tileTiles.put( currentLayerPatchTiles.get( t.getPatch() ), t );
 			
 			for ( final AbstractAffineTile2D< ? > t : currentLayerTiles )
 			{
 				final AbstractAffineTile2D< ? > csLayerTile = tileTiles.get( t );
 				csLayerTile.addMatches( t.getMatches() );
 				for ( Tile< ? > ct : t.getConnectedTiles() )
 					csLayerTile.addConnectedTile( tileTiles.get( ct ) );
 			}
 			
 			/* add a fixed tile only if there was a Patch selected */
 			allFixedTiles.addAll( csFixedTiles );
 			
 			/* first, align connected graphs to each other */
 			
 			/* graphs in the current layer */
 			final List< Set< Tile< ? > > > currentLayerGraphs = AbstractAffineTile2D.identifyConnectedGraphs( csCurrentLayerTiles );
 			if (Thread.currentThread().isInterrupted()) return;
 			
 //			/* TODO just for visualization */
 //			for ( final Set< Tile< ? > > graph : currentLayerGraphs )
 //			{
 //				Display.getFront().getSelection().clear();
 //				Display.getFront().setLayer( ( ( AbstractAffineTile2D< ? > )graph.iterator().next() ).getPatch().getLayer() );
 //				
 //				for ( final Tile< ? > tile : graph )
 //				{
 //					Display.getFront().getSelection().add( ( ( AbstractAffineTile2D< ? > )tile ).getPatch() );
 //					Display.repaint();
 //				}
 //				Utils.showMessage( "OK" );
 //			}
 			
 			/* graphs from the whole system that are present in the previous layer */
 			final List< Set< Tile< ? > > > graphs = AbstractAffineTile2D.identifyConnectedGraphs( allTiles );
 			final HashMap< Set< Tile< ? > >, Set< Tile< ? > > > graphGraphs = new HashMap< Set<Tile<?>>, Set<Tile<?>> >();
 			for ( final Set< Tile< ? > > graph : graphs )
 			{
 				if (Thread.currentThread().isInterrupted()) return;
 				final Set< Tile< ?  > > previousLayerGraph = new HashSet< Tile< ? > >();
 				for ( final Tile< ? > tile : previousLayerTiles )
 				{
 					if ( graph.contains( tile ) )
 					{
 						graphGraphs.put( graph, previousLayerGraph );
 						previousLayerGraph.add( tile );
 					}
 				}
 			}
 			final Collection< Set< Tile< ? > > > previousLayerGraphs = graphGraphs.values();
 			
 //			/* TODO just for visualization */
 //			for ( final Set< Tile< ? > > graph : previousLayerGraphs )
 //			{
 //				Display.getFront().getSelection().clear();
 //				Display.getFront().setLayer( ( ( AbstractAffineTile2D< ? > )graph.iterator().next() ).getPatch().getLayer() );
 //				
 //				for ( final Tile< ? > tile : graph )
 //				{
 //					Display.getFront().getSelection().add( ( ( AbstractAffineTile2D< ? > )tile ).getPatch() );
 //					Display.repaint();
 //				}
 //				Utils.showMessage( "OK" );
 //			}
 			
 			/* generate snapshots of the graphs and preregister them using the parameters defined in cp */
 			final List< AbstractAffineTile2D< ? >[] > crossLayerTilePairs = new ArrayList< AbstractAffineTile2D< ? >[] >();
 			for ( final Set< Tile< ? > > currentLayerGraph : currentLayerGraphs )
 			{
 				for ( final Set< Tile< ? > > previousLayerGraph : previousLayerGraphs )
 				{
 					if (Thread.currentThread().isInterrupted()) return;
 					alignGraphs( cp, layer, previousLayer, currentLayerGraph, previousLayerGraph );
 					
 					/* TODO this is pointless data shuffling just for type incompatibility---fix this at the root */
 					final ArrayList< AbstractAffineTile2D< ? > > previousLayerGraphTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 					previousLayerGraphTiles.addAll( ( Set )previousLayerGraph );
 					
 					final ArrayList< AbstractAffineTile2D< ? > > currentLayerGraphTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 					currentLayerGraphTiles.addAll( ( Set )currentLayerGraph );
 					
 					AbstractAffineTile2D.pairOverlappingTiles( previousLayerGraphTiles, currentLayerGraphTiles, crossLayerTilePairs );
 				}
 			}
 			
 			
 			/* ------------------------------------------------------------------------ */
 			
 			
 			/* this is without the affine/rigid approximation per graph */
 			//AbstractAffineTile2D.pairTiles( previousLayerTiles, csCurrentLayerTiles, crossLayerTilePairs );
 			
 			Align.connectTilePairs( cp, csCurrentLayerTiles, crossLayerTilePairs, Runtime.getRuntime().availableProcessors() );
 			if (Thread.currentThread().isInterrupted()) return;
 			
 //			for ( final AbstractAffineTile2D< ? >[] tilePair : crossLayerTilePairs )
 //			{
 //				Display.getFront().setLayer( tilePair[ 0 ].getPatch().getLayer() );
 //				Display.getFront().getSelection().clear();
 //				Display.getFront().getSelection().add( tilePair[ 0 ].getPatch() );
 //				Display.getFront().getSelection().add( tilePair[ 1 ].getPatch() );
 //				
 //				Utils.showMessage( "1: OK?" );
 //				
 //				Display.getFront().setLayer( tilePair[ 1 ].getPatch().getLayer() );
 //				Display.getFront().getSelection().clear();
 //				Display.getFront().getSelection().add( tilePair[ 0 ].getPatch() );
 //				Display.getFront().getSelection().add( tilePair[ 1 ].getPatch() );
 //				
 //				Utils.showMessage( "2: OK?" );
 //			}
 			
 			/* prepare the next loop */
 			
 			allTiles.addAll( csCurrentLayerTiles );
 			previousLayerTiles.clear();
 			previousLayerTiles.addAll( csCurrentLayerTiles );
 			
 			/* optimize */
 			Align.optimizeTileConfiguration( pcp, allTiles, allFixedTiles );
 			if (Thread.currentThread().isInterrupted()) return;
 			
 			for ( AbstractAffineTile2D< ? > t : allTiles )
 				t.getPatch().setAffineTransform( t.getModel().createAffine() );
 			
 			previousLayer = layer;
 		}
 		
 		List< Set< Tile< ? > > > graphs = AbstractAffineTile2D.identifyConnectedGraphs( allTiles );
 		
 		final List< AbstractAffineTile2D< ? > > interestingTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 		
 		if ( largestGraphOnly && ( hideDisconnectedTiles || deleteDisconnectedTiles ) )
 		{
 			if ( Thread.currentThread().isInterrupted() ) return;
 			
 			/* find largest graph. */
 			
 			Set< Tile< ? > > largestGraph = null;
 			for ( Set< Tile< ? > > graph : graphs )
 				if ( largestGraph == null || largestGraph.size() < graph.size() )
 					largestGraph = graph;
 			
 			final Set<AbstractAffineTile2D<?>> tiles_to_keep = new HashSet<AbstractAffineTile2D<?>>();
 			
 			for ( Tile< ? > t : largestGraph )
 				tiles_to_keep.add( ( AbstractAffineTile2D< ? > )t );
 			
 			if ( hideDisconnectedTiles )
 				for ( AbstractAffineTile2D< ? > t : allTiles )
 					if ( !tiles_to_keep.contains( t ) )
 						t.getPatch().setVisible( false );
 			if ( deleteDisconnectedTiles )
 				for ( AbstractAffineTile2D< ? > t : allTiles )
 					if ( !tiles_to_keep.contains( t ) )
 						t.getPatch().remove( false );
 			
 			interestingTiles.addAll(tiles_to_keep);
 		}
 		else
 			interestingTiles.addAll( allTiles );
 
 
 		if ( deform )
 		{
 			/* ############################################ */
 			/* experimental: use the center points of all tiles to define a MLS deformation from the pure intra-layer registration to the globally optimal */
 			
 			Utils.log( "deforming..." );
 			
 			/* store the center location of each single tile for later deformation */
 			for ( final AbstractAffineTile2D< ? > t : interestingTiles )
 			{
 				final float[] c = new float[]{ ( float )t.getWidth() / 2.0f,( float )t.getHeight() / 2.0f };
 				t.getModel().applyInPlace( c );
 				final Point q = new Point( c );
 				tileCenterPoints.put( t.getPatch(), new PointMatch( q.clone(), q ) );
 			}
 			
 			for ( final Layer layer : layerRange )
 			{
 				Utils.log( "layer" + layer );
 				
 				if ( Thread.currentThread().isInterrupted() ) return;
 
 				/* again, align all tiles in the layer */
 				
 				List< Patch > patches = new ArrayList< Patch >();
 				for ( Displayable a : layer.getDisplayables( Patch.class ) )
 					if ( a instanceof Patch ) patches.add( ( Patch )a );
 				final List< AbstractAffineTile2D< ? > > currentLayerTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 				final List< AbstractAffineTile2D< ? > > fixedTiles = new ArrayList< AbstractAffineTile2D< ? > > ();
 				Align.tilesFromPatches( p, patches, fixedPatches, currentLayerTiles, fixedTiles );
 							
 				/* add a fixed tile only if there was a Patch selected */
 				allFixedTiles.addAll( fixedTiles );
 				
 				alignTiles( p, currentLayerTiles, fixedTiles, true, false, false, false ); // will consider graphs and hide/delete tiles when all cross-layer graphs are found
 				
 				/* for each independent graph do an independent transform */
 				final List< Set< Tile< ? > > > currentLayerGraphs = AbstractAffineTile2D.identifyConnectedGraphs( currentLayerTiles );
 				for ( final Set< Tile< ? > > graph : currentLayerGraphs )
 				{
 				
 					/* update the tile-center pointmatches */
 					final Collection< PointMatch > matches = new ArrayList< PointMatch >();
 					final Collection< AbstractAffineTile2D< ? > > toBeDeformedTiles = new ArrayList< AbstractAffineTile2D< ? > >();
 					for ( final AbstractAffineTile2D< ? > t : ( Collection< AbstractAffineTile2D< ? > > )( Collection )graph )
 					{
 						final PointMatch pm = tileCenterPoints.get( t.getPatch() );
 						if ( pm == null ) continue;
 						
 						final float[] pl = pm.getP1().getL();
 						pl[ 0 ] = ( float )t.getWidth() / 2.0f;
 						pl[ 1 ] = ( float )t.getHeight() / 2.0f;
 						t.getModel().applyInPlace( pl );
 						matches.add( pm );
 						toBeDeformedTiles.add( t );
 					}
 					
 					for ( final AbstractAffineTile2D< ? > t : toBeDeformedTiles )
 					{
 						if ( Thread.currentThread().isInterrupted() ) return;
 						
 						try
 						{
 							final Patch patch = t.getPatch();
 							final Rectangle pbox = patch.getCoordinateTransformBoundingBox();
 							final AffineTransform pat = new AffineTransform();
 							pat.translate( -pbox.x, -pbox.y );
 							pat.preConcatenate( patch.getAffineTransform() );
 							
 							final mpicbg.trakem2.transform.AffineModel2D toWorld = new mpicbg.trakem2.transform.AffineModel2D();
 							toWorld.set( pat );
 							
 							final MovingLeastSquaresTransform mlst = Align.createMLST( matches, 1.0f );
 							
 							final CoordinateTransformList< CoordinateTransform > ctl = new CoordinateTransformList< CoordinateTransform >();
 							ctl.add( toWorld );
 							ctl.add( mlst );
 							ctl.add( toWorld.createInverse() );
 							
 							patch.appendCoordinateTransform( ctl );
 							
 							patch.getProject().getLoader().regenerateMipMaps( patch );
 						}
 						catch ( Exception e )
 						{
 							e.printStackTrace();
 						}
 					}
 				}
 			}
 		}
 		
 		layerRange.get(0).getParent().setMinimumDimensions();
 		IJ.log( "Done: register multi-layer mosaic." );
 		
 		return;
 	}
 
 
 	/** The ParamOptimize object containg all feature extraction and registration model parameters for the "snap" function. */
 	static public final Align.ParamOptimize p_snap = Align.paramOptimize.clone();
 
 	/** Find the most overlapping image to @param patch in the same layer where @param patch sits, and snap @param patch and all its linked Displayable objects.
 	 *  If a null @param p_snap is given, it will use the AlignTask.p_snap.
 	 *  If @param setup is true, it will show a dialog to adjust parameters. */
 	static public final Bureaucrat snap(final Patch patch, final Align.ParamOptimize p_snap, final boolean setup) {
 		return Bureaucrat.createAndStart(new Worker.Task("Snapping", true) {
 			public void exec() {
 
 		final Align.ParamOptimize p = null == p_snap ? AlignTask.p_snap : p_snap;
 		if (setup) p.setup("Snap");
 
 		// Collect Patch linked to active
 		final List<Displayable> linked_images = new ArrayList<Displayable>();
 		for (final Displayable d : patch.getLinkedGroup(null)) {
 			if (d.getClass() == Patch.class && d != patch) linked_images.add(d);
 		}
 		// Find overlapping images
 		final List<Patch> overlapping = new ArrayList<Patch>( (Collection<Patch>) (Collection) patch.getLayer().getIntersecting(patch, Patch.class));
 		overlapping.remove(patch);
 		if (0 == overlapping.size()) return; // nothing overlaps
 
 		// Discard from overlapping any linked images
 		overlapping.removeAll(linked_images);
 
 		if (0 == overlapping.size()) {
 			Utils.log("Cannot snap: overlapping images are linked to the one to snap.");
 			return;
 		}
 
 		// flush
 		linked_images.clear();
 
 		// Find the image that overlaps the most
 		Rectangle box = patch.getBoundingBox(null);
 		Patch most = null;
 		Rectangle most_inter = null;
 		for (final Patch other : overlapping) {
 			if (null == most) {
 				most = other;
 				most_inter = other.getBoundingBox();
 				continue;
 			}
 			Rectangle inter = other.getBoundingBox().intersection(box);
 			if (inter.width * inter.height > most_inter.width * most_inter.height) {
 				most = other;
 				most_inter = inter;
 			}
 		}
 		// flush
 		overlapping.clear();
 
 		// Define two lists:
 		//  - a list with all involved tiles: the active and the most overlapping one
 		final List<Patch> patches = new ArrayList<Patch>();
 		patches.add(most);
 		patches.add(patch);
 		//  - a list with all tiles except the active, to be set as fixed, immobile
 		final List<Patch> fixedPatches = new ArrayList<Patch>();
 		fixedPatches.add(most);
 
 		// Patch as Tile
 		List< AbstractAffineTile2D< ? > > tiles = new ArrayList< AbstractAffineTile2D< ? > >();
 		List< AbstractAffineTile2D< ? > > fixedTiles = new ArrayList< AbstractAffineTile2D< ? > > ();
 		Align.tilesFromPatches( p, patches, fixedPatches, tiles, fixedTiles );
 
 		// Pair and connect overlapping tiles
 		final List< AbstractAffineTile2D< ? >[] > tilePairs = new ArrayList< AbstractAffineTile2D< ? >[] >();
 		AbstractAffineTile2D.pairOverlappingTiles( tiles, tilePairs );
 		Align.connectTilePairs( p, tiles, tilePairs, Runtime.getRuntime().availableProcessors() );
 
 		if ( Thread.currentThread().isInterrupted() ) return;
 
 		Align.optimizeTileConfiguration( p, tiles, fixedTiles );
 
 		for ( AbstractAffineTile2D< ? > t : tiles ) {
 			if (t.getPatch() == patch) {
 				AffineTransform at = t.getModel().createAffine();
 				try {
 					at.concatenate(patch.getAffineTransform().createInverse());
 					patch.transform(at);
 				} catch (NoninvertibleTransformException nite) {
 					IJError.print(nite);
 				}
 				break;
 			}
 		}
 
 		Display.repaint();
 
 		}}, patch.getProject());
 	}
 
 	static public final Bureaucrat registerStackSlices(final Patch slice) {
 		return Bureaucrat.createAndStart(new Worker.Task("Registering slices", true) {
 			public void exec() {
 
 		// build the list
 		ArrayList<Patch> slices = slice.getStackPatches();
 		if (slices.size() < 2) {
 			Utils.log2("Not a stack!");
 			return;
 		}
 
 		// check that none are linked to anything other than images
 		for (final Patch patch : slices) {
 			if (!patch.isOnlyLinkedTo(Patch.class)) {
 				Utils.log("Can't register: one or more slices are linked to objects other than images.");
 				return;
 			}
 		}
 
 		// ok proceed
 		final Align.ParamOptimize p = Align.paramOptimize.clone();
 		p.setup("Register stack slices");
 
 		List<Patch> fixedSlices = new ArrayList<Patch>();
 		fixedSlices.add(slice);
 
 		alignPatches( p, slices, fixedSlices, false, false, false, false );
 
 		Display.repaint();
 
 		}}, slice.getProject());
 	}
 }
