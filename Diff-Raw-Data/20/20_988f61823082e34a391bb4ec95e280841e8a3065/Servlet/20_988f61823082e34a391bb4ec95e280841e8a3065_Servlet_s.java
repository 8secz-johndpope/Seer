 package java2xtend.webapp;
 
 import java.io.IOException;
 import java.util.logging.Logger;
 import javax.servlet.ServletException;
 import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import org.apache.commons.io.IOUtils;
 import org.eclipse.xtend.java2xtend.Java2Xtend;
 import org.eclipse.xtext.xbase.lib.Functions.Function0;
 
 @SuppressWarnings("all")
 public class Servlet extends HttpServlet {
   private final static Logger log = new Function0<Logger>() {
     public Logger apply() {
       String _name = Servlet.class.getName();
       Logger _logger = Logger.getLogger(_name);
       return _logger;
     }
   }.apply();
   
   protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
     Java2Xtend _java2Xtend = new Java2Xtend();
     final Java2Xtend conv = _java2Xtend;
     final ServletInputStream in = req.getInputStream();
     try {
       final String java = IOUtils.toString(in);
       Servlet.log.info(java);
       final String xtend = conv.toXtend(java);
       Servlet.log.info(xtend);
      resp.setContentType("text/plain");
      ServletOutputStream _outputStream = resp.getOutputStream();
      _outputStream.print(xtend);
     } finally {
       IOUtils.closeQuietly(in);
     }
   }
 }
