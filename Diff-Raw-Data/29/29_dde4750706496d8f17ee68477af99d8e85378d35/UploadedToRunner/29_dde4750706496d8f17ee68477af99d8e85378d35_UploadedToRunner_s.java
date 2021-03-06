 package cz.vity.freerapid.plugins.services.uploadedto;
 
 import cz.vity.freerapid.plugins.exceptions.*;
 import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
 import cz.vity.freerapid.plugins.webclient.AbstractRunner;
 import cz.vity.freerapid.plugins.webclient.FileState;
 import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
 import org.apache.commons.httpclient.Cookie;
 import org.apache.commons.httpclient.HttpMethod;
 
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 
 /**
 * @author Ladislav Vitasek, ntoskrnl
  */
 class UploadedToRunner extends AbstractRunner {
     private final static Logger logger = Logger.getLogger(UploadedToRunner.class.getName());
 
     @Override
     public void runCheck() throws Exception {
         super.runCheck();
         addCookie(new Cookie(".uploaded.to", "lang", "en", "/", 86400, false));
         addCookie(new Cookie(".ul.to", "lang", "en", "/", 86400, false));
         final HttpMethod method = getGetMethod(fileURL);
         if (makeRedirectedRequest(method)) {
             checkSizeAndName();
             checkProblems();
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     @Override
     public void run() throws Exception {
         super.run();
         logger.info("Starting download in TASK " + fileURL);
         addCookie(new Cookie(".uploaded.to", "lang", "en", "/", 86400, false));
         addCookie(new Cookie(".ul.to", "lang", "en", "/", 86400, false));
         HttpMethod method = getGetMethod(fileURL);
         if (makeRedirectedRequest(method)) {
             checkProblems();
             checkSizeAndName();
             fileURL = method.getURI().toString();
             final String fileId = getFileId();
             final int wait = PlugUtils.getNumberBetween(getContentAsString(), "<span>", "</span> seconds");
             setFileStreamContentTypes(new String[0], new String[]{"application/javascript"});
             method = getGetMethod("http://uploaded.to/io/ticket/slot/" + fileId);
             if (makeRedirectedRequest(method)) {
                 if (getContentAsString().contains("err:")) {
                     throw new ServiceConnectionProblemException("All download slots are full");
                 }
                 downloadTask.sleep(wait + 1);
                 while (true) {
                     if (!makeRedirectedRequest(stepCaptcha(fileId))) {
                         checkProblems();
                         throw new ServiceConnectionProblemException();
                     }
                     if (!getMatcherAgainstContent("err\"?:\"?captcha").find()) {
                        if (getContentAsString().contains("limit-dl")) {
                             throw new ServiceConnectionProblemException("Free download limit reached");
                         }
                        //By Abinash Bishoyi
                        if (getContentAsString().contains("max. number of possible free downloads for this hour")) {
                            throw new ServiceConnectionProblemException("Hourly free download limit reached");
                        }
                        if (getContentAsString().contains("limit-parallel")) {
                             throw new ServiceConnectionProblemException("You are already downloading a file");
                         }
                        if (getContentAsString().contains("limit-size") || getContentAsString().contains("In order to download files bigger")) {
                             throw new NotRecoverableDownloadException("Only premium users may download files larger than 1 GB");
                         }
                         method = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("url:'", "'").toGetMethod();
                         if (!tryDownloadAndSaveFile(method)) {
                             checkProblems();
                             throw new ServiceConnectionProblemException("Error starting download");
                         }
                         break;
                     }
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
 
     private void checkSizeAndName() throws ErrorDuringDownloadingException {
         final Matcher matcher = getMatcherAgainstContent("<title>(.+?) \\((.+?)\\) \\- uploaded\\.to</title>");
         if (!matcher.find()) {
             throw new PluginImplementationException("File name/size not found");
         }
         httpFile.setFileName(matcher.group(1));
         httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
         httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
     }
 
     private void checkProblems() throws ErrorDuringDownloadingException {
         if (getContentAsString().contains("Page not found") || getContentAsString().contains("The requested file isn't available anymore")) {
             throw new URLNotAvailableAnymoreException("File not found");
         }
         if (getContentAsString().contains("Our service is currently unavailable in your country")) {
             throw new NotRecoverableDownloadException("Our service is currently unavailable in your country");
         }
         if (getContentAsString().contains("already downloading") || getContentAsString().contains("The internal connection has failed")) {
             throw new ServiceConnectionProblemException("Your IP address is already downloading a file");
         }
         if (getContentAsString().contains("Currently a lot of users")) {
             throw new ServiceConnectionProblemException("Currently a lot of users are downloading files");
         }
         if (getContentAsString().contains("can only be queried by premium users")) {
             throw new ServiceConnectionProblemException("The file status can only be queried by premium users");
         }
     }
 
     private String getFileId() throws ErrorDuringDownloadingException {
         final Matcher matcher = PlugUtils.matcher("/file/([^/]+)", fileURL);
         if (!matcher.find()) {
             throw new PluginImplementationException("Error parsing file URL");
         }
         return matcher.group(1);
     }
 
     private HttpMethod stepCaptcha(final String fileId) throws Exception {
         final ReCaptcha r = new ReCaptcha("6Lcqz78SAAAAAPgsTYF3UlGf2QFQCNuPMenuyHF3", client);
         final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
         if (captcha == null) {
             throw new CaptchaEntryInputMismatchException();
         }
         r.setRecognized(captcha);
         return r.modifyResponseMethod(
                 getMethodBuilder().setReferer(fileURL).setAction("http://uploaded.to/io/ticket/captcha/" + fileId)
         ).toPostMethod();
     }
 
 }
