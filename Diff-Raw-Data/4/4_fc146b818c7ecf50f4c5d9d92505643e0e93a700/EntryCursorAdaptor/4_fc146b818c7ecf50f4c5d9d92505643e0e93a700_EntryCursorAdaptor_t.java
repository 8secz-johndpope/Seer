 /*
  *   Licensed to the Apache Software Foundation (ASF) under one
  *   or more contributor license agreements.  See the NOTICE file
  *   distributed with this work for additional information
  *   regarding copyright ownership.  The ASF licenses this file
  *   to you under the Apache License, Version 2.0 (the
  *   "License"); you may not use this file except in compliance
  *   with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  *   Unless required by applicable law or agreed to in writing,
  *   software distributed under the License is distributed on an
  *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  *   KIND, either express or implied.  See the License for the
  *   specific language governing permissions and limitations
  *   under the License.
  *
  */
 package org.apache.directory.server.core.partition.impl.btree;
 
 
 import org.apache.directory.server.xdbm.IndexEntry;
 import org.apache.directory.server.xdbm.search.Evaluator;
 import org.apache.directory.server.xdbm.search.PartitionSearchResult;
 import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
 import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
 import org.apache.directory.shared.ldap.model.cursor.Cursor;
 import org.apache.directory.shared.ldap.model.entry.Entry;
 import org.apache.directory.shared.ldap.model.filter.ExprNode;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 
 /**
  * Adapts index cursors to return just Entry objects.
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  */
 public class EntryCursorAdaptor extends AbstractCursor<Entry>
 {
     /** A dedicated log for cursors */
     private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );
 
     /** Speedup for logs */
     private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();
 
     private final AbstractBTreePartition db;
     private final Cursor<IndexEntry<String, String>> indexCursor;
     private final Evaluator<? extends ExprNode> evaluator;
 
 
     public EntryCursorAdaptor( AbstractBTreePartition db, PartitionSearchResult searchResult )
     {
     	if ( IS_DEBUG )
     	{
     		LOG_CURSOR.debug( "Creating EntryCursorAdaptor {}", this );
     	}
     	
         this.db = db;
         indexCursor = searchResult.getResultSet();
         evaluator = searchResult.getEvaluator();
     }
 
 
     /* 
      * @see Cursor#after(java.lang.Object)
      */
     public void after( Entry element ) throws Exception
     {
         throw new UnsupportedOperationException();
     }
 
 
     /* 
      * @see Cursor#afterLast()
      */
     public void afterLast() throws Exception
     {
         this.indexCursor.afterLast();
     }
 
 
     /* 
      * @see Cursor#available()
      */
     public boolean available()
     {
         return indexCursor.available();
     }
 
 
     /* 
      * @see Cursor#before(java.lang.Object)
      */
     public void before( Entry element ) throws Exception
     {
         throw new UnsupportedOperationException();
     }
 
 
     /* 
      * @see Cursor#beforeFirst()
      */
     public void beforeFirst() throws Exception
     {
         indexCursor.beforeFirst();
     }
 
 
     public final void setClosureMonitor( ClosureMonitor monitor )
     {
         indexCursor.setClosureMonitor( monitor );
     }
 
 
     /**
      * {@inheritDoc}}
      */
     public void close() throws Exception
     {
     	if ( IS_DEBUG )
     	{
     		LOG_CURSOR.debug( "Closing EntryCursorAdaptor {}", this );
     	}
     	
         indexCursor.close();
     }
 
 
     /**
      * {@inheritDoc}
      */
     public void close( Exception cause ) throws Exception
     {
     	if ( IS_DEBUG )
     	{
     		LOG_CURSOR.debug( "Closing EntryCursorAdaptor {}", this );
     	}
     	
         indexCursor.close( cause );
     }
 
 
     /* 
      * @see Cursor#first()
      */
     public boolean first() throws Exception
     {
         return indexCursor.first();
     }
 
 
     /* 
      * @see Cursor#get()
      */
     public Entry get() throws Exception
     {
         IndexEntry<String, String> indexEntry = indexCursor.get();
 
         if ( evaluator.evaluate( indexEntry ) )
         {
             return indexEntry.getEntry();
         }
        else
        {
            indexEntry.setEntry( null );
        }
 
         return null;
     }
 
 
     /* 
      * @see Cursor#isClosed()
      */
     public boolean isClosed() throws Exception
     {
         return indexCursor.isClosed();
     }
 
 
     /* 
      * @see Cursor#last()
      */
     public boolean last() throws Exception
     {
         return indexCursor.last();
     }
 
 
     /* 
      * @see Cursor#next()
      */
     public boolean next() throws Exception
     {
         return indexCursor.next();
     }
 
 
     /* 
      * @see Cursor#previous()
      */
     public boolean previous() throws Exception
     {
         return indexCursor.previous();
     }
 }
