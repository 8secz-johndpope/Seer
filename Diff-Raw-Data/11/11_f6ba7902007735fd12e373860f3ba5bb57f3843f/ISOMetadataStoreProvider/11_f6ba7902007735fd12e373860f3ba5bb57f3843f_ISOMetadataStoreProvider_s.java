 //$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/deegree3/trunk/deegree-core/deegree-core-metadata/src/main/java/org/deegree/metadata/iso/persistence/ISOMetadataStoreProvider.java $
 /*----------------------------------------------------------------------------
  This file is part of deegree, http://deegree.org/
  Copyright (C) 2001-2009 by:
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
 package org.deegree.metadata.iso.persistence;
 
 import static org.slf4j.LoggerFactory.getLogger;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.UnsupportedEncodingException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.List;
 
 import javax.xml.bind.JAXBContext;
 import javax.xml.bind.JAXBException;
 import javax.xml.bind.Unmarshaller;
 
 import org.deegree.commons.config.DeegreeWorkspace;
 import org.deegree.commons.config.ResourceInitException;
 import org.deegree.commons.config.ResourceManager;
 import org.deegree.commons.jdbc.ConnectionManager;
 import org.deegree.commons.jdbc.ConnectionManager.Type;
 import org.deegree.commons.utils.ProxyUtils;
 import org.deegree.filter.function.FunctionManager;
 import org.deegree.metadata.i18n.Messages;
 import org.deegree.metadata.persistence.MetadataStoreProvider;
 import org.deegree.metadata.persistence.iso19115.jaxb.ISOMetadataStoreConfig;
 import org.deegree.sqldialect.SQLDialect;
 import org.deegree.sqldialect.SQLDialectManager;
 import org.slf4j.Logger;
 
 /**
  * {@link MetadataStoreProvider} for the {@link ISOMetadataStore}.
  * 
  * @author <a href="mailto:thomas@lat-lon.de">Steffen Thomas</a>
  * @author last edited by: $Author: lbuesching $
  * 
  * @version $Revision: 30800 $, $Date: 2011-05-12 16:49:44 +0200 (Do, 12. Mai 2011) $
  */
 public class ISOMetadataStoreProvider implements MetadataStoreProvider {
 
     private static final Logger LOG = getLogger( ISOMetadataStoreProvider.class );
 
     private DeegreeWorkspace workspace;
 
     @Override
     public String getConfigNamespace() {
         return "http://www.deegree.org/datasource/metadata/iso19115";
     }
 
     @Override
     public URL getConfigSchema() {
         return ISOMetadataStoreProvider.class.getResource( "/META-INF/schemas/datasource/metadata/iso19115/3.2.0/iso19115.xsd" );
     }
 
     @Override
     public String[] getCreateStatements( Type dbType )
                             throws UnsupportedEncodingException, IOException {
         List<String> creates = new ArrayList<String>();
         if ( dbType == Type.MSSQL ) {
             URL script = ISOMetadataStoreProvider.class.getResource( "mssql/create.sql" );
             creates.addAll( readStatements( new BufferedReader( new InputStreamReader( script.openStream(), "UTF-8" ) ) ) );
             script = ISOMetadataStoreProvider.class.getResource( "mssql/create_inspire.sql" );
             creates.addAll( readStatements( new BufferedReader( new InputStreamReader( script.openStream(), "UTF-8" ) ) ) );
         } else if ( dbType == Type.PostgreSQL ) {
             URL script = ISOMetadataStoreProvider.class.getResource( "postgis/create.sql" );
             creates.addAll( readStatements( new BufferedReader( new InputStreamReader( script.openStream(), "UTF-8" ) ) ) );
             script = ISOMetadataStoreProvider.class.getResource( "postgis/create_inspire.sql" );
             creates.addAll( readStatements( new BufferedReader( new InputStreamReader( script.openStream(), "UTF-8" ) ) ) );
         }
         return creates.toArray( new String[creates.size()] );
     }
 
     @Override
     public String[] getDropStatements( Type dbType )
                             throws UnsupportedEncodingException, IOException {
         List<String> creates = new ArrayList<String>();
         if ( dbType == Type.MSSQL ) {
             URL script = ISOMetadataStoreProvider.class.getResource( "mssql/drop_inspire.sql" );
             creates.addAll( readStatements( new BufferedReader( new InputStreamReader( script.openStream(), "UTF-8" ) ) ) );
             script = ISOMetadataStoreProvider.class.getResource( "mssql/drop.sql" );
             creates.addAll( readStatements( new BufferedReader( new InputStreamReader( script.openStream(), "UTF-8" ) ) ) );
         } else if ( dbType == Type.PostgreSQL ) {
             URL script = ISOMetadataStoreProvider.class.getResource( "postgis/drop_inspire.sql" );
             creates.addAll( readStatements( new BufferedReader( new InputStreamReader( script.openStream(), "UTF-8" ) ) ) );
             script = ISOMetadataStoreProvider.class.getResource( "postgis/drop.sql" );
             creates.addAll( readStatements( new BufferedReader( new InputStreamReader( script.openStream(), "UTF-8" ) ) ) );
         }
         return creates.toArray( new String[creates.size()] );
     }
 
     private List<String> readStatements( BufferedReader reader )
                             throws IOException {
         List<String> stmts = new ArrayList<String>();
         String currentStmt = "";
         String line = null;
         while ( ( line = reader.readLine() ) != null ) {
             if ( line.startsWith( "--" ) || line.trim().isEmpty() ) {
                 // skip
             } else if ( line.contains( ";" ) ) {
                 currentStmt += line.substring( 0, line.indexOf( ';' ) );
                 stmts.add( currentStmt );
                 currentStmt = "";
             } else {
                 currentStmt += line + "\n";
             }
         }
         return stmts;
     }
 
     public ISOMetadataStore create( URL configURL )
                             throws ResourceInitException {
         if ( configURL == null ) {
             LOG.warn( Messages.getMessage( "WARN_NO_CONFIG" ) );
         } else {
             try {
                 JAXBContext jc;
                 if ( workspace == null ) {
                     jc = JAXBContext.newInstance( "org.deegree.metadata.persistence.iso19115.jaxb" );
                 } else {
                     jc = JAXBContext.newInstance( "org.deegree.metadata.persistence.iso19115.jaxb",
                                                   workspace.getModuleClassLoader() );
                 }
                 Unmarshaller u = jc.createUnmarshaller();
 
                 ISOMetadataStoreConfig cfg = (ISOMetadataStoreConfig) u.unmarshal( configURL );
 
                 ConnectionManager mgr = workspace.getSubsystemManager( ConnectionManager.class );
                 Type connType = mgr.getType( cfg.getJDBCConnId() );
                 if ( connType == null ) {
                     throw new ResourceInitException( "No JDBC connection with id '" + cfg.getJDBCConnId()
                                                      + "' defined." );
                 }
                 LOG.debug( "Connection type is {}.", connType );
 
                 SQLDialectManager dialectMgr = workspace.getSubsystemManager( SQLDialectManager.class );
                 if ( dialectMgr == null ) {
                     throw new ResourceInitException( "SQLDialectManager not found in workspace / classpath." );
                 }
                 SQLDialect dialect = dialectMgr.create( cfg.getJDBCConnId() );
                 return new ISOMetadataStore( cfg, dialect );
             } catch ( JAXBException e ) {
                 String msg = Messages.getMessage( "ERROR_IN_CONFIG_FILE", configURL, e.getMessage() );
                 LOG.error( msg );
                 throw new ResourceInitException( msg, e );
             }
         }
         return null;
     }
 
     @SuppressWarnings("unchecked")
     public Class<? extends ResourceManager>[] getDependencies() {
         return new Class[] { ProxyUtils.class, ConnectionManager.class, FunctionManager.class, SQLDialectManager.class };
     }
 
     public void init( DeegreeWorkspace workspace ) {
         this.workspace = workspace;
     }
 }
