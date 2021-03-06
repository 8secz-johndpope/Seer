 package cz.vity.freerapid.plugins.services.turbobit;
 
 import cz.vity.freerapid.plugins.exceptions.*;
 import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
 import cz.vity.freerapid.plugins.services.turbobit.captcha.CaptchaReader;
 import cz.vity.freerapid.plugins.webclient.AbstractRunner;
 import cz.vity.freerapid.plugins.webclient.FileState;
 import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
 import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
 import org.apache.commons.httpclient.Cookie;
 import org.apache.commons.httpclient.HttpMethod;
 
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 
 
 /**
  * Class which contains main code
  *
  * @author Arthur Gunawan, RickCL, ntoskrnl, tong2shot
  */
 class TurboBitFileRunner extends AbstractRunner {
     private final static Logger logger = Logger.getLogger(TurboBitFileRunner.class.getName());
 
     private final static int CAPTCHA_MAX = 0;
     private int captchaCounter = 1;
 
     @Override
     public void runCheck() throws Exception {
         super.runCheck();
         addCookie(new Cookie(".turbobit.net", "user_lang", "en", "/", 86400, false));
         fileURL = checkFileURL(fileURL);
         final HttpMethod method = getMethodBuilder().setAction(fileURL).toGetMethod();
         if (makeRedirectedRequest(method)) {
             checkFileProblems();
             checkNameAndSize();
         } else {
             checkFileProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private String checkFileURL(final String fileURL) throws ErrorDuringDownloadingException {
         final Matcher matcher = PlugUtils.matcher("http://(?:www\\.)?turbobit\\.net/(?:download/free/)?([a-z0-9]+)(?:\\.html?)?", fileURL);
         if (!matcher.find()) {
             throw new PluginImplementationException("Error parsing download link");
         }
         return "http://turbobit.net/" + matcher.group(1) + ".html";
     }
 
     private void checkNameAndSize() throws ErrorDuringDownloadingException {
         final Matcher filenameMatcher = getMatcherAgainstContent("<title>\\s+Download (.+?)\\. Free download");
         if (filenameMatcher.find()) {
             httpFile.setFileName(filenameMatcher.group(1));
         } else {
             throw new PluginImplementationException("File name not found");
         }
         final Matcher filesizeMatcher = getMatcherAgainstContent("<span.+?</span>\\s+\\((.+?)\\)");
         if (filesizeMatcher.find()) {
             httpFile.setFileSize(PlugUtils.getFileSizeFromString(filesizeMatcher.group(1)));
         } else {
             throw new PluginImplementationException("File size not found");
         }
         httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
     }
 
     @Override
     public void run() throws Exception {
         super.run();
         logger.info("Starting download in TASK " + fileURL);
         addCookie(new Cookie(".turbobit.net", "user_lang", "en", "/", 86400, false));
         fileURL = checkFileURL(fileURL);
 
         HttpMethod method = getMethodBuilder().setAction(fileURL).toGetMethod();
         if (makeRedirectedRequest(method)) {
             checkProblems();
             checkNameAndSize();
 
             method = getMethodBuilder()
                     .setReferer(method.getURI().toString())
                     .setActionFromAHrefWhereATagContains("Regular Download")
                     .toGetMethod();
             if (!makeRedirectedRequest(method)) {
                 checkProblems();
                 throw new ServiceConnectionProblemException();
             }
             checkProblems();
 
             Matcher matcher = getMatcherAgainstContent("limit\\s*:\\s*(\\d+)");
             if (matcher.find()) {
                 throw new YouHaveToWaitException("Download limit reached", Integer.parseInt(matcher.group(1)));
             }
 
             while (getContentAsString().contains("/captcha/")) {
                 if (!makeRedirectedRequest(stepCaptcha(method.getURI().toString()))) {
                     checkProblems();
                     throw new ServiceConnectionProblemException();
                 }
                 checkProblems();
             }
 
            matcher = getMatcherAgainstContent("\"fileId\"\\s*?:\\s*?\"(.+?)\"");
             if (!matcher.find()) {
                throw new PluginImplementationException("File ID not found");
             }
 
             method = getMethodBuilder()
                     .setReferer(method.getURI().toString())
                    .setAction("/download/getlinktimeout/" + matcher.group(1))
                     .toGetMethod();
             method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
 
            downloadTask.sleep(61);
 
             if (!makeRedirectedRequest(method)) {
                 checkProblems();
                 throw new ServiceConnectionProblemException();
             }
             checkProblems();
 
             method = getMethodBuilder()
                     .setReferer(method.getURI().toString())
                     .setActionFromAHrefWhereATagContains("Download")
                     .toGetMethod();
             if (!tryDownloadAndSaveFile(method)) {
                 checkProblems();
                 throw new ServiceConnectionProblemException("Error starting download");
             }
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private void checkFileProblems() throws ErrorDuringDownloadingException {
         if (getContentAsString().contains("File was not found")
                 || getContentAsString().contains("Probably it was deleted"))
             throw new URLNotAvailableAnymoreException("File not found");
     }
 
     private void checkDownloadProblems() throws ErrorDuringDownloadingException {
         if (getContentAsString().contains("The file is not avaliable now because of technical problems")) {
             throw new ServiceConnectionProblemException("The file is not available now because of technical problems");
         }
     }
 
     private void checkProblems() throws ErrorDuringDownloadingException {
         checkFileProblems();
         checkDownloadProblems();
     }
 
     private HttpMethod stepCaptcha(final String action) throws Exception {
         if (getContentAsString().contains("recaptcha")) {
             logger.info("Handling ReCaptcha");
 
             final Matcher m = getMatcherAgainstContent("api.recaptcha.net/noscript\\?k=([^\"]+)\"");
             if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
             final String reCaptchaKey = m.group(1);
 
             final String content = getContentAsString();
             final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
             final CaptchaSupport captchaSupport = getCaptchaSupport();
 
             final String captchaURL = r.getImageURL();
             logger.info("Captcha URL " + captchaURL);
 
             final String captcha = captchaSupport.getCaptcha(captchaURL);
             if (captcha == null) throw new CaptchaEntryInputMismatchException();
             r.setRecognized(captcha);
 
             return r.modifyResponseMethod(
                     getMethodBuilder(content)
                             .setReferer(action)
                             .setActionFromFormByIndex(3, true)
                             .setAction(action)
             ).toPostMethod();
         } else {
             logger.info("Handling regular captcha");
 
             final CaptchaSupport captchaSupport = getCaptchaSupport();
             final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
             logger.info("Captcha URL " + captchaSrc);
 
             final String captcha;
             if (captchaCounter <= CAPTCHA_MAX) {
                 captcha = CaptchaReader.recognize(captchaSupport.getCaptchaImage(captchaSrc));
                 if (captcha == null) {
                     logger.info("Could not separate captcha letters, attempt " + captchaCounter + " of " + CAPTCHA_MAX);
                 }
                 logger.info("OCR recognized " + captcha + ", attempt " + captchaCounter + " of " + CAPTCHA_MAX);
                 captchaCounter++;
             } else {
                 captcha = captchaSupport.getCaptcha(captchaSrc);
                 if (captcha == null) throw new CaptchaEntryInputMismatchException();
                 logger.info("Manual captcha " + captcha);
             }
 
             return getMethodBuilder()
                     .setReferer(action)
                     .setActionFromFormWhereTagContains("captcha", true)
                     .setAction(action)
                     .setParameter("captcha_response", captcha)
                     .toPostMethod();
         }
     }
 
 }
