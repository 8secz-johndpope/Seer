 package org.eclipse.jetty.embedded;
 
 import org.eclipse.jetty.server.Server;
 import org.eclipse.jetty.util.resource.Resource;
 import org.eclipse.jetty.xml.XmlConfiguration;
 
 
 /* ------------------------------------------------------------ */
 /** A Jetty FileServer.
  * This server is identical to {@link FileServer}, except that it
  * is configured via an {@link XmlConfiguration} config file that
  * does the identical work.
  * <p>
 * See <a href="http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/example-jetty-embedded/src/main/resources/fileserver.xml">fileserver.xml</a>
  */
 public class FileServerXml
 {
     public static void main(String[] args) throws Exception
     {
         Resource fileserver_xml = Resource.newSystemResource("fileserver.xml");
         XmlConfiguration configuration = new XmlConfiguration(fileserver_xml.getInputStream());
         Server server = (Server)configuration.configure();
         server.start();
         server.join();
     }
 }
