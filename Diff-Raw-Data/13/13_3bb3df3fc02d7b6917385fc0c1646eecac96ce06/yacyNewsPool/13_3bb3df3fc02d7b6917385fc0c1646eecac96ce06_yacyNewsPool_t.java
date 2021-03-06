 // yacyNewsActions.java
 // -----------------------
 // part of YaCy
 // (C) by Michael Peter Christen; mc@anomic.de
 // first published on http://www.anomic.de
 // Frankfurt, Germany, 2005
 //
 // $LastChangedDate$
 // $LastChangedRevision$
 // $LastChangedBy$
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
 //
 // Using this software in any meaning (reading, learning, copying, compiling,
 // running) means that you agree that the Author(s) is (are) not responsible
 // for cost, loss of data or any harm that may be caused directly or indirectly
 // by usage of this softare or this documentation. The usage of this software
 // is on your own risk. The installation and usage (starting/running) of this
 // software may allow other people or application to access your computer and
 // any attached devices and is highly dependent on the configuration of the
 // software which must be done by the user of the software; the author(s) is
 // (are) also not responsible for proper configuration and usage of the
 // software, even if provoked by documentation provided together with
 // the software.
 //
 // Any changes to this file according to the GPL as documented in the file
 // gpl.txt aside this file in the shipment you received can be done to the
 // lines that follows this copyright notice here, but changes must not be
 // done inside the copyright notice above. A re-distribution must contain
 // the intact and unchanged copyright notice.
 // Contributions and changes to the program code must be marked as such.
 
 package de.anomic.yacy;
 
 import java.io.File;
 import java.io.IOException;
 import java.util.HashSet;
 
 public class yacyNewsPool {
     
     public static final int INCOMING_DB  = 0;
     public static final int PROCESSED_DB = 1;
     public static final int OUTGOING_DB  = 2;
     public static final int PUBLISHED_DB = 3;
     
     public static final String[] category = {
         "prfleupd", // a profile entry was updated (implemented)
         "crwlstrt", // a crawl with remote indexing was startet
         "crwlstop", // a crawl with remote indexing was stopped
         "crwlcomm", // a comment on a crawl with remote indexing
         "blckladd", // a public blacklist entry was added
         "blcklavt", // a vote and comment on a public blacklist add
         "blckldel", // a public blacklist entry was deleted
         "blckldvt", // a vote and comment on a public blacklist delete
         "flshradd", // a file was added to the file share
         "flshrdel", // a file was added to the file share
         "flshrcom", // a comment to a file share entry
         "bkmrkadd", // a bookmark was added/created
         "bkmrkavt", // a vote and comment on a bookmark add
         "bkmrkmov", // a bookmark was moved
         "bkmrkmvt", // a vote and comment on a bookmark move
         "bkmrkdel", // a bookmark was deleted
         "bkmrkdvt", // a vote and comment on a bookmark delete
         "stippadd", // a surf tipp was added
         "stippavt", // a vote and comment on a surf tipp
         "wiki_upd", // a wiki page was updated
         "wiki_del", // a wiki page das deleted
         "blog_add", // a blog entry was added
         "blog_del"  // a blog page das deleted
     };
     public static HashSet categories;
     static {
         categories = new HashSet();
         for (int i = 0; i < category.length; i++) categories.add(category[i]);
     }
     
     private yacyNewsDB newsDB;
     private yacyNewsQueue outgoingNews, publishedNews, incomingNews, processedNews;
     private int maxDistribution;
     
     
     public yacyNewsPool(File yacyDBPath, long preloadTime) throws IOException {
         newsDB = new yacyNewsDB(new File(yacyDBPath, "news1.db"), preloadTime);
         outgoingNews  = new yacyNewsQueue(new File(yacyDBPath, "newsOut1.stack"), newsDB);
         publishedNews = new yacyNewsQueue(new File(yacyDBPath, "newsPublished1.stack"), newsDB);
         incomingNews  = new yacyNewsQueue(new File(yacyDBPath, "newsIn1.stack"), newsDB);
         processedNews = new yacyNewsQueue(new File(yacyDBPath, "newsProcessed1.stack"), newsDB);
         maxDistribution = 30;
     }
     
     public synchronized void close() {
         newsDB.close();
         outgoingNews.close();
         publishedNews.close();
         incomingNews.close();
         processedNews.close();
     }
     
     public int dbSize() {
         try {
             return newsDB.size();
         } catch (IOException e) {
             e.printStackTrace();
             return 0;
         }
     }
     
     public void publishMyNews(yacyNewsRecord record) {
         // this shall be called if our peer generated a new news record and wants to publish it
         try {
             if (newsDB.get(record.id()) == null) {
                 incomingNews.push(record); // we want to see our own news..
                 outgoingNews.push(record); // .. and put it on the publishing list
             }
         } catch (IOException e) {}
     }
     
     public yacyNewsRecord myPublication() throws IOException {
         // generate a record for next peer-ping
         if (outgoingNews.size() == 0) return null;
         yacyNewsRecord record = outgoingNews.topInc();
         if ((record != null) && (record.distributed() >= maxDistribution)) {
             // move record to its final position. This is only for history
             publishedNews.push(outgoingNews.pop(0));
         }
         return record;
     }
     
     public void enqueueIncomingNews(yacyNewsRecord record) throws IOException {
         // called if a news is attached to a seed
 
         // check consistency
         if (record.id() == null) return;
         if (record.id().length() != yacyNewsRecord.idLength) return;
         if (record.category() == null) return;
         if (!(categories.contains(record.category()))) return;
         if (record.created().getTime() == 0) return;
         
         // double-check with old news
         if (newsDB.get(record.id()) != null) return;
         incomingNews.push(record);
     }
     
     public int size(int dbKey) {
         return switchQueue(dbKey).size();
     }
     
     public int automaticProcess() throws IOException, InterruptedException {
         // processes news in the incoming-db
         // returns number of processes
         yacyNewsRecord record;
         int pc = 0;
         synchronized (this.incomingNews) {
             for (int i = this.incomingNews.size() - 1; i >= 0; i--) {
                 // check for interruption
                 if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Shutdown in progress");
                 
                 // get next news record
                 record = this.incomingNews.top(i);
                 if ((i > 500) || (automaticProcessP(record))) {
                     this.incomingNews.pop(i);
                     this.processedNews.push(record);
                     //newsDB.remove(id);
                     pc++;
                 }
             }
         }
         return pc;
     }
     
     long day = 1000 * 60 * 60 * 24;
     private boolean automaticProcessP(yacyNewsRecord record) {
         if (record == null) return false;
         if (record.category() == null) return true;
         if ((System.currentTimeMillis() - record.created().getTime()) > (14 * day)) {
             // remove everything after 1 week
             return true;
         }
         if ((record.category().equals("wiki_upd")) &&
                 ((System.currentTimeMillis() - record.created().getTime()) > (3 * day))) {
                 return true;
             }
         if ((record.category().equals("blog_add")) &&
                 ((System.currentTimeMillis() - record.created().getTime()) > (3 * day))) {
                 return true;
             }
         if ((record.category().equals("prfleupd")) &&
                 ((System.currentTimeMillis() - record.created().getTime()) > (7 * day))) {
                 return true;
             }
         if ((record.category().equals("crwlstrt")) &&
             ((System.currentTimeMillis() - record.created().getTime()) > (2 * day))) {
             yacySeed seed = yacyCore.seedDB.get(record.originator());
             if (seed == null) return false;
             try {
                 return (Integer.parseInt(seed.get(yacySeed.ISPEED, "-")) < 10);
             } catch (NumberFormatException ee) {
                 return true;
             }
         }
         return false;
     }
     
     public yacyNewsRecord get(int dbKey, int element) throws IOException {
         yacyNewsQueue queue = switchQueue(dbKey);
        yacyNewsRecord record = null;
        int s;
         synchronized (queue) {
            while ((record == null) && ((s = queue.size()) > 0)) {
                record = queue.top(element);
                if (record == null) {
                    queue.pop(element);
                    if (queue.size() == s) break;
                }
            }
         }
         return record;
     }
     
     public synchronized yacyNewsRecord getSpecific(int dbKey, String category, String key, String value) throws IOException {
         yacyNewsQueue queue = switchQueue(dbKey);
         yacyNewsRecord record;
         String s;
         for (int i = queue.size() - 1; i >= 0; i--) {
             record = queue.top(i);
             if ((record != null) && (record.category().equals(category))) {
                 s = (String) record.attributes().get(key);
                 if ((s != null) && (s.equals(value))) return record;
             }
         }
         return null;
     }
 
     public synchronized yacyNewsRecord getByOriginator(int dbKey, String category, String originatorHash) throws IOException {
         yacyNewsQueue queue = switchQueue(dbKey);
         yacyNewsRecord record;
         for (int i = queue.size() - 1; i >= 0; i--) {
             record = queue.top(i);
             if ((record != null) &&
                 (record.category().equals(category)) &&
                 (record.originator().equals(originatorHash))) {
                 return record;
             }
         }
         return null;
     }
 
     public synchronized yacyNewsRecord getByID(int dbKey, String id) throws IOException {
         switch (dbKey) {
             case INCOMING_DB:   return incomingNews.get(id);
             case PROCESSED_DB:  return processedNews.get(id);
             case OUTGOING_DB:   return outgoingNews.get(id);
             case PUBLISHED_DB:  return publishedNews.get(id);
         }
         return null;
     }
 
     private yacyNewsQueue switchQueue(int dbKey) {
         switch (dbKey) {
             case INCOMING_DB:	return incomingNews;
             case PROCESSED_DB:  return processedNews;
             case OUTGOING_DB:   return outgoingNews;
             case PUBLISHED_DB:  return publishedNews;
         }
         return null;
     }
     
     public void clear(int dbKey) {
         // this is called if a queue element shall be moved to another queue or off the queue
         // it depends on the dbKey how the record is handled
         switch (dbKey) {
             case INCOMING_DB:	incomingNews.clear(); break;
             case PROCESSED_DB:  processedNews.clear(); break;
             case OUTGOING_DB:   outgoingNews.clear(); break;
             case PUBLISHED_DB:  publishedNews.clear(); break;
         }
     }
 
     public void moveOff(int dbKey, String id) throws IOException {
         // this is called if a queue element shall be moved to another queue or off the queue
         // it depends on the dbKey how the record is handled
         switch (dbKey) {
             case INCOMING_DB:   moveOff(incomingNews, processedNews, id); break;
             case PROCESSED_DB:  moveOff(processedNews, null,id); break;
             case OUTGOING_DB:   moveOff(outgoingNews, publishedNews, id); break;
             case PUBLISHED_DB:  moveOff(publishedNews, null, id); break;
         }
     }
 
     private boolean moveOff(yacyNewsQueue fromqueue, yacyNewsQueue toqueue, String id) throws IOException {
         // called if a published news shall be removed
         yacyNewsRecord record = fromqueue.remove(id);
         if (record == null) {
             //System.out.println("DEBUG: record == null");
             return false;
         }
         if (toqueue != null) toqueue.push(record);
         else if ((incomingNews.get(id) == null) && (processedNews.get(id) == null) && (outgoingNews.get(id) == null) && (publishedNews.get(id) == null)){
             newsDB.remove(id);
             //System.out.println("DEBUG: News-ID " + id + " deleted");
         }
         //if (incomingNews.get(id) != null) System.out.println("DEBUG: News-ID " + id + " in incomingNews");
         //if (processedNews.get(id) != null) System.out.println("DEBUG: News-ID " + id + " in processedNews");
         //if (outgoingNews.get(id) != null) System.out.println("DEBUG: News-ID " + id + " in outgoingNews");
         //if (publishedNews.get(id) != null) System.out.println("DEBUG: News-ID " + id + " in publishedNews");
         return true;
     }
     
     
 }
