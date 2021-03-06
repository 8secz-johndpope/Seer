 // IndexCreate_p.java
 // -----------------------
 // part of the AnomicHTTPD caching proxy
 // (C) by Michael Peter Christen; mc@anomic.de
 // first published on http://www.anomic.de
 // Frankfurt, Germany, 2004
 // last major change: 02.12.2004
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
 // done inside the copyright notive above. A re-distribution must contain
 // the intact and unchanged copyright notice.
 // Contributions and changes to the program code must be marked as such.
 
 // You must compile this file with
 // javac -classpath .:../classes IndexCreate_p.java
 // if the shell's current path is HTROOT
 
 import java.io.File;
 import java.io.OutputStream;
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.Map;
 
 import de.anomic.data.wikiCode;
 import de.anomic.htmlFilter.htmlFilterContentScraper;
 import de.anomic.htmlFilter.htmlFilterOutputStream;
 import de.anomic.http.httpHeader;
 import de.anomic.plasma.plasmaCrawlProfile;
 import de.anomic.plasma.plasmaSwitchboard;
 import de.anomic.plasma.plasmaURL;
 import de.anomic.server.serverFileUtils;
 import de.anomic.server.serverObjects;
 import de.anomic.server.serverSwitch;
 import de.anomic.server.serverThread;
 import de.anomic.tools.bitfield;
 import de.anomic.yacy.yacyCore;
 import de.anomic.yacy.yacySeed;
 import de.anomic.yacy.yacyNewsRecord;
 import de.anomic.yacy.yacyNewsPool;
 
 public class IndexCreate_p {
     
     public static serverObjects respond(httpHeader header, serverObjects post, serverSwitch env) {
         // return variable that accumulates replacements
         plasmaSwitchboard switchboard = (plasmaSwitchboard) env;
         serverObjects prop = new serverObjects();
         
         prop.put("error", 0);
         prop.put("info", 0);
         prop.put("refreshbutton", 0);
                             
         switchboard.cleanProfiles();
         
         if (post != null) {
             if (post.containsKey("crawlingstart")) {
                 // init crawl
                 if (yacyCore.seedDB == null) {
                     prop.put("error", 3);
                 } else {
                     // set new properties
                     String newcrawlingfilter = post.get("crawlingFilter", ".*");
                     env.setConfig("crawlingFilter", newcrawlingfilter);
                     int newcrawlingdepth = Integer.parseInt(post.get("crawlingDepth", "0"));
                     env.setConfig("crawlingDepth", Integer.toString(newcrawlingdepth));
                    int recrawlIfOlder = Integer.parseInt(post.get("crawlingIfOlder", "-1"));
                     env.setConfig("crawlingIfOlder", recrawlIfOlder);
                    int domFilterDepth = Integer.parseInt(post.get("crawlingDomFilterDepth", "-1"));
                     env.setConfig("crawlingDomFilterDepth", Integer.toString(domFilterDepth));
                    int domMaxPages = Integer.parseInt(post.get("crawlingDomMaxPages", "-1"));
                     env.setConfig("crawlingDomMaxPages", Integer.toString(domMaxPages));
                     boolean crawlingQ = post.get("crawlingQ", "").equals("on");
                     env.setConfig("crawlingQ", (crawlingQ) ? "true" : "false");
                     boolean storeHTCache = post.get("storeHTCache", "").equals("on");
                     env.setConfig("storeHTCache", (storeHTCache) ? "true" : "false");
                     boolean localIndexing = post.get("localIndexing", "").equals("on");
                     env.setConfig("localIndexing", (localIndexing) ? "true" : "false");
                     boolean crawlOrder = post.get("crawlOrder", "").equals("on");
                     env.setConfig("crawlOrder", (crawlOrder) ? "true" : "false");
                     boolean xsstopw = post.get("xsstopw", "").equals("on");
                     env.setConfig("xsstopw", (xsstopw) ? "true" : "false");
                     boolean xdstopw = post.get("xdstopw", "").equals("on");
                     env.setConfig("xdstopw", (xdstopw) ? "true" : "false");
                     boolean xpstopw = post.get("xpstopw", "").equals("on");
                     env.setConfig("xpstopw", (xpstopw) ? "true" : "false");
                     
                     String crawlingMode = post.get("crawlingMode","url");
                     if (crawlingMode.equals("url")) {
                         // getting the crawljob start url
                         String crawlingStart = post.get("crawlingURL","");
                         crawlingStart = crawlingStart.trim();
                         
                         // adding the prefix http:// if necessary
                         if (!(crawlingStart.startsWith("http"))) crawlingStart = "http://" + crawlingStart;
 
                         // normalizing URL
                         crawlingStart = htmlFilterContentScraper.urlNormalform(null, crawlingStart);
                         
                         // check if url is proper
                         URL crawlingStartURL = null;
                         try {
                             crawlingStartURL = new URL(crawlingStart);
                         } catch (MalformedURLException e) {
                             crawlingStartURL = null;
                         }
                         
                         // check if pattern matches
                         if ((crawlingStartURL == null) /* || (!(crawlingStart.matches(newcrawlingfilter))) */) {
                             // print error message
                             prop.put("error", 4); //crawlfilter does not match url
                             prop.put("error_newcrawlingfilter", newcrawlingfilter);
                             prop.put("error_crawlingStart", crawlingStart);
                         } else try {
                             // stack request
                             // first delete old entry, if exists
                             String urlhash = plasmaURL.urlHash(crawlingStart);
                             switchboard.urlPool.loadedURL.remove(urlhash);
                             switchboard.urlPool.noticeURL.remove(urlhash);
                             switchboard.urlPool.errorURL.remove(urlhash);
                             
                             // stack url
                             plasmaCrawlProfile.entry pe = switchboard.profiles.newEntry(crawlingStartURL.getHost(), crawlingStart, newcrawlingfilter, newcrawlingfilter, newcrawlingdepth, newcrawlingdepth, recrawlIfOlder, domFilterDepth, domMaxPages, crawlingQ, storeHTCache, true, localIndexing, crawlOrder, xsstopw, xdstopw, xpstopw);
                             String reasonString = switchboard.sbStackCrawlThread.stackCrawl(crawlingStart, null, yacyCore.seedDB.mySeed.hash, "CRAWLING-ROOT", new Date(), 0, pe);
                             
                             if (reasonString == null) {
                                 // liftoff!
                                 prop.put("info", 2);//start msg
                                 prop.put("info_crawlingURL", ((String) post.get("crawlingURL")));
                                 
                                 // generate a YaCyNews if the global flag was set
                                 if (crawlOrder) {
                                     Map m = new HashMap(pe.map()); // must be cloned
                                     m.remove("specificDepth");
                                     m.remove("localIndexing");
                                     m.remove("remoteIndexing");
                                     m.remove("xsstopw");
                                     m.remove("xpstopw");
                                     m.remove("xdstopw");
                                     m.remove("storeTXCache");
                                     m.remove("storeHTCache");
                                     m.remove("generalFilter");
                                     m.remove("specificFilter");
                                     m.put("intention", post.get("intention", "").replace(',', '/'));
                                     yacyCore.newsPool.publishMyNews(new yacyNewsRecord("crwlstrt", m));
                                 }
                                 
                             } else {
                                 prop.put("error", 5); //Crawling failed
                                 prop.put("error_crawlingURL", wikiCode.replaceHTML(((String) post.get("crawlingURL"))));
                                 prop.put("error_reasonString", reasonString);
                                 
                                 switchboard.urlPool.errorURL.newEntry(crawlingStartURL, null, yacyCore.seedDB.mySeed.hash, yacyCore.seedDB.mySeed.hash,
                                                                       crawlingStartURL.getHost(), reasonString, new bitfield(plasmaURL.urlFlagLength), false);                                
                             }
                         } catch (Exception e) {
                             // mist
                             prop.put("error", 6);//Error with url
                             prop.put("error_crawlingStart", crawlingStart);
                             prop.put("error_error", e.getMessage());
                             e.printStackTrace();
                         }                        
                         
                     } else if (crawlingMode.equals("file")) {                        
                         if (post.containsKey("crawlingFile")) {
                             // getting the name of the uploaded file
                             String fileName = (String) post.get("crawlingFile");  
                             try {                         
                                 File file = new File(fileName);
                                 
                                 // getting the content of the bookmark file
                                 byte[] fileContent = (byte[]) post.get("crawlingFile$file");
                                 
                                 // parsing the bookmark file and fetching the headline and contained links
                                 htmlFilterContentScraper scraper = new htmlFilterContentScraper(file.toURL());
                                 OutputStream os = new htmlFilterOutputStream(null, scraper, null, false);
                                 serverFileUtils.write(fileContent,os);
                                 os.close();
                                 
                                 //String headline = scraper.getHeadline();
                                 HashMap hyperlinks = (HashMap) scraper.getAnchors();
                                 
                                 // creating a crawler profile
                                 plasmaCrawlProfile.entry profile = switchboard.profiles.newEntry(fileName, file.toURL().toString(), newcrawlingfilter, newcrawlingfilter, newcrawlingdepth, newcrawlingdepth, recrawlIfOlder, domFilterDepth, domMaxPages, crawlingQ, storeHTCache, true, localIndexing, crawlOrder, xsstopw, xdstopw, xpstopw);                                
                                 
                                 // loop through the contained links
                                 Iterator interator = hyperlinks.entrySet().iterator();
                                 int c = 0;
                                 while (interator.hasNext()) {
                                     Map.Entry e = (Map.Entry) interator.next();
                                     String nexturlstring = (String) e.getKey();
                                     
                                     if (nexturlstring == null) continue;
                                     
                                     nexturlstring = nexturlstring.trim();
                                     
                                     // normalizing URL
                                     nexturlstring = htmlFilterContentScraper.urlNormalform(null, nexturlstring);                                    
                                     
                                     // generating an url object
                                     URL nexturlURL = null;
                                     try {
                                         nexturlURL = new URL(nexturlstring);
                                     } catch (MalformedURLException ex) {
                                         nexturlURL = null;
                                         c++;
                                         continue;
                                     }                                    
                                     
                                     // enqueuing the url for crawling
                                     String rejectReason = switchboard.sbStackCrawlThread.stackCrawl(nexturlstring, null, yacyCore.seedDB.mySeed.hash, (String)e.getValue(), new Date(), 1, profile);                                    
                                     
                                     // if something failed add the url into the errorURL list
                                     if (rejectReason == null) {
                                         c++;
                                     } else {
                                         switchboard.urlPool.errorURL.newEntry(nexturlURL, null, yacyCore.seedDB.mySeed.hash, yacyCore.seedDB.mySeed.hash,
                                        (String) e.getValue(), rejectReason, new bitfield(plasmaURL.urlFlagLength), false);
                                     }
                                 }                             
                                 
                             } catch (Exception e) {
                                 // mist
                                 prop.put("error", 7);//Error with file
                                 prop.put("error_crawlingStart", fileName);
                                 prop.put("error_error", e.getMessage());
                                 e.printStackTrace();                                
                             }
                         }                        
                     }
                 }
             }
 
             
             
             if (post.containsKey("distributedcrawling")) {
                 long newBusySleep = Integer.parseInt(env.getConfig("62_remotetriggeredcrawl_busysleep", "100"));
                 if (post.get("dcr", "").equals("acceptCrawlMax")) {
                     env.setConfig("crawlResponse", "true");
                     newBusySleep = 100;
                 } else if (post.get("dcr", "").equals("acceptCrawlLimited")) {
                     env.setConfig("crawlResponse", "true");
                     int newppm = Integer.parseInt(post.get("acceptCrawlLimit", "1"));
                     if (newppm < 1) newppm = 1;
                     newBusySleep = 60000 / newppm;
                     if (newBusySleep < 100) newBusySleep = 100;
                 } else if (post.get("dcr", "").equals("acceptCrawlDenied")) {
                     env.setConfig("crawlResponse", "false");
                 }
                 serverThread rct = switchboard.getThread("62_remotetriggeredcrawl");
                 rct.setBusySleep(newBusySleep);
                 env.setConfig("62_remotetriggeredcrawl_busysleep", Long.toString(newBusySleep));
                 //boolean crawlResponse = ((String) post.get("acceptCrawlMax", "")).equals("on");
                 //env.setConfig("crawlResponse", (crawlResponse) ? "true" : "false");
             }
 
             
             if (post.containsKey("pausecrawlqueue")) {
                 switchboard.pauseCrawlJob(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL);
                 prop.put("info", 4);//crawling paused
             }           
             
             if (post.containsKey("continuecrawlqueue")) {
                 switchboard.continueCrawlJob(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL);
                 prop.put("info", 5);//crawling continued
             }                
         }
         
         // define visible variables
         prop.put("proxyPrefetchDepth", env.getConfig("proxyPrefetchDepth", "0"));
         prop.put("crawlingDepth", env.getConfig("crawlingDepth", "0"));
         prop.put("crawlingFilter", env.getConfig("crawlingFilter", "0"));
         prop.put("crawlingIfOlder", env.getConfig("crawlingIfOlder", "-1"));
         prop.put("crawlingDomFilterDepth", env.getConfig("crawlingDomFilterDepth", "-1"));
         prop.put("crawlingDomMaxPages", env.getConfig("crawlingDomMaxPages", "-1"));
         prop.put("crawlingQChecked", env.getConfig("crawlingQ", "").equals("true") ? 1 : 0);
         prop.put("storeHTCacheChecked", env.getConfig("storeHTCache", "").equals("true") ? 1 : 0);
         prop.put("localIndexingChecked", env.getConfig("localIndexing", "").equals("true") ? 1 : 0);
         prop.put("crawlOrderChecked", env.getConfig("crawlOrder", "").equals("true") ? 1 : 0);
         long busySleep = Integer.parseInt(env.getConfig("62_remotetriggeredcrawl_busysleep", "100"));
         if (busySleep < 100) {
             busySleep = 100;
             env.setConfig("62_remotetriggeredcrawl_busysleep", Long.toString(busySleep));
         }
         if (env.getConfig("crawlResponse", "").equals("true")) {
             if (busySleep <= 100) {
                 prop.put("acceptCrawlMaxChecked", 1);
                 prop.put("acceptCrawlLimitedChecked", 0);
                 prop.put("acceptCrawlDeniedChecked", 0);
             } else {
                 prop.put("acceptCrawlMaxChecked", 0);
                 prop.put("acceptCrawlLimitedChecked", 1);
                 prop.put("acceptCrawlDeniedChecked", 0);
             }
         } else {
             prop.put("acceptCrawlMaxChecked", 0);
             prop.put("acceptCrawlLimitedChecked", 0);
             prop.put("acceptCrawlDeniedChecked", 1);
         }
         int ppm = (int) (60000L / busySleep);
         if (ppm > 60) ppm = 60;
         prop.put("PPM", ppm);
         prop.put("xsstopwChecked", env.getConfig("xsstopw", "").equals("true") ? 1 : 0);
         prop.put("xdstopwChecked", env.getConfig("xdstopw", "").equals("true") ? 1 : 0);
         prop.put("xpstopwChecked", env.getConfig("xpstopw", "").equals("true") ? 1 : 0);
         
         int queueStackSize = switchboard.sbQueue.size();
         int loaderThreadsSize = switchboard.cacheLoader.size();
         int crawlerListSize = switchboard.urlPool.noticeURL.stackSize();
         int completequeue = queueStackSize + loaderThreadsSize + crawlerListSize;
         
         if ((completequeue > 0) || ((post != null) && (post.containsKey("refreshpage")))) {
             prop.put("refreshbutton", 1);
         }
         
         // create prefetch table
         boolean dark;
         
         //  sed crawl profiles
         int count = 0;
         //try{
         Iterator it = switchboard.profiles.profiles(true);
         plasmaCrawlProfile.entry profile;
         dark = true;
        Iterator domnamesi;
        String domnames;
         while (it.hasNext()) {
             profile = (plasmaCrawlProfile.entry) it.next();
             //table += profile.map().toString() + "<br>";
             prop.put("crawlProfiles_"+count+"_dark", ((dark) ? 1 : 0));
             prop.put("crawlProfiles_"+count+"_name", wikiCode.replaceHTML(profile.name()));
             prop.put("crawlProfiles_"+count+"_startURL", wikiCode.replaceHTML(profile.startURL()));
             prop.put("crawlProfiles_"+count+"_handle", wikiCode.replaceHTML(profile.handle()));
             prop.put("crawlProfiles_"+count+"_depth", profile.generalDepth());
             prop.put("crawlProfiles_"+count+"_filter", profile.generalFilter());
            prop.put("crawlProfiles_"+count+"_crawlingIfOlder", (profile.recrawlIfOlder() == Long.MAX_VALUE) ? "no re-crawl" : ""+profile.recrawlIfOlder());
            prop.put("crawlProfiles_"+count+"_crawlingDomFilterDepth", (profile.domFilterDepth() == Integer.MAX_VALUE) ? "inactive" : ""+profile.domFilterDepth());
            domnamesi = profile.domNames();
            domnames=""; while (domnamesi.hasNext()) domnames += ((String) domnamesi.next()) + ", ";
            prop.put("crawlProfiles_"+count+"_crawlingDomFilterContent", domnames);
            prop.put("crawlProfiles_"+count+"_crawlingDomMaxPages", (profile.domMaxPages() == Integer.MAX_VALUE) ? "unlimited" : ""+profile.domMaxPages());
             prop.put("crawlProfiles_"+count+"_withQuery", ((profile.crawlingQ()) ? 1 : 0));
             prop.put("crawlProfiles_"+count+"_storeCache", ((profile.storeHTCache()) ? 1 : 0));
             prop.put("crawlProfiles_"+count+"_localIndexing", ((profile.localIndexing()) ? 1 : 0));
             prop.put("crawlProfiles_"+count+"_remoteIndexing", ((profile.remoteIndexing()) ? 1 : 0));
             
             dark = !dark;
             count++;
         }
         //}catch(IOException e){};
         prop.put("crawlProfiles", count);
         
         
         // create other peer crawl table using YaCyNews
         int availableNews = yacyCore.newsPool.size(yacyNewsPool.INCOMING_DB);
         int showedCrawl = 0;
         yacyNewsRecord record;
         yacySeed peer;
         String peername;
         try {
             for (int c = 0; c < availableNews; c++) {
                 record = yacyCore.newsPool.get(yacyNewsPool.INCOMING_DB, c);
                 if (record == null) continue;
                 if (record.category().equals("crwlstrt")) {
                     peer = yacyCore.seedDB.get(record.originator());
                     if (peer == null) peername = record.originator(); else peername = peer.getName();
                     prop.put("otherCrawlStartInProgress_" + showedCrawl + "_dark", ((dark) ? 1 : 0));
                     prop.put("otherCrawlStartInProgress_" + showedCrawl + "_cre", record.created());
                     prop.put("otherCrawlStartInProgress_" + showedCrawl + "_peername", wikiCode.replaceHTML(peername));
                     prop.put("otherCrawlStartInProgress_" + showedCrawl + "_startURL", wikiCode.replaceHTML(record.attributes().get("startURL").toString()));
                     prop.put("otherCrawlStartInProgress_" + showedCrawl + "_intention", wikiCode.replaceHTML(record.attributes().get("intention").toString()));
                     prop.put("otherCrawlStartInProgress_" + showedCrawl + "_generalDepth", record.attributes().get("generalDepth"));
                     prop.put("otherCrawlStartInProgress_" + showedCrawl + "_crawlingQ", (record.attributes().get("crawlingQ").equals("true")) ? 1 : 0);
                     showedCrawl++;
                     if (showedCrawl > 20) break;
                 }
                 
             }
         } catch (IOException e) {}
         prop.put("otherCrawlStartInProgress", showedCrawl);
         
         // finished remote crawls
         availableNews = yacyCore.newsPool.size(yacyNewsPool.PROCESSED_DB);
         showedCrawl = 0;
         try {
             for (int c = 0; c < availableNews; c++) {
                 record = yacyCore.newsPool.get(yacyNewsPool.PROCESSED_DB, c);
                 if (record == null) continue;
                 if (record.category().equals("crwlstrt")) {
                     peer = yacyCore.seedDB.get(record.originator());
                     if (peer == null) peername = record.originator(); else peername = peer.getName();
                     prop.put("otherCrawlStartFinished_" + showedCrawl + "_dark", ((dark) ? 1 : 0));
                     prop.put("otherCrawlStartFinished_" + showedCrawl + "_cre", record.created());
                     prop.put("otherCrawlStartFinished_" + showedCrawl + "_peername", wikiCode.replaceHTML(peername));
                     prop.put("otherCrawlStartFinished_" + showedCrawl + "_startURL", wikiCode.replaceHTML(record.attributes().get("startURL").toString()));
                     prop.put("otherCrawlStartFinished_" + showedCrawl + "_intention", wikiCode.replaceHTML(record.attributes().get("intention").toString()));
                     prop.put("otherCrawlStartFinished_" + showedCrawl + "_generalDepth", record.attributes().get("generalDepth"));
                     prop.put("otherCrawlStartFinished_" + showedCrawl + "_crawlingQ", (record.attributes().get("crawlingQ").equals("true")) ? 1 : 0);
                     showedCrawl++;
                     if (showedCrawl > 20) break;
                 }
                 
             }
         } catch (IOException e) {}
         prop.put("otherCrawlStartFinished", showedCrawl);
 
         
         // remote crawl peers
         if (yacyCore.seedDB == null) {
             //table += "Sorry, cannot show any crawl output now because the system is not completely initialised. Please re-try.";
             prop.put("error", 3);
         } else {
             Enumeration crawlavail = yacyCore.dhtAgent.getAcceptRemoteCrawlSeeds(plasmaURL.dummyHash, true);
             Enumeration crawlpendi = yacyCore.dhtAgent.getAcceptRemoteCrawlSeeds(plasmaURL.dummyHash, false);
             if ((!(crawlavail.hasMoreElements())) && (!(crawlpendi.hasMoreElements()))) {
                 prop.put("remoteCrawlPeers", 0); //no peers availible
             } else {
                 prop.put("remoteCrawlPeers", 1);
                 int maxcount = 100;
                 int availcount = 0;
                 yacySeed seed;
                 while ((availcount < maxcount) && (crawlavail.hasMoreElements())) {
                     seed = (yacySeed) crawlavail.nextElement();
                     prop.put("remoteCrawlPeers_available_" + availcount + "_name", seed.getName());
                     prop.put("remoteCrawlPeers_available_" + availcount + "_due", (yacyCore.yacyTime() - seed.available));
                     availcount++;
                 }
                 prop.put("remoteCrawlPeers_available", availcount);
                 int pendicount = 0;
                 while ((pendicount < maxcount) && (crawlpendi.hasMoreElements())) {
                     seed = (yacySeed) crawlpendi.nextElement();
                     prop.put("remoteCrawlPeers_busy_" + pendicount + "_name", seed.getName());
                     prop.put("remoteCrawlPeers_busy_" + pendicount + "_due", (yacyCore.yacyTime() - seed.available));
                     pendicount++;
                 }
                 prop.put("remoteCrawlPeers_busy", pendicount);
                 prop.put("remoteCrawlPeers_num", (availcount + pendicount));
             }
 
         }
         
         
         prop.put("crawler-paused",(switchboard.crawlJobIsPaused(plasmaSwitchboard.CRAWLJOB_LOCAL_CRAWL))?0:1);
         
         // return rewrite properties
         return prop;
     }
     
 }
 
 
 
