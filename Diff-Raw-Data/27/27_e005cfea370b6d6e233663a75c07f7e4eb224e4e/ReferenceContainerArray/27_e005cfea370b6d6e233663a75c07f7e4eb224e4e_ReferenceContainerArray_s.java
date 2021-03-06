 // ReferenceContainerArray.java
 // (C) 2009 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 // first published 04.01.2009 on http://yacy.net
 //
 // $LastChangedDate$
 // $LastChangedRevision$
 // $LastChangedBy$
 //
 // LICENSE
 // 
 // This program is free software; you can redistribute it and/or modify
 // it under the terms of the GNU General Public License as published by
 // the Free Software Foundation; either version 2 of the License, or
 // (at your option) any later version.
 //
 // This program is distributed in the hope that it will be useful,
 // but WITHOUT ANY WARRANTY; without even the implied warranty of
 // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 // GNU General Public License for more details.
 //
 // You should have received a copy of the GNU General Public License
 // along with this program; if not, write to the Free Software
 // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
 package de.anomic.kelondro.text;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Date;
 import java.util.Iterator;
 import java.util.List;
 
 import de.anomic.kelondro.blob.BLOB;
 import de.anomic.kelondro.blob.BLOBArray;
 import de.anomic.kelondro.index.IntegerHandleIndex;
 import de.anomic.kelondro.index.Row;
 import de.anomic.kelondro.index.RowSet;
 import de.anomic.kelondro.order.ByteOrder;
 import de.anomic.kelondro.order.CloneableIterator;
 import de.anomic.kelondro.util.Log;
 
 public final class ReferenceContainerArray<ReferenceType extends Reference> {
 
     private final ReferenceFactory<ReferenceType> factory;
     private final Row payloadrow;
     private final BLOBArray array;
     private final IODispatcher<ReferenceType> merger;
     
     /**
      * open a index container based on a BLOB dump. The content of the BLOB will not be read
      * unless a .idx file exists. Only the .idx file is opened to get a fast read access to
      * the BLOB. This class provides no write methods, because BLOB files should not be
      * written in random access. To support deletion, a write access to the BLOB for deletion
      * is still possible
      * @param payloadrow
      * @param log
      * @throws IOException 
      */
     public ReferenceContainerArray(
     		final File heapLocation,
     		final ReferenceFactory<ReferenceType> factory,
     		final ByteOrder termOrder,
     		final Row payloadrow,
     		IODispatcher<ReferenceType> merger) throws IOException {
         this.factory = factory;
         this.payloadrow = payloadrow;
         this.array = new BLOBArray(
             heapLocation,
             "index",
             payloadrow.primaryKeyLength,
             termOrder,
             0);
         assert merger != null;
         this.merger = merger;
     }
     
     public synchronized void close() {
     	this.array.close(true);
     }
     
     public synchronized void clear() throws IOException {
     	this.array.clear();
     }
     
     public int size() {
         return (this.array == null) ? 0 : this.array.size();
     }
     
     public ByteOrder ordering() {
         return this.array.ordering();
     }
     
     public File newContainerBLOBFile() {
     	return this.array.newBLOB(new Date());
     }
     
     public void mountBLOBFile(File location) throws IOException {
         this.array.mountBLOB(location, false);
     }
     
     public Row rowdef() {
         return this.payloadrow;
     }
     
     /**
      * return an iterator object that creates top-level-clones of the indexContainers
      * in the cache, so that manipulations of the iterated objects do not change
      * objects in the cache.
      * @throws IOException 
      */
     public synchronized CloneableIterator<ReferenceContainer<ReferenceType>> wordContainerIterator(final byte[] startWordHash, final boolean rot, final boolean ram) {
         try {
             return new heapCacheIterator(startWordHash, rot);
         } catch (IOException e) {
             e.printStackTrace();
             return null;
         }
     }
 
     /**
      * cache iterator: iterates objects within the heap cache. This can only be used
      * for write-enabled heaps, read-only heaps do not have a heap cache
      */
     public class heapCacheIterator implements CloneableIterator<ReferenceContainer<ReferenceType>>, Iterable<ReferenceContainer<ReferenceType>> {
 
         // this class exists, because the wCache cannot be iterated with rotation
         // and because every indexContainer Object that is iterated must be returned as top-level-clone
         // so this class simulates wCache.tailMap(startWordHash).values().iterator()
         // plus the mentioned features
         
         private final boolean rot;
         private CloneableIterator<byte[]> iterator;
         
         public heapCacheIterator(final byte[] startWordHash, final boolean rot) throws IOException {
             this.rot = rot;
             this.iterator = array.keys(true, startWordHash);
             // The collection's iterator will return the values in the order that their corresponding keys appear in the tree.
         }
         
         public heapCacheIterator clone(final Object secondWordHash) {
             try {
 				return new heapCacheIterator((byte[]) secondWordHash, rot);
 			} catch (IOException e) {
 				e.printStackTrace();
 				return null;
 			}
         }
         
         public boolean hasNext() {
             if (this.iterator == null) return false;
             if (rot) return true;
             return iterator.hasNext();
         }
 
         public ReferenceContainer<ReferenceType> next() {
         	try {
 				if (iterator.hasNext()) {
                 	return get(iterator.next());
 				}
 	            // rotation iteration
 	            if (!rot) {
 	                return null;
 	            }
 	            iterator = array.keys(true, null);
 	            return get(iterator.next());
             } catch (IOException e) {
 				e.printStackTrace();
 				return null;
 			}
         }
 
         public void remove() {
             iterator.remove();
         }
 
         public Iterator<ReferenceContainer<ReferenceType>> iterator() {
             return this;
         }
         
     }
 
     /**
      * test if a given key is in the heap
      * this works with heaps in write- and read-mode
      * @param key
      * @return true, if the key is used in the heap; false othervise
      * @throws IOException 
      */
     public synchronized boolean has(final byte[] termHash) {
         return this.array.has(termHash);
     }
     
     /**
      * get a indexContainer from a heap
      * @param key
      * @return the indexContainer if one exist, null otherwise
      * @throws IOException 
      */
     public synchronized ReferenceContainer<ReferenceType> get(final byte[] termHash) throws IOException {
     	List<byte[]> entries = this.array.getAll(termHash);
     	if (entries == null || entries.size() == 0) return null;
     	byte[] a = entries.remove(0);
     	ReferenceContainer<ReferenceType> c = new ReferenceContainer<ReferenceType>(this.factory, termHash, RowSet.importRowSet(a, payloadrow));
     	while (entries.size() > 0) {
     		c = c.merge(new ReferenceContainer<ReferenceType>(this.factory, termHash, RowSet.importRowSet(entries.remove(0), payloadrow)));
     	}
     	return c;
     }
     
     /**
      * delete a indexContainer from the heap cache. This can only be used for write-enabled heaps
      * @param wordHash
      * @return the indexContainer if the cache contained the container, null othervise
      * @throws IOException 
      */
     public synchronized void delete(final byte[] termHash) throws IOException {
         // returns the index that had been deleted
     	array.remove(termHash);
     }
     
     public synchronized int replace(final byte[] termHash, ContainerRewriter<ReferenceType> rewriter) throws IOException {
         return array.replace(termHash, new BLOBRewriter(termHash, rewriter));
     }
     
     public class BLOBRewriter implements BLOB.Rewriter {
 
         ContainerRewriter<ReferenceType> rewriter;
         byte[] wordHash;
         
         public BLOBRewriter(byte[] wordHash, ContainerRewriter<ReferenceType> rewriter) {
             this.rewriter = rewriter;
             this.wordHash = wordHash;
         }
         
         public byte[] rewrite(byte[] b) {
             if (b == null) return null;
             ReferenceContainer<ReferenceType> c = rewriter.rewrite(new ReferenceContainer<ReferenceType>(factory, this.wordHash, RowSet.importRowSet(b, payloadrow)));
             if (c == null) return null;
             return c.exportCollection();
         }
     }
 
     public interface ContainerRewriter<ReferenceType extends Reference> {
         
         public ReferenceContainer<ReferenceType> rewrite(ReferenceContainer<ReferenceType> container);
         
     }
     
     public int entries() {
         return this.array.entries();
     }
     
     public synchronized boolean shrink(long targetFileSize, long maxFileSize) {
         if (this.array.entries() < 2) return false;
         boolean donesomething = false;
         
         // first try to merge small files that match
         while (this.merger.queueLength() < 3 || this.array.entries() >= 50) {
             File[] ff = this.array.unmountBestMatch(2.0, targetFileSize);
             if (ff == null) break;
             Log.logInfo("RICELL-shrink1", "unmountBestMatch(2.0, " + targetFileSize + ")");
             merger.merge(ff[0], ff[1], this.array, this.payloadrow, newContainerBLOBFile());
             donesomething = true;
         }
         
         // then try to merge simply any small file
         while (this.merger.queueLength() < 2) {
             File[] ff = this.array.unmountSmallest(targetFileSize);
             if (ff == null) break;
             Log.logInfo("RICELL-shrink2", "unmountSmallest(" + targetFileSize + ")");
             merger.merge(ff[0], ff[1], this.array, this.payloadrow, newContainerBLOBFile());
             donesomething = true;
         }
         
         // if there is no small file, then merge matching files up to limit
         while (this.merger.queueLength() < 1) {
             File[] ff = this.array.unmountBestMatch(2.0, maxFileSize);
             if (ff == null) break;
             Log.logInfo("RICELL-shrink3", "unmountBestMatch(2.0, " + maxFileSize + ")");
             merger.merge(ff[0], ff[1], this.array, this.payloadrow, newContainerBLOBFile());
             donesomething = true;
         }
 
         return donesomething;
     }
     
     public static <ReferenceType extends Reference> IntegerHandleIndex referenceHashes(
                             final File heapLocation,
                             final ReferenceFactory<ReferenceType> factory,
                             final ByteOrder termOrder,
                             final Row payloadrow) throws IOException {
        
         System.out.println("CELL REFERENCE COLLECTION startup");
         IntegerHandleIndex references = new IntegerHandleIndex(payloadrow.primaryKeyLength, termOrder, 0, 1000000);
         String[] files = heapLocation.list();
         for (String f: files) {
            if (f.length() < 22 && !f.startsWith("index") && !f.endsWith(".blob")) continue;
             File fl = new File(heapLocation, f);
             System.out.println("CELL REFERENCE COLLECTION opening blob " + fl);
             CloneableIterator<ReferenceContainer<ReferenceType>>  ei = new ReferenceContainerCache.blobFileEntries<ReferenceType>(fl, factory, payloadrow);
         
             ReferenceContainer<ReferenceType> container;
             final long start = System.currentTimeMillis();
             long lastlog = start - 27000;
             int count = 0;
             while (ei.hasNext()) {
                 container = ei.next();
                 if (container == null) continue;
                 Iterator<ReferenceType> refi = container.entries();
                 while (refi.hasNext()) {
                     references.inc(refi.next().metadataHash().getBytes(), 1);
                 }
                 count++;
                 // write a log
                 if (System.currentTimeMillis() - lastlog > 30000) {
                     System.out.println("CELL REFERENCE COLLECTION scanned " + count + " RWI index entries. ");
                     //Log.logInfo("COLLECTION INDEX REFERENCE COLLECTION", "scanned " + count + " RWI index entries. " + (((System.currentTimeMillis() - start) * (array.size() + array.free() - count) / count) / 60000) + " minutes remaining for this array");
                     lastlog = System.currentTimeMillis();
                 }
             }
         }
         System.out.println("CELL REFERENCE COLLECTION finished");
         return references;
     }
 
     
 }
