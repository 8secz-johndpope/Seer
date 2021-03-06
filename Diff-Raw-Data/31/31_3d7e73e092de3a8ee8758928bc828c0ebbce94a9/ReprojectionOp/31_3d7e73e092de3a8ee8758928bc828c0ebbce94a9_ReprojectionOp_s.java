 /*
  * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
  *
  * This program is free software; you can redistribute it and/or modify it
  * under the terms of the GNU General Public License as published by the
  * Free Software Foundation. This program is distributed in the hope it will
  * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
  * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
  */
 package org.esa.beam.gpf.common.reproject;
 
 import com.bc.ceres.glevel.MultiLevelImage;
 import com.bc.ceres.glevel.MultiLevelModel;
 import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
 import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
 import org.esa.beam.framework.datamodel.Band;
 import org.esa.beam.framework.datamodel.CrsGeoCoding;
 import org.esa.beam.framework.datamodel.FlagCoding;
 import org.esa.beam.framework.datamodel.GeoCoding;
 import org.esa.beam.framework.datamodel.GeoPos;
 import org.esa.beam.framework.datamodel.IndexCoding;
 import org.esa.beam.framework.datamodel.Pin;
 import org.esa.beam.framework.datamodel.PlacemarkSymbol;
 import org.esa.beam.framework.datamodel.Product;
 import org.esa.beam.framework.datamodel.ProductData;
 import org.esa.beam.framework.datamodel.ProductNodeGroup;
 import org.esa.beam.framework.datamodel.RasterDataNode;
 import org.esa.beam.framework.dataop.dem.ElevationModel;
 import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
 import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
 import org.esa.beam.framework.dataop.dem.Orthorectifier;
 import org.esa.beam.framework.dataop.dem.Orthorectifier2;
 import org.esa.beam.framework.dataop.resamp.Resampling;
 import org.esa.beam.framework.gpf.Operator;
 import org.esa.beam.framework.gpf.OperatorException;
 import org.esa.beam.framework.gpf.OperatorSpi;
 import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
 import org.esa.beam.framework.gpf.annotations.Parameter;
 import org.esa.beam.framework.gpf.annotations.SourceProduct;
 import org.esa.beam.framework.gpf.annotations.TargetProduct;
 import org.esa.beam.jai.ImageManager;
 import org.esa.beam.jai.ResolutionLevel;
 import org.esa.beam.jai.VirtualBandOpImage;
 import org.esa.beam.util.Debug;
 import org.esa.beam.util.ProductUtils;
 import org.esa.beam.util.io.FileUtils;
 import org.geotools.factory.Hints;
 import org.geotools.referencing.CRS;
 import org.opengis.referencing.FactoryException;
 import org.opengis.referencing.crs.CoordinateReferenceSystem;
 import org.opengis.referencing.cs.AxisDirection;
 import org.opengis.referencing.operation.TransformException;
 
 import javax.media.jai.ImageLayout;
 import javax.media.jai.Interpolation;
 import javax.media.jai.JAI;
 import java.awt.Dimension;
 import java.awt.Rectangle;
 import java.awt.geom.AffineTransform;
 import java.awt.image.RenderedImage;
 import java.io.File;
 import java.io.IOException;
 import java.text.MessageFormat;
 
 /**
  * @author Marco Zuehlke
  * @author Marco Peters
  * @version $Revision$ $Date$
  * @since BEAM 4.7
  */
 @OperatorMetadata(alias = "Reproject",
                   version = "1.0",
                   authors = "Marco Zühlke, Marco Peters, Ralf Quast",
                   copyright = "(c) 2009 by Brockmann Consult",
                   description = "Reprojection of a source product to a target Coordinate Reference System.",
                   internal = false)
 @SuppressWarnings({"UnusedDeclaration"})
 public class ReprojectionOp extends Operator {
 
 
     @SourceProduct(alias = "source", description = "The product which will be reprojected.")
     private Product sourceProduct;
     @SourceProduct(alias = "collocateWith", optional = true, label = "Collocation product",
                    description = "The source product will be collocated with this product.")
     private Product collocationProduct;
     @TargetProduct
     private Product targetProduct;
 
     @Parameter(description = "A file which contains the target Coordinate Reference System in WKT format.")
     private File wktFile;
 
     @Parameter(description = "A text specifying the target Coordinate Reference System, either in WKT or as an " +
                              "authority code. For appropriate EPSG authority codes see (www.epsg-registry.org). " +
                              "AUTO authority can be used with code 42001 (UTM), and 42002 (Transverse Mercator) " +
                              "where the scene center is used as reference. Examples: EPSG:4326, AUTO:42001")
     private String crs;
 
     @Parameter(alias = "resampling",
                label = "Resampling Method",
                description = "The method used for resampling of floating-point raster data.",
                valueSet = {"Nearest", "Bilinear", "Bicubic"},
                defaultValue = "Nearest")
     private String resamplingName;
 
     @Parameter(description = "Wether tie-point grids should be included in the output product.",
                defaultValue = "true")
     private boolean includeTiePointGrids;
 
     // Referencing  todo - parameter object?
     @Parameter(description = "The X-position of the reference pixel.")
     private Double referencePixelX;
     @Parameter(description = "The Y-position of the reference pixel.")
     private Double referencePixelY;
     @Parameter(description = "The easting of the reference pixel.")
     private Double easting;
     @Parameter(description = "The northing of the reference pixel.")
     private Double northing;
     @Parameter(description = "The orientation of the output product (in degree).",
                defaultValue = "0", interval = "[-360,360]")
     private Double orientation;
 
 
     // target grid  todo - parameter object?
     @Parameter(description = "The pixels per reference unit in X direction.")
     private Double pixelSizeX;
     @Parameter(description = "The pixels per reference unit in Y direction.")
     private Double pixelSizeY;
     @Parameter(description = "The width of the output product.")
     private Integer width;
     @Parameter(description = "The height of the output product.")
     private Integer height;
 
     @Parameter(
             description = "Wether the source product should be orthorectified. (Currently only applicable for MERIS and AATSR)",
             defaultValue = "false")
     private boolean orthorectify;
     @Parameter(
             description = "The name of the elevation model for the orthorectification. If not given tie-point data is used.")
     private String elevationModelName;
 
     @Parameter(description = "The value used to indicate no-data.")
     private Double noDataValue;
 
     private ElevationModel elevationModel;
     private MultiLevelModel srcModel;
     private MultiLevelModel targetModel;
     private Reproject reprojection;
 
 
     @Override
     public void initialize() throws OperatorException {
         validateCrsParameters();
         validateResamplingParameter();
         validateReferencingParameters();
         validateTargetGridParameters();
 
         /*
         * 1. Compute the target CRS
         */
         CoordinateReferenceSystem targetCrs = createTargetCRS();
         /*
         * 2. Compute the target geometry
         */
         ImageGeometry targetImageGeometry = createImageGeometry(targetCrs);
 
         /*
         * 3. Create the target product
         */
         Rectangle targetRect = targetImageGeometry.getImageRect();
         targetProduct = new Product("projected_" + sourceProduct.getName(),
                                     "projection of: " + sourceProduct.getDescription(),
                                     targetRect.width,
                                     targetRect.height);
         Dimension tileSize = ImageManager.getPreferredTileSize(targetProduct);
         targetProduct.setPreferredTileSize(tileSize);
         /*
         * 4. Define some target properties
         */
         if (orthorectify) {
             elevationModel = createElevationModel();
         }
         ProductUtils.copyMetadata(sourceProduct, targetProduct);
         ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
         copyIndexCoding();
         try {
             targetProduct.setGeoCoding(new CrsGeoCoding(targetImageGeometry.getMapCrs(),
                                                         targetRect,
                                                         targetImageGeometry.getImage2MapTransform()));
         } catch (Exception e) {
             throw new OperatorException(e);
         }
 
         srcModel = ImageManager.getMultiLevelModel(sourceProduct.getBandAt(0));
         targetModel = ImageManager.createMultiLevelModel(targetProduct);
         reprojection = new Reproject(targetModel.getLevelCount());
         reprojectRasterDataNodes(sourceProduct.getBands());
         if (includeTiePointGrids) {
             reprojectRasterDataNodes(sourceProduct.getTiePointGrids());
         }
         /*
         * Placemarks & masks
         */
         copyPlacemarks(sourceProduct.getPinGroup(), targetProduct.getPinGroup(),
                        PlacemarkSymbol.createDefaultPinSymbol());
         copyPlacemarks(sourceProduct.getGcpGroup(), targetProduct.getGcpGroup(),
                        PlacemarkSymbol.createDefaultGcpSymbol());
         ProductUtils.copyVectorData(sourceProduct, targetProduct);
         ProductUtils.copyMasks(sourceProduct, targetProduct);
         ProductUtils.copyOverlayMasks(sourceProduct, targetProduct);
         ProductUtils.copyRoiMasks(sourceProduct, targetProduct);
     }
 
     @Override
     public void dispose() {
         if (elevationModel != null) {
             elevationModel.dispose();
         }
     }
 
     private ElevationModel createElevationModel() throws OperatorException {
         if (elevationModelName != null) {
             final ElevationModelDescriptor demDescriptor = ElevationModelRegistry.getInstance().getDescriptor(
                     elevationModelName);
             if (!demDescriptor.isDemInstalled()) {
                 throw new OperatorException("DEM not installed: " + elevationModelName);
             }
             return demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
         }
         return null; // force use of elevation from tie-points
     }
 
     private GeoCoding getSourceGeoCoding(final RasterDataNode sourceBand) {
         if (orthorectify && sourceBand.canBeOrthorectified()) {
             return createOrthorectifier(sourceBand);
         } else {
             return sourceBand.getGeoCoding();
         }
     }
 
     private Orthorectifier createOrthorectifier(final RasterDataNode sourceBand) {
         return new Orthorectifier2(sourceBand.getSceneRasterWidth(),
                                    sourceBand.getSceneRasterHeight(),
                                    sourceBand.getPointing(),
                                    elevationModel, 25);
     }
 
 
     private void reprojectRasterDataNodes(RasterDataNode[] rasterDataNodes) {
         for (RasterDataNode raster : rasterDataNodes) {
             reprojectSourceRaster(raster);
         }
     }
 
     private void reprojectSourceRaster(RasterDataNode sourceRaster) {
         int geoDataType = sourceRaster.getGeophysicalDataType();
         double targetNoDataValue = getTargetNoDataValue(sourceRaster);
         Band targetBand = targetProduct.addBand(sourceRaster.getName(), geoDataType);
         targetBand.setNoDataValue(targetNoDataValue);
         targetBand.setNoDataValueUsed(true);
         targetBand.setDescription(sourceRaster.getDescription());
         targetBand.setUnit(sourceRaster.getUnit());
 
         GeoCoding sourceGeoCoding = getSourceGeoCoding(sourceRaster);
         MultiLevelImage sourceImage = sourceRaster.getGeophysicalImage();
         String exp = sourceRaster.getValidMaskExpression();
         if (exp != null) {
             // TODO decide between Virtualband and a special implementation (mz, 2009.11.11)
 //            final String externalName = BandArithmetic.createExternalName(sourceRaster.getName());
 //            exp = String.format("(%s) ? %s : %s", exp, externalName, Double.toString(targetNoDataValue));
 //            sourceImage = createVirtualSourceImage(exp, geoDataType, targetNoDataValue);
             sourceImage = createNoDataReplacedImage(sourceImage, sourceRaster.getValidMaskImage(), targetNoDataValue);
         }
 
         final Interpolation resampling = getResampling(targetBand);
         MultiLevelImage projectedImage = createProjectedImage(sourceGeoCoding, sourceImage, targetBand, resampling);
         if (mustReplaceNaN(sourceRaster, geoDataType, targetNoDataValue)) {
             projectedImage = createNaNReplacedImage(projectedImage, targetNoDataValue);
         }
         targetBand.setSourceImage(projectedImage);
 
         /*
         * Flag and index codings
         */
         if (sourceRaster instanceof Band) {
             Band sourceBand = (Band) sourceRaster;
             ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
             FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
             IndexCoding sourceIndexCoding = sourceBand.getIndexCoding();
             if (sourceFlagCoding != null) {
                 String flagCodingName = sourceFlagCoding.getName();
                 FlagCoding destFlagCoding = targetProduct.getFlagCodingGroup().get(flagCodingName);
                 targetBand.setSampleCoding(destFlagCoding);
             } else if (sourceIndexCoding != null) {
                 String indexCodingName = sourceIndexCoding.getName();
                 IndexCoding destIndexCoding = targetProduct.getIndexCodingGroup().get(indexCodingName);
                 targetBand.setSampleCoding(destIndexCoding);
             }
         }
     }
 
     private boolean mustReplaceNaN(RasterDataNode sourceRaster, int geophysicalDataType, double targetNoDataValue) {
         final boolean isFloat = ProductData.isFloatingPointType(geophysicalDataType);
         final boolean isNoDataGiven = sourceRaster.isNoDataValueUsed() || noDataValue != null;
         final boolean isNoDataNaN = Double.isNaN(targetNoDataValue);
         return isFloat && isNoDataGiven && !isNoDataNaN;
     }
 
     private double getTargetNoDataValue(RasterDataNode sourceRaster) {
         double targetNoDataValue = Double.NaN;
         if (noDataValue != null) {
             targetNoDataValue = noDataValue;
         } else if (sourceRaster.isNoDataValueUsed()) {
             targetNoDataValue = sourceRaster.getNoDataValue();
         }
         return targetNoDataValue;
     }
 
     private MultiLevelImage createNaNReplacedImage(final MultiLevelImage srcImage, final double value) {
 
         return new DefaultMultiLevelImage(new AbstractMultiLevelSource(srcModel) {
 
             @Override
             public RenderedImage createImage(int level) {
                 final int sourceLevel = getSourceLevel(srcModel, level);
                 return new ReplaceNaNOpImage(srcImage.getImage(sourceLevel), value);
             }
         });
     }
 
     private MultiLevelImage createNoDataReplacedImage(final MultiLevelImage srcImage, final MultiLevelImage maskImage,
                                                       final double noData) {
 
         return new DefaultMultiLevelImage(new AbstractMultiLevelSource(srcModel) {
 
             @Override
             public RenderedImage createImage(int level) {
                 return new InsertNoDataValueOpImage(srcImage.getImage(level), maskImage.getImage(level), noData);
             }
         });
     }
 
     private MultiLevelImage createVirtualSourceImage(final String expression, final int geoDataType,
                                                      final Number noDataValue) {
 
         return new DefaultMultiLevelImage(new AbstractMultiLevelSource(srcModel) {
 
             @Override
             public RenderedImage createImage(int level) {
                 return VirtualBandOpImage.create(expression, geoDataType,
                                                  noDataValue, sourceProduct,
                                                  ResolutionLevel.create(getModel(), level));
             }
         });
     }
 
     private MultiLevelImage createProjectedImage(final GeoCoding sourceGeoCoding, final MultiLevelImage sourceImage,
                                                  final Band targetBand, final Interpolation resampling) {
 
 
         final CoordinateReferenceSystem sourceModelCrs = ImageManager.getModelCrs(sourceGeoCoding);
         final CoordinateReferenceSystem targetModelCrs = ImageManager.getModelCrs(targetProduct.getGeoCoding());
         final AffineTransform i2mSourceProduct = ImageManager.getImageToModelTransform(sourceGeoCoding);
         final AffineTransform i2mTargetProduct = ImageManager.getImageToModelTransform(targetProduct.getGeoCoding());
 
 
         return new DefaultMultiLevelImage(new AbstractMultiLevelSource(targetModel) {
 
             @Override
             public RenderedImage createImage(int targetLevel) {
                 int sourceLevel = getSourceLevel(srcModel, targetLevel);
                 RenderedImage leveledSourceImage = sourceImage.getImage(sourceLevel);
 
                 final Rectangle sourceBounds = new Rectangle(leveledSourceImage.getWidth(),
                                                              leveledSourceImage.getHeight());
 
                 // the following transformation maps the source level image to level zero and then to the model,
                 // which either is a map or an image CRS
                 final AffineTransform i2mSource = srcModel.getImageToModelTransform(sourceLevel);
                 i2mSource.concatenate(srcModel.getModelToImageTransform(0));
                 i2mSource.concatenate(i2mSourceProduct);
 
                 ImageGeometry sourceGeometry = new ImageGeometry(sourceBounds,
                                                                  sourceModelCrs,
                                                                  i2mSource);
 
                 ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(
                         ImageManager.getDataBufferType(targetBand.getDataType()),
                         targetProduct.getSceneRasterWidth(),
                         targetProduct.getSceneRasterHeight(),
                         targetProduct.getPreferredTileSize(),
                         ResolutionLevel.create(getModel(), targetLevel));
                 Rectangle targetBounds = new Rectangle(imageLayout.getWidth(null), imageLayout.getHeight(null));
 
                 // the following transformation maps the target level image to level zero and then to the model,
                 // which always is a map
                 final AffineTransform i2mTarget = getModel().getImageToModelTransform(targetLevel);
                 i2mTarget.concatenate(getModel().getModelToImageTransform(0));
                 i2mTarget.concatenate(i2mTargetProduct);
 
                 ImageGeometry targetGeometry = new ImageGeometry(targetBounds,
                                                                  targetModelCrs,
                                                                  i2mTarget);
                 Hints hints = new Hints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
 
                 Dimension tileSize = ImageManager.getPreferredTileSize(targetProduct);
                 try {
                     return reprojection.reproject(leveledSourceImage, sourceGeometry, targetGeometry,
                                                   targetBand.getNoDataValue(), resampling, hints, targetLevel,
                                                   tileSize);
                 } catch (FactoryException e) {
                     Debug.trace(e);
                     throw new RuntimeException(e);
                 } catch (TransformException e) {
                     Debug.trace(e);
                     throw new RuntimeException(e);
                 }
             }
         });
     }
 
     private int getSourceLevel(MultiLevelModel srcModel, int targetLevel) {
         int sourceLevel = targetLevel;
         int sourceLevelCount = srcModel.getLevelCount();
         if (sourceLevelCount - 1 < targetLevel) {
             sourceLevel = sourceLevelCount - 1;
         }
         return sourceLevel;
     }
 
     private void copyIndexCoding() {
         final ProductNodeGroup<IndexCoding> indexCodingGroup = sourceProduct.getIndexCodingGroup();
         for (int i = 0; i < indexCodingGroup.getNodeCount(); i++) {
             IndexCoding sourceIndexCoding = indexCodingGroup.get(i);
             ProductUtils.copyIndexCoding(sourceIndexCoding, targetProduct);
         }
     }
 
     private static void copyPlacemarks(ProductNodeGroup<Pin> sourcePlacemarkGroup,
                                        ProductNodeGroup<Pin> targetPlacemarkGroup, PlacemarkSymbol symbol) {
         final Pin[] placemarks = sourcePlacemarkGroup.toArray(new Pin[0]);
         for (Pin placemark : placemarks) {
             final Pin pin1 = new Pin(placemark.getName(), placemark.getLabel(),
                                      placemark.getDescription(), null, placemark.getGeoPos(),
                                     symbol);
             targetPlacemarkGroup.add(pin1);
         }
     }
 
     public static class Spi extends OperatorSpi {
 
         public Spi() {
             super(ReprojectionOp.class);
         }
     }
 
     private CoordinateReferenceSystem createTargetCRS() throws OperatorException {
         try {
             if (wktFile != null) {
                 return CRS.parseWKT(FileUtils.readText(wktFile));
             }
             if (crs != null) {
                 try {
                     return CRS.parseWKT(crs);
                 } catch (FactoryException e) {
                     // prefix with EPSG, if there are only numbers
                     if (crs.matches("[0-9]*")) {
                         crs = "EPSG:" + crs;
                     }
                     // append center coordinates for AUTO code
                     if (crs.matches("AUTO:[0-9]*")) {
                         final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                         crs = String.format("%s,%s,%s", crs, centerGeoPos.lon, centerGeoPos.lat);
                     }
                     // force longitude==x-axis and latitude==y-axis
                     return CRS.decode(crs, true);
                 }
             }
             if (collocationProduct != null && collocationProduct.getGeoCoding() != null) {
                 return collocationProduct.getGeoCoding().getMapCRS();
             }
         } catch (FactoryException e) {
             throw new OperatorException(String.format("Target CRS could not be created: %s", e.getMessage()), e);
         } catch (IOException e) {
             throw new OperatorException(String.format("Target CRS could not be created: %s", e.getMessage()), e);
         }
 
         throw new OperatorException("Target CRS could not be created.");
     }
 
     protected void validateCrsParameters() {
         final String msgPattern = "Invalid target CRS specification.\nSpecify {0} one of the " +
                                   "''wktFile'', ''crs'' or ''collocationProduct'' parameters.";
 
         if (wktFile == null && crs == null && collocationProduct == null) {
             throw new OperatorException(MessageFormat.format(msgPattern, "at least"));
         }
 
         boolean crsDefined = false;
         final String exceptionMsg = MessageFormat.format(msgPattern, "only");
         if (wktFile != null) {
             crsDefined = true;
         }
         if (crs != null) {
             if (crsDefined) {
                 throw new OperatorException(exceptionMsg);
             }
             crsDefined = true;
         }
         if (collocationProduct != null) {
             if (crsDefined) {
                 throw new OperatorException(exceptionMsg);
             }
         }
     }
 
     private Interpolation getResampling(Band band) {
         int resampleType = getResampleType();
         if (!ProductData.isFloatingPointType(band.getDataType())) {
             resampleType = Interpolation.INTERP_NEAREST;
         }
         return Interpolation.getInstance(resampleType);
     }
 
     private int getResampleType() {
         final int resamplingType;
         if ("Nearest".equalsIgnoreCase(resamplingName)) {
             resamplingType = Interpolation.INTERP_NEAREST;
         } else if ("Bilinear".equalsIgnoreCase(resamplingName)) {
             resamplingType = Interpolation.INTERP_BILINEAR;
         } else if ("Bicubic".equalsIgnoreCase(resamplingName)) {
             resamplingType = Interpolation.INTERP_BICUBIC;
         } else {
             resamplingType = -1;
         }
         return resamplingType;
     }
 
     void validateResamplingParameter() {
         if (getResampleType() == -1) {
             throw new OperatorException("Invalid resampling method: " + resamplingName);
         }
     }
 
     void validateReferencingParameters() {
         if (!((referencePixelX == null && referencePixelY == null && easting == null && northing == null)
               || (referencePixelX != null && referencePixelY != null && easting != null && northing != null))) {
             throw new OperatorException("Invalid referencing parameters: \n" +
                                         "'referencePixelX, referencePixelY, easting and northing' have to be specified either all or non.");
         }
     }
 
     void validateTargetGridParameters() {
         if ((pixelSizeX != null && pixelSizeY == null) ||
             (pixelSizeX == null && pixelSizeY != null)) {
             throw new OperatorException("'pixelSizeX' and 'pixelSizeY' must be specifies both or not at all.");
         }
     }
 
     private ImageGeometry createImageGeometry(CoordinateReferenceSystem targetCrs) {
         if (collocationProduct != null) {
             return ImageGeometry.createCollocationTargetGeometry(collocationProduct);
 
         } else {
             ImageGeometry imageGeometry = ImageGeometry.createTargetGeometry(sourceProduct, targetCrs,
                                                                              pixelSizeX, pixelSizeY,
                                                                              width, height, orientation,
                                                                              easting, northing,
                                                                              referencePixelX, referencePixelY);
             final AxisDirection targetAxisDirection = targetCrs.getCoordinateSystem().getAxis(1).getDirection();
             // When collocating the Y-Axis is DISPLAY_DOWN, then pixelSizeY must not negated
             if (!AxisDirection.DISPLAY_DOWN.equals(targetAxisDirection)) {
                 imageGeometry.changeYAxisDirection();
             }
             return imageGeometry;
         }
     }
 }
