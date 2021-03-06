 //$HeadURL: svn+ssh://mschneider@svn.wald.intevation.org/deegree/deegree3/trunk/deegree-core/src/main/java/org/deegree/feature/persistence/postgis/PostGISFeatureStore.java $
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
 package org.deegree.console;
 
 import java.sql.Connection;
 import java.sql.SQLException;
 import java.sql.Statement;
 
 import org.deegree.commons.jdbc.ConnectionManager;
 import org.deegree.commons.utils.JDBCUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
public class SQLExecution {
 
     private static Logger LOG = LoggerFactory.getLogger( SQLExecution.class );
 
     private String connId;
 
     private String[] sqlStatements;
 
     private String message = "Click Execute button to create tables.";
 
     public SQLExecution( String connId, String[] sqlStatements ) {
         this.connId = connId;
         this.sqlStatements = sqlStatements;
     }
 
     public String getMessage() {
         return message;
     }
 
     public String getStatements() {
         StringBuffer sql = new StringBuffer();
         for ( int i = 0; i < sqlStatements.length; i++ ) {
             sql.append( sqlStatements[i] );
             if ( !sqlStatements[i].trim().isEmpty() ) {
                 sql.append( ";" );
             }
             sql.append( "\n" );
         }
         return sql.toString();
     }
 
     public void setStatements( String sql ) {
         sqlStatements = sql.split( ";\\s*" );
     }
 
     public void execute() {
         Connection conn = null;
         Statement stmt = null;
         try {
             conn = ConnectionManager.getConnection( connId );
             conn.setAutoCommit( false );
             stmt = conn.createStatement();
             for ( String sql : sqlStatements ) {
                 LOG.debug( "Executing: {}", sql );
                 stmt.execute( sql );
             }
             conn.commit();
             message = "Executed " + sqlStatements.length + " statements successfully.";
         } catch ( SQLException e ) {
             try {
                 conn.rollback();
             } catch ( SQLException e1 ) {
                 e1.printStackTrace();
             }
             JDBCUtils.close( null, stmt, conn, LOG );
             message = "Error: " + e.getMessage();
         }
     }
 }
