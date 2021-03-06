 //
 // MicromanagerReader.java
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
 
 import java.io.*;
 import java.util.*;
 import loci.common.*;
 import loci.formats.*;
 import loci.formats.meta.FilterMetadata;
 import loci.formats.meta.MetadataStore;
 
 /**
  * MicromanagerReader is the file format reader for Micro-Manager files.
  *
  * <dl><dt><b>Source code:</b></dt>
  * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/bio-formats/src/loci/formats/in/MicromanagerReader.java">Trac</a>,
  * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/bio-formats/src/loci/formats/in/MicromanagerReader.java">SVN</a></dd></dl>
  */
 public class MicromanagerReader extends FormatReader {
 
   // -- Constants --
 
   /** File containing extra metadata. */
   private static final String METADATA = "metadata.txt";
 
   // -- Fields --
 
   /** Helper reader for TIFF files. */
   private MinimalTiffReader tiffReader;
 
   /** List of TIFF files to open. */
   private Vector tiffs;
 
   private String metadataFile;
 
   private String base;
   private String[] channels;
 
   // -- Constructor --
 
   /** Constructs a new Micromanager reader. */
   public MicromanagerReader() {
     super("Micro-Manager", new String[] {"tif", "tiff", "txt"});
   }
 
   // -- IFormatReader API methods --
 
   /* @see loci.formats.IFormatReader#isThisType(String, boolean) */
   public boolean isThisType(String name, boolean open) {
     if (name.equals(METADATA) || name.endsWith(File.separator + METADATA)) {
       try {
         RandomAccessStream stream = new RandomAccessStream(name);
         long length = stream.length();
         stream.close();
         return length > 0;
       }
       catch (IOException e) {
         return false;
       }
     }
     if (!open) return false; // not allowed to touch the file system
     Location parent = new Location(name).getAbsoluteFile().getParentFile();
     Location metaFile = new Location(parent, METADATA);
     return metaFile.exists() && metaFile.length() > 0;
   }
 
   /* @see loci.formats.IFormatReader#fileGroupOption(String) */
   public int fileGroupOption(String id) throws FormatException, IOException {
     return FormatTools.MUST_GROUP;
   }
 
   /* @see loci.formats.IFormatReader#isThisType(RandomAccessStream) */
   public boolean isThisType(RandomAccessStream stream) throws IOException {
     return tiffReader.isThisType(stream);
   }
 
   /* @see loci.formats.IFormatReader#getUsedFiles() */
   public String[] getUsedFiles() {
     FormatTools.assertId(currentId, true, 1);
     String[] s = new String[tiffs.size() + 1];
     tiffs.copyInto(s);
     s[tiffs.size()] = metadataFile;
     return s;
   }
 
   /**
    * @see loci.formats.IFormatReader#openBytes(int, byte[], int, int, int, int)
    */
   public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
     throws FormatException, IOException
   {
     FormatTools.assertId(currentId, true, 1);
     FormatTools.checkPlaneNumber(this, no);
     int[] coords = getZCTCoords(no);
     tiffReader.setId(base + coords[0] + "_" + channels[coords[1]] + "_" +
       coords[2] + ".tif");
     return tiffReader.openBytes(0, buf, x, y, w, h);
   }
 
   /* @see loci.formats.IFormatReader#close(boolean) */
   public void close(boolean fileOnly) throws IOException {
     if (fileOnly) {
       if (tiffReader != null) tiffReader.close(fileOnly);
     }
     else close();
   }
 
   // -- IFormatHandler API methods --
 
   /* @see loci.formats.IFormatHandler#close() */
   public void close() throws IOException {
     super.close();
     if (tiffReader != null) tiffReader.close();
     tiffReader = null;
     tiffs = null;
   }
 
   // -- Internal FormatReader API methods --
 
   /* @see loci.formats.FormatReader#initFile(String) */
   public void initFile(String id) throws FormatException, IOException {
     super.initFile(id);
     tiffReader = new MinimalTiffReader();
 
     status("Reading metadata file");
 
     // find metadata.txt
 
     Location file = new Location(currentId).getAbsoluteFile();
     metadataFile = file.exists() ? new Location(file.getParentFile(),
       METADATA).getAbsolutePath() : METADATA;
     in = new RandomAccessStream(metadataFile);
     String parent = file.exists() ?
       file.getParentFile().getAbsolutePath() + File.separator : "";
 
     // usually a small file, so we can afford to read it into memory
 
     byte[] meta = new byte[(int) in.length()];
     in.read(meta);
     String s = new String(meta);
     meta = null;
 
     status("Finding image file names");
 
     // first find the name of each TIFF file
     tiffs = new Vector();
     int pos = 0;
     while (true) {
       pos = s.indexOf("FileName", pos);
       if (pos == -1 || pos >= in.length()) break;
       String name = s.substring(s.indexOf(":", pos), s.indexOf(",", pos));
       tiffs.add(0, parent + name.substring(3, name.length() - 1));
       pos++;
     }
 
     base = (String) tiffs.get(0);
     base = base.substring(0, base.indexOf("_",
       base.lastIndexOf(File.separator)) + 1);
 
     // now parse the rest of the metadata
 
     // metadata.txt looks something like this:
     //
     // {
     //   "Section Name": {
     //      "Key": "Value",
     //      "Array key": [
     //        first array value, second array value
     //      ]
     //   }
     //
     // }
 
     status("Populating metadata");
 
     int start = s.indexOf("Summary");
     int end = s.indexOf("}", start);
     if (start != -1 && end > start) {
       s = s.substring(s.indexOf("\n", start), end).trim();
     }
 
     MetadataStore store =
       new FilterMetadata(getMetadataStore(), isMetadataFiltered());
 
     StringTokenizer st = new StringTokenizer(s, "\n");
     int[] slice = new int[3];
     while (st.hasMoreTokens()) {
       String token = st.nextToken();
       boolean open = token.indexOf("[") != -1;
       boolean closed = token.indexOf("]") != -1;
       if (open || (!open && !closed)) {
         int quote = token.indexOf("\"") + 1;
         String key = token.substring(quote, token.indexOf("\"", quote));
         String value = null;
 
         if (!open && !closed) {
           value = token.substring(token.indexOf(":") + 1);
         }
         else if (!closed){
           StringBuffer valueBuffer = new StringBuffer();
           while (!closed) {
             token = st.nextToken();
             closed = token.indexOf("]") != -1;
             valueBuffer.append(token);
           }
           value = valueBuffer.toString();
           value = value.replaceAll("\n", "");
         }
         else {
           value = token.substring(token.indexOf("[") + 1, token.indexOf("]"));
         }
         value = value.trim();
         value = value.substring(0, value.length() - 1);
         addMeta(key, value);
         if (key.equals("Channels")) core[0].sizeC = Integer.parseInt(value);
         else if (key.equals("ChNames")) {
           StringTokenizer t = new StringTokenizer(value, ",");
           int nTokens = t.countTokens();
           channels = new String[nTokens];
           for (int q=0; q<nTokens; q++) {
             channels[q] = t.nextToken().replaceAll("\"", "").trim();
           }
         }
       }
 
       if (token.trim().startsWith("\"FrameKey")) {
         int dash = token.indexOf("-") + 1;
         slice[2] = Integer.parseInt(token.substring(dash,
           token.indexOf("-", dash)));
         dash = token.indexOf("-", dash);
         slice[1] = Integer.parseInt(token.substring(dash,
           token.indexOf("-", dash)));
         dash = token.indexOf("-", dash);
         slice[0] = Integer.parseInt(token.substring(dash,
           token.indexOf("\"", dash)));
 
         token = st.nextToken().trim();
         String key = "", value = "";
         while (!token.startsWith("}")) {
           int colon = token.indexOf(":");
           key = token.substring(1, colon).trim();
           value = token.substring(colon + 1, token.length() - 1).trim();
 
           addMeta(key, value);
           token = st.nextToken().trim();
         }
       }
 
     }
     tiffReader.setId((String) tiffs.get(0));
 
    String z = (String) metadata.get("Slices");
    if (z != null) {
      core[0].sizeZ = Integer.parseInt(z);
    }
    else core[0].sizeZ = 1;

    String t = (String) metadata.get("Frames");
    if (t != null) {
      core[0].sizeT = Integer.parseInt(t);
    }
    else core[0].sizeT = tiffs.size() / getSizeC();
 
     core[0].sizeX = tiffReader.getSizeX();
     core[0].sizeY = tiffReader.getSizeY();
     core[0].dimensionOrder = "XYZCT";
     core[0].pixelType = tiffReader.getPixelType();
     core[0].rgb = tiffReader.isRGB();
     core[0].interleaved = false;
     core[0].littleEndian = tiffReader.isLittleEndian();
     core[0].imageCount = tiffs.size();
     core[0].indexed = false;
     core[0].falseColor = false;
     core[0].metadataComplete = true;
 
     store.setImageName("", 0);
     MetadataTools.setDefaultCreationDate(store, id, 0);
     MetadataTools.populatePixels(store, this);
   }
 
 }
