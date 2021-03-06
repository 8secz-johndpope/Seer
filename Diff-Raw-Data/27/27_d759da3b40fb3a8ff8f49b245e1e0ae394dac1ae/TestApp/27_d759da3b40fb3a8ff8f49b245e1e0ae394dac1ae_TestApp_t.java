 package cz.vity.freerapid.plugins.services.rapidgator;
 
 import cz.vity.freerapid.plugins.dev.PluginDevApplication;
 import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
 import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
 import org.jdesktop.application.Application;
 
 import java.net.URL;
 
 /**
 * @author tong2shot, Abinash Bishoyi
  */
 public class TestApp extends PluginDevApplication {
     @Override
     protected void startup() {
         final HttpFile httpFile = getHttpFile();
         try {
            httpFile.setNewURL(new URL("http://rapidgator.net/file/193818d17e4ede41fb1a9b01186b10bb/Ravi_Shankar_-_The_Master.softarchive.net.part1.rar.html"));
            //httpFile.setNewURL(new URL("http://rapidgator.net/file/12111436"));
             //httpFile.setNewURL(new URL("http://rapidgator.net/folder/1251306/M+ki.html"));    // folder
             //httpFile.setNewURL(new URL("http://rapidgator.net/file/3575395")); // > 1 GB
             final ConnectionSettings connectionSettings = new ConnectionSettings();
             //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
             final RapidGatorServiceImpl service = new RapidGatorServiceImpl();
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
