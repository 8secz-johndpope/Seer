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
         final Matcher filenameMatcher = getMatcherAgainstContent("<title>\\s+Download (.+?). Free download");
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
 
             while (getContentAsString().contains("captcha")) {
                 if (!makeRedirectedRequest(stepCaptcha(method.getURI().toString()))) {
                     checkProblems();
                     throw new ServiceConnectionProblemException();
                 }
                 checkProblems();
             }
 
            matcher = getMatcherAgainstContent("(min(?:Time)?Limit)\\s*:\\s*(\\d+)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Wait time not found");
            }
            final String timeVariable = matcher.group(1);
            int waitTime = Integer.parseInt(matcher.group(2));

            matcher = getMatcherAgainstContent("Timeout\\." + timeVariable + "\\s*=\\s*Timeout\\." + timeVariable + "\\s*/\\s*(\\d+)");
            if (matcher.find()) { // there is a divisor
                final int waitTimeDivider = Integer.parseInt(matcher.group(1));
                 waitTime = waitTime / waitTimeDivider;
             }
 
             method = getMethodBuilder()
                     .setReferer(method.getURI().toString())
                    .setActionFromTextBetween("var url = \"", "\"")
                     .toGetMethod();
             method.addRequestHeader("X-Requested-With", "XMLHttpRequest");
 
             downloadTask.sleep(waitTime);
 
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
         final String contentAsString = getContentAsString();
         Matcher err404Matcher = PlugUtils.matcher("<div class=\"text-404\">(.*?)</div", contentAsString);
         if (err404Matcher.find()) {
             if (err404Matcher.group(1).contains("\u00d0\u2014\u00d0\u00b0\u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d1?\u00d0\u00b5\u00d0\u00bd\u00d0\u00bd\u00d1\u2039\u00d0\u00b9 \u00d0\u00b4\u00d0\u00be\u00d0\u00ba\u00d1?\u00d0\u00bc\u00d0\u00b5\u00d0\u00bd\u00d1\u201a \u00d0\u00bd\u00d0\u00b5 \u00d0\u00bd\u00d0\u00b0\u00d0\u00b9\u00d0\u00b4\u00d0\u00b5\u00d0\u00bd"))
                 throw new URLNotAvailableAnymoreException(err404Matcher.group(1));
         }
         if (contentAsString.contains("\u0424\u0430\u0439\u043B \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D")
                 || contentAsString.contains("File was not found") || contentAsString.contains("Probably it was deleted"))
             throw new URLNotAvailableAnymoreException("File not found");
     }
 
     private void checkDownloadProblems() throws ErrorDuringDownloadingException {
         try {
             Matcher waitMatcher = PlugUtils.matcher("\u00d0\u0178\u00d0\u00be\u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d0\u00b1\u00d1?\u00d0\u00b9\u00d1\u201a\u00d0\u00b5\\s+\u00d0\u00bf\u00d0\u00be\u00d0\u00b2\u00d1\u201a\u00d0\u00be\u00d1\u20ac\u00d0\u00b8\u00d1\u201a\u00d1\u0152.*<span id='timeout'>([^>]*)<", getContentAsString());
             if (waitMatcher.find()) {
                 throw new YouHaveToWaitException("You have to wait", Integer.valueOf(waitMatcher.group(1)));
             }
             Matcher errMatcher = PlugUtils.matcher("<div[^>]*class='error'[^>]*>([^<]*)<", getContentAsString());
             if (errMatcher.find() && !errMatcher.group(1).isEmpty()) {
                 if (errMatcher.group(1).contains("\u00d0?\u00d0\u00b5\u00d0\u00b2\u00d0\u00b5\u00d1\u20ac\u00d0\u00bd\u00d1\u2039\u00d0\u00b9 \u00d0\u00be\u00d1\u201a\u00d0\u00b2\u00d0\u00b5\u00d1\u201a")
                         || errMatcher.group(1).contains("\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u043e\u0442\u0432\u0435\u0442!"))
                     throw new CaptchaEntryInputMismatchException();
                 throw new PluginImplementationException();
             }
             if (getContentAsString().contains("The file is not avaliable now because of technical problems")) {
                 throw new ServiceConnectionProblemException("The file is not available now because of technical problems");
             }
             if (getContentAsString().contains("\u00d0\u00a1\u00d1?\u00d1\u2039\u00d0\u00bb\u00d0\u00ba\u00d0\u00b0 \u00d0\u00bf\u00d1\u20ac\u00d0\u00be\u00d1?\u00d1\u20ac\u00d0\u00be\u00d1\u2021\u00d0\u00b5\u00d0\u00bd\u00d0\u00b0")) // it's unlikely we get this...
                 throw new YouHaveToWaitException("Trying again...", 10);
         } catch (NumberFormatException e) {
             throw new PluginImplementationException();
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
