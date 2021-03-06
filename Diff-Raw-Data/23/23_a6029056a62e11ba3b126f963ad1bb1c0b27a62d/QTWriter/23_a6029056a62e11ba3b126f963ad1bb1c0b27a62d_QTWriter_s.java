 //
 // QTWriter.java
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
 
 package loci.formats.out;
 
 import java.io.IOException;
 import java.util.Vector;
 
 import loci.common.DataTools;
 import loci.common.RandomAccessInputStream;
 import loci.common.RandomAccessOutputStream;
 import loci.formats.FormatException;
 import loci.formats.FormatTools;
 import loci.formats.FormatWriter;
 import loci.formats.MetadataTools;
 import loci.formats.gui.LegacyQTTools;
 import loci.formats.meta.MetadataRetrieve;
 
 /**
  * QTWriter is the file format writer for uncompressed QuickTime movie files.
  *
  * <dl><dt><b>Source code:</b></dt>
  * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/out/QTWriter.java">Trac</a>,
  * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/out/QTWriter.java">SVN</a></dd></dl>
  *
  * @author Melissa Linkert linkert at wisc.edu
  */
 
 public class QTWriter extends FormatWriter {
 
   // -- Constants --
 
   // NB: Writing to Motion JPEG-B with QTJava seems to be broken.
   /** Value indicating Motion JPEG-B codec. */
   public static final int CODEC_MOTION_JPEG_B = 1835692130;
 
   /** Value indicating Cinepack codec. */
   public static final int CODEC_CINEPAK = 1668704612;
 
   /** Value indicating Animation codec. */
   public static final int CODEC_ANIMATION = 1919706400;
 
   /** Value indicating H.263 codec. */
   public static final int CODEC_H_263 = 1748121139;
 
   /** Value indicating Sorenson codec. */
   public static final int CODEC_SORENSON = 1398165809;
 
   /** Value indicating Sorenson 3 codec. */
   public static final int CODEC_SORENSON_3 = 0x53565133;
 
   /** Value indicating MPEG-4 codec. */
   public static final int CODEC_MPEG_4 = 0x6d703476;
 
   /** Value indicating Raw codec. */
   public static final int CODEC_RAW = 0;
 
   /** Value indicating Low quality. */
   public static final int QUALITY_LOW = 256;
 
   /** Value indicating Normal quality. */
   public static final int QUALITY_NORMAL = 512;
 
   /** Value indicating High quality. */
   public static final int QUALITY_HIGH = 768;
 
   /** Value indicating Maximum quality. */
   public static final int QUALITY_MAXIMUM = 1023;
 
   // -- Fields --
 
   /** Current file. */
   protected RandomAccessOutputStream out;
 
   /** The codec to use. */
   protected int codec = CODEC_RAW;
 
   /** The quality to use. */
   protected int quality = QUALITY_NORMAL;
 
   /** Number of planes written. */
   protected int numWritten;
 
   /** Seek to this offset to update the total number of pixel bytes. */
   protected long byteCountOffset;
 
   /** Total number of pixel bytes. */
   protected int numBytes;
 
   /** Vector of plane offsets. */
   protected Vector offsets;
 
   /** Time the file was created. */
   protected int created;
 
   /** Whether we need the legacy writer. */
   protected boolean needLegacy = false;
 
   /** Legacy QuickTime writer. */
   protected LegacyQTWriter legacy;
 
   // -- Constructor --
 
   public QTWriter() {
     super("QuickTime", "mov");
     LegacyQTTools tools = new LegacyQTTools();
     if (tools.canDoQT()) {
       compressionTypes = new String[] {
         "Uncompressed",
         // NB: Writing to Motion JPEG-B with QTJava seems to be broken.
         /*"Motion JPEG-B",*/
         "Cinepak", "Animation", "H.263", "Sorenson", "Sorenson 3", "MPEG 4"
       };
     }
     else compressionTypes = new String[] {"Uncompressed"};
   }
 
   // -- QTWriter API methods --
 
   /**
    * Sets the encoded movie's codec.
    * @param codec Codec value:<ul>
    *   <li>QTWriter.CODEC_CINEPAK</li>
    *   <li>QTWriter.CODEC_ANIMATION</li>
    *   <li>QTWriter.CODEC_H_263</li>
    *   <li>QTWriter.CODEC_SORENSON</li>
    *   <li>QTWriter.CODEC_SORENSON_3</li>
    *   <li>QTWriter.CODEC_MPEG_4</li>
    *   <li>QTWriter.CODEC_RAW</li>
    * </ul>
    */
   public void setCodec(int codec) { this.codec = codec; }
 
   /**
    * Sets the quality of the encoded movie.
    * @param quality Quality value:<ul>
    *   <li>QTWriter.QUALITY_LOW</li>
    *   <li>QTWriter.QUALITY_MEDIUM</li>
    *   <li>QTWriter.QUALITY_HIGH</li>
    *   <li>QTWriter.QUALITY_MAXIMUM</li>
    * </ul>
    */
   public void setQuality(int quality) { this.quality = quality; }
 
   // -- IFormatWriter API methods --
 
   /* @see loci.formats.IFormatWriter#saveBytes(byte[], int, boolean, boolean) */
   public void saveBytes(byte[] buf, int series, boolean lastInSeries,
     boolean last) throws FormatException, IOException
   {
     if (legacy == null) legacy = new LegacyQTWriter();
 
     if (needLegacy) {
       legacy.saveBytes(buf, last);
       return;
     }
 
     MetadataRetrieve r = getMetadataRetrieve();
     MetadataTools.verifyMinimumPopulated(r, series);
 
     // get the width and height of the image
     int width = r.getPixelsSizeX(series, 0).intValue();
     int height = r.getPixelsSizeY(series, 0).intValue();
 
     // need to check if the width is a multiple of 8
     // if it is, great; if not, we need to pad each scanline with enough
     // bytes to make the width a multiple of 8
 
     int bytesPerPixel =
       FormatTools.pixelTypeFromString(r.getPixelsPixelType(series, 0));
     int pad = ((4 - (width % 4)) % 4) * bytesPerPixel;
     Integer samples = r.getLogicalChannelSamplesPerPixel(series, 0);
     if (samples == null) {
       warn("SamplesPerPixel #0 is null.  It is assumed to be 1.");
     }
     int nChannels = samples == null ? 1 : samples.intValue();
 
     if (!initialized) {
       initialized = true;
       setCodec();
       if (codec != 0) {
         needLegacy = true;
         legacy.setCodec(codec);
         legacy.setMetadataRetrieve(getMetadataRetrieve());
         legacy.setId(currentId);
         legacy.saveBytes(buf, series, lastInSeries, last);
         return;
       }
 
       // -- write the header --
 
       offsets = new Vector();
       out = new RandomAccessOutputStream(currentId);
       created = (int) System.currentTimeMillis();
       numWritten = 0;
      numBytes = buf.length;
       byteCountOffset = 8;
 
       if (out.length() == 0) {
         // -- write the first header --
 
         DataTools.writeInt(out, 8, false);
         out.writeBytes("wide");
 
         DataTools.writeInt(out, numBytes + 8, false);
         out.writeBytes("mdat");
       }
       else {
         out.seek(byteCountOffset);
 
         RandomAccessInputStream in = new RandomAccessInputStream(currentId);
         in.seek(byteCountOffset);
         numBytes = (int) DataTools.read4UnsignedBytes(in, false) - 8;
         in.close();
 
        numWritten = numBytes / buf.length;
        numBytes += buf.length;
 
         out.seek(byteCountOffset);
         DataTools.writeInt(out, numBytes + 8, false);
 
         for (int i=0; i<numWritten; i++) {
          offsets.add(new Integer(16 + i * buf.length));
         }
 
         out.seek(out.length());
       }
 
       // -- write the first plane of pixel data (mdat) --
 
       offsets.add(new Integer((int) out.length()));
     }
     else {
       // update the number of pixel bytes written
       int planeOffset = numBytes;
      numBytes += buf.length;
       out.seek(byteCountOffset);
       DataTools.writeInt(out, numBytes + 8, false);
 
       // write this plane's pixel data
       out.seek(out.length());
 
       offsets.add(new Integer(planeOffset + 16));
     }
 
     // invert each pixel
     // this will makes the colors look right in other readers (e.g. xine),
     // but needs to be reversed in QTReader
 
     if (nChannels == 1 && bytesPerPixel == 1 && !needLegacy) {
       for (int i=0; i<buf.length; i++) {
         buf[i] = (byte) (255 - buf[i]);
       }
     }
 
     if (!interleaved) {
       // need to write interleaved data
       byte[] tmp = new byte[buf.length];
       System.arraycopy(buf, 0, tmp, 0, buf.length);
       for (int i=0; i<buf.length; i++) {
         int c = i / (width * height);
         int index = i % (width * height);
         buf[index * nChannels + c] = tmp[i];
       }
     }
 
    out.write(buf);
     numWritten++;
 
     if (last) {
       int timeScale = 100;
       int duration = numWritten * (timeScale / fps);
       int bitsPerPixel = (nChannels > 1) ? bytesPerPixel * 24 :
         bytesPerPixel * 8 + 32;
       if (bytesPerPixel == 2) {
         bitsPerPixel = nChannels > 1 ? 16 : 40;
       }
       int channels = (bitsPerPixel >= 40) ? 1 : 3;
 
       // -- write moov atom --
 
       int atomLength = 685 + 8*numWritten;
       DataTools.writeInt(out, atomLength, false);
       out.writeBytes("moov");
 
       // -- write mvhd atom --
 
       DataTools.writeInt(out, 108, false);
       out.writeBytes("mvhd");
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeInt(out, created, false); // creation time
       DataTools.writeInt(out, (int) System.currentTimeMillis(), false);
       DataTools.writeInt(out, timeScale, false); // time scale
       DataTools.writeInt(out, duration, false); // duration
       out.write(new byte[] {0, 1, 0, 0});  // preferred rate & volume
       out.write(new byte[] {0, -1, 0, 0, 0, 0, 0, 0, 0, 0}); // reserved
 
       // 3x3 matrix - tells reader how to rotate image
 
       DataTools.writeInt(out, 1, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 1, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 16384, false);
 
       DataTools.writeShort(out, 0, false); // not sure what this is
       DataTools.writeInt(out, 0, false); // preview duration
       DataTools.writeInt(out, 0, false); // preview time
       DataTools.writeInt(out, 0, false); // poster time
       DataTools.writeInt(out, 0, false); // selection time
       DataTools.writeInt(out, 0, false); // selection duration
       DataTools.writeInt(out, 0, false); // current time
       DataTools.writeInt(out, 2, false); // next track's id
 
       // -- write trak atom --
 
       atomLength -= 116;
       DataTools.writeInt(out, atomLength, false);
       out.writeBytes("trak");
 
       // -- write tkhd atom --
 
       DataTools.writeInt(out, 92, false);
       DataTools.writeString(out, "tkhd");
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 15, false); // flags
 
       DataTools.writeInt(out, created, false); // creation time
       DataTools.writeInt(out, (int) System.currentTimeMillis(), false);
       DataTools.writeInt(out, 1, false); // track id
       DataTools.writeInt(out, 0, false); // reserved
 
       DataTools.writeInt(out, duration, false); // duration
       DataTools.writeInt(out, 0, false); // reserved
       DataTools.writeInt(out, 0, false); // reserved
       DataTools.writeShort(out, 0, false); // reserved
 
       DataTools.writeInt(out, 0, false); // unknown
 
       // 3x3 matrix - tells reader how to rotate the image
 
       DataTools.writeInt(out, 1, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 1, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 0, false);
       DataTools.writeInt(out, 16384, false);
 
       DataTools.writeInt(out, width, false); // image width
       DataTools.writeInt(out, height, false); // image height
       DataTools.writeShort(out, 0, false); // reserved
 
       // -- write edts atom --
 
       DataTools.writeInt(out, 36, false);
       out.writeBytes("edts");
 
       // -- write elst atom --
 
       DataTools.writeInt(out, 28, false);
       out.writeBytes("elst");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeInt(out, 1, false); // number of entries in the table
       DataTools.writeInt(out, duration, false); // duration
       DataTools.writeShort(out, 0, false); // time
       DataTools.writeInt(out, 1, false); // rate
       DataTools.writeShort(out, 0, false); // unknown
 
       // -- write mdia atom --
 
       atomLength -= 136;
       DataTools.writeInt(out, atomLength, false);
       out.writeBytes("mdia");
 
       // -- write mdhd atom --
 
       DataTools.writeInt(out, 32, false);
       out.writeBytes("mdhd");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeInt(out, created, false); // creation time
       DataTools.writeInt(out, (int) System.currentTimeMillis(), false);
       DataTools.writeInt(out, timeScale, false); // time scale
       DataTools.writeInt(out, duration, false); // duration
       DataTools.writeShort(out, 0, false); // language
       DataTools.writeShort(out, 0, false); // quality
 
       // -- write hdlr atom --
 
       DataTools.writeInt(out, 58, false);
       out.writeBytes("hdlr");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       out.writeBytes("mhlr");
       out.writeBytes("vide");
       out.writeBytes("appl");
       out.write(new byte[] {16, 0, 0, 0, 0, 1, 1, 11, 25});
       out.writeBytes("Apple Video Media Handler");
 
       // -- write minf atom --
 
       atomLength -= 98;
       DataTools.writeInt(out, atomLength, false);
       out.writeBytes("minf");
 
       // -- write vmhd atom --
 
       DataTools.writeInt(out, 20, false);
       out.writeBytes("vmhd");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 1, false); // flags
       DataTools.writeShort(out, 64, false); // graphics mode
       DataTools.writeShort(out, 32768, false);  // opcolor 1
       DataTools.writeShort(out, 32768, false);  // opcolor 2
       DataTools.writeShort(out, 32768, false);  // opcolor 3
 
       // -- write hdlr atom --
 
       DataTools.writeInt(out, 57, false);
       out.writeBytes("hdlr");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       out.writeBytes("dhlr");
       out.writeBytes("alis");
       out.writeBytes("appl");
       out.write(new byte[] {16, 0, 0, 1, 0, 1, 1, 31, 24});
       out.writeBytes("Apple Alias Data Handler");
 
       // -- write dinf atom --
 
       DataTools.writeInt(out, 36, false);
       out.writeBytes("dinf");
 
       // -- write dref atom --
 
       DataTools.writeInt(out, 28, false);
       out.writeBytes("dref");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeShort(out, 0, false); // version 2
       DataTools.writeShort(out, 1, false); // flags 2
       out.write(new byte[] {0, 0, 0, 12});
       out.writeBytes("alis");
       DataTools.writeShort(out, 0, false); // version 3
       DataTools.writeShort(out, 1, false); // flags 3
 
       // -- write stbl atom --
 
       atomLength -= 121;
       DataTools.writeInt(out, atomLength, false);
       out.writeBytes("stbl");
 
       // -- write stsd atom --
 
       DataTools.writeInt(out, 118, false);
       out.writeBytes("stsd");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeInt(out, 1, false); // number of entries in the table
       out.write(new byte[] {0, 0, 0, 102});
       out.writeBytes("raw "); // codec
       out.write(new byte[] {0, 0, 0, 0, 0, 0});  // reserved
       DataTools.writeShort(out, 1, false); // data reference
       DataTools.writeShort(out, 1, false); // version
       DataTools.writeShort(out, 1, false); // revision
       out.writeBytes("appl");
       DataTools.writeInt(out, 0, false); // temporal quality
       DataTools.writeInt(out, 768, false); // spatial quality
       DataTools.writeShort(out, width, false); // image width
       DataTools.writeShort(out, height, false); // image height
       out.write(new byte[] {0, 72, 0, 0}); // horizontal dpi
       out.write(new byte[] {0, 72, 0, 0}); // vertical dpi
       DataTools.writeInt(out, 0, false); // data size
       DataTools.writeShort(out, 1, false); // frames per sample
       DataTools.writeShort(out, 12, false); // length of compressor name
       out.writeBytes("Uncompressed"); // compressor name
       DataTools.writeInt(out, bitsPerPixel, false); // unknown
       DataTools.writeInt(out, bitsPerPixel, false); // unknown
       DataTools.writeInt(out, bitsPerPixel, false); // unknown
       DataTools.writeInt(out, bitsPerPixel, false); // unknown
       DataTools.writeInt(out, bitsPerPixel, false); // unknown
       DataTools.writeShort(out, bitsPerPixel, false); // bits per pixel
       DataTools.writeInt(out, 65535, false); // ctab ID
       out.write(new byte[] {12, 103, 97, 108}); // gamma
       out.write(new byte[] {97, 1, -52, -52, 0, 0, 0, 0}); // unknown
 
       // -- write stts atom --
 
       DataTools.writeInt(out, 24, false);
       out.writeBytes("stts");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeInt(out, 1, false); // number of entries in the table
       DataTools.writeInt(out, numWritten, false); // number of planes
       DataTools.writeInt(out, (timeScale / fps), false); // frames per second
 
       // -- write stsc atom --
 
       DataTools.writeInt(out, 28, false);
       out.writeBytes("stsc");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeInt(out, 1, false); // number of entries in the table
       DataTools.writeInt(out, 1, false); // chunk
       DataTools.writeInt(out, 1, false); // samples
       DataTools.writeInt(out, 1, false); // id
 
       // -- write stsz atom --
 
       DataTools.writeInt(out, 20 + 4*numWritten, false);
       out.writeBytes("stsz");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeInt(out, 0, false); // sample size
       DataTools.writeInt(out, numWritten, false); // number of planes
       for (int i=0; i<numWritten; i++) {
         // sample size
         DataTools.writeInt(out, channels*height*(width+pad)*bytesPerPixel,
           false);
       }
 
       // -- write stco atom --
 
       DataTools.writeInt(out, 16 + 4*numWritten, false);
       out.writeBytes("stco");
 
       DataTools.writeShort(out, 0, false); // version
       DataTools.writeShort(out, 0, false); // flags
       DataTools.writeInt(out, numWritten, false); // number of planes
       for (int i=0; i<numWritten; i++) {
         // write the plane offset
         DataTools.writeInt(out, ((Integer) offsets.get(i)).intValue(), false);
       }
 
       out.close();
     }
   }
 
   /* @see loci.formats.IFormatWriter#canDoStacks() */
   public boolean canDoStacks() { return true; }
 
   /* @see loci.formats.IFormatWriter#getPixelTypes(String) */
   public int[] getPixelTypes() {
     return new int[] {FormatTools.UINT8};
   }
 
   // -- IFormatHandler API methods --
 
   /* @see loci.formats.IFormatHandler#close() */
   public void close() throws IOException {
     if (out != null) out.close();
     out = null;
     numWritten = 0;
     byteCountOffset = 0;
     numBytes = 0;
     created = 0;
     offsets = null;
     currentId = null;
     initialized = false;
   }
 
   // -- Helper methods --
 
   private void setCodec() {
     if (compression == null) return;
     if (compression.equals("Uncompressed")) codec = CODEC_RAW;
     // NB: Writing to Motion JPEG-B with QTJava seems to be broken.
     else if (compression.equals("Motion JPEG-B")) codec = CODEC_MOTION_JPEG_B;
     else if (compression.equals("Cinepak")) codec = CODEC_CINEPAK;
     else if (compression.equals("Animation")) codec = CODEC_ANIMATION;
     else if (compression.equals("H.263")) codec = CODEC_H_263;
     else if (compression.equals("Sorenson")) codec = CODEC_SORENSON;
     else if (compression.equals("Sorenson 3")) codec = CODEC_SORENSON_3;
     else if (compression.equals("MPEG 4")) codec = CODEC_MPEG_4;
   }
 
 }
