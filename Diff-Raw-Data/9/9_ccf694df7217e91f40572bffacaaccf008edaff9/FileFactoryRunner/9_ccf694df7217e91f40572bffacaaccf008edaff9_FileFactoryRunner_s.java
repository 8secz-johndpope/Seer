 package cz.vity.freerapid.plugins.services.filefactory;
 
 import cz.vity.freerapid.plugins.exceptions.*;
 import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
 import cz.vity.freerapid.plugins.webclient.AbstractRunner;
 import cz.vity.freerapid.plugins.webclient.FileState;
 import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
 import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
 import org.apache.commons.httpclient.HttpMethod;
 import org.apache.commons.httpclient.methods.GetMethod;
 
 import java.util.concurrent.TimeUnit;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 
 /**
  * @author Kajda, ntoskrnl
  */
 class FileFactoryRunner extends AbstractRunner {
     private static final Logger logger = Logger.getLogger(FileFactoryRunner.class.getName());
     private String reCaptchaKey;
     private String captchaCheck;
 
     @Override
     public void runCheck() throws Exception {
         super.runCheck();
         final GetMethod getMethod = getGetMethod(fileURL);
 
         if (makeRedirectedRequest(getMethod)) {
             checkSeriousProblems();
             checkNameAndSize(getContentAsString());
         } else {
             checkSeriousProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     @Override
     public void run() throws Exception {
         super.run();
         logger.info("Starting download in TASK " + fileURL);
         GetMethod getMethod = getGetMethod(fileURL);
 
         if (makeRedirectedRequest(getMethod)) {
             checkAllProblems();
             checkNameAndSize(getContentAsString());
 
             if (getContentAsString().contains("Download with FileFactory TrafficShare")) {
                 HttpMethod finalMethod = getMethodBuilder()
                         .setReferer(fileURL)
                         .setActionFromAHrefWhereATagContains("Download with FileFactory TrafficShare")
                         .toGetMethod();
                 if (tryDownloadAndSaveFile(finalMethod)) {
                     return;
                 }
                 makeRedirectedRequest(getMethod);
             }
 
             do {
                 if (!makeRedirectedRequest(stepCaptcha())) {
                     throw new ServiceConnectionProblemException();
                 }
             } while (!getContentAsString().contains("\"status\":\"ok\""));
 
             final String action = PlugUtils.getStringBetween(getContentAsString(), "\"path\":\"", "\"").replace("\\/", "/");
             final HttpMethod httpMethod = getMethodBuilder()
                     .setReferer(fileURL)
                     .setAction(action)
                     .toGetMethod();
             if (makeRedirectedRequest(httpMethod)) {
                 checkAllProblems();
 
                 final HttpMethod finalMethod = getMethodBuilder()
                         .setReferer(httpMethod.getURI().toString())
                         .setActionFromAHrefWhereATagContains("Click here to download now")
                         .toGetMethod();
 
                 downloadTask.sleep(PlugUtils.getWaitTimeBetween(getContentAsString(), "id=\"startWait\" value=\"", "\"", TimeUnit.SECONDS) + 1);
 
                 if (!tryDownloadAndSaveFile(finalMethod)) {
                     checkAllProblems();
                     throw new ServiceConnectionProblemException("Error starting download");
                 }
 
             } else {
                 throw new ServiceConnectionProblemException();
             }
         } else {
             checkAllProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private void checkNameAndSize(final String content) throws ErrorDuringDownloadingException {
         PlugUtils.checkName(httpFile, content, "class=\"last\">", "</span");
         PlugUtils.checkFileSize(httpFile, content, "<h2>", "file uploaded");
         httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
     }
 
     private void checkSeriousProblems() throws ErrorDuringDownloadingException {
         final String contentAsString = getContentAsString();
         if (contentAsString.contains("Sorry, this file is no longer available")) {
             throw new URLNotAvailableAnymoreException("Sorry, this file is no longer available. It may have been deleted by the uploader, or has expired");
         }
         if (contentAsString.contains("What is FileFactory?")) {
             throw new URLNotAvailableAnymoreException("Page not found");
         }
        if (contentAsString.contains("Sorry, there are currently no free download slots available on this server")) {
            throw new ServiceConnectionProblemException("Sorry, there are currently no free download slots available on this server");
        }
        if (contentAsString.contains("All of the available ")) {
            throw new ServiceConnectionProblemException("All of the available free download slots on this server are in use");
         }
     }
 
     private void checkAllProblems() throws ErrorDuringDownloadingException {
         checkSeriousProblems();
 
         final String contentAsString = getContentAsString();
         if (contentAsString.contains("Your download slot has expired")) {
             throw new ServiceConnectionProblemException("Your download slot has expired. Please try again");
         }
         if (contentAsString.contains("You are currently downloading too many files at once")) {
             throw new ServiceConnectionProblemException("You are currently downloading too many files at once. Multiple simultaneous downloads are only permitted for Premium Members");
         }
         Matcher matcher = getMatcherAgainstContent("You(?:r IP)? \\((.+?)\\) (?:has|have) exceeded the download limit for free users");
         if (matcher.find()) {
             final String userIP = matcher.group(1);
             matcher = getMatcherAgainstContent("Please wait (.+?) (.+?) to download more files");
             int waitSeconds = 2 * 60;
             if (matcher.find()) {
                 if (matcher.group(2).equals("minutes")) {
                     waitSeconds = 60 * Integer.parseInt(matcher.group(1));
                 } else {
                     waitSeconds = Integer.parseInt(matcher.group(1));
                 }
             }
             throw new YouHaveToWaitException(String.format("You (%s) have exceeded the download limit for free users", userIP), waitSeconds);
         }
     }
 
     private HttpMethod stepCaptcha() throws Exception {
         if (reCaptchaKey == null) {
             final Matcher matcher = getMatcherAgainstContent("Recaptcha\\.create\\(\\s*?\"(.+?)\"");
             if (!matcher.find()) {
                 throw new PluginImplementationException("ReCaptcha key not found");
             }
             reCaptchaKey = matcher.group(1);
         }
         if (captchaCheck == null) {
             final Matcher matcher = getMatcherAgainstContent("check\\s*?:\\s*?'(.+?)'");
             if (!matcher.find()) {
                 throw new PluginImplementationException("Captcha check not found");
             }
             captchaCheck = matcher.group(1);
         }
         final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
         final CaptchaSupport captchaSupport = getCaptchaSupport();
 
         final String captchaURL = r.getImageURL();
         logger.info("Captcha URL " + captchaURL);
 
         final String captcha = captchaSupport.getCaptcha(captchaURL);
         if (captcha == null) throw new CaptchaEntryInputMismatchException();
         r.setRecognized(captcha);
 
         final HttpMethod httpMethod = r.modifyResponseMethod(
                 getMethodBuilder()
                         .setReferer(fileURL)
                         .setAction("http://www.filefactory.com/file/checkCaptcha.php")
                         .setParameter("check", captchaCheck)
         ).toPostMethod();
         httpMethod.addRequestHeader("X-Requested-With", "XMLHttpRequest");//use AJAX
         return httpMethod;
     }
 
 }
