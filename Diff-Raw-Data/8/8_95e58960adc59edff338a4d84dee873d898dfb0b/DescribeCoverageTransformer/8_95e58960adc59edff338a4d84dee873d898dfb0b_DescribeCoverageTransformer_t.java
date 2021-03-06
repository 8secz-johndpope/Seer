 /* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
  * This code is licensed under the GPL 2.0 license, availible at the root
  * application directory.
  */
 package org.geoserver.wcs.response;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.logging.Logger;
 
 import net.opengis.wcs.v1_1_1.DescribeCoverageType;
 
 import org.geoserver.ows.util.RequestUtils;
 import org.geotools.geometry.GeneralEnvelope;
 import org.geotools.referencing.CRS;
 import org.geotools.referencing.operation.LinearTransform;
 import org.geotools.util.NumberRange;
 import org.geotools.util.logging.Logging;
 import org.geotools.xml.transform.TransformerBase;
 import org.geotools.xml.transform.Translator;
 import org.opengis.referencing.FactoryException;
 import org.opengis.referencing.crs.CoordinateReferenceSystem;
 import org.opengis.referencing.operation.Matrix;
 import org.vfny.geoserver.global.CoverageDimension;
 import org.vfny.geoserver.global.CoverageInfo;
 import org.vfny.geoserver.global.Data;
 import org.vfny.geoserver.global.MetaDataLink;
 import org.vfny.geoserver.global.WCS;
 import org.vfny.geoserver.wcs.WcsException;
 import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
 import org.vfny.geoserver.wcs.responses.CoverageResponseDelegate;
 import org.vfny.geoserver.wcs.responses.CoverageResponseDelegateFactory;
 import org.xml.sax.ContentHandler;
 import org.xml.sax.helpers.AttributesImpl;
 
 /**
  * Based on the <code>org.geotools.xml.transform</code> framework, does the
  * job of encoding a WCS 1.1.1 DescribeCoverage document.
  * 
  * @author Andrea Aime, TOPP
  */
 public class DescribeCoverageTransformer extends TransformerBase {
     private static final Logger LOGGER = Logging.getLogger(DescribeCoverageTransformer.class
             .getPackage().getName());
 
     private static final String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
 
     private static final String XSI_PREFIX = "xsi";
 
     private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
 
     private static final Map<String, String> METHOD_NAME_MAP = new HashMap<String, String>();
 
     static {
         METHOD_NAME_MAP.put("nearest neighbor", "nearest");
         METHOD_NAME_MAP.put("bilinear", "linear");
         METHOD_NAME_MAP.put("bicubic", "cubic");
     }
 
     private WCS wcs;
 
     private Data catalog;
 
     /**
      * Creates a new WFSCapsTransformer object.
      */
     public DescribeCoverageTransformer(WCS wcs, Data catalog) {
         super();
         this.wcs = wcs;
         this.catalog = catalog;
         setNamespaceDeclarationEnabled(false);
     }
 
     public Translator createTranslator(ContentHandler handler) {
         return new WCS111DescribeCoverageTranslator(handler);
     }
 
     private class WCS111DescribeCoverageTranslator extends TranslatorSupport {
         private DescribeCoverageType request;
 
         private String proxifiedBaseUrl;
 
         /**
          * Creates a new WFSCapsTranslator object.
          * 
          * @param handler
          *            DOCUMENT ME!
          */
         public WCS111DescribeCoverageTranslator(ContentHandler handler) {
             super(handler, null, null);
         }
 
         /**
          * Encode the object.
          * 
          * @param o
          *            The Object to encode.
          * 
          * @throws IllegalArgumentException
          *             if the Object is not encodeable.
          */
         public void encode(Object o) throws IllegalArgumentException {
             // try {
             if (!(o instanceof DescribeCoverageType)) {
                 throw new IllegalArgumentException(new StringBuffer("Not a GetCapabilitiesType: ")
                         .append(o).toString());
             }
 
             this.request = (DescribeCoverageType) o;
 
             final AttributesImpl attributes = new AttributesImpl();
             attributes.addAttribute("", "xmlns:wcs", "xmlns:wcs", "", WCS_URI);
 
             attributes.addAttribute("", "xmlns:xlink", "xmlns:xlink", "",
                     "http://www.w3.org/1999/xlink");
             attributes.addAttribute("", "xmlns:ogc", "xmlns:ogc", "", "http://www.opengis.net/ogc");
             attributes.addAttribute("", "xmlns:ows", "xmlns:ows", "",
                     "http://www.opengis.net/ows/1.1");
             attributes.addAttribute("", "xmlns:gml", "xmlns:gml", "", "http://www.opengis.net/gml");
 
             final String prefixDef = new StringBuffer("xmlns:").append(XSI_PREFIX).toString();
             attributes.addAttribute("", prefixDef, prefixDef, "", XSI_URI);
 
             final String locationAtt = new StringBuffer(XSI_PREFIX).append(":schemaLocation")
                     .toString();
 
             proxifiedBaseUrl = RequestUtils.proxifiedBaseURL(request.getBaseUrl(), wcs
                     .getGeoServer().getProxyBaseUrl());
             final String locationDef = WCS_URI + " " + proxifiedBaseUrl
                     + "schemas/wcs/1.1.1/wcsDescribeCoverage.xsd";
             attributes.addAttribute("", locationAtt, locationAtt, "", locationDef);
 
             start("wcs:CoverageDescriptions", attributes);
             for (Iterator it = request.getIdentifier().iterator(); it.hasNext();) {
                 String coverageId = (String) it.next();
 
                 // check the coverage is known
                 if (!Data.TYPE_RASTER.equals(catalog.getLayerType(coverageId))) {
                     throw new WcsException("Could not find the specified coverage: "
                             + coverageId, WcsExceptionCode.InvalidParameterValue, "identifiers");
                 }
 
                 CoverageInfo ci = catalog.getCoverageInfo(coverageId);
                 try {
                     handleCoverageDescription(ci);
                 } catch (Exception e) {
                     throw new RuntimeException(
                             "Unexpected error occurred during describe coverage xml encoding", e);
                 }
 
             }
             end("wcs:CoverageDescriptions");
         }
 
         void handleCoverageDescription(CoverageInfo ci) throws Exception {
             start("wcs:CoverageDescription");
             element("ows:Title", ci.getLabel());
             element("ows:Abstract", ci.getDescription());
             handleKeywords(ci.getKeywords());
             element("wcs:Identifier", ci.getName());
            handleMetadataLink(ci.getMetadataLink(), "simple");
             handleDomain(ci);
             handleRange(ci);
             handleSupportedCRSs(ci);
             handleSupportedFormats(ci);
             end("wcs:CoverageDescription");
         }
 
         // TODO: find a way to share this with the capabilities transfomer
        private void handleMetadataLink(MetaDataLink mdl, String linkType) {
             if (mdl != null) {
                 AttributesImpl attributes = new AttributesImpl();
 
                 if ((mdl.getAbout() != null) && (mdl.getAbout() != "")) {
                     attributes.addAttribute("", "about", "about", "", mdl.getAbout());
                 }
 
                 if ((mdl.getMetadataType() != null) && (mdl.getMetadataType() != "")) {
                    attributes.addAttribute("", "xlink:type", "xlink:type", "", linkType);
                 }
 
                 if (attributes.getLength() > 0) {
                     element("ows:Metadata", null, attributes);
                 }
             }
         }
 
         // TODO: find a way to share this with the capabilities transfomer
         private void handleKeywords(List kwords) {
             start("ows:Keywords");
 
             if (kwords != null) {
                 for (Iterator it = kwords.iterator(); it.hasNext();) {
                     element("ows:Keyword", it.next().toString());
                 }
             }
 
             end("ows:Keywords");
         }
 
         private void handleDomain(CoverageInfo ci) throws Exception {
             start("wcs:Domain");
             start("wcs:SpatialDomain");
             handleBoundingBox(ci.getWGS84LonLatEnvelope(), true);
             handleBoundingBox(ci.getEnvelope(), false);
             handleGridCRS(ci);
             end("wcs:SpatialDomain");
             end("wcs:Domain");
         }
 
         private void handleGridCRS(CoverageInfo ci) throws Exception {
             start("wcs:GridCRS");
             element("wcs:GridBaseCRS", urnIdentifier(ci.getCrs()));
             element("wcs:GridType", "urn:ogc:def:method:WCS:1.1:2dGridin2dCrs");
             // TODO: go back to using the metadata once they can be trusted
             final LinearTransform tx = (LinearTransform) ci.getGrid().getGridToCRS();
             final Matrix matrix = tx.getMatrix();
             // origin
             StringBuffer origins = new StringBuffer();
             for (int i = 0; i < matrix.getNumRow() - 1; i++) {
                 origins.append(matrix.getElement(i, matrix.getNumCol() - 1));
                 if (i < matrix.getNumRow() - 2)
                     origins.append(" ");
             }
             element("wcs:GridOrigin", origins.toString());
             // offsets
             StringBuffer offsets = new StringBuffer();
             for (int i = 0; i < matrix.getNumRow() - 1; i++) {
                 for (int j = 0; j < matrix.getNumCol() - 1; j++) {
                     offsets.append(matrix.getElement(i, j));
                     if (j < matrix.getNumCol() - 2)
                         offsets.append(" ");
                 }
                 if (i < matrix.getNumRow() - 2)
                     offsets.append(" ");
 
             }
             element("wcs:GridOffsets", offsets.toString());
             element("wcs:GridCS", "urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS");
             end("wcs:GridCRS");
         }
 
         private void handleBoundingBox(GeneralEnvelope envelope, boolean wgsLonLat)
                 throws Exception {
             final AttributesImpl attributes = new AttributesImpl();
             final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
             GeneralEnvelope encodedEnvelope = envelope;
             if (wgsLonLat) {
                 attributes.addAttribute("", "crs", "crs", "", "urn:ogc:def:crs:OGC:1.3:CRS84");
             } else {
                 String urnIdentifier = urnIdentifier(crs);
                 CoordinateReferenceSystem latlonCrs = CRS.decode(urnIdentifier);
                 encodedEnvelope = CRS.transform(CRS.findMathTransform(crs, latlonCrs, true),
                         envelope);
                 attributes.addAttribute("", "crs", "crs", "", urnIdentifier);
             }
             attributes.addAttribute("", "dimensions", "dimensions", "", Integer.toString(crs
                     .getCoordinateSystem().getDimension()));
             start("ows:BoundingBox", attributes);
             element("ows:LowerCorner", new StringBuffer(Double.toString(encodedEnvelope
                     .getLowerCorner().getOrdinate(0))).append(" ").append(
                     encodedEnvelope.getLowerCorner().getOrdinate(1)).toString());
             element("ows:UpperCorner", new StringBuffer(Double.toString(encodedEnvelope
                     .getUpperCorner().getOrdinate(0))).append(" ").append(
                     encodedEnvelope.getUpperCorner().getOrdinate(1)).toString());
             end("ows:BoundingBox");
         }
 
         private void handleRange(CoverageInfo ci) {
             start("wcs:Range");
             // at the moment we only handle single field coverages
             start("wcs:Field");
             CoverageDimension[] dimensions = ci.getDimensions();
             element("wcs:Identifier", "contents");
             // the output domain of the field
             start("wcs:Definition");
             NumberRange range = getCoverageRange(dimensions);
             if (range == null || range.isEmpty()) {
                 element("wcs:AnyValue", "");
             } else {
                 start("ows:AllowedValues");
                 start("ows:Range");
                 element("ows:MinimumValue", Double.toString(range.getMinimum()));
                 element("ows:MaximumValue", Double.toString(range.getMaximum()));
                 end("ows:Range");
                 end("ows:AllowedValues");
             }
             end("wcs:Definition");
             handleNullValues(dimensions);
             handleInterpolationMethods(ci);
             handleAxis(ci);
             end("wcs:Field");
             end("wcs:Range");
         }
 
         private void handleAxis(CoverageInfo ci) {
             final AttributesImpl attributes = new AttributesImpl();
             attributes.addAttribute("", "identifier", "identifier", "", "Bands");
             start("wcs:Axis", attributes);
             start("wcs:AvailableKeys");
             CoverageDimension[] dimensions = ci.getDimensions();
             for (int i = 0; i < dimensions.length; i++) {
                 element("wcs:Key", dimensions[i].getName().replace(' ', '_'));
             }
             end("wcs:AvailableKeys");
             end("wcs:Axis");
         }
 
         /**
          * Given a set of sample dimensions, this will return a valid range only
          * if all sample dimensions have one, otherwise null
          * 
          * @param dimensions
          * @return
          */
         private NumberRange getCoverageRange(CoverageDimension[] dimensions) {
             NumberRange range = null;
             for (int i = 0; i < dimensions.length; i++) {
                 if (dimensions[i].getRange() == null)
                     return null;
                 else if (range == null)
                     range = dimensions[i].getRange();
                 else
                     range.union(dimensions[i].getRange());
             }
             return range;
         }
 
         private void handleNullValues(CoverageDimension[] dimensions) {
             for (int i = 0; i < dimensions.length; i++) {
                 CoverageDimension cd = dimensions[i];
                 Double[] nulls = cd.getNullValues();
                 if(nulls == null)
                     return;
                 if (nulls.length == 1) {
                     element("wcs:NullValue", nulls[0].toString());
                 } else if (nulls.length >= 1) {
                     // the new specification allows only for a list of values,
                     // Can we assume min and max are two integer numbers and
                     // make up a list out of them? For the moment, just fail
                     throw new IllegalArgumentException("Cannot encode a range of null values, "
                             + "only single values are handled");
                 }
             }
         }
 
         private void handleInterpolationMethods(CoverageInfo ci) {
             start("wcs:InterpolationMethods");
             for (Iterator it = ci.getInterpolationMethods().iterator(); it.hasNext();) {
                 String method = (String) it.next();
                 String converted = METHOD_NAME_MAP.get(method);
                 if (converted != null)
                     element("wcs:InterpolationMethod", converted);
 
             }
             elementIfNotEmpty("wcs:Default", ci.getDefaultInterpolationMethod());
             end("wcs:InterpolationMethods");
         }
 
         private void handleSupportedFormats(CoverageInfo ci) throws Exception {
             // gather all the formats for this coverage 
             Set<String> formats = new HashSet<String>();
             for (Iterator it = ci.getSupportedFormats().iterator(); it.hasNext();) {
                 String format = (String) it.next();
                 // wcs 1.1 requires mime types, not format names
                 try  {
                     CoverageResponseDelegate delegate = CoverageResponseDelegateFactory
                             .encoderFor(format);
                     formats.addAll(delegate.getSupportedFormats());
                 } catch(Exception e) {
                     // no problem, we just want to avoid people writing HALLABALOOLA in the
                     // supported formats section of the coverage config and then break the
                     // describe response
                 }
             }
             // sort them
             List<String> sortedFormats = new ArrayList<String>(formats);
             Collections.sort(sortedFormats);
             for (String format : sortedFormats) {
                 element("wcs:SupportedFormat", format);
             }
             
         }
 
         private void handleSupportedCRSs(CoverageInfo ci) throws Exception {
             Set supportedCRSs = new LinkedHashSet();
             if (ci.getRequestCRSs() != null)
                 supportedCRSs.addAll(ci.getRequestCRSs());
             if (ci.getResponseCRSs() != null)
                 supportedCRSs.addAll(ci.getResponseCRSs());
             for (Iterator it = supportedCRSs.iterator(); it.hasNext();) {
                 String crsName = (String) it.next();
                 CoordinateReferenceSystem crs = CRS.decode(crsName);
                 element("wcs:SupportedCRS", urnIdentifier(crs));
                 element("wcs:SupportedCRS", crsName);
             }
         }
 
         private String urnIdentifier(final CoordinateReferenceSystem crs) throws FactoryException {
             String authorityAndCode = CRS.lookupIdentifier(crs, false);
             String code = authorityAndCode.substring(authorityAndCode.lastIndexOf(":") + 1);
             return "urn:ogc:def:crs:EPSG:" + code;
         }
 
         /**
          * Writes the element if and only if the content is not null and not
          * empty
          * 
          * @param elementName
          * @param content
          */
         private void elementIfNotEmpty(String elementName, String content) {
             if (content != null && !"".equals(content.trim()))
                 element(elementName, content);
         }
     }
 
 }
