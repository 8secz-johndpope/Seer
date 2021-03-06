 //
 // MetamorphReader.java
 //
 
 /*
 OME Bio-Formats package for reading and converting biological file formats.
 Copyright (C) 2005-@year@ UW-Madison LOCI and Glencoe Software, Inc.
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
 package loci.formats.in;
 
 import java.io.*;
 import java.text.*;
 import java.util.*;
 import loci.common.*;
 import loci.formats.*;
 import loci.formats.meta.*;
 
 /**
  * Reader is the file format reader for Metamorph STK files.
  *
  * <dl><dt><b>Source code:</b></dt>
  * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/in/MetamorphReader.java">Trac</a>,
  * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/in/MetamorphReader.java">SVN</a></dd></dl>
  *
  * @author Eric Kjellman egkjellman at wisc.edu
  * @author Melissa Linkert linkert at wisc.edu
  * @author Curtis Rueden ctrueden at wisc.edu
  * @author Sebastien Huart Sebastien dot Huart at curie.fr
  */
 public class MetamorphReader extends BaseTiffReader {
 
   // -- Constants --
 
   public static final String[] ND_SUFFIX = {"nd"};
   public static final String[] STK_SUFFIX = {"stk", "tif", "tiff"};
 
   // IFD tag numbers of important fields
   private static final int METAMORPH_ID = 33628;
   private static final int UIC1TAG = METAMORPH_ID;
   private static final int UIC2TAG = 33629;
   private static final int UIC3TAG = 33630;
   private static final int UIC4TAG = 33631;
 
   private static final String DATE_FORMAT = "yyyyMMdd HH:mm:ss.SSS";
 
   // -- Fields --
 
   /** The TIFF's name */
   private String imageName;
 
   /** The TIFF's creation date */
   private String imageCreationDate;
 
   /** The TIFF's emWavelength */
   private long[] emWavelength;
 
   private double[] wave;
 
   private String binning;
   private float zoom, stepSize;
   private Float exposureTime;
   private Vector waveNames;
   private long[] internalStamps;
   private double[] zDistances, stageX, stageY;
 
   private int mmPlanes; //number of metamorph planes
 
   private MetamorphReader stkReader;
 
   /** List of STK files in the dataset. */
   private String[][] stks;
 
   private String ndFilename;
 
   // -- Constructor --
 
   /** Constructs a new Metamorph reader. */
   public MetamorphReader() {
     super("Metamorph STK", new String[] {"stk", "nd"});
   }
 
   // -- IFormatReader API methods --
 
   /* @see loci.formats.IFormatReader#fileGroupOption(String) */
   public int fileGroupOption(String id) throws FormatException, IOException {
     if (checkSuffix(id, ND_SUFFIX)) return FormatTools.MUST_GROUP;
 
     Location l = new Location(id).getAbsoluteFile();
     String[] files = l.getParentFile().list();
 
     for (int i=0; i<files.length; i++) {
       if (checkSuffix(files[i], ND_SUFFIX) &&
        id.startsWith(files[i].substring(0, files[i].lastIndexOf("."))))
       {
         return FormatTools.MUST_GROUP;
       }
     }
 
     return FormatTools.CANNOT_GROUP;
   }
 
   /* @see loci.formats.IFormatReader#getUsedFiles() */
   public String[] getUsedFiles() {
     FormatTools.assertId(currentId, true, 1);
     if (stks == null) return super.getUsedFiles();
     Vector v = new Vector();
     if (ndFilename != null) v.add(ndFilename);
     for (int i=0; i<stks.length; i++) {
       for (int j=0; j<stks[i].length; j++) {
         v.add(stks[i][j]);
       }
     }
     return (String[]) v.toArray(new String[0]);
   }
 
   /**
    * @see loci.formats.IFormatReader#openBytes(int, byte[], int, int, int, int)
    */
   public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
     throws FormatException, IOException
   {
     FormatTools.assertId(currentId, true, 1);
     if (stks == null) {
       return super.openBytes(no, buf, x, y, w, h);
     }
 
     int[] coords = FormatTools.getZCTCoords(this, no % getSizeZ());
     int ndx = no / getSizeZ();
     if (stks[series].length == 1) ndx = 0;
     String file = stks[series][ndx];
 
     // the original file is a .nd file, so we need to construct a new reader
     // for the constituent STK files
     if (stkReader == null) stkReader = new MetamorphReader();
     stkReader.setId(file);
     int plane = stks[series].length == 1 ? no : coords[0];
     return stkReader.openBytes(plane, buf, x, y, w, h);
   }
 
   // -- IFormatHandler API methods --
 
   /* @see loci.formats.IFormatHandler#close() */
   public void close() throws IOException {
     super.close();
     if (stkReader != null) stkReader.close();
     stkReader = null;
     imageName = imageCreationDate = null;
     emWavelength = null;
     stks = null;
     mmPlanes = 0;
     ndFilename = null;
   }
 
   // -- Internal FormatReader API methods --
 
   /* @see loci.formats.FormatReader#initFile(String) */
   protected void initFile(String id) throws FormatException, IOException {
     if (checkSuffix(id, ND_SUFFIX)) {
       status("Initializing " + id);
       // find an associated STK file
       String stkFile = id.substring(0, id.lastIndexOf("."));
       if (stkFile.indexOf(File.separator) != -1) {
         stkFile = stkFile.substring(stkFile.lastIndexOf(File.separator) + 1);
       }
       String parentPath = id.substring(0, id.lastIndexOf(File.separator) + 1);
       Location parent = new Location(parentPath).getAbsoluteFile();
       status("Looking for STK file in " + parent.getAbsolutePath());
       String[] dirList = parent.list();
       for (int i=0; i<dirList.length; i++) {
         if (dirList[i].indexOf(stkFile) != -1 &&
           checkSuffix(dirList[i], STK_SUFFIX))
         {
           stkFile = new Location(
             parent.getAbsolutePath(), dirList[i]).getAbsolutePath();
           break;
         }
       }
 
       if (!checkSuffix(stkFile, STK_SUFFIX)) {
         throw new FormatException("STK file not found in " +
           parent.getAbsolutePath() + ".");
       }
 
       super.initFile(stkFile);
     }
     else super.initFile(id);
 
     Location ndfile = null;
 
     if (checkSuffix(id, ND_SUFFIX)) ndfile = new Location(id);
 
     String creationTime = null;
 
     if (ndfile != null && ndfile.exists() &&
       (fileGroupOption(id) == FormatTools.MUST_GROUP || isGroupFiles()))
     {
       // parse key/value pairs from .nd file
 
       ndFilename = ndfile.getAbsolutePath();
 
       RandomAccessStream ndStream = new RandomAccessStream(ndFilename);
       String line = ndStream.readLine().trim();
 
       int zc = getSizeZ(), cc = getSizeC(), tc = getSizeT();
       String z = null, c = null, t = null;
       Vector hasZ = new Vector();
       waveNames = new Vector();
 
       while (!line.equals("\"EndFile\"")) {
         String key = line.substring(1, line.indexOf(",") - 1).trim();
         String value = line.substring(line.indexOf(",") + 1).trim();
 
         addMeta(key, value);
         if (key.equals("NZSteps")) z = value;
         else if (key.equals("NWavelengths")) c = value;
         else if (key.equals("NTimePoints")) t = value;
         else if (key.startsWith("WaveDoZ")) {
           hasZ.add(new Boolean(value.toLowerCase()));
         }
         else if (key.startsWith("WaveName")) {
           waveNames.add(value);
         }
         else if (key.startsWith("StartTime")) {
           creationTime = value;
         }
         else if (key.equals("ZStepSize")) {
           stepSize = Float.parseFloat(value);
         }
 
         line = ndStream.readLine().trim();
       }
 
       // figure out how many files we need
 
       if (z != null) zc = Integer.parseInt(z);
       if (c != null) cc = Integer.parseInt(c);
       if (t != null) tc = Integer.parseInt(t);
 
       int numFiles = cc * tc;
 
       // determine series count
 
       int seriesCount = 1;
       for (int i=0; i<cc; i++) {
         boolean hasZ1 = ((Boolean) hasZ.get(i)).booleanValue();
         boolean hasZ2 = i != 0 && ((Boolean) hasZ.get(i - 1)).booleanValue();
         if (i > 0 && hasZ1 != hasZ2) seriesCount = 2;
       }
 
       int channelsInFirstSeries = cc;
       if (seriesCount == 2) {
         channelsInFirstSeries = 0;
         for (int i=0; i<cc; i++) {
           if (((Boolean) hasZ.get(i)).booleanValue()) channelsInFirstSeries++;
         }
       }
 
       stks = new String[seriesCount][];
       if (seriesCount == 1) stks[0] = new String[numFiles];
       else {
         stks[0] = new String[channelsInFirstSeries * tc];
         stks[1] = new String[(cc - channelsInFirstSeries) * tc];
       }
 
       String prefix = ndfile.getPath();
       prefix = prefix.substring(prefix.lastIndexOf(File.separator) + 1,
         prefix.lastIndexOf("."));
 
       for (int i=0; i<cc; i++) {
         if (waveNames.get(i) != null) {
           String name = (String) waveNames.get(i);
           waveNames.setElementAt(name.substring(1, name.length() - 1), i);
         }
       }
 
       // build list of STK files
 
       int[] pt = new int[seriesCount];
       for (int i=0; i<tc; i++) {
         for (int j=0; j<cc; j++) {
           boolean validZ = ((Boolean) hasZ.get(j)).booleanValue();
           int seriesNdx = (seriesCount == 1 || validZ) ? 0 : 1;
           stks[seriesNdx][pt[seriesNdx]] = prefix;
           if (waveNames.get(j) != null) {
             stks[seriesNdx][pt[seriesNdx]] += "_w" + (j + 1) + waveNames.get(j);
           }
           stks[seriesNdx][pt[seriesNdx]] += "_t" + (i + 1) + ".STK";
           pt[seriesNdx]++;
         }
       }
 
       ndfile = ndfile.getAbsoluteFile();
 
       // check that each STK file exists
 
       for (int s=0; s<stks.length; s++) {
         for (int f=0; f<stks[s].length; f++) {
           Location l = new Location(ndfile.getParent(), stks[s][f]);
           if (!l.exists()) {
             // '%' can be converted to '-'
             if (stks[s][f].indexOf("%") != -1) {
               stks[s][f] = stks[s][f].replaceAll("%", "-");
               l = new Location(ndfile.getParent(), stks[s][f]);
               if (!l.exists()) {
                 // try replacing extension
                 stks[s][f] = stks[s][f].substring(0,
                   stks[s][f].lastIndexOf(".")) + ".TIF";
                 l = new Location(ndfile.getParent(), stks[s][f]);
                 if (!l.exists()) {
                   String filename = stks[s][f];
                   stks = null;
                   throw new FormatException("Missing STK file: " + filename);
                 }
               }
             }
 
             if (!l.exists()) {
               // try replacing extension
               stks[s][f] = stks[s][f].substring(0,
                 stks[s][f].lastIndexOf(".")) + ".TIF";
               l = new Location(ndfile.getParent(), stks[s][f]);
               if (!l.exists()) {
                 String filename = stks[s][f];
                 stks = null;
                 throw new FormatException("Missing STK file: " + filename);
               }
             }
           }
           if (stks != null) stks[s][f] = l.getAbsolutePath();
           else break;
         }
         if (stks == null) break;
       }
 
       core[0].sizeZ = zc;
       core[0].sizeC = cc;
       core[0].sizeT = tc;
       core[0].imageCount = zc * tc * cc;
       core[0].dimensionOrder = "XYZCT";
 
       if (stks != null && stks.length > 1) {
         CoreMetadata[] newCore = new CoreMetadata[stks.length];
         for (int i=0; i<stks.length; i++) {
           newCore[i] = new CoreMetadata();
           newCore[i].sizeX = getSizeX();
           newCore[i].sizeY = getSizeY();
           newCore[i].sizeZ = getSizeZ();
           newCore[i].sizeC = getSizeC();
           newCore[i].sizeT = getSizeT();
           newCore[i].pixelType = getPixelType();
           newCore[i].imageCount = getImageCount();
           newCore[i].dimensionOrder = getDimensionOrder();
           newCore[i].rgb = isRGB();
           newCore[i].littleEndian = isLittleEndian();
           newCore[i].interleaved = isInterleaved();
           newCore[i].orderCertain = true;
         }
         newCore[0].sizeC = stks[0].length / getSizeT();
         newCore[1].sizeC = stks[1].length / newCore[1].sizeT;
         newCore[1].sizeZ = 1;
         newCore[0].imageCount =
           newCore[0].sizeC * newCore[0].sizeT * newCore[0].sizeZ;
         newCore[1].imageCount = newCore[1].sizeC * newCore[1].sizeT;
         core = newCore;
       }
     }
 
     String comment = TiffTools.getComment(ifds[0]);
     MetamorphHandler handler = new MetamorphHandler(getMetadata());
     if (comment != null && comment.startsWith("<MetaData>")) {
       DataTools.parseXML(comment, handler);
     }
 
     Vector timestamps = handler.getTimestamps();
 
     MetadataStore store =
       new FilterMetadata(getMetadataStore(), isMetadataFiltered());
     MetadataTools.populatePixels(store, this, true);
     for (int i=0; i<getSeriesCount(); i++) {
       if (creationTime != null) {
         store.setImageCreationDate(DataTools.formatDate(creationTime,
           "yyyyMMdd HH:mm:ss"), 0);
       }
       else if (i > 0) MetadataTools.setDefaultCreationDate(store, id, i);
       if (i == 0) store.setImageName(handler.getImageName(), 0);
       else store.setImageName("", i);
       store.setImageDescription("", i);
     }
 
     for (int i=0; i<timestamps.size(); i++) {
       addMeta("timestamp " + i, DataTools.formatDate(
         (String) timestamps.get(i), DATE_FORMAT));
     }
 
     long startDate = 0;
     if (timestamps.size() > 0) {
       startDate = DataTools.getTime((String) timestamps.get(0), DATE_FORMAT);
     }
 
     Float positionX = new Float(handler.getStagePositionX());
     Float positionY = new Float(handler.getStagePositionY());
    if (exposureTime == null) exposureTime = new Float(handler.getExposure());
 
     for (int i=0; i<getImageCount(); i++) {
       int[] coords = getZCTCoords(i);
       if (coords[2] < timestamps.size()) {
         String stamp = (String) timestamps.get(coords[2]);
         long ms = DataTools.getTime(stamp, DATE_FORMAT);
        store.setPlaneTimingDeltaT(new Float(ms - startDate), 0, 0, i);
        store.setPlaneTimingExposureTime(exposureTime, 0, 0, i);
       }
       else if (internalStamps != null && i < internalStamps.length) {
         long delta = internalStamps[i] - internalStamps[0];
         store.setPlaneTimingDeltaT(new Float(delta / 1000f), 0, 0, i);
        store.setPlaneTimingExposureTime(exposureTime, 0, 0, i);
       }
       if (stageX != null && i < stageX.length) {
         store.setStagePositionPositionX(new Float((float) stageX[i]), 0, 0, i);
       }
       if (stageY != null && i < stageY.length) {
         store.setStagePositionPositionY(new Float((float) stageY[i]), 0, 0, i);
       }
     }
 
     store.setImagingEnvironmentTemperature(
       new Float(handler.getTemperature()), 0);
     store.setDimensionsPhysicalSizeX(new Float(handler.getPixelSizeX()), 0, 0);
     store.setDimensionsPhysicalSizeY(new Float(handler.getPixelSizeY()), 0, 0);
     if (zDistances != null) {
       stepSize = (float) zDistances[0];
     }
     store.setDimensionsPhysicalSizeZ(new Float(stepSize), 0, 0);
 
     for (int i=0; i<getEffectiveSizeC(); i++) {
       if (waveNames != null && i < waveNames.size()) {
         store.setLogicalChannelName((String) waveNames.get(i), 0, i);
       }
       store.setDetectorSettingsBinning(binning, 0, i);
       if (handler.getBinning() != null) {
         store.setDetectorSettingsBinning(handler.getBinning(), 0, i);
       }
       if (handler.getReadOutRate() != 0) {
         store.setDetectorSettingsReadOutRate(
           new Float(handler.getReadOutRate()), 0, i);
       }
       store.setDetectorSettingsDetector("Detector:0", 0, i);
 
       int index = getIndex(0, i, 0);
       if (wave != null && index < wave.length && (int) wave[index] >= 1) {
         store.setLightSourceSettingsWavelength(
           new Integer((int) wave[index]), 0, i);
 
         // link LightSource to Image
         store.setLightSourceID("LightSource:" + i, 0, i);
         store.setLightSourceSettingsLightSource("LightSource:" + i, 0, i);
         store.setLaserType("Unknown", 0, i);
       }
     }
     store.setDetectorID("Detector:0", 0, 0);
     store.setDetectorZoom(new Float(zoom), 0, 0);
     if (handler.getZoom() != 0f) {
       store.setDetectorZoom(new Float(handler.getZoom()), 0, 0);
     }
     store.setDetectorType("Unknown", 0, 0);
   }
 
   // -- Internal BaseTiffReader API methods --
 
   /* @see BaseTiffReader#initStandardMetadata() */
   protected void initStandardMetadata() throws FormatException, IOException {
     super.initStandardMetadata();
 
     try {
       // Now that the base TIFF standard metadata has been parsed, we need to
       // parse out the STK metadata from the UIC4TAG.
       TiffIFDEntry uic1tagEntry = TiffTools.getFirstIFDEntry(in, UIC1TAG);
       TiffIFDEntry uic2tagEntry = TiffTools.getFirstIFDEntry(in, UIC2TAG);
       TiffIFDEntry uic4tagEntry = TiffTools.getFirstIFDEntry(in, UIC4TAG);
       mmPlanes = uic4tagEntry.getValueCount();
       parseUIC2Tags(uic2tagEntry.getValueOffset());
       parseUIC4Tags(uic4tagEntry.getValueOffset());
       parseUIC1Tags(uic1tagEntry.getValueOffset(),
         uic1tagEntry.getValueCount());
       in.seek(uic4tagEntry.getValueOffset());
 
       // copy ifds into a new array of Hashtables that will accommodate the
       // additional image planes
       long[] uic2 = TiffTools.getIFDLongArray(ifds[0], UIC2TAG, true);
       core[0].imageCount = uic2.length;
 
       TiffRational[] uic3 =
         (TiffRational[]) TiffTools.getIFDValue(ifds[0], UIC3TAG);
       wave = new double[uic3.length];
       for (int i=0; i<uic3.length; i++) {
         wave[i] = uic3[i].doubleValue();
         addMeta("Wavelength [" + intFormatMax(i, mmPlanes) + "]", wave[i]);
       }
 
       Hashtable[] tempIFDs = new Hashtable[getImageCount()];
 
       long[] oldOffsets = TiffTools.getStripOffsets(ifds[0]);
       long[] stripByteCounts = TiffTools.getStripByteCounts(ifds[0]);
       int rowsPerStrip = (int) TiffTools.getRowsPerStrip(ifds[0])[0];
       int stripsPerImage = getSizeY() / rowsPerStrip;
       if (stripsPerImage * rowsPerStrip != getSizeY()) stripsPerImage++;
 
       int check = TiffTools.getPhotometricInterpretation(ifds[0]);
       if (check == TiffTools.RGB_PALETTE) {
         TiffTools.putIFDValue(ifds[0], TiffTools.PHOTOMETRIC_INTERPRETATION,
           TiffTools.BLACK_IS_ZERO);
       }
 
       emWavelength = TiffTools.getIFDLongArray(ifds[0], UIC3TAG, true);
 
       // for each image plane, construct an IFD hashtable
 
       int pointer = 0;
 
       Hashtable temp;
       for (int i=0; i<getImageCount(); i++) {
         // copy data from the first IFD
         temp = new Hashtable(ifds[0]);
 
         // now we need a StripOffsets entry - the original IFD doesn't have this
 
         long[] newOffsets = new long[stripsPerImage];
         if (stripsPerImage * (i + 1) <= oldOffsets.length) {
           System.arraycopy(oldOffsets, stripsPerImage * i, newOffsets, 0,
             stripsPerImage);
         }
         else {
           System.arraycopy(oldOffsets, 0, newOffsets, 0, stripsPerImage);
           long image = (stripByteCounts[0] / rowsPerStrip) * getSizeY();
           for (int q=0; q<stripsPerImage; q++) {
             newOffsets[q] += i * image;
           }
         }
 
         temp.put(new Integer(TiffTools.STRIP_OFFSETS), newOffsets);
 
         long[] newByteCounts = new long[stripsPerImage];
         if (stripsPerImage * i < stripByteCounts.length) {
           System.arraycopy(stripByteCounts, stripsPerImage * i, newByteCounts,
             0, stripsPerImage);
         }
         else {
           Arrays.fill(newByteCounts, stripByteCounts[0]);
         }
         temp.put(new Integer(TiffTools.STRIP_BYTE_COUNTS), newByteCounts);
 
         tempIFDs[pointer] = temp;
         pointer++;
       }
       ifds = tempIFDs;
     }
     catch (UnknownTagException exc) { trace(exc); }
     catch (NullPointerException exc) { trace(exc); }
     catch (IOException exc) { trace(exc); }
     catch (FormatException exc) { trace(exc); }
 
     try {
       super.initStandardMetadata();
     }
     catch (FormatException exc) {
       if (debug) trace(exc);
     }
     catch (IOException exc) {
       if (debug) trace(exc);
     }
 
     // parse (mangle) TIFF comment
     String descr = TiffTools.getComment(ifds[0]);
     if (descr != null) {
       String[] lines = descr.split("\n");
       StringBuffer sb = new StringBuffer();
       for (int i=0; i<lines.length; i++) {
         String line = lines[i].trim();
         int colon = line.indexOf(": ");
 
         String descrValue = null;
 
         if (colon < 0) {
           // normal line (not a key/value pair)
           if (line.length() > 0) {
             // not a blank line
             descrValue = line;
           }
         }
         else {
           if (i == 0) {
             // first line could be mangled; make a reasonable guess
             int dot = line.lastIndexOf(".", colon);
             if (dot >= 0) {
               descrValue = line.substring(0, dot + 1);
             }
             line = line.substring(dot + 1);
             colon -= dot + 1;
           }
 
           // append value to description
           if (descrValue != null) {
             sb.append(descrValue);
             if (!descrValue.endsWith(".")) sb.append(".");
             sb.append("  ");
           }
 
           // add key/value pair embedded in comment as separate metadata
           String key = line.substring(0, colon);
           String value = line.substring(colon + 2);
           addMeta(key, value);
           if (key.equals("Exposure")) {
             if (value.indexOf(" ") != -1) {
               value = value.substring(0, value.indexOf(" "));
             }
             float exposure = Float.parseFloat(value);
             exposureTime = new Float(exposure / 1000);
           }
         }
       }
 
       // replace comment with trimmed version
       descr = sb.toString().trim();
       if (descr.equals("")) metadata.remove("Comment");
       else addMeta("Comment", descr);
     }
     try {
       if (getSizeZ() == 0) {
         core[0].sizeZ =
           TiffTools.getIFDLongArray(ifds[0], UIC2TAG, true).length;
       }
       if (getSizeT() == 0) core[0].sizeT = getImageCount() / getSizeZ();
     }
     catch (FormatException exc) {
       if (debug) trace(exc);
     }
   }
 
   // -- Helper methods --
 
   /**
    * Populates metadata fields with some contained in MetaMorph UIC2 Tag.
    * (for each plane: 6 integers:
    * zdistance numerator, zdistance denominator,
    * creation date, creation time, modif date, modif time)
    * @param uic2offset offset to UIC2 (33629) tag entries
    *
    * not a regular tiff tag (6*N entries, N being the tagCount)
    * @throws IOException
    */
   void parseUIC2Tags(long uic2offset) throws IOException {
     long saveLoc = in.getFilePointer();
     in.seek(uic2offset);
 
     /*number of days since the 1st of January 4713 B.C*/
     String cDate;
     /*milliseconds since 0:00*/
     String cTime;
 
     /*z step, distance separating previous slice from  current one*/
     String iAsString;
 
     zDistances = new double[mmPlanes];
     internalStamps = new long[mmPlanes];
 
     for (int i=0; i<mmPlanes; i++) {
       iAsString = intFormatMax(i, mmPlanes);
       zDistances[i] = readRational(in).doubleValue();
       addMeta("zDistance[" + iAsString + "]", zDistances[i]);
 
       cDate = decodeDate(in.readInt());
       cTime = decodeTime(in.readInt());
 
       internalStamps[i] = DataTools.getTime(cDate + " " + cTime,
         "dd/MM/yyyy HH:mm:ss:SSS");
 
       addMeta("creationDate[" + iAsString + "]", cDate);
       addMeta("creationTime[" + iAsString + "]", cTime);
       // modification date and time are skipped as they all seem equal to 0...?
       in.skip(8);
     }
     in.seek(saveLoc);
   }
 
   /**
    * UIC4 metadata parser
    *
    * UIC4 Table contains per-plane blocks of metadata
    * stage X/Y positions,
    * camera chip offsets,
    * stage labels...
    * @param long uic4offset: offset of UIC4 table (not tiff-compliant)
    * @throws IOException
    */
   private void parseUIC4Tags(long uic4offset) throws IOException {
     long saveLoc = in.getFilePointer();
     in.seek(uic4offset);
     short id = in.readShort();
     while (id != 0) {
       switch (id) {
         case 28:
           readStagePositions();
           break;
         case 29:
           readRationals(
             new String[] {"cameraXChipOffset", "cameraYChipOffset"});
           break;
         case 37:
           readStageLabels();
           break;
         case 40:
           readRationals(new String[] {"absoluteZ"});
           break;
         case 41:
           readAbsoluteZValid();
           break;
       }
       id = in.readShort();
     }
     in.seek(saveLoc);
   }
 
   private void readStagePositions() throws IOException {
     stageX = new double[mmPlanes];
     stageY = new double[mmPlanes];
     String pos;
     for (int i=0; i<mmPlanes; i++) {
       pos = intFormatMax(i, mmPlanes);
       stageX[i] = readRational(in).doubleValue();
       stageY[i] = readRational(in).doubleValue();
       addMeta("stageX[" + pos + "]", stageX[i]);
       addMeta("stageY[" + pos + "]", stageY[i]);
     }
   }
 
   private void readRationals(String[] labels) throws IOException {
     String pos;
     for (int i=0; i<mmPlanes; i++) {
       pos = intFormatMax(i, mmPlanes);
       for (int q=0; q<labels.length; q++) {
         addMeta(labels[q] + "[" + pos + "]", readRational(in).doubleValue());
       }
     }
   }
 
   void readStageLabels() throws IOException {
     int strlen;
     String iAsString;
     for (int i=0; i<mmPlanes; i++) {
       iAsString = intFormatMax(i, mmPlanes);
       strlen = in.readInt();
       addMeta("stageLabel[" + iAsString + "]", in.readString(strlen));
     }
   }
 
   void readAbsoluteZValid() throws IOException {
     for (int i=0; i<mmPlanes; i++) {
       addMeta("absoluteZValid[" + intFormatMax(i, mmPlanes) + "]",
         in.readInt());
     }
   }
 
   /**
    * UIC1 entry parser
    * @throws IOException
    * @param long uic1offset : offset as found in the tiff tag 33628 (UIC1Tag)
    * @param int uic1count : number of entries in UIC1 table (not tiff-compliant)
    */
   private void parseUIC1Tags(long uic1offset, int uic1count) throws IOException
   {
     // Loop through and parse out each field. A field whose
     // code is "0" represents the end of the fields so we'll stop
     // when we reach that; much like a NULL terminated C string.
     long saveLoc = in.getFilePointer();
     in.seek(uic1offset);
     int currentID, valOrOffset;
     // variable declarations, because switch is dumb
     int num, denom;
     String thedate, thetime;
     long lastOffset;
     for (int i=0; i<uic1count; i++) {
       currentID = in.readInt();
       valOrOffset = in.readInt();
       lastOffset = in.getFilePointer();
 
       String key = getKey(currentID);
       Object value = String.valueOf(valOrOffset);
 
       switch (currentID) {
         case 3:
           value = valOrOffset != 0 ? "on" : "off";
           break;
         case 4:
         case 5:
         case 21:
         case 22:
         case 23:
         case 24:
         case 38:
         case 39:
           value = readRational(in, valOrOffset);
           break;
         case 6:
         case 25:
           in.seek(valOrOffset);
           num = in.readInt();
           value = in.readString(num);
           break;
         case 7:
           in.seek(valOrOffset);
           num = in.readInt();
           imageName = in.readString(num);
           value = imageName;
           break;
         case 8:
           if (valOrOffset == 1) value = "inside";
           else if (valOrOffset == 2) value = "outside";
           else value = "off";
           break;
         case 17: // oh how we hate you Julian format...
           in.seek(valOrOffset);
           thedate = decodeDate(in.readInt());
           thetime = decodeTime(in.readInt());
           imageCreationDate = thedate + " " + thetime;
           value = imageCreationDate;
           break;
         case 16:
           in.seek(valOrOffset);
           thedate = decodeDate(in.readInt());
           thetime = decodeTime(in.readInt());
           value = thedate + " " + thetime;
           break;
         case 26:
           in.seek(valOrOffset);
           int standardLUT = in.readInt();
           switch (standardLUT) {
             case 0:
               value = "monochrome";
               break;
             case 1:
               value = "pseudocolor";
               break;
             case 2:
               value = "Red";
               break;
             case 3:
               value = "Green";
               break;
             case 4:
               value = "Blue";
               break;
             case 5:
               value = "user-defined";
               break;
             default:
               value = "monochrome";
           }
           break;
         case 34:
           value = String.valueOf(in.readInt());
           break;
         case 46:
           in.seek(valOrOffset);
           int xBin = in.readInt();
           int yBin = in.readInt();
           binning = xBin + "x" + yBin;
           value = binning;
           break;
       }
       addMeta(key, value);
       in.seek(lastOffset);
 
       if ("Zoom".equals(key) && value != null) {
         zoom = Float.parseFloat(value.toString());
       }
     }
     in.seek(saveLoc);
   }
 
   // -- Utility methods --
 
   /** Converts a Julian date value into a human-readable string. */
   public static String decodeDate(int julian) {
     long a, b, c, d, e, alpha, z;
     short day, month, year;
 
     // code reused from the Metamorph data specification
     z = julian + 1;
 
     if (z < 2299161L) a = z;
     else {
       alpha = (long) ((z - 1867216.25) / 36524.25);
       a = z + 1 + alpha - alpha / 4;
     }
 
     b = (a > 1721423L ? a + 1524 : a + 1158);
     c = (long) ((b - 122.1) / 365.25);
     d = (long) (365.25 * c);
     e = (long) ((b - d) / 30.6001);
 
     day = (short) (b - d - (long) (30.6001 * e));
     month = (short) ((e < 13.5) ? e - 1 : e - 13);
     year = (short) ((month > 2.5) ? (c - 4716) : c - 4715);
 
     return intFormat(day, 2) + "/" + intFormat(month, 2) + "/" + year;
   }
 
   /** Converts a time value in milliseconds into a human-readable string. */
   public static String decodeTime(int millis) {
     Calendar time = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
     time.setTimeInMillis(millis);
     String hours = intFormat(time.get(Calendar.HOUR_OF_DAY), 2);
     String minutes = intFormat(time.get(Calendar.MINUTE), 2);
     String seconds = intFormat(time.get(Calendar.SECOND), 2);
     String ms = intFormat(time.get(Calendar.MILLISECOND), 3);
     return hours + ":" + minutes + ":" + seconds + ":" + ms;
   }
 
   /** Formats an integer value with leading 0s if needed. */
   public static String intFormat(int myint, int digits) {
     DecimalFormat df = new DecimalFormat();
     df.setMaximumIntegerDigits(digits);
     df.setMinimumIntegerDigits(digits);
     return df.format(myint);
   }
 
   /**
    * Formats an integer with leading 0 using maximum sequence number.
    *
    * @param myint integer to format
    * @param maxint max of "myint"
    * @return String
    */
   public static String intFormatMax(int myint, int maxint) {
     return intFormat(myint, String.valueOf(maxint).length());
   }
 
   private TiffRational readRational(RandomAccessStream s) throws IOException {
     return readRational(s, s.getFilePointer());
   }
 
   private TiffRational readRational(RandomAccessStream s, long offset)
     throws IOException
   {
     s.seek(offset);
     int num = s.readInt();
     int denom = s.readInt();
     return new TiffRational(num, denom);
   }
 
   private String getKey(int id) {
     switch (id) {
       case 1: return "MinScale";
       case 2: return "MaxScale";
       case 3: return "Spatial Calibration";
       case 4: return "XCalibration";
       case 5: return "YCalibration";
       case 6: return "CalibrationUnits";
       case 7: return "Name";
       case 8: return "ThreshState";
       case 9: return "ThreshStateRed";
       // there is no 10
       case 11: return "ThreshStateGreen";
       case 12: return "ThreshStateBlue";
       case 13: return "ThreshStateLo";
       case 14: return "ThreshStateHi";
       case 15: return "Zoom";
       case 16: return "DateTime";
       case 17: return "LastSavedTime";
       case 18: return "currentBuffer";
       case 19: return "grayFit";
       case 20: return "grayPointCount";
       case 21: return "grayX";
       case 22: return "gray";
       case 23: return "grayMin";
       case 24: return "grayMax";
       case 25: return "grayUnitName";
       case 26: return "StandardLUT";
       case 27: return "Wavelength";
       case 30: return "OverlayMask";
       case 31: return "OverlayCompress";
       case 32: return "Overlay";
       case 33: return "SpecialOverlayMask";
       case 34: return "SpecialOverlayCompress";
       case 35: return "SpecialOverlay";
       case 36: return "ImageProperty";
       case 38: return "AutoScaleLoInfo";
       case 39: return "AutoScaleHiInfo";
       case 42: return "Gamma";
       case 43: return "GammaRed";
       case 44: return "GammaGreen";
       case 45: return "GammaBlue";
       case 46: return "CameraBin";
     }
     return null;
   }
 
 }
