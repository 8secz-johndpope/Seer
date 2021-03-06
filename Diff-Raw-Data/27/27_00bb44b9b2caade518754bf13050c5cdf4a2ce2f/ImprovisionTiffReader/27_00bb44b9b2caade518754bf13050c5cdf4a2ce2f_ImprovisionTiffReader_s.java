 //
 // ImprovisionTiffReader.java
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
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Arrays;
 
 import loci.common.Location;
 import loci.common.RandomAccessInputStream;
 import loci.formats.FormatException;
 import loci.formats.FormatTools;
 import loci.formats.MetadataTools;
 import loci.formats.meta.MetadataStore;
 import loci.formats.tiff.TiffParser;
 
 /**
  * ImprovisionTiffReader is the file format reader for
  * Improvision TIFF files.
  *
  * <dl><dt><b>Source code:</b></dt>
  * <dd><a href="http://dev.loci.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/in/ImprovisionTiffReader.java">Trac</a>,
  * <a href="http://dev.loci.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/in/ImprovisionTiffReader.java">SVN</a></dd></dl>
  */
 public class ImprovisionTiffReader extends BaseTiffReader {
 
   // -- Constants --
 
   public static final String IMPROVISION_MAGIC_STRING = "Improvision";
 
   // -- Fields --
 
   private String[] cNames;
   private int pixelSizeT;
   private double pixelSizeX, pixelSizeY, pixelSizeZ;
 
   private String[] files;
   private MinimalTiffReader[] readers;
 
   // -- Constructor --
 
   public ImprovisionTiffReader() {
     super("Improvision TIFF", new String[] {"tif", "tiff"});
     suffixSufficient = false;
     domains = new String[] {FormatTools.UNKNOWN_DOMAIN};
   }
 
   // -- IFormatReader API methods --
 
   /* @see loci.formats.IFormatReader#isThisType(RandomAccessInputStream) */
   public boolean isThisType(RandomAccessInputStream stream) throws IOException {
     TiffParser tp = new TiffParser(stream);
     String comment = tp.getComment();
     if (comment == null) return false;
     return comment.indexOf(IMPROVISION_MAGIC_STRING) >= 0;
   }
 
   /* @see loci.formats.IFormatReader#close(boolean) */
   public void close(boolean fileOnly) throws IOException {
     super.close(fileOnly);
     if (!fileOnly) {
       cNames = null;
       pixelSizeT = 1;
       pixelSizeX = pixelSizeY = pixelSizeZ = 0;
       if (readers != null) {
         for (MinimalTiffReader reader : readers) {
           if (reader != null) reader.close();
         }
       }
       readers = null;
       files = null;
     }
   }
 
   /* @see loci.formats.IFormatReader#getSeriesUsedFiles(boolean) */
   public String[] getSeriesUsedFiles(boolean noPixels) {
     FormatTools.assertId(currentId, true, 1);
     return noPixels ? null : files;
   }
 
   /**
    * @see loci.formats.IFormatReader#openBytes(int, byte[], int, int, int, int)
    */ 
   public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
     throws FormatException, IOException
   {
     FormatTools.checkPlaneParameters(this, no, buf.length, x, y, w, h);
 
     int[] zct = getZCTCoords(no);
     int file = FormatTools.getIndex("XYZCT", getSizeZ(), getEffectiveSizeC(),
       getSizeT(), getImageCount(), zct[0], zct[1], zct[2]) % files.length;
     int plane = no / files.length;
 
     return readers[file].openBytes(plane, buf, x, y, w, h);
   }
 
   // -- Internal BaseTiffReader API methods --
 
   /* @see BaseTiffReader#initStandardMetadata() */
   protected void initStandardMetadata() throws FormatException, IOException {
     super.initStandardMetadata();
 
     put("Improvision", "yes");
 
     // parse key/value pairs in the comment
     String comment = ifds.get(0).getComment();
     String tz = null, tc = null, tt = null;
     if (comment != null) {
       String[] lines = comment.split("\n");
       for (String line : lines) {
         int equals = line.indexOf("=");
         if (equals < 0) continue;
         String key = line.substring(0, equals);
         String value = line.substring(equals + 1);
         addGlobalMeta(key, value);
         if (key.equals("TotalZPlanes")) tz = value;
         else if (key.equals("TotalChannels")) tc = value;
         else if (key.equals("TotalTimepoints")) tt = value;
         else if (key.equals("XCalibrationMicrons")) {
           pixelSizeX = Double.parseDouble(value);
         }
         else if (key.equals("YCalibrationMicrons")) {
           pixelSizeY = Double.parseDouble(value);
         }
         else if (key.equals("ZCalibrationMicrons")) {
           pixelSizeZ = Double.parseDouble(value);
         }
       }
       metadata.remove("Comment");
     }
 
     core[0].sizeT = 1;
     if (getSizeZ() == 0) core[0].sizeZ = 1;
     if (getSizeC() == 0) core[0].sizeC = 1;
 
     if (tz != null) core[0].sizeZ *= Integer.parseInt(tz);
     if (tc != null) core[0].sizeC *= Integer.parseInt(tc);
     if (tt != null) core[0].sizeT *= Integer.parseInt(tt);
 
     if (getSizeZ() * getSizeC() * getSizeT() < getImageCount()) {
       core[0].sizeC *= getImageCount();
     }
     else core[0].imageCount = getSizeZ() * getSizeT() * Integer.parseInt(tc);
 
     // parse each of the comments to determine axis ordering
 
     long[] stamps = new long[ifds.size()];
     int[][] coords = new int[ifds.size()][3];
 
     cNames = new String[getSizeC()];
 
     boolean multipleFiles = false;
 
     for (int i=0; i<ifds.size(); i++) {
       Arrays.fill(coords[i], -1);
       comment = ifds.get(i).getComment();
       comment = comment.replaceAll("\r\n", "\n");
       comment = comment.replaceAll("\r", "\n");
       String channelName = null;
       String[] lines = comment.split("\n");
       for (String line : lines) {
         int equals = line.indexOf("=");
         if (equals < 0) continue;
         String key = line.substring(0, equals);
         String value = line.substring(equals + 1);
 
         if (key.equals("TimeStampMicroSeconds")) {
           stamps[i] = Long.parseLong(value);
         }
         else if (key.equals("ZPlane")) coords[i][0] = Integer.parseInt(value);
         else if (key.equals("ChannelNo")) {
           coords[i][1] = Integer.parseInt(value);
         }
         else if (key.equals("TimepointName")) {
           coords[i][2] = Integer.parseInt(value);
         }
         else if (key.equals("ChannelName")) {
           channelName = value;
         }
         else if (key.equals("ChannelNo")) {
           int ndx = Integer.parseInt(value);
           if (cNames[ndx] == null) cNames[ndx] = channelName;
         }
         else if (key.equals("MultiFileTIFF")) {
           multipleFiles = value.equalsIgnoreCase("yes");
         }
 
         if (getMetadataOptions().getMetadataLevel() == MetadataLevel.MINIMUM &&
           coords[i][0] >= 0 && coords[i][1] >= 0 && coords[i][2] >= 0)
         {
           break;
         }
       }
     }
 
     if (multipleFiles) {
       // look for other TIFF files that belong to this dataset
 
       String currentUUID = getUUID(currentId);
 
       Location parent =
         new Location(currentId).getAbsoluteFile().getParentFile();
       String[] list = parent.list(true);
       Arrays.sort(list);
       ArrayList<String> matchingFiles = new ArrayList<String>();
       for (String f : list) {
         String path = new Location(parent, f).getAbsolutePath();
         if (isThisType(path) && getUUID(path).equals(currentUUID)) {
           matchingFiles.add(path);
         }
       }
 
       files = matchingFiles.toArray(new String[matchingFiles.size()]);
     }
     else {
       files = new String[] {currentId};
     }
     readers = new MinimalTiffReader[files.length];
     for (int i=0; i<readers.length; i++) {
       readers[i] = new MinimalTiffReader();
       readers[i].setId(files[i]);
     }
 
     if (getMetadataOptions().getMetadataLevel() != MetadataLevel.MINIMUM) {
       // determine average time per plane
 
       long sum = 0;
       for (int i=1; i<stamps.length; i++) {
         long diff = stamps[i] - stamps[i - 1];
         if (diff > 0) sum += diff;
       }
       pixelSizeT = (int) (sum / getSizeT());
     }
 
     // determine dimension order
 
     core[0].dimensionOrder = "XY";
     if (isRGB()) core[0].dimensionOrder += "C";
     for (int i=1; i<coords.length; i++) {
       int zDiff = coords[i][0] - coords[i - 1][0];
       int cDiff = coords[i][1] - coords[i - 1][1];
       int tDiff = coords[i][2] - coords[i - 1][2];
 
       if (zDiff > 0 && getDimensionOrder().indexOf("Z") < 0) {
         core[0].dimensionOrder += "Z";
       }
       if (cDiff > 0 && getDimensionOrder().indexOf("C") < 0) {
         core[0].dimensionOrder += "C";
       }
       if (tDiff > 0 && getDimensionOrder().indexOf("T") < 0) {
         core[0].dimensionOrder += "T";
       }
       if (core[0].dimensionOrder.length() == 5) break;
     }
 
     if (getDimensionOrder().indexOf("Z") < 0) core[0].dimensionOrder += "Z";
     if (getDimensionOrder().indexOf("C") < 0) core[0].dimensionOrder += "C";
     if (getDimensionOrder().indexOf("T") < 0) core[0].dimensionOrder += "T";
   }
 
   /* @see BaseTiffReader#initMetadataStore() */
   protected void initMetadataStore() throws FormatException {
     super.initMetadataStore();
     MetadataStore store = makeFilterMetadata();
     MetadataTools.populatePixels(store, this);
 
     if (getMetadataOptions().getMetadataLevel() != MetadataLevel.MINIMUM) {
       store.setPixelsPhysicalSizeX(pixelSizeX, 0);
       store.setPixelsPhysicalSizeY(pixelSizeY, 0);
       store.setPixelsPhysicalSizeZ(pixelSizeZ, 0);
       store.setPixelsTimeIncrement(pixelSizeT / 1000000.0, 0);
       for (int i=0; i<getEffectiveSizeC(); i++) {
         if (cNames != null && i < cNames.length) {
           store.setChannelName(cNames[i], 0, i);
         }
       }
       store.setImageDescription("", 0);
     }
   }
 
   // -- Helper methods --
 
   private String getUUID(String path) throws FormatException, IOException {
     RandomAccessInputStream s = new RandomAccessInputStream(path);
     TiffParser parser = new TiffParser(s);
     String comment = parser.getComment();
 
     comment = comment.replaceAll("\r\n", "\n");
     comment = comment.replaceAll("\r", "\n");
     String[] lines = comment.split("\n");
     for (String line : lines) {
       line = line.trim();
       if (line.startsWith("SampleUUID=")) {
         return line.substring(line.indexOf("=") + 1).trim();
       }
     }
     return "";
   }
 
 }
