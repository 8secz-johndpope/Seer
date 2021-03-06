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
 package org.apache.directory.server.core.partition.avl;
 
 
 import java.io.File;
 
 import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
 import org.apache.directory.server.core.partition.impl.btree.LongComparator;
 import org.apache.directory.server.xdbm.Index;
 import org.apache.directory.server.xdbm.IndexCursor;
 import org.apache.directory.server.xdbm.Tuple;
 import org.apache.directory.shared.ldap.cursor.Cursor;
 import org.apache.directory.shared.ldap.entry.client.ClientBinaryValue;
 import org.apache.directory.shared.ldap.schema.AttributeType;
 import org.apache.directory.shared.ldap.schema.LdapComparator;
 import org.apache.directory.shared.ldap.schema.MatchingRule;
 import org.apache.directory.shared.ldap.schema.Normalizer;
 
 
 /**
  * An Index backed by an AVL Tree.
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$, $Date$
  */
 public class AvlIndex<K,O> implements Index<K, O>
 {
     private Normalizer normalizer;
     private AttributeType attributeType;
     private AvlTable<K,Long> forward;
     private AvlTable<Long,K> reverse;
     private String attributeId;
     
     
     public AvlIndex()
     {
     }
 
     
     public AvlIndex( String attributeId )
     {
         setAttributeId( attributeId );
     }
 
     
     void initialize( AttributeType attributeType ) throws Exception
     {
         this.attributeType = attributeType;
 
         MatchingRule mr = attributeType.getEquality();
         
         if ( mr == null )
         {
             mr = attributeType.getOrdering();
         }
         
         if ( mr == null )
         {
             mr = attributeType.getSubstring();
         }
 
         normalizer = mr.getNormalizer();
         
         if ( normalizer == null )
         {
             throw new Exception( "No Normalizer present for attribute type " + attributeType );
         }
 
         LdapComparator comp = mr.getLdapComparator();
 
         /*
          * The forward key/value map stores attribute values to master table 
          * primary keys.  A value for an attribute can occur several times in
          * different entries so the forward map can have more than one value.
          */
         forward = new AvlTable<K, Long>(
             attributeType.getName(), 
             comp, LongComparator.INSTANCE, true);
 
         /*
          * Now the reverse map stores the primary key into the master table as
          * the key and the values of attributes as the value.  If an attribute
          * is single valued according to its specification based on a schema 
          * then duplicate keys should not be allowed within the reverse table.
          */
         if ( attributeType.isSingleValued() )
         {
             reverse = new AvlTable<Long,K>(
                 attributeType.getName(),
                 LongComparator.INSTANCE,
                 comp,
                 false );
         }
         else
         {
             reverse = new AvlTable<Long,K>(
                 attributeType.getName(),
                 LongComparator.INSTANCE, comp,
                 true);
         }
     }
 
     
     public void add( K attrVal, Long id ) throws Exception
     {
         forward.put( attrVal, id );
        reverse.put( id, getNormalized( attrVal ) );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public void close() throws Exception
     {
         forward.close();
         reverse.close();
     }
 
 
     /**
      * {@inheritDoc}
      */
     public int count() throws Exception
     {
         return forward.count();
     }
 
 
     /**
      * {@inheritDoc}
      */
     public int count( K attrVal ) throws Exception
     {
         return forward.count( attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public void drop( Long id ) throws Exception
     {
         Cursor<Tuple<Long,K>> cursor = reverse.cursor( id );
         
         while ( cursor.next() )
         {
             Tuple<Long,K> tuple = cursor.get();
             forward.remove( tuple.getValue(), id );
         }
         
         reverse.remove( id );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public void drop( K attrVal, Long id ) throws Exception
     {
         forward.remove( attrVal, id );
         reverse.remove( id, attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean forward( K attrVal ) throws Exception
     {
         return forward.has( attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean forward( K attrVal, Long id ) throws Exception
     {
         return forward.has( attrVal, id );
     }
 
 
     /**
      * {@inheritDoc}
      */
     @SuppressWarnings("unchecked")
     public IndexCursor<K, O> forwardCursor() throws Exception
     {
         return new IndexCursorAdaptor( forward.cursor(), true );
     }
 
 
     /**
      * {@inheritDoc}
      */
     @SuppressWarnings("unchecked")
     public IndexCursor<K, O> forwardCursor( K key ) throws Exception
     {
         return new IndexCursorAdaptor( forward.cursor( key ), true );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean forwardGreaterOrEq( K attrVal ) throws Exception
     {
         return forward.hasGreaterOrEqual( attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean forwardGreaterOrEq( K attrVal, Long id ) throws Exception
     {
         return forward.hasGreaterOrEqual( attrVal, id );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean forwardLessOrEq( K attrVal ) throws Exception
     {
         return forward.hasLessOrEqual( attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean forwardLessOrEq( K attrVal, Long id ) throws Exception
     {
         return forward.hasLessOrEqual( attrVal, id );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public Long forwardLookup( K attrVal ) throws Exception
     {
         return forward.get( attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public Cursor<Long> forwardValueCursor( K key ) throws Exception
     {
         return forward.valueCursor( key );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public AttributeType getAttribute()
     {
         return attributeType;
     }
 
 
     /**
      * {@inheritDoc}
      */
     public String getAttributeId()
     {
         return attributeId;
     }
 
 
     /**
      * {@inheritDoc}
      */
     @SuppressWarnings("unchecked")
     public K getNormalized( K attrVal ) throws Exception
     {
         if( attrVal instanceof Long )
         {
             return attrVal;
         }
 
         if( attrVal instanceof String )
         {
             return ( K ) normalizer.normalize( ( String ) attrVal );
         }
         else
         {
             return ( K ) normalizer.normalize( new ClientBinaryValue( ( byte[] ) attrVal ) );
         }
     }
 
 
     /**
      * {@inheritDoc}
      */
     public int greaterThanCount( K attrVal ) throws Exception
     {
         return forward.greaterThanCount( attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean isCountExact()
     {
         return false;
     }
 
 
     /**
      * {@inheritDoc}
      */
     public int lessThanCount( K attrVal ) throws Exception
     {
         return forward.lessThanCount( attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean reverse( Long id ) throws Exception
     {
         return reverse.has( id );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean reverse( Long id, K attrVal ) throws Exception
     {
         return reverse.has( id, attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     @SuppressWarnings("unchecked")
     public IndexCursor<K, O> reverseCursor() throws Exception
     {
         return new IndexCursorAdaptor( reverse.cursor(), false );
     }
 
 
     /**
      * {@inheritDoc}
      */
     @SuppressWarnings("unchecked")
     public IndexCursor<K, O> reverseCursor( Long id ) throws Exception
     {
         return new IndexCursorAdaptor( reverse.cursor( id ), false );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean reverseGreaterOrEq( Long id ) throws Exception
     {
         return reverse.hasGreaterOrEqual( id );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean reverseGreaterOrEq( Long id, K attrVal ) throws Exception
     {
         return reverse.hasGreaterOrEqual( id, attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public boolean reverseLessOrEq( Long id ) throws Exception
     {
         return reverse.hasLessOrEqual( id );
     }
 
     
     /**
      * {@inheritDoc}
      */
     public boolean reverseLessOrEq( Long id, K attrVal ) throws Exception
     {
         return reverse.hasLessOrEqual( id, attrVal );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public K reverseLookup( Long id ) throws Exception
     {
         return reverse.get( id );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public Cursor<K> reverseValueCursor( Long id ) throws Exception
     {
         return reverse.valueCursor( id );
     }
 
 
     /**
      * {@inheritDoc}
      */
     public void setAttributeId( String attributeId )
     {
         this.attributeId = attributeId;
     }
 
     
     /**
      * throws UnsupportedOperationException cause it is a in-memory index
      */
     public void setWkDirPath( File wkDirPath )
     {
         throw new UnsupportedOperationException( "in memory index cannot store the data on disk" );
     }
 
     
     /**
      * this method always returns null for AvlIndex cause this is a in-memory index.
      */
     public File getWkDirPath()
     {
         return null;
     }
 
     
     /**
      * throws UnsupportedOperationException cause it is a in-memory index
      */
     public void setCacheSize( int cacheSize )
     {
         throw new UnsupportedOperationException( "in memory index doesn't support explicit caching" );
     }
 
     
     public int getCacheSize()
     {
         return 0;
     }
 
 
     /**
      * {@inheritDoc}
      */
     public void sync() throws Exception
     {
     }
 }
