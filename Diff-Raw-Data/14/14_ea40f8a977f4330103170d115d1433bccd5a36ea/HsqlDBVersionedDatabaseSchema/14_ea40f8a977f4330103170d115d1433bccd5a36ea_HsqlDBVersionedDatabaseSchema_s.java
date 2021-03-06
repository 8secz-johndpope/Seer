 /*
  * Copyright (C) 2009 - 2012 OpenSubsystems.com/net/org and its owners. All rights reserved.
  * 
  * This file is part of OpenSubsystems.
  *
  * OpenSubsystems is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
  */
 
 package org.opensubsystems.core.persist.jdbc.database.hsqldb;
 
 import java.sql.Connection;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.opensubsystems.core.error.OSSException;
 import org.opensubsystems.core.persist.jdbc.impl.DatabaseImpl;
 import org.opensubsystems.core.persist.jdbc.impl.VersionedDatabaseSchemaImpl;
 import org.opensubsystems.core.util.Log;
 import org.opensubsystems.core.util.jdbc.DatabaseUtils;
 
 /**
  * Database specific operations related to persistence of database schemas in 
  * HSQLDB.
  *
  * @author bastafidli
  */
 public class HsqlDBVersionedDatabaseSchema extends VersionedDatabaseSchemaImpl
 {
    /*
       Use autogenerated numbers for IDs using IDENTITY column.
       Identity automatically defines primary key
       Name all constraints to easily identify them later.
       For all unique constraint we need to define unique indexes instead of 
       unique constraint otherwise we won't be able to identify the violation of
       this constraint by name. 
 
       CREATE TABLE BF_SCHEMA
       (
          ID INTEGER IDENTITY,
          SCHEMA_NAME VARCHAR(50) NOT NULL,
          SCHEMA_VERSION INTEGER NOT NULL,
          CREATION_DATE TIMESTAMP NOT NULL,
          MODIFICATION_DATE TIMESTAMP NOT NULL,
          CONSTRAINT BF_SCH_UQ UNIQUE (SCHEMA_NAME)
          // CONSTRAINT BF_SCH_PK PRIMARY KEY (ID),
       ) 
    
       create unique index BF_SCH_UQ on BF_SCHEMA (SCHEMA_NAME)
    */
    
    // Cached values ////////////////////////////////////////////////////////////
 
    /**
     * Logger for this class
     */
    private static Logger s_logger = Log.getInstance(HsqlDBVersionedDatabaseSchema.class);
 
    // Constructors /////////////////////////////////////////////////////////////
    
    /**
     * @throws OSSException - database cannot be started.
     */
    public HsqlDBVersionedDatabaseSchema(
    ) throws OSSException
    {
       super();
    }
    
    /**
     * {@inheritDoc}
     */
    public void create(
       Connection cntDBConnection, 
       String     strUserName
    ) throws SQLException
    {
       s_logger.entering(this.getClass().getName(), "create");
 
       try
       {
          Statement stmQuery = null;
          try
          {
             stmQuery = cntDBConnection.createStatement();
             if (stmQuery.execute(
                "create table " + SCHEMA_TABLE_NAME + NL +
                "(" + NL +
                "   ID INTEGER IDENTITY," + NL +
                "   SCHEMA_NAME VARCHAR(" + SCHEMA_NAME_MAXLENGTH + ") NOT NULL," + NL +
                "   SCHEMA_VERSION INTEGER NOT NULL," + NL +
                "   CREATION_DATE TIMESTAMP NOT NULL," + NL +
                "   MODIFICATION_DATE TIMESTAMP NOT NULL," + NL +
                // Identity automatically defines primary key
                // "   CONSTRAINT " + getSchemaPrefix() + "SCH_PK PRIMARY KEY (ID)," + NL +
                "   CONSTRAINT " + getSchemaPrefix() + "SCH_UQ UNIQUE (SCHEMA_NAME)" + NL +
                ")"))
             {
                // Close any results
                stmQuery.getMoreResults(Statement.CLOSE_ALL_RESULTS);
             }
            s_logger.log(Level.FINEST, "Table " + SCHEMA_TABLE_NAME + " created.");
 
             if (stmQuery.execute("grant all on " + SCHEMA_TABLE_NAME + " to " 
                                  + strUserName))
             {
                // Close any results
                stmQuery.getMoreResults(Statement.CLOSE_ALL_RESULTS);
             }
            s_logger.log(Level.FINEST, "Access for table " + SCHEMA_TABLE_NAME 
                                       + " set for user " + strUserName);
             
             ///////////////////////////////////////////////////////////////////////
          }
          catch (SQLException sqleExc)
          {
             // Catch this just so we can log the message
             s_logger.log(Level.WARNING, "Failed to create version schema.",
                                 sqleExc);
             throw sqleExc;
          }
          finally
          {
             DatabaseUtils.closeStatement(stmQuery);
          }
       }
       finally
       {
          s_logger.exiting(this.getClass().getName(), "create");
       }
    }
 
    /**
     * Method returns simple insert user query. This method is common for all
     * databases and can be overwritten for each specific database schema.
     *
     * @return String - simple insert schema query
     * @throws OSSException - exception during getting query
     */
    public String getInsertSchema(
    ) throws OSSException
    {
      StringBuffer buffer = new StringBuffer();
       
       buffer.append("insert into " + SCHEMA_TABLE_NAME 
                     + " (ID, SCHEMA_NAME, SCHEMA_VERSION,"
                     + " CREATION_DATE, MODIFICATION_DATE)"
                     + " values (null, ?,?,");
       buffer.append(DatabaseImpl.getInstance().getSQLCurrentTimestampFunctionCall());
       buffer.append(",");
       buffer.append(DatabaseImpl.getInstance().getSQLCurrentTimestampFunctionCall());
       buffer.append(")");
 
       return buffer.toString();
    }
 }
