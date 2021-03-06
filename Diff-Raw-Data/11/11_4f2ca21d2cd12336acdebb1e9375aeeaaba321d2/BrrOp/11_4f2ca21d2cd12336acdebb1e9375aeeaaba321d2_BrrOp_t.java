 package org.esa.beam.meris.brr;
 
 import java.awt.Color;
 import java.awt.Rectangle;
 import java.util.Map;
 
 import org.esa.beam.dataio.envisat.EnvisatConstants;
 import org.esa.beam.framework.datamodel.Band;
 import org.esa.beam.framework.datamodel.BitmaskDef;
 import org.esa.beam.framework.datamodel.FlagCoding;
 import org.esa.beam.framework.datamodel.MetadataAttribute;
 import org.esa.beam.framework.datamodel.Product;
 import org.esa.beam.framework.datamodel.ProductData;
 import org.esa.beam.framework.datamodel.RasterDataNode;
 import org.esa.beam.framework.gpf.OperatorException;
 import org.esa.beam.framework.gpf.OperatorSpi;
 import org.esa.beam.framework.gpf.Tile;
 import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
 import org.esa.beam.framework.gpf.annotations.Parameter;
 import org.esa.beam.framework.gpf.annotations.SourceProduct;
 import org.esa.beam.framework.gpf.annotations.TargetProduct;
 import org.esa.beam.framework.gpf.operators.meris.MerisBasisOp;
 import org.esa.beam.meris.brr.dpm.AtmosphericCorrectionLand;
 import org.esa.beam.meris.brr.dpm.CloudClassification;
 import org.esa.beam.meris.brr.dpm.DpmPixel;
 import org.esa.beam.meris.brr.dpm.GaseousAbsorptionCorrection;
 import org.esa.beam.meris.brr.dpm.L1bDataExtraction;
 import org.esa.beam.meris.brr.dpm.PixelIdentification;
 import org.esa.beam.meris.brr.dpm.RayleighCorrection;
 import org.esa.beam.meris.l2auxdata.Constants;
 import org.esa.beam.meris.l2auxdata.L2AuxData;
 import org.esa.beam.meris.l2auxdata.L2AuxdataProvider;
 import org.esa.beam.util.BitSetter;
 
 import com.bc.ceres.core.ProgressMonitor;
 
 
 @OperatorMetadata(alias = "Meris.Brr",
         version = "1.0",
         authors = "Marco Zhlke",
         copyright = "(c) 2007 by Brockmann Consult",
         description = "Compute the BRR of a MERIS L1b product.")
 public class BrrOp extends MerisBasisOp {
 
     protected L1bDataExtraction extdatl1;
     protected PixelIdentification pixelid;
     protected CloudClassification classcloud;
     protected GaseousAbsorptionCorrection gaz_cor;
     protected RayleighCorrection ray_cor;
     protected AtmosphericCorrectionLand landac;
 
     // source product
     private RasterDataNode[] tpGrids;
     private RasterDataNode[] l1bRadiance;
     private RasterDataNode detectorIndex;
     private RasterDataNode l1bFlags;
 
     // target product
     protected Band l2FlagsP1;
     protected Band l2FlagsP2;
     protected Band l2FlagsP3;
 
     protected Band[] brrReflecBands = new Band[Constants.L1_BAND_NUM];
     protected Band[] toaReflecBands = new Band[Constants.L1_BAND_NUM];
 
     @SourceProduct(alias="input")
     private Product sourceProduct;
     @TargetProduct
     private Product targetProduct;
    @Parameter(description="If 'true' the TOA reflectances will be included into the target product.", defaultValue="false")
     public boolean outputToar = false;
    @Parameter(description="If 'false' the algorithm will only be aplied over land.", defaultValue="true")
    public boolean correctWater = true;
 
 
     @Override
     public void initialize() throws OperatorException {
         // todo - tell someone else that we need a 4x4 subwindow
 
         checkInputProduct(sourceProduct);
         prepareSourceProducts();
 
 
         targetProduct = createCompatibleProduct(sourceProduct, "BRR", "BRR");
 
         createOutputBands(brrReflecBands, "brr");
         if (outputToar) {
             createOutputBands(toaReflecBands, "toar");
         }
 
         l2FlagsP1 = addFlagsBand(createFlagCodingP1(), 0.0, 1.0, 0.5);
         l2FlagsP2 = addFlagsBand(createFlagCodingP2(), 0.2, 0.7, 0.0);
         l2FlagsP3 = addFlagsBand(createFlagCodingP3(), 0.8, 0.1, 0.3);
 
         initAlgorithms(sourceProduct); 
         pixelid.setCorrectWater(correctWater);
         landac.setCorrectWater(correctWater);
     }
 
     private void initAlgorithms(Product inputProduct) throws IllegalArgumentException {
         try {
             final L2AuxData auxData = L2AuxdataProvider.getInstance().getAuxdata(inputProduct);
             extdatl1 = new L1bDataExtraction(auxData);
             gaz_cor = new GaseousAbsorptionCorrection(auxData);
             pixelid = new PixelIdentification(auxData, gaz_cor);
             ray_cor = new RayleighCorrection(auxData);
             classcloud = new CloudClassification(auxData, ray_cor);
             landac = new AtmosphericCorrectionLand(ray_cor);
         } catch (Exception e) { // todo handle IOException and DpmException
             e.printStackTrace();
             throw new IllegalArgumentException(e.getMessage());
         }
     }
 
     protected void prepareSourceProducts() {
         final int numTPGrids = EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES.length;
         tpGrids = new RasterDataNode[numTPGrids];
         for (int i = 0; i < numTPGrids; i++) {
             tpGrids[i] = sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[i]);
         }
 
 // mz 2007-11-22 at the moment lat and lon are not used for any computation        
 //        if (sourceProduct.getProductType().equals(EnvisatConstants.MERIS_FSG_L1B_PRODUCT_TYPE_NAME)) {
 //            tpGrids[Constants.LATITUDE_TPG_INDEX] = sourceProduct.getBand("corr_latitude");
 //            tpGrids[Constants.LONGITUDE_TPG_INDEX] = sourceProduct.getBand("corr_longitude");
 //        }
         
         l1bRadiance = new RasterDataNode[EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES.length];
         for (int i = 0; i < l1bRadiance.length; i++) {
         	l1bRadiance[i] = sourceProduct.getBand(EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES[i]);
         }
         detectorIndex = sourceProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME);
         l1bFlags = sourceProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME);
     }
     
     @Override
     public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
 
         final int frameSize = rectangle.height * rectangle.width;
         DpmPixel[] frame = new DpmPixel[frameSize];
         DpmPixel[][] block = new DpmPixel[rectangle.height][rectangle.width];
         for (int pixelIndex = 0; pixelIndex < frameSize; pixelIndex++) {
             final DpmPixel pixel = new DpmPixel();
             pixel.i = pixelIndex % rectangle.width;
             pixel.j = pixelIndex / rectangle.width;
             frame[pixelIndex] = block[pixel.j][pixel.i] = pixel;
         }
 
         int[] l2FlagsP1Frame = new int[frameSize];
         int[] l2FlagsP2Frame = new int[frameSize];
         int[] l2FlagsP3Frame = new int[frameSize];
         Tile[] l1bTiePoints = new Tile[tpGrids.length];
         for (int i = 0; i < tpGrids.length; i++) {
             l1bTiePoints[i] = getSourceTile(tpGrids[i], rectangle, pm);
         }
         Tile[] l1bRadiances = new Tile[l1bRadiance.length];
         for (int i = 0; i < l1bRadiance.length; i++) {
             l1bRadiances[i] = getSourceTile(l1bRadiance[i], rectangle, pm);
         }
         Tile l1bDetectorIndex = getSourceTile(detectorIndex, rectangle, pm);
         Tile l1bFlagRaster = getSourceTile(l1bFlags, rectangle, pm);
         
         for (int pixelIndex = 0; pixelIndex < frameSize; pixelIndex++) {
             DpmPixel pixel = frame[pixelIndex];
             extdatl1.l1_extract_pixbloc(pixel,
                                         rectangle.x + pixel.i,
                                         rectangle.y + pixel.j,
                                         l1bTiePoints,
                                         l1bRadiances,
                                         l1bDetectorIndex,
                                         l1bFlagRaster);
 
             if (!BitSetter.isFlagSet(pixel.l2flags, Constants.F_INVALID)) {
                 pixelid.rad2reflect(pixel);
                 classcloud.classify_cloud(pixel);
             }
         }
 
         for (int iPL1 = 0; iPL1 < rectangle.height; iPL1 += Constants.SUBWIN_HEIGHT) {
             for (int iPC1 = 0; iPC1 < rectangle.width; iPC1 += Constants.SUBWIN_WIDTH) {
                 final int iPC2 = Math.min(rectangle.width, iPC1 + Constants.SUBWIN_WIDTH) - 1;
                 final int iPL2 = Math.min(rectangle.height, iPL1 + Constants.SUBWIN_HEIGHT) - 1;
                 pixelid.pixel_classification(block, iPC1, iPC2, iPL1, iPL2);
                 landac.landAtmCor(block, iPC1, iPC2, iPL1, iPL2);
             }
         }
 
         for (int iP = 0; iP < frame.length; iP++) {
             DpmPixel pixel = frame[iP];
             l2FlagsP1Frame[iP] = (int) ((pixel.l2flags & 0x00000000ffffffffL));
             l2FlagsP2Frame[iP] = (int) ((pixel.l2flags & 0xffffffff00000000L) >> 32);
             l2FlagsP3Frame[iP] = pixel.ANNOT_F;
         }
 
 
         for (int bandIndex = 0; bandIndex < brrReflecBands.length; bandIndex++) {
             if (isValidRhoSpectralIndex(bandIndex)) {
                 ProductData data = targetTiles.get(brrReflecBands[bandIndex]).getRawSamples();
                 float[] ddata = (float[]) data.getElems();
                 for (int iP = 0; iP < rectangle.width * rectangle.height; iP++) {
                     ddata[iP] = (float) frame[iP].rho_top[bandIndex];
                 }
                 targetTiles.get(brrReflecBands[bandIndex]).setRawSamples(data);
             }
         }
         if (outputToar) {
             for (int bandIndex = 0; bandIndex < toaReflecBands.length; bandIndex++) {
                 ProductData data = targetTiles.get(toaReflecBands[bandIndex]).getRawSamples();
                 float[] ddata = (float[]) data.getElems();
                 for (int iP = 0; iP < rectangle.width * rectangle.height; iP++) {
                     ddata[iP] = (float) frame[iP].rho_toa[bandIndex];
                 }
                 targetTiles.get(toaReflecBands[bandIndex]).setRawSamples(data);
             }
         }
         ProductData flagData = targetTiles.get(l2FlagsP1).getRawSamples();
         int[] intFlag = (int[]) flagData.getElems();
         System.arraycopy(l2FlagsP1Frame, 0, intFlag, 0, rectangle.width * rectangle.height);
         targetTiles.get(l2FlagsP1).setRawSamples(flagData);
         
         flagData = targetTiles.get(l2FlagsP2).getRawSamples();
         intFlag = (int[]) flagData.getElems();
         System.arraycopy(l2FlagsP2Frame, 0, intFlag, 0, rectangle.width * rectangle.height);
         targetTiles.get(l2FlagsP2).setRawSamples(flagData);
 
         flagData = targetTiles.get(l2FlagsP3).getRawSamples();
         intFlag = (int[]) flagData.getElems();
         System.arraycopy(l2FlagsP3Frame, 0, intFlag, 0, rectangle.width * rectangle.height);
         targetTiles.get(l2FlagsP3).setRawSamples(flagData);
     }
 
     protected Band addFlagsBand(final FlagCoding flagCodingP1, final double rf1, final double gf1, final double bf1) {
         addFlagCodingAndCreateBMD(flagCodingP1, rf1, gf1, bf1);
         final Band l2FlagsP1Band = new Band(flagCodingP1.getName(), ProductData.TYPE_INT32,
                                         targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight());
         l2FlagsP1Band.setFlagCoding(flagCodingP1);
         targetProduct.addBand(l2FlagsP1Band);
         return l2FlagsP1Band;
     }
 
     protected void addFlagCodingAndCreateBMD(FlagCoding flagCodingP1, double rf1, double gf1, double bf1) {
         targetProduct.addFlagCoding(flagCodingP1);
         for (int i = 0; i < flagCodingP1.getNumAttributes(); i++) {
             final MetadataAttribute attribute = flagCodingP1.getAttributeAt(i);
             final double a = 2 * Math.PI * (i / 31.0);
             final Color color = new Color((float) (0.5 + 0.5 * Math.sin(a + rf1 * Math.PI)),
                                           (float) (0.5 + 0.5 * Math.sin(a + gf1 * Math.PI)),
                                           (float) (0.5 + 0.5 * Math.sin(a + bf1 * Math.PI)));
             targetProduct.addBitmaskDef(new BitmaskDef(attribute.getName(),
                                                        null,
                                                        flagCodingP1.getName() + "." + attribute.getName(),
                                                        color,
                                                        0.4F));
         }
     }
 
     protected void createOutputBands(Band[] bands, final String name) {
         final Product soucreProduct = getSourceProduct("input");
         final int sceneWidth = targetProduct.getSceneRasterWidth();
         final int sceneHeight = targetProduct.getSceneRasterHeight();
 
         for (int bandId = 0; bandId < bands.length; bandId++) {
             if (isValidRhoSpectralIndex(bandId) || name.equals("toar")) {
                 Band aNewBand = new Band(name + "_" + (bandId + 1), ProductData.TYPE_FLOAT32, sceneWidth,
                                          sceneHeight);
                 aNewBand.setNoDataValueUsed(true);
                 aNewBand.setNoDataValue(-1);
                 aNewBand.setSpectralBandIndex(soucreProduct.getBandAt(bandId).getSpectralBandIndex());
                 aNewBand.setSpectralWavelength(soucreProduct.getBandAt(bandId).getSpectralWavelength());
                aNewBand.setSpectralBandwidth(soucreProduct.getBandAt(bandId).getSpectralBandwidth());
                 targetProduct.addBand(aNewBand);
                 bands[bandId] = aNewBand;
             }
         }
     }
 
     protected void checkInputProduct(Product inputProduct) throws IllegalArgumentException {
         String name;
 
         for (int i = 0; i < EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES.length; i++) {
             name = EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[i];
             if (inputProduct.getTiePointGrid(name) == null) {
                 throw new IllegalArgumentException("Invalid input product. Missing tie point grid '" + name + "'.");
             }
         }
 
         for (int i = 0; i < EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES.length; i++) {
             name = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES[i];
             if (inputProduct.getBand(name) == null) {
                 throw new IllegalArgumentException("Invalid input product. Missing band '" + name + "'.");
             }
         }
 
         name = EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME;
         if (inputProduct.getBand(name) == null) {
             throw new IllegalArgumentException("Invalid input product. Missing dataset '" + name + "'.");
         }
 
         name = EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME;
         if (inputProduct.getBand(name) == null) {
             throw new IllegalArgumentException("Invalid input product. Missing dataset '" + name + "'.");
         }
     }
 
     protected static FlagCoding createFlagCodingP1() {
         FlagCoding flagCoding = new FlagCoding("l2_flags_p1");
         flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, Constants.F_BRIGHT), null);
         flagCoding.addFlag("F_CASE2_S", BitSetter.setFlag(0, Constants.F_CASE2_S), null);
         flagCoding.addFlag("F_CASE2ANOM", BitSetter.setFlag(0, Constants.F_CASE2ANOM), null);
         flagCoding.addFlag("F_CASE2Y", BitSetter.setFlag(0, Constants.F_CASE2Y), null);
         flagCoding.addFlag("F_CHL1RANGE_IN", BitSetter.setFlag(0, Constants.F_CHL1RANGE_IN), null);
         flagCoding.addFlag("F_CHL1RANGE_OUT", BitSetter.setFlag(0, Constants.F_CHL1RANGE_OUT), null);
         flagCoding.addFlag("F_CIRRUS", BitSetter.setFlag(0, Constants.F_CIRRUS), null);
         flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, Constants.F_CLOUD), null);
         flagCoding.addFlag("F_CLOUDPART", BitSetter.setFlag(0, Constants.F_CLOUDPART), null);
         flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, Constants.F_COASTLINE), null);
         flagCoding.addFlag("F_COSMETIC", BitSetter.setFlag(0, Constants.F_COSMETIC), null);
         flagCoding.addFlag("F_DDV", BitSetter.setFlag(0, Constants.F_DDV), null);
         flagCoding.addFlag("F_DUPLICATED", BitSetter.setFlag(0, Constants.F_DUPLICATED), null);
         flagCoding.addFlag("F_HIINLD", BitSetter.setFlag(0, Constants.F_HIINLD), null);
         flagCoding.addFlag("F_ICE_HIGHAERO", BitSetter.setFlag(0, Constants.F_ICE_HIGHAERO), null);
         flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, Constants.F_INVALID), null);
         flagCoding.addFlag("F_ISLAND", BitSetter.setFlag(0, Constants.F_ISLAND), null);
         flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, Constants.F_LAND), null);
         flagCoding.addFlag("F_LANDCONS", BitSetter.setFlag(0, Constants.F_LANDCONS), null);
         flagCoding.addFlag("F_LOINLD", BitSetter.setFlag(0, Constants.F_LOINLD), null);
         flagCoding.addFlag("F_MEGLINT", BitSetter.setFlag(0, Constants.F_MEGLINT), null);
         flagCoding.addFlag("F_ORINP1", BitSetter.setFlag(0, Constants.F_ORINP1), null);
         flagCoding.addFlag("F_ORINP2", BitSetter.setFlag(0, Constants.F_ORINP2), null);
         flagCoding.addFlag("F_ORINPWV", BitSetter.setFlag(0, Constants.F_ORINPWV), null);
         flagCoding.addFlag("F_OROUT1", BitSetter.setFlag(0, Constants.F_OROUT1), null);
         flagCoding.addFlag("F_OROUT2", BitSetter.setFlag(0, Constants.F_OROUT2), null);
         flagCoding.addFlag("F_OROUTWV", BitSetter.setFlag(0, Constants.F_OROUTWV), null);
         flagCoding.addFlag("F_SUSPECT", BitSetter.setFlag(0, Constants.F_SUSPECT), null);
         flagCoding.addFlag("F_UNCGLINT", BitSetter.setFlag(0, Constants.F_UNCGLINT), null);
         flagCoding.addFlag("F_WHITECAPS", BitSetter.setFlag(0, Constants.F_WHITECAPS), null);
         flagCoding.addFlag("F_WVAP", BitSetter.setFlag(0, Constants.F_WVAP), null);
         flagCoding.addFlag("F_ACFAIL", BitSetter.setFlag(0, Constants.F_ACFAIL), null);
         return flagCoding;
     }
 
     protected static FlagCoding createFlagCodingP2() {
         FlagCoding flagCoding = new FlagCoding("l2_flags_p2");
         flagCoding.addFlag("F_CONSOLID", BitSetter.setFlag(0, Constants.F_CONSOLID), null);
         flagCoding.addFlag("F_ORINP0", BitSetter.setFlag(0, Constants.F_ORINP0), null);
         flagCoding.addFlag("F_OROUT0", BitSetter.setFlag(0, Constants.F_OROUT0), null);
         flagCoding.addFlag("F_LOW_NN_P", BitSetter.setFlag(0, Constants.F_LOW_NN_P), null);
         flagCoding.addFlag("F_PCD_NN_P", BitSetter.setFlag(0, Constants.F_PCD_NN_P), null);
         flagCoding.addFlag("F_LOW_POL_P", BitSetter.setFlag(0, Constants.F_LOW_POL_P), null);
         flagCoding.addFlag("F_PCD_POL_P", BitSetter.setFlag(0, Constants.F_PCD_POL_P), null);
         flagCoding.addFlag("F_CONFIDENCE_P", BitSetter.setFlag(0, Constants.F_CONFIDENCE_P), null);
         flagCoding.addFlag("F_SLOPE_1", BitSetter.setFlag(0, Constants.F_SLOPE_1), null);
         flagCoding.addFlag("F_SLOPE_2", BitSetter.setFlag(0, Constants.F_SLOPE_2), null);
         flagCoding.addFlag("F_UNCERTAIN", BitSetter.setFlag(0, Constants.F_UNCERTAIN), null);
         flagCoding.addFlag("F_SUN70", BitSetter.setFlag(0, Constants.F_SUN70), null);
         flagCoding.addFlag("F_WVHIGLINT", BitSetter.setFlag(0, Constants.F_WVHIGLINT), null);
         flagCoding.addFlag("F_TOAVIVEG", BitSetter.setFlag(0, Constants.F_TOAVIVEG), null);
         flagCoding.addFlag("F_TOAVIBAD", BitSetter.setFlag(0, Constants.F_TOAVIBAD), null);
         flagCoding.addFlag("F_TOAVICSI", BitSetter.setFlag(0, Constants.F_TOAVICSI), null);
         flagCoding.addFlag("F_TOAVIWS", BitSetter.setFlag(0, Constants.F_TOAVIWS), null);
         flagCoding.addFlag("F_TOAVIBRIGHT", BitSetter.setFlag(0, Constants.F_TOAVIBRIGHT), null);
         flagCoding.addFlag("F_TOAVIINVALREC", BitSetter.setFlag(0, Constants.F_TOAVIINVALREC), null);
         return flagCoding;
     }
 
     protected static FlagCoding createFlagCodingP3() {
         FlagCoding flagCoding = new FlagCoding("l2_flags_p3");
         for (int i = 0; i < Constants.L1_BAND_NUM; i++) {
             flagCoding.addFlag("F_INVALID_REFLEC_" + (i + 1), BitSetter.setFlag(0, i), null);
         }
         return flagCoding;
     }
     
     static boolean isValidRhoSpectralIndex(int i) {
         return i >= Constants.bb1 && i < Constants.bb15 && i != Constants.bb11;
     }
 
     public static class Spi extends OperatorSpi {
         public Spi() {
             super(BrrOp.class);
         }
     }
 }
