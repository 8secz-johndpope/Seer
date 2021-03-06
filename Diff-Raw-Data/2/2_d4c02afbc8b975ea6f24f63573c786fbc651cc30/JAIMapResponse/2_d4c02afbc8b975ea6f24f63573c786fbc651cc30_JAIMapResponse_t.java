 /* Copyright (c) 2001, 2003 TOPP - www.openplans.org.  All rights reserved.
  * This code is licensed under the GPL 2.0 license, availible at the root
  * application directory.
  */
 package org.vfny.geoserver.responses.wms.map;
 
 import com.vividsolutions.jts.geom.Envelope;
 import org.geotools.data.FeatureSource;
 import org.geotools.data.Query;
 import org.geotools.feature.FeatureType;
 import org.geotools.filter.Filter;
 import org.geotools.filter.FilterFactory;
 import org.geotools.filter.IllegalFilterException;
 import org.geotools.map.DefaultMapContext;
 import org.geotools.map.DefaultMapLayer;
 import org.geotools.map.MapContext;
 import org.geotools.map.MapLayer;
 import org.geotools.renderer.lite.LiteRenderer;
 import org.geotools.styling.Style;
 import org.geotools.styling.StyleAttributeExtractor;
 import org.vfny.geoserver.WmsException;
 import org.vfny.geoserver.global.FeatureTypeInfo;
 import org.vfny.geoserver.global.GeoServer;
 import org.vfny.geoserver.requests.wms.GetMapRequest;
 import java.awt.Graphics2D;
 import java.awt.Rectangle;
 import java.awt.AlphaComposite;
 import java.awt.geom.AffineTransform;
 import java.awt.image.BufferedImage;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Iterator;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.imageio.ImageIO;
 import javax.imageio.ImageWriter;
 import javax.imageio.stream.ImageOutputStream;
 
 
 /**
  * Generates a map using the geotools jai rendering classes.  Uses the Lite
  * renderer, loading the data on the fly, which is quite nice.  Thanks Andrea
  * and Gabriel.  The word is that we should eventually switch over to
  * StyledMapRenderer and do some fancy stuff with caching layers, but  I think
  * we are a ways off with its maturity to try that yet.  So Lite treats us
  * quite well, as it is stateless and therefor loads up nice and fast.
  *
  * @author Chris Holmes, TOPP
  * @version $Id: JAIMapResponse.java,v 1.29 2004/09/16 21:44:28 cholmesny Exp $
  */
 public class JAIMapResponse extends GetMapDelegate {
     /** A logger for this class. */
     private static final Logger LOGGER = Logger.getLogger(
             "org.vfny.geoserver.responses.wms.map");
 
     /** The formats supported by this map delegate. */
     private static List supportedFormats = null;
 
     /** The image generated by the execute method. */
     private BufferedImage image;
     private LiteRenderer renderer;
     private Graphics2D graphic;
 
     /**
      * setted in execute() from the requested output format, it's holded just
      * to be sure that method has been called before getContentType() thus
      * supporting the workflow contract of the request processing
      */
     private String format = null;
 
     /**
      *
      */
     public JAIMapResponse() {
     }
 
     /**
      * Evaluates if this Map producer can generate the map format specified by
      * <code>mapFormat</code>
      *
      * @param mapFormat the mime type of the output map format requiered
      *
      * @return true if class can produce a map in the passed format
      */
     public boolean canProduce(String mapFormat) {
         return getSupportedFormats().contains(mapFormat);
     }
 
     /**
      * The formats this delegate supports. Includes those formats supported by
      * the Java ImageIO extension, mostly: <i>png, x-portable-graymap, jpeg,
      * jpeg2000, x-png, tiff, vnd.wap.wbmp, x-portable-pixmap,
      * x-portable-bitmap, bmp and x-portable-anymap</i>, but the specific ones
      * will depend on the platform and JAI version. At leas JPEG and PNG will
      * generally work.
      *
      * @return The list of the supported formats, as returned by the Java
      *         ImageIO extension.
      */
     public List getSupportedFormats() {
         if (supportedFormats == null) {
             //LiteRenderer renderer = null;
             String[] mimeTypes = null;
 
             try {
                 renderer = new LiteRenderer();
                 mimeTypes = ImageIO.getWriterMIMETypes();
             } catch (NoClassDefFoundError ncdfe) {
                 supportedFormats = Collections.EMPTY_LIST;
                 LOGGER.warning("could not find jai: " + ncdfe);
 
                 //this will occur if JAI is not present, so please do not
                 //delete, or we get really nasty messages on getCaps for wms.
             }
 
             if ((renderer == null) || (mimeTypes == null)) {
                 LOGGER.info("renderer was null, so jai not found");
                 supportedFormats = Collections.EMPTY_LIST;
             } else {
                 supportedFormats = Arrays.asList(mimeTypes);
 
                 if (LOGGER.isLoggable(Level.CONFIG)) {
                     StringBuffer sb = new StringBuffer(
                             "Supported JAIMapResponse's MIME Types: [");
 
                     for (Iterator it = supportedFormats.iterator();
                             it.hasNext();) {
                         sb.append(it.next());
 
                         if (it.hasNext()) {
                             sb.append(", ");
                         }
                     }
 
                     sb.append("]");
                     LOGGER.config(sb.toString());
                 }
             }
         }
 
         return supportedFormats;
     }
 
     /**
      * Writes the image to the client.
      *
      * @param out The output stream to write to.
      *
      * @throws org.vfny.geoserver.ServiceException DOCUMENT ME!
      * @throws java.io.IOException DOCUMENT ME!
      */
     public void writeTo(OutputStream out)
         throws org.vfny.geoserver.ServiceException, java.io.IOException {
         formatImageOutputStream(format, image, out);
         graphic.dispose(); //I think this may have been causing problems,
 
         //getting rid of it too soon.
     }
 
     /**
      * Transforms the rendered image into the appropriate format, streaming to
      * the output stream.
      *
      * @param format The name of the format
      * @param image The image to be formatted.
      * @param outStream The stream to write to.
      *
      * @throws WmsException
      * @throws IOException DOCUMENT ME!
      */
     public void formatImageOutputStream(String format, BufferedImage image,
         OutputStream outStream) throws WmsException, IOException {
         if (format.equalsIgnoreCase("jpeg")) {
             format = "image/jpeg";
         }
 
         Iterator it = ImageIO.getImageWritersByMIMEType(format);
 
         if (!it.hasNext()) {
             throw new WmsException( //WMSException.WMSCODE_INVALIDFORMAT,
                 "Format not supported: " + format);
         }
 
         ImageWriter writer = (ImageWriter) it.next();
         ImageOutputStream ioutstream = null;
 
         ioutstream = ImageIO.createImageOutputStream(outStream);
         writer.setOutput(ioutstream);
         writer.write(image);
         writer.dispose();
         ioutstream.close();
     }
 
     /**
      * Halts the loading.  Right now just calls renderer.stopRendering.
      *
      * @param gs DOCUMENT ME!
      *
      * @task TODO: What would be nice is if we could also put the image being
      *       worked on in the GarbageCollector
      */
     public void abort(GeoServer gs) {
         renderer.stopRendering();
 
         //taking out for now, Andrea says it might have problems.
         //though this is in the abort, so do we really care if it throws
         //an exception?  Can it mess things up more than that?
         // if (graphic != null) {
         //   graphic.dispose();
         //}
     }
 
     /**
      * Gets the content type.  This is set by the request, should only be
      * called after execute.  GetMapResponse should handle this though.
      *
      * @param gs server configuration
      *
      * @return The mime type that this response will generate.
      *
      * @throws java.lang.IllegalStateException if <code>execute()</code> has
      *         not been previously called
      */
     public String getContentType(GeoServer gs)
         throws java.lang.IllegalStateException {
         //Return a default?  Format is not set until execute is called...
         return format;
     }
 
     /**
      * returns the content encoding for the output data (null for this class)
      *
      * @return <code>null</code> since no special encoding is performed while
      *         wrtting to the output stream
      */
     public String getContentEncoding() {
         return format;
     }
 
     /**
      * Over ride to return the same filter passed in, as this subclass
      * automatically handles the bboxing of the filter.  There is probably a
      * more elegant way to handle this, or rather the SVG renderer should be
      * improved to be able to select its own bboxes given the filter area
      * passed in.
      *
      * @param filter The additional filter to process with.
      * @param requestExtent The extent to filter out.
      * @param ffactory A filterFactory to create new filters.
      * @param schema The FeatureTypeInfo of the request of this filter.
      *
      * @return A custom filter of the bbox and any optional custom filters.
      *
      * @throws IllegalFilterException For problems making the filter.
      */
     protected Filter buildFilter(Filter filter, Envelope requestExtent,
         FilterFactory ffactory, FeatureType schema)
         throws IllegalFilterException {
         return filter;
     }
 
     /**
      * Performs the execute request using geotools rendering.
      *
      * @param requestedLayers The information on the types requested.
      * @param queries The results of the queries to generate maps with.
      * @param styles The styles to be used on the results.
      *
      * @throws WmsException For any problems.
      *
      * @task TODO: Update to feature streaming and latest api, Map is
      *       deprecated.
      */
     protected void execute(FeatureTypeInfo[] requestedLayers, Query[] queries,
         Style[] styles) throws WmsException {
         GetMapRequest request = getRequest();
         this.format = request.getFormat();
 
         int width = request.getWidth();
         int height = request.getHeight();
 
         try {
             LOGGER.fine("setting up map");
 
             MapContext map = new DefaultMapContext();
             MapLayer layer;
 
             for (int i = 0; i < requestedLayers.length; i++) {
                 Style style = styles[i];
                 Query query = queries[i];
                 FeatureSource source = requestedLayers[i].getFeatureSource();
                 checkStyle(style, source);
                 layer = new DefaultMapLayer(source, style);
                 layer.setQuery(query);
                 map.addLayer(layer);
             }
 
             LOGGER.fine("map setup");
 
             BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
 
             //LOGGER.fine("setting up renderer");
             //java.awt.Graphics g = image.getGraphics();
             graphic = image.createGraphics();
    
 
             if (!request.isTransparent()) {
                graphic.setColor(request.getBgColor()); 
                graphic.fillRect(0, 0, width, height);
             } else {
                 LOGGER.fine("setting to transparent");
 		int type = AlphaComposite.SRC_OVER;
 		graphic.setComposite(AlphaComposite.getInstance(type, 0));
 	    }
 
             renderer = new LiteRenderer(map);
 
             //we already do everything that the optimized data loading does...
             //if we set it to true then it does it all twice...
             renderer.setOptimizedDataLoadingEnabled(true);
 
             //Envelope dataArea = map.getLayerBounds();
             Envelope dataArea = request.getBbox();
             Rectangle paintArea = new Rectangle(width, height);
             AffineTransform at = renderer.worldToScreenTransform(dataArea,
                     paintArea);
 
             //renderer.paint((Graphics2D) image.getGraphics(), paintArea, at);
             renderer.paint(graphic, paintArea, at);
 
             LOGGER.fine("called renderer");
 
             map = null;
             this.image = image;
         } catch (IOException exp) {
             exp.printStackTrace();
 
             //LOGGER.info("uh, we're in this catch loop");
             //throw new RuntimeException("can we get a wms exception?");
             throw new WmsException(null, "Internal error : " + exp.getMessage());
         }
     }
 
     /**
      * Checks to make sure that the style passed in can process this
      * FeatureSource. This should really be done at start up time, and
      * returned as part of the  WMS capabilities.
      *
      * @param style The style to check
      * @param source The source requested.
      *
      * @throws WmsException DOCUMENT ME!
      */
     private void checkStyle(Style style, FeatureSource source)
         throws WmsException {
         FeatureType fType = source.getSchema();
         StyleAttributeExtractor sae = new StyleAttributeExtractor();
         sae.visit(style);
 
         String[] styleAttributes = sae.getAttributeNames();
 
         for (int i = 0; i < styleAttributes.length; i++) {
             String attName = styleAttributes[i];
 
             if (fType.getAttributeType(attName) == null) {
                 throw new WmsException(
                     "The requested Style can not be used with "
                     + "this featureType.  The style specifies an attribute of "
                     + attName + " and the featureType definition is: " + fType);
             }
         }
     }
 
     /**
      * Sets up the affine transform.  Stolen from liteRenderer code.
      *
      * @param mapExtent the map extent
      * @param screenSize the screen size
      *
      * @return a transform that maps from real world coordinates to the screen
      */
     public AffineTransform worldToScreenTransform(Envelope mapExtent,
         Rectangle screenSize) {
         double scaleX = screenSize.getWidth() / mapExtent.getWidth();
         double scaleY = screenSize.getHeight() / mapExtent.getHeight();
 
         double tx = -mapExtent.getMinX() * scaleX;
         double ty = (mapExtent.getMinY() * scaleY) + screenSize.getHeight();
 
         AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY,
                 tx, ty);
 
         return at;
     }
 }
