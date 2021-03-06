 package de.matrixweb.smaller.servlet;
 
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.util.Set;
 
 import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
 import javax.servlet.ServletContext;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.apache.commons.lang3.StringUtils;
 
 import de.matrixweb.smaller.common.Task;
 import de.matrixweb.smaller.pipeline.Pipeline;
 import de.matrixweb.smaller.pipeline.Result;
 import de.matrixweb.smaller.resource.impl.JavaEEProcessorFactory;
 
 /**
  * @author markusw
  */
 public class EmbeddedSmaller {
 
   private FilterConfig filterConfig;
 
  private ServletConfig servletConfig;
 
   private Result result;
 
   /**
    * @param filterConfig
    */
   public EmbeddedSmaller(final FilterConfig filterConfig) {
     this.filterConfig = filterConfig;
   }
 
   /**
   * @param servletConfig
    */
  public EmbeddedSmaller(final ServletConfig servletConfig) {
    this.servletConfig = servletConfig;
   }
 
   /**
    * @throws ServletException
    */
   public void init() throws ServletException {
     if (!isDevelopment()) {
       process();
     }
   }
 
   /**
    * @param request
    * @param response
    * @throws ServletException
    * @throws IOException
    */
   public void execute(final HttpServletRequest request,
       final HttpServletResponse response) throws ServletException, IOException {
     if (isDevelopment()) {
       process();
     }
     String contentType = request.getContentType();
     if (contentType == null) {
       if (request.getRequestURI().endsWith("js")) {
         contentType = "text/javascript";
       } else if (request.getRequestURI().endsWith("css")) {
         contentType = "text/css";
       }
     }
     response.setContentType(contentType);
     final PrintWriter writer = response.getWriter();
     if ("text/javascript".equals(contentType)) {
       writer.print(this.result.getJs().getContents());
     } else if ("text/css".equals(contentType)) {
       writer.print(this.result.getCss().getContents());
     }
     writer.close();
   }
 
   private String getInitParameter(final String name) {
    if (this.servletConfig != null) {
      return this.servletConfig.getInitParameter(name);
     }
     return this.filterConfig.getInitParameter(name);
   }
 
   private ServletContext getServletContext() {
    if (this.servletConfig != null) {
      return this.servletConfig.getServletContext();
     }
     return this.filterConfig.getServletContext();
   }
 
   private boolean isDevelopment() {
     return "development".equals(getInitParameter("mode"));
   }
 
   private void process() throws ServletException {
     final String processors = getInitParameter("processors");
     if (processors == null) {
       throw new ServletException("init-param 'processors' must be configured");
     }
     final String includes = getInitParameter("includes");
     if (StringUtils.isBlank(includes)) {
       throw new ServletException("init-param 'includes' must be configured");
     }
     final String excludes = getInitParameter("excludes");
     final Set<String> resources = new ResourceScanner(getServletContext(),
         includes.split("[, ]"), excludes != null ? excludes.split("[, ]")
             : new String[] {}).getResources();
 
     final Task task = new Task();
     task.setProcessor(processors);
     task.setIn(resources.toArray(new String[resources.size()]));
     this.result = new Pipeline(new JavaEEProcessorFactory()).execute(
         new ServletContextResourceResolver(getServletContext()), task);
   }
 
 }
