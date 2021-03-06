 //$HeadURL$
 /*----------------------------------------------------------------------------
  This file is part of deegree, http://deegree.org/
  Copyright (C) 2001-2009 by:
  Department of Geography, University of Bonn
  and
  lat/lon GmbH
 
  This library is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the Free
  Software Foundation; either version 2.1 of the License, or (at your option)
  any later version.
  This library is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
  details.
  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation, Inc.,
  59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 
  Contact information:
 
  lat/lon GmbH
  Aennchenstr. 19, 53177 Bonn
  Germany
  http://lat-lon.de/
 
  Department of Geography, University of Bonn
  Prof. Dr. Klaus Greve
  Postfach 1147, 53001 Bonn
  Germany
  http://www.geographie.uni-bonn.de/deegree/
 
  e-mail: info@deegree.org
  ----------------------------------------------------------------------------*/
 
 package org.deegree.services.wms.controller.capabilities;
 
 import static java.lang.Double.NEGATIVE_INFINITY;
 import static java.lang.Double.POSITIVE_INFINITY;
 import static org.deegree.commons.xml.CommonNamespaces.SLDNS;
 import static org.deegree.commons.xml.CommonNamespaces.WMSNS;
 import static org.deegree.commons.xml.CommonNamespaces.XLNNS;
 import static org.deegree.commons.xml.CommonNamespaces.XSINS;
 import static org.deegree.cs.persistence.CRSManager.lookup;
 import static org.deegree.layer.dims.Dimension.formatDimensionValueList;
 import static org.slf4j.LoggerFactory.getLogger;
 
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 
 import javax.xml.namespace.QName;
 import javax.xml.stream.XMLInputFactory;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamReader;
 import javax.xml.stream.XMLStreamWriter;
 import javax.xml.transform.dom.DOMSource;
 
 import org.deegree.commons.annotations.LoggingNotes;
 import org.deegree.commons.tom.ows.CodeType;
 import org.deegree.commons.tom.ows.LanguageString;
 import org.deegree.commons.utils.DoublePair;
 import org.deegree.commons.utils.Pair;
 import org.deegree.commons.utils.StringUtils;
 import org.deegree.commons.xml.XMLAdapter;
 import org.deegree.commons.xml.stax.XMLStreamUtils;
 import org.deegree.cs.components.Axis;
 import org.deegree.cs.coordinatesystems.ICRS;
 import org.deegree.geometry.Envelope;
 import org.deegree.geometry.GeometryTransformer;
 import org.deegree.geometry.metadata.SpatialMetadata;
 import org.deegree.geometry.primitive.Point;
 import org.deegree.geometry.standard.primitive.DefaultPoint;
 import org.deegree.layer.dims.Dimension;
 import org.deegree.layer.metadata.LayerMetadata;
 import org.deegree.protocol.ows.metadata.ServiceIdentification;
 import org.deegree.protocol.ows.metadata.ServiceProvider;
 import org.deegree.protocol.ows.metadata.party.Address;
 import org.deegree.protocol.ows.metadata.party.ResponsibleParty;
 import org.deegree.services.jaxb.wms.LanguageStringType;
 import org.deegree.services.metadata.ServiceMetadata;
 import org.deegree.services.wms.MapService;
 import org.deegree.services.wms.controller.WMSController;
 import org.deegree.services.wms.controller.WMSController130;
 import org.deegree.services.wms.model.layers.Layer;
 import org.deegree.style.se.unevaluated.Style;
 import org.deegree.theme.Theme;
 import org.slf4j.Logger;
 import org.w3c.dom.Element;
 
 /**
  * <code>Capabilities130XMLAdapter</code>
  * 
  * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
  * @author last edited by: $Author$
  * 
  * @version $Revision$, $Date$
  */
 @LoggingNotes(warn = "logs problems with CRS when outputting 1.3.0 capabilities", trace = "logs stack traces")
 public class Capabilities130XMLAdapter extends XMLAdapter {
 
     private static final Logger LOG = getLogger( Capabilities130XMLAdapter.class );
 
     private final String getUrl;
 
     private final String postUrl;
 
     private final ServiceIdentification identification;
 
     private final ServiceProvider provider;
 
     private MapService service;
 
     private final WMSController controller;
 
     private ServiceMetadata metadata;
 
     /**
      * @param identification
      * @param provider
      * @param getUrl
      * @param postUrl
      * @param service
      * @param controller
      */
     public Capabilities130XMLAdapter( ServiceIdentification identification, ServiceProvider provider,
                                       ServiceMetadata metadata, String getUrl, String postUrl, MapService service,
                                       WMSController controller ) {
         this.identification = identification;
         this.provider = provider;
         this.metadata = metadata;
         this.getUrl = getUrl;
         this.postUrl = postUrl;
         this.service = service;
         this.controller = controller;
     }
 
     /**
      * Writes out a 1.3.0 style capabilities document.
      * 
      * @param writer
      * @throws XMLStreamException
      */
     public void export( XMLStreamWriter writer )
                             throws XMLStreamException {
 
         writer.setDefaultNamespace( WMSNS );
         writer.writeStartElement( WMSNS, "WMS_Capabilities" );
         writer.writeAttribute( "version", "1.3.0" );
         writer.writeAttribute( "updateSequence", "" + service.updateSequence );
         writer.writeDefaultNamespace( WMSNS );
         writer.writeNamespace( "xsi", XSINS );
         writer.writeNamespace( "xlink", XLNNS );
         writer.writeNamespace( "sld", SLDNS );
 
         writer.writeAttribute( XSINS,
                                "schemaLocation",
                                "http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd "
                                                        + "http://www.opengis.net/sld http://schemas.opengis.net/sld/1.1.0/sld_capabilities.xsd" );
 
         writeService( writer );
 
         writeCapability( writer );
 
         writer.writeEndElement();
         writer.writeEndDocument();
     }
 
     private void writeExtendedCapabilities( XMLStreamWriter writer ) {
         List<Element> caps = controller.getExtendedCapabilities( "1.3.0" );
         if ( caps != null ) {
             XMLInputFactory fac = XMLInputFactory.newInstance();
             for ( Element c : caps ) {
                 try {
                     XMLStreamReader reader = fac.createXMLStreamReader( new DOMSource( c ) );
                     XMLStreamUtils.skipStartDocument( reader );
                     XMLAdapter.writeElement( writer, reader );
                 } catch ( Throwable e ) {
                     LOG.warn( "Could not export extended capabilities snippet" );
                     LOG.trace( "Stack trace", e );
                 }
             }
         }
     }
 
     private void writeCapability( XMLStreamWriter writer )
                             throws XMLStreamException {
         writer.writeStartElement( WMSNS, "Capability" );
 
         writeRequest( writer );
         writer.writeStartElement( WMSNS, "Exception" );
         writeElement( writer, "Format", "XML" );
         writeElement( writer, "Format", "INIMAGE" );
         writeElement( writer, "Format", "BLANK" );
         writer.writeEndElement();
 
         writeExtendedCapabilities( writer );
 
         if ( service.isNewStyle() ) {
             writeThemes( writer, service.getThemes() );
         } else {
             writeLayers( writer, service.getRootLayer() );
         }
 
         writer.writeEndElement();
     }
 
     private void writeTheme( XMLStreamWriter writer, Theme theme )
                             throws XMLStreamException {
         LayerMetadata md = theme.getMetadata();
         writer.writeStartElement( WMSNS, "Layer" );
 
         if ( md.isQueryable() ) {
             writer.writeAttribute( "queryable", "1" );
         }
         if ( md.getCascaded() != 0 ) {
             writer.writeAttribute( "cascaded", md.getCascaded() + "" );
         }
 
         if ( md.getName() != null ) {
             writeElement( writer, WMSNS, "Name", md.getName() );
         }
         writeElement( writer, WMSNS, "Title", md.getDescription().getTitles().get( 0 ).getString() );
         List<LanguageString> abs = md.getDescription().getAbstracts();
         if ( abs != null && !abs.isEmpty() ) {
             writeElement( writer, WMSNS, "Abstract", abs.get( 0 ).getString() );
         }
         List<Pair<List<LanguageString>, CodeType>> kws = md.getDescription().getKeywords();
         if ( kws != null && !kws.isEmpty() && !kws.get( 0 ).first.isEmpty() ) {
             writer.writeStartElement( WMSNS, "KeywordList" );
             for ( LanguageString ls : kws.get( 0 ).first ) {
                 writeElement( writer, WMSNS, "Keyword", ls.getString() );
             }
             writer.writeEndElement();
         }
 
         SpatialMetadata smd = md.getSpatialMetadata();
         writeSrsAndEnvelope( writer, smd.getCoordinateSystems(), smd.getEnvelope() );
 
         for ( Theme t : theme.getThemes() ) {
             writeTheme( writer, t );
         }
         writer.writeEndElement();
     }
 
     private void writeThemes( XMLStreamWriter writer, List<Theme> themes )
                             throws XMLStreamException {
         if ( themes.size() == 1 ) {
             writeTheme( writer, themes.get( 0 ) );
         } else {
             // synthetic root layer needed
             writer.writeStartElement( WMSNS, "Layer" );
             // TODO
             writer.writeAttribute( "queryable", "1" );
             writeElement( writer, WMSNS, "Title", "Root" );
             for ( Theme t : themes ) {
                 writeTheme( writer, t );
             }
             writer.writeEndElement();
         }
     }
 
     private static void writeSrsAndEnvelope( XMLStreamWriter writer, List<ICRS> crsList, Envelope layerEnv )
                             throws XMLStreamException {
         for ( ICRS crs : crsList ) {
             if ( crs.getAlias().startsWith( "AUTO" ) ) {
                 writeElement( writer, WMSNS, "CRS", crs.getAlias().replace( "AUTO", "AUTO2" ) );
             } else {
                 writeElement( writer, WMSNS, "CRS", crs.getAlias() );
             }
         }
 
         ICRS latlon;
         try {
             latlon = lookup( "CRS:84" );
             if ( layerEnv != null && layerEnv.getCoordinateDimension() >= 2 ) {
                 Envelope bbox = new GeometryTransformer( latlon ).transform( layerEnv );
                 writer.writeStartElement( WMSNS, "EX_GeographicBoundingBox" );
                 Point min = bbox.getMin();
                 Point max = bbox.getMax();
                 if ( min.equals( max ) ) {
                     // TODO uncomment this once it's implemented
                     min = new DefaultPoint( min.getId(), min.getCoordinateSystem(), min.getPrecision(),
                                             new double[] { min.get0() - 0.0001, min.get1() - 0.0001 } );
                     // bbox = (Envelope) bbox.getBuffer( 0.0001 ); // should be ok to just use the same value for all
                     // crs
                 }
                 writeElement( writer, WMSNS, "westBoundLongitude", min.get0() + "" );
                 writeElement( writer, WMSNS, "eastBoundLongitude", max.get0() + "" );
                 writeElement( writer, WMSNS, "southBoundLatitude", min.get1() + "" );
                 writeElement( writer, WMSNS, "northBoundLatitude", max.get1() + "" );
                 writer.writeEndElement();
 
                 for ( ICRS crs : crsList ) {
                     if ( crs.getAlias().startsWith( "AUTO" ) ) {
                         continue;
                     }
                     // try {
                     // crs.getWrappedCRS();
                     // } catch ( UnknownCRSException e ) {
                     // LOG.warn( "Cannot find: {}", e.getLocalizedMessage() );
                     // LOG.trace( "Stack trace:", e );
                     // continue;
                     // }
                     Envelope envelope;
                     ICRS srs = crs;
                     try {
                         Envelope src = layerEnv;
                         GeometryTransformer transformer = new GeometryTransformer( srs );
                         if ( src.getCoordinateSystem() == null ) {
                             envelope = transformer.transform( layerEnv, latlon );
                         } else {
                             envelope = transformer.transform( layerEnv );
                         }
                     } catch ( Throwable e ) {
                         LOG.warn( "Cannot transform: {}", e.getLocalizedMessage() );
                         LOG.trace( "Stack trace:", e );
                         continue;
                     }
 
                     writer.writeStartElement( WMSNS, "BoundingBox" );
                     writer.writeAttribute( "CRS", crs.getAlias() );
 
                     min = envelope.getMin();
                     max = envelope.getMax();
                     if ( min.equals( max ) ) {
                         // TODO uncomment this once it's implemented
                         min = new DefaultPoint( min.getId(), min.getCoordinateSystem(), min.getPrecision(),
                                                 new double[] { min.get0() - 0.0001, min.get1() - 0.0001 } );
                         // bbox = (Envelope) bbox.getBuffer( 0.0001 ); // should be ok to just use the same value for
                         // all
                         // crs
                     }
 
                     // check for srs with northing as first axis
                     // try {
                     srs = WMSController130.getCRS( crs.getAlias() );
                     // } catch ( UnknownCRSException e ) {
                     // // may fail if CRS is determined eg. from .prj
                     // LOG.warn( "Cannot find: {}", e.getLocalizedMessage() );
                     // LOG.trace( "Stack trace:", e );
                     // }
                     switch ( srs.getAxis()[0].getOrientation() ) {
                     case Axis.AO_NORTH:
                         writer.writeAttribute( "miny", Double.toString( min.get0() ) );
                         writer.writeAttribute( "minx", Double.toString( min.get1() ) );
                         writer.writeAttribute( "maxy", Double.toString( max.get0() ) );
                         writer.writeAttribute( "maxx", Double.toString( max.get1() ) );
                         break;
                     default:
                         writer.writeAttribute( "minx", Double.toString( min.get0() ) );
                         writer.writeAttribute( "miny", Double.toString( min.get1() ) );
                         writer.writeAttribute( "maxx", Double.toString( max.get0() ) );
                         writer.writeAttribute( "maxy", Double.toString( max.get1() ) );
                     }
                     writer.writeEndElement();
                 }
             }
         } catch ( Throwable e ) {
             LOG.warn( "Cannot transform: {}", e.getLocalizedMessage() );
             LOG.trace( "Stack trace:", e );
         }
     }
 
     @Deprecated
     private void writeLayers( XMLStreamWriter writer, Layer layer )
                             throws XMLStreamException {
         if ( layer.getTitle() == null || !layer.isAvailable() ) {
             return;
         }
 
         writer.writeStartElement( WMSNS, "Layer" );
         if ( layer.isQueryable() ) {
             writer.writeAttribute( "queryable", "1" );
         }
 
         maybeWriteElementNS( writer, WMSNS, "Name", layer.getName() );
         writeElement( writer, WMSNS, "Title", layer.getTitle() );
         maybeWriteElementNS( writer, WMSNS, "Abstract", layer.getAbstract() );
 
         if ( !layer.getKeywords().isEmpty() ) {
             writer.writeStartElement( WMSNS, "KeywordList" );
             for ( Pair<org.deegree.commons.tom.ows.CodeType, LanguageStringType> p : layer.getKeywords() ) {
                 writer.writeStartElement( WMSNS, "Keyword" );
                 if ( p.first != null ) {
                     writer.writeAttribute( "vocabulary", p.first.getCodeSpace() );
                 }
                 writer.writeCharacters( p.second.getValue() );
                 writer.writeEndElement();
             }
             writer.writeEndElement();
         }
 
         writeSrsAndEnvelope( writer, layer.getSrs(), layer.getBbox() );
 
         Map<String, Dimension<?>> dims = layer.getDimensions();
         for ( Entry<String, Dimension<?>> entry : dims.entrySet() ) {
             writer.writeStartElement( WMSNS, "Dimension" );
             Dimension<?> dim = entry.getValue();
             writer.writeAttribute( "name", entry.getKey() );
             writer.writeAttribute( "units", dim.getUnits() == null ? "CRS:88" : dim.getUnits() );
             writer.writeAttribute( "unitSymbol", dim.getUnitSymbol() == null ? "" : dim.getUnitSymbol() );
             if ( dim.getDefaultValue() != null ) {
                 writer.writeAttribute( "default",
                                        formatDimensionValueList( dim.getDefaultValue(), "time".equals( entry.getKey() ) ) );
             }
             if ( dim.getNearestValue() ) {
                 writer.writeAttribute( "nearestValue", "1" );
             }
             if ( dim.getMultipleValues() ) {
                 writer.writeAttribute( "multipleValues", "1" );
             }
             if ( dim.getCurrent() ) {
                 writer.writeAttribute( "current", "1" );
             }
             writer.writeCharacters( dim.getExtentAsString() );
             writer.writeEndElement();
         }
 
         if ( layer.getAuthorityURL() != null ) {
             writer.writeStartElement( WMSNS, "AuthorityURL" );
             writer.writeAttribute( "name", "fromISORecord" );
             writer.writeStartElement( WMSNS, "OnlineResource" );
             writer.writeAttribute( XLNNS, "href", layer.getAuthorityURL() );
             writer.writeEndElement();
             writer.writeEndElement();
         }
 
         if ( layer.getAuthorityIdentifier() != null ) {
             writer.writeStartElement( WMSNS, "Identifier" );
             writer.writeAttribute( "authority", "fromISORecord" );
             writer.writeCharacters( layer.getAuthorityIdentifier() );
             writer.writeEndElement();
         }
 
        String mdUrl = layer.getName() == null || metadata == null ? null
                                                                  : metadata.getDataMetadataUrl( new QName(
                                                                                                            layer.getName() ) );
 
         mdlabel: if ( mdUrl == null && controller.getMetadataURLTemplate() != null ) {
             String id = layer.getDataMetadataSetId();
             if ( id == null ) {
                 break mdlabel;
             }
             String mdurlTemplate = controller.getMetadataURLTemplate();
             if ( mdurlTemplate.isEmpty() ) {
                 mdurlTemplate = getUrl;
                 if ( !( mdurlTemplate.endsWith( "?" ) || mdurlTemplate.endsWith( "&" ) ) ) {
                     mdurlTemplate += "?";
                 }
                 mdurlTemplate += "service=CSW&request=GetRecordById&version=2.0.2&outputSchema=http://www.isotc211.org/2005/gmd&elementSetName=full&id=${metadataSetId}";
             }
 
             mdUrl = StringUtils.replaceAll( mdurlTemplate, "${metadataSetId}", id );
         }
 
         if ( mdUrl != null ) {
             writer.writeStartElement( WMSNS, "MetadataURL" );
             writer.writeAttribute( "type", "ISO19115:2003" );
             writeElement( writer, WMSNS, "Format", "application/xml" );
             writer.writeStartElement( WMSNS, "OnlineResource" );
             writer.writeAttribute( XLNNS, "type", "simple" );
             writer.writeAttribute( XLNNS, "href", mdUrl );
             writer.writeEndElement();
             writer.writeEndElement();
         }
 
         Style def = service.getStyles().get( layer.getName(), null );
         if ( def != null ) {
             if ( def.getName() != null && !def.getName().isEmpty() ) {
                 writeStyle( writer, "default", def.getName(), service.getLegendSize( def ), layer.getName(), def ); // TODO
                 // title/description/whatever
             } else {
                 writeStyle( writer, "default", "default", service.getLegendSize( def ), layer.getName(), def ); // TODO
                 // title/description/whatever
             }
         }
         HashSet<Style> visited = new HashSet<Style>();
         for ( Style s : service.getStyles().getAll( layer.getName() ) ) {
             if ( visited.contains( s ) ) {
                 continue;
             }
             visited.add( s );
             String name = s.getName();
             if ( name != null && !name.isEmpty() ) {
                 writeStyle( writer, name, name, service.getLegendSize( s ), layer.getName(), s ); // TODO
                 // title/description/whatever
             }
         }
 
         DoublePair hint = layer.getScaleHint();
         if ( hint.first != NEGATIVE_INFINITY ) {
             writeElement( writer, WMSNS, "MinScaleDenominator", hint.first + "" );
         }
         if ( hint.second != POSITIVE_INFINITY ) {
             writeElement( writer, WMSNS, "MaxScaleDenominator", hint.second + "" );
         }
 
         for ( Layer l : new LinkedList<Layer>( layer.getChildren() ) ) {
             writeLayers( writer, l );
         }
 
         writer.writeEndElement();
     }
 
     private void writeStyle( XMLStreamWriter writer, String name, String title, Pair<Integer, Integer> legendSize,
                              String layerName, Style style )
                             throws XMLStreamException {
         writer.writeStartElement( WMSNS, "Style" );
         writeElement( writer, WMSNS, "Name", name );
         writeElement( writer, WMSNS, "Title", title );
         if ( legendSize.first > 0 && legendSize.second > 0 ) {
             writer.writeStartElement( WMSNS, "LegendURL" );
             writer.writeAttribute( "width", "" + legendSize.first );
             writer.writeAttribute( "height", "" + legendSize.second );
             writeElement( writer, WMSNS, "Format", "image/png" );
             writer.writeStartElement( WMSNS, "OnlineResource" );
             writer.writeAttribute( XLNNS, "type", "simple" );
             if ( style.getLegendURL() == null || style.prefersGetLegendGraphicUrl() ) {
                 String styleName = style.getName() == null ? "" : ( "&style=" + style.getName() );
                 writer.writeAttribute( XLNNS, "href", getUrl
                                                       + "?request=GetLegendGraphic&version=1.3.0&service=WMS&layer="
                                                       + layerName + styleName + "&format=image/png" );
             } else {
                 writer.writeAttribute( XLNNS, "href", style.getLegendURL().toExternalForm() );
             }
             writer.writeEndElement();
             writer.writeEndElement();
         }
         writer.writeEndElement();
     }
 
     private void writeDCP( XMLStreamWriter writer, boolean get, boolean post )
                             throws XMLStreamException {
         writer.writeStartElement( WMSNS, "DCPType" );
         writer.writeStartElement( WMSNS, "HTTP" );
         if ( get ) {
             writer.writeStartElement( WMSNS, "Get" );
             writer.writeStartElement( WMSNS, "OnlineResource" );
             writer.writeAttribute( XLNNS, "type", "simple" );
             writer.writeAttribute( XLNNS, "href", getUrl + "?" );
             writer.writeEndElement();
             writer.writeEndElement();
         }
         if ( post ) {
             writer.writeStartElement( WMSNS, "Post" );
             writer.writeStartElement( WMSNS, "OnlineResource" );
             writer.writeAttribute( XLNNS, "type", "simple" );
             writer.writeAttribute( XLNNS, "href", postUrl );
             writer.writeEndElement();
             writer.writeEndElement();
         }
         writer.writeEndElement();
         writer.writeEndElement();
     }
 
     private void writeRequest( XMLStreamWriter writer )
                             throws XMLStreamException {
         writer.writeStartElement( WMSNS, "Request" );
 
         writer.writeStartElement( WMSNS, "GetCapabilities" );
         writeElement( writer, WMSNS, "Format", "text/xml" );
         writeDCP( writer, true, false );
         writer.writeEndElement();
 
         writer.writeStartElement( WMSNS, "GetMap" );
         writeImageFormats( writer );
         writeDCP( writer, true, false );
         writer.writeEndElement();
 
         writer.writeStartElement( WMSNS, "GetFeatureInfo" );
         writeInfoFormats( writer );
         writeDCP( writer, true, false );
         writer.writeEndElement();
 
         writer.writeStartElement( SLDNS, "GetLegendGraphic" );
         writeImageFormats( writer );
         writeDCP( writer, true, false );
         writer.writeEndElement();
 
         writer.writeEndElement();
     }
 
     private void writeImageFormats( XMLStreamWriter writer )
                             throws XMLStreamException {
         for ( String f : controller.supportedImageFormats ) {
             writeElement( writer, "Format", f );
         }
     }
 
     private void writeInfoFormats( XMLStreamWriter writer )
                             throws XMLStreamException {
         for ( String f : controller.supportedFeatureInfoFormats.keySet() ) {
             writeElement( writer, "Format", f );
         }
     }
 
     private void writeService( XMLStreamWriter writer )
                             throws XMLStreamException {
         writer.writeStartElement( WMSNS, "Service" );
 
         writeElement( writer, WMSNS, "Name", "WMS" );
 
         List<LanguageString> titles = identification == null ? null : identification.getTitles();
         String title = ( titles != null && !titles.isEmpty() ) ? titles.get( 0 ).getString() : "deegree 3 WMS";
         writeElement( writer, WMSNS, "Title", title );
 
         List<LanguageString> abstracts = identification == null ? null : identification.getAbstracts();
         if ( abstracts != null && !abstracts.isEmpty() ) {
             writeElement( writer, WMSNS, "Abstract", abstracts.get( 0 ).getString() );
         }
 
         List<Pair<List<LanguageString>, CodeType>> keywords = identification == null ? null
                                                                                     : identification.getKeywords();
         if ( keywords != null && !keywords.isEmpty() ) {
             writer.writeStartElement( WMSNS, "KeywordList" );
 
             for ( Pair<List<LanguageString>, CodeType> key : keywords ) {
                 CodeType type = key.second;
                 for ( LanguageString lanString : key.first ) {
                     writer.writeStartElement( WMSNS, "Keyword" );
                     if ( type != null ) {
                         writer.writeAttribute( "vocabulary", type.getCodeSpace() );
                     }
                     writer.writeCharacters( lanString.getString() );
                     writer.writeEndElement();
                 }
             }
 
             writer.writeEndElement();
         }
 
         String url = getUrl;
         if ( provider != null && provider.getServiceContact() != null
              && provider.getServiceContact().getContactInfo() != null
              && provider.getServiceContact().getContactInfo().getOnlineResource() != null ) {
             url = provider.getServiceContact().getContactInfo().getOnlineResource().toExternalForm();
         }
         writer.writeStartElement( WMSNS, "OnlineResource" );
         writer.writeAttribute( XLNNS, "type", "simple" );
         writer.writeAttribute( XLNNS, "href", url );
         writer.writeEndElement();
 
         if ( provider != null ) {
             ResponsibleParty contact = provider.getServiceContact();
             if ( contact != null ) {
                 writer.writeStartElement( WMSNS, "ContactInformation" );
 
                 if ( contact.getIndividualName() != null ) {
                     writer.writeStartElement( WMSNS, "ContactPersonPrimary" );
                     writeElement( writer, WMSNS, "ContactPerson", contact.getIndividualName() );
                     writeElement( writer, WMSNS, "ContactOrganization", provider.getProviderName() );
                     writer.writeEndElement();
                 }
 
                 maybeWriteElementNS( writer, WMSNS, "ContactPosition", contact.getPositionName() );
                 Address addr = contact.getContactInfo().getAddress();
                 if ( addr != null ) {
                     writer.writeStartElement( WMSNS, "ContactAddress" );
                     writeElement( writer, WMSNS, "AddressType", "postal" );
                     for ( String s : addr.getDeliveryPoint() ) {
                         maybeWriteElementNS( writer, WMSNS, "Address", s );
                     }
                     writeElement( writer, WMSNS, "City", addr.getCity() );
                     writeElement( writer, WMSNS, "StateOrProvince", addr.getAdministrativeArea() );
                     writeElement( writer, WMSNS, "PostCode", addr.getPostalCode() );
                     writeElement( writer, WMSNS, "Country", addr.getCountry() );
                     writer.writeEndElement();
                 }
 
                 maybeWriteElementNS( writer, WMSNS, "ContactVoiceTelephone",
                                      contact.getContactInfo().getPhone().getVoice().get( 0 ) );
                 maybeWriteElementNS( writer, WMSNS, "ContactFacsimileTelephone",
                                      contact.getContactInfo().getPhone().getFacsimile().get( 0 ) );
                 if ( addr != null ) {
                     for ( String email : addr.getElectronicMailAddress() ) {
                         maybeWriteElementNS( writer, WMSNS, "ContactElectronicMailAddress", email );
                     }
                 }
 
                 writer.writeEndElement();
             }
 
             if ( identification != null ) {
                 maybeWriteElementNS( writer, WMSNS, "Fees", identification.getFees() );
                 List<String> constr = identification.getAccessConstraints();
                 if ( constr != null ) {
                     for ( String cons : constr ) {
                         maybeWriteElementNS( writer, WMSNS, "AccessConstraints", cons );
                     }
                 }
             } else {
                 writeElement( writer, WMSNS, "Fees", "none" );
                 writeElement( writer, WMSNS, "AccessConstraints", "none" );
             }
 
         }
 
         writer.writeEndElement();
     }
 
 }
