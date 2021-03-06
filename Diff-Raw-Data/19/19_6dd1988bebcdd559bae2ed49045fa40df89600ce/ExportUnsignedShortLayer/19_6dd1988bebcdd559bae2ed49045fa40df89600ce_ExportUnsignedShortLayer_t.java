 package mpicbg.trakem2.transform;
 
 import ij.ImagePlus;
 import ij.ImageStack;
 import ij.process.ByteProcessor;
 import ij.process.ImageProcessor;
 import ij.process.ShortProcessor;
 import ini.trakem2.display.Displayable;
 import ini.trakem2.display.Layer;
 import ini.trakem2.display.Patch;
 
 import java.awt.Rectangle;
 import java.awt.geom.AffineTransform;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.concurrent.Callable;
 
 import mpicbg.models.CoordinateTransform;
 import mpicbg.models.CoordinateTransformList;
 import mpicbg.models.CoordinateTransformMesh;
 import mpicbg.models.TranslationModel2D;
 import mpicbg.trakem2.util.Triple;
 
 public class ExportUnsignedShortLayer
 {
 	static protected class PatchIntensityRange
 	{
 		final public Patch patch;
 		final public double a, min, max;
 		
 		PatchIntensityRange( final Patch patch )
 		{
 			this.patch = patch;
 			a = patch.getMax() - patch.getMin();
 			final ImageProcessor ip = patch.getImageProcessor();
 			ip.resetMinAndMax();
 			min = ( ip.getMin() - patch.getMin() ) / a;
 			max = ( ip.getMax() - patch.getMin() ) / a;
 			ip.setMinAndMax( patch.getMin(), patch.getMax() );
 		}
 	}
 	
 	static protected class PatchTransform
 	{
 		final PatchIntensityRange pir;
 		final CoordinateTransform ct;
 		
 		PatchTransform( final PatchIntensityRange pir )
 		{
 			this.pir = pir;
 			final CoordinateTransform ctp = pir.patch.getCoordinateTransform();
 			if ( ctp == null )
 			{
 				final AffineModel2D affine = new AffineModel2D();
 				affine.set( pir.patch.getAffineTransform() );
 				ct = affine;
 			}
 			else
 			{
 				final Rectangle box = pir.patch.getCoordinateTransformBoundingBox();
 				final AffineTransform at = pir.patch.getAffineTransformCopy();
 				at.translate( -box.x, -box.y );
 				final AffineModel2D affine = new AffineModel2D();
 				affine.set( at );
 				
 				final CoordinateTransformList< CoordinateTransform > ctl = new CoordinateTransformList< CoordinateTransform >();
 				ctl.add( ctp );
 				ctl.add( affine );
 				
 				ct = ctl;
 			}
 		}
 	}
 	
 	final static protected ShortProcessor mapIntensities( final PatchIntensityRange pir, final double min, final double max )
 	{
 		final double a = 65535.0 / ( max - min );
 		final ImageProcessor source = pir.patch.getImageProcessor();
 		final short[] targetPixels = new short[ source.getWidth() * source.getHeight() ];
 		for ( int i = 0; i < targetPixels.length; ++i )
 		{
 			targetPixels[ i ] = ( short )Math.max( 0, Math.min( 65535, Math.round( ( ( source.getf( i ) - pir.patch.getMin() ) / pir.a - min ) * a ) ) );
 		}
 		final ShortProcessor target = new ShortProcessor( source.getWidth(), source.getHeight(), targetPixels, null );
 		target.setMinAndMax( -min * a, ( 1.0 - min ) * a );
 		return target;
 	}
 	
 	final static protected void map( final PatchTransform pt, final double x, final double y, final ShortProcessor mappedIntensities, final ShortProcessor target )
 	{
 		final TranslationModel2D t = new TranslationModel2D();
 		t.set( ( float )-x, ( float )-y );
 		
 		final CoordinateTransformList< CoordinateTransform > ctl = new CoordinateTransformList< CoordinateTransform >();
 		ctl.add( pt.ct );
 		ctl.add( t );
 		
 		final CoordinateTransformMesh mesh = new CoordinateTransformMesh( ctl, pt.pir.patch.getMeshResolution(), pt.pir.patch.getOWidth(), pt.pir.patch.getOHeight() );
 		
 		final TransformMeshMappingWithMasks< CoordinateTransformMesh > mapping = new TransformMeshMappingWithMasks< CoordinateTransformMesh >( mesh );
 		
 		mappedIntensities.setInterpolationMethod( ImageProcessor.BILINEAR );
 		if ( pt.pir.patch.hasAlphaMask() )
 		{
 			final ByteProcessor alpha = pt.pir.patch.getProject().getLoader().fetchImageMask( pt.pir.patch );
 			alpha.setInterpolationMethod( ImageProcessor.BILINEAR );
 			mapping.map( mappedIntensities, alpha, target );
 		}
 		else
 		{
 			mapping.mapInterpolated( mappedIntensities, target );
 		}
 	}
 	
 	final static public void exportTEST( final Layer layer, final int tileWidth, final int tileHeight )
 	{
 		/* calculate intensity transfer */
 		final ArrayList< Displayable > patches = layer.getDisplayables( Patch.class );
 		final ArrayList< PatchIntensityRange > patchIntensityRanges = new ArrayList< PatchIntensityRange >();
 		double min = Double.MAX_VALUE;
 		double max = -Double.MAX_VALUE;
 		for ( final Displayable d : patches )
 		{
 			final Patch patch = ( Patch )d;
 			final PatchIntensityRange pir = new PatchIntensityRange( patch );
 			if ( pir.min < min )
 				min = pir.min;
 			if ( pir.max > max )
 				max = pir.max;
 			patchIntensityRanges.add( pir );
 		}
 		
 		/* render tiles */
 		/* TODO Do not render them into a stack but save them as files */
 		
 		final ImageStack stack = new ImageStack( tileWidth, tileHeight );
 		ImagePlus imp = null;
 		final double minI = -min * 65535.0 / ( max - min );
 		final double maxI = ( 1.0 - min ) * 65535.0 / ( max - min );
		
		//ij.IJ.log("min, max: " + min + ", " + max + ",    minI, maxI: " + minI + ", " + maxI);
		
 		final int nc = ( int )Math.ceil( layer.getLayerWidth() / tileWidth );
 		final int nr = ( int )Math.ceil( layer.getLayerHeight() / tileHeight );
 		for ( int r = 0; r < nr; ++r )
 		{
 			final int y0 = r * tileHeight;
 			for ( int c = 0; c < nc; ++c )
 			{
 				final int x0 = c * tileWidth;
 				final Rectangle box = new Rectangle( x0, y0, tileWidth, tileHeight );
 				final ShortProcessor sp = new ShortProcessor( tileWidth, tileHeight );
 				sp.setMinAndMax( minI, maxI );
 				for ( final PatchIntensityRange pir : patchIntensityRanges )
 				{
 					if ( pir.patch.getBoundingBox().intersects( box ) )
 						map( new PatchTransform( pir ), x0, y0, mapIntensities( pir, min, max ), sp );
 				}
 				stack.addSlice( r + ", " + c , sp );
				if ( null == imp && stack.getSize() > 1 )
 				{
 					imp = new ImagePlus( "tiles", stack );
 					imp.show();
 				}
				if (null != imp) {
					imp.setSlice( stack.getSize() );
					imp.updateAndDraw();
				}
 			}
 		}
		if (null == imp) {
			new ImagePlus( "tiles", stack ).show(); // single-slice, non-StackWindow
		}
 	}
 	
 	/** Create constant size tiles that carpet the areas of the {@param layer} where there are images;
 	 * these tiles are returned in a lazy sequence of {@link Callable} objects that create a tripled
 	 * consisting of the {@link ShortProcessor} and the X and Y pixel coordinates of that tile.
 	 * 
 	 * @param layer The layer to export images for
 	 * @param tileWidth The width of the tiles to export
 	 * @param tileHeight
 	 * @return A lazy sequence of {@link Callable} instances, each holding a {@link Triple} that specifies the ShortProcessor,
 	 * the X and the Y (both in world pixel uncalibrated coordinates).
 	 */
 	final static public Iterable<Callable<ExportedTile>> exportTiles( final Layer layer, final int tileWidth, final int tileHeight, final boolean visible_only )
 	{
 		final ArrayList< Displayable > patches = layer.getDisplayables( Patch.class, visible_only );
 		// If the Layer lacks images, return an empty sequence.
 		if ( patches.isEmpty() )
 		{
 			return Collections.emptyList();
 		}
 
 		/* calculate intensity transfer */
 		final ArrayList< PatchIntensityRange > patchIntensityRanges = new ArrayList< PatchIntensityRange >();
 		double min_ = Double.MAX_VALUE;
 		double max_ = -Double.MAX_VALUE;
 		for ( final Displayable d : patches )
 		{
 			final Patch patch = ( Patch )d;
 			final PatchIntensityRange pir = new PatchIntensityRange( patch );
 			if ( pir.min < min_ )
 				min_ = pir.min;
 			if ( pir.max > max_ )
 				max_ = pir.max;
 			patchIntensityRanges.add( pir );
 		}
 
 		final double min = min_;
 		final double max = max_;
 
 		/* Create lazy sequence that creates Callable instances. */
 		
 		final Rectangle box = layer.getMinimalBoundingBox( Patch.class, visible_only );
 		final int nCols = ( int )Math.ceil( box.width / (double)tileWidth );
 		final int nRows = ( int )Math.ceil( box.height / (double)tileHeight );
 		final double minI = -min * 65535.0 / ( max - min );
 		final double maxI = ( 1.0 - min ) * 65535.0 / ( max - min );
 
		//ij.IJ.log("min, max: " + min + ", " + max + ",    minI, maxI: " + minI + ", " + maxI);
 
 		return new Iterable<Callable<ExportedTile>>()
 		{
 			@Override
 			public Iterator<Callable<ExportedTile>> iterator() {
 				return new Iterator<Callable<ExportedTile>>() {
 					// Internal state
 					private int row = 0,
 					            col = 0,
 					            x0 = box.x,
 					            y0 = box.y;
 					private final ArrayList< PatchIntensityRange > ps = new ArrayList< PatchIntensityRange >();
 					
 					{
 						// Constructor body. Prepare to be able to answer "hasNext()"
 						findNext();
 					}
 
 					private final void findNext() {
 						// Iterate until finding a tile that intersects one or more patches
 						ps.clear();
 						while (true)
 						{
 							if (nRows == row) {
 								// End of domain
 								break;
 							}
 
 							x0 = box.x + col * tileWidth;
 							y0 = box.y + row * tileHeight;
 							final Rectangle tileBounds = new Rectangle( x0, y0, tileWidth, tileHeight );
 
 							for ( final PatchIntensityRange pir : patchIntensityRanges )
 							{
 								if ( pir.patch.getBoundingBox().intersects( tileBounds ) )
 								{
 									ps.add( pir );
 								}
 							}
 							
 							// Prepare next iteration
 							col += 1;
 							if (nCols == col) {
 								col = 0;
 								row += 1;
 							}
 			
 							if ( ps.size() > 0 )
 							{
 								// Ready for next iteration
 								break;
 							}
 						}
 					}
 
 					@Override
 					public boolean hasNext()
 					{
 						return ps.size() > 0;
 					}
 
 					@Override
 					public Callable<ExportedTile> next()
 					{
 						// Capture state locally
 						final ArrayList< PatchIntensityRange > pirs = new ArrayList< PatchIntensityRange >( ps );
 						final int x = x0;
 						final int y = y0;
 						// Advance
 						findNext();
 
 						return new Callable<ExportedTile>()
 						{
 
 							@Override
 							public ExportedTile call()
 									throws Exception {
 								final ShortProcessor sp = new ShortProcessor( tileWidth, tileHeight );
 								sp.setMinAndMax( minI, maxI );
 								
 								for ( final PatchIntensityRange pir : pirs )
 								{
 									map( new PatchTransform( pir ), x, y, mapIntensities( pir, min, max ), sp );
 								}
 								
 								return new ExportedTile( sp, x, y, minI, maxI );
 							}
 						};
 					}
 
 					@Override
 					public void remove()
 					{
 						throw new UnsupportedOperationException();
 					}
 				};
 			}
 		};
 	}
 }
