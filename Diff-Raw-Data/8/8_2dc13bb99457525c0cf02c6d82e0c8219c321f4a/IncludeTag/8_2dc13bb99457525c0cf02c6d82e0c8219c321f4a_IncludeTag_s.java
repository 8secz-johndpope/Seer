 /*
 * $Id: IncludeTag.java,v 1.8 2001/01/10 22:05:24 craigmcc Exp $
  * ====================================================================
  *
  * The Apache Software License, Version 1.1
  *
  * Copyright (c) 1999 The Apache Software Foundation.  All rights
  * reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  * 1. Redistributions of source code must retain the above copyright
  *    notice, this list of conditions and the following disclaimer.
  *
  * 2. Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in
  *    the documentation and/or other materials provided with the
  *    distribution.
  *
  * 3. The end-user documentation included with the redistribution, if
  *    any, must include the following acknowlegement:
  *       "This product includes software developed by the
  *        Apache Software Foundation (http://www.apache.org/)."
  *    Alternately, this acknowlegement may appear in the software itself,
  *    if and wherever such third-party acknowlegements normally appear.
  *
  * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
  *    Foundation" must not be used to endorse or promote products derived
  *    from this software without prior written permission. For written
  *    permission, please contact apache@apache.org.
  *
  * 5. Products derived from this software may not be called "Apache"
  *    nor may "Apache" appear in their names without prior written
  *    permission of the Apache Group.
  *
  * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
  * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
  * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
  * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
  * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  * ====================================================================
  *
  * This software consists of voluntary contributions made by many
  * individuals on behalf of the Apache Software Foundation.  For more
  * information on the Apache Software Foundation, please see
  * <http://www.apache.org/>.
  *
  * [Additional notices, if required by prior licensing conditions]
  *
  */
 
 package org.apache.struts.taglib.bean;
 
 
 import java.io.BufferedInputStream;
 import java.io.InputStreamReader;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.net.URLConnection;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpSession;
 import javax.servlet.jsp.JspException;
 import javax.servlet.jsp.PageContext;
 import javax.servlet.jsp.tagext.TagSupport;
 import org.apache.struts.action.Action;
 import org.apache.struts.action.ActionForward;
 import org.apache.struts.action.ActionForwards;
 import org.apache.struts.util.MessageResources;
 import org.apache.struts.util.RequestUtils;
 
 
 /**
  * Define the contents of a specified intra-application request as a
  * page scope attribute of type <code>java.lang.String</code>.  If the
  * current request is part of a session, the session identifier will be
  * included in the generated request, so it will be part of the same
  * session.
  * <p>
  * <strong>FIXME</strong>:  In a servlet 2.3 environment, we can use a
  * wrapped response passed to RequestDispatcher.include().
  *
  * @author Craig R. McClanahan
 * @version $Revision: 1.8 $ $Date: 2001/01/10 22:05:24 $
  */
 
 public class IncludeTag extends TagSupport {
 
 
     // ------------------------------------------------------------- Properties
 
 
     /**
      * Buffer size to use when reading the input stream.
      */
     protected static final int BUFFER_SIZE = 256;
 
 
     /**
      * The name of the global <code>ActionForward</code> that contains a
      * path to our requested resource.
      */
     protected String forward = null;
 
     public String getForward() {
         return (this.forward);
     }
 
     public void setForward(String forward) {
         this.forward = forward;
     }
 
 
     /**
      * The absolute URL to the resource to be included.
      */
     protected String href = null;
 
     public String getHref() {
         return (this.href);
     }
 
     public void setHref(String href) {
         this.href = href;
     }
 
 
     /**
      * The name of the scripting variable that will be exposed as a page
      * scope attribute.
      */
     protected String id = null;
 
     public String getId() {
         return (this.id);
     }
 
     public void setId(String id) {
         this.id = id;
     }
 
 
     /**
      * The message resources for this package.
      */
     protected static MessageResources messages =
         MessageResources.getMessageResources
         ("org.apache.struts.taglib.bean.LocalStrings");
 
 
     /**
      * Deprecated method to set the "name" attribute, which has been
      * replaced by the "page" attribute.
      *
      * @deprecated use setPage(String) instead
      */
     public void setName(String name) {
         this.page = name;
     }
 
 
     /**
      * The context-relative URI of the page or servlet to be included.
      */
     protected String page = null;
 
     public String getPage() {
 	return (this.page);
     }
 
     public void setPage(String page) {
 	this.page = page;
     }
 
 
     // --------------------------------------------------------- Public Methods
 
 
     /**
      * Define the contents returned for the specified resource as a
      * page scope attribute.
      *
      * @exception JspException if a JSP error occurs
      */
     public int doStartTag() throws JspException {
 
 	// Set up a URLConnection to read the requested resource
         URL url = hyperlink();
 	URLConnection conn = null;
 	try {
 	    conn = url.openConnection();
 	    conn.setAllowUserInteraction(false);
 	    conn.setDoInput(true);
 	    conn.setDoOutput(false);
 	    conn.connect();
 	} catch (Exception e) {
             pageContext.setAttribute(Action.EXCEPTION_KEY, e,
                                      PageContext.REQUEST_SCOPE);
 	    throw new JspException
                 (messages.getMessage("include.open",
                                      url.toString(), e.toString()));
 	}
 
 	// Copy the contents of this URL
         StringBuffer sb = new StringBuffer();
 	try {
 	    BufferedInputStream is =
 		new BufferedInputStream(conn.getInputStream());
 	    InputStreamReader in = new InputStreamReader(is); // FIXME - encoding
             char buffer[] = new char[BUFFER_SIZE];
             int n = 0;
 	    while (true) {
                 n = in.read(buffer);
                 if (n < 1)
                     break;
                 sb.append(buffer, 0, n);
 	    }
             in.close();
 	} catch (Exception e) {
             pageContext.setAttribute(Action.EXCEPTION_KEY, e,
                                      PageContext.REQUEST_SCOPE);
             throw new JspException
                 (messages.getMessage("include.read",
                                      url.toString(), e.toString()));
 	}
 
         // Define the retrieved content as a page scope attribute
         pageContext.setAttribute(id, sb.toString());
 
 	// Skip any body of this tag
 	return (SKIP_BODY);
 
     }
 
 
     /**
      * Release all allocated resources.
      */
     public void release() {
 
         super.release();
         forward = null;
         href = null;
         id = null;
         page = null;
 
     }
 
 
     // ------------------------------------------------------ Protected Methods
 
 
     /**
      * Return a URL to the requested resource, modified to include the session
      * identifier if necessary.
      *
      * @exception JspException if an error occurs preparing the hyperlink
      */
     protected URL hyperlink() throws JspException {
 
         // Validate the number of href specifiers that were specified
         int n = 0;
         if (forward != null)
             n++;
         if (href != null)
             n++;
         if (page != null)
             n++;
         if (n != 1) {
             JspException e = new JspException
                 (messages.getMessage("include.destination"));
             pageContext.setAttribute(Action.EXCEPTION_KEY, e,
                                      PageContext.REQUEST_SCOPE);
             throw e;
         }
 
         // Calculate the appropriate hyperlink
         String href = null;
         boolean includeSession = true;
         if (this.forward != null) {
             ActionForwards forwards = (ActionForwards)
                 pageContext.getAttribute(Action.FORWARDS_KEY,
                                          PageContext.APPLICATION_SCOPE);
             if (forwards == null)
                 throw new JspException
                     (messages.getMessage("include.forwards"));
             ActionForward forward = forwards.findForward(this.forward);
             if (forward == null)
                 throw new JspException
                     (messages.getMessage("include.forward", this.forward));
             HttpServletRequest request =
                 (HttpServletRequest) pageContext.getRequest();
             href = RequestUtils.absoluteURL(request, forward.getPath());
         } else if (this.href != null) {
             href = this.href;
             includeSession = false;
        } /* else if (this.page != null) */ {
             HttpServletRequest request =
                 (HttpServletRequest) pageContext.getRequest();
             href = RequestUtils.absoluteURL(request, this.page);
         }
 
         // Append the session identifier if appropriate
         if (includeSession) {
             String sessionId = null;
             HttpServletRequest request =
                 (HttpServletRequest) pageContext.getRequest();
             HttpSession session = request.getSession();
             try {
                 sessionId = session.getId();
             } catch (Throwable t) {
                 sessionId = null;
             }
             if (sessionId != null) {
                 int question = href.indexOf('?');
                 if (question < 0)
                     href += ";jsessionid=" + sessionId;
                 else
                     href = href.substring(0, question) +
                         ";jsessionid=" + sessionId +
                         href.substring(question);
             }
         }
 
         // Convert the hyperlink to a URL
         try {
             return (new URL(href));
         } catch (MalformedURLException e) {
             JspException f = new JspException
                 (messages.getMessage("include.malformed", href));
             pageContext.setAttribute(Action.EXCEPTION_KEY, e,
                                      PageContext.APPLICATION_SCOPE);
             throw f;
         }
 
     }
 
 
 }
