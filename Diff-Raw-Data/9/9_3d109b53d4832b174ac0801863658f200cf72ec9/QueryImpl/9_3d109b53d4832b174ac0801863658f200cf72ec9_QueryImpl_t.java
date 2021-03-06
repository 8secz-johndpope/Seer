 /*
  * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
  *
  * This file is part of Resin(R) Open Source
  *
  * Each copy or derived work must preserve the copyright notice and this
  * notice unmodified.
  *
  * Resin Open Source is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * Resin Open Source is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
  * of NON-INFRINGEMENT.  See the GNU General Public License for more
  * details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Resin Open Source; if not, write to the
  *
  *   Free Software Foundation, Inc.
  *   59 Temple Place, Suite 330
  *   Boston, MA 02111-1307  USA
  *
  * @author Scott Ferguson
  */
 
 package com.caucho.amber.manager;
 
 import java.lang.reflect.Constructor;
 
 import java.sql.Connection;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.ResultSetMetaData;
 import java.sql.SQLException;
 import java.sql.Types;
 
 import java.util.List;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.Calendar;
 
 import javax.persistence.Query;
 import javax.persistence.TemporalType;
 import javax.persistence.FlushModeType;
 
 import com.caucho.amber.AmberException;
 
 import com.caucho.amber.entity.Entity;
 
 import com.caucho.amber.manager.AmberConnection;
 
 import com.caucho.amber.query.AbstractQuery;
 import com.caucho.amber.query.ResultSetImpl;
 import com.caucho.amber.query.SelectQuery;
 import com.caucho.amber.query.UserQuery;
 
 import com.caucho.amber.type.CalendarType;
 import com.caucho.amber.type.UtilDateType;
 
 import com.caucho.util.L10N;
 
 import com.caucho.ejb.EJBExceptionWrapper;
 
 /**
  * The EJB query
  */
 public class QueryImpl implements Query {
   private static final L10N L = new L10N(QueryImpl.class);
 
   private AbstractQuery _query;
   private UserQuery _userQuery;
 
   private AmberConnection _aConn;
   private int _firstResult;
   private int _maxResults = Integer.MAX_VALUE / 2;
 
   private int _currIndex;
 
   /**
    * Creates a manager instance.
    */
   QueryImpl(AbstractQuery query, AmberConnection aConn)
   {
     _query = query;
     _aConn = aConn;
 
     _userQuery = new UserQuery(query);
     _userQuery.setSession(_aConn);
   }
 
   /**
    * Execute the query and return as a List.
    */
   public List getResultList()
   {
     try {
 
       Class constructorClass = null;
 
       SelectQuery selectQuery;
 
       if (_query instanceof SelectQuery) {
         selectQuery = (SelectQuery) _query;
         constructorClass = selectQuery.getConstructorClass();
       }
       else
         throw new IllegalStateException(L.l("javax.persistence.Query.getResultList() can only be applied to a SELECT statement"));
 
       Constructor constructor = null;
 
       ArrayList results = new ArrayList();
 
       ResultSet rs = executeQuery();
 
       ResultSetMetaData metaData = null;
       int columnCount = -1;
 
       int n = 0;
 
       Object row[] = null;
 
       ArrayList columns = new ArrayList();
 
       while (rs.next()) {
         Object object = null;
 
         _currIndex = 1;
 
         if (n == 0) {
 
           try {
 
             metaData = rs.getMetaData();
 
             if (metaData != null)
               columnCount = metaData.getColumnCount();
 
           } catch (Exception ex) {
             // Below, we work around if DB is not able
             // to retrieve result set meta data. jpa/0t00
             metaData = null;
           }
 
           if (columnCount <= 0)
             columnCount = 10000;
 
           for (int i=1; i <= columnCount; i++) {
 
             int columnType = -1;
 
             try {
               columnType = metaData.getColumnType(i);
             } catch (Exception ex) {
             }
 
             try {
 
               object = getInternalObject(rs, columnType);
 
               columns.add(object);
 
            } catch (IndexOutOfBoundsException ex1) {
               break;
             }
             // catch (Exception ex2) {
             //  break;
             // }
           }
 
           n = columns.size();
           row = columns.toArray();
 
           if (constructorClass != null) {
 
             StringBuilder argTypes = new StringBuilder();
 
             try {
 
               Class paramTypes[] = new Class[n];
 
               boolean isFirst = true;
 
               for (int i=0; i < n; i++) {
                 paramTypes[i] = row[i].getClass();
 
                 if (isFirst)
                   isFirst = false;
                 else
                   argTypes.append(", ");
 
                 argTypes.append(paramTypes[i].getName());
               }
 
               constructor = constructorClass.getDeclaredConstructor(paramTypes);
 
             } catch (Exception ex) {
               throw error(L.l("Unable to find constructor {0}. Make sure there is a public constructor for the given argument types ({1})", constructorClass.getName(), argTypes));
             }
           }
         }
         else {
 
           row = new Object[n];
 
           for (int i=0; i < n; i++) {
 
             int columnType = -1;
 
             try {
               columnType = metaData.getColumnType(i + 1);
             } catch (Exception ex) {
             }
 
             row[i] = getInternalObject(rs, columnType);
           }
         }
 
         if (constructor == null) {
           if (n == 1)
             results.add(row[0]);
           else
             results.add(row);
         }
         else {
 
           try {
             object = constructor.newInstance(row);
           } catch (Exception ex) {
 
             StringBuilder argTypes = new StringBuilder();
 
             boolean isFirst = true;
 
             for (int i=0; i<row.length; i++) {
 
               if (isFirst)
                 isFirst = false;
               else
                 argTypes.append(", ");
 
               if (row[i] == null)
                 argTypes.append("null");
               else
                 argTypes.append(row[i].toString()); // .getClass().getName());
             }
 
             throw error(L.l("Unable to instantiate {0} with parameters ({1}).", constructorClass.getName(), argTypes));
           }
 
           results.add(object);
         }
       }
 
       rs.close();
 
       return results;
     } catch (Exception e) {
       throw EJBExceptionWrapper.createRuntime(e);
     }
   }
 
   /**
    * Returns a single result.
    */
   public Object getSingleResult()
   {
     try {
       ResultSet rs = executeQuery();
 
       Object value = null;
 
       if (rs.next())
         value = rs.getObject(1);
 
       rs.close();
 
       return value;
     } catch (Exception e) {
       throw EJBExceptionWrapper.createRuntime(e);
     }
   }
 
   /**
    * Execute an update or delete.
    */
   public int executeUpdate()
   {
     try {
       return _userQuery.executeUpdate();
     } catch (Exception e) {
       throw EJBExceptionWrapper.createRuntime(e);
     }
   }
 
   /**
    * Executes the query returning a result set.
    */
   protected ResultSet executeQuery()
     throws SQLException
   {
     return _userQuery.executeQuery();
   }
 
   /**
    * Sets the maximum result returned.
    */
   public Query setMaxResults(int maxResult)
   {
     return this;
   }
 
   /**
    * Sets the position of the first result.
    */
   public Query setFirstResult(int startPosition)
   {
     return this;
   }
 
   /**
    * Sets a hint.
    */
   public Query setHint(String hintName, Object value)
   {
     return this;
   }
 
   /**
    * Sets a named parameter.
    */
   public Query setParameter(String name, Object value)
   {
     ArrayList<String> mapping = _query.getPreparedMapping();
 
     int n = mapping.size();
 
     for (int i=0; i < n; i++) {
       if (mapping.get(i).equals(name)) {
         setParameter(i+1, value);
       }
     }
 
     return this;
   }
 
   /**
    * Sets a date parameter.
    */
   public Query setParameter(String name, Date value, TemporalType type)
   {
     ArrayList<String> mapping = _query.getPreparedMapping();
 
     int n = mapping.size();
 
     for (int i=0; i < n; i++) {
       if (mapping.get(i).equals(name)) {
         setParameter(i+1, value, type);
       }
     }
 
     return this;
   }
 
   /**
    * Sets a calendar parameter.
    */
   public Query setParameter(String name, Calendar value, TemporalType type)
   {
     ArrayList<String> mapping = _query.getPreparedMapping();
 
     int n = mapping.size();
 
     for (int i=0; i < n; i++) {
       if (mapping.get(i).equals(name)) {
         setParameter(i+1, value, type);
       }
     }
 
     return this;
   }
 
   /**
    * Sets an indexed parameter.
    */
   public Query setParameter(int index, Object value)
   {
     if (value == null)
       _userQuery.setNull(index, java.sql.Types.JAVA_OBJECT);
     else if (value instanceof Double)
       _userQuery.setString(index, value.toString()); // commented out (jpa/141a): ((Double) value).doubleValue());
     else if (value instanceof Character)
       _userQuery.setString(index, value.toString());
     else {
       _userQuery.setObject(index, value);
     }
 
     return this;
   }
 
   /**
    * Sets a date parameter.
    */
   public Query setParameter(int index, Date value, TemporalType type)
   {
     if (value == null)
       _userQuery.setNull(index, Types.JAVA_OBJECT);
     else {
       switch (type) {
       case TIME:
         _userQuery.setObject(index, value, UtilDateType.TEMPORAL_TIME_TYPE);
         break;
 
       case DATE:
         _userQuery.setObject(index, value, UtilDateType.TEMPORAL_DATE_TYPE);
         break;
 
       default:
         _userQuery.setObject(index, value, UtilDateType.TEMPORAL_TIMESTAMP_TYPE);
       }
     }
 
     return this;
   }
 
   /**
    * Sets a calendar parameter.
    */
   public Query setParameter(int index, Calendar value, TemporalType type)
   {
     if (value == null)
       _userQuery.setNull(index, Types.JAVA_OBJECT);
     else {
       switch (type) {
       case TIME:
         _userQuery.setObject(index, value, CalendarType.TEMPORAL_TIME_TYPE);
         break;
 
       case DATE:
         _userQuery.setObject(index, value, CalendarType.TEMPORAL_DATE_TYPE);
         break;
 
       default:
         _userQuery.setObject(index, value, CalendarType.TEMPORAL_TIMESTAMP_TYPE);
       }
     }
 
     return this;
   }
 
   /**
    * Sets the flush mode type.
    */
   public Query setFlushMode(FlushModeType mode)
   {
     return this;
   }
 
   //
   // extensions
 
   /**
    * Sets an indexed parameter.
    */
   public Query setDouble(int index, double value)
   {
     _userQuery.setDouble(index, value);
 
     return this;
   }
 
   /**
    * Creates an error.
    */
   private AmberException error(String msg)
   {
     msg += "\nin \"" + _query.getQueryString() + "\"";
 
     return new AmberException(msg);
   }
 
   /**
    * Returns the object using the correct
    * result set getter based on SQL type.
    */
   private Object getInternalObject(ResultSet rs,
                                    int columnType)
     throws Exception
   {
     // jpa/110-, jpa/11a4, and jpa/11z1
 
     int oldIndex = _currIndex;
 
     _currIndex++;
 
     Object object = rs.getObject(oldIndex);
 
     if (object instanceof Entity) {
       // _currIndex += ((ResultSetImpl) rs).getNumberOfLoadingColumns();
 
       return object;
     }
 
     if (object == null)
       return null;
 
     switch (columnType) {
     case Types.BIT:
     case Types.BOOLEAN:
       //      try {
       //        object = rs.getInt(oldIndex);
       //      } catch (Exception ex) {
       if (! (object instanceof Boolean))
         object = rs.getBoolean(oldIndex);
       //      }
       break;
 
     case Types.TINYINT:
       if (! (object instanceof Number))
         object = rs.getByte(oldIndex);
       break;
 
     case Types.SMALLINT:
       if (! (object instanceof Number))
         object = rs.getShort(oldIndex);
       break;
 
     case Types.INTEGER:
       if (! (object instanceof Number))
         object = rs.getLong(oldIndex);
       break;
 
     case Types.DECIMAL:
     case Types.DOUBLE:
     case Types.NUMERIC:
     case Types.REAL:
       if (! (object instanceof Number))
         object = rs.getDouble(oldIndex);
       break;
 
     case Types.FLOAT:
       if (! (object instanceof Number))
         object = rs.getFloat(oldIndex);
       break;
 
     // It was fetched with getObject (see top).
     // default:
     //  object = rs.getObject(oldIndex);
     }
 
     return object;
   }
 }
