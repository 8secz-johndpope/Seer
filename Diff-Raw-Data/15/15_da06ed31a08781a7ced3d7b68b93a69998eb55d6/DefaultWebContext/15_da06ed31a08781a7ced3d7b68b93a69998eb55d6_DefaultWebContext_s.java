 /*
  * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
  *
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the GNU Lesser General Public License
  * (LGPL) version 2.1 which accompanies this distribution, and is available at
  * http://www.gnu.org/licenses/lgpl.html
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * Contributors:
  *     bstefanescu
  *
  * $Id$
  */
 
 package org.nuxeo.ecm.webengine;
 
 import java.io.File;
 import java.io.IOException;
 import java.io.Serializable;
 import java.security.Principal;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import javax.script.Bindings;
 import javax.script.SimpleBindings;
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import javax.servlet.http.HttpSession;
 
 import net.sf.json.JSONObject;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 import org.nuxeo.common.utils.Path;
 import org.nuxeo.ecm.core.api.ClientException;
 import org.nuxeo.ecm.core.api.CoreSession;
 import org.nuxeo.ecm.core.api.DocumentModel;
 import org.nuxeo.ecm.core.api.DocumentRef;
 import org.nuxeo.ecm.core.api.DocumentSecurityException;
 import org.nuxeo.ecm.core.api.NuxeoPrincipal;
 import org.nuxeo.ecm.core.api.repository.Repository;
 import org.nuxeo.ecm.core.api.repository.RepositoryManager;
 import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
 import org.nuxeo.ecm.webengine.exceptions.WebResourceNotFoundException;
 import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
 import org.nuxeo.ecm.webengine.scripting.ScriptFile;
 import org.nuxeo.ecm.webengine.scripting.Scripting;
 import org.nuxeo.ecm.webengine.util.FormData;
 import org.nuxeo.ecm.webengine.util.JSonHelper;
 import org.nuxeo.runtime.api.Framework;
 import org.python.core.PyDictionary;
 
 /**
  * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
  *
  */
 public class DefaultWebContext implements WebContext {
 
     private static final Log log = LogFactory.getLog(WebContext.class);
 
     public static final String CORESESSION_KEY = "SiteCoreSession";
 
     public static boolean USE_CORE_SEARCH = false;
     private static CoreSession anonymousSession; //TODO: this should be relative to the web app
 
     protected final WebEngine engine;
     protected CoreSession session;
     protected boolean isCanceled = false;
 
     protected WebObject head; // the site root
     protected WebObject tail;
 
     protected final HttpServletRequest request;
     protected final HttpServletResponse response;
 
     protected PathInfo pathInfo;
 
     protected WebApplication app;
     protected FormData form;
 
     protected final Map<String,Object> vars; // global vars to share between scripts
 
     protected List<File> scriptExecutionStack;
 
     protected String basePath;
     protected String applicationPath;
     private boolean isInitialized;
 
     public DefaultWebContext(WebApplication app, PathInfo pathInfo,
             HttpServletRequest req, HttpServletResponse resp) {
         this.pathInfo = pathInfo;
         request = req;
         this.app = app;
         engine = app.getWebEngine();
         response = resp;
         vars = new HashMap<String, Object>();
         scriptExecutionStack = new ArrayList<File>();
     }
 
     public void initialize() throws WebException, IllegalStateException {
         if (isInitialized) {
             throw new IllegalStateException("Context already initialized");
         }
         isInitialized = true;
         DocumentRef documentRoot = pathInfo.getDocument();
         if (documentRoot == null) { //TODO XXX: here we can add a custom root resolver
             pathInfo.setTrailingPath(pathInfo.getTraversalPath());
             return;
         }
         CoreSession session = getCoreSession();
         DocumentModel doc =  null;
         try {
             doc = session.getDocument(documentRoot);
         } catch (Exception e) {
             throw WebException.wrap(e);
         }
         addWebObject(doc.getName(), doc);
         Path traversalPath = pathInfo.getTraversalPath();
 
         for (int i=0, len=traversalPath.segmentCount(); i<len; i++) {
             String name = traversalPath.segment(i);
             doc = getLastObject().traverse(name); // get next object if any
             if (doc != null) {
                 addWebObject(name, doc);
             } else if (i == 0) {
                 pathInfo.setTrailingPath(traversalPath);
                 break;
             } else {
                 pathInfo.setTrailingPath(traversalPath.removeFirstSegments(i));
                 break;
             }
         }
     }
 
 
     public boolean isCanceled() {
         return isCanceled;
     }
 
     public void cancel() {
         isCanceled = true;
     }
 
     public void cancel(int errorCode) {
         isCanceled = true;
         response.setStatus(errorCode);
     }
 
     public Collection<ActionDescriptor> getActions() throws WebException {
         WebObject obj = getTargetObject();
         return obj != null ? obj.getActions() : null;
     }
 
     public Collection<ActionDescriptor> getActions(String category) throws WebException {
         WebObject obj = getTargetObject();
         return obj != null ? obj.getActions(category) : null;
     }
 
     public Map<String, Collection<ActionDescriptor>> getActionsByCategory() throws WebException {
         WebObject obj = getTargetObject();
         return obj != null ? obj.getActionsByCategory() : null;
     }
 
     public DocumentModel getTargetDocument() {
         WebObject obj = getTargetObject();
         return obj != null ? obj.getDocument() : null;
     }
 
     public Map<String, Object> getEnvironment() {
         return engine.getEnvironment();
     }
 
     public FormData getForm() {
         if (form == null) {
             form = new FormData(request);
         }
         return form;
     }
 
     public String getApplicationPath() {
         if (applicationPath == null) {
             applicationPath = new StringBuilder(256).append(getBasePath())
                 .append(pathInfo.getApplicationPath().toString()).toString();
         }
         return applicationPath;
     }
 
     public String getUrlPath(DocumentModel document) {
         if (head != null) {
             String appPath = getApplicationPath();
             StringBuilder buf = new StringBuilder(512);
             // resolve the document relative to the root
             Path docPath = document.getPath().makeAbsolute();
             Path path = null;
             path = getRelativePath(head.getDocument().getPath().makeAbsolute(), docPath);
             if (path != null) {
                 buf.append(appPath);
             } else {
                 buf.append(getBasePath());
                 path = engine.getUrlPath(docPath);
             }
             if (path == null) {
                 path = docPath;
             }
             if (path.segmentCount() > 0) {
                 buf.append(path.removeTrailingSeparator().toString());
             }
             return buf.toString();
         }
         return null;
     }
 
     public PathInfo getPathInfo() {
         return pathInfo;
     }
 
     public Principal getPrincipal() {
         return request.getUserPrincipal();
     }
 
     public HttpServletRequest getRequest() {
         return request;
     }
 
     public HttpServletResponse getResponse() {
         return response;
     }
 
     public WebApplication getApplication() {
         return app;
     }
 
     public WebObject getFirstObject() {
         return head;
     }
 
     public CoreSession getCoreSession() throws WebException {
         if (session == null) {
             try {
                 session = getCoreSession(request);
             } catch (WebException e) {
                 throw e;
             } catch (DocumentSecurityException e) {
                 throw new WebSecurityException("Failed to get a session to the core repository", e);
             } catch (Exception e) {
                 throw new WebException("Failed to get core session", e);
             }
         }
         return session;
     }
 
     public WebObject getTargetObject() {
         return tail;
     }
 
     public String getTargetObjectUrlPath() {
         WebObject obj = getTargetObject();
         return obj != null ? obj.getUrlPath() : null;
     }
 
 
     public ScriptFile getTargetScript() throws IOException {
         ScriptFile script = computeTargetScript();
         if (script == null && tail != null) { // show default page only when the target document exists
             script = getFile(app.getDefaultPage());
         }
         return script;
     }
 
     protected ScriptFile computeTargetScript() throws IOException {
         String targetScriptPath = pathInfo.getScript();
         if (targetScriptPath != null) { // custom script
             return getFile(targetScriptPath);
         } else if (tail == null) { // there is no traversal path
             if (pathInfo.hasTrailingPath()) { // use the trail path
                 Path trailingPath = pathInfo.getTrailingPath();
                 String path = trailingPath.toString();
                 ScriptFile script = getFile(path);
                 if (script != null) {
                     return script;
                 }
             } else { // use the index page
                 String path = app.getIndexPage();
                 ScriptFile script = getFile(path);
                 if (script != null) {
                     return script;
                 }
             }
         } else if (pathInfo.getAction() != null) { // there is a contextual object and an action
             return tail.getActionScript(pathInfo.getAction());
         } else if (pathInfo.hasTrailingPath()) { // there is no action - use the trailing path
             Path trailingPath = pathInfo.getTrailingPath();
             String path = trailingPath.toString();
             ScriptFile script = getFile(path);
             if (script != null) {
                 return script;
             }
         }
         return null;
     }
 
     public ScriptFile getFile(String path) throws IOException {
         if (path == null || path.length() == 0) {
             return null;
         }
         char c = path.charAt(0);
         if (c == '.') { // local path - use the path stack to resolve it
             File file = getCurrentScriptDirectory();
             if (file != null) {
                 // get the file local path - TODO this should be done in ScriptFile?
                 file = new File(file, path).getCanonicalFile();
                 if (file.isFile()) {
                     return new ScriptFile(file);
                 }
                 // try using stacked roots
                 String rootPath = engine.getRootDirectory().getAbsolutePath();
                 String filePath = file.getAbsolutePath();
                 path = filePath.substring(rootPath.length());
             } else {
                 log.warn("Relative path used but there is any running script");
                 path = new Path(path).makeAbsolute().toString();
             }
         }
         return app.getFile(path);
     }
 
     public void pushScriptFile(File file) {
         if (scriptExecutionStack.size() > 64) { // stack limit
             throw new IllegalStateException("Script execution stack overflowed. More than 64 calls between scripts");
         }
         if (file == null) {
             throw new IllegalArgumentException("Cannot push a null file");
         }
         scriptExecutionStack.add(file);
     }
 
     public File popScriptFile() {
         int size = scriptExecutionStack.size();
         if (size == 0) {
             throw new IllegalStateException("Script execution stack underflowed. No script path to pop");
         }
         return scriptExecutionStack.remove(size-1);
     }
 
     public File getCurrentScriptFile() {
         int size = scriptExecutionStack.size();
         if (size == 0) {
             return null;
         }
         return scriptExecutionStack.get(size-1);
     }
 
     public File getCurrentScriptDirectory() {
         int size = scriptExecutionStack.size();
         if (size == 0) {
             return null;
         }
         return scriptExecutionStack.get(size-1).getParentFile();
     }
 
 
     public String getURI() {
         return request.getRequestURI();
     }
 
     public String getURL() {
         return request.getRequestURL().toString();
     }
 
     public String getUrlPath() {
         StringBuilder buf = new StringBuilder(request.getRequestURI().length());
         String path = request.getContextPath();
         if (path == null) {
             path = "/nuxeo/site"; // for testing
         }
         buf.append(path).append(request.getServletPath());
         path = request.getPathInfo();
         if (path != null) {
             buf.append(path);
         }
         return buf.toString();
     }
 
     public String getBasePath() {
         if (basePath == null) {
             StringBuilder buf = new StringBuilder(request.getRequestURI().length());
             String path = request.getContextPath();
             if (path == null) {
                 path = "/nuxeo/site"; // for testing
             }
             buf.append(path).append(request.getServletPath());
             int len = buf.length();
             if (len > 0 && buf.charAt(len-1) == '/') {
                 buf.setLength(len-1);
             }
             basePath = buf.toString();
         }
         return basePath;
     }
 
     public String getBaseURL() {
         StringBuffer sb = request.getRequestURL();
         int p = sb.indexOf(getBasePath());
         if (p > -1) {
             return sb.substring(0, p);
         }
         return sb.toString();
     }
 
     public Object getProperty(String key) {
        return vars.get(key);
     }
 
     public Object getProperty(String key, Object defaultValue) {
         Object value = vars.get(key);
         return value == null ? defaultValue : value;
     }
 
     public WebEngine getWebEngine() {
         return engine;
     }
 
     public void print(String text) throws IOException {
         response.getWriter().write(text);
     }
 
     public void redirect(String url) throws IOException {
         response.sendRedirect(url);
         isCanceled = true;
     }
 
 
     public void render(String template) throws WebException {
         render(template, null);
     }
 
     @SuppressWarnings("unchecked")
     public void render(String template, Object ctx) throws WebException {
         try {
             ScriptFile script = getFile(template);
             if (script != null) {
                 render(script, ctx);
             } else {
                 throw new WebResourceNotFoundException("Template not found: "+template);
             }
         } catch (IOException e) {
             throw new WebException("Failed to get script file for: "+template);
         }
     }
 
     @SuppressWarnings("unchecked")
     public void render(ScriptFile script, Object ctx) throws WebException {
         Map map = null;
         if (ctx != null) {
             if (ctx instanceof Map) {
                 map = (Map) ctx;
             } else if (ctx instanceof PyDictionary) {
                 map = Scripting.convertPythonMap((PyDictionary) ctx);
             }
         }
         try {
             String template = script.getURL();
             Bindings bindings = createBindings(map);
             if (log.isDebugEnabled()) {
                 log.debug("## Rendering: "+template);
             }
             pushScriptFile(script.getFile());
             app.getRendering().render(template, bindings, response.getWriter());
         } catch (Exception e) {
             e.printStackTrace();
             throw new WebException("Failed to render template: "+script.getAbsolutePath(), e);
         } finally {
             popScriptFile();
         }
     }
 
     public Object runScript(String script) throws WebException {
         return runScript(script, null);
     }
 
     public Object runScript(String script, Map<String, Object> args) throws WebException {
         try {
             ScriptFile sf = getFile(script);
             if (sf != null) {
                 return runScript(sf, args);
             } else {
                 throw new WebResourceNotFoundException("Script not found: "+script);
             }
         } catch (IOException e) {
             throw new WebException("Failed to get script file: "+script, e);
         }
     }
 
     public Object runScript(ScriptFile script, Map<String, Object> args) throws WebException {
         try {
             pushScriptFile(script.getFile());
             return engine.getScripting().runScript(this, script, createBindings(args));
         } catch (WebException e) {
             throw e;
         } catch (Exception e) {
             throw new WebException("Failed to run script "+script, e);
         } finally {
             popScriptFile();
         }
     }
 
     public Object exec(ScriptFile script, Map<String, Object> args) throws WebException {
         String ext = script.getExtension();
         if ("ftl".equals(ext)) {
             render(script, args);
             return null;
         } else {
             return runScript(script, args);
         }
     }
 
     public void setProperty(String key, Object value) {
         vars.put(key, value);
     }
 
 
     public WebObject getLastObject() {
         return tail;
     }
 
     public boolean hasTraversalPath() {
         return head != null;
     }
 
 
 
     public void removeLastTraversalObject() {
         if (head != null) {
             tail.prev.next = null;
             tail = tail.prev;
         }
     }
 
     public List<WebObject> getTraversalObjects() {
         ArrayList<WebObject> objects = new ArrayList<WebObject>();
         WebObject p = head;
         while (p != null) {
             objects.add(p);
             p = p.next;
         }
         return objects;
     }
 
     public JSONObject toJSon(DocumentModel doc) throws WebException {
         return toJSon(doc, (String[])null);
     }
 
     public JSONObject toJSon(DocumentModel doc, String ... schemas) throws WebException {
         return JSonHelper.doc2JSon(doc, schemas);
     }
 
     public void resolveFirstUnresolvedSegment(DocumentModel doc) {
         if (pathInfo.hasTrailingPath()) {
             Path trailingPath = pathInfo.getTrailingPath();
             String name = trailingPath.lastSegment();
             trailingPath = trailingPath.removeLastSegments(1);
             pathInfo.setTrailingPath(trailingPath);
             addWebObject(name, doc);
         }
     }
 
     /**
      * XXX this is a shortcut method we need to remove
      * @return
      */
     public String getFirstUnresolvedSegment() {
         if (pathInfo.hasTrailingPath()) {
             return pathInfo.getTrailingPath().segment(0);
         }
         return null;
     }
 
     //--------------------------------------------------------------------------- TODO internal API
 
     public Bindings createBindings(Map<String, Object> vars) {
         Bindings bindings = new SimpleBindings();
         if (vars != null) {
             bindings.putAll(vars);
         }
         initDefaultBindings(bindings);
         return bindings;
     }
 
     protected void initDefaultBindings(Bindings bindings) {
         bindings.put("Context", this);
         bindings.put("Request", request);
         bindings.put("Response", response);
         bindings.put("This", getTargetObject());
         bindings.put("Root", getFirstObject());
         bindings.put("Document", getTargetDocument());
         bindings.put("Engine", engine);
         bindings.put("basePath", getBasePath());
         bindings.put("appPath", getApplicationPath());
         try {
             bindings.put("Session", getCoreSession());
         } catch (Exception e) {
             e.printStackTrace(); // TODO
         }
     }
 
 
    /**
     * XXX should be this made part of the API? or may be createa WebContexFactory ..
     * @param name
     * @param doc
     * @return
     */
    public WebObject addWebObject(String name, DocumentModel doc) {
        if (name == null) {
            name = ""; // this happens for the root node
        }
        WebObject object = new WebObject(this, name, doc);
        if (head == null) {
            head = tail = object;
            object.prev = null;
        } else {
            tail.next = object;
            object.prev = tail;
        }
        object.next = null;
        tail = object;
        return object;
    }
 
 
    @Override
    public String toString() {
        return "PathInfo: " + pathInfo.toString();
    }
 
 
 
     public CoreSession getCoreSession(HttpServletRequest request)
     throws Exception {
         CoreSession session = null;
 
         HttpSession httpSession = request.getSession(true);
 
         // FIXME: session is alway null here, there is no need to check ?
         if (session == null) {
             session = (CoreSession) httpSession.getAttribute(CORESESSION_KEY);
             if (session != null) {
                 return session;
             }
         }
         Principal principal = request.getUserPrincipal();
         if (principal instanceof NuxeoPrincipal) {
             if (((NuxeoPrincipal)principal).isAnonymous()) { // use the anonymous session
                 session = getAnonymousSession();
             } else {
                 session = openSession();
             }
         } else {
             session = openSession();
         }
         if (httpSession != null) {
             httpSession.setAttribute(CORESESSION_KEY, session);
         }
         return session;
     }
 
     public  CoreSession openSession() throws Exception {
         String repoName = app.getRepositoryName();
         RepositoryManager rm = Framework.getService(RepositoryManager.class);
         Repository repo = rm.getRepository(repoName);
         if (repo == null) {
             throw new ClientException("Unable to get " + repoName
                     + " repository");
         }
         // we should set the principal in the session context to be sure it will work for
         // both POJO session and bean session (bean session are working always
         // because they get the principal from the bean context)
         Principal principal = request.getUserPrincipal();
 //        //TODO ========== temporary code
 //        if (principal == null) {
 //            principal = new UserPrincipal("system");
 //        }
 //        //TODO ========== temporary code
         if (principal instanceof Serializable) {
             HashMap<String,Serializable> ctx = new HashMap<String, Serializable>();
             ctx.put("principal", (Serializable)principal);
             return repo.open(ctx);
         } else {
             return repo.open();
         }
     }
 
     /**
      * TODO move this into WebApplication
      * @deprecated
      * @return
      * @throws Exception
      */
     @Deprecated
     public CoreSession getAnonymousSession() throws Exception {
         if (anonymousSession == null) {
             anonymousSession = openSession();
         }
         return anonymousSession;
     }
 
     public static void destroyAnonymousSession() {
         if (anonymousSession != null) {
             anonymousSession.destroy();
         }
         anonymousSession = null;
     }
 
     /**
      * Given an absolute path return its relative path to the given base path if any.
      * <p>
      * If the path cannot be made relative to the basePath then null is returned
      * <br>
      * The returned path is always starting with a '/'
      * @param basePath the base path
      * @param path the path
      * @return the relative path or null if the relative path cannot be computed
      */
     public static Path getRelativePath(Path basePath, Path path) {
         int cnt = basePath.matchingFirstSegments(path);
         if (cnt == basePath.segmentCount()) {
             return path.removeFirstSegments(cnt).makeAbsolute();
         }
         return null;
     }
 }
