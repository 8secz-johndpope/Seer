 //$HeadURL$
 /*----------------------------------------------------------------------------
  This file is part of deegree, http://deegree.org/
  Copyright (C) 2001-2011 by:
  - Department of Geography, University of Bonn -
  and
  - lat/lon GmbH -
 
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
 package org.deegree.services.wfs;
 
 import static org.deegree.protocol.ows.exception.OWSException.INVALID_PARAMETER_VALUE;
 import static org.deegree.protocol.wfs.WFSConstants.WFS_200_NS;
 import static org.deegree.protocol.wfs.WFSConstants.WFS_200_SCHEMA_URL;
 import static org.deegree.services.wfs.WebFeatureService.getXMLResponseWriter;
 
 import java.io.IOException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 
 import javax.xml.namespace.QName;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamWriter;
 
 import org.deegree.commons.tom.ows.LanguageString;
 import org.deegree.feature.types.FeatureType;
 import org.deegree.protocol.ows.exception.OWSException;
 import org.deegree.protocol.wfs.storedquery.CreateStoredQuery;
 import org.deegree.protocol.wfs.storedquery.DescribeStoredQueries;
 import org.deegree.protocol.wfs.storedquery.DropStoredQuery;
 import org.deegree.protocol.wfs.storedquery.ListStoredQueries;
 import org.deegree.protocol.wfs.storedquery.Parameter;
 import org.deegree.protocol.wfs.storedquery.QueryExpressionText;
 import org.deegree.protocol.wfs.storedquery.StoredQueryDefinition;
 import org.deegree.protocol.wfs.storedquery.StoredQueryDefinitionXMLAdapter;
 import org.deegree.services.controller.utils.HttpResponseBuffer;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 /**
  * Handles {@link CreateStoredQuery}, {@link DescribeStoredQueries}, {@link DropStoredQuery} and
  * {@link ListStoredQueries} requests for the {@link WebFeatureService}.
  * 
  * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
  * @author last edited by: $Author$
  * 
  * @version $Revision$, $Date$
  */
 class StoredQueryHandler {
 
     private static final Logger LOG = LoggerFactory.getLogger( StoredQueryHandler.class );
 
     public static final String GET_FEATURE_BY_ID = "urn:ogc:def:query:OGC-WFS::GetFeatureById";
 
    public static final String GET_FEATURE_BY_TYPE = "urn:ogc:def:stored-query:ogc-wfs::GetFeatureByType";
 
     private final Map<String, StoredQueryDefinition> idToQuery = Collections.synchronizedMap( new TreeMap<String, StoredQueryDefinition>() );
 
     private WebFeatureService wfs;
 
     StoredQueryHandler( WebFeatureService wfs ) {
         this.wfs = wfs;
         // add mandatory GetFeatureById query
         URL url = StoredQueryHandler.class.getResource( "idquery.xml" );
         StoredQueryDefinitionXMLAdapter xmlAdapter = new StoredQueryDefinitionXMLAdapter();
         xmlAdapter.load( url );
         addStoredQuery( xmlAdapter.parse() );
         // add GetFeatureByType query
         url = StoredQueryHandler.class.getResource( "typequery.xml" );
         xmlAdapter = new StoredQueryDefinitionXMLAdapter();
         xmlAdapter.load( url );
         addStoredQuery( xmlAdapter.parse() );
     }
 
     private void addStoredQuery( StoredQueryDefinition queryDefinition ) {
         LOG.info( "Adding stored query definition with id '" + queryDefinition.getId() + "'" );
         idToQuery.put( queryDefinition.getId(), queryDefinition );
     }
 
     /**
      * Performs the given {@link CreateStoredQuery} request.
      * 
      * @param request
      *            request to be handled, must not be <code>null</code>
      * @param response
      *            response that is used to write the result, must not be <code>null</code>
      * @throws IOException
      * @throws XMLStreamException
      */
     void doCreateStoredQuery( CreateStoredQuery request, HttpResponseBuffer response ) {
         throw new UnsupportedOperationException( "Performing CreateStoredQuery requests is not implemented yet." );
     }
 
     /**
      * Performs the given {@link DescribeStoredQueries} request.
      * 
      * @param request
      *            request to be handled, must not be <code>null</code>
      * @param response
      *            response that is used to write the result, must not be <code>null</code>
      * @throws IOException
      * @throws XMLStreamException
      * @throws OWSException
      */
     void doDescribeStoredQueries( DescribeStoredQueries request, HttpResponseBuffer response )
                             throws XMLStreamException, IOException, OWSException {
 
         List<StoredQueryDefinition> returnedDescriptions = new ArrayList<StoredQueryDefinition>();
         if ( request.getStoredQueryIds().length == 0 ) {
             returnedDescriptions.addAll( idToQuery.values() );
         } else {
             for ( String id : request.getStoredQueryIds() ) {
                 StoredQueryDefinition queryDef = idToQuery.get( id );
                 if ( queryDef == null ) {
                     String msg = "No StoredQuery with id '" + id + "' is known to this server.";
                     throw new OWSException( msg, INVALID_PARAMETER_VALUE );
                 }
                 returnedDescriptions.add( queryDef );
             }
         }
 
         String schemaLocation = WFS_200_NS + " " + WFS_200_SCHEMA_URL;
         XMLStreamWriter writer = getXMLResponseWriter( response, "text/xml", schemaLocation );
         writer.setDefaultNamespace( WFS_200_NS );
         writer.writeStartElement( WFS_200_NS, "DescribeStoredQueriesResponse" );
         writer.writeDefaultNamespace( WFS_200_NS );
         for ( StoredQueryDefinition queryDef : returnedDescriptions ) {
             writer.writeStartElement( WFS_200_NS, "StoredQueryDescription" );
             writer.writeAttribute( "id", queryDef.getId() );
             for ( LanguageString title : queryDef.getTitles() ) {
                 writer.writeStartElement( WFS_200_NS, "Title" );
                 if ( title.getLanguage() != null ) {
                     // check this
                     writer.writeAttribute( "xml:lang", title.getLanguage() );
                 }
                 writer.writeCharacters( title.getString() );
                 writer.writeEndElement();
             }
             for ( LanguageString abstr : queryDef.getAbstracts() ) {
                 writer.writeStartElement( WFS_200_NS, "Abstract" );
                 if ( abstr.getLanguage() != null ) {
                     // check this
                     writer.writeAttribute( "xml:lang", abstr.getLanguage() );
                 }
                 writer.writeCharacters( abstr.getString() );
                 writer.writeEndElement();
             }
 
             // TODO <xsd:element ref="ows:Metadata" minOccurs="0" maxOccurs="unbounded"/>
 
             // <xsd:element name="Parameter" type="wfs:ParameterExpressionType" minOccurs="0"
             // maxOccurs="unbounded"/>
             for ( Parameter parameter : queryDef.getParameters() ) {
                 writer.writeStartElement( WFS_200_NS, "Parameter" );
 
                 // <xsd:attribute name="name" type="xsd:string" use="required"/>
                 writer.writeAttribute( "name", parameter.getName() );
 
                 // <xsd:attribute name="type" type="xsd:QName" use="required"/>
                 QName type = parameter.getType();
                 String prefixedName = type.getLocalPart();
                 if ( type.getPrefix() != null ) {
                     prefixedName = type.getPrefix() + ":" + type.getLocalPart();
                     writer.writeNamespace( type.getPrefix(), type.getNamespaceURI() );
                 }
                 writer.writeAttribute( "type", prefixedName );
 
                 // <xsd:element ref="wfs:Title" minOccurs="0" maxOccurs="unbounded"/>
                 for ( LanguageString title : parameter.getTitles() ) {
                     writer.writeStartElement( WFS_200_NS, "Title" );
                     if ( title.getLanguage() != null ) {
                         // check this
                         writer.writeAttribute( "xml:lang", title.getLanguage() );
                     }
                     writer.writeCharacters( title.getString() );
                     writer.writeEndElement();
                 }
 
                 // <xsd:element ref="wfs:Abstract" minOccurs="0" maxOccurs="unbounded"/>
                 for ( LanguageString abstr : parameter.getAbstracts() ) {
                     writer.writeStartElement( WFS_200_NS, "Abstract" );
                     if ( abstr.getLanguage() != null ) {
                         // check this
                         writer.writeAttribute( "xml:lang", abstr.getLanguage() );
                     }
                     writer.writeCharacters( abstr.getString() );
                     writer.writeEndElement();
                 }
 
                 // <xsd:element ref="ows:Metadata" minOccurs="0" maxOccurs="unbounded"/>
 
                 writer.writeEndElement();
             }
 
             // <xsd:element name="QueryExpressionText" type="wfs:QueryExpressionTextType" minOccurs="1"
             // maxOccurs="unbounded"/>
             List<QueryExpressionText> queryExprTexts = queryDef.getQueryExpressionTextEls();
             for ( QueryExpressionText queryExprText : queryExprTexts ) {
                 writer.writeStartElement( WFS_200_NS, "QueryExpressionText" );
 
                 // <xsd:attribute name="returnFeatureTypes" type="wfs:ReturnFeatureTypesListType" use="required"/>
                 List<QName> ftNames = new ArrayList<QName>( wfs.getStoreManager().getFeatureTypes().size() );
                 for ( FeatureType ft : wfs.getStoreManager().getFeatureTypes() ) {
                     ftNames.add( ft.getName() );
                 }
                 Collections.sort( ftNames, new Comparator<QName>() {
                     @Override
                     public int compare( QName arg0, QName arg1 ) {
                         String s0 = arg0.toString();
                         String s1 = arg1.toString();
                         return s0.compareTo( s1 );
                     }
                 } );
 
                 StringBuilder returnFeatureTypes = new StringBuilder();
                 Set<String> exportedPrefixes = new HashSet<String>();
                 for ( QName ftName : ftNames ) {
                     if ( returnFeatureTypes.length() != 0 ) {
                         returnFeatureTypes.append( ' ' );
                     }
                     String prefixedName = ftName.getLocalPart();
                     if ( ftName.getPrefix() != null ) {
                         prefixedName = ftName.getPrefix() + ":" + ftName.getLocalPart();
                         if ( !exportedPrefixes.contains( ftName.getPrefix() ) ) {
                             writer.writeNamespace( ftName.getPrefix(), ftName.getNamespaceURI() );
                             exportedPrefixes.add( ftName.getPrefix() );
                         }
                         returnFeatureTypes.append( prefixedName );
                     }
                 }
                 writer.writeAttribute( "returnFeatureTypes", returnFeatureTypes.toString() );
 
                 writer.writeAttribute( "language", queryExprText.getLanguage() );
                 if ( queryExprText.isPrivate() ) {
                     writer.writeAttribute( "isPrivate", "true" );
                 } else {
                     // TODO export actual query expression
                     // XMLStreamReader reader = queryDef.getRootElement().getXMLStreamReader();
                     // skipStartDocument( reader );
                     // writeElement( writer, reader );
                 }
                 writer.writeEndElement();
             }
             writer.writeEndElement();
         }
         writer.writeEndElement();
     }
 
     /**
      * Performs the given {@link DropStoredQuery} request.
      * 
      * @param request
      *            request to be handled, must not be <code>null</code>
      * @param response
      *            response that is used to write the result, must not be <code>null</code>
      * @throws IOException
      * @throws XMLStreamException
      */
     void doDropStoredQuery( DropStoredQuery dropStoredQuery, HttpResponseBuffer response ) {
         throw new UnsupportedOperationException( "Performing DropStoredQuery requests is not implemented yet." );
     }
 
     /**
      * Performs the given {@link ListStoredQueries} request.
      * 
      * @param request
      *            request to be handled, must not be <code>null</code>
      * @param response
      *            response that is used to write the result, must not be <code>null</code>
      * @throws IOException
      * @throws XMLStreamException
      */
     void doListStoredQueries( ListStoredQueries listStoredQueries, HttpResponseBuffer response )
                             throws XMLStreamException, IOException {
         String schemaLocation = WFS_200_NS + " " + WFS_200_SCHEMA_URL;
         XMLStreamWriter writer = getXMLResponseWriter( response, "text/xml", schemaLocation );
         writer.setDefaultNamespace( WFS_200_NS );
         writer.writeStartElement( WFS_200_NS, "ListStoredQueriesResponse" );
         writer.writeDefaultNamespace( WFS_200_NS );
 
         for ( StoredQueryDefinition queryDef : idToQuery.values() ) {
             writer.writeStartElement( WFS_200_NS, "StoredQuery" );
             writer.writeAttribute( "id", queryDef.getId() );
             for ( LanguageString title : queryDef.getTitles() ) {
                 writer.writeStartElement( WFS_200_NS, "Title" );
                 if ( title.getLanguage() != null ) {
                     writer.writeAttribute( "xml:lang", title.getLanguage() );
                 }
                 writer.writeCharacters( title.getString() );
                 writer.writeEndElement();
 
                 List<QName> ftNames = new ArrayList<QName>( wfs.getStoreManager().getFeatureTypes().size() );
                 for ( FeatureType ft : wfs.getStoreManager().getFeatureTypes() ) {
                     ftNames.add( ft.getName() );
                 }
                 Collections.sort( ftNames, new Comparator<QName>() {
                     @Override
                     public int compare( QName arg0, QName arg1 ) {
                         String s0 = arg0.toString();
                         String s1 = arg1.toString();
                         return s0.compareTo( s1 );
                     }
                 } );
 
                 for ( QName ftName : ftNames ) {
                     writer.writeStartElement( WFS_200_NS, "ReturnFeatureType" );
                     String prefix = ftName.getPrefix();
                     if ( prefix == null && ftName.getNamespaceURI() != null ) {
                         prefix = "app";
                     }
                     if ( ftName.getPrefix() != null ) {
                         writer.writeNamespace( prefix, ftName.getNamespaceURI() );
                         writer.writeCharacters( prefix + ":" + ftName.getLocalPart() );
                     } else {
                         writer.writeCharacters( ftName.getLocalPart() );
                     }
                     writer.writeEndElement();
                 }
             }
             writer.writeEndElement();
         }
         writer.writeEndElement();
     }
 }
