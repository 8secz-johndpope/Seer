 //
 // MinimalTiffReader.java
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
 
 import loci.common.DataTools;
 import loci.common.RandomAccessInputStream;
 import loci.formats.FormatException;
 import loci.formats.FormatReader;
 import loci.formats.FormatTools;
 import loci.formats.tiff.IFD;
 import loci.formats.tiff.IFDList;
 import loci.formats.tiff.PhotoInterp;
 import loci.formats.tiff.TiffParser;
 
 /**
  * MinimalTiffReader is the superclass for file format readers compatible with
  * or derived from the TIFF 6.0 file format.
  *
  * <dl><dt><b>Source code:</b></dt>
  * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/in/MinimalTiffReader.java">Trac</a>,
  * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/in/MinimalTiffReader.java">SVN</a></dd></dl>
  *
  * @author Melissa Linkert melissa at glencoesoftware.com
  */
 public class MinimalTiffReader extends FormatReader {
 
   // -- Fields --
 
   /** List of IFDs for the current TIFF. */
   protected IFDList ifds;
 
   /** List of thumbnail IFDs for the current TIFF. */
   protected IFDList thumbnailIFDs;
 
   protected TiffParser tiffParser;
 
   private int lastPlane;
 
   // -- Constructors --
 
   /** Constructs a new MinimalTiffReader. */
   public MinimalTiffReader() {
     this("Minimal TIFF", new String[] {"tif", "tiff"});
   }
 
   /** Constructs a new MinimalTiffReader. */
   public MinimalTiffReader(String name, String suffix) {
     this(name, new String[] {suffix});
   }
 
   /** Constructs a new MinimalTiffReader. */
   public MinimalTiffReader(String name, String[] suffixes) {
     super(name, suffixes);
     domains = new String[] {FormatTools.GRAPHICS_DOMAIN};
     suffixNecessary = false;
   }
 
   // -- MinimalTiffReader API methods --
 
   /** Gets the list of IFDs associated with the current TIFF's image planes. */
   public IFDList getIFDs() {
     return ifds;
   }
 
   /** Gets the list of IFDs associated with the current TIFF's thumbnails. */
   public IFDList getThumbnailIFDs() {
     return thumbnailIFDs;
   }
 
   // -- IFormatReader API methods --
 
   /* @see loci.formats.IFormatReader#isThisType(RandomAccessInputStream) */
   public boolean isThisType(RandomAccessInputStream stream) throws IOException {
     return new TiffParser(stream).isValidHeader();
   }
 
   /* @see loci.formats.IFormatReader#get8BitLookupTable() */
   public byte[][] get8BitLookupTable() throws FormatException, IOException {
     FormatTools.assertId(currentId, true, 1);
     if (ifds == null || lastPlane < 0 || lastPlane > ifds.size()) return null;
     IFD lastIFD = ifds.get(lastPlane);
     int[] bits = lastIFD.getBitsPerSample();
     if (bits[0] <= 8) {
       int[] colorMap = lastIFD.getIFDIntArray(IFD.COLOR_MAP);
       if (colorMap == null) {
         // it's possible that the LUT is only present in the first IFD
         if (lastPlane != 0) {
           lastIFD = ifds.get(0);
           colorMap = lastIFD.getIFDIntArray(IFD.COLOR_MAP);
           if (colorMap == null) return null;
         }
         else return null;
       }
 
       byte[][] table = new byte[3][colorMap.length / 3];
       int next = 0;
       for (int j=0; j<table.length; j++) {
         for (int i=0; i<table[0].length; i++) {
           table[j][i] = (byte) ((colorMap[next++] >> 8) & 0xff);
         }
       }
 
       return table;
     }
     return null;
   }
 
   /* @see loci.formats.IFormatReader#get16BitLookupTable() */
   public short[][] get16BitLookupTable() throws FormatException, IOException {
     FormatTools.assertId(currentId, true, 1);
     if (ifds == null || lastPlane < 0 || lastPlane > ifds.size()) return null;
     IFD lastIFD = ifds.get(lastPlane);
     int[] bits = lastIFD.getBitsPerSample();
     if (bits[0] <= 16 && bits[0] > 8) {
       int[] colorMap = lastIFD.getIFDIntArray(IFD.COLOR_MAP);
       if (colorMap == null || colorMap.length < 65536 * 3) {
         // it's possible that the LUT is only present in the first IFD
         if (lastPlane != 0) {
           lastIFD = ifds.get(0);
           colorMap = lastIFD.getIFDIntArray(IFD.COLOR_MAP);
           if (colorMap == null || colorMap.length < 65536 * 3) return null;
         }
         else return null;
       }
 
       short[][] table = new short[3][colorMap.length / 3];
       int next = 0;
       for (int i=0; i<table.length; i++) {
         for (int j=0; j<table[0].length; j++) {
           table[i][j] = (short) (colorMap[next++] & 0xffff);
         }
       }
       return table;
     }
     return null;
   }
 
   /* @see loci.formats.FormatReader#getThumbSizeX() */
   public int getThumbSizeX() {
     if (thumbnailIFDs != null && thumbnailIFDs.size() > 0) {
       try {
         return (int) thumbnailIFDs.get(0).getImageWidth();
       }
       catch (FormatException e) {
         LOGGER.debug("Could not retrieve thumbnail width", e);
       }
     }
     return super.getThumbSizeX();
   }
 
   /* @see loci.formats.FormatReader#getThumbSizeY() */
   public int getThumbSizeY() {
     if (thumbnailIFDs != null && thumbnailIFDs.size() > 0) {
       try {
         return (int) thumbnailIFDs.get(0).getImageLength();
       }
       catch (FormatException e) {
         LOGGER.debug("Could not retrieve thumbnail height", e);
       }
     }
     return super.getThumbSizeY();
   }
 
   /* @see loci.formats.FormatReader#openThumbBytes(int) */
   public byte[] openThumbBytes(int no) throws FormatException, IOException {
     FormatTools.assertId(currentId, true, 1);
     if (thumbnailIFDs == null || thumbnailIFDs.size() <= no) {
       return super.openThumbBytes(no);
     }
     int[] bps = thumbnailIFDs.get(no).getBitsPerSample();
     int b = bps[0];
     while ((b % 8) != 0) b++;
     b /= 8;
     if (b != FormatTools.getBytesPerPixel(getPixelType()) ||
       bps.length != getRGBChannelCount())
     {
       return super.openThumbBytes(no);
     }
 
     byte[] buf = new byte[getThumbSizeX() * getThumbSizeY() *
       getRGBChannelCount() * FormatTools.getBytesPerPixel(getPixelType())];
     return tiffParser.getSamples(thumbnailIFDs.get(no), buf);
   }
 
   /**
    * @see loci.formats.FormatReader#openBytes(int, byte[], int, int, int, int)
    */
   public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
     throws FormatException, IOException
   {
     FormatTools.checkPlaneParameters(this, no, buf.length, x, y, w, h);
 
     lastPlane = no;
     tiffParser.getSamples(ifds.get(no), buf, x, y, w, h);
 
     boolean float16 = getPixelType() == FormatTools.FLOAT &&
       ifds.get(0).getBitsPerSample()[0] == 16;
     boolean float24 = getPixelType() == FormatTools.FLOAT &&
       ifds.get(0).getBitsPerSample()[0] == 24;
 
     if (float16 || float24) {
      int nPixels = w * h * getRGBChannelCount();
       int nBytes = float16 ? 2 : 3;
       int mantissaBits = float16 ? 10 : 16;
       int exponentBits = float16 ? 5 : 7;
       int maxExponent = (int) Math.pow(2, exponentBits) - 1;
       int bits = (nBytes * 8) - 1;
 
       byte[] newBuf = new byte[buf.length];
       for (int i=0; i<nPixels; i++) {
         int v =
           DataTools.bytesToInt(buf, i * nBytes, nBytes, isLittleEndian());
         int sign = v >> bits;
         int exponent =
           (v >> mantissaBits) & (int) (Math.pow(2, exponentBits) - 1);
         int mantissa = v & (int) (Math.pow(2, mantissaBits) - 1);
 
         if (exponent == 0) {
           if (mantissa != 0) {
             while ((mantissa & (int) Math.pow(2, mantissaBits)) == 0) {
               mantissa <<= 1;
               exponent--;
             }
             exponent++;
             mantissa &= (int) (Math.pow(2, mantissaBits) - 1);
             exponent += 127 - (Math.pow(2, exponentBits - 1) - 1);
           }
         }
         else if (exponent == maxExponent) {
           exponent = 255;
         }
         else {
           exponent += 127 - (Math.pow(2, exponentBits - 1) - 1);
         }
 
         mantissa <<= (23 - mantissaBits);
 
         int value = (sign << 31) | (exponent << 23) | mantissa;
         DataTools.unpackBytes(value, newBuf, i * 4, 4, isLittleEndian());
       }
       System.arraycopy(newBuf, 0, buf, 0, newBuf.length);
     }
 
     return buf;
   }
 
   /* @see loci.formats.IFormatReader#close(boolean) */
   public void close(boolean fileOnly) throws IOException {
     super.close(fileOnly);
     if (!fileOnly) {
       ifds = null;
       thumbnailIFDs = null;
       lastPlane = 0;
       tiffParser = null;
     }
   }
 
   // -- Internal FormatReader API methods --
 
   /* @see loci.formats.FormatReader#initFile(String) */
   protected void initFile(String id) throws FormatException, IOException {
     super.initFile(id);
     in = new RandomAccessInputStream(id);
     tiffParser = new TiffParser(in);
     Boolean littleEndian = tiffParser.checkHeader();
     if (littleEndian == null) {
       throw new FormatException("Invalid TIFF file");
     }
     boolean little = littleEndian.booleanValue();
     in.order(little);
 
     LOGGER.info("Reading IFDs");
 
     ifds = tiffParser.getNonThumbnailIFDs();
     if (ifds == null || ifds.size() == 0) {
       throw new FormatException("No IFDs found");
     }
 
     thumbnailIFDs = tiffParser.getThumbnailIFDs();
 
     LOGGER.info("Populating metadata");
 
     core[0].imageCount = ifds.size();
 
     IFD firstIFD = ifds.get(0);
 
     PhotoInterp photo = firstIFD.getPhotometricInterpretation();
     int samples = firstIFD.getSamplesPerPixel();
     core[0].rgb = samples > 1 || photo == PhotoInterp.RGB;
     core[0].interleaved = false;
     core[0].littleEndian = firstIFD.isLittleEndian();
 
     core[0].sizeX = (int) firstIFD.getImageWidth();
     core[0].sizeY = (int) firstIFD.getImageLength();
     core[0].sizeZ = 1;
     core[0].sizeC = isRGB() ? samples : 1;
     core[0].sizeT = ifds.size();
     core[0].pixelType = firstIFD.getPixelType();
     core[0].metadataComplete = true;
     core[0].indexed = photo == PhotoInterp.RGB_PALETTE &&
       (get8BitLookupTable() != null || get16BitLookupTable() != null);
     if (isIndexed()) {
       core[0].sizeC = 1;
       core[0].rgb = false;
       for (IFD ifd : ifds) {
         ifd.putIFDValue(IFD.PHOTOMETRIC_INTERPRETATION,
           PhotoInterp.RGB_PALETTE);
       }
     }
     if (getSizeC() == 1 && !isIndexed()) core[0].rgb = false;
     core[0].falseColor = false;
     core[0].dimensionOrder = "XYCZT";
     core[0].bitsPerPixel = firstIFD.getBitsPerSample()[0];
   }
 
 }
