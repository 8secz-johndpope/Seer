 package org.openqa.grid.web.servlet;
 
 import com.google.common.io.ByteStreams;
 
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Properties;
 import java.util.logging.Logger;
 
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 public class DisplayHelpServlet extends HttpServlet {
 
   private static final long serialVersionUID = 8484071790930378855L;
   private static final Logger log = Logger.getLogger(ConsoleServlet.class.getName());
   private static String coreVersion;
   private static String coreRevision;
 
   public DisplayHelpServlet() {
     getVersion();
   }
 
 
   protected void doGet(HttpServletRequest request, HttpServletResponse response)
       throws ServletException, IOException {
     process(request, response);
   }
 
   protected void doPost(HttpServletRequest request, HttpServletResponse response)
       throws ServletException, IOException {
     process(request, response);
   }
 
   protected void process(HttpServletRequest request, HttpServletResponse response)
       throws IOException {
     response.setContentType("text/html");
     response.setCharacterEncoding("UTF-8");
     response.setStatus(200);
 
     StringBuilder builder = new StringBuilder();
 
     builder.append("<html>");
     builder.append("<head>");
 
     builder.append("<title>Selenium Grid2.0 help</title>");
 
 
     builder.append("</head>");
 
     builder.append("<body>");
    builder.append("You are using grid ").append(coreVersion).append(coreRevision);
     builder
        .append("<br>Find help on the official selenium wiki : <a href='http://code.google.com/p/selenium/wiki/Grid2' >more help here</a>");
    builder.append("<br>default monitoring page : <a href='/grid/console' >console</a>");
    
     builder.append("</body>");
     builder.append("</html>");
 
     InputStream in = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
     try {
       ByteStreams.copy(in, response.getOutputStream());
     } finally {
       in.close();
       response.getOutputStream().close();
     }
   }
 
   private void getVersion() {
     final Properties p = new Properties();
 
     InputStream stream =
         Thread.currentThread().getContextClassLoader().getResourceAsStream("VERSION.txt");
     if (stream == null) {
       log.severe("Couldn't determine version number");
       return;
     }
     try {
       p.load(stream);
     } catch (IOException e) {
       log.severe("Cannot load version from VERSION.txt" + e.getMessage());
     }
     coreVersion = p.getProperty("selenium.core.version");
     coreRevision = p.getProperty("selenium.core.revision");
     if (coreVersion == null) {
       log.severe("Cannot load selenium.core.version from VERSION.txt");
     }
   }
 }
