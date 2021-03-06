 package org.klomp.snark.web;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.text.Collator;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.TreeSet;
 
 import javax.servlet.ServletConfig;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import net.i2p.I2PAppContext;
 import net.i2p.data.Base64;
 import net.i2p.data.DataHelper;
 import net.i2p.util.FileUtil;
 import net.i2p.util.I2PAppThread;
 import net.i2p.util.Log;
 
 import org.klomp.snark.MetaInfo;
 import org.klomp.snark.Peer;
 import org.klomp.snark.Snark;
 import org.klomp.snark.SnarkManager;
 import org.klomp.snark.Storage;
 import org.klomp.snark.TrackerClient;
 
 /**
  *
  */
 public class I2PSnarkServlet extends HttpServlet {
     private I2PAppContext _context;
     private Log _log;
     private SnarkManager _manager;
     private static long _nonce;
     
     public static final String PROP_CONFIG_FILE = "i2psnark.configFile";
     
     @Override
     public void init(ServletConfig cfg) throws ServletException {
         super.init(cfg);
         _context = I2PAppContext.getGlobalContext();
         _log = _context.logManager().getLog(I2PSnarkServlet.class);
         _nonce = _context.random().nextLong();
         _manager = SnarkManager.instance();
         String configFile = _context.getProperty(PROP_CONFIG_FILE);
         if ( (configFile == null) || (configFile.trim().length() <= 0) )
             configFile = "i2psnark.config";
         _manager.loadConfig(configFile);
         _manager.start();
     }
     
     @Override
     public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
         req.setCharacterEncoding("UTF-8");
         resp.setCharacterEncoding("UTF-8");
         resp.setContentType("text/html; charset=UTF-8");
         /** dl, ul, down rate, up rate, peers, size */
         final long stats[] = {0,0,0,0,0,0};
         
         String nonce = req.getParameter("nonce");
         if ( (nonce != null) && (nonce.equals(String.valueOf(_nonce))) )
             processRequest(req);
         
         String peerParam = req.getParameter("p");
         String peerString;
         if (peerParam == null) {
             peerString = "";
         } else {
             peerString = "?p=" + peerParam;
         }
 
         PrintWriter out = resp.getWriter();
         out.write("<html>\n" +
                   "<head>\n" +
                   "<title>");
         out.write(_("I2PSnark - Anonymous BitTorrent Client"));
         out.write("</title>\n");
                                          
         // we want it to go to the base URI so we don't refresh with some funky action= value
         out.write("<meta http-equiv=\"refresh\" content=\"60;" + req.getRequestURI() + peerString + "\">\n");
         out.write(HEADER);
         out.write("</head><body>");
         out.write("<center>");
         out.write("<div class=\"snarknavbar\"><a href=\"" + req.getRequestURI() + peerString + "\" title=\"");
         out.write(_("Refresh page"));
         out.write("\" class=\"snarkRefresh\">");
         out.write(_("I2PSnark"));
         out.write("</a> <a href=\"http://forum.i2p/viewforum.php?f=21\" class=\"snarkRefresh\" target=\"_blank\">");
         out.write(_("Forum"));
         out.write("</a>\n");
 
         Map trackers = _manager.getTrackers();
         for (Iterator iter = trackers.entrySet().iterator(); iter.hasNext(); ) {
             Map.Entry entry = (Map.Entry)iter.next();
             String name = (String)entry.getKey();
             String baseURL = (String)entry.getValue();
             int e = baseURL.indexOf('=');
             if (e < 0)
                 continue;
             baseURL = baseURL.substring(e + 1);
             out.write(" <a href=\"" + baseURL + "\" class=\"snarkRefresh\" target=\"_blank\">" + name + "</a>");
         }
         out.write("</div>\n");
         out.write("<div class=\"page\"><div class=\"mainsection\"><div class=\"snarkMessages\"><table><tr><td align=\"left\"><pre>");
         List msgs = _manager.getMessages();
         for (int i = msgs.size()-1; i >= 0; i--) {
             String msg = (String)msgs.get(i);
             out.write(msg + "\n");
         }
         out.write("</pre></td></tr></table></div>");
 
         List snarks = getSortedSnarks(req);
         String uri = req.getRequestURI();
         out.write(TABLE_HEADER);
         out.write(_("Status"));
         if (_manager.util().connected() && snarks.size() > 0) {
             out.write(" (<a href=\"");
             out.write(req.getRequestURI());
             if (peerParam != null) {
                 out.write("\">");
                 out.write(_("Hide Peers"));
             } else {
                 out.write("?p=1\">");
                 out.write(_("Show Peers"));
             }
             out.write("</a>)<br>\n");
         }
         out.write("</th>\n<th align=\"left\">");
         out.write(_("Torrent"));
         out.write("</th>\n<th align=\"center\">");
         out.write(_("ETA"));
         out.write("</th>\n<th align=\"right\">");
         out.write(_("Downloaded"));
         out.write("</th>\n<th align=\"right\">");
         out.write(_("Uploaded"));
         out.write("</th>\n<th align=\"right\">");
         out.write(_("Down Rate"));
         out.write("</th>\n<th align=\"right\">");
         out.write(_("Up Rate"));
         out.write("</th>\n");
     
         out.write("<th align=\"center\">");
         if (_manager.util().connected()) {
             out.write("<a href=\"" + uri + "?action=StopAll&nonce=" + _nonce +
                       "\" title=\"");
             out.write(_("Stop all torrents and the I2P tunnel"));
             out.write("\">");
             out.write(_("Stop All"));
             out.write("</a>");
         } else if (snarks.size() > 0) {
             out.write("<a href=\"" + uri + "?action=StartAll&nonce=" + _nonce +
                       "\" title=\"");
             out.write(_("Start all torrents and the I2P tunnel"));
             out.write("\">");
             out.write(_("Start All"));
             out.write("</a>");
         } else {
             out.write("&nbsp;");
         }
         out.write("</th></tr></thead>\n");
         for (int i = 0; i < snarks.size(); i++) {
             Snark snark = (Snark)snarks.get(i);
             boolean showDebug = "2".equals(peerParam);
             boolean showPeers = showDebug || "1".equals(peerParam) || Base64.encode(snark.meta.getInfoHash()).equals(peerParam);
             displaySnark(out, snark, uri, i, stats, showPeers, showDebug);
         }
 
         if (snarks.size() <= 0) {
             out.write("<tr class=\"snarkTorrentEven\">" +
                       "<td class=\"snarkTorrentEven\" align=\"center\"" +
                       " colspan=\"8\"><i>");
             out.write(_("No torrents loaded."));
             out.write("</i></td></tr>\n");
         } else if (snarks.size() > 1) {
             out.write("<tfoot><tr>\n" +
                       "    <th align=\"left\" colspan=\"2\">");
             out.write(_("Totals"));
            out.write(" {");
             out.write(_("{0} torrents", snarks.size()));
             out.write(", ");
             out.write(DataHelper.formatSize(stats[5]) + "B, ");
             out.write(_("{0} connected peers", stats[4]));
             out.write(")</th>\n" +
                       "    <th>&nbsp;</th>\n" +
                       "    <th align=\"right\">" + formatSize(stats[0]) + "</th>\n" +
                       "    <th align=\"right\">" + formatSize(stats[1]) + "</th>\n" +
                       "    <th align=\"right\">" + formatSize(stats[2]) + "ps</th>\n" +
                       "    <th align=\"right\">" + formatSize(stats[3]) + "ps</th>\n" +
                       "    <th>&nbsp;</th></tr>\n" +
                       "</tfoot>\n");
         }
         
         out.write(TABLE_FOOTER);
 
         writeAddForm(out, req);
         if (true) // seeding needs to register the torrent first, so we can't start it automatically (boo, hiss)
             writeSeedForm(out, req);
         writeConfigForm(out, req);
         out.write(FOOTER);
     }
     
     /**
      * Do what they ask, adding messages to _manager.addMessage as necessary
      */
     private void processRequest(HttpServletRequest req) {
         String action = req.getParameter("action");
         if (action == null) {
             // noop
        } else if (_("Add torrent").equals(action)) {
             String newFile = req.getParameter("newFile");
             String newURL = req.getParameter("newURL");
             // NOTE - newFile currently disabled in HTML form - see below
             File f = null;
             if ( (newFile != null) && (newFile.trim().length() > 0) )
                 f = new File(newFile.trim());
             if ( (f != null) && (!f.exists()) ) {
                 _manager.addMessage(_("Torrent file {0} does not exist", newFile));
             }
             if ( (f != null) && (f.exists()) ) {
                 File local = new File(_manager.getDataDir(), f.getName());
                 String canonical = null;
                 try {
                     canonical = local.getCanonicalPath();
                     
                     if (local.exists()) {
                         if (_manager.getTorrent(canonical) != null)
                             _manager.addMessage(_("Torrent already running: {0}", newFile));
                         else
                             _manager.addMessage(_("Torrent already in the queue: {0}", newFile));
                     } else {
                         boolean ok = FileUtil.copy(f.getAbsolutePath(), local.getAbsolutePath(), true);
                         if (ok) {
                             _manager.addMessage(_("Copying torrent to {0}", local.getAbsolutePath()));
                             _manager.addTorrent(canonical);
                         } else {
                             _manager.addMessage(_("Unable to copy the torrent to {0}", local.getAbsolutePath()) + ' ' + _("from {0}", f.getAbsolutePath()));
                         }
                     }
                 } catch (IOException ioe) {
                     _log.warn("hrm: " + local, ioe);
                 }
             } else if (newURL != null) {
                 if (newURL.startsWith("http://")) {
                     _manager.addMessage(_("Fetching {0}", newURL));
                     I2PAppThread fetch = new I2PAppThread(new FetchAndAdd(_manager, newURL), "Fetch and add");
                     fetch.start();
                 } else {
                     _manager.addMessage(_("Invalid URL - must start with http://"));
                 }
             } else {
                 // no file or URL specified
             }
        } else if (_("Stop").equals(action)) {
             String torrent = req.getParameter("torrent");
             if (torrent != null) {
                 byte infoHash[] = Base64.decode(torrent);
                 if ( (infoHash != null) && (infoHash.length == 20) ) { // valid sha1
                     for (Iterator iter = _manager.listTorrentFiles().iterator(); iter.hasNext(); ) {
                         String name = (String)iter.next();
                         Snark snark = _manager.getTorrent(name);
                         if ( (snark != null) && (DataHelper.eq(infoHash, snark.meta.getInfoHash())) ) {
                             _manager.stopTorrent(name, false);
                             break;
                         }
                     }
                 }
             }
        } else if (_("Start").equals(action)) {
             String torrent = req.getParameter("torrent");
             if (torrent != null) {
                 byte infoHash[] = Base64.decode(torrent);
                 if ( (infoHash != null) && (infoHash.length == 20) ) { // valid sha1
                     for (Iterator iter = _manager.listTorrentFiles().iterator(); iter.hasNext(); ) {
                         String name = (String)iter.next();
                         Snark snark = _manager.getTorrent(name);
                         if ( (snark != null) && (DataHelper.eq(infoHash, snark.meta.getInfoHash())) ) {
                             snark.startTorrent();
                             _manager.addMessage(_("Starting up torrent {0}", name));
                             break;
                         }
                     }
                 }
             }
         } else if ("Remove".equals(action)) {
             String torrent = req.getParameter("torrent");
             if (torrent != null) {
                 byte infoHash[] = Base64.decode(torrent);
                 if ( (infoHash != null) && (infoHash.length == 20) ) { // valid sha1
                     for (Iterator iter = _manager.listTorrentFiles().iterator(); iter.hasNext(); ) {
                         String name = (String)iter.next();
                         Snark snark = _manager.getTorrent(name);
                         if ( (snark != null) && (DataHelper.eq(infoHash, snark.meta.getInfoHash())) ) {
                             _manager.stopTorrent(name, true);
                             // should we delete the torrent file?
                             // yeah, need to, otherwise it'll get autoadded again (at the moment
                             File f = new File(name);
                             f.delete();
                             _manager.addMessage(_("Torrent file deleted: {0}", f.getAbsolutePath()));
                             break;
                         }
                     }
                 }
             }
         } else if ("Delete".equals(action)) {
             String torrent = req.getParameter("torrent");
             if (torrent != null) {
                 byte infoHash[] = Base64.decode(torrent);
                 if ( (infoHash != null) && (infoHash.length == 20) ) { // valid sha1
                     for (Iterator iter = _manager.listTorrentFiles().iterator(); iter.hasNext(); ) {
                         String name = (String)iter.next();
                         Snark snark = _manager.getTorrent(name);
                         if ( (snark != null) && (DataHelper.eq(infoHash, snark.meta.getInfoHash())) ) {
                             _manager.stopTorrent(name, true);
                             File f = new File(name);
                             f.delete();
                             _manager.addMessage(_("Torrent file deleted: {0}", f.getAbsolutePath()));
                             List files = snark.meta.getFiles();
                             String dataFile = snark.meta.getName();
                             f = new File(_manager.getDataDir(), dataFile);
                             if (files == null) { // single file torrent
                                 if (f.delete())
                                     _manager.addMessage(_("Data file deleted: {0}", f.getAbsolutePath()));
                                 else
                                     _manager.addMessage(_("Data file could not be deleted: {0}", f.getAbsolutePath()));
                                 break;
                             }
                             for (int i = 0; i < files.size(); i++) { // pass 1 delete files
                                 // multifile torrents have the getFiles() return lists of lists of filenames, but
                                 // each of those lists just contain a single file afaict...
                                 File df = Storage.getFileFromNames(f, (List) files.get(i));
                                 if (df.delete())
                                     _manager.addMessage(_("Data file deleted: {0}", df.getAbsolutePath()));
                                 else
                                     _manager.addMessage(_("Data file could not be deleted: {0}", df.getAbsolutePath()));
                             }
                             for (int i = files.size() - 1; i >= 0; i--) { // pass 2 delete dirs - not foolproof,
                                                                           // we could sort and do a strict bottom-up
                                 File df = Storage.getFileFromNames(f, (List) files.get(i));
                                 df = df.getParentFile();
                                 if (df == null || !df.exists())
                                     continue;
                                 if(df.delete())
                                     _manager.addMessage(_("Data dir deleted: {0}", df.getAbsolutePath()));
                             }
                             break;
                         }
                     }
                 }
             }
        } else if (_("Save configuration").equals(action)) {
             String dataDir = req.getParameter("dataDir");
             boolean autoStart = req.getParameter("autoStart") != null;
             String seedPct = req.getParameter("seedPct");
             String eepHost = req.getParameter("eepHost");
             String eepPort = req.getParameter("eepPort");
             String i2cpHost = req.getParameter("i2cpHost");
             String i2cpPort = req.getParameter("i2cpPort");
             String i2cpOpts = req.getParameter("i2cpOpts");
             String upLimit = req.getParameter("upLimit");
             String upBW = req.getParameter("upBW");
             boolean useOpenTrackers = req.getParameter("useOpenTrackers") != null;
             String openTrackers = req.getParameter("openTrackers");
             _manager.updateConfig(dataDir, autoStart, seedPct, eepHost, eepPort, i2cpHost, i2cpPort, i2cpOpts, upLimit, upBW, useOpenTrackers, openTrackers);
        } else if (_("Create torrent").equals(action)) {
             String baseData = req.getParameter("baseFile");
             if (baseData != null && baseData.trim().length() > 0) {
                 File baseFile = new File(_manager.getDataDir(), baseData);
                 String announceURL = req.getParameter("announceURL");
                 String announceURLOther = req.getParameter("announceURLOther");
                 if ( (announceURLOther != null) && (announceURLOther.trim().length() > "http://.i2p/announce".length()) )
                     announceURL = announceURLOther;
 
                 if (announceURL == null || announceURL.length() <= 0)
                     _manager.addMessage(_("Error creating torrent - you must select a tracker"));
                 else if (baseFile.exists()) {
                     try {
                         Storage s = new Storage(_manager.util(), baseFile, announceURL, null);
                         s.create();
                         s.close(); // close the files... maybe need a way to pass this Storage to addTorrent rather than starting over
                         MetaInfo info = s.getMetaInfo();
                         File torrentFile = new File(baseFile.getParent(), baseFile.getName() + ".torrent");
                         if (torrentFile.exists())
                             throw new IOException("Cannot overwrite an existing .torrent file: " + torrentFile.getPath());
                         _manager.saveTorrentStatus(info, s.getBitField()); // so addTorrent won't recheck
                         // DirMonitor could grab this first, maybe hold _snarks lock?
                         FileOutputStream out = new FileOutputStream(torrentFile);
                         out.write(info.getTorrentData());
                         out.close();
                         _manager.addMessage(_("Torrent created for \"{0}\"", baseFile.getName()) + ": " + torrentFile.getAbsolutePath());
                         // now fire it up, but don't automatically seed it
                         _manager.addTorrent(torrentFile.getCanonicalPath(), true);
                         _manager.addMessage(_("Many I2P trackers require you to register new torrents before seeding - please do so before starting \"{0}\"", baseFile.getName()));
                     } catch (IOException ioe) {
                         _manager.addMessage(_("Error creating a torrent for \"{0}\"", baseFile.getAbsolutePath()) + ": " + ioe.getMessage());
                     }
                 } else {
                     _manager.addMessage(_("Cannot create a torrent for the nonexistent data: {0}", baseFile.getAbsolutePath()));
                 }
             } else {
                 _manager.addMessage(_("Error creating torrent - you must enter a file or directory"));
             }
         } else if ("StopAll".equals(action)) {
             _manager.addMessage(_("Stopping all torrents and closing the I2P tunnel."));
             List snarks = getSortedSnarks(req);
             for (int i = 0; i < snarks.size(); i++) {
                 Snark snark = (Snark)snarks.get(i);
                 if (!snark.stopped)
                     _manager.stopTorrent(snark.torrent, false);
             }
             if (_manager.util().connected()) {
                 _manager.util().disconnect();
                 _manager.addMessage(_("I2P tunnel closed."));
             }
         } else if ("StartAll".equals(action)) {
             _manager.addMessage(_("Opening the I2P tunnel and starting all torrents."));
             List snarks = getSortedSnarks(req);
             for (int i = 0; i < snarks.size(); i++) {
                 Snark snark = (Snark)snarks.get(i);
                 if (snark.stopped)
                     snark.startTorrent();
             }
         }
     }
     
     private List getSortedSnarks(HttpServletRequest req) {
         Set files = _manager.listTorrentFiles();
         TreeSet fileNames = new TreeSet(Collator.getInstance()); // sorts it alphabetically
         fileNames.addAll(files);
         ArrayList rv = new ArrayList(fileNames.size());
         for (Iterator iter = fileNames.iterator(); iter.hasNext(); ) {
             String name = (String)iter.next();
             Snark snark = _manager.getTorrent(name);
             if (snark != null)
                 rv.add(snark);
         }
         return rv;
     }
 
     private static final int MAX_DISPLAYED_FILENAME_LENGTH = 44;
     private static final int MAX_DISPLAYED_ERROR_LENGTH = 40;
     private void displaySnark(PrintWriter out, Snark snark, String uri, int row, long stats[], boolean showPeers, boolean showDebug) throws IOException {
         String filename = snark.torrent;
         File f = new File(filename);
         filename = f.getName(); // the torrent may be the canonical name, so lets just grab the local name
         int i = filename.lastIndexOf(".torrent");
         if (i > 0)
             filename = filename.substring(0, i);
         if (filename.length() > MAX_DISPLAYED_FILENAME_LENGTH)
             filename = filename.substring(0, MAX_DISPLAYED_FILENAME_LENGTH) + "&hellip;";
         long total = snark.meta.getTotalLength();
         // Early typecast, avoid possibly overflowing a temp integer
         long remaining = (long) snark.storage.needed() * (long) snark.meta.getPieceLength(0); 
         if (remaining > total)
             remaining = total;
         long downBps = 0;
         long upBps = 0;
         if (snark.coordinator != null) {
             downBps = snark.coordinator.getDownloadRate();
             upBps = snark.coordinator.getUploadRate();
         }
         long remainingSeconds;
         if (downBps > 0)
             remainingSeconds = remaining / downBps;
         else
             remainingSeconds = -1;
         boolean isRunning = !snark.stopped;
         long uploaded = 0;
         if (snark.coordinator != null) {
             uploaded = snark.coordinator.getUploaded();
             stats[0] += snark.coordinator.getDownloaded();
         }
         stats[1] += uploaded;
         if (isRunning) {
             stats[2] += downBps;
             stats[3] += upBps;
         }
         stats[5] += total;
         
         boolean isValid = snark.meta != null;
         boolean singleFile = snark.meta.getFiles() == null;
         
         String err = null;
         int curPeers = 0;
         int knownPeers = 0;
         if (snark.coordinator != null) {
             err = snark.coordinator.trackerProblems;
             curPeers = snark.coordinator.getPeerCount();
             stats[4] += curPeers;
             knownPeers = snark.coordinator.trackerSeenPeers;
         }
         
         String statusString = _("Unknown");
         if (err != null) {
             if (isRunning && curPeers > 0 && !showPeers)
                 statusString = "<a title=\"" + err + "\">" + _("TrackerErr") + "</a> (" +
                                curPeers + "/" + knownPeers +
                                " <a href=\"" + uri + "?p=" + Base64.encode(snark.meta.getInfoHash()) + "\">" + _("peers") + "</a>)";
             else if (isRunning)
                 statusString = "<a title=\"" + err + "\">" + _("TrackerErr") + " (" + curPeers + '/' + knownPeers + ' ' + _("peers") + ')';
             else {
                 if (err.length() > MAX_DISPLAYED_ERROR_LENGTH)
                     err = err.substring(0, MAX_DISPLAYED_ERROR_LENGTH) + "&hellip;";
                 statusString = _("TrackerErr") + "<br>(" + err + ")";
             }
         } else if (remaining <= 0) {
             if (isRunning && curPeers > 0 && !showPeers)
                 statusString = _("Seeding") + " (" +
                                curPeers + '/' + knownPeers +
                                " <a href=\"" + uri + "?p=" + Base64.encode(snark.meta.getInfoHash()) + "\">" + _("peers") + "</a>)";
             else if (isRunning)
                 statusString = _("Seeding") + " (" + curPeers + "/" + knownPeers + ' ' + _("peers") + ')';
             else
                 statusString = _("Complete");
         } else {
             if (isRunning && curPeers > 0 && downBps > 0 && !showPeers)
                 statusString = _("OK") + " (" +
                                curPeers + "/" + knownPeers +
                                " <a href=\"" + uri + "?p=" + Base64.encode(snark.meta.getInfoHash()) + "\">" + _("peers") + "</a>)";
             else if (isRunning && curPeers > 0 && downBps > 0)
                 statusString = _("OK") + " (" + curPeers + "/" + knownPeers + ' ' + _("peers") + ')';
             else if (isRunning && curPeers > 0 && !showPeers)
                 statusString = _("Stalled") + " (" +
                                curPeers + '/' + knownPeers +
                                " <a href=\"" + uri + "?p=" + Base64.encode(snark.meta.getInfoHash()) + "\">" + _("peers") + "</a>)";
             else if (isRunning && curPeers > 0)
                 statusString = _("Stalled") + " (" + curPeers + '/' + knownPeers + ' ' + _("peers") + ')';
             else if (isRunning)
                 statusString = _("No Peers") + " (0/" + knownPeers + ')';
             else
                 statusString = _("Stopped");
         }
         
         String rowClass = (row % 2 == 0 ? "snarkTorrentEven" : "snarkTorrentOdd");
         out.write("<tr class=\"" + rowClass + "\">");
         out.write("<td align=\"center\" class=\"snarkTorrentStatus " + rowClass + "\">");
         out.write(statusString + "</td>\n\t");
         out.write("<td align=\"left\" class=\"snarkTorrentName " + rowClass + "\">");
         
         if (remaining == 0) {
             out.write("<a href=\"" + _manager.linkPrefix() + snark.meta.getName() 
                       + "\" title=\"");
             if (snark.meta.getFiles() != null)
                 out.write(_("View files"));
             else
                 out.write(_("Open file"));
             out.write("\">");
         }
         out.write(filename);
         if (remaining == 0)
             out.write("</a>");
         // temporarily hardcoded for postman* and anonymity, requires bytemonsoon patch for lookup by info_hash
         String announce = snark.meta.getAnnounce();
         if (announce.startsWith("http://YRgrgTLG") || announce.startsWith("http://8EoJZIKr") ||
             announce.startsWith("http://lnQ6yoBT") || announce.startsWith("http://tracker2.postman.i2p/")) {
             Map trackers = _manager.getTrackers();
             for (Iterator iter = trackers.entrySet().iterator(); iter.hasNext(); ) {
                 Map.Entry entry = (Map.Entry)iter.next();
                 String name = (String)entry.getKey();
                 String baseURL = (String)entry.getValue();
                 if (!(baseURL.startsWith(announce) || // vvv hack for non-b64 announce in list vvv
                       (announce.startsWith("http://lnQ6yoBT") && baseURL.startsWith("http://tracker2.postman.i2p/"))))
                     continue;
                 int e = baseURL.indexOf('=');
                 if (e < 0)
                     continue;
                 baseURL = baseURL.substring(e + 1);
                 out.write("&nbsp;&nbsp;&nbsp;[<a href=\"" + baseURL + "details.php?dllist=1&filelist=1&info_hash=");
                 out.write(TrackerClient.urlencode(snark.meta.getInfoHash()));
                 out.write("\" title=\"" + name + ' ' + _("Tracker") + "\">");
                 out.write(_("Details"));
                 out.write("</a>]");
                 break;
             }
         }
         out.write("</td>\n\t");
         
         out.write("<td align=\"center\" class=\"snarkTorrentETA " + rowClass + "\">");
         if(isRunning && remainingSeconds > 0)
             out.write(DataHelper.formatDuration(remainingSeconds*1000)); // (eta 6h)
         out.write("</td>\n\t");
         out.write("<td align=\"right\" class=\"snarkTorrentDownloaded " + rowClass + "\">");
         if (remaining > 0)
             out.write(formatSize(total-remaining) + "/" + formatSize(total)); // 18MB/3GB
         else
             out.write(formatSize(total)); // 3GB
         out.write("</td>\n\t");
         out.write("<td align=\"right\" class=\"snarkTorrentUploaded " + rowClass 
                   + "\">" + formatSize(uploaded) + "</td>\n\t");
         out.write("<td align=\"right\" class=\"snarkTorrentRate\">");
         if(isRunning && remaining > 0)
             out.write(formatSize(downBps) + "ps");
         out.write("</td>\n\t");
         out.write("<td align=\"right\" class=\"snarkTorrentRate\">");
         if(isRunning)
             out.write(formatSize(upBps) + "ps");
         out.write("</td>\n\t");
         out.write("<td align=\"center\" class=\"snarkTorrentAction " + rowClass + "\">");
         String parameters = "&nonce=" + _nonce + "&torrent=" + Base64.encode(snark.meta.getInfoHash());
         if (showPeers)
             parameters = parameters + "&p=1";
         if (isRunning) {
             out.write("<a href=\"" + uri + "?action=Stop" + parameters
                       + "\" title=\"");
             out.write(_("Stop the torrent"));
             out.write("\">");
             out.write(_("Stop"));
             out.write("</a>");
         } else {
             if (isValid) {
                 out.write("<a href=\"" + uri + "?action=Start" + parameters
                           + "\" title=\"");
                 out.write(_("Start the torrent"));
                 out.write("\">");
                 out.write(_("Start"));
                 out.write("</a>\n");
             }
             out.write("<a href=\"" + uri + "?action=Remove" + parameters
                       + "\" title=\"");
             out.write(_("Remove the torrent from the active list, deleting the .torrent file"));
             out.write("\">");
             out.write(_("Remove"));
             out.write("</a><br>");
             out.write("<a href=\"" + uri + "?action=Delete" + parameters
                       + "\" title=\"");
             out.write(_("Delete the .torrent file and the associated data file(s)"));
             out.write("\">");
             out.write(_("Delete"));
             out.write("</a>");
         }
         out.write("</td>\n</tr>\n");
 
         if(showPeers && isRunning && curPeers > 0) {
             List peers = snark.coordinator.peerList();
             Iterator it = peers.iterator();
             while (it.hasNext()) {
                 Peer peer = (Peer)it.next();
                 if (!peer.isConnected())
                     continue;
                 out.write("<tr class=\"" + rowClass + "\">");
                 out.write("<td align=\"center\" class=\"snarkTorrentStatus " + rowClass + "\">");
                 out.write("</td>\n\t");
                 out.write("<td align=\"right\" class=\"snarkTorrentStatus " + rowClass + "\">");
                 String ch = peer.toString().substring(0, 4);
                 String client;
                 if ("AwMD".equals(ch))
                     client = _("I2PSnark");
                 else if ("BFJT".equals(ch))
                     client = "I2PRufus";
                 else if ("TTMt".equals(ch))
                     client = "I2P-BT";
                 else if ("LUFa".equals(ch))
                     client = "Azureus";
                 else if ("CwsL".equals(ch))
                     client = "I2PSnarkXL";
                 else if ("ZV".equals(ch.substring(2,4)))
                     client = "Robert";
                 else if ("VUZP".equals(ch))
                     client = "Robert";
                 else
                     client = _("Unknown") + " (" + ch + ')';
                 out.write(client + "&nbsp;&nbsp;" + peer.toString().substring(5, 9));
                 if (showDebug)
                     out.write(" inactive " + (peer.getInactiveTime() / 1000) + "s");
                 out.write("</td>\n\t");
                 out.write("<td class=\"snarkTorrentStatus " + rowClass + "\">");
                 out.write("</td>\n\t");
                 out.write("<td align=\"right\" class=\"snarkTorrentStatus " + rowClass + "\">");
                 float pct = (float) (100.0 * (float) peer.completed() / snark.meta.getPieces());
                 if (pct == 100.0)
                     out.write(_("Seed"));
                 else {
                     String ps = String.valueOf(pct);
                     if (ps.length() > 5)
                         ps = ps.substring(0, 5);
                     out.write(ps + "%");
                 }
                 out.write("</td>\n\t");
                 out.write("<td class=\"snarkTorrentStatus " + rowClass + "\">");
                 out.write("</td>\n\t");
                 out.write("<td align=\"right\" class=\"snarkTorrentStatus " + rowClass + "\">");
                 if (remaining > 0) {
                     if (peer.isInteresting() && !peer.isChoked()) {
                         out.write("<font color=#008000>");
                         out.write(formatSize(peer.getDownloadRate()) + "ps</font>");
                     } else {
                         out.write("<font color=#a00000><a title=\"");
                         if (!peer.isInteresting())
                             out.write(_("Uninteresting (The peer has no pieces we need)"));
                         else
                             out.write(_("Choked (The peer is not allowing us to request pieces)"));
                         out.write("\">");
                         out.write(formatSize(peer.getDownloadRate()) + "ps</a></font>");
                     }
                 }
                 out.write("</td>\n\t");
                 out.write("<td align=\"right\" class=\"snarkTorrentStatus " + rowClass + "\">");
                 if (pct != 100.0) {
                     if (peer.isInterested() && !peer.isChoking()) {
                         out.write("<font color=#008000>");
                         out.write(formatSize(peer.getUploadRate()) + "ps</font>");
                     } else {
                         out.write("<font color=#a00000><a title=\"");
                         if (!peer.isInterested())
                             out.write(_("Uninterested (We have no pieces the peer needs)"));
                         else
                             out.write(_("Choking (We are not allowing the peer to request pieces)"));
                         out.write("\">");
                         out.write(formatSize(peer.getUploadRate()) + "ps</a></font>");
                     }
                 }
                 out.write("</td>\n\t");
                 out.write("<td class=\"snarkTorrentStatus " + rowClass + "\">");
                 out.write("</td></tr>\n\t");
                 if (showDebug)
                     out.write("<tr><td colspan=\"8\" align=\"right\" class=\"snarkTorrentStatus " + rowClass + "\">" + peer.getSocket() + "</td></tr>");
             }
         }
     }
     
     private void writeAddForm(PrintWriter out, HttpServletRequest req) throws IOException {
         String uri = req.getRequestURI();
         String newURL = req.getParameter("newURL");
         if ( (newURL == null) || (newURL.trim().length() <= 0) ) newURL = "";
         String newFile = req.getParameter("newFile");
         if ( (newFile == null) || (newFile.trim().length() <= 0) ) newFile = "";
         
         out.write("<span class=\"snarkNewTorrent\">\n");
         // *not* enctype="multipart/form-data", so that the input type=file sends the filename, not the file
         out.write("<form action=\"" + uri + "\" method=\"POST\">\n");
         out.write("<input type=\"hidden\" name=\"nonce\" value=\"" + _nonce + "\" >\n");
         out.write("<div class=\"addtorrentsection\"><span class=\"snarkConfigTitle\">");
         out.write(_("Add Torrent"));
         out.write("</span><br>\n<table border=\"0\"><tr><td>");
         out.write(_("From URL"));
         out.write(":<td><input type=\"text\" name=\"newURL\" size=\"80\" value=\"" + newURL + "\" > \n");
         // not supporting from file at the moment, since the file name passed isn't always absolute (so it may not resolve)
         //out.write("From file: <input type=\"file\" name=\"newFile\" size=\"50\" value=\"" + newFile + "\" /><br>\n");
         out.write("<tr><td>&nbsp;<td><input type=\"submit\" value=\"");
         out.write(_("Add torrent"));
        out.write("\" name=\"action\" ><br>\n");
         out.write("<tr><td>&nbsp;<td><span class=\"snarkAddInfo\">");
         out.write(_("Alternately, you can copy .torrent files to the directory {0}.", _manager.getDataDir().getAbsolutePath()));
         out.write("\n");
         out.write(_("Removing a .torrent file will cause the torrent to stop."));
         out.write("<br></span></table>\n");
         out.write("</form>\n</span></div>");  
     }
     
     private void writeSeedForm(PrintWriter out, HttpServletRequest req) throws IOException {
         String uri = req.getRequestURI();
         String baseFile = req.getParameter("baseFile");
         if (baseFile == null)
             baseFile = "";
         
         out.write("<div class=\"newtorrentsection\"><span class=\"snarkNewTorrent\">\n");
         // *not* enctype="multipart/form-data", so that the input type=file sends the filename, not the file
         out.write("<form action=\"" + uri + "\" method=\"POST\">\n");
         out.write("<input type=\"hidden\" name=\"nonce\" value=\"" + _nonce + "\" >\n");
         out.write("<span class=\"snarkConfigTitle\">");
         out.write(_("Create Torrent"));
         out.write("</span><br>\n<table border=\"0\"><tr><td>");
         //out.write("From file: <input type=\"file\" name=\"newFile\" size=\"50\" value=\"" + newFile + "\" /><br>\n");
         out.write(_("Data to seed"));
         out.write(":<td>" + _manager.getDataDir().getAbsolutePath() + File.separatorChar 
                   + "<input type=\"text\" name=\"baseFile\" size=\"20\" value=\"" + baseFile 
                   + "\" title=\"");
         out.write(_("File or directory to seed (must be within the specified path)"));
         out.write("\" ><tr><td>\n");
         out.write(_("Tracker"));
         out.write(":<td><select name=\"announceURL\"><option value=\"\">");
         out.write(_("Select a tracker"));
         out.write("</option>\n");
         Map trackers = _manager.getTrackers();
         for (Iterator iter = trackers.entrySet().iterator(); iter.hasNext(); ) {
             Map.Entry entry = (Map.Entry)iter.next();
             String name = (String)entry.getKey();
             String announceURL = (String)entry.getValue();
             int e = announceURL.indexOf('=');
             if (e > 0)
                 announceURL = announceURL.substring(0, e);
             out.write("\t<option value=\"" + announceURL + "\">" + name + "</option>\n");
         }
         out.write("</select>\n");
         out.write(_("or"));
         out.write("<tr><td>&nbsp;<td><input type=\"text\" name=\"announceURLOther\" size=\"50\" value=\"http://\" " +
                   "title=\"");
         out.write(_("Specify custom tracker announce URL"));
         out.write("\" > ");
         out.write("<tr><td>&nbsp;<td><input type=\"submit\" value=\"");
         out.write(_("Create torrent"));
        out.write("\" name=\"action\" ></table>\n");
         out.write("</form>\n</span></div>");        
     }
     
     private void writeConfigForm(PrintWriter out, HttpServletRequest req) throws IOException {
         String uri = req.getRequestURI();
         String dataDir = _manager.getDataDir().getAbsolutePath();
         boolean autoStart = _manager.shouldAutoStart();
         boolean useOpenTrackers = _manager.util().shouldUseOpenTrackers();
         String openTrackers = _manager.util().getOpenTrackerString();
         //int seedPct = 0;
        
         out.write("<form action=\"" + uri + "\" method=\"POST\">\n");
         out.write("<div class=\"configsection\"><span class=\"snarkConfig\">\n");
         out.write("<input type=\"hidden\" name=\"nonce\" value=\"" + _nonce + "\" >\n");
         out.write("<span class=\"snarkConfigTitle\">");
         out.write(_("Configuration"));
         out.write("</span><br>\n");
         out.write("<table border=\"0\"><tr><td>");
         out.write(_("Data directory"));
         out.write(": <td><input type=\"text\" size=\"50\" name=\"dataDir\" value=\"" + dataDir + "\" ");
         out.write("title=\"");
         out.write(_("Directory to store torrents and data"));
         out.write("\" disabled=\"true\" ><br><i>(");
         out.write(_("Edit i2psnark.config and restart to change"));
         out.write(")</i><br>\n");
 
         out.write("<tr><td>");
         out.write(_("Auto start"));
         out.write(": <td><input type=\"checkbox\" class=\"optbox\" name=\"autoStart\" value=\"true\" " 
                   + (autoStart ? "checked " : "") 
                   + "title=\"");
         out.write(_("If checked, automatically start torrents that are added"));
         out.write("\" >");
 
         //Auto add: <input type="checkbox" name="autoAdd" value="true" title="If true, automatically add torrents that are found in the data directory" />
         //Auto stop: <input type="checkbox" name="autoStop" value="true" title="If true, automatically stop torrents that are removed from the data directory" />
         //out.write("<br>\n");
 /*
         out.write("Seed percentage: <select name=\"seedPct\" disabled=\"true\" >\n\t");
         if (seedPct <= 0)
             out.write("<option value=\"0\" selected=\"true\">Unlimited</option>\n\t");
         else
             out.write("<option value=\"0\">Unlimited</option>\n\t");
         if (seedPct == 100)
             out.write("<option value=\"100\" selected=\"true\">100%</option>\n\t");
         else
             out.write("<option value=\"100\">100%</option>\n\t");
         if (seedPct == 150)
             out.write("<option value=\"150\" selected=\"true\">150%</option>\n\t");
         else
             out.write("<option value=\"150\">150%</option>\n\t");
         out.write("</select><br>\n");
 */
         out.write("<tr><td>");
         out.write(_("Total uploader limit"));
         out.write(": <td><input type=\"text\" name=\"upLimit\" value=\""
                   + _manager.util().getMaxUploaders() + "\" size=\"3\" maxlength=\"3\" > ");
         out.write(_("peers"));
         out.write("<br>\n");
 
         out.write("<tr><td>");
         out.write(_("Up bandwidth limit"));
         out.write(": <td><input type=\"text\" name=\"upBW\" value=\""
                   + _manager.util().getMaxUpBW() + "\" size=\"3\" maxlength=\"3\" > KBps <i>(");
         out.write(_("Half available bandwidth recommended."));
         out.write(" <a href=\"/config.jsp\" target=\"blank\">");
         out.write(_("View or change router bandwidth"));
         out.write("</a>)</i><br>\n");
         
         out.write("<tr><td>");
         out.write(_("Use open trackers also"));
         out.write(": <td><input type=\"checkbox\" class=\"optbox\" name=\"useOpenTrackers\" value=\"true\" " 
                   + (useOpenTrackers ? "checked " : "") 
                   + "title=\"");
         out.write(_("If checked, announce torrents to open trackers as well as the tracker listed in the torrent file"));
         out.write("\" > ");
 
         out.write("<tr><td>");
         out.write(_("Open tracker announce URLs"));
         out.write(": <td><input type=\"text\" name=\"openTrackers\" value=\""
                   + openTrackers + "\" size=\"50\" ><br>\n");
 
         //out.write("\n");
         //out.write("EepProxy host: <input type=\"text\" name=\"eepHost\" value=\""
         //          + _manager.util().getEepProxyHost() + "\" size=\"15\" /> ");
         //out.write("port: <input type=\"text\" name=\"eepPort\" value=\""
         //          + _manager.util().getEepProxyPort() + "\" size=\"5\" maxlength=\"5\" /><br>\n");
 
         out.write("<tr><td>");
         out.write(_("I2CP host"));
         out.write(": <td><input type=\"text\" name=\"i2cpHost\" value=\"" 
                   + _manager.util().getI2CPHost() + "\" size=\"15\" > ");
 
         out.write("<tr><td>");
         out.write(_("I2CP port"));
         out.write(": <td><input type=\"text\" name=\"i2cpPort\" value=\"" +
                   + _manager.util().getI2CPPort() + "\" size=\"5\" maxlength=\"5\" > <br>\n");
 
         StringBuilder opts = new StringBuilder(64);
         Map options = new TreeMap(_manager.util().getI2CPOptions());
         for (Iterator iter = options.entrySet().iterator(); iter.hasNext(); ) {
             Map.Entry entry = (Map.Entry)iter.next();
             String key = (String)entry.getKey();
             String val = (String)entry.getValue();
             opts.append(key).append('=').append(val).append(' ');
         }
         out.write("<tr><td>");
         out.write(_("I2CP options"));
         out.write(": <td><textarea name=\"i2cpOpts\" cols=\"60\" rows=\"1\" wrap=\"off\" >"
                   + opts.toString() + "</textarea><br>\n");
 
         out.write("<tr><td>&nbsp;<td><input type=\"submit\" value=\"");
         out.write(_("Save configuration"));
        out.write("\" name=\"action\" >\n");
         out.write("</table></span>\n");
         out.write("</form></div>");
     }
     
     /** translate */
     private String _(String s) {
         return _manager.util().getString(s);
     }
 
     /** translate */
     private String _(String s, Object o) {
         return _manager.util().getString(s, o);
     }
 
     // rounding makes us look faster :)
     private String formatSize(long bytes) {
         if (bytes < 5*1024)
             return bytes + "B";
         else if (bytes < 5*1024*1024)
             return ((bytes + 512)/1024) + "KB";
         else if (bytes < 10*1024*1024*1024l)
             return ((bytes + 512*1024)/(1024*1024)) + "MB";
         else
             return ((bytes + 512*1024*1024)/(1024*1024*1024)) + "GB";
     }
     
     private static final String HEADER = "<link href=\"../themes/console/snark.css\" rel=\"stylesheet\" type=\"text/css\" >";
                                        
 
     private static final String TABLE_HEADER = "<table border=\"0\" class=\"snarkTorrents\" width=\"100%\" cellpadding=\"0 10px\">\n" +
                                                "<thead>\n" +
                                                "<tr><th align=\"center\">";
 
     private static final String TABLE_FOOTER = "</table></div>\n";
     
     private static final String FOOTER = "</div></div></div></center></body></html>";
 
 /** inner class, don't bother reindenting */
 private static class FetchAndAdd implements Runnable {
     private SnarkManager _manager;
     private String _url;
     public FetchAndAdd(SnarkManager mgr, String url) {
         _manager = mgr;
         _url = url;
     }
     public void run() {
         _url = _url.trim();
         // 3 retries
         File file = _manager.util().get(_url, false, 3);
         try {
             if ( (file != null) && (file.exists()) && (file.length() > 0) ) {
                 _manager.addMessage(_("Torrent fetched from {0}", _url));
                 FileInputStream in = null;
                 try {
                     in = new FileInputStream(file);
                     MetaInfo info = new MetaInfo(in);
                     String name = info.getName();
                     name = name.replace('/', '_');
                     name = name.replace('\\', '_');
                     name = name.replace('&', '+');
                     name = name.replace('\'', '_');
                     name = name.replace('"', '_');
                     name = name.replace('`', '_');
                     name = name + ".torrent";
                     File torrentFile = new File(_manager.getDataDir(), name);
 
                     String canonical = torrentFile.getCanonicalPath();
 
                     if (torrentFile.exists()) {
                         if (_manager.getTorrent(canonical) != null)
                             _manager.addMessage(_("Torrent already running: {0}", name));
                         else
                             _manager.addMessage(_("Torrent already in the queue: {0}", name));
                     } else {
                         FileUtil.copy(file.getAbsolutePath(), canonical, true);
                         _manager.addTorrent(canonical);
                     }
                 } catch (IOException ioe) {
                     _manager.addMessage(_("Torrent at {0} was not valid", _url) + ": " + ioe.getMessage());
                 } finally {
                     try { in.close(); } catch (IOException ioe) {}
                 }
             } else {
                 _manager.addMessage(_("Torrent was not retrieved from {0}", _url));
             }
         } finally {
             if (file != null) file.delete();
         }
     }
 
     private String _(String s, String o) {
         return _manager.util().getString(s, o);
     }
 
 }
 
 }
