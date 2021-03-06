 // plasmaWebStructure.java
 // -----------------------------
 // (C) 2007 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 // first published 15.05.2007 on http://yacy.net
 //
 // This is a part of YaCy, a peer-to-peer based web search engine
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
 
 package de.anomic.yacy.graphics;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Map;
 import java.util.Set;
 import java.util.SortedMap;
 import java.util.TreeMap;
 import java.util.TreeSet;
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.LinkedBlockingQueue;
 
 import net.yacy.cora.date.GenericFormatter;
 import net.yacy.cora.document.MultiProtocolURI;
 import net.yacy.cora.document.UTF8;
 import net.yacy.document.Condenser;
 import net.yacy.document.Document;
 import net.yacy.kelondro.data.meta.DigestURI;
 import net.yacy.kelondro.logging.Log;
 import net.yacy.kelondro.util.FileUtils;
 import net.yacy.kelondro.util.LookAheadIterator;
 
 
 public class WebStructureGraph {
 
     public static int maxCRLDump = 500000;
     public static int maxCRGDump = 200000;
     public static int maxref = 300; // maximum number of references, to avoid overflow when a large link farm occurs (i.e. wikipedia)
     public static int maxhosts = 20000; // maximum number of hosts in web structure map
     
     private final Log    log;
     private final File   structureFile;
     private final TreeMap<String, String> structure_old; // <b64hash(6)>','<host> to <date-yyyymmdd(8)>{<target-b64hash(6)><target-count-hex(4)>}*
     private final TreeMap<String, String> structure_new;
     private final BlockingQueue<leanrefObject> publicRefDNSResolvingQueue;
     private final publicRefDNSResolvingProcess publicRefDNSResolvingWorker;
     
     private final static leanrefObject leanrefObjectPOISON = new leanrefObject(null, null);
     
     private static class leanrefObject {
         public final DigestURI url;
         public final Set<MultiProtocolURI> globalRefURLs;
         public leanrefObject(final DigestURI url, final Set<MultiProtocolURI> globalRefURLs) {
             this.url = url;
             this.globalRefURLs = globalRefURLs;
         }
     }
     
     public WebStructureGraph(final Log log, final File structureFile) {
         this.log = log;
         this.structure_old = new TreeMap<String, String>();
         this.structure_new = new TreeMap<String, String>();
         this.structureFile = structureFile;
         this.publicRefDNSResolvingQueue = new LinkedBlockingQueue<leanrefObject>();
         
         // load web structure
         Map<String, String> loadedStructure;
         try {
             loadedStructure = (this.structureFile.exists()) ? FileUtils.loadMap(this.structureFile) : new TreeMap<String, String>();
         } catch (OutOfMemoryError e) {
             loadedStructure = new TreeMap<String, String>();
         }
         if (loadedStructure != null) this.structure_old.putAll(loadedStructure);
         
         // delete out-dated entries in case the structure is too big
         if (this.structure_old.size() > maxhosts) {
         	// fill a set with last-modified - dates of the structure
         	final TreeSet<String> delset = new TreeSet<String>();
         	String key, value;
         	for (final Map.Entry<String, String> entry : this.structure_old.entrySet()) {
         		key = entry.getKey();
         		value = entry.getValue();
         		if (value.length() >= 8) delset.add(value.substring(0, 8) + key); 
         	}
         	int delcount = this.structure_old.size() - (maxhosts * 9 / 10);
         	final Iterator<String> j = delset.iterator();
         	while ((delcount > 0) && (j.hasNext())) {
         		this.structure_old.remove(j.next().substring(8));
         		delcount--;
         	}
         }
         this.publicRefDNSResolvingWorker = new publicRefDNSResolvingProcess();
         this.publicRefDNSResolvingWorker.start();
     }
     
     private class publicRefDNSResolvingProcess extends Thread {
         public publicRefDNSResolvingProcess() {
         }
         public void run() {
             leanrefObject lro;
             try {
                 while ((lro = publicRefDNSResolvingQueue.take()) != leanrefObjectPOISON) {
                     learnrefs(lro);
                 }
             } catch (InterruptedException e) {
             }
         }
     }
     
     public Integer[] /*(outlinksSame, outlinksOther)*/ generateCitationReference(final DigestURI url, final Document document, final Condenser condenser, final Date docDate) {
         // generate citation reference
         final Map<MultiProtocolURI, String> hl = document.getHyperlinks();
         final Iterator<MultiProtocolURI> it = hl.keySet().iterator();
         final HashSet<MultiProtocolURI> globalRefURLs = new HashSet<MultiProtocolURI>();
         final String refhost = url.getHost();
         MultiProtocolURI u;
         int GCount = 0;
         int LCount = 0;
         while (it.hasNext()) {
             u = it.next();
            if (u == null) continue;
            if (refhost != null && u.getHost() != null && u.getHost().equals(refhost)) {
                 // this is a local link
                 LCount++;
             } else {
                 // this is a global link
                 GCount++;
                 globalRefURLs.add(u);
             }
         }
         
         if (globalRefURLs.size() > 0) try {
             if (this.publicRefDNSResolvingWorker.isAlive()) {
                 this.publicRefDNSResolvingQueue.put(new leanrefObject(url, globalRefURLs));
             } else {
                 this.learnrefs(new leanrefObject(url, globalRefURLs));
             }
         } catch (InterruptedException e) {
             this.learnrefs(new leanrefObject(url, globalRefURLs));
         }
         
         return new Integer[] {Integer.valueOf(LCount), Integer.valueOf(GCount)};
     }
     
     public void learnrefs(final leanrefObject lro) {
         final StringBuilder cpg = new StringBuilder(240);
         assert cpg.length() % 12 == 0 : "cpg.length() = " + cpg.length() + ", cpg = " + cpg.toString();
         final String refhashp = UTF8.String(lro.url.hash(), 6, 6); // ref hash part
         String nexturlhash;
         for (MultiProtocolURI u: lro.globalRefURLs) {
             byte[] nexturlhashb = new DigestURI(u).hash();
             assert nexturlhashb != null;
             if (nexturlhashb != null) {
                 nexturlhash = UTF8.String(nexturlhashb);
                 assert nexturlhash.length() == 12 : "nexturlhash.length() = " + nexturlhash.length() + ", nexturlhash = " + nexturlhash;
                 assert !nexturlhash.substring(6).equals(refhashp);
                 // this is a global link
                 cpg.append(nexturlhash); // store complete hash
                 assert cpg.length() % 12 == 0 : "cpg.length() = " + cpg.length() + ", cpg = " + cpg.toString();
             }
         }
         assert cpg.length() % 12 == 0 : "cpg.length() = " + cpg.length() + ", cpg = " + cpg.toString();
         learn(lro.url, cpg);
     }
     
     private static int refstr2count(final String refs) {
         if ((refs == null) || (refs.length() <= 8)) return 0;
         assert (refs.length() - 8) % 10 == 0 : "refs = " + refs + ", length = " + refs.length();
         return (refs.length() - 8) / 10;
     }
     
     static Map<String, Integer> refstr2map(final String refs) {
         if ((refs == null) || (refs.length() <= 8)) return new HashMap<String, Integer>();
         final Map<String, Integer> map = new HashMap<String, Integer>();
         String c;
         final int refsc = refstr2count(refs);
         int d;
         for (int i = 0; i < refsc; i++) {
             c = refs.substring(8 + i * 10, 8 + (i + 1) * 10);
             try {
                 d = Integer.valueOf(c.substring(6), 16);
             } catch (NumberFormatException e) {
                 d = 1;
             }
             map.put(c.substring(0, 6), d);
         }
         return map;
     }
     
     private static String map2refstr(final Map<String, Integer> map) {
         final StringBuilder s = new StringBuilder(map.size() * 10);
         s.append(GenericFormatter.SHORT_DAY_FORMATTER.format());
         String h;
         for (final Map.Entry<String, Integer> entry : map.entrySet()) {
             s.append(entry.getKey());
             h = Integer.toHexString(entry.getValue().intValue());
             if (h.length() == 0) {
                 s.append("0000");
             } else if (h.length() == 1) {
                 s.append("000").append(h);
             } else if (h.length() == 2) {
                 s.append("00").append(h);
             } else if (h.length() == 3) {
                 s.append('0').append(h);
             } else if (h.length() == 4) {
                 s.append(h);
             } else {
                 s.append("FFFF");
             }
         }
         return s.toString();
     }
     
     public structureEntry outgoingReferences(final String domhash) {
         // returns a map with a domhash(String):refcount(Integer) relation
         assert domhash.length() == 6;
         SortedMap<String, String> tailMap;
         Map<String, Integer> h = new HashMap<String, Integer>();
         String domain = "";
         String date = "";
         String ref;
         synchronized (structure_old) {
             tailMap = structure_old.tailMap(domhash);
             if (!tailMap.isEmpty()) {
                 final String key = tailMap.firstKey();
                 if (key.startsWith(domhash)) {
                     domain = key.substring(7);
                     ref = tailMap.get(key);
                     date = ref.substring(0, 8);
                     h = refstr2map(ref);
                 }
             }
         }
         synchronized (structure_new) {
             tailMap = structure_new.tailMap(domhash);
             if (!tailMap.isEmpty()) {
                 final String key = tailMap.firstKey();
                 if (key.startsWith(domhash)) {
                     ref = tailMap.get(key);
                     if (domain.length() == 0) domain = key.substring(7);
                     if (date.length() == 0) date = ref.substring(0, 8);
                     assert domain.equals(key.substring(7)) : "domain = " + domain + ", key = " + key;
                     h.putAll(refstr2map(ref));
                 }
             }
         }
         if (h.isEmpty()) return null;
         return new structureEntry(domhash, domain, date, h);
     }
     
     public structureEntry incomingReferences(final String domhash) {
         String host = resolveDomHash2DomString(domhash);
         if (host == null) return null;
         // collect the references
         WebStructureGraph.structureEntry sentry;
         HashMap<String, Integer> domhashes = new HashMap<String, Integer>();
         Iterator<WebStructureGraph.structureEntry> i = structureEntryIterator(false);
         while (i.hasNext()) {
             sentry = i.next();
             if (sentry.references.containsKey(domhash)) domhashes.put(sentry.domhash, sentry.references.get(domhash));
         }
         i = structureEntryIterator(true);
         while (i.hasNext()) {
             sentry = i.next();
             if (sentry.references.containsKey(domhash)) domhashes.put(sentry.domhash, sentry.references.get(domhash));
         }
         // construct a new structureEntry Object
         return new structureEntry(
                 domhash,
                 host,
                 GenericFormatter.SHORT_DAY_FORMATTER.format(),
                 domhashes);
     }
     
     public HashMap<String, Integer> incomingDomains(final String domhash) {
         // collect the references
         WebStructureGraph.structureEntry sentry;
         HashMap<String, Integer> domains = new HashMap<String, Integer>();
         Iterator<WebStructureGraph.structureEntry> i = structureEntryIterator(false);
         while (i.hasNext()) {
             sentry = i.next();
             if (sentry.references.containsKey(domhash)) domains.put(sentry.domain, sentry.references.get(domhash));
         }
         i = structureEntryIterator(true);
         while (i.hasNext()) {
             sentry = i.next();
             if (sentry.references.containsKey(domhash)) domains.put(sentry.domain, sentry.references.get(domhash));
         }
         return domains;
     }
     
     public int referencesCount(final String domhash) {
         // returns the number of domains that are referenced by this domhash
         assert domhash.length() == 6 : "domhash = " + domhash;
         SortedMap<String, String> tailMap;
         int c = 0;
         synchronized (structure_old) {
             tailMap = structure_old.tailMap(domhash);
             if (!tailMap.isEmpty()) {
                 final String key = tailMap.firstKey();
                 if (key.startsWith(domhash)) {
                     c = refstr2count(tailMap.get(key));
                 }
             }
         }
         synchronized (structure_new) {
             tailMap = structure_new.tailMap(domhash);
             if (!tailMap.isEmpty()) {
                 final String key = tailMap.firstKey();
                 if (key.startsWith(domhash)) {
                     c += refstr2count(tailMap.get(key));
                 }
             }
         }
         return c;
     }
     
     public String resolveDomHash2DomString(final String domhash) {
         // returns the domain as string, null if unknown
         assert domhash.length() == 6;
         SortedMap<String, String> tailMap;
         synchronized(structure_old) {
             tailMap = structure_old.tailMap(domhash);
             if (!tailMap.isEmpty()) {
                 final String key = tailMap.firstKey();
                 if (key.startsWith(domhash)) {
                     return key.substring(7);
                 }
             }
         }
         synchronized(structure_new) {
             tailMap = structure_new.tailMap(domhash);
             if (!tailMap.isEmpty()) {
                 final String key = tailMap.firstKey();
                 if (key.startsWith(domhash)) {
                     return key.substring(7);
                 }
             }
         }
         return null;
     }
     
     private void learn(final DigestURI url, final StringBuilder reference /*string of b64(12digits)-hashes*/) {
         final String domhash = UTF8.String(url.hash(), 6, 6);
 
         // parse the new reference string and join it with the stored references
         structureEntry structure = outgoingReferences(domhash);
         final Map<String, Integer> refs = (structure == null) ? new HashMap<String, Integer>() : structure.references;
         assert reference.length() % 12 == 0 : "reference.length() = " + reference.length() + ", reference = " + reference.toString();
         String dom;
         int c;
         for (int i = 0; i < reference.length() / 12; i++) {
             dom = reference.substring(i * 12 + 6, (i + 1) * 12);
             c = 0;
             if (refs.containsKey(dom)) {
                 c = (refs.get(dom)).intValue();
             }
             refs.put(dom, Integer.valueOf(++c));
         }
 
         // check if the maxref is exceeded
         if (refs.size() > maxref) {
             int shrink = refs.size() - (maxref * 9 / 10);
             delloop: while (shrink > 0) {
                 // shrink the references: the entry with the smallest number of references is removed
                 int minrefcount = Integer.MAX_VALUE;
                 String minrefkey = null;
                 findloop: for (final Map.Entry<String, Integer> entry : refs.entrySet()) {
                     if (entry.getValue().intValue() < minrefcount) {
                         minrefcount = entry.getValue().intValue();
                         minrefkey = entry.getKey();
                     }
                     if (minrefcount == 1) break findloop;
                 }
                 // remove the smallest
                 if (minrefkey == null) break delloop;
                 refs.remove(minrefkey);
                 shrink--;
             }
         }
 
         // store the map back to the structure
         synchronized(structure_new) {
             structure_new.put(domhash + "," + url.getHost(), map2refstr(refs));
         }
     }
     
     private static void joinStructure(final TreeMap<String, String> into, final TreeMap<String, String> from) {
         for (final Map.Entry<String, String> e: from.entrySet()) {
             if (into.containsKey(e.getKey())) {
                 final Map<String, Integer> s0 = refstr2map(into.get(e.getKey()));
                 final Map<String, Integer> s1 = refstr2map(e.getValue());
                 for (final Map.Entry<String, Integer> r: s1.entrySet()) {
                     if (s0.containsKey(r.getKey())) {
                         s0.put(r.getKey(), s0.get(r.getKey()).intValue() + r.getValue().intValue());
                     } else {
                         s0.put(r.getKey(), r.getValue().intValue());
                     }
                 }
                 into.put(e.getKey(), map2refstr(s0));
             } else {
                 into.put(e.getKey(), e.getValue());
             }
         }
     }
     
     public void joinOldNew() {
         synchronized(structure_new) {
             joinStructure(this.structure_old, this.structure_new);
             this.structure_new.clear();
         }
     }
     
     public void saveWebStructure() {
         joinOldNew();
         try {
             synchronized(structure_old) {
                 FileUtils.saveMap(this.structureFile, this.structure_old, "Web Structure Syntax: <b64hash(6)>','<host> to <date-yyyymmdd(8)>{<target-b64hash(6)><target-count-hex(4)>}*");
             }
         } catch (final IOException e) {
             Log.logException(e);
         }
     }
     
     public String hostWithMaxReferences() {
         // find domain with most references
         String maxhost = null;
         int refsize, maxref = 0;
         joinOldNew();
         synchronized(structure_new) {
             for (final Map.Entry<String, String> entry : structure_old.entrySet()) {
                 refsize = entry.getValue().length();
                 if (refsize > maxref) {
                     maxref = refsize;
                     maxhost = entry.getKey().substring(7);
                 }
             }
         }
         return maxhost;
     }
     
     public Iterator<structureEntry> structureEntryIterator(final boolean latest) {
         // iterates objects of type structureEntry
         return new structureIterator(latest);
     }
     
     public class structureIterator extends LookAheadIterator<structureEntry> implements Iterator<structureEntry> {
 
         private final Iterator<Map.Entry<String, String>> i;
         
         public structureIterator(final boolean latest) {
             i = ((latest) ? structure_new : structure_old).entrySet().iterator();
         }
         
         public structureEntry next0() {
             Map.Entry<String, String> entry = null;
             String dom = null, ref = "";
             while (i.hasNext()) {
                 entry = i.next();
                 ref = entry.getValue();
                 if ((ref.length() - 8) % 10 != 0) continue;
                 dom = entry.getKey();
                 if (dom.length() >= 8) break;
                 dom = null;
             }
             if (entry == null || dom == null) return null;
             assert (ref.length() - 8) % 10 == 0 : "refs = " + ref + ", length = " + ref.length();
             return new structureEntry(dom.substring(0, 6), dom.substring(7), ref.substring(0, 8), refstr2map(ref));
         }
     }
     
     public static class structureEntry {
         public String domhash, domain, date;
         public Map<String, Integer> references;
         public structureEntry(
                 final String domhash,
                 final String domain, 
                 final String date,
                 final Map<String, Integer> references) {
             this.domhash = domhash;
             this.domain = domain;
             this.date = date;
             this.references = references;
         }
     }
     
     public void close() {
         if (this.publicRefDNSResolvingWorker.isAlive()) {
             log.logInfo("Waiting for the DNS Resolving Queue to terminate");
             try {
                 this.publicRefDNSResolvingQueue.put(leanrefObjectPOISON);
                 this.publicRefDNSResolvingWorker.join(5000);
             } catch (InterruptedException e) {
             }
         }
         log.logInfo("Saving Web Structure File");
         saveWebStructure();
     }
 }
