 package cz.vity.freerapid.plugins.services.wowebookcom;
 
 import cz.vity.freerapid.plugins.dev.PluginDevApplication;
 import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
 import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
 import org.jdesktop.application.Application;
 
 import java.net.URL;
 
 /**
  * @author CrazyCoder
  */
 public class TestApp extends PluginDevApplication {
     @Override
     protected void startup() {
         final HttpFile httpFile = getHttpFile();
         try {
            httpFile.setNewURL(new URL("http://www.wowebook.info/book/vagrant-up-and-running/"));
             final ConnectionSettings connectionSettings = new ConnectionSettings();
             //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
             final WowEbookComServiceImpl service = new WowEbookComServiceImpl();
             testRun(service, httpFile, connectionSettings);
         } catch (Exception e) {
             e.printStackTrace();
         }
         this.exit();
     }
 
     /**
      * Main start method for running this application
      * Called from IDE
      *
      * @param args arguments for application
      */
     public static void main(String[] args) {
         Application.launch(TestApp.class, args);
     }
 }
