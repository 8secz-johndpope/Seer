 /*
  * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  *
  * For further details of the Gene Expression Atlas project, including source code,
  * downloads and documentation, please see:
  *
  * http://gxa.github.com/gxa
  */
 
 package uk.ac.ebi.gxa.netcdf.reader;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import ucar.ma2.*;
 import ucar.nc2.NetcdfFile;
 import ucar.nc2.Variable;
 import ucar.nc2.dataset.NetcdfDataset;
 import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.*;
 
 /**
  * An object that proxies an Atlas NetCDF file and provides convenience methods for accessing the data from within. This
  * class should be used when trying to read data on-the-fly out of a NetCDF directly.
  * <p/>
  * The NetCDFs for Atlas are structured as follows:
  * <pre>
  *    long AS(AS) ;
  *    long BS(BS) ;
  *    int BS2AS(BS, AS) ;
  *    long DE(DE) ;
  *    long GN(GN) ;
  *    int DE2GN(DE, GN) ;
  *    char EF(EF, EFlen) ;
  *    char EFV(EF, AS, EFlen) ;
  *    char uEFV(uEFV, EFlen) ;
  *    int uEFVnum(EF) ;
  *    char SC(SC, SClen) ;
  *    char SCV(SC, BS, SClen) ;
  *    float BDC(DE, AS) ;
  *    float PVAL(DE, uEFV) ;
  *    float TSTAT(DE, uEFV) ;
  * </pre>
  *
  * @author Tony Burdett
  * @date 11-Nov-2009
  */
 public class NetCDFProxy {
     // this is false if opening a connection to the netcdf file failed
     private boolean proxied;
     private String pathToNetCDF;
 
     private NetcdfFile netCDF;
 
     private final Logger log = LoggerFactory.getLogger(getClass());
     private long experimentId;
 
     public NetCDFProxy(File netCDF) {
         this.pathToNetCDF = netCDF.getAbsolutePath();
         try {
             this.netCDF = NetcdfDataset.acquireFile(netCDF.getAbsolutePath(), null);
             this.experimentId = Long.valueOf(netCDF.getName().split("_")[0]);
             proxied = true;
         }
         catch (IOException e) {
             proxied = false;
         }
     }
 
     /**
      * eg. pathToNetCDF: ~/Documents/workspace/atlas-data/netCDF/223403015_221532256.nc
      * @return fileName (i.e. substring after the last '/', e.g. "223403015_221532256.nc")
      */
     public String getId(){
         String[] parts = pathToNetCDF.split(File.separator);
         return parts[parts.length - 1];
     }
 
     public String getExperiment() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         return netCDF.findGlobalAttribute("experiment_accession").getStringValue();
     }
 
     public String getArrayDesignAccession() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         return netCDF.findGlobalAttribute("ADaccession").getStringValue();
     }
 
     public long getArrayDesignID() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         if (netCDF.findGlobalAttribute("ADid") != null) {
             Number value = netCDF.findGlobalAttribute("ADid").getNumericValue();
             if (value != null)
                 return value.longValue();
             return -1;
         }
 
         return -1;
     }
 
     public long[] getAssays() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         if (netCDF.findVariable("AS") == null) {
             return new long[0];
         } else {
             return (long[]) netCDF.findVariable("AS").read().get1DJavaArray(long.class);
         }
     }
 
     public long[] getSamples() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         if (netCDF.findVariable("BS") == null) {
             return new long[0];
         } else {
             return (long[]) netCDF.findVariable("BS").read().get1DJavaArray(long.class);
         }
     }
 
     public int[][] getSamplesToAssays() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         // read BS2AS
         if (netCDF.findVariable("BS2AS") == null) {
             return new int[0][0];
         } else {
 
             Array bs2as = netCDF.findVariable("BS2AS").read();
             // copy to an int array - BS2AS is 2d array so this should drop out
             return (int[][]) bs2as.copyToNDJavaArray();
         }
     }
 
     public long[] getDesignElements() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         if (netCDF.findVariable("DE") == null) {
             return new long[0];
         } else {
             return (long[]) netCDF.findVariable("DE").read().get1DJavaArray(long.class);
         }
     }
 
     /**
      * Gets the array of gene IDs from this NetCDF
      *
      * @return an long[] representing the one dimensional array of gene identifiers
      * @throws IOException if accessing the NetCDF failed
      */
     public long[] getGenes() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         if (netCDF.findVariable("GN") == null) {
             return new long[0];
         } else {
             return (long[]) netCDF.findVariable("GN").read().get1DJavaArray(long.class);
         }
     }
 
     public String[] getDesignElementAccessions() throws IOException {
         if (!proxied)
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
 
         if (netCDF.findVariable("DEacc") == null) {
             return new String[0];
         } else {
             ArrayChar deacc = (ArrayChar) netCDF.findVariable("DEacc").read();
             ArrayChar.StringIterator si = deacc.getStringIterator();
             String[] result = new String[deacc.getShape()[0]];
             for (int i = 0; i < result.length && si.hasNext(); ++i)
                 result[i] = si.next();
             return result;
         }
     }
 
     public String[] getFactors() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         if (netCDF.findVariable("EF") == null) {
             return new String[0];
         } else {
             // create a array of characters from the "EF" dimension
             ArrayChar efs = (ArrayChar) netCDF.findVariable("EF").read();
             // convert to a string array and return
             Object[] efsArray = (Object[]) efs.make1DStringArray().get1DJavaArray(String.class);
             String[] result = new String[efsArray.length];
             for (int i = 0; i < efsArray.length; i++) {
                 result[i] = (String) efsArray[i];
                 if (result[i].startsWith("ba_"))
                     result[i] = result[i].substring(3);
             }
             return result;
         }
     }
 
     public String[] getFactorValues(String factor) throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         // get all factors
         String[] efs = getFactors();
 
         // iterate over factors to find the index of the one we're interested in
         int efIndex = 0;
         boolean efFound = false;
         for (String ef : efs) {
             // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
             if (factor.matches("(ba_)?" + ef)) {
                 efFound = true;
                 break;
             } else {
                 efIndex++;
             }
         }
 
         // if we couldn't match the factor we're looking for, return empty array
         if (!efFound) {
             log.warn("Couldn't locate index of " + factor + " in " + pathToNetCDF);
             return new String[0];
         }
 
         // if the EFV variable is empty
         if (netCDF.findVariable("EFV") == null) {
             return new String[0];
         } else {
             // now we have index of our ef, so take a read from efv for this index
             Array efvs = netCDF.findVariable("EFV").read();
             // slice this array on dimension '0' (this is EF dimension), retaining only these efvs ordered by assay
             ArrayChar ef_efv = (ArrayChar) efvs.slice(0, efIndex);
 
             // convert to a string array and return
             Object[] ef_efvArray = (Object[]) ef_efv.make1DStringArray().get1DJavaArray(String.class);
             String[] result = new String[ef_efvArray.length];
             for (int i = 0; i < ef_efvArray.length; i++) {
                 result[i] = (String) ef_efvArray[i];
             }
             return result;
         }
     }
 
     public String[] getUniqueFactorValues() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         // create a array of characters from the "SC" dimension
         if (netCDF.findVariable("uEFV") == null) {
             return new String[0];
         } else {
             ArrayChar uefv = (ArrayChar) netCDF.findVariable("uEFV").read();
 
             // convert to a string array and return
             Object[] uefvArray = (Object[]) uefv.make1DStringArray().get1DJavaArray(String.class);
             String[] result = new String[uefvArray.length];
             for (int i = 0; i < uefvArray.length; i++) {
                 result[i] = (String) uefvArray[i];
             }
             return result;
         }
     }
 
     public String[] getCharacteristics() throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         if (netCDF.findVariable("SC") == null) {
             return new String[0];
         } else {
             // create a array of characters from the "SC" dimension
             ArrayChar scs = (ArrayChar) netCDF.findVariable("SC").read();
             // convert to a string array and return
             Object[] scsArray = (Object[]) scs.make1DStringArray().get1DJavaArray(String.class);
             String[] result = new String[scsArray.length];
             for (int i = 0; i < scsArray.length; i++) {
                 result[i] = (String) scsArray[i];
                 if (result[i].startsWith("bs_"))
                     result[i] = result[i].substring(3);
             }
             return result;
         }
     }
 
     public String[] getCharacteristicValues(String characteristic) throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         // get all characteristics
         String[] scs = getCharacteristics();
 
         // iterate over factors to find the index of the one we're interested in
         int scIndex = 0;
         boolean scFound = false;
         for (String sc : scs) {
             // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
             if (characteristic.matches("(bs_)?" + sc)) {
                 scFound = true;
                 break;
             } else {
                 scIndex++;
             }
         }
 
         // if we couldn't match the characteristic we're looking for, return empty array
         if (!scFound) {
             log.error("Couldn't locate index of " + characteristic + " in " + pathToNetCDF);
             return new String[0];
         }
 
         if (netCDF.findVariable("SCV") == null) {
             return new String[0];
         } else {
             // now we have index of our sc, so take a read from scv for this index
             ArrayChar scvs = (ArrayChar) netCDF.findVariable("SCV").read();
             // slice this array on dimension '0' (this is SC dimension), retaining only these scvs ordered by sample
             ArrayChar sc_scv = (ArrayChar) scvs.slice(0, scIndex);
             // convert to a string array and return
             Object[] sc_scvArray = (Object[]) sc_scv.make1DStringArray().get1DJavaArray(String.class);
             String[] result = new String[sc_scvArray.length];
             for (int i = 0; i < sc_scvArray.length; i++) {
                 result[i] = (String) sc_scvArray[i];
             }
             return result;
         }
     }
 
     /**
      * Gets a single row from the expression data matrix representing all expression data for a single design element.
      * This is obtained by retrieving all data from the given row in the expression matrix, where the design element
      * index supplied is the row number.  As the expression value matrix has the same ordering as the design element
      * array, you can iterate over the design element array to retrieve the index of the row you want to fetch.
      *
      * @param designElementIndex the index of the design element which we're interested in fetching data for
      * @return the double array representing expression values for this design element
      * @throws IOException if the NetCDF could not be accessed
      */
     public float[] getExpressionDataForDesignElementAtIndex(int designElementIndex) throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         Variable bdcVariable = netCDF.findVariable("BDC");
         if (bdcVariable == null) {
             return new float[0];
         } else {
             int[] bdcShape = bdcVariable.getShape();
             int[] origin = {designElementIndex, 0};
             int[] size = new int[]{1, bdcShape[1]};
             try {
                 return (float[]) bdcVariable.read(origin, size).get1DJavaArray(float.class);
             }
             catch (InvalidRangeException e) {
                 log.error("Error reading from NetCDF - invalid range at " + designElementIndex + ": " + e.getMessage());
                 throw new IOException("Failed to read expression data for design element at " + designElementIndex +
                         ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
             }
         }
     }
 
     /**
      * Gets a single column from the expression data matrix representing all expression data for a single assay. This is
      * obtained by retrieving all data from the given column in the expression matrix, where the assay index supplied is
      * the column number.  As the expression value matrix has the same ordering as the assay array, you can iterate over
      * the assay array to retrieve the index of the column you want to fetch.
      *
      * @param assayIndex the index of the assay which we're interested in fetching data for
      * @return the double array representing expression values for this assay
      * @throws IOException if the NetCDF could not be accessed
      */
     public float[] getExpressionDataForAssay(int assayIndex) throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         Variable bdcVariable = netCDF.findVariable("BDC");
 
         if (bdcVariable == null) {
             return new float[0];
         } else {
             int[] bdcShape = bdcVariable.getShape();
             int[] origin = {0, assayIndex};
             int[] size = new int[]{bdcShape[0], 1};
             try {
                 return (float[]) bdcVariable.read(origin, size).get1DJavaArray(float.class);
             }
             catch (InvalidRangeException e) {
                 log.error("Error reading from NetCDF - invalid range at " + assayIndex + ": " + e.getMessage());
                 throw new IOException("Failed to read expression data for assay at " + assayIndex +
                         ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
             }
         }
     }
 
     public float[] getPValuesForDesignElement(int designElementIndex) throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         Variable pValVariable = netCDF.findVariable("PVAL");
         if (pValVariable == null) {
             return new float[0];
         } else {
             int[] pValShape = pValVariable.getShape();
             int[] origin = {designElementIndex, 0};
             int[] size = new int[]{1, pValShape[1]};
             try {
                 return (float[]) pValVariable.read(origin, size).get1DJavaArray(float.class);
             }
             catch (InvalidRangeException e) {
                 log.trace("Error reading from NetCDF - invalid range at " + designElementIndex + ": " + e.getMessage());
                 throw new IOException("Failed to read p-value data for design element at " + designElementIndex +
                         ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
             }
         }
     }
 
     public float[] getPValuesForUniqueFactorValue(int uniqueFactorValueIndex) throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         Variable pValVariable = netCDF.findVariable("PVAL");
 
         if (pValVariable == null) {
             return new float[0];
         } else {
             int[] pValShape = pValVariable.getShape();
             int[] origin = {0, uniqueFactorValueIndex};
             int[] size = new int[]{pValShape[0], 1};
             try {
                 return (float[]) pValVariable.read(origin, size).get1DJavaArray(float.class);
             }
             catch (InvalidRangeException e) {
                 log.error("Error reading from NetCDF - invalid range at " + uniqueFactorValueIndex + ": " +
                         e.getMessage());
                 throw new IOException("Failed to read p-value data for unique factor value at " +
                         uniqueFactorValueIndex + ": caused by " + e.getClass().getSimpleName() + " " +
                         "[" + e.getMessage() + "]");
             }
         }
     }
 
     public float[] getTStatisticsForDesignElement(int designElementIndex) throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         Variable tStatVariable = netCDF.findVariable("TSTAT");
         if (tStatVariable == null) {
             return new float[0];
         } else {
             int[] tStatShape = tStatVariable.getShape();
             int[] origin = {designElementIndex, 0};
             int[] size = new int[]{1, tStatShape[1]};
             try {
                 return (float[]) tStatVariable.read(origin, size).get1DJavaArray(float.class);
             }
             catch (InvalidRangeException e) {
                 log.error("Error reading from NetCDF - invalid range at " + designElementIndex + ": " + e.getMessage());
                 throw new IOException("Failed to read t-statistic data for design element at " + designElementIndex +
                         ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
             }
         }
     }
 
     public float[] getTStatisticsForUniqueFactorValue(int uniqueFactorValueIndex) throws IOException {
         if (!proxied) {
             throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
         }
 
         Variable tStatVariable = netCDF.findVariable("TSTAT");
 
         if (tStatVariable == null) {
             return new float[0];
         } else {
             int[] tStatShape = tStatVariable.getShape();
             int[] origin = {0, uniqueFactorValueIndex};
             int[] size = new int[]{tStatShape[0], 1};
             try {
                 return (float[]) tStatVariable.read(origin, size).get1DJavaArray(float.class);
             }
             catch (InvalidRangeException e) {
                 log.error("Error reading from NetCDF - invalid range at " + uniqueFactorValueIndex + ": " +
                         e.getMessage());
                 throw new IOException("Failed to read t-statistic data for unique factor value at " +
                         uniqueFactorValueIndex + ": caused by " + e.getClass().getSimpleName() + " " +
                         "[" + e.getMessage() + "]");
             }
         }
     }
 
     /**
      * Closes the proxied NetCDF file
      * @throws IOException
      */
     public void close() throws IOException {
         if (this.netCDF != null)
             this.netCDF.close();
     }
 
     public Map<Long, List<ExpressionAnalysis>> getExpressionAnalysesForGenes() throws IOException {
 
         final Map<Long, List<ExpressionAnalysis>> geas = new HashMap<Long, List<ExpressionAnalysis>>();
         final String[] uEFVs = getUniqueFactorValues();
 
         final long[] genes = getGenes();
         final long[] des = getDesignElements();
         final ArrayFloat pval = (ArrayFloat) netCDF.findVariable("PVAL").read();
         final ArrayFloat tstat = (ArrayFloat) netCDF.findVariable("TSTAT").read();
 
         IndexIterator pvalIter = pval.getIndexIterator();
         IndexIterator tstatIter = tstat.getIndexIterator();
 
         for (int i = 0; i < genes.length; i++) {
             List<ExpressionAnalysis> eas;
 
             if (0 != genes[i] &&
                     !geas.containsKey(genes[i]))
                 eas = new LinkedList<ExpressionAnalysis>();
             else
                 eas = geas.get(genes[i]);
 
             for (int j = 0; j < uEFVs.length; j++) {
                 if (!pvalIter.hasNext() || !tstatIter.hasNext()) {
                     throw new RuntimeException("Unexpected end of expression analytics data in " + pathToNetCDF);
                 }
 
                 float pval_ = pvalIter.getFloatNext();
                 float tstat_ = tstatIter.getFloatNext();
 
                 if (genes[i] == 0) continue; // skip geneid = 0
 
                 ExpressionAnalysis ea = new ExpressionAnalysis();
 
                 String[] efefv = uEFVs[j].split("\\|\\|");
 
                 ea.setDesignElementID(des[i]);
                 ea.setEfName(efefv[0]);
                 ea.setEfvName(efefv.length == 2 ? efefv[1] : "");
                 ea.setPValAdjusted(pval_);
                 ea.setTStatistic(tstat_);
                 ea.setExperimentID(getExperimentId());
 
                 eas.add(ea);
             }
 
             if (genes[i] != 0)  // skip geneid = 0
                 geas.put(genes[i], eas);
         }
 
         return geas;
     }
 
     public long getExperimentId() {
         return experimentId;
     }
 
     public void setExperimentId(long experimentId) {
         this.experimentId = experimentId;
     }
 
 
     /**
      * For each gene in the keySet() of geneIdsToDEIndexes, and each efv in uEF_EFVs, find
      * the design element with a minPvalue and store it as an ExpressionAnalysis object in
      * geneIdsToEfToEfvToEA if the minPvalus found in this proxy is better than the one already in
      * geneIdsToEfToEfvToEA. This method cane be called for multiple proxies in turn, accumulating
      * data with the best pValues across all proxies.
      *
      * @param geneIdsToDEIndexes   geneId -> list of desinglemenet indexes containing data for that gene
      * @param geneIdsToEfToEfvToEA geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
      *                             Note that ea contains proxyId and designElement index from which it came, so that
      *                             the actual expression values can be easily retrieved later
      * @throws IOException
      */
     public void addExpressionAnalysesForDesignElementIndexes(
             final Map<Long, List<Integer>> geneIdsToDEIndexes,
             Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA
     ) throws IOException {
         // Get unique factor values from this proxy geneIdsToDEIndexes, find design element with 
         String[] uEFVs = getUniqueFactorValues();
         List<String[]> uEF_EFVs = new LinkedList<String[]>();
         for (String uEFV : uEFVs) {
             uEF_EFVs.add(uEFV.split("\\|\\|"));
         }
        final long[] des = getDesignElements(); // TWill need it to retrieve design element ids for a given index
 
         for (Long geneId : geneIdsToDEIndexes.keySet()) {
             if (!geneIdsToEfToEfvToEA.containsKey(geneId)) {
                 Map<String, Map<String, ExpressionAnalysis>> efToEfvToEA = new HashMap<String, Map<String, ExpressionAnalysis>>();
                 geneIdsToEfToEfvToEA.put(geneId, efToEfvToEA);
             }
             for (Integer deIndex : geneIdsToDEIndexes.get(geneId)) {
 
                 float[] p = getPValuesForDesignElement(deIndex);
                 float[] t = getTStatisticsForDesignElement(deIndex);
 
                 for (int j = 0; j < p.length; j++) {
                     String ef = uEF_EFVs.get(j)[0];
                     if (geneIdsToEfToEfvToEA.get(geneId).get(ef) == null) {
                         Map<String, ExpressionAnalysis> efvToEA = new HashMap<String, ExpressionAnalysis>();
                         geneIdsToEfToEfvToEA.get(geneId).put(ef, efvToEA);
                     }
                     String efv = uEF_EFVs.get(j).length == 2 ? uEF_EFVs.get(j)[1] : "";
 
                     ExpressionAnalysis prevBestPValueEA =
                             geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);
                     if (prevBestPValueEA == null || prevBestPValueEA.getPValAdjusted() > p[j]) {
                         // Add this EA only if we don't yet have one for this geneid->ef->efv combination, or the
                         // previously found one has worse pValue than the current one
                         ExpressionAnalysis ea = new ExpressionAnalysis();
 
                        ea.setDesignElementID(des[deIndex]);
                         ea.setEfName(ef);
                         ea.setEfvName(efv);
                         ea.setPValAdjusted(p[j]);
                         ea.setTStatistic(t[j]);
                         ea.setExperimentID(getExperimentId());
                         ea.setDesignElementIndex(deIndex);
                         ea.setProxyId(this.getId());
                         geneIdsToEfToEfvToEA.get(geneId).get(ef).put(efv, ea);
                     }
                 }
             }
         }
     }
 }
