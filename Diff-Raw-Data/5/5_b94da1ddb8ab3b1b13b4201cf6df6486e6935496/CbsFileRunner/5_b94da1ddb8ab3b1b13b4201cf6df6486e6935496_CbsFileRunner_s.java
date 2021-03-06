 package cz.vity.freerapid.plugins.services.cbs;
 
 import cz.vity.freerapid.plugins.exceptions.*;
 import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
 import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
 import cz.vity.freerapid.plugins.services.tunlr.Tunlr;
 import cz.vity.freerapid.plugins.webclient.FileState;
 import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
 import org.apache.commons.httpclient.HttpMethod;
 
 import java.util.Collections;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 
 /**
  * Class which contains main code
  *
  * @author ntoskrnl
  */
 class CbsFileRunner extends AbstractRtmpRunner {
     private final static Logger logger = Logger.getLogger(CbsFileRunner.class.getName());
     private final static String SWF_URL = "http://www.cbs.com/thunder/canplayer/canplayer.swf";
 
     @Override
     public void runCheck() throws Exception {
         super.runCheck();
         final HttpMethod method = getGetMethod(fileURL);
         if (makeRedirectedRequest(method)) {
             checkProblems();
             checkNameAndSize();
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private void checkNameAndSize() throws ErrorDuringDownloadingException {
         final Matcher matcher = getMatcherAgainstContent("<title>(.+?) Video \\- (.+?) \\- CBS\\.com</title>");
         if (!matcher.find()) {
             throw new PluginImplementationException("File name not found");
         }
         httpFile.setFileName(matcher.group(1) + " - " + matcher.group(2) + ".flv");
         httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
     }
 
     @Override
     public void run() throws Exception {
         super.run();
         logger.info("Starting download in TASK " + fileURL);
         HttpMethod method = getGetMethod(fileURL);
         if (makeRedirectedRequest(method)) {
             checkProblems();
             checkNameAndSize();
             String pid;
             try {
                 pid = PlugUtils.getStringBetween(getContentAsString(), "pid = '", "'");
             } catch (final PluginImplementationException e) {
                 final Matcher matcher = PlugUtils.matcher("pid=(.+?)(?:&.+?)?$", fileURL);
                 if (!matcher.find()) {
                     throw new PluginImplementationException("PID not found");
                 }
                 pid = matcher.group(1);
             }
             setFileStreamContentTypes(new String[0], new String[]{"application/smil"});
             method = getGetMethod("http://link.theplatform.com/s/dJ5BDC/" + pid + "?format=SMIL&Tracking=true&mbr=true");
             if (!client.getSettings().isProxySet()) {
                 Tunlr.setupMethod(method);
             }
             if (!makeRedirectedRequest(method)) {
                 checkProblems();
                 throw new ServiceConnectionProblemException();
             }
             checkProblems();
             final RtmpSession rtmpSession = getRtmpSession();
             rtmpSession.getConnectParams().put("swfUrl", SWF_URL);
             rtmpSession.getConnectParams().put("pageUrl", fileURL);
             tryDownloadAndSaveFile(rtmpSession);
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private void checkProblems() throws ErrorDuringDownloadingException {
         if (getContentAsString().contains("Not the page you were looking for")
                 || getContentAsString().contains("Media is not available")) {
             throw new URLNotAvailableAnymoreException("File not found");
         }
        if (getContentAsString().contains("title=\"Unavailable\"")) {
            throw new NotRecoverableDownloadException("This video cannot be streamed in your geographical area");
         }
     }
 
     private RtmpSession getRtmpSession() throws ErrorDuringDownloadingException {
         final String baseUrl = PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "<meta base=\"", "\""));
         final List<Stream> list = new LinkedList<Stream>();
         final Matcher matcher = getMatcherAgainstContent("<video src=\"(.+?)\" system\\-bitrate=\"(\\d+)\"");
         while (matcher.find()) {
             list.add(new Stream(PlugUtils.unescapeHtml(matcher.group(1)), Integer.parseInt(matcher.group(2))));
         }
         if (list.isEmpty()) {
             throw new PluginImplementationException("No streams found");
         }
         return new RtmpSession(baseUrl, Collections.max(list).getPlayName());
     }
 
     private static class Stream implements Comparable<Stream> {
         private final String playName;
         private final int bitrate;
 
         public Stream(final String playName, final int bitrate) {
             this.playName = playName;
             this.bitrate = bitrate;
         }
 
         public String getPlayName() throws ErrorDuringDownloadingException {
             if (playName.contains(".mp4") && !playName.startsWith("mp4:")) {
                 return "mp4:" + playName;
             } else {
                 return playName;
             }
         }
 
         @Override
         public int compareTo(final Stream that) {
             return Integer.valueOf(this.bitrate).compareTo(that.bitrate);
         }
     }
 
 }
