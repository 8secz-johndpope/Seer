 /**
  * Tungsten Scale-Out Stack
  * Copyright (C) 2011 Continuent Inc.
  * Contact: tungsten@continuent.org
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of version 2 of the GNU General Public License as
  * published by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA
  *
  * Initial developer(s): Robert Hodges
  * Contributor(s):
  */
 
 package com.continuent.tungsten.replicator.thl;
 
 import org.apache.log4j.Logger;
 
 import com.continuent.tungsten.replicator.ReplicatorException;
 import com.continuent.tungsten.replicator.event.ReplControlEvent;
 import com.continuent.tungsten.replicator.event.ReplDBMSEvent;
 import com.continuent.tungsten.replicator.event.ReplDBMSHeader;
 import com.continuent.tungsten.replicator.event.ReplEvent;
 import com.continuent.tungsten.replicator.extractor.Extractor;
 import com.continuent.tungsten.replicator.extractor.ExtractorException;
 import com.continuent.tungsten.replicator.plugin.PluginContext;
 import com.continuent.tungsten.replicator.thl.log.LogConnection;
 
 /**
  * Implements Extractor interface for a transaction history log (THL).
  * 
  * @author <a href="mailto:robert.hodges@continuent.com">Robert Hodges</a>
  * @version 1.0
  */
 public class THLStoreExtractor implements Extractor
 {
     private static Logger logger     = Logger.getLogger(THLStoreExtractor.class);
     private String        storeName;
     private THL           thl;
     private LogConnection client;
 
     // Pointers to track storage.
     private boolean       positioned = false;
     private long          seqno;
     private short         fragno;
 
     /**
      * Instantiate the adapter.
      */
     public THLStoreExtractor()
     {
     }
 
     public String getStoreName()
     {
         return storeName;
     }
 
     public void setStoreName(String storeName)
     {
         this.storeName = storeName;
     }
 
     /**
      * {@inheritDoc}
      * 
      * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#configure(com.continuent.tungsten.replicator.plugin.PluginContext)
      */
     public void configure(PluginContext context) throws ReplicatorException
     {
         // Do nothing.
     }
 
     /**
      * Connect to store. {@inheritDoc}
      * 
      * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#prepare(com.continuent.tungsten.replicator.plugin.PluginContext)
      */
     public void prepare(PluginContext context) throws ReplicatorException
     {
         try
         {
             thl = (THL) context.getStore(storeName);
             client = thl.connect(true);
         }
         catch (ClassCastException e)
         {
             throw new ReplicatorException(
                     "Invalid storage class; configuration may be in error: "
                             + context.getStore(storeName).getClass().getName());
         }
         if (thl == null)
             throw new ReplicatorException(
                     "Unknown storage name; configuration may be in error: "
                             + storeName);
         logger.info("Storage adapter is prepared: name=" + storeName);
     }
 
     /**
      * {@inheritDoc}
      * 
      * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#release(com.continuent.tungsten.replicator.plugin.PluginContext)
      */
     public void release(PluginContext context) throws ReplicatorException
     {
         if (thl != null)
         {
             thl.disconnect(client);
             thl = null;
         }
     }
 
     /**
      * {@inheritDoc}
      * 
      * @see com.continuent.tungsten.replicator.extractor.Extractor#extract()
      */
    public ReplEvent extract() throws ExtractorException,
             InterruptedException
     {
         // If we did not position for the first time yet, do so now.
         if (!positioned)
         {
             try
             {
                 if (! client.seek(seqno, fragno))
                 {
                     throw new ExtractorException(
                             "Unable to find event; may not exist in log: seqno="
                                     + seqno + " fragno=" + fragno);
                 }
 
             }
             catch (THLException e)
             {
                 throw new ExtractorException(
                         "Unable to position on initial event: seqno=" + seqno
                                 + " fragno=" + fragno, e);
             }
             positioned = true;
         }
 
         // Fetch next event and update sequence numbers.
         try
         {
             THLEvent thlEvent = client.next();
 
             if (thlEvent == null)
             {
                 throw new THLException("Event missing from storage");
             }
             ReplEvent replEvent = thlEvent.getReplEvent();
             if (replEvent instanceof ReplDBMSEvent)
             {
                 ReplDBMSEvent replDbmsEvent = (ReplDBMSEvent) replEvent;
                 seqno = replDbmsEvent.getSeqno();
                 fragno = replDbmsEvent.getFragno();
                 return replDbmsEvent;
             }
             else if (replEvent instanceof ReplControlEvent)
             {
                 ReplDBMSHeader replDbmsHeader = ((ReplControlEvent) replEvent)
                         .getHeader();
                 seqno = replDbmsHeader.getSeqno();
                 fragno = replDbmsHeader.getFragno();
                 return replEvent;
             }
             else
             {
                 logger.warn("No repl event found for seqno="
                         + thlEvent.getSeqno());
                 return null;
             }
         }
         catch (ReplicatorException e)
         {
             throw new ExtractorException("Unable to fetch after event: seqno="
                     + seqno + " fragno=" + fragno, e);
         }
     }
 
     /**
      * Return the event ID for a flush; does not make sense for a store.
      * {@inheritDoc}
      * 
      * @see com.continuent.tungsten.replicator.extractor.Extractor#getCurrentResourceEventId()
      */
    public String getCurrentResourceEventId() throws ExtractorException,
             InterruptedException
     {
         return null;
     }
 
     /**
      * Returns true if the queue has more events. {@inheritDoc}
      * 
      * @see com.continuent.tungsten.replicator.extractor.Extractor#hasMoreEvents()
      */
     public boolean hasMoreEvents()
     {
         // TODO: Clean up; latter predicate is approximate/off by one?
         return (fragno > 0 || thl.pollSeqno(seqno + 1));
     }
 
     /**
      * Stores the last event we have processed. {@inheritDoc}
      * 
      * @see com.continuent.tungsten.replicator.extractor.Extractor#setLastEvent(com.continuent.tungsten.replicator.event.ReplDBMSHeader)
      */
    public void setLastEvent(ReplDBMSHeader event) throws ExtractorException
     {
         // Remember where we were.
         if (event == null)
         {
             // Start at beginning for next event.
             seqno = 0;
             fragno = 0;
         }
         else
         {
             if (event.getLastFrag())
             {
                 // Start at next full event following this one.
                 seqno = event.getSeqno() + 1;
                 fragno = 0;
             }
             else
             {
                 // Start at next fragment in current event.
                 seqno = event.getSeqno();
                 fragno = (short) (event.getFragno() + 1);
             }
         }
     }
 
     /**
      * Ignored for now as stores do not extract. {@inheritDoc}
      * 
      * @see com.continuent.tungsten.replicator.extractor.Extractor#setLastEventId(java.lang.String)
      */
    public void setLastEventId(String eventId) throws ExtractorException
     {
         logger.warn("Attempt to set last event ID on THL storage: " + eventId);
     }
 }
