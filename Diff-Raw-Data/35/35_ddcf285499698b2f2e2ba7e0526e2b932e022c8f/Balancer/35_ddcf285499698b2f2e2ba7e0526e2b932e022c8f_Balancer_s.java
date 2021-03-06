 // plasmaCrawlBalancer.java 
 // -----------------------
 // part of YaCy
 // (C) by Michael Peter Christen; mc@yacy.net
 // first published on http://www.anomic.de
 // Frankfurt, Germany, 2005
 // created: 24.09.2005
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
 
 package de.anomic.crawler;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.concurrent.ConcurrentHashMap;
 
 import de.anomic.kelondro.kelondroBase64Order;
 import de.anomic.kelondro.kelondroEcoTable;
 import de.anomic.kelondro.kelondroIndex;
 import de.anomic.kelondro.kelondroRow;
 import de.anomic.kelondro.kelondroStack;
 import de.anomic.plasma.plasmaSwitchboard;
 import de.anomic.server.logging.serverLog;
 import de.anomic.yacy.yacySeedDB;
 import de.anomic.yacy.yacyURL;
 
 public class Balancer {
     
     private static final String stackSuffix = "9.stack";
     private static final String indexSuffix = "9.db";
     private static final int EcoFSBufferSize = 200;
 
     // a shared domainAccess map for all balancers. the key is a domain-hash (6 bytes)
     public static final ConcurrentHashMap<String, domaccess> domainAccess = new ConcurrentHashMap<String, domaccess>();
     
     // definition of payload for fileStack
     private static final kelondroRow stackrow = new kelondroRow("byte[] urlhash-" + yacySeedDB.commonHashLength, kelondroBase64Order.enhancedCoder, 0);
     
     // class variables
     private final ConcurrentHashMap<String, LinkedList<String>>
                                      domainStacks;    // a map from domain name part to Lists with url hashs
     private final ArrayList<String>  urlRAMStack;     // a list that is flushed first
     private kelondroStack            urlFileStack;    // a file with url hashes
     private kelondroIndex            urlFileIndex;
     private final File               cacheStacksPath;
     private final String             stackname;
     private boolean                  top;             // to alternate between top and bottom of the file stack
     private final boolean            fullram;
 
     public static class domaccess {
     	public long time;
     	public int count;
     	public String host;
     	public domaccess(String host) {
     	    this.host = host;
     		this.time = System.currentTimeMillis();
     		this.count = 0;
     	}
     	public void update() {
     		this.time = System.currentTimeMillis();
     		this.count++;
     	}
     	public long flux(long range) {
     	    return count >= 1000 ? range * Math.min(5000, count) / 1000 : range / (1000 - count);
     	}
     }
     
     public Balancer(final File cachePath, final String stackname, final boolean fullram) {
         this.cacheStacksPath = cachePath;
         this.stackname = stackname;
         final File stackFile = new File(cachePath, stackname + stackSuffix);
         this.urlFileStack   = kelondroStack.open(stackFile, stackrow);
         this.domainStacks   = new ConcurrentHashMap<String, LinkedList<String>>();
         this.urlRAMStack    = new ArrayList<String>();
         this.top            = true;
         this.fullram        = fullram;
         
         // create a stack for newly entered entries
         if (!(cachePath.exists())) cachePath.mkdir(); // make the path
         openFileIndex();
         if (urlFileStack.size() != urlFileIndex.size() || (urlFileIndex.size() < 10000 && urlFileIndex.size() > 0)) {
             // fix the file stack
             serverLog.logInfo("Balancer", "re-creating the " + stackname + " balancer stack, size = " + urlFileIndex.size() + ((urlFileStack.size() == urlFileIndex.size()) ? "" : " (the old stack size was wrong)" ));
             urlFileStack = kelondroStack.reset(urlFileStack);
             try {
                 final Iterator<byte[]> i = urlFileIndex.keys(true, null);
                 byte[] hash;
                 while (i.hasNext()) {
                     hash = i.next();
                     pushHash(new String(hash));
                 }
             } catch (final IOException e) {
                 e.printStackTrace();
             }
         }
     }
 
     public synchronized void close() {
         while (domainStacksNotEmpty()) flushOnceDomStacks(0, true); // flush to ram, because the ram flush is optimized
         size();
         try { flushAllRamStack(); } catch (final IOException e) {}
         if (urlFileIndex != null) {
             urlFileIndex.close();
             urlFileIndex = null;
         }
         if (urlFileStack != null) {
             urlFileStack.close();
             urlFileStack = null;
         }
     }
     
     protected void finalize() {
         if (urlFileStack != null) {
             serverLog.logWarning("Balancer", "crawl stack " + stackname + " closed by finalizer");
             close();
         }
     }
     
     public synchronized void clear() {
         urlFileStack = kelondroStack.reset(urlFileStack);
         domainStacks.clear();
         urlRAMStack.clear();
         resetFileIndex();
     }
     
     private void openFileIndex() {
         cacheStacksPath.mkdirs();
         urlFileIndex = new kelondroEcoTable(new File(cacheStacksPath, stackname + indexSuffix), CrawlEntry.rowdef, (fullram) ? kelondroEcoTable.tailCacheUsageAuto : kelondroEcoTable.tailCacheDenyUsage, EcoFSBufferSize, 0);
     }
     
     private void resetFileIndex() {
         if (urlFileIndex != null) {
             urlFileIndex.close();
             urlFileIndex = null;
             new File(cacheStacksPath, stackname + indexSuffix).delete();
         }
         openFileIndex();
     }
     
     public synchronized CrawlEntry get(final String urlhash) throws IOException {
         assert urlhash != null;
         if (urlFileIndex == null) return null; // case occurs during shutdown
         final kelondroRow.Entry entry = urlFileIndex.get(urlhash.getBytes());
         if (entry == null) return null;
         return new CrawlEntry(entry);
     }
     
     public synchronized int removeAllByProfileHandle(final String profileHandle, final long timeout) throws IOException {
         // removes all entries with a specific profile hash.
         // this may last some time
         // returns number of deletions
         
         // first find a list of url hashes that shall be deleted
         final Iterator<kelondroRow.Entry> i = urlFileIndex.rows(true, null);
         final HashSet<String> urlHashes = new HashSet<String>();
         kelondroRow.Entry rowEntry;
         CrawlEntry crawlEntry;
         final long terminate = (timeout > 0) ? System.currentTimeMillis() + timeout : Long.MAX_VALUE;
         while (i.hasNext() && (System.currentTimeMillis() < terminate)) {
             rowEntry = i.next();
             crawlEntry = new CrawlEntry(rowEntry);
             if (crawlEntry.profileHandle().equals(profileHandle)) {
                 urlHashes.add(crawlEntry.url().hash());
             }
         }
         
         // then delete all these urls from the queues and the file index
         return this.remove(urlHashes);
     }
     
     /**
      * this method is only here, because so many import/export methods need it
        and it was implemented in the previous architecture
        however, usage is not recommended
      * @param urlHashes, a list of hashes that shall be removed
      * @return number of entries that had been removed
      * @throws IOException
      */
     public synchronized int remove(final HashSet<String> urlHashes) throws IOException {
         final int s = urlFileIndex.size();
         int removedCounter = 0;
         for (final String urlhash: urlHashes) {
             final kelondroRow.Entry entry = urlFileIndex.remove(urlhash.getBytes());
             if (entry != null) removedCounter++;
         }
         if (removedCounter == 0) return 0;
         assert urlFileIndex.size() + removedCounter == s : "urlFileIndex.size() = " + urlFileIndex.size() + ", s = " + s;
        
         // now delete these hashes also from the queues
 
         // iterate through the RAM stack
         Iterator<String> i = urlRAMStack.iterator();
         String h;
         while (i.hasNext()) {
            h = i.next();
            if (urlHashes.contains(h)) i.remove();
        }
        
        // iterate through the file stack
        // in general this is a bad idea. But this can only be avoided by avoidance of this method
        final Iterator<kelondroRow.Entry> j = urlFileStack.stackIterator(true);
        while (j.hasNext()) {
            h = new String(j.next().getColBytes(0));
            if (urlHashes.contains(h)) j.remove();
        }
        
        // iterate through the domain stacks
        final Iterator<Map.Entry<String, LinkedList<String>>> k = domainStacks.entrySet().iterator();
        Map.Entry<String, LinkedList<String>> se;
        LinkedList<String> stack;
        while (k.hasNext()) {
            se = k.next();
            stack = se.getValue();
            i = stack.iterator();
            while (i.hasNext()) {
                if (urlHashes.contains(i.next())) i.remove();
            }
            if (stack.size() == 0) k.remove();
        }
        
        return removedCounter;
     }
     
    public synchronized boolean has(final String urlhash) {
         return urlFileIndex.has(urlhash.getBytes());
     }
     
     public boolean notEmpty() {
         // alternative method to the property size() > 0
         // this is better because it may avoid synchronized access to domain stack summarization
         return urlRAMStack.size() > 0 || urlFileStack.size() > 0 || domainStacksNotEmpty();
     }
     
     public int size() {
         final int componentsize = urlFileIndex.size();
         /*
         assert componentsize == urlFileStack.size() + urlRAMStack.size() + sizeDomainStacks() :
 		        "size wrong in " + stackname +
 		        " - urlFileIndex = " + urlFileIndex.size() +
 		        ", componentsize = " + urlFileStack.size() + urlRAMStack.size() + sizeDomainStacks() +
                 " = (urlFileStack = " + urlFileStack.size() +
 		        ", urlRAMStack = " + urlRAMStack.size() +
 		        ", sizeDomainStacks = " + sizeDomainStacks() + ")";
 		*/
         return componentsize;
     }
     
     private boolean domainStacksNotEmpty() {
         if (domainStacks == null) return false;
         synchronized (domainStacks) {
             final Iterator<LinkedList<String>> i = domainStacks.values().iterator();
             while (i.hasNext()) {
                 if (i.next().size() > 0) return true;
             }
         }
         return false;
     }
     
     private int sizeDomainStacks() {
         if (domainStacks == null) return 0;
         int sum = 0;
         synchronized (domainStacks) {
             final Iterator<LinkedList<String>> i = domainStacks.values().iterator();
             while (i.hasNext()) sum += i.next().size();
         }
         return sum;
     }
     
     private void flushOnceDomStacks(final int minimumleft, final boolean ram) {
         // takes one entry from every domain stack and puts it on the ram or file stack
         // the minimumleft value is a limit for the number of entries that should be left
         if (domainStacks.size() == 0) return;
         synchronized (domainStacks) {
             final Iterator<Map.Entry<String, LinkedList<String>>> i = domainStacks.entrySet().iterator();
             Map.Entry<String, LinkedList<String>> entry;
             LinkedList<String> list;
             while (i.hasNext()) {
                 entry = i.next();
                 list = entry.getValue();
                 if (list.size() > minimumleft) {
                     if (ram) {
                         urlRAMStack.add(list.removeFirst());
                     } else try {
                         urlFileStack.push(urlFileStack.row().newEntry(new byte[][] { (list.removeFirst()).getBytes() }));
                     } catch (final IOException e) {
                         e.printStackTrace();
                     }
                 }
                 if (list.size() == 0)  i.remove();
             }
         }
     }
     
     private void flushAllRamStack() throws IOException {
         // this flushes only the ramStack to the fileStack, but does not flush the domainStacks
         for (int i = 0; i < urlRAMStack.size() / 2; i++) {
             urlFileStack.push(urlFileStack.row().newEntry(new byte[][]{(urlRAMStack.get(i)).getBytes()}));
             urlFileStack.push(urlFileStack.row().newEntry(new byte[][]{(urlRAMStack.get(urlRAMStack.size() - i - 1)).getBytes()}));
         }
         if (urlRAMStack.size() % 2 != 0) 
             urlFileStack.push(urlFileStack.row().newEntry(new byte[][]{(urlRAMStack.get(urlRAMStack.size() / 2)).getBytes()}));
     }
     
     public synchronized void push(final CrawlEntry entry) throws IOException {
         assert entry != null;
         if (urlFileIndex.has(entry.url().hash().getBytes())) {
             serverLog.logWarning("PLASMA BALANCER", "double-check has failed for urlhash " + entry.url().hash()  + " in " + stackname + " - fixed");
             return;
         }
         
         // add to index
         urlFileIndex.put(entry.toRow());
         
         // add the hash to a queue
         pushHash(entry.url().hash());
     }
     
     private void pushHash(final String hash) {
         // extend domain stack
         final String dom = hash.substring(6);
         LinkedList<String> domainList = domainStacks.get(dom);
         if (domainList == null) {
             // create new list
             domainList = new LinkedList<String>();
             synchronized (domainStacks) {
                 domainList.add(hash);
                 domainStacks.put(dom, domainList);
             }
         } else {
             // extend existent domain list
             domainList.addLast(hash);
         }
         
         // check size of domainStacks and flush
         if ((domainStacks.size() > 100) || (sizeDomainStacks() > 1000)) {
             flushOnceDomStacks(1, urlRAMStack.size() < 100); // when the ram stack is small, flush it there
         }
     }
     
     public synchronized CrawlEntry pop(final long minimumLocalDelta, final long minimumGlobalDelta, final long maximumAge) throws IOException {
         // returns an url-hash from the stack and ensures minimum delta times
         // we have 3 sources to choose from: the ramStack, the domainStacks and the fileStack
         
         String result = null; // the result
         
         // 1st: check ramStack
         if (urlRAMStack.size() > 0) {
             result = urlRAMStack.remove(0);
         }
         
         // 2nd-a: check domainStacks for latest arrivals
         if ((result == null) && (domainStacks.size() > 0)) synchronized (domainStacks) {
             // we select specific domains that have not been used for a long time
             // i.e. 60 seconds. Latest arrivals that have not yet been crawled
             // fit also in that scheme
             final Iterator<Map.Entry<String, LinkedList<String>>> i = domainStacks.entrySet().iterator();
             Map.Entry<String, LinkedList<String>> entry;
             String domhash;
             long delta, maxdelta = 0;
             String maxhash = null;
             LinkedList<String> domlist;
             while (i.hasNext()) {
                 entry = i.next();
                 domhash = entry.getKey();
                 delta = lastAccessDelta(domhash);
                 if (delta == Integer.MAX_VALUE) {
                     // a brand new domain - we take it
                     domlist = entry.getValue();
                     result = domlist.removeFirst();
                     if (domlist.size() == 0) i.remove();
                     break;
                 }
                 if (delta > maxdelta) {
                     maxdelta = delta;
                     maxhash = domhash;
                 }
             }
             if (maxdelta > maximumAge) {
                 // success - we found an entry from a domain that has not been used for a long time
                 domlist = domainStacks.get(maxhash);
                 result = domlist.removeFirst();
                 if (domlist.size() == 0) domainStacks.remove(maxhash);
             }
         }
         
         // 2nd-b: check domainStacks for best match between stack size and retrieval time
         if ((result == null) && (domainStacks.size() > 0)) synchronized (domainStacks) {
             // we order all domains by the number of entries per domain
             // then we iterate through these domains in descending entry order
             // and that that one, that has a delta > minimumDelta
             final Iterator<Map.Entry<String, LinkedList<String>>> i = domainStacks.entrySet().iterator();
             Map.Entry<String, LinkedList<String>> entry;
             String domhash;
             LinkedList<String> domlist;
             final TreeMap<Integer, String> hitlist = new TreeMap<Integer, String>();
             int count = 0;
             // first collect information about sizes of the domain lists
             while (i.hasNext()) {
                 entry = i.next();
                 domhash = entry.getKey();
                 domlist = entry.getValue();
                 hitlist.put(Integer.valueOf(domlist.size() * 100 + count++), domhash);
             }
             
             // now iterate in descending order and fetch that one,
             // that is acceptable by the minimumDelta constraint
             long delta;
             String maxhash = null;
             while (hitlist.size() > 0) {
                 domhash = hitlist.remove(hitlist.lastKey());
                 if (maxhash == null) maxhash = domhash; // remember first entry
                 delta = lastAccessDelta(domhash);
                 if (delta > ((yacyURL.isLocal(domhash)) ? minimumLocalDelta : minimumGlobalDelta)) {
                     domlist = domainStacks.get(domhash);
                     result = domlist.removeFirst();
                     if (domlist.size() == 0) domainStacks.remove(domhash);
                     break;
                 }
             }
             
             // if we did yet not choose any entry, we simply take that one with the most entries
             if ((result == null) && (maxhash != null)) {
                 domlist = domainStacks.get(maxhash);
                 result = domlist.removeFirst();
                 if (domlist.size() == 0) domainStacks.remove(maxhash);
             }
         }
         
         // 3rd: take entry from file
         if ((result == null) && (urlFileStack.size() > 0)) {
             final kelondroRow.Entry nextentry = (top) ? urlFileStack.top() : urlFileStack.bot();
             if (nextentry == null) {
                 // emergency case: this means that something with the stack organization is wrong
                 // the file appears to be broken. We kill the file.
                 kelondroStack.reset(urlFileStack);
                 serverLog.logSevere("PLASMA BALANCER", "get() failed to fetch entry from file stack. reset stack file.");
             } else {
                 final String nexthash = new String(nextentry.getColBytes(0));
 
                 // check if the time after retrieval of last hash from same
                 // domain is not shorter than the minimumDelta
                 long delta = lastAccessDelta(nexthash);
                 if (delta > ((yacyURL.isLocal(nexthash)) ? minimumLocalDelta : minimumGlobalDelta)) {
                     // the entry is fine
                     result = new String((top) ? urlFileStack.pop().getColBytes(0) : urlFileStack.pot().getColBytes(0));
                 } else {
                     // try other entry
                     result = new String((top) ? urlFileStack.pot().getColBytes(0) : urlFileStack.pop().getColBytes(0));
                     delta = lastAccessDelta(result);
                 }
             }
             top = !top; // alternate top/bottom
         }
         
         // check case where we did not found anything
         if (result == null) {
             serverLog.logSevere("PLASMA BALANCER", "get() was not able to find a valid urlhash - total size = " + size() + ", fileStack.size() = " + urlFileStack.size() + ", ramStack.size() = " + urlRAMStack.size() + ", domainStacks.size() = " + domainStacks.size());
             return null;
         }
         
         // finally: check minimumDelta and if necessary force a sleep
         final long delta = lastAccessDelta(result);
         assert delta >= 0: "delta = " + delta;
         final int s = urlFileIndex.size();
         final kelondroRow.Entry rowEntry = urlFileIndex.remove(result.getBytes());
         assert (rowEntry == null) || (urlFileIndex.size() + 1 == s) : "urlFileIndex.size() = " + urlFileIndex.size() + ", s = " + s + ", result = " + result;
         if (rowEntry == null) {
             serverLog.logSevere("PLASMA BALANCER", "get() found a valid urlhash, but failed to fetch the corresponding url entry - total size = " + size() + ", fileStack.size() = " + urlFileStack.size() + ", ramStack.size() = " + urlRAMStack.size() + ", domainStacks.size() = " + domainStacks.size());
             return null;
         }
         assert urlFileIndex.size() + 1 == s : "urlFileIndex.size() = " + urlFileIndex.size() + ", s = " + s + ", result = " + result;
         final CrawlEntry crawlEntry = new CrawlEntry(rowEntry);
         final long genericDelta = ensureDelta(result.substring(6), crawlEntry, minimumLocalDelta, minimumGlobalDelta);
         if (delta < genericDelta) {
             // force a busy waiting here
             // in best case, this should never happen if the balancer works propertly
             // this is only to protection against the worst case, where the crawler could
             // behave in a DoS-manner
             final long sleeptime = genericDelta - delta;
             try {synchronized(this) { this.wait(sleeptime); }} catch (final InterruptedException e) {}
         }
         
         // update statistical data
         domaccess lastAccess = domainAccess.get(result.substring(6));
         if (lastAccess == null) {
             lastAccess = new domaccess(crawlEntry.url().getHost());
             domainAccess.put(result.substring(6), lastAccess);
         } else {
             lastAccess.update();
         }
         
         return crawlEntry;
     }
     
     private long ensureDelta(String hosthash, CrawlEntry crawlEntry, final long minimumLocalDelta, final long minimumGlobalDelta) {
         long deltaBase = (yacyURL.isLocal(hosthash)) ? minimumLocalDelta : minimumGlobalDelta;
         if (crawlEntry.url().isCGI()) deltaBase = deltaBase * 2; // mostly there is a database access in the background which creates a lot of unwanted IO on target site
         domaccess lastAccess = domainAccess.get(hosthash);
         return Math.min(
                     60000,
                     Math.max(
                       deltaBase + ((lastAccess == null) ? 0 : lastAccess.flux(deltaBase)),
                       plasmaSwitchboard.getSwitchboard().robots.crawlDelayMillis(crawlEntry.url()))
                   ); // prevent that that robots file can stop our indexer completely
     }
     
     private long lastAccessDelta(final String hash) {
         assert hash != null;
         final domaccess lastAccess = domainAccess.get((hash.length() > 6) ? hash.substring(6) : hash);
         if (lastAccess == null) return Long.MAX_VALUE; // never accessed
         return System.currentTimeMillis() - lastAccess.time;
     }
 
     public synchronized ArrayList<CrawlEntry> top(int count) throws IOException {
         // if we need to flush anything, then flush the domain stack first,
         // to avoid that new urls get hidden by old entries from the file stack
         if (urlRAMStack == null) return null;
         while ((domainStacksNotEmpty()) && (urlRAMStack.size() <= count)) {
             // flush only that much as we need to display
             flushOnceDomStacks(0, true); 
         }
         while ((urlFileStack != null) && (urlRAMStack.size() <= count) && (urlFileStack.size() > 0)) {
             // flush some entries from disc to ram stack
             try {
                 // one from the top:
                 kelondroRow.Entry t = urlFileStack.pop();
                 if (t == null) break;
                 urlRAMStack.add(new String(t.getColBytes(0)));
                 if (urlFileStack.size() == 0) break;
                 // one from the bottom:
                 t = urlFileStack.pot();
                 if (t == null) break;
                 urlRAMStack.add(new String(t.getColBytes(0)));
             } catch (final IOException e) {
                 break;
             }
         }
         
         count = Math.min(count, urlRAMStack.size());
         final ArrayList<CrawlEntry> list = new ArrayList<CrawlEntry>();
         for (int i = 0; i < count; i++) {
             final String urlhash = urlRAMStack.get(i);
             final kelondroRow.Entry entry = urlFileIndex.get(urlhash.getBytes());
             if (entry == null) break;
             list.add(new CrawlEntry(entry));
         }
         return list;
     }
     
     public synchronized Iterator<CrawlEntry> iterator() throws IOException {
         return new EntryIterator();
     }
     
     private class EntryIterator implements Iterator<CrawlEntry> {
 
         private Iterator<kelondroRow.Entry> rowIterator;
         
         public EntryIterator() throws IOException {
             rowIterator = urlFileIndex.rows(true, null);
         }
         
         public boolean hasNext() {
             return (rowIterator == null) ? false : rowIterator.hasNext();
         }
 
         public CrawlEntry next() {
             final kelondroRow.Entry entry = rowIterator.next();
             try {
                 return (entry == null) ? null : new CrawlEntry(entry);
             } catch (final IOException e) {
                 rowIterator = null;
                 return null;
             }
         }
 
         public void remove() {
             if (rowIterator != null) rowIterator.remove();
         }
         
     }
     
 }
