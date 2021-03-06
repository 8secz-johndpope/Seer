 package cz.vity.freerapid.plugins.services.mediafire;
 
 import cz.vity.freerapid.plugins.exceptions.*;
 import cz.vity.freerapid.plugins.webclient.AbstractRunner;
 import cz.vity.freerapid.plugins.webclient.FileState;
 import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
 import cz.vity.freerapid.utilities.LogUtils;
 import org.apache.commons.httpclient.HttpMethod;
 import org.apache.commons.httpclient.methods.GetMethod;
 
import java.io.UnsupportedEncodingException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URLDecoder;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 
 /**
  * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
  */
 class MediafireRunner extends AbstractRunner {
     private final static Logger logger = Logger.getLogger(MediafireRunner.class.getName());
 
     public MediafireRunner() {
         super();
     }
 
     @Override
     public void runCheck() throws Exception {
         super.runCheck();
         final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
         if (makeRedirectedRequest(getMethod)) {
             checkProblems();
             checkNameAndSize(getContentAsString());
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     @Override
     public void run() throws Exception {
         super.run();
 
         if (isList()) {
             runList();
             return;
         }
 
         final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
         if (makeRedirectedRequest(getMethod)) {
             checkProblems();
 
             if (getContentAsString().contains("dh('');")) { //if passworded
                 while (getContentAsString().contains("dh('');")) {
                     HttpMethod postPwd = getMethodBuilder()
                             .setReferer(fileURL)
                             .setBaseURL("http://www.mediafire.com/")
                             .setActionFromFormByName("form_password", true)
                             .setAndEncodeParameter("downloadp", getPassword())
                             .toPostMethod();
                     if (!makeRedirectedRequest(postPwd)) {
                         throw new ServiceConnectionProblemException("Some issue while posting password");
                     }
                 }
             }
             downloadTask.sleep(5);
             if (getContentAsString().contains("unescape")) {
                 String cont = processUnescapeSection(getContentAsString());
 
                 Matcher match = PlugUtils.matcher("([0-9A-Za-z]*)\\('([^']*)','([^']*)','([^']*)'\\)", cont);
 
                 if (!match.find()) {
                     throw new PluginImplementationException();
                 }
                 String function = match.group(1);
                 String qk = match.group(2);
                 String pk = match.group(3);
                 String r = match.group(4);
                 String url = "http://www.mediafire.com/dynamic/download.php?qk=" + qk + "&pk=" + pk + "&r=" + r;
                 logger.info("Script target URL " + url);
                 client.setReferer("http://www.mediafire.com/?" + qk);
                 GetMethod method = getGetMethod(url);
 
                 int functionStart = getContentAsString().indexOf("function " + function);
                 String regToId = "getElementById\\('([^']*)'\\).innerHTML";
                 Matcher match2 = getMatcherAgainstContent(regToId);
                 if (!match2.find(functionStart)) {
                     throw new PluginImplementationException();
                 }
                 String posID = match2.group(1);
 
                 if (makeRequest(method)) {
                     String rec = processUnescapeSection(getContentAsString());
 
                     if (!rec.contains("'download")) {
                         throw new ServiceConnectionProblemException();
                     }
 
                     String rawlink;
                     try {
                         rawlink = PlugUtils.getStringBetween(rec, posID + "').innerHTML = 'Your download is starting..';\" href=\"h", "\"> Click");
                     } catch (PluginImplementationException e) {
                         throw new ServiceConnectionProblemException(e);
                     }
                     logger.info("raw URL " + rawlink);
                     String finalLink = parseLink("h" + rawlink);
                     logger.info("Final URL " + finalLink);
 
                     client.setReferer("http://www.mediafire.com/?" + qk);
                     GetMethod method2 = getGetMethod(finalLink);
                     client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
                     downloadTask.sleep(5);
                     if (!tryDownloadAndSaveFile(method2)) {
                         checkProblems();
                         logger.warning(getContentAsString());
                         throw new ServiceConnectionProblemException("Error starting download");
                     }
 
                 } else {
                     checkProblems();
                     throw new ServiceConnectionProblemException();
                 }
             } else {
                 checkProblems();
                throw new ServiceConnectionProblemException();
             }
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
 
     }
 
    private String processUnescapeSection(String cont) throws UnsupportedEncodingException, PluginImplementationException {
        String regx = "var [a-zA-Z0-9]+=unescape\\('([^']*)'\\);var [a-zA-Z0-9]+=([a-zA-Z0-9]+);for\\(.=.;.<[a-zA-Z0-9]+;.\\+\\+\\)[a-zA-Z0-9]+=[a-zA-Z0-9]+\\+\\(String.fromCharCode\\([a-zA-Z0-9]+.charCodeAt\\(.\\)\\^([0-9^]+)";
         Boolean loop = true;
        String globalCont = cont;
         String tuReturn = "";
         while (loop) {
             cont = cont.replace("\\", "");
             Matcher matcher = PlugUtils.matcher(regx, cont);
             int findTime = 0;
             while (matcher.find()) {
                 if (findTime++ == 0) cont = "";
                 String esc = matcher.group(1);
                int toFor = 0;
                try {
                    toFor = Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException e) {
                    toFor = Integer.parseInt(getVar(matcher.group(2), cont + tuReturn + globalCont));
                }
                 String shift = matcher.group(3);
                 String shiftA[] = shift.split("\\^");
                 int nax = Integer.parseInt((shiftA[0]));
                 for (int i = 1; i < shiftA.length; i++) {
                     nax = nax ^ Integer.parseInt(shiftA[i]);
                 }
                 String new_cont = "";
                 esc = URLDecoder.decode(esc, "UTF-8");
                //  toFor = Math.min(toFor,esc.length());
                toFor = esc.length();
                 for (int i = 0; i < toFor; i++) {
                    //  System.out.println(esc.codePointAt(i));
                     new_cont = new_cont + ((char) (esc.codePointAt(i) ^ nax));
                 }
                 cont = cont + "\n" + new_cont.replace("\\", "");
                //   logger.info(cont);
             }
 
             tuReturn = tuReturn + "\n" + cont;
             if (findTime == 0) loop = false;
 
         }
         logger.info(tuReturn);
         return tuReturn;
     }
 
     private void checkNameAndSize(String content) throws Exception {
         if (isList()) return;
         PlugUtils.checkFileSize(httpFile, content, "sharedtabsfileinfo1-fs\" value=\"", "\">");
         PlugUtils.checkName(httpFile, content, "sharedtabsfileinfo1-fn\" value=\"", "\">");
         httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
     }
 
     private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
         final String contentAsString = getContentAsString();
         if (contentAsString.contains("The key you provided for file download was invalid") || contentAsString.contains("How can MediaFire help you?")) {
             throw new URLNotAvailableAnymoreException("File not found");
         }
     }
 
     private void runList() throws Exception {
         final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
 
         if (makeRedirectedRequest(getMethod)) {
             final Matcher matcher = getMatcherAgainstContent("src=\"(/js/myfiles.php[^\"]+?)\"");
             if (!matcher.find()) throw new PluginImplementationException("URL to list not found");
             HttpMethod listMethod = getMethodBuilder().setBaseURL("http://www.mediafire.com").setAction(matcher.group(1)).toHttpMethod();
 
             if (makeRedirectedRequest(listMethod)) parseList();
             else throw new ServiceConnectionProblemException();
 
         } else throw new ServiceConnectionProblemException();
     }
 
 
     private String parseLink(String rawlink) throws Exception {
 
         String link = "";
 
         Matcher matcher = PlugUtils.matcher("([^'\"]*)(?:'|\")([^'\"]*)'", rawlink);
         while (matcher.find()) {
             link = link + matcher.group(1);
             Matcher matcher1 = PlugUtils.matcher("\\+\\s*(\\w+)", matcher.group(2));
             while (matcher1.find()) {
 
                 link = link + (getVar(matcher1.group(1), getContentAsString()));
             }
 
 
         }
         matcher = PlugUtils.matcher("([^'\"]*)$", rawlink);
         if (matcher.find()) {
             link = link + (matcher.group(1));
         }
 
         return link;
     }
 
     private String getVar(String s, String content) throws PluginImplementationException {
 
         Matcher matcher = PlugUtils.matcher("var " + s + "\\s*=\\s*'([^']*)'", content);
         if (matcher.find()) {
             return matcher.group(1);
         }
         matcher = PlugUtils.matcher("var " + s + "\\s*=\\s*([0-9]+)", content);
         if (matcher.find()) {
             return matcher.group(1);
         }
 
         throw new PluginImplementationException("Parameter " + s + " was not found");
     }
 
 
     private void parseList() {
         final Matcher matcher = getMatcherAgainstContent("oe\\[[0-9]+\\]=Array\\('([^']+?)'");
         int start = 0;
         final List<URI> uriList = new LinkedList<URI>();
         while (matcher.find(start)) {
             final String link = "http://www.mediafire.com/download.php?" + matcher.group(1);
             try {
                 uriList.add(new URI(link));
             } catch (URISyntaxException e) {
                 LogUtils.processException(logger, e);
             }
             start = matcher.end();
         }
         getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
     }
 
 
     private boolean isList() {
         return (fileURL.contains("?sharekey="));
     }
 
     private String getPassword() throws Exception {
         MediafirePasswordUI ps = new MediafirePasswordUI();
         if (getDialogSupport().showOKCancelDialog(ps, "Secured file on Mediafire")) {
             return (ps.getPassword());
         } else throw new NotRecoverableDownloadException("This file is secured with a password");
 
     }
 
 }
