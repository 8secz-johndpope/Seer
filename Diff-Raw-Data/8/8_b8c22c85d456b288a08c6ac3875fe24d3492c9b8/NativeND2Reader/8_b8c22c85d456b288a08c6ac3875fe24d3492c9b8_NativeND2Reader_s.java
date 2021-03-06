 //
 // NativeND2Reader.java
 //
 
 /*
 OME Bio-Formats package for reading and converting biological file formats.
 Copyright (C) 2005-@year@ UW-Madison LOCI and Glencoe Software, Inc.
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
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
 
 import java.awt.Point;
 import java.io.*;
 import java.util.*;
 import loci.common.*;
 import loci.formats.*;
 import loci.formats.codec.*;
 import loci.formats.meta.FilterMetadata;
 import loci.formats.meta.MetadataStore;
 import org.xml.sax.Attributes;
 import org.xml.sax.helpers.DefaultHandler;
 
 /**
  * NativeND2Reader is the file format reader for Nikon ND2 files.
  * The JAI ImageIO library is required to use this reader; it is available from
  * http://jai-imageio.dev.java.net. Note that JAI ImageIO is bundled with a
  * version of the JJ2000 library, so it is important that either:
  * (1) the JJ2000 jar file is *not* in the classpath; or
  * (2) the JAI jar file precedes JJ2000 in the classpath.
  *
  * Thanks to Tom Caswell for additions to the ND2 metadata parsing logic.
  *
  * <dl><dt><b>Source code:</b></dt>
  * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/in/NativeND2Reader.java">Trac</a>,
  * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/in/NativeND2Reader.java">SVN</a></dd></dl>
  */
 public class NativeND2Reader extends FormatReader {
 
   // -- Fields --
 
   /** Array of image offsets. */
   private long[][] offsets;
 
   /** Whether or not the pixel data is compressed using JPEG 2000. */
   private boolean isJPEG;
 
   /** Whether or not the pixel data is losslessly compressed. */
   private boolean isLossless;
 
   private Vector zs = new Vector();
   private Vector ts = new Vector();
   private Vector tsT = new Vector();
 
   private int numSeries;
 
   private float pixelSizeX, pixelSizeY, pixelSizeZ;
   private String voltage, mag, na, objectiveModel, immersion;
 
   private Vector channelNames;
   private Vector binning;
   private Vector speed;
   private Vector gain;
   private Vector temperature;
   private Vector exposureTime;
   private Vector modality;
   private Vector exWave, emWave;
   private Vector power;
 
   private String cameraModel;
 
   // -- Constructor --
 
   /** Constructs a new ND2 reader. */
   public NativeND2Reader() {
     super("Nikon ND2", new String[] {"nd2", "jp2"});
     blockCheckLen = 8;
   }
 
   // -- IFormatReader API methods --
 
   /* @see loci.formats.IFormatReader#isThisType(RandomAccessStream) */
   public boolean isThisType(RandomAccessStream stream) throws IOException {
     if (!FormatTools.validStream(stream, blockCheckLen, false)) return false;
     stream.seek(4);
     return stream.readInt() == 0x6a502020;
   }
 
   /**
    * @see loci.formats.IFormatReader#openBytes(int, byte[], int, int, int, int)
    */
   public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
     throws FormatException, IOException
   {
     FormatTools.assertId(currentId, true, 1);
     FormatTools.checkPlaneNumber(this, no);
     FormatTools.checkBufferSize(this, buf.length, w, h);
 
     in.seek(offsets[series][no]);
 
     int bpp = FormatTools.getBytesPerPixel(getPixelType());
     int pixel = bpp * getRGBChannelCount();
 
     long maxFP = no == getImageCount() - 1 ?
       in.length() : offsets[series][no + 1];
 
     CodecOptions options = new CodecOptions();
     options.littleEndian = isLittleEndian();
     options.interleaved = isInterleaved();
     options.maxBytes = (int) maxFP;
 
     if (isJPEG) {
       byte[] tmp = new JPEG2000Codec().decompress(in, options);
       for (int row=y; row<h+y; row++) {
         System.arraycopy(tmp, pixel * row * getSizeX(), buf,
           pixel * w * (row - y), pixel * w);
       }
       System.arraycopy(tmp, 0, buf, 0, (int) Math.min(tmp.length, buf.length));
       tmp = null;
     }
     else if (isLossless) {
       // plane is compressed using ZLIB
 
       int effectiveX = getSizeX();
       if ((getSizeX() % 2) != 0) effectiveX++;
       byte[] t = new ZlibCodec().decompress(in, options);
 
       for (int row=0; row<h; row++) {
         int offset = (row + y) * effectiveX * pixel + x * pixel;
         if (offset + w * pixel <= t.length) {
           System.arraycopy(t, offset, buf, row * w * pixel, w * pixel);
         }
       }
     }
     else {
       // plane is not compressed
       readPlane(in, x, y, w, h, buf);
     }
     return buf;
   }
 
   // -- IFormatHandler API methods --
 
   /* @see loci.formats.IFormatHandler#close() */
   public void close() throws IOException {
     super.close();
 
     offsets = null;
     zs.clear();
     ts.clear();
     isJPEG = isLossless = false;
     numSeries = 0;
     tsT.clear();
 
     pixelSizeX = pixelSizeY = pixelSizeZ = 0f;
     voltage = mag = na = objectiveModel = immersion = null;
     channelNames = null;
     binning = null;
     speed = null;
     gain = null;
     temperature = null;
     exposureTime = null;
     modality = null;
     exWave = null;
     emWave = null;
     power = null;
     cameraModel = null;
   }
 
   // -- Internal FormatReader API methods --
 
   /* @see loci.formats.FormatReader#initFile(String) */
   protected void initFile(String id) throws FormatException, IOException {
     if (debug) debug("ND2Reader.initFile(" + id + ")");
     super.initFile(id);
 
     channelNames = new Vector();
     binning = new Vector();
     speed = new Vector();
     gain = new Vector();
     temperature = new Vector();
     exposureTime = new Vector();
     modality = new Vector();
     exWave = new Vector();
     emWave = new Vector();
     power = new Vector();
 
     in = new RandomAccessStream(id);
 
     if (in.read() == -38 && in.read() == -50) {
       // newer version of ND2 - doesn't use JPEG2000
 
       isJPEG = false;
       in.seek(0);
       in.order(true);
 
       // assemble offsets to each block
 
       Vector imageOffsets = new Vector();
       Vector imageLengths = new Vector();
       Vector xmlOffsets = new Vector();
       Vector xmlLengths = new Vector();
       Vector customDataOffsets = new Vector();
       Vector customDataLengths = new Vector();
 
       while (in.getFilePointer() < in.length() && in.getFilePointer() >= 0) {
         while (in.read() != -38);
         in.skipBytes(3);
 
         int lenOne = in.readInt();
         int lenTwo = in.readInt();
         int len = lenOne + lenTwo;
         in.skipBytes(4);
 
         String blockType = in.readString(12);
         long fp = in.getFilePointer() - 12;
         in.skipBytes(len - 12);
 
         if (blockType.startsWith("ImageDataSeq")) {
           imageOffsets.add(new Long(fp));
           imageLengths.add(new Point(lenOne, lenTwo));
         }
         else if (blockType.startsWith("Image")) {
           xmlOffsets.add(new Long(fp));
           xmlLengths.add(new Point(lenOne, lenTwo));
         }
         else if (blockType.startsWith("CustomData|A")) {
           customDataOffsets.add(new Long(fp));
           customDataLengths.add(new Point(lenOne, lenTwo));
         }
       }
 
       // parse XML blocks
 
       DefaultHandler handler = new ND2Handler();
       ByteVector xml = new ByteVector();
 
       for (int i=0; i<xmlOffsets.size(); i++) {
         long offset = ((Long) xmlOffsets.get(i)).longValue();
         Point p = (Point) xmlLengths.get(i);
         int length = (int) (p.x + p.y);
 
         byte[] b = new byte[length];
         in.seek(offset);
         in.read(b);
 
         // strip out invalid characters
         int off = 0;
         for (int j=0; j<length; j++) {
           char c = (char) b[j];
           if ((off == 0 && c == '!') || c == 0) off = j + 1;
           if (Character.isISOControl(c) || !Character.isDefined(c)) {
             b[j] = (byte) ' ';
           }
         }
 
         if (length - off >= 5 && b[off] == '<' && b[off + 1] == '?' &&
           b[off + 2] == 'x' && b[off + 3] == 'm' && b[off + 4] == 'l')
         {
           boolean endBracketFound = false;
           while (!endBracketFound) {
             if (b[off++] == '>') {
               endBracketFound = true;
             }
           }
           xml.add(b, off, b.length - off);
         }
       }
 
       String xmlString = new String(xml.toByteArray());
       xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ND2>" +
         xmlString + "</ND2>";
       DataTools.parseXML(xmlString, handler);
 
       // rearrange image data offsets
 
       if (numSeries == 0) numSeries = 1;
 
       if (getSizeZ() == 0) {
         for (int i=0; i<getSeriesCount(); i++) {
           core[i].sizeZ = 1;
         }
       }
       if (getSizeT() == 0) {
         for (int i=0; i<getSeriesCount(); i++) {
           core[i].sizeT = 1;
         }
       }
 
       // calculate the image count
       for (int i=0; i<getSeriesCount(); i++) {
         core[i].imageCount = getSizeZ() * getSizeT() * getSizeC();
         if (imageOffsets.size() < core[i].imageCount) {
           core[i].imageCount /= getSizeC();
         }
         if (core[i].imageCount > imageOffsets.size() / getSeriesCount()) {
           if (core[i].imageCount == imageOffsets.size()) {
             CoreMetadata originalCore = core[0];
             core = new CoreMetadata[] {originalCore};
             numSeries = 1;
             break;
           }
           else {
             core[i].imageCount = imageOffsets.size() / getSeriesCount();
             core[i].sizeZ = 1;
             core[i].sizeT = core[i].imageCount;
           }
         }
       }
 
       offsets = new long[numSeries][getImageCount()];
 
       for (int i=0; i<imageOffsets.size(); i++) {
         long offset = ((Long) imageOffsets.get(i)).longValue();
         Point p = (Point) imageLengths.get(i);
         int length = (int) (p.x + p.y);
 
         in.seek(offset);
         byte[] b = new byte[length];
         in.read(b);
 
         StringBuffer sb = new StringBuffer();
         int pt = 13;
         while (b[pt] != '!') {
           sb.append((char) b[pt++]);
         }
         int ndx = Integer.parseInt(sb.toString());
 
         if (getSizeC() == 0) {
           int sizeC = length / (getSizeX() * getSizeY() *
             FormatTools.getBytesPerPixel(getPixelType()));
           for (int q=0; q<getSeriesCount(); q++) {
             core[q].sizeC = sizeC;
           }
         }
 
         int seriesIndex = ndx / (getSizeT() * getSizeZ());
         int plane = ndx % (getSizeT() * getSizeZ());
 
         if (seriesIndex < offsets.length && plane < offsets[seriesIndex].length)
         {
           offsets[seriesIndex][plane] = offset + p.x + 8;
         }
         b = null;
       }
 
       Vector tmpOffsets = new Vector();
       for (int i=0; i<offsets.length; i++) {
         if (offsets[i][0] > 0) tmpOffsets.add(offsets[i]);
       }
 
       offsets = new long[tmpOffsets.size()][];
       for (int i=0; i<tmpOffsets.size(); i++) {
         offsets[i] = (long[]) tmpOffsets.get(i);
       }
 
       if (offsets.length != getSeriesCount()) {
         int x = getSizeX();
         int y = getSizeY();
         int c = getSizeC();
         int pixelType = getPixelType();
         boolean rgb = isRGB();
         core = new CoreMetadata[offsets.length];
         for (int i=0; i<offsets.length; i++) {
           core[i] = new CoreMetadata();
           core[i].sizeX = x;
           core[i].sizeY = y;
           core[i].sizeC = c == 0 ? 1 : c;
           core[i].pixelType = pixelType;
           core[i].rgb = rgb;
           core[i].sizeZ = 1;
 
           int invalid = 0;
           for (int q=0; q<offsets[i].length; q++) {
             if (offsets[i][q] == 0) invalid++;
           }
           core[i].imageCount = offsets[i].length - invalid;
           core[i].sizeT = core[i].imageCount / (rgb ? 1 : core[i].sizeC);
           if (core[i].sizeT == 0) core[i].sizeT = 1;
         }
       }
       else {
         for (int i=0; i<getSeriesCount(); i++) {
           core[i].sizeX = getSizeX();
           core[i].sizeY = getSizeY();
           core[i].sizeC = getSizeC() == 0 ? 1 : getSizeC();
           core[i].sizeZ = getSizeZ() == 0 ? 1 : getSizeZ();
           core[i].sizeT = getSizeT() == 0 ? 1 : getSizeT();
           core[i].imageCount = getImageCount();
           core[i].pixelType = getPixelType();
         }
       }
 
       for (int i=0; i<getSeriesCount(); i++) {
         core[i].dimensionOrder = "XYCZT";
         core[i].rgb = getSizeC() > 1;
         core[i].littleEndian = true;
         core[i].interleaved = true;
         core[i].indexed = false;
         core[i].falseColor = false;
         core[i].metadataComplete = true;
         core[i].imageCount = core[i].sizeZ * core[i].sizeT;
         if (!core[i].rgb) core[i].imageCount *= core[i].sizeC;
       }
 
       // read first CustomData block
 
       if (customDataOffsets.size() > 0) {
         in.seek(((Long) customDataOffsets.get(0)).longValue());
         Point p = (Point) customDataLengths.get(0);
         int len = (int) (p.x + p.y);
 
         int timestampBytes = imageOffsets.size() * 8;
         in.skipBytes(len - timestampBytes);
 
         // the acqtimecache is a undeliniated stream of doubles
 
         for (int series=0; series<getSeriesCount(); series++) {
           for (int plane=0; plane<getImageCount(); plane++) {
             // timestamps are stored in ms; we want them in seconds
             double time = in.readDouble() / 1000;
             tsT.add(new Double(time));
             addMeta("series " + series + " timestamp " + plane, time);
           }
         }
       }
 
       populateMetadataStore();
       return;
     }
     else in.seek(0);
 
     // older version of ND2 - uses JPEG 2000 compression
 
     isJPEG = true;
 
     status("Calculating image offsets");
 
     Vector vs = new Vector();
 
     long pos = in.getFilePointer();
     boolean lastBoxFound = false;
     int length = 0;
     int box = 0;
 
     // assemble offsets to each plane
 
     int x = 0, y = 0, c = 0, type = 0;
 
     while (!lastBoxFound) {
       pos = in.getFilePointer();
       length = in.readInt();
       long nextPos = pos + length;
       if (nextPos < 0 || nextPos >= in.length() || length == 0) {
         lastBoxFound = true;
       }
       box = in.readInt();
       pos = in.getFilePointer();
       length -= 8;
 
       if (box == 0x6a703263) {
         vs.add(new Long(pos));
       }
       else if (box == 0x6a703268) {
         in.skipBytes(4);
         String s = in.readString(4);
         if (s.equals("ihdr")) {
           y = in.readInt();
           x = in.readInt();
           c = in.readShort();
           type = in.readInt();
           if (type == 0xf070100 || type == 0xf070000) type = FormatTools.UINT16;
           else type = FormatTools.UINT8;
         }
       }
       if (!lastBoxFound && box != 0x6a703268) in.skipBytes(length);
     }
 
     status("Finding XML metadata");
 
     // read XML metadata from the end of the file
 
     in.seek(((Long) vs.get(vs.size() - 1)).longValue());
 
     boolean found = false;
     long off = -1;
     byte[] buf = new byte[8192];
     while (!found && in.getFilePointer() < in.length()) {
       int read = 0;
       if (in.getFilePointer() == ((Long) vs.get(vs.size() - 1)).longValue()) {
         read = in.read(buf);
       }
       else {
         System.arraycopy(buf, buf.length - 10, buf, 0, 10);
         read = in.read(buf, 10, buf.length - 10);
       }
 
       if (read == buf.length) read -= 10;
       for (int i=0; i<read+9; i++) {
         if (buf[i] == (byte) 0xff && buf[i+1] == (byte) 0xd9) {
           found = true;
           off = in.getFilePointer() - (read + 10) + i;
           i = buf.length;
           break;
         }
       }
     }
 
     buf = null;
 
     status("Parsing XML");
 
     if (off > 0 && off < in.length() - 5 && (in.length() - off - 5) > 14) {
       in.seek(off + 5);
       String xml = in.readString((int) (in.length() - in.getFilePointer()));
       StringTokenizer st = new StringTokenizer(xml, "\n");
       StringBuffer sb = new StringBuffer();
 
       // stored XML doesn't have a root node - add one, so that we can parse
       // using SAX
 
       sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><NIKON>");
 
       while (st.hasMoreTokens()) {
         String token = st.nextToken().trim();
         if (token.indexOf("<!--") != -1 || token.indexOf("VCAL") != -1) {
           continue;
         }
         if (token.startsWith("<")) sb.append(token);
       }
 
       sb.append("</NIKON>");
       xml = sb.toString();
 
       status("Finished assembling XML string");
 
       DefaultHandler handler = new ND2Handler();
 
       // strip out invalid characters
       int offset = 0;
       byte[] b = xml.getBytes();
       int len = b.length;
       for (int i=0; i<len; i++) {
         char ch = (char) b[i];
         if (offset == 0 && ch == '!') offset = i + 1;
 
         if (Character.isISOControl(ch) || !Character.isDefined(ch)) {
           b[i] = (byte) ' ';
         }
       }
       DataTools.parseXML(new String(b, offset, len - offset), handler);
       xml = null;
     }
 
     status("Populating metadata");
 
     core[0].pixelType = FormatTools.UINT8;
     offsets = new long[1][2];
     offsets[0][0] = ((Long) vs.get(0)).longValue();
     if (offsets[0].length > 1 && vs.size() > 1) {
       offsets[0][1] = ((Long) vs.get(1)).longValue();
     }
 
     in.seek(offsets[0][0]);
 
     if (getSizeC() == 0) core[0].sizeC = 1;
     int numBands = c;
     c = numBands > 1 ? numBands : getSizeC();
     if (numBands == 1 && getImageCount() == 1) c = 1;
     for (int i=0; i<getSeriesCount(); i++) {
       core[i].sizeC = c;
       core[i].rgb = numBands > 1;
       core[i].pixelType = type;
     }
 
     if (getImageCount() == 0) {
       core[0].imageCount = vs.size();
       core[0].sizeZ = (int) Math.max(zs.size(), 1);
       core[0].sizeT = (int) Math.max(ts.size(), 1);
       int channels = isRGB() ? 1 : getSizeC();
       if (channels * getSizeZ() * getSizeT() != getImageCount()) {
         core[0].sizeZ = 1;
         core[0].sizeT = getImageCount() / channels;
         core[0].imageCount = getSizeZ() * getSizeT() * channels;
       }
     }
 
     if (getSizeZ() == 0) core[0].sizeZ = 1;
     if (getSizeT() == 0) core[0].sizeT = 1;
 
     for (int i=0; i<getSeriesCount(); i++) {
       core[i].sizeZ = getSizeZ();
       core[i].sizeT = getSizeT();
       core[i].imageCount = getSizeZ() * getSizeT() * (isRGB() ? 1 : getSizeC());
       core[i].dimensionOrder = "XYCZT";
       core[i].sizeX = x;
       core[i].sizeY = y;
       core[i].interleaved = false;
       core[i].littleEndian = false;
       core[i].metadataComplete = true;
     }
 
     if (numSeries == 0) numSeries = 1;
     offsets = new long[numSeries][getImageCount()];
 
     int nplanes = getSizeZ() * getEffectiveSizeC();
     for (int i=0; i<getSizeT(); i++) {
       for (int j=0; j<numSeries; j++) {
         for (int q=0; q<nplanes; q++) {
           offsets[j][i*nplanes + q] = ((Long) vs.remove(0)).longValue();
         }
       }
     }
 
     populateMetadataStore();
   }
 
   // -- Helper class --
 
   /** SAX handler for parsing XML. */
   class ND2Handler extends DefaultHandler {
     private String prefix = null;
 
     public void endElement(String uri, String localName, String qName,
       Attributes attributes)
     {
       if (qName.equals("CalibrationSeq") || qName.equals("MetadataSeq")) {
         prefix = null;
       }
     }
 
     public void startElement(String uri, String localName, String qName,
       Attributes attributes)
     {
       String value = attributes.getValue("value");
       if (qName.equals("uiWidth")) {
         core[0].sizeX = Integer.parseInt(value);
       }
       else if (qName.equals("uiWidthBytes") || qName.equals("uiBpcInMemory")) {
         int div = qName.equals("uiWidthBytes") ? getSizeX() : 8;
         int bytes = Integer.parseInt(value) / div;
 
         switch (bytes) {
           case 2:
             core[0].pixelType = FormatTools.UINT16;
             break;
           case 4:
             core[0].pixelType = FormatTools.UINT32;
             break;
           default:
             core[0].pixelType = FormatTools.UINT8;
         }
         parseKeyAndValue(qName, value);
       }
       else if (qName.startsWith("item_")) {
         int v = Integer.parseInt(qName.substring(qName.indexOf("_") + 1));
         if (v == numSeries) numSeries++;
       }
       else if (qName.equals("uiCompCount")) {
         int v = Integer.parseInt(value);
         core[0].sizeC = (int) Math.max(getSizeC(), v);
       }
       else if (qName.equals("uiHeight")) {
         core[0].sizeY = Integer.parseInt(value);
       }
       else if (qName.startsWith("TextInfo")) {
         parseKeyAndValue(qName, attributes.getValue("Text"));
         parseKeyAndValue(qName, value);
       }
       else if (qName.equals("dCompressionParam")) {
         isLossless = Integer.parseInt(value) > 0;
         parseKeyAndValue(qName, value);
       }
       else if (qName.equals("CalibrationSeq") || qName.equals("MetadataSeq")) {
         prefix = qName + " " + attributes.getValue("_SEQUENCE_INDEX");
       }
       else {
         StringBuffer sb = new StringBuffer();
         if (prefix != null) {
           sb.append(prefix);
           sb.append(" ");
         }
         sb.append(qName);
         parseKeyAndValue(sb.toString(), value);
       }
     }
   }
 
   // -- Helper methods --
 
   private void populateMetadataStore() {
     MetadataStore store =
       new FilterMetadata(getMetadataStore(), isMetadataFiltered());
     MetadataTools.populatePixels(store, this, true);
 
     store.setInstrumentID("Instrument:0", 0);
 
     // populate Image data
     for (int i=0; i<getSeriesCount(); i++) {
       store.setImageName("Series " + i, i);
       MetadataTools.setDefaultCreationDate(store, currentId, i);
 
       // link Instrument and Image
       store.setImageInstrumentRef("Instrument:0", i);
     }
 
     // populate Dimensions data
     for (int i=0; i<getSeriesCount(); i++) {
       store.setDimensionsPhysicalSizeX(new Float(pixelSizeX), i, 0);
       store.setDimensionsPhysicalSizeY(new Float(pixelSizeY), i, 0);
       store.setDimensionsPhysicalSizeZ(new Float(pixelSizeZ), i, 0);
     }
 
     // populate PlaneTiming data
     for (int i=0; i<getSeriesCount(); i++) {
       if (tsT.size() > 0) {
         setSeries(i);
         for (int n=0; n<getImageCount(); n++) {
           int[] coords = getZCTCoords(n);
           int stampIndex = coords[2];
           if (tsT.size() == getImageCount()) stampIndex = n;
           float stamp = ((Double) tsT.get(stampIndex)).floatValue();
           store.setPlaneTimingDeltaT(new Float(stamp), i, 0, n);
 
           int index = i * getSizeC() + coords[1];
           if (index < exposureTime.size()) {
             store.setPlaneTimingExposureTime(
               (Float) exposureTime.get(index), i, 0, n);
           }
         }
       }
     }
 
     store.setDetectorID("Detector:0", 0, 0);
     store.setDetectorModel(cameraModel, 0, 0);
     store.setDetectorType("Unknown", 0, 0);
 
     for (int i=0; i<getSeriesCount(); i++) {
       for (int c=0; c<getSizeC(); c++) {
         int index = i * getSizeC() + c;
         if (index < channelNames.size()) {
           store.setLogicalChannelName((String) channelNames.get(index), i, c);
         }
         if (index < modality.size()) {
           store.setLogicalChannelMode((String) modality.get(index), i, c);
         }
         if (index < emWave.size()) {
           store.setLogicalChannelEmWave((Integer) emWave.get(index), i, c);
         }
         if (index < exWave.size()) {
           store.setLogicalChannelExWave((Integer) exWave.get(index), i, c);
         }
         if (index < binning.size()) {
           store.setDetectorSettingsBinning((String) binning.get(index), i, c);
         }
         if (index < gain.size()) {
           store.setDetectorSettingsGain((Float) gain.get(index), i, c);
         }
         if (index < speed.size()) {
           store.setDetectorSettingsReadOutRate((Float) speed.get(index), i, c);
         }
         store.setDetectorSettingsDetector("Detector:0", i, c);
       }
     }
 
     for (int i=0; i<getSeriesCount(); i++) {
       if (i * getSizeC() < temperature.size()) {
         Float temp = (Float) temperature.get(i * getSizeC());
         store.setImagingEnvironmentTemperature(temp, i);
       }
     }
 
     // populate DetectorSettings
     if (voltage != null) {
       store.setDetectorSettingsVoltage(new Float(voltage), 0, 0);
     }
 
     // populate Objective
     if (na != null) store.setObjectiveLensNA(new Float(na), 0, 0);
     if (mag != null) {
       store.setObjectiveCalibratedMagnification(new Float(mag), 0, 0);
     }
     if (objectiveModel != null) {
       store.setObjectiveModel(objectiveModel, 0, 0);
     }
     if (immersion == null) immersion = "Unknown";
     store.setObjectiveImmersion(immersion, 0, 0);
     store.setObjectiveCorrection("Unknown", 0, 0);
 
     // link Objective to Image
     store.setObjectiveID("Objective:0", 0, 0);
     for (int i=0; i<getSeriesCount(); i++) {
       store.setObjectiveSettingsObjective("Objective:0", 0);
     }
 
     setSeries(0);
   }
 
   private void parseKeyAndValue(String key, String value) {
     if (key == null || value == null) return;
     addMeta(key, value);
     if (key.endsWith("dCalibration")) {
       pixelSizeX = Float.parseFloat(value);
       pixelSizeY = pixelSizeX;
     }
     else if (key.endsWith("dZStep")) pixelSizeZ = Float.parseFloat(value);
     else if (key.endsWith("Gain")) gain.add(new Float(value));
     else if (key.endsWith("dLampVoltage")) voltage = value;
     else if (key.endsWith("dObjectiveMag")) mag = value;
     else if (key.endsWith("dObjectiveNA")) na = value;
     else if (key.equals("sObjective") || key.equals("wsObjectiveName")) {
       String[] tokens = value.split(" ");
       int magIndex = -1;
       for (int i=0; i<tokens.length; i++) {
         if (tokens[i].endsWith("x")) {
           magIndex = i;
           break;
         }
       }
       StringBuffer model = new StringBuffer();
       for (int i=0; i<magIndex; i++) {
         model.append(tokens[i]);
         if (i < magIndex - 1) model.append(" ");
       }
       objectiveModel = model.toString();
       immersion = tokens[magIndex + 1];
     }
     else if (key.endsWith("dTimeMSec")) {
       long v = (long) Double.parseDouble(value);
       if (!ts.contains(new Long(v))) {
         ts.add(new Long(v));
         addMeta("number of timepoints", ts.size());
       }
     }
     else if (key.endsWith("dZPos")) {
       long v = (long) Double.parseDouble(value);
       if (!zs.contains(new Long(v))) {
         zs.add(new Long(v));
       }
     }
     else if (key.endsWith("uiCount")) {
       if (getSizeT() == 0) {
         core[0].sizeT = Integer.parseInt(value);
       }
     }
     else if (key.equals("VirtualComponents")) {
       if (getSizeC() == 0) {
         core[0].sizeC = Integer.parseInt(value);
       }
     }
     else if (key.startsWith("TextInfoItem") || key.endsWith("TextInfoItem")) {
       metadata.remove(key);
       value = value.replaceAll("&#x000d;&#x000a;", "\n");
       StringTokenizer tokens = new StringTokenizer(value, "\n");
       while (tokens.hasMoreTokens()) {
         String t = tokens.nextToken().trim();
         if (t.startsWith("Dimensions:")) {
           t = t.substring(11);
           StringTokenizer dims = new StringTokenizer(t, " x ");
 
           core[0].sizeZ = 1;
           core[0].sizeT = 1;
           core[0].sizeC = 1;
 
           while (dims.hasMoreTokens()) {
             String dim = dims.nextToken().trim();
             int v = Integer.parseInt(dim.replaceAll("\\D", ""));
             v = (int) Math.max(v, 1);
             if (dim.startsWith("XY")) {
               numSeries = v;
               if (numSeries > 1) {
                 int x = getSizeX();
                 int y = getSizeY();
                 int z = getSizeZ();
                 int tSize = getSizeT();
                 int c = getSizeC();
                 core = new CoreMetadata[numSeries];
                 for (int i=0; i<numSeries; i++) {
                   core[i] = new CoreMetadata();
                   core[i].sizeX = x;
                   core[i].sizeY = y;
                   core[i].sizeZ = z == 0 ? 1 : z;
                   core[i].sizeC = c == 0 ? 1 : c;
                   core[i].sizeT = tSize == 0 ? 1 : tSize;
                 }
               }
             }
             else if (dim.startsWith("T")) core[0].sizeT = v;
             else if (dim.startsWith("Z")) core[0].sizeZ = v;
             else core[0].sizeC = v;
           }
 
           core[0].imageCount = getSizeZ() * getSizeC() * getSizeT();
         }
         else if (t.startsWith("Number of Picture Planes")) {
           core[0].sizeC = Integer.parseInt(t.replaceAll("\\D", ""));
         }
         else {
           String[] v = t.split(":");
           if (v.length == 2) {
             if (v[0].equals("Name")) {
               channelNames.add(v[1]);
             }
             else if (v[0].equals("Modality")) {
               modality.add(v[1]);
             }
             else if (v[0].equals("Camera Type")) {
               cameraModel = v[1];
             }
             else if (v[0].equals("Binning")) {
               binning.add(v[1]);
             }
             else if (v[0].equals("Readout Speed")) {
               int last = v[1].lastIndexOf(" ");
               if (last != -1) v[1] = v[1].substring(0, last);
               speed.add(new Float(v[1]));
             }
             else if (v[0].equals("Temperature")) {
               String temp = v[1].replaceAll("[\\D&&[^-.]]", "");
               temperature.add(new Float(temp));
             }
             else if (v[0].equals("Exposure")) {
               String[] s = v[1].trim().split(" ");
               try {
                 float time = Float.parseFloat(s[0]);
                 // TODO: check for other units
                 if (s[1].equals("ms")) time /= 1000;
                 exposureTime.add(new Float(time));
               }
               catch (NumberFormatException e) { }
             }
           }
           else if (v[0].startsWith("- Step")) {
             int space = v[0].indexOf(" ", v[0].indexOf("Step") + 1);
             int last = v[0].indexOf(" ", space + 1);
             if (last == -1) last = v[0].length();
             pixelSizeZ = Float.parseFloat(v[0].substring(space, last));
           }
           else if (v[0].equals("Line")) {
             String[] values = t.split(";");
             for (int q=0; q<values.length; q++) {
               int colon = values[q].indexOf(":");
               String nextKey = values[q].substring(0, colon).trim();
               String nextValue = values[q].substring(colon + 1).trim();
               if (nextKey.equals("Emission wavelength")) {
                 emWave.add(new Integer(nextValue));
               }
               else if (nextKey.equals("Excitation wavelength")) {
                 exWave.add(new Integer(nextValue));
               }
               else if (nextKey.equals("Power")) {
                 power.add(new Integer((int) Float.parseFloat(nextValue)));
               }
             }
           }
         }
       }
     }
   }
 
 }
