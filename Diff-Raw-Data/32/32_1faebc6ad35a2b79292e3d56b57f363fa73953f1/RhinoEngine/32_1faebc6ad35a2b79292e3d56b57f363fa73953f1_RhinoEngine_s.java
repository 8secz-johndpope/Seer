 /*
  * Helma License Notice
  *
  * The contents of this file are subject to the Helma License
  * Version 2.0 (the "License"). You may not use this file except in
  * compliance with the License. A copy of the License is available at
  * http://adele.helma.org/download/helma/license.txt
  *
  * Copyright 1998-2003 Helma Software. All Rights Reserved.
  *
  * $RCSfile: RhinoEngine.java,v $
  * $Author: hannes $
  * $Revision: 1.54 $
  * $Date: 2006/05/11 19:22:07 $
  */
 
 /* 
  * Modified by:
  * 
  * Axiom Software Inc., 11480 Commerce Park Drive, Third Floor, Reston, VA 20191 USA
  * email: info@axiomsoftwareinc.com
  */
 package axiom.scripting.rhino;
 
 import org.mozilla.javascript.*;
 import org.mozilla.javascript.serialize.ScriptableOutputStream;
 import org.mozilla.javascript.serialize.ScriptableInputStream;
 import org.mozilla.javascript.xmlimpl.XHTML;
 import org.mozilla.javascript.xmlimpl.XML;
 import org.mozilla.javascript.xmlimpl.XMLList;
 
 import axiom.extensions.ConfigurationException;
 import axiom.extensions.AxiomExtension;
 import axiom.framework.*;
 import axiom.framework.core.*;
 import axiom.main.Server;
 import axiom.objectmodel.*;
 import axiom.objectmodel.db.DbMapping;
 import axiom.objectmodel.db.Relation;
 import axiom.scripting.*;
 import axiom.scripting.rhino.debug.AxiomDebugger;
 import axiom.scripting.rhino.extensions.XMLSerializer;
 import axiom.util.ExecutionCache;
 
 import java.util.*;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import java.io.*;
 import java.lang.ref.WeakReference;
 
 /**
  * This is the implementation of ScriptingEnvironment for the Mozilla Rhino EcmaScript interpreter.
  */
 public class RhinoEngine implements ScriptingEngine {
     // map for Application to RhinoCore binding
     static Map coreMap;
 
     // the application we're running in
     public Application app;
 
     // The Rhino context
     Context context;
 
     // the per-thread global object
     GlobalObject global;
 
     // the request evaluator instance owning this fesi evaluator
     RequestEvaluator reval;
     RhinoCore core;
 
     // remember global variables from last invokation to be able to
     // do lazy cleanup
     Map lastGlobals = null;
 
     // the global vars set by extensions
     HashMap extensionGlobals;
 
     // the thread currently running this engine
     Thread thread;
     
     // app's execution cache
     ExecutionCache executionCache;
     
     /**
      *  Zero argument constructor.
      */
     public RhinoEngine() {
         // nothing to do
     }
 
     /**
      * Init the scripting engine with an application and a request evaluator
      */
     public synchronized void init(Application app, RequestEvaluator reval) {
         this.app = app;
         this.executionCache = app.getExecutionCache();
         this.reval = reval;
         core = getRhinoCore(app);
         context = Context.enter();
         context.setLanguageVersion(170);
         context.setCompileFunctionsWithDynamicScope(true);
         context.setApplicationClassLoader(app.getClassLoader());
 
         try {
             global = new GlobalObject(core, app, true);
 
             extensionGlobals = new HashMap();
 
             if (Server.getServer() != null) {
                 Vector extVec = Server.getServer().getExtensions();
 
                 for (int i = 0; i < extVec.size(); i++) {
                     AxiomExtension ext = (AxiomExtension) extVec.get(i);
 
                     try {
                         HashMap tmpGlobals = ext.initScripting(app, this);
 
                         if (tmpGlobals != null) {
                             extensionGlobals.putAll(tmpGlobals);
                         }
                     } catch (ConfigurationException e) {
                         app.logError("Couldn't initialize extension " + ext.getName(), e);
                     }
                 }
             }
 
         } catch (Exception e) {
             System.err.println("Cannot initialize interpreter");
             System.err.println("Error: " + e);
             e.printStackTrace();
             throw new RuntimeException(e.getMessage());
         } finally {
             Context.exit ();
         }
     }
 
     public static synchronized RhinoCore getRhinoCore(Application app) {
         RhinoCore core = null;
 
         if (coreMap == null) {
             coreMap = new WeakHashMap();
         } else {
             WeakReference ref = (WeakReference) coreMap.get(app);
             if (ref != null) {
                 core = (RhinoCore) ref.get();
             }
         }
 
         if (core == null) {
             core = new RhinoCore(app);
             coreMap.put(app, new WeakReference(core));
         }
 
         return core;
     }
 
     /**
      *  This method is called before an execution context is entered to let the
      *  engine know it should update its prototype information.
      */
     public void updatePrototypes() throws IOException {
         // remember the current thread as our thread - we this here so
         // the thread is already set when the RequestEvaluator calls
         // Application.getDataRoot(), which may result in a function invocation
         // (chicken and egg problem, kind of)
         thread = Thread.currentThread();
 
         context = Context.enter();
         context.setLanguageVersion(170);
         context.setCompileFunctionsWithDynamicScope(true);
         context.setApplicationClassLoader(app.getClassLoader());
         context.setWrapFactory(core.wrapper);
 
         boolean isDebuggerOn = "true".equalsIgnoreCase(this.app.getProperty(RhinoCore.DEBUGGER_PROPERTY));
         if (isDebuggerOn) {
         	context.setDebugger(new AxiomDebugger(this.app), null);
         } else {
         	context.setDebugger(null, null);
         }
 
         // Set default optimization level according to whether debugger is on
         int optLevel = !isDebuggerOn ? 0 : -1;
 
         try {
         	if (!isDebuggerOn) {
         		optLevel = Integer.parseInt(app.getProperty("rhino.optlevel"));
         	}
         } catch (Exception ignore) {
             // use default opt-level
         }
         
         context.setOptimizationLevel(optLevel);
         // register the per-thread scope with the dynamic scope
         context.putThreadLocal("threadscope", global);
         // added null check, for onSessiomTimeout hook
         if(reval != null){
         	context.putThreadLocal("reval", reval);
         }
         context.putThreadLocal("engine", this);
         // update prototypes
         core.updatePrototypes(this.app.autoUpdate()); // accomodate for new input params
     }
 
     /**
      *  This method is called when an execution context for a request
      *  evaluation is entered. The globals parameter contains the global values
      *  to be applied during this execution context.
      */
     public synchronized void enterContext(Map globals) throws ScriptingException {
         // remember the current thread as our thread
         thread = Thread.currentThread();
 
         // set globals on the global object
         if (globals != lastGlobals) {
             // add globals from extensions
             globals.putAll(extensionGlobals);
             // loop through global vars and set them
             for (Iterator i = globals.keySet().iterator(); i.hasNext();) {
                 String k = (String) i.next();
                 Object v = globals.get(k);
                 Scriptable scriptable;
 
                 // create a special wrapper for the path object.
                 // other objects are wrapped in the default way.
                 if (v == null) {
                     continue;
                 } else {
                     scriptable = Context.toObject(v, global);
                 }
 
                 global.put(k, global, scriptable);
             }
             // remember the globals set on this evaluator
             lastGlobals = globals;
         }
     }
 
     /**
      *   This method is called to let the scripting engine know that the current
      *   execution context has terminated.
      */
     public synchronized void exitContext() {
         context.removeThreadLocal("reval");
         context.removeThreadLocal("engine");
         context.removeThreadLocal("threadscope");
         Context.exit();
         // core.global.unregisterScope();
         thread = null;
 
         // loop through previous globals and unset them, if necessary.
         if (lastGlobals != null) {
             for (Iterator i = lastGlobals.keySet().iterator(); i.hasNext();) {
                 String g = (String) i.next();
                 try {
                     global.delete(g);
                 } catch (Exception x) {
                     System.err.println("Error resetting global property: " + g);
                 }
             }
             lastGlobals = null;
         }
     }
 
     /**
      * Invoke a function on some object, using the given arguments and global vars.
      * XML-RPC calls require special input and output parameter conversion.
      *
      * @param thisObject the object to invoke the function on, or null for
      *                   global functions
      * @param functionName the name of the function to be invoked
      * @param args array of argument objects
      * @param argsWrapMode indicated the way to process the arguments. Must be
      *                   one of <code>ARGS_WRAP_NONE</code>,
      *                          <code>ARGS_WRAP_DEFAULT</code>,
      *                          <code>ARGS_WRAP_XMLRPC</code>
      * @param resolve indicates whether functionName may contain an object path
      *                   or just the plain function name
      * @return the return value of the function
      * @throws ScriptingException to indicate something went wrong
      *                   with the invocation
      */
     public Object invoke(Object thisObject, String functionName, Object[] args,
                          int argsWrapMode, boolean resolve) throws ScriptingException {
         
         try { 
             Scriptable obj = thisObject == null ? global : Context.toObject(thisObject, global);
             
             // if function name should be resolved interpret it as member expression,
             // otherwise replace dots with underscores.
             if (resolve) {
                 if (functionName.indexOf('.') > 0) {
                     StringTokenizer st = new StringTokenizer(functionName, ".");
                     for (int i = 0; i < st.countTokens() - 1; i++) {
                         String propName = st.nextToken();
                         Object propValue = ScriptableObject.getProperty(obj, propName);
                         if (propValue instanceof Scriptable) {
                             obj = (Scriptable) propValue;
                         } else {
                             throw new RuntimeException("Can't resolve function name " +
                                     functionName + " in " + thisObject);
                         }
                     }
                     functionName = st.nextToken();
                 }
             } else {
                 functionName = functionName.replace('.', '_');
             }
             
             if (this.executionCache == null) {
                 this.executionCache = this.app.getExecutionCache();
             }
             
             boolean cachable = false;
             if (thisObject instanceof INode) {
             	cachable = ((INode) thisObject).getState() != INode.TRANSIENT;
             } else if (thisObject instanceof AxiomObject) {
             	cachable = ((AxiomObject) thisObject).node.getState() != INode.TRANSIENT;
             }
             
             Object result = null;
             
             if (cachable) {
             	result = this.executionCache.getFunctionResult(obj, functionName);
             	if (result != null) {
             		return result;
             	}
             }
 
             if (args == null) {
             	args = new Object[0];
             }
             
             for (int i = 0; i < args.length; i++) {
                 switch (argsWrapMode) {
                     case ARGS_WRAP_DEFAULT:
                         // convert java objects to JavaScript
                         if (args[i] != null) {
                             args[i] = Context.javaToJS(args[i], global);
                         }
                         break;
                     case ARGS_WRAP_XMLRPC:
                         // XML-RPC requires special argument conversion
                         args[i] = core.processXmlRpcArgument(args[i]);
                         break;
                 }
 
             }
             
             // eval() needs a special way to be invoked, can not simply retrieve the 
             // function object on global and invoke it.  That is why we have a seperate
             // loop of execution here.
             if ("eval".equals(functionName) && obj == global && args.length > 0) {
             	result = context.evaluateString(obj, args[0].toString(), "eval", 1, null);
             	if (result instanceof Wrapper) {
             		result = ((Wrapper) result).unwrap();
             	}
             } else {
             	ScriptableObject.getPropertyIds(obj);
             	Object f = ScriptableObject.getProperty(obj, functionName);
             	if ((f == ScriptableObject.NOT_FOUND) || !(f instanceof Function)) {
             		return null;
             	}
 
             	Object retval = ((Function) f).call(context, global, obj, args);
 
             	if (retval instanceof Wrapper) {
             		retval = ((Wrapper) retval).unwrap();
             	}
 
             	if ((retval == null) || (retval == Undefined.instance)) {
             		result = null;
             	} else if (argsWrapMode == ARGS_WRAP_XMLRPC) {
             		result = core.processXmlRpcResponse(retval);
             	} else {
             		result = retval;
             	}
             }
 
             if (cachable) {
             	Object cachedResult = this.toCachableResult(result, obj);
 
             	if (this.isResultCachable(obj, cachedResult, functionName)) {
             		this.executionCache.putResultInCache(obj, functionName, cachedResult);
             	}
             }
             
             return result;
             
         } catch (RedirectException redirect) {
             throw redirect;
         } catch (TimeoutException timeout) {
             throw timeout;
         } catch (ConcurrencyException concur) {
             throw concur;
         } catch (Exception x) {
             x.printStackTrace();
             // has the request timed out? If so, throw TimeoutException
             if (thread != Thread.currentThread()) {
                 throw new TimeoutException();
             }
 
             // create and throw a ScriptingException with the right message
             String msg;
             if (x instanceof JavaScriptException) {
                 // created by javascript throw statement
                 msg = x.getMessage();
             } else if (x instanceof WrappedException) {
                 // wrapped java excepiton
                 WrappedException wx = (WrappedException) x;
                 Throwable wrapped = wx.getWrappedException();
                 // if this is a wrapped concurrencyException, rethrow it.
                 if (wrapped instanceof ConcurrencyException) {
                     throw (ConcurrencyException) wrapped;
                 }
                 // also check if this is a wrapped redirect exception
                 if (wrapped instanceof RedirectException) {
                     throw (RedirectException) wrapped;
                 }
                 // we need to build our own message here, default implementation is broken
                 StringBuffer b = new StringBuffer(wrapped.toString());
                 b.append(" (").append(wx.getSourceName()).append("#")
                         .append(wx.getLineNumber()).append(")");
                 msg = b.toString();
                 // replace wrapper with original exception
                 if (wrapped instanceof Exception) {
                     x = (Exception) wrapped;
                 }
             } else if (x instanceof EcmaError) {
                 msg = x.toString();
             } else {
                 msg = x.getMessage();
             }
            
             throw new ScriptingException(msg, x);
         }
     }
 
     /**
      *  Let the evaluator know that the current evaluation has been
      *  aborted.
      */
     public void abort() {
         // current request has been aborted.
         Thread t = thread;
         if (t != null && t.isAlive()) {
             t.interrupt();
             if ("true".equals(app.getProperty("requestTimeoutStop", "true"))) {
                 try {
                     Thread.sleep(5000);
                     if (t.isAlive()) {
                         // thread is still running, gotta stop it.
                         t.stop();
                     }
                 } catch (InterruptedException i) {
                     // interrupted, ignore
                 	app.logEvent("Thread interrupted in RhinoEngine::abort: "+i.getMessage());
                 }
             }
         }
     }
 
     /**
      * Check if an object has a function property (public method if it
      * is a java object) with that name.
      */
     public boolean hasFunction(Object obj, String fname) {
         // Convert '.' to '_' in function name
         fname = fname.replace('.', '_');
         // Treat AxiomObjects separately - otherwise we risk to fetch database
         // references/child objects just to check for function properties.
         if (obj instanceof INode) {
             String protoname = ((INode) obj).getPrototype();
             return core.hasFunction(protoname, fname);
         }
 
         Scriptable op = obj == null ? global : Context.toObject(obj, global);
 
         Object func = ScriptableObject.getProperty(op, fname);
 
         return func instanceof Function;
     }
 
     /**
      * Check if an object has a defined property (public field if it
      * is a java object) with that name.
      */
     public Object get(Object obj, String propname) {
         if ((obj == null) || (propname == null)) {
             return null;
         }
 
         String prototypeName = app.getPrototypeName(obj);
 
         if ("user".equalsIgnoreCase(prototypeName) &&
                 "password".equalsIgnoreCase(propname)) {
             return "[macro access to password property not allowed]";
         }
 
         // if this is a AxiomObject, check if the property is defined
         // in the prototype.properties db-mapping.
         if (obj instanceof INode) {
             DbMapping dbm = app.getDbMapping(prototypeName);
 
             if (dbm != null) {
                 Relation rel = dbm.propertyToRelation(propname);
 
                 if ((rel == null) || !rel.isPrimitive()) {
                     return "[property \"" + propname + "\" is not defined for " +
                            prototypeName + "]";
                 }
             }
         }
 
         Scriptable so = Context.toObject(obj, global);
 
         try {
             Object prop = so.get(propname, so);
 
             if ((prop == null) || (prop == Undefined.instance)
 	                       || (prop == ScriptableObject.NOT_FOUND)) {
                 return null;
             } else if (prop instanceof Wrapper) {
                 return ((Wrapper) prop).unwrap();
             } else {
                 // not all Rhino types convert to a string as expected
                 // when calling toString() - try to do better by using
                 // Rhino's ScriptRuntime.toString(). Note that this
                 // assumes that people always use this method to get
                 // a string representation of the object - which is
                 // currently the case since it's only used in Skin rendering.
                 try {
                     return ScriptRuntime.toString(prop);
                 } catch (Exception x) {
                     // just return original property object
                 }
                 return prop;
             }
         } catch (Exception esx) {
             // System.err.println ("Error in getProperty: "+esx);
             return null;
         }
     }
 
    /**
      * Provide object serialization for this engine's scripted objects. If no special
      * provisions are required, this method should just wrap the stream with an
      * ObjectOutputStream and write the object.
      *
      * @param obj the object to serialize
      * @param out the stream to write to
      * @throws java.io.IOException
      */
     public void serialize(Object obj, OutputStream out) throws IOException {
         Context.enter();
         try {
             // use a special ScriptableOutputStream that unwraps Wrappers
             ScriptableOutputStream sout = new ScriptableOutputStream(out, core.global) {
                 protected Object replaceObject(Object obj) {
                     if (obj instanceof Wrapper)
                         obj = ((Wrapper) obj).unwrap();
                     return super.replaceObject(obj);
                 }
             };
             sout.writeObject(obj);
             sout.flush();
         } finally {
             Context.exit();
         }
     }
 
     /**
      * Provide object deserialization for this engine's scripted objects. If no special
      * provisions are required, this method should just wrap the stream with an
      * ObjectIntputStream and read the object.
      *
      * @param in the stream to read from
      * @return the deserialized object
      * @throws java.io.IOException
      */
     public Object deserialize(InputStream in) throws IOException, ClassNotFoundException {
         Context.enter();
         try {
             ObjectInputStream sin = new ScriptableInputStream(in, core.global);
             return sin.readObject();
         } finally {
             Context.exit();
         }
     }
 
     /**
      * Return the application we're running in
      */
     public Application getApplication() {
         return app;
     }
 
     /**
      *  Return the RequestEvaluator owning and driving this FESI evaluator.
      */
     public RequestEvaluator getRequestEvaluator() {
         return reval;
     }
 
     /**
      *  Return the Response object of the current evaluation context.
      *  Proxy method to RequestEvaluator.
      */
     public ResponseTrans getResponse() {
         return reval.getResponse();
     }
 
     /**
      *  Return the Request object of the current evaluation context.
      *  Proxy method to RequestEvaluator.
      */
     public RequestTrans getRequest() {
         return reval.getRequest();
     }
 
     /**
      *  Return the RhinoCore object for the application this engine belongs to.
      *
      * @return this engine's RhinoCore instance
      */
     public RhinoCore getCore() {
         return core;
     }
     
     private Object toCachableResult(Object result, Scriptable obj) {
         Object ret = null;
        if (result instanceof String) {
            ret = (String) result;
        } else {
            String className = result.getClass().getSimpleName();
            String doctype = app.getProperty("doctype");
            doctype = (doctype != null) ? doctype+"\n" : "";
            if(className.equalsIgnoreCase("XML")){
            	ret = doctype+((XML) result).toXMLString();	
            } else if(className.equalsIgnoreCase("XHTML")){
            	ret = doctype+((XHTML) result).toXMLString();	
            } else if(className.equalsIgnoreCase("XMLList")){
            	ret = doctype+((XMLList) result).toXMLString();	
             } else {
            	ret = result;
             }
        }        
         return ret;
     }
     
     private boolean isResultCachable(Scriptable obj, Object result, String func) {
         if (result != null && obj instanceof AxiomObject) {
             INode n = ((AxiomObject) obj).getNode();
             String prototype = n.getPrototype();
             Prototype p = this.app.getPrototypeByName(prototype);
             if (p.isFunctionResponseCachable(func)) {
                 return true;
             }
         }
         return false;
     }
     
     public Object newArray(Object[] arr) {
         return Context.getCurrentContext().newArray(this.core.getScope(), arr);
     }
     
     public GlobalObject getGlobal() {
     	return this.global;
     }
 
 }
