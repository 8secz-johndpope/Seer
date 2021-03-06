 // genericImageParser.java
 // (C) 2009 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 // first published 16.10.2009 on http://yacy.net
 //
 // This is a part of YaCy, a peer-to-peer based web search engine
 //
 // $LastChangedDate: 2009-10-11 02:12:19 +0200 (So, 11 Okt 2009) $
 // $LastChangedRevision: 6398 $
 // $LastChangedBy: orbiter $
 //
 // LICENSE
 // 
 // This program is free software; you can redistribute it and/or modify
 // it under the terms of the GNU General Public License as published by
 // the Free Software Foundation; either version 2 of the License, or
 // (at your option) any later version.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
 package net.yacy.document.parser.images;
 
 import java.awt.image.BufferedImage;
 import java.io.EOFException;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;import java.net.MalformedURLException;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 
 import javax.imageio.ImageIO;
 
 import com.drew.imaging.jpeg.JpegMetadataReader;
 import com.drew.metadata.Directory;
 import com.drew.metadata.Metadata;
 import com.drew.metadata.MetadataException;
 import com.drew.metadata.Tag;
 import com.sun.image.codec.jpeg.ImageFormatException;
 import com.sun.image.codec.jpeg.JPEGCodec;
 import com.sun.image.codec.jpeg.JPEGDecodeParam;
 import com.sun.image.codec.jpeg.JPEGImageDecoder;
 
 import net.yacy.cora.document.MultiProtocolURI;
 import net.yacy.document.AbstractParser;
 import net.yacy.document.Document;
 import net.yacy.document.Idiom;
 import net.yacy.document.ParserException;
 import net.yacy.document.parser.html.ImageEntry;
 import net.yacy.document.parser.images.bmpParser.IMAGEMAP;
 import net.yacy.kelondro.logging.Log;
 import net.yacy.kelondro.util.FileUtils;
 
 public class genericImageParser extends AbstractParser implements Idiom {
 
     /**
      * a list of mime types that are supported by this parser class
      * @see #getSupportedMimeTypes()
      */
     public static final Set<String> SUPPORTED_MIME_TYPES = new HashSet<String>();
     public static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<String>();
     static {
         SUPPORTED_EXTENSIONS.add("png");
         SUPPORTED_EXTENSIONS.add("gif");
         SUPPORTED_EXTENSIONS.add("jpg");
         SUPPORTED_EXTENSIONS.add("jpeg");
         SUPPORTED_EXTENSIONS.add("jpe");
         SUPPORTED_EXTENSIONS.add("bmp");
         SUPPORTED_MIME_TYPES.add("image/png");
         SUPPORTED_MIME_TYPES.add("image/gif");
         SUPPORTED_MIME_TYPES.add("image/jpg");
         SUPPORTED_MIME_TYPES.add("image/bmp");
     }
     
     public genericImageParser() {
         super("Generic Image Parser"); 
     }
     
     @SuppressWarnings("unchecked")
     @Override
     public Document parse(
             final MultiProtocolURI location, 
             final String mimeType, 
             final String documentCharset, 
             final InputStream sourceStream) throws ParserException, InterruptedException {
         
         ImageInfo ii = null;
         String title = null;
         String author = null;
         String keywords = null;
         String description = null;
         if (mimeType.equals("image/bmp") ||
             location.getFileExtension().equals("bmp")) {
             byte[] b;
             try {
                 b = FileUtils.read(sourceStream);
             } catch (IOException e) {
                 Log.logException(e);
                 throw new ParserException(e.getMessage(), location);
             }
             IMAGEMAP imap = bmpParser.parse(b);
             ii = parseJavaImage(location, imap.getImage());
         } else if (mimeType.equals("image/jpg") ||
                    location.getFileExtension().equals("jpg") ||
                    location.getFileExtension().equals("jpeg") ||
                    location.getFileExtension().equals("jpe")) {
             // use the exif parser from
             // http://www.drewnoakes.com/drewnoakes.com/code/exif/
             // javadoc is at: http://www.drewnoakes.com/drewnoakes.com/code/exif/javadoc/
             // a tutorial is at: http://www.drewnoakes.com/drewnoakes.com/code/exif/sampleUsage.html
             JPEGImageDecoder jpegDecoder = JPEGCodec.createJPEGDecoder(sourceStream);
             BufferedImage image = null;
             try {
                 image = jpegDecoder.decodeAsBufferedImage();
             } catch (ImageFormatException e) {
                Log.logException(e);
                 throw new ParserException(e.getMessage(), location);
             } catch (IOException e) {
                Log.logException(e);
                 throw new ParserException(e.getMessage(), location);
             }
             JPEGDecodeParam decodeParam = jpegDecoder.getJPEGDecodeParam();
             Metadata metadata = JpegMetadataReader.readMetadata(decodeParam);
             ii = parseJavaImage(location, image);
             
             Iterator<Directory> directories = (Iterator<Directory>) metadata.getDirectoryIterator();
             HashMap<String, String> props = new HashMap<String, String>();
             while (directories.hasNext()) {
                 Directory directory = directories.next();
                 Iterator<Tag> tags = (Iterator<Tag>) directory.getTagIterator();
                 while (tags.hasNext()) {
                     Tag tag = tags.next();
                     try {
                         props.put(tag.getTagName(), tag.getDescription());
                         ii.info.append(tag.getTagName() + ": " + tag.getDescription() + " .\n");
                     } catch (MetadataException e) {
                         Log.logException(e);
                     }
                 }
                 title = props.get("Image Description");
                 if (title == null || title.length() == 0) title = props.get("Headline");
                 if (title == null || title.length() == 0) title = props.get("Object Name");
                 
                 author = props.get("Artist");
                 if (author == null || author.length() == 0) author = props.get("Writer/Editor");
                 if (author == null || author.length() == 0) author = props.get("By-line");
                 if (author == null || author.length() == 0) author = props.get("Credit");
                 if (author == null || author.length() == 0) author = props.get("Make");
                 
                 keywords = props.get("Keywords");
                 if (keywords == null || keywords.length() == 0) keywords = props.get("Category");
                 if (keywords == null || keywords.length() == 0) keywords = props.get("Supplemental Category(s)");
                 
                 description = props.get("Caption/Abstract");
                 if (description == null || description.length() == 0) description = props.get("Country/Primary Location");
                 if (description == null || description.length() == 0) description = props.get("Province/State");
                 if (description == null || description.length() == 0) description = props.get("Copyright Notice");
             }
         } else {
             ii = parseJavaImage(location, sourceStream);
         }        
         
         final HashSet<String> languages = new HashSet<String>();
         final HashMap<MultiProtocolURI, String> anchors = new HashMap<MultiProtocolURI, String>();
         final HashMap<MultiProtocolURI, ImageEntry> images  = new HashMap<MultiProtocolURI, ImageEntry>();
         // add this image to the map of images
         String infoString = ii.info.toString();
         images.put(ii.location, new ImageEntry(location, "", ii.width, ii.height, -1));
         
         if (title == null) title = location.toNormalform(true, true);
         
         return new Document(
              location,
              mimeType,
              "UTF-8",
              languages,
              keywords == null ? new String[]{} : keywords.split(keywords.indexOf(',') > 0 ? "," : " "), // keywords
              title, // title
              author == null ? "" : author, // author
              location.getHost(), // Publisher
              new String[]{}, // sections
              description == null ? "" : description, // description
              infoString.getBytes(), // content text
              anchors, // anchors
              images,
              false); // images
     }
     
     public Set<String> supportedMimeTypes() {
         return SUPPORTED_MIME_TYPES;
     }
     
     public Set<String> supportedExtensions() {
         return SUPPORTED_EXTENSIONS;
     }
     
     public static ImageInfo parseJavaImage(
                             final MultiProtocolURI location,
                             final InputStream sourceStream) throws ParserException {
         BufferedImage image = null;
         try {
             ImageIO.setUseCache(false); // do not write a cache to disc; keep in RAM
             image = ImageIO.read(sourceStream);
         } catch (final EOFException e) {
             Log.logException(e);
             throw new ParserException(e.getMessage(), location);
         } catch (final IOException e) {
             Log.logException(e);
             throw new ParserException(e.getMessage(), location);
         }
         if (image == null) throw new ParserException("ImageIO returned NULL", location);
         return parseJavaImage(location, image);
     }
     
     public static ImageInfo parseJavaImage(
                             final MultiProtocolURI location,
                             final BufferedImage image) {
         ImageInfo ii = new ImageInfo(location);
         ii.image = image;
         
         // scan the image
         ii.height = ii.image.getHeight();
         ii.width = ii.image.getWidth();
         /*
         Raster raster = image.getData();
         int[] pixel = raster.getPixel(0, 0, (int[])null);
         long[] average = new long[pixel.length];
         for (int i = 0; i < average.length; i++) average[i] = 0L;
         int pc = 0;
         for (int x = width / 4; x < 3 * width / 4; x = x + 2) {
             for (int y = height / 4; y < 3 * height / 4; y = y + 2) {
                 pixel = raster.getPixel(x, y, pixel);
                 for (int i = 0; i < average.length; i++) average[i] += pixel[i];
                 pc++;
             }
         }
         */
         // get image properties
         String [] propNames = ii.image.getPropertyNames();
         if (propNames == null) propNames = new String[0];
         ii.info.append("\n");
         for (String propName: propNames) {
             ii.info.append(propName).append(" = ").append(ii.image.getProperty(propName)).append(" .\n");
         }
         // append also properties that we measured
         ii.info.append("width").append(": ").append(Integer.toString(ii.width)).append(" .\n");
         ii.info.append("height").append(": ").append(Integer.toString(ii.height)).append(" .\n");
         
         return ii;
     }
     
     public static class ImageInfo {
         public MultiProtocolURI location;
         public BufferedImage image;
         public StringBuilder info;
         public int height;
         public int width;
         public ImageInfo(final MultiProtocolURI location) {
             this.location = location;
             this.image = null;
             this.info = new StringBuilder();
             this.height = -1;
             this.width = -1;
         }
     }
     
     
     
     public static void main(final String[] args) {
         File image = new File(args[0]);
         genericImageParser parser = new genericImageParser();
         MultiProtocolURI uri;
         try {
             uri = new MultiProtocolURI("http://localhost/" + image.getName());
             Document document = parser.parse(uri, "image/" + uri.getFileExtension(), "UTF-8", new FileInputStream(image));
             System.out.println(document.toString());
         } catch (MalformedURLException e) {
             e.printStackTrace();
         } catch (FileNotFoundException e) {
             e.printStackTrace();
         } catch (ParserException e) {
             e.printStackTrace();
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }
     
 }
