 /*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *  http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
 package org.apache.directory.server.core.partition.impl.btree.jdbm;
 
 
 import org.apache.directory.server.core.cursor.AbstractCursor;
 import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
 import org.apache.directory.server.xdbm.Tuple;
 
 import java.util.Comparator;
 
 import jdbm.helper.TupleBrowser;
 import jdbm.btree.BTree;
 
 
 /**
  * Cursor over a set of values for the same key which are store in another
  * BTree.  This Cursor is limited to the same key and it's tuples will always
  * return the same key.
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$, $Date$
  */
 public class KeyTupleBTreeCursor<K,V> extends AbstractCursor<Tuple<K,V>>
 {
    private final Comparator<V> comparator;
     private final BTree btree;
     private final K key;
 
     private jdbm.helper.Tuple valueTuple = new jdbm.helper.Tuple();
    private Tuple<K,V> returnedTuple = new Tuple<K,V>();
     private TupleBrowser browser;
     private boolean valueAvailable;
 
 
     /**
      * Creates a Cursor over the tuples of a JDBM BTree.
      *
      * @param btree the JDBM BTree to build a Cursor over
      * @param key the constant key for which values are returned
      * @param comparator the Comparator used to determine <b>key</b> ordering
      * @throws Exception of there are problems accessing the BTree
      */
    public KeyTupleBTreeCursor( BTree btree, K key, Comparator<V> comparator ) throws Exception
     {
         this.key = key;
         this.btree = btree;
         this.comparator = comparator;
     }
 
 
     private void clearValue()
     {
         returnedTuple.setKey( key );
         returnedTuple.setValue( null );
         valueAvailable = false;
     }
 
 
     public boolean available()
     {
         return valueAvailable;
     }
 
 
     /**
      * Positions this Cursor over the same keys before the value of the
      * supplied valueTuple.  The supplied element Tuple's key is not considered at
      * all.
      *
      * @param element the valueTuple who's value is used to position this Cursor
      * @throws Exception if there are failures to position the Cursor
      */
     public void before( Tuple<K,V> element ) throws Exception
     {
         browser = btree.browse( element.getValue() );
         clearValue();
     }
 
 
     public void after( Tuple<K,V> element ) throws Exception
     {
         browser = btree.browse( element.getValue() );
 
         /*
          * While the next value is less than or equal to the element keep
          * advancing forward to the next item.  If we cannot advance any
          * further then stop and return.  If we find a value greater than
          * the element then we stop, backup, and return so subsequent calls
          * to getNext() will return a value greater than the element.
          */
         while ( browser.getNext( valueTuple ) )
         {
             //noinspection unchecked
            V next = ( V ) valueTuple.getKey();
 
             //noinspection unchecked
            int nextCompared = comparator.compare( next, element.getValue() );
 
             if ( nextCompared <= 0 )
             {
                 // just continue
             }
             else if ( nextCompared > 0 )
             {
                 /*
                  * If we just have values greater than the element argument
                  * then we are before the first element and cannot backup, and
                  * the call below to getPrevious() will fail.  In this special
                  * case we just reset the Cursor's browser and return.
                  */
                 if ( browser.getPrevious( valueTuple ) )
                 {
                 }
                 else
                 {
                     browser = btree.browse( element.getKey() );
                 }
 
                 clearValue();
                 return;
             }
         }
 
         clearValue();
         // just return
     }
 
 
     public void beforeFirst() throws Exception
     {
         browser = btree.browse();
         clearValue();
     }
 
 
     public void afterLast() throws Exception
     {
         browser = btree.browse( null );
     }
 
 
     public boolean first() throws Exception
     {
         beforeFirst();
         return next();
     }
 
 
     public boolean last() throws Exception
     {
         afterLast();
         return previous();
     }
 
 
     public boolean previous() throws Exception
     {
         if ( browser.getPrevious( valueTuple ) )
         {
             returnedTuple.setKey( key );
            returnedTuple.setValue( ( V ) valueTuple.getKey() );
             return valueAvailable = true;
         }
         else
         {
             clearValue();
             return false;
         }
     }
 
 
     public boolean next() throws Exception
     {
         if ( browser.getNext( valueTuple ) )
         {
             returnedTuple.setKey( key );
            returnedTuple.setValue( ( V ) valueTuple.getKey() );
             return valueAvailable = true;
         }
         else
         {
             clearValue();
             return false;
         }
     }
 
 
     public Tuple<K,V> get() throws Exception
     {
         if ( valueAvailable )
         {
             //noinspection unchecked
             return returnedTuple;
         }
 
         throw new InvalidCursorPositionException();
     }
 
 
     public boolean isElementReused()
     {
         return true;
     }
 }
