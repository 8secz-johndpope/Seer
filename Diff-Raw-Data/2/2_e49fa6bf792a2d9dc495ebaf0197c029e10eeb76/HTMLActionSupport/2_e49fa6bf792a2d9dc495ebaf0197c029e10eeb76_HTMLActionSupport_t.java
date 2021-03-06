 /*
  *  Weblounge: Web Content Management System
  *  Copyright (c) 2009 The Weblounge Team
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
 
 package ch.o2it.weblounge.common.impl.site;
 
 import ch.o2it.weblounge.common.content.Renderer;
 import ch.o2it.weblounge.common.content.ResourceURI;
 import ch.o2it.weblounge.common.content.page.Composer;
 import ch.o2it.weblounge.common.content.page.DeclarativeHTMLHeadElement;
 import ch.o2it.weblounge.common.content.page.HTMLHeadElement;
 import ch.o2it.weblounge.common.content.page.Link;
 import ch.o2it.weblounge.common.content.page.Page;
 import ch.o2it.weblounge.common.content.page.PageTemplate;
 import ch.o2it.weblounge.common.content.page.Pagelet;
 import ch.o2it.weblounge.common.content.page.PageletRenderer;
 import ch.o2it.weblounge.common.content.page.Script;
 import ch.o2it.weblounge.common.impl.content.page.PageURIImpl;
 import ch.o2it.weblounge.common.impl.request.RequestUtils;
 import ch.o2it.weblounge.common.impl.util.config.ConfigurationUtils;
 import ch.o2it.weblounge.common.request.RequestFlavor;
 import ch.o2it.weblounge.common.request.WebloungeRequest;
 import ch.o2it.weblounge.common.request.WebloungeResponse;
 import ch.o2it.weblounge.common.site.ActionException;
 import ch.o2it.weblounge.common.site.HTMLAction;
 import ch.o2it.weblounge.common.site.I18nDictionary;
 import ch.o2it.weblounge.common.site.Module;
 import ch.o2it.weblounge.common.site.Site;
 
 import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.IOException;
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 
 /**
  * This class is the default implementation for an <code>HTMLAction</code>. The
  * implementations of the various <code>startXYZ</code> methods are implemented
  * such that they leave it to the target page to render the stuff.
  * <p>
  * <b>Note:</b> Be aware of the fact that actions are pooled, so make sure to
  * implement the <code>activate()</code> and <code>passivate()</code> method
  * accordingly and include the respective super implementations.
  */
 public class HTMLActionSupport extends ActionSupport implements HTMLAction {
 
   /** Logging facility */
   protected static final Logger logger = LoggerFactory.getLogger(HTMLActionSupport.class);
 
   /** The default path to render on */
   protected String targetPath = null;
 
   /** The page uri, deducted from targetPath */
   protected ResourceURI pageURI = null;
 
   /** The page template id */
   protected String templateId = null;
 
   /** The stage renderer id */
   protected String stageRendererId = null;
 
   /** The page template */
   protected PageTemplate template = null;
 
   /** The underlying page */
   protected Page page = null;
 
   /** The renderer used for this request */
   protected PageletRenderer renderer = null;
 
   /** The information messages */
   protected List<String> infoMessages = null;
 
   /** The warning messages */
   protected List<String> warningMessages = null;
 
   /** The error messages */
   protected List<String> errorMessages = null;
 
   /**
    * Creates a new action implementation that directly supports the generation
    * of <code>HTML</code> pages.
    */
   public HTMLActionSupport() {
     this(null);
   }
 
   /**
    * Creates a new action implementation that directly supports the generation
    * of <code>HTML</code> pages.
    * <p>
    * When passing in <code>renderer</code>, the action's default behavior will
    * be to write the renderer to the stage.
    * 
    * @param renderer
    *          the renderer identifier
    */
   public HTMLActionSupport(String renderer) {
     flavors.add(RequestFlavor.HTML);
     stageRendererId = renderer;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.o2it.weblounge.common.site.Action#passivate()
    */
   public void passivate() {
     super.passivate();
     page = null;
     renderer = null;
     infoMessages = null;
     warningMessages = null;
     errorMessages = null;
   }
 
   /**
    * Sets the associated site if this is a site related renderer configuration.
    * 
    * @param site
    *          the associated site
    */
   public void setSite(Site site) {
     super.setSite(site);
     if (site == null)
       return;
 
     if (targetPath != null)
       this.pageURI = new PageURIImpl(site, targetPath);
     if (templateId != null)
       this.template = site.getTemplate(templateId);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.o2it.weblounge.common.impl.site.ActionSupport#setModule(ch.o2it.weblounge.common.site.Module)
    */
   @Override
   public void setModule(Module module) {
     super.setModule(module);
     if (StringUtils.isNotBlank(stageRendererId)) {
       this.renderer = getModule().getRenderer(stageRendererId);
       if (this.renderer == null) {
         logger.warn("Stage renderer '{}' for action {} not found", renderer, this);
       }
     }
   }
 
   /**
    * Sets the page that is used to do the action's rendering.
    * 
    * @param page
    *          the page
    */
   public void setPage(Page page) {
     this.page = page;
 
     if (page == null)
       return;
 
     // Add pagelet renderer includes
     for (Pagelet p : page.getPagelets()) {
       Module module = site.getModule(p.getModule());
       if (module == null)
         continue;
       PageletRenderer renderer = module.getRenderer(p.getIdentifier());
       if (renderer == null)
         continue;
       for (HTMLHeadElement header : renderer.getHTMLHeaders())
         addHTMLHeader(header);
     }
 
   }
 
   /**
    * Returns the page that is used to do the action's rendering.
    * 
    * @return the target page
    */
   public Page getPage() {
     return page;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.o2it.weblounge.common.site.Action#setPageURI(ch.o2it.weblounge.common.content.ResourceURI)
    */
   public void setPageURI(ResourceURI uri) {
     this.pageURI = uri;
   }
 
   /**
    * Convenience method used to be able to define the target URI without having
    * access to the <code>Site</code> yet that will allow to set the uri as an
    * object of type <code>PageURI</code>.
    * 
    * @param uri
    *          the target path
    * @see #setPageURI(ResourceURI)
    */
   void setPageURI(String uri) {
     this.targetPath = uri;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.o2it.weblounge.common.site.Action#getPageURI()
    */
   public ResourceURI getPageURI() {
     if (pageURI == null && site != null && StringUtils.isNotBlank(targetPath)) {
       pageURI = new PageURIImpl(site, targetPath);
     }
     return pageURI;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.o2it.weblounge.common.site.Action#setTemplate(ch.o2it.weblounge.common.content.page.PageTemplate)
    */
   public void setTemplate(PageTemplate template) {
     this.template = template;
   }
 
   /**
    * Convenience method used to be able to define the target template without
    * having access to the <code>Site</code> yet that will allow to set the
    * template as an object of type <code>PageTemplate</code>.
    * 
    * @param template
    *          the template identifier
    * @see #setTemplate(PageTemplate)
    */
   void setTemplate(String template) {
     this.templateId = template;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.o2it.weblounge.common.site.Action#getTemplate()
    */
   public PageTemplate getTemplate() {
     return template;
   }
 
   /**
    * Sets the renderer that is used to renderer the <code>stage</code> composer
    * when implementers don't overwrite
    * {@link #startStage(WebloungeRequest, WebloungeResponse, Composer)} and
    * {@link #startComposer(WebloungeRequest, WebloungeResponse, Composer)}.
    * 
    * @param renderer
    *          the stage renderer
    */
   public void setStageRenderer(PageletRenderer renderer) {
     this.stageRendererId = (renderer != null) ? renderer.getIdentifier() : null;
     this.renderer = renderer;
   }
 
   /**
    * Returns the renderer that is used to renderer the <code>stage</code>
    * composer when implementers don't overwrite
    * {@link #startStage(WebloungeRequest, WebloungeResponse, Composer)} and
    * {@link #startComposer(WebloungeRequest, WebloungeResponse, Composer)}.
    * 
    * @return the renderer
    */
   public PageletRenderer getStageRenderer() {
     if (renderer == null) {
       if (StringUtils.isNotBlank(stageRendererId)) {
         if (module == null) {
           throw new IllegalStateException("Module was not set");
         }
         renderer = getModule().getRenderer(stageRendererId);
         if (renderer != null) {
           logger.warn("Stage renderer '{}' not found in module '{}'", stageRendererId, module.getIdentifier());
         }
       }
     }
     return renderer;
   }
 
   /**
    * Returns <code>true</code> if <code>composer</code> equals the stage of the
    * current renderer.
    * 
    * @param composer
    *          the composer to test
    * @param request
    *          the request
    * @return <code>true</code> if <code>composer</code> is the main stage
    */
   protected boolean isStage(String composer, WebloungeRequest request) {
     if (composer == null)
       throw new IllegalArgumentException("Composer may not be null!");
 
     String stage = PageTemplate.DEFAULT_STAGE;
     PageTemplate template = (PageTemplate) request.getAttribute(WebloungeRequest.TEMPLATE);
     if (template != null)
       stage = template.getStage();
     return composer.equalsIgnoreCase(stage);
   }
 
   /**
    * This implementation always returns
    * {@link ch.o2it.weblounge.common.site.Action#EVAL_REQUEST} and simply sets
    * the content type on the response to <code>text/html;charset=utf-8</code>.
    * This means that subclasses should either overwrite this method to specify a
    * different encoding or make sure that everything that is written to the
    * response is encoded to <code>utf-8</code>.
    * 
    * @param request
    *          the servlet request
    * @param response
    *          the servlet response
    * @return {@link ch.o2it.weblounge.common.site.Action#EVAL_REQUEST}
    */
   public int startResponse(WebloungeRequest request, WebloungeResponse response)
       throws ActionException {
     response.setContentType("text/html;charset=utf-8");
     return EVAL_REQUEST;
   }
 
   /**
    * {@inheritDoc}
    * <p>
    * This implementation asks the action to return the include headers
    * <code>&lt;script&gt;</code> and <code>&lt;link&gt;</code> by calling
    * {@link #getHTMLHeaders()} and writes them to the response.
    * <p>
    * Since this implementation collects all of the includes that are needed to
    * render the page by iterating over the included elements, it returns
    * {@link ch.o2it.weblounge.common.site.HTMLAction#SKIP_HEADER} to tell the
    * tag implementation that no further work is needed.
    * 
    * @see ch.o2it.weblounge.common.site.HTMLAction#startHeader(ch.o2it.weblounge.common.request.WebloungeRequest,
    *      ch.o2it.weblounge.common.request.WebloungeResponse)
    */
   public int startHeader(WebloungeRequest request, WebloungeResponse response)
       throws IOException, ActionException {
 
     // The headers returned by getHTMLHeaders() may contain configured as well
     // as unconfigured (containing ${module.root} in their paths) elements. This
     // is why we need to make sure that we are not writing duplicates to the
     // response by configuring all elements and then removing duplicates.
 
     Set<HTMLHeadElement> processedHeaders = new HashSet<HTMLHeadElement>();
 
     // Configure the elements and add them to a set
     for (HTMLHeadElement header : getHTMLHeaders()) {
       if (header instanceof DeclarativeHTMLHeadElement)
         ((DeclarativeHTMLHeadElement) header).configure(request, site, module);
       processedHeaders.add(header);
     }
 
     // Clean the headers collection and write to the response what's left
     for (HTMLHeadElement header : processedHeaders) {
       if (headers.contains(header)) {
         headers.remove(header);
         headers.add(header);
       }
       response.getWriter().println(header.toXml());
     }
     return SKIP_HEADER;
   }
 
   /**
    * This method always returns
    * {@link ch.o2it.weblounge.common.site.HTMLAction#EVAL_COMPOSER} and
    * therefore leaves rendering to the actual content of the composer. This
    * means that if this action is rendered on an existing page, a call to
    * {@link #startPagelet(WebloungeRequest, WebloungeResponse, String, ch.o2it.weblounge.common.content.page.Pagelet)}
    * for each of them will be issued.
    * 
    * @param request
    *          the request object
    * @param response
    *          the response object
    * @return <code>EVAL_COMPOSER</code>
    * @see ch.o2it.weblounge.common.site.HTMLAction#startStage(ch.o2it.weblounge.api.request.WebloungeRequest,
    *      ch.o2it.weblounge.api.request.WebloungeResponse,
    *      ch.o2it.weblounge.common.content.page.Composer)
    */
   public int startStage(WebloungeRequest request, WebloungeResponse response,
       Composer composer) throws IOException, ActionException {
     if (renderer != null) {
       include(request, response, renderer);
       return SKIP_COMPOSER;
     }
     return EVAL_COMPOSER;
   }
 
   /**
    * This method always returns
    * {@link ch.o2it.weblounge.common.site.HTMLAction#EVAL_COMPOSER} and
    * therefore leaves rendering to the actual content of the composer. This
    * means that if this action is rendered on an existing page, a call to
    * {@link #startPagelet(WebloungeRequest, WebloungeResponse, String, ch.o2it.weblounge.common.content.page.Pagelet)}
    * for each of them will be issued.
    * 
    * @param request
    *          the request object
    * @param response
    *          the response object
    * @return <code>EVAL_COMPOSER</code>
    * @see ch.o2it.weblounge.common.site.HTMLAction#startComposer(ch.o2it.weblounge.common.request.WebloungeRequest,
    *      ch.o2it.weblounge.common.request.WebloungeResponse,
    *      ch.o2it.weblounge.common.content.page.Composer)
    */
   public int startComposer(WebloungeRequest request,
       WebloungeResponse response, Composer composer) throws IOException,
       ActionException {
     return EVAL_COMPOSER;
   }
 
   /**
    * This method always returns
    * {@link ch.o2it.weblounge.common.site.HTMLAction#EVAL_PAGELET} and therefore
    * leaves rendering to the pagelet.
    * 
    * @param request
    *          the request object
    * @param response
    *          the response object
    * @param pagelet
    *          the pagelet
    * @return <code>EVAL_PAGELET</code>
    * @see ch.o2it.weblounge.common.site.HTMLAction#startPagelet(ch.o2it.weblounge.common.request.WebloungeRequest,
    *      ch.o2it.weblounge.common.request.WebloungeResponse,
    *      ch.o2it.weblounge.common.content.page.Pagelet)
    */
   public int startPagelet(WebloungeRequest request, WebloungeResponse response,
       Pagelet pagelet) throws IOException, ActionException {
     return EVAL_PAGELET;
   }
 
   /**
    * Includes the given renderer with the request.
    * 
    * @param request
    *          the request
    * @param response
    *          the response
    * @param renderer
    *          the renderer to include
    */
   protected void include(WebloungeRequest request, WebloungeResponse response,
       Renderer renderer) {
     if (renderer == null) {
       String msg = "Renderer to be included in action '" + this + "' on " + request.getUrl() + " was not found!";
       logger.error(msg);
       response.invalidate();
       return;
     }
     try {
       renderer.render(request, response);
     } catch (Throwable t) {
       String params = RequestUtils.dumpParameters(request);
       String msg = "Error including '" + renderer + "' in action '" + this + "' on " + request.getUrl() + " " + params;
       Throwable o = t.getCause();
       if (o != null) {
         msg += ": " + o.getMessage();
         logger.error(msg, o);
       } else {
         logger.error(msg, t);
       }
       response.invalidate();
     }
   }
 
   /**
    * Adds the message to the list of information messages.
    * 
    * @param request
    *          the weblounge request
    * @param msg
    *          the message
    */
   protected void reportInfo(WebloungeRequest request, String msg) {
     reportInfo(request, msg, new Object[] {});
   }
 
   /**
    * Adds the message to the list of information messages.
    * 
    * @param request
    *          the weblounge request
    * @param msg
    *          the message
    * @param arguments
    *          the list of arguments to incorporate into the message
    */
   protected void reportInfo(WebloungeRequest request, String msg,
       Object... arguments) {
     if (msg == null)
       throw new IllegalArgumentException("Message cannot be null");
     if (infoMessages == null) {
       infoMessages = new ArrayList<String>();
       request.setAttribute(HTMLAction.INFOS, infoMessages);
     }
     infoMessages.add(createMessage(request, msg, arguments));
   }
 
   /**
    * Returns <code>true</code> if info messages have been reported.
    * 
    * @return <code>true</code> if there are info messages
    */
   protected boolean hasMessages() {
     return infoMessages == null || infoMessages.size() == 0;
   }
 
   /**
    * Adds the message to the list of warning messages.
    * 
    * @param request
    *          the weblounge request
    * @param msg
    *          the warning message
    */
   protected void reportWarning(WebloungeRequest request, String msg) {
     reportWarning(request, msg, null, new Object[] {});
   }
 
   /**
    * Adds the message to the list of warning messages.
    * 
    * @param request
    *          the weblounge request
    * @param msg
    *          the warning message
    * @param arguments
    *          the list of arguments to incorporate into the message
    */
   protected void reportWarning(WebloungeRequest request, String msg,
       Object... arguments) {
     if (msg == null)
       throw new IllegalArgumentException("Warning message cannot be null");
     if (warningMessages == null) {
       warningMessages = new ArrayList<String>();
       request.setAttribute(HTMLAction.WARNINGS, warningMessages);
     }
     warningMessages.add(createMessage(request, msg, arguments));
   }
 
   /**
    * Returns <code>true</code> if warning messages have been reported.
    * 
    * @return <code>true</code> if there are warning messages
    */
   protected boolean hasWarnings() {
     return warningMessages == null || warningMessages.size() == 0;
   }
 
   /**
    * Adds the message to the list of error messages.
    * 
    * @param msg
    *          the error message
    * @param request
    *          the weblounge request
    */
   protected void reportError(WebloungeRequest request, String msg) {
     reportError(request, msg, null, new Object[] {});
   }
 
   /**
    * Adds the message to the list of error messages.
    * 
    * @param msg
    *          the error message
    * @param request
    *          the weblounge request
    * @param arguments
    *          the list of arguments to incorporate into the message
    */
   protected void reportError(WebloungeRequest request, String msg,
       Object... arguments) {
     if (msg == null)
       throw new IllegalArgumentException("Error message cannot be null");
     if (errorMessages == null) {
       errorMessages = new ArrayList<String>();
       request.setAttribute(HTMLAction.ERRORS, errorMessages);
     }
     errorMessages.add(createMessage(request, msg, arguments));
   }
 
   /**
    * Processes the message by first looking up its I18n translation and then
    * applying optional arguments using a {@link MessageFormat}.
    * 
    * @param request
    *          the request
    * @param msg
    *          the message
    * @param args
    *          optional message arguments
    * @return the message
    */
   private String createMessage(WebloungeRequest request, String msg,
       Object... args) {
     I18nDictionary dictionary = request.getSite().getI18n();
     msg = dictionary.get(msg, request.getLanguage());
     if (args == null || args.length == 0)
       return msg;
     return MessageFormat.format(msg, args);
   }
 
   /**
    * Returns <code>true</code> if error messages have been reported.
    * 
    * @return <code>true</code> if there are error messages
    */
   protected boolean hasErrors() {
     return errorMessages == null || errorMessages.size() == 0;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.o2it.weblounge.common.site.Action#toXml()
    */
   public String toXml() {
     StringBuffer b = new StringBuffer();
     b.append("<action id=\"");
     b.append(identifier);
     b.append("\">");
 
     // Names
     if (StringUtils.isNotBlank(name)) {
       b.append("<name>");
       b.append(name);
       b.append("</name>");
     }
 
     // class
     b.append("<class>").append(getClass().getName()).append("</class>");
 
     // mountpoint
     b.append("<mountpoint>").append(mountpoint).append("</mountpoint>");
 
     // pageuri
     if (pageURI != null)
       b.append("<page>").append(pageURI.getPath()).append("</page>");
     else if (targetPath != null)
       b.append("<page>").append(targetPath).append("</page>");
 
     // template
     if (template != null)
       b.append("<template>").append(template).append("</template>");
     else if (templateId != null)
       b.append("<template>").append(templateId).append("</template>");
 
     // Recheck time
     if (recheckTime >= 0) {
       b.append("<recheck>");
       b.append(ConfigurationUtils.toDuration(recheckTime));
       b.append("</recheck>");
     }
 
     // Valid time
     if (validTime >= 0) {
       b.append("<valid>");
       b.append(ConfigurationUtils.toDuration(validTime));
       b.append("</valid>");
     }
 
     // Includes
    if (headers != null && headers.size() > 0) {
       b.append("<includes>");
       for (HTMLHeadElement include : getHTMLHeaders()) {
         if (include instanceof Link)
           b.append(include.toXml());
       }
       for (HTMLHeadElement include : getHTMLHeaders()) {
         if (include instanceof Script)
           b.append(include.toXml());
       }
       b.append("</includes>");
     }
 
     // Options
     b.append(options.toXml());
 
     b.append("</action>");
     return b.toString();
   }
 
 }
