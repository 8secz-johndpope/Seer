 /*
  * $Source$
  * $Revision$
  *
  * Copyright (C) 2000 Tim Joyce
  *
  * Part of Melati (http://melati.org), a framework for the rapid
  * development of clean, maintainable web applications.
  *
  * Melati is free software; Permission is granted to copy, distribute
  * and/or modify this software under the terms either:
  *
  * a) the GNU General Public License as published by the Free Software
  *    Foundation; either version 2 of the License, or (at your option)
  *    any later version,
  *
  *    or
  *
  * b) any version of the Melati Software License, as published
  *    at http://melati.org
  *
  * You should have received a copy of the GNU General Public License and
  * the Melati Software License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA to obtain the
  * GNU General Public License and visit http://melati.org to obtain the
  * Melati Software License.
  *
  * Feel free to contact the Developers of Melati (http://melati.org),
  * if you would like to work out a different arrangement than the options
  * outlined here.  It is our intention to allow Melati to be used by as
  * wide an audience as possible.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * Contact details for copyright holder:
  *
  *     Tim Joyce <timj@paneris.org>
  *     http://paneris.org/
  *     68 Sandbanks Rd, Poole, Dorset. BH14 8BY. UK
  */
 
 /*
  * Config Servlet is the simplest way to use Melati.
  *
  * All a ConfigServlet does is to configure a melati and combine the
  * doGet and doPost methods.  Importantly it does not establish a poem session
  * leaving you to do this for yourself.
  *
  * if you want a poem session established, please extend PoemServlet
  *
  * <A NAME=pathinfoscan>ConfigServlet does set up a basic
  * MelatiContext with the Method set,
  * but not the POEM logicaldatabase, table or troid
  *
  * The URL is expected to take one of the following form:
  *
  * <BLOCKQUOTE><TT>
  * http://<I>h</I>/<I>s</I>/<I>meth</I>
  * </TT></BLOCKQUOTE>
  *
  * the method is broken out of the path info and passed to
  * your application code in the <TT>Melati</TT> and
  * <TT>MelatiContext</TT> parameter
  *
  * <TABLE>
  *   <TR>
  *     <TD><TT><I>h</I></TT></TD>
  *     <TD>host name, such as <TT>www.melati.org</TT></TD>
  *   </TR>
  *   <TR>
  *     <TD><TT><I>s</I></TT></TD>
  *     <TD>
  *       servlet-determining part, such as
  *       <TT>melati/org.melati.admin.Admin</TT>
  *     </TD>
  *   </TR>
  *   <TR>
  *     <TD><TT><I>meth</I></TT></TD>
  *     <TD>
  *       A freeform string telling your servlet what it is meant to do.  This
  *       is automatically made available in templates as
  *       <TT>$melati.Method</TT>.
  *     </TD>
  *   </TR>
  * </TABLE>
  *
  * You can change the way these things are determined by overriding
  * <TT>melatiContext</TT>.
  */
 
 package org.melati.servlet;
 
 import java.io.StringWriter;
 import java.io.PrintWriter;
 import java.io.IOException;
 
 import javax.servlet.ServletConfig;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServlet;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 
 import org.melati.Melati;
 import org.melati.MelatiConfig;
 import org.melati.util.ConnectionPendingException;
 import org.melati.util.MelatiException;
 import org.melati.util.MelatiLocale;
 import org.melati.util.StringUtils;
 import org.melati.util.MelatiWriter;
 
 public abstract class ConfigServlet extends HttpServlet {
 
   // the melati
   protected MelatiConfig melatiConfig;
 
   /**
    * Inititialise Melati
    * @param ServletConfig
    */
 
   public void init(ServletConfig config) throws ServletException {
     super.init(config);
     try {
       melatiConfig = melatiConfig();
     } catch (MelatiException e) {
       // log it to system.err as ServletExceptions go to the
       // servlet runner log (eg jserv.log), and don't have a stack trace!
       e.printStackTrace(System.err);
       throw new ServletException(e.toString ());
     }
   }
 
   /**
    * Handles GET
    */
 
   public void doGet(HttpServletRequest request, HttpServletResponse response)
       throws ServletException, IOException {
     doGetPostRequest(request, response);
   }
 
   /**
    * Handle a POST
    */
 
   public void doPost(HttpServletRequest request, HttpServletResponse response)
       throws ServletException, IOException {
     doGetPostRequest(request, response);
   }
 
   /**
    * Process the request.
    */
 
   private void doGetPostRequest(final HttpServletRequest request, 
                                 final HttpServletResponse response)
       throws IOException {
     try {
       Melati melati = melatiConfig.getMelati(request, response);
       try {
         MelatiContext melatiContext = melatiContext(melati);
         melati.setContext(melatiContext);
         doConfiguredRequest(melati);
         // send the output to the client
         melati.write();
       }
       catch (Exception f) {
         error(melati,f);
       }
     }
     catch (Exception e) {
       // log it
       e.printStackTrace(System.err);
     }
   }
 
   /**
    * Send an error message
    */
 
   public void error(Melati melati, Exception e ) throws IOException {
     // has it been trapped already, if so, we don't need to relog it here
     if (! (e instanceof TrappedException)) {
       // log it
       e.printStackTrace(System.err);
       // and put it on the page
       melati.getResponse().setContentType ("text/html");
       MelatiWriter mw =  melati.getWriter();
       // get rid of anything that has been written so far
       mw.reset();
       PrintWriter out = mw.getPrintWriter();
       if (e instanceof ConnectionPendingException) {
         writeConnectionPendingException(out,e);
       } else {
         writeError(out,e);
       }
       melati.write();
     }
   }
   
   public void writeError(PrintWriter out, Exception e) {
     out.println("<html><head><title>Melati Error</title></head>");
     out.println("<!-- HTML generated in org.melati.servlet.ConfigServlet.java -->");
     out.println("<body><h2>Melati Error</h2>");
    // This is rarely called, eg when the template engine 
    // fails to render the default error template
    out.println("<h3>Reported from ConfigServlet</h3>");
     out.println("<p>An error has occured in the application"); 
     out.println("that runs this website, please contact <a href='mailto:");
     out.println(getSysAdminEmail() + "'>" + getSysAdminName() + "</a>");
     out.println(", with the information given below.</p>");
     out.println("<h4><font color=red><pre>" );
     e.printStackTrace(out);
     out.println("</pre></font></h4></body></html>");
   }    
   
   public void writeConnectionPendingException(PrintWriter out, Exception e) {
     out.println("<html><head><title>Database Initialising</title><META HTTP-EQUIV='Refresh' CONTENT='30'></head>");
     out.println("<!-- HTML generated in org.melati.servlet.ConfigServlet.java -->");
     out.println("<body><center><h2>Database Initialising</h2><p>&nbsp;</p>");
     out.println("<p>Sorry, but the database that runs this website is just starting up.");
     out.println("This takes a few seconds, so you should be able to use the site in a moment.");
     out.println("<p>This page will refresh in 30 seconds, and you will be able to continue.</p>");
     out.println("<!--" );
     e.printStackTrace(out);
     out.println("--></center></body></html>");
   }    
 
   /*
    * Please override these settings.
    */
 
   public String getSysAdminName () {
     return "nobody";
   }
 
   public String getSysAdminEmail () {
     return "nobody@nobody.com";
   }
 
   protected MelatiContext melatiContext(Melati melati) 
    throws PathInfoException {
      MelatiContext it = new MelatiContext();
      String[] parts = melati.getPathInfoParts();
      if (parts.length > 0)
       it.method = StringUtils.nulled(parts[parts.length - 1]);
     return it;
   }
   
   /** 
    * To override any setting from MelatiServlet.properties,
    * simply override this method and return a vaild MelatiConfig.
    *
    * eg to use a different AccessHandler from the default:
    *
    * <PRE>
    *   protected MelatiConfig melatiConfig() throws MelatiException {
    *     MelatiConfig config = super.melatiConfig();
    *     config.setAccessHandler(new YourAccessHandler());
    *     return config;
    *   }
    * </PRE>
    *
    */
 
   protected MelatiConfig melatiConfig() throws MelatiException {
     return new MelatiConfig();
   }
   
   /**
    * Override this method to build up your output
    * @param melati
    */
 
   protected abstract void doConfiguredRequest (Melati melati)
       throws Exception;
 }
