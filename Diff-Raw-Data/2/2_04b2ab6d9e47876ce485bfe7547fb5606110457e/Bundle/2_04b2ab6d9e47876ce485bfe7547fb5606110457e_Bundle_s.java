 /*
  * Bundle.java
  *
  * Version: $Revision$
  *
  * Date: $Date$
  *
  * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
  * Institute of Technology.  All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are
  * met:
  *
  * - Redistributions of source code must retain the above copyright
  * notice, this list of conditions and the following disclaimer.
  *
  * - Redistributions in binary form must reproduce the above copyright
  * notice, this list of conditions and the following disclaimer in the
  * documentation and/or other materials provided with the distribution.
  *
  * - Neither the name of the Hewlett-Packard Company nor the name of the
  * Massachusetts Institute of Technology nor the names of their
  * contributors may be used to endorse or promote products derived from
  * this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  * DAMAGE.
  */
 
 package org.dspace.content;
 
 import java.io.InputStream;
 import java.io.IOException;
 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.ListIterator;
 
 import org.apache.log4j.Logger;
 
 import org.dspace.authorize.AuthorizeException;
 import org.dspace.authorize.AuthorizeManager;
 import org.dspace.core.Constants;
 import org.dspace.core.Context;
 import org.dspace.core.LogManager;
 import org.dspace.storage.rdbms.DatabaseManager;
 import org.dspace.storage.rdbms.TableRow;
 import org.dspace.storage.rdbms.TableRowIterator;
 
 
 /**
  * Class representing bundles of bitstreams stored in the DSpace system
  * <P>
  * The corresponding Bitstream objects are loaded into memory.  At present,
  * there is no metadata associated with bundles - they are simple containers.
  * Thus, the <code>update</code> method doesn't do much yet.  Creating, adding
  * or removing bitstreams has instant effect in the database.
  *
  * @author   Robert Tansley
  * @version  $Revision$
  */
 public class Bundle
 {
     /** log4j logger */
     private static Logger log = Logger.getLogger(Bundle.class);
 
     /** Our context */
     private Context ourContext;
 
     /** The table row corresponding to this bundle */
     private TableRow bundleRow;
     
     /** The bitstreams in this bundle */
     private List bitstreams;
 
 
     /**
      * Construct a bundle object with the given table row
      *
      * @param context  the context this object exists in
      * @param row      the corresponding row in the table
      */
     Bundle(Context context, TableRow row)
         throws SQLException
     {
         ourContext = context;
         bundleRow = row;
         bitstreams = new ArrayList();
         
         // Get bitstreams
         TableRowIterator tri = DatabaseManager.query(ourContext,
             "bitstream",
             "SELECT bitstream.* FROM bitstream, bundle2bitstream WHERE " +
                 "bundle2bitstream.bitstream_id=bitstream.bitstream_id AND " +
                 "bundle2bitstream.bundle_id=" +
                 bundleRow.getIntColumn("bundle_id") + ";");
 
         while (tri.hasNext())
         {
             TableRow r = (TableRow) tri.next();
 
             // First check the cache
             Bitstream fromCache = (Bitstream) context.fromCache(
                 Bitstream.class, r.getIntColumn("bitstream_id"));
 
             if (fromCache != null)
             {
                 bitstreams.add(fromCache);
             }
             else
             {
                 bitstreams.add(new Bitstream(ourContext, r));
             }
         }
 
         // Cache ourselves
         context.cache(this, row.getIntColumn("bundle_id"));
     }
 
     
     /**
      * Get a bundle from the database.  The bundle and bitstream metadata are
      * all loaded into memory.
      *
      * @param  context  DSpace context object
      * @param  id       ID of the bundle
      *   
      * @return  the bundle, or null if the ID is invalid.
      */
     public static Bundle find(Context context, int id)
         throws SQLException
     {
         // First check the cache
         Bundle fromCache = (Bundle) context.fromCache(Bundle.class, id);
             
         if (fromCache != null)
         {
             return fromCache;
         }
 
         TableRow row = DatabaseManager.find(context,
             "bundle",
             id);
 
         if (row == null)
         {
             if (log.isDebugEnabled())
             {
                 log.debug(LogManager.getHeader(context,
                     "find_bundle",
                     "not_found,bundle_id=" + id));
             }
 
             return null;
         }
         else
         {
             if (log.isDebugEnabled())
             {
                 log.debug(LogManager.getHeader(context,
                     "find_bundle",
                     "bundle_id=" + id));
             }
 
             return new Bundle(context, row);
         }
     }
     
 
     /**
      * Create a new bundle, with a new ID.  This method is not public, since
      * bundles need to be created within the context of an item.  For this
      * reason, authorisation is also not checked; that is the responsibility
      * of the caller.
      *
      * @param  context  DSpace context object
      *
      * @return  the newly created bundle
      */
     static Bundle create(Context context)
         throws SQLException
     {
         // Create a table row
         TableRow row = DatabaseManager.create(context, "bundle");
 
         log.info(LogManager.getHeader(context,
             "create_bundle",
             "bundle_id=" + row.getIntColumn("bundle_id")));
 
         return new Bundle(context, row);
     }
 
 
     /**
      * Get the internal identifier of this bundle
      *
      * @return the internal identifier
      */
     public int getID()
     {
         return bundleRow.getIntColumn("bundle_id");
     }
 
 
     /**
      * Get the bitstreams in this bundle
      *
      * @return the bitstreams
      */
     public Bitstream[] getBitstreams()
     {
         Bitstream[] bitstreamArray = new Bitstream[bitstreams.size()];
         bitstreamArray = (Bitstream[]) bitstreams.toArray(bitstreamArray);
         
         return bitstreamArray;
     }
     
 
     /**
      * Get the items this bundle appears in
      *
      * @return array of <code>Item</code>s this bundle appears
      *         in
      */
     public Item[] getItems()
         throws SQLException
     {
         List items = new ArrayList();
 
         // Get items
         TableRowIterator tri = DatabaseManager.query(ourContext,
             "item",
             "SELECT item.* FROM item, item2bundle WHERE " +
                 "item2bundle.item_id=item.item_id AND " +
                 "item2bundle.bundle_id=" +
                 bundleRow.getIntColumn("bundle_id") + ";");
 
         while (tri.hasNext())
         {
             TableRow r = (TableRow) tri.next();
 
             // Used cached copy if there is one
             Item fromCache = (Item) ourContext.fromCache(
                 Item.class, r.getIntColumn("item_id"));
 
             if (fromCache != null)
             {
                 items.add(fromCache);
             }
             else
             {
                 items.add(new Item(ourContext, r));
             }
         }
         
         Item[] itemArray = new Item[items.size()];
         itemArray = (Item[]) items.toArray(itemArray);
         
         return itemArray;
     }
     
 
     /**
      * Create a new bitstream in this bundle.
      *
      * @param is   the stream to read the new bitstream from
      *
      * @return  the newly created bitstream
      */
     public Bitstream createBitstream(InputStream is)
         throws AuthorizeException, IOException, SQLException
     {
         // Check authorisation
         AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);
 
         Bitstream b = Bitstream.create(ourContext, is);
         addBitstream(b);
         return b;
     }
 
 
     /**
      * Add an existing bitstream to this bundle
      *
      * @param b  the bitstream to add
      */
     public void addBitstream(Bitstream b)
         throws SQLException, AuthorizeException
     {
         // Check authorisation
         AuthorizeManager.authorizeAction(ourContext, this, Constants.ADD);
 
         log.info(LogManager.getHeader(ourContext,
             "add_bitstream",
             "bundle_id=" + getID() + ",bitstream_id=" + b.getID()));
 
         // First check that the bitstream isn't already in the list
         for (int i = 0; i < bitstreams.size(); i++)
         {
             Bitstream existing = (Bitstream) bitstreams.get(i);
             if (b.getID() == existing.getID())
             {
                 // Bitstream is already there; no change
                 return;
             }
         }
         
         // Add the bitstream object
         bitstreams.add(b);
 
         // Add the mapping row to the database
         TableRow mappingRow = DatabaseManager.create(ourContext,
             "bundle2bitstream");
         mappingRow.setColumn("bundle_id", getID());
         mappingRow.setColumn("bitstream_id", b.getID());
         DatabaseManager.update(ourContext, mappingRow);
     }
     
 
     /**
      * Remove a bitstream from this bundle - the bitstream is not deleted
      *
      * @param b  the bitstream to remove
      */
     public void removeBitstream(Bitstream b)
         throws AuthorizeException, SQLException
     {
         // Check authorisation
         AuthorizeManager.authorizeAction(ourContext, this, Constants.REMOVE);
 
         log.info(LogManager.getHeader(ourContext,
             "remove_bitstream",
             "bundle_id=" + getID() + ",bitstream_id=" + b.getID()));
 
         ListIterator li = bitstreams.listIterator();
 
         while (li.hasNext())
         {
             Bitstream existing = (Bitstream) li.next();
 
             if (b.getID() == existing.getID())
             {
                 // We've found the bitstream to remove
                 li.remove();               
 
                 // Delete the mapping row
                 DatabaseManager.updateQuery(ourContext,
                     "DELETE FROM bundle2bitstream WHERE bundle_id=" + getID() +
                         " AND bitstream_id=" + b.getID());
 
             }
         }
     }
 
 
     /**
      * Update the bundle metadata
      */
     public void update()
         throws SQLException, AuthorizeException
     {
         // Check authorisation
         AuthorizeManager.authorizeAction(ourContext, this, Constants.WRITE);
 
         log.info(LogManager.getHeader(ourContext,
             "update_bundle",
             "bundle_id=" + getID()));
 
         DatabaseManager.update(ourContext, bundleRow);
     }
 
 
     /**
      * Delete the bundle.  Any association between the bundle and bitstreams
      * or items are removed.  The bitstreams contained in the bundle are
      * NOT removed.
      */
     public void delete()
         throws SQLException, AuthorizeException
     {
         // Check authorisation
         AuthorizeManager.authorizeAction(ourContext, this, Constants.DELETE);
 
         log.info(LogManager.getHeader(ourContext,
             "delete_bundle",
             "bundle_id=" + getID()));
 
         // Remove from cache
         ourContext.removeCached(this, getID());
 
         // Remove item-bundle mappings
         DatabaseManager.updateQuery(ourContext,
             "delete from item2bundle where bundle_id=" + getID());
 
         // Remove bundle-bitstream mappings
         DatabaseManager.updateQuery(ourContext,
            "delete from bundle2bitstream where bitstream_id=" + getID());
 
         // Remove ourself
         DatabaseManager.delete(ourContext, bundleRow);
     }
 
 
     /**
      * Delete the bundle, and any bitstreams it contains.  Any associations
      * with items are deleted.  However, bitstreams that are also contained
      * in other bundles are NOT deleted.
      */
     public void deleteWithContents()
         throws SQLException, AuthorizeException, IOException
     {
         // Check authorisation
         AuthorizeManager.authorizeAction(ourContext, this, Constants.DELETE);
 
         // First delete ourselves
         delete();
         
         // Now see if any of our bitstreams were in other bundles
         Iterator i = bitstreams.iterator();
 
         while (i.hasNext())
         {
             Bitstream b = (Bitstream) i.next();
             
             // Try and find any mapping rows pertaining to the bitstream
             TableRowIterator tri = DatabaseManager.query(ourContext,
                 "bundle2bitstream",
                 "select * from bundle2bitstream where bitstream_id=" +
                     b.getID());
             
             if (!tri.hasNext())
             {
                 // The bitstream is not in any other bundle, so delete it
                 b.delete();
             }
         }
     }
 }
