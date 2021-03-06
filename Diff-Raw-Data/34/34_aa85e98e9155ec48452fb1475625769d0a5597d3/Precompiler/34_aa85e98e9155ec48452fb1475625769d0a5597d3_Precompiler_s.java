 /*
  *  Weblounge: Web Content Management System
  *  Copyright (c) 2010 The Weblounge Team
  *  http://weblounge.o2it.ch
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2
  *  of the License, or (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software Foundation
  *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
 
 package ch.o2it.weblounge.dispatcher.impl;
 
 import ch.o2it.weblounge.common.impl.testing.MockHttpServletRequest;
 import ch.o2it.weblounge.common.impl.testing.MockHttpServletResponse;
 import ch.o2it.weblounge.common.impl.url.UrlUtils;
 import ch.o2it.weblounge.common.site.Site;
 
 import org.osgi.framework.Bundle;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.net.URL;
 import java.util.Enumeration;
 
 /**
  * This precompiler searches an OSGi bundle for Java Server Pages (JSP) and
  * sends a request to <code>JspC</code>, the java server page compiler provided
  * by Jasper.
  */
 public class Precompiler {
 
   /** Logging facility */
   protected static final Logger logger = LoggerFactory.getLogger(SiteDispatcherServiceImpl.class);
 
   /** The site servlet */
   protected SiteServlet servlet = null;
 
   /** The worker */
   protected PrecompileWorker worker = null;
 
   /** Flag to indicate whether to keep working or not */
   protected boolean keepGoing = true;
 
   /** Switch for precompiler error logging */
   protected boolean logErrors = true;
 
   /**
    * Creates a new precompiler for the site identified by the servlet.
    * 
    * @param servlet
    *          the site servlet
    * @param logErrors
    *          <code>true</code> to log precompilation errors
    */
   public Precompiler(SiteServlet servlet, boolean logErrors) {
     this.servlet = servlet;
     this.logErrors = logErrors;
   }
 
   /**
    * Precompiles all of the bundle's server pages to the output directory as
    * specified in the <code>scratchdir</code> setting of the compiler
    * configuration.
    * 
    * @param outputDir
    *          the path to the output directory
    */
   public void precompile() {
     worker = new PrecompileWorker(servlet);
     Thread workerThread = new Thread(worker);
     workerThread.setPriority(Thread.MIN_PRIORITY);
     workerThread.setDaemon(true);
     workerThread.start();
   }
 
   /**
    * Stops the current precompilation work.
    */
   public void stop() {
     logger.info("Asking precompiler for '{}' to stop", servlet.getSite());
     keepGoing = false;
   }
 
   class PrecompileWorker implements Runnable {
 
     /** The servlet to use */
     private SiteServlet servlet = null;
 
     /**
      * Creates a new jsp precompilation worker.
      * 
      * @param servlet
      *          the servlet
      */
     public PrecompileWorker(SiteServlet servlet) {
       this.servlet = servlet;
     }
 
     /**
      * {@inheritDoc}
      * 
      * @see java.lang.Runnable#run()
      */
     @SuppressWarnings("unchecked")
     public void run() {
 
       Site site = servlet.getSite();
       Bundle bundle = servlet.getBundle();
       String bundlePath = servlet.getBundleContext().getBundlePath();
       String httpContextURI = servlet.getBundleContext().getSiteURI();
 
       // Prepare the mock request and response objects
       MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
       request.setLocalAddr(site.getURL().toExternalForm());
       request.setServletPath(httpContextURI);
       MockHttpServletResponse response = new MockHttpServletResponse();
 
       // Collect all jsp files and ask for precompilation
       Enumeration<URL> jspEntries = bundle.findEntries(bundlePath, "*.jsp", true);
       if (jspEntries == null) {
         logger.debug("No java server pages found to precompile for {}", site);
         return;
       }
 
       logger.info("Precompiling java server pages for '{}'", site);
       int errorCount = 0;
       while (keepGoing && jspEntries.hasMoreElements()) {
         URL entry = jspEntries.nextElement();
         String path = entry.getPath();
         String pathInfo = path.substring(path.indexOf(bundlePath) + bundlePath.length());
         request.setPathInfo(pathInfo);
         request.setRequestURI(UrlUtils.concat(httpContextURI, pathInfo));
         try {
           logger.debug("Precompiling {}:/{}", site, pathInfo);
           servlet.service(request, response);
         } catch (Throwable t) {
           if (logErrors)
             logger.warn("Error precompiling " + site + ":/" + pathInfo, t);
          errorCount ++;
         }
       }
 
       // Log the precompilation results
       if (!keepGoing) {
         logger.info("Precompilation for '{}' canceled", site);
       } else if (!logErrors && errorCount > 0) {
         String compilationResult = "finished";
         compilationResult += " with " + errorCount + " errors";
         logger.warn("Precompilation for '{}' {}", site, compilationResult);
         if (!logErrors)
           logger.info("Precompilation error logging can be enabled in the site dispatcher service");
       } else {
         logger.info("Precompilation for '{}' finished", site);
       }
     }
 
   }
 
 }
