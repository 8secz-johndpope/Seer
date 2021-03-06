 /*
  * $Header: /home/cvs/jakarta-commons-sandbox/jelly/src/java/org/apache/commons/jelly/JellyContext.java,v 1.10 2002/04/26 12:20:12 jstrachan Exp $
  * $Revision: 1.10 $
  * $Date: 2002/04/26 12:20:12 $
  *
  * ====================================================================
  *
  * The Apache Software License, Version 1.1
  *
  * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
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
  * 4. The names "The Jakarta Project", "Commons", and "Apache Software
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
  * $Id: JellyContext.java,v 1.10 2002/04/26 12:20:12 jstrachan Exp $
  */
 package org.apache.commons.jelly;
 
 import java.io.File;
 import java.io.InputStream;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.Hashtable;
 import java.util.Iterator;
 import java.util.Map;
 
 import org.apache.commons.jelly.parser.XMLParser;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 /** <p><code>JellyContext</code> represents the Jelly context.</p>
   *
   * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
   * @version $Revision: 1.10 $
   */
 public class JellyContext {
 
     /** The root URL context (where scripts are located from) */
     private URL rootURL;
 
     /** The current URL context (where relative scripts are located from) */
     private URL currentURL;
 
     /** Tag libraries found so far */
     private Map taglibs = new Hashtable();
 
     /** synchronized access to the variables in scope */
     private Map variables = new Hashtable();
 
     /** The Log to which logging calls will be made. */
     private Log log = LogFactory.getLog(JellyContext.class);
 
     /** The parent context */
     private JellyContext parent;
 
     /** Do we inherit variables from parent context? */
     private boolean shouldInherit = false;
 
     /** Do we export our variables to parent context? */
     private boolean shouldExport  = false;
     
     /**
      * The class loader to use for instantiating application objects.
      * If not specified, the context class loader, or the class loader
      * used to load this class itself, is used, based on the value of the
      * <code>useContextClassLoader</code> variable.
      */
     protected ClassLoader classLoader;
 
     /**
      * Do we want to use the Context ClassLoader when loading classes
      * for instantiating new objects?  Default is <code>false</code>.
      */
     protected boolean useContextClassLoader = false;
 
 
     public JellyContext() {
         this.currentURL = rootURL;
         init();
     }
 
     public JellyContext(URL rootURL) {
         this( rootURL, rootURL );
     }
 
     public JellyContext(URL rootURL, URL currentURL) {
         this.rootURL = rootURL;
         this.currentURL = currentURL;
         init();
     }
 
     public JellyContext(JellyContext parent) {
         this.parent = parent;
         this.rootURL = parent.rootURL;
         this.currentURL = parent.currentURL;
         this.taglibs = parent.taglibs;
         this.variables.put("parentScope", parent.variables);
         init();
     }
 
     public JellyContext(JellyContext parentJellyContext, URL currentURL) {
         this(parentJellyContext);
         this.currentURL = currentURL;
     }
 
     public JellyContext(JellyContext parentJellyContext, URL rootURL, URL currentURL) {
         this(parentJellyContext, currentURL);
         this.rootURL = rootURL;
     }
 
     private void init() {
         variables.put("context", this);
         variables.put("systemScope", System.getProperties());
     }    
     
     /**
      * @return the parent context for this context
      */
     public JellyContext getParent() {
         return parent;
     }
 
     public void setExport(boolean shouldExport) {
         this.shouldExport = shouldExport;
     }
 
     public boolean getExport() {
         return this.shouldExport;
     }
 
     public void setInherit(boolean shouldInherit) {
         this.shouldInherit = shouldInherit;
     }
 
     public boolean getInherit() {
         return this.shouldInherit;
     }
 
     /**
      * @return the scope of the given name, such as the 'parent' scope.
      * If Jelly is used in a Servlet situation then 'request', 'session' and 'application' are other names 
      * for scopes
      */
     public JellyContext getScope(String name) {
         if ( "parent".equals( name ) ) {
             return getParent();
         }
         return null;
     }
     
     /** 
      * Finds the variable value of the given name in this context or in any other parent context.
      * If this context does not contain the variable, then its parent is used and then its parent 
      * and so forth until the context with no parent is found.
      * 
      * @return the value of the variable in this or one of its descendant contexts or null
      *  if the variable could not be found.
      */
     public Object findVariable(String name) {
         Object answer = variables.get(name);
         if ( answer == null && parent != null ) {
             answer = parent.findVariable(name);
         }
         // ### this is a hack - remove this when we have support for pluggable Scopes
         if ( answer == null ) {
             try {
                 answer = System.getProperty(name);
             }
             catch (Throwable t) {
                 // ignore security exceptions
             }
         }
         return answer;
     }
             
         
     /** @return the value of the given variable name */
     public Object getVariable(String name) {
         Object value = variables.get(name);
 
         if ( value == null 
              &&
              getInherit() ) {
             value = getParent().findVariable( name );
         }
 
         return value;
     }
     
     /** 
      * @return the value of the given variable name in the given variable scope 
      * @param name is the name of the variable
      * @param scopeName is the optional scope name such as 'parent'. For servlet environments
      * this could be 'application', 'session' or 'request'.
      */
     public Object getVariable(String name, String scopeName) {
         JellyContext scope = getScope(scopeName);
         if ( scope != null ) {
             return scope.getVariable(name);
         }
         return null;
     }
     
     
 
     /** Sets the value of the given variable name */
     public void setVariable(String name, Object value) {
         if ( getExport() ) {
             getParent().setVariable( name, value );
             return;
         }
         if (value == null) {
             variables.remove(name);
         }
         else {
             variables.put(name, value);
         }
     }
 
     /** 
      * Sets the value of the given variable name in the given variable scope 
      * @param name is the name of the variable
      * @param scopeName is the optional scope name such as 'parent'. For servlet environments
      *  this could be 'application', 'session' or 'request'.
      * @param value is the value of the attribute
      */
     public void setVariable(String name, String scopeName, Object value) {
         JellyContext scope = getScope(scopeName);
         if ( scope != null ) {
             scope.setVariable(name, value);
         }
     }
     
     /** Removes the given variable */
     public void removeVariable(String name) {
         variables.remove(name);
     }
 
     /** 
      * Removes the given variable in the specified scope.
      * 
      * @param name is the name of the variable
      * @param scopeName is the optional scope name such as 'parent'. For servlet environments
      *  this could be 'application', 'session' or 'request'.
      * @param value is the value of the attribute
      */
     public void removeVariable(String name, String scopeName) {
         JellyContext scope = getScope(scopeName);
         if ( scope != null ) {
             scope.removeVariable(name);
         }
     }
     
     /** 
      * @return an Iterator over the current variable names in this
      * context 
      */
     public Iterator getVariableNames() {
         return variables.keySet().iterator();
     }
 
     /**
      * @return the Map of variables in this scope
      */
     public Map getVariables() {
         return variables;
     }
 
     /**
      * Sets the Map of variables to use
      */
 
     public void setVariables(Map variables) {
         this.variables = variables;
     }
 
     /**
      * A factory method to create a new child context of the
      * current context.
      */
     public JellyContext newJellyContext(Map newVariables) {
         // XXXX: should allow this new context to
         // XXXX: inherit parent contexts? 
         // XXXX: Or at least publish the parent scope
         // XXXX: as a Map in this new variable scope?
         newVariables.put("parentScope", variables);
         JellyContext answer = createChildContext();
         answer.setVariables(newVariables);
         return answer;
     }
 
     /** Registers the given tag library against the given namespace URI.
      * This should be called before the parser is used.
      */
     public void registerTagLibrary(String namespaceURI, TagLibrary taglib) {
         if (log.isDebugEnabled()) {
             log.debug("Registering tag library to: " + namespaceURI + " taglib: " + taglib);
         }
         taglibs.put(namespaceURI, taglib);
     }
 
     /** Registers the given tag library class name against the given namespace URI.
      * The class will be loaded via the given ClassLoader
      * This should be called before the parser is used.
      */
     public void registerTagLibrary(
         String namespaceURI,
         String className) {
             
         if (log.isDebugEnabled()) {
             log.debug("Registering tag library to: " + namespaceURI + " taglib: " + className);
         }
         taglibs.put(namespaceURI, className);
     }
 
     public boolean isTagLibraryRegistered(String namespaceURI) {
         return taglibs.containsKey( namespaceURI );
     }
 
     /** 
      * @return the TagLibrary for the given namespace URI or null if one could not be found
      */
     public TagLibrary getTagLibrary(String namespaceURI) {
 
         Object answer = null;
 
         if ( getInherit()
              &&
              getParent() != null ) {
             answer = getParent().getTagLibrary( namespaceURI );
         }
 
         if ( answer == null ) {
             answer = taglibs.get(namespaceURI);
         }
 
         if ( answer instanceof TagLibrary ) {
             return (TagLibrary) answer;
         }
         else if ( answer instanceof String ) {
             String className = (String) answer;
             Class theClass = null;
             try {
                 theClass = getClassLoader().loadClass(className);
             }
             catch (ClassNotFoundException e) {
                 log.error("Could not find the class: " + className, e);
             }
             if ( theClass != null ) {
                 try {
                     Object object = theClass.newInstance();
                     if (object instanceof TagLibrary) {
                         taglibs.put(namespaceURI, object);
                         return (TagLibrary) object;
                     }                
                     else {
                         log.error(
                             "The tag library object mapped to: "
                                 + namespaceURI
                                 + " is not a TagLibrary. Object = "
                                 + object);
                     }
                 }
                 catch (Exception e) {
                     log.error(
                         "Could not instantiate instance of class: " + className + ". Reason: " + e,
                         e);
                 }
             }
         }
 
         return null;
     }
 
     /** 
      * Attempts to parse the script from the given uri using the 
     * {@link #getResource} method then returns the compiled script.
      */
     public Script compileScript(String uri) throws Exception {
         XMLParser parser = new XMLParser();
         parser.setContext(this);
         InputStream in = getResourceAsStream(uri);
         if (in == null) {
             throw new JellyException("Could not find Jelly script: " + uri);
         }
         Script script = parser.parse(in);
         return script.compile();
     }
 
     /** 
      * Attempts to parse the script from the given URL using the 
     * {@link #getResource} method then returns the compiled script.
      */
     public Script compileScript(URL url) throws Exception {
         XMLParser parser = new XMLParser();
         parser.setContext(this);
         Script script = parser.parse(url.toString());
         return script.compile();
     }
 
     /** 
      * Parses the script from the given File then compiles it and runs it.
      * 
      * @return the new child context that was used to run the script
      */
     public JellyContext runScript(File file, XMLOutput output) throws Exception {
         return runScript(file.toURL(), output);
     }
 
     /** 
      * Parses the script from the given URL then compiles it and runs it.
      * 
      * @return the new child context that was used to run the script
      */
     public JellyContext runScript(URL url, XMLOutput output) throws Exception {
         Script script = compileScript(url);
         
         URL newJellyContextURL = getJellyContextURL(url);
         JellyContext newJellyContext = new JellyContext(this, newJellyContextURL);
         script.run(newJellyContext, output);
         return newJellyContext;
     }
 
     /** 
      * Parses the script from the given uri using the 
      * JellyContext.getResource() API then compiles it and runs it.
      * 
      * @return the new child context that was used to run the script
      */
     public JellyContext runScript(String uri, XMLOutput output) throws Exception {
         URL url = getResource(uri);
         if (url == null) {
             throw new JellyException("Could not find Jelly script: " + url);
         }
         Script script = compileScript(url);
         
         URL newJellyContextURL = getJellyContextURL(url);
         JellyContext newJellyContext = new JellyContext(this, newJellyContextURL);
         script.run(newJellyContext, output);
         return newJellyContext;
     }
 
     /** 
      * Parses the script from the given uri using the 
      * JellyContext.getResource() API then compiles it and runs it.
      * 
      * @return the new child context that was used to run the script
      */
     public JellyContext runScript(String uri, XMLOutput output,
                           boolean export, boolean inherit) throws Exception {
         URL url = getResource(uri);
         if (url == null) {
             throw new JellyException("Could not find Jelly script: " + url);
         }
         Script script = compileScript(url);
         
         URL newJellyContextURL = getJellyContextURL(url);
         
         JellyContext newJellyContext = new JellyContext(this, newJellyContextURL);
         newJellyContext.setExport( export );
         newJellyContext.setInherit( inherit );
             
         if ( inherit ) {
             // use the same variable scopes
             newJellyContext.variables = this.variables;
         } 
 
         script.run(newJellyContext, output);
         
         return newJellyContext;
     }
 
     /**
      * Returns a URL for the given resource from the specified path.
      * If the uri starts with "/" then the path is taken as relative to 
      * the current context root. If the uri is a well formed URL then it
      * is used. Otherwise the uri is interpreted as relative to the current
      * context (the location of the current script).
      */
     public URL getResource(String uri) throws MalformedURLException {
         if (uri.startsWith("/")) {
             // append this uri to the context root
             return createRelativeURL(rootURL, uri.substring(1));
         }
         else {
             try {
                 return new URL(uri);
             }
             catch (MalformedURLException e) {
                 // lets try find a relative resource
                 try {
                     return createRelativeURL(currentURL, uri);
                 }
                 catch (MalformedURLException e2) {
                     throw e;
                 }
             }
         }
     }
 
     /**
      * Attempts to open an InputStream to the given resource at the specified path.
      * If the uri starts with "/" then the path is taken as relative to 
      * the current context root. If the uri is a well formed URL then it
      * is used. Otherwise the uri is interpreted as relative to the current
      * context (the location of the current script).
      *
      * @return null if this resource could not be loaded, otherwise the resources 
      *  input stream is returned.
      */
     public InputStream getResourceAsStream(String uri) {
         try {
             URL url = getResource(uri);
             return url.openStream();
         }
         catch (Exception e) {
             if (log.isTraceEnabled()) {
                 log.trace(
                     "Caught exception attempting to open: " + uri + ". Exception: " + e,
                     e);
             }
             return null;
         }
     }
 
 
     // Properties
     //-------------------------------------------------------------------------                
 
     /**
      * @return the current root context URL from which all absolute resource URIs
      *  will be relative to. For example in a web application the root URL will
      *  map to the web directory which contains the WEB-INF directory.
      */
     public URL getRootURL() {
         return rootURL;
     }
     
     /**
      * Sets the current root context URL from which all absolute resource URIs
      *  will be relative to. For example in a web application the root URL will
      *  map to the web directory which contains the WEB-INF directory.
      */
     public void setRootURL(URL rootURL) {
         this.rootURL = rootURL;
     }
     
 
     /** 
      * @return the current URL context of the current script that is executing. 
      *  This URL context is used to deduce relative scripts when relative URIs are
     *  used in calls to {@link #getResource} to process relative scripts.
      */ 
     public URL getCurrentURL() {
         return currentURL;
     }
     
     /** 
      * Sets the current URL context of the current script that is executing. 
      *  This URL context is used to deduce relative scripts when relative URIs are
     *  used in calls to {@link #getResource} to process relative scripts.
      */ 
     public void setCurrentURL(URL currentURL) { 
         this.currentURL = currentURL;
     }
 
     /**
      * Return the class loader to be used for instantiating application objects
      * when required.  This is determined based upon the following rules:
      * <ul>
      * <li>The class loader set by <code>setClassLoader()</code>, if any</li>
      * <li>The thread context class loader, if it exists and the
      *     <code>useContextClassLoader</code> property is set to true</li>
      * <li>The class loader used to load the XMLParser class itself.
      * </ul>
      */
     public ClassLoader getClassLoader() {
         if (this.classLoader != null) {
             return (this.classLoader);
         }
         if (this.useContextClassLoader) {
             ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
             if (classLoader != null) {
                 return (classLoader);
             }
         }
         return (this.getClass().getClassLoader());
     }
     
     /**
      * Set the class loader to be used for instantiating application objects
      * when required.
      *
      * @param classLoader The new class loader to use, or <code>null</code>
      *  to revert to the standard rules
      */
     public void setClassLoader(ClassLoader classLoader) {
         this.classLoader = classLoader;
     }
     
     /**
      * Return the boolean as to whether the context classloader should be used.
      */
     public boolean getUseContextClassLoader() {
         return useContextClassLoader;
     }
 
     /**
      * Determine whether to use the Context ClassLoader (the one found by
      * calling <code>Thread.currentThread().getContextClassLoader()</code>)
      * to resolve/load classes.  If not
      * using Context ClassLoader, then the class-loading defaults to
      * using the calling-class' ClassLoader.
      *
      * @param boolean determines whether to use JellyContext ClassLoader.
      */
     public void setUseContextClassLoader(boolean use) {
         useContextClassLoader = use;
     }
     
 
     // Implementation methods
     //-------------------------------------------------------------------------                
     /**
      * @return a new relative URL from the given root and with the addition of the
      * extra relative URI
      *
      * @param rootURL is the root context from which the relative URI will be applied
      * @param relativeURI is the relative URI (without a leading "/")
      * @throws MalformedURLException if the URL is invalid.
      */
     protected URL createRelativeURL(URL rootURL, String relativeURI)
         throws MalformedURLException {
         String urlText = null;
         if (rootURL == null) {
             String userDir = System.getProperty("user.dir");
             urlText = "file://" + userDir + relativeURI;
         }
         else {
             urlText = rootURL.toString() + relativeURI;
         }
         if ( log.isDebugEnabled() ) {
             log.debug("Attempting to open url: " + urlText);
         }
         return new URL(urlText);
     }
 
     /** 
      * Strips off the name of a script to create a new context URL
      */
     protected URL getJellyContextURL(URL url) throws MalformedURLException {
         String text = url.toString();
         int idx = text.lastIndexOf('/');
         text = text.substring(0, idx + 1);
         return new URL(text);
     }
 
     /**
      * Factory method to create a new child of this context
      */
     protected JellyContext createChildContext() {
         return new JellyContext(this);
     }
 }
