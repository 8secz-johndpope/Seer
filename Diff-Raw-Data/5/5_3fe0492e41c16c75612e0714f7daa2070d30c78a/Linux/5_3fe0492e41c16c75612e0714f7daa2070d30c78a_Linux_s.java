 package ru.naumen.servacc.platform;
 
 import java.io.File;
 import java.io.IOException;
 import java.text.MessageFormat;
 
 /**
  * @author Andrey Hitrin
  * @since 08.02.12
  */
 public class Linux implements Platform
 {
     @Override
     public void openTerminal(Object[] params) throws IOException {
         Runtime.getRuntime().exec(MessageFormat.format("putty {2} -telnet {0} -P {1,number,#}", params));
     }
 
     @Override
     public void openFTPBrowser(Object[] params) throws IOException {
        Runtime.getRuntime().exec(MessageFormat.format("gftp ftp://anonymous@{0}:{1,number,#}", params));
     }
 
     @Override
     public void openInBrowser(String url) throws IOException {
        Runtime.getRuntime().exec("firefox " + url);
     }
 
     @Override
     public File getConfigFile() throws IOException {
         final String userHome = System.getProperty("user.home");
         return new File(userHome, ".serveraccess");
     }
 
     @Override
     public boolean isTraySupported() {
         return false;
     }
 
     @Override
     public boolean needToNegotiateProtocolOptions() {
         return false;
     }
 
     @Override
     public boolean useSystemSearchWidget() {
         return false;
     }
 
     @Override
     public boolean displayFolderIcon() {
         return true;
     }
 }
