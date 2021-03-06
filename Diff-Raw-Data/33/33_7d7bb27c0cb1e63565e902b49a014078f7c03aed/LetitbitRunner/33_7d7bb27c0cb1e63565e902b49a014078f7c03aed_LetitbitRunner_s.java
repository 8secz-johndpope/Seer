 package cz.vity.freerapid.plugins.services.letitbit;
 
 import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
 import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
 import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
 import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
 import cz.vity.freerapid.plugins.webclient.AbstractRunner;
 import cz.vity.freerapid.plugins.webclient.FileState;
 import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
 import org.apache.commons.httpclient.Cookie;
 import org.apache.commons.httpclient.HttpMethod;
 
 import java.util.logging.Logger;
 
 /**
  * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
  */
 class LetitbitRunner extends AbstractRunner {
     private final static Logger logger = Logger.getLogger(LetitbitRunner.class.getName());
 
     @Override
     public void runCheck() throws Exception {
         super.runCheck();
         addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
         setPageEncoding("Windows-1251");
         final HttpMethod httpMethod = getGetMethod(fileURL);
         if (makeRedirectedRequest(httpMethod)) {
             checkProblems();
             checkNameAndSize();
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private void checkNameAndSize() throws Exception {
         final String contentAsString = getContentAsString();
         PlugUtils.checkName(httpFile, contentAsString, "File: <span>", "</span>");
         PlugUtils.checkFileSize(httpFile, contentAsString, "[<span>", "</span>]");
         httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
     }
 
     @Override
     public void run() throws Exception {
         super.run();
         logger.info("Starting download in TASK " + fileURL);
         addCookie(new Cookie(".letitbit.net", "lang", "en", "/", 86400, false));
         setPageEncoding("Windows-1251");
         client.getHTTPClient().getParams().setBooleanParameter("dontUseHeaderFilename", true);
 
         HttpMethod httpMethod = getGetMethod(fileURL);
         if (makeRedirectedRequest(httpMethod)) {
             checkProblems();
             checkNameAndSize();
 
             httpMethod = getMethodBuilder()
                     .setReferer(fileURL)
                     .setActionFromFormByName("ifree_form", true)
                     .toPostMethod();
             if (!makeRedirectedRequest(httpMethod)) {
                 checkProblems();
                 throw new ServiceConnectionProblemException();
             }
             String pageUrl = httpMethod.getURI().toString();
 
             if (!getContentAsString().contains("\"dvifree\"")) {
                 //Russian IPs may see a different page here, let's handle it
                 httpMethod = getMethodBuilder()
                         .setReferer(pageUrl)
                        .setActionFromFormWhereActionContains("letitbit.net", true)
                         .toPostMethod();
                 if (!makeRedirectedRequest(httpMethod)) {
                     checkProblems();
                     throw new ServiceConnectionProblemException();
                 }
                 pageUrl = httpMethod.getURI().toString();
             }
 
             httpMethod = getMethodBuilder()
                     .setReferer(pageUrl)
                     .setActionFromFormByName("dvifree", true)
                     .toPostMethod();
             if (!makeRedirectedRequest(httpMethod)) {
                 checkProblems();
                 throw new ServiceConnectionProblemException();
             }
             pageUrl = httpMethod.getURI().toString();
 
             downloadTask.sleep(PlugUtils.getNumberBetween(getContentAsString(), "seconds =", ";") + 1);
 
             httpMethod = getMethodBuilder()
                     .setReferer(pageUrl)
                     .setAction("/ajax/download3.php")
                     .toPostMethod();
             if (!makeRedirectedRequest(httpMethod)) {
                 checkProblems();
                 throw new ServiceConnectionProblemException();
             }
 
             final String content = getContentAsString().trim();
             if (content.isEmpty()) {
                 throw new PluginImplementationException("Download link not found");
             }
             httpMethod = getMethodBuilder()
                     .setReferer(pageUrl)
                     .setAction(content)
                     .toGetMethod();
             if (!tryDownloadAndSaveFile(httpMethod)) {
                 checkProblems();
                 throw new ServiceConnectionProblemException("Error starting download");
             }
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private void checkProblems() throws ErrorDuringDownloadingException {
         final String content = getContentAsString();
         if (content.contains("The page is temporarily unavailable")) {
             throw new ServiceConnectionProblemException("The page is temporarily unavailable");
         }
         if (content.contains("You must have static IP")) {
             throw new ServiceConnectionProblemException("You must have static IP");
         }
         if (content.contains("file was not found")
                 || content.contains("\u043D\u0430\u0439\u0434\u0435\u043D")
                 || content.contains("<title>404</title>")
                 || (content.contains("Request file ") && content.contains(" Deleted"))) {
             throw new URLNotAvailableAnymoreException("The requested file was not found");
         }
     }
 
    @Override
    protected String getBaseURL() {
        return "http://letitbit.net";
    }

 }
