 //
 // MRCReader.java
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
 
 import loci.common.RandomAccessInputStream;
 import loci.formats.FormatException;
 import loci.formats.FormatReader;
 import loci.formats.FormatTools;
 import loci.formats.MetadataTools;
 import loci.formats.meta.FilterMetadata;
 import loci.formats.meta.MetadataStore;
 
 /**
  * MRCReader is the file format reader for MRC files.
  * Specifications available at
  * http://bio3d.colorado.edu/imod/doc/mrc_format.txt
  *
  * <dl><dt><b>Source code:</b></dt>
  * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/in/MRCReader.java">Trac</a>,
  * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/in/MRCReader.java">SVN</a></dd></dl>
  */
 public class MRCReader extends FormatReader {
 
  // -- Constants --

  private static final String[] TYPES =
    new String[] {"mono", "tilt", "tilts", "lina", "lins"};

   // -- Fields --
 
   /** Number of bytes per pixel */
   private int bpp = 0;
 
   /** Size of extended header */
   private int extHeaderSize = 0;
 
   /** Flag set to true if we are using float data. */
   private boolean isFloat = false;
 
   // -- Constructor --
 
   /** Constructs a new MRC reader. */
   public MRCReader() {
     super("Medical Research Council", "mrc");
   }
 
   // -- IFormatReader API methods --
 
   /**
    * @see loci.formats.IFormatReader#openBytes(int, byte[], int, int, int, int)
    */
   public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
     throws FormatException, IOException
   {
     FormatTools.checkPlaneParameters(this, no, buf.length, x, y, w, h);
 
    in.seek(1024 + extHeaderSize + no * FormatTools.getPlaneSize(this));
     readPlane(in, x, y, w, h, buf);
 
     return buf;
   }
 
   // -- IFormatHandler API methods --
 
   /* @see loci.formats.IFormatHandler#close() */
   public void close() throws IOException {
     super.close();
     bpp = extHeaderSize = 0;
     isFloat = false;
   }
 
   // -- Internal FormatReader API methods --
 
   /* @see loci.formats.FormatReader#initFile(String) */
   public void initFile(String id) throws FormatException, IOException {
     debug("MRCReader.initFile(" + id + ")");
     super.initFile(id);
     in = new RandomAccessInputStream(id);
 
     status("Reading header");
 
     // check endianness
 
    in.seek(212);
     core[0].littleEndian = in.read() == 68;
 
     // read dimension information from 1024 byte header
 
     in.seek(0);
     in.order(core[0].littleEndian);
 
     core[0].sizeX = in.readInt();
     core[0].sizeY = in.readInt();
     core[0].sizeZ = in.readInt();
 
     core[0].sizeC = 1;
 
     int mode = in.readInt();
     switch (mode) {
       case 0:
         core[0].pixelType = FormatTools.UINT8;
         break;
       case 1:
         core[0].pixelType = FormatTools.INT16;
         break;
       case 6:
         core[0].pixelType = FormatTools.UINT16;
         break;
       case 2:
         isFloat = true;
         core[0].pixelType = FormatTools.FLOAT;
         break;
       case 3:
         core[0].pixelType = FormatTools.UINT32;
         break;
       case 4:
         isFloat = true;
         core[0].pixelType = FormatTools.DOUBLE;
         break;
       case 16:
         core[0].sizeC = 3;
         core[0].pixelType = FormatTools.UINT16;
         break;
     }
 
     bpp = FormatTools.getBytesPerPixel(getPixelType());
 
    in.skipBytes(12);

     // pixel size = xlen / mx
 
     int mx = in.readInt();
     int my = in.readInt();
     int mz = in.readInt();
 
     float xlen = in.readFloat();
     float ylen = in.readFloat();
     float zlen = in.readFloat();
 
     addGlobalMeta("Pixel size (X)", xlen / mx);
     addGlobalMeta("Pixel size (Y)", ylen / my);
     addGlobalMeta("Pixel size (Z)", zlen / mz);
 
     addGlobalMeta("Alpha angle", in.readFloat());
     addGlobalMeta("Beta angle", in.readFloat());
     addGlobalMeta("Gamma angle", in.readFloat());
 
     in.skipBytes(12);
 
     // min, max and mean pixel values
 
     addGlobalMeta("Minimum pixel value", in.readFloat());
     addGlobalMeta("Maximum pixel value", in.readFloat());
     addGlobalMeta("Mean pixel value", in.readFloat());
 
     in.skipBytes(4);
     extHeaderSize = in.readInt();
 
     in.skipBytes(64);
 
     int idtype = in.readShort();
 
    String type = (idtype >= 0 && idtype < TYPES.length) ? TYPES[idtype] :
       "unknown";
 
     addGlobalMeta("Series type", type);
     addGlobalMeta("Lens", in.readShort());
     addGlobalMeta("ND1", in.readShort());
     addGlobalMeta("ND2", in.readShort());
     addGlobalMeta("VD1", in.readShort());
     addGlobalMeta("VD2", in.readShort());
 
     for (int i=0; i<6; i++) {
       addGlobalMeta("Angle " + (i + 1), in.readFloat());
     }
 
     in.skipBytes(24);
 
     addGlobalMeta("Number of useful labels", in.readInt());
 
     for (int i=0; i<10; i++) {
       addGlobalMeta("Label " + (i + 1), in.readString(80));
     }
 
     in.skipBytes(extHeaderSize);
 
     status("Populating metadata");
 
     core[0].sizeT = 1;
     core[0].dimensionOrder = "XYZTC";
     core[0].imageCount = getSizeZ();
     core[0].rgb = false;
     core[0].interleaved = true;
     core[0].indexed = false;
     core[0].falseColor = false;
     core[0].metadataComplete = true;
 
     MetadataStore store =
       new FilterMetadata(getMetadataStore(), isMetadataFiltered());
     MetadataTools.populatePixels(store, this);
     MetadataTools.setDefaultCreationDate(store, id, 0);
 
     float x = (xlen / mx) == Float.POSITIVE_INFINITY ? 1f : (xlen / mx);
     float y = (ylen / my) == Float.POSITIVE_INFINITY ? 1f : (ylen / my);
     float z = (zlen / mz) == Float.POSITIVE_INFINITY ? 1f : (zlen / mz);
 
     store.setDimensionsPhysicalSizeX(new Float(x), 0, 0);
     store.setDimensionsPhysicalSizeY(new Float(y), 0, 0);
     store.setDimensionsPhysicalSizeZ(new Float(z), 0, 0);
   }
 
 }
