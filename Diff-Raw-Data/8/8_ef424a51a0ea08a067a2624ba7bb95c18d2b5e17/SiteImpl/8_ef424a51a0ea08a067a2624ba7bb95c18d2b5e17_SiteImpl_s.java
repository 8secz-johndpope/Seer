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
 
 package ch.entwine.weblounge.common.impl.site;
 
 import ch.entwine.weblounge.common.content.page.PageLayout;
 import ch.entwine.weblounge.common.content.page.PageTemplate;
 import ch.entwine.weblounge.common.content.repository.ContentRepository;
 import ch.entwine.weblounge.common.content.repository.ContentRepositoryException;
 import ch.entwine.weblounge.common.impl.content.page.PageTemplateImpl;
 import ch.entwine.weblounge.common.impl.language.LanguageUtils;
 import ch.entwine.weblounge.common.impl.scheduler.QuartzJob;
 import ch.entwine.weblounge.common.impl.scheduler.QuartzJobTrigger;
 import ch.entwine.weblounge.common.impl.scheduler.QuartzJobWorker;
 import ch.entwine.weblounge.common.impl.scheduler.QuartzTriggerListener;
 import ch.entwine.weblounge.common.impl.security.SiteAdminImpl;
 import ch.entwine.weblounge.common.impl.security.jaas.AuthenticationModuleImpl;
 import ch.entwine.weblounge.common.impl.url.WebUrlImpl;
 import ch.entwine.weblounge.common.impl.util.config.ConfigurationUtils;
 import ch.entwine.weblounge.common.impl.util.config.OptionsHelper;
 import ch.entwine.weblounge.common.impl.util.xml.ValidationErrorHandler;
 import ch.entwine.weblounge.common.impl.util.xml.XPathHelper;
 import ch.entwine.weblounge.common.impl.util.xml.XPathNamespaceContext;
 import ch.entwine.weblounge.common.language.Language;
 import ch.entwine.weblounge.common.language.UnknownLanguageException;
 import ch.entwine.weblounge.common.request.RequestListener;
 import ch.entwine.weblounge.common.request.WebloungeRequest;
 import ch.entwine.weblounge.common.request.WebloungeResponse;
 import ch.entwine.weblounge.common.scheduler.Job;
 import ch.entwine.weblounge.common.scheduler.JobTrigger;
 import ch.entwine.weblounge.common.scheduler.JobWorker;
 import ch.entwine.weblounge.common.security.AuthenticationModule;
 import ch.entwine.weblounge.common.security.Role;
 import ch.entwine.weblounge.common.security.User;
 import ch.entwine.weblounge.common.security.UserListener;
 import ch.entwine.weblounge.common.security.WebloungeUser;
 import ch.entwine.weblounge.common.site.I18nDictionary;
 import ch.entwine.weblounge.common.site.Module;
 import ch.entwine.weblounge.common.site.ModuleException;
 import ch.entwine.weblounge.common.site.Site;
 import ch.entwine.weblounge.common.site.SiteException;
 import ch.entwine.weblounge.common.site.SiteListener;
 import ch.entwine.weblounge.common.url.UrlUtils;
 import ch.entwine.weblounge.common.url.WebUrl;
 
 import org.osgi.framework.Bundle;
 import org.osgi.framework.BundleContext;
 import org.osgi.service.component.ComponentContext;
 import org.quartz.JobDataMap;
 import org.quartz.JobDetail;
 import org.quartz.Scheduler;
 import org.quartz.SchedulerException;
 import org.quartz.Trigger;
 import org.quartz.TriggerListener;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.w3c.dom.Document;
 import org.w3c.dom.Node;
 import org.w3c.dom.NodeList;
 
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Date;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.regex.Pattern;
 
 import javax.xml.XMLConstants;
 import javax.xml.parsers.DocumentBuilder;
 import javax.xml.parsers.DocumentBuilderFactory;
 import javax.xml.validation.Schema;
 import javax.xml.validation.SchemaFactory;
 import javax.xml.xpath.XPath;
 import javax.xml.xpath.XPathFactory;
 
 /**
  * Default implementation of a site.
  */
 public class SiteImpl implements Site {
 
   /** Serial version uid */
   private static final long serialVersionUID = 5544198303137698222L;
 
   /** Logging facility */
   static final Logger logger = LoggerFactory.getLogger(SiteImpl.class);
 
   /** Bundle property name of the site identifier */
   public static final String PROP_IDENTIFIER = "site.identifier";
 
   /** Xml namespace for the site */
   public static final String SITE_XMLNS = "http://www.entwinemedia.com/weblounge/3.0/site";
 
   /** Regular expression to test the validity of a site identifier */
   private static final String SITE_IDENTIFIER_REGEX = "^[a-zA-Z0-9]+[a-zA-Z0-9-_.]*$";
 
   /** Name of the administrator role. TODO: Should be configurable */
   private static final String ADMINISTRATOR_ROLE = "administrator";
 
   /** Name of the anonymous role. TODO: Should be configurable */
   private static final String ANONYMOUS_ROLE = "anonymous";
 
   /** The site identifier */
   protected String identifier = null;
 
   /** Site enabled state */
   protected boolean autoStart = true;
 
   /** Site running state */
   private boolean isOnline = false;
 
   /** Url of this site */
   private WebUrl url = null;
 
   /** Site description */
   protected String name = null;
 
   /** Site administrator */
   protected WebloungeUser administrator = null;
 
   /** Page languages */
   protected Map<String, Language> languages = null;
 
   /** The default language */
   protected Language defaultLanguage = null;
 
   /** Page templates */
   protected Map<String, PageTemplate> templates = null;
 
   /** The default page template */
   protected PageTemplate defaultTemplate = null;
 
   /** Page layouts */
   protected Map<String, PageLayout> layouts = null;
 
   /** The default page template */
   protected PageLayout defaultLayout = null;
 
   /** Modules */
   protected Map<String, Module> modules = null;
 
   /** The default hostname */
   protected URL defaultURL = null;
 
   /** Ordered list of site urls */
   protected List<URL> urls = null;
 
   /** Jobs */
   protected Map<String, QuartzJob> jobs = null;
 
   /** The i18n dictionary */
   protected I18nDictionaryImpl i18n = null;
 
   /** The site's content repository */
   protected ContentRepository contentRepository = null;
 
   /** Authentication modules */
   protected List<AuthenticationModule> authenticationModules = null;
 
   /** Option handling support */
   protected OptionsHelper options = null;
 
   /** Request listeners */
   private List<RequestListener> requestListeners = null;
 
   /** Site listeners */
   private List<SiteListener> siteListeners = null;
 
   /** User listeners */
   private List<UserListener> userListeners = null;
 
   /** Scheduling service tracker */
   private SchedulingServiceTracker schedulingServiceTracker = null;
 
   /** Quartz scheduler */
   private Scheduler scheduler = null;
 
   /** Listener for the quartz scheduler */
   private TriggerListener quartzTriggerListener = null;
 
   /** Flag to tell whether we are currently shutting down */
   private boolean isShutdownInProgress = false;
 
   /**
    * Creates a new site that is initially disabled. Use {@link #setEnabled()} to
    * enable the site.
    */
   public SiteImpl() {
     languages = new HashMap<String, Language>();
     templates = new HashMap<String, PageTemplate>();
     layouts = new HashMap<String, PageLayout>();
     modules = new HashMap<String, Module>();
     urls = new ArrayList<URL>();
     jobs = new HashMap<String, QuartzJob>();
     authenticationModules = new ArrayList<AuthenticationModule>();
     i18n = new I18nDictionaryImpl();
     options = new OptionsHelper();
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#setIdentifier(java.lang.String)
    */
   public void setIdentifier(String identifier) {
     if (identifier == null)
       throw new IllegalArgumentException("Site identifier must not be null");
     else if (!Pattern.matches(SITE_IDENTIFIER_REGEX, identifier))
       throw new IllegalArgumentException("Site identifier '" + identifier + "' is malformed");
     this.identifier = identifier;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getIdentifier()
    */
   public String getIdentifier() {
     return identifier;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#setAutoStart(boolean)
    */
   public void setAutoStart(boolean enabled) {
     this.autoStart = enabled;
     if (isOnline)
       stop();
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#isStartedAutomatically()
    */
   public boolean isStartedAutomatically() {
     return autoStart;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#setName(java.lang.String)
    */
   public void setName(String description) {
     this.name = description;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getName()
    */
   public String getName() {
     return name;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#setAdministrator(ch.entwine.weblounge.common.security.WebloungeUser)
    */
   public void setAdministrator(WebloungeUser administrator) {
     if (administrator != null)
       logger.debug("Site administrator is {}", administrator);
     else
       logger.debug("Site administrator is now undefined");
     this.administrator = administrator;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getAdministrator()
    */
   public WebloungeUser getAdministrator() {
     return administrator;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getI18n()
    */
   public I18nDictionary getI18n() {
     return i18n;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addTemplate(ch.entwine.weblounge.common.content.page.PageTemplate)
    */
   public void addTemplate(PageTemplate template) {
     templates.put(template.getIdentifier(), template);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeTemplate(ch.entwine.weblounge.common.content.page.PageTemplate)
    */
   public void removeTemplate(PageTemplate template) {
     if (template == null)
       throw new IllegalArgumentException("Template must not be null");
     logger.debug("Removing page template '{}'", template.getIdentifier());
     templates.remove(template.getIdentifier());
     if (template.equals(defaultTemplate)) {
       defaultTemplate = null;
       logger.debug("Default template is now undefined");
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getTemplate(java.lang.String)
    */
   public PageTemplate getTemplate(String template) {
     return templates.get(template);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getTemplates()
    */
   public PageTemplate[] getTemplates() {
     return templates.values().toArray(new PageTemplate[templates.size()]);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#setDefaultTemplate(ch.entwine.weblounge.common.content.page.PageTemplate)
    */
   public void setDefaultTemplate(PageTemplate template) {
     if (template != null) {
       templates.put(template.getIdentifier(), template);
       logger.debug("Default page template is '{}'", template.getIdentifier());
     } else
       logger.debug("Default template is now undefined");
     this.defaultTemplate = template;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getDefaultTemplate()
    */
   public PageTemplate getDefaultTemplate() {
     return defaultTemplate;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addLanguage(ch.entwine.weblounge.common.language.Language)
    */
   public void addLanguage(Language language) {
     if (language != null)
       languages.put(language.getIdentifier(), language);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeLanguage(ch.entwine.weblounge.common.language.Language)
    */
   public void removeLanguage(Language language) {
     if (language != null) {
       languages.remove(language.getIdentifier());
       if (language.equals(defaultLanguage))
         defaultLanguage = null;
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getLanguage(java.lang.String)
    */
   public Language getLanguage(String languageId) {
     return languages.get(languageId);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getLanguages()
    */
   public Language[] getLanguages() {
     return languages.values().toArray(new Language[languages.size()]);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#supportsLanguage(ch.entwine.weblounge.common.language.Language)
    */
   public boolean supportsLanguage(Language language) {
     return languages.values().contains(language);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#setDefaultLanguage(ch.entwine.weblounge.common.language.Language)
    */
   public void setDefaultLanguage(Language language) {
     addLanguage(language);
     defaultLanguage = language;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getDefaultLanguage()
    */
   public Language getDefaultLanguage() {
     return defaultLanguage;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addLayout(ch.entwine.weblounge.common.content.page.PageLayout)
    */
   public void addLayout(PageLayout layout) {
     if (layout == null)
       throw new IllegalStateException("Layout must not be null");
     layouts.put(layout.getIdentifier(), layout);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeLayout(java.lang.String)
    */
   public PageLayout removeLayout(String layout) {
     if (layout == null)
       throw new IllegalStateException("Layout must not be null");
     return layouts.remove(layout);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getLayout(java.lang.String)
    */
   public PageLayout getLayout(String layout) {
     return layouts.get(layout);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getLayouts()
    */
   public PageLayout[] getLayouts() {
     return layouts.values().toArray(new PageLayout[layouts.size()]);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#setDefaultURL(URL)
    */
   public void setDefaultURL(URL url) {
     defaultURL = url;
     if (url != null)
       addURL(url);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addURL(URL)
    */
   public void addURL(URL url) {
     if (url == null)
       throw new IllegalArgumentException("Url must not be null");
     urls.add(url);
 
     // Make sure we have a default URL
     if (defaultURL == null)
       defaultURL = url;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeURL(URL)
    */
   public boolean removeURL(URL url) {
     if (url == null)
       throw new IllegalArgumentException("Hostname must not be null");
     if (url.equals(defaultURL))
       defaultURL = null;
     return urls.remove(url);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getURLs()
    */
   public URL[] getURLs() {
     return urls.toArray(new URL[urls.size()]);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getURL()
    */
   public URL getURL() {
     return defaultURL;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addAuthenticationModule(ch.entwine.weblounge.common.security.AuthenticationModule)
    */
   public void addAuthenticationModule(AuthenticationModule module) {
     if (!authenticationModules.contains(module))
       authenticationModules.add(module);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeAuthenticationModule(ch.entwine.weblounge.common.security.AuthenticationModule)
    */
   public void removeAuthenticationModule(AuthenticationModule module) {
     authenticationModules.remove(module);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getLoginModules()
    */
   public AuthenticationModule[] getAuthenticationModules() {
     return authenticationModules.toArray(new AuthenticationModule[authenticationModules.size()]);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getUrl()
    */
   public WebUrl getUrl() {
     if (url != null)
       return url;
     if (defaultURL == null)
       return null;
     url = new WebUrlImpl(this, defaultURL.getPath());
     return url;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addModule(ch.entwine.weblounge.common.site.Module)
    */
   public void addModule(Module module) throws ModuleException {
     if (module == null)
       throw new IllegalArgumentException("Module must not be null");
     module.setSite(this);
     modules.put(module.getIdentifier(), module);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeModule(java.lang.String)
    */
   public Module removeModule(String module) throws ModuleException {
     if (module == null)
       throw new IllegalArgumentException("Module must not be null");
     Module m = modules.remove(module);
     if (m != null)
       m.destroy();
     return m;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getModule(java.lang.String)
    */
   public Module getModule(String module) {
     if (module == null)
       throw new IllegalArgumentException("Module must not be null");
     return modules.get(module);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getModules()
    */
   public Module[] getModules() {
     return modules.values().toArray(new Module[modules.size()]);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#setContentRepository(ch.entwine.weblounge.common.content.repository.ContentRepository)
    */
   public void setContentRepository(ContentRepository repository) {
     ContentRepository oldRepository = contentRepository;
     this.contentRepository = repository;
     if (repository != null) {
       logger.info("Content repository {} connected to site '{}'", repository, this);
       fireRepositoryConnected(repository);
     } else {
       logger.info("Content repository {} disconnected from site '{}'", oldRepository, this);
       fireRepositoryDisconnected(oldRepository);
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getContentRepository()
    */
   public ContentRepository getContentRepository() {
     if (!isOnline)
       return null;
     return contentRepository;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#suggest(java.lang.String,
    *      java.lang.String, int)
    */
   public List<String> suggest(String dictionary, String seed, int count)
       throws ContentRepositoryException {
     if (contentRepository == null)
       throw new IllegalStateException("Cannot suggest while site without a content repository");
     return contentRepository.suggest(dictionary, seed, count);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addRequestListener(ch.entwine.weblounge.common.request.RequestListener)
    */
   public void addRequestListener(RequestListener listener) {
     if (requestListeners == null)
       requestListeners = new ArrayList<RequestListener>();
     synchronized (requestListeners) {
       requestListeners.add(listener);
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeRequestListener(ch.entwine.weblounge.common.request.RequestListener)
    */
   public void removeRequestListener(RequestListener listener) {
     if (requestListeners != null) {
       synchronized (requestListeners) {
         requestListeners.remove(listener);
       }
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addSiteListener(ch.entwine.weblounge.common.site.SiteListener)
    */
   public void addSiteListener(SiteListener listener) {
     if (siteListeners == null)
       siteListeners = new ArrayList<SiteListener>();
     synchronized (siteListeners) {
       siteListeners.add(listener);
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeSiteListener(ch.entwine.weblounge.common.site.SiteListener)
    */
   public void removeSiteListener(SiteListener listener) {
     if (siteListeners != null) {
       synchronized (siteListeners) {
         siteListeners.remove(listener);
       }
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#addUserListener(ch.entwine.weblounge.common.security.UserListener)
    */
   public void addUserListener(UserListener listener) {
     if (userListeners == null)
       userListeners = new ArrayList<UserListener>();
     synchronized (userListeners) {
       userListeners.add(listener);
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#removeUserListener(ch.entwine.weblounge.common.security.UserListener)
    */
   public void removeUserListener(UserListener listener) {
     if (userListeners != null) {
       synchronized (userListeners) {
         userListeners.remove(listener);
       }
     }
   }
   
   /**
    * {@inheritDoc}
    *
    * @see ch.entwine.weblounge.common.site.Site#getAdministratorRole()
    */
   public String getAdministratorRole() {
     return ADMINISTRATOR_ROLE;
   }
   
   /**
    * {@inheritDoc}
    *
    * @see ch.entwine.weblounge.common.site.Site#getAnonymousRole()
    */
   public String getAnonymousRole() {
     return ANONYMOUS_ROLE;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#start()
    */
   public synchronized void start() throws SiteException, IllegalStateException {
     logger.debug("Starting site {}", this);
     if (isOnline)
       throw new IllegalStateException("Site is already running");
 
     // Start the site modules
     synchronized (modules) {
       List<Module> started = new ArrayList<Module>(modules.size());
       for (Module module : modules.values()) {
         if (!module.isEnabled())
           continue;
 
         try {
           module.start();
           started.add(module);
 
           // start jobs
           for (Job job : module.getJobs()) {
             scheduleJob(job);
           }
 
           // actions are being registered automatically
 
         } catch (Throwable t) {
           logger.error("Error starting module '{}'", module, t);
         }
       }
     }
 
     // Finally, mark this site as running
     isOnline = true;
     logger.info("Site '{}' started", this);
 
     // Tell listeners
     fireSiteStarted();
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#stop()
    */
   public synchronized void stop() throws IllegalStateException {
     logger.debug("Stopping site '{}'", this);
     if (!isOnline)
       throw new IllegalStateException("Site is not running");
 
     // Stop jobs
     synchronized (jobs) {
       for (QuartzJob job : jobs.values()) {
         unscheduleJob(job);
       }
     }
 
     // actions are being unregistered automatically
 
     // Shutdown all of the modules
     synchronized (modules) {
       for (Module module : modules.values()) {
         try {
           logger.debug("Stopping module '{}'", module);
           module.stop();
         } catch (Throwable t) {
           logger.error("Error stopping module '{}'", module, t);
         }
       }
     }
 
     // Finally, mark this site as stopped
     isOnline = false;
     logger.info("Site '{}' stopped", this);
 
     // Tell listeners
     fireSiteStopped();
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#isOnline()
    */
   public boolean isOnline() {
     return isOnline && contentRepository != null;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.request.RequestListener#requestStarted(ch.entwine.weblounge.common.request.WebloungeRequest,
    *      ch.entwine.weblounge.common.request.WebloungeResponse)
    */
   public void requestStarted(WebloungeRequest request,
       WebloungeResponse response) {
     // TODO: Remove
     fireRequestStarted(request, response);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.request.RequestListener#requestDelivered(ch.entwine.weblounge.common.request.WebloungeRequest,
    *      ch.entwine.weblounge.common.request.WebloungeResponse)
    */
   public void requestDelivered(WebloungeRequest request,
       WebloungeResponse response) {
     // TODO: Remove
     fireRequestDelivered(request, response);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.request.RequestListener#requestFailed(ch.entwine.weblounge.common.request.WebloungeRequest,
    *      ch.entwine.weblounge.common.request.WebloungeResponse, int)
    */
   public void requestFailed(WebloungeRequest request,
       WebloungeResponse response, int reason) {
     // TODO: Remove
     fireRequestFailed(request, response, reason);
   }
 
   /**
    * Method to fire a <code>requestStarted()</code> message to all registered
    * <code>RequestListener</code>s.
    * 
    * @param request
    *          the started request
    * @param response
    *          the response
    */
   protected void fireRequestStarted(WebloungeRequest request,
       WebloungeResponse response) {
     if (requestListeners == null)
       return;
     synchronized (requestListeners) {
       for (RequestListener listener : requestListeners) {
         listener.requestStarted(request, response);
       }
     }
   }
 
   /**
    * Method to fire a <code>requestDelivered()</code> message to all registered
    * <code>RequestListener</code>s.
    * 
    * @param request
    *          the delivered request
    * @param response
    *          the response
    */
   protected void fireRequestDelivered(WebloungeRequest request,
       WebloungeResponse response) {
     if (requestListeners == null)
       return;
     synchronized (requestListeners) {
       for (RequestListener listener : requestListeners) {
         listener.requestDelivered(request, response);
       }
     }
   }
 
   /**
    * Method to fire a <code>requestFailed()</code> message to all registered
    * <code>RequestListener</code>s.
    * 
    * @param request
    *          the failed request
    * @param response
    *          the response
    * @param error
    *          the error code
    */
   protected void fireRequestFailed(WebloungeRequest request,
       WebloungeResponse response, int error) {
     if (requestListeners == null)
       return;
     synchronized (requestListeners) {
       for (RequestListener listener : requestListeners) {
         listener.requestFailed(request, response, error);
       }
     }
   }
 
   /**
    * This method is called if a user is logged in.
    * 
    * @param user
    *          the user that logged in
    */
   protected void fireUserLoggedIn(User user) {
     if (userListeners == null)
       return;
     synchronized (userListeners) {
       for (UserListener listener : userListeners) {
         listener.userLoggedIn(user);
       }
     }
   }
 
   /**
    * This method is called if a user is logged out.
    * 
    * @param user
    *          the user that logged out
    */
   protected void fireUserLoggedOut(User user) {
     if (userListeners == null)
       return;
     synchronized (userListeners) {
       for (UserListener listener : userListeners) {
         listener.userLoggedOut(user);
       }
     }
   }
 
   /**
    * Method to fire a <code>siteStarted()</code> message to all registered
    * <code>SiteListener</code>s.
    */
   protected void fireSiteStarted() {
     if (siteListeners == null)
       return;
     synchronized (siteListeners) {
       for (SiteListener listener : siteListeners) {
         listener.siteStarted(this);
       }
     }
   }
 
   /**
    * Method to fire a <code>siteStopped()</code> message to all registered
    * <code>SiteListener</code>s.
    */
   protected void fireSiteStopped() {
     if (siteListeners == null)
       return;
     synchronized (siteListeners) {
       for (SiteListener listener : siteListeners) {
         listener.siteStopped(this);
       }
     }
   }
 
   /**
    * Method to fire a <code>repositoryConnected()</code> message to all
    * registered <code>SiteListener</code>s.
    * 
    * @param repository
    *          the content repository
    */
   protected void fireRepositoryConnected(ContentRepository repository) {
     if (siteListeners == null)
       return;
     synchronized (siteListeners) {
       for (SiteListener listener : siteListeners) {
         listener.repositoryConnected(this, repository);
       }
     }
   }
 
   /**
    * Method to fire a <code>repositoryDisconnected()</code> message to all
    * registered <code>SiteListener</code>s.
    * 
    * @param repository
    *          the content repository
    */
   protected void fireRepositoryDisconnected(ContentRepository repository) {
     if (siteListeners == null)
       return;
     synchronized (siteListeners) {
       for (SiteListener listener : siteListeners) {
         listener.repositoryDisconnected(this, repository);
       }
     }
   }
 
   /* -------------------------------- OSGi -------------------------------- */
 
   /**
    * This method is a callback from the service tracker that is started when
    * this site is started. It is looking for an implementation of the Quartz
    * scheduler. The configuration is expected to be <code>0..1</code>, so there
    * should only be one scheduler instance at any given moment.
    * 
    * @param scheduler
    *          the quartz scheduler
    */
   synchronized void setScheduler(Scheduler scheduler) {
     this.scheduler = scheduler;
     this.quartzTriggerListener = new QuartzTriggerListener(this);
     try {
       this.scheduler.addTriggerListener(quartzTriggerListener);
       if (isOnline) {
         synchronized (jobs) {
           for (QuartzJob job : jobs.values()) {
             scheduleJob(job);
           }
         }
       }
     } catch (SchedulerException e) {
       logger.error("Error adding trigger listener to quartz scheduler", e);
     }
   }
 
   /**
    * This method is a callback from the service tracker that is started when
    * this site is started, indicating that the scheduler service is no longer
    * available.
    */
   void removeScheduler() {
     if (!isShutdownInProgress)
       logger.info("Site '{}' can no longer execute jobs (scheduler was taken down)", this);
     this.quartzTriggerListener = null;
   }
 
   /**
    * Callback from the OSGi environment to activate the site. Subclasses should
    * make sure to call this super implementation as it will assist in correctly
    * setting up the site.
    * <p>
    * This method should be configured in the <tt>Dynamic Services</tt> section
    * of your bundle.
    * 
    * @param context
    *          the component context
    * @throws Exception
    *           if the site activation fails
    */
   @SuppressWarnings("unchecked")
   protected void activate(ComponentContext context) throws Exception {
 
     BundleContext bundleContext = context.getBundleContext();
     final Bundle bundle = bundleContext.getBundle();
 
     // Fix the site identifier
     if (getIdentifier() == null) {
       String identifier = (String) context.getProperties().get(PROP_IDENTIFIER);
       if (identifier == null)
         throw new IllegalStateException("Site needs an identifier");
       setIdentifier(identifier);
     }
 
     logger.debug("Initializing site '{}'", this);
 
     // Load i18n dictionary
     Enumeration<URL> i18nEnum = bundle.findEntries("site/i18n", "*.xml", true);
     while (i18nEnum != null && i18nEnum.hasMoreElements()) {
       i18n.addDictionary(i18nEnum.nextElement());
     }
 
     // Prepare schema validator
     SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
     URL schemaUrl = SiteImpl.class.getResource("/xsd/module.xsd");
     Schema moduleSchema = schemaFactory.newSchema(schemaUrl);
 
     // Set up the document builder
     DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
     docBuilderFactory.setSchema(moduleSchema);
     docBuilderFactory.setNamespaceAware(true);
     DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
 
     // Load the modules
     final Enumeration<URL> e = bundle.findEntries("site", "module.xml", true);
 
     if (e != null) {
       while (e.hasMoreElements()) {
         URL moduleXmlUrl = e.nextElement();
         int endIndex = moduleXmlUrl.toExternalForm().lastIndexOf('/');
         URL moduleUrl = new URL(moduleXmlUrl.toExternalForm().substring(0, endIndex));
         logger.debug("Loading module '{}' for site '{}'", moduleXmlUrl, this);
 
         // Load and validate the module descriptor
         ValidationErrorHandler errorHandler = new ValidationErrorHandler(moduleXmlUrl);
         docBuilder.setErrorHandler(errorHandler);
         Document moduleXml = docBuilder.parse(moduleXmlUrl.openStream());
         if (errorHandler.hasErrors()) {
           logger.error("Errors found while validating module descriptor {}. Site '{}' is not loaded", moduleXml, this);
           throw new IllegalStateException("Errors found while validating module descriptor " + moduleXml);
         }
 
         // Load i18n dictionaries
         String i18nPath = UrlUtils.concat(moduleUrl.getPath(), "i18n");
         i18nEnum = bundle.findEntries(i18nPath, "*.xml", true);
         while (i18nEnum != null && i18nEnum.hasMoreElements()) {
           i18n.addDictionary(i18nEnum.nextElement());
         }
 
         Node moduleNode = moduleXml.getFirstChild();
         String moduleId = moduleNode.getAttributes().getNamedItem("id").getNodeValue();
 
         try {
           Module m = ModuleImpl.fromXml(moduleNode);
           logger.debug("Module '{}' loaded for site '{}'", m, this);
           addModule(m);
         } catch (Throwable t) {
           logger.error("Error loading module '{}' of site {}", moduleId, identifier);
           if (t instanceof Exception)
             throw (Exception)t;
           throw new Exception(t);
         }
       }
     } else {
       logger.debug("Site '{}' has no modules", this);
     }
 
     // Look for a job scheduler
     logger.debug("Signing up for a job scheduling services");
     schedulingServiceTracker = new SchedulingServiceTracker(bundleContext, this);
     schedulingServiceTracker.open();
 
     logger.info("Site '{}' initialized", this);
   }
 
   /**
    * Callback from the OSGi environment to deactivate the site. Subclasses
    * should make sure to call this super implementation as it will assist in
    * correctly shutting down the site.
    * <p>
    * This method should be configured in the <tt>Dynamic Services</tt> section
    * of your bundle.
    * 
    * @param context
    *          the component context
    * @throws Exception
    *           if the site deactivation fails
    */
   protected void deactivate(ComponentContext context) throws Exception {
     try {
       isShutdownInProgress = true;
       logger.debug("Taking down site '{}'", this);
       logger.debug("Stopped looking for a job scheduling services");
       schedulingServiceTracker.close();
       logger.info("Site '{}' deactivated", this);
     } finally {
       isShutdownInProgress = false;
     }
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getRole(java.lang.String,
    *      java.lang.String)
    */
   public Role getRole(String role, String context) {
     // TODO Auto-generated method stub
     return null;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#getUser(java.lang.String)
    */
   public WebloungeUser getUser(String login) {
     // TODO Auto-generated method stub
     return null;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.Customizable#setOption(java.lang.String,
    *      java.lang.String)
    */
   public void setOption(String name, String value) {
     options.setOption(name, value);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.Customizable#removeOption(java.lang.String)
    */
   public void removeOption(String name) {
     options.removeOption(name);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.Customizable#getOptionValue(java.lang.String)
    */
   public String getOptionValue(String name) {
     return options.getOptionValue(name);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.Customizable#getOptionValue(java.lang.String,
    *      java.lang.String)
    */
   public String getOptionValue(String name, String defaultValue) {
     return options.getOptionValue(name, defaultValue);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.Customizable#getOptionValues(java.lang.String)
    */
   public String[] getOptionValues(String name) {
     return options.getOptionValues(name);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.Customizable#hasOption(java.lang.String)
    */
   public boolean hasOption(String name) {
     return options.hasOption(name);
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.Customizable#getOptions()
    */
   public Map<String, List<String>> getOptions() {
     return options.getOptions();
   }
 
   /**
    * Schedules the job with the Quartz job scheduler.
    * 
    * @param job
    *          the job
    */
   private void scheduleJob(Job job) {
     if (scheduler == null)
       return;
 
     // If this scheduling operation is due to a site restart, the job needs to
     // be reset, otherwise fire-once jobs won't work.
     job.getTrigger().reset();
 
     // Throw the job at quartz
     String groupName = "site " + this.getIdentifier();
     String jobIdentifier = job.getIdentifier();
     Class<? extends JobWorker> jobClass = job.getWorker();
     JobTrigger trigger = job.getTrigger();
 
     synchronized (jobs) {
 
       // Set up the job detail
       JobDataMap jobData = new JobDataMap();
       jobData.put(QuartzJobWorker.CLASS, jobClass);
       jobData.put(QuartzJobWorker.CONTEXT, job.getContext());
       job.getContext().put(Site.class.getName(), this);
       JobDetail jobDetail = new JobDetail(jobIdentifier, groupName, QuartzJobWorker.class);
       jobDetail.setJobDataMap(jobData);
 
       // Define the trigger
       Trigger quartzTrigger = new QuartzJobTrigger(jobIdentifier, groupName, trigger);
       quartzTrigger.addTriggerListener(quartzTriggerListener.getName());
 
       // Schedule
       try {
         Date date = scheduler.scheduleJob(jobDetail, quartzTrigger);
         jobs.put(jobIdentifier, new QuartzJob(jobIdentifier, jobClass, trigger));
         String repeat = trigger.getNextExecutionAfter(date) != null ? " first" : "";
         logger.info("Job '{}' scheduled,{} execution scheduled for {}", new Object[] {
             jobIdentifier,
             repeat,
             date });
       } catch (SchedulerException e) {
         logger.error("Error trying to schedule job '{}': {}", new Object[] {
             jobIdentifier,
             e.getMessage(),
             e });
       }
     }
   }
 
   /**
    * Removes the job from the Quartz job scheduler.
    * 
    * @param job
    *          the job
    */
   private void unscheduleJob(QuartzJob job) {
    if (scheduler == null)
      return;
 
     String groupName = "site " + this.getIdentifier();
     String jobIdentifier = job.getIdentifier();
     try {
       if (scheduler.unscheduleJob(jobIdentifier, groupName))
         logger.info("Job '{}' unscheduled", jobIdentifier);
     } catch (SchedulerException e) {
       logger.error("Error trying to schedule job {}: {}", new Object[] {
           jobIdentifier,
           e.getMessage(),
           e });
     }
   }
 
   /**
    * Initializes this site from an XML node that was generated using
    * {@link #toXml()}.
    * <p>
    * To speed things up, you might consider using the second signature that uses
    * an existing <code>XPath</code> instance instead of creating a new one.
    * 
    * @param config
    *          the site node
    * @throws IllegalStateException
    *           if the site cannot be parsed
    * @see #fromXml(Node, XPath)
    * @see #toXml()
    */
   public static Site fromXml(Node config) throws IllegalStateException {
     XPath xpath = XPathFactory.newInstance().newXPath();
 
     // Define the xml namespace
     XPathNamespaceContext nsCtx = new XPathNamespaceContext(false);
     nsCtx.defineNamespaceURI("ns", SITE_XMLNS);
     xpath.setNamespaceContext(nsCtx);
 
     return fromXml(config, xpath);
   }
 
   /**
    * Initializes this site from an XML node that was generated using
    * {@link #toXml()}.
    * 
    * @param config
    *          the site node
    * @param xpathProcessor
    *          xpath processor to use
    * @throws IllegalStateException
    *           if the site cannot be parsed
    * @see #toXml()
    */
   @SuppressWarnings("unchecked")
   public static Site fromXml(Node config, XPath xpathProcessor)
       throws IllegalStateException {
     ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
 
     // identifier
     String identifier = XPathHelper.valueOf(config, "@id", xpathProcessor);
     if (identifier == null)
       throw new IllegalStateException("Unable to create sites without identifier");
 
     // class
     Site site = null;
     String className = XPathHelper.valueOf(config, "ns:class", xpathProcessor);
     if (className != null) {
       try {
         Class<? extends Site> c = (Class<? extends Site>) classLoader.loadClass(className);
         site = c.newInstance();
         site.setIdentifier(identifier);
       } catch (Throwable t) {
         throw new IllegalStateException("Unable to instantiate class " + className + " for site '" + identifier + ": " + t.getMessage(), t);
       }
     } else {
       site = new SiteImpl();
       site.setIdentifier(identifier);
     }
 
     // name
     String name = XPathHelper.valueOf(config, "ns:name", xpathProcessor);
     if (name == null)
       throw new IllegalStateException("Site '" + identifier + " has no name");
     site.setName(name);
 
     // domains
     NodeList urlNodes = XPathHelper.selectList(config, "ns:domains/ns:url", xpathProcessor);
     if (urlNodes.getLength() == 0)
       throw new IllegalStateException("Site '" + identifier + " has no hostname");
     String url = null;
     try {
       for (int i = 0; i < urlNodes.getLength(); i++) {
         url = urlNodes.item(i).getFirstChild().getNodeValue();
         if (ConfigurationUtils.isDefault(urlNodes.item(i)))
           site.setDefaultURL(new URL(url));
         else
           site.addURL(new URL(url));
       }
     } catch (MalformedURLException e) {
       throw new IllegalStateException("Site '" + identifier + "' defines malformed url: " + url);
     }
 
     // languages
     NodeList languageNodes = XPathHelper.selectList(config, "ns:languages/ns:language", xpathProcessor);
     if (languageNodes.getLength() == 0)
       throw new IllegalStateException("Site '" + identifier + " has no languages");
     for (int i = 0; i < languageNodes.getLength(); i++) {
       Node languageNode = languageNodes.item(i);
       Node defaultAttribute = languageNode.getAttributes().getNamedItem("default");
       String languageId = languageNode.getFirstChild().getNodeValue();
       try {
         Language language = LanguageUtils.getLanguage(languageId);
         if (ConfigurationUtils.isTrue(defaultAttribute))
           site.setDefaultLanguage(language);
         else
           site.addLanguage(language);
       } catch (UnknownLanguageException e) {
         throw new IllegalStateException("Site '" + identifier + "' defines unknown language: " + languageId);
       }
     }
 
     // templates
     NodeList templateNodes = XPathHelper.selectList(config, "ns:templates/ns:template", xpathProcessor);
     for (int i = 0; i < templateNodes.getLength(); i++) {
       PageTemplate template = PageTemplateImpl.fromXml(templateNodes.item(i), xpathProcessor);
       boolean isDefault = ConfigurationUtils.isDefault(templateNodes.item(i));
       if (isDefault)
         site.setDefaultTemplate(template);
       else
         site.addTemplate(template);
     }
 
     // administrator
     Node adminNode = XPathHelper.select(config, "ns:security/ns:administrator", xpathProcessor);
     if (adminNode != null) {
       site.setAdministrator(SiteAdminImpl.fromXml(adminNode, site, xpathProcessor));
     }
 
     // login modules
     NodeList loginModuleNodes = XPathHelper.selectList(config, "ns:security/ns:authentication/ns:loginmodule", xpathProcessor);
     for (int i = 0; i < loginModuleNodes.getLength(); i++) {
       AuthenticationModule module = AuthenticationModuleImpl.fromXml(loginModuleNodes.item(i), xpathProcessor);
       site.addAuthenticationModule(module);
     }
 
     // options
     Node optionsNode = XPathHelper.select(config, "ns:options", xpathProcessor);
     OptionsHelper.fromXml(optionsNode, site, xpathProcessor);
 
     return site;
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see ch.entwine.weblounge.common.site.Site#toXml()
    */
   public String toXml() {
     StringBuffer b = new StringBuffer();
     b.append("<site id=\"");
     b.append(identifier);
     b.append("\" ");
 
     // schema reference
     b.append("xmlns=\"http://www.entwinemedia.com/weblounge/3.0/site\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.entwinemedia.com/weblounge/3.0/site http://www.entwinemedia.com/xsd/weblounge/3.0/site.xsd\"");
     b.append(">");
 
     // autostart
     b.append("<autostart>").append(autoStart).append("</autostart>");
 
     // name
     b.append("<name><![CDATA[").append(name).append("]]></name>");
 
     // class
     if (!this.getClass().equals(SiteImpl.class))
       b.append("<class>").append(this.getClass().getName()).append("</class>");
 
     // languages
     if (languages.size() > 0) {
       b.append("<languages>");
       for (Language language : languages.values()) {
         b.append("<language");
         if (language.equals(defaultLanguage))
           b.append(" default=\"true\"");
         b.append(">");
         b.append(language.getIdentifier());
         b.append("</language>");
       }
       b.append("</languages>");
     }
 
     // hostnames
     if (urls.size() > 0) {
       b.append("<domains>");
       for (URL url : urls) {
         b.append("<url");
         if (url.equals(defaultURL))
           b.append(" default=\"true\"");
         b.append(">");
         b.append(url.toExternalForm());
         b.append("</url>");
       }
       b.append("</domains>");
     }
 
     // security
     if (administrator != null || authenticationModules.size() > 0) {
       b.append("<security>");
       if (administrator != null)
         b.append(administrator.toXml());
 
       if (authenticationModules.size() > 0) {
         b.append("<authentication>");
         for (AuthenticationModule module : authenticationModules) {
           b.append(module.toXml());
         }
         b.append("</authentication>");
       }
       b.append("</security>");
     }
 
     // templates
     if (templates.size() > 0) {
       b.append("<templates>");
       for (PageTemplate template : templates.values()) {
         b.append(template.toXml());
       }
       b.append("</templates>");
     }
 
     // Options
     b.append(options.toXml());
 
     b.append("</site>");
     return b.toString();
   }
 
   /**
    * {@inheritDoc}
    * 
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
     return identifier;
   }
 
 }
