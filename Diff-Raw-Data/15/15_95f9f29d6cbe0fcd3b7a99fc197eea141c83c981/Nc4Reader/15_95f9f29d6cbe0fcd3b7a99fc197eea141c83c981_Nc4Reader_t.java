 package org.esa.beam.dataio.netcdf4;
 
 import com.bc.ceres.core.ProgressMonitor;
 import org.esa.beam.dataio.netcdf4.convention.AbstractModelFactory;
 import org.esa.beam.dataio.netcdf4.convention.Model;
 import org.esa.beam.dataio.netcdf4.convention.ModelFactoryRegistry;
 import org.esa.beam.framework.dataio.AbstractProductReader;
 import org.esa.beam.framework.dataio.IllegalFileFormatException;
 import org.esa.beam.framework.dataio.ProductReaderPlugIn;
 import org.esa.beam.framework.datamodel.Band;
 import org.esa.beam.framework.datamodel.Product;
 import org.esa.beam.framework.datamodel.ProductData;
 import org.esa.beam.util.Guardian;
 import ucar.ma2.Array;
 import ucar.ma2.InvalidRangeException;
 import ucar.nc2.NetcdfFile;
 import ucar.nc2.Variable;
 
 import java.io.File;
 import java.io.IOException;
 
 /**
  * A product reader for NetCDF files.
  * <p/>
  * This reader tries to support the
  * <a href="http://ferret.wrc.noaa.gov/noaa_coop/coop_cdf_profile.html">COARDS</a> profile and
  * <a href="http://www.cgd.ucar.edu/cms/eaton/cf-metadata/CF-1.0.html">CF Conventions</a> to a maximum extend.
  * <p/>
  * The CF Conventions are supported for regular, lat/lon grids as follows.
  * If the dimensions are
  * <pre>
  *    lon = <i>integer</i>
  *    lat = <i>integer</i>
  *    time = <i>integer</i> (currently assumed to be 1)
  * </pre>
  * then the following variables are expected
  * <pre>
  *    lon(lon)
  *    lat(lat)
  *    time(time)
  *    <i>band-1</i>(time, lat, lon)
  *    <i>band-2</i>(time, lat, lon)
  *    ...
  * </pre>
  * <p/>
  * The CF Conventions are supported for non-regular, lat/lon grids as follows:
  * If the dimensions are
  * <pre>
  *    ni = <i>integer</i>
  *    nj = <i>integer</i>
  *    time = <i>integer</i> (currently assumed to be 1)
  *    ...
  * </pre>
  * then the following variables are expected
  * <pre>
  *    lat(nj, ni)
  *    lon(nj, ni)
  *    time(time)
  *    <i>band-1</i>(time, nj, ni)
  *    <i>band-2</i>(time, nj, ni)
  *    ...
  * </pre>
  * <p/>
  * The COARDS profile is supported as follows:
  * If the dimensions are
  * <pre>
  *    longitude = <i>integer</i>
  *    latitude = <i>integer</i>
  *    ...
  * </pre>
  * then the following variables are expected
  * <pre>
  *    longitude(longitude)
  *    latitude(latitude)
  *    <i>band-1</i>(latitude, longitude)
  *    <i>band-2</i>(latitude, longitude)
  *    ...
  * </pre>
  *
  * @author Norman Fomferra
  */
 public class Nc4Reader extends AbstractProductReader {
 
     private Nc4ReaderParameters rp;
 
     public Nc4Reader(final ProductReaderPlugIn readerPlugIn) {
         super(readerPlugIn);
     }
 
     /**
      * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
      * method can be sure that the input object and eventually the subset information has already been set.
      * <p/>
      * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
      *
      * @throws java.io.IOException if an I/O error occurs
      */
     @Override
     protected Product readProductNodesImpl() throws
                                              IOException {
 
         final File fileLocation = new File(getInput().toString());
         NetcdfFile netcdfFile = NetcdfFile.open(fileLocation.getPath());
 
         System.out.println("netcdfFile = " + netcdfFile);
 
         AbstractModelFactory modelFactory = ModelFactoryRegistry.getInstance().getModelFactory(netcdfFile);
         if (modelFactory == null) {
             netcdfFile.close();
             throw new IllegalFileFormatException("No convention factory found for netCDF.");
         }
         final Model convention = modelFactory.createModel(netcdfFile);
         rp = convention.getReaderParameters();
 
         if (rp.getRasterDigest() == null) {
             close();
             throw new IllegalFileFormatException("No netCDF variables found which could\n" +
                                                  "be interpreted as remote sensing bands.");  /*I18N*/
         }
 
        final String productName = fileLocation.getName();
         final Product product = convention.readProduct(productName, rp);
         product.setFileLocation(fileLocation);
         product.setProductReader(this);
         product.setModified(false);
         return product;
     }
 
     @Override
     protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                           int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                           int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                           ProgressMonitor pm) throws IOException {
         Guardian.assertTrue("sourceStepX == 1 && sourceStepY == 1", sourceStepX == 1 && sourceStepY == 1);
         Guardian.assertTrue("sourceWidth == destWidth", sourceWidth == destWidth);
         Guardian.assertTrue("sourceHeight == destHeight", sourceHeight == destHeight);
 
         final int sceneHeight = destBand.getProduct().getSceneRasterHeight();
         final int y0 = rp.isYFlipped() ? (sceneHeight - 1) - sourceOffsetY : sourceOffsetY;
 
         final Variable variable = rp.getRasterVariableMap().get(destBand.getName());
         final int rank = variable.getRank();
         final int[] origin = new int[rank];
         final int[] shape = new int[rank];
         for (int i = 0; i < rank; i++) {
             shape[i] = 1;
             origin[i] = 0;
         }
         shape[rank - 2] = 1;
         shape[rank - 1] = destWidth;
         origin[rank - 1] = sourceOffsetX;
 
         pm.beginTask("Reading data from band '" + destBand.getName() + "'", destHeight);
         try {
             for (int y = 0; y < destHeight; y++) {
                 origin[rank - 2] = rp.isYFlipped() ? y0 - y : y0 + y;
                 final Array array;
                 synchronized (rp.getNetcdfFile()) {
                     array = variable.read(origin, shape);
                 }
                 final Object storage = array.getStorage();
                 //noinspection SuspiciousSystemArraycopy
                 System.arraycopy(storage, 0, destBuffer.getElems(), y * destWidth, destWidth);
                 pm.worked(1);
                 if (pm.isCanceled()) {
                     throw new IOException("Process terminated by user."); /*I18N*/
                 }
             }
         } catch (InvalidRangeException e) {
             final IOException ioException = new IOException(e.getMessage());
             ioException.initCause(e);
             throw ioException;
         } finally {
             pm.done();
         }
     }
 
     /**
      * Closes the access to all currently opened resources such as file input streams and all resources of this children
      * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
      * <p/>
      * <p>This method should be called only if it is for sure that this object instance will never be used again. The
      * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
      * <p/>
      * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
      *
      * @throws java.io.IOException if an I/O error occurs
      */
     @Override
     public void close() throws
                         IOException {
         rp.close();
         super.close();
     }
 }
