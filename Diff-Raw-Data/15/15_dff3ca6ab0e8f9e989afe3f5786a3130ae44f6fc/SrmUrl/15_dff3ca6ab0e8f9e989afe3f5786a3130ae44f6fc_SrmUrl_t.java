 /**
  * Trivial extension of GlobusURL class to handle default port
  * 
  */
 package gov.fnal.srm.util;
 
 import java.net.MalformedURLException;
 import org.globus.util.GlobusURL;
 
 public class SrmUrl  extends GlobusURL { 
 
        private int defaultPort=8443;
         public SrmUrl(String url) 
                 throws MalformedURLException { 
                 super(url);
                 massagePath();
         }
         
         public  SrmUrl(String url,
                       int defaultPortNumber) 
                 throws MalformedURLException { 
                 super(url);
                 if (!getProtocol().equals("file")) {
                        if (super.getPort()==-1) { 
                                defaultPort=defaultPortNumber;
                        }
                 }
 //                massagePath();
         }
 
         /**
          * The purpose of this call is to handle "///"
          */
     
         private void massagePath() {
                 if (getProtocol().equals("file")) { 
                         if (!urlPath.startsWith("/")) { 
                                 urlPath="/"+urlPath;
                         }
                 }
         }
        
         public void setPort(int portNumber) { 
                 port=portNumber;
         }
         
         public int getPort() { 
                 int p= super.getPort();
                 if (p==-1&&super.getProtocol().equals("srm")) {
                        return defaultPort;
                 }
                 return p;
         }
 }
