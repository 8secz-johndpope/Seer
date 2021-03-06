 /*
  * Copyright 2011-2012 the original author or authors.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.vertx.java.platform.impl;
 
 import org.mozilla.javascript.*;
 import org.mozilla.javascript.commonjs.module.ModuleScript;
 import org.mozilla.javascript.commonjs.module.Require;
 import org.mozilla.javascript.commonjs.module.RequireBuilder;
 import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
 import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
 import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
 import org.vertx.java.core.Vertx;
 import org.vertx.java.core.json.DecodeException;
 import org.vertx.java.core.json.JsonObject;
 import org.vertx.java.core.logging.Logger;
 import org.vertx.java.platform.Container;
 import org.vertx.java.platform.Verticle;
 import org.vertx.java.platform.VerticleFactory;
 
 import java.io.*;
 import java.net.URI;
 import java.net.URL;
 import java.util.Scanner;
 
 
 /**
  * @author <a href="http://tfox.org">Tim Fox</a>
  */
 public class RhinoVerticleFactory implements VerticleFactory {
 
   static {
     ContextFactory.initGlobal(new RhinoContextFactory());
   }
 
   private ClassLoader cl;
   private Vertx vertx;
   private Container container;
 
  private static ThreadLocal<ClassLoader> clThreadLocal = new ThreadLocal<>();
   private static CoffeeScriptCompiler coffeeScriptCompiler = null;
   private ScriptableObject scope;
 
   public RhinoVerticleFactory() {
   }
 
   @Override
   public void init(Vertx vertx, Container container, ClassLoader cl) {
     this.cl = cl;
     this.vertx = vertx;
     this.container = container;
   }
 
   public Verticle createVerticle(String main) throws Exception {
     return new RhinoVerticle(main);
   }
 
   public void reportException(Logger logger, Throwable t) {
 
     if (t instanceof RhinoException) {
       RhinoException je = (RhinoException)t;
 
       logger.error("Exception in JavaScript verticle:\n"
                    + je.details() +
                    "\n" + je.getScriptStackTrace());
     } else {
       logger.error("Exception in JavaScript verticle", t);
     }
   }
 
   public void close() {
   }
 
   public static synchronized Object load(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws Exception {
    ClassLoader cl = clThreadLocal.get();
    Scriptable scope = funObj.getParentScope();
     String moduleName = (String)args[0];
     loadScript(cl, cx, scope, moduleName);
     return null;
   }
 
   private void addStandardObjectsToScope(ScriptableObject scope) {
     Object jsStdout = Context.javaToJS(System.out, scope);
     ScriptableObject.putProperty(scope, "stdout", jsStdout);
     Object jsStderr = Context.javaToJS(System.err, scope);
     ScriptableObject.putProperty(scope, "stderr", jsStderr);
   }
 
   private static synchronized CoffeeScriptCompiler getCoffeeScriptCompiler(ClassLoader cl) {
     // Lazy load coffee script compiler
     if (RhinoVerticleFactory.coffeeScriptCompiler == null) {
       RhinoVerticleFactory.coffeeScriptCompiler = new CoffeeScriptCompiler(cl);
     }
     return RhinoVerticleFactory.coffeeScriptCompiler;
   }
 
   private static final class CoffeeCompilingUrlModuleSourceProvider extends UrlModuleSourceProvider {
     private final ClassLoader cl;
 
     private CoffeeCompilingUrlModuleSourceProvider(Iterable<URI> privilegedUris, Iterable<URI> fallbackUris, ClassLoader cl) {
       super(privilegedUris, fallbackUris);
       this.cl = cl;
     }
 
     public ModuleSource loadSource(URI uri, URI base, Object validator) throws IOException ,java.net.URISyntaxException {
       ModuleSource source = super.loadSource(uri, base, validator);
       if (uri != null && uri.toString().endsWith(".coffee")) {
         return getCoffeeScriptCompiler(cl).coffeeScriptToJavaScript(source);
       }
       return source;
     }
   }
 
   // Support for loading from CommonJS modules
   private static Require installRequire(final ClassLoader cl, Context cx, ScriptableObject globalScope) {
     RequireBuilder rb = new RequireBuilder();
     rb.setSandboxed(false);
 
     rb.setModuleScriptProvider(
         new SoftCachingModuleScriptProvider(new CoffeeCompilingUrlModuleSourceProvider(null, null, cl)) {
 
           @Override
           public ModuleScript getModuleScript(Context cx, String moduleId, URI uri, URI base, Scriptable paths) throws Exception {
 
             // Check for cached version
             CachedModuleScript cachedModule = getLoadedModule(moduleId);
             if (cachedModule != null) {
               // cachedModule.getModule() is not public
               // super.getModuleScript uses moduleSourceProvider.loadSource to check for modifications
               return super.getModuleScript(cx, moduleId, uri, uri, paths);
             }
 
             if (uri == null) {
               URL url;
               if (!moduleId.endsWith(".js") && !moduleId.endsWith(".coffee")) {
                 url = cl.getResource(moduleId + ".js"); // Try .js first
                 if (url == null) {
                   url = cl.getResource(moduleId + ".coffee"); // Then try .coffee
                 }
               } else {
                 url = cl.getResource(moduleId);
               }
               if (url != null) {
                 uri = url.toURI();
               }
             }
             return super.getModuleScript(cx, moduleId, uri, uri, paths);
           }
         });
 
     // Force export of vertxStop
     rb.setPostExec(new Script() {
       @Override
       public Object exec(Context context, Scriptable scope) {
         String js = "if(typeof vertxStop == 'function'){ " +
             "module.exports.vertxStop = vertxStop;" +
             "}";
         return context.evaluateString(scope, js, "postExec", 1, null);
       }
     });
 
     Require require = rb.createRequire(cx, globalScope);
 
     return require;
   }
 
   private static void loadScript(ClassLoader cl, Context cx, Scriptable scope, String scriptName) throws Exception {
     Reader reader;
     if (scriptName != null && scriptName.endsWith(".coffee")) {
       URL resource = cl.getResource(scriptName);
       if (resource != null) {
         reader = new StringReader(getCoffeeScriptCompiler(cl).coffeeScriptToJavaScript(resource.toURI()));
       } else {
         throw new FileNotFoundException("Cannot find script: " + scriptName);
       }
     } else {
       InputStream is = cl.getResourceAsStream(scriptName);
       if (is == null) {
         throw new FileNotFoundException("Cannot find script: " + scriptName);
       }
       reader = new BufferedReader(new InputStreamReader(is));
     }
     cx.evaluateReader(scope, reader, scriptName, 1, null);
     try {
       reader.close();
     } catch (IOException ignore) {
     }
   }
 
   private synchronized ScriptableObject getScope(Context cx) {
     if (scope == null) {
       scope = cx.initStandardObjects();
       addStandardObjectsToScope(scope);
       scope.defineFunctionProperties(new String[]{"load"}, RhinoVerticleFactory.class, ScriptableObject.DONTENUM);
       Object jsVertx = Context.javaToJS(vertx, scope);
       ScriptableObject.putProperty(scope, "__jvertx", jsVertx);
       Object jsContainer = Context.javaToJS(container, scope);
       ScriptableObject.putProperty(scope, "__jcontainer", jsContainer);
     }
     return scope;
   }
 
   private class RhinoVerticle extends Verticle {
 
     private final String scriptName;
     private Function stopFunction;
 
     RhinoVerticle(String scriptName) {
       this.scriptName = scriptName;
     }
 
     public void start() {
       Context cx = Context.enter();
       cx.setOptimizationLevel(2);
       try {
         // This is the global scope used to store JS native objects
         ScriptableObject globalScope = getScope(cx);
        // This is pretty ugly - we have to set a thread local so we can get a reference to the
        // classloader in the load() method - this is because Rhino insists load() must be static
        // what a PITA!
        clThreadLocal.set(cl);
         Require require = installRequire(cl, cx, globalScope);
         Scriptable script = require.requireMain(cx, scriptName);
         try {
           stopFunction = (Function) script.get("vertxStop", globalScope);
         } catch (ClassCastException e) {
           // Get CCE if no such function
           stopFunction = null;
         }
       } finally {
         Context.exit();
       }
     }
 
     public void stop() {
       if (stopFunction != null) {
         Context cx = Context.enter();
         try {
           stopFunction.call(cx, scope, scope, null);
         } finally {
           Context.exit();
         }
       }
      // Make sure we remove the threadlocal or we will have a leak
      clThreadLocal.remove();
     }
   }
 }
 
