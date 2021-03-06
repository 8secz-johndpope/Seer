 /*
  *  Weblounge: Web Content Management System
  *  Copyright (c) 2003 - 2011 The Weblounge Team
  *  http://entwinemedia.com/weblounge
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
 
 package ch.entwine.weblounge.dispatcher.impl.handler;
 
 import ch.entwine.weblounge.common.content.PageSearchResultItem;
 import ch.entwine.weblounge.common.content.Renderer.RendererType;
 import ch.entwine.weblounge.common.content.SearchQuery;
 import ch.entwine.weblounge.common.content.SearchQuery.Order;
 import ch.entwine.weblounge.common.content.SearchResult;
 import ch.entwine.weblounge.common.content.SearchResultItem;
 import ch.entwine.weblounge.common.content.page.Composer;
 import ch.entwine.weblounge.common.content.page.Page;
 import ch.entwine.weblounge.common.content.page.Pagelet;
 import ch.entwine.weblounge.common.content.page.PageletRenderer;
 import ch.entwine.weblounge.common.content.repository.ContentRepository;
 import ch.entwine.weblounge.common.content.repository.ContentRepositoryException;
 import ch.entwine.weblounge.common.impl.content.SearchQueryImpl;
 import ch.entwine.weblounge.common.impl.content.page.ComposerImpl;
 import ch.entwine.weblounge.common.impl.testing.MockHttpServletRequest;
 import ch.entwine.weblounge.common.impl.testing.MockHttpServletResponse;
 import ch.entwine.weblounge.common.language.Language;
 import ch.entwine.weblounge.common.request.WebloungeRequest;
 import ch.entwine.weblounge.common.request.WebloungeResponse;
 import ch.entwine.weblounge.common.site.Environment;
 import ch.entwine.weblounge.common.site.Module;
 import ch.entwine.weblounge.common.site.Site;
 import ch.entwine.weblounge.common.url.UrlUtils;
 import ch.entwine.weblounge.dispatcher.RequestHandler;
 import ch.entwine.weblounge.dispatcher.impl.DispatchUtils;
 
 import com.sun.syndication.feed.atom.Content;
 import com.sun.syndication.feed.synd.SyndCategory;
 import com.sun.syndication.feed.synd.SyndCategoryImpl;
 import com.sun.syndication.feed.synd.SyndContent;
 import com.sun.syndication.feed.synd.SyndContentImpl;
 import com.sun.syndication.feed.synd.SyndEntry;
 import com.sun.syndication.feed.synd.SyndEntryImpl;
 import com.sun.syndication.feed.synd.SyndFeed;
 import com.sun.syndication.feed.synd.SyndFeedImpl;
 import com.sun.syndication.io.FeedException;
 import com.sun.syndication.io.SyndFeedOutput;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.commons.lang.StringEscapeUtils;
 import org.apache.commons.lang.StringUtils;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.Filter;
 import org.osgi.framework.InvalidSyntaxException;
 import org.osgi.framework.ServiceReference;
 import org.osgi.service.component.ComponentContext;
 import org.osgi.util.tracker.ServiceTracker;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.EOFException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStreamWriter;
 import java.io.Writer;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.servlet.Servlet;
 import javax.servlet.ServletException;
 
 /**
  * The feed request handler will answer requests that are looking for
  * <code>rss</code> or <code>atom</code> feeds for a certain site. The request
  * is expected to provide feed type and version as the first two parameters on
  * the request path, e. g.
  * 
  * <pre>
  *  http://localhost:8080/weblounge-feeds/atom/0.3
  * </pre>
  * 
  * <p>
  * The request implementation can handle a parameter that specifies one or more
  * subjects that may appear in the pages that make up the feed entries. Like
  * this, different feeds can be created.
  * 
  * <pre>
  *  http://localhost:8080/weblounge-feeds/atom/0.3?subject=a,b,c
  * </pre>
  * 
  * </p>
  */
 public class FeedRequestHandlerImpl implements RequestHandler {
 
   /** The subjects parameter name */
   public static final String PARAM_SUBJECT = "subject";
 
   /** The limit parameter name */
   public static final String PARAM_LIMIT = "limit";
 
   /** Default value for the <code>limit</code> parameter */
   public static final int DEFAULT_LIMIT = 10;
 
   /** Alternate uri prefix */
   protected static final String URI_PREFIX = "/weblounge-feeds/";
 
   /** The site servlets */
   private static Map<String, Servlet> siteServlets = new HashMap<String, Servlet>();
 
   /** The cache service tracker */
   private ServiceTracker siteServletTracker = null;
 
   /** Filter expression used to look up site servlets */
   private static final String serviceFilter = "(&(objectclass=" + Servlet.class.getName() + ")(" + Site.class.getName().toLowerCase() + "=*))";
 
   /** Logging facility */
   private static final Logger logger = LoggerFactory.getLogger(FeedRequestHandlerImpl.class);
 
   /**
    * Callback from OSGi declarative services on component startup.
    * 
    * @param ctx
    *          the component context
    */
   void activate(ComponentContext ctx) {
     try {
       Filter filter = ctx.getBundleContext().createFilter(serviceFilter);
       siteServletTracker = new SiteServletTracker(ctx.getBundleContext(), filter);
       siteServletTracker.open();
     } catch (InvalidSyntaxException e) {
       throw new IllegalStateException(e);
     }
   }
 
   /**
    * Callback from OSGi declarative services on component shutdown.
    */
   void deactivate() {
     if (siteServletTracker != null) {
       siteServletTracker.close();
     }
   }
 
   /**
    * Handles the request for a feed of a certain type.
    * <p>
    * This method returns <code>true</code> if the handler is decided to handle
    * the request, <code>false</code> otherwise.
    * 
    * @param request
    *          the weblounge request
    * @param response
    *          the weblounge response
    */
   public boolean service(WebloungeRequest request, WebloungeResponse response) {
 
     Site site = request.getSite();
     String path = request.getRequestURI();
     String feedType = null;
 
     // Currently, we only support feeds mapped to our well-known uri
     if (!path.startsWith(URI_PREFIX) || !(path.length() > URI_PREFIX.length()))
       return false;
 
     // Check the request method. Only GET is supported right now.
     String requestMethod = request.getMethod();
     if (!"GET".equals(requestMethod)) {
       logger.debug("Feed request handler does not support {} requests", requestMethod);
       return false;
     }
 
     // Get hold of the content repository
     ContentRepository contentRepository = site.getContentRepository();
     if (contentRepository == null) {
       logger.warn("No content repository found for site '{}'", site);
       return false;
     }
 
     // TODO: Add check for if-modified-since
 
     // Check for feed type and version
     String feedURI = path.substring(URI_PREFIX.length());
     String[] feedURIParts = feedURI.split("/");
     if (feedURIParts.length == 0) {
       logger.debug("Feed request {} does not include feed type", path);
       return false;
     } else if (feedURIParts.length == 1) {
       logger.debug("Feed request {} does not include feed version", path);
       return false;
     }
 
     // Extract the subjects. The parameter may be specified multiple times
     // and add more than one subject by separating them using a comma.
     String[] subjectParameter = request.getParameterValues(PARAM_SUBJECT);
     List<String> subjects = new ArrayList<String>();
     if (subjectParameter != null) {
       for (String parameter : subjectParameter) {
         for (String subject : parameter.split(",")) {
           if (StringUtils.isNotBlank(subject))
             subjects.add(StringUtils.trim(subject));
         }
       }
     }
 
     // How many entries do we need?
     int limit = DEFAULT_LIMIT;
     String limitParameter = StringUtils.trimToNull(request.getParameter(PARAM_LIMIT));
     if (limitParameter != null) {
       try {
         limit = Integer.parseInt(limitParameter);
       } catch (Throwable t) {
         logger.debug("Non parseable number {} specified as limit", limitParameter);
         limit = DEFAULT_LIMIT;
       }
     }
 
     // User and language
     Language language = request.getLanguage();
     // User user = request.getUser();
 
     // Determine the feed type
     feedType = feedURIParts[0].toLowerCase() + "_" + feedURIParts[1];
     SyndFeed feed = new SyndFeedImpl();
     feed.setFeedType(feedType);
     feed.setLink(request.getRequestURL().toString());
     feed.setTitle(site.getName());
     feed.setDescription(site.getName());
     feed.setLanguage(language.getDescription());
 
     // TODO: Add more feed metadata, ask site
 
     // Find the pages that will form the feed
     try {
       SearchQuery query = new SearchQueryImpl(site);
       query.withType(Page.TYPE);
       query.withLimit(limit);
       query.sortByModificationDate(Order.Descending);
       for (String subject : subjects) {
         query.withSubject(subject);
       }
 
       // Load the result and add feed entries
       SearchResult result = contentRepository.find(query);
       List<SyndEntry> entries = new ArrayList<SyndEntry>();
       for (SearchResultItem item : result.getItems()) {
         if (limit == 0)
           break;
 
         // Get the page
         PageSearchResultItem pageItem = (PageSearchResultItem) item;
         Page page = pageItem.getPage();
         page.switchTo(language);
 
         // Create the entry
         SyndEntry entry = new SyndEntryImpl();
         entry.setPublishedDate(page.getPublishFrom());
         entry.setLink(item.getUrl().getLink());
         entry.setAuthor(page.getCreator().getName());
         entry.setTitle(page.getTitle());
 
         // Categories
         if (page.getSubjects().length > 0) {
           List<SyndCategory> categories = new ArrayList<SyndCategory>();
           for (String subject : page.getSubjects()) {
             SyndCategory category = new SyndCategoryImpl();
             category.setName(subject);
             categories.add(category);
           }
           entry.setCategories(categories);
         }
 
         // TODO: Can the page be accessed?
 
         // Try to render the preview pagelets and write them to the feed
         List<SyndContent> entryContent = new ArrayList<SyndContent>();
         Composer composer = new ComposerImpl("preview", page.getPreview());
         for (Pagelet pagelet : composer.getPagelets()) {
           Module module = site.getModule(pagelet.getModule());
           PageletRenderer renderer = null;
           if (module == null) {
             logger.warn("Skipping pagelet {} in feed due to missing module '{}'", pagelet, pagelet.getModule());
             continue;
           }
 
           renderer = module.getRenderer(pagelet.getIdentifier());
           URL rendererURL = renderer.getRenderer(RendererType.Feed.toString());
           Environment environment = request.getEnvironment();
           if (rendererURL == null)
             rendererURL = renderer.getRenderer();
           if (rendererURL != null) {
             String rendererContent = null;
             try {
               rendererContent = loadContents(rendererURL, site, page, composer, pagelet, environment);
             } catch (ServletException e) {
               logger.warn("Error processing the pagelet renderer at {}: {}", rendererURL, e.getMessage());
             } catch (IOException e) {
               logger.warn("Error processing the pagelet renderer at {}: {}", rendererURL, e.getMessage());
             }
             SyndContent content = new SyndContentImpl();
             content.setType("text/html");
             content.setMode("escaped");
             content.setValue(StringEscapeUtils.escapeHtml(rendererContent));
             entryContent.add(content);
           }
         }
         if (entryContent.size() > 0) {
           entry.setContents(entryContent);
         }
 
         entries.add(entry);
         limit--;
       }
 
       feed.setEntries(entries);
     } catch (ContentRepositoryException e) {
       logger.error("Error loading articles for feeds from {}: {}", contentRepository, e.getMessage());
       DispatchUtils.sendInternalError(request, response);
       return true;
     }
 
     // Set the response type
     if ("atom".equalsIgnoreCase("atom"))
       response.setContentType("application/atom+xml");
     else if ("rss".equalsIgnoreCase(feedType))
       response.setContentType("application/rss+xml");
 
     // Set the character encoding
     response.setCharacterEncoding(feed.getEncoding());
 
     // Write the feed back to the response
     try {
 
       SyndFeedOutput output = new SyndFeedOutput();
       Writer responseWriter = new OutputStreamWriter(response.getOutputStream());
       output.output(feed, responseWriter);
       response.getOutputStream().flush();
       return true;
     } catch (FeedException e) {
       logger.error("Error creating {} feed: {}", feedType, e.getMessage());
       DispatchUtils.sendInternalError(request, response);
       return true;
     } catch (EOFException e) {
       logger.debug("Error writing feed '{}' back to client: connection closed by client", feedType);
       return true;
     } catch (IOException e) {
       logger.error("Error sending {} feed to the client: {}", feedType, e.getMessage());
       DispatchUtils.sendInternalError(request, response);
       return true;
     }
   }
 
   /**
    * Adds the image as a content element to the feed entry.
    * 
    * @param entry
    *          the feed entry
    * @param imageUrl
    *          the image url
    * @return the image
    */
   protected Content setImage(String imageUrl) {
     StringBuffer buf = new StringBuffer("<div xmlns=\"http://www.w3.org/1999/xhtml\">");
     buf.append("<img src=\"");
     buf.append(imageUrl);
     buf.append("\" />");
     buf.append("</div>");
     Content image = new Content();
     image.setType("application/xhtml+xml");
     image.setValue(buf.toString());
     return image;
   }
 
   /**
    * Asks the site servlet to render the given url using the page, composer and
    * pagelet as the rendering environment. If the no servlet is available for
    * the given site, the contents are loaded from the url directly.
    * 
    * @param rendererURL
    *          the renderer url
    * @param site
    *          the site
    * @param page
    *          the page
    * @param composer
    *          the composer
    * @param pagelet
    *          the pagelet
    * @param environment
    *          the environment
    * @return the servlet response, serialized to a string
    * @throws IOException
    *           if the servlet fails to create the response
    * @throws ServletException
    *           if an exception occurs while processing
    */
   private String loadContents(URL rendererURL, Site site, Page page,
       Composer composer, Pagelet pagelet, Environment environment)
       throws IOException, ServletException {
 
     Servlet servlet = siteServlets.get(site.getIdentifier());
 
     String httpContextURI = UrlUtils.concat("/weblounge-sites", site.getIdentifier());
     int httpContextURILength = httpContextURI.length();
     String url = rendererURL.toExternalForm();
     int uriInPath = url.indexOf(httpContextURI);
 
     if (uriInPath > 0) {
       String pathInfo = url.substring(uriInPath + httpContextURILength);
 
       // Prepare the mock request
       MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
      request.setServerName(site.getConnector(environment).getURL().getHost());
      request.setServerPort(site.getConnector(environment).getURL().getPort());
      request.setMethod(site.getConnector(environment).getURL().getProtocol());
       request.setAttribute(WebloungeRequest.PAGE, page);
       request.setAttribute(WebloungeRequest.COMPOSER, composer);
       request.setAttribute(WebloungeRequest.PAGELET, pagelet);
       request.setPathInfo(pathInfo);
       request.setRequestURI(UrlUtils.concat(httpContextURI, pathInfo));
 
       MockHttpServletResponse response = new MockHttpServletResponse();
       servlet.service(request, response);
       return response.getContentAsString();
     } else {
       InputStream is = null;
       try {
         is = rendererURL.openStream();
         return IOUtils.toString(is, "utf-8");
       } finally {
         IOUtils.closeQuietly(is);
       }
     }
   }
 
   /**
    * Adds the site servlet to the list of servlets.
    * 
    * @param id
    *          the site identifier
    * @param servlet
    *          the site servlet
    */
   void addSiteServlet(String id, Servlet servlet) {
     logger.debug("Site servlet attached to {} workbench", id);
     siteServlets.put(id, servlet);
   }
 
   /**
    * Removes the site servlet from the list of servlets
    * 
    * @param site
    *          the site identifier
    */
   void removeSiteServlet(String id) {
     logger.debug("Site servlet detached from {} workbench", id);
     siteServlets.remove(id);
   }
 
   /**
    * Implementation of a <code>ServiceTracker</code> that is tracking instances
    * of type {@link Servlet} with an associated <code>site</code> attribute.
    */
   private class SiteServletTracker extends ServiceTracker {
 
     /**
      * Creates a new servlet tracker that is using the given bundle context to
      * look up service instances.
      * 
      * @param ctx
      *          the bundle context
      * @param filter
      *          the service filter
      */
     SiteServletTracker(BundleContext ctx, Filter filter) {
       super(ctx, filter, null);
     }
 
     /**
      * {@inheritDoc}
      * 
      * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
      */
     @Override
     public Object addingService(ServiceReference reference) {
       Servlet servlet = (Servlet) super.addingService(reference);
       String site = (String) reference.getProperty(Site.class.getName().toLowerCase());
       addSiteServlet(site, servlet);
       return servlet;
     }
 
     /**
      * {@inheritDoc}
      * 
      * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
      *      java.lang.Object)
      */
     @Override
     public void removedService(ServiceReference reference, Object service) {
       String site = (String) reference.getProperty("site");
       removeSiteServlet(site);
     }
 
   }
 
   /**
    * @see ch.entwine.weblounge.dispatcher.api.request.RequestHandler#getName()
    */
   public String getName() {
     return "feed request handler";
   }
 
   /**
    * Returns a string representation of this request handler.
    * 
    * @return the handler name
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
     return getName();
   }
 
 }
