 package cz.vity.freerapid.plugins.services.euroshare;
 
 import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
 import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
 import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
 import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
 import cz.vity.freerapid.plugins.webclient.AbstractRunner;
 import cz.vity.freerapid.plugins.webclient.FileState;
 import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
 import org.apache.commons.httpclient.HttpMethod;
 import org.apache.commons.httpclient.methods.GetMethod;
 
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 
 /**
  * Class which contains main code
  *
  * @author birchie
  */
 class EuroShareFileRunner extends AbstractRunner {
     private final static Logger logger = Logger.getLogger(EuroShareFileRunner.class.getName());
 
     @Override
     public void runCheck() throws Exception { //this method validates file
         super.runCheck();
         final GetMethod getMethod = getGetMethod(fileURL);//make first request
         if (makeRedirectedRequest(getMethod)) {
             checkProblems();
             checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher match = PlugUtils.matcher("<h1.*?>(.+?) \\((.+?)\\)</h1>", content);
         if (!match.find())
             throw new PluginImplementationException("File name/size not found");
         httpFile.setFileName(match.group(1).trim());
         httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2).trim()));
         httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
     }
 
     @Override
     public void run() throws Exception {
         super.run();
         logger.info("Starting download in TASK " + fileURL);
         final GetMethod method = getGetMethod(fileURL); //create GET request
         if (makeRedirectedRequest(method)) { //we make the main request
             final String contentAsString = getContentAsString();//check for response
             checkProblems();//check problems
             checkNameAndSize(contentAsString);//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("STIAHNUŤ AKO FREE").toHttpMethod();
 
             //here is the download link extraction
             if (!tryDownloadAndSaveFile(httpMethod)) {
                 checkProblems();//if downloading failed
                 throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
             }
         } else {
             checkProblems();
             throw new ServiceConnectionProblemException();
         }
     }
 
     private void checkProblems() throws ErrorDuringDownloadingException {
         final String contentAsString = getContentAsString();
        if (contentAsString.contains("Požadovaný súbor sa na serveri nenachádza alebo bol odstránený") ||
                contentAsString.contains("Súbor neexistuje")) {
             throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
         }
     }
 
 }
