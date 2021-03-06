 /*
  * Copyright (c) 2007-2012 The Broad Institute, Inc.
  * SOFTWARE COPYRIGHT NOTICE
  * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  *
  * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  *
  * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
  * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
  */
 /**
  * SnapshotUtilities.java
  *
  * Created on November 29, 2007, 2:14 PM
  *
  * To change this template, choose Tools | Template Manager
  * and open the template in the editor.
  */
 package org.broad.igv.ui.util;
 
 import org.apache.batik.dom.GenericDOMImplementation;
 import org.apache.batik.svggen.SVGGraphics2D;
 import org.apache.log4j.Logger;
 import org.broad.igv.ui.panel.MainPanel;
 import org.broad.igv.ui.panel.Paintable;
 import org.w3c.dom.DOMImplementation;
 import org.w3c.dom.Document;
 
 import javax.imageio.ImageIO;
 import java.awt.*;
 import java.awt.image.BufferedImage;
 import java.io.*;
 
 /**
  * Utility methods for supporting saving of images as jpeg, png, and svg files.
  *
  * @author eflakes
  * @modified jrobinso
  */
 public class SnapshotUtilities {
 
     /**
      * Class logger
      */
     private static Logger log = Logger.getLogger(SnapshotUtilities.class);
 
 
     /**
      * The maximum height in pixels for snapshots of a panel.
      */
     public static int DEFAULT_MAX_PANEL_HEIGHT = 1000;
 
     /**
      * We need to use a static for max panel height,  or alternatively much refactoring, however this class might
      * be accessed by multiple threads which set this to different values => use a thread local
      */
     private static ThreadLocal<Integer> maxPanelHeight = new ThreadLocal() {
         @Override
         protected Object initialValue() {
             return new Integer(DEFAULT_MAX_PANEL_HEIGHT);
         }
     };
 
     public static int getMaxPanelHeight() {
         return maxPanelHeight.get().intValue();
     }
 
     public static void setMaxPanelHeight(int h) {
         maxPanelHeight.set(h);
     }
 
     // Treat this class as a singleton, no instances allowed
     private SnapshotUtilities() {
     }
 
 
     public static String doComponentSnapshot(Component component, File file, SnapshotFileChooser.SnapshotFileType type, boolean paintOffscreen) throws IOException{
 
         //TODO Should really make this work for more components
         if (paintOffscreen && !(component instanceof Paintable)) {
             log.error("Component cannot be painted offscreen. Performing onscreen paint");
             paintOffscreen = false;
         }
 
         if(paintOffscreen){
 
             Rectangle rect = component.getBounds();
 
             if(component instanceof MainPanel){
                 rect.height = ((MainPanel) component).getOffscreenImageHeight();
             }else{
                 rect.height = Math.min(component.getHeight(), getMaxPanelHeight());
             }
 
             // translate to (0, 0) if necessary
             int dx = rect.x;
             int dy = rect.y;
             rect.x = 0;
             rect.y = 0;
             rect.width -= dx;
             rect.height -= dy;
 
             component.setBounds(rect);
         }
 
         int width = component.getWidth();
         int height = component.getHeight();
 
         // Call appropriate converter
         String format = null;
         String[] exts = null;
         switch (type) {
             case SVG:
                 //log.debug("Exporting svg screenshot");
                 exportScreenshotSVG(component, file, paintOffscreen);
                 break;
             case JPEG:
                 format = "jpeg";
                 exts = new String[]{".jpg", ".jpeg"};
                 break;
             case PNG:
                 format = "png";
                 exts = new String[]{"." + format};
                 break;
         }
         if(format != null && exts != null){
             exportScreenShotBufferedImage(component, file, width, height, exts, format, paintOffscreen);
         }
         return "OK";
     }
 
     private static void exportScreenshotSVG(Component target, File selectedFile, boolean paintOffscreen) throws IOException {
 
         String format = "svg";
         selectedFile = fixFileExt(selectedFile, new String[]{format}, format);
 
         // Create an instance of org.w3c.dom.Document.
         DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
         String svgNS = "http://www.w3.org/2000/svg";
         Document document = domImpl.createDocument(svgNS, format, null);
 
         // Write image data into document
         SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
 
         choosePaint(target, svgGenerator, paintOffscreen);
 
         Writer out = null;
         try {
             // Finally, stream out SVG to the standard output using
             // UTF-8 encoding.
             boolean useCSS = true; // we want to use CSS style attributes
            out = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(selectedFile), "UTF-8"));
             svgGenerator.stream(out, useCSS);
         } finally {
             if (out != null) try {
                 out.close();
             } catch (IOException e) {
                 log.error("Error closing svg file", e);
             }
         }
 
     }
 
     private static void choosePaint(Component target, Graphics2D g, boolean paintOffscreen){
         log.debug("Painting to target " + target + " , offscreen " + paintOffscreen);
         if(paintOffscreen){
             ((Paintable) target).paintOffscreen(g, target.getBounds());
         }else{
             target.paintAll(g);
         }
     }
 
     /**
      *
      * Export the specified {@code target} component as a {@code BufferedImage} to the given file.
      * @param target
      * @param selectedFile
      * @param width
      * @param height
      * @param allowedExts
      * @param format Format, also appended as an extension if the file doesn't end with anything in {@code allowedExts}
      * @param paintOffscreen
      * @throws IOException
      */
     private static void exportScreenShotBufferedImage(Component target, File selectedFile, int width, int height,
                                                       String[] allowedExts, String format, boolean paintOffscreen) throws IOException{
         BufferedImage image = getDeviceCompatibleImage(width, height);
         Graphics2D g = image.createGraphics();
 
         choosePaint(target, g, paintOffscreen);
 
         selectedFile = fixFileExt(selectedFile, allowedExts, format);
         if (selectedFile != null) {
             log.debug("Writing image to " + selectedFile.getAbsolutePath());
             ImageIO.write(image, format, selectedFile);
         }
     }
 
     /**
      * Add a file extension to the file if it doesn't already
      * have an acceptable one
      * @param selectedFile
      * @param allowedExts  Strings which qualify as extensions
      * @param defExtension Default extension. A period be inserted in between the file path iff {@code defExtension}
      *                     does not already have it
      * @return Either the input File, if it had an extension contained in {@code allowedExts},
      *         or a new with with {@code defExtension} appended
      */
     private static File fixFileExt(File selectedFile, String[] allowedExts, String defExtension){
         boolean hasExt = false;
         if (selectedFile != null) {
             for(String ext: allowedExts){
                 if (selectedFile.getName().toLowerCase().endsWith(ext)) {
                     hasExt = true;
                     break;
                 }
             }
             if(!hasExt){
                 String addExt = defExtension.startsWith(".") ? defExtension : "." + defExtension;
                 String correctedFilename = selectedFile.getAbsolutePath() + addExt;
                 selectedFile = new File(correctedFilename);
             }
         }
         return selectedFile;
     }
 
 
     /**
      * Creates a device compatible BufferedImage
      *
      * @param width  the width in pixels
      * @param height the height in pixels
      */
     public static BufferedImage getDeviceCompatibleImage(int width, int height) {
 
         GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
         GraphicsDevice screenDevice = graphicsEnvironment.getDefaultScreenDevice();
         GraphicsConfiguration graphicConfiguration = screenDevice.getDefaultConfiguration();
         BufferedImage image = graphicConfiguration.createCompatibleImage(width, height);
 
         return image;
     }
 
 
 }
