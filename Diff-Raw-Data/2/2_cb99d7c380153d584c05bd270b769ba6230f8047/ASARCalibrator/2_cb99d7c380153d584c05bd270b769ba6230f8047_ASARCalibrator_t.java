 /*
  * Copyright (C) 2002-2007 by ?
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
 package org.esa.nest.gpf;
 
 import com.bc.ceres.core.ProgressMonitor;
 import org.esa.beam.dataio.envisat.EnvisatAuxReader;
 import org.esa.beam.framework.datamodel.*;
 import org.esa.beam.framework.gpf.OperatorException;
 import org.esa.beam.framework.gpf.Tile;
 import org.esa.beam.framework.gpf.internal.OperatorContext;
 import org.esa.beam.util.math.MathUtils;
 import org.esa.nest.datamodel.AbstractMetadata;
 import org.esa.nest.datamodel.Calibrator;
 import org.esa.nest.datamodel.Unit;
 import org.esa.nest.util.Constants;
 import org.esa.nest.util.GeoUtils;
 import org.esa.nest.util.Settings;
 
 import java.awt.*;
 import java.io.File;
 import java.io.IOException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.HashMap;
 
 /**
  * Calibration for ASAR data products.
  */
 public class ASARCalibrator implements Calibrator {
 
     private Product sourceProduct;
     private Product targetProduct;
 
     private File externalAuxFile = null;
     private boolean outputImageScaleInDb = false;
 
     protected MetadataElement absRoot = null;
 
     private String productType = null;
     private String oldXCAFileName = null; // the old XCA file
     private String newXCAFileName = null; // XCA file for radiometric calibration
     private String newXCAFilePath = null; // absolute path for XCA file
     protected final String[] mdsPolar = new String[2]; // polarizations for the two bands in the product
 
     protected TiePointGrid incidenceAngle = null;
     protected TiePointGrid slantRangeTime = null;
     protected TiePointGrid latitude = null;
     protected TiePointGrid longitude = null;
 
     private boolean srgrFlag = false;
     private boolean multilookFlag = false;
     private boolean wideSwathProductFlag = false;
     private boolean retroCalibrationFlag = false;
     private boolean applyAntennaPatternCorr = false;
     private boolean applyRangeSpreadingCorr = false;
 
     private double firstLineUTC = 0.0; // in days
     private double lineTimeInterval = 0.0; // in days
     private double avgSceneHeight = 0.0; // in m
     private double rangeSpacing = 0.0; // in m
     private double azimuthSpacing = 0.0; // in m
     private double rangeSpreadingCompPower = 0.0; // power in range spreading loss compensation calculation
     private double halfRangeSpreadingCompPower = 0.0;
     private double latMin = 0.0;
     private double latMax = 0.0;
     private double delLat = 0.0;
 
     private double[] earthRadius = null; // Earth radius for all range lines, in m
     private double[] newCalibrationConstant = new double[2];
     private double[] oldRefElevationAngle = null; // reference elevation angle for given swath in old aux file, in degree
     private double[] newRefElevationAngle = null; // reference elevation angle for given swath in new aux file, in degree
     private double[][] targetTileOldAntPat = null; // old antenna pattern gains for row pixels in a tile, in linear scale
     private double[][] targetTileNewAntPat = null; // new antenna pattern gains for row pixels in a tile, in linear scale
 
     private float[][] oldAntennaPatternSingleSwath = null; // old antenna pattern gains for single swath product, in dB
     private float[][] oldAntennaPatternWideSwath = null; // old antenna pattern gains for single swath product, in dB
     private float[][] newAntennaPatternSingleSwath = null; // new antenna pattern gains for single swath product, in dB
     private float[][] newAntennaPatternWideSwath = null; // new antenna pattern gains for single swath product, in dB
 
     protected int numMPPRecords; // number of MPP ADSR records
     protected int[] lastLineIndex = null; // the index of the last line covered by each MPP ADSR record
     protected String swath;
     private AbstractMetadata.OrbitStateVector[] orbitStateVectors = null;
     private AbstractMetadata.SRGRCoefficientList[] srgrConvParams = null;
 
     private static final int numOfGains = 201; // number of antenna pattern gain values for a given swath and
                                                // polarization in the aux file
     private static final double refSlantRange = 800000.0; //  m
     protected static final double halfLightSpeedByRefSlantRange = Constants.halfLightSpeed / refSlantRange;
     protected static final double underFlowFloat = 1.0e-30;
     private static final double MeanEarthRadius = 6371008.7714; // in m (WGS84)
     private static final int INVALID_SUB_SWATH_INDEX = -1;
 
     public ASARCalibrator() {
     }
 
     /**
      * Set flag indicating if target image is output in dB scale.
      */
     @Override
     public void setOutputImageIndB(boolean flag) {
         outputImageScaleInDb = flag;
     }
 
     /**
      * Set external auxiliary file.
      */
     @Override
     public void setExternalAuxFile(File file) {
         externalAuxFile = file;
     }
 
     /**
 
      */
     @Override
     public void initialize(Product srcProduct, Product tgtProduct) throws OperatorException {
         try {
             sourceProduct = srcProduct;
             targetProduct = tgtProduct;
 
             absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
 
             getProductType();
 
             getSRGRFlag();
 
             getCalibrationFlags();
 
             getMultilookFlag();
 
             getProductSwath();
 
             setCalibrationFlags();
 
             getProductPolarization();
 
             getRangeAzimuthSpacing();
             
             numMPPRecords = getNumOfRecordsInMainProcParam(sourceProduct);  //???
 
             getTiePointGridData(sourceProduct);
 
             if (retroCalibrationFlag) {
 
                 getSrgrCoeff();
 
                 getOldXCAFile();
 
                 getOldAntennaPattern();
             }
 
             if (applyAntennaPatternCorr) {
 
                 getFirstLineTime();
 
                 getLineTimeInterval();
 
                 getAverageSceneHeight();
 
                 getOrbitStateVectors();
 
                 getNewXCAFile();
 
                 getNewAntennaPattern();
 
                 getNewCalibrationFactor();
 
                 computeEarthRadius();
             }
 
             setRangeSpreadingLossCompPower();
 
             updateTargetProductMetadata();
 
         } catch(Exception e) {
             throw new OperatorException(e);
         }
     }
 
     /**
      * Get Product ID from MPH.
      * @throws OperatorException The exceptions.
      */
     private void getProductType() throws OperatorException {
 
         productType = sourceProduct.getProductType();
         // product type could be 1P or 1C
         if (!productType.contains("ASA_IMP_1") && !productType.contains("ASA_IMM_1") &&
             !productType.contains("ASA_APP_1") && !productType.contains("ASA_APM_1") &&
             !productType.contains("ASA_WSM_1") && !productType.contains("ASA_IMG_1") &&
             !productType.contains("ASA_APG_1") && !productType.contains("ASA_IMS_1") &&
             !productType.contains("ASA_APS_1") && !productType.contains("ASA_GM")) {
 
             throw new OperatorException(productType + " is not a valid ASAR product type for calibration.");
         }
     }
 
     /**
      * Get antenna elevation correction flag and range spreading compensation flag from Metadata.
      * @throws Exception The exceptions.
      */
     private void getCalibrationFlags() throws Exception {
 
         if (AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.abs_calibration_flag)) {
             throw new OperatorException("The product has already been calibrated.");
         }
 
         if (AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.ant_elev_corr_flag) != srgrFlag) {
             throw new OperatorException("The ant_elev_corr_flag is not consistent with srgr_flag in metadata.");
         }
 
         if (AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.range_spread_comp_flag) != srgrFlag) {
             throw new OperatorException("The range_spread_comp_flag is not consistent with srgr_flag in metadata.");
         }
     }
 
     /**
      * Get SRGR flag from the abstracted metadata.
      * @throws Exception The exceptions.
      */
     private void getSRGRFlag() throws Exception {
         srgrFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.srgr_flag);
     }
 
     /**
      * Get multilook flag from the abstracted metadata.
      * @throws Exception The exceptions.
      */
     private void getMultilookFlag() throws Exception {
         multilookFlag = AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.multilook_flag);
     }
 
     /**
      * Get product swath.
      * @throws Exception The exceptions.
      */
     private void getProductSwath() throws Exception {
         swath = absRoot.getAttributeString(AbstractMetadata.SWATH);
         wideSwathProductFlag = swath.contains("WS");
     }
 
     /**
      * Set calibration flags.
      */
     private void setCalibrationFlags() {
 
         if (srgrFlag) {
             if (multilookFlag) {
                 retroCalibrationFlag = false;
                 System.out.println("Only constant and incidence angle corrections will be performed for radiometric calibration");
             } else {
                 retroCalibrationFlag = true;
             }
         }
 
         applyAntennaPatternCorr = !srgrFlag || retroCalibrationFlag;
         applyRangeSpreadingCorr = !srgrFlag;
     }
 
     /**
      * Get SRGR conversion parameters.
      * @throws Exception The exceptions.
      */
     private void getSrgrCoeff() throws Exception {
         srgrConvParams = AbstractMetadata.getSRGRCoefficients(absRoot);
     }
 
     /**
      * Get range spacing from the abstracted metadata.
      * @throws Exception The exceptions.
      */
     private void getRangeAzimuthSpacing() throws Exception {
         rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
         azimuthSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
     }
 
     /**
      * Get product polarizations for each band in the product.
      * @throws Exception The exceptions.
      */
     private void getProductPolarization() throws Exception {
 
         String polarName = absRoot.getAttributeString(AbstractMetadata.mds1_tx_rx_polar);
         mdsPolar[0] = null;
         if (polarName.contains("HH") || polarName.contains("HV") || polarName.contains("VH") || polarName.contains("VV")) {
             mdsPolar[0] = polarName.toLowerCase();
         }
 
         mdsPolar[1] = null;
         polarName = absRoot.getAttributeString(AbstractMetadata.mds2_tx_rx_polar);
         if (polarName.contains("HH") || polarName.contains("HV") || polarName.contains("VH") || polarName.contains("VV")) {
             mdsPolar[1] = polarName.toLowerCase();
         }
     }
 
 
     /**
      * Get XCA file used for the original radiometric calibration.
      * @throws Exception The exceptions.
      */
     private void getOldXCAFile() throws Exception {
         oldXCAFileName = absRoot.getAttributeString(AbstractMetadata.external_calibration_file);
     }
 
     /**
      * Get old antenna pattern gains.
      */
     private void getOldAntennaPattern() {
 
         final String xcaFilePath = Settings.instance().get("AuxData/envisatAuxDataPath") + File.separator + oldXCAFileName;
 
         if (wideSwathProductFlag) {
 
             oldRefElevationAngle = new double[5]; // reference elevation angles for 5 sub swathes
             oldAntennaPatternWideSwath = new float[5][numOfGains]; // antenna pattern gain for 5 sub swathes
             getWideSwathAntennaPatternGainFromAuxData(
                     xcaFilePath, mdsPolar[0], numOfGains, oldRefElevationAngle, oldAntennaPatternWideSwath);
 
         } else {
 
             oldRefElevationAngle = new double[1]; // reference elevation angle for 1 swath
             oldAntennaPatternSingleSwath = new float[2][numOfGains]; // antenna pattern gain for 2 bands
             getSingleSwathAntennaPatternGainFromAuxData(
                     xcaFilePath, swath, mdsPolar, numOfGains, oldRefElevationAngle, oldAntennaPatternSingleSwath);
         }
     }
 
     /**
      * Get number of records in Main Processing Params data set.
      * @param sourceProduct The source prodict.
      * @return The number of records.
      * @throws org.esa.beam.framework.gpf.OperatorException The exceptions.
      */
     static int getNumOfRecordsInMainProcParam(Product sourceProduct) throws OperatorException {
 
         final MetadataElement dsd = sourceProduct.getMetadataRoot().getElement("DSD").getElement("DSD.3");
         if (dsd == null) {
             throw new OperatorException("DSD not found");
         }
 
         final MetadataAttribute numRecordsAttr = dsd.getAttribute("num_records");
         if (numRecordsAttr == null) {
             throw new OperatorException("num_records not found");
         }
         int numMPPRecords = numRecordsAttr.getData().getElemInt();
         if (numMPPRecords < 1) {
             throw new OperatorException("Invalid num_records.");
         }
         //System.out.println("The number of Main Processing Params records is " + numMPPRecords);
         return numMPPRecords;
     }
 
     /**
      * Get incidence angle and slant range time tie point grids.
      * @param sourceProduct the source
      */
     private void getTiePointGridData(Product sourceProduct) {
         slantRangeTime = OperatorUtils.getSlantRangeTime(sourceProduct);
         incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
         latitude = OperatorUtils.getLatitude(sourceProduct);
         longitude = OperatorUtils.getLongitude(sourceProduct);
     }
 
     private void getNewXCAFile() throws Exception {
 
         if (externalAuxFile != null && externalAuxFile.exists()) {
 
             if (!externalAuxFile.getName().contains("ASA_XCA")) {
                 throw new OperatorException("Invalid XCA file for ASAR product");
             }
             newXCAFileName = externalAuxFile.getName();
             newXCAFilePath = externalAuxFile.getAbsolutePath();
 
         } else {
 
             final Date startDate = sourceProduct.getStartTime().getAsDate();
             final Date endDate = sourceProduct.getEndTime().getAsDate();
             final File xcaFileDir = new File(Settings.instance().get("AuxData/envisatAuxDataPath"));
             newXCAFileName = findXCAFile(xcaFileDir, startDate, endDate);
             newXCAFilePath = xcaFileDir.toString() + File.separator + newXCAFileName;
         }
 
         if (newXCAFileName == null) {
             throw new OperatorException("No proper XCA file has been found");
         }
     }
 
     /**
      * Find the latest XVA file available.
      * @param xcaFileDir The complete path to the XCA file directory.
      * @param productStartDate The product start date.
      * @param productEndDate The product end data.
      * @return The name of the XCA file found.
      * @throws Exception The exceptions.
      */
     public static String findXCAFile(File xcaFileDir, Date productStartDate, Date productEndDate) throws Exception {
 
         final File[] list = xcaFileDir.listFiles();
         if(list == null) {
             return null;
         }
 
         final SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMdd_HHmmss");
         Date latestCreationDate = dateformat.parse("19000101_000000");
         String xcaFileName = null;
 
         for(File f : list) {
 
             final String fileName = f.getName();
             if (fileName.length() < 61 || !fileName.substring(0,10).equals("ASA_XCA_AX")) {
                 continue;
             }
             final Date creationDate = dateformat.parse(fileName.substring(14, 29));
             final Date validStartDate = dateformat.parse(fileName.substring(30, 45));
             final Date validStopDate = dateformat.parse(fileName.substring(46, 61));
 
             if (productStartDate.after(validStartDate) && productEndDate.before(validStopDate) &&
                 latestCreationDate.before(creationDate)) {
 
                 latestCreationDate = creationDate;
                 xcaFileName = fileName;
             }
         }
         return xcaFileName;
     }
 
     /**
      * Get calibration factor.
      */
     private void getNewCalibrationFactor() {
 
         if (newXCAFilePath != null) {
             getCalibrationFactorFromExternalAuxFile(newXCAFilePath, swath, mdsPolar, productType, newCalibrationConstant);
         } else {
             getCalibrationFactorFromMetadata();
         }
     }
 
     /**
      * Get calibration factor from user specified auxiliary data.
      * @param auxFilePath The absolute path to the aux file.
      * @param swath The product swath.
      * @param mdsPolar The product polarizations.
      * @param productType The product type.
      * @param calibrationFactor The calibration factors.
      */
     public static void getCalibrationFactorFromExternalAuxFile(
             String auxFilePath, String swath, String[] mdsPolar, String productType, double[] calibrationFactor) {
 
         final EnvisatAuxReader reader = new EnvisatAuxReader();
 
         try {
 
             reader.readProduct(auxFilePath);
 
             final int numOfSwaths = 7;
             String calibrationFactorName;
             for (int i = 0; i < 2 && mdsPolar[i] != null && mdsPolar[i].length() != 0; i++) {
 
                 calibrationFactor[i] = 0;
 
                 if (productType.contains("ASA_IMP_1")) {
                     calibrationFactorName = "ext_cal_im_pri_" + mdsPolar[i];
                 } else if (productType.contains("ASA_IMM_1")) {
                     calibrationFactorName = "ext_cal_im_med_" + mdsPolar[i];
                 } else if (productType.contains("ASA_APP_1")) {
                     calibrationFactorName = "ext_cal_ap_pri_" + mdsPolar[i];
                 } else if (productType.contains("ASA_APM_1")) {
                     calibrationFactorName = "ext_cal_ap_med_" + mdsPolar[i];
                 } else if (productType.contains("ASA_WSM_1")) {
                     calibrationFactorName = "ext_cal_ws_" + mdsPolar[i];
                 } else if (productType.contains("ASA_GM1_1")) {
                    calibrationFactorName = "ext_cal_gm_" + mdsPolar[i];
                 } else if (productType.contains("ASA_IMG_1")) {
                     calibrationFactorName = "ext_cal_im_geo_" + mdsPolar[i];
                 } else if (productType.contains("ASA_APG_1")) {
                     calibrationFactorName = "ext_cal_ap_geo_" + mdsPolar[i];
                 } else if (productType.contains("ASA_IMS_1")) {
                     calibrationFactorName = "ext_cal_im_" + mdsPolar[i];
                 } else if (productType.contains("ASA_APS_1")) {
                     calibrationFactorName = "ext_cal_ap_" + mdsPolar[i];
                 } else {
                     throw new OperatorException("Invalid ASAR product type.");
                 }
 
                 final ProductData factorData = reader.getAuxData(calibrationFactorName);
                 final float[] factors = (float[]) factorData.getElems();
 
                 if (productType.contains("ASA_WSM_1") || productType.contains("ASA_GM1")) {
                     calibrationFactor[i] = factors[0];
                 } else {
                     if (factors.length != numOfSwaths) {
                         throw new OperatorException("Incorrect array length for " + calibrationFactorName);
                     }
                     if (swath.contains("IS1")) {
                         calibrationFactor[i] = factors[0];
                     } else if (swath.contains("IS2")) {
                         calibrationFactor[i] = factors[1];
                     } else if (swath.contains("IS3")) {
                         calibrationFactor[i] = factors[2];
                     } else if (swath.contains("IS4")) {
                         calibrationFactor[i] = factors[3];
                     } else if (swath.contains("IS5")) {
                         calibrationFactor[i] = factors[4];
                     } else if (swath.contains("IS6")) {
                         calibrationFactor[i] = factors[5];
                     } else if (swath.contains("IS7")) {
                         calibrationFactor[i] = factors[6];
                     } else {
                         throw new OperatorException("Invalid swath");
                     }
                 }
             }
 
         } catch (IOException e) {
             throw new OperatorException(e);
         }
 
         if (Double.compare(calibrationFactor[0], 0.0) == 0 && Double.compare(calibrationFactor[1], 0.0) == 0) {
             throw new OperatorException("Calibration factors in user provided auxiliary file are zero");
         }
     }
 
     /**
      * Get calibration factors from Metadata for each band in the product.
      * Here it is assumed that the calibration factor values do not change in case that there are
      * multiple records in the Main Processing Parameters data set.
      */
     private void getCalibrationFactorFromMetadata() {
 
         MetadataElement ads;
 
         if (numMPPRecords == 1) {
             ads = sourceProduct.getMetadataRoot().getElement("MAIN_PROCESSING_PARAMS_ADS");
         } else {
             ads = sourceProduct.getMetadataRoot().getElement("MAIN_PROCESSING_PARAMS_ADS").
                     getElement("MAIN_PROCESSING_PARAMS_ADS.1");
         }
 
         if (ads == null) {
             throw new OperatorException("MAIN_PROCESSING_PARAMS_ADS not found");
         }
 
         MetadataAttribute calibrationFactorsAttr =
                 ads.getAttribute("ASAR_Main_ADSR.sd/calibration_factors.1.ext_cal_fact");
 
         if (calibrationFactorsAttr == null) {
             throw new OperatorException("calibration_factors.1.ext_cal_fact not found");
         }
 
         newCalibrationConstant[0] = (double) calibrationFactorsAttr.getData().getElemFloat();
 
         calibrationFactorsAttr = ads.getAttribute("ASAR_Main_ADSR.sd/calibration_factors.2.ext_cal_fact");
 
         if (calibrationFactorsAttr == null) {
             throw new OperatorException("calibration_factors.2.ext_cal_fact not found");
         }
 
         newCalibrationConstant[1] = (double) calibrationFactorsAttr.getData().getElemFloat();
 
         if (Double.compare(newCalibrationConstant[0], 0.0) == 0 && Double.compare(newCalibrationConstant[1], 0.0) == 0) {
             throw new OperatorException("Calibration factors in metadata are zero");
         }
         //System.out.println("calibration factor for band 1 is " + calibrationFactor[0]);
         //System.out.println("calibration factor for band 2 is " + calibrationFactor[1]);
     }
 
     /**
      * Get orbit state vectors from the abstracted metadata.
      * @throws Exception The exceptions.
      */
     private void getOrbitStateVectors() throws Exception {
         orbitStateVectors = AbstractMetadata.getOrbitStateVectors(absRoot);
     }
 
     /**
      * Get first line time from the abstracted metadata (in days).
      * @throws Exception The exceptions.
      */
     private void getFirstLineTime() throws Exception {
         firstLineUTC = absRoot.getAttributeUTC(AbstractMetadata.first_line_time).getMJD(); // in days
     }
 
     /**
      * Get line time interval from the abstracted metadata (in days).
      * @throws Exception The exceptions.
      */
     private void getLineTimeInterval() throws Exception {
         lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / 86400.0; // s to day
     }
 
     /**
      * Get average scene height from abstracted metadata.
      * @throws Exception The exceptions.
      */
     private void getAverageSceneHeight() throws Exception {
         avgSceneHeight = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.avg_scene_height);
     }
 
     /**
      * Get the new antenna pattern gain from the latest XCA file available.
      * @throws Exception The exceptions.
      */
     private void getNewAntennaPattern() throws Exception {
 
         if (wideSwathProductFlag) {
 
             newRefElevationAngle = new double[5]; // reference elevation angles for 5 sub swathes
             newAntennaPatternWideSwath = new float[5][numOfGains]; // antenna pattern gain for 5 sub swathes
             getWideSwathAntennaPatternGainFromAuxData(
                     newXCAFilePath, mdsPolar[0], numOfGains, newRefElevationAngle, newAntennaPatternWideSwath);
 
         } else {
 
             newRefElevationAngle = new double[1]; // reference elevation angle for 1 swath
             newAntennaPatternSingleSwath = new float[2][numOfGains];  // antenna pattern gain for 2 bands
             getSingleSwathAntennaPatternGainFromAuxData(
                     newXCAFilePath,  swath, mdsPolar, numOfGains, newRefElevationAngle, newAntennaPatternSingleSwath);
         }
     }
 
     /**
      * Get reference elevation angle and antenna pattern gain from auxiliary file for single swath product.
      * @param fileName The auxiliary data file name
      * @param swath The swath name.
      * @param pol The polarizations for 2 bands.
      * @param numOfGains The number of gains for given swath and polarization (201).
      * @param refElevAngle The reference elevation angle array.
      * @param antPatArray The antenna pattern array.
      * @throws org.esa.beam.framework.gpf.OperatorException The IO exception.
      */
      public static void getSingleSwathAntennaPatternGainFromAuxData(
             String fileName, String swath, String[] pol, int numOfGains, double[] refElevAngle, float[][] antPatArray)
             throws OperatorException {
 
         final EnvisatAuxReader reader = new EnvisatAuxReader();
         try {
             reader.readProduct(fileName);
 
             String swathName;
             if (swath.contains("IS1")) {
                 swathName = "is1";
             } else if (swath.contains("IS2")) {
                 swathName = "is2";
             } else if (swath.contains("IS3")) {
                 swathName = "is3_ss2";
             } else if (swath.contains("IS4")) {
                 swathName = "is4_ss3";
             } else if (swath.contains("IS5")) {
                 swathName = "is5_ss4";
             } else if (swath.contains("IS6")) {
                 swathName = "is6_ss5";
             } else if (swath.contains("IS7")) {
                 swathName = "is7";
             } else {
                 throw new OperatorException("Invalid swath");
             }
 
             final String refElevAngleName = "elev_ang_" + swathName;
             final ProductData refElevAngleData = reader.getAuxData(refElevAngleName);
             refElevAngle[0] = (double) refElevAngleData.getElemFloat();
 
             final String patternName = "pattern_" + swathName;
             final ProductData patternData = reader.getAuxData(patternName);
             final float[] pattern = ((float[]) patternData.getElems());
             if (pattern.length != 804) {
                 throw new OperatorException("Incorret array length for " + patternName);
             }
 
             for (int i = 0; i < 2 && pol[i] != null && pol[i].length() != 0; i++) {
                 if (pol[i].contains("hh")) {
                     System.arraycopy(pattern, 0, antPatArray[i], 0, numOfGains);
                 } else if (pol[i].contains("vv")) {
                     System.arraycopy(pattern, numOfGains, antPatArray[i], 0, numOfGains);
                 } else if (pol[i].contains("hv")) {
                     System.arraycopy(pattern, 2 * numOfGains, antPatArray[i], 0, numOfGains);
                 } else if (pol[i].contains("vh")) {
                     System.arraycopy(pattern, 3 * numOfGains, antPatArray[i], 0, numOfGains);
                 }
             }
 
         } catch (IOException e) {
             throw new OperatorException(e);
         }
     }
 
     /**
      * Get reference elevation angle and antenna pattern gain from auxiliary file for wide swath product.
      * @param fileName The auxiliary data file name
      * @param pol The polarization.
      * @param numOfGains The number of gains for given swath and polarization (201).
      * @param refElevAngle The reference elevation angle array.
      * @param antPatArray The antenna pattern array.
      * @throws org.esa.beam.framework.gpf.OperatorException The IO exception.
      */
     public static void getWideSwathAntennaPatternGainFromAuxData(
             String fileName, String pol, int numOfGains, double[] refElevAngle, float[][] antPatArray)
             throws OperatorException {
 
         final EnvisatAuxReader reader = new EnvisatAuxReader();
         try {
             reader.readProduct(fileName);
 
             String[] swathName = {"ss1", "is3_ss2", "is4_ss3", "is5_ss4", "is6_ss5"};
 
             for (int i = 0; i < swathName.length; i++) {
 
                 // read elevation angles
                 final String refElevAngleName = "elev_ang_" + swathName[i];
                 final ProductData refElevAngleData = reader.getAuxData(refElevAngleName);
                 refElevAngle[i] = (double) refElevAngleData.getElemFloat();
 
                 // read antenna pattern gains
                 final String patternName = "pattern_" + swathName[i];
                 final ProductData patternData = reader.getAuxData(patternName);
                 final float[] pattern = ((float[]) patternData.getElems());
                 if (pattern.length != 804) {
                     throw new OperatorException("Incorret array length for " + patternName);
                 }
 
                 if (pol.contains("hh")) {
                     System.arraycopy(pattern, 0, antPatArray[i], 0, numOfGains);
                 } else if (pol.contains("vv")) {
                     System.arraycopy(pattern, numOfGains, antPatArray[i], 0, numOfGains);
                 } else if (pol.contains("hv")) {
                     System.arraycopy(pattern, 2 * numOfGains, antPatArray[i], 0, numOfGains);
                 } else if (pol.contains("vh")) {
                     System.arraycopy(pattern, 3 * numOfGains, antPatArray[i], 0, numOfGains);
                 }
             }
         } catch (IOException e) {
             throw new OperatorException(e);
         }
     }
 
     /**
      * Set power coefficient used in range spreading loss compensation computation for slant range images.
      */
     private void setRangeSpreadingLossCompPower() {
         rangeSpreadingCompPower = 3.0;
         if (productType.contains("ASA_APS_1")) {
             rangeSpreadingCompPower = 4.0;
         }
         halfRangeSpreadingCompPower = rangeSpreadingCompPower / 2.0;
     }
 
     /**
      * Update the metadata in the target product.
      */
     public void updateTargetProductMetadata() {
 
         final MetadataElement tgtAbsRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
 
         if (!srgrFlag) {
             AbstractMetadata.setAttribute(tgtAbsRoot, AbstractMetadata.SAMPLE_TYPE, "DETECTED");
         }
 
         if (applyAntennaPatternCorr) {
             AbstractMetadata.setAttribute(tgtAbsRoot, AbstractMetadata.ant_elev_corr_flag, 1);
         }
 
         if (applyRangeSpreadingCorr) {
             AbstractMetadata.setAttribute(tgtAbsRoot, AbstractMetadata.range_spread_comp_flag, 1);
         }
 
         AbstractMetadata.setAttribute(tgtAbsRoot, AbstractMetadata.abs_calibration_flag, 1);
         AbstractMetadata.setAttribute(tgtAbsRoot, AbstractMetadata.external_calibration_file, newXCAFileName);
         AbstractMetadata.setAttribute(tgtAbsRoot, AbstractMetadata.calibration_factor, newCalibrationConstant[0]);
     }
 
     /**
      * Called by the framework in order to compute a tile for the given target band.
      * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
      *
      * @param targetBand The target band.
      * @param targetTile The current tile associated with the target band to be computed.
      * @param pm         A progress monitor which should be used to determine computation cancelation requests.
      * @throws org.esa.beam.framework.gpf.OperatorException
      *          If an error occurs during computation of the target raster.
      */
     @Override
 
     public void computeTile(Band targetBand, Tile targetTile,
                             HashMap<String, String[]> targetBandNameToSourceBandName,
                             ProgressMonitor pm) throws OperatorException {
 
         final Rectangle targetTileRectangle = targetTile.getRectangle();
         final int x0 = targetTileRectangle.x;
         final int y0 = targetTileRectangle.y;
         final int w = targetTileRectangle.width;
         final int h = targetTileRectangle.height;
         //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);
 
         Band sourceBand1;
         Tile sourceRaster1;
         ProductData srcData1;
         ProductData srcData2 = null;
 
         final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
         if (srcBandNames.length == 1) {
             sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
             sourceRaster1 = getSourceTile(sourceBand1, targetTileRectangle, pm);
             srcData1 = sourceRaster1.getDataBuffer();
         } else {
             sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
             final Band sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
             sourceRaster1 = getSourceTile(sourceBand1, targetTileRectangle, pm);
             final Tile sourceRaster2 = getSourceTile(sourceBand2, targetTileRectangle, pm);
             srcData1 = sourceRaster1.getDataBuffer();
             srcData2 = sourceRaster2.getDataBuffer();
         }
 
         final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
 
         // copy band if unit is phase
         if(bandUnit == Unit.UnitType.PHASE) {
             targetTile.setRawSamples(sourceRaster1.getRawSamples());
             return;
         }
 
         final String pol = OperatorUtils.getPolarizationFromBandName(srcBandNames[0]);
         int prodBand = 0;
         if (pol != null && mdsPolar[1] != null && pol.contains(mdsPolar[1])) {
             prodBand = 1;
         }
 
         final ProductData trgData = targetTile.getDataBuffer();
 
         final int maxY = y0 + h;
         final int maxX = x0 + w;
 
         final float[] incidenceAnglesArray = new float[w];
         final float[] slantRangeTimeArray = new float[w];
 
         if (applyAntennaPatternCorr) {
             if (wideSwathProductFlag) {
                 computeWideSwathAntennaPatternForCurrentTile(x0, y0, w, h);
             } else {
                 computeSingleSwathAntennaPatternForCurrentTile(x0, y0, w, h, prodBand);
             }
         }
 
         double sigma, dn, i, q, time;
         final double theCalibrationFactor = newCalibrationConstant[prodBand];
 
         int index;
         for (int y = y0, yy = 0; y < maxY; ++y, ++yy) {
 
             incidenceAngle.getPixels(x0, y, w, 1,incidenceAnglesArray, pm, TiePointGrid.QUADRATIC);
 
             if (applyRangeSpreadingCorr) {
                 slantRangeTime.getPixels(x0, y, w, 1,slantRangeTimeArray, pm, TiePointGrid.QUADRATIC);
             }
 
             for (int x = x0, xx = 0; x < maxX; ++x, ++xx) {
                 index = sourceRaster1.getDataBufferIndex(x, y);
 
                 if (bandUnit == Unit.UnitType.AMPLITUDE) {
                     dn = srcData1.getElemDoubleAt(index);
                     sigma = dn*dn;
                 } else if (bandUnit == Unit.UnitType.INTENSITY) {
                     sigma = srcData1.getElemDoubleAt(index);
                 } else if (bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
                     i = srcData1.getElemDoubleAt(index);
                     q = srcData2.getElemDoubleAt(index);
                     sigma = i * i + q * q;
                 } else {
                     throw new OperatorException("ASAR Calibration: unhandled unit");
                 }
 
                 if (retroCalibrationFlag) { // remove old antenna pattern gain
                     sigma *= targetTileOldAntPat[yy][xx] * targetTileOldAntPat[yy][xx];
                 }
 
                 // apply calibration constant and incidence angle corrections
                 sigma *= Math.sin(incidenceAnglesArray[xx] * MathUtils.DTOR) / theCalibrationFactor;
 
                 if (applyRangeSpreadingCorr) { // apply range spreading loss compensation
                     time = slantRangeTimeArray[xx] / 1000000000.0; //convert ns to s
                     sigma *= Math.pow(time * halfLightSpeedByRefSlantRange, rangeSpreadingCompPower);
                 }
 
                 if (applyAntennaPatternCorr) { // apply antenna pattern correction
                     sigma /= targetTileNewAntPat[yy][xx] * targetTileNewAntPat[yy][xx];
                 }
 
                 if (outputImageScaleInDb) { // convert calibration result to dB
                     if (sigma < underFlowFloat) {
                         sigma = -underFlowFloat;
                     } else {
                         sigma = 10.0 * Math.log10(sigma);
                     }
                 }
 
                 trgData.setElemDoubleAt(targetTile.getDataBufferIndex(x, y), sigma);
             }
         }
     }
 
     /**
      * Compute antenna pattern for the middle row of the given tile for single swath product.
      * Here it is assumed that the elevation angles for pixels in the same column are the same.
      * @param x0 The x coordinate of the upper left point in the current tile.
      * @param y0 The y coordinate of the upper left point in the current tile.
      * @param w The width of the current tile.
      * @param h The height of the current tile.
      * @param band The band index.
      */
     private void computeSingleSwathAntennaPatternForCurrentTile(int x0, int y0, int w, int h, int band) {
 
         targetTileNewAntPat = new double[h][w];
         if (retroCalibrationFlag) {
             targetTileOldAntPat = new double[h][w];
         }
 
         for (int y = y0; y < y0 + h; y++) {
 
             final double zeroDopplerTime = firstLineUTC + y*lineTimeInterval;
 
             double satelitteHeight = computeSatelliteHeight(zeroDopplerTime, orbitStateVectors);
 
             AbstractMetadata.SRGRCoefficientList srgrConvParam = null;
             if (srgrFlag) {
                 srgrConvParam = getSRGRCoefficientsForARangeLine(zeroDopplerTime);
             }
 
             for (int x = x0; x < x0 + w; x++) {
 
                 final double slantRange = computeSlantRange(x, y, srgrConvParam); // in m
                 /*
                 final double earthRadius = computeEarthRadius(
                         latitude.getPixelFloat((float)x, (float)y, TiePointGrid.QUADRATIC),
                         longitude.getPixelFloat((float)x, (float)y, TiePointGrid.QUADRATIC)); // in m
                 */
                 int i = (int)((latMax - latitude.getPixelFloat((float)x, (float)y, TiePointGrid.QUADRATIC))/delLat + 0.5);
 
                 final double theta = computeElevationAngle(
                         slantRange, satelitteHeight, avgSceneHeight + earthRadius[i]); // in degree
 
                 targetTileNewAntPat[y - y0][x - x0] = computeAntPatGain(
                         theta, newRefElevationAngle[0], newAntennaPatternSingleSwath[band]);
 
                 if (retroCalibrationFlag) {
                     targetTileOldAntPat[y - y0][x - x0] = computeAntPatGain(
                             theta, oldRefElevationAngle[0], oldAntennaPatternSingleSwath[band]);
                 }
             }
         }
     }
 
     /**
      * Compute antenna pattern for the middle row of the given tile for wide swath product.
      * Here it is assumed that the elevation angles for pixels in the same column are the same.
      * @param x0 The x coordinate of the upper left point in the current tile.
      * @param y0 The y coordinate of the upper left point in the current tile.
      * @param w The width of the current tile.
      * @param h The height of the current tile.
      */
     private void computeWideSwathAntennaPatternForCurrentTile(int x0, int y0, int w, int h) {
 
         targetTileNewAntPat = new double[h][w];
         if (retroCalibrationFlag) {
             targetTileOldAntPat = new double[h][w];
         }
 
         for (int y = y0; y < y0 + h; y++) {
 
             final double zeroDopplerTime = firstLineUTC + y*lineTimeInterval;
 
             double satelitteHeight = computeSatelliteHeight(zeroDopplerTime, orbitStateVectors);
 
             AbstractMetadata.SRGRCoefficientList srgrConvParam = null;
             if (srgrFlag) {
                 srgrConvParam = getSRGRCoefficientsForARangeLine(zeroDopplerTime);
             }
 
             for (int x = x0; x < x0 + w; x++) {
 
                 final double slantRange = computeSlantRange(x, y, srgrConvParam); // in m
                 /*
                 final double earthRadius = computeEarthRadius(
                         latitude.getPixelFloat((float)x, (float)y, TiePointGrid.QUADRATIC),
                         longitude.getPixelFloat((float)x, (float)y, TiePointGrid.QUADRATIC)); // in m
                 */
                 int i = (int)((latMax - latitude.getPixelFloat((float)x, (float)y, TiePointGrid.QUADRATIC))/delLat + 0.5);
 
                 final double theta = computeElevationAngle(
                                                 slantRange, satelitteHeight, avgSceneHeight + earthRadius[i]); // in degree
 
                 int subSwathIndex = findSubSwath(theta, newRefElevationAngle);
 
                 targetTileNewAntPat[y - y0][x - x0] = computeAntPatGain(
                         theta, newRefElevationAngle[subSwathIndex], newAntennaPatternWideSwath[subSwathIndex]);
 
                 if (retroCalibrationFlag) {
                     subSwathIndex = findSubSwath(theta, oldRefElevationAngle);
 
                     targetTileOldAntPat[y - y0][x - x0] = computeAntPatGain(
                             theta, oldRefElevationAngle[subSwathIndex], oldAntennaPatternWideSwath[subSwathIndex]);
                 }
             }
         }
     }
 
     /**
      * Find the sub swath index for given elevation angle.
      * @param theta The elevation angle.
      * @param refElevationAngle The reference elevation array.
      * @return The sub swath index.
      */
     public static int findSubSwath(double theta, double[] refElevationAngle) {
         // The method below finds the nearest reference elevation angle to the given elevation angle theta.
         // The method is equivalent to the one proposed by Romain in his email dated April 28, 2009, in which
         // middle point of the overlapped area of two adjacent sub swathes is used as boundary of sub swath.
         int idx = -1;
         double min = 360.0;
         for (int i = 0 ; i < refElevationAngle.length; i++) {
             double d = Math.abs(theta - refElevationAngle[i]);
             if (d < min) {
                 min = d;
                 idx = i;
             }
         }
         return idx;
     }
 
     /**
      * Compute antenna pattern gains for the given elevation angle using linear interpolation.
      *
      * @param elevAngle The elevation angle (in degree) of a given pixel.
      * @param refElevationAngle The reference elevation angle (in degree).
      * @param antPatArray The antenna pattern array.
      * @return The antenna pattern gain (in linear scale).
      */
     public static double computeAntPatGain(double elevAngle, double refElevationAngle, float[] antPatArray) {
 
         final double delta = 0.05;
         int k0 = (int) ((elevAngle - refElevationAngle + 5.0) / delta);
         if (k0 < 0) {
             k0 = 0;
         } else if (k0 >= antPatArray.length - 1) {
             k0 = antPatArray.length - 2;
         }
         final double theta0 = refElevationAngle - 5.0 + k0*delta;
         final double theta1 = theta0 + delta;
         final double gain0 = Math.pow(10, (double) antPatArray[k0] / 10.0); // convert dB to linear scale
         final double gain1 = Math.pow(10, (double) antPatArray[k0+1] / 10.0);
         final double mu = (elevAngle - theta0) / (theta1 - theta0);
 
         return org.esa.nest.util.MathUtils.interpolationLinear(gain0, gain1, mu);
     }
 
     //============================================================================================================
 
     /**
      * Get the SRGR coefficients for given zero Doppler time.
      * @param zeroDopplerTime The zero Doppler time in MJD.
      * @return The SRGR coefficients.
      */
     private AbstractMetadata.SRGRCoefficientList getSRGRCoefficientsForARangeLine(final double zeroDopplerTime) {
 
         if (srgrConvParams.length == 1) {
             return srgrConvParams[0];
         }
 
         int idx = 0;
         for (int i = 0; i < srgrConvParams.length && zeroDopplerTime >= srgrConvParams[i].timeMJD; i++) {
             idx = i;
         }
 
         if (idx == srgrConvParams.length - 1) {
             idx--;
         }
 
         AbstractMetadata.SRGRCoefficientList srgrConvParam = new AbstractMetadata.SRGRCoefficientList();
         srgrConvParam.timeMJD = zeroDopplerTime;
         srgrConvParam.ground_range_origin = srgrConvParams[idx].ground_range_origin;
         srgrConvParam.coefficients = new double[srgrConvParams[idx].coefficients.length];
         final double mu = (zeroDopplerTime - srgrConvParams[idx].timeMJD) /
                           (srgrConvParams[idx+1].timeMJD - srgrConvParams[idx].timeMJD);
 
         for (int i = 0; i < srgrConvParam.coefficients.length; i++) {
             srgrConvParam.coefficients[i] = org.esa.nest.util.MathUtils.interpolationLinear(
                     srgrConvParams[idx].coefficients[i], srgrConvParams[idx+1].coefficients[i], mu);
         }
         return srgrConvParam;
     }
 
     /**
      * Compute slant range for given pixel.
      * @param x The x coordinate of the pixel in the source image.
      * @param y The y coordinate of the pixel in the source image.
      * @param srgrConvParam The SRGR coefficients.
      * @return The slant range (in meters).
      */
     private double computeSlantRange(int x, int y, AbstractMetadata.SRGRCoefficientList srgrConvParam) {
 
         if (srgrFlag) { // for ground detected product, compute slant range from SRGR coefficients
             return org.esa.nest.util.MathUtils.computePolynomialValue(
                         x*rangeSpacing + srgrConvParam.ground_range_origin, srgrConvParam.coefficients);
 
         } else { // for slant range product, compute slant range from slant range time
 
             final double time = slantRangeTime.getPixelFloat((float)x, (float)y, TiePointGrid.QUADRATIC) / 1000000000.0; //convert ns to s
             return time * Constants.halfLightSpeed; // in m
         }
     }
     
     /**
      * Compute distance from satelitte to the Earth centre (in meters).
      * @param zeroDopplerTime The zero Doppler time (in days).
      * @param orbitStateVectors The orbit state vectors.
      * @return The distance.
      */
     public static double computeSatelliteHeight(double zeroDopplerTime, AbstractMetadata.OrbitStateVector[] orbitStateVectors) {
 
         // todo should use the 3rd state vector as suggested by the doc?
         int idx = 0;
         for (int i = 0; i < orbitStateVectors.length && zeroDopplerTime >= orbitStateVectors[i].time.getMJD(); i++) {
             idx = i;
         }
         final double xPos = orbitStateVectors[idx].x_pos;
         final double yPos = orbitStateVectors[idx].y_pos;
         final double zPos = orbitStateVectors[idx].z_pos;
         return Math.sqrt(xPos*xPos + yPos*yPos + zPos*zPos);
     }
 
     /**
      * Compute earth radius for all range lines (in m).
      */
     private void computeEarthRadius() {
 
         final GeoCoding geoCoding = sourceProduct.getGeoCoding();
         if(geoCoding == null) {
             throw new OperatorException("Product does not contain a geocoding");
         }
         final GeoPos geoPosFirstNear = geoCoding.getGeoPos(new PixelPos(0,0), null);
         final GeoPos geoPosFirstFar = geoCoding.getGeoPos(new PixelPos(sourceProduct.getSceneRasterWidth()-1,0), null);
         final GeoPos geoPosLastNear = geoCoding.getGeoPos(new PixelPos(0,sourceProduct.getSceneRasterHeight()-1), null);
         final GeoPos geoPosLastFar = geoCoding.getGeoPos(new PixelPos(sourceProduct.getSceneRasterWidth()-1,
                                                                       sourceProduct.getSceneRasterHeight()-1), null);
 
         final double[] lats  = {geoPosFirstNear.getLat(), geoPosFirstFar.getLat(), geoPosLastNear.getLat(), geoPosLastFar.getLat()};
         latMin = 90.0;
         latMax = -90.0;
         for (double lat : lats) {
             if (lat < latMin) {
                 latMin = lat;
             }
             if (lat > latMax) {
                 latMax = lat;
             }
         }
 
         final double minSpacing = Math.min(rangeSpacing, azimuthSpacing);
         double minAbsLat;
         if (latMin*latMax > 0) {
             minAbsLat = Math.min(Math.abs(latMin), Math.abs(latMax)) * org.esa.beam.util.math.MathUtils.DTOR;
         } else {
             minAbsLat = 0.0;
         }
         delLat = minSpacing / MeanEarthRadius * org.esa.beam.util.math.MathUtils.RTOD;
         final double delLon = minSpacing / (MeanEarthRadius*Math.cos(minAbsLat)) * org.esa.beam.util.math.MathUtils.RTOD;
         delLat = Math.min(delLat, delLon);
 
         final int h = (int)((latMax - latMin)/delLat) + 1;
 
         earthRadius = new double[h + 1];
         for (int i = 0; i <= h; i++) {
             earthRadius[i] = computeEarthRadius((float)(latMax - i*delLat), 0.0f);
         }
     }
 
     /**
      * Compute Earth radius (in meters) for given pixel in source image.
      * @param lat The latitude of a given pixel in source image.
      * @param lon The longitude of a given pixel in source image.
      * @return The Earth radius.
      */
     private static double computeEarthRadius(float lat, float lon) {
         final double[] xyz = new double[3];
         GeoUtils.geo2xyz(lat, lon, 0.0, xyz, GeoUtils.EarthModel.WGS84);
         return Math.sqrt(xyz[0]*xyz[0] + xyz[1]*xyz[1] + xyz[2]*xyz[2]);
     }
 
 
     /**
      * Compute elevation angle (in degree).
      * @param slantRange The slant range (in meters).
      * @param satelliteHeight The distance from satelitte to the Earth centre (in meters).
      * @param sceneToEarthCentre The distance from the backscatter element to the Earth centre (in meters).
      * @return The elevation angle.
      */
     public static double computeElevationAngle(double slantRange, double satelliteHeight, double sceneToEarthCentre) {
 
         return Math.acos((slantRange*slantRange + satelliteHeight*satelliteHeight -
                (sceneToEarthCentre)*(sceneToEarthCentre))/(2*slantRange*satelliteHeight))*MathUtils.RTOD;
     }
 
     /**
     * Set the XCA file name.
     * This function is used by unit test only.
     * @param xcaFileName The XCA file name.
     */
     public void setExternalAntennaPatternFile(String xcaFileName) {
 
         String path = Settings.instance().get("AuxData/envisatAuxDataPath") + File.separator + xcaFileName;
         externalAuxFile = new File(path);
         if (!externalAuxFile.exists()) {
             throw new OperatorException("External antenna pattern file for unit test does not exist");
         }
     }
 
     /**
      * Gets a {@link Tile} for a given band and rectangle.
      *
      * @param rasterDataNode the raster data node of a data product,
      *                       e.g. a {@link org.esa.beam.framework.datamodel.Band Band} or
      *                       {@link org.esa.beam.framework.datamodel.TiePointGrid TiePointGrid}.
      * @param rectangle      the raster rectangle in pixel coordinates
      * @param pm             The progress monitor passed into the
      *                       the computeTile method or the computeTileStack method.
      * @return a tile.
      * @throws OperatorException if the tile request cannot be processed
      */
     public static Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
         return OperatorContext.getSourceTile(rasterDataNode, rectangle, pm);
     }
 
     //==================================== pixel calibration used by RD ======================================
 
     /**
      * Remove the antenna pattern compensation and range spreading loss applied to the pixel.
      * @param x The x coordinate of the pixel in the source image.
      * @param y The y coordinate of the pixel in the source image.
      * @param v The pixel value.
      * @param bandPolar The polarization of the source band.
      * @param bandUnit The source band unit.
      * @param subSwathIndex The sub swath index for current pixel for wide swath product case.
      * @return The pixel value with antenna pattern compensation and range spreading loss correction removed.
      */
     public double applyRetroCalibration(int x, int y, double v, int bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {
 
         if (!retroCalibrationFlag) {
             return v;
         }
         
         final double zeroDopplerTime = firstLineUTC + y*lineTimeInterval;
 
         double satelitteHeight = computeSatelliteHeight(zeroDopplerTime, orbitStateVectors);
 
         AbstractMetadata.SRGRCoefficientList srgrConvParam = getSRGRCoefficientsForARangeLine(zeroDopplerTime);
 
         final double slantRange = computeSlantRange(x, y, srgrConvParam); // in m
 
         int i = (int)((latMax - latitude.getPixelFloat((float)x, (float)y, TiePointGrid.QUADRATIC))/delLat + 0.5);
         if (i < 0) {
             i = 0;
         }
 
         final double elevationAngle = computeElevationAngle(slantRange, satelitteHeight, avgSceneHeight+earthRadius[i]);
 
         double gain = 0.0;
         if (wideSwathProductFlag) {
             gain = getAntennaPatternGain(
                     elevationAngle, bandPolar, oldRefElevationAngle, oldAntennaPatternWideSwath, true, subSwathIndex);
         } else {
             gain = computeAntPatGain(elevationAngle, oldRefElevationAngle[0], oldAntennaPatternSingleSwath[bandPolar]);
         }
 
         if (bandUnit == Unit.UnitType.AMPLITUDE) {
             return v*gain*Math.pow(refSlantRange / slantRange, halfRangeSpreadingCompPower); // amplitude
         } else if (bandUnit == Unit.UnitType.INTENSITY || bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
             return v*gain*gain*Math.pow(refSlantRange / slantRange, rangeSpreadingCompPower); // intensity
         } else if (bandUnit == Unit.UnitType.INTENSITY_DB) {
             return 10.0*Math.log10(Math.pow(10, v/10.0)*gain*gain*Math.pow(refSlantRange/slantRange, rangeSpreadingCompPower));
         } else {
             throw new OperatorException("Uknown band unit");
         }
     }
 
     /**
      * Get antenna pattern gain value for given elevation angle.
      * @param elevationAngle The elevation angle (in degree).
      * @param bandPolar The source band polarization index.
      * @param refElevationAngle The reference elevation angles for different swathes or sub swathes.
      * @param antennaPattern The antenna pattern array. For single swath product, it contains two 201-length arrays
      *                       corresponding to the two bands of different polarizations. For wide swath product, it
      *                       contains five 201-length arrays with each for a sub swath.
      * @param compSubSwathIdx The boolean flag indicating if sub swath index should be computed.
      * @param subSwathIndex The sub swath index for current pixel for wide swath product case.
      * @return The antenna pattern gain value.
      */
     private static double getAntennaPatternGain(double elevationAngle, int bandPolar, double[] refElevationAngle,
                                          float[][] antennaPattern, boolean compSubSwathIdx, int[] subSwathIndex) {
 
         if (refElevationAngle.length == 1) { // single swath
 
             return computeAntPatGain(elevationAngle, refElevationAngle[0], antennaPattern[bandPolar]);
 
         } else { // wide swath
 
             if (compSubSwathIdx || subSwathIndex[0] == INVALID_SUB_SWATH_INDEX) {
                 subSwathIndex[0] = findSubSwath(elevationAngle, refElevationAngle);
             }
 
             return computeAntPatGain(
                     elevationAngle, refElevationAngle[subSwathIndex[0]], antennaPattern[subSwathIndex[0]]);
         }
     }
 
     /**
      * Apply calibrations to the given point. The following calibrations are included: calibration constant,
      * antenna pattern compensation, range spreading loss correction and incidence angle correction.
      * @param v The pixel value.
      * @param slantRange The slant range (in m).
      * @param satelliteHeight The distance from satellite to earth centre (in m).
      * @param sceneToEarthCentre The distance from the backscattering element position to earth centre (in m).
      * @param localIncidenceAngle The local incidence angle (in degrees).
      * @param bandPolar The source band polarization index.
      * @param bandUnit The source band unit.
      * @param subSwathIndex The sub swath index for current pixel for wide swath product case.
      * @return The calibrated pixel value.
      */
     public double applyCalibration(
             final double v, final double slantRange, final double satelliteHeight, final double sceneToEarthCentre,
             final double localIncidenceAngle, final int bandPolar, final Unit.UnitType bandUnit, int[] subSwathIndex) {
 
         double sigma = 0.0;
         if (bandUnit == Unit.UnitType.AMPLITUDE) {
             sigma = v*v;
         } else if (bandUnit == Unit.UnitType.INTENSITY || bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY) {
             sigma = v;
         } else if (bandUnit == Unit.UnitType.INTENSITY_DB) {
             sigma = Math.pow(10, v/10.0); // convert dB to linear scale
         } else {
             throw new OperatorException("Uknown band unit");
         }
 
         if (multilookFlag) { // calibration constant and incidence angle corrections only
             return sigma / newCalibrationConstant[bandPolar] *
                    Math.sin(Math.abs(localIncidenceAngle)*org.esa.beam.util.math.MathUtils.DTOR);
         }
 
         final double elevationAngle = computeElevationAngle(slantRange, satelliteHeight, sceneToEarthCentre); // in degrees
 
         double gain;
         if (wideSwathProductFlag) {
             gain = getAntennaPatternGain(
                     elevationAngle, bandPolar, newRefElevationAngle, newAntennaPatternWideSwath, false, subSwathIndex);
         } else {
             gain = computeAntPatGain(
                     elevationAngle, newRefElevationAngle[0], newAntennaPatternSingleSwath[bandPolar]);
         }
 
         return sigma / newCalibrationConstant[bandPolar] / (gain*gain) *
                Math.pow(slantRange/refSlantRange, rangeSpreadingCompPower) *
                Math.sin(Math.abs(localIncidenceAngle)*org.esa.beam.util.math.MathUtils.DTOR);
     }
 
 }
